package com.bloomlet.herobrine.client;

/**
 * Carries "he was just hit, and this hard" onto the render state.
 *
 * The same arrangement as {@link PossessedEyes} and for the same reason:
 * rendering in 26.x reads an extracted state rather than the entity, so anything
 * the renderer needs has to be copied across during extraction. Mixed into
 * LivingEntityRenderState next to the eyes field.
 *
 * A float from one down to nought rather than a tick count, because the fade is
 * the only thing the layer cares about and working it out twice would be two
 * places to get it wrong.
 */
public interface Wounded {
	float herobrine$wound();

	void herobrine$wound(float fade);
}
