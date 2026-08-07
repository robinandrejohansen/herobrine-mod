package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 0 content. The cheapest things in the whole mod and, per DESIGN.md,
 * the most important.
 *
 * Every trace here must have a mundane explanation. A torch burned out. You
 * misheard. The pig died of something. The player should be able to talk
 * themselves out of it — the fear lives in the gap between "something
 * happened" and "I can prove it", and that gap only exists while the events
 * are deniable.
 *
 * Nothing here damages, takes, or blocks anything. That is what makes it safe
 * to fire at a player who has no idea the mod is installed.
 */
public final class Traces {
	private Traces() {}

	/**
	 * Footsteps behind you, once, with nothing there.
	 *
	 * Placed behind the player and close — the whole trick is that it sounds
	 * like it is inside your personal space. Stone steps rather than the local
	 * block's, because stone carries and reads as "someone in a cave".
	 */
	public static boolean footsteps(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		Vec3 behind = player.position()
			.subtract(player.getViewVector(1.0F).normalize().scale(2.5 + random.nextDouble() * 2.0));

		// Four steps, walking pace. One footstep is a glitch; four is someone.
		for (int i = 0; i < 4; i++) {
			level.playSound(null, behind.x, behind.y, behind.z,
				SoundEvents.STONE_STEP, SoundSource.HOSTILE,
				0.28F, 0.85F + random.nextFloat() * 0.1F);
		}
		return true;
	}

	/**
	 * Cave ambience where there is no cave.
	 *
	 * Deniable to the point of being almost nothing — which is the point at
	 * phase 0. Underwater ambience is used because it is unsettling out of
	 * context and players do not have it memorised the way they do cave sounds.
	 */
	public static boolean wrongSound(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = 8.0 + random.nextDouble() * 6.0;
		double x = player.getX() + Math.cos(angle) * distance;
		double z = player.getZ() + Math.sin(angle) * distance;

		level.playSound(null, x, player.getY(), z,
			SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, SoundSource.AMBIENT,
			0.7F, 0.6F + random.nextFloat() * 0.15F);
		return true;
	}

	/**
	 * A torch you placed is lying on the floor and the light is out.
	 *
	 * There is no unlit torch block in the game — torches are burning or they
	 * are an item — so this takes the placed block and drops the item at its
	 * feet. The light goes out because there is no longer a torch on the wall.
	 *
	 * That happens to be the most deniable thing in the set: torches pop off
	 * on their own in vanilla when their supporting block goes, so a torch on
	 * the floor is something players have seen happen for ordinary reasons.
	 * It is also the only trace that leaves evidence, and the evidence is an
	 * item you can pick up and put back — exactly the level of harm phase 0
	 * is allowed.
	 *
	 * Only ever takes a torch that is BEHIND the player and out of sight, so
	 * it is never witnessed happening.
	 */
	public static boolean snuffTorch(ServerLevel level, ServerPlayer player) {
		List<BlockPos> candidates = new ArrayList<>();
		BlockPos origin = player.blockPosition();
		int r = 12;

		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
			if (!level.getBlockState(pos).is(Blocks.TORCH)
				&& !level.getBlockState(pos).is(Blocks.WALL_TORCH)) {
				continue;
			}
			if (isInFrontOf(player, pos)) {
				continue;   // never let it happen where you can see it
			}
			candidates.add(pos.immutable());
		}
		if (candidates.isEmpty()) {
			return false;
		}

		BlockPos chosen = candidates.get(level.getRandom().nextInt(candidates.size()));
		level.removeBlock(chosen, false);
		// Drop rather than delete: nothing is lost, and the player can put it back.
		ItemEntity dropped = new ItemEntity(level,
			chosen.getX() + 0.5, chosen.getY() + 0.1, chosen.getZ() + 0.5,
			new ItemStack(Items.TORCH));
		dropped.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(dropped);

		level.playSound(null, chosen.getX() + 0.5, chosen.getY() + 0.5, chosen.getZ() + 0.5,
			SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
		return true;
	}

	private static boolean isInFrontOf(ServerPlayer player, BlockPos pos) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() + 0.5 - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) > 0.2;
	}
}
