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
 */
public final class SignLines {
	private SignLines() {}

	// ---------------------------------------------------------------- generic
	/** Always available. Warnings, misread as threats. */
	private static final String[][] BASE = {
		{"go back"},
		{"stop digging"},
		{"this is deep", "enough"},
		{"not deeper"},
		{"it is awake"},
		{"it is not", "your house"},
		{"turn around"},
		{"i was here", "first"},
		{"i do not", "like you"},
		{"you think i", "cannot see"},
	};

	// --------------------------------------------------------------- contextual
	/** Below y-0. His territory, and the thing his brother did. */
	private static final String[][] DEEP = {
		{"you went too", "far down"},
		{"he dug here", "too"},
		{"there is nothing", "under this"},
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
		{"i know where", "you sleep"},
		{"you left the", "door open"},
	};

	/** Has killed a great deal. He notices violence. */
	private static final String[][] KILLER = {
		{"you kill more", "than you need"},
		{"so many"},
		{"you are the", "same"},
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
		{"i will write", "it again"},
		{"%s", "stop"},
	};

	/** He knows the name. Only from MIMIC. */
	private static final String[][] NAMED = {
		{"%s"},
		{"i can see", "the light"},
		{"%s", "i see you"},
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

	private static String key(String[] lines) {
		return String.join("|", lines);
	}

	/** @return up to four lines for a sign, or null if everything is spent */
	public static String[] pick(Phase phase, ServerPlayer player, RandomSource random) {
		List<String[]> pool = new ArrayList<>();
		java.util.Collections.addAll(pool, BASE);

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
		// Their personal share only climbs this high through defiance.
		if (Wrath.getShare(player) > 120) {
			groups.add(DEFIANT);
		}
		if (phase.atLeast(Phase.MIMIC)) {
			groups.add(NAMED);
		}
		return groups;
	}
}
