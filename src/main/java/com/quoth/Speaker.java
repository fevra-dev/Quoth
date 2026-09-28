package com.quoth;

import java.util.Locale;

/** Gives every speaker a voice of their own: a pitch offset that is stable for their name. */
final class Speaker
{
	static final int SPREAD = 4;
	/** How far a species word moves a voice, in semitones. */
	static final int SPECIES_SHIFT = 5;
	/** The per-name wobble left on top of a species shift, so two goblins still differ. */
	static final int SPECIES_SPREAD = 2;

	private static final String[] GRUFF = {
		"goblin", "hobgoblin", "dwarf", "dwarven", "troll", "ogre", "ogress", "giant", "demon", "dragon",
		"golem", "barbarian", "orc", "cyclops", "gorilla", "bear"
	};
	private static final String[] SMALL = {
		"gnome", "imp", "fairy", "pixie", "sprite", "child", "boy", "girl", "kid", "chick", "kitten"
	};

	private Speaker()
	{
	}

	/**
	 * Semitones for an NPC name: a species shift if the name has one, plus a small per-name
	 * wobble; otherwise within [-SPREAD, SPREAD]. 0 for no name (the player, message boxes).
	 */
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
		int species = speciesShift(clean);
		if (species != 0)
		{
			return species + Math.floorMod(clean.hashCode(), 2 * SPECIES_SPREAD + 1) - SPECIES_SPREAD;
		}
		return Math.floorMod(clean.hashCode(), 2 * SPREAD + 1) - SPREAD;
	}

	/** -SPECIES_SHIFT for big, gruff creatures, +SPECIES_SHIFT for small ones, else 0. */
	static int speciesShift(String lowerName)
	{
		for (String word : lowerName.split("[^a-z]+"))
		{
			for (String g : GRUFF)
			{
				if (word.equals(g) || word.equals(g + "s"))
				{
					return -SPECIES_SHIFT;
				}
			}
			for (String s : SMALL)
			{
				if (word.equals(s) || word.equals(s + "s"))
				{
					return SPECIES_SHIFT;
				}
			}
		}
		return 0;
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
