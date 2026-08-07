#!/usr/bin/env python3
"""Render a 64x64 Minecraft skin on the classic player model in 3D.

Orthographic raycast against the 12 model boxes (6 base + 6 overlay, the
overlay inflated 0.5px exactly as the game does it), per-limb rotation for a
posed figure, per-face directional shading, and supersampled AA.
Pure Python, no dependencies.

  python3 render3d.py <skin.png> <out.png> [yaw1,yaw2,...]
"""
import sys, math
from skinlib import read_png, write_png, atlas

D2R = math.pi / 180.0

def mat(rx, ry, rz):
    """R = Ry . Rx . Rz applied to a point, as a 3x3 row-major tuple."""
    cx, sx = math.cos(rx * D2R), math.sin(rx * D2R)
    cy, sy = math.cos(ry * D2R), math.sin(ry * D2R)
    cz, sz = math.cos(rz * D2R), math.sin(rz * D2R)
    Rz = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))
    Rx = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    Ry = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    def mul(A, B):
        return tuple(tuple(sum(A[i][k] * B[k][j] for k in range(3))
                           for j in range(3)) for i in range(3))
    return mul(Ry, mul(Rx, Rz))

def apply(M, v):
    return (M[0][0] * v[0] + M[0][1] * v[1] + M[0][2] * v[2],
            M[1][0] * v[0] + M[1][1] * v[1] + M[1][2] * v[2],
            M[2][0] * v[0] + M[2][1] * v[1] + M[2][2] * v[2])

def transpose(M):
    return tuple(tuple(M[j][i] for j in range(3)) for i in range(3))

def fs(p):
    return {"front": f"{p}_f", "back": f"{p}_b", "right": f"{p}_r",
            "left": f"{p}_l", "top": f"{p}_top", "bottom": f"{p}_bot"}

def box(name, pos, size, pivot=None, rot=(0, 0, 0), inflate=0.0):
    x, y, z = pos; sx, sy, sz = size
    if inflate:
        x -= inflate; y -= inflate; z -= inflate
        sx += 2 * inflate; sy += 2 * inflate; sz += 2 * inflate
    R = mat(*rot)
    return dict(name=name, lo=(x, y, z), hi=(x + sx, y + sy, z + sz),
                f=fs(name), pivot=pivot or (0, 0, 0), R=R, Rt=transpose(R),
                rotated=rot != (0, 0, 0))

# y up, feet at y=0, +z front, the character's right is -x.
# A loose, weight-on-one-hip stance — nothing symmetrical.
HEAD_P, HEAD_R = (0, 24, 0), (-3, -14, 2)
RARM_P, RARM_R = (-4, 23, 0), (-9, 0, -5)
LARM_P, LARM_R = (4, 23, 0), (5, 0, 6)
RLEG_P, RLEG_R = (-2, 12, 0), (5, 0, -2)
LLEG_P, LLEG_R = (2, 12, 0), (-4, 0, 2)

PARTS = [
    box("head",  (-4, 24, -4), (8, 8, 8),  HEAD_P, HEAD_R),
    box("body",  (-4, 12, -2), (8, 12, 4)),
    box("rarm",  (-8, 12, -2), (4, 12, 4), RARM_P, RARM_R),
    box("larm",  (4, 12, -2),  (4, 12, 4), LARM_P, LARM_R),
    box("rleg",  (-4, 0, -2),  (4, 12, 4), RLEG_P, RLEG_R),
    box("lleg",  (0, 0, -2),   (4, 12, 4), LLEG_P, LLEG_R),
    box("hat",   (-4, 24, -4), (8, 8, 8),  HEAD_P, HEAD_R, inflate=0.5),
    box("jac",   (-4, 12, -2), (8, 12, 4), None, (0, 0, 0), inflate=0.5),
    box("rsl",   (-8, 12, -2), (4, 12, 4), RARM_P, RARM_R, inflate=0.5),
    box("lsl",   (4, 12, -2),  (4, 12, 4), LARM_P, LARM_R, inflate=0.5),
    box("rpant", (-4, 0, -2),  (4, 12, 4), RLEG_P, RLEG_R, inflate=0.5),
    box("lpant", (0, 0, -2),   (4, 12, 4), LLEG_P, LLEG_R, inflate=0.5),
]

SHADE = {"top": 1.00, "front": 0.92, "left": 0.82,
         "right": 0.70, "back": 0.60, "bottom": 0.44}

AXIS_FACE = {(0, -1): "right", (0, 1): "left",
             (1, -1): "bottom", (1, 1): "top",
             (2, -1): "back", (2, 1): "front"}

def uv_for(face, lo, hi, p):
    x, y, z = p; x0, y0, z0 = lo; x1, y1, z1 = hi
    sx, sy, sz = x1 - x0, y1 - y0, z1 - z0
    if face == "front":   return (x - x0) / sx, (y1 - y) / sy
    if face == "back":    return (x1 - x) / sx, (y1 - y) / sy
    if face == "right":   return (z - z0) / sz, (y1 - y) / sy
    if face == "left":    return (z1 - z) / sz, (y1 - y) / sy
    if face == "top":     return (x - x0) / sx, (z - z0) / sz
    return (x - x0) / sx, (z1 - z) / sz

