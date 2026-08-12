package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * THE THIRD WAY INTO THE UNDERCITY, AND THE ONLY ONE THAT IS EARNED.
 *
 * There were already two: the dry stair behind the altar and the swim down the
 * well. Both are secrets, and both are simply found. This one is a hundred and
 * forty blocks of gauntlet, and it exists because a cult that is STILL MEETING
 * needs a door that the congregation can use and a stranger cannot.
 *
 * WHY A TRAP CORRIDOR IS USUALLY BAD, AND WHAT MAKES THIS ONE NOT.
 *
 * The failure mode of every parkour trap in every mod is the same: the player
 * dies to something they could not have seen, loses an inventory in lava, and
 * never comes back. That is not difficulty, it is a tax on curiosity, and it
 * would undo the one thing this whole mod is trying to buy — a group willing to
 * go deeper. So four rules, and they are not negotiable:
 *
 *   1. TEACH BEFORE YOU TEST. The first chamber contains one of every hazard in
 *      the corridor, defanged: a gap you cannot fall into, a plate that fires
 *      into a wall, a false floor over a metre of water. The player learns every
 *      tell in the place where learning it is free.
 *
 *   2. A LANDING BETWEEN EVERY SEGMENT. Failure costs one segment, never the
 *      whole run. A corridor you must do perfectly is a corridor nobody finishes.
 *
 *   3. WATER CATCHES THE FALLS; LAVA ONLY THREATENS. The lava is real, visible
 *      and lights the place — and it is beside the path rather than under it. To
 *      lose an inventory here you have to jump sideways on purpose. Anybody who
 *      simply misses a jump lands in water, climbs out, and swears.
 *
 *   4. NOTHING IS BLIND. Every gap is visible from the ledge before it. Every
 *      plate is a different block from the floor around it. The tension is
 *      entirely in execution, never in ignorance.
 *
 * That is what "Indiana Jones" actually means mechanically — not that it is
 * unfair, but that the room tells you what it is about to do and does it anyway.
 */
public final class Trial {
	private Trial() {}

	/** Long enough to be a journey in its own right. */
	private static final int SEGMENT = 20;
	private static final int SEGMENTS = 7;

	/**
	 * What each stretch is about, in order, and each adds exactly one thing.
	 *
	 * LESSON is free. GAPS is jumping. PLATES is jumping while reading the floor.
	 * PISTONS adds something that moves. DARK removes the light. FALSE adds a
	 * floor that is not one. And the last is the door, which is a reward and not
	 * a test — ending on a hazard would mean the last thing the corridor does is
	 * kill somebody who had already won.
	 */
	private enum Leg { LESSON, GAPS, PLATES, PISTONS, DARK, FALSE_FLOOR, DOOR }

	/**
	 * Cut the whole thing, running outward from the undercity.
	 *
	 * Built from the inside out so the far end is the SURFACE end: the corridor
	 * is authored in the direction the builders would have cut it, and walked in
	 * the opposite direction by anybody who finds the shaft.
	 *
	 * @return the surface end, where the entrance shaft comes up
	 */
	public static BlockPos cut(ServerLevel level, BlockPos from, Direction heading,
	                           RandomSource random) {
		BlockPos at = from;
		for (int leg = 0; leg < SEGMENTS; leg++) {
			Leg kind = Leg.values()[leg];
			hollow(level, at, heading, kind);
			switch (kind) {
				case LESSON -> lesson(level, at, heading);
				case GAPS -> gaps(level, at, heading, random, false);
				case PLATES -> plates(level, at, heading, random);
				case PISTONS -> pistons(level, at, heading, random);
				case DARK -> gaps(level, at, heading, random, true);
				case FALSE_FLOOR -> falseFloor(level, at, heading, random);
				case DOOR -> door(level, at, heading);
			}
			landing(level, at.relative(heading, SEGMENT - 1), heading, kind);
			at = at.relative(heading, SEGMENT);
		}
		HerobrineMod.LOGGER.info("a trial cut {} blocks {} from [{}, {}, {}]",
			SEGMENTS * SEGMENT, heading.getName(), from.getX(), from.getY(), from.getZ());
		return at;
	}

