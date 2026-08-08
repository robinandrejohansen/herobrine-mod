"""Minimal PNG read/write. No Pillow anywhere on this machine, and a mod that
needs one Python package to build its textures is a mod nobody else can build."""
import struct, zlib


def read(path):
	data = open(path, 'rb').read()
	assert data[:8] == b'\x89PNG\r\n\x1a\n', path
	pos, idat, w, h, depth, ctype, plte, trns = 8, b'', 0, 0, 0, 0, None, None
	while pos < len(data):
		(length,) = struct.unpack('>I', data[pos:pos + 4])
		kind = data[pos + 4:pos + 8]
		body = data[pos + 8:pos + 8 + length]
		if kind == b'IHDR':
			w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
		elif kind == b'PLTE':
			plte = body
		elif kind == b'tRNS':
			trns = body
		elif kind == b'IDAT':
			idat += body
		pos += 12 + length
	assert depth == 8, 'only 8-bit PNGs'
	channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
	raw = zlib.decompress(idat)
	stride = w * channels
	rows, prev, at = [], bytearray(stride), 0
	for _ in range(h):
		f = raw[at]; at += 1
		line = bytearray(raw[at:at + stride]); at += stride
		for i in range(stride):
			a = line[i - channels] if i >= channels else 0
			b = prev[i]
			c = prev[i - channels] if i >= channels else 0
			if f == 1: line[i] = (line[i] + a) & 255
			elif f == 2: line[i] = (line[i] + b) & 255
			elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
			elif f == 4:
				p = a + b - c
				pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
				line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
		rows.append(line); prev = line
	px = [[(0, 0, 0, 0)] * w for _ in range(h)]
	for y in range(h):
		line = rows[y]
		for x in range(w):
			o = x * channels
			if ctype == 6: px[y][x] = tuple(line[o:o + 4])
			elif ctype == 2: px[y][x] = (line[o], line[o + 1], line[o + 2], 255)
			elif ctype == 3:
				i = line[o]
				px[y][x] = (plte[i * 3], plte[i * 3 + 1], plte[i * 3 + 2],
				            trns[i] if trns and i < len(trns) else 255)
			elif ctype == 0: px[y][x] = (line[o],) * 3 + (255,)
			else: px[y][x] = (line[o],) * 3 + (line[o + 1],)
	return w, h, px


def write(path, w, h, px):
	raw = bytearray()
	for y in range(h):
		raw.append(0)
		for x in range(w):
			raw.extend(px[y][x])
	def chunk(kind, body):
		return (struct.pack('>I', len(body)) + kind + body
		        + struct.pack('>I', zlib.crc32(kind + body) & 0xFFFFFFFF))
	open(path, 'wb').write(
		b'\x89PNG\r\n\x1a\n'
		+ chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
		+ chunk(b'IDAT', zlib.compress(bytes(raw), 9))
		+ chunk(b'IEND', b''))
