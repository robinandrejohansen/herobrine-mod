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

	private static final Map<EntityType<?>, Identifier> LOCKED = new HashMap<>();
	private static final Map<EntityType<?>, Identifier> HUNTING = new HashMap<>();

	static {
		add(EntityTypes.COW, "cow");
		add(EntityTypes.PIG, "pig");
		add(EntityTypes.SHEEP, "sheep");
		add(EntityTypes.VILLAGER, "villager");
	}

	private static void add(EntityType<?> type, String name) {
		LOCKED.put(type, HerobrineMod.id("textures/entity/possessed/" + name + ".png"));
		HUNTING.put(type, HerobrineMod.id("textures/entity/possessed/" + name + "_hunting.png"));
	}

	/**
	 * @param menace 0 while it is still stalking, 1 locked on, 2 hunting
	 * @return the emissive overlay to draw, or null for no eyes at all
	 */
	public static @org.jspecify.annotations.Nullable Identifier forType(
			EntityType<?> type, int menace) {
		if (menace <= 0) {
			return null;   // stalking animals look like animals
		}
		return (menace >= 2 ? HUNTING : LOCKED).get(type);
	}
}
