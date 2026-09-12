"""Generates the store assets from the PassTick wordmark and screenshots.

Usage: python tools/store-assets/generate.py
Requires: pillow, svglib, reportlab.
"""

from __future__ import annotations

import io
import sys
from pathlib import Path

try:
    import cairocffi  # noqa: F401
except (ImportError, OSError):
    # rlPyCairo prefers cairocffi but falls back to pycairo only on ImportError.
    sys.modules["cairocffi"] = None

from PIL import Image
from reportlab.graphics import renderPM
from svglib.svglib import svg2rlg

ROOT = Path(__file__).resolve().parents[2]
FASTLANE_IMAGES = ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images"
FASTLANE_SCREENSHOTS = FASTLANE_IMAGES / "phoneScreenshots"
SCREENSHOTS = ROOT / "docs" / "assets" / "screenshots"
PROMO = ROOT / "meta" / "gfx" / "promo"

NAVY = "#102F4F"
CORAL = "#FF6249"
WORDMARK_FILL = "#F7F2FA"

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


def generate_screenshots() -> None:
    FASTLANE_SCREENSHOTS.mkdir(parents=True, exist_ok=True)
    for index, source in enumerate(sorted(SCREENSHOTS.glob("*.webp")), start=1):
        image = Image.open(source).convert("RGB")
        width, height = image.size
        target_height = width * 2
        if height > target_height:
            offset = (height - target_height) // 2
            image = image.crop((0, offset, width, offset + target_height))
        image.save(FASTLANE_SCREENSHOTS / f"{index}.png", "PNG", optimize=True)


def main() -> None:
    FASTLANE_IMAGES.mkdir(parents=True, exist_ok=True)
    icon = generate_icon()
    icon.save(FASTLANE_IMAGES / "icon.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "icon512x512.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "icon512x512_no_border.png", "PNG", optimize=True)
    icon.convert("RGB").save(PROMO / "ic_launcher-web.png", "PNG", optimize=True)
    feature_graphic = generate_feature_graphic()
    feature_graphic.convert("RGB").save(FASTLANE_IMAGES / "featureGraphic.png", "PNG", optimize=True)
    feature_graphic.convert("RGB").save(PROMO / "1024x500.png", "PNG", optimize=True)
    generate_screenshots()


if __name__ == "__main__":
    main()
