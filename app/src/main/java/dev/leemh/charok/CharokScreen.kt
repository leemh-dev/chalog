package dev.leemh.charok

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val Ink = Color(0xFF172B29)
private val Muted = Color(0xFF677772)
private val Green = Color(0xFF176953)
private val Wash = Color(0xFFF3F6F4)
private val Line = Color(0xFFE3E9E5)
private val Amber = Color(0xFF8B5A13)
private val Palette = lightColorScheme(primary = Green, onPrimary = Color.White, background = Color.White,
    surface = Color.White, surfaceVariant = Wash, onSurface = Ink, onSurfaceVariant = Muted,
    outline = Color(0xFFA7B6AE), secondaryContainer = Color(0xFFE8F3ED), onSecondaryContainer = Green)
private fun at(time: Long) = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(time))
private fun date(time: Long) = SimpleDateFormat("M월 d일 · HH:mm", Locale.KOREA).format(Date(time))

@Composable fun CharokScreen(model: RecorderModel, prepare: () -> Unit, hide: () -> Unit,
    connectPleos: () -> Unit, share: (File) -> Unit) {
    val state by model.ui.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf("main") }
    var consent by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var includeLocation by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var testTools by remember { mutableStateOf(false) }
    var writing by remember { mutableStateOf(false) }
    LaunchedEffect(state.mode) { consent = false }
    LaunchedEffect(state.phase) { if (state.phase == Phase.SAVE_FAILED) page = "main" }
    val active = state.phase in listOf(Phase.ARMED, Phase.RECORDING, Phase.SAVE_FAILED)
    MaterialTheme(colorScheme = Palette) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            BoxWithConstraints {
                val compact = maxWidth < 600.dp
                Column(Modifier.fillMaxHeight().widthIn(max = 760.dp).fillMaxWidth().align(Alignment.TopCenter)
                    .padding(horizontal = if (compact) 24.dp else 44.dp, vertical = 22.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Green, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text("차", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("차록", modifier = Modifier.padding(start = 10.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Badge(if ((state.session?.mode ?: state.mode) == InputMode.TEST) "TEST 입력" else "Pleos", (state.session?.mode ?: state.mode) == InputMode.TEST)
                    }
                    Spacer(Modifier.height(22.dp))
                    if (!state.ready) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("저장된 기록을 확인하고 있어요", Modifier.padding(top = 18.dp), color = Muted)
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { page = "main" }) { Text("기록", color = if (page == "main") Green else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { model.loadHistory(); page = "history" }, enabled = state.parked && !active) { Text("보관함", fontSize = 16.sp) }
                            Spacer(Modifier.weight(1f))
                            if (page != "main") TextButton(onClick = { page = "main" }) { Text("기록 상태로") }
                        }
                        HorizontalDivider(color = Line)
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (page != "main" && !state.parked) {
                                Heading("정차 후 확인해 주세요")
                                Text("주행 중이거나 차량 상태를 알 수 없을 때는 상세 화면을 잠급니다.", color = Muted)
                            } else when (page) {
                                "details" -> {
                                    Heading("정비 상담 카드")
                                    state.session?.let { s ->
                                        Text("${date(s.startedAt)} · ${s.mode}", color = Muted)
                                        Info("운전자 설명", s.notes.filterNot { it.bookmark }.joinToString("\n\n") { "${it.text}\n${it.origin}" }.ifBlank { "아직 남긴 설명이 없어요" })
                                        Info("표시한 순간", s.notes.filter { it.bookmark }.joinToString("\n") { "${at(it.at)} · ${it.text}" }.ifBlank { "표시한 순간이 없어요" })
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Metric("차량 관측", "${s.observations.size}건", Modifier.weight(1f))
                                            Metric("수신 불가", "${s.observations.count { !it.available }}건", Modifier.weight(1f))
                                        }
                                        Text("원음은 녹음하지 않았으며, 고장 원인을 진단한 자료가 아닙니다.\n${s.reason}", color = Muted, fontSize = 14.sp)
                                        OutlinedTextField(note, { note = it.take(980) }, label = { Text("설명 보완 · 원문은 유지됩니다") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                        Secondary("보완 내용 저장", enabled = note.isNotBlank()) { model.supplement(note); note = "" }
                                        Action("보고서 내보내기") { page = "export" }
                                        Secondary("관측 타임라인") { page = "timeline" }
                                        Secondary("센터 위치로 길 안내") { page = "center" }
                                    }
                                }
                                "timeline" -> {
                                    Heading("관측 타임라인")
                                    Text("최근 100개 · 전체 내용은 보고서에서 확인하세요", color = Muted)
                                    state.session?.observations?.takeLast(100)?.forEach { o ->
                                        Info("${at(o.receivedAt)} · ${o.origin}", "${o.field}  ${if (o.available) o.value else "수신 불가"} ${o.unit}")
                                    }
                                }
                                "export" -> {
                                    Heading("필요한 기록만 전달하세요")
                                    Info("보고서에 담기는 내용", "증상 설명, 표시한 순간과 차량 관측\n수신하지 못한 구간도 함께 표시합니다.")
                                    Check("위치를 포함해 내보내기", includeLocation, true) { includeLocation = it }
                                    Text("기본값은 위치 제외입니다. 설명에 개인정보가 없는지 확인해 주세요.", color = Muted)
                                    Action("보고서 생성·공유", "export") { model.export(includeLocation, share) }
                                    Text("파일 생성과 정비소 수신은 별개입니다. 자동 예약·센터 접수·GDS는 연결되지 않았습니다.", color = Muted, fontSize = 14.sp)
                                }
                                "history" -> {
                                    Heading("저장된 기록")
                                    Text("필요한 순간을 다시 꺼내 보세요", color = Muted)
                                    state.history.forEach { s ->
                                        OutlinedCard(onClick = { model.openHistory(s.id); page = "main" }, colors = CardDefaults.outlinedCardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(date(s.startedAt), fontWeight = FontWeight.SemiBold)
                                                Text(s.notes.firstOrNull { !it.bookmark }?.text ?: "차량 상태 기록", maxLines = 2)
                                                Text("${s.mode} · ${s.observations.size}개 관측 · ${if (s.phase == Phase.SAVED) "저장 완료" else "중단 기록"}", color = Muted, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    if (state.history.isEmpty()) Info("첫 기록을 기다리고 있어요", "기록을 마치면 이곳에 보관됩니다.")
                                }
                                "center" -> CenterForm(state) { model.navigate(it) }
                                else -> {
                                    when (state.phase) {
                                        Phase.IDLE -> {
                                            Eyebrow("차량 증상 기록")
                                            Heading("이상했던 순간,\n놓치지 마세요")
                                            Text("평소에는 내비를 보세요.\n필요한 순간만 차록이 함께 남길게요.", fontSize = 17.sp, lineHeight = 27.sp, color = Muted)
                                            Info("내비 뒤에서 조용히 준비", "최근 최대 60초의 차량 상태를 준비하고,\n기록 과정은 알림으로 알려 드립니다.")
                                            Check("차량 상태 기록 준비에 동의합니다", consent, state.parked) { consent = it }
                                            if (state.mode == InputMode.PLEOS) Check("위치도 함께 기록 (선택)", state.locationConsent, state.parked) { model.locationConsent(it) }
                                            state.notice?.let { Text(it, color = Amber, fontSize = 14.sp) }
                                            Action("기록 준비 켜기", "arm", consent && state.parked && !state.backgroundRunning) { prepare() }
                                            Text("소리 원음은 녹음하지 않습니다. 준비를 켜려면 정차 상태를 확인해야 합니다.", fontSize = 13.sp, lineHeight = 21.sp, color = Muted)
                                            TextButton(onClick = { settings = !settings }, modifier = Modifier.testTag("input-settings")) { Text(if (settings) "입력 설정 접기 −" else "입력 설정 +") }
                                            if (settings) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = { model.chooseMode(InputMode.TEST); settings = false }, modifier = Modifier.testTag("test-mode")) { Text("TEST 선택") }
                                                    OutlinedButton(onClick = { connectPleos(); settings = false }) { Text("Pleos 연결") }
                                                }
                                                Text("TEST는 개발용 입력이며 실제 차량 데이터가 아닙니다.", color = Amber, fontSize = 13.sp)
                                            }
                                        }
                                        Phase.ARMED -> {
                                            Eyebrow("● 기록 준비 중")
                                            Heading("내비를 보셔도\n준비는 계속됩니다")
                                            StatusCard("준비한 구간", "${state.bufferSeconds}", "초 / 최대 60초", "기록 시작 전까지의 차량 상태")
                                            Action("기록 시작", "start") { model.start(source = if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL) }
                                            Secondary("내비로 돌아가기", "hide") { hide() }
                                            Text("알림에서도 기록을 시작할 수 있어요.", color = Muted, fontSize = 14.sp)
                                            TextButton(onClick = { model.closeRecord(); consent = false }) { Text("준비 끄기", color = Muted) }
                                        }
                                        Phase.RECORDING -> {
                                            Eyebrow("● 기록 중")
                                            Heading(if (state.session?.notes?.lastOrNull()?.bookmark == true) "방금 순간을\n표시했어요" else "운전에 집중하세요.\n차록이 남기고 있어요")
                                            val s = state.session
                                            StatusCard("저장 확인한 관측", "${s?.observations?.size ?: 0}", "건", "${s?.notes?.count { it.bookmark } ?: 0}개 순간 표시 · ${s?.startedAt?.let { at(it) } ?: ""} 시작")
                                            s?.notes?.lastOrNull { !it.bookmark }?.let { Info("남긴 설명", it.text) }
                                            Secondary("내비로 돌아가기", "hide") { hide() }
                                            if (state.parked) {
                                                TextButton(onClick = { writing = !writing }) { Text(if (writing) "설명 입력 접기 −" else "증상 설명 추가 +") }
                                                if (writing) {
                                                    OutlinedTextField(note, { note = it.take(1000) }, label = { Text("어떤 증상이 있었나요?") }, modifier = Modifier.fillMaxWidth().testTag("note"), shape = RoundedCornerShape(16.dp))
                                                    Secondary("설명 남기기", enabled = note.isNotBlank()) { model.voice(note, if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL); note = ""; writing = false }
                                                }
                                            }
                                        }
                                        Phase.SAVED, Phase.INTERRUPTED -> {
                                            Eyebrow(if (state.phase == Phase.SAVED) "✓ 저장 완료" else "중단된 기록 복원")
                                            Heading("필요한 순간이\n안전하게 남았어요")
                                            val s = state.session!!
                                            Info("운전자 설명", s.notes.firstOrNull { !it.bookmark }?.text ?: "설명 없이 차량 상태를 기록했어요")
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Metric("차량 관측", "${s.observations.size}건", Modifier.weight(1f))
                                                Metric("순간 표시", "${s.notes.count { it.bookmark }}개", Modifier.weight(1f))
                                            }
                                            if (s.reason.isNotBlank()) Text(s.reason, color = Muted, fontSize = 14.sp)
                                            Action("상담 카드 보기", "details", state.parked) { page = "details" }
                                            Secondary("새 기록 준비", "close") { model.closeRecord(); consent = false }
                                        }
                                        Phase.SAVE_FAILED -> {
                                            Eyebrow("저장 확인 필요", Amber)
                                            Heading("기록을 멈췄어요.\n저장을 다시 확인해 주세요")
                                            Text("같은 기록으로 다시 저장합니다. 앱을 종료하면 아직 저장하지 못한 구간을 잃을 수 있습니다.", color = Muted, lineHeight = 26.sp)
                                            Action("같은 기록 다시 저장", "retry") { model.retry() }
                                        }
                                    }
                                    if (state.phase in listOf(Phase.ARMED, Phase.RECORDING) && state.mode == InputMode.PLEOS) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { model.listen() }) { Text("음성으로 말하기") }
                                            TextButton(onClick = { model.stopListening() }) { Text("마이크 끄기") }
                                        }
                                        Text(state.speech, color = Muted, fontSize = 13.sp)
                                    }
                                    if (state.mode == InputMode.TEST) {
                                        TextButton(onClick = { testTools = !testTools }, modifier = Modifier.testTag("test-tools")) { Text(if (testTools) "TEST 도구 접기 −" else "TEST 도구 +", color = Amber, fontSize = 13.sp) }
                                        if (testTools) {
                                            if (state.phase == Phase.RECORDING) Secondary("TEST: 핸들을 돌릴 때 앞쪽 소음", "test-note", state.parked) { model.voice("핸들을 돌릴 때 앞쪽에서 덜컥 소리가 나요") }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { model.toggleTestMotion() }, modifier = Modifier.testTag("motion")) { Text(if (state.parked) "TEST 주행으로" else "TEST 정차로", color = Amber) }
                                                if (BuildConfig.DEBUG) TextButton(onClick = { model.injectFailure() }, modifier = Modifier.testTag("fail-save")) { Text("저장 실패 TEST", color = Amber) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (state.phase == Phase.RECORDING) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { model.mark() }, modifier = Modifier.weight(1f).heightIn(min = 58.dp).testTag("mark"), shape = RoundedCornerShape(16.dp)) { Text("지금이야", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                                Button(onClick = { model.finish() }, modifier = Modifier.weight(1f).heightIn(min = 58.dp).testTag("stop"), shape = RoundedCornerShape(16.dp)) { Text("종료·저장", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                        HorizontalDivider(color = Line)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (state.parked) "${state.mode} · P 확인" else if (state.gear == "DRIVE") "주행 중 · 상세 잠금" else "차량 상태 미확인 · 상세 잠금", color = if (state.parked) Muted else Amber, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            if (state.backgroundRunning) Text("백그라운드 유지", color = Green, fontSize = 12.sp)
                        }
                        if (!state.parked || state.phase == Phase.SAVE_FAILED) Text(state.message, color = Muted, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
@Composable private fun Heading(text: String) { Text(text, fontSize = 29.sp, fontWeight = FontWeight.Bold, lineHeight = 39.sp, letterSpacing = (-0.6).sp) }
@Composable private fun Eyebrow(text: String, color: Color = Green) { Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
@Composable private fun Badge(text: String, test: Boolean) {
    Text(text, color = if (test) Amber else Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(if (test) Color(0xFFFFF4E3) else Color(0xFFEAF4EE), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
}
@Composable private fun Info(title: String, text: String) {
    Column(Modifier.fillMaxWidth().background(Wash, RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Medium)
        Text(text, fontSize = 16.sp, lineHeight = 25.sp)
    }
}
@Composable private fun StatusCard(label: String, value: String, unit: String, description: String) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF0F7F2), RoundedCornerShape(22.dp)).border(1.dp, Color(0xFFDCEBE1), RoundedCornerShape(22.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Green, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = Green)
            Text(unit, Modifier.padding(bottom = 8.dp), color = Muted, fontSize = 14.sp)
        }
        Text(description, color = Muted, fontSize = 13.sp)
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Wash, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Muted, fontSize = 13.sp); Text(value, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun Action(text: String, tag: String = text, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag(tag)) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}
@Composable private fun Secondary(text: String, tag: String = text, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag(tag)) { Text(text, fontSize = 16.sp) }
}
@Composable private fun Check(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, change, enabled = enabled, modifier = Modifier.testTag(if (text.startsWith("차량")) "consent" else text))
        Text(text, fontSize = 14.sp, lineHeight = 22.sp, color = if (enabled) Ink else Muted)
    }
}
@Composable private fun CenterForm(state: RecorderUi, request: (CenterTarget) -> Unit) {
    var name by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }; var lon by remember { mutableStateOf("") }; var source by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    Heading("방문할 센터 확인")
    Text("공식 정보에서 확인한 위치를 입력하세요. 예약 기능은 연결되지 않았습니다.", color = Muted)
    OutlinedTextField(name, { name = it; confirmed = false }, label = { Text("센터 이름") })
    OutlinedTextField(address, { address = it; confirmed = false }, label = { Text("주소") })
    OutlinedTextField(lat, { lat = it; confirmed = false }, label = { Text("위도") })
    OutlinedTextField(lon, { lon = it; confirmed = false }, label = { Text("경도") })
    OutlinedTextField(source, { source = it; confirmed = false }, label = { Text("확인한 공식 출처 URL (https)") })
    Check("센터와 좌표를 직접 확인했습니다", confirmed, state.parked) { confirmed = it }
    val target = CenterTarget(name, address, lat.toDoubleOrNull() ?: Double.NaN, lon.toDoubleOrNull() ?: Double.NaN, source)
    Action("지금 순정 내비로 안내", enabled = confirmed && target.valid() && state.mode == InputMode.PLEOS && state.parked) { request(target) }
    Text(state.nav, color = Muted)
}
