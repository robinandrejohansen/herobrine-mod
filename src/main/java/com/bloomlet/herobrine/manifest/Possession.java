package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.ConfinedPlacement;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One of your animals is his.
 *
 * The behaviour IS the horror here, and it needs almost no code. A vanilla
 * animal is never still: it wanders, it grazes, it flees when you swing at it,
 * it looks away. One that stops dead, turns to face you and tracks you as you
 * walk around it is wrong in a way a player registers immediately, long before
 * they could say why. Nothing about it is hostile — that is what makes it
 * unbearable.
 *
 * Implemented without touching the goal system. Mob.goalSelector is protected
 * and reaching it needs a mixin; instead this runs at the END of the server
 * tick, after the goals have already decided what the animal wants, and simply
 * overrules them. Navigation is stopped and the head is aimed. Cheaper than a
 * mixin, and it cannot break when a mob has an unusual goal set.
 *
 * Deliberately non-lethal at this phase. It will not attack, and it never
 * stops being an ordinary cow you could kill if you wanted to — which is
 * itself unpleasant, because you have to decide whether to.
 */
public final class Possession {
	private Possession() {}

	public static final AttachmentType<Boolean> POSSESSED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possessed"), Codec.BOOL);

	/**
	 * Whose it is.
	 *
	 * A possessed animal belongs to exactly one player, and this is the single
	 * most important line in the file for more than one person on a server.
	 * Without it a mob standing between two players has its facing overwritten
	 * twice a tick and visibly flickers between them, its place in the ring
	 * oscillates between a ring around each, and the catch-up can fire twice
	 * and drag it toward whichever player happened to be iterated last.
	 *
	 * It is also the right design and not merely the fix. DESIGN.md's
	 * multiplayer rule is that the seal is shared but the ATTENTION is
	 * personal, and nothing carries that better than this: your animals follow
	 * you and walk straight past your friend as though they were furniture. To
	 * them it looks like a cow that has singled you out, which is worse for
	 * both of you than a cow that hates everybody.
	 */
	public static final AttachmentType<String> OWNER =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possessed_by"), Codec.STRING);

	/**
	 * Whether it has stopped pretending.
	 *
	 * The eyes are NOT on from the moment he takes something, and that
	 * ordering is the whole design. The first encounter has to be deniable: a
	 * cow that has stopped moving is a cow that has stopped moving, and the
	 * player's first thought should be that their game is stuttering. If it
	 * glowed straight away there would be nothing to doubt, and a horror the
	 * player never doubted is a horror they were only ever told about.
	 *
	 * It reveals once the animal has FOLLOWED them — which is the moment the
	 * innocent explanations run out. A cow standing still is a glitch. A cow
	 * that was in the field when you left and is outside your door at dusk is
	 * not, and that is the right moment to confirm it, because the player has
	 * already arrived at the answer and the eyes only agree with them.
	 *
	 * Synced, because the client has to draw it.
	 */
	public static final AttachmentType<Boolean> REVEALED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possessed_revealed"), Codec.BOOL);

	/**
	 * How far past pretending it is. 0 stalking, 1 locked on, 2 hunting.
	 *
	 * The eyes belong HERE and not on the reveal, and separating the two was
	 * the fix. A stalking animal has to look completely ordinary — that is the
	 * entire first act, an animal that has stopped moving and will not make a
	 * sound, unsettling precisely because there is nothing to point at. Marking
	 * it answers the question the player is supposed to be sitting with.
	 *
	 * The eyes are what it wears once it has turned on them, which makes them
	 * information rather than decoration: they mean this one is coming for you
	 * now, and they only ever appear at the moment that becomes true.
	 *
	 * Driven by the world's phase rather than by anything the individual animal
	 * did, so it arrives as one event. Every one of his in the world lights up
	 * on the same tick, which is a far better moment than each of them turning
	 * privately at its own pace.
	 *
	 * Synced, because the client draws from it.
	 */
	public static final AttachmentType<Integer> MENACE = AttachmentRegistry
		.<Integer>builder()
		.persistent(Codec.INT)
		.syncWith(ByteBufCodecs.VAR_INT.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("possessed_menace"));

