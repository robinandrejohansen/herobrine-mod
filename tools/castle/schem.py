#!/usr/bin/env python3
"""Read a Sponge .schem and write the blueprint JSON the mod loads.

    python3 tools/castle/schem.py thing.schem thing.json [--ground 0]

WHY A CONVERTER AND NOT A READER IN THE MOD. The mod already has a blueprint
format and something that places it; a second format inside the jar would be a
second thing to keep working. Everything version-specific stays here, in a script
that is run once per building on a machine that can be fixed.

THE FORMAT, and the one part of it that is not like Anvil:

    Version      1, 2 or 3
    Width Height Length     shorts
    Palette      { "minecraft:stone": 0, ... }   name -> index
    BlockData    a VARINT STREAM of palette indices, YZX order

Anvil packs fixed-width indices into longs. Sponge writes varints — one byte per
index below 128, two below 16384, and so on. A fixed-width reader produces
plausible garbage rather than an error, which is the worst kind of wrong, so the
decoder below is written to run off the end rather than to trust a count.

Version 3 moved the palette and data down a level, into a `Blocks` compound, and
left everything else where it was. Both shapes are handled.

Air is dropped. The mod's format stores non-air only and clears the box first —
see Blueprint.stand.
"""

import gzip
import json
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import anvil


def varints(data):
    """The BlockData stream -> palette indices."""
    out, i, n = [], 0, len(data)
    while i < n:
        value, shift = 0, 0
        while True:
            if i >= n:
                return out                 # truncated stream; keep what we read
            b = data[i] & 0xFF
            i += 1
            value |= (b & 0x7F) << shift
            if not (b & 0x80):
                break
            shift += 7
            if shift > 35:
                raise ValueError("varint longer than five bytes at %d" % i)
        out.append(value)
    return out


def read(path):
    with open(path, "rb") as f:
        raw = f.read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    root = anvil.parse(raw)
    # Some writers wrap the whole thing in a "Schematic" compound.
    if "Schematic" in root and isinstance(root["Schematic"], dict):
        root = root["Schematic"]
    return root


def convert(path, ground=0):
    root = read(path)
    version = root.get("Version", 1)
    w = root.get("Width")
    h = root.get("Height")
    l = root.get("Length")

    if "Blocks" in root and isinstance(root["Blocks"], dict):
        block = root["Blocks"]                     # version 3
        pal_in = block.get("Palette", {})
        data = block.get("Data", b"")
    else:
        pal_in = root.get("Palette", {})
        data = root.get("BlockData", b"")

    if not pal_in:
        raise SystemExit(
            "%s has no Palette. Version %s. This is probably the OLD MCEdit\n"
            ".schematic format, which stores numeric block ids and needs a\n"
            "1.12-to-1.13 mapping table this script does not carry." % (path, version))

    # index -> "name[props]", stripped of the namespace the mod re-adds
    by_index = {}
    for name, idx in pal_in.items():
        by_index[int(idx)] = name.replace("minecraft:", "", 1)

    indices = varints(bytes(data))
    want = w * h * l
    if len(indices) != want:
        print("  note: %d indices for a %dx%dx%d box (%d expected)"
              % (len(indices), w, h, l, want), file=sys.stderr)

    pal, seen, rows = [], {}, []
    for i, v in enumerate(indices):
        spec = by_index.get(v)
        if spec is None or spec == "air" or spec.startswith("air["):
            continue
        # YZX, which is the order the format writes and not the one Anvil uses.
        y = i // (w * l)
        rest = i % (w * l)
        z = rest // w
        x = rest % w
        if spec not in seen:
            seen[spec] = len(pal)
            pal.append(spec)
        rows.append((x, y, z, seen[spec]))

    return {
        "source": os.path.basename(path),
        "size": {"x": w, "y": h, "z": l},
        "ground": ground,
        "palette": pal,
        "blocks": rows,
    }


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    src, dst = sys.argv[1], sys.argv[2]
    ground = 0
    if "--ground" in sys.argv:
        ground = int(sys.argv[sys.argv.index("--ground") + 1])
    out = convert(src, ground)
    with open(dst, "w") as f:
        json.dump(out, f, separators=(",", ":"))
    print("%s -> %s" % (os.path.basename(src), os.path.basename(dst)))
    print("  %d x %d x %d,  %d blocks,  %d palette entries,  %.2f MB"
          % (out["size"]["x"], out["size"]["y"], out["size"]["z"],
             len(out["blocks"]), len(out["palette"]),
             os.path.getsize(dst) / 1048576))
    if ground == 0:
        print("  ground is 0 — pass --ground N to say which layer sits at the surface")


if __name__ == "__main__":
    main()
