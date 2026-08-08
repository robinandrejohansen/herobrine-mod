#!/usr/bin/env python3
"""The infected villager's skin.

Made from the vanilla villager rather than drawn, and that is the point. The
player has to recognise it — the shape, the nose, the brow, the same body under
the same robes — while knowing immediately that it is not right. Something drawn
from scratch would read as a different creature, and a different creature is not
frightening in the same way. This is somebody's neighbour.

Only the BASE texture is replaced. Villager clothing and profession are drawn as
separate layers on top, so an infected librarian still wears the librarian's
robe and still has the librarian's book at his belt. That is worth more than any
amount of gore: the thing coming down the corridor at you had a job.

Three changes, all of them restrained:

  drained    saturation pulled most of the way out, so the skin goes grey-green
             rather than a colour anybody chose
  sunken     everything darkened, and the darkest pixels darkened furthest, so
             the eye sockets and the mouth deepen on their own without needing
             to be found
  marked     a few deterministic blotches, always in the same places, because
             randomised damage looks like noise and repeated damage looks like
             a condition

Run:  python3 tools/gen_infected.py
"""
import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pngio

SOURCE = 'assets/minecraft/textures/entity/villager/villager.png'
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'assets', 'herobrine',
                   'textures', 'entity', 'infected')

# Where the sickness has taken hold. Fixed, not random.
BLOTCHES = [(10, 18, 3, 2), (20, 20, 2, 2), (41, 22, 3, 3),
            (28, 30, 4, 2), (16, 38, 3, 3), (46, 34, 2, 4)]


def drain(px):
    r, g, b, a = px
    if a == 0:
        return px
    grey = (r * 30 + g * 59 + b * 11) // 100
    # Most of the way to grey, then pushed a little green and darkened. The
    # darkest pixels lose the most, which deepens sockets without finding them.
    r = int((r * 0.22 + grey * 0.78) * 0.62)
    g = int((g * 0.22 + grey * 0.78) * 0.66)
    b = int((b * 0.22 + grey * 0.78) * 0.58)
    return (max(0, r), max(0, g), max(0, b), a)


def main():
    jars = glob.glob(os.path.expanduser(
        '~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
    if not jars:
        raise SystemExit('no client jar; run ./gradlew genSources first')

    os.makedirs(OUT, exist_ok=True)
    scratch = os.path.join(OUT, '.vanilla.png')
    with open(scratch, 'wb') as f:
        f.write(zipfile.ZipFile(jars[0]).read(SOURCE))

    width, height, px = pngio.read(scratch)
    for y in range(height):
        for x in range(width):
            px[y][x] = drain(px[y][x])

    for bx, by, bw, bh in BLOTCHES:
        for y in range(by, min(height, by + bh)):
            for x in range(bx, min(width, bx + bw)):
                r, g, b, a = px[y][x]
                if a == 0:
                    continue
                px[y][x] = (int(r * 0.45), int(g * 0.5), int(b * 0.42), a)

    out = os.path.join(OUT, 'villager.png')
    pngio.write(out, width, height, px)
    os.remove(scratch)
    print('infected/villager.png  %dx%d' % (width, height))


if __name__ == '__main__':
    main()
