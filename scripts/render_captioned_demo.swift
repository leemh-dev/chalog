import AVFoundation
import AppKit
import CoreText
import Foundation
import ImageIO

// Actual timestamped display frames only. Adds separate title/subtitle bands;
// never draws or replaces application/system UI. Run after stopping the AVD.
struct Frame: Decodable { let path: String; let timestamp: Double }
struct Clip: Codable {
    let start: Double; let end: Double
    let title: String; let subtitle: String
}
struct Plan: Decodable { let capture: String; let clips: [Clip] }
let args = CommandLine.arguments
guard args.count == 3 else { fatalError("Usage: renderer PLAN.json OUTPUT.mp4") }
let planURL = URL(fileURLWithPath: args[1])
let plan = try JSONDecoder().decode(Plan.self, from: Data(contentsOf: planURL))
let root = URL(fileURLWithPath: plan.capture)
let manifest = try String(contentsOf: root.appendingPathComponent("frames.jsonl"), encoding: .utf8)
let frames = try manifest.split(separator: "\n").map { try JSONDecoder().decode(Frame.self, from: Data($0.utf8)) }
precondition(!frames.isEmpty && !plan.clips.isEmpty)
let output = URL(fileURLWithPath: args[2])
precondition(!FileManager.default.fileExists(atPath: output.path), "Preserve existing output")
let width = 1920, height = 1280, fps = 24
let fontName = "AppleSDGothicNeo-Medium"
guard let titleFont = NSFont(name: fontName, size: 34),
      let subtitleFont = NSFont(name: fontName, size: 32),
      let metaFont = NSFont(name: fontName, size: 22) else { fatalError("Korean font unavailable") }
let meta = "CHALOG 0.3.1 · 실제 Pleos 화면\nTEST 입력 · 무음 · 버튼/알림 직접 조작"
let allText = plan.clips.map { $0.title + $0.subtitle }.joined() + meta
let characters = Array(allText.replacingOccurrences(of: "\n", with: "").utf16)
var glyphs = [CGGlyph](repeating: 0, count: characters.count)
let ctFont = CTFontCreateWithName(fontName as CFString, 32, nil)
precondition(CTFontGetGlyphsForCharacters(ctFont, characters, &glyphs, characters.count), "Missing subtitle glyph")
precondition(!glyphs.contains(0), "Missing Korean glyph")

let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
writer.shouldOptimizeForNetworkUse = true
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width, AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 2_000_000,
        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
        AVVideoExpectedSourceFrameRateKey: fps,
        AVVideoMaxKeyFrameIntervalKey: fps * 2,
        AVVideoAllowFrameReorderingKey: false
    ]
])
input.expectsMediaDataInRealTime = false
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input,
    sourcePixelBufferAttributes: [
        kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
        kCVPixelBufferWidthKey as String: width,
        kCVPixelBufferHeightKey as String: height,
        kCVPixelBufferCGImageCompatibilityKey as String: true,
        kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
    ])
writer.add(input)
precondition(writer.startWriting())
writer.startSession(atSourceTime: .zero)

