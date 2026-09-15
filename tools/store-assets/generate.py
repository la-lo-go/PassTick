"""Generates the store assets from the PassTick wordmark and screenshots.

Imports new captures from build/screenshots-raw when the capture script has written them,
exports the clean 2:1 set and the framed 9:16 set, and publishes one set to fastlane.

Usage:
    python tools/store-assets/generate.py [--set framed|clean]
Requires: pillow, svglib, reportlab.
"""

from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

try:
    import cairocffi  # noqa: F401
except (ImportError, OSError):
    # rlPyCairo prefers cairocffi but falls back to pycairo only on ImportError.
    sys.modules["cairocffi"] = None

from PIL import Image, ImageDraw, ImageFilter, ImageFont
from reportlab.graphics import renderPM
from svglib.svglib import svg2rlg

ROOT = Path(__file__).resolve().parents[2]
FASTLANE_IMAGES = ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images"
FASTLANE_SCREENSHOTS = FASTLANE_IMAGES / "phoneScreenshots"
SCREENSHOTS = ROOT / "docs" / "assets" / "screenshots"
THEME_TILES = SCREENSHOTS / "themes"
RAW_SCREENSHOTS = ROOT / "build" / "screenshots-raw"
STORE_ASSETS = ROOT / "build" / "store-assets"
PROMO = ROOT / "meta" / "gfx" / "promo"

NAVY = "#102F4F"
CORAL = "#FF6249"
WORDMARK_FILL = "#F7F2FA"
FRAME_GRADIENT_TOP = (23, 60, 97)
FRAME_GRADIENT_BOTTOM = (11, 35, 56)
FRAME_BEZEL = (10, 33, 56)
FRAME_EDGE = "#1E4E7E"

CLEAN_SIZE = (1080, 2160)
FRAMED_SIZE = (1080, 1920)

THEME_TILE_ORDER = (
    "theme-coral",
    "theme-coral-light",
    "theme-ocean",
    "theme-violet",
    "theme-amber",
    "theme-forest",
)

# Sources in listing order with their framed headline.
CAPTIONS = (
    ("home-today", "Every pass in one place"),
    ("timeline", "Every date on one timeline"),
    ("pass-detail", "Show the code. Scan. Go."),
    ("export-image", "Export passes as images"),
    ("themes", "Material You. Any color."),
    ("settings-privacy", "Private by design"),
    ("edit-pass", "Edit every detail"),
    ("tags", "Organize with colored tags"),
)

FONT_CANDIDATES = (
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
)

TICKET_PATHS = (
    '<path fill="{coral}" fill-rule="evenodd" d="M14 29h80v17c-5.5 0-10 3.6-10 8s4.5 8 10 8v17H14V62'
    'c5.5 0 10-3.6 10-8s-4.5-8-10-8V29zm38 6c0-1.1.9-2 2-2s2 .9 2 2v6c0 1.1-.9 2-2 2s-2-.9-2-2v-6zm0 13'
    'c0-1.1.9-2 2-2s2 .9 2 2v6c0 1.1-.9 2-2 2s-2-.9-2-2v-6zm0 13c0-1.1.9-2 2-2s2 .9 2 2v6c0 1.1-.9 2-2 2'
    's-2-.9-2-2v-6z"/>'.format(coral=CORAL)
)

ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
<rect width="108" height="108" fill="{navy}"/>
<g transform="translate(54 54) rotate(-45) scale(.72) translate(-54 -54)">
{paths}
</g>
</svg>"""

FEATURE_SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
<rect width="1024" height="500" fill="{navy}"/>
<g transform="translate(187 160) scale(1.66667)">
<g transform="translate(54 54) rotate(-45) scale(.72) translate(-54 -54)">
{paths}
</g>
<text x="112" y="69" font-family="Helvetica" font-size="43" font-weight="bold" fill="{fill}">PassTick</text>
</g>
</svg>"""


def render_svg(svg: str, width: int, height: int) -> Image.Image:
    drawing = svg2rlg(io.BytesIO(svg.encode()))
    data = renderPM.drawToString(drawing, fmt="PNG", dpi=96)
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    if image.size != (width, height):
        raise SystemExit(f"Unexpected render size {image.size}, expected {(width, height)}")
    return image


