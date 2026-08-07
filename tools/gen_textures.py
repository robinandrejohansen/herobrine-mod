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

TWO GLOW COLOURS, TWO MEANINGS — see LORE.md:

    WHITE   what is left of the brother. Only ever his eyes.
    VIOLET  the thing wearing him, bleeding through the cracks.

The balance is the story. Right now the eyes dominate and the violet is a
few thin fissures; at later phases the violet spreads until the white is
nearly gone, and the player watches him disappear without being told that is
what they are watching.

Marker characters carry position only; each output picks its own colour:

    3  eye         dark socket      -> white
    1  crack core  near-black       -> bright violet
    2  crack edge  near-black       -> dim violet
    4  palm        shadowed skin    -> violet

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
    "3": C("14100C"),
    "1": C("2A1533"),
    "2": C("1C0F22"),
    "4": C("8A6242"),
    "-": CLEAR,
}
GLOW = {k: CLEAR for k in LIT}
GLOW.update({
    "3": C("FFFFFF"),   # him
    "1": C("E0B6FF"),   # it
    "2": C("9B5FD6"),
    "4": C("B87FE8"),
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

# ============================== BODY: one thin fissure, not a lightning storm
paint("body_f", [
    "EssssssE",   # collar
    "SSSSSSSS",
    "SSS1SSSS",
    "SSS2SSSS",
    "SSSS1SSS",
    "SSSS2SSS",
    "SSS1SSSS",
    "SSS2SSSS",
    "SSSSSSSS",
    "SSSSSSSS",
    "ESSSSSSE",
    "ESSSSSSE",
])
paint("body_b", [
    "EssssssE", "SSSSSSSS", "SSSS1SSS", "SSSS2SSS",
    "SSS1SSSS", "SSS2SSSS", "SSSS1SSS", "SSSS2SSS",
    "SSSSSSSS", "SSSSSSSS", "ESSSSSSE", "ESSSSSSE",
])
for f in ("body_r", "body_l"):
    paint(f, ["EssE", "SSSS", "SSSS", "SS1S", "SS2S", "SSSS",
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

# ================================== ARMS: short sleeves, and it is in his hands
arm =     ["EssE", "SSSS", "SS1S", "SS2S", "ESSE", "nnnn",
           "nnnn", "nnnn", "nnnn", "nnnn", "n44n", "N44N"]
arm_alt = ["EssE", "SSSS", "S1SS", "S2SS", "ESSE", "nnnn",
           "nnnn", "nnnn", "nnnn", "nnnn", "n44n", "N44N"]
for pre in ("rarm", "larm"):
    paint(f"{pre}_f", arm)
    paint(f"{pre}_b", arm_alt)
    paint(f"{pre}_r", arm_alt)
    paint(f"{pre}_l", arm)
    paint(f"{pre}_top", ["EssE", "sSSs", "sSSs", "ESSE"])
    paint(f"{pre}_bot", ["N44N", "4334", "4334", "N44N"])

# ================================================= LEGS: jeans and grey shoes
leg =     ["JJJJ", "JjjJ", "JJ1J", "JJ2J", "JJJJ", "JJJJ",
           "JJJJ", "JJJJ", "JJJJ", "IIII", "GGGG", "GggG"]
leg_alt = ["JJJJ", "JjjJ", "J1JJ", "J2JJ", "JJJJ", "JJJJ",
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
violet = sum(1 for row in glow for px in row
             if px[3] == 255 and px != C("FFFFFF"))
assert eyes and violet, "expected both white and violet emissive pixels"

out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src", "main", "resources", "assets", "herobrine",
                   "textures", "entity")
os.makedirs(out, exist_ok=True)
write_png(os.path.join(out, "herobrine.png"), lit)
write_png(os.path.join(out, "herobrine_eyes.png"), glow)
print(f"wrote herobrine.png + herobrine_eyes.png "
      f"({eyes} white eye px, {violet} violet corruption px)")
