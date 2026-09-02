package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

/**
 * CHESTS IN HIS CASTLE, WHICH HAD NONE.
 *
 * The castle arrives as a blueprint, and Keep.raise RETURNS THE MOMENT IT PLACES
 * — so every furnishing step the built-in castle gets is skipped for the one that
 * actually stands in the world. The file has three chests in seventy-three
 * thousand blocks and nothing ever filled them, which made the last building in
 * the mod, at the end of its longest walk, completely empty.
 *
 * <h2>Why the chests are placed here rather than drawn in the blueprint</h2>
 *
 * They could be added to the JSON. They should not be: the blueprint is a
 * transcription of somebody's build and editing it by hand means the file no
 * longer matches its source, so the next re-export silently loses the work. The
 * chests belong to the mod, not to the building.
 *
 * <h2>Where they go</h2>
 *
 * Read off the world after it is standing, by the same rule a player uses to
 * decide a room is a room: somewhere to stand, headroom above it, a ROOF over
 * that, and a WALL to set the chest against. The wall requirement does most of
 * the work — it keeps them out of the middle of halls and out of doorways, and it
 * is why they look put there rather than dropped.
 *
 * Then spread. Candidates are taken greedily with a minimum separation, which
 * scatters them across the seven floors instead of clustering them all in the
 * great hall, because the great hall has three hundred standable tiles and would
 * otherwise win every roll.
 *
 * <h2>And it is staged</h2>
 *
 * The box is 71x49x72 — a quarter of a million block reads. That is nothing
 * spread over the height of the building and a visible stutter in one tick, so it
 * goes one horizontal slice per tick behind the placement that is still running.
 */
public final class Hoard {

	private Hoard() {}

	/**
	 * How many, and how far apart.
	 *
	 * EIGHTEEN IS "GANSKE GODT ANTALL" AND NOT MORE. The castle is nine floors and
	 * a courtyard; eighteen is two a floor, which reads as a garrison that stored
	 * things everywhere. Forty would read as a loot pinata and would also mean
	 * nobody ever opens the last twenty.
	 *
	 * SEVEN APART is measured, not guessed: the smallest real room in the building
	 * is about eight tiles across, so seven guarantees at most one chest per small
	 * room while still allowing two or three along a long hall.
	 */
	private static final int CHESTS = 18;
	private static final int APART = 7;
	/** Trapped chests stay as the blueprint drew them; only plain ones are added. */
	private static final int HEADROOM = 2;

	/**
	 * Stock the castle, once it is standing.
	 *
	 * @param after ticks to wait — the placement is still running when this is
	 *              called, and reading the world before it finishes finds a hole
	 */
	public static void stock(ServerLevel his, BlockPos corner, int sx, int sy, int sz,
	                         int after) {
		var server = his.getServer();
		List<BlockPos> found = new ArrayList<>();
		java.util.Set<BlockPos> open = new java.util.HashSet<>();
		java.util.List<BlockPos> outside = new ArrayList<>();

		// One slice a tick, queued behind the build.
		for (int dy = 0; dy < sy; dy++) {
			final int y = corner.getY() + dy;
			com.bloomlet.herobrine.manifest.Cadence.in(server, after + dy,
				() -> sweep(his, corner, sx, sy, sz, y, found, open, outside));
		}
		com.bloomlet.herobrine.manifest.Cadence.in(server, after + sy + 1,
			() -> settle(his, corner, sx, sy, sz, found, open, outside));
	}

	/** Every spot on one level that a chest could stand against a wall indoors. */
	private static void sweep(ServerLevel his, BlockPos corner, int sx, int sy, int sz,
	                          int y, List<BlockPos> found, java.util.Set<BlockPos> open,
	                          List<BlockPos> outside) {
		int top = corner.getY() + sy - 1;
		for (int dx = 0; dx < sx; dx++) {
			for (int dz = 0; dz < sz; dz++) {
				BlockPos floor = new BlockPos(corner.getX() + dx, y, corner.getZ() + dz);
				if (!solid(his, floor)) {
					continue;
				}
				// Room for a man, which is room for a chest and the player opening
				// it. HEADROOM is 2, so this is the block the chest goes in and the
				// one above it.
				BlockPos at = floor.above();
				boolean clear = true;
				for (int h = 0; h < HEADROOM; h++) {
					if (!his.getBlockState(at.above(h)).isAir()) {
						clear = false;
						break;
					}
				}
				if (!clear) {
					continue;
				}
				// EVERY tile a man could stand on, whether or not a chest wants it.
				// This is the graph the reachability test walks; without it there is
				// no way to tell a cellar from a sealed void, and about one chest in
				// fourteen ends up mortared inside the foundation.
				open.add(at);
				// INDOORS, and this is the whole test. A roof somewhere above means
				// a room; no roof means the courtyard, the battlements or the field
				// outside, and a chest standing in the rain is not furnishing.
				boolean roofed = false;
				for (int up = at.getY() + HEADROOM; up <= top; up++) {
					if (solid(his, new BlockPos(at.getX(), up, at.getZ()))) {
						roofed = true;
						break;
					}
				}
				if (!roofed) {
					// Under open sky: the courtyard, the battlements, the field
					// outside the wall. No chest goes here — but this is where the
					// flood starts, because it is where a player walks in from.
					outside.add(at);
					continue;
				}
				// A WALL TO STAND IT AGAINST. Keeps them out of the middle of the
				// floor and out of doorways, and gives the chest a facing.
				for (Direction way : Direction.Plane.HORIZONTAL) {
					if (solid(his, at.relative(way))
						&& his.getBlockState(at.relative(way.getOpposite())).isAir()) {
						found.add(at);
						break;
					}
				}
			}
		}
	}

