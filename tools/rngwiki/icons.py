"""Render inventory-style item icons from block and item models."""

from __future__ import annotations

import math

from PIL import Image, ImageDraw, ImageEnhance

from .resources import Resources, split_id

SIZE = 128
_SUPERSAMPLE = 2
_FOLIAGE_TINT = (124, 189, 107)
_SHADE = {"up": 1.0, "north": 0.8, "west": 0.6}


def _resolve_model(resources: Resources, model_id: str) -> tuple[dict, list | None, list[str]]:
    textures: dict[str, str] = {}
    elements = None
    chain: list[str] = []
    current: str | None = model_id
    while current and len(chain) < 16:
        chain.append(current)
        model = resources.model(current)
        if model is None:
            break
        for key, value in model.get("textures", {}).items():
            textures.setdefault(key, value)
        if elements is None and "elements" in model:
            elements = model["elements"]
        current = model.get("parent")
        if current and ":" not in current:
            current = f"minecraft:{current}"
    return textures, elements, chain


def _texture_ref(textures: dict, ref: str | None) -> str | None:
    seen = 0
    while ref and ref.startswith("#") and seen < 16:
        ref = textures.get(ref[1:])
        seen += 1
    return ref


def _tinted(image: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    r, g, b, a = image.split()
    r = r.point(lambda v: v * color[0] // 255)
    g = g.point(lambda v: v * color[1] // 255)
    b = b.point(lambda v: v * color[2] // 255)
    return Image.merge("RGBA", (r, g, b, a))


def _flat_icon(resources: Resources, textures: dict) -> Image.Image | None:
    layers = []
    index = 0
    while f"layer{index}" in textures:
        layers.append(textures[f"layer{index}"])
        index += 1
    if not layers:
        layers = [textures[key] for key in ("base", "particle") if key in textures][:1]
    images = [img for img in (resources.texture(_texture_ref(textures, ref) or "") for ref in layers) if img]
    if not images:
        return None
    width = max(img.width for img in images)
    icon = Image.new("RGBA", (width, width), (0, 0, 0, 0))
    for img in images:
        icon.alpha_composite(img.resize((width, width), Image.NEAREST))
    return icon.resize((SIZE, SIZE), Image.NEAREST)


class _Projection:
    """Maps model-space block coordinates (0-16) to an inventory-angle canvas."""

    def __init__(self, size: int):
        width = size * 0.875
        top = width / 2
        side = width * math.cos(math.radians(30)) / math.sqrt(2)
        x0 = (size - width) / 2
        y0 = (size - (top + side)) / 2
        self.corner = (x0 + width / 2, y0 + top)  # top north-west corner, nearest the viewer
        self.ex = (-width / 2 / 16, -top / 2 / 16)  # +x (east) runs up-left
        self.ez = (width / 2 / 16, -top / 2 / 16)  # +z (south) runs up-right
        self.ey = (0.0, -side / 16)  # +y runs straight up

    def point(self, x: float, y: float, z: float) -> tuple[float, float]:
        cx, cy = self.corner
        dy = y - 16
        return (
            cx + x * self.ex[0] + z * self.ez[0] + dy * self.ey[0],
            cy + x * self.ex[1] + z * self.ez[1] + dy * self.ey[1],
        )


def _face_source(texture: Image.Image, uv: list[float], rotation: int) -> Image.Image | None:
    scale = texture.width / 16
    u1, v1, u2, v2 = (c * scale for c in uv)
    box = (round(min(u1, u2)), round(min(v1, v2)), round(max(u1, u2)), round(max(v1, v2)))
    if box[2] <= box[0] or box[3] <= box[1]:
        return None
    source = texture.crop(box)
    if u1 > u2:
        source = source.transpose(Image.FLIP_LEFT_RIGHT)
    if v1 > v2:
        source = source.transpose(Image.FLIP_TOP_BOTTOM)
    if rotation:
        source = source.rotate(-rotation, expand=True)
    return source


def _draw_face(canvas: Image.Image, source: Image.Image, origin, u_end, v_end, shade: float) -> None:
    # Upscale and edge-pad the texture so neighbouring faces overlap by a fraction of a texel.
    k = 16
    big = source.resize((source.width * k, source.height * k), Image.NEAREST)
    padded = Image.new("RGBA", (big.width + 2, big.height + 2))
    padded.paste(big, (1, 1))
    padded.paste(big.crop((0, 0, big.width, 1)), (1, 0))
    padded.paste(big.crop((0, big.height - 1, big.width, big.height)), (1, big.height + 1))
    padded.paste(padded.crop((1, 0, 2, padded.height)), (0, 0))
    padded.paste(padded.crop((padded.width - 2, 0, padded.width - 1, padded.height)), (padded.width - 1, 0))
    sw, sh = big.width, big.height
    ux, uy = (u_end[0] - origin[0]) / sw, (u_end[1] - origin[1]) / sw
    vx, vy = (v_end[0] - origin[0]) / sh, (v_end[1] - origin[1]) / sh
    det = ux * vy - vx * uy
    if abs(det) < 1e-9:
        return
    a, b = vy / det, -vx / det
    d, e = -uy / det, ux / det
    ox, oy = origin
    coefficients = (a, b, -(a * ox + b * oy) + 1, d, e, -(d * ox + e * oy) + 1)
    face = padded.transform(canvas.size, Image.AFFINE, coefficients, Image.NEAREST, fillcolor=(0, 0, 0, 0))
    if shade < 1:
        alpha = face.getchannel("A")
        face = ImageEnhance.Brightness(face).enhance(shade)
        face.putalpha(alpha)
    canvas.alpha_composite(face)


def _fit_small_model(elements: list) -> list:
    """Enlarge small models such as cable cores so they stay readable at slot size."""
    lows = [min(e["from"][i] for e in elements) for i in range(3)]
    highs = [max(e["to"][i] for e in elements) for i in range(3)]
    extent = max(h - l for l, h in zip(lows, highs))
    if extent <= 0 or extent >= 10:
        return elements
    scale = min(2.0, 12 / extent)
    centre = [(l + h) / 2 for l, h in zip(lows, highs)]

    def move(point):
        return [8 + (point[i] - centre[i]) * scale for i in range(3)]

    return [{**e, "from": move(e["from"]), "to": move(e["to"]), "uv_box": (e["from"], e["to"])} for e in elements]


def _block_icon(resources: Resources, textures: dict, elements: list, tint: bool) -> Image.Image | None:
    size = SIZE * _SUPERSAMPLE
    projection = _Projection(size)
    elements = _fit_small_model(elements)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    def depth(element):
        (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
        return (x1 + x2) + (z1 + z2) - (y1 + y2)

    drawn = False
    for element in sorted(elements, key=depth, reverse=True):
        (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
        faces = element.get("faces", {})
        (a1, b1, c1), (a2, b2, c2) = element.get("uv_box", (element["from"], element["to"]))
        p = projection.point
        geometry = {
            "up": ((a1, c1, a2, c2), p(x1, y2, z1), p(x2, y2, z1), p(x1, y2, z2)),
            "north": ((16 - a2, 16 - b2, 16 - a1, 16 - b1), p(x2, y2, z1), p(x1, y2, z1), p(x2, y1, z1)),
            "west": ((c1, 16 - b2, c2, 16 - b1), p(x1, y2, z1), p(x1, y2, z2), p(x1, y1, z1)),
        }
        for name in ("up", "north", "west"):
            face = faces.get(name)
            if not face:
                continue
            texture = resources.texture(_texture_ref(textures, face.get("texture")) or "")
            if texture is None:
                continue
            default_uv, origin, u_end, v_end = geometry[name]
            source = _face_source(texture, face.get("uv", list(default_uv)), face.get("rotation", 0))
            if source is None:
                continue
            if tint and "tintindex" in face:
                source = _tinted(source, _FOLIAGE_TINT)
            _draw_face(canvas, source, origin, u_end, v_end, _SHADE[name])
            drawn = True
    if not drawn:
        return None
    return canvas.resize((SIZE, SIZE), Image.LANCZOS)


def placeholder() -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((16, 16, SIZE - 16, SIZE - 16), radius=12, fill=(139, 139, 139, 255), outline=(55, 55, 55, 255), width=6)
    draw.text((SIZE / 2, SIZE / 2), "?", fill=(255, 255, 255, 255), anchor="mm", font_size=56)
    return image


def render(resources: Resources, item_id: str) -> Image.Image | None:
    namespace, path = split_id(item_id)
    textures, elements, chain = _resolve_model(resources, f"{namespace}:item/{path}")
    if len(chain) == 1 and resources.model(chain[0]) is None:
        textures, elements, chain = _resolve_model(resources, f"{namespace}:block/{path}")
    generated = any(c.endswith("item/generated") or c.endswith("builtin/generated") for c in chain)
    if elements and not generated:
        icon = _block_icon(resources, textures, elements, tint=namespace == "minecraft")
        if icon:
            return icon
    return _flat_icon(resources, textures)
