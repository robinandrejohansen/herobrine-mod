package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders him on the standard humanoid rig, then hangs the emissive layer off it.
 *
 * The mesh is borrowed from ModelLayers.ZOMBIE rather than registering our own
 * — it is the plain humanoid shape with a hat layer, which is all we need, and
 * it avoids maintaining a mesh definition for a body identical to vanilla's.
 *
 * Known gap: this rig has no second layer on the torso or limbs, so the
 * tattered shroud in the texture will not show. That needs the player rig,
 * which is typed to AvatarRenderState and drags in player-specific state.
 */
public class HerobrineRenderer
		extends HumanoidMobRenderer<HerobrineEntity, HerobrineRenderState, HumanoidModel<HerobrineRenderState>> {

	private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/herobrine.png");
	/**
	 * The face he makes once he has stopped pretending.
	 *
	 * Chosen off the render state's SCALE rather than off an act number, and that
	 * is not a shortcut. SCALE is already synced because it is an attribute, so the
	 * size and the face are one value — they physically cannot disagree, and there
	 * is no second field to forget to update. He gets bigger and angrier in the
	 * same frame because they are the same fact.
	 */
	private static final Identifier ANGRY =
		HerobrineMod.id("textures/entity/herobrine_angry.png");
	/** Anything above a man's size is a man who has stopped being one. */
	private static final float STOPPED_PRETENDING = 1.05F;

	public HerobrineRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(new HerobrineEyesLayer<>(this));
	}

	@Override
	public HerobrineRenderState createRenderState() {
		return new HerobrineRenderState();
	}

	@Override
	public Identifier getTextureLocation(HerobrineRenderState state) {
		return state.scale > STOPPED_PRETENDING ? ANGRY : TEXTURE;
	}

	/**
	 * Copies the fade across, because the layer cannot see the entity.
	 *
	 * Worked out from a TIMESTAMP the server synced once rather than from a
	 * counter it syncs every tick — see HerobrineEntity.WOUNDED. The client has
	 * the same game clock, so subtracting is free and exact.
	 *
	 * This replaced a bespoke render layer that drew him black. See WOUND_FLASH for
	 * why black could never have worked on a black skin.
	 */
	@Override
	public void extractRenderState(HerobrineEntity entity, HerobrineRenderState state,
	                               float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		// THE WHITE. The same overlay a creeper uses when it swells: from nothing to
		// everything over the seven seconds he takes to leave.
		Long dying = entity.getAttached(HerobrineEntity.DYING_SINCE);
		state.whiteness = dying == null ? 0.0F : net.minecraft.util.Mth.clamp(
			(entity.level().getGameTime() - dying + partialTicks) / (float) HerobrineEntity.DYING_TAKES,
			0.0F, 1.0F);
		// THE DARK. While he rises between acts the light goes out of him over two
		// seconds — sky and block light on the render state scaled toward zero — so
		// he hangs there as a shape, not a lit model. His eyes render fullbright and
		// stay.
		Long rising = entity.getAttached(HerobrineEntity.RISING_SINCE);
		if (rising != null) {
			float dark = net.minecraft.util.Mth.clamp(
				(entity.level().getGameTime() - rising + partialTicks) / 40.0F, 0.0F, 1.0F);
			int sky = (state.lightCoords >> 20) & 0xF;
			int block = (state.lightCoords >> 4) & 0xF;
			state.lightCoords = (Math.round(sky * (1.0F - dark)) << 20)
				| (Math.round(block * (1.0F - dark)) << 4);
		}
		// ACT THREE IS UNLIT FOR GOOD. The rise's darkness, kept: no light on him at
		// all, so in a torch-lit hall he is a hole in the room with two white eyes.
		// Not while he is leaving — the white of the ending needs him lit to read.
		if (entity.isShadow() && dying == null) {
			state.lightCoords = 0;
		}
		Long hit = entity.getAttached(HerobrineEntity.WOUNDED);
		long since = hit == null ? Long.MAX_VALUE : entity.level().getGameTime() - hit;
		// Vanilla's own field, so the body is tinted by the same code that tints
		// every other mob in the game — and RenderTypes.eyes() ignores the overlay,
		// so his eyes stay lit straight through it.
		state.hasRedOverlay = since >= 0 && since < HerobrineEntity.WOUND_FLASH;

		// AND THE ARM, FROM THE SAME KIND OF STAMP.
		//
		// attackTime is what HumanoidModel.setupAttackAnimation reads, and vanilla
		// fills it from getAttackAnim — which is fed by updateSwingTime, which
		// nothing calls for a mob in 26.2. So it was always zero and he swung a
		// sword without moving.
		//
		// Ramped up over the first two thirds and back down over the last, because
		// setupAttackAnimation treats it as a progress value and a swing that
		// stops at full extension reads as a mob freezing mid-blow.
		Long swung = entity.getAttached(HerobrineEntity.SWUNG);
		float ago = swung == null ? Float.MAX_VALUE
			: entity.level().getGameTime() - swung + partialTicks;
		state.attackTime = ago < 0.0F || ago >= HerobrineEntity.SWING_SHOWS ? 0.0F
			: 1.0F - Math.abs(ago / HerobrineEntity.SWING_SHOWS - 0.35F) / 0.65F;
	}

	@Override
	protected float getWhiteOverlayProgress(HerobrineRenderState state) {
		return state.whiteness;
	}
}
