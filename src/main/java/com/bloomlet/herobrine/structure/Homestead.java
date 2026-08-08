package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The first house. Somebody lived here.
 *
 * This is the only place in the mod where the player meets the family as
 * people rather than as a story about people, and everything about it is built
 * to be READ rather than survived. Nothing here attacks, nothing spawns,
 * nothing chases. It is a house with the furniture still in it.
 *
 * The design rule throughout: EVERY WRONG THING HAS AN ORDINARY EXPLANATION,
 * and the ordinary explanations do not survive being put next to each other.
 * Two beds in a house with four names in the ledger. Three graves outside for
 * four people. A sheep pen with the gate shut and bones inside it. A wall in
 * the back of the house built out of the wrong stone, from the outside, with
 * no door in it. Any one of those is a derelict building. All of them at once
 * is a sentence, and the player assembles it themselves — which is the only
 * way it ever lands.
 *
 * Kept to three blocks of interior height on purpose. A tall building reads as
 * a landmark or a dungeon; a low one reads as somewhere people ate.
 *
 * Laid out as character maps by layer rather than as a list of block placements
 * because the layout is the design here, and a floorplan you can see in the
 * source is a floorplan you can argue with. The maps are validated on class
 * load — a miscounted row is otherwise invisible and would silently shift half
 * the house sideways.
 */
public final class Homestead {
	private Homestead() {}

	/** Where the front door sits in the maps, for facing the path. */
	private static final int DOOR_X = 5;

	private static final String[] GROUND = {
		"                       ",
		"                       ",
		"  #############~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #...........#~~~~~~~ ",
		"  #############        ",
		"   ,,,,,,      OOO     ",
		"     ,         OoO     ",
		"     , ------  OOO     ",
		"     , ------     *    ",
		"     , ------       *  ",
		"     , ------      *   ",
	};

	private static final String[] COURSE_ONE = {
		"                       ",
		"                       ",
		"  #############fffffff ",
		"  #AFcCh WB BP#f     f ",
		"  #   hThW   C#f k   f ",
		"  #L   h      #G     f ",
		"  #S     WMMMM#f   k f ",
		"  #S    xW ^^x#f     f ",
		"  # l x rWB   #f  k  f ",
		"  #P^   CW  xC#fffffff ",
		"  ###D#########        ",
		"               OOO     ",
		"               OoO     ",
		"               OOO     ",
		"                  ^    ",
		"                    ^  ",
		"                   ^   ",
	};

	private static final String[] COURSE_TWO = {
		"                       ",
		"                       ",
		"  WWgWWWgWWgWgW        ",
		"  W   t  W    W        ",
		"  g      W  t W        ",
		"  W           W        ",
		"  WS     WMMMMW        ",
		"  WS     W    W        ",
		"  b      W x  b        ",
		"  W    x W    W        ",
		"  WWWDWWWWWWWWW        ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
	};

	private static final String[] COURSE_THREE = {
		"                       ",
		"                       ",
		"  WWWWWWWWWWWWW        ",
		"  W      W    W        ",
		"  W x    W    W        ",
		"  W      W    W        ",
		"  W      WMMMMW        ",
		"  W      W    W        ",
		"  W      W  x W        ",
		"  W      W    W        ",
		"  WWWWWWWWWWWWW        ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
	};

	private static final String[] ROOF = {
		"                       ",
		"                       ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"  _____________        ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
		"                       ",
	};


	private static final String[][] LAYERS = {
		GROUND, COURSE_ONE, COURSE_TWO, COURSE_THREE, ROOF
	};

	static {
		int width = GROUND[0].length();
		for (String[] layer : LAYERS) {
			if (layer.length != GROUND.length) {
				throw new IllegalStateException("homestead layer has wrong depth");
			}
			for (String row : layer) {
				if (row.length() != width) {
					throw new IllegalStateException("homestead row is " + row.length()
						+ " wide, expected " + width + ": '" + row + "'");
				}
			}
		}
	}

	public static int width() {
		return GROUND[0].length();
	}

	public static int depth() {
		return GROUND.length;
	}

	/**
	 * Raise it, with its north-west corner at the given position.
	 *
	 * @param origin the block the map's (0,0) sits on; y is the floor level
	 */
	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		levelGround(level, origin);

