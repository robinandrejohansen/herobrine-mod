package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.entity.Corpses;
import com.bloomlet.herobrine.entity.PlayerCorpseEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What keeps a kept mob a corpse. See Corpses.
 *
 * The pose is not saved with the entity, so a body reloaded with its chunk would
 * stand up; and a zombie's own aiStep sets it alight in daylight whether it has
 * a mind or not. Both are put back at the end of every tick. The lying-down box
 * is what makes a body clickable: the sleeping pose's own box is a fifth of a
 * block, which is a hitbox nobody can find.
 */
@Mixin(LivingEntity.class)
public abstract class CorpseMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void herobrine$lieStill(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof PlayerCorpseEntity || !Corpses.isCorpse(self)) {
			return;
		}
		if (self.getPose() != Pose.SLEEPING) {
			self.setPose(Pose.SLEEPING);
			self.refreshDimensions();
		}
		if (self.getRemainingFireTicks() > 0) {
			self.clearFire();
		}
		if (self.isAlive() && self.getHealth() < 1.0F) {
			self.setHealth(1.0F);
		}
	}

	@Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
	private void herobrine$lyingBox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof PlayerCorpseEntity || !Corpses.isCorpse(self)) {
			return;
		}
		float tall = self.getType().getDimensions().height();
		cir.setReturnValue(EntityDimensions.fixed(Math.min(3.0F, Math.max(0.6F, tall)), 0.45F));
	}
}
