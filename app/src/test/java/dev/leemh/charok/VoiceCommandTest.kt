package dev.leemh.charok

import dev.leemh.charok.core.*
import org.junit.Assert.*
import org.junit.Test

class VoiceCommandTest {
    @Test fun onlyExplicitStopStops() {
        assertEquals(VoiceCommand.Stop, parseVoice("차록 기록 종료해 줘."))
        assertTrue(parseVoice("시동을 종료하면 소리가 나요") is VoiceCommand.Description)
        assertTrue(parseVoice("종료하지 말고 소리만 기록해 줘") is VoiceCommand.Description)
    }
    @Test fun startAndMarkUseDistinctIntents() {
        assertEquals(VoiceCommand.Start, parseVoice("증상 기록해 줘"))
        assertEquals(VoiceCommand.Mark, parseVoice("지금이야"))
        assertTrue(parseVoice("지금은 소리가 안 나요") is VoiceCommand.Description)
    }
    @Test fun followUpDescriptionOnStartKeepsSameSession() {
        val store = MemoryStore(); val r = Recorder(TestClock(), store); r.arm(InputMode.PLEOS)
        val id = r.start(); r.start("공조 소음", Origin.GLEO_INTENT); r.start("공조 소음", Origin.GLEO_INTENT)
        assertEquals(id, r.session!!.id); assertEquals(1, r.session!!.notes.size)
    }
}
