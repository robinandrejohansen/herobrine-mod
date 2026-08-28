package com.bloomlet.herobrine.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * A HOUSE SOMEBODY LIVED IN, RATHER THAN A BOX WITH A DOOR.
 *
 * The old one was seven by seven, four high, walls of one material and a lid of
 * dark oak slabs. Thirty of them in a street read as a texture rather than as a
 * town, and the reason is not detail — it is that every one of them had the same
 * silhouette. A row of identical boxes is a car park.
 *
 * FOUR THINGS DO ALL THE WORK, and they are all shape rather than decoration:
 *
 *   A PITCHED ROOF. The single change that matters most. A flat lid reads as
 *   unfinished at any level of trim; a gable reads as a house even in one
 *   material, because the outline is the thing the eye recognises from two
 *   hundred blocks away. It also gives an attic, which is where the dormer goes.
 *
 *   A CHIMNEY, and it is brick where everything else is stone. One warm colour
 *   in a cold palette, standing above the ridge line — it is what makes the
 *   silhouette legible against a dark forest, and it says fire, which says
 *   somebody.
 *
 *   FOUR STONES, NOT ONE. Cobble, mossy cobble, andesite and stone brick, mixed
 *   per block with the mossy weighted toward the bottom courses. Real walls are
 *   damp at the foot and dry at the head, and a wall that is uniformly speckled
 *   looks like noise while a wall that is dirtier at the bottom looks old.
 *
 *   AND IT SITS IN SOMETHING. Terraced ground, a shovelled path to the door,
 *   flowers in the grass, a log pile against the gable. A perfect building on
 *   flat grass is a model; the same building with a worn path up to it is a
 *   place. This is the half people mean when they say a build looks "lived in",
 *   and it is nearly all outside the walls.
 *
 * NO TWO ARE THE SAME AND NONE OF THEM ARE RANDOM. Everything is drawn from a
 * seeded RandomSource keyed on the plot, so a given house is identical every
 * time the chunk loads, and the street has variety without anything flickering.
 */
public final class Cottage {
	private Cottage() {}

	/** Half-width and half-depth ranges. The roof pitch is derived from these. */
	private static final int NARROW = 3;
	private static final int WIDE = 4;
	/** Height of the wall to the eaves. */
	private static final int EAVES = 4;
	/** How far the ground may fall across the footprint before the plot is refused. */
	private static final int TOLERATES = 3;

	/**
	 * @param facing the way the front looks — the street, so a row all agree
	 * @return true if it was built
	 */
	public static boolean raise(ServerLevel level, BlockPos plot, Direction facing,
	                            long seed) {
		RandomSource random = RandomSource.create(seed);
		int w = NARROW + random.nextInt(WIDE - NARROW + 1);
		int d = NARROW + random.nextInt(WIDE - NARROW + 1);

		if (!level.isLoaded(plot.atY(level.getSeaLevel()))) {
			return false;
		}
		// LEVEL ENOUGH, AND DRY. topOf reports the seabed under a lake perfectly
		// happily, so without the dry test a plot over water passes every
		// flatness check there is and the house goes up on the bottom of it.
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int dx = -w; dx <= w; dx += w) {
			for (int dz = -d; dz <= d; dz += d) {
				if (!Ground.dry(level, plot.getX() + dx, plot.getZ() + dz)) {
					return false;
				}
				int y = Ground.topOf(level, plot.getX() + dx, plot.getZ() + dz);
				low = Math.min(low, y);
				high = Math.max(high, y);
			}
		}
		if (high - low > TOLERATES || low <= level.getMinY() + 8) {
			return false;
		}
		BlockPos base = new BlockPos(plot.getX(), low + 1, plot.getZ());

		// THE RIDGE RUNS ACROSS THE FRONT, always, whichever way the plot came out
		// longer. A gable end facing the street is the shape everybody draws when
		// they draw a house, and having half the row show a long flank instead
		// would read as two different towns.
		boolean ridgeAlongZ = facing.getAxis() == Direction.Axis.X;
		int pitch = ridgeAlongZ ? w : d;

