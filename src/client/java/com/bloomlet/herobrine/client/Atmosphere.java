package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * The world goes wrong, in the way Minecraft's own weather goes wrong.
 *
 * The first attempt at this led with fog DISTANCE — shrink the far plane, watch
 * the horizon close in — and it read as a filter switched on over the top of
 * the game rather than as weather. Checking what vanilla actually does explains
 * why: its rain and thunder layers never touch a fog distance at all. Not once.
 * What they change is COLOUR and LIGHT — sky blended toward grey, fog
 * multiplied darker, clouds greyed, sky light dimmed, stars put out.
 *
 * So distance was never the game's vocabulary for this, and using it meant
 * speaking with an accent no player has ever heard from Minecraft. The rewrite
 * says the same thing in the language the game already uses: a sky that greys
 * off, fog that darkens and loses its colour, clouds that go to slate, stars
 * that stop. Vanilla thunder is exactly this and nobody has ever called a
 * thunderstorm fake.
 *
 * Distance survives only in a much smaller role, at the top two phases, as a
 * gentle pull rather than the main event — and every distance moves together
 * with the clouds and the sky, because the previous version scaled terrain fog
 * without them and left crisp clouds hanging over a fogged world, which is its
 * own kind of wrong.
 *
 * Nothing at all before TRESPASSER. The early phases have to be an ordinary
 * world with a few things wrong in it, and a world that had visibly changed
 * would answer the question the whole first act is built on.
 */
public final class Atmosphere {
	private Atmosphere() {}

	/** What everything drifts towards: cold, dark, and nearly colourless. */
	private static final int PALL = ARGB.color(255, 54, 56, 62);
	/** Fog goes slightly warmer than the sky, so the two do not flatten together. */
	private static final int HAZE = ARGB.color(255, 62, 58, 58);

	public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall() * 0.85F, colour, PALL));
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall() * 0.7F, colour, HAZE));
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall(), colour, PALL));
		builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS,
			(brightness, tick) -> brightness * (1.0F - pall()));

		// Distance, gently, and all three together so nothing is left sharp
		// while the rest of the world hazes.
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());

		builder.addTimeBasedLayer(EnvironmentAttributes.MUSIC_VOLUME,
			(volume, tick) -> volume * loudness());
	}

	/**
	 * How far the colour has gone over, 0 to 1.
	 *
	 * Blended toward rather than set, so biome and weather keep their say. A
	 * swamp still looks like a swamp and a storm still darkens it further; this
	 * only ever pulls whatever the world already decided a bit closer to grey.
	 * The moment it overrides instead of tinting, every biome starts looking
	 * the same and the world stops being a place.
	 */
	private static float pall() {
		return switch (phase()) {
			case RUMOUR, WATCHER -> 0.0F;
			case TRESPASSER -> 0.15F;
			case MIMIC -> 0.32F;
			case HUNTER -> 0.5F;
			case SIEGE -> 0.68F;
		};
	}

	/**
	 * And how much is left of the distance.
	 *
	 * Deliberately timid next to the colour. Two thirds at SIEGE is a hazy day,
	 * not a wall thirty blocks off — the previous third-of-normal was the part
	 * that gave the game away, because no weather in Minecraft has ever done
	 * that and the eye knows it.
	 */
	private static float closeness() {
		return switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER -> 1.0F;
			case MIMIC -> 0.92F;
			case HUNTER -> 0.8F;
			case SIEGE -> 0.66F;
		};
	}

	/**
	 * And how much of the music.
	 *
	 * Gone entirely by SIEGE. Held at full until MIMIC because the loss only
	 * registers if there was a long time when it was there — silence that
	 * arrives in the first hour is just a mod that forgot to play music.
	 */
	private static float loudness() {
		return switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER -> 1.0F;
			case MIMIC -> 0.6F;
			case HUNTER -> 0.25F;
			case SIEGE -> 0.0F;
		};
	}

	/**
	 * What the server last told this client.
	 *
	 * Read fresh every time rather than cached: these layers are evaluated per
	 * tick anyway, so the world changes the moment the phase does, without
	 * anything having to notice and rebuild.
	 */
	private static Phase phase() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? Phase.RUMOUR : Wrath.shownTo(client.player);
	}
}
