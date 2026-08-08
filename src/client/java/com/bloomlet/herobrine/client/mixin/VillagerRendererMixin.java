package com.bloomlet.herobrine.client.mixin;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.client.PossessedEyes;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The infected wear a different skin. Everyone else is untouched.
 *
 * Only the BASE texture is swapped, which is the whole trick. Villager clothing
 * and profession are drawn as separate layers on top of it, so an infected
 * librarian still wears the librarian's robe and still carries the librarian's
 * book. That is worth more than any amount of damage painted on: the thing
 * coming down the corridor at you had a job.
 *
 * Ordinary villagers never touch this path — the flag is set from the entity
 * during state extraction and is false for every villager in every village in
 * the world. Vanilla stays vanilla.
 */
@Mixin(VillagerRenderer.class)
public class VillagerRendererMixin {
	private static final Identifier INFECTED =
		HerobrineMod.id("textures/entity/infected/villager.png");

	@Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
	private void herobrine$infectedSkin(VillagerRenderState state,
	                                    CallbackInfoReturnable<Identifier> info) {
		if (((PossessedEyes)state).herobrine$infected()) {
			info.setReturnValue(INFECTED);
		}
	}
}
