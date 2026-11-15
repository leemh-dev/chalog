package dev.leemh.charok

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.json.JSONObject

/** Transparent command entry point: a Gleo delivery never opens the recorder dashboard. */
class GleoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent) else finish()
    }
    private fun handle(intent: Intent) {
        val action = intent.action ?: run { finish(); return }
        if (action !in setOf("START_SYMPTOM_RECORD", "ADD_SYMPTOM_NOTE", "END_SYMPTOM_RECORD")) { finish(); return }
        try {
            val raw = intent.getStringExtra("ACTION_PARAM") ?: "{}"
            require(raw.length <= 8192)
            val parsed = JSONObject(raw)
            val note = (intent.getStringExtra("note") ?: parsed.optString("note", "")).take(1000)
            val mark = intent.getBooleanExtra("markMoment", parsed.optBoolean("markMoment", false))
            (application as CharokApp).recorder.gleo(action, note, mark) { status, message -> respond(action, status, message) }
        } catch (_: Exception) { respond(action, "fail", "The command parameters could not be read.") }
    }
    private fun respond(action: String, status: String, message: String) {
        val response = JSONObject().put("functionName", action).put("status", status).put("message", message).put("mode", "voice")
        // No record, location or symptom text is included in the callback.
        sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")
            .putExtra("packageName", packageName).putExtra("response", response.toString()))
        runOnUiThread { finish() }
    }
}
