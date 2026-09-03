package com.bloomlet.herobrine.mixin;

/** Carried on LivingEntityRenderState by CorpseStateMixin: whether this body is a kept corpse. */
public interface CorpseState {
	boolean herobrine$isCorpse();

	void herobrine$setCorpse(boolean corpse);
}
