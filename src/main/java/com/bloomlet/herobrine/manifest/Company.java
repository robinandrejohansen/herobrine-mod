package com.bloomlet.herobrine.manifest;

import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.CompanionEntity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * KEEPING HER, AND WHAT SHE NOTICES.
 *
 * CompanionEntity is the creature: it walks, it runs, it breaks off, it eats.
 * This is everything about her that needs to know about the WORLD rather than
 * about her own body — what she remarks on, and what happens to her when you
 * die.
 *
 * WHY THIS IS NOT IN THE ENTITY. Two reasons, and the second is the real one.
 * A goal cannot see a death event. And a per-tick scan of everything nearby,
 * run from inside a mob, is a scan per mob — this runs once per player per
 * SPEAKS_EVERY ticks no matter how many of her there are.
 */
public final class Company {
	private Company() {}

	/** How often she is allowed to notice anything at all. */
	private static final int LOOKS_EVERY = 40;

	/** How near a thing has to be before she has an opinion about it. */
	private static final double NOTICES = 20.0;

	/**
	 * How long she stands where you fell before she comes to find you.
	 *
	 * Four minutes. Long enough that going back for your things is the obvious
	 * move and she is genuinely there waiting when you arrive — that is the beat
	 * the whole thing exists for. Short enough that a player who died somewhere
	 * unreachable, or who simply logged off, has not lost her.
	 */
	private static final long WAITS_FOR = 4800L;

	/** Where she is standing vigil, and since when. Not persisted; see below. */
	private static final java.util.Map<java.util.UUID, Long> WAITING =
		new java.util.HashMap<>();

	/**
	 * EVERYTHING HOSTILE LEARNS ABOUT HIM AS IT LOADS.
	 *
	 * A zombie hunts players, villagers, wandering traders, baby turtles and iron
	 * golems, because those five are named in Zombie.registerGoals. There is no
	 * hook for a sixth, so the goal is added from outside as each mob arrives —
	 * see MobTargetsAccessor for why an accessor rather than an injection.
	 *
	 * PRIORITY 3, BEHIND WHATEVER IT ALREADY WANTED. Vanilla's own targets are
	 * registered at 1 and 2, so a zombie standing between Addexio and a player
	 * still goes for the player. He is not a decoy that switches everything off
	 * you; he is one more thing in the field worth attacking, and the difference
	 * matters the first time you are glad he is there.
	 *
	 * MONSTER ONLY. A cow that hunted him would be funny once. Feral already owns
	 * the question of when ordinary animals turn, and it turns them on the PLAYER
	 * — leaving that alone means his presence never changes what the animals do.
	 */
	private static void hunted(net.minecraft.world.entity.Entity entity,
	                           net.minecraft.server.level.ServerLevel level) {
		if (!(entity instanceof net.minecraft.world.entity.monster.Monster mob)) {
			return;
		}
		((com.bloomlet.herobrine.mixin.MobTargetsAccessor) mob).herobrine$targets()
			.addGoal(3, new net.minecraft.world.entity.ai.goal.target
				.NearestAttackableTargetGoal<>(mob, CompanionEntity.class, true));
	}

	/**
	 * WHETHER HE HAS EVER TURNED UP, and it has to be persisted rather than
	 * counted.
	 *
	 * The obvious check is "is there a CompanionEntity in the world", and it is
	 * wrong in the one case that matters: the entity index only holds LOADED
	 * entities, so the moment a player walks four hundred blocks from wherever he
	 * was left, the answer comes back no and a second one is made. That is the
	 * exact bug Whereabouts hit with Herobrine over the keep — "there were 23 of
	 * him" — and the fix is the same. Absence of evidence is not evidence.
	 */
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean>
		HAS_COME = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
			.createPersistent(HerobrineMod.id("addexio_has_come"),
				com.mojang.serialization.Codec.BOOL);

	/** Says he has turned up, whichever of the two places did it. */
	public static void came(ServerLevel level) {
		level.getServer().overworld().setAttached(HAS_COME, true);
	}

	public static boolean hasCome(ServerLevel level) {
		return Boolean.TRUE.equals(
			level.getServer().overworld().getAttached(HAS_COME));
	}

	/** How far out he first appears, and how far he must be able to see. */
	private static final int COMES_FROM_MIN = 56;
	private static final int COMES_FROM_MAX = 84;
	private static final int TRIES = 24;

