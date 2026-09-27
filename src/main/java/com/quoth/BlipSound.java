package com.quoth;

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
	RANDOM("Random", -1),
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

	static int randomPresetId(Random random)
	{
		return PRESETS[random.nextInt(PRESETS.length)].id;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
