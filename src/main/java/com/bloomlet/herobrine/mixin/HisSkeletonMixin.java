package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.block.TheWayBlock;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IN HIS WORLD, WHAT LEAVES THE BOWSTRING IS NOT AN ARROW.
 *
 * A ghast's fireball, thrown by something holding a bow, that takes a bite out
 * of the ground where it lands. Which is the whole reason it is this and not a
 * stronger arrow: the dimension should be visibly WORSE every time the player
 * comes back to it, and nothing else available does that. A wood full of
 * skeletons shelling each other's cover for as long as the chunk is loaded
 * rearranges itself whether anybody is watching or not.
 *
 * THE BOW STAYS IN ITS HAND AND THAT IS THE POINT. Everything a player knows
 * about fighting skeletons still applies — they hold their range, they lead the
 * shot, they back off when crowded, they shuffle sideways behind cover — so the
 * one thing that has changed is unmissable the first time it happens. Taking the
 * bow away and giving them a new attack goal would have produced a different
 * mob, which teaches nothing.
 *
 * A NARROW MIXIN, and the narrowness is deliberate. It replaces one method on
 * one mob, refuses unless the level is his, and touches nothing about how the
 * skeleton decides to shoot — vanilla still owns the aiming, the cooldown, the
 * difficulty scaling and the line of sight. Every ordinary skeleton in every
 * ordinary world goes down the untouched path on the first line.
 */
@Mixin(AbstractSkeleton.class)
public abstract class HisSkeletonMixin {

	/**
	 * One, not four.
	 *
	 * A ghast throws power one and it makes a hole about three blocks across —
	 * enough to be terrain damage the player can point at, small enough that a
	 * volley across a clearing does not turn the ground into a crater field
	 * nobody can path across. Anything higher and the dimension digs itself down
	 * to bedrock over an afternoon.
	 */
	private static final int POWER = 1;

	@Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
	private void herobrine$throwFire(LivingEntity target, float pull, CallbackInfo info) {
		AbstractSkeleton bones = (AbstractSkeleton)(Object)this;
		if (!(bones.level() instanceof net.minecraft.server.level.ServerLevel here)
			|| !here.dimension().equals(TheWayBlock.HIS_WORLD)
			|| !Config.get().hisHost) {
			return;
		}
		info.cancel();

		Vec3 from = bones.getEyePosition();
		// Aimed at the middle of them rather than the feet, and with the same
		// small lead a bow gets, so a moving player can still step out of it.
		Vec3 along = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
			.subtract(from);
		LargeFireball ball = new LargeFireball(here, bones, along.normalize(), POWER);
		ball.snapTo(from.x, from.y, from.z, bones.getYRot(), bones.getXRot());
		here.addFreshEntity(ball);
		here.playSound(null, bones.blockPosition(),
			net.minecraft.sounds.SoundEvents.GHAST_SHOOT,
			net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.7F);
	}
}
