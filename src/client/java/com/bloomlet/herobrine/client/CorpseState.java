package com.bloomlet.herobrine.client;

/**
 * Carried on LivingEntityRenderState by CorpseStateMixin: whether this body is a
 * kept corpse. It lives OUTSIDE the mixin package on purpose — Sponge Mixin
 * refuses to load any class that sits in a configured mixin package and is
 * referenced directly, and both the state mixin and the render mixin reference
 * this interface by name.
 */
public interface CorpseState {
	boolean herobrine$isCorpse();

	void herobrine$setCorpse(boolean corpse);
}
