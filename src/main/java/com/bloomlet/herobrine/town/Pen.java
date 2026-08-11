package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.structure.Ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Animals, a trough, and a shelter with one open side.
 *
 * The cheapest building in the town and probably the most important, because it
 * is the one that puts LIVING THINGS in the streets. Eight houses and a hall
 * with nobody in them is a film set; the same place with a dozen sheep making
 * noise behind a fence is somewhere people are.
 *
 * TERRAIN-FOLLOWING RATHER THAN LEVELLED, and this is the one building where
 * that is right. Every other plot gets a footing so its floor is flat, because
 * a house with a sloping floor is a bug. A paddock with a sloping floor is a
 * paddock. Cutting a twelve-by-twelve shelf out of a hillside to hold four cows
 * would be the single most obviously artificial thing in the settlement, so the
 * fence just walks down the hill and the shelter sits wherever is flattest.
 *
 * The animals are real and they are placed by hand rather than left to natural
 * spawning, because natural spawning would give you a field of nothing until
 * the player happened to stand in it, and by then the town has already failed
 * to convince them.
 */
public final class Pen {
	private Pen() {}

	public static final int WIDTH = 12;
	public static final int DEPTH = 12;

	/** What lives here. One kind per pen — a mixed paddock reads as a zoo. */
	private enum Stock {
		SHEEP(EntityTypes.SHEEP, 7),
		COW(EntityTypes.COW, 5),
		PIG(EntityTypes.PIG, 6),
		CHICKEN(EntityTypes.CHICKEN, 8);

		final EntityType<? extends Mob> type;
		final int head;

		Stock(EntityType<? extends Mob> type, int head) {
			this.type = type;
			this.head = head;
		}
	}

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		Stock stock = Stock.values()[random.nextInt(Stock.values().length)];

