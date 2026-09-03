package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.entity.Corpses;
import com.bloomlet.herobrine.entity.PlayerCorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HOW A KEPT BODY LIES. The death animation's own roll — ninety degrees about
 * the model's long axis, the way every mob in the game falls over when it dies —
 * held forever, and lifted half a body so it rests on the floor instead of in
 * it. Works for a pig and a zombie alike, which the sleeping pose did not.
 * A player's body has its own renderer and its own pose; this leaves it alone.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CorpseRenderMixin {

	@Shadow
	protected abstract float getFlipDegrees();

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void herobrine$markCorpse(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		((CorpseState) state).herobrine$setCorpse(Corpses.isCorpse(entity) && !(entity instanceof PlayerCorpseEntity));
	}

	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
		at = @At("TAIL"))
	private void herobrine$lieOnTheSide(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
		if (!((CorpseState) state).herobrine$isCorpse() || state.hasPose(Pose.SLEEPING) || state.deathTime > 0.0F) {
			return;
		}
		poseStack.translate(0.0F, 0.42F * scale, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
	}
}
