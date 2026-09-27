package com.quoth;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(QuothConfig.GROUP)
public interface QuothConfig extends Config
{
	String GROUP = "quoth";

	@ConfigSection(name = "Text", description = "How dialogue appears", position = 0)
	String textSection = "text";

	@ConfigSection(name = "Voice", description = "The sound of each word", position = 1)
	String voiceSection = "voice";

	@ConfigSection(name = "Explore", description = "Audition the game's own sounds", position = 2)
	String exploreSection = "explore";

	@ConfigItem(
		keyName = "mode",
		name = "Reveal by",
		description = "Word or letter at a time, or Fade: each word fades in over the ones before it",
		section = textSection,
		position = 0
	)
	default RevealMode mode()
	{
		return RevealMode.WORD;
	}

	@ConfigItem(
		keyName = "clickToFinish",
		name = "Click to finish",
		description = "While a line is appearing, the first click on continue (or Space) shows the rest; the next continues",
		section = textSection,
		position = 3
	)
	default boolean clickToFinish()
	{
		return true;
	}

	// Own keys per mode: a value stored under an older shared key would override new defaults.
	@Range(min = 10, max = 600)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "wordDelay",
		name = "Word delay",
		description = "Time between words when revealing by word",
		section = textSection,
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
		section = textSection,
		position = 2
	)
	default int letterDelay()
	{
		return 70;
	}

	// New key: "blip" stored values like RANDOM no longer exist in this list.
	@ConfigItem(
		keyName = "voice",
		name = "Voice",
		description = "Soft, Warm, Reed and Your samples are played by Quoth and can be pitched. Game sounds follow the in-game effects volume",
		section = voiceSection,
		position = 0
	)
	default BlipSound voice()
	{
		return BlipSound.BOOP;
	}

	@ConfigItem(
		keyName = "sampleFile",
		name = "Sample file",
		description = "For Your samples: a file name in .runelite/quoth (WAV or AIFF). Leave blank to pick a random one each word",
		section = voiceSection,
		position = 1
	)
	default String sampleFile()
	{
		return "";
	}

	@Range(min = -12, max = 12)
	@ConfigItem(
		keyName = "pitch",
		name = "Pitch",
		description = "Semitones up or down. Not for game sounds",
		section = voiceSection,
		position = 2
	)
	default int pitch()
	{
		return 0;
	}



	@ConfigItem(
		keyName = "speakerPitch",
		name = "Voice per speaker",
		description = "Each NPC gets a pitch of their own from their name; goblins, dwarves and other big folk sound lower, gnomes and imps higher. Not for game sounds",
		section = voiceSection,
		position = 3
	)
	default boolean speakerPitch()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Not for game sounds; they use the in-game effects volume",
		section = voiceSection,
		position = 4
	)
	default int volume()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "soundId",
		name = "Custom sound ID",
		description = "Game sound effect ID. Follows the game preset you pick; editing it switches Voice to that preset or Custom",
		section = exploreSection,
		position = 0
	)
	default int soundId()
	{
		return 2266;
	}

	@ConfigItem(
		keyName = "rollKey",
		name = "Roll random sound",
		description = "Press to hear a random game sound. It becomes your Custom sound ID and its number is shown in chat",
		section = exploreSection,
		position = 1
	)
	default Keybind rollKey()
	{
		return Keybind.NOT_SET;
	}
}
