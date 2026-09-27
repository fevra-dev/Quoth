package com.quoth;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

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

	// ponytail: fade length is fixed at this many word delays so words overlap as they bloom;
	// give it its own setting if one ratio does not suit every speed.
	private static final int FADE_WORDS = 3;

	// Fixed character for Quoth's own audio, so the settings stay few: a little random pitch
	// keeps a voice alive, and a cap keeps every sample a short stab.
	private static final int VARIATION_CENTS = 30;
	private static final int STAB_MS = 150;
	private static final int QUESTION_LIFT = 3;
	private static final double EXCLAIM_GAIN = 1.5;

	private static final int[] DIALOGUE_CONTINUE = {
		InterfaceID.ChatLeft.CONTINUE,
		InterfaceID.ChatRight.CONTINUE,
		InterfaceID.Messagebox.CONTINUE,
	};

	private static final String IMMERSIVE_DIALOGUE = "ImmersiveDialoguePlugin";
	private static final String IMMERSIVE_GROUP = "immersivedialogue";
	private static final int IMMERSIVE_DEFAULT_SPEED = 35;

	// Size of the game's synth list: symbols/synth.sym in Joshua-F/osrs-dumps, 2026-09-27.
	private static final int SYNTH_COUNT = 12155;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private QuothConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private final Random random = new Random();
	private final BlipPlayer player = new BlipPlayer(new File(RuneLite.RUNELITE_DIR, "quoth"));
	private boolean immersiveActive;
	private String voicedLine;
	private long voicedStart;
	private int voicedChars;
	private Reveal voicedReveal;
	private int voicedShown;
	private int speakerPitch;
	private int inkFrom;
	private int inkTo;

	private final HotkeyListener rollListener = new HotkeyListener(() -> config.rollKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(QuothPlugin.this::roll);
		}
	};

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
	private boolean done;
	/** Read from the key thread, so volatile: true while a line is still appearing. */
	private volatile boolean revealing;

	private final KeyListener spaceToFinish = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_SPACE && revealing && config.clickToFinish())
			{
				e.consume();
				clientThread.invoke(QuothPlugin.this::finishLine);
			}
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	};

	@Provides
	QuothConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(QuothConfig.class);
	}

	@Override
	protected void startUp()
	{
		player.start();
		keyManager.registerKeyListener(rollListener);
		keyManager.registerKeyListener(spaceToFinish);
		immersiveActive = isImmersiveActive();
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(rollListener);
		keyManager.unregisterKeyListener(spaceToFinish);
		player.stop();
		clientThread.invoke(this::release);
	}

	/**
	 * Immersive Dialogue rebuilds the chatbox every frame and has its own typing and voice
	 * blips, so running both would fight over the same widgets and double every sound.
	 */
	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		immersiveActive = isImmersiveActive();
	}

	private boolean isImmersiveActive()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (IMMERSIVE_DIALOGUE.equals(p.getClass().getSimpleName()) && pluginManager.isPluginActive(p))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * With Immersive Dialogue drawing the text, Quoth only lends its voice: it runs blips on
	 * Immersive's own clock (Text speed, characters per second) and never touches its panel.
	 * Immersive only types while its Voice blips setting is on; set its Voice volume to 0 so
	 * one voice speaks, not two.
	 * ponytail: clicking to skip a line in Immersive is invisible here, so blips run to the
	 * end of that line; mirror its skip if that ever grates.
	 */
	private void voiceImmersive()
	{
		boolean typing = Boolean.TRUE.equals(configManager.getConfiguration(IMMERSIVE_GROUP, "voiceBlips", Boolean.class));
		String body = null;
		int speakerId = -1;
		for (int id : DIALOGUE_TEXT)
		{
			Widget w = client.getWidget(id);
			// Immersive hides the native widgets but leaves their text intact.
			if (w != null && w.getText() != null && !w.getText().isEmpty())
			{
				body = Text.removeTags(w.getText().replace("<br>", " "));
				speakerId = id;
				break;
			}
		}
		if (body == null)
		{
			voicedLine = null;
			return;
		}
		if (!body.equals(voicedLine))
		{
			voicedLine = body;
			voicedStart = System.nanoTime();
			voicedChars = 0;
			voicedShown = 0;
			voicedReveal = Reveal.of(body, config.mode());
			speakerPitch = speakerPitchFor(speakerId);
		}
		if (!typing)
		{
			// Immersive shows the line whole; Quoth still speaks it, on its own rhythm.
			long elapsedMs = (System.nanoTime() - voicedStart) / 1_000_000L;
			int target = voicedReveal.startedBy(elapsedMs, Math.max(1, voicedDelay()));
			int mood = target > voicedShown ? voicedReveal.mood(target - 1) : 0;
			boolean blip = target > voicedShown && (config.mode() != RevealMode.LETTER
				|| voicedShown / LETTERS_PER_BLIP != target / LETTERS_PER_BLIP || voicedShown == 0 || mood != 0);
			voicedShown = Math.max(voicedShown, target);
			if (blip && config.voice() != BlipSound.OFF)
			{
				playBlip(config.voice(), mood);
			}
			return;
		}

		Integer speed = configManager.getConfiguration(IMMERSIVE_GROUP, "textSpeed", Integer.class);
		double cps = speed != null && speed > 0 ? speed : IMMERSIVE_DEFAULT_SPEED;
		long elapsedMs = (System.nanoTime() - voicedStart) / 1_000_000L;
		int target = (int) Math.min(body.length(), elapsedMs * cps / 1000.0);
		boolean blip = false;
		for (int i = voicedChars; i < target; i++)
		{
			char c = body.charAt(i);
			if (Character.isWhitespace(c))
			{
				continue;
			}
			boolean wordStart = i == 0 || Character.isWhitespace(body.charAt(i - 1));
			blip |= config.mode() != RevealMode.LETTER ? wordStart : nonSpaceIndex(body, i) % LETTERS_PER_BLIP == 0;
		}
		voicedChars = target;
		BlipSound voice = config.voice();
		if (blip && voice != BlipSound.OFF)
		{
			playBlip(voice);
		}
	}

	private int voicedDelay()
	{
		return config.mode() == RevealMode.LETTER ? config.letterDelay() : config.wordDelay();
	}

	/** An NPC speaking gets a pitch from their name; the player and message boxes stay central. */
	private int speakerPitchFor(int textWidgetId)
	{
		if (textWidgetId != InterfaceID.ChatLeft.TEXT)
		{
			return 0;
		}
		Widget name = client.getWidget(InterfaceID.ChatLeft.NAME);
		return Speaker.pitchFor(name == null ? null : name.getText());
	}

	private static int nonSpaceIndex(String s, int i)
	{
		int n = 0;
		for (int k = 0; k < i; k++)
		{
			if (!Character.isWhitespace(s.charAt(k)))
			{
				n++;
			}
		}
		return n;
	}

	/** Tells the player where the folder is and what loaded, since the settings panel cannot. */
	private void reportSamples()
	{
		int count = player.refreshUser();
		String where = player.userDir().getAbsolutePath();
		String msg = count == 0
			? "Quoth: no samples yet. Put WAV or AIFF files in " + where
			: "Quoth: " + count + " sample" + (count == 1 ? "" : "s") + " in " + where;
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(msg)
			.build());
	}

	private void roll()
	{
		int id = random.nextInt(SYNTH_COUNT);
		// The sync handler moves Voice to the matching preset or Custom.
		configManager.setConfiguration(QuothConfig.GROUP, "soundId", id);
		client.playSoundEffect(id);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("Quoth: sound " + id + " (now your Custom sound ID)")
			.build());
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (immersiveActive)
		{
			release();
			voiceImmersive();
			return;
		}

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
		done = false;
		speakerPitch = speakerPitchFor(widget.getId());
		inkTo = widget.getTextColor() & 0xffffff;
		inkFrom = Speaker.faintOf(inkTo);
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
		if (copy == null || done)
		{
			return;
		}
		if (!source.isHidden())
		{
			// The game re-showed its widget mid-line; keep it out of the way.
			source.setHidden(true);
		}

		long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
		int delay = Math.max(1, delay());
		int fadeMs = delay * FADE_WORDS;
		int target = reveal.startedBy(elapsedMs, delay);
		boolean stepped = target > shown;

		BlipSound voice = config.voice();
		int mood = stepped ? reveal.mood(target - 1) : 0;
		boolean blip = stepped && voice != BlipSound.OFF && (mode != RevealMode.LETTER
			|| shown / LETTERS_PER_BLIP != target / LETTERS_PER_BLIP
			|| shown == 0 || mood != 0);
		shown = Math.max(shown, target);

		boolean finished = mode == RevealMode.FADE
			? elapsedMs >= reveal.inkedBy(delay, fadeMs)
			: shown >= reveal.size();
		if (finished)
		{
			finishLine();
		}
		else if (mode == RevealMode.FADE)
		{
			copy.setText(reveal.inked(elapsedMs, delay, fadeMs, inkFrom, inkTo));
		}
		else if (stepped)
		{
			copy.setText(reveal.prefix(shown));
		}
		if (blip)
		{
			playBlip(voice, mood);
		}
		revealing = !done;
	}

	/** Shows the rest of the line at once and hands the box back to the game's own widget. */
	private void finishLine()
	{
		if (copy == null || done)
		{
			return;
		}
		done = true;
		revealing = false;
		shown = reveal.size();
		source.setHidden(false);
		copy.setHidden(true);
	}

	/**
	 * While a line is appearing, the first click on continue finishes it instead of moving on,
	 * so no line is skipped half-read. The click after that continues as normal.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!revealing || !config.clickToFinish())
		{
			return;
		}
		for (int id : DIALOGUE_CONTINUE)
		{
			if (event.getParam1() == id)
			{
				event.consume();
				finishLine();
				return;
			}
		}
	}

	private void playBlip(BlipSound voice)
	{
		playBlip(voice, 0);
	}

	private void playBlip(BlipSound voice, int mood)
	{
		if (voice.isOwnAudio())
		{
			int pitch = config.pitch() + (config.speakerPitch() ? speakerPitch : 0);
			int volume = config.volume();
			if ((mood & Reveal.MOOD_QUESTION) != 0)
			{
				pitch += QUESTION_LIFT;
			}
			if ((mood & Reveal.MOOD_EXCLAIM) != 0)
			{
				volume = (int) Math.min(100, Math.round(volume * EXCLAIM_GAIN));
			}
			player.play(voice, config.sampleFile(), pitch, VARIATION_CENTS, volume, STAB_MS);
		}
		else
		{
			client.playSoundEffect(voice == BlipSound.CUSTOM ? config.soundId() : voice.getId());
		}
	}

	private int delay()
	{
		return mode == RevealMode.LETTER ? config.letterDelay() : config.wordDelay();
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

		BlipSound voice = config.voice();
		int id = config.soundId();
		if (("voice".equals(event.getKey()) && voice == BlipSound.USER) || "sampleFile".equals(event.getKey()))
		{
			reportSamples();
		}
		if ("voice".equals(event.getKey()))
		{
			if (voice.isGamePreset() && id != voice.getId())
			{
				configManager.setConfiguration(QuothConfig.GROUP, "soundId", voice.getId());
			}
		}
		else if ("soundId".equals(event.getKey()))
		{
			if (voice == BlipSound.CUSTOM || (voice.isGamePreset() && id == voice.getId()))
			{
				return;
			}
			BlipSound match = BlipSound.presetFor(id);
			configManager.setConfiguration(QuothConfig.GROUP, "voice", match != null ? match : BlipSound.CUSTOM);
		}
	}

	/** Restores the game's widget and drops the copy. Safe to call at any time. */
	private void release()
	{
		revealing = false;
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
