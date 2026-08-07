package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.wrath.Phase;

/**
 * The elder brother's account, sixteen torn pages.
 *
 * The only thing in the mod that carries the story. Signs are six words and
 * cannot; without these, the brother, the seal and the thing under the world
 * exist solely in LORE.md and never reach a player.
 *
 * It works without breaking the "never explain" rule because the author is
 * unreliable and the pages are incomplete. He is not writing a record for
 * anyone; he is working out what happened to his brother, and then doing
 * something about it. Nothing is addressed to the reader and nothing is
 * clarified for them.
 *
 * THE VOICE IS THE OPPOSITE OF THE SIGNS. Full sentences, past tense, first
 * person, correct punctuation — and degrading. One of them is a man writing
 * carefully, the other is what is left of a man who cannot. They must never
 * sound alike, so the punctuation in here is deliberate and the lack of it on
 * the signs is too.
 *
 * Nothing states that he was losing his mind. The prose simply stops being
 * written by someone who is fine.
 */
public final class JournalPages {
	private JournalPages() {}

	/** Sixteen pages, in order. Index 0 is page one. */
	private static final String[] PAGES = {
		// 1-3 — ordinary. Establish two people and a home worth losing.
		"""
		We finished the roof today.

		It took the whole summer and it is not straight, but it will keep the
		rain off, and he laughed at me from the ladder, so I have decided not
		to mind about the straightness.

		A good year.""",

		"""
		He has started digging.

		Not for anything. We have more stone than we can use and the iron is
		already stacked past the door.

		I asked him what he was looking for and he said he would know when he
		found it. He was pleased with the answer. I was not.""",

		"""
		Down again before light.

		I walked the shaft this morning while he slept. It goes further than I
		thought, and it is neat, which is the part I keep returning to. He is
		not in a hurry. He is doing it properly.

		Whatever is at the bottom, he means to reach it.""",

		// 4-6 — small wrongness. Nothing you could point at.
		"""
		He does not sleep now.

		I have stopped hearing him come up. I hear the pick until I fall
		asleep and I hear the pick when I wake, and I have not asked him about
		it because I am afraid of what a reasonable answer would mean.""",

		"""
		He is not eating.

		Three days that I have counted. He sits with us and he lifts the fork
		and he puts it down again, and he watches to see whether I have
		noticed.

		I have started pretending I have not.""",

		"""
		He has stopped pretending too.

		Tonight he did not lift the fork at all. He sat and he looked at me
		for a long time and then he smiled, and it was his smile, it was
		exactly his smile, and that is the thing I cannot get past.

		Something is wearing it correctly.""",

		// 7-9 — it is not him. The worst of it, in the fewest words.
		"""
		It is not him.

		I do not know when. I have gone back through this book looking for the
		day and there is no day. It is like looking for the moment a candle
		became dark.

		He is still in there. I am nearly sure. He looked at me this morning
		and something behind his face was trying very hard.""",

		"""
		The house is quiet now.

		I will not write what I found. I have thought about it and I have
		decided that I will not, because if anyone ever reads this I do not
		want them to carry it as well.

		It used his hands. He would never. It used his hands.""",

		"""
		Four days in the shaft.

		I could not stay up there. I have brought what I could carry and I am
		writing this by a torch he did not light, in a tunnel he dug, which is
		the only place I can think where he will not look for me.

		He is not looking for me. He is waiting.""",

		// 10-12 — the decision, and its cost.
		"""
		I have my axe and I cannot do it.

		I have stood over him twice. Both times he was still, and both times I
		thought: if there is any of him left, then this is murder, and if
		there is not, then it will not work anyway.

		I am not brave enough to be wrong about that.""",

		"""
		There is another way.

		The deep stone can be opened. He proved that, and the thing that came
		up through it proved it twice. What opens can be opened again, and a
		door that swings one way swings the other.

		I do not have to kill my brother. I have to send him back.""",

		"""
		It will cost.

		Nothing down there is free and I have stopped expecting it to be. I
		have read what I can and I understand perhaps half of it, and the half
		I understand says that the one who closes it stays close to it.

		I have decided. I would rather be near it than above it.""",

		// 13-15 — it worked, and it did not.
		"""
		It is done.

		He went through and it shut and I sat in the dark for a long time
		afterwards, and I am writing this because my hands have stopped
		shaking enough to hold the pen.

		I thought he might say something. He did not say anything.""",

		"""
		it is not a door

		I have been calling it a door for a year and it is not a door, it is a
		scar, and I know the difference now because I have been watching it
		and doors do not thin.

		I can hear it some nights. Not him. It.""",

		"""
		thinner

		i measure it. i have measured it every season since and i am the only
		one counting and the numbers are not going the way i want

		it is not years now. it is not going to be years

		whoever digs here next. do not. i am asking you. do not""",

		// 16 — a different hand.
		"""
		he is still here

		he never left. he sat down beside it so it would not open and he is
		still sitting there and he thinks that is the same as winning

		you can end me. bring what i have taken from you and i will be a body
		and a body can be ended

		i am asking too""",
	};

	/** How far into the account a player may get, by phase. */
	public static int maxPageFor(Phase phase) {
		if (phase.atLeast(Phase.SIEGE)) {
			return 16;
		}
		if (phase.atLeast(Phase.HUNTER)) {
			return 15;
		}
		if (phase.atLeast(Phase.MIMIC)) {
			return 11;
		}
		return 6;   // TRESPASSER
	}

	public static int count() {
		return PAGES.length;
	}

	/** @param page 1-based */
	public static String text(int page) {
		// Written as text blocks for readability here; Minecraft wraps its own
		// lines, so the source indentation has to come back out.
		return PAGES[page - 1].replaceAll("(?m)^\\s+", "").trim();
	}
}
