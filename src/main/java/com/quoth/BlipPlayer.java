package com.quoth;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Plays the bundled blips through Java Sound, off the client thread. Unlike game sounds these
 * can be pitched, but they do not follow the in-game sound effect volume, hence their own.
 */
@Slf4j
final class BlipPlayer
{
	private final Map<BlipSound, byte[]> pcm = new EnumMap<>(BlipSound.class);
	private final Random random = new Random();
	private AudioFormat format;
	private ExecutorService executor;

	void start()
	{
		for (BlipSound b : BlipSound.values())
		{
			if (!b.isBundled())
			{
				continue;
			}
			try (InputStream in = BlipPlayer.class.getResourceAsStream("blips/" + b.getSample() + ".wav");
				AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(in)))
			{
				format = audio.getFormat();
				pcm.put(b, audio.readAllBytes());
			}
			catch (Exception e)
			{
				log.warn("Quoth: could not load blip {}", b, e);
			}
		}
		executor = Executors.newSingleThreadExecutor(r ->
		{
			Thread t = new Thread(r, "quoth-blips");
			t.setDaemon(true);
			return t;
		});
	}

	void stop()
	{
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	/**
	 * @param semitones fixed pitch offset
	 * @param variationCents random spread either side, per blip
	 * @param volume 0-100
	 */
	void play(BlipSound voice, int semitones, int variationCents, int volume)
	{
		byte[] data = pcm.get(voice);
		ExecutorService ex = executor;
		if (data == null || ex == null || volume <= 0)
		{
			return;
		}
		double cents = variationCents <= 0 ? 0 : (random.nextDouble() * 2 - 1) * variationCents;
		double ratio = Resample.ratio(semitones, cents);
		ex.execute(() -> playNow(Resample.shift(data, ratio), volume));
	}

	private void playNow(byte[] data, int volume)
	{
		try
		{
			Clip clip = AudioSystem.getClip();
			clip.open(format, data, 0, data.length);
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float db = (float) (20 * Math.log10(volume / 100.0));
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
			}
			clip.addLineListener(e ->
			{
				if (e.getType() == LineEvent.Type.STOP)
				{
					clip.close();
				}
			});
			clip.start();
		}
		catch (Exception e)
		{
			log.debug("Quoth: blip failed", e);
		}
	}
}
