package com.bloomlet.herobrine.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Herobrine's render state: the humanoid one, plus how far into leaving he is (0..1). */
public class HerobrineRenderState extends HumanoidRenderState {
	public float whiteness;
}
