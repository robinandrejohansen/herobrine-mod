package com.bloomlet.herobrine.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * Lets us add a target to a vanilla mob's list without rewriting the mob.
 *
 * Mob.targetSelector is protected final. Vanilla mobs decide what they hunt in
 * their own registerGoals, and nothing about that is extensible: a zombie goes
 * after players, villagers, wandering traders, baby turtles and iron golems
 * because those five classes are named in Zombie.registerGoals, and there is no
 * hook to add a sixth.
 *
 * Addexio has to be the sixth. He is a man in enchanted plate walking through a
 * hostile country and everything in it ignoring him would be the one thing about
 * him nobody would believe.
 *
 * AN ACCESSOR RATHER THAN AN INJECTION, for the same reason as
 * the other accessors: nothing about vanilla's behaviour is altered here.
 * Every goal a mob already has still runs, in the order it was registered, under
 * vanilla's own selector. One more entry is added to a list the mob itself adds
 * entries to during construction — which is all registerGoals is doing.
 */
@Mixin(Mob.class)
public interface MobTargetsAccessor {
	@Accessor("targetSelector")
	GoalSelector herobrine$targets();
}
