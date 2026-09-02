#!/usr/bin/env python3
"""Cut six buildings out of the Colorful Medieval Village world into blueprints.

WHY SIX FILES AND NOT ONE. Oakhold went in as a single four-hundred-thousand
block blueprint and the complaint about it was simply that it was too big — one
enormous object with one orientation, dropped whole. This pack is not one build:
it is an asset grid, thirty-four separate buildings repeated in thirteen colour
variants at a pitch of thirty blocks in x. Taking a slab of that grid would take
the showcase's plinths and spacing with it.

So each building comes out on its own and structure/Hamlet.java arranges them.
Six files, rotatable, laid round a square — a settlement rather than a monument.

WHICH VARIANT. Thirteen exist and only the roof and one accent differ; the
geometry is identical to the block. VARIANT picks the column. 5 is the green
terracotta roof, which is the closest of the thirteen to the promo render and
reads as weathered on an abandoned village.

THE FRONT IS THE WEST FACE. Every one of the six has its entrance at low local x
with the door facing east, so the unrotated building fronts -X. Hamlet turns each
one from that: WEST is no rotation, NORTH is clockwise, EAST is 180.

EVERY NON-AIR BLOCK, NO FILTER. Oakhold was first taken with a "natural blocks"
filter that dropped 53,166 blocks which stand()'s clear had already turned to
air, and it placed hollow. A blueprint records what is there; the clear is what
decides what goes.

Run:  python3 tools/village/extract.py
"""
import collections, gzip, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'castle'))
import modern

WORLD = '/Users/robin/Downloads/Colorful Medieval Village Assets'
OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources',
                   'assets', 'herobrine', 'blueprints')

PLATE = -63          # the world's ground plate; -64 is bedrock and is not taken
VARIANT = 5          # green terracotta roofs
PITCH = 30           # x between colour variants

# x is given for variant 0; VARIANT * PITCH is added. z and the top are shared.
#
# `front` IS MEASURED, NOT ASSUMED. Five of the six put the ground-floor door at
# low x facing east — you walk east through it, so you enter off the west face.
# cottage_a does not: its door is at (3,4) facing SOUTH, which is a north front.
# Writing "west" for all six would have turned that one's back wall to the square
# and looked like a building placed wrong rather than data read wrong.
PARTS = [
    # name              x0   x1     z0    z1   top   front   what it is
    ('village_hall',     43,  59,  678,  698, -37, 'west',
     'the flag, the bell, the long hall'),
    ('village_chapel',   44,  59,  653,  671, -45, 'west',
     'two bells and a gallery'),
    ('village_smithy',   43,  60,  787,  808, -40, 'west',
     'anvils, forge, cartography; it has a second door on its east side'),
    ('village_store',    43,  57,  629,  643, -49, 'west',
     'barrels and looms'),
    ('village_cottage_a', 45,  59,  581,  591, -50, 'north',
     'kitchen, two beds'),
    ('village_cottage_b', 45,  56,  603,  616, -51, 'west',
     'two beds, a workshop'),
]

# WHERE THE SHAFT GOES DOWN, in the hall's own unrotated coordinates. Interior
# floor, clear of the walls and of both interior doors, verified below.
DESCENT = {'village_hall': (9, 1, 11)}


def spec(name, props):
    if not props:
        return name
    return '%s[%s]' % (name, ','.join('%s=%s' % kv for kv in sorted(props.items())))


def main():
    W = modern.World(WORLD)
    os.makedirs(OUT, exist_ok=True)
    for name, vx0, vx1, z0, z1, top, front, note in PARTS:
        x0, x1 = vx0 + VARIANT * PITCH, vx1 + VARIANT * PITCH
        blocks = W.box(x0, x1, PLATE, top, z0, z1)
        sx, sy, sz = x1 - x0 + 1, top - PLATE + 1, z1 - z0 + 1

        pal, index, rows = [], {}, []
        for (x, y, z), (nm, props) in sorted(blocks.items()):
            s = spec(nm, props)
            if s not in index:
                index[s] = len(pal)
                pal.append(s)
            rows.append([x - x0, y - PLATE, z - z0, index[s]])

        doc = {
            'source': 'Colorful Medieval Village Assets, variant %d (green roofs)'
                      % VARIANT,
            'what': note,
            'world_origin': {'x': x0, 'y': PLATE, 'z': z0},
            'size': {'x': sx, 'y': sy, 'z': sz},
            # The plate's dirt/grass course is local y 0, and that is the course
            # that wants to land on the terrain surface.
            'ground': 0,
            'front': front,
            'note': 'every non-air block in the box. ground is the local y that'
                    ' sits at the terrain surface; front is the face with the door.',
            'palette': pal,
            'blocks': rows,
        }
        if name in DESCENT:
            dx, dy, dz = DESCENT[name]
            here = blocks.get((x0 + dx, PLATE + dy, z0 + dz))
            # ASSERTED, BECAUSE THE FIRST GUESS WAS UNDER A LECTERN. A descent
            # named by eye off a plan view lands on furniture, and the failure is
            # a ladder through a bookstand in a finished building nobody looks at
            # again. Floor below, three clear above, checked at extraction.
            over = [blocks.get((x0 + dx, PLATE + dy + u, z0 + dz)) for u in (1, 2, 3)]
            print('  descent (%d,%d,%d): floor=%s  above=%s'
                  % (dx, dy, dz, here[0] if here else 'AIR',
                     ', '.join(o[0] if o else 'air' for o in over)))
            assert here and 'brick' in here[0], 'descent is not on a floor'
            assert not any(over), 'descent has something standing on it'
            doc['descent'] = {'x': dx, 'y': dy, 'z': dz}

        path = os.path.join(OUT, name + '.json.gz')
        with gzip.open(path, 'wt', encoding='utf-8') as f:
            json.dump(doc, f, separators=(',', ':'))
        c = collections.Counter(v[0] for v in blocks.values())
        print('%-20s %2dx%-2dx%-2d  %5d blocks  %3d palette  %6d bytes  %s'
              % (name, sx, sy, sz, len(rows), len(pal), os.path.getsize(path),
                 c.most_common(1)[0][0]))


if __name__ == '__main__':
    main()
