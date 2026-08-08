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

/**
 * A family house, and the most-built thing in the town.
 *
 * Six of these go up, so it sets the style for everything else — the hall and
 * the shops are variations on whatever this establishes, which is why it was
 * built before the church rather than after.
 *
 * TWO STOREYS, and the reason is space rather than grandeur. A town wall costs
 * stone by the length, so land inside it is the expensive thing and everybody
 * builds upward: living below, sleeping above, one small footprint. That is
 * true of every walled settlement that has ever existed and it means the town
 * reads correctly without anybody being told why.
 *
 * The ward is beside the door and never explained. Three by three, plain dark
 * stone, on every house in the town — a player sees forty of them before
 * anything happens and learns what "normal" looks like without once being
 * told they are learning it. That is the whole investment: when one of them
 * changes, nobody has to write a word.
 */
public final class Lodge {
	private Lodge() {}

	/** The roof starts here, above two storeys of wall. */
	private static final int EAVE = 8;
	private static final int RIDGE_Z = 4;

	private static final String[] GROUND = {
		"###########",
		"#.........#",
		"#.........#",
		"#.........#",
		"#.........#",
		"#.........#",
		"#.........#",
		"#.........#",
		"###########",
	};

	private static final String[] FLOOR_ONE = {
		"###########",
		"#  Fc CA  #",
		"#        S#",
		"#    h    #",
		"# A hTh   #",
		"#         #",
		"#s       P#",
		"#         #",
		"#####D#EEE#",
	};

	private static final String[] FLOOR_TWO = {
		"WWgWWWWWgWW",
		"W         W",
		"W         W",
		"g         g",
		"W         W",
		"Ws        W",
		"g         g",
		"W         W",
		"WWgWWDWEeEW",
	};

	private static final String[] FLOOR_THREE = {
		"WWWWWWWWWWW",
		"W         W",
		"W         W",
		"W         W",
		"Ws        W",
		"W         W",
		"W         W",
		"W         W",
		"WWWWWWWEEEW",
	};

	private static final String[] DECK = {
		"WWWWWWWWWWW",
		"W_________W",
		"W_________W",
		"W_________W",
		"Wo________W",
		"Wo________W",
		"Wo________W",
		"W_________W",
		"WWWWWWWWWWW",
	};

	private static final String[] UPPER_ONE = {
		"WWWWWWWWWWW",
		"W B  W  B W",
		"W    W    W",
		"W    W    W",
		"W         W",
		"W    W    W",
		"W xK W   AW",
		"W    W    W",
		"WWWWWWWWWWW",
	};

	private static final String[] UPPER_TWO = {
		"WWgWWWWWgWW",
		"W    W    W",
		"g    W    g",
		"W    W    W",
		"W         W",
		"W    W    W",
		"g    W    g",
		"W    W    W",
		"WWgWWWWWgWW",
	};

	private static final String[] UPPER_THREE = {
		"WWWWWWWWWWW",
		"W    W    W",
		"W    W    W",
		"W    W    W",
		"W    W    W",
		"W    W    W",
		"W    W    W",
		"W    W    W",
		"WWWWWWWWWWW",
	};

	private static final String[][] LAYERS = {
		GROUND, FLOOR_ONE, FLOOR_TWO, FLOOR_THREE, DECK, UPPER_ONE, UPPER_TWO, UPPER_THREE
	};

