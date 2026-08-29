package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Chambers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * A system under a building, rather than a cellar with a passage off it.
 *
 * Every house so far has had the same thing beneath it: one chamber and one or
 * two bores that stop. That is a basement. What each of these should have is
 * somewhere you can get LOST — junctions, spurs, rooms at some ends and rock at
 * others, and a route through that has to be worked out rather than followed.
 *
 * THE NAVIGATION IS THE CONTENT, and it is the one thing a cave system can do
 * that a building cannot. A house is understood from the doorway; a warren is
 * understood only by having walked it, and the map a player ends up with is
 * theirs rather than the designer's. That is also why this is generated as a
 * branching tree and not laid out by hand — a hand-built maze is a puzzle with
 * a solution, and this is meant to be a place with a shape.
 *
 * SO IT IS MADE READABLE, deliberately, rather than made confusing. Getting
 * lost is only interesting if it can be recovered from:
 *
 *   - The TRUNK is paved and lit. It is the way he went and the way back.
 *   - SPURS are unlit and unpaved, and most of them stop in rock.
 *   - JUNCTIONS carry a cairn, so a player can tell one crossroads from
 *     another — which is the single thing that separates a place you are
 *     exploring from a place you are trapped in.
 *
 * Take those three away and this is a headache. With them it is a map somebody
 * else drew, in the dark, that you are reading by torchlight.
 */
public final class Warren {
	private Warren() {}

	/**
	 * What sort of warren this is.
	 *
	 * The same generator with different manners, because the four buildings
	 * that get one are four different people's work — or the same person at
	 * four different points, which is worse.
	 */
	public enum Manner {
		/** Neat, surveyed, lit at every junction. Somebody still had a plan. */
		SURVEYED,
		/** Institutional. Wide trunk, ordered spurs, everything squared. */
		WORKED,
		/** A crypt: short, low, and it goes down more than it goes along. */
		BURIED,
		/** Given up on. Long spurs, no lights, and the trunk stops being one. */
		FAILING,
	}

	public static BlockPos dig(ServerLevel level, BlockPos root, Manner manner,
	                           RandomSource random) {
		int trunkLegs = switch (manner) {
			case SURVEYED -> 5;
			case WORKED -> 6;
			case BURIED -> 3;
			case FAILING -> 4;
		};

		List<BlockPos> junctions = new ArrayList<>();
		BlockPos at = root;
		Vec3 heading = new Vec3(random.nextDouble() - 0.5, -0.18, random.nextDouble() - 0.5);

		// THE TRUNK IS CUT FIRST, ALL OF IT, before a single room or spur.
		// The homestead taught this the hard way: boring a passage after a
		// chest was already placed drove straight through it and left the books
		// on the floor as items counting down to despawning.
		for (int leg = 0; leg < trunkLegs; leg++) {
			int length = switch (manner) {
				case BURIED -> 10 + random.nextInt(8);
				case WORKED -> 20 + random.nextInt(10);
				default -> 15 + random.nextInt(12);
			};
			at = Digging.bore(level, at, heading, length,
				manner == Manner.WORKED ? 1.9 : 1.6, random, manner != Manner.FAILING);
			junctions.add(at);
			Digging.hollow(level, at, 3.0, random);
			// Turned rather than randomised, so the trunk reads as one route
			// that bends instead of four passages that happen to touch.
			heading = new Vec3(
				heading.x * 0.55 + (random.nextDouble() - 0.5),
				manner == Manner.BURIED ? -0.4 : -0.12,
				heading.z * 0.55 + (random.nextDouble() - 0.5));
		}

		// Then every spur, still before anything is put down.
		List<BlockPos> ends = new ArrayList<>();
		for (BlockPos junction : junctions) {
			int spurs = manner == Manner.FAILING ? 1 + random.nextInt(2) : 2;
			for (int i = 0; i < spurs; i++) {
				Vec3 off = new Vec3(random.nextDouble() - 0.5,
					(random.nextDouble() - 0.5) * 0.4, random.nextDouble() - 0.5);
				int length = manner == Manner.FAILING
					? 18 + random.nextInt(16)
					: 9 + random.nextInt(11);
				ends.add(Digging.bore(level, junction, off, length, 1.4, random));
			}
		}

		// And only now: rooms, marks, and the things at the ends.
		int roomsWanted = switch (manner) {
			case WORKED -> 3;
			case SURVEYED -> 3;
			default -> 2;
		};
		int placed = 0;
		List<BlockPos> passedOver = new ArrayList<>();
		for (BlockPos end : ends) {
			if (placed < roomsWanted && room(level, end)) {
				Chambers.place(level, end, random.nextInt(Chambers.kinds()), random);
				placed++;
				continue;
			}
			// Everything else simply stops, and stops the way a person stops:
			// rubble at the foot of a rough face rather than a clean wall.
			passedOver.add(end);
			givingUp(level, end, random);
		}

		// AND IT IS NEVER ALLOWED TO BE ENTIRELY EMPTY.
		//
		// room() wants sixty-two per cent of a nine-by-eight-by-nine box to be
		// untouched rock before it will carve a chamber, which is the right test
		// for "is there room here" and a terrible one to have no answer for. Bore a
		// warren through cavey ground and every single end fails it, placed stays
		// at zero, and what the player walks down into is tunnels, rubble at every
		// dead end, and a stack of marker blocks at each junction.
		//
		// Which is exactly what somebody reported: all the way down past the cells,
		// and the room at the end of it is two blocks. That was the cairn. There
		// was nothing else down there to find.
		//
		// The failure is silent and it is not rare — it depends entirely on what
		// the terrain happened to do underneath, so it works perfectly in testing
		// and produces a dead half-hour in somebody's world.
		//
		// So one is forced. Chambers.place carves its own space, so the rock test
		// is a preference rather than a requirement — the worst it can do on a bad
		// end is open a room into a cave, which is a strange room and not an empty
		// warren. The deepest end gets it, because that is the one somebody who
		// walked the whole thing has earned.
		if (placed == 0 && !passedOver.isEmpty()) {
			BlockPos deepest = passedOver.get(0);
			for (BlockPos end : passedOver) {
				if (end.getY() < deepest.getY()) {
					deepest = end;
				}
			}
			Chambers.place(level, deepest, random.nextInt(Chambers.kinds()), random);
			placed++;
			HerobrineMod.LOGGER.info(
				"no end had rock enough for a chamber — one forced at [{}, {}, {}]",
				deepest.getX(), deepest.getY(), deepest.getZ());
		}

		for (BlockPos junction : junctions) {
			cairn(level, junction, manner, random);
		}

		HerobrineMod.LOGGER.info("a {} warren under [{}, {}, {}]: {} junctions, {} ends, {} rooms",
			manner.name().toLowerCase(java.util.Locale.ROOT),
			root.getX(), root.getY(), root.getZ(), junctions.size(), ends.size(), placed);
		// The deepest junction, so a caller can hang something on the far end.
		return junctions.isEmpty() ? root : junctions.get(junctions.size() - 1);
	}

