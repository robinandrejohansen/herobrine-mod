package com.bloomlet.herobrine.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Where the ground actually is, which is not what the heightmaps say.
 *
 * Every heightmap Minecraft offers answers a different question and none of
 * them answers this one. MOTION_BLOCKING_NO_LEAVES sounds exactly right and is
 * a trap: it drops the leaves and keeps the TRUNK, so in a forest it reports
 * the top of a tree. Building a house on that put it in the air above the
 * canopy — which is precisely what happened, and it happened in the one biome
 * most likely to be tested in.
 *
 * WORLD_SURFACE and OCEAN_FLOOR have the same problem for the same reason. A
 * log is a solid, motion-blocking, non-leaf block, and no amount of picking a
 * different heightmap changes that.
 *
 * So this starts where the heightmap points and walks down until it finds
 * something a house could actually stand on: a full solid cube that is not
 * wood. Grass, dirt, stone, sand, gravel and snow all qualify; trunks, leaves,
 * grass tufts, flowers, snow layers and water do not.
 */
public final class Ground {
	private Ground() {}

	/** How far below the canopy to keep looking before giving up. */
	private static final int MAX_DESCENT = 40;

	/** @return the Y of the topmost block a structure could rest on */
	public static int topOf(ServerLevel level, int x, int z) {
		int from = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		int floor = level.getMinY() + 1;

		for (int y = from; y >= Math.max(floor, from - MAX_DESCENT); y--) {
			if (isFooting(level.getBlockState(new BlockPos(x, y, z)))) {
				return y;
			}
		}
		return from - 1;
	}

	/** @return the Y a structure's floor sits at, one above the ground */
	public static int floorOver(ServerLevel level, int x, int z) {
		return topOf(level, x, z) + 1;
	}

	/**
	 * IS THERE DRY LAND HERE, OR IS THIS THE SEABED?
	 *
	 * topOf answers "what is the highest thing you could stand on", and it does
	 * that by scanning DOWN past anything that is not footing — which includes
	 * water. Over an ocean it therefore returns the sea floor, cheerfully, with
	 * no indication that there are thirty blocks of sea on top of it.
	 *
	 * Every caller that builds something has to ask this as well, and until now
	 * none of them did. The keep sited over water put its courtyard floor at the
	 * seabed, cleared the inside to air, ran its curtain eleven blocks up — five
	 * short of the surface — and the sea came straight back in over the top. A
	 * castle in a bowl, underwater, with a city drowning around it.
	 *
	 * Two tests rather than one. Above sea level catches the open ocean; no fluid
	 * in the block above the footing catches everything else — a lake in a hollow
	 * on a hill, a river, a spring.
	 */
	public static boolean dry(ServerLevel level, int x, int z) {
		int top = topOf(level, x, z);
		if (top <= level.getSeaLevel()) {
			return false;
		}
		return level.getFluidState(new BlockPos(x, top, z)).isEmpty()
			&& level.getFluidState(new BlockPos(x, top + 1, z)).isEmpty();
	}

	private static boolean isFooting(BlockState state) {
		if (state.isAir() || !state.isSolid()) {
			return false;   // grass tufts, flowers, snow layers, water
		}
		return !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES);
	}
}
