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
	 * Somebody walks past behind you.
	 *
	 * The first version played four steps in a single tick, which is not four
	 * steps — it is one loud noise, and that is what it sounded like. They are
	 * now spread eight ticks apart, which is walking pace, and each one lands
	 * a little further along a line that passes behind the player rather than
	 * all at the same point.
	 *
	 * That second part matters as much as the timing. Repeated sound at one
	 * fixed position reads as a machine; sound that MOVES reads as a person,
	 * and the player turns to follow it instead of just flinching.
	 *
	 * Stone steps rather than the local block's, because stone carries and
	 * reads as "someone in a cave" wherever you happen to be standing.
	 */
	public static boolean footsteps(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 behind = player.position().subtract(look.scale(3.0 + random.nextDouble() * 1.5));

		// Across, not towards. Something crossing behind you is a person going
		// about its business; something walking at you is an attack, and at
		// phase 0 he is not attacking anybody.
		Vec3 across = new Vec3(-look.z, 0.0, look.x)
			.scale(random.nextBoolean() ? 0.55 : -0.55);
		Vec3 start = behind.subtract(across.scale(3.0));

		int steps = 7 + random.nextInt(3);
		for (int i = 0; i < steps; i++) {
			Vec3 at = start.add(across.scale(i));
			// Captured per step so each plays where that step should be.
			final double x = at.x;
			final double y = at.y;
			final double z = at.z;
			final float pitch = 0.82F + random.nextFloat() * 0.12F;
			Cadence.in(level.getServer(), i * 8, () ->
				level.playSound(null, x, y, z, SoundEvents.STONE_STEP,
					SoundSource.HOSTILE, 0.34F, pitch));
		}
		return true;
	}

	/**
	 * A creeper that is not there.
	 *
	 * The single most conditioned sound in Minecraft. Nobody thinks about it,
	 * nobody has to see anything, and every player who has ever lost a chest
	 * to one will spin round and sprint before they have decided to.
	 *
	 * The fuse is thirty ticks in vanilla and it is thirty ticks here, because
	 * the whole effect is that the player's body counts it. What arrives at the
	 * end is nothing at all — no explosion, no damage, no block broken. They
	 * brace, and the world simply carries on, and then they have to decide what
	 * they just heard.
	 *
	 * One footstep lands two ticks after the fuse should have run out, close
	 * and behind. It is the only part that is not deniable as a real creeper,
	 * and it is what turns "there must have been a creeper somewhere" into
	 * something worse.
	 *
	 * Costs nothing and takes nothing, which is what keeps it inside the phase
	 * 0 rules — but it is easily the most frightening thing available this
	 * early, so the director's suppression matters more for this than for
	 * anything else in the set.
	 */
	public static boolean fuse(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 behind = player.position().subtract(look.scale(2.0 + random.nextDouble()));

		level.playSound(null, behind.x, behind.y, behind.z,
			SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);

		final double x = behind.x;
		final double y = behind.y;
		final double z = behind.z;
		Cadence.in(level.getServer(), 32, () ->
			level.playSound(null, x, y, z, SoundEvents.STONE_STEP,
				SoundSource.HOSTILE, 0.4F, 0.8F));
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
