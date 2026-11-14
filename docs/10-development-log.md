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
