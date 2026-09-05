package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.entity.HerobrineEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WHAT HE THROWS GOES OFF.
 *
 * SmallFireball does not explode — five points of fire damage and a lit block,
 * which is a blaze's attack and reads like one. What the hunt wanted was a
 * BLAST, and the obvious route was the ghast's LargeFireball, except that its
 * explosionPower is an int and one is already its floor. A ghast blast is
 * Level.explode at radius 1.0, next to TNT's 4.0 — so "like a ghast but smaller"
 * cannot be expressed by that constructor at all.
 *
 * Hence a custom detonation on impact, where the radius is a float and can be
 * whatever it should be. One and two tenths: fractionally more than the ghast,
 * because it has to read as a blast rather than as a spark, and a very long way
 * under anything that would level a wall.
 *
 * TWO THINGS IT DELIBERATELY DOES NOT DO.
 *
 * It does not start fires. ExplosionInteraction with the fire flag is how a
 * ghast lights a nether floor, and this mod has already burned an entire
 * dimension to the ground once by being relaxed about fire that spreads. The
 * fireball still ignites what it hits directly — SmallFireball's own behaviour,
 * untouched — so the flavour survives without the fire front.
 *
 * And it does not break anybody's build. Block damage is refused near anything
 * a player placed, on top of the usual mobGriefing check, which means a shot
 * aimed at somebody standing in their own house scorches them and leaves the
 * house standing. A hunt is allowed to be terrifying; it is not allowed to be a
 * demolition somebody has to repair.
 *
 * NARROW. Only fireballs whose owner is him, so every blaze in every world takes
 * the first branch and leaves.
 */
@Mixin(SmallFireball.class)
public abstract class HisBlastMixin {

	/** A shade over the ghast's 1.0, and a very long way under TNT's 4.0. */
	/** Act one's fire. Small fireballs are only thrown in act one, so this is act one's number: down from 1.2 so the first act can be stood in. */
	private static final float BLAST = 0.95F;

	@Inject(method = "onHit", at = @At("TAIL"))
	private void herobrine$blast(HitResult hit, CallbackInfo info) {
		SmallFireball ball = (SmallFireball)(Object)this;
		if (!(ball.level() instanceof ServerLevel here)
			|| !(ball.getOwner() instanceof HerobrineEntity)) {
			return;
		}
		Vec3 at = hit.getLocation();
		// The gamerule and nothing else. There used to be a second condition —
		// anythingBuiltNear — and it read as "anything a player placed is off the
		// table", which is not what it did: DwellTracker is a material list, so it
		// meant his fireballs could not scratch a plank anywhere in the world,
		// including a village, a mineshaft and his own house. Somebody who turned
		// mobGriefing off has said what they want. Everybody else gets the fireball.
		boolean digs = here.getGameRules().get(GameRules.MOB_GRIEFING);
		here.explode(ball, at.x, at.y, at.z, BLAST,
			digs ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
	}
}
