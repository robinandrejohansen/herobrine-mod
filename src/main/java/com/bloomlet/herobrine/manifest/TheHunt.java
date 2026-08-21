package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HauntingSpawner;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The chapter you have to get through.
 *
 * THE HUNT USED TO BE ONE EVENT IN A POOL, and the church that follows it used
 * to arrive on a clock. Both of those are fixed here by the same change: the
 * hunt is now REQUIRED, and surviving one is what sites the next building. It
 * stops being a thing that might happen to you during HUNTER and becomes the
 * thing HUNTER is.
 *
 * That is worth more than any amount of new behaviour, because it is what makes
 * the behaviour matter. A random event that wrecks your base is a grievance. The
 * same event, when getting through it is the only way the story continues, is a
 * chapter — and the players will describe it afterwards as the night they held
 * the house rather than as the night the mod broke their windows.
 *
 * ---
 *
 * WHAT HE ACTUALLY DOES, IN ORDER. The order is the design and it is not
 * arbitrary — each rung costs more to repair than the one above it, so the
 * hunt escalates in a currency the player can feel without being told.
 *
 *   1  GLASS, some of it and never all. The cheapest thing here to put back and
 *      the loudest to look at: a window gone is the one block in Minecraft that
 *      says somebody got IN. It drops SAND rather than glass, which is the
 *      whole calibration of this event in one line — nothing is lost, and it
 *      still costs a trip to the furnace. An evening, not a week.
 *
 *   2  THE TORCHES OUT. Now it is dark, and they are outside in it. Dropped, so
 *      they go straight back — the same bargain the rest of the mod makes.
 *
 *   3  THE TREELINE, by lightning. This is where the damage stops being theirs
 *      and becomes the land's, which is the only way to make a fire in this mod
 *      frightening rather than ruinous. It is struck WELL AWAY from anybody and
 *      well away from the house, so what they get is a forest going up on the
 *      ridge and not their roof.
 *
 *   4  THE GROUND. Fireballs into the yard, and where they land the ground is
 *      not there any more. Craters are the only thing here that does not heal:
 *      fire goes out and glass goes back, and a hole stays a hole. It is last
 *      for exactly that reason.
 *
 *   +  THUNDER THROUGHOUT, which is the soundtrack rather than a rung.
 *
 * Every one of these refuses itself near anything the player built that is not
 * on the list. He takes the windows and the lights and the field; he does not
 * take the walls, the chests or the roof. See DESIGN.md §9 — this bends the
 * rule in the same direction the break-in already does, and stops in the same
 * place.
 */
public final class TheHunt {
	private TheHunt() {}

	// ---- THE GATE ---------------------------------------------------------
	/**
	 * A hunt has been got through, and the church may be sited.
	 *
	 * On the overworld rather than per-player, because the building is one
	 * building and a server does not want six of them waiting on six people.
	 * One hunt survived by anybody opens the chapter for everybody, which is
	 * the same rule discovery already runs on.
	 */
	private static final AttachmentType<Boolean> SURVIVED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hunt_survived"), Codec.BOOL);

	/** Every blow anybody has landed on him, across every hunt. For status. */
	private static final AttachmentType<Integer> WOUNDS =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hunt_wounds"), Codec.INT);

	public static boolean survived(MinecraftServer server) {
		return Boolean.TRUE.equals(server.overworld().getAttached(SURVIVED));
	}

	public static int wounds(MinecraftServer server) {
		return server.overworld().getAttachedOrElse(WOUNDS, 0);
	}

	/**
	 * IT IS OVER, AND IT COUNTED.
	 *
	 * Called from the one place a hunt can end — the entity discarding itself —
	 * so every way out of it lands here: three blows and he breaks off, a
	 * hundred seconds endured, outrun across a field, or lost down a hole. All
	 * four are surviving it. The mod does not get to have an opinion about
	 * which was the brave one.
	 *
	 * AND BEING KILLED BY IT COUNTS. That is a deliberate answer to a real
	 * question rather than an oversight, and it is worth being explicit about
	 * because the method is called "endured" and dying is not enduring anything.
	 * A hunt that killed somebody unquestionably HAPPENED — and the alternative
	 * is that the chapter refuses to advance for exactly the players who are
	 * having the hardest time with it, which is the worst possible group to
	 * stall. Death already lowers wrath on its own; it should not also cost them
	 * the story.
	 *
	 * The single exception is a hunt nobody ever laid eyes on, which happens
	 * when he is placed and the player logs out in the first seconds. Counting
	 * that would open the next chapter for something that did not happen to
	 * anybody.
	 */
	public static void endured(ServerLevel level, boolean witnessed) {
		MinecraftServer server = level.getServer();
		if (server == null || !witnessed || survived(server)) {
			return;
		}
		// AND IT HAS TO BE A HUNTER HUNT.
		//
		// "He comes home" starts one from MIMIC — so without this the tower's
		// arrival, two chapters early and entirely unavoidable, would satisfy
		// the gate for every player in every world before HUNTER had begun. A
		// gate that an earlier compulsory event always opens is not a gate; it
		// is a line of code that reads like one.
		if (!Wrath.phase(server).atLeast(Phase.HUNTER)) {
			return;
		}
		server.overworld().setAttached(SURVIVED, true);
		HerobrineMod.LOGGER.info("a hunt has been through — the church can be sited");
	}

	// ---- AND THE SKY GOES WITH HIM -----------------------------------------
	/**
	 * THE STORM CAME WITH HIM AND IT SHOULD LEAVE WITH HIM.
	 *
	 * The opening turns the sky, which is most of why a hunt lands — a storm
	 * that arrives BECAUSE of something reads nothing like one that arrived on a
	 * timer. But Skies.turn books nine to eighteen minutes of weather and the
	 * hunt is three, so for the next quarter of an hour the player was standing
	 * in his thunderstorm with nothing in it. The causality that made the
	 * arrival work was undone by the same weather five minutes later.
	 *
	 * So it eases off behind him, and in that order: THE THUNDER STOPS FIRST,
	 * half a minute after he goes, and the rain thins out a minute after that.
	 * Which is what real weather does — the lightning always passes before the
	 * cloud — and it means the player gets a slow all-clear rather than a switch
	 * being thrown. Standing in the rain listening for thunder that has stopped
	 * is a better ninety seconds than either a storm or a clear sky.
	 *
	 * ONLY IF WE STARTED IT. A world that was already under weather when the
	 * hunt began keeps it: clearing somebody's genuine storm because a hunt
	 * happened to end would be the mod reaching further than it was asked to.
	 * The flag is session-scoped on purpose — after a restart the storm simply
	 * runs its natural course, which is the old behaviour and is harmless.
	 */
	private static boolean ours;
	/**
	 * WHICH HUNT'S SKY THIS IS.
	 *
	 * passes() does not clear the weather, it SCHEDULES the clearing — minutes
	 * later, so the last of the storm fades instead of stopping between one step
	 * and the next. Which is right, and which means the callback outlives the hunt
	 * that booked it.
	 *
	 * The playtest caught what that costs: hunt one ended at 11:03:59, hunt two
	 * opened its own storm at 11:05:08, and at 11:05:33 hunt one's callback came
	 * due and put the sky back — twenty-five seconds into somebody else's hunt.
	 * The `ours` flag could not catch it because it had already been handed to the
	 * second storm.
	 *
	 * So the sky is stamped when he takes it, and a scheduled clearing only acts if
	 * the stamp is still the one it was booked under. A later hunt silently voids
	 * an earlier hunt's cleanup, which is exactly the intent — that storm is not
	 * over, it has been replaced.
	 */
	private static int skyOwner;
	private static final int THUNDER_STOPS = 600;
	private static final int RAIN_STOPS = 1800;

	public static void passes(ServerLevel level) {
		MinecraftServer server = level.getServer();
		if (server == null || !ours) {
			return;
		}
		ours = false;
		final int mine = skyOwner;
		Cadence.in(server, THUNDER_STOPS, () -> {
			if (mine == skyOwner && server.overworld().isThundering()) {
				// Rain kept, thunder dropped. The weather is still there; the
				// thing that made it dangerous is not.
				server.setWeatherParameters(0, RAIN_STOPS + 600, true, false);
				HerobrineMod.LOGGER.debug("the thunder passes");
			}
		});
		Cadence.in(server, RAIN_STOPS, () -> {
			if (mine == skyOwner && server.overworld().isRaining()) {
				// Cleared over a long spell rather than switched off, so the
				// last of it fades instead of stopping between one step and the
				// next.
				server.setWeatherParameters(12000, 0, false, false);
				HerobrineMod.LOGGER.info("the sky goes back to what it was");
			}
		});
	}

	public static void wounded(ServerLevel level) {
		MinecraftServer server = level.getServer();
		if (server != null) {
			server.overworld().setAttached(WOUNDS, wounds(server) + 1);
		}
	}

