package dev.leemh.charok

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.leemh.charok.core.*
import dev.leemh.charok.sdk.CenterTarget
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF101922)
private val CardInk = Color(0xFF192631)
private val Mint = Color(0xFF75E4C4)
private val Muted = Color(0xFFB6C4CE)
private val Amber = Color(0xFFFFCE85)
private val Palette = darkColorScheme(primary = Color(0xFFA9C9FF), onPrimary = Ink, background = Ink,
    surface = Ink, surfaceVariant = CardInk, onSurface = Color(0xFFF5F8FA), onSurfaceVariant = Muted)
private fun at(time: Long) = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(time))

@Composable fun CharokScreen(model: RecorderModel, connectPleos: () -> Unit, share: (File) -> Unit) {
    val state by model.ui.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf("main") }
    var consent by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var includeLocation by remember { mutableStateOf(false) }
    LaunchedEffect(state.mode) { consent = false }
    MaterialTheme(colorScheme = Palette) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            BoxWithConstraints {
                val compact = maxWidth < 600.dp
                val padding = if (compact) 20.dp else 40.dp
                Column(Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = padding, vertical = 18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("차록", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(if (state.mode == InputMode.TEST) "TEST · 차량 입력 재현" else "PLEOS · SDK 입력", color = if (state.mode == InputMode.TEST) Amber else Mint, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!state.ready) { Text("저장된 기록을 확인하고 있어요", color = Muted); Text(state.message); return@Column }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val body = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    Column(body, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (page != "main" && !state.parked) {
                            Heading("정차 후\n확인해 주세요.")
                            Text("상세 열람·수정·공유는 주행 또는 상태 미확인 시 제한됩니다.", color = Muted)
                            Action("기록 상태로") { page = "main" }
                        } else when (page) {
                            "details" -> {
                                val s = state.session
                                Heading("정비 상담 카드")
                                if (s != null) {
                                    Text("${s.mode} · ${at(s.startedAt)} 시작", color = Mint)
                                    Info("운전자 설명", s.notes.filterNot { it.bookmark }.joinToString("\n") { "${it.text}\n[${it.origin}]" }.ifBlank { "설명 없음" })
                                    Info("사용자가 표시한 순간", s.notes.filter { it.bookmark }.joinToString("\n") { "${at(it.at)} · ${it.text}" }.ifBlank { "표시 없음" })
                                    Info("함께 남긴 관측", "${s.observations.size}건 · ${s.observations.map { it.field }.distinct().joinToString()}")
                                    Info("수신하지 못한 관측", "${s.observations.count { !it.available }}건 · 관측 사이 구간과 미지원 필드는 미확인")
                                    Text("원음 미녹음 · 고장 원인 미판정\n${s.reason}", color = Muted)
                                    Action("관측 타임라인") { page = "timeline" }
                                    Action("내보내기 검토") { page = "export" }
                                    Action("센터 위치로 길 안내") { page = "center" }
                                }
                            }
                            "timeline" -> {
                                Heading("관측 타임라인")
                                Text("최근 100개 표시 · 전체는 보고서에 포함합니다.", color = Muted)
                                state.session?.observations?.takeLast(100)?.forEach { o ->
                                    Info("${at(o.receivedAt)} · ${o.origin}", "${o.field}: ${if (o.available) o.value else "수신 불가"} ${o.unit}")
                                }
                            }
                            "export" -> {
                                Heading("선택한 기록만\n전달합니다.")
                                Text("증상 설명 · 순간 표시 · 차량 관측 · 누락 구간", fontSize = 20.sp)
                                Check("위치를 포함해 내보내기", includeLocation, true) { includeLocation = it }
                                Text("기본값은 위치 제외입니다. 공유할 설명에 개인정보가 없는지 확인하세요.\n파일 생성과 정비소 수신은 다릅니다.", color = Muted)
                                Action("보고서 생성·공유", "export") { model.export(includeLocation, share) }
                                Text("자동 예약·센터 자료 접수·GDS는 현재 미연결입니다.", color = Amber)
                            }
                            "history" -> {
                                Heading("저장된 기록")
                                state.history.forEach { s -> Action("${at(s.startedAt)} · ${s.mode} · ${s.phase}") { model.openHistory(s.id); page = "main" } }
                                if (state.history.isEmpty()) Text("아직 저장된 기록이 없습니다.")
                            }
                            "center" -> CenterForm(state) { model.navigate(it) }
                            else -> when (state.phase) {
                                Phase.IDLE -> {
                                    Heading("필요할 때,\n그 순간을 남기세요.")
                                    Text("이상했던 순간과 전후 상황을\n정비 상담 때 함께 확인하세요.", fontSize = 20.sp, color = Muted)
                                    Info("기록 준비", "실제 받은 최근 최대 60초의 차량 상태\n소리 원음은 녹음하지 않습니다.")
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(onClick = { model.chooseMode(InputMode.TEST) }, modifier = Modifier.testTag("test-mode")) { Text("TEST 선택") }
                                        OutlinedButton(onClick = connectPleos) { Text("Pleos 연결") }
                                    }
                                    Check("차량 상태 기록 준비에 동의합니다", consent, state.parked) { consent = it }
                                    if (state.mode == InputMode.PLEOS) Check("위치도 기록에 포함 (선택)", state.locationConsent, state.parked) { model.locationConsent(it) }
                                    Action("기록 준비 켜기", "arm", consent && state.parked) { model.arm(consent) }
                                    Action("저장된 기록", enabled = state.parked) { model.loadHistory(); page = "history" }
                                }
                                Phase.ARMED -> {
                                    Text("기록 준비 중", color = Mint, fontSize = 16.sp)
                                    Heading("말씀하시면\n그 순간부터 남길게요.")
                                    Info("실제 확보한 준비 구간", "${state.bufferSeconds}초 / 최대 60초")
                                    Text("“차록, 증상 기록해 줘.”", fontSize = 22.sp)
                                    Action("기록 시작", "start") { model.start(source = if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL) }
                                    Action("준비 끄기") { model.closeRecord(); consent = false }
                                }
                                Phase.RECORDING -> {
                                    Text("● 차량 상태 기록 중", color = Mint, fontSize = 16.sp)
                                    Heading(if (state.session?.notes?.lastOrNull()?.bookmark == true) "방금 순간을\n표시했어요." else "차량 증상을\n기록합니다.")
                                    Text("${state.session?.observations?.size ?: 0}건 저장 확인 · ${state.session?.mode}", color = Muted)
                                    val last = state.session?.notes?.lastOrNull()
                                    Info(if (last == null) "어떤 증상이 있었나요?" else "남긴 설명 · ${last.origin}", last?.text ?: "짧게 말씀해 주세요.")
                                    if (state.mode == InputMode.TEST) {
                                        Action("TEST: 핸들을 돌릴 때 앞쪽 소음", "test-note", state.parked) { model.voice("핸들을 돌릴 때 앞쪽에서 덜컥 소리가 나요") }
                                    }
                                }
                                Phase.SAVED, Phase.INTERRUPTED -> {
                                    Text(if (state.phase == Phase.SAVED) "저장 완료 · DB 재조회 확인" else "중단된 기록 · 마지막 저장 지점 복원", color = Mint, fontSize = 16.sp)
                                    Heading("상담할 기록이\n남았어요.")
                                    val s = state.session!!
                                    Info("사용자 설명", s.notes.firstOrNull { !it.bookmark }?.text ?: "설명 없이 기록한 구간입니다.")
                                    Text("${s.observations.size}개 관측 · ${s.notes.count { it.bookmark }}개 순간 표시", color = Muted)
                                    Text(s.reason, color = Muted)
                                    Action("상담 카드 보기", "details", state.parked) { page = "details" }
                                    Action("닫기 · 준비 끄기", "close") { model.closeRecord(); consent = false }
                                }
                                Phase.SAVE_FAILED -> {
                                    Text("저장 실패 · 관측 중지", color = Amber)
                                    Heading("저장을 마치지\n못했어요.")
                                    Text("같은 기록으로 다시 저장합니다. 앱을 종료하면 저장되지 않은 구간을 잃을 수 있습니다.", fontSize = 20.sp)
                                    Action("같은 기록 다시 저장", "retry") { model.retry() }
                                }
                            }
                        }
                        if (page == "main" && state.phase in listOf(Phase.ARMED, Phase.RECORDING) && state.mode == InputMode.PLEOS) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { model.listen() }) { Text("음성으로 말하기") }
                                OutlinedButton(onClick = { model.stopListening() }) { Text("마이크 끄기") }
                            }
                            Text(state.speech, color = Muted, fontSize = 14.sp)
                        }
                        if (page == "main" && state.phase == Phase.RECORDING && state.parked) {
                            OutlinedTextField(note, { note = it.take(1000) }, label = { Text("정차 후 설명 입력") }, modifier = Modifier.fillMaxWidth().testTag("note"))
                            OutlinedButton(onClick = { model.voice(note, if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL); note = "" }, enabled = note.isNotBlank()) { Text("설명 남기기") }
                        }
                        if (page == "main" && state.mode == InputMode.TEST) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { model.toggleTestMotion() }, modifier = Modifier.testTag("motion")) { Text(if (state.parked) "TEST 주행으로" else "TEST 정차로") }
                                if (BuildConfig.DEBUG) TextButton(onClick = { model.injectFailure() }, modifier = Modifier.testTag("fail-save")) { Text("저장 실패 TEST") }
                            }
                        }
                    }
                    if (state.phase == Phase.RECORDING) {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { model.mark() }, modifier = Modifier.weight(1f).heightIn(min = 60.dp).testTag("mark"), colors = ButtonDefaults.buttonColors(containerColor = CardInk, contentColor = Mint)) { Text("지금이야", fontSize = 20.sp) }
                            Button(onClick = { model.finish() }, modifier = Modifier.weight(1f).heightIn(min = 60.dp).testTag("stop")) { Text("기록 종료", fontSize = 20.sp) }
                        }
                    }
                    if (page != "main") TextButton(onClick = { page = "main" }) { Text("기록 상태로") }
                    Text(if (state.parked) "${state.mode} · P 확인" else "주행/상태 미확인 · 상세 조작 제한", color = if (state.parked) Muted else Amber, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(state.message, color = Muted, fontSize = 13.sp, maxLines = 2)
                }
            }
        }
    }
}
@Composable private fun Heading(text: String) { Text(text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 43.sp, modifier = Modifier.padding(vertical = 10.dp)) }
@Composable private fun Info(title: String, text: String) {
    Column(Modifier.fillMaxWidth().background(CardInk, RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, fontSize = 15.sp, color = Muted); Text(text, fontSize = 20.sp, lineHeight = 30.sp)
    }
}
@Composable private fun Action(text: String, tag: String = text, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag(tag)) { Text(text, fontSize = 19.sp) }
}
@Composable private fun Check(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, change, enabled = enabled, modifier = Modifier.testTag(if (text.startsWith("차량")) "consent" else text)); Text(text, fontSize = 17.sp) }
}
@Composable private fun CenterForm(state: RecorderUi, request: (CenterTarget) -> Unit) {
    var name by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }; var lon by remember { mutableStateOf("") }; var source by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    Heading("방문할 센터를\n확인해 주세요.")
    Text("공식 정보에서 확인한 위치를 입력하세요. 실제 예약 기능은 연결되지 않았습니다.", color = Muted)
    OutlinedTextField(name, { name = it; confirmed = false }, label = { Text("센터 이름") })
    OutlinedTextField(address, { address = it; confirmed = false }, label = { Text("주소") })
    OutlinedTextField(lat, { lat = it; confirmed = false }, label = { Text("위도") })
    OutlinedTextField(lon, { lon = it; confirmed = false }, label = { Text("경도") })
    OutlinedTextField(source, { source = it; confirmed = false }, label = { Text("확인한 공식 출처 URL (https)") })
    Check("센터와 좌표를 직접 확인했습니다", confirmed, state.parked) { confirmed = it }
    val target = CenterTarget(name, address, lat.toDoubleOrNull() ?: Double.NaN, lon.toDoubleOrNull() ?: Double.NaN, source)
    Action("지금 순정 내비로 안내", enabled = confirmed && target.valid() && state.mode == InputMode.PLEOS && state.parked) { request(target) }
    Text(state.nav, color = Mint)
}
