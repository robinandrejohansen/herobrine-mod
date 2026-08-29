package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Cadence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.network.chat.Component;

/**
 * WHAT HAPPENED ON THE ROAD, between one place and the next.
 *
 * The trail is six buildings and a map in each. Between them is several hundred
 * blocks of untouched forest, which is a walk rather than a journey — the map
 * points, you hold W, and nothing in between has anything to say. Every piece of
 * story this mod tells is inside a building, so the country between them reads
 * as the loading screen.
 *
 * THE MOD ALREADY KNOWS BOTH ENDS. leaveTheWay has the place you are standing in
 * and the place the map points at, which is a line — and a line is all this needs
 * to put four things on.
 *
 * FOUR, AND NOT MORE. The temptation is a scene every eighty blocks, and that
 * turns a road into a corridor of set dressing: once a player expects something
 * around each bend, the ones they find stop being discoveries and start being
 * furniture. Four over half a kilometre is rare enough that each one is still an
 * event, and sparse enough that most of the walk is genuinely empty — which is
 * what makes the empty part feel like anything at all.
 *
 * EVERY SCENE POINTS ONWARD. Not with a sign saying which way — with the shape
 * of what is left. A camp abandoned mid-meal faces the direction they were
 * going. A drag mark leads off the road, and the road goes on without whoever
 * made it. That is the difference between a landmark and a waypoint: a landmark
 * tells you somebody came this way, and you draw your own conclusion about
 * whether to follow.
 *
 * STAGED, ONE A TICK, because a route can be thirteen hundred blocks long and
 * each scene generates the chunk it lands in. Four generations spread over four
 * ticks is nothing; four in one frame is a stutter on somebody's laptop.
 */
public final class Wayside {
	private Wayside() {}

	/**
	 * ONE THING PER HUNDRED AND FIFTY BLOCKS, and never more than five.
	 *
	 * A flat count was wrong in both directions. Four scenes is right for the long
	 * legs — the church sits eleven hundred blocks out — and on the short first
	 * hop to the town it is one every forty-four, which is exactly the corridor of
	 * set dressing this class was written to avoid. Once a player expects something
	 * round each bend, the ones they find stop being discoveries and become
	 * furniture.
	 *
	 * Scaled, floored at two so even the shortest road has a middle, capped at five
	 * so the longest does not become a trail of breadcrumbs.
	 */
	private static final int EVERY = 150;
	private static final int FEWEST = 2;
	private static final int MOST = 5;
	/** Kept off both ends, so nothing lands in somebody's yard. */
	private static final double FROM_END = 0.18;
	/** And how far off the straight line each one may sit. */
	private static final int WANDERS = 22;

	public static void lay(ServerLevel level, BlockPos from, BlockPos to,
	                       RandomSource random) {
		double span = Math.sqrt(from.distSqr(to));
		if (span < 120.0) {
			return;      // too short to have a middle
		}
		MinecraftServer server = level.getServer();
		int scenes = Math.max(FEWEST, Math.min(MOST, (int) (span / EVERY)));
		int placed = 0;
		for (int i = 0; i < scenes; i++) {
			double along = FROM_END
				+ (1.0 - FROM_END * 2.0) * ((i + 0.5) / scenes)
				+ (random.nextDouble() - 0.5) * 0.06;
			int x = (int) Math.round(from.getX() + (to.getX() - from.getX()) * along)
				+ random.nextInt(WANDERS * 2) - WANDERS;
			int z = (int) Math.round(from.getZ() + (to.getZ() - from.getZ()) * along)
				+ random.nextInt(WANDERS * 2) - WANDERS;
			final int kind = random.nextInt(6);
			final long seed = random.nextLong();
			Cadence.in(server, i + 1, () -> scene(level, x, z, kind, seed, to));
			placed++;
		}
		HerobrineMod.LOGGER.info("{} things left on the {}-block road to [{}, {}]",
			placed, (int) span, to.getX(), to.getZ());
	}

	private static void scene(ServerLevel level, int x, int z, int kind, long seed,
	                          BlockPos onward) {
		// THE GROUND HAS TO EXIST BEFORE IT CAN BE STOOD ON. Same lesson as
		// Dwellings.ready: these land hundreds of blocks out, where nothing is
		// loaded, and Ground.topOf on an ungenerated column returns the bottom of
		// the world.
		level.getChunk(x >> 4, z >> 4);
		int y = Ground.topOf(level, x, z);
		if (y <= level.getSeaLevel()) {
			return;      // in the water. leave it alone.
		}
		BlockPos at = new BlockPos(x, y + 1, z);
		RandomSource random = RandomSource.create(seed);

		// The bearing they were travelling, so everything left behind agrees
		// with it. This is the whole navigational value of the set.
		Direction way = Direction.getApproximateNearest(
			onward.getX() - x, 0.0, onward.getZ() - z);

		switch (kind) {
			case 0 -> camp(level, at, way, random);
			case 1 -> grave(level, at, way, random);
			case 2 -> cart(level, at, way, random);
			case 3 -> burnt(level, at, random);
			case 4 -> dragged(level, at, way, random);
			default -> cairn(level, at, way, random);
		}
	}

