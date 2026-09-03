package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * THE DARK.
 *
 * The first thing in the mod that happens TO the world rather than in it.
 *
 * Every event before this one is deniable and local: a sound, a figure, a sign
 * on your wall. This one takes the sky. The day ends early, the storm arrives
 * in seconds rather than over a morning, every torch you own goes out at once,
 * and the lightning starts landing near enough to hear the ground take it. A
 * player can argue their way out of a shape at the treeline. Nobody argues
 * their way out of the afternoon ending.
 *
 * NOTHING HERE DESTROYS ANYTHING, and that constraint did most of the design
 * work. The bolts are visual-only, so no fire and no damage — checked in
 * LightningBolt rather than assumed, where the visualOnly flag gates both the
 * spawnFire call and the whole thunderHit block. The torches are DROPPED, not
 * broken, so the player picks them straight back up. What they lose is the
 * next two minutes and their nerve, and they can have both back.
 *
 * It reaches indoors on purpose. Hiding in a lit room is the correct answer to
 * almost everything else here and it is the wrong answer to this: the room
 * goes dark with them in it, and the strikes land in the field outside the
 * window. Shelter stops being safety and becomes a box they are in.
 */
public final class TheDark {
	private TheDark() {}

	/** How far out the lights go. Comfortably a whole camp or small base. */
	private static final int REACH = 22;
	/** Vertical reach is smaller, so a cellar below is not stripped as well. */
	private static final int REACH_Y = 6;

	/** Two and a half minutes of storm, then the world has it back. */
	private static final int STORM_TICKS = 3000;

	private static final int BOLTS = 9;
	private static final double BOLT_NEAR = 7.0;
	private static final double BOLT_FAR = 26.0;

	/**
	 * Take the sky.
	 *
	 * @return false only if there is nothing here to take — no torches and it
	 *         is already the middle of a storm at night, in which case this
	 *         would be an event the player could not perceive at all
	 */
	public static boolean fall(ServerLevel level, ServerPlayer player) {
		boolean wasDay = level.isBrightOutside();
		boolean wasCalm = !level.isThundering();
		int snuffed = snuff(level, player);

		if (!wasDay && !wasCalm && snuffed == 0) {
			return false;   // already night, already storming, already dark
		}

		// The day ends. Not "gets on toward evening" — ends.
		//
		// 26.2 has no setDayTime any more; time is a WorldClock and the way to
		// move it is the same time marker `/time set midnight` uses.
		// resolveTimeToMoveTo returns `totalTicks + durationToNext(...)`, so it
		// can only ever go FORWARD — which is the behaviour wanted here anyway.
		// Winding back to reach midnight would hand the player a whole extra
		// night they had not earned, and that is a gift, not a threat.
		MinecraftServer server = level.getServer();
		if (wasDay) {
			Optional<? extends Holder<WorldClock>> clock =
				level.registryAccess().get(WorldClocks.OVERWORLD);
			clock.ifPresent(held ->
				server.clockManager().moveToTimeMarker(held, ClockTimeMarkers.MIDNIGHT));
		}

		// Thunder, immediately. A zero clear-time is the honest way to say "and
		// it is storming now" — anything gentler ramps in over a minute and the
		// player never connects it to anything.
		server.setWeatherParameters(0, STORM_TICKS, true, true);

		ManifestationDirector.noteLocation(player.blockPosition());
		HerobrineMod.LOGGER.info("the dark falls on {}: {} lights taken, day={} calm={}",
			player.getName().getString(), snuffed, wasDay, wasCalm);

		strikes(level, player);
		return true;
	}

	/**
	 * Every torch, not three of them.
	 *
	 * The stare takes a couple on its way out and that is a detail. This is
	 * the event, so it takes the lot — a base that loses three torches is
	 * untidy, a base that loses all of them is somewhere the player now has to
	 * cross in the dark.
	 *
	 * Dropped rather than destroyed, and only torches. Nothing that strands
	 * anybody, nothing that undoes work: lanterns, campfires, glowstone and
	 * every other light are left exactly alone, so the player who invested in
	 * real lighting is rewarded for it and the player who did not is the one
	 * standing in the dark.
	 */
	private static int snuff(ServerLevel level, ServerPlayer player) {
		if (!com.bloomlet.herobrine.Config.get().takeTheLight) {
			return 0;
		}
		BlockPos origin = player.blockPosition();
		int taken = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-REACH, -REACH_Y, -REACH),
				origin.offset(REACH, REACH_Y, REACH))) {
			net.minecraft.world.level.block.state.BlockState lit = level.getBlockState(pos);   // once, not twice
			if (!lit.is(Blocks.TORCH) && !lit.is(Blocks.WALL_TORCH)) {
				continue;
			}
			BlockPos at = pos.immutable();
			level.removeBlock(at, false);
			level.addFreshEntity(new ItemEntity(level,
				at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5,
				new ItemStack(Items.TORCH)));
			level.playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
				SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.3F);
			taken++;
		}
		return taken;
	}

	/**
	 * Lightning, close, and none of it real.
	 *
	 * setVisualOnly(true) gates both the fire and the damage inside
	 * LightningBolt, so this cannot burn a forest, cannot cook the player and
	 * cannot destroy a single block. It is pure noise and light — which is all
	 * a lightning strike ever was to somebody standing under it.
	 *
	 * Placed at the SURFACE above the ground rather than at the player, so it
	 * still lands out in the field when they are hiding in a cellar and they
	 * hear it come down through the ceiling. Spread over several seconds and
	 * at unequal intervals, because a burst that arrives all at once is one
	 * event and the same bolts arriving raggedly are a storm that has taken an
	 * interest.
	 */
	private static void strikes(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		int when = 0;
		for (int i = 0; i < BOLTS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = BOLT_NEAR + random.nextDouble() * (BOLT_FAR - BOLT_NEAR);
			int x = (int)Math.round(player.getX() + Math.cos(angle) * range);
			int z = (int)Math.round(player.getZ() + Math.sin(angle) * range);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			final BlockPos at = new BlockPos(x, y, z);

			when += 12 + random.nextInt(34);
			Cadence.in(level.getServer(), when, () -> {
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT
					.create(level, EntitySpawnReason.EVENT);
				if (bolt == null) {
					return;
				}
				bolt.setVisualOnly(true);
				bolt.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
				level.addFreshEntity(bolt);
			});
		}
	}
}
