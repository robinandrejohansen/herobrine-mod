package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * HOUSE FIVE. THE CHURCH — and it looks like one.
 *
 * It was a roofless ruin: pillars, an aisle, a chancel open to the sky. The
 * idea was good and the reading was not; nobody standing in it said "church".
 * So: a nave with stone walls six high and a pitched slate roof, arched
 * windows down both sides, a bell tower over the porch with a bell in it,
 * pews in two rows down to a rail, and the altar at the far end with the
 * candles still lit. Graves outside. One chest by the door.
 *
 * UNDER THE ALTAR IS WHERE HE LIVED. A hatch behind the altar, a shaft eleven
 * down, and a crypt: a vaulted room with a black bed in it, skulls, chains, a
 * chest, cobwebs, the priest's bowl. That is the story Addexio tells here, and
 * the room says it without a word. There are no signs.
 */
public final class Shrine {
	private Shrine() {}

	private static final int WIDTH = 13;
	private static final int LENGTH = 23;
	private static final int WALL = 6;
	private static final int TOWER_HALF = 2;
	private static final int TOWER_OUT = 4;
	private static final int TOWER_HEIGHT = 14;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos corner = new BlockPos(origin.getX() - WIDTH / 2,
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ() - LENGTH / 2);
		clearing(level, corner, random);
		platform(level, corner, random);
		nave(level, corner, random);
		roof(level, corner, random);
		tower(level, corner, random);
		inside(level, corner, random);
		chancel(level, corner, random);
		graves(level, corner, random);
		crypt(level, corner, random);
		belongings(level, corner, random);
		HerobrineMod.LOGGER.info("the church stands at [{}, {}, {}]",
			corner.getX(), corner.getY(), corner.getZ());
	}

	private static BlockPos at(BlockPos corner, int dx, int up, int dz) {
		return corner.offset(dx, up, dz);
	}

	private static void clearing(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = -6; dx < WIDTH + 6; dx++) {
			for (int dz = -6; dz < LENGTH + TOWER_OUT + 6; dz++) {
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
				BlockPos above = ground.above();
				if (!level.getBlockState(above).isAir() && !level.getBlockState(above).isSolid()) {
					level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				}
				boolean inside = dx >= 0 && dx < WIDTH && dz >= 0 && dz < LENGTH + TOWER_OUT;
				if (!inside && random.nextInt(3) == 0 && level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
					level.setBlock(ground, random.nextBoolean()
						? Blocks.PODZOL.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState(), 2);
				}
			}
		}
	}

	/** One flat floor at the corner's height; the ground under it filled with stone, the air over it cleared. */
	private static void platform(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = 0; dx < WIDTH; dx++) {
			for (int dz = 0; dz < LENGTH + TOWER_OUT; dz++) {
				boolean tower = dz >= LENGTH && Math.abs(dx - WIDTH / 2) <= TOWER_HALF;
				if (dz >= LENGTH && !tower) {
					continue;
				}
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				int ground = Ground.topOf(level, x, z);
				for (int y = ground; y < corner.getY() - 1; y++) {
					level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				level.setBlock(new BlockPos(x, corner.getY() - 1, z), paving(random, dx), 2);
				for (int up = 0; up < TOWER_HEIGHT + 4; up++) {
					BlockPos clear = new BlockPos(x, corner.getY() + up, z);
					if (!level.getBlockState(clear).isAir()) {
						level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/** Walls six high, arched windows every fourth block, gable ends up to the ridge. */
	private static void nave(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = 0; dx < WIDTH; dx++) {
			for (int dz = 0; dz < LENGTH; dz++) {
				boolean side = dx == 0 || dx == WIDTH - 1;
				boolean end = dz == 0 || dz == LENGTH - 1;
				if (!side && !end) {
					continue;
				}
				int ridge = WALL + (WIDTH / 2 - Math.abs(dx - WIDTH / 2));
				int height = end ? ridge : WALL;
				for (int up = 0; up < height; up++) {
					BlockPos here = at(corner, dx, up, dz);
					boolean window = side && dz % 4 == 2 && up >= 2 && up <= 4;
					boolean door = dz == LENGTH - 1 && Math.abs(dx - WIDTH / 2) <= 1 && up <= 2;
					boolean eastWindow = dz == 0 && Math.abs(dx - WIDTH / 2) <= 1 && up >= 2 && up <= 5;
					if (door) {
						level.setBlock(here, Blocks.AIR.defaultBlockState(), 2);
					} else if (window || eastWindow) {
						level.setBlock(here, Blocks.GLASS_PANE.defaultBlockState(), 2);
					} else {
						level.setBlock(here, stone(random), 2);
					}
				}
				if (side && dz % 4 == 2) {
					level.setBlock(at(corner, dx, 5, dz), Blocks.STONE_BRICK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, dx == 0 ? Direction.EAST : Direction.WEST)
						.setValue(BlockStateProperties.HALF, Half.TOP), 2);      // the arch over the window
				}
			}
		}
	}

	/** A pitched slate roof along the length, stairs stepping up to a tiled ridge, a chandelier under it. */
	private static void roof(ServerLevel level, BlockPos corner, RandomSource random) {
		int half = WIDTH / 2;
		for (int dz = 0; dz < LENGTH; dz++) {
			for (int dx = 0; dx < WIDTH; dx++) {
				int rise = half - Math.abs(dx - half);
				int up = WALL + rise;
				BlockPos here = at(corner, dx, up, dz);
				if (dx == half) {
					level.setBlock(here, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
					continue;
				}
				level.setBlock(here, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, dx < half ? Direction.EAST : Direction.WEST), 2);
				// the underside, so the roof reads as a roof from inside too
				if (rise > 0 && dz > 0 && dz < LENGTH - 1) {
					level.setBlock(here.below(), Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, dx < half ? Direction.WEST : Direction.EAST)
						.setValue(BlockStateProperties.HALF, Half.TOP), 2);
				}
			}
			if (dz % 6 == 3) {
				BlockPos hang = at(corner, half, WALL + half - 1, dz);
				level.setBlock(hang, Blocks.IRON_CHAIN.defaultBlockState(), 2);
				level.setBlock(hang.below(), Blocks.IRON_CHAIN.defaultBlockState(), 2);
				level.setBlock(hang.below(2), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
	}

	/** The bell tower over the porch: five wide, four deep, fourteen high, open at the top, a bell in it, a pointed roof. */
	private static void tower(ServerLevel level, BlockPos corner, RandomSource random) {
		int cx = WIDTH / 2;
		for (int dx = cx - TOWER_HALF; dx <= cx + TOWER_HALF; dx++) {
			for (int dz = LENGTH - 1; dz < LENGTH + TOWER_OUT; dz++) {
				boolean wall = dx == cx - TOWER_HALF || dx == cx + TOWER_HALF || dz == LENGTH + TOWER_OUT - 1;
				if (dz == LENGTH - 1) {
					wall = Math.abs(dx - cx) > 1;      // the nave's own wall, with the passage through it
				}
				for (int up = 0; up < TOWER_HEIGHT; up++) {
					BlockPos here = at(corner, dx, up, dz);
					if (!wall) {
						if (dz > LENGTH - 1) {
							level.setBlock(here, up == 3 && Math.abs(dx - cx) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
						}
						continue;
					}
					boolean door = dz == LENGTH + TOWER_OUT - 1 && Math.abs(dx - cx) <= 1 && up <= 2;
					boolean belfry = up >= TOWER_HEIGHT - 4 && up <= TOWER_HEIGHT - 2
						&& ((dz == LENGTH + TOWER_OUT - 1 && Math.abs(dx - cx) <= 1)
							|| ((dx == cx - TOWER_HALF || dx == cx + TOWER_HALF) && dz == LENGTH + 1));
					boolean slit = up == 6 && dz == LENGTH + TOWER_OUT - 1 && dx == cx;
					if (door || belfry) {
						level.setBlock(here, Blocks.AIR.defaultBlockState(), 2);
					} else if (slit) {
						level.setBlock(here, Blocks.GLASS_PANE.defaultBlockState(), 2);
					} else {
						level.setBlock(here, stone(random), 2);
					}
				}
			}
		}
		// belfry floor, the bell, and the pointed roof
		for (int dx = cx - TOWER_HALF + 1; dx <= cx + TOWER_HALF - 1; dx++) {
			for (int dz = LENGTH; dz < LENGTH + TOWER_OUT - 1; dz++) {
				level.setBlock(at(corner, dx, TOWER_HEIGHT - 5, dz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
			}
		}
		for (int dx = cx - TOWER_HALF; dx <= cx + TOWER_HALF; dx++) {
			for (int dz = LENGTH - 1; dz < LENGTH + TOWER_OUT; dz++) {
				level.setBlock(at(corner, dx, TOWER_HEIGHT, dz), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
			}
		}
		level.setBlock(at(corner, cx, TOWER_HEIGHT - 1, LENGTH + 1), Blocks.BELL.defaultBlockState()
			.setValue(BlockStateProperties.BELL_ATTACHMENT,
				net.minecraft.world.level.block.state.properties.BellAttachType.CEILING), 2);
		for (int ring = 1; ring <= TOWER_HALF; ring++) {
			for (int dx = cx - TOWER_HALF + ring; dx <= cx + TOWER_HALF - ring; dx++) {
				for (int dz = LENGTH - 1 + ring; dz < LENGTH + TOWER_OUT - ring; dz++) {
					level.setBlock(at(corner, dx, TOWER_HEIGHT + ring, dz), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
				}
			}
		}
		level.setBlock(at(corner, cx, TOWER_HEIGHT + TOWER_HALF + 1, LENGTH + 1), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
		for (int dx = cx - 1; dx <= cx + 1; dx++) {
			level.setBlock(at(corner, dx, -1, LENGTH + TOWER_OUT), Blocks.STONE_BRICK_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);      // the step up to the door
		}
	}

	/** Pews in two rows facing the altar, an aisle down the middle, wall lanterns, a rail before the chancel. */
	private static void inside(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dz = 8; dz <= LENGTH - 5; dz += 2) {
			for (int dx : new int[] {2, 3, 4, WIDTH - 5, WIDTH - 4, WIDTH - 3}) {
				if (random.nextInt(9) == 0) {
					continue;      // one taken away, or knocked over
				}
				level.setBlock(at(corner, dx, 0, dz), Blocks.DARK_OAK_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
			}
		}
		for (int dz = 4; dz < LENGTH - 2; dz += 6) {
			level.setBlock(at(corner, 1, 3, dz), Blocks.WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
			level.setBlock(at(corner, WIDTH - 2, 3, dz), Blocks.WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 2);
		}
		for (int dx = 2; dx < WIDTH - 2; dx++) {
			if (Math.abs(dx - WIDTH / 2) <= 1) {
				continue;      // the opening in the rail
			}
			level.setBlock(at(corner, dx, 0, 6), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
		}
		level.setBlock(at(corner, WIDTH - 4, 0, 5), Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		for (int i = 0; i < 6; i++) {
			BlockPos web = at(corner, 1 + random.nextInt(WIDTH - 2), 3 + random.nextInt(3), 1 + random.nextInt(LENGTH - 2));
			if (level.getBlockState(web).isAir() && random.nextBoolean()) {
				level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}
	}

	/** The chancel: raised a step, the altar of chiseled deepslate with the candles lit and a soul lantern over it. */
	private static void chancel(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = 1; dx < WIDTH - 1; dx++) {
			for (int dz = 1; dz <= 5; dz++) {
				level.setBlock(at(corner, dx, -1, dz), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
			}
		}
		for (int dx = 2; dx < WIDTH - 2; dx++) {
			level.setBlock(at(corner, dx, 0, 5), Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), 2);
		}
		BlockPos altar = at(corner, WIDTH / 2, 0, 3);
		level.setBlock(altar, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.west(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.east(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.above(), Blocks.CANDLE.defaultBlockState()
			.setValue(BlockStateProperties.CANDLES, 3).setValue(BlockStateProperties.LIT, true), 2);
		level.setBlock(altar.west().above(), Blocks.CANDLE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, true), 2);
		level.setBlock(altar.east().above(), Blocks.CANDLE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, true), 2);
		level.setBlock(altar.above(3), Blocks.IRON_CHAIN.defaultBlockState(), 2);
		level.setBlock(altar.above(2), Blocks.SOUL_LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
		for (int dx = -2; dx <= 2; dx += 4) {
			level.setBlock(altar.offset(dx, 0, -1), Blocks.WOOL
				.pick(random.nextBoolean() ? DyeColor.RED : DyeColor.BROWN).defaultBlockState(), 2);
		}
		level.setBlock(at(corner, WIDTH / 2, 0, 1), Blocks.CARPET.pick(DyeColor.RED).defaultBlockState(), 2);
	}

	private static void belongings(ServerLevel level, BlockPos corner, RandomSource random) {
		BlockPos chestAt = at(corner, 2, 0, LENGTH - 3);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
		if (!(level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest)) {
			return;
		}
		chest.setItem(0, Loot.tome(level.registryAccess(), random, 2));
		chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
		chest.setItem(3, new ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL));
		chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.BREAD, 9));
		Loot.scatter(chest, random, Loot.Tier.TOWER);
	}

	private static void graves(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int i = 0; i < 14; i++) {
			int dx = random.nextBoolean() ? -5 + random.nextInt(4) : WIDTH + 1 + random.nextInt(4);
			int dz = 3 + random.nextInt(LENGTH - 6);
			int x = corner.getX() + dx;
			int z = corner.getZ() + dz;
			int y = Ground.topOf(level, x, z) + 1;
			level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
			level.setBlock(new BlockPos(x, y - 1, z), Blocks.PODZOL.defaultBlockState(), 2);
			if (random.nextInt(3) == 0) {
				level.setBlock(new BlockPos(x, y + 1, z), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
			}
		}
	}

	/** Behind the altar, a hatch; eleven down, the room he lived in. */
	private static void crypt(ServerLevel level, BlockPos corner, RandomSource random) {
		BlockPos under = at(corner, WIDTH / 2, 0, 1);
		BlockPos landing = Descent.shaft(level, under, 11, stone(random));
		Descent.hatch(level, under.above(), Direction.SOUTH);
		room(level, landing.below(), random);
	}

	/** A vaulted room seven by nine, three high: a black bed, skulls, chains, a chest, the bowl he was fed from. */
	private static void room(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -3; dz <= 5; dz++) {
				for (int up = 0; up <= 4; up++) {
					BlockPos here = floor.offset(dx, up, dz);
					boolean shell = Math.abs(dx) == 4 || dz == -3 || dz == 5 || up == 0 || up == 4;
					boolean vault = up == 4 && Math.abs(dx) <= 1 && dz > -3 && dz < 5;
					if (dx == 0 && dz == 0 && up > 0) {
						continue;      // the shaft comes down here
					}
					if (vault) {
						level.setBlock(here, Blocks.AIR.defaultBlockState(), 2);
						level.setBlock(here.above(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					} else if (shell) {
						level.setBlock(here, up == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					} else {
						level.setBlock(here, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		BlockState bed = Blocks.BED.pick(DyeColor.BLACK).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		level.setBlock(floor.offset(-2, 1, 3), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
		level.setBlock(floor.offset(-2, 1, 4), bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);
		level.setBlock(floor.offset(2, 1, 4), Blocks.CAULDRON.defaultBlockState(), 2);
		level.setBlock(floor.offset(3, 1, 4), Blocks.SKELETON_SKULL.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 8), 2);
		level.setBlock(floor.offset(-3, 1, -2), Blocks.SKELETON_SKULL.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 2), 2);
		BlockPos chestAt = floor.offset(3, 1, -2);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			chest.setItem(0, Loot.tome(level.registryAccess(), random, 3));
			Loot.scatter(chest, random, Loot.Tier.GAOL);
		}
		for (int dz = -1; dz <= 3; dz += 4) {
			BlockPos hang = floor.offset(0, 4, dz);
			level.setBlock(hang, Blocks.IRON_CHAIN.defaultBlockState(), 2);
			level.setBlock(hang.below(), Blocks.SOUL_LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
		for (int i = 0; i < 5; i++) {
			BlockPos web = floor.offset(random.nextInt(7) - 3, 1 + random.nextInt(3), random.nextInt(8) - 2);
			if (level.getBlockState(web).isAir()) {
				level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}
	}

	private static BlockState paving(RandomSource random, int dx) {
		if (dx == WIDTH / 2) {
			return Blocks.POLISHED_ANDESITE.defaultBlockState();
		}
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.ANDESITE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	/** Stone brick, old: one in five mossy, one in eight cracked. */
	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(40);
		if (roll < 8) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 13) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
