"""Turns sound files into short voice stabs for Quoth's Your samples folder. No DAW needed.

Trims leading silence, keeps the first --ms milliseconds, fades the tail so the cut does not
click, normalises to -3 dBFS, and writes 16-bit mono WAV. Accepts 8- or 16-bit PCM WAV.

Run: uv run python3 tools/make_stabs.py SRC_DIR [--out ~/.runelite/quoth] [--ms 120]
"""
import argparse
import array
import wave
from pathlib import Path

FADE_MS = 10
SILENCE = 0.02  # fraction of full scale that counts as the sound starting


def read_mono(path):
    with wave.open(str(path), "rb") as w:
        ch, width, rate = w.getnchannels(), w.getsampwidth(), w.getframerate()
        raw = w.readframes(w.getnframes())
    if width == 1:
        samples = [(b - 128) / 128.0 for b in raw]  # 8-bit WAV is unsigned
    elif width == 2:
        samples = [s / 32768.0 for s in array.array("h", raw)]
    else:
        raise ValueError("unsupported sample width %d" % width)
    if ch > 1:
        samples = [sum(samples[i:i + ch]) / ch for i in range(0, len(samples), ch)]
    return samples, rate


def stab(samples, rate, ms):
    start = next((i for i, s in enumerate(samples) if abs(s) >= SILENCE), 0)
    out = samples[start:start + int(rate * ms / 1000)]
    fade = min(len(out), int(rate * FADE_MS / 1000))
    for i in range(fade):
        out[len(out) - fade + i] *= 1 - (i + 1) / fade
    peak = max((abs(s) for s in out), default=0)
    gain = (10 ** (-3 / 20)) / peak if peak > 0 else 1
    return [s * gain for s in out]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("--out", default=str(Path.home() / ".runelite" / "quoth"))
    ap.add_argument("--ms", type=int, default=120)
    args = ap.parse_args()
    out = Path(args.out).expanduser()
    out.mkdir(parents=True, exist_ok=True)
    made = 0
    for path in sorted(Path(args.src).expanduser().glob("*.wav")):
        samples, rate = read_mono(path)
        s = stab(samples, rate, args.ms)
        with wave.open(str(out / path.name), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            w.writeframes(array.array("h", (int(x * 32767) for x in s)).tobytes())
        print("%-10s %4d ms -> %3d ms" % (path.stem, len(samples) * 1000 // rate, len(s) * 1000 // rate))
        made += 1
    print("%d stabs in %s" % (made, out))


if __name__ == "__main__":
    main()
