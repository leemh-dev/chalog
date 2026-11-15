# CHALOG 저장소·소스 이름 변경

영문은 **CHALOG**(`차 + log`, 기록), 한글 서비스명·호출어는 **차록**을 유지합니다. 이름 변경으로 기능 검증 수준이 달라지지는 않습니다.

## 변경 범위

- GitHub 저장소: `leemh-dev/charok` → [`leemh-dev/chalog`](https://github.com/leemh-dev/chalog). 저장소 ID1458180705, 커밋·이슈·PR 이력을 유지한 이름 변경입니다. 로컬 origin도 새 주소입니다.
- Gradle 프로젝트 `Chalog`, 소스 namespace `dev.leemh.chalog`, `ChalogApp`·`ChalogScreen`, 리소스 `chalog_icon`, main/test/androidTest 폴더와 Room schema 출력 경로를 변경했습니다.
- Android **0.3.2 / versionCode8**. UI·한국어 호출어·기록 데이터 형식은 유지합니다.
- 현재 Gleo schema는 `OPEN_CHALOG_RECORDS`를 사용합니다. 기존 `OPEN_CHAROK_RECORDS`도 받아 같은 패널을 열고 받은 action 이름으로 응답합니다. 둘 다 열기만으로 기록을 시작하지 않습니다.
- CI 이름·artifact 이름, 활성 프로토타입 `design/chalog-prototype.html`, 스크립트의 소스 클래스 경로, 문서의 저장소 링크를 정리했습니다.

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

## 검증 진행

이름 변경 빌드·단위 검사와 기존 Mac Pleos에 대한 보존 업데이트 검증을 진행 중입니다. 결과와 실제 증거를 이 문서에 추가합니다. 이전 0.3.1 자막 영상은 해당 버전의 검증 영상으로 유지합니다.