	/**
	 * HE WALKS IN OUT OF THE DISTANCE, AT THE FIRST HOUSE.
	 *
	 * He used to be standing in the undercity waiting to be found, which is under
	 * the town — the SECOND place on the trail — and it meant the companion the mod
	 * holds a four-minute vigil over arrived a third of the way through the story
	 * and only if you went down the crypt stair.
	 *
	 * So he comes to you instead, and he comes at the first house, which is the
	 * building his own first book is sitting in. You read a man's account of
	 * watching something stand in his wheat, you put the book down, and there is
	 * somebody on the ridge sixty blocks off walking towards you.
	 *
	 * FAR ENOUGH TO BE A SILHOUETTE AND NEAR ENOUGH TO ARRIVE. Fifty-six to
	 * eighty-four: past anything you would call the yard, inside the distance a
	 * name tag renders, and about fifteen seconds of walking at his pace. Follow
	 * brings him the rest of the way and he does the last part himself, which is
	 * the whole of the effect — nothing is spawned next to you.
	 *
	 * ON GROUND HE CAN WALK OFF, checked rather than hoped: dry, solid under him,
	 * two clear blocks over him, and not in a wall. Twenty-four tries, and if none
	 * of them works he simply does not come this time and the next person to walk
	 * up to the house gets another twenty-four.
	 */
	public static void arrives(ServerLevel level, Player near) {
		if (hasCome(level)) {
			return;
		}
		for (int attempt = 0; attempt < TRIES; attempt++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			double out = COMES_FROM_MIN
				+ level.getRandom().nextDouble() * (COMES_FROM_MAX - COMES_FROM_MIN);
			int x = (int) Math.round(near.getX() + Math.cos(angle) * out);
			int z = (int) Math.round(near.getZ() + Math.sin(angle) * out);
			if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
				continue;
			}
			int y = com.bloomlet.herobrine.structure.Ground.topOf(level, x, z);
			BlockPos feet = new BlockPos(x, y + 1, z);
			if (!level.getFluidState(feet).isEmpty()
				|| !level.getBlockState(feet).isAir()
				|| !level.getBlockState(feet.above()).isAir()
				|| !level.getBlockState(feet.below()).isSolid()) {
				continue;
			}
			CompanionEntity him = com.bloomlet.herobrine.entity.ModEntities.COMPANION
				.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (him == null) {
				return;
			}
			him.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
				(float) Math.toDegrees(Math.atan2(near.getZ() - feet.getZ(),
					near.getX() - feet.getX())) - 90.0F, 0.0F);
			him.setPersistenceRequired();
			him.setTarget(null);
			// AND THE TELEPORT IS HELD OFF WHILE HE COVERS IT. Without this he is
			// eighty blocks out and over Follow's twenty-six block backstop on his
			// very first tick, so the entrance is a man blinking into being next to
			// you. See CompanionEntity.beginTheWalkIn.
			him.beginTheWalkIn();
			level.addFreshEntity(him);
			came(level);
			HerobrineMod.LOGGER.info(
				"addexio is coming in from [{}, {}, {}], {} blocks off {}",
				feet.getX(), feet.getY(), feet.getZ(), (int) out,
				near.getName().getString());
			return;
		}
		HerobrineMod.LOGGER.info(
			"nowhere for addexio to walk in from yet — he will try again");
	}

	/*
	 * HE COMES FOR YOU AT FIRST LIGHT.
	 *
	 * The first meeting used to be a map in your inventory at world start and a
	 * man walking up when you reached the farm. Now there is no map: you play,
	 * and on the first morning after you have lived a full day — on the surface,
	 * not in water, not being hit — he walks out of the trees thirty blocks off,
	 * says one thing, and leads you there. Time-based so it cannot be missed;
	 * daylight so you see him coming; a day so the world has had its first night
	 * to be wrong in.
	 *
	 * Cost: one attachment read and write per online overworld player every forty
	 * ticks, and one clock read. Nothing is scanned.
	 */
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Integer> LIVED =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Integer>builder()
			.persistent(com.mojang.serialization.Codec.INT)
			.copyOnDeath()
			.buildAndRegister(HerobrineMod.id("lived"));
	private static final int A_DAY = 24000;
	private static final int MORNING_ENDS = 6000;
	private static final int COMES_TO_YOU_MIN = 30;
	private static final int COMES_TO_YOU_MAX = 42;

	private static void firstLight(MinecraftServer server) {
		ServerLevel over = server.overworld();
		if (hasCome(over) || !com.bloomlet.herobrine.Config.get().houses) {
			return;
		}
		BlockPos house = Whereabouts.home(over);
		if (house == null) {
			return;
		}
		var clock = over.registryAccess().get(net.minecraft.world.clock.WorldClocks.OVERWORLD);
		long timeOfDay = clock.isPresent()
			? Math.floorMod(server.clockManager().getTotalTicks(clock.get()), (long) A_DAY) : 0L;
		for (ServerPlayer who : over.players()) {
			if (who.isSpectator() || !who.isAlive()) {
				continue;
			}
			int lived = who.getAttachedOrElse(LIVED, 0) + LOOKS_EVERY;
			who.setAttached(LIVED, lived);
			if (lived < A_DAY || timeOfDay >= MORNING_ENDS) {
				continue;
			}
			if (!over.canSeeSky(who.blockPosition()) || who.isInWater() || who.isPassenger()
				|| who.hurtTime > 0) {
				continue;
			}
			comeFor(over, who, house);
			return;
		}
	}

	/** Like arrives(), but from the direction of the farm, and leading rather than walking in. */
	public static void comeFor(ServerLevel level, ServerPlayer near, BlockPos house) {
		if (hasCome(level)) {
			return;
		}
		double toward = Math.atan2(house.getZ() - near.getZ(), house.getX() - near.getX());
		for (int attempt = 0; attempt < TRIES; attempt++) {
			double angle = toward + (level.getRandom().nextDouble() - 0.5) * 1.4;
			double out = COMES_TO_YOU_MIN
				+ level.getRandom().nextDouble() * (COMES_TO_YOU_MAX - COMES_TO_YOU_MIN);
			int x = (int) Math.round(near.getX() + Math.cos(angle) * out);
			int z = (int) Math.round(near.getZ() + Math.sin(angle) * out);
			if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
				continue;
			}
			int y = com.bloomlet.herobrine.structure.Ground.topOf(level, x, z);
			BlockPos feet = new BlockPos(x, y + 1, z);
			if (!level.getFluidState(feet).isEmpty()
				|| !level.getBlockState(feet).isAir()
				|| !level.getBlockState(feet.above()).isAir()
				|| !level.getBlockState(feet.below()).isSolid()) {
				continue;
			}
			CompanionEntity him = com.bloomlet.herobrine.entity.ModEntities.COMPANION
				.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (him == null) {
				return;
			}
			him.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
				(float) Math.toDegrees(Math.atan2(near.getZ() - feet.getZ(),
					near.getX() - feet.getX())) - 90.0F, 0.0F);
			him.setPersistenceRequired();
			him.setTarget(null);
			him.lead(house, near);
			level.addFreshEntity(him);
			came(level);
			HerobrineMod.LOGGER.info(
				"addexio comes for {} at first light, from [{}, {}, {}] — the farm is {} blocks off",
				near.getName().getString(), feet.getX(), feet.getY(), feet.getZ(),
				(int) Math.sqrt(house.distSqr(near.blockPosition())));
			return;
		}
		HerobrineMod.LOGGER.info("nowhere for addexio to come from yet — he will try again");
	}

	public static void listen() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD
			.register(Company::hunted);
		ServerTickEvents.END_SERVER_TICK.register(Company::tick);

		// SHE STAYS WHERE YOU FELL.
		//
		// Asked for as a choice between hiding at the death site and running to
		// the player, and it is better as both in sequence: standing over the spot
		// is what a person does, and it puts her and your items in the same place,
		// so the trip back is one trip. The walk afterwards is only the guarantee
		// that she cannot be stranded.
		ServerLivingEntityEvents.AFTER_DEATH.register((died, source) -> {
			if (!(died instanceof ServerPlayer fallen)
				|| !(fallen.level() instanceof ServerLevel here)) {
				return;
			}
			for (CompanionEntity her : hers(here, fallen)) {
				WAITING.put(her.getUUID(), here.getGameTime());
				her.getNavigation().stop();
				com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, fallen,
					com.bloomlet.herobrine.entity.Sayings.YOU_DIED);
				HerobrineMod.LOGGER.info("{} is standing over where {} fell at [{}, {}, {}]",
					her.getName().getString(), fallen.getName().getString(),
					her.getBlockX(), her.getBlockY(), her.getBlockZ());
			}
		});
	}

	/**
	 * TWO BOUNDED LOOKS, NOT ONE ENORMOUS ONE.
	 *
	 * The first version of this inflated an AABB to thirty million blocks and asked
	 * every level for every CompanionEntity in it, twice a second — which is a
	 * whole-level entity sweep to find at most one villager in a red coat.
	 *
	 * The two cases are genuinely different and neither needs that. A companion who
	 * is FOLLOWING is by definition next to her player, so sixty-four blocks round
	 * each player finds her. A companion who is WAITING is by definition not, and
	 * she is already in WAITING by UUID, so she can be fetched by name.
	 */
	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % LOOKS_EVERY != 0) {
			return;
		}
		firstLight(server);
		for (ServerLevel here : server.getAllLevels()) {
			for (ServerPlayer with : here.players()) {
				for (CompanionEntity her : hers(here, with)) {
					if (!WAITING.containsKey(her.getUUID())) {
						notice(here, her, with);
					}
				}
			}
		}
		// The ones standing over a death site, and the ones who have fallen out of
		// the world. Both are somewhere no player-centred scan reaches.
		for (java.util.UUID who : List.copyOf(WAITING.keySet())) {
			CompanionEntity her = null;
			ServerLevel where = null;
			for (ServerLevel here : server.getAllLevels()) {
				if (here.getEntity(who) instanceof CompanionEntity found) {
					her = found;
					where = here;
					break;
				}
			}
			if (her == null || where == null) {
				WAITING.remove(who);      // unloaded, or gone. she resumes following.
				continue;
			}
			Player with = her.companion();
			if (with != null) {
				settled(where, her, with);
			}
		}
		fetch(server);
		fish(server);
	}

	/** Further behind the person he is with than this, whatever he is doing, and he is brought up. Follow's own limit is forty. */
	private static final double FETCHES_FROM = 48.0;

	/**
	 * A man stuck in a fight he cannot leave — the melee goal outranks Follow — is
	 * a man the player walks out of loaded range of, and that is the end of him
	 * until they happen back. This runs from the server side every two seconds and
	 * does not care what his goals are doing. One flat pass over the level's
	 * entities, a class check each; there is only ever one of him.
	 */
	private static void fetch(MinecraftServer server) {
		for (ServerLevel here : server.getAllLevels()) {
			if (here.players().isEmpty()) {
				continue;
			}
			for (CompanionEntity her : here.getEntities(
					net.minecraft.world.level.entity.EntityTypeTest.forClass(CompanionEntity.class),
					h -> !h.isFallen() && h.companion() != null)) {
				Player with = her.companion();
				if (with != null && with.level() == here && her.distanceTo(with) > FETCHES_FROM) {
					her.comeUp(here, with);
				}
			}
		}
	}

	/**
	 * Anybody who has gone off the bottom of the world.
	 *
	 * She cannot die, so the void does not kill her — it holds her at two hearts
	 * and drops her for ever, which is the one way left to lose her permanently.
	 * Follow's own teleport would eventually catch it, but only after Falter has
	 * finished eating, and "eventually" is not good enough for a hole with no
	 * bottom.
	 */
	private static void fish(MinecraftServer server) {
		for (ServerLevel here : server.getAllLevels()) {
			for (ServerPlayer with : here.players()) {
				for (CompanionEntity her : hers(here, with)) {
					if (her.getY() >= here.getMinY() - 8) {
						continue;
					}
					her.snapTo(with.getX(), with.getY(), with.getZ(),
						her.getYRot(), her.getXRot());
					her.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					her.getNavigation().stop();
					HerobrineMod.LOGGER.warn("{} went under the world and was fetched back",
						her.getName().getString());
				}
			}
		}
	}

	/**
	 * Whether she is still keeping vigil, and whether it is time to stop.
	 *
	 * WAITING is deliberately NOT persisted across a restart. If the server goes
	 * down while she is standing over a death site, the honest outcome on the way
	 * back up is that she simply resumes following — the vigil is a moment, not a
	 * state worth carrying in a save file, and the failure mode of getting it
	 * wrong is a companion frozen in a field for ever with no way to explain why.
	 */
	/**
	 * A PLACE HAS BEEN FOUND, and he was there when it happened to somebody else.
	 * Called from Dwellings the tick a player arrives at a new site; whoever has him
	 * with them, within earshot, hears the story of that place — a couple of seconds
	 * after the ground has finished moving, and once.
	 */
	public static void placeFound(ServerLevel level, String place) {
		String[] story = switch (place) {
			case "TOWN" -> com.bloomlet.herobrine.entity.Sayings.FOUND_TOWN;
			case "TOWER" -> com.bloomlet.herobrine.entity.Sayings.FOUND_TOWER;
			case "GAOL" -> com.bloomlet.herobrine.entity.Sayings.FOUND_GAOL;
			case "CHURCH" -> com.bloomlet.herobrine.entity.Sayings.FOUND_CHURCH;
			case "THRESHOLD" -> com.bloomlet.herobrine.entity.Sayings.FOUND_THRESHOLD;
			default -> null;
		};
		if (story == null) {
			return;
		}
		for (ServerPlayer with : level.players()) {
			for (CompanionEntity her : hers(level, with)) {
				if (her.distanceTo(with) > 32.0) {
					continue;
				}
				com.bloomlet.herobrine.entity.Sayings.tell(level, her, with, story, 60);
				HerobrineMod.LOGGER.info("addexio tells {} about the {}", with.getName().getString(),
					place.toLowerCase());
				break;
			}
		}
	}

	/** His watch has just been posted at a place somebody is walking toward. Addexio, if he is with them, says so. */
	public static void watched(ServerLevel level, BlockPos site) {
		for (ServerPlayer with : level.players()) {
			if (with.distanceToSqr(site.getX() + 0.5, site.getY(), site.getZ() + 0.5) > 128.0 * 128.0) {
				continue;
			}
			for (CompanionEntity her : hers(level, with)) {
				if (her.distanceTo(with) > 32.0) {
					continue;
				}
				com.bloomlet.herobrine.entity.Sayings.tell(level, her, with,
					com.bloomlet.herobrine.entity.Sayings.WATCHED, 40);
				return;
			}
		}
	}

	private static boolean settled(ServerLevel here, CompanionEntity her, Player with) {
		Long since = WAITING.get(her.getUUID());
		if (since == null) {
			return false;
		}
		if (her.distanceTo(with) < 8.0) {
			// You came back. That is the whole point of her having stayed.
			WAITING.remove(her.getUUID());
			return false;
		}
		if (here.getGameTime() - since < WAITS_FOR) {
			her.getNavigation().stop();
			return true;
		}
		WAITING.remove(her.getUUID());
		com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
			com.bloomlet.herobrine.entity.Sayings.WALKED_TO_YOU);
		HerobrineMod.LOGGER.info("{} gave up waiting and is walking to {}",
			her.getName().getString(), with.getName().getString());
		return false;
	}

	/**
	 * What she remarks on, in priority order, at most one thing per call.
	 *
	 * Ordered rather than rolled, and the order is worth reading: a Gaunt in the
	 * dark in his world would otherwise fire three lines at once, and Sayings has
	 * a quiet timer precisely because that is what a talking hat sounds like.
	 * The tall one wins, because it is the only one of the three that is standing
	 * in front of you right now.
	 */
	private static void notice(ServerLevel here, CompanionEntity her, Player with) {
		if (her.isFallen() || her.isFaltering()) {
			return;               // she has other things on her mind. Falter talks.
		}
		AABB round = her.getBoundingBox().inflate(NOTICES);

		List<com.bloomlet.herobrine.entity.GauntEntity> tall = here.getEntitiesOfClass(
			com.bloomlet.herobrine.entity.GauntEntity.class, round,
			g -> g.isAlive() && her.hasLineOfSight(g));
		if (!tall.isEmpty()) {
			com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.GAUNT_SEEN);
			return;
		}
		if (here.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			// Every four minutes at most. It was every twenty seconds from a pool of
			// three, which is the same three sentences on a loop for the whole fight.
			com.bloomlet.herobrine.entity.Sayings.toldOfRarely(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.HIS_WORLD, 4800L);
			return;
		}
		// Dark, and underground or at night. Not "dark" alone — a player who
		// steps into a doorway has not entered the dark.
		if (here.getMaxLocalRawBrightness(her.blockPosition()) <= 3
			&& (her.getBlockY() < 50 || !here.isBrightOutside())) {
			com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.DARK);
		}
	}

	/**
	 * Her, near a player. Sixty-four out and five hundred DOWN.
	 *
	 * The vertical reach is not symmetry, it is the void. A cube of 64 misses a
	 * companion who stepped off the edge — free fall clears sixty-four blocks in
	 * about two seconds and this only looks twice a second, so she can be past the
	 * box before it is next opened. Five hundred covers the whole build height,
	 * and there is nothing else down there for the query to find.
	 */
	private static List<? extends CompanionEntity> hers(ServerLevel here, Player with) {
		// ONE PASS OVER THE LEVEL'S ENTITY LIST, NOT A 128 x 1024 x 128 BOX. That
		// box spans four thousand entity sections and was walked twice per player
		// every two seconds to find, at most, one man. getEntities with a class test
		// is a flat walk of every loaded entity with an instanceof each: a few
		// thousand cheap checks, no section walk. The reach is kept as the same plain
		// distances, so the answer does not change.
		return here.getEntities(
			net.minecraft.world.level.entity.EntityTypeTest.forClass(CompanionEntity.class),
			her -> !her.isFallen() && her.companion() == with
				&& Math.abs(her.getX() - with.getX()) <= 65.0
				&& Math.abs(her.getZ() - with.getZ()) <= 65.0
				&& Math.abs(her.getY() - with.getY()) <= 513.0);
	}
}
