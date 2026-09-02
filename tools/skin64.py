"""Turn a 64x32 player skin into the 64x64 sheet the humanoid model reads.

The old format has one arm and one leg and the game mirrors them. The 64x64
format draws the left limbs separately at (16,48) and (32,48). Vanilla converts
a legacy skin on download with twelve rectangle copies, each mirrored
horizontally — this is that list, verbatim, so the result is what the game
itself would have made of the file.

    python3 tools/skin64.py in.png out.png
"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import pngio

# (x, y, dx, dy, w, h): copy the rect at (x,y) to (x+dx, y+dy), mirrored in x.
COPIES = [
	(4, 16, 16, 32, 4, 4),    # leg top
	(8, 16, 16, 32, 4, 4),    # leg bottom
	(0, 20, 24, 32, 4, 12),   # leg outer  -> left leg's inner
	(4, 20, 16, 32, 4, 12),   # leg front
	(8, 20, 8, 32, 4, 12),    # leg inner  -> left leg's outer
	(12, 20, 16, 32, 4, 12),  # leg back
	(44, 16, -8, 32, 4, 4),   # arm top
	(48, 16, -8, 32, 4, 4),   # arm bottom
	(40, 20, 0, 32, 4, 12),   # arm outer  -> left arm's inner
	(44, 20, -8, 32, 4, 12),  # arm front
	(48, 20, -16, 32, 4, 12), # arm inner  -> left arm's outer
	(52, 20, -8, 32, 4, 12),  # arm back
]

def convert(src, dst):
	w, h, px = pngio.read(src)
	assert (w, h) == (64, 32), 'expected a 64x32 legacy skin, got %dx%d' % (w, h)
	out = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
	for y in range(32):
		for x in range(64):
			out[y][x] = px[y][x]
	for x, y, dx, dy, cw, ch in COPIES:
		for yy in range(ch):
			for xx in range(cw):
				out[y + dy + yy][x + dx + (cw - 1 - xx)] = px[y + yy][x + xx]
	pngio.write(dst, 64, 64, out)
	painted = sum(1 for row in out for c in row if c[3])
	print('%s -> %s  64x64, %d pixels painted' % (os.path.basename(src), dst, painted))
	return out

if __name__ == '__main__':
	convert(sys.argv[1], sys.argv[2])
