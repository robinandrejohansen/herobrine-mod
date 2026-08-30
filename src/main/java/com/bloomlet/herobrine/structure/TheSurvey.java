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
 * hundred and forty blocks on one bearing, torch-lit, railed, rising and
 * falling with the rock and bridged in planks wherever it crosses a void. That
 * is not a mine. Nobody prospects in a straight line — you follow the ore, and
 * the ore is never straight. This is somebody who knew exactly where he was
 * going and how far it was, and was prepared to dig for months to get there.
 *
 * Straight in PLAN and not in section, which is the distinction that makes it
 * believable. It never deviates a block horizontally, because that is the whole
 * claim — but it takes the grade as it comes, because a dead-level tunnel
 * through two hundred and forty blocks of varied stone is a thing no person has
 * ever dug.
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

		// IT IS DIGGING TOWARD THE WAY, AND IT ALWAYS SHOULD HAVE BEEN.
		//
		// The heading was a random horizontal direction, which makes two hundred and
		// forty blocks of lit, paved, rail-laid tunnel a walk to nowhere in
		// particular — and the sign at the far end says NOT FAR ENOUGH, which is a
		// good line about a distance nobody can measure and a great one about a
		// distance they can.
		//
		// Aimed at the homestead, because the way through is UNDER IT. He was
		// tunnelling from the tower toward the crossing and he stopped short. That
		// makes the sign literally true, it makes the direction checkable against a
		// map the player already has, and it explains what the whole warren was for
		// without a word: the plan was to reach the hole without going overground.
		//
		// Falls back to a roll if the homestead is not sited, which cannot happen in
		// the normal sequence — the tower is third and the homestead is first — but
		// this is also reachable by command.
		BlockPos home = level.getServer() == null ? null
			: Dwellings.origin(level.getServer().overworld());
		Direction heading = home == null
			? Direction.Plane.HORIZONTAL.getRandomDirection(random)
			: Direction.getApproximateNearest(
				home.getX() - middle.getX(), 0.0, home.getZ() - middle.getZ());

		BlockPos end = tunnel(level, middle, heading, random);
		abandoned(level, end, heading, random);

		HerobrineMod.LOGGER.info(
			"the survey at [{}, {}, {}], driving {} for {} blocks — {} short of the way",
			middle.getX(), middle.getY(), middle.getZ(), heading.getName(), RUN,
			home == null ? "aimed at nothing"
				: (int) Math.sqrt(end.distSqr(home)) + " blocks");
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
	 * Two by two, on one bearing, for two hundred and forty blocks.
	 *
	 * STRAIGHT IN PLAN AND NOT IN SECTION, which is the distinction that makes
	 * this work. It never deviates by a single block horizontally — that is the
	 * whole claim, and any wander would let it be read as mining — but it rises
	 * and falls, in runs, because the rock does and he was not going to quarry
	 * out half a mountain to keep a level floor.
	 *
	 * A dead-level tunnel through two hundred and forty blocks of varied stone
	 * is a thing no person has ever dug. One that holds its bearing and takes
	 * the grade as it comes is exactly what somebody does when they have
	 * decided where they are going and are being practical about the rest.
	 *
	 * And where it crosses open air it is BRIDGED rather than left as a hole.
	 * The first version punched straight through caves and ravines and left
	 * gaps, which undoes the effect entirely — a tunnel with holes in it reads
	 * as terrain that happened to line up, not as something cut.
	 */
	private static BlockPos tunnel(ServerLevel level, BlockPos from, Direction heading,
	                               RandomSource random) {
		Direction across = heading.getClockWise();
		BlockPos at = from;
		int y = from.getY();
		// Grade runs: level for a while, then up or down for a while. Changed
		// in stretches rather than per block, so it reads as a decision
		// somebody made and stuck to rather than as noise.
		int grade = 0;
		int untilTurn = 20 + random.nextInt(30);

		for (int step = 6; step < RUN; step++) {
			if (--untilTurn <= 0) {
				grade = random.nextInt(3) - 1;
				untilTurn = 18 + random.nextInt(34);
			}
			// One in two blocks of rise or fall at most, which is walkable
			// without jumping and is what a person cuts.
			if (grade != 0 && step % 2 == 0) {
				y += grade;
			}
			at = new BlockPos(
				from.getX() + heading.getStepX() * step, y,
				from.getZ() + heading.getStepZ() * step);

			boolean overAir = !level.getBlockState(at.below()).isSolid()
				&& !level.getBlockState(at.below(2)).isSolid();

			for (int side = 0; side <= 1; side++) {
				BlockPos lane = at.relative(across, side);
				for (int up = 0; up <= 2; up++) {
					level.setBlock(lane.above(up), Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
				if (overAir) {
					deck(level, lane, side, across, step, random);
				} else if (!level.getBlockState(lane.below()).isSolid()) {
					level.setBlock(lane.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
				}
			}

			if (step % TORCH_EVERY == 0 && !overAir) {
				BlockPos wall = at.relative(across, -1);
				if (level.getBlockState(wall).isSolid()) {
					level.setBlock(at.above(), Blocks.WALL_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, across), 2);
				}
			}
			// Rail only on solid ground and only while it lasted: it stops
			// twenty short of the face, because he ran out before the digging
			// did, and it never runs across the bridges — nobody lays track on
			// planks they have not finished nailing down.
			if (step < RUN - 20 && !overAir && grade == 0) {
				level.setBlock(at, Blocks.RAIL.defaultBlockState()
					.setValue(BlockStateProperties.RAIL_SHAPE,
						heading.getAxis() == Direction.Axis.X
							? RailShape.EAST_WEST : RailShape.NORTH_SOUTH), 2);
			}
		}
		return at;
	}

	/**
	 * A plank deck over a gap, and half of it is gone.
	 *
	 * The best thing in the tunnel and it costs four block types. Where the cut
	 * meets a cave or a ravine he laid boards across rather than filling it —
	 * which is what anybody does, because filling a ravine is a week and a
	 * bridge is an afternoon — and the boards have not lasted.
	 *
	 * MISSING PLANKS ARE PLACED IN A PATTERN THAT IS ALWAYS CROSSABLE. One in
	 * five is gone, and never both lanes at the same step, so there is always a
	 * board to be on and the crossing is a matter of watching your feet rather
	 * than of luck. A bridge that can strand somebody two hundred blocks down
	 * is not tension, it is a bug report — and the drop below is real, so it
	 * only has to be possible once to ruin an evening.
	 *
	 * The rail never crosses these. Track on an unfinished bridge is the one
	 * detail that would say somebody had come back and tidied up.
	 */
	private static void deck(ServerLevel level, BlockPos lane, int side,
	                         Direction across, int step, RandomSource random) {
		// A gap in one lane only, alternating which, so a walker can always
		// step sideways onto a board rather than jump a hole in both.
		boolean gone = (step * 7 + side * 3) % 19 < 3 && side == step % 2;
		BlockPos board = lane.below();
		if (gone) {
			level.setBlock(board, Blocks.AIR.defaultBlockState(), 2);
		} else {
			level.setBlock(board, random.nextInt(6) == 0
				? Blocks.SPRUCE_SLAB.defaultBlockState()
				: Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
		}

		// A rail along the outside edge, and it has come down in places too.
		BlockPos edge = lane.relative(across, side == 0 ? -1 : 1);
		if (level.getBlockState(edge).isAir() && random.nextInt(4) != 0) {
			level.setBlock(edge, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
		}
		// A lantern hung from the handrail every so often, which is the only
		// light out over a drop and is worth more than any of the wall torches.
		if (step % 14 == 0 && side == 0
			&& level.getBlockState(edge.above()).isAir()) {
			level.setBlock(edge.above(), Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
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
		// AND THE SECOND SIGN, which is the one that turns a dead end into a fact.
		//
		// A player who has walked two hundred and forty blocks of tunnel and found a
		// worn pickaxe has been told nothing. Told which way he was digging, they
		// can put it against the map in their pocket — and the answer is the first
		// house, which they have already been to, which means this hole was an
		// attempt to reach the crossing from underneath.
		sign(level, end.relative(heading, -1).relative(across, -1),
			new String[] { "I WAS", "DIGGING", "TOWARD", "THE HOUSE" });
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
