#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then echo "Usage: $0 SERIAL APK_PATH" >&2; exit 2; fi
serial="$1"
apk_path="$2"
[[ -f "$apk_path" ]] || { echo "APK does not exist" >&2; exit 2; }
mkdir -p private-evidence
python3 scripts/adb_readiness.py --serial "$serial" --output private-evidence/pre-install-readiness.json
# Recheck immediately before installing. Never uninstall on signature conflict.
python3 scripts/adb_readiness.py --serial "$serial" --output private-evidence/install-readiness.json
adb -s "$serial" install -r "$apk_path"
adb -s "$serial" shell am start -W -n dev.leemh.charok/.MainActivity
echo "Check the actual screen and SDK status. Activity launch alone does not verify SDK integration."
