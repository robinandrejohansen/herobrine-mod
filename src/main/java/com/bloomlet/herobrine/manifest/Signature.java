package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The things the original story actually says he does.
 *
 * The 2010 creepypasta is very short and almost all of it is a list of marks
 * left on a world: "2x2 tunnels in the rocks, small perfect pyramids made of
 * sand in the ocean, and groves of trees with all their leaves cut off". The
 * community added redstone torches in caves and passages found bricked up.
 *
 * Not one of those is an attack, a jump scare, or an encounter. They are all
 * the same thing — evidence that somebody has been working, methodically, in a
 * world you thought was yours. That is the register the whole mod was reaching
 * for from the start, and it turns out the source material had already written
 * it down.
 *
 * WHY EACH OF THESE IS BETTER THAN ANYTHING INVENTED. A player who finds a
 * stripped grove has two thoughts in order: "that is odd", and then, if they
 * have ever heard of him, "oh". The second thought is doing something no
 * designed effect can do, because it is not coming from the mod at all — it is
 * coming from fifteen years of the internet. Everything here is a quotation.
 *
 * SO NONE OF IT IS SIGNPOSTED. No sound, no message, no particles, nothing
 * pointing at any of it. Half of these will never be found and that is correct;
 * the ones that are found are found by somebody who walked round a corner, and
 * that is the only way they work.
 */
public final class Signature {
	private Signature() {}

	private static final int NEAR = 26;
	private static final int FAR = 74;

