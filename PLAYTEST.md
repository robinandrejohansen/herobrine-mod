# Playtest checklist

What is actually in the build, described as things you can see and do — not
as code. Work down the list; anything that fails is a bug worth reporting.

Launch with `./run.sh`. Commands need cheats: **Esc → Open to LAN → Allow
Cheats: ON → Start LAN World**.

---

## A. In the build right now

### A1 — He exists and can be summoned

- [ ] `/summon herobrine:herobrine ~ ~ ~25` places him 25 blocks away
- [ ] He is player-shaped and player-sized
- [ ] His name shows as **Herobrine** (not `entity.herobrine.herobrine`) when
      you aim at him with `F3+B` hitboxes on

### A2 — He looks right in the light

Do this at **midday, above ground** (`/time set day`).

- [ ] He looks like **Steve** — same build, warm skin, brown hair, short
      sleeves, blue jeans, grey shoes
- [ ] The shirt is **oxblood red**, not Steve's cyan
- [ ] Eyes are **dark hollow sockets** — not glowing, not white
- [ ] **No cracks, no marks, nothing unusual on his body** — he is just a
      person in ordinary clothes
- [ ] He casts a shadow and is lit normally by the sun

> The point is that he reads as *a person*, not a monster. A shambling corpse
> is a mob; a man standing in your valley with your posture and no eyes is
> someone. If he looks like a zombie variant, the texture failed.

### A3 — The glow

Now `/time set midnight`, or go into an unlit cave.

- [ ] Eyes are **pure white and fully bright**
- [ ] **Nothing else on him glows at all** — see [LORE.md](LORE.md): glow on
      his body reads as magic, and magic is not frightening
- [ ] The eyes are **exactly as bright in pitch darkness as in daylight** —
      they do not dim with the surroundings
- [ ] The rest of him (skin, clothes, hair) *does* darken normally

> This is the emissive layer. If the eyes dim in the dark like the rest of
> him, the render layer is not working.

### A4 — Standoff stalking

- [ ] Summon him ~30 blocks away and stand still
- [ ] He walks toward you and **stops at roughly 18 blocks**
- [ ] He does **not** keep closing the distance
- [ ] He turns to face you and tracks you as you strafe
- [ ] Walk toward him — he does **not** back away, he just stops advancing
- [ ] Walk more than ~32 blocks away — he stops following entirely
- [ ] He never attacks you, ever, at any range
- [ ] **Hit him** — the damage does nothing at all, and he leaves
- [ ] **Chase him** — closing the distance raises wrath sharply. Check with
      `/herobrine status` before and after. Chasing him is supposed to be the
      mistake, not the solution
- [ ] Chase him and **turn around**: sometimes he is standing where *you*
      were when he arrived, facing you. No sound, no effect — you never see
      him move
- [ ] Sometimes he just goes instead. **You should not be able to predict
      which** — at WATCHER it is roughly one chase in three
- [ ] `/herobrine wrath 200` (TRESPASSER) and chase again — it should happen
      about half the time. At MIMIC (500) it should happen every time
- [ ] He relocates at most twice per visit, then leaves for good
- [ ] Roughly one visit in three, **torches near you go out as he leaves** —
      all dropped as items, nothing lost

### A5 — Vanishing when watched

- [ ] Look at him — roughly, you do not need to be precise — and hold it
- [ ] After about **2 seconds** he disappears
- [ ] Leave him alone entirely: he goes on his own after **30 seconds**
- [ ] Walk toward him: at about **8 blocks he is simply gone**. You can never
      reach him
- [ ] There is **no puff, no teleport sound** — he is just not there
- [ ] A **single footstep sounds from somewhere else** a moment later
- [ ] Look slightly off to the side instead — he stays indefinitely
- [ ] Stand far away and glance at him — takes longer / does not trigger
      (the required aim gets tighter with distance)
- [ ] Put a wall between you and look at him — does **not** vanish

### A6 — He turns up on his own

The main event. No commands.

- [ ] `/time set midnight`, then just play normally — walk, mine, build
- [ ] Within a few minutes he appears somewhere **behind you**
- [ ] You **never see him arrive** — he is always already standing there
      when you turn around
- [ ] Roughly one arrival in three makes **a single ordinary noise** behind
      you — grass, stone, gravel, whatever he is standing on. It should sound
      like the world, not like the mod. If you learn to treat it as a warning
      signal, the odds are wrong
- [ ] He is roughly **26–44 blocks** away when he appears
- [ ] He does **not** appear in bright daylight on the surface
- [ ] He **does** appear in an unlit cave at midday
- [ ] **Deep in a cave**, he appears *in the cave with you* — not on the
      surface above, and not inside rock
- [ ] **In a 2x1 mined tunnel**, he appears down the tunnel behind you, at
      the edge of your light, with a clear line of sight to you
- [ ] He never appears round a corner where you could not possibly see him
- [ ] There is **never more than one of him in the entire world** — not
      per player, not per area. Two clients far apart cannot both have one

