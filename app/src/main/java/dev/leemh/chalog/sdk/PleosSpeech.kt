package dev.leemh.chalog.sdk

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
    @Volatile private var generation = 0L
    private var currentListener: ResultListener? = null
    private fun valid(token: Long) = active && generation == token
    private fun listener(token: Long) = object : ResultListener {
        override fun onReady() { if (valid(token)) status("마이크 준비됨 · 원음 저장 안 함") }
        override fun onStartedRecognition() { if (valid(token)) status("말씀을 듣고 있어요") }
        override fun onUpdated(stt: String, completed: Boolean) {
            if (completed) accept(token, stt)
        }
        override fun onEndedRecognition() { if (valid(token)) { stop(); status("음성 인식 종료") } }
        override fun onError(errorType: ErrorType) { if (valid(token)) { stop(); status("음성 인식 실패: $errorType") } }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }
    @Synchronized private fun accept(token: Long, text: String) {
        if (!valid(token) || delivered || text.isBlank()) return
        delivered = true; stop(); finalText(text.take(1000))
    }
    fun listen() {
        stop(); val oldVoice = tts; tts = null
        runCatching { oldVoice?.stop(); oldVoice?.release() }; active = true; delivered = false
        val token = generation
        status("공식 음성 서비스 연결 중")
        try {
            val speech = SpeechToText(context, Mode.ON_DEVICE); stt = speech
            currentListener = listener(token).also { speech.addListener(it) }
            speech.initialize(object : OnInitializeListener {
                override fun onSuccess() { if (valid(token) && stt === speech) {
                    runCatching { speech.setLocale(Locale.KOREAN); speech.request() }.onFailure { stop(); status("음성 인식 요청 실패") }
                } }
                override fun onFailure(errorType: ErrorType) { if (valid(token) && stt === speech) { stop(); status("음성 초기화 실패: $errorType") } }
            })
        } catch (_: Exception) { stop(); status("음성 서비스 연결/권한 확인 필요") }
        catch (_: LinkageError) { stop(); status("이 환경에 Pleos 음성 서비스가 없습니다") }
    }
    fun say(text: String) {
        stop(); val oldVoice = tts; tts = null; runCatching { oldVoice?.release() }
        try {
            val voice = ai.pleos.playground.tts.TextToSpeech(context, ai.pleos.playground.tts.constant.Mode.ON_DEVICE); tts = voice
            voice.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { if (tts === voice) { runCatching { voice.setLocale(Locale.KOREAN); voice.speak(text, false) }.onFailure { status("음성 응답 요청 실패") } } }
                override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) { if (tts === voice) status("음성 응답 실패: $errorType") }
            })
        } catch (_: Exception) { status("음성 응답 연결 실패") }
    }
    @Synchronized fun stop() {
        active = false; generation++; val previous = stt; stt = null
        val oldListener = currentListener; currentListener = null
        runCatching { if (oldListener != null) previous?.removeListener(oldListener); previous?.stop(); previous?.release() }
    }
    fun close() { stop(); val voice = tts; tts = null; runCatching { voice?.stop(); voice?.release() } }
}