	public static final int WIDTH = 11;
	public static final int DEPTH = 9;

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		if (!Blueprint.place(level, corner, LAYERS, facing, (l, at, c, dir) ->
				set(l, at, c, dir, random))) {
			return false;
		}
		gable(level, corner, facing, random);
		return true;
	}

	/**
	 * What each character is.
	 *
	 * Takes the building's facing so doors, beds and stairs turn with it. A
	 * house rotated to front the lane with its bed still pointing north is the
	 * kind of thing nobody consciously notices and everybody feels.
	 */
	private static void set(ServerLevel level, BlockPos at, char c, Direction facing,
	                        RandomSource random) {
		switch (c) {
			case '#' -> put(level, at, weathered(random));
			case 'W' -> put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case '.', '_' -> put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case 'o' -> put(level, at, Blocks.AIR.defaultBlockState());
			case 'g' -> put(level, at, Blocks.GLASS_PANE.defaultBlockState());
			case 'D' -> door(level, at, facing);
			case 'B' -> bed(level, at, facing, random);
			case 'C' -> chest(level, at, facing, random, Loot.Tier.LARDER);
			case 'K' -> chest(level, at, facing, random, Loot.Tier.HOMESTEAD);
			case 'c' -> put(level, at, Blocks.CRAFTING_TABLE.defaultBlockState());
			case 'F' -> put(level, at, Blocks.FURNACE.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
			case 'A' -> put(level, at, Blocks.BARREL.defaultBlockState());
			case 'S' -> put(level, at, Blocks.BOOKSHELF.defaultBlockState());
			case 'P' -> put(level, at, Blocks.POTTED_FERN.defaultBlockState());
			case 'T' -> table(level, at);
			case 'h' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
			// The way upstairs. A three-block run against the west wall with
			// the deck opened above it — the first version was a single ladder
			// at one height facing the wrong wall, so there was no way up at
			// all and the whole first floor was unreachable.
			case 's' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.NORTH, facing)));
			case 'x' -> put(level, at, Blocks.COBWEB.defaultBlockState());
			// The ward. Deliberately dull stone, and deliberately identical on
			// every house — the meaning is in the repetition, not the design.
			case 'E' -> put(level, at, Blocks.DEEPSLATE_TILES.defaultBlockState());
			case 'e' -> put(level, at, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			default -> { }
		}
	}

	/**
	 * The roof, gabled, same as the farmhouse.
	 *
	 * Deliberately the same language. A town whose houses are roofed like the
	 * homestead is a town the family came from, and that is worth more than any
	 * amount of variety — the player should reach the farmhouse and find it
	 * familiar without knowing why.
	 */
	private static void gable(ServerLevel level, BlockPos corner, Direction facing,
	                          RandomSource random) {
		for (int h = 0; h <= 2; h++) {
			int y = EAVE + h;
			int near = -1 + h * 2;
			int far = DEPTH - h * 2;
			for (int x = -1; x <= WIDTH; x++) {
				slate(level, corner, x, y, near, facing, Direction.NORTH, true, random);
				slate(level, corner, x, y, near + 1, facing, Direction.NORTH, false, random);
				slate(level, corner, x, y, far, facing, Direction.SOUTH, true, random);
				slate(level, corner, x, y, far - 1, facing, Direction.SOUTH, false, random);
			}
			for (int z = near + 2; z <= far - 2; z++) {
				slate(level, corner, 0, y, z, facing, Direction.NORTH, false, random);
				slate(level, corner, WIDTH - 1, y, z, facing, Direction.NORTH, false, random);
			}
		}
		for (int x = -1; x <= WIDTH; x++) {
			slate(level, corner, x, EAVE + 2, RIDGE_Z, facing, Direction.NORTH, false, random);
		}
	}

	private static void slate(ServerLevel level, BlockPos corner, int x, int y, int z,
	                          Direction facing, Direction fall, boolean stepped,
	                          RandomSource random) {
		if (random.nextInt(24) == 0) {
			return;   // a slate off, here and there
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
		BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		if (level.getBlockState(at).is(Blocks.SPRUCE_DOOR)) {
			return;
		}
		put(level, at, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER));
		put(level, at.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER));
	}

	private static void bed(ServerLevel level, BlockPos at, Direction facing,
	                        RandomSource random) {
		DyeColor colour = random.nextBoolean() ? DyeColor.WHITE
			: random.nextBoolean() ? DyeColor.BROWN : DyeColor.LIGHT_GRAY;
		BlockState bed = Blocks.BED.pick(colour).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
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
