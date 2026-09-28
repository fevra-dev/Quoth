package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SampleFolderTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	/** Writes a PCM WAV by hand, so the tests share no code with the decoder they check. */
	static byte[] wav(int rate, int bits, int channels, int frames, int value)
	{
		int bytesPer = bits / 8;
		int dataLen = frames * channels * bytesPer;
		ByteBuffer b = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
		b.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes());
		b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) channels)
			.putInt(rate).putInt(rate * channels * bytesPer).putShort((short) (channels * bytesPer)).putShort((short) bits);
		b.put("data".getBytes()).putInt(dataLen);
		for (int i = 0; i < frames * channels; i++)
		{
			if (bits == 8)
			{
				b.put((byte) ((value >> 8) + 128));
			}
			else if (bits == 16)
			{
				b.putShort((short) value);
			}
			else
			{
				int v = value << 8;
				b.put((byte) v).put((byte) (v >> 8)).put((byte) (v >> 16));
			}
		}
		return b.array();
	}

	private static short first(byte[] pcm)
	{
		return (short) ((pcm[0] & 0xff) | (pcm[1] << 8));
	}

	@Test
	public void sixteenBitMonoAtFullRatePassesThrough() throws Exception
	{
		byte[] pcm = Wav.decode(wav(44100, 16, 1, 441, -1234));
		assertEquals(441, pcm.length / 2);
		assertEquals(-1234, first(pcm));
	}

	@Test
	public void stereo48kDecodesToMono44k() throws Exception
	{
		assertEquals(4410, Wav.decode(wav(48000, 16, 2, 4800, 1000)).length / 2, 2); // 100 ms
	}

	@Test
	public void eightBitAndTwentyFourBitKeepTheirLevel() throws Exception
	{
		assertEquals(4410, Wav.decode(wav(22050, 8, 1, 2205, 0)).length / 2, 2);
		assertEquals(-12800, first(Wav.decode(wav(44100, 8, 1, 10, -12800))));
		assertEquals(20000, first(Wav.decode(wav(44100, 24, 1, 10, 20000))));
	}

	@Test
	public void encodeThenDecodeIsLossless() throws Exception
	{
		byte[] pcm = Wav.decode(wav(44100, 16, 1, 100, 777));
		org.junit.Assert.assertArrayEquals(pcm, Wav.decode(Wav.encode(pcm)));
	}

	@Test(expected = java.io.IOException.class)
	public void nonWavIsRefused() throws Exception
	{
		Wav.decode("not audio at all".getBytes());
	}

	@Test
	public void folderLoadsWavsAndSkipsEverythingElse() throws Exception
	{
		File dir = tmp.newFolder("quoth");
		Files.write(new File(dir, "a.wav").toPath(), wav(44100, 16, 1, 441, 1));
		Files.write(new File(dir, "b.WAV").toPath(), wav(48000, 16, 2, 480, 1));
		Files.write(new File(dir, "notes.txt").toPath(), "hi".getBytes());
		Files.write(new File(dir, "broken.wav").toPath(), "not audio".getBytes());
		assertEquals(2, new BlipPlayer(dir, null).refreshUser());
	}

	@Test
	public void missingFolderLoadsNothingAndIsNotCreated()
	{
		File absent = new File(tmp.getRoot(), "absent");
		assertEquals(0, new BlipPlayer(absent, null).refreshUser());
		assertFalse(absent.exists());
	}

	@Test
	public void soundIdsFindTheirFileCaseInsensitively() throws Exception
	{
		File dir = tmp.newFolder("ids");
		Files.write(new File(dir, "2266.WAV").toPath(), wav(22050, 16, 1, 441, 1));
		BlipPlayer p = new BlipPlayer(dir, null);
		assertTrue(p.hasUser("2266"));
		assertFalse(p.hasUser("9999"));
	}

	@Test
	public void volumeMapsToGainWithFullAsUnity()
	{
		assertEquals(0f, BlipPlayer.gainDb(100), 1e-6);
		assertEquals(-6.02f, BlipPlayer.gainDb(50), 0.01);
	}
}
