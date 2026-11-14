package dev.leemh.charok

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.leemh.charok.core.*
import dev.leemh.charok.data.ConsultationReport
import dev.leemh.charok.data.SessionCodec
import dev.leemh.charok.sdk.*
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecorderUi(val ready: Boolean = false, val mode: InputMode = InputMode.PLEOS,
    val phase: Phase = Phase.IDLE, val session: Session? = null, val bufferSeconds: Long = 0,
    val parked: Boolean = false, val gear: String = "미확인", val busy: Boolean = false,
    val message: String = "차량 연결 대기", val speech: String = "마이크 꺼짐", val nav: String = "내비 요청 없음",
    val locationConsent: Boolean = false, val history: List<Session> = emptyList(), val failNextSave: Boolean = false)

class RecorderModel(app: Application) : AndroidViewModel(app) {
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val clock = object : Clock { override fun elapsed() = SystemClock.elapsedRealtime(); override fun wall() = System.currentTimeMillis() }
    private val backing = (app as CharokApp).store
    private var failNext = false
    private val store = object : RecordStore by backing {
        override fun save(session: Session) { if (failNext) { failNext = false; error("TEST 저장 실패") }; backing.save(session) }
    }
    private val recorder = Recorder(clock, store)
    private val mutable = MutableStateFlow(RecorderUi())
    val ui = mutable.asStateFlow()
    private var foreground = false
    private var generation = 0
    private var gear: String? = null
    private var gearAt: Long? = null
    private var speed: Double? = null
    private var speedAt: Long? = null
    private var testParked = true
    private var signals: PleosSignals? = null
    private var speech: PleosSpeech? = null
    private var navigator: CenterNavigator? = null
    private var loaded = false

