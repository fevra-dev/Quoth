package com.quoth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ResampleTest
{
	private static byte[] ramp(int frames)
	{
		byte[] pcm = new byte[frames * 2];
		for (int i = 0; i < frames; i++)
		{
			int s = i * 100 - 5000;
			pcm[2 * i] = (byte) s;
			pcm[2 * i + 1] = (byte) (s >> 8);
		}
		return pcm;
	}

	@Test
	public void ratioIsEqualTempered()
	{
		assertEquals(2.0, Resample.ratio(12, 0), 1e-9);
		assertEquals(0.5, Resample.ratio(-12, 0), 1e-9);
		assertEquals(Resample.ratio(1, 0), Resample.ratio(0, 100), 1e-9);
	}

	@Test
	public void unityRatioIsLossless()
	{
		byte[] pcm = ramp(100);
		assertArrayEquals(pcm, Resample.shift(pcm, 1.0));
	}

	@Test
	public void octaveUpHalvesLengthAndKeepsEverySecondSample()
	{
		byte[] pcm = ramp(100);
		byte[] up = Resample.shift(pcm, 2.0);
		assertEquals(pcm.length / 2, up.length);
		assertEquals(pcm[4], up[2]);
		assertEquals(pcm[5], up[3]);
	}

	@Test
	public void negativeSamplesSurviveTheRoundTrip()
	{
		byte[] pcm = ramp(10); // starts at -5000
		byte[] out = Resample.shift(pcm, 1.0);
		assertEquals(-5000, (short) ((out[0] & 0xff) | (out[1] << 8)));
	}
}
