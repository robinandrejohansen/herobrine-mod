package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Cadence;
import com.bloomlet.herobrine.manifest.ManifestationDirector;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Places him in the world. WHEN is the director's call, not this class's.
 *
 * Deliberately NOT vanilla natural spawning. Biome spawn rules would give him
 * a spawn weight and a pack size, which means groups of Herobrines in caves —
 * exactly wrong. A haunting is one figure, rarely, where you did not expect
 * one.
 *
 * Three rules make it work:
 *   1. DARKNESS, not time of day. Gating on light level means he also appears
 *      down a mineshaft at noon, and the emissive eyes only pay off in the
 *      dark anyway.
 *   2. OUT OF VIEW. He is placed behind you. He should never pop into
 *      existence while you watch — the whole effect is "he was already there".
 *   3. ONE AT A TIME. If a Herobrine already exists near the player, nothing
 *      spawns. Two of him is a mob; one of him is a ghost story.
 */
public final class HauntingSpawner {
	private HauntingSpawner() {}

	/**
	 * How far out he stands.
	 *
	 * Pushed a long way back after playing it. At twenty-six blocks he is a
	 * figure you can make out, and something you can make out is something you
	 * can assess — you look at him, decide what he is, and the moment is over.
	 * At forty-plus he is a shape at the treeline that might be a fence post,
	 * and the player spends several seconds deciding whether to walk towards
	 * it. Those seconds are the entire event.
	 */
	private static final double MIN_RADIUS = 42.0;
	private static final double MAX_RADIUS = 68.0;
	/**
	 * The fallback ring, tried only when the whole of the proper one failed.
	 *
	 * Standing well back is the intent and it is still what gets tried first.
	 * But in broken country -- a peak, a ravine, a coastline -- there may be
	 * no visible standable ground at all between forty and seventy blocks, and
	 * the choice is then between a closer figure and no figure. A closer
	 * figure is a worse version of the event. No figure is not the event.
	 */
	private static final double CLOSE_RADIUS = 26.0;

	/**
	 * THE SKY MARKS WHERE HE ARRIVES — AND THIS STARTED AS A DEBUG AID.
	 *
	 * It was a visual-only bolt on every single spawn, added so a tester could
	 * see where he had actually gone instead of walking a bearing and hoping. It
	 * was meant to be deleted and it shipped by accident for several versions.
	 *
	 * It was also the single most frightening thing in the mod, which is the sort
	 * of thing no amount of design gets you. So it stays — properly, rather than
	 * as a leftover switch.
	 *
	 * WHAT MAKES IT CONTENT RATHER THAN A BUG IS THE PHASE GATE. Every appearance
	 * in the first two chapters is built on him never being SEEN to arrive: you
	 * look up and he is already standing there, and there was no moment to point
	 * at. A bolt announcing every spawn destroys that, and destroys it worst
	 * exactly where the mod is quietest and most fragile.
	 *
	 * So the sky says nothing at all until TRESPASSER, and then it escalates:
	 *
	 *   RUMOUR, WATCHER  nothing. He is never seen arriving.
	 *   TRESPASSER       one bolt, somewhere near, not on him. Weather.
	 *   MIMIC            one bolt on the ground he is standing on.
	 *   HUNTER           two or three, staggered, around him.
	 *   SIEGE            three or four, and the ground he came from is lit up.
	 *
	 * Visual-only throughout, checked against 26.2 rather than assumed:
	 * setVisualOnly(true) gates BOTH the fire and the entity damage in
	 * LightningBolt.tick. It cannot burn a forest down and it cannot hurt
	 * anybody, which is the only reason it is allowed to happen in somebody's
	 * field at all.
	 */
	private static void omen(ServerLevel level, BlockPos pos) {
		Phase phase = Wrath.phase(level.getServer());
		int bolts = switch (phase) {
			case RUMOUR, WATCHER -> 0;
			case TRESPASSER, MIMIC -> 1;
			case HUNTER -> 2 + level.getRandom().nextInt(2);
			case SIEGE -> 3 + level.getRandom().nextInt(2);
		};
		if (bolts == 0) {
			return;
		}
		RandomSource random = level.getRandom();
		// At TRESPASSER it lands NEAR him rather than on him, which is the whole
		// difference between weather and an announcement. A player who walks
		// toward that bolt finds him by accident; one who walks toward a bolt
		// that struck his feet was told.
		double spread = phase == Phase.TRESPASSER ? 9.0 : 3.5;
		for (int i = 0; i < bolts; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = random.nextDouble() * spread;
			final double x = pos.getX() + 0.5 + Math.cos(angle) * range;
			final double z = pos.getZ() + 0.5 + Math.sin(angle) * range;
			Cadence.in(level.getServer(), i * (3 + random.nextInt(6)), () -> {
				net.minecraft.world.entity.LightningBolt bolt =
					net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
						.create(level, EntitySpawnReason.EVENT);
				if (bolt == null) {
					return;
				}
				bolt.setVisualOnly(true);
				bolt.snapTo(x, pos.getY(), z, 0.0F, 0.0F);
				level.addFreshEntity(bolt);
			});
		}
	}

