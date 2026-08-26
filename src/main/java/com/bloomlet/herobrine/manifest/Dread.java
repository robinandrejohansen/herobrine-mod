package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * WHAT THE REST OF THE WORLD DOES ABOUT HIM.
 *
 * Rank cannot be asserted, only witnessed, and everything else in this mod is him
 * doing something TO the player. A number they cannot price. What settles it is
 * what he does to things they already understand — a field of cows, a zombie, a
 * torch on their own wall.
 *
 * THREE THINGS, ALL OF THEM THE SAME SENTENCE.
 *
 * Every animal for forty blocks runs, and runs the moment he crests the hill
 * rather than when he gets close, so what a player sees first is the FIELD moving
 * — and then the reason for it walking in behind them.
 *
 * Every hostile runs too, which is the one nothing else in Minecraft does. A
 * creeper backing away is not a mechanic the player has any category for, and
 * they will remember it longer than any damage number.
 *
 * And the light goes out. Not broken, not dropped — the torches on their own
 * walls go red and stop being enough, and come back the moment he is far enough
 * away. Nothing is destroyed and nothing can be repaired, which leaves them with
 * an effect they cannot answer.
 */
public final class Dread {
	private Dread() {}

	/** How far the field feels him. Deliberately further than he can see. */
	private static final double FLEES_AT = 40.0;
	/** And how far the light fails. Inside this, nothing is lit properly. */
	private static final int DARKENS_AT = 18;
	/** And how far above and below. A cellar two floors down is not his problem. */
	private static final int DARKENS_DEEP = 5;
	/**
	 * The light is checked half as often as the herd runs.
	 *
	 * A radius of eighteen by five is twelve thousand block lookups a sweep, and at
	 * twice a second that is a real bill for something nobody can perceive changing
	 * that fast. Animals need the faster clock — a stampede has to start on the
	 * same beat he appears. A torch does not.
	 */
	private static final int LIGHT_EVERY = 20;
	/** With hysteresis, so a torch on the edge does not flicker every second. */
	private static final int RELIGHTS_AT = 30;
	/** Twice a second is enough for a stampede and cheap enough to run always. */
	private static final int EVERY = 10;

	/** What was there before he came, so it can be exactly what it was after. */
	private static final java.util.Map<net.minecraft.resources.ResourceKey<
		net.minecraft.world.level.Level>, java.util.Map<BlockPos, BlockState>> doused =
			new java.util.HashMap<>();

