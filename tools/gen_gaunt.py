#!/usr/bin/env python3
"""The tall one. The skin of the thing that is only ever closer.

A SECOND SKIN OFF THE SAME MAN, and everything about it is a decision to go the
other way from tools/gen_turned.py.

The Turned keeps his head its own colour, because the effect there is a man you
traded with this morning wearing clothes that have gone black — one half normal
is what makes the other half legible. This one is not a man you traded with. It
is the shape of one, stretched, at the treeline, and the read has to survive at
forty blocks in the rain with no light on it at all.

So the curve runs over EVERYTHING here, hood included, and then the FACE ALONE is
bleached back out to a cold off-white. A black figure with a pale face is the
oldest silhouette in this particular genre and it is old because it works: at
range the body disappears into the trunks and the face does not, so what a player
actually sees is a face, floating, at the wrong height.

The features come off. No brow line, no nose shading, no mouth — the villager
nose is a separate cube on the model and GauntRenderer shrinks it to almost
nothing, so the front of the head can be a flat plane with two eyes in it.

The eyes are the Turned's, moved up a row and given a shadow band above and
below. On a pale face a white socket has no contrast on its own; the band gives
it a rim, and a rimmed white eye on a blank face reads as sunken rather than
painted on. Same white, same small dark pupil, same reason — a pupil is the only
thing that reads as looking AT you from across a field.

Run:  python3 tools/gen_gaunt.py [--preview]
"""
import os
import sys
import glob
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'gaunt')

SCALE = 4

# The front face of the head cube, read off the vanilla sheet the same way
# gen_turned.py reads the eye row: x 8..15, y 8..15, eight by eight.
FACE_X0, FACE_X1 = 8, 15
FACE_Y0, FACE_Y1 = 8, 15

# EDGE TO EDGE, which is the whole look. Three wide starting at 8 and at 13
# covers 8,9,10 and 13,14,15 — both eyes touch the sides of the head and the
# only pale left between them is the two-pixel bridge the nose sits on.
LEFT = 8
RIGHT = 13
EYE_WIDE = 3
EYE_TALL = 2

# One row higher than the Turned's. The head is drawn stretched, so every row
# below the eyes is lengthened by the renderer — putting them high leaves a long
# blank jaw underneath, and a long blank jaw is most of what "gaunt" means.
EYE_Y = 12
# The shadow band: two rows over the eyes, one under.
SHADE_TOP = 11
SHADE_BOTTOM = EYE_Y + EYE_TALL

PALE = (196, 194, 200, 255)
# Not black — a shadow on a pale face is still lit. Pure black here reads as a
# hole punched in the texture, which is a different and much cheaper effect.
SHADE = (86, 86, 96, 255)
WHITE = (238, 238, 238, 255)
PUPIL = (14, 14, 18, 255)
# TWO, NOT THREE. The Turned's pupil fills a third of its socket because that
# face is meant to be read at conversational range — you are supposed to get
# close enough to see it looking at you. This one is read at forty blocks and
# never up close if it is working, and a big pupil at that distance blurs into
# the shadow band and the eye stops being an eye. Small and hard is what leaves
# a bright ring with a point in it.
PUPIL_WIDE = 2


def ashen(c):
	"""The same curve gen_turned.py runs on the clothes, run on all of him.

	Kept identical on purpose rather than tuned separately: these two are meant
	to be the same material, the same night, the same thing that happened to a
	village. If the blacks did not match, one of them would read as a mob from a
	different mod.
	"""
	if c[3] == 0:
		return c
	lum = 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
	v = (lum / 255.0) ** 1.55 * 0.78
	g = int(max(0, min(255, round(v * 255))))
	return (g, g, min(255, int(g * 1.07)), c[3])


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


def block(px, at, colour):
	x0, y0 = at[0] * SCALE, at[1] * SCALE
	for y in range(y0, y0 + SCALE):
		for x in range(x0, x0 + SCALE):
			px[y][x] = colour


def pupil_in(px, eye):
	"""Dead centre of the socket, three base-pixels across at 4x.

	Straight out of gen_turned.pupil_in and for its reasons: two disappears at
	three blocks, four is half the socket and reads as a hole, three is the
	largest thing that still looks like somebody looking.
	"""
	wide = EYE_WIDE * SCALE
	tall = EYE_TALL * SCALE
	x0 = eye * SCALE + (wide - PUPIL_WIDE) // 2
	y0 = EYE_Y * SCALE + (tall - PUPIL_WIDE) // 2
	for y in range(y0, y0 + PUPIL_WIDE):
		for x in range(x0, x0 + PUPIL_WIDE):
			px[y][x] = PUPIL


