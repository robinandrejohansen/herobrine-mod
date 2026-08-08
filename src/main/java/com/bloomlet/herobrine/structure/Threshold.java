package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

/**
 * The last one, and it is not a house.
 *
 * House one is a home with the furniture still in it. This is the far end of
 * that line: no bed, no table, no hearth, nothing anybody ate or slept or sat
 * in. Whatever this place was for, it was not for living in — and the player
 * has followed a family from a kitchen to here, which is the whole story told
 * without a word of it being said.
 *
 * Almost nothing on the surface. A stair mouth in the ground and a doorframe
 * with no house behind it, and a player could walk within twenty blocks and
 * never look twice. Everything is underneath.
 *
 * THE CELLS ARE THE POINT, and specifically the fact that they are BUILT. The
 * approach is all cut rock — irregular, wandering, propped, the same hand that
 * dug under the farmhouse. Then it opens into a hall, and inside the hall
 * somebody has laid courses. Right angles, squared walls, iron bars, a corridor
 * with doors down both sides.
 *
 * Architecture appearing in a place that had none is the most alarming thing
 * available underground, and it asks the question the whole mod has been
 * circling: a man digging alone needs no cells. Cells are for keeping
 * something, which means there was something to keep, which means he was not
 * alone down here — and one of the cells has its bars pushed outward.
 *
 * At the end, the seal. It does nothing. It cannot be opened, mined, or used,
 * and that is deliberate: the reward for finding the bottom of this is not a
 * dimension, it is the certainty that there is one and that it is shut. The
 * dimension is never visited (LORE.md); it only leaks.
 */
public final class Threshold {
	private Threshold() {}

	/** How far under the surface the complex sits. */
	private static final int DEPTH = 34;

	// The cell block, in its own coordinates. Cells north and south of a
	// corridor you have to walk the length of.
	private static final int BLOCK_W = 17;
	private static final int BLOCK_D = 13;
	private static final int BLOCK_H = 5;
	private static final int CORRIDOR_Z0 = 5;
	private static final int CORRIDOR_Z1 = 7;

	public static void raise(ServerLevel level, BlockPos site, RandomSource random) {
		int surface = Ground.topOf(level, site.getX(), site.getZ());
		BlockPos mouth = new BlockPos(site.getX(), surface, site.getZ());

		doorway(level, mouth, random);
		BlockPos bottom = stair(level, mouth, random);

		// The approach. Long, dug, and empty — the player needs to have gone a
		// long way down before anything is asked of them.
		BlockPos hall = Digging.bore(level, bottom, new Vec3(0.2, -0.5, 1.0), 26, 1.6, random);
		Digging.hollow(level, hall, 6.5, random);
		Digging.props(level, hall, 8, random);

		// Then the thing nobody dug.
		BlockPos cells = hall.offset(4, -2, -BLOCK_D / 2);
		cellBlock(level, cells, random);

		BlockPos beyond = new BlockPos(
			cells.getX() + BLOCK_W + 1, cells.getY() + 1, cells.getZ() + BLOCK_D / 2);
		BlockPos end = Digging.bore(level, beyond, new Vec3(1.0, -0.25, 0.15), 18, 1.7, random);
		Digging.hollow(level, end, 7.0, random);
		seal(level, end, random);

		HerobrineMod.LOGGER.info("threshold raised, mouth at [{}, {}, {}], seal at [{}, {}, {}]",
			mouth.getX(), mouth.getY(), mouth.getZ(), end.getX(), end.getY(), end.getZ());
	}

