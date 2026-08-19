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

**HUNTER is no longer a gap — it is a gate.** The church is sited by surviving
a hunt rather than by a clock, the hunt is owed rather than rolled for, three
blows drive him off, and the hunt takes the house apart on the way through. See
"The hunt is the chapter now" below.

---

## The phases

| Phase | Wrath | Its own events | Built |
|---|---|---|---|
| RUMOUR | 0 | the glimpse, the grove, the redstone torch, footsteps, wrong sound, snuffed torch, the fuse, the breathing | ✅ |
| WATCHER | 60 | the stare, the passage, the sand pyramid, the 2x2 tunnel | ✅ |
| TRESPASSER | 200 | signs, ruins, journal pages, the sealed passage | ✅ |
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

### The warrens
A system under each of the four houses, rather than a cellar with a passage off
it. Junctions, spurs, rooms at some ends and rock at others, and a route through
that has to be worked out rather than followed.

Made readable on purpose — getting lost is only interesting if it can be
recovered from. The **trunk** is paved and lit and is the way back. **Spurs** are
unlit and most stop in rock. Every **junction** carries a cairn of a different
height, which is the one thing separating a place you are exploring from a
place you are trapped in.

| House | Manner | What it says |
|---|---|---|
| 1 the homestead | FAILING | long spurs, no lights, a trunk that stops being one |
| 2 the tower | SURVEYED | paved, lit, every junction marked — he still had a plan |

| 3 the gaol | WORKED | wide trunk, ordered spurs, the same hand that cut the cells |
| 4 the church | BURIED | short and low, and it goes down more than along |

Read in sequence they say something the buildings above them do not: he did not
get worse steadily. He was already like this at the start, got organised for a
while, and then it went again.

### The survey — what the tower was for
At the far end of the tower's warren: a working room, and a **two-by-two tunnel
running 240 blocks on one bearing** — torch-lit every ten, cobbled underfoot,
railed, rising and falling with the rock, and **bridged in planks wherever it
crosses a void**.

Straight in plan and not in section. It never deviates a block horizontally,
because that is the whole claim; it takes the grade as it comes, because a
dead-level tunnel through 240 blocks of varied stone is a thing no person has
ever dug. The bridges are half gone — but never both boards at the same step,
so it is always crossable.

Nobody prospects in a straight line — you follow the ore, and ore is never
straight. This is somebody who knew exactly where he was going and how far it
was, and was prepared to dig for months.

Cobblestone, torches and rails on purpose: every one of those is a block a
person places by hand, in that order, because that is what digging a long way
actually looks like. It reads as somebody's project because it is one.

It stops unfinished, and the tools are on the floor.

### The chambers
Eight kinds of small room cut into solid rock behind your caves, scattered and
unnumbered. The five houses are a sequence read in order; these are the
opposite — one fact each and no context, so finding three over a week assembles
something nobody wrote down for you.

- [x] **The shelter** — barrels stocked for far longer than anybody plans for
- [x] **The observation room** — a barred window, and a cell on the far side
- [x] **The cell** — the same room from inside. The door went outward
- [x] **The workshop** — anvil, grindstone, and something that would not hold
- [x] **The dig** — stopped mid-swing, the pick still on the floor
- [x] **The reading room** — shelves both sides, and a chair facing the wall
- [x] **The tally** — four walls of marks and nothing else at all
- [x] **The tiles** — a perfectly regular floor, and one square worn dark

### The undercity is not a dome any more

It was `sqrt(dx*dx + dz*dz)` against a constant under a cosine vault — a
mathematically perfect disc under a mathematically perfect dome. Every "reads
fake" complaint was about those two lines.

The outline is now a wandering radius: three harmonics at random phases summed
around 64 bearings, so it has **lobes, bays and headlands**. Radius runs 12–27
instead of a flat 21. The ceiling wanders on its own phases, so **the high point
is not over the middle** — a dome's apex dead-centre is the other half of the
tell.

And the floor stops being a disc of paving: laid stone where people work, then
**moss, coarse dirt, gravel and podzol** fraying out toward the walls.

Still to do: wood framing, water channels, food in production, glass, decorated
houses. Those are worth adding now that the shell underneath them is not a
formula.

### What the congregation wrote down

**Six accounts, in the barrels in people's houses**, placed after every tunnel
and trap is cut.

They used to be six chests standing loose on the cavern floor at six random
bearings, and that one decision was doing more damage to the room than the
perfect dome ever did. **A chest on open ground is the most artificial object in
Minecraft** — nobody keeps anything in a box in the middle of a street, so six of
them ringing a square could only read as loot markers, and a book inside a loot
marker is the mod talking rather than somebody writing.

Now: **ten barrels across the five houses, one in the library, and only six hold
anything.** The book goes in with the bread, in the same barrel, because that is
the whole sentence — whoever wrote it kept it where they kept their food, in the
one place in the house nobody would look twice at. Which of a house's two barrels
has it is rolled per house.

The rest is flour. Most of what you open down there is somebody's dinner, and
that is what makes finding an account feel like finding something rather than
collecting it.

The sixth is in the reading room, and that is the point of it being sixth: the
accounts are written in order and the last is the one who has **stopped being
frightened**. His is not hidden at home. He keeps it on the shelf where they
meet, because he no longer minds who reads it.

### It was a bowl in the ocean

*Nothing anywhere checked for water.* `Ground.topOf` answers "what is the highest
thing you could stand on", and it does that by scanning **down past anything that
is not footing — including water.** Over an ocean it returns the seabed,
cheerfully, with no indication there are thirty blocks of sea on top of it.

His world runs on the **overworld** noise settings — that is what gives it hills,
caves and a forest floor worth walking on — and the same settings give it oceans,
which a fixed dark-forest biome does nothing whatever to remove.

So a castle sited over water put its courtyard floor on the seabed at ~y47, ran
its curtain eleven blocks up to y58, cleared the inside to air — and the sea came
straight back in over the top. A drowned bowl, with a city sinking around it.

`Ground.dry()` now exists and is asked by everything that builds: two tests, above
sea level *and* no fluid on the footing, so it catches the open ocean and a lake
in a hollow on a hill. The keep re-rolls its bearing up to sixteen times to find
land; streets, the market, stalls, houses and the rampart all simply stop at the
water — which is what roads and walls do.

*Also fixed: the stair behind the altar led to a dead end about half the time.
`cryptStair` finishes by carving a 5×5×4 breakout box where the shaft bottoms
out, and the bore that connects that box to the chamber started in the middle of
it — with its "am I already inside the room?" exemption set at two steps, the
third step tested a block of the spiral's own breakout, found air, decided it had
arrived, and returned having cut nothing. Whether it died depended on which side
the spiral's last step landed, which is why it was intermittent. The exemption is
seven now, which clears the box.*

*Also fixed: one of the five houses was being raised straight through the
library's east wall — the fourth bearing on a ring of fourteen overlaps it by
three columns, and houses go up after the library. It slides round the ring until
it clears, rather than outward, because the rim wanders in to twelve blocks and
pushing a house further out can put it through the cavern wall instead.*

