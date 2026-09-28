"""Draws a batch of Quoth icon concepts (48x72, RuneLite's plugin icon size) and a contact sheet.

Run: uv run --with pillow python3 tools/gen_icons.py   -> docs/icons/*.png, docs/icons/sheet.png
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

W, H, SS = 48, 72, 8
TILE, INK, AMBER = (53, 50, 45), (232, 226, 214), (224, 160, 64)
OUT = Path(__file__).resolve().parent.parent / "docs" / "icons"


def canvas():
    im = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, W * SS - 1, H * SS - 1], radius=6 * SS, fill=TILE)
    return im, d


def s(*v):
    return [x * SS for x in v]


def bubble(d, box, tail_left=True, width=3):
    x0, y0, x1, y1 = box
    d.rounded_rectangle(s(x0, y0, x1, y1), radius=7 * SS, outline=INK, width=width * SS)
    tx = x0 + 8 if tail_left else x1 - 8
    d.polygon(s(tx, y1 - 1, tx + (5 if tail_left else -5), y1 - 1, tx - (1 if tail_left else -1), y1 + 7), fill=INK)


def font(size):
    for f in ("/System/Library/Fonts/Supplemental/Georgia Bold.ttf", "/System/Library/Fonts/Helvetica.ttc"):
        try:
            return ImageFont.truetype(f, size * SS)
        except OSError:
            pass
    return ImageFont.load_default()


def ellipsis_in_bubble(d):
    bubble(d, (8, 20, 40, 44))
    for i, x in enumerate((16, 24, 32)):
        c = AMBER if i == 2 else INK
        d.ellipse(s(x - 2.5, 30, x + 2.5, 35), fill=c)


def quote(d):
    d.text((24 * SS, 38 * SS), "“", font=font(46), fill=INK, anchor="mm")
    d.rectangle(s(14, 52, 34, 54), fill=AMBER)


def raven(d):
    # Quoth the Raven: a head in profile, beak forward, one amber eye.
    d.ellipse(s(12, 22, 34, 44), fill=INK)
    d.polygon(s(32, 28, 42, 34, 32, 38), fill=INK)
    d.polygon(s(14, 40, 30, 42, 20, 54, 12, 50), fill=INK)
    d.ellipse(s(25, 28, 29, 32), fill=AMBER)


def wave_bubble(d):
    bubble(d, (8, 20, 40, 44))
    for x, h in ((16, 4), (21, 9), (26, 6), (31, 10), (36, 3)):
        if x > 34:
            continue
        d.rounded_rectangle(s(x - 1.2, 32 - h / 2, x + 1.2, 32 + h / 2), radius=SS, fill=AMBER if x == 26 else INK)


def caret(d):
    d.text((20 * SS, 36 * SS), "Q", font=font(26), fill=INK, anchor="mm")
    d.rectangle(s(33, 26, 36, 46), fill=AMBER)


def reveal_steps(d):
    # Words arriving: three bars, the newest short and amber.
    for i, (w, c) in enumerate(((28, INK), (22, INK), (12, AMBER))):
        y = 26 + i * 9
        d.rounded_rectangle(s(10, y, 10 + w, y + 4), radius=2 * SS, fill=c)


def quill(d):
    d.polygon(s(36, 14, 40, 18, 18, 52, 14, 50), fill=INK)
    d.polygon(s(36, 14, 30, 20, 22, 30, 38, 22), fill=INK)
    d.line(s(16, 51, 12, 58), fill=AMBER, width=3 * SS)


def q_bubble(d):
    d.ellipse(s(10, 20, 38, 48), outline=INK, width=4 * SS)
    d.polygon(s(28, 42, 40, 54, 34, 44), fill=AMBER)


CONCEPTS = {
    "bubble-ellipsis": ellipsis_in_bubble,
    "quote-mark": quote,
    "raven": raven,
    "bubble-wave": wave_bubble,
    "q-caret": caret,
    "reveal-bars": reveal_steps,
    "quill": quill,
    "q-bubble": q_bubble,
}


def filled_bubble(d, box, color):
    x0, y0, x1, y1 = box
    d.rounded_rectangle(s(x0, y0, x1, y1), radius=7 * SS, fill=color)
    d.polygon(s(x0 + 8, y1 - 1, x0 + 14, y1 - 1, x0 + 7, y1 + 7), fill=color)


def ell_filled(d):
    filled_bubble(d, (8, 20, 40, 44), INK)
    for x, c in ((16, TILE), (24, TILE), (32, AMBER)):
        d.ellipse(s(x - 2.5, 30, x + 2.5, 35), fill=c)


def ell_growing(d):
    d.ellipse(s(8, 18, 40, 46), outline=INK, width=3 * SS)
    d.polygon(s(13, 40, 19, 44, 10, 52), fill=INK)
    for x, r in ((17, 1.6), (24, 2.3), (31, 3.0)):
        d.ellipse(s(x - r, 32 - r, x + r, 32 + r), fill=AMBER if r > 2.5 else INK)


def ell_typing(d):
    bubble(d, (8, 20, 40, 44))
    d.ellipse(s(13.5, 30, 18.5, 35), fill=INK)
    d.ellipse(s(21.5, 30, 26.5, 35), fill=INK)
    d.ellipse(s(30.5, 31, 33.5, 34), fill=AMBER)


def wave_filled(d):
    filled_bubble(d, (8, 20, 40, 44), AMBER)
    for x, h in ((15, 4), (20, 9), (25, 6), (30, 11), (35, 4)):
        d.rounded_rectangle(s(x - 1.2, 32 - h / 2, x + 1.2, 32 + h / 2), radius=SS, fill=TILE)


def wave_sine(d):
    import math
    bubble(d, (8, 20, 40, 44))
    pts = [(12 + i * 0.5, 32 + 5 * math.sin(i * 0.5) * math.sin(i * 0.5 * 0.2)) for i in range(0, 49)]
    d.line([(x * SS, y * SS) for x, y in pts], fill=AMBER, width=2 * SS, joint="curve")


def wave_pair(d):
    d.rounded_rectangle(s(16, 14, 42, 32), radius=6 * SS, outline=INK, width=2 * SS)
    filled_bubble(d, (6, 28, 34, 50), INK)
    for x, h in ((13, 4), (18, 8), (23, 5), (28, 9)):
        d.rounded_rectangle(s(x - 1.1, 39 - h / 2, x + 1.1, 39 + h / 2), radius=SS, fill=AMBER if x == 23 else TILE)


def q_dots(d):
    # A Q whose tail is a speech-bubble tail, pointing down and left, so it never reads as a lens.
    d.ellipse(s(8, 18, 40, 46), outline=INK, width=4 * SS)
    d.polygon(s(14, 40, 22, 45, 10, 54), fill=INK)
    for x, c in ((17, INK), (24, INK), (31, AMBER)):
        d.ellipse(s(x - 2.2, 30, x + 2.2, 34.4), fill=c)


def q_filled(d):
    d.ellipse(s(8, 18, 40, 46), fill=INK)
    d.polygon(s(14, 40, 22, 45, 10, 54), fill=AMBER)
    d.ellipse(s(17, 27, 31, 37), fill=TILE)


def q_wave(d):
    d.ellipse(s(8, 18, 40, 46), outline=INK, width=4 * SS)
    d.polygon(s(14, 40, 22, 45, 10, 54), fill=INK)
    for x, h in ((17, 4), (21, 8), (25, 5), (29, 9)):
        d.rounded_rectangle(s(x - 1, 32 - h / 2, x + 1, 32 + h / 2), radius=SS, fill=AMBER if x == 25 else INK)


VARIANTS = {
    "ellipsis-filled": ell_filled,
    "ellipsis-growing": ell_growing,
    "ellipsis-typing": ell_typing,
    "wave-filled": wave_filled,
    "wave-sine": wave_sine,
    "wave-pair": wave_pair,
    "q-dots": q_dots,
    "q-filled": q_filled,
    "q-wave": q_wave,
}


# --- Square Q: the chat bubble is a squared Q whose leg is also the bubble's tail.
BOX = (9, 19, 39, 47)  # the Q's bowl


def q_leg(d, color, width=4):
    # The leg crosses the bowl's bottom-right corner and runs out down-right, like a Q's tail.
    d.line(s(29, 39, 40, 52), fill=color, width=width * SS)


def sq_outline(d, stroke=4, radius=7):
    d.rounded_rectangle(s(*BOX), radius=radius * SS, outline=INK, width=stroke * SS)


def sq_q_outline(d):
    sq_outline(d)
    q_leg(d, AMBER)


def sq_q_filled(d):
    d.rounded_rectangle(s(*BOX), radius=7 * SS, fill=INK)
    d.rounded_rectangle(s(17, 27, 31, 39), radius=3 * SS, fill=TILE)
    q_leg(d, AMBER, 5)


def sq_q_dots(d):
    sq_outline(d)
    q_leg(d, INK)
    for x, c in ((17, INK), (24, INK), (31, AMBER)):
        d.ellipse(s(x - 2.2, 30.8, x + 2.2, 35.2), fill=c)


def sq_q_wave(d):
    sq_outline(d)
    q_leg(d, INK)
    for x, h in ((16, 4), (20.5, 9), (25, 6), (29.5, 11)):
        d.rounded_rectangle(s(x - 1.1, 33 - h / 2, x + 1.1, 33 + h / 2), radius=SS, fill=AMBER if x == 25 else INK)


def sq_q_filled_dots(d):
    d.rounded_rectangle(s(*BOX), radius=7 * SS, fill=INK)
    q_leg(d, INK, 5)
    for x, c in ((17, TILE), (24, TILE), (31, AMBER)):
        d.ellipse(s(x - 2.4, 30.6, x + 2.4, 35.4), fill=c)


def sq_q_amber(d):
    d.rounded_rectangle(s(*BOX), radius=7 * SS, fill=AMBER)
    d.rounded_rectangle(s(17, 27, 31, 39), radius=3 * SS, fill=TILE)
    q_leg(d, INK, 5)


def sq_q_bold(d):
    # Heavy bowl, the leg cut through the stroke in the tile colour, then drawn out in amber.
    sq_outline(d, stroke=6, radius=8)
    d.line(s(27, 37, 34, 45), fill=TILE, width=6 * SS)
    d.line(s(30, 40, 40, 52), fill=AMBER, width=5 * SS)


def sq_q_letter(d):
    # The squared Q bubble with the letter itself typed inside it, in amber.
    sq_outline(d, stroke=3)
    q_leg(d, INK, 3)
    d.text((24 * SS, 33 * SS), "Q", font=font(15), fill=AMBER, anchor="mm")


SQUARE_Q = {
    "sq-q-outline": sq_q_outline,
    "sq-q-filled": sq_q_filled,
    "sq-q-dots": sq_q_dots,
    "sq-q-wave": sq_q_wave,
    "sq-q-filled-dots": sq_q_filled_dots,
    "sq-q-amber": sq_q_amber,
    "sq-q-bold": sq_q_bold,
    "sq-q-letter": sq_q_letter,
}

# --- Final round: wave-filled in palettes, and the solid square Q holding waves.
SAGE, SLATE, BRICK = (143, 170, 128), (122, 150, 184), (196, 98, 76)
BARS = ((15, 4), (20, 9), (25, 6), (30, 11), (35, 4))


def wave_filled_in(bubble_color, bar_color, tile=None):
    def draw(d):
        if tile is not None:
            d.rounded_rectangle([0, 0, W * SS - 1, H * SS - 1], radius=6 * SS, fill=tile)
        filled_bubble(d, (8, 20, 40, 44), bubble_color)
        for x, h in BARS:
            d.rounded_rectangle(s(x - 1.2, 32 - h / 2, x + 1.2, 32 + h / 2), radius=SS, fill=bar_color)
    return draw


def sq_solid(d):
    d.rounded_rectangle(s(*BOX), radius=7 * SS, fill=INK)
    q_leg(d, INK, 5)


def bar(d, x, h, cy=33, w=1.2, c=TILE):
    d.rounded_rectangle(s(x - w, cy - h / 2, x + w, cy + h / 2), radius=SS, fill=c)


def sqw_even(d):
    sq_solid(d)
    for x in (16, 20, 24, 28, 32):
        bar(d, x, 7)


def sqw_peak(d):
    sq_solid(d)
    for x, h in ((15, 3), (18.5, 6), (22, 10), (25.5, 14), (29, 9), (32.5, 4)):
        bar(d, x, h, w=1.1)


def sqw_sine(d):
    import math
    sq_solid(d)
    pts = [(14 + i * 0.4, 33 - 5 * math.sin(i * 0.4 * 0.55)) for i in range(0, 51)]
    d.line([(x * SS, y * SS) for x, y in pts], fill=TILE, width=int(2.4 * SS), joint="curve")


def sqw_arcs(d):
    # A speaker's sound: a dot and three widening arcs.
    sq_solid(d)
    d.ellipse(s(14, 30.5, 19, 35.5), fill=TILE)
    for i, r in enumerate((6, 10, 14)):
        c = AMBER if i == 2 else TILE
        d.arc(s(16.5 - r, 33 - r, 16.5 + r, 33 + r), start=-45, end=45, fill=c, width=int(2.2 * SS))


def sqw_dotted(d):
    import math
    sq_solid(d)
    for i in range(8):
        x = 14.5 + i * 2.5
        y = 33 - 4.5 * math.sin(i * 0.8)
        d.ellipse(s(x - 1.2, y - 1.2, x + 1.2, y + 1.2), fill=AMBER if i == 7 else TILE)


def sqw_amber_peak(d):
    sq_solid(d)
    for x, h in ((15.5, 4), (20, 8), (24.5, 13), (29, 7), (33.5, 4)):
        bar(d, x, h, c=AMBER if h == 13 else TILE)


FINAL = {
    "wave-amber": wave_filled_in(AMBER, TILE),
    "wave-cream": wave_filled_in(INK, TILE),
    "wave-sage": wave_filled_in(SAGE, TILE),
    "wave-slate": wave_filled_in(SLATE, TILE),
    "wave-brick": wave_filled_in(BRICK, TILE),
    "wave-inverse": wave_filled_in(TILE, AMBER, tile=INK),
    "sqw-even": sqw_even,
    "sqw-peak": sqw_peak,
    "sqw-sine": sqw_sine,
    "sqw-arcs": sqw_arcs,
    "sqw-dotted": sqw_dotted,
    "sqw-amber-peak": sqw_amber_peak,
}

def render(concepts, sheet_name, cols):
    icons = []
    for name, draw in concepts.items():
        im, d = canvas()
        draw(d)
        small = im.resize((W, H), Image.LANCZOS)
        small.save(OUT / f"{name}.png")
        icons.append((name, small))
    cell_w, cell_h, pad = W * 3 + W + 40, H * 3 + 34, 12
    rows = (len(icons) + cols - 1) // cols
    sheet = Image.new("RGB", (cell_w * cols + pad, cell_h * rows + pad), (30, 30, 30))
    d = ImageDraw.Draw(sheet)
    label = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 13)
    for i, (name, im) in enumerate(icons):
        x, y = pad + (i % cols) * cell_w, pad + (i // cols) * cell_h
        big = im.resize((W * 3, H * 3), Image.NEAREST)
        sheet.paste(big, (x, y), big)
        sheet.paste(im, (x + W * 3 + 10, y), im)
        light = Image.new("RGB", (W + 8, H + 8), (240, 240, 240))
        light.paste(im, (4, 4), im)
        sheet.paste(light, (x + W * 3 + 6, y + H + 14))
        d.text((x, y + H * 3 + 8), f"{i + 1}. {name}", font=label, fill=(220, 220, 220))
    sheet.save(OUT / sheet_name)
    return len(icons)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = render(CONCEPTS, "sheet.png", 4) + render(VARIANTS, "sheet-variants.png", 3)
    n += render(SQUARE_Q, "sheet-square-q.png", 4)
    n += render(FINAL, "sheet-final.png", 6)
    print("wrote", n, "icons to", OUT)


if __name__ == "__main__":
    main()
