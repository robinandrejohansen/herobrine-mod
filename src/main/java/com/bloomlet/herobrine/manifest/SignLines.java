package com.bloomlet.herobrine.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.bloomlet.herobrine.wrath.Phase;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
 * The important part, and the one thing a reader of this file should not
 * forget: at TRESPASSER these are WARNINGS. The brother is still in there and
 * is trying to get the player to stop, in the only language he has left. The
 * player will read them as threats and dig faster. Nothing ever tells them
 * otherwise. That is the whole reframe the story rests on, so early lines must
 * work read either way — "stop digging" is a warning and a threat with equal
 * force, which is exactly why it is the best line in the set.
 *
 * By HUNTER there is nothing of him left and the lines stop being about you at
 * all.
 */
public final class SignLines {
	private SignLines() {}

	/** Warnings, misread as threats. */
	private static final String[][] TRESPASSER = {
		{"go back"},
		{"stop digging"},
		{"this is deep", "enough"},
		{"not deeper"},
		{"it is awake"},
		{"it is not", "your house"},
		{"turn around"},
		{"i was here", "first"},
	};

	/** He knows who you are. Deniability is spent. */
	private static final String[][] MIMIC = {
		{"%s"},
		{"do you sleep"},
		{"you left the", "door open"},
		{"i can see", "the light"},
		{"%s", "stop"},
		{"i know where", "you sleep"},
	};

	/** Nothing of him is left. */
	private static final String[][] HUNTER = {
		{"you are the", "same"},
		{"i remember", "hands"},
		{"open it"},
		{"almost"},
		{"it is not", "me any more"},
	};

	/**
	 * Recently written lines, so he does not repeat himself.
	 *
	 * A player who finds the same four words twice has caught the machine, and
	 * a machine is not frightening. Kept generously long because there are few
	 * lines and they are the most memorable thing in the mod.
	 */
	private static final Deque<String> recent = new ArrayDeque<>();
	private static final int NO_REPEAT_FOR = 6;

	/** Identity of a line-set, for the no-repeat list. */
	private static String key(String[] lines) {
		return String.join("|", lines);
	}

	/** @return up to four lines for a sign, or null if everything is spent */
	public static String[] pick(Phase phase, ServerPlayer player, BlockPos where, RandomSource random) {
		List<String[]> pool = new ArrayList<>();
		java.util.Collections.addAll(pool, TRESPASSER);
		if (phase.atLeast(Phase.MIMIC)) {
			java.util.Collections.addAll(pool, MIMIC);
		}
		if (phase.atLeast(Phase.HUNTER)) {
			java.util.Collections.addAll(pool, HUNTER);
		}

		List<String[]> fresh = new ArrayList<>();
		for (String[] lines : pool) {
			if (!recent.contains(key(lines))) {
				fresh.add(lines);
			}
		}
		// Everything used lately. Better to write nothing than to repeat.
		if (fresh.isEmpty()) {
			return null;
		}

		String[] chosen = fresh.get(random.nextInt(fresh.size()));
		recent.addLast(key(chosen));
		while (recent.size() > NO_REPEAT_FOR) {
			recent.removeFirst();
		}

		String[] out = new String[chosen.length];
		for (int i = 0; i < chosen.length; i++) {
			// %s is the player's own name. Nothing else in the mod is as
			// effective, and nothing else is as easy to overuse.
			out[i] = chosen[i].replace("%s", player.getName().getString());
		}
		return out;
	}
}
