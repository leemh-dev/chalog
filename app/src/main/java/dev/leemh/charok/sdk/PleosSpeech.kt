package dev.leemh.charok.sdk

import android.content.Context
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale

/** Only official on-device STT. No raw sound recording, RMS animation or external ASR. */
class PleosSpeech(private val context: Context, private val finalText: (String) -> Unit, private val status: (String) -> Unit) {
    private var stt: SpeechToText? = null
    private var tts: ai.pleos.playground.tts.TextToSpeech? = null
    @Volatile private var active = false
    @Volatile private var delivered = false
    private val listener = object : ResultListener {
        override fun onReady() { if (active) status("마이크 준비됨 · 원음 저장 안 함") }
        override fun onStartedRecognition() { if (active) status("말씀을 듣고 있어요") }
        override fun onUpdated(stt: String, completed: Boolean) {
            if (active && completed && !delivered && stt.isNotBlank()) {
                delivered = true; stop(); finalText(stt.take(1000))
            }
        }
        override fun onEndedRecognition() { if (active) { stop(); status("음성 인식 종료") } }
        override fun onError(errorType: ErrorType) { if (active) { stop(); status("음성 인식 실패: $errorType") } }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }
    fun listen() {
        stop(); runCatching { tts?.stop() }; active = true; delivered = false
        status("공식 음성 서비스 연결 중")
        try {
            val speech = SpeechToText(context, Mode.ON_DEVICE); stt = speech
            speech.addListener(listener)
            speech.initialize(object : OnInitializeListener {
                override fun onSuccess() { if (active && stt === speech) { speech.setLocale(Locale.KOREAN); speech.request() } }
                override fun onFailure(errorType: ErrorType) { if (stt === speech) { stop(); status("음성 초기화 실패: $errorType") } }
            })
        } catch (_: Exception) { stop(); status("음성 서비스 연결/권한 확인 필요") }
        catch (_: LinkageError) { stop(); status("이 환경에 Pleos 음성 서비스가 없습니다") }
    }
    fun say(text: String) {
        stop(); runCatching { tts?.release() }
        try {
            val voice = ai.pleos.playground.tts.TextToSpeech(context, ai.pleos.playground.tts.constant.Mode.ON_DEVICE); tts = voice
            voice.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { if (tts === voice) { voice.setLocale(Locale.KOREAN); voice.speak(text, false) } }
                override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) { status("음성 응답 실패: $errorType") }
            })
        } catch (_: Exception) { status("음성 응답 연결 실패") }
    }
    fun stop() { active = false; val previous = stt; stt = null; runCatching { previous?.removeListener(listener); previous?.stop(); previous?.release() } }
    fun close() { stop(); val voice = tts; tts = null; runCatching { voice?.stop(); voice?.release() } }
}
