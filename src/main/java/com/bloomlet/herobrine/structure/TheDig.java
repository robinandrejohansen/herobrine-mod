package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Feral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * HOUSE THREE. The gaol.
 *
 * A long barrel-vaulted hall cut into rock, cells down both sides of it, iron
 * on every door, and a warder's room at the end with a desk and a ledger. This
 * is the first building in the sequence that was not made for HIM to live in.
 * One was his home. Two was his home buried with somewhere to watch from on
 * top. Three is somewhere to keep other people, and nothing about it is
 * domestic at all.
 *
 * THE HAUNTING IS IN THE ARRANGEMENT, not in anything that jumps out. Fourteen
 * cells, and thirteen of them are open with the doors swung back and nothing
 * inside but a straw bed and a bucket. One is shut, and there is something in
 * it, and it has been in there long enough to have stopped reacting to the
 * door. A player checks every cell — they will, because the empty ones train
 * them to — and the arithmetic does the rest.
 *
 * The old version of this was a bed in a cave with four tunnels that stopped.
 * The idea was right and it was unbuildable: bore() clamps to seven blocks under
 * real ground, so a dig started at the surface had no mouth and the whole thing
 * was sealed underground. That is why the third house "didn't work" — it was
 * there, complete, with no way in. Descent cuts the opening now.
 */
public final class TheDig {
	private TheDig() {}

	private static final int DROP = 16;
	private static final int HALL = 34;
	private static final int CELLS_PER_SIDE = 7;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos top = new BlockPos(origin.getX(),
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ());

		gatehouse(level, top, random);
		BlockPos landing = Descent.stair(level, top.offset(-1, 0, -1), DROP,
			brick(random), random);

		BlockPos far = hall(level, landing.below(2), random);
		warder(level, far, random);
		workings(level, far, random);

