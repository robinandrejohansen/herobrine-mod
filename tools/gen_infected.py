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

  TORN OPEN.     The shirt is cut away to a dark cavity with a ragged edge, and
                 the ribs are laid INSIDE it. Painting bone straight onto the
                 cloth looks like stripes on a jumper; bone only reads as bone
                 when there is a hole for it to be inside.

  A MOUTH.       Vanilla leaves the lower face blank — eyes and nothing else.
                 An open jaw with a couple of teeth is the whole difference
                 between a face and a mask.

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

BONE = (206, 199, 180, 255)
# Dark enough to read as the gap BETWEEN two ribs. At a lighter value the
# three bands merge into one pale block and it stops looking like a ribcage.
BONE_DARK = (48, 40, 35, 255)
# A cavity, not a shadow. Near black, so bone inside it reads as depth.
CAVITY = (26, 20, 18, 255)
BLOOD = (78, 24, 20, 255)
# Fresh at the mouth, where it is still coming from. Everything else on the
# body is old and dry — the contrast is what makes the mouth read as wet.
WET = (122, 26, 24, 255)
BRAIN = (146, 78, 80, 255)
TOOTH = (198, 192, 176, 255)

# THE SHIRT IS TORN OPEN, THE BONE IS NOT PAINTED ON IT.
#
# The first attempt drew pale ribs straight over the shirt, which looks exactly
# like what it was: stripes on a jumper. Bone only reads as bone if there is a
# hole for it to be inside — so the cloth is cut away to a dark cavity first,
# with a ragged edge rather than a rectangle, and the ribs are laid in the hole
# afterwards with a shadow line under each. Order matters more than colour.
#
# Body front is (20, 20), eight wide and twelve tall.
TEAR = [(22, 24, 4, 1), (21, 25, 6, 1), (21, 26, 6, 1), (22, 27, 5, 1), (23, 28, 3, 1)]
RIBS = [(22, 25, 4, 1), (23, 27, 3, 1)]
RIB_SHADOW = [(22, 26, 4, 1)]

# Where the arm was. The body's left face is at (28, 20).
STUMP = [(28, 20, 3, 1)]
STUMP_RAW = [(28, 21, 3, 2)]

# Head front is (8, 8). Vanilla leaves the lower face blank — eyes at row 12
# and nothing else — which is why it read as a mask.
#
# The mouth is open and full. A dark rim so it reads as a hole rather than a
# painted shape, wet red inside it, a couple of teeth left, and blood over the
# chin — which then keeps going down the chest, because blood does not stop at
# the edge of a texture and the two faces meet at the neck.
MOUTH_RIM = [(10, 13, 4, 1)]
MOUTH_WET = [(11, 14, 2, 1), (10, 15, 4, 1)]
MOUTH_DEEP = [(10, 14, 1, 1), (13, 14, 1, 1)]
TEETH = [(11, 13, 1, 1), (12, 13, 1, 1)]

# Down the front, from the chin. Uneven lengths — a run of even streaks looks
# like a pattern, and this wants to look like it happened.
DRIPS = [(22, 20, 1, 6), (24, 20, 1, 3), (23, 20, 1, 2), (25, 21, 1, 2), (21, 20, 1, 1)]

# Head top is (8, 0).
SKULL = [(10, 2, 4, 3), (11, 1, 2, 1)]

STAINS = [(24, 29, 2, 1), (20, 31, 2, 1), (5, 22, 2, 3), (6, 27, 2, 2), (26, 22, 1, 3)]


def speckle(px, width, height):
	"""One shade of movement across the painted areas, deterministically."""
	touched = (BONE, BONE_DARK, CAVITY, BLOOD, WET, BRAIN, TOOTH)
	for y in range(height):
		for x in range(width):
			px_at = px[y][x]
			if px_at[3] == 0 or px_at not in touched:
				continue
			# A fixed checker rather than a random roll, so the texture is the
			# same every time it is generated and can be diffed.
			shift = -9 if (x * 7 + y * 13) % 3 == 0 else 6 if (x + y) % 4 == 0 else 0
			if shift:
				r, g, b, a = px_at
				px[y][x] = (max(0, min(255, r + shift)), max(0, min(255, g + shift)),
				            max(0, min(255, b + shift)), a)


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

	# Cut the cloth away first, then put the bone in the hole. Reversing these
	# two is the difference between a wound and a jumper with stripes on it.
	paint(px, TEAR, CAVITY, width, height)
	paint(px, RIB_SHADOW, BONE_DARK, width, height)
	paint(px, RIBS, BONE, width, height)

	paint(px, STUMP_RAW, BLOOD, width, height)
	paint(px, STUMP, BONE, width, height)

	paint(px, MOUTH_RIM, CAVITY, width, height)
	paint(px, MOUTH_DEEP, CAVITY, width, height)
	paint(px, MOUTH_WET, WET, width, height)
	paint(px, TEETH, TOOTH, width, height)
	paint(px, DRIPS, BLOOD, width, height)
	paint(px, SKULL, BRAIN, width, height)
	paint(px, STAINS, BLOOD, width, height)

	# A pixel of variation over everything that was painted flat. Four blocks
	# of identical colour read as a sticker; the same four with one shade of
	# movement in them read as a surface, which is the whole difference between
	# pixel art and a coloured rectangle.
	speckle(px, width, height)

	out = os.path.join(OUT, 'zombie.png')
	pngio.write(out, width, height, px)
	os.remove(scratch)
	print('infected/zombie.png  %dx%d' % (width, height))


if __name__ == '__main__':
	main()