		footing(level, base, w, d, random);
		walls(level, base, w, d, random);
		roof(level, base, w, d, pitch, ridgeAlongZ, random);
		chimney(level, base, w, d, pitch, random);
		front(level, base, w, d, facing, random);
		windows(level, base, w, d, facing, random);
		inside(level, base, w, d, facing, random);
		around(level, base, w, d, facing, random);
		return true;
	}

	// ---- THE FOUR STONES ---------------------------------------------------
	/**
	 * @param damp how strongly the mossy end of the palette is favoured, 0 to 1
	 *
	 * Weighted rather than uniform, and the weight is what stops it reading as
	 * static. An even quarter-share of four materials is visual noise: the eye
	 * finds no pattern and files the whole wall as texture. Two thirds cobble with
	 * the rest scattered through it reads as a cobble wall with history in it.
	 */
	private static BlockState stone(RandomSource random, double damp) {
		if (random.nextDouble() < damp * 0.55) {
			return random.nextInt(3) == 0
				? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
				: Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		return switch (random.nextInt(8)) {
			case 0, 1, 2, 3 -> Blocks.COBBLESTONE.defaultBlockState();
			case 4, 5 -> Blocks.ANDESITE.defaultBlockState();
			case 6 -> Blocks.STONE_BRICKS.defaultBlockState();
			default -> Blocks.STONE.defaultBlockState();
		};
	}

	// ---- BELOW THE FLOOR ---------------------------------------------------
	/**
	 * Down to solid ground, and the floor above it.
	 *
	 * THE PLINTH IS WHY IT DOES NOT LOOK DROPPED. A house whose walls begin at the
	 * grass line looks placed on the world; the same house standing one course
	 * proud, with the ground banked against it, looks dug into it. One block of
	 * height, and it is the cheapest realism in the whole method.
	 */
	private static void footing(ServerLevel level, BlockPos base, int w, int d,
	                            RandomSource random) {
		for (int dx = -w - 1; dx <= w + 1; dx++) {
			for (int dz = -d - 1; dz <= d + 1; dz++) {
				boolean plinth = Math.abs(dx) == w + 1 || Math.abs(dz) == d + 1;
				BlockPos top = base.offset(dx, plinth ? -1 : 0, dz);
				// Down to whatever is under it, so nothing is left on stilts.
				for (int dy = 0; dy < 6; dy++) {
					BlockPos at = top.below(dy);
					if (dy > 0 && level.getBlockState(at).isSolid()) {
						break;
					}
					put(level, at, dy == 0 && plinth
						? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
						: stone(random, 0.8));
				}
				if (!plinth) {
					// The floor inside: boards, because a stone shell with a
					// timber floor is how these were actually built and the two
					// materials meeting at the skirting is half the interior.
					put(level, top, random.nextInt(6) == 0
						? Blocks.SPRUCE_SLAB.defaultBlockState()
							.setValue(BlockStateProperties.SLAB_TYPE,
								net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)
						: Blocks.SPRUCE_PLANKS.defaultBlockState());
				}
			}
		}
	}

	// ---- THE WALLS ---------------------------------------------------------
	private static void walls(ServerLevel level, BlockPos base, int w, int d,
	                          RandomSource random) {
		for (int dx = -w; dx <= w; dx++) {
			for (int dz = -d; dz <= d; dz++) {
				boolean edge = Math.abs(dx) == w || Math.abs(dz) == d;
				boolean corner = Math.abs(dx) == w && Math.abs(dz) == d;
				for (int dy = 1; dy <= EAVES; dy++) {
					BlockPos at = base.offset(dx, dy, dz);
					if (!edge) {
						clear(level, at);
						continue;
					}
					if (dy == EAVES) {
						// THE BEAM COURSE. A band of stripped log all the way round
						// under the eaves, and it is the line that separates wall
						// from roof. Without it the stone runs straight into the
						// slate and the building has no waist.
						put(level, at, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
						continue;
					}
					// QUOINS. Dressed stone at the corners where the wall is
					// rubble — the one place a real builder spent money, and the
					// detail that makes the outline crisp from a distance.
					if (corner) {
						put(level, at, dy % 2 == 0
							? Blocks.STONE_BRICKS.defaultBlockState()
							: Blocks.POLISHED_ANDESITE.defaultBlockState());
						continue;
					}
					// Damp at the foot, dry at the head.
					put(level, at, stone(random, 1.0 - (dy - 1) / (double) EAVES));
				}
			}
		}
	}

	// ---- THE ROOF ----------------------------------------------------------
	/**
	 * A gable, with an overhang, and moss growing on the north of it.
	 *
	 * THE OVERHANG IS NOT TRIM. Stairs that stop flush with the wall make the roof
	 * look like a hat sitting on the house. One block proud, all the way round,
	 * puts the wall in shadow and is most of what reads as "a roof" rather than
	 * "a lid" — and it costs one extra course.
	 */
	private static void roof(ServerLevel level, BlockPos base, int w, int d,
	                         int pitch, boolean ridgeAlongZ, RandomSource random) {
		for (int step = 0; step <= pitch; step++) {
			int y = EAVES + step;
			int out = pitch - step + 1;      // +1 for the overhang
			int along = (ridgeAlongZ ? d : w) + 1;
			for (int side = -1; side <= 1; side += 2) {
				for (int run = -along; run <= along; run++) {
					int dx = ridgeAlongZ ? side * out : run;
					int dz = ridgeAlongZ ? run : side * out;
					BlockPos at = base.offset(dx, y, dz);
					if (step == pitch && side == 1) {
						continue;      // the ridge is laid once, below
					}
					Direction face = ridgeAlongZ
						? (side < 0 ? Direction.WEST : Direction.EAST)
						: (side < 0 ? Direction.NORTH : Direction.SOUTH);
					put(level, at, stairs(random, face));
					// MOSS ON THE ROOF, in patches rather than everywhere. It is
					// the difference between a slate roof and a slate roof that
					// has been rained on for thirty years.
					if (random.nextInt(7) == 0) {
						put(level, at.above(), Blocks.MOSS_CARPET.defaultBlockState());
					}
				}
			}
			// Close the gable ends with wall material, so the attic is not open
			// to the street.
			for (int run = -out + 1; run <= out - 1; run++) {
				for (int end = -1; end <= 1; end += 2) {
					int dx = ridgeAlongZ ? run : end * ((ridgeAlongZ ? d : w));
					int dz = ridgeAlongZ ? end * d : run;
					if (ridgeAlongZ) {
						dx = run;
						dz = end * d;
					} else {
						dx = end * w;
						dz = run;
					}
					put(level, base.offset(dx, y, dz), stone(random, 0.25));
				}
			}
		}
		// The ridge itself, one slab wide, which stops the two pitches meeting in
		// a seam of stair backs.
		int ridge = EAVES + pitch;
		int along = (ridgeAlongZ ? d : w) + 1;
		for (int run = -along; run <= along; run++) {
			int dx = ridgeAlongZ ? 0 : run;
			int dz = ridgeAlongZ ? run : 0;
			put(level, base.offset(dx, ridge, dz),
				Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
		}
	}

	private static BlockState stairs(RandomSource random, Direction facing) {
		BlockState pick = switch (random.nextInt(8)) {
			case 0, 1, 2, 3, 4 -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();
			case 5, 6 -> Blocks.COBBLED_DEEPSLATE_STAIRS.defaultBlockState();
			default -> Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
		};
		return pick.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	// ---- THE CHIMNEY -------------------------------------------------------
	private static void chimney(ServerLevel level, BlockPos base, int w, int d,
	                            int pitch, RandomSource random) {
		// Against a gable end rather than in the middle of the roof, because a
		// stack that pierces the slope needs flashing to look right and a stack
		// running up the outside of a wall needs nothing at all.
		int dx = random.nextBoolean() ? w : -w;
		int dz = random.nextInt(2 * d - 1) - (d - 1);
		int top = EAVES + pitch + 2 + random.nextInt(2);
		for (int dy = 1; dy <= top; dy++) {
			put(level, base.offset(dx, dy, dz), random.nextInt(11) == 0
				? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
				: Blocks.BRICKS.defaultBlockState());
		}
		// A lip at the head of it, and the fire below.
		put(level, base.offset(dx, top + 1, dz),
			Blocks.BRICK_SLAB.defaultBlockState());
		put(level, base.offset(dx, 1, dz), Blocks.CAMPFIRE.defaultBlockState()
			.setValue(BlockStateProperties.LIT, true));
	}

	// ---- THE FRONT ---------------------------------------------------------
	private static void front(ServerLevel level, BlockPos base, int w, int d,
	                          Direction facing, RandomSource random) {
		int reach = facing.getAxis() == Direction.Axis.X ? w : d;
		BlockPos door = base.relative(facing, reach).above();
		put(level, door, Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
		put(level, door.above(), Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
		// A lintel over it, and a slab hood above that with the lanterns hung
		// under it — which is how you get a wall lantern in a game that has no
		// wall lanterns.
		put(level, door.above(2), Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
		Direction across = facing.getClockWise();
		for (int side = -1; side <= 1; side += 2) {
			BlockPos bracket = door.above(2).relative(across, side);
			put(level, bracket.relative(facing),
				Blocks.STONE_BRICK_SLAB.defaultBlockState());
			put(level, bracket.relative(facing).below(),
				Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true));
		}
		// AND A DORMER, on the front slope, which is the detail that makes the
		// attic read as a room somebody is in rather than a void under a roof.
		if (random.nextInt(3) != 0) {
			BlockPos eye = base.relative(facing, reach).above(EAVES + 1);
			put(level, eye, Blocks.GLASS.defaultBlockState());
			put(level, eye.above(), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HALF, Half.TOP)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
		}
	}

	// ---- THE WINDOWS -------------------------------------------------------
	/**
	 * Panes with shutters, and half the shutters are open.
	 *
	 * OPEN AND SHUT IN THE SAME STREET is the whole point of them. Every shutter
	 * closed is a boarded-up town; every one open is a showroom. Mixed, per
	 * window, is somebody's morning.
	 */
	private static void windows(ServerLevel level, BlockPos base, int w, int d,
	                            Direction facing, RandomSource random) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int reach = side.getAxis() == Direction.Axis.X ? w : d;
			int span = side.getAxis() == Direction.Axis.X ? d : w;
			Direction across = side.getClockWise();
			for (int off = -span + 1; off <= span - 1; off++) {
				if (Math.abs(off) % 2 == 1 || (side == facing && off == 0)) {
					continue;      // every other bay, and never over the door
				}
				// PLAIN GLASS, NOT PANES, AND THAT IS A FIX RATHER THAN A CHOICE.
				//
				// A pane is not a block, it is a shape that decides what it looks
				// like from its four neighbours — and in a one-block hole in a
				// thick wall it connects to the stone on both sides and renders as
				// a cross with daylight down the corners. Put a trapdoor next to it
				// and it tries to connect to that as well. The result is a window
				// that is visibly not filled in, which is what "glitched" means
				// here: nothing is broken, the pane is doing exactly what a pane
				// does and it is the wrong thing for this opening.
				//
				// A full block has no neighbour logic at all. It fills the hole,
				// it is the same shape every time, and it is what every build in
				// the reference screenshots is actually using.
				BlockPos at = base.relative(side, reach).above(2).relative(across, off);
				put(level, at, Blocks.GLASS.defaultBlockState());
				put(level, at.below(), Blocks.GLASS.defaultBlockState());
				put(level, at.below(2).relative(side),
					Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
				// AND THE SHUTTERS SIT FLAT ON THE WALL.
				//
				// They used to be hung a block out in open air, where a trapdoor has
				// nothing to read as a hinge and floats. Against the face, opened
				// outward, they land where a real shutter is — on the wall, beside
				// the opening, folded back.
				boolean open = random.nextBoolean();
				for (int flank = -1; flank <= 1; flank += 2) {
					BlockPos hinge = at.relative(across, flank);
					if (!level.getBlockState(hinge).isSolid()) {
						continue;      // that bay is a corner or another window
					}
					put(level, hinge.relative(side),
						Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
							.setValue(BlockStateProperties.OPEN, open)
							.setValue(BlockStateProperties.HALF, Half.BOTTOM)
							.setValue(BlockStateProperties.HORIZONTAL_FACING,
								open ? across.getOpposite() : side.getOpposite()));
				}
			}
		}
	}

	// ---- WHAT IS IN IT -----------------------------------------------------
	/**
	 * Somebody's things, and the arrangement is the point rather than the loot.
	 *
	 * A chest on its own in an empty room is a loot container. The same chest with
	 * a table, a stool, a cauldron of water and a bed against the far wall is a
	 * room that has a chest in it, and the player reads the whole thing as
	 * belonging to a person before they have opened anything.
	 *
	 * THE BED IS SAFE TO PLACE. His world sets bed_rule explodes, which fires on
	 * SLEEPING, not on the block existing — so a made bed is the strongest
	 * "somebody lives here" signal available and also a trap, and neither of those
	 * is an accident.
	 */
	private static void inside(ServerLevel level, BlockPos base, int w, int d,
	                           Direction facing, RandomSource random) {
		Direction back = facing.getOpposite();
		Direction across = facing.getClockWise();
		int reach = back.getAxis() == Direction.Axis.X ? w : d;
		int span = back.getAxis() == Direction.Axis.X ? d : w;

		// TWO BEDS, BECAUSE TWO PEOPLE LIVE HERE.
		//
		// One bed and two of them standing in the room is a detail that actively
		// works against itself — the player counts the beds, counts the people, and
		// the house stops adding up. It is the same arithmetic that makes a set
		// table for four read as a family and a set table for one read as a prop.
		BlockPos head = base.relative(back, reach - 1).above()
			.relative(across, span - 1);
		put(level, head, Blocks.BED.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, back)
			.setValue(BlockStateProperties.BED_PART,
				net.minecraft.world.level.block.state.properties.BedPart.HEAD));
		put(level, head.relative(facing), Blocks.BED.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, back)
			.setValue(BlockStateProperties.BED_PART,
				net.minecraft.world.level.block.state.properties.BedPart.FOOT));

		// The other back corner: the working half of the room.
		BlockPos work = base.relative(back, reach - 1).above()
			.relative(across, -(span - 1));
		BlockState[] trade = {
			Blocks.CRAFTING_TABLE.defaultBlockState(),
			Blocks.LOOM.defaultBlockState(),
			Blocks.SMOKER.defaultBlockState(),
			Blocks.CARTOGRAPHY_TABLE.defaultBlockState(),
			Blocks.FLETCHING_TABLE.defaultBlockState(),
			Blocks.BARREL.defaultBlockState(),
		};
		put(level, work, trade[random.nextInt(trade.length)]);
		put(level, work.relative(facing), random.nextBoolean()
			? Blocks.CAULDRON.defaultBlockState()
			: Blocks.COMPOSTER.defaultBlockState());

		// A table in the middle with something on it, and a stool.
		BlockPos table = base.above();
		put(level, table, Blocks.SPRUCE_FENCE.defaultBlockState());
		put(level, table.above(), Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
		put(level, table.relative(across), Blocks.SPRUCE_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, across.getOpposite()));
		put(level, table.above(2), random.nextBoolean()
			? Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true)
			: Blocks.SOUL_LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true));

		// AND A CONTAINER WITH SOMETHING IN IT. Loot.scatter is the same call the
		// overworld town uses, so what is in a cupboard here reads as the same
		// kind of thing as what is in a cupboard there — which is what makes the
		// two towns feel built by the same hands.
		BlockPos box = base.relative(facing, 1).above().relative(across, span - 1);
		put(level, box, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, across));
		fill(level, box, random, TRADES[random.nextInt(TRADES.length)]);

		// AND A BARREL, WHICH IS A DIFFERENT KIND OF CONTAINER.
		//
		// The chest is what the household OWNED and the barrel is what they were in
		// the middle of — tools, torches, string, a worn pickaxe. Two containers
		// drawing from the same pool is one container twice; two drawing from
		// different pools is a house with a cupboard and a workbench in it.
		//
		// Beside the door rather than in the back, because that is where a barrel
		// of tools actually lives: you pick it up on the way out.
		BlockPos barrel = base.relative(facing, reachOf(facing, w, d) - 1).above()
			.relative(across, -(span - 1));
		put(level, barrel, Blocks.BARREL.defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.UP));
		fill(level, barrel, random, Loot.Tier.TOWN_TOOLS);

		// The second bed, along the same wall, one clear block off the first.
		BlockPos spare = head.relative(across, -2);
		if (level.getBlockState(spare).isAir()
			&& level.getBlockState(spare.relative(facing)).isAir()) {
			put(level, spare, Blocks.BED.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)
				.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, back)
				.setValue(BlockStateProperties.BED_PART,
					net.minecraft.world.level.block.state.properties.BedPart.HEAD));
			put(level, spare.relative(facing),
				Blocks.BED.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)
					.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, back)
					.setValue(BlockStateProperties.BED_PART,
						net.minecraft.world.level.block.state.properties.BedPart.FOOT));
		}

		dwellers(level, base, random);
	}

	// ---- WHO IS IN IT ------------------------------------------------------
	/**
	 * TWO OF THEM, AND THEY WERE HERE BEFORE THE PLAYER WAS.
	 *
	 * The city has been standing empty since it was written, and empty was the
	 * right first draft — an intact town with nobody in it has no explanation
	 * available and that is worse than a ruin. But empty only works ONCE. The
	 * second visit it is scenery, because nothing in it can surprise you.
	 *
	 * So the houses have people in them, and they are the wrong people. Not a
	 * horde and not a dungeon: two per house, at home, awake, doing nothing. The
	 * arithmetic is what does the work — a street of eight houses is sixteen of
	 * them, spread across sixteen rooms rather than massed in a square, and the
	 * player meets them one door at a time.
	 *
	 * NOTHING HERE MAKES THEM PERSISTENT, because TurnedEntity already is: it
	 * calls setPersistenceRequired in its constructor and overrides
	 * removeWhenFarAway to false. They were put here on purpose and they stay
	 * whether or not anybody is looking, which is the whole point of a town you
	 * can leave and come back to.
	 */
	private static final int PER_HOUSE = 2;

	private static void dwellers(ServerLevel level, BlockPos base,
	                             RandomSource random) {
		for (int who = 0; who < PER_HOUSE; who++) {
			com.bloomlet.herobrine.entity.TurnedEntity turned =
				com.bloomlet.herobrine.entity.ModEntities.TURNED.create(level,
					net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
			if (turned == null) {
				return;
			}
			// Inside the room, off the middle, so two of them are not standing in
			// each other on the first tick.
			double ox = (who == 0 ? -1.0 : 1.0) * (0.5 + random.nextDouble());
			double oz = (random.nextDouble() - 0.5) * 2.0;
			turned.snapTo(base.getX() + 0.5 + ox, base.getY() + 1,
				base.getZ() + 0.5 + oz, random.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntity(turned);
		}
	}

	/**
	 * WHAT EVERY HOUSE HAS IS NOT WHAT EVERY HOUSE HAS.
	 *
	 * Drawn per house rather than fixed, and that is the whole reason the street
	 * survives being walked down twice. One pool for thirty houses means the player
	 * knows what is in the fifth after opening the fourth, and stops opening them —
	 * which loses the town as well as the loot, because a container nobody opens is
	 * furniture.
	 *
	 * Home is weighted heaviest because most houses are houses. The forge and the
	 * armoury are in here at one share each, so a street of eight has probably got
	 * one good chest in it and no way of telling which from outside.
	 */
	private static final Loot.Tier[] TRADES = {
		// WEIGHTED TO HIS CITY, because that is where these houses are.
		//
		// The overworld pools are in here at one share each and they earn their
		// place: a cupboard of bread among cupboards of emeralds is what stops the
		// street being uniform, and one poor chest makes the next good one land.
		// But the majority has to pay, or crossing over was not worth doing.
		Loot.Tier.HIS_CITY, Loot.Tier.HIS_CITY, Loot.Tier.HIS_CITY,
		Loot.Tier.HIS_CITY, Loot.Tier.HIS_CITY,
		Loot.Tier.TOWN_FORGE,
		Loot.Tier.TOWN_ARMS,
		Loot.Tier.TOWN_TRADE,
		Loot.Tier.LARDER,
	};

	private static void fill(ServerLevel level, BlockPos at, RandomSource random,
	                         Loot.Tier tier) {
		if (level.getBlockEntity(at)
				instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity box) {
			Loot.scatter(box, random, tier);
		}
	}

	private static int reachOf(Direction facing, int w, int d) {
		return facing.getAxis() == Direction.Axis.X ? w : d;
	}

	// ---- AND WHAT IS AROUND IT ---------------------------------------------
	/**
	 * THE HALF THAT IS NOT THE BUILDING.
	 *
	 * A shovelled path to the door, banked ground either side of it, flowers in
	 * the grass and a log pile against the gable. Every one of these is two lines
	 * of code and together they do more than the roof does: a perfect house on
	 * flat turf is a model, and the same house with a worn track up to it is
	 * somewhere people walked every day for years.
	 *
	 * The path is DIRT_PATH proper, stepped where the ground steps, because a path
	 * that ignores the contour is a runway.
	 */
	private static void around(ServerLevel level, BlockPos base, int w, int d,
	                          Direction facing, RandomSource random) {
		int reach = facing.getAxis() == Direction.Axis.X ? w : d;
		Direction across = facing.getClockWise();

		// The track from the door, three wide, running out until it has gone far
		// enough to have met the street.
		for (int out = 1; out <= 9; out++) {
			for (int side = -1; side <= 1; side++) {
				// Ragged at the edges — a path worn by feet is not a ruler width.
				if (side != 0 && random.nextInt(4) == 0) {
					continue;
				}
				int x = base.getX() + facing.getStepX() * (reach + out)
					+ across.getStepX() * side;
				int z = base.getZ() + facing.getStepZ() * (reach + out)
					+ across.getStepZ() * side;
				if (!level.isLoaded(new BlockPos(x, base.getY(), z))
					|| !Ground.dry(level, x, z)) {
					continue;
				}
				int y = Ground.topOf(level, x, z);
				BlockPos on = new BlockPos(x, y, z);
				if (!level.getBlockState(on).isSolid()) {
					continue;
				}
				put(level, on, random.nextInt(7) == 0
					? Blocks.COARSE_DIRT.defaultBlockState()
					: Blocks.DIRT_PATH.defaultBlockState());
				clear(level, on.above());
			}
		}

		// THE LOG PILE, against a gable, axis across the wall so the ends show.
		BlockPos pile = base.relative(facing.getOpposite(), reach)
			.relative(across, random.nextBoolean() ? 1 : -1).above();
		for (int dy = 0; dy < 2; dy++) {
			for (int off = 0; off < 2; off++) {
				put(level, pile.above(dy).relative(across, off),
					Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
						.setValue(BlockStateProperties.AXIS, across.getAxis()));
			}
		}

		// AND THE GROUND, WHICH IS SOMEBODY ELSE'S JOB NOW.
		//
		// This was forty lines of the same scatter that the homestead wanted and the
		// shed wanted and the tower wanted, and four copies of a scatter is four
		// places for it to drift out of step. Grounds owns it.
		Grounds.dress(level, base, Math.max(w, d) + 2, Math.max(w, d) + 6, random);
		Grounds.yard(level, base.relative(facing, reachOf(facing, w, d) + 3),
			facing, random);
	}

	// ---- THE TWO PRIMITIVES ------------------------------------------------
	/** Never through bedrock, and never through a player's own work. */
	private static void put(ServerLevel level, BlockPos at, BlockState state) {
		if (at.getY() <= level.getMinY() || at.getY() >= level.getMaxY()) {
			return;
		}
		level.setBlock(at, state, 2);
	}

	private static void clear(ServerLevel level, BlockPos at) {
		put(level, at, Blocks.AIR.defaultBlockState());
	}
}
