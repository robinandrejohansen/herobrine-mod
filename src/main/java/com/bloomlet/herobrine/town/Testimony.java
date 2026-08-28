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
	/**
	 * WHAT THEY ARE NOW: SIX PEOPLE WHO GOT OUT, AND NONE OF THEM SAW IT.
	 *
	 * These were accounts of SIGHTINGS — a figure at the treeline, a man in the
	 * west field who did not wave back — written by a cult that had made a doctrine
	 * out of looking. That was a good document for a town with people still in it.
	 * There are no people in it any more. The doors are shut, the lamps are lit,
	 * and everybody who is still breathing is forty blocks underneath.
	 *
	 * So these are what the survivors wrote. Six of them, one night each, and the
	 * thing they all have in common is the thing that makes it work:
	 *
	 *   NOT ONE OF THEM SAW WHAT DID IT.
	 *
	 * They heard it through a floor. They found the door open. They came back from
	 * the field. One of them was holding the other end of a rope. Every account
	 * stops at the same wall — the writer describes with total precision everything
	 * up to the moment, and then has nothing, because there was nothing to see.
	 *
	 * THAT IS WHY IT IS SIX AND NOT ONE. A single unreliable narrator is a man who
	 * might be mistaken. Six people, separately, in their own words, all going
	 * blank at exactly the same point, is not six mistakes — it is the shape of the
	 * thing pressed into six different pieces of paper, and the reader assembles it
	 * without anybody having described it once.
	 *
	 * AND THE HORROR IS IN THE INVENTORY. Nobody writes "it was terrible". They
	 * write what was in the room, in the order they found it, because that is what
	 * people actually do — and a list of objects is worse than any adjective. The
	 * one who counts the boots is the worst page down here and it never raises its
	 * voice once.
	 */
	private static final Account[] ACCOUNTS = {
		new Account("what was under the floor", new String[] {
			"""
			I am setting this down
			because I am the only one
			who was awake and I will
			not be believed later.

			It began under the boards.
			Not footsteps. A dragging,
			the length of the room and
			back, the length of the
			room and back, for the
			part of an hour it takes
			to talk yourself out of
			getting up.""",
			"""
			Then it stopped, and the
			ladder to the loft moved.

			I did not go up. I want
			that written in my own
			hand rather than said
			about me afterwards. I
			did not go up.

			In the morning the loft
			was empty and the window
			was still latched from the
			inside and there was no
			ladder against the house.""",
		}),

		new Account("the door was open", new String[] {
			"""
			I came in from the low
			field at dusk, the same as
			every day of my life.

			The door was open. Not
			forced. Open, the way you
			leave it when you are
			carrying something in both
			hands and mean to come
			straight back.

			The fire was still going.
			The pot was still on it.""",
			"""
			Three bowls out. Three
			spoons. Two of the bowls
			had been started.

			I have been asked what I
			found upstairs and I have
			stopped answering, so I
			will write it once and
			then this book goes in the
			barrel.

			I found the ceiling.""",
		}),

		new Account("a list, in the order found", new String[] {
			"""
			The gate, shut and pegged.
			The dog, not barking, and
			the dog always barked.

			Both boots by the step,
			which is how I knew he was
			inside, because a man does
			not walk the valley in his
			stockings.

			The scythe, hung up. The
			hands still on it.""",
			"""
			I have gone over this list
			every night since and it
			is always the same list and
			it never explains itself.

			Everything was where it
			was supposed to be. That
			is the part I cannot get
			past. Nothing was knocked
			over. Nothing was taken.

			It was tidy in there.""",
		}),

		new Account("to Marta, who will not read this", new String[] {
			"""
			You asked me to hold the
			rope while you went down
			for the bucket. Eleven
			feet of well and I have
			pulled you up out of it
			forty times.

			It went slack. Not cut and
			not dropped. Slack, the way
			it goes when somebody at
			the other end has decided
			to let go, except you had
			it round your wrist.""",
			"""
			I hauled it up. It came up
			easily and it came up
			whole and it came up
			wearing your bracelet.

			I have been down that well
			four times since. There is
			eleven feet of it and then
			there is water and then
			there is the bottom, and I
			have had my hands on all
			three.

			There is nowhere for you
			to be.""",
		}),

		new Account("the seventh house", new String[] {
			"""
			We went along the row in
			the morning, six of us,
			because nobody had come out
			for the water.

			Six houses. I will not
			write what was in them. It
			was the same thing six
			times and doing it once is
			a murder and doing it six
			times before anybody woke
			is not a man.""",
			"""
			The seventh was mine.

			They were sitting up. All
			of them, in a row on the
			bed, facing the door, and
			they had been arranged that
			way because people do not
			die sitting up in a row.

			They were waiting for me to
			come in and see it. That is
			what it was for.""",
		}),

		new Account("we are not going back up", new String[] {
			"""
			There are forty-one of us
			down here and between us we
			have lost two hundred and
			six.

			Not one of us saw it. I
			have asked every single
			person in this chamber and
			I have written down every
			answer and the answers are
			a floor, a door, a rope, a
			row of them sitting up.

			Nobody saw it.""",
			"""
			They say up there that it
			was fought off. That thirty
			men went up the valley and
			finished it and there is a
			stone with the names on.

			We have read the stone. We
			put two of those names on
			it ourselves.

			It was not fought off. It
			was PUT somewhere, and the
			man who put it there is
			still sitting on the lid,
			and one day he will get
			tired.

			Grow the wheat. Keep the
			lamps lit. Do not go up.""",
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

	/**
	 * Lines a written book draws before it stops, and it stops silently.
	 *
	 * These pages are hard-wrapped by hand — the writer chose where every line
	 * breaks, which is most of why they read like somebody's handwriting rather
	 * than a paragraph. That makes LINES the limit rather than characters, and it
	 * makes overflow invisible: the page looks finished and the last thing on it
	 * is simply not there.
	 *
	 * Thirteen, matching what the original twelve entries were written to.
	 */
	private static final int LINES = 13;

	/**
	 * One entry, across as many leaves as its lines need.
	 *
	 * Split on the blank lines the writer already put in, so a break never lands
	 * mid-thought. Anything that will not fit is given its own leaf rather than
	 * being cut — a survivor's account that stops in the middle of the sentence
	 * about the loft is worse than no account at all.
	 */
	private static List<String> leaves(String page) {
		List<String> out = new ArrayList<>();
		StringBuilder leaf = new StringBuilder();
		int lines = 0;
		for (String para : page.split("\n\n")) {
			int high = para.split("\n").length;
			if (lines > 0 && lines + high + 1 > LINES) {
				out.add(leaf.toString());
				leaf.setLength(0);
				lines = 0;
			}
			if (lines > 0) {
				leaf.append("\n\n");
				lines++;
			}
			leaf.append(para);
			lines += high;
		}
		if (leaf.length() > 0) {
			out.add(leaf.toString());
		}
		return out;
	}

	/**
	 * THIRTY-TWO CHARACTERS, AND GOING OVER DISCONNECTS THE PLAYER.
	 *
	 * A written book's title is written to the wire by Utf8String with a hard cap
	 * of 32, and nothing checks it on the way in. A 33-character title compiles,
	 * builds, boots, generates, saves — and then the first person to OPEN the chest
	 * gets
	 *
	 *     EncoderException: String too big (was 33 characters, max 32)
	 *
	 * on container_set_content, which does not throw an error in the chest, it
	 * severs the connection. "Internal Exception ... Failed to encode packet" and
	 * they are on the title screen. The chest also fails to serialise on save, so
	 * the item quietly vanishes from the world as well.
	 *
	 * Two of the titles written this week were 33. Both crashed. It presented as
	 * "the map is not in the chest", which is exactly what it looks like from
	 * inside the game, and it cost an evening.
	 *
	 * So no title reaches a book without coming through here. Trimming at a word
	 * boundary rather than mid-word, because a title cut to "an inventory of the sec"
	 * is a bug report of its own — and it SHOUTS in the log, because a silently
	 * shortened title is a thing nobody notices until it is in a release.
	 */
	private static final int TITLE_FITS = 32;

	private static String title(String wanted) {
		if (wanted.length() <= TITLE_FITS) {
			return wanted;
		}
		int cut = wanted.lastIndexOf(' ', TITLE_FITS);
		String short_ = wanted.substring(0, cut > 12 ? cut : TITLE_FITS).trim();
		com.bloomlet.herobrine.HerobrineMod.LOGGER.warn(
			"book title was {} characters and the wire allows {} — \"{}\" became \"{}\"",
			wanted.length(), TITLE_FITS, wanted, short_);
		return short_;
	}

	private static ItemStack book(Account account) {
		List<Filterable<Component>> pages = new ArrayList<>();
		for (String page : account.pages()) {
			for (String leaf : leaves(page.stripIndent().trim())) {
				pages.add(Filterable.passThrough(Component.literal(leaf)));
			}
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(title(account.who())),
			"—",
			0,
			pages,
			true
		));
		return book;
	}
}
