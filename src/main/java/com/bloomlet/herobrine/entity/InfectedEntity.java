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
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			// TALLER, AND THE WHOLE THING RATHER THAN THE MODEL.
			//
			// SCALE moves the hitbox with the mesh, so this is genuinely a bigger
			// creature and not a stretched texture: it is hit where it looks like
			// it should be hit, its eyes are where its head is, and it reaches
			// from the height it appears to reach from. Scaling the model alone
			// gives a thing whose sword arrives from its knees.
			//
			// 1.6, which is 3.1 blocks against a vanilla zombie's 1.95. Double
			// was the instinct and it is too much — at 2.0 it stops reading as a
			// person who is wrong and starts reading as a different species, and
			// the whole point of these is that they used to be people.
			//
			// THE CONSEQUENCE IS THAT THEY CANNOT COME INDOORS, and that is worth
			// knowing rather than discovering. Anything over about 1.03 no longer
			// fits a two-block doorway, so these will crowd outside a house and
			// never enter one. For the cells and the open ground they are meant
			// for, that is fine and arguably better. For anything expected to
			// follow a player into a corridor, it is not.
			.add(Attributes.SCALE, 1.6);
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
