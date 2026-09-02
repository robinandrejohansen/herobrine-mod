#!/usr/bin/env python3
"""Addexio: the one who walks with you, painted from his own skin.

HE IS THE ONLY THING IN THIS MOD YOU MUST NEVER MISIDENTIFY, and that is a
harder requirement than "looks nice". A player who cannot tell him from a
Turned at twenty blocks will kill him, or worse, will hesitate over one of them
at the wrong moment. So the design starts from what he must not resemble:

    ordinary villagers   brown robe, big nose, no arms to speak of
    the Turned           the same robe, drained ashen grey
    the Gaunt            the same robe again, three blocks tall
    Herobrine            Steve's cyan shirt and blue trousers

Every one of those is a ROBE. He is the only human-shaped thing on your side of
the world: separate arms, a pale tunic, a strap across the chest, boots. The
silhouette does the identifying before any colour does, which is what works at
distance and in the dark.

HE USED TO BE A VILLAGER AND WAS CALLED VERA. That version leaned the other way
— an ordinary villager in a red coat, on the argument that she should be utterly
unremarkable up close so that the day something wore her face there was nothing
to spot. Red was chosen because it was the one hue nothing else in the mod used.
It worked, and it cost him a face: a villager head has a nose the size of a fist
and no expression, and a companion you are supposed to care about cannot be a
trade menu with legs.

A NAME IS WORTH MORE THAN A DISGUISE. He is called Addexio now and the nameplate
says so, which also makes the mimic worse rather than better: a plate reading
Addexio over the wrong thing is a far colder moment than an unnamed villager
standing where one should not be.

THE LAYOUT IS THE 1.8 PLAYER SHEET, 64x64, because that is what the reference
is. Written out rather than derived: a part's four side faces share the same
rows, so painting a horizontal band across the whole 16-wide block paints front,
back and both sides in one go and cannot get them out of step.

    head      ( 0, 0) 32x16      right leg ( 0,16) 16x16
    hat       (32, 0) 32x16      body      (16,16) 24x16
    left arm  (32,48) 16x16      right arm (40,16) 16x16
    left leg  (16,48) 16x16      every  L2 block  transparent

Run:  python3 tools/gen_addexio.py [--preview]
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'addexio')
W = H = 64
SCALE = 4                  # to 256x256, so he matches the Turned on screen

CLEAR = (0, 0, 0, 0)

# ---- MEASURED OFF THE REFERENCE, not invented.
HAIR = (74, 50, 30, 255)
HAIR_LIT = (108, 76, 46, 255)
SKIN = (198, 154, 118, 255)
SKIN_LOW = (170, 128, 96, 255)
BEARD = (104, 72, 44, 255)
BEARD_LOW = (80, 54, 32, 255)
EYE_WHITE = (234, 238, 242, 255)
EYE_BLUE = (86, 122, 190, 255)
BROW = (60, 40, 24, 255)

TUNIC = (198, 198, 196, 255)
TUNIC_LOW = (170, 170, 168, 255)
SLEEVE = (150, 150, 148, 255)
STRAP = (56, 54, 52, 255)
STRAP_LOW = (36, 34, 32, 255)

TROUSER = (112, 78, 50, 255)
TROUSER_LOW = (92, 62, 38, 255)
CUFF = (168, 92, 66, 255)      # the rust band at the wrist, on the side views
BOOT = (26, 24, 22, 255)


def blank():
	return [[CLEAR for _ in range(W)] for _ in range(H)]


def fill(px, u, v, w, h, colour):
	for y in range(v, v + h):
		for x in range(u, u + w):
			px[y][x] = colour


def speckle(px, u, v, w, h, base, lit):
	"""Two-tone, deterministically. His hair is messy and a flat brown is a cap.

	Hashed off the coordinate rather than random, so the sheet is byte-identical
	every run — a generator whose output moves cannot be diffed, and this one is
	checked into the repository.
	"""
	for y in range(v, v + h):
		for x in range(u, u + w):
			px[y][x] = lit if ((x * 7 + y * 13) ^ (x * y)) % 3 == 0 else base


def band(px, u, w, rows, colour):
	"""A horizontal band across a whole part block: all four sides at once."""
	for y in rows:
		for x in range(u, u + w):
			px[y][x] = colour


def head(px):
	# The crown and the back, all hair. Rows 8-10 of the sides are the hairline.
	speckle(px, 8, 0, 8, 8, HAIR, HAIR_LIT)            # top
	band(px, 0, 32, range(8, 11), HAIR)
	speckle(px, 0, 8, 32, 3, HAIR, HAIR_LIT)
	band(px, 0, 32, range(11, 16), SKIN)               # the rest is face and neck
	speckle(px, 24, 8, 8, 6, HAIR, HAIR_LIT)           # the back is hair further down
	fill(px, 16, 0, 8, 8, SKIN_LOW)                    # under the jaw

	# ---- THE FACE, x 8..15, rows 8..15, and it is the only part written pixel
	#      by pixel. Everything else on this sheet is bands.
	f = 8
	# brow line first, so the eyes sit under something...
	band(px, f, 8, range(11, 12), SKIN)
	for x in (f + 1, f + 2, f + 5, f + 6):
		px[11][x] = BROW
	# ...and the sideburns AFTER it. Painted before, the brow band ran straight
	# over them and the hair stopped dead at the hairline on both sides, which
	# reads as a cap rather than as hair.
	for y in (11, 12):
		px[y][f] = HAIR
		px[y][f + 7] = HAIR
	# eyes on row 12: white outside, blue in. Two pixels each, wide-set.
	px[12][f + 1] = EYE_WHITE
	px[12][f + 2] = EYE_BLUE
	px[12][f + 5] = EYE_BLUE
	px[12][f + 6] = EYE_WHITE
	# the moustache, then the beard across the jaw and chin
	band(px, f, 8, range(13, 14), SKIN)
	for x in range(f + 2, f + 6):
		px[13][x] = BEARD
	px[13][f] = BEARD_LOW
	px[13][f + 7] = BEARD_LOW
	band(px, f, 8, range(14, 16), BEARD)
	for x in range(f + 3, f + 5):
		px[14][x] = BEARD_LOW                          # the mouth, in shadow
	px[15][f] = BEARD_LOW
	px[15][f + 7] = BEARD_LOW


def body(px):
	u = 16
	fill(px, 20, 16, 8, 4, TUNIC)                      # shoulders
	fill(px, 28, 16, 8, 4, TUNIC_LOW)                  # underside
	band(px, u, 24, range(20, 32), TUNIC)
	# a seam down the sides, so a flat tunic has an edge
	for y in range(20, 32):
		px[y][u] = TUNIC_LOW
		px[y][u + 12] = TUNIC_LOW
		px[y][u + 23] = TUNIC_LOW

	# ---- THE STRAP, and it has to go all the way round or it is a stripe.
	#
	# Front from the left shoulder down to the right hip, back mirrored, and the
	# two joined over the shoulder on the top face. Drawn two pixels wide because
	# one pixel of dark on a pale tunic reads as a scratch.
	for i in range(8):
		y = 21 + i
		for x in (20 + i, 21 + i):
			if x < 28 and y < 32:
				px[y][x] = STRAP if x == 20 + i else STRAP_LOW
	for i in range(8):
		y = 21 + i
		for x in (39 - i, 38 - i):
			if 32 <= x < 40 and y < 32:
				px[y][x] = STRAP if x == 39 - i else STRAP_LOW
	# over the shoulder
	for x in range(20, 22):
		fill(px, x, 16, 1, 4, STRAP)


def arm(px, u, v):
	fill(px, u + 4, v, 4, 4, TUNIC)                    # shoulder cap
	fill(px, u + 8, v, 4, 4, SKIN_LOW)                 # the palm
	band(px, u, 16, range(v + 4, v + 14), SLEEVE)      # sleeve
	band(px, u, 16, range(v + 14, v + 15), CUFF)       # the rust band at the wrist
	band(px, u, 16, range(v + 15, v + 16), SKIN)       # and the hand
	for y in range(v + 4, v + 14):
		px[y][u] = TUNIC_LOW


def leg(px, u, v):
	fill(px, u + 4, v, 4, 4, TROUSER)                  # hip
	fill(px, u + 8, v, 4, 4, BOOT)                     # the sole
	band(px, u, 16, range(v + 4, v + 14), TROUSER)
	band(px, u, 16, range(v + 14, v + 16), BOOT)
	for y in range(v + 4, v + 14):
		px[y][u] = TROUSER_LOW
		px[y][u + 12] = TROUSER_LOW


def preview(px):
	key = [(HAIR, '#'), (HAIR_LIT, '%'), (SKIN, '.'), (SKIN_LOW, ','),
	       (BEARD, 'b'), (BEARD_LOW, 'B'), (EYE_WHITE, 'O'), (EYE_BLUE, 'o'),
	       (BROW, '^'), (TUNIC, '-'), (TUNIC_LOW, '='), (SLEEVE, '~'),
	       (STRAP, 'S'), (STRAP_LOW, 's'), (TROUSER, 'r'), (TROUSER_LOW, 'R'),
	       (CUFF, 'c'), (BOOT, 'X')]
	look = {c: m for c, m in key}
	print('\n    his face, head front (x 8..15, rows 8..15):\n')
	for y in range(8, 16):
		print('      %2d  %s' % (y, ' '.join(look.get(px[y][x], '?')
		                                     for x in range(8, 16))))
	print('\n    the body front (x 20..27, rows 16..31):\n')
	for y in range(16, 32):
		print('      %2d  %s' % (y, ' '.join(look.get(px[y][x], '?')
		                                     for x in range(20, 28))))
	print('\n      # hair  . skin  b beard  O/o eye  - tunic  S strap'
	      '  ~ sleeve  r trouser  X boot\n')


def main():
	os.makedirs(OUT, exist_ok=True)
	px = blank()
	head(px)
	body(px)
	arm(px, 40, 16)          # right arm
	arm(px, 32, 48)          # left arm
	leg(px, 0, 16)           # right leg
	leg(px, 16, 48)          # left leg
	# The hat and every second layer stay transparent. He wears no hood — the
	# thing that wears hoods in this mod is not on your side.

	big = [[px[y // SCALE][x // SCALE] for x in range(W * SCALE)]
	       for y in range(H * SCALE)]
	path = os.path.join(OUT, 'addexio.png')
	pngio.write(path, W * SCALE, H * SCALE, big)
	drawn = sum(1 for row in px for c in row if c[3] != 0)
	print('%-22s %dx%d   %d of %d pixels painted, %d left clear'
	      % ('addexio/addexio', W * SCALE, H * SCALE, drawn, W * H, W * H - drawn))
	if '--preview' in sys.argv:
		preview(px)


if __name__ == '__main__':
	main()