	/**
	 * AND IT HAS TO HAPPEN.
	 *
	 * The director is a lottery, and a lottery is the wrong instrument for
	 * something the story is now waiting on: THE_HUNT is one weight of fourteen
	 * in a pool of forty-odd, so a group could sit in HUNTER for an evening,
	 * never draw it, and find the mod had simply stopped. Gating the church on
	 * a hunt without guaranteeing the hunt would have replaced a pacing problem
	 * with a dead end.
	 *
	 * So once the chapter has had its time and there is still no hunt behind
	 * them, he stops waiting to be drawn. It is tried every ninety seconds
	 * until one lands — not immediately, because a placement can refuse for
	 * perfectly good reasons (he is already out there, they are in a boat, they
	 * are in the roof of a cave with nowhere to stand) and hammering it every
	 * tick would put him in the first bad spot that came up.
	 */
	private static final int DUE_INTERVAL = 1800;
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TheHunt::onTick);
	}

	private static void onTick(MinecraftServer server) {
		// THE GNAWING RUNS ON ITS OWN CLOCK, well inside the owed interval.
		if (++tickCounter % GNAW_INTERVAL == 0 && Config.get().theHunt) {
			for (ServerLevel level : server.getAllLevels()) {
				gnaw(level);
			}
		}
		if (tickCounter % DUE_INTERVAL != 0) {
			return;
		}
		if (!Config.get().enabled || !Config.get().theHunt) {
			return;
		}
		// Anything he said would happen while they were underground, the moment
		// the ground it was going to happen to is loaded again. Every player on
		// the server rather than every player in the overworld — the debt was
		// very likely run up by somebody who then went through a portal.
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			settleUp(server.overworld(), player);
		}
		if (Wrath.phase(server).atLeast(Phase.HUNTER) && !survived(server)
			&& Wrath.settled(server)) {
			ServerLevel overworld = server.overworld();
			if (overworld.players().isEmpty()) {
				return;
			}
			ServerPlayer on = overworld.players().get(
				overworld.getRandom().nextInt(overworld.players().size()));
			HauntingSpawner.Outcome outcome = begin(overworld, on);
			HerobrineMod.LOGGER.info("the hunt is owed — tried {}: {}",
				on.getName().getString(),
				outcome == HauntingSpawner.Outcome.PLACED ? "on" : outcome.reason());
		}
	}

	/**
	 * START ONE, WHEREVER THEY ARE.
	 *
	 * The one entry point, used by the director and by the tick above, because
	 * "he is in a cave" must not be the difference between the chapter
	 * happening and not happening. Outdoors he comes over the field. Underground
	 * he is simply down the passage, and the house gets its evening anyway —
	 * see {@link #awayFromHome}.
	 */
	public static HauntingSpawner.Outcome begin(ServerLevel level, ServerPlayer player) {
		boolean home = Hearth.home(player);
		HauntingSpawner.Outcome outcome = HauntingSpawner.hunt(level, player);
		if (outcome != HauntingSpawner.Outcome.PLACED) {
			return outcome;
		}
		// NOTHING YET. HE IS ONLY OUT THERE.
		//
		// The storm, the bolts and the burning used to fire here, at placement — so
		// a hunt announced itself in the same instant it existed and every one of
		// them opened identically. They belong to {@link #begins}, which runs when
		// the prowl turns into a hunt: either because somebody held his eye, or
		// because the minute ran out. See HerobrineEntity.beginProwl.
		//
		// `home` is deliberately not carried across. It is asked again at the other
		// end, because a minute of prowling is long enough for somebody to have run
		// out of their own front door — and where they are standing when it starts is
		// the answer that matters.
		return outcome;
	}

	/**
	 * THE MOMENT IT ACTUALLY STARTS, and the sky goes with it.
	 *
	 * @param spotted true if they held his eye and brought it on themselves, false
	 *                if the minute simply ran out. He knows the difference and it is
	 *                the only thing he says about it.
	 */
	public static void begins(ServerLevel level, ServerPlayer player, boolean spotted) {
		if (Hearth.home(player)) {
			arrives(level, player);
		} else {
			awayFromHome(level, player);
		}
		String[] pool = spotted ? SEEN_ME : NEVER_LOOKED;
		player.sendSystemMessage(Component.literal(
			"§8§o" + pool[level.getRandom().nextInt(pool.length)]));
		HerobrineMod.LOGGER.info("the hunt begins on {} ({})",
			player.getName().getString(), spotted ? "he was seen" : "never spotted");
	}

	/** They looked. He is not going to pretend that did not happen. */
	private static final String[] SEEN_ME = {
		"there we are",
		"you kept looking",
		"now we both know",
		"you could have walked away from that",
		"say it out loud so it is real",
	};

	/** They never looked up, and he has been there the whole minute. */
	private static final String[] NEVER_LOOKED = {
		"i have been here a while",
		"you never once looked up",
		"i watched you finish what you were doing",
		"i was close enough to touch you twice",
	};

	// ---- HE ARRIVES WITH THE WEATHER ---------------------------------------
	/**
	 * THE SKY GOES FIRST, BEFORE HE HAS TAKEN ANYTHING.
	 *
	 * The ladder used to open on a window going in, six seconds after he was
	 * placed, and that was the wrong first move for a reason worth stating: a
	 * broken pane is a SMALL thing, and small things read as vandalism. The
	 * player's first response to it is annoyance, and once a scare has started
	 * as annoyance it does not recover.
	 *
	 * So the opening is the largest thing available and it costs them nothing.
	 * The storm turns, and the sky comes down on the treeline, on the ridge and
	 * on their own roof — all at once, all in the first four seconds, before he
	 * is close enough to touch anything. What that buys is the right order of
	 * thoughts: something enormous is happening, and THEN it turns out to be
	 * personal. Glass after that is not vandalism, it is the thing getting
	 * closer.
	 *
	 * AND THE BOLTS ON THE HOUSE ARE VISUAL ONLY. This is the whole safety
	 * argument and it is not negotiable. A real bolt on a plank roof burns the
	 * building down, and a mod that burns somebody's base down over an event
	 * they cannot refuse has taken their save rather than frightened them. So
	 * every strike within reach of anything anybody built is a flash and a
	 * crack and nothing else — full spectacle, zero cost — and only the ones out
	 * in the trees, well away from all of it, actually burn.
	 *
	 * The player cannot tell which is which while it is happening. They find out
	 * in the morning, and what they find is that the wood went and the house
	 * did not.
	 */
	private static final int OPENING_BOLTS_MIN = 6;
	private static final int OPENING_BOLTS_SPREAD = 4;
	/** How many of them are allowed to actually burn. Not overdone. */
	private static final int OPENING_FIRES = 3;
	private static final int OPENING_NEAR = 8;
	private static final int OPENING_FAR = 40;

	private static void arrives(ServerLevel level, ServerPlayer player) {
		if (!Config.get().huntWrecks) {
			return;
		}
		Skies.turn(level);
		ours = true;
		skyOwner++;
		RandomSource random = level.getRandom();
		BlockPos from = player.blockPosition();
		int bolts = OPENING_BOLTS_MIN + random.nextInt(OPENING_BOLTS_SPREAD);
		int burning = 0;

		for (int i = 0; i < bolts; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = OPENING_NEAR + random.nextDouble() * (OPENING_FAR - OPENING_NEAR);
			int x = from.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = from.getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			final BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);

			// It burns only if it is in the wild, a long way from everybody, a
			// long way from anything built, and we have not already lit three.
			// Every one of those has to be true; failing any of them makes it a
			// flash, which is the same show and no consequence.
			boolean wild = Config.get().huntFire
				&& burning < OPENING_FIRES
				&& clearOfPeople(level, at)
				&& Hearth.built(level, at) <= 4
				&& (level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.LEAVES)
					|| level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.LOGS));
			if (wild) {
				burning++;
			}
			final boolean real = wild;
			// Staggered across about four seconds. A volley that lands in one
			// tick is one loud noise and one white frame; spread out it is a
			// storm arriving, which is a thing with a direction and a duration.
			Cadence.in(level.getServer(), i * (5 + random.nextInt(12)),
				() -> strike(level, at, real));
		}
		HerobrineMod.LOGGER.info("the hunt opens on {}: {} bolts, {} of them burning",
			player.getName().getString(), bolts, burning);
	}

	// ---- 2.1 NOBODY IS IN --------------------------------------------------
	/**
	 * THEY ARE NOT THERE, SO HE GOES TO WHERE THEY ARE AND TELLS THEM.
	 *
	 * The hunt's whole shape assumes a house with people in it. A group two
	 * hundred blocks down a ravine gets a figure in the tunnel and nothing to
	 * defend, which is the same hunt with the point taken out of it.
	 *
	 * So the house gets its evening regardless, and they are told about it in
	 * five words while they are somewhere they cannot possibly check. That is
	 * the best thing in this whole event and it is not the cruelty — it is that
	 * it is TRUE. There is a real fire, at a real place, and the only thing
	 * they can do about it is a two-minute climb.
	 *
	 * AND IT IS NEVER A BLUFF. If the chunk is loaded, it happens now. If it is
	 * not — which is most of the time, because nobody is standing in it — it is
	 * written down and happens the moment somebody comes back within range. He
	 * does not say a thing that is not so, and a mod that lied here would be
	 * caught within a week and never believed again.
	 */
	private static final AttachmentType<Long> OWED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hunt_owed"), Codec.LONG);

	private static void awayFromHome(ServerLevel level, ServerPlayer player) {
		BlockPos hearth = Hearth.of(player);
		if (hearth == null) {
			return;
		}
		player.sendSystemMessage(Component.literal("§8your house is on fire"));
		Skies.turn(level);
		thunder(level, player.blockPosition(), 2);

		if (level.isLoaded(hearth)) {
			burnTheHouse(level, hearth);
			return;
		}
		// PERSISTENT, not a map in memory. A debt he forgets over a restart is
		// a sentence he said that turned out not to be true, and there is no
		// version of this mod that survives being caught doing that once.
		player.setAttached(OWED, hearth.asLong());
		HerobrineMod.LOGGER.info("{}'s house is owed a fire at [{}, {}]",
			player.getName().getString(), hearth.getX(), hearth.getZ());
	}

	/** Pay what was promised, the moment the ground is loaded again. */
	public static void settleUp(ServerLevel level, ServerPlayer player) {
		Long where = player.getAttached(OWED);
		if (where == null) {
			return;
		}
		BlockPos hearth = BlockPos.of(where);
		if (!level.isLoaded(hearth)) {
			return;
		}
		player.setAttached(OWED, null);
		burnTheHouse(level, hearth);
	}

	/**
	 * The unattended version, and it is the whole ladder at once.
	 *
	 * Nobody is watching, so there is no rhythm to keep — the point of this one
	 * is what it looks like when they walk back up the path, not what it looks
	 * like while it happens. Windows out, lights out, the treeline going, and
	 * two holes in the yard.
	 */
	private static void burnTheHouse(ServerLevel level, BlockPos hearth) {
		glass(level, hearth);
		torches(level, hearth);
		// AND SOMETHING IS ACTUALLY BURNING WHEN THEY GET BACK.
		//
		// The treeline is the version worth having and it can refuse — a house
		// on open grassland has no wood within range to strike. It is not
		// allowed to refuse into nothing, because he has already told them
		// their house is on fire, so a ring of embers goes down instead. Same
		// safeguards, less spectacle, and the sentence stays true.
		if (!treeline(level, hearth, hearth)) {
			embers(level, hearth);
		}
		thunder(level, hearth, 3);
		HerobrineMod.LOGGER.info("he was at the house while they were out, at [{}, {}]",
			hearth.getX(), hearth.getZ());
	}

	/**
	 * Fire on the ground outside, and nowhere it can go.
	 *
	 * Placed twelve to twenty blocks out, only on ground that cannot catch and
	 * with nothing flammable within two blocks of it — the same three rules the
	 * trespasser scorch has always used, which is why fire has never once cost
	 * anybody a build in this mod. Left burning far longer than a scorch,
	 * because this one has to still be there when somebody climbs out of a mine
	 * and walks home, and a fire that went out while they were on the ladder is
	 * a fire that never happened.
	 */
	private static void embers(ServerLevel level, BlockPos house) {
		RandomSource random = level.getRandom();
		int lit = 0;
		for (int attempt = 0; attempt < 60 && lit < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 12.0 + random.nextDouble() * 8.0;
			int x = house.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = house.getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			BlockPos ground = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
			if (!level.getBlockState(ground).isSolid()
				|| !level.getBlockState(ground.above()).isAir()
				|| !safeToBurn(level, ground)) {
				continue;
			}
			final BlockPos flame = ground.above();
			level.setBlock(flame, Blocks.FIRE.defaultBlockState(), 2);
			Cadence.in(level.getServer(), 2400, () -> {
				if (level.getBlockState(flame).is(Blocks.FIRE)) {
					level.setBlock(flame, Blocks.AIR.defaultBlockState(), 2);
				}
			});
			lit++;
		}
		HerobrineMod.LOGGER.info("hunt: {} fires outside the house at [{}, {}]",
			lit, house.getX(), house.getZ());
	}

	/** Nothing within reach may be able to catch, including the floor itself. */
	private static boolean safeToBurn(ServerLevel level, BlockPos ground) {
		for (BlockPos near : BlockPos.betweenClosed(ground.offset(-2, -1, -2),
				ground.offset(2, 3, 2))) {
			if (level.getBlockState(near).ignitedByLava()) {
				return false;
			}
		}
		return true;
	}

	// ---- THE LADDER --------------------------------------------------------
	/**
	 * One rung, and it climbs until something actually happens.
	 *
	 * "Glass first, if there is any" is the requirement, and the loop is how it
	 * is met: a base with no windows in it does not spend the first thirty
	 * seconds of the hunt on a rung that has nothing to do. It falls through to
	 * the torches, and if those are all lanterns it falls through to the trees.
	 * The order is preserved and the pacing is not held hostage to it.
	 *
	 * @param step which rung to try first
	 * @return the rung to try NEXT, which is one past whichever one landed
	 */
	public static int wreck(ServerLevel level, ServerPlayer quarry,
	                        net.minecraft.world.entity.LivingEntity him, int step) {
		if (!Config.get().huntWrecks) {
			return step + 1;
		}
		// The house if they have one and are in it, and the ground under their
		// feet if they do not. The fallback is not a degraded version — it is
		// what the hunt did before any of this existed, and out in a field with
		// no glass and no torches it simply starts at the treeline.
		BlockPos hearth = Hearth.of(quarry);
		BlockPos house = hearth != null && Hearth.home(quarry)
			? hearth : quarry.blockPosition();

		for (int rung = Math.max(0, step); rung < step + 4; rung++) {
			boolean did = switch (rung % 4) {
				case 0 -> glass(level, house);
				case 1 -> torches(level, house);
				case 2 -> treeline(level, house, quarry.blockPosition());
				default -> theGround(level, quarry, him);
			};
			if (did) {
				thunder(level, quarry.blockPosition(), 1);
				return rung + 1;
			}
		}
		// Nothing on the whole ladder had anything to do — a bare hillside with
		// no glass, no lights, no wood and nowhere to put a hole. Thunder is
		// what covers it, because the one thing that must never happen is a beat
		// of the hunt in which nothing at all reaches the player.
		thunder(level, quarry.blockPosition(), 2);
		return step + 1;
	}

	// ---- 1. THE GLASS ------------------------------------------------------
	/**
	 * Some of it. Never all of it, and never the same amount twice.
	 *
	 * Taking every pane would be a demolition and the player would rebuild it
	 * as one job. Taking four out of eleven is worse, because the wall is still
	 * a wall and it has holes in it, and every time they look up from what they
	 * are doing there is somewhere new to see out of.
	 *
	 * IT DROPS SAND. Glass does not survive being broken in vanilla and handing
	 * the pane straight back would make this free; deleting it outright would
	 * make it the first thing in the mod that costs somebody a resource. Sand
	 * is the honest middle and it is the whole tuning of this event: what they
	 * lost is an evening at a furnace, which is a real cost and not a wound.
	 */
	private static final int PANES_MOST = 7;

	private static boolean glass(ServerLevel level, BlockPos house) {
		if (!level.isLoaded(house)) {
			return false;
		}
		java.util.List<BlockPos> panes = new java.util.ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(
				house.offset(-10, -5, -10), house.offset(10, 8, 10))) {
			if (isGlass(level.getBlockState(pos))) {
				panes.add(pos.immutable());
			}
		}
		if (panes.isEmpty()) {
			return false;
		}
		RandomSource random = level.getRandom();
		java.util.Collections.shuffle(panes, new java.util.Random(random.nextLong()));
		// A QUARTER OF WHAT IS LEFT, and never more than seven at once. The
		// fraction is what keeps "some, not all" true across a whole hunt
		// rather than only within one rung: the pool shrinks every time this
		// comes round, so twelve panes lose three, then two, then one — about
		// half of them by the end, and there is always a window still in.
		//
		// Taking a fixed number instead would have emptied a small house on the
		// second visit, which is a demolition, and the player rebuilds a
		// demolition as one job. A wall that is still a wall with holes in it is
		// worse every time they look up from what they are doing.
		int take = Math.max(1, Math.min(PANES_MOST, panes.size() / 4));

		for (int i = 0; i < take; i++) {
			BlockPos at = panes.get(i);
			boolean pane = !level.getBlockState(at).is(net.minecraft.tags.BlockTags.IMPERMEABLE);
			level.destroyBlock(at, false);
			// Half a pane's worth of sand for a pane, a whole one for a block,
			// so putting it back is the same arithmetic it was to build.
			if (!pane || i % 2 == 0) {
				level.addFreshEntity(new ItemEntity(level,
					at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5,
					new ItemStack(Items.SAND)));
			}
			level.playSound(null, at, SoundEvents.GLASS_BREAK,
				SoundSource.HOSTILE, 1.6F, 0.8F + random.nextFloat() * 0.3F);
		}
		HerobrineMod.LOGGER.info("hunt: {} of {} panes out at [{}, {}]",
			take, panes.size(), house.getX(), house.getZ());
		return true;
	}

	/**
	 * Glass, and nothing that merely looks like it.
	 *
	 * IMPERMEABLE is the vanilla tag for the solid blocks — clear, stained and
	 * tinted — and the panes have to be named separately because vanilla builds
	 * them on the same class as iron bars, and a hunt that quietly removed
	 * somebody's bars would be taking a wall rather than a window.
	 */
	private static boolean isGlass(BlockState state) {
		return state.is(net.minecraft.tags.BlockTags.IMPERMEABLE)
			|| state.is(Blocks.GLASS_PANE)
			|| state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
	}

	// ---- 2. THE LIGHTS -----------------------------------------------------
	/**
	 * And now it is dark, and they are outside in it.
	 *
	 * The same effect the mod has used since RUMOUR, at the scale the hunt
	 * needs: every torch in the house rather than three near the player. Torches
	 * and lanterns both, because a base lit entirely with lanterns should not be
	 * exempt from the one rung that changes what they can see.
	 *
	 * Dropped, always. This is the most REVERSIBLE thing on the ladder and it is
	 * deliberately second: the player's first thought must be that they can put
	 * this right, so that rung three landing in the treeline is the moment they
	 * find out they cannot put all of it right.
	 */
	private static boolean torches(ServerLevel level, BlockPos house) {
		if (!level.isLoaded(house)) {
			return false;
		}
		int out = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				house.offset(-10, -6, -10), house.offset(10, 8, 10))) {
			BlockState state = level.getBlockState(pos);
			net.minecraft.world.item.Item dropped;
			if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
				dropped = Items.TORCH;
			} else if (state.is(Blocks.LANTERN)) {
				dropped = Items.LANTERN;
			} else if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
				dropped = Items.SOUL_TORCH;
			} else {
				continue;
			}
			BlockPos at = pos.immutable();
			level.removeBlock(at, false);
			level.addFreshEntity(new ItemEntity(level,
				at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, new ItemStack(dropped)));
			out++;
		}
		if (out == 0) {
			return false;
		}
		level.playSound(null, house, SoundEvents.FIRE_EXTINGUISH,
			SoundSource.HOSTILE, 2.2F, 0.5F);
		HerobrineMod.LOGGER.info("hunt: {} lights out at [{}, {}]",
			out, house.getX(), house.getZ());
		return true;
	}

	// ---- 3. THE TREELINE ---------------------------------------------------
	/**
	 * The wood goes up, and it goes up somewhere else.
	 *
	 * A REAL bolt, which every other piece of lightning in this mod refuses to
	 * be — see Skies, and the arsenal, both of which are visual-only precisely
	 * because a fire in the wrong place costs somebody their world rather than
	 * their evening. This one is allowed to burn because of where it is put and
	 * for no other reason.
	 *
	 * THE TWO DISTANCES ARE THE ENTIRE SAFETY ARGUMENT and neither is
	 * negotiable. It must be at least fourteen blocks from every living player,
	 * so nobody is standing in it. And it must be at least twenty-four from the
	 * house, so that what they are watching is a hillside and not their roof —
	 * fire spreads about a block a minute in still conditions, and twenty-four
	 * blocks of gap is longer than the hunt lasts.
	 *
	 * What that buys is the only genuinely large-scale thing in the mod: they
	 * come out of the door and half the ridge is orange, and none of it is
	 * theirs. The scale is the point. Ownership of the damage is what makes it
	 * survivable.
	 */
	private static final int TREE_NEAR = 14;
	private static final int TREE_FAR = 44;
	private static final int CLEAR_OF_HOUSE = 24;

	private static boolean treeline(ServerLevel level, BlockPos house, BlockPos from) {
		if (!Config.get().huntFire) {
			return false;
		}
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = TREE_NEAR + random.nextDouble() * (TREE_FAR - TREE_NEAR);
			int x = from.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = from.getZ() + (int)Math.round(Math.sin(angle) * range);
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			int y = level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			BlockPos top = new BlockPos(x, y - 1, z);
			// It has to actually be a tree. Striking bare grass gives a flash
			// and a scorch mark, which is weather; striking a canopy gives a
			// wood on fire, which is the event.
			if (!level.getBlockState(top).is(net.minecraft.tags.BlockTags.LEAVES)
				&& !level.getBlockState(top).is(net.minecraft.tags.BlockTags.LOGS)) {
				continue;
			}
			if (house.closerThan(top, CLEAR_OF_HOUSE)) {
				continue;
			}
			if (!clearOfPeople(level, top)) {
				continue;
			}
			// And nothing anybody built anywhere near it, which catches the
			// outlying shed, the bridge and the mine entrance that the hearth
			// on its own would not.
			if (Hearth.built(level, top) > 4) {
				continue;
			}
			strike(level, new BlockPos(x, y, z), true);
			HerobrineMod.LOGGER.info("hunt: the treeline at [{}, {}], {} blocks out",
				x, z, (int)Math.sqrt(from.distSqr(top)));
			return true;
		}
		return false;
	}

	private static boolean clearOfPeople(ServerLevel level, BlockPos at) {
		return clearOfPeopleBy(level, at, TREE_NEAR);
	}

	// ---- 4. THE GROUND -----------------------------------------------------
	/**
	 * Fireballs into the yard, and holes where they land.
	 *
	 * The last rung because it is the only one that does not heal. Fire goes
	 * out, glass goes back, torches were dropped at their feet — a crater is
	 * still a crater next week, and it is the thing they will point at when they
	 * tell somebody about this.
	 *
	 * IT IS THROWN RATHER THAN SIMPLY HAPPENING. The fireball leaves his hand,
	 * crosses the yard, and the hole appears when it arrives, so the player is
	 * given a second and a half in which they can see where it is going. That
	 * matters more than the damage does: something you watched land is something
	 * that happened in the world, and something that simply appears is a mod
	 * doing a thing to you.
	 *
	 * NOTHING BUILT IS EVER TAKEN. The dish only eats ground — dirt, grass,
	 * stone, sand — and it refuses outright if there is anything crafted within
	 * four blocks. Which does mean a base paved wall to wall gets no craters at
	 * all, and that is the correct failure: he would rather do nothing than take
	 * somebody's floor.
	 */
	private static final int CRATER_NEAR = 5;
	private static final int CRATER_FAR = 13;

	private static boolean theGround(ServerLevel level, ServerPlayer quarry,
	                                 net.minecraft.world.entity.LivingEntity him) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 30; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = CRATER_NEAR + random.nextDouble() * (CRATER_FAR - CRATER_NEAR);
			int x = quarry.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = quarry.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			int y = level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			final BlockPos target = new BlockPos(x, y - 1, z);
			// AND IT HAS TO BE THE GROUND THEY ARE ON. The crater is sited from
			// the surface heightmap, so for a player eighty blocks down a mine
			// it would open a hole in a hillside they will never see, take a
			// rung of the ladder to do it, and reach nobody. Underground the
			// hunt has other things to be doing.
			if (Math.abs(y - quarry.blockPosition().getY()) > 8) {
				continue;
			}
			if (!level.getFluidState(target).isEmpty() || !diggable(level, target)) {
				continue;
			}
			if (anythingBuiltNear(level, target)) {
				continue;
			}
			// Never under their feet. The hole is a thing they watch arrive, not
			// a thing that opens beneath them — the mod does not kill anybody by
			// deleting the block they are standing on.
			if (quarry.blockPosition().closerThan(target, 3.5)) {
				continue;
			}

			Vec3 from = him.getEyePosition();
			Vec3 to = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
			Vec3 along = to.subtract(from);

			net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball ball =
				new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(
					level, him, along.normalize());
			ball.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
			ball.shoot(along.x, along.y, along.z, 1.2F, 1.0F);
			level.addFreshEntity(ball);
			level.playSound(null, him.blockPosition(), SoundEvents.BLAZE_SHOOT,
				SoundSource.HOSTILE, 2.2F, 0.5F);

			int flight = (int)(along.length() / 1.1) + 2;
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), flight,
				() -> crater(level, target));
			HerobrineMod.LOGGER.info("hunt: something is coming down at [{}, {}]", x, z);
			return true;
		}
		return false;
	}

	/** A shallow dish, two across and two deep, and the rim is left ragged. */
	private static void crater(ServerLevel level, BlockPos middle) {
		if (!level.isLoaded(middle)) {
			return;
		}
		RandomSource random = level.getRandom();
		for (BlockPos pos : BlockPos.betweenClosed(
				middle.offset(-2, -2, -2), middle.offset(2, 1, 2))) {
			double away = Math.sqrt(pos.distSqr(middle));
			if (away > 2.2 || random.nextInt(6) == 0) {
				continue;      // ragged, so it does not read as a stamped shape
			}
			if (!diggable(level, pos)) {
				continue;
			}
			// No drops. This is the ground rather than anybody's property, and a
			// crater that hands back forty dirt is a delivery, not a wound.
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		// The floor of it burnt, which is what says a thing landed here rather
		// than somebody having dug.
		for (BlockPos pos : BlockPos.betweenClosed(
				middle.offset(-2, -3, -2), middle.offset(2, -2, 2))) {
			if (level.getBlockState(pos).isSolid() && random.nextInt(3) == 0) {
				level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
			}
		}
		level.playSound(null, middle, SoundEvents.GENERIC_EXPLODE.value(),
			SoundSource.HOSTILE, 3.0F, 0.6F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			middle.getX() + 0.5, middle.getY() + 0.5, middle.getZ() + 0.5, 40, 1.4, 0.8, 1.4, 0.05);
	}

	/** Ground, and only ground. Anything crafted is somebody's and is refused. */
	private static boolean diggable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
			return false;
		}
		if (DwellTracker.isBuilt(level, pos) || level.getBlockEntity(pos) != null) {
			return false;
		}
		return state.is(net.minecraft.tags.BlockTags.DIRT)
			|| state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
			|| state.is(net.minecraft.tags.BlockTags.SAND)
			|| state.is(Blocks.GRAVEL)
			|| state.is(net.minecraft.tags.BlockTags.TERRACOTTA)
			|| state.is(net.minecraft.tags.BlockTags.SNOW)
			|| state.is(net.minecraft.tags.BlockTags.REPLACEABLE);
	}

	public static boolean anythingBuiltNear(ServerLevel level, BlockPos middle) {
		for (BlockPos pos : BlockPos.betweenClosed(
				middle.offset(-4, -2, -4), middle.offset(4, 3, 4))) {
			if (DwellTracker.isBuilt(level, pos)) {
				return true;
			}
		}
		return false;
	}

	// ---- WHAT HE SENDS --------------------------------------------------
	/**
	 * HE DOES NOT COME FOR YOU. HE SENDS SOMETHING AND WATCHES.
	 *
	 * The best thing in the hunt and the thing it was missing. A figure that
	 * walks over and hits you is a mob with a melee attack; a figure that stands
	 * at thirty blocks, perfectly still, while things rise out of the ground
	 * between you and him, is somebody who has decided about you and is not in a
	 * hurry. The distance is the menace. He never joins in.
	 *
	 * SMALL, FAST, AND ARMED. Three-quarter scale, which is the size that reads
	 * as wrong rather than as a baby zombie — and a stone axe each, because an
	 * empty-handed zombie is scenery and a full iron one is a boss fight. What
	 * they are for is PRESSURE: ten things converging while the ladder takes the
	 * house apart behind you and he stands there watching it happen.
	 *
	 * THEY ARE HIS, AND THEY GO WHEN HE DOES. Marked on spawn, and burned off
	 * the moment the hunt ends — which is both the honest reading (they were
	 * never really there) and the only way this is allowed to exist at all. Ten
	 * armed zombies left standing in somebody's base after the event is over is
	 * not a scare, it is a mess somebody has to clean up.
	 *
	 * They drop nothing, they do not burn in daylight, and they cannot pick
	 * anything up. Nothing about them is farmable.
	 */
	private static final AttachmentType<Boolean> SENT =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hunt_sent"), Codec.BOOL);

	private static final int SENDS_MIN = 7;
	private static final int SENDS_SPREAD = 4;
	/** And three more on every return. */
	private static final int SENDS_PER_ROUND = 3;
	/** Between you and him, mostly. Near enough to be a problem at once. */
	private static final int SENT_NEAR = 7;
	private static final int SENT_FAR = 16;
	/** However long the hunt runs, never more than this alive at once. */
	private static final int SENT_CAP = 16;

	/**
	 * HOW MANY OF HIS ARE STILL ON THEIR FEET.
	 *
	 * The thing the pause was missing. It used to run on a clock — twenty to
	 * thirty seconds and he walked back in, whether the player had killed all ten
	 * of them or run in a circle and ignored every one. So the wave was scenery:
	 * fighting it changed nothing and neither did not fighting it.
	 *
	 * With a count, the pause becomes a PHASE somebody clears. Kill them and he
	 * comes back early, which is the deal being offered; leave them alive and he
	 * stands out there for as long as his patience lasts, and they keep coming at
	 * you the whole time.
	 *
	 * Counted around the PLAYER rather than around him, because they are the
	 * player's problem and he is thirty blocks away by construction.
	 */
	/** Is this one of his? Asked before anything is done to a mob on his behalf. */
	public static boolean isHis(net.minecraft.world.entity.Entity what) {
		return Boolean.TRUE.equals(what.getAttached(SENT));
	}

	public static int stillStanding(ServerLevel level, ServerPlayer quarry) {
		// Mob rather than Zombie, because the waves are not all zombies any more —
		// wave one is a crowd of the turned. Filtering on the attachment rather than
		// the class is what makes a new wave type free: mark it SENT and every count,
		// every dismissal and the whole gate below already understand it.
		return level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
			quarry.getBoundingBox().inflate(64.0),
			m -> m.isAlive() && Boolean.TRUE.equals(m.getAttached(SENT))).size();
	}

	/**
	 * @param wave which of the three this is, one upward. THE CROWD GROWS WITH IT —
	 *             three more each time — because a boss that sends the same ten
	 *             every phase is a boss whose second phase is easier than its
	 *             first, the player now having the measure of it.
	 *
	 *             AND IT CHANGES WHAT ARRIVES. One is the small ones with axes,
	 *             which teaches the player what a wave is. Two is a crowd of
	 *             villagers — the frightening one precisely because it is not a
	 *             monster: the most familiar friendly silhouette in the game,
	 *             a dozen of it, walking. Three is not decided.
	 */
	public static void send(ServerLevel level, net.minecraft.world.entity.LivingEntity him,
	                        ServerPlayer quarry, int wave) {
		if (!Config.get().huntWrecks) {
			return;
		}
		int alive = stillStanding(level, quarry);
		if (alive >= SENT_CAP) {
			return;
		}
		RandomSource random = level.getRandom();
		// Biased toward his side, so they come from between the two of you and
		// the player's instinct is to back away from him — which is the one
		// direction the rest of them are also arriving from.
		double toward = Math.atan2(him.getZ() - quarry.getZ(), him.getX() - quarry.getX());
		int wanted = Math.min(SENDS_MIN + Math.max(0, wave - 1) * SENDS_PER_ROUND
			+ random.nextInt(SENDS_SPREAD), SENT_CAP - alive);
		int made = 0;

		for (int attempt = 0; attempt < wanted * 6 && made < wanted; attempt++) {
			double angle = toward + (random.nextDouble() - 0.5) * 2.6;
			double range = SENT_NEAR + random.nextDouble() * (SENT_FAR - SENT_NEAR);
			int x = quarry.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = quarry.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
			if (!com.bloomlet.herobrine.entity.ConfinedPlacement.canStand(level, at)) {
				continue;
			}
			// WAVE ONE IS THE SMALL ONES, WAVE TWO IS THE CROWD.
			//
			// Swapped after seeing them in that order. The turned are the better
			// scare and that is exactly why they should not open: the first wave
			// arrives while the player is still working out that a wave is a thing
			// that happens, and spending the villagers there wastes them on somebody
			// who is busy reading the rules. Small things with axes teach the rule.
			// Then the rule is broken by a crowd of people.
			boolean up = wave >= 2
				? raiseTurned(level, at, quarry)
				: raise(level, at, quarry, random);
			if (up) {
				made++;
			}
		}
		if (made > 0) {
			level.playSound(null, quarry.blockPosition(),
				com.bloomlet.herobrine.sound.ModSounds.BREATH,
				SoundSource.HOSTILE, 2.2F, 0.7F);
			HerobrineMod.LOGGER.info("hunt: wave {} — {} {} sent for {}",
				wave, made, wave >= 2 ? "of the turned" : "small ones",
				quarry.getName().getString());
		}
	}

	/**
	 * AND ONE AT THE PERSON.
	 *
	 * SmallFireball rather than the ghast's LargeFireball, and that is the "not so
	 * much" — five points and it sets them alight, against a ghast's six plus an
	 * explosion. It is also the safe choice for a reason worth writing down: a
	 * large fireball explodes, and an exploding projectile aimed at a player
	 * standing in their own base is a hole in their floor. This mod has already
	 * burned a whole dimension down once by being casual about fire that spreads.
	 *
	 * LED, not aimed at where they are. Five ticks of their own velocity added to
	 * the target, so a player sprinting in a straight line gets hit and a player
	 * changing direction does not. That is the difference between a projectile
	 * that is dodgeable and a projectile that is decorative — and the old one was
	 * neither, since it was never pointed at anybody.
	 */
	private static void atThem(ServerLevel level, net.minecraft.world.entity.LivingEntity him,
	                           ServerPlayer quarry) {
		Vec3 from = him.getEyePosition();
		Vec3 lead = quarry.getDeltaMovement().scale(LEAD_TICKS);
		Vec3 along = quarry.getEyePosition().add(lead.x, 0.0, lead.z).subtract(from);
		if (along.lengthSqr() < 1.0E-4) {
			return;
		}
		net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball ball =
			new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(
				level, him, along.normalize());
		ball.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		ball.shoot(along.x, along.y, along.z, BALL_SPEED, AIM_AT_THEM);
		level.addFreshEntity(ball);
		level.playSound(null, him.blockPosition(), SoundEvents.BLAZE_SHOOT,
			SoundSource.HOSTILE, 2.4F, 0.45F);
		HerobrineMod.LOGGER.debug("hunt: one at {}, {} blocks",
			quarry.getName().getString(), (int)along.length());
	}

	/**
	 * WAVE ONE: A CROWD OF THE TURNED, AND NOT ONE OF THEM IS A MONSTER.
	 *
	 * The best thing in the whole event and it is almost free, because the mob
	 * already exists — TurnedEntity is the villager who does not sleep and comes
	 * at you with an axe after dark. Twelve of it, in the rain, walking.
	 *
	 * WHY VILLAGERS AND NOT SOMETHING WITH TEETH. Every hostile silhouette in
	 * Minecraft is a promise: the player reads "zombie" and instantly knows the
	 * damage, the speed, the reach and how many hits it takes. A villager is the
	 * opposite promise — it is the one shape in the game that has never once been a
	 * threat — so a dozen of them closing in has no rules attached to it at all,
	 * and the player has to work out what is happening while it happens.
	 *
	 * THEY ARE SILENT IN VANILLA'S VOICE. setSilent kills the villager sound set
	 * outright, and what replaces it is ours: the same closed-mouth hum, two thirds
	 * the pitch, with the note failing in the middle. See ModSounds.HUM. One of them
	 * doing that is ambiguous. Twelve is not.
	 *
	 * Marked SENT like everything else he brings, so they go when he goes — as
	 * chickens, standing where the crowd was.
	 */
	private static boolean raiseTurned(ServerLevel level, BlockPos at, ServerPlayer quarry) {
		com.bloomlet.herobrine.entity.TurnedEntity one =
			com.bloomlet.herobrine.entity.ModEntities.TURNED.create(
				level, EntitySpawnReason.EVENT);
		if (one == null) {
			return false;
		}
		one.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
			level.getRandom().nextFloat() * 360.0F, 0.0F);
		one.setSilent(true);          // vanilla's "hmm" never plays
		one.setTarget(quarry);
		one.setAttached(SENT, true);
		one.setPersistenceRequired();
		if (!level.addFreshEntity(one)) {
			return false;
		}
		level.playSound(null, at, com.bloomlet.herobrine.sound.ModSounds.HUM,
			SoundSource.HOSTILE, 1.1F, 0.94F + level.getRandom().nextFloat() * 0.12F);
		return true;
	}

	private static boolean raise(ServerLevel level, BlockPos at, ServerPlayer quarry,
	                             RandomSource random) {
		net.minecraft.world.entity.monster.zombie.Zombie z =
			EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
		if (z == null) {
			return false;
		}
		z.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
			random.nextFloat() * 360.0F, 0.0F);
		z.setAttached(SENT, true);
		z.setPersistenceRequired();
		z.setCanPickUpLoot(false);

		// ACTUAL BABIES, not adults shrunk with the SCALE attribute.
		//
		// The first version set SCALE to 0.72, which makes a small adult — same
		// gait, same proportions, same everything, just further away looking.
		// A baby zombie is a genuinely different creature: its own head-to-body
		// ratio, its own run, and a hitbox low and narrow enough that swinging
		// at one in a crowd is a real problem. It is also the one vanilla mob
		// with a reputation, and borrowing that reputation is free.
		z.setBaby(true);

		// AND A HELMET, WHICH IS THE WHOLE REASON THEY SURVIVE A DAY HUNT.
		//
		// Zombies burn in sunlight and the hunt stopped waiting for dark two
		// updates ago, so a daytime sending caught fire and was gone in about
		// eight seconds while he was still walking over. Vanilla's own answer
		// is a hat: isSunBurnTick checks whether the head slot is empty.
		//
		// Dyed black rather than plain leather, so it reads as ISSUED. A scatter
		// of ordinary brown caps looks like zombies that found them; ten
		// identical black ones looks like somebody handed them out.
		ItemStack cap = new ItemStack(Items.LEATHER_HELMET);
		cap.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
			new net.minecraft.world.item.component.DyedItemColor(0x141418));
		cap.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
			net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
		z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, cap);
		z.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0F);

		ItemStack axe = new ItemStack(Items.STONE_AXE);
		axe.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
			net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
		z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, axe);
		z.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);

		// TUNED DOWN FOR THE BABY MULTIPLIER. Vanilla gives babies a large
		// speed bonus on top of whatever their base is, so the adult figures
		// from the first pass would have come out well above a sprint — ten of
		// those is not pressure, it is an execution. Set low here so the bonus
		// lands them a shade above a normal zombie: they stay on you, and they
		// do not gain.
		set(z, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0);
		set(z, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3.0);
		set(z, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
			0.17 + random.nextDouble() * 0.04);
		// THEY SEE MUCH FURTHER THAN A ZOMBIE DOES. Sixteen is vanilla's, which
		// in a wood means half of them lose the player before they arrive and
		// stand about — and a sending that half-arrives is worse than no
		// sending at all.
		set(z, net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 64.0);
		z.setHealth(10.0F);
		z.setTarget(quarry);
		// His, so they wear his eyes. MENACE is the synced attachment the eye
		// layer already reads for possessed animals — nothing new to plumb.
		z.setAttached(Possession.MENACE, 1);
		level.addFreshEntity(z);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			at.getX() + 0.5, at.getY() + 0.4, at.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.01);
		return true;
	}

	private static void set(net.minecraft.world.entity.LivingEntity on,
	                        net.minecraft.core.Holder<
	                            net.minecraft.world.entity.ai.attributes.Attribute> which,
	                        double value) {
		if (on.getAttribute(which) != null) {
			on.getAttribute(which).setBaseValue(value);
		}
	}

	/**
	 * AND THEY GO WHEN HE DOES.
	 *
	 * Burned off rather than left standing, which is the difference between an
	 * event and a mess. It also says the right thing: they were his, they were
	 * never really there, and the moment he stops looking at you there is
	 * nothing in the field.
	 */
	/** A block a second, and only ever one — a gnawing rather than a demolition. */
	private static final int GNAW_INTERVAL = 20;
	/** Below this they can simply walk to you and none of this applies. */
	private static final int OUT_OF_REACH = 3;

	/**
	 * THEY START EATING WHATEVER IS HOLDING YOU UP.
	 *
	 * Being somewhere they cannot path to was a complete answer, and it should
	 * never be one: a tree, a pillar, a one-block ledge, and a dozen armed things
	 * mill about underneath you until the event expires. He arrives for that case
	 * himself now, which fixes HIM and leaves them looking stupid.
	 *
	 * So they take the perch apart from below. One block a second across the whole
	 * horde rather than one each — sixteen of them clearing a tree in a second
	 * would be a chainsaw, and what this wants is the sound of something steadily
	 * chewing while you decide whether to jump.
	 *
	 * NOTHING ANYBODY BUILT. Same promise the rest of the mod keeps: a wall is
	 * answered by him breaking it, slowly and audibly, and if these could eat
	 * masonry there would be no reason to build any. So they only ever take
	 * NATURAL blocks — the trunk, the branch, the stone under your boots — which
	 * is exactly the set of things somebody hiding up a tree is relying on.
	 */
	private static void gnaw(ServerLevel level) {
		java.util.List<net.minecraft.world.entity.Mob> theirs =
			level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
				new net.minecraft.world.phys.AABB(-30000000, level.getMinY(), -30000000,
					30000000, level.getMaxY(), 30000000),
				z -> z.isAlive() && Boolean.TRUE.equals(z.getAttached(SENT)));
		if (theirs.isEmpty()) {
			return;
		}
		for (net.minecraft.world.entity.Mob z : theirs) {
			if (!(z.getTarget() instanceof ServerPlayer up)
				|| up.getY() - z.getY() < OUT_OF_REACH) {
				continue;
			}
			// Down from their feet to the first thing actually bearing weight —
			// six is enough for a branch and short enough that somebody on a
			// mountainside is not having the mountain removed.
			for (int down = 1; down <= 6; down++) {
				BlockPos at = up.blockPosition().below(down);
				if (!level.getBlockState(at).isSolid()) {
					continue;
				}
				if (!diggable(level, at)
					|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, at)) {
					break;   // masonry is his job, not theirs
				}
				level.destroyBlock(at, false, z);
				level.playSound(null, at, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
					SoundSource.HOSTILE, 1.4F, 0.7F);
				return;      // ONE. See the interval above.
			}
		}
	}

	public static void dismiss(ServerLevel level) {
		int gone = 0;
		for (net.minecraft.world.entity.Mob z
				: level.getEntitiesOfClass(
					net.minecraft.world.entity.Mob.class,
					new net.minecraft.world.phys.AABB(-30000000, level.getMinY(), -30000000,
						30000000, level.getMaxY(), 30000000),
					mob -> Boolean.TRUE.equals(mob.getAttached(SENT)))) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
				z.getX(), z.getY() + 0.6, z.getZ(), 18, 0.3, 0.5, 0.3, 0.02);
			// AND WHAT IS LEFT STANDING THERE IS A CHICKEN.
			//
			// They used to simply stop existing, which is the honest reading — they
			// were never really there — and it plays as a despawn bug. Sixteen armed
			// things converging on you, and then a field of chickens, is a far worse
			// sentence: it says the horde was never the point, that he can do this to
			// anything, and it leaves you standing in the evidence.
			//
			// PERSISTENT, so they never despawn. They are the only thing in the mod
			// that outlives the event that made them, and a player who keeps finding
			// them years later around the places he was driven off is the whole joke.
			net.minecraft.world.entity.animal.chicken.Chicken left =
				EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
			if (left != null) {
				left.snapTo(z.getX(), z.getY(), z.getZ(), z.getYRot(), 0.0F);
				left.setPersistenceRequired();
				level.addFreshEntity(left);
			}
			z.discard();
			gone++;
		}
		if (gone > 0) {
			HerobrineMod.LOGGER.info("hunt: {} of his left as chickens", gone);
		}
	}

	// ---- AND THE SKY PICKS A SPOT ------------------------------------------
	/**
	 * A BOLT THAT IS AIMED, AND TELLS YOU WHERE FIRST.
	 *
	 * Everything else this mod throws at a player is either harmless or
	 * unavoidable. This is the one attack with a dodge in it: the ground marks
	 * itself, smokes for a second and a half, and then the sky comes down on it.
	 * DESIGN §9 asks for warning before lethality, and a mark on the floor is
	 * the most literal warning available.
	 *
	 * It leaves a divot — three across, tapering, three to four deep and ragged
	 * so it does not read as stamped. Craters are the only thing in this event
	 * that does not heal, and one of these in the middle of a yard is the thing
	 * a player points at afterwards.
	 *
	 * Never on anything built, never on the player's own square, and the sides
	 * are stepped rather than sheer — a hole somebody cannot climb out of while
	 * being hunted is not a scare, it is a death sentence with extra steps.
	 */
	private static final int MARKED_FOR = 32;

	public static void callDown(ServerLevel level, ServerPlayer quarry) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 20; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 4.0 + random.nextDouble() * 9.0;
			int x = quarry.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = quarry.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			final BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
			if (anythingBuiltNear(level, at) || quarry.blockPosition().closerThan(at, 3.0)) {
				continue;
			}
			// The mark. Smoke and a sound at the spot, and then a second and a
			// half in which the player can be somewhere else.
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
				at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 60, 0.6, 0.1, 0.6, 0.02);
			level.playSound(null, at, SoundEvents.TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM,
				SoundSource.HOSTILE, 1.6F, 0.6F);
			Cadence.in(level.getServer(), MARKED_FOR, () -> {
				strike(level, at, false);
				divot(level, at);
			});
			return;
		}
	}

	/** Three across, tapering, and the sides are climbable on purpose. */
	private static void divot(ServerLevel level, BlockPos middle) {
		if (!level.isLoaded(middle)) {
			return;
		}
		RandomSource random = level.getRandom();
		int deep = 3 + random.nextInt(2);
		for (int dy = 0; dy > -deep; dy--) {
			// Narrows as it goes, so the walls are a staircase rather than a
			// shaft. Somebody standing in the bottom can get out.
			int r = dy > -2 ? 1 : 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos at = middle.offset(dx, dy, dz);
					if (random.nextInt(7) == 0 || !diggable(level, at)) {
						continue;
					}
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.playSound(null, middle, SoundEvents.GENERIC_EXPLODE.value(),
			SoundSource.HOSTILE, 2.6F, 0.7F);
	}

	// ---- THE SOUNDTRACK ----------------------------------------------------
	/**
	 * Thunder, and it is the cheapest thing here by a distance.
	 *
	 * Visual-only bolts a long way off plus the roll itself. Nothing it does can
	 * touch anything, which is exactly why it is allowed to happen constantly:
	 * it is the only part of the hunt with no budget to spend, so it carries the
	 * beats where the ladder found nothing to do and the player would otherwise
	 * have had thirty silent seconds.
	 */
	public static void thunder(ServerLevel level, BlockPos around, int bolts) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < bolts; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 26.0 + random.nextDouble() * 40.0;
			final int x = around.getX() + (int)Math.round(Math.cos(angle) * range);
			final int z = around.getZ() + (int)Math.round(Math.sin(angle) * range);
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(),
					i * (6 + random.nextInt(14)), () -> {
				if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
					return;
				}
				strike(level, new BlockPos(x, level.getHeight(
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z),
					false);
			});
		}
	}

	/**
	 * HE THROWS FIRE AT THE COUNTRY WHILE HE PACES.
	 *
	 * The pause's own destruction, and it is deliberately aimed AWAY. Everything
	 * else on the ladder happens to the player's things; this happens to the
	 * horizon, forty blocks out, in a direction that has nothing to do with
	 * them. They watch a fireball leave his hand, cross the valley, and something
	 * they were not using catch light.
	 *
	 * WHICH IS THE WHOLE POINT OF IT BEING OUT THERE. Fire near the house is a
	 * problem the player can solve — dig a break, throw water, panic usefully.
	 * Fire two fields away is not a problem at all, and that is far worse to
	 * stand and look at, because there is nothing to do about it and it does not
	 * stop. He is not even attacking them. He is just wrecking the place.
	 *
	 * THE SAFETY IS THE DISTANCE, and it is the same argument the treeline rung
	 * makes: never within twenty blocks of any player, never anywhere with more
	 * than a trace of anything built near it, and never in the direction the
	 * player is standing. A fireball crossing the yard could clip somebody or
	 * light a fence; one thrown at a hillside on the far side of him cannot
	 * reach either.
	 */
	/**
	 * IT LANDS AROUND THEM, AND IT USED TO LAND BEHIND HIM.
	 *
	 * The first version aimed OUTWARD — a bearing taken from the player through
	 * him and onward, twenty-four to fifty-two blocks past. The reasoning was
	 * safety, and it produced something that reads as broken: he stands at
	 * thirty blocks, faces you, and lobs fire into the empty country behind
	 * himself. A player watching that does not think "he is shelling the
	 * hillside", they think the aiming is wrong. Which it was.
	 *
	 * So it comes at their ground now — six to eighteen blocks from where they
	 * are standing, on any bearing, close enough that they are being SHOT AT and
	 * far enough that it is never a hit they could not have moved out of.
	 *
	 * THE SAFETY MOVES FROM DISTANCE TO A GUARD, which is what the ladder's own
	 * crater rung already does: nothing lands where anything crafted is within
	 * four blocks. So the yard gets holes in it and the house does not, and a
	 * base paved wall to wall simply takes no fire at all — the correct failure.
	 */
	private static final int RAZE_NEAR = 6;
	private static final int RAZE_FAR = 18;
	/** Never nearer than this to anybody. They must be able to step out of it. */
	private static final int RAZE_CLEAR_OF_PEOPLE = 5;

	/** Ghast pace. It was 1.25 and eighteen blocks took a second and a half. */
	private static final float BALL_SPEED = 1.9F;
	/** Straight at them. Not perfect — a fireball you cannot dodge is a tax. */
	private static final float AIM_AT_THEM = 0.5F;
	/** And loose enough at the landscape to still read as shelling. */
	private static final float AIM_AT_GROUND = 1.0F;
	/** How far ahead of a running player he throws. */
	private static final double LEAD_TICKS = 5.0;

	public static void raze(ServerLevel level, net.minecraft.world.entity.LivingEntity him,
	                        ServerPlayer quarry) {
		if (!Config.get().huntWrecks) {
			return;
		}
		RandomSource random = level.getRandom();

		// ONE IN THREE COMES STRAIGHT AT THEM, and the absence of this was the
		// whole complaint. Every shot was aimed at a ground square at least five
		// blocks clear of every player, so the player could not be hit by one even
		// in principle — they were watching a fireworks display about themselves.
		//
		// Not gated on huntFire, unlike the shelling below. huntFire is the switch
		// for "he sets my landscape alight"; a fireball thrown at a person is an
		// attack, and turning off forest fires should not disarm him.
		if (random.nextInt(3) == 0) {
			atThem(level, him, quarry);
			return;
		}
		if (!Config.get().huntFire) {
			return;
		}
		// Outward from the PLAYER through him and onward, so whatever he throws
		// at is on the far side of him. The player is never between him and the
		// thing he is aiming at.
		for (int attempt = 0; attempt < 24; attempt++) {
			// Any bearing around them, so it does not read as a lane he is
			// firing down. The threat is that it could come from any side.
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = RAZE_NEAR + random.nextDouble() * (RAZE_FAR - RAZE_NEAR);
			int x = quarry.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = quarry.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
			if (!clearOfPeopleBy(level, at, RAZE_CLEAR_OF_PEOPLE)
				|| anythingBuiltNear(level, at)) {
				continue;
			}

			Vec3 from = him.getEyePosition();
			Vec3 along = new Vec3(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5)
				.subtract(from);
			net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball ball =
				new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(
					level, him, along.normalize());
			ball.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
			// Ghast pace, and a third of the old scatter. Inaccuracy three meant a
			// shot at eighteen blocks could land nine off target, so "he is
			// shelling the treeline" and "he is throwing fire at nothing" looked
			// identical. It still arcs slightly, because the arc is what makes a
			// ground shot readable from the side.
			ball.shoot(along.x, along.y + along.length() * 0.08, along.z,
				BALL_SPEED, AIM_AT_GROUND);
			level.addFreshEntity(ball);
			level.playSound(null, him.blockPosition(), SoundEvents.BLAZE_SHOOT,
				SoundSource.HOSTILE, 2.4F, 0.5F);
			HerobrineMod.LOGGER.debug("hunt: something goes up at [{}, {}], {} blocks out",
				x, z, (int)Math.sqrt(quarry.blockPosition().distSqr(at)));
			return;
		}
	}

	private static boolean clearOfPeopleBy(ServerLevel level, BlockPos at, int howFar) {
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(at, howFar)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * ONE BOLT, CLOSE TO A SPOT, AND IT CANNOT BURN ANYTHING.
	 *
	 * The watch's own punctuation — see HerobrineEntity.watch. Distinct from
	 * {@link #thunder} because that scatters bolts twenty-six to sixty-six
	 * blocks out to fill a horizon, and this wants the opposite: right here,
	 * sometimes exactly here, so that a figure standing in the rain at thirty
	 * blocks is lit from behind and there is nothing left to wonder about.
	 *
	 * Visual only with no argument about it. Real fire belongs to the treeline
	 * rung, which has two distance checks and a cap of three behind it; this
	 * fires next to somebody every few seconds and could not carry those.
	 *
	 * @param onTheSpot true to land it on the position itself rather than near
	 */
	public static void overhead(ServerLevel level, BlockPos at, boolean onTheSpot) {
		RandomSource random = level.getRandom();
		int x = at.getX();
		int z = at.getZ();
		if (!onTheSpot) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 6.0 + random.nextDouble() * 16.0;
			x += (int)Math.round(Math.cos(angle) * range);
			z += (int)Math.round(Math.sin(angle) * range);
		}
		if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
			return;
		}
		strike(level, new BlockPos(x, level.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z), false);
	}

	private static void strike(ServerLevel level, BlockPos at, boolean real) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}
		bolt.setVisualOnly(!real);
		bolt.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(bolt);
		scar(level, at);
	}

	/**
	 * AND EVERY ONE OF THEM LEAVES A MARK NOW.
	 *
	 * Only the aimed bolt did — callDown digs a proper three-deep divot — and that
	 * fires on one pause in three. Every other bolt in the event, and there are six
	 * to nine on arrival alone, was pure light: a storm that flashed all night and
	 * left a landscape you could not tell had been struck.
	 *
	 * Smaller than a divot on purpose. A crater is an ATTACK and belongs to the one
	 * he aimed; this is weather, and weather leaves scorch and a scuff. Two across,
	 * one deep, most of it left alone — so twenty of them over a hunt read as a
	 * field that has been hit twenty times rather than as a quarry.
	 *
	 * Same three guards as everything else that touches ground: mobGriefing off
	 * means nothing happens, and nothing anybody placed is ever taken.
	 */
	private static void scar(ServerLevel level, BlockPos at) {
		if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)
			|| !level.isLoaded(at)) {
			return;
		}
		RandomSource random = level.getRandom();
		BlockPos ground = null;
		for (int down = 0; down <= 3 && ground == null; down++) {
			BlockPos maybe = at.below(down);
			if (level.getBlockState(maybe).isSolid()) {
				ground = maybe;
			}
		}
		if (ground == null) {
			return;
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = ground.offset(dx, 0, dz);
				if (random.nextInt(3) != 0 || !diggable(level, pos)
					|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, pos)) {
					continue;
				}
				// Scooped, not cratered — and the floor under it goes dead, which
				// is what says something came down here rather than that a mob dug.
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				BlockPos under = pos.below();
				if (level.getBlockState(under).isSolid() && diggable(level, under)
					&& !com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, under)) {
					level.setBlock(under, Blocks.COARSE_DIRT.defaultBlockState(), 3);
				}
			}
		}
	}

	// ---- #9: WHAT HE DOES TO THE GROUND WHILE HE IS STANDING STILL ---------
	/**
	 * THE PLACE GOES WRONG AROUND YOU WHILE HE WATCHES.
	 *
	 * The pause is the best beat in the event and it was also the emptiest: a
	 * motionless figure at thirty blocks, ten things converging, and a landscape
	 * that stayed exactly as pretty as it was before he arrived. He is supposed to
	 * be the reason the world looks like this.
	 *
	 * SO IT SPREADS RATHER THAN EXPLODING. Nothing here is an attack and nothing
	 * here can hurt anybody — it is the grass dying, the bark going, the flowers
	 * turning to sticks. The player is fighting the horde and notices, four seconds
	 * later, that the field they are fighting in has changed colour.
	 *
	 * THE PALE GARDEN PALETTE, because the game already owns a haunted wood and it
	 * would be perverse to invent a worse one. Pale moss for grass, pale hanging
	 * moss under the leaves, dead bush where anything was flowering, bark stripped
	 * off the trunks. A player who has seen a pale garden reads it instantly; one
	 * who has not simply sees the colour drain out of the ground.
	 *
	 * DELIBERATELY NOT MOVEMENT AND NOT A TARGET. It changes no state he acts on —
	 * he stands exactly as still, stares exactly as long, sends exactly the same
	 * wave — so it cannot conflict with the watch, with the horde, or with the
	 * moment he decides to come back in. It is only ever blocks.
	 */
	private static final int BLIGHT_NEAR = 4;
	private static final int BLIGHT_FAR = 16;
	private static final int BLIGHT_TRIES = 14;

	public static void blight(ServerLevel level, ServerPlayer quarry) {
		if (!Config.get().huntWrecks
			|| !level.getGameRules().get(
				net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		RandomSource random = level.getRandom();
		int touched = 0;
		for (int attempt = 0; attempt < BLIGHT_TRIES; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = BLIGHT_NEAR + random.nextDouble() * (BLIGHT_FAR - BLIGHT_NEAR);
			int x = quarry.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = quarry.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			int y = level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			for (int dy = 0; dy <= 6 && touched < 3; dy++) {
				if (wither(level, new BlockPos(x, y - dy, z))) {
					touched++;
				}
			}
			if (touched >= 3) {
				return;
			}
		}
	}

	/** One block, turned to whatever the dead version of itself is. */
	private static boolean wither(ServerLevel level, BlockPos at) {
		if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, at)) {
			return false;      // never anything anybody put there
		}
		net.minecraft.world.level.block.state.BlockState was = level.getBlockState(at);
		net.minecraft.world.level.block.Block block = was.getBlock();
		if (block == Blocks.GRASS_BLOCK || block == Blocks.MOSS_BLOCK) {
			level.setBlock(at, Blocks.PALE_MOSS_BLOCK.defaultBlockState(), 3);
			return true;
		}
		if (block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS
			|| block == Blocks.FERN || block == Blocks.LARGE_FERN
			|| was.is(net.minecraft.tags.BlockTags.FLOWERS)) {
			level.setBlock(at, Blocks.DEAD_BUSH.defaultBlockState(), 3);
			return true;
		}
		if (was.is(net.minecraft.tags.BlockTags.LEAVES)) {
			// Left standing, with the moss hanging out of the underside of it.
			BlockPos under = at.below();
			if (level.getBlockState(under).isAir()) {
				level.setBlock(under, Blocks.PALE_HANGING_MOSS.defaultBlockState(), 3);
				return true;
			}
			return false;
		}
		if (block == Blocks.OAK_LOG) {
			level.setBlock(at, Blocks.STRIPPED_OAK_LOG.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
					was.getValue(
						net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)),
				3);
			return true;
		}
		if (block == Blocks.BIRCH_LOG) {
			level.setBlock(at, Blocks.STRIPPED_BIRCH_LOG.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
					was.getValue(
						net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)),
				3);
			return true;
		}
		return false;
	}

	// ---- WHAT HE SAYS WHEN HE IS HURT --------------------------------------
	/**
	 * THE ONE TIME HE BREAKS HIS OWN VOICE.
	 *
	 * Every word he has written so far follows a rule: lowercase, four words,
	 * no full stop, never a threat outright. Signs that could have been anybody.
	 * That restraint is most of what makes him work — a thing that explains
	 * itself is a thing you have measured.
	 *
	 * This is the only place it goes. Not because the writing got worse, but
	 * because it is the ONLY MOMENT IN THE ENTIRE MOD IN WHICH HE HAS BEEN
	 * HURT, and something losing its composure is the tell that you reached it.
	 * The player has spent forty hours learning that he never says anything
	 * direct; the value of these lines is entirely borrowed from that.
	 *
	 * So they stay in his register and only drop the restraint. No gore —
	 * SPECIFICITY, which is worse. "i know where you sleep alex" is a smaller
	 * sentence than any threat and it names a real place, and the name is the
	 * device: the sign system has done %s substitution since TRESPASSER and it
	 * has never once been aimed at somebody mid-fight.
	 */
	/**
	 * WHAT HE SAYS WHEN HE HAS ALREADY DONE IT.
	 *
	 * Every one of these is written to be read AFTER he is in position, never
	 * before — a warning is a warning and this is not one. The player is looking
	 * at a wall of their own cave when the line arrives, and the line's entire
	 * job is to make them turn around.
	 *
	 * Short, lowercase, no punctuation, same as the rest of him. "look behind
	 * you" is four words and it is the best thing in this file, because the mod
	 * has spent the whole game earning the player's willingness to believe it.
	 */
	/**
	 * WHAT HE SAYS BEFORE HE DOES IT.
	 *
	 * A tell, asked for by name, and three rules came with it.
	 *
	 * NO SOUND. Every other line he speaks rolls a cue off the hills, and on a tell
	 * that is exactly wrong — a noise announcing an announcement is a cutscene, and
	 * it tells the player to stop and listen at the moment they should be deciding
	 * what to do. This arrives silently in the corner of the screen while they are
	 * already busy, and finding it there is the whole effect.
	 *
	 * WHISPERED TO ONE PERSON, like `found` and unlike `taunt`. A warning read by
	 * five people is four people being told about somebody else's problem.
	 *
	 * AND IT IS THE ADULT REGISTER. Everything else he says is restrained on
	 * purpose — four lowercase words, never an outright threat, signs that could
	 * have been anybody. That restraint is what makes him work and it is also why
	 * these can go where they go: he has spent forty hours never once saying what
	 * he wants, so the first time he describes it in bodily detail it lands like a
	 * different thing has started speaking. Physical, unhurried, specific about
	 * what a person is made of. Not slasher-film — a slasher enjoys the audience.
	 * These are said to somebody he has already decided about.
	 */
	private static final String[][] TELLS = {
		// 0 — the wave is on its way. About THEM, not about him.
		{
			"they have been under there a long time",
			"they can smell where you have been bleeding",
			"they do not stop when you break them",
			"they were people last week",
			"count them if you like",
			"they are hungrier than i am",
			"do not let them get behind you",
		},
		// 1 — he has finished waiting and is walking in.
		{
			"i am walking now",
			"i want to hear the small bones first",
			"hold still it is easier that way",
			"there is a great deal of blood in a person",
			"you will be tired long before i am",
			"i have been deciding how to start",
			"i am going to take my time",
			"look at your hands while you still have them",
		},
		// 2 — the sky. Said once, when the storm turns.
		{
			"the ground is mine tonight",
			"nothing out here is going to help you",
			"i have opened the sky over your house",
			"there will not be anywhere dry",
		},
	};

	/**
	 * @param moment 0 the wave, 1 he is coming in, 2 the sky
	 */
	/** He is not a narrator. Thirty seconds between anything he says. */
	private static final int TELL_COOLDOWN = 600;
	private static long lastTold = -10000L;

	public static void tell(ServerLevel level, ServerPlayer quarry, int moment) {
		// THIRTEEN OF THESE LANDED IN ONE HUNT and that is a different character
		// from the one they were written for. Every wave says something and every
		// return from a pause says something, so a five-minute fight with four
		// waves and half a dozen repositions produced a running commentary.
		//
		// The lines are good BECAUSE they are rare — he has spent the whole game
		// saying nothing, and a thing that comments on its own fight is a thing you
		// have got the measure of. Half a minute apart, and the ones that lose the
		// race are simply dropped rather than queued: a warning that arrives late is
		// about a moment that has already happened.
		long now = level.getGameTime();
		if (now < lastTold + TELL_COOLDOWN) {
			return;
		}
		lastTold = now;
		String[] pool = TELLS[Math.max(0, Math.min(TELLS.length - 1, moment))];
		String line = pool[level.getRandom().nextInt(pool.length)];
		// No roll, no cue, nothing played. See above.
		quarry.sendSystemMessage(Component.literal("§8§o" + line));
		HerobrineMod.LOGGER.info("hunt: tell {} to {} — \"{}\"",
			moment, quarry.getName().getString(), line);
	}

	private static final String[] FOUND = {
		"look behind you",
		"found you",
		"you stopped moving",
		"i can see you %s",
		"turn around",
		"there you are",
	};

	/**
	 * He is standing behind them, and now they know.
	 *
	 * Called at the moment of arrival rather than before it, so that the two
	 * seconds between reading it and being hit are two seconds of the player
	 * doing something about it — which is the only kind of warning worth giving.
	 */
	public static void found(ServerLevel level, ServerPlayer quarry) {
		String line = FOUND[level.getRandom().nextInt(FOUND.length)]
			.replace("%s", quarry.getName().getString());
		// ONLY THEM. This is the opposite case from taunt, and deliberately so:
		// a taunt is a public humiliation and works better with an audience,
		// while "look behind you" read by five people is five people turning
		// round and four of them seeing nothing. Whispered to the one person it
		// is true about.
		quarry.sendSystemMessage(Component.literal("§8§o" + line));
		com.bloomlet.herobrine.sound.ModSounds.roll(level, quarry.blockPosition(),
			com.bloomlet.herobrine.sound.ModSounds.BREATH, 1.6F, 0.72F);
		HerobrineMod.LOGGER.info("hunt: arrived behind {} — \"{}\"",
			quarry.getName().getString(), line);
	}

	private static final String[][] SAID = {
		// The first blow. He is not threatening yet, he is correcting them.
		{
			"you should not have done that %s",
			"that was a mistake %s",
			"i felt that",
		},
		// The second. The restraint is gone.
		{
			"i will take everything from you",
			"i know where you sleep %s",
			"you will not be able to stay awake %s",
		},
		// The third, as he goes, and it is four words again. He is back in
		// control of himself, which is the note this should end on.
		{
			"not tonight %s",
			"soon",
			"i am not finished",
		},
	};

	public static void taunt(ServerLevel level, ServerPlayer striker, int blow) {
		String[] pool = SAID[Math.max(0, Math.min(SAID.length - 1, blow - 1))];
		String line = pool[level.getRandom().nextInt(pool.length)]
			.replace("%s", striker.getName().getString());
		// EVERYBODY PRESENT HEARS IT, and the name in it belongs to one of them.
		// On a server that is the whole trick: five people watch him single
		// somebody out, and the person he named has to keep playing next to
		// them.
		//
		// Present, though — not the whole player list. Somebody farming two
		// thousand blocks away reading a threat aimed at a fight they are not in
		// gets the spectacle with none of the fear, which is how a line that
		// should land once a campaign becomes chat noise.
		for (ServerPlayer here : level.players()) {
			if (here.blockPosition().closerThan(striker.blockPosition(), 256.0)) {
				here.sendSystemMessage(Component.literal("§8§o" + line));
			}
		}
		// SILENT. THIS IS THE THIRD TIME IT HAS BEEN ASKED FOR.
		//
		// The line used to roll ANGER off the hills behind the player, which is the
		// sound that kept getting reported as "the enderman echo when he is hurt".
		// It was never the hit sound — that was removed two builds ago — it was the
		// TAUNT announcing itself, and a threat that arrives with its own fanfare is
		// a threat somebody wrote rather than something that happened.
		//
		// The words are the whole event. They land in chat, in his own register,
		// with nothing telling the player to look at them.
		HerobrineMod.LOGGER.info("hunt: blow {} from {} — \"{}\"",
			blow, striker.getName().getString(), line);
	}
}
