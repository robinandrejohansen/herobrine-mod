package com.bloomlet.herobrine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
	/** No second spawn within this range of the player. */
	private static final double SOLITUDE_RADIUS = 96.0;
	/** Above this, the position is in front of the player and unusable. */
	private static final double IN_VIEW_DOT = 0.25;

	/**
	 * Places him behind the player, if the world allows it.
	 *
	 * @return false when it could not happen — too bright, nowhere to stand, or
	 *         one of him is already nearby. The director treats that as a quiet
	 *         night rather than retrying.
	 */
	public static boolean spawnBehind(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return false;
		}

		AABB nearby = player.getBoundingBox().inflate(SOLITUDE_RADIUS);
		if (!level.getEntitiesOfClass(HerobrineEntity.class, nearby).isEmpty()) {
			return false;
		}

		RandomSource random = level.getRandom();
		// Several attempts, because most candidate rings will be too bright or
		// in front of the player. Failing quietly is correct — he simply does
		// not appear this time.
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
			int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);

			if (level.getMaxLocalRawBrightness(pos) > MAX_LIGHT) {
				continue;
			}
			if (isInFrontOf(player, pos)) {
				continue;
			}

			HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
			if (herobrine == null) {
				return false;
			}
			// Face him at the player from the moment he exists. Turning to
			// look at you afterwards would give away that he just arrived.
			double dx = player.getX() - (x + 0.5);
			double dz = player.getZ() - (z + 0.5);
			float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
			herobrine.snapTo(x + 0.5, y, z + 0.5, yaw, 0.0F);
			level.addFreshEntity(herobrine);
			return true;
		}
		return false;
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
