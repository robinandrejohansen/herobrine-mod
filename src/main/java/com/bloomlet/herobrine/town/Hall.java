package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.structure.Loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Where the town eats.
 *
 * The one building everybody uses and nobody owns, which is why a settlement
 * needs it and why it should be the biggest thing in the place after the
 * church. Two long tables, a hearth at the head of the room, a counter, and
 * rooms upstairs for anybody who came in off the road.
 *
 * THE MIDDLE IS OPEN TO THE ROOF. The upper floor is a gallery round a well
 * rather than a ceiling, so the hall is two storeys high where people sit and
 * one where they sleep. That single hole is what stops this reading as a large
 * house: houses have ceilings because heat is expensive, and a room that spends
 * its warmth on height is a room built to hold a crowd.
 *
 * Same timber framing, same roof, same shutters as the houses. Deliberately —
 * a hall in a different style is a hall somebody imported, and this one was put
 * up by the same people who built their own homes.
 */
public final class Hall {
	private Hall() {}

	public static final int WIDTH = 15;
	public static final int DEPTH = 13;

	private static final int EAVE = 8;
	private static final int RIDGE_Z = 6;

	private static final String[] GROUND = {
		"               ",
		" ############# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" #...........# ",
		" ############# ",
		"               ",
	};

	private static final String[] FLOOR_ONE = {
		"               ",
		" LWWWWWWWWWWWL ",
		" WA   #Q# NNNW ",
		" W          AW ",
		" WPhTj   hTjAW ",
		" W hTj   hTj W ",
		" W hTj   hTj W ",
		" W hTj   hTjPW ",
		" W           W ",
		" W          CW ",
		" Ws          W ",
		" LWWWWDDWWWWWL ",
		"               ",
	};

	private static final String[] FLOOR_TWO = {
		"               ",
		" LWggWWWWWggWL ",
		" W        nnnW ",
		" W           W ",
		" g           g ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" gs          g ",
		" W           W ",
		// Air over both door columns. FLOOR_ONE's door() already placed its
		// top half here, and anything solid in this row destroys it.
		" Lbggb  WbggbL ",
		"               ",
	};

	private static final String[] FLOOR_THREE = {
		"               ",
		" LHHHHHHHHHHHL ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" Hs          H ",
		" H           H ",
		" H           H ",
		" LHHHHHHHHHHHL ",
		"               ",
	};

	private static final String[] DECK = {
		"               ",
		" HHHHHHHHHHHHH ",
		" H___________H ",
		" H___________H ",
		" H___     ___H ",
		" H___     ___H ",
		" H___     ___H ",
		" H___     ___H ",
		" Ho__     ___H ",
		" Ho__________H ",
		" Ho__________H ",
		" HHHHHHHHHHHHH ",
		"               ",
	};

	private static final String[] UPPER_ONE = {
		"               ",
		" LWKWWWWWWWKWL ",
		" Wd         dW ",
		" W  rWrrrWr  W ",
		" W  r     r  W ",
		" W  r     r  W ",
		" W  r     r  W ",
		" W  r     r  W ",
		" W  r     r  W ",
		" W  rrrqrrr  W ",
		" W           W ",
		" LWWWWWWWWWWWL ",
		"               ",
	};

	private static final String[] UPPER_TWO = {
		"               ",
		" LWgWWWWWWWgWL ",
		" W           W ",
		" W   W   W   W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" LWWWWggWWWWWL ",
		"               ",
	};

	private static final String[] UPPER_THREE = {
		"               ",
		" LHHHWHHHWHHHL ",
		" H   W   W   H ",
		" H   W   W   H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" H           H ",
		" LHHHHHHHHHHHL ",
		"               ",
	};

