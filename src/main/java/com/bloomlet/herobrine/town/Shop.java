package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.structure.Loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * A shop, which is a front room somebody sells out of.
 *
 * Nine by nine and it has to do two jobs in that, so it is honestly divided:
 * the front two thirds is the shop and the back third is the room the family
 * lives in. One counter, one door, and a wall between the two with a curtain of
 * a doorway in it. That division is the entire design — a building where you
 * can see the private half from the public half is a building somebody lives
 * above their work in, which is what a town this size actually had.
 *
 * The awning is the part that does the most for a lane. Two of these facing
 * each other across a road with cloth over their windows reads instantly as a
 * street rather than as two sheds, and it costs six blocks.
 *
 * Which trade it is gets decided on placement rather than written in, so the
 * two shops in the town are never the same one twice.
 */
public final class Shop {
	private Shop() {}

	public static final int WIDTH = 9;
	public static final int DEPTH = 9;

	private static final String[] GROUND = {
		"         ",
		" ####### ",
		" ####### ",
		" ####### ",
		" ####### ",
		" ####### ",
		" ####### ",
		" ####### ",
		"         ",
	};

	private static final String[] SHOP = {
		"         ",
		" LWWWWWL ",
		" WK   AW ",
		" W  t  W ",
		" WPP_PPW ",
		" W   d W ",
		" Wnnn  W ",
		" LWgDgWL ",
		"         ",
	};

	/**
	 * THE TOP OF THE WALLS, AND NOTHING SOLID MAY SIT OVER THE DOOR.
	 *
	 * door() places BOTH halves of the door — lower here, upper one block up —
	 * and this layer runs afterwards. A trapdoor sat directly on the door's top
	 * half, which broke the door, popped it off as an item, and left the shop
	 * with no entrance at all. That is the whole "the trade shops have no way in"
	 * report, and it was three characters wide.
	 *
	 * The column above a 'D' is air here, always. Anything placed there wins.
	 */
	private static final String[] UPPER = {
		"         ",
		" LWWWWWL ",
		" W     W ",
		" g     g ",
		" W     W ",
		" W     W ",
		" g     g ",
		" LWb bWL ",
		"         ",
	};

	private static final String[][] LAYERS = { GROUND, SHOP, UPPER };

	/** What this one sells. Decided per building so no two are alike. */
	private enum Trade { BAKER, WEAVER, POTTER, FLETCHER }

	public static boolean build(ServerLevel level, BlockPos corner, Direction facing,
	                            RandomSource random) {
		Trade trade = Trade.values()[random.nextInt(Trade.values().length)];
		if (!Blueprint.place(level, corner, LAYERS, facing,
				(l, at, c, dir, mx, mz) -> set(l, at, c, dir, mx, mz, random, trade))) {
			return false;
		}
		gable(level, corner, facing);
		awning(level, corner, facing, trade);
		return true;
	}

	private static void set(ServerLevel level, BlockPos at, char c, Direction facing,
	                        int x, int z, RandomSource random, Trade trade) {
		switch (c) {
			case '#' -> Blueprint.put(level, at, Blocks.COBBLESTONE.defaultBlockState());
			case 'W' -> Blueprint.put(level, at, plaster(random));
			case 'L' -> Blueprint.put(level, at, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
			case 'g' -> Blueprint.put(level, at, Blocks.GLASS_PANE.defaultBlockState());
			case 'D' -> door(level, at, facing);
			// The wall between shop and home, with a gap you can see through.
			case '_' -> Blueprint.put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			case 'P' -> Blueprint.put(level, at, Blocks.SPRUCE_PLANKS.defaultBlockState());
			// The counter, and the whole reason the room reads as a shop.
			case 'n' -> Blueprint.put(level, at, counter(trade));
			case 't' -> Blueprint.put(level, at, bench(trade)
				.setValue(BlockStateProperties.HORIZONTAL_FACING,
					Blueprint.turned(Direction.SOUTH, facing)));
			case 'A' -> Blueprint.barrel(level, at, random,
				com.bloomlet.herobrine.structure.Loot.Tier.TOWN_TRADE);
			case 'K' -> chest(level, at, facing, random);
			case 'd' -> bed(level, at, facing, random);
			case 'b' -> Blueprint.put(level, at, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.HALF, Half.TOP));
			default -> { }
		}
	}

