# Herobrine

A Minecraft Java Edition mod (Fabric, 26.2). He was already in your world.

This file is the single source of truth for the mod: the story, how it plays,
how the fight works, the commands, the config, and how to install, run and
release it. The code is the other half; where the two disagree, fix one.

**Download:** [herobrine-mod.vercel.app](https://herobrine-mod.vercel.app) ·
[latest release](https://github.com/robinandrejohansen/herobrine-mod/releases/latest)

---

## The story

There was a village, and it had a bad year. Doors boarded overnight. Signs
nobody had written. A man on the square who did not sleep and did not trade and
had a dot in the middle of each eye. People began to go missing, and the ones
who came back were not the ones who had left.

One of them, **Addexio**, would not accept it. He read what the others would
not read and walked where they would not walk, and he wrote it all down — ten
books, one or two left in every place *he* had touched, so that whoever came
after would not have to learn it the way Addexio had. Then the village did the
brave thing: they armed themselves and went out to fight him.

Almost all of them died. Addexio came home alone.

**That is where you come in.** You find the farm, and the first book. Addexio
finds you, and tells you the truth in the shortest form he can manage — *he has
been seen again. Everyone I have ever walked this road with is dead. I am
telling you now, while you can still say no.* You do not say no. Together you
follow the books: the town, his tower, the prison, the church he lived under,
the last house where they lost him. And in the last house you find what the
village never had — **a way through**. A door into the place he goes.

On the other side is his world: a forest where it is always midnight and always
raining, a city he built and filled with the people he took, and above it his
castle, lit and standing, the one competent building he ever made. You go in.
He is waiting. The door behind you is dead.

Beat him there and the world is saved — not figuratively. The rain stops. The
people he turned are people again. The sky over his world, which has not moved
in years, remembers how, and gives that place its first sunset. A door home
opens on the square of his city, and behind it stands his statue, so nobody
forgets what it cost.

---

## How it plays

### The arc

Nothing is announced. Wrath is one number for the whole world; it climbs with
what the players do, and it moves the story through six phases. Each phase has
its own events and one **place** the players are led to.

| Phase | Wrath | What happens | The place |
|---|---|---|---|
| RUMOUR | 0 | a glimpse, footsteps, a torch that goes out, breathing | **the farm** — where Addexio lived; the first book and the first map |
| WATCHER | 60 | the stare, the passages, the one who does not sleep | **the town** — boarded, an undercity beneath it, the mapmaker's house |
| TRESPASSER | 200 | signs, ruins, journal pages, the sealed shaft | **his tower** — over lava, lit every night for a month |
| MIMIC | 500 | possessed animals, something wearing your friends | **the prison** — cell nine |
| HUNTER | 1000 | the hunt, the dark | **the church** — he lived under the altar |
| SIEGE | 1800 | the night stops, the storm never ends, the animals turn | **the last house** — and the way through |

Each place is found by a map left in the one before it; Addexio tells you about
each one when you arrive. `/herobrine status` shows the current wrath and phase.

### The ten books

All by Addexio, in the order he wrote them, one or two in every place:

1. the farm · 2. what I saw · 3. the town · 4. what he did here · 5. the tower ·
6. the prison · 7. the one who came back · 8. where he lives · 9. the last house ·
10. he has been seen

### Addexio

He comes to you at the farm and introduces himself (seven lines, paced so they
can be read). From then on he follows, fights, and talks.

- Enchanted diamond armour, a diamond sword, a shield he raises when something
  swings at him. Twenty hearts.
- Fights monsters and anything of Herobrine's. Eats when he is hurt and it is
  quiet; faces what attacks him; sprints to keep up.
- **Only Herobrine can kill him.** Everything else stops at two hearts. When he
  falls he says one thing and lies where he fell, arms out, for thirty minutes.
- Crosses into his world with you — through the way, or with `/herobrine boss` —
  if he is within thirty blocks when you go. He tells you what he knows of the
  other side, once.

### His world

Through the way you land in a **vault**: deepslate, a soul lantern, the frame you
came through standing behind you — dead. It does not work from this side. In the
far wall, behind iron bars, a narrow stair climbs to the surface, its steps gone
to moss, roots and cobweb, its mouth grown over. Break the bars, dig out, and
you are in a dark forest at permanent midnight, in the rain.

Ahead is **his city** — streets, a square, houses you were told not to enter,
graves under the trees — and over it **the keep**: a castle raised from a
blueprint (`tutorial_castle`, 71×49×72), stocked with eighteen chests of food
and stone and, rarely, an enchanted golden apple. He is over it when you arrive.

Nothing in his world belongs to you. Fire spreads there. Nothing hurts the city
or the castle except him.

---

## The fight

One class owns it: `Duel`. From the first blow, nothing else moves him.

**The entrance.** He circles the keep, then watches from the walls, then waits
in the great hall. He whispers. He does not strike first unless you stand next to
him for four seconds.

**The first blow takes the sky from him.** He is *bound*: grounded for the rest
of the fight, and the level remembers it — a save, a respawn, a reload all find
him bound, at the right act, in the right form. Come back after leaving and he
says so (*i kept your place*) and resumes.

**A hundred blows** to put him down (`blowsToKill`), in three acts of a third
each. The count shows on his boss bar. A blow only counts if it lands ten ticks
after the last one in act one, thirteen in act two, sixteen in act three: he gets
harder to hurt as he grows. Two players raise the count by half again, four
double it, capped at three times.

| Range | What he does |
|---|---|
| arm's length | swings once a second, feints between; after 4 / 3 / 2 blows taken in a row he blinks out |
| four to twelve | advances, sidesteps, or holds and throws — rolled, never a metronome |
| past twelve | appears six to nine blocks in front of you, or throws from where he is |
| out of sight | three seconds blind and he blows the wall between you open; stubborn hiding and he comes through it |

**His moves.** The sword (twelve, flat, through armour like any hit). Fireballs,
more and stronger each act. Lightning: telegraphed by sparks for a second, then
a bolt that lands on the floor you stand on, hurts by act, and takes a crater.
The **salvo**: hold him past eight blocks for six seconds and he stops, roars,
and fires six to ten shots three ticks apart — blaze fire in act one, ghast fire
from act two. Hide in a hole with one block open and he reads the cover and blows
it out. Stand in a room too low for him and he takes the ceiling off rather than
leave. Crowd him and he sweeps the lot of you back.

**Helpers.** Golems, wolves, Addexio — every fourth helper hit counts as a blow.
He deals with them himself, between you: Addexio takes twelve at a time, anything
else dies.

**Between acts.** He rises slowly from where he stood, fourteen blocks, the
light going out of him, through the ceiling if there is one; hangs against the
sky while a ring of lightning comes down round him; drops, faster every tick;
and lands with a blast that throws everybody within seven blocks and takes the
walls round him. Act two he stands 1.4×. **Act three he stands 1.7× and stays
dark** — unlit, smoking, two white eyes — for the rest of it.

**If you die** and nobody else is standing, the fight falls back to the start of
the act you were in. `/herobrine boss` resumes a bound fight; `/herobrine boss
fresh` starts over.

**If he dies.** He rises from where he fell, whitens, grows, and goes, over seven
seconds. *Removed Herobrine.* The music is the old one. Then:

- the rain stops, for good, in his world and yours
- every villager he turned is a villager again
- the clock is set to late afternoon and his sky clears over two minutes —
  the first sunset that world has ever had, then a real night, then ordinary days
- at your feet: his sword, named **Herobrine**, three enchanted golden apples,
  diamonds, netherite, a totem, and the experience
- on the square of his city: the way home, and behind it his statue — his own
  skin at one block per pixel, the eyes in sea lanterns, the sword in his hand,
  a plaque with your name on it

---

## Commands

All under `/herobrine`. Everything is for testing; nothing here is needed to
play.

| Command | What it does |
|---|---|
| `status` | wrath, phase, and where things stand |
| `phase <name>` | jump the story to a phase |
| `boss [fresh]` | straight to the fight: SIEGE, his city and keep raised, you put down 70 blocks from the keep in full diamond with his sword and twenty golden apples, Addexio with you if he is near. Resumes a bound fight unless `fresh` |
| `resite` | forget where the places were going to go; choose again near whoever is online. Leaves what is built |
| `turned` | put one of the turned in the nearest village |
| `locate` · `here` · `speed` | find him, bring him here, change his pace |
| `house` · `town` · `castle` · `recastle` · `chamber` · `passage` · `threshold` · `theway` · `aftermath` · `blueprint` | raise a place, or the ending's aftermath, where you stand |
| `glimpse` · `stranger` · `sign` · `warning` · `dark` · `hunt` · `force` · `provoke` · `gaunt` | fire one event |

---

## Configuration

`config/herobrine.json` writes itself on first run. On a server the server's copy
decides; a player's own does nothing. The ones worth knowing:

| Key | Default | |
|---|---|---|
| `enabled` | true | the whole mod |
| `atmosphere` | true | the sky, fog and music changes |
| `breakIn` | true | he breaks through walls, doors and cover; everything he breaks drops |
| `realLightning` | true | his bolts hurt and burn |
| `blowsToKill` | 100 | the length of the fight, at one player |
| `theTurning` | true | the one who does not sleep |
| `theHunt` · `theDark` · `possession` · `signs` · `traces` · `ruins` | true | the phase events |
| `hisHost` · `hostileAnimals` · `theTaking` | true | what he does to the world's mobs and animals |
| `hisKeep` · `keepBlueprint` | true · `tutorial_castle` | the castle |
| `houses` · `town` · `villageDecay` | true | the places, and what happens to villages |
| `weather` · `longerNights` · `endlessNight` | true | the storm and the night in the later phases |
| `quietDeaths` | true | no death messages for what he takes |

---

## Install

1. [Fabric](https://fabricmc.net/use/installer/) for **Minecraft 26.2**
2. **Fabric API** and `herobrine-<version>.jar` into your `mods` folder
3. Play normally. Don't go looking — the first hour is meant to feel like
   nothing is installed.

### Joining a server

Use the `.mrpack` on the [release](https://github.com/robinandrejohansen/herobrine-mod/releases/latest),
imported into [Modrinth App](https://modrinth.com/app) or
[Prism](https://prismlauncher.org/). It pins Fabric Loader, Fabric API and this
mod to the versions the server expects. A vanilla client cannot join: the mod
adds entities and a block, so the join is refused rather than degraded.

Build a pack with the server preloaded:

    python3 tools/mkpack.py play.example.com

### Running a server

| | |
|---|---|
| Java | **25 or newer** |
| Minecraft | 26.2, Fabric server launcher |
| Mods | `fabric-api-0.156.0+26.2.jar` and `herobrine-<version>.jar` — both |

Run the launcher once, accept the EULA, put both jars in `mods/`, start it
again. The log says `wrote a default config to config/herobrine.json`.

**The server and every client must run the same version of this mod.** The
`.mrpack` pins it by hash; when you update the server, hand out the new pack.

With several people: wrath is one number for the world, there is only ever one
of him, structures build when the first player comes within 112 blocks and are
then permanent, and the ending is world-wide. `simulation-distance` of 10 is
ample; below 5 his sightings get unreliable.

Swapping the jar changes the code and nothing about the world — every position,
phase and flag lives in persistent attachments. `/herobrine resite` re-chooses
where unbuilt places will go.

---

## Building and releasing

| | |
|---|---|
| Minecraft | 26.2 |
| Loader | Fabric (loader `0.19.3`, API `0.156.0+26.2`) |
| Mappings | Mojang official |
| Loom | `1.17-SNAPSHOT` · Gradle 9.5.1 (wrapper) |
| Java | **25+** required — built against JDK 26 |

```bash
./gradlew build          # jar in build/libs/
./gradlew runClient      # dev client; worlds in run/saves/
scripts/release.sh 2.82.3
```

The release script bumps the version, builds the jar and the modpack, commits,
pushes, and cuts the GitHub release. The website reads the latest release at
page load; nothing to redeploy. **Never build while the dev client is running** —
Loom runs it off `build/classes`.

```
src/main/java/com/bloomlet/herobrine/          common (client + server)
  entity/    him (HerobrineEntity, Duel), Addexio (CompanionEntity), the turned, his mobs
  manifest/  the phase events, weather, the reckoning, the hunt
  structure/ every place: farm, town, tower, prison, church, threshold, the way,
             his city, the keep, the statue
  wrath/     the one number, and the phases
src/client/java/com/bloomlet/herobrine/client/ rendering, the sky (Atmosphere)
src/main/resources/                            textures, sounds, the castle blueprint
tools/                                         generators — castle (tools/castle), books (tools/books), pack
```

Licence: CC0-1.0, inherited from the Fabric example mod scaffold.