	private static final String[][] LAYERS = {
		GROUND, FLOOR_ONE, FLOOR_TWO, FLOOR_THREE, DECK, UPPER_ONE, UPPER_TWO, UPPER_THREE
	};

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		if (!Blueprint.place(level, corner, LAYERS, facing, (l, at, c, dir, mx, mz) ->
				set(l, at, c, dir, mx, mz, random))) {
			return false;
		}
		gable(level, corner, facing, random);
		return true;
	}

	private static void set(ServerLevel level, BlockPos at, char c, Direction facing,
	                        int x, int z, RandomSource random) {
		switch (c) {
			case '#' -> put(level, at, weathered(random));
			case 'W', '.', '_' -> put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case 'L' -> put(level, at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
			case 'H' -> put(level, at, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
			case 'g' -> put(level, at, Blocks.GLASS.defaultBlockState());
			case 'b' -> put(level, at, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.HALF, Half.TOP));
			case 'D' -> door(level, at, facing);
			case 'd' -> bed(level, at, facing, random);
			case 'K' -> chest(level, at, facing, random, Loot.Tier.TOWN_TRADE);
			case 'C' -> chest(level, at, facing, random, Loot.Tier.TOWN_HOME);
			case 'A' -> Blueprint.barrel(level, at, random, Loot.Tier.TOWN_ARMS);
			case 'T' -> table(level, at);
			// Benches look AT the table, not all one way. A stair chair seats
			// you facing where its step points, so the one west of a table
			// faces east and the one east of it faces west — otherwise a row
			// of diners all sit staring at the same wall.
			// Seats work out where the table is and turn their backs on it.
			case 'h', 'j' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.seatFacing(FLOOR_ONE, x, z, 'T', facing)));
			case 's' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.NORTH, facing)));
			case 'P' -> put(level, at, potted(random));
			// The hearth, and it is the only fire in the town that is not a
			// lantern. A hall is where the warmth is.
			case 'Q' -> put(level, at, Blocks.CAMPFIRE.defaultBlockState());
			// The counter: a course of planks with a slab lipped over it.
			case 'N' -> put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case 'n' -> put(level, at, Blocks.SPRUCE_SLAB.defaultBlockState());
			// The gallery rail. Fences rather than a wall, because the point of
			// the well is being able to see down into the room from it.
			case 'r' -> put(level, at, Blocks.SPRUCE_FENCE.defaultBlockState());
			case 'q' -> put(level, at, Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));
			case 'o' -> put(level, at, Blocks.AIR.defaultBlockState());
			default -> { }
		}
	}

	private static void gable(ServerLevel level, BlockPos corner, Direction facing,
	                          RandomSource random) {
		for (int h = 0; h <= 3; h++) {
			int y = EAVE + h;
			int near = h * 2;
			int far = DEPTH - 1 - h * 2;
			for (int x = 0; x <= WIDTH - 1; x++) {
				slate(level, corner, x, y, near, facing, Direction.NORTH, true, random);
				slate(level, corner, x, y, near + 1, facing, Direction.NORTH, false, random);
				slate(level, corner, x, y, far, facing, Direction.SOUTH, true, random);
				slate(level, corner, x, y, far - 1, facing, Direction.SOUTH, false, random);
			}
			for (int z = near + 2; z <= far - 2; z++) {
				slate(level, corner, 1, y, z, facing, Direction.NORTH, false, random);
				slate(level, corner, WIDTH - 2, y, z, facing, Direction.NORTH, false, random);
			}
		}
		for (int x = 0; x <= WIDTH - 1; x++) {
			slate(level, corner, x, EAVE + 3, RIDGE_Z, facing, Direction.NORTH, false, random);
		}
	}

	private static void slate(ServerLevel level, BlockPos corner, int x, int y, int z,
	                          Direction facing, Direction fall, boolean stepped,
	                          RandomSource random) {
		if (random.nextInt(28) == 0) {
			return;
		}
		BlockPos at = corner.offset(Blueprint.spinX(x, z, WIDTH, DEPTH, facing), y,
			Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
		put(level, at, stepped
			? Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(fall.getOpposite(), facing))
			: Blocks.SPRUCE_PLANKS.defaultBlockState());
	}

	private static void door(ServerLevel level, BlockPos at, Direction facing) {
		if (level.getBlockState(at).is(Blocks.SPRUCE_DOOR)) {
			return;
		}
		BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		put(level, at, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER));
		put(level, at.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER));
	}

	private static void bed(ServerLevel level, BlockPos at, Direction facing,
	                        RandomSource random) {
		BlockState bed = Blocks.BED.pick(random.nextBoolean() ? DyeColor.RED : DyeColor.BROWN)
			.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		put(level, at, bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		put(level, at.relative(facing), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
	}

	private static void chest(ServerLevel level, BlockPos at, Direction facing,
	                          RandomSource random, Loot.Tier tier) {
		put(level, at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			Loot.scatter(chest, random, tier);
		}
	}

	private static void table(ServerLevel level, BlockPos at) {
		put(level, at, Blocks.OAK_FENCE.defaultBlockState());
		put(level, at.above(), Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
	}

	private static BlockState potted(RandomSource random) {
		return switch (random.nextInt(4)) {
			case 0 -> Blocks.POTTED_FERN.defaultBlockState();
			case 1 -> Blocks.POTTED_RED_TULIP.defaultBlockState();
			case 2 -> Blocks.POTTED_OXEYE_DAISY.defaultBlockState();
			default -> Blocks.POTTED_CORNFLOWER.defaultBlockState();
		};
	}

	private static BlockState weathered(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		return roll < 8 ? Blocks.COBBLESTONE.defaultBlockState()
			: Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static void put(ServerLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, 2);
	}
}
