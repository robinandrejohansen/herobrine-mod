package com.bloomlet.herobrine.wrath;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * HE NOTICED, AND IT WEARS OFF.
 *
 * All that is left of four hundred and forty-five lines of wrath, and it does
 * the one thing that system did which a clock cannot: cause and effect. You
 * break something of his, and something happens sooner than it was going to.
 * That is worth keeping. Everything else about wrath was not.
 *
 * WHAT WAS WRONG WITH THE OLD ONE was not the arithmetic, it was the shape. It
 * only ever climbed — a total, accumulated from sleeping and mining and killing,
 * which are things people do without meaning anything by them. So a group's
 * anger reading rose steadily whatever they did, and the mod could not express
 * the single most valuable move in horror pacing: going quiet before a peak. A
 * number that cannot come down cannot build tension, it can only report age.
 *
 * This one falls. One point a second, from a ceiling of a hundred — so a
 * provocation buys about a minute and a half of him being closer to the surface,
 * and then the world settles again. The settling is the point. It means the next
 * provocation has somewhere to rise FROM.
 *
 * PER PLAYER, and in memory rather than in the save file. Both follow from what
 * it measures: it is about what YOU just did, not what the server has been up to
 * all week, and a value with a ninety-second half-life has no business surviving
 * a restart. Nothing here needs migrating and nothing here can go stale.
 *
 * It does NOT touch the story. Phases move when somebody walks into one of his
 * buildings, and that is the only thing that moves them — see Wrath.discovered.
 * Two dials that both claimed to be progress is exactly the confusion this
 * replaces.
 */
public final class Heat {
	private Heat() {}

	/** A minute and a half from the top. */
	private static final int CEILING = 100;
	private static final int DECAY_INTERVAL = 20;

	private static final Map<UUID, Integer> BURNING = new HashMap<>();
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Heat::onTick);
	}

	/**
	 * Somebody did something he would take an interest in.
	 *
	 * The one entry point, and every caller of the old WrathTriggers.defiance
	 * arrives here — striking him, breaking his signs, walking at him, killing
	 * something he was wearing. The amounts are the same ones those calls already
	 * passed, so the relative weights that were tuned in play are preserved; only
	 * the ceiling and the decay are new.
	 */
	public static void noticed(ServerPlayer player, int amount) {
		if (player == null || amount <= 0) {
			return;
		}
		int now = Math.min(CEILING, of(player) + amount);
		BURNING.put(player.getUUID(), now);
		HerobrineMod.LOGGER.debug("{} drew his eye (+{}, now {})",
			player.getName().getString(), amount, now);
	}

	/** Nought to a hundred. */
	public static int of(ServerPlayer player) {
		Integer held = BURNING.get(player.getUUID());
		return held == null ? 0 : held;
	}

	/** Nought to one, for anything scaling itself off how stirred up he is. */
	public static float scale(ServerPlayer player) {
		return of(player) / (float) CEILING;
	}

	/**
	 * Cools everybody, and forgets anybody who has left.
	 *
	 * The removal matters on a long-running server: a map keyed by every UUID
	 * that ever logged in is a small leak that nobody would ever notice and that
	 * has no reason to exist, since a player who is not here is not being
	 * watched.
	 */
	private static void onTick(MinecraftServer server) {
		if (++tickCounter % DECAY_INTERVAL != 0 || BURNING.isEmpty()) {
			return;
		}
		BURNING.entrySet().removeIf(entry -> {
			if (server.getPlayerList().getPlayer(entry.getKey()) == null) {
				return true;
			}
			entry.setValue(entry.getValue() - 1);
			return entry.getValue() <= 0;
		});
	}
}
