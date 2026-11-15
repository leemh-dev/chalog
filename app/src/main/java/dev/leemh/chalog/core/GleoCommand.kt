package dev.leemh.chalog.core

import org.json.JSONObject

/** Only these three capabilities are exposed to Gleo. Parameters cannot grant consent or change inputs. */
sealed interface GleoCommand {
    data class Start(val note: String = "") : GleoCommand
    data object Stop : GleoCommand
    data class Add(val note: String, val mark: Boolean) : GleoCommand
    data object Open : GleoCommand
}

object GleoContract {
    const val CONTROL = "CONTROL_SYMPTOM_RECORD"
    const val ADD = "ADD_SYMPTOM_EVENT"
    const val OPEN = "OPEN_CHALOG_RECORDS"
    const val LEGACY_OPEN = "OPEN_CHAROK_RECORDS"
    fun isOpen(action: String?) = action == OPEN || action == LEGACY_OPEN
    const val RESPONSE = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"
    fun parse(action: String?, raw: String): GleoCommand {
        require(raw.length <= 8192) { "Command is too large." }
        val value = JSONObject(raw)
        fun text(name: String): String {
            if (!value.has(name)) return ""
            require(value.get(name) is String) { "Invalid text parameter." }
            return value.getString(name).also { require(it.length <= 1000) }
        }
        return when (action) {
            CONTROL -> when (text("operation")) {
                "start" -> GleoCommand.Start(text("note"))
                "stop" -> GleoCommand.Stop
                else -> error("The recording operation must be start or stop.")
            }
            ADD -> {
                require(!value.has("markMoment") || value.get("markMoment") is Boolean)
                val mark = value.optBoolean("markMoment", false)
                val note = text("note")
                require(mark || note.isNotBlank()) { "A description or moment marker is required." }
                GleoCommand.Add(note, mark)
            }
            OPEN, LEGACY_OPEN -> GleoCommand.Open
            else -> error("Unsupported command.")
        }
    }
}
