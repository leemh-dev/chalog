"""Fill only project fields of the official DOCX. Never fill consent/signatures."""
import hashlib
import json
import sys
from pathlib import Path
from docx import Document
from docx.shared import Pt

root = Path(__file__).resolve().parents[1]
source = Path(sys.argv[1])
document = Document(source)
sections = [
    "차록은 운전자가 느낀 이상 증상과 실제 확보한 직전 최대 60초부터 종료까지의 차량 상태를 연결해 정비 상담을 돕는 Pleos 앱입니다.\n‘이럴 때만 이상해요’를 정비사와 함께 확인할 수 있는 기록으로 만듭니다.",
    "소음·진동·공조 이상이 간헐적으로 나타나면 정비소에서 재현되지 않을 수 있습니다. 차량 지식이 적은 운전자와 차를 함께 쓰는 가족은 당시 상황을 정확하게 설명하기 어렵고, 정비사는 기억에 의존한 설명과 현재 진단 사이의 빈틈을 채워야 합니다.\n차록은 운전자가 느낀 순간을 표시하고 전후의 확보 가능한 차량 상태를 함께 보존하여 상담의 출발 자료를 제공합니다. 기존 리모트케어·스캐너 진단을 대체하거나 고장 원인을 확정하는 서비스는 아닙니다.",
    "① 기록 준비를 켠 동안 지원 데이터의 최근 60초를 임시 보관하고, 시작 명령 후 같은 세션에 이후 관측을 연결합니다. 실제 수신하지 않은 과거·누락 값은 만들지 않습니다.\n② 음성으로 증상을 설명하거나 ‘지금이야’라고 말하면 사용자 표시를 남깁니다. 원음 녹음이나 소음 검출과 구분합니다.\n③ 순정 차량 정보·내비와 함께 사용하는 앱 패널에 상태·현재 질문·종료를 표시하며, 상세 수정·공유는 정차 후 제공합니다. 창 배치는 시스템 정책을 따릅니다.\n④ 사용자 설명·관측·누락을 상담 카드로 정리하고, 사용자가 확인한 한 건을 내보내 정비사와 함께 볼 수 있게 합니다.\n차량 특화성은 차량 관측과 증상 순간의 시간축 연결, 음성 중심 UX, 운전 상태에 따른 조작 제한입니다.",
    "정차 상태의 시연 예시입니다. 운전 전 기록 준비를 켭니다. ‘차록, 증상 기록해 줘. 핸들을 돌릴 때 앞쪽에서 덜컥 소리가 나요’라고 말하면 실제 확보한 직전 구간과 이후 상태를 남깁니다. ‘지금이야’라는 발화는 사용자 순간 표시로 저장합니다.\n‘기록 종료해 줘’ 이후 관측 중지→저장→DB 재조회가 완료되면 완료를 알립니다. 정차 후 원문·시각·관측·공백을 확인하고 상담 카드를 내보냅니다. 확인된 센터 좌표가 있는 경우 별도 사용자 요청으로 순정 내비 안내를 연결합니다.\nTEST 입력·공식 SDK 수신·실제 마이크 성공은 별도로 표시합니다. 원음 녹음·실제 자동 예약·GDS 자동 열람은 현재 지원이 확인되지 않은 확장 목표입니다.",
    "Kotlin·Jetpack Compose·Room으로 구현합니다. 1단계는 출처를 보존하는 TEST 입력, 60초 버퍼, 동일 세션 기록·표시·종료·저장·복원입니다. 2단계는 공개 SDK 어댑터와 실제 Pleos 검증, 3단계는 상담 보고서·선택 내보내기와 전체 시연입니다.\nVehicle 2.3.2의 메서드별 권한·capability를 확인해 기어·조향·브레이크·공조 등 지원되는 관측만 사용하고 PRIVATE API는 제외합니다. Gleo는 시작/설명·순간/종료의 3개 Action과 asking을 검증합니다. STT 3.3.1 ON_DEVICE는 공식 음성 인식 경로로 사용하며, 실제 마이크와 보유 PCM 주입을 구분합니다. TTS 응답은 Gleo 발화와 겹치지 않게 합니다. 선택 동의한 위치는 FusedLocation 1.1.3, 확인된 센터 경로 요청은 NaviHelper 3.0.5로 연동합니다.\nCloud에서 단위·저장·빌드를 검증하고 Mac Pleos에서 실제 입력·callback·화면·핀을 확인합니다. 권한 철회·중복 명령·저장 실패·프로세스 종료·주행 상태 미확인도 검사합니다.\n현재 산출물은 기획과 브라우저 UX 시안이며 Android 앱·SDK·APK 실행은 미완료입니다. 구현 결과에 따라 제출 직전 이 상태를 갱신합니다.",
    "운전자는 설명할 단서를 확보하고 정비사는 발생 조건을 질문할 자료를 얻는 것이 목표입니다. 운전자와 정비사에게 원문 메모와 차록 요약을 비교하게 하여 조건 누락·요약 왜곡·추가 질문·상담 활용 의향을 확인할 계획입니다. 효용·사용자 수·수리비 절감률은 아직 검증하지 않았습니다.\n저빈도 문제 해결 서비스이므로 근거 없는 MAU·Retention 전망 대신 필요 순간 기록 완결률과 상담 활용도를 우선 평가합니다. 향후 정식 지원·제휴가 확보되면 원음 채집, 동의 기반 센터 예약·자료 접수, GDS 연동으로 확장합니다. 수익 모델은 상담 효용을 검증한 뒤 접수 도구·차량 서비스 연계로 검토합니다. 실제 제휴나 매출은 없습니다.",
    "현재 참고자료: 기획·UX 설계 및 조작 가능한 브라우저 시안(실제 Pleos 실행 아님).\n별첨: docs/07-submission-plan.md, docs/08-ux-ui-spec.md, docs/09-demo-delivery-plan.md, docs/design/chalog-prototype.html 및 이미지.\nAPK·실제 앱 시연 영상은 아직 없습니다. 제출 시 검증된 링크와 최종 커밋을 추가합니다. 공개 자료에는 개인 정보·서명·계정 비밀을 넣지 않습니다.",
    "미제작·미게시. 선택 제출 항목입니다.",
]
assert len(document.tables) == 10, "Official template changed; inspect before filling."
for i, text in enumerate(sections, 1):
    table = document.tables[i]
    assert len(table.rows) == 1 and len(table.columns) == 1
    table.cell(0, 0).text = text
    for paragraph in table.cell(0, 0).paragraphs:
        for run in paragraph.runs:
            run.font.size = Pt(10)

