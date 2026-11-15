#!/usr/bin/env python3
"""Only run on this CI job's disposable AVD or a dedicated Chalog test AVD."""
import argparse
import json
import subprocess
import time
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
parser.add_argument("--dedicated-test-avd", action="store_true", required=True)
args = parser.parse_args()
adb = ["adb", "-s", args.serial]
out = Path("artifacts"); out.mkdir(exist_ok=True)
subprocess.run(["python3", "scripts/adb_readiness.py", "--serial", args.serial, "--output", str(out / "process-readiness.json")], check=True)
# Remove only this test's readiness marker; preserve the SQLite DB and all records.
subprocess.run(adb + ["shell", "run-as", "dev.leemh.charok", "rm", "-f", "files/process-seed.json"], check=True)
base = adb + ["shell", "am", "instrument", "-w", "-r", "-e", "class"]
runner = "dev.leemh.charok.test/androidx.test.runner.AndroidJUnitRunner"
with (out / "process-seed-log.txt").open("w") as log:
    seed = subprocess.Popen(base + ["dev.leemh.chalog.ProcessRecoveryTest#seedForProcessKill", runner], stdout=log, stderr=subprocess.STDOUT)
    marker = None
    try:
        for _ in range(60):
            read = subprocess.run(adb + ["shell", "run-as", "dev.leemh.charok", "cat", "files/process-seed.json"], capture_output=True, text=True, timeout=5)
            if read.returncode == 0:
                marker = json.loads(read.stdout); break
            if seed.poll() is not None: break
            time.sleep(1)
        if marker is None: raise RuntimeError("Live process seed marker not received")
        subprocess.run(adb + ["shell", "am", "force-stop", "dev.leemh.charok"], check=True)
        seed.wait(timeout=15)
    finally:
        if seed.poll() is None: seed.terminate()
result = subprocess.run(base + ["dev.leemh.chalog.ProcessRecoveryTest#verifyAfterProcessKill", runner], capture_output=True, text=True, timeout=90)
(out / "process-restore-log.txt").write_text(result.stdout + result.stderr)
assert result.returncode == 0 and "OK (1 test)" in result.stdout, result.stdout[-3000:]
print("New PID recovered the same SQLite session without restarting observation")
