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

	private static int tickCounter;

	/**
	 * The five houses, the town, and when each is allowed to exist.
	 *
	 * THE PHASE GATE IS THE POINT OF THIS TABLE. Sited all at once they are six
	 * things to find; sited one per phase they are a drip — every time the world
	 * gets worse, there is also somewhere new out there, and the two facts
	 * arrive together. It also enforces the reading order for free: nobody meets
	 * the shrine before the homestead, because the shrine does not exist yet.
	 */
	private enum Place {
		TOWN("town", Phase.RUMOUR),
		HOMESTEAD("homestead", Phase.RUMOUR),
		TOWER("house_two", Phase.WATCHER),
		GAOL("house_three", Phase.TRESPASSER),
		CHURCH("house_four", Phase.MIMIC),
		THRESHOLD("threshold", Phase.HUNTER);

		final Phase from;
		/** Where it was decided to go, once anybody was around to decide near. */
		final AttachmentType<Long> site;
		/** Whether the blocks exist. */
		final AttachmentType<Boolean> up;

		Place(String key, Phase from) {
			this.from = from;
			this.site = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_site"), Codec.LONG);
			this.up = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_up"), Codec.BOOL);
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
			if (!wanted || Boolean.TRUE.equals(overworld.getAttached(place.up))) {
				continue;
			}
			if (!phase.atLeast(place.from)) {
				continue;   // not part of the story yet
			}

			Long chosen = overworld.getAttached(place.site);
			if (chosen == null) {
				BlockPos picked = pick(overworld);
				if (picked == null) {
					continue;   // nowhere to put it yet; try again in two seconds
				}
				overworld.setAttached(place.site, picked.asLong());
				HerobrineMod.LOGGER.info("{} will stand near [{}, {}] ({})",
					place.name().toLowerCase(java.util.Locale.ROOT),
					picked.getX(), picked.getZ(), phase.name());
				continue;
			}

			BlockPos site = BlockPos.of(chosen);
			for (ServerPlayer player : overworld.players()) {
				if (!player.blockPosition().closerThan(site, RAISE_RANGE)) {
					continue;
				}
				if (build(overworld, place, site)) {
					overworld.setAttached(place.up, true);
				}
				break;
			}
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

	/** Every building and where it ended up, for /herobrine locate. */
	public static java.util.List<String> report(ServerLevel level) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		Phase phase = Wrath.phase(level.getServer());
		for (Place place : Place.values()) {
			Long at = level.getAttached(place.site);
			String name = place.name().toLowerCase(java.util.Locale.ROOT);
			if (at == null) {
				lines.add(String.format("%-11s not sited yet — needs %s",
					name, place.from.name()));
				continue;
			}
			BlockPos pos = BlockPos.of(at);
			lines.add(String.format("%-11s x %d z %d   %s",
				name, pos.getX(), pos.getZ(),
				Boolean.TRUE.equals(level.getAttached(place.up)) ? "built" : "waiting"));
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
