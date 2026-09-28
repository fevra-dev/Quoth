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


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icons = []
    for name, draw in CONCEPTS.items():
        im, d = canvas()
        draw(d)
        small = im.resize((W, H), Image.LANCZOS)
        small.save(OUT / f"{name}.png")
        icons.append((name, small))
    # Sheet: each icon at 1x and 3x on RuneLite's dark panel, plus 1x on light.
    cell_w, cell_h, pad = W * 3 + W + 40, H * 3 + 34, 12
    sheet = Image.new("RGB", (cell_w * 4 + pad, cell_h * 2 + pad), (30, 30, 30))
    d = ImageDraw.Draw(sheet)
    label = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 13)
    for i, (name, im) in enumerate(icons):
        x, y = pad + (i % 4) * cell_w, pad + (i // 4) * cell_h
        sheet.paste(im.resize((W * 3, H * 3), Image.NEAREST), (x, y), im.resize((W * 3, H * 3), Image.NEAREST))
        sheet.paste(im, (x + W * 3 + 10, y), im)
        light = Image.new("RGB", (W + 8, H + 8), (240, 240, 240))
        light.paste(im, (4, 4), im)
        sheet.paste(light, (x + W * 3 + 6, y + H + 14))
        d.text((x, y + H * 3 + 8), f"{i + 1}. {name}", font=label, fill=(220, 220, 220))
    sheet.save(OUT / "sheet.png")
    print("wrote", len(icons), "icons and sheet to", OUT)


if __name__ == "__main__":
    main()
