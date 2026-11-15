# CHALOG · 차록 심사용 APK

[이명훈_차록_release.apk](이명훈_차록_release.apk)는 **0.3.2 / versionCode 8, release 빌드, debuggable=false**인 실행 가능한 APK입니다. 기존 Mac 개발 인증서로 서명했습니다. 스토어 출시용 인증서로 서명한 배포본은 아닙니다. [metadata·해시·서명·실행 검사](apk-metadata.json).

- APK SHA-256: `b47b663a3a7fc52988691ab8acff59f18bda208ea668676afd63cb1463a51d3a`
- 개발 인증서 SHA-256: `868a8ee5b37eb58711ebf79da06145f6d935aa938b2db59da145aa8c8b33904d`
- 앱 코드: `ce4990558b0afb943b3a6a9ccb1a2f0140175b0c` (PR #2로 main에 병합). 기존 설치 호환 applicationId는 `dev.leemh.charok`, DB는 `charok.db`입니다.
- 검증 환경: Mac M1 8GB, 외장 SSD, 기존 AVD 1대, Pleos Connect 2.1.0 / Android 14(API 34) ARM64, 2560×1440.
- 실제 release 설치 → 기존 영상 기록 복원 → 수동 TEST 설명·순간 표시·종료/저장 → 새 PID 재실행 성공. 관측216건·설명1건·순간1건을 보존했고 수집은 자동 재개하지 않았습니다. 이전134개 모든 행을 보존해 최종135개·DB무결성ok입니다. [증거](../docs/evidence/chalog-submission-demo-2026-11-15/README.md).

ADB 연결과 boot_completed=1, package/activity 서비스를 확인하고 설치합니다. 기존 앱을 제거하거나 데이터를 초기화하지 않습니다. 다른 서명의 앱이 설치된 경우 기존 기록을 지우며 설치를 강행하지 않습니다.

```sh
adb devices -l
python3 scripts/adb_readiness.py --adb /absolute/path/to/adb --serial emulator-5560 --output /tmp/chalog-readiness.json
adb -s emulator-5560 install --no-incremental -r apk/이명훈_차록_release.apk
adb -s emulator-5560 shell am start --user 10 -W -a OPEN_CHALOG_RECORDS -p dev.leemh.charok
```

위 serial/user10은 검증한 Pleos 환경의 값입니다. 다른 환경에서는 실제 기기와 현재 사용자에 맞춥니다. 앱 아이콘으로도 실행할 수 있습니다. 첫 실행에서는 실제 차량 상태 미확인으로 시작합니다. 수동 검증은 **입력 설정 → TEST 선택 → 기록 동의 → 기록 준비 켜기 → 기록 시작 → TEST 도구/지금이야 → 종료·저장 → 상담 카드** 순서입니다. 기록이 이미 열려 있다면 ‘새 기록 준비’로 준비 화면에 돌아갑니다. TEST는 실제 차량·Gleo·마이크 성공을 뜻하지 않습니다.

release에서는 debug 전용 TEST 음성 설정과 저장 실패 주입을 노출하지 않습니다. 실제 Gleo 등록·발화·차량 SDK·마이크·자동 우측 창 배치는 별도 검증 범위입니다. 영상은 같은 앱 코드의 debug 빌드로 새로 촬영했고 release APK는 별도로 위 흐름을 검사했습니다.

공식 파일명 규칙 `팀명_앱이름_release.apk`에 따라 개인 참가자명 **이명훈**, 앱 이름 **차록**으로 확정했습니다. 파일명 변경 전후 APK 내용·해시·개발 서명은 같습니다. 서명키·DB 원본은 포함하지 않습니다.
