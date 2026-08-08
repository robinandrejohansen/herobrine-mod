package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * What the family wrote.
 *
 * These are NOT his. That distinction is the whole point of them, and it is
 * why they use ordinary punctuation and capital letters where his signs use
 * neither — a player who has been reading four-word lowercase threats on their
 * walls opens a book here and finds someone writing properly, about bread and
 * sheep, and the change in voice does more than any amount of dread would.
 *
 * The rule for all six: NOTHING SUPERNATURAL IS EVER DESCRIBED. Nobody sees a
 * ghost, nobody reports glowing eyes, nobody says the word possessed. Every
 * entry is a domestic observation that would be unremarkable on its own. A boy
 * is not hungry. A boy does not blink. The sheep are fewer. The horror is
 * entirely in the reader's head, assembled out of things that are individually
 * fine, and that is the only kind that survives being written down.
 *
 * They are also the load-bearing piece of lore delivery. LORE.md's premise —
 * the brother who went under the hill and came back — has until now only been
 * available through the Journal, which the player finds a page at a time over
 * hours. These put the family in the world as people first, so that when the
 * Journal explains what happened to them it is happening to somebody.
 */
public final class HouseBooks {
	private HouseBooks() {}

	/**
	 * The mother's household book.
	 *
	 * Deliberately the dullest thing in the mod for two pages. It has to be —
	 * it is establishing that these were people with a life worth losing, and
	 * the reader has to be a little bored before the last line lands. "I have
	 * never had to ask him twice to eat in his life" is a mother's observation
	 * about her son's appetite and nothing else, and it should take a second
	 * or two to understand why it is the worst sentence on the page.
	 */
	public static ItemStack household() {
		return book("the house book", "M.",
			"""
			Bread twice in the week. The big oven wants an hour before it will take a loaf.

			Wool from the four ewes at midsummer.

			J. is to fetch the water before dark. He forgets. He is nine.""",

			"""
			Rain all the week and the path is mud to the knee.

			The little one has made a whistle out of a reed and has not stopped since.

			Everyone is well. I write it down so that I will remember it was true.""",

			"""
			J. came back up from the north cut and would not eat.

			He says he is not hungry. He said it four times.

			I have never once had to ask that boy twice to eat.""");
	}

	/**
	 * The youngest child's book.
	 *
	 * A child reports what it sees without deciding what it means, which is
	 * why this one is allowed to be the most direct. An adult writing "he does
	 * not blink" is making an accusation. A child writing it has simply lost a
	 * game and is annoyed about it.
	 */
	public static ItemStack child() {
		return book("my book", "the little one",
			"""
			I drew the house. Mama says it looks like the house.

			I drew the sheep. There are four.

			I drew J. He is by the door. He is by the door a lot now.""",

			"""
			J. does not blink.

			I counted to two hundred.

			He said that is a silly game.

			He did not blink when he said it.""");
	}

	/**
	 * The father's ledger.
	 *
	 * A column of numbers is the most efficient horror device available here,
	 * because the reader does the arithmetic themselves and arrives at the
	 * conclusion a beat before the writer admits it. No adjectives are needed
	 * and none are used.
	 */
	public static ItemStack ledger() {
		return book("ledger", "R.",
			"""
			Midsummer. Four ewes, two lambs. Wheat good. Six loaves traded for nails.

			Autumn. Four ewes, one lamb. No sign of the other.

			Winter. Three ewes.""",

			"""
			Two ewes.

			I have walked the fence line twice over. There is no gap in it.

			Nothing has been dragged. There is no blood on the grass.

			They are only fewer each morning.""",

			"""
			None.

			I am not writing this down for anybody.

			I am writing it down so that the number is written.""");
	}

	/**
	 * The elder brother, on the younger.
	 *
	 * This is the Journal's voice arriving early, and the only book that
	 * touches the event directly. Even here it refuses to name it: he describes
	 * being GLAD, and then describes the small wrong detail he was too relieved
	 * to notice at the time. Guilt, not fear — which is the whole character.
	 */
	public static ItemStack brother() {
		return book("about my brother", "—",
			"""
			He was under the hill two days. We dug for one of them.

			When we broke through he was sitting up, and he was not cold, and he said my name before I said his.

			I was so glad that I did not think about that until much later.""",

			"""
			He is the same. That is the trouble with it. He is exactly the same.

			He does the things he did. He says the things he used to say.

			He does them the way a man does a thing he has been told about.""");
	}

