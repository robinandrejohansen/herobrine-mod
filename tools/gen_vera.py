#!/usr/bin/env python3
"""Vera: an ordinary villager in a red coat.

SHE IS THE ONLY THING IN THIS MOD YOU MUST NEVER MISIDENTIFY, and that is a
harder requirement than "looks nice". A player who cannot tell her from a
Turned at twenty blocks will kill her, or worse, will hesitate over one of them
at the wrong moment. So the design starts from what she must not resemble:

    ordinary villagers   brown robe, plains type
    the Turned           the same brown, drained ashen grey
    the Gaunt            the same brown again, three blocks tall
    Herobrine            Steve's cyan shirt and blue trousers

Everything in the mod is brown, grey, or cyan. Nothing is red. So she is red —
not because red suits her, because red is the one hue left that cannot be
confused with anything already walking around.

HOW THE RED IS MADE. Not a flood fill. Every pixel of the plains robe keeps its
own LUMINANCE and only its hue is moved, so all of vanilla's shading — the fold
down the front, the darker hem, the seam at the shoulder — survives intact. A
flat red villager reads as a texture error; this reads as a coat.

WHAT IS LEFT ALONE. The head, entirely. Her face is a completely ordinary
villager's face: the same skin, the same green eyes, the same nose at the same
size. That is the point of her. The Turned needed a swollen nose and a black
pupil to be wrong; Vera needs to be RIGHT, so that the day something copies her
you have nothing to go on but behaviour.

Rows 0 to 17 are the head cube and its hat, 18 and 19 are the blank seam, and
the body, arms and legs start at 20 — read off the file, the same split
gen_turned.py uses. Below that line, anything darker than luminance 100 is
cloth; anything lighter is the skin of her hands, and it is skipped.
"""

import os
import glob
import zipfile
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'vera')
SCALE = 4
HEAD_ROWS = 18
CLOTH_UNDER = 100          # luminance. above this, in the body, is her hands.

# The hue she is moved to, as multipliers on luminance. Checked against the real
# palette: the robe's commonest tone is luminance 91 and lands on (141, 47, 40),
# the hem's 46 lands on (71, 24, 20). A deep oxblood with a dark rust in the
# creases, which is what a coat somebody has been wearing underground looks like.
RED = (1.55, 0.52, 0.44)


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def over(top, bottom):
	if top[3] == 0:
		return bottom
	if top[3] == 255 or bottom[3] == 0:
		return top
	a = top[3] / 255.0
	return tuple([int(top[i] * a + bottom[i] * (1 - a)) for i in range(3)]
	             + [max(top[3], bottom[3])])


def lum(c):
	return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def reddened(c):
	light = lum(c)
	return tuple([min(255, int(light * m)) for m in RED] + [c[3]])


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

	cloth = skin = 0
	for y in range(HEAD_ROWS, height):
		for x in range(width):
			c = base[y][x]
			if c[3] == 0:
				continue
			if lum(c) >= CLOTH_UNDER:
				skin += 1                  # her hands. left as they are.
				continue
			base[y][x] = reddened(c)
			cloth += 1

	# Nearest-neighbour, at the Turned's scale so the two match on screen. The
	# model's UVs are fractions, so a 256x256 sheet lands on the same mesh.
	big = [[base[y // SCALE][x // SCALE]
	        for x in range(width * SCALE)] for y in range(height * SCALE)]

	path = os.path.join(OUT, 'vera.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d   %d cloth pixels recoloured, %d skin left alone'
	      % ('vera/vera', width * SCALE, height * SCALE, cloth, skin))


if __name__ == '__main__':
	main()
