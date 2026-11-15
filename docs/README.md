# 차록 문서

문서 기준일: 2026-11-15. 사용자와 합의한 최신 흐름을 기준으로 새 저장소에 정리했습니다.

| 문서 | 내용 |
| --- | --- |
| [01 · 기획서](01-product-plan.md) | 문제, 서비스 가치, 시나리오, UX, 구간 기록과 정비소 인계 |
| [02 · SDK 현실성](02-sdk-feasibility.md) | 공개 API·개별 권한·제약·기존 정비 장비와의 관계 |
| [03 · 개발 계획](03-development-plan.md) | 단계별 Cloud/Mac 작업, 검증 문턱, 시연 구성 |
| [04 · 의사결정 기록](04-decision-log.md) | 확정 요구, 추천 기본값, 미확정 사항, 조사 이력 |
| [05 · 대화형 시나리오 재검토](05-conversation-scenario-feasibility.md) | 3영역/Gleo/소리 녹음/예약·자료전달/내비의 실제 지원 범위 |
| [06 · 확인 범위/설명회](06-source-coverage-and-briefing.md) | 전수 검토와 기능별 조사 구분, 영상 접근 실패, 공모전 조건 재확인 |
| [07 · 공모전 기획서](07-submission-plan.md) | 최신 범위, 공식 작품 소개 1~8항목, 제출 조건 |
| [08 · UX/UI 명세](08-ux-ui-spec.md) | 화면·대화·상태·차량 제한·시각 토큰 |
| [09 · 시연·개발 계획](09-demo-delivery-plan.md) | Cloud/Mac 단계, 시연 구성, 다음 실행 프롬프트 |
| [10 · 개발 기록](10-development-log.md) | 실제 앱 개발·검사·실패/해결 이력 |
| [11 · 검증 현황](11-verification.md) | 최신 구현과 실제 실행 수준 |
| [12 · Mac Pleos 인계](12-mac-pleos-handoff.md) | 설치·TEST/SDK 확인·실제 화면 녹화 절차 |
| [13 · 다음 실행 프롬프트](13-next-development-prompt.md) | Mac 실제 앱·SDK 확인과 후속 녹화 작업 |
| [15 · Gleo 중심 개발 방향](15-gleo-background-direction.md) | 화면 없이 기록·종료, 명시적 우측 열기와 검증 조건 |
| [16 · 경고등 관측](16-warning-light-observations.md) | 점등·점멸·소등 이력과 수신 제한, 실제 TEST 검증 |
| [17 · CHALOG와 내비](17-chalog-and-navigation.md) | 영문 브랜드, 순정 검색·경로 안내와 지도 바탕 문제 분리 |
| [18 · 전체 서비스 검토](18-service-scenario-review.md) | 흐름도·12개 시나리오·실제 실행·미충족 우선순위 |
| [19 · 백그라운드 수정·재검증](19-background-fixes-and-reverification.md) | Broadcast/OPEN·토스트·보고서·12개 재검증 |
| [21 · CHALOG 이름 변경](21-chalog-rename.md) | 저장소·소스 이름 통일, 기존 설치·DB·실행 호환 |
| [20 · 한글 자막 시연](20-captioned-scenario-demo.md) | 실제 Pleos TEST 시나리오6개·영상·한글 자막·완성 후 검증 |
| [최신 우측 패널 시안](design/gleo-background/README.md) | 공식 Pleos 배치 유지, 흐름과 A/B/C 디자인 |
| [디자인 시안](design/README.md) | 조작 가능한 HTML과 7장 캡처, TEST 시뮬레이션 |
| [접수양식 초안](submission/charok-application-draft.docx) | 작품 소개 작성, 개인정보·동의·서명 미작성 |
| [정적 SDK 조사 근거](evidence/sdk-review/README.md) | 이전 조사에서 확인한 버전·해시·28개 메서드/권한 |

문서는 제품 목표, 구현 제안, 실제 검증 결과를 구분합니다. 기획 명세는 07~09, 최신 제품 방향은 15, 실제 구현·검증 상태는 10~14·18~21과 최신 실행 증거를 우선합니다. 이전 도착자리의 빌드·기기 검사 결과를 차록의 성공 증거로 사용하지 않습니다.

## 최신 실행 결과 · CHALOG 0.3.2

- [저장소·소스 이름 변경과 설치 호환](21-chalog-rename.md) · [JVM40·Pleos16·새 PID·119개 보존·APK 증거](evidence/macos-pleos-chalog-rename-2026-11-15/README.md)

## 이전0.3.1 · 한글 자막 시연

- [2분39초 시연 영상·SRT·완성 후 검증](20-captioned-scenario-demo.md) · [영상과 증거](evidence/macos-pleos-captioned-demo-2026-11-15/README.md)

## 이전 실행 결과 · 0.3.1 수정·재검증

- [수정과12개 판정](19-background-fixes-and-reverification.md) · [실제 화면·기기15·새 PID·CI·DB 증거](evidence/macos-pleos-gleo-fixes-2026-11-15/README.md)

## 이전 실행 결과 · 전체 서비스 시나리오 검토

- [12개 시나리오와 실행 판정](18-service-scenario-review.md) · [실제 화면11장·Mac 기기9·새 PID 복원·DB 보존](evidence/macos-pleos-scenario-2026-11-15/README.md)

## 이전 실행 결과 · 0.2.3 CHALOG와 순정 내비

- [CHALOG 실제 화면·순정 내비 경로·렌더러 비교·DB 보존 증거](evidence/macos-pleos-navigation-2026-11-15/README.md)
- [이전 경고등 실제 화면4장·검사·DB 보존 증거](evidence/macos-pleos-warnings-2026-11-15/README.md)
- [이전 SIGNAL 실제 화면8장·글자/버튼 크기·검증 증거](evidence/macos-pleos-signal-2026-11-15/README.md) — 이번에는 영상 없음, 다음 영상 한국어 기능 자막 적용
- [흰색 UI와 백그라운드 기록 원칙](14-background-and-white-ui.md)
- [이전 0.2 Mac Pleos 실제 화면·알림·시연 영상·검증 증거](evidence/macos-pleos-v0.2-2026-11-15/README.md)

- [22 · CHALOG 병합·재촬영·제출 준비](22-submission-ready.md) — 최신0.3.2 영상·심사용release·공식신청서·제출절차
