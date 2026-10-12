"""Render inventory-style item icons from block and item models."""

from __future__ import annotations

import math

from PIL import Image, ImageDraw, ImageEnhance

from .resources import Resources, split_id

SIZE = 128
_SUPERSAMPLE = 2
_FOLIAGE_TINT = (124, 189, 107)
# Direction toward the viewer: the projection collapses (1, -top/side, 1), and the viewer sits on the up-north-west side.
_VIEW = (-1.0, math.sqrt(2) / (2 * math.cos(math.radians(30))), -1.0)
_NORMALS = {
    "up": (0.0, 1.0, 0.0),
    "down": (0.0, -1.0, 0.0),
    "north": (0.0, 0.0, -1.0),
    "south": (0.0, 0.0, 1.0),
    "west": (-1.0, 0.0, 0.0),
    "east": (1.0, 0.0, 0.0),
}


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


def _shade(normal: tuple[float, float, float]) -> float:
    """Minecraft's directional face shading, so rotated faces shade between their neighbours."""
    nx, ny, nz = normal
    return nx * nx * 0.6 + nz * nz * 0.8 + ny * ny * (1.0 if ny > 0 else 0.5)


def _rotator(rotation: dict | None):
    """A point rotation for an element's "rotation" block: origin, axis, angle, and optional rescale."""
    if not rotation:
        return lambda point, is_vector=False: point
    angle = math.radians(rotation.get("angle", 0.0))
    axis = rotation.get("axis", "y")
    origin = rotation.get("origin", [8, 8, 8])
    cos, sin = math.cos(angle), math.sin(angle)
    scale = 1.0 / cos if rotation.get("rescale") and abs(cos) > 1e-6 else 1.0

    def rotate(point, is_vector=False):
        x, y, z = (point[i] - (0 if is_vector else origin[i]) for i in range(3))
        if axis == "x":
            y, z = y * cos - z * sin, y * sin + z * cos
            if not is_vector:
                y, z = y * scale, z * scale
        elif axis == "y":
            x, z = x * cos + z * sin, -x * sin + z * cos
            if not is_vector:
                x, z = x * scale, z * scale
        else:
            x, y = x * cos - y * sin, x * sin + y * cos
            if not is_vector:
                x, y = x * scale, y * scale
        if is_vector:
            return (x, y, z)
        return (x + origin[0], y + origin[1], z + origin[2])

    return rotate


def _face_geometry(element: dict) -> dict:
    """Per face: default UV, then the texture's top-left, top-right, and bottom-left corners in model space."""
    (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
    (a1, b1, c1), (a2, b2, c2) = element.get("uv_box", (element["from"], element["to"]))
    return {
        "up": ((a1, c1, a2, c2), (x1, y2, z1), (x2, y2, z1), (x1, y2, z2)),
        "down": ((a1, 16 - c2, a2, 16 - c1), (x1, y1, z2), (x2, y1, z2), (x1, y1, z1)),
        "north": ((16 - a2, 16 - b2, 16 - a1, 16 - b1), (x2, y2, z1), (x1, y2, z1), (x2, y1, z1)),
        "south": ((a1, 16 - b2, a2, 16 - b1), (x1, y2, z2), (x2, y2, z2), (x1, y1, z2)),
        "west": ((c1, 16 - b2, c2, 16 - b1), (x1, y2, z1), (x1, y2, z2), (x1, y1, z1)),
        "east": ((16 - c2, 16 - b2, 16 - c1, 16 - b1), (x2, y2, z2), (x2, y2, z1), (x2, y1, z2)),
    }


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

    def moved(element):
        fitted = {**element, "from": move(element["from"]), "to": move(element["to"]), "uv_box": (element["from"], element["to"])}
        if element.get("rotation"):
            fitted["rotation"] = {**element["rotation"], "origin": move(element["rotation"].get("origin", [8, 8, 8]))}
        return fitted

    return [moved(e) for e in elements]


def _block_icon(resources: Resources, textures: dict, elements: list, tint: bool) -> Image.Image | None:
    size = SIZE * _SUPERSAMPLE
    projection = _Projection(size)
    elements = _fit_small_model(elements)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    def depth(element):
        (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
        x, y, z = _rotator(element.get("rotation"))(((x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2))
        return 2 * x + 2 * z - 2 * y

    drawn = False
    for element in sorted(elements, key=depth, reverse=True):
        faces = element.get("faces", {})
        rotate = _rotator(element.get("rotation"))
        for name, (default_uv, origin, u_end, v_end) in _face_geometry(element).items():
            face = faces.get(name)
            if not face:
                continue
            normal = rotate(_NORMALS[name], is_vector=True)
            if sum(n * v for n, v in zip(normal, _VIEW)) <= 1e-6:
                continue
            texture = resources.texture(_texture_ref(textures, face.get("texture")) or "")
            if texture is None:
                continue
            source = _face_source(texture, face.get("uv", list(default_uv)), face.get("rotation", 0))
            if source is None:
                continue
            if tint and "tintindex" in face:
                source = _tinted(source, _FOLIAGE_TINT)
            corners = [projection.point(*rotate(corner)) for corner in (origin, u_end, v_end)]
            _draw_face(canvas, source, *corners, _shade(normal))
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
