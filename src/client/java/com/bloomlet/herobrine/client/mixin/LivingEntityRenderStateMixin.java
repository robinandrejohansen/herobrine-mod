package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.PossessedEyes;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Somewhere on the render state to put "this one has stopped pretending". */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements PossessedEyes {
	@Unique
	private @org.jspecify.annotations.Nullable Identifier herobrine$eyesTexture;

	@Override
	public @org.jspecify.annotations.Nullable Identifier herobrine$eyes() {
		return this.herobrine$eyesTexture;
	}

	@Override
	public void herobrine$eyes(@org.jspecify.annotations.Nullable Identifier texture) {
		this.herobrine$eyesTexture = texture;
	}

}