The cult is about **sightings** — nobody down there worships anything, they
each saw something at the edge of a field and came underground because they were
the only people who believed each other.

They disagree with each other, which is the point: one thinks it is a man, one
thinks it is the mine, one keeps a tally, one is writing to somebody who left,
one counted seven shadows at a table of six, and one has stopped being afraid.

Every book describes something the players have already experienced. It is not
"these poor lunatics" — it is a chat log from two hours ago.

### The trial — the third way into the undercity

The stair behind the altar and the well are both simply *found*. This one is
**140 blocks of gauntlet**, seven legs of twenty, and it exists because a cult
that is still meeting needs a door the congregation can use and a stranger
cannot.

| leg | adds |
|---|---|
| lesson | one of every hazard, **defanged** |
| gaps | three-block jumps, staggered lanes |
| plates | reading the floor while jumping |
| pistons | something that moves |
| dark | the same geometry, no lanterns |
| false floor | gravel that isn't ground |
| door | lit candles — somebody is down there |

**Lava under everything that is not the path, and no water anywhere.** Four-block
sprint jumps, a surviving lane that moves between gaps, hidden stone plates on an
uneven grey floor, pistons that can reach the lava, and gravel that gives way
over it. A full stack of arrows per dispenser.

Two rules survive because they are about fairness rather than difficulty: **a
landing between every leg**, so failure costs one stretch and not the run, and
**one sign at the mouth** — a warning, not a tutorial.

The section wanders: width, height, floor level and bearing all vary, and the
cut is rough in places. A dead-straight tube for 140 blocks is the same fault as
a perfect dome.

The far end climbs to daylight now and comes up under a **trapdoor** — a lid in
the middle of nowhere, and the only question is whether to open it.

### He knows you opened it

The **first chest in each of his buildings**, once for good: the sky turns to
thunder, and **every torch in the building goes out**.

Torches do not go out. There is no vanilla mechanic that snuffs one, so a player
standing in a room that just went dark knows with certainty that something did
it deliberately. Snuffed rather than broken — a flint and steel puts them back,
so it costs a moment of blindness and not a repair.

### The villager who is him

Possessed villagers now have the two absences that matter in a crowd:

- **He never sleeps.** Night falls, every villager files indoors, one stands in
  the square. The contrast does all of it
- **He will not trade.** No profession, so right-clicking opens nothing — which
  is how anybody checks whether a villager is real
- Plus what possession already gave him: **silent**, standing still, and facing
  you

### The sky marks where he arrives

The lightning was a debug aid that shipped by accident — and it was the most
frightening thing in the mod, so it stays properly. **Phase-gated**, because a
bolt announcing every spawn destroys the first two chapters, where he must never
be seen arriving.

| phase | the sky |
|---|---|
| rumour, watcher | **nothing** |
| trespasser | one bolt, near him, not on him — weather |
| mimic | one, on the ground he stands on |
| hunter | two or three, staggered |
| siege | three or four |

Visual-only throughout: no fire, no damage. `/herobrine mark` is gone.

### The town, in a forest

Buildings **fell the trees** in their footprint plus a one-block margin before
they go up. `Ground.topOf` always found correct footing under a canopy, so the
building was at the right height with the trunk standing straight through it.

And the silent slope refusal was **4 blocks across a whole plot**, which most
ground outside plains and deserts fails — so plots came up empty with no
explanation. It is 7 now, and it logs when it refuses.

Villagers get a **profession matched to their building** — officials in the hall,
smiths in the shop, farmers everywhere else — so they trade, restock and work.
Beds are claimed on their own, which they could not do while the buildings were
full of tree.

### The floor under each chapter

Finding a place advances the story at once — but it does **not** conjure the next
one. The next place is sited only once the current phase has been lived in, and
**the floor climbs**:

| chapter | must be lived in | by then |
|---|---|---|
| rumour | 20 min | 0h20 |
| watcher | 30 min | 0h50 |
| trespasser | 40 min | 1h30 |
| mimic | 50 min | 2h20 |
| hunter | 60 min | 3h20 |

A short first chapter so the world starts happening quickly; long later ones so
the phases with the most in them get room to show it.

Measured in elapsed time rather than events seen, because manifestations are
per-player and six people would burn an event quota in minutes.

The rhythm: find a place → the world changes → **live in it** → somewhere new
turns out to be out there.

### He comes home

Finding a place starts a clock. **Two to four minutes** of being left alone —
enough to get into the cellar, find the chest, start reading — and then the sky
turns and he is there.

Only while somebody is still within 48 blocks; walk away and the clock resets,
because coming home only means anything if it lands while you are in the house.
Before MIMIC he arrives watching. From MIMIC he arrives hunting.

Which turns looting into a decision: read the second book, or take the chest and
go.

### Two dials

**PHASE is the story, and only finding his places moves it.** One building, one
phase. It is stored, not derived — so nobody advances by sleeping and mining any
more, and the middle of the mod cannot be missed.

**WRATH is his temper**, and it ramps *inside* each phase. `Wrath.into()` runs 0
to 1 across a chapter and tightens the event window to 60% — 8–20 minutes
becomes 4.8–12. The new thing arrives once and quietly when the story turns, and
is the weather by the end of the same chapter.

Existing worlds seed their phase once from the old wrath derivation, so no
campaign is demoted.

### The order, and the distance

**You cannot find house 3 before house 2.** The next place is not even *sited*
until the previous one has been **found** — so it does not exist to stumble on.

And the walk gets longer as the story goes deeper:

| | sited at | ignored past |
|---|---|---|
| homestead | 280–520 | 1140 |
| town | 340–620 | 1240 |
| tower | 450–800 | 1420 |
| gaol | 550–950 | 1570 |
| church | 650–1100 | 1720 |
| threshold | 800–1300 | 1920 |

Built at 192 blocks; **found** at 60. If a place is built and then abandoned,
the story moves on without it rather than stalling — the building stays where it
is, to be found whenever anybody comes back.

### Finding his places

Every building now advertises itself at four ranges, and all four get worse as
the phases climb.

| Range | What | Early | Late |
|---|---|---|---|
| hundreds | **smoke** | woodsmoke column | **blue** |
| ~90 | **roads** — 5, radiating, fading out | dirt path + cobble | stone, then deepslate |
| road's end | **a sign** | "home / not far" — the family's | "nobody / walks back" — his |
| ~70 | **a sound** | an axe, a bell | not a tool |

### What he took

From TRESPASSER he goes through your chests — one or two stacks, never the
chest, never while anybody is looking, and **he leaves one behind**. One diamond
where there were twelve.

**Nothing is ever deleted.** It is held on the world, persisted, capped at 48
stacks, and it comes back three ways:

| Where | When | Why that one |
|---|---|---|
| **Inside possessed animals** | any time after | Invisible until you kill it. Every staring cow you already walked past is now a question |
| **A grave where he broke off** | after a hunt | Somebody else's name on the stone, your own belongings under it — **and a map to the next house he has not shown you yet** |
| **Chests from HUNTER on** | the late houses | Your own pickaxe in the church. Those chests were never treasure — they are where he keeps things |

Config: `theTaking`.

### The seventh name

