# Reading a Minecraft world

`anvil.py` reads NBT and Anvil region files. `extract.py` pulls a box of blocks
out of a world as `{(x,y,z): (name, properties)}`.

Written to measure the Legacy Console Edition tutorial-world castle as a
reference for `structure/Keep.java`. Point it at any 1.13-era save:

    import extract
    blocks = extract.box("TU19", x0, x1, y0, y1, z0, z1)

## Three bugs worth remembering

**Evaluation order.** `out[r.st()] = payload(r, tt)` reads the payload BEFORE
the key, because Python evaluates the right-hand side of an assignment first.
That desyncs the tag stream one field in and surfaces much later as a nonsense
tag id. Read the name onto its own line.

**Bits per block comes from the array length, not the palette size.** The
documented `max(4, ceil(log2(len(palette))))` is not what every chunk on disk
holds — a save that has been through a version upgrade can carry a section whose
array was written at a wider stride than its palette now needs. `len*64/4096` is
exact for pre-1.16 and cannot lie.

**Sign extension.** `(states[j] >> off) & mask` is wrong for every value that
straddles a word boundary, and wrong silently. `struct '>q'` hands back SIGNED
longs, Python's `>>` sign-extends negatives for ever, and a mask wider than the
bits left in the word reads those phantom ones as data. At `off` 62 with a 5-bit
stride only two bits are real and the other three come back set — a 17-entry
palette gets asked for index 31. Mask to `64 - off` first.

Verified: 15,389 sections of TU19 decode with no out-of-range index.

## What the world holds

The castle is only in TU9 and later. TU1 has no castle at all — 3,138 built
blocks, 19 materials, no nether portal, four doors.

    TU19  castle    x  70..140   y 56..104   z -120..-49
                    20,102 blocks, 77 block types, 243 block states

## Placing what you measured

`extract.py` gives you `{(x,y,z): (name, properties)}`. Write it out as:

    { "size":    { "x": 71, "y": 49, "z": 72 },
      "palette": [ "cobblestone", "oak_stairs[facing=south,half=bottom]", ... ],
      "blocks":  [ [x, y, z, paletteIndex], ... ] }

Drop that in `config/herobrine/blueprints/<name>.json` and place it in game
with `/herobrine blueprint <name>`.

**What ships and what does not.** The mod carries the reader, and a blueprint
dropped in the player's own config directory always wins over a bundled one. Two
sets are bundled because the author holds or was granted the rights to them:
`tutorial_castle` and the six `village_*` files. Anything measured out of a build
that is not yours belongs in the config directory and not in this repository —
it is public, and a release goes to a website.

**`modern.py` reads a 1.18-or-later save.** `anvil.py` reads the NBT either way;
what moved is the layout — `Level.Sections[]` became `sections[]`, `Palette` and
`BlockStates` became `block_states.{palette,data}`, and values stopped straddling
longs. That last one is the dangerous change: `anvil.unpack` takes a `spanning`
flag and passing the wrong one does not fail, it returns plausible indices for
the wrong blocks. `modern.bits_for` derives the stride from the palette and then
argues with the array length, and a section whose two disagree is solved from the
length.

### Crossing versions

`Blueprint.modernise` translates 1.13 states. Measured against the tutorial
castle — 243 palette entries, 20,102 blocks — 98.6% land byte-identical with no
translation at all. The other 1.4% is two changes:

| | blocks | what happened |
|---|---|---|
| `*_wall` sides | 269 | booleans in 1.13, a `WallSide` enum since 1.16. `true` maps to `low`, `false` to `none`, which is what vanilla's own converter did |
| `wall_sign` | 20 | became `oak_wall_sign` in 1.14. A renamed block does not fail on its properties, it fails entirely |

Everything else passes through. `waterlogged`, `up`, `powered`, `open`,
`occupied`, `enabled`, `lit`, `in_wall`, the brewing-stand bottles, and the four
sides of a fence, pane or iron bars are all still booleans and all still mean the
same thing.

An entry this table does not know falls back to the bare block name, and then to
being skipped — a building with a hole in it beats no building.