    init {
        launch {
            recorder.restore(); loaded = true
            // Always start on real, unknown vehicle status even if a saved session came from TEST.
            publish(message = if (recorder.phase == Phase.INTERRUPTED) "마지막 저장 지점 복원 · 수집은 꺼짐" else "기록 준비를 켜기 전에 입력 환경을 확인하세요")
        }
        viewModelScope.launch(worker) {
            while (isActive) {
                delay(1000)
                if (!foreground || !loaded) continue
                if (mutable.value.mode == InputMode.TEST) {
                    gear = if (testParked) "PARK" else "DRIVE"; gearAt = clock.elapsed()
                    speed = if (testParked) 0.0 else 5.0; speedAt = clock.elapsed()
                    if (recorder.phase in listOf(Phase.ARMED, Phase.RECORDING)) {
                        listOf("gear" to gear!!, "speed" to speed.toString(), "steering" to if (testParked) "0.0" else "12.0", "brake" to testParked.toString()).forEach { (field, value) ->
                            recorder.observe(Observation(field, value, when(field) { "steering" -> "degree"; "speed" -> "m/s"; else -> "state" }, Origin.TEST, clock.elapsed(), clock.wall()))
                        }
                    }
                } else withContext(Dispatchers.Main) { signals?.poll(recorder.phase in listOf(Phase.ARMED, Phase.RECORDING)) }
                recorder.tick(); publish()
                if (recorder.phase !in listOf(Phase.ARMED, Phase.RECORDING)) withContext(Dispatchers.Main) { signals?.setLocation(false) }
            }
        }
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch(worker) {
        try { block() } catch (_: Exception) { publish(message = "요청을 완료하지 못했습니다. 상태·권한·저장 공간을 확인하세요.") }
    }
    private fun parked() = isParked(gear, gearAt, clock.elapsed()) && speed?.let { it.isFinite() && kotlin.math.abs(it) < 0.1 } == true && speedAt?.let { clock.elapsed() - it in 0..3000 } == true
    private fun publish(message: String? = null) {
        mutable.value = mutable.value.copy(ready = loaded, phase = recorder.phase, session = recorder.session,
            bufferSeconds = recorder.bufferedMs() / 1000, parked = parked(), gear = gear ?: "미확인", busy = false,
            message = if (recorder.phase == Phase.SAVE_FAILED) recorder.storageError else message ?: mutable.value.message,
            failNextSave = failNext)
    }
    fun foreground() = launch {
        foreground = true; connect()
    }
    fun background() = launch {
        foreground = false; generation++; gear = null; gearAt = null; speed = null; speedAt = null
        withContext(Dispatchers.Main) { signals?.close(); signals = null; speech?.close(); navigator?.close() }
        if (loaded) recorder.stop("앱 비활성: 관측 중지", interrupted = true)
        mutable.value = mutable.value.copy(locationConsent = false, speech = "마이크 꺼짐")
        publish()
    }
    private suspend fun connect() {
        generation++; val token = generation
        withContext(Dispatchers.Main) { signals?.close(); speech?.close(); signals = null }
        gear = null; gearAt = null; speed = null; speedAt = null
        if (!foreground) return
        if (mutable.value.mode == InputMode.TEST) { gear = if (testParked) "PARK" else "DRIVE"; gearAt = clock.elapsed(); speed = if (testParked) 0.0 else 5.0; speedAt = clock.elapsed(); publish(); return }
        withContext(Dispatchers.Main) {
            signals = PleosSignals(getApplication(), { observation -> launch {
                if (foreground && generation == token && mutable.value.mode == InputMode.PLEOS) {
                    if (observation.field == "gear") { gear = if (observation.available) observation.value else null; gearAt = observation.elapsed }
                    if (observation.field == "speed") { speed = if (observation.available) observation.value.toDoubleOrNull() else null; speedAt = observation.elapsed }
                    recorder.observe(observation)
                    if (observation.field in listOf("gear", "speed") && !observation.available) {
                        recorder.stop("차량 상태 연결/권한 손실", interrupted = true)
                        withContext(Dispatchers.Main) { signals?.setLocation(false); speech?.close() }
                    }
                    publish(message = if (observation.available) "Pleos SDK 관측 수신 · 실차 여부는 별도 확인" else "일부 차량 상태 수신 불가")
                }
            } }, { message -> launch { if (generation == token) publish(message) } }).also { it.start() }
            speech = PleosSpeech(getApplication(), { text -> voice(text, Origin.PLEOS_STT) }, { status -> launch { mutable.value = mutable.value.copy(speech = status) } })
        }
    }
    fun chooseMode(mode: InputMode) = launch {
        check(loaded && recorder.phase !in listOf(Phase.RECORDING, Phase.ARMED, Phase.SAVE_FAILED))
        // TEST is a separate, explicit development input; never evidence of being parked in a real car.
        mutable.value = mutable.value.copy(mode = mode, locationConsent = false)
        connect(); publish(message = if (mode == InputMode.TEST) "TEST 입력 선택 · 실제 차량 데이터 아님" else "Pleos 차량 상태 수신 대기")
    }
    fun toggleTestMotion() = launch { if (mutable.value.mode == InputMode.TEST) { testParked = !testParked; gear = if (testParked) "PARK" else "DRIVE"; gearAt = clock.elapsed(); speed = if (testParked) 0.0 else 5.0; speedAt = clock.elapsed(); publish() } }
    fun arm(consent: Boolean) = launch {
        check(loaded && foreground && parked() && consent)
        recorder.arm(mutable.value.mode)
        withContext(Dispatchers.Main) { signals?.setLocation(mutable.value.locationConsent) }
        publish(message = "실제 수신한 최근 구간을 준비합니다")
    }
    fun locationConsent(enabled: Boolean) = launch {
        check(parked())
        mutable.value = mutable.value.copy(locationConsent = enabled)
        withContext(Dispatchers.Main) { signals?.setLocation(enabled && recorder.phase in listOf(Phase.ARMED, Phase.RECORDING)) }
        publish(message = if (enabled) "위치 포함 선택 · 실제 수신 시에만 저장" else "위치 수집 중지 · 이미 저장한 위치는 기록에 남아 있습니다")
    }
    fun start(text: String = "", source: Origin = Origin.MANUAL) = launch { startNow(text, source); publish() }
    private fun startNow(text: String, source: Origin) { check(loaded && foreground); recorder.start(text, source) }
    fun mark() = launch { recorder.note("지금이야", if (mutable.value.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL, true); publish(message = "사용자가 표시한 순간을 저장했습니다") }
    fun finish() = launch { finishNow(); publish(message = "관측 중지 · 저장 내용 재조회 완료") }
    private suspend fun finishNow() {
        mutable.value = mutable.value.copy(busy = true)
        withContext(Dispatchers.Main) { speech?.close(); signals?.setLocation(false) }
        recorder.stop(); mutable.value = mutable.value.copy(speech = "마이크 꺼짐")
    }
    fun retry() = launch { mutable.value = mutable.value.copy(busy = true); recorder.retry(); publish() }
    fun closeRecord() = launch { withContext(Dispatchers.Main) { speech?.close(); signals?.setLocation(false) }; recorder.close(); publish(message = "기록 준비 꺼짐 · 자동 재개하지 않습니다") }
    fun injectFailure() = launch {
        check(BuildConfig.DEBUG && mutable.value.mode == InputMode.TEST)
        failNext = true
        if (recorder.phase == Phase.RECORDING) finishNow()
        publish(message = "DB 저장 1회 실패 TEST")
    }
    fun listen() = launch {
        check(foreground && mutable.value.mode == InputMode.PLEOS && recorder.phase in listOf(Phase.ARMED, Phase.RECORDING))
        withContext(Dispatchers.Main) { speech?.listen() }
    }
    fun stopListening() = launch { withContext(Dispatchers.Main) { speech?.close() }; mutable.value = mutable.value.copy(speech = "마이크 꺼짐") }
    fun voice(text: String, source: Origin = Origin.TEST) = launch {
        check(foreground && loaded)
        if (source == Origin.TEST) check(mutable.value.mode == InputMode.TEST)
        val reply = when (val command = parseVoice(text)) {
            VoiceCommand.Start -> { startNow("", source); "기록을 시작했어요. 어떤 증상이 있었나요?" }
            VoiceCommand.Stop -> { finishNow(); "기록을 멈췄어요." }
            VoiceCommand.Mark -> { recorder.note(text, source, true); "방금 순간을 표시했어요." }
            is VoiceCommand.Description -> {
                if (recorder.phase == Phase.ARMED) startNow(command.text, source) else recorder.note(command.text, source)
                "증상 설명을 남겼어요. 다시 느껴지면 지금이라고 말씀해 주세요."
            }
        }
        publish(message = reply)
        if (source == Origin.PLEOS_STT) withContext(Dispatchers.Main) { speech?.say(if (recorder.phase == Phase.SAVE_FAILED) recorder.storageError else reply) }
    }
    fun gleo(action: String, note: String, mark: Boolean, response: (String, String) -> Unit) = launch {
        withContext(Dispatchers.Main) { speech?.close() }
        if (!loaded || !foreground || mutable.value.mode != InputMode.PLEOS) { response("fail", "Open Charok in Pleos input mode first."); return@launch }
        if (action == "START_SYMPTOM_RECORD" && recorder.phase !in listOf(Phase.ARMED, Phase.RECORDING)) {
            response("fail", "Please enable recording preparation with consent while parked first."); return@launch
        }
        if (action == "ADD_SYMPTOM_NOTE" && note.isBlank() && !mark) { response("asking", "What symptom would you like to add?"); return@launch }
        if (action == "END_SYMPTOM_RECORD" && recorder.phase !in listOf(Phase.RECORDING, Phase.SAVED, Phase.INTERRUPTED)) { response("fail", "There is no active or saved record to stop."); return@launch }
        try {
            when (action) {
                "START_SYMPTOM_RECORD" -> startNow(note, Origin.GLEO_INTENT)
                "ADD_SYMPTOM_NOTE" -> recorder.note(note, Origin.GLEO_INTENT, mark)
                "END_SYMPTOM_RECORD" -> finishNow()
                else -> error("Unknown action")
            }
            publish()
            if (recorder.phase == Phase.SAVE_FAILED) response("fail", "Recording stopped because storage could not be confirmed. Please retry saving the same record.")
            else if (action == "START_SYMPTOM_RECORD" && recorder.session?.notes?.isEmpty() == true) response("asking", "Recording has started. What symptom did you notice?")
            else response("success", if (action == "END_SYMPTOM_RECORD") "Recording has stopped. Check the saved record in Charok." else "The request was saved in Charok.")
        } catch (_: Exception) { response("fail", "The record is not ready for this action.") }
    }
    fun loadHistory() = launch { check(parked()); mutable.value = mutable.value.copy(history = store.list()); publish() }
    fun openHistory(id: String) = launch { check(parked()); recorder.open(id); publish() }
    fun export(includeLocation: Boolean, done: (File) -> Unit) = launch {
        check(parked() && recorder.phase in listOf(Phase.SAVED, Phase.INTERRUPTED))
        val selected = store.get(recorder.session!!.id) ?: error("No saved record")
        val report = if (includeLocation) selected else selected.copy(observations = selected.observations.filterNot { it.origin == Origin.PLEOS_LOCATION })
        val dir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.filter { clock.wall() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        val file = File(dir, "charok-${selected.id}.txt")
        file.writeText(ConsultationReport.text(report)); check(file.readText() == ConsultationReport.text(report))
        File(dir, "charok-${selected.id}.json").writeText(SessionCodec.encode(report))
        publish(message = "보고서 파일 생성·재조회 완료 · 업체 수신 미확인")
        withContext(Dispatchers.Main) { done(file) }
    }
    fun navigate(target: CenterTarget) = launch {
        check(parked() && mutable.value.mode == InputMode.PLEOS && target.valid())
        withContext(Dispatchers.Main) {
            navigator?.close(); navigator = CenterNavigator(getApplication()) { message -> launch { mutable.value = mutable.value.copy(nav = message) } }
            navigator!!.request(target)
        }
    }
    override fun onCleared() { signals?.close(); speech?.close(); navigator?.close(); worker.close(); super.onCleared() }
}
