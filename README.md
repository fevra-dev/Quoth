# Quoth

Dialogue that speaks, like the old RPGs.

![Talking to Hans in Lumbridge with Quoth](docs/demo.gif)

NPC dialogue types itself out in the chatbox one word at a time, with a soft voice blip for each
word. Lines breathe at commas and stop at full stops. Every NPC has a voice of their own: the Cook
always sounds like the Cook, goblins and dwarves sound gruff, gnomes and imps sound small.

Everything happens inside the game's own chatbox. There is no new panel to place or resize.

## How it sounds

- **A voice per speaker.** Each NPC's pitch is fixed by their name, so characters sound different
  from each other and the same every time you meet them. Big folk (goblins, dwarves, trolls, ogres,
  giants, demons, dragons) sit lower; small folk (gnomes, imps, fairies, children) sit higher. Your
  own lines stay at the base pitch.
- **Speech rhythm.** A short pause after a comma, a longer one after a full stop, and the longest
  after "...". *"Erm... Sorry, I don't have any of that with me..."* lands like speech.
- **Mood.** A question lifts the last blip, and an exclamation makes it louder.
- **Never skip a line half-read.** While a line is still appearing, the first click on *Click here
  to continue* (or Space) shows the rest of it. The next click continues as normal.

## Settings

### Text

| Setting | Default | What it does |
|---|---|---|
| Reveal by | Word | *Word* or *Letter* at a time, or *Fade*, where each word fades in from faint |
| Pace | 130 ms | Time per word. Letters and fades follow it: at 130 ms a letter takes 70 ms and a fade 390 ms |
| Click to finish | On | The first continue while a line is appearing shows the rest instead of moving on |

### Voice

| Setting | Default | What it does |
|---|---|---|
| Voice | Soft | *Soft*, *Softer*, *Warm*, *Reed*, *Bell*, *Pip* or *Wood*, or one of the game's own sounds |
| Pitch | 0 | Semitones up or down |
| Voice per speaker | On | Each NPC gets their own pitch, with big and small folk set apart |
| Volume | 60% | How loud the voice is |

Quoth's own voices (Soft to Wood) take every setting above. The *Game* voices play the game's own
sound effects, which RuneLite can only play as they are: they follow your in-game sound effects
volume, and Pitch, speaker voices and mood do not reach them.

### Explore

| Setting | What it does |
|---|---|
| Custom sound ID | Any of the game's sound effect IDs, used when Voice is *Game (Custom ID)* |
| Roll random sound | A key that plays a random game sound, makes it your Custom sound ID and prints its number in chat, for hunting new voices |

## Works with

- **NPC Overhead Dialogue** and other plugins that read the chatbox always see the complete line.
  Quoth never edits the game's own text; it draws the typing on top and hands the box back when the
  line is done.
- **Immersive Dialogue** draws dialogue in its own panel, so Quoth steps back from the text and
  lends only its voice. Turn Immersive's *Voice blips* off (or its volume to 0) so one voice speaks,
  not two.

## Notes

Quoth only changes how dialogue looks and sounds on your screen. It sends nothing to the game,
makes no network calls and automates nothing. *Click to finish* can only ever add a click; it never
removes one.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
