# 차록 개발 기록

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
