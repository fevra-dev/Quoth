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
	SOFTER("Softer", "soft", -1, -9),
	REED("Reed", "reed", -1, 0),
	BELL("Bell", "bell", -1, 0),
	PIP("Pip", "pip", -1, 0),
	WOOD("Wood", "wood", -1, 0),
	// Game sounds by ID, ascending, picked by ear. Each plays from a matching stab file in
	// .runelite/quoth when there is one (so pitch, volume and mood apply), else from the game.
	SOUND_1833("Game 1833", null, 1833, 0),
	SOUND_2215("Game 2215", null, 2215, 0),
	BOOP("Game 2266", null, 2266, 0),
	SOUND_2269("Game 2269", null, 2269, 0),
	SOUND_2278("Game 2278", null, 2278, 0),
	SOUND_5242("Game 5242", null, 5242, 0),
	SOUND_7013("Game 7013", null, 7013, 0),
	CUSTOM("Game (Custom ID)", null, -1, 0);

	private static final BlipSound[] GAME_PRESETS = {
		SOUND_1833, SOUND_2215, BOOP, SOUND_2269, SOUND_2278, SOUND_5242, SOUND_7013
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
