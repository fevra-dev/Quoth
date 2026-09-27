"""Generates Quoth's bundled voice blips. Pure synthesis, no samples, so no licensing.

Run: uv run python3 tools/gen_blips.py   (writes src/main/resources/com/quoth/blips/*.wav)
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/com/quoth/blips"

# name: (fundamental Hz, length ms, attack ms, decay tau ms, harmonic amplitudes, pitch glide)
VOICES = {
    "soft": (587.3, 55, 4, 18, [1.0, 0.02], 0.0),                 # near-pure sine, D5
    "warm": (392.0, 70, 5, 25, [1.0, 0.35, 0.15], -0.03),         # rounder, slight fall, G4
    "reed": (293.7, 60, 3, 20, [1.0 / n if n % 2 else 0.0 for n in range(1, 10)], 0.0),  # band-limited square, D4
}


def render(freq, ms, attack, tau, harmonics, glide):
    n = int(RATE * ms / 1000)
    fade = int(RATE * 0.005)
    phase = 0.0
    out = []
    for i in range(n):
        t = i / RATE
        f = freq * (1 + glide * i / n)
        phase += 2 * math.pi * f / RATE
        s = sum(a * math.sin(k * phase) for k, a in enumerate(harmonics, 1))
        env = min(1.0, t * 1000 / attack) * math.exp(-t * 1000 / tau)
        if i > n - fade:
            env *= (n - i) / fade  # tail to exact zero: no click
        out.append(s * env)
    peak = max(abs(x) for x in out)
    gain = 10 ** (-3 / 20) / peak  # normalise to -3 dBFS
    return [x * gain for x in out]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, spec in VOICES.items():
        samples = render(*spec)
        with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(b"".join(struct.pack("<h", int(x * 32767)) for x in samples))
        print(name, len(samples), "frames")


if __name__ == "__main__":
    main()
