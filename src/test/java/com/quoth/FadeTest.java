package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FadeTest
{
	private static final String LINE = "Hello <col=ff0000>brave</col> adventurer!<br>Welcome.";
	private final Reveal r = Reveal.of(LINE, RevealMode.FADE);

	@Test
	public void finishesAsExactlyTheGameText()
	{
		assertEquals(LINE, r.inked(r.inkedBy(100, 300), 100, 300, 0xaaaaaa, 0x000000));
	}

	@Test
	public void newWordStartsFaintAndDarkens()
	{
		String first = r.inked(0, 100, 300, 0xc8b28a, 0x000000);
		assertTrue(first, first.startsWith("<col=c8b28a>Hello</col>"));
		String later = r.inked(150, 100, 300, 0xc8b28a, 0x000000);
		assertTrue(later, later.startsWith("<col=645945>Hello</col>")); // halfway
	}

	@Test
	public void unstartedWordsAreAbsent()
	{
		String first = r.inked(0, 100, 300, 0xaaaaaa, 0x000000);
		assertTrue(first, !first.contains("brave") && !first.contains("Welcome"));
	}

	@Test
	public void faintIsTowardParchmentForDarkTextAndBlackForLight()
	{
		assertEquals(Reveal.blend(0x000000, 0xc8b28a, 0.8), Speaker.faintOf(0x000000));
		assertEquals(Reveal.blend(0xffffff, 0x000000, 0.8), Speaker.faintOf(0xffffff));
	}
}
