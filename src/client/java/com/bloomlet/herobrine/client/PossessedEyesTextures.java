package com.bloomlet.herobrine.client;

import java.util.HashMap;
import java.util.Map;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * Which mobs he can show through.
 *
 * There has to be one texture per mob type and there is no way around it: the
 * eyes are drawn by submitting the whole model again with an emissive texture,
 * so the file has to use that mob's own UV layout. A cow's eye pixels are
 * nowhere near a villager's.
 *
 * Anything not listed simply does not glow. That is a deliberate choice rather
 * than a gap to be papered over — a wrong texture on an unlisted mob would put
 * two burning rectangles on its flank, and an animal that behaves wrongly with
 * no glow is still the effect. The glow is the confirmation, not the whole
 * scare. Adding a mob is a coordinate line in tools/gen_possessed_eyes.py.
 */
public final class PossessedEyesTextures {
	private PossessedEyesTextures() {}

	private static final Map<EntityType<?>, Identifier> BY_TYPE = new HashMap<>();

	static {
		add(EntityTypes.COW, "cow");
		add(EntityTypes.PIG, "pig");
		add(EntityTypes.SHEEP, "sheep");
		add(EntityTypes.VILLAGER, "villager");
	}

	private static void add(EntityType<?> type, String name) {
		BY_TYPE.put(type, HerobrineMod.id("textures/entity/possessed/" + name + ".png"));
	}

	public static @org.jspecify.annotations.Nullable Identifier forType(EntityType<?> type) {
		return BY_TYPE.get(type);
	}
}
