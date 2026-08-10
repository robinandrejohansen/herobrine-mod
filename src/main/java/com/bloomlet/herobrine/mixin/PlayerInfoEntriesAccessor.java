package com.bloomlet.herobrine.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

/**
 * Lets us put somebody in the tab list who is not on the server.
 *
 * Both public constructors of the player-info packet take real ServerPlayers
 * and build their entries from them, which is correct for vanilla and useless
 * here: the whole point is a name in the list with nobody behind it. There is no
 * constructor taking arbitrary entries, so this reaches in and replaces the
 * list after the packet is built from an empty collection.
 *
 * A narrow accessor rather than an injection on purpose. Nothing about vanilla's
 * behaviour is being changed — the packet is still assembled, encoded and read
 * by vanilla code in the vanilla order. We are only handing it different rows,
 * which is exactly what the server does to itself every time somebody joins.
 */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoEntriesAccessor {
	@Mutable
	@Accessor("entries")
	void herobrine$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
