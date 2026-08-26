package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * WHAT RUNS BETWEEN HIS TWO BUILDINGS, AND SOMEBODY BUILT IT.
 *
 * The first version of this wandered — an organic bore with a breathing radius,
 * on the theory that a straight tunnel reads as plumbing. It does, and the
 * conclusion was still wrong: what a wandering tunnel reads as is a CAVE, and a
 * cave is a thing that happened rather than a thing anybody did.
 *
 * The reference is architecture. Pillars, a beamed ceiling, a row of hanging
 * lanterns down the middle, a floor somebody laid in a pattern, and growth coming
 * through it. That is a much worse place to find than a cave, and the reason is
 * simple: ruin needs something to have ruined. Nobody is unsettled by a hole.
 * Everybody is unsettled by a hall.
 *
 * SO IT IS LAID OUT LIKE A PLAN, not walked like a path. Axis-aligned legs with
 * square turns, the way anybody digging with a purpose would cut it — and each
 * turn is a chamber, because that is where a corridor that was designed puts its
 * junctions.
 *
 * THE HORROR GOES ON TOP OF THE ARCHITECTURE RATHER THAN INSTEAD OF IT. Every
 * course is mixed with its cracked and mossy variants, so the whole thing is
 * old before anything else happens to it. Then the growth comes through the
 * ceiling, then the webs, then the meat.
 */
public final class Passage {
	private Passage() {}

	/** Five wide and four high — a corridor two people pass in, not a crawl. */
	private static final int HALF_WIDE = 2;
	private static final int TALL = 4;
	/** How long a straight run gets before it turns. */
	private static final int LEG_MIN = 11;
	private static final int LEG_SPREAD = 14;
	/** Pillars this far apart along both walls. */
	private static final int BAY = 3;
	/** And a lantern every this many, down the centre line. */
	private static final int LAMP_BAY = 5;
	private static final int MOST_LEGS = 40;

	public static boolean bore(ServerLevel level, BlockPos from, BlockPos to,
	                           @org.jspecify.annotations.Nullable BlockPos through,
	                           RandomSource random) {
		List<BlockPos> corners = new ArrayList<>();
		BlockPos at = from;
		BlockPos aim = through == null ? to : through;
		boolean bent = through == null;

		for (int leg = 0; leg < MOST_LEGS; leg++) {
			int dx = aim.getX() - at.getX();
			int dz = aim.getZ() - at.getZ();
			// SIX SHORT OF THE CELLS, NOT ON TOP OF THEM.
			//
			// Undercroft.dig already cut a prison down there, and a five-wide
			// corridor arriving at its exact coordinates would carve straight
			// through the bars and out the far side — a passage that demolishes the
			// thing it exists to reach. Stopping short and cutting the last few
			// blocks as a doorway leaves both intact and makes the arrival read as
			// breaking IN.
			int stop = bent ? 6 : 4;
			if (Math.abs(dx) < stop && Math.abs(dz) < stop) {
				if (!bent) {
					bent = true;
					aim = to;
					continue;
				}
				doorway(level, at, aim, random);
				hall(level, at, random);
				HerobrineMod.LOGGER.info(
					"a passage runs from [{}, {}, {}] to [{}, {}, {}] — {} legs, {} chambers",
					from.getX(), from.getY(), from.getZ(),
					at.getX(), at.getY(), at.getZ(), leg, corners.size());
				return true;
			}

			// THE LONGER AXIS FIRST, which is how anybody lays out a passage they
			// have to finish: get the distance done, then square up. It also makes
			// the turns land in the middle rather than all at one end.
			Direction heading = Math.abs(dx) >= Math.abs(dz)
				? (dx > 0 ? Direction.EAST : Direction.WEST)
				: (dz > 0 ? Direction.SOUTH : Direction.NORTH);
			int want = Math.min(
				Math.max(Math.abs(dx), Math.abs(dz)),
				LEG_MIN + random.nextInt(LEG_SPREAD));

			// And it climbs or drops across the leg rather than in a step, so the
			// floor is a ramp somebody cut and not a staircase bolted on.
			int rise = Integer.compare(aim.getY(), at.getY());
			at = run(level, at, heading, want, rise, random);
			corners.add(at);
			chamber(level, at, random);
		}
		HerobrineMod.LOGGER.info("a passage ran out of legs before it arrived");
		return false;
	}

