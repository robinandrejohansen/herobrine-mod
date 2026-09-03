package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.block.TheWayBlock;
import com.bloomlet.herobrine.manifest.HisHost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AND IT LEAVES A MARK ON THE GROUND, WHICH THE EXPLOSION DOES NOT.
 *
 * The fireball already explodes — LargeFireball.onHit calls Level.explode at the
 * power it was given, with MOB interaction, gated on mobGriefing — so on paper
 * the terrain damage was already there. In a dark forest it is very nearly
 * invisible, and the reason is worth writing down because it is a geometry
 * problem rather than a tuning one.
 *
 * A SKELETON SHOOTS FLAT, AT CHEST HEIGHT, AT SOMETHING STANDING ON THE GROUND.
 * What it hits, in a wood this dense, is a TRUNK — about a block and a half up.
 * A power-one blast centred there takes out some leaves and a bit of dark oak
 * and never reaches the floor at all, so the player walks through a fight that
 * has been going on around them for a minute and the ground is pristine. The
 * dimension was supposed to look worse every time they came back to it, and it
 * looked identical.
 *
 * So the ground under the impact is dished out deliberately. Small — two across
 * and one or two deep — because the point is a POCKMARKED wood rather than a
 * bombed one; twenty of these over an afternoon should read as a place that has
 * been fought over for a very long time, not as a quarry.
 *
 * NARROW, AND GATED THREE WAYS. Only in his world, only when mobGriefing allows
 * it, and only when hisHost is on. Every ghast fireball in every ordinary world
 * takes the first branch and leaves.
 */
@Mixin(LargeFireball.class)
public abstract class HisFireballMixin {

	@Inject(method = "onHit", at = @At("TAIL"))
	private void herobrine$dent(HitResult hit, CallbackInfo info) {
		LargeFireball ball = (LargeFireball)(Object)this;
		if (!(ball.level() instanceof ServerLevel here)
			|| !here.dimension().equals(TheWayBlock.HIS_WORLD)
			|| !Config.get().hisHost) {
			return;
		}
		// The same rule the explosion itself asked. Somebody who has turned
		// mobGriefing off has said they do not want terrain touched, and this is
		// terrain being touched however it is dressed up.
		if (!here.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(hit.getLocation());
		if (ball.getAttached(com.bloomlet.herobrine.entity.HerobrineEntity.BREACH) != null) {
			// Aimed at a wall on purpose: the wall goes, whatever it is made of — the
			// one he AIMED at, not only the block the ball happened to reach. A ball
			// that hits the player, or stops a block short, still opens the wall.
			Long aimed = ball.getAttached(com.bloomlet.herobrine.entity.HerobrineEntity.BREACH_AT);
			if (aimed != null) {
				HisHost.punch(here, net.minecraft.core.BlockPos.of(aimed), 1.6);
			}
			HisHost.punch(here, at, 1.2);
		}
		HisHost.dent(here, at);
	}
}
