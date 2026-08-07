package com.bloomlet.herobrine.wrath;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The world's memory of you.
 *
 * Not a difficulty slider. Per LORE.md this is how much you have started to
 * look like the person his brother became — digging, hoarding, going deeper
 * than you need to. Everything he does is a reaction to this number.
 *
 * Stored as a Fabric attachment on the overworld rather than vanilla
 * SavedData: SavedDataType is a record requiring a vanilla DataFixTypes
 * constant, which a mod cannot supply honestly, and passing an unrelated one
 * would hand our data to a datafixer written for something else. Attachments
 * are built for exactly this and take a plain Codec.
 *
 * One number per save, held on the overworld, because a haunting that reset
 * when you stepped into the Nether would not be a haunting.
 */
public final class Wrath {
	private Wrath() {}

	private static final AttachmentType<Integer> TOTAL =
		AttachmentRegistry.createPersistent(HerobrineMod.id("wrath"), Codec.INT);

	/** Per-player share, so one player on a server cannot speak for everyone. */
	private static final AttachmentType<Integer> PLAYER_SHARE =
		AttachmentRegistry.createPersistent(HerobrineMod.id("wrath_share"), Codec.INT);

	public static int get(MinecraftServer server) {
		return server.overworld().getAttachedOrElse(TOTAL, 0);
	}

	public static int getShare(ServerPlayer player) {
		return player.getAttachedOrElse(PLAYER_SHARE, 0);
	}

	public static Phase phase(MinecraftServer server) {
		return Phase.forWrath(get(server));
	}

	/**
	 * @param player may be null for world-level causes with no single culprit
	 */
	public static void add(MinecraftServer server, ServerPlayer player, int amount, Reason reason) {
		if (amount == 0) {
			return;
		}
		Phase before = phase(server);

		int total = Math.max(0, get(server) + amount);
		server.overworld().setAttached(TOTAL, total);
		if (player != null) {
			player.setAttached(PLAYER_SHARE, Math.max(0, getShare(player) + amount));
		}

		Phase after = Phase.forWrath(total);
		if (after != before) {
			HerobrineMod.LOGGER.info("Wrath {} -> phase {} ({})", total, after.name(), reason.name());
		}
	}

	/** Why wrath moved. Logged, and later used to weight what he does about it. */
	public enum Reason {
		TIME,          // baseline drift; the world remembers you being in it
		KILL,          // he notices violence
		DEPTH,         // his territory
		SLEEP,         // you denied him the dark
		DEFIANCE,      // you broke something of his — the biggest single jump
		DEATH          // he is satisfied, briefly
	}
}
