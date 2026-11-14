#!/usr/bin/env bash
set -euo pipefail
mkdir -p artifacts
collect() { adb -s emulator-5554 pull /sdcard/Android/data/dev.leemh.charok/files/evidence artifacts/device || true; }
trap collect EXIT
python3 scripts/adb_readiness.py --serial emulator-5554 --output artifacts/adb-readiness.json
adb -s emulator-5554 shell wm size 900x1000
adb -s emulator-5554 shell wm density 160
python3 scripts/adb_readiness.py --serial emulator-5554 --output artifacts/pre-install-readiness.json
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.leemh.charok.RecorderDeviceTest --console=plain
python3 scripts/process_recovery.py --serial emulator-5554 --dedicated-test-avd
