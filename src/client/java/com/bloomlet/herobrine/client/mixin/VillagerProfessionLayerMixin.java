package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.PossessedEyes;

import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drains an infected villager, clothes and all.
 *
 * Swapping the base skin did nothing and it took a diagnostic on both sides to
 * find out why: this layer draws the villager's TYPE texture over the entire
 * model, so the base underneath it is invisible on every pixel that matters.
 * The clothing is the villager.
 *
 * So this tints instead of replacing. Every pass the layer makes — the type
 * robe, the profession apron, the badge — is multiplied by one sickly colour,
 * which drains the whole figure at once. That turns out to be the better
 * effect as well as the easier one: a replaced texture would have to be drawn
 * for every villager type in the game, and a tint keeps the librarian's robe
 * legible while making it obvious the librarian is not well.
 *
 * The flag is read at the top of the layer and held in a field for the three
 * calls that follow, which is safe because all of this happens on one render
 * thread and none of it outlives the method.
 *
 * Both injections name submit's FULL descriptor, and that is not tidiness. The
 * class carries two of them — the real one and a synthetic bridge taking the
 * erased EntityRenderState — and a bare "submit" matches both. The bridge
 * contains none of the calls being modified, so the argument modifier scanned
 * zero targets there, failed its injection check, and crashed the game on
 * startup.
 */
@Mixin(VillagerProfessionLayer.class)
public class VillagerProfessionLayerMixin {
	/** A drained, faintly green grey. Multiplied over whatever is underneath. */
	@Unique
	private static final int HEROBRINE$SICK = 0xFF6E7A64;

	@Unique
	private boolean herobrine$drained;

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
		at = @At("HEAD"))
	private void herobrine$check(com.mojang.blaze3d.vertex.PoseStack poseStack,
	                             net.minecraft.client.renderer.SubmitNodeCollector collector,
	                             int lightCoords, LivingEntityRenderState state,
	                             float yRot, float xRot, CallbackInfo info) {
		this.herobrine$drained = ((PossessedEyes)state).herobrine$infected();
	}

	@ModifyArg(
		method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;"
				+ "renderColoredCutoutModel(Lnet/minecraft/client/model/Model;"
				+ "Lnet/minecraft/resources/Identifier;"
				+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
				+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
				+ "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;II)V"),
		index = 6)
	private int herobrine$tint(int colour) {
		return this.herobrine$drained ? HEROBRINE$SICK : colour;
	}
}
