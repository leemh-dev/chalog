# 대화형 UX 시나리오 재검토

2026-11-14 UTC · 공식 웹 문서 재조회 · 실제 앱/기기 실행 없음

## 결론

**시나리오의 기록·질문·정리·내비 요청은 공개 인터페이스를 조합해 개발할 근거가 있다. 전체를 그대로 제공한다고 확정할 수는 없다.** 차량 마이크 원음 저장, 글레오와 동시 녹음, 앱의 오른쪽 배치 강제, 현대 정비 예약/자료 접수 자동화는 현재 지원 경로를 확인하지 못했다. 정밀 증상 진단 엔진도 SDK 제공 기능으로 확인하지 못했다.

이번 판정은 문서/API 경로 수준이며 컴파일/에뮬레이터/실차 성공을 뜻하지 않는다. 이전 기획의 목표 UX는 유지하되 구현 시에는 아래 조건을 표시한다.

## 1. 사용자가 요청한 전체 흐름

“증상 기록해 줘” → 차량 정보/내비/차록 3영역 → 기록 시작 안내 → 현재 운행 위치 설명 → 글레오 후속 질문 → 사용자 증상 설명과 ‘지금이야’ 순간 표시 → 실제 소리 녹음 → 증상 취합/방문 권유 → 센터/일시/공유자료 승인 → 예약·기록 전달 → 순정 내비 안내.

| 단계 | 공식 근거와 제한 | 판정/제품 처리 |
| --- | --- | --- |
| 음성으로 앱 실행 | Gleo App Schema의 activity 호출 | 경로 있음. 등록/실제 명령 전달/기존 세션 재진입 검증 필요 |
| 3영역 동시 표시 | 시스템 Driving view + 앱 윈도1~2개 | 시각적 조합의 근거 있음. 앱이 순정 내비·자신의 위치/크기를 강제하는 공개 API는 미확인 |
| “차량 증상을 기록합니다” | Gleo callback 응답/공식 TTS | 경로 있음. 같은 안내를 Gleo와앱TTS가중복재생하지않도록한쪽을선택 |
| 직전1분부터 기록 | 앱의 사전 callback 버퍼 | 준비/실제수신된과거만가능. 앱이처음시작됐으면과거없음 |
| “어디를 운행 중” | FusedLocation + 도로명 자료 연결 | 위치/신뢰도/시각 확인. 좌표가 곧 도로명이 아니며 터널·저신뢰·공백 때 미확인 |
| 대화/추가 질문 | callback `asking`, `message`, `data` | 후속 질문 근거 있음. 무제한 연속 청취/대화 전문 수신 보장 아님 |
| “지금이야” 순간 표시 | 앱 기능으로 mark 저장 | 앱구현가능. 발화와action수신사이지연/모호함표시. 센서가증상발생을검출했다고표시하지않음 |
| 기계음 원음 녹음 | STT 결과 리스너/오디오 입력 문서 확인 | **원음반환/파일저장 경로 미확인**. 동시Gleo/녹음도미검증. 핵심완료에넣지않음 |
| 증상 요약 | 앱 규칙/템플릿, Gleo응답생성 | 가능성이있는앱구현. 사용자원문/관측/추론구분,고장확정하지않음 |
| 의심 원인과 센터 추천 | 정밀진단/정비지식API미확인 | 원인판정대신반복증상에대한상담권유부터. 센터진료항목/차종/가능일확인필요 |
| 승인 후 즉시 예약 | 현대 공식 웹/전화 예약 경로 | **제3자예약 API미확인**. 미래날짜/전화상담조건있음. 요청과확정구분 |
| 업체로 자료 자동 전달 | GDS/업체 공식 접수 API 미확인 | 고객동의기록을자체뷰어로공유하는구현과현대업체접수연동은별도 |
| 순정 내비 안내 | NaviHelper `requestRoute/getDestinationInfo` | 공개경로있음. 요청/SDK결과/순정핀/실제안내별도검증. 사용자선택센터로요청 |

## 2. 창 배치: 3개의 독립 앱이 아니다

공식 Display Layout은 시스템 Driving view와1~2개의앱영역을설명한다. Window Policy는Connect-S에서앱2개1:1,Connect-W에서2:1/1:2비율을설명하며신규앱의primary진입/다른앱이동등시스템정책이있다. 사용자제스처/윈도바조작으로배치가바뀐다.

