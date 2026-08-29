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
 * What the people who lived here wrote, in the order a player finds it.
 *
 * SIX CHAPTERS, AND EACH ONE ENDS BY POINTING AT THE NEXT PLACE.
 *
 *     1  the homestead   a family, and a man standing in their field
 *     2  the town        it happens to everyone. they go underground
 *     3  the tower       they built somewhere to watch from. it did not work
 *     4  the gaol        they started locking up the ones who came back
 *     5  the church      the last thing anybody organised. they decide to dig
 *     6  the threshold   they open the way, and then they lie about it
 *
 * REWRITTEN AFTER THE FIRST FULL PLAYTHROUGH, and the note was blunt: the story
 * was too hard to follow. That is worth recording honestly, because the old
 * version was not badly written — it was written to a rule that turned out to be
 * the wrong one. The rule was NOTHING SUPERNATURAL IS EVER DESCRIBED: every
 * entry a domestic observation, unremarkable on its own, the horror left for the
 * reader to assemble. A boy who will not eat. A fence found open rather than
 * broken.
 *
 * It reads beautifully and it fails at the job. A player finds these one at a
 * time, hours apart, between two other things they were doing — and asking them
 * to hold six oblique fragments in their head across a whole evening and infer a
 * plot from the gaps is asking for reading they came here to avoid. Restraint
 * that nobody assembles is not restraint. It is a story that did not get told.
 *
 * So: SHORT LINES, PLAIN WORDS, AND THE WORST THING SAID OUT LOUD. The tally
 * still counts, but it says what it is counting. The tower still fails, but it
 * says who did not come down. Every chapter is four to six lines a page and ends
 * with somewhere to go.
 *
 * What survives from the old rule is the voice: these are not his. He writes in
 * lowercase fragments on walls. These people use capital letters and full stops
 * right up until the moment they stop writing, and that contrast still does more
 * than any adjective would.
 */
public final class HouseBooks {
	private HouseBooks() {}

	// ---- 1. THE HOMESTEAD --------------------------------------------------

	/** The mother. She is the one who decides they are leaving. */
	public static ItemStack household() {
		return book("the house book", "M.",
			"""
			Bread twice in the week.
			Wool from the four ewes at midsummer.

			J. fetches the water before dark.
			He is nine and he always forgets.

			Tonight he did not forget.
			He ran the whole way back.""",

			"""
			J. says there is a man standing at
			the treeline.

			He says the man does not move.

			R. went out with the lamp and found
			nothing at all. The grass is dead in
			a ring where J. says he stands.""",

			"""
			J. would not eat tonight.

			He said he was not hungry.
			He said it four times.

			I have never once had to ask that
			boy twice to eat.

			We are going to the town in the
			morning. All of us.""");
	}

	/** The youngest. Says the frightening thing without knowing it is one. */
	public static ItemStack child() {
		return book("my book", "the little one",
			"""
			There is a man in our field.

			We play the game where you do not
			blink. I always win.

			He never wins and he does not mind.""",

			"""
			I told mother about him.
			She went white and shut the door.

			She says do not look at him.

			But if I do not look at him then
			nobody is looking at him.""",

			"""
			He was closer this morning.

			Nobody moved him.""");
	}

	/** The father. Practical, right up until he is not. */
	public static ItemStack ledger() {
		return book("ledger", "R.",
			"""
			Four ewes. Two lambs. Eleven hens.
			Fence on the north cut wants mending.

			Something has been standing in the
			north cut. The ground is bare in a
			circle and nothing will grow there.""",

			"""
			Mended the fence.
			It was open again by morning.

			Not broken. OPENED.

			Whatever is out there has hands.""",

			"""
			We leave for the town tomorrow.

			I am writing this down so somebody
			knows we did not simply walk off and
			leave the animals.

			We were driven out of our own house.""");
	}

	/** Kept by whoever was awake. It stops counting days halfway down. */
	public static ItemStack tally() {
		return book("tally", "—",
			"""
			Days he has stood in the field:

			IIII IIII IIII IIII
			IIII IIII IIII IIII""",

			"""
			I stopped counting the days.

			I started counting the paces from
			the door to where he stands.

			Forty.
			Thirty-one.
			Nine.""");
	}

	/** The room with no window, and why a child asked for it. */
	public static ItemStack farRoom() {
		return book("the small room", "M.",
			"""
			We have put J. in the small room
			because it has no window.

			He asked for it himself.

			A boy of nine asked to sleep in a
			room with no window.""",

			"""
			He sleeps now.

			He talks while he sleeps and it is
			not his voice.

			R. sat outside that door all night
			with the axe across his knees.""");
	}