	/**
	 * What there is to find above ground: almost nothing.
	 *
	 * A doorframe with no house behind it and a hole beside it. Everything the
	 * homestead does to say somebody lived somewhere is deliberately absent —
	 * no path, no field, no fence, no graves. This is not a place anybody came
	 * back to.
	 */
	private static void doorway(ServerLevel level, BlockPos at, RandomSource random) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int up = 1; up <= 4; up++) {
					BlockPos clear = at.offset(dx, up, dz);
					if (!level.getBlockState(clear).isAir()) {
						level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		// Two jambs and a lintel, standing in the grass with nothing attached.
		for (int side = -1; side <= 1; side += 2) {
			for (int up = 1; up <= 3; up++) {
				level.setBlock(at.offset(side * 2, up, -2), weathered(random), 2);
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			level.setBlock(at.offset(dx, 4, -2), weathered(random), 2);
		}
		sign(level, at.offset(0, 1, -1), "there is a door", "under this");
	}

	/**
	 * Down.
	 *
	 * Cut steps rather than a ladder, because a stair is a commitment — you can
	 * see how far it goes before you start and it keeps going anyway. Walled in
	 * cracked brick that gets rougher as it descends, so the workmanship gives
	 * out on the way down.
	 */
	private static BlockPos stair(ServerLevel level, BlockPos mouth, RandomSource random) {
		BlockPos at = mouth;
		for (int step = 0; step < DEPTH; step++) {
			at = at.offset(0, -1, 1);
			for (int dx = -1; dx <= 1; dx++) {
				for (int up = 0; up <= 3; up++) {
					BlockPos air = at.offset(dx, up, 0);
					if (!level.getBlockState(air).is(Blocks.BEDROCK)) {
						level.setBlock(air, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
					Digging.seal(level, air);
				}
				level.setBlock(at.offset(dx, -1, 0),
					step * 3 > DEPTH * 2 ? Blocks.COBBLESTONE.defaultBlockState()
						: weathered(random), 2);
			}
			if (step % 6 == 0) {
				Digging.lamp(level, at.above());
			}
		}
		return at;
	}

	/**
	 * The cells.
	 *
	 * Built, squared, and laid out — the only right angles in the entire
	 * complex, dropped into the middle of a cave that has none. That contrast
	 * is doing all the work here; the bars are almost incidental.
	 *
	 * Eight of them, four a side, and the player has to walk the corridor
	 * between them to get anywhere. There is no way round.
	 */
	private static void cellBlock(ServerLevel level, BlockPos origin, RandomSource random) {
		// One cell in eight has been opened from the inside. Chosen up front so
		// exactly one exists — the whole point of it is that it is singular.
		int broken = random.nextInt(8);
		int cell = 0;

		for (int x = 0; x < BLOCK_W; x++) {
			for (int z = 0; z < BLOCK_D; z++) {
				boolean wall = x == 0 || x == BLOCK_W - 1 || z == 0 || z == BLOCK_D - 1
					|| x % 4 == 0;
				boolean corridor = z >= CORRIDOR_Z0 && z <= CORRIDOR_Z1;
				boolean front = z == CORRIDOR_Z0 - 1 || z == CORRIDOR_Z1 + 1;

				for (int y = 0; y < BLOCK_H; y++) {
					BlockPos pos = origin.offset(x, y, z);
					BlockState state;
					if (y == 0 || y == BLOCK_H - 1) {
						state = brick(random);
					} else if (front && !wall) {
						state = Blocks.IRON_BARS.defaultBlockState();
					} else if (wall && !(corridor && (x == 0 || x == BLOCK_W - 1))) {
						state = brick(random);
					} else {
						state = Blocks.CAVE_AIR.defaultBlockState();
					}
					level.setBlock(pos, state, 2);
					Digging.seal(level, pos);
				}
			}
		}

		// The two ends stay open, so the corridor is the way through.
		for (int z = CORRIDOR_Z0; z <= CORRIDOR_Z1; z++) {
			for (int y = 1; y < BLOCK_H - 1; y++) {
				level.setBlock(origin.offset(0, y, z), Blocks.CAVE_AIR.defaultBlockState(), 2);
				level.setBlock(origin.offset(BLOCK_W - 1, y, z),
					Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}

		for (int bay = 0; bay < 4; bay++) {
			int x0 = bay * 4 + 1;
			furnish(level, origin, x0, 1, cell++ == broken, random);
			furnish(level, origin, x0, CORRIDOR_Z1 + 2, cell++ == broken, random);
		}

		for (int x = 2; x < BLOCK_W; x += 5) {
			Digging.lamp(level, origin.offset(x, 2, CORRIDOR_Z0));
		}
	}

	/**
	 * What is left in one cell.
	 *
	 * Bones, cobwebs and a chain, and in two of the eight a few words scratched
	 * on the wall. Never a bed and never a bowl: the moment this looks like
	 * somewhere a person was KEPT ALIVE it becomes a prison, and a prison is a
	 * building with a purpose you can name. Whatever these were for should stay
	 * out of reach.
	 *
	 * The opened one has its bars pushed OUT into the corridor, which is the
	 * single most important detail in the complex and takes four blocks to say.
	 */
	private static void furnish(ServerLevel level, BlockPos origin, int x0, int z0,
	                            boolean opened, RandomSource random) {
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos floor = origin.offset(x0 + dx, 1, z0 + dz);
				int roll = random.nextInt(9);
				if (roll == 0) {
					level.setBlock(floor, Blocks.BONE_BLOCK.defaultBlockState()
						.setValue(BlockStateProperties.AXIS, Direction.Axis.X), 2);
				} else if (roll == 1) {
					level.setBlock(floor, Blocks.COBWEB.defaultBlockState(), 2);
				} else if (roll == 2) {
					level.setBlock(origin.offset(x0 + dx, 3, z0 + dz),
						Blocks.IRON_CHAIN.defaultBlockState(), 2);
				}
			}
		}
		if (random.nextInt(4) == 0) {
			sign(level, origin.offset(x0 + 1, 2, z0 + 1),
				"it does not", "sleep either");
		}
		if (!opened) {
			return;
		}
		// Bars bent outward, into the corridor. Not broken in. Broken OUT.
		boolean north = z0 < CORRIDOR_Z0;
		int barsZ = north ? CORRIDOR_Z0 - 1 : CORRIDOR_Z1 + 1;
		for (int dx = 0; dx < 3; dx++) {
			for (int y = 1; y < BLOCK_H - 1; y++) {
				level.setBlock(origin.offset(x0 + dx, y, barsZ),
					Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
		int spill = north ? CORRIDOR_Z0 : CORRIDOR_Z1;
		level.setBlock(origin.offset(x0, 1, spill), Blocks.IRON_BARS.defaultBlockState(), 2);
		level.setBlock(origin.offset(x0 + 2, 1, spill), Blocks.IRON_BARS.defaultBlockState(), 2);
		sign(level, origin.offset(x0 + 1, 2, z0 + 1), "i was in here", "for a long time");
	}

	/**
	 * The door, and it is shut.
	 *
	 * An obsidian frame with something newer bricked into it — the frame was
	 * cut for an opening and then somebody filled the opening in, badly and in
	 * a hurry, from this side. That is the shape of the whole story: it was
	 * opened once, and closing it was the emergency.
	 *
	 * It does nothing. No portal, no particles, no interaction, nothing to
	 * mine. The reward for reaching the bottom is not a dimension — it is the
	 * certainty that there is one, and that somebody sealed it, and that the
	 * seal is cracked.
	 */
	private static void seal(ServerLevel level, BlockPos centre, RandomSource random) {
		BlockPos base = Digging.groundUnder(level, centre);
		if (base == null) {
			base = centre.below();
		}
		BlockPos foot = base.above();

		for (int dx = -3; dx <= 3; dx++) {
			for (int y = 0; y <= 6; y++) {
				boolean edge = dx == -3 || dx == 3 || y == 0 || y == 6;
				BlockPos pos = foot.offset(dx, y, 0);
				if (edge) {
					level.setBlock(pos, random.nextInt(5) == 0
						? Blocks.CRYING_OBSIDIAN.defaultBlockState()
						: Blocks.OBSIDIAN.defaultBlockState(), 2);
					continue;
				}
				// The fill. Newer than the frame, and coming apart.
				level.setBlock(pos, random.nextInt(3) == 0
					? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
					: Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
			}
		}
		for (int dx = -2; dx <= 2; dx += 2) {
			level.setBlock(foot.offset(dx, 5, 1), Blocks.IRON_CHAIN.defaultBlockState(), 2);
			level.setBlock(foot.offset(dx, 4, 1), Blocks.IRON_CHAIN.defaultBlockState(), 2);
		}
		for (int dx = -4; dx <= 4; dx++) {
			if (random.nextInt(3) == 0) {
				BlockPos stain = foot.offset(dx, 0, 1 + random.nextInt(3));
				if (level.getBlockState(stain).isAir()
					&& level.getBlockState(stain.below()).isSolid()) {
					level.setBlock(stain, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
				}
			}
		}
		sign(level, foot.offset(0, 1, 2), "we put it back", "it did not hold");
	}

	private static BlockState brick(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState weathered(RandomSource random) {
		return random.nextInt(3) == 0
			? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
			: Blocks.COBBLESTONE.defaultBlockState();
	}

	private static void sign(ServerLevel level, BlockPos at, String... lines) {
		if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).isSolid()) {
			return;
		}
		level.setBlock(at, Blocks.SPRUCE_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 8), 2);
		if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
			SignText text = sign.getFrontText();
			for (int i = 0; i < lines.length && i < 4; i++) {
				text = text.setMessage(i, Component.literal(lines[i]));
			}
			sign.setText(text, true);
			sign.setWaxed(true);
		}
	}
}
