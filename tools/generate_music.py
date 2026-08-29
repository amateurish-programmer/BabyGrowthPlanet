"""Generate three small, original, loopable PCM background tracks."""
from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22_050
BEAT_SECONDS = 0.5
BEATS = 32

THEMES = {
    "music_space.wav": ([261.63, 329.63, 392.00, 523.25], 0.18, 0.002),
    "music_forest.wav": ([220.00, 277.18, 329.63, 440.00], 0.12, 0.004),
    "music_rainbow.wav": ([293.66, 369.99, 440.00, 587.33], 0.23, 0.003),
}


def make_track(path: Path, notes: list[float], sparkle_rate: float, vibrato: float) -> None:
    frames = bytearray()
    total_samples = int(SAMPLE_RATE * BEAT_SECONDS * BEATS)
    for index in range(total_samples):
        time = index / SAMPLE_RATE
        beat = int(time / BEAT_SECONDS)
        beat_phase = (time % BEAT_SECONDS) / BEAT_SECONDS
        root = notes[beat % len(notes)]
        envelope = min(1.0, beat_phase * 12.0) * (0.78 + 0.22 * math.cos(math.pi * beat_phase))
        wobble = 1.0 + vibrato * math.sin(2.0 * math.pi * 4.0 * time)
        tone = (
            0.56 * math.sin(2.0 * math.pi * root * wobble * time)
            + 0.24 * math.sin(2.0 * math.pi * (root * 1.5) * time)
            + 0.12 * math.sin(2.0 * math.pi * (root * 2.0) * time)
            + sparkle_rate * math.sin(2.0 * math.pi * (notes[(beat + 2) % len(notes)] * 2.0) * time)
        )
        sample = int(max(-1.0, min(1.0, tone * envelope * 0.26)) * 32767)
        frames.extend(struct.pack("<h", sample))
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(frames)


if __name__ == "__main__":
    output_dir = Path(__file__).parents[1] / "app" / "src" / "main" / "res" / "raw"
    output_dir.mkdir(parents=True, exist_ok=True)
    for filename, (notes, sparkle, vibrato) in THEMES.items():
        make_track(output_dir / filename, notes, sparkle, vibrato)