	/**
	 * One straight run of finished corridor.
	 *
	 * Everything that makes it read as built happens here: the bays, the beams and
	 * the lantern line. Each is on its own count so they fall out of step with one
	 * another down the length, which is what stops a long corridor looking tiled.
	 */
	private static BlockPos run(ServerLevel level, BlockPos start, Direction heading,
	                            int length, int rise, RandomSource random) {
		Direction across = heading.getClockWise();
		BlockPos at = start;
		for (int step = 0; step < length; step++) {
			at = at.relative(heading);
			if (rise != 0 && step % 3 == 2) {
				at = at.above(rise);
			}
			slice(level, at, across, random);
			if (step % BAY == 0) {
				bay(level, at, across, random);
			}
			if (step % LAMP_BAY == 2) {
				lamp(level, at);
			}
		}
		return at;
	}

	/** Hollow it, floor it, roof it. */
	private static void slice(ServerLevel level, BlockPos middle, Direction across,
	                          RandomSource random) {
		for (int side = -HALF_WIDE; side <= HALF_WIDE; side++) {
			BlockPos column = middle.relative(across, side);
			for (int up = 0; up < TALL; up++) {
				clear(level, column.above(up));
			}
			// THE FLOOR IS A PATTERN, which is the single clearest signal that
			// somebody laid it. Polished andesite on a two-by-two grid against
			// stone brick, and the grid is read off world coordinates so it stays
			// continuous through every turn instead of restarting at each leg.
			boolean accent = ((column.getX() >> 1) + (column.getZ() >> 1)) % 2 == 0;
			put(level, column.below(), accent && Math.abs(side) < HALF_WIDE
				? Blocks.POLISHED_ANDESITE.defaultBlockState()
				: aged(random));
			// Beams across, planks between: an actual ceiling rather than a lid.
			BlockPos roof = column.above(TALL);
			boolean beam = (middle.getX() + middle.getZ()) % BAY == 0;
			put(level, roof, beam
				? Blocks.DARK_OAK_LOG.defaultBlockState().setValue(
					BlockStateProperties.AXIS, across.getAxis())
				: Blocks.DARK_OAK_PLANKS.defaultBlockState());
			growth(level, roof.below(), random);
		}
		// And walls, so it is a room rather than a hole in the stone.
		for (int side : new int[] { -HALF_WIDE - 1, HALF_WIDE + 1 }) {
			BlockPos wall = middle.relative(across, side);
			for (int up = -1; up <= TALL; up++) {
				fill(level, wall.above(up), aged(random));
			}
		}
	}

	/**
	 * A PAIR OF PILLARS, flared at the foot and again under the beam.
	 *
	 * The stairs at top and bottom are the whole difference between a column and a
	 * stack of blocks, and they are what the reference has at every bay.
	 */
	private static void bay(ServerLevel level, BlockPos middle, Direction across,
	                        RandomSource random) {
		for (int side : new int[] { -HALF_WIDE, HALF_WIDE }) {
			BlockPos foot = middle.relative(across, side);
			for (int up = 0; up < TALL; up++) {
				put(level, foot.above(up), random.nextInt(6) == 0
					? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
					: Blocks.STONE_BRICKS.defaultBlockState());
			}
			Direction inward = side < 0 ? across : across.getOpposite();
			flare(level, foot, inward, false);
			flare(level, foot.above(TALL - 1), inward, true);
			// SET INTO THE WALL, NOT STUCK ON IT.
			//
			// A skull floating in the air off a pillar is the single clearest
			// "a mod placed this" signal there is — real ones sit in something.
			// So a niche is cut a block back into the masonry and the head sits at
			// the bottom of it, in shadow, which also means you only find them by
			// looking rather than by walking past.
			if (random.nextInt(3) == 0) {
				niche(level, foot.above(1), inward.getOpposite(), random);
			}
		}
	}

