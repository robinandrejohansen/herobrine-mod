package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** White eyes, no pupils, and they glow: you see them across a dark street before you see him. */
public class TurnedEyesLayer extends EyesLayer<VillagerRenderState, VillagerModel> {
	private static final RenderType EYES =
		RenderTypes.eyes(HerobrineMod.id("textures/entity/turned/villager_eyes.png"));

	public TurnedEyesLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
		super(renderer);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}