	/** How the sweep is spread: every bearing, a few distances down each. */
	private static final int BEARINGS = 32;
	private static final int DISTANCES = 3;
	/**
	 * How bright it may be where he stands, by phase — and it climbs.
	 *
	 * This was a flat 7, which is "dark enough for monsters", which outdoors
	 * means night and nothing else. That quietly made the mod unplayable for
	 * anyone who sleeps: night is eight minutes in twenty, sleeping removes
	 * almost all of it, and sleeping is ALSO the largest single source of
	 * wrath. So the players provoking him hardest were the ones with no window
	 * left for him to appear in — one outdoor sighting a day of real play. Two
	 * systems that did not know about each other.
	 *
	 * A curve fixes it and says something at the same time. At WATCHER he still
	 * needs proper dark — dusk, dawn, the middle of a storm — and by SIEGE he
	 * will stand in a field at noon. The thing that starts as something you
	 * might have imagined in bad light ends up refusing to need the excuse,
	 * which is the whole arc of the mod expressed as a number.
	 *
	 * Rain does the work in the middle of that range without any special
	 * casing: an overcast sky genuinely lowers the light level, so "he turns up
	 * on grey afternoons" falls out of it for free.
	 */
	private static int maxLight(Phase phase) {
		return switch (phase) {
			case RUMOUR, WATCHER -> 9;
			case TRESPASSER -> 11;
			case MIMIC -> 12;
			case HUNTER -> 14;
			case SIEGE -> 15;
		};
	}
	/**
	 * There is only ever ONE of him, in the entire world.
	 *
	 * This used to be a 96-block radius around the player, which is not the
	 * same thing at all: two players far enough apart would each get their own
	 * simultaneously. Two Herobrines existing at once is the single most
	 * damaging thing that could happen to this mod — the moment there can be
	 * more than one, he is a mob type rather than a person, and everything
	 * else here is built on him being a person.
	 *
	 * It also costs nothing narratively. He is in one place because he is one
	 * thing; if he is with someone else you get a noise instead, and the
	 * director already falls through to the other manifestations.
	 */
	/**
	 * ONE AT A TIME, WHERE THEY CAN SEE.
	 *
	 * This was existsAnywhere, and it swept EVERY LEVEL ON THE SERVER — so one
	 * Herobrine standing anywhere, including in his own dimension, made place(),
	 * glimpse(), passage(), atPlace() and hunt() all return ALREADY_NEARBY. Which
	 * quietly deleted the three heaviest things in the mod: THE_STARE at weight
	 * eighteen, THE_GLIMPSE at twelve and THE_PASSAGE at fourteen. The content of
	 * the two chapters you unlock by finding his house could not happen, because
	 * finding his house is what put a resident in it.
	 *
	 * Rule 3 in this file's own header — "ONE AT A TIME" — was written when he was a
	 * visitor. It is right about what it is protecting: two of him in one clearing
	 * is a category error the mod has avoided since the first version. It was wrong
	 * about the radius, and now that he lives on the far side of the way it was
	 * wrong about the dimension as well.
	 *
	 * So: this level, and near this player. Comfortably wider than WATCH_RANGE, so
	 * there is no encounter in which two of him could both be relevant — and no
	 * longer any way for somebody four thousand blocks off, or in another world
	 * entirely, to switch the haunting off.
	 */
	/**
	 * ONE PER LEVEL, WHICH IS WHAT I SHOULD HAVE WRITTEN THE FIRST TIME.
	 *
	 * The version before this was a hundred and ninety-two block box around the
	 * player, and the reasoning was that two of him in one clearing is the thing
	 * being prevented. Which is true and is not the whole rule: it also means a
	 * player who walks two hundred blocks away from him gets a SECOND one placed,
	 * and in his own world — where he holds station over the keep and the player is
	 * off exploring — that is most of the time.
	 *
	 * The dimension is the right unit. It keeps what the change to this method was
	 * for — him standing in his own world no longer switches off the stares in the
	 * overworld, which is what existsAnywhere did — while restoring the actual
	 * invariant: there is one of him in a world, or none.
	 */
	private static boolean existsNear(ServerLevel level, ServerPlayer player) {
		return HerobrineEntity.oneIn(level) != null;
	}
	/** Above this, the position is in front of the player and unusable. */
	private static final double IN_VIEW_DOT = 0.25;

