package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.ModBlocks;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * THE FRAME, AND WHAT IS ON THE OTHER SIDE OF IT.
 *
 * Two jobs that have to agree with each other, so they live together: raising
 * the portal where he died, and making sure the ground somebody lands on when
 * they walk into it exists.
 *
 * THE SECOND ONE IS NOT A DETAIL. His world generates on the nether's noise
 * settings, which means lava at the height anybody would arrive at, and a
 * player who has just spent forty hours reaching the ending should not lose it
 * to a coin flip about what the terrain did. So the landing is CUT rather than
 * found: a floor, a roof, a rim and a way back, built into whatever was there.
 *
 * The way back matters as much as the way in. A one-way door is a trap, and a
 * trap at the end of a horror mod is not an ending, it is a bug report from
 * somebody who cannot reach their own base again.
 */
public final class TheWay {
	private TheWay() {}

	/** Five wide, five high. Big enough to be a door and not an arch. */
	/** Half the opening, so the piers stand at two and three. */
	private static final int HALF = 2;
	/** How tall the way itself is. */
	private static final int TALL = 4;
	/** How deep the piers are, front to back. Thickness is what sells masonry. */
	private static final int DEPTH = 1;
	/** And how far the threshold and the cornice reach past all of it. */
	private static final int REACH = 4;

	/**
	 * A GATE, NOT A FRAME.
	 *
	 * Five blocks of deepslate in a rectangle with a light in the middle is what a
	 * portal looks like when nobody designed it. It is the shape the Nether portal
	 * already owns, at half the size, in different stone — and next to anything
	 * else in this mod it read as a placeholder that got shipped.
	 *
	 * This is a piece of architecture. Nine across, eight up, three deep, standing
	 * on its own threshold: two thick piers with a chiselled base and a chiselled
	 * cap, a lintel across them, a corbelled cornice over that, and lanterns
	 * hanging in the shadow underneath. It is the only thing on his land that was
	 * BUILT rather than dug or thrown up, and it should be the only thing that
	 * looks it.
	 *
	 * SEVEN STONES, not one. Deepslate brick, cracked deepslate brick, deepslate
	 * tile, polished and chiselled deepslate, tuff brick, polished tuff. Every one
	 * of them is a cold grey-blue and none of them is the same grey-blue, which is
	 * the entire difference between masonry and a texture repeated four hundred
	 * times.
	 *
	 * AMETHYST IN THE PIERS. One block set into the face of each, at eye height,
	 * lit from the portal beside it. It is the only warm-coloured thing in the
	 * structure and it is the same violet as the way itself — so the stone reads
	 * as something built AROUND the light rather than a wall with a hole in it.
	 *
	 * REINFORCED DEEPSLATE STILL AT THE FOUR CORNERS, doing the job crying obsidian
	 * used to. It is the only block in Minecraft a player cannot obtain by any
	 * means, and vanilla places it in exactly one place — the ancient cities,
	 * around something nobody has explained. A player who knows that reads the
	 * corners as evidence. A player who does not simply cannot mine them, which
	 * says the same thing more slowly.
	 *
	 * And no blackstone anywhere near it. Blackstone is the second most
	 * nether-flavoured stone in the game and it was holding up the frame, the step
	 * and the whole landing chamber.
	 */
	/**
	 * A DOORWAY THAT WORKS, which is now the exception rather than the rule.
	 *
	 * There are three of these in a playthrough and only one of them should let
	 * anybody through:
	 *
	 *   under the homestead   DEAD. The first thing anybody dug, forced open from
	 *                         the far side and then gone out. See remains().
	 *   at the threshold      the live one. It is what the whole chain is for.
	 *   the tower, at the end Spire.join, after he is driven off.
	 *
	 * It used to be the homestead's that worked, and a playthrough log settled the
	 * argument: the homestead went up at 11:08:01, the way opened at 11:08:02, and
	 * somebody was standing in his world at 11:08:18. SEVENTEEN SECONDS into a
	 * fresh save, at RUMOUR, before a single thing had happened — and everything in
	 * that dimension is the ending.
	 */
	public static void open(ServerLevel level, BlockPos site) {
		build(level, site, true);
	}

