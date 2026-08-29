package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * HOUSE FOUR. A church with no roof, and nobody sleeps here.
 *
 * Open to the sky on purpose, and it is the whole design. Every other building
 * in this sequence is about being enclosed — a farmhouse, then the same
 * farmhouse buried, then a hall of cells sixteen blocks underground. This one
 * has walls and pillars and an aisle and a chancel and NO ROOF, so a player
 * walks in out of the weather into more weather.
 *
 * That reversal is what makes it frightening rather than restful. Three
 * buildings have taught them that his places close over the top of you; this
 * one is a room built to be seen INTO, which is a much worse thought and takes
 * no blocks at all to say. It is not shelter. It is a stage.
 *
 * THERE IS NO BED, and after three buildings that each had one that is the
 * loudest thing this mod ever says without a word. One was a home. Two put the
 * home underground and built somewhere to watch from on top. Three had no bed
 * for HIM because it was built to hold other people. Four has none because he
 * has stopped.
 *
 * The signs are the first writing in the sequence that is not a diary. He has
 * stopped recording and started instructing, and instructions have a reader.
 * That change of address is the content: somebody is being spoken to, and it is
 * not the player, and it may not be a person.
 */
public final class Shrine {
	private Shrine() {}

	private static final int WIDTH = 13;
	private static final int LENGTH = 23;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos corner = new BlockPos(origin.getX() - WIDTH / 2,
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ() - LENGTH / 2);

		clearing(level, corner, random);
		nave(level, corner, random);
		colonnade(level, corner, random);
		chancel(level, corner, random);
		instructions(level, corner, random);
		graves(level, corner, random);
		// THE CRYPT CARVES, SO IT GOES BEFORE THE THINGS THAT CAN BE CARVED THROUGH.
		//
		// crypt() ends in Warren.dig, which bores a trunk and spurs out from under
		// the chancel — and belongings() had already set this building's chests
		// down. Same fault as the gaol, same rule, same file that documents it.
		crypt(level, corner, random);
		belongings(level, corner, random);