	/**
	 * A hollow cut back into the stone with something in the bottom of it.
	 *
	 * @param into which way the wall is, from the corridor
	 */
	private static void niche(ServerLevel level, BlockPos at, Direction into,
	                          RandomSource random) {
		BlockPos back = at.relative(into);
		if (!level.getBlockState(back).isSolid()) {
			return;
		}
		level.setBlock(back, Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(back.above(), Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(back.below(), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
		BlockState inside = switch (random.nextInt(6)) {
			case 0, 1 -> Blocks.SKELETON_SKULL.defaultBlockState();
			case 2 -> Blocks.ZOMBIE_HEAD.defaultBlockState();
			case 3 -> Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
			case 4 -> Blocks.CANDLE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.CandleBlock.LIT, true);
			default -> Blocks.BONE_BLOCK.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
		};
		level.setBlock(back, inside, 2);
	}

	private static void flare(ServerLevel level, BlockPos at, Direction inward, boolean top) {
		BlockPos step = at.relative(inward);
		if (!level.getBlockState(step).isAir()) {
			return;
		}
		level.setBlock(step, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, inward.getOpposite())
			.setValue(BlockStateProperties.HALF, top ? Half.TOP : Half.BOTTOM), 2);
	}

	private static void lamp(ServerLevel level, BlockPos middle) {
		BlockPos at = middle.above(TALL - 1);
		if (level.getBlockState(at).isAir()) {
			level.setBlock(at, Blocks.LANTERN.defaultBlockState()
				.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
		}
	}

	/**
	 * WHAT IS COMING THROUGH THE CEILING.
	 *
	 * Three things, in descending order of how often anybody should see them. Moss
	 * everywhere, because water gets in; webs where nobody reaches; and rarely, a
	 * patch of nether wart block, which is the only material in the game that reads
	 * as meat and needs no explanation at all hanging over a corridor.
	 */
	private static void growth(ServerLevel level, BlockPos under, RandomSource random) {
		if (!level.getBlockState(under).isAir()) {
			return;
		}
		int roll = random.nextInt(14);
		if (roll == 0) {
			level.setBlock(under, Blocks.PALE_HANGING_MOSS.defaultBlockState(), 2);
		} else if (roll == 1) {
			level.setBlock(under, Blocks.COBWEB.defaultBlockState(), 2);
		} else if (roll == 2) {
			level.setBlock(under.above(), Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
		}
	}

	/**
	 * The last few blocks into the undercroft, cut narrow.
	 *
	 * Two wide and three high rather than the corridor's five, so the passage necks
	 * down as it arrives — which is both how a real connection between two
	 * separately-dug things looks, and the only way to reach the cells without
	 * taking their walls with it.
	 */
	private static void doorway(ServerLevel level, BlockPos from, BlockPos to,
	                            RandomSource random) {
		int steps = Math.max(1, (int) Math.sqrt(from.distSqr(to)));
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			BlockPos at = BlockPos.containing(
				from.getX() + (to.getX() - from.getX()) * t,
				from.getY() + (to.getY() - from.getY()) * t,
				from.getZ() + (to.getZ() - from.getZ()) * t);
			for (int dy = 0; dy < 3; dy++) {
				for (int d = 0; d <= 1; d++) {
					clear(level, at.offset(d, dy, 0));
					clear(level, at.offset(0, dy, d));
				}
			}
			put(level, at.below(), aged(random));
		}
	}

	/** Where the legs meet: wider, taller, and worth stopping in. */
	private static void chamber(ServerLevel level, BlockPos middle, RandomSource random) {
		int r = 4;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				boolean wall = Math.abs(dx) == r || Math.abs(dz) == r;
				for (int up = 0; up < TALL + 1; up++) {
					BlockPos at = middle.offset(dx, up, dz);
					if (wall) {
						fill(level, at, aged(random));
					} else {
						clear(level, at);
					}
				}
				boolean accent = (Math.abs(dx) + Math.abs(dz)) % 3 == 0;
				put(level, middle.offset(dx, -1, dz), accent
					? Blocks.POLISHED_ANDESITE.defaultBlockState() : aged(random));
				put(level, middle.offset(dx, TALL + 1, dz),
					Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		// Four corner columns, which is what makes a square room read as a hall.
		for (int cx : new int[] { -2, 2 }) {
			for (int cz : new int[] { -2, 2 }) {
				for (int up = 0; up < TALL; up++) {
					put(level, middle.offset(cx, up, cz), Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
		}
		lamp(level, middle);
		// THE CHEST IS NOT IN THE MIDDLE OF THE ROOM.
		//
		// Standing one in the open makes the chamber a vending machine: you arrive,
		// you see it, you take it, you leave, and nothing about the room was worth
		// looking at. Set back in a side alcove behind the corner columns it has to
		// be FOUND, which is the difference between loot and a discovery — and it
		// means players who search the walls are rewarded for it everywhere else in
		// the passage too.
		Direction off = Direction.from2DDataValue(random.nextInt(4));
		BlockPos alcove = middle.relative(off, 3);
		for (int up = 0; up < 3; up++) {
			BlockPos at = alcove.above(up);
			if (diggable(level, at)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			}
		}
		put(level, alcove.below(), Blocks.POLISHED_ANDESITE.defaultBlockState());
		put(level, alcove.above(3), Blocks.STONE_BRICK_SLAB.defaultBlockState());
		if (level.getBlockState(alcove.below()).isSolid()) {
			level.setBlock(alcove, Blocks.CHEST.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ChestBlock.FACING,
					off.getOpposite()), 2);
			if (level.getBlockEntity(alcove) instanceof BaseContainerBlockEntity box) {
				stash(level, box, random);
			}
		}
		// And a curtain of vine over the mouth of it, so from the corridor it is a
		// shadow rather than a chest.
		BlockPos mouth = middle.relative(off, 2).above(2);
		if (level.getBlockState(mouth).isAir() && random.nextBoolean()) {
			level.setBlock(mouth, Blocks.PALE_HANGING_MOSS.defaultBlockState(), 2);
		}
	}

	/**
	 * WHAT IS ACTUALLY IN THEM, AND IT IS NOT A LOOT TABLE.
	 *
	 * Loot.scatter draws from a fixed pool per tier, which is right for a farmhouse
	 * barrel — a farmhouse should hold bread and string and nothing else. It is
	 * wrong for the one place in the mod that is meant to be worth a long walk in
	 * the dark: the pool has no weapons in it, no armour, and every chest is
	 * therefore the same chest.
	 *
	 * So these are rolled rather than tabled. A weapon or a tool, in any material,
	 * ENCHANTED through vanilla's own enchantItem — so what comes out is a real
	 * roll a player could have got from their own table, with real names on it, and
	 * two chests are never alike.
	 *
	 * AND COLOUR, which is the part a loot table can never give you. Dyed leather
	 * is the only equipment in Minecraft with a free variable in it, so every piece
	 * comes out a different shade and a chest full of it reads as somebody's
	 * belongings rather than as a drop.
	 */
	private static void stash(ServerLevel level, BaseContainerBlockEntity box,
	                          RandomSource random) {
		net.minecraft.core.RegistryAccess access = level.registryAccess();
		int slot = 0;
		int pieces = 3 + random.nextInt(4);
		for (int i = 0; i < pieces && slot < box.getContainerSize() - 2; i++) {
			net.minecraft.world.item.ItemStack out = switch (random.nextInt(3)) {
				case 0 -> new net.minecraft.world.item.ItemStack(
					BLADES[random.nextInt(BLADES.length)]);
				case 1 -> new net.minecraft.world.item.ItemStack(
					PLATE[random.nextInt(PLATE.length)]);
				default -> dyed(random);
			};
			// Not everything. Three in four, so an unenchanted piece still turns up
			// and the enchanted ones stay worth something.
			if (random.nextInt(4) != 0) {
				com.bloomlet.herobrine.manifest.HisHost.enchant(out, random, access,
					10 + random.nextInt(24));
			}
			box.setItem(slot++, out);
		}
		// And something ordinary underneath it, because a chest of nothing but
		// weapons is a reward and a chest with somebody's candles in it is a life.
		for (int i = 0; i < 3 && slot < box.getContainerSize(); i++) {
			box.setItem(slot++, new net.minecraft.world.item.ItemStack(
				SUNDRIES[random.nextInt(SUNDRIES.length)], 1 + random.nextInt(6)));
		}
		box.setChanged();
	}

	/** Any material. A gold sword with a good roll on it beats a plain diamond. */
	private static final net.minecraft.world.item.Item[] BLADES = {
		net.minecraft.world.item.Items.IRON_SWORD,
		net.minecraft.world.item.Items.DIAMOND_SWORD,
		net.minecraft.world.item.Items.GOLDEN_SWORD,
		net.minecraft.world.item.Items.IRON_AXE,
		net.minecraft.world.item.Items.DIAMOND_AXE,
		net.minecraft.world.item.Items.IRON_PICKAXE,
		net.minecraft.world.item.Items.DIAMOND_PICKAXE,
		net.minecraft.world.item.Items.BOW,
		net.minecraft.world.item.Items.CROSSBOW,
		net.minecraft.world.item.Items.TRIDENT,
	};

	private static final net.minecraft.world.item.Item[] PLATE = {
		net.minecraft.world.item.Items.IRON_HELMET,
		net.minecraft.world.item.Items.IRON_CHESTPLATE,
		net.minecraft.world.item.Items.IRON_LEGGINGS,
		net.minecraft.world.item.Items.IRON_BOOTS,
		net.minecraft.world.item.Items.DIAMOND_HELMET,
		net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
		net.minecraft.world.item.Items.DIAMOND_BOOTS,
		net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE,
		net.minecraft.world.item.Items.SHIELD,
	};

	private static final net.minecraft.world.item.Item[] LEATHER = {
		net.minecraft.world.item.Items.LEATHER_HELMET,
		net.minecraft.world.item.Items.LEATHER_CHESTPLATE,
		net.minecraft.world.item.Items.LEATHER_LEGGINGS,
		net.minecraft.world.item.Items.LEATHER_BOOTS,
	};

	private static final net.minecraft.world.item.Item[] SUNDRIES = {
		net.minecraft.world.item.Items.CANDLE,
		net.minecraft.world.item.Items.BONE,
		net.minecraft.world.item.Items.STRING,
		net.minecraft.world.item.Items.GOLD_INGOT,
		net.minecraft.world.item.Items.LAPIS_LAZULI,
		net.minecraft.world.item.Items.GLASS_BOTTLE,
		net.minecraft.world.item.Items.PAPER,
		net.minecraft.world.item.Items.EMERALD,
	};

	/** Every piece a different shade, and none of them a colour anybody chose. */
	private static net.minecraft.world.item.ItemStack dyed(RandomSource random) {
		net.minecraft.world.item.ItemStack piece =
			new net.minecraft.world.item.ItemStack(LEATHER[random.nextInt(LEATHER.length)]);
		// Muted on purpose: two of the three channels are pulled down, so what comes
		// out is dust, rust, moss and dried blood rather than a dye-shop rainbow.
		int r = 60 + random.nextInt(150);
		int g = 40 + random.nextInt(110);
		int b = 35 + random.nextInt(90);
		piece.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
			new net.minecraft.world.item.component.DyedItemColor((r << 16) | (g << 8) | b));
		return piece;
	}

	/** The last one, at the far end, and larger than the rest. */
	private static void hall(ServerLevel level, BlockPos middle, RandomSource random) {
		chamber(level, middle, random);
		for (int dx = -1; dx <= 1; dx++) {
			put(level, middle.offset(dx, 0, -3), Blocks.OAK_FENCE.defaultBlockState());
			BlockPos head = middle.offset(dx, 1, -3);
			if (level.getBlockState(head).isAir()) {
				level.setBlock(head, Blocks.SKELETON_SKULL.defaultBlockState(), 2);
			}
		}
	}

	/** Stone brick and its two failures, so nothing is ever one flat colour. */
	private static BlockState aged(RandomSource random) {
		return switch (random.nextInt(10)) {
			case 0, 1, 2 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			case 3, 4 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			case 5 -> Blocks.COBBLESTONE.defaultBlockState();
			default -> Blocks.STONE_BRICKS.defaultBlockState();
		};
	}

	private static void clear(ServerLevel level, BlockPos at) {
		if (diggable(level, at)) {
			level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
		}
	}

	/** Only into solid rock — never wall off a cave he could have walked through. */
	private static void fill(ServerLevel level, BlockPos at, BlockState what) {
		if (!level.getBlockState(at).isAir() && diggable(level, at)) {
			level.setBlock(at, what, 2);
		}
	}

	private static void put(ServerLevel level, BlockPos at, BlockState what) {
		if (diggable(level, at)) {
			level.setBlock(at, what, 2);
		}
	}

	private static boolean diggable(ServerLevel level, BlockPos at) {
		return level.getBlockState(at).getDestroySpeed(level, at) >= 0.0F
			&& !com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, at);
	}
}
