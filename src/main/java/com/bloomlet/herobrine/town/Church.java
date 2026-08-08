package com.bloomlet.herobrine.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The church, and the way down.
 *
 * The tallest, longest, coldest building in the town and the only one made
 * entirely of stone. Everything else here is spruce framing over plaster
 * because that is what people could afford; this took the whole settlement a
 * generation, and it should look like it.
 *
 * A LONG NAVE IS THE ONLY THING THAT MATTERS ARCHITECTURALLY. Height impresses
 * in a screenshot and length works in play: twenty-three blocks of pews with
 * the altar at the far end means the player walks the building, and by the time
 * they reach the front they have been inside long enough for the quiet to
 * register. A square room the same volume would be crossed in three seconds and
 * remembered as a big room.
 *
 * AND IT IS ONE OF THE TWO WAYS INTO WHAT IS UNDER THE TOWN. Behind the altar,
 * a stair that is not hidden by a mechanism — no lever, no pressure plate,
 * nothing to solve. It is hidden by being somewhere nobody looks, which is the
 * only kind of secret that works twice: the player who finds it feels observant
 * rather than lucky, and the player who does not walks out past it.
 *
 * The other way in is the well in the square, and the two are deliberately
 * unalike. One is a staircase behind an altar and one is a swim.
 */
public final class Church {
	private Church() {}

	public static final int WIDTH = 15;
	public static final int DEPTH = 25;

	private static final String[] GROUND = {
		"               ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		" ############# ",
		"               ",
	};

	/**
	 * Floor level. The chancel is the top five rows, the nave is the middle,
	 * and the porch under the tower is the bottom three.
	 */
	private static final String[] NAVE = {
		"               ",
		" LWWWWWWWWWWWL ",
		" W     V     W ",
		" W    aaa    W ",
		" W  k     k  W ",
		" WWWWW   WWWWW ",
		" W           W ",
		" W ppp   ppp W ",
		" W ppp   ppp W ",
		" W           W ",
		" W ppp   ppp W ",
		" W ppp   ppp W ",
		" W           W ",
		" W ppp   ppp W ",
		" W ppp   ppp W ",
		" W           W ",
		" W ppp   ppp W ",
		" W ppp   ppp W ",
		" W           W ",
		" WWWWW   WWWWW ",
		" W  k     k  W ",
		" W           W ",
		" LWWWWWDWWWWWL ",
		"               ",
		"               ",
	};

	private static final String[] MID = {
		"               ",
		" LWgWWWWWWWgWL ",
		" W           W ",
		" W           W ",
		" g           g ",
		" WWWWW   WWWWW ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" WWWWW   WWWWW ",
		" W           W ",
		" g           g ",
		" LWWWWWWWWWWWL ",
		"               ",
		"               ",
	};

	private static final String[] UPPER = {
		"               ",
		" LWgWWWWWWWgWL ",
		" W           W ",
		" W           W ",
		" g           g ",
		" WWWWW   WWWWW ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" g           g ",
		" W           W ",
		" W           W ",
		" WWWWW   WWWWW ",
		" W           W ",
		" g           g ",
		" LWWWWWWWWWWWL ",
		"               ",
		"               ",
	};

	private static final String[] EAVES = {
		"               ",
		" LWWWWWWWWWWWL ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" WWWWWWWWWWWWW ",
		" LWWWWWWWWWWWL ",
		"               ",
		"               ",
	};

	private static final String[][] LAYERS = { GROUND, NAVE, MID, UPPER, EAVES };