따라서 디자인은 **시스템 차량 정보 유지 + 순정 내비 + 반응형 차록 패널**로 그린다. 차록은오른쪽전용이아니라시스템이준윈도에서가로/세로를지원해야한다. 음성명령직후정확한위치로분할되는것은실행검증전성공콘티에넣지않는다. 새앱실행이내비를덮거나위치를바꿀수있다는정책을확인한다.

## 3. Gleo 대화: 지원 경로와 새 제약

공식App Schema는최대3개function을허용한다. activity는UI실행,문서의service항목은broadcast전달이다. 후자는서비스를영구실행시켜주는API가아니다. 등록스키마/intent/Callback을구현하고앱이세션상태와다음질문을보관해야한다.

Callback은추가정보나승인요청용`asking`,성공/실패,응답모드와문맥data를제공한다. 문서에는Action이7초후취소되는timeout안내가있다. 녹음종료나긴예약통신까지Action하나를계속붙잡지말고,짧은접수응답과별도처리상태를분리한다. 음성승인도정확한센터/시간/자료/현재요청과결부시킨다.

초기검증은`START_SYMPTOM_RECORD` / `ADD_SYMPTOM_NOTE` / `END_SYMPTOM_RECORD`의3개후보로시작할수있다. 이는공식API명이아니라차록스키마제안이다. ADD에현재순간표시여부와짧은설명을넘길수있는지시험한다. 예약/안내까지음성기능을확대하려면3개제약안에서명확한계약을다시설계해야한다. 함수여러개를무제한등록하거나범용명령함수로묶으면자동으로모든요청을정확히라우팅한다고가정하지않는다.

Gleo대화의전체원문/모든발화시각이앱에자동으로전달된다는근거는없다. 전달된parameter와자체수신한STT결과만출처대로저장한다. Gleo와자체STT를동시에항상켜두는것도지원/오디오경합확인전금지된전제로둔다.

일반문서는스키마검토/승인절차를설명하고,기존공모전공지조사는참가팀별도승인불필요를명시했다. 이번에는공모전공지008URL이웹도구에서열리지않아현재본문을재확인하지못했다. 종전조사와현재플랫폼가이드를구분하고실제schema설치/호출시험이필요하다.

## 4. 실제 소리 녹음: 아직 가장 큰 기술 차단

공식STT ResultListener는텍스트,발화구간byte offset,RMS,시작/종료/오류를설명한다. 원시PCM반환이나오디오파일다운로드callback은확인하지못했다. RMS는dB도아니고재생가능한소리도아니다. 파형/볼륨처럼보이는입력크기표시를원음녹음성공으로해석하지않는다.

`sendAudio()`는앱이이미가진PCM을공식인식기에넣는입력API다. 차량마이크원음을앱이얻는출력API가아니다. 하드웨어직접접근정책때문에모바일의AudioRecord/MediaRecorder/RECORD_AUDIO만으로차량마이크녹음을보장할수없다. 오디오코덱지원또한입력권한의증거가아니다.

지원되더라도STT용음성처리/노이즈억제가작은기계음을보존하는지,차내마이크가차량외부/하부소음을실제로포착하는지,대화/내비TTS/음악이기록을오염시키는지별도평가해야한다. ‘마이크가있다→진단용소리를얻는다’는성립하지않는다.

공식지원경로/허용용도가확인되면‘짧은소리채집→대화재개’의교대모드를먼저검증한다. 현재디자인에서는**순간표시/사용자설명/실제차량관측이기본**,원음은‘지원확인후추가’로둔다. 사전테스트음원을시연할때실제차량녹음이라고표시하지않는다.

## 5. 예약·전달·진단·안내

현대공식웹예약페이지는3일후~6주이내일정,하이테크센터전화예약을안내한다. ARS페이지도상담후배정/안내와사전예약을설명한다. 공개자료에서차록이빈시간조회/예약생성/변경/자료접수할수있는제3자API는확인하지못했다. 이는비공개제휴가능성부정이아니라현재보장근거부재다.

따라서첫버전은센터/예약채널안내와선택기록공유를구현하고,정식연동전예약완료상태를만들지않는다. 자체테스트센터서버로승인→예약요청→자료수신→확정흐름은개발할수있지만**차록테스트센터**이며실제블루핸즈예약성공으로표시하지않는다. 네트워크응답유실은같은요청ID조회로복구하고새예약을반복생성하지않는다.

