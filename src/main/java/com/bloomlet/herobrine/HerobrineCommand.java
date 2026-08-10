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

				// Where everything is. Added after a server walked a thousand
				// blocks west, then fourteen hundred the other way, and found
				// none of it — which is not bad luck, it is arithmetic. Each
				// building is ONE point at one random bearing, and you have to
				// pass within a hundred and twelve blocks of it. Walking in a
				// straight line will essentially never do that.
				.then(Commands.literal("locate").executes(HerobrineCommand::locate))

				// Underground only, and it is over in half a second.
				.then(Commands.literal("chamber").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					boolean done = com.bloomlet.herobrine.manifest.Chambers
						.cut((ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(done
						? "a room was cut" + where(p) + " — dig toward it"
						: "not here — needs solid rock underground"), false);
					return done ? 1 : 0;
				}))

				.then(Commands.literal("passage").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					var outcome = com.bloomlet.herobrine.entity.HauntingSpawner
						.passage((ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(
						outcome == com.bloomlet.herobrine.entity.HauntingSpawner
							.Outcome.PLACED
							? "he is down the tunnel" + where(p)
							: "not here — " + outcome.reason()), false);
					return 1;
				}))

				.then(Commands.literal("stranger").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					boolean ok = com.bloomlet.herobrine.manifest.Mimicry
						.appear((ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(ok
						? "someone else is here" + where(p)
						: "nowhere in sight to stand"), false);
					return 1;
				}))

				.then(Commands.literal("glimpse").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					var outcome = com.bloomlet.herobrine.entity.HauntingSpawner
						.glimpse((ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(
						outcome == com.bloomlet.herobrine.entity.HauntingSpawner
							.Outcome.PLACED
							? "look up" + where(p)
							: "not here — " + outcome.reason()), false);
					return 1;
				}))

				// The five signs from the original story, on demand. Every one
				// of them refuses itself unless the world suits it — no ocean,
				// no pyramid; daylight, no torch — so they need trying in the
				// right place rather than waiting on the director.
				.then(Commands.literal("sign")
					.then(Commands.argument("which", StringArgumentType.word())
						.suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider
							.suggest(new String[] {
								"grove", "pyramid", "tunnel", "torch", "seal" }, b))
						.executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							ServerLevel level = (ServerLevel)p.level();
							String which = StringArgumentType.getString(ctx, "which");
							boolean done = switch (which) {
								case "grove" -> com.bloomlet.herobrine.manifest.Signature
									.grove(level, p);
								case "pyramid" -> com.bloomlet.herobrine.manifest.Signature
									.pyramid(level, p);
								case "tunnel" -> com.bloomlet.herobrine.manifest.Signature
									.tunnel(level, p);
								case "torch" -> com.bloomlet.herobrine.manifest.Signature
									.torch(level, p);
								case "seal" -> com.bloomlet.herobrine.manifest.Signature
									.seal(level, p);
								default -> false;
							};
							ctx.getSource().sendSuccess(() -> Component.literal(done
								? which + " left" + where(p)
								: which + " refused — the world here does not suit it"),
								false);
							return done ? 1 : 0;
						})))

				// For a world that was played before the buildings moved to
				// being sited near the players. Clears where they were going to
				// go so they are chosen again; touches nothing already built.
				.then(Commands.literal("resite").executes(ctx -> {
					ServerLevel level = (ServerLevel)ctx.getSource()
						.getPlayerOrException().level();
					int cleared = Dwellings.forget(level);
					ctx.getSource().sendSuccess(() -> Component.literal(
						cleared + " places forgotten — they will be chosen again near"
							+ " whoever is online, within a couple of seconds."
							+ " Anything already built is left where it is."), false);
					return 1;
				}))

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
					.then(Commands.literal("here").executes(HerobrineCommand::houseHere))
					// Two, three and four at your feet. They sit between the
					// homestead and the threshold in a real world, which is a
					// two-thousand-block walk to look at a doorway.
					.then(Commands.argument("number", IntegerArgumentType.integer(2, 4))
						.executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							int which = IntegerArgumentType.getInteger(ctx, "number");
							ServerLevel level = (ServerLevel)p.level();
							net.minecraft.core.BlockPos at = p.blockPosition();
							switch (which) {
								case 2 -> com.bloomlet.herobrine.structure.SecondHouse
									.build(level, at, level.getRandom());
								case 3 -> com.bloomlet.herobrine.structure.TheDig
									.build(level, at, level.getRandom());
								default -> com.bloomlet.herobrine.structure.Shrine
									.build(level, at, level.getRandom());
							}
							ctx.getSource().sendSuccess(() -> Component.literal(
								"house " + which + " raised here"), false);
							return 1;
						})))

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

				// The tenth-blow church, on demand. Reaching it honestly needs
				// SIEGE and ten connected swings, which is a long way to walk
				// to check whether a doorway is in the right wall.
				// The death aftermath, without the thirty swings in front of it.
				.then(Commands.literal("aftermath").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					com.bloomlet.herobrine.manifest.Reckoning.aftermath(
						(ServerLevel)p.level(), p.blockPosition(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(
						"the portal is up — it fails in six seconds"), false);
					return 1;
				}))

				.then(Commands.literal("warning").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					com.bloomlet.herobrine.manifest.Reckoning.theWarning(
						(ServerLevel)p.level(), p, p);
					ctx.getSource().sendSuccess(() -> Component.literal(
						"the warning goes up — check the log if you cannot see it"), false);
					return 1;
				}))

				.then(Commands.literal("dark").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					boolean fell = com.bloomlet.herobrine.manifest.TheDark.fall(
						(ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(fell
						? "the dark falls"
						: "nothing to take — already night, already storming, no torches"), false);
					return fell ? 1 : 0;
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
		// Nothing has a position before it is sited any more — a place is
		// chosen near the players when their phase reaches it, so before that
		// there is genuinely no answer to give rather than a seeded one.
		net.minecraft.core.BlockPos origin = Dwellings.origin(level);
		String line = origin == null
			? "the homestead has not been sited yet — /herobrine locate"
			: "homestead at " + origin.getX() + " " + origin.getY() + " " + origin.getZ()
				+ "  |  " + (int)Math.sqrt(origin.distSqr(player.blockPosition()))
				+ " blocks " + compass(player, origin);
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
		String line = found == null
			? "the threshold has not been sited yet — it needs HUNTER"
			: "threshold at " + found.getX() + " " + found.getY() + " " + found.getZ()
				+ "  |  " + (int)Math.sqrt(found.distSqr(player.blockPosition()))
				+ " blocks " + compass(player, found);
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
	/**
	 * Every building, how far, and which way.
	 *
	 * Deliberately a command rather than a map item or a compass. Handing the
	 * player a marker turns the whole thing into a quest log; telling whoever
	 * runs the server where the buildings are lets them decide what to do with
	 * it — walk their friends past one, or say nothing.
	 */
	private static int locate(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel)player.level();
		for (String line : Dwellings.report(level)) {
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(
			"§8sited near the players when their phase arrives; built at 192 blocks"),
			false);
		return 1;
	}

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
