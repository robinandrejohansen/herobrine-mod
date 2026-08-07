package com.bloomlet.herobrine.entity;

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

		Player nearest = this.level().getNearestPlayer(this, 48.0);
		if (nearest != null && isLookingAtMe(nearest)) {
			// Hold still while watched — moving would break the illusion that
			// he was always standing there.
			this.getNavigation().stop();
			if (++this.watchedTicks > 40) {
				this.discard();
			}
		} else {
			this.watchedTicks = 0;
		}
	}

	/**
	 * Vanilla's Enderman test: the further away he is, the tighter the angle
	 * has to be before it counts as being looked at, so distant glances do not
	 * trigger him. Requires unobstructed line of sight.
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
		return dot > 1.0 - 0.025 / distance && player.hasLineOfSight(this);
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
