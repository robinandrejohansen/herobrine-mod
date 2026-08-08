package com.bloomlet.herobrine.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The eyes.
 *
 * Written as a RenderLayer rather than extending EyesLayer, because EyesLayer's
 * renderType() takes no arguments — it is built for one entity with one
 * texture, and this needs the texture the state is carrying. Everything else is
 * the same submission EyesLayer makes.
 *
 * RenderTypes.eyes() draws at full brightness with world lighting ignored, so
 * these pixels are exactly as bright in a midnight field as at noon. That is
 * the whole point: a painted highlight would be darkened along with the animal
 * and would vanish at the moment it mattered.
 */
public class PossessedEyesLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
		extends RenderLayer<S, M> {

	public PossessedEyesLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector,
	                   int lightCoords, S state, float yRot, float xRot) {
		Identifier eyes = ((PossessedEyes)state).herobrine$eyes();
		if (eyes == null || state.isInvisible) {
			return;
		}
		collector.order(1).submitModel(this.getParentModel(), state, poseStack,
			RenderTypes.eyes(eyes), lightCoords, OverlayTexture.NO_OVERLAY,
			state.outlineColor, null);
	}
}
