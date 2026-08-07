package com.bloomlet.herobrine.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Closes to a fixed standoff distance and holds there.
 *
 * A normal follow goal walks right up to you, which is the opposite of
 * unsettling. This one approaches only while further away than {@code
 * preferredDistance}, then stops and stares. If you walk toward him he does
 * not retreat — he simply stops advancing, so you are always the one closing
 * the gap.
 */
public class StalkPlayerGoal extends Goal {
	private final PathfinderMob mob;
	private final double preferredDistance;
	private final double maxDistance;
	private Player target;
	private int repathCooldown;

	public StalkPlayerGoal(PathfinderMob mob, double preferredDistance, double maxDistance) {
		this.mob = mob;
		this.preferredDistance = preferredDistance;
		this.maxDistance = maxDistance;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		this.target = this.mob.level().getNearestPlayer(this.mob, this.maxDistance);
		return this.target != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.target != null
			&& this.target.isAlive()
			&& this.mob.distanceTo(this.target) <= this.maxDistance;
	}

	@Override
	public void stop() {
		this.target = null;
		this.mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}

		this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

		double distance = this.mob.distanceTo(this.target);
		if (distance <= this.preferredDistance) {
			// Close enough. Stand and watch.
			this.mob.getNavigation().stop();
			return;
		}

		// Repath on a cooldown rather than every tick — recalculating a path
		// 20x a second to a moving player is wasteful and makes him jitter.
		if (--this.repathCooldown <= 0) {
			this.repathCooldown = 20;
			this.mob.getNavigation().moveTo(this.target, 0.7);
		}
	}
}