> Too rare to test comfortably? Say so and I will drop the spawn chance
> temporarily. It is one constant.

### A7 — Wrath and pacing (Step 1)

Mostly invisible by design. These commands are the window into it — they need
cheats, same as `/summon`.

- [ ] `/herobrine status` prints wrath, phase, your share, seconds until your
      next possible manifestation, and the **light level where you stand** —
      if that reads "too bright for him", he will not appear here
- [ ] Kill a few mobs — wrath goes up by 1 each
- [ ] Sleep in a bed — wrath jumps by 12
- [ ] Stand below y=0 for a minute — wrath rises faster than on the surface
- [ ] Die — wrath drops by 40 (he is satisfied, briefly)
- [ ] `/herobrine wrath 250` — phase should move to **TRESPASSER**
- [ ] Phase names in order: RUMOUR → WATCHER → TRESPASSER → MIMIC → HUNTER →
      SIEGE
- [ ] `/herobrine provoke` makes him appear immediately — **needs darkness**
      (`/time set midnight` or go underground). In daylight it now tells you
      exactly that instead of failing silently
- [ ] `/herobrine provoke force` places him regardless of light, for checking
      how he looks without waiting for night
- [ ] **`/herobrine provoke the_stare`** runs one named event directly,
      ignoring the pool and suppression — use this to test a specific thing
      rather than rolling for it. Tab-completes the list.
- [ ] A failed `provoke` now lists what it tried and what was suppressed
- [ ] `/herobrine provoke` twice in a row: the second says *nothing —
      suppressed*, because the same event cannot fire consecutively
- [ ] Quit to title and rejoin — **wrath is still there**

> At wrath 0 you are in RUMOUR, which is below the stare's minimum phase — so
> a brand new world gives you nothing at all until wrath reaches 60. That is
> correct. Use `/herobrine wrath 60` to skip ahead when testing.

### A7b — Signs (TRESPASSER)

```
/herobrine wrath 200
/herobrine provoke the_sign
```

You need a wall within 10 blocks — stand in a tunnel, a cave, or your base.

- [ ] A sign appears on a wall **behind you**, never where you were looking
- [ ] Short, lowercase, no punctuation: `go back`, `stop digging`,
      `this is deep enough`
- [ ] It is a normal oak sign — you can break it and keep it
- [ ] Provoke it several times: **the same words never come back** for at
      least six signs
- [ ] `/herobrine wrath 500` (MIMIC) and provoke again — lines using **your
      username** start appearing (the dev client is now named `Robin` rather
      than `Player393`; change it in `build.gradle` under `runConfigs.client`)
- [ ] **The lines respond to what you have been doing.** Go below y=0 and
      provoke — he should mention depth. Sleep in a bed, then provoke — he
      should mention watching you sleep. Do not sleep for an in-game day and
      he asks whether you sleep at all. Die three times and he notices
- [ ] Break one of his signs, then provoke a few times — he starts referring
      to it
- [ ] **Break one of his signs**, then `/herobrine status`: wrath jumps by 60,
      the biggest single rise in the mod
- [ ] Break a sign *you* placed yourself — wrath does not move

> The lines are warnings, not threats — see [LORE.md](LORE.md). The player is
> meant to read them the wrong way. If they land as generic monster taunts
> rather than as someone trying to make you stop, the writing has failed.

### A7c — The Journal (TRESPASSER)

```
/herobrine wrath 200
/herobrine provoke the_page
```

- [ ] A **written book** appears titled `torn page 1` — **inside a nearby
      chest** if there is one, otherwise on the floor behind you. Put a chest
      down and provoke to see the chest path
- [ ] Floor pages **do not despawn** — walk away for ten minutes, come back,
      it is still lying there
- [ ] Provoke again *without* collecting the first: **nothing happens**. There
      is never a second page while one is still waiting
- [ ] Collect it, then provoke: page 2 arrives
- [ ] **Abandon one deliberately**: leave a page uncollected, travel 100+
      blocks away, wait ten in-game minutes, then provoke. The *same* page is
      brought to you and the abandoned copy is removed — you are never locked
      out of the rest of the account, and there is still only one of it
- [ ] The log line says whether it went `in a chest` or `on the floor`
- [ ] Right-click to read it — it uses Minecraft's own book UI
- [ ] The author shows as a single dash, not a name
- [ ] Provoke repeatedly: pages arrive **in order**, 1, 2, 3…
- [ ] At TRESPASSER it stops at **page 6** and refuses to give more
- [ ] `/herobrine wrath 500` (MIMIC) unlocks up to page 11; HUNTER 15; SIEGE 16
- [ ] `/herobrine status` shows `pages 3/6 readable now`
- [ ] Quit and rejoin — your progress is remembered, you do not restart at 1

> The pages are the ONLY thing that carries the story. Read them in order and
> judge whether the arc lands: ordinary → something is wrong with him → it is
> not him → I cannot kill my brother → I sent him back → it is thinning.
>
> The handwriting is meant to degrade. Page 1 is a man writing carefully;
> page 15 has lost its punctuation and page 16 is not the same person. Nothing
> ever says he was losing his mind — if you notice it happening without being
> told, it worked.

