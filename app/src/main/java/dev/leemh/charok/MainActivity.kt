package dev.leemh.charok

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider

class MainActivity : ComponentActivity() {
    private val model get() = (application as CharokApp).recorder
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.arm(true) else model.notificationDenied()
    }
    private fun prepareRecording() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else model.arm(true)
    }
    private val carPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Denial remains unknown/locked; never substitute a simulated parked state.
        model.chooseMode(dev.leemh.charok.core.InputMode.PLEOS)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CharokScreen(model, prepare = ::prepareRecording, hide = { moveTaskToBack(true) }, connectPleos = { carPermission.launch("android.car.permission.CAR_SPEED") }) { file ->
            val uri = FileProvider.getUriForFile(this, "$packageName.reports", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { startActivity(Intent.createChooser(share, "선택한 차록 기록 공유")) }
        } }
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { if (!isChangingConfigurations) model.background(); super.onStop() }
}