func text(_ value: String, font: NSFont, rect: CGRect, alignment: NSTextAlignment) {
    let style = NSMutableParagraphStyle()
    style.alignment = alignment
    style.lineBreakMode = .byWordWrapping
    let string = NSAttributedString(string: value, attributes: [
        .font: font, .foregroundColor: NSColor.white, .paragraphStyle: style
    ])
    let measured = string.boundingRect(with: rect.size, options: [.usesLineFragmentOrigin, .usesFontLeading])
    precondition(measured.height <= rect.height + 1, "Caption would be clipped: \(value)")
    string.draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading])
}
func render(_ source: Frame, _ clip: Clip) throws -> CVPixelBuffer {
    var pixel: CVPixelBuffer?
    precondition(CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &pixel) == kCVReturnSuccess)
    let buffer = pixel!
    CVPixelBufferLockBaseAddress(buffer, [])
    defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
    let ctx = CGContext(data: CVPixelBufferGetBaseAddress(buffer), width: width, height: height,
        bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
        space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    ctx.setFillColor(CGColor(red: 0.045, green: 0.078, blue: 0.14, alpha: 1))
    ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))
    let data = try Data(contentsOf: root.appendingPathComponent(source.path))
    guard let imageSource = CGImageSourceCreateWithData(data as CFData, nil),
          let image = CGImageSourceCreateImageAtIndex(imageSource, 0, nil) else { fatalError("Invalid frame") }
    precondition(image.width == 2560 && image.height == 1440)
    ctx.interpolationQuality = .high
    ctx.draw(image, in: CGRect(x: 0, y: 112, width: 1920, height: 1080))
    ctx.setFillColor(CGColor(red: 0.25, green: 0.42, blue: 1, alpha: 1))
    ctx.fill(CGRect(x: 0, y: 1192, width: 1920, height: 2))
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)
    text(clip.title, font: titleFont, rect: CGRect(x: 48, y: 1206, width: 1160, height: 52), alignment: .left)
    text(meta, font: metaFont, rect: CGRect(x: 1230, y: 1206, width: 642, height: 58), alignment: .right)
    text(clip.subtitle, font: subtitleFont, rect: CGRect(x: 80, y: 14, width: 1760, height: 82), alignment: .center)
    NSGraphicsContext.restoreGraphicsState()
    return buffer
}
func timecode(_ seconds: Double) -> String {
    let ms = Int((seconds * 1000).rounded())
    return String(format: "%02d:%02d:%02d,%03d", ms / 3600000, ms / 60000 % 60, ms / 1000 % 60, ms % 1000)
}
var outputIndex = 0
var timeline: [[String: Any]] = []
var srt = ""
for (clipIndex, clip) in plan.clips.enumerated() {
    precondition(clip.end > clip.start && clip.start >= frames.first!.timestamp && clip.end <= frames.last!.timestamp)
    let count = Int(((clip.end - clip.start) * Double(fps)).rounded())
    let begin = Double(outputIndex) / Double(fps)
    var index = frames.lastIndex(where: { $0.timestamp <= clip.start }) ?? 0
    var lastIndex = -1
    var buffer: CVPixelBuffer?
    for n in 0..<count {
        let sourceTime = clip.start + Double(n) / Double(fps)
        while index + 1 < frames.count && frames[index + 1].timestamp <= sourceTime { index += 1 }
        if index != lastIndex {
            buffer = try autoreleasepool { try render(frames[index], clip) }
            lastIndex = index
        }
        while !input.isReadyForMoreMediaData {
            if writer.status == .failed { fatalError(writer.error!.localizedDescription) }
            Thread.sleep(forTimeInterval: 0.005)
        }
        precondition(adaptor.append(buffer!, withPresentationTime: CMTime(value: Int64(outputIndex), timescale: Int32(fps))), writer.error?.localizedDescription ?? "Append failed")
        outputIndex += 1
    }
    let end = Double(outputIndex) / Double(fps)
    timeline.append(["index": clipIndex + 1, "start": begin, "end": end,
        "title": clip.title, "subtitle": clip.subtitle,
        "source_start": clip.start, "source_end": clip.end])
    srt += "\(clipIndex + 1)\n\(timecode(begin)) --> \(timecode(end))\n\(clip.title)\n\(clip.subtitle)\n\n"
    print("Rendered clip \(clipIndex + 1)/\(plan.clips.count): \(begin)–\(end)s")
    fflush(stdout)
}
input.markAsFinished()
let completion = DispatchSemaphore(value: 0)
writer.finishWriting { completion.signal() }
completion.wait()
precondition(writer.status == .completed, writer.error?.localizedDescription ?? "Encoding failed")
let base = output.deletingPathExtension()
try (srt.trimmingCharacters(in: .newlines) + "\n").write(to: base.appendingPathExtension("srt"), atomically: true, encoding: .utf8)
let metadata: [String: Any] = ["width": width, "height": height, "fps": fps,
    "frames": outputIndex, "duration": Double(outputIndex) / Double(fps),
    "font": fontName, "all_subtitle_glyphs_present": true, "audio": false,
    "source": "Actual timestamped complete Android display; chronological cuts, no invented UI",
    "source_image_rect": [0, 88, 1920, 1080], "timeline": timeline]
try JSONSerialization.data(withJSONObject: metadata, options: [.prettyPrinted, .sortedKeys]).write(to: base.appendingPathExtension("json"))
print("Completed actual-screen video with embedded Korean titles/captions and UTF-8 SRT.")
