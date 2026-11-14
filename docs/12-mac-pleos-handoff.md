# Mac Pleos 설치·실행·녹화 인계

현재 Cloud에서 Mac에 접속하거나 Pleos를 부팅하지 않았습니다. 아래는 **Mac Codex에서 실행할 절차**이며 완료 기록이 아닙니다. 기획 시안 대신 실제 앱 화면을 나중에 촬영합니다.

## 1. 용량과 기기 확인

Mac M1 8GB에서는 Pleos AVD 한 대만 실행하고 빌드는 Cloud 산출물을 우선 사용합니다. 외장 SSD의 실제 마운트명을 먼저 `ls /Volumes`와 `df -h`로 확인합니다. SDK/AVD 기존 경로를 조사하기 전에 옮기거나 삭제하지 않습니다. 이미 성공한 Pleos 환경이 있으면 보존합니다.

새 clone 예시에서 `<SSD>`는 실제 이름으로 바꿉니다.

```bash
mkdir -p "/Volumes/<SSD>/Projects"
git clone --branch feat/charok-recorder https://github.com/leemh-dev/charok.git "/Volumes/<SSD>/Projects/charok"
cd "/Volumes/<SSD>/Projects/charok"
git status --short --branch
adb devices -l
```

이 앱은 새 패키지 dev.leemh.charok입니다. 이전 도착자리 데이터와 별개입니다. 이전 차록 설치가 있다면 서명 충돌 시 삭제하지 말고 **새 전용 AVD**를 사용합니다. Cloud/CI 개발 키는 다를 수 있으므로 같은 artifact의 앱/test APK를 함께 사용합니다. 키 복사/인증 우회/Always allow 자동 선택 금지.

## 2. APK 선택·설치

GitHub Actions의 `Charok Android` 성공 run에서 `charok-apks-and-evidence`를 받습니다. artifact의 `apk-metadata.json`과 실제 APK SHA-256, 버전, 개발 서명을 대조합니다. debug APK는 실행 확인용 개발 서명이며 release-unsigned는 설치용이 아닙니다.

```bash
shasum -a 256 /path/to/app-debug.apk
python3 scripts/adb_readiness.py --serial emulator-5554 --output private-evidence/readiness.json
bash scripts/install_apk.sh emulator-5554 /path/to/app-debug.apk
```

예상: devices=device, boot=1, package/activity found, 각rc0. 준비가 하나라도 실패하면 설치 차단. 설치 성공 뒤 실제 앱 화면과 오류를 확인합니다. Android 보안 승인창은 정상 화면에서 직접 승인하며, unauthorized를 앱 성공으로 표시하지 않습니다.

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
