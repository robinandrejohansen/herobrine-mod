package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The tenth blow.
 *
 * A little church, and a grave in front of it with the player's own name cut
 * into the stone. It goes up in the time it takes to swing again.
 *
 * THIS IS A WARNING THAT ARRIVES TOO LATE TO TAKE, and that is the whole
 * reason it is at ten rather than at one. A warning before the fight is a
 * tutorial and the player would read it as a difficulty setting. A warning a
 * third of the way in, when they have already committed, is not advice — it is
 * a comment on what they have decided, and the only thing they can do with it
 * is keep going.
 *
 * It is also the one moment in the whole mod where he says something plainly.
 * Everything else is deniable, sideways, four words on a wall that could have
 * been anyone. This is a building with their grave outside it, and there is no
 * reading of that which is not addressed to them.
 *
 * Nothing here is destroyed to make room. The chapel is small and it is placed
 * where there is space for it, because a last warning that flattens somebody's
 * house is not frightening, it is a bug report.
 */
public final class Reckoning {
	private Reckoning() {}

	/** Far enough to be a thing you walk to, near enough that you cannot miss it. */
	private static final int NEAR = 11;
	private static final int FAR = 20;

	/**
	 * Put it up.
	 *
	 * @param him so the chapel faces the fight rather than a compass point
	 */
	public static void theWarning(ServerLevel level, ServerPlayer player, Entity him) {
		BlockPos site = clearing(level, player);
		if (site == null) {
			HerobrineMod.LOGGER.info("the warning: nowhere to put it");
			return;
		}
		RandomSource random = level.getRandom();
		Direction facing = Direction.getApproximateNearest(
			player.getX() - site.getX(), 0.0, player.getZ() - site.getZ());

		chapel(level, site, facing);
		grave(level, site.relative(facing, 4), facing, player);
		scripture(level, site, facing, random);

		// Heard from wherever they are standing, because they are not looking
		// this way — they are looking at him.
		level.playSound(null, site, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 4.0F, 0.55F);
		player.sendSystemMessage(Component.literal("§8You shouldn't have done that."));
		HerobrineMod.LOGGER.info("the warning went up at [{}, {}, {}] for {}",
			site.getX(), site.getY(), site.getZ(), player.getName().getString());
	}

