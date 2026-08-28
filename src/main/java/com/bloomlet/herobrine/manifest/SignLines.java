package com.bloomlet.herobrine.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;

/**
 * What he writes.
 *
 * Voice rules from LORE.md, and they are not decoration — they are what keeps
 * this from reading like a horror-game cliché generator:
 *
 *   lowercase, no full stops   punctuation reads as composed, and he is not
 *   four words beats ten       sign width is a gift, not a target
 *   present tense, second person
 *   never explain, never threaten outright — the threat is that he knows
 *   things, not that he says he will do something
 *
 * At TRESPASSER these are WARNINGS. The brother is still in there, trying to
 * make the player stop in the only language he has left. The player will read
 * them as threats and dig faster. Nothing ever tells them otherwise, so early
 * lines must work read either way — "stop digging" is a warning and a threat
 * with equal force, which is why it is the best line in the set.
 *
 * SPECIFICITY IS THE WEAPON. Generic dread is free and worthless. The lines
 * below are grouped by what the player has actually been doing, read from
 * vanilla stats and their position, so he comments on THIS player rather than
 * on players in general. "i watch you sleep" is a stock horror line; the same
 * line to someone who has not slept in six in-game days is not.
 *
 * AND HE IS NOT THREATENING TO KILL ANYBODY, because in this game death is
 * nothing. You respawn, you walk back, you pick your things up. A Herobrine who
 * says "you will die" is a mob with a text file. Everything he writes is aimed
 * at one outcome instead: LEAVE. Stop playing here, go somewhere else, log off
 * and do not come back. That is the only threat in Minecraft that cannot be
 * undone by respawning, and it is the whole spine of the mod — he is trying to
 * make the world not worth being in, and the players win by staying in it.
 *
 * So the register is eviction, not menace. Not "i will hurt you" but "nobody
 * stays", which is worse, because it is not about what he intends to do. It is
 * about what he has already watched happen to everybody else.
 */
public final class SignLines {
	private SignLines() {}

	// ---------------------------------------------------------------- generic
	/** Always available. Warnings, misread as threats. */
	private static final String[][] BASE = {
		{"go back"},
		{"stop digging"},
		{"i opened the", "last one who", "came down here"},
		{"the marrow", "is the best", "part of you"},
		{"it is awake", "and it is", "hungry"},
		{"you are mostly", "water and", "string"},
		{"i know where", "your seams", "are"},
		{"i can hear", "your heart", "from up here"},
		{"turn around"},
		{"i was here", "first"},
	};

	/**
	 * THE SPINE, and it is always in the pool.
	 *
	 * Every one of these is the same sentence with the volume changed: go, and
	 * do not come back. None of them threatens anything, which is what makes
	 * them work — "nobody stays" is not a thing he is going to do to you, it is
	 * a thing he has watched happen, and a fact is heavier than a threat
	 * because there is nothing to fight.
	 *
	 * "this is not your world" is the one that carries the whole framing: the
	 * overworld is his and always was, the players are the trespass, and every
	 * ruin they have found belongs to somebody who worked that out.
	 */
	private static final String[][] LEAVING = {
		{"go home"},
		{"nobody stays", "whole"},
		{"they left", "in pieces"},
		{"you can still", "leave", "most of you"},
		{"there is", "nothing here", "but the ground"},
		{"this is not", "your world"},
		{"how long"},
		{"others tried", "this", "i kept them"},
	};

	/**
	 * The one that stops pretending, and it is held back to HUNTER.
	 *
	 * Every other line in this file talks to the character. These talk to the
	 * person holding the mouse, and that is a switch that can only be thrown
	 * once before it becomes a gimmick. By HUNTER he has been in the world long
	 * enough that the players have started saying "he knows" out loud, and this
	 * is the sign that agrees with them.
	 *
	 * It is the single strongest thing in the mod and also the easiest to
	 * cheapen. Late, rare, and never explained.
	 */
	private static final String[][] FOURTH_WALL = {
		{"log off"},
		{"close the game"},
		{"go outside", "while you", "still can"},
	};

