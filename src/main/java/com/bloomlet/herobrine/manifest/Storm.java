package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;
import com.bloomlet.herobrine.wrath.Wrath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * THE ONLY THING THAT TOUCHES THE OVERWORLD'S WEATHER.
 *
 * The weather is vanilla's. It used to be the story's: storms rolled by phase,
 * rain at his address from the third place, thunder from the fourth, endless
 * night and a grey pall over everything at the last — and nobody liked it. The
 * world went dark and wet and stayed that way, and it meant nothing because it
 * was always there.
 *
 * Now there are exactly two ways the sky turns, and both are him:
 *
 *   HE IS HERE. Any time he is loaded within NEAR of a player — a glimpse, a
 *   stare, a passage, a hunt — it storms: rain and thunder for as long as he is
 *   there and LINGERS after. When he is gone, the sky clears.
 *
 *   HE DID SOMETHING. omen() is called when one of his works happens near a
 *   player (a door swings open, leaves fall off a wood, a fire starts, an
 *   animal turns). It rolls one of several shapes: a short thunderstorm, a
 *   short rain, or only thunder rolling dry over the hills with a bolt on the
 *   horizon. Short, and then normal again.
 *
 * ONLY OURS IS CLEARED. A storm that was already falling when he came is
 * vanilla's and is left alone; `ours` says whether the current one is his.
 * In 26.2 weather is server-wide, so this is the overworld's and nothing else's
 * — his world's sky is HisWeather's, built from per-level rain levels.
 *
 * Cost: one pass a second over the overworld's loaded Herobrines (there is at
 * most one) and its players.
 */
public final class Storm {

	private Storm() {}

	private static final int EVERY = 20;
	/** How close he has to be to a player for the sky to turn. */
	private static final double NEAR = 96.0;
	/** How long it goes on after he has gone. */
	private static final int LINGERS = 600;
	/** A storm that is ours is booked this long at a time and topped up while he stays. */
	private static final int HOLDS = 1200;
	/** After a storm of ours, clear for this long, then vanilla has the sky again. */
	private static final int CLEAR_MIN = 6000;
	private static final int CLEAR_SPREAD = 12000;

	private static boolean ours;
	private static boolean nearNow;

	/** He is in the overworld, in the world, near somebody — as of the last second. Cheap; cached. */
	public static boolean heIsNear() {
		return nearNow;
	}
	private static long until;
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Storm::onTick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ours = false;
			until = 0L;
		});
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % EVERY != 0 || !Config.get().enabled || !Config.get().weather) {
			return;
		}
		ServerLevel over = server.overworld();
		long now = over.getGameTime();
		nearNow = !Wrath.removed(server) && hereNow(over);
		if (nearNow) {
			if (!over.isThundering()) {
				if (!over.isRaining() || ours) {
					server.setWeatherParameters(0, HOLDS, true, true);
					if (!ours) {
						HerobrineMod.LOGGER.info("he is near somebody — the sky turns");
					}
					ours = true;
				}
			}
			if (ours) {
				until = Math.max(until, now + LINGERS);
			}
			return;
		}
		if (ours && now >= until) {
			clear(server);
		}
	}

	/** He is loaded in the overworld, in the world (not hidden), within NEAR of somebody. */
	private static boolean hereNow(ServerLevel over) {
		if (over.players().isEmpty()) {
			return false;
		}
		for (HerobrineEntity him : HerobrineEntity.all(over)) {
			if (!him.isAlive() || him.isRemoved() || !him.isPresent()) {
				continue;
			}
			for (ServerPlayer player : over.players()) {
				if (!player.isSpectator() && player.distanceToSqr(him) <= NEAR * NEAR) {
					return true;
				}
			}
		}
		return false;
	}

	private static void clear(MinecraftServer server) {
		RandomSource random = server.overworld().getRandom();
		server.setWeatherParameters(CLEAR_MIN + random.nextInt(CLEAR_SPREAD), 0, false, false);
		ours = false;
		HerobrineMod.LOGGER.info("he has gone — the sky clears");
	}

	/**
	 * Something of his happened near `near`. A short turn in the weather, in one
	 * of several shapes, so it is never the same twice. Only in the overworld;
	 * never over a storm that is already falling on its own.
	 */
	public static void omen(ServerLevel level, BlockPos near) {
		MinecraftServer server = level.getServer();
		if (server == null || level != server.overworld()
			|| !Config.get().enabled || !Config.get().weather || Wrath.removed(server)) {
			return;
		}
		RandomSource random = level.getRandom();
		long now = level.getGameTime();
		boolean wet = level.isRaining();
		int roll = random.nextInt(100);
		if (!wet && roll < 40) {
			int length = 500 + random.nextInt(900);
			server.setWeatherParameters(0, length, true, true);
			book(now, length);
			HerobrineMod.LOGGER.info("omen: thunder for {} s", length / 20);
		} else if (!wet && roll < 65) {
			int length = 600 + random.nextInt(1200);
			server.setWeatherParameters(0, length, true, false);
			book(now, length);
			HerobrineMod.LOGGER.info("omen: rain for {} s", length / 20);
		} else {
			dryThunder(level, near, random);
			HerobrineMod.LOGGER.info("omen: thunder on the horizon");
		}
	}

	/** A storm of ours, for a hunt or the dark: it runs `ticks` and then clears like any other of ours. */
	public static void hold(ServerLevel level, int ticks) {
		MinecraftServer server = level.getServer();
		if (server == null || level != server.overworld() || !Config.get().weather) {
			return;
		}
		if (level.isRaining() && !ours) {
			return;      // vanilla's own storm. Leave it
		}
		server.setWeatherParameters(0, ticks, true, true);
		book(level.getGameTime(), ticks);
	}

	/** The thing that held the sky has ended: let it clear on the usual delay. */
	public static void release(ServerLevel level) {
		if (ours) {
			until = Math.min(until, level.getGameTime() + LINGERS);
		}
	}

	private static void book(long now, int ticks) {
		ours = true;
		until = Math.max(until, now + ticks);
	}

	/** Thunder with no cloud: a bolt on the horizon and the roll of it, heard by whoever is near. */
	private static void dryThunder(ServerLevel level, BlockPos near, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double out = 60.0 + random.nextDouble() * 50.0;
		int x = near.getX() + (int) Math.round(Math.cos(angle) * out);
		int z = near.getZ() + (int) Math.round(Math.sin(angle) * out);
		if (level.hasChunk(x >> 4, z >> 4)) {
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
			if (bolt != null) {
				bolt.setVisualOnly(true);
				bolt.snapTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5, 0.0F, 0.0F);
				level.addFreshEntity(bolt);
			}
		}
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(near, 128.0)) {
				level.playSound(null, player.getX() + Math.cos(angle) * 16.0, player.getY(),
					player.getZ() + Math.sin(angle) * 16.0, SoundEvents.LIGHTNING_BOLT_THUNDER,
					SoundSource.WEATHER, 1.4F, 0.6F + random.nextFloat() * 0.3F);
			}
		}
	}
}