def preview(px):
	"""The face at base resolution, so it can be looked at before it ships.

	Every set of pixel coordinates anybody has eyeballed for this mod has been
	wrong at least once. This is cheaper than launching the game.
	"""
	# AT FULL RESOLUTION, NOT ONE CHAR PER BASE PIXEL.
	#
	# The first version of this sampled the top-left corner of each base pixel,
	# which was fine while the pupil was three wide and silently blind the moment
	# it went to two — a 2x2 dot centred in a 12x8 socket does not touch a base
	# pixel corner, so the preview cheerfully reported an eye with nothing in it.
	# A preview that cannot see the change it exists to check is worse than none.
	key = {PALE: '.', SHADE: ':', WHITE: 'O', PUPIL: '@'}
	print('\n    the face at 4x — %d x %d pixels:\n'
	      % ((FACE_X1 - FACE_X0 + 1) * SCALE, (FACE_Y1 - FACE_Y0 + 1) * SCALE))
	for y in range(FACE_Y0 * SCALE, (FACE_Y1 + 1) * SCALE):
		row = ''.join(key.get(px[y][x], '?')
		              for x in range(FACE_X0 * SCALE, (FACE_X1 + 1) * SCALE))
		print('      ' + row)
	print('\n      . pale   : shadow   O eye white   @ pupil\n')


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

	# ALL OF HIM, unlike the Turned. See the module docstring.
	base = [[ashen(base[y][x]) for x in range(width)] for y in range(height)]

	big = [[base[y // SCALE][x // SCALE] for x in range(width * SCALE)]
	       for y in range(height * SCALE)]

	# The face goes flat first and the features are drawn back on. Bleaching the
	# vanilla art in place would keep the brow and the nose shading as faint
	# ghosts, and a faint ghost of a face is worse than no face — it looks like a
	# texture that failed rather than a thing without features.
	for y in range(FACE_Y0, FACE_Y1 + 1):
		for x in range(FACE_X0, FACE_X1 + 1):
			block(big, (x, y), PALE)

	for y in range(SHADE_TOP, SHADE_BOTTOM + 1):
		for x in range(FACE_X0, FACE_X1 + 1):
			block(big, (x, y), SHADE)

	for eye in (LEFT, RIGHT):
		for dx in range(EYE_WIDE):
			for dy in range(EYE_TALL):
				block(big, (eye + dx, EYE_Y + dy), WHITE)
		pupil_in(big, eye)

	path = os.path.join(OUT, 'gaunt.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d' % ('gaunt/gaunt', width * SCALE, height * SCALE))

	# THE EYES AGAIN, ON THEIR OWN, FOR THE GLOW.
	#
	# RenderTypes.eyes draws a second pass at full brightness with no lighting
	# applied, which is how spider and enderman eyes stay lit in a cave. It takes a
	# whole texture and composites it over the model, so everything that is not
	# the eyes has to be transparent — anything opaque on this sheet becomes a
	# fullbright patch of villager.
	#
	# THE PUPIL IS LEFT OUT, and that is not laziness. The pass is additive: black
	# adds nothing and would come out invisible anyway, so painting the pupil here
	# would do nothing except hide the fact that it is the pupil UNDERNEATH, on the
	# base texture, that makes the shape read. Leaving the hole means the glow is a
	# ring around a dark centre, which is an eye. A filled glowing square is a
	# torch.
	#
	# This is the single highest-value pixel in the mob. The whole creature is
	# built to be seen at forty blocks in a forest at night with no light on it,
	# and at forty blocks in the dark this is the ONLY thing a player can see.
	glow = [[(0, 0, 0, 0) for _ in range(width * SCALE)]
	        for _ in range(height * SCALE)]
	for eye in (LEFT, RIGHT):
		for dx in range(EYE_WIDE):
			for dy in range(EYE_TALL):
				block(glow, (eye + dx, EYE_Y + dy), WHITE)
		pupil_in(glow, eye)
	for y in range(height * SCALE):
		for x in range(width * SCALE):
			if glow[y][x] == PUPIL:
				glow[y][x] = (0, 0, 0, 0)

	eyes = os.path.join(OUT, 'gaunt_eyes.png')
	pngio.write(eyes, width * SCALE, height * SCALE, glow)
	print('%-22s %dx%d' % ('gaunt/gaunt_eyes', width * SCALE, height * SCALE))

	if '--preview' in sys.argv:
		preview(big)


if __name__ == '__main__':
	main()
