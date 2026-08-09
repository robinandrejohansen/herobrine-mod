# Herobrine — design

> This is his world. You are not clearing a dungeon and you are not
> killing a boss — you are being argued out of a place you have decided
> to live. **He wins when you stop playing. You win by staying.**

Status: **design only.** Nothing below is implemented except where marked
`BUILT`.

---

## 0. The spine

Everything in this document answers to one sentence, and anything that does
not answer to it is cut.

**He is trying to make the world not worth being in. The players win by
refusing to go.**

### Why it has to be that and not a boss fight

**In Minecraft, death is nothing.** You respawn, you walk back, you pick your
things up off the floor. A Herobrine who threatens to kill you is a mob with a
text file attached, and every player works that out inside one evening.

Leaving is the only threat in this game that cannot be undone by respawning.
So that is the one he makes. Not *I will hurt you* — **nobody stays**.

### What that makes everything else

| Mechanic | What it is under the spine |
|---|---|
| Editing your base, traps, stolen loot | An **eviction campaign**, not an attack |
| Forest fires, mob waves | Making the land not worth holding |
| Wearing your friends' skins | Attacking the **social** reason you log in |
| Your bed telling him where you live | Your claim on the world is how he finds it |
| Razing you back to spawn | His closing argument |
| Sitting after the Ender Dragon | You beat the game. This is the game beating back |

### The two halves of his argument

He shows the players two things constantly, and they say opposite things on
purpose:

- **An empty house is somebody who left.**
- **A grave is somebody who stayed.**

Leaving is what everyone does, and staying is what it costs. He argues both
sides because he only wants one outcome. It is also a lie, and the mod never
says so — the players are the counterexample, if they last.

### He is never defeated

The 2010 mythos has no defeat and inventing one would be the anticlimax. He
gives up. The world goes quiet. **"Removed Herobrine"** is the patch note, the
running joke, and the honest ending, all at once.

### The rule this puts on destruction

**Real but recoverable.** He breaks and burns things that have to be repaired —
walls, floors, a section of roof — and his traps are real and can kill. It costs
an evening. It never costs a week. A mod that deletes a month of somebody's
building has not frightened them, it has just made them quit, and then it has
lost on its own terms: the whole point is that they *could* have stayed.

---

## 1. The pitch

Most Herobrine mods give you a summoning totem and a boss fight. This one
inverts it: he is present from world creation, he escalates on his own, and
for a long stretch of the game **you cannot fight him at all**. You hide, you
light your base, you avoid deep caves at night. Then you find the means to
give him a body, and the game changes from horror to a fight you have earned.

The arc in one line: **denial → dread → siege → reckoning.**

---

## 2. Design pillars

These are the rules that keep it frightening. Every feature below is
subordinate to them.

**Deniability first.** Early events must have a mundane explanation. A torch
is out. A door is open. A pig is dead with no wound. The player should be
able to tell themselves they misremembered. Horror lives in the gap between
"something happened" and "I can prove it". Once he is undeniable, that
tension is spent forever — so spend it late.

**Never twice in a row.** Repetition kills fear faster than anything. The
same manifestation must never fire consecutively, and recently-used events
are suppressed. Variety is not decoration here; it is the mechanic.

**Silence is the majority state.** If something happens every five minutes,
nothing is scary. Long quiet stretches are what make a single sign in a cave
land. The event budget (§5) exists to enforce restraint against our own
temptation to add more.

**He is never seen arriving.** Spawning in view turns him into a mob. Placed
behind you, he was always standing there. `BUILT`

**Player agency must survive.** He can frighten, take, and mark. He must not
softlock a run or erase a build beyond recovery (§9).

**He acts like a player, not a monster.** Building, mining, opening doors,
leaving signs, wearing a skin. The uncanny part is that everything he does is
something *a person* could have done.

---

## 3. The arc

Phases are driven by **Wrath** (§4). Each phase adds to the pool; it does not
replace it — a phase 4 world can still get a quiet phase 1 stare, and should.

