package com.quoth;

import com.google.inject.Provides;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
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

	// The game's own text widget is never edited: other plugins (overhead text, dialogue
	// loggers) read it every tick and would see each partial line as a new one. Instead the
	// original is hidden with its full text intact and the reveal is drawn in a copy on top.
	private Widget source;
	private Widget copy;
	private String fullText;
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
		clientThread.invoke(this::release);
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		Widget widget = null;
		for (int id : DIALOGUE_TEXT)
		{
			Widget w = client.getWidget(id);
			// The one we hid is still the live one; any other must be visible to count.
			if (w != null && (w == source || !w.isHidden()))
			{
				widget = w;
				break;
			}
		}

		if (widget == null)
		{
			// Dialogue closed. Forget it, so an identical next line still animates.
			release();
			return;
		}

		String current = widget.getText();
		if (current == null)
		{
			return;
		}

		// ponytail: a new line is detected by its text changing. Two identical lines in a row
		// on the same widget without the box closing will not re-animate; hook WidgetLoaded if that shows up.
		if (widget != source || !current.equals(fullText))
		{
			release();
			start(widget, current);
		}

		advance();
	}

	private void start(Widget widget, String text)
	{
		source = widget;
		fullText = text;
		mode = config.mode();
		reveal = Reveal.of(text, mode);
		startNanos = System.nanoTime();
		shown = 0;
		if (reveal.size() == 0)
		{
			return;
		}

		copy = widget.getParent().createChild(-1, WidgetType.TEXT);
		copy.setFontId(widget.getFontId());
		copy.setTextColor(widget.getTextColor());
		copy.setTextShadowed(widget.getTextShadowed());
		copy.setXTextAlignment(widget.getXTextAlignment());
		copy.setYTextAlignment(widget.getYTextAlignment());
		copy.setLineHeight(widget.getLineHeight());
		copy.setXPositionMode(widget.getXPositionMode());
		copy.setYPositionMode(widget.getYPositionMode());
		copy.setWidthMode(widget.getWidthMode());
		copy.setHeightMode(widget.getHeightMode());
		copy.setOriginalX(widget.getOriginalX());
		copy.setOriginalY(widget.getOriginalY());
		copy.setOriginalWidth(widget.getOriginalWidth());
		copy.setOriginalHeight(widget.getOriginalHeight());
		copy.setText("");
		copy.revalidate();
		widget.setHidden(true);
	}

	private void advance()
	{
		if (copy == null || shown >= reveal.size())
		{
			return;
		}
		if (!source.isHidden())
		{
			// The game re-showed its widget mid-line; keep it out of the way.
			source.setHidden(true);
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
		if (shown >= reveal.size())
		{
			// Done: hand the box back to the game's own widget, untouched.
			source.setHidden(false);
			copy.setHidden(true);
		}
		else
		{
			copy.setText(reveal.prefix(shown));
		}
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

	/** Restores the game's widget and drops the copy. Safe to call at any time. */
	private void release()
	{
		if (copy != null)
		{
			copy.setHidden(true);
			if (source != null)
			{
				source.setHidden(false);
			}
		}
		source = null;
		copy = null;
		fullText = null;
	}
}
