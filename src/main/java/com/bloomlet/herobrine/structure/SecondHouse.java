package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * HOUSE TWO. The tower, and everything under it.
 *
 * A round stone tower standing on its own in open country, with a stair
 * spiralling up the OUTSIDE of it — no rail, no roof over the steps, nothing
 * between you and the drop. It is the tallest thing for a thousand blocks and
 * it is visible long before you reach it, which is the entire reason it is a
 * tower: everything else in this sequence has to be stumbled on, and this one
 * calls you across a valley.
 *
 * AND THERE IS ALMOST NOTHING IN IT. Four floors of empty room and a chair at
 * the top facing a window. The climb is the content. A player spends two
 * minutes going up the outside of a building in the wind expecting a room at
 * the top, and what is at the top is somewhere to sit and look at the horizon,
 * which tells them exactly what he was doing here and takes one block to say.
 *
 * The house is UNDERNEATH. Down a well in the floor, the same farmhouse plan as
 * the homestead — same footprint, same rooms, door in the same wall — buried,
 * windowless, lit, and lived in. That is the horror the whole building is
 * arranged around: he did not stop living in a house. He put the house
 * underground and built somewhere to watch from on top of it.
 *
 * The old version of this was a sunken box with bricked windows and the player
 * saw nothing from outside but a roof in a field. Same idea, and it read as
 * rubble. This one is the same idea with a reason to walk toward it.
 */
public final class SecondHouse {
	private SecondHouse() {}

	private static final int TOWER_RADIUS = 4;
	private static final int TOWER_HEIGHT = 26;
	/** How far under the tower's floor the buried house sits. */
	private static final int CELLAR_DROP = 12;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos base = new BlockPos(origin.getX(),
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ());

		tower(level, base, random);
		outsideStair(level, base, random);
		crown(level, base, random);

		// Down the middle of it, and then the house.
		BlockPos wellTop = base.offset(-2, 0, -2);
		BlockPos landing = Descent.stair(level, wellTop, CELLAR_DROP, lining(random), random);
		buried(level, landing.below(2), random);

		grounds(level, base, random);

