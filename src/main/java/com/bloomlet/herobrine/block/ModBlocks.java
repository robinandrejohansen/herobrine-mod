package com.bloomlet.herobrine.block;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The Effigy, which is the only thing this mod ever lets a player keep.
 *
 * A carved head, left standing in the wreckage where he died. It can be broken,
 * it drops itself, and it can be carried home and put on a shelf — and that is
 * the entire point of it. Forty hours of a thing that could not be touched,
 * could not be cornered and could not be proven, and at the end of it there is
 * an object. Something that will still be in a chest next week when the player
 * has half-convinced themselves the rest of it was atmosphere.
 *
 * A CUSTOM BLOCK RATHER THAN A PLAYER HEAD, and that is not fussiness. A skull
 * gets its face from a Mojang-hosted texture resolved by profile, so a genuinely
 * custom one is not possible without hosting a skin — a "Herobrine" head would
 * either fail to resolve or quietly show somebody else's face. Cutting our own
 * block sidesteps all of it and means the texture in the world is the same file
 * as the texture on him.
 *
 * Deliberately not useful. It has no recipe, no power and no function; it is a
 * trophy and it stays a trophy. The moment it grants something the whole ending
 * becomes a loot drop, and this mod has spent its entire length refusing to be
 * about loot.
 */
public final class ModBlocks {
	private ModBlocks() {}

	public static final Block EFFIGY = register("effigy",
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			// Stone-hard, so prising it out is a small act of work rather than
			// a click. A player should have to decide to take it.
			.strength(4.0F, 1200.0F)
			.sound(SoundType.DEEPSLATE)
			.requiresCorrectToolForDrops());

	/**
	 * The way through, standing in the frame he did not live to finish.
	 *
	 * No item form and no loot table, unlike the Effigy — see TheWayBlock. It is
	 * registered as a block only, so it cannot be picked up, crafted or carried
	 * home, and the one place it exists is where the mod put it.
	 *
	 * Indestructible on purpose. A player who broke their own way through in a
	 * moment of curiosity would have deleted the ending, and there is no honest
	 * way to give it back to them.
	 */
	public static final Block THE_WAY = registerBlockOnly("the_way",
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.noOcclusion()
			.noLootTable()
			.strength(-1.0F, 3600000.0F)
			.lightLevel(state -> 11)
			.sound(SoundType.GLASS)
			.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));

	private static Block registerBlockOnly(String name, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, HerobrineMod.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key,
			new TheWayBlock(properties.setId(key)));
	}

	private static Block register(String name, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, HerobrineMod.id(name));
		Block block = new Block(properties.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, HerobrineMod.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
			new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	/** Called from the mod initialiser so the static block above runs. */
	public static void register() {
		HerobrineMod.LOGGER.info("effigy registered");
	}
}
