package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.wrath.Phase;

import net.minecraft.util.ARGB;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
 * With colour and distance now agreeing, the fog is allowed to be heavy again.
 * A world reduced to forty per cent of its sight lines is a real thing to be
 * standing in — it was never the amount that was wrong, it was that the fog
 * and the sky it met were different colours and the seam gave it away.
 *
 * Nothing at all before TRESPASSER. The early phases have to be an ordinary
 * world with a few things wrong in it, and a world that had visibly changed
 * would answer the question the whole first act is built on.
 */
public final class Atmosphere {
	private Atmosphere() {}

	/**
	 * What the world drifts towards: cold, dark, and nearly colourless.
	 *
	 * ONE colour, used for the sky and the fog and the clouds alike, and that
	 * is the fix that lets the fog get heavy without looking painted on. Real
	 * distance fog is invisible as fog — you see it as the horizon dissolving —
	 * and that only works if the fog is the same colour as the sky it meets. The
	 * first version blended them toward different colours at different rates, so
	 * a seam appeared where the fogged ground met the sky, and a seam is exactly
	 * what the eye reads as fake.
	 *
	 * Converged like this, the far plane can come a long way in and still look
	 * like weather, because there is nothing to see except a world quietly
	 * running out.
	 */
	private static final int PALL = ARGB.color(255, 58, 60, 66);

	public static void addLayers(EnvironmentAttributeSystem.Builder builder, ClientLevel level) {
		// Same target, same strength, all three. They must agree.
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall(), colour, PALL));
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall(), colour, PALL));
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR,
			(colour, tick) -> ARGB.srgbLerp(pall(), colour, PALL));
		builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS,
			(brightness, tick) -> brightness * (1.0F - pall()));

		// Now the distance can do real work, because there is no longer a seam
		// for it to expose. All three move together — terrain, sky and cloud —
		// so nothing stays sharp while the rest goes soft.
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());

		// And the light, which was the real omission. Everything above changes
		// what colour the world is; none of it changes how BRIGHT the world is,
		// so a storm at SIEGE came out as a grey afternoon rather than as a
		// bad one. Vanilla's own thunder layer dims exactly these two, and
		// stopping short of them meant recolouring a scene that was still lit
		// like noon.
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR,
			(factor, tick) -> factor * (1.0F - gloom(level)));
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_LEVEL,
			(lit, tick) -> lit * (1.0F - gloom(level) * 0.7F));

		// Clouds come down. A ceiling you can nearly touch is oppressive in a
		// way a grey one is not, and it is the only attribute here that changes
		// the shape of the sky rather than its colour.
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_HEIGHT,
			(height, tick) -> height - 46.0F * pall());

		builder.addTimeBasedLayer(EnvironmentAttributes.MUSIC_VOLUME,
			(volume, tick) -> volume * loudness());
	}

	/**
	 * How dark it has got, 0 to 1.
	 *
	 * The only thing here that reads the weather, and it is what makes a storm
	 * at SIEGE the worst the world ever looks rather than just another grey
	 * day. Thunder is worth over half again, so the difference between a wet
	 * afternoon and a bad one is something the player sees rather than infers.
	 *
	 * Capped well short of black. A world the player cannot see to walk through
	 * is a handicap rather than a mood, and §9 rules that out — this is meant
	 * to be a sky you keep glancing at, not a reason to stop playing.
	 *
	 * Client-only, which is the happy accident that makes it safe: SKY_LIGHT_LEVEL
	 * is a gameplay attribute and feeds mob spawning on the server, but our
	 * layers exist only on the client. The world LOOKS darker without a single
	 * extra zombie, so the atmosphere costs the player nothing.
	 */
	private static float gloom(ClientLevel level) {
		float base = switch (phase()) {
			case RUMOUR, WATCHER -> 0.0F;
			case TRESPASSER -> 0.10F;
			case MIMIC -> 0.24F;
			case HUNTER -> 0.40F;
			case SIEGE -> 0.56F;
		};
		if (base <= 0.0F) {
			return 0.0F;
		}
		float weather = level.isThundering() ? 1.55F : level.isRaining() ? 1.2F : 1.0F;
		return Math.min(0.82F, base * weather);
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
	 * Pulled right back after playing with it. Heavy fog held up in a
	 * screenshot and did not survive an evening: it softens everything past
	 * twenty blocks, so the world reads as low resolution rather than as
	 * weather, and a sunset seen through it looks like a rendering fault rather
	 * than a sunset.
	 *
	 * Which returns to what vanilla already knew. Minecraft's own weather never
	 * touches a fog distance — it changes colour and light — and both times this
	 * has been pushed toward distance it has looked wrong. Three quarters at
	 * SIEGE is the most it gets now, and nothing at all before MIMIC.
	 *
	 * The colour and the darkness do the work. They always did.
	 */
	private static float closeness() {
		return switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER -> 1.0F;
			case MIMIC -> 0.95F;
			case HUNTER -> 0.85F;
			case SIEGE -> 0.75F;
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
	 * The rain, turned.
	 *
	 * WeatherEffectRenderer builds every raindrop quad with `ARGB.white(alpha)`
	 * — one hardcoded expression, which is the only reason this is possible at
	 * all. The texture is greyscale, so the vertex colour IS the colour of the
	 * rain, and the alpha carries the distance fade and must survive untouched.
	 *
	 * Held back until HUNTER, and that is the whole point of doing it as a
	 * gradient rather than a switch. Rain is rain, and a mod whose rain is red
	 * from the first afternoon has told the player everything on day one. When
	 * it does turn it should be the thing they cannot get anybody to believe:
	 * they looked up, and it was the wrong colour.
	 *
	 * @param white the ARGB the renderer was about to use
	 */
	public static int rainTint(int white) {
		float strength = switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER, MIMIC -> 0.0F;
			case HUNTER -> 0.6F;
			case SIEGE -> 1.0F;
		};
		if (strength <= 0.0F) {
			return white;
		}
		int alpha = ARGB.alpha(white);
		int red = Math.round(255 + (RAIN_RED[0] - 255) * strength);
		int green = Math.round(255 + (RAIN_RED[1] - 255) * strength);
		int blue = Math.round(255 + (RAIN_RED[2] - 255) * strength);
		return ARGB.color(alpha, red, green, blue);
	}

	/**
	 * Not blood, and deliberately not.
	 *
	 * Rain is thin and half transparent, so a deep arterial red simply goes
	 * dark and reads as dirty water. This is lighter and hotter than the colour
	 * anybody would pick on paper, because it has to survive being drawn at
	 * about a third opacity over whatever is behind it.
	 */
	private static final int[] RAIN_RED = { 205, 58, 48 };

	/**
	 * What the server last told this client.
	 *
	 * Read fresh every time rather than cached: these layers are evaluated per
	 * tick anyway, so the world changes the moment the phase does, without
	 * anything having to notice and rebuild.
	 */
	private static Phase phase() {
		// Switched off reports RUMOUR rather than being checked in six places.
		// Every layer here is already neutral at RUMOUR — that is the phase
		// whose whole job is to look like an ordinary world — so one gate at
		// the source turns the lot off and cannot be forgotten in one of them.
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().atmosphere) {
			return Phase.RUMOUR;
		}
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? Phase.RUMOUR : Wrath.shownTo(client.player);
	}
}
