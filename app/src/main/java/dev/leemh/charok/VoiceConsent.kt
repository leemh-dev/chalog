package dev.leemh.charok

import android.content.Context

/** Consent persists, collection does not. TEST consent is deliberately process-local. */
class VoiceConsent(context: Context) {
    private val preferences = context.getSharedPreferences("voice_recording", Context.MODE_PRIVATE)
    var realEnabled: Boolean
        get() = preferences.getInt("consent_version", 0) == VERSION
        set(value) { check(preferences.edit().putInt("consent_version", if (value) VERSION else 0).commit()) }
    var testEnabled = false
    companion object { private const val VERSION = 1 }
}
