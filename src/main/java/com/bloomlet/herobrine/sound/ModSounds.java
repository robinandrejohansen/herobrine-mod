package com.bloomlet.herobrine.sound;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * HIS OWN VOICE, AND IT IS THE FIRST TIME HE HAS HAD ONE.
 *
 * Every sound in this mod was borrowed until now, and SIX OF THEM WERE THE
 * WARDEN'S — the heartbeat behind the rock, the noise he makes when he is hurt,
 * the noise he makes when he dies. The warden is one of the three most
 * recognisable sounds in the game, and a player who hears it does not think
 * "him", they think "warden". Forty hours of writing insists he is a PERSON and
 * the audio was arguing the opposite every time it played.
 *
 * SYNTHESISED RATHER THAN RECORDED, and that is a practical decision before it
 * is an aesthetic one: a synthesiser lives in the repository and a microphone
 * does not. No licence to track, nobody to credit, no binary anybody has to
 * trust, and the whole set regenerates from tools/gen_sounds.py in about a
 * second. Retuning a sound is editing a number rather than booking a studio.
 *
 * It also keeps them honest about what they are. Synthesis is good at drones,
 * sub-bass, breath and room tone and bad at voices and growls — so nothing here
 * attempts a roar, which suits him exactly. Everything he does in this mod is
 * pressure, weather and absence.
 *
 * VARIABLE RANGE on all of them, deliberately. A fixed-range event ignores the
 * volume it is played at when working out how far it carries, and half the
 * point of these is that the same sound is a whisper across a room and a
 * pressure in the chest at four blocks.
 */
public final class ModSounds {
	private ModSounds() {}

	/** Something breathing behind the rock, that is not behind the rock. */
	public static final SoundEvent BREATH = register("breath");

	/** He has been hurt, and the room gets heavier. Not a roar. */
	public static final SoundEvent ANGER = register("anger");

	/** The pressure leaving. Played once, ever, per world. */
	public static final SoundEvent GONE = register("gone");

	/** The bed under his world. Twenty-two seconds, seamless, streamed. */
	public static final SoundEvent HIS_WORLD = register("his_world");

	/** The frame closing over him. Thin and clean — white is his colour. */
	public static final SoundEvent THE_WAY = register("the_way");


	/**
	 * GOING THROUGH THE DOOR, which was vanilla's nether travel sound.
	 *
	 * TeleportTransition.PLAY_PORTAL_SOUND — the most recognisable two seconds of
	 * audio in Minecraft, on the one structure in this mod that exists to feel
	 * unprecedented. A player crossing into his world heard the thing they have
	 * heard every time they have ever gone to the nether, which undoes the frame,
	 * the texture and the whole ending in a single cue.
	 *
	 * ONE ASSET, PITCHED BOTH WAYS at the call site: down going out, up coming
	 * home. The same crossing is not the same experience in both directions and a
	 * second file would be the wrong way to say so — pitch carries direction on
	 * its own, and it means the two can never drift apart.
	 */
	public static final SoundEvent CROSSING = register("crossing");

	/**
	 * THE VILLAGER NOISE, WITH SOMETHING WRONG IN THE THROAT.
	 *
	 * Vanilla's "hmm" is the most recognisably friendly sound in Minecraft, which
	 * is the entire reason to ruin it. Same closed-mouth hum, two thirds the pitch,
	 * nasal formants only so it never becomes a groan — and a break in the middle
	 * where the note fails and comes back a semitone under.
	 *
	 * It is deliberately NOT a monster sound. Nothing about it growls or rasps: the
	 * moment it does, the player files it under "hostile mob" and stops listening.
	 * The first one has to be ambiguous — did that sound off, or am I imagining it —
	 * and it should only be obviously wrong once there are twelve of them.
	 */
	public static final SoundEvent HUM = register("hum");
	/**
	 * THE ENDING'S MUSIC — C418's "Minecraft", which is the client's own
	 * music/game/calm1.ogg, referenced by name in sounds.json and never shipped.
	 * The one track everybody knows, over the one moment the mod is built towards:
	 * the world that was his is yours again.
	 */
	public static final SoundEvent THE_ENDING = register("the_ending");


	// ---- AND IT COMES BACK OFF THE HILLS ----------------------------------
	/**
	 * PLAYED ONCE, HEARD FOUR TIMES.
	 *
	 * Hitting him made one noise at one point, at volume one — which in
	 * Minecraft is sixteen blocks of range, so the loudest thing that happens in
	 * the whole mod could not be heard from the far side of a field. It landed
	 * like a door closing.
	 *
	 * There are two ways to make a sound bigger and only one of them is any
	 * good. Putting more reverb in the FILE makes the sound longer, and a longer
	 * sound is just a longer sound — the player still hears one object, in one
	 * place, going on a bit. What actually reads as a landscape is the same
	 * sound arriving AGAIN, later, quieter, FROM SOMEWHERE ELSE. That is what an
	 * echo is: not a tail, a return.
	 *
	 * So the direct hit carries properly, and then two to four returns come back
	 * off the country from random bearings, each one further, later, quieter and
	 * a little duller. Minecraft has no filtering, so "duller" is done by
	 * dropping the pitch a few per cent per return — which is not what distance
	 * does physically and is exactly what it sounds like.
	 *
	 * AND IT READS THE ROOM. Under open sky the returns are thirty to eighty
	 * blocks out and up to a second apart, which is a valley. Underground or
	 * indoors they are close and fast, which is a cellar. The same call gives a
	 * hillside and a corridor without the caller knowing which it is in.
	 */
	private static final int RETURNS_OUTDOORS = 3;
	private static final int RETURNS_INDOORS = 2;

	public static void roll(net.minecraft.server.level.ServerLevel level,
	                        net.minecraft.core.BlockPos at, SoundEvent sound,
	                        float volume, float pitch) {
		// SIXTEEN BLOCKS PER POINT OF VOLUME is Minecraft's rule, and it is the
		// whole reason this was inaudible. The direct hit is loud so it carries
		// across open ground; the returns are what give it a size.
		level.playSound(null, at, sound, net.minecraft.sounds.SoundSource.HOSTILE,
			volume, pitch);

		net.minecraft.util.RandomSource random = level.getRandom();
		boolean open = level.canSeeSky(at);
		int returns = open
			? RETURNS_OUTDOORS + random.nextInt(2)
			: RETURNS_INDOORS;

		for (int i = 0; i < returns; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = open
				? 30.0 + i * 16.0 + random.nextDouble() * 14.0
				: 9.0 + i * 6.0 + random.nextDouble() * 5.0;
			final net.minecraft.core.BlockPos back = at.offset(
				(int)Math.round(Math.cos(angle) * range),
				open ? random.nextInt(9) - 2 : 0,
				(int)Math.round(Math.sin(angle) * range));
			// Roughly the time sound would take to get there and back, which is
			// far slower than real sound and much better to listen to.
			int delay = open
				? 6 + i * (7 + random.nextInt(6))
				: 3 + i * (2 + random.nextInt(3));
			final float faded = volume * (float)Math.pow(0.62, i + 1);
			final float duller = pitch * (float)Math.pow(0.955, i + 1);
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), delay,
				() -> level.playSound(null, back, sound,
					net.minecraft.sounds.SoundSource.HOSTILE, faded, duller));
		}
	}

	private static SoundEvent register(String name) {
		Identifier id = HerobrineMod.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id,
			SoundEvent.createVariableRangeEvent(id));
	}

	/** Called from the mod initialiser so the static block above runs. */
	public static void register() {
		HerobrineMod.LOGGER.info("his voice registered");
	}
}
