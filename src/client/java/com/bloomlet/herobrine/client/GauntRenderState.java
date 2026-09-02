package com.bloomlet.herobrine.client;

/**
 * Two numbers the enderman's own render state has nowhere to put.
 *
 * A render state is the whole contract between the entity and the model — the
 * model never sees the entity — so anything the drawing depends on has to be a
 * field on one of these. EndermanRenderState carries isCreepy and a carried
 * block, and this creature needs neither of those and two things it does not
 * have.
 *
 * Extending it rather than smuggling the values through isCreepy, which is the
 * obvious shortcut and would have cost the angry arm pose to say "it is
 * talking".
 */
public class GauntRenderState
		extends net.minecraft.client.renderer.entity.state.EndermanRenderState {

	/** It has turned round and is looking back at somebody. */
	public boolean staring;

	/** 1 on the tick it made a noise, falling to 0 over twelve. */
	public float voice;
}
