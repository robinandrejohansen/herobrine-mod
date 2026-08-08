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

	private static boolean isFooting(BlockState state) {
		if (state.isAir() || !state.isSolid()) {
			return false;   // grass tufts, flowers, snow layers, water
		}
		return !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES);
	}
}
