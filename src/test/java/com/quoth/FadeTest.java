package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FadeTest
{
	private static final String LINE = "Hello <col=ff0000>brave</col> adventurer!<br>Welcome.";
	private final Reveal r = Reveal.of(LINE, RevealMode.FADE);

	private static String stripTrans(String s)
	{
		return s.replaceAll("</?trans(=\\d+)?>", "");
	}

	@Test
	public void everyFrameCarriesTheWholeLineSoLayoutNeverMoves()
	{
		for (long t = 0; t <= r.fadeDuration(100, 300); t += 37)
		{
			assertEquals("t=" + t, LINE, stripTrans(r.fade(t, 100, 300)));
		}
	}

	@Test
	public void startsInvisibleAndEndsAsPlainText()
	{
		assertTrue(r.fade(-1, 100, 300).startsWith("<trans=255>"));
		assertEquals(LINE, r.fade(r.fadeDuration(100, 300), 100, 300));
	}

	@Test
	public void midFadeWordIsPartlyTransparentAndLaterWordsHidden()
	{
		String mid = r.fade(150, 100, 300); // word 0 at 50%, word 1 at ~17%, words 2-3 not started
		assertTrue(mid, mid.startsWith("<trans=128>Hello</trans>"));
		assertTrue(mid, mid.contains("<trans=255><br>Welcome.</trans>"));
	}

	@Test
	public void transTagsAreBalanced()
	{
		String mid = r.fade(150, 100, 300);
		int open = mid.split("<trans=", -1).length - 1;
		int close = mid.split("</trans>", -1).length - 1;
		assertEquals(open, close);
		assertFalse(mid.contains("<trans=0>"));
	}

	@Test
	public void durationCoversTheLastWordsFade()
	{
		assertEquals(3 * 100 + 300, r.fadeDuration(100, 300));
	}
}
