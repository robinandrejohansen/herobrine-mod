#!/usr/bin/env python3
"""Generates the two Herobrine entity textures from one source of truth.

He should read as Steve — same build, same clothes, same person — and be
wrong. Uncanny beats monstrous here: a shambling corpse is a mob, but a man
standing in your valley with your posture and no eyes is a person, and people
are frightening. So the skin is deliberately ordinary: warm skin, brown hair,
a shirt and jeans. The shirt is oxblood rather than Steve's cyan because he is
not Steve, and because of what he did with his hands.

Minecraft has no emissive channel on an entity texture, so glow is a second
texture drawn over the model by an EyesLayer at full brightness (the vanilla
Enderman and spider pattern). Both files come from the art below so they
cannot drift out of alignment.

ONLY THE EYES GLOW.

An earlier version bled violet corruption through cracks in his skin and
clothes. It read as magic, and magic is not frightening — it files him under
"enchanted thing" alongside every other glowing object in the game. An
ordinary man whose eyes are wrong has nowhere to be filed, which is the whole
effect.

Corruption belongs to the late phases, as a texture swap once he is openly
hunting and the pretence of being a person is gone. Not the baseline.

Marker characters carry position only; each output picks its own colour:

    3  eye   dark socket in light  ->  pure white, full bright

Run:  python3 tools/gen_textures.py
"""
import os
from skinlib import write_png, atlas

W = 64
CLEAR = (0, 0, 0, 0)

