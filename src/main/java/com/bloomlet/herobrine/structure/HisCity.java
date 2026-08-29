package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Cadence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * THE CITY UNDER THE CASTLE, AND NOBODY IS IN IT.
 *
 * Fallen Kingdom opens on a king walking down out of his castle into the village
 * he rules, past people talking in the street. This is that shot with the people
 * taken out of it — the same streets, the same houses, the same lamps still
 * burning, and no one at all. Which is a far worse thing to walk into than a
 * ruin, because a ruin has already finished happening.
 *
 * SO NOTHING HERE IS BROKEN. No collapsed roofs, no rubble, no scorch marks. The
 * doors are shut, the lamps are lit and the market stalls are standing. Every
 * instinct on a build like this is to smash it up to prove something happened,
 * and smashing it up is what would let the player put it down as a battle they
 * missed. An intact empty town has no explanation available at all.
 *
 * IT GROWS INTO THE WOOD RATHER THAN CLEARING IT. Only the streets and the house
 * plots are cut; the trees between them are left exactly where the dark forest
 * put them. That is the "fits its environment" test and it is the same one a
 * nether fortress passes — the corridors belong to the place they are in, rather
 * than sitting in a clearing that announces a structure. It also means no two
 * of these look alike, because the terrain decides which plots survive.
 *
 * BUILT IN BATCHES, one house per tick. A town of this size placed in a single
 * tick is a visible server stall, and it goes up at a hundred and forty-four
 * blocks where nobody can see the staging.
 */
public final class HisCity {
	private HisCity() {}

	/** How far the town reaches past the castle wall. */
	static final int REACH = 58;
	/** And how far back from the curtain the first houses stand. */
	private static final int CLEAR_OF_CASTLE = 8;

	/** Four ways in and out, matching the castle's own gate on the south. */
	private static final int STREET_HALF = 2;

	/**
	 * Raise it around the castle.
	 *
	 * @param base   the castle floor, on top of the motte
	 * @param castle the curtain's half-width
	 * @param motte  how far the castle stands above the town
	 */
	public static void raise(ServerLevel his, BlockPos base, int castle, int motte,
	                         RandomSource random) {
		MinecraftServer server = his.getServer();
		BlockPos ground = base.below(motte);

		Cadence.in(server, 0, () -> streets(his, ground, castle, random));
		Cadence.in(server, 4, () -> square(his, ground, castle, random));

		// The plots, laid on a rough grid and then filtered by what the forest
		// will allow. One a tick.
		int tick = 8;
		for (int ring = CLEAR_OF_CASTLE; ring < REACH; ring += 13) {
			for (double turn = 0; turn < Math.PI * 2.0; turn += 0.42) {
				int hx = ground.getX() + (int)Math.round(Math.cos(turn) * (castle + ring));
				int hz = ground.getZ() + (int)Math.round(Math.sin(turn) * (castle + ring));
				final BlockPos plot = new BlockPos(hx, 0, hz);
				final long seed = random.nextLong();
				Cadence.in(server, tick++, () -> house(his, plot, seed));
			}
		}
		Cadence.in(server, tick + 2, () -> rampart(his, ground, castle, random));
		HerobrineMod.LOGGER.info("his city is going up around [{}, {}]",
			ground.getX(), ground.getZ());
	}

	/**
	 * Four streets out from the castle gate, following the ground.
	 *
	 * Following it rather than levelling it, which is the whole difference
	 * between a road and a runway. A dark forest is lumpy; a street that rises
	 * and falls with it reads as something people wore into the ground, and a
	 * dead-flat one reads as something a generator laid.
	 */
	private static void streets(ServerLevel his, BlockPos ground, int castle,
	                            RandomSource random) {
		for (Direction way : Direction.Plane.HORIZONTAL) {
			for (int out = 2; out < castle + REACH; out++) {
				for (int across = -STREET_HALF; across <= STREET_HALF; across++) {
					int x = ground.getX() + way.getStepX() * out + way.getStepZ() * across;
					int z = ground.getZ() + way.getStepZ() * out + way.getStepX() * across;
					if (!his.isLoaded(new BlockPos(x, ground.getY(), z))
						|| !Ground.dry(his, x, z)) {
						// A street does not cross a lake, and paving the bed of
						// one lays cobbles under thirty blocks of water. The gap
						// is correct: the road stops at the water, which is what
						// roads do.
						continue;
					}
					int y = Ground.topOf(his, x, z);
					put(his, new BlockPos(x, y, z), cobbles(random));
					for (int dy = 1; dy <= 3; dy++) {
						clear(his, new BlockPos(x, y + dy, z));
					}
				}
			}
		}
	}

