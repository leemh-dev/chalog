package dev.leemh.charok.core

const val TELLTALE_READ = "telltales:read"
private const val LAMP_PREFIX = "telltale:"

data class LampSample(val type: String, val state: String)

/** A missing item or failed read is never an OFF observation. Repeated OFFs need no storage. */
class TelltaleSampler {
    private var previous = emptyMap<String, String>()
    fun reset() { previous = emptyMap() }
    fun snapshot(samples: List<LampSample>, origin: Origin, elapsed: Long, at: Long): List<Observation> {
        val current = samples.associate { it.type to it.state }
        val result = mutableListOf(Observation(TELLTALE_READ, "RECEIVED", "snapshot", origin, elapsed, at))
        current.forEach { (type, state) ->
            if (state != "OFF" || previous[type] in listOf("ON", "BLINK")) {
                result += Observation(LAMP_PREFIX + type, state, "state", origin, elapsed, at,
                    state in listOf("ON", "BLINK", "OFF"))
            }
        }
        previous.filter { (type, state) -> type !in current && state in listOf("ON", "BLINK") }.forEach { (type, _) ->
            result += Observation(LAMP_PREFIX + type, "", "state", origin, elapsed, at, false)
        }
        previous = current
        return result
    }
}

data class ObservedLamp(val type: String, val firstLit: Observation, val latest: Observation) {
    val label get() = lampName(type)
    val stateLabel get() = when {
        !latest.available -> "이후 상태 미확인"
        latest.value == "OFF" -> "소등 관측"
        latest.value == "BLINK" -> "점멸 관측"
        latest.value == "ON" -> "점등 관측"
        else -> "이후 상태 미확인"
    }
}

/** Historical observations, not a dashboard's current warning state or a diagnosis. */
fun Session.observedLamps(): List<ObservedLamp> {
    val sourceAllowed: (Observation) -> Boolean = { (it.origin == Origin.TEST) == (mode == InputMode.TEST) }
    val read = observations.lastOrNull { it.field == TELLTALE_READ && sourceAllowed(it) }
    return observations.filter { it.field.startsWith(LAMP_PREFIX) && sourceAllowed(it) }
        .groupBy { it.field.removePrefix(LAMP_PREFIX) }.mapNotNull { (type, rows) ->
            val first = rows.firstOrNull { it.available && it.value in listOf("ON", "BLINK") } ?: return@mapNotNull null
            val last = rows.last()
            val latest = if (read != null && !read.available && read.elapsed >= last.elapsed) read else last
            ObservedLamp(type, first, latest)
        }.sortedBy { it.firstLit.elapsed }
}

fun Session.lampCoverage(): String {
    val read = observations.lastOrNull { it.field == TELLTALE_READ && (it.origin == Origin.TEST) == (mode == InputMode.TEST) }
        ?: return "경고등 데이터 없음"
    return if (read.available) "수신 항목의 점등 기록 없음" else "경고등 수신 불가"
}

fun lampName(type: String): String = when (type) {
    "ENGINE" -> "엔진 경고등"
    "TPMS" -> "타이어 공기압 경고등"
    "ENGINE_OIL_PRESSURE" -> "엔진 오일 압력 경고등"
    "ENGINE_TEMPERATURE" -> "엔진 온도 경고등"
    "LOW_VOLT_BATTERY" -> "저전압 배터리 경고등"
    "ABS" -> "ABS 경고등"
    "MDPS" -> "전동식 조향 경고등"
    "AIRBAG" -> "에어백 경고등"
    "MASTER_WARNING" -> "통합 경고등"
    "EV_SYSTEM" -> "전기차 시스템 경고등"
    "HEV_SYSTEM" -> "하이브리드 시스템 경고등"
    "POWER_DOWN" -> "출력 제한 표시등"
    "RBS" -> "회생 제동 표시등"
    "LOW_EV_BATTERY" -> "구동 배터리 잔량 표시등"
    "LOW_FUEL", "LOW_FUEL_FCEV" -> "연료 부족 표시등"
    "PARKING_BRAKE" -> "주차 브레이크 표시등"
    "ELECTRONIC_PARKING_BRAKE" -> "전자식 주차 브레이크 표시등"
    "ESC" -> "차체 자세 제어 표시등"
    "ESC_OFF" -> "차체 자세 제어 OFF 표시등"
    "SEAT_BELT" -> "안전벨트 경고등"
    "DOOR_OPEN" -> "도어 열림 표시등"
    "TRUNK_OPEN" -> "트렁크 열림 표시등"
    "TURN_SIGNAL_LEFT" -> "좌측 방향 지시등"
    "TURN_SIGNAL_RIGHT" -> "우측 방향 지시등"
    "HEADLAMP_LOW" -> "하향 전조등"
    "HEADLAMP_HIGH" -> "상향 전조등"
    "AUTO_HOLD" -> "오토 홀드 표시등"
    "XEV_READY" -> "주행 가능 표시등"
    else -> "표시등 · $type"
}