	/**
	 * The void the corridor lives in.
	 *
	 * Five wide and six high, which is bigger than it needs to be for walking and
	 * exactly what it needs to be for jumping: a three-block gap in a two-wide
	 * corridor is a corridor, and the same gap with room either side is a
	 * decision about which line to take.
	 *
	 * The outer two columns of the floor are the lava channel — beside the path,
	 * never under it, per rule three.
	 */
	private static void hollow(ServerLevel level, BlockPos start, Direction heading,
	                          Leg kind) {
		Direction across = heading.getClockWise();
		for (int step = 0; step < SEGMENT; step++) {
			BlockPos spine = start.relative(heading, step);
			for (int side = -2; side <= 2; side++) {
				BlockPos column = spine.relative(across, side);
				for (int up = 0; up <= 5; up++) {
					level.setBlock(column.above(up), Blocks.AIR.defaultBlockState(), 2);
				}
				// Walls, ceiling and the bed the lava sits in.
				level.setBlock(column.above(6), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				level.setBlock(column.below(2), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				boolean edge = side == -2 || side == 2;
				level.setBlock(column.below(),
					edge ? Blocks.LAVA.defaultBlockState()
						: Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
				if (edge) {
					// A lip, so the lava cannot run into the walking lane.
					level.setBlock(column.relative(across, side < 0 ? -1 : 1),
						Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
			// The walking floor, which the hazard methods then cut holes in.
			for (int side = -1; side <= 1; side++) {
				level.setBlock(spine.relative(across, side),
					Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
			}
			if (kind != Leg.DARK && step % 6 == 3) {
				level.setBlock(spine.relative(across, 2).above(2),
					Blocks.WALL_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING,
							across.getOpposite()), 2);
			}
		}
	}

	/**
	 * THE ROOM THAT TEACHES, and it is the most important twenty blocks here.
	 *
	 * One of everything, made harmless. A gap with water under it instead of a
	 * drop. A pressure plate whose dispenser points into the wall. A gravel patch
	 * over one block of water. A player crosses this in fifteen seconds, learns
	 * that gravel means a hole and that a smooth plate means arrows, and carries
	 * both facts into the eighty blocks where they are true.
	 */
	private static void lesson(ServerLevel level, BlockPos start, Direction heading) {
		Direction across = heading.getClockWise();
		// A gap, over water.
		for (int step = 5; step <= 6; step++) {
			BlockPos spine = start.relative(heading, step);
			for (int side = -1; side <= 1; side++) {
				level.setBlock(spine.relative(across, side), Blocks.AIR.defaultBlockState(), 2);
				level.setBlock(spine.relative(across, side).below(),
					Blocks.WATER.defaultBlockState(), 2);
			}
		}
		// A plate, wired to a dispenser that fires into the wall.
		BlockPos taught = start.relative(heading, 11);
		level.setBlock(taught.below(), Blocks.DISPENSER.defaultBlockState()
			.setValue(BlockStateProperties.FACING, across), 2);
		arm(level, taught.below());
		level.setBlock(taught, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
		// And gravel, over one block of water.
		BlockPos soft = start.relative(heading, 16);
		level.setBlock(soft, Blocks.GRAVEL.defaultBlockState(), 2);
		level.setBlock(soft.below(), Blocks.WATER.defaultBlockState(), 2);
	}

	/**
	 * Jumps, and the water is the reason they are allowed to be hard.
	 *
	 * Three-block gaps, which is a running jump and not a sprint jump, staggered
	 * left and right so the line matters. Water directly under every one of them:
	 * a miss is a swim and a climb, never a death and never an inventory.
	 */
	private static void gaps(ServerLevel level, BlockPos start, Direction heading,
	                         RandomSource random, boolean dark) {
		Direction across = heading.getClockWise();
		for (int step = 3; step < SEGMENT - 2; step += 5) {
			int lane = random.nextInt(3) - 1;
			for (int cut = 0; cut < 3; cut++) {
				BlockPos spine = start.relative(heading, step + cut);
				for (int side = -1; side <= 1; side++) {
					// The safe lane survives; everything else is a hole.
					if (side == lane) {
						continue;
					}
					level.setBlock(spine.relative(across, side),
						Blocks.AIR.defaultBlockState(), 2);
					level.setBlock(spine.relative(across, side).below(),
						Blocks.WATER.defaultBlockState(), 2);
				}
			}
			if (!dark) {
				// A lantern over the safe lane. In the DARK leg it is missing, and
				// that absence IS the leg — same geometry, no longer readable at a
				// glance, and the lava either side is the only light left.
				level.setBlock(start.relative(heading, step + 1)
					.relative(across, lane).above(4),
					Blocks.LANTERN.defaultBlockState()
						.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
	}

	/**
	 * Arrows out of the floor.
	 *
	 * A dispenser sunk flush in the walking surface, facing straight up, with a
	 * stone plate on its lid. That wiring is chosen because it cannot be wrong:
	 * a plate directly on a dispenser powers it, with no dust, no repeaters and
	 * nothing to misalign — and an untested redstone trap that silently does
	 * nothing is worse than no trap at all.
	 *
	 * Stone plates rather than wooden, so they are a different colour from the
	 * andesite floor. Rule four: visible before it is lethal.
	 */
	private static void plates(ServerLevel level, BlockPos start, Direction heading,
	                           RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 2; step < SEGMENT - 2; step += 3) {
			int side = random.nextInt(3) - 1;
			BlockPos at = start.relative(heading, step).relative(across, side);
			level.setBlock(at, Blocks.DISPENSER.defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.UP), 2);
			arm(level, at);
			level.setBlock(at.above(), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
		}
	}

	/**
	 * Something that moves, which is the one hazard the player cannot outread.
	 *
	 * Sticky pistons in the wall at chest height, armed by a plate on the floor
	 * directly beside them so the wiring is again as short as it can be. They
	 * shove sideways, toward the lava channel — and the channel is two blocks
	 * further out than the shove can reach, so being caught costs a stumble and
	 * a fright rather than an inventory. The threat does the work.
	 */
	private static void pistons(ServerLevel level, BlockPos start, Direction heading,
	                            RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 3; step < SEGMENT - 3; step += 6) {
			Direction shove = random.nextBoolean() ? across : across.getOpposite();
			BlockPos wall = start.relative(heading, step)
				.relative(shove.getOpposite(), 2).above();
			level.setBlock(wall, Blocks.STICKY_PISTON.defaultBlockState()
				.setValue(BlockStateProperties.FACING, shove), 2);
			// The trigger, on the floor, one step short of the piston's reach.
			level.setBlock(start.relative(heading, step).relative(shove.getOpposite(), 1),
				Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
		}
	}

	/**
	 * A floor that is not one.
	 *
	 * Gravel reads as loose ground and the player has already been taught in the
	 * lesson room that it means a hole. Water underneath, two blocks down, so it
	 * is a fall and a climb.
	 */
	private static void falseFloor(ServerLevel level, BlockPos start, Direction heading,
	                               RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 2; step < SEGMENT - 2; step++) {
			if (random.nextInt(3) != 0) {
				continue;
			}
			for (int side = -1; side <= 1; side++) {
				if (random.nextBoolean()) {
					continue;
				}
				BlockPos at = start.relative(heading, step).relative(across, side);
				level.setBlock(at, Blocks.GRAVEL.defaultBlockState(), 2);
				level.setBlock(at.below(), Blocks.AIR.defaultBlockState(), 2);
				level.setBlock(at.below(2), Blocks.WATER.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * The end, and it is a reward rather than a test.
	 *
	 * Candles, because they are the whole tell that somebody is STILL DOWN THERE.
	 * A ruin has cobwebs; a room with lit candles in it has a congregation, and
	 * the difference between those two readings is the entire point of the
	 * undercity being a cult rather than a crypt.
	 */
	private static void door(ServerLevel level, BlockPos start, Direction heading) {
		Direction across = heading.getClockWise();
		BlockPos face = start.relative(heading, SEGMENT - 3);
		for (int side = -2; side <= 2; side++) {
			for (int up = 0; up <= 5; up++) {
				BlockPos at = face.relative(across, side).above(up);
				boolean frame = side == -2 || side == 2 || up == 5;
				level.setBlock(at, frame
					? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
					: Blocks.AIR.defaultBlockState(), 2);
			}
		}
		for (int side = -1; side <= 1; side += 2) {
			BlockPos stand = face.relative(across, side).relative(heading.getOpposite(), 2);
			level.setBlock(stand, Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.LIT, true), 2);
		}
	}

	/**
	 * The safe ledge at the end of every stretch.
	 *
	 * Rule two, and it is what makes the corridor finishable. Solid floor, no
	 * hazards, a light, and a place to stand and look at the next twenty blocks
	 * before committing to them.
	 */
	private static void landing(ServerLevel level, BlockPos at, Direction heading,
	                            Leg kind) {
		Direction across = heading.getClockWise();
		for (int side = -1; side <= 1; side++) {
			BlockPos floor = at.relative(across, side);
			level.setBlock(floor, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
			level.setBlock(floor.above(), Blocks.AIR.defaultBlockState(), 2);
		}
		if (kind != Leg.DOOR) {
			level.setBlock(at.above(4), Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
	}

	/** A few arrows, and not a full stack. It is a warning, not an execution. */
	private static void arm(ServerLevel level, BlockPos at) {
		if (level.getBlockEntity(at) instanceof DispenserBlockEntity dispenser) {
			dispenser.setItem(4, new ItemStack(Items.ARROW, 6));
			dispenser.setChanged();
		}
	}
}
