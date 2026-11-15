#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 1 ]]; then echo "Usage: $0 SERIAL" >&2; exit 2; fi
serial="$1"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p private-evidence
python3 scripts/adb_readiness.py --serial "$serial" --output "private-evidence/record-readiness-$stamp.json"
remote_path="/sdcard/Download/chalog-$stamp.mp4"
adb -s "$serial" exec-out screencap -p > "private-evidence/before-$stamp.png"
echo "Recording the ACTUAL display for up to 120 seconds. Operate Chalog now."
adb -s "$serial" shell screenrecord --time-limit 120 "$remote_path"
adb -s "$serial" pull "$remote_path" "private-evidence/chalog-$stamp.mp4"
echo "Saved under private-evidence. Review and remove personal information before publishing."
