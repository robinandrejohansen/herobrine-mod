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

### A5 — Vanishing when watched

- [ ] Look at him — roughly, you do not need to be precise — and hold it
- [ ] After about **2 seconds** he disappears
- [ ] Leave him alone entirely: he goes on his own after **30 seconds**
- [ ] Walk toward him: at about **8 blocks he is simply gone**. You can never
      reach him
- [ ] He leaves a **puff of smoke** and a **teleport sound**
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
- [ ] He is roughly **26–44 blocks** away when he appears
- [ ] He does **not** appear in bright daylight on the surface
- [ ] He **does** appear in an unlit cave at midday
- [ ] There is **never more than one of him** near you at once

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
- [ ] `/herobrine provoke` twice in a row: the second says *nothing —
      suppressed*, because the same event cannot fire consecutively
- [ ] Quit to title and rejoin — **wrath is still there**

> At wrath 0 you are in RUMOUR, which is below the stare's minimum phase — so
> a brand new world gives you nothing at all until wrath reaches 60. That is
> correct. Use `/herobrine wrath 60` to skip ahead when testing.

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

## B. Deliberately NOT in the build yet

Do not report these as bugs — they are simply not written.

| Not there | Note |
|---|---|
| **He can be killed** | He has 40 health and no protection, so you can just hit him. Per the design he should be unkillable until the Effigy — not implemented. |
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
