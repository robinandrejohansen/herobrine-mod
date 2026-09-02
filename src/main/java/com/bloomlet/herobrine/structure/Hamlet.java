package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * SIX BUILDINGS ROUND A SQUARE, WHICH IS WHAT THE CITY SHOULD HAVE BEEN.
 *
 * The last house was Oakhold: one blueprint, four hundred thousand blocks, dropped
 * whole. The complaint about it was the correct one — it was too big. Not too
 * detailed or too slow, too BIG: a castle city at the end of a story about six
 * houses is a change of subject, and a player who has walked past a farmhouse, a
 * tower and a church arrives at a metropolis.
 *
 * So this is a village. Sixty-three by fifty-six, which is about a vanilla plains
 * village and a half, with a square in the middle you can see across and the hall
 * at the head of it with its flag up. Small enough to read as the last place
 * people lived rather than as a set piece.
 *
 * WHY SIX FILES AND NOT ONE. The source is not a build, it is an asset grid —
 * thirty-four separate buildings, thirteen colour variants, thirty blocks apart,
 * each on its own showcase plinth. Cutting a slab out of it would take the
 * plinths and the spacing with it and produce a museum. Taken one at a time they
 * can be turned, spaced and grounded individually, and the arrangement is the
 * mod's rather than the exhibition's.
 *
 * EVERY FRONT FACES THE SQUARE, which is the only reason Blueprint learned to
 * rotate. All six of these front their own -X face; unrotated they would stand in
 * a row all looking the same way, and three of them would show the square their
 * back wall. See Blueprint.spinX — the coordinates are ours and the block states
 * are BlockState.rotate's, because getting stair corners and door hinges right by
 * hand is a week of not noticing they are wrong.
 *
 * NOTHING IS BUILT BY HAND HERE. The buildings are somebody else's and they are
 * placed as measured. What this file owns is the arrangement, the lanes between
 * them, and what ends up in their chests.
 */
public final class Hamlet {
	private Hamlet() {}

	/**
	 * @param file   the blueprint's name
	 * @param front  which way the door faces in the FILE, before any turn
	 * @param faces  which way it should end up pointing once placed
	 * @param dx     where the middle of it sits relative to the site
	 * @param holds  what its chests and barrels are worth
	 */
	private record Part(String file, Direction front, Direction faces, int dx, int dz,
	                    Loot.Tier holds) {}

	/**
	 * THE PLAN, AND THE NUMBERS IN IT ARE CHECKED RATHER THAN CHOSEN.
	 *
	 * Every pair of these was tested for clearance before any of it was written:
	 * six buildings, fifteen pairs, and the first arrangement had the smithy two
	 * blocks off the second cottage — a gap you can walk down and cannot see is
	 * one, which is worse than a collision because a collision is obvious. Three
	 * blocks minimum, everywhere. The town's plots learned the same lesson the
	 * expensive way: see the note on whole-footprint testing in TheShambles.
	 *
	 *   hall       south   0,-18    the flag, at the head of the square
	 *   chapel     east  -22,  2    west side
	 *   smithy     west   24,  1    east side, opposite the chapel
	 *   store      north  -8, 22    south side
	 *   cottage_b  north  10, 21    south side
	 *   cottage_a  east  -22,-16    behind the chapel, off the square
	 *
	 * The open middle comes out about twenty-three by eighteen. Big enough that
	 * the hall reads as being at the END of something, small enough to cross.
	 */
	private static final Part[] PLAN = {
		new Part("village_hall", Direction.WEST, Direction.SOUTH,
			0, -18, Loot.Tier.HIS_CITY),
		new Part("village_chapel", Direction.WEST, Direction.EAST,
			-22, 2, Loot.Tier.TOWN_HOME),
		new Part("village_smithy", Direction.WEST, Direction.WEST,
			24, 1, Loot.Tier.TOWN_FORGE),
		new Part("village_store", Direction.WEST, Direction.NORTH,
			-8, 22, Loot.Tier.TOWN_TRADE),
		new Part("village_cottage_b", Direction.WEST, Direction.NORTH,
			10, 21, Loot.Tier.TOWN_TOOLS),
		// NORTH, AND IT IS THE ONE THAT IS NOT WEST.
		//
		// Five of the six put their entrance at low x with the door facing east,
		// which is a west front. This one's ground-floor door is at (3,4) facing
		// SOUTH — you walk south through it, so you come in off the north face.
		// Assuming all six alike would have turned this one's back wall to the
		// square, and it would have looked like a building placed wrong rather
		// than like data read wrong.
		new Part("village_cottage_a", Direction.NORTH, Direction.SOUTH,
			-22, -16, Loot.Tier.LARDER),
	};

