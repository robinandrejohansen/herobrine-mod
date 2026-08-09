package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * What the tower was FOR, and where the tunnel goes.
 *
 * The tower is a lookout. The warren under it is surveyed — paved, lit, marked
 * at every junction, dug by somebody who still had a plan. This is the room at
 * the far end of that plan, and the thing leaving it.
 *
 * A player who has climbed twenty-six blocks of open stair for a chair and a
 * view, then gone down a well into a buried copy of the first house, then
 * followed a lit trunk through a warren, arrives here and finds a working. Not
 * a shrine, not a cell, not a body — a room with a table, lamps on posts, a
 * chest of picks, and a two-by-two tunnel leaving the far wall going dead
 * straight into the dark.
 *
 * THE TUNNEL IS THE SECRET, and the room only exists to make it legible. Two
 * hundred and forty blocks, arrow-straight, torch-lit, on a level grade, with
 * rails down it. That is not a mine. Nobody prospects in a straight line — you
 * follow the ore, and the ore is never straight. This is somebody who knew
 * exactly where he was going and how far it was, and was prepared to dig for
 * months to get there.
 *
 * AND IT LOOKS BUILT BY A PLAYER, which is the whole trick and is why it is
 * cobblestone and torches and rails rather than anything from a structure file.
 * Every one of those is a block a person places by hand, in that order, because
 * that is what you do when you are digging a long way: cobble the floor so it
 * is level, torch it every ten so nothing spawns, lay rail because two hundred
 * blocks on foot is a waste of an afternoon. It reads as somebody's project
 * because it is exactly what somebody's project looks like.
 *
 * It stops unfinished. He did not get there, and where he stopped is the last
 * thing in the room's long argument: the tools are on the floor.
 */
public final class TheSurvey {
	private TheSurvey() {}

	private static final int RUN = 240;
	private static final int TORCH_EVERY = 10;

	public static void build(ServerLevel level, BlockPos at, RandomSource random) {
		BlockPos floor = Digging.groundUnder(level, at);
		BlockPos middle = floor == null ? at : floor.above();

		room(level, middle, random);
		Direction heading = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos end = tunnel(level, middle, heading, random);
		abandoned(level, end, heading, random);

		HerobrineMod.LOGGER.info("the survey at [{}, {}, {}], tunnel {} for {} blocks",
			middle.getX(), middle.getY(), middle.getZ(), heading.getName(), RUN);
	}

	/**
	 * A working room: table, lamps on posts, chests, and a wall of markings.
	 *
	 * Thirteen across, which is large enough that a player walking in stops.
	 * Everything in it is a block somebody would actually have carried down
	 * here, and nothing in it is decorative — the lamps are on posts because
	 * there is nothing to hang them from, and the floor is cobbled because
	 * stone underfoot at this depth is not flat.
	 */
	private static void room(ServerLevel level, BlockPos m, RandomSource random) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				for (int dy = -1; dy <= 6; dy++) {
					BlockPos at = m.offset(dx, dy, dz);
					boolean shell = Math.abs(dx) == 6 || Math.abs(dz) == 6
						|| dy == -1 || dy == 6;
					level.setBlock(at, shell ? built(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}

		// Lamps on posts. Four of them, in from the corners.
		for (int dx : new int[] { -4, 4 }) {
			for (int dz : new int[] { -4, 4 }) {
				for (int up = 0; up <= 2; up++) {
					level.setBlock(m.offset(dx, up, dz),
						Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
				}
				level.setBlock(m.offset(dx, 3, dz), Blocks.LANTERN.defaultBlockState(), 2);
			}
		}

		// The table: a slab top on a course of cobble, with the bearing on it.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(m.offset(dx, 0, dz), Blocks.COBBLESTONE.defaultBlockState(), 2);
				level.setBlock(m.offset(dx, 1, dz),
					Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2);
			}
		}
		level.setBlock(m.offset(0, 2, 0), Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);

		// The wall of markings: chiselled bricks in a block, which at a glance
		// is somebody's working-out and on inspection is nothing at all.
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = 1; dy <= 4; dy++) {
				if (random.nextInt(4) == 0) {
					continue;
				}
				level.setBlock(m.offset(dx, dy, -5),
					Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
			}
		}

