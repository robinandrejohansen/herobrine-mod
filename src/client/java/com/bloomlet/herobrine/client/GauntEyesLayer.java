package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * THE WHITE EYES, AND THEY HAVE TO BE A SEPARATE SHEET TO BE WHITE.
 *
 * Painted white pixels are white at noon and dark grey down a cell, because they
 * are lit like every other pixel on the model. That is fine for skin and wrong
 * for these: the reference for this face is two flat white rectangles that hold
 * their brightness in pitch dark, which is the one thing paint cannot do.
 *
 * RenderTypes.eyes() draws at full brightness with world lighting ignored — the
 * same mechanism HerobrineEyesLayer uses, and for the same reason. EyesLayer
 * submits the WHOLE parent model with this render type, so gaunt_eyes.png is
 * transparent everywhere except the two eye rectangles.
 *
 * There is no invisibility override here, unlike his. He blinks out around every
 * relocation and a pair of eyes left streaking across the view was a bug worth
 * fixing; this one never goes invisible. It stands there.
 */
public class GauntEyesLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>>
		extends EyesLayer<S, M> {

	private static final RenderType EYES =
		RenderTypes.eyes(HerobrineMod.id("textures/entity/gaunt/gaunt_eyes.png"));

	public GauntEyesLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}
