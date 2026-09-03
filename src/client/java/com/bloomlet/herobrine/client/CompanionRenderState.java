package com.bloomlet.herobrine.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Addexio's render state: the humanoid one, plus whether he is lying dead on the floor. */
public class CompanionRenderState extends HumanoidRenderState {
	public boolean fallen;
}
