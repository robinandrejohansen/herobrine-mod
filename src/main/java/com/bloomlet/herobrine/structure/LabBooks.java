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
 * STEVE'S OWN NOTEBOOK, FROM THE TIME.
 *
 * HouseBooks.theThresholdAfter is Steve confessing years afterwards, with the
 * headings gone and the worst sentence written on purpose. These are the notes
 * he was actually keeping WHILE HE DID IT, and the gap between the two registers
 * is the horror of the room: the retrospective one says "I made them out of my
 * neighbours". The contemporaneous one says "no change".
 *
 * A player finds both in the same building, feet apart. That is deliberate.
 *
 * NOTHING SUPERNATURAL IS EVER DESCRIBED IN HERE, and this is the one file where
 * that rule still earns its keep. No rituals, no incantations. A man measures a
 * thing he does not understand in the only vocabulary he has, which is a farm
 * ledger, and that is what makes it read like somebody's actual notebook.
 *
 * WHAT THIS FILE IS LOAD-BEARING FOR. Two things nothing else in the mod
 * explains:
 *
 *   GauntEntity. Subject nine is Corin, from the mill road. Taller than he was,
 *   no eating, no sleeping, no blinking, stands at the bars waiting for somebody
 *   to be curious. lastDay names the seven who walked out with him. Every tall
 *   pale thing in the forest is a neighbour of Steve's with a name.
 *
 *   Wendel. Subject three, the cleric who would not stop talking, is the same
 *   Wendel who writes HouseBooks.theShrine at the church years later and tells
 *   the player to ask Steve what he did to Herobrine. He is the only subject who
 *   stayed. Steve does not say why and did not ask.
 */
public final class LabBooks {
	private LabBooks() {}

	/**
	 * THE REGISTER, and it is deliberately the driest thing in the mod.
	 *
	 * A numbered list of neighbours by their trade does something no description
	 * could: it shows the exact moment they stopped being people and became
	 * entries, and it does it in the FORMAT rather than in the words. The worst
	 * line in it is a man noticing he has started writing "no change" as though it
	 * were a disappointment.
	 *
	 * Subject three is Wendel, cleric. Wendel is also the author of
	 * HouseBooks.theShrine, four sites and some years later — the man who tells you
	 * to ask Steve what he did to Herobrine before you let him near you. He knows
	 * because he was in one of these cells.
	 */
	public static ItemStack intake() {
		return book("the register", "Steve",
			"""
			One. Aldous, fletcher,
			from the east village.
			Two days. No change.

			Two. Hesk, farmer.
			Four days. No change.

			Three. Wendel, cleric.
			Six days. No change,
			and he would not stop
			talking, which I have
			decided is also no
			change.""",

			"""
			Four. Mila, farmer.
			Five. Bo, thatcher. Six.
			Ren, miller. Seven.
			Sera, weaver. Eight.
			Gild, the smith's boy,
			who is fifteen.

			No change. No change.
			No change. No change.
			No change.

			I have written those
			two words eleven
			times in this book.""",

			"""
			I have started writing
			them as though they
			were a disappointment.

			They are the only
			good news in here and
			I have started
			resenting them.

			I told all eight of them
			this was work. I paid
			them for the first
			week.""");
	}

	/**
	 * WHAT THE THING IN THE NEXT ROOM ACTUALLY IS, measured in a farm ledger's
	 * vocabulary because that is the only vocabulary he has.
	 *
	 * No ritual, no incantation, no glowing anything. A man notes that it opens for
	 * one person and not for him, builds a room around it to test that, and records
	 * the result in one word. "It can."
	 */
	public static ItemStack theDoor() {
		return book("on the door", "Steve",
			"""
			It is not a door.

			A door has two sides
			that agree about
			where they are.

			This has one side.
			Ours.

			Whatever is on the
			other side is not a
			place. It is a direction,
			and the direction is
			toward us.""",

			"""
			It gives when he is
			near it. It does not
			give when I am.

			That is the whole
			finding after eleven
			months, and it is the
			finding I did not want.""",

			"""
			So I have built a room
			around it and filled
			the room with people
			who are not him, to
			find out whether it
			can tell the
			difference.

			It can.

			It has never once
			opened for a farmer.""");
	}

	/**
	 * SUBJECT NINE IS WHERE THE GAUNTS COME FROM.
	 *
	 * Taller than he was, does not eat, does not sleep, does not blink, waits at
	 * the bars for somebody to be curious — that is GauntEntity, described from the
	 * outside by the man who made it. It is not a monster the mod happens to have;
	 * it is a person from the mill road called Corin.
	 *
	 * The last line is the one that matters: he has written that sentence before,
	 * years ago, about his brother.
	 */
	public static ItemStack subjectNine() {
		return book("nine", "Steve",
			"""
			Nine is Corin, from the
			mill road. Sixteen days.

			NINE CHANGED.

			Not the way Herobrine
			changed. Slower, and
			less of it, and then it
			stopped partway and
			stayed there.""",

			"""
			He is taller than he
			was. That should not
			be possible and I
			have measured him
			four times.

			He does not eat and
			he does not sleep and
			he does not blink, and
			when I stand at the
			bars he stands at the
			bars and waits for me
			to be curious.""",

			"""
			Nine answers to his
			own name.

			He answers to mine as
			well, which is new.

			I have written that
			sentence before.
			Years ago. About my
			brother.""");
	}

