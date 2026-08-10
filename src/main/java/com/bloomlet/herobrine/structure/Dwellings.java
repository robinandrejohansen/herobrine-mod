package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.Phase;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Where he lived, and when it appears.
 *
 * The house has ONE position per world, fixed by the world seed, decided
 * before anybody goes looking. That matters more than it sounds: a house that
 * appeared near whoever happened to wander furthest would be a house that
 * follows the player, and players work that out immediately. This one has
 * always been there. Two people on a server walk to the same coordinates and
 * find the same building, and a player who reads the seed can find it in a
 * copy of the world — which is exactly the kind of consistency that makes a
 * place feel like part of the map rather than part of the mod.
 *
 * It is only BUILT when somebody gets close, because blocks cannot be placed
 * in chunks that are not loaded and forcing them open across a thousand blocks
 * to furnish a room nobody is in would be indefensible. The position is the
 * real thing; the blocks are just what happens when you arrive.
 *
 * Deliberately not gated on wrath. Everything else in this mod is paced, and
 * this is the one thing that is not: it does not wait for the player to earn
 * it and it does not care what phase they are in. If they walk far enough on
 * their first day, it is there on their first day. It was there before them.
 */
public final class Dwellings {
	private Dwellings() {}

	/** Set once the blocks exist, so it is never built twice. */
	public static final AttachmentType<Boolean> RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_raised"), Codec.BOOL);

	/** Where it went, once it went somewhere. */
	public static final AttachmentType<Long> ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_origin"), Codec.LONG);

	public static final AttachmentType<Boolean> THRESHOLD_RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_raised"), Codec.BOOL);

	public static final AttachmentType<Long> THRESHOLD_ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_origin"), Codec.LONG);