| Phase | Name | He is… | Player posture |
|---|---|---|---|
| 0 | **Rumour** | traces only — a sound, a snuffed torch, distant footsteps | unaware |
| 1 | **Watcher** | seen at distance, gone when looked at `BUILT` | unsettled |
| 2 | **Trespasser** | touching the world — signs, small builds, dead animals, opened doors | defensive |
| 3 | **Mimic** | wearing skins and names, possessing mobs, stealing | paranoid |
| 4 | **Hunter** | chasing, flying, breaking in, weather turning | fleeing |
| 5 | **Siege** | hordes, sustained assault, blood rain | fighting or dying |
| — | **Reckoning** | given a body, mortal, furious | hunting *him* |

The player should reach phase 2 in a first evening of play, phase 3 over a
few sessions, and phase 5 only if they have pushed. Reckoning is opt-in — it
requires deliberate preparation, never an accident.

---

## 4. Wrath — the memory

A single persistent number per world, plus a smaller per-player share. This
is the "how much have we done in total" the whole thing hangs on.

**Raises wrath**

| Action | Why it fits |
|---|---|
| Time survived in the world | baseline drift; the world remembers |
| Descending below y-0, first time and per session | depth is his territory |
| Killing mobs, weighted by rarity | he notices violence |
| Mining diamonds / ancient debris | greed |
| Entering the Nether, killing a boss | milestones |
| Destroying his builds or breaking his signs | **defiance — the biggest single jump** |
| Sleeping through a night | you denied him the dark |

**Lowers wrath**

| Action | Why |
|---|---|
| Leaving his marks untouched for a long stretch | submission |
| Being underground in the dark without a light | ? |
| Dying to him | he is satisfied, briefly |

That last one matters: **death should reset a little of the pressure**, or
phase 5 becomes a death spiral the player cannot escape.

Defiance being the largest riser is the key tuning decision. It makes the
player's own reaction the engine of escalation — tearing down his sign is
what summons the next thing. That is a much better loop than a timer.

---

## 5. The manifestation system

The part that determines whether this feels crafted or random.

- **Budget.** At most one manifestation per window (start: 8–20 min, jittered).
  Never a fixed interval — predictable timing is predictable horror.
- **Weighted pool per phase**, with weights shifting as wrath climbs inside a
  phase.
- **Suppression.** An event that has fired cannot recur for N draws. Same
  *category* also suppressed, more weakly.
- **Context gates.** Cave events need a cave. Build events need a base. Sign
  events need a surface to place on. If nothing qualifies, spend nothing —
  do not fall back to a generic event just to fire something.
- **Aftermath quiet.** After a big manifestation, force a long silence. The
  quiet after is part of the event.

---

## 5b. His own behaviour must escalate too

Easy to miss, and it was missed once already: phases add **manifestations**,
but they must also change **how he personally reacts**. Otherwise he retreats
from a player at SIEGE exactly as politely as he did at WATCHER, and the
escalation is only ever in the surrounding furniture.

The chase response is the clearest case.

| Phase | You close on him |
|---|---|
| WATCHER | usually just leaves; one time in three he is behind you |
| TRESPASSER | half the time he is behind you |
| MIMIC | he is behind you, every time |
| **HUNTER** | **he does not move** |
| SIEGE | he closes on *you* |

The HUNTER row is the payoff for all three above it. A player who has spent
hours learning that he always gives ground, and then walks at him and watches
him stand there, has learned something no cutscene could tell them. That beat
is only available because the earlier phases spent so long teaching the
opposite — which is the general principle: **teach a rule, then break it.**

Anything that changes his personal behaviour should be entered in this table
rather than added ad hoc, so the arc stays legible.

---

## 6. Catalogue

### 6.1 Traces — phase 0–1

Cheap, deniable, high value.

