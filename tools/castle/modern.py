"""Read a MODERN (1.18+) world. anvil.py reads the NBT; this reads the layout.

Three things moved between the 1.13-era format anvil.py was written against and
what a 1.21 save holds, and only one of them is announced by the file:

    Level.Sections[]           ->  sections[]              (no Level wrapper)
    Palette / BlockStates      ->  block_states.{palette,data}
    values may straddle a long ->  they may NOT

The third is the dangerous one. anvil.unpack already takes a `spanning` flag, and
passing the wrong one does not fail — it returns plausible indices that are
simply the wrong blocks, which looks like a build made of the right materials in
the wrong order. There is no checksum to catch it. So bits-per-block is derived
from the palette AND cross-checked against the array length here, and a section
whose two disagree is solved from the length, because the length is a fact and
the palette is an inference.

A section with a palette but NO data array is a solid section of one block —
sixteen cubed of stone, or of air. Dropping those (they look empty) puts holes in
anything sitting on flat ground.
"""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import anvil

AIR = ("air", "cave_air", "void_air")


def bits_for(palette, data):
    """From the palette, then argued with the array length."""
    bits = max(4, (len(palette) - 1).bit_length())
    if not data:
        return bits
    per = 64 // bits
    if (4096 + per - 1) // per == len(data):
        return bits
    # THE LENGTH WINS. Same lesson as bits_for in anvil.py, arriving from the
    # other direction: a save that has been through an upgrade can hold a section
    # written at a stride its palette no longer needs.
    for b in range(4, 17):
        p = 64 // b
        if (4096 + p - 1) // p == len(data):
            return b
    raise ValueError("no stride fits %d longs" % len(data))


def sections(chunk):
    """[(sectionY, palette, [4096 indices])], including the uniform ones."""
    out = []
    for sec in chunk.get("sections", []):
        bs = sec.get("block_states")
        if not bs:
            continue
        pal = bs.get("palette")
        if not pal:
            continue
        data = bs.get("data")
        if not data:
            out.append((sec["Y"], pal, [0] * 4096))
            continue
        bits = bits_for(pal, data)
        idx = anvil.unpack(data, bits, 4096, False)
        if max(idx) >= len(pal):
            raise ValueError("index %d off a %d palette" % (max(idx), len(pal)))
        out.append((sec["Y"], pal, idx))
    return out


class World:
    def __init__(s, root, dim="minecraft/overworld"):
        s.dir = os.path.join(root, "dimensions", dim, "region")
        if not os.path.isdir(s.dir):
            s.dir = os.path.join(root, "region")
        s.regs = {}

    def region(s, rx, rz):
        if (rx, rz) not in s.regs:
            p = os.path.join(s.dir, "r.%d.%d.mca" % (rx, rz))
            s.regs[(rx, rz)] = anvil.Region(p) if os.path.exists(p) else None
        return s.regs[(rx, rz)]

    def chunk(s, cx, cz):
        reg = s.region(cx >> 5, cz >> 5)
        return None if reg is None else reg.chunk(cx & 31, cz & 31)

    def chunks(s):
        """Every (cx, cz) this world has a region file for."""
        for name in sorted(os.listdir(s.dir)):
            if not name.endswith(".mca"):
                continue
            _, rx, rz, _ = name.split(".")
            rx, rz = int(rx), int(rz)
            reg = s.region(rx, rz)
            if reg is None:
                continue
            for i in range(1024):
                cx, cz = rx * 32 + (i & 31), rz * 32 + (i >> 5)
                if reg.chunk(cx & 31, cz & 31) is not None:
                    yield cx, cz

    def box(s, x0, x1, y0, y1, z0, z1, skip_air=True):
        """{(x,y,z): (name, properties)} over an inclusive box."""
        out = {}
        for cx in range(x0 >> 4, (x1 >> 4) + 1):
            for cz in range(z0 >> 4, (z1 >> 4) + 1):
                ch = s.chunk(cx, cz)
                if ch is None:
                    continue
                for sy, pal, idx in sections(ch):
                    base = sy * 16
                    if base > y1 or base + 15 < y0:
                        continue
                    for i, v in enumerate(idx):
                        y = base + (i >> 8)
                        if y < y0 or y > y1:
                            continue
                        x = cx * 16 + (i & 15)
                        z = cz * 16 + ((i >> 4) & 15)
                        if x < x0 or x > x1 or z < z0 or z > z1:
                            continue
                        e = pal[v]
                        nm = e.get("Name", "")
                        nm = nm[10:] if nm.startswith("minecraft:") else nm
                        if skip_air and nm in AIR:
                            continue
                        out[(x, y, z)] = (nm, e.get("Properties") or {})
        return out

    def block_entities(s, x0, x1, y0, y1, z0, z1):
        """The chests and signs, which are not in block_states at all."""
        out = []
        for cx in range(x0 >> 4, (x1 >> 4) + 1):
            for cz in range(z0 >> 4, (z1 >> 4) + 1):
                ch = s.chunk(cx, cz)
                if ch is None:
                    continue
                for be in ch.get("block_entities", []):
                    if (x0 <= be.get("x", 0) <= x1 and y0 <= be.get("y", 0) <= y1
                            and z0 <= be.get("z", 0) <= z1):
                        out.append(be)
        return out
