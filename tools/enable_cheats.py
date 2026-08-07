#!/usr/bin/env python3
"""Turns on cheats for every world in run/saves.

Minecraft defaults new worlds to cheats OFF, and with cheats off it does not
put privileged commands in the player's command tree at all — so /herobrine
and even /time come back as "unknown command" rather than "no permission".
That is a confusing failure to hit repeatedly while developing, and "Open to
LAN" has to be redone every session.

run.sh calls this before launching, so any world created in the dev client
has cheats the next time you start.

Backs up level.dat before touching it. Safe to run repeatedly.
"""
import gzip
import pathlib
import shutil
import sys

FLAG = b"allowCommands"


def main():
    saves = pathlib.Path(__file__).resolve().parent.parent / "run" / "saves"
    if not saves.is_dir():
        return 0

    changed = 0
    for level_dat in sorted(saves.glob("*/level.dat")):
        try:
            raw = bytearray(gzip.open(level_dat, "rb").read())
        except OSError:
            continue  # mid-write, or not gzipped; leave it alone

        i = raw.find(FLAG)
        if i == -1:
            continue
        value = i + len(FLAG)          # TAG_Byte value sits right after the name
        if raw[value] == 1:
            continue

        shutil.copy2(level_dat, str(level_dat) + ".bak")
        raw[value] = 1
        with gzip.open(level_dat, "wb") as f:
            f.write(bytes(raw))
        print(f"  cheats enabled: {level_dat.parent.name}")
        changed += 1

    return 0 if changed >= 0 else 1


if __name__ == "__main__":
    sys.exit(main())
