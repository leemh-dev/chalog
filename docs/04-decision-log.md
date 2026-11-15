# 조사·의사결정 기록

## 2026-11-14 UTC · 프로젝트 분리와 초기 기획

사용자는 서비스명을 **차록**, 리포 이름을 `leemh-dev/chalog`으로 선택하고 새 저장소의 docs에 기획서 작성을 요청했습니다. 원격은 빈 공개 저장소/default main으로 확인했습니다. 기존 파일/작업 지침은 없었으며 이전 도착자리 저장소는 변경하지 않았습니다.

### 사용자 확정 요구

- “증상 기록해 줘”의 직전1분부터 “종료해 줘”까지 기록합니다.
- 주변/도로/운행 상황을 가능한 범위에서 함께 남겨 정비 대화에 활용합니다.
- 고객과 정비사가 같은 기록으로 상담합니다.
- 스캐너를 연결하면 기록이 바로 보이는 경험을 최종 목표로 둡니다.

### 설계 제안과 제약

- 직전1분은 사전 준비한 순환 버퍼로 구현하는 제안입니다. 앱/SDK가 받지 못한 과거는 복원하지 않습니다.
- 명령 종료 시 준비도 중지하는 것은 추천 기본값이며 별도 UX 확인이 필요합니다.
- 모든 데이터의 확보는 장기 비전입니다. 공개 SDK 범위/실제 권한/수신 필드만 1차에서 보장합니다.
- 정비사 별도 열람→정식 GDS 연동 순서는 현실적인 구현 제안입니다. 스캐너 자동 조회 지원은 미확인입니다.
- 음성 호출/수집 생명주기/저장 상한/위치 선택/공유 경로/보관 기간은 구현 시 구체화해야 합니다.

### 조사 계보

현재 날짜에 이 대화에서 확인한 공식 자료와 정적 Vehicle 조사를 이관했습니다. 원본은 이전 프로젝트의 다음 기록입니다.