From MIMIC, rarely, **somebody who is not on the server turns up wearing the
skin and the name of somebody who is** — floating nameplate, a row in the tab
list next to the real one, a matching ping. He copies whoever is FURTHEST away,
so it cannot be resolved by looking, only by asking. He walks, wanders, opens
doors, crosses water, and never once acknowledges anybody. Get close and he
walks away at walking pace. Hit him and there is nothing there, and the row
leaves the tab list with him.

**Underground he is mining** — iron pickaxe in hand, facing the wall, swinging,
cracks spreading, and the stone actually comes away. Plain rock only, from a
whitelist, and he drops nothing.

Test with `/herobrine stranger`. Config: `theStranger`.

**About one chest in six holds something he left** — enchanted, and always
something wrong with it. Sharpness V on a wooden sword; a diamond axe carrying
nothing but Bane of Arthropods I; a good helmet with Binding on it; an iron
sword two hits from dust. Four kinds, one shared pool of names, so the name
tells you who left it and never what it is. Every chest in the mod is eligible.

Every one has a chest in it, and a framed doorway where its corridor breaks
into your cave. Two of the eight hold a shut-in — the heartbeat carries through
rock, so those can be heard before they are seen.

### The signs from the original story
Quotations, not inventions. The 2010 account is almost entirely a list of marks
left on a world rather than things that happen to you, and none of these is
signposted — no sound, no message, nothing pointing at any of them.

- [x] **The glimpse** — him, in a cave, in front of you, for half a second.
      No sound, and if you were looking the other way you missed it
- [x] **The passage** — him, nine to twenty-four blocks down the tunnel you were
      walking along, for five to eight seconds, and the tunnel is the only way
      through. Its own logic; the stare is an outdoor event and nothing else
- [x] Groves of trees with every leaf taken off, in a rough circle
- [x] Small perfect sand pyramids standing in open water
- [x] 2x2 tunnels, dead straight, eighty long, going nowhere
- [x] One redstone torch burning in a cave nobody has been in
- [x] A passage bricked up — the only one made in response to *you*
- [x] Possessed animals with white eyes (already built, and canon)
- [x] He looks at you and is gone (already built, and canon)

### Traces
- [x] Footsteps that walk past, on the ground you are standing on
- [x] The fuse — a creeper that never goes off
- [x] Wrong sound
- [x] Snuffed torches
- [x] The breathing — a heartbeat buried in the rock, with nothing behind it

### The stare
- [x] The light he needs climbs with the phase — proper dark at WATCHER, and by
      SIEGE he will stand in a field at noon
- [x] **Sleeping owes him a sighting.** You took the night off him, so he is
      there when you get up, in daylight, regardless of brightness
- [x] Weighted heaviest of anything in the mod. He is the mod; the traces were
      drowning him out

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

### Nobody is told when somebody dies

The whole mod rests on the player being unable to corroborate anything. The stare
spends **one shared clock** so two people cannot agree on what was in the
clearing. The stranger copies whoever is **furthest away** so it cannot be
resolved by looking. The sighting is over before anybody is sure.

And then the death message arrived in chat, in yellow, with the cause spelled
out, and settled the argument for everyone on the server.

**It splits cleanly**, which is why this is a narrow change. `ServerPlayer.die`
does two separate things: it sends the dying player their own death screen down
their own connection, and it broadcasts a system message to everybody else.

**Only the second one goes.** You still know exactly what killed you — you have
to, or dying stops teaching anything. Nobody else knows you died at all.

So the only way the group finds out is **somebody saying it out loud**. That is
the thing this buys: a player has to break the fiction themselves, in their own
voice, to tell their friends what happened. There is a silence before that
sentence.

**All deaths, not only his** — and that is the design rather than laziness. If
only Herobrine's kills were quiet, then silence would *mean* Herobrine, and the
mod would have built a perfect notification for the one thing it most wants
nobody to be certain about. A drowning has to be as quiet as he is.

Config: `quietDeaths`. Off restores vanilla exactly — a server that runs on chat
rather than voice will genuinely want the messages.

### He has a voice now

**Every sound in this mod was borrowed, and six of them were the warden's** — the
heartbeat behind the rock, the noise he makes when hurt, the noise he makes when
he dies. The warden is one of the three most recognisable sounds in the game. A
player who hears it does not think *him*, they think *warden* — and forty hours
of writing insisting he is a **person** had the audio arguing the opposite every
time it played.

| sound | replaces | where |
|---|---|---|
| `breath` | `WARDEN_HEARTBEAT` | behind the rock, and in a possessed animal |
| `anger` | `WARDEN_ANGRY` | hurt mid-hunt, and each blow of the Reckoning |
| `gone` | `WARDEN_DEATH` | his death. Once, ever, per world |
| `his_world` | *nothing* | the bed under the dimension |
| `the_way` | `PORTAL_TRIGGER` | the frame closing over him |

**Not a warden remix, and that is a licence question before it is a craft one.**
Minecraft's sound files are Mojang's. A pitched-down, reversed, filtered warden
is still shipping Mojang's asset — processing does not launder ownership, and a
mod that redistributes them cannot be published. What *can* be taken is the
recipe, and it is four techniques rather than a waveform: **FM** for inharmonic
partials, **resonance** for a body, **pitch drift** because nothing alive holds a
note, and **long tails** so a sound arrives from somewhere with a size.

The first pass had none of those — stacked sines through a one-pole filter, which
is a church organ. The second has all four, and the difference is the whole
difference between a tone and a throat.

**Synthesised, not recorded** — `tools/gen_sounds.py`, pure stdlib to WAV then
ffmpeg to Ogg Vorbis. A synthesiser lives in the repository and a microphone does
not: no licence to track, nobody to credit, no binary anybody has to trust, and
the whole set regenerates in about a second. Retuning one is editing a number.

It also keeps them honest. Synthesis is good at drones, sub-bass, breath and room
tone and bad at voices and growls — **so nothing here attempts a roar**, which
suits him exactly. Everything he does in this mod is pressure, weather and
absence.

`anger` is deliberately *not* a roar: a sub-bass drop with noise swelling under
it and a soft clip, so the room gets heavier rather than something snarling. A
roar would file him with every other hostile in the game.

**The bed is the only sound here nobody is meant to notice.** 22 seconds,
crossfaded end to end so the loop has no seam, **attached to the player** rather
than played at a position — so it follows, and there is nowhere in the dimension
that is quiet. Two drones a little out of tune beating against each other, and a
wind on a different period so the two never line up the same way twice. The test
of it is that leaving for the overworld feels like silence.

**And it rolls away across the country.** Hitting him made one noise at one
point at **volume 1** — which in Minecraft is 16 blocks of range, so the loudest
thing in the whole mod could not be heard from the far side of a field. It landed
like a door closing.

More reverb in the *file* is not the fix: a longer sound is just a longer sound,
and the player still hears one object in one place going on a bit. What reads as
a landscape is the same sound arriving **again, later, quieter, from somewhere
else**. An echo is a return, not a tail.

