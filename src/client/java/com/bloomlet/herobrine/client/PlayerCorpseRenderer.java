package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.entity.PlayerCorpseEntity;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * A player's body: the humanoid mesh in that player's skin, lying in the fallen
 * pose Addexio uses. The skin comes from the skin manager by profile, so a body
 * left by somebody who has since logged off still looks like them.
 */
public class PlayerCorpseRenderer extends HumanoidMobRenderer<
		PlayerCorpseEntity, PlayerCorpseRenderState, HumanoidModel<PlayerCorpseRenderState>> {

	static final class Body extends HumanoidModel<PlayerCorpseRenderState> {
		Body(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(PlayerCorpseRenderState state) {
			super.setupAnim(state);
			this.rightArm.xRot = 0.0F;
			this.leftArm.xRot = 0.0F;
			this.rightArm.zRot = 1.35F;
			this.leftArm.zRot = -1.35F;
			this.rightLeg.xRot = 0.0F;
			this.leftLeg.xRot = 0.0F;
			this.rightLeg.zRot = 0.35F;
			this.leftLeg.zRot = -0.35F;
			this.head.yRot = 0.6F;
			this.head.xRot = 0.15F;
		}
	}

	private final Map<UUID, Supplier<PlayerSkin>> skins = new HashMap<>();

	public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
		super(context, new Body(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
	}

	@Override
	public PlayerCorpseRenderState createRenderState() {
		return new PlayerCorpseRenderState();
	}

	@Override
	public Identifier getTextureLocation(PlayerCorpseRenderState state) {
		return state.skin;
	}

	@Override
	public void extractRenderState(PlayerCorpseEntity entity, PlayerCorpseRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.fallen = true;
		UUID who = entity.whoId();
		if (who == null) {
			state.skin = DefaultPlayerSkin.getDefaultTexture();
			return;
		}
		Supplier<PlayerSkin> lookup = this.skins.computeIfAbsent(who, id -> Minecraft.getInstance()
			.getSkinManager().createLookup(new GameProfile(id, entity.whoName()), false));
		PlayerSkin skin = lookup.get();
		state.skin = skin != null ? skin.body().texturePath() : DefaultPlayerSkin.get(who).body().texturePath();
	}
}
