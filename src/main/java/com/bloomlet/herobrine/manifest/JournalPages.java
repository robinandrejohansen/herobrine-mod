package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.wrath.Phase;

/**
 * The elder brother's account, sixteen torn pages.
 *
 * The only thing in the mod that carries the story. Signs are six words and
 * cannot; without these, the brother, the seal and the thing under the world
 * exist solely in README.md and never reach a player.
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
	/**
	 * Sixteen pages, in order. Index 0 is page one.
	 *
	 * THIRD PERSON, AND THAT IS THE WHOLE REFRAME.
	 *
	 * This used to be the elder brother's diary — first person, present tense,
	 * degrading into somebody who could no longer punctuate. It was good and it was
	 * the wrong document, for one reason: a diary is written by a man who does not
	 * know how it ends. That means it cannot contain the thing the overworld now
	 * has to carry, which is THE OFFICIAL VERSION.
	 *
	 * So it is an account, written afterwards, by somebody who was not there for
	 * most of it and has taken statements from people who were. Past tense.
	 * Careful. Occasionally admitting what it does not know, which is what makes
	 * the two or three places it stops being careful land like a dropped plate.
	 *
	 * WHAT THE WORLD BELIEVES IS THAT HE WAS KILLED. Thirty men went up that valley
	 * and came back saying they had finished it, and that is the story on this side
	 * of the way — a thing that happened once, to somebody else, a long time ago,
	 * and was dealt with. Page fifteen is that version, written down and agreed on
	 * and NAMING THE BRAVE MEN. It is also the page that quietly says it is wrong.
	 *
	 * Which is what the whole dimension rests on. He is not a rumour that turned
	 * out to be true; he is a closed case. Everybody knows he is dead. The only
	 * thing anybody got wrong is what dead means, and the reader finds that out
	 * five buildings later with a plank in their hands.
	 *
	 * THE GATING FALLS WHERE THE STORY DOES, and it was not arranged to — the
	 * chapter breaks in maxPageFor already sat at 6, 11, 15 and 16. Six is the
	 * night he came up and stopped. Eleven is the men who went to meet him. Fifteen
	 * is the official version and the line under it. Sixteen is not this writer.
	 */
	private static final String[] PAGES = {
		// 1-3 — the valley. Ordinary, and warm, because none of the rest of it
		// costs anything unless there was something here worth losing.
		"""
		There were two brothers on the holding at the head of the valley, and for
		a long time that is all there is to say about them.

		They put the roof on in the summer of the second year. It was not
		straight. The elder was proud of it anyway and the younger laughed at him
		from the ladder, and neither of them thought to write any of this down,
		which is why it is being written now by somebody else.""",

		"""
		The younger one began to dig in the autumn.

		Not for anything. The holding had more stone than it could use and the
		iron was stacked past the door. When he was asked what he was looking for
		he said that he would know when he found it, and he was pleased with the
		answer.

		His brother was not, and said so, and it made no difference.""",

		"""
		The shaft went down a great deal further than the holding needed.

		What the elder remarked on afterwards was not the depth. It was the
		workmanship. The walls were true and the steps were even and the man
		cutting them was in no hurry at all.

		Whatever was at the bottom, he meant to reach it, and he meant to be able
		to walk back up.""",

		// 4-6 — the change. Nothing anybody could have acted on.
		"""
		He stopped sleeping some time that winter and nobody has been able to say
		when.

		His brother heard the pick when he went to bed and heard the pick when he
		woke. He did not ask about it. He has since explained that he had worked
		out there was no answer to that question he wanted to be given.""",

		"""
		He stopped eating in the spring.

		He came up for water and went back down. The hands went wrong before the
		rest of him did — the nails first, and then the way he held things, which
		was too hard, as though he had forgotten what was in his hand and how much
		of it he still needed.

		He broke two shovels that season and did not notice either of them.""",

		"""
		On a night in the summer he came up and did not go back down.

		He stood in the doorway of the house for some time. He was not carrying
		the pick. He was not carrying anything, and his hands and his forearms to
		the elbow were wet to the skin, and it was too dark to see with what.

		His brother asked him a question. He did not answer it. He was looking at
		the ceiling.""",

		// 7-11 — what he did. The account stops being careful in the middle of
		// this and the reader should be able to feel where.
		"""
		The animals went first, in the byre, and it was done quietly enough that
		nobody in the house woke.

		They were not killed for meat and they were not killed for sport. They
		were opened, and looked at, and left where they lay. A man who had raised
		those animals from calves had gone in there and taken them apart the way
		you take apart a thing you have been given to study.""",

		"""
		His wife was found at the foot of the stair.

		The statement describing who found her, and what he did for the hour
		afterwards, was taken down at the time and has since been lost. That is
		probably a mercy and it is certainly a loss.

		What can be set down is that it was done with the hands. There was a pick
		in that house and he did not go and fetch it.""",

		"""
		The children were in the loft and he went up after them.

		Nothing further is going to be written on this page.

		Two were buried whole. There is a third marker in the row and there is
		nothing underneath it.""",

		"""
		The elder came in from the low field at dusk and stood in the doorway of
		his brother's house for a very long time.

		He did not go in. He said afterwards, once, that he had not been able to
		work out where to put his feet.

		He was forty-one years old. He went out to the byre and sat down against
		the wall and stayed there until it was light.""",

		// 11 — the first attempt. This is where the valley learns what it has.
		"""
		Eleven men came up the valley when the word got out.

		They were farmers and they brought what farmers bring. He met them in the
		open, in daylight. He was not hiding and he was not running, and it was
		afterwards agreed by everybody still able to agree that he had been
		waiting for them.

		Four went home. Two of those did not go home with both arms.""",

		// 12-14 — what was done about it.
		"""
		The second attempt was made three weeks later, by men who knew what they
		were walking towards.

		They brought iron, and fire, and thirty of themselves. It is recorded that
		they hurt him, which nothing before them had managed, and it is recorded
		by four separate hands that he laughed while they were doing it.

		They did not kill him. They drove him back down the shaft, and they
		believed at the time that this was the same thing.""",

		"""
		It was the elder who went down after him, and he would not let anyone
		follow.

		The account he gave was short and he never gave it twice: that his brother
		was at the bottom, in a place that was no longer the bottom of a shaft,
		and that his brother knew him, and said his name.

		He had an axe with him. He did not use it. He has never explained why
		not and has never been made to.""",

		"""
		What he did instead was tear the world.

		He does not describe this and it is likely he could not. There was a hole
		where no hole had been. He put his brother through it. Then he spent the
		remainder of that season making certain it stayed shut — first with stone,
		and then with other things, and at the last with something he will not
		name and cannot be drawn on.

		He came back up the shaft with two of his fingers missing.""",

		// 15 — THE OFFICIAL VERSION, and the line under it.
		"""
		The record closes that it was ended.

		That is the version the valley settled on and it is the version that is
		still repeated: that the thing at the head of the holding was fought, and
		driven down, and destroyed, and that the men who did it were brave. Nine
		of them are named. There is a stone.

		The man who sealed it has read this account through. He asked for one line
		to be added at the end of it and would not be persuaded to say more.

		The line is: he is not dead, he is only somewhere else.""",

		// 16 — a different hand, and it is not the writer's.
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
		//
		// [ \t] RATHER THAN \s, AND THAT ONE CHARACTER WAS A BUG SINCE THIS FILE
		// WAS WRITTEN. In Java \s matches a newline, so `^\s+` starting at the
		// indent of a line ran straight through the line break and the blank line
		// after it and the next line's indent — which ate every paragraph break in
		// every entry. Sixteen carefully paragraphed pages have been rendering as
		// one unbroken block of text this whole time, and nothing reported it
		// because a wall of prose in a torn journal looks like a stylistic choice.
		return PAGES[page - 1].replaceAll("(?m)^[ \\t]+", "").trim();
	}
}
