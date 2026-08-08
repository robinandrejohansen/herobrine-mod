package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.Atmosphere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the mod into the world's own atmosphere.
 *
 * 26.2 keeps fog, sky, light and music in one registry of environment
 * attributes, assembled from LAYERS when the level is built — dimension, then
 * biome, then time of day, then weather, each modifying what the one before it
 * produced. Vanilla adds its own layers here for lightning flashes, so this is
 * the intended seam rather than a hole prised open.
 *
 * Injecting at RETURN puts our layers last, which matters: layers compose in
 * order, so ours modify the finished value rather than being overwritten by
 * the weather two lines later.
 *
 * Client-only, because all of it is drawn rather than simulated. The server
 * does not need to know what colour the fog is.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void herobrine$addAtmosphere(EnvironmentAttributeSystem.Builder builder,
	                                     CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> info) {
		Atmosphere.addLayers(builder);
	}
}
