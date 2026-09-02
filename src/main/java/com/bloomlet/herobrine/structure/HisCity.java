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
	/**
	 * How far out the first ring of houses stands, and how far apart the rings are.
	 *
	 * EIGHT AND THIRTEEN GAVE RINGS AT 8, 21, 34 AND 47 — and the first two ran
	 * straight over the market. Skipping those plots was the first fix and it was
	 * the wrong one: nineteen of sixty went, which is half the city, and the ask was
	 * explicitly for a space in the middle WITHOUT losing the houses round it.
	 *
	 * Twenty-one and twelve gives 21, 33, 45 and 57. Four rings, fifteen plots
	 * each, SIXTY PLOTS — the same sixty — and the innermost clears the square's
	 * corner at 18.4 with two and a half blocks to spare. The city did not get
	 * smaller, it got a hole in the middle.
	 */
	private static final int CLEAR_OF_CASTLE = 21;
	private static final int RING_STEP = 12;

	/** Four ways in and out, matching the castle's own gate on the south. */
	private static final int STREET_HALF = 2;

	/**
	 * THE SQUARE, AND IT IS RESERVED BEFORE ANYTHING IS BUILT ON IT.
	 *
	 * There has been a square in here all along — nineteen across, four stalls and
	 * a well — and the plot rings started eight blocks from the middle and ran
	 * outward every thirteen. So the ring at eight and the ring at twenty-one both
	 * put houses ON it. Whichever ran last won, which is a market with a cottage
	 * standing in the middle of it.
	 *
	 * Asked for a space in the middle WITHOUT losing the houses round it, and that
	 * is the distinction: the fix is not a smaller square or a wider first ring, it
	 * is the plot loop knowing the square is there. A plot inside KEPT_CLEAR is
	 * skipped and the next one on the ring still gets built, so the city loses a
	 * couple of houses out of sixty rather than a whole ring.
	 *
	 * Twenty-seven across rather than nineteen. Oakhold's market is about a quarter
	 * of its walled area; this is a tenth, which is as far as it goes before the
	 * plots have nowhere left.
	 */
	private static final int SQUARE_HALF = 13;

	/**
	 * Nothing is built inside this. The square's corner is SQUARE_HALF * sqrt(2)
	 * out — 18.4 — so nineteen is the smallest number that covers the whole of it.
	 */
	private static final int KEPT_CLEAR = 19;

	/**
	 * AND A QUADRANT LEFT TO THE FOREST.
	 *
	 * Oakhold gives a whole corner of the inside of its wall to a wood and a
	 * cemetery, and that is most of why it reads as a place rather than as a base:
	 * a city that is buildings to the wall on every side is a compound.
	 *
	 * Free here, and better than free. house() already refuses a plot the dark
	 * forest will not allow, so LEAVING a sector means not calling it — the trees
	 * that are standing there are the ones the world generator put there, which is
	 * the one kind of green no builder can fake.
	 */
	private static final double GROVE_FROM = 3.6;
	private static final double GROVE_TO = 5.1;

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
		int skipped = 0;
		int wooded = 0;
		for (int ring = CLEAR_OF_CASTLE; ring < REACH; ring += RING_STEP) {
			for (double turn = 0; turn < Math.PI * 2.0; turn += 0.42) {
				int hx = ground.getX() + (int)Math.round(Math.cos(turn) * (castle + ring));
				int hz = ground.getZ() + (int)Math.round(Math.sin(turn) * (castle + ring));
				// A BACKSTOP, NOT THE MECHANISM. CLEAR_OF_CASTLE is what keeps the
				// rings off the market; this only catches a plot that has been
				// nudged in by a future change to the spacing, and it is RADIAL
				// rather than square-on because a box test against a circular ring
				// fails on the diagonals — at radius 20 a plot at 45 degrees sits
				// at (14, 14) and a Chebyshev test throws it away.
				if (Math.hypot(hx - ground.getX(), hz - ground.getZ()) < KEPT_CLEAR) {
					skipped++;
					continue;
				}
				// And out of the wood. Not cleared and not replanted — simply not
				// built on, so what stands there is whatever the generator grew.
				if (turn >= GROVE_FROM && turn <= GROVE_TO) {
					wooded++;
					continue;
				}
				final BlockPos plot = new BlockPos(hx, 0, hz);
				final long seed = random.nextLong();
				Cadence.in(server, tick++, () -> house(his, plot, seed));
			}
		}
		Cadence.in(server, tick + 2, () -> rampart(his, ground, castle, random));
		Cadence.in(server, tick + 4, () -> boneyard(his, ground, random));
		HerobrineMod.LOGGER.info(
			"his city is going up around [{}, {}] — {} plots off the square,"
				+ " {} left to the wood",
			ground.getX(), ground.getZ(), skipped, wooded);
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
	/**
	 * THE CEMETERY, IN THE WOOD NOBODY BUILT ON.
	 *
	 * The other half of Oakhold's green corner. A sector was left standing at
	 * GROVE_FROM..GROVE_TO and this is what is in it — which matters, because an
	 * empty quadrant inside a wall is not a park, it is a plot the generator
	 * failed on. Twenty graves under the trees says somebody chose to leave this
	 * ground alone, and says why.
	 *
	 * Under the canopy rather than in a clearing. A cemetery you find by walking
	 * into it is worth four of one you can see across the square.
	 */
	private static void boneyard(ServerLevel his, BlockPos ground,
	                             RandomSource random) {
		double mid = (GROVE_FROM + GROVE_TO) / 2.0;
		int laid = 0;
		for (int i = 0; i < 20; i++) {
			double turn = mid + (random.nextDouble() - 0.5) * (GROVE_TO - GROVE_FROM);
			double out = 16 + random.nextDouble() * (REACH - 22);
			int x = ground.getX() + (int) Math.round(Math.cos(turn) * out);
			int z = ground.getZ() + (int) Math.round(Math.sin(turn) * out);
			if (!his.isLoaded(new BlockPos(x, ground.getY(), z))
				|| !Ground.dry(his, x, z)) {
				continue;
			}
			int y = Ground.topOf(his, x, z);
			// A mound and a marker. Podzol reads as turned earth under a dark
			// forest canopy where dirt just reads as dirt.
			for (int dz = 0; dz <= 1; dz++) {
				put(his, new BlockPos(x, y, z + dz), Blocks.PODZOL.defaultBlockState());
			}
			BlockPos head = new BlockPos(x, y + 1, z - 1);
			if (his.getBlockState(head).isAir()) {
				put(his, head, random.nextInt(4) == 0
					? Blocks.COBBLESTONE_WALL.defaultBlockState()
					: Blocks.STONE_BRICK_WALL.defaultBlockState());
			}
			laid++;
		}
		HerobrineMod.LOGGER.info("{} graves under the trees inside his wall", laid);
	}

	private static void square(ServerLevel his, BlockPos ground, int castle,
	                           RandomSource random) {
		// THE MIDDLE, and it used to be fourteen blocks off it.
		//
		// ground.offset(0, 0, castle + 14) was right when there was a castle here
		// to stand in front of. Keep calls this with castle = 0 now — the castle is
		// its own place, two hundred blocks out — so the offset was just putting the
		// market fourteen blocks north of the crossroads for no reason, which is
		// also what put it under the plot rings.
		BlockPos middle = ground;
		for (int dx = -SQUARE_HALF; dx <= SQUARE_HALF; dx++) {
			for (int dz = -SQUARE_HALF; dz <= SQUARE_HALF; dz++) {
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
