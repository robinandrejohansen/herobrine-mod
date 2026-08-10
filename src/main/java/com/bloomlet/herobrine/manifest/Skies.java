package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * The weather turns with him.
 *
 * Everything else in this mod happens to one player in one place. This happens
 * to the whole world at once, and it is the only thing here that a player
 * cannot walk away from — which makes it the cheapest atmosphere available and
 * the easiest to overdo.
 *
 * It is also completely deniable, which is why it is allowed to be so blunt.
 * Rain is rain. Nobody has ever looked at a wet afternoon and concluded
 * something was wrong, so this can push hard for hours before it registers as
 * anything at all — and then one day the player notices they cannot remember
 * the last clear night, and there is no moment they can point to when it
 * started.
 *
 * NOTHING AT RUMOUR. The first phase has to be a normal world with a few things
 * slightly off in it; a world that had already turned grey would be doing the
 * work the torch and the footsteps are supposed to do, and doing it worse.
 *
 * Only ever nudges. It starts weather and never stops it, so vanilla still ends
 * every storm on its own schedule and clear spells always come back. A player
 * who sleeps still clears the sky — they simply pay twelve wrath for it, which
 * is a trade worth leaving open.
 *
 * No lightning strikes. Deliberately: a bolt is an EVENT and events belong to
 * the director, so the storm stays a backdrop until there is something for the
 * lightning to be part of.
 */
public final class Skies {
	private Skies() {}

	/** Two minutes. Long enough that the sky is not visibly flickering. */
	private static final int CHECK_INTERVAL = 2400;

	/** Five to twelve minutes of it once it starts. */
	/**
	 * How long a storm runs. Nine to eighteen minutes, averaging thirteen.
	 *
	 * Longer than vanilla on purpose: the whole point of making them rare is
	 * that it buys the length. A storm that arrives after forty clear minutes
	 * and then sits over you for a quarter of an hour is an event. The same
	 * fifteen minutes arriving every eight is just the weather in this biome.
	 */
	private static final int STORM_MIN = 11000;
	private static final int STORM_SPREAD = 10000;
	private static final float STORM_MEAN = STORM_MIN + STORM_SPREAD / 2.0F;
	/** SIEGE: about three quarters wet, so a break is possible and rare. */
	private static final float SIEGE_ROLL = 0.45F;

	private static int tickCounter;

	/**
	 * THE SKY TURNS, NOW, BECAUSE SOMETHING HAPPENED.
	 *
	 * Everything else in this file is a slow dial — a wet fraction per phase, one
	 * decision per storm, deliberately unhurried so weather never becomes the
	 * mod's personality. This is the exception, and it is the exception for the
	 * one thing that earns it: a storm that arrives BECAUSE of something the
	 * players just did reads completely differently from the same storm arriving
	 * on a timer.
	 *
	 * Which is also what keeps mornings calm. The bad weather is a consequence
	 * rather than a climate, so the quiet days stay quiet and the sky going over
	 * means something every single time.
	 */
	public static void turn(ServerLevel level) {
		MinecraftServer server = level.getServer();
		if (server == null) {
			return;
		}
		RandomSource random = level.getRandom();
		int length = STORM_MIN + random.nextInt(STORM_SPREAD);
		server.setWeatherParameters(0, length, true, true);
		HerobrineMod.LOGGER.info("the sky turned — thunder for {} minutes", length / 1200);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Skies::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().weather) {
			return;
		}
		Phase phase = Wrath.phase(server);
		RandomSource random = server.overworld().getRandom();

		// AT SIEGE IT DOES NOT STOP RAINING.
		//
		// Every other phase rolls dice for it, because weather that always
		// does the same thing is not weather. This one is not weather. The
		// storm is simply the condition now, renewed before it can run out, so
		// a player never gets the twenty quiet minutes that would let them
		// believe it had passed.
		// SIEGE is heavy and it is not endless.
		//
		// It renewed the storm before it could ever run out, which is a fine
		// sentence in a design document and exhausting to actually live in —
		// and SIEGE can last days on a server. It now gets long storms with
		// real breaks between them: about three quarters of the time under
		// weather, and the other quarter a quiet grey sky that makes the next
		// one land. The night that never ends is already carrying this phase;
		// the sky does not have to do it as well.
		if (phase == Phase.SIEGE) {
			if (server.overworld().isRaining()) {
				return;
			}
			if (random.nextFloat() < SIEGE_ROLL) {
				server.setWeatherParameters(0, STORM_MIN + random.nextInt(STORM_SPREAD),
					true, true);
				HerobrineMod.LOGGER.info("the storm returns: SIEGE");
			}
			return;
		}

