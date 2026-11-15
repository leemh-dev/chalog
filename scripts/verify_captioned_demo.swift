import AVFoundation
import AppKit
import Foundation
import Vision

// Decode the entire final file, then inspect the actual encoded chapter frames.
let source = URL(fileURLWithPath: CommandLine.arguments[1])
let destination = URL(fileURLWithPath: CommandLine.arguments[2])
try FileManager.default.createDirectory(at: destination, withIntermediateDirectories: true)
let metadata = try JSONSerialization.jsonObject(with: Data(contentsOf: source.deletingPathExtension().appendingPathExtension("json"))) as! [String: Any]
let timeline = metadata["timeline"] as! [[String: Any]]
let asset = AVURLAsset(url: source)
let tracks = asset.tracks(withMediaType: .video)
precondition(tracks.count == 1 && asset.tracks(withMediaType: .audio).isEmpty)
let track = tracks[0]
precondition(track.naturalSize == CGSize(width: 1920, height: 1280))
let duration = CMTimeGetSeconds(asset.duration)
precondition(abs(duration - (metadata["duration"] as! Double)) < 0.05)
let reader = try AVAssetReader(asset: asset)
let output = AVAssetReaderTrackOutput(track: track, outputSettings: [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
])
reader.add(output)
precondition(reader.startReading())
var decoded = 0
var previous = -Double.infinity
while let sample = output.copyNextSampleBuffer() {
    let time = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sample))
    precondition(time > previous)
    previous = time
    decoded += 1
}
precondition(reader.status == .completed, reader.error?.localizedDescription ?? "Decode failed")
precondition(decoded == metadata["frames"] as! Int)

func recognize(_ image: CGImage, region: CGRect) throws -> String {
    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    request.recognitionLanguages = ["ko-KR", "en-US"]
    request.usesLanguageCorrection = false
    request.regionOfInterest = region
    try VNImageRequestHandler(cgImage: image).perform([request])
    return (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ")
}
func normalized(_ s: String) -> String {
    s.precomposedStringWithCanonicalMapping.unicodeScalars.filter { CharacterSet.alphanumerics.contains($0) }.map(String.init).joined()
}
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.requestedTimeToleranceBefore = .zero
generator.requestedTimeToleranceAfter = .zero
var checks: [[String: Any]] = []
for (index, clip) in timeline.enumerated() {
    let start = clip["start"] as! Double, end = clip["end"] as! Double
    let second = (start + end) / 2
    var actual = CMTime.zero
    let image = try generator.copyCGImage(at: CMTime(seconds: second, preferredTimescale: 600), actualTime: &actual)
    let name = String(format: "chapter-%02d.png", index + 1)
    let data = NSBitmapImageRep(cgImage: image).representation(using: .png, properties: [:])!
    try data.write(to: destination.appendingPathComponent(name))
    let header = try recognize(image, region: CGRect(x: 0, y: 1192.0 / 1280, width: 0.64, height: 88.0 / 1280))
    let caption = try recognize(image, region: CGRect(x: 0, y: 0, width: 1, height: 112.0 / 1280))
    let expectedTitle = clip["title"] as! String, expectedCaption = clip["subtitle"] as! String
    checks.append(["frame": name, "requested_seconds": second,
        "actual_seconds": CMTimeGetSeconds(actual), "expected_title": expectedTitle,
        "title_ocr": header, "title_ocr_matches": normalized(header).contains(normalized(expectedTitle)),
        "expected_subtitle": expectedCaption, "subtitle_ocr": caption,
        "subtitle_ocr_matches": normalized(caption).contains(normalized(expectedCaption))])
    print("Checked encoded chapter \(index + 1)/\(timeline.count)")
    fflush(stdout)
}
// Also decode both ends, so the file cannot start/end with an unreadable frame.
for second in [0.0, max(0, duration - 0.05)] {
    _ = try generator.copyCGImage(at: CMTime(seconds: second, preferredTimescale: 600), actualTime: nil)
}
let proof: [String: Any] = ["duration_seconds": duration, "width": 1920, "height": 1280,
    "nominal_fps": track.nominalFrameRate, "decoded_frames": decoded,
    "all_frames_decoded": true, "strictly_increasing_timestamps": true,
    "audio_tracks": 0, "subtitle_font_glyph_check": metadata["all_subtitle_glyphs_present"]!,
    "chapter_checks": checks, "note": "OCR differences require visual review; they are not silently marked passed."]
try JSONSerialization.data(withJSONObject: proof, options: [.prettyPrinted, .sortedKeys]).write(to: destination.appendingPathComponent("video-verification.json"))
print("Full video decoded. Chapter frames and Korean OCR saved for visual review.")
