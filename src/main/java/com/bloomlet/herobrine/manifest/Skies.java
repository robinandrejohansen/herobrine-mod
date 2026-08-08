package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
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
	private static final int RAIN_MIN = 6000;
	private static final int RAIN_SPREAD = 8400;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Skies::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		Phase phase = Wrath.phase(server);
		RandomSource random = server.overworld().getRandom();

		if (server.overworld().isRaining()) {
			// Already wet. The only thing left to add is teeth.
			if (!server.overworld().isThundering() && random.nextFloat() < thunderChance(phase)) {
				server.setWeatherParameters(0, RAIN_MIN + random.nextInt(RAIN_SPREAD), true, true);
				HerobrineMod.LOGGER.info("the weather turns: thunder at {}", phase.name());
			}
			return;
		}
		if (random.nextFloat() < rainChance(phase)) {
			server.setWeatherParameters(0, RAIN_MIN + random.nextInt(RAIN_SPREAD), true, false);
			HerobrineMod.LOGGER.info("the weather turns: rain at {}", phase.name());
		}
	}

	/**
	 * How often a clear sky gives up.
	 *
	 * Checked every two minutes, so the numbers are smaller than they look —
	 * one in ten at WATCHER is a shower every twenty minutes or so, which is
	 * barely a bias. By SIEGE it is more than half, and clear weather has
	 * become the thing worth remarking on.
	 */
	private static float rainChance(Phase phase) {
		return switch (phase) {
			case RUMOUR -> 0.0F;      // the world is still normal
			case WATCHER -> 0.10F;
			case TRESPASSER -> 0.18F;
			case MIMIC -> 0.28F;
			case HUNTER -> 0.40F;
			case SIEGE -> 0.55F;
		};
	}

	/**
	 * And how often rain becomes a storm.
	 *
	 * Held back until MIMIC because thunder is not weather, it is a mood, and
	 * it also darkens the sky enough to spawn hostiles in daylight. Handing a
	 * player that while they are still deciding whether anything is happening
	 * would be a difficulty change dressed as atmosphere.
	 */
	private static float thunderChance(Phase phase) {
		return switch (phase) {
			case RUMOUR, WATCHER, TRESPASSER -> 0.0F;
			case MIMIC -> 0.12F;
			case HUNTER -> 0.30F;
			case SIEGE -> 0.50F;
		};
	}
}
