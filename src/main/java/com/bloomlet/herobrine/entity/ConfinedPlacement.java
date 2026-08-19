package com.bloomlet.herobrine.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Finding somewhere to stand when the player is underground.
 *
 * The surface strategy picks a point on a ring 26-44 blocks out and drops it
 * to the terrain height. Underground that is catastrophically wrong: the
 * terrain height is the mountainside above you, so he gets placed outdoors,
 * through thirty blocks of rock, where you will never see him. In a 2x1
 * mining tunnel there is no valid point on that ring at all, in any
 * direction.
 *
 * So underground we do the opposite: instead of choosing a point in space and
 * hoping it is reachable, we flood-fill outward through the air the player is
 * actually standing in. In a cave that follows the cave. In a straight mined
 * shaft it runs down the shaft. He ends up in the same pocket of space as
 * you, which is the only place he can be seen from.
 *
 * Two rules make it frightening rather than merely correct:
 *
 *   LINE OF SIGHT is required. Round a corner he may as well not exist, and
 *   the manifestation is wasted. Down a corridor, at the edge of your torch
 *   light, is the entire point.
 *
 *   MUCH CLOSER than the surface. You cannot see 30 blocks underground.
 *   10-22 keeps him beyond the 8-block dissolve radius but inside the space
 *   you can actually perceive.
 */
public final class ConfinedPlacement {
	private ConfinedPlacement() {}

	/**
	 * Just outside the 8-block radius at which he dissolves. Underground you
	 * cannot see far, so there is little room between "he vanishes instantly"
	 * and "too far to make out".
	 */
	private static final double MIN_DISTANCE = 9.0;
	private static final double MAX_DISTANCE = 24.0;
	/** Cap on flood-fill work. A cave system is effectively unbounded. */
	private static final int MAX_NODES = 2000;
	private static final int MAX_RADIUS = 24;

	/** True when the ring-and-drop surface strategy would be nonsense here. */
	public static boolean isConfined(ServerLevel level, ServerPlayer player) {
		return !level.canSeeSky(player.blockPosition());
	}

	/**
	 * IS THERE ACTUALLY ROCK OVER THEM, OR IS IT A ROOF?
	 *
	 * {@link #isConfined} is one line — no sky overhead — and that line was doing
	 * two completely different jobs. Underground it is exactly right: the surface
	 * placement's whole vocabulary is distance against a horizon, and a horizon is
	 * not a thing a cave has. INSIDE A HOUSE it is catastrophically wrong, because
	 * a house is on the surface with the horizon still out there — it just has a
	 * plank between the player and the sky.
	 *
	 * The consequence was that standing in your own base disabled the two events
	 * that most want to find you in it. He would not stand in the field outside
	 * and look in at the window, and a hunt could not start at all. A player at
	 * home was safe from the only two things in the mod that are supposed to come
	 * to your home.
	 *
	 * So this measures the THICKNESS of what is above them rather than merely its
	 * existence. One column, from over their head to the surface, counting solid
	 * material. A roof is one or two blocks; a floor above a floor above a roof is
	 * four or five. Thirty blocks down a mineshaft is twenty-something, and there
	 * is no building anybody makes that reads like that.
	 *
	 * Six is the line, which comfortably clears a three-storey base and refuses
	 * anything genuinely buried.
	 */
	private static final int ROOF_AT_MOST = 6;

