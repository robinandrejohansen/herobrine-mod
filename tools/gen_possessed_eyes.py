#!/usr/bin/env python3
"""Emissive eye overlays for the mobs he takes.

RenderTypes.eyes() submits the ENTIRE model with one texture, so each of these
has to be the same size as the mob's own texture and transparent everywhere
except the eyes — the UVs are the mob's, not ours.

The eye rectangles below were read off the vanilla textures rather than
guessed. Every mob model gives the head cube a texOffs and a size, and the
front face of a box (w, h, d) at (u, v) always lands at (u + d, v + d) with
size w by h. Printing just that rectangle makes the eyes obvious — they are the
only bright pixels in it — and these are the coordinates that came out.

Run:  python3 tools/gen_possessed_eyes.py
"""
import os
import sys
import zipfile
import glob

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'possessed')

# name -> (vanilla texture inside the client jar, [(x, y, w, h), ...])
MOBS = {
	# Two wide rather than three: the outer column of a cow's eye is the dark
	# patch around it, not the eye, and lighting it made the whole side of the
	# face burn.
	'cow':      ('cow/cow_temperate.png',         [(7, 8, 2, 2), (11, 8, 2, 2)]),
	'pig':      ('pig/pig_temperate.png',         [(8, 11, 2, 1), (14, 11, 2, 1)]),
	'sheep':    ('sheep/sheep.png',               [(8, 10, 2, 1), (12, 10, 2, 1)]),
	'villager': ('villager/villager.png',         [(9, 14, 2, 1), (13, 14, 2, 1)]),
}

# Not white, and not opaque.
#
# Pure white at full alpha rendered as two lamps bolted to the animal's face —
# a light source rather than a pair of eyes, and it read as cartoonish the
# moment it was on screen. The EYES pipeline blends with BlendFunction.
# TRANSLUCENT, so alpha lets the animal's own face show through: what is left
# is a pale sheen sitting IN the eye rather than a hole punched through it.
#
# Slightly cool and slightly grey rather than neutral, because a warm white
# reads as firelight and a pure white reads as a screen. This wants to look
# reflective — like something catching light that is not there — which is what
# "glowing eyes" actually looks like when it is frightening rather than silly.
#
# Still emissive, so it holds up at midnight exactly as it does at noon. Only
# the intensity changed, never the trick.
# Two states, and the animal only ever wears one of them.
#
# Nothing at all while it is stalking. That is the whole first act: an animal
# that has stopped moving and will not make a sound is unsettling precisely
# because it looks completely ordinary, and marking it would answer the
# question the player is supposed to be sitting with.
#
# LOCKED is what it wears once it has turned on them — pale, cool, and still
# translucent so it sits IN the eye rather than replacing it. HUNTING is the
# last one, and it is the only red in the mod. Red is doing a job here that
# white cannot: white is his, and an animal wearing his eyes reads as "he is in
# there", where red reads as "this is going to hurt you". The player needs to
# be able to tell those apart across a field, at a glance, while running.
LOCKED = (206, 210, 216, 175)
HUNTING = (196, 44, 40, 190)
CLEAR = (0, 0, 0, 0)


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no decompiled client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def main():
	os.makedirs(OUT, exist_ok=True)
	jar = client_jar()
	scratch = os.path.join(OUT, '.vanilla.png')

	for name, (source, eyes) in MOBS.items():
		with open(scratch, 'wb') as f:
			f.write(jar.read('assets/minecraft/textures/entity/' + source))
		width, height, _ = pngio.read(scratch)

		for suffix, colour in (('', LOCKED), ('_hunting', HUNTING)):
			px = [[CLEAR] * width for _ in range(height)]
			lit = 0
			for x0, y0, w, h in eyes:
				for y in range(y0, y0 + h):
					for x in range(x0, x0 + w):
						px[y][x] = colour
						lit += 1

			# A texture that is entirely transparent renders nothing and would
			# fail silently, which is the worst way for this to break.
			assert lit > 0, name
			pngio.write(os.path.join(OUT, name + suffix + '.png'), width, height, px)
			print('%-18s %dx%d  %d pixels lit' % (name + suffix, width, height, lit))

	os.remove(scratch)


if __name__ == '__main__':
	main()