	/** Where the stair down starts: dead behind the altar. */
	private static final int STAIR_X = 7;
	private static final int STAIR_Z = 2;

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		if (!Blueprint.place(level, corner, LAYERS, facing,
				(l, at, c, dir, mx, mz) -> set(l, at, c, dir, mx, mz, random))) {
			return false;
		}
		roof(level, corner, facing);
		tower(level, corner, facing, random);
		return true;
	}

	/** The stair behind the altar, in world coordinates, for the undercity. */
	public static BlockPos crypt(BlockPos corner, Direction facing) {
		return corner.offset(
			Blueprint.spinX(STAIR_X, STAIR_Z, WIDTH, DEPTH, facing), 1,
			Blueprint.spinZ(STAIR_X, STAIR_Z, WIDTH, DEPTH, facing));
	}

	private static void set(ServerLevel level, BlockPos at, char c, Direction facing,
	                        int x, int z, RandomSource random) {
		switch (c) {
			case '#' -> Blueprint.put(level, at, floor(random, x, z));
			case 'W' -> Blueprint.put(level, at, stone(random));
			case 'L' -> Blueprint.put(level, at, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			// Tall coloured windows, the only real colour in the town.
			case 'g' -> Blueprint.put(level, at, Blocks.STAINED_GLASS_PANE
				.pick(glass(random)).defaultBlockState());
			case 'D' -> door(level, at, facing);
			// Pews. They face the altar, which is toward map-north here.
			case 'p' -> Blueprint.put(level, at, Blocks.SPRUCE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.SOUTH, facing)));
			case 'a' -> Blueprint.put(level, at, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			// The reredos: a wall of candles behind the altar, all unlit.
			case 'V' -> Blueprint.put(level, at, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
			case 'k' -> Blueprint.put(level, at, Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));
			default -> { }
		}
	}

	/**
	 * A steep double pitch running the length of the nave.
	 *
	 * Along the building rather than across it, which is the whole difference
	 * between a church roof and a very long shed: the ridge line drawing the
	 * eye toward the altar end is doing the same job inside and out.
	 */
	private static void roof(ServerLevel level, BlockPos corner, Direction facing) {
		for (int step = 0; step <= 6; step++) {
			for (int z = 1; z <= 22; z++) {
				for (int x : new int[] { step + 1, WIDTH - 2 - step }) {
					if (x < 0 || x >= WIDTH) {
						continue;
					}
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 5 + step,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, Blocks.DEEPSLATE_TILES.defaultBlockState());
				}
			}
		}
	}

	/**
	 * The tower, over the porch, with the bell in it.
	 *
	 * The one thing in the settlement visible from outside the wall, and the
	 * reason the town reads as a town from the ridge rather than as a compound.
	 */
	private static void tower(ServerLevel level, BlockPos corner, Direction facing,
	                          RandomSource random) {
		for (int up = 5; up <= 17; up++) {
			for (int x = 4; x <= 10; x++) {
				for (int z = 19; z <= 22; z++) {
					boolean edge = x == 4 || x == 10 || z == 19 || z == 22;
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), up,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					if (!edge) {
						Blueprint.put(level, at, up == 17
							? Blocks.DEEPSLATE_TILES.defaultBlockState()
							: Blocks.AIR.defaultBlockState());
						continue;
					}
					// The belfry openings, near the top.
					boolean louvre = up >= 13 && up <= 15 && (x == 7 || z == 20 || z == 21);
					Blueprint.put(level, at, louvre
						? Blocks.STONE_BRICK_WALL.defaultBlockState()
						: stone(random));
				}
			}
		}
		// The bell.
		BlockPos bell = corner.offset(
			Blueprint.spinX(7, 20, WIDTH, DEPTH, facing), 15,
			Blueprint.spinZ(7, 20, WIDTH, DEPTH, facing));
		Blueprint.put(level, bell, Blocks.BELL.defaultBlockState()
			.setValue(BlockStateProperties.BELL_ATTACHMENT,
				net.minecraft.world.level.block.state.properties.BellAttachType.CEILING));

		// And a spire, because a flat-topped tower is a keep.
		for (int up = 18; up <= 23; up++) {
			int inset = (up - 18) / 2;
			for (int x = 5 + inset; x <= 9 - inset; x++) {
				for (int z = 20 + inset; z <= 21 - inset; z++) {
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), up,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, Blocks.DEEPSLATE_TILES.defaultBlockState());
				}
			}
		}
	}

	private static void door(ServerLevel level, BlockPos at, Direction facing) {
		BlockState leaf = Blocks.DARK_OAK_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Blueprint.turned(Direction.SOUTH, facing));
		Blueprint.put(level, at, leaf.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER));
		Blueprint.put(level, at.above(), leaf.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER));
	}

	/** Worn flags, with a strip of polished stone up the middle as an aisle. */
	private static BlockState floor(RandomSource random, int x, int z) {
		if (x == 7) {
			return Blocks.POLISHED_ANDESITE.defaultBlockState();
		}
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.ANDESITE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static DyeColor glass(RandomSource random) {
		DyeColor[] panes = {
			DyeColor.BROWN, DyeColor.ORANGE, DyeColor.YELLOW,
			DyeColor.RED, DyeColor.BROWN,
		};
		return panes[random.nextInt(panes.length)];
	}

	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 4) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
