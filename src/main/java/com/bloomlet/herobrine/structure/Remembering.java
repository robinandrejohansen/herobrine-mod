package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * WHAT IS INSIDE THE KEEP, AND IT IS THE ANSWER.
 *
 * LORE.md has held this since the beginning and nothing in the mod has ever been
 * able to say it: two brothers, a valley, a house. The younger dug too deep,
 * something came up wearing him, and it killed the family WITH HIS HANDS. The
 * elder could not put an axe in his own brother, so he tore a hole and put him
 * through it and sealed it.
 *
 * This is the far side of that hole. He has been here ever since.
 *
 * AND HE HAS BEEN BUILDING. That is the whole reveal and it is delivered without
 * a word of explanation: a castle laid true, a city with lit lamps and shut
 * doors, and nobody in any of it. He built a town for a family that is dead. He
 * built it for the people he killed.
 *
 * The four floors are one sentence each, and they get worse going up:
 *
 *   THE HALL — a table laid for a family, and the food is still on it.
 *   THE NAMES — he has been writing his own name down so as not to lose it.
 *   THE HOUSE — four models of the homestead, built from memory, all wrong.
 *   THE WATCH — a chair at a window, facing the way the door was.
 *
 * NOTHING HERE EXPLAINS ANYTHING. There is no page that says who he was or what
 * came up the shaft — the books are a man's own handwriting failing, and the
 * models are a shape the player recognises from the first building in the mod.
 * A player who has been to the homestead will get it in the chest and a player
 * who has not will walk past four odd little houses on a floor.
 *
 * The violet is only at the top. Per LORE.md the colour belongs to the THING and
 * not to him, so it appears in exactly one room — the highest, the furthest from
 * the table, the one where there is the least of him left.
 */
public final class Remembering {
	private Remembering() {}

	/** Where each floor sits inside the keep's shell. */
	private static final int HALL = 0;
	private static final int NAMES = 8;
	private static final int HOUSE = 16;
	private static final int WATCH = 24;

	public static void furnish(ServerLevel his, BlockPos base, int half, int height,
	                           RandomSource random) {
		for (int level : new int[] { NAMES, HOUSE, WATCH }) {
			floor(his, base.above(level), half);
		}
		hall(his, base.above(HALL), half, random);
		names(his, base.above(NAMES), half);
		house(his, base.above(HOUSE), half, random);
		watch(his, base.above(WATCH), half, height);
		HerobrineMod.LOGGER.info("the keep is furnished at [{}, {}, {}]",
			base.getX(), base.getY(), base.getZ());
	}