def generate_icon() -> Image.Image:
    svg = ICON_SVG.format(navy=NAVY, paths=TICKET_PATHS)
    return render_svg(svg, 512, 512)


def generate_feature_graphic() -> Image.Image:
    svg = FEATURE_SVG.format(navy=NAVY, fill=WORDMARK_FILL, paths=TICKET_PATHS)
    return render_svg(svg, 1024, 500)


def load_bold_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for candidate in FONT_CANDIDATES:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    print("Warning: no bold system font found, falling back to the Pillow default font.")
    return ImageFont.load_default(size=size)


def import_raw_captures() -> None:
    """Refreshes the committed screenshot sources from the latest device captures."""
    captures = sorted(RAW_SCREENSHOTS.glob("*.png")) if RAW_SCREENSHOTS.is_dir() else []
    if not captures:
        return
    SCREENSHOTS.mkdir(parents=True, exist_ok=True)
    for capture in captures:
        clean = normalize_clean(Image.open(capture).convert("RGB"))
        if capture.stem.startswith("theme-"):
            THEME_TILES.mkdir(parents=True, exist_ok=True)
            clean.save(THEME_TILES / f"{capture.stem}.webp", "WEBP", quality=92, method=6)
        else:
            clean.save(SCREENSHOTS / f"{capture.stem}.webp", "WEBP", quality=92, method=6)


def normalize_clean(image: Image.Image, size: tuple[int, int] = CLEAN_SIZE) -> Image.Image:
    width, height = size
    if image.width != width:
        image = image.resize((width, round(image.height * width / image.width)), Image.LANCZOS)
    if image.height > height:
        offset = (image.height - height) // 2
        image = image.crop((0, offset, width, offset + height))
    elif image.height < height:
        pad = Image.new("RGB", size, image.getpixel((0, 0)))
        pad.paste(image, (0, (height - image.height) // 2))
        image = pad
    return image.convert("RGB")


def gradient_background(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        progress = y / max(1, height - 1)
        color = tuple(
            round(start + (end - start) * progress)
            for start, end in zip(FRAME_GRADIENT_TOP, FRAME_GRADIENT_BOTTOM)
        )
        draw.line((0, y, width, y), fill=color)
    return image


def wrap_headline(draw: ImageDraw.ImageDraw, text: str, font, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for word in text.split():
        candidate = f"{current} {word}".strip()
        if not current or draw.textlength(candidate, font=font) <= max_width:
            current = candidate
        else:
            lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def paste_rounded(canvas: Image.Image, image: Image.Image, position: tuple[int, int], radius: int) -> None:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, image.width - 1, image.height - 1), radius=radius, fill=255
    )
    canvas.paste(image, position, mask)


def draw_device_frame(canvas: Image.Image, screenshot: Image.Image) -> None:
    left, top, right, bottom = 145, 300, 935, 1880
    bezel = 18
    outer_radius = 80
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (left + 6, top + 18, right + 6, bottom + 18), radius=outer_radius, fill=(0, 0, 0, 120)
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(26)))

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((left, top, right, bottom), radius=outer_radius, fill=FRAME_BEZEL, outline=FRAME_EDGE, width=3)

    inner_width = right - left - 2 * bezel
    scaled = screenshot.resize((inner_width, round(screenshot.height * inner_width / screenshot.width)), Image.LANCZOS)
    inner_top = top + bezel + (bottom - top - 2 * bezel - scaled.height) // 2
    paste_rounded(canvas, scaled, (left + bezel, inner_top), radius=64)


def framed_screenshot(screenshot: Image.Image, headline: str) -> Image.Image:
    canvas = gradient_background(FRAMED_SIZE)
    draw_headline(canvas, headline, first_line_y=150)
    draw_device_frame(canvas, screenshot)
    return canvas.convert("RGB")


