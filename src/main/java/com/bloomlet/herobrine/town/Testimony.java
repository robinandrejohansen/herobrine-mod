package com.bloomlet.herobrine.town;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.server.network.Filterable;

/**
 * WHAT THE CONGREGATION WROTE DOWN.
 *
 * The journal pages upstairs are one person's account, torn up and scattered.
 * These are different, and the difference is the whole reason they exist: this
 * is a GROUP, writing about the same thing, and each of them is certain.
 *
 * THE CULT IS ABOUT SIGHTINGS. That is the entire doctrine and it is the only
 * thing that makes a cult about Herobrine coherent rather than silly. Nobody
 * down here worships a devil; they have each SEEN something at the edge of a
 * field, and they came underground because they were the only people who
 * believed each other. The horror is not that they are mad. It is that they are
 * right, and the players know they are right, because the players have seen it
 * too.
 *
 * WHICH TURNS THE ROOM INTO A MIRROR. Every one of these books is somebody
 * describing an experience the player has already had — a figure at the
 * treeline, a torch that went out, the sound of digging with nobody there. The
 * effect is not "these poor lunatics". It is "this is my server's chat log from
 * two hours ago", and that recognition is worth more than any amount of
 * ominous scripture.
 *
 * SO THEY ARE ORDINARY PEOPLE, WRITING BADLY. First person, plain language,
 * hedged, self-conscious. The moment one of these reads like a prophecy the
 * whole thing collapses into a fantasy cult and stops being about anybody. The
 * one who writes with certainty is the one who has stopped being frightened,
 * and that one is the worst to read.
 *
 * PLACED LAST, ALWAYS. Books go in after every tunnel, chamber and trap is
 * carved, because the homestead taught this the expensive way: boring a passage
 * after a chest existed drove straight through it and left the books on the
 * floor as items counting down to despawning.
 */
public final class Testimony {
	private Testimony() {}

	private record Account(String who, String[] pages) {}

	/**
	 * Six hands, and they disagree with each other.
	 *
	 * The disagreement is deliberate and it is the most valuable thing here. Six
	 * accounts that corroborate are a doctrine; six that argue about what they
	 * saw are six people. One thinks it is a man, one thinks it is the mine, one
	 * has stopped asking, one is keeping a tally, one is writing to somebody who
	 * is not coming, and one is not frightened any more.
	 */
	private static final Account[] ACCOUNTS = {
		new Account("what I saw, by Aldis", new String[] {
			"""
			I am writing this so that
			I cannot change it later.

			Third of the month, near
			dusk. A man standing at
			the top of the west field
			where there is no path.

			I waved. I want that
			written down. I waved,
			because I thought it was
			Corwin.""",
			"""
			He did not wave back and
			he did not move and I
			looked away because the
			dog wanted feeding.

			When I looked again he
			was not there.

			That is all it was. I know
			how it reads. I have read
			it back four times and it
			still reads like nothing."""
		}),
		new Account("the mine, not a man", new String[] {
			"""
			They keep saying they see
			a man. I have never seen
			a man.

			What I hear is digging,
			under the floor, at hours
			when the shaft is shut and
			I have the only key.

			A man does not explain
			digging. The mine explains
			digging.""",
			"""
			Aldis asked me what is
			doing the digging then,
			if not a man.

			I said the earth settles.

			He asked why the earth
			settles in strokes of three
			and then stops when you
			put your ear to it.

			I have not answered him."""
		}),
		new Account("a tally", new String[] {
			"""
			I have stopped writing
			what it looks like. Everyone
			writes what it looks like
			and it never matches.

			So: a count. One mark for
			every time, one line for
			every week.

			|||| ||
			||| |
			|||| |||| ||
			|||| |||| |||| ||||""",
			"""
			|||| |||| |||| |||| ||||
			|||| |||| |||| |||| ||||

			I am not going to write the
			number out. Anyone can
			count.

			What I will write is that
			the lines get longer and
			nobody has any theory
			that survives the counting."""
		}),
		new Account("to Marta", new String[] {
			"""
			Marta,

			You were right to go and I
			am not writing to argue
			with you.

			The others have made a
			room under the square. I
			said it was foolish and then
			I helped dig it, which tells
			you where I am.""",
			"""
			We meet because it is the
			only hour of the week I am
			not frightened. That is the
			whole of it. Not devotion.
			Company.

			If you are reading this you
			came back, and I would
			give a great deal to know
			why."""
		}),
		new Account("the seventh", new String[] {
			"""
			We were six at the table
			and I counted seven
			shadows on the wall.

			I said nothing. I want to be
			honest about that. I said
			nothing for the rest of the
			evening and I walked home
			with Corwin and I said
			nothing then either.""",
			"""
			I have counted every week
			since. It is six. It has been
			six every week since.

			Which means either I
			cannot count, or it was
			there once and has not
			come back.

			I do not know which of
			those I would prefer."""
		}),
		new Account("no longer afraid", new String[] {
			"""
			The others are still writing
			down what they saw as
			though the writing changes
			it.

			I have stopped. Not because
			I stopped seeing him.
			Because I have understood
			what he is doing and it is
			very simple.""",
			"""
			He is not hunting anybody.
			He is waiting.

			Every one of us has thought
			about leaving. He knows
			that. He does not have to
			do anything at all except be
			at the top of the field until
			we go.

			I am not going to go."""
		}),
	};

	/**
	 * Put the books down, in chests, once everything is cut.
	 *
	 * Spread across separate chests rather than gathered into one, because a
	 * single chest holding six books is a lore dump and six chests holding one
	 * each is a room somebody has to search. The player who finds two of them and
	 * leaves has a better story than the player handed all six.
	 */
	public static void write(ServerLevel level, List<net.minecraft.core.BlockPos> spots,
	                         RandomSource random) {
		if (spots.isEmpty()) {
			return;
		}
		int placed = 0;
		for (int i = 0; i < ACCOUNTS.length && i < spots.size(); i++) {
			net.minecraft.core.BlockPos at = spots.get(i);
			if (!level.getBlockState(at).canBeReplaced()) {
				continue;
			}
			level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
			if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
				continue;
			}
			// Slot 4, the middle of the top row, so it is the first thing seen.
			chest.setItem(4, book(ACCOUNTS[i]));
			// And something ordinary beside it, because a chest holding only a
			// book is a delivery. A chest holding a book and somebody's candles
			// is a chest that belonged to somebody.
			chest.setItem(random.nextInt(4) == 0 ? 0 : 6,
				new ItemStack(random.nextBoolean() ? Items.CANDLE : Items.BREAD,
					1 + random.nextInt(3)));
			chest.setChanged();
			placed++;
		}
		HerobrineMod.LOGGER.info("{} accounts left in the undercity", placed);
	}

	private static ItemStack book(Account account) {
		List<Filterable<Component>> pages = new ArrayList<>();
		for (String page : account.pages()) {
			pages.add(Filterable.passThrough(Component.literal(page.stripIndent())));
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(account.who()),
			"—",
			0,
			pages,
			true
		));
		return book;
	}
}