def render(tex, F, yaw, pitch, w, h, ss=2, bg_top=(46, 50, 62),
           bg_bot=(20, 22, 28)):
    V = mat(pitch, yaw, 0)      # world -> camera
    Vt = transpose(V)           # camera -> world
    d_cam = (0.0, 0.0, -1.0)
    d_world = apply(Vt, d_cam)

    W, H = w * ss, h * ss
    half_w, top_y, bot_y = 13.5, 35.5, -1.5

    prep = []
    for b in PARTS:
        d_local = apply(b["Rt"], d_world) if b["rotated"] else d_world
        inv = [1e30 if abs(c) < 1e-12 else 1.0 / c for c in d_local]
        # screen bbox of the (rotated) corners
        xs, ys = [], []
        for i in range(8):
            c = (b["lo"][0] if i & 1 else b["hi"][0],
                 b["lo"][1] if i & 2 else b["hi"][1],
                 b["lo"][2] if i & 4 else b["hi"][2])
            if b["rotated"]:
                p = b["pivot"]
                c = tuple(apply(b["R"], (c[0] - p[0], c[1] - p[1], c[2] - p[2]))[k]
                          + p[k] for k in range(3))
            cc = apply(V, c)
            xs.append(cc[0]); ys.append(cc[1])
        prep.append((b, d_local, inv, min(xs), max(xs), min(ys), max(ys)))

    img = [[None] * W for _ in range(H)]
    for py in range(H):
        wy = top_y - (py + 0.5) * (top_y - bot_y) / H
        row = img[py]
        for px in range(W):
            wx = -half_w + (px + 0.5) * (2 * half_w) / W
            o_world = apply(Vt, (wx, wy, 60.0))
            hits = []
            for b, dl, inv, bx0, bx1, by0, by1 in prep:
                if wx < bx0 or wx > bx1 or wy < by0 or wy > by1:
                    continue
                if b["rotated"]:
                    p = b["pivot"]
                    ol = apply(b["Rt"], (o_world[0] - p[0], o_world[1] - p[1],
                                         o_world[2] - p[2]))
                    ol = (ol[0] + p[0], ol[1] + p[1], ol[2] + p[2])
                else:
                    ol = o_world
                lo, hi = b["lo"], b["hi"]
                tmin, tmax, axis, sign, ok = -1e30, 1e30, 0, 1, True
                for a in range(3):
                    t1 = (lo[a] - ol[a]) * inv[a]
                    t2 = (hi[a] - ol[a]) * inv[a]
                    s = -1
                    if t1 > t2:
                        t1, t2 = t2, t1
                        s = 1
                    if t1 > tmin:
                        tmin, axis, sign = t1, a, s
                    if t2 < tmax:
                        tmax = t2
                    if tmin > tmax:
                        ok = False
                        break
                if ok and tmax > 0:
                    hits.append((tmin, b, axis, sign, ol, dl))
            if not hits:
                continue
            hits.sort(key=lambda t: t[0])
            for t, b, axis, sign, ol, dl in hits:
                p = (ol[0] + dl[0] * t, ol[1] + dl[1] * t, ol[2] + dl[2] * t)
                fname = AXIS_FACE[(axis, sign)]
                u, v = uv_for(fname, b["lo"], b["hi"], p)
                face = F[b["f"][fname]]
                tx = min(face.w - 1, max(0, int(u * face.w)))
                ty = min(face.h - 1, max(0, int(v * face.h)))
                c = tex[face.oy + ty][face.ox + tx]
                if c[3] == 0:
                    continue
                k = SHADE[fname]
                row[px] = (int(c[0] * k), int(c[1] * k), int(c[2] * k))
                break

    out = []
    for y in range(h):
        line = []
        for x in range(w):
            f = y / max(1, h - 1)
            bgc = tuple(int(bg_top[i] + (bg_bot[i] - bg_top[i]) * f)
                        for i in range(3))
            r = g = bl = 0
            for j in range(ss):
                for i in range(ss):
                    c = img[y * ss + j][x * ss + i] or bgc
                    r += c[0]; g += c[1]; bl += c[2]
            n = ss * ss
            line.append((r // n, g // n, bl // n, 255))
        out.append(line)
    return out

def main():
    skin, out_path = sys.argv[1], sys.argv[2]
    yaws = [float(y) for y in sys.argv[3].split(",")] if len(sys.argv) > 3 \
        else [-32, 20, 160]
    tex = read_png(skin)
    F = atlas(tex)
    w, h = 230, 360
    panels = [render(tex, F, yaw, 10, w, h, ss=3) for yaw in yaws]
    gap = 10
    total = w * len(panels) + gap * (len(panels) - 1)
    canvas = [[(20, 22, 28, 255)] * total for _ in range(h)]
    for i, p in enumerate(panels):
        ox = i * (w + gap)
        for y in range(h):
            canvas[y][ox:ox + w] = p[y]
    write_png(out_path, canvas)
    print("wrote", out_path, f"({total}x{h}, yaws={yaws})")

if __name__ == "__main__":
    main()