So `ModSounds.roll` plays the direct hit loud enough to carry, then throws 2–4
returns back off the country from random bearings — each further out, later,
quieter and a few per cent lower in pitch. Minecraft has no filtering, so
"duller" is done with pitch, which is not what distance does physically and is
exactly what it sounds like.

**It reads the room.** Under open sky the returns are 30–80 blocks out and up to
a second apart — a valley. Underground they are close and fast — a cellar. The
caller never has to know which it is in.

Used by the blow that hurts him, the taunt, and his death (volume 5, so it
reaches whoever is standing anywhere on the server).

**What stayed vanilla, on purpose:** his arrival cue is still the step sound of
whatever block he is standing on — DESIGN.md is explicit that it must read as the
world making an ordinary noise rather than as a mod cue. And the possessed
animals' anger stays an animal's, because an animal going wrong should sound like
an animal.

Total: **262 KB** for all five. Subtitles included.

### His world

The ending no longer closes. Killing him raises the frame he was cutting and
**it holds** — because he did not finish it, the fight did.

`the_way` is our own portal block. Vanilla's resolves its destination by
hardcoded dimension key, so borrowing it would mean a mixin into somebody else's
teleport path. It goes **both ways**, cannot be broken or carried, and the
landing on the far side is **cut before you arrive** — floor, walls, return
frame — so nobody lands in terrain.

**White and black**, not purple. White has been his since the first pair of eyes
in this mod. Eight animated frames of drifting filaments, generated by
`tools/gen_the_way.py`; every frame is ranked to exactly the same amount of
light, so it drifts rather than pulses.

**A dark forest that is always midnight, in permanent storm.**

| | how |
|---|---|
| always night | its **own clock and timeline**, generated by `tools/gen_his_night.py` — vanilla's day curve sampled at tick 18000 and emitted as single keyframes. `/time set day` at home cannot touch it |
| pitch dark | sky light pinned to 0, so every hostile spawns everywhere |
| the creaking is awake | normally night-only; here it is simply a resident |
| rain | `setRainLevel` on **both** sides — per level, so the overworld stays dry |
| lightning | ours, a bolt every ~8s per player, and **one far bolt in four** burns |
| the wood | a distant creak or drip every few seconds, never anything you can walk to |

**Weather turned out to be server-wide in 26.2** — `WeatherData` is one field on
`MinecraftServer` and `ServerLevel.getWeatherData()` hands back the server's
copy. Forcing a storm there would have put a permanent thunderstorm over the
player's own base as the price of a mood somewhere they visit once. So the rain
is `Level.setRainLevel` on the client, which *is* per-level, and the lightning is
thrown by hand.

Fire is allowed to spread there, which is the opposite of every other rule in
this mod. Nothing in his world belongs to the player — **except the castle and
the city**, and those are the one thing it may never touch.

*It burned the whole dimension down once. Half the bolts were real, in the most
flammable biome in the game, and the rain was **client-side only** — so the
player watched it pour while the server sat there convinced the place was bone
dry. Vanilla puts fire out in `FireBlock`'s random tick by asking `isRainingAt`,
and that was answered "no" every time. Nothing ever stopped. The wood went, then
the city, then the castle.*

Four things hold it now, meant to overlap rather than each be sufficient:

| | |
|---|---|
| **one in four** far bolts burns | was one in two |
| **the server is wet** | `setRainLevel` is a per-*level* float, so vanilla's own extinguishing works — and the overworld never hears about it |
| **every real strike books its own sweep** | 30s and 60s, for the canopy, which is the one place rain cannot reach |
| **nothing real near the keep** | refused inside the whole works plus a margin |

### What already lives there

Vanilla mobs, **dressed** — not four new entities. Everything the player learned
in forty hours about how a skeleton moves still applies, which is what makes the
one thing that changed unmissable.

| | |
|---|---|
| **skeletons** | enchanted iron, and the bow throws a **ghast fireball**. It craters where it lands, so the wood rearranges itself whether anybody is watching or not |
| **zombies** | armour rolled per piece and per tier, gaps left in the sets, **enchanted diamond weapons**, speed rolled per mob — one in five faster than a walk. **One in six mounted** on a skeleton horse |
| **creepers** | every one **charged** |
| **endermen** | untouched, deliberately. After an hour of white eyes, a thing behaving exactly as you remember is its own kind of wrong |

**His eyes on all of it.** White — the colour has been his since the first
sighting. The creeper's are 2×2 white with a **red pupil dead centre**, which
needs a 4× overlay because there is no middle of a 2×2 eye. `tools/gen_his_host.py`.

**Nothing drops.** A dimension full of enchanted diamond a player can farm is a
dimension they will farm, and the last chapter would become a gear run.

### And what it gives back

| | |
|---|---|
| **triple drops** | everything you break there gives **three** |
| **the hive** | one of them sees you and everything within 40 blocks comes |
| **skeleton range** | they notice at 48 and **open fire at 32** — out of the fog |
| **your torches reach further** | a ring of light blocks around each one, roughly double the pool |

**Triple is the only generous thing in the place, and it has to be.** Everything
else there is a cost: pitch dark, permanent rain, every mob in enchanted plate,
every creeper charged, and the garrison drops nothing at all. What the player
gets for going is *the ground* — and it turns the last chapter from a corridor
you walk once into somewhere worth the trip back.

Two *extra* sets rather than a loot-table multiplier, so it stacks honestly with
Fortune and Silk Touch: whatever your own tools would have produced, you get
three of.

**The hive is what makes it feel defended rather than populated.** Vanilla
hostiles are individuals — each notices you in its own radius while the rest of
the wood carries on, so a player picks them off one at a time in a corridor of
their own choosing. The alarm propagates from **the spotter**, not from the
player, so it travels outward from where the sighting happened and a second
sighting on the far side of the wood pulls a different group. One caller per
tick, so it spreads through the trees rather than resolving in a frame.

**Skeleton range took two changes, not one.** `FOLLOW_RANGE` alone only decides
how far they *notice*; the range they open fire at is a fixed `15.0F` baked into
`AbstractSkeleton`'s own bow goal. So the goal is removed by class and replaced
at 32 — with a **slower** cadence, because a fireball a second from something you
cannot see is not a fight, it is weather with a damage value.

**Torches: there is no per-dimension light radius.** A torch is level 14
everywhere and the falloff is baked into the light engine; the only dials are
`ambient_light` and the sky, and both lift the *whole* place, which would undo the
dark the dimension is for. So the reach is *added* — invisible vanilla `LIGHT`
blocks ringed around each torch. It sweeps rather than hooks placement, which
means it also **cleans up after itself**: mine the torch out and the halo goes
with it. Only the player's own light; the castle's soul lanterns are left alone,
or the settlement would light up like a football ground.

Enchantments are rolled by `EnchantmentHelper.enchantItem`, the way a table rolls
them — a hardcoded Protection IV on everything reads as a mod within a minute.

*Two implementation notes worth keeping. The skeleton's fireball is a **narrow
mixin** on `performRangedAttack` that refuses unless the level is his — vanilla
still owns aiming, cooldown, difficulty and line of sight, and the bow stays in
its hand so the AI is unchanged. And creepers are charged by `thunderHit` with a
bolt that is never added to the world, rather than by reflection on a private
field: field names survive a dev environment and break on remap, and it is also
simply true — the storm never stops, so everything standing in it has been hit.*

