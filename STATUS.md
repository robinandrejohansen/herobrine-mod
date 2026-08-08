# Where this is

The map of the mod. What exists, what does not, and what comes next.

Updated whenever something lands, so this file is always the truth and nothing
important is only in somebody's head.

- **DESIGN.md** — why things work the way they do
- **LORE.md** — the story
- **PLAYTEST.md** — how to check any of it in game
- **STATUS.md** — this. Where we are

---

## Right now

**Content is complete through MIMIC.** The two phases above it escalate what
already exists but have nothing of their own, so a player at 1800 wrath gets
the same rhythm they had at 500.

**Building the town.** Frame is in and the scale is confirmed; the buildings
are the next slice.

**Still the biggest gap: HUNTER.** See "The path" below.

---

## The phases

| Phase | Wrath | Its own events | Built |
|---|---|---|---|
| RUMOUR | 0 | footsteps, wrong sound, snuffed torch, the fuse | ✅ |
| WATCHER | 60 | the stare | ✅ |
| TRESPASSER | 200 | signs, ruins, journal pages | ✅ |
| MIMIC | 500 | possessed mobs | ✅ |
| HUNTER | 1000 | **none** | ❌ |
| SIEGE | 1800 | **none** | ❌ |

Roughly 2½–3 hours of normal play from MIMIC to HUNTER. Culling his animals is
much faster and is the intended route.

---

## Built

### The engine
- [x] Wrath — one shared number per world, plus a personal share that changes pacing
- [x] Six phases with thresholds
- [x] The director — one event per player every 8–20 min, shortening with attention
- [x] Suppression so the last two events never repeat
- [x] Failure reasons on every refusal, so testing tells you why

### Him
- [x] The entity — invulnerable, silent, never seen arriving
- [x] Placement in the open and underground, always out of view
- [x] Stands still. Never approaches
- [x] Look away and he is gone
- [x] Walk at him and he leaves, through rock if he has to
- [x] Relocates behind you instead, 1-in-3 → every time by MIMIC
- [x] Aware of every player, not just the nearest
- [x] Emissive eyes, turned down to a sheen

### Traces
- [x] Footsteps that walk past, on the ground you are standing on
- [x] The fuse — a creeper that never goes off
- [x] Wrong sound
- [x] Snuffed torches

### Writing
- [x] Signs, written from your own stats
- [x] Grave markers
- [x] The Journal — 16 pages, world-shared, phase-gated, re-issued if abandoned
- [x] 6 books in the homestead
- [x] 5 books in the lab

### Possessed animals
- [x] Silent, still, tracks you, will not deal with you
- [x] Follows you anywhere, including into caves
- [x] Spreads: kill one, two of the watchers are taken
- [x] Rings you at varied distances and paces
- [x] Loses interest and comes back
- [x] Tamed pets are never taken
- [x] Your dog growls at them, and fights first
- [x] White eyes when it turns on you, red at SIEGE
- [x] Hunts and hits you at HUNTER

### The world
- [x] Ruins
- [x] The homestead (house 1) — gable roof, chimney, porch, bedded into the hill
- [x] Its undercroft — carved dig, one chamber, two cells behind iron doors
- [x] The threshold (house 5) — compound, stair, blocked cave route, records
      room, cell block, lab office, the seal
- [x] Zombies break glass at SIEGE, and can see you through it

### The infected
- [x] Villagers shut in behind iron doors, sweeping their heads in step
- [x] Let one out and it charges, head locked, ordinary health
- [x] Red eyes — white is his, red is what he leaves behind

### Atmosphere
- [x] Rain and thunder scale with wrath
- [x] Nights get longer, days do not get shorter
- [x] Sky, fog and cloud converge on one colour; the horizon dissolves
- [x] The world dims, worst in a storm at SIEGE — client-side, so no extra mobs
- [x] The music fades out, gone by SIEGE
- [x] Vanilla villages decay as phases climb — boarded windows, graves, moss.
      Never destructive: no villager, light, door or bed is ever touched

### The town (in progress)
- [x] Sited, walled, one gate, two towers, terrain-following
- [x] Square with a well, lanes that wander, fields outside the wall
- [x] Thirteen plots allotted along the lanes, levelled and marked
- [ ] The buildings themselves — next up

---

## Not built

### The five houses
| # | Name | What it is | Status |
|---|---|---|---|
| 1 | The homestead | A home with the furniture still in it | ✅ |
| 2 | The second house | Same plan, buried, no windows | ❌ |
| 3 | The dig | A bed in a hollow, tunnels | ❌ |
| 4 | The shrine | No bed. Signs, and one chest | ❌ |
| 5 | The threshold | Not a house. The lab and the seal | ✅ |

Deliberately built the two ends first. 2–4 are the middle of a story whose
ending now exists, so they can be filled in whenever.

### Everything else outstanding
- [ ] **The town's buildings** — 6 two-storey houses, church, hall, 2 shops,
      smithy, 2 pens
- [ ] **Village names and signposts**, and roads between villages
- [ ] **Infected villager skins** — red eyes carry it for now, but the Last of
      Us look needs a villager-variant renderer
- [ ] **HUNTER content** — the phase has nothing of its own
- [ ] **SIEGE content** — same
- [ ] **An ending** — no fight, no Effigy, no way to kill anything
- [ ] **A config** — nothing can be switched off. Blocks anyone else playing it
- [ ] **Nothing points at the houses** — findable only by command or luck
- [ ] The stranger — an NPC who arrives before him
- [ ] Lightning, held back until there is an event for it to belong to
- [ ] Wind — Minecraft has none; would have to be faked with sound

---

## The path

In the order I would do them, and why.

**1. HUNTER.** The most-felt gap. Three pieces:
- He stands his ground when you walk at him — DESIGN already specifies it
- THE HUNT — he pursues instead of vanishing
- THE DARK — every torch near you goes out at once

**2. The seal reacts.** Visiting the threshold at SIEGE should not show the
same wall you found at TRESPASSER. Turns a dead end into somewhere you check on
with dread. Cheap.

**3. The ending.** The Effigy — something you build, at cost, that forces him
to be a real fight. This is the answer to the original brief: *run and hide,
then grow strong enough to kill it.*

**4. The config.** Not needed for your own testing, which is why it keeps
losing. It is the gate on anyone else ever playing this.

**5. The stranger.** Gives the middle of the game a person. Start with the
arrival only — one walk, three lines of chat, and he leaves.

**6. Houses 2–4.**

---

## Testing

```
/herobrine status
/herobrine wrath <n>
/herobrine speed <1-60>
/herobrine provoke [force|<name>]
/herobrine house [here]
/herobrine threshold [here]
/herobrine town here
```

Phase thresholds for `wrath`: 0, 60, 200, 500, 1000, 1800.

Cheats must be on. `run.sh` turns them on for every world in `run/saves`.
