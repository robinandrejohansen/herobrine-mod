package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * A MARKET STREET INSIDE THE WALLS, AND NOBODY HAS TRADED HERE IN YEARS.
 *
 * The bailey was reported as open, generic, empty rooms with nothing to explore,
 * and the diagnosis is right: the keep had a curtain wall, four towers, a hall
 * and about four thousand square blocks of paving with nothing standing on it.
 * A courtyard is not a room until something is in it.
 *
 * MEASURED OFF A REAL ONE. tools/castle reads the Legacy Console tutorial world;
 * TU19's castle has a market hall and this is built to its numbers. What a stall
 * is there, exactly:
 *
 *     a bay three wide, backed onto a wall
 *     TWO wall signs, two blocks apart, facing OUT into the street
 *     carpet on the floor — grey and brown, twenty-five and twenty-one of them
 *     an oak fence across the front, which is the counter
 *     a sample of the goods on the counter: coal blocks, cut sandstone,
 *       bookshelves, hay — a different commodity in every bay
 *     a torch
 *
 * The signs are the part worth copying. Two per bay facing opposite ways means a
 * shopper walking either direction reads the stall before they reach it, and it
 * is why a row of six bays reads as a market rather than as six alcoves.
 *
 * AND IT IS DEAD, WHICH IS THE ONE DELIBERATE CHANGE. A working market in his
 * keep would be the only place in the mod where somebody is having an ordinary
 * day. Everything else here is an institution that was in operation and then
 * stopped — the gaol, the church, the watch tower — and the stalls do far more
 * work empty: the counters are still up, the goods are still on them, the prices
 * are still chalked on the boards, and there is nobody behind any of it.
 *
 * The goods are the loot. That is the exploration the courtyard did not have.
 */
public final class TheShambles {
	private TheShambles() {}

	/** Half-width of the street between the two rows. */
	private static final int STREET = 3;

	/** A bay is this wide and this deep. */
	private static final int BAY = 3;
	private static final int DEEP = 4;

	/**
	 * How many bays per side, the gap between them, and where the row starts.
	 *
	 * FOUR, AND THE NUMBER IS THE KEEP'S DOING. The street runs from the gate at
	 * the wall straight in toward the middle, and the keep is eleven blocks
	 * half-width at the centre of the same axis: FROM_GATE + BAYS * (BAY + GAP) is
	 * the length, and it has to land no closer than that eleven.
	 *
	 * Four bays and a three-block approach makes nineteen, which stops the paving
	 * exactly on the keep's own wall — a market street that ends at the great hall
	 * door, which is where a market street in a bailey should end. Five bays
	 * overshoots by four blocks and paves through the building.
	 */
	private static final int BAYS = 4;
	private static final int GAP = 1;
	private static final int FROM_GATE = 3;

	/**
	 * What was sold, what it looks like, and what is still in the chest.
	 *
	 * One commodity per bay, exactly as TU19 does it — a stall that displays the
	 * same thing as the one next door is a shelf, not a market. The display block
	 * sits on the counter and the chest under it holds the rest, so a player who
	 * reads a bay from the street already knows whether it is worth walking into.
	 */
	private record Trade(String[] board, BlockState shown, ItemStack kept) {}

