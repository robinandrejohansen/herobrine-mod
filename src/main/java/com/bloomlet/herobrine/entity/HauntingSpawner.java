package com.bloomlet.herobrine.entity;

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

	private static final double MIN_RADIUS = 26.0;
	private static final double MAX_RADIUS = 44.0;
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
		int noFooting = 0;

		for (int attempt = 0; attempt < 20; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
			int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);

			if (!ignoreLight && level.getMaxLocalRawBrightness(pos) > MAX_LIGHT) {
				tooBright++;
				continue;
			}
			if (isInFrontOf(player, pos)) {
				inFront++;
				continue;
			}

			Outcome result = spawnAt(level, player, pos);
			if (result == Outcome.PLACED) {
				return result;
			}
			noFooting++;
		}
		if (tooBright >= inFront && tooBright >= noFooting) {
			return Outcome.TOO_BRIGHT;
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
		// A reason to turn around — sometimes. He is still never SEEN
		// arriving; you turn and find him already standing there.
		herobrine.announceArrival();
		return Outcome.PLACED;
	}

	/** True if the position falls inside the player's rough view cone. */
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