	/** In the undercroft, under the house. The one they shut in. */
	public static ItemStack brother() {
		return book("about my brother", "R.",
			"""
			My brother went under the hill in
			the spring.

			Something came back out in the
			autumn.""",

			"""
			It knows things about this house
			that he was never told.

			It calls me by our mother's name.

			I have put it below and I have
			barred the door.""",

			"""
			It does not knock.

			It waits.

			God forgive me, I can hear it
			breathing through the floor.""");
	}

	// ---- 2. THE TOWN -------------------------------------------------------

	/**
	 * Chapter two, and it is the one that was missing entirely.
	 *
	 * The town's story was only ever told DOWN in the undercity, in the survivors'
	 * accounts — six books that say what happened and one of which mentions the
	 * well. Every word of that is unreachable until you have already found the way
	 * down, so the chapter that is supposed to send you underground could only be
	 * read by somebody who had got there without it.
	 *
	 * This goes on the surface, at the well, in the open. It says the two things
	 * the player actually needs: THE TOWN IS UNDER THE TOWN, and the way in is the
	 * thing you are standing next to.
	 */
	public static ItemStack theTown() {
		return book("we went under", "the town",
			"""
			He walked into this square in the
			middle of the afternoon.

			Nobody stopped him.
			Nobody could say afterwards what
			he did. Only that eleven of us
			were gone by dark.""",

			"""
			So we went under the town.

			We dug out the cellars and joined
			them and we live down there now,
			in the dark, like something he put
			there.

			It is better than up here.""",

			"""
			The way down is the well.

			Go over the side and keep going.
			It is further than you think and
			the water does not last.

			If we are still alive we are at the
			bottom of it. Bring a light.""");
	}

	// ---- 3. THE TOWER ------------------------------------------------------

	/**
	 * Chapter three. They build somewhere to see him coming, and the point of
	 * the chapter is that seeing him coming was never the problem.
	 */
	public static ItemStack buried() {
		return book("the watch", "the watch",
			"""
			We built this to see him coming.

			Eighty feet of it, and a clear line
			to the wood on every side.

			Three of us. One awake at all times.""",

			"""
			Night forty. Nothing.
			Night forty-one. Nothing.

			Night forty-two, the man on the deck
			did not come down at dawn.

			His lamp was still burning.
			The stair was still barred.
			From the inside.""",

			"""
			You do not watch for him.

			He was up here before we were.

			The rest of us have gone to the gaol
			on the ridge. They have worked out
			what to do with the ones who come
			back wrong.""");
	}

	// ---- 4. THE GAOL -------------------------------------------------------

	/** Chapter four. Fourteen cells, and what they were really for. */
	public static ItemStack theDig() {
		return book("count them out", "the warder",
			"""
			Fourteen cells. We cut them in a
			week and we cut them badly.

			Not for thieves.

			For the ones who walk into that wood
			and walk back out of it.""",

			"""
			They look right. They talk right.

			Then they say a thing that only a
			dead man could know, and you put
			them behind iron and you do not
			open it again.

			Count them in. Count them out.""",

			"""
			Cell nine has been empty a month.

			The straw in it is still warm.

			We have given up. What is left of
			the town is in the church.

			Go there. It is the last thing
			anybody built on purpose.""");
	}

	// ---- 5. THE CHURCH -----------------------------------------------------

	/** Chapter five. They stop praying and pick up a shovel. */
	public static ItemStack theShrine() {
		return book("the last of us", "the last of us",
			"""
			Everyone still alive is in this
			room.

			We prayed for a month.

			Nothing came. Nothing left.""",

			"""
			So we have stopped praying and we
			have started digging.

			There is a way under the world and
			he has been using it the whole time.

			We have found the seam.
			We are going to open it and we are
			going to put him through it.""",

			"""
			If you are reading this, we did it.

			The stair behind this building goes
			down to what is left of the town.

			Take everything they left you.
			You are going to need all of it.""");
	}

	// ---- WHAT WAS LEFT AFTERWARDS ------------------------------------------

	/**
	 * A SECOND BOOK IN EVERY PLACE, AND IT IS A DIFFERENT KIND OF WRITING.
	 *
	 * The chapters above are the community's own record: written while it was
	 * happening, by people who did not yet know what it was, ending with somewhere
	 * to go. They are deliberately understated because the people writing them
	 * still thought they were describing an ordinary bad year.
	 *
	 * These are written by somebody who GOT OUT. They know exactly what it was,
	 * they saw what it did, and they are not describing a bad year — they are
	 * leaving a warning for whoever comes next and they have no reason left to be
	 * polite about it. So they say the thing plainly, once, and then they say what
	 * to do about it.
	 *
	 * That contrast is the point. Two books in one room, one written before and one
	 * after, and the distance between the voices is the whole story of the place.
	 *
	 * EVERY ONE ENDS WITH AN INSTRUCTION, because a warning that does not tell you
	 * what to do is a mood.
	 */
	public static ItemStack theHomesteadAfter() {
		return book("what he did to that family", "one who got out",
			"""
			I went back for the animals.

			The door was off its hinges and it
			was off them OUTWARD.

			Whatever left that house left it in
			a hurry and it did not use the
			handle.""",

			"""
			They were all still in the small
			room. All four.

			He had put them in there together
			and he had taken his time and I am
			not going to write down what I saw
			because you do not need it to
			believe me.""",

			"""
			The boy was not with them.

			Do not sleep in that house.
			Do not go down the hole under it.

			And if you find something wearing
			his face, it is not asking you for
			help.""");
	}

