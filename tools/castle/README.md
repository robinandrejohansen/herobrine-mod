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
