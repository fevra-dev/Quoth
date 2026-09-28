package com.quoth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlipSound
{
	OFF("Off", null, -1, 0),
	SOFT("Soft", "soft", -1, 0),
	WARM("Warm", "warm", -1, 0),
	REED("Reed", "reed", -1, 0),
	// Game sounds by ID, ascending, picked by ear. Each plays from a matching stab file in
	// .runelite/quoth when there is one (so pitch, volume and mood apply), else from the game.
	SOUND_842("842", null, 842, 0),
	SOUND_1229("1229", null, 1229, 0),
	SOUND_1833("1833", null, 1833, 0),
	SOUND_2215("2215", null, 2215, 0),
	BOOP("2266 (Boop)", null, 2266, 0),
	SOUND_2269("2269", null, 2269, 0),
	SOUND_2269_HIGH("2269 (high)", null, 2269, 12),
	SOUND_2278("2278", null, 2278, 0),
	SOUND_3113("3113", null, 3113, 0),
	SOUND_3916("3916", null, 3916, 0),
	SOUND_5242("5242", null, 5242, 0),
	SOUND_6650("6650", null, 6650, 0),
	SOUND_7013("7013", null, 7013, 0),
	SOUND_9490("9490", null, 9490, 0),
	SOUND_9921("9921", null, 9921, 0),
	SOUND_11404("11404", null, 11404, 0),
	SOUND_11427("11427", null, 11427, 0),
	CUSTOM("Custom ID", null, -1, 0);

	// Typing an ID selects the plain preset; "2269 (high)" is only picked from the list.
	private static final BlipSound[] GAME_PRESETS = {
		SOUND_842, SOUND_1229, SOUND_1833, SOUND_2215, BOOP, SOUND_2269, SOUND_2278, SOUND_3113,
		SOUND_3916, SOUND_5242, SOUND_6650, SOUND_7013, SOUND_9490, SOUND_9921, SOUND_11404, SOUND_11427
	};

	private final String label;
	/** Bundled sample name, or null for game sounds. */
	private final String sample;
	/** Game sound ID for presets, or -1. */
	private final int id;
	/** Semitones built into this voice, on top of the Pitch setting. Needs a file to apply. */
	private final int pitch;

	boolean isBundled()
	{
		return sample != null;
	}

	/** Played by Quoth itself from a bundled file, so pitch, volume and mood always apply. */
	boolean isOwnAudio()
	{
		return isBundled();
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
