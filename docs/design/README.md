# 차록 디자인 v0.3

**UX 시안이며 실제 Pleos 앱 실행 화면이 아닙니다.** 시스템 차량 정보와 지도는 배치를 설명하는 도식입니다. 차록의 우측 배치는 목표 예시이며 실제 창 배치는 시스템 정책에 따릅니다.

## 직접 살펴보기

[charok-prototype.html](charok-prototype.html)을 내려받아 브라우저에서 여세요. 로컬 파일 탐색이 제한된 환경은 저장소 루트에서 다음 명령으로 실행합니다.

```bash
python3 -m http.server 8080 --bind 127.0.0.1 --directory docs/design
```

브라우저에서 `http://127.0.0.1:8080/charok-prototype.html`을 엽니다. 아래 탐색 버튼으로 각 화면을 볼 수 있고 준비 동의→시작→순간 표시→종료→상담 카드로 이어갈 수 있습니다. 주행 제한·저장 실패도 예시로 확인합니다. 시안은 상태를 메모리에서 바꿀 뿐 실제 수집/저장/전송하지 않습니다.

## 화면

| 단계 | 이미지 |
| --- | --- |
| 준비 동의 | [ready.png](ready.png) |
| 기록·대화 | [recording.png](recording.png) |
| 사용자 순간 표시 | [marked.png](marked.png) |
| 저장·요약 | [saved.png](saved.png) |
| 정차 후 상담 카드 | [handoff.png](handoff.png) |
| 저장 실패 | [failure.png](failure.png) |
| 좁은 앱 창 | [narrow-recording.png](narrow-recording.png) |

글꼴은 Noto Sans CJK KR을 사용해 캡처했습니다. 외부 이미지·음원·지도 타일을 사용하지 않았고 차량/지도 도식은 직접 작성한 SVG입니다. 공식 Pleos 화면의 픽셀 단위 복제나 규격 인증 결과가 아닙니다.

## 검증

Python Playwright와 Chromium 환경에서 `python scripts/check_design.py`. 실제 Android 테스트와 분리된 시안 조작 검사입니다. 캡처와 [JSON 결과](../evidence/planning-design/prototype-checks.json)를 갱신합니다. 좁은 화면은 차록 패널만 보이며 실제 OS가 시스템 영역을 숨긴다는 의미가 아닙니다.