	/**
	 * How long it has spent coming after them.
	 *
	 * Counted in sweeps rather than ticks, and only while it is actually
	 * walking — an animal standing in a ring around a stationary player never
	 * accrues any, so somebody who parks next to a possessed cow and stares at
	 * it does not get the answer for free. You have to walk away from it.
	 */
	public static final AttachmentType<Integer> PURSUIT =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possessed_pursuit"), Codec.INT);

	/**
	 * The game time until which this one has lost interest.
	 *
	 * A flock that all sets off together, walks at one speed and stops in one
	 * ring is obviously ONE thing wearing four animals. Real pursuit is ragged:
	 * some come immediately, one hangs back, a couple give up halfway and go
	 * back to grazing as though nothing happened, and then later they are
	 * coming again.
	 *
	 * While lulled a mob is released entirely — no held navigation, no staring,
	 * nothing. Its own goals run and it behaves like the animal it is pretending
	 * to be. That is the whole value: the horror is not the following, it is
	 * that the following STOPS and then resumes, because an animal that goes
	 * back to eating grass is one the player has to talk themselves out of all
	 * over again.
	 */
	public static final AttachmentType<Long> LULL =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possessed_lull"), Codec.LONG);

	/**
	 * How far he will reach for something to take.
	 *
	 * Generous on purpose. Animals spawn in scattered herds rather than evenly,
	 * so in a forest the nearest cow is routinely thirty blocks off through the
	 * trees — a tighter radius made this fail over and over in ordinary terrain
	 * while looking like a bug.
	 */
	private static final double SEARCH_RADIUS = 48.0;

	/** Beyond this, walking will not do — it is brought closer instead. */
	private static final double CATCHUP_RADIUS = 44.0;
	/** How far out we look for followers that need moving. */
	private static final double SWEEP_RADIUS = 110.0;
	/** Deliberately slower than a walking player. It never has to be fast. */
	private static final double FOLLOW_SPEED = 0.55;
	/** Catch-up runs on this cadence, not every tick. */
	private static final int SWEEP_INTERVAL = 40;

	/** How far the news of a killing travels. */
	private static final double WITNESS_RADIUS = 20.0;
	/** How long the rest of them stare afterwards. Six seconds is plenty. */
	private static final int WITNESS_TICKS = 120;

	/** How many of the watchers he takes each time you put one down. */
	private static final int SPREAD_PER_KILL = 2;
	/** Ceiling on how many can be his at once near you. Sanity, not design. */
	private static final int LIVE_CAP = 16;
	/** Roughly a one-in-twenty-five chance per sweep, before temperament. */
	private static final int LULL_ODDS = 25;
	private static final int LULL_MIN_TICKS = 300;    // fifteen seconds
	private static final int LULL_MAX_TICKS = 900;    // forty-five

	/** How fast one comes at you once it is hunting. Ordinary animals are 1.0. */
	private static final double HUNT_SPEED = 1.45;
	/** Close enough to hit. */
	private static final double STRIKE_RANGE = 2.2;
	private static final int STRIKE_COOLDOWN = 20;
	private static final float STRIKE_DAMAGE = 3.0F;

	/** Sweeps of real pursuit before it stops pretending. Roughly half a minute. */
	private static final int PURSUIT_TO_REVEAL = 14;
	/** How many you may put down before it stops spreading. */
	private static final int TOLL_LIMIT = 100;
	/** Where they stand once they have caught you up. */
	private static final double RING_RADIUS = 9.0;
	/** Close enough to its place in the ring to stop walking. */
	private static final double SLOT_TOLERANCE = 3.0;
	/** Walk right up to one and it holds its ground rather than backing off. */
	private static final double HOLD_RADIUS = 7.0;

	/** How many of his you have killed. World-wide, and it does not reset. */
	public static final AttachmentType<Integer> TOLL =
		AttachmentRegistry.createPersistent(HerobrineMod.id("possession_toll"), Codec.INT);

	private static int tickCounter;

	/**
	 * Animals that saw you do it, and which of you they saw.
	 *
	 * Deliberately NOT an attachment. This is a few seconds of shock, not a
	 * property of the animal, and it should not survive a world reload — a cow
	 * still frozen in horror the next morning is a bug, not a scare.
	 *
	 * The killer is recorded because they all turn to face THAT player. A
	 * bystander watching a herd swing round to stare at their friend gets the
	 * better half of this: they can see exactly who it is about, and it is not
	 * them yet.
	 */
	private record Witness(long until, UUID sawWhom) {}

