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

	/**
	 * THE COURSE UNDER THE ROOF, AND IT IS A RING NOW RATHER THAN A LID.
	 *
	 * This was solid W across the whole footprint — a stone ceiling at head height
	 * with the entire pitched roof sitting on top of it, unseen. Which made the
	 * comment on roof() a lie: it says the nave runs twenty blocks from floor to
	 * ridge so that a player cannot see the top of the room without looking up,
	 * and there was a slab in the way at five.
	 *
	 * Hollow, so the nave opens all the way to the ridge and the roof becomes part
	 * of the room. The perimeter stays, because it is still a wall course and
	 * taking it out would leave a gap between the crown and the eaves. The two end
	 * rows stay solid as well: they are what the gables are built up from.
	 */
	private static final String[] EAVES = {
		"                   ",
		" WWWWWWWWWWWWWWWWW ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" W               W ",
		" WWWWWWWWWWWWWWWWW ",
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
		altar(level, corner, facing, random);
		return true;
	}

	/**
	 * The altar, built in world coordinates rather than out of blueprint glyphs.
	 *
	 * The chancel was three chiselled blocks in a row with a single dark block
	 * behind them, which is a shelf. Everything that makes an altar an altar —
	 * height, a step up to it, light on it, something left on it — needs blocks at
	 * several Y at once, and a layer-by-layer blueprint can only say one thing per
	 * column. So this is drawn by hand.
	 *
	 * THE CANDLES ON THE TABLE ARE LIT AND THE ONES ON THE WALL ARE NOT. Four
	 * standing candles nobody snuffed, in a building nobody has been inside for a
	 * year, in a town that is living underneath itself. It is the only warm light
	 * in the settlement and there is no one to have put it there.
	 */
	private static void altar(ServerLevel level, BlockPos corner, Direction facing,
	                          RandomSource random) {
		for (int x = 7; x <= 11; x++) {
			for (int z = 2; z <= 4; z++) {
				boolean table = x >= 8 && x <= 10 && z == 3;
				boolean step = z == 4;
				BlockPos at = corner.offset(
					Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 1,
					Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
				if (table) {
					Blueprint.put(level, at, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
				} else if (step) {
					// A course of polished stone the width of the chancel, so the
					// altar is stood ON something rather than set on the floor.
					Blueprint.put(level, at,
						Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
				}
			}
		}

		// The table top, and the candles standing on it.
		for (int x = 8; x <= 10; x++) {
			BlockPos top = corner.offset(
				Blueprint.spinX(x, 3, WIDTH, DEPTH, facing), 2,
				Blueprint.spinZ(x, 3, WIDTH, DEPTH, facing));
			Blueprint.put(level, top, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
		}
		for (int x : new int[] { 8, 10 }) {
			BlockPos on = corner.offset(
				Blueprint.spinX(x, 3, WIDTH, DEPTH, facing), 3,
				Blueprint.spinZ(x, 3, WIDTH, DEPTH, facing));
			Blueprint.put(level, on, Blocks.CANDLE
				.defaultBlockState()
				.setValue(BlockStateProperties.CANDLES, 2 + random.nextInt(2))
				.setValue(BlockStateProperties.LIT, true));
		}

		// Two more standing on the floor either side, taller, unlit.
		for (int x : new int[] { 6, 12 }) {
			BlockPos foot = corner.offset(
				Blueprint.spinX(x, 3, WIDTH, DEPTH, facing), 1,
				Blueprint.spinZ(x, 3, WIDTH, DEPTH, facing));
			Blueprint.put(level, foot, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			Blueprint.put(level, foot.above(), Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.CANDLES, 4)
				.setValue(BlockStateProperties.LIT, false));
		}

		// AND WHAT THEY LEFT ON IT. The town's church is the last thing anybody
		// organised, so what is in here is what a settlement takes when it is
		// deciding whether to go underground and not coming back for the rest.
		BlockPos box = corner.offset(
			Blueprint.spinX(9, 5, WIDTH, DEPTH, facing), 1,
			Blueprint.spinZ(9, 5, WIDTH, DEPTH, facing));
		Blueprint.put(level, box, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Blueprint.turned(Direction.SOUTH, facing)));
		if (level.getBlockEntity(box)
				instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			chest.setItem(0, com.bloomlet.herobrine.structure.HouseBooks.theTown());
			chest.setItem(1, com.bloomlet.herobrine.structure.HouseBooks.theTownAfter());
			com.bloomlet.herobrine.structure.Loot.scatter(chest, random,
				com.bloomlet.herobrine.structure.Loot.Tier.TOWN_TOOLS);
		}
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
			case 'g' -> Blueprint.put(level, at, Blocks.STAINED_GLASS
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
			// THE WOOL IS GONE. It hung between the windows as the one warm thing in
			// a stone building — and in a room full of spruce-stair pews it read as
			// upholstery, as though somebody had put cushions on the chairs. A
			// church the town abandoned in a hurry does not have soft furnishings.
			case 'm' -> { }
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
					// AND THE STEP UNDER IT, WHICH IS WHAT SHUTS THE ROOF.
					//
					// One block per course per side draws the pitch as a diagonal
					// line of blocks meeting corner to corner — and a diagonal line
					// of cubes is not a surface. Every joint is an open corner: you
					// could stand in the nave and see daylight through the slope,
					// and rain and snow came through with it.
					//
					// A second block directly beneath turns each course into an L,
					// the corners close on each other, and the pitch becomes solid
					// without changing its angle or its silhouette by a pixel.
					if (step > 0) {
						Blueprint.put(level, corner.offset(
							Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 5 + step,
							Blueprint.spinZ(x, z, WIDTH, DEPTH, facing)), roofing(step));
					}
				}
			}
			// AND THE GABLE ENDS GET CLOSED, which they never were.
			//
			// The loop above lays the two slopes and nothing else, so both ends of
			// the building were open triangles looking straight into the roof
			// void — the pitch was visible from outside with the sky behind it.
			// A roof with no gable is not a roof, it is two ramps.
			//
			// Filled in stone rather than in roofing, because a gable is WALL: it
			// is the end of the building carried up to the ridge, and doing it in
			// slate would read as a roof folded round a corner.
			for (int z : new int[] { 1, 26 }) {
				for (int x = step + 1; x <= WIDTH - 2 - step; x++) {
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 6 + step,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, Blocks.STONE_BRICKS.defaultBlockState());
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
