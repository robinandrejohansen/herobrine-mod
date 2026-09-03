package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import java.util.ArrayList;
import java.util.List;

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
		"  #################          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
		"  #...............#          ",
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
		"  #################          ",
		"  #AFc1   PWB  B P#          ",
		"  #    h   W     3#          ",
		"  #   hTh         #          ",
		"  #L   h   W x    #          ",
		"  #S     x W      #          ",
		"  #SS    qrWMMMMMM#          ",
		"  #WWW WWWWW  we  #          ",
		"  #A      CW    x #          ",
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


	private static final String[][] LAYERS = {
		GROUND, COURSE_ONE, COURSE_TWO, COURSE_THREE, COURSE_FOUR
	};

	/** The roof is built rather than mapped — see gable(). */
	private static final int EAVE = 5;
	/** The ridge runs along the middle of the depth. */
	private static final int RIDGE_Z = (HOUSE_Z0 + HOUSE_Z1) / 2;

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
		gable(level, origin, random);
		wing(level, origin, random);
		chimney(level, origin, random);
		porch(level, origin, random);
		trim(level, origin, random);
		cellar(level, origin, random);
		// AND THE GROUND IT STANDS ON.
		//
		// levelGround flattens the footprint, which is what stops the house sinking
		// into a slope — and leaves it standing on a suspiciously tidy platform of
		// grass. This is the other half of that: the apron round the walls goes back
		// to being ground, with the boulders and moss and flowers coming up to meet
		// the plinth, and a working yard off the front.
		//
		// Both are deliberately OUTSIDE the char grid above. That grid is a set of
		// elevations and it is the wrong tool for anything organic — a wall is a
		// drawing and a garden is a scatter, and trying to express one in the other
		// is how you get a flowerbed with corners.
		int wide = width();
		int deep = depth();
		BlockPos middle = origin.offset(wide / 2, 0, deep / 2);
		Grounds.dress(level, middle, Math.max(wide, deep) / 2 + 2,
			Math.max(wide, deep) / 2 + 14, random);
		Grounds.yard(level, origin.offset(wide / 2, 0, -4), Direction.NORTH, random);
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
			// AND THE WING, which stands eight blocks past the old east wall.
			// Levelling only the main block left the whole workshop hanging off
			// the side of whatever the terrain happened to do out there.
			int east = z >= WING_Z0 - 1 && z <= WING_Z1 + 1 ? WING_X1 + 1 : HOUSE_X1 + 1;
			for (int x = HOUSE_X0 - 1; x <= east; x++) {
				BlockPos floor = origin.offset(x, 0, z);

				// Down to whatever the ground actually was, in cobblestone.
				// This is the beard: a house on a slope gets a stone footing
				// under its low side, which is what a real one would have, and
				// it reads as part of the building rather than as a lump of
				// dirt somebody dumped there.
				// Three at most. A footing is a course of stone under the low
				// corner of a building; anything deeper is a plinth, and a
				// plinth means the site was wrong and should have been
				// refused rather than propped up.
				for (int down = 1; down <= 3; down++) {
					BlockPos below = floor.below(down);
					if (!level.getBlockState(below).isAir()
						&& level.getFluidState(below).isEmpty()) {
						break;
					}
					set(level, below, Blocks.COBBLESTONE.defaultBlockState());
				}
				// Up past the ridge, so the roof is never built through a tree
				// that was standing where the gable now is.
				for (int up = 0; up <= EAVE + 6; up++) {
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
		return Ground.topOf(level, x, z);
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
			case 'g' -> set(level, pos, Blocks.GLASS.defaultBlockState());
			case 'b' -> set(level, pos, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HALF, Half.BOTTOM));
			case 'D' -> door(level, pos, layer);
			case 'B' -> bed(level, pos);
			case 'C' -> chest(level, pos, null, random);
			case '1' -> chest(level, pos, null, random);
			// BOOK ONE GOES IN THE CHEST WITH THE MAP, AND THE BOOK SAYS SO.
			//
			// "There is a map in this chest. I drew it." — HouseBooks.one(), page
			// one. leaveTheWay has always put the map in the same container as the
			// book; this is the first time a book has depended on it, so the two
			// cannot be separated any more without making the text wrong.
			//
			// chest() is the helper every container in the house goes through, so it
			// takes null for the other four. Five copies of the introduction is not
			// generosity, it is noise.
			case '2' -> chest(level, pos, HouseBooks.one(), random);
			case '3' -> chest(level, pos, null, random);
			case '4' -> chest(level, pos, null, random);
			case 'c' -> set(level, pos, Blocks.CRAFTING_TABLE.defaultBlockState());
			case 'F' -> set(level, pos, Blocks.FURNACE.defaultBlockState());
			case 'A' -> set(level, pos, Blocks.BARREL.defaultBlockState());
			case 'S' -> set(level, pos, Blocks.BOOKSHELF.defaultBlockState());
			case 'L' -> lectern(level, pos);
			case 'T' -> table(level, pos);
			case 'h' -> set(level, pos, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, seatFacing(x, z)));
			case 'P' -> set(level, pos, Blocks.POTTED_DEAD_BUSH.defaultBlockState());
			case 't' -> torch(level, pos, Blocks.WALL_TORCH, Blocks.TORCH);
			case 'r' -> torch(level, pos, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_TORCH);
			case 'l' -> set(level, pos, Blocks.AIR.defaultBlockState());   // the way down
			case 'x' -> set(level, pos, Blocks.COBWEB.defaultBlockState());
			// A bone block in somebody's kitchen is a decorating tic rather
			// than a detail — it is a big pale skeleton-textured cube and it
			// reads as loot, not as a house that was lived in. A barrel is what
			// was actually there.
			case 'k' -> set(level, pos, Blocks.BARREL.defaultBlockState());
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
			default -> { }
		}
	}

	/**
	 * Which way a chair points, from what it is standing next to.
	 *
	 * A stair used as a chair puts its TALL BACK on the side its facing names,
	 * so a chair has to face AWAY from the table for its back to be outside and
	 * its seat to open toward the food. These had no facing at all and so all
	 * pointed north, which put three of the four with their backs to dinner.
	 *
	 * Read off the map rather than written per chair, so moving the table moves
	 * the chairs with it.
	 */
	private static Direction seatFacing(int x, int z) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int nx = x + side.getStepX();
			int nz = z + side.getStepZ();
			if (nz < 0 || nz >= COURSE_ONE.length || nx < 0 || nx >= COURSE_ONE[nz].length()) {
				continue;
			}
			if (COURSE_ONE[nz].charAt(nx) == 'T') {
				return side.getOpposite();
			}
		}
		return Direction.SOUTH;
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
			// AND A SECOND COPY OF BOOK ONE, OPEN, ON A STAND, IN THE MAIN ROOM.
			//
			// Deliberate duplication and the only one on the trail. Ten numbered
			// books are worth nothing to somebody who never found number one, and a
			// chest is missable in a way an open lectern is not.
			lectern.setBook(HouseBooks.one());
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
	 * lowercase-no-punctuation voice README.md gives him, and the change of hand
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
	 * A pitched roof, which is most of what makes it look like a house.
	 *
	 * The first version was a flat deck of planks and it was the single most
	 * generated-looking thing in the building — nobody has ever built a flat
	 * roof on a farmhouse, and a box with a lid on it reads as a box. This is a
	 * proper gable: two slopes stepping up two rows at a time to a ridge down
	 * the middle, with the ends filled in as triangles and an overhang past the
	 * walls.
	 *
	 * Holes still, roughly one panel in ten. Daylight coming into the main room
	 * in stripes is the only lighting effect the building has and it is free.
	 */
	private static void gable(ServerLevel level, BlockPos origin, RandomSource random) {
		for (int h = 0; h <= 3; h++) {
			int y = EAVE + h;
			int near = HOUSE_Z0 - 1 + h * 2;
			int far = HOUSE_Z1 + 1 - h * 2;

			for (int x = HOUSE_X0 - 1; x <= HOUSE_X1 + 1; x++) {
				// Two rows a side per level: a stair on the low edge so the
				// slope actually steps, a full course behind it.
				slate(level, origin.offset(x, y, near), Direction.NORTH, true, random);
				slate(level, origin.offset(x, y, near + 1), Direction.NORTH, false, random);
				slate(level, origin.offset(x, y, far), Direction.SOUTH, true, random);
				slate(level, origin.offset(x, y, far - 1), Direction.SOUTH, false, random);
			}

			// The gable ends: the triangle of wall left between the two slopes.
			for (int z = near + 2; z <= far - 2; z++) {
				set(level, origin.offset(HOUSE_X0, y, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
				set(level, origin.offset(HOUSE_X1, y, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		// The ridge itself, capping where the two slopes meet.
		for (int x = HOUSE_X0 - 1; x <= HOUSE_X1 + 1; x++) {
			slate(level, origin.offset(x, EAVE + 3, RIDGE_Z), Direction.NORTH, false, random);
		}
	}

	private static void slate(ServerLevel level, BlockPos at, Direction fall,
	                          boolean stepped, RandomSource random) {
		if (random.nextInt(10) == 0) {
			return;   // fallen in
		}
		set(level, at, stepped
			? Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, fall.getOpposite())
			: Blocks.SPRUCE_PLANKS.defaultBlockState());
	}

	/**
	 * THE EAST WING, AND THE REASON THE HOUSE NEEDED ONE.
	 *
	 * Seventeen by thirteen under one ridge is a hall, and a hall is one room you
	 * take in from the doorway. Nothing in it is discovered — you see all of it at
	 * once and then you are finished with the building.
	 *
	 * A second mass fixes that on its own, and it fixes it in the way real houses
	 * do: somebody needed more room, so they put more room on the side, and it does
	 * not match. The wing is lower, its ridge crosses the main one, its walls are
	 * timber over stone where the house is stone throughout, and the only way into
	 * it is a door punched through what used to be an outside wall. You have to go
	 * round a corner to find out it is there.
	 *
	 * IT IS THE WORKSHOP. The house is where the family ate; this is where the work
	 * was done, and the difference in what is standing in the two rooms says more
	 * about who lived here than any of the books do.
	 *
	 * Built as code rather than mapped, because the character maps are validated on
	 * class load against one width and one depth — a wing bolted into them means
	 * editing every layer of every row, and a miscount there silently shifts half
	 * the house sideways.
	 */
	private static final int WING_X0 = 19;
	private static final int WING_X1 = 26;
	private static final int WING_Z0 = 4;
	private static final int WING_Z1 = 11;
	/** Lower than the house, which is what stops it reading as one building. */
	private static final int WING_EAVE = 4;

	private static void wing(ServerLevel level, BlockPos origin, RandomSource random) {
		int mid = (WING_Z0 + WING_Z1) / 2;

		for (int x = WING_X0; x <= WING_X1; x++) {
			for (int z = WING_Z0; z <= WING_Z1; z++) {
				boolean wall = x == WING_X0 || x == WING_X1
					|| z == WING_Z0 || z == WING_Z1;
				boolean corner = (x == WING_X0 || x == WING_X1)
					&& (z == WING_Z0 || z == WING_Z1);
				// A floor of two woods, laid in no pattern, the way a floor put
				// down out of what was left over actually looks.
				set(level, origin.offset(x, 0, z), random.nextInt(4) == 0
					? Blocks.DARK_OAK_PLANKS.defaultBlockState()
					: Blocks.SPRUCE_PLANKS.defaultBlockState());
				for (int y = 1; y <= 3; y++) {
					BlockPos at = origin.offset(x, y, z);
					if (!wall) {
						set(level, at, Blocks.AIR.defaultBlockState());
						continue;
					}
					if (corner) {
						// Stripped log posts at the corners. One block, and it is
						// the whole difference between a shed and a frame.
						set(level, at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
					} else if (y == 1) {
						set(level, at, weathered(random));      // stone to the waist
					} else {
						set(level, at, random.nextInt(5) == 0
							? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
							: Blocks.SPRUCE_PLANKS.defaultBlockState());
					}
				}
			}
		}

		// WINDOWS, and shutters on the ones that still have them.
		for (int z : new int[] { WING_Z0 + 2, WING_Z1 - 2 }) {
			hole(level, origin.offset(WING_X1, 2, z), Blocks.GLASS.defaultBlockState());
			set(level, origin.offset(WING_X1 + 1, 2, z),
				Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
					.setValue(BlockStateProperties.OPEN, true));
		}
		hole(level, origin.offset(WING_X0 + 3, 2, WING_Z0),
			Blocks.GLASS.defaultBlockState());

		// THE DOOR THROUGH WHAT USED TO BE AN OUTSIDE WALL.
		for (int y = 1; y <= 2; y++) {
			set(level, origin.offset(HOUSE_X1, y, mid), Blocks.AIR.defaultBlockState());
			set(level, origin.offset(WING_X0, y, mid), Blocks.AIR.defaultBlockState());
		}
		// Hung facing east, into the workshop. The map's door() helper is hard
		// wired to north for the front of the house and places both halves off
		// the lower call, so the second call was a no-op and the first would have
		// hung this one sideways in the wall.
		BlockState leaf = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		set(level, origin.offset(WING_X0, 1, mid), leaf.setValue(
			BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		set(level, origin.offset(WING_X0, 2, mid), leaf.setValue(
			BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		set(level, origin.offset(HOUSE_X1, 3, mid),
			Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());

		// WHAT THE WORK WAS. Nothing here is decoration — a smith's corner, a
		// stone bench, wool being made, and a fire that has been out a long time.
		set(level, origin.offset(WING_X0 + 1, 1, WING_Z0 + 1),
			Blocks.SMITHING_TABLE.defaultBlockState());
		set(level, origin.offset(WING_X0 + 2, 1, WING_Z0 + 1),
			Blocks.ANVIL.defaultBlockState());
		set(level, origin.offset(WING_X0 + 1, 1, WING_Z0 + 2),
			Blocks.GRINDSTONE.defaultBlockState());
		set(level, origin.offset(WING_X1 - 1, 1, WING_Z0 + 1),
			Blocks.STONECUTTER.defaultBlockState());
		set(level, origin.offset(WING_X1 - 1, 1, WING_Z1 - 1),
			Blocks.LOOM.defaultBlockState());
		set(level, origin.offset(WING_X1 - 2, 1, WING_Z1 - 1),
			Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
		set(level, origin.offset(WING_X0 + 1, 1, WING_Z1 - 1),
			Blocks.CAULDRON.defaultBlockState());
		set(level, origin.offset(WING_X0 + 2, 1, WING_Z1 - 1),
			Blocks.COMPOSTER.defaultBlockState());
		for (int i = 0; i < 3; i++) {
			set(level, origin.offset(WING_X1 - 1, 1 + i, WING_Z0 + 3),
				Blocks.BARREL.defaultBlockState());
		}
		set(level, origin.offset(WING_X0 + 4, 1, mid + 1),
			Blocks.HAY_BLOCK.defaultBlockState());
		chest(level, origin.offset(WING_X0 + 4, 1, WING_Z1 - 1), null, random);

		// The fire that heated the work, and it is out.
		BlockPos hearth = origin.offset(WING_X0 + 3, 1, WING_Z0 + 1);
		set(level, hearth, Blocks.CAMPFIRE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, false));
		for (int dx = -1; dx <= 1; dx++) {
			set(level, hearth.offset(dx, -1, 0), Blocks.COBBLESTONE.defaultBlockState());
		}

		// Light, hung rather than stuck to the walls.
		for (int z : new int[] { WING_Z0 + 2, WING_Z1 - 2 }) {
			BlockPos hook = origin.offset(WING_X0 + 4, 3, z);
			set(level, hook, Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, hook.below(), Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));
		}

		// AND ITS OWN ROOF, crossing the main one rather than continuing it.
		for (int h = 0; h <= 2; h++) {
			int y = WING_EAVE + h;
			int near = WING_Z0 - 1 + h * 2;
			int far = WING_Z1 + 1 - h * 2;
			for (int x = WING_X0 - 1; x <= WING_X1 + 1; x++) {
				slate(level, origin.offset(x, y, near), Direction.NORTH, true, random);
				slate(level, origin.offset(x, y, near + 1), Direction.NORTH, false, random);
				slate(level, origin.offset(x, y, far), Direction.SOUTH, true, random);
				slate(level, origin.offset(x, y, far - 1), Direction.SOUTH, false, random);
			}
			for (int z = near + 2; z <= far - 2; z++) {
				set(level, origin.offset(WING_X1, y, z),
					Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		for (int x = WING_X0 - 1; x <= WING_X1 + 1; x++) {
			slate(level, origin.offset(x, WING_EAVE + 2, mid), Direction.NORTH, false, random);
		}
	}

	/** Take a block out and put a smaller one back, which is what a window is. */
	private static void hole(ServerLevel level, BlockPos at, BlockState pane) {
		set(level, at, pane);
	}

	/**
	 * WHAT TURNS A BOX INTO A BUILDING, AND IT IS ALL ONE BLOCK THICK.
	 *
	 * Posts at the corners, boxes under the windows, a paved apron at the foot of
	 * the walls, ivy up the side nobody uses and a light either side of the door.
	 * None of it is structural and all of it is the difference between a shape and
	 * a place — a flat wall has no scale, and the moment there is something small
	 * on it the eye has something to measure the rest against.
	 */
	private static void trim(ServerLevel level, BlockPos origin, RandomSource random) {
		// Corner posts, full height, on both masses.
		for (int x : new int[] { HOUSE_X0, HOUSE_X1 }) {
			for (int z : new int[] { HOUSE_Z0, HOUSE_Z1 }) {
				for (int y = 1; y <= 4; y++) {
					set(level, origin.offset(x, y, z),
						Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
				}
			}
		}

		// A paved apron, so the walls meet the ground on something.
		for (int x = HOUSE_X0 - 1; x <= WING_X1 + 1; x++) {
			for (int z = HOUSE_Z0 - 1; z <= HOUSE_Z1 + 1; z++) {
				boolean rim = x == HOUSE_X0 - 1 || z == HOUSE_Z0 - 1 || z == HOUSE_Z1 + 1
					|| (x == HOUSE_X1 + 1 && (z < WING_Z0 - 1 || z > WING_Z1 + 1))
					|| x == WING_X1 + 1;
				if (!rim) {
					continue;
				}
				BlockPos at = origin.offset(x, 0, z);
				if (!level.getBlockState(at).isSolid()) {
					continue;
				}
				set(level, at, switch (random.nextInt(6)) {
					case 0, 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
					case 2 -> Blocks.ANDESITE.defaultBlockState();
					case 3 -> Blocks.GRAVEL.defaultBlockState();
					default -> Blocks.COBBLESTONE.defaultBlockState();
				});
			}
		}

		// Window boxes on the south face — a trapdoor shelf with something dead
		// in it. Nobody has watered these since whatever happened here.
		for (int x = HOUSE_X0 + 3; x <= HOUSE_X1 - 3; x += 4) {
			BlockPos ledge = origin.offset(x, 1, HOUSE_Z1 + 1);
			if (!level.getBlockState(ledge).isAir()) {
				continue;
			}
			set(level, ledge, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
				.setValue(BlockStateProperties.OPEN, true));
			set(level, ledge.above(), random.nextBoolean()
				? Blocks.POTTED_DEAD_BUSH.defaultBlockState()
				: Blocks.POTTED_FERN.defaultBlockState());
		}

        // Ivy up the north side, which is the side that never sees the sun and
        // the side nobody has any reason to walk along.
		for (int x = HOUSE_X0; x <= HOUSE_X1; x++) {
			for (int y = 1; y <= 4; y++) {
				BlockPos face = origin.offset(x, y, HOUSE_Z0 - 1);
				if (level.getBlockState(face).isAir() && random.nextInt(3) != 0) {
					set(level, face, Blocks.VINE.defaultBlockState().setValue(
						net.minecraft.world.level.block.VineBlock
							.PROPERTY_BY_DIRECTION.get(Direction.SOUTH), true));
				}
			}
		}

		// And a light either side of the door.
		for (int side = -1; side <= 1; side += 2) {
			BlockPos post = origin.offset(DOOR_X + side * 2, 4, HOUSE_Z1 + 2);
			set(level, post, Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));
		}
	}

	/**
	 * A chimney, up the outside of the north wall.
	 *
	 * Rises from the hearth inside and stands a good way clear of the ridge,
	 * because that is where the smoke has to get to. It is also the only part
	 * of the building visible over a hedge, which makes it the thing a player
	 * sees first — a house you spot by its chimney has been found rather than
	 * placed in front of you.
	 */
	private static void chimney(ServerLevel level, BlockPos origin, RandomSource random) {
		int x = 4;
		for (int y = 1; y <= EAVE + 5; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				set(level, origin.offset(x + dx, y, HOUSE_Z0),
					y > EAVE + 4 ? Blocks.COBBLESTONE_SLAB.defaultBlockState()
						: weathered(random));
			}
		}
		// Hollow, so it reads as a flue rather than a buttress.
		for (int y = 2; y <= EAVE + 4; y++) {
			set(level, origin.offset(x, y, HOUSE_Z0 - 1), Blocks.AIR.defaultBlockState());
		}
	}

	/**
	 * A porch over the door.
	 *
	 * Two posts and a lean-to, and it does more for the front of the building
	 * than anything else its size. A door in a flat wall is an opening; a door
	 * under a porch is an entrance somebody stood in out of the rain.
	 */
	private static void porch(ServerLevel level, BlockPos origin, RandomSource random) {
		int z = HOUSE_Z1 + 1;
		for (int side = -1; side <= 1; side += 2) {
			BlockPos post = origin.offset(DOOR_X + side * 2, 0, z + 1);
			for (int y = 1; y <= 3; y++) {
				set(level, post.above(y), Blocks.SPRUCE_FENCE.defaultBlockState());
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			set(level, origin.offset(DOOR_X + dx, 4, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
			set(level, origin.offset(DOOR_X + dx, 4, z + 1), Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		}
		// A worn step, because the ground in front of a door always is.
		for (int dx = -1; dx <= 1; dx++) {
			BlockPos step = origin.offset(DOOR_X + dx, 0, z);
			set(level, step, Blocks.COBBLESTONE_SLAB.defaultBlockState());
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

		// AND THE HOLE IS FILLED IN.
		//
		// It used to be an open square in the floor at the back of the room, which
		// is the first thing anybody sees on walking in — and it gave away the one
		// thing in the whole mod that should have to be earned. The way to his
		// dimension was a visible hole in the first building of the story.
		//
		// So the floor is whole. The stair is still under it, exactly as it was;
		// there is simply a plank over the top and no reason to suspect it. What
		// tells you is the book at the END of the sequence — five buildings later,
		// after the threshold, when the last thing you find sends you back to the
		// first thing you found. See Dwellings.theLastWord.
		//
		// ONE BLOCK, NOT A PUZZLE. When they know, they break a plank and drop in.
		// Anything cleverer — a lever, a trapped chest, a pattern — would be a
		// riddle, and the discovery is meant to be the reward rather than the lock.
		set(level, hole, Blocks.SPRUCE_PLANKS.defaultBlockState());
		set(level, origin.offset(5, 0, 13), Blocks.SPRUCE_PLANKS.defaultBlockState());

		// A STAIR DOWN, NOT A LADDER — AND THE FLOOR PLANE IS WHY IT MATTERED.
		//
		// The house floor is the block layer at y=0, so a body stands at y=1 and the
		// cellar floor is at y=-3. That is a four-block drop, and a ladder is not a
		// route: ground pathfinding offers +1 up and no rungs, so he could FALL into
		// his own undercroft and never get out of it.
		//
		// Four treads, each exactly one down and one across, walkable in both
		// directions, landing on the block Undercroft.dig opens from.
		//
		//   y=+1  ░        ← the house, standing on the floor
		//   y= 0  ▓░       ← through the hole at z=12
		//   y=-1   ▓░      ← z=13
		//   y=-2    ▓░     ← z=14
		//   y=-3     ▓▒▒▒  ← turn east onto the cellar floor
		//
		set(level, origin.offset(5, -1, 12), Blocks.COBBLESTONE.defaultBlockState());
		set(level, origin.offset(5, -2, 13), Blocks.COBBLESTONE.defaultBlockState());
		set(level, origin.offset(5, -3, 14), Blocks.COBBLESTONE.defaultBlockState());

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
		List<Integer> heights = new ArrayList<>();
		for (int z = HOUSE_Z0; z <= HOUSE_Z1; z += 2) {
			for (int x = HOUSE_X0; x <= HOUSE_X1; x += 2) {
				heights.add(Ground.topOf(level, originX + x, originZ + z));
			}
		}
		java.util.Collections.sort(heights);
		// Median rather than mean, because one boulder or one dip should not
		// lift or drop the whole building — and the median of a lumpy field is
		// the height most of the house is actually standing on.
		//
		// And ON that block rather than above it, which is the bug this fixes.
		// Averaging ground-plus-one put the floor a block over the average, so
		// on any slope the low half of the house hung in the air and got a
		// ten-deep cobblestone plinth to stand on. A house should be bedded
		// INTO a hill and have the high side dug out, not perched on top of it
		// on a pillar of stone.
		return heights.get(heights.size() / 2);
	}

	public static int doorX() {
		return DOOR_X;
	}
}