def C(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

LIT = {
    # skin — warm and ordinary, very slightly drained
    "n": C("C08E5E"), "l": C("D6A377"), "N": C("9A6E45"), "D": C("7A5636"),
    "d": C("A6784E"),
    # hair — Steve brown
    "H": C("3F2B1E"), "h": C("59402C"), "K": C("2A1C13"),
    "b": C("2E1F14"), "m": C("6B4230"),
    # oxblood shirt
    "S": C("6E1F22"), "s": C("8A2C2F"), "E": C("4A1417"),
    # jeans
    "J": C("3B4468"), "j": C("4A5580"), "I": C("2A3050"),
    # shoes
    "G": C("4A4A4A"), "g": C("5A5A5A"),
    # markers as they appear in normal light
    "3": C("14100C"),   # eye socket
    "-": CLEAR,
}
GLOW = {k: CLEAR for k in LIT}
GLOW.update({
    "3": C("FFFFFF"),   # the eyes, and nothing else
})

lit = [[CLEAR] * W for _ in range(W)]
glow = [[CLEAR] * W for _ in range(W)]
L, G = atlas(lit), atlas(glow)

def paint(face, art):
    L[face].art(art, LIT)
    G[face].art(art, GLOW)

def paint_mirrored(face, art):
    flipped = [r[::-1] for r in art]
    L[face].art(flipped, LIT)
    G[face].art(flipped, GLOW)

# ===================================================================== HEAD
paint("head_f", [
    "HHHHHHHH",
    "HHHHHHHH",
    "HbbnnbbH",   # hairline and a heavy brow
    "n33nn33n",   # the eyes
    "nnnNNnnn",   # nose
    "nnnnnnnn",
    "nndmmdnn",   # a flat mouth — no expression at all
    "NnnnnnnN",
])
paint("head_top", [
    "KHHHHHHK", "HHhhhhHH", "HhHHHHhH", "HHhHHhHH",
    "HhHHHHhH", "HHhHHhHH", "HhHHHHhH", "KHHHHHHK",
])
head_side = [
    "HHHHHHHH", "HHHHHHHH", "HHHHHHHH", "HHHHHHhn",
    "nnnnnnnn", "nnnnnnnn", "nnnnnnnN", "DnnnnnnD",
]
paint("head_r", head_side)
paint_mirrored("head_l", head_side)
paint("head_b", [
    "HHHHHHHH", "HHhhhhHH", "HhHHHHhH", "HHHHHHHH",
    "HHHHHHHH", "HHHHHHHH", "KHHHHHHK", "DnnnnnnD",
])
paint("head_bot", [
    "DDDDDDDD", "DNNNNNND", "DNnnnnND", "DNnnnnND",
    "DNnnnnND", "DNnnnnND", "DNNNNNND", "DDDDDDDD",
])

# hat layer: just enough hair to break the square silhouette
paint("hat_f", [
    "KHHHHHHK", "Kh----hK", "--------", "--------",
    "--------", "--------", "--------", "--------",
])
hat_side = [
    "KHHHHHHK", "KhhhhhhK", "HHhhhhHH", "-HH--HH-",
    "--------", "--------", "--------", "--------",
]
paint("hat_r", hat_side)
paint_mirrored("hat_l", hat_side)
paint("hat_b", [
    "KHHHHHHK", "KhhhhhhK", "HHhhhhHH", "-H-HH-H-",
    "--------", "--------", "--------", "--------",
])
paint("hat_top", [
    "KHHHHHHK", "HHhhhhHH", "HhHHHHhH", "HHhHHhHH",
    "HhHHHHhH", "HHhHHhHH", "HhHHHHhH", "KHHHHHHK",
])
L["hat_bot"].fill(CLEAR); G["hat_bot"].fill(CLEAR)

# ================================================ BODY: just a shirt
paint("body_f", [
    "EssssssE",   # collar
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "ESSSSSSE",
    "ESSSSSSE",
])
paint("body_b", [
    "EssssssE", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS",
    "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS",
    "SSSSSSSS", "SSSSSSSS", "ESSSSSSE", "ESSSSSSE",
])
for f in ("body_r", "body_l"):
    paint(f, ["EssE", "SSSS", "SSSS", "SSSS", "SSSS", "SSSS",
              "SSSS", "SSSS", "SSSS", "SSSS", "ESSE", "ESSE"])
paint("body_top", ["EssssssE", "sSSSSSSs", "sSSSSSSs", "ESSSSSSE"])
paint("body_bot", ["ESSSSSSE", "SSSSSSSS", "SSSSSSSS", "ESSSSSSE"])

# No shroud. He wears what a person wears — that is the whole point.
for f in ("jac_f", "jac_b", "jac_r", "jac_l", "jac_top", "jac_bot",
          "rsl_f", "rsl_b", "rsl_r", "rsl_l", "rsl_top", "rsl_bot",
          "lsl_f", "lsl_b", "lsl_r", "lsl_l", "lsl_top", "lsl_bot",
          "rpant_f", "rpant_b", "rpant_r", "rpant_l", "rpant_top", "rpant_bot",
          "lpant_f", "lpant_b", "lpant_r", "lpant_l", "lpant_top", "lpant_bot"):
    L[f].fill(CLEAR); G[f].fill(CLEAR)

# ============================================ ARMS: short sleeves, bare hands
arm =     ["EssE", "SSSS", "SSSS", "SSSS", "ESSE", "nnnn",
           "nnnn", "nnnn", "nnnn", "nnnn", "nnnn", "NnnN"]
arm_alt = ["EssE", "SSSS", "SSSS", "SSSS", "ESSE", "nnnn",
           "nnnn", "nnnn", "nnnn", "nnnn", "nnnn", "NnnN"]
for pre in ("rarm", "larm"):
    paint(f"{pre}_f", arm)
    paint(f"{pre}_b", arm_alt)
    paint(f"{pre}_r", arm_alt)
    paint(f"{pre}_l", arm)
    paint(f"{pre}_top", ["EssE", "sSSs", "sSSs", "ESSE"])
    paint(f"{pre}_bot", ["NnnN", "nnnn", "nnnn", "NnnN"])

# ================================================= LEGS: jeans and grey shoes
leg =     ["JJJJ", "JjjJ", "JJJJ", "JJJJ", "JJJJ", "JJJJ",
           "JJJJ", "JJJJ", "JJJJ", "IIII", "GGGG", "GggG"]
leg_alt = ["JJJJ", "JjjJ", "JJJJ", "JJJJ", "JJJJ", "JJJJ",
           "JJJJ", "JJJJ", "JJJJ", "IIII", "GGGG", "GggG"]
for pre in ("rleg", "lleg"):
    paint(f"{pre}_f", leg)
    paint(f"{pre}_b", leg_alt)
    paint(f"{pre}_r", leg_alt)
    paint(f"{pre}_l", leg)
    paint(f"{pre}_top", ["JJJJ", "JjjJ", "JjjJ", "JJJJ"])
    paint(f"{pre}_bot", ["GGGG", "GggG", "GggG", "GGGG"])

# ---------------------------------------------------------------- checks
BASE = [n for n in L if n.split("_")[0] not in
        ("hat", "jac", "rsl", "lsl", "rpant", "lpant")]
bad = [(n, x, y) for n in BASE for y in range(L[n].h) for x in range(L[n].w)
       if lit[L[n].oy + y][L[n].ox + x][3] != 255]
assert not bad, f"{len(bad)} transparent pixels in the lit base layer, e.g. {bad[:5]}"

eyes = sum(1 for row in glow for px in row if px == C("FFFFFF"))
other = sum(1 for row in glow for px in row
            if px[3] == 255 and px != C("FFFFFF"))
assert eyes, "no emissive eye pixels"
assert not other, f"{other} non-eye emissive pixels — only the eyes may glow"

# ---------------------------------------------------------------- intensity
#
# The eyes pipeline blends with TRANSLUCENT, so the alpha here is the whole
# dial: it is the difference between eyes that are lit and eyes that are
# LIGHTS.
#
# This sat at 150 for a long while, on the argument that anything brighter
# read as two lamps set into the face and looked like it was emitting. That
# was judged at arm's length in an inventory screen, and it was the wrong
# place to judge it: he is SEEN at forty to seventy blocks, where four pixels
# at fifty-nine per cent alpha are not subtle, they are absent. The whole
# event is a figure you cannot quite explain, and the eyes are the only part
# of him that says so.
#
# So: near-full on the eyes themselves, and a faint ring around them. The ring
# is what stops the brightness reading as lamps — a hard-edged bright square
# looks like a light source stuck on the face, whereas the same brightness
# with a little bleed around it looks like it is coming from inside him. It
# also survives distance far better, because when the eye itself is down to
# one screen pixel the halo is still carrying the shape.
#
# Turned up after the checks above rather than before, so the assertions still
# compare against a clean marker colour.
EYE = (255, 255, 255, 240)
HALO = (232, 236, 240, 60)

sockets = [(x, y) for y, row in enumerate(glow)
           for x, px in enumerate(row) if px == C("FFFFFF")]
for x, y in sockets:
    glow[y][x] = EYE

# One pixel of bleed, and NOT into the gap between the eyes.
#
# A plain eight-neighbour ring was tried and joined them: the sockets are two
# pixels apart, so the two halos met in the middle and the whole thing read as
# a single lit band across the face — a visor, not eyes. It is a good example
# of a rule that is right per-pixel and wrong on the model.
#
# So the bleed goes up, down, and outward only. A horizontal neighbour is
# refused if there is another socket within three pixels that way, which keeps
# the bridge clear without any coordinate here having to know where the eyes
# happen to be.
socket_set = set(sockets)
halo_at = set()
for x, y in sockets:
    halo_at.add((x, y - 1))
    halo_at.add((x, y + 1))
    for step in (-1, 1):
        if not any((x + step * n, y) in socket_set for n in (1, 2, 3)):
            halo_at.add((x + step, y))

for x, y in sorted(halo_at):
    # Eyes sit on row 11 of the head's front face, so every one of these is
    # inside the 8..15 square. Asserted rather than trusted: a halo leaking
    # onto the head's side face would show up as a glowing stripe down his
    # temple, and it would look deliberate.
    assert 8 <= x <= 15 and 8 <= y <= 15, \
        f"halo at ({x},{y}) is outside the head's front face"
    if (x, y) not in socket_set and glow[y][x][3] == 0:
        glow[y][x] = HALO

out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src", "main", "resources", "assets", "herobrine",
                   "textures", "entity")
os.makedirs(out, exist_ok=True)
write_png(os.path.join(out, "herobrine.png"), lit)
write_png(os.path.join(out, "herobrine_eyes.png"), glow)
halo = sum(1 for row in glow for px in row if px == HALO)
print(f"wrote herobrine.png + herobrine_eyes.png ({eyes} eye px at alpha {EYE[3]}, {halo} halo px at {HALO[3]})")