		chest(level, m.offset(-5, 1, 3), random, new ItemStack(Items.TORCH, 64));
		chest(level, m.offset(5, 1, 3), random, new ItemStack(Items.RAIL, 32));
		sign(level, m.offset(0, 1, -4),
			new String[] { "IT IS NOT", "FAR NOW", "", "IT IS NOT FAR" });
	}

	/**
	 * Two by two, dead straight, torch-lit, railed, for two hundred and forty.
	 *
	 * The straightness is the entire content and it is worth being pedantic
	 * about: no wander, no grade, no branch, no ore taken on the way. Anything
	 * that varies would let a player read it as mining, and the moment it reads
	 * as mining it stops saying anything at all.
	 */
	private static BlockPos tunnel(ServerLevel level, BlockPos from, Direction heading,
	                               RandomSource random) {
		Direction across = heading.getClockWise();
		BlockPos at = from;

		for (int step = 6; step < RUN; step++) {
			at = from.relative(heading, step);
			for (int side = 0; side <= 1; side++) {
				for (int up = 0; up <= 1; up++) {
					level.setBlock(at.relative(across, side).above(up),
						Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
				// Cobbled, because stone at this depth is not level and a floor
				// somebody laid is the difference between a tunnel and a hole.
				level.setBlock(at.relative(across, side).below(),
					Blocks.COBBLESTONE.defaultBlockState(), 2);
			}
			if (step % TORCH_EVERY == 0) {
				BlockPos wall = at.relative(across, -1);
				if (!level.getBlockState(wall).isAir()) {
					level.setBlock(at.above(), Blocks.WALL_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, across), 2);
				}
			}
			// Rail, and it stops twenty short of the face — he ran out before
			// the digging did, which is a better detail than either running out
			// together or the rail going all the way.
			if (step < RUN - 20) {
				level.setBlock(at, Blocks.RAIL.defaultBlockState()
					.setValue(BlockStateProperties.RAIL_SHAPE,
						heading.getAxis() == Direction.Axis.X
							? RailShape.EAST_WEST : RailShape.NORTH_SOUTH), 2);
			}
		}
		return at;
	}

	/**
	 * Where it stops, and it stops the way a person stops.
	 *
	 * The face is rough, there is rubble at the foot of it, and the tools are
	 * on the floor rather than in a chest. Nothing here says why. Two hundred
	 * and forty blocks of somebody knowing exactly where they were going, and
	 * then a pick lying down.
	 */
	private static void abandoned(ServerLevel level, BlockPos end, Direction heading,
	                              RandomSource random) {
		Direction across = heading.getClockWise();
		for (int i = 0; i < 12; i++) {
			BlockPos at = end.relative(across, random.nextInt(2))
				.relative(heading, -random.nextInt(4));
			if (level.getBlockState(at).isAir()) {
				level.setBlock(at, random.nextBoolean()
					? Blocks.COBBLESTONE.defaultBlockState()
					: Blocks.GRAVEL.defaultBlockState(), 2);
			}
		}
		BlockPos rest = end.relative(heading, -2);
		if (level.getBlockState(rest).isAir()) {
			level.setBlock(rest, Blocks.CHEST.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, heading.getOpposite()), 2);
			if (level.getBlockEntity(rest) instanceof ChestBlockEntity chest) {
				ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
				pick.setDamageValue(pick.getMaxDamage() - 40);
				chest.setItem(0, pick);
				chest.setItem(1, new ItemStack(Items.TORCH, 9));
				Loot.scatter(chest, random, Loot.Tier.LARDER);
			}
		}
		sign(level, end.relative(heading, -1).relative(across, 1),
			new String[] { "", "NOT", "FAR ENOUGH", "" });
	}

	private static void chest(ServerLevel level, BlockPos at, RandomSource random,
	                          ItemStack first) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			chest.setItem(0, first);
			Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
		}
	}

	private static void sign(ServerLevel level, BlockPos at, String[] lines) {
		if (!level.getBlockState(at).isAir()) {
			return;
		}
		level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 0), 2);
		if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
			SignText text = new SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, Component.literal(
					row < lines.length ? lines[row] : ""));
			}
			sign.setText(text, true);
			sign.setWaxed(true);
		}
	}

	/**
	 * What a person builds with when they are a long way down and carrying it.
	 *
	 * Cobblestone, mostly, because that is what you get from the hole you are
	 * standing in. Nothing quarried, nothing decorative, nothing that had to be
	 * brought from the surface.
	 */
	private static BlockState built(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 6) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 9) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
