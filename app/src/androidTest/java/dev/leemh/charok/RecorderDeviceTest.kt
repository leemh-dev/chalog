package dev.leemh.charok

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.leemh.charok.core.*
import dev.leemh.charok.data.ConsultationReport
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorderDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<CharokApp>()
    private fun has(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun await(tag: String) = ui.waitUntil(15_000) { has(tag) }
    private fun tap(tag: String) { ui.onNodeWithTag(tag).performScrollTo().performClick(); ui.waitForIdle() }
    private fun prepare() {
        ui.waitUntil(15_000) { has("close") || has("test-mode") }
        if (has("close")) tap("close")
        await("test-mode"); tap("test-mode")
        ui.waitUntil { ui.onAllNodesWithText("TEST · P 확인").fetchSemanticsNodes().isNotEmpty() }
        tap("consent"); tap("arm"); await("start")
    }
    private fun capture(name: String) {
        val dir = File(app.getExternalFilesDir(null), "evidence").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out ->
            ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
    @Test fun recordsBookmarksAndPersistsRealDatabase() {
        prepare(); Thread.sleep(2200); capture("01-prepared-test")
        tap("start"); await("test-note"); tap("test-note")
        ui.waitUntil { app.store.latest()?.notes?.isNotEmpty() == true }
        capture("02-recording-test")
        ui.onNodeWithTag("mark").performClick()
        ui.waitUntil { app.store.latest()?.notes?.any { it.bookmark } == true }
        capture("03-marked-test")
        ui.onNodeWithTag("stop").performClick(); await("details")
        val saved = app.store.latest()!!
        assertEquals(Phase.SAVED, saved.phase); assertEquals(InputMode.TEST, saved.mode)
        assertTrue(saved.observations.isNotEmpty()); assertTrue(saved.observations.all { it.origin == Origin.TEST })
        assertTrue(saved.notes.any { it.bookmark }); assertEquals(saved, app.store.get(saved.id))
        capture("04-saved-test"); tap("details"); capture("05-consultation-test")
        assertTrue(ConsultationReport.text(saved).contains("TEST"))
        File(app.getExternalFilesDir(null), "evidence/session-proof.json").writeText(dev.leemh.charok.data.SessionCodec.encode(saved))
    }
    @Test fun failedSaveRetriesSameIdWithoutNewSession() {
        prepare(); tap("start"); await("test-note")
        val id = app.store.latest()!!.id
        tap("fail-save"); await("retry"); capture("06-save-failed-test")
        tap("retry"); await("details")
        assertEquals(id, app.store.latest()!!.id)
        assertEquals(Phase.SAVED, app.store.latest()!!.phase)
    }
    @Test fun drivingLocksDetailsButAllowsStop() {
        prepare(); tap("start"); await("motion"); tap("motion")
        ui.onNodeWithTag("stop").performClick(); await("details")
        ui.onNodeWithTag("details").assertIsNotEnabled()
        capture("07-driving-lock-test")
        tap("motion"); ui.onNodeWithTag("details").assertIsEnabled()
    }
    @Test fun recreationRetainsSessionAndBackgroundDoesNotResume() {
        prepare(); tap("start"); await("stop"); val id = app.store.latest()!!.id
        ui.activityRule.scenario.recreate(); await("stop")
        assertEquals(id, app.store.latest()!!.id)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(1500)
        assertEquals(Phase.INTERRUPTED, app.store.latest()!!.phase)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        await("details"); assertEquals(id, app.store.latest()!!.id)
        assertFalse(has("stop"))
    }
    @Test fun rapidCommandsKeepTheSameSession() {
        prepare(); tap("start"); await("stop")
        val original = app.store.latest()!!
        val count = app.store.list().size
        ui.activityRule.scenario.onActivity { activity ->
            val model = androidx.lifecycle.ViewModelProvider(activity)[RecorderModel::class.java]
            repeat(8) { model.start() }
            repeat(8) { model.finish() }
        }
        await("details")
        assertEquals(original.id, app.store.latest()!!.id)
        assertEquals(count, app.store.list().size)
        assertEquals(Phase.SAVED, app.store.latest()!!.phase)
    }
}
