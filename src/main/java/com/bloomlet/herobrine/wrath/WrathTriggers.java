package com.bloomlet.herobrine.wrath;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What moves the number.
 *
 * Every entry here is something the younger brother also did. That is the
 * whole conceit — he is not reacting to a threat, he is recognising a
 * pattern.
 */
public final class WrathTriggers {
	private WrathTriggers() {}

	/** Baseline drift, so a world left alone still slowly remembers. */
	private static final int DRIFT_INTERVAL = 1200;   // once a minute
	private static final int DRIFT_AMOUNT = 1;

	/** Being deep is worth more than being alive, and it stacks with drift. */
	private static final int DEPTH_Y = 0;
	private static final int DEPTH_AMOUNT = 2;

	private static final int KILL_AMOUNT = 1;
	private static final int SLEEP_AMOUNT = 12;
	/** Dying to him bleeds pressure off, or phase 5 becomes a death spiral. */
	private static final int DEATH_RELIEF = -40;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(WrathTriggers::onTick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level().isClientSide()) {
				return;
			}
			if (entity instanceof ServerPlayer player) {
				// He is satisfied for a while.
				Wrath.add(serverOf(player), player, DEATH_RELIEF, Wrath.Reason.DEATH);
			} else if (source.getEntity() instanceof ServerPlayer killer) {
				// Killing one of his is not the same as killing a cow. It is
				// the only way to be rid of a follower, so it must cost —
				// otherwise the choice the possessed animal poses ("live with
				// it, or put it down") has an obviously correct answer and
				// stops being a choice at all.
				if (entity instanceof net.minecraft.world.entity.Mob mob
					&& com.bloomlet.herobrine.manifest.Possession.isPossessed(mob)) {
					// Awarded by Possession itself, which is the only place
					// that knows how many have been put down already — and the
					// value depends entirely on that.
					return;
				}
				Wrath.add(serverOf(killer), killer, KILL_AMOUNT, Wrath.Reason.KILL);
			}
		});

		EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayer player) {
				Wrath.add(serverOf(player), player, SLEEP_AMOUNT, Wrath.Reason.SLEEP);
			}
		});
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % DRIFT_INTERVAL != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator()) {
					continue;
				}
				Wrath.add(server, player, DRIFT_AMOUNT, Wrath.Reason.TIME);
				if (player.getY() < DEPTH_Y) {
					Wrath.add(server, player, DEPTH_AMOUNT, Wrath.Reason.DEPTH);
				}
			}
		}
	}

	/** Called when the player breaks something of his. The biggest single jump. */
	public static void defiance(ServerPlayer player, int amount) {
		Wrath.add(serverOf(player), player, amount, Wrath.Reason.DEFIANCE);
	}

	/** ServerPlayer has no getServer() in 26.2; the level owns the reference. */
	private static MinecraftServer serverOf(ServerPlayer player) {
		return ((ServerLevel)player.level()).getServer();
	}
}
