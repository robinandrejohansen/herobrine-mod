#!/usr/bin/env python3
"""The eyes of everything in his world.

Same trick as the possessed animals — an emissive overlay the size of the mob's
own texture, transparent everywhere except the eyes, submitted through
RenderTypes.eyes() so the pixels are as bright at midnight as at noon. What is
different here is that it applies to VANILLA mobs, everywhere in one dimension,
rather than to one animal he took.

WHITE, AND ONLY WHITE. Every other glow in this mod means something specific:
pale grey is a possessed animal locked on, red is one about to hurt you, green
with a pupil is the villager who has turned. White is HIS, it has been since the
first sighting, and a wood where every single thing looking at you has his eyes
says the only thing that place needs to say.

THE CREEPER IS UPSCALED FOUR TIMES and the others are not. Its eyes are 2x2, so
a red dot in the middle of one is four pixels wide at native size — there is no
middle. At 4x each eye is eight by eight and the dot is a clean two by two, dead
centre. Skeletons and zombies have 2x1 eyes with nothing inside them, so they
stay at native size where they cost nothing.

The coordinates below were read off the vanilla textures by scanning the head's
front face for its darkest pixels, using max(r,g,b) rather than the red channel
— a creeper is green, so its red channel is low everywhere and reading that
alone finds the whole face.

Run:  python3 tools/gen_his_host.py
"""
import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'host')

# name -> (vanilla texture, [(x, y, w, h), ...], upscale, pupil)
MOBS = {
	'skeleton': ('skeleton/skeleton.png',
	             [(9, 12, 2, 1), (13, 12, 2, 1)], 1, False),
	'zombie':   ('zombie/zombie.png',
	             [(9, 12, 2, 1), (13, 12, 2, 1)], 1, False),
	# Two by two, and the only one with anything in the middle of it.
	'creeper':  ('creeper/creeper.png',
	             [(9, 10, 2, 2), (13, 10, 2, 2)], 4, True),
}

# NOT PURE WHITE AND NOT FULLY OPAQUE, for the same reason the possessed eyes
# are not: at full alpha it renders as a lamp bolted to the face rather than as
# an eye, and the mob's own skin has to show through for it to sit IN the socket.
WHITE = (236, 240, 248, 190)
# And the creeper's pupil, which is the one warm thing in the whole dimension.
# Deep rather than bright — a bright red reads as a targeting laser, and this
# wants to read as something behind the white that is looking back.
PUPIL = (168, 26, 22, 235)
CLEAR = (0, 0, 0, 0)


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def main():
	os.makedirs(OUT, exist_ok=True)
	jar = client_jar()
	scratch = os.path.join(OUT, '.vanilla.png')

	for name, (source, eyes, scale, pupil) in MOBS.items():
		with open(scratch, 'wb') as f:
			f.write(jar.read('assets/minecraft/textures/entity/' + source))
		width, height, _ = pngio.read(scratch)
		w, h = width * scale, height * scale

		px = [[CLEAR] * w for _ in range(h)]
		lit = 0
		for ex, ey, ew, eh in eyes:
			for y in range(ey * scale, (ey + eh) * scale):
				for x in range(ex * scale, (ex + ew) * scale):
					px[y][x] = WHITE
					lit += 1
			if not pupil:
				continue
			# Dead centre, half the width of the eye. Any bigger and the eye is
			# simply red at four blocks, which is a different mob.
			px0 = ex * scale + ew * scale * 3 // 8
			py0 = ey * scale + eh * scale * 3 // 8
			for y in range(py0, py0 + max(1, eh * scale // 4)):
				for x in range(px0, px0 + max(1, ew * scale // 4)):
					px[y][x] = PUPIL

		assert lit > 0, name
		pngio.write(os.path.join(OUT, name + '.png'), w, h, px)
		print('%-10s %dx%d  %d pixels lit%s'
		      % (name, w, h, lit, '  (+pupil)' if pupil else ''))

	os.remove(scratch)


if __name__ == '__main__':
	main()
