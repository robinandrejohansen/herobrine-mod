package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Dwellings;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.structure.Loot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * NEW MARKS. The graves, the torches and the signs had been seen; these had
 * not. Two things he leaves, both found by walking round a corner, neither
 * announced, both different every time:
 *
 *   THE CAMP    a small perfect room cut into a hillside, a bed that has been
 *               slept in, a chest, torches on the walls and a furnace still
 *               burning. Somebody stayed the night here. Somebody is not here.
 *   THE CROSS   a hole in the shape of a cross, one block wide, cut straight
 *               down twelve to thirty blocks with a red torch at the bottom.
 *               Sometimes one arm is longer.
 *
 * NEVER NEAR A HOUSE. Every mark checks Dwellings.nearAPlace and DwellTracker
 * around itself first, so the places on the road and their yards stay as they
 * were built; what he leaves, he leaves in the open country between them.
 */
public final class Marks {
	private Marks() {}

	private static final int NEAR = 26;
	private static final int FAR = 74;
	/** Clear of the places by this, on top of each place's own spread. */
	private static final double CLEAR_OF_PLACES = 40.0;
	private static final int CLEAR_OF_BUILT = 14;

	/** True when nothing built stands near here: no place within reach, no tracked blocks in a ring around it. */
	static boolean clearOf(ServerLevel level, BlockPos at) {
		if (Dwellings.nearAPlace(level, at, CLEAR_OF_PLACES)) {
			return false;
		}
		for (int dx = -CLEAR_OF_BUILT; dx <= CLEAR_OF_BUILT; dx += CLEAR_OF_BUILT) {
			for (int dz = -CLEAR_OF_BUILT; dz <= CLEAR_OF_BUILT; dz += CLEAR_OF_BUILT) {
				int x = at.getX() + dx;
				int z = at.getZ() + dz;
				BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
				if (DwellTracker.isBuilt(level, ground) || DwellTracker.isBuilt(level, ground.above())
					|| DwellTracker.isBuilt(level, ground.below(3))) {
					return false;
				}
			}
		}
		return true;
	}