	/** The one that has to be there. Without it there is no village and no portal. */
	private static final String HALL = "village_hall";

	/**
	 * How far to turn a building to get its front pointing where it should.
	 *
	 * Counted in quarter turns from the face the FILE fronts rather than assumed
	 * from -X, because one of the six does not front -X — see cottage_a in PLAN.
	 * Walked with Direction.getClockWise instead of done as arithmetic on
	 * ordinals: Direction's order is DOWN, UP, NORTH, SOUTH, WEST, EAST, and
	 * anything clever done with that is a bug waiting for somebody to reorder an
	 * enum.
	 */
	private static Rotation turnFor(Direction front, Direction faces) {
		Direction at = front;
		for (int quarters = 0; quarters < 4; quarters++) {
			if (at == faces) {
				return switch (quarters) {
					case 1 -> Rotation.CLOCKWISE_90;
					case 2 -> Rotation.CLOCKWISE_180;
					case 3 -> Rotation.COUNTERCLOCKWISE_90;
					default -> Rotation.NONE;
				};
			}
			at = at.getClockWise();
		}
		return Rotation.NONE;
	}

	private static Rotation turnFor(Part part) {
		return turnFor(part.front(), part.faces());
	}

	/** Are all six files here? Half a village is worse than the fallback. */
	public static boolean have() {
		for (Part part : PLAN) {
			if (!Blueprint.have(part.file())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Put the village up, and say where the stair down should start.
	 *
	 * @return the block inside the hall the descent cuts from, or null if it could
	 *         not be built — in which case the caller must fall back, because
	 *         raise() runs once per world and there is no second attempt.
	 */
	public static @org.jspecify.annotations.Nullable BlockPos raise(
			ServerLevel level, BlockPos site, RandomSource random) {
		if (!have()) {
			return null;
		}
		BlockPos head = null;
		int last = 0;
		int total = 0;
		for (Part part : PLAN) {
			int x = site.getX() + part.dx();
			int z = site.getZ() + part.dz();
			// ITS OWN GROUND, NOT THE SITE'S. Six buildings sharing one Y is a
			// village on a plinth the moment the terrain is not flat — three of
			// them buried to the windows and three standing on stilts. stand()
			// beards each footprint down to the real surface per column, so every
			// one of them only needs to be told where its own middle is.
			//
			// topOf and not topOf + 1: the blueprint's course zero is the dirt and
			// grass the building stands on, so it wants to land ON the terrain
			// surface and replace it. One higher and every building has a step
			// round it.
			BlockPos centre = new BlockPos(x, Ground.topOf(level, x, z), z);
			Rotation turn = turnFor(part);
			Blueprint.Placed done = Blueprint.stand(level, centre, part.file(), turn);
			if (done == null) {
				HerobrineMod.LOGGER.warn("the village is missing {}", part.file());
				continue;
			}
			total += done.blocks();
			last = Math.max(last, done.ticks());

			BlockPos corner = cornerOf(level, centre, part, done);
			// AFTER ITS OWN PLACING, not after all of them. Each building finishes
			// at its own tick and the loot is put in as soon as that one is done —
			// a chest filled before place() has written it is a chest filled into
			// thin air, and a shared delay long enough for the slowest is dead time
			// for the other five.
			final Loot.Tier holds = part.holds();
			final BlockPos box = corner;
			final int sx = done.sizeX();
			final int sy = done.sizeY();
			final int sz = done.sizeZ();
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(),
				done.ticks() + 1, () -> stock(level, box, sx, sy, sz, holds, random));

			if (part.file().equals(HALL)) {
				head = descentIn(corner, part, done);
			}
		}
		// The lanes go on last of all. They are drawn on the ground between the
		// buildings and every one of those buildings clears its own footprint on
		// its own clock — a path laid first is a path erased.
		final int after = last + 2;
		com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), after,
			() -> lanes(level, site, random));
		HerobrineMod.LOGGER.info(
			"the last house is a village of {} buildings, {} blocks, done in {} ticks",
			PLAN.length, total, after);
		return head;
	}

