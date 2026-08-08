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
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
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

	private static final double SEARCH_RADIUS = 24.0;

	/** Inside this it stops and stares. Outside it walks. */
	private static final double STARE_RADIUS = 12.0;
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

	private static int tickCounter;

	/**
	 * Animals that saw you do it.
	 *
	 * Deliberately NOT an attachment. This is a few seconds of shock, not a
	 * property of the animal, and it should not survive a world reload — a cow
	 * still frozen in horror the next morning is a bug, not a scare.
	 */
	private static final Map<UUID, Long> witnesses = new HashMap<>();

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

	public static boolean isPossessed(Mob mob) {
		return Boolean.TRUE.equals(mob.getAttached(POSSESSED));
	}

	/** Marks a mob as his. Silent, and it will not wander off or despawn. */
	public static boolean take(ServerLevel level, ServerPlayer player) {
		List<Mob> candidates = new ArrayList<>();
		AABB around = player.getBoundingBox().inflate(SEARCH_RADIUS);
		Vec3 look = player.getViewVector(1.0F).normalize();

		for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
			if (!(mob instanceof Animal) && !(mob instanceof AbstractVillager)) {
				continue;   // only things that ought to be harmless
			}
			if (isPossessed(mob) || mob.isBaby()) {
				continue;   // a possessed lamb is comic, not frightening
			}
			double distance = mob.distanceTo(player);
			if (distance < 6.0) {
				continue;
			}
			// Never taken while you are watching it happen — an animal that
			// visibly stops mid-step is a bug; one you turn back to is not.
			Vec3 toMob = mob.position().subtract(player.position()).normalize();
			if (look.dot(toMob) > 0.2) {
				continue;
			}
			candidates.add(mob);
		}
		if (candidates.isEmpty()) {
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
			taken.setAttached(POSSESSED, true);
			// No idle noise. A cow that stares in silence is worse than one
			// that stares and then moos, which would break it instantly.
			taken.setSilent(true);
			taken.setPersistenceRequired();
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
		long until = level.getGameTime() + WITNESS_TICKS;
		for (Mob other : level.getEntitiesOfClass(
				Mob.class, mob.getBoundingBox().inflate(WITNESS_RADIUS))) {
			if (other == mob || isPossessed(other)) {
				continue;
			}
			if (other instanceof Animal || other instanceof AbstractVillager) {
				witnesses.put(other.getUUID(), until);
			}
		}
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
						if (mob.distanceTo(player) <= STARE_RADIUS) {
							hold(mob, player);
						}
					} else if (sawIt(level, mob)) {
						hold(mob, player);
					}
				}
				if (!sweep) {
					continue;
				}
				AABB far = player.getBoundingBox().inflate(SWEEP_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, far)) {
					if (!isPossessed(mob)) {
						continue;
					}
					double distance = mob.distanceTo(player);
					if (distance <= STARE_RADIUS) {
						continue;   // handled per-tick above
					}
					if (distance > CATCHUP_RADIUS) {
						catchUp(level, mob, player);
					} else {
						follow(mob, player);
					}
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
		mob.getNavigation().moveTo(player, FOLLOW_SPEED);
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

		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
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

	/** True while an ordinary animal is still frozen by what it just watched. */
	private static boolean sawIt(ServerLevel level, Mob mob) {
		Long until = witnesses.get(mob.getUUID());
		if (until == null) {
			return false;
		}
		if (level.getGameTime() >= until) {
			witnesses.remove(mob.getUUID());
			return false;
		}
		return true;
	}

	private static void hold(Mob mob, Player player) {
		// Undo whatever the goals decided this tick.
		mob.getNavigation().stop();
		mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
		mob.setJumping(false);

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
