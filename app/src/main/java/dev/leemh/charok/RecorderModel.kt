package dev.leemh.charok

import android.app.Application
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RecorderUi(val ready: Boolean = false, val mode: InputMode = InputMode.PLEOS,
    val phase: Phase = Phase.IDLE, val session: Session? = null, val bufferSeconds: Long = 0,
    val parked: Boolean = false, val gear: String = "미확인", val busy: Boolean = false,
    val message: String = "차량 연결 대기", val speech: String = "마이크 꺼짐", val nav: String = "내비 요청 없음",
    val notice: String? = null, val backgroundRunning: Boolean = false, val starting: Boolean = false,
    val voiceEnabled: Boolean = false, val startFailure: Boolean = false,
    val panelVisible: Boolean = false, val reportStatus: String? = null,
    val locationConsent: Boolean = false, val history: List<Session> = emptyList(), val failNextSave: Boolean = false)

class RecorderModel(app: Application) : AndroidViewModel(app) {
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val operation = Mutex()
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
    private var serviceRunning = false
    private var preparationRequested = false
    private val voiceConsent = VoiceConsent(app)
    private data class PendingVoiceStart(val note: String, val replies: MutableList<(String, String) -> Unit>)
    private var pendingVoiceStart: PendingVoiceStart? = null
    private var voiceDeadline: Job? = null
    private fun collectingAllowed() = foreground || serviceRunning
    private var generation = 0
    private var gear: String? = null
    private var gearAt: Long? = null
    private var speed: Double? = null
    private var speedAt: Long? = null
    private var testParked = true
    private val testTelltales = TelltaleSampler()
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
                operation.withLock {
                    if (!collectingAllowed() || !loaded) return@withLock
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
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch(worker) {
        operation.withLock {
            try { block() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { publish(message = "요청을 완료하지 못했습니다. 상태·권한·저장 공간을 확인하세요.") }
        }
    }
    private fun parked() = isParked(gear, gearAt, clock.elapsed()) && speed?.let { it.isFinite() && kotlin.math.abs(it) < 0.1 } == true && speedAt?.let { clock.elapsed() - it in 0..3000 } == true
    private fun motionKnown() = gear != null && gearAt?.let { clock.elapsed() - it in 0..3000 } == true &&
        speed?.isFinite() == true && speedAt?.let { clock.elapsed() - it in 0..3000 } == true
    private fun voiceEnabled() = if (mutable.value.mode == InputMode.TEST) BuildConfig.DEBUG && voiceConsent.testEnabled else voiceConsent.realEnabled
    private fun notificationsAllowed() = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
    private fun publish(message: String? = null) {
        mutable.value = mutable.value.copy(ready = loaded, phase = recorder.phase, session = recorder.session,
            bufferSeconds = recorder.bufferedMs() / 1000, parked = parked(), gear = gear ?: "미확인", busy = false,
            message = if (recorder.phase == Phase.SAVE_FAILED) recorder.storageError else message ?: mutable.value.message,
            failNextSave = failNext, backgroundRunning = serviceRunning,
            starting = pendingVoiceStart != null, voiceEnabled = voiceEnabled(), panelVisible = foreground)
    }
    fun foreground() = launch {
        foreground = true
        if (!serviceRunning) connect()
        publish()
    }
    fun background() = launch {
        foreground = false
        // App-owned STT is explicit and visible. Gleo remains the system voice entry point.
        withContext(Dispatchers.Main) { speech?.close(); navigator?.close() }
        mutable.value = mutable.value.copy(speech = "마이크 꺼짐")
        if (!serviceRunning && !preparationRequested) disconnect()
        publish()
    }
    private suspend fun disconnect() {
        generation++
        gear = null; gearAt = null; speed = null; speedAt = null
        withContext(Dispatchers.Main) { signals?.close(); signals = null; speech?.close() }
    }
    fun serviceStarted() = launch {
        if (!preparationRequested) {
            withContext(Dispatchers.Main) { getApplication<Application>().stopService(Intent(getApplication(), RecordingService::class.java)) }
            return@launch
        }
        preparationRequested = false
        serviceRunning = true
        testTelltales.reset()
        if (pendingVoiceStart != null) {
            connect()
            completeVoiceStartIfReady()
            publish(message = if (pendingVoiceStart != null) "요청하신 기록을 위해 차량 상태를 확인합니다" else "요청하신 기록을 시작했습니다")
        } else {
            withContext(Dispatchers.Main) { signals?.resetTelltales() }
            recorder.arm(mutable.value.mode)
            withContext(Dispatchers.Main) { signals?.setLocation(mutable.value.locationConsent) }
            publish(message = "다른 화면에서도 기록 준비를 이어갑니다")
        }
    }
    fun serviceStopped() = launch {
        finishVoiceRequest("fail", "The recording service stopped before recording could start.")
        preparationRequested = false
        serviceRunning = false
        if (loaded) recorder.stop("기록 서비스 종료", interrupted = true)
        withContext(Dispatchers.Main) { speech?.close(); signals?.setLocation(false) }
        if (!foreground) disconnect()
        publish()
    }
    private suspend fun connect() {
        generation++; val token = generation
        withContext(Dispatchers.Main) { signals?.close(); speech?.close(); signals = null }
        gear = null; gearAt = null; speed = null; speedAt = null
        if (!collectingAllowed()) return
        if (mutable.value.mode == InputMode.TEST) { gear = if (testParked) "PARK" else "DRIVE"; gearAt = clock.elapsed(); speed = if (testParked) 0.0 else 5.0; speedAt = clock.elapsed(); publish(); return }
        withContext(Dispatchers.Main) {
            signals = PleosSignals(getApplication(), { observation -> launch {
                if (collectingAllowed() && generation == token && mutable.value.mode == InputMode.PLEOS) {
                    if (observation.field == "gear") { gear = if (observation.available) observation.value else null; gearAt = observation.elapsed }
                    if (observation.field == "speed") { speed = if (observation.available) observation.value.toDoubleOrNull() else null; speedAt = observation.elapsed }
                    completeVoiceStartIfReady()
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
        check(loaded && pendingVoiceStart == null && recorder.phase !in listOf(Phase.RECORDING, Phase.ARMED, Phase.SAVE_FAILED))
        // TEST is a separate, explicit development input; never evidence of being parked in a real car.
        mutable.value = mutable.value.copy(mode = mode, locationConsent = false)
        connect(); publish(message = if (mode == InputMode.TEST) "TEST 입력 선택 · 실제 차량 데이터 아님" else "Pleos 차량 상태 수신 대기")
    }
    fun toggleTestMotion() = launch { if (mutable.value.mode == InputMode.TEST) { testParked = !testParked; gear = if (testParked) "PARK" else "DRIVE"; gearAt = clock.elapsed(); speed = if (testParked) 0.0 else 5.0; speedAt = clock.elapsed(); publish() } }
    fun notificationDenied() = launch { mutable.value = mutable.value.copy(notice = "진행 알림을 받으려면 차록의 알림 권한을 허용해 주세요. 기록 준비는 아직 꺼져 있습니다.") }
    fun arm(consent: Boolean) = launch {
        check(loaded && foreground && parked() && consent)
        check(notificationsAllowed())
        if (preparationRequested || serviceRunning) return@launch
        saveVoiceConsent(true)
        mutable.value = mutable.value.copy(notice = null, startFailure = false)
        preparationRequested = true
        try {
            withContext(Dispatchers.Main) {
                ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), RecordingService::class.java))
            }
        } catch (error: Exception) {
            preparationRequested = false
            mutable.value = mutable.value.copy(notice = "백그라운드 기록을 시작하지 못했습니다. 앱에서 다시 시도해 주세요.")
            publish()
        }
    }
    private fun saveVoiceConsent(enabled: Boolean) {
        if (mutable.value.mode == InputMode.TEST) {
            voiceConsent.testEnabled = enabled && BuildConfig.DEBUG
        } else voiceConsent.realEnabled = enabled
    }
    fun configureVoice(enabled: Boolean) = launch {
        check(loaded && foreground && parked() && !preparationRequested && !serviceRunning)
        if (enabled) check(notificationsAllowed())
        saveVoiceConsent(enabled)
        publish(message = if (enabled) "Gleo 음성 기록 설정 완료 · 호출 전에는 수집하지 않습니다" else "음성 기록 동의를 해제했습니다")
    }
    private fun finishVoiceRequest(status: String, message: String) {
        val request = pendingVoiceStart ?: return
        pendingVoiceStart = null
        mutable.value = mutable.value.copy(startFailure = status == "fail" && recorder.phase != Phase.SAVE_FAILED)
        voiceDeadline?.cancel(); voiceDeadline = null
        request.replies.forEach { reply -> runCatching { reply(status, message) } }
    }
    private suspend fun completeVoiceStartIfReady() {
        val request = pendingVoiceStart ?: return
        if (!serviceRunning || !motionKnown()) return
        if (!voiceEnabled() || !notificationsAllowed()) {
            finishVoiceRequest("fail", "Recording consent or notification permission is unavailable.")
            return
        }
        recorder.arm(mutable.value.mode)
        recorder.start(request.note, if (mutable.value.mode == InputMode.TEST) Origin.TEST else Origin.GLEO_INTENT)
        withContext(Dispatchers.Main) { signals?.setLocation(mutable.value.locationConsent) }
        finishVoiceRequest(if (recorder.phase == Phase.RECORDING) "success" else "fail",
            if (recorder.phase == Phase.RECORDING) commandSuccess("Recording has started.") else "Storage could not be confirmed. Retry saving the same record in Chalog.")
    }
    private fun commandSuccess(message: String) = if (mutable.value.mode == InputMode.TEST) "TEST input only. $message" else message
    fun locationConsent(enabled: Boolean) = launch {
        check(parked())
        mutable.value = mutable.value.copy(locationConsent = enabled)
        withContext(Dispatchers.Main) { signals?.setLocation(enabled && recorder.phase in listOf(Phase.ARMED, Phase.RECORDING)) }
        publish(message = if (enabled) "위치 포함 선택 · 실제 수신 시에만 저장" else "위치 수집 중지 · 이미 저장한 위치는 기록에 남아 있습니다")
    }
    fun start(text: String = "", source: Origin = Origin.MANUAL) = launch { startNow(text, source); publish() }
    private fun startNow(text: String, source: Origin) { check(loaded && serviceRunning); recorder.start(text, source) }
    fun mark() = launch { recorder.note("지금이야", if (mutable.value.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL, true); publish(message = "사용자가 표시한 순간을 저장했습니다") }
    fun testWarningLights(action: String) = launch {
        check(mutable.value.mode == InputMode.TEST && recorder.phase == Phase.RECORDING)
        if (action == "missing") recorder.observe(Observation(TELLTALE_READ, "", "snapshot", Origin.TEST, clock.elapsed(), clock.wall(), false))
        else {
            require(action in listOf("on", "off"))
            testTelltales.snapshot(listOf(LampSample("ENGINE", if (action == "on") "ON" else "OFF"),
                LampSample("TPMS", "BLINK")), Origin.TEST, clock.elapsed(), clock.wall()).forEach(recorder::observe)
        }
        publish(message = "TEST 경고등 관측을 기록했습니다 · 실제 차량 데이터 아님")
    }
    fun finish() = launch { finishNow(); publish(message = "관측 중지 · 저장 내용 재조회 완료") }
    private suspend fun finishNow() {
        mutable.value = mutable.value.copy(busy = true)
        withContext(Dispatchers.Main) { speech?.close(); signals?.setLocation(false); signals?.resetTelltales() }
        recorder.stop(); mutable.value = mutable.value.copy(speech = "마이크 꺼짐")
    }
    fun retry() = launch { mutable.value = mutable.value.copy(busy = true); recorder.retry(); publish() }
    fun closeRecord() = launch { withContext(Dispatchers.Main) { speech?.close(); signals?.setLocation(false); signals?.resetTelltales() }; recorder.close(); publish(message = "기록 준비 꺼짐 · 자동 재개하지 않습니다") }
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
        check(collectingAllowed() && loaded)
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
    fun gleo(command: GleoCommand, response: (String, String) -> Unit): Job {
        var replied = false
        val reply: (String, String) -> Unit = { status, message ->
            if (!replied) { replied = true; runCatching { response(status, message) } }
        }
        return launch {
            try {
                check(loaded)
                withContext(Dispatchers.Main) { speech?.close() }
                val origin = if (mutable.value.mode == InputMode.TEST) Origin.TEST else Origin.GLEO_INTENT
                when (command) {
                    GleoCommand.Open -> reply("fail", "Use the explicit Chalog panel action to view records.")
                    GleoCommand.Stop -> {
                        // Stop never requires a fresh vehicle reading, consent or an Activity.
                        finishVoiceRequest("fail", "The start request was cancelled by the driver.")
                        preparationRequested = false
                        finishNow()
                        reply(if (recorder.phase == Phase.SAVE_FAILED) "fail" else "success",
                            if (recorder.phase == Phase.SAVE_FAILED) "Collection stopped, but storage could not be confirmed. Retry saving the same record in Chalog."
                            else commandSuccess("Recording is stopped. Any active record has been saved."))
                    }
                    is GleoCommand.Add -> {
                        check(serviceRunning && recorder.phase == Phase.RECORDING)
                        recorder.note(command.note.ifBlank { "지금이야" }, origin, command.mark)
                        reply(if (recorder.phase == Phase.SAVE_FAILED) "fail" else "success",
                            if (recorder.phase == Phase.SAVE_FAILED) "Storage could not be confirmed. Retry saving the same record in Chalog."
                            else commandSuccess("The symptom event was saved."))
                    }
                    is GleoCommand.Start -> {
                        if (!voiceEnabled()) {
                            reply("fail", "Enable voice recording with consent in Chalog while parked first.")
                        } else if (!notificationsAllowed()) {
                            reply("fail", "Allow Chalog notifications while parked before starting a new record.")
                        } else if (mutable.value.mode == InputMode.PLEOS && ContextCompat.checkSelfPermission(getApplication(), "android.car.permission.CAR_SPEED") != PackageManager.PERMISSION_GRANTED) {
                            reply("fail", "Grant vehicle state permission in Chalog while parked first.")
                        } else if (recorder.phase == Phase.SAVE_FAILED) {
                            reply("fail", "A record still needs to be saved. Retry the same record in Chalog.")
                        } else if (pendingVoiceStart != null) {
                            if (pendingVoiceStart!!.replies.size < 8) pendingVoiceStart!!.replies.add(reply)
                            else reply("fail", "A recording start request is already being processed.")
                        } else if (serviceRunning && recorder.phase in listOf(Phase.ARMED, Phase.RECORDING)) {
                            startNow(command.note, origin)
                            reply(if (recorder.phase == Phase.RECORDING) "success" else "fail",
                                if (recorder.phase == Phase.RECORDING) commandSuccess("Recording is active.") else "Storage could not be confirmed. Retry the same record in Chalog.")
                        } else if (serviceRunning || preparationRequested) {
                            reply("fail", "The previous recording service is finishing. Please try again shortly.")
                        } else {
                            // A received user command, never app/process startup, initiates collection.
                            pendingVoiceStart = PendingVoiceStart(command.note, mutableListOf(reply))
                            mutable.value = mutable.value.copy(startFailure = false)
                            preparationRequested = true
                            publish(message = "음성 요청을 받아 기록을 준비합니다")
                            voiceDeadline = viewModelScope.launch(worker) {
                                delay(4000)
                                operation.withLock {
                                    if (pendingVoiceStart != null) {
                                        preparationRequested = false
                                        finishVoiceRequest("fail", "Vehicle state or the recording service is unavailable. No new record was started.")
                                        publish(message = "차량 상태를 확인하지 못해 기록을 시작하지 않았습니다")
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), RecordingService::class.java))
                            }
                        }
                    }
                }
                publish()
            } catch (_: Exception) {
                if (pendingVoiceStart?.replies?.contains(reply) == true) {
                    preparationRequested = false
                    finishVoiceRequest("fail", "Android could not start the background recording service. No new record was started.")
                } else reply("fail", "The record is not ready for this action.")
                publish(message = "요청을 완료하지 못했습니다. 기록 상태와 권한을 확인하세요.")
            }
        }
    }
    fun reportDelivery(message: String?) = launch { mutable.value = mutable.value.copy(reportStatus = message); publish() }
    fun loadHistory() = launch { check(parked()); mutable.value = mutable.value.copy(history = store.list()); publish() }
    fun supplement(text: String) = launch { check(parked()); recorder.supplement(text); publish(message = "원문을 유지하고 사용자 보완을 저장했습니다") }
    fun openHistory(id: String) = launch { check(parked()); recorder.open(id); publish() }
    fun export(includeLocation: Boolean, done: (File) -> Unit) = launch {
        mutable.value = mutable.value.copy(reportStatus = null)
        try {
            check(parked() && recorder.phase in listOf(Phase.SAVED, Phase.INTERRUPTED))
            val selected = store.get(recorder.session!!.id) ?: error("No saved record")
            val report = if (includeLocation) selected else selected.copy(observations = selected.observations.filterNot { it.origin == Origin.PLEOS_LOCATION })
            val dir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
            dir.listFiles()?.filter { clock.wall() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
            val file = File(dir, "chalog-${selected.id}.txt")
            file.writeText(ConsultationReport.text(report)); check(file.readText() == ConsultationReport.text(report))
            File(dir, "chalog-${selected.id}.json").writeText(SessionCodec.encode(report))
            mutable.value = mutable.value.copy(reportStatus = "보고서 파일 생성·재조회 완료 · 업체 수신 미확인")
            publish()
            withContext(Dispatchers.Main) { done(file) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            mutable.value = mutable.value.copy(reportStatus = "보고서를 만들지 못했습니다. 기존 기록은 보존됐습니다.")
            publish()
        }
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
