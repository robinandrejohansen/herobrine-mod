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

	public static final int WIDTH = 19;
	public static final int DEPTH = 31;

	private static final String[] GROUND = {
		"                   ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		" ################# ",
		"                   ",
	};

	/**
	 * Floor level. The chancel is the top five rows, the nave is the middle,
	 * and the porch under the tower is the bottom three.
	 */
	/**
	 * THE FLOOR OF THE CHURCH, AND THREE THINGS WERE WRONG WITH IT.
	 *
	 * THE AISLE WAS ONE BLOCK WIDE. Nineteen blocks across, ten rows of pews, and
	 * a single-file gap down the middle — which is not an aisle, it is a corridor
	 * between two blocks of furniture. Three now, which is the narrowest thing
	 * anybody would call an aisle and wide enough for two people to pass.
	 *
	 * THE WOOL HUNG IN IT. The banner glyph sat in the exact centre column, so the
	 * only walkable line in the building was blocked at head height by cloth. It
	 * hangs against the side walls now, which is where a hanging is hung.
	 *
	 * AND THE WINDOWS WERE ALL ABOVE EYE LEVEL. There was stained glass on every
	 * layer except this one, and this is the layer a standing player's head is on
	 * — so from inside, the church had solid walls and a lot of coloured light
	 * coming from somewhere you could not look out of. Glass on the empty rows
	 * between the pews now, at the height somebody would actually see through.
	 */
	private static final String[] NAVE = {
		"                   ",
		" LWWWWWWWWWWWWWWWL ",
		" W       V       W ",
		" W      aaa      W ",
		" W   c       c   W ",
		" WWWWW       WWWWW ",
		" g               g ",
		" W  pppp   pppp  W ",
		" W  pppp   pppp  W ",
		" g               g ",
		" W  pppp   pppp  W ",
		" W  pppp   pppp  W ",
		" g               g ",
		" W  pppp   pppp  W ",
		" W  pppp   pppp  W ",
		" g               g ",
		" W  pppp   pppp  W ",
		" W  pppp   pppp  W ",
		" g               g ",
		" W  pppp   pppp  W ",
		" W  pppp   pppp  W ",
		" W               W ",
		" WWWWW       WWWWW ",
		" W   c       c   W ",
		" W               W ",
		" W               W ",
		" LWWWWWW D WWWWWWL ",
		"                   ",
		"                   ",
		"                   ",
		"                   ",
	};

	private static final String[] MID = {
		"                   ",
		" LWgWWWWWWWWWWWgWL ",
		" W               W ",
		" W               W ",
		" g   m       m   g ",
		" WWWWW       WWWWW ",
		" W               W ",
		" g   m       m   g ",
		" W               W ",
		" W               W ",
		" g   m       m   g ",
		" W               W ",
		" W               W ",
		" g   m       m   g ",
		" W               W ",
		" W               W ",
		" g   m       m   g ",
		" W               W ",
		" W               W ",
		" g   m       m   g ",
		" W               W ",
		" W               W ",
		" WWWWW       WWWWW ",
		" W               W ",
		" W               W ",
		" W               W ",
		" LWWWWWWWWWWWWWWWL ",
		"                   ",
		"                   ",
		"                   ",
		"                   ",
	};

	private static final String[] UPPER = {
		"                   ",
		" LWgWWWWWWWWWWWgWL ",
		" W               W ",
		" W               W ",
		" g               g ",
		" WWWWW       WWWWW ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" WWWWW       WWWWW ",
		" W               W ",
		" W               W ",
		" W               W ",
		" LWWWWWWWWWWWWWWWL ",
		"                   ",
		"                   ",
		"                   ",
		"                   ",
	};

	private static final String[] EAVES = {
		"                   ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" WWWWWWWWWWWWWWWWW ",
		" LWWWWWWWWWWWWWWWL ",
		"                   ",
		"                   ",
		"                   ",
		"                   ",
	};

	private static final String[] CROWN = {
		"                   ",
		" LWgWWWWWWWWWWWgWL ",
		" W               W ",
		" W               W ",
		" W               W ",
		" WWWWW       WWWWW ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" g               g ",
		" W               W ",
		" W               W ",
		" WWWWW       WWWWW ",
		" W               W ",
		" W               W ",
		" W               W ",
		" LWWWWWWWWWWWWWWWL ",
		"                   ",
		"                   ",
		"                   ",
		"                   ",
	};

	private static final String[][] LAYERS = { GROUND, NAVE, MID, UPPER, CROWN, EAVES };

	/**
	 * Where the stair down starts, and it is INSIDE.
	 *
	 * Between the altar and the back wall of the chancel, which is the one
	 * patch of floor in the building nobody walks over: the pews face it, the
	 * aisle stops short of it, and everything in the room is arranged to make
	 * you look AT the altar rather than behind it.
	 *
	 * It was outside before, because the shaft it fed wandered out from under
	 * the building. That is fixed in Undercity, and this is the other half of
	 * the same repair — a secret entrance in the churchyard is a hole in the
	 * ground, and a secret entrance behind the altar is a secret.
	 */
	private static final int STAIR_X = 9;
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
			// Hangings between the windows. Cloth is the only warm thing in a
			// stone building and the only colour the town could actually make
			// itself, which is why the church has wool where a richer one would
			// have had paint.
			case 'm' -> Blueprint.put(level, at, Blocks.WOOL.pick(cloth(random))
				.defaultBlockState());
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
		// Five courses of wall now rather than three, and the pitch runs up
		// nine more on top of that. Height is what a church has instead of
		// space: the nave is nineteen across and twenty from floor to ridge,
		// so a player standing in the aisle cannot see the top of the room
		// without looking up, and looking up is the entire architectural
		// argument of the building.
		for (int step = 0; step <= 8; step++) {
			for (int z = 1; z <= 26; z++) {
				for (int x : new int[] { step + 1, WIDTH - 2 - step }) {
					if (x < 0 || x >= WIDTH) {
						continue;
					}
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 6 + step,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, step == 8
						? Blocks.DARK_OAK_PLANKS.defaultBlockState()
						: roofing(step));
				}
			}
		}
	}

	/**
	 * Timber over slate, and the timber is the point.
	 *
	 * A stone building with a stone roof reads as a keep. Slate on the lower
	 * pitch with dark beams over the top is what an actual parish church looks
	 * like, and it ties the most expensive building in the town back to the
	 * spruce framing on every house around it — the same people built both.
	 */
	private static BlockState roofing(int step) {
		if (step >= 6) {
			return Blocks.DARK_OAK_PLANKS.defaultBlockState();
		}
		return step % 3 == 0
			? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
				.setValue(BlockStateProperties.SLAB_TYPE,
					net.minecraft.world.level.block.state.properties.SlabType.TOP)
			: Blocks.DEEPSLATE_TILES.defaultBlockState();
	}

	/**
	 * The tower, over the porch, with the bell in it.
	 *
	 * The one thing in the settlement visible from outside the wall, and the
	 * reason the town reads as a town from the ridge rather than as a compound.
	 */
	private static void tower(ServerLevel level, BlockPos corner, Direction facing,
	                          RandomSource random) {
		for (int up = 6; up <= 22; up++) {
			for (int x = 5; x <= 13; x++) {
				for (int z = 22; z <= 26; z++) {
					boolean edge = x == 5 || x == 13 || z == 22 || z == 26;
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), up,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					if (!edge) {
						Blueprint.put(level, at, up == 22
							? Blocks.DARK_OAK_PLANKS.defaultBlockState()
							: Blocks.AIR.defaultBlockState());
						continue;
					}
					boolean louvre = up >= 17 && up <= 19
						&& (x == 9 || z == 24 || x == 7 || x == 11);
					Blueprint.put(level, at, louvre
						? Blocks.STONE_BRICK_WALL.defaultBlockState()
						: stone(random));
				}
			}
		}

		BlockPos bell = corner.offset(
			Blueprint.spinX(9, 24, WIDTH, DEPTH, facing), 19,
			Blueprint.spinZ(9, 24, WIDTH, DEPTH, facing));
		Blueprint.put(level, bell, Blocks.BELL.defaultBlockState()
			.setValue(BlockStateProperties.BELL_ATTACHMENT,
				net.minecraft.world.level.block.state.properties.BellAttachType.CEILING));

		// The spire, in timber, because a stone one this thin would look like
		// masonry nobody could have built.
		for (int up = 23; up <= 31; up++) {
			int inset = (up - 23) / 2;
			for (int x = 6 + inset; x <= 12 - inset; x++) {
				for (int z = 23 + inset; z <= 25 - inset; z++) {
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), up,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				}
			}
		}
	}

	/** The dyes a town this size could actually make. No purple, no magenta. */
	private static DyeColor cloth(RandomSource random) {
		DyeColor[] made = { DyeColor.RED, DyeColor.BROWN, DyeColor.YELLOW,
			DyeColor.WHITE, DyeColor.RED };
		return made[random.nextInt(made.length)];
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
		if (x == 9) {
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