### The keep

**The one competent building in the mod.** Every other place of his is somewhere
he *stopped* — a farmhouse he left, a tower he abandoned, a gaol he walked away
from — and read in order they say a man coming apart. This is the other end of
that line: the place he was going, still standing, still lit, built by somebody
who had not come apart at all.

The homestead's roof sags and the church's warren is a hole. **This has
battlements that line up.** That contrast is the reason it is here.

| | |
|---|---|
| curtain wall | 42 across, 7 high, crenellated, arrow slits, a wall walk |
| corner towers | four, 15 high, **soul fire on top** |
| gate | **standing open** |
| keep | 15 across, 19 high, hollow, a stair round the inside |
| stone | deepslate, cracked and cobbled and polished |

**It is found by its light, not by a road.** The dimension's fog closes at 112
blocks, so it is sited at 80–104 — the first thing you see after stepping out of
the landing is a blue glow through the trees with nothing to explain it. A path
would have answered the question before it was asked.

**The gate is open on purpose.** A locked castle is a puzzle and the player goes
looking for the key. An open one is an invitation, which is worse — whoever lives
here is not worried about anybody walking in. The portcullis is up, in its slot,
where you can see it could come down.

Sited **deterministically** off the arrival coordinates rather than rolled, so
two players on a server walk to the same castle.

**The inside is empty**, and that is a placeholder rather than a claim. A player
who climbs it and finds nothing has found nothing, which is honest; one who finds
decorative barrels has been told the building is finished.

### The city under it

Fallen Kingdom opens on a king walking down out of his castle into the village he
rules, past people talking in the street. **This is that shot with the people
taken out of it** — the same streets, the same houses, the same lamps still
burning, and nobody at all.

**So nothing here is broken.** No collapsed roofs, no rubble, no scorch marks.
Doors shut, lamps lit, market stalls standing. Every instinct on a build like
this is to smash it up to prove something happened — and smashing it up is what
lets the player file it as a battle they missed. **An intact empty town has no
explanation available at all.**

| | |
|---|---|
| **the motte** | the castle stands 6 blocks above its town, reached by a great stair |
| **streets** | four out from the gate, **following the ground** rather than levelling it |
| **market** | at the foot of the steps — stalls, a paved square |
| **houses** | dark oak timber on a deepslate course, shut doors, glass, **one lit lamp each** |
| **town wall** | cobbles, 4–5 high, ragged — **built to a different standard than the castle**, which says two efforts decades apart |

**It grows into the wood rather than clearing it.** Only the streets and the
house plots are cut; the trees between them stay where the forest put them. A
plot on ground too steep is *refused*, leaving a gap with trees in it — so the
empty lots are the terrain's decision, not a designer's, and no two of these look
alike. That is the same test a nether fortress passes: it belongs to the place it
is in rather than sitting in a clearing that announces a structure.

**Built in stages, one house per tick.** A quarter of a million blocks in one
tick is a visible server stall, and a freeze on arrival would be the worst
possible first impression of the best thing in the mod. It goes up at 144 blocks,
well past sight in this fog, so the staging is invisible.

Every write checks the block first — `setBlock` on something already correct
still costs a neighbour update, a lighting update and a dirty chunk section, and
most of the clearing pass is air above the treetops.

*Also fixed: **`Cadence` would have thrown.** It ran each action from inside the
iteration over its own pending list, so anything that scheduled work from inside
scheduled work appended to the list being iterated and the next `hasNext()` threw
`ConcurrentModificationException`. Nothing had nested a schedule before, so the
fault sat there harmless; the city is the first thing that does. Drained first,
run after.*

### What is inside the keep

**The answer.** LORE.md has held it since the beginning and nothing in the mod
has ever been able to say it: two brothers, a valley, a house. The younger dug
too deep, something came up wearing him, and it killed the family **with his
hands**. The elder could not put an axe in his own brother, so he tore a hole,
put him through it, and sealed it.

This is the far side of that hole. **He has been here ever since — and he has
been building.** A castle laid true, a city with lit lamps and shut doors, and
nobody in any of it. He built a town for the people he killed.

Four floors, one sentence each, getting worse going up:

| | |
|---|---|
| **the hall** | a table laid for **four**, supper still on it, candles still lit. One chair at the head pushed back |
| **the names** | lecterns, open. He is writing his own name down so as not to lose it. *"It has four letters. I am certain of the four."* |
| **the house** | **four models of the homestead**, built from memory, every one wrong — one missing its chimney, one with the door on the long side, one twelve boards instead of eleven, one half taken apart because he was starting again |
| **the watch** | a chair at a window, facing the way the door was. The only violet in the building |

**Nothing here explains anything.** No page says who he was or what came up the
shaft. The books are a man's handwriting failing and the models are a shape the
player recognises from the *first* building in the mod. Someone who has stood in
the homestead gets it in the chest; someone who has not walks past four odd
little houses on a floor. That is the correct failure.

**The one place he speaks at length** — and he is not speaking to the player.
Forty hours of four-word signs aimed outward, and these are notes to himself. He
does not know anybody is reading.

*Violet is only at the top, per LORE.md: the colour belongs to the **thing**, not
to him. Highest room, furthest from the supper table, least of him left.*

**Still to do:** whether anything is standing in it.

### The one who does not sleep

The village was the only place in the world with living people in it, and the
only thing that ever happened to it was the buildings getting worse. Boarded
windows are a scare about a **place**. This is the one about a **person**.

**From WATCHER, and for the rest of the game.** Not a phase's set piece and it
never escalates into anything — it is a thing that can happen any night you stop
somewhere with people, which is what makes a village stop being a safe place to
sleep. Odds climb with the phase and never reach certainty: one in six at
WATCHER down to one in two at SIEGE, **one roll per village per night**, at most
three alive anywhere.

| | |
|---|---|
| **the eyes** | a black pupil in the middle of the iris. No villager in the game has one |
| **he will not trade** | no profession, no menu. Right-clicking does nothing |
| **he does not sleep** | the square empties at dusk and one man is still standing in it |
| **he talks more** | every 2–5 seconds instead of every 10–20, pitched down |
| **by day he watches you** | stops, squares up, and follows you across the square with his face |
| **after dark, if he sees you** | he comes, with an axe, and does not stop |

Faster than a walk, slower than a sprint — so running works and costs hunger,
and a closed door is a real answer, because unlike the hunt **he does not break
in**. He lives here.

**The morning is not an answer.** Night decides whether it starts; nothing
decides whether it stops except one of them going down. A pursuer who gives up
at sunrise can be waited out.

**Nothing else in the dark gets to have him.** Zombies, skeletons, fire, falls
and somebody else's dog all do nothing — a player can kill him and nothing else
can. Without that he is a mob standing outside all night, and the commonest
outcome is that the event dies before anybody meets it.

**No villager is ever taken.** DESIGN §9 and the whole of Villages refuse to
remove residents. One more person is added instead — indistinguishable from
inside the game, because nobody counts villagers, and it means this can happen a
dozen times over a campaign without a village ever being emptied.

