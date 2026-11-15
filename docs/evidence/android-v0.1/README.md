# Android 0.1.0 실제 검사 증거

- 앱 코드: `ef85516e89bd74e50dc23038fbcd52b0bfaf55cb`, 버전0.1.0/1.
- [CI43650320801](https://github.com/leemh-dev/charok/actions/runs/43650320801), [artifact14136300842](https://github.com/leemh-dev/charok/actions/runs/43650320801/artifacts/14136300842).
- 환경: GitHub 새 전용 일반 Android API34 x86_64 AVD. TEST 차량/증상 입력. **Pleos·실차·실제 마이크 성공 증거가 아닙니다.**
- 캡처: native Compose root 900×916, 시스템 상태/내비 바 제외. 원본 PNG이며 HTML/디자인 이미지가 아닙니다. 창 폭900dp에서 확인했으며 실제 Pleos 좁은 창은 별도 검증합니다.
- JVM24, 기능 기기5, lint 오류0, 세 APK 빌드, 실제 force-stop/새 PID 복원 성공. 전체 XML/HTML 보고서는 artifact에 있습니다.

## 실제 앱 화면

| 파일 | 확인한 상태 |
| --- | --- |
| [01 준비](screens/01-prepared-test.png) | 준비2초 그대로 표시. 즉시 과거60초를 생성하지 않음 |
| [02 기록](screens/02-recording-test.png) | TEST 사용자 설명·관측12건 저장 확인 |
| [03 순간](screens/03-marked-test.png) | 사용자 ‘지금이야’ 표시. 자동 소음 검출 아님 |
| [04 저장](screens/04-saved-test.png) | 같은 기록의 관측12건·순간1건, SQLite 재조회 |
| [05 상담](screens/05-consultation-test.png) | 원문·출처·관측·미확인 영역·원음 미녹음 표시 |
| [06 실패](screens/06-save-failed-test.png) | 종료 저장1회 실패 TEST, 같은 ID 재시도 성공은 기기 검사로 확인 |
| [07 주행](screens/07-driving-lock-test.png) | TEST 주행 중 상세 버튼 비활성. 종료는 가능. 수집 전 종료한0건을 그대로 표시 |

## 저장·복원·준비 상태

- [session-proof.json](session-proof.json): 앱이 실제 SQLite에서 재조회한 TEST 저장 내용. 5건의 기기 검사 중 기록 흐름의 세션입니다.
- [process-recovery.json](process-recovery.json): 별도 프로세스 복원 시나리오의 세션. PID2657→2730, 동일ID·설명2건·INTERRUPTED·관측 자동 증가 없음.
- [복원 runner 결과](process-restore-log.txt): 복원 검증 `OK (1 test)`. seed는 살아 있는 프로세스를 의도적으로 종료하므로 [seed 로그](process-seed-log.txt)의 process crashed는 이 시험의 예상 결과입니다. 일반 앱 장애로 세지 않습니다.
- [최초 준비](adb-readiness.json), [설치 직전 준비](pre-install-readiness.json), [복원 시험 전 준비](process-readiness.json): transport/boot/package/activity·각rc 확인.
- [검사·화면 해시 요약](ci-43650320801-summary.json), [APK metadata](apk-metadata.json), [실제 APK 재대조](apk-crosscheck.json).

## APK 선택

debug 앱 SHA-256: `c559512b0a319f7a7b0f7d723ab3b304d07395489ca17a199825d55e81a8af0f`.

debug/test는 CI 개발 서명이며 같은 인증서를 사용합니다. 이전 Cloud/다른 CI 개발 키와 같다고 가정하지 않습니다. release APK는 unsigned(apksigner rc1)이므로 설치용/서명 완료 제출본이 아닙니다. APK 자체는 artifact에 있고 보존 기간은14일입니다.

이 디렉터리의 이전 CI 실패 자료는 원인과 해결 이력입니다. 최신 성공을 이전 실행에 소급하지 않습니다. 실제 Pleos 화면/마이크/Gleo/내비 핀/정비소 수신/영상은 아직 없습니다.
