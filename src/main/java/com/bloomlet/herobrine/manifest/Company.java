package com.bloomlet.herobrine.manifest;

import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.CompanionEntity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * KEEPING HER, AND WHAT SHE NOTICES.
 *
 * CompanionEntity is the creature: it walks, it runs, it breaks off, it eats.
 * This is everything about her that needs to know about the WORLD rather than
 * about her own body — what she remarks on, and what happens to her when you
 * die.
 *
 * WHY THIS IS NOT IN THE ENTITY. Two reasons, and the second is the real one.
 * A goal cannot see a death event. And a per-tick scan of everything nearby,
 * run from inside a mob, is a scan per mob — this runs once per player per
 * SPEAKS_EVERY ticks no matter how many of her there are.
 */
public final class Company {
	private Company() {}

	/** How often she is allowed to notice anything at all. */
	private static final int LOOKS_EVERY = 40;

	/** How near a thing has to be before she has an opinion about it. */
	private static final double NOTICES = 20.0;

	/**
	 * How long she stands where you fell before she comes to find you.
	 *
	 * Four minutes. Long enough that going back for your things is the obvious
	 * move and she is genuinely there waiting when you arrive — that is the beat
	 * the whole thing exists for. Short enough that a player who died somewhere
	 * unreachable, or who simply logged off, has not lost her.
	 */
	private static final long WAITS_FOR = 4800L;

	/** Where she is standing vigil, and since when. Not persisted; see below. */
	private static final java.util.Map<java.util.UUID, Long> WAITING =
		new java.util.HashMap<>();

