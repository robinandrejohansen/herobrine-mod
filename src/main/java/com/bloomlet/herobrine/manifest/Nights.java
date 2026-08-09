package com.bloomlet.herobrine.manifest;

import java.util.Optional;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

/**
 * The nights get longer. The days do not get shorter.
 *
 * That asymmetry is the entire design. Stretching the whole day/night cycle
 * would just be slowing the game down, and a player would feel it as their
 * farm being slower rather than as the dark lasting. Only the dark is
 * lengthened; the working day is exactly as long as it has always been, so
 * every hour the player loses is an hour they spend indoors listening.
 *
 * 26.2 made this properly supportable. Clocks are registry objects with a
 * RATE, and the server broadcasts rate changes to clients, so the sun visibly
 * slows rather than the sky stuttering as time is shoved backwards — which is
 * what any earlier approach would have looked like.
 *
 * Sleeping still skips the night entirely and always will. That is the trade
 * the whole mod is built on: the way out costs twelve wrath, it is always
 * available, and taking it every night is how a player walks themselves into
 * the later phases without noticing.
 */
public final class Nights {
	private Nights() {}

	/** Vanilla night runs from about 13000 to 23000 on the overworld clock. */
	private static final long NIGHT_FROM = 13000L;
	private static final long NIGHT_TO = 23000L;

	private static final int CHECK_INTERVAL = 40;

	private static int tickCounter;
	/** What we last asked for, so a rate is only ever set when it changes. */
	/**
	 * What we last asked for — and it is a CACHE OF SAVED STATE, which is the
	 * whole problem.
	 *
	 * Both of these start at "normal" every time the JVM does, while the thing
	 * they describe lives in the world file. So a world saved with the clock
	 * paused, or running at half speed, comes back with the code believing it
	 * is neither — and because both setters return early when the value they
	 * are asked for matches the cache, the correction never happens.
	 *
	 * The failure is silent and permanent: a night that was stopped at SIEGE
	 * stays stopped after a restart if wrath has since dropped below it, and no
	 * amount of playing puts the sun back. Reset on server start so the first
	 * tick re-applies the truth rather than trusting a value from a previous
	 * session.
	 */
	private static float applied = 1.0F;

	public static void register() {
		// Forget everything cached, and put the world back to normal before the
		// first tick decides what it should be. Belt and braces: the reset
		// alone would be enough for the code, but a world that had been left
		// paused by an older build would still be paused on disk, and nothing
		// would ever ask it not to be.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			applied = 1.0F;
			held = false;
			Optional<? extends Holder<WorldClock>> clock =
				server.overworld().registryAccess().get(WorldClocks.OVERWORLD);
			clock.ifPresent(c -> {
				server.clockManager().setPaused(c, false);
				server.clockManager().setRate(c, 1.0F);
			});
			HerobrineMod.LOGGER.info("clock handed back at startup; the tick will retake it");
		});

		ServerTickEvents.END_SERVER_TICK.register(Nights::onTick);
		// Put the world back before we go. A clock rate is saved data, so a
		// world left at half speed would stay that way after the mod was
		// removed, and the player would have no idea why their nights were
		// wrong and nothing to blame.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			apply(server, 1.0F);
			// A paused clock is saved data too. Leaving one behind would give
			// somebody a world with no sunrise and nothing to blame for it.
			hold(server, false);
		});
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().longerNights) {
			return;
		}
		long hour = server.overworld().getOverworldClockTime() % 24000L;
		boolean dark = hour >= NIGHT_FROM && hour < NIGHT_TO;
		Phase phase = Wrath.phase(server);

		// AT SIEGE THE NIGHT SIMPLY DOES NOT END.
		//
		// Not a slower clock — a stopped one. Slowing it further was the
		// obvious next step off the existing curve and it is the wrong shape:
		// a night at a third speed is a night that still ends, so a player
		// waits it out and the phase is something to be endured for twenty
		// minutes rather than a world they now live in.
		//
		// The sun not coming up at all is a different sentence. It is also
		// completely unambiguous, which matters for the LAST phase — every
		// other atmosphere effect here is deniable on purpose, and this one
		// must not be.
		//
		// Sleeping still works and is still the way out, which is the trade the
		// whole mod is built on. It costs wrath every time, and at SIEGE that
		// is the most expensive bargain in the game: the only way to see the
		// sun is to bring him closer.
		hold(server, com.bloomlet.herobrine.Config.get().endlessNight
			&& phase == Phase.SIEGE && dark);
		apply(server, dark ? rateFor(phase) : 1.0F);
	}

	/** What we last asked for, so the clock is only ever paused once. */
	private static boolean held;

	private static void hold(MinecraftServer server, boolean stop) {
		if (stop == held) {
			return;
		}
		Optional<? extends Holder<WorldClock>> clock =
			server.overworld().registryAccess().get(WorldClocks.OVERWORLD);
		if (clock.isEmpty()) {
			return;
		}
		server.clockManager().setPaused(clock.get(), stop);
		held = stop;
		HerobrineMod.LOGGER.info(stop ? "the night stops" : "the clock runs again");
	}

	private static void apply(MinecraftServer server, float rate) {
		if (rate == applied) {
			return;   // setting it broadcasts a packet to everyone; do it once
		}
		Optional<? extends Holder<WorldClock>> clock =
			server.overworld().registryAccess().get(WorldClocks.OVERWORLD);
		if (clock.isEmpty()) {
			return;
		}
		server.clockManager().setRate(clock.get(), rate);
		applied = rate;
		if (rate != 1.0F) {
			HerobrineMod.LOGGER.info("the night draws out: clock at {}x", rate);
		}
	}

	/**
	 * How much slower the dark runs.
	 *
	 * Nothing at RUMOUR, because the first phase has to be an ordinary world.
	 * Half speed at SIEGE, so a ten-minute night becomes twenty — which is a
	 * long time to be somewhere with the lights out and is meant to be.
	 *
	 * Deliberately stops at a half. A third would be atmospheric for one night
	 * and unplayable by the third, and DESIGN §9 is clear that nothing here
	 * gets to make the game tedious in the name of dread.
	 */
	private static float rateFor(Phase phase) {
		return switch (phase) {
			case RUMOUR -> 1.0F;
			case WATCHER -> 0.9F;
			case TRESPASSER -> 0.8F;
			case MIMIC -> 0.7F;
			case HUNTER -> 0.6F;
			case SIEGE -> 0.5F;
		};
	}
}