for paragraph in document.paragraphs:
    if paragraph.text.startswith("앱명 / 프로젝트명:"):
        paragraph.text = "앱명 / 프로젝트명: 차록 (CHALOG)"

# Preserve every applicant, consent and signature table cell exactly as supplied.
original = Document(source)
for index in (0, 9):
    assert [[c.text for c in r.cells] for r in original.tables[index].rows] == [
        [c.text for c in r.cells] for r in document.tables[index].rows
    ]
document.core_properties.author = ""
document.core_properties.last_modified_by = ""
document.core_properties.title = "차록 공모전 접수양식 초안 — 개인정보·동의·서명 미작성"
out = root / "docs/submission/chalog-application-draft.docx"
document.save(out)
check = Document(out)
assert all(check.tables[i].cell(0, 0).text == text for i, text in enumerate(sections, 1))
evidence = {
    "status": "DRAFT: applicant details, consent and signatures not completed; not submitted",
    "official_source": "https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx",
    "template_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
    "draft_sha256": hashlib.sha256(out.read_bytes()).hexdigest(),
    "filled_project_sections": len(sections),
    "checks": ["8 project sections reopened and matched", "applicant and signature tables unchanged", "no applicant identity added"],
    "limitation": "Word page rendering and pagination not verified here; review in Word before signing/submitting",
}
(root / "docs/evidence/planning-design/submission-draft.json").write_text(
    json.dumps(evidence, ensure_ascii=False, indent=2) + "\n"
)
print(json.dumps(evidence, ensure_ascii=False))
