package com.bloomlet.herobrine.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * WHAT IS BETWEEN THE BUILDINGS, AND IT IS MOST OF WHAT MAKES THEM A PLACE.
 *
 * Every structure in this mod has been built as an object: a house, a shed, a
 * tower, each one sited on its own and each one landing on untouched grass. That
 * is why the property has never read as a property. Three good buildings on
 * separate patches of forest are three buildings; the same three with a worn
 * track running between them are a smallholding, and the track is about forty
 * lines of code.
 *
 * TWO THINGS, AND THEY WANT TO BE ONE CLASS because they are the same idea.
 *
 *   A TRACK. Dirt path from A to B, following the contour, stepping where the
 *   ground steps. A path is the only block in the game that means "somebody
 *   walked here more than once", and there is nothing else that says it — no
 *   amount of trim on a wall implies use, and a single strip of worn ground
 *   implies years of it.
 *
 *   AND THE DRESSING. Banked stone, moss, ferns, flowers, thinning outward so
 *   there is no edge to it. A building meeting flat turf at a hard line is the
 *   giveaway that something was pasted in; the same building with the ground
 *   coming up to meet it is dug in.
 *
 * IT NEVER TOUCHES ANYTHING STANDING. Both methods refuse any position that is
 * not open air over solid ground, so a path aimed through a wall stops at the
 * wall rather than tunnelling, and the dressing cannot put a fern on somebody's
 * doorstep or a boulder through a window. Which is what makes it safe to run
 * over ground that already has buildings on it.
 */
public final class Grounds {
	private Grounds() {}

	// ---- THE TRACK ---------------------------------------------------------
	/** Widest a track gets. Three is a cart, one is a desire line. */
	private static final int WIDE = 1;
	/** Above this climb in one step it lays a stone tread instead of dirt. */
	private static final int STEPS_AT = 1;
	/** And it gives up rather than walking off the end of the world. */
	private static final int GIVES_UP_AFTER = 400;