Placed out of everybody's sight, like everything else here. His death message
says *slain by Villager*, which is left exactly as it is.

The texture is a **4x upscale**, and it has to be: a vanilla villager eye is two
pixels — one white, one green — so there is no middle of it to put a dot in.
At 4x the same eye is eight by four. `tools/gen_turned.py`. Test with
`/herobrine turned`. Config: `theTurning`.

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
- [x] Break his line of sight and he loses the trail — but the wait is twenty
      to sixty seconds, rolled fresh each time, so it cannot be counted out
      behind a door. Running is not an escape; hiding is. Digging through your
      wall does not count as losing you
- [x] AND WHEN YOU HAVE GONE TO GROUND HE STOPS MAKING NOISE. Two seconds of
      not moving with no sightline and he puts the axe down: no chopping, no
      pathing, no announcement. What he was doing before was a position report
      four times a second, and a countdown you can hear is not a threat. The
      only sound left is the ten zombies he already sent, gathering at a wall
      they cannot get through either — near, and telling you nothing about
      where he is. The ladder carries on regardless: the glass still goes, the
      torches still go out, and the house comes apart without him ever saying
      which side of it he is on. Move again and the chase resumes instantly
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
- [x] Ends on three blows, on going to ground, or on 88 blocks held for three
      seconds — never on a wall clock
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

### Four ways a hunt ends, and one of them was a reflex

| exit | was | now |
|---|---|---|
| **three blows** | — | unchanged |
| **outrun** | 52 blocks, tested on a **single tick** | **88 blocks, held for 3 seconds** |
| **hidden from** | 8s with no line of sight | **20–60s, rolled fresh each time** |
| **gone to ground** | — | **new: he stops making noise at all** |
| **aged out** | 3 minutes | **removed — replaced by a 2-minute stalemate** |

*Fifty-two was about ten seconds of sprinting, and it fired on one frame past the
line.* Which made the whole event optional: hit him once, watch him reposition
twelve blocks back, hold sprint in a straight line, and it was over before
anything had happened. The three-blow fight, the ladder and the pauses never got
a chance to run.

Eighty-eight and **sustained**, so a moment's gap — a drop off a ledge, a
reposition that lands long, one good burst — is not mistaken for an escape.
Getting away should be something the player *did*.

### And the sky goes with him

The opening turns the weather, which is most of why a hunt lands — a storm that
arrives *because* of something reads nothing like one on a timer. But `Skies.turn`
books **9–18 minutes** and a hunt is three, so the player was left standing in his
thunderstorm with nothing in it for the next quarter of an hour. The causality
that made the arrival work was undone by the same weather five minutes later.

It eases off behind him now, in this order:

```
he goes  →  30s  →  thunder stops  →  60s  →  rain thins  →  clear
```

**Thunder first**, which is what real weather does — the lightning always passes
before the cloud. Standing in the rain listening for thunder that has stopped is
a better ninety seconds than either a storm or a clear sky.

**Only if we started it.** A world already under weather keeps it; clearing
somebody's genuine storm because a hunt ended would be the mod reaching further
than it was asked to.

### A tree used to beat him

*If you climbed a tree during a hunt he stood underneath you doing nothing,
occasionally chipping one block out of the trunk. The single worst thing this
entity can look like — a pursuer you have beaten by climbing.*

The cause was one line in the flight. `glide` landed when
`away < 2.5 && !overhead`, where `away` is **horizontal** and `overhead` meant
"more than a block above them". A player up a tree is horizontally on top of him
and vertically above — **both halves true the instant he left the ground.** He
took off, climbed nothing, landed, got stuck, took off again.

Two changes:

- **He climbs to clear and then comes down on them.** Three above the higher of
  the two while there is ground to cross; their own level once he is over them.
  So the last thing he does is descend onto the branch rather than hover.
- **He only lands when he has actually arrived** — level with them, within a
  block and a half. Nothing else counts.

And a net under it: if the flight runs out with them still out of reach — a
pillar, a boat, a ledge, a hole in a roof — **he stops obeying the geometry and
appears beside them.** He goes through rock, relocates when charged, and ignores
doors; gravity was the last thing he was still obeying, and a tree was the only
place in forty hours where the answer was "he gives up".

The flight still gets six seconds to solve it honestly first, because a figure
climbing toward you is worth far more than one that appears.

### The hunt is the chapter now

It used to be one weight of fourteen in a pool of forty-odd, and the church
after it arrived on a sixty-minute clock. A group could pass the whole of
HUNTER without ever drawing a hunt and get the next building anyway.

*Also fixed: **"he comes home" had never once hunted.** `place()` takes
`(ignoreLight, hunting)` and the call passed `(hunting, false)` — so from MIMIC
the flag went into the **light check** and the hunting flag was hard `false`.
Every time in this mod's history that he came home to a building you were still
in, he stood there watching, while the log line printed "hunting" and everyone
believed it. `ignoreLight` is true outright now: a clock running out inside a lit
building should not care whether the field outside is dark, and asking meant the
whole beat silently failed in daylight.*

**The church is gated on surviving a hunt**, and the hunt is *owed* rather than
rolled for — once the chapter has had its hour, he stops waiting to be drawn.
It cannot stall and it cannot be missed. `/herobrine status` says `hunt STILL
OWED` until it is done, so a late church is never mistakable for a bug.

Every way out of it counts: three blows, three rounds endured, outrun across a
field, lost down a hole. The only one that does not is a hunt nobody laid eyes
on.

**AND TIME IS NOT ONE OF THEM ANY MORE.** There was a three-minute limit, tuned
so that three rounds of pausing would fit inside it, and the tuning was correct
and the idea was not: a wall clock racing the third blow is a wall clock that
eventually wins, and it won by taking the event away from somebody two hits in
at two fifty-five. It counted as surviving, too, so waiting was a strategy. The
backstop is now a STALEMATE — two minutes in which nobody reaches anybody and
no round closes — which catches the one case the limit was really for, a player
in a boat on a lake, and catches nothing else. The ladder deliberately does not
reset it; the house coming apart is not him making progress on a person.

**Three blows and he goes.** Counted in blows and not in damage, so a netherite
axe and a stone sword end it in the same three connections. The first two put
him out of reach again, so it is three *separate* hits on something faster than
a sprint. Six people manage it in seconds — which is the right reward for
standing together — and one person alone is in real trouble, which is where the
pressure was always supposed to be.

**And it takes the house apart while it happens.** One rung every ten seconds,
in this order, each costing more to put right than the last:

**The sky goes first.** The ladder used to open on a window going in, and that
was the wrong first move: a broken pane is a *small* thing, and small things read
as vandalism. Once a scare has started as annoyance it does not recover.

So the opening is the largest thing available and it costs them nothing. The
storm turns and **6–9 bolts come down across the treeline, the ridge and their
own roof** in the first four seconds, before he is close enough to touch
anything. Something enormous is happening, and *then* it turns out to be
personal. Glass after that is not vandalism, it is the thing getting closer.

