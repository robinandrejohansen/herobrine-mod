package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Which forty blocks of the world are yours.
 *
 * THE HUNT NEEDED A HOUSE. Everything he does is placed relative to the player,
 * which was enough while the events were things that happened TO somebody —
 * a torch, a sign, a figure at the treeline. The hunt is the first thing in the
 * mod that happens to a PLACE, and a place has to be identified before it can
 * be wrecked. "Wherever the player is standing" is not a home; it is a home
 * about a third of the time and a mineshaft the rest of it.
 *
 * There is no way to ask Minecraft where somebody lives. The bed is the obvious
 * candidate and it is wrong twice over: plenty of players never sleep, and the
 * ones who do leave beds in every cave they overnighted in. So this measures
 * the only thing that actually correlates — HOW MUCH SOMEBODY HAS BUILT here.
 * A base is a dense patch of crafted blocks and nothing else in Minecraft is,
 * which is the same heuristic {@link DwellTracker} already uses to keep ruins
 * out of somebody's kitchen, pointed the other way round.
 *
 * It records the BEST one rather than the last one, so a night spent walled
 * into a hillside does not become the thing he comes for. And it re-measures
 * whenever the player is home, so a base that gets pulled down and rebuilt
 * elsewhere moves with them instead of leaving him hunting an empty field.
 *
 * Overworld only. A hearth in the Nether is not a home, it is a portal hut.
 */
public final class Hearth {
	private Hearth() {}

	/** Where it is. Packed BlockPos, per player, and it survives a restart. */
	private static final AttachmentType<Long> WHERE =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hearth"), Codec.LONG);

	/**
	 * How much was standing there when it was recorded.
	 *
	 * Kept so the hearth can be BEATEN rather than merely replaced. Without it
	 * every shelter with a door on it takes the title in turn, and the thing he
	 * comes to burn ends up being whichever hole they slept in last.
	 */
	private static final AttachmentType<Integer> WORTH =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hearth_worth"), Codec.INT);

	/** Ten seconds. Nobody builds a house faster than this notices. */
	private static final int CHECK_INTERVAL = 200;

	/** How far out the sample reaches, and how tall. */
	private static final int LOOK = 6;
	private static final int BELOW = 2;
	private static final int ABOVE = 4;

	/**
	 * How many crafted blocks make it a house.
	 *
	 * Twenty-four is about a five-by-five hut with a door, a floor and a
	 * torch — the smallest thing anybody would call theirs. Lower and every
	 * roofed hole in a cliff qualifies; higher and the players who build small
	 * never get a home at all, which would quietly exclude them from the whole
	 * chapter.
	 */
	public static final int ENOUGH = 24;

	/** Inside this and they are home, as far as he is concerned. */
	private static final double AT_HOME = 40.0;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Hearth::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled) {
			return;
		}
		ServerLevel overworld = server.overworld();
		for (ServerPlayer player : overworld.players()) {
			if (player.isSpectator() || !player.isAlive()) {
				continue;
			}
			consider(overworld, player);
		}
	}

	/**
	 * Is this worth more than what they already have?
	 *
	 * Two separate jobs and they share a measurement. If they are standing at
	 * the recorded hearth, the number is simply refreshed — including
	 * DOWNWARD, so a base somebody has dismantled stops being defended by a
	 * score it earned two months ago. Anywhere else it has to beat that number
	 * outright.
	 */
	private static void consider(ServerLevel level, ServerPlayer player) {
		BlockPos here = player.blockPosition();
		Long known = player.getAttached(WHERE);
		int worth = player.getAttachedOrElse(WORTH, 0);

		if (known != null && BlockPos.of(known).closerThan(here, AT_HOME)) {
			int now = built(level, BlockPos.of(known));
			if (now != worth) {
				player.setAttached(WORTH, now);
			}
			if (now < ENOUGH) {
				// Gone. Better to have no hearth than a wrong one — a hunt with
				// no house in it falls back to the ground the player is on,
				// which is the old behaviour and is merely plainer.
				player.setAttached(WHERE, null);
				player.setAttached(WORTH, null);
				HerobrineMod.LOGGER.info("{} has no home there any more",
					player.getName().getString());
			}
			return;
		}

		int count = built(level, here);
		if (count < ENOUGH || count <= worth) {
			return;
		}
		player.setAttached(WHERE, here.asLong());
		player.setAttached(WORTH, count);
		HerobrineMod.LOGGER.info("{} lives at [{}, {}, {}] ({} blocks of it)",
			player.getName().getString(), here.getX(), here.getY(), here.getZ(), count);
	}

	/** How much of what is around this spot did somebody put there. */
	public static int built(ServerLevel level, BlockPos middle) {
		if (!level.isLoaded(middle)) {
			return 0;
		}
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				middle.offset(-LOOK, -BELOW, -LOOK), middle.offset(LOOK, ABOVE, LOOK))) {
			if (DwellTracker.isBuilt(level, pos)) {
				count++;
			}
		}
		return count;
	}

	/** Where they live, or null if they have not built anywhere yet. */
	public static @org.jspecify.annotations.Nullable BlockPos of(ServerPlayer player) {
		Long packed = player.getAttached(WHERE);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * Are they in it?
	 *
	 * True with no hearth at all, deliberately. Somebody who has never built
	 * anything cannot be told their house is on fire, and the hunt should treat
	 * wherever they are standing as the place — which is exactly what it did
	 * before any of this existed.
	 */
	public static boolean home(ServerPlayer player) {
		BlockPos hearth = of(player);
		return hearth == null || hearth.closerThan(player.blockPosition(), AT_HOME);
	}
}