	/** Why a placement did or did not happen — debug commands need the reason. */
	/**
	 * Why he did or did not turn up.
	 *
	 * Split finer than it needs to be for the code, and entirely for the
	 * person testing. "Wrong surroundings" covered five unrelated situations
	 * and named none of them, and the commonest one by far — he is already out
	 * there from the last attempt — looks exactly like a broken command.
	 */
	public enum Outcome {
		PLACED,
		/** One of him already exists somewhere in the world. */
		ALREADY_NEARBY,
		/** Enclosed, and there is no room to stand back far enough. */
		NO_ROOM_HERE,
		/** Somewhere to stand, but all of it lit. */
		TOO_BRIGHT,
		/** Dark enough, but every option was in front of the player. */
		NOTHING_BEHIND,
		/** Behind and dark, but nothing the player could actually see. */
		NOTHING_VISIBLE,
		/** Ground would not take him. */
		NO_FOOTING,
		BAD_PLAYER;

		public String reason() {
			return switch (this) {
				case PLACED -> "";
				case ALREADY_NEARBY -> "he is already somewhere in this world — "
					+ "wait for him to go, or find him";
				case NO_ROOM_HERE -> "you are enclosed and there is nowhere he could stand "
					+ "9 blocks back in this space — step outside or into a bigger cave";
				case TOO_BRIGHT -> "everywhere behind you is lit — he needs light 7 or under. "
					+ "Try at night, or use 'provoke force'";
				case NOTHING_BEHIND -> "everywhere dark enough was in front of you — "
					+ "turn around and try again";
				case NOTHING_VISIBLE -> "every dark spot behind you is hidden by terrain — "
					+ "he would have been standing somewhere you could never see";
				case NO_FOOTING -> "no solid ground to stand on in the ring behind you";
				case BAD_PLAYER -> "you are dead or spectating";
			};
		}
	}

	/**
	 * Places him behind the player, if the world allows it.
	 *
	 * The director treats anything but PLACED as a quiet night rather than
	 * retrying — see ManifestationDirector.
	 */
	public static boolean spawnBehind(ServerLevel level, ServerPlayer player) {
		return place(level, player, false) == Outcome.PLACED;
	}

	/**
	 * @param ignoreLight debug only. Skips the darkness requirement so he can
	 *                    be placed in daylight for appearance testing.
	 */
	public static Outcome place(ServerLevel level, ServerPlayer player, boolean ignoreLight) {
		return place(level, player, ignoreLight, false);
	}