	// --------------------------------------------------------------- contextual
	/** Below y-0. His territory, and the thing his brother did. */
	private static final String[][] DEEP = {
		{"you went too", "far down"},
		{"he dug here", "too", "he is still", "down there"},
		{"there is", "nothing under", "this but me"},
		{"go up"},
	};

	/** Has not slept in a long time. */
	private static final String[][] SLEEPLESS = {
		{"do you sleep"},
		{"you never", "sleep"},
		{"sleep"},
	};

	/** Sleeps often — so he has watched them do it. */
	private static final String[][] SLEEPER = {
		{"i watch you", "sleep"},
		{"i have counted", "your teeth"},
		{"i know how", "you smell", "when you dream"},
		{"you left the", "door open"},
	};

	/** Has killed a great deal. He notices violence. */
	private static final String[][] KILLER = {
		{"you kill more", "than you need"},
		{"so many"},
		{"you are the", "same as me", "underneath"},
		{"i have seen", "what you are", "made of"},
	};

	/** Has died repeatedly. */
	private static final String[][] DYING = {
		{"again"},
		{"you keep", "coming back"},
		{"stay down"},
	};

	/** Has torn one of his signs down. */
	private static final String[][] DEFIANT = {
		{"you broke it"},
		{"i will write", "it again", "in something", "wetter"},
		{"%s", "stop"},
	};

	/** He knows the name. Only from MIMIC. */
	private static final String[][] NAMED = {
		{"%s"},
		{"i can see", "the light"},
		{"%s", "i see you"},
		{"%s", "i have been", "inside your", "house"},
	};

	/**
	 * Recently written lines, so he does not repeat himself.
	 *
	 * A player who finds the same four words twice has caught the machine, and
	 * a machine is not frightening. Kept generously long because there are few
	 * lines and they are the most memorable thing in the mod.
	 */
	private static final Deque<String> recent = new ArrayDeque<>();
	private static final int NO_REPEAT_FOR = 8;

	/** One in-game day of not sleeping. Phantoms use three. */
	private static final int SLEEPLESS_TICKS = 24000;
	private static final int MANY_KILLS = 60;
	private static final int MANY_DEATHS = 3;

	// ------------------------------------------------------------- graves
	/**
	 * Whose grave it is.
	 *
	 * Early ones are anonymous, and the anonymity is the point: per LORE.md
	 * the name has been scratched out so long that the scratching is the only
	 * legible part. A worn marker asks a question; a named one answers it.
	 *
	 * The family are here rather than invented strangers. Random names would
	 * read as filler — "she was first" is four words and tells you there was a
	 * household, and an order to what happened to it.
	 */
	private static final String[][] GRAVE_WORN = {
		{"- - - - -", "and his family"},
		{"- - - -", "who dug"},
		{"- - - - -", "she was first"},
		{"- - -", "the youngest"},
		{"- - - - -", "he did not", "know either"},
		{"- - - -", "and the others"},
		{"- - - - -", "one of them", "got out"},
		// THE GRAVES AND THE HOUSES ARE THE TWO HALVES OF HIS ARGUMENT, and it
		// is worth being deliberate about which is which. An empty house is
		// somebody who left. A grave is somebody who stayed. He shows the
		// players both, constantly, and the pairing does the work no single
		// sign can: leaving is what everyone does, and staying is what this
		// costs. He is arguing both sides because he only wants one outcome.
		//
		// It is also a lie, and the mod never says so. The players are the
		// counterexample, if they last.
		{"- - - -", "would not go"},
		{"- - - - -", "stayed"},
		{"- - -", "kept rebuilding"},
		{"- - - - -", "said it was", "his"},
	};

	/**
	 * Yours.
	 *
	 * Held back to MIMIC because a grave with your own name on it is the
	 * heaviest single image available here and it only works once. Spending it
	 * at TRESPASSER would burn it before the player knows enough for it to
	 * mean anything.
	 */
	private static final String[][] GRAVE_NAMED = {
		{"%s"},
		{"%s", "not the first"},
		{"%s", "and his family"},
	};

	/** Someone else who is in this world with you. */
	private static final String[][] GRAVE_OTHER = {
		{"%o"},
		{"%o", "and the others"},
	};

