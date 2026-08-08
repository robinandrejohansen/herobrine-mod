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

	public static final AttachmentType<Boolean> THRESHOLD_RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_raised"), Codec.BOOL);

	public static final AttachmentType<Long> THRESHOLD_ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_origin"), Codec.LONG);

	/**
	 * Two, three and four — the middle of the story.
	 *
	 * The two ends were built first on purpose: the first house had to
	 * establish what a home of his looks like and the last had to establish
	 * where it was all going, and the middle is only legible once both of those
	 * exist. These are what the player finds between them, and each is defined
	 * by what the one before it still had and this one does not.
	 */
	private static final AttachmentType<Boolean>[] MIDDLE_RAISED = middleFlags();

	@SuppressWarnings("unchecked")
	private static AttachmentType<Boolean>[] middleFlags() {
		return new AttachmentType[] {
			AttachmentRegistry.createPersistent(HerobrineMod.id("house_two_raised"), Codec.BOOL),
			AttachmentRegistry.createPersistent(HerobrineMod.id("house_three_raised"), Codec.BOOL),
			AttachmentRegistry.createPersistent(HerobrineMod.id("house_four_raised"), Codec.BOOL),
		};
	}

	/**
	 * Where each of the middle three sits, and why the bands do not overlap.
	 *
	 * Distance is the ONLY thing telling a player these are a sequence — there
	 * is no map, no quest and no numbering anywhere in the world. So the bands
	 * are strictly ordered and they do not touch: whichever one you stumble on
	 * first, the next one out is always the next one along, and a player who
	 * walks outward is reading the story in order without ever being told there
	 * was an order.
	 */
	private static final int[][] MIDDLE_BANDS = {
		{ 1950, 2200 },
		{ 2250, 2450 },
		{ 2500, 2700 },
	};

	/**
	 * Eight bytes each, and not nine.
	 *
	 * These are spelled-out words in hex because a salt you can read is a salt
	 * you can tell apart at a glance — but a long is eight bytes, and the first
	 * attempt spelled longer words than that and would not compile.
	 */
	private static final long[] MIDDLE_SALTS = {
		0x486F757365325F5FL,   // House2__
		0x4469675F5F5F5F33L,   // Dig____3
		0x536872696E653401L,   // Shrine4
	};

	/** Far enough to be a journey, near enough to be reachable on foot. */
	private static final int MIN_RANGE = 1100;
	private static final int MAX_RANGE = 1900;

	/**
	 * The threshold sits further out than the homestead, and always further
	 * than it in the same world.
	 *
	 * Distance is the only thing telling the player these are a sequence. He
	 * did not move house to somewhere nicer; each one is further from anywhere
	 * anybody else would go, and the last is a long way past the first.
	 */
	private static final int THRESHOLD_MIN = 2600;
	private static final int THRESHOLD_MAX = 3600;
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
		if (!Boolean.TRUE.equals(overworld.getAttached(RAISED))) {
			BlockPos site = siteFor(overworld);
			for (ServerPlayer player : overworld.players()) {
				if (player.blockPosition().closerThan(site, RAISE_RANGE)) {
					raise(overworld, site);
					break;
				}
			}
		}
		if (!Boolean.TRUE.equals(overworld.getAttached(THRESHOLD_RAISED))) {
			BlockPos site = thresholdSiteFor(overworld);
			for (ServerPlayer player : overworld.players()) {
				if (player.blockPosition().closerThan(site, RAISE_RANGE)) {
					raiseThreshold(overworld, site);
					break;
				}
			}
		}

		for (int which = 0; which < MIDDLE_RAISED.length; which++) {
			if (Boolean.TRUE.equals(overworld.getAttached(MIDDLE_RAISED[which]))) {
				continue;
			}
			BlockPos site = middleSiteFor(overworld, which);
			for (ServerPlayer player : overworld.players()) {
				if (player.blockPosition().closerThan(site, RAISE_RANGE)) {
					raiseMiddle(overworld, site, which);
					break;
				}
			}
		}
	}

	/** Seeded like the other two, so it is in the same place in every copy. */
	public static BlockPos middleSiteFor(ServerLevel level, int which) {
		RandomSource random = RandomSource.create(level.getSeed() ^ MIDDLE_SALTS[which]);
		double angle = random.nextDouble() * Math.PI * 2.0;
		int[] band = MIDDLE_BANDS[which];
		double range = band[0] + random.nextDouble() * (band[1] - band[0]);
		BlockPos spawn = level.getLevelData().getRespawnData().pos();
		return new BlockPos(
			spawn.getX() + (int)Math.round(Math.cos(angle) * range), 0,
			spawn.getZ() + (int)Math.round(Math.sin(angle) * range));
	}

	/**
	 * @param which 0 the buried house, 1 the dig, 2 the shrine
	 */
	public static boolean raiseMiddle(ServerLevel level, BlockPos near, int which) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!buildable(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			RandomSource random = level.getRandom();
			switch (which) {
				case 0 -> SecondHouse.build(level, origin, random);
				case 1 -> TheDig.build(level, origin, random);
				default -> Shrine.build(level, origin, random);
			}
			level.getServer().overworld().setAttached(MIDDLE_RAISED[which], true);
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for house {} near [{}, {}]",
			which + 2, near.getX(), near.getZ());
		return false;
	}

	public static BlockPos thresholdSiteFor(ServerLevel level) {
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x546872657368L);
		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = THRESHOLD_MIN + random.nextDouble() * (THRESHOLD_MAX - THRESHOLD_MIN);
		BlockPos spawn = level.getLevelData().getRespawnData().pos();
		return new BlockPos(
			spawn.getX() + (int)(Math.cos(angle) * range),
			spawn.getY(),
			spawn.getZ() + (int)(Math.sin(angle) * range));
	}

	public static @org.jspecify.annotations.Nullable BlockPos thresholdOrigin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(THRESHOLD_ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * Raise the threshold near its site.
	 *
	 * Far less fussy about ground than the homestead, and deliberately so:
	 * almost nothing of it is above the surface, so a slope that would tilt a
	 * farmhouse does not matter to a stair mouth. All it needs is dry land.
	 */
	public static boolean raiseThreshold(ServerLevel level, BlockPos near) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			int ground = Ground.topOf(level, x, z);
			if (ground <= level.getSeaLevel()
				|| !level.getFluidState(new BlockPos(x, ground, z)).isEmpty()) {
				continue;
			}
			BlockPos origin = new BlockPos(x, ground, z);
			Threshold.raise(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(THRESHOLD_RAISED, true);
			overworld.setAttached(THRESHOLD_ORIGIN, origin.asLong());
			return true;
		}
		HerobrineMod.LOGGER.warn("no dry ground for the threshold near [{}, {}]",
			near.getX(), near.getZ());
		return false;
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
				// Real ground, not the canopy. Judging a forest site by the
				// heightmap made it look wildly uneven AND put the floor level
				// somewhere above the trees.
				int height = Ground.floorOver(level, x + dx, z + dz);
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
		// Two, not three. The footing is only three deep now, so a site that
		// varies more than this cannot be built on without a visible plinth —
		// better to walk on and find flatter ground.
		return high - low <= 2;
	}
}
