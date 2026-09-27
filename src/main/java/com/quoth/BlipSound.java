package com.quoth;

import java.util.Arrays;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlipSound
{
	OFF("Off", -1),
	BOOP("Boop (2266)", 2266),
	SOUND_2269("2269", 2269),
	SOUND_2276("2276", 2276),
	SOUND_2278("2278", 2278),
	RANDOM("Random (pool)", -1),
	CUSTOM("Custom", -1);

	private static final BlipSound[] PRESETS = {BOOP, SOUND_2269, SOUND_2276, SOUND_2278};

	private final String label;
	private final int id;

	boolean isPreset()
	{
		return id >= 0;
	}

	/** The preset with this ID, or {@code null}. */
	static BlipSound presetFor(int id)
	{
		for (BlipSound b : PRESETS)
		{
			if (b.id == id)
			{
				return b;
			}
		}
		return null;
	}

	static final String DEFAULT_POOL = "2266, 2269, 2276, 2278";

	/**
	 * Parses a comma-separated list of sound IDs, skipping anything that is not a
	 * non-negative number. An empty result falls back to the presets, so Random is never silent.
	 */
	static int[] parsePool(String text)
	{
		int[] ids = new int[0];
		if (text != null)
		{
			ids = Arrays.stream(text.split(","))
				.map(String::trim)
				.filter(s -> s.matches("\\d{1,6}"))
				.mapToInt(Integer::parseInt)
				.toArray();
		}
		return ids.length > 0 ? ids : Arrays.stream(PRESETS).mapToInt(BlipSound::getId).toArray();
	}

	static int pick(int[] pool, Random random)
	{
		return pool[random.nextInt(pool.length)];
	}

	@Override
	public String toString()
	{
		return label;
	}
}
