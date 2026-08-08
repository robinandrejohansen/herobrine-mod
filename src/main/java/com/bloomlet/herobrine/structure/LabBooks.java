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
 * What the elder brother was doing down here.
 *
 * The farmhouse books are a family writing about their lives. These are one man
 * writing up an experiment, and the change of register between the two is the
 * point: the person who wrote "he was under the hill two days, we dug for one
 * of them" is the same person who later wrote "four through eight, no change".
 * A player who reads the house and then reads this watches somebody stop
 * talking about his brother and start keeping records.
 *
 * The rule from HouseBooks still holds and matters more here — NOTHING
 * SUPERNATURAL IS EVER DESCRIBED. There are no rituals, no incantations, no
 * glowing anything. A man measures a thing he does not understand, in the only
 * vocabulary he has, and the vocabulary is a farm ledger. That is what makes it
 * land: it reads like somebody's actual notebook.
 *
 * And it is never gratuitous. What happened to the villagers is in the gaps
 * between entries and in a count that does not add up — never on the page. The
 * worst thing in these books is a man noticing he has started writing "no
 * change" as though it were a disappointment.
 */
public final class LabBooks {
	private LabBooks() {}

	/**
	 * The register.
	 *
	 * Deliberately the driest thing in the mod. A numbered list of people by
	 * their trade is doing something no description could: it shows the exact
	 * moment they stopped being neighbours and became entries, and it does it
	 * in the format rather than in the words.
	 */
	public static ItemStack intake() {
		return book("intake", "—",
			"""
			One. Fletcher, from the east village. Two days. No change.

			Two. Farmer. Four days. No change.

			Three. Cleric. Six days. No change, and he would not stop talking, which I have decided is also no change.""",

			"""
			Four through eight. No change.

			I have written "no change" nine times in this book.

			I have started writing it as though it were a disappointment.

			It is the only good news in here.""");
	}

	/**
	 * The research, such as it is.
	 *
	 * The one book that says outright what the cells are for, and it says it in
	 * the flattest possible sentence — "to see whether it can tell the
	 * difference" — because a man who was horrified by what he was doing would
	 * not have kept doing it for eleven months.
	 */
	public static ItemStack theDoor() {
		return book("on the door", "—",
			"""
			It is not a door. A door has two sides that agree about where they are.

			This has one side. Ours.

			Whatever is on the other is not a place. It is a direction, and the direction is towards us.""",

			"""
			It gives when he is near it and it does not give when I am. That is the whole finding after eleven months.

			So I have built a room around it, and filled the room with people who are not him, to find out whether it can tell the difference.

			It can.""");
	}

	/**
	 * The one that changed.
	 *
	 * Ends on a sentence lifted word for word out of the farmhouse — "he stands
	 * at them and waits for me to be curious" — and then tells the reader where
	 * it came from. A player who found the sealed room in house one gets that
	 * for free, and it is the best thing either book does.
	 */
	public static ItemStack subjectNine() {
		return book("nine", "—",
			"""
			Nine changed.

			Not the way he changed. Slower, and less of it, and then it stopped.

			Nine answers to his own name. He answers to mine as well, which is new.""",

			"""
			I have moved nine to the far cell.

			He has not tried the bars. He stands at them and waits for me to be curious.

			I have written that sentence before. Years ago. About my brother.""");
	}

	/** The end of it, and the only page written in a hurry. */
	public static ItemStack lastDay() {
		return book("the last day", "—",
			"""
			The bars are out, not in.

			I want that written down because nobody will believe it and I will not be here to say it twice.

			He did not break into anything. He walked out.""",

			"""
			I have put the door back. Stone, and more stone, and everything I had left.

			It is not going to hold. It was never a door.

			If you are reading this you have come a long way down, and I am sorry, because it means you were looking.""");
	}

	/**
	 * The confession, and the reason the whole place exists.
	 *
	 * Placed furthest in, because it recontextualises everything behind it: the
	 * three graves at the farmhouse, the ledger, the cells. He was not trying to
	 * save anybody. And the last line hands the player the arithmetic rather
	 * than the conclusion — three graves, four names, and nine intake numbers.
	 */
	public static ItemStack whatIWas() {
		return book("plainly, once", "—",
			"""
			I want this written plainly, once, and then I am going to stop writing.

			I did not do any of it to save him. He was gone before we broke through the hill. I knew it when he said my name.

			I did it because I wanted to know whether I could have.""",

			"""
			The others were farmers and fletchers and a cleric who would not stop talking.

			I told myself they were not part of it.

			There are three graves at the house and none of them are theirs, and I know exactly what that means.""");
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
