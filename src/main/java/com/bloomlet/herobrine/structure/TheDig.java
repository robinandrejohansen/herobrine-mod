package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * THE GAOL. House three.
 *
 * A gatehouse on the surface, a stair sixteen down, and a hall with cells off
 * both sides. They built it to keep him out and ended up keeping each other in
 * (book four): the rule was that anyone who was "not themselves" went in a
 * cell, and anyone could say it of anyone.
 *
 * THE HALL IS EIGHTY LONG AND THE CELLS ARE ROOMS. It was thirty-four with
 * seven cells a side the size of cupboards; the request was a longer walk with
 * more of them and room to stand in each. Nine a side now, six deep, five wide,
 * five high, a hay bed, a cauldron, a chain from the ceiling, and the scratched
 * sign on the back wall that says who was in it.
 *
 * SIX OF THEM ARE STILL SHUT, AND SOMETHING IS IN EACH. Four on the west, two
 * on the east, a Gaunt behind every closed door, kept there by its latch until
 * you are near (GauntEntity.keptBehind). Cell nine is one of them.
 *
 * AT THE END, THE WARDER'S ROOM, AND OUT OF ITS BACK WALL THE WAY THEY LEFT.
 * The last of them dug out through the wall and kept going, and the tunnel is
 * still there: three wide, propped with timber, climbing a block every three
 * until it breaks the surface fifty-odd blocks on, grown over. There is a sign
 * at its mouth saying where they were going. The map to the church is in the
 * chest beside it (Dwellings.inTheGaol finds it through keepAt).
 */
public final class TheDig {
	private TheDig() {}

	private static final int DROP = 16;
	private static final int HALL = 80;
	private static final int CELLS_PER_SIDE = 9;
	private static final int SPACING = HALL / (CELLS_PER_SIDE + 1);      // eight; a cell is seven across
	/** A cell's inside: six deep, five wide (two either side of the door line), five high. */
	private static final int CELL_DEEP = 6;
	private static final int CELL_HALF = 2;
	private static final int CELL_HIGH = 5;