	/** Is there rock enough here to put a seven-by-seven room in? */
	private static boolean room(ServerLevel level, BlockPos at) {
		int solid = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				at.offset(-4, -2, -4), at.offset(4, 5, 4))) {
			if (level.getBlockState(pos).isSolid()) {
				solid++;
			}
		}
		// Most of it, not all: a room that only goes in where the rock is
		// perfect would never go in at all this far down.
		return solid > 400;
	}

	/**
	 * A marker at a crossroads, and it is the whole reason this is navigable.
	 *
	 * Three or four stacked blocks with a lantern on top, and no two the same
	 * height. That difference is doing all the work: identical markers tell a
	 * player they are at A junction, and markers that differ tell them WHICH,
	 * which is the entire distinction between exploring somewhere and being
	 * lost in it.
	 */
	private static void cairn(ServerLevel level, BlockPos at, Manner manner,
	                          RandomSource random) {
		BlockPos floor = Digging.groundUnder(level, at);
		if (floor == null) {
			return;
		}
		// THE WHOLE COLUMN IS CHECKED BEFORE ANY OF IT IS BUILT.
		//
		// This used to set blocks as it went and return the moment it met one that
		// was not air — which leaves a HALF cairn: one or two stones, no chiselled
		// cap, and the lantern skipped because the loop never reached it. An unlit
		// stump of two blocks at a junction, in a place whose one navigational aid
		// is that no two markers are the same height.
		//
		// Three or four, never two, because the doc above has always said three or
		// four and the code has always been able to roll two — and a two-high cairn
		// with a lantern is indistinguishable from the broken one.
		int height = 3 + random.nextInt(2);
		for (int up = 1; up <= height + 1; up++) {
			if (!level.getBlockState(floor.above(up)).isAir()) {
				return;      // not enough headroom. build nothing rather than half.
			}
		}
		for (int up = 1; up <= height; up++) {
			level.setBlock(floor.above(up), up == height
				? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
				: stack(random), 2);
		}
		// FAILING gets no lights, which is most of what makes it feel failing.
		if (manner != Manner.FAILING) {
			level.setBlock(floor.above(height + 1), Blocks.LANTERN.defaultBlockState(), 2);
		}
	}

	/**
	 * How a passage stops when nobody decided to stop.
	 *
	 * Not a wall and not a chamber: the cut gets rougher, rubble gathers at the
	 * foot of it, and there is nothing there. A tunnel ending in a room is a
	 * tunnel that was going somewhere. One ending mid-swing is a person who put
	 * the pick down and did not pick it back up, and that is a different
	 * sentence for about a dozen blocks.
	 */
	private static void givingUp(ServerLevel level, BlockPos end, RandomSource random) {
		Digging.props(level, end, 2, random);
		for (int i = 0; i < 10; i++) {
			BlockPos at = end.offset(random.nextInt(5) - 2, random.nextInt(3) - 1,
				random.nextInt(5) - 2);
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			BlockPos under = Digging.groundUnder(level, at);
			if (under == null) {
				continue;
			}
			level.setBlock(under, random.nextBoolean()
				? Blocks.COBBLESTONE.defaultBlockState()
				: Blocks.GRAVEL.defaultBlockState(), 2);
		}
	}

	/**
	 * One sign, at the mouth, and it is the only writing down here.
	 *
	 * Put at the top rather than at any of the ends, so it is read on the way
	 * IN — while it is still a warning rather than a summary.
	 */
	public static void warn(ServerLevel level, BlockPos at, String[] lines) {
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
			sign.setWaxed(true);
		}
	}

	private static BlockState stack(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
