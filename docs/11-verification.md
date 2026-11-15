# 차록 구현·검증 현황

## 2026-11-15 Mac Pleos 실행 결과

기준 CI43650320801 APK의 metadata·해시·개발 서명을 대조하고 준비된 Pleos에 설치했습니다. 초기 Compose 크래시를 재현해 b6b7e77에서 로딩 조기 반환을 조건 분기로 수정했습니다. Mac JVM24·lint 오류0·debug/test 빌드, Pleos TEST 기능5건과 별도 새 프로세스 복원1건을 통과했습니다. 수동 준비·기록·상담136개 관측, SQLite 무결성·동일ID·보고서 전체 일치를 확인했습니다. OS 창 손잡이로 차록과 순정 내비를 함께 표시했습니다.

실제 SDK는 정상 권한 승인 후에도 Console/VHAL 비활성으로 차단됐습니다. Console P/속도0 요청은 emulator_inactive, VHAL에는 SecureStorage RPC Permission denied가 남았습니다. 실제 마이크·TTS·Gleo 세 Action·센터 callback/핀·상대 보고서 수신은 미검증입니다. 기존 오디오 크래시 이력에 따라 이번 실행은 no-audio입니다.

화면·검사 로그·수정 APK metadata·후속 절차는 [Mac 검증 증거](evidence/macos-pleos-2026-11-15/README.md)에 있습니다. 기존 기획·DB·AVD는 보존했고 자동 병합은 수행하지 않았습니다.

[실제 TEST 시연 영상](evidence/macos-pleos-2026-11-15/charok-pleos-test-demo.mp4)은89.6초입니다. 수정본 [CI43666974677](https://github.com/leemh-dev/charok/actions/runs/43666974677)도 전체 성공했습니다.

## 이전 기록과 절차

기획 자료와 실제 앱 검증은 이 문서에서 구분합니다. 앱0.1.0/1, 새 패키지 dev.leemh.charok.

최신 앱 코드 **ef85516e89bd74e50dc23038fbcd52b0bfaf55cb**, [CI43650320801 전체 성공](https://github.com/leemh-dev/charok/actions/runs/43650320801). 2026-11-14 UTC / 11-15 KST에 산출물을 내려받아 실제 파일·보고서·화면을 대조했습니다.

- JVM24건, lint 오류0, debug/test/unsigned release 빌드 성공.
- 새 일반 Android API34 x86_64에서 기능5건 성공. 준비/기록/순간/저장/상담, 동일 ID 저장 실패 재시도, 주행 제한, Activity 재생성·background 중단, 연속 명령을 확인했습니다.
- 별도 프로세스2단계: 기록 seed→호스트 force-stop→새 PID 검증 성공. seed는 의도적으로 종료했으므로 ‘JUnit2건 완료’가 아닙니다. PID2657→2730, 같은 ID·설명2건·INTERRUPTED·관측 자동 재개 없음.
- [원본 앱 화면7장과 검증 증거](evidence/android-v0.1/README.md), [APK metadata](evidence/android-v0.1/apk-metadata.json), [실제 파일 재대조](evidence/android-v0.1/apk-crosscheck.json).
- [APK artifact14136300842](https://github.com/leemh-dev/charok/actions/runs/43650320801/artifacts/14136300842). debug/test는 같은 CI 개발 서명, release는 unsigned입니다. artifact 보존14일.

전체 위치: ①기획 보존 완료 → ②기록 엔진/UI/TEST Cloud 검증 완료 → ③실제 SDK 어댑터 컴파일 완료·Mac 실행 미검증 → ④상담 자료 구현·상대 수신/실제 내비 미검증 → ⑤실제 Pleos 시연 영상 추후 제작.

| 기능 | 구현 | 현재 실제 검사 수준 |
| --- | --- | --- |
| 60초 준비 버퍼/같은 세션 | 구현 | JVM:0/20/60초 경계, clock 변경, 지연/누락·타모드 입력, 중복/동시 시작 |
| 저장 실패/종료/복원 | Room/엔진 구현 | JVM+Robolectric SQLite 재열기, 일반 Android 같은ID 재시도·실제 force-stop/새 PID 복원 성공 |
| 차록 Compose UI | 구현 | 일반 Android 기능5건·실제 앱 영역 캡처7장. 좁은 Pleos 창/실제 분할 배치는 Mac에서 미검증 |
| TEST 관측·증상·순간 | 구현 | 일반 Android 조작→실제 SQLite 재조회 성공. 실제 차량/마이크 입력 아님 |
| 실제 차량 데이터 | AAOS+Vehicle 어댑터 | 컴파일 수준. callback 실제 수신/Pleos Console/실차 미검증 |
| 음성 | 공식 STT/TTS·Gleo schema/Intent | 컴파일 수준. 실제 마이크·PCM·Gleo 대화 미검증. Gleo Intent 표시는 발신자 인증 증거 아님 |
| 위치 | 선택 FusedLocation 어댑터 | 컴파일 수준. 기본 OFF, 실제 수신 미검증 |
| 상담 자료 | 요약·관측·보고서 생성·공유 | codec/내용 단위 검사·기기 상담 카드 확인. 실제 공유 UI/상대 수신은 미검증 |
| 센터 내비 | 확인 정보 입력 후 NaviHelper 요청 | 컴파일 수준. 실제 좌표 선택·SDK 이벤트·순정 핀 미검증 |
| 원음/예약/GDS | 미구현 확장 | 실제 지원·제휴 경로 미확인 |
| 영상 | Mac 녹화 명령 준비 | 실제 Pleos 녹화·최종 편집 미실행 |

## 이전 검사 이력

첫 통합 JVM19건·lint 오류0·debug/test/unsigned release 빌드 통과. 후속 코드의 검사/CI 결과는 아래에 추가하며 이전 통과를 후속 기기 성공으로 표시하지 않습니다.

후속 Cloud: 음성 parser/후속 질문3건을 추가해 JVM22건 통과, lint 오류0, debug/test/unsigned release 빌드 통과. 최초 CI43649069111은 폐기된 SDK tools 패키지 설치 단계에서 실패했으며 앱·기기 검사는 미실행. workflow 패키지 지정과 실제 force-stop/새 PID 검사 코드를 보완해 다음 CI에서 확인합니다.

6c9f300 / CI43649588669: 일반 Android API34 기기4건 통과. Gradle 검사 후 앱 제거로 화면 수집/새 PID 검사는 실패했고 실제 캡처 파일을 확보하지 못했습니다. APK 유지 옵션을 적용해 후속 확인합니다. 독립 보완 코드의 Cloud JVM24·lint·debug/test/unsigned release 빌드 통과, 새 반복 명령 기기 검사는 컴파일 수준입니다.

## 해석 제한

- TEST 준비 길이는 앱이 실제로 생성·수신한 TEST 구간이며 차량 과거를 복원한 것이 아닙니다.
- Vehicle.initialize 반환, STT 초기화, Navi 요청 반환을 실제 수신/핀/음성 성공으로 세지 않습니다.
- 1초 조회의 누락·간격은 보고서에서 그대로 유지합니다. 소음·진동 감지/고장 원인 판정 기능은 없습니다.
- 정차 판단은 최근 P와 속도0 근처를 모두 요구합니다. 미수신/오래된 값은 상세 잠금입니다. TEST 정차는 별도 개발 입력입니다.
- 앱이 비활성화되면 기록을 중단합니다. 실제 검증되지 않은 background 지속·자동 재개를 약속하지 않습니다.