	public static boolean buried(ServerLevel level, ServerPlayer player) {
		if (level.canSeeSky(player.blockPosition())) {
			return false;
		}
		int x = player.getBlockX();
		int z = player.getBlockZ();
		int surface = level.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
		int solid = 0;
		// From over their head, so the block they are standing under does not
		// count twice, up to whatever the heightmap calls the top.
		for (int y = player.getBlockY() + 2; y <= surface; y++) {
			if (level.getBlockState(new BlockPos(x, y, z)).isSolidRender()) {
				solid++;
				if (solid > ROOF_AT_MOST) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return somewhere he can stand, in the player's own space, behind them
	 *         and in view — or null, which simply means a quiet night.
	 */
	public static @Nullable BlockPos find(ServerLevel level, ServerPlayer player) {
		return nearby(level, player, MIN_DISTANCE, MAX_DISTANCE, true, true);
	}

	/**
	 * SOMEWHERE HE CAN STAND THAT IS REACHABLE THROUGH AIR FROM WHERE THEY ARE.
	 *
	 * Generalised out of {@link #find} for the hunt, which needs the same
	 * flood-fill and none of the same manners. The stare wants distance, wants
	 * to be behind them and wants to be seen. Arriving mid-hunt wants the
	 * opposite of the first two: close enough to swing at, and it does not care
	 * in the slightest whether they are looking.
	 *
	 * The reason this exists at all rather than the hunt reusing reappearAt is
	 * that reappearAt places by HEIGHTMAP — it asks the world for the top of the
	 * column and puts him there. Forty blocks down a cave that is the surface,
	 * which is why he could never follow anybody underground: he was being sent
	 * to a field directly above their head, every time, and then losing them.
	 *
	 * A flood-fill through passable space cannot make that mistake, because it
	 * only ever reaches somewhere connected to the air the player is breathing.
	 * The same property handles the other half of it for free: on top of a
	 * pillar the only standable node it can reach is the pillar, so he arrives
	 * ON the pillar rather than in the field below it.
	 *
	 * @param requireBehind out of their view cone
	 * @param requireSight  and with a clear line to where they stand
	 */
	public static @Nullable BlockPos nearby(ServerLevel level, ServerPlayer player,
			double min, double max, boolean requireBehind, boolean requireSight) {
		BlockPos start = player.blockPosition();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F).normalize();

		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		Map<BlockPos, Integer> depth = new HashMap<>();
		List<BlockPos> candidates = new ArrayList<>();

		queue.add(start);
		seen.add(start);
		depth.put(start, 0);

		while (!queue.isEmpty() && seen.size() < MAX_NODES) {
			BlockPos pos = queue.poll();
			int d = depth.get(pos);
			if (d > MAX_RADIUS) {
				continue;
			}

			// Filter on REAL distance, not flood-fill depth. Ten steps round a
			// twisting cave can be five blocks away in a straight line — well
			// inside the radius where he dissolves on sight, so he would have
			// arrived and instantly left.
			double actual = Math.sqrt(pos.distSqr(start));
			if (actual >= min && actual <= max
				&& canStand(level, pos)
				&& (!requireBehind || isBehind(player, look, pos))
				&& (!requireSight || hasLineOfSight(level, eye, pos))) {
				candidates.add(pos);
			}

			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (seen.contains(next) || !passable(level, next)) {
					continue;
				}
				seen.add(next);
				depth.put(next, d + 1);
				queue.add(next);
			}
		}

		if (candidates.isEmpty()) {
			return null;
		}
		// Furthest wins WHEN HE IS MEANT TO HAVE BEEN THERE ALL ALONG: the far
		// end of the corridor beats three blocks behind your shoulder, which
		// would read as an ambush rather than a figure already standing there.
		//
		// Nearest wins when he is ARRIVING, which is the hunt. The whole point
		// of that placement is to be inside swinging distance, so the far end
		// of the corridor is the one answer that fails at the only thing it was
		// asked to do. requireBehind separates them: a placement that does not
		// care about being seen is a placement that is not pretending.
		BlockPos best = candidates.get(0);
		for (BlockPos pos : candidates) {
			boolean better = requireBehind
				? pos.distSqr(start) > best.distSqr(start)
				: pos.distSqr(start) < best.distSqr(start);
			if (better) {
				best = pos;
			}
		}
		return best;
	}

	/** Air the flood-fill may spread through. */
	private static boolean passable(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).blocksMotion()
			&& level.getFluidState(pos).isEmpty();
	}

	/**
	 * Room for a person, on a floor, out of any liquid.
	 *
	 * isFaceSturdy rather than isSolid: the latter demands a full cube, so he
	 * refused to stand on slabs, stairs, paths and a great deal of ordinary
	 * cave floor, and placement failed for reasons no player could have
	 * guessed at.
	 */
	public static boolean canStand(ServerLevel level, BlockPos pos) {
		return passable(level, pos)
			&& passable(level, pos.above())
			&& level.getBlockState(pos.below())
				.isFaceSturdy(level, pos.below(), Direction.UP);
	}

	private static boolean isBehind(ServerPlayer player, Vec3 look, BlockPos pos) {
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) <= 0.25;
	}

	/**
	 * Walks the straight line between the player's eye and his head, stepping
	 * in half-blocks. Cheap, and precise enough — a corner blocks it, a
	 * corridor does not.
	 */
	private static boolean hasLineOfSight(ServerLevel level, Vec3 eye, BlockPos pos) {
		Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5);
		Vec3 delta = target.subtract(eye);
		int steps = (int)(delta.length() * 2);
		for (int i = 1; i < steps; i++) {
			Vec3 point = eye.add(delta.scale((double)i / steps));
			if (level.getBlockState(BlockPos.containing(point)).blocksMotion()) {
				return false;
			}
		}
		return true;
	}
}
