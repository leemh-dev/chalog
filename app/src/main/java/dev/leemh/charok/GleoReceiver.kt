package dev.leemh.charok

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.leemh.charok.core.GleoCommand
import dev.leemh.charok.core.GleoContract
import org.json.JSONObject

/** Official component=service transport. This component never starts an Activity. */
class GleoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(GleoContract.CONTROL, GleoContract.ADD)) return
        val command = try {
            GleoContract.parse(action, GleoIntents.parameters(intent))
        } catch (_: Exception) {
            GleoResponse.send(context, action, "fail", "The command parameters could not be read.")
            return
        }
        if (command is GleoCommand.Open) return
        val pending = goAsync()
        (context.applicationContext as CharokApp).recorder.gleo(command) { status, message ->
            try { GleoResponse.send(context, action, status, message) }
            finally { pending.finish() }
        }
    }
}

object GleoIntents {
    fun parameters(intent: Intent): String {
        if (intent.hasExtra("ACTION_PARAM")) return requireNotNull(intent.getStringExtra("ACTION_PARAM"))
        val parameters = JSONObject()
        // The guide also describes separate typed extras; never read consent, input mode or location flags.
        for (key in listOf("operation", "note", "markMoment")) if (intent.hasExtra(key)) {
            @Suppress("DEPRECATION") val value = intent.extras?.get(key)
            require(value is String || value is Boolean)
            parameters.put(key, value)
        }
        return parameters.toString()
    }
}

object GleoResponse {
    fun send(context: Context, action: String, status: String, message: String) {
        val response = JSONObject().put("functionName", action).put("status", status)
            .put("message", message).put("mode", if (status == "success") "beep" else "voice")
        // The public contract specifies this broadcast; no symptom, ID, location or record is returned.
        context.sendBroadcast(Intent(GleoContract.RESPONSE).putExtra("packageName", context.packageName)
            .putExtra("response", response.toString()))
    }
}