	private static int ticks;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Dread::onTick);
		// A server that stops mid-darkening would leave somebody's base red
		// forever. Everything goes back before the lights go off.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				relight(level, null);
			}
		});
	}

	private static void onTick(MinecraftServer server) {
		if (++ticks % EVERY != 0 || !Config.get().enabled) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			java.util.List<HerobrineEntity> them = level.getEntitiesOfClass(
				HerobrineEntity.class,
				new net.minecraft.world.phys.AABB(-30000000, level.getMinY(), -30000000,
					30000000, level.getMaxY(), 30000000),
				him -> him.isAlive());
			if (them.isEmpty()) {
				relight(level, null);
				continue;
			}
			for (HerobrineEntity him : them) {
				scatter(level, him);
				if (ticks % LIGHT_EVERY == 0) {
					douse(level, him);
				}
			}
			relight(level, them);
		}
	}

	/**
	 * THE FIELD MOVES BEFORE HE DOES.
	 *
	 * getPosAway is the same call a panicking chicken uses when it is on fire, so
	 * the motion is one every player has seen a thousand times and read instantly
	 * as terror. Applied to a cow it is quaint. Applied to forty of them at once,
	 * away from one figure walking, it is the best shot in the mod.
	 *
	 * Nothing of his, and nothing already running — re-issuing the path twice a
	 * second would hold them in place jittering instead of letting them go.
	 */
	private static void scatter(ServerLevel level, HerobrineEntity him) {
		Vec3 from = him.position();
		for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class,
				him.getBoundingBox().inflate(FLEES_AT),
				m -> m.isAlive() && !(m instanceof HerobrineEntity)
					&& !TheHunt.isHis(m)
					// EXCEPT THE THREE THAT COME FOR HIM.
					//
					// Golems and illagers were just told to seek him out, and this
					// would have told them to run from him on the same tick — and
					// cleared the target while it did it. The only things in the
					// game that do not fear him should not be the things sprinting
					// away from him.
					&& !HerobrineEntity.challenger(m))) {
			if (!mob.getNavigation().isDone()) {
				continue;
			}
			Vec3 away = DefaultRandomPos.getPosAway(mob, 20, 8, from);
			if (away == null) {
				continue;
			}
			// Faster than they have any business moving. A herd ambling away is a
			// herd that has not understood.
			mob.getNavigation().moveTo(away.x, away.y, away.z, 1.9);
			mob.setTarget(null);
		}
	}

	/**
	 * AND THE LIGHT FAILS.
	 *
	 * Redstone rather than removal, and that choice is the whole effect. A torch
	 * that vanishes is damage — the player rebuilds it and the moment is over. A
	 * torch that is still on the wall, still burning, and no longer bright enough
	 * to keep anything out is a thing they can look directly at and do nothing
	 * about.
	 *
	 * Every position is remembered with exactly what it was, so relight puts back
	 * the block rather than a guess at it: soul torches come back soul, wall
	 * torches come back on the right wall.
	 */
	private static void douse(ServerLevel level, HerobrineEntity him) {
		java.util.Map<BlockPos, BlockState> kept =
			doused.computeIfAbsent(level.dimension(), k -> new java.util.HashMap<>());
		BlockPos at = him.blockPosition();
		for (int dx = -DARKENS_AT; dx <= DARKENS_AT; dx++) {
			for (int dy = -DARKENS_DEEP; dy <= DARKENS_DEEP; dy++) {
				for (int dz = -DARKENS_AT; dz <= DARKENS_AT; dz++) {
					BlockPos pos = at.offset(dx, dy, dz);
					BlockState was = level.getBlockState(pos);
					BlockState red = redOf(was);
					if (red == null) {
						continue;
					}
					kept.put(pos.immutable(), was);
					level.setBlock(pos, red, 2);
				}
			}
		}
	}

	/** The same fitting, wired wrong. Null if this is not a light he touches. */
	private static @org.jspecify.annotations.Nullable BlockState redOf(BlockState was) {
		if (was.is(Blocks.TORCH) || was.is(Blocks.SOUL_TORCH)) {
			return Blocks.REDSTONE_TORCH.defaultBlockState();
		}
		if (was.is(Blocks.WALL_TORCH) || was.is(Blocks.SOUL_WALL_TORCH)) {
			return Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(
				BlockStateProperties.HORIZONTAL_FACING,
				was.getValue(BlockStateProperties.HORIZONTAL_FACING));
		}
		return null;
	}

	/** Everything he is no longer standing near goes back to what it was. */
	private static void relight(ServerLevel level,
			java.util.@org.jspecify.annotations.Nullable List<HerobrineEntity> them) {
		java.util.Map<BlockPos, BlockState> kept = doused.get(level.dimension());
		if (kept == null || kept.isEmpty()) {
			return;
		}
		java.util.Iterator<java.util.Map.Entry<BlockPos, BlockState>> walk =
			kept.entrySet().iterator();
		while (walk.hasNext()) {
			java.util.Map.Entry<BlockPos, BlockState> one = walk.next();
			boolean near = false;
			if (them != null) {
				for (HerobrineEntity him : them) {
					if (him.blockPosition().closerThan(one.getKey(), RELIGHTS_AT)) {
						near = true;
						break;
					}
				}
			}
			if (near) {
				continue;
			}
			// Only if it is still the red one. Somebody who mined it, replaced it
			// or built over it has had the last word.
			if (level.isLoaded(one.getKey())
				&& redOf(one.getValue()) != null
				&& level.getBlockState(one.getKey()).getBlock()
					== redOf(one.getValue()).getBlock()) {
				level.setBlock(one.getKey(), one.getValue(), 2);
			}
			walk.remove();
		}
	}
}