	/**
	 * The same doorway with nothing in it.
	 *
	 * Not a different structure — the same masonry, the same batter, the same
	 * rubble on the floor, and an empty opening. Which is the honest picture: this
	 * is the door Steve's ledger says they found under the hill, and the books say
	 * what happened to it. Somebody sealed it, something came out anyway, and what
	 * is left is a frame with a draught through it.
	 *
	 * It is also the thing that teaches the player what they are looking for. By
	 * the time they reach the live one at the bottom of the threshold they have
	 * already stood in front of one of these, twenty hours earlier, and know
	 * exactly what it is.
	 */
	public static void remains(ServerLevel level, BlockPos site) {
		build(level, site, false);
	}

	private static void build(ServerLevel level, BlockPos site, boolean works) {
		net.minecraft.util.RandomSource random = level.getRandom();

		// THE THRESHOLD. A gate standing on the floor is a doorway; a gate standing
		// on its own step is a building, and the step is what tells you which way
		// you are meant to approach from.
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -DEPTH - 1; dz <= DEPTH + 1; dz++) {
				BlockPos at = site.offset(dx, -1, dz);
				boolean rim = Math.abs(dx) == REACH || Math.abs(dz) == DEPTH + 1;
				if (rim) {
					level.setBlock(at, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, outward(dx, dz)), 2);
				} else {
					level.setBlock(at, paving(random), 2);
					// Not in the doorway. Nothing grows where he walks.
					if (Math.abs(dx) > 1 && random.nextInt(4) == 0) {
						level.setBlock(at.above(),
							Blocks.PALE_MOSS_CARPET.defaultBlockState(), 2);
					}
				}
			}
		}

		// THE PIERS. Two blocks thick and three deep, which is what stops them
		// being a line of blocks seen edge-on from every angle but one.
		for (int side = -1; side <= 1; side += 2) {
			for (int out = HALF; out <= HALF + 1; out++) {
				for (int dz = -DEPTH; dz <= DEPTH; dz++) {
					for (int up = 0; up <= TALL; up++) {
						level.setBlock(site.offset(side * out, up, dz),
							pier(random, up), 2);
					}
				}
			}
			// The jewel, set into the INNER face at eye height — the one the way
			// itself lights. On the outside edge it was a purple block on a grey
			// wall; here it is the same violet as the light beside it, and the
			// stone reads as something built around the light rather than a wall
			// with a hole in it.
			level.setBlock(site.offset(side * HALF, 2, 0),
				Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
			// And the corners of the opening, which cannot be taken out.
			level.setBlock(site.offset(side * HALF, 0, 0),
				Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
			level.setBlock(site.offset(side * HALF, TALL, 0),
				Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
		}

		// THE LINTEL, spanning both piers, and a chiselled band over it.
		for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
			for (int dz = -DEPTH; dz <= DEPTH; dz++) {
				level.setBlock(site.offset(dx, TALL + 1, dz), pier(random, TALL + 1), 2);
				level.setBlock(site.offset(dx, TALL + 2, dz),
					Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
			}
		}

		// THE CORNICE. Stairs turned outward all the way round, one block proud of
		// everything under them — the single detail that throws a shadow, and the
		// reason this reads as a gate from two hundred blocks instead of a slab.
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -DEPTH - 1; dz <= DEPTH + 1; dz++) {
				boolean edge = Math.abs(dx) >= HALF + 1 || Math.abs(dz) == DEPTH + 1;
				if (!edge) {
					continue;
				}
				level.setBlock(site.offset(dx, TALL + 3, dz),
					Blocks.TUFF_BRICK_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, outward(dx, dz)), 2);
				// And what grows on a ledge nobody has been up to in a long time.
				BlockPos under = site.offset(dx, TALL + 2, dz);
				if (level.getBlockState(under).isAir() && random.nextInt(3) == 0) {
					level.setBlock(under,
						Blocks.PALE_HANGING_MOSS.defaultBlockState(), 2);
				}
			}
		}
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -DEPTH; dz <= DEPTH; dz++) {
				level.setBlock(site.offset(dx, TALL + 3, dz),
					Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), 2);
			}
		}

		// A LIGHT IN THE SHADOW UNDER THE LINTEL. Soul lanterns, because the flame
		// is the one cold blue in the game and it is the only lamp that does not
		// make a stone doorway look welcoming.
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				// Under the cornice, at the four corners. Inside the opening they
				// were buried in the piers; out here they light the whole face of
				// it and the overhang gives them something to hang from.
				BlockPos hook = site.offset(sx * (HALF + 1), TALL + 2, sz * (DEPTH + 1));
				if (level.getBlockState(hook).isAir()) {
					level.setBlock(hook, Blocks.SOUL_LANTERN.defaultBlockState()
						.setValue(BlockStateProperties.HANGING, true), 2);
				}
			}
		}

		// AND THE WAY ITSELF, last, so nothing overwrites it — IF IT WORKS.
		//
		// A dead doorway is the same masonry with nothing in the opening, so the
		// two are one builder and one flag rather than two files that will drift.
		if (works) {
			for (int dx = -HALF + 1; dx <= HALF - 1; dx++) {
				for (int dy = 0; dy <= TALL; dy++) {
					// ACROSS X, because this loop is the one that varies dx. Said
					// out loud rather than left to the block's default — the
					// default being right here is what hid the bug in seal().
					level.setBlock(site.offset(dx, dy, 0),
						ModBlocks.THE_WAY.defaultBlockState().setValue(
							com.bloomlet.herobrine.block.TheWayBlock.AXIS,
							net.minecraft.core.Direction.Axis.X), 2);
				}
			}
		}

		// What has grown up the front of it since anybody last used it.
		for (int side = -1; side <= 1; side += 2) {
			for (int up = 1; up < TALL; up++) {
				BlockPos face = site.offset(side * (HALF + 2), up, 0);
				if (level.getBlockState(face).isAir() && random.nextInt(3) != 0) {
					level.setBlock(face, Blocks.VINE.defaultBlockState().setValue(
						net.minecraft.world.level.block.VineBlock.PROPERTY_BY_DIRECTION
							.get(side > 0 ? Direction.WEST : Direction.EAST), true), 2);
				}
			}
		}

		battered(level, site, random);

		if (works) {
			level.playSound(null, site, com.bloomlet.herobrine.sound.ModSounds.THE_WAY,
				net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.5F);
		}
		HerobrineMod.LOGGER.info("the way {} at [{}, {}, {}]",
			works ? "is open" : "is DEAD — frame only",
			site.getX(), site.getY(), site.getZ());
	}

	/** Whichever way is out, for a stair on the rim of something square. */
	private static Direction outward(int dx, int dz) {
		return Math.abs(dx) >= Math.abs(dz)
			? (dx > 0 ? Direction.EAST : Direction.WEST)
			: (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	/**
	 * The pier stone. Chiselled at the foot and the cap, brick between, and one
	 * course in five is cracked or tuff — so no two piers are laid the same and
	 * neither of them is a column of one texture.
	 */
	private static BlockState pier(net.minecraft.util.RandomSource random, int up) {
		if (up == 0 || up == TALL) {
			return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
		}
		return switch (random.nextInt(9)) {
			case 0, 1 -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
			case 2 -> Blocks.TUFF_BRICKS.defaultBlockState();
			case 3 -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			case 4 -> Blocks.DEEPSLATE_TILES.defaultBlockState();
			default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		};
	}

	/** And the floor of it, which is older than the gate standing on it. */
	private static BlockState paving(net.minecraft.util.RandomSource random) {
		return switch (random.nextInt(8)) {
			case 0, 1 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			case 2 -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
			case 3 -> Blocks.POLISHED_TUFF.defaultBlockState();
			case 4 -> Blocks.CALCITE.defaultBlockState();
			default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
		};
	}

	/**
	 * WHERE THEY COME OUT, CUT IF IT IS NOT ALREADY THERE.
	 *
	 * Called on the way in, on the destination level, before the teleport
	 * happens — so by the time the screen has finished going purple there is a
	 * floor under them and a frame behind them.
	 *
	 * Searched for first and built only if nothing is found, so going through a
	 * second time returns to the same landing rather than scattering chambers
	 * across the map. The search is the cheapest possible one: the same
	 * coordinates they left from, which is also the most legible — the door is
	 * in the same place on both sides of the wall.
	 */
	public static BlockPos landing(ServerLevel bound, ServerPlayer player) {
		BlockPos at = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
		int y = Math.max(bound.getMinY() + 8, Math.min(96, player.getBlockY()));
		BlockPos site = at.atY(y);
		for (long each : bound.getAttachedOrElse(LANDINGS, List.of())) {
			BlockPos cut = BlockPos.of(each);
			if (Math.abs(cut.getX() - site.getX()) <= SAME_DOOR
				&& Math.abs(cut.getZ() - site.getZ()) <= SAME_DOOR) {
				return cut.offset(0, 0, 3);      // been through this door before; same room
			}
		}
		if (bound.getBlockState(site).is(ModBlocks.THE_WAY)) {
			return site;         // already been through; same door
		}
		for (BlockPos near : BlockPos.betweenClosed(
				site.offset(-8, -6, -8), site.offset(8, 6, 8))) {
			if (bound.getBlockState(near).is(ModBlocks.THE_WAY)) {
				return near.immutable().offset(0, 0, 2);
			}
		}
		// A room from before the list existed: the floor, looked for in a column
		// and not at one exact height — see LANDINGS for why that mattered.
		for (int dy = 0; dy <= FLOOR_SEARCH; dy++) {
			for (int sign : new int[] {1, -1}) {
				BlockPos probe = site.offset(0, dy * sign, 0);
				if (bound.getBlockState(probe.below()).is(Blocks.POLISHED_DEEPSLATE)
					&& bound.getBlockState(probe.offset(0, -2, 0)).is(Blocks.DEEPSLATE_TILES)) {
					remember(bound, probe);
					return probe.offset(0, 0, 3);
				}
				if (dy == 0) {
					break;
				}
			}
		}
		chamber(bound, site);
		remember(bound, site);
		return site.offset(0, 0, 3);
	}

	/**
	 * WHERE EACH DOOR LETS OUT, REMEMBERED. The room is cut once, at the spot the
	 * first person through was standing over, and every walk through after that
	 * has to land in the same room. It used to be found by looking for the floor
	 * at ONE exact height — the player's block Y the moment they stepped in — and
	 * a player who stepped in from the frame's step, or mid-jump, was one block
	 * higher, the floor was not where it was looked for, and a second room was cut
	 * on top of the first: sealed exit, iron bars, the lot. Reported as "the way
	 * out was closed again". Nothing had reset; the room had been rebuilt.
	 *
	 * So the site is written down, per door, in the level it opens into, and read
	 * back by the door's position: within SAME_DOOR blocks flat is the same door.
	 */
	private static final AttachmentType<List<Long>> LANDINGS = AttachmentRegistry.createPersistent(
		HerobrineMod.id("landings"), Codec.LONG.listOf());
	private static final int SAME_DOOR = 24;
	private static final int FLOOR_SEARCH = 4;

	private static void remember(ServerLevel bound, BlockPos site) {
		List<Long> cut = new ArrayList<>(bound.getAttachedOrElse(LANDINGS, List.of()));
		if (!cut.contains(site.asLong())) {
			cut.add(site.asLong());
			bound.setAttached(LANDINGS, List.copyOf(cut));
		}
	}

	/**
	 * A room, hollowed out of whatever was there.
	 *
	 * Deliberately bare. This is a doorstep rather than a build — the world
	 * beyond it is the thing worth making, and a decorated arrival hall would
	 * spend the first impression on architecture instead of on the place.
	 *
	 * Everything inside it is replaced rather than tested, including lava. That
	 * is the whole reason it exists.
	 */
	private static void chamber(ServerLevel level, BlockPos site) {
		int r = 7;
		for (BlockPos pos : BlockPos.betweenClosed(
				site.offset(-r, -2, -r), site.offset(r, TALL + 4, r))) {
			double away = Math.max(Math.abs(pos.getX() - site.getX()),
				Math.abs(pos.getZ() - site.getZ()));
			boolean shell = away >= r || pos.getY() <= site.getY() - 2
				|| pos.getY() >= site.getY() + TALL + 4;
			level.setBlock(pos, shell
				? Blocks.DEEPSLATE_TILES.defaultBlockState()
				: Blocks.AIR.defaultBlockState(), 2);
		}
		for (BlockPos pos : BlockPos.betweenClosed(
				site.offset(-r + 1, -1, -r + 1), site.offset(r - 1, -1, r - 1))) {
			level.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
		}
		// The way back, facing the room, so it is the first thing they see when
		// they turn round. Nobody is stranded here.
		// THE DOOR BEHIND YOU IS DEAD. The frame stands — it is what you came
		// through — but it does not work from this side. There is no going back
		// the way you came; the way home is the one his death opens.
		remains(level, site);
		stairOut(level, site);
		for (int dx : new int[] { -3, 3 }) {
			level.setBlock(site.offset(dx, 1, 3), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
		}
		HerobrineMod.LOGGER.info("cut a landing in his world at [{}, {}, {}] — dead door, barred stair out",
			site.getX(), site.getY(), site.getZ());
	}

	/** Which way the frame faces, for anything that needs to stand clear of it. */
	/**
	 * THE WAY OUT OF THE VAULT, AND IT IS OLD.
	 *
	 * A one-wide stair leaves the far wall and climbs south until it breaks the
	 * surface, and everything about it says it was cut long before you: the steps
	 * are deepslate brick gone to moss, roots hang from the ceiling, cobwebs sit in
	 * the corners, and where the wall has failed it is cracked brick and gravel.
	 * Iron bars close it at the bottom — you see it through them from the vault, a
	 * soul lantern a few steps up — and the top is grown over, moss and rooted
	 * earth you dig through into the open. Somebody barred this from the inside.
	 */
	private static void stairOut(ServerLevel level, BlockPos site) {
		net.minecraft.util.RandomSource random = level.getRandom();
		int x = site.getX();
		int wall = site.getZ() + 7;
		int surface = Ground.topOf(level, x, wall + 30);
		int steps = Math.max(6, Math.min(40, surface - site.getY() + 1));

		// The gate in the wall, barred.
		for (int up = 0; up <= 1; up++) {
			level.setBlock(new BlockPos(x, site.getY() + up, wall), Blocks.IRON_BARS.defaultBlockState(), 2);
		}
		level.setBlock(new BlockPos(x, site.getY() + 2, wall), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);

		for (int i = 0; i < steps; i++) {
			int y = site.getY() + i;
			int z = wall + 1 + i;
			BlockPos step = new BlockPos(x, y, z);
			BlockState tread = random.nextInt(4) == 0
				? Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState()
				: Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
			level.setBlock(step, tread.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
			level.setBlock(step.below(), random.nextInt(3) == 0
				? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
				: Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
			// Headroom, and the walls either side made good where they were air.
			for (int up = 1; up <= 2; up++) {
				level.setBlock(step.above(up), Blocks.AIR.defaultBlockState(), 2);
				for (int side = -1; side <= 1; side += 2) {
					BlockPos flank = step.offset(side, up, 0);
					if (!level.getBlockState(flank).isSolid()) {
						level.setBlock(flank, random.nextInt(3) == 0
							? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					}
				}
			}
			BlockPos lid = step.above(3);
			if (!level.getBlockState(lid).isSolid()) {
				level.setBlock(lid, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
			}
			// What grew in here.
			if (i == 2) {
				level.setBlock(step.above(2), Blocks.SOUL_LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			} else if (random.nextInt(4) == 0) {
				level.setBlock(step.above(2), Blocks.HANGING_ROOTS.defaultBlockState(), 2);
			} else if (random.nextInt(5) == 0) {
				level.setBlock(step.above(2), Blocks.COBWEB.defaultBlockState(), 2);
			}
			if (random.nextInt(3) == 0) {
				int side = random.nextBoolean() ? 1 : -1;
				level.setBlock(step.offset(side, 1, 0), Blocks.GLOW_LICHEN.defaultBlockState()
					.setValue(side == 1 ? BlockStateProperties.WEST : BlockStateProperties.EAST, true), 2);
			}
		}
		// The top: grown shut. Moss and rooted earth for the last three steps'
		// headroom, azalea over the hole, and then the sky.
		for (int i = steps - 3; i < steps; i++) {
			BlockPos step = new BlockPos(x, site.getY() + i, wall + 1 + i);
			for (int up = 1; up <= 2; up++) {
				level.setBlock(step.above(up), random.nextBoolean()
					? Blocks.MOSS_BLOCK.defaultBlockState()
					: Blocks.ROOTED_DIRT.defaultBlockState(), 2);
			}
		}
		BlockPos mouth = new BlockPos(x, site.getY() + steps, wall + steps);
		level.setBlock(mouth, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
		if (level.getBlockState(mouth.above()).isAir()) {
			level.setBlock(mouth.above(), Blocks.AZALEA.defaultBlockState(), 2);
		}
		HerobrineMod.LOGGER.info("the stair out of the vault climbs {} steps south from [{}, {}, {}], barred and grown over",
			steps, x, site.getY(), wall);
	}

	public static Direction across() {
		return Direction.EAST;
	}

	/**
	 * IT HAS BEEN FORCED, AND THE FRAME SHOWS IT.
	 *
	 * This stood finished: a clean lintel, a cornice, four soul lanterns and vines
	 * up the face. Which is the wrong claim. Every book in the mod says the same
	 * thing about this doorway — nineteen people put him through one and eleven
	 * came back, they sealed it, and it did not hold. A doorway in that story is
	 * not a doorway anybody maintained. It is one somebody shut and something
	 * opened again from the other side.
	 *
	 * NOTHING HERE TOUCHES THE_WAY, and that is the whole constraint. The portal
	 * surface is placed last precisely so nothing overwrites it, so this only ever
	 * looks at blocks that are already SOLID and are not the way itself. Knocking
	 * a hole in the portal would not read as damage, it would read as a portal
	 * with a hole in it, and it would break the only route into his world.
	 *
	 * Three kinds of damage and they are different on purpose. The lintel loses
	 * blocks from the TOP, which is what falls first. The piers lose them from the
	 * OUTSIDE, because that is the face a crowd would have got at. And the rubble
	 * goes on the floor in front, because the blocks that came out of a doorway
	 * are still lying there — a broken frame with a clean floor under it is a
	 * texture, not an event.
	 */
	private static void battered(ServerLevel level, BlockPos site,
	                             net.minecraft.util.RandomSource random) {
		// The lintel and the cornice above it, coming down.
		for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
			for (int dz = -DEPTH - 1; dz <= DEPTH + 1; dz++) {
				for (int up = TALL + 1; up <= TALL + 3; up++) {
					if (random.nextInt(4) != 0) {
						continue;
					}
					BlockPos at = site.offset(dx, up, dz);
					if (!level.getBlockState(at).isSolid()
						|| level.getBlockState(at).is(ModBlocks.THE_WAY)) {
						continue;
					}
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		// The piers, from the outside in. Cracked where it has not gone yet.
		for (int side = -1; side <= 1; side += 2) {
			for (int up = 0; up <= TALL; up++) {
				for (int out = HALF; out <= HALF + 1; out++) {
					BlockPos at = site.offset(side * out, up, 0);
					BlockState was = level.getBlockState(at);
					if (!was.isSolid() || was.is(ModBlocks.THE_WAY)) {
						continue;
					}
					int roll = random.nextInt(6);
					if (roll == 0) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
					} else if (roll < 3) {
						level.setBlock(at,
							Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), 2);
					}
				}
			}
		}
		// And what came out of it, on the floor where it fell.
		int put = 0;
		for (int dx = -HALF - 2; dx <= HALF + 2; dx++) {
			for (int dz = -DEPTH - 3; dz <= DEPTH + 3; dz++) {
				if (Math.abs(dz) <= DEPTH || random.nextInt(3) != 0) {
					continue;   // not in the opening, and not everywhere
				}
				BlockPos at = site.offset(dx, 0, dz);
				if (!level.getBlockState(at).isAir()
					|| !level.getBlockState(at.below()).isSolid()) {
					continue;
				}
				level.setBlock(at, random.nextInt(3) == 0
					? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
					: Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 2);
				put++;
			}
		}
		HerobrineMod.LOGGER.info("the way was forced — {} blocks of it on the floor",
			put);
	}
}
