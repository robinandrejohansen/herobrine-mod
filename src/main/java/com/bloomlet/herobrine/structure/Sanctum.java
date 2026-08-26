package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * THE DEEPEST ROOM UNDER HIS HOUSE, AND THE DOOR IS IN IT.
 *
 * The way out used to stand on the tower deck, twenty-nine blocks up, on the
 * skyline, visible from four hundred blocks. Which is a fine image and the wrong
 * place: it made the ending the most public thing on the map, and it meant the
 * portal had to be sited wherever the tower could be stood — on a coast, in a
 * crag, sometimes not at all.
 *
 * Under the house it cannot fail. The passage already goes there, the deepest
 * waypoint of it is a position we chose rather than found, and every world gets
 * the same room in the same relationship to the same building.
 *
 * AND IT IS THE LAST THING BUILT. The undercroft, the shaft, the passage and the
 * tower all get to put their blocks down first and be wherever the terrain made
 * them; this is carved into whatever is left. Nothing can be placed on top of it
 * afterwards, which is the only way to guarantee a portal is not half inside
 * somebody else's tunnel.
 *
 * THE TOWER KEEPS ITS SILHOUETTE and loses its job. It is still the thing you see
 * from off his land and still the reason to walk over — it simply turns out to be
 * a marker rather than a door, and what it marks is under it.
 */
public final class Sanctum {
	private Sanctum() {}

	/** Thirteen across and nine high. Big enough that the light does not reach. */
	private static final int HALF = 6;
	private static final int TALL = 8;
	/** The dais, three steps up out of the floor. */
	private static final int DAIS = 3;

	public static BlockPos raise(ServerLevel level, BlockPos middle, RandomSource random) {
		BlockPos floor = new BlockPos(middle.getX(),
			Math.max(level.getMinY() + 6, middle.getY() - 1), middle.getZ());

		hollow(level, floor, random);
		pillars(level, floor, random);
		dais(level, floor, random);
		growth(level, floor, random);

		BlockPos gate = floor.above(DAIS + 1);
		TheWay.open(level, gate);
		HerobrineMod.LOGGER.info("the way is under his house at [{}, {}, {}]",
			gate.getX(), gate.getY(), gate.getZ());
		return gate;
	}

	/** The room itself, walls and vaulted-ish ceiling, all of it old. */
	private static void hollow(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
			for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
				for (int dy = -1; dy <= TALL + 1; dy++) {
					BlockPos at = floor.offset(dx, dy, dz);
					boolean shell = Math.abs(dx) > HALF || Math.abs(dz) > HALF
						|| dy < 0 || dy > TALL;
					if (shell) {
						level.setBlock(at, aged(random), 2);
					} else {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		// A patterned floor, laid in world coordinates so the courses run true no
		// matter which way the room ended up facing.
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				BlockPos at = floor.offset(dx, -1, dz);
				boolean light = ((at.getX() >> 1) + (at.getZ() >> 1)) % 2 == 0;
				level.setBlock(at, light
					? Blocks.POLISHED_ANDESITE.defaultBlockState()
					: aged(random), 2);
			}
		}
	}

