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

	@Range(min = 10, max = 400)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "delay",
		name = "Delay",
		description = "Time between each word (or letter)",
		position = 1
	)
	default int delay()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "blip",
		name = "Voice blip",
		description = "Sound played as text appears. Follows the game's sound effect volume",
		position = 2
	)
	default BlipSound blip()
	{
		return BlipSound.BOOP;
	}

	@ConfigItem(
		keyName = "soundId",
		name = "Custom sound ID",
		description = "Game sound effect ID used when Voice blip is set to Custom",
		position = 3
	)
	default int soundId()
	{
		return SoundEffectID.UI_BOOP;
	}
}