- A torch you placed is lying on the floor and that corner is dark.
- Footstep sounds behind you, once, no entity.
- A distant, wrong sound: cave ambience where there is no cave.
- Your crafting table or chest is open when you return.
- One animal in your pen is dead, no damage source, eyes white.
- Rendered at the very edge of render distance for under a second.

### 6.2 Signs — phase 2+

Literal signs, placed where you will find them: in a dead-end, on your own
wall, at the bottom of a shaft. Short, lowercase, no punctuation flourish.
Personal once he knows you.

```
i was here first          stop digging          it is not your house
go back                   i can see the light   you left the door open
this is deep enough       do you sleep          <playername>
```

Escalating variants at phase 3+ use the player's name, their death count, or
the coordinates of their bed. **The bed one is the single most effective
line in the design** — it proves he has been there.

Rules: never more than one sign per manifestation early. A wall of them is
funny; one in the wrong place is not.

### 6.3 Builds — phase 2+

He builds where you spend time. Track dwell time per chunk; his structures
appear at the edges of the places you live.

- His name in block letters on a hillside you can see from your base.
- A 2×2 sand pillar with a redstone torch on top — the classic.
- A small tomb: a stone box with a sign bearing your name.
- Two redstone torches set into a dark wall at eye height. Just the eyes.
- A copy of *your* build, wrong: same shape, obsidian, no door.
- In caves: a sealed room, ores arranged into a face, a corridor that was
  lit an hour ago and is not now.

Later escalation worth remembering: **swap a torch for a soul torch.** The
light is still there, so nothing is missing — it is just the wrong colour.
That is a much better phase 3 beat than taking the torch away, because it
cannot be explained as a block popping off.

Builds should be **discovered, never witnessed**. If the player watches him
place blocks, he is a mob with an AI. If they walk back into their valley and
the letters are on the hill, he is a person who was here while they were out.

### 6.4 Mimicry — phase 3+

The strongest idea in the list, and the one to hold back longest.

- **A fake player.** Standing at distance with a name tag and a skin, an
  ordinary username. Gone when approached. Never fights.
- **He wears your skin and your name.** He is you, standing in your base,
  facing the wall. Save this — it should happen once per world, at most.
- **Possessed mobs.** Any animal or villager can be his: white eyes, doesn't
  flee, faces you and tracks you as you move, will not attack. A possessed
  zombie is more dangerous and hits harder.
- **Theft.** One specific item taken from a chest. Not a stack. Something you
  will notice and doubt yourself about.

### 6.5 Hunting — phase 4+

Where he stops pretending.

- Chase: fast, direct, on the surface, at night. Does not stop at distance.
- Flight: brief, over terrain, to cut you off.
- Breaking in: he removes blocks to reach you. Torches go out around him.
- Ambush at a chokepoint — the ladder up your mineshaft.
- Weather turns on his arrival: rain, then lightning that lands *near* you.

### 6.6 Siege — phase 5

- **Blood rain.** Sky and rain tinted red. Persistent, unmissable.
- **Hordes.** Waves of possessed mobs converging on the player's position,
  raid-shaped: announced, escalating, with a lull between waves.
- Lightning that starts fires at the perimeter of your base.
- He appears during the horde, not fighting, watching.

---

## 7. Making him mortal — the Reckoning

He cannot be hurt. Damage passes through; he vanishes if pressed. The
endgame is a deliberate ritual that gives him a body.

**The proposal: an Effigy built from what he has done to you.**

Reagents are drops from his own manifestations — so the endgame is gated on
having *survived content*, not on grinding ore:

- **Ash** — from breaking one of his builds.
- **A marked sign** — one of his signs, broken and kept.
- **A hollow eye** — dropped by a possessed mob when killed.
- **Something of yours he took** — recovered from where he leaves it.

Assemble on an altar; he is bound and mortal until the fight resolves. He
fights properly: teleports, summons, breaks terrain, drains light.

Why this shape: it makes the horror phase *the tutorial for the boss fight*,
and it rewards the player for engaging rather than hiding. It also means the
player chooses when the fight happens — consent matters for the arc to feel
like triumph rather than relief.

