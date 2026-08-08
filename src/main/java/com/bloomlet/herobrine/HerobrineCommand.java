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

				// DEBUG AID — delete with markSpawns in HauntingSpawner.
				.then(Commands.literal("mark").executes(ctx -> {
					boolean on = HauntingSpawner.toggleMark();
					ctx.getSource().sendSuccess(() -> Component.literal(on
						? "lightning marks every spawn (harmless — no fire, no damage)"
						: "spawn marks off"), false);
					return 1;
				}))

				.then(Commands.literal("town")
					.then(Commands.literal("here").executes(HerobrineCommand::townHere)))

				.then(Commands.literal("threshold")
					.executes(HerobrineCommand::threshold)
					.then(Commands.literal("here").executes(HerobrineCommand::thresholdHere)))

				.then(Commands.literal("house")
					.executes(HerobrineCommand::house)
					// Raises it at your feet instead of a thousand blocks out.
					// The only sane way to look at a building while working on
					// it; the real one needs a long walk.
					.then(Commands.literal("here").executes(HerobrineCommand::houseHere)))

				// Starts a hunt regardless of phase, so it can be tested without
				// grinding to a thousand wrath first.
				.then(Commands.literal("hunt").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					HauntingSpawner.Outcome outcome = HauntingSpawner.place(
						(ServerLevel)p.level(), p, true, true);
					ctx.getSource().sendSuccess(() -> Component.literal(
						outcome == HauntingSpawner.Outcome.PLACED
							? "the hunt is on" + where(p)
							: "could not start it — " + outcome.reason()), false);
					return outcome == HauntingSpawner.Outcome.PLACED ? 1 : 0;
				}))

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
			ctx.getSource().sendSuccess(() -> Component.literal(
				outcome == HauntingSpawner.Outcome.PLACED
					? "placed (ignoring darkness)" + where(player)
					: "could not place him — " + outcome.reason()), false);
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

	private static int townHere(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		net.minecraft.core.BlockPos at = player.blockPosition();
		com.bloomlet.herobrine.town.Township.raise(level, at, level.getRandom());
		ctx.getSource().sendSuccess(() -> Component.literal(
			"township laid out around you at " + at.getX() + " " + at.getY() + " " + at.getZ()
				+ "  |  wall, lanes and square only so far"), false);
		return 1;
	}

	private static int threshold(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		net.minecraft.core.BlockPos found = Dwellings.thresholdOrigin(level);
		net.minecraft.core.BlockPos site = found != null ? found : Dwellings.thresholdSiteFor(level);
		int distance = (int)Math.sqrt(site.distSqr(player.blockPosition()));
		String line = (found != null ? "threshold standing at " : "threshold will stand near ")
			+ site.getX() + " " + site.getY() + " " + site.getZ()
			+ "  |  " + distance + " blocks away";
		ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		return 1;
	}

	private static int thresholdHere(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		net.minecraft.core.BlockPos at = player.blockPosition().offset(0, 0, 6);
		com.bloomlet.herobrine.structure.Threshold.raise(level, at, level.getRandom());
		ctx.getSource().sendSuccess(() -> Component.literal(
			"threshold raised at " + at.getX() + " " + at.getY() + " " + at.getZ()
				+ "  |  the stair goes down and south"), false);
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
			+ " (" + distance + " blocks " + compass(player, pos) + ")";
	}

	/**
	 * Which way to turn.
	 *
	 * Without this, "he was not placed" and "he was placed and you were facing
	 * the wrong hill" produce exactly the same report from a tester — "I cannot
	 * see him" — and there is no way to tell them apart. A bearing makes the
	 * command falsifiable: turn that way, and either he is there or the bug is
	 * real.
	 */
	private static String compass(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		double dx = pos.getX() + 0.5 - player.getX();
		double dz = pos.getZ() + 0.5 - player.getZ();
		// Minecraft: +Z is south, +X is east, and yaw 0 looks down +Z.
		String[] points = { "south", "south-west", "west", "north-west",
			"north", "north-east", "east", "south-east" };
		double angle = Math.toDegrees(Math.atan2(-dx, dz));
		int index = (int)Math.round(((angle % 360) + 360) % 360 / 45.0) % 8;
		return "to the " + points[index];
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
