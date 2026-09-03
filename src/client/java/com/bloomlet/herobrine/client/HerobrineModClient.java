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
		EntityRenderers.register(ModEntities.GAUNT, GauntRenderer::new);
		EntityRenderers.register(ModEntities.COMPANION, CompanionRenderer::new);
		EntityRenderers.register(ModEntities.PLAYER_CORPSE, PlayerCorpseRenderer::new);
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
				// THE CLIENT FORCES THE RAIN OVER HIS WORLD — which is why the server's
				// "it is dry now" packets never showed: this line put it back every
				// tick. Once his sky is clear, it forces the opposite.
				boolean clear = Boolean.TRUE.equals(
					client.level.getAttached(com.bloomlet.herobrine.wrath.Wrath.CLEAR_SKY));
				client.level.setRainLevel(clear ? 0.0F : 1.0F);
				// Thunder level is what darkens the sky and deepens the sound.
				//
				// PULLED BACK FROM 0.85, and the reasoning that set it there was
				// sound and incomplete: it was chosen as "not quite as black as a
				// vanilla storm at midnight" against a dimension that had
				// ambient_light 0.0 and fog starting eight blocks away. Three
				// separate maximum-darkness settings in the same place do not add
				// up to atmosphere, they add up to a black screen — you could not
				// see the castle from the city, which is the one shot the whole
				// dimension is built for.
				client.level.setThunderLevel(clear ? 0.0F : 0.6F);
			});
	}
}