	/** Where the chest is, from the origin; Dwellings looks here for a container to leave the map in. */
	public static BlockPos keepAt(ServerLevel level, BlockPos origin) {
		int ground = Ground.topOf(level, origin.getX(), origin.getZ()) + 1;
		return new BlockPos(origin.getX() - 3, ground - DROP - 2, origin.getZ() + HALL + 5);
	}

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos top = new BlockPos(origin.getX(),
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ());
		gatehouse(level, top, random);
		BlockPos landing = Descent.stair(level, top.offset(-1, 0, -1), DROP,
			brick(random), random);
		BlockPos start = landing.below(2);
		BlockPos far = hall(level, start, random);
		warder(level, far, random);
		keeps(level, far, random);
		escape(level, far, random);
		dressCells(level, start, random);
		HerobrineMod.LOGGER.info("the gaol opened at [{}, {}, {}] — {} cells, {} of them shut",
			landing.getX(), landing.getY(), landing.getZ(), CELLS_PER_SIDE * 2, shutCount());
	}

	private static void gatehouse(ServerLevel level, BlockPos top, RandomSource random) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				int x = top.getX() + dx;
				int z = top.getZ() + dz;
				int y = Ground.topOf(level, x, z) + 1;
				boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
				level.setBlock(new BlockPos(x, y - 1, z),
					Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
				if (!wall) {
					continue;
				}
				int height = 1 + random.nextInt(4);
				for (int up = 0; up < height; up++) {
					level.setBlock(new BlockPos(x, y + up, z), brick(random), 2);
				}
			}
		}
		BlockPos gate = top.offset(0, 0, 3);
		BlockState iron = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
			.setValue(BlockStateProperties.OPEN, true);
		level.setBlock(gate, iron.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(gate.above(), iron.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);
		sign(level, top.offset(4, 1, 0), new String[] { "NO ONE", "IS KEPT", "AGAINST", "THEIR WILL" });
	}

	// ---- THE HALL -----------------------------------------------------------

	private static BlockPos hall(ServerLevel level, BlockPos start, RandomSource random) {
		for (int out = 0; out < HALL; out++) {
			for (int side = -6; side <= 6; side++) {
				for (int up = -1; up <= 6; up++) {
					BlockPos at = start.offset(side, up, out);
					double curve = Math.abs(side) / 6.0;
					int roof = (int)Math.round(5 - curve * curve * 3.0);
					boolean corridor = Math.abs(side) <= 2;
					if (up == -1) {
						level.setBlock(at, out % 6 == 0
							? Blocks.POLISHED_ANDESITE.defaultBlockState()
							: floor(random), 2);
					} else if (corridor && up <= roof) {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					} else if (corridor) {
						level.setBlock(at, brick(random), 2);
					}
				}
			}
			if (out % 8 == 4) {
				level.setBlock(start.offset(0, 5, out), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
			if (out % 16 == 12) {
				// a dropped pail, a chain, a puddle: the hall was used
				level.setBlock(start.offset(random.nextInt(3) - 1, 0, out), random.nextBoolean()
					? Blocks.CAULDRON.defaultBlockState()
					: Blocks.COBWEB.defaultBlockState(), 2);
			}
		}
		for (Cell spot : cells(start)) {
			cell(level, spot.mouth(), spot.into(), random, spot.shut());
		}
		return start.offset(0, 0, HALL - 1);
	}

	private record Cell(BlockPos mouth, Direction into, boolean shut) {}

	/** West before east at each rank, so the shut west cell at rank five is the ninth: cell nine. */
	private static java.util.List<Cell> cells(BlockPos start) {
		java.util.List<Cell> found = new java.util.ArrayList<>();
		for (int i = 1; i <= CELLS_PER_SIDE; i++) {
			int out = i * SPACING;
			boolean shutWest = i >= 4 && i <= 7;
			boolean shutEast = i == 6 || i == 8;
			found.add(new Cell(start.offset(-3, 0, out), Direction.WEST, shutWest));
			found.add(new Cell(start.offset(3, 0, out), Direction.EAST, shutEast));
		}
		return found;
	}

	private static int shutCount() {
		int n = 0;
		for (int i = 1; i <= CELLS_PER_SIDE; i++) {
			if (i >= 4 && i <= 7) {
				n++;
			}
			if (i == 6 || i == 8) {
				n++;
			}
		}
		return n;
	}

	private static void cell(ServerLevel level, BlockPos mouth, Direction into,
	                         RandomSource random, boolean shut) {
		Direction across = into.getClockWise();
		int back = CELL_DEEP + 1;
		int wall = CELL_HALF + 1;
		for (int in = 0; in <= back; in++) {
			for (int side = -wall; side <= wall; side++) {
				for (int up = -1; up <= CELL_HIGH; up++) {
					BlockPos at = mouth.relative(into, in).relative(across, side).above(up);
					boolean shell = in == back || Math.abs(side) == wall || up == -1 || up == CELL_HIGH;
					if (in == 0 && !shell) {
						continue;      // the front is done below
					}
					level.setBlock(at, shell ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
		// THE FRONT: frame, a lintel, bars either side of a two-high doorway with
		// bars above it, so the whole cell is visible from the hall.
		for (int up = 0; up < CELL_HIGH; up++) {
			for (int side = -wall; side <= wall; side++) {
				BlockPos at = mouth.relative(across, side).above(up);
				if (Math.abs(side) == wall || up == CELL_HIGH - 1) {
					level.setBlock(at, brick(random), 2);
				} else if (side == 0 && up <= 1) {
					level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
				} else {
					level.setBlock(at, Blocks.IRON_BARS.defaultBlockState(), 2);
				}
			}
		}
		BlockState door = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, into.getOpposite())
			.setValue(BlockStateProperties.OPEN, !shut);
		level.setBlock(mouth, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(mouth.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);
		BlockPos switchAt = mouth.relative(across, wall)
			.relative(into.getOpposite(), 1).above();
		if (level.getBlockState(switchAt).isAir()
			&& level.getBlockState(switchAt.relative(into)).isSolid()) {
			level.setBlock(switchAt, Blocks.LEVER.defaultBlockState()
				.setValue(BlockStateProperties.ATTACH_FACE,
					net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, into.getOpposite()), 2);
		}
		// INSIDE: a hay bed against the back wall, a cauldron, a brown rag of
		// carpet, a chain hanging from the ceiling, webs in the open ones.
		BlockPos bed = mouth.relative(into, CELL_DEEP).relative(across, -CELL_HALF + 1);
		level.setBlock(bed, Blocks.HAY_BLOCK.defaultBlockState(), 2);
		level.setBlock(bed.relative(across, 1), Blocks.HAY_BLOCK.defaultBlockState(), 2);
		level.setBlock(mouth.relative(into, 2).relative(across, CELL_HALF), Blocks.CAULDRON.defaultBlockState(), 2);
		level.setBlock(mouth.relative(into, 3).relative(across, -1),
			Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState(), 2);
		BlockPos chain = mouth.relative(into, 3).relative(across, 1).above(CELL_HIGH - 1);
		level.setBlock(chain, Blocks.IRON_CHAIN.defaultBlockState(), 2);
		level.setBlock(chain.below(), Blocks.IRON_CHAIN.defaultBlockState(), 2);
		if (!shut) {
			for (int i = 0; i < 6; i++) {
				BlockPos web = mouth.relative(into, 1 + random.nextInt(CELL_DEEP))
					.relative(across, random.nextInt(CELL_HALF * 2 + 1) - CELL_HALF)
					.above(random.nextInt(CELL_HIGH));
				if (level.getBlockState(web).isAir() && random.nextBoolean()) {
					level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
				}
			}
			return;
		}
		com.bloomlet.herobrine.entity.GauntEntity kept = com.bloomlet.herobrine.entity.ModEntities.GAUNT
			.create(level, EntitySpawnReason.STRUCTURE);
		if (kept == null) {
			return;
		}
		BlockPos stand = mouth.relative(into, 3);
		kept.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
		kept.setPersistenceRequired();
		kept.keptBehind(mouth);
		level.addFreshEntity(kept);
	}

	// ---- THE WARDER'S ROOM ------------------------------------------------------

	private static void warder(ServerLevel level, BlockPos far, RandomSource random) {
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = 0; dz <= 8; dz++) {
				for (int up = -1; up <= 5; up++) {
					BlockPos at = far.offset(dx, up, dz);
					boolean shell = Math.abs(dx) == 5 || dz == 8 || up == -1 || up == 5;
					level.setBlock(at, shell ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
		// THE DESK STANDS ON THE FLOOR. It floated: the old warren dug out of the
		// back wall took the floor under it. The floor is laid again here, under
		// the desk and under the chest, before either is set.
		BlockPos desk = far.offset(0, 0, 5);
		for (BlockPos under : new BlockPos[] { desk, desk.west(), desk.east(), far.offset(-3, 0, 5) }) {
			level.setBlock(under.below(), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
		}
		level.setBlock(desk, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
		level.setBlock(desk.west(), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
		level.setBlock(desk.east(), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
		level.setBlock(desk.above(), Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		level.setBlock(desk.south(), Blocks.SPRUCE_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		level.setBlock(far.offset(0, 4, 4), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
		for (int dx = -3; dx <= 3; dx += 2) {
			if (dx == -1 || dx == 1) {
				continue;      // the mouth of the tunnel is between them
			}
			level.setBlock(far.offset(dx, 1, 7), Blocks.BOOKSHELF.defaultBlockState(), 2);
			level.setBlock(far.offset(dx, 2, 7), Blocks.BOOKSHELF.defaultBlockState(), 2);
		}
		sign(level, far.offset(2, 1, 5),
			new String[] { "COUNT", "THEM IN", "COUNT", "THEM OUT" });
	}

	private static void keeps(ServerLevel level, BlockPos far, RandomSource random) {
		BlockPos chestAt = far.offset(-3, 0, 5);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			chest.setItem(0, HouseBooks.four());
			chest.setItem(2, new ItemStack(Items.IRON_INGOT, 6));
			Loot.scatter(chest, random, Loot.Tier.GAOL);
		}
	}

	// ---- THE WAY THEY LEFT ------------------------------------------------------

	private static final int TUNNEL_MOST = 160;

	/**
	 * Out of the back wall, three wide and three high, propped with timber every
	 * six blocks, climbing one every three, until it breaks the surface. Where it
	 * comes out is grown over: moss, azalea, leaves, a ring of mossy stone. The
	 * sign at the mouth says where they were going, which is where the map in the
	 * chest goes.
	 */
	private static void escape(ServerLevel level, BlockPos far, RandomSource random) {
		BlockPos mouth = far.offset(0, 0, 8);
		level.setBlock(mouth, Blocks.CAVE_AIR.defaultBlockState(), 2);
		level.setBlock(mouth.above(), Blocks.CAVE_AIR.defaultBlockState(), 2);
		level.setBlock(mouth.east(), Blocks.CAVE_AIR.defaultBlockState(), 2);
		level.setBlock(mouth.east().above(), Blocks.CAVE_AIR.defaultBlockState(), 2);
		scratch(level, far.offset(-2, 1, 7), Direction.NORTH,
			new String[] { "WE DUG OUT", "THIS WAY", "ON TO THE", "CHURCH" });
		int x0 = far.getX();
		int y = far.getY();
		int z = far.getZ() + 9;
		int props = 0;
		for (int s = 0; s < TUNNEL_MOST; s++, z++) {
			if (s % 3 == 2) {
				y++;
			}
			int x = x0 + (int)Math.round(Math.sin(s / 11.0) * 1.5);
			int surface = Ground.topOf(level, x, z);
			if (y + 1 >= surface) {
				breakOut(level, new BlockPos(x, y, z), surface, random);
				HerobrineMod.LOGGER.info("the gaol's tunnel comes out {} blocks on, at [{}, {}, {}]",
					s, x, surface + 1, z);
				return;
			}
			boolean prop = s % 6 == 3;
			for (int dx = -1; dx <= 1; dx++) {
				for (int up = -1; up <= 3; up++) {
					BlockPos at = new BlockPos(x + dx, y + up, z);
					if (up == -1) {
						if (!level.getBlockState(at).isSolid()) {
							level.setBlock(at, floor(random), 2);
						}
						if (random.nextInt(9) == 0) {
							level.setBlock(at, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
						}
					} else if (up == 3) {
						if (!level.getBlockState(at).isSolid()) {
							level.setBlock(at, brick(random), 2);
						}
					} else if (prop && (Math.abs(dx) == 1 || up == 2)) {
						level.setBlock(at, up == 2 && dx == 0
							? Blocks.DARK_OAK_PLANKS.defaultBlockState()
							: Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
					} else {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
			if (prop) {
				props++;
			}
			int roll = random.nextInt(12);
			BlockPos over = new BlockPos(x, y + 2, z);
			if (roll == 0 && level.getBlockState(over).isAir()) {
				level.setBlock(over, Blocks.HANGING_ROOTS.defaultBlockState(), 2);
			} else if (roll == 1 && level.getBlockState(over).isAir()) {
				level.setBlock(over, Blocks.GLOW_LICHEN.defaultBlockState()
					.setValue(net.minecraft.world.level.block.MultifaceBlock
						.getFaceProperty(Direction.UP), true), 2);
			} else if (roll == 2) {
				BlockPos web = new BlockPos(x + (random.nextBoolean() ? 1 : -1), y + 1 + random.nextInt(2), z);
				if (level.getBlockState(web).isAir()) {
					level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
				}
			} else if (roll == 3 && s > 20 && s % 24 == 0) {
				level.setBlock(new BlockPos(x, y + 2, z), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
		HerobrineMod.LOGGER.warn("the gaol's tunnel ran {} blocks and never found the sky", TUNNEL_MOST);
	}

	/** The last few blocks: up to daylight, and the ground around it grown over. */
	private static void breakOut(ServerLevel level, BlockPos at, int surface, RandomSource random) {
		for (int up = at.getY(); up <= surface + 1; up++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					level.setBlock(new BlockPos(at.getX() + dx, up, at.getZ() + dz),
						Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -2; dz <= 4; dz++) {
				int x = at.getX() + dx;
				int z = at.getZ() + dz;
				int top = Ground.topOf(level, x, z);
				BlockPos ground = new BlockPos(x, top, z);
				BlockPos over = ground.above();
				if (!level.getBlockState(over).isAir() || !level.getBlockState(ground).isSolid()) {
					continue;
				}
				boolean rim = Math.abs(dx) >= 2 || dz <= -1 || dz >= 3;
				int roll = random.nextInt(10);
				if (!rim) {
					continue;
				}
				if (roll < 3) {
					level.setBlock(ground, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
				} else if (roll < 5) {
					level.setBlock(ground, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
					level.setBlock(over, Blocks.MOSS_CARPET.defaultBlockState(), 2);
				} else if (roll < 7) {
					level.setBlock(over, (random.nextBoolean() ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA)
						.defaultBlockState(), 2);
				} else if (roll < 9) {
					level.setBlock(over, Blocks.OAK_LEAVES.defaultBlockState()
						.setValue(BlockStateProperties.PERSISTENT, true), 2);
				} else {
					level.setBlock(over, Blocks.FERN.defaultBlockState(), 2);
				}
			}
		}
	}

	// ---- THE WALLS ---------------------------------------------------------------

	private static final String[][] SCRATCHED = {
		new String[] { "MARTA", "DAY SIX", "STILL", "MYSELF" },
		new String[] { "IIII IIII", "IIII III", "ELEVEN", "DAYS" },
		new String[] { "I AM NOT", "HIM", "I AM NOT", "HIM" },
		new String[] { "TELL MY", "BROTHER", "I WENT IN", "CLEAN" },
		new String[] { "WHOEVER", "READS THIS", "I WAS", "REAL" },
		new String[] { "DAY ONE", "SCARED", "DAY NINE", "HUNGRY" },
		new String[] { "ASK ME", "SOMETHING", "ONLY I", "WOULD KNOW" },
		new String[] { "HE KNEW", "THE ANSWER", "HOW DID", "HE KNOW" },
		new String[] { "NINE", "I AM", "STILL", "MYSELF" },
		new String[] { "COUNT ME", "OUT", "PLEASE", "COUNT ME" },
		new String[] { "IIII", "I SLEPT", "THAT IS", "GOOD" },
		new String[] { "IF I STOP", "EATING", "DO NOT", "OPEN THIS" },
		new String[] { "JOREN", "WARDER", "SECOND", "MONTH" },
		new String[] { "NOBODY", "HAS COME", "IN FOUR", "DAYS" },
		new String[] { "THE BOY", "WOULD NOT", "BLINK", "" },
		new String[] { "IT IS ON", "THE", "CEILING", "" },
		new String[] { "MY WIFE", "SAID MY", "NAME WRONG", "" },
		new String[] { "TWELVE", "TWELVE", "TWELVE", "TWELVE" },
	};

	private static void dressCells(ServerLevel level, BlockPos start, RandomSource random) {
		java.util.List<Cell> spots = cells(start);
		for (int n = 0; n < spots.size(); n++) {
			Cell spot = spots.get(n);
			BlockPos at = spot.mouth().relative(spot.into(), CELL_DEEP).above(2);
			scratch(level, at, spot.into().getOpposite(),
				SCRATCHED[n % SCRATCHED.length]);
		}
	}

	private static void scratch(ServerLevel level, BlockPos at, Direction faces,
	                            String[] lines) {
		level.setBlock(at, Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, faces), 2);
		if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
			SignText text = new SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, Component.literal(
					row < lines.length ? lines[row] : ""));
			}
			sign.setText(text, true);
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
		}
	}

	private static BlockState floor(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.GRAVEL.defaultBlockState();
		}
		return Blocks.ANDESITE.defaultBlockState();
	}

	private static BlockState brick(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 4) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 8) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