	/**
	 * A worn way from one place to another.
	 *
	 * WALKED RATHER THAN DRAWN. A straight line between two points is a survey
	 * mark; this wanders by a block either side as it goes, which is what a track
	 * made by feet does — it avoids the wet bit, it cuts the corner, and the
	 * wander is the entire difference between a path and a runway.
	 *
	 * @return true if it got there
	 */
	public static boolean track(ServerLevel level, BlockPos from, BlockPos to,
	                            RandomSource random) {
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		double span = Math.sqrt(dx * dx + dz * dz);
		if (span < 4.0 || span > GIVES_UP_AFTER) {
			return false;
		}
		int steps = (int) Math.ceil(span);
		int lastY = Integer.MIN_VALUE;
		int laid = 0;
		for (int step = 0; step <= steps; step++) {
			double along = step / (double) steps;
			// The wander: a slow sine across the line, plus a block of noise, so it
			// neither reads as straight nor as random.
			double sway = Math.sin(along * Math.PI * 3.0) * 1.6
				+ (random.nextInt(3) - 1) * 0.5;
			double nx = -dz / span;
			double nz = dx / span;
			int x = (int) Math.round(from.getX() + dx * along + nx * sway);
			int z = (int) Math.round(from.getZ() + dz * along + nz * sway);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			for (int off = -WIDE; off <= WIDE; off++) {
				// Ragged at the edges. A path worn by feet is not a ruler width, and
				// the ragged edge is what stops the eye reading a boundary.
				if (off != 0 && random.nextInt(3) == 0) {
					continue;
				}
				int px = x + (int) Math.round(nx * off);
				int pz = z + (int) Math.round(nz * off);
				if (!Ground.dry(level, px, pz)) {
					continue;
				}
				int y = Ground.topOf(level, px, pz);
				BlockPos on = new BlockPos(px, y, pz);
				// NEVER THROUGH ANYTHING BUILT. If what is under our feet is not
				// natural ground, we are crossing a floor or a roof, and the answer
				// is to leave it exactly as it is.
				if (!walkable(level, on)) {
					continue;
				}
				if (off == 0 && lastY != Integer.MIN_VALUE
					&& Math.abs(y - lastY) > STEPS_AT) {
					// A TREAD RATHER THAN A SCRAMBLE. Where the ground jumps, a real
					// path has a stone in it, and one mossy slab reads as a step
					// somebody put there far better than more dirt does.
					put(level, on, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
					put(level, on.above(),
						Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
					laid++;
					continue;
				}
				put(level, on, random.nextInt(8) == 0
					? Blocks.COARSE_DIRT.defaultBlockState()
					: Blocks.DIRT_PATH.defaultBlockState());
				// And nothing growing in the middle of a road.
				BlockState over = level.getBlockState(on.above());
				if (!over.isAir() && !over.isSolid()) {
					put(level, on.above(), Blocks.AIR.defaultBlockState());
				}
				laid++;
			}
			int mid = Ground.topOf(level, x, z);
			lastY = mid;
		}
		return laid > 0;
	}

	/**
	 * Whether a path is allowed to lie here.
	 *
	 * Grass, dirt, gravel, sand, stone and moss. Not planks, not a slab, not
	 * anything with a block entity — which is the test that keeps a track from
	 * eating the floor of the room it was aimed at.
	 */
	private static boolean walkable(ServerLevel level, BlockPos on) {
		BlockState state = level.getBlockState(on);
		if (level.getBlockEntity(on) != null) {
			return false;
		}
		return state.is(Blocks.GRASS_BLOCK)
			|| state.is(net.minecraft.tags.BlockTags.DIRT)
			|| state.is(Blocks.GRAVEL)
			|| state.is(net.minecraft.tags.BlockTags.SAND)
			|| state.is(Blocks.STONE)
			|| state.is(Blocks.ANDESITE)
			|| state.is(Blocks.MOSS_BLOCK)
			|| state.is(Blocks.PODZOL)
			|| state.is(Blocks.DIRT_PATH)
			|| state.is(Blocks.COARSE_DIRT);
	}

	// ---- THE DRESSING ------------------------------------------------------
	/**
	 * Ground that has been lived on.
	 *
	 * @param inner  how far out the clear apron round the walls runs
	 * @param outer  and where it has faded to nothing
	 *
	 * THINNING IS THE WHOLE TECHNIQUE. A uniform scatter over a fixed radius has
	 * a visible edge, and a visible edge reads as a mod having run. The chance of
	 * placing anything falls off with distance, so the effect has no boundary — it
	 * is simply denser near the door, which is also true of real ground.
	 */
	public static void dress(ServerLevel level, BlockPos centre, int inner, int outer,
	                         RandomSource random) {
		for (int dx = -outer; dx <= outer; dx++) {
			for (int dz = -outer; dz <= outer; dz++) {
				double away = Math.sqrt(dx * dx + dz * dz);
				if (away < inner || away > outer) {
					continue;
				}
				if (random.nextDouble() > 1.0 - away / (outer + 3.0)) {
					continue;
				}
				int x = centre.getX() + dx;
				int z = centre.getZ() + dz;
				if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))
					|| !Ground.dry(level, x, z)) {
					continue;
				}
				int y = Ground.topOf(level, x, z);
				BlockPos on = new BlockPos(x, y, z);
				BlockPos over = on.above();
				if (!level.getBlockState(over).isAir()
					|| !walkable(level, on)
					|| level.getBlockState(on).is(Blocks.DIRT_PATH)) {
					continue;      // never over a road and never inside anything
				}
				switch (random.nextInt(18)) {
					case 0, 1 -> put(level, on, Blocks.MOSS_BLOCK.defaultBlockState());
					case 2 -> put(level, on, Blocks.PODZOL.defaultBlockState());
					case 3 -> {
						// The terracing. A boulder with a mossy cap, and this one
						// pair of blocks is most of what the screenshots are doing.
						put(level, over, random.nextBoolean()
							? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
							: Blocks.ANDESITE.defaultBlockState());
						put(level, over.above(),
							Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
					}
					case 4, 5, 6 -> put(level, over, Blocks.SHORT_GRASS.defaultBlockState());
					case 7 -> put(level, over, Blocks.TALL_GRASS.defaultBlockState());
					case 8 -> put(level, over, Blocks.FERN.defaultBlockState());
					case 9 -> put(level, over, Blocks.POPPY.defaultBlockState());
					case 10 -> put(level, over, Blocks.AZURE_BLUET.defaultBlockState());
					case 11 -> put(level, over, Blocks.OXEYE_DAISY.defaultBlockState());
					case 12 -> put(level, over, Blocks.MOSS_CARPET.defaultBlockState());
					case 13 -> put(level, over, Blocks.SWEET_BERRY_BUSH.defaultBlockState());
					case 14 -> put(level, over, Blocks.DEAD_BUSH.defaultBlockState());
					case 15 -> put(level, over,
						Blocks.PALE_MOSS_CARPET.defaultBlockState());
					default -> { }
				}
			}
		}
	}

