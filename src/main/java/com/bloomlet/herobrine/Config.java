package com.bloomlet.herobrine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Everything in this mod, and a switch for it.
 *
 * DESIGN.md §9 has asked for this from the beginning and it was the last thing
 * standing between this being a thing one person plays and a thing anybody can.
 * Some people want the stare and none of the theft. Some people want the whole
 * of it except the part where he takes a wall out. A mod that answers "no" to
 * both of those is a mod with one user.
 *
 * FLAT AND BORING ON PURPOSE. One file, plain booleans and numbers, no
 * categories nested three deep and no dependency on a config library. Somebody
 * editing this at midnight because a hunt just cost them a door should be able
 * to find the line by reading it, and a missing or half-written file falls back
 * to the defaults rather than stopping the game.
 *
 * The defaults are the mod as designed. Nothing here is off out of the box.
 */
public final class Config {

	// ---- the whole thing ---------------------------------------------------
	/** Off, and nothing below matters. */
	public boolean enabled = true;
	/** How fast wrath climbs. 0.5 doubles the game's length; 2.0 halves it. */
	public double wrathRate = 1.0;

	// ---- what he does ------------------------------------------------------
	public boolean traces = true;
	public boolean theBreathing = true;
	public boolean theStare = true;
	public boolean signsAndPages = true;
	public boolean ruins = true;
	public boolean possession = true;
	public boolean theHunt = true;
	public boolean theDark = true;

	// ---- the things people actually argue about ----------------------------
	/**
	 * He mines through walls and doors during a hunt.
	 *
	 * The single most likely thing to want off, and the one exception to
	 * DESIGN §9. Everything he breaks drops, so nothing is lost but the wall —
	 * and some people still do not want a mod touching their build at all,
	 * which is entirely reasonable.
	 */
	public boolean breakIn = true;
	/** He takes torches. They drop; you can put them straight back. */
	public boolean takeTheLight = true;
	/** He leaves fires. Already refuses to light near anything flammable. */
	public boolean scorch = true;

	// ---- the world ---------------------------------------------------------
	public boolean weather = true;
	public boolean longerNights = true;
	/** The SIEGE night that never ends. Separable from longer nights. */
	public boolean endlessNight = true;
	/** Every animal turns on you at SIEGE. */
	public boolean hostileAnimals = true;
	/** Fog, sky colour, the red rain. Client-side only; changes no spawning. */
	public boolean atmosphere = true;

	// ---- what gets built ---------------------------------------------------
	public boolean houses = true;
	public boolean town = true;
	public boolean villageDecay = true;

	// ---- the ending --------------------------------------------------------
	/** He can be killed at SIEGE. Off means the mod has no ending. */
	public boolean theReckoning = true;
	/** How many blows he takes. The church still arrives a third of the way. */
	public int blowsToKill = 30;

	// ------------------------------------------------------------------------

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config active = new Config();

	public static Config get() {
		return active;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("herobrine.json");
	}

	/**
	 * Read it, or write the defaults out so there is something to edit.
	 *
	 * A broken file is reported and then ignored in favour of the defaults. The
	 * alternative — refusing to load — punishes somebody for a missing comma by
	 * taking their whole game away, and this is a horror mod rather than a
	 * compiler.
	 */
	public static void load() {
		Path path = file();
		try {
			if (Files.notExists(path)) {
				Files.createDirectories(path.getParent());
				Files.writeString(path, GSON.toJson(new Config()));
				HerobrineMod.LOGGER.info("wrote a default config to {}", path);
				return;
			}
			Config read = GSON.fromJson(Files.readString(path), Config.class);
			if (read != null) {
				active = read;
			}
			HerobrineMod.LOGGER.info("config loaded: enabled={} breakIn={} wrathRate={}",
				active.enabled, active.breakIn, active.wrathRate);
		} catch (IOException | JsonSyntaxException broken) {
			HerobrineMod.LOGGER.warn("config at {} could not be read, using defaults: {}",
				path, broken.getMessage());
		}
	}
}
