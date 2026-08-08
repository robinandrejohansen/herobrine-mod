package com.bloomlet.herobrine.manifest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * They get through the window.
 *
 * A base with glass in it is not a base, it is a diorama — the player can see
 * out, the horde cannot get in, and both sides know it. This is the one thing
 * that makes a night indoors tense rather than a formality.
 *
 * The whole feature is built around one restraint: GLASS ONLY. Not walls, not
 * doors, not floors. That is not timidity, it is what keeps this on the right
 * side of DESIGN.md §9 — a pane costs a shovelful of sand and thirty seconds
 * to replace, so losing one is a fright rather than a loss. Letting them chew
 * through stone would mean coming home to a demolished base with nothing to be
 * done about it, and no scare is worth that.
 *
 * It is slow on purpose. Ten seconds per pane, with the real vanilla crack
 * overlay spreading across it the whole time and the glass ticking under their
 * hands. The player is meant to watch it happening and have time to decide:
 * block it up, fight them off, or stand there and let it finish. A pane that
 * simply vanished would be a notification. One that visibly crazes over while
 * you work out what to do is a siege.
 *
 * Gated to SIEGE, the final phase, so nobody meets this by accident.
 */
public final class Breach {
	private Breach() {}

	/** Ten seconds a pane. Long enough to watch, short enough to matter. */
	private static final int CHEW_TICKS = 200;
	/** They only do it where someone is there to see it. */
	private static final double WITNESS_RANGE = 40.0;
	/** How far from the glass they can reach. */
	private static final double REACH = 3.2;
	/** How far they will notice you through a window. */
	private static final double SEEK_RANGE = 24.0;
	/** A wall is a wall. Two panes and a greenhouse roof is not a sightline. */
	private static final int PANES_SEEN_THROUGH = 3;

	private record Chew(BlockPos pos, int ticks, int away) {}

	/** Ticks a zombie may be off the pane before its progress is given up. */
	private static final int GRACE = 60;

	private static final Map<UUID, Chew> chewing = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Breach::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (!Wrath.phase(server).atLeast(Phase.SIEGE)) {
			if (!chewing.isEmpty()) {
				chewing.clear();
			}
			return;
		}

