package com.quoth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlipSoundTest
{
	@Test
	public void typedIdsMapBackToTheirGamePreset()
	{
		assertEquals(BlipSound.SOUND_2278, BlipSound.presetFor(2278));
		assertEquals(BlipSound.BOOP, BlipSound.presetFor(2266));
		assertNull(BlipSound.presetFor(2270));
		assertNull("Off, bundled voices and Custom share -1 and must never match", BlipSound.presetFor(-1));
	}

	@Test
	public void everyBundledVoiceShipsItsSample()
	{
		for (BlipSound b : BlipSound.values())
		{
			assertFalse(b + " is both bundled and a game preset", b.isBundled() && b.isGamePreset());
			if (b.isBundled())
			{
				assertTrue(b + " has no wav", BlipSound.class.getResource("blips/" + b.getSample() + ".wav") != null);
			}
		}
	}

	@Test
	public void highVariantCarriesAnOctaveButTypingPicksThePlainOne()
	{
		assertEquals(12, BlipSound.SOUND_2269_HIGH.getPitch());
		assertEquals(2269, BlipSound.SOUND_2269_HIGH.getId());
		assertEquals(BlipSound.SOUND_2269, BlipSound.presetFor(2269));
	}
}
