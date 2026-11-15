#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path

args = argparse.ArgumentParser(); args.add_argument("--output", required=True); options = args.parse_args()
sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "/workspace/tools/android-sdk")))
signers = sorted(sdk.glob("build-tools/*/apksigner"))
badgers = sorted(sdk.glob("build-tools/*/aapt"))
files = []
for apk in sorted(Path("app/build/outputs/apk").rglob("*.apk")):
    signature = subprocess.run([str(signers[-1]), "verify", "--print-certs", str(apk)], capture_output=True, text=True) if signers else None
    badging = subprocess.run([str(badgers[-1]), "dump", "badging", str(apk)], capture_output=True, text=True) if badgers else None
    files.append({"path": str(apk), "bytes": apk.stat().st_size, "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
                  "package": next((line for line in badging.stdout.splitlines() if line.startswith("package:")), "") if badging else "not checked",
                  "signature_rc": signature.returncode if signature else None,
                  "signature": signature.stdout if signature and signature.returncode == 0 else "unsigned or verification failed; not an installable signed release",
                  "signing_purpose": "development/test" if "debug" in str(apk).lower() else "unsigned release"})
result = {"code_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip(),
          "working_tree_dirty": bool(subprocess.check_output(["git", "status", "--porcelain"], text=True).strip()),
          "apk_files": files, "device_validation": "see separate device reports; hashes do not prove execution"}
out = Path(options.output); out.parent.mkdir(parents=True, exist_ok=True); out.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
print(f"Recorded metadata for {len(files)} APK files")
