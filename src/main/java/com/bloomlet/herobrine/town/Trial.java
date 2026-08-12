package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
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
 * IT IS MEANT TO BE HARD, AND IT IS MEANT TO COST SOMETHING.
 *
 * An earlier version of this caught every fall in water and kept the lava beside
 * the path as a light source and a threat. That is the safe design and it is
 * defensible: nobody loses an inventory, so nobody stops exploring. It was
 * overruled deliberately, and the reasoning is sound — a hazard that cannot take
 * anything from you is scenery, and a congregation's private door SHOULD be
 * impassable to somebody who wandered in unprepared.
 *
 * So: lava under the gaps, no water anywhere, four-block jumps, and plates you
 * will not see unless you are looking for them. What that buys is a corridor
 * people PREPARE for — they come back with blocks, with fire resistance, with a
 * plan — and the undercity stops being a place you found and becomes a place you
 * got into.
 *
 * The two rules that survive, because they are about fairness rather than
 * difficulty:
 *
 *   A LANDING BETWEEN EVERY LEG. Failure costs one stretch, not the whole run.
 *     Hard is fine. Restarting a hundred and forty blocks is not hard, it is
 *     tedious, and tedium is the only thing that actually stops people.
 *
 *   ONE SIGN AT THE MOUTH. Not a tutorial — a warning. The corridor is allowed
 *     to kill somebody who ignored it and not somebody who never knew.
 *
 * AND IT IS NOT A PERFECT TUBE. Width, height and floor level all wander, the
 * bearing joggles, and there are stretches where the cut is rough and unfinished.
 * A dead-straight five-by-six corridor for a hundred and forty blocks is the same
 * fault as a mathematically perfect dome: whatever is put inside it, the shape
 * says a machine made it.
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
	private enum Leg { WARNING, GAPS, PLATES, PISTONS, DARK, FALSE_FLOOR, DOOR }

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
			hollow(level, at, heading, kind, random);
			switch (kind) {
				case WARNING -> warning(level, at, heading);
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
		surface(level, at, heading, random);
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
	                          Leg kind, RandomSource random) {
		Direction across = heading.getClockWise();
		// THE SECTION WANDERS. Half a metre of variation either side and a block
		// of variation in the roof is enough — the eye reads a corridor that
		// changes as cut by hand and one that does not as extruded.
		double phase = random.nextDouble() * Math.PI * 2.0;
		for (int step = 0; step < SEGMENT; step++) {
			double wobble = Math.sin(step * 0.4 + phase);
			int half = 2 + (wobble > 0.6 ? 1 : 0);
			int tall = 5 + (wobble < -0.5 ? 1 : 0);
			BlockPos spine = start.relative(heading, step);
			for (int side = -half; side <= half; side++) {
				BlockPos column = spine.relative(across, side);
				for (int up = 0; up <= tall; up++) {
					level.setBlock(column.above(up), Blocks.AIR.defaultBlockState(), 2);
				}
				level.setBlock(column.above(tall + 1), rough(random), 2);
				level.setBlock(column.below(2), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				// LAVA UNDER EVERYTHING THAT IS NOT THE PATH, out to the walls.
				level.setBlock(column.below(), Blocks.LAVA.defaultBlockState(), 2);
			}
			// The walking surface, cut back to a narrow lane so a miss is a fall.
			for (int side = -1; side <= 1; side++) {
				level.setBlock(spine.relative(across, side), floorOf(random), 2);
			}
			// Torches only on the finished stretches. The lava lights the rest.
			if (kind != Leg.DARK && step % 9 == 4) {
				level.setBlock(spine.relative(across, half).above(2),
					Blocks.WALL_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING,
							across.getOpposite()), 2);
			}
		}
	}

	/**
	 * A wall that was not finished to the same standard all the way along.
	 *
	 * Mostly brick, with cobble and cracked courses through it. Somebody cut this
	 * over a long time and got worse at caring.
	 */
	private static BlockState rough(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 5) {
			return Blocks.STONE_BRICKS.defaultBlockState();
		}
		if (roll < 8) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.COBBLESTONE.defaultBlockState();
	}

	/** And the floor is laid to the same standard, which is to say unevenly. */
	private static BlockState floorOf(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 6) {
			return Blocks.POLISHED_ANDESITE.defaultBlockState();
		}
		return roll < 9
			? Blocks.ANDESITE.defaultBlockState()
			: Blocks.COBBLESTONE.defaultBlockState();
	}

	/**
	 * ONE SIGN, AT THE MOUTH, AND THAT IS THE ENTIRETY OF THE WARNING.
	 *
	 * The teaching room that used to be here demonstrated every hazard harmlessly
	 * before it could hurt anybody. That was the right call for a corridor meant
	 * to be crossable and the wrong one for a corridor meant to keep people out.
	 * A congregation does not put a tutorial in front of its own door.
	 *
	 * So it is a sign and a clear stretch of floor. Anybody who reads it and turns
	 * round has lost nothing; anybody who walks past it has been told.
	 */
	private static void warning(ServerLevel level, BlockPos start, Direction heading) {
		Direction across = heading.getClockWise();
		BlockPos post = start.relative(heading, 4).relative(across, 2).above();
		if (level.getBlockState(post).isAir()) {
			level.setBlock(post, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, 0), 3);
			if (level.getBlockEntity(post) instanceof SignBlockEntity plate) {
				SignText text = new SignText();
				String[] said = { "", "not for you", "turn round", "" };
				for (int row = 0; row < 4; row++) {
					text = text.setMessage(row, Component.literal(said[row]));
				}
				plate.setText(text, true);
				plate.setWaxed(true);
			}
		}
	}

	/**
	 * FOUR-BLOCK GAPS OVER OPEN LAVA, and the lane is not always the same one.
	 *
	 * Four is a sprint jump — it cannot be walked and it cannot be done wrong
	 * twice. Three was a running jump, which is to say a formality.
	 *
	 * The surviving lane moves between gaps, so the corridor cannot be crossed on
	 * one line: every gap has to be read. And the lava is directly beneath, out to
	 * the walls, with nothing to catch anybody — which is the whole change from
	 * the earlier version of this file and the reason people will bring blocks.
	 */
	private static void gaps(ServerLevel level, BlockPos start, Direction heading,
	                         RandomSource random, boolean dark) {
		Direction across = heading.getClockWise();
		for (int step = 3; step < SEGMENT - 3; step += 6) {
			int lane = random.nextInt(3) - 1;
			for (int cut = 0; cut < 4; cut++) {
				BlockPos spine = start.relative(heading, step + cut);
				for (int side = -1; side <= 1; side++) {
					if (side == lane && cut == 0) {
						continue;   // the lip you leave from
					}
					level.setBlock(spine.relative(across, side),
						Blocks.AIR.defaultBlockState(), 2);
					level.setBlock(spine.relative(across, side).below(),
						Blocks.LAVA.defaultBlockState(), 2);
				}
			}
			if (!dark) {
				level.setBlock(start.relative(heading, step + 2)
					.relative(across, lane).above(4),
					Blocks.LANTERN.defaultBlockState()
						.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
	}

	/**
	 * HIDDEN PLATES, and hiding them is a matter of matching the floor.
	 *
	 * The earlier version used stone plates on polished andesite specifically so
	 * they would stand out, on the principle that nothing should be lethal without
	 * being visible first. That principle is gone here by instruction.
	 *
	 * A stone pressure plate on a plain andesite or cobble floor is very nearly
	 * invisible at walking speed and from a normal head height — and since the
	 * floor is now laid unevenly out of three similar greys, the plates read as
	 * one more patch of it. Dispenser in the floor facing up with the plate on its
	 * lid, which is the one wiring that cannot be mis-assembled.
	 *
	 * A full stack of arrows now rather than six. It is not a warning any more.
	 */
	private static void plates(ServerLevel level, BlockPos start, Direction heading,
	                           RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 2; step < SEGMENT - 2; step += 2) {
			int side = random.nextInt(3) - 1;
			BlockPos at = start.relative(heading, step).relative(across, side);
			level.setBlock(at, Blocks.DISPENSER.defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.UP), 2);
			arm(level, at);
			level.setBlock(at.above(), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
			// And the block beside it is plain stone too, so the plate is not the
			// only thing in the corridor that colour.
			level.setBlock(at.relative(across), Blocks.ANDESITE.defaultBlockState(), 2);
		}
	}

	/**
	 * Something that moves, and now it can actually put you in the lava.
	 *
	 * The pistons used to shove one block short of the channel, so being caught
	 * was a fright. The lava reaches the walls now, so the shove reaches it too.
	 */
	private static void pistons(ServerLevel level, BlockPos start, Direction heading,
	                            RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 3; step < SEGMENT - 3; step += 5) {
			Direction shove = random.nextBoolean() ? across : across.getOpposite();
			BlockPos wall = start.relative(heading, step)
				.relative(shove.getOpposite(), 2).above();
			level.setBlock(wall, Blocks.STICKY_PISTON.defaultBlockState()
				.setValue(BlockStateProperties.FACING, shove), 2);
			level.setBlock(start.relative(heading, step).relative(shove.getOpposite(), 1),
				Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
		}
	}

	/**
	 * A floor that is not one, over lava rather than over a swim.
	 *
	 * Gravel that gives way, and more of it than before. With water underneath
	 * this was an inconvenience; it is now the cheapest killer in the corridor,
	 * which is appropriate for the hazard that requires the least skill to avoid —
	 * you only have to be looking down.
	 */
	private static void falseFloor(ServerLevel level, BlockPos start, Direction heading,
	                               RandomSource random) {
		Direction across = heading.getClockWise();
		for (int step = 2; step < SEGMENT - 2; step++) {
			if (random.nextInt(2) != 0) {
				continue;
			}
			for (int side = -1; side <= 1; side++) {
				if (random.nextInt(3) == 0) {
					continue;
				}
				BlockPos at = start.relative(heading, step).relative(across, side);
				level.setBlock(at, Blocks.GRAVEL.defaultBlockState(), 2);
				level.setBlock(at.below(), Blocks.LAVA.defaultBlockState(), 2);
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

	/**
	 * THE WAY IN, AND WITHOUT IT NONE OF THIS EXISTS.
	 *
	 * The corridor was cut and then dead-ended in solid rock, which made a
	 * hundred and forty blocks of gauntlet completely unreachable — a thing that
	 * only a player already inside the undercity could ever walk, in the wrong
	 * direction, having skipped the door. The most elaborate piece here was also
	 * the most invisible.
	 *
	 * So the far end climbs to daylight, and it comes up as something a person
	 * would build to hide a way down rather than as a hole: a ladder shaft, a
	 * stone rim at the top, and a trapdoor over it. Somebody who finds this is
	 * looking at a trapdoor in the middle of nowhere, and the only question worth
	 * having is whether to open it.
	 */
	private static void surface(ServerLevel level, BlockPos end, Direction heading,
	                            RandomSource random) {
		BlockPos base = end.relative(heading, 2);
		int top = level.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			base.getX(), base.getZ());
		// A shaft one block square, laddered the whole way, so it is a climb
		// rather than a fall in either direction.
		for (int y = base.getY(); y <= top; y++) {
			BlockPos at = new BlockPos(base.getX(), y, base.getZ());
			level.setBlock(at, Blocks.LADDER.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, heading), 2);
			// A sleeve of brick, so it reads as cut rather than as a cave.
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (side == heading.getOpposite()) {
					continue;   // the face the ladder is fixed to
				}
				BlockPos wall = at.relative(side);
				if (!level.getBlockState(wall).isSolid()) {
					level.setBlock(wall, Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
			level.setBlock(at.relative(heading.getOpposite()),
				Blocks.STONE_BRICKS.defaultBlockState(), 2);
		}
		BlockPos mouth = new BlockPos(base.getX(), top, base.getZ());
		// The rim, and then a lid on it.
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos rim = mouth.relative(side);
			if (level.getBlockState(rim).canBeReplaced()
				|| !level.getBlockState(rim).isSolid()) {
				level.setBlock(rim, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
			}
		}
		level.setBlock(mouth.above(), Blocks.OAK_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, heading)
			.setValue(BlockStateProperties.HALF,
				net.minecraft.world.level.block.state.properties.Half.BOTTOM), 2);
		HerobrineMod.LOGGER.info("the trial surfaces at [{}, {}, {}]",
			mouth.getX(), top, mouth.getZ());
	}

	/** A few arrows, and not a full stack. It is a warning, not an execution. */
	private static void arm(ServerLevel level, BlockPos at) {
		if (level.getBlockEntity(at) instanceof DispenserBlockEntity dispenser) {
			dispenser.setItem(4, new ItemStack(Items.ARROW, 64));
			dispenser.setChanged();
		}
	}
}