	/**
	 * @param hunting he follows instead of standing. HUNTER and up only — the
	 *                caller decides, because the spawner has no business
	 *                knowing which manifestation asked for him.
	 */
	public static Outcome place(ServerLevel level, ServerPlayer player, boolean ignoreLight,
	                            boolean hunting) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}

		if (existsNear(level, player)) {
			return Outcome.ALREADY_NEARBY;
		}

		// THE STARE IS AN OUTDOOR EVENT AND NOTHING ELSE.
		//
		// It used to fall back to a cave placement when the player was
		// enclosed, which made one piece of code responsible for two
		// experiences that have nothing in common. Outdoors he is a shape at
		// the treeline sixty blocks off, read against a horizon, and the
		// whole effect is distance. In a cave there is no horizon, no distance
		// and no sky — he is in the corridor with you, and the effect is that
		// there is no way round him.
		//
		// Sharing the code made the cave version a worse copy of the outdoor
		// one: too far to matter in a passage, judged by rules about being
		// behind you that mean nothing in a tunnel with two ends. Underground
		// belongs to glimpse() and passage(), which own their own logic.
		// BURIED, not merely roofed. isConfined is "no sky overhead", which is
		// true of a cave and equally true of somebody standing in their kitchen —
		// so a player at home was exempt from the stare entirely. The horizon is
		// still out there when there is a plank above you; all that has changed
		// is that he has to be looked at through a window, which visibleFrom now
		// allows. See ConfinedPlacement.buried.
		if (ConfinedPlacement.buried(level, player)) {
			return Outcome.NO_ROOM_HERE;
		}

		RandomSource random = level.getRandom();
		// Several attempts, because most candidate rings will be too bright or
		// in front of the player. Failing quietly is correct — he simply does
		// not appear this time.
		// Counted rather than merely skipped, so a refusal can say which of
		// these it was. "Too bright" is a different night from "you were
		// facing the only dark side", and only one of them is worth waiting
		// out.
		int tooBright = 0;
		int inFront = 0;
		int roofed = 0;
		int blocked = 0;
		int noFooting = 0;

		// SWEEP THE CIRCLE, do not sample it.
		//
		// Forty independent random angles sound like good coverage and are
		// not: they clump, and they leave wedges of the horizon untried. From
		// a mountaintop that is fatal, because the player can see an enormous
		// amount of ground and the one direction that was never tested is
		// exactly the one with a visible ridge in it.
		//
		// So every direction gets looked at. Thirty-two evenly spaced
		// bearings, offset by a random amount so he does not favour the
		// compass points, three distances down each one. Ninety-six candidates
		// covering the whole horizon instead of forty landing wherever they
		// fell.
		//
		// Then, only if the whole ring failed, the same sweep again closer in.
		// He is meant to stand well back and that is still tried first, but a
		// player who never sees him has lost the event entirely, and thirty
		// blocks away is a great deal better than nothing at all.
		double[][] rings = {
			{ MIN_RADIUS, MAX_RADIUS },
			{ CLOSE_RADIUS, MIN_RADIUS },
		};
		for (double[] ring : rings) {
			double spin = random.nextDouble() * Math.PI * 2.0;
			for (int attempt = 0; attempt < BEARINGS * DISTANCES; attempt++) {
				double angle = spin + (attempt % BEARINGS) * (Math.PI * 2.0 / BEARINGS);
				double step = (attempt / BEARINGS) + random.nextDouble();
				double radius = ring[0] + (ring[1] - ring[0]) * (step / DISTANCES);
				int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
				int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				BlockPos pos = new BlockPos(x, y, z);

				// Cheapest test first. This is pure arithmetic on two vectors and
				// it throws away roughly a third of every sweep, so running it
				// after two chunk lookups and a raycast was simply paying for
				// answers about candidates that were never going to be used.
				if (isInFrontOf(player, pos)) {
					inFront++;
					continue;
				}

				// GROUND THAT WOULD ACTUALLY HOLD HIM, and this is where he was
				// standing on the sea.
				//
				// Every Minecraft heightmap counts fluid as surface — the real
				// predicate is `blocksMotion() || !getFluidState().isEmpty()` —
				// so over an ocean this returns the top of the water, and
				// nothing downstream ever asked whether that was a floor. He
				// was placed on the waves; and because open water has no trees,
				// no ridge and no canopy, that was ALSO the only place the
				// sightline test could pass. One bug produced the other, and
				// the sea was very nearly the only place he was ever put.
				//
				// It walks DOWN as well, because the heightmap stops above a
				// snow layer, a carpet or a bottom slab and none of those has a
				// sturdy top face — which failed an entire mountainside for
				// ground a mob stands on quite happily. Four blocks clears any
				// stack of those and cannot reach a seabed, so water still
				// refuses at every level.
				BlockPos stand = null;
				for (int down = 0; down <= 4 && stand == null; down++) {
					BlockPos maybe = pos.below(down);
					if (ConfinedPlacement.canStand(level, maybe)) {
						stand = maybe;
					}
				}
				if (stand == null) {
					noFooting++;
					continue;
				}
				pos = stand;
				// And he stands in the open.
				//
				// A spot under a forest canopy satisfies every other rule and is
				// unlookable-at from fifty blocks, because leaves are colliders and
				// the ray dies in them. Requiring sky above him costs nothing, puts
				// him on ridges and in clearings and at treelines where the whole
				// image belongs, and stops the placement loop burning all twenty
				// attempts inside a wood and reporting NOTHING_VISIBLE.
				if (!level.canSeeSky(pos)) {
					roofed++;
					continue;
				}

				if (!ignoreLight && level.getMaxLocalRawBrightness(pos)
					> maxLight(Wrath.phase(level.getServer()))) {
					tooBright++;
					continue;
				}
				// And the player must actually be able to SEE the spot.
				//
				// Behind them and dark enough was not sufficient: a hill, a stand
				// of trees or the far side of a ridge would satisfy both and put
				// him somewhere nobody could ever look at. The whole event is
				// being seen, so a placement that cannot be seen is not a quiet
				// night — it is a wasted one, and the player is left turning on the
				// spot wondering what the command did.
				if (!visibleFrom(level, player, pos)) {
					blocked++;
					continue;
				}

				Outcome result = spawnAt(level, player, pos, hunting);
				if (result == Outcome.PLACED) {
					return result;
				}
				noFooting++;
				HerobrineMod.LOGGER.info("stare: could not create at [{}, {}, {}]",
					pos.getX(), pos.getY(), pos.getZ());
			}
		}
		// Roofed and blocked are counted apart on purpose: "under a canopy" and
		// "behind the brow of the hill" are the same refusal to the player and
		// completely different things to fix.
		int hidden = roofed + blocked;
		HerobrineMod.LOGGER.info(
			"stare refused after {} tries from y={}: {} lit, {} in front, "
				+ "{} roofed, {} behind terrain, {} no footing",
			BEARINGS * DISTANCES * rings.length, player.blockPosition().getY(),
			tooBright, inFront, roofed, blocked, noFooting);
		if (tooBright >= inFront && tooBright >= hidden && tooBright >= noFooting) {
			return Outcome.TOO_BRIGHT;
		}
		if (hidden >= inFront && hidden >= noFooting) {
			return Outcome.NOTHING_VISIBLE;
		}
		return inFront >= noFooting ? Outcome.NOTHING_BEHIND : Outcome.NO_FOOTING;
	}

	/**
	 * A GLIMPSE, underground, and it is the opposite of the stare in almost
	 * every respect.
	 *
	 * The stare puts him BEHIND you, a long way off, and lets you look at him
	 * until you have had enough. This puts him in FRONT of you, close, in a
	 * cave, for about half a second — and it is gone before you have decided
	 * whether it was there. The player is not meant to resolve it. They are
	 * meant to stop, look again at an empty passage, and carry on mining
	 * slightly differently.
	 *
	 * That is the original account of meeting him, almost verbatim: "he looked
	 * at me and quickly ran into the fog". One sentence, no confrontation, and
	 * over immediately.
	 *
	 * NO SOUND, and that is deliberate. Every instinct says to put a cue on it,
	 * and a cue is exactly what turns this from something a player half-saw
	 * into something the mod told them about. If they were looking the other
	 * way, they missed it. That has to be a real possibility or the ones they
	 * do catch are worth nothing.
	 */
	public static Outcome glimpse(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsNear(level, player)) {
			return Outcome.ALREADY_NEARBY;
		}
		// Underground, or it is not this event. Above ground he has the stare,
		// which is a better version of being seen at distance in the open.
		if (level.canSeeSky(player.blockPosition())) {
			return Outcome.NOTHING_VISIBLE;
		}

		RandomSource random = level.getRandom();
		Vec3 look = player.getViewVector(1.0F).normalize();

		for (int attempt = 0; attempt < 48; attempt++) {
			// Close. A figure forty blocks down a tunnel is a shape; one nine
			// blocks away is a person, and nine blocks is also about as far as
			// a torch reaches, so he is at the edge of what the player can see.
			double range = 7.0 + random.nextDouble() * 11.0;
			// Roughly where they are looking, with enough spread that it is not
			// always dead centre — being slightly off to one side is worse,
			// because it makes them turn their head.
			double spread = (random.nextDouble() - 0.5) * 1.1;
			double cos = Math.cos(spread);
			double sin = Math.sin(spread);
			Vec3 out = new Vec3(look.x * cos - look.z * sin, 0.0,
				look.x * sin + look.z * cos).normalize().scale(range);

			BlockPos at = BlockPos.containing(
				player.getX() + out.x,
				player.getY() + random.nextInt(5) - 2,
				player.getZ() + out.z);

			BlockPos stand = null;
			for (int down = 0; down <= 3 && stand == null; down++) {
				BlockPos maybe = at.below(down);
				if (ConfinedPlacement.canStand(level, maybe)) {
					stand = maybe;
				}
			}
			if (stand == null || !visibleFrom(level, player, stand)) {
				continue;
			}

			// ONE OF HIM. See HerobrineEntity.outInTheOverworld.
			if (HerobrineEntity.outInTheOverworld(level)) {
				return Outcome.ALREADY_NEARBY;
			}
			HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
			if (him == null) {
				return Outcome.NO_FOOTING;
			}
			double dx = player.getX() - (stand.getX() + 0.5);
			double dz = player.getZ() - (stand.getZ() + 0.5);
			float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
			him.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 0.0F);
			// Ten to eighteen ticks. Long enough to register as a figure,
			// nowhere near long enough to be studied, and short enough that a
			// player who blinked genuinely did miss it.
			him.beGlimpse(10 + random.nextInt(9));
			him.setAnchor(player.blockPosition());
			level.addFreshEntity(him);
			ManifestationDirector.noteLocation(stand);
			HerobrineMod.LOGGER.info("a glimpse at [{}, {}, {}], {} blocks off",
				stand.getX(), stand.getY(), stand.getZ(),
				(int)Math.sqrt(stand.distSqr(player.blockPosition())));
			return Outcome.PLACED;
		}
		return Outcome.NOTHING_VISIBLE;
	}

	/**
	 * HIM, AT THE HOUSE, THE FIRST TIME ANYBODY FINDS IT.
	 *
	 * The one sighting in the mod that is meant to be SEEN. Everything else he
	 * does at distance is built to be missable — the glimpse is ten ticks and
	 * gone, and that is right, because a scare you are certain of is a scare
	 * you have finished with. This is the exception, and it is the exception for
	 * a reason: finding one of these buildings is the largest thing that happens
	 * in a session. Somebody walked four hundred blocks, and what they get for
	 * it is a house. Standing in the doorway of it, watching them arrive, is the
	 * difference between finding a structure and being SHOWN one.
	 *
	 * So: two seconds, not half of one, and always facing them. Long enough that
	 * everybody on voice chat gets told to look, which is exactly the moment
	 * this is for.
	 *
	 * AND HE IS AT THE BUILDING RATHER THAN NEAR THE PLAYER, which is the whole
	 * grammar of it. A figure beside you is a threat. A figure standing at the
	 * empty house you have just walked four hundred blocks to find is a caption:
	 * somebody lived here, and this is what happened, and he was there for it.
	 * Under the spine of the mod that is his argument in one image — everybody
	 * leaves eventually — made without a word on a sign.
	 */
	public static Outcome atPlace(ServerLevel level, ServerPlayer player, BlockPos site) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsNear(level, player)) {
			return Outcome.ALREADY_NEARBY;
		}

		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 64; attempt++) {
			// Just outside the walls. Close enough to read as AT the building
			// and never so close that he is inside it, because a figure indoors
			// is somewhere the player is about to walk and this is not a fight.
			double angle = random.nextDouble() * Math.PI * 2.0;
			double out = 7.0 + random.nextDouble() * 7.0;
			BlockPos at = BlockPos.containing(
				site.getX() + Math.cos(angle) * out,
				site.getY() + random.nextInt(4),
				site.getZ() + Math.sin(angle) * out);

			BlockPos stand = null;
			for (int down = 0; down <= 6 && stand == null; down++) {
				BlockPos maybe = at.below(down);
				if (ConfinedPlacement.canStand(level, maybe)) {
					stand = maybe;
				}
			}
			if (stand == null) {
				continue;
			}
			// Far enough that he reads as part of the scene rather than as
			// something that has come for them, and near enough to be a person
			// rather than two pixels.
			double away = Math.sqrt(stand.distSqr(player.blockPosition()));
			if (away < 18.0 || away > 90.0 || !visibleFrom(level, player, stand)) {
				continue;
			}

			// ONE OF HIM. See HerobrineEntity.outInTheOverworld.
			if (HerobrineEntity.outInTheOverworld(level)) {
				return Outcome.ALREADY_NEARBY;
			}
			HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
			if (him == null) {
				return Outcome.NO_FOOTING;
			}
			double dx = player.getX() - (stand.getX() + 0.5);
			double dz = player.getZ() - (stand.getZ() + 0.5);
			float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
			him.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 0.0F);
			him.beGlimpse(40 + random.nextInt(21));
			him.setAnchor(player.blockPosition());
			level.addFreshEntity(him);
			ManifestationDirector.noteLocation(stand);
			HerobrineMod.LOGGER.info("he is at the house [{}, {}, {}], {} blocks from {}",
				stand.getX(), stand.getY(), stand.getZ(), (int)away,
				player.getName().getString());
			return Outcome.PLACED;
		}
		return Outcome.NOTHING_VISIBLE;
	}

	/**
	 * HIM, IN THE PASSAGE, AND THERE IS NO WAY ROUND HIM.
	 *
	 * The third of the three, and the one the cave actually asks for. The
	 * glimpse is half a second and gone before you are sure. The stare is
	 * sixty blocks of open country. This is neither: he is nine to twenty-four
	 * blocks down the tunnel you were walking along, standing still, facing
	 * you, for five to eight seconds — and the tunnel is the only way through.
	 *
	 * IT WORKS BECAUSE OF THE GEOMETRY, not because of anything he does. Above
	 * ground a figure at distance is one of many things on a horizon and the
	 * player can simply walk elsewhere. In a corridor there is no elsewhere.
	 * The stare's rules — a long way off, behind you, judged against a skyline
	 * — mean nothing in a tunnel with two ends, which is exactly why this
	 * cannot share its code and did not deserve to.
	 *
	 * ConfinedPlacement is used because it is genuinely a cave tool: it floods
	 * the space the player is standing in and comes back with somewhere in it,
	 * so what it returns is always down a passage that connects. The rules
	 * about how long he stays and what ends it are this method's own.
	 */
	public static Outcome passage(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsNear(level, player)) {
			return Outcome.ALREADY_NEARBY;
		}
		if (!ConfinedPlacement.isConfined(level, player)) {
			return Outcome.NO_ROOM_HERE;   // out in the open; that is the stare
		}

		BlockPos spot = ConfinedPlacement.find(level, player);
		if (spot == null) {
			return Outcome.NO_ROOM_HERE;
		}
		// No light check at all, deliberately. A cave a player has torched is
		// still a cave, and refusing to appear in the one they have lit would
		// mean he only ever turns up where they cannot see him.
		// ONE OF HIM. See HerobrineEntity.outInTheOverworld.
		if (HerobrineEntity.outInTheOverworld(level)) {
			return Outcome.ALREADY_NEARBY;
		}
		HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (him == null) {
			return Outcome.NO_FOOTING;
		}
		double dx = player.getX() - (spot.getX() + 0.5);
		double dz = player.getZ() - (spot.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		him.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);

		// Long enough to be certain, short enough that waiting him out is not a
		// plan. Walking at him still ends it early — the standoff owns that,
		// and being made to back down a tunnel is the best thing that can
		// happen here.
		RandomSource random = level.getRandom();
		him.beGlimpse(100 + random.nextInt(60));
		him.setAnchor(player.blockPosition());
		level.addFreshEntity(him);
		ManifestationDirector.noteLocation(spot);
		HerobrineMod.LOGGER.info("he is in the passage at [{}, {}, {}], {} blocks off",
			spot.getX(), spot.getY(), spot.getZ(),
			(int)Math.sqrt(spot.distSqr(player.blockPosition())));
		return Outcome.PLACED;
	}

	/**
	 * A HUNT, WHEREVER THEY HAPPEN TO BE STANDING.
	 *
	 * {@link #place} is an outdoor placement and refuses underground, which was
	 * entirely correct while the hunt was one event among forty — a quiet night
	 * costs nothing when something else will be along in ten minutes. It stopped
	 * being correct the moment the story started waiting on a hunt: a group who
	 * spend HUNTER in a mine would have had the chapter refused every time it
	 * was offered, and the mod would simply have stopped for them.
	 *
	 * So a hunt gets the cave version for free. The rules underground are
	 * passage()'s rather than the stare's — he is nine to twenty-four blocks
	 * down the tunnel rather than forty across a field — and everything after
	 * placement is identical, because the hunt's own behaviour never cared
	 * where it started.
	 */
	public static Outcome hunt(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsNear(level, player)) {
			return Outcome.ALREADY_NEARBY;
		}
		if (!ConfinedPlacement.buried(level, player)) {
			// AND IT DOES NOT WAIT FOR THE DARK. Every other placement in this
			// file needs light 7 or under, which is right for a figure that has
			// to be half-seen at sixty blocks and wrong for one that is coming
			// whether or not you can make him out.
			//
			// It is also what keeps the chapter from stalling. The hunt is owed
			// now rather than rolled for, and a light gate on an owed event means
			// a group who play daytime sessions get offered it, refused, and
			// offered it again every ninety seconds until dusk — the story
			// waiting on the clock, invisibly, for hours.
			Outcome across = place(level, player, true, true);
			if (across == Outcome.PLACED || across == Outcome.ALREADY_NEARBY) {
				return across;
			}
			// AND IF THERE IS NOWHERE HE COULD BE SEEN FROM, HE COMES ANYWAY.
			//
			// place() requires a spot the player could actually lay eyes on,
			// which is the entire point of the STARE and has nothing to do with
			// this. A hunt is not a sighting: he is not there to be noticed at
			// sixty blocks, he is there to arrive. Being unseen at the start is
			// the mod's oldest rule, not a failure of one.
			//
			// It was also the last way the chapter could still stall. Somebody
			// in a windowless room fails every visibility test there is — so an
			// owed hunt would be refused, and refused again ninety seconds
			// later, for as long as they stayed in the room the hunt exists to
			// come to. The church would simply never be sited.
			return unseen(level, player);
		}
		BlockPos spot = ConfinedPlacement.find(level, player);
		if (spot == null) {
			// Deep, and the space around them will not take him either — a
			// one-block crawl, a boat on an underground lake. The last resort
			// applies here too rather than losing the chapter to it.
			return unseen(level, player);
		}
		// ONE OF HIM. See HerobrineEntity.outInTheOverworld.
		if (HerobrineEntity.outInTheOverworld(level)) {
			return Outcome.ALREADY_NEARBY;
		}
		HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (him == null) {
			return Outcome.NO_FOOTING;
		}
		double dx = player.getX() - (spot.getX() + 0.5);
		double dz = player.getZ() - (spot.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		him.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
		him.setAnchor(player.blockPosition());
		// No glimpse timer. This is the one thing in a tunnel that does not
		// resolve itself by standing still — see HerobrineEntity's hunt.
		//
		// PROWLING RATHER THAN HUNTING. He is down the passage looking for them; the
		// hunt starts when they see him, or when the minute is up. Underground that
		// minute is the best version of it — a corridor with something moving about
		// at the far end of it, and no way to know whether it has noticed you.
		him.beginProwl();
		level.addFreshEntity(him);
		ManifestationDirector.noteLocation(spot);
		him.announceArrival();
		HerobrineMod.LOGGER.info("the hunt starts underground at [{}, {}, {}], {} blocks off",
			spot.getX(), spot.getY(), spot.getZ(),
			(int)Math.sqrt(spot.distSqr(player.blockPosition())));
		return Outcome.PLACED;
	}

	/**
	 * SOMEWHERE HE CAN STAND, AND NOTHING ELSE ASKED OF IT.
	 *
	 * The hunt's last resort, and deliberately the least fussy placement in the
	 * file: ground that will take him, out of arm's reach, and that is the whole
	 * specification. No light level, no sightline, no rule about being behind
	 * them — every one of those exists to make a SIGHTING work, and a hunt is not
	 * one. What happens next is that he walks to them, which needs nothing except
	 * somewhere to start walking from.
	 *
	 * Close in, at twenty-four to forty-eight, because this is reached when the
	 * player is somewhere awkward — indoors, walled in, down a hole — and putting
	 * him seventy blocks out through two hills only means a longer walk to the
	 * same door. He should be at it shortly.
	 */
	private static final double UNSEEN_NEAR = 24.0;
	private static final double UNSEEN_FAR = 48.0;

	private static Outcome unseen(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		BlockPos from = player.blockPosition();
		for (int attempt = 0; attempt < 96; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = UNSEEN_NEAR + random.nextDouble() * (UNSEEN_FAR - UNSEEN_NEAR);
			int x = from.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = from.getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, from.getY(), z))) {
				continue;
			}
			// Their own level first, then the surface. A player in a cellar gets
			// him in the cellar's rock rather than on the lawn above it, and a
			// player in a house gets him in the garden — but either will do, and
			// having two is what makes this refuse almost never.
			BlockPos at = standing(level, x, from.getY(), z);
			if (at == null) {
				at = standing(level, x, level.getHeight(
					Heightmap.Types.MOTION_BLOCKING, x, z), z);
			}
			if (at == null || at.closerThan(from, 8.0)) {
				continue;
			}
			return spawnAt(level, player, at, true);
		}
		HerobrineMod.LOGGER.warn("hunt: no footing anywhere around [{}, {}, {}]",
			from.getX(), from.getY(), from.getZ());
		return Outcome.NO_FOOTING;
	}

	/** The nearest place to this height with two blocks of air over solid ground. */
	private static @org.jspecify.annotations.Nullable BlockPos standing(
			ServerLevel level, int x, int y, int z) {
		for (int drop = 0; drop <= 12; drop++) {
			for (int dy : new int[] { -drop, drop }) {
				BlockPos at = new BlockPos(x, y + dy, z);
				if (level.isOutsideBuildHeight(at) || !ConfinedPlacement.canStand(level, at)) {
					continue;
				}
				return at;
			}
		}
		return null;
	}

	/** Puts him at a chosen spot, already facing the player. */
	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos) {
		return spawnAt(level, player, pos, false);
	}

	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos,
	                               boolean hunting) {
		// ONE OF HIM. See HerobrineEntity.outInTheOverworld.
		if (HerobrineEntity.outInTheOverworld(level)) {
			return Outcome.ALREADY_NEARBY;
		}
		HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (herobrine == null) {
			return Outcome.NO_FOOTING;
		}
		// Facing the player from the moment he exists. Turning to look at you
		// afterwards would give away that he had just arrived.
		double dx = player.getX() - (pos.getX() + 0.5);
		double dz = player.getZ() - (pos.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		herobrine.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		// Where the player is standing right now. Chase him and he comes back
		// to it, so the ground you gave up is the ground he takes.
		herobrine.setAnchor(player.blockPosition());
		if (hunting) {
			herobrine.beginProwl();
		}
		level.addFreshEntity(herobrine);
		// Say where he is, which the stare never did.
		//
		// Every other manifestation reports its position and this one did not,
		// so `/herobrine provoke` answered with the coordinates of whatever
		// sign or page had been left last — a real place, tens or hundreds of
		// blocks from him, with a plausible distance beside it. A tester
		// following that number is looking at empty ground and concluding he
		// never spawned, and there is nothing in the output to suggest
		// otherwise. Of all the ways to lose a week, being confidently told
		// the wrong coordinates is the worst.
		ManifestationDirector.noteLocation(pos);
		omen(level, pos);
		// A reason to turn around — sometimes. He is still never SEEN
		// arriving; you turn and find him already standing there.
		herobrine.announceArrival();
		return Outcome.PLACED;
	}

	/** True if the position falls inside the player's rough view cone. */
	/**
	 * Could the player see that spot if they turned round?
	 *
	 * Aimed a little above the block, because he stands ON it and it is his
	 * head and shoulders that have to clear the ridge, not his feet. Checking
	 * the ground itself would reject a perfectly good spot just over the brow
	 * of a hill — which is one of the better places for him to be.
	 */
	/**
	 * AND A WINDOW IS NOT A WALL.
	 *
	 * VISUAL rather than COLLIDER, and that one word is the whole of it. COLLIDER
	 * asks "would something bump into this", and glass would — it has a full
	 * collision box. So every sightline out of a window failed, and a player who
	 * had glazed their base had quietly made themselves unwatchable: he would
	 * refuse every position they could actually see him from and then report that
	 * nothing was visible.
	 *
	 * VISUAL asks the question that was meant all along, and it is vanilla's own
	 * answer to it — glass and panes and bars all return an empty visual shape,
	 * because that is the same test the game uses to decide whether you can see
	 * out of a block. Nothing here has to keep its own list of what counts as
	 * transparent, which is the kind of list that goes stale the moment somebody
	 * adds a new pane.
	 *
	 * Leaves, slabs and fences still block, and they should — a figure read
	 * through a hedge is not a sighting.
	 *
	 * Aimed a little above the block, because he stands ON it and it is his head
	 * and shoulders that have to clear the ridge, not his feet.
	 */
	public static boolean visibleFrom(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 head = new Vec3(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
		return level.clip(new net.minecraft.world.level.ClipContext(eye, head,
			net.minecraft.world.level.ClipContext.Block.VISUAL,
			net.minecraft.world.level.ClipContext.Fluid.NONE, player))
			.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	private static boolean isInFrontOf(ServerPlayer player, BlockPos pos) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) > IN_VIEW_DOT;
	}
}
