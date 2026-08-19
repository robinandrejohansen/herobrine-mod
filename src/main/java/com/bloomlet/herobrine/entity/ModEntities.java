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

	/**
	 * The thing in the cells.
	 *
	 * Tracked further out than he is, because unlike him it is meant to be
	 * seen coming.
	 */
	public static final EntityType<InfectedEntity> INFECTED = register(
		"infected",
		EntityType.Builder.of(InfectedEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.clientTrackingRange(8)
	);

	/**
	 * Somebody who is not on the server.
	 *
	 * Tracked as far out as the game will comfortably allow, because unlike him
	 * this one is meant to be watched for a long time from a long way off —
	 * followed, lost, and argued about. A figure that blinks out at sixteen
	 * blocks is a rendering bug; the whole event is somebody trailing him over a
	 * hill.
	 */
	public static final EntityType<MimicEntity> MIMIC = register(
		"mimic",
		EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.8F)
			.clientTrackingRange(10)
	);

	/**
	 * The villager who does not sleep.
	 *
	 * Villager-sized to the pixel, because everything about him depends on his
	 * being indistinguishable from the people standing next to him. Tracked as
	 * far out as the mimic: he is meant to be noticed across a square, watched,
	 * and argued about before anybody gets close enough to see his eyes.
	 */
	public static final EntityType<TurnedEntity> TURNED = register(
		"turned",
		EntityType.Builder.of(TurnedEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.clientTrackingRange(10)
	);

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HerobrineMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Called from the mod initialiser; attributes must be registered separately from the type. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(INFECTED, InfectedEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TURNED, TurnedEntity.createAttributes());
	}
}
