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
# ONE TONE UNDER THE SKIN — UNDER THE DARK ONE, WHICH IS NOT THE SAME NUMBER.
#
# It was 168,118,92: a step under 190,136,108 and correct against a flat face.
# This face is not flat. shaded() keeps the enderman's own edges by multiplying
# every pixel by 0.86 or by 1.0 depending on what vanilla drew underneath, and
# 0.86 of the skin is 163,116,92 — five points off the old nose in one channel
# and none at all in the other two. So wherever the enderman sheet happened to be
# dark, and on the front of the head that is fully half of it, the nose was the
# same colour as its neighbours and was simply not there.
#
# Invisible for half its length mattered little on an eight-unit face where the
# nose had two rows to itself. It matters now: the head is half again as tall,
# most of the new length is BELOW the eyes, and the bridge is the only thing
# drawn in it. A blank lower face is not a longer face, it is a bigger gap.
#
# 150,106,84 is a step under the DARK skin as well as two under the light, so it
# reads the whole way down whichever of the two vanilla put beneath it.
NOSE = (150, 106, 84, 255)
ROBE = (113, 84, 77, 255)
HEM = (66, 47, 41, 255)
BROW = (51, 36, 17, 255)
# The inside of the mouth. Warm rather than neutral, because a pure black hole in
# a coloured face reads as a rendering fault and a dark red-brown one reads as a
# throat.
GULLET = (30, 18, 16, 255)
WHITE = (238, 238, 236, 255)

# ---- WHERE EVERYTHING IS ON THE ENDERMAN SHEET, derived from its own boxes.
#      head   texOffs(0,0)   8x8x8   -> x  0..32, y  0..16
#      hat    texOffs(0,16)  8x8x8   -> x  0..32, y 16..32
#      body   texOffs(32,16) 8x12x4  -> x 32..56, y 16..32
#      limbs  texOffs(56,0)  2x30x2  -> x 56..64, y  0..32   (arms AND legs)
FACE = (8, 8, 16, 16)        # the front face of the head cube
HEAD_TOP = (8, 0, 16, 8)     # the crown, which becomes hood
HEAD_BACK = (24, 8, 32, 16)  # and the back of it

# A LONG MOUTH: FOUR ROWS OF THE EIGHT, NOT TWO.
#
# Two rows was a slot. The face this is being pointed at has a mouth taking most
# of the lower half of a very long head — so it runs from texel 12 to the jaw,
# which at a 1.5 head is six units of open mouth on a twelve unit face.
#
# Rows 14 and 15 come with alpha 0 already; vanilla put the enderman's own mouth
# there. 12 and 13 are opaque skin on the source sheet and have to be CUT — see
# the hole() call in main(). Forgetting that is a mouth two rows long with skin
# painted over the other two.
MOUTH_ROWS = (12, 13, 14, 15)
EYE_COLS = (8, 13)       # left texel of each socket
EYE_WIDE = 3             # and how many texels wide it is

# ---- THE EYE IS DRAWN IN SUB-PIXELS, BECAUSE THE FACE IS EIGHT TEXELS AND THE
#      HEAD IS NOW STRETCHED TO ONE AND A HALF OVER THEM.
#
# The face has to hold, top to bottom: forehead, eyes, the drop to the mouth, and
# two rows of mouth that are a hole and cannot move. Eight rows for four things,
# and the ask was MORE forehead and MORE face under the eye and the eye HIGHER —
# which is three demands on a budget that does not grow. On whole texels they
# cancel: every row the eye moves up is a row off the forehead.
#
# It does grow sideways, though. SCALE is 4, so the sheet is written at four
# sub-pixels per texel and the eye can sit at a quarter of a row. Seven
# sub-pixels tall instead of eight whole ones, starting half a texel higher:
#
#     forehead   texel 8    -> 9.5     1.50 rows   2.25 units at 1.5
#     eye                   -> 11.25   1.75 rows   2.63
#     the drop              -> 14      2.75 rows   4.13
#     mouth                 -> 16      2.00 rows   3.00
#
# Against the eight-unit face it replaces, where all four were 2.00 flat. Longer
# above, half again as long below, and the eye's centre has gone from three
# eighths of the way down the face to a little under three tenths — up, and
# smaller as a share of the face, while staying about the same size on screen.
# BACK TO SUB-PIXELS, BECAUSE THE HEAD IS 2.6 NOW AND A TEXEL ROW IS EXPENSIVE.
#
# The head was stretched 1.5 when the eye was given two whole texels, so it stood
# three units tall and that was right. At 2.6 the same two rows are five and a
# quarter units of eye on a twenty-unit face — a pair of dinner plates. The
# reference this is drawn from has small eyes set high, and the mouth doing all
# the work.
#
# Five sub-pixels, starting half a texel down. Over a face where one texel row is
# 2.6 units that comes out as:
#
#     forehead   texel 8    -> 9.5      3.9 units
#     eye                   -> 10.75    3.25
#     nose, cheek           -> 12       3.25
#     mouth                 -> 16      10.4     <- half the face
#
# Which is the whole silhouette in four numbers.
EYE_TOP_PX = 38
EYE_TALL_PX = 6

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
# WHITE EYES IN A DARK SOCKET, AND THE IRIS IS GONE.
#
# It was white, a dark red ring and a black pupil — three layers, argued for at
# length on the grounds that the Turned's eyes are the unsettling thing in this
# mod and the tall one should have them. That was the wrong reference. The tall
# one is not one of them any more, and the face being reached for has two flat
# white rectangles in it and nothing else.
#
# A pupil is also what makes an eye read as LOOKING somewhere. Taking it out is
# what makes this face read as looking at nothing, from a head that is
# nevertheless turned squarely at you.
#
# THE SOCKET IS THE PART THAT MAKES IT WORK. White on villager skin is barely a
# value step — 238 against 190 — so a white rectangle painted straight onto the
# face is a pale smudge. A one-pixel near-black rim gives it an edge, and the
# reference has exactly that. It is also why the glow sheet is worth having: see
# eyes() below.
SOCKET = (22, 16, 18, 255)