	/**
	 * Somewhere with room for it, and never on top of anything.
	 *
	 * Refuses rather than clears. A player who turns round to find their
	 * workshop replaced by a chapel has been handed a grievance instead of a
	 * fright, and the moment is spent arguing with the mod rather than with
	 * him.
	 */
	private static BlockPos clearing(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 60; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = (int)Math.round(player.getX() + Math.cos(angle) * range);
			int z = (int)Math.round(player.getZ() + Math.sin(angle) * range);
			int y = Ground.topOf(level, x, z);
			BlockPos base = new BlockPos(x, y, z);

			if (roomFor(level, base)) {
				return base;
			}
		}
		return null;
	}

	private static boolean roomFor(ServerLevel level, BlockPos base) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dy = 0; dy <= 6; dy++) {
					BlockPos at = base.offset(dx, dy, dz);
					if (dy == 0) {
						BlockState under = level.getBlockState(at.below());
						if (!under.isSolid() || !level.getFluidState(at).isEmpty()) {
							return false;
						}
					}
					if (!level.getBlockState(at).isAir()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Small, plain and old-looking, in three or four block types.
	 *
	 * Deliberately not impressive. A cathedral would read as a set piece and
	 * invite the player to admire it; a parish chapel the size of a shed reads
	 * as somewhere a small number of frightened people once went, which is a
	 * much worse thing to find with your own name outside it.
	 */
	private static void chapel(ServerLevel level, BlockPos base, Direction facing) {
		Direction across = facing.getClockWise();
		RandomSource random = level.getRandom();

		for (int out = -4; out <= 1; out++) {
			for (int side = -2; side <= 2; side++) {
				for (int dy = 0; dy <= 4; dy++) {
					BlockPos at = base.relative(facing, out).relative(across, side).above(dy);
					boolean wall = side == -2 || side == 2 || out == -4 || out == 1;
					boolean roof = dy == 4;
					if (roof) {
						level.setBlock(at, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), 2);
					} else if (wall && dy > 0) {
						level.setBlock(at, stone(random), 2);
					} else if (dy == 0) {
						level.setBlock(at, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
					} else {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}

		// The doorway, facing them.
		for (int dy = 1; dy <= 2; dy++) {
			level.setBlock(base.relative(facing, 1).above(dy), Blocks.AIR.defaultBlockState(), 2);
		}
		// Two narrow windows, because a chapel with none is a bunker.
		for (int side : new int[] { -2, 2 }) {
			level.setBlock(base.relative(facing, -1).relative(across, side).above(2),
				Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BROWN)
					.defaultBlockState(), 2);
		}
		// The altar, at the far end, with a candle nobody lit.
		BlockPos altar = base.relative(facing, -3).above(1);
		level.setBlock(altar, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.above(), Blocks.CANDLE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, false), 2);

		// Pews, four rows, facing the altar.
		for (int out = -2; out <= 0; out++) {
			for (int side : new int[] { -1, 1 }) {
				level.setBlock(base.relative(facing, out).relative(across, side).above(1),
					Blocks.DARK_OAK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 2);
			}
		}
	}

	/**
	 * The grave, outside the door, with their name on it.
	 *
	 * No date and no verdict. A stone that says only who it is for is a
	 * question; anything more would be the mod answering it.
	 */
	private static void grave(ServerLevel level, BlockPos at, Direction facing,
	                         ServerPlayer player) {
		BlockPos ground = new BlockPos(at.getX(), Ground.topOf(level, at.getX(), at.getZ()),
			at.getZ());
		Direction across = facing.getClockWise();

		for (int side = -1; side <= 1; side++) {
			level.setBlock(ground.relative(across, side), Blocks.PODZOL.defaultBlockState(), 2);
		}
		BlockPos head = ground.relative(facing, -1).above();
		level.setBlock(head, Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);

		BlockPos stone = head.above();
		level.setBlock(stone, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16,
				rotationFor(facing)), 2);
		if (level.getBlockEntity(stone) instanceof SignBlockEntity sign) {
			sign.setText(new SignText()
				.setMessage(0, Component.literal("HERE LIES"))
				.setMessage(1, Component.literal(player.getName().getString()))
				.setMessage(2, Component.literal(""))
				.setMessage(3, Component.literal("")), true);
		}
	}

	/** Signs around it, and every one of them says the same thing. */
	private static void scripture(ServerLevel level, BlockPos base, Direction facing,
	                              RandomSource random) {
		String[] lines = { "PRAY", "PRAY", "PRAY", "IT IS TOO LATE", "YOU KNEW",
			"HE WAS PATIENT", "NOT ANY MORE" };
		Direction across = facing.getClockWise();

		for (int i = 0; i < 7; i++) {
			BlockPos at = base.relative(facing, 2 + random.nextInt(5))
				.relative(across, random.nextInt(9) - 4);
			BlockPos ground = new BlockPos(at.getX(),
				Ground.topOf(level, at.getX(), at.getZ()), at.getZ());
			if (!level.getBlockState(ground).isAir()
				|| !level.getBlockState(ground.below()).isSolid()) {
				continue;
			}
			level.setBlock(ground, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 2);
			if (level.getBlockEntity(ground) instanceof SignBlockEntity sign) {
				String word = lines[i % lines.length];
				sign.setText(new SignText().setMessage(1, Component.literal(word)), true);
			}
		}
	}

	private static int rotationFor(Direction facing) {
		return switch (facing) {
			case SOUTH -> 0;
			case WEST -> 4;
			case NORTH -> 8;
			case EAST -> 12;
			default -> 0;
		};
	}

	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