		HerobrineMod.LOGGER.info("the tower went up at [{}, {}, {}]",
			base.getX(), base.getY(), base.getZ());
	}

	/**
	 * A round tower, hollow, with a window on every third course.
	 *
	 * Round rather than square because a square tower is a keep and this is not
	 * defending anything. It is also the only round building in the mod, which
	 * does more to make it feel deliberate than any amount of detailing.
	 */
	private static void tower(ServerLevel level, BlockPos base, RandomSource random) {
		for (int up = 0; up < TOWER_HEIGHT; up++) {
			for (int dx = -TOWER_RADIUS; dx <= TOWER_RADIUS; dx++) {
				for (int dz = -TOWER_RADIUS; dz <= TOWER_RADIUS; dz++) {
					double reach = Math.hypot(dx, dz);
					if (reach > TOWER_RADIUS + 0.5) {
						continue;
					}
					BlockPos at = base.offset(dx, up, dz);
					boolean shell = reach > TOWER_RADIUS - 0.6;

					if (!shell) {
						// Floors every six courses, so it is storeys rather than
						// a chimney. Left mostly empty on purpose.
						boolean deck = up % 6 == 0 && up > 0;
						level.setBlock(at, deck
							? Blocks.SPRUCE_PLANKS.defaultBlockState()
							: Blocks.CAVE_AIR.defaultBlockState(), 2);
						continue;
					}
					// A slit on every third course, facing out. Narrow, because
					// he was looking rather than living.
					boolean slit = up % 3 == 1 && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)
						&& random.nextInt(3) == 0;
					level.setBlock(at, slit
						? Blocks.CAVE_AIR.defaultBlockState()
						: lining(random), 2);
				}
			}
		}
	}

	/**
	 * The stair, and it is on the OUTSIDE.
	 *
	 * No rail and no roof. An internal spiral would be safe, ordinary, and
	 * completely forgettable; going up the outside of a tower in the open is
	 * the thing a player will describe to somebody afterwards, and it costs the
	 * same number of blocks.
	 *
	 * Each tread is a stair block facing along the climb, so it looks like
	 * masonry corbelled out of the wall rather than a ribbon stuck to it.
	 */
	private static void outsideStair(ServerLevel level, BlockPos base, RandomSource random) {
		double turns = 3.2;
		int steps = TOWER_HEIGHT - 2;

		for (int i = 0; i < steps; i++) {
			double angle = (i / (double)steps) * turns * Math.PI * 2.0;
			double out = TOWER_RADIUS + 1;
			int x = base.getX() + (int)Math.round(Math.cos(angle) * out);
			int z = base.getZ() + (int)Math.round(Math.sin(angle) * out);
			BlockPos at = new BlockPos(x, base.getY() + i, z);

			// The tread, and a bracket under it so it is held up by something.
			Direction along = Direction.getApproximateNearest(
				-Math.sin(angle), 0.0, Math.cos(angle));
			level.setBlock(at, Blocks.COBBLESTONE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, along), 2);
			level.setBlock(at.below(), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
			for (int clear = 1; clear <= 2; clear++) {
				if (level.getBlockState(at.above(clear)).isAir()) {
					continue;
				}
				level.setBlock(at.above(clear), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
			// A doorway through the wall wherever the stair passes a floor, so
			// the climb and the inside are actually one building.
			if (i % 6 == 5) {
				pierce(level, base, at, angle);
			}
			if (i % 5 == 0) {
				BlockPos hook = at.above(2);
				if (level.getBlockState(hook).isAir()) {
					level.setBlock(hook, Blocks.LANTERN.defaultBlockState()
						.setValue(BlockStateProperties.HANGING, true), 2);
				}
			}
		}
	}

	/** A hole through the shell where the outside stair meets a floor. */
	private static void pierce(ServerLevel level, BlockPos base, BlockPos at, double angle) {
		for (int step = 1; step <= 2; step++) {
			int x = at.getX() - (int)Math.round(Math.cos(angle) * step);
			int z = at.getZ() - (int)Math.round(Math.sin(angle) * step);
			for (int up = 0; up <= 1; up++) {
				level.setBlock(new BlockPos(x, at.getY() + up, z),
					Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * The top: a chair, facing out, and nothing else at all.
	 *
	 * The whole tower is an argument that ends here. Two minutes of climbing
	 * for a seat and a view is not an anticlimax if the seat is the point —
	 * somebody sat up here, above the trees, watching, for a very long time,
	 * and the emptiness around the chair is what says so.
	 */
	private static void crown(ServerLevel level, BlockPos base, RandomSource random) {
		int top = base.getY() + TOWER_HEIGHT - 1;

		for (int dx = -TOWER_RADIUS; dx <= TOWER_RADIUS; dx++) {
			for (int dz = -TOWER_RADIUS; dz <= TOWER_RADIUS; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > TOWER_RADIUS + 0.5) {
					continue;
				}
				BlockPos at = new BlockPos(base.getX() + dx, top, base.getZ() + dz);
				// Open to the sky, with a battlement rather than a roof.
				level.setBlock(at, reach > TOWER_RADIUS - 0.6
					? Blocks.COBBLESTONE_WALL.defaultBlockState()
					: Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
			}
		}

		BlockPos chair = new BlockPos(base.getX(), top + 1, base.getZ() + 2);
		level.setBlock(chair, Blocks.SPRUCE_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		level.setBlock(chair.north(2), Blocks.LANTERN.defaultBlockState(), 2);
	}

	/**
	 * The homestead's plan, buried under the tower, with no windows.
	 *
	 * The recognition is the point and it is why the footprint is copied rather
	 * than reinvented: a player who has been in the first house knows this room
	 * before they have crossed it, and knowing it is what makes the absence of
	 * windows land.
	 */
	private static void buried(ServerLevel level, BlockPos floor, RandomSource random) {
		int w = 11;
		int d = 9;
		BlockPos corner = floor.offset(-w / 2, 0, -1);

		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				for (int y = 0; y <= 5; y++) {
					BlockPos at = corner.offset(x, y, z);
					boolean wall = x == 0 || x == w - 1 || z == 0 || z == d - 1;
					boolean post = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);

					if (y == 0) {
						level.setBlock(at, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
					} else if (y == 5) {
						level.setBlock(at, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
					} else if (post) {
						level.setBlock(at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
							.setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 2);
					} else if (wall) {
						// Where a window would have been, and is not.
						boolean blinded = y == 2 && (x % 4 == 2 || z % 4 == 2);
						level.setBlock(at, blinded
							? Blocks.COBBLESTONE.defaultBlockState()
							: Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
					} else {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		// The way in from the stair well.
		for (int up = 1; up <= 2; up++) {
			level.setBlock(corner.offset(w / 2, up, 0), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}

		BlockPos in = corner.above();
		BlockState bed = Blocks.BED.pick(DyeColor.RED).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		level.setBlock(in.offset(2, 0, 2),
			bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
		level.setBlock(in.offset(2, 0, 3),
			bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);

		level.setBlock(in.offset(7, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
		level.setBlock(in.offset(8, 0, 3), Blocks.FURNACE.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 2);
		level.setBlock(in.offset(5, 3, 4), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);

		BlockPos chestAt = in.offset(7, 0, 6);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			ItemStack book = HouseBooks.buried();
			if (book != null) {
				chest.setItem(0, book);
			}
			chest.setItem(1, new ItemStack(Items.TORCH, 12));
			Loot.scatter(chest, random, Loot.Tier.LARDER);
		}

		// And a passage out of the buried house that goes somewhere.
		BlockPos out = in.offset(w - 2, 1, d / 2);
		Digging.bore(level, out, new Vec3(1.0, -0.25, 0.2), 26, 1.5, random);
	}

	/**
	 * The ground around it, because a tower alone in a field is a model.
	 *
	 * A ring of cleared earth, a broken outbuilding, and a path worn from the
	 * door to nowhere in particular. None of it is interactive and all of it is
	 * there so the tower has been LIVED beside rather than dropped.
	 */
	private static void grounds(ServerLevel level, BlockPos base, RandomSource random) {
		for (int dx = -12; dx <= 12; dx++) {
			for (int dz = -12; dz <= 12; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach < TOWER_RADIUS + 1 || reach > 11) {
					continue;
				}
				if (random.nextInt(3) != 0) {
					continue;
				}
				int x = base.getX() + dx;
				int z = base.getZ() + dz;
				BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
				level.setBlock(ground, random.nextInt(3) == 0
					? Blocks.COARSE_DIRT.defaultBlockState()
					: Blocks.PODZOL.defaultBlockState(), 2);
			}
		}

		// A shed that has fallen in, eight blocks off.
		BlockPos shed = base.offset(9, 0, -4);
		for (int dx = 0; dx < 5; dx++) {
			for (int dz = 0; dz < 4; dz++) {
				int x = shed.getX() + dx;
				int z = shed.getZ() + dz;
				int y = Ground.topOf(level, x, z) + 1;
				boolean wall = dx == 0 || dx == 4 || dz == 0 || dz == 3;
				if (!wall) {
					continue;
				}
				int height = random.nextInt(3);   // most of it is gone
				for (int up = 0; up < height; up++) {
					level.setBlock(new BlockPos(x, y + up, z),
						Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
				}
			}
		}
	}

	private static BlockState lining(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 4) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 10) {
			return Blocks.STONE_BRICKS.defaultBlockState();
		}
		return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
	}
}
