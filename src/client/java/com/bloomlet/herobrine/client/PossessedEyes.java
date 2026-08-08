package com.bloomlet.herobrine.client;

import net.minecraft.resources.Identifier;

/**
 * Carries "this one has stopped pretending" onto the render state.
 *
 * Rendering in 26.x reads from an extracted state rather than from the entity,
 * so anything the renderer needs to know has to be copied across during
 * extraction. Mixed into LivingEntityRenderState, which gives every living
 * thing in the game somewhere to put it.
 */
public interface PossessedEyes {
	@org.jspecify.annotations.Nullable Identifier herobrine$eyes();

	void herobrine$eyes(@org.jspecify.annotations.Nullable Identifier texture);
}