### A7d — Ruins (TRESPASSER)

```
/herobrine wrath 200
/herobrine provoke the_ruin
```

Stand somewhere open with flat ground around. **The command now tells you
where it went** — `ran THE_RUIN | at -113 79 2 (41 blocks away)` — so you can
walk to it rather than hunting. Signs and pages report their position too.

- [ ] One of three appears: a **doorway** standing alone with nothing behind
      it, a **cairn** with a grave marker, or the **footprint** of a house
      that is no longer there
- [ ] Provoke several cairns: the **grave never says the same thing twice** —
      `and his family`, `she was first`, `the youngest`, `who dug`
- [ ] At TRESPASSER the name is always **worn away**
- [ ] `/herobrine wrath 500` (MIMIC) and provoke cairns: **your own name**
      starts appearing on graves. In multiplayer, sometimes another player's
- [ ] Everything is **mossy, cracked, weathered** — never fresh stone
- [ ] **Redstone dust** on the ground around it, like a stain
- [ ] Cobwebs and dead bushes
- [ ] The doorway has a single **redstone torch** in it, at head height,
      **mounted on a post** — not floating in the gap
- [ ] **Nothing floats.** Cobwebs only sit in corners touching the stone, dead
      bushes only on ground that can hold them
- [ ] All of it is ordinary and can be mined
- [ ] It is **never raised on top of anything you built** — try provoking
      while standing in your base and it should pick somewhere else entirely

> The reaction to aim for is "what is this, and why is it here" — a question,
> not an answer. If it reads as decoration, or as obviously the mod, it has
> failed. The trick is the contradiction: mossy cracked stone is the
> vocabulary of something that has stood for centuries, and it was not there
> an hour ago.

### A7e — Possessed mobs (MIMIC)

```
/herobrine wrath 500
```

Get some animals around you — a few cows or pigs, at least 6 blocks away — or
stand near a village. Then look away from them and:

```
/herobrine provoke possessed_mob
```

The command prints which animal and where. Now go and look at it.

- [ ] It **stops moving entirely**. No wandering, no grazing
- [ ] It **turns its whole body to face you**, and keeps facing you as you
      walk around it
- [ ] It makes **no sound at all** — no moo, no oink, nothing
- [ ] It does **not flee** when you approach, or when you hit it
- [ ] It never attacks you
- [ ] Walk 40 blocks away and back — it is still there, still watching, and it
      does not despawn

**Then walk away and keep walking.** This is the real test:

- [ ] Stand next to it and stare at it for two minutes. It does **not** reveal
      — you have to walk away from it, not watch it
- [ ] Walk away so it has to follow. After about half a minute of it actually
      walking after you, the log says it **stopped pretending**
- [ ] Or outrun it entirely so it has to catch up: that reveals it at once
- [ ] Kill it and let it spread. The two new ones start over — unrevealed

**The eyes:**

- [ ] While it is stalking — at MIMIC, however long it has followed you — it
      looks **completely normal**. No glow at all, ever
- [ ] `/herobrine wrath 1000`: every one of his in the world lights up **on
      the same tick**, pale and cool
- [ ] `/herobrine wrath 1800`: they go **red**
- [ ] Take it into a dark cave or wait for night: the eyes are **exactly as
      bright** as at noon. World light does not touch them
- [ ] They still read as part of the face at ten blocks, not as a light source
- [ ] Say if they are still too strong, or now too faint to notice at dusk —
      the colour and opacity are two numbers in `tools/gen_possessed_eyes.py`
- [ ] Works on cows, pigs, sheep and villagers. Anything else behaves the same
      but does not glow — that is expected, not a bug
- [ ] Ordinary animals standing next to it never glow
- [ ] The eyes are in the right place on the face for each of the four

**What it can see:**

- [ ] **Before** it reveals: put a wall between you. It stays stopped and
      silent but does **not** track you through it. Step into view and it
      turns
- [ ] **After** it reveals: wall it in, stand on the far side of a hill, go
      behind three blocks of stone. It faces you through all of it
- [ ] Witnesses after a kill only turn if they can actually see the killer

- [ ] It **comes after you**, on foot, slower than you walk
- [ ] Sprint 200 blocks across open ground to your base. Wait. It arrives —
      it does not give up and it does not get stuck a chunk behind you
- [ ] It never arrives *in front of* you or while you are looking at the spot
- [ ] Do the same underground: run out of a cave and along a tunnel. It
      appears **in your passage, behind you** — never embedded in stone or
      stranded on the mountainside above the cave
- [ ] **Go down a cave mouth twenty blocks and wait.** They come. Vanilla
      pathfinding will not walk an animal down a drop, so this is the case
      that used to fail silently — the flock milled about on the surface and
      it looked like the mod had stopped