		for (ServerLevel level : server.getAllLevels()) {
			// Gathered into a set first: a zombie stood between two players
			// would otherwise be advanced twice in one tick and chew at double
			// speed, which is exactly the kind of thing nobody would ever
			// reproduce deliberately.
			Set<Zombie> hunting = new HashSet<>();
			for (ServerPlayer player : level.players()) {
				notice(level, player);
				AABB around = player.getBoundingBox().inflate(WITNESS_RANGE);
				for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, around)) {
					if (zombie.getTarget() instanceof Player) {
						hunting.add(zombie);
					}
				}
			}
			Set<UUID> touched = new HashSet<>();
			for (Zombie zombie : hunting) {
				advance(level, zombie);
				touched.add(zombie.getUUID());
			}
			forget(level, touched);
		}
	}

	/**
	 * They can see you through the window.
	 *
	 * Without this the whole feature never fires. Glass has a collision box, so
	 * vanilla's hasLineOfSight treats a pane as a solid wall and the targeting
	 * goal drops the player the moment they step behind one — the zombie
	 * forgets you exist, wanders off, and never gets near enough to chew
	 * anything. Standing at a window watching them lose interest is exactly the
	 * "base is a diorama" problem this was built to fix.
	 *
	 * Only fills in an EMPTY target rather than overwriting whatever it is
	 * already doing, so a zombie mid-fight with an iron golem is not yanked off
	 * it. If the goals then drop the player again for the same reason, the next
	 * tick puts them back, which makes this self-healing rather than a fight
	 * with the AI.
	 */
	private static void notice(ServerLevel level, ServerPlayer player) {
		AABB around = player.getBoundingBox().inflate(SEEK_RANGE);
		for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, around)) {
			if (zombie.getTarget() != null || !zombie.isAlive()) {
				continue;
			}
			if (throughGlass(level, zombie, player)) {
				zombie.setTarget(player);
			}
		}
	}

	/**
	 * Is the only thing in the way glass?
	 *
	 * Vanilla's own sightline check with the panes stepped over: clip, and if
	 * what stopped the ray is something they could break, start again from just
	 * past it. Capped at three so this stays "they can see through a window"
	 * and never becomes "they can see through a greenhouse and four walls".
	 */
	private static boolean throughGlass(ServerLevel level, Zombie zombie, ServerPlayer player) {
		Vec3 from = zombie.getEyePosition();
		Vec3 to = player.getEyePosition();

		for (int panes = 0; panes <= PANES_SEEN_THROUGH; panes++) {
			BlockHitResult hit = level.clip(new ClipContext(
				from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, zombie));
			if (hit.getType() == HitResult.Type.MISS) {
				return true;
			}
			if (!breakable(level.getBlockState(hit.getBlockPos()))) {
				return false;
			}
			// Step out of the pane we stopped on and carry on looking.
			//
			// This has to walk right out of the block. Nudging a tenth of a
			// block past the hit point lands INSIDE the glass, and clipping
			// from inside a block hits that same block again — so the loop
			// spent all its attempts on one pane and reported that the player
			// could not be seen. That was the whole bug: no zombie ever took a
			// target through a window, so nothing sustained a chew.
			Vec3 direction = to.subtract(from).normalize();
			BlockPos pane = hit.getBlockPos();
			Vec3 onward = hit.getLocation();
			for (int step = 1; step <= 24; step++) {
				onward = hit.getLocation().add(direction.scale(0.1 * step));
				if (!BlockPos.containing(onward).equals(pane)) {
					break;
				}
			}
			if (onward.distanceToSqr(to) >= from.distanceToSqr(to)) {
				return true;
			}
			from = onward;
		}
		return false;
	}

	/**
	 * One tick of work on one pane.
	 *
	 * It STAYS on the pane it started, and that is the important part. The
	 * first version re-chose a pane every tick and reset the moment the choice
	 * came out differently — which it did constantly, because a zombie shuffling
	 * against a wall changes block position and the scan then found a different
	 * neighbouring pane. The result was a window that took one crack and never
	 * took a second.
	 */
	private static void advance(ServerLevel level, Zombie zombie) {
		Chew chew = chewing.get(zombie.getUUID());
		BlockPos target;

		if (chew != null && stillWorth(level, zombie, chew.pos())) {
			target = chew.pos();
		} else {
			if (chew != null) {
				clear(level, zombie, chew.pos());
			}
			chew = null;
			target = glassInReach(level, zombie);
		}
		if (target == null) {
			return;
		}

		int ticks = (chew == null ? 0 : chew.ticks()) + 1;
		chewing.put(zombie.getUUID(), new Chew(target, ticks, 0));

		BlockState state = level.getBlockState(target);
		// The vanilla crack overlay, keyed to this zombie so two of them on
		// the same pane do not fight over it.
		level.destroyBlockProgress(zombie.getId(), target, ticks * 10 / CHEW_TICKS);

		if (ticks % 8 == 0) {
			level.playSound(null, target, state.getSoundType().getHitSound(),
				SoundSource.HOSTILE, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.3F);
		}
		if (ticks >= CHEW_TICKS) {
			// No drops. It did not mine the glass, it broke it.
			level.destroyBlock(target, false, zombie);
			clear(level, zombie, target);
		}
	}

	/** Still glass, and still close enough to keep working on. */
	private static boolean stillWorth(ServerLevel level, Zombie zombie, BlockPos pos) {
		return breakable(level.getBlockState(pos)) && inReach(zombie, pos);
	}

	private static boolean inReach(Zombie zombie, BlockPos pos) {
		return zombie.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
			<= REACH * REACH;
	}

	/**
	 * A pane it can actually reach, at a height it could climb through.
	 *
	 * Deliberately not "any glass nearby" — a zombie chewing a greenhouse roof
	 * two blocks over its head while the player watches from inside is comic.
	 * It has to be glass in the way.
	 */
	private static @org.jspecify.annotations.Nullable BlockPos glassInReach(
			ServerLevel level, Zombie zombie) {
		BlockPos feet = zombie.blockPosition();
		BlockPos best = null;
		double nearest = Double.MAX_VALUE;

		for (int dy = 0; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					BlockPos pos = feet.offset(dx, dy, dz);
					if (!breakable(level.getBlockState(pos)) || !inReach(zombie, pos)) {
						continue;
					}
					double distance = zombie.distanceToSqr(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
					if (distance < nearest) {
						nearest = distance;
						best = pos;
					}
				}
			}
		}
		return best;
	}

	/**
	 * Glass, and only glass.
	 *
	 * Barriers are excluded despite being in the same tag — they are an admin
	 * block and breaking one would be a genuine bug. Iron bars look like panes
	 * and are not: they cost iron, so they stay, which also gives the player a
	 * real answer to this. Bar your windows and they cannot get in.
	 */
	private static boolean breakable(BlockState state) {
		if (state.is(Blocks.BARRIER) || state.is(Blocks.IRON_BARS)) {
			return false;
		}
		return state.is(BlockTags.IMPERMEABLE) || state.getBlock() instanceof IronBarsBlock;
	}

	private static void clear(ServerLevel level, Zombie zombie, BlockPos pos) {
		level.destroyBlockProgress(zombie.getId(), pos, -1);
		chewing.remove(zombie.getUUID());
	}

	/**
	 * Give up on anything that has stopped working, but not immediately.
	 *
	 * The grace period exists because a zombie's target flickers: the goals
	 * drop the player for a tick, this misses it for that tick, and without a
	 * grace period its progress was thrown away and started again from nothing
	 * every couple of seconds. Sixty ticks is long enough to ride out the
	 * wobble and far shorter than a pane takes to break.
	 */
	private static void forget(ServerLevel level, Set<UUID> touched) {
		Iterator<Map.Entry<UUID, Chew>> it = chewing.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Chew> entry = it.next();
			if (touched.contains(entry.getKey())) {
				continue;
			}
			Chew chew = entry.getValue();
			if (chew.away() < GRACE) {
				entry.setValue(new Chew(chew.pos(), chew.ticks(), chew.away() + 1));
				continue;
			}
			// Heal the pane if the zombie is still around to key the overlay
			// to. If it is not — dead, despawned, unloaded — the overlay went
			// with it and only the entry needs dropping.
			if (level.getEntity(entry.getKey()) instanceof Zombie gone) {
				level.destroyBlockProgress(gone.getId(), chew.pos(), -1);
			}
			it.remove();
		}
	}
}
