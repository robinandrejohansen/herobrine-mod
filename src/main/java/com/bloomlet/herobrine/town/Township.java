package com.bloomlet.herobrine.town;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The town, and the reason it is where it is.
 *
 * A settlement that works the way a real one does, which mostly means putting
 * things where people would actually have put them. Water at the centre because
 * everybody needs it every day and a well is where you meet your neighbours.
 * The fields OUTSIDE the wall, because enclosing arable land wastes the
 * expensive thing to protect the cheap one. The smithy against the wall by the
 * gate, because it is the loudest, smelliest, most flammable building in the
 * place and nobody wants it next to their bed.
 *
 * None of that is decoration. A village laid out by need reads as inhabited
 * even before anybody is in it, and a village laid out by a grid reads as a
 * mod however much furniture is in the houses.
 *
 * THE WALL is the thing that says what kind of place this is. Vanilla villages
 * are open, and open is what people build when nothing is coming. A wall, a
 * single gate and a watch platform say that something is, and that these people
 * decided it was worth the stone — which is the story arriving before a word of
 * it is written down.
 *
 * Laid out from the terrain rather than from a fixed map. Lanes run from the
 * square outward and buildings stand along them facing the road, so the shape
 * of the town comes from the ground it is on. Two worlds never get the same
 * town, and no house is ever back-to-front to the street it is on.
 */
public final class Township {
	private Township() {}

	/**
	 * How far the wall stands from the middle.
	 *
	 * Forty, and it was arrived at by counting rather than by eye. Thirteen
	 * buildings — a church, a hall, two shops, a smithy, six houses and two
	 * pens — come to about 1,700 blocks of footprint. A wall at thirty
	 * encloses 2,400 usable blocks once the square and the lanes are taken out,
	 * which fits them only if they touch, and a town where the buildings touch
	 * is a terrace. Forty gives 4,400, which leaves room for gardens, gaps and
	 * the space between a house and its neighbour that makes it a house.
	 *
	 * Sized before the buildings rather than after, because a wall is the one
	 * thing here that cannot be adjusted later without moving everything.
	 */
	private static final int WALL_RADIUS = 40;
	/** The open ground at the centre, where the well is. */
	private static final int SQUARE = 7;
	private static final int WALL_HEIGHT = 4;

	public static void raise(ServerLevel level, BlockPos site, RandomSource random) {
		int base = Ground.topOf(level, site.getX(), site.getZ()) + 1;
		BlockPos centre = new BlockPos(site.getX(), base, site.getZ());

		// The gate faces the way the road comes in, and every lane is measured
		// from it, so the town has a front and a back like a real one.
		Direction approach = Direction.Plane.HORIZONTAL.getRandomDirection(random);

		List<Direction> lanes = new ArrayList<>();
		lanes.add(approach);
		lanes.add(approach.getClockWise());
		lanes.add(approach.getCounterClockWise());
		if (random.nextBoolean()) {
			lanes.add(approach.getOpposite());
		}

		square(level, centre, random);
		for (Direction lane : lanes) {
			lane(level, centre, lane, random);
		}
		wall(level, centre, approach, random);
		fields(level, centre, approach, random);

		List<Plot> plots = allot(level, centre, lanes, random);
		for (Plot plot : plots) {
			mark(level, plot, random);
		}

		HerobrineMod.LOGGER.info("township laid out at [{}, {}, {}], gate facing {}, {} plots",
			centre.getX(), centre.getY(), centre.getZ(), approach.getName(), plots.size());
	}

	/**
	 * A place a building will stand, and which way it will face.
	 *
	 * @param facing the way its front points, which is always at the lane
	 */
	public record Plot(BlockPos corner, int width, int depth, Direction facing, String kind) {}

