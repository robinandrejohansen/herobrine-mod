package com.bloomlet.herobrine.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Cutting rock so it does not look cut.
 *
 * Shared by everything underground, because the hardest part of a dig is the
 * same everywhere: making it not read as architecture. Rooms have corners,
 * courses and right angles, and every one of those says somebody was making a
 * place to BE. Hollows have none — irregular, uneven-ceilinged, wider in some
 * places than others for no reason, on a line that wanders because whoever cut
 * it was not working to a plan.
 *
 * Which makes the exception worth stating: when something IS built down here,
 * inside all this, the contrast does the work by itself. Right angles in a
 * place that had none are the most alarming thing you can put underground.
 */
public final class Digging {
	private Digging() {}

	/** Never break the surface, and never open into the sky. */
	public static final int ROOF_CLEARANCE = 7;
	/** Nor drop out of the bottom of the world. */
	public static final int FLOOR_CLEARANCE = 10;

	/**
	 * Wander a passage in roughly a direction and return where it ended.
	 *
	 * The wobble is the point. A straight line between two rooms is a corridor
	 * and reads as architecture; a line that drifts a block off course and
	 * comes back reads as somebody digging by torchlight without a compass.
	 * Radius varies as it goes for the same reason — a passage of constant bore
	 * is a pipe.
	 */
	static BlockPos bore(ServerLevel level, BlockPos from, Vec3 heading,
	                     int length, double radius, RandomSource random) {
		return bore(level, from, heading, length, radius, random, false);
	}

	/**
	 * @param waymarked lay a floor and line the walls at intervals, so this
	 *                  passage reads as the one somebody meant you to take
	 */
	static BlockPos bore(ServerLevel level, BlockPos from, Vec3 heading,
	                     int length, double radius, RandomSource random, boolean waymarked) {
		Vec3 at = Vec3.atCenterOf(from);
		Vec3 course = heading.normalize();

		for (int step = 0; step < length; step++) {
			// Nudge the heading rather than replacing it, so it curves instead
			// of zig-zagging.
			course = course.add(
				(random.nextDouble() - 0.5) * 0.35,
				(random.nextDouble() - 0.5) * 0.18,
				(random.nextDouble() - 0.5) * 0.35).normalize();

			at = at.add(course.scale(1.4));
			at = clamp(level, at);
			hollow(level, BlockPos.containing(at),
				radius + (random.nextDouble() - 0.4) * 0.6, random);

			if (step > 0 && step % 7 == 0) {
				support(level, BlockPos.containing(at), random);
			}
			if (random.nextInt(9) == 0) {
				lamp(level, BlockPos.containing(at));
			}
			if (waymarked) {
				pave(level, BlockPos.containing(at), random);
				if (step > 0 && step % 9 == 0) {
					line(level, BlockPos.containing(at), radius, random);
				}
			}
		}
		return BlockPos.containing(at);
	}

