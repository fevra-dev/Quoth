package com.quoth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlipSound
{
	OFF("Off", null, -1),
	SOFT("Soft", "soft", -1),
	WARM("Warm", "warm", -1),
	REED("Reed", "reed", -1),
	BOOP("Game: Boop (2266)", null, 2266),
	SOUND_2269("Game: 2269", null, 2269),
	SOUND_2276("Game: 2276", null, 2276),
	SOUND_2278("Game: 2278", null, 2278),
	SOUND_11427("Game: 11427", null, 11427),
	CUSTOM("Game: Custom ID", null, -1);

	private static final BlipSound[] GAME_PRESETS = {BOOP, SOUND_2269, SOUND_2276, SOUND_2278, SOUND_11427};

	private final String label;
	/** Bundled sample name, or null for game sounds. */
	private final String sample;
	/** Game sound ID for presets, or -1. */
	private final int id;

	boolean isBundled()
	{
		return sample != null;
	}

	boolean isGamePreset()
	{
		return id >= 0;
	}

	/** The game preset with this ID, or {@code null}. */
	static BlipSound presetFor(int id)
	{
		for (BlipSound b : GAME_PRESETS)
		{
			if (b.id == id)
			{
				return b;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
