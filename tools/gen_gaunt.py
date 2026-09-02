#!/usr/bin/env python3
"""The tall one: the ENDERMAN's body, painted like a VILLAGER.

WHY THE GEOMETRY IS THE ENDERMAN'S. The thing being asked for is a mouth that is
always open, and on a villager head that has to be a dark rectangle painted on a
flat face — which reads as a dark rectangle painted on a flat face. Vanilla
already solved it, and the solution is one no amount of paint reaches:

    THE ENDERMAN'S FACE IS TRANSPARENT WHERE ITS MOUTH IS.

Not dark. Absent. Rows 14 and 15 of the front of the head are alpha 0, so you
look through the front of the skull — and behind the hole, half a unit further
in, sits the `hat` cube, built with a NEGATIVE deformation so it is SMALLER than
the head rather than larger, carrying its own art on rows 29 to 31. The mouth is
therefore genuinely recessed: a hole, a lit surface behind it, and a parallax
that behaves correctly as the head turns, because it is geometry.

Taking the model takes the proportions with it, and they are the proportions the
mod spent three attempts rebuilding badly out of a villager: two-unit limbs
thirty units long, a narrow body, a head sitting high on no neck at all.

WHY THE PAINT IS THE VILLAGER'S. Because it has to be one of THEM. A black
figure with white eyes is an enderman someone has edited; a man in a brown robe
with a villager's face and a hole where his jaw should be is somebody you traded
with. The mod already rests on that — the Turned are villagers, the undercity is
the town again underneath the town — and the tall one has to belong to it.

So every colour here is measured off the real villager sheet rather than guessed:
skin 190,136,108, robe 113,84,77, the dark hem 66,47,41, the brow 51,36,17.

The enderman sheet is 64x32, indexed at 2 bits per pixel, which tools/pngio.py
cannot read. sips converts it. macOS only, and this is a dev script.

Run:  python3 tools/gen_gaunt.py [--preview]
"""
import glob
import os
import subprocess
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'gaunt')

SCALE = 4

# ---- MEASURED OFF assets/minecraft/textures/entity/villager/, not invented.
SKIN = (190, 136, 108, 255)
NOSE = (168, 118, 92, 255)
ROBE = (113, 84, 77, 255)
HEM = (66, 47, 41, 255)
BROW = (51, 36, 17, 255)
# The inside of the mouth. Warm rather than neutral, because a pure black hole in
# a coloured face reads as a rendering fault and a dark red-brown one reads as a
# throat.
GULLET = (30, 18, 16, 255)
WHITE = (238, 238, 236, 255)
PUPIL = (18, 16, 18, 255)

# ---- WHERE EVERYTHING IS ON THE ENDERMAN SHEET, derived from its own boxes.
#      head   texOffs(0,0)   8x8x8   -> x  0..32, y  0..16
#      hat    texOffs(0,16)  8x8x8   -> x  0..32, y 16..32
#      body   texOffs(32,16) 8x12x4  -> x 32..56, y 16..32
#      limbs  texOffs(56,0)  2x30x2  -> x 56..64, y  0..32   (arms AND legs)
FACE = (8, 8, 16, 16)        # the front face of the head cube
HEAD_TOP = (8, 0, 16, 8)     # the crown, which becomes hood
HEAD_BACK = (24, 8, 32, 16)  # and the back of it

MOUTH_ROWS = (14, 15)
EYE_ROWS = (10, 11)
EYE_COLS = (8, 9, 10, 13, 14, 15)
BRIDGE = (11, 12)
BROW_ROW = 9
PUPIL_WIDE = 3

# RED BEHIND THE BLACK, NOT GREEN.
#
# The green was the villager's own, copied off gen_turned.py, and on the Turned it
# is right: he is one of them and the green says so. On this it says the same
# thing, which is the problem — the tall one is not one of them any more, and an
# eye that files it under "villager" is doing the opposite of the work.
#
# A dark red reads as blood behind the pupil rather than as an iris at all, and it
# is the one hue that appears nowhere else on this creature. Muted rather than
# bright: a saturated red is a GLOWING eye, and glowing eyes are his.
IRIS = (124, 26, 26, 255)
IRIS_WIDE = 5

# AND THE PUPIL SITS LOW IN THE SOCKET.
#
# Centred, there was as much white under the pupil as over it, which is a level
# gaze. Dropped two pixels there is a band of white above and almost none below —
# the eye of something looking UP at you from under its own brow. The brow is
# gone now, so the eye has to do that on its own.
#
# ONE, NOT TWO. Two put the iris flush against the bottom of the socket, which
# breaks the rule this file argued for when the ring went in: white on all four
# sides, or it stops reading as an eye and starts reading as a coloured square.
# One is three rows of white above and one below — asymmetric enough to be a
# heavy lid, and still an eye.
PUPIL_DROP = 1


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def read_vanilla(scratch):
	jar = client_jar()
	raw, rgba = scratch + '.raw.png', scratch + '.rgba.png'
	with open(raw, 'wb') as f:
		f.write(jar.read('assets/minecraft/textures/entity/enderman/enderman.png'))
	subprocess.run(['sips', '-s', 'format', 'png', raw, '--out', rgba],
	               check=True, capture_output=True)
	w, h, px = pngio.read(rgba)
	os.remove(raw)
	os.remove(rgba)
	return w, h, px


