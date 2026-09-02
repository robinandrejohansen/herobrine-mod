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

	// ---- WHAT THE FIGHT REMEMBERS ------------------------------------------------
	//
	// The count of blows and whether the sky has been taken from him, kept on HIS
	// level rather than on him. He is discarded when everybody leaves and a fresh
	// one is stood over the keep when they come back; the Ender Dragon does not
	// forget how much of her you have already done, and neither does he. Cleared
	// by the death that ends it.

	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Integer> HITS =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.createPersistent(
			com.bloomlet.herobrine.HerobrineMod.id("reckoning_hits"),
			com.mojang.serialization.Codec.INT);
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> BOUND =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.createPersistent(
			com.bloomlet.herobrine.HerobrineMod.id("reckoning_bound"),
			com.mojang.serialization.Codec.BOOL);

	public static int hits(ServerLevel his) {
		return his.getAttachedOrElse(HITS, 0);
	}

	public static boolean bound(ServerLevel his) {
		return Boolean.TRUE.equals(his.getAttached(BOUND));
	}

	public static void record(ServerLevel his, int hits, boolean bound) {
		his.setAttached(HITS, hits);
		his.setAttached(BOUND, bound);
	}

	public static void clear(ServerLevel his) {
		his.removeAttached(HITS);
		his.removeAttached(BOUND);
	}
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
	 * WHAT IS LEFT WHERE HE DIED.
	 *
	 * The ending had a good beat — the rain stopping — and nothing you could
	 * walk back to the next morning. This is the rest of it, and it is
	 * deliberately staged in two parts rather than dumped at once.
	 *
	 * FIRST, for six seconds, a portal. Obsidian and the purple of it, standing
	 * in a ring of fire, and the player has just long enough to look at it and
	 * work out what it means: he was going somewhere, and he was almost there.
	 *
	 * THEN IT COLLAPSES. Not opened, not entered, not usable — broken. That is
	 * the whole sentence the ending needs to say: this was stopped, and it was
	 * only just stopped. A portal the player can walk through is a new
	 * dimension and an entirely different mod; a portal that fails in front of
	 * them is the thing they beat.
	 *
	 * And then the ruin stays. A burnt ring, the shell of a small house, signs
	 * that are still threatening somebody who is no longer in a position to do
	 * anything about it, and the Effigy on a plinth in the middle to be prised
	 * out and taken home.
	 */
	public static void aftermath(ServerLevel level, BlockPos where, ServerPlayer killer) {
		RandomSource random = level.getRandom();
		BlockPos site = new BlockPos(where.getX(),
			Ground.topOf(level, where.getX(), where.getZ()) + 1, where.getZ());

		// THE TOWER CLOSES OVER ITSELF, and that is where the way out is.
		//
		// A portal opening on the spot he died is a fine ending and it wastes the
		// fifteen hours before it: nobody knows the ending exists until it is in
		// front of them. The tower has been standing broken on the skyline since
		// the first day, in two halves that cannot reach each other, and killing
		// him is what brings them together.
		//
		// Which turns the last fight from "the final boss" into "the door".
		//
		// The old behaviour survives as the fallback. If the tower never got sited
		// — no dry ground near his house, a world where his place was never raised
		// — then a portal here is far better than no ending at all.
		if (!com.bloomlet.herobrine.structure.Spire.join(level, random)) {
			com.bloomlet.herobrine.structure.TheWay.open(level, site);
		}
		burning(level, site, random);

		// Experience, and a great deal of it. The one straightforwardly
		// generous thing in the entire mod, on the grounds that thirty
		// exchanges with something that ignores armour has earned it.
		net.minecraft.world.entity.ExperienceOrb.award(level,
			new net.minecraft.world.phys.Vec3(site.getX() + 0.5, site.getY() + 1.0,
				site.getZ() + 0.5), 2600);

		Cadence.in(level.getServer(), 120, () -> {
			steady(level, site);
			memorial(level, site, random, killer);
			warnings(level, site, random);
			HerobrineMod.LOGGER.info("the way held at [{}, {}, {}]",
				site.getX(), site.getY(), site.getZ());
		});
	}

	// The old portal() lived here: four by five of obsidian filled with vanilla
	// NETHER_PORTAL blocks, which would have sent anybody who reached it to the
	// nether. It is gone rather than kept, because a dead copy of the frame in
	// the file next to the live one is how two versions of a thing drift apart.
	// structure/TheWay owns the frame now, on both sides of it.

	/**
	 * AND IT HOLDS. IT USED TO BREAK.
	 *
	 * The old version raised the portal, gave the player six seconds to look at
	 * it, and then collapsed it — most of the frame gone, a few stones left
	 * standing. That was a good ending and a shut one. The sentence it spoke was
	 * "he was going somewhere, and he was almost through", and the only thing
	 * anybody could do with it was read it and walk home.
	 *
	 * It stays open now, and the reason is worse than the reason it broke. HE
	 * DID NOT FINISH IT — the fight did. The last thing standing between him and
	 * wherever he was going was the player, and the frame closes over his body
	 * about four seconds after he stops moving.
	 *
	 * The six seconds are kept exactly as they were, because the beat is still
	 * the beat: they get long enough to work out what they are looking at before
	 * anything happens to it. All that changed is what happens at the end of
	 * them.
	 */
	private static void steady(ServerLevel level, BlockPos site) {
		level.playSound(null, site, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
			SoundSource.HOSTILE, 4.0F, 0.5F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
			site.getX() + 0.5, site.getY() + 2.0, site.getZ() + 0.5, 200, 2.0, 2.0, 2.0, 0.6);
	}

	/** A burnt ring. It is a storm; most of it will be out before long. */
	private static void burning(ServerLevel level, BlockPos site, RandomSource random) {
		for (int i = 0; i < 22; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 2.0 + random.nextDouble() * 7.0;
			int x = site.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = site.getZ() + (int)Math.round(Math.sin(angle) * range);
			BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
			if (!level.getBlockState(ground.above()).isAir()) {
				continue;
			}
			level.setBlock(ground, random.nextInt(3) == 0
				? Blocks.BASALT.defaultBlockState()
				: Blocks.BLACKSTONE.defaultBlockState(), 2);
			if (random.nextInt(2) == 0) {
				level.setBlock(ground.above(), Blocks.FIRE.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * A small shell of a house, and the Effigy in the middle of it.
	 *
	 * Roofless and half-built on purpose. A finished building would read as
	 * somebody's memorial to the fight; an unfinished one reads as the thing he
	 * was in the middle of when it stopped, which is a far worse note to leave
	 * a player standing in.
	 */
	private static void memorial(ServerLevel level, BlockPos site, RandomSource random,
	                             ServerPlayer killer) {
		BlockPos at = site.offset(6, 0, 4);
		BlockPos base = new BlockPos(at.getX(),
			Ground.topOf(level, at.getX(), at.getZ()) + 1, at.getZ());

		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
				level.setBlock(base.offset(dx, -1, dz),
					Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
				if (!wall) {
					continue;
				}
				// Ragged. Each course of each column stops at its own height.
				int up = 1 + random.nextInt(3);
				for (int dy = 0; dy < up; dy++) {
					level.setBlock(base.offset(dx, dy, dz), random.nextInt(4) == 0
						? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
		// The doorway, such as it is.
		for (int dy = 0; dy <= 2; dy++) {
			level.setBlock(base.offset(0, dy, 3), Blocks.AIR.defaultBlockState(), 2);
		}

		level.setBlock(base, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
		level.setBlock(base.above(), com.bloomlet.herobrine.block.ModBlocks.EFFIGY
			.defaultBlockState(), 2);
		for (int dx : new int[] { -1, 1 }) {
			level.setBlock(base.offset(dx, 0, 0), Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.LIT, true), 2);
		}
	}

	/**
	 * Signs, still threatening somebody who cannot do anything about it.
	 *
	 * That gap is the whole reason they are here rather than a "you win". They
	 * are in the present tense and they are wrong, and the player standing in
	 * the wreckage reading them gets to notice that for themselves.
	 */
	private static void warnings(ServerLevel level, BlockPos site, RandomSource random) {
		String[][] said = {
			{ "YOU ARE", "GOING TO", "DIE" },
			{ "I WAS", "ALMOST", "THROUGH" },
			{ "THIS IS NOT", "THE END", "OF IT" },
			{ "COUNT", "THE DAYS" },
			{ "I KNOW", "WHERE", "YOU SLEEP" },
		};
		for (int i = 0; i < said.length; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 3.0 + random.nextDouble() * 6.0;
			int x = site.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = site.getZ() + (int)Math.round(Math.sin(angle) * range);
			BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			if (!level.getBlockState(ground).isAir()
				|| !level.getBlockState(ground.below()).isSolid()) {
				continue;
			}
			level.setBlock(ground, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 2);
			if (level.getBlockEntity(ground) instanceof SignBlockEntity sign) {
				String[] lines = said[i];
				sign.setText(new SignText()
					.setMessage(0, Component.literal(lines[0]))
					.setMessage(1, Component.literal(lines.length > 1 ? lines[1] : ""))
					.setMessage(2, Component.literal(lines.length > 2 ? lines[2] : ""))
					.setMessage(3, Component.literal("")), true);
			}
		}
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
				Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BROWN)
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
