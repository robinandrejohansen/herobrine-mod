package com.bloomlet.herobrine.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * He does not fight you. He watches, and he is gone when you look.
 *
 * Deliberately NOT a Monster: Monster brings hostile targeting and melee
 * attack goals, which would turn him into an ordinary mob you can kill and
 * stop being afraid of. PathfinderMob gives navigation and goals with no
 * combat, so the only behaviour is the one that makes him unsettling.
 */
public class HerobrineEntity extends PathfinderMob {
	/** Ticks a player has held their gaze on him before he leaves. */
	private int watchedTicks;
	/** Ticks since he arrived. */
	private int age;

	/**
	 * He leaves on his own after this long, seen or not.
	 *
	 * A haunting is a moment. Left indefinitely he becomes scenery — you walk
	 * over, study him, and discover he does nothing, which is the end of being
	 * afraid of him. Better to be gone before the player is certain of what
	 * they saw.
	 */
	private static final int LIFETIME = 600;          // 30 seconds

	/** Get closer than this and he is simply not there any more. */
	private static final double TOO_CLOSE = 8.0;

	/**
	 * How precisely you must be looking at him for it to count.
	 *
	 * Vanilla's Enderman formula is 0.025/distance, which at 18 blocks is
	 * about 3 degrees — appropriate for a mob you are meant to aggro by
	 * accident, far too strict for one whose whole behaviour is reacting to
	 * being seen. This is roughly 7 degrees at stalking range and still
	 * tightens with distance, so a glance across a valley does not count.
	 */
	private static final double GAZE_TOLERANCE = 0.15;

	public HerobrineEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			// He needs to be aware of you from much further than he ever
			// approaches — the whole behaviour is about distance.
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new StalkPlayerGoal(this, 18.0, 32.0));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 64.0F));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}

		// Gone on his own, before he can become furniture.
		if (++this.age > LIFETIME) {
			this.vanish();
			return;
		}

		Player nearest = this.level().getNearestPlayer(this, 48.0);

		// You never get to reach him. Walking up to something that does not
		// react is how a threat becomes an exhibit.
		if (nearest != null && this.distanceTo(nearest) < TOO_CLOSE) {
			this.vanish();
			return;
		}

		if (nearest != null && isLookingAtMe(nearest)) {
			// Hold still while watched — moving would break the illusion that
			// he was always standing there.
			this.getNavigation().stop();
			if (++this.watchedTicks > 40) {
				this.vanish();
			}
		} else {
			this.watchedTicks = 0;
		}
	}

	/**
	 * Leaves in a puff rather than blinking out.
	 *
	 * discard() alone is instantaneous and reads as a bug — you are never sure
	 * whether he left or the game hiccuped. Smoke and a sound at the position
	 * he occupied make it deliberate.
	 *
	 * level().playSound rather than this.playSound: he is isSilent(), which
	 * suppresses entity-emitted sound, and that suppression is wanted
	 * everywhere except here.
	 */
	private void vanish() {
		if (this.level() instanceof ServerLevel server) {
			server.sendParticles(
				ParticleTypes.LARGE_SMOKE,
				this.getX(), this.getY() + 1.0, this.getZ(),
				30, 0.3, 0.7, 0.3, 0.02
			);
			server.playSound(
				null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 0.7F, 0.6F
			);
		}
		this.discard();
	}

	/**
	 * The further away he is, the tighter the angle has to be, so a distant
	 * glance does not count. Requires unobstructed line of sight — looking at
	 * a wall he happens to be behind is not seeing him.
	 */
	private boolean isLookingAtMe(Player player) {
		Vec3 view = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		);
		double distance = toMe.length();
		double dot = view.dot(toMe.normalize());
		return dot > 1.0 - GAZE_TOLERANCE / distance && player.hasLineOfSight(this);
	}

	/** Silent by design — no idle noise to give away where he is standing. */
	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return true;
	}
}