		// ALREADY WET? Then leave it entirely alone.
		//
		// The old version rolled again every two minutes while it was already
		// raining, to decide whether to add thunder — and setting the weather
		// again RESTARTS the timer, so a shower that should have blown over in
		// eight minutes kept renewing itself for as long as the dice were kind.
		// That, far more than the probabilities, is why it never stopped.
		//
		// One decision per storm, taken when it starts. After that the weather
		// runs down on its own and the sky is allowed to clear.
		if (server.overworld().isRaining()) {
			return;
		}

		if (random.nextFloat() >= rollFor(phase)) {
			return;
		}
		// Longer than vanilla, because a storm you notice has to outlast the
		// walk back indoors — and rare enough that the length is affordable.
		int length = STORM_MIN + random.nextInt(STORM_SPREAD);
		boolean teeth = random.nextFloat() < thunderShare(phase);
		server.setWeatherParameters(0, length, true, teeth);
		HerobrineMod.LOGGER.info("the weather turns: {} at {} for {} min",
			teeth ? "thunder" : "rain", phase.name(), length / 1200);
	}

	/**
	 * HOW MUCH OF THE TIME IT SHOULD BE WET, and everything else is derived.
	 *
	 * This used to be a per-check probability, which is an unreadable way to
	 * state a design: 0.40 at HUNTER sounds moderate and actually meant it was
	 * raining SIXTY-THREE PER CENT of the time. That is not weather, it is a
	 * climate, and a server playing for days through it got sick of the sound
	 * long before he ever did anything.
	 *
	 * So the number here is the thing that was actually meant — the fraction of
	 * play spent under rain — and rollFor() works backwards from it. When these
	 * are wrong now, they are wrong in a way somebody can see.
	 *
	 * The shape matters as much as the level. Storms are LONGER than vanilla
	 * and rarer, so each one arrives as an event rather than as the background:
	 * a clear morning, a clear afternoon, and then twelve minutes of thunder
	 * that means something because the last hour did not sound like it.
	 */
	private static float wetFraction(Phase phase) {
		return switch (phase) {
			case RUMOUR -> 0.00F;     // the world is still normal
			case WATCHER -> 0.08F;
			case TRESPASSER -> 0.13F;
			case MIMIC -> 0.20F;
			case HUNTER -> 0.28F;
			case SIEGE -> 0.75F;      // handled separately, above
		};
	}

	/**
	 * And of the wet time, how much of it has teeth in it.
	 *
	 * Thunder is the jump scare, so it is a share of the rain rather than its
	 * own roll — a storm that arrives out of a clear sky reads as scripted,
	 * and one that builds out of rain already falling reads as weather turning.
	 */
	private static float thunderShare(Phase phase) {
		return switch (phase) {
			case RUMOUR, WATCHER -> 0.0F;
			case TRESPASSER -> 0.20F;
			case MIMIC -> 0.35F;
			case HUNTER -> 0.55F;
			case SIEGE -> 1.0F;
		};
	}

	/**
	 * The per-check roll that produces that fraction over time.
	 *
	 * A check happens every CHECK_INTERVAL and a storm lasts about STORM_MEAN,
	 * so with a roll of p the expected dry stretch is CHECK_INTERVAL/p and
	 *
	 *     f = STORM_MEAN / (STORM_MEAN + CHECK_INTERVAL/p)
	 *
	 * which rearranges to the line below. Worth writing the derivation down
	 * because the first version of it was algebraically backwards and produced
	 * MORE rain than the numbers it replaced — and it looked perfectly
	 * reasonable while doing it.
	 */
	private static float rollFor(Phase phase) {
		float wet = wetFraction(phase);
		if (wet <= 0.0F) {
			return 0.0F;
		}
		return Math.min(1.0F, wet * CHECK_INTERVAL / (STORM_MEAN * (1.0F - wet)));
	}
}
