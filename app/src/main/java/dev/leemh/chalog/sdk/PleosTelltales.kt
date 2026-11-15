package dev.leemh.chalog.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import ai.pleos.playground.vehicle.api.Safety
import dev.leemh.chalog.core.*

/** Public CAR_SAFETY snapshot getter only; PRIVATE_CAR_SAFETY subscription is not used. */
internal class PleosTelltales(private val context: Context, private val safety: () -> Safety?,
    private val emit: (Observation) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val sampler = TelltaleSampler()
    private var generation = 0
    private var capture = false
    private var nextPoll = 0L
    private var pending = false

    fun stopCapture() {
        generation++; capture = false; nextPoll = 0; pending = false; sampler.reset()
    }
    private fun missing() = emit(Observation(TELLTALE_READ, "", "snapshot", Origin.PLEOS_VEHICLE,
        SystemClock.elapsedRealtime(), System.currentTimeMillis(), false))

    fun poll(enabled: Boolean) {
        if (!enabled) { if (capture) stopCapture(); return }
        capture = true
        val now = SystemClock.elapsedRealtime()
        if (now < nextPoll) return
        if (pending) missing() // A query exceeding five seconds is an unconfirmed interval.
        val token = ++generation
        nextPoll = now + 5_000; pending = false
        if (ContextCompat.checkSelfPermission(context, "pleos.car.permission.CAR_SAFETY") != PackageManager.PERMISSION_GRANTED) {
            missing(); return
        }
        try {
            val api = safety() ?: run { missing(); return }
            pending = true
            api.getTelltalesInfo(onComplete = { snapshot -> main.post {
                if (capture && token == generation) {
                    pending = false
                    if (snapshot == null) missing()
                    else sampler.snapshot(snapshot.telltales.map { LampSample(it.type.name, it.state.name) },
                        Origin.PLEOS_VEHICLE, SystemClock.elapsedRealtime(), System.currentTimeMillis()).forEach(emit)
                }
            } }, onFailed = { main.post {
                if (capture && token == generation) { pending = false; missing() }
            } })
        } catch (_: Exception) { pending = false; missing() }
    }
}
