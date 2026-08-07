package com.bloomlet.herobrine.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Decides WHEN he does something, and what. Nothing else may.
 *
 * This exists because the failure mode of a mod like this is not a bad idea,
 * it is too many good ones firing at once. Three rules hold the line:
 *
 *   BUDGET       at most one manifestation per player per window, and the
 *                window is jittered. A fixed interval is predictable, and
 *                predictable horror is not horror.
 *   SUPPRESSION  the last few things he did cannot happen again yet. The
 *                same event twice running kills fear faster than anything.
 *   SILENCE      a failed attempt costs nothing and is not retried. If the
 *                world cannot accommodate it, he simply does not appear —
 *                far better than substituting a lesser event to fill the
 *                slot, which is how pacing turns into a metronome.
 */
public final class ManifestationDirector {
	private ManifestationDirector() {}

	/** Design target. Long on purpose — the quiet is the majority state. */
	private static final int WINDOW_MIN_TICKS = 8 * 60 * 20;
	private static final int WINDOW_MAX_TICKS = 20 * 60 * 20;
	/** How many recent manifestations are blocked from repeating. */
	private static final int SUPPRESS_LAST = 2;
	private static final int CHECK_INTERVAL = 100;

	/**
	 * Debug time compression. The real window is 8-20 minutes, which is right
	 * for play and useless for testing — you cannot judge pacing you have to
	 * wait an hour to see. This shrinks it so an evening of haunting happens
	 * in a few minutes.
	 */
	private static int speed = 1;

	private static final Map<UUID, Long> nextAllowed = new HashMap<>();
	private static final Deque<Manifestation> recent = new ArrayDeque<>();
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ManifestationDirector::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		long now = server.overworld().getGameTime();
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator() || !player.isAlive()) {
					continue;
				}
				long due = nextAllowed.computeIfAbsent(
					player.getUUID(), id -> now + window(level.getRandom()));
				if (now >= due) {
					attempt(server, level, player, false);
				}
			}
		}
	}

	/**
	 * @param forced debug path — ignores the budget but still respects
	 *               suppression and the world's own constraints, so what you
	 *               see when testing is what players will see.
	 * @return the manifestation that ran, or null
	 */
	public static Manifestation attempt(MinecraftServer server, ServerLevel level,
	                                    ServerPlayer player, boolean forced) {
		Phase phase = Wrath.phase(server);
		RandomSource random = level.getRandom();

		List<Manifestation> eligible = new ArrayList<>();
		for (Manifestation m : Manifestation.values()) {
			if (phase.atLeast(m.minimum) && !recent.contains(m)) {
				eligible.add(m);
			}
		}
		// Everything is suppressed — better to stay quiet than to repeat.
		if (eligible.isEmpty()) {
			reschedule(player, level, server);
			return null;
		}

		Manifestation chosen = pick(eligible, random);
		boolean happened = chosen.run(level, player);

		if (happened) {
			recent.addLast(chosen);
			while (recent.size() > SUPPRESS_LAST) {
				recent.removeFirst();
			}
		}
		// Reschedule either way, forced included — otherwise a debug provoke
		// leaves the window armed and a natural manifestation lands seconds
		// later, which looks like a pacing bug.
		reschedule(player, level, server);
		return happened ? chosen : null;
	}

	private static Manifestation pick(List<Manifestation> pool, RandomSource random) {
		int total = 0;
		for (Manifestation m : pool) {
			total += m.weight;
		}
		int roll = random.nextInt(Math.max(1, total));
		for (Manifestation m : pool) {
			roll -= m.weight;
			if (roll < 0) {
				return m;
			}
		}
		return pool.get(pool.size() - 1);
	}

	private static void reschedule(ServerPlayer player, ServerLevel level, MinecraftServer server) {
		long now = server.overworld().getGameTime();
		nextAllowed.put(player.getUUID(), now + window(level.getRandom()));
	}

	private static int window(RandomSource random) {
		int base = WINDOW_MIN_TICKS + random.nextInt(WINDOW_MAX_TICKS - WINDOW_MIN_TICKS);
		return Math.max(40, base / speed);
	}

	/** 1 is normal. Higher compresses the wait; 20 turns 8-20 min into 24-60s. */
	public static void setSpeed(int value, MinecraftServer server) {
		speed = Math.max(1, value);
		nextAllowed.clear();   // re-arm everyone against the new window
	}

	public static int speed() {
		return speed;
	}

	/** Seconds until this player's next possible manifestation. For /herobrine status. */
	public static long secondsUntilNext(MinecraftServer server, ServerPlayer player) {
		Long due = nextAllowed.get(player.getUUID());
		if (due == null) {
			return -1;
		}
		return Math.max(0, (due - server.overworld().getGameTime()) / 20);
	}

	/** Whether anything at all is eligible right now, for debug reporting. */
	public static boolean anythingEligible(MinecraftServer server) {
		Phase phase = Wrath.phase(server);
		for (Manifestation m : Manifestation.values()) {
			if (phase.atLeast(m.minimum) && !recent.contains(m)) {
				return true;
			}
		}
		return false;
	}
}
