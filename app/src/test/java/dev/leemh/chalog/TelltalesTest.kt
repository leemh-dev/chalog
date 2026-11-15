package dev.leemh.chalog

import dev.leemh.chalog.core.*
import dev.leemh.chalog.data.ConsultationReport
import dev.leemh.chalog.data.SessionCodec
import org.junit.Assert.*
import org.junit.Test

class TelltalesTest {
    private fun session(rows: List<Observation>, mode: InputMode = InputMode.TEST) =
        Session("lamp-record", mode, 10_000, 1_000, Phase.SAVED, 20_000, observations = rows)
    private fun sample(s: TelltaleSampler, time: Long, vararg lamps: Pair<String, String>) =
        s.snapshot(lamps.map { LampSample(it.first, it.second) }, Origin.TEST, time, 10_000 + time)

    @Test fun repeatedLitSamplesBecomeOneLampAndExplicitOffKeepsItsHistory() {
        val s = TelltaleSampler()
        val rows = sample(s, 1_100, "ENGINE" to "ON", "TPMS" to "BLINK") +
            sample(s, 2_000, "ENGINE" to "ON", "TPMS" to "BLINK") +
            sample(s, 3_000, "ENGINE" to "OFF", "TPMS" to "BLINK")
        val lamps = session(rows).observedLamps()
        assertEquals(2, lamps.size)
        assertEquals(1_100L, lamps[0].firstLit.elapsed)
        assertEquals("소등 관측", lamps[0].stateLabel)
        assertEquals("점멸 관측", lamps[1].stateLabel)
    }
    @Test fun omittedLampIsUnknownInsteadOfCleared() {
        val s = TelltaleSampler()
        val rows = sample(s, 1_100, "ENGINE" to "ON") + sample(s, 2_000)
        assertEquals("이후 상태 미확인", session(rows).observedLamps().single().stateLabel)
        assertFalse(rows.any { it.field == "telltale:ENGINE" && it.value == "OFF" })
    }
    @Test fun failedReadDoesNotHideARecordedWarningOrClaimItIsStillOn() {
        val s = TelltaleSampler()
        val rows = sample(s, 1_100, "ENGINE" to "ON") +
            Observation(TELLTALE_READ, "", "snapshot", Origin.TEST, 2_000, 12_000, false)
        val record = session(rows)
        assertEquals(1, record.observedLamps().size)
        assertEquals("이후 상태 미확인", record.observedLamps().single().stateLabel)
        assertEquals("경고등 수신 불가", record.lampCoverage())
    }
    @Test fun absentDataAndSuccessfulEmptyReadAreDifferent() {
        assertEquals("경고등 데이터 없음", session(emptyList()).lampCoverage())
        val s = TelltaleSampler()
        val record = session(sample(s, 1_100, "ENGINE" to "OFF"))
        assertTrue(record.observedLamps().isEmpty())
        assertEquals("수신 항목의 점등 기록 없음", record.lampCoverage())
    }
    @Test fun preparationTimeAndUnknownTypeArePreservedWithoutInventingAName() {
        val s = TelltaleSampler()
        val lamp = session(sample(s, 500, "FUTURE_LAMP" to "ON")).observedLamps().single()
        assertEquals(-500L, lamp.firstLit.elapsed - 1_000)
        assertEquals("표시등 · FUTURE_LAMP", lamp.label)
    }
    @Test fun invalidStatesAndTestInputNeverBecomeRealWarningEvidence() {
        val s = TelltaleSampler()
        val rows = sample(s, 1_100, "ENGINE" to "UNKNOWN", "TPMS" to "BLINK")
        assertEquals(listOf("TPMS"), session(rows).observedLamps().map { it.type })
        assertTrue(session(rows, InputMode.PLEOS).observedLamps().isEmpty())
        assertEquals("경고등 데이터 없음", session(rows, InputMode.PLEOS).lampCoverage())
    }
    @Test fun codecAndConsultationReportPreserveStatesSourceAndObservedTimes() {
        val s = TelltaleSampler()
        val record = session(sample(s, 500, "ENGINE" to "ON") + sample(s, 2_000, "ENGINE" to "OFF"))
        val restored = SessionCodec.decode(SessionCodec.encode(record))
        assertEquals(record, restored)
        val report = ConsultationReport.text(restored)
        assertTrue(report.contains("엔진 경고등 [ENGINE]"))
        assertTrue(report.contains("최초 관측 10500 [TEST]"))
        assertTrue(report.contains("마지막 확인 12000: 소등 관측"))
        s.reset()
        assertTrue(session(sample(s, 3_000, "ENGINE" to "OFF")).observedLamps().isEmpty())
    }
}