	/**
	 * Eight bytes each, and not nine.
	 *
	 * These are spelled-out words in hex because a salt you can read is a salt
	 * you can tell apart at a glance — but a long is eight bytes, and the first
	 * attempt spelled longer words than that and would not compile.
	 */
	/**
	 * The town, and it had no way of existing until now.
	 *
	 * Township.raise was only ever called from /herobrine town here, so on an
	 * ordinary world the whole settlement — walls, hall, forge, church and the
	 * chamber under the square — simply never appeared. It was advertised and
	 * unreachable, which is the worst of both.
	 *
	 * Sited nearer than any of the houses, because it is the one thing here
	 * that is meant to be FOUND rather than sought: a walled town with people
	 * in it, at the edge of where somebody's first world reaches, and then
	 * everything wrong with it discovered afterwards.
	 */
	private static final AttachmentType<Boolean> TOWN_RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("town_raised"), Codec.BOOL);

	/** Far enough to be a journey, near enough to be reachable on foot. */

	/** Build when somebody is this close. Inside a default simulation radius. */
	/**
	 * How close somebody has to get before it builds.
	 *
	 * Raised from 112. A building is one point at one random bearing eleven
	 * hundred blocks out, and 112 asks a player to pass through a 224-block
	 * window on a circle 7,000 blocks around — which a server walking in
	 * straight lines will essentially never do, and did not.
	 *
	 * 192 is still inside a default simulation radius, so it cannot try to
	 * build in chunks the server is not ticking, and it nearly quadruples the
	 * chance of an ordinary journey catching one.
	 */
	/**
	 * How close somebody has to get before it builds.
	 *
	 * Chunks have to be ticking for this to be safe, so it stays inside a
	 * default simulation radius.
	 */
	private static final int RAISE_RANGE = 192;
	private static final int CHECK_INTERVAL = 40;

	/**
	 * How far from the players a new building is put.
	 *
	 * Far enough to be a walk with a reason, near enough that a server which
	 * has settled in one valley will actually meet it. The old scheme sited
	 * everything on a ring around WORLD SPAWN, eleven hundred to thirty-six
	 * hundred blocks out, which quietly assumed the players would explore
	 * outward for hours — and a group that builds a base together and stays
	 * near it never went anywhere near any of them.
	 */
	private static final int NEAR = 340;
	private static final int FAR = 780;
	/** Everybody this far from an unbuilt place, and it goes somewhere else. */
	private static final int ABANDONED = 1400;

	private static int tickCounter;

	/**
	 * The five houses, the town, and when each is allowed to exist.
	 *
	 * ONE PLACE PER PHASE, IN ORDER, AND NEVER TWO AT ONCE. Six phases and six
	 * buildings is not a coincidence any more — each phase brings exactly one
	 * new place, so every time the world gets worse there is also somewhere new
	 * out there, and the two arrive together. Two of them used to share RUMOUR,
	 * which spent the opening move twice.
	 *
	 * The order is the story. The homestead first, because it has to establish
	 * what a home of his looks like before anything can be measured against it.
	 * Then the town, arriving exactly when he starts being SEEN — the one place
	 * with living people in it, found at the moment the world stops being
	 * ordinary. Then the four buildings that are each less like somewhere a
	 * person lived than the last, and the threshold at the end, which is the
	 * only one with an answer in it.
	 *
	 * AND THE NEXT IS NOT SITED UNTIL THE LAST HAS BEEN FOUND. That is what
	 * makes it readable rather than scattered: a player cannot stumble into the
	 * gaol before the tower and wonder what they missed, because until the
	 * tower is standing the gaol does not exist. Skipping ahead with
	 * /herobrine wrath does not skip the sequence either — it only unlocks how
	 * far it is allowed to get.
	 */
	private enum Place {
		HOMESTEAD("homestead", Phase.RUMOUR),
		TOWN("town", Phase.WATCHER),
		TOWER("house_two", Phase.TRESPASSER),
		GAOL("house_three", Phase.MIMIC),
		CHURCH("house_four", Phase.HUNTER),
		THRESHOLD("threshold", Phase.SIEGE);

		final Phase from;
		/** Where it was decided to go, once anybody was around to decide near. */
		final AttachmentType<Long> site;
		/** Whether the blocks exist. */
		final AttachmentType<Boolean> up;
		/** Whether anybody has walked up on it yet. Spent once, for good. */
		final AttachmentType<Boolean> met;

		Place(String key, Phase from) {
			this.from = from;
			this.site = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_site"), Codec.LONG);
			this.up = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_up"), Codec.BOOL);
			this.met = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_met"), Codec.BOOL);
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Dwellings::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled) {
			return;
		}
		ServerLevel overworld = server.overworld();
		if (overworld.players().isEmpty()) {
			return;
		}
		Phase phase = Wrath.phase(server);

		for (Place place : Place.values()) {
			boolean wanted = place == Place.TOWN
				? com.bloomlet.herobrine.Config.get().town
				: com.bloomlet.herobrine.Config.get().houses;
			if (!wanted) {
				continue;
			}
			if (Boolean.TRUE.equals(overworld.getAttached(place.up))) {
				arriving(overworld, place);
				continue;   // standing; on to the next chapter
			}
			if (!phase.atLeast(place.from)) {
				return;     // and nothing after it either — this is a sequence
			}

			Long chosen = overworld.getAttached(place.site);
			if (chosen == null) {
				BlockPos picked = pick(overworld);
				if (picked != null) {
					overworld.setAttached(place.site, picked.asLong());
					HerobrineMod.LOGGER.info("{} will stand near [{}, {}] ({})",
						place.name().toLowerCase(java.util.Locale.ROOT),
						picked.getX(), picked.getZ(), phase.name());
				}
				return;     // one at a time, and the next waits for this one
			}

			BlockPos site = BlockPos.of(chosen);
			double nearest = Double.MAX_VALUE;
			for (ServerPlayer player : overworld.players()) {
				nearest = Math.min(nearest, Math.sqrt(
					site.distSqr(player.blockPosition())));
			}

			// IT FOLLOWS THEM IF THEY NEVER CAME.
			//
			// A place chosen near where the group was an hour ago is no use to
			// a group that has since moved five hundred blocks and built
			// somewhere else — it sits there, unfound, and the sequence stops
			// dead behind it because nothing after it is allowed to exist yet.
			//
			// So if everybody is a long way off, it is forgotten and chosen
			// again. Fourteen hundred is far enough that nobody walking toward
			// it can trip this by accident; at that distance they are not
			// coming, and the story is waiting on somebody who does not know
			// it is waiting.
			if (nearest > ABANDONED) {
				overworld.setAttached(place.site, null);
				HerobrineMod.LOGGER.info("{} was never found at [{}, {}] — moving it",
					place.name().toLowerCase(java.util.Locale.ROOT),
					site.getX(), site.getZ());
				return;
			}

			if (nearest <= RAISE_RANGE && build(overworld, place, site)) {
				overworld.setAttached(place.up, true);
			}
			return;         // whatever happened, the next one is not due yet
		}
	}

	/**
	 * The next one nobody has walked up on yet.
	 *
	 * Reuses the same `met` flag the arrival sighting spends, which is exactly
	 * the right definition: a building somebody has already stood outside is not
	 * somewhere to be sent, and one that is merely SITED and not yet built still
	 * is — it will exist by the time anybody gets near it, because building is
	 * what happens when a player comes within range.
	 *
	 * @return where to point somebody, or null if there is nothing left to find
	 */
	public static @org.jspecify.annotations.Nullable BlockPos unfound(ServerLevel level) {
		for (Place place : Place.values()) {
			if (Boolean.TRUE.equals(level.getAttached(place.met))) {
				continue;
			}
			Long chosen = level.getAttached(place.site);
			if (chosen != null) {
				return BlockPos.of(chosen);
			}
		}
		return null;
	}

	/**
	 * SOMEBODY IS WALKING UP ON IT FOR THE FIRST TIME, AND HE IS OUTSIDE.
	 *
	 * The payoff for the four-hundred-block walk. Finding one of these is the
	 * biggest thing that happens in a session, and until now the reward for it
	 * was a building — good, but silent. Standing at the door watching the group
	 * come over the rise turns finding a structure into being SHOWN one, which
	 * is the difference between world generation and somebody having been here.
	 *
	 * ONCE PER BUILDING, EVER, and persistent so it survives a restart. The
	 * second time is a spawner and everybody knows it.
	 *
	 * Sixty blocks rather than the raise range: they have to be close enough
	 * that the house is already in view, or he is a figure standing in a field
	 * for no reason. This wants to land in the same breath as "there it is".
	 */
	private static final int ARRIVING = 60;

	private static void arriving(ServerLevel level, Place place) {
		if (Boolean.TRUE.equals(level.getAttached(place.met))) {
			return;
		}
		Long chosen = level.getAttached(place.site);
		if (chosen == null) {
			return;
		}
		BlockPos site = BlockPos.of(chosen);
		for (ServerPlayer player : level.players()) {
			if (site.distSqr(player.blockPosition()) > (double)ARRIVING * ARRIVING) {
				continue;
			}
			// Marked spent on the approach rather than on a successful placement.
			// If the geometry refuses — they came in through the back, or it is
			// a hillside with no sightline — the moment is gone, and trying
			// again every two seconds until it works would put him outside the
			// house long after they had walked into it, which is worse than
			// nothing.
			level.setAttached(place.met, true);
			com.bloomlet.herobrine.entity.HauntingSpawner.atPlace(level, player, site);
			return;
		}
	}

	/**
	 * Somewhere out of sight of everybody, at a walkable distance.
	 *
	 * Measured from the middle of wherever the players actually are rather than
	 * from any one of them, so on a server it lands somewhere the group might
	 * plausibly go instead of behind whoever happened to be furthest out.
	 *
	 * Refuses anything closer than NEAR to ANY player. A house that appears
	 * three hundred blocks from the base of the one person who went mining is
	 * a house somebody watches arrive, and nothing here is ever watched
	 * arriving.
	 */
	private static @org.jspecify.annotations.Nullable BlockPos pick(ServerLevel level) {
		double cx = 0;
		double cz = 0;
		for (ServerPlayer player : level.players()) {
			cx += player.getX();
			cz += player.getZ();
		}
		cx /= level.players().size();
		cz /= level.players().size();

		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 48; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = (int)Math.round(cx + Math.cos(angle) * range);
			int z = (int)Math.round(cz + Math.sin(angle) * range);

			if (!buildable(level, x, z)) {
				continue;
			}
			BlockPos at = new BlockPos(x, Ground.topOf(level, x, z), z);
			boolean tooNear = false;
			for (ServerPlayer player : level.players()) {
				if (player.blockPosition().closerThan(at, NEAR)) {
					tooNear = true;
					break;
				}
			}
			if (!tooNear) {
				return at;
			}
		}
		return null;
	}

	/** Put the right building on the chosen ground. */
	private static boolean build(ServerLevel level, Place place, BlockPos site) {
		RandomSource random = level.getRandom();
		return switch (place) {
			case TOWN -> {
				com.bloomlet.herobrine.town.Township.raise(level, site, random);
				yield true;
			}
			case HOMESTEAD -> raise(level, site);
			case TOWER -> raiseMiddle(level, site, 0);
			case GAOL -> raiseMiddle(level, site, 1);
			case CHURCH -> raiseMiddle(level, site, 2);
			case THRESHOLD -> raiseThreshold(level, site);
		};
	}

	/**
	 * Forget where everything was going to be, so it is chosen again.
	 *
	 * Needed because a jar swap changes nothing about a world. All of this
	 * lives in persistent attachments — that is what makes a site survive a
	 * restart, which is the whole point of it — so a server that ran the old
	 * spawn-relative scheme still has those far-off positions recorded after
	 * updating, and would go on waiting for somebody to walk eleven hundred
	 * blocks to a coordinate the new code would never have chosen.
	 *
	 * This clears the bookkeeping ONLY. Anything already standing in the world
	 * stays standing; the mod simply stops believing it owns those places and
	 * picks new ones near the players at the next tick. That is the honest
	 * behaviour — deleting somebody's discovered buildings to tidy up a
	 * migration would be a far worse trade than leaving a spare farmhouse
	 * somewhere.
	 *
	 * @return how many places were forgotten
	 */
	public static int forget(ServerLevel level) {
		ServerLevel overworld = level.getServer().overworld();
		int cleared = 0;
		for (Place place : Place.values()) {
			if (overworld.getAttached(place.site) != null
				|| Boolean.TRUE.equals(overworld.getAttached(place.up))) {
				cleared++;
			}
			overworld.setAttached(place.site, null);
			overworld.setAttached(place.up, null);
		}
		// The two left over from the old scheme, or /herobrine house goes on
		// reporting a farmhouse that is no longer anybody's business.
		overworld.setAttached(ORIGIN, null);
		overworld.setAttached(THRESHOLD_ORIGIN, null);
		HerobrineMod.LOGGER.info("forgot {} sites; they will be chosen again", cleared);
		return cleared;
	}

	/** Every building and where it ended up, for /herobrine locate. */
	public static java.util.List<String> report(ServerLevel level) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		Phase phase = Wrath.phase(level.getServer());
		boolean reached = true;
		for (Place place : Place.values()) {
			String name = place.name().toLowerCase(java.util.Locale.ROOT);
			boolean built = Boolean.TRUE.equals(level.getAttached(place.up));
			Long at = level.getAttached(place.site);

			if (built) {
				BlockPos pos = BlockPos.of(at == null ? 0L : at);
				lines.add(String.format("%-11s found        x %d z %d",
					name, pos.getX(), pos.getZ()));
				continue;
			}
			if (!reached) {
				// Everything after the current chapter, and saying so is more
				// use than six identical "not sited" lines.
				lines.add(String.format("%-11s later        after %s is found",
					name, Place.values()[place.ordinal() - 1]
						.name().toLowerCase(java.util.Locale.ROOT)));
				continue;
			}
			reached = false;
			if (at == null) {
				lines.add(String.format("%-11s waiting for  %s", name, place.from.name()));
			} else {
				BlockPos pos = BlockPos.of(at);
				lines.add(String.format("%-11s OUT THERE    x %d z %d",
					name, pos.getX(), pos.getZ()));
			}
		}
		lines.add("phase " + phase.name());
		return lines;
	}

	public static boolean raiseMiddle(ServerLevel level, BlockPos near, int which) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!buildable(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			RandomSource random = level.getRandom();
			switch (which) {
				case 0 -> SecondHouse.build(level, origin, random);
				case 1 -> TheDig.build(level, origin, random);
				default -> Shrine.build(level, origin, random);
			}
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for house {} near [{}, {}]",
			which + 2, near.getX(), near.getZ());
		return false;
	}


	public static @org.jspecify.annotations.Nullable BlockPos thresholdOrigin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(THRESHOLD_ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * Raise the threshold near its site.
	 *
	 * Far less fussy about ground than the homestead, and deliberately so:
	 * almost nothing of it is above the surface, so a slope that would tilt a
	 * farmhouse does not matter to a stair mouth. All it needs is dry land.
	 */
	public static boolean raiseThreshold(ServerLevel level, BlockPos near) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			int ground = Ground.topOf(level, x, z);
			if (ground <= level.getSeaLevel()
				|| !level.getFluidState(new BlockPos(x, ground, z)).isEmpty()) {
				continue;
			}
			BlockPos origin = new BlockPos(x, ground, z);
			Threshold.raise(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(THRESHOLD_ORIGIN, origin.asLong());
			return true;
		}
		HerobrineMod.LOGGER.warn("no dry ground for the threshold near [{}, {}]",
			near.getX(), near.getZ());
		return false;
	}


	/** Where it actually stands, once raised. */
	public static @org.jspecify.annotations.Nullable BlockPos origin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	public static boolean raised(ServerLevel level) {
		return Boolean.TRUE.equals(level.getServer().overworld().getAttached(RAISED));
	}

	/**
	 * Put it down, near the site, wherever the ground will take it.
	 *
	 * The seed picks the neighbourhood and the terrain picks the spot. Dropping
	 * it on the exact seeded block would put it in a lake or halfway up a cliff
	 * often enough to matter, and a house standing in water is not eerie, it is
	 * broken.
	 */
	public static boolean raise(ServerLevel level, BlockPos near) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!buildable(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Homestead.floorHeightAt(level, x, z), z);
			Homestead.build(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(ORIGIN, origin.asLong());
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for the homestead near [{}, {}]",
			near.getX(), near.getZ());
		return false;
	}

	/**
	 * Is there dry, loaded, roughly level ground here?
	 *
	 * Samples the corners and the middle rather than every column — the ground
	 * only has to be good enough that levelling it does not leave a four-block
	 * step of dirt down one side.
	 */
	private static boolean buildable(ServerLevel level, int x, int z) {
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		// Only the building has to be level. The yard follows the ground now,
		// so a site is judged on the ground under the HOUSE rather than on the
		// whole map — which was rejecting perfectly good spots because a
		// grave marker forty blocks away would have been on a hill.
		for (int dz = 2; dz <= 14; dz += 4) {
			for (int dx = 2; dx <= 18; dx += 4) {
				BlockPos column = new BlockPos(x + dx, 0, z + dz);
				if (!level.isLoaded(column.atY(level.getSeaLevel()))) {
					return false;
				}
				// Real ground, not the canopy. Judging a forest site by the
				// heightmap made it look wildly uneven AND put the floor level
				// somewhere above the trees.
				int height = Ground.floorOver(level, x + dx, z + dz);
				if (height <= level.getSeaLevel()) {
					return false;   // in the sea, or in a lake
				}
				if (!level.getFluidState(new BlockPos(x + dx, height - 1, z + dz)).isEmpty()) {
					return false;
				}
				low = Math.min(low, height);
				high = Math.max(high, height);
			}
		}
		// Two, not three. The footing is only three deep now, so a site that
		// varies more than this cannot be built on without a visible plinth —
		// better to walk on and find flatter ground.
		return high - low <= 2;
	}
}