	/**
	 * The tally.
	 *
	 * Five pages, and four of them are a shortening list. There is no sentence
	 * anywhere in it doing any work; the whole effect is a name that is present
	 * on one page and absent on the next, and the reader turning back to check.
	 * It is the shortest thing in the mod and the only one that has made me
	 * stop while writing it.
	 */
	public static ItemStack tally() {
		return book("tally", "—",
			"Mother. Father. The little one. J.\n\nThat is the house.",
			"Mother. Father. The little one.\n\nThat is the house.",
			"Father. The little one.",
			"The little one.",
			"That is the house.");
	}

	/**
	 * The last one, left by the sealed wall.
	 *
	 * Placed where the player will already be standing when they notice the
	 * stonework does not match, so it answers the question they have just
	 * started asking. The second page is the only outright threat in any of
	 * these, and it is not even a threat — it is a description of somebody
	 * being patient.
	 */
	public static ItemStack farRoom() {
		return book("the small room", "—",
			"""
			We put him in the small room because he asked us to.

			He said he did not want to be near the little one at night.

			We thought that was a kindness in him.

			We understand now that it was not a warning about the little one.""",

			"""
			The door is barred and the bar is on our side and that is the whole of what I am able to do.

			He has not tried it once.

			He stands on the other side of it and waits for one of us to be curious.""");
	}

	/**
	 * House two. Written by somebody explaining a decision to nobody.
	 *
	 * The trick here is that every sentence is reasonable. Each line on its own
	 * is a sane thing for a person to say, and the paragraph they add up to is
	 * somebody bricking up their own windows — which is a much worse way to
	 * read it than any amount of raving would be.
	 */
	public static ItemStack buried() {
		return book("the windows", "—",
			"""
			I have closed the windows on the east side. It is not because of anything.

			It is only that a window is a thing that can be looked into as easily as out of, and I had not thought of that before.""",

			"""
			The north ones as well now. The room is not darker. I keep a lamp.

			I find I am sleeping.""",

			"""
			I have taken the last of them out and put stone in.

			There is nothing wrong with the house. The house is exactly as it was.

			I simply do not need to see the field.""");
	}

	/**
	 * House three, left in the dirt beside the bed.
	 *
	 * Not a diary any more, because he has stopped keeping one. Four fragments
	 * with nothing joining them, the last of which is about the digging and is
	 * not about the digging.
	 */
	public static ItemStack theDig() {
		return book("notes", "—",
			"the seam runs south. I follow it. it does not end.",
			"""
			slept here. no reason to go up.

			nothing is up there that is not also down here.""",

			"""
			I have stopped counting the days because the counting was the last thing I was doing for anybody else's benefit.""",

			"""
			It is not that I am looking for something.

			It is that the digging is the only thing left that I am still doing on purpose, and I would like to be doing something on purpose.""");
	}

	/**
	 * House four, on the altar. The only one of these not in the first person.
	 *
	 * By this point he is not writing a journal, he is writing instructions —
	 * and instructions have a reader. That change of address is the whole
	 * content: somebody is being spoken to, and it is not us.
	 */
	public static ItemStack theShrine() {
		return book("what is required", "—",
			"""
			Do not sleep. Sleeping is how they find the way in and you have let them in every night of your life without once being asked.""",

			"""
			Bring nothing. Everything you carry belongs to the person you were and he is not welcome here.""",

			"""
			Stand where the light is and wait to be looked at.

			It will take as long as it takes. It has always taken as long as it takes.""",

			"""
			When you are looked at you will know, because you will want to leave.

			Do not leave.""");
	}

	private static ItemStack book(String title, String author, String... pages) {
		List<Filterable<Component>> written = new ArrayList<>();
		for (String page : pages) {
			written.add(Filterable.passThrough(Component.literal(page)));
		}
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(title), author, 0, written, true));
		return stack;
	}
}
