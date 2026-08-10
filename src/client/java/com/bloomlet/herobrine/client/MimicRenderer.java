package com.bloomlet.herobrine.client;

import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.MimicEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Wears whoever he is copying.
 *
 * The skin is not downloaded and nothing is fetched. He can only ever be wearing
 * somebody who is logged in, so their skin is ALREADY loaded on this client —
 * it arrived with their tab-list entry when they joined, and it is the same
 * texture their own body is being drawn with two hundred blocks away. We look it
 * up by the fake profile's id and hand it to the same humanoid rig he uses.
 *
 * THE FALLBACK IS HIM. If the entry has not arrived yet, or the skin is still
 * resolving, he renders as Herobrine rather than as Steve. Every other choice
 * here is a bug that looks like a bug; this one is a bug that looks like the
 * mod. Worth having on a path that can genuinely lose a race with the network.
 *
 * KNOWN GAP: the rig is the wide-armed humanoid, so somebody using a slim skin
 * renders three pixels too broad in the arms. It is the kind of thing one player
 * in fifty notices and cannot explain, which is not the worst place for it to
 * sit, but it is a real difference and not a deliberate one.
 */
public class MimicRenderer
		extends HumanoidMobRenderer<MimicEntity, MimicRenderer.State, HumanoidModel<MimicRenderer.State>> {

	private static final Identifier FALLBACK = HerobrineMod.id("textures/entity/herobrine.png");

	/** The humanoid state plus the one thing it does not carry: whose face this is. */
	public static class State extends HumanoidRenderState {
		public Identifier skin = FALLBACK;
	}

	public MimicRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		// AND NO EYES LAYER, which is the whole point of a separate renderer. The
		// white eyes are the one thing that would give him away instantly, and
		// they are exactly what a player expects to be looking for.
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MimicEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.skin = skinOf(entity.wearing());
	}

	private static Identifier skinOf(String raw) {
		if (raw.isEmpty() || Minecraft.getInstance().getConnection() == null) {
			return FALLBACK;
		}
		try {
			PlayerInfo info = Minecraft.getInstance().getConnection()
				.getPlayerInfo(UUID.fromString(raw));
			return info == null ? FALLBACK : info.getSkin().body().texturePath();
		} catch (IllegalArgumentException malformed) {
			return FALLBACK;
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.skin;
	}
}
