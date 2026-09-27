package com.quoth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RhythmTest
{
	@Test
	public void wordsPauseAtCommasStopsAndEllipses()
	{
		// Erm... | Sorry, | I | don't.
		Reveal r = Reveal.of("Erm... Sorry, I don't.", RevealMode.WORD);
		assertEquals(0, r.startMs(0, 100));
		assertEquals((1 + Reveal.PAUSE_ELLIPSIS) * 100, r.startMs(1, 100));
		assertEquals(r.startMs(1, 100) + (1 + Reveal.PAUSE_COMMA) * 100, r.startMs(2, 100));
		assertEquals(r.startMs(2, 100) + 100, r.startMs(3, 100));
	}

	@Test
	public void lettersPauseOnceAtTheEndOfAnEllipsis()
	{
		Reveal r = Reveal.of("Hm... ok", RevealMode.LETTER); // H m . . . o k
		assertEquals(100, r.startMs(1, 100)); // m
		assertEquals(200, r.startMs(2, 100)); // first dot, no pause after m
		assertEquals(300, r.startMs(3, 100)); // dots run on
		assertEquals(400, r.startMs(4, 100));
		assertEquals(400 + (1 + Reveal.PAUSE_ELLIPSIS) * 100, r.startMs(5, 100)); // o waits
	}

	@Test
	public void punctuationRunsAndClosingQuotesPauseOnce()
	{
		assertEquals(0, Reveal.pauseAfter("What?", '!'));
		assertEquals(Reveal.PAUSE_STOP, Reveal.pauseAfter("What?!", ' '));
		assertEquals(Reveal.PAUSE_STOP, Reveal.pauseAfter("\"Go.\"", 'H'));
		assertEquals(0, Reveal.pauseAfter("plain", 'x'));
	}

	@Test
	public void startedByFollowsTheSchedule()
	{
		Reveal r = Reveal.of("One. Two", RevealMode.WORD);
		assertEquals(1, r.startedBy(0, 100));
		assertEquals(1, r.startedBy(399, 100));
		assertEquals(2, r.startedBy(400, 100));
	}

	@Test
	public void speakersGetStableDistinctPitchesInRange()
	{
		assertEquals(Speaker.pitchFor("Cook"), Speaker.pitchFor("<col=ff0000>cook</col>"));
		assertEquals(0, Speaker.pitchFor(null));
		assertEquals(0, Speaker.pitchFor(""));
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (String n : new String[]{"Cook", "Hans", "Duke Horacio", "Bob", "Wise Old Man", "Gertrude", "Zaff"})
		{
			int p = Speaker.pitchFor(n);
			assertEquals(true, p >= -Speaker.SPREAD && p <= Speaker.SPREAD);
			seen.add(p);
		}
		assertEquals(true, seen.size() > 2);
	}
}