	/**
	 * @param other another player's name, or null when alone
	 */
	public static String[] grave(Phase phase, ServerPlayer player, String other,
	                             RandomSource random) {
		List<String[]> pool = new ArrayList<>();
		java.util.Collections.addAll(pool, GRAVE_WORN);
		if (phase.atLeast(Phase.MIMIC)) {
			java.util.Collections.addAll(pool, GRAVE_NAMED);
			if (other != null) {
				java.util.Collections.addAll(pool, GRAVE_OTHER);
			}
		}

		List<String[]> fresh = new ArrayList<>();
		for (String[] lines : pool) {
			if (!recent.contains(key(lines))) {
				fresh.add(lines);
			}
		}
		// A blank marker would look broken, so unlike wall signs a grave
		// falls back to repeating rather than saying nothing.
		if (fresh.isEmpty()) {
			fresh = pool;
		}

		String[] chosen = fresh.get(random.nextInt(fresh.size()));
		recent.addLast(key(chosen));
		while (recent.size() > NO_REPEAT_FOR) {
			recent.removeFirst();
		}

		String[] out = new String[chosen.length];
		for (int i = 0; i < chosen.length; i++) {
			out[i] = chosen[i]
				.replace("%s", player.getName().getString())
				.replace("%o", other == null ? player.getName().getString() : other);
		}
		return out;
	}

	private static String key(String[] lines) {
		return String.join("|", lines);
	}

	/** @return up to four lines for a sign, or null if everything is spent */
	public static String[] pick(Phase phase, ServerPlayer player, RandomSource random) {
		List<String[]> pool = new ArrayList<>();
		java.util.Collections.addAll(pool, BASE);
		java.util.Collections.addAll(pool, LEAVING);
		// Once, and only from HUNTER. Single weight against a pool of forty-odd
		// is roughly one sign in fifteen, which is the correct amount of a line
		// that is only devastating while it is still surprising.
		if (phase.atLeast(Phase.HUNTER)) {
			java.util.Collections.addAll(pool, FOURTH_WALL);
		}

		// Everything below is about THIS player. Weighted in twice, so a line
		// that fits what they have been doing beats a generic one more often
		// than not without ever excluding the generic set.
		for (String[][] group : relevant(phase, player)) {
			java.util.Collections.addAll(pool, group);
			java.util.Collections.addAll(pool, group);
		}

		List<String[]> fresh = new ArrayList<>();
		for (String[] lines : pool) {
			if (!recent.contains(key(lines))) {
				fresh.add(lines);
			}
		}
		if (fresh.isEmpty()) {
			return null;   // all said lately; better to say nothing
		}

		String[] chosen = fresh.get(random.nextInt(fresh.size()));
		recent.addLast(key(chosen));
		while (recent.size() > NO_REPEAT_FOR) {
			recent.removeFirst();
		}

		String[] out = new String[chosen.length];
		for (int i = 0; i < chosen.length; i++) {
			out[i] = chosen[i].replace("%s", player.getName().getString());
		}
		return out;
	}

	/** Which groups apply to what this player has actually been doing. */
	private static List<String[][]> relevant(Phase phase, ServerPlayer player) {
		List<String[][]> groups = new ArrayList<>();

		if (player.getY() < 0) {
			groups.add(DEEP);
		}

		int sinceRest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
		if (sinceRest > SLEEPLESS_TICKS) {
			groups.add(SLEEPLESS);
		} else {
			// They do sleep — which means he has had the chance to watch.
			groups.add(SLEEPER);
		}

		if (player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)) > MANY_KILLS) {
			groups.add(KILLER);
		}
		if (player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) >= MANY_DEATHS) {
			groups.add(DYING);
		}
		// Only provoking him gets it this high, and it falls back down again — so
		// this reads "they have been at it recently" rather than "they have been
		// playing a while", which is what the old cumulative share amounted to.
		if (com.bloomlet.herobrine.wrath.Heat.of(player) > 60) {
			groups.add(DEFIANT);
		}
		if (phase.atLeast(Phase.MIMIC)) {
			groups.add(NAMED);
		}
		// FOURTH_WALL is deliberately NOT here. Everything returned from this
		// method gets weighted into the pool twice by the caller, and that group
		// is the one thing in the file that has to stay rare after it unlocks.
		return groups;
	}
}
