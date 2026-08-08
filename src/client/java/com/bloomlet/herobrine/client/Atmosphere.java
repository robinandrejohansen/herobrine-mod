package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * The world closes in.
 *
 * Two effects, and between them they do something nothing else in the mod can:
 * they change the world itself rather than putting something in it. Every other
 * manifestation is an event a player could have missed. These are conditions
 * they are inside, all the time, with no moment they could point at.
 *
 * FOG is the only thing available that makes the world SMALLER. Rain you look
 * through; fog takes the render distance away and turns the horizon into
 * whatever is thirty blocks off. A player at SIEGE is walking around a world
 * the size of a room, and everything the mod does at a distance — the figure at
 * the treeline, the animal that stopped — now happens at the edge of what they
 * can see instead of comfortably beyond it.
 *
 * THE MUSIC STOPS, and it is the cheapest frightening thing in the entire
 * project: one number, falling to nothing. Minecraft's ambient music is so
 * constant that players stop hearing it, which is exactly why taking it away
 * works. Nobody notices silence arriving. They notice, an hour later, that they
 * have been tense for a while and cannot say why.
 *
 * Nothing happens before TRESPASSER. The early phases have to be an ordinary
 * world with a few things wrong in it, and a world that had visibly changed
 * would be answering the question the whole first act is built on.
 */
public final class Atmosphere {
	private Atmosphere() {}

	public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_FOG_END_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_START_DISTANCE,
			(distance, tick) -> distance * closeness());
		builder.addTimeBasedLayer(EnvironmentAttributes.MUSIC_VOLUME,
			(volume, tick) -> volume * loudness());
	}

	/**
	 * How much of the world is left, as a fraction of what it should be.
	 *
	 * Applied to the distances rather than set outright, so biome and weather
	 * keep their say. A foggy swamp in a storm at SIEGE is still fogger than a
	 * clear desert at SIEGE, which is what stops this reading as a filter
	 * switched on over the top of the game.
	 */
	private static float closeness() {
		return switch (phase()) {
			case RUMOUR, WATCHER -> 1.0F;
			case TRESPASSER -> 0.85F;
			case MIMIC -> 0.7F;
			case HUNTER -> 0.5F;
			case SIEGE -> 0.35F;
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