	// ---- THE SCENES --------------------------------------------------------

	/**
	 * A camp nobody broke. The fire is out and the beds are still made.
	 *
	 * Two bedrolls and a third space with nothing in it, which is the same trick
	 * the tower's quarters use: an absence is louder than a body.
	 */
	private static void camp(ServerLevel level, BlockPos at, Direction way,
	                         RandomSource random) {
		level.setBlock(at, Blocks.CAMPFIRE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, false), 2);
		for (int side = -1; side <= 1; side += 2) {
			BlockPos bed = at.relative(way.getClockWise(), side * 2);
			ground(level, bed);
			level.setBlock(bed, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), 2);
			level.setBlock(bed.relative(way), Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), 2);
		}
		BlockPos pack = at.relative(way.getOpposite(), 2);
		ground(level, pack);
		level.setBlock(pack, Blocks.BARREL.defaultBlockState(), 2);
		if (level.getBlockEntity(pack) instanceof BarrelBlockEntity store) {
			Loot.scatter(store, random, Loot.Tier.LARDER);
		}
		for (int i = 0; i < 3; i++) {
			BlockPos log = at.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			ground(level, log);
			level.setBlock(log, Blocks.OAK_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.X), 2);
		}
	}

	/** One grave, dug in a hurry, with the shovel still in the heap. */
	private static void grave(ServerLevel level, BlockPos at, Direction way,
	                          RandomSource random) {
		for (int d = 0; d <= 2; d++) {
			BlockPos on = at.relative(way, d);
			ground(level, on);
			level.setBlock(on.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
			level.setBlock(on, d == 1
				? Blocks.DIRT.defaultBlockState()
				: Blocks.COARSE_DIRT.defaultBlockState(), 2);
		}
		BlockPos marker = at.relative(way.getOpposite());
		ground(level, marker);
		level.setBlock(marker, Blocks.OAK_FENCE.defaultBlockState(), 2);
		writ(level, marker.above(), new String[] {
			"", "WE COULD NOT", "CARRY HIM", "ANY FURTHER" });
		// The shovel, left standing in the heap. An item frame is the only way to
		// leave a TOOL lying in the world that does not despawn and cannot be
		// walked into and picked up by accident.
		BlockPos spade = at.relative(way, 3);
		ground(level, spade);
		level.setBlock(spade, Blocks.BARREL.defaultBlockState(), 2);
		if (level.getBlockEntity(spade) instanceof BarrelBlockEntity heap) {
			heap.setItem(0, new ItemStack(Items.IRON_SHOVEL));
		}
	}

	/** A cart off its wheel, and everything that was in it still lying there. */
	private static void cart(ServerLevel level, BlockPos at, Direction way,
	                         RandomSource random) {
		for (int d = 0; d <= 2; d++) {
			for (int side = -1; side <= 1; side++) {
				BlockPos on = at.relative(way, d).relative(way.getClockWise(), side);
				ground(level, on);
				level.setBlock(on, Blocks.OAK_PLANKS.defaultBlockState(), 2);
			}
		}
		// The wheel, off and leaning. A trapdoor on its edge is the only round
		// thing this game has at this size.
		BlockPos wheel = at.relative(way.getClockWise(), 2);
		ground(level, wheel);
		level.setBlock(wheel, Blocks.OAK_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, way)
			.setValue(BlockStateProperties.OPEN, true), 2);
		BlockPos load = at.relative(way, 3);
		ground(level, load);
		level.setBlock(load, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, way), 2);
		if (level.getBlockEntity(load) instanceof ChestBlockEntity box) {
			Loot.scatter(box, random, Loot.Tier.TOWN_TRADE);
		}
	}

	/**
	 * A ring of ground that burned, with the trunks still standing in it.
	 *
	 * No story attached and none needed. Something very hot happened here and
	 * whatever it was did not spread, which is its own small horror in a wood.
	 */
	private static void burnt(ServerLevel level, BlockPos at, RandomSource random) {
		int wide = 4 + random.nextInt(3);
		for (int dx = -wide; dx <= wide; dx++) {
			for (int dz = -wide; dz <= wide; dz++) {
				if (dx * dx + dz * dz > wide * wide) {
					continue;
				}
				BlockPos on = at.offset(dx, 0, dz);
				ground(level, on);
				BlockState was = level.getBlockState(on.below());
				if (!was.isSolid()) {
					continue;
				}
				level.setBlock(on.below(), random.nextInt(4) == 0
					? Blocks.COARSE_DIRT.defaultBlockState()
					: Blocks.PODZOL.defaultBlockState(), 2);
				if (level.getBlockState(on).is(Blocks.GRASS_BLOCK)
					|| !level.getBlockState(on).isAir()) {
					level.setBlock(on, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		for (int i = 0; i < 3; i++) {
			BlockPos stump = at.offset(random.nextInt(wide * 2) - wide, 0,
				random.nextInt(wide * 2) - wide);
			ground(level, stump);
			int tall = 2 + random.nextInt(4);
			for (int up = 0; up < tall; up++) {
				level.setBlock(stump.above(up),
					Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Something was pulled off the road, and the road carries on without it.
	 *
	 * The one scene with no object at the end of it. A player follows the scuff
	 * into the trees expecting to find what was dragged, and finds where it stops
	 * and nothing else — which is the most this mod ever says out loud.
	 */
	private static void dragged(ServerLevel level, BlockPos at, Direction way,
	                            RandomSource random) {
		Direction off = random.nextBoolean() ? way.getClockWise() : way.getCounterClockWise();
		for (int d = 0; d <= 9; d++) {
			BlockPos on = at.relative(off, d);
			ground(level, on);
			if (!level.getBlockState(on.below()).isSolid()) {
				break;
			}
			level.setBlock(on.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
			level.setBlock(on, Blocks.AIR.defaultBlockState(), 2);
			if (d == 2 && random.nextBoolean()) {
				level.setBlock(on, Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
		BlockPos dropped = at.relative(off, 3);
		ground(level, dropped);
		level.setBlock(dropped, Blocks.BARREL.defaultBlockState(), 2);
		if (level.getBlockEntity(dropped) instanceof BarrelBlockEntity store) {
			store.setItem(0, new ItemStack(Items.LEATHER_BOOTS));
			Loot.scatter(store, random, Loot.Tier.HOMESTEAD);
		}
	}

	/**
	 * A stack of stones with a lantern on it, and the direction cut into a board.
	 *
	 * The only scene that is deliberately helpful, and it is the rarest thing on
	 * the road to be helpful about: somebody came this way, and they came this way
	 * ON PURPOSE. Everything else out here is people it went wrong for.
	 */
	private static void cairn(ServerLevel level, BlockPos at, Direction way,
	                          RandomSource random) {
		int tall = 3 + random.nextInt(2);
		for (int up = 0; up < tall; up++) {
			level.setBlock(at.above(up), up == tall - 1
				? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
				: (random.nextBoolean()
					? Blocks.COBBLESTONE.defaultBlockState()
					: Blocks.MOSSY_COBBLESTONE.defaultBlockState()), 2);
		}
		level.setBlock(at.above(tall), Blocks.LANTERN.defaultBlockState(), 2);
		BlockPos board = at.relative(way.getOpposite());
		ground(level, board);
		level.setBlock(board, Blocks.OAK_FENCE.defaultBlockState(), 2);
		writ(level, board.above(), new String[] {
			"KEEP GOING", "", "DO NOT SLEEP", "ON THIS ROAD" });
	}

	// ---- THE MECHANICS -----------------------------------------------------

	/** Drop a position onto whatever the ground actually is under it. */
	private static void ground(ServerLevel level, BlockPos at) {
		// Nothing here is more than a dozen blocks from the scene's own centre, so
		// the chunk is already generated by the time this runs.
		int y = Ground.topOf(level, at.getX(), at.getZ()) + 1;
		if (y != at.getY()) {
			// The caller keeps its own BlockPos; this only fixes the column under
			// it so nothing is left floating over a dip.
			for (int fill = Math.min(y, at.getY()); fill < at.getY(); fill++) {
				level.setBlock(new BlockPos(at.getX(), fill, at.getZ()),
					Blocks.DIRT.defaultBlockState(), 2);
			}
		}
	}

	private static void writ(ServerLevel level, BlockPos at, String[] lines) {
		if (!level.getBlockState(at).isAir()) {
			return;
		}
		level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 0), 2);
		if (!(level.getBlockEntity(at) instanceof SignBlockEntity sign)) {
			return;
		}
		SignText text = new SignText();
		for (int row = 0; row < 4; row++) {
			text = text.setMessage(row, Component.literal(
				row < lines.length ? lines[row] : ""));
		}
		sign.setText(text, true);
	}
}
