"""Check the browser UX prototype; this is not an Android/SDK test."""
import json
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "docs/design"
server = ThreadingHTTPServer(("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=str(DESIGN)))
Thread(target=server.serve_forever, daemon=True).start()
URL = f"http://127.0.0.1:{server.server_port}/chalog-prototype.html"
results = {"level": "browser UX simulation only", "checks": [], "layouts": []}


def passed(name):
    results["checks"].append(name)


with sync_playwright() as p:
    browser = p.chromium.launch(executable_path="/usr/bin/chromium", headless=True,
                                args=["--no-sandbox"])
    page = browser.new_page(viewport={"width": 1600, "height": 930})
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.goto(URL)
    assert page.locator("#arm").is_disabled()
    page.locator("#consent").check()
    page.locator("#arm").click()
    assert "60초" in page.locator("#panel").inner_text()
    passed("explicit TEST preparation consent")
    page.locator('[data-go="recording"]').click()
    page.locator('[data-go="marked"]').click()
    assert "사용자가 표시한 순간" in page.locator("#panel").inner_text()
    page.locator('[data-go="saving"]').click()
    assert "저장하고 있어요" in page.locator("#panel").inner_text()
    page.wait_for_selector('[data-go="handoff"]')
    assert "저장 완료 · 시뮬레이션" in page.locator("#panel").inner_text()
    passed("record → bookmark → saving → simulated saved")
    page.locator("#driving").click()
    assert page.locator('[data-go="handoff"]').is_disabled()
    page.locator('[data-scene="handoff"]').click()
    assert "정차 후" in page.locator("#panel").inner_text()
    passed("driving locks summary and direct scene access")
    page.locator("#driving").click()
    page.locator('[data-go="export"]').click()
    assert "파일을 생성·전송하지 않습니다" in page.locator("#panel").inner_text()
    page.locator('[data-go="center"]').click()
    assert page.get_by_role("button", name="연결 전 · 안내 불가").is_disabled()
    passed("export and center are explicitly disconnected")
    page.locator('[data-scene="failure"]').click()
    page.locator('[data-go="saving"]').click()
    page.wait_for_selector('[data-go="closed"]')
    page.locator('[data-go="closed"]').click()
    assert "수집을 자동으로 재개하지 않습니다" in page.locator("#panel").inner_text()
    passed("failure retry and close states")
    for width, height in [(1600, 930), (1000, 1000), (430, 1000)]:
        page.set_viewport_size({"width": width, "height": height})
        for scene in ["ready", "armed", "recording", "marked", "saved", "handoff", "export", "center", "failure", "closed"]:
            page.goto(URL + "?scene=" + scene)
            metrics = page.locator("#panel").evaluate("x => ({scroll:x.scrollHeight,height:x.clientHeight,width:x.scrollWidth,client:x.clientWidth})")
            assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), (width, scene, "page horizontal overflow")
            assert metrics["width"] <= metrics["client"], (width, scene, "panel horizontal overflow")
            if scene in ("recording", "marked"):
                assert metrics["scroll"] <= metrics["height"], (width, scene, "driving content needs scroll")
            results["layouts"].append({"viewportWidth": width, "scene": scene, **metrics})
            if width == 1600 and scene in ("ready", "recording", "marked", "saved", "handoff", "failure"):
                page.screenshot(path=str(DESIGN / (scene + ".png")), full_page=True)
            if width == 430 and scene == "recording":
                page.screenshot(path=str(DESIGN / "narrow-recording.png"), full_page=True)
    assert not errors, errors
    passed("30 responsive layouts; driving views need no scroll; no JavaScript errors")
    browser.close()
server.shutdown()

output = ROOT / "docs/evidence/planning-design/prototype-checks.json"
output.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n")
print(json.dumps({"checks": results["checks"], "layouts": len(results["layouts"])}, ensure_ascii=False))
