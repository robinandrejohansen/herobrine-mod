package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * THE BROKEN TOWER, AND THE DOOR AT THE TOP OF IT THAT DOES NOT WORK.
 *
 * The portal used to appear where he died, which is a fine ending and a wasted
 * fifteen hours: nobody knows the ending exists until it is standing in front of
 * them, so the whole game is spent with nothing on the horizon.
 *
 * This is on the horizon from the first day. It is near his house, it is taller
 * than the trees, and the map handed to the first player leads within sight of
 * it. They will climb it in the first hour — that is the intention, not a leak —
 * and what they find at the top is a portal in two halves that cannot reach each
 * other.
 *
 * THE GAP IS THE GATE, AND IT IS THE ONLY KIND WORTH HAVING. No locked door, no
 * message, no "you are not worthy": the top of the tower has come off and is
 * hanging in the air above the rest of it, and the frame is cut in half between
 * them. A player looks at that for four seconds and understands the entire shape
 * of the mod — there is a way out, it is broken, and something has to change.
 *
 * WHAT CHANGES IS HIM. Killing him brings it down — see join. Which turns the
 * last fight from "the final boss" into "the door", and means every glance at the
 * skyline for the fifteen hours before it was a glance at the reason.
 *
 * OVERGROWN, because it has been failing here for a long time and neither of the
 * two of you built it.
 */
public final class Spire {
	private Spire() {}

	/** Where the lower half of the frame stands, once raised. */
	private static final AttachmentType<Long> SITE = AttachmentRegistry
		.createPersistent(HerobrineMod.id("spire_site"), Codec.LONG);
	/** Whether the halves have been brought together. */
	private static final AttachmentType<Boolean> JOINED = AttachmentRegistry
		.createPersistent(HerobrineMod.id("spire_joined"), Codec.BOOL);

	/**
	 * IT TAPERS. Nine across at the foot, seven, then five at the deck.
	 *
	 * A tower of one width is a chimney. The step-in at each tier is what gives it
	 * a silhouette from a distance, and each step leaves a LEDGE — which is where
	 * all the detail then goes: the cornice under it, the growth on top of it, and
	 * the shadow it throws down the face below.
	 */
	private static final int[] TIER_HALF = { 4, 3, 2 };
	private static final int[] TIER_RISE = { 11, 10, 8 };
	private static final int RISE = 29;
	/** How much air sits between the two halves. Enough to be a distance. */
	private static final int GAP = 7;

