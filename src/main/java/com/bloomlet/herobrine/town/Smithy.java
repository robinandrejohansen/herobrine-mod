package com.bloomlet.herobrine.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The forge, and it is open to the street.
 *
 * A smithy is the one building in a settlement that has to be seen working. The
 * whole front wall is missing — a timber lintel on two posts and nothing under
 * it — because a forge that shuts its door is a forge nobody buys from, and
 * because the glow of the fire reaching the lane at night is the cheapest way
 * to make a town feel inhabited from a hundred blocks away.
 *
 * It is also the only building here made mostly of STONE. Every house in the
 * town is spruce framing over plaster, so a slab-and-brick workshop with a
 * chimney reads as different at a glance and for a reason a player can work out
 * without being told: this is the building that is on fire all day.
 *
 * The chimney is the tallest thing in the town after the church spire, which is
 * a small deliberate lie about scale — it makes the skyline read as a place
 * with industry rather than a ring of cottages.
 */
public final class Smithy {
	private Smithy() {}

	public static final int WIDTH = 11;
	public static final int DEPTH = 9;

	private static final String[] GROUND = {
		"           ",
		" ######### ",
		" ######### ",
		" ######### ",
		" ######### ",
		" ######### ",
		" ######### ",
		" ######### ",
		"           ",
	};

	/**
	 * Floor level. The front row is deliberately almost empty — that gap is
	 * the shopfront, and everything the player can see from the lane is
	 * arranged to be looked at through it.
	 */
	private static final String[] WORKING = {
		"           ",
		" LSSFSSSSL ",
		" S  a   BS ",
		" S       S ",
		" Sv    gwS ",
		" S   n   S ",
		" Sk      S ",
		" L~~~~~~~L ",
		"           ",
	};

	private static final String[] WAIST = {
		"           ",
		" LSSCSSSSL ",
		" S       S ",
		" S       S ",
		" S       S ",
		" S       S ",
		" S       S ",
		" L     r L ",
		"           ",
	};

	private static final String[] EAVES = {
		"           ",
		" LSSCSSSSL ",
		" S       S ",
		" S       S ",
		" S       S ",
		" S       S ",
		" S       S ",
		" LHHHHHHHL ",
		"           ",
	};

	private static final String[][] LAYERS = { GROUND, WORKING, WAIST, EAVES };

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		if (!Blueprint.place(level, corner, LAYERS, facing,
				(l, at, c, dir, mx, mz) -> set(l, at, c, dir, mx, mz, random))) {
			return false;
		}
		roof(level, corner, facing, random);
		chimney(level, corner, facing, random);
		return true;
	}

	private static void set(ServerLevel level, BlockPos at, char c, Direction facing,
	                        int x, int z, RandomSource random) {
		switch (c) {
			case '#' -> Blueprint.put(level, at, Blocks.COBBLESTONE.defaultBlockState());
			case 'S' -> Blueprint.put(level, at, wall(random));
			case 'L' -> Blueprint.put(level, at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
			case 'H' -> Blueprint.put(level, at, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
			// The open front: a lintel across, and air beneath it.
			case '~' -> Blueprint.put(level, at, Blocks.AIR.defaultBlockState());
			// The fire itself, and it faces out so the light reaches the lane.
			case 'F' -> Blueprint.put(level, at, Blocks.BLAST_FURNACE.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.SOUTH, facing))
				.setValue(BlockStateProperties.LIT, true));
			case 'C' -> Blueprint.put(level, at, Blocks.BRICKS.defaultBlockState());
			case 'a' -> Blueprint.put(level, at, Blocks.ANVIL.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.EAST, facing)));
			case 'g' -> Blueprint.put(level, at, Blocks.GRINDSTONE.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.SOUTH, facing)));
			case 'w' -> Blueprint.put(level, at, Blocks.SMITHING_TABLE.defaultBlockState());
			case 'B' -> Blueprint.put(level, at, Blocks.BARREL.defaultBlockState());
			case 'k' -> Blueprint.put(level, at, Blocks.CAULDRON.defaultBlockState());
			// The quench trough. Water in a workshop is not decoration — it is
			// the second most important thing in the room after the fire.
			case 'v' -> Blueprint.put(level, at, Blocks.WATER_CAULDRON.defaultBlockState()
				.setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
			case 'n' -> Blueprint.put(level, at, Blocks.STONECUTTER.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.SOUTH, facing)));
			case 'r' -> Blueprint.put(level, at, Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));
			default -> { }
		}
	}

	/**
	 * A shallow lean-to rather than a gable.
	 *
	 * The houses all have steep spruce gables. This one slopes one way and
	 * stops, which is what a workshop tacked onto a plot actually looks like
	 * and keeps it from reading as somebody's home with the front wall missing.
	 */
	private static void roof(ServerLevel level, BlockPos corner, Direction facing,
	                         RandomSource random) {
		for (int z = 0; z < DEPTH; z++) {
			int lift = z / 3;
			for (int x = 0; x < WIDTH; x++) {
				BlockPos at = corner.offset(
					Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 4 + lift,
					Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
				boolean edge = z % 3 == 0 && z > 0;
				Blueprint.put(level, at, edge
					? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING,
							Blueprint.turned(Direction.NORTH, facing))
					: Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
		}
	}

	/** Up past the ridge, because it has to be seen over the roofs. */
	private static void chimney(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		int x = 3;
		int z = 1;
		for (int up = 4; up <= 10; up++) {
			BlockPos at = corner.offset(
				Blueprint.spinX(x, z, WIDTH, DEPTH, facing), up,
				Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
			Blueprint.put(level, at, up >= 9
				? Blocks.BRICK_SLAB.defaultBlockState()
				: Blocks.BRICKS.defaultBlockState());
		}
		// Smoke, permanently. A cold chimney on a working forge is a detail
		// nobody notices and everybody feels.
		BlockPos vent = corner.offset(
			Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 8,
			Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
		Blueprint.put(level, vent, Blocks.CAMPFIRE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, true));
	}

	private static BlockState wall(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