	/**
	 * AND SEVEN OF THEM WALKED OUT.
	 *
	 * The bars are out, not in. This is the book that puts every tall silent thing
	 * in the forest into the story by name — Aldous, Hesk, Mila, Bo, Ren, Sera and
	 * a smith's boy of fifteen — and hands the player the sentence the whole mod
	 * needed: they are mine, I made them out of my neighbours, and they are still
	 * out there waiting for somebody to be curious.
	 *
	 * Wendel is the only one who stayed. He does not say why and he did not ask.
	 */
	public static ItemStack lastDay() {
		return book("the last day", "Steve",
			"""
			The bars are out, not
			in.

			I want that written
			down because nobody
			is going to believe it
			and I will not be here
			to say it twice.

			Corin did not break
			into anything. He
			walked OUT.""",

			"""
			The others went with
			him. Aldous, Hesk, Mila,
			Bo, Ren, Sera, and the
			smith's boy who is
			fifteen.

			Seven of them, up the
			shaft, in the dark,
			without a lamp between
			them.""",

			"""
			Wendel is the only one
			who stayed and I do
			not know why and I did
			not ask.

			If you have seen a
			tall pale thing
			standing still in the
			wood, three blocks of
			it, silent, shaped
			almost like a person
			and not quite:""",

			"""
			That is Corin. Or it is
			one of the seven.

			They are mine. I made
			them out of my
			neighbours in a room I
			dug myself, and they
			are still out there,
			and they are still
			standing very still,
			and they are still
			waiting for somebody
			to be curious.""",

			"""
			I have put the door
			back. Stone, and more
			stone, and everything
			I had left.

			It is not going to hold.
			It was never a door.""");
	}

	/**
	 * PLAINLY, ONCE.
	 *
	 * The confession as he wrote it AT THE TIME, years before the six documents. He
	 * did not do it to save his friend. He did it to find out whether it could have
	 * been him, and the answer was yes.
	 *
	 * Then the count, which is the point of the whole file: nine names, and three
	 * he had stopped writing down. Three graves at the farm and none of them are
	 * theirs.
	 */
	public static ItemStack whatIWas() {
		return book("plainly, once", "Steve",
			"""
			I want this written
			plainly, one time, and
			then I am going to
			stop keeping notes.

			I did not do any of
			this to get Herobrine
			back.""",

			"""
			He was gone before
			we broke through the
			hill. I knew it the day
			he came up. He said my
			mother's name and my
			mother had been dead
			eleven years.

			I did it because I
			wanted to know
			whether I COULD HAVE.""",

			"""
			Whether it could have
			been my hand on that
			door. Whether there
			was something in me
			that would have come
			up out of that hole
			the same way he did.

			There was.""",

			"""
			That is what I found
			out. That is the whole
			of what I found out,
			and it cost twelve
			people who thought
			they were being paid
			to dig.""",

			"""
			Aldous. Hesk. Wendel.
			Mila. Bo. Ren. Sera. Gild.
			Corin. And three I did
			not write down
			because I had
			stopped writing them
			down.

			There are three
			graves at the farm
			and none of them are
			theirs.""",

			"""
			I know exactly what
			that means and so do
			you now.""");
	}

	private static final int TITLE_FITS = 32;

	/**
	 * The wire caps a book title at 32 characters and THROWS rather than truncating.
	 *
	 * HouseBooks has carried this guard since a 41-character title disconnected the
	 * player who opened the chest — an EncoderException out of Utf8String, which
	 * looks like a network fault and not like a typo in a book. This file had no
	 * guard at all and passed the title straight through.
	 */
	private static String title(String wanted) {
		if (wanted.length() <= TITLE_FITS) {
			return wanted;
		}
		String short_ = wanted.substring(0, TITLE_FITS).trim();
		com.bloomlet.herobrine.HerobrineMod.LOGGER.warn(
			"book title was {} characters and the wire allows {} — \"{}\" became \"{}\"",
			wanted.length(), TITLE_FITS, wanted, short_);
		return short_;
	}

	private static ItemStack book(String title, String author, String... pages) {
		List<Filterable<Component>> written = new ArrayList<>();
		for (String page : pages) {
			written.add(Filterable.passThrough(Component.literal(page)));
		}
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(title(title)), author, 0, written, true));
		return stack;
	}
}
