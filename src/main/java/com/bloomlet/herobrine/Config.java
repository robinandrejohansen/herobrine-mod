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

	// ---- what he does ------------------------------------------------------
	public boolean traces = true;
	/**
	 * The marks from the original story: stripped groves, sand pyramids in the
	 * ocean, 2x2 tunnels, a redstone torch in a cave, a passage bricked up.
	 *
	 * Off leaves the mod's own inventions and removes the quotations. Worth
	 * having as its own switch because these are the only things here that
	 * change the world permanently without ever being an event.
	 */
	public boolean signs = true;
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
	/**
	 * The hunt takes the house apart around you: glass, then lights, then the
	 * treeline, then holes in the ground.
	 *
	 * Off leaves the pursuit exactly as it was — he still comes, he still breaks
	 * in, he still cannot be outwalked — and the house is simply left alone. The
	 * chapter still completes, because what sites the church is surviving a
	 * hunt rather than being wrecked by one.
	 */
	public boolean huntWrecks = true;
	/**
	 * And the treeline rung is real lightning that really burns.
	 *
	 * Its own switch, separate from the rest of the ladder, because it is the
	 * only part of the hunt that can spread. It is already refused within
	 * fourteen blocks of any player and twenty-four of anybody's house, so what
	 * burns is a wood at a distance — but "no fire near my base ever" is a
	 * reasonable thing to want and should not cost somebody the whole event.
	 */
	public boolean huntFire = true;
	/**
	 * HOW MUCH DAMAGE drives him off a hunt. Not how many hits.
	 *
	 * It was three blows, and three blows is not a fight — it is three taps, and
	 * because he teleports out of reach after every one of them the skill it
	 * actually tested was patience. A stone sword and a netherite axe drove him
	 * off in exactly the same three swings, so nothing a player had done all game
	 * mattered here, which is the one place it should.
	 *
	 * SIXTY, ACROSS THREE WAVES — ten, then twenty, then thirty. The total is the
	 * only number here because the three shares come off it: cross a sixth of it
	 * and he withdraws and sends the first wave, cross half and he sends a bigger
	 * one, and the last thirty are what actually drives him off.
	 *
	 * That shape is why it is not one threshold any more. Forty in one block meant
	 * a hunt was over the first time anything went wrong, and a mercy at three
	 * hearts ended one at SEVEN damage in — which read, correctly, as the shortest
	 * hunt in the game. Now the early exits move him to the next phase instead of
	 * out of the event.
	 *
	 * (The old `blowsToBreakOff` is gone. An existing config file simply has a
	 * key nothing reads any more, and this defaults in beside it.)
	 */
	public double damageToBreakOff = 60.0;
	/** He leaves fires. Already refuses to light near anything flammable. */
	public boolean scorch = true;
	/**
	 * Act three lightning is REAL: it burns and it hurts.
	 *
	 * The one setting here that can cost somebody a build, and it is on by
	 * design — the ending is meant to leave a mark on the world. Off makes
	 * every bolt cosmetic, which is what it was before, and the fight is still
	 * a fight.
	 */
	public boolean realLightning = true;

	// ---- the world ---------------------------------------------------------
	public boolean weather = true;
	public boolean longerNights = true;
	/** The SIEGE night that never ends. Separable from longer nights. */
	public boolean endlessNight = true;
	/**
	 * One of the villagers has gone wrong.
	 *
	 * Nothing is taken from a village to do it — an extra person is added
	 * rather than a resident replaced — so this costs nobody a trade. Off for
	 * anybody who wants the village to stay the one place in the world where
	 * nothing happens.
	 */
	public boolean theTurning = true;
	/**
	 * What is already living in his world: armed skeletons that throw fire,
	 * zombies in enchanted plate, charged creepers, and his eyes on all of it.
	 *
	 * Off leaves the dimension's mobs entirely vanilla. The place is still dark,
	 * still storming and still his — it simply is not defended.
	 */
	public boolean hisHost = true;
	/**
	 * The keep — the one competent building he ever put up, and the only thing
	 * standing in his world.
	 *
	 * Off leaves the dimension as bare forest. There is nothing to find in it
	 * then, which is a reasonable thing to want only if you are testing.
	 */
	public boolean hisKeep = true;
	/** Every animal turns on you at SIEGE. */
	public boolean hostileAnimals = true;
	/**
	 * NOBODY IS TOLD WHEN SOMEBODY DIES.
	 *
	 * The death message is the one thing in vanilla that settles an argument for
	 * everybody at once, in yellow, with the cause spelled out — and this whole
	 * mod is built on nobody being able to corroborate anything. You still get
	 * your own death screen; the rest of the server gets nothing, and the only
	 * way they find out is somebody saying it out loud.
	 *
	 * ALL deaths, not only his. If only his were quiet then silence would MEAN
	 * him, which is a perfect notification for the one thing nobody should ever
	 * be certain about.
	 *
	 * Off restores vanilla exactly. Worth having as a switch because a server
	 * that runs on chat rather than voice will genuinely want the messages.
	 */
	public boolean quietDeaths = true;
	/** Fog, sky colour, the red rain. Client-side only; changes no spawning. */
	public boolean atmosphere = true;

	// ---- what gets built ---------------------------------------------------
	public boolean houses = true;
	public boolean town = true;
	public boolean villageDecay = true;

	/**
	 * He wears the skin and name of somebody who is logged in.
	 *
	 * Given its own switch rather than riding on possession, because it is the
	 * one event here that some groups will want off on principle — impersonating
	 * a real person by name is a different kind of thing from a haunted cow, and
	 * a server that is fine with the second may not be fine with the first.
	 */
	public boolean theStranger = true;

	/**
	 * He takes things out of your chests.
	 *
	 * Nothing is ever deleted — it comes back in possessed animals, in graves
	 * after a hunt, and in the late houses. The switch exists anyway, because
	 * "a mod may touch my chests" is a decision an owner should make out loud.
	 */
	public boolean theTaking = true;

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
			HerobrineMod.LOGGER.info("config loaded: enabled={} breakIn={} damageToBreakOff={}",
				active.enabled, active.breakIn, active.damageToBreakOff);
		} catch (IOException | JsonSyntaxException broken) {
			HerobrineMod.LOGGER.warn("config at {} could not be read, using defaults: {}",
				path, broken.getMessage());
		}
	}
}