**The bolts on the house are visual only** — full spectacle, zero cost. A real
bolt on a plank roof burns the building down, and a mod that burns somebody's
base down over an event they cannot refuse has taken their save rather than
frightened them. **At most three** of the volley actually burn, and only ones out
in the trees, clear of every player and of anything anybody built. You cannot
tell which is which while it happens. You find out in the morning: the wood went
and the house did not.

**Between blows he stands still for 20–30 seconds.** It was three to five, and
three to five is not a pause — it is a gap between two attacks. The player never
stopped moving, never got to look at him, and read the whole hunt as one thing
that hit them three times.

Half a minute changes what it is. He is thirty blocks off, motionless, facing
you, and it lasts long enough that you have to decide what to do with the
time — eat, run, wall up, or stand there watching him back.

*Two bugs found from a single playtest log, and one of them could destroy a save.*

***The approach charge was billing 25 wrath a tick.*** *Five hundred a second,
for as long as anybody stood inside the standoff. One test hunt took a world from
9,000 to 25,000 in forty-six seconds — the whole ladder from RUMOUR to SIEGE is
1,800, so this could carry a save through the entire mod in four seconds, and
did. Two things were wrong: no cooldown, and it charged **during a hunt at all**,
where the player never approached anything — he closed on them. It is the stare's
charge only now, once per approach, on a five-second cooldown.*

***The long pause never once ran.*** `roundOver` *set the 20–30 second watch only
if the reposition succeeded, and that asks for a spot in front of the player,
26–46 blocks off, unobstructed — which fails most of the time in woodland or
hills. On failure it fell through to whatever* `strike()` *had left a tick
earlier: the* **30–55 tick** *step-back he uses between two people in one round.
So the whole hunt ran on the two-second version — hit, 2s, hit, 2s, hit, gone, in
forty-nine seconds. Three fallbacks now, and the last cannot fail.*

**He does not come for you — he sends something and watches.**

A figure that walks over and hits you is a mob with a melee attack. A figure that
stands at thirty blocks, perfectly still, while things rise out of the ground
between you and him, is somebody who has *decided* about you and is not in a
hurry. **The distance is the menace.** He never joins in.

| | |
|---|---|
| **the sent** | 7–10 **baby** zombies, black leather caps, stone axes, **his eyes**. Follow range 64 so none of them lose you in the trees |
| where | between you and him, 7–16 blocks, so backing away from him walks you into the rest |
| | drop nothing, can't pick anything up |

**Real babies, not shrunk adults.** The first version set `SCALE` to 0.72, which
makes a small *adult* — same gait, same proportions, just further-away looking. A
baby zombie is a different creature: its own run, and a hitbox low and narrow
enough that swinging at one in a crowd is a real problem. It is also the one
vanilla mob with a reputation, and borrowing that is free.

**The caps are not decoration.** *Zombies burn in sunlight, and the hunt stopped
waiting for dark two updates ago — so a daytime sending caught fire and was gone
in eight seconds while he was still walking over.* Vanilla's own answer is a hat:
`isSunBurnTick` checks whether the head slot is empty. Dyed black so it reads as
**issued** — a scatter of brown caps looks like zombies that found them; ten
identical black ones looks like somebody handed them out.

Speed is tuned *down* for the baby multiplier, which vanilla applies on top of
the base. The adult figures would have put them well above a sprint, and ten of
those is not pressure, it is an execution. They stay on you and they do not gain.
| **and they go when he does** | burned off the instant the hunt ends |

That last one is not tidiness. Ten armed zombies left standing in somebody's base
after the event is over is not a scare, it is a mess somebody has to clean up —
and *they were never really there* is the better reading anyway.

*Also fixed: **he could lose the trail during his own pause.** The blind timer
ran through the whole 20–30 seconds — so he would stop at thirty blocks, send ten
of them, and then lose you because you quite reasonably took cover from the things
he had just sent. The hunt ended mid-approach and everything he sent vanished with
it. He is not searching during a pause: he chose that distance, and he is watching
through what he sent. Hiding is still an answer — it is an answer to being CHASED.*

**A bolt that is aimed, and tells you first.** The one attack in the mod with a
dodge in it: the ground **marks itself**, smokes for a second and a half, and
then the sky comes down on it. §9 asks for warning before lethality and a mark on
the floor is the most literal warning available.

It leaves a **divot** — three across, tapering, 3–4 deep, ragged. Never on
anything built, never on your own square, and the sides are **stepped rather than
sheer**: a hole you cannot climb out of while being hunted is a death sentence
with extra steps.

**The pause is one of two moods, rolled when it begins.**

The first version had him amble round the ring at half a walking pace, lobbing
fire into the country *behind himself*, for half a minute. Which reads exactly as
what it was — a mob whose AI has come apart. **Slow movement with no destination
is the most broken-looking thing an entity can do**, and it undid everything the
long pause was for.

| mood | what he does |
|---|---|
| **circling** | **runs** the ring, never closing, putting fire into the ground around you the whole way. Kinetic and loud — the reason he has not come yet is that he has not chosen to |
| **still** | does not move at all. And then he is **somewhere else on the ring**, without crossing the ground between. Three times, in silence. Nothing thrown, nothing said |

They are opposites on purpose. A player who has had the circling one twice knows
what a pause looks like — and the third time he simply stands there and starts
vanishing is a different event wearing the same shape.

**And the fire comes at you now.** It used to aim *outward* — a bearing taken
from you, through him, and onward 24–52 blocks past. The reasoning was safety and
the result read as broken: he faces you at thirty blocks and lobs fire into the
empty country behind himself. Nobody thinks *he is shelling the hillside*; they
think the aiming is wrong.

It lands **6–18 blocks from you** now, on any bearing, flatter and faster. The
safety moved from distance to a **guard** — the same one the crater rung uses:
nothing lands where anything crafted is within four blocks. The yard gets holes
in it and the house does not.

**And he does not stand still in it — he works.** A motionless figure for half a
minute does not read as menace, it reads as a mob that has got stuck, and once
the player has thought *is it broken* the hunt cannot get it back.

So he paces the ring at 26–46 blocks, in legs — walk, stop, look, walk — **never
closing**, any step that would bring him nearer refused outright. And he throws
fireballs at the country on the far side of him: the treeline, the far field,
never within 20 blocks of anybody and never anywhere with anything built near it.
Something is burning the whole time and **none of it is aimed at the player**.

That is a worse half minute than being chased, because being chased is a problem
with a solution. He is not coming, so there is nothing to run from. He is not
stopping, so there is nothing to wait out. The horizon goes up while you watch.

**His head never leaves you.** The body follows the path round the ring and the
face stays on the player the whole way — the image this entity was built around,
and the reason `getMaxHeadYRot` is 150° instead of the 75° a neck allows. Walking
one way, looking at you, setting fire to something else.

On top of that a bolt every 3–5 seconds — visual only, and **one in three lands
on him**, so a shape at thirty blocks in the rain is silhouetted and there is
nothing left to wonder about.

The ladder also runs *through* the watch: nothing is coming at you and your
windows go anyway.

