package com.quoth;

import com.google.inject.Provides;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	@Inject
	private ConfigManager configManager;

	private final Random random = new Random();

	private int trackedId = -1;
	private String fullText;
	private String lastSet;
	private Reveal reveal;
	private RevealMode mode;
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
		mode = config.mode();
		reveal = Reveal.of(text, mode);
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
		int target = (int) Math.min(reveal.size(), 1 + elapsedMs / Math.max(1, delay()));
		if (target <= shown)
		{
			return;
		}

		int soundId = soundId();
		boolean blip = soundId >= 0 && (mode == RevealMode.WORD
			|| shown / LETTERS_PER_BLIP != target / LETTERS_PER_BLIP
			|| shown == 0);
		shown = target;
		set(widget, shown >= reveal.size() ? fullText : reveal.prefix(shown));
		if (blip)
		{
			client.playSoundEffect(soundId);
		}
	}

	private int delay()
	{
		return mode == RevealMode.WORD ? config.wordDelay() : config.letterDelay();
	}

	private int soundId()
	{
		switch (config.blip())
		{
			case CUSTOM:
				return config.soundId();
			case RANDOM:
				return BlipSound.pick(BlipSound.parsePool(config.randomPool()), random);
			default:
				return config.blip().getId();
		}
	}

	/**
	 * Keeps the dropdown and the ID field telling the same story: picking a preset writes its
	 * ID into the field, and typing an ID selects the matching preset, or Custom if none.
	 * Each write is skipped when the other side already agrees, so neither echo loops.
	 * ponytail: RuneLite's settings panel only redraws when reopened, so the field shows the
	 * synced value after closing and reopening Quoth's settings.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!QuothConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		BlipSound blip = config.blip();
		int id = config.soundId();
		if ("blip".equals(event.getKey()))
		{
			if (blip.isPreset() && id != blip.getId())
			{
				configManager.setConfiguration(QuothConfig.GROUP, "soundId", blip.getId());
			}
		}
		else if ("soundId".equals(event.getKey()))
		{
			if (blip == BlipSound.CUSTOM || (blip.isPreset() && id == blip.getId()))
			{
				return;
			}
			BlipSound match = BlipSound.presetFor(id);
			configManager.setConfiguration(QuothConfig.GROUP, "blip", match != null ? match : BlipSound.CUSTOM);
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
