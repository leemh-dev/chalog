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
import androidx.lifecycle.lifecycleScope
import dev.leemh.charok.core.GleoContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class MainActivity : ComponentActivity() {
    private val model get() = (application as CharokApp).recorder
    private var prepareBuffer = true
    private var pendingReport: File? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) completeSetup() else model.notificationDenied()
    }
    private fun completeSetup() { if (prepareBuffer) model.arm(true) else model.configureVoice(true) }
    private fun prepareRecording(buffer: Boolean) {
        prepareBuffer = buffer
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else completeSetup()
    }
    private val saveReport = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val source = pendingReport; pendingReport = null
        if (uri == null) model.reportDelivery("파일을 저장하지 않았습니다. 기존 기록은 보존됐습니다.")
        if (uri != null && source != null) lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri, "w")!!.use { out -> source.inputStream().use { it.copyTo(out) } }
                }.isSuccess
            }
            model.reportDelivery(if (saved) "선택한 위치에 보고서를 저장했습니다" else "파일을 저장하지 못했습니다. 기존 기록은 보존됐습니다")
        }
    }
    private val carPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Denial remains unknown/locked; never substitute a simulated parked state.
        model.chooseMode(dev.leemh.charok.core.InputMode.PLEOS)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareBuffer = savedInstanceState?.getBoolean("prepareBuffer", true) ?: true
        savedInstanceState?.getString("pendingReport")?.takeIf { it == File(it).name }?.let { name ->
            pendingReport = File(File(cacheDir, "reports"), name).takeIf { it.isFile }
        }
        setContent { CharokScreen(model, prepare = { prepareRecording(true) }, setupVoice = { prepareRecording(false) },
            hide = { finish() }, connectPleos = { carPermission.launch("android.car.permission.CAR_SPEED") },
            saveReport = { file ->
                pendingReport = file
                runCatching { saveReport.launch(file.name) }.onFailure {
                    pendingReport = null
                    model.reportDelivery("파일 저장 화면을 열 수 없습니다. 공유 앱을 설치하거나 기록을 앱에 보관하세요.")
                }
            }) { file ->
            val uri = FileProvider.getUriForFile(this, "$packageName.reports", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (packageManager.queryIntentActivities(share, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                model.reportDelivery("공유할 앱이 없습니다. ‘파일로 저장’으로 보고서를 보관하세요.")
            } else runCatching { startActivity(Intent.createChooser(share, "선택한 차록 기록 공유")) }
                .onFailure { model.reportDelivery("공유 앱을 열지 못했습니다. ‘파일로 저장’을 이용하세요.") }
        } }
        if (savedInstanceState == null) respondToOpen(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); respondToOpen(intent) }
    private fun respondToOpen(intent: Intent) {
        if (intent.action != GleoContract.OPEN) return
        lifecycleScope.launch {
            val ready = runCatching {
                GleoContract.parse(intent.action, GleoIntents.parameters(intent))
                withTimeout(5000) { model.ui.first { it.ready } }
            }.isSuccess
            GleoResponse.send(this@MainActivity, GleoContract.OPEN, if (ready) "success" else "fail",
                if (ready) "The Chalog panel is open. Recording has not been started by this action." else "The Chalog panel could not load the saved record.")
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("prepareBuffer", prepareBuffer)
        pendingReport?.let { outState.putString("pendingReport", it.name) }
        super.onSaveInstanceState(outState)
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { if (!isChangingConfigurations) model.background(); super.onStop() }
}
