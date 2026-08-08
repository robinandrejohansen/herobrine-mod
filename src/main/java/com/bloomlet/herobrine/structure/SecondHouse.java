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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * HOUSE TWO. The same house, and he has buried it.
 *
 * The single most important thing about this building is that a player who has
 * been in the homestead recognises the FLOOR PLAN. Same footprint, same room
 * sizes, the door in the same wall — and every window bricked up from the
 * inside, the whole thing sunk to its eaves in the earth, and a lamp burning in
 * a room with nothing to see.
 *
 * That recognition is the entire horror of it and it cannot be written on a
 * sign. He did not move somewhere stranger. He rebuilt the same house and then
 * started taking things away from it: first the light, then the view out, then
 * the ground level. A player works out on their own that these are in order,
 * and that the order is a person deteriorating.
 *
 * Sunk rather than underground. The roof line still shows, so it reads as a
 * house that has been PUT here rather than a bunker that was always a bunker —
 * you find a roof in a field, and then find there is no door in it at ground
 * level, and have to dig down to the step.
 *
 * Still furnished. A bed, a table, a chest with food in it. Somebody was living
 * here, on purpose, in the dark, for a long time.
 */
public final class SecondHouse {
	private SecondHouse() {}

	public static final int WIDTH = 11;
	public static final int DEPTH = 9;
	/** How far the whole thing sits below the surrounding ground. */
	private static final int SUNK = 4;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos base = origin.below(SUNK);

		shell(level, base, random);
		sinking(level, base, random);
		inside(level, base, random);
		stairwell(level, base, random);

		HerobrineMod.LOGGER.info("the second house went up at [{}, {}, {}]",
			base.getX(), base.getY(), base.getZ());
	}

	/**
	 * The homestead's plan, in the homestead's materials, with no openings.
	 *
	 * The windows are not missing — they are BRICKED. Each one is a patch of
	 * cobble in an otherwise plank wall, obvious from inside and invisible from
	 * out, which says somebody stood in this room and filled them in rather
	 * than a builder who simply never cut them.
	 */
	private static void shell(ServerLevel level, BlockPos base, RandomSource random) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				for (int y = 0; y <= 5; y++) {
					BlockPos at = base.offset(x, y, z);
					boolean wall = x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1;
					boolean corner = (x == 0 || x == WIDTH - 1) && (z == 0 || z == DEPTH - 1);

					if (y == 0) {
						level.setBlock(at, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
					} else if (y == 5) {
						level.setBlock(at, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
					} else if (corner) {
						level.setBlock(at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
							.setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 2);
					} else if (wall) {
						// Where a window would have been.
						boolean blinded = y == 2 && (x % 4 == 2 || z % 4 == 2);
						level.setBlock(at, blinded
							? bricked(random)
							: Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
					} else {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/** Earth back over the top of it, leaving only the roof showing. */
	private static void sinking(ServerLevel level, BlockPos base, RandomSource random) {
		for (int x = -2; x < WIDTH + 2; x++) {
			for (int z = -2; z < DEPTH + 2; z++) {
				boolean over = x >= 0 && x < WIDTH && z >= 0 && z < DEPTH;
				for (int y = 1; y <= SUNK; y++) {
					BlockPos at = base.offset(x, y, z);
					if (over) {
						continue;   // the house is in the way, which is the point
					}
					if (level.getBlockState(at).isAir()) {
						level.setBlock(at, random.nextInt(6) == 0
							? Blocks.COARSE_DIRT.defaultBlockState()
							: Blocks.DIRT.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/**
	 * Furnished, and lit, in a room with nothing to look at.
	 *
	 * The lantern is the detail that does the work. A dark buried house is
	 * somewhere abandoned; a buried house with the light still on is somewhere
	 * somebody chose to sit.
	 */
	private static void inside(ServerLevel level, BlockPos base, RandomSource random) {
		BlockPos floor = base.above();

		BlockState bed = Blocks.BED.pick(DyeColor.RED).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		level.setBlock(floor.offset(2, 0, 2),
			bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
		level.setBlock(floor.offset(2, 0, 3),
			bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);

		level.setBlock(floor.offset(7, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
		level.setBlock(floor.offset(8, 0, 3), Blocks.FURNACE.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 2);
		level.setBlock(floor.offset(5, 3, 4), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);

		BlockPos chestAt = floor.offset(7, 0, 6);
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

		for (int i = 0; i < 7; i++) {
			BlockPos web = floor.offset(1 + random.nextInt(WIDTH - 2), random.nextInt(3),
				1 + random.nextInt(DEPTH - 2));
			if (level.getBlockState(web).isAir() && random.nextBoolean()) {
				level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * The way in, and there isn't one at ground level.
	 *
	 * A cut in the earth down to the door, open to the sky. The player has to
	 * walk down into it, which is a small physical commitment and does more for
	 * the mood than any amount of decoration — you go DOWN to this house.
	 */
	private static void stairwell(ServerLevel level, BlockPos base, RandomSource random) {
		int doorX = WIDTH / 2;
		BlockPos mouth = base.offset(doorX, 1, DEPTH - 1);

		BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		level.setBlock(mouth, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(mouth.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);

		for (int out = 1; out <= SUNK + 2; out++) {
			int lift = Math.min(SUNK, out - 1);
			for (int side = -1; side <= 1; side++) {
				BlockPos step = base.offset(doorX + side, lift, DEPTH - 1 + out);
				level.setBlock(step, Blocks.COBBLESTONE.defaultBlockState(), 2);
				for (int up = 1; up <= 3; up++) {
					level.setBlock(step.above(up), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private static BlockState bricked(RandomSource random) {
		return random.nextInt(3) == 0
			? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
			: Blocks.COBBLESTONE.defaultBlockState();
	}
}
