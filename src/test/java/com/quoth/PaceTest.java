package com.quoth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaceTest
{
	@Test
	public void defaultPaceGivesTheValuesPickedByEar()
	{
		assertEquals(130, RevealMode.WORD.delay(130));
		assertEquals(130, RevealMode.FADE.delay(130));
		assertEquals(70, RevealMode.LETTER.delay(130));
		assertEquals(390, RevealMode.fadeLength(130));
	}

	@Test
	public void veryFastPaceNeverDropsBelowAFloor()
	{
		assertEquals(10, RevealMode.LETTER.delay(1));
		assertEquals(10, RevealMode.WORD.delay(0));
	}
}