		fence(level, corner, facing);
		shelter(level, corner, facing, random, stock);
		trough(level, corner, facing);
		stockIt(level, corner, random, stock);
		return true;
	}

	/**
	 * The fence, following the ground rather than a line.
	 *
	 * Each post is placed on whatever the surface is at that column, so the
	 * rail runs down a slope the way a real one does. A gate on the lane side,
	 * because a pen with no way in is scenery.
	 */
	private static void fence(ServerLevel level, BlockPos corner, Direction facing) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				boolean edge = x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1;
				if (!edge) {
					continue;
				}
				int wx = corner.getX() + Blueprint.spinX(x, z, WIDTH, DEPTH, facing);
				int wz = corner.getZ() + Blueprint.spinZ(x, z, WIDTH, DEPTH, facing);
				BlockPos ground = new BlockPos(wx, Ground.topOf(level, wx, wz) + 1, wz);

				// The gateway, dead centre of the side that faces the lane.
				boolean gateway = z == DEPTH - 1 && (x == WIDTH / 2 || x == WIDTH / 2 - 1);
				if (gateway) {
					Blueprint.put(level, ground, Blocks.SPRUCE_FENCE_GATE.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING,
							Blueprint.turned(Direction.SOUTH, facing)));
					continue;
				}
				Blueprint.put(level, ground, Blocks.SPRUCE_FENCE.defaultBlockState());
				// Corner posts stand a course taller, which is what stops a
				// long fence reading as a single extruded line.
				boolean post = (x == 0 || x == WIDTH - 1) && (z == 0 || z == DEPTH - 1);
				if (post) {
					Blueprint.put(level, ground.above(),
						Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
							.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
				}
			}
		}
	}

	/**
	 * Three walls and a roof, open to the yard.
	 *
	 * Deliberately not a barn. A closed building would need a door, a door
	 * needs a floor, and a floor needs the ground levelling — which is exactly
	 * the thing this plot is built to avoid. An open-fronted field shelter can
	 * stand on a slope and still look like somebody put it there on purpose.
	 */
	private static void shelter(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random, Stock stock) {
		int ox = 2;
		int oz = 2;
		int w = 5;
		int d = 4;

		int floor = Integer.MAX_VALUE;
		for (int x = ox; x < ox + w; x++) {
			for (int z = oz; z < oz + d; z++) {
				int wx = corner.getX() + Blueprint.spinX(x, z, WIDTH, DEPTH, facing);
				int wz = corner.getZ() + Blueprint.spinZ(x, z, WIDTH, DEPTH, facing);
				floor = Math.min(floor, Ground.topOf(level, wx, wz) + 1);
			}
		}

		for (int x = ox; x < ox + w; x++) {
			for (int z = oz; z < oz + d; z++) {
				int wx = corner.getX() + Blueprint.spinX(x, z, WIDTH, DEPTH, facing);
				int wz = corner.getZ() + Blueprint.spinZ(x, z, WIDTH, DEPTH, facing);
				boolean back = z == oz;
				boolean side = x == ox || x == ox + w - 1;

				for (int dy = 0; dy <= 3; dy++) {
					BlockPos at = new BlockPos(wx, floor + dy, wz);
					if (dy == 3) {
						Blueprint.put(level, at, Blocks.SPRUCE_SLAB.defaultBlockState());
					} else if ((back || side) && dy > 0) {
						Blueprint.put(level, at, random.nextInt(4) == 0
							? Blocks.HAY_BLOCK.defaultBlockState()
							: Blocks.SPRUCE_PLANKS.defaultBlockState());
					} else if (dy == 0) {
						// A STRIP OF WORKED GROUND, because the farmers had none.
						//
						// They were given the profession and nothing to do with it:
						// a farmer with no farmland will not till, will not plant,
						// will not harvest and will not restock, so the whole trade
						// was decoration. Two rows of crops fixes it, and a village
						// with somebody actually working in it is worth more than
						// any amount of building detail.
						//
						// Deliberately half grown and patchy. A field at full
						// harvest reads as a farm somebody is tending well, and this
						// is a town in a mod about somebody who is not coming back.
						boolean field = z > oz + 1 && !side && random.nextInt(3) != 0;
						if (field) {
							Blueprint.put(level, at, Blocks.FARMLAND.defaultBlockState()
								.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE, 7));
							Blueprint.put(level, at.above(), random.nextBoolean()
								? Blocks.WHEAT.defaultBlockState().setValue(
									net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7,
									random.nextInt(8))
								: Blocks.CARROTS.defaultBlockState().setValue(
									net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7,
									random.nextInt(8)));
						} else {
							Blueprint.put(level, at, Blocks.DIRT_PATH.defaultBlockState());
						}
					} else {
						Blueprint.put(level, at, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}

		// Bedding, and the smell of the place.
		Blueprint.put(level, new BlockPos(
			corner.getX() + Blueprint.spinX(ox + 1, oz + 1, WIDTH, DEPTH, facing),
			floor, corner.getZ() + Blueprint.spinZ(ox + 1, oz + 1, WIDTH, DEPTH, facing)),
			Blocks.HAY_BLOCK.defaultBlockState());
		Blueprint.put(level, new BlockPos(
			corner.getX() + Blueprint.spinX(ox + 3, oz + 1, WIDTH, DEPTH, facing),
			floor, corner.getZ() + Blueprint.spinZ(ox + 3, oz + 1, WIDTH, DEPTH, facing)),
			stock == Stock.CHICKEN
				? Blocks.HAY_BLOCK.defaultBlockState()
				: Blocks.COMPOSTER.defaultBlockState());
	}

	/** Water, because animals drink and because it catches the light. */
	private static void trough(ServerLevel level, BlockPos corner, Direction facing) {
		for (int i = 0; i < 2; i++) {
			int x = 8;
			int z = 7 + i;
			int wx = corner.getX() + Blueprint.spinX(x, z, WIDTH, DEPTH, facing);
			int wz = corner.getZ() + Blueprint.spinZ(x, z, WIDTH, DEPTH, facing);
			BlockPos at = new BlockPos(wx, Ground.topOf(level, wx, wz) + 1, wz);
			Blueprint.put(level, at, Blocks.WATER_CAULDRON.defaultBlockState()
				.setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
		}
	}

	/**
	 * Put the animals in.
	 *
	 * setPersistenceRequired, or the whole point of the pen walks out of it the
	 * first time the chunk unloads with nobody watching — and a paddock the
	 * player finds empty on their second visit is worse than never having built
	 * one.
	 */
	private static void stockIt(ServerLevel level, BlockPos corner, RandomSource random,
	                            Stock stock) {
		for (int i = 0; i < stock.head; i++) {
			int x = corner.getX() + 2 + random.nextInt(WIDTH - 4);
			int z = corner.getZ() + 2 + random.nextInt(DEPTH - 4);
			int y = Ground.topOf(level, x, z) + 1;

			Mob beast = stock.type.create(level, EntitySpawnReason.STRUCTURE);
			if (beast == null) {
				continue;
			}
			beast.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			beast.setPersistenceRequired();
			level.addFreshEntity(beast);
		}
	}
}
