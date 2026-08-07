#!/usr/bin/env python3
"""Reads decompiled Minecraft sources out of Loom's decompile cache.

`./gradlew genSources` writes its output to a content-addressed cache
(~/.gradle/caches/fabric-loom/decompile/v1.zip) rather than a sources jar, so
the filenames are hashes and you cannot grep it by path. Each entry is a small
binary container holding a NAME field (the class path) and a SRC field (the
decompiled Java).

This builds an index of class path -> source and prints whatever you ask for,
which is how we check real 26.2 signatures instead of trusting memory.

    python3 tools/mcsrc.py list EyesLayer          # find matching classes
    python3 tools/mcsrc.py show net/minecraft/.../EyesLayer
    python3 tools/mcsrc.py grep "class EyesLayer"  # search all sources
"""
import os, re, sys, zipfile, functools

CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/decompile/v1.zip")

@functools.lru_cache(maxsize=1)
def index():
    """class path -> decompiled java source."""
    out = {}
    with zipfile.ZipFile(CACHE) as z:
        for info in z.infolist():
            if info.is_dir() or info.file_size < 32:
                continue
            raw = z.read(info)
            # layout: "NAME" + 4-byte big-endian length + class path + "SRC"
            #         + 4-byte length + the decompiled java
            i = raw.find(b"NAME")
            j = raw.find(b"SRC", i)
            k = raw.find(b"package ")
            if i == -1 or j == -1 or k == -1:
                continue
            name = raw[i + 8:j].decode("utf-8", "replace")
            out[name] = raw[k:].decode("utf-8", "replace")
    return out

def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    cmd, arg = sys.argv[1], sys.argv[2]
    idx = index()

    if cmd == "list":
        hits = sorted(k for k in idx if arg.lower() in k.lower())
        print(f"{len(hits)} match(es) for {arg!r}")
        for h in hits[:40]:
            print(" ", h)
    elif cmd == "show":
        key = arg if arg in idx else next(
            (k for k in sorted(idx) if k.endswith("/" + arg) or arg in k), None)
        if not key:
            print(f"no class matching {arg!r}; try: list {arg}")
            return 1
        print(f"// ==== {key} ====")
        print(idx[key])
    elif cmd == "grep":
        for k in sorted(idx):
            for n, line in enumerate(idx[k].splitlines(), 1):
                if arg in line:
                    print(f"{k}:{n}: {line.strip()}")
    else:
        print(__doc__)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
