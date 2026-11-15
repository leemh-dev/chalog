package dev.leemh.charok

import dev.leemh.charok.core.*
import org.junit.Assert.*
import org.junit.Test

class GleoCommandTest {
    @Test fun controlAndExplicitOpenAreDifferentIntents() {
        assertEquals(GleoCommand.Start("덜컥 소리"), GleoContract.parse(GleoContract.CONTROL, """{"operation":"start","note":"덜컥 소리"}"""))
        assertEquals(GleoCommand.Stop, GleoContract.parse(GleoContract.CONTROL, """{"operation":"stop"}"""))
        assertEquals(GleoCommand.Open, GleoContract.parse(GleoContract.OPEN, "{}"))
    }
    @Test fun descriptionAndMomentKeepTheirMeaning() {
        assertEquals(GleoCommand.Add("회전할 때 소음", false), GleoContract.parse(GleoContract.ADD, """{"note":"회전할 때 소음"}"""))
        assertEquals(GleoCommand.Add("", true), GleoContract.parse(GleoContract.ADD, """{"markMoment":true}"""))
    }
    @Test fun malformedMissingAndOversizedCommandsAreRejected() {
        for ((action, raw) in listOf(
            GleoContract.CONTROL to "{}", GleoContract.CONTROL to """{"operation":"delete"}""",
            GleoContract.CONTROL to """{"operation":true}""", GleoContract.ADD to "{}",
            GleoContract.ADD to """{"markMoment":"true"}""", GleoContract.ADD to """{"note":null}""",
            "START_SYMPTOM_RECORD" to "{}", GleoContract.CONTROL to "not json",
            GleoContract.CONTROL to "x".repeat(8193), GleoContract.ADD to """{"note":"${"x".repeat(1001)}"}"""
        )) assertTrue("$action must reject invalid input", runCatching { GleoContract.parse(action, raw) }.isFailure)
    }
    @Test fun parametersCannotSwitchInputOrGrantConsent() {
        assertEquals(GleoCommand.Start(), GleoContract.parse(GleoContract.CONTROL,
            """{"operation":"start","mode":"TEST","consent":true,"locationConsent":true}"""))
    }
}