	/**
	 * The trade decides three blocks and nothing else.
	 *
	 * A baker with a smoker and a composter and a weaver with a loom and wool
	 * are recognisably different shops from the street, and neither one needed
	 * a different floor plan to say so. Cheap, and it means the second shop in
	 * the town is never a copy of the first.
	 */
	private static BlockState bench(Trade trade) {
		return switch (trade) {
			case BAKER -> Blocks.SMOKER.defaultBlockState();
			case WEAVER -> Blocks.LOOM.defaultBlockState();
			case POTTER -> Blocks.SMOKER.defaultBlockState();
			case FLETCHER -> Blocks.FLETCHING_TABLE.defaultBlockState();
		};
	}

	private static BlockState counter(Trade trade) {
		return switch (trade) {
			case BAKER -> Blocks.SPRUCE_SLAB.defaultBlockState();
			case WEAVER -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN)
				.defaultBlockState();
			case POTTER -> Blocks.DECORATED_POT.defaultBlockState();
			case FLETCHER -> Blocks.SPRUCE_SLAB.defaultBlockState();
		};
	}

	/**
	 * Cloth over the window, out into the lane.
	 *
	 * The single most valuable six blocks in the building. Walls make a
	 * settlement; things that stick out OVER the road make a street, because
	 * they are what a player walks under rather than past.
	 */
	private static void awning(ServerLevel level, BlockPos corner, Direction facing,
	                           Trade trade) {
		net.minecraft.world.item.DyeColor cloth = switch (trade) {
			case BAKER -> net.minecraft.world.item.DyeColor.YELLOW;
			case WEAVER -> net.minecraft.world.item.DyeColor.LIGHT_BLUE;
			case POTTER -> net.minecraft.world.item.DyeColor.ORANGE;
			case FLETCHER -> net.minecraft.world.item.DyeColor.GREEN;
		};
		for (int x = 2; x <= 6; x++) {
			BlockPos at = corner.offset(
				Blueprint.spinX(x, 8, WIDTH, DEPTH, facing), 3,
				Blueprint.spinZ(x, 8, WIDTH, DEPTH, facing));
			Blueprint.put(level, at, Blocks.WOOL.pick(cloth).defaultBlockState());
		}
	}

	/** The same steep spruce gable every roof in the town has. */
	private static void gable(ServerLevel level, BlockPos corner, Direction facing) {
		for (int step = 0; step <= 4; step++) {
			for (int x = 0; x < WIDTH; x++) {
				for (int z : new int[] { step, DEPTH - 1 - step }) {
					if (z < 0 || z >= DEPTH) {
						continue;
					}
					BlockPos at = corner.offset(
						Blueprint.spinX(x, z, WIDTH, DEPTH, facing), 3 + step,
						Blueprint.spinZ(x, z, WIDTH, DEPTH, facing));
					Blueprint.put(level, at, Blocks.DEEPSLATE_TILES.defaultBlockState());
				}
			}
		}
	}

	private static void door(ServerLevel level, BlockPos at, Direction facing) {
		BlockState oak = Blocks.SPRUCE_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Blueprint.turned(Direction.SOUTH, facing));
		Blueprint.put(level, at, oak.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER));
		Blueprint.put(level, at.above(), oak.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER));
	}

	private static void bed(ServerLevel level, BlockPos at, Direction facing,
	                        RandomSource random) {
		net.minecraft.world.item.DyeColor[] plain = {
			net.minecraft.world.item.DyeColor.WHITE,
			net.minecraft.world.item.DyeColor.BROWN,
			net.minecraft.world.item.DyeColor.LIGHT_GRAY,
		};
		Direction head = Blueprint.turned(Direction.NORTH, facing);
		BlockState bed = Blocks.BED.pick(plain[random.nextInt(plain.length)])
			.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, head);
		Blueprint.put(level, at, bed.setValue(BlockStateProperties.BED_PART,
			net.minecraft.world.level.block.state.properties.BedPart.FOOT));
		Blueprint.put(level, at.relative(head), bed.setValue(BlockStateProperties.BED_PART,
			net.minecraft.world.level.block.state.properties.BedPart.HEAD));
	}

	private static void chest(ServerLevel level, BlockPos at, Direction facing,
	                          RandomSource random) {
		Blueprint.put(level, at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Blueprint.turned(Direction.SOUTH, facing)));
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			Loot.scatter(chest, random, Loot.Tier.TOWN_TRADE);
		}
	}

	private static BlockState plaster(RandomSource random) {
		return random.nextInt(6) == 0
			? Blocks.MUD_BRICKS.defaultBlockState()
			: Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE)
				.defaultBlockState();
	}
}
