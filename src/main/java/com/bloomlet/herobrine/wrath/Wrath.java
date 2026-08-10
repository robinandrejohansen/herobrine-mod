package com.bloomlet.herobrine.wrath;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The world's memory of you.
 *
 * Not a difficulty slider. Per LORE.md this is how much you have started to
 * look like the person his brother became — digging, hoarding, going deeper
 * than you need to. Everything he does is a reaction to this number.
 *
 * Stored as a Fabric attachment on the overworld rather than vanilla
 * SavedData: SavedDataType is a record requiring a vanilla DataFixTypes
 * constant, which a mod cannot supply honestly, and passing an unrelated one
 * would hand our data to a datafixer written for something else. Attachments
 * are built for exactly this and take a plain Codec.
 *
 * One number per save, held on the overworld, because a haunting that reset
 * when you stepped into the Nether would not be a haunting.
 */
public final class Wrath {
	private Wrath() {}

	private static final AttachmentType<Integer> TOTAL =
		AttachmentRegistry.createPersistent(HerobrineMod.id("wrath"), Codec.INT);

	/** Per-player share, so one player on a server cannot speak for everyone. */
	private static final AttachmentType<Integer> PLAYER_SHARE =
		AttachmentRegistry.createPersistent(HerobrineMod.id("wrath_share"), Codec.INT);

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

	public static int get(MinecraftServer server) {
		return server.overworld().getAttachedOrElse(TOTAL, 0);
	}

	public static int getShare(ServerPlayer player) {
		return player.getAttachedOrElse(PLAYER_SHARE, 0);
	}

	/**
	 * The story's own position, kept rather than derived.
	 *
	 * STORY is the phase ordinal; SINCE is the wrath total when it was entered,
	 * which is what {@link Phase#into} needs to ramp inside a phase.
	 */
	private static final AttachmentType<Integer> STORY = AttachmentRegistry
		.createPersistent(HerobrineMod.id("story"), Codec.INT);
	private static final AttachmentType<Integer> SINCE = AttachmentRegistry
		.createPersistent(HerobrineMod.id("story_since"), Codec.INT);

	/**
	 * HOW FAR INTO THE STORY, AND IT IS NO LONGER A FUNCTION OF WRATH.
	 *
	 * This used to be Phase.forWrath(get(server)), and that one line was the
	 * reason a group could play for days and miss the entire middle of the mod.
	 * Progress came from sleeping, mining and killing — things everybody does
	 * without meaning to — so the phases advanced whether or not anybody had ever
	 * walked into one of his buildings. The buildings were a REWARD for
	 * progressing, so progressing without them was the default.
	 *
	 * It is stored now, and only {@link #discovered} moves it. Wrath still exists
	 * and still matters: it is how angry he is RIGHT NOW, which sets how often
	 * and how hard the current phase's events land. Two dials — the story and
	 * his temper — and the player drives both, one by going deeper and one by
	 * disturbing things.
	 *
	 * MIGRATION IS THE DELICATE PART. A world saved before this change has no
	 * stored phase and a large wrath total, and reading zero would silently throw
	 * away a campaign. So the first read seeds from the old derivation, and the
	 * seed is a MAXIMUM against what discovery would give — nobody is ever
	 * demoted by installing this.
	 */
	public static Phase phase(MinecraftServer server) {
		Integer stored = server.overworld().getAttached(STORY);
		if (stored == null) {
			Phase seeded = Phase.forWrath(get(server));
			set(server, seeded);
			HerobrineMod.LOGGER.info("no stored story — seeded {} from {} wrath",
				seeded.name(), get(server));
			return seeded;
		}
		Phase[] all = Phase.values();
		return all[Math.max(0, Math.min(all.length - 1, stored))];
	}

	/** Wrath at the moment the current phase began. */
	public static int since(MinecraftServer server) {
		return server.overworld().getAttachedOrElse(SINCE, 0);
	}

	/**
	 * How deep into the current phase, nought to one.
	 *
	 * The single value every event should be scaling on. Early in a phase the
	 * new thing happens once and quietly; late in it, it is the weather.
	 */
	public static float into(MinecraftServer server) {
		return phase(server).into(get(server), since(server));
	}

	private static void set(MinecraftServer server, Phase phase) {
		server.overworld().setAttached(STORY, phase.ordinal());
		server.overworld().setAttached(SINCE, get(server));
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

	/**
	 * @param player may be null for world-level causes with no single culprit
	 */
	public static void add(MinecraftServer server, ServerPlayer player, int amount, Reason reason) {
		if (amount == 0) {
			return;
		}
		// The pace dial, and this is the only place it can go.
		//
		// Every single thing that moves wrath comes through here, so scaling it
		// once at the door means the whole arc stretches or compresses evenly —
		// sleeping, killing, defiance, the drift, all of it. Scaling at the call
		// sites would have meant thirty separate multiplications and one of them
		// forgotten.
		//
		// GAINS ONLY. The ending subtracts the entire total to put the world
		// back, and scaling that would leave a remainder behind — a world that
		// had been at 0.5 pace would come out of its ending still half way to
		// WATCHER, which is not an ending.
		double rate = Config.get().wrathRate;
		if (amount > 0 && rate != 1.0) {
			amount = Math.max(1, (int)Math.round(amount * rate));
		}
		Phase before = phase(server);

		int total = Math.max(0, get(server) + amount);
		server.overworld().setAttached(TOTAL, total);
		if (player != null) {
			player.setAttached(PLAYER_SHARE, Math.max(0, getShare(player) + amount));
		}

		Phase after = Phase.forWrath(total);
		if (after != before) {
			HerobrineMod.LOGGER.info("Wrath {} -> phase {} ({})", total, after.name(), reason.name());
		}
	}

	/** Why wrath moved. Logged, and later used to weight what he does about it. */
	public enum Reason {
		TIME,          // baseline drift; the world remembers you being in it
		KILL,          // he notices violence
		DEPTH,         // his territory
		SLEEP,         // you denied him the dark
		DEFIANCE,      // you broke something of his — the biggest single jump
		DEATH          // he is satisfied, briefly
	}
}