	/**
	 * A floor, with the stair left open round the edge.
	 *
	 * The keep's stair spirals up the inside of the shell, so the boards stop
	 * one block short of the wall all the way round and what is left is a
	 * gallery. Which is also the better room: you come up the stair at the edge
	 * and the floor is laid out in front of you, rather than arriving through a
	 * hole in the middle of it.
	 */
	private static void floor(ServerLevel his, BlockPos at, int half) {
		for (int dx = -half + 2; dx <= half - 2; dx++) {
			for (int dz = -half + 2; dz <= half - 2; dz++) {
				put(his, at.offset(dx, 0, dz), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
	}

	// ---- THE HALL ----------------------------------------------------------
	/**
	 * A table laid for a family, and nobody has cleared it.
	 *
	 * FOUR PLACES, and the number is the point. The player has read six accounts
	 * in the undercity written by people who each saw something, and a journal by
	 * one person who did not survive knowing about it; this is the household the
	 * whole mod started with, seated. One chair at the head is pushed back from
	 * the table and the others are not.
	 *
	 * The food is real food on real plates rather than decoration. Bread and
	 * bowls and a cake, because those are what a table looks like when somebody
	 * has laid it — and it is the ordinariness that hurts. A hall with a throne
	 * in it is a boss room. A hall with supper on the table is a house.
	 */
	private static void hall(ServerLevel his, BlockPos at, int half, RandomSource random) {
		int span = half - 4;
		for (int dz = -span; dz <= span; dz++) {
			put(his, at.offset(0, 1, dz), Blocks.DARK_OAK_SLAB.defaultBlockState()
				.setValue(BlockStateProperties.SLAB_TYPE,
					net.minecraft.world.level.block.state.properties.SlabType.TOP));
		}
		// Places laid down both sides, and one at the head.
		int[] seats = { -span + 2, -1, span - 2 };
		for (int i = 0; i < seats.length; i++) {
			int dz = seats[i];
			int dx = i % 2 == 0 ? -2 : 2;
			chair(his, at.offset(dx, 1, dz), dx < 0 ? Direction.EAST : Direction.WEST);
			setting(his, at.offset(0, 2, dz), random);
		}
		// The head of the table. The chair is pushed BACK, one block clear, and
		// it is the only thing in the room that is out of place.
		chair(his, at.offset(0, 1, -span - 2), Direction.SOUTH);
		setting(his, at.offset(0, 2, -span), random);

		// Light down the length of it, still burning.
		for (int dz = -span + 3; dz <= span - 3; dz += 4) {
			put(his, at.offset(0, 2, dz), Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.LIT, true)
				.setValue(BlockStateProperties.CANDLES, 1 + random.nextInt(3)));
		}
		for (int dx : new int[] { -half + 3, half - 3 }) {
			for (int dz = -span; dz <= span; dz += 6) {
				put(his, at.offset(dx, 4, dz), Blocks.SOUL_LANTERN.defaultBlockState());
			}
		}
	}

	private static void chair(ServerLevel his, BlockPos at, Direction facing) {
		put(his, at, Blocks.DARK_OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
			.setValue(BlockStateProperties.HALF, Half.BOTTOM));
	}

	private static void setting(ServerLevel his, BlockPos at, RandomSource random) {
		BlockState[] supper = {
			Blocks.CAKE.defaultBlockState(),
			Blocks.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState(),
			Blocks.FLOWER_POT.defaultBlockState(),
		};
		put(his, at, supper[random.nextInt(supper.length)]);
	}

	// ---- THE NAMES ---------------------------------------------------------
	/**
	 * He has been writing his own name down so as not to lose it.
	 *
	 * THE ONE PLACE IN THE MOD WHERE HE SPEAKS AT LENGTH, and he is not speaking
	 * to the player — these are notes to himself, and that is the only reason
	 * they are allowed to exist. Every sign, page and taunt in forty hours has
	 * been four words aimed outward. This is the inside of his head and he does
	 * not know anybody is reading.
	 *
	 * LORE.md: "He copies you because he is trying to remember himself." The
	 * mimicry, the skin-wearing, the standing in somebody's base facing a wall —
	 * this room is the note those events were footnotes to.
	 *
	 * Nothing here says what he is or what came up the shaft. It is a man losing
	 * a word and writing it down harder.
	 */
	private static final String[][] NOTES = {
		{
			"""
			the name
			""",
			"""
			I am writing it here so
			that I have it somewhere
			that is not me.

			It has four letters. I am
			certain of the four.

			""",
			"""
			It was here yesterday and
			it is not here now.

			I have looked at the shape
			of it a long while and it
			is a shape I have made,
			not a name.

			I will ask at supper."""
		},
		{
			"""
			the house
			""",
			"""
			Eleven long. Not twelve.
			I have counted the boards
			twice.

			The door is on the short
			side and there is a step,
			and the step was worn down
			on the left.
			""",
			"""
			I have built it four times.

			It is not right and I can
			not say what is wrong with
			it. Somebody would know.
			Somebody stood in the
			doorway and would know."""
		},
		{
			"""
			the count
			""",
			"""
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||
			""",
			"""
			I stopped at ninety and
			started again, and I have
			stopped again.

			It does not matter what the
			number is. It matters that
			I was the one keeping it.

			So: keep it."""
		},
		{
			"""
			supper
			""",
			"""
			Four. It has always been
			four and I have laid it for
			four.

			I do not put it away. If it
			is on the table then they
			have not come down yet.
			""",
			"""
			I know.

			I want it written that I
			know, and that I lay it
			anyway, and that those are
			two different things and I
			am doing both."""
		},
	};

	private static void names(ServerLevel his, BlockPos at, int half) {
		// Shelves down both long walls, and lecterns with the books open on
		// them. Open, so the first one is READ rather than found — a chest of
		// books is a thing to loot and a lectern is a thing somebody left.
		for (int dz = -half + 3; dz <= half - 3; dz++) {
			for (int dy = 1; dy <= 3; dy++) {
				for (int dx : new int[] { -half + 2, half - 2 }) {
					put(his, at.offset(dx, dy, dz), Blocks.BOOKSHELF.defaultBlockState());
				}
			}
		}
		for (int i = 0; i < NOTES.length; i++) {
			int dz = -half + 5 + i * 4;
			BlockPos stand = at.offset(i % 2 == 0 ? -3 : 3, 1, dz);
			put(his, stand, Blocks.LECTERN.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					i % 2 == 0 ? Direction.EAST : Direction.WEST)
				.setValue(BlockStateProperties.HAS_BOOK, true));
			if (his.getBlockEntity(stand) instanceof LecternBlockEntity lectern) {
				lectern.setBook(note(NOTES[i]));
				lectern.setChanged();
			}
			put(his, at.offset(0, 3, dz), Blocks.SOUL_LANTERN.defaultBlockState());
		}
	}

	private static ItemStack note(String[] pages) {
		List<Filterable<Component>> written = new ArrayList<>();
		for (int i = 1; i < pages.length; i++) {
			written.add(Filterable.passThrough(Component.literal(pages[i].stripIndent())));
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(pages[0].strip()), "—", 0, written, true));
		return book;
	}

