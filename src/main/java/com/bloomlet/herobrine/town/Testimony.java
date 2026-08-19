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
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
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
	 * Put the books down, IN THE BARRELS IN THE HOUSES, once everything is cut.
	 *
	 * This used to place six chests on the open cavern floor, and that one choice
	 * was doing more damage to the undercity than the perfect dome ever did. A
	 * chest on open ground is the most artificial object in Minecraft: nobody in
	 * any world keeps anything in a box in the middle of a street, so six of them
	 * ringing a square could only read as loot markers — and once the player has
	 * read them as loot markers, the books inside them are the mod talking rather
	 * than somebody writing.
	 *
	 * A barrel indoors, next to the bread, is the opposite claim. It says this
	 * was hidden by the person who lived here, in the place a person actually
	 * hides a thing they cannot throw away.
	 *
	 * Spread one per household rather than gathered into one, because a single
	 * container holding six books is a lore dump and six households holding one
	 * each is a room somebody has to search. The player who finds two of them and
	 * leaves has a better story than the player handed all six.
	 *
	 * @param spots    one per account, and they are used in order — the last is
	 *                 the library's, and the last account is the one who has
	 *                 stopped being frightened
	 * @param pantries the other barrels, which get nothing but food. Most of what
	 *                 the player opens down here has to be somebody's flour or
	 *                 the searching is not searching
	 */
	public static void write(ServerLevel level, List<net.minecraft.core.BlockPos> spots,
	                         List<net.minecraft.core.BlockPos> pantries, RandomSource random) {
		int placed = 0;
		for (int i = 0; i < ACCOUNTS.length && i < spots.size(); i++) {
			net.minecraft.core.BlockPos at = spots.get(i);
			BarrelBlockEntity barrel = barrelAt(level, at);
			if (barrel == null) {
				continue;
			}
			// Slot 4, the middle of the top row, so it is the first thing seen.
			barrel.setItem(4, book(ACCOUNTS[i]));
			// AND THE BREAD GOES IN WITH IT. Not beside it in a different
			// container and not instead of it — in the same barrel, which is the
			// entire sentence this is trying to say: whoever wrote this kept it
			// where they kept their food, because it was the only place in the
			// house nobody would look twice at.
			stock(barrel, random);
			barrel.setChanged();
			placed++;
		}
		for (net.minecraft.core.BlockPos at : pantries) {
			BarrelBlockEntity barrel = barrelAt(level, at);
			if (barrel == null) {
				continue;
			}
			stock(barrel, random);
			barrel.setChanged();
		}
		HerobrineMod.LOGGER.info("{} accounts left in the undercity, in {} pantries",
			placed, pantries.size() + placed);
	}

	/**
	 * What a household forty blocks under a town actually has in.
	 *
	 * Bread first and always, because that is what makes the barrel a pantry
	 * rather than a container the mod filled. Then a couple of things from a
	 * short list of the dullest items in the game — and dull is the requirement.
	 * Anything valuable turns the barrel back into loot and the player back into
	 * somebody looting, and then the book in it is a reward instead of a
	 * discovery.
	 *
	 * No underground farm to grow any of it, and that is deliberately left
	 * hanging. The town is directly overhead.
	 */
	private static final net.minecraft.world.item.Item[] LARDER = {
		Items.BREAD, Items.WHEAT, Items.CANDLE, Items.BOWL, Items.POTATO,
		Items.BEETROOT, Items.STRING, Items.PAPER, Items.FLINT,
	};

	private static void stock(BarrelBlockEntity barrel, RandomSource random) {
		barrel.setItem(random.nextInt(3) == 0 ? 0 : 6,
			new ItemStack(Items.BREAD, 2 + random.nextInt(5)));
		int extras = 1 + random.nextInt(3);
		for (int i = 0; i < extras; i++) {
			int slot = 9 + random.nextInt(18);
			if (!barrel.getItem(slot).isEmpty()) {
				continue;
			}
			barrel.setItem(slot, new ItemStack(
				LARDER[random.nextInt(LARDER.length)], 1 + random.nextInt(4)));
		}
	}

	/**
	 * The barrel that is already there, or one put down if the spot is empty.
	 *
	 * The houses place their own barrels while they are being built, so normally
	 * this only has to look one up. The fallback exists because this runs last,
	 * after every tunnel and trap is cut, and a spot that has been carved through
	 * since is better filled than silently skipped — but a spot that is now solid
	 * stone is left alone rather than punched open.
	 */
	private static @org.jspecify.annotations.Nullable BarrelBlockEntity barrelAt(
			ServerLevel level, net.minecraft.core.BlockPos at) {
		if (!level.getBlockState(at).is(Blocks.BARREL)) {
			if (!level.getBlockState(at).canBeReplaced()) {
				HerobrineMod.LOGGER.warn("no barrel and no room for one at [{}, {}, {}]",
					at.getX(), at.getY(), at.getZ());
				return null;
			}
			level.setBlock(at, Blocks.BARREL.defaultBlockState(), 3);
		}
		return level.getBlockEntity(at) instanceof BarrelBlockEntity barrel ? barrel : null;
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