		for (int layer = 0; layer < LAYERS.length; layer++) {
			for (int z = 0; z < depth(); z++) {
				String row = LAYERS[layer][z];
				for (int x = 0; x < width(); x++) {
					char c = row.charAt(x);
					if (c == ' ') {
						continue;
					}
					place(level, origin.offset(x, layer, z), c, x, z, layer, random);
				}
			}
		}
		cellar(level, origin, random);
		HerobrineMod.LOGGER.info("homestead raised at [{}, {}, {}]",
			origin.getX(), origin.getY(), origin.getZ());
	}

	/**
	 * Flatten what it stands on and clear what it stands in.
	 *
	 * Without this the house sinks into a slope at one corner and floats at
	 * another, which is the single most common way a placed structure announces
	 * that it was placed. Fills underneath with dirt rather than leaving a
	 * hole, and clears five blocks of air above so it is never built through a
	 * tree.
	 */
	private static void levelGround(ServerLevel level, BlockPos origin) {
		for (int z = 0; z < depth(); z++) {
			for (int x = 0; x < width(); x++) {
				BlockPos floor = origin.offset(x, 0, z);
				for (int down = 1; down <= 4; down++) {
					BlockPos below = floor.below(down);
					if (level.getBlockState(below).isAir()
						|| !level.getFluidState(below).isEmpty()) {
						level.setBlockAndUpdate(below, Blocks.DIRT.defaultBlockState());
					}
				}
				for (int up = 0; up <= 5; up++) {
					BlockPos above = floor.above(up);
					if (!level.getBlockState(above).isAir()) {
						level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	private static void place(ServerLevel level, BlockPos pos, char c,
	                          int x, int z, int layer, RandomSource random) {
		switch (c) {
			case '#' -> set(level, pos, weathered(random));
			case 'W' -> set(level, pos, Blocks.SPRUCE_PLANKS.defaultBlockState());
			// The wall that does not match. Always mossy cobble, never the
			// weathered mix, so it reads as a different job done at a
			// different time by somebody in a hurry.
			case 'M' -> set(level, pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			case '.' -> set(level, pos, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case ',' -> set(level, pos, Blocks.COARSE_DIRT.defaultBlockState());
			case '~' -> set(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
			case '-' -> field(level, pos, random);
			case 'O' -> set(level, pos, Blocks.COBBLESTONE.defaultBlockState());
			case 'o' -> set(level, pos, Blocks.WATER.defaultBlockState());
			case '*' -> set(level, pos, Blocks.PODZOL.defaultBlockState());
			case 'f' -> set(level, pos, Blocks.SPRUCE_FENCE.defaultBlockState());
			case 'G' -> set(level, pos, Blocks.SPRUCE_FENCE_GATE.defaultBlockState());
			case 'g' -> set(level, pos, Blocks.GLASS_PANE.defaultBlockState());
			case 'b' -> set(level, pos, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HALF, Half.BOTTOM));
			case 'D' -> door(level, pos, layer);
			case 'B' -> bed(level, pos);
			case 'C' -> chest(level, pos, x, z);
			case 'c' -> set(level, pos, Blocks.CRAFTING_TABLE.defaultBlockState());
			case 'F' -> set(level, pos, Blocks.FURNACE.defaultBlockState());
			case 'A' -> set(level, pos, Blocks.BARREL.defaultBlockState());
			case 'S' -> set(level, pos, Blocks.BOOKSHELF.defaultBlockState());
			case 'L' -> lectern(level, pos);
			case 'T' -> table(level, pos);
			case 'h' -> set(level, pos, Blocks.SPRUCE_STAIRS.defaultBlockState());
			case 'P' -> set(level, pos, Blocks.POTTED_DEAD_BUSH.defaultBlockState());
			case 't' -> torch(level, pos, Blocks.WALL_TORCH, Blocks.TORCH);
			case 'r' -> torch(level, pos, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_TORCH);
			case 'l' -> set(level, pos, Blocks.AIR.defaultBlockState());   // the way down
			case 'x' -> set(level, pos, Blocks.COBWEB.defaultBlockState());
			case 'k' -> set(level, pos, Blocks.BONE_BLOCK.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
			case '^' -> sign(level, pos, x, z);
			default -> { }
		}
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, 2);
	}

	/** The same palette the ruins use, so the two read as one hand. */
	private static BlockState weathered(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.COBBLESTONE.defaultBlockState();
	}

	/** A field nobody harvested and nobody replanted. */
	private static void field(ServerLevel level, BlockPos pos, RandomSource random) {
		set(level, pos, Blocks.FARMLAND.defaultBlockState());
		if (random.nextInt(3) > 0) {
			set(level, pos.above(), Blocks.WHEAT.defaultBlockState()
				.setValue(BlockStateProperties.AGE_7, 7));
		}
	}

	private static void door(ServerLevel level, BlockPos pos, int layer) {
		if (layer != 1) {
			return;   // the upper half comes with the lower
		}
		BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		set(level, pos, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		set(level, pos.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
	}

	/**
	 * Foot here, head one to the south.
	 *
	 * Colours are a ColorCollection in 26.2 rather than sixteen separate
	 * fields, so a bed is Blocks.BED.pick(DyeColor.…).
	 */
	private static void bed(ServerLevel level, BlockPos pos) {
		BlockState bed = Blocks.BED.pick(DyeColor.WHITE).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		set(level, pos, bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		set(level, pos.south(), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
	}

	/** A table is a fence with a plate on it, which is the oldest trick there is. */
	private static void table(ServerLevel level, BlockPos pos) {
		set(level, pos, Blocks.OAK_FENCE.defaultBlockState());
		set(level, pos.above(), Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
	}

	/**
	 * A torch on whichever wall is next to it.
	 *
	 * Worked out from the neighbours rather than written into the map, because
	 * a hardcoded facing is wrong the moment a wall moves, and a torch left
	 * floating in the middle of a room is the kind of detail that makes a whole
	 * building look generated.
	 */
	private static void torch(ServerLevel level, BlockPos pos,
	                          net.minecraft.world.level.block.Block wall,
	                          net.minecraft.world.level.block.Block standing) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(facing.getOpposite())).isSolid()) {
				set(level, pos, wall.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
				return;
			}
		}
		if (level.getBlockState(pos.below()).isSolid()) {
			set(level, pos, standing.defaultBlockState());
		}
	}

	private static void lectern(ServerLevel level, BlockPos pos) {
		set(level, pos, Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
			.setValue(BlockStateProperties.HAS_BOOK, true));
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
			lectern.setBook(HouseBooks.household());
		}
	}

	/**
	 * What is in each chest, by where it stands.
	 *
	 * Deliberately not random. Each book belongs to the room it is found in —
	 * the ledger in the bedroom where the man who kept it slept, the tally
	 * behind the sealed wall where the person who wrote it was put. A player
	 * who finds them in the wrong order still gets the story; one who finds
	 * them in the wrong PLACES does not get anything.
	 */
	private static void chest(ServerLevel level, BlockPos pos, int x, int z) {
		set(level, pos, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		BlockEntity entity = level.getBlockEntity(pos);
		if (!(entity instanceof ChestBlockEntity chest)) {
			return;
		}
		if (x == 6 && z == 3) {                 // by the hearth
			chest.setItem(0, HouseBooks.child());
			chest.setItem(2, new ItemStack(Items.BREAD, 3));
			chest.setItem(3, new ItemStack(Items.WHEAT, 12));
			chest.setItem(5, new ItemStack(Items.WOOL.pick(DyeColor.WHITE), 4));
		} else if (x == 8 && z == 9) {          // nearest the sealed wall
			chest.setItem(0, HouseBooks.farRoom());
			chest.setItem(2, new ItemStack(Items.IRON_INGOT, 2));
			chest.setItem(4, new ItemStack(Items.STICK, 9));
		} else if (x == 13 && z == 4) {         // the bedroom
			chest.setItem(0, HouseBooks.ledger());
			chest.setItem(3, new ItemStack(Items.SHEARS));
			chest.setItem(5, new ItemStack(Items.LEATHER, 3));
		} else {                                 // behind the wall
			chest.setItem(0, HouseBooks.tally());
		}
	}

	/**
	 * What is written where.
	 *
	 * The family write like people. He writes in the sealed room, in the
	 * lowercase-no-punctuation voice LORE.md gives him, and the change of hand
	 * halfway through the building is the loudest thing in it — nobody has to
	 * be told that two different people wrote these.
	 */
	private static void sign(ServerLevel level, BlockPos pos, int x, int z) {
		String[] lines;
		if (x == 4 && z == 9) {
			lines = new String[]{"water before dark", "bread on the", "second day", "J. — remember"};
		} else if (z == 7 && x == 11) {
			lines = new String[]{"i can hear them", "sleeping"};
		} else if (z == 7 && x == 12) {
			lines = new String[]{"the bar is on", "their side", "i can wait"};
		} else if (x == 18) {
			lines = new String[]{"M."};
		} else if (x == 20) {
			lines = new String[]{"R."};
		} else {
			// Three markers, four names in the books. Nobody comments on it.
			lines = new String[]{"the little one"};
		}
		set(level, pos, Blocks.SPRUCE_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 8));
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			net.minecraft.world.level.block.entity.SignText text = sign.getFrontText();
			for (int i = 0; i < lines.length && i < 4; i++) {
				text = text.setMessage(i, net.minecraft.network.chat.Component.literal(lines[i]));
			}
			sign.setText(text, true);
			sign.setWaxed(true);
		}
	}

	/**
	 * Under the floor.
	 *
	 * The cellar is the only part that is not domestic, and it is placed at the
	 * end of the visit on purpose — the player has spent five minutes reading
	 * about bread and sheep before they find the hole. What is down there is
	 * not a mine. There is no ore in it, no rail, no branch pattern, nothing
	 * anybody would dig FOR. It is a room, and then a tunnel going north that
	 * stops in the middle of the stone without reaching anything.
	 *
	 * That is the entire content of it, and it is enough, because the question
	 * it asks — what was he digging towards — is the question the rest of the
	 * mod is the answer to.
	 */
	private static void cellar(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos hole = origin.offset(4, 0, 8);

		// The room, hollowed under the west half of the house.
		for (int x = 3; x <= 7; x++) {
			for (int z = 6; z <= 10; z++) {
				for (int y = -1; y >= -3; y--) {
					set(level, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
				set(level, origin.offset(x, -4, z), Blocks.COBBLESTONE.defaultBlockState());
			}
		}
		// The hole in the floor itself. The map cannot do this: its ground
		// layer lays a plank floor across the whole interior, so without
		// taking one back out the cellar is a sealed box nobody can reach.
		set(level, hole, Blocks.AIR.defaultBlockState());

		// A pillar beside the shaft, so the ladder has something to hang on.
		// Carved out of the room after it is hollowed, not before.
		for (int y = -1; y >= -3; y--) {
			set(level, origin.offset(4, y, 7), Blocks.COBBLESTONE.defaultBlockState());
			set(level, origin.offset(4, y, 8), Blocks.LADDER.defaultBlockState()
				.setValue(LadderBlock.FACING, Direction.SOUTH));
		}

		// The tunnel. It goes north and it stops.
		for (int run = 1; run <= 11; run++) {
			BlockPos at = origin.offset(5, -3, 6 - run);
			set(level, at, Blocks.AIR.defaultBlockState());
			set(level, at.below(), Blocks.AIR.defaultBlockState());
			set(level, at.below(2), Blocks.COBBLESTONE.defaultBlockState());
			if (run % 4 == 0) {
				// Inside the tunnel, hung on its west wall — so it faces east.
				// Put in the wall itself it would have had nothing behind it.
				set(level, at, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
			}
			if (random.nextInt(4) == 0) {
				set(level, at.below(), Blocks.COBWEB.defaultBlockState());
			}
		}

		BlockPos crate = origin.offset(6, -3, 9);
		set(level, crate, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		if (level.getBlockEntity(crate) instanceof ChestBlockEntity chest) {
			chest.setItem(0, HouseBooks.brother());
			chest.setItem(2, new ItemStack(Items.IRON_PICKAXE));
			chest.setItem(4, new ItemStack(Items.TORCH, 17));
		}
		set(level, origin.offset(3, -3, 9), Blocks.COBWEB.defaultBlockState());
	}

	/** Where the ground actually is, averaged so a slope does not tilt it. */
	public static int floorHeightAt(ServerLevel level, int originX, int originZ) {
		long total = 0;
		int count = 0;
		for (int z = 0; z < depth(); z += 4) {
			for (int x = 0; x < width(); x += 4) {
				total += level.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, originX + x, originZ + z);
				count++;
			}
		}
		return (int)(total / count);
	}

	public static int doorX() {
		return DOOR_X;
	}
}
