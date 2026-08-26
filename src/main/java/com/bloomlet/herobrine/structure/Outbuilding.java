package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * THE OTHER BUILDING ON HIS LAND, AND IT IS NOT A SECOND HOUSE.
 *
 * A duplicate homestead was a bug — the flag that should have stopped it was
 * never written — and the right fix for finding two of something is usually to
 * delete one. It is the wrong fix here, because a property with two buildings on
 * it is a property somebody has been living on, and one building alone is a
 * diorama.
 *
 * So the second one is deliberately a DIFFERENT KIND OF THING. The homestead is
 * spruce, warm, and has a bed in it: somewhere a person sleeps. This is stone,
 * windowless, and the only thing in it is a way down. Nobody lives here. It is
 * the top of a hole, with a roof on so the rain does not get in.
 *
 * WHICH IS THE WHOLE STORY OF THE PLACE IN TWO BUILDINGS. You find a house and
 * think you understand it. Then you find the shed forty blocks away, and the shed
 * is only a lid.
 */
public final class Outbuilding {
	private Outbuilding() {}

	/** Small. It is a doorway with walls, not a room. */
	private static final int HALF = 2;

	/**
	 * @return the floor of the cellar it opens onto, for a passage to start from
	 */
	public static BlockPos build(ServerLevel level, BlockPos at, RandomSource random) {
		int floor = Ground.topOf(level, at.getX(), at.getZ());
		BlockPos base = new BlockPos(at.getX(), floor, at.getZ());

		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				boolean wall = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
				put(level, base.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState());
				for (int up = 0; up < 3; up++) {
					BlockPos pos = base.offset(dx, up, dz);
					if (wall) {
						put(level, pos, stone(random));
					} else if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				// A flat roof of slabs. No overhang, no chimney, nothing that
				// suggests anybody meant to spend time under it.
				put(level, base.offset(dx, 3, dz), Blocks.COBBLESTONE_SLAB.defaultBlockState());
			}
		}

		// One doorway, and it has no door. Whatever uses this does not need one.
		Direction facing = Direction.from2DDataValue(random.nextInt(4));
		for (int up = 0; up < 2; up++) {
			BlockPos hole = base.relative(facing, HALF).above(up);
			level.setBlock(hole, Blocks.AIR.defaultBlockState(), 2);
		}

		// THE ONLY THING IN IT, AND IT IS A STAIR NOW.
		//
		// It was a ladder, and Descent has had a spiral in it the whole time —
		// SecondHouse and TheDig both use it. The ladder was the wrong pick twice
		// over: ground pathfinding generates no vertical nodes, so a rung is not a
		// route, and this is the ONLY way into forty metres of passage. He owned
		// the best thing on his land and could not physically get to it.
		//
		// It is the better building as well. A ladder in a horror corridor is a
		// chore; a stair turning down into the dark is the corridor working.
		BlockPos mouth = base;
		BlockPos bottom = Descent.stair(level, base.offset(-1, 0, -1),
			9 + random.nextInt(5), Blocks.COBBLESTONE.defaultBlockState(), random);
		if (bottom == null) {
			bottom = mouth.below(9);
		}

		// One barrel at the bottom, so arriving is worth something before the
		// passage even starts.
		BlockPos stash = bottom.relative(facing.getOpposite());
		if (level.getBlockState(stash).isAir()
			&& level.getBlockState(stash.below()).isSolid()) {
			level.setBlock(stash, Blocks.BARREL.defaultBlockState(), 2);
			if (level.getBlockEntity(stash) instanceof BaseContainerBlockEntity barrel) {
				Loot.store(barrel, random, Loot.Tier.HOMESTEAD);
			}
		}
		HerobrineMod.LOGGER.info("an outbuilding at [{}, {}, {}], and it goes down to {}",
			base.getX(), base.getY(), base.getZ(), bottom.getY());
		return bottom;
	}

	/** Old, and repaired at least once by somebody who did not care how it looked. */
	private static BlockState stone(RandomSource random) {
		return switch (random.nextInt(6)) {
			case 0, 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			case 2 -> Blocks.COBBLESTONE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
			case 3 -> Blocks.STONE_BRICKS.defaultBlockState();
			default -> Blocks.COBBLESTONE.defaultBlockState();
		};
	}

	private static void put(ServerLevel level, BlockPos at, BlockState what) {
		if (!com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, at)) {
			level.setBlock(at, what, 2);
		}
	}
}