- [ ] Same again behind a **closed door**, across **water**, or up a
      one-block ledge. Roughly eight seconds of getting nowhere and they
      arrive anyway
- [ ] They never arrive inside solid rock, and never on the surface above you
- [ ] Kill it. Run `/herobrine status` before and after: wrath goes up by 25,
      as **defiance**, not as an ordinary mob kill
- [ ] Keep culling and watch the price fall. Around the 30th kill it is worth
      about 15; by the 63rd it bottoms out at 4 and stays there
- [ ] `/herobrine status` shows the toll, so you can check the price against it

**When you kill it:**

- [ ] It drops a **bone and rotten flesh** on top of its normal drops
- [ ] Every ordinary animal within 20 blocks **stops and turns to face you**,
      in silence, for about six seconds — then goes back to being an animal
- [ ] **Two of the watchers never stop watching.** They are his now — walk
      away and they follow. You did not have to spawn or provoke anything
- [ ] Kill those two and it spreads again. The herd builds itself
- [ ] `/herobrine status` shows `his animals killed n/100`. At 100 it stops
      spreading, and never resets
- [ ] It stops spreading if 16 are already his nearby, so it cannot run away
      with the whole world
- [ ] Ordinary witnesses that were *not* taken go back to normal after six
      seconds — they do not follow and never attack
- [ ] Reload the world during the six seconds: nobody is still frozen

**Try to interact with a possessed one:**

- [ ] Right-click it. **Nothing happens at all** — no trade screen on a
      villager, no breeding, no lead, no sound
- [ ] A normal villager next to it still trades fine

**Dogs:**

- [ ] Tame a wolf. It can **never** be taken — not by `provoke`, not by the
      spread when you kill one next to it
- [ ] Your tamed dog still freezes and stares for the six seconds after a kill
      like everything else
- [ ] A **wild** wolf can be taken
- [ ] Feed a possessed wild wolf bones: nothing happens. It cannot be tamed
- [ ] Hit a possessed wild wolf. It **does not fight back** — it takes the hit
      and goes on looking at you

**Your dog knows:**

- [ ] With a tamed wolf following you, get one of his animals within 24 blocks.
      The dog **plants itself, turns to face it, and growls** every couple of
      seconds
- [ ] It **refuses to follow you** while the thing is there
- [ ] It does **not** attack it — that decision stays yours
- [ ] The direction it faces is always right. Put two of his on opposite
      sides: it faces the nearer one
- [ ] `/herobrine provoke the_stare` — the dog growls at **him** from up to 40
      blocks, through walls, before you can see him
- [ ] With both a possessed cow and him nearby, it faces **him**
- [ ] Tell the dog to sit. It stops warning — a sitting dog was told to stay
- [ ] **It still fights.** With one of his animals 20 blocks off, let zombies
      come at you at night. The dog **stops warning and defends you** — it
      never stands frozen while you are being hit
- [ ] Once the zombies are dead it goes back to warning
- [ ] Nothing nearby: it behaves like an ordinary wolf

**At higher wrath he takes more than one:**

```
/herobrine wrath 1000
/herobrine provoke possessed_mob
```

- [ ] **Two** animals are taken (four at `/herobrine wrath 1800`)
- [ ] They do **not** move as one. With four of his, some set off at once,
      one hangs back, and a couple give up halfway
- [ ] One that gives up goes back to **behaving like an animal** — grazing,
      wandering, moving on its own. Still silent
- [ ] It starts coming again on its own after fifteen to forty-five seconds
- [ ] The same individual behaves the same way each time. One sheep is always
      the flighty one; another never stops
- [ ] **Stay in one place long enough and all of them arrive.** Nobody is ever
      permanently lost
- [ ] Run 60+ blocks away: even the ones that had lost interest come. Distance
      overrules it
- [ ] Once one has **revealed**, it never loses interest again

**When they turn (HUNTER, `/herobrine wrath 1000`):**

- [ ] Eyes on. No more ring, no more keeping their distance — they **come
      straight at you**, faster than an ordinary animal
- [ ] The head is **locked on you the whole way**, even while running and
      turning corners. An angry cow looks where it is going; this does not
- [ ] They **hit you**. Not hard — the danger is that there are several and
      they do not stop
- [ ] They are still ordinary mobs. You can kill them, and you should
- [ ] Drop back to `/herobrine wrath 500` and they go back to stalking, eyes
      off
- [ ] They **ring** you rather than piling up on one side. Stand still in a
      field with six of his and they spread out around you
- [ ] They stop at **different distances**. One is almost in the doorway,
      another is a shape at the treeline twenty blocks out. You should have to
      look around to find them all
- [ ] They **arrive at different times**, not as a group — some walk noticeably
      slower than others
- [ ] The same animal always keeps the same distance and the same pace. The one
      that hangs back always hangs back
- [ ] The far ones still stop and stare rather than drifting back to grazing
- [ ] Each keeps its own side as you walk about — they do not reshuffle
- [ ] Go into your base and stay a while. They gather around **the building**,
      spread out, all facing it
