package com.bloomlet.herobrine.client;

import java.util.UUID;

import com.bloomlet.herobrine.entity.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * THE ACTUAL PLAYER RIG, not the humanoid one he uses.
 *
 * He renders on the zombie mesh, which is fine for him — he is not pretending to
 * be anybody in particular. This one is, and on that rig it was subtly wrong in
 * two ways that both matter more than they sound:
 *
 *   - SLIM SKINS RENDERED THREE PIXELS TOO BROAD in the arms. Nobody could name
 *     it, and roughly one player in fifty would keep looking at him because
 *     something was off. That is the wrong KIND of uncanny: it makes people
 *     doubt the rendering rather than the situation.
 *   - THE SECOND LAYER DID NOT DRAW AT ALL. Jackets, sleeves, trouser overlays,
 *     hats. Most custom skins put half their detail there, so a mimic of
 *     somebody with a good skin was a flat approximation of them — which is
 *     exactly the thing a friend would notice about a copy of their own avatar.
 *
 * Both are gone. This runs PlayerModel on AvatarRenderState, the same pairing
 * vanilla draws every real player with, so what stands in the cave is rendered
 * by the same code as the person it is copying.
 *
 * WIDE AND SLIM ARE TWO MODELS AND THE CHOICE IS PER-ENTITY, which is the one
 * wrinkle. Vanilla sidesteps it by registering two whole renderers and picking
 * between them upstream; a custom entity type only gets one, so both meshes are
 * baked here and this.model is pointed at the right one in submit(), immediately
 * before the model reference is handed to the collector. Doing it there rather
 * than during extraction is deliberate — extraction can be batched across
 * several entities, and setting a shared field during a batch would give every
 * mimic whichever arm width was extracted last.
 */
public class MimicRenderer
		extends HumanoidMobRenderer<MimicEntity, AvatarRenderState, PlayerModel> {

	private final PlayerModel wide;
	private final PlayerModel slim;

	public MimicRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.wide = this.getModel();
		this.slim = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		// AND NO EYES LAYER, which is the whole point of a separate renderer. The
		// white eyes are the one thing that would give him away instantly, and
		// they are exactly what a player expects to be looking for.
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	/**
	 * Whose skin, and nothing is downloaded to find out.
	 *
	 * He can only ever wear somebody who is logged in, so their skin is ALREADY
	 * loaded on this client — it arrived with their tab-list entry, and it is the
	 * same texture their own body is being drawn with somewhere else in the world.
	 * We look it up by the fake profile's id and take the whole PlayerSkin, which
	 * carries the arm width along with the texture.
	 */
	@Override
	public void extractRenderState(MimicEntity entity, AvatarRenderState state,
			float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.skin = skinOf(entity.wearing());
		state.id = entity.getId();
	}

	private static net.minecraft.world.entity.player.PlayerSkin skinOf(String raw) {
		if (raw.isEmpty() || Minecraft.getInstance().getConnection() == null) {
			return DefaultPlayerSkin.getDefaultSkin();
		}
		try {
			UUID id = UUID.fromString(raw);
			PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(id);
			// The default is keyed off the id when the entry has not arrived yet,
			// so the fallback is at least a stable Steve or Alex rather than a
			// flicker between two different bodies.
			return info == null ? DefaultPlayerSkin.get(id) : info.getSkin();
		} catch (IllegalArgumentException malformed) {
			return DefaultPlayerSkin.getDefaultSkin();
		}
	}

	@Override
	public void submit(AvatarRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera) {
		this.model = state.skin.model() == PlayerModelType.SLIM ? this.slim : this.wide;
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}
}
