package com.quoth;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
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
 * Plays Quoth's own voices and the player's samples through Java Sound, off the client thread.
 * Unlike game sounds these can be pitched and shortened, but they do not follow the in-game
 * sound effect volume, hence their own. Everything is decoded once to 44.1 kHz 16-bit mono.
 */
@Slf4j
final class BlipPlayer
{
	static final AudioFormat FORMAT = new AudioFormat(44100f, 16, 1, true, false);
	private static final int FADE_OUT_MS = 5;
	// A voice blip is a fraction of a second; anything longer is cut on load so a stray song in
	// the folder cannot eat memory. Length trims further per blip.
	private static final int MAX_SAMPLE_MS = 2000;
	private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

	private final Map<BlipSound, byte[]> bundled = new EnumMap<>(BlipSound.class);
	private final Random random = new Random();
	private final File userDir;
	private volatile Map<String, byte[]> user = new LinkedHashMap<>();
	private long userDirStamp = Long.MIN_VALUE;
	private ExecutorService executor;

	BlipPlayer(File userDir)
	{
		this.userDir = userDir;
	}

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
				bundled.put(b, decode(audio));
			}
			catch (Exception e)
			{
				log.warn("Quoth: could not load blip {}", b, e);
			}
		}
		if (!userDir.isDirectory() && !userDir.mkdirs())
		{
			log.warn("Quoth: could not create {}", userDir);
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

	File userDir()
	{
		return userDir;
	}

	/** Whether the folder holds a sample with this name (no extension), rescanning if it changed. */
	boolean hasUser(String name)
	{
		refreshUser();
		for (String key : user.keySet())
		{
			if (key.equalsIgnoreCase(name))
			{
				return true;
			}
		}
		return false;
	}

	/** Rescans the samples folder if it changed and returns how many samples loaded. */
	synchronized int refreshUser()
	{
		long stamp = userDir.lastModified();
		if (stamp == userDirStamp)
		{
			return user.size();
		}
		userDirStamp = stamp;
		Map<String, byte[]> loaded = new LinkedHashMap<>();
		File[] files = userDir.listFiles();
		if (files != null)
		{
			Arrays.sort(files);
			for (File f : files)
			{
				String name = f.getName();
				String lower = name.toLowerCase(Locale.ROOT);
				if (!f.isFile() || f.length() > MAX_FILE_BYTES
					|| !(lower.endsWith(".wav") || lower.endsWith(".aif") || lower.endsWith(".aiff")))
				{
					continue;
				}
				try (AudioInputStream audio = AudioSystem.getAudioInputStream(f))
				{
					byte[] pcm = decode(audio);
					int maxFrames = (int) (FORMAT.getSampleRate() * MAX_SAMPLE_MS / 1000);
					loaded.put(stripExtension(name), Resample.trim(pcm, maxFrames, 0));
				}
				catch (Exception e)
				{
					log.warn("Quoth: skipped sample {}: {}", name, e.getMessage());
				}
			}
		}
		user = loaded;
		return loaded.size();
	}

	/**
	 * @param sampleName a sample in the folder to play, by name without extension; null plays
	 *                   the bundled voice
	 * @param semitones fixed pitch offset
	 * @param variationCents random spread either side, per blip
	 * @param volume 0-100
	 * @param lengthMs cut each blip to this long after pitching; 0 plays it whole
	 */
	void play(BlipSound voice, String sampleName, int semitones, int variationCents, int volume, int lengthMs)
	{
		ExecutorService ex = executor;
		if (ex == null || volume <= 0)
		{
			return;
		}
		double cents = variationCents <= 0 ? 0 : (random.nextDouble() * 2 - 1) * variationCents;
		double ratio = Resample.ratio(semitones, cents);
		int maxFrames = lengthMs <= 0 ? 0 : (int) (FORMAT.getSampleRate() * lengthMs / 1000);
		int fadeFrames = (int) (FORMAT.getSampleRate() * FADE_OUT_MS / 1000);
		ex.execute(() ->
		{
			byte[] data = sampleName != null ? pickUser(sampleName) : bundled.get(voice);
			if (data != null)
			{
				playNow(Resample.trim(Resample.shift(data, ratio), maxFrames, fadeFrames), volume);
			}
		});
	}

	private byte[] pickUser(String name)
	{
		refreshUser();
		for (Map.Entry<String, byte[]> e : user.entrySet())
		{
			if (e.getKey().equalsIgnoreCase(name))
			{
				return e.getValue();
			}
		}
		return null;
	}

	/** Decodes any Java Sound PCM input to {@link #FORMAT}: 16-bit, mono, 44.1 kHz. */
	static byte[] decode(AudioInputStream audio) throws Exception
	{
		AudioFormat src = audio.getFormat();
		AudioFormat pcm16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16,
			src.getChannels(), src.getChannels() * 2, src.getSampleRate(), false);
		byte[] data;
		try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm16, audio))
		{
			data = converted.readAllBytes();
		}
		data = Resample.downmix(data, src.getChannels());
		double rateRatio = src.getSampleRate() / FORMAT.getSampleRate();
		return Math.abs(rateRatio - 1) < 1e-6 ? data : Resample.shift(data, rateRatio);
	}

	private static String stripExtension(String name)
	{
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	private void playNow(byte[] data, int volume)
	{
		try
		{
			Clip clip = AudioSystem.getClip();
			clip.open(FORMAT, data, 0, data.length);
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
