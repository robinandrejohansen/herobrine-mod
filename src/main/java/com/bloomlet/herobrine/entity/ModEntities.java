package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity registration.
 *
 * The EntityType has to be built against its own ResourceKey — the key is
 * passed into build() rather than being attached afterwards — so registration
 * and key creation happen together in {@link #register}.
 */
public final class ModEntities {
	private ModEntities() {}

	public static final EntityType<HerobrineEntity> HEROBRINE = register(
		"herobrine",
		EntityType.Builder.of(HerobrineEntity::new, MobCategory.MONSTER)
			// Player-sized. He is meant to read as "another player, wrong"
			// at distance, so matching the avatar hitbox matters.
			.sized(0.6F, 1.8F)
			.clientTrackingRange(16)
	);

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HerobrineMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Called from the mod initialiser; attributes must be registered separately from the type. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.createAttributes());
	}
}