	/**
	 * A floor somebody laid.
	 *
	 * The single most useful thing underground, and it took a playtest to see
	 * it. A player forty blocks down in the dark cannot tell a passage that was
	 * cut on purpose from a natural cave they have wandered into, so the way on
	 * and the way nowhere look identical and finding the route stops being
	 * tense and starts being tedious.
	 *
	 * A laid floor fixes it without a word: stone under your feet where the
	 * rock is bare everywhere else. It gives the player a rule they work out in
	 * about ten seconds and then trust — follow the paving — and it means the
	 * dead ends can stay genuinely indistinguishable at eye level while still
	 * being fair.
	 */
	static void pave(ServerLevel level, BlockPos at, RandomSource random) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos floor = groundUnder(level, at.offset(dx, 0, dz));
				if (floor == null || floor.getY() < at.getY() - 3) {
					continue;
				}
				int roll = random.nextInt(8);
				level.setBlock(floor, roll == 0 ? Blocks.GRAVEL.defaultBlockState()
					: roll < 3 ? Blocks.COBBLESTONE.defaultBlockState()
					: roll < 5 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
					: Blocks.STONE_BRICKS.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Brickwork around the passage, every so often.
	 *
	 * Reads as a doorway or a shored section without being either. Where the
	 * paving tells the player they are on the route, these tell them somebody
	 * was here often enough to make it permanent — and they mark the distance
	 * covered, which a featureless tunnel cannot.
	 */
	static void line(ServerLevel level, BlockPos at, double radius, RandomSource random) {
		int reach = (int)Math.ceil(radius) + 2;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dz = -reach; dz <= reach; dz++) {
					BlockPos pos = at.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || state.is(Blocks.BEDROCK) || !state.isSolid()) {
						continue;
					}
					if (!touchesAir(level, pos)) {
						continue;   // only the face of the tunnel, not the rock behind
					}
					level.setBlock(pos, random.nextInt(3) == 0
						? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
						: random.nextInt(2) == 0
							? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
	}

	private static boolean touchesAir(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).isAir()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Keep the dig inside the rock.
	 *
	 * A passage that wanders up into daylight ruins it twice over — the player
	 * walks out of a hole in a hillside, and the thing that was supposed to be
	 * buried turns out to have a back door.
	 */
	static Vec3 clamp(ServerLevel level, Vec3 at) {
		// Real ground, not the top of a tree — a dig under a forest would
		// otherwise think it had twenty more blocks of headroom than it has and
		// open into daylight.
		int surface = Ground.topOf(level, (int)at.x, (int)at.z);
		double ceiling = surface - ROOF_CLEARANCE;
		double floor = level.getMinY() + FLOOR_CLEARANCE;
		return new Vec3(at.x, Math.max(floor, Math.min(ceiling, at.y)), at.z);
	}

	/**
	 * Cut a rough ball of air, with the edges eaten into.
	 *
	 * The noise on the radius is what stops these reading as spheres when two
	 * of them overlap. Without it a chamber looks like it was made with a
	 * brush, which is exactly what it was made with.
	 */
	static void hollow(ServerLevel level, BlockPos centre, double radius,
	                           RandomSource random) {
		int reach = (int)Math.ceil(radius) + 1;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dz = -reach; dz <= reach; dz++) {
					// Squashed vertically: a hollow taller than it is wide
					// reads as a shaft, and these are passages.
					double d = Math.sqrt(dx * dx + (dy * dy) * 2.2 + dz * dz);
					if (d > radius - 0.35 + random.nextDouble() * 0.7) {
						continue;
					}
					BlockPos pos = centre.offset(dx, dy, dz);
					if (level.getBlockState(pos).is(Blocks.BEDROCK) || precious(level, pos)) {
						continue;
					}
					level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					seal(level, pos);
				}
			}
		}
	}

	/**
	 * Never carve away something that was put there on purpose.
	 *
	 * Insurance against an ordering mistake, and it is here because that
	 * mistake was already made twice: a passage bored out of a chamber AFTER
	 * the chest went in cut straight through it, and the contents fell on the
	 * floor as items and began quietly counting down to despawning. A player
	 * finds a book they were meant to read lying in the dirt, or does not find
	 * it at all.
	 *
	 * The real fix is to cut every passage before furnishing anything, and both
	 * callers do that now. This makes sure the next one cannot get it wrong
	 * without noticing.
	 */
	static boolean precious(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos) != null
			|| level.getBlockState(pos).is(Blocks.BOOKSHELF);
	}

	/**
	 * Nothing floods.
	 *
	 * Carving through an aquifer or a lava pocket turns the whole undercroft
	 * into a lake or a chimney, and a player who drowns walking into somebody's
	 * cellar is not having the experience this is for. Any fluid touching fresh
	 * air becomes stone, which is also what a person digging would have done.
	 */
	static void seal(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			BlockPos next = pos.relative(side);
			if (!level.getFluidState(next).isEmpty()) {
				level.setBlock(next, Blocks.STONE.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Somebody propped this up.
	 *
	 * The one architectural note allowed down here, and it is doing a specific
	 * job: props are evidence of a person. Rough stone alone could be a natural
	 * cave, and a natural cave is not frightening — a cave somebody cut and
	 * then shored up so they could keep cutting is.
	 */
	static void support(ServerLevel level, BlockPos at, RandomSource random) {
		BlockPos floor = groundUnder(level, at);
		if (floor == null) {
			return;
		}
		int height = 0;
		while (height < 5 && level.getBlockState(floor.above(height + 1)).isAir()) {
			height++;
		}
		if (height < 2) {
			return;
		}
		Direction across = random.nextBoolean() ? Direction.EAST : Direction.NORTH;
		for (int side = -1; side <= 1; side += 2) {
			BlockPos post = floor.relative(across, side);
			for (int y = 1; y <= height; y++) {
				if (level.getBlockState(post.above(y)).isAir()) {
					level.setBlock(post.above(y), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
				}
			}
			BlockPos beam = post.above(height + 1);
			if (level.getBlockState(beam).isAir()) {
				level.setBlock(beam, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
			}
		}
	}

	/** A red light on the wall, and never a torch. */
	static void lamp(ServerLevel level, BlockPos near) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos wall = near.relative(side);
			if (level.getBlockState(wall).isSolid() && level.getBlockState(near).isAir()) {
				level.setBlock(near, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, side.getOpposite()), 2);
				return;
			}
		}
	}

	/**
	 * The little that is down here at all.
	 *
	 * Cobwebs where they can hang off something, gravel underfoot in patches,
	 * and the occasional dripstone. All three do the same job: they say time
	 * has passed and nobody has swept.
	 */
	static void props(ServerLevel level, BlockPos centre, int count, RandomSource random) {
		for (int i = 0; i < count * 4; i++) {
			BlockPos at = centre.offset(
				random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			int roll = random.nextInt(6);
			if (roll < 2 && touchesSomething(level, at)) {
				level.setBlock(at, Blocks.COBWEB.defaultBlockState(), 2);
			} else if (roll < 4) {
				BlockPos floor = groundUnder(level, at);
				if (floor != null && !level.getBlockState(floor).is(Blocks.BEDROCK)) {
					level.setBlock(floor, random.nextBoolean()
						? Blocks.GRAVEL.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState(), 2);
				}
			} else if (roll == 4 && level.getBlockState(at.above()).isSolid()) {
				level.setBlock(at, Blocks.POINTED_DRIPSTONE.defaultBlockState()
					.setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN), 2);
			}
		}
	}

	/** The first solid block under a position, within a few blocks. */
	static @org.jspecify.annotations.Nullable BlockPos groundUnder(
			ServerLevel level, BlockPos from) {
		for (int down = 0; down <= 6; down++) {
			BlockPos pos = from.below(down);
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && state.isSolid()) {
				return pos;
			}
		}
		return null;
	}

	static boolean touchesSomething(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).isSolid()) {
				return true;
			}
		}
		return false;
	}
}
