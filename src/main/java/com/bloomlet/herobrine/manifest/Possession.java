package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
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
	/** Beyond this he loses interest and the animal is simply an animal again. */
	private static final double ATTENTION_RADIUS = 32.0;

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
	 * Scoped to mobs near players rather than every entity in the world: the
	 * effect only exists when someone is there to see it, so there is no
	 * reason to pay for it anywhere else.
	 */
	private static void onTick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				AABB around = player.getBoundingBox().inflate(ATTENTION_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
					if (isPossessed(mob)) {
						hold(mob, player);
					}
				}
			}
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
