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
EYES = [
	# (white pixel, green pixel) — outer to inner, so the pupil goes on the
	# green one and the white stays as the corner of the eye it already is.
	((9, 14), (10, 14)),
	((14, 14), (13, 14)),
]

# The iris he is given, and it is the villager's own green.
#
# Not red and not white. White is HIS, and the whole mod's colour grammar rests
# on that; red is what a possessed animal wears when it is about to hurt you.
# This is neither — it is a villager, with a villager's eyes, and the only thing
# wrong with them is that there is something looking back out. Keeping the
# vanilla green is what makes the pupil the entire message.
IRIS = (56, 148, 56, 255)
WHITE = (238, 238, 238, 255)
PUPIL = (8, 8, 8, 255)


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

	for white, green in EYES:
		block(big, white, WHITE)
		block(big, green, IRIS)
		# THE PUPIL, dead centre of the iris and half its width. Two by two out
		# of four by four: any bigger and the eye is simply black at three
		# blocks, which reads as a hole rather than as somebody looking at you.
		pupil(big, green)

	path = os.path.join(OUT, 'villager.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d' % ('turned/villager', width * SCALE, height * SCALE))


def block(px, at, colour):
	x0, y0 = at[0] * SCALE, at[1] * SCALE
	for y in range(y0, y0 + SCALE):
		for x in range(x0, x0 + SCALE):
			px[y][x] = colour


def pupil(px, at):
	x0, y0 = at[0] * SCALE + SCALE // 4, at[1] * SCALE + SCALE // 4
	for y in range(y0, y0 + SCALE // 2):
		for x in range(x0, x0 + SCALE // 2):
			px[y][x] = PUPIL


if __name__ == '__main__':
	main()
