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
	'cow':      ('cow/cow_temperate.png',         [(6, 8, 3, 2), (11, 8, 3, 2)]),
	'pig':      ('pig/pig_temperate.png',         [(8, 11, 2, 1), (14, 11, 2, 1)]),
	'sheep':    ('sheep/sheep.png',               [(8, 10, 2, 1), (12, 10, 2, 1)]),
	'villager': ('villager/villager.png',         [(9, 14, 2, 1), (13, 14, 2, 1)]),
}

WHITE = (255, 255, 255, 255)
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

		px = [[CLEAR] * width for _ in range(height)]
		lit = 0
		for x0, y0, w, h in eyes:
			for y in range(y0, y0 + h):
				for x in range(x0, x0 + w):
					px[y][x] = WHITE
					lit += 1

		# A texture that is entirely transparent renders nothing and would fail
		# silently, which is the worst way for this to break.
		assert lit > 0, name
		out = os.path.join(OUT, name + '.png')
		pngio.write(out, width, height, px)
		print('%-9s %dx%d  %d pixels lit' % (name, width, height, lit))

	os.remove(scratch)


if __name__ == '__main__':
	main()
