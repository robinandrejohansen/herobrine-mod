#!/usr/bin/env python3
"""The infected: a zombie that has come apart.

Built from the vanilla zombie rather than drawn, for the same reason the
farmhouse books are written in a farm ledger's vocabulary — the player has to
recognise the shape before they notice what is wrong with it. A creature drawn
from scratch is a new monster. This is something that used to be a person and
has been in a cell a very long time.

Four changes, and the first one does most of the work:

  ONE ARM GONE.  The whole left-arm block of the texture is cleared to
                 transparency, so the model renders the cube and nothing shows
                 up in it. Minecraft gives that away for free and it is the
                 single most legible silhouette change available — you read it
                 from across a room, in the dark, before you read anything else.

  BONE THROUGH.  Pale ribs across the chest and a strip at the shoulder, where
                 the arm went.

  BRAIN.         The top of the skull opened. Only visible when it is below you
                 or you are standing over it, which is the right amount.

  BLOOD.         Old rather than fresh — dark, brown-red, and dry. Bright red
                 reads as a wound that just happened; this happened years ago.

Deliberately no glowing eyes. Those belong to him and to the animals he takes;
this is not being worn by anything, it is simply what is left.

Run:  python3 tools/gen_infected.py
"""
import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

SOURCE = 'assets/minecraft/textures/entity/zombie/zombie.png'
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'infected')

# The standard 64x64 humanoid layout. The left arm's whole block, base and
# overlay, so nothing of it is drawn at all.
LEFT_ARM = [(32, 48, 16, 16), (48, 48, 16, 16)]

BONE = (208, 202, 186, 255)
BLOOD = (86, 26, 22, 255)
BRAIN = (140, 74, 78, 255)

# Ribs across the chest, and the stump at the shoulder. Body front is at
# (20, 20) and is eight wide by twelve tall.
RIBS = [(21, 23, 6, 1), (21, 25, 5, 1), (22, 27, 4, 1), (26, 20, 2, 3)]
# Head top is (8, 0), eight by eight.
SKULL = [(10, 2, 4, 3), (11, 1, 2, 1)]
# Old blood: chest, hip, and down one leg. Leg front is at (4, 20).
STAINS = [(23, 29, 3, 2), (20, 31, 2, 1), (5, 22, 2, 3), (6, 27, 2, 2),
          (44, 22, 2, 4), (25, 21, 1, 2)]


def paint(px, boxes, colour, width, height):
	for bx, by, bw, bh in boxes:
		for y in range(by, min(height, by + bh)):
			for x in range(bx, min(width, bx + bw)):
				px[y][x] = colour


def main():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')

	os.makedirs(OUT, exist_ok=True)
	scratch = os.path.join(OUT, '.vanilla.png')
	with open(scratch, 'wb') as f:
		f.write(zipfile.ZipFile(jars[0]).read(SOURCE))

	width, height, px = pngio.read(scratch)

	# Darken and drain the whole thing first, so the additions sit on something
	# that already looks long dead rather than on a healthy green zombie.
	for y in range(height):
		for x in range(width):
			r, g, b, a = px[y][x]
			if a:
				px[y][x] = (int(r * 0.72), int(g * 0.66), int(b * 0.62), a)

	paint(px, LEFT_ARM, (0, 0, 0, 0), width, height)
	paint(px, RIBS, BONE, width, height)
	paint(px, SKULL, BRAIN, width, height)
	paint(px, STAINS, BLOOD, width, height)

	out = os.path.join(OUT, 'zombie.png')
	pngio.write(out, width, height, px)
	os.remove(scratch)
	print('infected/zombie.png  %dx%d' % (width, height))


if __name__ == '__main__':
	main()
