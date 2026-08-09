# Running a server

Everything here is for the machine hosting the game. Players joining it need
the modpack instead — see [README](README.md#joining-a-server).

## What the server needs

| | |
|---|---|
| Java | **25 or newer**. `java -version` must say 25+, not 17 or 21. |
| Minecraft | 26.2 |
| Loader | Fabric server launcher for 26.2 |
| Mods | `fabric-api-0.156.0+26.2.jar` and `herobrine-<version>.jar` |

1. Get the **Fabric server launcher** for 26.2 from
   [fabricmc.net/use/server](https://fabricmc.net/use/server/).
2. Run it once so it generates `mods/`, `server.properties` and `eula.txt`.
3. Accept the EULA in `eula.txt`.
4. Put **both** jars in `mods/`. Fabric API is not optional; the mod will not
   load without it.
5. Start it again. The log should say `wrote a default config to
   config/herobrine.json` on the first run with the mod installed.

## The one thing that will bite you

**The server and every client must run the same version of this mod.** Not
"close enough" — the same. A player on 1.0.1 joining a 1.0.2 server, or the
reverse, is the single most likely way this goes wrong, and the symptom is
usually a confusing disconnect rather than a clear message.

The `.mrpack` exists to make that automatic: it pins the mod version by hash,
so everybody who imports the same pack is on the same build. When you update
the server, hand out the new pack.

To build one with your server already in the players' list:

    python3 tools/mkpack.py play.yourserver.com

## Updating an existing world

Swapping the jar changes the code and **nothing about the world**. Every
position, phase and flag lives in persistent attachments — that is what makes
them survive a restart — so a world played on an older version keeps whatever
it decided back then.

That matters once, for worlds started before 1.0.5, because the buildings used
to be sited on a ring around world spawn and are now chosen near the players.
An old world still has the far-off positions recorded and will go on waiting
for somebody to walk out to them.

    /herobrine resite

Forgets where everything was going to go, so it is chosen again near whoever is
online. **It leaves anything already built standing** — it only stops the mod
believing it owns those places.

Wrath is separate and is not touched; `/herobrine status` shows it and
`/herobrine wrath <n>` moves it.

## Configuration

`config/herobrine.json`, written on first run, in the server directory. It is
the server's copy that decides what happens — a player editing their own does
nothing to a multiplayer game.

The three worth knowing before friends arrive:

- `breakIn` — he mines through walls and doors during a hunt. Everything he
  breaks **drops**, so nothing is lost but the wall. Still the most common
  thing to want off on a shared world.
- `realLightning` — act three of the ending throws lightning that burns and
  hurts. On by default because the ending is meant to leave a mark.
- `wrathRate` — how fast the whole thing escalates. `0.5` doubles the length of
  the game; `2.0` halves it. On a server where people play at different times,
  lower is usually better, because wrath is shared and climbs whenever anybody
  is online.

## How this behaves with several people

- **Wrath is one number for the world**, not per player. Everything anybody
  does adds to the same total, so a server of six reaches the later phases far
  faster than one person would. `wrathRate` is the dial for that.
- **There is only ever one of him in the entire world.** Never one per player.
  If he is busy with somebody, everybody else gets a quiet night.
- **A hunt works through the group one at a time** — he reaches one person,
  finishes with them, and turns to somebody who has not been reached yet.
  Standing together is the worst possible arrangement.
- **Structures build when the first player gets within 112 blocks** and are
  then permanent. Whoever explores furthest finds them; everybody shares them.
- **The ending is world-wide.** When he dies, wrath goes to zero, the storm
  breaks and the night ends for everyone. Then it starts again from nothing.

## Server settings that matter

Defaults are fine. Two are worth a glance if things look wrong:

- `simulation-distance` — he is placed **42–68 blocks** from a player and needs
  to be in a ticking chunk to do anything. The default of 10 chunks is ample;
  dropping it to 4 or 5 on a small host will make sightings unreliable.
- `view-distance` — does not affect his behaviour, only whether distant
  structures are visible. Anything from 8 up is fine.

Nothing in this mod needs a whitelist, a permission plugin, or an open port
beyond the usual one.

## Checking it loaded

In the server console:

    /herobrine status

That prints the current wrath and phase. If the command is unknown, the mod is
not loaded — check `mods/` has both jars and the log for a Fabric error near
startup.
