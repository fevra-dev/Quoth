package com.quoth;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SampleFolderTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static void writeWav(File f, float rate, int bits, int channels, int frames) throws Exception
	{
		AudioFormat fmt = new AudioFormat(rate, bits, channels, bits > 8, false);
		byte[] data = new byte[frames * fmt.getFrameSize()];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (byte) (i * 7);
		}
		AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(data), fmt, frames);
		AudioSystem.write(in, AudioFileFormat.Type.WAVE, f);
	}

	@Test
	public void stereo48kDecodesToMono44k() throws Exception
	{
		File f = tmp.newFile("stereo.wav");
		writeWav(f, 48000f, 16, 2, 4800); // 100 ms
		try (AudioInputStream in = AudioSystem.getAudioInputStream(f))
		{
			int frames = BlipPlayer.decode(in).length / 2;
			assertEquals(4410, frames, 2);
		}
	}

	@Test
	public void eightBitMono22kDecodesToMono44k() throws Exception
	{
		File f = tmp.newFile("lofi.wav");
		writeWav(f, 22050f, 8, 1, 2205); // 100 ms
		try (AudioInputStream in = AudioSystem.getAudioInputStream(f))
		{
			assertEquals(4410, BlipPlayer.decode(in).length / 2, 2);
		}
	}

	@Test
	public void folderLoadsAudioAndSkipsEverythingElse() throws Exception
	{
		File dir = tmp.newFolder("quoth");
		writeWav(new File(dir, "a.wav"), 44100f, 16, 1, 441);
		writeWav(new File(dir, "b.WAV"), 48000f, 16, 2, 480);
		Files.write(new File(dir, "notes.txt").toPath(), "hi".getBytes());
		Files.write(new File(dir, "broken.wav").toPath(), "not audio".getBytes());
		assertEquals(2, new BlipPlayer(dir).refreshUser());
	}

	@Test
	public void emptyOrMissingFolderLoadsNothing()
	{
		assertEquals(0, new BlipPlayer(new File(tmp.getRoot(), "absent")).refreshUser());
	}
}
