package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.InfectedEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * One texture, one model, and nothing of anybody else's touched.
 *
 * The whole reason this exists. Swapping a villager's skin meant injecting into
 * vanilla's villager renderer, and that broke the game twice — here the mob
 * owns its renderer outright and the texture is simply returned.
 *
 * The missing arm is in the texture rather than the model: the arm block is
 * cleared to transparency, so the cube is still rendered and nothing appears in
 * it. Minecraft gives that for free, and it means no mesh definition to
 * maintain.
 */
public class InfectedRenderer
		extends HumanoidMobRenderer<InfectedEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

	private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/infected/zombie.png");

	public InfectedRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
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
