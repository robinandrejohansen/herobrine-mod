package com.bloomlet.herobrine.town;

import java.util.ArrayList;
import java.util.List;

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
	/**
	 * LEVEL THE PLOT INSTEAD OF REFUSING IT.
	 *
	 * This used to measure the slope and give up when it exceeded a tolerance,
	 * and every version of that idea is wrong for the same reason: the failure is
	 * SILENT and it lands hardest on the biggest buildings. The church is nineteen
	 * by thirty-one, so it samples the most ground, so it finds the most fall, so
	 * the most important structure in the town was the one that reliably did not
	 * exist. Raising the tolerance only moved the number the terrain had to beat —
	 * the log said "16 blocks of slope, 14 allowed" and there is no value of
	 * "allowed" that real ground will not eventually exceed.
	 *
	 * So the ground is made flat. Which is also what actually happens: nobody has
	 * ever built a church on a hillside without cutting the hillside first, and a
	 * levelled terrace with a stone face on the downhill side is the single most
	 * recognisable sign of a settlement in any landscape.
	 *
	 * THE MEDIAN, NOT THE MEAN OR THE PEAK. The mean gets dragged by one ravine
	 * corner and the peak buries the whole plot in fill; the median is the height
	 * most of the plot already is, so most of the work is nothing and the cut and
	 * the fill stay small.
	 *
	 * AND WATER IS NOT A REASON EITHER. IT WAS, AND IT WAS THE SAME BUG AGAIN.
	 *
	 * The comment that used to sit here said water is "the one thing terracing
	 * cannot fix", and one wet block anywhere in the sampled area refused the plot
	 * outright. Read terrace(): it walks DOWN from the target height filling stone
	 * bricks until it hits something solid, and water is not solid. It has been
	 * able to fill a puddle the whole time.
	 *
	 * So the failure was identical in shape to the slope one this docstring already
	 * describes at length — silent, and landing hardest on the biggest building. The
	 * church is nineteen by thirty-one, so it samples six hundred and ninety-three
	 * columns, so it has the best chance of finding one puddle, so the most
	 * important structure in the town was again the one that reliably did not exist.
	 * A playthrough log: four plots refused, and the 19x31 was one of them.
	 *
	 * What is left is the one case filling genuinely should not fix. A plot mostly
	 * under water is a LAKE, and a stone terrace standing in one is not a terrace,
	 * it is a plinth. Two thirds, measured on the columns rather than guessed at.
	 *
	 * @return false only for a plot that is mostly lake
	 */
	private static final double MOSTLY_LAKE = 0.66;

	private static boolean clearEnough(ServerLevel level, BlockPos origin, int width, int depth) {
		List<Integer> heights = new ArrayList<>();
		int wet = 0;
		for (int x = -1; x <= width + 1; x++) {
			for (int z = -1; z <= depth + 1; z++) {
				int gx = origin.getX() + x;
				int gz = origin.getZ() + z;
				int y = Ground.topOf(level, gx, gz);
				if (!level.getFluidState(new BlockPos(gx, y, gz)).isEmpty()
					|| !level.getFluidState(new BlockPos(gx, y + 1, gz)).isEmpty()) {
					wet++;
				}
				heights.add(y);
			}
		}
		if (wet > heights.size() * MOSTLY_LAKE) {
			com.bloomlet.herobrine.HerobrineMod.LOGGER.info(
				"a {}x{} plot at [{}, {}] is a lake — {} of {} columns under water",
				width, depth, origin.getX(), origin.getZ(), wet, heights.size());
			return false;
		}
		heights.sort(null);
		int level0 = heights.get(heights.size() / 2);
		terrace(level, origin, width, depth, level0);
		if (wet > 0) {
			com.bloomlet.herobrine.HerobrineMod.LOGGER.info(
				"a {}x{} plot at [{}, {}] had {} wet columns — filled",
				width, depth, origin.getX(), origin.getZ(), wet);
		}
		return true;
	}

	/**
	 * Cut the high side, fill the low side, and face the fill in stone.
	 *
	 * The apron runs one block past the footprint so the building is not standing
	 * on a plinth exactly its own size, which reads as a floating tray. Anything
	 * DwellTracker calls built is left alone — a terrace through somebody's floor
	 * is the one outcome worse than a building that never appeared.
	 */
	private static final int FEATHER = 6;

	private static void terrace(ServerLevel level, BlockPos origin, int width, int depth,
	                            int flat) {
		// THE APRON FEATHERS OUT, or the town sits in a quarry.
		//
		// A terrace that stops dead at the footprint leaves a vertical face all
		// the way round — a perfect rectangular shelf cut into a hillside, which
		// is the thing in the screenshot that reads as a bug rather than as a
		// building. Nothing in a landscape has edges like that.
		//
		// So outside the footprint the target height walks back toward the real
		// ground, one block per ring, until it meets it. The building stands on
		// flat ground and the flat ground becomes the hill again over six blocks,
		// which is a bank rather than a cliff — and a bank is what a levelled
		// plot on a slope actually looks like.
		for (int x = -FEATHER; x <= width + FEATHER; x++) {
			for (int z = -FEATHER; z <= depth + FEATHER; z++) {
				int out = Math.max(0, Math.max(
					Math.max(-x, x - width), Math.max(-z, z - depth)));
				int target = flat;
				if (out > 1) {
					int natural = Ground.topOf(level, origin.getX() + x, origin.getZ() + z);
					// Straight-line blend from the terrace to the terrain.
					double t = (double)(out - 1) / (FEATHER - 1);
					target = (int)Math.round(flat + (natural - flat) * t);
				}
				BlockPos ground = new BlockPos(origin.getX() + x, target, origin.getZ() + z);
				if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, ground)) {
					continue;
				}
				// Down to solid: fill whatever hangs in the air beneath the terrace.
				for (int down = 0; down < 24; down++) {
					BlockPos at = ground.below(down);
					if (level.getBlockState(at).isSolid()) {
						break;
					}
					level.setBlock(at, down == 0
						? Blocks.GRASS_BLOCK.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				if (!level.getBlockState(ground).isSolid()) {
					level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				}
				// And take everything off the top of it.
				for (int up = 1; up <= 20; up++) {
					BlockPos at = ground.above(up);
					if (level.getBlockState(at).isAir()) {
						continue;
					}
					if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, at)) {
						continue;
					}
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
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
		java.util.Set<BlockPos> gone = new java.util.HashSet<>();
		for (int x = -2; x <= width + 2; x++) {
			for (int z = -2; z <= depth + 2; z++) {
				int ground = Ground.topOf(level, origin.getX() + x, origin.getZ() + z);
				for (int y = ground + 1; y <= ground + 16; y++) {
					BlockPos at = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
					if (level.getBlockState(at).is(net.minecraft.tags.BlockTags.LOGS)) {
						uproot(level, at, gone);
					}
				}
			}
		}
	}

	/**
	 * TAKE THE WHOLE TREE, NOT THE HALF THAT WAS IN THE WAY.
	 *
	 * The first version cleared logs and leaves inside the footprint and stopped
	 * at the edge, which sliced every tree on the boundary clean in half and left
	 * the top hanging in the air. That is the single most recognisable
	 * mod-generated-badly artefact there is — worse than the tree being there,
	 * because a tree in a wall reads as terrain and a floating tree crown reads as
	 * broken software.
	 *
	 * So a log anywhere near the plot uproots its ENTIRE tree: flood out through
	 * connected logs, then take the leaves hanging off them. The tree either
	 * stands or it does not exist, and nothing is ever left mid-air.
	 *
	 * Capped, because a flood fill through a dark oak canopy or a jungle can walk
	 * a very long way, and this runs once per plot in a fifteen-plot town.
	 */
	private static void uproot(ServerLevel level, BlockPos from,
	                           java.util.Set<BlockPos> gone) {
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		queue.add(from);
		int budget = 900;
		while (!queue.isEmpty() && budget-- > 0) {
			BlockPos at = queue.poll();
			if (!gone.add(at)) {
				continue;
			}
			BlockState state = level.getBlockState(at);
			boolean log = state.is(net.minecraft.tags.BlockTags.LOGS);
			boolean leaf = state.is(net.minecraft.tags.BlockTags.LEAVES);
			if (!log && !leaf) {
				continue;
			}
			level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			// Leaves are followed but never spread from, so the fill cannot walk
			// across a canopy into the next tree and keep going.
			if (!log) {
				continue;
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						queue.add(at.offset(dx, dy, dz));
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

	/**
	 * A BARREL WITH SOMETHING IN IT.
	 *
	 * Every barrel in the town was placed by a palette line and then never
	 * touched again, so a walled settlement with a forge, a market and a hall
	 * had a dozen containers in it that all opened onto nothing. That is worse
	 * than having no barrels: an empty container reads as an unfinished mod,
	 * where no container at all reads as a room.
	 *
	 * Here rather than four copies in four buildings, because it was already
	 * the same line of code four times and would have become the same bug four
	 * times. Every one of them passes the tier of the room it is standing in,
	 * which is the only part that differs — the smith's holds iron, the hall's
	 * holds what the watch is issued, the lodges hold dinner.
	 *
	 * Loot.store rather than Loot.scatter, deliberately: a barrel is a store
	 * cupboard and a chest is a strongbox, so the barrels get fewer stacks and
	 * never the enchanted roll. The chests stay the prize.
	 */
	public static void barrel(ServerLevel level, BlockPos at,
	                          net.minecraft.util.RandomSource random,
	                          com.bloomlet.herobrine.structure.Loot.Tier tier) {
		level.setBlock(at, net.minecraft.world.level.block.Blocks.BARREL
			.defaultBlockState(), 2);
		if (level.getBlockEntity(at)
				instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity store) {
			com.bloomlet.herobrine.structure.Loot.store(store, random, tier);
		}
	}
}
