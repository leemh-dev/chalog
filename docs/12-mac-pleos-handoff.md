# Mac Pleos 설치·실행·녹화 인계

## 2026-11-15 · CHALOG 이름 변경

저장소는 `leemh-dev/chalog`, 소스 namespace는 `dev.leemh.chalog`, 버전은0.3.2/8입니다. 개발 브랜치·설치 ID·DB는 유지합니다. Mac JVM40·Pleos16·새 PID 복원과 CI43707910220을 통과했고 기존119개 모든 행을 보존했습니다(최종133개, 추가14개 TEST). AVD 정상 종료·자동 병합 없음. [현재 변경·검증 상태](21-chalog-rename.md)를 먼저 확인하세요. 아래 항목은 각 버전의 검증 이력입니다.

## 2026-11-15 · 한글 자막 시연 제작·최종 검증

사용자 요청에 따라 [2분39초 실제 Pleos TEST 시연](evidence/macos-pleos-captioned-demo-2026-11-15/README.md)을 만들었습니다. 좌측 상단 시나리오6개와 하단 한국어 자막17구간을 영상에 넣었습니다. 전체3,820프레임 디코딩·한글 제목/자막 OCR17/17·SRT 타이밍 대조·주요 장면 시각 검토를 통과했습니다. [시나리오·검증 상세](20-captioned-scenario-demo.md).

기존0.3.1/7 APK를 재설치/변경하지 않았습니다. 기존118개 모든 행 보존, 시연 TEST 저장1개 추가 →119개·무결성ok. 동일ID·사전59.831초·저장 후987건 고정·새 PID 재실행 후 자동 수집 없음 확인. 실제 Gleo/차량 SDK/마이크·자동 우측 배치·토스트 성공으로 표시하지 않습니다. 정상 종료한 AVD의 데이터를 유지하고 이후 렌더링했습니다. 자동 병합 없음.

## 2026-11-15 · 0.3.1 백그라운드 명령·토스트·보고서 수정

[전체 흐름과12개 재검증](19-background-fixes-and-reverification.md): 최초 동의/매 기록 시작을 분리하고 CONTROL/ADD Broadcast·전용 OPEN을 구현했습니다. 권한 없는 앱 호출 차단, 짧은 시스템 토스트 요청, 동의 행 조작, 보고서 저장/공유 결과 안내를 추가했습니다. portrait 검은 여백은 제거했습니다.