	private static Trade[] trades(RandomSource random) {
		return new Trade[] {
			new Trade(new String[] { "COAL", "BY THE", "BASKET", "ASK INSIDE" },
				Blocks.COAL_BLOCK.defaultBlockState(),
				new ItemStack(Items.COAL, 12 + random.nextInt(20))),
			new Trade(new String[] { "BREAD", "AND", "WHAT ELSE", "THERE IS" },
				Blocks.HAY_BLOCK.defaultBlockState(),
				new ItemStack(Items.BREAD, 3 + random.nextInt(6))),
			new Trade(new String[] { "IRON", "NAILS", "HINGES", "NO CREDIT" },
				Blocks.IRON_BLOCK.defaultBlockState(),
				new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(7))),
			new Trade(new String[] { "PAPER", "INK", "AND", "COPYING" },
				Blocks.BOOKSHELF.defaultBlockState(),
				new ItemStack(Items.PAPER, 6 + random.nextInt(12))),
			new Trade(new String[] { "STONE", "CUT TO", "ANY", "SIZE" },
				Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
				new ItemStack(Items.STONE_BRICKS, 8 + random.nextInt(16))),
			new Trade(new String[] { "WOOL", "DYED", "OR", "PLAIN" },
				// Wool is a ColorCollection in 26.2, the way BED and CARPET are —
				// there is no Blocks.WHITE_WOOL to reach for.
				Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE)
					.defaultBlockState(),
				new ItemStack(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE),
					4 + random.nextInt(8))),
			new Trade(new String[] { "LAMP OIL", "TORCHES", "TALLOW", "BY WEIGHT" },
				Blocks.GLOWSTONE.defaultBlockState(),
				new ItemStack(Items.TORCH, 10 + random.nextInt(20))),
			new Trade(new String[] { "SEED", "AND", "ROOT", "SPRING ONLY" },
				Blocks.PUMPKIN.defaultBlockState(),
				new ItemStack(Items.WHEAT_SEEDS, 6 + random.nextInt(10))),
			new Trade(new String[] { "GLASS", "PANES", "MENDED", "OR NEW" },
				Blocks.GLASS.defaultBlockState(),
				new ItemStack(Items.GLASS, 4 + random.nextInt(8))),
			new Trade(new String[] { "CLOSED", "—", "ASK AT", "THE HALL" },
				Blocks.COBWEB.defaultBlockState(), ItemStack.EMPTY),
			new Trade(new String[] { "NO MEAT", "THIS WEEK", "OR", "NEXT" },
				Blocks.COBWEB.defaultBlockState(), ItemStack.EMPTY),
			new Trade(new String[] { "GONE", "TO THE", "CHURCH", "DO NOT WAIT" },
				Blocks.COBWEB.defaultBlockState(), ItemStack.EMPTY),
		};
	}

	/**
	 * Lay the street out from the gate, inward.
	 *
	 * `gate` is the middle of the gateway on the wall and `into` points at the
	 * courtyard, so this works whichever way the gate ends up facing without the
	 * caller having to think about it.
	 */
	/** Whether a block is inside the walls. See Keep.inside — the shape varies. */
	public interface Fits {
		boolean at(BlockPos where);
	}

	public static void lay(ServerLevel his, BlockPos gate, Direction into, Fits fits,
	                       RandomSource random) {
		Direction across = into.getClockWise();
		Trade[] all = trades(random);
		int put = 0;

		// The paving between the rows, so the street reads as a street before a
		// single stall is up.
		// ONE SHORT AT BOTH ENDS, and both ends were wrong.
		//
		// At the gate: the street is seven wide and the arch is five, so paving at
		// along 0 cut a notch in the curtain either side of the gateway.
		//
		// At the hall: the run is FROM_GATE + BAYS * (BAY + GAP) = nineteen, and the
		// keep is eleven half-width on the same axis — so the last course landed
		// exactly on the keep's south wall. The street is laid at stage 30, after
		// keep() at 22, so it replaced seven blocks of the great hall with paving
		// and left a hole straight into it.
		for (int along = 1; along < FROM_GATE + BAYS * (BAY + GAP); along++) {
			for (int side = -STREET; side <= STREET; side++) {
				BlockPos at = gate.relative(into, along).relative(across, side);
				his.setBlock(at, along % 7 == 0
					? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
					: Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
				for (int up = 1; up <= 4; up++) {
					his.setBlock(at.above(up), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}

		for (int i = 0; i < BAYS; i++) {
			int along = FROM_GATE + i * (BAY + GAP);
			for (int hand = -1; hand <= 1; hand += 2) {
				BlockPos front = gate.relative(into, along)
					.relative(across, hand * (STREET + 1));
				// AND IT HAS TO FIT INSIDE THE WALL.
				//
				// The circuit is a different reach on every bearing and the gate
				// corner is pinned to WALL, so the two runs either side of the gate
				// routinely come in nearer than that — on one seed they sit at 23,
				// and a bay whose back is eight blocks out from the street then goes
				// straight through the rampart.
				//
				// EVERY BLOCK OF THE FOOTPRINT, NOT THE CORNERS.
				//
				// Testing the two back corners left twenty-nine clipped bays across
				// three hundred seeds, and the reason is that the circuit is CONCAVE:
				// reaches run from 22 to 50, so a run can cut across the middle of a
				// bay while both of its far corners sit comfortably inside. Corner
				// testing is only sound against a convex boundary.
				//
				// Fifteen point-in-polygon tests per bay. A bay that does not fit is
				// skipped rather than clipped — half a stall buried in a rampart is
				// worse than a gap in the row, and the row is meant to look like a
				// street that lost tenants anyway.
				Direction out = hand > 0 ? across.getOpposite() : across;
				boolean room = true;
				for (int w = 0; w < BAY && room; w++) {
					for (int d = 0; d <= DEEP && room; d++) {
						room = fits.at(front.relative(into, w)
							.relative(out.getOpposite(), d));
					}
				}
				if (!room) {
					continue;
				}
				Trade trade = all[(i * 2 + (hand > 0 ? 1 : 0)) % all.length];
				bay(his, front, into, across, hand, trade, random);
				put++;
			}
		}
		HerobrineMod.LOGGER.info("the shambles: {} of {} stalls fit off the gate at [{}, {}, {}]",
			put, BAYS * 2, gate.getX(), gate.getY(), gate.getZ());
	}

	/**
	 * One stall. `hand` is -1 for the row on the left of the street and +1 for the
	 * right, and it flips which way the signs face and which way the bay is dug.
	 */
	private static void bay(ServerLevel his, BlockPos front, Direction into,
	                        Direction across, int hand, Trade trade,
	                        RandomSource random) {
		Direction out = hand > 0 ? across.getOpposite() : across;
		Direction back = out.getOpposite();

		// The shell: three wide, four deep, open to the street.
		for (int w = 0; w < BAY; w++) {
			for (int d = 0; d <= DEEP; d++) {
				BlockPos floor = front.relative(into, w).relative(back, d);
				boolean shell = d == DEEP || w == 0 || w == BAY - 1;
				his.setBlock(floor, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
				for (int up = 1; up <= 4; up++) {
					BlockPos at = floor.above(up);
					if (up == 4 || (shell && d > 0)) {
						his.setBlock(at, up == 4
							? Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState()
								.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					} else {
						his.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		// Carpet, because it is the cheapest thing that says somebody chose this
		// floor. Grey and brown, which is what the real ones use.
		BlockState rug = (random.nextBoolean()
			? Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.GRAY)
			: Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN))
			.defaultBlockState();
		for (int d = 1; d < DEEP; d++) {
			his.setBlock(front.relative(into, 1).relative(back, d).above(), rug, 2);
		}

		// THE COUNTER, which is the whole silhouette of a stall.
		BlockPos counter = front.relative(into, 1);
		his.setBlock(counter.above(), Blocks.OAK_FENCE.defaultBlockState(), 2);
		his.setBlock(counter.above(2), trade.shown(), 2);

		// TWO BOARDS, FACING OPPOSITE WAYS, two blocks apart — the measured detail
		// that makes a row of these read as a market from either end of the street.
		board(his, front.above(2), out, trade.board());
		board(his, front.relative(into, 2).above(2), out, trade.board());

		his.setBlock(front.relative(into, 1).relative(back, DEEP - 1).above(3),
			Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);

		// What is left of the stock. Placed LAST, after every wall of this bay is
		// up — the rule Warren states and three files in this package have broken.
		if (trade.kept().isEmpty()) {
			his.setBlock(front.relative(into, 1).relative(back, 2).above(),
				Blocks.COBWEB.defaultBlockState(), 2);
			return;
		}
		BlockPos crate = front.relative(into, 1).relative(back, DEEP - 1);
		his.setBlock(crate, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, out), 2);
		if (his.getBlockEntity(crate) instanceof ChestBlockEntity chest) {
			chest.setItem(0, trade.kept());
			Loot.scatter(chest, random, Loot.Tier.HIS_CITY);
		}
	}

	private static void board(ServerLevel his, BlockPos at, Direction faces,
	                          String[] lines) {
		his.setBlock(at, Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, faces), 2);
		if (his.getBlockEntity(at) instanceof SignBlockEntity sign) {
			SignText text = new SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, Component.literal(
					row < lines.length ? lines[row] : ""));
			}
			sign.setText(text, true);
		}
	}
}
