package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HauntingSpawner;
import com.bloomlet.herobrine.entity.MimicEntity;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.entity.ConfinedPlacement;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;

/**
 * THE SEVENTH NAME IN A SIX-PLAYER GAME.
 *
 * Two halves, and they are independent on purpose. The entity is a figure with
 * somebody's skin on it; this is the paperwork that makes the server agree he
 * exists. Either alone is a good scare. Together they are the only thing in the
 * mod a player cannot resolve by themselves.
 *
 * WHO HE COPIES IS THE WHOLE TRICK, and the obvious choice is wrong. Copying
 * the player who is looking is a famous image and it fails instantly in
 * multiplayer, because seeing YOURSELF is unambiguous — you know where you are,
 * so you know what that is, and the event is over in a second with nothing left
 * to say.
 *
 * So he copies SOMEBODY ELSE WHO IS LOGGED IN, and preferably somebody a long
 * way off. Now it is not a puzzle you can solve by looking; it is a question you
 * have to ASK. "Are you at the base?" is the sentence this whole class exists to
 * produce, and the answer arriving over voice chat — "yeah, why?" — does more
 * than any texture could.
 *
 * Alone on the server, he copies you, because that version still works and
 * refusing to appear is worse.
 *
 * FREE, AND WITH NO API. The skin is not fetched from anywhere: every player's
 * signed texture property is already on this server, having arrived when they
 * logged in. We copy it onto a new profile with a new id and the same name. That
 * is also WHY he can only ever wear somebody who is currently online — a
 * limitation that turns out to be exactly the design, because a stranger's face
 * would mean nothing and a friend's means everything.
 *
 * THE NAME COLLIDES DELIBERATELY. The tab list is keyed by id, not by name, so
 * the fake sits in the list next to the real one with the same text — and the
 * list is sorted, so they land ADJACENT. Nobody has to be told to look.
 */
public final class Mimicry {
	private Mimicry() {}

	/**
	 * Every fake currently in somebody's tab list.
	 *
	 * Held here rather than on the entity because the entry has to be removable
	 * without the entity — a chunk unloading, a dimension change, a crash, a
	 * server stop. A name stuck in the tab list forever is the single worst bug
	 * this feature can produce: it is permanent, everyone sees it, and it makes
	 * the mod look broken rather than haunted.
	 */
	private static final List<UUID> listed = new ArrayList<>();

	public static void register() {
		// Belt and braces. If anything at all goes wrong between spawning and
		// vanishing, the list is still clean by the time anybody logs back in.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> clear(server));
	}

	/**
	 * @return true if somebody who is not on the server is now walking around
	 */
	public static boolean appear(ServerLevel level, ServerPlayer viewer) {
		MinecraftServer server = level.getServer();
		ServerPlayer subject = subject(level, viewer);
		if (subject == null) {
			return false;
		}

		RandomSource random = level.getRandom();
		BlockPos stand = somewhereVisible(level, viewer, random);
		if (stand == null) {
			return false;
		}

		// A new id with the same name and the same textures. The id is what makes
		// it a different row in the list; the name is what makes that row awful.
		UUID id = UUID.randomUUID();
		GameProfile real = subject.getGameProfile();
		GameProfile worn = new GameProfile(id, real.name(), real.properties());

		MimicEntity him = ModEntities.MIMIC.create(level, EntitySpawnReason.EVENT);
		if (him == null) {
			return false;
		}
		double dx = viewer.getX() - (stand.getX() + 0.5);
		double dz = viewer.getZ() - (stand.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		// Facing roughly the player on arrival and then never again. The first
		// frame is the only one where being looked at is useful — after that,
		// being looked THROUGH is the effect.
		him.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5,
			yaw + (random.nextFloat() - 0.5F) * 90.0F, 0.0F);
		him.wear(id, real.name());
		him.setLifetime(random);
		if (!level.addFreshEntity(him)) {
			return false;
		}

		add(server, worn, subject);
		listed.add(id);
		HerobrineMod.LOGGER.info("{} is also at [{}, {}, {}], {} blocks from {}",
			real.name(), stand.getX(), stand.getY(), stand.getZ(),
			(int)Math.sqrt(stand.distSqr(viewer.blockPosition())),
			viewer.getName().getString());
		return true;
	}

