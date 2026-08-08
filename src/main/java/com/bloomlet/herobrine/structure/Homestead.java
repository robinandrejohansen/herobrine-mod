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
 * Four blocks of interior height under a roof, which is low enough to read as
 * somewhere people ate rather than as a landmark or a dungeon, and tall enough
 * that walking through it does not feel like crouching in a crawlspace. The
 * first cut of this was three high and roofless and played like a model of a
 * house rather than a house.
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
	private static final int DOOR_X = 6;

	/** The building itself. Everything outside this follows the ground. */
	private static final int HOUSE_X0 = 2;
	private static final int HOUSE_X1 = 18;
	private static final int HOUSE_Z0 = 2;
	private static final int HOUSE_Z1 = 14;

	/**
	 * Yard pieces that sit on the ground rather than on the building's floor.
	 *
	 * This is the same split vanilla makes. A village house is projected onto
	 * the heightmap as one rigid block and the terrain is bent to meet it, but
	 * village ROADS use terrain_matching and follow the ground per column —
	 * which is why a village on a hillside has level houses and paths that run
	 * up the slope, instead of one enormous flat platform.
	 *
	 * Flattening the whole 29x23 map was the single worst thing about how this
	 * sat in the world. A rectangle of level ground that size reads as a
	 * building site from three hundred blocks away, and no amount of moss on
	 * the walls recovers from it.
	 */
	private static final String FOLLOWS_GROUND = "~,-O*fGkmnp";

	/** How far a following block may stray from the floor before it is dropped. */
	private static final int MAX_DRIFT = 7;

	private static final String[] GROUND = {
		"                             ",
		"                             ",
		"  ################# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............# ~~~~~~~~ ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............# OOO      ",
		"  ################# OoO      ",
		"    ,,,,,,,         OOO      ",
		"      ,                      ",
		"   ---,------                ",
		"   ---,------         *      ",
		"   ---,------            *   ",
		"   ---,------                ",
		"   ---,------          *     ",
		"      ,                      ",
	};

	private static final String[] COURSE_ONE = {
		"                             ",
		"                             ",
		"  ################# ffffffff ",
		"  #AFc1   PWB  B P# f      f ",
		"  #    h   W     3# f k    f ",
		"  #   hTh         # f      f ",
		"  #L   h   W x    # G      f ",
		"  #S     x W      # f    k f ",
		"  #SS    qrWMMMMMM# f      f ",
		"  #WWW WWWWW  we  # f  k   f ",
		"  #A      CW    x # ffffffff ",
		"  #A   x   WB     #          ",
		"  #  l     W x  C #          ",
		"  #      2 W     4# OOO      ",
		"  ####D############ OoO      ",
		"                    OOO      ",
		"                             ",
		"                             ",
		"                      m      ",
		"                         n   ",
		"                             ",
		"                       p     ",
		"                             ",
	};

	private static final String[] COURSE_TWO = {
		"                             ",
		"                             ",
		"  WWWgWWgWWWWgWWgWW          ",
		"  W  t     W  t   W          ",
		"  W        W      g          ",
		"  g     t         W          ",
		"  W        W      g          ",
		"  gS     x W      W          ",
		"  WS       WMMMMMMW          ",
		"  WWWW WWWWW      W          ",
		"  W        W      W          ",
		"  W        W x    b          ",
		"  g  t     W      W          ",
		"  W        W      b          ",
		"  WWWWDWWWWWWWWbWWW          ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
	};

	private static final String[] COURSE_THREE = {
		"                             ",
		"                             ",
		"  WWWWWWWWWWWWWWWWW          ",
		"  W        W      W          ",
		"  W x      W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        WMMMMMMW          ",
		"  WWWWWWWWWW      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W    x W          ",
		"  W        W      W          ",
		"  WWWWWWWWWWWWWWWWW          ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
	};

	private static final String[] COURSE_FOUR = {
		"                             ",
		"                             ",
		"  WWWWWWWWWWWWWWWWW          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W    x   W      W          ",
		"  W        WMMMMMMW          ",
		"  WWWWWWWWWW      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  W        W      W          ",
		"  WWWWWWWWWWWWWWWWW          ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
	};

	private static final String[] ROOF = {
		"                             ",
		" ///////////////////         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" /_________________/         ",
		" ///////////////////         ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
		"                             ",
	};

	private static final String[][] LAYERS = {
		GROUND, COURSE_ONE, COURSE_TWO, COURSE_THREE, COURSE_FOUR, ROOF
	};

	/** Which layer index the roof deck sits on, for the eaves. */
	private static final int ROOF_LAYER = 5;

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
					BlockPos at;
					if (FOLLOWS_GROUND.indexOf(c) >= 0) {
						int ground = groundTop(level, origin.getX() + x, origin.getZ() + z);
						if (Math.abs(ground - origin.getY()) > MAX_DRIFT) {
							continue;   // a cliff. Do not build a fence into it
						}
						at = new BlockPos(origin.getX() + x, ground + layer, origin.getZ() + z);
						clearAbove(level, at);
					} else {
						at = origin.offset(x, layer, z);
					}
					place(level, at, c, x, z, layer, random);
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
		for (int z = HOUSE_Z0 - 1; z <= HOUSE_Z1 + 1; z++) {
			for (int x = HOUSE_X0 - 1; x <= HOUSE_X1 + 1; x++) {
				BlockPos floor = origin.offset(x, 0, z);

				// Down to whatever the ground actually was, in cobblestone.
				// This is the beard: a house on a slope gets a stone footing
				// under its low side, which is what a real one would have, and
				// it reads as part of the building rather than as a lump of
				// dirt somebody dumped there.
				for (int down = 1; down <= 10; down++) {
					BlockPos below = floor.below(down);
					if (!level.getBlockState(below).isAir()
						&& level.getFluidState(below).isEmpty()) {
						break;
					}
					set(level, below, Blocks.COBBLESTONE.defaultBlockState());
				}
				for (int up = 0; up < LAYERS.length + 2; up++) {
					BlockPos above = floor.above(up);
					if (!level.getBlockState(above).isAir()) {
						set(level, above, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	/** The top solid block of the natural ground in this column. */
	private static int groundTop(ServerLevel level, int x, int z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
	}

	/** Take the grass and flowers off, so nothing is buried in a bush. */
	private static void clearAbove(ServerLevel level, BlockPos pos) {
		for (int up = 1; up <= 2; up++) {
			BlockState state = level.getBlockState(pos.above(up));
			if (!state.isAir() && !state.isSolid()) {
				set(level, pos.above(up), Blocks.AIR.defaultBlockState());
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
			// Nothing. Just the clearing already done above it — so the yard
			// keeps its own podzol, sand or snow instead of having a patch of
			// plains grass stamped over it.
			case '~' -> { }
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
			case 'C' -> chest(level, pos, null, random);
			case '1' -> chest(level, pos, HouseBooks.child(), random);
			case '2' -> chest(level, pos, HouseBooks.farRoom(), random);
			case '3' -> chest(level, pos, HouseBooks.ledger(), random);
			case '4' -> chest(level, pos, HouseBooks.tally(), random);
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
			case 'q' -> sign(level, pos, "water before dark", "bread on the",
				"second day", "J. — remember");
			// His hand. Lowercase, no full stops, unlike every other written
			// word in the building — nobody has to be told these are not the
			// family's.
			case 'w' -> sign(level, pos, "i can hear them", "sleeping");
			case 'e' -> sign(level, pos, "the bar is on", "their side", "i can wait");
			case 'm' -> sign(level, pos, "M.");
			case 'n' -> sign(level, pos, "R.");
			// Three markers. Four names in the books. Nobody comments on it.
			case 'p' -> sign(level, pos, "the little one");
			case '_' -> roofDeck(level, pos, random);
			case '/' -> eave(level, pos, x, z);
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
	/**
	 * A chest, its book if it has one, and then whatever the house had left.
	 *
	 * The book goes in first and into slot zero, and the loot only ever fills
	 * slots that are still empty. That ordering is the guarantee: no roll of
	 * the dice can leave the sealed room without its tally, which is the one
	 * thing in this building that must never be missing.
	 */
	private static void chest(ServerLevel level, BlockPos pos,
	                          @org.jspecify.annotations.Nullable ItemStack book,
	                          RandomSource random) {
		set(level, pos, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
			return;
		}
		if (book != null) {
			chest.setItem(0, book);
		}
		Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
	}

	/**
	 * What is written where.
	 *
	 * The family write like people. He writes in the sealed room, in the
	 * lowercase-no-punctuation voice LORE.md gives him, and the change of hand
	 * halfway through the building is the loudest thing in it — nobody has to
	 * be told that two different people wrote these.
	 */
	private static void sign(ServerLevel level, BlockPos pos, String... lines) {
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
	 * The roof, with weather in it.
	 *
	 * Holes rather than a clean deck, because a sound roof on a house nobody
	 * has lived in for years is the detail that would undo all the others. The
	 * gaps are what let daylight into the main room in stripes, which is the
	 * only lighting effect in the building and it is free.
	 */
	private static void roofDeck(ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(9) == 0) {
			return;   // fallen in
		}
		set(level, pos, Blocks.SPRUCE_PLANKS.defaultBlockState());
	}

	/**
	 * The overhang, one block out from the wall all the way round.
	 *
	 * Facing is worked out from which side the deck is on rather than written
	 * into the map, so the eave stays correct if the footprint ever changes.
	 * Without it the roof meets the walls flush and the whole thing reads as a
	 * box with a lid.
	 */
	private static void eave(ServerLevel level, BlockPos pos, int x, int z) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			int nx = x + facing.getStepX();
			int nz = z + facing.getStepZ();
			if (nz < 0 || nz >= depth() || nx < 0 || nx >= width()) {
				continue;
			}
			if (LAYERS[ROOF_LAYER][nz].charAt(nx) == '_') {
				set(level, pos, Blocks.SPRUCE_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
				return;
			}
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
		BlockPos hole = origin.offset(5, 0, 12);

		// The room, hollowed under the store at the back of the house.
		for (int x = 3; x <= 9; x++) {
			for (int z = 10; z <= 14; z++) {
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
		// Carved back out of the room after it is hollowed, not before.
		for (int y = -1; y >= -3; y--) {
			set(level, origin.offset(5, y, 11), Blocks.COBBLESTONE.defaultBlockState());
			set(level, origin.offset(5, y, 12), Blocks.LADDER.defaultBlockState()
				.setValue(LadderBlock.FACING, Direction.SOUTH));
		}

		// And then it stops being a cellar.
		//
		// Everything past this point is carved rather than built, and there is
		// a great deal more of it than the house above needs. That contrast is
		// the whole point of the building: a farmhouse with a hole in the floor
		// and sixty blocks of dig under it. The house is the smaller half.
		Undercroft.dig(level, origin.offset(6, -3, 14), random);
	}

	/**
	 * Where the floor goes.
	 *
	 * Averaged over the BUILDING's columns only, not the whole map, so the
	 * house is not lifted or sunk by whatever the ground does out where the
	 * graves are.
	 */
	public static int floorHeightAt(ServerLevel level, int originX, int originZ) {
		long total = 0;
		int count = 0;
		for (int z = HOUSE_Z0; z <= HOUSE_Z1; z += 3) {
			for (int x = HOUSE_X0; x <= HOUSE_X1; x += 3) {
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
