package dev.leemh.charok.core

import java.util.UUID

enum class InputMode { TEST, PLEOS }
enum class Origin { TEST, AAOS_PROPERTY, PLEOS_VEHICLE, PLEOS_LOCATION, PLEOS_STT, GLEO_INTENT, MANUAL }
enum class Phase { IDLE, ARMED, RECORDING, SAVED, INTERRUPTED, SAVE_FAILED }
interface Clock { fun elapsed(): Long; fun wall(): Long }
data class Observation(val field: String, val value: String, val unit: String, val origin: Origin,
    val elapsed: Long, val receivedAt: Long, val available: Boolean = true, val sampleAt: Long? = null)
data class Note(val id: String, val text: String, val origin: Origin, val elapsed: Long, val at: Long, val bookmark: Boolean)
data class Session(val id: String, val mode: InputMode, val startedAt: Long, val triggerElapsed: Long,
    val phase: Phase = Phase.RECORDING, val endedAt: Long? = null, val reason: String = "",
    val observations: List<Observation> = emptyList(), val notes: List<Note> = emptyList())
interface RecordStore { fun save(session: Session); fun get(id: String): Session?; fun latest(): Session?; fun list(): List<Session> }

/** Serialized by a monitor; all storage work is invoked on a worker dispatcher by the app. */
class Recorder(private val clock: Clock, private val store: RecordStore, private val id: () -> String = { UUID.randomUUID().toString() }) {
    var phase = Phase.IDLE; private set
    var mode = InputMode.PLEOS; private set
    var session: Session? = null; private set
    var storageError = ""; private set
    private var pending: Session? = null
    private val buffer = ArrayDeque<Observation>()
    private var armedAt = 0L
    private var lastObserved = 0L
    private val deliveries = linkedSetOf<String>()

    @Synchronized fun restore() {
        check(phase == Phase.IDLE)
        val previous = store.latest() ?: return
        session = previous; mode = previous.mode
        if (previous.phase == Phase.RECORDING) persist(previous.copy(phase = Phase.INTERRUPTED,
            reason = "프로세스 종료: 마지막 저장 지점까지만 복원", endedAt = null))
        else phase = previous.phase
        buffer.clear() // Never rebuild past observations as a live preparation buffer.
    }

    @Synchronized fun arm(input: InputMode) {
        check(phase != Phase.RECORDING && phase != Phase.SAVE_FAILED) { "현재 기록을 먼저 종료하거나 저장하세요." }
        mode = input; phase = Phase.ARMED; session = null; pending = null; storageError = ""
        buffer.clear(); deliveries.clear(); armedAt = clock.elapsed(); lastObserved = armedAt
    }

    @Synchronized fun bufferedMs(): Long {
        trim()
        if (buffer.isEmpty()) return 0
        return (clock.elapsed() - buffer.first().elapsed).coerceIn(0, 60_000)
    }

    @Synchronized fun observe(value: Observation) {
        if (phase != Phase.ARMED && phase != Phase.RECORDING) return
        if ((value.origin == Origin.TEST) != (mode == InputMode.TEST)) return
        if (value.elapsed < armedAt || value.elapsed > clock.elapsed() || value.field.length > 80 || value.value.length > 512) return
        if (phase == Phase.ARMED) {
            buffer.addLast(value); trim(); return
        }
        val current = session ?: return
        if (clock.elapsed() - current.triggerElapsed >= 600_000) { stop("10분 기록 상한"); return }
        if (current.observations.size >= 6_000) { stop("관측 개수 상한"); return }
        if (value.elapsed < lastObserved) return // Late asynchronous callback: do not move the timeline backwards.
        lastObserved = value.elapsed
        persist(current.copy(observations = current.observations + value))
    }

    @Synchronized fun start(text: String = "", origin: Origin = Origin.MANUAL): String {
        if (phase == Phase.RECORDING) {
            if (session!!.notes.isEmpty() && text.isNotBlank()) note(text, origin)
            return session!!.id
        }
        check(phase == Phase.ARMED) { "정차 후 기록 준비를 먼저 켜주세요." }
        trim()
        val now = clock.elapsed()
        val current = Session(id(), mode, clock.wall(), now, observations = buffer.toList(),
            notes = if (text.isBlank()) emptyList() else listOf(Note(id(), text.take(1000), origin, now, clock.wall(), false)))
        buffer.clear(); lastObserved = now
        persist(current)
        return current.id
    }

    @Synchronized fun note(text: String, origin: Origin, bookmark: Boolean = false, deliveryId: String? = null) {
        check(phase == Phase.RECORDING) { "기록 중에만 설명을 추가할 수 있습니다." }
        if (deliveryId != null && deliveryId in deliveries) return
        val current = session!!
        check(current.notes.size < 200) { "설명 개수 상한에 도달했습니다." }
        require(text.isNotBlank() || bookmark)
        val updated = current.copy(notes = current.notes + Note(id(), text.take(1000), origin, clock.elapsed(), clock.wall(), bookmark))
        persist(updated)
        if (phase != Phase.SAVE_FAILED && deliveryId != null) deliveries.add(deliveryId)
    }

    @Synchronized fun tick() {
        if (phase == Phase.RECORDING && clock.elapsed() - session!!.triggerElapsed >= 600_000) stop("10분 기록 상한")
        if (phase == Phase.ARMED) trim()
    }

    @Synchronized fun stop(reason: String = "사용자 종료", interrupted: Boolean = false) {
        buffer.clear()
        if (phase == Phase.ARMED) { phase = Phase.IDLE; return }
        if (phase != Phase.RECORDING) return
        persist(session!!.copy(phase = if (interrupted) Phase.INTERRUPTED else Phase.SAVED,
            endedAt = clock.wall(), reason = reason))
    }

    @Synchronized fun retry() {
        val value = pending ?: return
        // A failed checkpoint must never silently resume acquisition.
        persist(if (value.phase == Phase.RECORDING) value.copy(phase = Phase.INTERRUPTED,
            reason = "저장 실패 후 관측 중지", endedAt = clock.wall()) else value)
    }

    @Synchronized fun close() {
        check(phase != Phase.SAVE_FAILED) { "저장되지 않은 기록이 있습니다. 먼저 재시도하세요." }
        stop("화면 닫기", interrupted = true)
        if (phase != Phase.SAVE_FAILED) { phase = Phase.IDLE; session = null; buffer.clear() }
    }

    @Synchronized fun open(id: String) {
        check(phase != Phase.RECORDING && phase != Phase.ARMED && phase != Phase.SAVE_FAILED)
        val value = store.get(id) ?: error("저장된 기록을 찾지 못했습니다.")
        check(value.phase != Phase.RECORDING)
        session = value; phase = value.phase; mode = value.mode
    }

    private fun trim() {
        val oldest = clock.elapsed() - 60_000
        while (buffer.isNotEmpty() && buffer.first().elapsed < oldest) buffer.removeFirst()
        while (buffer.size > 1200) buffer.removeFirst()
    }

    private fun persist(value: Session) {
        try {
            store.save(value)
            check(store.get(value.id) == value) { "저장 후 재조회 불일치" }
            session = value; phase = value.phase; pending = null; storageError = ""
        } catch (_: Exception) {
            pending = value; session = value; phase = Phase.SAVE_FAILED
            storageError = "저장을 확인하지 못했습니다. 같은 기록으로 다시 저장하세요."
            buffer.clear()
        }
    }
}

/** Parked state is never inferred from unavailable or old readings. */
fun isParked(gear: String?, receivedElapsed: Long?, now: Long): Boolean =
    gear == "PARK" && receivedElapsed != null && now - receivedElapsed in 0..3_000
