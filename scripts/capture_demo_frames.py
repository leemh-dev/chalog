#!/usr/bin/env python3
"""Record the actual complete Android display, including SystemUI overlays.

Each PNG has a measured capture timestamp; no UI is reconstructed. Create STOP
in the output directory to finish. Encoding happens later, with the AVD off.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import time

p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=Path, required=True)
p.add_argument('--fps', type=float, default=5)
p.add_argument('--limit', type=float, default=900)
a = p.parse_args()
assert 0 < a.fps <= 15 and 0 < a.limit <= 1800
a.output.mkdir(parents=True, exist_ok=False)
(a.output / 'frames').mkdir()
start = time.time()
index = 0
with (a.output / 'frames.jsonl').open('w') as manifest:
    while time.time() - start < a.limit and not (a.output / 'STOP').exists():
        began = time.time()
        data = subprocess.check_output(
            [a.adb, '-s', a.serial, 'exec-out', 'screencap', '-p'], timeout=20)
        ended = time.time()
        assert data.startswith(b'\x89PNG\r\n\x1a\n'), 'Invalid actual capture'
        width, height = struct.unpack('>II', data[16:24])
        name = f'frames/{index:06d}.png'
        (a.output / name).write_bytes(data)
        manifest.write(json.dumps(dict(path=name, capture_start=began,
            capture_end=ended, timestamp=(began + ended) / 2, width=width,
            height=height, sha256=hashlib.sha256(data).hexdigest())) + '\n')
        manifest.flush()
        index += 1
        if index % 100 == 0:
            print(f'{index} actual frames, {ended - start:.1f}s elapsed', flush=True)
        time.sleep(max(0, 1 / a.fps - (time.time() - began)))
(a.output / 'capture.json').write_text(json.dumps(dict(start=start,
    end=time.time(), frames=index, requested_fps=a.fps, audio=False,
    source='adb exec-out screencap -p; actual complete display'), indent=2) + '\n')
print(f'Finished: {index} actual display frames', flush=True)
