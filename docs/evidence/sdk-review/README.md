# SDK 정적 조사 근거

2026-11-14 UTC. 이전 프로젝트의 정적 조사 파일을 변경 없이 이관했습니다. 차록에서 SDK 서비스나 앱을 실행한 결과가 아닙니다.

- [binary-inspection.json](binary-inspection.json): `ai.pleos.playground:Vehicle:2.3.2` AAR/classes SHA-256와 javap 조회 종료 코드
- [vehicle-selected-methods.json](vehicle-selected-methods.json): 선정28개 메서드 시그니처와 RequiresPermission
- [migration-checks.json](migration-checks.json): 이관한 파일의 SHA-256/원본 대조

원본은 [dochagjari 9adb2c5 정적 증거](https://github.com/leemh-dev/dochagjari/tree/9adb2c5/docs/evidence/symptom-sdk-feasibility)입니다. AAR/JAR/전체 바이트코드·개인정보는 이관하지 않았습니다.

원 조사에서는 기존 Gradle 캐시의 Vehicle2.3.2 AAR에서 Python zipfile로 classes.jar를 추출한 뒤 JDK17 `javap -classpath ...`와 `javap -v -classpath ...`를 사용했습니다. 최초 Steeringwheel 오기는 rc1이며 대소문자를 수정한 SteeringWheel은 rc0입니다. bare jar/javap PATH 부재는 명시 JDK 경로로 해결했습니다. 일부 웹 상세가 시그니처를 표시하지 않아 버전 명확한 바이너리로 보완했습니다.

중요 발견은 타이어 실측 수치와 통합 경고 이벤트의 PRIVATE_CAR_SAFETY 권한입니다. 일반 Safety getter/개별 상태와 구분합니다. STT/TTS/FusedLocation은 문서 조사이며 이번 파일들로 바이너리/기기 성공을 입증하지 않습니다. [지원 분석](../../02-sdk-feasibility.md).
