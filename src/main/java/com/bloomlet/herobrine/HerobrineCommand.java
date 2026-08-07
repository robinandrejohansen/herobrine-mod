package com.bloomlet.herobrine;

import com.bloomlet.herobrine.manifest.Manifestation;
import com.bloomlet.herobrine.manifest.ManifestationDirector;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug surface for the parts of this mod that are deliberately invisible.
 *
 * Wrath and pacing produce nothing you can see by design, which makes them
 * impossible to playtest and easy to get wrong. This is how we look at them.
 */
public final class HerobrineCommand {
	private HerobrineCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
			dispatcher.register(Commands.literal("herobrine")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

				.then(Commands.literal("status").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					MinecraftServer server = ctx.getSource().getServer();
					int wrath = Wrath.get(server);
					Phase phase = Wrath.phase(server);
					int remaining = phase.remaining(wrath);
					long seconds = ManifestationDirector.secondsUntilNext(server, player);

					ctx.getSource().sendSuccess(() -> Component.literal(
						"wrath " + wrath + "  |  phase " + phase.name()
							+ (remaining >= 0 ? "  |  next phase in " + remaining : "  |  final phase")
							+ "  |  your share " + Wrath.getShare(player)
							+ "  |  next manifestation "
							+ (seconds < 0 ? "unscheduled" : seconds + "s")
					), false);
					return 1;
				}))

				.then(Commands.literal("provoke").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					MinecraftServer server = ctx.getSource().getServer();
					ManifestationDirector.provoke(server, player);
					Manifestation ran = ManifestationDirector.attempt(
						server, player.level() instanceof net.minecraft.server.level.ServerLevel sl
							? sl : server.overworld(), player, true);
					ctx.getSource().sendSuccess(() -> Component.literal(
						ran == null
							? "nothing — suppressed, or the world could not take it"
							: "ran " + ran.name()
					), false);
					return 1;
				}))

				.then(Commands.literal("wrath")
					.then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							int amount = IntegerArgumentType.getInteger(ctx, "amount");
							Wrath.add(ctx.getSource().getServer(), player, amount, Wrath.Reason.DEFIANCE);
							int now = Wrath.get(ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.literal(
								"wrath " + now + "  |  phase " + Wrath.phase(ctx.getSource().getServer()).name()
							), false);
							return 1;
						})))
			));
	}
}
