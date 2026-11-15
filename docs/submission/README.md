# 공식 접수양식 초안

[chalog-application-draft.docx](chalog-application-draft.docx)는 공식 접수양식의 작품 소개 1~8항목과 프로젝트명 3곳을 채운 최신 초안입니다. **개인정보·참가 구분·동의·서명은 비어 있으며 제출하지 않았습니다.**

- 개인/팀, 부문, 참가자 정보는 본인이 비공개 사본에 작성합니다. 서명과 동의는 참가자별로 직접 확인합니다.
- Mac 렌더링에서 5페이지의 한글 표시와 배치를 확인했습니다. Word 인쇄 렌더링은 검증하지 못했으므로 서명 후 Word에서 문단·표·페이지 나눔을 확인합니다.
- 초안 본문에는 실제 구현·검증 수준과 제출 자료 경로를 반영했습니다. 상세 제출 절차와 남아 있는 검증 범위는 [22-submission-ready.md](../22-submission-ready.md)를 참고합니다.
- 공개 저장소에 완성된 개인정보·서명 포함 파일을 커밋하지 않습니다. 작품 소개의 성명·소속·연락처 비기재 조건을 유지합니다.
- 실제 이메일 발송은 별도 지시가 있어야 진행하며 이번 작업에서 발송하지 않았습니다.

원본: https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx

생성: `python scripts/make_submission_draft.py /path/to/official-template.docx` (python-docx 필요). 이 스크립트는 최초 초안 문구를 채우므로 실행하면 현재 초안을 덮어씁니다. 공식 양식 구조가 달라지면 스크립트가 중단되며 새 양식을 검토해야 합니다. [원본/초안 해시와 확인 결과](../evidence/planning-design/submission-draft.json).
