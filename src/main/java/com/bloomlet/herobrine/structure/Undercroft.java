package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * What is actually under the house.
 *
 * A farmhouse with a hole in the floor, and rather more under it than a farm
 * needed. A cellar is storage; this is not storage.
 *
 * Kept small on purpose. It is the first of five and each is meant to be less
 * like somewhere a person lived than the last, so a house whose cellar already
 * ran to three chambers would leave the later ones nowhere to go. One chamber,
 * and a passage that gives up.
 *
 * Carved rather than built, and that distinction is the whole brief. Rooms have
 * corners, courses and right angles, and every one of those says somebody was
 * making a place to be. These are hollows: irregular, uneven-ceilinged, wider
 * in some places than in others for no reason, following a line that wanders
 * because whoever cut it was not working to a plan. The moment a player can
 * read a rectangle down here it stops being a dig and becomes a basement.
 *
 * Almost nothing is put in it. One chest, a few props, and long stretches of
 * nothing at all. The emptiness IS the content —
 * the question the whole thing exists to ask is why a family with four names in
 * a ledger needed this, and any answer left lying around makes the question
 * smaller.
 *
 * There is no ore, no rail, no branch pattern and no dead-end alcove anybody
 * would recognise as prospecting. It is not a mine. Nobody was looking for
 * anything down here; they were going somewhere.
 */
public final class Undercroft {
	private Undercroft() {}

	/** Never break the surface, and never open into the sky. */
	private static final int ROOF_CLEARANCE = 7;
	/** Nor drop out of the bottom of the world. */
	private static final int FLOOR_CLEARANCE = 10;

	/**
	 * Dig it, starting from the cellar the house already has.
	 *
	 * Deliberately modest. This is the FIRST house and it has to leave room for
	 * the ones after it — a farmhouse whose cellar already runs to three
	 * chambers has nowhere left to escalate to, and the whole point of the five
	 * is that each one is less like somewhere a person lived than the last. So:
	 * one chamber, and then a passage that gives up. Enough to say he was
	 * digging, and nowhere near enough to say what for.
	 *
	 * @param mouth the cellar floor position the descent leaves from
	 */
	public static void dig(ServerLevel level, BlockPos mouth, RandomSource random) {
		// Down and away from the house, winding and tight. A squeeze before it
		// opens out, so the player has to commit before they can see whether
		// there is anything worth committing to.
		BlockPos chamber = bore(level, mouth, new Vec3(0.15, -0.5, 1.0), 18, 1.5, random);

		hollow(level, chamber, 3.6, random);
		crate(level, chamber.offset(2, 0, 0), HouseBooks.brother(), random);
		props(level, chamber, 4, random);

		// And on, deeper, until it simply stops. No wall, no door, no chamber —
		// the pick marks end mid-stone. Whatever he was going towards, he did
		// not reach it from this end, and nothing down here says what it was.
		BlockPos end = bore(level, chamber, new Vec3(0.85, -0.45, -0.3), 22, 1.4, random);
		props(level, end, 2, random);

		HerobrineMod.LOGGER.info("undercroft dug, ends at [{}, {}, {}]",
			end.getX(), end.getY(), end.getZ());
	}

	/**
	 * Wander a passage in roughly a direction and return where it ended.
	 *
	 * The wobble is the point. A straight line between two rooms is a corridor
	 * and reads as architecture; a line that drifts a block off course and
	 * comes back reads as somebody digging by torchlight without a compass.
	 * Radius varies as it goes for the same reason — a passage of constant bore
	 * is a pipe.
	 */
	private static BlockPos bore(ServerLevel level, BlockPos from, Vec3 heading,
	                             int length, double radius, RandomSource random) {
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
		}
		return BlockPos.containing(at);
	}

	/**
	 * Keep the dig inside the rock.
	 *
	 * A passage that wanders up into daylight ruins it twice over — the player
	 * walks out of a hole in a hillside, and the thing that was supposed to be
	 * buried turns out to have a back door.
	 */
	private static Vec3 clamp(ServerLevel level, Vec3 at) {
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
	private static void hollow(ServerLevel level, BlockPos centre, double radius,
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
					if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
						continue;
					}
					level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					seal(level, pos);
				}
			}
		}
	}

	/**
	 * Nothing floods.
	 *
	 * Carving through an aquifer or a lava pocket turns the whole undercroft
	 * into a lake or a chimney, and a player who drowns walking into somebody's
	 * cellar is not having the experience this is for. Any fluid touching fresh
	 * air becomes stone, which is also what a person digging would have done.
	 */
	private static void seal(ServerLevel level, BlockPos pos) {
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
	private static void support(ServerLevel level, BlockPos at, RandomSource random) {
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
	private static void lamp(ServerLevel level, BlockPos near) {
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
	private static void props(ServerLevel level, BlockPos centre, int count, RandomSource random) {
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

	private static void crate(ServerLevel level, BlockPos near,
	                          @org.jspecify.annotations.Nullable ItemStack book,
	                          RandomSource random) {
		BlockPos floor = groundUnder(level, near);
		if (floor == null) {
			return;
		}
		BlockPos at = floor.above();
		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			if (book != null) {
				chest.setItem(0, book);
			}
			chest.setItem(1, new ItemStack(Items.IRON_PICKAXE));
			Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
		}
	}

	/** The first solid block under a position, within a few blocks. */
	private static @org.jspecify.annotations.Nullable BlockPos groundUnder(
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

	private static boolean touchesSomething(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).isSolid()) {
				return true;
			}
		}
		return false;
	}
}
