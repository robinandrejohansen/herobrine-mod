package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * THE FRAME, AND WHAT IS ON THE OTHER SIDE OF IT.
 *
 * Two jobs that have to agree with each other, so they live together: raising
 * the portal where he died, and making sure the ground somebody lands on when
 * they walk into it exists.
 *
 * THE SECOND ONE IS NOT A DETAIL. His world generates on the nether's noise
 * settings, which means lava at the height anybody would arrive at, and a
 * player who has just spent forty hours reaching the ending should not lose it
 * to a coin flip about what the terrain did. So the landing is CUT rather than
 * found: a floor, a roof, a rim and a way back, built into whatever was there.
 *
 * The way back matters as much as the way in. A one-way door is a trap, and a
 * trap at the end of a horror mod is not an ending, it is a bug report from
 * somebody who cannot reach their own base again.
 */
public final class TheWay {
	private TheWay() {}

	/** Five wide, five high. Big enough to be a door and not an arch. */
	private static final int WIDTH = 2;
	private static final int HEIGHT = 4;

	/**
	 * Raise it, standing, over the ground he died on.
	 *
	 * Obsidian for the frame and crying obsidian at the corners — the second is
	 * the only block in the game that reads as a portal that has been WEPT over,
	 * and the corners are where a person would have had to stand to build it.
	 */
	public static void open(ServerLevel level, BlockPos site) {
		for (int dx = -WIDTH; dx <= WIDTH; dx++) {
			for (int dy = 0; dy <= HEIGHT; dy++) {
				BlockPos at = site.offset(dx, dy, 0);
				boolean edge = Math.abs(dx) == WIDTH || dy == 0 || dy == HEIGHT;
				boolean corner = Math.abs(dx) == WIDTH && (dy == 0 || dy == HEIGHT);
				if (corner) {
					level.setBlock(at, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
				} else if (edge) {
					level.setBlock(at, Blocks.OBSIDIAN.defaultBlockState(), 2);
				} else {
					level.setBlock(at, ModBlocks.THE_WAY.defaultBlockState(), 2);
				}
			}
		}
		// A step up to it, so it does not read as a hole in the air.
		for (int dx = -WIDTH; dx <= WIDTH; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos under = site.offset(dx, -1, dz);
				if (!level.getBlockState(under).isSolid()) {
					level.setBlock(under, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
				}
			}
		}
		level.playSound(null, site, com.bloomlet.herobrine.sound.ModSounds.THE_WAY,
			net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.5F);
		HerobrineMod.LOGGER.info("the way is open at [{}, {}, {}]",
			site.getX(), site.getY(), site.getZ());
	}

	/**
	 * WHERE THEY COME OUT, CUT IF IT IS NOT ALREADY THERE.
	 *
	 * Called on the way in, on the destination level, before the teleport
	 * happens — so by the time the screen has finished going purple there is a
	 * floor under them and a frame behind them.
	 *
	 * Searched for first and built only if nothing is found, so going through a
	 * second time returns to the same landing rather than scattering chambers
	 * across the map. The search is the cheapest possible one: the same
	 * coordinates they left from, which is also the most legible — the door is
	 * in the same place on both sides of the wall.
	 */
	public static BlockPos landing(ServerLevel bound, ServerPlayer player) {
		BlockPos at = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
		// A sensible height for the nether noise settings: above the lava seas,
		// below anything that would be inside the rock ceiling.
		int y = Math.max(bound.getMinY() + 8, Math.min(96, player.getBlockY()));
		BlockPos site = at.atY(y);

		if (bound.getBlockState(site).is(ModBlocks.THE_WAY)) {
			return site;         // already been through; same door
		}
		// Look for one nearby before cutting a new one, in case the arrival
		// drifted by a block between visits.
		for (BlockPos near : BlockPos.betweenClosed(
				site.offset(-8, -6, -8), site.offset(8, 6, 8))) {
			if (bound.getBlockState(near).is(ModBlocks.THE_WAY)) {
				return near.immutable().offset(0, 0, 2);
			}
		}
		chamber(bound, site);
		return site.offset(0, 0, 3);
	}

	/**
	 * A room, hollowed out of whatever was there.
	 *
	 * Deliberately bare. This is a doorstep rather than a build — the world
	 * beyond it is the thing worth making, and a decorated arrival hall would
	 * spend the first impression on architecture instead of on the place.
	 *
	 * Everything inside it is replaced rather than tested, including lava. That
	 * is the whole reason it exists.
	 */
	private static void chamber(ServerLevel level, BlockPos site) {
		int r = 7;
		for (BlockPos pos : BlockPos.betweenClosed(
				site.offset(-r, -2, -r), site.offset(r, HEIGHT + 3, r))) {
			double away = Math.max(Math.abs(pos.getX() - site.getX()),
				Math.abs(pos.getZ() - site.getZ()));
			boolean shell = away >= r || pos.getY() <= site.getY() - 2
				|| pos.getY() >= site.getY() + HEIGHT + 3;
			level.setBlock(pos, shell
				? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
				: Blocks.AIR.defaultBlockState(), 2);
		}
		for (BlockPos pos : BlockPos.betweenClosed(
				site.offset(-r + 1, -1, -r + 1), site.offset(r - 1, -1, r - 1))) {
			level.setBlock(pos, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
		}
		// The way back, facing the room, so it is the first thing they see when
		// they turn round. Nobody is stranded here.
		open(level, site);
		for (int dx : new int[] { -3, 3 }) {
			level.setBlock(site.offset(dx, 1, 3), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
		}
		HerobrineMod.LOGGER.info("cut a landing in his world at [{}, {}, {}]",
			site.getX(), site.getY(), site.getZ());
	}

	/** Which way the frame faces, for anything that needs to stand clear of it. */
	public static Direction across() {
		return Direction.EAST;
	}
}