---

## 8. Hiding — the defensive layer

"Run and hide" only works if hiding is a real mechanic with real limits.

| Defence | Works until | Notes |
|---|---|---|
| Bright light | phase 4 | he avoids lit areas early; later he unlights them |
| Fully enclosed room, no windows | phase 4 | later he breaks in |
| Sleeping | always partially | ends the night, but raises wrath |
| Being near villagers / iron golems | phase 3 | he possesses them instead |
| Water / boats | never | he is unbothered, and knowing that is its own scare |

The progression should teach the player that **every defence eventually
fails**, which is what pushes them toward the Effigy.

---

## 9. Anti-frustration rules

The line between horror and an annoying mod. Non-negotiable.

- **No irreversible destruction of player builds.** He may open doors, snuff
  torches, take one item, place blocks *nearby*. He does not burn your house
  down or break your chests.
- **From HUNTER he will break IN, and this is the one deliberate exception.**
  Shelter is the right answer to almost everything he does, and by the fifth
  phase it has to stop being one — a pursuer that gives up at a wooden door is
  not a pursuer, it is weather. So he mines through: the correct tool in his
  hand, the cracking overlay, one visible swing at a time, slow enough that
  the player watches it happen and has time to decide what to do about it.

  The word doing the work above is *irreversible*. **Every block he takes out
  drops**, so the cost is the wall and the evening and never an item. He
  refuses containers outright — he is coming through the wall, not through
  your chest — and refuses anything indestructible rather than standing at
  bedrock swinging forever. That bargain is what keeps this the wrong side of
  frightening instead of the wrong side of griefing, and it is the same one
  the torches make.
- **Everything he places is removable** and drops normally.
- **THE ENDING IS EXEMPT, and knowingly.** Act three of the Reckoning throws
  real lightning that burns and hurts, and his death leaves a scorched ring,
  a failed portal and a half-built ruin. Defeating him is meant to cost the
  world something visible — a last act that cannot break anything is a
  fireworks display. The permanent SIEGE storm works against the fire the
  whole time, and `realLightning` in the config turns every bolt back to
  cosmetic for anybody who would rather keep their forest.
- **Warning before lethality.** Phase 4+ events telegraph — weather, sound,
  a sign — so death feels earned, not arbitrary.
- **Death lowers pressure**, so a bad run recovers.
- **A config for everything**, including full disable of griefing-adjacent
  behaviour. Some players want the stare and none of the theft.
- **Never touch another player's stuff in multiplayer** without that player's
  own wrath justifying it.

---

## 10. Risks and unknowns

Flagged honestly, to be resolved before the relevant phase is built.

- **Skin mimicry is technically uncertain.** Player skins come from Mojang's
  session servers and are cached client-side. Rendering *the local player's*
  skin on an entity is very likely feasible; arbitrary usernames may not be.
  Needs a spike before §6.4 is promised.
- **Blood rain** probably needs a client-side render hook or biome/fog
  manipulation, not a simple particle. May be phase-5-only for that reason.
- **Hordes are a performance risk**, especially on a laptop. Cap concurrent
  entities hard and prefer fewer, tougher mobs over swarms.
- **Dwell-time tracking** for build placement needs a cheap representation —
  per-chunk counters, decayed over time, not a full heatmap.
