package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.Config;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NOBODY IS TOLD WHEN SOMEBODY DIES.
 *
 * The whole mod rests on the player being unable to corroborate anything. The
 * stare spends one shared clock precisely so two people cannot agree on what was
 * in the clearing; the stranger copies whoever is FURTHEST away so it cannot be
 * resolved by looking; the sighting is deliberately over before anybody is sure.
 * And then the death message arrived in chat, in yellow, with the cause spelled
 * out, and settled the argument for everyone on the server.
 *
 * IT SPLITS CLEANLY, WHICH IS THE ONLY REASON THIS IS A NARROW CHANGE. Vanilla's
 * ServerPlayer.die does two separate things: it sends the dying player their own
 * death screen down their own connection, and it broadcasts a system message to
 * everybody else. Only the second one goes. YOU still know exactly what killed
 * you — you have to, or dying stops teaching anything — and nobody else knows you
 * died at all.
 *
 * So the only way the group finds out is somebody saying it out loud. Which is
 * the best thing this change buys: a player has to break the fiction themselves,
 * in their own voice, to tell their friends what happened. There is a silence
 * before that sentence and the silence is the whole point.
 *
 * ALL DEATHS, NOT ONLY HIS, and that is not laziness — it is the entire design.
 * If only Herobrine's kills were silent then silence would MEAN Herobrine, and
 * the mod would have built a perfect notification for the one thing it most
 * wants nobody to be certain about. A drowning has to be as quiet as he is.
 *
 * A redirect on one call in one method. If Mojang moves it the mixin fails at
 * load rather than silently doing nothing, which is the correct failure for
 * something whose absence is invisible.
 */
@Mixin(ServerPlayer.class)
public abstract class QuietDeathsMixin {

	@Redirect(
		method = "die",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;"
				+ "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private void herobrine$tellNobody(PlayerList players, Component message, boolean overlay) {
		if (Config.get().enabled && Config.get().quietDeaths) {
			return;
		}
		players.broadcastSystemMessage(message, overlay);
	}
}
