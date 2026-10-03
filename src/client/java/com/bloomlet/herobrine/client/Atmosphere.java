package com.bloomlet.herobrine.client;


import net.minecraft.util.ARGB;

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

	/**
	 * WHETHER HIS SKY HAS BEEN CLEARED — he is dead, and the level says so. Every
	 * darkening below answers to this; and the layers added first in addLayers
	 * replace the timeline's pinned midnight with a day that turns with the
	 * overworld clock.
	 */
	static boolean cleared() {
		ClientLevel level = Minecraft.getInstance().level;
		return level != null
			&& level.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)
			&& Boolean.TRUE.equals(level.getAttached(com.bloomlet.herobrine.wrath.Wrath.CLEAR_SKY));
	}

	/** Where the overworld's day is right now, 0..1 round the clock, vanilla's own curve. */
	/**
	 * HOW FAR THE SKY HAS CLEARED, 0..1. It used to be a switch: the frame he died,
	 * the fog was gone and the sun was in its place. Now it is two minutes from the
	 * tick he fell — the pall thinning, the horizon coming back, the sun sliding
	 * into where it should have been — while the clock, set to late afternoon,
	 * gives this world its first sunset. A world whose sky has not moved in years
	 * should be seen to remember how.
	 */
	static float clearing() {
		if (!cleared()) {
			return 0.0F;
		}
		ClientLevel level = Minecraft.getInstance().level;
		Long since = level == null ? null : level.getAttached(com.bloomlet.herobrine.wrath.Wrath.CLEARED_AT);
		if (since == null) {
			return 1.0F;
		}
		return net.minecraft.util.Mth.clamp((level.getGameTime() - since) / (float) CLEARS_OVER, 0.0F, 1.0F);
	}

	private static final int CLEARS_OVER = 2400;

	private static float dayFraction() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return 0.25F;
		}
		double d = net.minecraft.util.Mth.frac(level.getOverworldClockTime() / 24000.0 - 0.25);
		double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
		return (float) ((d * 2.0 + e) / 3.0);
	}

	/** How bright the sky is at that hour, 0 (midnight) to 1 (noon). */
	private static float daylight() {
		float f = 1.0F - (float) (Math.cos(dayFraction() * Math.PI * 2.0) * 2.0 + 0.2);
		return 1.0F - net.minecraft.util.Mth.clamp(f, 0.0F, 1.0F);
	}

	private static final int DAY_SKY = ARGB.color(255, 120, 167, 255);
	private static final int NIGHT_SKY = ARGB.color(255, 10, 12, 30);
	private static final int DAY_FOG = ARGB.color(255, 192, 216, 255);
	private static final int NIGHT_FOG = ARGB.color(255, 12, 12, 22);
	private static final int DAY_CLOUD = ARGB.color(255, 255, 255, 255);
	private static final int NIGHT_CLOUD = ARGB.color(255, 40, 40, 60);

	public static void addLayers(EnvironmentAttributeSystem.Builder builder, ClientLevel level) {
		if (level.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			// A DAY OVER HIS WORLD, ONCE HE IS GONE. The timeline pins the sun at zero
			// and the sky at black; these come after it and, when the level says the
			// sky is clear, put the overworld's own day in its place — sun, moon,
			// stars, light and colour all turning with the overworld clock. Until then
			// they pass the values straight through.
			builder.addTimeBasedLayer(EnvironmentAttributes.SUN_ANGLE,
				(angle, tick) -> cleared() ? net.minecraft.util.Mth.rotLerp(clearing(), angle, dayFraction() * 360.0F) : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.MOON_ANGLE,
				(angle, tick) -> cleared() ? net.minecraft.util.Mth.rotLerp(clearing(), angle, (dayFraction() * 360.0F + 180.0F) % 360.0F) : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.STAR_ANGLE,
				(angle, tick) -> cleared() ? net.minecraft.util.Mth.rotLerp(clearing(), angle, dayFraction() * 360.0F) : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS,
				(bright, tick) -> cleared() ? net.minecraft.util.Mth.lerp(clearing(), bright, (1.0F - daylight()) * (1.0F - daylight()) * 0.5F) : bright);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR,
				(factor, tick) -> cleared() ? net.minecraft.util.Mth.lerp(clearing(), factor, Math.max(0.05F, daylight())) : factor);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_LEVEL,
				(lit, tick) -> cleared() ? net.minecraft.util.Mth.lerp(clearing(), lit, Math.max(0.05F, daylight())) : lit);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(clearing(), colour, ARGB.srgbLerp(daylight(), NIGHT_SKY, DAY_SKY)) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(clearing(), colour, ARGB.srgbLerp(daylight(), NIGHT_FOG, DAY_FOG)) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(clearing(), colour, ARGB.srgbLerp(daylight(), NIGHT_CLOUD, DAY_CLOUD)) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_START_DISTANCE,
				(distance, tick) -> cleared() ? net.minecraft.util.Mth.lerp(clearing(), distance, Math.max(distance, 160.0F)) : distance);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
				(distance, tick) -> cleared() ? net.minecraft.util.Mth.lerp(clearing(), distance, Math.max(distance, 480.0F)) : distance);
		}
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
			(distance, tick) -> distance * net.minecraft.util.Mth.lerp(clearing(), closeness(), 1.0F));
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_FOG_END_DISTANCE,
			(distance, tick) -> distance * net.minecraft.util.Mth.lerp(clearing(), closeness(), 1.0F));
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE,
			(distance, tick) -> distance * net.minecraft.util.Mth.lerp(clearing(), closeness(), 1.0F));

		// And the light, which was the real omission. Everything above changes
		// what colour the world is; none of it changes how BRIGHT the world is,
		// so a storm at SIEGE came out as a grey afternoon rather than as a
		// bad one. Vanilla's own thunder layer dims exactly these two, and
		// stopping short of them meant recolouring a scene that was still lit
		// like noon.
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR,
			(factor, tick) -> factor * (1.0F - (gloom(level) * (1.0F - clearing()))));
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_LEVEL,
			(lit, tick) -> lit * (1.0F - (gloom(level) * (1.0F - clearing())) * 0.7F));

		// Clouds come down. A ceiling you can nearly touch is oppressive in a
		// way a grey one is not, and it is the only attribute here that changes
		// the shape of the sky rather than its colour.
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_HEIGHT,
			(height, tick) -> height - 46.0F * pall());

		builder.addTimeBasedLayer(EnvironmentAttributes.MUSIC_VOLUME,
			(volume, tick) -> volume * net.minecraft.util.Mth.lerp(clearing(), loudness(), 1.0F));
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
	// ---- EVERYTHING BELOW IS HOW CLOSE HE IS, AND NOTHING ELSE ---------------
	//
	// Every one of these used to read the story phase: the light went out of the
	// sky a little more at each house, the fog closed in, the music sank, the rain
	// went red at the fourth and the whole overworld turned grey at the last — for
	// good, whether he was there or not. Nobody liked it, and phases now decide
	// which house comes next and nothing else. So it is all one number, near(),
	// which the server sends (Whereabouts.NEAR_HIS): how close the nearest of him
	// is. He comes, the world darkens and the rain runs red; he goes, it is
	// normal again. Same in his world as in yours.

	private static float gloom(ClientLevel level) {
		float base = DARK_AT_HIS * near();
		if (base <= 0.0F) {
			return 0.0F;
		}
		float weather = level.isThundering() ? 1.35F : level.isRaining() ? 1.15F : 1.0F;
		return Math.min(0.82F, base * weather);
	}

	private static float pall() {
		return Math.min(0.88F, GREY_AT_HIS * near()) * daylit() * (1.0F - clearing());
	}

	private static float daylit() {
		net.minecraft.client.multiplayer.ClientLevel level =
			net.minecraft.client.Minecraft.getInstance().level;
		if (level == null) {
			return 1.0F;
		}
		return Math.max(0.0F, 1.0F - level.getSkyDarken() / 11.0F);
	}

	private static float closeness() {
		return Math.max(0.6F, 1.0F - SHUTS_IN_AT_HIS * near());
	}

	private static float loudness() {
		return 1.0F - 0.55F * near();
	}

	public static int rainTint(int white) {
		float strength = near() * (1.0F - clearing());      // the rain runs red only while he is close
		if (strength <= 0.0F) {
			return white;
		}
		int alpha = ARGB.alpha(white);
		int red = Math.round(255 + (RAIN_RED[0] - 255) * strength);
		int green = Math.round(255 + (RAIN_RED[1] - 255) * strength);
		int blue = Math.round(255 + (RAIN_RED[2] - 255) * strength);
		return ARGB.color(alpha, red, green, blue);
	}

	public static float @org.jspecify.annotations.Nullable [] splashTint() {
		float strength = near();
		if (strength <= 0.0F) {
			return null;
		}
		return new float[] {
			1.0F,
			1.0F - 0.78F * strength,
			1.0F - 0.80F * strength,
		};
	}

	private static final int[] RAIN_RED = { 205, 58, 48 };
	private static final float GREY_AT_HIS = 0.55F;
	private static final float DARK_AT_HIS = 0.45F;
	private static final float SHUTS_IN_AT_HIS = 0.40F;

	/** How close the nearest of him is, 0 to 1, as the server last said. */
	private static float near() {
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().atmosphere) {
			return 0.0F;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return 0.0F;
		}
		Float in = client.player.getAttached(
			com.bloomlet.herobrine.manifest.Whereabouts.NEAR_HIS);
		return in == null ? 0.0F : net.minecraft.util.Mth.clamp(in, 0.0F, 1.0F);
	}

}
