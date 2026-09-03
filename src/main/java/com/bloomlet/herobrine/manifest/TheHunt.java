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
 * take the walls, the chests or the roof. See README.md — this bends the
 * rule in the same direction the break-in already does, and stops in the same
 * place.
 */
public final class TheHunt {

	private TheHunt() {}


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
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE
			.register(TheHunt::spares);
		// A fighter dies in his world: the act is the checkpoint. See playerFell.
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH
			.register((dead, how) -> {
				if (dead instanceof ServerPlayer gone && dead.level() instanceof ServerLevel where
					&& where.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
					com.bloomlet.herobrine.entity.HerobrineEntity him =
						com.bloomlet.herobrine.entity.HerobrineEntity.oneIn(where);
					if (him != null) {
						him.playerFell(gone);
					}
				}
			});
	}

	/**
	 * HE DOES NOT KILL ANYBODY ON THIS SIDE OF THE WAY.
	 *
	 * The answer to him is not supposed to be in the overworld. Everything he does
	 * out here — the sword, the burning, the roof coming in, the bolts, the
	 * fireballs — hurts exactly as much as it ever did, right down to half a heart,
	 * and then the blow that would finish somebody does not land.
	 *
	 * ONE CHOKEPOINT RATHER THAN NINE. His damage reaches a player through the
	 * melee, the shell, the sweep, the lunge, three kinds of lightning, a fireball
	 * and an explosion, and clamping each of those in place would be eight more
	 * things to keep in step. ALLOW_DAMAGE is where all of it converges.
	 *
	 * NOT THE OLD MERCY, AND THE DIFFERENCE MATTERS. That one triggered on a health
	 * threshold and skipped the WHOLE BLOW — no fire, no shove — and then withdrew,
	 * so a player parked at four hearts made him walk up and decline, forever. This
	 * refuses one thing only: the last point of damage. He still connects, still
	 * throws you, still takes the house apart while you stand in it at half a
	 * heart. There is nothing to farm.
	 *
	 * HIS DOING ONLY. A creeper, a fall into the ravine he threw you toward, the
	 * mobs that turned up because it is night — all still kill you. He is not a
	 * shield.
	 */
	private static boolean spares(net.minecraft.world.entity.LivingEntity hurt,
	                              net.minecraft.world.damagesource.DamageSource source,
	                              float amount) {
		if (!(hurt instanceof ServerPlayer player)
			|| !(hurt.level() instanceof ServerLevel here)
			|| here.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			return true;      // his own world is where he is allowed to win
		}
		if (player.getHealth() - amount > 0.0F || !hisDoing(here, source)) {
			return true;
		}
		// And put them out on the way past. Burning is a hundred separate one-point
		// blows, so without this they would stand in his fire at half a heart with
		// every tick of it refused and none of it stopping.
		player.clearFire();
		return false;
	}

	/**
	 * Whether this came from him.
	 *
	 * The mirror of HerobrineEntity.hisOwnDoing, and the same shape: his hand, his
	 * projectiles, and — while he is actually out here — the weather. A bolt is not
	 * owned by anybody in the way a fireball is, so the test for lightning is that
	 * he is standing in this level to have called it.
	 */
	private static boolean hisDoing(ServerLevel here,
	                                net.minecraft.world.damagesource.DamageSource source) {
		if (source.getEntity() instanceof com.bloomlet.herobrine.entity.HerobrineEntity
			|| source.getDirectEntity()
				instanceof com.bloomlet.herobrine.entity.HerobrineEntity) {
			return true;
		}
		if (source.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot
			&& shot.getOwner() instanceof com.bloomlet.herobrine.entity.HerobrineEntity) {
			return true;
		}
		if (source.getDirectEntity()
				instanceof net.minecraft.world.entity.projectile.Projectile flew
			&& flew.getOwner() instanceof com.bloomlet.herobrine.entity.HerobrineEntity) {
			return true;
		}
		if (!com.bloomlet.herobrine.entity.HerobrineEntity.outInTheOverworld(here)) {
			return false;
		}
		return source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)
			|| source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
			|| source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
	}


	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		++tickCounter;
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
		// AND THE OWED HUNT IS GONE.
		//
		// This picked a random overworld player once a chapter was reached and put a
		// hunt on them. It was the one thing in the mod that came looking for you
		// without you having done anything, which was its point — and it is exactly
		// what the dimension is for now. Nothing hunts anybody on this side; you go
		// through and find it.
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


	/** They looked. He is not going to pretend that did not happen. */
	private static final String[] SEEN_ME = {
		"there we are",
		"you kept looking",
		"now we both know",
		"i can smell the iron in you from here",
		"say it out loud so it is real",
	};

	/** They never looked up, and he has been there the whole minute. */
	private static final String[] NEVER_LOOKED = {
		"i have been here a while",
		"you never once looked up",
		"i watched you finish what you were doing",
		"i was close enough to smell your hair",
		"i counted your breaths and stopped at four hundred",
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


	/** Ground, and only ground. Anything crafted is somebody's and is refused. */
	/**
	 * NOTHING BUILT IS SAFE. ONLY THE WAY ITSELF.
	 *
	 * There were four separate build vetoes stacked on top of each other and
	 * between them they cancelled most of what he does. DwellTracker.isBuilt,
	 * here; a SECOND copy of the same call inside scar; anythingBuiltNear
	 * refusing whole crater SITES rather than single blocks, at four call sites;
	 * and the same veto inside the fireball mixin. All of them gone.
	 *
	 * The thing to understand about the one that lived here is that it never did
	 * what its name suggests. Minecraft does not record who placed a block, so
	 * DwellTracker cannot know — it is a MATERIAL LIST. Planks, stone bricks,
	 * glass, doors, wool, beds, fences. Which meant the veto did not protect
	 * "somebody's base"; it protected the idea of a wall, everywhere, forever,
	 * including a village street, a mineshaft fence, a ruined portal and his own
	 * house. He could not break a plank anywhere in the world.
	 *
	 * The visible symptom was one line: "he opened the roof over Robin — 0 blocks
	 * off". Zero, reported as a success, because the whole building was made of
	 * materials on the list.
	 *
	 * So the only thing that survives a blast is the way out. Not because it is
	 * built, but because it is the one block in the mod whose destruction would
	 * break the story rather than damage a world.
	 */
	private static boolean diggable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
			return false;
		}
		if (isTheWay(state)) {
			return false;
		}
		// AND THAT IS THE WHOLE TEST NOW. THE WHITELIST WAS THE BUG.
		//
		// This used to end with a list: dirt, base stone, sand, gravel, terracotta,
		// snow, replaceables. Everything else was refused — which meant his
		// lightning silently did nothing on a stone-brick floor, a plank deck, a
		// path, a village street, deep slate variants outside the base-stone tag,
		// or in a nether biome. Standing on the wrong block made him harmless and
		// there was no way to tell that was why.
		//
		// Worse, it could not finish its own work: scar lays MAGMA_BLOCK at the
		// bottom of a deep crater and magma is on no list, so the hole sealed itself
		// against the next bolt.
		//
		// And the two guards above are now the whole test: nothing indestructible,
		// and not the way out. Past that it breaks, whoever put it there.
		return true;
	}

	/**
	 * The way out, and the frames that hold it.
	 *
	 * REINFORCED_DEEPSLATE is the marker rather than a coordinate check, and that
	 * is deliberate: it is placed in exactly two places in the mod — the four
	 * corners of the gate and the tower's anchor — and it is not obtainable, so a
	 * player cannot accidentally make something immune by building with it.
	 *
	 * The vanilla portal surfaces go on the list too. Their frames do not — an
	 * obsidian ring is a build and builds are fair game now — but deleting the
	 * portal BLOCK out from under somebody mid-hunt is a different class of
	 * accident from wrecking their wall, and it is not the one being asked for.
	 */
	private static boolean isTheWay(BlockState state) {
		return state.is(com.bloomlet.herobrine.block.ModBlocks.THE_WAY)
			|| state.is(Blocks.REINFORCED_DEEPSLATE)
			|| state.is(Blocks.NETHER_PORTAL)
			|| state.is(Blocks.END_PORTAL)
			|| state.is(Blocks.END_GATEWAY)
			|| state.is(Blocks.END_PORTAL_FRAME);
	}

	/**
	 * Take one block out, and let a container spill rather than vanish.
	 *
	 * THE ONE THING THAT IS NOT SIMPLY DELETED. Everything else in a crater is
	 * gone with no drops, which is right — a hole that hands back forty dirt is a
	 * delivery, not a wound. A chest is different in kind: setBlock over the top
	 * of one silently voids everything inside it, and "his lightning ate my
	 * enchanted books" is a bug report about the mod rather than a story about
	 * him. destroyBlock breaks it properly, so the shulkers and the diamonds end
	 * up on the floor of the crater, on fire, where he can see them.
	 */
	private static void carve(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) != null) {
			level.destroyBlock(pos, true);
			return;
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
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

	private static void set(net.minecraft.world.entity.LivingEntity on,
	                        net.minecraft.core.Holder<
	                            net.minecraft.world.entity.ai.attributes.Attribute> which,
	                        double value) {
		if (on.getAttribute(which) != null) {
			on.getAttribute(which).setBaseValue(value);
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
		callDown(level, quarry, quarry.blockPosition());
	}

	/**
	 * @param about where he thinks they are — see HerobrineEntity.mark
	 *
	 * A ranged attack aimed at a belief. Four to thirteen blocks out from the spot,
	 * so a stale mark still puts bolts in the right field and the wrong garden.
	 */
	public static void callDown(ServerLevel level, ServerPlayer quarry, BlockPos about) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 20; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 4.0 + random.nextDouble() * 9.0;
			int x = about.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = about.getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
				continue;
			}
			final BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
			// Never right on top of them. Nothing else disqualifies a spot any more
			// — a bolt that refuses to land near a wall refuses to land anywhere a
			// person actually is.
			if (quarry.blockPosition().closerThan(at, 3.0)) {
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
					carve(level, at);
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
	/**
	 * WHAT HE DOES ABOUT A ROOF.
	 *
	 * Going quiet when somebody seals themselves in is the right FIRST answer and a
	 * terrible only one. It works because they cannot tell where he is; it stops
	 * working the moment they work out that waiting costs them nothing. A player
	 * with a dirt box and ten spare seconds beats the entire mod.
	 *
	 * So the silence stays and becomes a countdown. Stay in there and the ceiling
	 * comes off — and it comes off from OUTSIDE, aimed at the structure rather than
	 * at them, which is the difference between being attacked and being excavated.
	 *
	 * BIG ON THE BUILDING, SMALL ON THE PERSON. A vanilla explosion is the wrong
	 * tool: at a radius that opens a roof it also removes most of a player, and
	 * then camping is not solved, it is punished with death. This one is hand-rolled
	 * precisely so the two can be tuned apart — nine blocks across of hole and a
	 * couple of hearts through armour.
	 *
	 * IT TAKES WHAT THEY BUILT — and so, now, does everything else. This used to be
	 * the exception in a file where every other beat checked DwellTracker and
	 * refused; it is no longer an exception, because that check is gone from all of
	 * them. The whole point is the wall they put up, and a bomb that politely leaves
	 * the player's blocks alone is a bomb aimed at scenery.
	 */
	private static final int SHELL_WIDE = 4;
	private static final int SHELL_DOWN = 5;
	private static final float SHELL_HURTS = 5.0F;
	private static final double SHELL_REACH = 7.0;
	/** How far up it looks for something over their head. */
	private static final int LOOKS_UP = 18;

	public static void shell(ServerLevel level, net.minecraft.world.entity.LivingEntity him,
	                         ServerPlayer quarry) {
		shell(level, him, quarry,
			BlockPos.containing(quarry.getX(), quarry.getEyeY(), quarry.getZ()));
	}

	/**
	 * @param head the spot he is aiming over — see roofOver(ServerLevel, BlockPos)
	 *
	 * HE CAN MISS NOW. Every caller used to pass the player's live position, which
	 * meant the roof came in over them however deep they were and however little he
	 * could see of it. The callers pass their MARK instead — them if he has eyes on
	 * them, otherwise the last place he did — so sealing yourself in and then moving
	 * two rooms over is an answer to this, and it was not one before.
	 */
	public static void shell(ServerLevel level, net.minecraft.world.entity.LivingEntity him,
	                         ServerPlayer quarry, BlockPos head) {
		if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		BlockPos aim = roofOver(level, head);
		RandomSource random = level.getRandom();

		// The noise and the flash, hand-played. level.explode at a radius that
		// opens a roof deals its own entity damage on the way past and there is no
		// setting that separates the two.
		level.playSound(null, aim, SoundEvents.GENERIC_EXPLODE.value(),
			SoundSource.HOSTILE, 4.0F, 0.7F + random.nextFloat() * 0.2F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
			aim.getX() + 0.5, aim.getY() + 0.5, aim.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);

		int taken = 0;
		for (int dx = -SHELL_WIDE; dx <= SHELL_WIDE; dx++) {
			for (int dz = -SHELL_WIDE; dz <= SHELL_WIDE; dz++) {
				double out = Math.sqrt(dx * dx + dz * dz);
				if (out > SHELL_WIDE + 0.5) {
					continue;
				}
				// Ragged, so it never reads as a shape somebody typed in.
				if (out > SHELL_WIDE - 1 && random.nextInt(3) == 0) {
					continue;
				}
				// Deeper in the middle, and it punches DOWNWARD — a roof opened
				// from above is a hole you are standing under, which is the whole
				// image. Upward it would only take sky.
				int deep = (int) Math.round(SHELL_DOWN * (1.0 - out / (SHELL_WIDE + 0.5)));
				for (int down = -1; down <= deep; down++) {
					BlockPos at = aim.below(down);
					BlockPos pos = at.offset(dx, 0, dz);
					// THEIR ROOF INCLUDED. That is the whole beat.
					if (!diggable(level, pos)) {
						continue;
					}
					carve(level, pos);
					taken++;
				}
			}
		}

		// AND IT BURNS. On the rim rather than in the hole, so what is left standing
		// is what catches — a fire in a crater goes out on its own and a fire on the
		// broken edge of somebody's roof does not.
		if (Config.get().huntFire) {
			for (int attempt = 0; attempt < 14; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double out = SHELL_WIDE - 1 + random.nextDouble() * 2.0;
				BlockPos lip = aim.offset((int) Math.round(Math.cos(angle) * out),
					random.nextInt(3) - 2, (int) Math.round(Math.sin(angle) * out));
				if (level.getBlockState(lip).isAir()
					&& level.getBlockState(lip.below()).isSolid()) {
					level.setBlock(lip, Blocks.FIRE.defaultBlockState(), 3);
				}
			}
		}

		// And the person under it is shaken, not killed.
		for (ServerPlayer near : level.players()) {
			double away = Math.sqrt(near.blockPosition().distSqr(aim));
			if (away > SHELL_REACH) {
				continue;
			}
			float hurt = SHELL_HURTS * (float) (1.0 - away / SHELL_REACH);
			near.hurtServer(level, level.damageSources().explosion(him, him), hurt);
			near.push((near.getX() - aim.getX()) * 0.12, 0.42,
				(near.getZ() - aim.getZ()) * 0.12);
			near.hurtMarked = true;
		}
		HerobrineMod.LOGGER.info("hunt: he opened the roof over {} — {} blocks off",
			quarry.getName().getString(), taken);
	}

	/** Whatever is over their head, or just above them if the answer is sky. */
	private static BlockPos roofOver(ServerLevel level, ServerPlayer quarry) {
		return roofOver(level,
			BlockPos.containing(quarry.getX(), quarry.getEyeY(), quarry.getZ()));
	}

	/**
	 * Whatever is over a SPOT, which is not always over a person.
	 *
	 * The spot is the point. He aims this at where he believes somebody is, and
	 * being wrong about that is now a thing that can happen — so the roof that
	 * comes in is the roof over the room he heard them in, whether or not they are
	 * still standing under it.
	 */
	private static BlockPos roofOver(ServerLevel level, BlockPos head) {
		for (int up = 1; up <= LOOKS_UP; up++) {
			BlockPos at = head.above(up);
			if (level.getBlockState(at).isSolid()) {
				return at;
			}
		}
		return head.above(3);
	}

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
			// People, not property. The clearance is about not dropping a fireball
			// into somebody's lap; what it lands ON is no longer its business.
			if (!clearOfPeopleBy(level, at, RAZE_CLEAR_OF_PEOPLE)) {
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
	 * A BOLT ON SOMETHING SPECIFIC, WHICH HE HAD NO WAY TO ASK FOR.
	 *
	 * Every other bolt in this file is aimed at a place — the storm, the pause, the
	 * ground near somebody. There was nothing that put one on a MOB, so a flying
	 * Herobrine had no attack at all: he could reach nothing from up there and
	 * anything with range could work on him for as long as it liked.
	 */
	public static void smite(ServerLevel level, net.minecraft.world.entity.Entity at) {
		strike(level, at.blockPosition(), true);
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

	/** Across and deep. A bowl, not a scuff. */
	/** How far under the strike it will hunt for something to break. */
	private static final int LOOKS_DOWN = 24;
	private static final int CRATER = 3;
	private static final int CRATER_DEEP = 3;

	/**
	 * EVERY BOLT DIGS A CRATER, AND ALL OF THEM ARE BIG.
	 *
	 * There used to be two sizes: the aimed bolt dug a divot and everything else
	 * left a scuff two across, on the reasoning that the aimed one was an ATTACK
	 * and the rest were weather. It reads as neither. What it actually produced was
	 * twenty barely-visible dents, so a hunt could flash all night over a field you
	 * could not afterwards tell had been struck — and the one real hole was
	 * indistinguishable from the noise around it.
	 *
	 * One size now, and it is the big one. Seven across at the lip, three deep in
	 * the middle, a proper bowl with the floor of it burnt to coarse dirt and
	 * magma at the very bottom. Twenty of those over a fight is a landscape the
	 * player will still be walking around in a week — and the hunt stops being an
	 * event that happened and becomes damage to the map.
	 *
	 * A BOWL, NOT A BOX. Depth falls off with distance from the middle, so the rim
	 * is one deep and the centre is three. A cylinder reads as something dug; a
	 * bowl reads as something that landed.
	 *
	 * Same guards as everything else that touches ground: mobGriefing off means
	 * nothing happens, and nothing anybody placed is ever taken.
	 */
	private static void scar(ServerLevel level, BlockPos at) {
		if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)
			|| !level.isLoaded(at)) {
			return;
		}
		RandomSource random = level.getRandom();
		// FINDING THE FLOOR, AND THREE BLOCKS WAS NOT FAR ENOUGH TO LOOK.
		//
		// The first bolt on a spot takes three off it. The second one arrives at
		// the same coordinates, searches three down, finds nothing but its own
		// hole, and gives up — so a spot could be struck twenty times and never get
		// deeper than the first strike made it. Every crater in the game was
		// exactly one crater deep.
		//
		// Deep enough now to fall through several of its own, so hitting the same
		// ground repeatedly does what it looks like it should: the hole keeps
		// going. Bedrock still refuses — diggable prices it at less than nothing —
		// so it cannot dig out of the world.
		BlockPos ground = null;
		for (int down = 0; down <= LOOKS_DOWN && ground == null; down++) {
			BlockPos maybe = at.below(down);
			if (level.getBlockState(maybe).isSolid()) {
				ground = maybe;
			}
		}
		if (ground == null) {
			return;
		}
		for (int dx = -CRATER; dx <= CRATER; dx++) {
			for (int dz = -CRATER; dz <= CRATER; dz++) {
				double out = Math.sqrt(dx * dx + dz * dz);
				if (out > CRATER + 0.5) {
					continue;
				}
				// The lip is ragged. A circle you could have drawn is the one thing
				// that would give the whole away.
				if (out > CRATER - 1 && random.nextInt(3) == 0) {
					continue;
				}
				int deep = (int) Math.round(CRATER_DEEP * (1.0 - out / (CRATER + 0.5)));
				for (int down = 0; down <= deep; down++) {
					BlockPos pos = ground.offset(dx, -down, dz);
					if (!diggable(level, pos)) {
						continue;
					}
					carve(level, pos);
				}
				// And what is left underneath is dead, which is what says something
				// came down here rather than that a mob dug.
				BlockPos floor = ground.offset(dx, -deep - 1, dz);
				if (!level.getBlockState(floor).isSolid() || !diggable(level, floor)) {
					continue;
				}
				// Whatever it was first, so a chest under the burnt floor spills
				// rather than becoming coarse dirt with the contents inside it.
				carve(level, floor);
				level.setBlock(floor, deep >= CRATER_DEEP && random.nextInt(4) == 0
					? Blocks.MAGMA_BLOCK.defaultBlockState()
					: Blocks.COARSE_DIRT.defaultBlockState(), 3);
			}
		}
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
			"hold still and it will be tidier",
			"there is a great deal of blood in a person",
			"you will be tired long before i am",
			"i have been deciding where to start",
			"i am going to take my time with you",
			"look at your hands while you still have them",
			"i want to see what colour you are inside",
			"i will do the legs first so you stay",
		},
		// 2 — the sky. Said once, when the storm turns.
		{
			"the ground is mine tonight",
			"nothing out here is going to help you",
			"i have opened the sky over your house",
			"there will not be anywhere dry to lie down",
			"the ground here has had people in it before",
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
		quarry.sendSystemMessage(com.bloomlet.herobrine.entity.Sayings.his(line));
		HerobrineMod.LOGGER.info("hunt: tell {} to {} — \"{}\"",
			moment, quarry.getName().getString(), line);
	}

	/**
	 * WHAT HE SAYS WHEN HE HAS NOTICED SOMETHING AND DOES NOT KNOW WHAT.
	 *
	 * The moment between hearing you and finding you, and it did not exist — he
	 * went from unaware to hunting in half a second, so being caught was a state
	 * change rather than an event you could do anything about.
	 *
	 * These are the sound of him TURNING. Not a threat and not a taunt: a question,
	 * asked into a dark wood by somebody who is fairly sure of the answer and is
	 * going to come and check. The player hears it, knows exactly what it means,
	 * and gets to decide whether to run — which is the whole point, because a
	 * warning you can act on is worth ten that you cannot.
	 *
	 * He does not know it is them. That is why none of these use a name.
	 */
	private static final String[] WHO = {
		"who is there",
		"i heard that",
		"come out then",
		"there is something in here with me",
		"i can hear your heart working",
		"do not make me come and look",
		"i can hear you deciding",
		"stand still and you will keep more of it",
		"i will find what is left of you",
		"you are leaking somewhere",
	};

	/** And when he gets to the spot and there is nothing on it. */
	private static final String[] NOTHING = {
		"nothing",
		"i know what i heard",
		"there is warm air where you were standing",
		"go on then",
		"i am not finished looking",
	};

	private static long lastSuspected = -10000L;
	private static boolean askedWho;
	/** The same half minute the taunts use. He is not a narrator. */
	private static final int SUSPICION_COOLDOWN = 600;

	/**
	 * He has noticed something. Said to everybody near, because on a server the
	 * whole group needs to know one of them has been heard.
	 *
	 * ON A COOLDOWN, BECAUSE THE BEAT ATE ITSELF. Detection re-arms the moment the
	 * last look expires, so a player simply standing in a wood near him produced
	 * "who is there" / "nothing" / "who is there" every fifteen seconds — fourteen
	 * lines in four minutes in the log that caught it. A question asked once into
	 * the dark is frightening; the same question on a loop is a mob with a
	 * soundboard.
	 *
	 * He still comes to look every time. Only the saying of it is rationed, so the
	 * silent approaches are the ones you do not get told about.
	 *
	 * The "nothing" half is gated on the question having actually been asked, so
	 * he never announces the answer to something nobody heard him ask.
	 *
	 * @return whether he said anything
	 */
	public static boolean suspects(ServerLevel level, BlockPos at, boolean found) {
		if (found) {
			if (!askedWho) {
				return false;
			}
			askedWho = false;
		} else {
			long now = level.getGameTime();
			if (now < lastSuspected + SUSPICION_COOLDOWN) {
				return false;
			}
			lastSuspected = now;
			askedWho = true;
		}
		String[] pool = found ? NOTHING : WHO;
		String line = pool[level.getRandom().nextInt(pool.length)];
		for (ServerPlayer near : level.players()) {
			if (near.blockPosition().closerThan(at, 64.0)) {
				near.sendSystemMessage(com.bloomlet.herobrine.entity.Sayings.his(line));
			}
		}
		HerobrineMod.LOGGER.info("suspicion at [{}, {}, {}] — \"{}\"",
			at.getX(), at.getY(), at.getZ(), line);
		return true;
	}

	private static final String[] FOUND = {
		"look behind you",
		"found you",
		"you stopped moving",
		"i can see your pulse from here %s",
		"turn around",
		"there you are",
	};


	private static final String[][] SAID = {
		// The first blow. He is not threatening yet, he is correcting them.
		{
			"you should not have done that %s",
			"that was a mistake %s",
			"i felt that and i want more",
			"do it again i want to feel it",
		},
		// The second. The restraint is gone.
		{
			"i will take everything from you",
			"i will take you apart at the joints %s",
			"i know how you come apart %s",
			"you will not be able to stay awake %s",
		},
		// The third, as he goes, and it is four words again. He is back in
		// control of himself, which is the note this should end on.
		{
			"not tonight %s",
			"soon",
			"i am not finished opening you",
			"i will come back for the rest %s",
		},
	};

}
