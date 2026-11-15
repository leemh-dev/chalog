# 차록 개발 기록

## 2026-11-15 · 0.3.1 백그라운드 명령·토스트·보고서 수정

[전체 흐름과12개 재검증](19-background-fixes-and-reverification.md): 최초 동의/매 기록 시작을 분리하고 CONTROL/ADD Broadcast·전용 OPEN을 구현했습니다. 권한 없는 앱 호출 차단, 짧은 시스템 토스트 요청, 동의 행 조작, 보고서 저장/공유 결과 안내를 추가했습니다. portrait 검은 여백은 제거했습니다.

코드 **8c8f793 / 0.3.1/7**. Mac JVM39·lint 오류0/경고14·Pleos 기기15·새 PID 복원, [CI43701282754](https://github.com/leemh-dev/charok/actions/runs/43701282754) JVM39·일반 Android 기기15·복원 모두 통과했습니다. 최초77개 전체 행 보존, 최종118개·무결성ok. [실제 화면·APK·CI·DB 증거](evidence/macos-pleos-gleo-fixes-2026-11-15/README.md).

**실제 Gleo 발화/등록·차량/마이크 SDK·음악 재생·차록 SDK 목적지·자동 우측 지정·토스트 실제 노출은 별도 미검증/차단입니다.** 순정 내비340m 안내는 동작하지만 지도 바탕은 비어 있습니다. 우측 화면은 OS 수동 배치, 외부 보고서 수신은 별도 TEST 앱 검사입니다. 영상·자동 병합은 하지 않았습니다. 다음 검증은 이 제한을 정상 환경에서 해결한 뒤 같은12개 기준으로 진행합니다.

## 2026-11-15 · 전체 서비스12개 시나리오 검토

[전체 흐름도·번호순 시나리오·판정/수정 우선순위](18-service-scenario-review.md)를 기준으로 기존0.2.3/5를 실제 Pleos에서 확인했습니다. 알림 TEST 기록·동일 ID·59.23초 사전 구간·설명/순간/경고등·백그라운드/종료·상담/보고서가 확인됐고, Mac 기기9개와 새 PID 복원도 통과했습니다. 기존66개 전체 보존, 최종77개·무결성ok. [실제 화면11장·검사·DB 증거](evidence/macos-pleos-scenario-2026-11-15/README.md).

**제품 전체 시나리오는 미완료입니다.** Gleo 최초 시작은 여전히 수동 준비가 필요하고 전용 OPEN/Broadcast·자동 우측 열기가 없습니다. 실제 차량/마이크/Gleo·차록 SDK 목적지는 별도 미검증, 음악은 Bluetooth 연결 해제, 외부 공유는 수신 앱 없음으로 차단됐습니다. 새 앱 코드·APK·영상은 만들지 않았습니다.

## 2026-11-15 · 0.2.3 CHALOG와 순정 내비

영문 CHALOG / 한글 차록으로 헤더·보고서·영문 응답을 바꿨습니다. 앱 식별자와 DB는 유지했고 기존66개 전체 행이 정확히 동일합니다. 코드 **1ed571d / 0.2.3/5**. Mac JVM31·lint 오류0/경고14, [CI43690911090](https://github.com/leemh-dev/charok/actions/runs/43690911090) 일반 Android 기기9·새 PID 복원까지 통과했습니다.

**순정 내비 직접 조작으로 충전소 검색·340m 경로 안내 시작·종료를 확인했습니다.** 경로선/회전 안내/목적지 핀은 나오지만 배경 도로·건물은 비어 있습니다. SwiftShader 비교는 검은 화면 오류가 나 기존 host로 복귀했으며 지도 바탕의 원인은 미확정입니다. 차록 SDK 목적지 요청, 실제 주행·마이크·Gleo 성공으로 확대하지 않습니다. [브랜드/조사 상세](17-chalog-and-navigation.md) · [실제 화면·APK·DB·검증 증거](evidence/macos-pleos-navigation-2026-11-15/README.md). 영상은 만들지 않았습니다.

## 2026-11-15 · 0.2.2 경고·표시등 관측

SIGNAL 기록 요약·상담 카드·보고서에 경고·표시등 이름, 최초 관측 시각/출처, 점등·점멸·소등 이력을 추가했습니다. 누락/조회 실패는 미확인으로 남깁니다. 공개 CAR_SAFETY getter를 최대5초 간격으로 연결했고 private 구독 API는 사용하지 않았습니다. DB 스키마는 그대로입니다. [명세](16-warning-light-observations.md).

코드 **79d93a1 / 0.2.2/4**. Mac JVM31·lint 오류0/경고14·Pleos 개별 기기9건, [CI43689445105](https://github.com/leemh-dev/charok/actions/runs/43689445105) 일반 Android 기기9건·새 PID 복원 모두 통과했습니다. 기존56개 payload 전체 보존, 최종DB66개·무결성ok. 실제 Pleos에서 TEST 점등→소등→저장·상담 화면을 확인했습니다. CAR_SAFETY 정상 부여는 확인했지만 실제 SDK 경고 수신은 미검증입니다. 영상은 만들지 않았습니다. [실제 화면4장·검사·DB·APK 증거](evidence/macos-pleos-warnings-2026-11-15/README.md).

## 2026-11-15 · 0.2.1 SIGNAL 구현과 실제 크기 검토

선택된 A · SIGNAL을 흰색/코발트 우측 패널에 구현했습니다. 제목26sp·본문18sp·보조16sp·시간38sp·버튼 최소52dp로 맞추고 설명·시간/순간 요약·번호 타임라인·명시적 닫기를 적용했습니다. 기존 Pleos 2560×1440/density160에서 시스템 배율을 바꾸지 않고 준비·기록·저장·상담 화면을 확인했습니다. 우측 배치는 OS 수동 조작이며 Gleo/자동 창 지정 구현이 아닙니다.

앱 코드 **31ba254**, 버전 **0.2.1/3**. Mac JVM24·lint 오류0/경고14·Pleos 개별 기기8건 통과, [CI43683208769](https://github.com/leemh-dev/charok/actions/runs/43683208769) 일반 Android 기기8건과 새 PID 복원도 통과했습니다. 기존47개 payload 전체 보존, 최종DB56개·무결성ok입니다. 화면을 닫아도 TEST 기록 유지, 알림 종료/저장 뒤 대시보드 미표시를 확인했습니다.

**이번에는 영상을 만들지 않았습니다.** 다음 영상은 [한국어 장면 자막과 TEST/실제 입력 구분](09-demo-delivery-plan.md)을 따릅니다. 실제 SDK·마이크·Gleo·순정 내비 목적지 성공은 별도 미검증/차단입니다. [실제 화면8장·APK·DB·검사 증거](evidence/macos-pleos-signal-2026-11-15/README.md).

## 2026-11-15 · Gleo 중심 방향 정정과 디자인만 작성

공식 Pleos 사진에서 왼쪽 차량정보 / 오른쪽 전체 내비 및 왼쪽 차량정보 / 중앙 내비 / 오른쪽 음악 구조를 확인했습니다. 사용자 요청에 따라 기록 시작·종료에는 현재 화면을 유지하고 시스템 알림만 표시하며, ‘차록 열어줘’만 우측 앱 패널을 사용하는 목표를 [docs/15](15-gleo-background-direction.md)에 고정했습니다.

공식 Gleo 문서의 UI 없는 service/Broadcast 전달을 확인해 후속 개발 후보로 기록했습니다. 실제 발화 분기·FGS 시작·우측 지정은 미검증입니다. [흐름 이미지와 우측 패널 A/B/C](design/gleo-background/README.md)를 생성하고 검토했습니다. 앱 소스·schema·manifest는 수정하지 않았고 빌드·기기 실행·DB 변경도 하지 않았습니다.

## 2026-11-15 · 0.2 흰색 UI와 백그라운드 기록

흰색 테마·상태 카드·접힌 TEST 도구를 적용하고, 순정 내비 가운데/차록 오른쪽의 실제 OS 배치를 확인했습니다. 명시적으로 준비한 기록은 앱 화면을 닫아도 유지되고 알림에서 시작·순간 표시·종료할 수 있습니다. 화면 재진입은 같은 ID를 유지하며 프로세스 강제 종료 뒤에는 자동 수집하지 않습니다.

Mac JVM24·lint 오류0, Pleos 기기8개 메서드 개별 실행·별도 새 프로세스 복원1건 및 최종 CI가 통과했습니다. 초기 화면 닫기 방식과 검사 타이밍 문제를 수정했고 원본 실패 로그를 보존했습니다. 기존 8개 기록 payload 전체가 동일하며 최종 DB 47개·무결성ok입니다. 실제 SDK·마이크·Gleo·센터 목적지 연동은 환경 차단/미검증 상태를 유지합니다.

[실제 화면·알림·영상·APK·검증 증거](evidence/macos-pleos-v0.2-2026-11-15/README.md) · [0.2 사용/구현 원칙](14-background-and-white-ui.md)


## 2026-11-15 Mac Pleos 실행 결과

기준 CI43650320801 APK의 metadata·해시·개발 서명을 대조하고 준비된 Pleos에 설치했습니다. 초기 Compose 크래시를 재현해 b6b7e77에서 로딩 조기 반환을 조건 분기로 수정했습니다. Mac JVM24·lint 오류0·debug/test 빌드, Pleos TEST 기능5건과 별도 새 프로세스 복원1건을 통과했습니다. 수동 준비·기록·상담136개 관측, SQLite 무결성·동일ID·보고서 전체 일치를 확인했습니다. OS 창 손잡이로 차록과 순정 내비를 함께 표시했습니다.

실제 SDK는 정상 권한 승인 후에도 Console/VHAL 비활성으로 차단됐습니다. Console P/속도0 요청은 emulator_inactive, VHAL에는 SecureStorage RPC Permission denied가 남았습니다. 실제 마이크·TTS·Gleo 세 Action·센터 callback/핀·상대 보고서 수신은 미검증입니다. 기존 오디오 크래시 이력에 따라 이번 실행은 no-audio입니다.

화면·검사 로그·수정 APK metadata·후속 절차는 [Mac 검증 증거](evidence/macos-pleos-2026-11-15/README.md)에 있습니다. 기존 기획·DB·AVD는 보존했고 자동 병합은 수행하지 않았습니다.

[실제 TEST 시연 영상](evidence/macos-pleos-2026-11-15/charok-pleos-test-demo.mp4)은89.6초입니다. 수정본 [CI43666974677](https://github.com/leemh-dev/charok/actions/runs/43666974677)도 전체 성공했습니다.

## 이전 기록과 절차

## 2026-11-15 KST / 11-14 UTC · Android 첫 구현

요청: 기획을 보존하고 실제 개발을 이어간다. 추후 Mac Pleos에서 실제 동작 화면을 녹화해 시연 영상을 만든다. 기존 기획 PR #1은 open/3f728dd이며 새 `feat/charok-recorder`를 그 커밋에서 분기했다. 기획 문서와 기존 도착자리 앱은 변경하지 않았다.

### 환경과 조사

- `/workspace` Cloud Linux, JDK17, Android compile35/target34/min28, Gradle8.10.2. /dev/kvm 없음.
- sandbox ADB server socket 차단 후 허용된 실행 권한으로 `adb devices -l` 확인: 연결 기기 없음. Mac 원격 endpoint 도구 없음. 기기 부팅/설치/녹화는 하지 않음.
- 공식 Gleo 예제, STT/FusedLocation 문서와 배포 AAR 시그니처를 확인했다. 기어 getter의 deprecation을 컴파일에서 확인하여 AAOS GEAR_SELECTION/PERF_VEHICLE_SPEED로 보완했다.
- Gradle 직접 연결 대기→Cloud 지정 프록시 사용→TLS PKIX 오류→시스템 Java trustStore(`/etc/ssl/certs/java/cacerts`) 적용으로 해결. 인증서 검증을 끄지 않았다. kapt marker 해석은 Kotlin 플러그인 모듈에 연결해 해결했다. 이 프록시/호스트 설정은 프로젝트에 강제하지 않았다.

### 구현

- Kotlin/Compose 신규 앱0.1.0/1, 패키지 dev.leemh.charok. 기획의 dark/mint/blue 차록 패널. 시스템 차량/내비를 복제하지 않고 앱 창만 구현.
- 60초 실제 수신 버퍼, monotonic 경계, 동일 세션 시작/표시/종료, Room SQLite checkpoint/commit/재조회, 프로세스 복원 시 중단 표시·자동 수집 OFF.
- 저장 실패 시 관측 중지·동일 ID 재시도. TEST와 SDK 출처 분리, 원문과 Gleo parameter 구분, 누락을 정상 값으로 채우지 않음. 기록 상한10분/6000관측/200설명 및10MiB payload.
- 차량 입력: TEST는 현재 시각부터 생성, 과거60초를 즉시 채우지 않음. 실제 어댑터는 AAOS 속도/기어와 공개 Vehicle 조향/브레이크/지원 좌석 공조, 선택 FusedLocation. 전체 연속 ECU 로그가 아닌 1초 간격 조회 관측이며 필드별 가용 상태/수신 시각 보존.
- 공식 STT/TTS ON_DEVICE와 Gleo3Action/schema/callback. 실제 마이크·Gleo 자연어 매핑은 기기 검증 전. 원음 녹음/외부 STT 없음.
- 정차 후 상담 카드/타임라인/선택 보고서 생성·재조회·Sharesheet. 기본 위치 제외. 실제 업체 수신/예약 미연결. 공식 위치를 사용자가 확인한 센터만 NaviHelper 요청하며 호출/이벤트/실제 핀을 구분.
- ADB 준비/설치 직전 재확인, Mac 실제 screenrecord 스크립트, 일반 Android 전용 CI와 화면/DB 증거 수집 추가.

### 검사와 수정

초기 core18건 통과, Room 검사1건은 앱 Application 클래스 작성 중 빌드되어 class missing 실패. 클래스 포함 후 JVM19건 모두 통과했다. SDK nullable 기어 컴파일 오류는 null→미수신으로 수정하고 조향/브레이크/공조도 같은 원칙으로 보완했다.

첫 통합 `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease` 성공. lint 오류0이며 target/dependency/선택 아이콘 등 경고는 별도 보고서에 남았다. 이후 AAOS와 실제 기기 검사 코드 보완 결과는 후속 기록에 추가한다. 이 시점의 성공은 실제 Pleos 기기 성공이 아니다.

### 남은 검증

CI 일반 Android TEST 흐름과 실제 앱 캡처, 새로운 PID 복원, Mac Pleos 실제 SDK/마이크/Gleo/분할/내비 핀/보고서 전달. APK는 생성 파일마다 metadata를 남긴다. debug/test는 개발 서명, release는 미서명이며 실제 제출용 실행 가능한 signed release가 아니다. 시연 영상은 이번에 제작하지 않는다.

AAOS 보완 후 동일 통합 명령 재실행은 48초에 성공했다. JVM19건, lint 오류0, debug/test/unsigned release APK 생성. workflow는 actionlint1.7.7 정적 검사 통과. CI의 실제 기기 결과는 아직 대기이며 이번 기능 커밋과 함께 실행한다.

### 첫 CI 환경 실패와 후속 검증 보완

기능954dc0c / PR #2 / CI43649069111은 setup-android 기본값이 폐기된 `sdkmanager tools`를 요청해 package not found로 실패했다. build·에뮬레이터·기기 검사는 시작되지 않았다. 지원 패키지(platform-tools/platform35/build-tools35)를 명시해 수정했다. 실패한 실행을 앱 실패나 Pleos 실패로 세지 않는다.

일반 Android 전용 force-stop/새 PID 검사2단계를 추가했다. seed marker만 교체하고 DB를 보존하며, 실행 중 기록 상태에서 종료 후 같은ID/설명/관측 유지·자동 수집 없음 확인을 목표로 한다. 추가 음성 parser/후속 질문 검사를 작성하고 CAR_SPEED 정상 런타임 권한 요청과 앱 아이콘을 보완했다. 후속 실제 결과는 다음 기록에 남긴다.

후속 Cloud 통합 검사: JVM22건·lint 오류0·debug/test/unsigned release 성공. 코드 검토에서 이전 STT 세션의 늦은 callback과 초기화 중 TTS가 다음 STT 위에 재생될 가능성을 발견해 세대별 callback 차단과 TTS 참조 해제로 보완했다. 이는 실제 기기에서 재현된 오류가 아닌 코드 검토 수정이며 음성 실행 성공으로 세지 않는다. 이 변경 후 통합 명령은24초에 통과했다.

### 일반 Android 4건 통과 / 후속 증거 수집 실패

6c9f300의 CI43649588669는 SDK 설치·JVM22·lint·빌드를 통과했고 새 API34 AVD의 transport/boot/package/activity 준비 후 connectedDebugAndroidTest 4건을 통과했다. 이후 run-as unknown package로 프로세스 복원 seed 시작이 실패했다. Gradle 기본 정리로 APK가 제거돼 앱 전용 화면 파일도 남지 않았다. 화면을 확보하지 못했으며 앱 화면 캡처를 보여줬다고 기록하지 않는다.

AGP8.8.0 BooleanOption javap에서 `android.injected.androidTest.leaveApksInstalledAfterRun`을 확인해 true로 설정하고 `uninstallIncompatibleApks=false`도 명시했다. 기존 데이터를 지우는 재설치로 해결하지 않는다. 후속 새 전용 AVD에서 실제 화면/새 PID 검사를 이어간다.

독립 보완: 코루틴이 Main 전환에서 중단될 때 제어 작업이 엇갈릴 가능성을 코드 검토로 확인해 Mutex로 작업과 tick을 직렬화했다. 원문 보존 사용자 보완을 추가하고 저장 실패 시 재시도 화면으로 전환한다. latest 조회는 모든 기록을 읽지 않고 1건만 조회한다. 추가2건 포함 JVM24·lint·세 APK 빌드 통과(39초), 반복 명령 기기 검사 추가 컴파일 통과(12초). 문서만 바뀌면 기존 앱 검사를 불필요하게 반복하지 않도록 CI paths를 지정했다.

### 최신 CI 전체 성공과 증거 확보

기능/제어 보완 커밋 **ef85516**, CI **43650320801**, artifact **14136300842**. 새 일반 Android API34 AVD에서 `bash scripts/ci_device.sh` 실행: ADB transport=device·boot1·package/activity found와 모든rc0, 설치 직전 재확인 성공. JVM24·lint 오류0·debug/test/unsigned release 성공, connectedDebugAndroidTest 기능5건 성공. 이어 process_recovery.py의 seed/force-stop/새 PID 검증 성공. 같은 세션525d092f-f572-43c9-a06f-5e15e60ee0c4, PID2657→2730, 설명2건·INTERRUPTED·관측 수 유지. seed는 의도적으로 강제종료하며 별도의 완료된 JUnit2건으로 세지 않는다.

APK 유지 옵션으로 이전 증거 수집 실패를 해결했다. 실제 native Compose 앱 영역 캡처7장과 session-proof/process-recovery JSON을 확보하고 기록/저장/준비/상담/실패/주행 제한 화면을 채팅에 즉시 표시했다. 이전 HTML 시안을 새 앱 캡처로 사용하지 않았다. 화면은 TEST 입력이고 시스템 바/순정 내비는 포함하지 않는다. 실제 주행 상세 잠금 시험에서 수집 이전 종료한 기록은 관측0건이며 가짜 데이터를 넣지 않았다.

artifact ZIP 다운로드는 GitHub CLI의 저장소 blob 경로403으로 막혔다. 연결된 GitHub artifact 다운로드 도구의 파일 전달 경로로 받았으며 임시 URL은 공개 증거에 넣지 않았다. 실제 APK3개 SHA256·version·apksigner 결과를 CI metadata와 재대조했다. 로컬 apksigner35와 CI 도구의 `Signer #1`/`V2 Signer` 출력 접두어 차이로 전체 문자열 비교가 실패했으나 certificate SHA256과 검증rc는 동일했다. 해시/인증서 필드 단위로 대조해 일치 확인했다. 공개 metadata의 인증서 지문은 공개키 지문이며 개인키를 포함하지 않는다.

증거: docs/evidence/android-v0.1/{README.md,ci-43650320801-summary.json,apk-metadata.json,apk-crosscheck.json,screens/}. APK코드ef85516·0.1.0/1·개발서명/unsigned를 구분한다. 문서/증거 커밋에는 앱 변경이 없으므로 동일 검사를 반복하지 않는다. PR #2에 결과·차단·다음 작업을 반영한다.

남은 작업: Mac 외장SSD에서 기존 Pleos 환경/자원 확인→같은 artifact debug APK 준비·설치→실제 SDK/Console/STT/Gleo/순정 내비 핀/보고서 전달→실제 화면 녹화. Cloud에서 Mac 접속/녹화를 수행하지 않았고 원음·자동예약·GDS는 미연결 확장이다. 다음 작업 프롬프트는 docs/13에 저장하고 채팅에도 제공한다. 기획 DOCX는 기존 초안으로 유지하며 제출/서명/개인정보 작성은 수행하지 않았다.
