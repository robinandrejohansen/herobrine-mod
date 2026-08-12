package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The town under the town.
 *
 * A vaulted chamber the width of the square, forty blocks across and fourteen
 * high, with streets and houses and lantern posts in it and people still living
 * there. Not a cave with things in it — a CHAMBER. The distinction is the whole
 * design: caves are irregular and you read them as terrain, and the moment a
 * player sees a barrel vault and a row of pillars underground they understand
 * that this was quarried out on purpose by somebody who meant to stay.
 *
 * TWO WAYS IN, AND THEY ARE NOTHING ALIKE.
 *
 * The first is behind the altar in the church: a dry stone stair, no mechanism,
 * hidden only by being somewhere nobody looks. Finding it feels like
 * observation.
 *
 * The second is the well in the square, which is a swim. You go down the shaft
 * in the dark not knowing whether it opens out, and it does. Finding that one
 * feels like nerve.
 *
 * A secret with one entrance is a room. A secret with two, which are found
 * differently and arrive at opposite ends, is a place — because the second time
 * a player comes down they get to choose, and choosing is what makes somewhere
 * theirs.
 *
 * THE LIBRARY IS THE POINT OF IT. Whatever else is down here, the thing the
 * player will remember is a room with more books in it than the entire surface
 * town, in a settlement that has no school and whose houses hold four items
 * each. Somebody was reading down here, at length, in secret. The mod never
 * says what about.
 */
public final class Undercity {
	private Undercity() {}

	/** How far under the square. Deep enough that nothing above hints at it. */
	private static final int DEPTH = 26;
	/** Half-width of the chamber floor. */
	private static final int SPAN = 21;
	private static final int HEIGHT = 13;

	public static void dig(ServerLevel level, BlockPos square, BlockPos crypt,
	                       RandomSource random) {
		BlockPos floor = new BlockPos(square.getX(), square.getY() - DEPTH, square.getZ());

		chamber(level, floor, random);
		pillars(level, floor, random);
		streets(level, floor, random);
		pool(level, floor);
		wellShaft(level, square, floor);
		cryptStair(level, crypt, floor);
		// AND THE STAIR IS JOINED TO THE CHAMBER, which it was not.
		//
		// The spiral carves a five-by-five shaft down to the chamber's floor
		// level and stops there, on the assumption that the church stands over
		// the chamber and the shaft therefore breaks into it. That held while the
		// chamber was a perfect disc of a known radius. It stopped holding the
		// moment the outline started to wander: the rim now comes in as close as
		// twelve blocks on some bearings, so on the church's bearing the shaft can
		// land entirely OUTSIDE the wall — and what the player walks down to is a
		// five-by-five dead end with nothing in it.
		//
		// A bore from the shaft foot to the middle fixes it for every shape,
		// because it stops depending on the shape at all.
		join(level, new BlockPos(crypt.getX(), floor.getY(), crypt.getZ()), floor);

		library(level, floor.offset(-13, 0, -13), random);
		for (int i = 0; i < 5; i++) {
			double angle = i * (Math.PI * 2.0 / 5.0) + 0.6;
			int hx = floor.getX() + (int)Math.round(Math.cos(angle) * 14);
			int hz = floor.getZ() + (int)Math.round(Math.sin(angle) * 14);
			dwelling(level, new BlockPos(hx, floor.getY(), hz), random);
		}
		people(level, floor, random);

		HerobrineMod.LOGGER.info("undercity opened at [{}, {}, {}]",
			floor.getX(), floor.getY(), floor.getZ());

		// AND THE BOOKS GO DOWN LAST, after every tunnel and trap is cut.
		//
		// The homestead taught this the expensive way: boring a passage after a
		// chest existed drove straight through it and left the books on the floor
		// as items counting down to despawning. Nothing is carved after this.
		java.util.List<BlockPos> shelves = new java.util.ArrayList<>();
		for (int i = 0; i < 6; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 6.0 + random.nextDouble() * (SPAN - 9);
			BlockPos spot = new BlockPos(
				floor.getX() + (int)Math.round(Math.cos(angle) * range),
				floor.getY(),
				floor.getZ() + (int)Math.round(Math.sin(angle) * range));
			shelves.add(spot);
		}
		Testimony.write(level, shelves, random);

		// AND A THIRD WAY IN, WHICH IS THE ONLY ONE THAT IS EARNED.
		//
		// The stair and the well are both simply found. This is a hundred and
		// forty blocks of gauntlet, and it exists because a cult that is still
		// meeting needs a door the congregation can use and a stranger cannot.
		// Cut outward from the chamber wall so the far end surfaces somewhere
		// else entirely — which also means it is discovered from the far end,
		// by somebody who has no idea what it leads to.
		Trial.cut(level, floor.relative(net.minecraft.core.Direction.EAST, SPAN + 3)
			.above(), net.minecraft.core.Direction.EAST, random);
	}

