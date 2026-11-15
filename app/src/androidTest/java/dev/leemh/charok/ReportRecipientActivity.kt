package dev.leemh.charok

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.security.MessageDigest

/** Separate test-APK UID; no production component or actual service-center delivery. */
class ReportRecipientActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val result = Intent("dev.leemh.charok.TEST_REPORT_RECEIVED").setPackage("dev.leemh.charok")
        try {
            val data = contentResolver.openInputStream(uri!!)!!.use { it.readBytes() }
            result.putExtra("sha256", MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) })
            result.putExtra("containsTest", data.toString(Charsets.UTF_8).contains("TEST"))
            val sibling = uri.buildUpon().path(uri.path!!.substringBeforeLast('/') + "/unselected-scope-test.txt").build()
            val denied = try { contentResolver.openInputStream(sibling)?.close(); false } catch (_: SecurityException) { true }
            result.putExtra("siblingDenied", denied)
        } catch (_: Exception) { result.putExtra("readFailed", true) }
        sendBroadcast(result); finish()
    }
}
