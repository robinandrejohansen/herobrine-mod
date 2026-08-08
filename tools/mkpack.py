#!/usr/bin/env python3
"""Build a Modrinth modpack (.mrpack) for this mod.

    python3 tools/mkpack.py                       # just the mod + Fabric API
    python3 tools/mkpack.py play.example.com      # ...and preload that server

WHY A MODPACK AT ALL. Minecraft can push a resource pack to a joining client
and nothing else — it has never been able to install executable mods, and it
should not be able to. So a server running this cannot hand the mod to whoever
turns up; the player has to arrive already holding it. A .mrpack is the standard
answer: one file, imported once, and the launcher fetches the loader, Fabric API
and the mod at the pinned versions and makes a profile to launch from.

WHY THIS MOD CANNOT BE SERVER-SIDE ONLY. It registers a custom entity and a
custom block, and it ships client mixins for the fog, the eyes and the rain. A
vanilla client has no idea what a `herobrine:herobrine` is, so it cannot be
allowed to connect and be told about one. That is why the join is refused rather
than degraded.

NOTHING IS BUNDLED. The pack is a manifest of URLs: Fabric API from the Modrinth
CDN, and this mod from its own GitHub release. Both are hosts Modrinth's format
permits, so the pack stays a few kilobytes, nobody is redistributing anybody
else's jar, and the versions are pinned by hash rather than by hope.
"""
import hashlib
import json
import os
import struct
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = "robinandrejohansen/herobrine-mod"


def properties():
    values = {}
    with open(os.path.join(ROOT, "gradle.properties")) as handle:
        for line in handle:
            if "=" in line and not line.startswith("#"):
                key, _, value = line.partition("=")
                values[key.strip()] = value.strip()
    return values


def fetch(url):
    # curl rather than urllib. This machine's Python has no CA bundle wired up,
    # so urlopen fails with CERTIFICATE_VERIFY_FAILED on a perfectly valid
    # certificate — and a build script that only works where somebody has
    # already fixed their Python install is not a build script.
    out = subprocess.run(
        ["curl", "-sSfL", "-A", f"{REPO}/mkpack", url],
        capture_output=True, text=True)
    if out.returncode != 0:
        raise SystemExit(f"could not reach {url}\n{out.stderr.strip()}")
    return json.loads(out.stdout)


def fabric_api(game_version):
    """The newest Fabric API published for this exact Minecraft version."""
    url = ("https://api.modrinth.com/v2/project/fabric-api/version"
           f'?game_versions=["{game_version}"]&loaders=["fabric"]')
    versions = fetch(url.replace('"', "%22").replace("[", "%5B").replace("]", "%5D"))
    if not versions:
        raise SystemExit(f"Fabric API has no build for Minecraft {game_version}")
    return versions[0]["files"][0]


def entry(path, url, sha1, sha512, size):
    return {
        "path": path,
        "hashes": {"sha1": sha1, "sha512": sha512},
        # env is required by the format. Both sides need this mod, and Fabric
        # API, so neither is marked optional or client-only.
        "env": {"client": "required", "server": "required"},
        "downloads": [url],
        "fileSize": size,
    }


def servers_dat(name, address):
    """A minimal uncompressed NBT servers.dat, so the server is already listed.

    Hand-rolled because pulling an NBT library in for eleven tags would be a
    silly dependency. servers.dat is uncompressed NBT, which is the one detail
    worth getting right — gzip it and the launcher silently ignores the file.
    """
    def string(text):
        raw = text.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw

    def tag(kind, key, payload):
        return struct.pack(">B", kind) + string(key) + payload

    server = tag(8, "name", string(name)) + tag(8, "ip", string(address)) + b"\x00"
    servers = struct.pack(">B", 10) + struct.pack(">i", 1) + server
    body = tag(9, "servers", servers) + b"\x00"
    return struct.pack(">B", 10) + string("") + body


def main():
    props = properties()
    version = props["mod_version"]
    game = props["minecraft_version"]
    loader = props["loader_version"]

    jar = os.path.join(ROOT, "build", "libs", f"herobrine-{version}.jar")
    if not os.path.exists(jar):
        raise SystemExit(f"{jar} is not there — run ./run.sh build first")
    payload = open(jar, "rb").read()

    api = fabric_api(game)
    files = [
        entry(f"mods/herobrine-{version}.jar",
              f"https://github.com/{REPO}/releases/download/v{version}/herobrine-{version}.jar",
              hashlib.sha1(payload).hexdigest(),
              hashlib.sha512(payload).hexdigest(),
              len(payload)),
        entry(f"mods/{api['filename']}", api["url"],
              api["hashes"]["sha1"], api["hashes"]["sha512"], api["size"]),
    ]

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": "Herobrine",
        "summary": "He is never seen arriving.",
        "files": files,
        "dependencies": {"minecraft": game, "fabric-loader": loader},
    }

    out = os.path.join(ROOT, "build", "libs", f"Herobrine-{version}.mrpack")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as pack:
        pack.writestr("modrinth.index.json", json.dumps(index, indent=2))
        if len(sys.argv) > 1:
            pack.writestr("overrides/servers.dat",
                          servers_dat("Herobrine", sys.argv[1]))

    print(f"wrote {out}")
    print(f"  minecraft {game} · fabric-loader {loader}")
    for f in files:
        print(f"  {f['path']}")
    if len(sys.argv) > 1:
        print(f"  server preloaded: {sys.argv[1]}")
    else:
        print("  no server preloaded — pass an address to add one")


if __name__ == "__main__":
    main()