	/**
	 * Hollow it, and vault the ceiling.
	 *
	 * The roof curves down toward the walls rather than stopping flat, which is
	 * the single thing that separates this from a very large room. A flat
	 * ceiling at this span would need pillars every four blocks to look
	 * plausible and would still read as a warehouse; a barrel vault carries
	 * itself and lets the middle stay open.
	 */
	/**
	 * HOW MANY BEARINGS THE OUTLINE IS DRAWN ON.
	 *
	 * Sixty-four is fine enough that no straight facets show at this span and
	 * coarse enough that the wall wanders in bays rather than in fringe.
	 */
	private static final int BEARINGS = 64;

	private static void chamber(ServerLevel level, BlockPos floor, RandomSource random) {
		// THE SHAPE WAS THE WHOLE PROBLEM, AND NO AMOUNT OF DRESSING FIXES IT.
		//
		// This was sqrt(dx*dx + dz*dz) tested against a constant, with a cosine
		// vault over it — a mathematically perfect disc under a mathematically
		// perfect dome. Every complaint about the undercity reading fake was a
		// complaint about those two lines: wood framing, water channels and
		// overgrown grass laid inside a shape like that still read as decoration
		// applied to a generated volume, because they are.
		//
		// So the outline is drawn once per build as a wandering radius — three
		// harmonics at random phases summed around the circle — and every column
		// reads its limit off that. Three, because one gives an egg and a dozen
		// gives noise: three gives LOBES, which is what a worked-out cavern is.
		// Bays, headlands, a couple of places where the wall comes in close and
		// somewhere it opens right out.
		double[] rim = new double[BEARINGS];
		double[] cap = new double[BEARINGS];
		double p1 = random.nextDouble() * Math.PI * 2.0;
		double p2 = random.nextDouble() * Math.PI * 2.0;
		double p3 = random.nextDouble() * Math.PI * 2.0;
		for (int b = 0; b < BEARINGS; b++) {
			double a = b * Math.PI * 2.0 / BEARINGS;
			rim[b] = SPAN
				+ Math.sin(a * 2 + p1) * (SPAN * 0.22)
				+ Math.sin(a * 3 + p2) * (SPAN * 0.13)
				+ Math.sin(a * 5 + p3) * (SPAN * 0.07);
			// The roof wanders on its own phases, so the highest part of the
			// ceiling is NOT over the middle of the floor. A dome's apex sitting
			// dead centre is the other half of what gives it away.
			cap[b] = HEIGHT + Math.sin(a * 2 + p3) * 2.5 + Math.sin(a * 3 + p1) * 1.5;
		}

		for (int dx = -SPAN - 6; dx <= SPAN + 6; dx++) {
			for (int dz = -SPAN - 6; dz <= SPAN + 6; dz++) {
				double reach = Math.sqrt(dx * dx + dz * dz);
				if (reach > SPAN + 6) {
					continue;
				}
				// Which bearing this column sits on, interpolated between the two
				// nearest so the wall curves instead of stepping.
				double turn = (Math.atan2(dz, dx) + Math.PI * 2.0) % (Math.PI * 2.0);
				double slot = turn / (Math.PI * 2.0) * BEARINGS;
				int lo = (int)Math.floor(slot) % BEARINGS;
				int hi = (lo + 1) % BEARINGS;
				double mix = slot - Math.floor(slot);
				double edge = rim[lo] + (rim[hi] - rim[lo]) * mix;
				double top = cap[lo] + (cap[hi] - cap[lo]) * mix;
				if (reach > edge + 2) {
					continue;
				}

				double t = Math.min(1.0, reach / edge);
				int roof = (int)Math.round(top * Math.cos(t * Math.PI / 2.0));

				for (int dy = -1; dy <= HEIGHT + 4; dy++) {
					BlockPos at = floor.offset(dx, dy, dz);
					if (dy == -1) {
						level.setBlock(at, ground(random, reach / edge), 2);
					} else if (dy <= roof && reach <= edge) {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					} else if (dy <= roof + 1) {
						level.setBlock(at, vaulting(random), 2);
					}
				}
			}
		}
	}

