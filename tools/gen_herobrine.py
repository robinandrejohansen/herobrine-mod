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

# The eye pixels, and there are FOUR of them.
#
# Read off the texture rather than assumed, because the first version assumed
# and was wrong. Steve's face at row 12 is:
#
#     skin  WHITE  iris  skin  skin  iris  WHITE  skin
#
# so each eye is two pixels wide and ONE tall — the white and the iris sit side
# by side, not stacked. Row 13 is `E H H L L H H E`: that is his nose and his
# mouth, and the first attempt painted white over both. It read as a figure
# with enormous square eyes and no face under them, which is a monster, and he
# is specifically not one. He is the default player with nothing behind the
# eyes, and everything else about the face has to survive intact for that to
# land.
EYES = [(9, 12), (10, 12), (13, 12), (14, 12)]

# What replaces them in normal light: not white, but the dark of a socket. The
# glow layer is what makes them read as lit, and if the base were white too they
# would look like paint in daylight rather than like something behind the face.
SOCKET = (20, 16, 12, 255)
# And the emissive pass. FOUR PIXELS AND NOTHING ELSE.
#
# There was a ring of faint glow around them, added because four pixels at
# seventy blocks are hard to see. It worked and it was wrong: every image of
# him for fifteen years is Steve with two white eyes, flat, with no bloom and
# no aura, and the moment there is a halo he stops being a player with
# something missing and starts being a creature with powers.
#
# The alpha carries the distance instead. RenderTypes.eyes() blends with
# TRANSLUCENT and ignores world light, so at full opacity the eyes are the only
# thing on him that does not go dark at night — which is the same reason the
# original screenshot worked, and it needs no help.
EYE = (255, 255, 255, 255)


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

    write_png(os.path.join(OUT, "herobrine.png"), lit)
    write_png(os.path.join(OUT, "herobrine_eyes.png"), glow)
    print(f"wrote herobrine.png + herobrine_eyes.png from vanilla Steve "
          f"({len(EYES)} eye px, no halo)")


if __name__ == "__main__":
    main()
