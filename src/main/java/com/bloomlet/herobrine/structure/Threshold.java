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

	/** The two built rooms either side of the cells, same depth so they line up. */
	private static final int WING_W = 11;

	public static void raise(ServerLevel level, BlockPos site, RandomSource random) {
		int surface = Ground.topOf(level, site.getX(), site.getZ());
		BlockPos mouth = new BlockPos(site.getX(), surface, site.getZ());

		compound(level, mouth, random);
		BlockPos bottom = stair(level, mouth, random);

		// The approach, and it is meant to be work.
		//
		// A clean tunnel from the stair to the door would make this a corridor
		// with rooms off it. Three separate runs with collapses in them and two
		// branches that go nowhere make it a place the player has to find their
		// way through, and getting lost for two minutes in the dark on the way
		// down is worth more than anything that could be put at the end of it.
		BlockPos leg = Digging.bore(level, bottom, new Vec3(0.2, -0.4, 1.0), 22, 1.6, random, true);
		collapse(level, leg, random, true);
		blindAlley(level, leg, new Vec3(-1.0, -0.1, 0.3), random);

		leg = Digging.bore(level, leg, new Vec3(1.0, -0.3, 0.4), 24, 1.5, random, true);
		collapse(level, leg, random, false);
		blindAlley(level, leg, new Vec3(0.2, 0.1, -1.0), random);

		BlockPos hall = Digging.bore(level, leg, new Vec3(0.6, -0.35, 0.8), 20, 1.7, random, true);
		Digging.hollow(level, hall, 6.5, random);
		Digging.props(level, hall, 8, random);

		// And then the part nobody dug. Records, then the cells, then the room
		// they were watched from — in that order, because the player should
		// read the paperwork before they understand what it was for.
		BlockPos cells = new BlockPos(hall.getX() + 20, hall.getY() - 2, hall.getZ() - BLOCK_D / 2);
		BlockPos records = cells.offset(-WING_W, 0, 0);
		BlockPos office = cells.offset(BLOCK_W, 0, 0);

		// Cut in from the hall FIRST, so the passage is bored through rock
		// rather than through the shelves. Doing this after the rooms were
		// furnished drove it straight through the records room and left the
		// books lying on the floor as items.
		Digging.bore(level, hall, new Vec3(1.0, -0.12, 0.0), 14, 1.6, random, true);

		records(level, records, random);
		cellBlock(level, cells, random);
		office(level, office, random);

		BlockPos beyond = office.offset(WING_W, 2, BLOCK_D / 2);
		BlockPos end = Digging.bore(level, beyond, new Vec3(1.0, -0.3, 0.15), 20, 1.7, random, true);
		Digging.hollow(level, end, 7.0, random);
		seal(level, end, random);

		HerobrineMod.LOGGER.info("threshold raised, mouth at [{}, {}, {}], seal at [{}, {}, {}]",
			mouth.getX(), mouth.getY(), mouth.getZ(), end.getX(), end.getY(), end.getZ());
	}

	/**
	 * The roof came in here.
	 *
	 * Rubble rather than gravel for the body of it — gravel falls, and a
	 * collapse that drains away into the passage below the moment the player
	 * touches it is a physics toy rather than an obstacle. Cobble, tuff and
	 * deepslate hold.
	 *
	 * @param crawlable leave the top block open, so this one can be squeezed
	 *                  through rather than mined. Alternating the two is what
	 *                  stops the route feeling like a series of identical walls.
	 */
	private static void collapse(ServerLevel level, BlockPos at, RandomSource random,
	                             boolean crawlable) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -1; dy <= (crawlable ? 1 : 3); dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir()) {
						continue;
					}
					if (dx * dx + dz * dz > 7) {
						continue;
					}
					level.setBlock(pos, rubble(random), 2);
				}
			}
		}
		// A little loose material on top of it, because a fall leaves dust.
		for (int i = 0; i < 8; i++) {
			BlockPos pos = at.offset(random.nextInt(5) - 2, 2, random.nextInt(5) - 2);
			if (level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.below()).isSolid()) {
				level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * A passage that goes nowhere, and the player cannot know that from here.
	 *
	 * Two of these are the difference between a route and a place. A single
	 * corridor is followed; a fork has to be chosen, and choosing wrong in the
	 * dark forty blocks down is the only way to make somebody feel lost in a
	 * structure this small.
	 */
	private static void blindAlley(ServerLevel level, BlockPos from, Vec3 heading,
	                               RandomSource random) {
		// No paving and no brickwork in here, and that is the whole mechanism.
		// The player learns in about ten seconds that the laid floor is the way
		// on, and from then on a bare passage tells them they have guessed
		// wrong — without a sign, a marker, or anything that admits a designer
		// was involved.
		BlockPos end = Digging.bore(level, from, heading, 9 + random.nextInt(5), 1.4, random);
		collapse(level, end, random, false);
	}

	private static BlockState rubble(RandomSource random) {
		return switch (random.nextInt(4)) {
			case 0 -> Blocks.COBBLESTONE.defaultBlockState();
			case 1 -> Blocks.TUFF.defaultBlockState();
			case 2 -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
			default -> Blocks.STONE.defaultBlockState();
		};
	}

	/**
	 * The compound. Nobody has been here in a long time.
	 *
	 * The homestead says a family lived somewhere. This says an OPERATION was
	 * run here and then abandoned where it stood — two roofless outbuildings, a
	 * field nobody harvested, fence line half gone, and a stair into the ground
	 * that is plainly the reason the rest of it exists.
	 *
	 * Not one sign anywhere. A sign is somebody explaining, and there is nobody
	 * left here to explain; every word in this place is underground, written in
	 * a book, by a man who did not expect to be read. The surface has to be
	 * read off the shapes alone.
	 */
	private static void compound(ServerLevel level, BlockPos at, RandomSource random) {
		shell(level, at.offset(-19, 0, -4), 9, 7, random);
		shell(level, at.offset(11, 0, -13), 7, 6, random);

		// A field, long dead. Farmland with nothing growing reads as a season
		// that never got finished.
		for (int dx = -8; dx <= 4; dx++) {
			for (int dz = 8; dz <= 18; dz++) {
				if (random.nextInt(7) == 0) {
					continue;   // gone back to grass in patches
				}
				BlockPos soil = new BlockPos(at.getX() + dx,
					Ground.topOf(level, at.getX() + dx, at.getZ() + dz), at.getZ() + dz);
				level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
				if (random.nextInt(4) == 0) {
					level.setBlock(soil.above(), Blocks.DEAD_BUSH.defaultBlockState(), 2);
				}
			}
		}

		// What is left of a fence, and it is mostly gaps.
		for (int dx = -20; dx <= 14; dx += 1) {
			if (random.nextInt(3) != 0) {
				continue;
			}
			post(level, at.getX() + dx, at.getZ() - 16, random);
			post(level, at.getX() + dx, at.getZ() + 20, random);
		}

		// A way to the hole worn into the ground, so the compound reads as
		// somewhere people walked between rather than three ruins near a pit.
		trodden(level, at, at.offset(-15, 0, 0), random);
		trodden(level, at, at.offset(11, 0, -10), random);
		trodden(level, at, at.offset(-2, 0, 12), random);

		scatter(level, at, random);
		mouth(level, at, random);
	}

	/** A line of worn ground between two places somebody went often. */
	private static void trodden(ServerLevel level, BlockPos from, BlockPos to,
	                            RandomSource random) {
		int steps = (int)Math.sqrt(from.distSqr(to));
		for (int i = 0; i <= steps; i++) {
			double t = i / (double)Math.max(1, steps);
			int x = (int)Math.round(from.getX() + (to.getX() - from.getX()) * t);
			int z = (int)Math.round(from.getZ() + (to.getZ() - from.getZ()) * t);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (random.nextInt(4) == 0) {
						continue;   // grown back over in places
					}
					int y = Ground.topOf(level, x + dx, z + dz);
					BlockPos on = new BlockPos(x + dx, y, z + dz);
					if (!level.getBlockState(on).isSolid()) {
						continue;
					}
					level.setBlock(on, random.nextInt(4) == 0
						? Blocks.GRAVEL.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState(), 2);
					BlockPos above = on.above();
					if (!level.getBlockState(above).isAir()
						&& !level.getBlockState(above).isSolid()) {
						level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/** A roofless outbuilding, walls to about chest height and no more. */
	private static void shell(ServerLevel level, BlockPos corner, int w, int d,
	                          RandomSource random) {
		int base = Ground.topOf(level, corner.getX() + w / 2, corner.getZ() + d / 2) + 1;
		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
				BlockPos floor = new BlockPos(corner.getX() + dx, base, corner.getZ() + dz);
				for (int up = 0; up <= 4; up++) {
					if (!level.getBlockState(floor.above(up)).isAir()) {
						level.setBlock(floor.above(up), Blocks.AIR.defaultBlockState(), 2);
					}
				}
				level.setBlock(floor.below(), weathered(random), 2);
				if (!wall) {
					continue;
				}
				// Height falls away at random, so it reads as collapsed rather
				// than as a wall somebody chose to build low.
				int height = 1 + random.nextInt(3);
				for (int up = 0; up < height; up++) {
					level.setBlock(floor.above(up), weathered(random), 2);
				}
			}
		}
		// A gap where the door was.
		for (int up = 0; up < 3; up++) {
			level.setBlock(new BlockPos(corner.getX() + w / 2, base + up, corner.getZ()),
				Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void post(ServerLevel level, int x, int z, RandomSource random) {
		int y = Ground.topOf(level, x, z) + 1;
		int height = 1 + random.nextInt(2);
		for (int up = 0; up < height; up++) {
			level.setBlock(new BlockPos(x, y + up, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
		}
	}

	/** Things left where they were put down. */
	private static void scatter(ServerLevel level, BlockPos at, RandomSource random) {
		for (int i = 0; i < 26; i++) {
			int x = at.getX() + random.nextInt(37) - 18;
			int z = at.getZ() + random.nextInt(37) - 18;
			BlockPos on = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			if (!level.getBlockState(on).isAir()) {
				continue;
			}
			int roll = random.nextInt(10);
			if (roll < 3) {
				level.setBlock(on, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2);
			} else if (roll < 5) {
				level.setBlock(on.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
			} else if (roll < 7) {
				level.setBlock(on, Blocks.BARREL.defaultBlockState(), 2);
			} else if (roll == 7) {
				level.setBlock(on, Blocks.CAULDRON.defaultBlockState(), 2);
			} else if (roll == 8) {
				level.setBlock(on, Blocks.DEAD_BUSH.defaultBlockState(), 2);
			}
		}
	}

	/** The way in: a collapsed opening, not a built entrance. */
	private static void mouth(ServerLevel level, BlockPos at, RandomSource random) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 1; dz++) {
				for (int up = 1; up <= 4; up++) {
					BlockPos clear = at.offset(dx, up, dz);
					if (!level.getBlockState(clear).isAir()) {
						level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		for (int side = -1; side <= 1; side += 2) {
			for (int up = 1; up <= 1 + random.nextInt(3); up++) {
				level.setBlock(at.offset(side * 2, up, -2), weathered(random), 2);
			}
		}
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
			if (step % 4 == 0) {
				// Against the side wall, not the middle of the stair. The
				// bracket needs something solid behind it, and every block
				// beside the centre of a three-wide cut is also air — which is
				// why none of these were appearing at all.
				bracket(level, at.offset(random.nextBoolean() ? 1 : -1, 2, 0), random);
			}
		}
		return at;
	}

	/**
	 * A light that mostly is not working any more.
	 *
	 * Redstone torches carry a LIT state, so a burnt-out one is a real block
	 * rather than an absence — the fitting is still on the wall and it is
	 * simply dark. That is worth far more than leaving a gap: a corridor with
	 * no lights was never lit, and a corridor where two in five are dead was
	 * maintained by somebody who stopped.
	 *
	 * A fifth are gone entirely, fallen off the wall. Nothing has replaced
	 * them.
	 */
	private static void bracket(ServerLevel level, BlockPos at, RandomSource random) {
		int roll = random.nextInt(5);
		if (roll == 0) {
			return;   // fell off years ago
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (!level.getBlockState(at.relative(side)).isSolid()
				|| !level.getBlockState(at).isAir()) {
				continue;
			}
			level.setBlock(at, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, side.getOpposite())
				.setValue(BlockStateProperties.LIT, roll > 2), 2);
			return;
		}
	}

	/**
	 * A brick box in the middle of a cave.
	 *
	 * Walls, floor and ceiling and nothing else. Both wings and the cell block
	 * share it, and share a depth, so the three line up into one straight run
	 * the player walks end to end without ever choosing a direction.
	 */
	private static void room(ServerLevel level, BlockPos origin, int w, int d,
	                         RandomSource random) {
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				for (int y = 0; y < BLOCK_H; y++) {
					boolean wall = x == 0 || x == w - 1 || z == 0 || z == d - 1
						|| y == 0 || y == BLOCK_H - 1;
					BlockPos pos = origin.offset(x, y, z);
					level.setBlock(pos, wall ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
					Digging.seal(level, pos);
				}
			}
		}
	}

	/** Knock a hole through one wall at the corridor height. */
	private static void doorGap(ServerLevel level, BlockPos origin, int x, int z0, int z1) {
		for (int z = z0; z <= z1; z++) {
			for (int y = 1; y < BLOCK_H - 1; y++) {
				level.setBlock(origin.offset(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Records. The paperwork comes before the cells.
	 *
	 * Ordered on purpose: a player who walks into the cells cold sees a
	 * dungeon, and a dungeon is a place monsters live. A player who has just
	 * read a numbered list of villagers by trade walks into the same room and
	 * sees where the list was kept. Same blocks, completely different building.
	 *
	 * Shelves with gaps in them, because somebody took an armful and left in a
	 * hurry, and what is still here is the part that was not worth carrying.
	 */
	private static void records(ServerLevel level, BlockPos origin, RandomSource random) {
		room(level, origin, WING_W, BLOCK_D, random);
		doorGap(level, origin, WING_W - 1, CORRIDOR_Z0, CORRIDOR_Z1);
		doorGap(level, origin, 0, CORRIDOR_Z0 + 1, CORRIDOR_Z0 + 1);

		for (int x = 1; x < WING_W - 1; x++) {
			for (int z : new int[]{1, BLOCK_D - 2}) {
				if (random.nextInt(4) == 0) {
					continue;   // an armful gone
				}
				level.setBlock(origin.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState(), 2);
				if (random.nextInt(3) > 0) {
					level.setBlock(origin.offset(x, 2, z), Blocks.BOOKSHELF.defaultBlockState(), 2);
				}
			}
		}
		lectern(level, origin.offset(3, 1, 3), LabBooks.intake());
		lectern(level, origin.offset(7, 1, 9), LabBooks.theDoor());
		crate(level, origin.offset(2, 1, 10), LabBooks.whatIWas(), random);
		level.setBlock(origin.offset(8, 1, 2), Blocks.BARREL.defaultBlockState(), 2);
		level.setBlock(origin.offset(9, 1, 3), Blocks.BARREL.defaultBlockState(), 2);
		mess(level, origin, WING_W, random);
		for (int x = 3; x < WING_W; x += 4) {
			bracket(level, origin.offset(x, 3, CORRIDOR_Z0), random);
		}
	}

	/**
	 * The room it was all watched from.
	 *
	 * At the far end, so the player reaches it having already walked the
	 * corridor — and then turns round and sees the corridor again through a
	 * wall of glass, from the seat somebody sat in to do exactly that. The view
	 * is the whole point of the room, and it is a view the player has just been
	 * on the wrong side of.
	 *
	 * Brewing stands and a cauldron because they are the only apparatus the
	 * game has. They are doing a job no book can: work was done here, and it
	 * was not honest work.
	 */
	private static void office(ServerLevel level, BlockPos origin, RandomSource random) {
		room(level, origin, WING_W, BLOCK_D, random);
		doorGap(level, origin, WING_W - 1, CORRIDOR_Z0 + 1, CORRIDOR_Z0 + 1);

		// The observation wall: a door at the middle, glass either side of it.
		for (int z = CORRIDOR_Z0; z <= CORRIDOR_Z1; z++) {
			for (int y = 1; y < BLOCK_H - 1; y++) {
				boolean door = z == CORRIDOR_Z0 + 1 && y < 3;
				// One pane in four is out. Somebody went through this glass, or
				// something did, and nobody put it back.
				BlockState pane = random.nextInt(4) == 0
					? Blocks.CAVE_AIR.defaultBlockState()
					: Blocks.GLASS_PANE.defaultBlockState();
				level.setBlock(origin.offset(0, y, z),
					door ? Blocks.CAVE_AIR.defaultBlockState() : pane, 2);
			}
		}

		level.setBlock(origin.offset(3, 1, 2), Blocks.BREWING_STAND.defaultBlockState(), 2);
		level.setBlock(origin.offset(4, 1, 2), Blocks.BREWING_STAND.defaultBlockState(), 2);
		level.setBlock(origin.offset(6, 1, 2), Blocks.CAULDRON.defaultBlockState(), 2);
		level.setBlock(origin.offset(2, 1, 4), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
		level.setBlock(origin.offset(7, 1, 4), Blocks.SMITHING_TABLE.defaultBlockState(), 2);

		// A desk, facing the glass.
		level.setBlock(origin.offset(3, 1, 6), Blocks.OAK_FENCE.defaultBlockState(), 2);
		level.setBlock(origin.offset(3, 2, 6), Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState(), 2);
		level.setBlock(origin.offset(4, 1, 6), Blocks.SPRUCE_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);

		lectern(level, origin.offset(5, 1, 8), LabBooks.subjectNine());
		crate(level, origin.offset(8, 1, 9), LabBooks.lastDay(), random);
		level.setBlock(origin.offset(2, 1, 10), Blocks.BARREL.defaultBlockState(), 2);
		mess(level, origin, WING_W, random);
		for (int x = 3; x < WING_W; x += 4) {
			bracket(level, origin.offset(x, 3, CORRIDOR_Z1), random);
		}
	}

	/**
	 * Whatever happened here, nobody tidied up after it.
	 *
	 * Cobwebs, spilled redstone, a cracked floor. Restrained on purpose — a
	 * room strewn with debris reads as a set. A room that is merely dirty, with
	 * one or two things obviously knocked over, reads as a room somebody left
	 * quickly, and that is a completely different feeling.
	 */
	private static void mess(ServerLevel level, BlockPos origin, int w, RandomSource random) {
		for (int i = 0; i < 22; i++) {
			BlockPos at = origin.offset(1 + random.nextInt(w - 2),
				1 + random.nextInt(BLOCK_H - 2), 1 + random.nextInt(BLOCK_D - 2));
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			int roll = random.nextInt(8);
			if (roll < 3 && Digging.touchesSomething(level, at)) {
				level.setBlock(at, Blocks.COBWEB.defaultBlockState(), 2);
			} else if (roll < 5 && level.getBlockState(at.below()).isSolid()) {
				level.setBlock(at, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
			} else if (roll == 5 && level.getBlockState(at.below()).isSolid()) {
				level.setBlock(at.below(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 2);
			}
		}
	}

	private static void lectern(ServerLevel level, BlockPos at,
	                            net.minecraft.world.item.ItemStack book) {
		level.setBlock(at, Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
			.setValue(BlockStateProperties.HAS_BOOK, true), 2);
		if (level.getBlockEntity(at)
				instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
			lectern.setBook(book);
		}
	}

	private static void crate(ServerLevel level, BlockPos at,
	                          net.minecraft.world.item.ItemStack book, RandomSource random) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		if (level.getBlockEntity(at)
				instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			chest.setItem(0, book);
			Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
		}
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
