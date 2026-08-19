package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The glow.
 *
 * This is the whole reason the textures are split in two. RenderTypes.eyes()
 * draws at full brightness with world lighting ignored, so these pixels are
 * exactly as bright in a pitch black cave as they are at noon. A painted-on
 * highlight cannot do that — it gets darkened along with everything else.
 *
 * EyesLayer submits the ENTIRE parent model with this render type, not just
 * the head, which is why herobrine_eyes.png carries the body cracks and the
 * palms as well as the eyes, and why every other pixel in it is transparent.
 */
public class HerobrineEyesLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>>
		extends EyesLayer<S, M> {

	private static final RenderType EYES =
		RenderTypes.eyes(HerobrineMod.id("textures/entity/herobrine_eyes.png"));

	public HerobrineEyesLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}

	/**
	 * AND WHEN HE IS NOT THERE, HIS EYES ARE NOT THERE EITHER.
	 *
	 * Vanilla's EyesLayer deliberately ignores invisibility — it is why an
	 * invisible spider is still two red dots in a cave, which is a good effect
	 * and completely wrong here.
	 *
	 * He goes invisible for four ticks around every relocation, so that the
	 * client's position interpolation smears something nobody can see rather
	 * than dragging him bodily across the player's view (see
	 * HerobrineEntity.blink). Without this override that fix makes the problem
	 * WORSE: the body vanishes and a pair of white eyes streaks across the field
	 * on its own, which is both more visible and more obviously a bug.
	 */
	@Override
	public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack,
	                   net.minecraft.client.renderer.SubmitNodeCollector collector,
	                   int lightCoords, S state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
	}
}
