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

	/**
	 * Whether this one wears the infected skin instead of its own.
	 *
	 * Separate from the eyes because they answer different questions. The eyes
	 * say it has turned on you; this says what it IS, and a villager shut in a
	 * cell for years is infected whether or not it has noticed you yet.
	 */
	boolean herobrine$infected();

	void herobrine$infected(boolean infected);
}
