package com.quoth;

public enum RevealMode
{
	WORD,
	LETTER,
	/** Word by word, each word fading in over the ones before it. */
	FADE;

	// One Pace setting drives every mode. At the default 130 ms per word these give 70 ms per
	// letter and a 390 ms fade, the values picked by ear when each had its own setting.
	static final double LETTER_RATIO = 0.54;
	static final int FADE_WORDS = 3;

	/** Milliseconds between steps in this mode, for a pace in milliseconds per word. */
	int delay(int paceMs)
	{
		return this == LETTER ? Math.max(10, (int) Math.round(paceMs * LETTER_RATIO)) : Math.max(10, paceMs);
	}

	/** How long a word takes to fade in, for a pace in milliseconds per word. */
	static int fadeLength(int paceMs)
	{
		return Math.max(10, paceMs) * FADE_WORDS;
	}
}
