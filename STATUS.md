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
| RUMOUR | 0 | footsteps, wrong sound, snuffed torch, the fuse, the breathing | ✅ |
| WATCHER | 60 | the stare | ✅ |
| TRESPASSER | 200 | signs, ruins, journal pages | ✅ |
| MIMIC | 500 | possessed mobs | ✅ |
| HUNTER | 1000 | the hunt, the dark | ✅ |
| SIEGE | 1800 | the world turns — night stops, storm never ends, animals turn | ✅ |

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
- [x] The breathing — a heartbeat buried in the rock, with nothing behind it

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
- [x] Its own mob, its own skin — not a modded villager, so villagers stay normal
- [x] Torn-open torso, missing arm, ribs through the shirt, open bleeding mouth
- [x] Shut in behind iron doors, sweeping their heads in step
- [x] A heartbeat through the door, staggered per creature so two never overlap
- [x] Let one out and it charges, head locked, ordinary health
- [x] No eyes — white is his, and this is what is left when he has finished

### The hunt (HUNTER)
- [x] He follows, faster than you, and he does not stop for water
- [x] Steps a full block without breaking pace; vaults 2–4; flies over
      anything bigger and comes down the far side
- [x] The head stays locked on you while the body walks wherever it is going
- [x] He hits and IS GONE — two hearts, then straight out of sight and back in
      from somewhere else. He never stands and trades, which is the only reason
      the phase is survivable at all given he is faster than a sprint
- [x] He takes one person at a time. Once he has reached you he is finished
      with you and turns to somebody who has not been reached yet, round by
      round, until everybody has — so a fast player cannot draw him off the
      party, and a group is not diluted by being a group
- [x] Break his line of sight for eight seconds and he loses the trail. Running
      is not an escape; hiding is. Digging through your wall does not count as
      losing you
- [x] Still invulnerable — there is no killing him until the ending exists
- [x] Swing at him and three fires are left on the ground he was standing on.
      Same safeguards as the trespasser scorch, so indoors you get none
- [x] He breaks off three times — 26–46 blocks away, standing, watching — and
      each return comes in closer and gives you less room than the last
- [x] It ends where you can see it: he stops dead in the open, looks at you
      for two and a half seconds, and goes
- [x] Surviving it brings him on. Enduring the whole hunt is worth 130 wrath to
      everyone still standing; slipping him by hiding is worth 55. Six or seven
      survived hunts is the road from HUNTER to SIEGE
- [x] Swims at walking speed; a lake is not a moat
- [x] Ends after 100 seconds, or when you put 52 blocks between you
- [x] Blocked by a ravine, he stops trying and reappears closer, behind you
- [x] He always has a diamond axe in hand — cosmetic, so it cannot change what
      he hits for; swaps to pickaxe or shovel while mining, then back
- [x] He cannot hit through a wall. No line of sight, no blow — he digs instead
- [x] Wooden doors, gates and trapdoors simply open for him — he does not chop
      what he can push
- [x] Iron holds. He has to cut an iron door, and that is three seconds you
      bought by building properly
- [x] Anything else he mines through — diamond axe for wood, pickaxe for stone,
      cracking overlay, one visible swing at a time. Every block DROPS, so you
      lose the wall and nothing else
- [x] The standoff breaks: 17 blocks becomes 7, and he no longer leaves
- [x] Walk into the last of it and he closes it himself, then goes — and the
      torches go with him

### The dark (HUNTER)
- [x] If it is day, the day ends — forward only, never a free extra night
- [x] Thunderstorm, at once rather than over a morning
- [x] Every torch within 22 blocks goes out — dropped, never destroyed
- [x] Nine bolts land around you over several seconds, visual-only: no fire,
      no damage, nothing broken
- [x] It reaches you indoors — the room goes dark and the field outside lights up
- [x] Rain turns red at HUNTER, fully red at SIEGE

### SIEGE — the world turns
- [x] The night does not end. Not a slower clock, a stopped one
- [x] Sleeping still works, and is now the most expensive bargain in the game:
      the only way to see the sun is to bring him closer
- [x] The storm never stops — renewed before it can run out
- [x] Every untamed animal turns on you. Half a heart, every two seconds,
      forever. Nothing to beat, which is the point
- [x] He arrives with three bolts on the ground he lands on — the one phase
      that abandons "never seen arriving", and loudly

### The reckoning (SIEGE only)
- [x] He can be hurt, and only by a player, and only at SIEGE. Five phases of
      being untouchable is what gives the sixth its weight
- [x] Counted in BLOWS, not damage — thirty of them. A netherite axe changes
      how the fight looks and never how long it lasts, so the tenth blow is the
      tenth blow for everybody
