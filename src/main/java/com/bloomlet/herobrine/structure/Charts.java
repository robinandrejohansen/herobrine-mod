package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.mixin.MapDataInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * EVERY MAP THE MOD LEAVES, DRAWN SO THAT YOU AND THE MARK ARE BOTH ON IT.
 *
 * A map is only a direction if your own arrow and the red cross are on the same
 * sheet. Every map used to be made with MapItem.create, centred "on" the target
 * at a fixed scale — and create snaps the centre to a grid of 128 << scale, so
 * the target could sit anywhere in its tile, including at the edge with you two
 * tiles away. People stood over a map with a cross on it and guessed.
 *
 * This centres the sheet on the midpoint between where the map is found and
 * where it points, and picks the smallest scale on which both fit inside the
 * middle FILLS of the sheet. Scale 0 is a hundred and twenty-eight blocks
 * across; each step doubles it; four is the largest the game has, two thousand
 * and forty-eight, which holds anything the mod builds. Unlimited tracking, so
 * the arrows stay even if somebody wanders off the edge. The coordinates go in
 * the name as well, because a number on the item cannot fail.
 *
 * Nothing is pre-drawn: the sheet fills in as you walk, like any map. The cross
 * and your arrow are there from the start, and that is what you steer by.
 */
public final class Charts {

	private Charts() {}

	/** Both points inside this much of the sheet's width, so neither hugs the edge. */
	private static final double FILLS = 0.78;
	private static final byte LARGEST = 4;

	public static ItemStack between(ServerLevel level, BlockPos from, BlockPos to, String name) {
		int span = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
		byte scale = 0;
		while (scale < LARGEST && (128 << scale) * FILLS < span) {
			scale++;
		}
		int centerX = (from.getX() + to.getX()) / 2;
		int centerZ = (from.getZ() + to.getZ()) / 2;
		MapItemSavedData sheet = MapDataInvoker.herobrine$newMap(
			centerX, centerZ, scale, true, true, false, level.dimension());
		MapId id = level.getFreeMapId();
		level.setMapData(id, sheet);
		ItemStack map = new ItemStack(Items.FILLED_MAP);
		map.set(DataComponents.MAP_ID, id);
		MapItemSavedData.addTargetDecoration(map, to, "+", MapDecorationTypes.RED_MARKER);
		map.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return map;
	}
}
