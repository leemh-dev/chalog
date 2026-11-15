package dev.leemh.chalog.sdk

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import dev.leemh.chalog.core.*

/** Recommended GEAR_SELECTION path; unknown/unavailable is never parked. No writes. */
class AaosMotion(private val context: Context, private val emit: (Observation) -> Unit) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var active = false
    fun start() {
        active = true
        if (Build.VERSION.SDK_INT < 29) return
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!active) { connected.disconnect(); return@createCar }
                manager = if (ready) connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager else null
                if (!ready) { missing("gear"); missing("speed") }
            }
        } catch (_: Exception) { missing("gear"); missing("speed") }
        catch (_: LinkageError) { missing("gear"); missing("speed") }
    }
    private fun missing(field: String) { if (active) emit(Observation(field, "", "", Origin.AAOS_PROPERTY, SystemClock.elapsedRealtime(), System.currentTimeMillis(), false)) }
    fun poll() {
        if (!active) return
        val m = manager ?: return
        for ((id, field) in listOf(VehiclePropertyIds.GEAR_SELECTION to "gear", VehiclePropertyIds.PERF_VEHICLE_SPEED to "speed")) {
            try {
                val value = m.getProperty<Any>(id, 0)
                val number = value.value as? Number
                if (value.status != CarPropertyValue.STATUS_AVAILABLE || number == null ||
                    (field == "gear" && number.toInt() == 0) || !number.toDouble().isFinite()) { missing(field); continue }
                val text = if (field == "gear") if (number.toInt() == 4) "PARK" else "GEAR_${number.toInt()}" else number.toDouble().toString()
                if (active) emit(Observation(field, text, if (field == "speed") "m/s" else "enum", Origin.AAOS_PROPERTY,
                    SystemClock.elapsedRealtime(), System.currentTimeMillis()))
            } catch (_: Exception) { missing(field) }
        }
    }
    fun close() { active = false; manager = null; runCatching { car?.disconnect() }; car = null }
}