*Also fixed: **he was visibly dragged across the view every time he relocated.**
Every jump called `snapTo`, which the client does not treat as a teleport — the
position arrives as a sync packet and `InterpolationHandler` smears it over three
ticks, so a forty-block jump was drawn as forty blocks of travel in a seventh of a
second. The player could see which way he went, that he moved rather than
reappeared, and that a mod was teleporting an entity. The oldest rule in the mod,
broken by a rendering default.*

*Turning interpolation off is the wrong fix — it is a property of the entity, not
of the move, so it would snap his walking too, and interpolation is the only
reason a figure crossing a field looks like it is walking. He goes invisible for
four ticks instead, moves, and comes back. Which is not a workaround; it is what
this mod has always claimed happens.*

*That needed a second fix to not be worse: vanilla's `EyesLayer` deliberately
ignores invisibility — it is why an invisible spider is still two red dots — so
the first version hid his body and left a pair of white eyes streaking across the
field on their own.*

A hunt runs as long as it stays a hunt, and a rung lands every 13 seconds — so a
typical visit sees each rung about twice and there is still something left to
come back for.

Then the ladder:

| | what | what it costs |
|---|---|---|
| 1 | **glass**, some of it, never all | drops **sand** — an evening at a furnace, not a loss |
| 2 | **the torches out**, all of them | dropped; straight back |
| 3 | **the treeline**, by real lightning | a wood burning — never within 14 of a player or 24 of the house |
| 4 | **the ground**, by fireball | craters, and craters are the only thing here that does not heal |
| + | **thunder** throughout | the soundtrack |

Glass first because it is the cheapest to repair and the loudest to look at:
it is the one block that says somebody got *in*. If there is none, the ladder
falls through to the next rung rather than spending the beat.

Nothing built is ever taken. The craters refuse outright if there is anything
crafted within four blocks, which does mean a base paved wall to wall gets none
at all — the right failure.

**The three lines are the one time he breaks his own voice.** Everything he has
written for five phases is lowercase, four words, never a threat outright. This
is the only moment in the mod in which he has been hurt, and losing his
composure is the tell that you reached him. Specificity rather than gore, and
the name of whoever hit him:

> *you should not have done that alex* → *i know where you sleep alex* → *soon*

**And if nobody is home he goes to them and tells them.** A group down a ravine
gets a figure in the tunnel and five words: *your house is on fire*. It is
never a bluff — the house really does get its evening, and if the chunk is not
loaded the debt is written down and paid the moment somebody comes back within
range.

Home is measured rather than declared: the densest patch of blocks somebody
built, kept as a per-player hearth and re-measured whenever they are standing
in it. No bed required.

Config: `huntWrecks`, `huntFire`, `damageToBreakOff`.

### Being indoors used to make you invisible to him

`isConfined` was one line — *no sky overhead* — and it was doing two unrelated
jobs. Underground it is right: the surface placement's whole vocabulary is
distance against a horizon, and a cave has no horizon. **Inside a house it was
catastrophically wrong**, because the horizon is still out there; there is just a
plank between you and the sky.

So standing in your own base disabled the two events that most want to find you
in it. He would not stand in the field and look in at the window, and **a hunt
could not start at all**. A player at home was safe from the only two things in
the mod that are supposed to come to your home — and with the church now gated on
surviving a hunt, an indoor group would have had the owed hunt refused every
ninety seconds, forever, with nothing in the game to say why.

Three changes:

- **`buried` replaces `isConfined`** for the surface events. It measures the
  *thickness* of what is overhead — one column, counting solid blocks — rather
  than its existence. A roof is one or two; a three-storey base is four or five;
  thirty blocks down a mineshaft is twenty-something. Six is the line.
- **A window is not a wall.** The sightline test used `ClipContext.Block.COLLIDER`,
  which glass fails — it has a full collision box. It uses `VISUAL` now, which is
  vanilla's own answer to *can you see out of this block*, so glass, panes and
  bars are transparent and nothing here has to keep a list that goes stale.
  Leaves, slabs and fences still block, and should.
- **The hunt has a last resort.** If there is nowhere he could be *seen* from —
  a windowless room — he comes anyway, from 24–48 blocks, with no light, sightline
  or behind-you rule. Those exist to make a sighting work and a hunt is not one:
  being unseen at the start is the mod's oldest rule, not a failure of one. This
  was the last way the chapter could stall.

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
| 2 | The tower | Stair up the outside, the buried house under it, and a 240-block tunnel under that | ✅ |
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

Sited near the players when their phase reaches them — 340 to 780 blocks, out
of sight of everyone — rather than on a ring around world spawn. The phase gate
enforces the reading order for free: nobody meets the shrine before the
homestead, because the shrine does not exist yet.

One per phase, in order, and the next is not sited until the last has been
found — so the sequence cannot be read out of order or skipped past.

| Phase | The new place |
|---|---|
| RUMOUR | 1 the homestead |
| WATCHER | the town — people, exactly when he starts being seen |
| TRESPASSER | 2 the tower |
| MIMIC | 3 the gaol |
| HUNTER | 4 the open church |
| SIEGE | 5 the threshold |

If nobody comes within 1400 blocks of one, it is forgotten and chosen again
near wherever the players have got to. A place can never be stranded behind
them, and the sequence can never stall.

### Everything else outstanding

- [ ] **Village names and signposts**, and roads between villages
- [ ] **The fight** — he flies, throws fire, looses arrows. Deliberately held
      for the ending rather than spent at HUNTER, or SIEGE has nowhere to go
- [ ] **SIEGE: the caves and long paths near him** — still to build

- [x] **A config** — `config/herobrine.json`, one flat file. A master switch, a
      wrath rate, every event, and separate toggles for the three things people
      actually argue about: breaking in, taking torches, leaving fires
- [ ] **Nothing points at the houses or the town** — `/herobrine locate` tells
      an operator where they are, but in-world there are still no signposts or
      roads leading to them
- [ ] The stranger — an NPC who arrives before him
- [ ] Lightning, held back until there is an event for it to belong to
- [ ] Wind — Minecraft has none; would have to be faked with sound
- [ ] Remove the spawn-marker lightning (`/herobrine mark`) before release —
      it is fenced in HauntingSpawner and the command, and nothing else uses it

---

## The path

In the order I would do them, and why.

**1. RUN IT.** ✅ built / ❌ never tested. Nothing in his world has ever executed
successfully — the one attempt crashed on a stack overflow before the portal was
reached. The castle, the city, the keep's four floors, the garrison, the triple
drops, the hive and the torch haloes are all code nobody has watched work. This
is the next thing and it is not close.

**2. Whatever that turns up.** A quarter of a million blocks placed across
staged ticks, a datapack dimension with a hand-written clock and timeline, and
two mixins into vanilla mobs. Something in there is wrong.

**3. Is anything standing in the keep.** The four floors are furnished and empty.
Deliberately, for now — but it is the last open design question in the mod.

**4. The seal reacts.** Visiting the threshold at SIEGE should not show the same
wall you found at TRESPASSER. Turns a dead end into somewhere you check on with
dread. Cheap, and still not done.

**5. The duplicate logic.** Real, catalogued, not urgent. Four copies of the same
chest helper were one of them and are now one. There are more.

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
