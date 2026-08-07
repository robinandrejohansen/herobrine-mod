#!/usr/bin/env python3
"""Generates the two Herobrine entity textures from one source of truth.

Minecraft renders emissive detail as a SECOND texture drawn over the model by
an EyesLayer at full brightness, ignoring world light (this is how vanilla does
Enderman, spider and phantom eyes). So the same art has to produce two files:

  herobrine.png       the lit texture — ashen skin, dark hair, rotted clothes.
                      Eyes are dark sockets and the energy cracks are dark
                      fractures, so he still reads correctly in daylight.

  herobrine_eyes.png  the emissive texture — fully transparent EXCEPT the eyes,
                      the crack cores and the palms. These are the only pixels
                      that glow, and they glow at full brightness in a pitch
                      black cave.

Marker characters carry position only; each output picks its own colour for
them, which keeps the two textures perfectly aligned by construction:

    3  eye        dark socket        -> pure white
    1  crack core dark fracture      -> bright cyan-white
    2  crack edge slightly less dark -> dim cyan
    4  palm       shadowed skin      -> bright cyan-white

Run:  python3 tools/gen_textures.py
"""
import os
from skinlib import write_png, atlas

W = 64
CLEAR = (0, 0, 0, 0)

def C(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

# ---------------------------------------------------------------- palettes
# Everything that is not a marker renders normally in the lit texture and is
# transparent in the emissive one.
LIT = {
    "H": C("241F1B"), "h": C("322B25"), "K": C("14100E"),          # hair
    "s": C("B3A99E"), "d": C("8B8177"), "D": C("6B6259"),          # ashen skin
    "u": C("3A3430"),                                              # mouth
    "T": C("1E5F63"), "t": C("2B7C80"), "E": C("123E42"),          # rotted shirt
    "J": C("2B3358"), "j": C("3A4573"), "I": C("1B2140"),          # jeans
    "N": C("14161C"), "n": C("1E212A"),                            # shroud
    "B": C("33333A"), "b": C("4A4A52"), "e": C("4A3A2A"),          # boots, belt
    # markers, as they appear in normal light
    "3": C("0A0C0F"),   # eye socket
    "1": C("0B1014"),   # fracture core
    "2": C("141A20"),   # fracture edge
    "4": C("8B8177"),   # palm, just shadowed skin
    "-": CLEAR,
}
GLOW = {k: CLEAR for k in LIT}
GLOW.update({
    "3": C("FFFFFF"),   # eyes: pure white
    "1": C("BFF4FF"),   # crack core
    "2": C("4FB8D4"),   # crack falloff
    "4": C("D8F8FF"),   # palms
})

lit = [[CLEAR] * W for _ in range(W)]
glow = [[CLEAR] * W for _ in range(W)]
L, G = atlas(lit), atlas(glow)

def paint(face, art):
    """Paint one face into both textures from a single art block."""
    L[face].art(art, LIT)
    G[face].art(art, GLOW)

# ===================================================================== HEAD
paint("head_f", [
    "HHHHHHHH",
    "HHhhhhHH",
    "ssssssss",
    "s33ss33s",   # the eyes
    "ssssssss",
    "sdssssds",
    "ssduudss",   # grim mouth
    "dssssssd",
])
paint("head_top", [
    "KHHHHHHK", "HHhhhhHH", "HhHHHHhH", "HHhHHhHH",
    "HhHHHHhH", "HHhHHhHH", "HhHHHHhH", "KHHHHHHK",
])
side = [
    "HHHHHHHH", "HHhhhhHH", "ssssssds", "ssssssds",
    "ssssssds", "sdssssds", "ssssssdd", "dsssssdd",
]
paint("head_r", side)
L["head_l"].art([r[::-1] for r in side], LIT)
G["head_l"].art([r[::-1] for r in side], GLOW)
paint("head_b", [
    "HHHHHHHH", "HHhhhhHH", "HhHHHHhH", "HHHHHHHH",
    "HHHHHHHH", "sdssssds", "ssssssss", "dssssssd",
])
paint("head_bot", [
    "DDDDDDDD", "DddddddD", "DdssssdD", "DdssssdD",
    "DdssssdD", "DdssssdD", "DddddddD", "DDDDDDDD",
])

# hat layer: hair silhouette only. No painted halo any more — the EyesLayer
# does the glow now, so the geometry stays clean.
paint("hat_f", [
    "KHHHHHHK", "Kh----hK", "--------", "--------",
    "--------", "--------", "--------", "--------",
])
hat_side = [
    "KHHHHHHK", "KhhhhhhK", "HHhhhhHH", "HHHHHHHH",
    "-HH--HH-", "--H--H--", "--------", "--------",
]
paint("hat_r", hat_side)
L["hat_l"].art([r[::-1] for r in hat_side], LIT)
G["hat_l"].art([r[::-1] for r in hat_side], GLOW)
paint("hat_b", [
    "KHHHHHHK", "KhhhhhhK", "HHhhhhHH", "HHHHHHHH",
    "-HHHHHH-", "-H-HH-H-", "--------", "--------",
])
paint("hat_top", [
    "KHHHHHHK", "HHhhhhHH", "HhHHHHhH", "HHhHHhHH",
    "HhHHHHhH", "HHhHHhHH", "HhHHHHhH", "KHHHHHHK",
])
for f in ("hat_bot",):
    L[f].fill(CLEAR); G[f].fill(CLEAR)

# ===================================== BODY: one continuous fracture, lit dark
paint("body_f", [
    "TtTTTTtT", "TTTTTTTT", "TTT1TTTT", "TTT21TTT",
    "TTTT12TT", "TTTT1TTT", "TTT21TTT", "TT21TTTT",
    "TT1TTTTT", "TT2TTTTT", "eeeeeeee", "JJJJJJJJ",
])
paint("body_b", [
    "TTTTTTTT", "TTTTTTTT", "TTTT1TTT", "TTT21TTT",
    "TT21TTTT", "TT1TTTTT", "TT21TTTT", "TTTT1TTT",
    "TTTT1TTT", "TTTT2TTT", "eeeeeeee", "JJJJJJJJ",
])
for f in ("body_r", "body_l"):
    paint(f, ["TtTT", "TTTT", "TT1T", "TT2T", "T1TT", "T2TT",
              "TTTT", "TT1T", "TT2T", "TTTT", "eeee", "JJJJ"])
paint("body_top", ["TtttttTT", "tTTTTTTt", "tTTTTTTt", "TTTTTTTT"])
paint("body_bot", ["JJJJJJJJ", "JIIIIIIJ", "JIIIIIIJ", "JJJJJJJJ"])

# tattered shroud on the body overlay
paint("jac_b", [
    "NNNNNNNN", "NnNNNNnN", "NNNNNNNN", "NnNNNNnN",
    "NNNNNNNN", "NnNNNNnN", "NNNNNNNN", "-NNNNNN-",
    "-N2NN2N-", "--N--N--", "--------", "--------",
])
paint("jac_f", [
    "NNNNNNNN", "Nn----nN", "NN----NN", "Nn----nN",
    "NN----NN", "-N----N-", "-N----N-", "-2----2-",
    "--------", "--------", "--------", "--------",
])
for name, flip in (("jac_r", False), ("jac_l", True)):
    art = ["NNNN", "NnnN", "NNNN", "NnnN", "NNNN", "NnnN",
           "NNNN", "-NN-", "-22-", "--N-", "----", "----"]
    art = [r[::-1] for r in art] if flip else art
    L[name].art(art, LIT); G[name].art(art, GLOW)
paint("jac_top", ["NNNNNNNN", "NnnnnnnN", "NnnnnnnN", "NNNNNNNN"])
L["jac_bot"].fill(CLEAR); G["jac_bot"].fill(CLEAR)

# ============================== ARMS: fractured sleeves into glowing palms
arm =     ["TtTT", "TT1T", "TT1T", "T12T", "T1TT", "T21T",
           "TT1T", "TT2T", "EEEE", "ssss", "s44s", "d44d"]
arm_alt = ["TtTT", "T1TT", "T1TT", "T21T", "TT1T", "T12T",
           "T1TT", "T2TT", "EEEE", "ssss", "s44s", "d44d"]
for pre in ("rarm", "larm"):
    paint(f"{pre}_f", arm)
    paint(f"{pre}_b", arm_alt)
    paint(f"{pre}_r", arm_alt)
    paint(f"{pre}_l", arm)
    paint(f"{pre}_top", ["TtTT", "tTTt", "tTTt", "TTTT"])
    paint(f"{pre}_bot", ["d44d", "4334", "4334", "d44d"])

wisp = ["NNNN", "NnnN", "-NN-", "-2--", "----", "----",
        "----", "----", "----", "----", "----", "----"]
for pre in ("rsl", "lsl"):
    for f in ("f", "b", "r", "l"):
        paint(f"{pre}_{f}", wisp)
    paint(f"{pre}_top", ["NNNN", "NnnN", "NnnN", "NNNN"])
    L[f"{pre}_bot"].fill(CLEAR); G[f"{pre}_bot"].fill(CLEAR)

# ================================================= LEGS: fractured, dark boots
leg =     ["JJJJ", "JJ1J", "JJ1J", "J12J", "J1JJ", "J21J",
           "JJ1J", "JJ1J", "JJ2J", "IIII", "BBBB", "BbbB"]
leg_alt = ["JJJJ", "J1JJ", "J1JJ", "J21J", "JJ1J", "J12J",
           "J1JJ", "J1JJ", "J2JJ", "IIII", "BBBB", "BbbB"]
for pre in ("rleg", "lleg"):
    paint(f"{pre}_f", leg)
    paint(f"{pre}_b", leg_alt)
    paint(f"{pre}_r", leg_alt)
    paint(f"{pre}_l", leg)
    paint(f"{pre}_top", ["JJJJ", "JjjJ", "JjjJ", "JJJJ"])
    paint(f"{pre}_bot", ["BBBB", "BbbB", "BbbB", "BBBB"])
for pre in ("rpant", "lpant"):
    for f in ("f", "b", "r", "l", "top", "bot"):
        L[f"{pre}_{f}"].fill(CLEAR); G[f"{pre}_{f}"].fill(CLEAR)

# ---------------------------------------------------------------- checks
# Every base-layer pixel must be opaque or the model renders see-through.
BASE = [n for n in L if n.split("_")[0] not in
        ("hat", "jac", "rsl", "lsl", "rpant", "lpant")]
bad = [(n, x, y) for n in BASE for y in range(L[n].h) for x in range(L[n].w)
       if lit[L[n].oy + y][L[n].ox + x][3] != 255]
assert not bad, f"{len(bad)} transparent pixels in the lit base layer, e.g. {bad[:5]}"

glow_px = sum(1 for row in glow for px in row if px[3] == 255)
assert glow_px, "emissive texture is entirely empty"

out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src", "main", "resources", "assets", "herobrine",
                   "textures", "entity")
os.makedirs(out, exist_ok=True)
write_png(os.path.join(out, "herobrine.png"), lit)
write_png(os.path.join(out, "herobrine_eyes.png"), glow)
print(f"wrote herobrine.png (lit) and herobrine_eyes.png ({glow_px} emissive pixels)")
