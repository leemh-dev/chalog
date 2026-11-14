package dev.leemh.charok

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val model: RecorderModel by viewModels()
    private val carPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Denial remains unknown/locked; never substitute a simulated parked state.
        model.chooseMode(dev.leemh.charok.core.InputMode.PLEOS)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CharokScreen(model, connectPleos = { carPermission.launch("android.car.permission.CAR_SPEED") }) { file ->
            val uri = FileProvider.getUriForFile(this, "$packageName.reports", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { startActivity(Intent.createChooser(share, "선택한 차록 기록 공유")) }
        } }
        if (savedInstanceState == null) window.decorView.post { handle(intent) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { if (!isChangingConfigurations) model.background(); super.onStop() }
    private fun handle(intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf("START_SYMPTOM_RECORD", "ADD_SYMPTOM_NOTE", "END_SYMPTOM_RECORD")) return
        // ACTION_PARAM JSON and individual extras are both documented. Do not log symptom text.
        try {
            val raw = intent.getStringExtra("ACTION_PARAM") ?: "{}"
            require(raw.length <= 8192)
            val parsed = JSONObject(raw)
            val note = (intent.getStringExtra("note") ?: parsed.optString("note", "")).take(1000)
            val mark = intent.getBooleanExtra("markMoment", parsed.optBoolean("markMoment", false))
            model.gleo(action, note, mark) { status, message -> respond(action, status, message) }
        } catch (_: Exception) { respond(action, "fail", "The command parameters could not be read.") }
        intent.action = Intent.ACTION_MAIN // Do not replay a delivered action on activity recreation.
    }
    private fun respond(action: String, status: String, message: String) {
        val response = JSONObject().put("functionName", action).put("status", status).put("message", message).put("mode", "voice")
        // No private record, location or symptom text is broadcast in the callback.
        sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").putExtra("packageName", packageName).putExtra("response", response.toString()))
    }
}
