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
	 * At SIEGE he does not simply be there. The sky comes with him.
	 *
	 * Every appearance before this one is built on him never being seen to
	 * arrive — you look up and he is already standing there, and the whole
	 * effect depends on there having been no moment of arrival to point at.
	 * This is the phase that abandons it, and it should be the loudest possible
	 * abandonment: three bolts on the ground he is standing on, so the player
	 * does not find him, they are TOLD where he is from across the valley.
	 *
	 * Visual-only, like everything else that throws lightning here. It cannot
	 * burn anything down, which is the only reason it is allowed to happen in a
	 * player's field at all.
	 */
	private static void arrival(ServerLevel level, BlockPos pos) {
		if (Wrath.phase(level.getServer()) != Phase.SIEGE) {
			return;
		}
		RandomSource random = level.getRandom();
		for (int i = 0; i < 3; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = random.nextDouble() * 3.5;
			final double x = pos.getX() + 0.5 + Math.cos(angle) * range;
			final double z = pos.getZ() + 0.5 + Math.sin(angle) * range;
			Cadence.in(level.getServer(), i * (3 + random.nextInt(5)), () -> {
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

	// ---- DEBUG AID, DELETE WHEN DONE -------------------------------------
	// A visual-only bolt on every placement, so a tester can see where he
	// actually went instead of walking a bearing and hoping.
	//
	// setVisualOnly(true) gates BOTH the fire and the entity damage in
	// LightningBolt.tick, checked against 26.2 rather than assumed — this
	// cannot burn a forest down or hurt anybody, which is the only reason it
	// is safe to leave switched on while playing.
	//
	// To remove: delete this field, the strike() call in spawnAt, the method
	// itself, and the "mark" branch in HerobrineCommand. Nothing else refers
	// to any of it.
	private static boolean markSpawns = true;

	public static boolean toggleMark() {
		markSpawns = !markSpawns;
		return markSpawns;
	}

	private static void strike(ServerLevel level, BlockPos pos) {
		if (!markSpawns) {
			return;
		}
		net.minecraft.world.entity.LightningBolt bolt =
			net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
				.create(level, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}
		bolt.setVisualOnly(true);
		bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(bolt);
	}
	// ---- END DEBUG AID ---------------------------------------------------

	/** How the sweep is spread: every bearing, a few distances down each. */
	private static final int BEARINGS = 32;
	private static final int DISTANCES = 3;
	/** 0-15. 7 and below is "dark enough for monsters". */
	private static final int MAX_LIGHT = 7;
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
	private static boolean existsAnywhere(ServerLevel level) {
		for (ServerLevel other : level.getServer().getAllLevels()) {
			if (!other.getEntities(ModEntities.HEROBRINE, e -> true).isEmpty()) {
				return true;
			}
		}
		return false;
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

		if (existsAnywhere(level)) {
			return Outcome.ALREADY_NEARBY;
		}

		// Underground the ring-and-drop strategy below is nonsense — it would
		// place him on the terrain above, through the rock. Follow the space
		// the player is actually in instead.
		if (ConfinedPlacement.isConfined(level, player)) {
			BlockPos spot = ConfinedPlacement.find(level, player);
			if (spot == null) {
				return Outcome.NO_ROOM_HERE;
			}
			if (!ignoreLight && level.getMaxLocalRawBrightness(spot) > MAX_LIGHT) {
				return Outcome.TOO_BRIGHT;
			}
			return spawnAt(level, player, spot, hunting);
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

				if (!ignoreLight && level.getMaxLocalRawBrightness(pos) > MAX_LIGHT) {
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

	/** Puts him at a chosen spot, already facing the player. */
	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos) {
		return spawnAt(level, player, pos, false);
	}

	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos,
	                               boolean hunting) {
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
			herobrine.beginHunt();
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
		arrival(level, pos);
		strike(level, pos);   // DEBUG AID — see markSpawns above
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
	private static boolean visibleFrom(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 head = new Vec3(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
		return level.clip(new net.minecraft.world.level.ClipContext(eye, head,
			net.minecraft.world.level.ClipContext.Block.COLLIDER,
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
