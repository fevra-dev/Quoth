package com.quoth;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;

/**
 * Plays Quoth's own voices, and any sound files the player has added, through RuneLite's
 * AudioPlayer, off the client thread. Each blip is decoded once, then pitched and trimmed in
 * memory and handed over as a small WAV. Unlike game sounds these can be pitched and shortened,
 * but they do not follow the in-game sound effect volume, hence their own.
 */
@Slf4j
final class BlipPlayer
{
	private static final int FADE_OUT_MS = 5;
	// A voice blip is a fraction of a second; anything longer is cut on load so a stray song in
	// the folder cannot eat memory. Blips are trimmed further as they play.
	private static final int MAX_SAMPLE_MS = 2000;
	private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

	private final Map<BlipSound, byte[]> bundled = new EnumMap<>(BlipSound.class);
	private final Random random = new Random();
	private final File userDir;
	private final AudioPlayer audio;
	private volatile Map<String, byte[]> user = new LinkedHashMap<>();
	private long userDirStamp = Long.MIN_VALUE;
	private ExecutorService executor;

	BlipPlayer(File userDir, AudioPlayer audio)
	{
		this.userDir = userDir;
		this.audio = audio;
	}

	void start()
	{
		for (BlipSound b : BlipSound.values())
		{
			if (!b.isBundled())
			{
				continue;
			}
			try (InputStream in = BlipPlayer.class.getResourceAsStream("blips/" + b.getSample() + ".wav"))
			{
				bundled.put(b, Wav.decode(in.readAllBytes()));
			}
			catch (Exception e)
			{
				log.warn("Quoth: could not load blip {}", b, e);
			}
		}
		// The samples folder is optional and never created: only read if the player made it.
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
				if (!f.isFile() || f.length() > MAX_FILE_BYTES || !name.toLowerCase(Locale.ROOT).endsWith(".wav"))
				{
					continue;
				}
				try
				{
					byte[] pcm = Wav.decode(Files.readAllBytes(f.toPath()));
					loaded.put(stripExtension(name), Resample.trim(pcm, Wav.RATE * MAX_SAMPLE_MS / 1000, 0));
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
		int maxFrames = lengthMs <= 0 ? 0 : Wav.RATE * lengthMs / 1000;
		int fadeFrames = Wav.RATE * FADE_OUT_MS / 1000;
		float gainDb = gainDb(volume);
		ex.execute(() ->
		{
			byte[] data = sampleName != null ? pickUser(sampleName) : bundled.get(voice);
			if (data == null)
			{
				return;
			}
			byte[] wav = Wav.encode(Resample.trim(Resample.shift(data, ratio), maxFrames, fadeFrames));
			try
			{
				audio.play(new ByteArrayInputStream(wav), gainDb);
			}
			catch (Exception e)
			{
				log.debug("Quoth: blip failed", e);
			}
		});
	}

	/** 0-100 volume as decibels of gain; 100 is unity. */
	static float gainDb(int volume)
	{
		return (float) (20 * Math.log10(Math.max(1, Math.min(100, volume)) / 100.0));
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

	private static String stripExtension(String name)
	{
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}
