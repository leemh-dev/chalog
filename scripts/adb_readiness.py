#!/usr/bin/env python3
"""Read-only transport/framework gate; no auth changes, key copying or data reset."""
import argparse
import json
import subprocess
import sys
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
parser.add_argument("--adb", default="adb")
parser.add_argument("--output", required=True)
args = parser.parse_args()
checks = {}
commands = {
    "devices": [args.adb, "devices", "-l"],
    "transport": [args.adb, "-s", args.serial, "get-state"],
    "boot": [args.adb, "-s", args.serial, "shell", "getprop", "sys.boot_completed"],
    "package": [args.adb, "-s", args.serial, "shell", "service", "check", "package"],
    "activity": [args.adb, "-s", args.serial, "shell", "service", "check", "activity"],
}
for name, command in commands.items():
    # Do not issue shell commands through unauthorized/offline transport.
    if name not in ("devices", "transport") and not (checks["transport"]["rc"] == 0 and checks["transport"]["stdout"].strip() == "device"):
        checks[name] = {"rc": None, "stdout": "", "stderr": "skipped: transport not ready"}
        continue
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=8)
        checks[name] = {"rc": result.returncode, "stdout": result.stdout, "stderr": result.stderr}
    except (subprocess.TimeoutExpired, OSError) as error:
        checks[name] = {"rc": None, "stdout": "", "stderr": type(error).__name__}
ready = all(x["rc"] == 0 for x in checks.values()) and checks["transport"]["stdout"].strip() == "device" and checks["boot"]["stdout"].strip() == "1" and all(f"Service {name}: found" in checks[name]["stdout"] for name in ("package", "activity"))
report = {"ready": ready, "serial": args.serial, "checks": checks}
out = Path(args.output); out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
print(json.dumps({"ready": ready, "report": str(out)}))
sys.exit(0 if ready else 1)
