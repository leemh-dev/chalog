# CHALOG 저장소·소스 이름 변경

영문은 **CHALOG**(`차 + log`, 기록), 한글 서비스명·호출어는 **차록**을 유지합니다. 이름 변경으로 기능 검증 수준이 달라지지는 않습니다.

## 변경 범위

- 로컬 저장소 폴더도 `/Volumes/Myeonghun 2TB/DochagjariDev/chalog`로 변경했습니다. 이전 `charok` 경로는 같은 폴더를 가리키는 호환 심볼릭 링크여서 기존 작업 경로가 끊기지 않습니다. SDK/AVD는 이동하지 않았습니다.
- GitHub 저장소: `leemh-dev/charok` → [`leemh-dev/chalog`](https://github.com/leemh-dev/chalog). 저장소 ID1458180705, 커밋·이슈·PR 이력을 유지한 이름 변경입니다. 로컬 origin도 새 주소입니다.
- Gradle 프로젝트 `Chalog`, 소스 namespace `dev.leemh.chalog`, `ChalogApp`·`ChalogScreen`, 리소스 `chalog_icon`, main/test/androidTest 폴더와 Room schema 출력 경로를 변경했습니다.
- Android **0.3.2 / versionCode8**. UI·한국어 호출어·기록 데이터 형식은 유지합니다.
- 현재 Gleo schema는 `OPEN_CHALOG_RECORDS`를 사용합니다. 기존 `OPEN_CHAROK_RECORDS`도 받아 같은 패널을 열고 받은 action 이름으로 응답합니다. 둘 다 열기만으로 기록을 시작하지 않습니다.
- CI 이름·artifact 이름, 활성 프로토타입 `design/chalog-prototype.html`, 스크립트의 소스 클래스 경로, 문서의 저장소 링크를 정리했습니다. 제출 초안 생성기는 앞으로 `chalog-application-draft.docx`와 같은 이름의 metadata를 만들며 과거 초안/검증 파일을 덮어쓰지 않습니다.

## 의도적으로 보존하는 이름

| 항목 | 보존 이유 |
| --- | --- |
| applicationId `dev.leemh.charok` / test ID `.test` | 같은 앱으로 업데이트하여 기존 사용자 데이터·권한·FileProvider URI를 유지 |
| DB `charok.db`, schema1·Room identity hash | 기존 기록을 이동·삭제·재생성하지 않고 그대로 읽음 |
| launcher alias `dev.leemh.charok.MainActivity` | 기존 런처/바로가기 및 이전 실행 명령 호환. 실제 Activity는 `dev.leemh.chalog.MainActivity` |
| `OPEN_CHAROK_RECORDS` | 이전 외부 호출과 호환 |
| `feat/charok-recorder` | 기존 개발 브랜치에서 계속하라는 사용자 지침·PR #2 유지. 자동 병합하지 않음 |
| 기존 AVD `CharokPleosMac` | 기존 Pleos 환경·userdata 보존. 새 AVD를 만들지 않음 |
| 과거 APK metadata·로그·영상·제출 DOCX | 당시 결과와 서명·해시를 바꾸지 않는 원본 증거. 과거 검증이 새 버전 검증으로 바뀌지 않음 |

Android는 [소스 namespace와 설치 applicationId를 분리](https://developer.android.com/build/configure-app-module)할 수 있습니다. 업데이트를 유지하기 위해 설치 ID·개발 서명은 바꾸지 않습니다. 런처 호환은 공식 [activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)를 사용합니다. GitHub의 [저장소 이름 변경](https://docs.github.com/en/repositories/creating-and-managing-repositories/renaming-a-repository)은 이전 주소를 새 주소로 연결하므로 과거 증거의 링크도 계속 사용할 수 있습니다.

## 검증 결과

코드 **ce49905 / 0.3.2/8**. Mac JVM40·lint 오류0/경고14, 실제 Pleos16개·새 PID 복원 검사를 통과했습니다. 업데이트 직후119→119, 기기 검사 후119→133이며 기존119개 모든 행(id/startedAt/payload)이 정확히 같습니다. 추가14개는 TEST 저장13개·중단1개입니다. 기록 서비스가 종료된 것을 확인하고 AVD를 정상 종료했습니다.

[실제 화면·새/구 실행 호환·기기 검사·APK·서명·DB 보존 증거](evidence/macos-pleos-chalog-rename-2026-11-15/README.md).

이전0.3.1 시연 영상은 당시 APK의 검증 영상으로 보존합니다. 실제 Gleo/차량SDK/마이크·자동 우측 배치 등 미검증 범위는 그대로입니다. [CI43707910220](https://github.com/leemh-dev/chalog/actions/runs/43707910220)도 JVM40·일반 Android 기기16·새 PID 복원을 통과했습니다. [artifact14159438002](https://github.com/leemh-dev/chalog/actions/runs/43707910220/artifacts/14159438002)의 ZIP/APK3개 해시·버전·개발 서명은 실제 파일과 metadata가 일치합니다. [CI 대조 요약](evidence/macos-pleos-chalog-rename-2026-11-15/ci-summary.json).
