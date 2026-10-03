package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * SMALL THINGS HE DOES, NEAR YOU.
 *
 * Each of these is one random event the director can pick, and each is meant to
 * have an ordinary explanation right up until it happens to you twice: a door
 * swings open on its own, a trapdoor falls open, a chest's lid lifts and drops
 * back with nobody at it, a stand of trees loses every leaf at once, a fire
 * starts in a field. Simple on purpose — find something, do one thing to it,
 * make the sound it would make — and each one is a single bounded block scan
 * on a loaded chunk, run once when the event fires and never on a tick.
 *
 * The weather answers some of them (Storm.omen), which the director decides.
 */
public final class Omens {

	private Omens() {}

	// ---- A DOOR, A TRAPDOOR, A CHEST ---------------------------------------

	private static final int OPENS_WITHIN = 14;
	private static final int OPENS_UP_DOWN = 4;

	/** Something near the player that opens, opens. */
	public static boolean opens(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		BlockPos at = player.blockPosition();
		List<BlockPos> found = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -OPENS_WITHIN; dx <= OPENS_WITHIN; dx++) {
			for (int dz = -OPENS_WITHIN; dz <= OPENS_WITHIN; dz++) {
				int x = at.getX() + dx;
				int z = at.getZ() + dz;
				if (!level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				for (int dy = -OPENS_UP_DOWN; dy <= OPENS_UP_DOWN; dy++) {
					pos.set(x, at.getY() + dy, z);
					BlockState state = level.getBlockState(pos);
					if (state.getBlock() instanceof DoorBlock door && !door.isOpen(state)
						&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
						&& !state.is(Blocks.IRON_DOOR)) {
						found.add(pos.immutable());
					} else if (state.getBlock() instanceof TrapDoorBlock && !state.getValue(BlockStateProperties.OPEN)
						&& !state.is(Blocks.IRON_TRAPDOOR)) {
						found.add(pos.immutable());
					} else if (state.getBlock() instanceof ChestBlock
						|| (state.getBlock() instanceof BarrelBlock && !state.getValue(BarrelBlock.OPEN))) {
						found.add(pos.immutable());
					}
				}
			}
		}
		if (found.isEmpty()) {
			ManifestationDirector.refused("nothing that opens within " + OPENS_WITHIN + " blocks");
			return false;
		}
		BlockPos one = found.get(random.nextInt(found.size()));
		BlockState state = level.getBlockState(one);
		if (state.getBlock() instanceof DoorBlock door) {
			door.setOpen(null, level, state, one, true);      // plays its own sound
		} else if (state.getBlock() instanceof TrapDoorBlock) {
			level.setBlock(one, state.setValue(BlockStateProperties.OPEN, true), 3);
			level.playSound(null, one, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
		} else if (state.getBlock() instanceof ChestBlock) {
			lid(level, one, state, random);
		} else if (state.getBlock() instanceof BarrelBlock) {
			level.setBlock(one, state.setValue(BarrelBlock.OPEN, true), 3);
			level.playSound(null, one, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
			Cadence.in(level.getServer(), 40 + random.nextInt(80), () -> {
				BlockState now = level.getBlockState(one);
				if (now.getBlock() instanceof BarrelBlock && now.getValue(BarrelBlock.OPEN)) {
					level.setBlock(one, now.setValue(BarrelBlock.OPEN, false), 3);
					level.playSound(null, one, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
				}
			});
		}
		ManifestationDirector.noteLocation(one);
		HerobrineMod.LOGGER.info("a {} opened by itself at [{}, {}, {}]",
			state.getBlock().getName().getString().toLowerCase(java.util.Locale.ROOT),
			one.getX(), one.getY(), one.getZ());
		return true;
	}

	/** The lid comes up — the block event every chest uses for a viewer — and goes down again. */
	private static void lid(ServerLevel level, BlockPos chest, BlockState state, RandomSource random) {
		level.blockEvent(chest, state.getBlock(), 1, 1);
		level.playSound(null, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 0.8F);
		Cadence.in(level.getServer(), 40 + random.nextInt(80), () -> {
			BlockState now = level.getBlockState(chest);
			if (now.getBlock() instanceof ChestBlock) {
				level.blockEvent(chest, now.getBlock(), 1, 0);
				level.playSound(null, chest, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.9F, 0.8F);
			}
		});
	}

	// ---- THE LEAVES ---------------------------------------------------------

	private static final int BARE_RADIUS = 6;
	private static final int BARE_HIGH = 14;
	private static final int BARE_ENOUGH = 24;

	/** A stand of trees within sight loses every leaf at once. */
	public static boolean bare(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int tries = 0; tries < 8; tries++) {
			BlockPos spot = out(player, 16.0, 36.0, random);
			if (!level.hasChunk(spot.getX() >> 4, spot.getZ() >> 4)) {
				continue;
			}
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
			List<BlockPos> leaves = new ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(
					spot.offset(-BARE_RADIUS, 0, -BARE_RADIUS).atY(ground),
					spot.offset(BARE_RADIUS, 0, BARE_RADIUS).atY(ground + BARE_HIGH))) {
				if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
					leaves.add(pos.immutable());
				}
			}
			if (leaves.size() < BARE_ENOUGH) {
				continue;
			}
			for (BlockPos leaf : leaves) {
				level.setBlock(leaf, Blocks.AIR.defaultBlockState(), 2);
				if (random.nextInt(6) == 0) {
					level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, leaf.getX() + 0.5, leaf.getY() + 0.5,
						leaf.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0.0);
				}
			}
			level.playSound(null, spot.atY(ground + 4), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 2.5F, 0.5F);
			ManifestationDirector.noteLocation(spot.atY(ground));
			HerobrineMod.LOGGER.info("{} leaves gone at once at [{}, {}]", leaves.size(), spot.getX(), spot.getZ());
			return true;
		}
		ManifestationDirector.refused("no stand of trees within sight");
		return false;
	}

	// ---- THE FIRE -----------------------------------------------------------

	/** A fire starts in the open, somewhere the player can see it — never against anything somebody built. */
	public static boolean fire(ServerLevel level, ServerPlayer player) {
		if (!Config.get().huntFire || !level.getGameRules().get(GameRules.MOB_GRIEFING) || level.isRaining()) {
			ManifestationDirector.refused("no fire: griefing off, fire off, or raining");
			return false;
		}
		RandomSource random = level.getRandom();
		for (int tries = 0; tries < 10; tries++) {
			BlockPos spot = out(player, 18.0, 40.0, random);
			if (!level.hasChunk(spot.getX() >> 4, spot.getZ() >> 4)) {
				continue;
			}
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spot.getX(), spot.getZ());
			BlockPos ground = spot.atY(y - 1);
			BlockState under = level.getBlockState(ground);
			if (!(under.is(Blocks.GRASS_BLOCK) || under.is(Blocks.PODZOL) || under.is(Blocks.MOSS_BLOCK)
				|| under.is(BlockTags.LEAVES))) {
				continue;
			}
			if (built(level, ground)) {
				continue;
			}
			int lit = 0;
			for (int i = 0; i < 4 + random.nextInt(4); i++) {
				BlockPos at = ground.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
				BlockPos flame = at.atY(top);
				if (level.getBlockState(flame).isAir()) {
					level.setBlock(flame, net.minecraft.world.level.block.BaseFireBlock.getState(level, flame), 3);
					lit++;
				}
			}
			if (lit == 0) {
				continue;
			}
			ManifestationDirector.noteLocation(ground);
			HerobrineMod.LOGGER.info("a fire started by itself at [{}, {}, {}], {} flames",
				ground.getX(), ground.getY(), ground.getZ(), lit);
			return true;
		}
		ManifestationDirector.refused("nowhere open to burn");
		return false;
	}

	/** Anything a person makes, within six blocks: planks, glass, wool, beds, doors, chests, crafted stone. */
	private static boolean built(ServerLevel level, BlockPos around) {
		for (BlockPos pos : BlockPos.betweenClosed(around.offset(-6, -2, -6), around.offset(6, 4, 6))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOOL) || state.is(BlockTags.BEDS)
				|| state.is(BlockTags.DOORS) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
				|| state.is(Blocks.CHEST) || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE)
				|| state.is(Blocks.STONE_BRICKS) || state.is(Blocks.COBBLESTONE)) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos out(ServerPlayer player, double min, double max, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double r = min + random.nextDouble() * (max - min);
		return BlockPos.containing(player.getX() + Math.cos(angle) * r, player.getY(), player.getZ() + Math.sin(angle) * r);
	}

}
