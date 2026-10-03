package com.bloomlet.herobrine.manifest;

import java.util.Optional;

import com.bloomlet.herobrine.HerobrineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

/**
 * THE NIGHTS ARE VANILLA'S.
 *
 * This used to stretch every night to half speed at the last house and then
 * stop the clock at midnight for good — the night that never ended. It was the
 * biggest single part of "the world goes dark and stays dark", it hung off the
 * story phase, and phases now decide which house comes next and nothing else.
 *
 * What is left is the repair: a world saved while the old code had the clock
 * paused or slowed comes back with it paused or slowed, so on every start the
 * clock is handed back — running, at normal speed.
 */
public final class Nights {

	private Nights() {}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(Nights::handBack);
	}

	private static void handBack(MinecraftServer server) {
		Optional<? extends Holder<WorldClock>> clock =
			server.overworld().registryAccess().get(WorldClocks.OVERWORLD);
		clock.ifPresent(c -> {
			server.clockManager().setPaused(c, false);
			server.clockManager().setRate(c, 1.0F);
		});
		HerobrineMod.LOGGER.info("the clock is vanilla's: running, at normal speed");
	}
}
