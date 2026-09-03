package com.bloomlet.herobrine.wrath;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * WHICH CHAPTER THE WORLD IS IN. Nothing else.
 *
 * This file used to hold two things that both claimed to be progress, and the
 * second one had to go. There was the story — six chapters, moved by walking
 * into one of his buildings — and there was WRATH, a running total fed by
 * sleeping, mining, killing and provoking him, with six thresholds of its own.
 *
 * The thresholds had already been disconnected when the story moved onto
 * discovery, and what was left behind was worse than dead code. Four hundred
 * and forty-five lines, a six-value reason enum, a per-player share, a save
 * migration and a pace multiplier reached gameplay in exactly ONE place: the
 * gap between manifestations, shortened by up to forty percent. Nobody can feel
 * forty percent. Meanwhile Phase.forWrath was still being called to LOG a phase
 * transition, so a playtest was told the story had advanced to WATCHER four
 * separate times while it was demonstrably still in RUMOUR.
 *
 * So the number is gone and the one behaviour worth having went to {@link Heat},
 * which is short, per-player, and falls — see the reasoning there.
 *
 * Stored as a Fabric attachment on the overworld rather than vanilla SavedData:
 * SavedDataType is a record requiring a vanilla DataFixTypes constant, which a
 * mod cannot supply honestly, and passing an unrelated one would hand our data
 * to a datafixer written for something else. Attachments are built for exactly
 * this and take a plain Codec.
 *
 * On the overworld rather than per-dimension, because a haunting that reset when
 * you stepped into the Nether would not be a haunting.
 */
public final class Wrath {
	private Wrath() {}

	/**
	 * The phase, told to the client.
	 *
	 * Wrath lives on the server and the client has no way to ask. Everything
	 * atmospheric — fog, the music, the colour of the sky — is drawn on the
	 * client and has to know how bad things have got, so the number is pushed
	 * out to each player and read back from there.
	 *
	 * The ordinal rather than the wrath total, deliberately. The client has no
	 * business knowing the exact figure — it would be one tooltip away from
	 * turning a thing the player feels into a number they optimise — and the
	 * phase is all the atmosphere needs.
	 */
	public static final AttachmentType<Integer> SHOWN_PHASE = AttachmentRegistry
		.<Integer>builder()
		.persistent(Codec.INT)
		.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_INT.cast(),
			net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(HerobrineMod.id("shown_phase"));

	/** Ticks between checks that every player has been told. Cheap, and covers joins. */
	private static final int TELL_INTERVAL = 40;

	private static int tickCounter;

	/**
	 * Forces this class to initialise during mod setup.
	 *
	 * The AttachmentTypes above are created by the static initialiser, which
	 * Java only runs when the class is first touched. Nothing touched it until
	 * the first wrath event — which happens AFTER the world has loaded — so on
	 * load Fabric found saved data referring to types it had never heard of,
	 * logged "unknown attachment type", and discarded it. Every session
	 * silently started from zero.
	 *
	 * Called from the mod initialiser purely for this side effect.
	 */
	/**
	 * @return the phase this player has been told about, for client-side use
	 */
	public static Phase shownTo(net.minecraft.world.entity.player.Player player) {
		Integer ordinal = player.getAttached(SHOWN_PHASE);
		if (ordinal == null || ordinal < 0 || ordinal >= Phase.values().length) {
			return Phase.RUMOUR;
		}
		return Phase.values()[ordinal];
	}