	/**
	 * A grove with its leaves taken off.
	 *
	 * The cheapest thing in this file and probably the best. It is deniable at
	 * a glance — trees do die — and completely undeniable once a player counts
	 * them and finds every trunk in a patch bare and every trunk outside it
	 * fine. Nothing has been added to the world. Something has been removed
	 * from it, in a shape.
	 */
	public static boolean grove(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		BlockPos middle = around(level, player, random);
		if (middle == null || !Marks.clearOf(level, middle)) {
			return false;
		}

		int stripped = 0;
		int radius = 9 + random.nextInt(6);
		for (BlockPos at : BlockPos.betweenClosed(
				middle.offset(-radius, -4, -radius), middle.offset(radius, 14, radius))) {
			if (!level.getBlockState(at).is(net.minecraft.tags.BlockTags.LEAVES)) {
				continue;
			}
			// Round, not square. A square hole in a canopy is a player with a
			// shears and a plan; a rough circle is harder to explain.
			if (middle.distToCenterSqr(at.getX(), middle.getY(), at.getZ())
				> (double)radius * radius) {
				continue;
			}
			level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			stripped++;
		}
		if (stripped < 20) {
			return false;   // no wood here worth the name
		}
		// AND THE GROUND UNDER IT IS DEAD. Grass to coarse dirt and podzol in
		// patches, a dead bush here and there: the trees did not lose their leaves
		// to the season.
		for (int i = 0; i < radius * 3; i++) {
			int x = middle.getX() + random.nextInt(radius * 2 + 1) - radius;
			int z = middle.getZ() + random.nextInt(radius * 2 + 1) - radius;
			BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
			BlockState was = level.getBlockState(ground);
			if (!was.is(Blocks.GRASS_BLOCK) && !was.is(Blocks.DIRT)) {
				continue;
			}
			level.setBlock(ground, random.nextInt(3) == 0
				? Blocks.PODZOL.defaultBlockState()
				: Blocks.COARSE_DIRT.defaultBlockState(), 2);
			BlockPos over = ground.above();
			if (random.nextInt(4) == 0 && level.getBlockState(over).isAir()) {
				level.setBlock(over, Blocks.DEAD_BUSH.defaultBlockState(), 2);
			} else if (!level.getBlockState(over).isAir() && !level.getBlockState(over).isSolid()) {
				level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);
			}
		}
		HerobrineMod.LOGGER.info("a grove was stripped at [{}, {}]: {} leaves",
			middle.getX(), middle.getZ(), stripped);
		ManifestationDirector.noteLocation(middle);
		return true;
	}

	/**
	 * A small perfect pyramid of sand, standing in open water.
	 *
	 * The single most quoted image in the whole legend and nobody has one in
	 * their world. It is also the only trace here that is unmistakably BUILT —
	 * everything else he does is subtraction, and this is four courses of sand
	 * stacked in the middle of an ocean by somebody who wanted it there.
	 *
	 * Perfect on purpose. Weathering it or leaning it would make it read as
	 * terrain; the whole point is that it is too regular to be anything but
	 * deliberate.
	 */
	public static boolean pyramid(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 60; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = (int)Math.round(player.getX() + Math.cos(angle) * range);
			int z = (int)Math.round(player.getZ() + Math.sin(angle) * range);

			int surface = level.getSeaLevel();
			BlockPos top = new BlockPos(x, surface, z);
			if (!level.getBlockState(top).is(Blocks.WATER)) {
				continue;
			}
			// Deep enough that it is out at sea rather than on a shoreline, and
			// clear enough all round that it stands alone.
			if (!openWater(level, top, 5)) {
				continue;
			}

			int size = 3;   // a 7x7 base, four courses, apex above the waves
			for (int course = 0; course <= size; course++) {
				int reach = size - course;
				for (int dx = -reach; dx <= reach; dx++) {
					for (int dz = -reach; dz <= reach; dz++) {
						level.setBlock(new BlockPos(x + dx, surface - 2 + course, z + dz),
							Blocks.SAND.defaultBlockState(), 2);
					}
				}
			}
			HerobrineMod.LOGGER.info("a pyramid stands in the water at [{}, {}]", x, z);
			ManifestationDirector.noteLocation(new BlockPos(x, surface, z));
			return true;
		}
		return false;
	}

	private static boolean openWater(ServerLevel level, BlockPos middle, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (!level.getBlockState(middle.offset(dx, 0, dz)).is(Blocks.WATER)) {
					return false;
				}
				// And it has to be deep, or the base sits on a sandbank and the
				// thing looks like an island rather than a construction.
				if (!level.getBlockState(middle.offset(dx, -2, dz)).is(Blocks.WATER)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * A tunnel two blocks square, dead straight, going a long way.
	 *
	 * The other half of the original sentence, and the one that is unnerving
	 * for a reason a player can articulate: nobody digs like this. A person
	 * mining follows ore and wanders; a person going somewhere digs one block
	 * wide. Two by two, arrow-straight, on a level grade, for eighty blocks, is
	 * a corridor — and it is in rock nobody has been to.
	 *
	 * It goes nowhere. There is no chamber at the end, no ore, no reason. It
	 * simply stops, which is the part that lasts.
	 */
	public static boolean tunnel(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		if (player.getY() > 56) {
			return false;   // this is a thing found underground, by miners
		}
		Direction heading = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos start = player.blockPosition()
			.relative(heading, 18 + random.nextInt(24))
			.above(random.nextInt(9) - 4);

		if (!buriedIn(level, start)) {
			return false;
		}
		Direction across = heading.getClockWise();
		int length = 60 + random.nextInt(40);
		int cut = 0;

		for (int step = 0; step < length; step++) {
			BlockPos at = start.relative(heading, step);
			for (int side = 0; side <= 1; side++) {
				for (int up = 0; up <= 1; up++) {
					BlockPos pos = at.relative(across, side).above(up);
					if (!level.getBlockState(pos).isSolid()) {
						continue;
					}
					level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					cut++;
				}
			}
		}
		if (cut < 120) {
			return false;   // it broke into a cave almost at once; not worth it
		}
		HerobrineMod.LOGGER.info("a 2x2 tunnel was cut at [{}, {}, {}] running {}",
			start.getX(), start.getY(), start.getZ(), heading.getName());
		ManifestationDirector.noteLocation(start);
		return true;
	}

	/** Solid rock all round, so the tunnel starts in stone rather than in air. */
	private static boolean buriedIn(ServerLevel level, BlockPos at) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(2, 2, 2))) {
			if (!level.getBlockState(pos).isSolid()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * One redstone torch, lit, in a cave nobody has been in.
	 *
	 * The smallest thing in the mod. It is also the one that has made the most
	 * people close the game, because a torch means a person, redstone means
	 * that person chose it over a normal torch, and the red light is the only
	 * light down there.
	 *
	 * Exactly one. Two would be a base and three would be a corridor; one is
	 * somebody standing here, once.
	 */
	public static boolean torch(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		if (player.getY() > 48 || level.canSeeSky(player.blockPosition())) {
			return false;
		}
		for (int attempt = 0; attempt < 60; attempt++) {
			BlockPos at = player.blockPosition().offset(
				random.nextInt(41) - 20, random.nextInt(17) - 8, random.nextInt(41) - 20);

			if (!level.getBlockState(at).isAir()
				|| level.getMaxLocalRawBrightness(at) > 3) {
				continue;   // it has to be dark, or it is not saying anything
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos wall = at.relative(side);
				if (!level.getBlockState(wall).isSolid()) {
					continue;
				}
				level.setBlock(at, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, side.getOpposite())
					.setValue(BlockStateProperties.LIT, true), 2);
				HerobrineMod.LOGGER.info("a redstone torch is burning at [{}, {}, {}]",
					at.getX(), at.getY(), at.getZ());
				ManifestationDirector.noteLocation(at);
				return true;
			}
		}
		return false;
	}

	/**
	 * A passage, bricked up.
	 *
	 * The community half of the legend, and the one that is genuinely
	 * unpleasant to find because it is the only trace that was made in response
	 * to YOU. The others are things he did in a world you happen to be in; this
	 * is a hole you dug, closed.
	 *
	 * COBBLESTONE, always, whatever it is closing. Matching the surrounding
	 * stone would let a player miss it, and being missed is the one thing this
	 * must not be — the whole content is standing in your own tunnel looking at
	 * a wall that was not there this morning.
	 *
	 * Never within sight of the player, and never sealing them IN: it is always
	 * put down a passage away from them, so it is walked into rather than
	 * watched, and there is always the way they came.
	 */
	public static boolean seal(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		if (level.canSeeSky(player.blockPosition())) {
			return false;
		}
		for (int attempt = 0; attempt < 80; attempt++) {
			BlockPos at = player.blockPosition().offset(
				random.nextInt(49) - 24, random.nextInt(13) - 6, random.nextInt(49) - 24);
			if (at.closerThan(player.blockPosition(), 12.0)) {
				continue;   // too near; they would see it happen
			}
			if (!corridor(level, at)) {
				continue;
			}
			for (int up = 0; up <= 2; up++) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos pos = at.above(up).relative(side, 0);
					if (level.getBlockState(pos).isAir()) {
						level.setBlock(pos, wall(random), 2);
					}
				}
				if (level.getBlockState(at.above(up)).isAir()) {
					level.setBlock(at.above(up), wall(random), 2);
				}
			}
			HerobrineMod.LOGGER.info("a passage was closed at [{}, {}, {}]",
				at.getX(), at.getY(), at.getZ());
			ManifestationDirector.noteLocation(at);
			return true;
		}
		return false;
	}

	/** Air with rock on both sides and a floor: something somebody walks down. */
	private static boolean corridor(ServerLevel level, BlockPos at) {
		if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) {
			return false;
		}
		if (!level.getBlockState(at.below()).isSolid()) {
			return false;
		}
		for (Direction axis : new Direction[] { Direction.NORTH, Direction.EAST }) {
			boolean walls = level.getBlockState(at.relative(axis)).isSolid()
				&& level.getBlockState(at.relative(axis.getOpposite())).isSolid();
			if (walls) {
				return true;
			}
		}
		return false;
	}

	private static BlockState wall(RandomSource random) {
		return random.nextInt(5) == 0
			? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
			: Blocks.COBBLESTONE.defaultBlockState();
	}

	/** Somewhere on the surface a little way off, on ground that exists. */
	private static BlockPos around(ServerLevel level, ServerPlayer player,
	                               RandomSource random) {
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = (int)Math.round(player.getX() + Math.cos(angle) * range);
			int z = (int)Math.round(player.getZ() + Math.sin(angle) * range);
			int y = Ground.topOf(level, x, z);
			if (y > level.getMinY() + 4) {
				return new BlockPos(x, y, z);
			}
		}
		return null;
	}
}
