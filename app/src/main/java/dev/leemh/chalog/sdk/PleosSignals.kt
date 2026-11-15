package dev.leemh.chalog.sdk

import android.content.Context
import android.os.SystemClock
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import dev.leemh.chalog.core.*

/** Read-only public API. One-second reads are sampled observations, not a continuous ECU log. */
class PleosSignals(private val context: Context, private val emit: (Observation) -> Unit, private val status: (String) -> Unit) {
    private var vehicle: Vehicle? = null
    private val telltales = PleosTelltales(context, { vehicle?.getSafety() }, emit)
    private var motion: AaosMotion? = null
    @Volatile private var active = false
    @Volatile private var steering = false
    @Volatile private var brake = false
    @Volatile private var seats: List<SeatArea> = emptyList()
    private var location: FusedLocation? = null
    private var locationActive = false
    private val locationListener = object : FusedLocationListener {
        override fun onChanged(value: LocationData?) {
            if (!active || !locationActive) return
            if (value == null) { data("location", "", "", false, Origin.PLEOS_LOCATION); return }
            // Preserve SDK confidence without inventing metre accuracy or road names.
            data("location", "${value.latitude},${value.longitude}; confidence=${value.confidence}", "WGS84", true, Origin.PLEOS_LOCATION, value.timestampMillis)
        }
        override fun onError(exception: Exception) { if (active && locationActive) data("location", "", "", false, Origin.PLEOS_LOCATION) }
    }
    private fun data(field: String, value: String, unit: String, available: Boolean = true, origin: Origin = Origin.PLEOS_VEHICLE, sampleAt: Long? = null) {
        if (active) emit(Observation(field, value, unit, origin, SystemClock.elapsedRealtime(), System.currentTimeMillis(), available, sampleAt))
    }
    fun start() {
        if (active) return
        active = true
        try { motion = AaosMotion(context, emit).also { it.start() } }
        catch (_: LinkageError) { status("AAOS 차량 상태 서비스 미지원") }
        try {
            val v = Vehicle(context).also { it.initialize() }; vehicle = v
            v.getSteeringWheel().checkSteeringWheelAngleCapabilities(onComplete = { config, _ -> steering = active && config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) }, onFailed = { steering = false })
            v.getBrake().checkBrakePedalStatusCapabilities(onComplete = { config -> brake = active && config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) }, onFailed = { brake = false })
            v.getHVAC().checkHvacAcCapabilities(onComplete = { config, areas -> seats = if (active && config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE)) areas.take(4) else emptyList() }, onFailed = { seats = emptyList() })
            status("Vehicle 초기화 요청 · 실제 수신 대기")
        } catch (_: Exception) { status("Vehicle 연결/권한 확인 필요") }
        catch (_: LinkageError) { status("이 환경에 Pleos 차량 서비스가 없습니다") }
    }
    fun poll(capture: Boolean) {
        if (!active) return
        motion?.poll()
        telltales.poll(capture)
        val v = vehicle ?: return
        try {
            if (!capture) return
            if (steering) v.getSteeringWheel().getCurrentSteeringWheelAngle(onComplete = { data("steering", it?.toString().orEmpty(), "degree", it != null) }, onFailed = { data("steering", "", "degree", false) })
            else data("steering", "", "degree", false)
            if (brake) v.getBrake().getCurrentBrakePedalStatus(onComplete = { data("brake", it?.toString().orEmpty(), "boolean", it != null) }, onFailed = { data("brake", "", "boolean", false) })
            else data("brake", "", "boolean", false)
            if (seats.isEmpty()) data("ac", "", "boolean", false)
            seats.forEach { seat -> v.getHVAC().getHvacAcState(seat, onComplete = { data("ac:${seat.values.joinToString { it.name }}", it?.toString().orEmpty(), "boolean", it != null) }, onFailed = { data("ac", "", "boolean", false) }) }
        } catch (_: Exception) { data("vehicle", "", "", false); status("Vehicle 호출 실패 · 일부 관측 미수신") }
    }
    fun setLocation(enabled: Boolean) {
        if (!enabled) {
            locationActive = false
            runCatching { location?.unregisterFusedLocationCallback(locationListener); location?.release() }; location = null
            return
        }
        if (locationActive || !active) return
        try {
            val f = FusedLocation(context)
            if (!f.isSupported()) { status("위치 서비스 미지원"); return }
            location = f; locationActive = true; f.initialize(); f.registerFusedLocationCallback(locationListener)
        } catch (_: Exception) { setLocation(false); status("위치 연결/권한 확인 필요") }
    }
    fun close() {
        telltales.stopCapture()
        active = false; motion?.close(); motion = null; setLocation(false); steering = false; brake = false; seats = emptyList()
        runCatching { vehicle?.release() }; vehicle = null
    }
    fun resetTelltales() = telltales.stopCapture()
}