		HerobrineMod.LOGGER.info("the gaol opened at [{}, {}, {}]",
			landing.getX(), landing.getY(), landing.getZ());
	}

	/**
	 * What you see from the surface: a low stone mouth and a gate off its
	 * hinges.
	 *
	 * Small, because the building is not up here. It has to be enough to say
	 * "this is a door" from across a field and nothing more — everything that
	 * matters is sixteen blocks down, and a large surface building would spend
	 * the surprise before the player has committed to the stair.
	 */
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
				// Ragged: each column stops at its own height, so it reads as a
				// building that has come down rather than one built low.
				int height = 1 + random.nextInt(4);
				for (int up = 0; up < height; up++) {
					level.setBlock(new BlockPos(x, y + up, z), brick(random), 2);
				}
			}
		}
		// The gate, hanging open and never closing again.
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

	/**
	 * The hall, and the cells down both sides of it.
	 *
	 * Barrel-vaulted and long. Length is what makes a corridor of cells read as
	 * an institution rather than a basement — you can see all the way to the
	 * far end from the bottom of the stair, and every door between here and
	 * there is standing open.
	 */
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
		}

		int spacing = HALL / (CELLS_PER_SIDE + 1);
		for (int i = 1; i <= CELLS_PER_SIDE; i++) {
			int out = i * spacing;
			// The shut one is always the same distance in, so it is not the
			// last cell and not the first — it is one they walk past twice.
			boolean shut = i == CELLS_PER_SIDE - 2;
			cell(level, start.offset(-3, 0, out), Direction.EAST, random, shut);
			cell(level, start.offset(3, 0, out), Direction.WEST, random, false);
		}
		return start.offset(0, 0, HALL - 1);
	}

	/**
	 * One cell. Five by five, low, with a barred front.
	 *
	 * The bars rather than a wall with a door in it, for the same reason the
	 * undercroft's are: a barred frontage is a thing built so whoever is
	 * outside can watch whoever is inside, and a player understands that in
	 * half a second without being told.
	 */
	private static void cell(ServerLevel level, BlockPos mouth, Direction into,
	                         RandomSource random, boolean shut) {
		Direction across = into.getClockWise();

		for (int in = 0; in <= 5; in++) {
			for (int side = -2; side <= 2; side++) {
				for (int up = -1; up <= 4; up++) {
					BlockPos at = mouth.relative(into, in).relative(across, side).above(up);
					boolean shell = in == 5 || Math.abs(side) == 2 || up == -1 || up == 4;
					level.setBlock(at, shell ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}

		// The frontage.
		for (int side = -1; side <= 1; side++) {
			for (int up = 0; up <= 2; up++) {
				BlockPos at = mouth.relative(across, side).above(up);
				level.setBlock(at, side == 0
					? Blocks.CAVE_AIR.defaultBlockState()
					: Blocks.IRON_BARS.defaultBlockState(), 2);
			}
		}
		BlockState door = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, into.getOpposite())
			.setValue(BlockStateProperties.OPEN, !shut);
		level.setBlock(mouth, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(mouth.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);

		// Straw, a bucket, and cobwebs. The same in every one of them, which is
		// what makes fourteen of them read as a system rather than as rooms.
		BlockPos in = mouth.relative(into, 3);
		level.setBlock(in.relative(across, -1), Blocks.HAY_BLOCK.defaultBlockState(), 2);
		level.setBlock(in.relative(across, 1), Blocks.CAULDRON.defaultBlockState(), 2);
		for (int i = 0; i < 4; i++) {
			BlockPos web = mouth.relative(into, 1 + random.nextInt(4))
				.relative(across, random.nextInt(3) - 1).above(random.nextInt(3));
			if (level.getBlockState(web).isAir() && random.nextBoolean()) {
				level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}

		if (!shut) {
			return;
		}
		// And the one that is still occupied.
		Mob kept = com.bloomlet.herobrine.entity.ModEntities.INFECTED
			.create(level, EntitySpawnReason.STRUCTURE);
		if (kept == null) {
			return;
		}
		BlockPos stand = mouth.relative(into, 3);
		kept.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
		// Into the world first, THEN marked — attachments set before the entity
		// is in a level have nowhere to be tracked from and never sync.
		level.addFreshEntity(kept);
		Feral.shutIn(kept);
	}

	/**
	 * The warder's room, and the ledger in it.
	 *
	 * The only room down here with a chair, a table and a lamp somebody chose
	 * the position of. It is also the room that answers what the place was for,
	 * and the answer is worse than the cells: somebody sat here, at a desk, and
	 * kept records.
	 */
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

		BlockPos desk = far.offset(0, 0, 5);
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
			level.setBlock(far.offset(dx, 1, 7), Blocks.BOOKSHELF.defaultBlockState(), 2);
			level.setBlock(far.offset(dx, 2, 7), Blocks.BOOKSHELF.defaultBlockState(), 2);
		}

		BlockPos chestAt = far.offset(-3, 0, 5);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			ItemStack book = HouseBooks.theDig();
			if (book != null) {
				chest.setItem(0, book);
			}
			chest.setItem(1, new ItemStack(Items.IRON_INGOT, 6));
			Loot.scatter(chest, random, Loot.Tier.LARDER);
		}

		sign(level, far.offset(2, 1, 5),
			new String[] { "COUNT", "THEM IN", "COUNT", "THEM OUT" });
	}

	/**
	 * And the workings behind it — the part he was actually here for.
	 *
	 * Three passages out of the back of the warder's room, unlined and
	 * unlantern'd, going down and away from the cells. This is the connection
	 * the whole sequence needs: the gaol is not the end of the story, it is a
	 * thing built ON TOP of a hole he was already digging, and the passages
	 * make that legible without a word.
	 *
	 * They are bored rather than built, so they stay under the clamp and cannot
	 * surface — which is correct here, unlike at the entrance.
	 */
	private static void workings(ServerLevel level, BlockPos far, RandomSource random) {
		BlockPos back = far.offset(0, 1, 7);
		BlockPos[] ends = {
			Digging.bore(level, back, new Vec3(-1.0, -0.35, 0.5), 28, 1.5, random),
			Digging.bore(level, back, new Vec3(1.0, -0.30, 0.4), 32, 1.5, random),
			Digging.bore(level, back, new Vec3(0.1, -0.55, 1.0), 24, 1.6, random),
		};
		for (BlockPos end : ends) {
			Digging.hollow(level, end, 3.2, random);
			Digging.props(level, end, 3, random);
			BlockPos under = Digging.groundUnder(level, end);
			if (under != null && level.getBlockState(under.above()).isAir()) {
				level.setBlock(under.above(), Blocks.SOUL_TORCH.defaultBlockState(), 2);
			}
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