	// ---- THE HOUSE ---------------------------------------------------------
	/**
	 * FOUR MODELS OF THE HOMESTEAD, BUILT FROM MEMORY, ALL WRONG.
	 *
	 * The best thing in the room and it needs no text at all. The homestead is
	 * the FIRST building anybody finds in this mod — twenty minutes into a save,
	 * a farmhouse with a sagging roof and a chimney — and every player who gets
	 * here will have stood in it. These are that house, at a third the size, on
	 * a floor, four times, each one a different wrong.
	 *
	 * One is missing the chimney. One has the door on the long side. One is
	 * twelve boards instead of eleven, which is the mistake he wrote down and
	 * still made. And one is taken half to pieces, because he was starting again.
	 *
	 * A player who has been to the homestead gets it in the chest. One who has
	 * not walks past four odd little houses on a floor, which is the correct
	 * failure — this mod has never once explained itself and is not going to
	 * start in the last room.
	 */
	private static void house(ServerLevel his, BlockPos at, int half, RandomSource random) {
		int[][] spots = { { -5, -5 }, { 5, -5 }, { -5, 5 }, { 5, 5 } };
		for (int i = 0; i < spots.length; i++) {
			model(his, at.offset(spots[i][0], 1, spots[i][1]), i, random);
		}
		for (int dx : new int[] { -half + 3, half - 3 }) {
			put(his, at.offset(dx, 4, 0), Blocks.SOUL_LANTERN.defaultBlockState());
		}
	}

	private static void model(ServerLevel his, BlockPos at, int which, RandomSource random) {
		int len = which == 2 ? 4 : 3;      // the one that is twelve boards, not eleven
		int wide = 2;
		int high = which == 3 ? 1 : 2;     // the one he had started taking apart

		for (int dx = -wide; dx <= wide; dx++) {
			for (int dz = -len; dz <= len; dz++) {
				put(his, at.offset(dx, 0, dz), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
				boolean wall = Math.abs(dx) == wide || Math.abs(dz) == len;
				for (int dy = 1; dy <= high; dy++) {
					if (!wall) {
						continue;
					}
					// Half of the last one is simply not there.
					if (which == 3 && dz > 0 && random.nextInt(2) == 0) {
						continue;
					}
					put(his, at.offset(dx, dy, dz), Blocks.OAK_PLANKS.defaultBlockState());
				}
			}
		}
		// The roof, in stairs, gabled the way the homestead's is.
		for (int dz = -len; dz <= len; dz++) {
			for (int dx = -wide; dx <= wide; dx++) {
				if (which == 3 && dz > 0) {
					continue;
				}
				put(his, at.offset(dx, high + 1, dz), dx == 0
					? Blocks.OAK_SLAB.defaultBlockState()
					: Blocks.OAK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING,
							dx < 0 ? Direction.EAST : Direction.WEST));
			}
		}
		// The chimney — except on the one that has not got one.
		if (which != 1) {
			put(his, at.offset(wide - 1, high + 2, -len + 1),
				Blocks.BRICKS.defaultBlockState());
		}
		// The door. On the short side, the way it actually is — except on the
		// one where he put it on the long side.
		BlockPos door = which == 0
			? at.offset(wide, 1, 0)
			: at.offset(0, 1, len);
		put(his, door, Blocks.AIR.defaultBlockState());
	}

	// ---- THE WATCH ---------------------------------------------------------
	/**
	 * A chair at a window, facing the way the door was.
	 *
	 * The last room and the smallest thing in it. No table, no books, no models —
	 * one seat, one window, and the violet.
	 *
	 * PER LORE.MD THE COLOUR BELONGS TO THE THING RATHER THAN TO HIM, which is
	 * why it is only here: the highest room, the furthest from the supper table,
	 * the one with the least of him left in it. Everything below is a man's
	 * handwriting and a man's furniture; this is where that runs out.
	 *
	 * And what he is looking at is the direction the player came from.
	 */
	private static void watch(ServerLevel his, BlockPos at, int half, int height) {
		chair(his, at.offset(0, 1, -2), Direction.SOUTH);
		// The window, cut through the shell on the gate side.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = 1; dy <= 3; dy++) {
				put(his, at.offset(dx, dy, half),
					Blocks.STAINED_GLASS_PANE.pick(
						net.minecraft.world.item.DyeColor.PURPLE).defaultBlockState());
			}
		}
		// The violet, and it is the only light in the room.
		for (int dx : new int[] { -3, 3 }) {
			put(his, at.offset(dx, 1, 2), Blocks.AMETHYST_BLOCK.defaultBlockState());
			put(his, at.offset(dx, 2, 2), Blocks.AMETHYST_CLUSTER.defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.UP));
		}
		put(his, at.offset(0, 1, 3), Blocks.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.PURPLE).defaultBlockState()
			.setValue(BlockStateProperties.LIT, true));
	}

	private static void put(ServerLevel his, BlockPos at, BlockState state) {
		if (!his.getBlockState(at).equals(state)) {
			his.setBlock(at, state, 2);
		}
	}
}
