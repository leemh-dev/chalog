package dev.leemh.charok

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.leemh.charok.core.InputMode
import dev.leemh.charok.core.Origin
import dev.leemh.charok.core.Phase
import kotlinx.coroutines.*
import java.util.UUID

/** Explicit, user-started vehicle recording. Process death never resumes collection. */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val model get() = (application as CharokApp).recorder
    private val notifications get() = getSystemService(NotificationManager::class.java)
    private val token = UUID.randomUUID().toString()
    private var promoted = false
    private var previousPhase: Phase? = null
    private var previousNotes = 0

    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(NotificationChannel(PROGRESS, "기록 진행", NotificationManager.IMPORTANCE_LOW))
        notifications.createNotificationChannel(NotificationChannel(EVENTS, "기록 상태 변경", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "기록 시작, 순간 표시, 저장 결과를 짧게 알립니다"
            setSound(null, null)
            enableVibration(false)
        })
        try {
            ServiceCompat.startForeground(this, ONGOING_ID,
                notification(model.ui.value, "기록 준비를 시작합니다", ongoing = true),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
            promoted = true
        } catch (_: RuntimeException) {
            model.serviceStopped()
            stopSelf()
            return
        }
        scope.launch {
            model.ui.collect { state ->
                if (!state.backgroundRunning) return@collect
                val title = when (state.phase) {
                    Phase.ARMED -> "기록 준비 중"
                    Phase.RECORDING -> "증상을 기록하고 있어요"
                    Phase.SAVED -> "기록을 저장했어요"
                    Phase.INTERRUPTED -> "기록이 중단됐어요"
                    Phase.SAVE_FAILED -> "저장을 다시 확인해 주세요"
                    Phase.IDLE -> "기록 준비를 껐어요"
                }
                val active = state.phase in listOf(Phase.ARMED, Phase.RECORDING, Phase.SAVE_FAILED)
                val notes = state.session?.notes?.size ?: 0
                val changed = previousPhase != state.phase || previousNotes != notes
                if (changed) {
                    notifications.notify(ONGOING_ID, notification(state, title, ongoing = active))
                    val eventTitle = if (previousPhase == Phase.RECORDING && state.phase == Phase.RECORDING) {
                        if (state.session?.notes?.lastOrNull()?.bookmark == true) "방금 순간을 표시했어요" else "증상 설명을 남겼어요"
                    } else title
                    notifications.notify(EVENT_ID, notification(state, eventTitle, ongoing = false, event = true))
                }
                previousPhase = state.phase
                previousNotes = notes
                if (!active) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promoted || intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent.action == null) {
            model.serviceStarted()
        } else if (intent.getStringExtra("token") == token) {
            when (intent.action) {
                START -> model.start(source = if (model.ui.value.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL)
                MARK -> model.mark()
                STOP -> model.finish()
            }
        } else if (!model.ui.value.backgroundRunning) stopSelf()
        return START_NOT_STICKY
    }

    private fun action(name: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, RecordingService::class.java).setAction(name)
            .setData(Uri.parse("charok://recording/$token")).putExtra("token", token),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun notification(state: RecorderUi, title: String, ongoing: Boolean, event: Boolean = false): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val input = if (state.mode == InputMode.TEST) "TEST 입력 · " else ""
        val description = when (state.phase) {
            Phase.ARMED -> "최근 최대 60초를 준비합니다"
            Phase.RECORDING -> "내비를 보면서 계속 기록할 수 있어요"
            Phase.SAVE_FAILED -> "앱을 열어 같은 기록으로 다시 저장해 주세요"
            Phase.SAVED, Phase.INTERRUPTED -> "정차 후 앱에서 기록을 확인하세요"
            Phase.IDLE -> "차록을 열어 기록을 준비하세요"
        }
        val builder = NotificationCompat.Builder(this, if (event) EVENTS else PROGRESS)
            .setSmallIcon(R.drawable.ic_record_notification)
            .setContentTitle(title)
            .setContentText(input + description)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(!event)
        if (event) builder.setTimeoutAfter(8_000)
        if (ongoing && !event) {
            when (state.phase) {
                Phase.ARMED -> builder.addAction(0, "기록 시작", action(START, 1)).addAction(0, "준비 끄기", action(STOP, 3))
                Phase.RECORDING -> builder.addAction(0, "지금이야", action(MARK, 2)).addAction(0, "종료·저장", action(STOP, 3))
                else -> Unit
            }
        }
        return builder.build()
    }

    override fun onDestroy() {
        scope.cancel()
        model.serviceStopped()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PROGRESS = "recording_progress"
        const val EVENTS = "recording_events"
        const val ONGOING_ID = 41
        const val EVENT_ID = 42
        private const val START = "dev.leemh.charok.START"
        private const val MARK = "dev.leemh.charok.MARK"
        private const val STOP = "dev.leemh.charok.STOP"
    }
}
