package com.bloomlet.herobrine.client;

import net.minecraft.resources.Identifier;

/** A body's render state: Addexio's fallen pose, plus whose skin it wears. */
public class PlayerCorpseRenderState extends CompanionRenderState {
	public Identifier skin = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
}