- [x] He does not flee, relocate or vanish when struck any more. He stands and
      takes it and gets worse
- [x] Visibly angrier each act — smoke, soul fire, and more scorch per blow
- [x] **The tenth blow: the church.** A parish chapel the size of a shed, your
      grave outside it with your name on the stone, signs saying PRAY, and a
      bell you hear from wherever you are standing
- [x] His blows go through armour and enchantments. Eight is eight whatever you
      are wearing, so the fight is about not being hit
- [x] Act two (10–19): fireballs and arrow volleys, lobbed so you can see them
      coming and get behind something
- [x] Act three (20–29): three fireballs at once, four-arrow volleys, lightning
      around you, and he stops staying on the ground
- [x] Act three lightning is real — it burns and it hurts, and the volley
      varies: distant flashes with one or two that actually land near you
- [x] **What he leaves.** A nether portal stands for six seconds and then
      FAILS — never usable, which is the point. A burnt ring, a half-built
      ruin, signs still threatening you in the present tense, and 2600 xp
- [x] **The Effigy** — a carved head on a plinth in the ruin. Break it, take it
      home, keep it. The only object this mod ever lets anybody keep
- [x] **He dies.** The storm breaks, the clock starts, the sun comes up, and
      wrath goes to zero — which is how the world is put back, since everything
      here reads off that one number. And then it can begin again

### The herd
- [x] By HUNTER every cow, pig and sheep has white eyes — not one taken animal,
      all of them, which is the point: it stops being personal
- [x] By SIEGE they are red. Decided client-side off the phase it already knows,
      so the server never touches an entity to say it
- [x] Villagers are exempt. They get eyes only by being taken

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
- [x] Eight two-storey houses — timber framing, jettied upper floor
- [x] The hall — open gallery over the tables
- [x] The smithy — open-fronted forge, stone, chimney over the rooftops
- [x] Two shops — a trade each, awnings out over the lane, family room behind
- [x] Two pens — terrain-following fences, open shelters, real animals in them
- [x] The church — 19x31, five courses of wall and nine of roof, coloured glass
      down both sides, wool hangings between them, timber roof and spire, and
      the stair down is behind the altar INSIDE the building
- [x] Villagers on their own doorsteps, one per plot, three in the hall
- [x] **The undercity** — a vaulted chamber under the square with streets,
      houses, lantern posts and a library. Two ways in: the altar stair and a
      swim down the well. Villagers still live there

---

## Not built

### The five houses
| # | Name | What it is | Status |
|---|---|---|---|
| 1 | The homestead | A home with the furniture still in it | ✅ |
| 2 | The tower | Stair up the outside, the buried house underneath | ✅ |
| 3 | The gaol | Fourteen cells. Thirteen open, one shut | ✅ |
| 4 | The open church | Walls, pillars, an altar, and no roof | ✅ |
| 5 | The threshold | Not a house. The lab and the seal | ✅ |

Built the two ends first on purpose: the first had to establish what a home of
his looks like and the last had to establish where it was going, and the middle
is only legible once both exist. Each of 2–4 is defined by what the one before
it still had and this one does not — the windows, then the domesticity, then the
roof and the bed together.

Every one of them has ground around it to explore and a passage leaving it that
goes somewhere: the tower's cellar bores east, the gaol's warder's room opens on
three unlit workings, and the church has a shaft under the chancel heading
toward the threshold.

Sited in strictly ordered bands (1950–2200, 2250–2450, 2500–2700) that do not
overlap, so whichever you stumble on first, the next one out is always the next
one along. Distance is the only thing telling you they are a sequence.

### Everything else outstanding

- [ ] **Village names and signposts**, and roads between villages
- [ ] **The fight** — he flies, throws fire, looses arrows. Deliberately held
      for the ending rather than spent at HUNTER, or SIEGE has nowhere to go
- [ ] **SIEGE: the caves and long paths near him** — still to build

- [x] **A config** — `config/herobrine.json`, one flat file. A master switch, a
      wrath rate, every event, and separate toggles for the three things people
      actually argue about: breaking in, taking torches, leaving fires
- [ ] **Nothing points at the houses or the town** — they generate now, but
      there are still no signposts or roads leading to them
- [ ] The stranger — an NPC who arrives before him
- [ ] Lightning, held back until there is an event for it to belong to
- [ ] Wind — Minecraft has none; would have to be faked with sound
- [ ] Remove the spawn-marker lightning (`/herobrine mark`) before release —
      it is fenced in HauntingSpawner and the command, and nothing else uses it

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