- [ ] Do the same in a cave: they come down and take up positions in the
      passages around you
- [ ] Walk right up to one. It **holds its ground** and stares — it never
      backs off to keep its distance
- [ ] Only adults, never babies
- [ ] It is never taken while you are looking at it

> The behaviour is the whole scare and it should not need explaining. A
> vanilla animal is never still. If a stopped, silent cow tracking you across
> a field does not feel wrong, then something is not working — check it is
> actually facing you rather than just aiming its head.
>
> Known gap: **the eyes do not glow yet.** That needs a separate texture per
> mob type aligned to each one's UV map, and is a follow-up.

### A7f — Possessed mobs with two players

Needs a second player. Open the world to LAN (`Esc` -> `Open to LAN`, allow
cheats) and have someone join.

- [ ] Get one of yours following you, then stand next to your friend. It faces
      **you**, and keeps facing you as they walk around it. It never flickers
      between you
- [ ] Walk apart. It follows **you**, and ignores them completely
- [ ] Both of you get your own. They do not share a flock, and each animal
      rings its own owner
- [ ] Have your friend kill one of **yours**. The two new ones follow
      **them**, not you — and the defiance lands on them
- [ ] Log out with some of yours following you. Your friend finds them
      **standing still and silent** where you left them — not wandering, not
      transferring to them
- [ ] Log back in: yours resume following you
- [ ] Go through a nether portal with some following. They stop rather than
      twitching toward a player who is not in that world
- [ ] Kill one in front of your friend: the herd turns to face **the killer**,
      not the bystander
- [ ] `/herobrine status` shows the same `his animals killed n/100` for both of
      you — the toll is the world's, not yours

### A7g — They get through the window (SIEGE)

```
/herobrine wrath 1800
```

Build a small shelter with a **glass** window at eye level, go inside at night
and let zombies find you.

- [ ] Stand behind the glass where they can see you. They **come for you and
      keep coming** — they do not lose interest and wander off
- [ ] A zombie that has you as its target starts on the glass. **Vanilla
      cracks spread across the pane** as it works
- [ ] It takes about **ten seconds**, with glass ticking under its hands
- [ ] The pane breaks and **drops nothing**
- [ ] Move out of reach — it stops, and the cracks **heal**
- [ ] Kill it mid-chew: the cracks vanish rather than sticking on the block
- [ ] Two zombies on the same window each show their own progress
- [ ] It **only** touches glass. Stone, planks, doors, floors: untouched
- [ ] Replace the pane with **iron bars**. They cannot touch it — that is the
      real answer to this
- [ ] Put a **stone wall** between you instead. They lose you again, as normal.
      Only glass is see-through
- [ ] Stand behind four or more panes stacked up: they stop noticing you. It is
      a window, not x-ray vision
- [ ] Drop below SIEGE (`/herobrine wrath 0`) and nothing chews at all

> This is the only thing in the mod that damages what the player built, which
> is why it is glass only and the final phase only. A pane costs a shovelful of
> sand; losing one is a fright rather than a loss.

### A7h — The homestead

```
/herobrine house
```

Reports where it is. It is 1100–1900 blocks from world spawn and it does not
move — the seed decides, not your position. Walk there, or to look at it now:

```
/herobrine house here
```

**Outside:**

- [ ] The **building** sits flat and level. No corner floating, no corner
      buried
- [ ] **Raise it in a dense forest.** It sits on the ground, not above the
      canopy. This is the case that was broken — every Minecraft heightmap
      counts a tree trunk as terrain
- [ ] On a slope it gets a **cobblestone footing** under its low side, like a
      real house would — not a lump of dirt
- [ ] The **yard follows the ground.** The fence, the field, the path, the
      well and the graves run up and down the slope. There is no giant flat
      rectangle around it
- [ ] The yard keeps its own ground: podzol in a taiga, sand near a beach. No
      patch of plains grass stamped over it
- [ ] Nothing in the yard is buried in a bush or left floating over a dip
- [ ] A path leads to the door, a dead wheat field, a well
- [ ] A sheep pen with the **gate shut** and bones inside it
- [ ] **Three** graves. Count them, then count the names in the books

**Inside — the main room:**

- [ ] A hearth wall, a table with chairs, a lectern with a book on it
- [ ] The lectern book (`the house book`) reads as an ordinary household
      diary until its last line
- [ ] Chests hold `my book` and `the small room`
- [ ] A store room at the back with barrels and the cellar hatch
- [ ] Interior is **four blocks high** everywhere, and the rooms are big
      enough to move around furniture in
- [ ] There is a **roof**, with holes in it letting daylight through in
      stripes, and an overhanging eave all the way round

**The bedroom:**

- [ ] **Two** beds
- [ ] The chest holds `ledger`
- [ ] The back wall is **mossy cobblestone** where everything else is planks —
      it should be obviously a different job

**Behind that wall:**

- [ ] There is **no door**. You have to break in
- [ ] Inside: one bed, cobwebs, and two signs in **his** handwriting —
      lowercase, no full stops, unlike every other word in the building
