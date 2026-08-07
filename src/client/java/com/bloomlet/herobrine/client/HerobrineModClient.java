package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.entity.EntityRenderers;

public class HerobrineModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.HEROBRINE, HerobrineRenderer::new);
	}
}
