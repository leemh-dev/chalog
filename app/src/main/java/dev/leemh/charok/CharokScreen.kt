package dev.leemh.charok

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.leemh.charok.core.*
import dev.leemh.charok.sdk.CenterTarget
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF18243A)
private val Muted = Color(0xFF667085)
private val SignalBlue = Color(0xFF3158D5)
private val Wash = Color(0xFFF4F6FA)
private val Line = Color(0xFFE1E6EF)
private val Amber = Color(0xFF8B5A13)
private val Palette = lightColorScheme(primary = SignalBlue, onPrimary = Color.White, background = Color.White,
    surface = Color.White, surfaceVariant = Wash, onSurface = Ink, onSurfaceVariant = Muted,
    outline = Color(0xFFABB6CA), secondaryContainer = Color(0xFFEDF2FF), onSecondaryContainer = SignalBlue)
private fun at(time: Long) = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(time))
private fun date(time: Long) = SimpleDateFormat("M월 d일 · HH:mm", Locale.KOREA).format(Date(time))

@Composable fun CharokScreen(model: RecorderModel, prepare: () -> Unit, setupVoice: () -> Unit, hide: () -> Unit,
    connectPleos: () -> Unit, saveReport: (File) -> Unit, share: (File) -> Unit) {
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
    val active = state.starting || state.phase in listOf(Phase.ARMED, Phase.RECORDING, Phase.SAVE_FAILED)
    MaterialTheme(colorScheme = Palette, typography = Typography(
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 27.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 27.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.Medium))) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            BoxWithConstraints {
                val compact = maxWidth < 600.dp
                Column(Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth().align(Alignment.TopCenter)
                    .padding(horizontal = if (compact) 24.dp else 30.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.charok_icon), contentDescription = null, modifier = Modifier.size(32.dp))
                        Text("차록", modifier = Modifier.padding(start = 8.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("CHALOG", modifier = Modifier.padding(start = 10.dp), fontSize = 16.sp, color = Muted, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Badge(if ((state.session?.mode ?: state.mode) == InputMode.TEST) "TEST 입력" else "Pleos", (state.session?.mode ?: state.mode) == InputMode.TEST)
                        IconButton(onClick = hide, modifier = Modifier.testTag("dismiss-panel").semantics { contentDescription = "차록 화면 닫기" }) {
                            Text("×", fontSize = 26.sp, color = Muted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!state.ready) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("저장된 기록을 확인하고 있어요", Modifier.padding(top = 18.dp), color = Muted)
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { page = "main" }) { Text("기록", color = if (page == "main") SignalBlue else Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { model.loadHistory(); page = "history" }, enabled = state.parked && !active) { Text("보관함", fontSize = 18.sp) }
                            Spacer(Modifier.weight(1f))
                            if (page != "main") TextButton(onClick = { page = "main" }) { Text("기록 상태로") }
                        }
                        HorizontalDivider(color = Line)
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (page != "main" && !state.parked) {
                                Heading("정차 후 확인해 주세요")
                                Text("주행 중이거나 차량 상태를 알 수 없을 때는 상세 화면을 잠급니다.", color = Muted)
                            } else when (page) {
                                "details" -> {
                                    Heading("정비 상담 카드")
                                    state.session?.let { s ->
                                        Text("${date(s.startedAt)} · ${s.mode}", color = Muted)
                                        RecordQuote(s, true, expanded = true)
                                        TelltaleCard(s, expanded = true)
                                        MomentPreview(s, limit = 200)
                                        if (s.notes.count { !it.bookmark } > 1) Info("추가 설명", s.notes.filterNot { it.bookmark }.drop(1).joinToString("\n\n") { "${it.text} · ${it.origin}" })
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Metric("차량 관측", "${s.observations.size}건", Modifier.weight(1f))
                                            Metric("수신 불가", "${s.observations.count { !it.available }}건", Modifier.weight(1f))
                                        }
                                        Text("원음은 녹음하지 않았으며, 고장 원인을 진단한 자료가 아닙니다.\n${s.reason}", color = Muted, fontSize = 16.sp)
                                        OutlinedTextField(note, { note = it.take(980) }, label = { Text("설명 보완 · 원문은 유지됩니다") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
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
                                    Secondary("파일로 저장", "save-report") { model.export(includeLocation, saveReport) }
                                    Text("파일 생성과 정비소 수신은 별개입니다. 자동 예약·센터 접수·GDS는 연결되지 않았습니다.", color = Muted, fontSize = 16.sp)
                                }
                                "history" -> {
                                    Heading("저장된 기록")
                                    Text("필요한 순간을 다시 꺼내 보세요", color = Muted)
                                    state.history.forEach { s ->
                                        OutlinedCard(onClick = { model.openHistory(s.id); page = "main" }, colors = CardDefaults.outlinedCardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(date(s.startedAt), fontWeight = FontWeight.SemiBold)
                                                Text(s.notes.firstOrNull { !it.bookmark }?.text ?: "차량 상태 기록", maxLines = 2)
                                                Text("${s.mode} · ${s.observations.size}개 관측 · ${if (s.phase == Phase.SAVED) "저장 완료" else "중단 기록"}", color = Muted, fontSize = 16.sp)
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
                                            Heading("이상했던 순간을 남기세요")
                                            Text("차량 상태와 운전자 설명을 함께 보관합니다.", color = Muted)
                                            if (state.starting) Info("음성 요청을 받았어요", "차량 상태를 확인하고 있습니다. 화면은 닫으셔도 됩니다.")
                                            else if (state.voiceEnabled) Info("Gleo 음성 기록 사용 중", "호출할 때만 기록을 시작합니다.\n앱을 닫아도 다음 음성 요청을 받을 수 있습니다.")
                                            else Info("한 번 설정하고, 말로 기록하세요", "정차 중 동의와 권한을 설정하면\n다음부터는 Gleo로 기록을 시작할 수 있습니다.")
                                            Check("차량 상태 기록에 동의합니다", consent, state.parked && !state.starting) { consent = it }
                                            if (state.mode == InputMode.PLEOS) Check("위치도 함께 기록 (선택)", state.locationConsent, state.parked) { model.locationConsent(it) }
                                            state.notice?.let { Text(it, color = Amber, fontSize = 16.sp) }
                                            if (!state.voiceEnabled && (state.mode == InputMode.PLEOS || BuildConfig.DEBUG)) Action("음성 기록 사용 설정", "voice-setup", consent && state.parked && !active) { setupVoice() }
                                            Action("기록 준비 켜기", "arm", consent && state.parked && !state.backgroundRunning && !state.starting) { prepare() }
                                            Text("미리 준비하면 실제 수집한 최근 최대 60초를 함께 남깁니다. 음성으로 바로 시작한 기록에는 없는 과거를 채우지 않습니다. 원음은 녹음하지 않습니다.", fontSize = 16.sp, lineHeight = 21.sp, color = Muted)
                                            if (state.voiceEnabled) TextButton(onClick = { model.configureVoice(false) }, enabled = state.parked && !active, modifier = Modifier.testTag("voice-disable")) { Text("음성 기록 사용 끄기") }
                                            if (state.voiceEnabled) Secondary("내비로 돌아가기", "hide") { hide() }
                                            TextButton(onClick = { settings = !settings }, modifier = Modifier.testTag("input-settings")) { Text(if (settings) "입력 설정 접기 −" else "입력 설정 +") }
                                            if (settings) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = { model.chooseMode(InputMode.TEST); settings = false }, modifier = Modifier.testTag("test-mode")) { Text("TEST 선택") }
                                                    OutlinedButton(onClick = { connectPleos(); settings = false }) { Text("Pleos 연결") }
                                                }
                                                Text("TEST는 개발용 입력이며 실제 차량 데이터가 아닙니다.", color = Amber, fontSize = 16.sp)
                                            }
                                        }
                                        Phase.ARMED -> {
                                            Eyebrow("● 기록 준비 중")
                                            Heading("기록할 준비가 됐어요")
                                            StatusCard("준비한 구간", "${state.bufferSeconds}", "초 / 최대 60초", "기록 시작 전까지의 차량 상태")
                                            Action("기록 시작", "start") { model.start(source = if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL) }
                                            Secondary("내비로 돌아가기", "hide") { hide() }
                                            Text("알림에서도 기록을 시작할 수 있어요.", color = Muted, fontSize = 16.sp)
                                            TextButton(onClick = { model.closeRecord(); consent = false }) { Text("준비 끄기", color = Muted) }
                                        }
                                        Phase.RECORDING -> {
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Heading("지금 남기는 기록")
                                                Spacer(Modifier.weight(1f))
                                                Badge("● 기록 중", false)
                                            }
                                            state.session?.let { s ->
                                                RecordQuote(s, state.parked)
                                                RecordNumbers(s)
                                                if (state.parked) TelltaleCard(s)
                                                if (state.parked) MomentPreview(s)
                                            }
                                            Secondary("내비로 돌아가기", "hide") { hide() }
                                            if (state.parked) {
                                                TextButton(onClick = { writing = !writing }) { Text(if (writing) "설명 입력 접기 −" else "증상 설명 추가 +") }
                                                if (writing) {
                                                    OutlinedTextField(note, { note = it.take(1000) }, label = { Text("어떤 증상이 있었나요?") }, modifier = Modifier.fillMaxWidth().testTag("note"), shape = RoundedCornerShape(12.dp))
                                                    Secondary("설명 남기기", enabled = note.isNotBlank()) { model.voice(note, if (state.mode == InputMode.TEST) Origin.TEST else Origin.MANUAL); note = ""; writing = false }
                                                }
                                            }
                                        }
                                        Phase.SAVED, Phase.INTERRUPTED -> {
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Heading("남겨둔 기록")
                                                Spacer(Modifier.weight(1f))
                                                Badge(if (state.phase == Phase.SAVED) "✓ 저장 완료" else "중단 복원", state.phase != Phase.SAVED)
                                            }
                                            val s = state.session!!
                                            Text(date(s.startedAt), color = Muted, fontSize = 16.sp)
                                            RecordQuote(s, state.parked)
                                            RecordNumbers(s)
                                            if (state.parked) TelltaleCard(s)
                                            if (state.parked) MomentPreview(s)
                                            if (s.reason.isNotBlank()) Text(s.reason, color = Muted, fontSize = 16.sp)
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
                                        Text(state.speech, color = Muted, fontSize = 16.sp)
                                    }
                                    if (state.mode == InputMode.TEST) {
                                        TextButton(onClick = { testTools = !testTools }, modifier = Modifier.testTag("test-tools")) { Text(if (testTools) "TEST 도구 접기 −" else "TEST 도구 +", color = Amber, fontSize = 16.sp) }
                                        if (testTools) {
                                            if (state.phase == Phase.RECORDING) Secondary("TEST: 핸들을 돌릴 때 앞쪽 소음", "test-note", state.parked) { model.voice("핸들을 돌릴 때 앞쪽에서 덜컥 소리가 나요") }
                                            if (state.phase == Phase.RECORDING && state.parked) {
                                                Secondary("TEST: 경고등 점등", "test-lamps-on") { model.testWarningLights("on") }
                                                Secondary("TEST: 엔진 경고등 소등", "test-lamps-off") { model.testWarningLights("off") }
                                                TextButton(onClick = { model.testWarningLights("missing") }, modifier = Modifier.testTag("test-lamps-missing")) { Text("TEST: 경고등 수신 끊김", color = Amber) }
                                            }
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
                                OutlinedButton(onClick = { model.mark() }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("mark"), shape = RoundedCornerShape(12.dp)) { Text("지금이야", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                                Button(onClick = { model.finish() }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("stop"), shape = RoundedCornerShape(12.dp)) { Text("종료·저장", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                        HorizontalDivider(color = Line)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (state.parked) "${state.mode} · P 확인" else if (state.gear == "DRIVE") "주행 중 · 상세 잠금" else "차량 상태 미확인 · 상세 잠금", color = if (state.parked) Muted else Amber, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (state.backgroundRunning) Text("백그라운드 유지", color = SignalBlue, fontSize = 16.sp)
                        }
                        if (!state.parked || state.phase == Phase.SAVE_FAILED) Text(state.message, color = Muted, fontSize = 16.sp, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
private fun duration(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60)
}

@Composable private fun RecordQuote(session: Session, reveal: Boolean, expanded: Boolean = false) {
    val description = if (reveal) session.notes.firstOrNull { !it.bookmark }?.text
        ?: "설명 없이 차량 상태를 기록했어요" else "운전자 설명은 정차 후 확인할 수 있어요"
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(SignalBlue, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("운전자 설명", color = Muted, fontSize = 16.sp)
            Text(description, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium,
                maxLines = if (expanded) Int.MAX_VALUE else if (reveal) 5 else 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun RecordNumbers(session: Session) {
    val latest = maxOf(session.triggerElapsed, session.observations.lastOrNull()?.elapsed ?: 0,
        session.notes.lastOrNull()?.elapsed ?: 0)
    val elapsed = session.endedAt?.let { it - session.startedAt } ?: (latest - session.triggerElapsed)
    Column(Modifier.fillMaxWidth().background(Wash, RoundedCornerShape(14.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (session.endedAt == null) "마지막 확인 구간" else "기록 시간", color = Muted, fontSize = 16.sp)
                Text(duration(elapsed), fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1).sp, modifier = Modifier.testTag("record-duration"))
            }
            Box(Modifier.width(1.dp).height(50.dp).background(Line))
            Column(Modifier.weight(1f).padding(start = 22.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("표시한 순간", color = Muted, fontSize = 16.sp)
                Text("${session.notes.count { it.bookmark }}", fontSize = 30.sp, lineHeight = 40.sp,
                    fontWeight = FontWeight.SemiBold, color = SignalBlue)
            }
        }
        HorizontalDivider(color = Line)
        Text("차량 관측 ${session.observations.size}건 · ${session.mode}", color = Muted, fontSize = 16.sp)
    }
}

@Composable private fun MomentPreview(session: Session, limit: Int = 3) {
    val moments = session.notes.filter { it.bookmark }
    val shown = moments.takeLast(limit)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("표시한 순간", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(if (moments.size > limit) "최근 ${limit}개 / 전체 ${moments.size}개" else "기록 시작 기준", color = Muted, fontSize = 16.sp)
        }
        if (moments.isEmpty()) Text("아직 표시한 순간이 없어요", fontSize = 18.sp, color = Muted)
        shown.forEachIndexed { index, moment ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(28.dp).background(SignalBlue, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("${(moments.size - shown.size) + index + 1}", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    if (index < shown.size - 1) Box(Modifier.width(1.dp).weight(1f).background(Line))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(duration(moment.elapsed - session.triggerElapsed), fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, color = SignalBlue)
                        Text(moment.text.ifBlank { "순간 표시" }, fontSize = 18.sp, maxLines = if (limit > 3) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    Text("${at(moment.at)} · ${moment.origin}", color = Muted, fontSize = 16.sp)
                    if (index < shown.size - 1) HorizontalDivider(Modifier.padding(top = 8.dp), color = Line)
                }
            }
        }
    }
}

@Composable private fun TelltaleCard(session: Session, expanded: Boolean = false) {
    val lamps = remember(session) { session.observedLamps() }
    Column(Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp).testTag("telltales"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("관측된 경고·표시등", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(if (lamps.isEmpty()) "미확인" else "${lamps.size}종", fontSize = 16.sp, color = Muted)
        }
        if (lamps.isEmpty()) {
            Text(session.lampCoverage(), fontSize = 18.sp, color = Muted)
            Text("수신하지 못한 상태를 정상으로 판단하지 않습니다.", fontSize = 16.sp, color = Muted)
        } else {
            lamps.take(if (expanded) lamps.size else 3).forEach { lamp ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(38.dp).background(Color(0xFFFFF4E3), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        val icon = when (lamp.type) { "ENGINE" -> R.drawable.ic_lamp_engine; "TPMS" -> R.drawable.ic_lamp_tire; else -> null }
                        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = Amber, modifier = Modifier.size(26.dp))
                        else Text("!", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Amber)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(lamp.label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        val offset = lamp.firstLit.elapsed - session.triggerElapsed
                        Text("${if (offset < 0) "준비 −" else "+"}${duration(kotlin.math.abs(offset))} · ${at(lamp.firstLit.receivedAt)} · ${lamp.firstLit.origin}", fontSize = 16.sp, color = Muted)
                        if (expanded) Text("마지막 확인 ${at(lamp.latest.receivedAt)} · ${lamp.stateLabel}", fontSize = 16.sp, color = Muted)
                    }
                    if (!expanded) Text(lamp.stateLabel, fontSize = 16.sp, color = Muted)
                }
            }
            if (!expanded && lamps.size > 3) Text("외 ${lamps.size - 3}종 · 상담 카드에서 전체 확인", fontSize = 16.sp, color = Muted)
            Text("기록 구간의 관측입니다. 현재 상태·고장 진단이 아닙니다.", fontSize = 16.sp, color = Muted)
        }
    }
}

@Composable private fun Heading(text: String) { Text(text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp, letterSpacing = (-0.4).sp) }
@Composable private fun Eyebrow(text: String, color: Color = SignalBlue) { Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
@Composable private fun Badge(text: String, test: Boolean) {
    Text(text, color = if (test) Amber else SignalBlue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(if (test) Color(0xFFFFF4E3) else Color(0xFFEDF2FF), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
}
@Composable private fun Info(title: String, text: String) {
    Column(Modifier.fillMaxWidth().background(Wash, RoundedCornerShape(12.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 16.sp, color = Muted, fontWeight = FontWeight.Medium)
        Text(text, fontSize = 18.sp, lineHeight = 27.sp)
    }
}
@Composable private fun StatusCard(label: String, value: String, unit: String, description: String) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF1F4FC), RoundedCornerShape(14.dp)).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = SignalBlue, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, color = SignalBlue)
            Text(unit, Modifier.padding(bottom = 8.dp), color = Muted, fontSize = 16.sp)
        }
        Text(description, color = Muted, fontSize = 16.sp)
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Wash, RoundedCornerShape(12.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Muted, fontSize = 16.sp); Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun Action(text: String, tag: String = text, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag)) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
}
@Composable private fun Secondary(text: String, tag: String = text, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag)) { Text(text, fontSize = 18.sp) }
}
@Composable private fun Check(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, change, enabled = enabled, modifier = Modifier.testTag(if (text.startsWith("차량")) "consent" else text))
        Text(text, fontSize = 16.sp, lineHeight = 22.sp, color = if (enabled) Ink else Muted)
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
