#!/usr/bin/env python3
"""The skin of the one who does not sleep.

He has to be indistinguishable from the villager standing next to him at any
distance, and unmistakable close up. That rules out a new outfit, a new colour
and anything on the body — the whole point of him is that you walked past him
in the square this morning. Everything is in the eyes.

WHICH IS WHY IT IS UPSCALED. A vanilla villager eye is TWO PIXELS: one white,
one green, side by side, and one pixel tall. There is no middle of a one-pixel
eye to put anything in, so the requested black dot is not a texture edit at
64x64 — it is arithmetically impossible. At 4x the same eye is eight by four,
which is enough for a white surround, a green iris and a two-by-two pupil in the
centre of it.

Upscaling costs nothing here because it is our own texture on our own renderer.
The model's UVs are fractions, so a 256x256 file maps onto the villager mesh
exactly as the 64x64 one does — every other pixel is the vanilla art, blown up
with nearest-neighbour so it stays blocky and matches the villagers around him.

The base is villager.png with type/plains.png composited over it, which is what
vanilla does at render time. No profession layer, deliberately: he has no trade
and right-clicking him opens nothing, and that absence is the second tell after
the eyes.

Run:  python3 tools/gen_turned.py
"""
import os
import sys
import zipfile
import glob

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'turned')

SCALE = 4

# READ OFF THE VANILLA TEXTURE, NOT GUESSED — the same discipline as the
# possessed eyes, and for the same reason: every set of coordinates anybody has
# eyeballed for this mod has been wrong.
#
# The villager head cube puts its front face at x 8..15, y 8..15. Scanning that
# region for the brow line finds a solid black row at y=13, and the row under it
# is the eyes: white at x=9, green at x=10, then a gap for the nose, green at
# x=13, white at x=14. Two pixels per eye, one row tall.
# TWO BY TWO, NOT TWO BY ONE.
#
# A villager's eye is two pixels wide and ONE tall, and at that height it reads
# as a slit however white it is — which is why the last pass still looked like an
# ordinary villager with a dot in it. The eye has to be a SQUARE before any of
# this registers from across a field.
#
# AND IT GROWS DOWNWARD, NOT UP. The first attempt took the brow row at y=13 to
# buy the height, and the reference says otherwise: the dark brow is still there
# above the eyes, and the white starts under it and runs down toward the nose.
# That is the whole face in the photograph — a heavy line, then two white squares
# hanging off it. Eating the brow removes the thing the eyes are hanging from.
#
# Top-left of a two-by-two, in base pixels. Row 14 is the eye row vanilla draws;
# 15 is the cheek under it, which is what gets taken instead.
EYES = [(9, 14), (13, 14)]

# The iris he is given, and it is the villager's own green.
#
# Not red and not white. White is HIS, and the whole mod's colour grammar rests
# on that; red is what a possessed animal wears when it is about to hurt you.
# This is neither — it is a villager, with a villager's eyes, and the only thing
# wrong with them is that there is something looking back out. Keeping the
# vanilla green is what makes the pupil the entire message.
IRIS = (56, 148, 56, 255)
WHITE = (238, 238, 238, 255)
# GREEN, not black. A black pupil reads as a doll — the eye is dead and painted
# on. A saturated green one reads as LIT, which is the difference between a
# thing that has been made and a thing that is looking at you. Picked off the
# reference: white sclera the full width of the socket, and a two-by-two of
# something switched on in the middle of it.
PUPIL = (54, 214, 84, 255)


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def over(top, bottom):
	"""Straight alpha compositing, which is all vanilla's layers ever need."""
	if top[3] == 0:
		return bottom
	if top[3] == 255 or bottom[3] == 0:
		return top
	a = top[3] / 255.0
	return tuple([int(top[i] * a + bottom[i] * (1 - a)) for i in range(3)]
	             + [max(top[3], bottom[3])])


def main():
	os.makedirs(OUT, exist_ok=True)
	jar = client_jar()
	scratch = os.path.join(OUT, '.vanilla.png')

	base = None
	width = height = 0
	for part in ('villager.png', 'type/plains.png'):
		with open(scratch, 'wb') as f:
			f.write(jar.read('assets/minecraft/textures/entity/villager/' + part))
		width, height, px = pngio.read(scratch)
		if base is None:
			base = px
			continue
		base = [[over(px[y][x], base[y][x]) for x in range(width)]
		        for y in range(height)]
	os.remove(scratch)

	# Nearest-neighbour, so it is still Minecraft. Anything smoothed would make
	# him the one soft-edged person in the village, which is the exact opposite
	# of the effect.
	big = [[base[y // SCALE][x // SCALE] for x in range(width * SCALE)]
	       for y in range(height * SCALE)]

	for eye in EYES:
		# THE WHOLE SQUARE WHITE, AND THE GREEN GOES IN THE MIDDLE OF IT.
		#
		# It used to paint the inner cell as a solid green iris with the pupil
		# inside it, which is how a villager's eye is actually built — and it is
		# the wrong reference. The eye wanted here is the one from the photograph:
		# a wide WHITE eye with one small green square dead centre, so the white
		# is the whole shape and the green is a point inside it.
		#
		# The difference matters at distance. A half-green eye is a villager
		# squinting. A white eye with a dot in it is a pupil, and a pupil is the
		# only thing that reads as looking AT you from across a field.
		for dx in range(2):
			for dy in range(2):
				block(big, (eye[0] + dx, eye[1] + dy), WHITE)
		pupil_in(big, eye)

	path = os.path.join(OUT, 'villager.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d' % ('turned/villager', width * SCALE, height * SCALE))


def block(px, at, colour):
	x0, y0 = at[0] * SCALE, at[1] * SCALE
	for y in range(y0, y0 + SCALE):
		for x in range(x0, x0 + SCALE):
			px[y][x] = colour


def pupil_in(px, eye):
	"""A two-by-two, centred on the seam of the two cells that make one eye.

	Centred across the PAIR rather than inside one of them, because the eye is
	two base pixels wide and a pupil in either half is an eye looking sideways.
	Two by two out of the eight by four the pair covers: any bigger and it is a
	hole rather than somebody looking at you, any smaller and it is gone at three
	blocks, which is the range this has to work at.
	"""
	# Dead centre of the eight-by-eight the square covers, three across. Two was
	# right in a two-by-one eye and disappears in a two-by-two; four is half the
	# width and reads as a hole. Three is the largest thing that still looks like
	# a pupil rather than a missing pixel.
	x0 = eye[0] * SCALE + SCALE - 1
	y0 = eye[1] * SCALE + SCALE - 1
	for y in range(y0, y0 + 3):
		for x in range(x0, x0 + 3):
			px[y][x] = PUPIL


if __name__ == '__main__':
	main()