# AND A BLACK DOT BACK IN THE MIDDLE OF IT.
#
# The pupil came out when the iris did, on the argument that a pupil is what
# makes an eye read as LOOKING somewhere and this face should look at nothing.
# That argument holds for a creature standing in a field at forty blocks. Up
# close it stops being eerie and starts being unfinished: two flat white
# rectangles with no centre are a texture that has not been drawn yet.
#
# A dot is not an iris. It is one dark mark in a field of white, and what it
# does is give the glow somewhere to be AROUND — the eye now reads as lit from
# behind rather than as painted.
PUPIL = (16, 14, 16, 255)
PUPIL_WIDE = 3
PUPIL_TALL = 2

# HOW HARD THE GLOW SHEET BURNS.
#
# EyesLayer draws at full brightness with world lighting ignored, so a solid
# white eye on that layer is the brightest thing on the screen in a dark cell —
# it was reported as glowing too much and it was: two headlamps.
#
# Alpha is the only dimmer there is, and it works because the render type is
# translucent rather than additive. 110 of 255 keeps them clearly lit in the
# dark and stops them being the only thing you can see.
GLOW_ALPHA = 110



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


def rect(big, x0, y0, x1, y1, colour):
	"""A flat fill in SUB-PIXELS rather than texels.

	block() paints a whole texel because everything on this sheet used to land on
	one. The eye no longer does — it is seven sub-pixels tall and starts on a half
	— so it needs a fill that can address the grid the file is actually written on.

	Alpha 0 is left alone, same as block(). Rows 14 and 15 of the face are the
	hole and nothing here may close them.
	"""
	for y in range(y0, y1):
		for x in range(x0, x1):
			if big[y][x][3] != 0:
				big[y][x] = colour