	/** Four pillars with flared feet and heads, and a beamed ceiling between. */
	private static void pillars(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int x = sx * (HALF - 1);
				int z = sz * (HALF - 1);
				for (int up = 0; up <= TALL; up++) {
					level.setBlock(floor.offset(x, up, z),
						up == 0 || up == TALL ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
							: aged(random), 2);
				}
				flare(level, floor.offset(x, 1, z), random);
				flare(level, floor.offset(x, TALL - 1, z), random);
				// A lantern hung off each pillar head, which is all the light there is.
				level.setBlock(floor.offset(x, TALL - 2, z + (sz > 0 ? -1 : 1)),
					Blocks.SOUL_LANTERN.defaultBlockState()
						.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
		// Beams across the ceiling, so it is a room and not a hole.
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int z : new int[] { -HALF + 2, 0, HALF - 2 }) {
				level.setBlock(floor.offset(dx, TALL, z),
					Blocks.DARK_OAK_WOOD.defaultBlockState(), 2);
			}
		}
	}

	/** Stairs turned outward all round, which is what makes a pillar a pillar. */
	private static void flare(ServerLevel level, BlockPos at, RandomSource random) {
		for (Direction way : Direction.Plane.HORIZONTAL) {
			BlockPos side = at.relative(way);
			if (!level.getBlockState(side).isAir()) {
				continue;
			}
			level.setBlock(side, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, way.getOpposite())
				.setValue(BlockStateProperties.HALF,
					at.getY() % 2 == 0 ? Half.TOP : Half.BOTTOM), 2);
		}
	}

	/** Three steps up to it, so the door is raised out of its own room. */
	private static void dais(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int step = 0; step < DAIS; step++) {
			int reach = 4 - step;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					BlockPos at = floor.offset(dx, step, dz);
					boolean rim = Math.abs(dx) == reach || Math.abs(dz) == reach;
					if (rim) {
						level.setBlock(at, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
							.setValue(BlockStateProperties.HORIZONTAL_FACING,
								Math.abs(dx) >= Math.abs(dz)
									? (dx > 0 ? Direction.EAST : Direction.WEST)
									: (dz > 0 ? Direction.SOUTH : Direction.NORTH)), 2);
					} else {
						level.setBlock(at, aged(random), 2);
					}
				}
			}
		}
		// Candles round the top of it. Nobody lit these recently.
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				level.setBlock(floor.offset(sx * 2, DAIS, sz * 2),
					Blocks.CANDLE.defaultBlockState()
						.setValue(BlockStateProperties.LIT, true)
						.setValue(BlockStateProperties.CANDLES, 1 + random.nextInt(3)), 2);
			}
		}
	}

	/**
	 * AND IT HAS BEEN DOWN HERE A LONG TIME.
	 *
	 * Moss on the floor, moss hanging off the beams, vines down the walls and glow
	 * lichen where the lanterns do not reach. Skulls set INTO the wall rather than
	 * stuck on it — a head on a spike is a decoration, a head in the masonry is
	 * something the masonry was built around.
	 */
	private static void growth(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				BlockPos on = floor.offset(dx, 0, dz);
				if (level.getBlockState(on).isAir()
					&& level.getBlockState(on.below()).isSolid()
					&& random.nextInt(4) == 0) {
					level.setBlock(on, Blocks.PALE_MOSS_CARPET.defaultBlockState(), 2);
				}
				BlockPos under = floor.offset(dx, TALL - 1, dz);
				if (level.getBlockState(under).isAir()
					&& level.getBlockState(under.above()).isSolid()
					&& random.nextInt(5) == 0) {
					level.setBlock(under, Blocks.PALE_HANGING_MOSS.defaultBlockState(), 2);
				}
			}
		}
		for (int up = 1; up < TALL; up++) {
			for (int side = -1; side <= 1; side += 2) {
				vine(level, floor.offset(side * HALF, up, random.nextInt(HALF * 2 + 1) - HALF),
					side > 0 ? Direction.WEST : Direction.EAST, random);
				vine(level, floor.offset(random.nextInt(HALF * 2 + 1) - HALF, up, side * HALF),
					side > 0 ? Direction.NORTH : Direction.SOUTH, random);
			}
		}
		// Four of them, in the wall, at head height.
		for (int i = 0; i < 4; i++) {
			int dx = random.nextInt(HALF * 2 - 1) - HALF + 1;
			BlockPos niche = floor.offset(dx, 2 + random.nextInt(3),
				random.nextBoolean() ? HALF : -HALF);
			level.setBlock(niche, random.nextBoolean()
				? Blocks.SKELETON_SKULL.defaultBlockState()
				: Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 2);
		}
	}

	private static void vine(ServerLevel level, BlockPos at, Direction face,
	                         RandomSource random) {
		if (random.nextInt(3) == 0 || !level.getBlockState(at).isAir()) {
			return;
		}
		level.setBlock(at, random.nextBoolean()
			? Blocks.GLOW_LICHEN.defaultBlockState().setValue(
				net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(face), true)
			: Blocks.VINE.defaultBlockState().setValue(
				net.minecraft.world.level.block.VineBlock.PROPERTY_BY_DIRECTION.get(face),
				true), 2);
	}

	/** Failing for a long time, and neither of you built it. */
	private static BlockState aged(RandomSource random) {
		return switch (random.nextInt(10)) {
			case 0, 1, 2, 3 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			case 4, 5 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			case 6 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			case 7 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			default -> Blocks.STONE_BRICKS.defaultBlockState();
		};
	}
}
