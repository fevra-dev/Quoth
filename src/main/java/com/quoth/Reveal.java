package com.quoth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits dialogue text into reveal steps and schedules them like speech. Game markup
 * ({@code <col=ff0000>}, {@code <br>}) is never split: tags and whitespace ride along with the
 * next visible unit, so every prefix is well-formed text the chatbox can render. Punctuation
 * holds the next step back, so a line breathes at commas and stops at full stops.
 */
final class Reveal
{
	// Extra delays after a step ending in each kind of punctuation.
	static final int PAUSE_COMMA = 1;
	static final int PAUSE_STOP = 3;
	static final int PAUSE_ELLIPSIS = 5;

	private final List<String> steps;
	/** When each step starts, in units of one delay. */
	private final int[] startUnits;

	private Reveal(List<String> steps)
	{
		this.steps = steps;
		startUnits = new int[steps.size()];
		String[] visible = new String[steps.size()];
		for (int k = 0; k < steps.size(); k++)
		{
			visible[k] = steps.get(k).replaceAll("<[^>]*>", "");
		}
		StringBuilder soFar = new StringBuilder();
		int units = 0;
		for (int k = 0; k < steps.size(); k++)
		{
			startUnits[k] = units;
			soFar.append(visible[k]);
			char next = k + 1 < steps.size() ? firstNonSpace(visible[k + 1]) : ' ';
			units += 1 + pauseAfter(soFar, next);
		}
	}

	static Reveal of(String text, RevealMode mode)
	{
		List<String> steps = new ArrayList<>();
		StringBuilder pending = new StringBuilder();
		int i = 0;
		while (i < text.length())
		{
			char c = text.charAt(i);
			if (c == '<')
			{
				int end = text.indexOf('>', i);
				if (end < 0)
				{
					end = text.length() - 1;
				}
				pending.append(text, i, end + 1);
				i = end + 1;
			}
			else if (Character.isWhitespace(c))
			{
				pending.append(c);
				i++;
			}
			else
			{
				int end = i;
				while (end < text.length() && text.charAt(end) != '<' && !Character.isWhitespace(text.charAt(end)))
				{
					end++;
				}
				if (mode != RevealMode.LETTER)
				{
					steps.add(pending + text.substring(i, end));
					pending.setLength(0);
				}
				else
				{
					for (int k = i; k < end; k++)
					{
						steps.add(pending.toString() + text.charAt(k));
						pending.setLength(0);
					}
				}
				i = end;
			}
		}
		if (pending.length() > 0)
		{
			if (steps.isEmpty())
			{
				steps.add(pending.toString());
			}
			else
			{
				int last = steps.size() - 1;
				steps.set(last, steps.get(last) + pending);
			}
		}
		return new Reveal(Collections.unmodifiableList(steps));
	}

	/**
	 * Extra delays after the text so far, given the next visible character. Runs of punctuation
	 * ("?!", "...") pause once, at their end, so letter and word modes breathe alike.
	 */
	static int pauseAfter(CharSequence soFar, char next)
	{
		String t = soFar.toString().trim();
		while (!t.isEmpty() && "\"')]".indexOf(t.charAt(t.length() - 1)) >= 0)
		{
			t = t.substring(0, t.length() - 1);
		}
		if (t.isEmpty() || ".!?,;:\u2026".indexOf(next) >= 0)
		{
			return 0;
		}
		if (t.endsWith("...") || t.endsWith("\u2026"))
		{
			return PAUSE_ELLIPSIS;
		}
		switch (t.charAt(t.length() - 1))
		{
			case '.':
			case '!':
			case '?':
				return PAUSE_STOP;
			case ',':
			case ';':
			case ':':
				return PAUSE_COMMA;
			default:
				return 0;
		}
	}

	private static char firstNonSpace(String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			if (!Character.isWhitespace(s.charAt(i)))
			{
				return s.charAt(i);
			}
		}
		return ' ';
	}

	static final int MOOD_QUESTION = 1;
	static final int MOOD_EXCLAIM = 2;

	/** How a step should sound: lifted if it ends a question, louder if it ends an exclamation. */
	int mood(int step)
	{
		if (step < 0 || step >= steps.size())
		{
			return 0;
		}
		String t = steps.get(step).replaceAll("<[^>]*>", "").trim();
		while (!t.isEmpty() && "\"')".indexOf(t.charAt(t.length() - 1)) >= 0)
		{
			t = t.substring(0, t.length() - 1);
		}
		int mood = 0;
		for (int i = t.length() - 1; i >= 0 && "?!".indexOf(t.charAt(i)) >= 0; i--)
		{
			mood |= t.charAt(i) == '?' ? MOOD_QUESTION : MOOD_EXCLAIM;
		}
		return mood;
	}

	int size()
	{
		return steps.size();
	}

	long startMs(int step, int delayMs)
	{
		return (long) startUnits[step] * delayMs;
	}

	/** How many steps have started by {@code elapsedMs}. */
	int startedBy(long elapsedMs, int delayMs)
	{
		int n = 0;
		while (n < steps.size() && startMs(n, delayMs) <= elapsedMs)
		{
			n++;
		}
		return n;
	}

	/** Text with the first {@code count} steps shown; {@code count >= size()} is the full text. */
	String prefix(int count)
	{
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < Math.min(count, steps.size()); k++)
		{
			sb.append(steps.get(k));
		}
		return sb.toString();
	}

	/**
	 * The started steps, each fading from colour {@code from} to {@code to} over {@code fadeMs}
	 * after it appears. Uses only the {@code <col>} tag the game's own dialogue already uses.
	 */
	String inked(long elapsedMs, int delayMs, int fadeMs, int from, int to)
	{
		StringBuilder sb = new StringBuilder();
		int started = startedBy(elapsedMs, delayMs);
		for (int k = 0; k < started; k++)
		{
			double a = (elapsedMs - startMs(k, delayMs)) / (double) Math.max(1, fadeMs);
			if (a >= 1)
			{
				sb.append(steps.get(k));
			}
			else
			{
				sb.append(String.format("<col=%06x>", blend(from, to, a) & 0xffffff))
					.append(steps.get(k))
					.append("</col>");
			}
		}
		return sb.toString();
	}

	/** When the last step is fully inked. */
	long inkedBy(int delayMs, int fadeMs)
	{
		return steps.isEmpty() ? 0 : startMs(steps.size() - 1, delayMs) + fadeMs;
	}

	/** Linear blend of two 0xRRGGBB colours; {@code a} = 0 is {@code from}, 1 is {@code to}. */
	static int blend(int from, int to, double a)
	{
		a = Math.max(0, Math.min(1, a));
		int r = (int) Math.round(((from >> 16) & 0xff) * (1 - a) + ((to >> 16) & 0xff) * a);
		int g = (int) Math.round(((from >> 8) & 0xff) * (1 - a) + ((to >> 8) & 0xff) * a);
		int b = (int) Math.round((from & 0xff) * (1 - a) + (to & 0xff) * a);
		return (r << 16) | (g << 8) | b;
	}
}
