package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoodTest
{
	@Test
	public void questionAndExclamationMarkTheirWord()
	{
		Reveal r = Reveal.of("Did you bring them? Hurry! \"Now?!\" ok", RevealMode.WORD);
		// Did you bring them? Hurry! "Now?!" ok
		assertEquals(0, r.mood(0));
		assertEquals(Reveal.MOOD_QUESTION, r.mood(3));
		assertEquals(Reveal.MOOD_EXCLAIM, r.mood(4));
		assertEquals(Reveal.MOOD_QUESTION | Reveal.MOOD_EXCLAIM, r.mood(5));
		assertEquals(0, r.mood(6));
		assertEquals(0, r.mood(99));
	}

	@Test
	public void letterModeMarksThePunctuationItself()
	{
		Reveal r = Reveal.of("Hi?", RevealMode.LETTER);
		assertEquals(0, r.mood(1));
		assertEquals(Reveal.MOOD_QUESTION, r.mood(2));
	}

	@Test
	public void colourTagsDoNotHideTheMark()
	{
		Reveal r = Reveal.of("<col=ff0000>Run!</col>", RevealMode.WORD);
		assertEquals(Reveal.MOOD_EXCLAIM, r.mood(0));
	}

	@Test
	public void bigFolkSoundLowerAndSmallFolkHigher()
	{
		for (String n : new String[]{"Goblin", "Dwarf", "Mountain Dwarf", "Cave goblin guard", "Hill Giant", "Ogress Warrior"})
		{
			int p = Speaker.pitchFor(n);
			assertTrue(n + " -> " + p, p <= -Speaker.SPECIES_SHIFT + Speaker.SPECIES_SPREAD);
		}
		for (String n : new String[]{"Gnome child", "Imp", "Fairy Nuff"})
		{
			int p = Speaker.pitchFor(n);
			assertTrue(n + " -> " + p, p >= Speaker.SPECIES_SHIFT - Speaker.SPECIES_SPREAD);
		}
	}

	@Test
	public void speciesMatchesWholeWordsOnly()
	{
		// "Impling" and "Bearded" must not match "imp" or "bear".
		assertEquals(0, Speaker.speciesShift("impling"));
		assertEquals(0, Speaker.speciesShift("bearded man"));
		assertEquals(0, Speaker.speciesShift("cook"));
	}
}
