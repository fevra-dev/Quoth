package com.quoth;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Quoth",
	description = "Dialogue types itself out in the chatbox, word by word, with an optional voice blip",
	tags = {"dialogue", "typewriter", "chatbox", "rpg", "text", "immersion", "sound"}
)
public class QuothPlugin extends Plugin
{
	// NPC speaking, player speaking, plain message box.
	private static final int[] DIALOGUE_TEXT = {
		InterfaceID.ChatLeft.TEXT,
		InterfaceID.ChatRight.TEXT,
		InterfaceID.Messagebox.TEXT,
	};

	// A blip per letter sounds like a drill; one per this many letters reads as speech.
	private static final int LETTERS_PER_BLIP = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private QuothConfig config;

	private int trackedId = -1;
	private String fullText;
	private String lastSet;
	private Reveal reveal;
	private long startNanos;
	private int shown;

	@Provides
	QuothConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(QuothConfig.class);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::finishNow);
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		Widget widget = null;
		for (int id : DIALOGUE_TEXT)
		{
			Widget w = client.getWidget(id);
			if (w != null && !w.isHidden())
			{
				widget = w;
				break;
			}
		}

		if (widget == null)
		{
			// Dialogue closed. Forget it, so an identical next line still animates.
			trackedId = -1;
			fullText = null;
			lastSet = null;
			return;
		}

		String current = widget.getText();
		if (current == null)
		{
			return;
		}

		boolean ours = widget.getId() == trackedId && current.equals(lastSet);
		if (!ours)
		{
			// ponytail: a new line is detected by its text changing. Two identical lines in a row
			// without the box closing between them will not re-animate; hook WidgetLoaded if that shows up.
			if (widget.getId() == trackedId && current.equals(fullText))
			{
				return;
			}
			start(widget, current);
		}

		advance(widget);
	}

	private void start(Widget widget, String text)
	{
		trackedId = widget.getId();
		fullText = text;
		reveal = Reveal.of(text, config.mode());
		startNanos = System.nanoTime();
		shown = 0;
		set(widget, "");
	}

	private void advance(Widget widget)
	{
		if (shown >= reveal.size())
		{
			return;
		}

		long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
		int target = (int) Math.min(reveal.size(), 1 + elapsedMs / Math.max(1, config.delay()));
		if (target <= shown)
		{
			return;
		}

		boolean blip = config.sound() && (config.mode() == RevealMode.WORD
			|| shown / LETTERS_PER_BLIP != target / LETTERS_PER_BLIP
			|| shown == 0);
		shown = target;
		set(widget, shown >= reveal.size() ? fullText : reveal.prefix(shown));
		if (blip)
		{
			client.playSoundEffect(config.soundId());
		}
	}

	private void finishNow()
	{
		if (trackedId == -1 || fullText == null)
		{
			return;
		}
		Widget w = client.getWidget(trackedId);
		if (w != null)
		{
			w.setText(fullText);
		}
		trackedId = -1;
	}

	private void set(Widget widget, String text)
	{
		lastSet = text;
		widget.setText(text);
	}
}