	private static void tell(MinecraftServer server) {
		if (++tickCounter % TELL_INTERVAL != 0) {
			return;
		}
		int ordinal = phase(server).ordinal();
		for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				Integer known = player.getAttached(SHOWN_PHASE);
				if (known == null || known != ordinal) {
					player.setAttached(SHOWN_PHASE, ordinal);
				}
			}
		}
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
			.register(Wrath::tell);
		HerobrineMod.LOGGER.debug("wrath attachments registered");
	}

	/** The story's own position, kept rather than derived. */
	private static final AttachmentType<Integer> STORY = AttachmentRegistry
		.createPersistent(HerobrineMod.id("story"), Codec.INT);

	/**
	 * WHICH CHAPTER, and only {@link #discovered} moves it.
	 *
	 * The migration that used to live here seeded a missing story from the old
	 * wrath total, so that a world saved before discovery-gating did not lose its
	 * campaign. It is gone with the total it read — there is nothing left to seed
	 * from, and a world old enough to need it has been through several releases
	 * since. A missing story is a new world, and a new world starts at RUMOUR,
	 * which is the correct and only honest answer now.
	 */
	public static Phase phase(MinecraftServer server) {
		Integer stored = server.overworld().getAttached(STORY);
		if (stored == null) {
			set(server, Phase.RUMOUR);
			HerobrineMod.LOGGER.info("no stored story — starting at RUMOUR");
			return Phase.RUMOUR;
		}
		Phase[] all = Phase.values();
		return all[Math.max(0, Math.min(all.length - 1, stored))];
	}

	/**
	 * How long a phase must be LIVED IN before the next place will site.
	 *
	 * This exists because discovery-gated progress has the
	 * exact opposite failure of wrath-gated progress. Wrath let a group advance
	 * without ever finding a building. Discovery lets a LUCKY group find three
	 * buildings in an hour and receive the thinnest possible version of all
	 * three phases — one manifestation each, the ramp never leaving nought, and
	 * RUMOUR over before anybody had time to disbelieve anything. Both failures
	 * end with the players not having seen the mod.
	 *
	 * So finding a place still advances the story immediately — that part was
	 * right. What it does NOT do is make the next place exist. The next one is
	 * sited only once this phase has had its time, which puts a floor under every
	 * chapter without putting a ceiling on anything.
	 *
	 * MEASURED IN GAME TIME RATHER THAN IN EVENTS SEEN, which was the first
	 * attempt and is wrong: manifestations are per-player, so six people would
	 * burn through an event quota in minutes and the floor would do nothing on
	 * exactly the servers it matters most on. Elapsed time behaves identically
	 * for one player and for six.
	 *
	 * The figure itself lives on {@link Phase}, because it is not one number —
	 * it climbs from twenty minutes to an hour as the chapters get fuller.
	 */
	private static final AttachmentType<Long> ENTERED = AttachmentRegistry
		.createPersistent(HerobrineMod.id("story_entered"), Codec.LONG);

	// settled() and owed() lived here and have gone with the gates that used them.
	// Nothing waits on a chapter having had its minutes any more — a place sites
	// when the one before it has been found and that is the whole of it. ENTERED is
	// still stamped by set(), because it costs nothing and it is the only record of
	// when a chapter began if anything ever wants one again.

	/** Minutes still owed to this chapter, for /herobrine status. */
	private static void set(MinecraftServer server, Phase phase) {
		server.overworld().setAttached(STORY, phase.ordinal());
		server.overworld().setAttached(ENTERED, server.overworld().getGameTime());
	}

	/**
	 * SOMEBODY FOUND ONE OF HIS PLACES, so the story moves.
	 *
	 * The only thing in the mod that advances a phase. Called from Dwellings the
	 * moment a building is marked found, and it steps exactly one phase — so the
	 * six buildings and the six phases are now the same sequence rather than two
	 * sequences that were supposed to stay in step and did not.
	 *
	 * Never goes backwards and never skips.
	 */
	/**
	 * Put the story wherever a tester asks for it.
	 *
	 * THE MISSING COMMAND, and its absence was worse than an inconvenience: the
	 * only way to see HUNTER was to play to HUNTER, so the most complicated event
	 * in the mod could only be reached through three hours of the four before it.
	 * That is why the hunt shipped with a hit window that had never once run for a
	 * player on their own.
	 *
	 * Backwards as well as forwards, deliberately. Half of testing a chapter is
	 * watching the one before it hand over.
	 */
	/**
	 * REMOVED HEROBRINE.
	 *
	 * The patch note, the joke, and the ending — and until now it had no bit. He
	 * died, the rain stopped for five minutes, and then Nights held the clock at
	 * midnight again because the story was still SIEGE, Skies rolled another
	 * storm, TheTurning added another villager to the town, and Whereabouts stood
	 * a fresh one of him over the keep. The world went on being haunted by a man
	 * whose body was on the floor of his own hall.
	 *
	 * The story stays where it is — SIEGE is what they earned and the books say
	 * so — but this is the switch every tick handler that makes the world WRONG
	 * checks first. Set once, by his death, and never cleared.
	 */
	private static final AttachmentType<Boolean> REMOVED = AttachmentRegistry
		.createPersistent(HerobrineMod.id("removed"), Codec.BOOL);

	public static boolean removed(MinecraftServer server) {
		return Boolean.TRUE.equals(server.overworld().getAttached(REMOVED));
	}

	public static void remove(MinecraftServer server) {
		server.overworld().setAttached(REMOVED, true);
		HerobrineMod.LOGGER.info("Removed Herobrine.");
	}

	/** The ending, undone — for /herobrine boss, so he can be fought again. */
	public static void restore(MinecraftServer server) {
		if (removed(server)) {
			server.overworld().removeAttached(REMOVED);
			HerobrineMod.LOGGER.info("un-removed Herobrine, by command");
		}
	}

	/**
	 * THE SKY OVER HIS WORLD IS CLEAR. Synced to clients, because the darkness over
	 * there is drawn by the client: a timeline pins the sun, Atmosphere thickens the
	 * fog by phase, and the client itself forces the rain level every tick. None of
	 * that is reachable from the server except by telling the client to stop —
	 * which is what this attachment on his level is. Set by his death; persistent.
	 */
	public static final AttachmentType<Boolean> CLEAR_SKY = AttachmentRegistry
		.<Boolean>builder()
		.persistent(Codec.BOOL)
		.syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL.cast(),
			net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("clear_sky"));

	public static void jumpTo(MinecraftServer server, Phase phase) {
		set(server, phase);
		HerobrineMod.LOGGER.info("story set to {} by command", phase.name());
	}

	public static void discovered(MinecraftServer server) {
		Phase now = phase(server);
		Phase[] all = Phase.values();
		if (now.ordinal() + 1 >= all.length) {
			return;
		}
		Phase next = all[now.ordinal() + 1];
		set(server, next);
		HerobrineMod.LOGGER.info("a place was found — the story moves to {}", next.name());
	}

}
