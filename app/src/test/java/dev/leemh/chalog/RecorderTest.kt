package dev.leemh.chalog

import dev.leemh.chalog.core.*
import dev.leemh.chalog.data.*
import org.junit.Assert.*
import org.junit.Test

class TestClock(var time: Long = 100_000, var wallTime: Long = 1_800_000_000_000) : Clock {
    override fun elapsed() = time
    override fun wall() = wallTime
    fun advance(ms: Long) { time += ms; wallTime += ms }
}
class MemoryStore : RecordStore {
    val records = linkedMapOf<String, Session>()
    var fail = false
    override fun save(session: Session) { if (fail) error("disk full"); records[session.id] = session }
    override fun get(id: String) = records[id]
    override fun latest() = records.values.lastOrNull()
    override fun list() = records.values.toList()
}
class RecorderTest {
    private val clock = TestClock()
    private val store = MemoryStore()
    private val recorder = Recorder(clock, store)
    private fun sample(origin: Origin = Origin.TEST) = Observation("gear", "PARK", "enum", origin, clock.elapsed(), clock.wall())
    private fun ready() { recorder.arm(InputMode.TEST); recorder.observe(sample()) }
    @Test fun noPreparationCannotInventPast() {
        assertThrows(IllegalStateException::class.java) { recorder.start() }
        recorder.arm(InputMode.TEST); recorder.start()
        assertTrue(recorder.session!!.observations.isEmpty())
    }
    @Test fun twentySecondsAreNotSixty() {
        ready(); clock.advance(20_000); recorder.observe(sample())
        assertEquals(20_000, recorder.bufferedMs()); recorder.start()
        assertEquals(2, recorder.session!!.observations.size)
    }
    @Test fun oldBufferIsEvictedEvenWithoutNewCallbacks() {
        ready(); clock.advance(61_000); assertEquals(0, recorder.bufferedMs()); recorder.start()
        assertTrue(recorder.session!!.observations.isEmpty())
    }
    @Test fun sixtySecondBoundaryUsesMonotonicTime() {
        ready(); clock.advance(60_000); clock.wallTime -= 300_000
        recorder.observe(sample()); recorder.start(); assertEquals(2, recorder.session!!.observations.size)
    }
    @Test fun duplicateStartAndStopKeepOneSession() {
        ready(); val id = recorder.start(); assertEquals(id, recorder.start()); recorder.stop(); recorder.stop()
        assertEquals(1, store.records.size); assertEquals(id, recorder.session!!.id)
    }
    @Test fun separateIdenticalUtterancesArePreservedButDeliveryIsDeduplicated() {
        ready(); recorder.start(); recorder.note("지금", Origin.TEST, true, "a"); recorder.note("지금", Origin.TEST, true, "a")
        recorder.note("지금", Origin.TEST, true, "b"); assertEquals(2, recorder.session!!.notes.size)
    }
    @Test fun stopReallyStopsObservationsAndNotes() {
        ready(); recorder.start(); recorder.stop(); val saved = recorder.session
        clock.advance(1000); recorder.observe(sample()); assertEquals(saved, recorder.session)
        assertThrows(IllegalStateException::class.java) { recorder.note("후속", Origin.TEST) }
    }
    @Test fun failureStopsAndRetryNeverStartsNewSession() {
        ready(); val id = recorder.start(); store.fail = true
        recorder.note("소리", Origin.TEST); assertEquals(Phase.SAVE_FAILED, recorder.phase)
        assertThrows(IllegalStateException::class.java) { recorder.arm(InputMode.TEST) }
        clock.advance(1000); recorder.observe(sample()); store.fail = false; recorder.retry()
        assertEquals(id, recorder.session!!.id); assertEquals(Phase.INTERRUPTED, recorder.phase)
        assertEquals(1, store.records.size); assertEquals(1, recorder.session!!.notes.size)
    }
    @Test fun lostSaveResponseReusesIdAndContents() {
        var failRead = false
        val responseLoss = object : RecordStore by store {
            override fun get(id: String): Session? { if (failRead) error("read response lost"); return store.get(id) }
        }
        val r = Recorder(clock, responseLoss); r.arm(InputMode.TEST); r.start(); failRead = true; r.stop()
        assertEquals(Phase.SAVE_FAILED, r.phase); val id = r.session!!.id
        failRead = false; r.retry(); assertEquals(id, r.session!!.id); assertEquals(1, store.records.size)
    }
    @Test fun newEngineRestoresCheckpointButNeverAcquires() {
        ready(); recorder.start(); recorder.note("소리", Origin.TEST, true)
        val r = Recorder(clock, store); r.restore(); assertEquals(Phase.INTERRUPTED, r.phase)
        assertNull(r.session!!.endedAt); assertEquals(0, r.bufferedMs()); val before = r.session
        r.observe(sample()); assertEquals(before, r.session)
    }
    @Test fun savedRestoreDoesNotOverwriteReason() {
        ready(); recorder.start(); recorder.stop(); val before = recorder.session
        val r = Recorder(clock, store); r.restore(); assertEquals(before, r.session)
    }
    @Test fun backgroundInterruptAndCloseDoNotRearm() {
        ready(); recorder.start(); recorder.stop("앱 비활성", true)
        assertEquals(Phase.INTERRUPTED, recorder.phase); recorder.close(); assertEquals(Phase.IDLE, recorder.phase)
    }
    @Test fun foreignModeObservationCannotContaminateSession() {
        ready(); recorder.observe(sample(Origin.PLEOS_VEHICLE)); recorder.start()
        assertEquals(1, recorder.session!!.observations.size)
    }
    @Test fun staleOrFutureCallbackIsRejected() {
        ready(); recorder.observe(sample().copy(elapsed = 1)); recorder.observe(sample().copy(elapsed = clock.time + 1))
        recorder.start(); assertEquals(1, recorder.session!!.observations.size)
    }
    @Test fun timeLimitStopsEvenIfNoObservationsArrive() {
        ready(); recorder.start(); clock.advance(600_000); recorder.tick()
        assertEquals(Phase.SAVED, recorder.phase); assertEquals("10분 기록 상한", recorder.session!!.reason)
    }
    @Test fun simultaneousStartsRemainOneSession() {
        ready(); val threads = List(8) { Thread { recorder.start() } }
        threads.forEach { it.start() }; threads.forEach { it.join() }; assertEquals(1, store.records.size)
    }
    @Test fun parkedGateRequiresFreshKnownGear() {
        assertFalse(isParked(null, null, clock.time)); assertFalse(isParked("PARK", clock.time - 3001, clock.time))
        assertFalse(isParked("DRIVE", clock.time, clock.time)); assertTrue(isParked("PARK", clock.time, clock.time))
    }
    @Test fun codecPreservesOriginMissingValuesAndQuotes() {
        ready(); recorder.start("덜컥\n\"소리\"", Origin.GLEO_INTENT)
        recorder.observe(sample().copy(available = false)); recorder.stop()
        val s = recorder.session!!; assertEquals(s, SessionCodec.decode(SessionCodec.encode(s)))
        val report = ConsultationReport.text(s)
        assertTrue(report.contains("TEST")); assertTrue(report.contains("수신 불가")); assertTrue(report.contains("GLEO_INTENT"))
    }
    @Test fun savedSupplementPreservesOriginalAndDoesNotResume() {
        ready(); val id = recorder.start("앞쪽 소리", Origin.PLEOS_STT); recorder.stop()
        recorder.supplement("앞쪽보다 오른쪽에 가까웠어요")
        assertEquals(id, recorder.session!!.id); assertEquals(Phase.SAVED, recorder.phase)
        assertEquals("앞쪽 소리", recorder.session!!.notes.first().text)
        assertTrue(store.get(id)!!.notes.last().text.startsWith("사용자 보완:"))
    }
    @Test fun failedSupplementRetriesSameSavedRecord() {
        ready(); val id = recorder.start(); recorder.stop(); store.fail = true; recorder.supplement("설명 보완")
        assertEquals(Phase.SAVE_FAILED, recorder.phase); store.fail = false; recorder.retry()
        assertEquals(id, recorder.session!!.id); assertEquals(Phase.SAVED, recorder.phase)
        assertEquals(1, store.get(id)!!.notes.size)
    }
}
