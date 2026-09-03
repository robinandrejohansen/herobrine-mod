package com.bloomlet.herobrine.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Heat;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
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
	/** However hot the player is and however bad he has got: never under this. */
	private static final int NEVER_TIGHTER = 2 * 60 * 20;

	/**
	 * Debug time compression. The real window is 8-20 minutes, which is right
	 * for play and useless for testing — you cannot judge pacing you have to
	 * wait an hour to see. This shrinks it so an evening of haunting happens
	 * in a few minutes.
	 */
	private static int speed = 1;

	private static final Map<UUID, Long> nextAllowed = new HashMap<>();
	/**
	 * Suppression, per player.
	 *
	 * Was shared, which meant one player's stare blocked another's — on a
	 * server two people playing together would quietly starve each other of
	 * content, and neither would ever know why. Suppression exists so an
	 * individual does not see the same thing twice running, so it belongs to
	 * the individual.
	 */
	private static final Map<UUID, Deque<Manifestation>> recent = new HashMap<>();
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ManifestationDirector::onTick);
	}

	private static Deque<Manifestation> recentFor(ServerPlayer player) {
		return recent.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
	}

	/**
	 * How much sooner he comes for a player who has provoked him.
	 *
	 * The seal is shared — the phase is a property of the world, because the
	 * door is as open as it is regardless of who opened it. But his ATTENTION
	 * is personal. A player who tears his signs down and chases him should be
	 * visited more than the one quietly farming next to them, and on a server
	 * that difference is the whole point: he is coming for someone specific.
	 */
	/**
	 * Somebody who has been provoking him gets seen to sooner.
	 *
	 * Was the per-player wrath share over 250, capped at triple. That share only
	 * ever climbed, so a player four hours in was permanently at the cap and the
	 * dial had stopped being a dial. Heat falls, so this can now go up AND back
	 * down — which is the entire reason the number was worth keeping.
	 */
	private static double attentionFactor(ServerPlayer player) {
		return 1.0 + 2.0 * Heat.scale(player);
	}

	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		long now = server.overworld().getGameTime();
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				// NOT IN HIS WORLD. A sign appeared in the castle mid-fight; the haunting
				// is what he does to YOUR world, and over there he is standing in front
				// of you.
				if (level.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
					continue;
				}
				if (player.isSpectator() || !player.isAlive()) {
					continue;
				}
				long due = nextAllowed.computeIfAbsent(
					player.getUUID(), id -> now + windowFor(player, level.getRandom()));
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

		Deque<Manifestation> mine = recentFor(player);
		List<Manifestation> eligible = new ArrayList<>();
		for (Manifestation m : Manifestation.values()) {
			if (m.allowed() && phase.atLeast(m.minimum) && !mine.contains(m)) {
				eligible.add(m);
			}
		}
		// Everything is suppressed — better to stay quiet than to repeat.
		if (eligible.isEmpty()) {
			reschedule(player, level, server);
			return null;
		}

		// Keep drawing until something actually runs. A pick that the world
		// cannot accommodate is not restraint, it is a wasted slot — the
		// player gets silence when a different event would have landed fine.
		// Only genuine exhaustion of the pool means a quiet night.
		Manifestation chosen = null;
		boolean happened = false;
		List<Manifestation> remaining = new ArrayList<>(eligible);
		while (!remaining.isEmpty() && !happened) {
			chosen = pick(remaining, phase, random);
			remaining.remove(chosen);
			happened = chosen.run(level, player);
			if (!happened) {
				HerobrineMod.LOGGER.debug("{} could not run here, trying another", chosen.name());
			}
		}

		if (happened) {
			mine.addLast(chosen);
			while (mine.size() > SUPPRESS_LAST) {
				mine.removeFirst();
			}
			// Logged so a playtester can correlate what fired with what they
			// felt. Without this the only record of a scare is a memory.
			HerobrineMod.LOGGER.info("{} at [{}, {}, {}] for {}{}",
				chosen.name(),
				(int)player.getX(), (int)player.getY(), (int)player.getZ(),
				player.getName().getString(),
				forced ? " (forced)" : "");
		}
		// Reschedule either way, forced included — otherwise a debug provoke
		// leaves the window armed and a natural manifestation lands seconds
		// later, which looks like a pacing bug.
		reschedule(player, level, server);
		return happened ? chosen : null;
	}

	/**
	 * Weighted pick, with the CURRENT phase's own content favoured heavily.
	 *
	 * Without this, every phase dilutes the one before it: by WATCHER the
	 * stare is one option in four and you barely ever see him, even though he
	 * is the entire point of that phase. Older content still appears — a late
	 * world should still get a quiet footstep — it just stops drowning out
	 * whatever the phase actually unlocked.
	 */
	private static final int CURRENT_PHASE_BOOST = 3;

	private static Manifestation pick(List<Manifestation> pool, Phase phase, RandomSource random) {
		Phase newest = newestWithContent(phase, pool);
		int total = 0;
		for (Manifestation m : pool) {
			total += weightIn(m, newest);
		}
		int roll = random.nextInt(Math.max(1, total));
		for (Manifestation m : pool) {
			roll -= weightIn(m, newest);
			if (roll < 0) {
				return m;
			}
		}
		return pool.get(pool.size() - 1);
	}

	private static int weightIn(Manifestation m, Phase newest) {
		return m.minimum == newest ? m.weight * CURRENT_PHASE_BOOST : m.weight;
	}

	/**
	 * The most recent phase that actually unlocked something.
	 *
	 * Not simply the current phase. A phase with no content of its own — which
	 * every phase is until it gets built — would boost nothing, so the
	 * previous phase's signature event silently loses its advantage and gets
	 * outweighed by the older traces again. Crossing into TRESPASSER would
	 * have made him appear LESS often than at WATCHER, which is backwards.
	 *
	 * Boosting the newest content the player has actually unlocked keeps the
	 * most recent thing prominent regardless of which phases are still empty.
	 */
	private static Phase newestWithContent(Phase current, List<Manifestation> pool) {
		Phase newest = Phase.RUMOUR;
		for (Manifestation m : pool) {
			if (current.atLeast(m.minimum) && m.minimum.ordinal() > newest.ordinal()) {
				newest = m.minimum;
			}
		}
		return newest;
	}

	private static void reschedule(ServerPlayer player, ServerLevel level, MinecraftServer server) {
		long now = server.overworld().getGameTime();
		nextAllowed.put(player.getUUID(), now + windowFor(player, level.getRandom()));
	}

	/** The base window, shortened by how much this player has provoked him. */
	/**
	 * AND IT TIGHTENS AS THE PHASE WEARS ON.
	 *
	 * The one place the intra-phase ramp is spent, and the highest-value place to
	 * spend it. Phases were a step function: the first minute of MIMIC and the
	 * last hour of it fired events at exactly the same rate, so a phase read as
	 * flat however good the individual events were. Six steps across a campaign
	 * is a staircase, not a shape.
	 *
	 * Heat is nought when he has been left alone and one when he has not, and
	 * the window closes to sixty per cent across that. The new thing therefore
	 * arrives once and quietly when the story turns, and by the end of the same
	 * chapter it is the weather — without a single new event existing.
	 *
	 * Only sixty per cent, deliberately. Doubling the rate would make late phases
	 * exhausting and would spend the quiet that makes any of this land; this is
	 * meant to be felt as a tightening rather than noticed as a number.
	 */
	private static int windowFor(ServerPlayer player, RandomSource random) {
		// One dial, applied once. This used to multiply Wrath.into here AND the
		// per-player share inside attentionFactor — two readings of the same
		// climbing total, compounding, which is how the gap between events could
		// quietly collapse to the forty-tick floor and stay there.
		// AND THE GAP CLOSES AS HE GETS WORSE.
		//
		// The POOL grew with the phase — nine things are possible at RUMOUR and
		// twenty-two by HUNTER — but the CLOCK never did, so the rate was one trace
		// every eight to twenty minutes from the first night to the last. Which
		// reads exactly as reported: something happens on the walk to the town, and
		// then it goes quiet, and stays quiet, because eight to twenty minutes is a
		// long time to be walking and nothing is broken.
		//
		// More KINDS of event at the same rate is variety. Escalation is the job.
		//
		// FLOORED AT TWO MINUTES, and that floor is the whole reason this is safe to
		// add. The comment above records what happened last time two climbing scales
		// were multiplied together: the gap collapsed to the forty-tick floor and
		// stayed there. Heat is the player's own attention and phase is the world's
		// state, so they are genuinely different axes — but two multipliers is two
		// multipliers, and a hard floor costs nothing and cannot be argued with.
		float tighter = switch (com.bloomlet.herobrine.wrath.Wrath.phase(
				((ServerLevel) player.level()).getServer())) {
			case RUMOUR -> 1.0F;
			case WATCHER -> 0.88F;
			case TRESPASSER -> 0.76F;
			case MIMIC -> 0.64F;
			case HUNTER -> 0.54F;
			case SIEGE -> 0.45F;
		};
		return Math.max(NEVER_TIGHTER,
			(int)(window(random) * tighter / attentionFactor(player)));
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

	/**
	 * Debug: run one specific manifestation, ignoring the pool, suppression
	 * and the phase gate. There is no way to test a single event otherwise —
	 * with the last two suppressed, several draws can pass before the one you
	 * want comes up.
	 *
	 * Deliberately does NOT reschedule or record: forcing a named event is an
	 * inspection, not a haunting, and it should not perturb the pacing you are
	 * about to observe.
	 */
	public static boolean runNamed(Manifestation manifestation, ServerLevel level, ServerPlayer player) {
		boolean happened = manifestation.run(level, player);
		HerobrineMod.LOGGER.info("{} forced by name -> {}", manifestation.name(),
			happened ? "ran" : "could not run here");
		return happened;
	}

	/** What is eligible right now, for debug reporting. */
	public static List<Manifestation> eligible(MinecraftServer server, ServerPlayer player) {
		Phase phase = Wrath.phase(server);
		Deque<Manifestation> mine = recentFor(player);
		List<Manifestation> out = new ArrayList<>();
		for (Manifestation m : Manifestation.values()) {
			if (m.allowed() && phase.atLeast(m.minimum) && !mine.contains(m)) {
				out.add(m);
			}
		}
		return out;
	}

	/** Recently used by this player, and therefore blocked for them. */
	public static List<Manifestation> suppressed(ServerPlayer player) {
		return new ArrayList<>(recentFor(player));
	}

	/**
	 * A manifestation happened and nobody perceived it.
	 *
	 * He is meant to be hard to catch — most visits should be missed, that is
	 * the whole behaviour. But a visit nobody experienced has still spent a
	 * window AND marked itself recently-used, so the next slot cannot give it
	 * either. The player gets a double silence for content that was delivered
	 * to an empty room.
	 *
	 * So an unwitnessed event is un-recorded: it may fire again immediately,
	 * and the window is pulled in. He gets another go at being noticed
	 * without the pacing budget paying for the miss.
	 */
	public static void wasted(Manifestation manifestation, ServerPlayer player) {
		recentFor(player).remove(manifestation);
		HerobrineMod.LOGGER.debug("{} went unwitnessed — not counted", manifestation.name());
	}

	/**
	 * Where the last manifestation put something.
	 *
	 * Debug only. Ruins are raised 28-60 blocks away out of sight, signs go
	 * behind you and pages onto the floor of somewhere you are not looking —
	 * all correct, and all of it means a tester has no idea where to go and
	 * cannot tell "it worked but I missed it" from "it silently failed".
	 */
	private static @org.jspecify.annotations.Nullable BlockPos lastLocation;

	public static void noteLocation(BlockPos pos) {
		lastLocation = pos;
	}

	public static @org.jspecify.annotations.Nullable BlockPos lastLocation() {
		return lastLocation;
	}

	/**
	 * Why the last attempt declined.
	 *
	 * Everything he does is placed out of sight deliberately, so a tester
	 * cannot tell a quiet success from a quiet refusal. "Wrong surroundings"
	 * covers half a dozen unrelated causes and names none of them, which cost
	 * a playtest session to a manifestation that was working exactly as
	 * designed and simply had nothing to work with.
	 */
	private static @org.jspecify.annotations.Nullable String lastRefusal;

	public static void refused(String reason) {
		lastRefusal = reason;
	}

	public static @org.jspecify.annotations.Nullable String lastRefusal() {
		return lastRefusal;
	}

	public static void clearRefusal() {
		lastRefusal = null;
	}

	/** Whether anything at all is eligible right now, for debug reporting. */
	public static boolean anythingEligible(MinecraftServer server, ServerPlayer player) {
		return !eligible(server, player).isEmpty();
	}
}
