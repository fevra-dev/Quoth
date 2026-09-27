package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	public void randomOnlyPicksPresets()
	{
		Random r = new Random(1);
		for (int i = 0; i < 200; i++)
		{
			assertNotNull(BlipSound.presetFor(BlipSound.randomPresetId(r)));
		}
	}
}
