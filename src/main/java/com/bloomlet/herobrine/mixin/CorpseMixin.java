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
 * A zombie's own aiStep sets it alight in daylight whether it has a mind or not,
 * so the fire is put out at the end of every tick. The lying-down box is what
 * makes a body clickable. The lying-down LOOK is the client's: CorpseRenderMixin
 * rolls the model onto its side the way the death animation does — the sleeping
 * pose was tried first and stands a pig on its hind legs, because a quadruped's
 * length is along the axis the sleeping rotation turns upright.
 */
@Mixin(LivingEntity.class)
public abstract class CorpseMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void herobrine$lieStill(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if ((self.tickCount & 3) != 0) {
			return;          // this runs on every living thing in every world; three ticks in four it does not
		}
		if (self instanceof PlayerCorpseEntity || !Corpses.isCorpse(self)) {
			return;
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
		// Rolled onto its side, the body reaches about its own height sideways from
		// where it stood, in whichever direction it faced — so the box is square,
		// that wide, and knee-high. Generous on purpose: this is the thing you click.
		float tall = self.getType().getDimensions().height();
		// GENEROUS ON PURPOSE. The box is what the crosshair has to land on, and a
		// body on the ground is thin: the old 0.6 of height meant looking almost
		// straight down at it. A metre tall and a body-length plus one wide is a
		// box you can open from where you are standing.
		cir.setReturnValue(EntityDimensions.fixed(Math.min(3.5F, Math.max(1.8F, tall + 1.0F)), 1.0F));
	}

	/** A body does not get shoved about by whoever walks through it. */
	@Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
	private void herobrine$notShoved(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && Corpses.isCorpse((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	/** And it does not shove anybody: with a box this size a yard of bodies would push you off your feet. */
	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void herobrine$noShoving(CallbackInfo ci) {
		if (Corpses.isCorpse((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