스캐너/GDS자동조회는02의정식연동미확인판정을유지한다. 보고서를서버에올렸다고센터가읽은것도아니다. 고객공유승인·업로드완료·업체접수·예약확정·업체열람을별도상태로둔다.

SDK에는고장원인/방문긴급도를정확히판단하는정비진단모델이확인되지않았다. 관측/사용자설명요약과상담권유를먼저제공하고,OEM경고나실제전문가안내보다낮은위험으로재판정하지않는다. 향후원인후보는검증된지식/평가가필요하며일반Gleo답변만으로확정하지않는다.

NaviHelper3.0.5는목적지요청·확인·콜백을제공한다. `requestRoute()`정상반환은계산완료가아니다. `onRoutingCompleted`와목적지조회/실제핀을대조한다. 이번문서의가이드버전이기존프로젝트에서실제로쓴버전이라는뜻은아니다. 차록실개발때호환버전을고정한다. 미래예약이면즉시경로를바꾸지말고현재방문할때사용자의선택으로안내한다.

추가발견: 공식FAQ는AAOS `CarPropertyManager`의`PERF_VEHICLE_SPEED`로차속을읽는경로를명시한다. 이전02의차속경로미확인을문서경로확인으로보완한다. 실제권한/기기값/시각수신은아직미검증이며GPS파생속도와구분한다.

## 6. 다음 검증과 중단 조건

1. Gleo시작/증상추가/종료3명령과`asking`후속질문을실제Pleos에서시험한다. 명령이누락되면UI가성공으로넘어가지않아야한다.
2. 60초지원상태수신→시작명령→현재순간추가→종료→실제DB재조회로범위/공백을확인한다.
3. 순정내비와차록동시표시/크기변화/새앱실행정책을확인한다. 오른쪽강제배치API가없으면시스템배치를따르는반응형콘티로고정한다.
4. 원음출력은공식API/정책근거를먼저확보한다. 없으면직접접근우회나가짜녹음을만들지않고옵션미지원으로유지한다.
5. 실제예약API를확보하기전까지센터안내/테스트센터를분리한다. 마지막에는선택센터NaviHelper요청/콜백/핀을대조한다.

**최소실행시나리오:** 사전준비→음성앱호출→가능한시스템분할→대화형설명/순간표시+차량상태구간→저장/요약→상담/예약채널안내→선택기록공유→사용자가원할때순정내비.

이흐름은제품방향을유지하는추천안이며원음녹음/실제예약확정을요구에서삭제했다는뜻은아니다. 두기능은지원근거를확보해야하는차단/확장항목으로둔다.

## 7. 공식 근거

| 자료 | 이번에 확인한 내용 |
| --- | --- |
| [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) | Driving view/앱윈도1~2개 |
| [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) | 기종별분할/primary진입/사용자배치변경 |
| [Gleo App Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability) | 최대3개함수/입력계약/호출방식 |
| [Gleo Callback](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback) | asking/응답모드/문맥/7초timeout |
| [전달/설치](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema) | assets/schema.json/intentfilter/등록설명 |
| [STT 결과](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners) | 텍스트/EPD/RMS,원음반환미확인 |
| [PCM 입력](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data) | 앱→인식기입력방향 |
| [하드웨어 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture) | 공식플랫폼경로/직접접근제한 |
| [BG→CCH](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/bg-to-cch-transition-guide) | 실제사용기능없는FGS유지금지. 단순BG만으로종료판정하지않음 |
| [위치](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro) | 위치/시각/신뢰도/마지막위치한계 |
| [FAQ/차속](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq) | AAOS차속경로/코덱은녹음권한근거아님 |
| [NaviHelper 목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations) | requestRoute/조회/최종콜백 |
| [현대 웹 예약](https://www.hyundai.com/kr/ko/service-membership/service-network/service-reservation-search/service-network-reservation) | 미래예약범위/하이테크전화예약 |
| [현대 ARS](https://www.hyundai.com/kr/ko/service-membership/service-network/service-reservation-center) | 상담/배정/사전예약 |

문서조사·상대링크/whitespace검사만수행했다. 앱코드/실기기/실제예약/자료전송/녹음/제조사문의는실행하지않았다. 추측한STT event-listeners URL은열리지않아공식 result-listeners 경로로보완했다. 공모전공지008본문은이번재조회실패로현재답변의새근거로쓰지않았다.
