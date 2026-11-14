# 공개 SDK와 정비 장비 연동 분석

기준일 2026-11-14 UTC. **문서 제공 / 정적 바이너리 확인 / 컴파일 / 실제 SDK 실행 / 실차**는 서로 다른 검증 수준입니다. 차록에서 새 SDK 실행·기기 검증은 아직 하지 않았습니다.

> 후속 조사: [05·대화형 전체 시나리오](05-conversation-scenario-feasibility.md)에서 Gleo 최대3함수/asking/7초timeout, 시스템 창 정책, 원음 미확인, 실제 예약 제약, AAOS 차속 문서 경로를 보완했습니다.

## 1. API·권한 판정

Vehicle 2.3.2 AAR의 정적 조사 결과를 이전 저장소에서 출처와 함께 가져왔습니다. STT/TTS 3.3.1과 FusedLocation 1.1.3은 공식 웹 문서 확인 수준입니다. 차록의 Gradle 의존성/매니페스트는 아직 없습니다.

| 기능 | 공식 API/정적 확인 | 제약과 판정 |
| --- | --- | --- |
| 음성→텍스트 | `SpeechToText.initialize/addListener/request/stop`, `ResultListener` | `STT_SERVICE`. ON_DEVICE는 네트워크·서버용 앱 등록 불필요. 한국어/마이크/최종 결과 미검증 |
| 녹음 PCM→공식 인식 | `sendAudio(ByteArray)`, `completeAudioSend()` | ON_DEVICE 전용. PCM 테스트 성공과 실제 마이크 성공은 별도 |
| 주행거리 | `Odometer.getCurrentDistanceDriven/registerDistanceDriven`, capability 조회 | `CAR_DRIVE`. 단위·정의·실제 수신 확인 필요 |
| 기어 | `Gear.getCurrentGearSelectionState/registerGearSelection` | AAR에서 `CAR_DRIVE`. 기어만으로 안전 조작 상태를 확정하지 않음 |
| 조향각 | `SteeringWheel.getCurrentSteeringWheelAngle/registerSteeringWheelAngle` | AAR에서 `CAR_DRIVE`. 단위/방향/수신 주기 미검증 |
| 브레이크 상태 | `Brake.getCurrentBrakePedalStatus/registerBrakePedalStatus` | `CAR_DRIVE`. 제동압/제동 성능 수치로 확대하지 않음 |
| 일부 경고 시점 조회 | `Safety.getTelltalesInfo` 및 일부 오일/브레이크액 getter | `CAR_SAFETY`. DTC/정밀 진단 API와 다름 |
| 통합 경고 연속 수신 | `checkTelltalesCapabilities/registerTelltales` | **`PRIVATE_CAR_SAFETY`**. 일반 앱 사용 가능으로 가정하지 않고 MVP 제외 |
| 공조 상태 | `HVAC.getHvacAcState/getHvacFanSpeed` 및 관련 callback | `CAR_CLIMATE`. 설정 상태와 실제 냉방 성능은 구분 |
| 연료/EV 충전량 | `Fuel.getFuelPercent`, `EvBattery.getEvBatteryStateOfCharge` | `CAR_ENERGY`. 차종·가용성에 따라 선택 |
| 타이어 공기압 수치 | `getTirePressure`와 관련 capability/callback | **`PRIVATE_CAR_SAFETY`**. MVP 제외 |
| 타이어 보조 getter | `getLowTirePressure` | `CAR_SAFETY`이나 Float 반환. 의미/단위 확인 전 저압 boolean 등으로 사용하지 않음 |
| 위치 구간 | `FusedLocation.isSupported/getLastKnownLocation/registerFusedLocationCallback/unregisterFusedLocationCallback` | `FUSED_LOCATION`. 시각·신뢰도·공백을 보존. 마지막 값이 최신 위치라는 보장 없음 |
| 음성 확인 읽기 | TTS `speak/stop` | ON_DEVICE 문서 제공. 한국어/권한/오디오 경합 실행 미검증 |
| 순정 Gleo에서 앱 명령 | App Schema / Gleo Action / Callback | 문서상 경로 있음. 증상 원문·시작/종료 전달·실제 앱 호출을 시험해야 함 |
| 로컬 보고서/공유 | Android SQLite/PdfDocument/FileProvider/공유 Intent 등 앱 구현 | Pleos의 공유 대상 앱·파일 접근·정비사 단말 열람은 별도 검증 |

Vehicle 대표 도메인 권한만 보고 판단하지 않습니다. 실제 함수의 `RequiresPermission`이 우선이며 PRIVATE 권한을 임의 선언해 해결하거나 우회하지 않습니다. 정적 증거는 [선정 28개 메서드](evidence/sdk-review/vehicle-selected-methods.json)와 [AAR 해시](evidence/sdk-review/binary-inspection.json)에 있습니다.

RPM·DTC·raw CAN·진동·제동압·도로 요철·외부 카메라 프레임은 이 조사에서 차록이 쓸 공개 경로를 확정하지 않았습니다. 차속은 후속 공식 FAQ에서 CarPropertyManager의 PERF_VEHICLE_SPEED 경로를 확인했으며 실제 권한/기기 수신은 미검증입니다. Android 권한 상수의 존재나 다른 앱의 동작만으로 제공을 선언하지 않습니다.