	public static ItemStack theTownAfter() {
		return book("eleven, in one afternoon", "one who got out",
			"""
			It was the middle of the day.

			He came in through the west lane
			and he was not in a hurry and
			nobody stopped him.

			Eleven of us by dark.""",

			"""
			We found them in the square.

			Not in one place. He had walked the
			whole town and left one in every
			doorway he passed, facing in, so
			that whoever opened it would be the
			one to see.

			The well ran red for two days.""",

			"""
			If you are standing in the square
			reading this then you are standing
			where they were.

			GO DOWN THE WELL.

			The rest of us are under your feet
			and we are still alive.""");
	}

	public static ItemStack theTowerAfter() {
		return book("we found what was left", "the last of the watch",
			"""
			Two of us went up at dawn for him.

			The stair was still barred from the
			inside. We took the bar off
			ourselves. Nobody had touched it.""",

			"""
			There was blood on the deck and on
			the inside of the wall and on the
			underside of the parapet, which is
			a place a man cannot bleed onto
			without being held above it.

			We did not find all of him.
			We found enough.""",

			"""
			His lamp was still lit and it had
			not been knocked over.

			Whatever did that had time.

			Do not stand watch alone. Do not
			stand watch at all. There is
			nothing to see until it is behind
			you.""");
	}

	public static ItemStack theGaolAfter() {
		return book("cell nine", "the warder",
			"""
			We put fourteen behind iron and it
			worked for a month.

			They stop eating first. Then they
			stop sleeping. Then they stand at
			the bars all night with their faces
			against them and they do not blink
			and they say your children's names.""",

			"""
			Cell nine went quiet on a Tuesday.

			The door was still locked. The bars
			were still whole. There was a great
			deal of blood and it was on the
			CEILING, in a spiral, and the man
			was gone.

			Nothing came out. Something left.""",

			"""
			Iron holds them. Iron does not hold
			HIM.

			If a cell is quiet, do not open it
			to find out why.

			We went to the church after that.
			It did not help either.""");
	}

	public static ItemStack theChurchAfter() {
		return book("the last night in here", "one who got out",
			"""
			Forty of us in one room with the
			doors barred and every candle we
			had left.

			We thought light was the answer.
			Light is how he finds the room.""",

			"""
			He did not break the door.

			He was already inside and he had
			been for some time, standing among
			us in the dark at the back, and
			when the first candle went out we
			learned how long.

			I got out through the graves.""",

			"""
			There were nine of us at the wall
			and four of us in the trees and one
			of me by morning.

			Do not gather. Do not count on
			numbers. Numbers only tell him how
			long it will take.

			Go under. Go to the seam.""");
	}

	public static ItemStack theThresholdAfter() {
		return book("what it cost to shut it", "the elder brother",
			"""
			It took nineteen of us to put him
			through and eleven came back up.

			We did not fight him. Nobody fights
			him. We made a hole and we made
			ourselves the reason he walked
			toward it.""",

			"""
			Eight people stood in a line and
			let him come, so that the ninth
			could be behind him when he did.

			I gave the order. I would give it
			again. That is the worst sentence
			in this book.""",

			"""
			We sealed it and we told everyone
			he was dead.

			HE IS NOT DEAD.
			He is only somewhere else.

			And if you are reading this at the
			bottom of that stair, you have
			already decided to go and get him,
			and there is nothing I can write
			that will stop you.""");
	}

	// ---- THE MECHANICS -----------------------------------------------------

	/**
	 * THIRTY-TWO CHARACTERS, AND GOING OVER DISCONNECTS THE PLAYER.
	 *
	 * A written book's title goes to the wire through Utf8String with a hard cap
	 * of 32 and nothing checks it on the way in. A 33-character title compiles,
	 * builds, boots and generates — and then the first person to OPEN the chest
	 * gets EncoderException: String too big on container_set_content, which does
	 * not throw an error in the chest, it severs the connection.
	 *
	 * It cost an evening once already, in Loot and Testimony, which both carry
	 * this guard. This file did not, purely because its titles happened to be
	 * short. Every title here is well under — and now it cannot stop being.
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
