# Herobrine

A Minecraft Java Edition mod. He was already in your world.

**Where this is up to: [STATUS.md](STATUS.md)** — what is built, what is not,
and what comes next.

**Download:** [herobrine-mod.vercel.app](https://herobrine-mod.vercel.app) ·
[latest release](https://github.com/robinandrejohansen/herobrine-mod/releases/latest)

## Install

1. [Fabric](https://fabricmc.net/use/installer/) for **Minecraft 26.2**
2. **Fabric API** and `herobrine-<version>.jar` into your `mods` folder
3. Play normally. Don't go looking — the first hour is meant to feel like
   nothing is installed.

Everything is switchable in `config/herobrine.json`, which writes itself on
first run.

## Status

All six phases and the ending are built. See [STATUS.md](STATUS.md).

## Releasing

    scripts/release.sh 1.0.1

Bumps the version, builds, pushes, and cuts a GitHub release with the jar on
it. The website reads the latest release at page load, so there is nothing to
redeploy after a fix.

## Stack

| | |
|---|---|
| Minecraft | 26.2 |
| Loader | Fabric (loader `0.19.3`, API `0.156.0+26.2`) |
| Mappings | Mojang official |
| Loom | `1.17-SNAPSHOT` |
| Gradle | 9.5.1 (wrapper) |
| Java | **25+** required — built against JDK 26 |

Scaffolded from the official [`fabric-example-mod`](https://github.com/FabricMC/fabric-example-mod)
`26.2` branch. Source sets are split into `main` and `client` so client-only code
(renderers, models) can never be loaded on a dedicated server.

Everything in this toolchain is free and open source. No accounts, API keys or
runtime services are involved.

## Building

Requires a JDK 25 or newer. On macOS with Homebrew:

```bash
brew install openjdk
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
```

Then:

```bash
./gradlew build
```

The mod jar lands in `build/libs/`.

## Running

Launch a development client with the mod already loaded — no launcher profile or
jar copying needed:

```bash
./gradlew runClient
```

Worlds created in the dev client live in `run/saves/`.

To play it in the normal launcher instead, install Fabric Loader from
[fabricmc.net/use](https://fabricmc.net/use/), then drop the built jar and
[Fabric API](https://modrinth.com/mod/fabric-api) into
`~/Library/Application Support/minecraft/mods/`.

## Layout

```
src/main/java/com/bloomlet/herobrine/          common (client + server)
src/client/java/com/bloomlet/herobrine/client/ client only — rendering
src/main/resources/fabric.mod.json             mod metadata, entrypoints
```

## Licence

CC0-1.0, inherited from the Fabric example mod scaffold.