	/**
	 * Hand out the ground.
	 *
	 * Buildings stand along the lanes rather than anywhere they fit, because
	 * that is how a settlement grows: somebody builds by the road, then
	 * somebody builds next to them. The big civic pieces take the plots nearest
	 * the square, since a church at the far end behind a barn is not how
	 * anybody has ever arranged a town.
	 *
	 * Both sides of every lane, alternating, so the road has two frontages and
	 * the town reads as a street rather than as a row.
	 */
	private static List<Plot> allot(ServerLevel level, BlockPos centre,
	                                List<Direction> lanes, RandomSource random) {
		String[] order = {
			"church", "hall", "shop", "house", "house", "shop",
			"house", "house", "smithy", "house", "house", "pen",
		};
		int[][] size = {
			{15, 25}, {15, 13}, {9, 9}, {11, 9}, {11, 9}, {9, 9},
			{11, 9}, {11, 9}, {11, 9}, {11, 9}, {11, 9}, {12, 12},
		};

		List<Plot> plots = new ArrayList<>();
		int taken = 0;
		int along = SQUARE + 4;

		while (taken < order.length && along < WALL_RADIUS - 14) {
			for (Direction lane : lanes) {
				for (int side = -1; side <= 1 && taken < order.length; side += 2) {
					int w = size[taken][0];
					int d = size[taken][1];
					Direction across = lane.getClockWise();

					// Set back from the road by three, so there is a doorstep
					// and a bit of garden rather than a wall on the verge.
					int offset = 3 + d / 2;
					int cx = centre.getX() + lane.getStepX() * (along + w / 2)
						+ across.getStepX() * side * offset;
					int cz = centre.getZ() + lane.getStepZ() * (along + w / 2)
						+ across.getStepZ() * side * offset;

					BlockPos corner = new BlockPos(cx - w / 2,
						Ground.topOf(level, cx, cz) + 1, cz - d / 2);
					// Facing the lane it stands on, which is the whole reason
					// plots carry a direction at all.
					Direction facing = side < 0 ? across : across.getOpposite();
					plots.add(new Plot(corner, w, d, facing, order[taken]));
					taken++;
				}
			}
			along += 18;
		}
		return plots;
	}

	/**
	 * Level the plot and lay its footprint, so the space is visible.
	 *
	 * Buildings come next; this is the ground they will stand on, cleared and
	 * paved to its outline. Walking a town of marked plots is the only way to
	 * find out whether thirteen buildings actually fit before building
	 * thirteen buildings.
	 */
	private static void mark(ServerLevel level, Plot plot, RandomSource random) {
		for (int dx = 0; dx < plot.width(); dx++) {
			for (int dz = 0; dz < plot.depth(); dz++) {
				BlockPos at = new BlockPos(plot.corner().getX() + dx,
					Ground.topOf(level, plot.corner().getX() + dx,
						plot.corner().getZ() + dz), plot.corner().getZ() + dz);
				boolean edge = dx == 0 || dz == 0
					|| dx == plot.width() - 1 || dz == plot.depth() - 1;
				fill(level, at, edge ? Blocks.COBBLESTONE.defaultBlockState()
					: paving(random));
				clearAbove(level, at.above(), 6);
			}
		}
	}

