#!/usr/bin/env python3
"""The portal, in white and black.

The nether's is purple and the end's is a starfield, and neither is his. White
is HIS COLOUR and it has been since the first pair of eyes in this mod — so the
door he was cutting is white light behind black, and nothing else in the game
looks like it.

ANIMATED, because a still portal is a wall with a texture on it. Eight frames of
a drifting field, played at three ticks each, which is slow enough to read as
something moving under the surface rather than as a flicker.

The pattern is value noise summed at three scales and then THRESHOLDED rather
than shaded. Smooth grey would look like fog; a hard cut gives filaments and
holes — light coming through a gap in something — and it is that shape, not the
brightness, that makes it read as a way through.

Every frame is generated from the same 3D field sampled at a different depth, so
frame eight wraps back into frame one and the loop has no seam.

Run:  python3 tools/gen_the_way.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'block')

SIZE = 16
FRAMES = 8
SEED = 0x48455242      # "HERB"
# How many lattice cells deep the loop is. Two is a slow drift; higher is a
# scroll, lower is a throb.
DEPTH = 2

# BLACK IS NOT QUITE BLACK AND WHITE IS NOT QUITE WHITE.
#
# Pure #000 next to pure #fff renders as a cut-out sticker — there is no
# material in it, and at any distance it reads as a hole in the world rather
# than as something occupying the frame. A near-black with a trace of blue in
# it, and a white pulled slightly cool, gives the surface a temperature.
# AND IT IS NOT GREY ANY MORE.
#
# Four steps of neutral grey is the cheapest thing a portal can be: no hue
# anywhere in it, so the eye reads flat noise rather than depth, and at any
# distance it is a television with no signal in a stone frame.
#
# A RAMP WITH A HUE IN IT INSTEAD, and the hue turns as it brightens. Nearly
# black with violet buried in it, up through a deep indigo, out to a cold lilac,
# and only the very top of it goes white. Light that changes colour as it gets
# brighter is what every real glow does and what no flat palette can fake — it
# is the whole difference between something lit and something coloured in.
#
# Not the nether's magenta. That purple is warm and saturated and belongs to a
# block everybody has already made a hundred of. This one is cold, and the white
# at the top of it is his — the same white as the eyes.
DARK = (9, 6, 20, 255)
MID = (46, 32, 84, 255)
PALE = (148, 126, 214, 255)
BRIGHT = (236, 230, 255, 255)


def hashed(x, y, z):
	"""One deterministic value per lattice point, with no dependencies."""
	h = (x * 374761393 + y * 668265263 + z * 2147483647 + SEED) & 0xFFFFFFFF
	h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
	return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def smooth(t):
	return t * t * (3.0 - 2.0 * t)


def noise(x, y, z, period, depth):
	"""Value noise on a lattice that WRAPS, so the tile has no visible seam."""
	x0, y0 = math.floor(x), math.floor(y)
	z0 = math.floor(z)
	fx, fy, fz = smooth(x - x0), smooth(y - y0), smooth(z - z0)
	total = 0.0
	for dz in (0, 1):
		plane = 0.0
		for dy in (0, 1):
			row = 0.0
			for dx in (0, 1):
				# Wrapped on the horizontal axes so left meets right and top
				# meets bottom; wrapped on depth so the last frame meets the
				# first.
				v = hashed((x0 + dx) % period, (y0 + dy) % period,
				           (z0 + dz) % depth)
				row += v * (fx if dx else 1.0 - fx)
			plane += row * (fy if dy else 1.0 - fy)
		total += plane * (fz if dz else 1.0 - fz)
	return total


def main():
	os.makedirs(OUT, exist_ok=True)
	px = [[DARK] * SIZE for _ in range(SIZE * FRAMES)]

	field = {}
	for frame in range(FRAMES):
		for y in range(SIZE):
			for x in range(SIZE):
				# Three octaves. One gives blobs, five gives static; three is
				# where filaments appear.
				# THE SAME DEPTH FOR EVERY OCTAVE, ADVANCING EXACTLY ONE
				# LATTICE PERIOD ACROSS THE EIGHT FRAMES.
				#
				# The first version stepped depth per octave, which meant each
				# octave travelled a different distance and none of them
				# travelled a whole one — so frame eight did not meet frame one,
				# and consecutive frames sampled effectively unrelated slices.
				# It came out as the whole texture pulsing bright and dark
				# rather than as anything moving through it.
				z = frame / FRAMES * DEPTH
				v = 0.0
				# Weighted toward the higher octaves, because the low one on its
				# own gives four fat blobs across sixteen pixels and what this
				# wants is filaments.
				for octave, weight in ((4, 0.35), (8, 0.4), (16, 0.25)):
					scale = octave / SIZE
					v += weight * noise(x * scale, y * scale, z, octave, DEPTH)
				field[(frame, x, y)] = v

	# THE BANDS ARE PERCENTILES, NOT FIXED CUTS, and that is not fussiness.
	#
	# Value noise averaged over eight lattice corners clusters hard around a
	# half, so a threshold picked by eye lands somewhere unpredictable on the
	# distribution — the first attempt at 0.62 was meant to light a fifth of the
	# texture and lit thirty-nine per cent of it, which came out as a white
	# block with dark speckles rather than a dark one with light in it.
	#
	# Ranking instead pins the LOOK rather than the number: the brightest eight
	# per cent is the core of the filaments, the next twelve is their edge, the
	# next fifteen is the surface catching a little of it, and the remaining two
	# thirds is black. Retune the octaves as much as you like and the balance
	# holds.
	# AND RANKED PER FRAME, not across all of them at once.
	#
	# Pooling every frame into one ranking sounds equivalent and is not: a frame
	# whose slice of the field happens to sit low overall then gets almost
	# nothing above the cut, and one sitting high gets nearly everything. Which
	# is the pulse the depth fix was supposed to remove, arriving by a different
	# route — frame zero came out three-quarters white and frame three came out
	# empty.
	#
	# Per frame, every single frame has exactly the same amount of light in it,
	# so nothing brightens or dims across the loop. The only thing that changes
	# is WHERE the light is, which is the entire point of animating it.
	for frame in range(FRAMES):
		here = sorted(field[(frame, x, y)]
		              for y in range(SIZE) for x in range(SIZE))

		def cut(fraction, ranked=here):
			return ranked[min(len(ranked) - 1, int(len(ranked) * (1.0 - fraction)))]

		core, edge, sheen = cut(0.08), cut(0.20), cut(0.35)
		for y in range(SIZE):
			for x in range(SIZE):
				v = field[(frame, x, y)]
				if v >= core:
					colour = BRIGHT
				elif v >= edge:
					colour = PALE
				elif v >= sheen:
					colour = MID
				else:
					colour = DARK
				px[frame * SIZE + y][x] = colour

	pngio.write(os.path.join(OUT, 'the_way.png'), SIZE, SIZE * FRAMES, px)
	with open(os.path.join(OUT, 'the_way.png.mcmeta'), 'w') as f:
		f.write('{\n  "animation": {\n    "frametime": 3,\n'
		        '    "interpolate": true\n  }\n}\n')

	lit = sum(1 for row in px for c in row if c in (PALE, BRIGHT))
	print('the_way  %dx%d, %d frames, %d%% lit'
	      % (SIZE, SIZE * FRAMES, FRAMES, 100 * lit // (SIZE * SIZE * FRAMES)))


if __name__ == '__main__':
	main()