	/** Where stand() actually put the corner, worked back the same way it did. */
	private static BlockPos cornerOf(ServerLevel level, BlockPos centre, Part part,
	                                 Blueprint.Placed done) {
		return new BlockPos(centre.getX() - done.sizeX() / 2, centre.getY(),
			centre.getZ() - done.sizeZ() / 2);
	}

	/**
	 * The hall's own descent point, turned with the hall.
	 *
	 * The file names it in its unrotated coordinates — an interior floor tile with
	 * nothing standing on it, asserted at extraction after the first guess landed
	 * under a lectern. The hall is placed a quarter turn anticlockwise, so the
	 * point has to travel with it or the shaft comes up through a wall.
	 */
	private static @org.jspecify.annotations.Nullable BlockPos descentIn(
			BlockPos corner, Part part, Blueprint.Placed done) {
		BlockPos spot = Blueprint.descent(part.file());
		BlockPos file = Blueprint.measure(part.file());
		if (spot == null || file == null) {
			HerobrineMod.LOGGER.warn("{} names no descent — the village has no door",
				part.file());
			return null;
		}
		BlockPos turned = Blueprint.turnedPoint(spot, file.getX(), file.getZ(),
			turnFor(part));
		return corner.offset(turned.getX(), turned.getY(), turned.getZ());
	}

	/**
	 * WHAT IS IN THE CHESTS, AND IT IS FOUND BY ASKING THE CHUNK.
	 *
	 * A blueprint records block states and nothing else, so every chest, barrel
	 * and shulker in these six buildings is placed empty. There are a lot of them:
	 * the smithy alone has seventeen chests and seventeen barrels.
	 *
	 * Read off the chunk's own block-entity map rather than by walking the box. The
	 * hall is twenty-one by twenty-seven by seventeen, which is nine and a half
	 * thousand getBlockEntity calls to find about a dozen containers, and six
	 * buildings of that is forty thousand lookups for a job the chunk already has
	 * the answer to.
	 *
	 * ONLY THE EMPTY ONES. This runs a tick after the building finishes, and it
	 * must be safe to run twice — a barrel that already has bread in it has been
	 * done, and refilling it would stack a second draw on top of the first.
	 *
	 * Barrels get store() and chests get scatter(), which is Loot's own split: a
	 * barrel is stock and sits in a block, a chest is what somebody left and is
	 * spread about the slots.
	 */
	private static void stock(ServerLevel level, BlockPos corner, int sx, int sy,
	                          int sz, Loot.Tier tier, RandomSource random) {
		int filled = 0;
		for (int cx = corner.getX() >> 4; cx <= (corner.getX() + sx) >> 4; cx++) {
			for (int cz = corner.getZ() >> 4; cz <= (corner.getZ() + sz) >> 4; cz++) {
				var chunk = level.getChunk(cx, cz);
				for (var entry : new java.util.ArrayList<>(
						chunk.getBlockEntities().entrySet())) {
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
					if (level.getBlockState(at).is(Blocks.BARREL)) {
						Loot.store(hold, random, tier);
					} else {
						Loot.scatter(hold, random, tier);
					}
					filled++;
				}
			}
		}
		HerobrineMod.LOGGER.info("{} containers stocked with {} at [{}, {}, {}]",
			filled, tier, corner.getX(), corner.getY(), corner.getZ());
	}

	// ---- THE GROUND BETWEEN THEM ---------------------------------------------

	/**
	 * How far the square reaches, and how wide a spur is.
	 *
	 * THIRTEEN REACHES THE HALL ON PURPOSE. The hall's front is ten blocks from the
	 * site and everything else is fifteen or more, so this disc is the only lane
	 * that touches a building — and it touches the one it should: the three courses
	 * of front garden between the square and the main door. That is where a path
	 * belongs. Do not "fix" it by shrinking the disc; wear() already refuses every
	 * column that is not bare earth, so what it can reach is grass and what it
	 * cannot is the building.
	 */
	private static final int SQUARE = 13;
	private static final int LANE = 2;

