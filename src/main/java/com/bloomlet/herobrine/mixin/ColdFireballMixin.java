package com.bloomlet.herobrine.mixin;

import com.bloomlet.herobrine.entity.HerobrineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ACT ONE FIRE DOES NOT CATCH.
 *
 * A blaze's fireball does five on the hit and sets you alight for five seconds,
 * and puts fire on whatever block it lands beside. In the act that is meant to
 * be survivable that was the bit nobody survived: the burning, not the hit.
 * A ball he throws in act one carries HerobrineEntity.COLD, and for that one
 * the two lines that start fires are skipped. The hit still lands.
 */
@Mixin(SmallFireball.class)
public abstract class ColdFireballMixin {

	private boolean herobrine$cold() {
		return Boolean.TRUE.equals(((SmallFireball) (Object) this).getAttached(HerobrineEntity.COLD));
	}

	@Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
	private void herobrine$noBurn(Entity target, float seconds) {
		if (!this.herobrine$cold()) {
			target.igniteForSeconds(seconds);
		}
	}

	@Redirect(method = "onHitBlock", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean herobrine$noFire(Level level, BlockPos pos, BlockState state) {
		if (this.herobrine$cold()) {
			return false;
		}
		return level.setBlockAndUpdate(pos, state);
	}
}
