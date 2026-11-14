# 기획·UX 근거 확인 기록

조회: 2026-11-14 UTC / 11-15 KST. 전체 문서 전수 조사나 영상 시청이 아닌 아래 기능별 공식 자료 재검토입니다. 앱/기기 실행 없음.

| 공식 자료 | 이번 확인과 설계 반영 |
| --- | --- |
| [공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission) | 접수양식이 필수 기획서. 목업/프로토타입/화면 녹화도 선택 제출 가능. 선택 APK는 실행 가능한 release. 문서만 있는 저장소를 소스+APK 산출물로 표시하지 않음 |
| [FAQ](https://appdevchallenge.auto-hmg.io/faq) | 공개 5개 SDK, 기획서 개발계획 항목, 2차 PT 전 실제 앱 필요. 현재 공개 페이지와 최종 제출 시 조건 재확인 |
| [공식 DOCX](https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx) | HTTP 다운로드 성공, XML과 python-docx로 작품 소개 1~8항목/운영 정보/동의서 구분 확인. 작품 소개 내용만 채운 초안 생성 |
| [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) | 시스템 Driving view와 앱 창 최대 두 개의 관계, 크기 적응 설계. 오른쪽 강제 배치의 근거 아님 |
| [UI 원칙](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro) | 짧은 시선으로 핵심 파악, 짧은 응답/진행 상태, 음성 활용. 긴 스크롤과 움직이는 장식 대신 한 상태·한 질문 |
| [Gleo schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability) | function 최대 3개, 설명/parameter 규칙. 시작·설명/순간·종료의 세 기능 제안 |
| [Gleo callback](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback) | success/asking/fail, 7초 action timeout. 장시간 세션을 열린 단일 action으로 두지 않음 |
| [STT intro](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro) | STT는 음성→텍스트, 동작 명령은 앱이 처리. ON_DEVICE 등록/네트워크 조건과 서버 모드 구분 |
| [STT 시작/종료](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop) | 공식 SDK의 차량 마이크 인식 경로 확인. 앱 실행 성공이나 원음 파일 반환의 증거 아님 |
| [STT 오디오 입력](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data) | 앱 보유 PCM을 엔진에 넣는 경로. 원음 추출 기능으로 해석하지 않음 |
| [Vehicle 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission) | 대표 도메인 표만으로 모든 필드를 허용하지 않음. 메서드별 RequiresPermission과 기존 선정28개 정적 근거를 함께 사용 |
| [NaviHelper 목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations) | 요청과 경로 탐색 완료 구분. 선택 센터 좌표를 확인하고 callback/실제 핀까지 개별 검사 |

직접 재조회한 공지008 URL은 이번 웹 도구에서 접근 오류였다. 직전 [06](../../06-source-coverage-and-briefing.md)의 목록 링크를 통한 본문 확인 이력과 혼동하지 않는다. 추정한 `/guide/evaluation` 경로는 유효한 심사 본문을 확보하지 못해 근거로 쓰지 않았다. 심사 항목은 실제 열람한 FAQ에 한정했다.

소개 영상은 이전 Cloud 접근 실패 상태 그대로이며 이번에 재시도하지 않았다. 영상 대본/Q&A를 새로 검토했다고 주장하지 않는다. 원음·실제 예약·GDS 관련 미확인 결론은 기존 [05](../../05-conversation-scenario-feasibility.md)의 조사 범위를 유지한다.

## 시안 실행

- 로컬 Chromium 첫 실행: sandbox 내 crashpad socket 권한 오류. 허용된 일반 실행 권한으로 재시도.
- `file://` 탐색: 브라우저 정책으로 차단. 파일 프로토콜을 변경하거나 정책을 해제하지 않고 일반 개발 방식인 localhost HTTP 서버로 시안 제공.
- `python scripts/check_design.py`: 6개 UX 검사 묶음, 3개 폭 × 10개 상태 = 30개 레이아웃 확인. [결과](prototype-checks.json).
- 최초 캡처에서 내비 도식의 꺾임 화살표 글리프가 사각형으로 출력되어 SVG로 교체. 상담 카드 하단 버튼이 경계에 걸려 카드 간격 조정 후 다시 캡처.
- 이미지와 HTML은 설계 자료이며 실제 Android/Pleos·마이크·SQLite·예약·정비사 수신 성공의 증거가 아니다.
