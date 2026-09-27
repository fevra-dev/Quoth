package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class RevealTest
{
	private static final String LINE = "Hello <col=ff0000>brave</col> adventurer!<br>Welcome.";

	@Test
	public void wordStepsEndOnTheFullText()
	{
		Reveal r = Reveal.of(LINE, RevealMode.WORD);
		assertEquals(4, r.size());
		assertEquals("Hello", r.prefix(1));
		assertEquals("Hello <col=ff0000>brave", r.prefix(2));
		assertEquals(LINE, r.prefix(r.size()));
	}

	@Test
	public void noPrefixEverSplitsATag()
	{
		for (RevealMode mode : RevealMode.values())
		{
			Reveal r = Reveal.of(LINE, mode);
			for (int n = 0; n <= r.size(); n++)
			{
				String p = r.prefix(n);
				assertFalse(mode + " step " + n + ": " + p, p.lastIndexOf('<') > p.lastIndexOf('>'));
			}
			assertEquals(LINE, r.prefix(r.size()));
		}
	}

	@Test
	public void letterModeRevealsOneCharacterPerStep()
	{
		Reveal r = Reveal.of("Hi there", RevealMode.LETTER);
		assertEquals(7, r.size());
		assertEquals("Hi t", r.prefix(3));
	}

	@Test
	public void trailingMarkupAndEmptyTextSurvive()
	{
		assertEquals("Bye.</col>", Reveal.of("Bye.</col>", RevealMode.WORD).prefix(1));
		assertEquals(0, Reveal.of("", RevealMode.WORD).size());
	}
}
