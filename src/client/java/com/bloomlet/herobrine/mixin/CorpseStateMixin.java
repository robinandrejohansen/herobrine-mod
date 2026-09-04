package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.client.CorpseState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class CorpseStateMixin implements CorpseState {
	@Unique
	private boolean herobrine$corpse;

	@Override
	public boolean herobrine$isCorpse() {
		return this.herobrine$corpse;
	}

	@Override
	public void herobrine$setCorpse(boolean corpse) {
		this.herobrine$corpse = corpse;
	}
}