코드 **8c8f793 / 0.3.1/7**. Mac JVM39·lint 오류0/경고14·Pleos 기기15·새 PID 복원, [CI43701282754](https://github.com/leemh-dev/chalog/actions/runs/43701282754) JVM39·일반 Android 기기15·복원 모두 통과했습니다. 최초77개 전체 행 보존, 최종118개·무결성ok. [실제 화면·APK·CI·DB 증거](evidence/macos-pleos-gleo-fixes-2026-11-15/README.md).

**실제 Gleo 발화/등록·차량/마이크 SDK·음악 재생·차록 SDK 목적지·자동 우측 지정·토스트 실제 노출은 별도 미검증/차단입니다.** 순정 내비340m 안내는 동작하지만 지도 바탕은 비어 있습니다. 우측 화면은 OS 수동 배치, 외부 보고서 수신은 별도 TEST 앱 검사입니다. 영상·자동 병합은 하지 않았습니다. 다음 검증은 이 제한을 정상 환경에서 해결한 뒤 같은12개 기준으로 진행합니다.

## 2026-11-15 · 전체 서비스12개 시나리오 검토

[전체 흐름도·번호순 시나리오·판정/수정 우선순위](18-service-scenario-review.md)를 기준으로 기존0.2.3/5를 실제 Pleos에서 확인했습니다. 알림 TEST 기록·동일 ID·59.23초 사전 구간·설명/순간/경고등·백그라운드/종료·상담/보고서가 확인됐고, Mac 기기9개와 새 PID 복원도 통과했습니다. 기존66개 전체 보존, 최종77개·무결성ok. [실제 화면11장·검사·DB 증거](evidence/macos-pleos-scenario-2026-11-15/README.md).

**제품 전체 시나리오는 미완료입니다.** Gleo 최초 시작은 여전히 수동 준비가 필요하고 전용 OPEN/Broadcast·자동 우측 열기가 없습니다. 실제 차량/마이크/Gleo·차록 SDK 목적지는 별도 미검증, 음악은 Bluetooth 연결 해제, 외부 공유는 수신 앱 없음으로 차단됐습니다. 새 앱 코드·APK·영상은 만들지 않았습니다.

## 2026-11-15 · 0.2.3 CHALOG와 순정 내비

영문 CHALOG / 한글 차록으로 헤더·보고서·영문 응답을 바꿨습니다. 앱 식별자와 DB는 유지했고 기존66개 전체 행이 정확히 동일합니다. 코드 **1ed571d / 0.2.3/5**. Mac JVM31·lint 오류0/경고14, [CI43690911090](https://github.com/leemh-dev/chalog/actions/runs/43690911090) 일반 Android 기기9·새 PID 복원까지 통과했습니다.

**순정 내비 직접 조작으로 충전소 검색·340m 경로 안내 시작·종료를 확인했습니다.** 경로선/회전 안내/목적지 핀은 나오지만 배경 도로·건물은 비어 있습니다. SwiftShader 비교는 검은 화면 오류가 나 기존 host로 복귀했으며 지도 바탕의 원인은 미확정입니다. 차록 SDK 목적지 요청, 실제 주행·마이크·Gleo 성공으로 확대하지 않습니다. [브랜드/조사 상세](17-chalog-and-navigation.md) · [실제 화면·APK·DB·검증 증거](evidence/macos-pleos-navigation-2026-11-15/README.md). 영상은 만들지 않았습니다.

## 2026-11-15 · 0.2.2 경고·표시등 관측

SIGNAL 기록 요약·상담 카드·보고서에 경고·표시등 이름, 최초 관측 시각/출처, 점등·점멸·소등 이력을 추가했습니다. 누락/조회 실패는 미확인으로 남깁니다. 공개 CAR_SAFETY getter를 최대5초 간격으로 연결했고 private 구독 API는 사용하지 않았습니다. DB 스키마는 그대로입니다. [명세](16-warning-light-observations.md).

코드 **79d93a1 / 0.2.2/4**. Mac JVM31·lint 오류0/경고14·Pleos 개별 기기9건, [CI43689445105](https://github.com/leemh-dev/chalog/actions/runs/43689445105) 일반 Android 기기9건·새 PID 복원 모두 통과했습니다. 기존56개 payload 전체 보존, 최종DB66개·무결성ok. 실제 Pleos에서 TEST 점등→소등→저장·상담 화면을 확인했습니다. CAR_SAFETY 정상 부여는 확인했지만 실제 SDK 경고 수신은 미검증입니다. 영상은 만들지 않았습니다. [실제 화면4장·검사·DB·APK 증거](evidence/macos-pleos-warnings-2026-11-15/README.md).

## 2026-11-15 · 0.2.1 SIGNAL 구현과 실제 크기 검토

선택된 A · SIGNAL을 흰색/코발트 우측 패널에 구현했습니다. 제목26sp·본문18sp·보조16sp·시간38sp·버튼 최소52dp로 맞추고 설명·시간/순간 요약·번호 타임라인·명시적 닫기를 적용했습니다. 기존 Pleos 2560×1440/density160에서 시스템 배율을 바꾸지 않고 준비·기록·저장·상담 화면을 확인했습니다. 우측 배치는 OS 수동 조작이며 Gleo/자동 창 지정 구현이 아닙니다.

앱 코드 **31ba254**, 버전 **0.2.1/3**. Mac JVM24·lint 오류0/경고14·Pleos 개별 기기8건 통과, [CI43683208769](https://github.com/leemh-dev/chalog/actions/runs/43683208769) 일반 Android 기기8건과 새 PID 복원도 통과했습니다. 기존47개 payload 전체 보존, 최종DB56개·무결성ok입니다. 화면을 닫아도 TEST 기록 유지, 알림 종료/저장 뒤 대시보드 미표시를 확인했습니다.

**이번에는 영상을 만들지 않았습니다.** 다음 영상은 [한국어 장면 자막과 TEST/실제 입력 구분](09-demo-delivery-plan.md)을 따릅니다. 실제 SDK·마이크·Gleo·순정 내비 목적지 성공은 별도 미검증/차단입니다. [실제 화면8장·APK·DB·검사 증거](evidence/macos-pleos-signal-2026-11-15/README.md).

## 2026-11-15 · 0.2 흰색 UI와 백그라운드 기록

흰색 테마·상태 카드·접힌 TEST 도구를 적용하고, 순정 내비 가운데/차록 오른쪽의 실제 OS 배치를 확인했습니다. 명시적으로 준비한 기록은 앱 화면을 닫아도 유지되고 알림에서 시작·순간 표시·종료할 수 있습니다. 화면 재진입은 같은 ID를 유지하며 프로세스 강제 종료 뒤에는 자동 수집하지 않습니다.

Mac JVM24·lint 오류0, Pleos 기기8개 메서드 개별 실행·별도 새 프로세스 복원1건 및 최종 CI가 통과했습니다. 초기 화면 닫기 방식과 검사 타이밍 문제를 수정했고 원본 실패 로그를 보존했습니다. 기존 8개 기록 payload 전체가 동일하며 최종 DB 47개·무결성ok입니다. 실제 SDK·마이크·Gleo·센터 목적지 연동은 환경 차단/미검증 상태를 유지합니다.

[실제 화면·알림·영상·APK·검증 증거](evidence/macos-pleos-v0.2-2026-11-15/README.md) · [0.2 사용/구현 원칙](14-background-and-white-ui.md)


## 2026-11-15 Mac Pleos 실행 결과

기준 CI43650320801 APK의 metadata·해시·개발 서명을 대조하고 준비된 Pleos에 설치했습니다. 초기 Compose 크래시를 재현해 b6b7e77에서 로딩 조기 반환을 조건 분기로 수정했습니다. Mac JVM24·lint 오류0·debug/test 빌드, Pleos TEST 기능5건과 별도 새 프로세스 복원1건을 통과했습니다. 수동 준비·기록·상담136개 관측, SQLite 무결성·동일ID·보고서 전체 일치를 확인했습니다. OS 창 손잡이로 차록과 순정 내비를 함께 표시했습니다.

실제 SDK는 정상 권한 승인 후에도 Console/VHAL 비활성으로 차단됐습니다. Console P/속도0 요청은 emulator_inactive, VHAL에는 SecureStorage RPC Permission denied가 남았습니다. 실제 마이크·TTS·Gleo 세 Action·센터 callback/핀·상대 보고서 수신은 미검증입니다. 기존 오디오 크래시 이력에 따라 이번 실행은 no-audio입니다.

화면·검사 로그·수정 APK metadata·후속 절차는 [Mac 검증 증거](evidence/macos-pleos-2026-11-15/README.md)에 있습니다. 기존 기획·DB·AVD는 보존했고 자동 병합은 수행하지 않았습니다.

[실제 TEST 시연 영상](evidence/macos-pleos-2026-11-15/charok-pleos-test-demo.mp4)은89.6초입니다. 수정본 [CI43666974677](https://github.com/leemh-dev/chalog/actions/runs/43666974677)도 전체 성공했습니다.

## 이전 기록과 절차

현재 Cloud에서 Mac에 접속하거나 Pleos를 부팅하지 않았습니다. 아래는 **Mac Codex에서 실행할 절차**이며 완료 기록이 아닙니다. 기획 시안 대신 실제 앱 화면을 나중에 촬영합니다.

## 1. 용량과 기기 확인

Mac M1 8GB에서는 Pleos AVD 한 대만 실행하고 빌드는 Cloud 산출물을 우선 사용합니다. 외장 SSD의 실제 마운트명을 먼저 `ls /Volumes`와 `df -h`로 확인합니다. SDK/AVD 기존 경로를 조사하기 전에 옮기거나 삭제하지 않습니다. 이미 성공한 Pleos 환경이 있으면 보존합니다.

새 clone 예시에서 `<SSD>`는 실제 이름으로 바꿉니다.

```bash
mkdir -p "/Volumes/<SSD>/Projects"
git clone --branch feat/charok-recorder https://github.com/leemh-dev/chalog.git "/Volumes/<SSD>/Projects/charok"
cd "/Volumes/<SSD>/Projects/charok"
git status --short --branch
adb devices -l
```

이 앱은 새 패키지 dev.leemh.charok입니다. 이전 도착자리 데이터와 별개입니다. 이전 차록 설치가 있다면 서명 충돌 시 삭제하지 말고 **새 전용 AVD**를 사용합니다. Cloud/CI 개발 키는 다를 수 있으므로 같은 artifact의 앱/test APK를 함께 사용합니다. 키 복사/인증 우회/Always allow 자동 선택 금지.

## 2. APK 선택·설치

현재 검증 기준은 [CI43650320801](https://github.com/leemh-dev/chalog/actions/runs/43650320801)의 [artifact14136300842](https://github.com/leemh-dev/chalog/actions/runs/43650320801/artifacts/14136300842), 앱 코드ef85516·0.1.0/1입니다. GitHub Actions의 `charok-apks-and-evidence`를 받습니다. artifact의 `apk-metadata.json`과 실제 APK SHA-256, 버전, 개발 서명을 대조합니다. debug APK는 실행 확인용 개발 서명이며 release-unsigned는 설치용이 아닙니다.

```bash
shasum -a 256 /path/to/app-debug.apk
python3 scripts/adb_readiness.py --serial emulator-5554 --output private-evidence/readiness.json
bash scripts/install_apk.sh emulator-5554 /path/to/app-debug.apk
```

예상: devices=device, boot=1, package/activity found, 각rc0. 준비가 하나라도 실패하면 설치 차단. 설치 성공 뒤 실제 앱 화면과 오류를 확인합니다. Android 보안 승인창은 정상 화면에서 직접 승인하며, unauthorized를 앱 성공으로 표시하지 않습니다.

기존 checkout이 있다면 먼저 `git status --short --branch`를 확인하고 작업이 남아 있으면 보존합니다. 깨끗한 동일 브랜치만 `git pull --ff-only`로 갱신합니다. 강제 reset/checkout으로 변경을 덮어쓰지 않습니다.

APK artifact는 보존 기간14일입니다. 만료된 경우 최신 소스의 CI에서 다시 빌드하고 새 코드·해시·서명을 기록합니다. 예전 metadata를 새 빌드에 재사용하지 않습니다.

## 3. TEST 기록 흐름

처음 화면→TEST 선택→정차 상태 표시→기록 준비 동의→준비 켜기→20초 정도 기다림→실제 준비 길이 확인→기록 시작→TEST 증상→지금이야→종료→저장 완료→상담 카드→보고서 확인.

debug의 저장 실패 TEST는 기록 중 누르면 종료 저장을1회 실패시킵니다. 같은 기록 다시 저장 후 동일ID를 확인합니다. TEST 주행으로 바꾸면 상세·공유가 잠겨야 합니다. 이미 저장한 데이터를 삭제하지 않습니다.

## 4. 실제 SDK는 별도 검증

기록 종료 후 Pleos 연결을 선택합니다. 공식 Console에서 기어P와 속도0을 설정하고 앱 수신을 확인합니다. 권한이 필요한 경우 정상 Android/Pleos 설정 UI에서 승인합니다. 미지원/미수신이면 상태 미확인 잠금을 유지합니다.

준비 동의 후 실제 조향·브레이크·공조 관측과 수신 시각을 확인합니다. Console 조작·SDK 수신·실차를 따로 기록합니다. 앱의 공식 음성 버튼은 STT ON_DEVICE 텍스트 인식입니다. 원음 녹음이 아닙니다. Gleo의 “차록, 증상 기록해 줘”, 설명 추가, “기록 종료해 줘”는 별도로 시험하며 STT 성공으로 대신하지 않습니다.

창 바를 사용해 순정 내비와 차록을 함께 표시합니다. 앱에서 강제로 오른쪽3분할을 제어하지 않습니다. 확인된 실제 센터의 공식 출처·주소·좌표를 입력하고 지금 안내를 눌러 요청/SDK 이벤트/순정 핀을 구분합니다. 실제 예약이 완료됐다고 안내하지 않습니다.

## 5. 녹화는 실제 화면으로

실제 개인정보·차량 식별자·개인 경로가 없는 TEST 시나리오를 먼저 선택합니다. Pleos Console 입력도 자막/기록에서 구분합니다. 아래는 전체 화면 최대120초 녹화이며 앱 패널만 합성하지 않습니다.

```bash
bash scripts/record_mac_demo.sh emulator-5554
```

`private-evidence/`는 Git 제외입니다. 원본 영상을 검토·정제한 뒤 공개 증거로 옮깁니다. 입력 방식, 실행 이미지/SDK 버전, 코드 커밋·APK 해시, 실제 통과/미검증을 함께 남깁니다. screenrecord 미지원이면 Mac 화면 녹화로 전환하되 실제 화면 녹화 여부를 기록합니다.

## 6. 실행 결과 인계

[다음 실행 프롬프트](13-next-development-prompt.md)를 사용합니다. 검증이 끝나면 코드/APK 해시·Pleos 이미지 버전·준비 상태·화면별 입력 출처·실제 결과를 docs/10과 docs/11에 남깁니다. 실제 화면 녹화는 확인된 기능 흐름으로 진행하며, 이번 Cloud 작업에서 Mac 실행/녹화를 했다고 표시하지 않습니다.
