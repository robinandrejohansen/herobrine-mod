package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders him on the standard humanoid rig, then hangs the emissive layer off it.
 *
 * The mesh is borrowed from ModelLayers.ZOMBIE rather than registering our own
 * — it is the plain humanoid shape with a hat layer, which is all we need, and
 * it avoids maintaining a mesh definition for a body identical to vanilla's.
 *
 * Known gap: this rig has no second layer on the torso or limbs, so the
 * tattered shroud in the texture will not show. That needs the player rig,
 * which is typed to AvatarRenderState and drags in player-specific state.
 */
public class HerobrineRenderer
		extends HumanoidMobRenderer<HerobrineEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

	private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/herobrine.png");

	public HerobrineRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(new HerobrineEyesLayer<>(this));
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