	/**
	 * THE FLOOR STOPS BEING A DISC OF PAVING.
	 *
	 * Laid stone in the middle where people actually stand and work, and then it
	 * gives out — the edges are dirt, gravel and moss with grass growing over
	 * them, because nobody paves the far corners of a cavern and because a floor
	 * that changes underfoot is the cheapest way to say which parts of a room are
	 * used.
	 *
	 * @param out how far toward the wall this column is, nought at the middle
	 */
	private static BlockState ground(RandomSource random, double out) {
		if (out < 0.45) {
			return paving(random);
		}
		// A ragged margin rather than a ring: the further out, the likelier it
		// has gone back to earth, so the boundary frays instead of drawing a line.
		if (random.nextDouble() > (out - 0.35) * 1.6) {
			return paving(random);
		}
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSS_BLOCK.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.COARSE_DIRT.defaultBlockState();
		}
		return roll < 9
			? Blocks.GRAVEL.defaultBlockState()
			: Blocks.PODZOL.defaultBlockState();
	}

	/**
	 * Two rings of them, and they are not structural.
	 *
	 * They hold nothing up — the vault does that. They are there because a
	 * forty-block room with nothing in the middle distance has no sense of
	 * scale, and a player walking between columns can finally tell how far
	 * across it is.
	 */
	private static void pillars(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int ring : new int[] { 9, 17 }) {
			int count = ring == 9 ? 6 : 10;
			for (int i = 0; i < count; i++) {
				double angle = i * (Math.PI * 2.0 / count);
				int px = floor.getX() + (int)Math.round(Math.cos(angle) * ring);
				int pz = floor.getZ() + (int)Math.round(Math.sin(angle) * ring);
				double reach = Math.hypot(px - floor.getX(), pz - floor.getZ());
				int top = (int)Math.round(HEIGHT * Math.cos(Math.min(1.0, reach / SPAN)
					* Math.PI / 2.0));

				for (int dy = 0; dy <= top; dy++) {
					BlockPos at = new BlockPos(px, floor.getY() + dy, pz);
					level.setBlock(at, dy == top
						? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				// A lantern on each, at head height, which is what actually
				// lights the room.
				level.setBlock(new BlockPos(px, floor.getY() + 3, pz)
					.relative(Direction.Plane.HORIZONTAL.iterator().next()),
					Blocks.AIR.defaultBlockState(), 2);
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos hook = new BlockPos(px, floor.getY() + 3, pz).relative(side);
					if (level.getBlockState(hook).isAir()) {
						level.setBlock(hook, Blocks.LANTERN.defaultBlockState(), 2);
						break;
					}
				}
			}
		}
	}

	/** Four roads out from the middle, so the floor is not one flat disc. */
	private static void streets(ServerLevel level, BlockPos floor, RandomSource random) {
		for (Direction lane : Direction.Plane.HORIZONTAL) {
			for (int out = 3; out <= SPAN - 1; out++) {
				for (int side = -1; side <= 1; side++) {
					Direction across = lane.getClockWise();
					BlockPos at = floor.relative(lane, out).relative(across, side).below();
					level.setBlock(at, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The pool the well drops into.
	 *
	 * Three deep, because that is the shallowest a player can fall into from
	 * any height without being hurt, and the whole water entrance depends on
	 * somebody surviving the arrival without being told they would.
	 */
	private static void pool(ServerLevel level, BlockPos floor) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (dx * dx + dz * dz > 10) {
					continue;
				}
				for (int dy = -3; dy <= -1; dy++) {
					level.setBlock(floor.offset(dx, dy, dz),
						Blocks.WATER.defaultBlockState(), 2);
				}
				level.setBlock(floor.offset(dx, -4, dz),
					Blocks.STONE_BRICKS.defaultBlockState(), 2);
			}
		}
		// A kerb, so it reads as a basin rather than as a hole that flooded.
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > 3.4 && reach <= 4.4) {
					level.setBlock(floor.offset(dx, -1, dz),
						Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The well, all the way down, full of water.
	 *
	 * Water the entire height rather than a drop into a pool at the bottom.
	 * A shaft that is dry until the last three blocks kills the first player
	 * who tries it, and a secret entrance that kills you is not a secret
	 * entrance, it is a trap — and DESIGN §9 does not allow one.
	 */
	private static void wellShaft(ServerLevel level, BlockPos square, BlockPos floor) {
		for (int y = floor.getY() + 1; y <= square.getY(); y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos at = new BlockPos(square.getX() + dx, y, square.getZ() + dz);
					boolean core = dx == 0 && dz == 0;
					level.setBlock(at, core
						? Blocks.WATER.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * And the dry way, down from behind the altar.
	 *
	 * A proper spiral rather than a ladder. The player should be walking for
	 * long enough to stop believing it is a cellar.
	 */
	/**
	 * Cut from the foot of a shaft to the middle of the chamber.
	 *
	 * Walked as a straight line rather than pathfound, and it simply stops as soon
	 * as it is standing in cave air — so on a bearing where the rim is generous it
	 * cuts almost nothing, and on one where the wall has closed in it cuts
	 * however far it has to. Either way the stair arrives somewhere.
	 */
	private static void join(ServerLevel level, BlockPos from, BlockPos floor) {
		int steps = (int)Math.ceil(Math.sqrt(from.distSqr(floor)));
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 1.0 : (double)i / steps;
			int x = (int)Math.round(from.getX() + (floor.getX() - from.getX()) * t);
			int z = (int)Math.round(from.getZ() + (floor.getZ() - from.getZ()) * t);
			BlockPos at = new BlockPos(x, floor.getY(), z);
			// Already inside the room, and there is nothing left to cut.
			if (i > 2 && level.getBlockState(at.above()).is(Blocks.CAVE_AIR)) {
				return;
			}
			for (int wide = -1; wide <= 1; wide++) {
				for (int up = 0; up <= 3; up++) {
					level.setBlock(at.offset(wide, up, 0), Blocks.CAVE_AIR.defaultBlockState(), 2);
					level.setBlock(at.offset(0, up, wide), Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
			level.setBlock(at.below(), paving(level.getRandom()), 2);
		}
	}

	private static void cryptStair(ServerLevel level, BlockPos crypt, BlockPos floor) {
		// A TIGHT SPIRAL IN A SHAFT, not a staircase that wanders.
		//
		// The first version walked three blocks along its heading per level and
		// only turned every fourth one, which over twenty-six levels of descent
		// carries it something like seventy blocks sideways — straight out from
		// under the church, out past the wall, and up through the hillside
		// outside the town. The "secret way in" came out in a field.
		//
		// Wound round a five-by-five shaft instead, so it descends where it
		// starts and the entrance stays inside the building it belongs to. The
		// player is still walking for the best part of a minute, which is the
		// only thing the length was ever for.
		int[][] ring = { {1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3},
			{2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0} };

		BlockPos head = crypt.offset(-1, 0, -1);
		int step = 0;
		BlockPos at = crypt;

		for (int y = crypt.getY(); y > floor.getY(); y--) {
			int[] cell = ring[step % ring.length];
			at = new BlockPos(head.getX() + cell[0], y, head.getZ() + cell[1]);
			step++;

			// FOUR high, not three. The old passage was cut nought to two and
			// then hung a lantern at two, which is the block a player's head
			// occupies — so the light itself was the thing blocking the way
			// down. Cutting one higher gives them two clear blocks to stand in
			// and a ceiling to hang from.
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(at.above(dy), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
			level.setBlock(at.below(), Blocks.STONE_BRICKS.defaultBlockState(), 2);

			if (step % 5 == 0) {
				level.setBlock(at.above(3), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}

		// Break out into the chamber wherever the shaft bottoms out.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlock(at.offset(dx, dy, dz),
						Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The library, and it is the largest building down here.
	 *
	 * More books than the entire surface town owns, in a settlement with no
	 * school. That contrast is the only thing this room has to say and it says
	 * it without a single sign.
	 */
	private static void library(ServerLevel level, BlockPos at, RandomSource random) {
		int w = 11;
		int d = 9;
		int h = 6;

		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				for (int dy = 0; dy <= h; dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
					boolean roof = dy == h;

					if (roof) {
						level.setBlock(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
					} else if (wall && dy > 0) {
						level.setBlock(pos, random.nextInt(5) == 0
							? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState(), 2);
					} else if (dy == 0) {
						level.setBlock(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
					} else {
						level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}

		// The doorway.
		for (int dy = 1; dy <= 2; dy++) {
			level.setBlock(at.offset(w / 2, dy, d - 1), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}

		// Shelves up both long walls, floor to ceiling, and an island in the
		// middle. Stacked rather than a single course — a wall one bookshelf
		// high is a bookcase, and four high is a library.
		for (int dz = 1; dz < d - 1; dz++) {
			for (int dy = 1; dy <= 4; dy++) {
				level.setBlock(at.offset(1, dy, dz), shelf(random), 2);
				level.setBlock(at.offset(w - 2, dy, dz), shelf(random), 2);
			}
		}
		for (int dx = 4; dx <= 6; dx++) {
			for (int dy = 1; dy <= 3; dy++) {
				level.setBlock(at.offset(dx, dy, 3), shelf(random), 2);
				level.setBlock(at.offset(dx, dy, 5), shelf(random), 2);
			}
		}

		// A reading desk, a lectern with something open on it, and light.
		level.setBlock(at.offset(w / 2, 1, 1), Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		level.setBlock(at.offset(3, 1, 7), Blocks.DARK_OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		level.setBlock(at.offset(7, 1, 7), Blocks.DARK_OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		for (int dx : new int[] { 3, 7 }) {
			level.setBlock(at.offset(dx, 5, 4), Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
	}

	/** One in six is a chiselled shelf, so the wall is not a flat texture. */
	private static BlockState shelf(RandomSource random) {
		return random.nextInt(6) == 0
			? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
			: Blocks.BOOKSHELF.defaultBlockState();
	}

	/** A small stone house, of which there are five. */
	private static void dwelling(ServerLevel level, BlockPos at, RandomSource random) {
		int w = 7;
		int d = 6;
		int h = 4;

		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				for (int dy = 0; dy <= h; dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
					if (dy == h) {
						level.setBlock(pos, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
							.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), 2);
					} else if (wall && dy > 0) {
						level.setBlock(pos, random.nextInt(4) == 0
							? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState(), 2);
					} else if (dy == 0) {
						level.setBlock(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
					} else {
						level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		for (int dy = 1; dy <= 2; dy++) {
			level.setBlock(at.offset(w / 2, dy, d - 1), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}
		level.setBlock(at.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
		level.setBlock(at.offset(w - 2, 1, 1), Blocks.BARREL.defaultBlockState(), 2);
		level.setBlock(at.offset(2, 1, d - 2), Blocks.BOOKSHELF.defaultBlockState(), 2);
		level.setBlock(at.offset(w / 2, 3, d / 2), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
		// A trapdoor over each window hole, shut, which is the town's own idiom.
		level.setBlock(at.offset(0, 2, d / 2), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
			.setValue(BlockStateProperties.HALF, Half.BOTTOM), 2);
	}

	/**
	 * And people, still living there.
	 *
	 * Ordinary villagers, persistent, unmodified. Nothing is wrong with them
	 * and nothing is supposed to be — the unsettling part is that they are fine
	 * and they are forty blocks under a town that does not mention them.
	 */
	private static final net.minecraft.resources.ResourceKey<
		net.minecraft.world.entity.npc.villager.VillagerProfession>[] TRADES =
			new net.minecraft.resources.ResourceKey[] {
				net.minecraft.world.entity.npc.villager.VillagerProfession.FARMER,
				net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN,
				net.minecraft.world.entity.npc.villager.VillagerProfession.CLERIC,
				net.minecraft.world.entity.npc.villager.VillagerProfession.MASON,
				net.minecraft.world.entity.npc.villager.VillagerProfession.BUTCHER,
				net.minecraft.world.entity.npc.villager.VillagerProfession.SHEPHERD,
			};

	private static void people(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int i = 0; i < 9; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 5.0 + random.nextDouble() * (SPAN - 8);
			int x = floor.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = floor.getZ() + (int)Math.round(Math.sin(angle) * range);

			Mob villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
			if (villager == null) {
				continue;
			}
			villager.snapTo(x + 0.5, floor.getY(), z + 0.5,
				random.nextFloat() * 360.0F, 0.0F);
			villager.setPersistenceRequired();
			// THE SAME TRADES AS THE TOWN ABOVE, and that is the whole point.
			//
			// Bare villagers down here would read as a spawner: no trades, no
			// professions, nothing to say when clicked. Giving them the SAME jobs
			// as the people upstairs says something much worse than anything a
			// sign could — this is not a dungeon full of prisoners, it is the town,
			// again, underneath the town, with the same butcher and the same
			// librarian in it. Nobody explains why and nobody has to.
			if (villager instanceof net.minecraft.world.entity.npc.villager.Villager who) {
				who.setVillagerData(who.getVillagerData().withProfession(
					level.registryAccess(), TRADES[random.nextInt(TRADES.length)]));
			}
			level.addFreshEntity(villager);
		}
	}

	private static BlockState paving(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState vaulting(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 5) {
			return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		}
		if (roll < 8) {
			return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
		}
		return Blocks.DEEPSLATE_TILES.defaultBlockState();
	}
}
