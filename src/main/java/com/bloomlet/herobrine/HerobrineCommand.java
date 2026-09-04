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
				.then(Commands.literal("blueprint")
					.then(Commands.argument("name", StringArgumentType.word())
						.suggests((context, builder) -> {
							for (String have
									: com.bloomlet.herobrine.structure.Blueprint.available()) {
								builder.suggest(have);
							}
							return builder.buildFuture();
						})
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							ServerLevel level = (ServerLevel) player.level();
							String name = StringArgumentType.getString(ctx, "name");
							// From the block in front of the player's feet, so the
							// corner of the blueprint is somewhere they chose.
							net.minecraft.core.BlockPos at = player.blockPosition();
							var done = com.bloomlet.herobrine.structure.Blueprint
								.place(level, at, name);
							if (done == null) {
								ctx.getSource().sendFailure(Component.literal(
									"no blueprint called \"" + name + "\" in "
										+ com.bloomlet.herobrine.structure.Blueprint.folder()));
								return 0;
							}
							int plain = com.bloomlet.herobrine.structure.Blueprint.lastPlain();
							ctx.getSource().sendSuccess(() -> Component.literal(
								done.blocks() + " blocks going up, " + done.sizeX() + "x"
									+ done.sizeY() + "x" + done.sizeZ() + ", corner at your"
									+ " feet." + (plain > 0
										? " " + plain + " palette entries lost their"
											+ " properties — older format."
										: "")
									+ (done.skipped() > 0
										? " " + done.skipped() + " blocks no longer exist."
										: "")),
								false);
							return 1;
						})))
				.then(Commands.literal("boss").executes(ctx -> boss(ctx, false))
					.then(Commands.literal("fresh").executes(ctx -> boss(ctx, true))))
				.then(Commands.literal("castle").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ServerLevel level = (ServerLevel) player.level();
					// FIFTY BLOCKS AHEAD, NOT UNDERFOOT.
					//
					// raise() levels a platform and stacks a curtain eleven courses
					// high over about four thousand square blocks, and its own comment
					// says nobody is standing in it while that happens — in his world
					// it fires at a hundred and forty-four blocks out. Centred on the
					// caller it would bury them inside the motte fill.
					//
					// Ahead of where they are looking, so they watch it come up.
					net.minecraft.world.phys.Vec3 look = player.getLookAngle();
					double away = 50.0;
					int x = (int) Math.round(player.getX() + look.x * away);
					int z = (int) Math.round(player.getZ() + look.z * away);
					// AND THE CHUNKS HAVE TO BE THERE FIRST.
					//
					// The circuit reaches fifty blocks from the site and the site is
					// fifty from the caller, so the far side of the curtain is a
					// hundred blocks out — five chunks past whoever typed this. In
					// single-player at sixteen chunks that is loaded anyway; on a
					// server with a short view distance it is not, and setBlock into
					// nothing builds half a castle without reporting anything.
					for (int cx = (x - 56) >> 4; cx <= (x + 56) >> 4; cx++) {
						for (int cz = (z - 56) >> 4; cz <= (z + 56) >> 4; cz++) {
							level.getChunk(cx, cz);
						}
					}
					net.minecraft.core.BlockPos site = new net.minecraft.core.BlockPos(
						x, com.bloomlet.herobrine.structure.Ground.topOf(level, x, z), z);
					com.bloomlet.herobrine.structure.Keep.raise(level, site);
					ctx.getSource().sendSuccess(() -> Component.literal(
						"a castle is going up at [" + x + ", " + z + "] — fifty blocks"
							+ " ahead of you. it takes about two seconds of ticks."),
						false);
					return 1;
				}))
				.then(Commands.literal("recastle").executes(ctx -> {
					// The castle only, and only in his world — Dwellings.forget is the
					// six overworld houses and has never touched the keep.
					net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
					ServerLevel his = server.getLevel(
						com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD);
					if (his == null) {
						ctx.getSource().sendFailure(Component.literal("no such world"));
						return 0;
					}
					boolean had = com.bloomlet.herobrine.structure.Keep.forget(his);
					ctx.getSource().sendSuccess(() -> Component.literal(had
						? "the castle is forgotten — walk into his world and it will be"
							+ " chosen and built again. the old one stays standing."
						: "there was no castle to forget yet"), false);
					return 1;
				}))
				.then(Commands.literal("resite").executes(ctx -> {
					ServerLevel level = (ServerLevel)ctx.getSource()
						.getPlayerOrException().level();
					int cleared = Dwellings.forget(level);
					ctx.getSource().sendSuccess(() -> Component.literal(
						cleared + " places forgotten — they will be chosen again near"
							+ " whoever is online, within a couple of seconds, and"
							+ " clear of anything already standing. Places that are"
							+ " already built keep the ground they are on."), false);
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
					// Through TheHunt, so what gets tested is the whole event —
					// the cave placement, the ladder, and the house catching it
					// while they are out — rather than only the spawn.
					HauntingSpawner.Outcome outcome =
						com.bloomlet.herobrine.manifest.TheHunt.begin(
							(ServerLevel)p.level(), p);
					ctx.getSource().sendSuccess(() -> Component.literal(
						outcome == HauntingSpawner.Outcome.PLACED
							? "the hunt is on" + where(p)
							: "could not start it — " + outcome.reason()), false);
					return outcome == HauntingSpawner.Outcome.PLACED ? 1 : 0;
				}))

				// One of the villagers, on demand. Meeting him honestly needs a
			// village, a night, and a one-in-six roll, which is a long wait to
			// check whether his eyes came out right.
			.then(Commands.literal("turned").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ServerLevel level = (ServerLevel)p.level();
					com.bloomlet.herobrine.entity.TurnedEntity him =
						com.bloomlet.herobrine.entity.ModEntities.TURNED.create(
							level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
					if (him == null) {
						ctx.getSource().sendFailure(Component.literal("could not make one"));
						return 0;
					}
					// In front of them and close, unlike the real placement —
					// this is for looking at, and the whole point of the spawn
					// rule is that you never get to.
					net.minecraft.world.phys.Vec3 ahead =
						p.position().add(p.getLookAngle().scale(5.0));
					him.snapTo(ahead.x, p.getY(), ahead.z, p.getYRot() + 180.0F, 0.0F);
					level.addFreshEntity(him);
					ctx.getSource().sendSuccess(() -> Component.literal(
						"one of them turned — he only comes for you after dark"), false);
					return 1;
				}))

			// The tall one, on demand — and unlike the others this one genuinely
			// cannot be looked at into existence any other way. Its whole rule is
			// that it does not move while observed, so a debug spawn in front of
			// the player is the ONLY way to watch it do nothing on purpose.
			.then(Commands.literal("watch").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					int stood = com.bloomlet.herobrine.manifest.Watch.raise(
						(ServerLevel)p.level(), p.blockPosition(), 3);
					ctx.getSource().sendSuccess(() -> Component.literal(stood > 0
						? stood + " posted around you — the gaol's watch. they hold this ground"
						: "nowhere level here for them to stand"), false);
					return stood > 0 ? 1 : 0;
				}))

			.then(Commands.literal("gaunt").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ServerLevel level = (ServerLevel)p.level();
					com.bloomlet.herobrine.entity.GauntEntity it =
						com.bloomlet.herobrine.entity.ModEntities.GAUNT.create(
							level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
					if (it == null) {
						ctx.getSource().sendFailure(Component.literal("could not make one"));
						return 0;
					}
					// Further out than the turned one. Five blocks is close enough
					// to see the face and far enough that the freeze is the first
					// thing you notice rather than the second.
					net.minecraft.world.phys.Vec3 ahead =
						p.position().add(p.getLookAngle().scale(14.0));
					it.snapTo(ahead.x, p.getY(), ahead.z, p.getYRot() + 180.0F, 0.0F);
					level.addFreshEntity(it);
					ctx.getSource().sendSuccess(() -> Component.literal(
						"it is in the trees — it only moves when you are not looking"),
						false);
					return 1;
				}))

			// His world, without the forty hours in front of it. Puts a way
			// through at your feet rather than teleporting you, so what gets
			// tested is the portal and the landing and not a debug shortcut.
			.then(Commands.literal("theway").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					com.bloomlet.herobrine.structure.TheWay.open((ServerLevel)p.level(),
						p.blockPosition().relative(p.getDirection(), 3));
					ctx.getSource().sendSuccess(() -> Component.literal(
						"a way is open in front of you"), false);
					return 1;
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
						"the way is open — walk into it"), false);
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

				.then(Commands.literal("phase")
					.then(Commands.argument("name", StringArgumentType.word())
						.suggests((context, builder) -> {
							for (Phase option : Phase.values()) {
								builder.suggest(option.name().toLowerCase(java.util.Locale.ROOT));
							}
							return builder.buildFuture();
						})
						.executes(HerobrineCommand::phase)))
			));
	}

	/**
	 * THE TESTER'S KIT. Full diamond, a shield on the left arm, a diamond sword and
	 * twenty enchanted golden apples — what a player who has done the whole road
	 * would plausibly walk in with, handed over so a test of the fight is a test
	 * of the fight and not of the mining. Nothing on the body is overwritten: an
	 * armour slot already worn keeps what it has, and the piece goes into the bag.
	 */
	private static void armFor(ServerPlayer player) {
		java.util.Map<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.Item> body =
			new java.util.LinkedHashMap<>();
		body.put(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.DIAMOND_HELMET);
		body.put(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
		body.put(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
		body.put(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.Items.DIAMOND_BOOTS);
		body.put(net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.item.Items.SHIELD);
		body.forEach((slot, item) -> {
			net.minecraft.world.item.ItemStack piece = new net.minecraft.world.item.ItemStack(item);
			if (player.getItemBySlot(slot).isEmpty()) {
				player.setItemSlot(slot, piece);
			} else {
				player.getInventory().add(piece);
			}
		});
		player.getInventory().add(com.bloomlet.herobrine.entity.HerobrineEntity.hisSword(player.level(), false));
		player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE, 20));
		player.containerMenu.broadcastChanges();
	}

	/**
	 * /herobrine boss — STRAIGHT TO THE FIGHT.
	 *
	 * The story to SIEGE, the ending undone if a previous test reached it, the
	 * count of blows reset, his city and his keep stood up in his world, and the
	 * player put down seventy blocks from the keep facing it — which is outside
	 * the idle brain's first ring, so the entrance plays: the circling, the walls,
	 * the hall. Whereabouts sees a player in his world with a keep to stand over
	 * and puts him there within a couple of seconds.
	 *
	 * Seventy, not at the gate: a test that skips the approach is not a test of
	 * the approach, and the approach is half of what was just built.
	 */
	private static int boss(CommandContext<CommandSourceStack> ctx, boolean fresh)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
		ServerLevel his = server.getLevel(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD);
		if (his == null) {
			ctx.getSource().sendFailure(Component.literal("his world does not exist"));
			return 0;
		}
		com.bloomlet.herobrine.wrath.Wrath.jumpTo(server, com.bloomlet.herobrine.wrath.Phase.SIEGE);
		com.bloomlet.herobrine.wrath.Wrath.restore(server);
		// A FIGHT IN PROGRESS IS RESUMED, NOT RESET. The first playtest died at act
		// two, ran the command to get back, and met a fresh act one — and read that as
		// him forgetting. He does not forget; the command did. `fresh` wipes it.
		boolean resumes = !fresh && com.bloomlet.herobrine.manifest.Reckoning.bound(his);
		if (!resumes) {
			com.bloomlet.herobrine.manifest.Reckoning.clear(his);
		} else {
			final int blows = com.bloomlet.herobrine.manifest.Reckoning.hits(his);
			ctx.getSource().sendSuccess(() -> Component.literal(
				"the fight resumes — " + blows + " blows in. /herobrine boss fresh starts over."), false);
		}

		// Through the door, as far as the world is concerned: the landing is cut
		// where the portal would have put them, and the city is anchored on it.
		// Only from the overworld. Run again from inside his world, landing() would
		// cut a fresh chamber — and open a portal — wherever they happen to stand,
		// which the first test did seventy blocks from the keep.
		net.minecraft.core.BlockPos landing = player.level() == his
			? player.blockPosition()
			: com.bloomlet.herobrine.structure.TheWay.landing(his, player);
		net.minecraft.core.BlockPos site =
			com.bloomlet.herobrine.structure.Keep.summon(his, landing);

		double angle = his.getRandom().nextDouble() * Math.PI * 2.0;
		int x = site.getX() + (int) Math.round(Math.cos(angle) * 70.0);
		int z = site.getZ() + (int) Math.round(Math.sin(angle) * 70.0);
		his.getChunk(x >> 4, z >> 4);
		int y = com.bloomlet.herobrine.structure.Ground.topOf(his, x, z) + 1;
		float yaw = (float) Math.toDegrees(Math.atan2(site.getZ() - z, site.getX() - x)) - 90.0F;
		ServerLevel from = (ServerLevel) player.level();
		net.minecraft.world.phys.Vec3 there = new net.minecraft.world.phys.Vec3(x + 0.5, y, z + 0.5);
		// Addexio first, from the level the player is still standing in.
		int came = com.bloomlet.herobrine.entity.CompanionEntity.crossWith(player, from, his, there);
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(his,
			there, net.minecraft.world.phys.Vec3.ZERO, yaw, 0.0F,
			net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
		if (came > 0) {
			ctx.getSource().sendSuccess(() -> Component.literal("Addexio came with you."), false);
		}

		ctx.getSource().sendSuccess(() -> Component.literal(
			"SIEGE. his city and his keep are going up — the keep is 70 blocks ahead"
				+ " of you at [" + site.getX() + ", " + site.getZ() + "]. he will be"
				+ " over it in a couple of seconds. walk in."), false);
		armFor(player);
		HerobrineMod.LOGGER.info("boss: {} put down 70 blocks from the keep at [{}, {}]",
			player.getName().getString(), site.getX(), site.getZ());
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		MinecraftServer server = ctx.getSource().getServer();
		ServerLevel level = (ServerLevel)player.level();

		Phase phase = Wrath.phase(server);
		long seconds = ManifestationDirector.secondsUntilNext(server, player);
		int light = level.getMaxLocalRawBrightness(player.blockPosition());

		// PHASE FIRST, and no wrath total, because there is not one any more. The
		// old first field was a number that had been disconnected from the story
		// for several releases and still printed next to it, which is how a
		// playtest ended up reading "phase WATCHER" four times during RUMOUR.
		String line = "phase " + phase.name()
			+ "  |  next place: walk into it (/herobrine locate)"
			+ "  |  heat " + com.bloomlet.herobrine.wrath.Heat.of(player) + "/100"
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
					+ " — everything is suppressed from recent use, or nothing in this"
					+ " phase can run where you are standing"), false);
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

		// IN HIS WORLD IT ANSWERS ABOUT HIS WORLD.
		//
		// Dwellings.report is the overworld trail — six buildings and a threshold —
		// and running this on the far side of the way used to print all of it, none
		// of which is over there. Somebody standing in the dimension asking where
		// he is got a list of farmhouses.
		//
		// The two things worth knowing here are the only two things in the place:
		// where the castle is, and whether he is on top of it. Both are cheap to
		// ask and neither was reachable from any command.
		if (level.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			net.minecraft.core.BlockPos keep =
				com.bloomlet.herobrine.structure.Keep.site(level);
			boolean up = com.bloomlet.herobrine.structure.Keep.raised(level);
			ctx.getSource().sendSuccess(() -> Component.literal(keep == null
				? "the keep has not been sited yet — stay here a couple of seconds"
				: "the keep " + (up ? "stands" : "will stand") + " at "
					+ keep.getX() + " " + keep.getY() + " " + keep.getZ()
					+ toward(player, keep)), false);

			com.bloomlet.herobrine.entity.HerobrineEntity him =
				com.bloomlet.herobrine.entity.HerobrineEntity.oneIn(level);
			ctx.getSource().sendSuccess(() -> Component.literal(him == null
				? "§8he is not loaded — he appears over the keep once it is sited"
				: "he is at " + him.blockPosition().getX() + " "
					+ him.blockPosition().getY() + " " + him.blockPosition().getZ()
					+ toward(player, him.blockPosition())), false);
			if (keep != null && !up) {
				ctx.getSource().sendSuccess(() -> Component.literal(
					"§8walk within 144 blocks of it and it goes up"), false);
			}
			return 1;
		}

		for (String line : Dwellings.report(level)) {
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(
			"§8sited near the players when their phase arrives; built at 192 blocks"),
			false);
		return 1;
	}

	/** Distance and bearing to a known position, for the things that have one. */
	private static String toward(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		int distance = (int)Math.sqrt(pos.distSqr(player.blockPosition()));
		return "  (" + distance + " blocks " + compass(player, pos) + ")";
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

	/**
	 * Jump the story to a chapter by name.
	 *
	 * Replaces `/herobrine wrath <n>`, which had quietly stopped working: wrath
	 * came off the story several releases ago, so setting it to two thousand moved
	 * nothing and reported a phase it had derived rather than the one in play.
	 */
	private static int phase(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		String asked = StringArgumentType.getString(ctx, "name");
		Phase wanted = null;
		for (Phase option : Phase.values()) {
			if (option.name().equalsIgnoreCase(asked)) {
				wanted = option;
			}
		}
		if (wanted == null) {
			ctx.getSource().sendFailure(Component.literal(
				"no chapter called \"" + asked + "\" — try one of "
					+ java.util.Arrays.stream(Phase.values())
						.map(option -> option.name().toLowerCase(java.util.Locale.ROOT))
						.collect(java.util.stream.Collectors.joining(", "))));
			return 0;
		}
		Wrath.jumpTo(server, wanted);
		final Phase moved = wanted;
		ctx.getSource().sendSuccess(() -> Component.literal(
			"the story is at " + moved.name() + " — places no longer wait on the"
				+ " chapter, only on the one before them being found"), false);
		return 1;
	}
}
