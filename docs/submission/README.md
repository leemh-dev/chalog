# 공식 접수양식 초안

[charok-application-draft.docx](charok-application-draft.docx)는 공식 접수양식의 작품 소개 1~8항목과 앱명만 채웠습니다. **개인정보·참가 구분·동의·서명은 비어 있으며 제출하지 않았습니다.**

- 개인/팀, 부문, 참가자 정보는 본인이 비공개 사본에 작성합니다. 서명과 동의는 참가자별로 직접 확인합니다.
- Word에서 문단·표·페이지 나눔을 확인합니다. 이 환경에서는 내용을 재조회했지만 Word 인쇄 렌더링은 검증하지 못했습니다.
- 앱/SDK/영상은 현재 미완료라고 작성했습니다. 실제 개발 후 확인된 수준과 자료 링크·최종 커밋으로 갱신해야 합니다.
- 공개 저장소에 완성된 개인정보·서명 포함 파일을 커밋하지 않습니다. 작품 소개의 성명·소속·연락처 비기재 조건을 유지합니다.
- 실제 이메일 발송은 별도 지시가 있어야 진행하며 이번 작업에서 발송하지 않았습니다.

원본: https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx

생성: `python scripts/make_submission_draft.py /path/to/official-template.docx` (python-docx 필요). 공식 양식 구조가 달라지면 스크립트가 중단되며 새 양식을 검토해야 합니다. [원본/초안 해시와 확인 결과](../evidence/planning-design/submission-draft.json).
