package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.entity.EntityRenderers;

public class HerobrineModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.HEROBRINE, HerobrineRenderer::new);
		EntityRenderers.register(ModEntities.INFECTED, InfectedRenderer::new);
		EntityRenderers.register(ModEntities.MIMIC, MimicRenderer::new);
		EntityRenderers.register(ModEntities.TURNED, TurnedRenderer::new);
		hisWeather();
	}

	/**
	 * IT IS ALWAYS RAINING IN HIS WORLD, AND ONLY THE CLIENT CAN SAY SO.
	 *
	 * Weather is server-wide in 26.2 — WeatherData is one field on
	 * MinecraftServer, and ServerLevel.getWeatherData() hands back the server's
	 * copy — so there is no honest way to storm one dimension. Forcing it would
	 * put a permanent thunderstorm over the player's own base as the price of a
	 * mood somewhere they visit at the end.
	 *
	 * But the RAIN LEVEL itself is a float on the Level object rather than on
	 * the server, and every renderer reads it: the weather effect pass, the
	 * splash particles, the sound loop and the sky darkening all come off
	 * getRainLevel. So the client can simply be told it is pouring, in one
	 * dimension, and the overworld never hears about it.
	 *
	 * Set every tick rather than once, because vanilla's own tickRain eases both
	 * values back toward whatever the server says the weather is — which is
	 * clear — and would fade this out over a few seconds.
	 */
	private static void hisWeather() {
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
			.register(client -> {
				if (client.level == null) {
					return;
				}
				if (!client.level.dimension().equals(
						com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
					return;
				}
				client.level.setRainLevel(1.0F);
				// Thunder level is what darkens the sky and deepens the sound;
				// short of one so the world is not quite as black as a vanilla
				// storm at midnight, which is dark enough to be unplayable.
				client.level.setThunderLevel(0.85F);
			});
	}
}
