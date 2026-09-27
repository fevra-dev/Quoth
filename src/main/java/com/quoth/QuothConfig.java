package com.quoth;

import net.runelite.api.SoundEffectID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(QuothConfig.GROUP)
public interface QuothConfig extends Config
{
	String GROUP = "quoth";

	@ConfigItem(
		keyName = "mode",
		name = "Reveal by",
		description = "Show dialogue one word or one letter at a time",
		position = 0
	)
	default RevealMode mode()
	{
		return RevealMode.WORD;
	}

	// New keys rather than the old shared "delay": a stored 90 would override any new default.
	@Range(min = 10, max = 600)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "wordDelay",
		name = "Word delay",
		description = "Time between words when revealing by word",
		position = 1
	)
	default int wordDelay()
	{
		return 130;
	}

	@Range(min = 10, max = 300)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "letterDelay",
		name = "Letter delay",
		description = "Time between letters when revealing by letter",
		position = 2
	)
	default int letterDelay()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "blip",
		name = "Voice blip",
		description = "Sound played as text appears. Random mixes the presets. Follows the game's sound effect volume",
		position = 3
	)
	default BlipSound blip()
	{
		return BlipSound.BOOP;
	}

	@ConfigItem(
		keyName = "soundId",
		name = "Custom sound ID",
		description = "Game sound effect ID. Follows the preset you pick; editing it switches Voice blip to Custom",
		position = 4
	)
	default int soundId()
	{
		return SoundEffectID.UI_BOOP;
	}

	@ConfigItem(
		keyName = "randomPool",
		name = "Random pool",
		description = "Sound IDs that Random picks from, separated by commas. Add any you like from Custom",
		position = 5
	)
	default String randomPool()
	{
		return BlipSound.DEFAULT_POOL;
	}
}