- [ ] A chest with `tally`. Read all five pages

**Under the floor — this is the bigger half of the building:**

- [ ] A hole in the store room floor with a ladder down to a small cellar
- [ ] Past the cellar it stops being built and starts being **dug**. No
      corners, no courses, no right angles — uneven ceilings, passages that
      wander and change width
- [ ] One chamber, then a passage that gives up. More than a farm needed, and
      deliberately far short of what the later houses will be
- [ ] Timber props shored into the passages at intervals: somebody cut this
- [ ] Red torches only. Never a normal torch
- [ ] Cobwebs, gravel underfoot, dripstone. Nothing valuable
- [ ] The passage goes deeper and **stops mid-stone**. No wall, no door,
      nothing at the end
- [ ] No ore, no rails, no branch mining. Nobody dug this **for** anything
- [ ] It never breaks the surface — you cannot walk out of a hillside
- [ ] It never floods. If it cuts a water or lava pocket, that is sealed

**The loot:**

- [ ] Every chest still has its book, every single time. Raise it ten times
      with `/herobrine house here` and check the sealed room always has
      `tally`
- [ ] The rest of the contents **differ between raisings**
- [ ] Nothing in it is exciting. Wheat, string, coal, wool, a bucket, at most
      a couple of iron. No diamonds, no enchantments, no armour
- [ ] Tools come out **half worn** — nobody left a pristine hoe behind
- [ ] Some chests hold something useless. That is deliberate

> If you ever open one and think "I'm kitted out now", that is a bug and tell
> me. The house should feel like a help, never like a prize — the moment it
> pays out it stops being somewhere people lived.

> The test is not whether it is scary. Nothing in it moves. The test is
> whether you come out having worked out what happened without being told —
> and whether you counted the beds and the graves.

### A1b — Footsteps and the fuse (RUMOUR)

```
/herobrine provoke footsteps
```

- [ ] **Seven to nine separate steps**, not one noise. You can count them
- [ ] They **move** — something crossing behind you, left to right or right to
      left, not stamping on one spot
- [ ] Walking pace. Not a sprint, not a stagger
- [ ] **It sounds like the ground you are standing on.** Grass in a forest,
      gravel on a shore, wood on your floorboards, stone in a cave. Run it in
      three biomes and check
- [ ] Stand where grass meets stone: a sequence crossing the line **changes
      sound partway through**
- [ ] Run it several times. Sometimes it is almost on top of you, sometimes
      across the clearing — the distance is not always the same
- [ ] You turn round and there is nothing there

```
/herobrine provoke the_fuse
```

- [ ] A creeper hiss **right behind you**
- [ ] It never goes off. No explosion, no damage, no broken blocks
- [ ] The silence lands at the moment a real creeper would have detonated —
      about a second and a half
- [ ] Then **one footstep**, close, just after

> Say plainly if the fuse is too much this early. It is the most conditioned
> sound in the game and it works on reflex, which is exactly why it might not
> belong in phase 0 — it is easy to move to WATCHER.

### A7i — The sighting

```
/herobrine provoke the_stare
```

Needs darkness. It reports where he is — turn and look.

- [ ] He is **already standing there**. He never walks up to you and you never
      watch him arrive
- [ ] He does **not** approach. Stand still and he stays where he is
- [ ] **Look away for a second** — turn round, step behind a tree, glance at
      your hotbar. When you look back he is gone
- [ ] Nothing announced it. No sound, no particles, no teleport effect
- [ ] Walk at him. Inside about twelve blocks he **turns and leaves**, faster
      than you can sprint. You never close the gap
- [ ] The moment anything breaks your view of him — a trunk, a corner, a rise
      — he is gone
- [ ] In a cave he backs **into the rock face** and is gone
- [ ] Sometimes instead he is behind you. That is the old behaviour and it is
      still there, roughly one approach in two to three depending on phase
- [ ] Chasing him still raises wrath. Check `/herobrine status` before and
      after

> The eyes should read as **pale**, not as headlights, and should not look
> like they are throwing light onto his face.

### A7j — The sighting with two players

- [ ] Player 1 sees him and stands still. Player 2 sprints at him from a
      different angle: he reacts to **player 2**, not to whoever saw him first
- [ ] Player 1 keeps staring while player 2 looks away. He **stays** — it takes
      every pair of eyes losing him
- [ ] Split up and keep him between you. You hold him there noticeably longer
      than one person can, but he still leaves within a few seconds of fleeing
- [ ] Approach from opposite sides. When he runs he does **not** bolt into the
      second player — he goes for the gap
- [ ] Stand directly either side of him with no gap: he goes immediately rather
      than jittering between you
- [ ] Both of you walk him down: **both** take the defiance. Check
      `/herobrine status` for each of you
- [ ] When he reappears behind someone it is never in the other player's face
- [ ] Only ever **one** of him in the world, however many players are online

### A7k — The homestead with two players

