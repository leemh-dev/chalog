package dev.leemh.charok.sdk

import android.content.Context
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

data class CenterTarget(val name: String, val address: String, val latitude: Double, val longitude: Double, val sourceUrl: String) {
    fun valid() = name.isNotBlank() && address.isNotBlank() && latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 && sourceUrl.startsWith("https://")
}
class CenterNavigator(private val context: Context, private val status: (String) -> Unit) {
    private var helper: NaviHelper? = null
    private var active = false
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { if (active) status("SDK 경로 시작 이벤트 · 실제 핀은 순정 내비에서 확인하세요") }
        override fun onRoutingCompleted() { if (active) status("SDK 계산 완료 이벤트 · 예약·도착 확인은 아닙니다") }
        override fun onDestinationInfo(destinationInfo: DestinationInfo) { if (active) status("SDK 목적지 정보 이벤트 · 이 요청과 실제 핀 일치 확인 필요") }
        override fun onError(errorCode: NaviErrorCode) { if (active) status("내비 SDK 오류: $errorCode") }
    }
    fun request(target: CenterTarget) {
        require(target.valid()); close(); active = true
        try {
            helper = NaviHelper(context).also { it.initialize(); it.addListener(listener) }
            helper!!.requestRoute(RouteInfo(latitude = target.latitude, longitude = target.longitude,
                poiId = "", poiSubId = "0", poiName = target.name, address = target.address))
            status("내비 요청 함수 반환 · 계산 완료/실제 핀 미확인")
            helper!!.getDestinationInfo()
        } catch (_: Exception) { status("내비 요청 실패 · 서비스/권한을 확인하세요") }
    }
    // Leaving Charok releases our listener; it must not cancel the user's stock navigation route.
    fun close() { active = false; runCatching { helper?.removeListener(listener); helper?.release() }; helper = null }
}
