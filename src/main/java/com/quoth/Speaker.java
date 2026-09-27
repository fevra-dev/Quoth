package com.quoth;

import java.util.Locale;

/** Gives every speaker a voice of their own: a pitch offset that is stable for their name. */
final class Speaker
{
	static final int SPREAD = 4;

	private Speaker()
	{
	}

	/** Semitones in [-SPREAD, SPREAD] for an NPC name; 0 for no name (the player, message boxes). */
	static int pitchFor(String name)
	{
		if (name == null)
		{
			return 0;
		}
		String clean = name.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
		if (clean.isEmpty())
		{
			return 0;
		}
		return Math.floorMod(clean.hashCode(), 2 * SPREAD + 1) - SPREAD;
	}

	/**
	 * The colour a word starts from when it fades in: the text colour pulled most of the way
	 * toward the parchment behind dark text, or toward black behind light text.
	 */
	static int faintOf(int textColor)
	{
		int r = (textColor >> 16) & 0xff;
		int g = (textColor >> 8) & 0xff;
		int b = textColor & 0xff;
		double luma = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
		int ground = luma < 0.5 ? PARCHMENT : 0x000000;
		return Reveal.blend(textColor, ground, 0.8);
	}

	// Approximate mid-tone of the dialogue box's parchment.
	private static final int PARCHMENT = 0xc8b28a;
}