	// ---- THE CLUTTER -------------------------------------------------------
	/**
	 * The things a working yard has in it, dropped round one point.
	 *
	 * A LOG PILE IS WORTH MORE THAN A GABLE. Everything here is one or two blocks
	 * and every one of them implies a job somebody does: split wood, a barrel of
	 * something, water drawn, a pot on a wall. They are the reason a build reads as
	 * occupied rather than finished, and none of them are architecture.
	 */
	public static void yard(ServerLevel level, BlockPos near, Direction outward,
	                        RandomSource random) {
		Direction across = outward.getClockWise();

		// The wood pile, ends showing, two courses.
		BlockPos pile = ground(level, near.relative(across, 3));
		if (pile != null) {
			for (int dy = 0; dy < 2; dy++) {
				for (int off = 0; off < 3; off++) {
					put(level, pile.above(dy + 1).relative(outward, off),
						(dy == 1 && off == 2
							? Blocks.SPRUCE_LOG : Blocks.STRIPPED_SPRUCE_LOG)
							.defaultBlockState()
							.setValue(BlockStateProperties.AXIS, outward.getAxis()));
				}
			}
			put(level, pile.above(3), Blocks.SPRUCE_SLAB.defaultBlockState());
		}

		// A barrel and a composter, which between them say "this was somebody's
		// job" without a single line of text.
		BlockPos work = ground(level, near.relative(across, -3));
		if (work != null) {
			put(level, work.above(), Blocks.BARREL.defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.UP));
			put(level, work.above().relative(outward),
				Blocks.COMPOSTER.defaultBlockState());
			put(level, work.above().relative(outward, 2),
				Blocks.HAY_BLOCK.defaultBlockState());
		}

		// Water drawn from somewhere. Not a whole well — a trough, which is
		// cheaper and reads better beside a house than a stone ring does.
		BlockPos trough = ground(level, near.relative(outward, 5).relative(across, 1));
		if (trough != null) {
			for (int off = -1; off <= 1; off++) {
				put(level, trough.relative(across, off),
					Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			}
			put(level, trough, Blocks.CAULDRON.defaultBlockState());
			put(level, trough.relative(across, 2),
				Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
			put(level, trough.relative(across, 2).above(),
				Blocks.FLOWER_POT.defaultBlockState());
		}

		// A fence line with a lantern on it, because a lit post at the edge of a
		// yard is how you tell from two hundred blocks away that this is a place
		// rather than a ruin.
		BlockPos post = ground(level, near.relative(outward, 7).relative(across, -2));
		if (post != null) {
			for (int dy = 1; dy <= 2; dy++) {
				put(level, post.above(dy), Blocks.SPRUCE_FENCE.defaultBlockState());
			}
			put(level, post.above(3), Blocks.LANTERN.defaultBlockState());
			for (int off = 1; off <= 3; off++) {
				BlockPos rail = ground(level, post.relative(across, off));
				if (rail != null) {
					put(level, rail.above(), Blocks.SPRUCE_FENCE.defaultBlockState());
				}
			}
		}
	}

	// ---- THE PRIMITIVES ----------------------------------------------------
	/** The top of the column, or null if there is nothing to stand on. */
	private static @org.jspecify.annotations.Nullable BlockPos ground(
			ServerLevel level, BlockPos near) {
		if (!level.isLoaded(near) || !Ground.dry(level, near.getX(), near.getZ())) {
			return null;
		}
		BlockPos on = new BlockPos(near.getX(),
			Ground.topOf(level, near.getX(), near.getZ()), near.getZ());
		if (!walkable(level, on) || !level.getBlockState(on.above()).isAir()) {
			return null;
		}
		return on;
	}

	private static void put(ServerLevel level, BlockPos at, BlockState state) {
		if (at.getY() <= level.getMinY() || at.getY() >= level.getMaxY()) {
			return;
		}
		level.setBlock(at, state, 2);
	}
}
