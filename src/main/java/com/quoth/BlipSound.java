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
	// Game sounds, ascending. Picked by ear; the game names none of them.
	SOUND_842("Game: 842", null, 842),
	SOUND_1229("Game: 1229", null, 1229),
	SOUND_1458("Game: 1458", null, 1458),
	SOUND_1833("Game: 1833", null, 1833),
	SOUND_2050("Game: 2050", null, 2050),
	SOUND_2215("Game: 2215", null, 2215),
	SOUND_2259("Game: 2259", null, 2259),
	BOOP("Game: Boop (2266)", null, 2266),
	SOUND_2269("Game: 2269", null, 2269),
	SOUND_2276("Game: 2276", null, 2276),
	SOUND_2278("Game: 2278", null, 2278),
	SOUND_2838("Game: 2838", null, 2838),
	SOUND_3113("Game: 3113", null, 3113),
	SOUND_3916("Game: 3916", null, 3916),
	SOUND_5242("Game: 5242", null, 5242),
	SOUND_6650("Game: 6650", null, 6650),
	SOUND_7013("Game: 7013", null, 7013),
	SOUND_9490("Game: 9490", null, 9490),
	SOUND_9921("Game: 9921", null, 9921),
	SOUND_11404("Game: 11404", null, 11404),
	SOUND_11427("Game: 11427", null, 11427),
	CUSTOM("Game: Custom ID", null, -1);

	private static final BlipSound[] GAME_PRESETS = {
		SOUND_842, SOUND_1229, SOUND_1458, SOUND_1833, SOUND_2050, SOUND_2215, SOUND_2259, BOOP, SOUND_2269, SOUND_2276, SOUND_2278, SOUND_2838, SOUND_3113, SOUND_3916, SOUND_5242, SOUND_6650, SOUND_7013, SOUND_9490, SOUND_9921, SOUND_11404, SOUND_11427
	};

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
