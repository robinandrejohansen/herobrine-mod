package com.bloomlet.herobrine.manifest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Telling the player's world from the world's world.
 *
 * A ruin that appears inside someone's base is not frightening, it is
 * vandalism, and README.md rules it out. But we cannot ask the game which
 * blocks a player placed — Minecraft does not record that — so this uses the
 * next best thing: whether a block is one that only turns up because somebody
 * put it there.
 *
 * It is a heuristic and it will be wrong occasionally. A village has planks
 * and a mineshaft has fences, so he will decline to build near those too.
 * That is the right way for it to fail: refusing a legitimate site costs one
 * quiet night, while building through someone's wall costs their trust in the
 * whole mod.
 */
public final class DwellTracker {
	private DwellTracker() {}

	/**
	 * @return true if this block is evidence of habitation — crafted materials
	 *         that do not generate naturally in the overworld surface.
	 */
	public static boolean isBuilt(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();

		return block == Blocks.CRAFTING_TABLE
			|| block == Blocks.FURNACE
			|| block == Blocks.CHEST
			|| block == Blocks.BARREL
			|| block == Blocks.TORCH
			|| block == Blocks.WALL_TORCH
			|| block == Blocks.LANTERN
			|| block == Blocks.GLASS
			|| block == Blocks.GLASS_PANE
			|| block == Blocks.BOOKSHELF
			|| block == Blocks.ENCHANTING_TABLE
			|| block == Blocks.ANVIL
			|| block == Blocks.STONE_BRICKS
			|| block == Blocks.SMOOTH_STONE
			|| block == Blocks.BRICKS
			|| state.is(net.minecraft.tags.BlockTags.PLANKS)
			|| state.is(net.minecraft.tags.BlockTags.WOODEN_STAIRS)
			|| state.is(net.minecraft.tags.BlockTags.WOODEN_SLABS)
			|| state.is(net.minecraft.tags.BlockTags.DOORS)
			|| state.is(net.minecraft.tags.BlockTags.BEDS)
			|| state.is(net.minecraft.tags.BlockTags.FENCES)
			|| state.is(net.minecraft.tags.BlockTags.WOOL);
	}
}
