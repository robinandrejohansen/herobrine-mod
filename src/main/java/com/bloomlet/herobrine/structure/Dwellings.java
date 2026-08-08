package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Where he lived, and when it appears.
 *
 * The house has ONE position per world, fixed by the world seed, decided
 * before anybody goes looking. That matters more than it sounds: a house that
 * appeared near whoever happened to wander furthest would be a house that
 * follows the player, and players work that out immediately. This one has
 * always been there. Two people on a server walk to the same coordinates and
 * find the same building, and a player who reads the seed can find it in a
 * copy of the world — which is exactly the kind of consistency that makes a
 * place feel like part of the map rather than part of the mod.
 *
 * It is only BUILT when somebody gets close, because blocks cannot be placed
 * in chunks that are not loaded and forcing them open across a thousand blocks
 * to furnish a room nobody is in would be indefensible. The position is the
 * real thing; the blocks are just what happens when you arrive.
 *
 * Deliberately not gated on wrath. Everything else in this mod is paced, and
 * this is the one thing that is not: it does not wait for the player to earn
 * it and it does not care what phase they are in. If they walk far enough on
 * their first day, it is there on their first day. It was there before them.
 */
public final class Dwellings {
	private Dwellings() {}

	/** Set once the blocks exist, so it is never built twice. */
	public static final AttachmentType<Boolean> RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_raised"), Codec.BOOL);

	/** Where it went, once it went somewhere. */
	public static final AttachmentType<Long> ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_origin"), Codec.LONG);

	/** Far enough to be a journey, near enough to be reachable on foot. */
	private static final int MIN_RANGE = 1100;
	private static final int MAX_RANGE = 1900;
	/** Build when somebody is this close. Inside a default simulation radius. */
	private static final int RAISE_RANGE = 112;
	private static final int CHECK_INTERVAL = 40;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Dwellings::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		ServerLevel overworld = server.overworld();
		if (Boolean.TRUE.equals(overworld.getAttached(RAISED))) {
			return;
		}
		BlockPos site = siteFor(overworld);
		for (ServerPlayer player : overworld.players()) {
			if (player.blockPosition().closerThan(site, RAISE_RANGE)) {
				raise(overworld, site);
				return;
			}
		}
	}

	/**
	 * The seed decides. Nothing else gets a say.
	 *
	 * XORed with a constant of its own so this does not land on top of
	 * anything else that seeds itself from the world, and so a later structure
	 * can take a different constant and be somewhere else.
	 */
	public static BlockPos siteFor(ServerLevel level) {
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x486F6D6553746564L);
		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);
		// World spawn moved in 26.2: it is now respawn data on the level data
		// rather than a getter on the level.
		BlockPos spawn = level.getLevelData().getRespawnData().pos();
		return new BlockPos(
			spawn.getX() + (int)(Math.cos(angle) * range),
			spawn.getY(),
			spawn.getZ() + (int)(Math.sin(angle) * range));
	}

	/** Where it actually stands, once raised. */
	public static @org.jspecify.annotations.Nullable BlockPos origin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	public static boolean raised(ServerLevel level) {
		return Boolean.TRUE.equals(level.getServer().overworld().getAttached(RAISED));
	}

	/**
	 * Put it down, near the site, wherever the ground will take it.
	 *
	 * The seed picks the neighbourhood and the terrain picks the spot. Dropping
	 * it on the exact seeded block would put it in a lake or halfway up a cliff
	 * often enough to matter, and a house standing in water is not eerie, it is
	 * broken.
	 */
	public static boolean raise(ServerLevel level, BlockPos near) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!buildable(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Homestead.floorHeightAt(level, x, z), z);
			Homestead.build(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(RAISED, true);
			overworld.setAttached(ORIGIN, origin.asLong());
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for the homestead near [{}, {}]",
			near.getX(), near.getZ());
		return false;
	}

	/**
	 * Is there dry, loaded, roughly level ground here?
	 *
	 * Samples the corners and the middle rather than every column — the ground
	 * only has to be good enough that levelling it does not leave a four-block
	 * step of dirt down one side.
	 */
	private static boolean buildable(ServerLevel level, int x, int z) {
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		// Only the building has to be level. The yard follows the ground now,
		// so a site is judged on the ground under the HOUSE rather than on the
		// whole map — which was rejecting perfectly good spots because a
		// grave marker forty blocks away would have been on a hill.
		for (int dz = 2; dz <= 14; dz += 4) {
			for (int dx = 2; dx <= 18; dx += 4) {
				BlockPos column = new BlockPos(x + dx, 0, z + dz);
				if (!level.isLoaded(column.atY(level.getSeaLevel()))) {
					return false;
				}
				int height = level.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
				if (height <= level.getSeaLevel()) {
					return false;   // in the sea, or in a lake
				}
				if (!level.getFluidState(new BlockPos(x + dx, height - 1, z + dz)).isEmpty()) {
					return false;
				}
				low = Math.min(low, height);
				high = Math.max(high, height);
			}
		}
		return high - low <= 3;
	}
}
