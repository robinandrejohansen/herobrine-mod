package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.Atmosphere;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.WeatherEffectRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Red rain.
 *
 * WeatherEffectRenderer.renderInstances builds every raindrop quad with
 * `int color = ARGB.white(alpha)`. That single hardcoded expression is the
 * only reason a mod can recolour the rain at all, and it is why this is a
 * two-line mixin rather than a reimplementation of the weather renderer.
 *
 * The target descriptor is spelled out in full because ARGB.white is
 * OVERLOADED — white(float) and white(int) — and a bare name would be
 * ambiguous. That is the same class of mistake that took this client down
 * twice already: a method name that matched more than one thing.
 * renderInstances itself was checked to be unique in the class, so there is no
 * synthetic bridge for the selector to catch on either.
 *
 * It tints snow as well as rain, since both go through this method separated
 * only by a maxAlpha argument. That is accepted rather than worked around: the
 * discriminator is fragile, snowstorms were cut from this mod on purpose, and
 * red snow at SIEGE is not a worse outcome than red rain.
 */
@Mixin(WeatherEffectRenderer.class)
public class WeatherTintMixin {

	@ModifyExpressionValue(
		method = "renderInstances",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;white(F)I"))
	private int herobrine$redden(int white) {
		return Atmosphere.rainTint(white);
	}
}
