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
				(angle, tick) -> cleared() ? dayFraction() * 360.0F : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.MOON_ANGLE,
				(angle, tick) -> cleared() ? (dayFraction() * 360.0F + 180.0F) % 360.0F : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.STAR_ANGLE,
				(angle, tick) -> cleared() ? dayFraction() * 360.0F : angle);
			builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS,
				(bright, tick) -> cleared() ? (1.0F - daylight()) * (1.0F - daylight()) * 0.5F : bright);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR,
				(factor, tick) -> cleared() ? Math.max(0.05F, daylight()) : factor);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_LEVEL,
				(lit, tick) -> cleared() ? Math.max(0.05F, daylight()) : lit);
			builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(daylight(), NIGHT_SKY, DAY_SKY) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(daylight(), NIGHT_FOG, DAY_FOG) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR,
				(colour, tick) -> cleared() ? ARGB.srgbLerp(daylight(), NIGHT_CLOUD, DAY_CLOUD) : colour);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_START_DISTANCE,
				(distance, tick) -> cleared() ? Math.max(distance, 160.0F) : distance);
			builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
				(distance, tick) -> cleared() ? Math.max(distance, 480.0F) : distance);
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
		// AND HIS GROUND IS DARK WHATEVER CHAPTER IT IS.
		//
		// Added to the base rather than multiplied into the result, so it survives
		// RUMOUR — where the phase term is deliberately nought and a multiplier
		// would be nought as well. Walking up to his house on the first morning of
		// a world is meant to be the darkest thing in it.
		base += DARK_AT_HIS * near();
		// The early-out AFTER the addition. Above it, a RUMOUR world returned zero
		// before his ground had been consulted and the whole effect was dead in the
		// four chapters that need it most.
		if (base <= 0.0F) {
			return 0.0F;
		}
		// AND THE SECOND DIMMER, WHICH I MISSED THE FIRST TIME.
		//
		// Damping near() covered his GROUND and left the CHAPTER term at full
		// strength, and that term is much the bigger of the two: at SIEGE it is
		// 0.56, times 1.2 for the permanent rain, which takes SKY_LIGHT_FACTOR down
		// to about a fifth. On top of a dimension whose own light is already
		// authored down to almost nothing, that is the same darkness counted twice —
		// so the place would have gone black again by HUNTER no matter what the
		// dimension type said.
		//
		// His world does not need the mood system. It IS the mood.
		if (level.dimension().equals(
				com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			base *= HIS_SIDE;
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
		float base = switch (phase()) {
			case RUMOUR, WATCHER -> 0.0F;
			case TRESPASSER -> 0.15F;
			case MIMIC -> 0.32F;
			case HUNTER -> 0.5F;
			case SIEGE -> 0.68F;
		};
		// Capped short of full grey even at his door in a SIEGE storm. Past about
		// nine tenths the biome stops having any say at all and the world in front
		// of you is one flat colour, which stops being weather and starts being a
		// broken shader.
		return Math.min(0.88F, base + GREY_AT_HIS * near()) * daylit();
	}

	/**
	 * AND IT ONLY APPLIES WHILE THERE IS LIGHT TO GREY OUT.
	 *
	 * PALL is rgb(58, 60, 66) — a dark slate, correct for an overcast noon. The
	 * night sky is nearly black, around rgb(2, 3, 8). So lerping the NIGHT toward
	 * PALL can only do one thing, and it is the opposite of everything this class
	 * is for: at HUNTER it took the night sky to rgb(30, 31, 37) and at his door in
	 * a SIEGE storm to about rgb(51, 53, 59).
	 *
	 * Which is a sky that looks like heavy dusk while the clock says midnight — so
	 * the world reads as LIT and creepers spawn in it anyway. Reported exactly that
	 * way: "det er lyst, men mobs spawner". Not a spawning bug and not the clock.
	 * The atmosphere was quietly raising the black point of the night.
	 *
	 * getSkyDarken is vanilla's own answer to "how dark is it", eleven at midnight
	 * and zero at noon, and it already accounts for weather. Scaling by it leaves
	 * the overcast at full strength through the day, fades it out across dusk, and
	 * gives the night back to the night.
	 */
	private static float daylit() {
		net.minecraft.client.multiplayer.ClientLevel level =
			net.minecraft.client.Minecraft.getInstance().level;
		if (level == null) {
			return 1.0F;
		}
		return Math.max(0.0F, 1.0F - level.getSkyDarken() / 11.0F);
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
		float base = switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER -> 1.0F;
			case MIMIC -> 0.95F;
			case HUNTER -> 0.85F;
			case SIEGE -> 0.75F;
		};
		// THE ONE PLACE THE DISTANCE IS ALLOWED TO BE HEAVY.
		//
		// The note above is right that heavy fog everywhere reads as low resolution
		// rather than as weather — it was tried and it did not survive an evening.
		// What makes it work here is that it is LOCAL: there is unfogged world
		// behind you the whole way in, so the eye has the comparison and reads the
		// murk as a property of the place instead of a property of the game.
		//
		// Floored, not subtracted freely. Below about four tenths the house itself
		// starts disappearing, and the point is to walk toward it.
		// FLOORED HIGHER THAN IT WAS. Forty-two per cent of the render distance is
		// a wall of murk you walk into, and the note has come back twice now that
		// heavy fog reads as low quality rather than as weather. Sixty-five still
		// shuts the world in noticeably around his ground and leaves you able to see
		// the building you are walking toward.
		return Math.max(0.65F, base - SHUTS_IN_AT_HIS * near());
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
			// Turned back up. Cutting the music to a quarter by HUNTER read as
			// the game being broken rather than as dread — a server playing for
			// days had simply lost one of the best things about Minecraft, and
			// silence only means something if there is something to lose.
			//
			// It thins rather than disappears. Even SIEGE keeps a little, so
			// the quiet is a mood instead of a missing feature.
			case RUMOUR, WATCHER, TRESPASSER -> 1.0F;
			case MIMIC -> 0.9F;
			case HUNTER -> 0.7F;
			case SIEGE -> 0.45F;
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
	 * The same turn, as a multiplier for the splash particles.
	 *
	 * A different kind of number from rainTint. That one replaces a colour;
	 * this one multiplies a sprite which is already a pale blue, so the green
	 * and blue channels have to be pulled DOWN rather than a red being pushed
	 * up — the same red arrived at from the opposite direction.
	 *
	 * @return null below HUNTER, when the rain is ordinary and so is the splash
	 */
	public static float @org.jspecify.annotations.Nullable [] splashTint() {
		float strength = switch (phase()) {
			case RUMOUR, WATCHER, TRESPASSER, MIMIC -> 0.0F;
			case HUNTER -> 0.6F;
			case SIEGE -> 1.0F;
		};
		if (strength <= 0.0F) {
			return null;
		}
		return new float[] {
			1.0F,
			1.0F - 0.78F * strength,
			1.0F - 0.80F * strength,
		};
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
	// ---- HIS GROUND -------------------------------------------------------
	//
	// One number from the server — see Whereabouts.NEAR_HIS — and it goes into the
	// same three layers the phase drives. Which is the whole reason this was worth
	// doing as an input rather than as a separate effect: the fog, the colour and
	// the light already agree with each other, and a second system painting its own
	// murk on top would have put a seam back exactly where the comment at the top
	// of this file explains it was removed from.

	/** How far toward grey his ground pulls the world. */
	private static final float GREY_AT_HIS = 0.55F;
	/** How much of the light it takes. */
	private static final float DARK_AT_HIS = 0.30F;
	/** And how much of the view distance closes up. */
	private static final float SHUTS_IN_AT_HIS = 0.50F;

	/**
	 * Nought to one. Nought everywhere but the ground around his house, his tower,
	 * and the keep on the other side.
	 *
	 * No coordinates ever reach the client. It is handed a single float and has no
	 * way to turn it back into a position, which is deliberate: "distance to his
	 * house" on the client would be one mod away from being a compass.
	 */
	/**
	 * HALF STRENGTH ON HIS OWN SIDE, AND THAT IS NOT A COMPROMISE.
	 *
	 * These three terms were built against the OVERWORLD — a bright baseline with
	 * plenty of light and distance to eat into, where taking half the view and a
	 * third of the light is the difference between a field and a bad field. His
	 * world starts with none of that: the dimension type already sets its own fog,
	 * its own near-black ambient light and a permanent storm. Applying the full
	 * overworld ramp on top of that is not atmosphere on atmosphere, it is the same
	 * effect counted twice, and what came out the other side was unnavigable.
	 *
	 * So the keep still murks up as you approach it — that part is right and it is
	 * the same language as his house — at about half the amount, because everything
	 * underneath it is already doing the job.
	 */
	private static final float HIS_SIDE = 0.45F;

	private static float near() {
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().atmosphere || cleared()) {
			return 0.0F;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return 0.0F;
		}
		Float in = client.player.getAttached(
			com.bloomlet.herobrine.manifest.Whereabouts.NEAR_HIS);
		if (in == null) {
			return 0.0F;
		}
		float held = net.minecraft.util.Mth.clamp(in, 0.0F, 1.0F);

		// AND IT FOLLOWS THE STORY, WHICH IT NEVER DID.
		//
		// This ramp ran at full strength from the first minute of a world: walk
		// within two hundred blocks of the homestead at RUMOUR and the sky went
		// fifty-five per cent grey and the fog shut to under half its distance.
		//
		// RUMOUR is the phase whose entire job is to look like an ordinary world —
		// Skies.wetFraction returns 0.00 for it, deliberately, with a comment saying
		// so — and then this contradicted it at the one building the player is
		// guaranteed to walk to first. The mod said "nothing is wrong yet" and its
		// own atmosphere said otherwise, loudly, in the same field.
		//
		// A quarter at RUMOUR, all of it by SIEGE. The place is still wrong on the
		// first visit; it is wrong the way a cold room is wrong rather than the way
		// a storm is.
		held *= switch (phase()) {
			case RUMOUR -> 0.25F;
			case WATCHER -> 0.45F;
			case TRESPASSER -> 0.65F;
			case MIMIC -> 0.8F;
			case HUNTER -> 0.92F;
			case SIEGE -> 1.0F;
		};

		return client.level.dimension().equals(
			com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)
			? held * HIS_SIDE : held;
	}

	private static Phase phase() {
		// Switched off reports RUMOUR rather than being checked in six places.
		// Every layer here is already neutral at RUMOUR — that is the phase
		// whose whole job is to look like an ordinary world — so one gate at
		// the source turns the lot off and cannot be forgotten in one of them.
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().atmosphere || cleared()) {
			return Phase.RUMOUR;
		}
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? Phase.RUMOUR : Wrath.shownTo(client.player);
	}
}