- [ ] `/herobrine house` reports the **same coordinates** for both of you
- [ ] Whoever gets there first triggers the building; the other walks up to the
      same house, already standing
- [ ] It is never built twice, and never moves once built
- [ ] Books and loot are whatever the first arrival's roll produced — the
      second player sees the same chests, not their own version

### A7l — The threshold (house 5)

```
/herobrine threshold
```

Reports where it is — 2600–3600 blocks out, always further than the homestead.
To look at it now:

```
/herobrine threshold here
```

**Above ground — a compound, abandoned where it stood:**

- [ ] **Two roofless outbuildings**, walls collapsed to uneven heights, a gap
      where each door was
- [ ] A **dead field** — farmland with nothing on it but dead bushes
- [ ] A fence line that is mostly gaps
- [ ] Barrels, a cauldron, slabs and dirt patches left lying about
- [ ] **Worn paths** of coarse dirt and gravel between the outbuildings, the
      field and the hole — grown back over in patches
- [ ] A collapsed opening in the ground with steps down
- [ ] **Not one sign anywhere on the surface.** Every word in this place is
      underground, in a book

**Going down:**

- [ ] A cut stair, about 34 steps, walled in brick
- [ ] The stonework gets **rougher the deeper it goes** — the workmanship
      gives out on the way down
- [ ] The lights are **there at all** — they were not before. Brackets on the
      side walls of the stair, every few steps
- [ ] Most are **dead**: unlit redstone torches still fixed to the wall, a
      fifth missing altogether, only some still burning
- [ ] Then it stops being built and starts being dug

**Finding your way — this should take a while:**

- [ ] Three separate runs of passage, not one corridor
- [ ] **Cave-ins.** One you can crawl over the top of, one you have to mine
      through
- [ ] **The route has a laid floor** — stone brick and cobble underfoot where
      the rock is bare everywhere else. Follow the paving
- [ ] Brickwork lining the passage every so often, like a doorway somebody
      made permanent
- [ ] **Two branches that go nowhere.** They have **no paving and no
      brickwork** — that is how you tell, and you should work it out in about
      ten seconds without being told
- [ ] Both end in collapse
- [ ] It should now be findable but not obvious. Say if the paving gives it
      away too fast, or still is not enough

**The records room, before the cells:**

- [ ] Bookshelves along both walls with **gaps** where an armful was taken
- [ ] Two lecterns: `intake` and `on the door`
- [ ] A chest with `plainly, once`
- [ ] Read `intake` **before** you walk into the cells. It changes what they are

**The cells — this is the moment:**

- [ ] Right angles, squared brick walls and iron bars **inside a cave that has
      none**. That contrast is the whole thing
- [ ] Eight cells, four a side, along a corridor you have to walk the length
      of. There is no way round
- [ ] Bones, cobwebs, hanging chains. **No beds and no bowls** — nothing that
      says anybody was kept alive in here
- [ ] Exactly **one** cell has its bars pushed **outward** into the corridor,
      with two bars left standing in the walkway. Not broken into. Broken out
- [ ] A few words scratched in his hand in some cells

**The office, at the far end:**

- [ ] You come out of the corridor into it, then **turn round and see the
      corridor through a wall of glass** — from the seat somebody sat in to do
      exactly that
- [ ] Roughly one pane in four is **out**. Something went through that glass
- [ ] Brewing stands, a cauldron, a smithing table, a desk facing the window
- [ ] A lectern with `nine`, a chest with `the last day`
- [ ] Dirty and knocked-about, not strewn with debris — a room somebody left
      quickly

**The seal:**

- [ ] An obsidian frame with newer, cracked deepslate bricked into it — the
      opening was cut, then filled in from this side
- [ ] Chains, and redstone staining the floor in front of it
- [ ] It does **nothing**. No portal, no particles, nothing to interact with
- [ ] `we put it back / it did not hold`

> The reward for reaching the bottom is not a dimension. It is the certainty
> that there is one, that somebody shut it, and that the seal is cracked.

### A8 — Living the pacing (the real test)

Everything above is mechanical. This is the one that tells you whether the
mod *works*, and it needs time compression — you cannot judge pacing you have
to wait 20 minutes to see.

```
/herobrine speed 20      pacing window drops from 8-20 min to ~24-60s
/herobrine wrath 60      skip to WATCHER so the stare is in the pool too
/time set midnight
```

Now **put the commands away and just play.** Mine, build, wander. Do not
provoke anything.

- [ ] Things happen without you asking, spaced out, not on a rhythm
- [ ] You get **footsteps behind you** at least once — and turn around
- [ ] You find a **torch lying on the floor** that you had placed on a wall,
      with that corner now dark
- [ ] The same thing never happens twice in a row
- [ ] There are stretches where nothing happens at all
- [ ] Nothing that happens is *destructive* — you can put the torch back

The questions that actually matter, and only you can answer them:

1. Did you turn around when you heard the footsteps?
2. When you found the fallen torch, did you doubt yourself for a second — or
   did you immediately think "the mod did that"?
3. Was the quiet boring, or was it tense?