	/**
	 * The middle of everything, and the well in it.
	 *
	 * Paved, and the only levelled ground in the town — a market square is the
	 * one place a settlement really would have flattened, because you cannot
	 * set out a stall on a slope. Everywhere else keeps the shape of the hill.
	 */
	private static void square(ServerLevel level, BlockPos centre, RandomSource random) {
		for (int dx = -SQUARE; dx <= SQUARE; dx++) {
			for (int dz = -SQUARE; dz <= SQUARE; dz++) {
				if (dx * dx + dz * dz > SQUARE * SQUARE + 4) {
					continue;
				}
				BlockPos at = centre.offset(dx, 0, dz);
				clearAbove(level, at, 5);
				fill(level, at.below(), paving(random));
			}
		}

		// The well. Rim a block proud so nobody walks into it in the dark.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos rim = centre.offset(dx, 0, dz);
				if (dx == 0 && dz == 0) {
					fill(level, rim.below(), Blocks.WATER.defaultBlockState());
					fill(level, rim.below(2), Blocks.WATER.defaultBlockState());
					fill(level, rim.below(3), Blocks.COBBLESTONE.defaultBlockState());
					continue;
				}
				fill(level, rim, Blocks.COBBLESTONE_WALL.defaultBlockState());
			}
		}
		// Posts and a beam over it, because a well without a bucket is a hole.
		for (int side = -1; side <= 1; side += 2) {
			fill(level, centre.offset(side, 1, 0), Blocks.SPRUCE_FENCE.defaultBlockState());
			fill(level, centre.offset(side, 2, 0), Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		fill(level, centre.above(3), Blocks.SPRUCE_SLAB.defaultBlockState());
		for (int side = -1; side <= 1; side += 2) {
			fill(level, centre.offset(side, 3, 0), Blocks.SPRUCE_SLAB.defaultBlockState());
		}
		fill(level, centre.above(2), Blocks.IRON_CHAIN.defaultBlockState());

		// A lantern on a post at the well, which is where you would want one.
		BlockPos lamp = centre.offset(0, 0, SQUARE - 1);
		fill(level, lamp, Blocks.SPRUCE_FENCE.defaultBlockState());
		fill(level, lamp.above(), Blocks.SPRUCE_FENCE.defaultBlockState());
		fill(level, lamp.above(2), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, false));
	}

	/**
	 * A lane from the square to the wall.
	 *
	 * Follows the ground rather than cutting through it, and wanders a block
	 * either side as it goes. A dead straight road is surveyed, and nobody
	 * surveyed this — it is where people have walked for long enough that the
	 * grass gave up.
	 */
	private static void lane(ServerLevel level, BlockPos centre, Direction towards,
	                         RandomSource random) {
		int drift = 0;
		for (int step = SQUARE - 1; step <= WALL_RADIUS + 2; step++) {
			if (random.nextInt(5) == 0) {
				drift += random.nextBoolean() ? 1 : -1;
				drift = Math.max(-2, Math.min(2, drift));
			}
			int x = centre.getX() + towards.getStepX() * step + towards.getStepZ() * drift;
			int z = centre.getZ() + towards.getStepZ() * step + towards.getStepX() * drift;

			for (int across = -1; across <= 1; across++) {
				int lx = x + Math.abs(towards.getStepZ()) * across;
				int lz = z + Math.abs(towards.getStepX()) * across;
				int y = Ground.topOf(level, lx, lz);
				BlockPos on = new BlockPos(lx, y, lz);
				if (!level.getFluidState(on).isEmpty()) {
					continue;
				}
				fill(level, on, paving(random));
				clearAbove(level, on.above(), 3);
			}
			// A lamp post every so often, on one side.
			if (step % 9 == 0 && step > SQUARE + 2) {
				int px = x + Math.abs(towards.getStepZ()) * 2;
				int pz = z + Math.abs(towards.getStepX()) * 2;
				post(level, px, pz);
			}
		}
	}

	/**
	 * The wall, following the ground it stands on.
	 *
	 * Height is measured from the terrain at every column rather than from one
	 * level, so it climbs a rise instead of burying itself in it — which is
	 * both what a real wall does and the only way it does not look dropped in.
	 *
	 * One gate. Two would be sensible and one is frightening, and these people
	 * were not being sensible.
	 */
	private static void wall(ServerLevel level, BlockPos centre, Direction gate,
	                         RandomSource random) {
		for (int angle = 0; angle < 360; angle++) {
			double radians = Math.toRadians(angle);
			// Not a circle. A slight wobble in the radius makes it a thing that
			// was paced out rather than drawn with a compass.
			double radius = WALL_RADIUS + Math.sin(radians * 3.0) * 1.5;
			int x = centre.getX() + (int)Math.round(Math.cos(radians) * radius);
			int z = centre.getZ() + (int)Math.round(Math.sin(radians) * radius);

			double toGate = Math.toDegrees(Math.atan2(gate.getStepZ(), gate.getStepX()));
			double apart = Math.abs(((angle - toGate + 540) % 360) - 180);
			if (apart > 176) {
				continue;   // the gateway
			}

			int ground = Ground.topOf(level, x, z);
			for (int up = 1; up <= WALL_HEIGHT; up++) {
				BlockPos at = new BlockPos(x, ground + up, z);
				fill(level, at, up == WALL_HEIGHT && random.nextInt(4) == 0
					? Blocks.COBBLESTONE_WALL.defaultBlockState()
					: weathered(random));
			}
			// Fill down to the ground so it never floats over a dip.
			for (int down = 0; down < 6; down++) {
				BlockPos below = new BlockPos(x, ground - down, z);
				if (!level.getBlockState(below).isAir()) {
					break;
				}
				fill(level, below, weathered(random));
			}
		}
		gatehouse(level, centre, gate, random);
	}

	/** Two towers and a beam over the road. The one way in. */
	private static void gatehouse(ServerLevel level, BlockPos centre, Direction gate,
	                              RandomSource random) {
		Direction across = gate.getClockWise();
		for (int side = -1; side <= 1; side += 2) {
			int x = centre.getX() + gate.getStepX() * WALL_RADIUS + across.getStepX() * side * 3;
			int z = centre.getZ() + gate.getStepZ() * WALL_RADIUS + across.getStepZ() * side * 3;
			int ground = Ground.topOf(level, x, z);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					for (int up = 1; up <= WALL_HEIGHT + 2; up++) {
						fill(level, new BlockPos(x + dx, ground + up, z + dz), weathered(random));
					}
				}
			}
			// Hollow it out and put a light on top, so somebody stood here.
			for (int up = 1; up <= WALL_HEIGHT; up++) {
				fill(level, new BlockPos(x, ground + up, z), Blocks.AIR.defaultBlockState());
			}
			fill(level, new BlockPos(x, ground + WALL_HEIGHT + 3, z),
				Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, false));
		}
	}

	/**
	 * Fields and pens, outside the wall.
	 *
	 * Where they have to be. Arable land is the one thing a settlement has too
	 * much of to enclose, and putting the crops inside would mean a wall three
	 * times as long protecting mostly turnips. So the food is out in the open
	 * and the people are behind stone, which tells you what they were afraid
	 * of and what they were prepared to lose.
	 */
	private static void fields(ServerLevel level, BlockPos centre, Direction gate,
	                           RandomSource random) {
		Direction out = gate.getOpposite();
		int cx = centre.getX() + out.getStepX() * (WALL_RADIUS + 12);
		int cz = centre.getZ() + out.getStepZ() * (WALL_RADIUS + 12);

		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				if (random.nextInt(9) == 0) {
					continue;
				}
				int y = Ground.topOf(level, cx + dx, cz + dz);
				BlockPos soil = new BlockPos(cx + dx, y, cz + dz);
				if (!level.getBlockState(soil).is(Blocks.GRASS_BLOCK)
					&& !level.getBlockState(soil).is(Blocks.DIRT)) {
					continue;
				}
				clearAbove(level, soil.above(), 2);
				fill(level, soil, Blocks.FARMLAND.defaultBlockState());
				if (random.nextInt(3) > 0) {
					fill(level, soil.above(), Blocks.WHEAT.defaultBlockState()
						.setValue(BlockStateProperties.AGE_7, 4 + random.nextInt(4)));
				}
			}
		}
	}

	private static void post(ServerLevel level, int x, int z) {
		int y = Ground.topOf(level, x, z);
		for (int up = 1; up <= 3; up++) {
			fill(level, new BlockPos(x, y + up, z), Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		fill(level, new BlockPos(x, y + 4, z), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, false));
	}

	private static BlockState paving(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.DIRT_PATH.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.COARSE_DIRT.defaultBlockState();
		}
		return roll < 9 ? Blocks.GRAVEL.defaultBlockState()
			: Blocks.COBBLESTONE.defaultBlockState();
	}

	private static BlockState weathered(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.COBBLESTONE_STAIRS.defaultBlockState();
		}
		return Blocks.COBBLESTONE.defaultBlockState();
	}

	private static void clearAbove(ServerLevel level, BlockPos at, int height) {
		for (int up = 0; up < height; up++) {
			BlockPos pos = at.above(up);
			if (!level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	private static void fill(ServerLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, 2);
	}
}
