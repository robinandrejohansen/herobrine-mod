package com.bloomlet.herobrine.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * THE ONE WAY TO A MAP CENTRED WHERE YOU SAY.
 *
 * MapItemSavedData.createFresh snaps the centre to a grid of 128 << scale, so a
 * map "of" a place is a map of whichever standard tile the place falls in, and
 * you can be off it. The constructor takes the centre as given. It is private.
 * See structure/Charts.
 */
@Mixin(MapItemSavedData.class)
public interface MapDataInvoker {
	@Invoker("<init>")
	static MapItemSavedData herobrine$newMap(int centerX, int centerZ, byte scale,
	                                         boolean trackingPosition, boolean unlimitedTracking,
	                                         boolean locked, ResourceKey<Level> dimension) {
		throw new AssertionError();
	}
}
