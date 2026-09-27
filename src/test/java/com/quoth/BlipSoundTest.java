package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class BlipSoundTest
{
	@Test
	public void typedIdsMapBackToTheirPreset()
	{
		assertEquals(BlipSound.SOUND_2278, BlipSound.presetFor(2278));
		assertEquals(BlipSound.BOOP, BlipSound.presetFor(2266));
		assertNull(BlipSound.presetFor(2270));
		assertNull("Off/Random/Custom share -1 and must never match", BlipSound.presetFor(-1));
	}

	@Test
	public void poolParsesLooselyAndSkipsJunk()
	{
		assertArrayEquals(new int[]{2266, 2269, 2276, 2278}, BlipSound.parsePool(BlipSound.DEFAULT_POOL));
		assertArrayEquals(new int[]{100, 3000}, BlipSound.parsePool(" 100,,abc, -5 ,3000 "));
	}

	@Test
	public void emptyPoolFallsBackToPresetsSoRandomIsNeverSilent()
	{
		int[] expected = {2266, 2269, 2276, 2278};
		assertArrayEquals(expected, BlipSound.parsePool(""));
		assertArrayEquals(expected, BlipSound.parsePool("nothing, here"));
		assertArrayEquals(expected, BlipSound.parsePool(null));
	}

	@Test
	public void pickStaysInsideThePool()
	{
		int[] pool = BlipSound.parsePool("7, 9");
		Random r = new Random(1);
		for (int i = 0; i < 200; i++)
		{
			int id = BlipSound.pick(pool, r);
			assertTrue(id == 7 || id == 9);
		}
	}
}