	private static @org.jspecify.annotations.Nullable BlockPos openGround(ServerLevel level, ServerPlayer player, RandomSource random) {
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = (int) Math.round(player.getX() + Math.cos(angle) * range);
			int z = (int) Math.round(player.getZ() + Math.sin(angle) * range);
			if (!level.hasChunk(x >> 4, z >> 4) || !Ground.dry(level, x, z)) {
				continue;
			}
			int y = Ground.topOf(level, x, z);
			BlockPos on = new BlockPos(x, y, z);
			if (y <= level.getMinY() + 8 || !level.getBlockState(on).isSolid() || !clearOf(level, on)) {
				continue;
			}
			return on;
		}
		return null;
	}

	// ---- THE CAMP -----------------------------------------------------------------

	public static boolean camp(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 24; attempt++) {
			BlockPos on = openGround(level, player, random);
			if (on == null) {
				return false;
			}
			Direction in = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			Direction across = in.getClockWise();
			int half = 2 + random.nextInt(2);         // room 5 or 7 wide
			int deep = 4 + random.nextInt(3);         // 4 to 6 long
			int drop = 2 + random.nextInt(3);         // the passage goes down 2 to 4
			BlockPos mouth = on.above();
			BlockPos front = mouth.relative(in, 4).below(drop);
			if (!buried(level, front, in, across, half, deep)) {
				continue;
			}
			// THE PASSAGE: one wide, two high, four steps in and down.
			for (int step = 0; step <= 4; step++) {
				BlockPos at = mouth.relative(in, step).below(drop * step / 4);
				for (int up = 0; up <= 1; up++) {
					level.setBlock(at.above(up), Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
				if (!level.getBlockState(at.below()).isSolid()) {
					level.setBlock(at.below(), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
			// THE ROOM: perfect. Stone brick floor, the rock left as it was, three high.
			for (int a = -half; a <= half; a++) {
				for (int b = 0; b < deep; b++) {
					BlockPos floor = front.relative(across, a).relative(in, b).below();
					level.setBlock(floor, random.nextInt(5) == 0
						? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
					for (int up = 0; up < 3; up++) {
						level.setBlock(floor.above(1 + up), Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
					BlockPos roof = floor.above(4);
					if (!level.getBlockState(roof).isSolid()) {
						level.setBlock(roof, Blocks.STONE.defaultBlockState(), 2);
					}
				}
			}
			// SOMEBODY STAYED THE NIGHT. A bed along the back wall, a chest by it, a
			// furnace on the right still going, a table, two torches, a rug.
			BlockPos back = front.relative(in, deep - 1);
			BlockState bed = Blocks.BED.pick(DyeColor.values()[random.nextInt(DyeColor.values().length)])
				.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, in);
			level.setBlock(back.relative(across, -1), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
			level.setBlock(back.relative(across, -1).relative(in, -1), bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);
			BlockPos chestAt = back.relative(across, 1);
			level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, in.getOpposite()), 2);
			if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
				if (random.nextInt(4) == 0) {
					chest.setItem(0, Loot.tome(level.registryAccess(), random, 1));
				}
				chest.setItem(1, new ItemStack(Items.TORCH, 4 + random.nextInt(6)));
				Loot.scatter(chest, random, Loot.Tier.LARDER);
			}
			BlockPos stove = front.relative(across, half).relative(in, 1);
			level.setBlock(stove, Blocks.FURNACE.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, across.getOpposite())
				.setValue(BlockStateProperties.LIT, true), 2);
			if (level.getBlockEntity(stove) instanceof AbstractFurnaceBlockEntity furnace) {
				furnace.setItem(0, new ItemStack(Items.BEEF, 2 + random.nextInt(3)));
				furnace.setItem(1, new ItemStack(Items.COAL, 4 + random.nextInt(6)));
				furnace.setItem(2, new ItemStack(Items.COOKED_BEEF, 1 + random.nextInt(3)));
			}
			level.setBlock(front.relative(across, half).relative(in, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
			if (random.nextBoolean()) {
				level.setBlock(front.relative(across, -half).relative(in, 1), Blocks.BARREL.defaultBlockState(), 2);
			} else {
				level.setBlock(front.relative(across, -half).relative(in, 1), Blocks.CAULDRON.defaultBlockState(), 2);
			}
			level.setBlock(front.relative(across, -half).relative(in, 2).above(), Blocks.WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, across), 2);
			level.setBlock(front.relative(across, half).relative(in, deep - 2).above(), Blocks.WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, across.getOpposite()), 2);
			level.setBlock(front.relative(in, deep - 2), Blocks.CARPET.pick(DyeColor.RED).defaultBlockState(), 2);
			if (random.nextInt(3) == 0) {
				// two slept here
				BlockState second = Blocks.BED.pick(DyeColor.WHITE).defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, in);
				BlockPos other = front.relative(across, -half).relative(in, deep - 1);
				level.setBlock(other, second.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
				level.setBlock(other.relative(in, -1), second.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);
			}
			HerobrineMod.LOGGER.info("a camp was cut into the hill at [{}, {}, {}], mouth at [{}, {}, {}]",
				front.getX(), front.getY(), front.getZ(), mouth.getX(), mouth.getY(), mouth.getZ());
			ManifestationDirector.noteLocation(mouth);
			return true;
		}
		return false;
	}

	/** The whole room box, one block bigger all round, has to be inside solid ground. */
	private static boolean buried(ServerLevel level, BlockPos front, Direction in, Direction across, int half, int deep) {
		for (int a = -half - 1; a <= half + 1; a += half + 1) {
			for (int b = -1; b <= deep; b += Math.max(1, deep / 2)) {
				for (int up = -2; up <= 4; up += 3) {
					BlockPos at = front.relative(across, a).relative(in, b).above(up);
					BlockState state = level.getBlockState(at);
					if (!state.isSolid() || !state.getFluidState().isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// ---- THE CROSS ----------------------------------------------------------------

	public static boolean cross(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		BlockPos on = openGround(level, player, random);
		if (on == null) {
			return false;
		}
		int arm = 2 + random.nextInt(3);
		int longArm = random.nextInt(3) == 0 ? arm + 2 + random.nextInt(2) : arm;
		int depth = 12 + random.nextInt(19);
		java.util.List<BlockPos> columns = new java.util.ArrayList<>();
		columns.add(on);
		for (int i = 1; i <= arm; i++) {
			columns.add(on.north(i));
			columns.add(on.east(i));
			columns.add(on.west(i));
		}
		for (int i = 1; i <= longArm; i++) {
			columns.add(on.south(i));
		}
		int cut = 0;
		int bottom = on.getY();
		for (BlockPos column : columns) {
			int top = Ground.topOf(level, column.getX(), column.getZ());
			if (Math.abs(top - on.getY()) > 2) {
				return false;      // not flat enough to read as a shape from above
			}
			for (int y = top; y > top - depth && y > level.getMinY() + 6; y--) {
				BlockPos at = new BlockPos(column.getX(), y, column.getZ());
				BlockState state = level.getBlockState(at);
				if (!state.getFluidState().isEmpty()) {
					break;      // never into water or lava
				}
				level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
				cut++;
				if (column.equals(on)) {
					bottom = y;
				}
			}
			BlockPos over = new BlockPos(column.getX(), top + 1, column.getZ());
			if (!level.getBlockState(over).isAir()) {
				level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);      // grass, flowers: the edge is clean
			}
		}
		if (cut < depth * 3) {
			return false;
		}
		BlockPos floor = new BlockPos(on.getX(), bottom - 1, on.getZ());
		if (level.getBlockState(floor).isSolid()) {
			level.setBlock(floor.above(), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
		}
		HerobrineMod.LOGGER.info("a cross was cut into the ground at [{}, {}, {}], {} deep, arms {}/{}",
			on.getX(), on.getY(), on.getZ(), on.getY() - bottom, arm, longArm);
		ManifestationDirector.noteLocation(on);
		return true;
	}
}