	/**
	 * The square, and a spur to every door.
	 *
	 * Six buildings on bare grass are six buildings. What makes a place is the
	 * ground between them being WORN — and it is the cheapest thing in this whole
	 * file: a disc of dirt path in the middle and a two-wide run out to each front
	 * door, laid on whatever the terrain turns out to be.
	 *
	 * Per column, never at a fixed Y. The village follows the ground and so must
	 * the path; a lane drawn at the site's height is a lane buried in the first
	 * hillside and bridging the first dip.
	 */
	private static void lanes(ServerLevel level, BlockPos site, RandomSource random) {
		int laid = 0;
		for (int dx = -SQUARE; dx <= SQUARE; dx++) {
			for (int dz = -SQUARE; dz <= SQUARE; dz++) {
				if (dx * dx + dz * dz > SQUARE * SQUARE) {
					continue;
				}
				laid += wear(level, site.getX() + dx, site.getZ() + dz, random) ? 1 : 0;
			}
		}
		for (Part part : PLAN) {
			laid += spur(level, site, part, random);
		}
		HerobrineMod.LOGGER.info("{} columns of lane worn between the buildings", laid);
	}

	/**
	 * A run from the square out to one building's front, STOPPING AT IT.
	 *
	 * dx,dz is the middle of the building, not its door, so running the full
	 * distance walks the path into the middle of the floor. wear() would refuse
	 * most of it — it only touches earth, and their floors are brick — but their
	 * gardens are earth, and it would have gone through somebody's flowerbeds and
	 * then stopped, invisibly, wherever the stone happened to start.
	 *
	 * So it stops short by half the building's own turned depth plus one. Which
	 * means asking for the TURNED size and not the file's: at a quarter turn the
	 * two differ, and using the file's would stop four blocks early on some
	 * buildings and four blocks late on others.
	 */
	private static int spur(ServerLevel level, BlockPos site, Part part,
	                        RandomSource random) {
		int laid = 0;
		double len = Math.sqrt(part.dx() * part.dx() + part.dz() * part.dz());
		if (len < 1.0) {
			return 0;
		}
		BlockPos box = Blueprint.turned(part.file(), turnFor(part));
		if (box != null) {
			boolean acrossX = Math.abs(part.dx()) > Math.abs(part.dz());
			len -= (acrossX ? box.getX() : box.getZ()) / 2.0 + 1.0;
		}
		if (len < 1.0) {
			return 0;
		}
		double reach = Math.sqrt(part.dx() * part.dx() + part.dz() * part.dz());
		for (double along = 0.0; along <= len; along += 0.5) {
			// Along the ray to the building's middle, scaled by the FULL distance
			// so the direction is right — `len` has already been shortened and
			// dividing by it would stretch the run back out to the wall again.
			int x = site.getX() + (int) Math.round(part.dx() * along / reach);
			int z = site.getZ() + (int) Math.round(part.dz() * along / reach);
			for (int wide = -LANE / 2; wide <= LANE / 2; wide++) {
				// Across the run rather than along it, so the lane has width where
				// it turns instead of pinching to one block on the diagonals.
				int ox = Math.abs(part.dx()) > Math.abs(part.dz()) ? 0 : wide;
				int oz = Math.abs(part.dx()) > Math.abs(part.dz()) ? wide : 0;
				laid += wear(level, x + ox, z + oz, random) ? 1 : 0;
			}
		}
		return laid;
	}

	/**
	 * One column of path, if there is anything there worth wearing down.
	 *
	 * Only grass, dirt and the things that grow on them. A path that overwrites
	 * whatever it finds walks straight through the buildings it is meant to join —
	 * their floors are stone brick and their gardens are somebody's design, and
	 * neither is a road. That guard is the whole of the reason this returns a
	 * boolean rather than void: the count in the log is how it is checked.
	 */
	private static boolean wear(ServerLevel level, int x, int z, RandomSource random) {
		int y = Ground.topOf(level, x, z);
		BlockPos at = new BlockPos(x, y, z);
		BlockState was = level.getBlockState(at);
		if (!was.is(Blocks.GRASS_BLOCK) && !was.is(Blocks.DIRT)
			&& !was.is(Blocks.COARSE_DIRT) && !was.is(Blocks.PODZOL)
			&& !was.is(Blocks.SAND) && !was.is(Blocks.SNOW_BLOCK)) {
			return false;
		}
		// Anything standing in the road goes. Grass and flowers, not the buildings —
		// the test above has already refused to touch a column that is one.
		BlockPos over = at.above();
		if (!level.getBlockState(over).isAir()
			&& !level.getBlockState(over).isSolid()) {
			level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);
		}
		level.setBlock(at, random.nextInt(6) == 0
			? Blocks.GRAVEL.defaultBlockState()
			: Blocks.DIRT_PATH.defaultBlockState(), 2);
		return true;
	}
}