	private static final Map<UUID, Witness> witnesses = new HashMap<>();

	/** When each hunting mob last landed a blow. Transient by design. */
	private static final Map<UUID, Long> lastStruck = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Possession::onTick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level
				&& entity instanceof Mob mob && isPossessed(mob)) {
				onKilled(level, mob, source.getEntity());
			}
		});
		// A possessed villager will not deal with you. Nothing happens at all
		// when you try — no screen, no sound, no refusal animation. Silence is
		// the whole point: you are left to decide whether the game is broken
		// or whether this one is not a villager any more.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof Mob mob && isPossessed(mob)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	public static int toll(ServerLevel level) {
		Integer n = level.getServer().overworld().getAttached(TOLL);
		return n == null ? 0 : n;
	}

	public static int tollLimit() {
		return TOLL_LIMIT;
	}

	/**
	 * Whether he is allowed to take this at all.
	 *
	 * Tamed animals are excluded, and that is a design ruling rather than a
	 * technical one. A wolf you tamed, named and walked halfway across a world
	 * with is the single most loaded thing he could take, and taking it would
	 * be the most effective scare in the mod — which is exactly why it is out.
	 * DESIGN.md §9 says he never touches what the player has built or earned,
	 * because a haunting you cannot opt out of stops being a horror and starts
	 * being griefing, and the player has no counter-play against losing a pet.
	 *
	 * A WILD wolf is fair game, and one of his cannot then be tamed: feeding it
	 * bones does nothing at all, because interaction with anything of his is
	 * refused outright. So the dog that will not become your dog is available,
	 * and the dog that already is yours is safe. That is the right way round.
	 */
	private static boolean eligible(Mob mob) {
		if (!(mob instanceof Animal) && !(mob instanceof AbstractVillager)) {
			return false;   // only things that ought to be harmless
		}
		if (mob.isBaby()) {
			return false;   // a possessed lamb is comic, not frightening
		}
		return !(mob instanceof TamableAnimal tame) || !tame.isTame();
	}

	/** Has this one gone back to being an animal for a while? */
	private static boolean lulled(ServerLevel level, Mob mob) {
		Long until = mob.getAttached(LULL);
		return until != null && level.getGameTime() < until;
	}

	private static void wake(Mob mob) {
		if (mob.getAttached(LULL) != null) {
			mob.setAttached(LULL, 0L);
		mob.setAttached(MENACE, 0);
		}
	}

	/**
	 * It might just stop bothering.
	 *
	 * Never once it has revealed itself. Losing interest is part of the
	 * pretence — an animal that wanders off to graze is one the player can
	 * still explain — and something that has already shown them what it is has
	 * nothing left to pretend with. After that it simply keeps coming, which is
	 * the escalation the reveal is for.
	 *
	 * How likely it is comes from the mob's own id, so it is a fixed trait
	 * rather than a dice roll: the same sheep is always the flighty one and the
	 * same one is always the one that never stops. A flock where every member
	 * behaves identically on average is still a flock that behaves identically,
	 * and the point of this is that the player starts recognising individuals.
	 */
	private static void maybeLose(ServerLevel level, Mob mob) {
		if (isRevealed(mob)) {
			return;
		}
		int resolve = Math.floorMod(mob.getUUID().hashCode() >> 8, 4);   // 0 flighty, 3 dogged
		if (level.getRandom().nextInt(LULL_ODDS + resolve * 20) != 0) {
			return;
		}
		int length = LULL_MIN_TICKS
			+ level.getRandom().nextInt(LULL_MAX_TICKS - LULL_MIN_TICKS + 1);
		mob.setAttached(LULL, level.getGameTime() + length);
	}

	/** 0 stalking, 1 locked on, 2 hunting. */
	public static int menace(Mob mob) {
		Integer level = mob.getAttached(MENACE);
		return level == null ? 0 : level;
	}

	/**
	 * What the world's phase says they should be by now.
	 *
	 * HUNTER is where the whole mod stops being about doubt, so it is where
	 * they stop pretending to be animals. SIEGE turns them red, which is the
	 * only red in the mod: white is his, and an animal wearing his eyes reads
	 * as "he is in there" where red reads as "this is going to hurt you". The
	 * player has to be able to tell those apart across a field, at a glance,
	 * while running.
	 */
	private static int menaceFor(Phase phase) {
		if (phase.atLeast(Phase.SIEGE)) {
			return 2;
		}
		return phase.atLeast(Phase.HUNTER) ? 1 : 0;
	}

	/** True once it has followed you far enough to stop pretending. */
	public static boolean isRevealed(Mob mob) {
		return Boolean.TRUE.equals(mob.getAttached(REVEALED));
	}

	/**
	 * One step closer to the player knowing.
	 *
	 * @param outright true when it has just crossed real ground to reach them,
	 *                 which is worth the whole counter on its own
	 */
	private static void pursued(Mob mob, boolean outright) {
		if (isRevealed(mob)) {
			return;
		}
		int so_far = (mob.getAttached(PURSUIT) == null ? 0 : mob.getAttached(PURSUIT)) + 1;
		mob.setAttached(PURSUIT, so_far);
		if (!outright && so_far < PURSUIT_TO_REVEAL) {
			return;
		}
		mob.setAttached(REVEALED, true);
		HerobrineMod.LOGGER.info("a possessed {} stopped pretending at [{}, {}, {}]",
			mob.getType().toShortString(), mob.blockPosition().getX(),
			mob.blockPosition().getY(), mob.blockPosition().getZ());
	}

	public static boolean isPossessed(Mob mob) {
		return Boolean.TRUE.equals(mob.getAttached(POSSESSED));
	}

	/** Marks a mob as his. Silent, and it will not wander off or despawn. */
	public static boolean take(ServerLevel level, ServerPlayer player) {
		List<Mob> candidates = new ArrayList<>();
		AABB around = player.getBoundingBox().inflate(SEARCH_RADIUS);
		Vec3 look = player.getViewVector(1.0F).normalize();

		// Counted rather than merely skipped, so a refusal can say which of
		// these it was. They are wildly different problems — "there are no
		// animals here" is terrain, "they are all in front of you" is the
		// player, and "they are all already his" is the mod working — and a
		// single "wrong surroundings" for all three is useless to a tester.
		int harmless = 0;
		int tooClose = 0;
		int watched = 0;
		int alreadyHis = 0;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
			if (!eligible(mob)) {
				continue;
			}
			harmless++;
			if (isPossessed(mob)) {
				alreadyHis++;
				continue;
			}
			if (mob.distanceTo(player) < 6.0) {
				tooClose++;
				continue;
			}
			// Never taken while you are watching it happen — an animal that
			// visibly stops mid-step is a bug; one you turn back to is not.
			Vec3 toMob = mob.position().subtract(player.position()).normalize();
			if (look.dot(toMob) > 0.2) {
				watched++;
				continue;
			}
			candidates.add(mob);
		}
		if (candidates.isEmpty()) {
			ManifestationDirector.refused(whyNot(harmless, tooClose, watched, alreadyHis));
			return false;
		}

		// How many he takes at once. One animal following you home is a
		// haunting; four standing in a line outside your door in the morning
		// is a statement, and it should only be available once he is past
		// hinting. They need no herding logic — they all follow you, so they
		// arrive together and stop at the same distance on their own.
		int wanted = takeCount(Wrath.phase(level.getServer()));
		java.util.Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));

		int took = 0;
		for (Mob taken : candidates) {
			if (took >= wanted) {
				break;
			}
			// No idle noise. A cow that stares in silence is worse than one
			// that stares and then moos, which would break it instantly.
			claim(taken, player);
			if (took == 0) {
				ManifestationDirector.noteLocation(taken.blockPosition());
			}
			HerobrineMod.LOGGER.info("possessed a {} at [{}, {}, {}]",
				taken.getType().toShortString(),
				taken.blockPosition().getX(), taken.blockPosition().getY(),
				taken.blockPosition().getZ());
			took++;
		}
		return took > 0;
	}

	private static String whyNot(int harmless, int tooClose, int watched, int alreadyHis) {
		if (harmless == 0) {
			return "no animals or villagers within " + (int)SEARCH_RADIUS + " blocks";
		}
		if (watched > 0 && tooClose == 0 && alreadyHis == 0) {
			return watched + " nearby, but all in front of you — look away and try again";
		}
		if (alreadyHis == harmless) {
			return "all " + harmless + " nearby are already his";
		}
		return harmless + " nearby: " + watched + " in your view, " + tooClose
			+ " too close, " + alreadyHis + " already his";
	}

	private static int takeCount(Phase phase) {
		if (phase.atLeast(Phase.SIEGE)) {
			return 4;
		}
		if (phase.atLeast(Phase.HUNTER)) {
			return 2;
		}
		return 1;
	}

	/**
	 * You put one down, and the rest of them know.
	 *
	 * This is the answer to the only move the player has here. Killing a
	 * follower already costs wrath, but a number in a command is not a
	 * consequence — every animal in earshot stopping and turning to face you,
	 * in silence, is. They do not follow and they never attack. They just saw.
	 *
	 * And what it leaves behind says what it was. A cow that drops a bone and
	 * rotten flesh was not a cow you killed; it was something already dead that
	 * had been walking. That is the only place in the mod the player is told
	 * outright, and it is told in loot rather than in words.
	 */
	private static void onKilled(ServerLevel level, Mob mob, net.minecraft.world.entity.Entity killer) {
		drop(level, mob, new ItemStack(Items.ROTTEN_FLESH, 1 + level.getRandom().nextInt(2)));
		drop(level, mob, new ItemStack(Items.BONE));

		if (!(killer instanceof ServerPlayer player)) {
			return;
		}

		ServerLevel overworld = level.getServer().overworld();
		int killed = toll(level) + 1;
		overworld.setAttached(TOLL, killed);

		List<Mob> watchers = new ArrayList<>();
		int live = 0;
		long until = level.getGameTime() + WITNESS_TICKS;
		for (Mob other : level.getEntitiesOfClass(
				Mob.class, mob.getBoundingBox().inflate(WITNESS_RADIUS))) {
			if (other == mob) {
				continue;
			}
			if (isPossessed(other)) {
				live++;
				continue;
			}
			if (other instanceof Animal || other instanceof AbstractVillager) {
				// Your dog watches it happen like everything else does. It
				// simply never becomes one of them.
				witnesses.put(other.getUUID(), new Witness(until, player.getUUID()));
				if (eligible(other)) {
					watchers.add(other);
				}
			}
		}

		// It spreads through the watchers. The ones that stopped to look at
		// what you did are the ones he takes, which is the whole point — the
		// player sees a field of animals turn, kills the one that was already
		// his, and only later works out that two of the watchers never
		// stopped watching.
		//
		// This is what makes the herd build itself. One possession is a
		// haunting you can end with a sword; two-for-one means ending it is
		// how it grows, and the player's own most obvious move is the engine.
		if (killed >= TOLL_LIMIT || live >= LIVE_CAP) {
			return;   // he has taken enough here
		}
		// The two new ones belong to whoever swung, not to whoever the dead one
		// belonged to. On a server that makes helping a friend cull their herd
		// the way you inherit it — which is the correct price, and the same one
		// wrath already charges, since the defiance for the kill goes to the
		// killer too. There is no way to take this on for someone else without
		// taking it on.
		int room = Math.min(SPREAD_PER_KILL, LIVE_CAP - live);
		java.util.Collections.shuffle(watchers, new java.util.Random(level.getRandom().nextLong()));
		for (int i = 0; i < room && i < watchers.size(); i++) {
			claim(watchers.get(i), player);
		}
	}

	private static void claim(Mob mob, ServerPlayer owner) {
		mob.setAttached(POSSESSED, true);
		mob.setAttached(REVEALED, false);
		mob.setAttached(PURSUIT, 0);
		mob.setAttached(LULL, 0L);
		mob.setAttached(MENACE, 0);
		mob.setAttached(OWNER, owner.getUUID().toString());
		mob.setSilent(true);
		mob.setPersistenceRequired();
	}

	/** @return the player this one belongs to, if they are here to be followed */
	private static @org.jetbrains.annotations.Nullable ServerPlayer ownerOf(
			ServerLevel level, Mob mob) {
		String id = mob.getAttached(OWNER);
		if (id == null) {
			return null;
		}
		ServerPlayer owner;
		try {
			owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(id));
		} catch (IllegalArgumentException malformed) {
			return null;
		}
		// Another dimension counts as gone. It has no way to follow them there
		// and should not be twitching toward a player who is not in this world.
		return owner != null && owner.level() == level ? owner : null;
	}

	private static void drop(ServerLevel level, Mob mob, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), stack);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
	}

	/**
	 * Runs after the goals, and overrules them.
	 *
	 * Two passes at different rates. The close one is per-tick because staring
	 * has to look smooth; the sweep that keeps distant followers moving runs
	 * every two seconds, because a search over a hundred blocks of entities is
	 * not something to do twenty times a second.
	 */
	private static void onTick(MinecraftServer server) {
		boolean sweep = ++tickCounter % SWEEP_INTERVAL == 0;

		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				AABB close = player.getBoundingBox().inflate(WITNESS_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, close)) {
					if (isPossessed(mob)) {
						ServerPlayer owner = ownerOf(level, mob);
						if (owner == null) {
							// Whoever it belongs to has logged off or left this
							// world. It does not transfer and it does not go
							// back to being an animal — it simply stops where
							// it is. Someone else logging in finds silent,
							// motionless cattle standing in a field, which is
							// the most unsettling thing in this file and costs
							// nothing to produce.
							stop(mob);
						} else if (owner == player && menace(mob) > 0) {
							// Head locked on, every tick, no matter where it
							// is or what is in the way. A stalking one looks
							// away sometimes; this one never does, and that
							// alone is legible from across a field.
							lockOn(mob, player);
							strike(level, mob, player);
						} else if (owner == player && !lulled(level, mob)
							&& settled(mob, player)) {
							hold(mob, player, knowsWhereYouAre(mob, player));
						}
					} else {
						ServerPlayer saw = sawIt(level, mob);
						if (saw == player) {
							hold(mob, player, mob.hasLineOfSight(player));
						}
					}
				}
				if (!sweep) {
					continue;
				}
				AABB far = player.getBoundingBox().inflate(SWEEP_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, far)) {
					// Only its owner moves it. Anyone else it is standing near
					// is scenery as far as it is concerned.
					if (!isPossessed(mob) || ownerOf(level, mob) != player) {
						continue;
					}

					// The world's phase decides, so they all turn together
					// rather than each at its own private pace.
					int should = menaceFor(Wrath.phase(server));
					if (menace(mob) != should) {
						mob.setAttached(MENACE, should);
					}
					if (should > 0) {
						// No ring, no lull, no keeping its distance. It comes.
						mob.getNavigation().moveTo(player, HUNT_SPEED);
						continue;
					}
					if (mob.distanceTo(player) > CATCHUP_RADIUS) {
						// Distance overrules everything, including having lost
						// interest. This is what keeps the guarantee: whatever
						// any individual is doing, none of them is ever
						// actually left behind, so a player who settles
						// somewhere is eventually joined by all of them.
						wake(mob);
						catchUp(level, mob, player);
						continue;
					}
					if (settled(mob, player) || lulled(level, mob)) {
						continue;
					}
					follow(mob, player);
					maybeLose(level, mob);
				}
			}
		}
	}

	/**
	 * It walks after you. Slowly, and it never gives up.
	 *
	 * Slower than a walking player on purpose. It is not supposed to catch you
	 * in the open — it is supposed to be there later, which is worse.
	 */
	private static void follow(Mob mob, ServerPlayer player) {
		disarm(mob);
		pursued(mob, false);
		Vec3 slot = slotOf(mob, player);
		mob.getNavigation().moveTo(slot.x, slot.y, slot.z, FOLLOW_SPEED);
	}

	/**
	 * Where this one stands, once it has caught you up.
	 *
	 * Each takes a fixed bearing derived from its own id, so they arrive on
	 * different sides and end up RINGING wherever you spend your time rather
	 * than piling up on whichever side they happened to come from. Nothing
	 * coordinates them — they have never heard of each other. The ring is what
	 * you get for free when a dozen things independently want to stand a fixed
	 * distance from you, and it is why walking out of your door at dawn to
	 * find them spread around the treeline looks deliberate.
	 *
	 * The bearing is fixed rather than random so a given animal keeps its
	 * place as you move around. One that reshuffled every two seconds would
	 * read as confused, and confusion is not frightening.
	 */
	private static Vec3 slotOf(Mob mob, ServerPlayer player) {
		double angle = (mob.getUUID().hashCode() & 0xFFFF) / 65536.0 * Math.PI * 2.0;
		return new Vec3(
			player.getX() + Math.cos(angle) * RING_RADIUS,
			player.getY(),
			player.getZ() + Math.sin(angle) * RING_RADIUS);
	}

	/**
	 * True when it should stop walking and simply look at you.
	 *
	 * Either it has reached its place in the ring, or you have walked right up
	 * to it — the second case matters, because an animal that backed away to
	 * keep its distance would read as evasive rather than possessed. It has no
	 * reason to avoid you. It stands there and lets you do whatever you came
	 * to do.
	 */
	private static boolean settled(Mob mob, ServerPlayer player) {
		if (mob.distanceTo(player) <= HOLD_RADIUS) {
			return true;
		}
		Vec3 slot = slotOf(mob, player);
		double dx = mob.getX() - slot.x;
		double dz = mob.getZ() - slot.z;
		return Math.sqrt(dx * dx + dz * dz) <= SLOT_TOLERANCE;
	}

	/**
	 * When walking will not do, it is simply closer.
	 *
	 * This exists because of a hard limit rather than for effect: chunks that
	 * are not loaded do not tick, so an animal left behind while you sprint
	 * home would freeze in place forever and never arrive. Bringing it closer
	 * WHILE IT IS STILL LOADED keeps it inside your loaded area, so it can
	 * keep following you across the world instead of stalling the moment you
	 * outrun it.
	 *
	 * Placed out of your view and well back, never near enough to read as an
	 * ambush. You are meant to notice it later, at the treeline, having
	 * assumed you had left it behind.
	 */
	private static void catchUp(ServerLevel level, Mob mob, ServerPlayer player) {
		// It has just crossed ground to reach them. Nothing the player can
		// tell themselves survives that, so this alone is enough.
		pursued(mob, true);
		// Underground, the surface heightmap is worse than useless — it would
		// drop the animal on the mountainside above your tunnel, where it can
		// never reach you. Same flood-fill the entity itself uses, so it
		// arrives in YOUR passage, behind you.
		if (ConfinedPlacement.isConfined(level, player)) {
			BlockPos found = ConfinedPlacement.find(level, player);
			if (found != null) {
				mob.snapTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5,
					mob.getYRot(), 0.0F);
			}
			return;
		}

		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 slot = slotOf(mob, player);
		double bearing = Math.atan2(slot.z - player.getZ(), slot.x - player.getX());

		for (int attempt = 0; attempt < 10; attempt++) {
			// Near its own bearing, so a flock arrives spread around you
			// rather than stacked in one place. Widened a little each try so
			// a blocked side does not strand it out of range forever.
			double angle = bearing + (level.getRandom().nextDouble() - 0.5) * (0.4 + attempt * 0.3);
			double range = 22.0 + level.getRandom().nextDouble() * 12.0;
			int x = (int)(player.getX() + Math.cos(angle) * range);
			int z = (int)(player.getZ() + Math.sin(angle) * range);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);

			if (!level.isLoaded(pos) || !ConfinedPlacement.canStand(level, pos)) {
				continue;
			}
			Vec3 toPos = new Vec3(
				pos.getX() + 0.5 - player.getX(),
				pos.getY() - player.getEyeY(),
				pos.getZ() + 0.5 - player.getZ()).normalize();
			if (look.dot(toPos) > 0.2) {
				continue;   // never watched arriving
			}

			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mob.getYRot(), 0.0F);
			return;
		}
	}

	/**
	 * @return the player an ordinary animal is still frozen watching, or null
	 *         if it saw nothing or has got over it
	 */
	private static @org.jetbrains.annotations.Nullable ServerPlayer sawIt(
			ServerLevel level, Mob mob) {
		Witness witness = witnesses.get(mob.getUUID());
		if (witness == null) {
			return null;
		}
		if (level.getGameTime() >= witness.until()) {
			witnesses.remove(mob.getUUID());
			return null;
		}
		return level.getServer().getPlayerList().getPlayer(witness.sawWhom());
	}

	/**
	 * The head never leaves you.
	 *
	 * Deliberately not a stare — it does not stop to look. It keeps running and
	 * its head stays pointed at the player the whole way, which is what
	 * separates this from an angry cow: an angry cow looks where it is going.
	 * Body yaw is left to pathfinding so it still turns corners like a real
	 * thing.
	 */
	private static void lockOn(Mob mob, ServerPlayer player) {
		mob.getLookControl().setLookAt(player, 90.0F, 90.0F);
		float yaw = (float)(Math.atan2(
			player.getZ() - mob.getZ(), player.getX() - mob.getX()) * (180.0 / Math.PI)) - 90.0F;
		mob.yHeadRot = yaw;
	}

	/**
	 * It can actually hurt you now.
	 *
	 * Dealt directly rather than through the goal system, because the things he
	 * takes are animals and villagers and none of them own an attack goal — a
	 * cow has no way to hit anybody, which is exactly why one that does is
	 * wrong. Modest per blow and on a cooldown: the danger is that there are
	 * four of them and they do not stop, not that any one of them is deadly.
	 *
	 * They stay ordinary mobs with ordinary health throughout. The player can
	 * kill them, and at this phase they very much should — which is the arc the
	 * whole mod is for.
	 */
	private static void strike(ServerLevel level, Mob mob, ServerPlayer player) {
		if (mob.distanceTo(player) > STRIKE_RANGE) {
			return;
		}
		long now = level.getGameTime();
		Long last = lastStruck.get(mob.getUUID());
		if (last != null && now - last < STRIKE_COOLDOWN) {
			return;
		}
		lastStruck.put(mob.getUUID(), now);
		player.hurtServer(level, level.damageSources().mobAttack(mob), STRIKE_DAMAGE);
	}

	/** Undo whatever the goals decided this tick. */
	private static void stop(Mob mob) {
		mob.getNavigation().stop();
		mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
		mob.setJumping(false);
		disarm(mob);
	}

	/**
	 * It does not fight back, ever, and that is the whole effect.
	 *
	 * Stopping navigation is not enough for anything that can be provoked. A
	 * wild wolf is neutral, so hitting one of his would have it turn on the
	 * player through the ordinary attack goal — the goals still choose a
	 * target even when they cannot walk anywhere, and melee does not need a
	 * path once you are stood next to it.
	 *
	 * A possessed animal that suddenly snarls and bites is just an angry wolf,
	 * and the player knows what to do with an angry wolf. One that lets you
	 * hit it and keeps looking at you is the thing they have no answer for.
	 */
	private static void disarm(Mob mob) {
		if (mob.getTarget() != null) {
			mob.setTarget(null);
		}
	}

	/**
	 * Does it know where you are when it cannot see you?
	 *
	 * Before it reveals, no. It only turns to face a player it has a clear
	 * line to, which keeps the first encounter arguable — an animal that
	 * tracked you through a hillside on day one would settle the question
	 * immediately, and the whole first act depends on the question staying
	 * open.
	 *
	 * After it reveals, yes, through anything. That is the escalation, and it
	 * costs nothing to add because the player has already been told what they
	 * are dealing with: it is not an animal using eyes. Walling one into a pit
	 * and coming back to find it facing you through three blocks of stone is
	 * the correct payoff for having worked out what it is.
	 */
	private static boolean knowsWhereYouAre(Mob mob, ServerPlayer player) {
		return isRevealed(mob) || mob.hasLineOfSight(player);
	}

	private static void hold(Mob mob, Player player, boolean facing) {
		stop(mob);
		if (!facing) {
			// Held in place, but not staring through a wall it has no business
			// seeing through. A stopped animal is still wrong; it just is not
			// yet impossible.
			return;
		}

		// Track the player with the head, and turn the body to match — a
		// creature facing you squarely reads as attention, where head-only
		// tracking reads as an idle animation.
		mob.getLookControl().setLookAt(player, 60.0F, 60.0F);
		float yaw = (float)(Math.atan2(
			player.getZ() - mob.getZ(), player.getX() - mob.getX()) * (180.0 / Math.PI)) - 90.0F;
		mob.setYRot(yaw);
		mob.setYBodyRot(yaw);
		mob.yHeadRot = yaw;
	}
}
