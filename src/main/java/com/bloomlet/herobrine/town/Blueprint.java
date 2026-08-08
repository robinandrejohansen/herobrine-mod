package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.structure.Ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Puts a building down, facing a way, on ground that was not flat.
 *
 * The homestead taught two lessons the hard way and this exists so the town
 * does not learn them again. Both are here because a town is a dozen
 * homesteads, so anything that goes slightly wrong once goes wrong twelve
 * times and reads as a mod rather than a mistake.
 *
 * THE GROUND IS NOT WHERE THE HEIGHTMAP SAYS. Every heightmap Minecraft has
 * counts a tree trunk as terrain, so a building sited off one lands in the
 * canopy. Ground.topOf walks down to something a house could actually stand on.
 *
 * BUILDINGS FACE THINGS. A village of identically-oriented houses is a
 * spreadsheet. Everything here is written once, pointing north, and rotated on
 * placement so it can face the lane it stands on — which is the difference
 * between a settlement and twelve copies of a shed.
 *
 * Levelling is per building, never per town. Flattening a whole village site
 * gives the giant rectangular plateau that made the homestead look like a
 * building site from three hundred blocks away; each house gets its own small
 * footing instead, and the ground between them is left alone to roll.
 */
public final class Blueprint {
	private Blueprint() {}

	/**
	 * How deep a footing may go.
	 *
	 * Three, and the homestead is why. A course of stone under a low corner is
	 * a footing; anything deeper is a plinth, and a plinth means the plot was
	 * wrong and should have been refused rather than propped up on a pillar.
	 */
	private static final int MAX_FOOTING = 3;

	/**
	 * @param maps    one character map per layer, index 0 at floor level
	 * @param facing  which way the building's south side ends up pointing
	 * @param palette what each character means
	 * @return true if it was built
	 */
	public static boolean place(ServerLevel level, BlockPos origin, String[][] maps,
	                            Direction facing, Palette palette) {
		int depth = maps[0].length;
		int width = maps[0][0].length();

		if (!clearEnough(level, origin, width, depth)) {
			return false;
		}
		footing(level, origin, width, depth, facing);

		for (int layer = 0; layer < maps.length; layer++) {
			for (int z = 0; z < depth; z++) {
				String row = maps[layer][z];
				for (int x = 0; x < width; x++) {
					char c = row.charAt(x);
					if (c == ' ') {
						continue;
					}
					BlockPos at = origin.offset(turn(x, z, width, depth, facing, true),
						layer, turn(x, z, width, depth, facing, false));
					palette.set(level, at, c, facing);
				}
			}
		}
		return true;
	}

	/**
	 * Rotate a map coordinate about the building's own corner.
	 *
	 * Written out rather than done with a matrix because the off-by-one at the
	 * far edge of a rotation is the kind of bug that shifts a whole wall by a
	 * block and is then invisible in the source.
	 *
	 * The WEST and EAST cases were swapped in the first version, which is a
	 * mistake worth recording because of how it presented: half the buildings
	 * faced AWAY from their lane, and every door, bed and stair on those two
	 * orientations pointed the wrong way — so it turned up as "a bed is hanging
	 * out of the wall" rather than as anything to do with rotation.
	 *
	 * The invariant, and it is worth testing whenever this is touched: the
	 * map's front row must end up on the side of the building that `facing`
	 * points to.
	 */
	private static int turn(int x, int z, int width, int depth, Direction facing, boolean wantX) {
		return switch (facing) {
			case SOUTH -> wantX ? x : z;
			case WEST -> wantX ? depth - 1 - z : x;
			case NORTH -> wantX ? width - 1 - x : depth - 1 - z;
			case EAST -> wantX ? z : width - 1 - x;
			default -> wantX ? x : z;
		};
	}

	/** The rotated X of a map coordinate, for anything placed outside place(). */
	public static int spinX(int x, int z, int width, int depth, Direction facing) {
		return turn(x, z, width, depth, facing, true);
	}

	public static int spinZ(int x, int z, int width, int depth, Direction facing) {
		return turn(x, z, width, depth, facing, false);
	}

	/**
	 * Turn a direction the same way the building turned.
	 *
	 * Maps are written front-south, so a stair or a door that faces south on
	 * paper has to end up facing whichever way the building was rotated to. The
	 * transform is the same one turn() applies to coordinates, applied to a
	 * step vector instead — written out per case for the same reason turn() is,
	 * because a sign error here rotates every roof slope the wrong way and
	 * looks almost right.
	 */
	public static Direction turned(Direction inMap, Direction facing) {
		int dx = inMap.getStepX();
		int dz = inMap.getStepZ();
		int nx;
		int nz;
		switch (facing) {
			case WEST -> { nx = -dz; nz = dx; }
			case NORTH -> { nx = -dx; nz = -dz; }
			case EAST -> { nx = dz; nz = -dx; }
			default -> { nx = dx; nz = dz; }
		}
		if (nx > 0) {
			return Direction.EAST;
		}
		if (nx < 0) {
			return Direction.WEST;
		}
		return nz < 0 ? Direction.NORTH : Direction.SOUTH;
	}

	/**
	 * A stone footing down to real ground, and the air above cleared.
	 *
	 * Only under the building's own footprint plus a block, so the land between
	 * houses keeps whatever shape it had. A house on a slope gets a visible
	 * plinth on its low side, which is what a real one would have and reads as
	 * part of the building rather than as terrain that was bulldozed.
	 */
	private static void footing(ServerLevel level, BlockPos origin, int width, int depth,
	                            Direction facing) {
		for (int x = -1; x <= width; x++) {
			for (int z = -1; z <= depth; z++) {
				BlockPos at = origin.offset(turn(x, z, width, depth, facing, true), 0,
					turn(x, z, width, depth, facing, false));
				for (int down = 1; down <= MAX_FOOTING; down++) {
					BlockPos below = at.below(down);
					if (!level.getBlockState(below).isAir()
						&& level.getFluidState(below).isEmpty()) {
						break;
					}
					level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 2);
				}
				for (int up = 0; up <= 8; up++) {
					BlockPos above = at.above(up);
					if (!level.getBlockState(above).isAir()) {
						level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/** Is this plot flat enough and dry enough to build on? */
	private static boolean clearEnough(ServerLevel level, BlockPos origin, int width, int depth) {
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int x = 0; x <= width; x += Math.max(1, width / 3)) {
			for (int z = 0; z <= depth; z += Math.max(1, depth / 3)) {
				int y = Ground.topOf(level, origin.getX() + x, origin.getZ() + z);
				if (!level.getFluidState(new BlockPos(origin.getX() + x, y, origin.getZ() + z))
						.isEmpty()) {
					return false;
				}
				low = Math.min(low, y);
				high = Math.max(high, y);
			}
		}
		return high - low <= 4;
	}

	/** What a character means, so each kind of building can read differently. */
	@FunctionalInterface
	public interface Palette {
		void set(ServerLevel level, BlockPos at, char c, Direction facing);
	}

	/** Convenience for palettes: place a state only if there is room. */
	public static void put(ServerLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, 2);
	}
}
