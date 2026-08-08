package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.PossessedEyes;
import com.bloomlet.herobrine.client.PossessedEyesLayer;
import com.bloomlet.herobrine.client.PossessedEyesTextures;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Feral;
import com.bloomlet.herobrine.manifest.Possession;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the eyes on every living thing in the game.
 *
 * A mixin rather than an API call, and not for want of looking. Fabric's
 * FeatureRendererRegistry in this version is the new submit-node system and
 * does not add model layers to vanilla entity renderers; there is no hook for
 * "give every mob an extra layer". So: add the layer in the constructor, and
 * fill in the texture during state extraction.
 *
 * Both injections are cheap. The layer returns immediately unless the state is
 * carrying a texture, and extraction does one attachment lookup — which is why
 * this can afford to be on LivingEntityRenderer, covering everything, instead
 * of being wired up per mob type.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity,
		S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

	@org.spongepowered.asm.mixin.Unique
	private static final java.util.Set<Integer> herobrine$told =
		java.util.Collections.synchronizedSet(new java.util.HashSet<>());

	@Shadow
	protected abstract boolean addLayer(RenderLayer<S, M> layer);

	@SuppressWarnings("unchecked")
	@Inject(method = "<init>", at = @At("TAIL"))
	private void herobrine$addEyes(EntityRendererProvider.Context context, M model,
	                               float shadow, CallbackInfo info) {
		this.addLayer(new PossessedEyesLayer<>((RenderLayerParent<S, M>)this));
	}

	/**
	 * Rendering reads the extracted state, never the entity, so the answer has
	 * to be copied across here or the layer has no way to ask.
	 */
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void herobrine$markRevealed(T entity, S state, float partialTicks,
	                                    CallbackInfo info) {
		((PossessedEyes)state).herobrine$eyes(
			entity instanceof Mob mob
				? PossessedEyesTextures.forType(mob.getType(), Possession.menace(mob))
				: null);
		((PossessedEyes)state).herobrine$infected(
			entity instanceof Mob mob && Feral.isFeral(mob));

		// Client-side counterpart to the log in Feral.shutIn. Says once per
		// villager what THIS side can see, so the two lines can be compared and
		// the break located rather than guessed at.
		if (entity instanceof Mob mob && mob.getType() == EntityTypes.VILLAGER
			&& herobrine$told.add(mob.getId())) {
			HerobrineMod.LOGGER.info("client sees villager {}: feral={} menace={} silent={}",
				mob.getId(), Feral.isFeral(mob), Possession.menace(mob), mob.isSilent());
		}
	}
}
