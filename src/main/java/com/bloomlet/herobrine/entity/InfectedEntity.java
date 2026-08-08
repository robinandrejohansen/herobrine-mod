package com.bloomlet.herobrine.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * What the cells were for.
 *
 * Its own entity rather than a dressed-up villager, and that was the right call
 * for a reason that has nothing to do with taste: modifying how a villager
 * renders means reaching into vanilla's villager renderer, which cost two
 * startup crashes and three wrong diagnoses before it was abandoned. A mob of
 * our own has its own renderer and its own texture and cannot break anybody
 * else's. Real villagers are left completely alone.
 *
 * Built on Zombie because a zombie already does the difficult half. It wants to
 * reach the player, it paths, it attacks, it cannot open an iron door. Writing
 * that again would only produce a worse zombie.
 *
 * Ordinary zombie strength and health. It is not a boss and it is not meant to
 * be survived by fighting well — it is meant to be met in a corridor you chose
 * to open, which is a different kind of frightening and does not need bigger
 * numbers.
 *
 * It does not burn in daylight. Everything else about this mod is a thing that
 * lasts, and a monster the player can defeat by opening a door and waiting for
 * noon would make the whole cellar a puzzle with a trick answer.
 */
public class InfectedEntity extends Zombie {

	public InfectedEntity(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Zombie.createAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.26)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	public boolean isSunSensitive() {
		return false;
	}

	/** No reinforcements. There is one of these per cell and that is the point. */
	@Override
	protected boolean canSpawnInLiquids() {
		return false;
	}
}
