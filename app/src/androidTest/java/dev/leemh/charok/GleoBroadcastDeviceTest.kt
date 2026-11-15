package dev.leemh.charok

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dev.leemh.charok.core.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Real Android broadcast/service/SQLite tests with explicitly selected TEST input. Not Gleo speech. */
@RunWith(AndroidJUnit4::class)
class GleoBroadcastDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<CharokApp>()
    private val replies = LinkedBlockingQueue<JSONObject>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("packageName") == context.packageName)
                replies.put(JSONObject(intent.getStringExtra("response")!!))
        }
    }
    @Before fun registerReplies() {
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(app.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        ContextCompat.registerReceiver(app, receiver, IntentFilter(GleoContract.RESPONSE), ContextCompat.RECEIVER_EXPORTED)
    }
    @After fun stopWithoutDeletingRecords() {
        runBlocking {
            app.recorder.gleo(GleoCommand.Stop) { _, _ -> }.join()
            if (app.recorder.ui.value.phase == Phase.SAVE_FAILED) app.recorder.retry().join()
            if (app.recorder.ui.value.mode == InputMode.TEST && !app.recorder.ui.value.parked) app.recorder.toggleTestMotion().join()
        }
        waitFor { !app.recorder.ui.value.backgroundRunning }
        app.unregisterReceiver(receiver)
    }
    private fun waitFor(test: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 15_000
        while (!test() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
        assertTrue("Timed out waiting for the expected recorder state", test())
    }
    private fun tap(tag: String) {
        val node = ui.onNodeWithTag(tag)
        if (tag == "dismiss-panel") node.performClick() else node.performScrollTo().performClick()
        ui.waitForIdle()
    }
    private fun selectTestInput() {
        waitFor { app.recorder.ui.value.ready }
        runBlocking {
            app.recorder.finish().join()
            app.recorder.closeRecord().join()
            app.recorder.chooseMode(InputMode.TEST).join()
        }
        // onStart/foreground runs asynchronously; unknown is not TEST driving.
        waitFor { app.recorder.ui.value.gear in listOf("PARK", "DRIVE") }
        if (app.recorder.ui.value.gear == "DRIVE") runBlocking { app.recorder.toggleTestMotion().join() }
        waitFor { app.recorder.ui.value.parked }
        runBlocking { app.recorder.configureVoice(false).join() }
        waitFor { app.recorder.ui.value.parked && !app.recorder.ui.value.voiceEnabled }
        ui.waitForIdle()
    }
    private fun setupVoice() {
        selectTestInput(); tap("consent"); tap("voice-setup")
        waitFor { app.recorder.ui.value.voiceEnabled }
        assertEquals(Phase.IDLE, app.recorder.ui.value.phase)
        assertFalse(app.recorder.ui.value.backgroundRunning)
    }
    private fun send(action: String, parameters: String) {
        app.sendBroadcast(Intent(action).setPackage(app.packageName).putExtra("ACTION_PARAM", parameters))
    }
    private fun reply(status: String) {
        val response = replies.poll(7, TimeUnit.SECONDS) ?: error("No command callback")
        assertEquals(response.toString(), status, response.getString("status"))
        assertFalse(response.has("data"))
    }
    private fun assertNoPanel() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        assertTrue(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).none { it is MainActivity })
    }
    private fun capture(name: String) {
        val dir = File(app.filesDir, "evidence").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No actual device screenshot")
        File(dir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun waitForEvent(title: String) {
        waitFor { app.getSystemService(NotificationManager::class.java).activeNotifications.any {
            it.id == RecordingService.EVENT_ID && it.notification.extras.getString(android.app.Notification.EXTRA_TITLE) == title
        } }
        Thread.sleep(250) // Let the normal SystemUI text toast render after the status notification.
    }
    @Test fun broadcastStartsWithoutPreparationAndStopsWithoutOpeningPanel() {
        setupVoice()
        runBlocking { app.recorder.toggleTestMotion().join() } // Explicit TEST driving, not a vehicle-state bypass.
        tap("dismiss-panel"); waitFor { ui.activityRule.scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        val count = app.savedRecordCount()
        app.sendBroadcast(Intent(GleoContract.CONTROL).setPackage(app.packageName).putExtra("operation", "start"))
        send(GleoContract.CONTROL, """{"operation":"start"}""")
        reply("success"); reply("success")
        waitFor { app.recorder.ui.value.phase == Phase.RECORDING }
        val started = app.store.latest()!!
        assertEquals(count + 1, app.savedRecordCount())
        assertTrue(started.observations.none { it.elapsed < started.triggerElapsed })
        assertEquals(InputMode.TEST, started.mode); assertFalse(app.recorder.ui.value.parked)
        assertNoPanel()
        waitForEvent("증상을 기록하고 있어요")
        capture("gleo-test-background-recording.png")
        val observations = started.observations.size
        waitFor { app.store.get(started.id)!!.observations.size > observations }
        send(GleoContract.ADD, """{"note":"회전할 때 덜컥 소리가 나요"}"""); reply("success")
        send(GleoContract.ADD, """{"markMoment":true}"""); reply("success")
        send(GleoContract.CONTROL, """{"operation":"stop"}"""); reply("success")
        send(GleoContract.CONTROL, """{"operation":"stop"}"""); reply("success")
        waitFor { !app.recorder.ui.value.backgroundRunning }
        val saved = app.store.get(started.id)!!
        assertEquals(Phase.SAVED, saved.phase); assertEquals(2, saved.notes.size)
        assertEquals(1, saved.notes.count { it.bookmark }); assertTrue(saved.notes.all { it.origin == Origin.TEST })
        assertEquals(count + 1, app.savedRecordCount()); assertNoPanel()
        waitForEvent("기록을 저장했어요")
        capture("gleo-test-background-saved.png")
        Thread.sleep(1200); assertEquals(saved, app.store.get(started.id))
        val notifications = app.getSystemService(NotificationManager::class.java)
        assertFalse(notifications.activeNotifications.any { it.id == RecordingService.ONGOING_ID })
        waitFor { notifications.activeNotifications.none { it.id == RecordingService.EVENT_ID } }
        ActivityScenario.launch<MainActivity>(Intent(GleoContract.OPEN).setPackage(app.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use {
            reply("success"); waitFor { app.recorder.ui.value.session?.id == saved.id }
            assertEquals(count + 1, app.savedRecordCount()); assertEquals(Phase.SAVED, app.recorder.ui.value.phase)
            assertFalse(app.recorder.ui.value.backgroundRunning)
            capture("gleo-test-explicit-open.png")
        }
    }
    @Test fun broadcastRejectsMissingConsentAndMalformedParameters() {
        selectTestInput(); val count = app.savedRecordCount()
        tap("dismiss-panel")
        send(GleoContract.CONTROL, """{"operation":"start","consent":true,"mode":"TEST"}"""); reply("fail")
        send(GleoContract.CONTROL, """{"operation":"delete"}"""); reply("fail")
        send(GleoContract.ADD, """{"markMoment":"true"}"""); reply("fail")
        send(GleoContract.ADD, """{"note":"inactive record"}"""); reply("fail")
        assertEquals(count, app.savedRecordCount()); assertFalse(app.recorder.ui.value.backgroundRunning); assertNoPanel()
    }
    @Test fun broadcastDoesNotReplaceFailedRecordAndRetryKeepsId() {
        setupVoice(); tap("dismiss-panel")
        send(GleoContract.CONTROL, """{"operation":"start"}"""); reply("success")
        val id = app.store.latest()!!.id; val count = app.savedRecordCount()
        runBlocking { app.recorder.injectFailure().join() }
        assertEquals(Phase.SAVE_FAILED, app.recorder.ui.value.phase)
        send(GleoContract.CONTROL, """{"operation":"stop"}"""); reply("fail")
        send(GleoContract.CONTROL, """{"operation":"start"}"""); reply("fail")
        assertEquals(id, app.recorder.ui.value.session!!.id); assertEquals(count, app.savedRecordCount())
        runBlocking { app.recorder.retry().join() }
        waitFor { !app.recorder.ui.value.backgroundRunning }
        assertEquals(Phase.SAVED, app.store.get(id)!!.phase); assertEquals(count, app.savedRecordCount()); assertNoPanel()
    }
    @Test fun selectedReportCanBeReadByGrantedExternalTestRecipientOnly() {
        setupVoice()
        send(GleoContract.CONTROL, """{"operation":"start"}"""); reply("success")
        send(GleoContract.ADD, """{"note":"공유 권한 검증용 TEST 설명"}"""); reply("success")
        send(GleoContract.CONTROL, """{"operation":"stop"}"""); reply("success")
        waitFor { !app.recorder.ui.value.backgroundRunning }
        var report: File? = null
        runBlocking { app.recorder.export(false) { report = it }.join() }
        val file = report!!; val content = file.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
        File(file.parentFile, "unselected-scope-test.txt").writeText("Unselected TEST fixture")
        val received = LinkedBlockingQueue<Intent>()
        val result = object : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { received.put(intent) } }
        ContextCompat.registerReceiver(app, result, IntentFilter("dev.leemh.charok.TEST_REPORT_RECEIVED"), ContextCompat.RECEIVER_EXPORTED)
        try {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.reports", file)
            ui.activityRule.scenario.onActivity { activity ->
                activity.startActivity(Intent(Intent.ACTION_SEND).setClassName("dev.leemh.charok.test", ReportRecipientActivity::class.java.name)
                    .setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
            val proof = received.poll(10, TimeUnit.SECONDS) ?: error("Test recipient did not respond")
            assertEquals(digest, proof.getStringExtra("sha256"))
            assertTrue(proof.getBooleanExtra("siblingDenied", false))
            assertTrue(proof.getBooleanExtra("containsTest", false))
            assertNotEquals(android.os.Process.myUid(), proof.getIntExtra("senderUid", -1))
        } finally { app.unregisterReceiver(result) }
    }
    @Test fun unprivilegedExternalAppCannotStartRecording() {
        setupVoice(); val count = app.savedRecordCount()
        val received = LinkedBlockingQueue<Intent>()
        val result = object : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { received.put(intent) } }
        ContextCompat.registerReceiver(app, result, IntentFilter("dev.leemh.charok.TEST_UNTRUSTED_PROBE_FINISHED"), ContextCompat.RECEIVER_EXPORTED)
        try {
            ui.activityRule.scenario.onActivity { activity ->
                activity.startActivity(Intent("dev.leemh.charok.test.PROBE_COMMAND")
                    .setClassName("dev.leemh.charok.test", ReportRecipientActivity::class.java.name))
            }
            val proof = received.poll(10, TimeUnit.SECONDS) ?: error("External command probe did not run")
            assertNotEquals(android.os.Process.myUid(), proof.getIntExtra("senderUid", -1))
            Thread.sleep(1000)
            assertEquals(count, app.savedRecordCount())
            assertFalse(app.recorder.ui.value.backgroundRunning)
            assertEquals(Phase.IDLE, app.recorder.ui.value.phase)
            assertTrue(replies.isEmpty()) // Android rejects the sender before the app's receiver executes.
        } finally { app.unregisterReceiver(result) }
    }
    @Test fun fileSaveCancellationOrMissingPickerShowsFeedbackWithoutLosingRecord() {
        setupVoice()
        send(GleoContract.CONTROL, """{"operation":"start"}"""); reply("success")
        send(GleoContract.CONTROL, """{"operation":"stop"}"""); reply("success")
        waitFor { !app.recorder.ui.value.backgroundRunning }
        val saved = app.store.latest()!!; val count = app.savedRecordCount()
        tap("details"); tap("보고서 내보내기"); tap("save-report")
        fun hasFeedback() = app.recorder.ui.value.reportStatus?.let {
            it.contains("파일을 저장하지 않았습니다") || it.contains("저장 화면을 열 수 없습니다")
        } == true
        fun panelResumed(): Boolean {
            var resumed = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is MainActivity }
            }
            return resumed
        }
        waitFor { hasFeedback() || !panelResumed() }
        if (!hasFeedback()) {
            // Dismiss the normal document picker without selecting/writing any user file.
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
        }
        waitFor { hasFeedback() && panelResumed() }
        ui.onNodeWithTag("report-status").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("report-status").assertTextContains("기존 기록은 보존됐습니다", substring = true)
        assertEquals(saved, app.store.get(saved.id)); assertEquals(count, app.savedRecordCount())
        capture("report-save-feedback.png")
    }
}
