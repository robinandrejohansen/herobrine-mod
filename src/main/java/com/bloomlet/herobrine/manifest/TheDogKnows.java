package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.entity.HerobrineEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.phys.AABB;

/**
 * Your dog knows before you do.
 *
 * The oldest device in the genre and still the best one, because it is the
 * rare piece of horror that gives the player something rather than taking
 * something away. Everything else here works by removing what they relied on —
 * the torch goes out, the animal will not behave, the window does not hold.
 * The dog is the one thing that works the other way: a player who tamed a wolf
 * and kept it alive has bought a warning system, and it pays out at exactly
 * the moments the rest of the mod is at its worst.
 *
 * That is why pets are untakeable (see Possession.eligible). A dog that could
 * be turned would be a better single scare and a far worse mod: the reward for
 * keeping it would be that it eventually betrays you, which teaches the player
 * that investment is punished. This teaches the opposite.
 *
 * It growls at what the player cannot see yet — through walls, across a dark
 * treeline, up a tunnel. A dog barking at nothing is only frightening if the
 * player later finds out it was not nothing, so it is never wrong: if it
 * growls, something is there.
 */
public final class TheDogKnows {
	private TheDogKnows() {}

	/** How far a dog senses one of his animals. */
	private static final double SENSE_POSSESSED = 24.0;
	/**
	 * And how far it senses HIM. Wider, because he is the thing worth warning
	 * about and he keeps his distance — a radius that only covered what the
	 * player could already see would never once go off before they saw him.
	 */
	private static final double SENSE_HIM = 40.0;

	private static final double OWNER_RANGE = 32.0;
	/** Roughly every two seconds. Often enough to read as a warning. */
	private static final int GROWL_INTERVAL = 45;
	private static final int CHECK_INTERVAL = 5;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TheDogKnows::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				for (Wolf wolf : level.getEntitiesOfClass(
						Wolf.class, player.getBoundingBox().inflate(OWNER_RANGE))) {
					if (!wolf.isTame() || wolf.isBaby() || wolf.isOrderedToSit()) {
						continue;   // a sitting dog was told to stay
					}
					if (wolf.getOwner() != player) {
						continue;
					}
					Entity threat = nearestThreat(level, wolf);
					if (threat != null) {
						warn(level, wolf, threat);
					}
				}
			}
		}
	}

	/**
	 * What it has noticed.
	 *
	 * Him first regardless of distance, because a dog growling at a cow while
	 * Herobrine stands behind it would be actively misleading — and the whole
	 * value of this is that the direction it faces is trustworthy.
	 */
	private static @org.jspecify.annotations.Nullable Entity nearestThreat(
			ServerLevel level, Wolf wolf) {
		AABB wide = wolf.getBoundingBox().inflate(SENSE_HIM);
		for (HerobrineEntity him : level.getEntitiesOfClass(HerobrineEntity.class, wide)) {
			return him;
		}
		Mob closest = null;
		double best = SENSE_POSSESSED * SENSE_POSSESSED;
		for (Mob mob : level.getEntitiesOfClass(
				Mob.class, wolf.getBoundingBox().inflate(SENSE_POSSESSED))) {
			if (!Possession.isPossessed(mob)) {
				continue;
			}
			double distance = mob.distanceToSqr(wolf);
			if (distance < best) {
				best = distance;
				closest = mob;
			}
		}
		return closest;
	}

	/**
	 * It plants itself and faces the thing.
	 *
	 * Navigation is stopped so it will not follow the player away while the
	 * threat is there — the dog refusing to come is half the signal, and a
	 * player who has walked a wolf across a world will notice it digging in
	 * long before they notice a sound. It does NOT attack: one of his animals
	 * is the player's problem to decide about, and setting the dog on it would
	 * take that choice away and get the dog killed for nothing.
	 */
	private static void warn(ServerLevel level, Wolf wolf, Entity threat) {
		wolf.getNavigation().stop();
		wolf.getLookControl().setLookAt(threat, 60.0F, 60.0F);

		float yaw = (float)(Math.atan2(
			threat.getZ() - wolf.getZ(), threat.getX() - wolf.getX()) * (180.0 / Math.PI)) - 90.0F;
		wolf.setYRot(yaw);
		wolf.setYBodyRot(yaw);
		wolf.yHeadRot = yaw;

		if (level.getGameTime() % GROWL_INTERVAL == 0) {
			SoundEvent growl = growlOf(wolf);
			if (growl != null) {
				level.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
					growl, SoundSource.NEUTRAL, 1.0F, 0.8F);
			}
		}
	}

	/**
	 * This wolf's own growl.
	 *
	 * Wolf sounds became data-driven per variant, so there is no single
	 * SoundEvents.WOLF_GROWL to reach for any more — a sad wolf and an angry
	 * wolf growl differently, and using one hardcoded sound for all of them
	 * would make somebody's dog sound like a different animal at the worst
	 * possible moment.
	 */
	private static @org.jspecify.annotations.Nullable SoundEvent growlOf(Wolf wolf) {
		Holder<WolfSoundVariant> variant = wolf.get(DataComponents.WOLF_SOUND_VARIANT);
		if (variant == null) {
			return null;
		}
		WolfSoundVariant sounds = variant.value();
		return (wolf.isBaby() ? sounds.babySounds() : sounds.adultSounds()).growlSound().value();
	}
}
