"""Bulk-read a box of world into a dict, decoding each chunk once."""
import os, sys, collections
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import anvil
from anvil import bits_for

def box(tu, x0, x1, y0, y1, z0, z1):
    root = os.path.join(HERE, tu, "region")
    regs, out = {}, {}
    for cx in range(x0 >> 4, (x1 >> 4) + 1):
        for cz in range(z0 >> 4, (z1 >> 4) + 1):
            rk = (cx >> 5, cz >> 5)
            if rk not in regs:
                p = os.path.join(root, "r.%d.%d.mca" % rk)
                regs[rk] = anvil.Region(p) if os.path.exists(p) else None
            reg = regs[rk]
            if reg is None:
                continue
            ch = reg.chunk(cx & 31, cz & 31)
            if ch is None:
                continue
            lvl = ch.get("Level", ch)
            for sec in lvl.get("Sections", []):
                sy = sec["Y"] * 16
                if sy > y1 or sy + 15 < y0:
                    continue
                pal, st = sec.get("Palette"), sec.get("BlockStates")
                if not pal or not st:
                    continue
                bits = bits_for(st, pal)
                idx = anvil.unpack(st, bits, 4096, True)
                for i, v in enumerate(idx):
                    e = pal[v]
                    nm = e.get("Name", "")[10:]
                    if nm in ("air", "cave_air", "void_air"):
                        continue
                    y = sy + (i >> 8)
                    if y < y0 or y > y1:
                        continue
                    x = cx * 16 + (i & 15)
                    z = cz * 16 + ((i >> 4) & 15)
                    if x < x0 or x > x1 or z < z0 or z > z1:
                        continue
                    out[(x, y, z)] = (nm, e.get("Properties") or {})
    return out