	/** Choose the spread, put them down, then fill everything empty in the box. */
	private static void settle(ServerLevel his, BlockPos corner, int sx, int sy, int sz,
	                           List<BlockPos> found, java.util.Set<BlockPos> open,
	                           List<BlockPos> outside) {
		RandomSource random = his.getRandom();
		// ---- CAN ANYBODY GET THERE?
		//
		// "Indoors" was the wrong test on its own. A blueprint this size has voids
		// in it — pockets in the eleven courses of foundation, a gap between a
		// tower roof and its cap — that pass every check a chest needs and cannot
		// be entered at all. Measured against this build: 100 of 1529 candidate
		// spots, so about 1.3 of the eighteen chests were mortared into the
		// masonry, holding an enchanted apple nobody would ever see.
		//
		// Height rules do not fix it. Requiring the ground course or above only
		// takes it from 6.5% to 4.8%, and throws away the cellar, which is
		// legitimately reachable and a good place for stores.
		//
		// So it is walked. A flood through the standable tiles from the ones under
		// open sky, stepping one block up or down the way a player does. Seven
		// thousand nodes and twelve edges each — trivial, and it is exact rather
		// than a heuristic.
		java.util.Set<BlockPos> reached = new java.util.HashSet<>(outside);
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(outside);
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Direction way : Direction.Plane.HORIZONTAL) {
				for (int step = -1; step <= 1; step++) {
					BlockPos next = at.relative(way).above(step);
					if (open.contains(next) && reached.add(next)) {
						queue.add(next);
					}
				}
			}
		}
		int walled = found.size();
		found.removeIf(at -> !reached.contains(at));
		// Shuffled before the greedy pass, so the spread is not biased towards the
		// low-x corner the sweep happens to start in.
		List<BlockPos> pool = new ArrayList<>(found);
		java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));

		List<BlockPos> taken = new ArrayList<>();
		for (BlockPos at : pool) {
			if (taken.size() >= CHESTS) {
				break;
			}
			boolean crowded = false;
			for (BlockPos was : taken) {
				if (was.distSqr(at) < (double) APART * APART) {
					crowded = true;
					break;
				}
			}
			if (!crowded) {
				taken.add(at);
			}
		}

		for (BlockPos at : taken) {
			Direction faces = Direction.NORTH;
			for (Direction way : Direction.Plane.HORIZONTAL) {
				if (solid(his, at.relative(way))
					&& his.getBlockState(at.relative(way.getOpposite())).isAir()) {
					faces = way.getOpposite();   // opening away from the wall
					break;
				}
			}
			his.setBlock(at, Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, faces), 3);
		}

		// AND THE THREE THE BLUEPRINT ALREADY HAD, in the same pass.
		//
		// A block-entity sweep of the box catches everything empty — the eighteen
		// just placed and the file's own three — so the chests that were drawn into
		// the build finally hold something too, and nothing needs to know which is
		// which.
		int filled = 0;
		for (int cx = corner.getX() >> 4; cx <= (corner.getX() + sx) >> 4; cx++) {
			for (int cz = corner.getZ() >> 4; cz <= (corner.getZ() + sz) >> 4; cz++) {
				var chunk = his.getChunk(cx, cz);
				for (var entry : new ArrayList<>(chunk.getBlockEntities().entrySet())) {
					BlockPos at = entry.getKey();
					if (at.getX() < corner.getX() || at.getX() >= corner.getX() + sx
						|| at.getZ() < corner.getZ() || at.getZ() >= corner.getZ() + sz
						|| at.getY() < corner.getY() || at.getY() >= corner.getY() + sy) {
						continue;
					}
					if (!(entry.getValue() instanceof BaseContainerBlockEntity hold)
						|| !hold.isEmpty()) {
						continue;
					}
					Loot.scatter(hold, random, Loot.Tier.KEEP);
					filled++;
				}
			}
		}
		HerobrineMod.LOGGER.info(
			"the castle is stocked: {} chests placed, {} containers filled — {} spots"
				+ " against a wall, {} of them reachable",
			taken.size(), filled, walled, found.size());
	}

	private static boolean solid(ServerLevel his, BlockPos at) {
		BlockState state = his.getBlockState(at);
		return !state.isAir() && state.isSolidRender();
	}
}
