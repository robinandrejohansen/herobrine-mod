package com.bloomlet.herobrine.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * HE GOES BLACK WHEN YOU HIT HIM.
 *
 * The red flash was asked for and then immediately corrected to black, and the
 * correction is right: vanilla's hurt tint means "this is an animal and you are
 * winning", which are the two things this fight should never say. Black says
 * something registered and tells you nothing about how it felt.
 *
 * WHICH IS ALSO WHY THIS IS NOT VANILLA'S OVERLAY. That red comes from the
 * OverlayTexture, a single dynamically-built atlas every entity in the game
 * shares, and recolouring it for one mob would recolour it for all of them. So
 * this ignores the mechanism entirely and draws his own model a second time,
 * with his own texture, tinted to nothing.
 *
 * The tint is the seventh argument of submitModel — an ARGB int the render type
 * multiplies through. Zero in all three colour channels leaves black whatever
 * the texture underneath it was, and the alpha is the fade. So the silhouette is
 * exactly his, which matters: a flash that changed his outline would read as a
 * different entity for a fifth of a second.
 *
 * NOT emissive and NOT eyes(). Both of those add light, and the whole point is
 * to remove it — RenderTypes.eyes() on a black texture draws nothing at all,
 * which is the mistake this comment exists to stop somebody making later.
 */
public class HerobrineWoundLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
		extends RenderLayer<S, M> {

	private final Identifier texture;

	public HerobrineWoundLayer(RenderLayerParent<S, M> renderer, Identifier texture) {
		super(renderer);
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector,
	                   int lightCoords, S state, float yRot, float xRot) {
		float fade = ((Wounded)state).herobrine$wound();
		if (fade <= 0.0F || state.isInvisible) {
			return;
		}
		// A TINT, NOT A SILHOUETTE. Seventy-eight percent was far too much: eight
		// ticks of near-solid black over his whole body does not read as a hit, it
		// reads as him blinking out — which is the one thing he already does, so
		// the flash was saying "he teleported" at the exact moment it needed to say
		// "you connected".
		//
		// Vanilla's hurt tint is subtle for this reason. It darkens toward red
		// without hiding the model, and the player reads it as damage because the
		// shape stayed put. A third is enough to see and not enough to lose him.
		int alpha = (int)(Math.min(1.0F, fade) * 0.34F * 255.0F);
		if (alpha <= 2) {
			return;
		}
		// Drawn after the body and after the eyes, so it darkens both — being hit
		// should put his eyes out for a moment as well.
		collector.order(2).submitModel(this.getParentModel(), state, poseStack,
			RenderTypes.entityTranslucent(this.texture), lightCoords,
			OverlayTexture.NO_OVERLAY, alpha << 24, null);
	}
}
