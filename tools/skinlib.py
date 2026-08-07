"""Shared 64x64 Minecraft skin plumbing: PNG read/write + face atlas."""
import zlib, struct

def write_png(path, pixels, scale=1):
    h = len(pixels); w = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        for _ in range(scale):
            raw.append(0)
            for px in row:
                raw.extend(bytes(px) * scale)
    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w * scale, h * scale, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)

def read_png(path):
    """Minimal reader: 8-bit RGBA/RGB, non-interlaced."""
    d = open(path, "rb").read()
    assert d[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos, idat, w = 8, bytearray(), None
    while pos < len(d):
        ln = struct.unpack(">I", d[pos:pos + 4])[0]
        tag = d[pos + 4:pos + 8]
        body = d[pos + 8:pos + 8 + ln]
        if tag == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and interlace == 0 and ctype in (2, 6), (depth, ctype, interlace)
        elif tag == b"IDAT":
            idat += body
        pos += 12 + ln
    ch = 4 if ctype == 6 else 3
    raw = zlib.decompress(bytes(idat))
    stride = w * ch
    out, prev = [], bytearray(stride)
    p = 0
    for _ in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        for i in range(stride):
            a = line[i - ch] if i >= ch else 0
            b = prev[i]
            c = prev[i - ch] if i >= ch else 0
            if f == 1:   line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        prev = line
        row = [tuple(line[x * ch:x * ch + ch]) + ((255,) if ch == 3 else ())
               for x in range(w)]
        out.append(row)
    return out

class Face:
    def __init__(self, buf, ox, oy, w, h):
        self.buf, self.ox, self.oy, self.w, self.h = buf, ox, oy, w, h
    def px(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.buf[self.oy + y][self.ox + x] = c
    def fill(self, c):
        for y in range(self.h):
            for x in range(self.w):
                self.px(x, y, c)
    def art(self, lines, palette):
        assert len(lines) == self.h, (self.ox, self.oy, "rows", len(lines))
        for dy, line in enumerate(lines):
            assert len(line) == self.w, (self.ox, self.oy, dy, line)
            for dx, ch in enumerate(line):
                if ch != ".":
                    self.px(dx, dy, palette[ch])

def atlas(buf):
    """name -> Face for the 1.8+ 64x64 layout."""
    def f(x, y, w, h): return Face(buf, x, y, w, h)
    return {
        "head_top": f(8, 0, 8, 8),   "head_bot": f(16, 0, 8, 8),
        "head_r":   f(0, 8, 8, 8),   "head_f":   f(8, 8, 8, 8),
        "head_l":   f(16, 8, 8, 8),  "head_b":   f(24, 8, 8, 8),
        "hat_top":  f(40, 0, 8, 8),  "hat_bot":  f(48, 0, 8, 8),
        "hat_r":    f(32, 8, 8, 8),  "hat_f":    f(40, 8, 8, 8),
        "hat_l":    f(48, 8, 8, 8),  "hat_b":    f(56, 8, 8, 8),
        "body_top": f(20, 16, 8, 4), "body_bot": f(28, 16, 8, 4),
        "body_r":   f(16, 20, 4, 12),"body_f":   f(20, 20, 8, 12),
        "body_l":   f(28, 20, 4, 12),"body_b":   f(32, 20, 8, 12),
        "jac_top":  f(20, 32, 8, 4), "jac_bot":  f(28, 32, 8, 4),
        "jac_r":    f(16, 36, 4, 12),"jac_f":    f(20, 36, 8, 12),
        "jac_l":    f(28, 36, 4, 12),"jac_b":    f(32, 36, 8, 12),
        "rarm_top": f(44, 16, 4, 4), "rarm_bot": f(48, 16, 4, 4),
        "rarm_r":   f(40, 20, 4, 12),"rarm_f":   f(44, 20, 4, 12),
        "rarm_l":   f(48, 20, 4, 12),"rarm_b":   f(52, 20, 4, 12),
        "rsl_top":  f(44, 32, 4, 4), "rsl_bot":  f(48, 32, 4, 4),
        "rsl_r":    f(40, 36, 4, 12),"rsl_f":    f(44, 36, 4, 12),
        "rsl_l":    f(48, 36, 4, 12),"rsl_b":    f(52, 36, 4, 12),
        "larm_top": f(36, 48, 4, 4), "larm_bot": f(40, 48, 4, 4),
        "larm_r":   f(32, 52, 4, 12),"larm_f":   f(36, 52, 4, 12),
        "larm_l":   f(40, 52, 4, 12),"larm_b":   f(44, 52, 4, 12),
        "lsl_top":  f(52, 48, 4, 4), "lsl_bot":  f(56, 48, 4, 4),
        "lsl_r":    f(48, 52, 4, 12),"lsl_f":    f(52, 52, 4, 12),
        "lsl_l":    f(56, 52, 4, 12),"lsl_b":    f(60, 52, 4, 12),
        "rleg_top": f(4, 16, 4, 4),  "rleg_bot": f(8, 16, 4, 4),
        "rleg_r":   f(0, 20, 4, 12), "rleg_f":   f(4, 20, 4, 12),
        "rleg_l":   f(8, 20, 4, 12), "rleg_b":   f(12, 20, 4, 12),
        "rpant_top":f(4, 32, 4, 4),  "rpant_bot":f(8, 32, 4, 4),
        "rpant_r":  f(0, 36, 4, 12), "rpant_f":  f(4, 36, 4, 12),
        "rpant_l":  f(8, 36, 4, 12), "rpant_b":  f(12, 36, 4, 12),
        "lleg_top": f(20, 48, 4, 4), "lleg_bot": f(24, 48, 4, 4),
        "lleg_r":   f(16, 52, 4, 12),"lleg_f":   f(20, 52, 4, 12),
        "lleg_l":   f(24, 52, 4, 12),"lleg_b":   f(28, 52, 4, 12),
        "lpant_top":f(4, 48, 4, 4),  "lpant_bot":f(8, 48, 4, 4),
        "lpant_r":  f(0, 52, 4, 12), "lpant_f":  f(4, 52, 4, 12),
        "lpant_l":  f(8, 52, 4, 12), "lpant_b":  f(12, 52, 4, 12),
    }

OVERLAY_PREFIXES = ("hat", "jac", "rsl", "lsl", "rpant", "lpant")

def validate(F):
    bad = []
    for name, f in F.items():
        ov = name.split("_")[0] in OVERLAY_PREFIXES
        for y in range(f.h):
            for x in range(f.w):
                a = f.buf[f.oy + y][f.ox + x][3]
                if (a != 255 and not ov) or a not in (0, 255):
                    bad.append((name, x, y, a))
    assert not bad, f"{len(bad)} bad pixels, e.g. {bad[:6]}"
