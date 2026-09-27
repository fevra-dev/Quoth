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
				if (mode == RevealMode.WORD)
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