## 2. 전1분 버퍼와 백그라운드의 현실성

callback을 실제 수신하는 앱 실행 상태에서는 시간 제한 순환 버퍼를 앱에서 만들 수 있습니다. SDK가 앱 실행 전의 모든 차량 이력을 제공한다는 뜻은 아닙니다. 프로세스 종료·권한 철회·화면 꺼짐·타 앱 전환·마이크 경합을 각각 시험해야 합니다.

공식 FusedLocation 권한 문서는 Android 일반 위치 권한/포그라운드·백그라운드 정책을 별도로 보장하지 않습니다. Android foreground service만 넣으면 해결된다고 약속하지 않습니다. 초기 시연은 명시 준비와 실제 수신이 확인된 실행 상태로 제한합니다.

## 3. 환경과 AI

위치에 지도·날씨 자료를 연결하는 것은 앱/외부 데이터 작업입니다. 데이터의 이용 권한·시각·정확도·매칭 오류를 검증해야 합니다. 지도상 도로명은 실제 차선/노면 상태 측정과 다릅니다.

공식 STT는 발화를 텍스트로 바꾸며 자동으로 앱 동작을 실행하거나 실제 기계음을 진단하지 않습니다. 첫 버전은 원문+내부 규칙/템플릿으로 충분한지 확인합니다. Gleo function calling은 임의 텍스트 추론을 위한 범용 LLM API가 아닙니다.

공모전 공개 공지는 외부 STT 전송 불가/공식 STT 사용, 미공개 LLM SDK 미제공을 안내합니다. 외부 LLM 요약은 핵심 의존성으로 넣지 않고, 텍스트 전송조차 허용 범위/동의/비식별 정책을 확인한 후 고려합니다.

## 4. 스캐너 연결 즉시 조회

**현재 공개 연동 경로는 미확인입니다.** OBD/GDS가 ECU를 읽는 경로와 인포테인먼트 앱 기록을 읽는 경로는 별개입니다. Pleos 앱 데이터를 ECU에 쓰거나 OBD에 노출하는 기능을 가정하지 않습니다.

정식 자동 조회에는 GDS/GIT/현대 측 인터페이스, 차량·정비 접수건 연결, 고객 공유 동의, 인증/접근 권한, 기록 포맷·단위·시간 정의가 필요합니다. 스캐너 연결과 고객 기록 열람 권한을 동일시하지 않습니다.

첫 버전은 고객이 선택한 기록을 정비사가 별도 뷰어/보고서로 열고 진단 결과와 비교합니다. 임의 JSON을 MSR/GSR로 이름만 바꾸거나 시연용 스캐너 이벤트를 실제 GDS 연결 성공으로 표시하지 않습니다.

## 5. 현대/GDS 기존 기능과 중복

- 현대 리모트케어는 지원 차종·가입 조건에서 이상 감지·알림·상담/정비 연계를 제공합니다.
- GDS Smart 공식 설명에는 recorded data 조회와 VCI II 운행 기록 복사가 있습니다.
- GDS Viewer 안내에는 MSR/GSR 기록을 GDS Smart/GENESIS Smart/KDS 2.0에서 볼 수 있다고 명시돼 있습니다.
- GDS Smart TQR에는 음성·PDF·센서·freeze frame 첨부 기능이 있습니다.

따라서 기록/자료 첨부 자체를 최초 기능이라고 주장하지 않습니다. 위 기능이 Pleos 앱의 사용자 표시 구간을 자동 가져온다는 증거도 아닙니다. 해외 지원 문서를 한국 모든 정비소/차종의 설치·제공 확인으로 확대하지 않습니다. 차록의 차별성은 정비소 방문 전에 운전자가 겪는 간헐적 증상을 보존해 상담으로 연결하는 전체 흐름이며, 효용과 동일 서비스 여부의 추가 검증이 필요합니다.

## 6. 공식 출처

- [STT 소개/모드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions), [마이크 시작·중지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop), [PCM 입력](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data)
- [Vehicle 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [개별 권한 우선 규칙](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [API 도메인](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/entry)
- [FusedLocation](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro), [위치 권한/백그라운드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)
- [TTS](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro), [Gleo Interface](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts)
- [현대 리모트케어](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/preemptive-care)
- [GDS Smart 기능](https://support.gitauto.com/hc/en-gb/articles/4403345307929--HYUNDAI-GDS-Smart-User-Manual), [기록 재생](https://support.gitauto.com/hc/en-us/articles/115010562608-GDS-Viewer-program), [TQR 첨부](https://support.gitauto.com/hc/en-gb/articles/55861362487961-New-Feature-GDS-Smart-TQR-Technical-Quality-Report)
- [공모전 SDK 범위 공지](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000008)

이번 저장소 초기화에서는 위 기존 조사 내용을 이관했으며 새 웹 재조사/SDK 실행을 수행한 것으로 세지 않습니다. 조사 계보와 원본 커밋은 [기록](04-decision-log.md)을 참조합니다.
