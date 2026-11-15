package dev.leemh.charok

import android.os.Process
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.leemh.charok.core.Phase
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Two invocations, with host force-stop between them. Excluded from the normal device class. */
@RunWith(AndroidJUnit4::class)
class ProcessRecoveryTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun allowTestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("dev.leemh.charok", android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private val app get() = ApplicationProvider.getApplicationContext<CharokApp>()
    private fun has(tag: String) = runCatching { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    private fun tap(tag: String) = ui.onNodeWithTag(tag).performScrollTo().performClick()
    @Test fun seedForProcessKill() {
        ui.waitUntil(15_000) { has("close") || has("input-settings") }
        if (has("close")) tap("close")
        ui.waitUntil(10_000) { has("input-settings") }; tap("input-settings"); tap("test-mode")
        ui.waitUntil(10_000) { ui.onAllNodesWithText("TEST · P 확인").fetchSemanticsNodes().isNotEmpty() }
        tap("consent"); tap("arm"); ui.waitUntil(10_000) { has("start") }; tap("test-tools")
        tap("start"); ui.waitUntil(10_000) { has("test-note") }; tap("test-note")
        ui.onNodeWithTag("mark").performClick()
        ui.waitUntil(10_000) { app.store.latest()?.notes?.any { it.bookmark } == true }
        val s = app.store.latest()!!
        assertEquals(Phase.RECORDING, s.phase)
        File(app.filesDir, "process-seed.json").writeText(JSONObject().put("id", s.id).put("pid", Process.myPid()).put("notes", s.notes.size).toString())
        // Host terminates the live recording process after reading the ready marker.
        Thread.sleep(120_000)
        fail("Host did not terminate the seed process")
    }
    @Test fun verifyAfterProcessKill() {
        ui.waitUntil(15_000) { has("details") }
        val seed = JSONObject(File(app.filesDir, "process-seed.json").readText())
        val restored = app.store.get(seed.getString("id"))!!
        assertNotEquals(seed.getInt("pid"), Process.myPid())
        assertEquals(Phase.INTERRUPTED, restored.phase)
        assertEquals(seed.getInt("notes"), restored.notes.size)
        assertNull(restored.endedAt)
        assertFalse(has("stop"))
        val count = restored.observations.size
        Thread.sleep(1800)
        assertEquals(count, app.store.get(restored.id)!!.observations.size)
        val proof = JSONObject().put("sessionId", restored.id).put("oldPid", seed.getInt("pid")).put("newPid", Process.myPid()).put("phase", restored.phase.name).put("notes", restored.notes.size).put("observationsUnchanged", true).put("input", "TEST").toString()
        // Internal copy supports normal run-as access on Pleos passenger/user profiles.
        listOf(File(app.filesDir, "evidence"), File(app.getExternalFilesDir(null), "evidence")).forEach { dir ->
            dir.mkdirs(); File(dir, "process-recovery.json").writeText(proof)
        }
    }
}