- [음성 증상노트 기획과 권한 조사 · 9adb2c5](https://github.com/leemh-dev/dochagjari/blob/9adb2c5/docs/48-voice-symptom-recorder-plan.md)
- [전1분~종료/정비소 인계 · c74a7d2](https://github.com/leemh-dev/dochagjari/blob/c74a7d2/docs/49-symptom-session-and-service-handoff.md)
- [원본 정적 조사 증거](https://github.com/leemh-dev/dochagjari/tree/9adb2c5/docs/evidence/symptom-sdk-feasibility)

Vehicle2.3.2 AAR의 선정28메서드/권한/해시를 [evidence](evidence/sdk-review/README.md)에 가져왔습니다. STT/TTS/FusedLocation과 GDS는 공식 문서 확인 수준입니다. PRIVATE 공기압/통합 경고 이벤트를 MVP에서 제외하고 일반 getter와 구분했습니다.

### 이번 실제 작업/검증

GitHub 저장소 조회→빈 저장소 clone→문서 작성→상대 링크/JSON/이관 파일 해시·whitespace 대조→최초 커밋/푸시로 진행합니다. 앱 코드·APK·서버·스캐너 통합은 추가하지 않습니다. 기기/마이크/위치/SDK 실행·GDS 연결·정비사 인터뷰는 수행하지 않았습니다.

이전 도착자리 앱0.11.0/11의 코드/DB/APK/검사 결과는 차록으로 이관하지 않았습니다. 기존 프로젝트 성공을 차록의 기능 구현 증거로 세지 않습니다. 다음 개발 요청 시 [단계0](03-development-plan.md)부터 시작합니다.

검사 결과: Markdown7파일 상대 링크 누락0, JSON3파일 파싱 성공, 이관 원본2파일 SHA-256 일치, 선정 메서드28개 일치. git diff --cached --check로 최종 whitespace를 확인합니다. 앱/SDK 실행 검사는 하지 않았습니다.


## 2026-11-14 UTC · 대화형3영역/원음/예약/내비 시나리오 재조사

사용자는 UX 설계 후 전체 흐름의 기술 재검토를 요청했다. 기능 구현은 하지 않고 docs/conversation-scenario-review 브랜치에서 [05](05-conversation-scenario-feasibility.md)를 작성했다.

- 공식 웹 문서를 새로 조회했다. Gleo asking·문맥 전달·최대3function·7초Action timeout, 시스템 Driving view+앱윈도1~2개/기종별primary진입, STT 결과/RMS/EPD와PCM입력방향, 하드웨어정책/BG→CCH, FusedLocation, NaviHelper3.0.5 목적지/콜백, 현대웹예약/ARS를 대조했다.
- 원음 반환/파일 녹음과 Gleo 동시 사용, 우측 배치 강제 API, 제3자 예약·현대업체 자료 접수 API는 확인하지 못했다. 현대웹은3일후~6주이내/하이테크전화예약 조건이 있어 즉시예약확정 UX를 조건부로 정정했다. 차속은 공식FAQ에서 AAOS PERF_VEHICLE_SPEED 경로를 추가 확인했다.
- 일부 추측 STT URL/이전 link ID 조회 실패 후 공식 result-listeners와검색으로보완했다. 공모전공지008 URL은이번에열리지않아 종전조사와현재재확인수준을분리했다. 새서버/녹음/기기/예약실행이나제조사문의는없다.
- 다음: 3개음성명령/후속질문과분할·버퍼실행검증을먼저한다. 원음/정식예약근거가없으면옵션미지원/예약채널안내·명시TEST센터로구분한다. 연구문서상대링크와whitespace를검사한뒤커밋·푸시·PR로남기고자동병합하지않는다.


## 2026-11-14 UTC · 설명회 링크와 조사 범위 정정

사용자가 na6tSJ44pWc 소개 영상과 Playground/문서 전수 확인 여부를 물었다. [06](06-source-coverage-and-briefing.md)에 전체 문서/로그인 Workspace/영상은 미확인이고 기능별 공개 문서 조사임을 명시했다. 포털/공모전개발환경/FAQ/공지목록/008본문/005안내, UI/릴리즈/샘플/Console/STT목록을 새 조회했다. 008은 이전 직접URL실패 후 이번 목록링크로 열람 성공했으며 Gleo 별도승인불필요/공개5SDK 준비중무관시험/공모전과입점심사별개를 재확인했다.

영상은 웹에서 제목만 식별,본문대본없음/재조회throttled. 공개metadata/자막 확보용 yt-dlp/youtube-transcript-api를 /tmp에 설치했으나 proxy403으로 실패했다. 쿠키/로그인/차단우회없고원음/화면/자막미확보다. 영상내용이나Q&A를봤다고표시하지않았다. 문서상대링크/whitespace확인 후 기존PR#1에추가하며 앱개발/기기실행/문의발송/자동병합없다.

## 2026-11-15 KST / 11-14 UTC · 제출 가능한 기획·UX 구체화

사용자는 공모전/Playground 근거를 확인해 시연·개발 가능한 차록 기획과 UI/UX를 요청했다. PR #1 open, 원격/로컬 HEAD 9b040a1 일치를 확인하고 기존 docs/conversation-scenario-review 브랜치에서 이어갔다. AGENTS.md는 저장소와 상위 /workspace에 없었다. 이전 도착자리와 앱 코드는 변경하지 않았다.

### 조사와 결정

- 공식 제출 안내·FAQ·DOCX, UI Layout/원칙, Gleo schema/callback, STT intro/마이크 시작/PCM 입력, Vehicle 권한, NaviHelper 목적지를 재조회했다. [조회 목록/실패 범위](evidence/planning-design/sources.md).
- 핵심을 ‘체감 증상과 실제 확보 전후 상태를 정비 상담에 연결’로 구체화했다. 자동 고장 진단·원음·예약·GDS는 첫 시연 필수 의존성에서 제외하되 확장 목표로 보존했다. 문서상 가능과 실제 실행은 구분했다.
- [07](07-submission-plan.md) 공식 작품 소개 1~8항목, [08](08-ux-ui-spec.md) 디자인·대화·저장/권한 상태, [09](09-demo-delivery-plan.md) Cloud/Mac 단계·시연·다음 프롬프트를 작성했다. 기존 01/03에는 최신 문서 우선순위만 덧붙였다.
- 공개 raw PCM 반환을 확인하지 못해 파형 대신 기록 상태와 질문 하나를 사용했다. 세 영역은 시스템 차량 정보+기존 내비+차록 앱 패널의 배치 예시이며 우측 강제 제어를 약속하지 않았다.
- 공식 양식에 프로젝트 소개만 입력한 DOCX 초안을 생성했다. 개인정보·동의·서명은 미작성이고 Word 페이지 렌더링은 미검증이다. 이메일 발송하지 않았다. 선택 APK 제출 시에는 실행 가능한 release와 서명 종류를 검증한다는 안내를 반영했다.

### 실제 산출물과 검사

- [HTML](design/chalog-prototype.html): 준비 동의→기록·대화→순간 표시→저장 상태→상담 카드·인계 검토, 실패/주행 잠금 시안. 실제 SDK·마이크·SQLite·전송 없는 메모리 시뮬레이션.
- Chromium: sandbox socket 오류와 file URL 정책 차단 후 localhost HTTP 방식으로 실행. 정책 변경/인증 우회 없음. 초기 이미지의 화살표 누락과 하단 버튼 여백을 확인·수정하고 최종 화면 7장을 캡처했다. 주요 이미지는 채팅에 직접 공유했다.
- `python scripts/check_design.py`: 6개 UX 검사 묶음 및 1600/1000/430px ×10개 상태, 가로 넘침/기록 화면 스크롤 없음, JS 오류 없음. 이는 브라우저 시안 검사이며 앱 단위/기기 테스트가 아니다.
- `python scripts/make_submission_draft.py /tmp/charok-official-form.docx`: 작품 소개8항목 재조회 일치, 참가자/서명 표 원본 유지. [원본/초안 SHA-256](evidence/planning-design/submission-draft.json).
- Markdown15파일 상대 링크 누락0, JSON 파싱, `git diff --check` 통과. [시안 파일 해시](evidence/planning-design/design-assets.json), [검사 JSON](evidence/planning-design/prototype-checks.json).

### 남은 작업

Android 기록 엔진·Room 복원·실제 SDK/마이크/Gleo·실제 분할/핀·내보낸 파일 전달은 미구현/미검증이다. 다음은 09의 단계②이며, 기기 접근이 가능할 때 단계③을 별도로 검증한다. 공식 접수양식 개인정보·동의·서명, 실제 결과를 반영한 링크/최종 커밋, Word 최종 확인과 제출은 남아 있다. 이번 변경을 PR #1에 커밋·푸시하고 자동 병합하지 않는다.