def draw_headline(canvas: Image.Image, headline: str, first_line_y: int) -> None:
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((504, 62, 576, 74), radius=6, fill=CORAL)

    size = 58
    font = load_bold_font(size)
    lines = wrap_headline(draw, headline, font, 880)
    while len(lines) > 2 and size > 40:
        size -= 6
        font = load_bold_font(size)
        lines = wrap_headline(draw, headline, font, 880)
    for index, line in enumerate(lines):
        draw.text((540, first_line_y - (len(lines) - 1) * 36 + index * 72), line, font=font, fill=WORDMARK_FILL, anchor="mm")


def material_you_collage(headline: str, size: tuple[int, int]) -> Image.Image:
    canvas = gradient_background(size)
    draw_headline(canvas, headline, first_line_y=150)

    tiles = [THEME_TILES / f"{name}.webp" for name in THEME_TILE_ORDER]
    missing = [tile for tile in tiles if not tile.is_file()]
    if missing:
        raise SystemExit("Missing Material You tiles: " + ", ".join(str(tile) for tile in missing))
    width, height = size
    columns, rows = 2, 3
    margin, gap = 70, 36
    top = 300
    tile_width = (width - 2 * margin - (columns - 1) * gap) // columns
    tile_height = (height - top - 50 - (rows - 1) * gap) // rows

    for index, tile in enumerate(tiles[: columns * rows]):
        column, row = index % columns, index // columns
        x = margin + column * (tile_width + gap)
        y = top + row * (tile_height + gap)
        screenshot = normalize_clean(Image.open(tile).convert("RGB"), (1080, 2160))
        scaled = screenshot.resize((tile_width, round(screenshot.height * tile_width / screenshot.width)), Image.LANCZOS)
        crop = scaled.crop((0, 0, tile_width, min(scaled.height, tile_height)))
        paste_rounded(canvas, crop, (x, y), radius=28)
        ImageDraw.Draw(canvas).rounded_rectangle(
            (x, y, x + tile_width - 1, y + crop.height - 1), radius=28, outline="#2A5680", width=2
        )
    return canvas.convert("RGB")


def write_sets(target_set: str) -> None:
    framed_directory = STORE_ASSETS / "framed"
    clean_directory = STORE_ASSETS / "clean"
    FASTLANE_SCREENSHOTS.mkdir(parents=True, exist_ok=True)
    for directory in (framed_directory, clean_directory):
        directory.mkdir(parents=True, exist_ok=True)
        for stale in directory.glob("*.png"):
            stale.unlink()

    for index, (name, headline) in enumerate(CAPTIONS, start=1):
        if name == "themes":
            framed = material_you_collage(headline, FRAMED_SIZE)
            clean = material_you_collage(headline, CLEAN_SIZE)
        else:
            source = SCREENSHOTS / f"{name}.webp"
            if not source.is_file():
                raise SystemExit(f"Missing screenshot source {source}")
            clean = normalize_clean(Image.open(source).convert("RGB"))
            framed = framed_screenshot(clean, headline)

        framed.save(framed_directory / f"{index:02d}-{name}.png", "PNG", optimize=True)
        clean.save(clean_directory / f"{index:02d}-{name}.png", "PNG", optimize=True)
        published = framed if target_set == "framed" else clean
        published.convert("RGB").save(FASTLANE_SCREENSHOTS / f"{index}.png", "PNG", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate the PassTick store assets.")
    parser.add_argument("--set", choices=("framed", "clean"), default="framed", dest="target_set")
    arguments = parser.parse_args()

    FASTLANE_IMAGES.mkdir(parents=True, exist_ok=True)
    import_raw_captures()

    icon = generate_icon()
    icon.save(FASTLANE_IMAGES / "icon.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "icon512x512.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "icon512x512_no_border.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "ic_launcher-web.png", "PNG", optimize=True)
    feature_graphic = generate_feature_graphic()
    feature_graphic.convert("RGB").save(FASTLANE_IMAGES / "featureGraphic.png", "PNG", optimize=True)
    feature_graphic.convert("RGB").save(PROMO / "1024x500.png", "PNG", optimize=True)

    write_sets(arguments.target_set)
    print(f"Wrote {len(CAPTIONS)} screenshots to {FASTLANE_SCREENSHOTS} as the {arguments.target_set} set.")


if __name__ == "__main__":
    main()
