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
# THREE WIDE, AND THEY GO TO THE EDGE OF THE FACE.
#
# The head's front face is base x 8..15 — eight pixels, no more. Two-wide eyes at
# 9 and 13 left a pixel of cheek outside each one, and that margin is what kept
# reading as "a villager with big eyes" rather than as staring. Three wide, from
# the outside edge inward, uses every pixel there is: 8.9.10, then 11.12 for the
# nose, then 13.14.15. It is not so much that the eyes got bigger as that the
# FACE ran out, which is the thing that makes a stare a stare.
LEFT = 8
RIGHT = 13
EYE_WIDE = 3
EYE_TALL = 2

# AND THEY MOVED UP, TAKING THE BROW WITH THEM.
#
# A villager's forehead is enormous — the hood sits high and there are five clear
# rows above the brow line. Eyes low on a face that tall read as sleepy, which is
# the exact opposite of what this is for. Everything shifts up one: the brow to
# 12, the eyes to 13 and 14, and row 15 goes back to being the cheek it always
# was in the vanilla art.
#
# The brow has to be REDRAWN rather than moved, because the eyes now occupy the
# row it used to be on. Without it the eyes hang off nothing, and the heavy line
# above them is the thing this whole face is meant to be hanging from.
BROW_Y = 12
EYE_Y = 13
# NEAR BLACK, AND IT HAS TO BE.
#
# The first pass used the vanilla brow brown (58,42,32) and it vanished — because
# the ashen curve takes the forehead down to about the same value, so a brow
# painted in the original colour lands on top of a forehead that is now exactly
# as dark as it is. Nothing was wrong with the line; there was simply no contrast
# left for it to sit in.
#
# So it is drawn against the NEW palette rather than the old one: darker than
# anything the curve can produce, which is what puts a hard shadow over the eyes
# and gives them something to hang from.
BROW = (12, 12, 16, 255)
# How big the dark point is, in upscaled pixels. Three out of the twelve the
# socket covers — a quarter of the eye, which is about a real one and is small
# enough that the white around it is unmistakably the shape you are reading.
PUPIL_WIDE = 3

# The iris he is given, and it is the villager's own green.
#
# Not red and not white. White is HIS, and the whole mod's colour grammar rests
# on that; red is what a possessed animal wears when it is about to hurt you.
# This is neither — it is a villager, with a villager's eyes, and the only thing
# wrong with them is that there is something looking back out. Keeping the
# vanilla green is what makes the pupil the entire message.
IRIS = (56, 148, 56, 255)
WHITE = (238, 238, 238, 255)
# BLACK, AND SMALL, WHICH REVERSES WHAT THIS COMMENT USED TO SAY.
#
# It argued for green: a black pupil reads as a doll, a lit one reads as
# something looking back. That reasoning was sound for a two-by-two eye where the
# pupil was most of the socket — at that size a dark centre really did read as a
# hole. It stops being true once the eye is three wide and edge to edge, because
# now the WHITE is the shape and the pupil is a point inside it, and a small dark
# point in a large white eye is the most direct way a face has of pointing at
# something. Green at that size read as glowing, which is a different creature.
#
# Not pure black. A hair of blue, the same cold neutral as the brow, so the two
# dark features on the face belong to each other.
PUPIL = (14, 14, 18, 255)


def ashen(c):
	"""Black and grey, and the green eyes are painted on afterwards.

	SAME MAN, NO COLOUR. Every thread on him is the vanilla villager's — the same
	robe, the same hood, the same apron folds — put through a curve rather than
	redrawn, so at any distance he is still the silhouette of somebody you walked
	past this morning. Nothing about his SHAPE says anything is wrong.

	A gamma above one is what makes it black rather than merely grey: it pushes
	the midtones down hard and leaves the highlights alone, so the brown robe goes
	to near-black while the pale trim stays readable as trim. A flat desaturation
	would have given a uniformly mid-grey man, and mid-grey is the one value that
	reads as a texture that failed to load.

	The last line puts a hair of blue in the greys. A dead neutral looks like a
	rendering fault; a cold neutral looks like a colour somebody chose — and it
	also puts the green in his eyes a very long way from anything else on him.
	"""
	if c[3] == 0:
		return c
	lum = 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
	v = (lum / 255.0) ** 1.55 * 0.78
	g = int(max(0, min(255, round(v * 255))))
	return (g, g, min(255, int(g * 1.07)), c[3])


# WHERE THE HEAD STOPS, READ OFF THE FILE RATHER THAN GUESSED.
#
# Scanning villager.png for rows with any opaque pixel gives the net exactly: the
# head cube and its hat occupy rows 0 to 17, then rows 18 and 19 are empty, then
# the body, arms and legs start at 20. A gap of two blank rows is not a
# coincidence, it is the seam between the two halves of the model, and it means
# the split can be a single number with nothing straddling it.
HEAD_ROWS = 18


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
	# THE CLOTHES ONLY. THE HEAD KEEPS ITS OWN COLOUR.
	#
	# Running the curve over everything made a grey man, and a grey man is a
	# different character — he reads as a wraith, or as a texture that failed. The
	# thing that actually unsettles is a perfectly ordinary villager's head, the
	# same colour as the ones you have traded with for forty hours, sitting on top
	# of clothes that have gone black. One half normal is what makes the other half
	# legible as wrong; both halves wrong is just a new mob.
	#
	# The curve runs BEFORE the upscale — one pass over four thousand pixels rather
	# than sixty-five thousand — and before the eyes, which are painted after and
	# are therefore untouched by it.
	base = [[ashen(base[y][x]) if y >= HEAD_ROWS else base[y][x]
	         for x in range(width)] for y in range(height)]

	big = [[base[y // SCALE][x // SCALE] for x in range(width * SCALE)]
	       for y in range(height * SCALE)]

	# The brow, one row higher than vanilla put it, drawn across both sockets AND
	# the bridge between them so it reads as a single line rather than two dashes.
	for x in range(LEFT, RIGHT + EYE_WIDE):
		block(big, (x, BROW_Y), BROW)

	for eye in (LEFT, RIGHT):
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
		for dx in range(EYE_WIDE):
			for dy in range(EYE_TALL):
				block(big, (eye + dx, EYE_Y + dy), WHITE)
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
	# Dead centre of the twelve-by-eight the socket now covers. Five across rather
	# than three: the eye grew by half again in both directions, and a pupil that
	# did not grow with it would be a big eye with a small dot in it, which reads
	# as surprise. Matched to the socket, it reads as attention.
	wide = EYE_WIDE * SCALE
	tall = EYE_TALL * SCALE
	x0 = eye * SCALE + (wide - PUPIL_WIDE) // 2
	y0 = EYE_Y * SCALE + (tall - PUPIL_WIDE) // 2
	for y in range(y0, y0 + PUPIL_WIDE):
		for x in range(x0, x0 + PUPIL_WIDE):
			px[y][x] = PUPIL


if __name__ == '__main__':
	main()
