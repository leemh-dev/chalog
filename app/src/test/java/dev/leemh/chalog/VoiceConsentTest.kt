package dev.leemh.chalog

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class VoiceConsentTest {
    @Test fun realConsentSurvivesRecreationButTestConsentDoesNot() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first = VoiceConsent(context)
        assertFalse(first.realEnabled)
        first.realEnabled = true; first.testEnabled = true
        val recreated = VoiceConsent(context)
        assertTrue(recreated.realEnabled); assertFalse(recreated.testEnabled)
        recreated.realEnabled = false
        assertFalse(VoiceConsent(context).realEnabled)
    }
}