- **Multiplayer** is resolved as follows, and the split is deliberate:

  **The seal is shared.** Wrath is one world number and the phase is the same
  for everybody, because the door is as open as it is regardless of who
  opened it. Two players in one world are in one story.

  **His attention is personal.** Each player has their own share, their own
  pacing window and their own suppression list. A player who tears his signs
  down and chases him is visited noticeably more often than someone quietly
  farming beside them — the window shortens by up to a third at high personal
  share.

  Suppression being per-player is not a nicety: shared, one player's stare
  blocks another's, and two friends playing together silently starve each
  other of content without ever knowing why.

  Sign text is chosen from the reading player's own stats, so two players at
  the same base get different words.

  **The dog knows.** A tamed wolf plants itself, faces and growls at one of
  his animals within 24 blocks, or at him within 40 — through walls, before
  the player can see anything. It never attacks; the decision stays the
  player's.

  This is the one piece of the mod that GIVES the player something. Everything
  else works by removing what they relied on: the torch goes out, the animal
  will not behave, the window does not hold. A player who tamed a wolf and kept
  it alive has bought a warning system, and it pays out exactly when the rest
  of this is at its worst. It is also why a dog that could be turned would be a
  better single scare and a far worse mod — the reward for keeping it would be
  that it eventually betrays you, which teaches the player that investment is
  punished.

  **Tamed animals are never taken.** A wolf someone tamed, named and walked
  halfway across a world with is the most loaded thing he could take, and
  taking it would be the most effective scare in the mod — which is exactly
  why it is out. §9 says he never touches what the player earned, and there is
  no counter-play against losing a pet. A wild wolf is fair game, and one of
  his can never then be tamed, since interaction with anything of his is
  refused. The dog that will not become yours is available; the dog that
  already is yours is safe.

  **A possessed animal belongs to one player.** It follows its owner, rings
  its owner, and walks past everyone else as though they were furniture. This
  is the clearest expression of the split anywhere in the mod: to a bystander
  it is a cow that has singled out their friend, which is worse for both of
  them than a cow that hates everybody.

  It is also load-bearing rather than flavour. Without an owner, a mob standing
  between two players has its facing overwritten twice a tick and visibly
  flickers between them, its place in the ring oscillates, and the catch-up can
  fire twice in a sweep and drag it toward whichever player was iterated last.

  Killing one of someone else's transfers the two it spreads to **the killer**,
  and the defiance lands on the killer as well. Helping a friend cull their
  herd is how you inherit it, and there is no way to take this on for someone
  else without taking it on.

  An owner who logs off or leaves the dimension does not hand their animals to
  anybody. They stop where they are — silent and motionless until that player
  comes back.

  **The Journal is shared**, on the same reasoning as the seal. There was one
  journal; the elder brother wrote it once and tore it up once. Two players
  assemble one account between them, which gives them something to compare and
  a reason to talk — and because the pages are ordinary books, whoever finds
  one can hand it over or leave it in a chest.

  A player joining a mature world therefore arrives partway through the story.
  That is intended: they are joining a haunting already in progress, and the
  earlier pages exist in the world for them to be shown.

  Still open: whether one player pushing the world to SIEGE is acceptable for
  a group that did not choose it. Currently it is, on the grounds that a
  haunting you can opt out of is not a haunting.

---

## 11. Build order

Each step should be playable before the next begins.

1. **Wrath + phases + the manifestation budget.** Infrastructure first — it is
   what everything hangs on, and it is invisible, so it must be built before
   there is content pressure.
2. **Traces, signs, and the Journal** (phase 0–2). Cheapest content, highest
   atmosphere per line of code. Proves the pacing works. The Journal is the
   only lore-delivery mechanism we build — signs are his voice, pages are the
   story, and nothing else narrates. Four voices telling one story is noise.
3. **Builds and dwell tracking** (phase 2).
4. **Possessed mobs** (phase 3), then the fake player.
5. **Hunting behaviours** (phase 4) — chase, break-in, weather.
6. **The Effigy and the fight** — before phase 5, so there is a way out.
7. **Siege and hordes** (phase 5) last, once there is a counter to them.

Note step 6 before step 7 deliberately: shipping the siege before the means
to end it would make the mod unwinnable and unfun.

---

## 12. Already built

- Entity with real emissive eyes and body cracks (`RenderTypes.eyes`)
- Stalk-to-standoff-distance goal; does not approach or attack
- Vanishes when watched, with smoke and sound
- Haunting spawner: darkness-gated, out of view, one at a time
