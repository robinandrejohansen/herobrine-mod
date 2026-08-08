package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.structure.Ground;
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
		"           ",
		" ######### ",
		" #.......# ",
		" #.......# ",
		" #.......# ",
		" #.......# ",
		" #.......# ",
		" ######### ",
		"           ",
	};

	private static final String[] FLOOR_ONE = {
		"           ",
		" LWFcWCAWL ",
		" W      PW ",
		" W       W ",
		" W  hTj  W ",
		" W      SW ",
		" Ws      W ",
		" LWWWDEEEL ",
		"           ",
	};

	private static final String[] FLOOR_TWO = {
		"           ",
		" LbggbggbL ",
		" W       W ",
		" g       g ",
		" W       W ",
		" gs      g ",
		" W       W ",
		" LbggDEeEL ",
		"           ",
	};

	private static final String[] FLOOR_THREE = {
		"           ",
		" LHHHHHHHL ",
		" H       H ",
		" H       H ",
		" Hs      H ",
		" H       H ",
		" H       H ",
		" LHHHHEEEL ",
		"           ",
	};

	private static final String[] DECK = {
		"           ",
		"HHHHHHHHHHH",
		"H_________H",
		"H_________H",
		"H_o_______H",
		"H_o_______H",
		"H_o_______H",
		"HHHHHHHHHHH",
		"           ",
	};

	private static final String[] UPPER_ONE = {
		"           ",
		"LWWWWWWWWWL",
		"W d  W  d W",
		"W    W    W",
		"W         W",
		"W    W    W",
		"W  K W x AW",
		"LWWWWWWWWWL",
		"           ",
	};

	private static final String[] UPPER_TWO = {
		"           ",
		"LWWggWggWWL",
		"W    W    W",
		"g    W    g",
		"W         W",
		"g    W    g",
		"W    W    W",
		"LWWggWWgWWL",
		"           ",
	};

	private static final String[] UPPER_THREE = {
		"           ",
		"LHHHHHHHHHL",
		"H    W    H",
		"H    W    H",
		"H    W    H",
		"H    W    H",
		"H    W    H",
		"LHHHHHHHHHL",
		"           ",
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
		garden(level, corner, facing, random);
		return true;
	}

	/**
	 * The strip in front of the door.
	 *
	 * Placed against the ground rather than the building's floor, because it is
	 * outside and the ground out there does whatever it likes. A house on a
	 * slight rise with its flowers hovering a block over the grass is worse than
	 * no flowers.
	 *
	 * Deliberately scruffy: a few flowers, a bit of tall grass, some path worn
	 * to the door and one fence post. A neat garden reads as landscaping; a
	 * patchy one reads as somewhere people walk in and out of.
	 */
	private static void garden(ServerLevel level, BlockPos corner, Direction facing,
	                           RandomSource random) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = DEPTH - 1; z <= DEPTH; z++) {
				int mx = Blueprint.spinX(x, z, WIDTH, DEPTH, facing);
				int mz = Blueprint.spinZ(x, z, WIDTH, DEPTH, facing);
				int gx = corner.getX() + mx;
				int gz = corner.getZ() + mz;
				BlockPos on = new BlockPos(gx, Ground.topOf(level, gx, gz) + 1, gz);
				if (!level.getBlockState(on).isAir()) {
					continue;
				}
				int roll = random.nextInt(12);
				if (roll < 2) {
					put(level, on, flower(random));
				} else if (roll < 4) {
					put(level, on, Blocks.SHORT_GRASS.defaultBlockState());
				} else if (roll == 4) {
					put(level, on.below(), Blocks.DIRT_PATH.defaultBlockState());
				}
			}
		}
	}

	private static BlockState flower(RandomSource random) {
		return switch (random.nextInt(5)) {
			case 0 -> Blocks.OXEYE_DAISY.defaultBlockState();
			case 1 -> Blocks.CORNFLOWER.defaultBlockState();
			case 2 -> Blocks.AZURE_BLUET.defaultBlockState();
			case 3 -> Blocks.POPPY.defaultBlockState();
			default -> Blocks.DANDELION.defaultBlockState();
		};
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
			// Timber framing. Upright posts at every corner and a beam course
			// between the floors, and it is the single thing that separates a
			// house somebody designed from a box with windows in it. Stripped
			// log against plank gives two tones and a visible structure, which
			// is what the eye reads as "built" rather than "generated".
			case 'L' -> put(level, at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
			case 'H' -> put(level, at, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
			// Shutters, open, either side of a window. Free depth on a flat
			// wall and the cheapest detail in the building.
			case 'b' -> put(level, at, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.HALF, Half.TOP));
			case '.', '_' -> put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case 'o' -> put(level, at, Blocks.AIR.defaultBlockState());
			// Panes connect to solid blocks and to each other, and to nothing
			// else. A single pane with a trapdoor shutter beside it has nothing
			// to reach for and renders as a thin post in the middle of a hole —
			// so every shuttered window is two panes wide, and the pair reads
			// as glass because it is connected to itself.
			case 'g' -> put(level, at, Blocks.GLASS_PANE.defaultBlockState());
			case 'D' -> door(level, at, facing);
			case 'd' -> bed(level, at, facing, random);
			case 'C' -> chest(level, at, facing, random, Loot.Tier.LARDER);
			case 'K' -> chest(level, at, facing, random, Loot.Tier.HOMESTEAD);
			case 'c' -> put(level, at, Blocks.CRAFTING_TABLE.defaultBlockState());
			case 'F' -> put(level, at, Blocks.FURNACE.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
			case 'A' -> put(level, at, Blocks.BARREL.defaultBlockState());
			case 'S' -> put(level, at, Blocks.BOOKSHELF.defaultBlockState());
			case 'P' -> put(level, at, potted(random));
			case 'T' -> table(level, at);
			// Benches look AT the table. West of it faces east, east of it
			// faces west — otherwise everyone sits staring at the same wall.
			case 'h' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.EAST, facing)));
			case 'j' -> put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.WEST, facing)));
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

	/**
	 * Something growing indoors.
	 *
	 * Varied per pot rather than one plant everywhere, because six identical
	 * ferns in six identical windows is worse than no ferns at all — it is the
	 * detail that proves the houses were stamped.
	 */
	private static BlockState potted(RandomSource random) {
		return switch (random.nextInt(5)) {
			case 0 -> Blocks.POTTED_FERN.defaultBlockState();
			case 1 -> Blocks.POTTED_RED_TULIP.defaultBlockState();
			case 2 -> Blocks.POTTED_OXEYE_DAISY.defaultBlockState();
			case 3 -> Blocks.POTTED_AZURE_BLUET.defaultBlockState();
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
