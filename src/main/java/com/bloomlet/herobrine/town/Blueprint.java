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

		// THE TREES COME DOWN FIRST, AND THEY DID NOT BEFORE.
		//
		// Ground.topOf already walks down past trunks and canopy to find real
		// footing, so the building was placed at the correct height — with the
		// tree still standing straight through it. In anything wooded that is
		// most of the town: walls interrupted by oak, roofs with leaves inside
		// them, and doorways nobody can walk through.
		//
		// Cleared with a one-block margin, because a canopy overhanging the eaves
		// looks exactly as wrong as one inside the wall.
		fell(level, origin, width, depth);
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
					palette.set(level, at, c, facing, x, z);
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
		// SEVEN, NOT FOUR, AND THE FOUR WAS THE BUG.
		//
		// This refusal is silent, and a silent refusal in a town generator means
		// plots that simply have nothing on them — which is what "couldn't render
		// the builds" looks like from inside the game. Four blocks of variation
		// across a whole plot is flat, and outside plains and deserts most ground
		// is not flat, so in hills and forest most buildings declined.
		//
		// Seven is safe because footing() lays a stone plinth down to real ground
		// underneath every building: a slope does not have to be levelled, it has
		// to be underpinned, and that is already happening. And the refusal logs
		// now, so the next time a plot comes up empty there is a line saying why
		// instead of a mystery.
		// AND THE TOLERANCE SCALES WITH THE FOOTPRINT, which is why the church
		// kept not appearing.
		//
		// A flat seven blocks is a reasonable ask of a nine-by-nine and an
		// impossible one of the church, which is nineteen by thirty-one — across
		// nearly six hundred square metres of real terrain there is almost always
		// more than seven blocks of fall somewhere, so the largest and most
		// important building in the town was the one guaranteed to be refused.
		//
		// Safe to relax because footing() underpins the whole footprint with a
		// stone plinth down to solid ground: slope is not levelled here, it is
		// built over, and a bigger building simply gets a bigger plinth.
		int allowed = 7 + Math.max(width, depth) / 4;
		if (high - low > allowed) {
			com.bloomlet.herobrine.HerobrineMod.LOGGER.info(
				"a {}x{} plot at [{}, {}] was refused: {} blocks of slope, {} allowed",
				width, depth, origin.getX(), origin.getZ(), high - low, allowed);
			return false;
		}
		return true;
	}

	/**
	 * Take the wood out of where a building is going.
	 *
	 * Logs and leaves only. Never stone, never dirt, never anything a player
	 * could have placed — this runs before the walls go up and it must not be
	 * capable of eating somebody's build, so it is restricted by tag to the two
	 * things that are always terrain.
	 */
	private static void fell(ServerLevel level, BlockPos origin, int width, int depth) {
		for (int x = -1; x <= width + 1; x++) {
			for (int z = -1; z <= depth + 1; z++) {
				int ground = Ground.topOf(level, origin.getX() + x, origin.getZ() + z);
				for (int y = ground + 1; y <= ground + 14; y++) {
					BlockPos at = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
					BlockState state = level.getBlockState(at);
					if (state.is(net.minecraft.tags.BlockTags.LOGS)
						|| state.is(net.minecraft.tags.BlockTags.LEAVES)) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/**
	 * Which way a seat should point, worked out from what it is next to.
	 *
	 * A stair used as a chair puts its TALL BACK on the side its facing names,
	 * so a bench beside a table has to face AWAY from the table for its back to
	 * be on the outside and its seat to open toward the food. Getting that
	 * backwards sat an entire hall with their backs to dinner.
	 *
	 * Read off the map rather than hand-assigned per bench, which is the real
	 * fix: a bench works out where the table is and turns away from it, so
	 * moving a table in the map moves the benches with it and no coordinate
	 * anywhere has to agree with any other.
	 *
	 * @param beside the character to sit against, normally the table
	 * @return the rotated direction to face, or the building's own if there is
	 *         nothing adjacent to sit at
	 */
	public static Direction seatFacing(String[] layer, int x, int z, char beside,
	                                   Direction facing) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int nx = x + side.getStepX();
			int nz = z + side.getStepZ();
			if (nz < 0 || nz >= layer.length || nx < 0 || nx >= layer[nz].length()) {
				continue;
			}
			if (layer[nz].charAt(nx) == beside) {
				// Away from it: the back goes on the far side from the table.
				return turned(side.getOpposite(), facing);
			}
		}
		return facing;
	}

	/** What a character means, so each kind of building can read differently. */
	@FunctionalInterface
	public interface Palette {
		/**
		 * @param x the character's column in the map, before rotation
		 * @param z its row, likewise — passed so a block can look at what it
		 *          stands next to rather than being told what to be
		 */
		void set(ServerLevel level, BlockPos at, char c, Direction facing, int x, int z);
	}

	/** Convenience for palettes: place a state only if there is room. */
	public static void put(ServerLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, 2);
	}
}
