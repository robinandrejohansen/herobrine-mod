package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.ConfinedPlacement;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
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

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Possession::onTick);
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

		Mob taken = candidates.get(level.getRandom().nextInt(candidates.size()));
		taken.setAttached(POSSESSED, true);
		// No idle noise. A cow that stares in silence is worse than one that
		// stares and then moos, which would break it instantly.
		taken.setSilent(true);
		taken.setPersistenceRequired();

		ManifestationDirector.noteLocation(taken.blockPosition());
		HerobrineMod.LOGGER.info("possessed a {} at [{}, {}, {}]",
			taken.getType().toShortString(),
			taken.blockPosition().getX(), taken.blockPosition().getY(),
			taken.blockPosition().getZ());
		return true;
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
				AABB close = player.getBoundingBox().inflate(STARE_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, close)) {
					if (isPossessed(mob)) {
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