def preview(big):
	"""THE FACE AT SUB-PIXEL RESOLUTION, because that is where it is drawn now.

	Sampling one pixel per texel — big[y * SCALE][x * SCALE] — was honest while
	every feature landed on a texel boundary. It is not any more: it would read
	texel 9 as bare skin and texel 10 as solid iris, showing a two-row eye that
	does not exist and hiding the half-row it actually moved. A generator whose
	preview cannot see the change it just made is worse than no preview.
	"""
	# NEAREST, NOT EXACT. shaded() multiplies every pixel it lays down by 0.86 to
	# 1.0 to keep the enderman's own edges, so almost nothing on the sheet equals
	# the constant it came from. An exact lookup printed '?' for most of the face.
	# AND BOTH SHADINGS OF EACH, or it reports a feature that is not on the sheet.
	# shaded() lays down 0.86 and 1.0 of every target, and a nearest match against
	# the bare constants filed dark skin under whichever OTHER constant it landed
	# closest to — which was the nose, and printed the entire forehead as one.
	key = []
	for colour, mark in [(SKIN, '.'), (NOSE, 'n'), (BROW, '#'), (WHITE, 'O'),
	                     (SOCKET, 'o'), (ROBE, 'r'), (GULLET, 'x')]:
		key.append((colour, mark))
		key.append((tuple(int(c * 0.86) for c in colour[:3]), mark))

	def glyph(p):
		if p[3] == 0:
			return ' '
		return min(key, key=lambda k: sum((a - b) ** 2
		                                  for a, b in zip(k[0][:3], p[:3])))[1]
	print('\n    the face at 1/%d texel — rows are sub-pixels, texel marked left:\n'
	      % SCALE)
	for y in range(8 * SCALE, 16 * SCALE):
		row = ''
		for x in range(8 * SCALE, 16 * SCALE):
			row += glyph(big[y][x])
		mark = '%5.2f' % (y / SCALE)
		note = ''
		if y == EYE_TOP_PX:
			note = '  <- eye opens'
		elif y == EYE_TOP_PX + EYE_TALL_PX - 1:
			note = '  <- eye closes'
		elif y == MOUTH_ROWS[0] * SCALE:
			note = '  <- the hole starts; you see through it'
		print('      %s  %s%s' % (mark, row, note))
	print('\n      . skin  n nose  O white  o socket  r hood  (blank) hole\n')


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
	# BROW is kept for the preview key and in case this is reversed. BROW_ROW went
	# with EYE_ROWS: both named whole texel rows, nothing read either of them, and
	# a row number sitting beside sub-pixel geometry only invites somebody to use
	# it and land four sub-pixels off.

	for eye in EYE_COLS:
		x0 = eye * SCALE
		x1 = (eye + EYE_WIDE) * SCALE
		rect(big, x0, EYE_TOP_PX, x1, EYE_TOP_PX + EYE_TALL_PX, SOCKET)
		rect(big, x0 + 1, EYE_TOP_PX + 1, x1 - 1, EYE_TOP_PX + EYE_TALL_PX - 1, WHITE)
		# The dot, centred in the white on both axes.
		px0 = x0 + (EYE_WIDE * SCALE - PUPIL_WIDE) // 2
		py0 = EYE_TOP_PX + (EYE_TALL_PX - PUPIL_TALL) // 2
		rect(big, px0, py0, px0 + PUPIL_WIDE, py0 + PUPIL_TALL, PUPIL)

	# ---- THE NOSE'S NET, AND THE PAINTED ONE IS GONE.
	#
	# There is a cube now — 2x2x2 standing one texel clear of the face, built in
	# GauntRenderer.mesh() at texOffs(32,0), which is the only ground vanilla left
	# free on this sheet. So the stripe that used to be painted down the bridge has
	# been deleted rather than kept: the cube stands in front of exactly where it
	# was, and two noses one behind the other is worse than either.
	#
	# FLAT, ON PURPOSE. Its whole net gets one colour, all eight by four texels of
	# it, and no attempt is made to shade the front face lighter than the sides.
	# Real geometry is lit per face by the game — that is the entire reason for
	# making it geometry — so shading painted into it would fight the lighting that
	# is already there. This is also why the exact face order in a cube's net does
	# not need to be known: every face of it is the same colour.
	rect(big, 32 * SCALE, 0, 40 * SCALE, 4 * SCALE, NOSE)

	# ---- CUT THE MOUTH OPEN.
	#
	# Vanilla gave rows 14 and 15 of the face alpha 0 and nothing else. The other
	# two rows of this mouth are ordinary opaque skin on the source sheet, and
	# every loop above has just painted over them — so the hole is made HERE,
	# last, after the face is finished.
	#
	# Only the front of the head. The same rows on the sides and the back are the
	# skull and the hood, and punching those out puts a window through the head.
	for row in MOUTH_ROWS:
		for y in range(row * SCALE, (row + 1) * SCALE):
			for x in range(8 * SCALE, 16 * SCALE):
				big[y][x] = (0, 0, 0, 0)

	path = os.path.join(OUT, 'gaunt.png')
	pngio.write(path, width * SCALE, height * SCALE, big)
	print('%-22s %dx%d' % ('gaunt/gaunt', width * SCALE, height * SCALE))
	cells = len(MOUTH_ROWS) * 8
	holes = sum(1 for y in MOUTH_ROWS for x in range(8, 16)
	            if big[y * SCALE][x * SCALE][3] == 0)
	print('%-22s %d of %d cells open, %d rows deep'
	      % ('the mouth', holes, cells, len(MOUTH_ROWS)))
	# ---- THE GLOW SHEET.
	#
	# EyesLayer submits the whole model again with RenderTypes.eyes(), which draws
	# at full brightness and ignores world lighting. So this sheet is the same
	# canvas, transparent everywhere, with the two white rectangles copied onto it
	# — and those pixels then hold their brightness in a pitch black cell, which is
	# where this creature is standing.
	#
	# The SOCKET rim is deliberately left out of it. A glowing black rim is a hole
	# punched in the light; the rim's job is on the base sheet, where it is lit
	# normally and gives the white something to sit against.
	glow = [[(0, 0, 0, 0)] * (width * SCALE) for _ in range(height * SCALE)]
	lit = WHITE[:3] + (GLOW_ALPHA,)
	for eye in EYE_COLS:
		x0 = eye * SCALE
		x1 = (eye + EYE_WIDE) * SCALE
		for y in range(EYE_TOP_PX + 1, EYE_TOP_PX + EYE_TALL_PX - 1):
			for x in range(x0 + 1, x1 - 1):
				# THE WHITE ONLY. Reading it back off the finished sheet rather
				# than recomputing the rectangle, so the dot cannot end up glowing
				# because somebody moved it and forgot this loop.
				if big[y][x][:3] == WHITE[:3]:
					glow[y][x] = lit
	eyes_path = os.path.join(OUT, 'gaunt_eyes.png')
	pngio.write(eyes_path, width * SCALE, height * SCALE, glow)
	lit = sum(1 for r in glow for c in r if c[3] != 0)
	print('%-22s %dx%d, %d lit pixels' % ('gaunt/gaunt_eyes', width * SCALE,
	                                      height * SCALE, lit))

	if '--preview' in sys.argv:
		preview(big)


if __name__ == '__main__':
	main()
