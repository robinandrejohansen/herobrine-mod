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
import net.minecraft.world.phys.AABB;

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
	private static final double REACH = 2.5;

	private record Chew(BlockPos pos, int ticks) {}

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
				AABB around = player.getBoundingBox().inflate(WITNESS_RANGE);
				for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, around)) {
					if (zombie.getTarget() instanceof Player) {
						hunting.add(zombie);
					}
				}
			}
			for (Zombie zombie : hunting) {
				advance(level, zombie);
			}
			forget(level, hunting);
		}
	}

	private static void advance(ServerLevel level, Zombie zombie) {
		BlockPos target = glassInReach(level, zombie);
		Chew chew = chewing.get(zombie.getUUID());

		if (target == null) {
			if (chew != null) {
				clear(level, zombie, chew.pos());
			}
			return;
		}
		// Moved on to a different pane — the old one heals.
		if (chew != null && !chew.pos().equals(target)) {
			clear(level, zombie, chew.pos());
			chew = null;
		}

		int ticks = (chew == null ? 0 : chew.ticks()) + 1;
		chewing.put(zombie.getUUID(), new Chew(target, ticks));

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
		for (int dy = 0; dy <= 1; dy++) {
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				BlockPos pos = feet.above(dy).relative(facing);
				if (!breakable(level.getBlockState(pos))) {
					continue;
				}
				if (zombie.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
					<= REACH * REACH) {
					return pos;
				}
			}
		}
		return null;
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

	/** Drop anything whose zombie has wandered off, died or unloaded. */
	private static void forget(ServerLevel level, Set<Zombie> hunting) {
		Set<UUID> live = new HashSet<>();
		for (Zombie zombie : hunting) {
			live.add(zombie.getUUID());
		}
		Iterator<Map.Entry<UUID, Chew>> it = chewing.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Chew> entry = it.next();
			if (live.contains(entry.getKey())) {
				continue;
			}
			// Heal the pane if the zombie is still around to key the overlay
			// to. If it is not — dead, despawned, unloaded — the overlay is
			// already gone with it and only the entry needs dropping. That
			// entry must go either way, or the map grows for the rest of the
			// session with one leak per zombie that ever touched a window.
			if (level.getEntity(entry.getKey()) instanceof Zombie gone) {
				level.destroyBlockProgress(gone.getId(), entry.getValue().pos(), -1);
			}
			it.remove();
		}
	}
}
