package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.ManifestationDirector;

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
			return spawnAt(level, player, spot);
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
		int hidden = 0;
		int noFooting = 0;

				// Forty rather than twenty. Two real filters were added above and each
		// one legitimately throws away candidates, so the old budget ran out
		// before it had found the clearing it was looking for.
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
			int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);

			// GROUND THAT WOULD ACTUALLY HOLD HIM, and this is where he was
			// standing on the sea.
			//
			// Every Minecraft heightmap counts fluid as surface — the real
			// predicate is `blocksMotion() || !getFluidState().isEmpty()` — so
			// over an ocean this returns the top of the water, and nothing
			// downstream ever asked whether that was a floor. He was placed on
			// the waves, and because open water has no trees, no ridge and no
			// canopy, that was ALSO the only place the sightline test could
			// pass. So the one bug produced the other: the sea was not merely
			// where he was visible, it was very nearly the only place he was
			// ever successfully put.
			if (!ConfinedPlacement.canStand(level, pos)) {
				noFooting++;
				continue;
			}
			// And he stands in the open.
			//
			// A spot under a forest canopy satisfies every other rule and is
			// unlookable-at from fifty blocks, because leaves are colliders and
			// the ray dies in them. Requiring sky above him costs nothing, puts
			// him on ridges and in clearings and at treelines where the whole
			// image belongs, and stops the placement loop burning all twenty
			// attempts inside a wood and reporting NOTHING_VISIBLE.
			if (!level.canSeeSky(pos)) {
				hidden++;
				continue;
			}

			if (!ignoreLight && level.getMaxLocalRawBrightness(pos) > MAX_LIGHT) {
				tooBright++;
				continue;
			}
			if (isInFrontOf(player, pos)) {
				inFront++;
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
				hidden++;
				continue;
			}

			Outcome result = spawnAt(level, player, pos);
			if (result == Outcome.PLACED) {
				return result;
			}
			noFooting++;
			HerobrineMod.LOGGER.info("stare: could not create at [{}, {}, {}]",
				pos.getX(), pos.getY(), pos.getZ());
		}
		HerobrineMod.LOGGER.info(
			"stare refused after 40 tries: {} lit, {} in front, {} unseeable, {} no footing",
			tooBright, inFront, hidden, noFooting);
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
