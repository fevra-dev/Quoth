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

	@Range(min = 40, max = 600)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "pace",
		name = "Pace",
		description = "Time per word. Letters and fades follow it: at 130 ms a letter takes 70 ms and a fade 390 ms",
		section = textSection,
		position = 1
	)
	default int pace()
	{
		return 130;
	}

	@ConfigItem(
		keyName = "clickToFinish",
		name = "Click to finish",
		description = "While a line is appearing, the first click on continue (or Space) shows the rest; the next continues",
		section = textSection,
		position = 2
	)
	default boolean clickToFinish()
	{
		return true;
	}

	// New key: "blip" stored values like RANDOM no longer exist in this list.
	@ConfigItem(
		keyName = "voice",
		name = "Voice",
		description = "Quoth's own voices (Soft to Wood) take every setting. Game voices play the game's own sound, so Pitch, Volume, speaker voices and mood only reach them if you have a matching file in .runelite/quoth",
		section = voiceSection,
		position = 0
	)
	default BlipSound voice()
	{
		// Bundled, so per-speaker pitch and punctuation mood work on every machine.
		return BlipSound.SOFT;
	}

	@Range(min = -12, max = 12)
	@ConfigItem(
		keyName = "pitch",
		name = "Pitch",
		description = "Semitones up or down, for Quoth's own voices",
		section = voiceSection,
		position = 1
	)
	default int pitch()
	{
		return 0;
	}



	@ConfigItem(
		keyName = "speakerPitch",
		name = "Voice per speaker",
		description = "Each NPC gets a pitch of their own from their name; ogres, dwarves and barbarians sound lower, gnomes and fairies higher. Quoth's own voices",
		section = voiceSection,
		position = 2
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
		description = "For Quoth's own voices; game voices follow the in-game sound effects volume",
		section = voiceSection,
		position = 3
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