def shaded(target, src):
	"""The villager's colour, wearing the enderman's shading.

	The enderman sheet is almost entirely two values — pure black and (22,22,22) —
	and that 22-point step is not noise, it is where vanilla put its edges: the
	seam down the arm, the line under the jaw, the crease of the brow. Replacing
	every opaque pixel with a flat colour throws all of it away and gives a
	cardboard cutout.

	So the step is kept as a multiplier instead. Black lands at 0.86 of the target
	and the lighter value at 1.0, which is a shade under a sixth of a stop — far
	too small to read as two colours, and exactly enough to keep every edge vanilla
	drew.
	"""
	if src[3] == 0:
		return src
	lum = 0.299 * src[0] + 0.587 * src[1] + 0.114 * src[2]
	k = 0.86 + min(1.0, lum / 22.0) * 0.14
	return (min(255, int(target[0] * k)), min(255, int(target[1] * k)),
	        min(255, int(target[2] * k)), src[3])


def region(px, x0, y0, x1, y1, target):
	for y in range(y0, y1):
		for x in range(x0, x1):
			px[y][x] = shaded(target, px[y][x])


def block(big, bx, by, colour):
	for y in range(by * SCALE, (by + 1) * SCALE):
		for x in range(bx * SCALE, (bx + 1) * SCALE):
			if big[y][x][3] != 0:
				big[y][x] = colour


def preview(big):
	key = {SKIN: '..', NOSE: 'nn', BROW: '##', WHITE: 'OO', PUPIL: '@@', ROBE: 'rr'}
	print('\n    the face, x 8..15:\n')
	for y in range(8, 16):
		row = ''
		for x in range(8, 16):
			p = big[y * SCALE][x * SCALE]
			row += '  ' if p[3] == 0 else key.get(p, '??')
		print('      %2d  %s%s' % (y, row, '   <- open, you see through it'
		                                   if y in MOUTH_ROWS else ''))
	print('\n      .. skin   nn nose   ## brow   OO eye   @@ pupil   (blank) hole\n')


def main():
	os.makedirs(OUT, exist_ok=True)
	scratch = os.path.join(OUT, '.ender')
	width, height, px = read_vanilla(scratch)

	# Limbs and body first, then the head over the top of them, because the head
	# regions overlap nothing but it keeps the order readable.
	region(px, 56, 0, 64, 32, ROBE)          # arms and legs share one strip
	region(px, 56, 26, 64, 32, HEM)          # and the last rows are hands and boots
	region(px, 32, 16, 56, 32, ROBE)         # the body
	region(px, 32, 28, 56, 32, HEM)          # hem of the robe
	region(px, 0, 0, 32, 16, SKIN)           # the whole head, then hood over it
	region(px, *HEAD_TOP, ROBE)
	region(px, *HEAD_BACK, ROBE)
	region(px, 0, 16, 32, 32, GULLET)        # the hat cube: the inside of the mouth

	big = [[px[y // SCALE][x // SCALE] for x in range(width * SCALE)]
	       for y in range(height * SCALE)]

	# ---- THE FACE.
	#
	# Rows 14 and 15 are never touched by anything below. They are the hole, they
	# are the reason this is built on an enderman at all, and every loop here stops
	# at 13 on purpose.
	# NO BROW. Tried it and it is better without.
	#
	# A villager's dark band across the forehead is what reads as a hood, and on a
	# face this size it also reads as an EXPRESSION — a line over the eyes is a
	# frown whether one was intended or not. Taking it out leaves an unbroken
	# forehead above two eyes that are open too wide, and the face stops looking
	# angry and starts looking absent. Absent is worse.
	#
	# BROW and BROW_ROW are kept for the preview key and in case this is reversed.

	for x in EYE_COLS:
		for y in EYE_ROWS:
			block(big, x, y, WHITE)

	# THE CRAZY VILLAGER'S EYE AFTER ALL, and this reverses a call made here.
	#
	# It was a sheep's — white with a dot in it — on the argument that a villager
	# eye is three values in a space that barely holds one, and that at range the
	# green only muddies the white. That argument is not wrong, and it is not what
	# was asked for: the Turned's eyes are the thing people find unsettling in this
	# mod, and the tall one should have them.
	#
	# So the middle value goes back in. White field, IRIS ring, black pupil in the
	# centre of it — the same three layers gen_turned.py builds, at the same green,
	# and the iris is sized to leave white on all four sides of it. An iris that
	# reaches the edge of the socket reads as a coloured eye; one with white around
	# it reads as an eye that is OPEN too wide.
	for eye in (8, 13):
		wide = 3 * SCALE
		tall = 2 * SCALE
		ix = eye * SCALE + (wide - IRIS_WIDE) // 2
		iy = EYE_ROWS[0] * SCALE + (tall - IRIS_WIDE) // 2 + PUPIL_DROP
		for y in range(iy, iy + IRIS_WIDE):
			for x in range(ix, ix + IRIS_WIDE):
				big[y][x] = IRIS

	for eye in (8, 13):
		wide = 3 * SCALE
		tall = 2 * SCALE
		x0 = eye * SCALE + (wide - PUPIL_WIDE) // 2
		y0 = EYE_ROWS[0] * SCALE + (tall - PUPIL_WIDE) // 2 + PUPIL_DROP
		for y in range(y0, y0 + PUPIL_WIDE):
			for x in range(x0, x0 + PUPIL_WIDE):
				big[y][x] = PUPIL

	# The nose, down the bridge and stopping at the lip. Painted rather than
	# modelled — the enderman head has no nose cube — so it is one tone under the
	# skin either side of it, which is what a plane turning away actually does.
	for x in BRIDGE:
		for y in (10, 11, 12, 13):
			block(big, x, y, NOSE)

	path = os.path.join(OUT, 'gaunt.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d' % ('gaunt/gaunt', width * SCALE, height * SCALE))
	holes = sum(1 for y in MOUTH_ROWS for x in range(8, 16)
	            if big[y * SCALE][x * SCALE][3] == 0)
	print('%-22s %d of 16 cells still open' % ('the mouth', holes))
	if '--preview' in sys.argv:
		preview(big)


if __name__ == '__main__':
	main()
