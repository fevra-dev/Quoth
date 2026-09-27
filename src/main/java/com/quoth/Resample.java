package com.quoth;

/** Pitch shifting by resampling 16-bit mono PCM: higher pitch plays shorter, like tape. */
final class Resample
{
	private Resample()
	{
	}

	/** Pitch ratio for a shift in semitones plus cents. */
	static double ratio(double semitones, double cents)
	{
		return Math.pow(2, (semitones + cents / 100.0) / 12.0);
	}

	/**
	 * Resamples little-endian signed 16-bit mono PCM by {@code ratio} with linear interpolation.
	 * A ratio of 2 is an octave up at half the length.
	 */
	static byte[] shift(byte[] pcm, double ratio)
	{
		int inFrames = pcm.length / 2;
		int outFrames = Math.max(1, (int) (inFrames / ratio));
		byte[] out = new byte[outFrames * 2];
		for (int i = 0; i < outFrames; i++)
		{
			double pos = i * ratio;
			int a = (int) pos;
			int b = Math.min(a + 1, inFrames - 1);
			double frac = pos - a;
			int s = (int) Math.round(sample(pcm, Math.min(a, inFrames - 1)) * (1 - frac) + sample(pcm, b) * frac);
			out[2 * i] = (byte) s;
			out[2 * i + 1] = (byte) (s >> 8);
		}
		return out;
	}

	private static int sample(byte[] pcm, int frame)
	{
		return (short) ((pcm[2 * frame] & 0xff) | (pcm[2 * frame + 1] << 8));
	}
}
