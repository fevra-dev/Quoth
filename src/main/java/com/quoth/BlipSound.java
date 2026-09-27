package com.quoth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlipSound
{
	OFF("Off", -1),
	BOOP("Boop (2266)", 2266),
	SOUND_2269("2269", 2269),
	SOUND_2276("2276", 2276),
	SOUND_2278("2278", 2278),
	CUSTOM("Custom", -1);

	private final String label;
	private final int id;

	@Override
	public String toString()
	{
		return label;
	}
}