	public static @org.jspecify.annotations.Nullable BlockPos site(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(SITE);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * The chest at the very top, if the tower has been built.
	 *
	 * Derived rather than stored, because it is nothing but arithmetic on the deck:
	 * the island sits GAP + 1 above it and the chest sits one above the island
	 * floor. Storing it would be a second copy of a number that already exists in
	 * two places, and the third copy is always the one that goes stale.
	 */
	public static @org.jspecify.annotations.Nullable BlockPos wings(ServerLevel level) {
		BlockPos deck = site(level);
		return deck == null ? null : deck.above(GAP + 2);
	}

	public static boolean joined(ServerLevel level) {
		return Boolean.TRUE.equals(level.getServer().overworld().getAttached(JOINED));
	}

	public static @org.jspecify.annotations.Nullable BlockPos raise(
			ServerLevel level, BlockPos near, RandomSource random) {
		if (site(level) != null) {
			return site(level);
		}
		BlockPos base = footing(level, near, random);
		tower(level, base, random);
		BlockPos deck = base.above(RISE);
		lower(level, deck, random);
		between(level, deck, random);
		island(level, deck.above(GAP + 1), random);
		// AND IT IS OPEN FROM THE FIRST DAY.
		//
		// The portal used to be the reward for killing him — the halves came
		// together and the way out appeared. That is a fine ending and it is the
		// wrong story now: he does not LIVE here. He lives through there, and this
		// is somewhere he comes. A door he uses cannot be a door that is broken.
		//
		// THE GAP STAYS, and that is the good part. TheWay.open builds its own
		// frame, so it needs nothing from the tower's missing lintel — which means
		// the silhouette survives intact. The top of this thing is still torn off
		// and hanging in the air above the rest of it. It just stopped meaning "the
		// way out is broken" and started meaning something far worse: something
		// took the top off this, and the door still works.
		// AND THE DOOR IS NOT UP HERE ANY MORE — see Sanctum.
		//
		// On the deck it was the most public thing on the map, and it could only be
		// wherever the tower could be stood: a coast, a crag, occasionally nowhere.
		// Under his house it is the same room in the same building every time.
		//
		// The tower keeps everything that made it worth building. It is still the
		// thing you see from off his land and still the reason to walk over — it
		// simply turns out to be a marker rather than a way through, and what it
		// marks is underneath it.
		level.getServer().overworld().setAttached(JOINED, true);
		level.getServer().overworld().setAttached(SITE, deck.asLong());
		HerobrineMod.LOGGER.info(
			"a broken tower stands at [{}, {}, {}] — the way out, in two halves",
			deck.getX(), deck.getY(), deck.getZ());
		return deck;
	}

	/**
	 * SOMEWHERE TO STAND IT, AND THERE IS ALWAYS SOMEWHERE.
	 *
	 * This used to take sixteen guesses at dry land and give up, which on a coast
	 * means no tower — and no tower means no portal, no skyline, and no reason to
	 * look up for the whole game. The one thing the mod cannot afford to leave out
	 * was the one thing allowed to fail quietly.
	 *
	 * So it looks harder: two rings outward, land first, near before far. And if
	 * the answer really is that he lives on a shore and everything within a
	 * hundred and sixty blocks is sea, it stands the thing IN the sea, which was
	 * always the better picture anyway.
	 */
	private static BlockPos footing(ServerLevel level, BlockPos near, RandomSource random) {
		for (int ring = 0; ring < 2; ring++) {
			double from = 60.0 + ring * 50.0;
			for (int attempt = 0; attempt < 14; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double range = from + random.nextDouble() * 50.0;
				int x = near.getX() + (int) Math.round(Math.cos(angle) * range);
				int z = near.getZ() + (int) Math.round(Math.sin(angle) * range);
				level.getChunk(x >> 4, z >> 4);
				if (Ground.dry(level, x, z)) {
					return new BlockPos(x, Ground.topOf(level, x, z), z);
				}
			}
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = 70.0 + random.nextDouble() * 40.0;
		int x = near.getX() + (int) Math.round(Math.cos(angle) * range);
		int z = near.getZ() + (int) Math.round(Math.sin(angle) * range);
		level.getChunk(x >> 4, z >> 4);
		HerobrineMod.LOGGER.info("no dry land for the tower — it comes out of the water instead");
		return crag(level, x, z, random);
	}

	/**
	 * THE ROCK IT COMES OUT OF.
	 *
	 * Raised off the seabed rather than dropped on the surface, because the tell
	 * for a building placed in an ocean is that the ocean carries on underneath
	 * it. Widest at the waterline and narrowing both ways from there, so the tide
	 * has an edge to have made, and the rim is nibbled at random so no ring of it
	 * is a circle you could have drawn.
	 *
	 * @return the shoulder the tower stands on, three above the tide
	 */
	private static BlockPos crag(ServerLevel level, int x, int z, RandomSource random) {
		int tide = level.getSeaLevel();
		// Down to the seabed — or eight under the tide when the scan cannot reach
		// it, which happens over a trench. Either way it is rooted in something
		// rather than perched on the surface.
		int bed = Math.max(level.getMinY() + 2,
			Math.min(Ground.topOf(level, x, z), tide - 8));
		int deck = tide + 3;
		for (int y = bed; y <= deck; y++) {
			int reach = y <= tide ? 10 - (tide - y) / 4 : 9 - (y - tide);
			reach = Math.max(6, Math.min(10, reach));
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int out = dx * dx + dz * dz;
					if (out > reach * reach) {
						continue;
					}
					if (out > (reach - 1) * (reach - 1) && random.nextInt(3) == 0) {
						continue;
					}
					level.setBlock(new BlockPos(x + dx, y, z + dz), rock(random), 2);
				}
			}
		}
		// Standing water sitting on top of the new rock, from wherever the surface
		// cut through it. Taken out to the rim so the deck is a deck and not a pool.
		for (int dx = -11; dx <= 11; dx++) {
			for (int dz = -11; dz <= 11; dz++) {
				for (int y = tide + 1; y <= deck + 1; y++) {
					BlockPos at = new BlockPos(x + dx, y, z + dz);
					if (!level.getFluidState(at).isEmpty()) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				BlockPos on = new BlockPos(x + dx, deck + 1, z + dz);
				if (level.getBlockState(on.below()).isSolid()) {
					moss(level, on, random);
				}
			}
		}
		return new BlockPos(x, deck, z);
	}

	/** The rock under it, which is not the masonry on it. */
	private static BlockState rock(RandomSource random) {
		return switch (random.nextInt(9)) {
			case 0, 1 -> Blocks.ANDESITE.defaultBlockState();
			case 2, 3 -> Blocks.TUFF.defaultBlockState();
			case 4 -> Blocks.COBBLESTONE.defaultBlockState();
			case 5 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			default -> Blocks.STONE.defaultBlockState();
		};
	}

	public static boolean join(ServerLevel level, RandomSource random) {
		BlockPos deck = site(level);
		if (deck == null || joined(level)) {
			return false;
		}
		for (int up = 1; up <= GAP + 1; up++) {
			ring(level, deck.above(up), TIER_HALF[2], random, true);
		}
		TheWay.open(level, deck.above(1));
		level.getServer().overworld().setAttached(JOINED, true);
		HerobrineMod.LOGGER.info("the tower closed over itself at [{}, {}, {}]",
			deck.getX(), deck.getY(), deck.getZ());
		return true;
	}

	/** Three tiers, each narrower, each with a cornice under its step-in. */
	private static void tower(ServerLevel level, BlockPos base, RandomSource random) {
		int up = -3;
		for (int tier = 0; tier < TIER_HALF.length; tier++) {
			int half = TIER_HALF[tier];
			int top = up + TIER_RISE[tier];
			for (; up < top; up++) {
				ring(level, base.above(up), half, random, false);
				if (up > 3 && up % 6 == 0) {
					windows(level, base.above(up), half);
				}
			}
			// The overhang at the head of each tier: stairs turned outward all the
			// way round, which is the single detail that stops a stone box being one.
			cornice(level, base.above(up), half, random);
			up += 2;
			if (tier == 0) {
				buttresses(level, base, half, TIER_RISE[0], random);
			}
		}
		stair(level, base, random);
		pit(level, base);
		// A door at the foot, and steps up to it out of the grass.
		for (int h = 0; h < 2; h++) {
			level.setBlock(base.above(h).relative(Direction.SOUTH, TIER_HALF[0]),
				Blocks.AIR.defaultBlockState(), 2);
		}
		for (int out = 1; out <= 2; out++) {
			level.setBlock(base.below(out - 1).relative(Direction.SOUTH, TIER_HALF[0] + out),
				Blocks.STONE_BRICK_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		}
	}

	/** How wide the hole in the floor is, leaving a rim at the wall. */
	private static final int PIT_HALF = 2;
	/** And how deep the lava in it goes. */
	private static final int PIT_DEEP = 2;

	/**
	 * A HOLE IN THE FLOOR WITH LAVA IN IT, AND IT IS WHAT MAKES THE CLIMB A CLIMB.
	 *
	 * The interior of this tower has been a spiral of single stair blocks up a
	 * twenty-nine block hollow shaft since it was built — which is parkour by every
	 * definition except the one that matters, because falling off it cost nothing.
	 * You landed on the floor and walked back to the bottom step. A jump with no
	 * consequence is a staircase with gaps in it.
	 *
	 * THE RIM IS THE WHOLE DESIGN. The pit is five across in a seven across room,
	 * so there is exactly one block of floor left against the wall — enough to walk
	 * in through the door, stand, and look at what you have got to do, and not
	 * enough to walk round the edge and avoid it. The first step of the spiral is
	 * on that rim.
	 *
	 * Two deep, and two is deliberate. Deeper is a pit you die at the bottom of;
	 * two is a bath you can climb out of if you are quick and have anything left,
	 * which is a far better punishment than dying because the player gets to
	 * experience failing rather than being told about it.
	 */
	private static void pit(ServerLevel level, BlockPos base) {
		for (int dx = -PIT_HALF; dx <= PIT_HALF; dx++) {
			for (int dz = -PIT_HALF; dz <= PIT_HALF; dz++) {
				// Down through whatever the tower was standing on.
				for (int dy = -1; dy >= -(PIT_DEEP + 2); dy--) {
					level.setBlock(base.offset(dx, dy, dz),
						Blocks.AIR.defaultBlockState(), 2);
				}
				for (int dy = -(PIT_DEEP + 2); dy <= -3; dy++) {
					level.setBlock(base.offset(dx, dy, dz),
						Blocks.LAVA.defaultBlockState(), 2);
				}
			}
		}
		// A lip of deepslate round the hole so the lava is contained and the edge
		// reads as cut rather than as the floor having failed.
		for (int dx = -PIT_HALF - 1; dx <= PIT_HALF + 1; dx++) {
			for (int dz = -PIT_HALF - 1; dz <= PIT_HALF + 1; dz++) {
				if (Math.abs(dx) <= PIT_HALF && Math.abs(dz) <= PIT_HALF) {
					continue;
				}
				for (int dy = -1; dy >= -(PIT_DEEP + 2); dy--) {
					level.setBlock(base.offset(dx, dy, dz),
						Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
				}
			}
		}
		HerobrineMod.LOGGER.info("the tower stands over lava at [{}, {}]",
			base.getX(), base.getZ());
	}

	/** A course of wall, hollow, aged and grown over. */
	private static void ring(ServerLevel level, BlockPos middle, int half,
	                         RandomSource random, boolean solid) {
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				boolean wall = Math.abs(dx) == half || Math.abs(dz) == half;
				BlockPos at = middle.offset(dx, 0, dz);
				if (wall) {
					level.setBlock(at, aged(random), 2);
					vine(level, at, random);
				} else if (!solid) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/** Stairs turned outward all round, with a slab lip over them. */
	private static void cornice(ServerLevel level, BlockPos at, int half,
	                            RandomSource random) {
		for (int dx = -half - 1; dx <= half + 1; dx++) {
			for (int dz = -half - 1; dz <= half + 1; dz++) {
				if (Math.abs(dx) <= half && Math.abs(dz) <= half) {
					continue;
				}
				Direction out = Math.abs(dx) > Math.abs(dz)
					? (dx > 0 ? Direction.EAST : Direction.WEST)
					: (dz > 0 ? Direction.SOUTH : Direction.NORTH);
				level.setBlock(at.offset(dx, 0, dz),
					Blocks.STONE_BRICK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, out), 2);
				// And the ledge it makes is where things grow.
				BlockPos ledge = at.offset(dx, 1, dz);
				if (random.nextInt(3) == 0 && level.getBlockState(ledge).isAir()) {
					level.setBlock(ledge, random.nextBoolean()
						? Blocks.PALE_MOSS_CARPET.defaultBlockState()
						: Blocks.SHORT_GRASS.defaultBlockState(), 2);
				}
			}
		}
		ring(level, at, half, random, true);
	}

	/** Corner piers on the lower tier, so it looks like it is holding itself up. */
	private static void buttresses(ServerLevel level, BlockPos base, int half,
	                               int rise, RandomSource random) {
		for (int cx : new int[] { -half, half }) {
			for (int cz : new int[] { -half, half }) {
				int lean = rise - 2;
				for (int up = -3; up < lean; up++) {
					int out = up < lean / 2 ? 1 : 0;
					level.setBlock(base.offset(cx + Integer.signum(cx) * out, up,
						cz + Integer.signum(cz) * out), aged(random), 2);
				}
				level.setBlock(base.offset(cx + Integer.signum(cx), lean, cz
					+ Integer.signum(cz)), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
			}
		}
	}

	/** Arched slits, one per face, with a stair over each. */
	private static void windows(ServerLevel level, BlockPos at, int half) {
		for (Direction face : Direction.Plane.HORIZONTAL) {
			BlockPos slit = at.relative(face, half);
			level.setBlock(slit, Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(slit.above(), Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(slit.above(2), Blocks.STONE_BRICK_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, face)
				.setValue(BlockStateProperties.HALF, Half.TOP), 2);
		}
	}

	/** Round the inside wall, all the way up. */
	private static void stair(ServerLevel level, BlockPos base, RandomSource random) {
		Direction[] turn = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
		for (int up = 0; up < RISE; up++) {
			int half = up < TIER_RISE[0] ? TIER_HALF[0]
				: up < TIER_RISE[0] + TIER_RISE[1] ? TIER_HALF[1] : TIER_HALF[2];
			Direction face = turn[up % 4];
			BlockPos step = base.above(up).relative(face, half - 1)
				.relative(face.getClockWise(), (up % 2 == 0) ? 1 : -1);
			level.setBlock(step, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, face.getCounterClockWise()), 2);
			for (int clear = 1; clear <= 2; clear++) {
				level.setBlock(step.above(clear), Blocks.AIR.defaultBlockState(), 2);
			}
			if (up % 7 == 3) {
				BlockPos lamp = step.above(2);
				level.setBlock(lamp, Blocks.LANTERN.defaultBlockState()
					.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
			}
			// AND FOUR OF THEM HANG OUT OVER THE HOLE.
			//
			// One block jutting inward off the spiral with a chest on it, at four
			// heights up the shaft. Reachable from the step you are standing on —
			// this is not a jump, and it should not be, because a jump you must
			// make to get the loot is a jump you will make forty times until you
			// get it and the lava stops being frightening and starts being tedious.
			//
			// What it is instead is a REASON TO STAND STILL over two blocks of lava
			// and take your hands off the controls. That is worse.
			if (up % 8 == 4) {
				Direction inward = face.getOpposite();
				BlockPos shelf = step.relative(inward);
				level.setBlock(shelf.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
				level.setBlock(shelf, Blocks.CHEST.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, face), 2);
				if (level.getBlockEntity(shelf)
						instanceof net.minecraft.world.level.block.entity.ChestBlockEntity box) {
					Loot.scatter(box, random, Loot.Tier.TOWER);
				}
			}
		}
	}

	/** The deck: battlements, and the lower half of a frame with no top to it. */
	/**
	 * THE HEAD OF IT, AND IT IS WIDER THAN THE SHAFT ON PURPOSE.
	 *
	 * The deck used to be the same five blocks as the tier under it, which made the
	 * whole tower one unbroken taper — and left the gate at the top with nowhere to
	 * stand. Nine across and eight up will not fit on a five-block roof.
	 *
	 * Eleven now, overhanging three on every side, carried on three courses of
	 * corbels that each step out one further than the one below. The corbel is the
	 * entire trick: an overhang with nothing under it reads as a mistake, and the
	 * same overhang with three courses of stone reaching out to meet it reads as
	 * the most deliberate thing on the skyline.
	 */
	private static final int DECK_HALF = 5;

	private static void lower(ServerLevel level, BlockPos deck, RandomSource random) {
		int shaft = TIER_HALF[2];
		// Three courses reaching out, bottom to top, each one wider.
		for (int step = 1; step <= 3; step++) {
			int reach = shaft + step;
			int y = step - 4;                  // -3, -2, -1
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if (Math.abs(dx) < reach && Math.abs(dz) < reach) {
						continue;
					}
					// Turned over, so the step is on the underside and the shadow
					// falls the way an eave's does.
					level.setBlock(deck.offset(dx, y, dz),
						Blocks.STONE_BRICK_STAIRS.defaultBlockState()
							.setValue(BlockStateProperties.HORIZONTAL_FACING,
								Math.abs(dx) >= Math.abs(dz)
									? (dx > 0 ? Direction.EAST : Direction.WEST)
									: (dz > 0 ? Direction.SOUTH : Direction.NORTH))
							.setValue(BlockStateProperties.HALF, Half.TOP), 2);
				}
			}
		}
		// The floor of it.
		for (int dx = -DECK_HALF; dx <= DECK_HALF; dx++) {
			for (int dz = -DECK_HALF; dz <= DECK_HALF; dz++) {
				level.setBlock(deck.offset(dx, 0, dz), switch (random.nextInt(7)) {
					case 0, 1 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
					case 2 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
					case 3 -> Blocks.POLISHED_ANDESITE.defaultBlockState();
					default -> Blocks.STONE_BRICKS.defaultBlockState();
				}, 2);
				boolean rim = Math.abs(dx) == DECK_HALF || Math.abs(dz) == DECK_HALF;
				if (rim && (dx + dz) % 2 == 0) {
					level.setBlock(deck.offset(dx, 1, dz), aged(random), 2);
				} else if (rim) {
					level.setBlock(deck.offset(dx, 1, dz),
						Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
				} else if (random.nextInt(7) == 0
					&& level.getBlockState(deck.offset(dx, 1, dz)).isAir()) {
					level.setBlock(deck.offset(dx, 1, dz),
						Blocks.PALE_MOSS_CARPET.defaultBlockState(), 2);
				}
			}
		}
		// And a lantern at each corner of it, which is what makes a platform read
		// as somewhere somebody stands rather than the top of a chimney.
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				level.setBlock(deck.offset(sx * (DECK_HALF - 1), 1, sz * (DECK_HALF - 1)),
					Blocks.SOUL_LANTERN.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * WHAT IS IN THE GAP, WHICH WAS NOTHING AND IS THE WHOLE EFFECT.
	 *
	 * Seven blocks of clean air between two halves reads as an unfinished build.
	 * The same seven blocks with rubble hanging motionless in them reads as a thing
	 * that came apart and stopped — and stopping is the impossible part, which is
	 * where all the unease is.
	 *
	 * So: single blocks and pairs suspended at random through the volume, thinning
	 * toward the middle, and chains that hang out of the island and reach nothing.
	 * Nothing here is climbable on purpose.
	 */
	private static void between(ServerLevel level, BlockPos deck, RandomSource random) {
		java.util.List<BlockPos> stones = new java.util.ArrayList<>();
		for (int i = 0; i < 26; i++) {
			int dy = 1 + random.nextInt(GAP);
			int spread = 2 + random.nextInt(4);
			int dx = random.nextInt(spread * 2 + 1) - spread;
			int dz = random.nextInt(spread * 2 + 1) - spread;
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
				continue;      // keep the middle clear, so the frame stays readable
			}
			BlockPos at = deck.offset(dx, dy, dz);
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			level.setBlock(at, aged(random), 2);
			stones.add(at);
			if (random.nextBoolean()) {
				level.setBlock(at.below(), Blocks.PALE_HANGING_MOSS.defaultBlockState(), 2);
			}
		}
		hoard(level, stones, random);
	}

	/**
	 * TWO OR THREE CHESTS OUT IN THE GAP, ON THE STONES THEMSELVES.
	 *
	 * The rubble between the deck and the island has been a view since it was
	 * written — twenty-six blocks hanging in the air that a player looks at, works
	 * out is jumpable, and then has no reason whatever to jump. A climb with
	 * nothing at the top of it is scenery.
	 *
	 * ON THE HIGH ONES, and that is the only rule. Sorting by height and taking
	 * from the top means every chest is further into the jump than the last, so the
	 * player is paid at three points on the way up rather than once at the end —
	 * which is what keeps somebody going after the first miss.
	 *
	 * The stones are spread out on purpose, so no two chests are reachable from the
	 * same footing. A chest you can open without committing to the next jump is a
	 * chest that makes the jump optional.
	 */
	private static void hoard(ServerLevel level, java.util.List<BlockPos> stones,
	                          RandomSource random) {
		stones.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
		int want = 2 + random.nextInt(2);
		int put = 0;
		for (BlockPos stone : stones) {
			if (put >= want) {
				break;
			}
			BlockPos on = stone.above();
			if (!level.getBlockState(on).isAir()) {
				continue;
			}
			level.setBlock(on, Blocks.CHEST.defaultBlockState(), 2);
			if (level.getBlockEntity(on)
					instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				Loot.scatter(chest, random, Loot.Tier.TOWER);
			}
			put++;
		}
		HerobrineMod.LOGGER.info("{} chests hang in the gap under the island", put);
	}

	/**
	 * AND THE THING AT THE TOP OF ALL OF IT.
	 *
	 * Twenty-nine blocks of interior stair, then a deck, then a gap with three
	 * chests strung across it, and then this: one chest on the floor of the island
	 * with a broken pair of wings in it and a lantern over it.
	 *
	 * THE ROOM IS EMPTY APART FROM IT, deliberately. Everything else in the tower
	 * is scattered — rubble, moss, hanging vine, chests wedged on stones. The
	 * summit is bare and swept and has one object in the middle of it, because a
	 * reward found among clutter is loot and a reward alone in a room is an ENDING.
	 * The player knows they have reached the top before they have opened anything.
	 *
	 * See Loot.brokenWings for why they are broken and why that is the good version.
	 */
	private static void summit(ServerLevel level, BlockPos middle, RandomSource random) {
		BlockPos on = middle.above();
		if (!level.getBlockState(on).isAir()) {
			return;
		}
		level.setBlock(on, Blocks.CHEST.defaultBlockState(), 2);
		level.setBlock(on.above(2), Blocks.SOUL_LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
		if (!(level.getBlockEntity(on)
				instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
			return;
		}
		// The wings first, into the middle slot, so they are the thing under the
		// cursor when the lid comes up rather than something noticed afterwards.
		chest.setItem(13, Loot.brokenWings(level.registryAccess(), random));
		Loot.scatter(chest, random, Loot.Tier.TOWER);
		HerobrineMod.LOGGER.info("the wings are at the top of the tower, [{}, {}, {}]",
			on.getX(), on.getY(), on.getZ());
	}

	/** The rest of it, torn off and hanging, with its own debris around it. */
	private static void island(ServerLevel level, BlockPos middle, RandomSource random) {
		for (int up = 0; up < 7; up++) {
			int half = up == 0 ? 2 : up < 5 ? 3 : 2;
			for (int dx = -half; dx <= half; dx++) {
				for (int dz = -half; dz <= half; dz++) {
					boolean wall = Math.abs(dx) == half || Math.abs(dz) == half;
					if (up < 2 && random.nextInt(3) == 0
						&& Math.abs(dx) + Math.abs(dz) >= half + 1) {
						continue;      // the bite out of the underside
					}
					BlockPos at = middle.offset(dx, up, dz);
					if (wall || up == 0 || up == 6) {
						level.setBlock(at, aged(random), 2);
						vine(level, at, random);
					}
				}
			}
			if (up == 4) {
				cornice(level, middle.above(up), 3, random);
			}
		}
		summit(level, middle, random);
		// The upper half of the frame, hanging upside down over the gap.
		for (int dx = -1; dx <= 1; dx++) {
			level.setBlock(middle.offset(dx, -1, 0), Blocks.CALCITE.defaultBlockState(), 2);
		}
		for (int side : new int[] { -2, 2 }) {
			level.setBlock(middle.offset(side, -1, 0),
				Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
			level.setBlock(middle.offset(side, -2, 0),
				Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
		}
		// Chains out of the underside, reaching nothing.
		for (int side : new int[] { -3, 3 }) {
			for (int dz : new int[] { -2, 2 }) {
				for (int down = 1; down <= 2 + random.nextInt(5); down++) {
					BlockPos link = middle.offset(side, -down, dz);
					if (level.getBlockState(link).isAir()) {
						level.setBlock(link, Blocks.IRON_CHAIN.defaultBlockState(), 2);
					}
				}
			}
		}
		// AND SATELLITES. Four or five separate lumps holding station around it,
		// which is what turns one floating object into a thing that BROKE.
		for (int i = 0; i < 5; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double out = 5.0 + random.nextDouble() * 5.0;
			BlockPos lump = middle.offset(
				(int) Math.round(Math.cos(angle) * out),
				random.nextInt(7) - 2,
				(int) Math.round(Math.sin(angle) * out));
			int r = random.nextInt(2);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy = 0; dy <= r; dy++) {
						BlockPos at = lump.offset(dx, dy, dz);
						if (level.getBlockState(at).isAir()) {
							level.setBlock(at, aged(random), 2);
							vine(level, at, random);
						}
					}
				}
			}
		}
	}

	private static void vine(ServerLevel level, BlockPos at, RandomSource random) {
		if (random.nextInt(4) != 0) {
			return;
		}
		for (Direction face : Direction.Plane.HORIZONTAL) {
			BlockPos side = at.relative(face);
			if (level.getBlockState(side).isAir() && random.nextBoolean()) {
				level.setBlock(side, Blocks.VINE.defaultBlockState()
					.setValue(net.minecraft.world.level.block.VineBlock.PROPERTY_BY_DIRECTION
						.get(face.getOpposite()), true), 2);
			}
		}
	}

	private static void moss(ServerLevel level, BlockPos at, RandomSource random) {
		if (level.getBlockState(at).isAir() && random.nextInt(5) == 0) {
			level.setBlock(at, Blocks.PALE_MOSS_CARPET.defaultBlockState(), 2);
		}
	}

	/** Failing for a long time, and neither of you built it. */
	private static BlockState aged(RandomSource random) {
		return switch (random.nextInt(10)) {
			case 0, 1, 2, 3 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			case 4, 5 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			case 6 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			default -> Blocks.STONE_BRICKS.defaultBlockState();
		};
	}
}