	/** Called when the entity goes, so the row goes with it. */
	public static void retire(MimicEntity him) {
		String raw = him.wearing();
		if (raw.isEmpty() || him.level().getServer() == null) {
			return;
		}
		try {
			UUID id = UUID.fromString(raw);
			listed.remove(id);
			remove(him.level().getServer(), List.of(id));
		} catch (IllegalArgumentException ignored) {
			// Nothing to take out of the list if it was never a valid id.
		}
	}

	private static void clear(MinecraftServer server) {
		if (listed.isEmpty()) {
			return;
		}
		remove(server, List.copyOf(listed));
		listed.clear();
	}

	/**
	 * Somebody else, and ideally somebody far away.
	 *
	 * The distance preference is doing real work. A copy of the person standing
	 * next to you is a bug report. A copy of the person who is down a mine four
	 * hundred blocks away is unfalsifiable until somebody speaks, and the moment
	 * of speaking is the event.
	 */
	private static @org.jspecify.annotations.Nullable ServerPlayer subject(
			ServerLevel level, ServerPlayer viewer) {
		List<ServerPlayer> others = new ArrayList<>();
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player != viewer && !player.isSpectator()) {
				others.add(player);
			}
		}
		if (others.isEmpty()) {
			// Alone: he wears you. A weaker version of the same idea, and far
			// better than declining to happen.
			return viewer;
		}
		ServerPlayer furthest = others.getFirst();
		double best = -1.0;
		for (ServerPlayer player : others) {
			// Cross-dimension counts as maximally far, which is correct: seeing
			// somebody in the overworld who is in the nether is the cleanest
			// impossibility available.
			double away = player.level() == level
				? player.distanceToSqr(viewer)
				: Double.MAX_VALUE;
			if (away > best) {
				best = away;
				furthest = player;
			}
		}
		return furthest;
	}

	/**
	 * Where he is standing when they first see him.
	 *
	 * Nearer than the stare and further than the glimpse. He has to be close
	 * enough to be recognisably a specific person — the skin and the nameplate
	 * are the entire payload and both are unreadable at sixty blocks — and far
	 * enough that walking over to him is a decision rather than a reflex.
	 */
	private static final double NEAR = 20.0;
	private static final double FAR = 40.0;

	private static @org.jspecify.annotations.Nullable BlockPos somewhereVisible(
			ServerLevel level, ServerPlayer viewer, RandomSource random) {
		for (int attempt = 0; attempt < 64; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double out = NEAR + random.nextDouble() * (FAR - NEAR);
			BlockPos at = BlockPos.containing(
				viewer.getX() + Math.cos(angle) * out,
				viewer.getY() + random.nextInt(7) - 3,
				viewer.getZ() + Math.sin(angle) * out);

			BlockPos stand = null;
			for (int down = 0; down <= 6 && stand == null; down++) {
				BlockPos maybe = at.below(down);
				if (ConfinedPlacement.canStand(level, maybe)) {
					stand = maybe;
				}
			}
			if (stand != null && HauntingSpawner.visibleFrom(level, viewer, stand)) {
				return stand;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// The paperwork
	// ------------------------------------------------------------------

	/**
	 * Put the row in, for everybody.
	 *
	 * Sent to the whole server rather than only to the player who is about to
	 * see him, and that is the point: the person whose face he is wearing sees
	 * their own name twice in their own tab list. There is no version of that
	 * which is not worse than what the viewer gets.
	 *
	 * The latency is copied off the real player. It is the one field somebody
	 * might actually check, and a green five-bar ping on a figure standing in a
	 * field would be the tell that unravels it.
	 */
	private static void add(MinecraftServer server, GameProfile worn, ServerPlayer like) {
		ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
			List.of());
		((com.bloomlet.herobrine.mixin.PlayerInfoEntriesAccessor)(Object)packet)
			.herobrine$setEntries(List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
				worn.id(), worn, true, like.connection.latency(),
				GameType.SURVIVAL, null, true, 0, null)));
		server.getPlayerList().broadcastAll(packet);
	}

	private static void remove(MinecraftServer server, List<UUID> ids) {
		server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(ids));
	}
}