		HerobrineMod.LOGGER.info("the open church stands at [{}, {}, {}]",
			corner.getX(), corner.getY(), corner.getZ());
	}

	/**
	 * Ground worn to rock, well past the walls.
	 *
	 * The cheapest way to say somebody was here every day for years. A building
	 * says somebody built something once; bare earth around it says somebody
	 * walked it into that state.
	 */
	private static void clearing(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = -6; dx < WIDTH + 6; dx++) {
			for (int dz = -6; dz < LENGTH + 6; dz++) {
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);

				BlockPos above = ground.above();
				if (!level.getBlockState(above).isAir()
					&& !level.getBlockState(above).isSolid()) {
					level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				}
				boolean inside = dx >= 0 && dx < WIDTH && dz >= 0 && dz < LENGTH;
				level.setBlock(ground, inside
					? paving(random, dx)
					: random.nextInt(3) == 0
						? Blocks.PODZOL.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Walls, and they stop where a roof would start.
	 *
	 * Chest-high to head-high and no higher, uneven along the top. A wall that
	 * ends in a straight line reads as unfinished; one that ends raggedly reads
	 * as a wall that was never going to be finished, which is a different and
	 * much better sentence.
	 */
	private static void nave(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = 0; dx < WIDTH; dx++) {
			for (int dz = 0; dz < LENGTH; dz++) {
				boolean wall = dx == 0 || dx == WIDTH - 1 || dz == 0 || dz == LENGTH - 1;
				if (!wall) {
					continue;
				}
				// The doorway, dead centre of the near end.
				if (dz == LENGTH - 1 && Math.abs(dx - WIDTH / 2) <= 1) {
					continue;
				}
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				int y = Ground.topOf(level, x, z) + 1;
				int height = 3 + random.nextInt(3);

				for (int up = 0; up < height; up++) {
					level.setBlock(new BlockPos(x, y + up, z), rough(random), 2);
				}
				// A course of wall blocks along the top, so the edge is a
				// parapet rather than a cut.
				level.setBlock(new BlockPos(x, y + height, z),
					Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * Two rows of pillars holding nothing up.
	 *
	 * They carry no roof — there is no roof. They are here because a roofless
	 * hall with bare walls is a yard, and the moment there are columns down
	 * both sides it is unmistakably a nave, and the player understands they are
	 * standing in a church rather than a ruin.
	 */
	private static void colonnade(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dz = 4; dz < LENGTH - 4; dz += 3) {
			for (int dx : new int[] { 3, WIDTH - 4 }) {
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				int y = Ground.topOf(level, x, z) + 1;
				int height = 5 + random.nextInt(2);

				for (int up = 0; up < height; up++) {
					level.setBlock(new BlockPos(x, y + up, z),
						Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				level.setBlock(new BlockPos(x, y + height, z),
					Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
				// Half of them have come down. A perfect colonnade is a temple;
				// a broken one is somewhere that has been standing a while.
				if (random.nextInt(3) == 0) {
					for (int up = height - 2; up <= height; up++) {
						level.setBlock(new BlockPos(x, y + up, z),
							Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	/** The far end: a raised chancel, an altar, and candles somebody lit. */
	private static void chancel(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int dx = 2; dx < WIDTH - 2; dx++) {
			for (int dz = 1; dz <= 5; dz++) {
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				int y = Ground.topOf(level, x, z) + 1;
				level.setBlock(new BlockPos(x, y, z),
					Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
			}
		}
		int mx = corner.getX() + WIDTH / 2;
		int mz = corner.getZ() + 3;
		int my = Ground.topOf(level, mx, mz) + 2;

		BlockPos altar = new BlockPos(mx, my, mz);
		level.setBlock(altar, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.west(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.east(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(altar.above(), Blocks.SOUL_LANTERN.defaultBlockState(), 2);

		for (BlockPos at : new BlockPos[] { altar.west(2), altar.east(2) }) {
			level.setBlock(at, Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.LIT, true), 2);
		}
		// Banners behind it, in the only colours the place could make.
		for (int dx = -2; dx <= 2; dx += 4) {
			level.setBlock(altar.offset(dx, 1, -1), Blocks.WOOL
				.pick(random.nextBoolean() ? DyeColor.RED : DyeColor.BROWN)
				.defaultBlockState(), 2);
		}
	}

	/** Pews, of a sort: two rows of stone benches facing the altar. */
	private static void instructions(ServerLevel level, BlockPos corner, RandomSource random) {
		String[][] lines = {
			{ "DO NOT", "SLEEP" },
			{ "BRING", "NOTHING" },
			{ "STAND", "IN THE", "LIGHT" },
			{ "DO NOT", "LEAVE" },
			{ "IT TAKES", "AS LONG", "AS IT TAKES" },
		};
		for (int i = 0; i < lines.length; i++) {
			int dx = i % 2 == 0 ? 2 : WIDTH - 3;
			int dz = 7 + i * 3;
			if (dz >= LENGTH - 2) {
				continue;
			}
			int x = corner.getX() + dx;
			int z = corner.getZ() + dz;
			BlockPos at = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 2);
			if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
				String[] said = lines[i];
				SignText text = new SignText();
				for (int row = 0; row < 4; row++) {
					text = text.setMessage(row, Component.literal(
						row < said.length ? said[row] : ""));
				}
				sign.setText(text, true);
			}
		}
	}

	/**
	 * Everything he was carrying, in a chest by the door.
	 *
	 * The detail the whole building turns on. An empty chest is a place
	 * somebody left; a chest with a good pickaxe, food, a flint and steel and a
	 * bed in it is a place somebody walked into ON PURPOSE having first put
	 * down every single thing that would have helped them.
	 */
	private static void belongings(ServerLevel level, BlockPos corner, RandomSource random) {
		int x = corner.getX() + 2;
		int z = corner.getZ() + LENGTH - 3;
		BlockPos at = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);

		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
		if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
			return;
		}
		ItemStack book = HouseBooks.theShrine();
		if (book != null) {
			chest.setItem(0, book);
		}
		chest.setItem(1, HouseBooks.theChurchAfter());
		chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
		chest.setItem(3, new ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL));
		chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.BREAD, 9));
		chest.setItem(5, new ItemStack(
			net.minecraft.world.item.Items.BED.pick(DyeColor.WHITE)));
		Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
	}

	/**
	 * A graveyard outside the wall, and every stone is blank.
	 *
	 * Not a single name. A churchyard with names is a place people were buried
	 * by somebody who knew them; a churchyard of blank stones is one where
	 * somebody kept digging and stopped bothering to record who, and it needs
	 * no sign to say so.
	 */
	private static void graves(ServerLevel level, BlockPos corner, RandomSource random) {
		for (int i = 0; i < 14; i++) {
			int dx = -5 + random.nextInt(4);
			int dz = 3 + random.nextInt(LENGTH - 6);
			int x = corner.getX() + dx;
			int z = corner.getZ() + dz;
			int y = Ground.topOf(level, x, z) + 1;

			level.setBlock(new BlockPos(x, y, z),
				Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
			level.setBlock(new BlockPos(x, y - 1, z), Blocks.PODZOL.defaultBlockState(), 2);
			if (random.nextInt(3) == 0) {
				level.setBlock(new BlockPos(x, y + 1, z),
					Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * And a way down under the chancel, because the sequence does not stop
	 * here.
	 *
	 * Short, unlit, and it goes toward the threshold rather than nowhere. The
	 * fifth building is the only one of these with an answer in it, and a
	 * passage leaving the fourth in its direction is the closest this mod comes
	 * to pointing at anything.
	 */
	private static void crypt(ServerLevel level, BlockPos corner, RandomSource random) {
		BlockPos under = new BlockPos(corner.getX() + WIDTH / 2,
			Ground.topOf(level, corner.getX() + WIDTH / 2, corner.getZ() + 6) + 1,
			corner.getZ() + 6);

		BlockPos landing = Descent.shaft(level, under, 11, rough(random));
		Descent.hatch(level, under.above(), Direction.SOUTH);

		// BURIED: short, low, and it goes down more than it goes along. A crypt
		// rather than a mine, under the one building in the sequence that has
		// no roof — everything above this is open to the sky and everything
		// below it is not, which is the whole of house four in one section.
		Warren.warn(level, under.above(), new String[] { "", "DO NOT", "LEAVE", "" });
		Warren.dig(level, landing, Warren.Manner.BURIED, random);
	}

	private static BlockState paving(RandomSource random, int dx) {
		if (dx == WIDTH / 2) {
			return Blocks.POLISHED_ANDESITE.defaultBlockState();
		}
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 6) {
			return Blocks.ANDESITE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState rough(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 4) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 10) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
	}
}
