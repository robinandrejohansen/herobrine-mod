package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * HOUSE FOUR. Nobody sleeps here.
 *
 * THERE IS NO BED, and after three buildings that each had one that is the
 * loudest thing this mod ever says without a word. One was a home. Two was the
 * same home with the windows filled in. Three had given up everything except a
 * bed, because a person still has to sleep. Four has given up the bed.
 *
 * The player will notice. They have been trained to look for it by the three
 * before this, and they will walk the whole building checking — which is
 * exactly the walk this place is designed to be found on.
 *
 * What is here instead is a room built to STAND IN. A ring of standing stones,
 * a lamp at the centre of it, and the floor worn down to bare rock in a circle
 * where somebody stood for a very long time. No altar in the religious sense,
 * nothing to worship, no idol — a place to be looked at from, which is a much
 * worse idea and takes fewer blocks.
 *
 * The signs are the first writing in the sequence that is not a diary. He has
 * stopped recording and started instructing, and instructions have a reader.
 * That change of address is the content: somebody is being spoken to, and it is
 * not the player, and it may not be a person.
 *
 * One chest, and what is in it is worse than an empty one: everything he was
 * carrying. He put it down before he stood in the circle, because the book says
 * to bring nothing.
 */
public final class Shrine {
	private Shrine() {}

	private static final int RADIUS = 7;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos centre = new BlockPos(origin.getX(),
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ());

		clearing(level, centre, random);
		stones(level, centre, random);
		middle(level, centre, random);
		instructions(level, centre, random);
		belongings(level, centre, random);

		HerobrineMod.LOGGER.info("the shrine stands at [{}, {}, {}]",
			centre.getX(), centre.getY(), centre.getZ());
	}

	/**
	 * Ground worn down to rock, in a circle, and nothing growing on it.
	 *
	 * The single cheapest way to say "somebody was here every day for years".
	 * A building says somebody built something once; bare earth in a ring says
	 * somebody walked it into that state.
	 */
	private static void clearing(ServerLevel level, BlockPos centre, RandomSource random) {
		for (int dx = -RADIUS - 1; dx <= RADIUS + 1; dx++) {
			for (int dz = -RADIUS - 1; dz <= RADIUS + 1; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > RADIUS + 1) {
					continue;
				}
				int x = centre.getX() + dx;
				int z = centre.getZ() + dz;
				BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);

				// Anything standing on it goes: grass, flowers, saplings. Not
				// the trees — a felled wood would look like a building site.
				BlockPos above = ground.above();
				if (!level.getBlockState(above).isAir()
					&& !level.getBlockState(above).isSolid()) {
					level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				}
				level.setBlock(ground, reach < 2.6
					? Blocks.STONE.defaultBlockState()
					: random.nextInt(4) == 0
						? Blocks.PODZOL.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * A ring of them, uneven, and not one is straight.
	 *
	 * Deliberately crude. Dressed stone would read as a monument somebody was
	 * proud of; rough pillars of different heights leaning at different angles
	 * read as something raised by one person over a long time with no help and
	 * no plan, which is what happened.
	 */
	private static void stones(ServerLevel level, BlockPos centre, RandomSource random) {
		int count = 9;
		for (int i = 0; i < count; i++) {
			double angle = i * (Math.PI * 2.0 / count) + random.nextDouble() * 0.12;
			int x = centre.getX() + (int)Math.round(Math.cos(angle) * (RADIUS - 1));
			int z = centre.getZ() + (int)Math.round(Math.sin(angle) * (RADIUS - 1));
			int base = Ground.topOf(level, x, z) + 1;
			int height = 3 + random.nextInt(3);

			for (int up = 0; up < height; up++) {
				// A lean: every second or third course steps a block sideways,
				// so no two stones are the same silhouette.
				int lean = up >= height - 1 && random.nextInt(3) == 0 ? 1 : 0;
				BlockPos at = new BlockPos(x + lean, base + up, z);
				level.setBlock(at, rough(random), 2);
			}
		}
	}

	/** The middle: a lamp, and the ground around it worn to nothing. */
	private static void middle(ServerLevel level, BlockPos centre, RandomSource random) {
		BlockPos plinth = new BlockPos(centre.getX(),
			Ground.topOf(level, centre.getX(), centre.getZ()) + 1, centre.getZ());
		level.setBlock(plinth, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
		level.setBlock(plinth.above(), Blocks.SOUL_LANTERN.defaultBlockState(), 2);

		// Four candles, and they are lit. Somebody has been back recently.
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos at = plinth.relative(side, 2);
			BlockPos ground = new BlockPos(at.getX(),
				Ground.topOf(level, at.getX(), at.getZ()) + 1, at.getZ());
			level.setBlock(ground, Blocks.CANDLE.defaultBlockState()
				.setValue(BlockStateProperties.LIT, true), 2);
		}
	}

	/**
	 * The signs, and one book on the plinth.
	 *
	 * The signs are terse because he has stopped explaining himself. The book
	 * carries the reasoning, and the reasoning is the frightening part — every
	 * line of it is a sane sentence and the paragraph they make is not.
	 */
	private static void instructions(ServerLevel level, BlockPos centre, RandomSource random) {
		String[][] lines = {
			{ "DO NOT", "SLEEP" },
			{ "BRING", "NOTHING" },
			{ "STAND", "IN THE", "LIGHT" },
			{ "DO NOT", "LEAVE" },
			{ "IT TAKES", "AS LONG", "AS IT TAKES" },
		};
		for (int i = 0; i < lines.length; i++) {
			double angle = i * (Math.PI * 2.0 / lines.length) + 0.35;
			int x = centre.getX() + (int)Math.round(Math.cos(angle) * (RADIUS - 3));
			int z = centre.getZ() + (int)Math.round(Math.sin(angle) * (RADIUS - 3));
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
	 * Everything he was carrying, in a chest at the edge.
	 *
	 * This is the detail the whole building turns on. An empty chest is a place
	 * somebody left; a chest with a good pickaxe, food, a bed roll and a flint
	 * and steel in it is a place somebody walked away from ON PURPOSE, having
	 * first put down every single thing that would have helped them.
	 */
	private static void belongings(ServerLevel level, BlockPos centre, RandomSource random) {
		int x = centre.getX() + RADIUS - 2;
		int z = centre.getZ();
		BlockPos at = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);

		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 2);
		if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
			return;
		}
		ItemStack book = HouseBooks.theShrine();
		if (book != null) {
			chest.setItem(0, book);
		}
		chest.setItem(1, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
		chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL));
		chest.setItem(3, new ItemStack(net.minecraft.world.item.Items.BREAD, 9));
		chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.BED.pick(net.minecraft.world.item.DyeColor.WHITE)));
		Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
	}

	private static BlockState rough(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 4) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.ANDESITE.defaultBlockState();
		}
		if (roll < 10) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
	}
}