	public static void listen() {
		ServerTickEvents.END_SERVER_TICK.register(Company::tick);

		// SHE STAYS WHERE YOU FELL.
		//
		// Asked for as a choice between hiding at the death site and running to
		// the player, and it is better as both in sequence: standing over the spot
		// is what a person does, and it puts her and your items in the same place,
		// so the trip back is one trip. The walk afterwards is only the guarantee
		// that she cannot be stranded.
		ServerLivingEntityEvents.AFTER_DEATH.register((died, source) -> {
			if (!(died instanceof ServerPlayer fallen)
				|| !(fallen.level() instanceof ServerLevel here)) {
				return;
			}
			for (CompanionEntity her : hers(here, fallen)) {
				WAITING.put(her.getUUID(), here.getGameTime());
				her.getNavigation().stop();
				com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, fallen,
					com.bloomlet.herobrine.entity.Sayings.YOU_DIED);
				HerobrineMod.LOGGER.info("{} is standing over where {} fell at [{}, {}, {}]",
					her.getName().getString(), fallen.getName().getString(),
					her.getBlockX(), her.getBlockY(), her.getBlockZ());
			}
		});
	}

	/**
	 * TWO BOUNDED LOOKS, NOT ONE ENORMOUS ONE.
	 *
	 * The first version of this inflated an AABB to thirty million blocks and asked
	 * every level for every CompanionEntity in it, twice a second — which is a
	 * whole-level entity sweep to find at most one villager in a red coat.
	 *
	 * The two cases are genuinely different and neither needs that. A companion who
	 * is FOLLOWING is by definition next to her player, so sixty-four blocks round
	 * each player finds her. A companion who is WAITING is by definition not, and
	 * she is already in WAITING by UUID, so she can be fetched by name.
	 */
	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % LOOKS_EVERY != 0) {
			return;
		}
		for (ServerLevel here : server.getAllLevels()) {
			for (ServerPlayer with : here.players()) {
				for (CompanionEntity her : hers(here, with)) {
					if (!WAITING.containsKey(her.getUUID())) {
						notice(here, her, with);
					}
				}
			}
		}
		// The ones standing over a death site, and the ones who have fallen out of
		// the world. Both are somewhere no player-centred scan reaches.
		for (java.util.UUID who : List.copyOf(WAITING.keySet())) {
			CompanionEntity her = null;
			ServerLevel where = null;
			for (ServerLevel here : server.getAllLevels()) {
				if (here.getEntity(who) instanceof CompanionEntity found) {
					her = found;
					where = here;
					break;
				}
			}
			if (her == null || where == null) {
				WAITING.remove(who);      // unloaded, or gone. she resumes following.
				continue;
			}
			Player with = her.companion();
			if (with != null) {
				settled(where, her, with);
			}
		}
		fish(server);
	}

	/**
	 * Anybody who has gone off the bottom of the world.
	 *
	 * She cannot die, so the void does not kill her — it holds her at two hearts
	 * and drops her for ever, which is the one way left to lose her permanently.
	 * Follow's own teleport would eventually catch it, but only after Falter has
	 * finished eating, and "eventually" is not good enough for a hole with no
	 * bottom.
	 */
	private static void fish(MinecraftServer server) {
		for (ServerLevel here : server.getAllLevels()) {
			for (ServerPlayer with : here.players()) {
				for (CompanionEntity her : hers(here, with)) {
					if (her.getY() >= here.getMinY() - 8) {
						continue;
					}
					her.snapTo(with.getX(), with.getY(), with.getZ(),
						her.getYRot(), her.getXRot());
					her.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					her.getNavigation().stop();
					HerobrineMod.LOGGER.warn("{} went under the world and was fetched back",
						her.getName().getString());
				}
			}
		}
	}

	/**
	 * Whether she is still keeping vigil, and whether it is time to stop.
	 *
	 * WAITING is deliberately NOT persisted across a restart. If the server goes
	 * down while she is standing over a death site, the honest outcome on the way
	 * back up is that she simply resumes following — the vigil is a moment, not a
	 * state worth carrying in a save file, and the failure mode of getting it
	 * wrong is a companion frozen in a field for ever with no way to explain why.
	 */
	private static boolean settled(ServerLevel here, CompanionEntity her, Player with) {
		Long since = WAITING.get(her.getUUID());
		if (since == null) {
			return false;
		}
		if (her.distanceTo(with) < 8.0) {
			// You came back. That is the whole point of her having stayed.
			WAITING.remove(her.getUUID());
			return false;
		}
		if (here.getGameTime() - since < WAITS_FOR) {
			her.getNavigation().stop();
			return true;
		}
		WAITING.remove(her.getUUID());
		com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
			com.bloomlet.herobrine.entity.Sayings.WALKED_TO_YOU);
		HerobrineMod.LOGGER.info("{} gave up waiting and is walking to {}",
			her.getName().getString(), with.getName().getString());
		return false;
	}

	/**
	 * What she remarks on, in priority order, at most one thing per call.
	 *
	 * Ordered rather than rolled, and the order is worth reading: a Gaunt in the
	 * dark in his world would otherwise fire three lines at once, and Sayings has
	 * a quiet timer precisely because that is what a talking hat sounds like.
	 * The tall one wins, because it is the only one of the three that is standing
	 * in front of you right now.
	 */
	private static void notice(ServerLevel here, CompanionEntity her, Player with) {
		if (her.isFaltering()) {
			return;               // she has other things on her mind. Falter talks.
		}
		AABB round = her.getBoundingBox().inflate(NOTICES);

		List<com.bloomlet.herobrine.entity.GauntEntity> tall = here.getEntitiesOfClass(
			com.bloomlet.herobrine.entity.GauntEntity.class, round,
			g -> g.isAlive() && her.hasLineOfSight(g));
		if (!tall.isEmpty()) {
			com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.GAUNT_SEEN);
			return;
		}
		if (here.dimension().equals(com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.HIS_WORLD);
			return;
		}
		// Dark, and underground or at night. Not "dark" alone — a player who
		// steps into a doorway has not entered the dark.
		if (here.getMaxLocalRawBrightness(her.blockPosition()) <= 3
			&& (her.getBlockY() < 50 || !here.isBrightOutside())) {
			com.bloomlet.herobrine.entity.Sayings.toldOf(here, her, with,
				com.bloomlet.herobrine.entity.Sayings.DARK);
		}
	}

	/**
	 * Her, near a player. Sixty-four out and five hundred DOWN.
	 *
	 * The vertical reach is not symmetry, it is the void. A cube of 64 misses a
	 * companion who stepped off the edge — free fall clears sixty-four blocks in
	 * about two seconds and this only looks twice a second, so she can be past the
	 * box before it is next opened. Five hundred covers the whole build height,
	 * and there is nothing else down there for the query to find.
	 */
	private static List<CompanionEntity> hers(ServerLevel here, Player with) {
		return here.getEntitiesOfClass(CompanionEntity.class,
			with.getBoundingBox().inflate(64.0, 512.0, 64.0),
			her -> her.companion() == with);
	}
}
