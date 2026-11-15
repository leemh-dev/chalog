package dev.leemh.chalog

import android.app.Application
import android.content.Intent
import dev.leemh.chalog.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GleoIntentsTest {
    @Test fun separatelyTypedExtrasUseTheSameContract() {
        val intent = Intent(GleoContract.ADD).putExtra("note", "덜컥").putExtra("markMoment", true).putExtra("consent", true)
        val raw = GleoIntents.parameters(intent)
        assertFalse(raw.contains("consent"))
        assertEquals(GleoCommand.Add("덜컥", true), GleoContract.parse(intent.action, raw))
    }
    @Test fun jsonPayloadTakesPrecedenceOverConflictingExtras() {
        val intent = Intent(GleoContract.CONTROL).putExtra("operation", "start")
            .putExtra("ACTION_PARAM", """{"operation":"stop"}""")
        assertEquals(GleoCommand.Stop, GleoContract.parse(intent.action, GleoIntents.parameters(intent)))
    }
    @Test fun unexpectedExtraTypesAreRejected() {
        assertTrue(runCatching { GleoIntents.parameters(Intent().putExtra("note", 12)) }.isFailure)
    }
}
