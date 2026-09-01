"""Minimal NBT + Anvil reader. Enough to walk a 1.13-era world's blocks."""
import gzip, zlib, struct, io, math

END,BYTE,SHORT,INT,LONG,FLOAT,DOUBLE,BYTE_ARRAY,STRING,LIST,COMPOUND,INT_ARRAY,LONG_ARRAY = range(13)

class R:
    def __init__(s, b): s.b, s.i = b, 0
    def take(s, n):
        v = s.b[s.i:s.i+n]; s.i += n
        if len(v) != n: raise EOFError
        return v
    def u1(s):  return s.take(1)[0]
    def i1(s):  return struct.unpack(">b", s.take(1))[0]
    def i2(s):  return struct.unpack(">h", s.take(2))[0]
    def u2(s):  return struct.unpack(">H", s.take(2))[0]
    def i4(s):  return struct.unpack(">i", s.take(4))[0]
    def i8(s):  return struct.unpack(">q", s.take(8))[0]
    def f4(s):  return struct.unpack(">f", s.take(4))[0]
    def f8(s):  return struct.unpack(">d", s.take(8))[0]
    def st(s):  return s.take(s.u2()).decode("utf-8", "replace")

def payload(r, t):
    if t == BYTE:   return r.i1()
    if t == SHORT:  return r.i2()
    if t == INT:    return r.i4()
    if t == LONG:   return r.i8()
    if t == FLOAT:  return r.f4()
    if t == DOUBLE: return r.f8()
    if t == BYTE_ARRAY: return r.take(r.i4())
    if t == STRING: return r.st()
    if t == LIST:
        et, n = r.u1(), r.i4()
        return [payload(r, et) for _ in range(max(0, n))]
    if t == COMPOUND:
        out = {}
        while True:
            tt = r.u1()
            if tt == END: return out
            # NAME FIRST, ON ITS OWN LINE. `out[r.st()] = payload(r, tt)` reads the
            # payload BEFORE the key, because Python evaluates the right-hand side
            # of an assignment first — which desyncs the stream one tag in and
            # surfaces as a nonsense tag id much later.
            name = r.st()
            out[name] = payload(r, tt)
    if t == INT_ARRAY:
        n = r.i4()
        return list(struct.unpack(">%di" % n, r.take(4 * n)))
    if t == LONG_ARRAY:
        n = r.i4()
        return list(struct.unpack(">%dq" % n, r.take(8 * n)))
    raise ValueError("tag %d" % t)

def parse(raw):
    r = R(raw)
    t = r.u1()
    if t == END: return {}
    r.st()
    return payload(r, t)

def read_dat(path):
    with open(path, "rb") as f: b = f.read()
    if b[:2] == b"\x1f\x8b": b = gzip.decompress(b)
    return parse(b)

class Region:
    """One .mca file: 32x32 chunks."""
    def __init__(s, path):
        with open(path, "rb") as f: s.b = f.read()
    def chunk(s, cx, cz):
        """cx,cz are 0..31 within the region."""
        i = 4 * ((cx & 31) + (cz & 31) * 32)
        off = int.from_bytes(s.b[i:i+3], "big") * 4096
        cnt = s.b[i+3]
        if off == 0 or cnt == 0: return None
        ln = int.from_bytes(s.b[off:off+4], "big")
        comp = s.b[off+4]
        data = s.b[off+5:off+4+ln]
        if comp == 1: data = gzip.decompress(data)
        elif comp == 2: data = zlib.decompress(data)
        elif comp == 3: pass
        else: return None
        return parse(data)

def bits_for(states, palette):
    """Bits per block, taken from the ARRAY LENGTH rather than the palette size.

    The palette-size guess — max(4, ceil(log2(len(palette)))) — is what the format
    is documented as and it is not what every chunk on disk actually holds. A world
    that has been through an upgrade can carry a section whose array was written at
    a wider stride than its palette now needs, and then the guess reads indices
    that fall off the end of the palette. The array length cannot lie: pre-1.16
    packs 4096 values with no gaps, so the width is exactly len*64/4096.
    """
    if not states:
        return 4
    return max(4, len(states) * 64 // 4096)


def unpack(states, bits, count, spanning):
    """Long-array -> list of palette indices."""
    out = []
    if spanning:
        # MASK TO THE BITS THAT ACTUALLY EXIST IN THIS WORD.
        #
        # The obvious version — (states[j] >> off) & mask — is wrong on every value
        # that straddles a word boundary, and wrong SILENTLY. struct '>q' hands back
        # signed longs, Python's >> on a negative sign-extends for ever, and a mask
        # wider than the bits remaining in the word then reads those phantom ones as
        # real data. At off 62 with a 5-bit stride only two bits are genuine and the
        # other three come back set, so a 17-entry palette gets asked for index 31.
        for i in range(count):
            start = i * bits
            j, off = start >> 6, start & 63
            avail = 64 - off
            v = (states[j] >> off) & ((1 << min(bits, avail)) - 1)
            if bits > avail and j + 1 < len(states):
                v |= (states[j+1] & ((1 << (bits - avail)) - 1)) << avail
            out.append(v)
    else:
        per = 64 // bits
        mask = (1 << bits) - 1
        for j in range(len(states)):
            w = states[j]
            for k in range(per):
                if len(out) >= count: break
                out.append((w >> (k * bits)) & mask)
    return out

def sections(chunk):
    """[(Y, palette, indices)] for a 1.13-era chunk."""
    lvl = chunk.get("Level", chunk)
    out = []
    for sec in lvl.get("Sections", []):
        pal = sec.get("Palette")
        st = sec.get("BlockStates")
        if not pal or not st:
            continue
        bits = bits_for(st, pal)
        out.append((sec["Y"], pal, unpack(st, bits, 4096, True)))
    return out

def name_of(entry):
    return entry.get("Name", "?")
