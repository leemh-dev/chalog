# CHALOG 병합·재촬영·제출 준비

사용자의 명시적 병합 요청으로 기획 PR #1과 개발 PR #2를 순서대로 정상 병합했습니다. PR #2는 기획 병합 후 main을 기준으로 바꿔 병합했습니다. 기존 커밋과 `feat/charok-recorder` 브랜치를 유지했고 auto-merge 설정을 켜지 않았습니다. 앱 코드ce49905가 포함된 병합 커밋은 `8534008bd723b833a9dd2f6e20cfc60d6e5e0ce0`입니다.

## 준비된 제출 자료

| 자료 | 파일 | 상태 |
| --- | --- | --- |
| 공식 신청서·기획서 | [chalog-application-draft.docx](submission/chalog-application-draft.docx) | 공식 양식8개 프로젝트 항목 작성,5페이지 한글/배치 검사, 개인정보·동의·서명은 미작성 |
| 실행 APK | [CHALOG_차록_release.apk](../apk/CHALOG_차록_release.apk) | 0.3.2/8 release·debuggable=false·개발 서명, 실제 Pleos 설치/기록/저장/재실행 확인 |
| 새 한글 자막 시연 | [2분39초 MP4](evidence/chalog-submission-demo-2026-11-15/chalog-verified-scenarios-ko.mp4) | 실제0.3.2 재촬영, 이전과 동일6개시나리오·17개구간,3820프레임/한글OCR17개검사 |
| 자막·검증 | [SRT·영상·DB·release 증거](evidence/chalog-submission-demo-2026-11-15/README.md) | 저장739건·새PID복원·원본133개완전보존, release검사후총135개 |
| 이전 영상 원본 | [0.3.1 MP4](evidence/macos-pleos-captioned-demo-2026-11-15/chalog-verified-scenarios-ko.mp4) | 수정하지 않음, 기존SHA-256 재대조 |

새 신청서는 공식 양식의 법률·동의 문구, 참가자/서명 표, 여백·스타일·머리글/바닥글을 유지했습니다. 프로젝트명3곳과 프로젝트 답변8칸을 채우고, Mac 렌더링에서 빈 추가 페이지를 만들던 공백 문단1개를 제거했습니다. 공식 원본 편집자의 이름이 파일 속성에 남지 않도록 마지막 편집자 metadata를 비웠습니다. 개인정보·서명·실적은 임의로 작성하지 않았습니다. [문서 검증](submission/chalog-application-verification.json).

## 공식 제출 절차

2026-11-15 확인 기준 마감은 **2026-11-15 23:59 KST**, 수신은 **appdevchallenge@hyundai-partners.com**입니다. 제목은 `[차량용 앱 개발 공모전] 팀명(개인은 성명)_대표자명` 형식입니다. [공식 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission) · [공식 FAQ](https://appdevchallenge.auto-hmg.io/faq).

1. DOCX의 개인/팀·참가 부문, 성명·연락처·인원·작성일을 본인 정보로 작성합니다. 프로젝트 소개 부분에는 개인 정보를 쓰지 않습니다.
2. 개인정보 동의 여부와 본인 서명·서명일을 해당 참가자가 직접 작성합니다. 팀 참가 시 모든 팀원의 개별 동의·서명과 제출 이메일 참조(CC)가 필요합니다.
3. 공식 APK 이름 규칙 `팀명_앱명_release.apk`에 맞춰 참가 팀명/개인 성명이 CHALOG와 다르면 파일명과 이메일/신청서의 안내를 함께 맞춥니다. APK 바이트/해시는 바뀌지 않습니다.
4. 이메일에 공개 저장소 URL, 최종 제출할 main 커밋 전체SHA, APK·영상 경로를 포함합니다. 영상·소스·APK는 선택 제출 참고자료이며 실제 검증 범위를 그대로 설명합니다.
5. 개인정보와 서명이 들어간 최종 DOCX는 공개 GitHub에 올리지 않고 이메일에 첨부합니다. 서명 후 Word에서 페이지와 줄바꿈을 확인하고 마감 전에 발송합니다.

이 작업은 **제출 준비**이며 이메일 발송이나 접수 완료를 의미하지 않습니다. 로컬 비공개 제출 묶음에 이메일 초안과 파일 해시를 제공합니다. 공개 저장소에는 개인 정보 없는 초안만 포함합니다.

## 남아 있는 제품 검증 범위

Gleo 실제 음성·정식 등록, 실제 차량 SDK·마이크, 자동 우측 창 배치·짧은 토스트, 차록의 순정 내비 목적지 전달, 실제 파일 내보내기·정비소 전송/예약은 별도 개발/검증 범위입니다. 제출 영상은 수동 TEST 입력과 실제 Pleos의 저장/복원 성공을 명확히 표시합니다. 순정 내비의 경로 안내는 동작하나 바탕지도는 비어 있습니다.