	/** The market, at the foot of the castle steps. Stalls, no stallholders. */
	private static void square(ServerLevel his, BlockPos ground, int castle,
	                           RandomSource random) {
		BlockPos middle = ground.offset(0, 0, castle + 14);
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				int x = middle.getX() + dx;
				int z = middle.getZ() + dz;
				if (!his.isLoaded(new BlockPos(x, ground.getY(), z))
					|| !Ground.dry(his, x, z)) {
					continue;
				}
				int y = Ground.topOf(his, x, z);
				put(his, new BlockPos(x, y, z), cobbles(random));
				for (int dy = 1; dy <= 4; dy++) {
					clear(his, new BlockPos(x, y + dy, z));
				}
			}
		}
		// Four stalls, and a well. The stalls are what say a market rather than
		// a paved space; the well is what says people lived here.
		for (int i = 0; i < 4; i++) {
			double turn = i * Math.PI / 2.0 + 0.4;
			int x = middle.getX() + (int)Math.round(Math.cos(turn) * 6);
			int z = middle.getZ() + (int)Math.round(Math.sin(turn) * 6);
			if (!his.isLoaded(new BlockPos(x, ground.getY(), z))
				|| !Ground.dry(his, x, z)) {
				continue;
			}
			int y = Ground.topOf(his, x, z) + 1;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					put(his, new BlockPos(x + dx, y + 2, z + dz),
						Blocks.DARK_OAK_SLAB.defaultBlockState());
				}
			}
			for (int dx : new int[] { -1, 1 }) {
				for (int dz : new int[] { -1, 1 }) {
					for (int dy = 0; dy < 2; dy++) {
						put(his, new BlockPos(x + dx, y + dy, z + dz),
							Blocks.DARK_OAK_FENCE.defaultBlockState());
					}
				}
			}
			put(his, new BlockPos(x, y, z), Blocks.BARREL.defaultBlockState());
		}
	}

	/**
	 * ONE HOUSE, AND IT IS NOT THIS CLASS'S JOB ANY MORE.
	 *
	 * What stood here was a seven by seven box with a lid of dark oak slabs, and
	 * thirty of them in a street read as a texture. The building moved out to
	 * Cottage — pitched roof, brick chimney, four stones mixed damp-to-dry,
	 * shutters, a bed, a chest, and a shovelled path up to the door.
	 *
	 * This keeps what it was always actually for: deciding WHERE, and which way
	 * round. The facing is the bearing back to the castle, so every front door in
	 * the town looks the same way and the street reads as a street.
	 */
	private static void house(ServerLevel his, BlockPos plot, long seed) {
		// THE MIDDLE OF THE TOWN, NOT THE CASTLE.
		//
		// Every front door used to face the keep, which was correct while the town
		// was a ring around it — the castle WAS the middle. It stands a couple of
		// hundred blocks away now, so facing it would turn every street in the place
		// the same way and point the whole town off at nothing.
		BlockPos castle = com.bloomlet.herobrine.structure.Keep.city(his);
		Direction facing = Direction.NORTH;
		if (castle != null) {
			int dx = castle.getX() - plot.getX();
			int dz = castle.getZ() - plot.getZ();
			facing = Math.abs(dx) > Math.abs(dz)
				? (dx > 0 ? Direction.EAST : Direction.WEST)
				: (dz > 0 ? Direction.SOUTH : Direction.NORTH);
		}
		Cottage.raise(his, plot, facing, seed);
	}

	/**
	 * The town wall, and it is lower and rougher than the castle's.
	 *
	 * Built by different hands and to a different standard, which is the thing
	 * it is for: the castle is dressed deepslate laid true, and this is cobbles
	 * heaped to five blocks with gaps where the ground beat it. Two walls that
	 * do not match say two efforts, decades apart, far better than any sign.
	 */
	private static void rampart(ServerLevel his, BlockPos ground, int castle,
	                            RandomSource random) {
		int r = castle + REACH;
		for (int step = 0; step < 360; step++) {
			double turn = Math.toRadians(step);
			int x = ground.getX() + (int)Math.round(Math.cos(turn) * r);
			int z = ground.getZ() + (int)Math.round(Math.sin(turn) * r);
			if (!his.isLoaded(new BlockPos(x, ground.getY(), z))) {
				continue;
			}
			// The four street mouths are left open. A walled town with no way in
			// is a wall around a town rather than a town with a wall.
			if (Math.abs(x - ground.getX()) < 4 || Math.abs(z - ground.getZ()) < 4) {
				continue;
			}
			if (!Ground.dry(his, x, z)) {
				continue;   // the wall stops at the water, as a real one would
			}
			int y = Ground.topOf(his, x, z) + 1;
			int height = 4 + random.nextInt(2);
			for (int dy = 0; dy < height; dy++) {
				put(his, new BlockPos(x, y + dy, z), random.nextInt(9) == 0
					? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
					: Blocks.COBBLED_DEEPSLATE.defaultBlockState());
			}
			for (int dy = -1; dy >= -3; dy--) {
				fill(his, new BlockPos(x, y + dy, z),
					Blocks.COBBLED_DEEPSLATE.defaultBlockState());
			}
		}
	}

	// ---- THE WORKSHOP ------------------------------------------------------
	private static BlockState cobbles(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.DEEPSLATE_TILES.defaultBlockState();
		}
		return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	}

	private static void put(ServerLevel his, BlockPos at, BlockState state) {
		if (!his.getBlockState(at).equals(state)) {
			his.setBlock(at, state, 2);
		}
	}

	private static void clear(ServerLevel his, BlockPos at) {
		if (!his.getBlockState(at).isAir()) {
			his.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void fill(ServerLevel his, BlockPos at, BlockState state) {
		if (!his.getBlockState(at).isSolid()) {
			his.setBlock(at, state, 2);
		}
	}
}
