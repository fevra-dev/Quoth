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

	/**
	 * Cuts 16-bit mono PCM to at most {@code maxFrames}, ramping the last {@code fadeFrames}
	 * down to silence so the cut does not click. Shorter input is returned unchanged.
	 */
	static byte[] trim(byte[] pcm, int maxFrames, int fadeFrames)
	{
		int frames = pcm.length / 2;
		if (maxFrames <= 0 || maxFrames >= frames)
		{
			return pcm;
		}
		byte[] out = java.util.Arrays.copyOf(pcm, maxFrames * 2);
		int fade = Math.min(fadeFrames, maxFrames);
		for (int i = 0; i < fade; i++)
		{
			int frame = maxFrames - fade + i;
			double gain = 1 - (i + 1) / (double) fade;
			int s = (int) Math.round(sample(out, frame) * gain);
			out[2 * frame] = (byte) s;
			out[2 * frame + 1] = (byte) (s >> 8);
		}
		return out;
	}

	/** Averages interleaved 16-bit little-endian channels down to mono. Mono passes through. */
	static byte[] downmix(byte[] pcm, int channels)
	{
		if (channels <= 1)
		{
			return pcm;
		}
		int frames = pcm.length / (2 * channels);
		byte[] out = new byte[frames * 2];
		for (int f = 0; f < frames; f++)
		{
			int sum = 0;
			for (int c = 0; c < channels; c++)
			{
				sum += sample(pcm, f * channels + c);
			}
			int s = Math.round(sum / (float) channels);
			out[2 * f] = (byte) s;
			out[2 * f + 1] = (byte) (s >> 8);
		}
		return out;
	}

	private static int sample(byte[] pcm, int frame)
	{
		return (short) ((pcm[2 * frame] & 0xff) | (pcm[2 * frame + 1] << 8));
	}
}