If (2) is "the mod did that", the traces are too strong for phase 0. If (3)
is "boring", the window is too long even at normal speed. Both are tuning,
and both are things I cannot see from here.

> Set `/herobrine speed 1` when you are done. x20 is a testing lie — real
> pacing is much slower, and judging the *feel* of the slow version needs a
> normal session.

### A9 — He does not wreck anything

- [ ] He never breaks blocks
- [ ] He never opens or takes from chests
- [ ] He never damages you or your animals
- [ ] Nothing about your world is different after he leaves

---

### A10 — Two or more players

Needs a second client, or a friend on Open to LAN.

- [ ] Both players get their own manifestations, at their own times
- [ ] **Only one of him exists at a time, world-wide.** If he is visiting one
      player, the other gets a trace instead — never a second Herobrine
- [ ] The **same event can happen to both** — one player seeing the stare does
      not block the other from it
- [ ] The player who breaks signs and chases him gets visited **noticeably
      more often** than the one quietly building
- [ ] `/herobrine status` shows the same **wrath and phase** for both, but a
      different **share**
- [ ] Signs address whoever they were written for, using that player's own
      habits — sleep, depth, deaths
- [ ] **The Journal is one account, shared.** If player A collects page 3,
      player B's next page is 4 — not their own page 3. There are never two
      copies of the same page in the world
- [ ] Either player can hand a page to the other; they are ordinary books

---

## B. Deliberately NOT in the build yet

Do not report these as bugs — they are simply not written.

| Not there | Note |
|---|---|
| **He drops nothing** | No loot table yet. |
| **Signs, builds, theft** | None. |
| **Possessed mobs, mimicry** | None. |
| **Chasing, flying, attacking** | None. He only ever stalks and stares. |
| **Weather, lightning, hordes** | None. |
| **Spawn egg** | You need cheats and `/summon`. |
| **Config** | Everything is hardcoded constants. |

---

## C. What each next step adds, in player terms

Ordered as in [DESIGN.md](DESIGN.md) §11. Each becomes a new section of this
checklist as it lands.

### Step 1 — Wrath, phases, pacing

**What you will see:** almost nothing new, and that is expected. This is the
memory and the pacing engine. The one visible addition is a debug command:

- `/herobrine status` — prints your current wrath, phase, and time until the
  next possible manifestation

**How to test:** kill mobs, mine diamonds, go below y-0, break one of his
signs (once signs exist) — watch the number move. Confirm his appearances get
more frequent as it rises, and that they never fire two of the same kind in a
row.

> Building this first is deliberate. It is invisible, so if we leave it until
> after there is content, it never gets built properly.

### Step 2 — Traces and signs

**What you will see:** a torch you placed lying on the floor. Footsteps
behind you with nothing there. Your chest lid open. One animal dead with no
wound. Then, later, **actual signs** with short messages — in a dead-end
tunnel, on your own wall.

**How to test:** play a long session and note what you find. The bar to pass:
every early event should have a *plausible mundane explanation*. If your first
reaction is "the mod did that", it is too strong for this phase.

### Step 3 — Builds

**What you will see:** you come home and his name is spelled out on a hillside
you can see from your base. A sand pillar with a redstone torch. Two redstone
torches set into a dark wall at eye height, like eyes. In caves: a sealed room,
a corridor that used to be lit.

**How to test:** stay in one area for a long time, then leave and come back.
Key check: **you should never witness him building.** If you see blocks being
placed, it has failed.

### Step 4 — Mimicry

**What you will see:** a player standing at distance with a name tag, who is
gone when you approach. Animals and villagers with white eyes that track you
and will not flee. One item missing from a chest. Eventually — once per world
— **him wearing your skin and your name**, standing in your base.

**How to test:** approach the fake player, check possessed animals do not flee,
audit a chest you know the contents of.

### Step 5 — Hunting

**What you will see:** he stops keeping his distance. He runs at you. He
breaks blocks to reach you. Torches go out around him. Weather turns when he
arrives; lightning lands near you.

**How to test:** this is the first step where **you can die to him**. Verify
every lethal event telegraphs first — weather, sound, or a sign — so death
never feels arbitrary.

### Step 6 — The Effigy and the fight

**What you will see:** the things he leaves behind become usable. Ash from
breaking his builds, a sign you kept, an eye from a possessed mob, the item he
stole. Assemble them and he becomes **mortal** — and fights back properly.

**How to test:** the whole arc, start to finish. Confirm the fight is winnable
with gear you would realistically have.

> Deliberately before the siege. Shipping step 7 first would make the mod
> unwinnable.

### Step 7 — Siege

**What you will see:** blood rain. Waves of possessed mobs converging on you,
announced and escalating like a raid. He watches without joining in.

**How to test:** framerate, honestly. And that a prepared player survives.

---

## D. Reporting

For anything that fails, the useful details are: what phase/step, what you
were doing, whether it was day or night, above or below ground, and whether
`run/logs/latest.log` has anything red in it.
