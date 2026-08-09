package com.bloomlet.herobrine.wrath;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.bloomlet.herobrine.manifest.Cadence;
import com.bloomlet.herobrine.entity.HauntingSpawner;
import com.bloomlet.herobrine.HerobrineMod;
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
				owed(player);
			}
		});
	}

	/**
	 * YOU TOOK THE NIGHT OFF HIM, SO HE IS THERE WHEN YOU GET UP.
	 *
	 * Sleeping was already the largest single source of wrath and also, without
	 * anybody intending it, the thing that removed almost every window in which
	 * he could appear — he needs the dark, and a bed deletes the dark. The
	 * player pushing him hardest was the one who never saw him.
	 *
	 * This turns that accident into the best trade in the mod. The bed is still
	 * always there, it still always works, and it now costs something you can
	 * feel rather than only a number going up: you wake, you step outside into
	 * a bright clear morning, and he is standing at the treeline in it.
	 *
	 * Scheduled a little after waking rather than instantly. Appearing while
	 * the screen is still fading in would be a jump scare and would also be
	 * missed by anyone who alt-tabbed; twelve to twenty seconds is long enough
	 * to have got up, walked out, and stopped expecting anything.
	 *
	 * It goes through the ordinary spawner, so every rule still holds — behind
	 * them, out of arm's reach, somewhere they could actually look. If the
	 * morning does not suit it, nothing happens and nothing is said.
	 */
	private static void owed(ServerPlayer sleeper) {
		MinecraftServer server = serverOf(sleeper);
		if (server == null || !Wrath.phase(server).atLeast(Phase.WATCHER)) {
			return;
		}
		int delay = 240 + server.overworld().getRandom().nextInt(160);
		Cadence.in(server, delay, () -> {
			ServerPlayer awake = server.getPlayerList().getPlayer(sleeper.getUUID());
			if (awake == null || awake.isSleeping() || !awake.isAlive()) {
				return;
			}
			// ignoreLight, and that is the entire point of this. He is owed a
			// sighting BECAUSE it is broad daylight — refusing on brightness
			// here would reinstate exactly the hole this exists to close.
			HauntingSpawner.Outcome outcome = HauntingSpawner.place(
				(ServerLevel)awake.level(), awake, true);
			if (outcome == HauntingSpawner.Outcome.PLACED) {
				HerobrineMod.LOGGER.info("{} slept; he was waiting at dawn",
					awake.getName().getString());
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
