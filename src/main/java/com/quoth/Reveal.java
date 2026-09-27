package com.quoth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits dialogue text into reveal steps. Game markup ({@code <col=ff0000>}, {@code <br>})
 * is never split: tags and whitespace ride along with the next visible unit, so every
 * prefix is well-formed text the chatbox can render.
 */
final class Reveal
{
	private final List<String> steps;

	private Reveal(List<String> steps)
	{
		this.steps = steps;
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

	int size()
	{
		return steps.size();
	}

	/**
	 * The full text with each step's opacity set by how long ago it started fading in. Steps that
	 * have not started are fully transparent, so the layout is the final layout from the first frame.
	 * Uses the game font's {@code <trans=N>} tag (0 opaque, 255 invisible).
	 */
	String fade(long elapsedMs, int delayMs, int fadeMs)
	{
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < steps.size(); k++)
		{
			double a = (elapsedMs - (double) k * delayMs) / Math.max(1, fadeMs);
			int trans = (int) Math.round(255 * (1 - Math.max(0, Math.min(1, a))));
			if (trans == 0)
			{
				sb.append(steps.get(k));
			}
			else
			{
				sb.append("<trans=").append(trans).append('>').append(steps.get(k)).append("</trans>");
			}
		}
		return sb.toString();
	}

	/** Milliseconds until the last step is fully opaque. */
	long fadeDuration(int delayMs, int fadeMs)
	{
		return steps.isEmpty() ? 0 : (long) (steps.size() - 1) * delayMs + fadeMs;
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
}
