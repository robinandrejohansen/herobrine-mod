package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * THREE MINUTES AFTER, THE END.
 *
 * When he dies the world is told at once — the title, the rain stopping, the
 * old music. Then it is left alone for three minutes, long enough to stand in
 * the quiet and look at what is left. And then every player gets the screen
 * that says it: you have beaten the game, for real; thank you; there is peace
 * now. With a way out, and a way to stay.
 *
 * The moment is stored on the overworld and each player's having seen it on the
 * player, both saved — so somebody who logs off before the three minutes, or a
 * server that restarts, still gets it, once, on their way back in.
 */
public final class Peace {

	private Peace() {}

	/** The one message: show the ending. No content; the client knows the words. */
	public record Shown() implements CustomPacketPayload {
		public static final Shown INSTANCE = new Shown();
		public static final Type<Shown> TYPE = new Type<>(HerobrineMod.id("peace"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Shown> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final int AFTER = 20 * 60 * 3;
	private static final int EVERY = 100;

	private static final AttachmentType<Long> ENDED_AT =
		AttachmentRegistry.createPersistent(HerobrineMod.id("ended_at"), Codec.LONG);
	private static final AttachmentType<Boolean> SAW_IT = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.copyOnDeath()
		.buildAndRegister(HerobrineMod.id("saw_the_end"));

	private static int tickCounter;

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Shown.TYPE, Shown.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Peace::onTick);
	}

	/** He is gone. The clock for the ending starts now, once. */
	public static void begin(MinecraftServer server) {
		if (server.overworld().getAttached(ENDED_AT) == null) {
			server.overworld().setAttached(ENDED_AT, server.overworld().getGameTime());
			HerobrineMod.LOGGER.info("he is gone — the ending in {} s", AFTER / 20);
		}
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % EVERY != 0) {
			return;
		}
		Long ended = server.overworld().getAttached(ENDED_AT);
		if (ended == null || server.overworld().getGameTime() < ended + AFTER) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (Boolean.TRUE.equals(player.getAttached(SAW_IT)) || !ServerPlayNetworking.canSend(player, Shown.TYPE)) {
				continue;
			}
			ServerPlayNetworking.send(player, Shown.INSTANCE);
			player.setAttached(SAW_IT, true);
			HerobrineMod.LOGGER.info("{} was shown the ending", player.getName().getString());
		}
	}
}
