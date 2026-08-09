#!/usr/bin/env python3
"""Herobrine's skin: vanilla Steve, with the eyes taken out.

    python3 tools/gen_herobrine.py

THIS IS THE WHOLE CHARACTER AND IT HAS TO BE EXACT. Every account of him from
2010 onward says the same thing — it is the default player, and the eyes are
wrong. Not a similar figure, not a pale one, not a designed villain: Steve,
who you have looked at for a decade, with nothing behind the eyes.

The previous version was hand-painted to look like Steve, which is a subtly
different and much weaker idea. A player who half-recognises a skin thinks "who
is that"; a player who recognises it exactly thinks "that is me", and only then
notices the eyes. The uncanniness is entirely in the gap between those two
thoughts, and a hand-drawn approximation closes it.

So the base is lifted from the client jar at build time rather than redrawn.
It is the same file the game uses for a player with no skin, which means it
tracks Mojang's own art and cannot drift away from it.

Two textures come out, and the split is required by the renderer: the eyes are
drawn by a second pass that ignores world lighting, so they have to live in
their own file with everything else transparent.
"""
import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
# pngio rather than skinlib for the read: Mojang ship Steve as a PALETTED png
# (colour type 3) and skinlib only handles RGB and RGBA, so it asserts out. The
# write side stays on skinlib, which is what every other texture here uses.
import pngio
from skinlib import write_png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "herobrine",
                   "textures", "entity")
STEVE = "assets/minecraft/textures/entity/player/wide/steve.png"

# The eye pixels on the head's front face. Steve's eyes are two pixels wide and
# one tall, at rows 12 and 13 of the 64x64 layout — the whites at row 12 and the
# irises at 13. Both go, because a white eye with a brown iris still reads as a
# person looking at something.
EYES = [(9, 12), (10, 12), (13, 12), (14, 12),
        (9, 13), (10, 13), (13, 13), (14, 13)]

# What replaces them in normal light: not white, but the dark of a socket. The
# glow layer is what makes them read as lit, and if the base were white too they
# would look like paint in daylight rather than like something behind the face.
SOCKET = (20, 16, 12, 255)
# And the emissive pass. The alpha is the intensity dial rather than a
# transparency: RenderTypes.eyes() blends with TRANSLUCENT, so this is the
# difference between eyes that are lit and eyes that are LAMPS.
EYE = (255, 255, 255, 240)
HALO = (232, 236, 240, 60)


def client_jar():
    jars = glob.glob(os.path.expanduser(
        "~/.gradle/caches/fabric-loom/**/minecraft-client.jar"), recursive=True)
    if not jars:
        raise SystemExit("no client jar; run ./gradlew genSources first")
    return zipfile.ZipFile(jars[0])


def main():
    os.makedirs(OUT, exist_ok=True)
    scratch = os.path.join(OUT, ".steve.png")
    with client_jar() as jar, open(scratch, "wb") as handle:
        handle.write(jar.read(STEVE))

    w, h, lit = pngio.read(scratch)
    os.remove(scratch)
    if (w, h) != (64, 64):
        raise SystemExit(f"expected a 64x64 skin, got {w}x{h}")

    glow = [[(0, 0, 0, 0)] * w for _ in range(h)]

    for x, y in EYES:
        lit[y][x] = SOCKET
        glow[y][x] = EYE

    # One pixel of bleed, up, down and outward only — never into the gap between
    # the eyes, or the two halos meet and the whole thing reads as a lit band
    # across the face rather than as eyes. Same rule as the old generator; it
    # was the one part of it worth keeping.
    sockets = set(EYES)
    halo = set()
    for x, y in EYES:
        halo.add((x, y - 1))
        halo.add((x, y + 1))
        for step in (-1, 1):
            if not any((x + step * n, y) in sockets for n in (1, 2, 3)):
                halo.add((x + step, y))

    for x, y in sorted(halo):
        # Everything here is inside the head's front face, 8..15 in both axes.
        # Asserted rather than trusted: a halo leaking onto the head's side
        # would show as a glowing stripe down his temple and look deliberate.
        if not (8 <= x <= 15 and 8 <= y <= 15):
            raise SystemExit(f"halo at ({x},{y}) is off the face")
        if (x, y) not in sockets and glow[y][x][3] == 0:
            glow[y][x] = HALO

    write_png(os.path.join(OUT, "herobrine.png"), lit)
    write_png(os.path.join(OUT, "herobrine_eyes.png"), glow)
    print(f"wrote herobrine.png + herobrine_eyes.png from vanilla Steve "
          f"({len(EYES)} eye px, {len(halo)} halo px)")


if __name__ == "__main__":
    main()
