package dev.leemh.chalog

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.leemh.chalog.core.*
import dev.leemh.chalog.data.ConsultationReport
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorderDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun allowTestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("dev.leemh.charok", android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    @org.junit.After fun stopThisTestWithoutDeletingRecords() {
        kotlinx.coroutines.runBlocking {
            app.recorder.finish().join()
            if (app.recorder.ui.value.phase == Phase.SAVE_FAILED) app.recorder.retry().join()
            if (app.recorder.ui.value.mode == InputMode.TEST && app.recorder.ui.value.gear == "DRIVE") app.recorder.toggleTestMotion().join()
        }
        val deadline = android.os.SystemClock.elapsedRealtime() + 5000
        while (app.recorder.ui.value.backgroundRunning && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
    }
    private val app get() = ApplicationProvider.getApplicationContext<ChalogApp>()
    private fun has(tag: String) = runCatching { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    private fun await(tag: String) = ui.waitUntil(15_000) { has(tag) }
    private fun tap(tag: String) { ui.waitForIdle(); ui.onNodeWithTag(tag).performScrollTo(); ui.waitForIdle(); ui.onNodeWithTag(tag).performClick(); ui.waitForIdle() }
    private fun prepare() {
        ui.waitUntil(15_000) { has("close") || has("input-settings") }
        if (has("close")) tap("close")
        await("input-settings"); tap("input-settings"); tap("test-mode")
        ui.waitUntil(10_000) { ui.onAllNodesWithText("TEST · P 확인").fetchSemanticsNodes().isNotEmpty() }
        tap("consent"); tap("arm"); await("start"); tap("test-tools")
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
        ui.waitUntil(10_000) { app.store.latest()?.notes?.isNotEmpty() == true }
        capture("02-recording-test")
        ui.onNodeWithTag("mark").performClick()
        ui.waitUntil(10_000) { app.store.latest()?.notes?.any { it.bookmark } == true }
        capture("03-marked-test")
        ui.onNodeWithTag("stop").performClick(); await("details")
        val saved = app.store.latest()!!
        assertEquals(Phase.SAVED, saved.phase); assertEquals(InputMode.TEST, saved.mode)
        assertTrue(saved.observations.isNotEmpty()); assertTrue(saved.observations.all { it.origin == Origin.TEST })
        assertTrue(saved.notes.any { it.bookmark }); assertEquals(saved, app.store.get(saved.id))
        capture("04-saved-test"); tap("details"); capture("05-consultation-test")
        assertTrue(ConsultationReport.text(saved).contains("TEST"))
        File(app.getExternalFilesDir(null), "evidence/session-proof.json").writeText(dev.leemh.chalog.data.SessionCodec.encode(saved))
    }
    @Test fun warningLightsSurviveSaveReopenAndReport() {
        prepare(); tap("start"); await("test-lamps-on")
        tap("test-lamps-on")
        ui.waitUntil(10_000) { app.recorder.ui.value.session?.observedLamps()?.size == 2 }
        ui.waitForIdle() // The added lamp card changes the position of the off button.
        tap("test-lamps-off")
        ui.waitUntil(10_000) { app.store.latest()?.observedLamps()?.firstOrNull { it.type == "ENGINE" }?.stateLabel == "소등 관측" }
        ui.onNodeWithTag("stop").performClick(); await("details")
        val saved = app.store.latest()!!
        assertEquals(Phase.SAVED, saved.phase)
        assertEquals(saved, app.store.get(saved.id))
        assertEquals(mapOf("ENGINE" to "OFF", "TPMS" to "BLINK"), saved.observedLamps().associate { it.type to it.latest.value })
        ui.activityRule.scenario.recreate(); await("details")
        assertEquals(saved.id, app.recorder.ui.value.session!!.id)
        tap("details")
        ui.onNodeWithText("엔진 경고등").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("타이어 공기압 경고등").assertIsDisplayed()
        capture("08-warning-lights-test")
        assertTrue(ConsultationReport.text(saved).contains("소등 관측"))
        File(app.getExternalFilesDir(null), "evidence/warning-session-proof.json").writeText(dev.leemh.chalog.data.SessionCodec.encode(saved))
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
        prepare(); tap("start"); await("stop"); tap("motion")
        ui.waitUntil(10_000) { !app.recorder.ui.value.parked }
        ui.onNodeWithTag("stop").performClick(); await("details")
        ui.onNodeWithTag("details").assertIsNotEnabled()
        capture("07-driving-lock-test")
        tap("motion"); ui.onNodeWithTag("details").assertIsEnabled()
    }
    @Test fun recreationAndBackgroundKeepRecordingUntilNotificationStop() {
        prepare(); tap("start"); await("stop"); val id = app.store.latest()!!.id
        ui.activityRule.scenario.recreate(); await("stop")
        assertEquals(id, app.store.latest()!!.id)
        val before = app.store.latest()!!.observations.size
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(2200)
        assertEquals(Phase.RECORDING, app.store.latest()!!.phase)
        assertTrue(app.store.latest()!!.observations.size > before)
        assertEquals(id, app.store.latest()!!.id)
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        val ongoing = manager.activeNotifications.first { it.id == RecordingService.ONGOING_ID }
        assertTrue(ongoing.notification.extras.getString(android.app.Notification.EXTRA_TEXT)!!.contains("TEST"))
        ongoing.notification.actions.first { it.title.toString() == "지금이야" }.actionIntent.send()
        Thread.sleep(600)
        assertTrue(app.store.latest()!!.notes.any { it.bookmark })
        manager.activeNotifications.first { it.id == RecordingService.ONGOING_ID }.notification.actions
            .first { it.title.toString() == "종료·저장" }.actionIntent.send()
        Thread.sleep(600)
        assertEquals(Phase.SAVED, app.store.latest()!!.phase)
        val stopped = app.store.latest()!!.observations.size
        Thread.sleep(1200)
        assertEquals(stopped, app.store.latest()!!.observations.size)
        assertFalse(app.recorder.ui.value.backgroundRunning)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        await("details"); assertEquals(id, app.store.latest()!!.id)
        assertFalse(has("stop"))
    }
    @Test fun notificationStartsPreparedRecordInBackgroundOnlyOnce() {
        prepare()
        val count = app.savedRecordCount()
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(2200)
        assertEquals(Phase.ARMED, app.recorder.ui.value.phase)
        assertTrue(app.recorder.ui.value.bufferSeconds > 0)
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        val start = manager.activeNotifications.first { it.id == RecordingService.ONGOING_ID }
            .notification.actions.first { it.title.toString() == "기록 시작" }.actionIntent
        start.send(); start.send()
        Thread.sleep(800)
        assertEquals(Phase.RECORDING, app.store.latest()!!.phase)
        assertEquals(count + 1, app.savedRecordCount())
        val id = app.store.latest()!!.id
        assertTrue(app.store.latest()!!.observations.isNotEmpty())
        manager.activeNotifications.first { it.id == RecordingService.ONGOING_ID }.notification.actions
            .first { it.title.toString() == "종료·저장" }.actionIntent.send()
        Thread.sleep(800)
        assertEquals(id, app.store.latest()!!.id)
        assertEquals(Phase.SAVED, app.store.latest()!!.phase)
        assertFalse(app.recorder.ui.value.backgroundRunning)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        await("details")
    }
    @Test fun closingDashboardKeepsServiceAndReopeningKeepsId() {
        prepare(); tap("start"); await("stop")
        val id = app.store.latest()!!.id
        val before = app.store.latest()!!.observations.size
        tap("hide")
        Thread.sleep(1800)
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, ui.activityRule.scenario.state)
        assertEquals(Phase.RECORDING, app.store.latest()!!.phase)
        assertTrue(app.store.latest()!!.observations.size > before)
        androidx.test.core.app.ActivityScenario.launch<MainActivity>(android.content.Intent(app, MainActivity::class.java)).use {
            Thread.sleep(600)
            assertEquals(id, app.recorder.ui.value.session!!.id)
            assertEquals(Phase.RECORDING, app.recorder.ui.value.phase)
            val manager = app.getSystemService(android.app.NotificationManager::class.java)
            manager.activeNotifications.first { n -> n.id == RecordingService.ONGOING_ID }.notification.actions
                .first { a -> a.title.toString() == "종료·저장" }.actionIntent.send()
            Thread.sleep(600)
            assertEquals(Phase.SAVED, app.store.latest()!!.phase)
            assertEquals(id, app.store.latest()!!.id)
        }
    }
    @Test fun staleNotificationCannotStartTheNextPreparation() {
        prepare()
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        fun startAction() = manager.activeNotifications.first { it.id == RecordingService.ONGOING_ID }
            .notification.actions.first { it.title.toString() == "기록 시작" }.actionIntent
        ui.waitUntil(10_000) { runCatching { startAction(); true }.getOrDefault(false) }
        val stale = startAction()
        kotlinx.coroutines.runBlocking { app.recorder.finish().join() }
        ui.waitUntil(10_000) { !app.recorder.ui.value.backgroundRunning }
        kotlinx.coroutines.runBlocking { app.recorder.arm(true).join() }
        ui.waitUntil(10_000) { app.recorder.ui.value.backgroundRunning && app.recorder.ui.value.phase == Phase.ARMED }
        ui.waitUntil(10_000) { runCatching { startAction() != stale }.getOrDefault(false) }
        val current = startAction()
        assertNotEquals(stale, current)
        val count = app.savedRecordCount()
        stale.send(); Thread.sleep(400)
        assertEquals(Phase.ARMED, app.recorder.ui.value.phase)
        assertEquals(count, app.savedRecordCount())
        current.send()
        ui.waitUntil(10_000) { app.recorder.ui.value.phase == Phase.RECORDING }
        assertEquals(count + 1, app.savedRecordCount())
    }
    @Test fun rapidCommandsKeepTheSameSession() {
        prepare(); tap("start"); await("stop")
        val original = app.store.latest()!!
        val count = app.savedRecordCount()
        ui.activityRule.scenario.onActivity { activity ->
            val model = (activity.application as ChalogApp).recorder
            repeat(8) { model.start() }
            repeat(8) { model.finish() }
        }
        await("details")
        assertEquals(original.id, app.store.latest()!!.id)
        assertEquals(count, app.savedRecordCount())
        assertEquals(Phase.SAVED, app.store.latest()!!.phase)
    }
}
