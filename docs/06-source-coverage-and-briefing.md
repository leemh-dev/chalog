# 확인 범위와 공모전 설명회 접근 기록

2026-11-14 UTC. 사용자가 [공모전 통합 온라인 설명회](https://youtu.be/na6tSJ44pWc)를 제공하고 Pleos Playground/문서를 전부 확인했는지 물었다.

## 답변 기준

**전부 확인한 것은 아니다.** 지금까지는 차록 기능에 관련된 공개 가이드·API·권한·일부 샘플/릴리즈 노트를 조사했다. 전체 문서의 모든 메서드, 로그인 Workspace/개인 프로젝트, 모든 샘플 소스와 영상 전체를 검토했다고 말하지 않는다. 문서 경로 확인과 실제 SDK 실행도 다르다.

## 이번 새 확인

| 자료 | 확인 수준 |
| --- | --- |
| [Playground 공개 포털](https://developers.pleos.ai/playground?locale=ko-KR) | Getting Started/SDK/Design/Gleo/Workspace 안내를 열람. 로그인 Workspace 미열람 |
| [공모전 개발 환경](https://appdevchallenge.auto-hmg.io/guide/development) | 다섯 SDK/공식 문서 우선/음성 메모 예시 확인 |
| [공모전 FAQ](https://appdevchallenge.auto-hmg.io/faq) | 제출·SDK·심사 조건을 재조회 |
| [공모전 개발 범위 공지008](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000008) | 이전 직접URL실패와 달리 이번 공지 목록 링크로 본문 열람 성공 |
| [설명회 안내005](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000005) | 공모전/개발환경/Q&A 프로그램, 다시보기 예정 문구 |
| [Playground 릴리즈 노트](https://document.pleos.ai/docs/pleos-only/release-note/playground-release-note) | 10/29 포털 주소 변경, 10/19 LLM SDK 종료/Gleo Interface 안내 |
| [SDK 릴리즈 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note) | Vehicle2.3.2/Navi3.0.5/TTS·STT3.3.1/위치1.1.3과 변경사항 확인 |
| [공식 Vehicle 샘플 사용 가이드](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app) | capability/get/listener 구분, 초기화 표시만으로 Service/개별기능 성공 아님. 모든 샘플 소스 검토/실행은 아님 |
| [Virtual Console 셋업](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator) | 가이드 열람. 실제 Console 조작/새 AVD 실행 없음 |
| [UI 디자인 가이드](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro) | 짧은 상태/주행 중 시선유도 최소화/음성 상호작용/디자인자료 안내 확인 |
| [STT 클래스 메서드 목록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/entry) | request/sendAudio/리스너 등 목록 확인. 일부 상세페이지는 본문 서명 렌더가 제한적 |

그전까지의 기능별 조사는 [02](02-sdk-feasibility.md), [05](05-conversation-scenario-feasibility.md)에 출처와 제한을 남겼다. 이번 웹 조회를 전체 바이너리/모든 문서의 전수 검사로 부르지 않는다.

## 설명회 영상: 제목만 확인, 내용 미확인

YouTube 링크는 웹 도구에서 ‘현대자동차그룹 차량용 앱 개발 공모전 통합 온라인 설명회’ 제목으로 식별됐다. 그러나 본문에 재생 내용/대본이 제공되지 않았고 watch URL 재조회는 throttled였다.

임시 폴더에 yt-dlp/youtube-transcript-api를 설치해 공개 정보/자막을 정상 요청했다. 두 경로 모두 네트워크 proxy의 **403 Forbidden**으로 실패했다. 로그인·개인 쿠키·접근 제한 우회는 하지 않았다. 영상 파일/자막/챕터/시각별 발언은 확보하지 못했으므로 영상 시청/내용 분석/타임스탬프 인용은 하지 않는다. 제목 검색 결과를 영상 대본으로 간주하지 않는다.

정리: `video_id=na6tSJ44pWc`, `title_identified=true`, `transcript_obtained=false`, `audio_or_frames_reviewed=false`. 원본 영상 또는 자막을 정상적으로 확보해야 영상 내용 대조를 완료할 수 있다. 영상 전체 대본은 저장소에 무단 재배포하지 않는다.

## 새로 확정한 공모전 조건

공식 공지008 본문을 이번에 다시 확인했다.

- 공개 다섯 SDK는 Workspace의 준비중 표시와 무관하게 문서 절차로 에뮬레이터 개발·시험 가능하다.
- 참가팀은 Gleo Interface를 별도 검토/승인 없이 사용할 수 있다. 일반 배포용 가이드의 승인 설명 때문에 공모전 검증을 멈추지 않는다.
- 공모전 심사는 앱마켓 입점 심사와 다르다. 출시 심사 절차를 공모전 필수 절차로 섞지 않는다. 다만 공개 SDK 한계/실제 동작/사용자 안전은 구분해 검증한다.
- 문서 미지원 기능은 현재 구현 기능으로 가정하지 않고 향후 계획으로 기재할 수 있다. 미공개 SDK/Pleos LLM SDK는 제공되지 않는다.
- 차량 마이크 외부 STT 전송은 지원되지 않으며 공식 STT를 사용한다. 2차는 에뮬레이터 구동 앱 시연이며 실차는 필요 없다.

원음 녹음/공유·예약·고정창배치 판정은 계속 **공개 지원 경로 미확인**이다. 자료 미발견을 기술적으로 절대 불가능하다는 증명으로 바꾸지 않는다. 반대로 포털 소개의 ‘다양한 차량 데이터’라는 문구를 모든 센서/원음/정비예약 API 제공으로 확대하지 않는다.

## 아직 확인해야 할 것

설명회 실제 발언과 Q&A, 필요한 SDK의 실제 artifact/샘플 호출, Pleos Gleo/마이크/창 동작, 원음 취득을 허용하는 공식 경로, 실제 예약·업체 접수 인터페이스다. 기획 확정 전에 구체적인 기능 단위로 제공 문서/실행 결과/미확인을 대조한다.

앱/기기/실제 예약/자료 발송은 하지 않았다. 이번은 조사 범위 정정과 문서 보완이다.
