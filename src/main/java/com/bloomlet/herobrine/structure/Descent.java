package com.bloomlet.herobrine.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * A way in from the surface, and it exists because the first dig had none.
 *
 * Digging.bore clamps every position it cuts to at least ROOF_CLEARANCE below
 * real ground, which is exactly right for its job — it stops a tunnel under a
 * forest opening into daylight. It also means a bore STARTED at the surface is
 * shoved seven blocks down before it cuts anything, so the passage begins
 * underground and there is no mouth. The third house was built, correctly and
 * completely, and sealed: you walked to the site and found a field.
 *
 * So openings are cut here instead, deliberately and without clamping, and
 * everything underground still uses bore. The two jobs are genuinely different
 * and the bug was one function being asked to do both.
 *
 * Two kinds. A SHAFT is a hole with a ladder in it — quick, cheap, and it reads
 * as something functional that somebody sank. A STAIR is a spiral in a square
 * well, which is slow to walk and is used where the descent is meant to be part
 * of the building rather than a way of getting under it.
 */
public final class Descent {
	private Descent() {}

	/**
	 * A square well with a stair winding down it.
	 *
	 * @param top    the block at ground level the well opens at
	 * @param bottom how far down to go
	 * @return where it lands, so the caller can carve on from there
	 */
	public static BlockPos stair(ServerLevel level, BlockPos top, int depth,
	                             BlockState wall, RandomSource random) {
		// Four by four, which is the smallest a spiral can be and still leave a
		// hole down the middle. Without the hole it is a staircase in a box and
		// the player cannot see how far they have left to go, which is most of
		// what makes a long descent worth walking.
		int[][] ring = { {1, 1}, {2, 1}, {2, 2}, {1, 2} };
		BlockPos at = top;

		for (int down = 0; down < depth; down++) {
			int[] cell = ring[down % ring.length];
			at = new BlockPos(top.getX() + cell[0], top.getY() - down, top.getZ() + cell[1]);

			// The shell of the well, cut fresh each course so it is lined all
			// the way down rather than only where the stair happens to touch.
			for (int dx = 0; dx <= 3; dx++) {
				for (int dz = 0; dz <= 3; dz++) {
					BlockPos pos = new BlockPos(top.getX() + dx, top.getY() - down,
						top.getZ() + dz);
					boolean edge = dx == 0 || dx == 3 || dz == 0 || dz == 3;
					level.setBlock(pos, edge ? wall : Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
			// The tread itself.
			level.setBlock(at, Blocks.COBBLESTONE_SLAB.defaultBlockState()
				.setValue(BlockStateProperties.SLAB_TYPE,
					net.minecraft.world.level.block.state.properties.SlabType.TOP), 2);
			for (int up = 1; up <= 2; up++) {
				level.setBlock(at.above(up), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}

			if (down % 6 == 3) {
				lantern(level, at.above(2));
			}
		}
		return at.below();
	}

	/**
	 * A hole with a ladder, and a lip round the top so nobody falls in it.
	 *
	 * The lip matters more than it sounds. An unmarked two-by-two hole in a
	 * field is indistinguishable from terrain damage, and a player walks past
	 * it; a course of stone round the rim says somebody made this, which is the
	 * difference between a bug and an entrance.
	 */
	public static BlockPos shaft(ServerLevel level, BlockPos top, int depth,
	                             BlockState rim) {
		for (int down = 0; down < depth; down++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					level.setBlock(top.offset(dx, -down, dz),
						Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
			BlockPos rung = top.offset(0, -down, 0);
			level.setBlock(rung, Blocks.LADDER.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
			// Something for the ladder to hang on, since a shaft through gravel
			// or sand would otherwise drop it the moment the chunk ticks.
			level.setBlock(rung.north(), rim, 2);
		}

		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 2; dz++) {
				boolean edge = dx == -1 || dx == 2 || dz == -1 || dz == 2;
				if (edge) {
					level.setBlock(top.offset(dx, 0, dz), rim, 2);
				}
			}
		}
		return top.below(depth - 1);
	}

	/** A trapdoor over an opening, so it reads as a door rather than a hole. */
	public static void hatch(ServerLevel level, BlockPos at, Direction facing) {
		level.setBlock(at, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
			.setValue(BlockStateProperties.HALF, Half.TOP)
			.setValue(BlockStateProperties.OPEN, true), 2);
	}

	private static void lantern(ServerLevel level, BlockPos at) {
		if (level.getBlockState(at).isAir()) {
			level.setBlock(at, Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
	}
}
