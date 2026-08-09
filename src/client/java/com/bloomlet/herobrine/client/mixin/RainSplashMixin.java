package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.client.Atmosphere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The splashes, so they match the rain that made them.
 *
 * Reported from a playtest: at SIEGE the rain fell red and then landed BLUE.
 * The two are completely separate systems and only one of them had been
 * touched — WeatherEffectRenderer draws the falling streaks, and the little
 * splash where a drop hits the ground is a particle, spawned by ClientLevel
 * and rendered from its own sprite.
 *
 * WaterDropParticle never calls setColor at all, so the blue is in the texture
 * rather than in a tint. Setting the colour multiplies that sprite, which is
 * why the correction has to be applied here and cannot simply reuse the ARGB
 * the weather renderer produces: one is a colour, the other is a filter over a
 * blue-white image.
 *
 * Injected at the END of the constructor rather than the start, so nothing here
 * can be overwritten by the object still setting itself up. There is exactly
 * one constructor on this class, which is checked rather than assumed — an
 * ambiguous selector has taken this client down twice.
 */
@Mixin(WaterDropParticle.class)
public abstract class RainSplashMixin {

	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD"
		+ "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("RETURN"))
	private void herobrine$redden(ClientLevel level, double x, double y, double z,
	                              TextureAtlasSprite sprite, CallbackInfo info) {
		float[] tint = Atmosphere.splashTint();
		if (tint != null) {
			((net.minecraft.client.particle.SingleQuadParticle)(Object)this)
				.setColor(tint[0], tint[1], tint[2]);
		}
	}
}
