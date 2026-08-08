package com.bloomlet.herobrine;

import com.bloomlet.herobrine.entity.HauntingSpawner;
import com.bloomlet.herobrine.entity.HerobrineEntity;
import com.bloomlet.herobrine.manifest.Manifestation;
import com.bloomlet.herobrine.structure.Dwellings;
import com.bloomlet.herobrine.structure.Homestead;
import com.bloomlet.herobrine.manifest.ManifestationDirector;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug surface for the parts of this mod that are deliberately invisible.
 *
 * Wrath and pacing produce nothing you can see by design, which makes them
 * impossible to playtest and easy to mistake for broken. Every failure path
 * here must say WHY it failed — "nothing happened" wasted a debugging round
 * when the real answer was simply that it was daytime.
 */
public final class HerobrineCommand {
	private HerobrineCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
			dispatcher.register(Commands.literal("herobrine")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

				.then(Commands.literal("status").executes(HerobrineCommand::status))

				.then(Commands.literal("house")
					.executes(HerobrineCommand::house)
					// Raises it at your feet instead of a thousand blocks out.
					// The only sane way to look at a building while working on
					// it; the real one needs a long walk.
					.then(Commands.literal("here").executes(HerobrineCommand::houseHere)))

				.then(Commands.literal("provoke")
					.executes(ctx -> provoke(ctx, false))
					// Ignores the darkness requirement. For checking how he
					// LOOKS without waiting for night.
					.then(Commands.literal("force").executes(ctx -> provoke(ctx, true)))
					// Name one directly, bypassing the pool and suppression.
					.then(Commands.argument("what", StringArgumentType.word())
						.suggests((c, b) -> {
							for (Manifestation m : Manifestation.values()) {
								b.suggest(m.name().toLowerCase(java.util.Locale.ROOT));
							}
							return b.buildFuture();
						})
						.executes(HerobrineCommand::provokeNamed)))

				.then(Commands.literal("speed")
					.then(Commands.argument("multiplier", IntegerArgumentType.integer(1, 60))
						.executes(ctx -> {
							int value = IntegerArgumentType.getInteger(ctx, "multiplier");
							ManifestationDirector.setSpeed(value, ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.literal(
								"pacing x" + value + " — window is now roughly "
									+ (8 * 60 / value) + "-" + (20 * 60 / value) + "s"), false);
							return 1;
						})))

				.then(Commands.literal("wrath")
					.then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
						.executes(HerobrineCommand::wrath)))
			));
	}

	private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		MinecraftServer server = ctx.getSource().getServer();
		ServerLevel level = (ServerLevel)player.level();

		int wrath = Wrath.get(server);
		Phase phase = Wrath.phase(server);
		int remaining = phase.remaining(wrath);
		long seconds = ManifestationDirector.secondsUntilNext(server, player);
		int light = level.getMaxLocalRawBrightness(player.blockPosition());

		String line = "wrath " + wrath + "  |  phase " + phase.name()
			+ (remaining >= 0 ? "  |  next phase in " + remaining : "  |  final phase")
			+ "  |  share " + Wrath.getShare(player)
			+ "  |  next in " + (seconds < 0 ? "unscheduled" : seconds + "s")
			+ "  |  light here " + light + (light > 7 ? " (too bright for him)" : " (dark enough)")
			+ "  |  pages " + com.bloomlet.herobrine.manifest.Journal.pagesFound(level)
				+ "/" + com.bloomlet.herobrine.manifest.JournalPages.maxPageFor(phase)
				+ " readable now"
			+ "  |  his animals killed " + com.bloomlet.herobrine.manifest.Possession.toll(level)
				+ "/" + com.bloomlet.herobrine.manifest.Possession.tollLimit()
			+ (ManifestationDirector.speed() > 1 ? "  |  pacing x" + ManifestationDirector.speed() : "");
		ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		return 1;
	}

	private static int provoke(CommandContext<CommandSourceStack> ctx, boolean force)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		MinecraftServer server = ctx.getSource().getServer();
		ServerLevel level = (ServerLevel)player.level();

		if (force) {
			HauntingSpawner.Outcome outcome = HauntingSpawner.place(level, player, true);
			ctx.getSource().sendSuccess(() -> Component.literal(switch (outcome) {
				case PLACED -> "placed (ignoring darkness)";
				case ALREADY_NEARBY -> "he already exists somewhere in this world — there is only ever one";
				case NO_DARK_SPOT -> "no standable ground behind you at 26-44 blocks";
				case BAD_PLAYER -> "you are spectating or dead";
			}), false);
			return 1;
		}

		Phase phase = Wrath.phase(server);
		if (!ManifestationDirector.anythingEligible(server, player)) {
			ctx.getSource().sendSuccess(() -> Component.literal(
				"nothing eligible at phase " + phase.name()
					+ " — either everything is suppressed from recent use, or wrath is too low"
					+ " (the stare needs " + Phase.WATCHER.threshold + ")"), false);
			return 0;
		}

		Manifestation ran = ManifestationDirector.attempt(server, level, player, true);
		if (ran != null) {
			ctx.getSource().sendSuccess(
				() -> Component.literal("ran " + ran.name() + where(player)), false);
			return 1;
		}

		// It was eligible but the world refused. Say which reason applies.
		int light = level.getMaxLocalRawBrightness(player.blockPosition());
		boolean crowded = !level.getEntities(
			com.bloomlet.herobrine.entity.ModEntities.HEROBRINE, e -> true).isEmpty();
		String why = crowded
			? "he already exists somewhere in this world — there is only ever one"
			: light > 7
				? "too bright — light here is " + light + ", he needs 7 or less"
				: "nowhere valid to place him from here";
		String tried = ManifestationDirector.eligible(server, player).stream()
			.map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("none");
		String blocked = ManifestationDirector.suppressed(player).stream()
			.map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("none");
		ctx.getSource().sendSuccess(() -> Component.literal(
			"nothing — " + why
				+ "  |  tried: " + tried
				+ "  |  suppressed: " + blocked
				+ "  |  name one directly with /herobrine provoke <what>"), false);
		return 0;
	}

	/** Run one named manifestation, ignoring pool, suppression and phase. */
	private static int provokeNamed(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		String requested = StringArgumentType.getString(ctx, "what");

		Manifestation match = null;
		for (Manifestation m : Manifestation.values()) {
			if (m.name().equalsIgnoreCase(requested)) {
				match = m;
				break;
			}
		}
		if (match == null) {
			String all = java.util.Arrays.stream(Manifestation.values())
				.map(m -> m.name().toLowerCase(java.util.Locale.ROOT))
				.reduce((a, b) -> a + ", " + b).orElse("");
			ctx.getSource().sendSuccess(() -> Component.literal(
				"no such manifestation. try: " + all), false);
			return 0;
		}

		Manifestation chosen = match;
		ManifestationDirector.clearRefusal();
		boolean ran = ManifestationDirector.runNamed(chosen, level, player);
		String refusal = ManifestationDirector.lastRefusal();
		ctx.getSource().sendSuccess(() -> Component.literal(
			ran ? "ran " + chosen.name() + where(player)
				: chosen.name() + " could not run here — "
					+ (refusal == null ? "wrong surroundings for it" : refusal)), false);
		return ran ? 1 : 0;
	}

	/**
	 * Where the homestead is, and whether it has been built yet.
	 *
	 * Reports the seeded site even before anything stands there, because the
	 * position is the real thing and the blocks are only what happens when
	 * somebody arrives.
	 */
	private static int house(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		net.minecraft.core.BlockPos origin = Dwellings.origin(level);
		net.minecraft.core.BlockPos site = origin != null ? origin : Dwellings.siteFor(level);
		int distance = (int)Math.sqrt(site.distSqr(player.blockPosition()));
		String line = (origin != null ? "homestead standing at " : "homestead will stand near ")
			+ site.getX() + " " + site.getY() + " " + site.getZ()
			+ "  |  " + distance + " blocks away";
		ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		return 1;
	}

	private static int houseHere(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		net.minecraft.core.BlockPos origin = player.blockPosition()
			.offset(-Homestead.doorX(), 0, 6);
		Homestead.build(level, origin, level.getRandom());
		ctx.getSource().sendSuccess(() -> Component.literal(
			"homestead raised at " + origin.getX() + " " + origin.getY() + " " + origin.getZ()
				+ "  |  the door faces you"), false);
		return 1;
	}

	/**
	 * Where it happened, and how far. Everything he does is placed out of
	 * sight on purpose, which leaves a tester unable to tell "it worked and I
	 * missed it" from "it quietly failed".
	 */
	private static String where(ServerPlayer player) {
		net.minecraft.core.BlockPos pos = ManifestationDirector.lastLocation();
		if (pos == null) {
			return "";
		}
		int distance = (int)Math.sqrt(pos.distSqr(player.blockPosition()));
		return "  |  at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
			+ " (" + distance + " blocks away)";
	}

	private static int wrath(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		MinecraftServer server = ctx.getSource().getServer();
		Wrath.add(server, player, IntegerArgumentType.getInteger(ctx, "amount"), Wrath.Reason.DEFIANCE);
		String line = "wrath " + Wrath.get(server) + "  |  phase " + Wrath.phase(server).name();
		ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		return 1;
	}
}
