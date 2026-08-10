package com.bloomlet.herobrine.client;

import java.util.HashMap;
import java.util.Map;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;

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
		add(EntityTypes.CHICKEN, "chicken");
		add(EntityTypes.VILLAGER, "villager");
	}

	private static void add(EntityType<?> type, String name) {
		LOCKED.put(type, HerobrineMod.id("textures/entity/possessed/" + name + ".png"));
		HUNTING.put(type, HerobrineMod.id("textures/entity/possessed/" + name + "_hunting.png"));
	}

	/**
	 * What the WHOLE herd is wearing, by phase, whether he took it or not.
	 *
	 * Possession takes one animal and means something by it. This means
	 * something else: by HUNTER it is not one cow, it is every cow, and the
	 * player looks up from a fence line to find the entire field looking back.
	 * Nothing has been done to them and nothing needs to be — the point is
	 * precisely that it is not personal any more.
	 *
	 * Decided on the CLIENT from the phase it already knows, rather than by
	 * writing an attachment onto every animal in the world. The server would
	 * have to touch and sync thousands of entities to say something it has
	 * already told the client once, and the answer would be identical.
	 *
	 * Villagers are left out. They get eyes only by being taken, because a
	 * whole town of them staring is a different story from the one this tells,
	 * and normal villagers were asked for by name.
	 */
	public static int herdMenace(EntityType<?> type, Phase phase) {
		if (type != EntityTypes.COW && type != EntityTypes.PIG
			&& type != EntityTypes.SHEEP && type != EntityTypes.CHICKEN) {
			return 0;
		}
		if (phase.atLeast(Phase.SIEGE)) {
			return 2;
		}
		return phase.atLeast(Phase.HUNTER) ? 1 : 0;
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
