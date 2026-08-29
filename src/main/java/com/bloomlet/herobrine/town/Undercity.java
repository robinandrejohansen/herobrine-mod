package com.bloomlet.herobrine.town;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The town under the town.
 *
 * A vaulted chamber the width of the square, forty blocks across and fourteen
 * high, with streets and houses and lantern posts in it and people still living
 * there. Not a cave with things in it — a CHAMBER. The distinction is the whole
 * design: caves are irregular and you read them as terrain, and the moment a
 * player sees a barrel vault and a row of pillars underground they understand
 * that this was quarried out on purpose by somebody who meant to stay.
 *
 * TWO WAYS IN, AND THEY ARE NOTHING ALIKE.
 *
 * The first is behind the altar in the church: a dry stone stair, no mechanism,
 * hidden only by being somewhere nobody looks. Finding it feels like
 * observation.
 *
 * The second is the well in the square, which is a swim. You go down the shaft
 * in the dark not knowing whether it opens out, and it does. Finding that one
 * feels like nerve.
 *
 * A secret with one entrance is a room. A secret with two, which are found
 * differently and arrive at opposite ends, is a place — because the second time
 * a player comes down they get to choose, and choosing is what makes somewhere
 * theirs.
 *
 * THE LIBRARY IS THE POINT OF IT. Whatever else is down here, the thing the
 * player will remember is a room with more books in it than the entire surface
 * town, in a settlement that has no school and whose houses hold four items
 * each. Somebody was reading down here, at length, in secret. The mod never
 * says what about.
 */
public final class Undercity {
	private Undercity() {}

	/** How far under the square. Deep enough that nothing above hints at it. */
	private static final int DEPTH = 26;
	/** Half-width of the chamber floor. */
	private static final int SPAN = 21;
	private static final int HEIGHT = 13;

	/**
	 * Where the library floor is, worked out from the square above it.
	 *
	 * Derived rather than stored, the same way the tower's summit is: the chamber
	 * hangs a fixed DEPTH under the town centre and the library sits at a fixed
	 * offset inside it, so both numbers already exist and a third copy in an
	 * attachment would only be the one that goes stale.
	 *
	 * Public because the map chain needs somewhere deliberate to leave the town's
	 * map, and the library is the only room down here that means anything: it is
	 * where the survivors keep what they have written.
	 */
	public static BlockPos libraryAt(BlockPos square) {
		return new BlockPos(square.getX(), square.getY() - DEPTH, square.getZ())
			.offset(-13, 0, -13);
	}

	public static void dig(ServerLevel level, BlockPos square, BlockPos crypt,
	                       RandomSource random) {
		BlockPos floor = new BlockPos(square.getX(), square.getY() - DEPTH, square.getZ());

		chamber(level, floor, random);
		pillars(level, floor, random);
		streets(level, floor, random);
		pool(level, floor);
		wellShaft(level, square, floor);
		cryptStair(level, crypt, floor);
		// AND THE STAIR IS JOINED TO THE CHAMBER, which it was not.
		//
		// The spiral carves a five-by-five shaft down to the chamber's floor
		// level and stops there, on the assumption that the church stands over
		// the chamber and the shaft therefore breaks into it. That held while the
		// chamber was a perfect disc of a known radius. It stopped holding the
		// moment the outline started to wander: the rim now comes in as close as
		// twelve blocks on some bearings, so on the church's bearing the shaft can
		// land entirely OUTSIDE the wall — and what the player walks down to is a
		// five-by-five dead end with nothing in it.
		//
		// A bore from the shaft foot to the middle fixes it for every shape,
		// because it stops depending on the shape at all.
		join(level, new BlockPos(crypt.getX(), floor.getY(), crypt.getZ()), floor);

		// WHERE THE ACCOUNTS END UP, AND IT IS SOMEBODY'S KITCHEN.
		//
		// They used to be six chests standing loose on the cavern floor at six
		// random bearings, and that one decision undid the entire room. A chest
		// on open ground is the most artificial object Minecraft has: nobody
		// keeps anything in a box in the middle of a street, so six of them in a
		// ring around a square read as loot markers with lore in them, which is
		// exactly what they were.
		//
		// They go in the HOUSES now, in the BARRELS the houses already had, next
		// to the bread. That is the whole difference between a note the mod left
		// for the player and a note somebody hid in their own pantry. It also
		// makes the room worth searching rather than worth scanning: the player
		// has to go indoors, into five separate people's homes, and open
		// containers most of which have nothing in them but food.
		//
		// Ten barrels across five houses and one in the library, and only six of
		// them hold anything. Which is what makes the ones that do land.
		java.util.List<BlockPos> accounts = new java.util.ArrayList<>();
		java.util.List<BlockPos> pantries = new java.util.ArrayList<>();
		BlockPos libraryAt = floor.offset(-13, 0, -13);
		library(level, libraryAt, random);
		for (int i = 0; i < 5; i++) {
			// AND ONE OF THE FIVE WAS BUILT THROUGH THE LIBRARY.
			//
			// Five houses on a ring of fourteen and the library at (-13, -13):
			// the fourth bearing lands its corner at (-5, -13), and a
			// seven-by-six house from there overlaps the library's east end by
			// three columns. The dwellings are raised after the library, so what
			// the player actually walked into was a stone house driven through a
			// wall of bookshelves — in the one room whose entire problem is that
			// it reads generated.
			//
			// Slid ROUND the ring rather than outward, because the rim wanders in
			// to twelve blocks on some bearings and pushing a house further from
			// the middle can put it through the cavern wall instead. The radius
			// is what keeps it indoors; the bearing is the free variable.
			double angle = i * (Math.PI * 2.0 / 5.0) + 0.6;
			BlockPos site = null;
			for (int nudge = 0; nudge < 10 && site == null; nudge++) {
				double bearing = angle + nudge * 0.22;
				int hx = floor.getX() + (int)Math.round(Math.cos(bearing) * 14);
				int hz = floor.getZ() + (int)Math.round(Math.sin(bearing) * 14);
				if (!overlaps(hx, hz, 7, 6, libraryAt.getX(), libraryAt.getZ(), 11, 9)) {
					site = new BlockPos(hx, floor.getY(), hz);
				}
			}
			if (site == null) {
				continue;   // four houses is better than one built through a wall
			}
			BlockPos[] barrels = dwelling(level, site, random);
			accounts.add(barrels[0]);
			pantries.add(barrels[1]);
		}
		// THE SIXTH IS IN THE READING ROOM, and it is the sixth on purpose. The
		// accounts are written in order and the last of them is the one who has
		// stopped being frightened — so his is the one that is not hidden in a
		// pantry at home. He keeps it on the shelf with the others, in the room
		// where they meet, because he no longer minds who reads it.
		accounts.add(libraryStore(level, libraryAt));

		HerobrineMod.LOGGER.info("undercity opened at [{}, {}, {}]",
			floor.getX(), floor.getY(), floor.getZ());

		// AND THE BOOKS GO DOWN LAST, after every tunnel and trap is cut.
		//
		// The homestead taught this the expensive way: boring a passage after a
		// chest existed drove straight through it and left the books on the floor
		// as items counting down to despawning. Nothing is carved after this.
		Testimony.write(level, accounts, pantries, random);

		// THE GAUNTLET IS GONE AND A FARM STANDS IN ITS PLACE.
		//
		// A hundred and forty blocks of four-block jumps over open lava used to run
		// east out of this wall, on the reasoning that a cult still meeting needs a
		// door a stranger cannot use. The reasoning was sound and the thing it
		// produced was a platforming level, and a platforming level is the one shape
		// that cannot be in this place: everything else down here is people quietly
		// getting on with living under a town that thinks they are dead, and you
		// cannot read that while counting your jumps.
		//
		// What replaces it answers the same question better. HOW ARE THEY STILL
		// ALIVE? Not "they are hard to reach" — that explains the door and not the
		// dinner. They are alive because they grew food down here, in the dark, for
		// years, and the evidence of that is worth ten times a gauntlet: a lit farm
		// under a town is the single most convincing thing a hidden settlement can
		// have in it, because nobody builds one unless they mean to stay.
		smallholding(level, floor, random);

		// AND THE PEOPLE GO IN LAST OF ALL.
		//
		// This used to run before the library shelves, the farm, the grove and the
		// pens, and the log said so every time: "Villager suffocated in a wall".
		// The grove plants an oak by choosing a floor square and stacking logs on
		// it, and nothing asked whether somebody was standing there — so a villager
		// placed thirty lines earlier got a tree through them.
		//
		// The file already knew the rule. Testimony.write carries a comment saying
		// the books go down last because boring a passage after a chest existed
		// drove through it. Entities are the same problem with a worse symptom: a
		// chest in a wall is invisible, and a villager in a wall is a death message.
		people(level, floor, random);
	}

	/** How far out from the chamber the workings run. */
	private static final int FARM = 14;

	/**
	 * WHAT THEY HAVE BEEN EATING, AND IT IS THE ANSWER TO THE OBVIOUS QUESTION.
	 *
	 * Three things, in three bays cut off the chamber, and each one is a different
	 * kind of proof:
	 *
	 *   THE FIELD. Tilled soil, water down the middle, four crops at every stage of
	 *   growth. Staggered on purpose — a field where everything is ripe at once is a
	 *   screenshot, and a field with seedlings in one row and stubble in the next is
	 *   somebody's rota. It is the stubble that says years.
	 *
	 *   THE GROVE. Trees, underground, under lanterns. This is the one that stops
	 *   people in the doorway: a tree needs light and there is no sky, so somebody
	 *   worked out the light and then waited a decade for the wood. Nothing says
	 *   patience like a full grown oak in a cellar.
	 *
	 *   THE PENS. Mushroom stems, composters, barrels. The unglamorous half, and the
	 *   half that makes the other two believable.
	 */
	private static void smallholding(ServerLevel level, BlockPos floor,
	                                 RandomSource random) {
		field(level, floor.relative(net.minecraft.core.Direction.EAST, SPAN - 4), random);
		grove(level, floor.relative(net.minecraft.core.Direction.WEST, SPAN - 4), random);
		pens(level, floor.relative(net.minecraft.core.Direction.NORTH, SPAN - 5), random);
		HerobrineMod.LOGGER.info("they have been feeding themselves down here");
	}

	private static void field(ServerLevel level, BlockPos middle, RandomSource random) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos at = middle.offset(dx, 0, dz);
				if (!level.getBlockState(at.above(2)).isSolid()
					&& !level.getBlockState(at.above(3)).isSolid()) {
					continue;      // outside the cut chamber
				}
				// A channel down the middle so every row is within four of water,
				// which is the actual rule farmland obeys and reads as competence.
				if (dz == 0) {
					level.setBlock(at, Blocks.WATER.defaultBlockState(), 2);
					continue;
				}
				level.setBlock(at, Blocks.FARMLAND.defaultBlockState()
					.setValue(BlockStateProperties.MOISTURE, 7), 2);
				BlockPos crop = at.above();
				if (!level.getBlockState(crop).isAir()) {
					continue;
				}
				// STAGGERED. Every row a different age, and one row in five bare,
				// because a rota has gaps in it and a decoration does not.
				int age = random.nextInt(8);
				if (age == 0 && random.nextBoolean()) {
					continue;
				}
				level.setBlock(crop, switch (Math.abs(dz) % 4) {
					case 0, 1 -> Blocks.WHEAT.defaultBlockState()
						.setValue(BlockStateProperties.AGE_7, age % 8);
					case 2 -> Blocks.CARROTS.defaultBlockState()
						.setValue(BlockStateProperties.AGE_7, age % 8);
					default -> Blocks.POTATOES.defaultBlockState()
						.setValue(BlockStateProperties.AGE_7, age % 8);
				}, 2);
			}
		}
		// The light it lives under, on posts, because farmland underground is a
		// claim and the lanterns are the receipt.
		for (int dx = -5; dx <= 5; dx += 5) {
			for (int dz = -3; dz <= 3; dz += 6) {
				BlockPos post = middle.offset(dx, 1, dz);
				level.setBlock(post, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
				level.setBlock(post.above(), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
				level.setBlock(post.above(2), Blocks.LANTERN.defaultBlockState(), 2);
			}
		}
	}

	private static void grove(ServerLevel level, BlockPos middle, RandomSource random) {
		for (int i = 0; i < 7; i++) {
			int dx = random.nextInt(11) - 5;
			int dz = random.nextInt(9) - 4;
			BlockPos root = middle.offset(dx, 0, dz);
			if (!level.getBlockState(root.above()).isAir()
				|| !level.getBlockState(root).isSolid()) {
				continue;
			}
			level.setBlock(root, Blocks.PODZOL.defaultBlockState(), 2);
			// Short, because the chamber is thirteen high and a full canopy would
			// go through the ceiling — and a tree that has been topped to fit the
			// room is more convincing than one that fits by luck.
			int tall = 3 + random.nextInt(2);
			for (int up = 1; up <= tall; up++) {
				level.setBlock(root.above(up), Blocks.OAK_LOG.defaultBlockState(), 2);
			}
			for (int lx = -2; lx <= 2; lx++) {
				for (int lz = -2; lz <= 2; lz++) {
					for (int ly = tall - 1; ly <= tall + 1; ly++) {
						if (Math.abs(lx) + Math.abs(lz) + Math.abs(ly - tall) > 3) {
							continue;
						}
						BlockPos leaf = root.offset(lx, ly, lz);
						if (level.getBlockState(leaf).isAir()) {
							level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState()
								.setValue(BlockStateProperties.PERSISTENT,
									true), 2);
						}
					}
				}
			}
			if (random.nextBoolean()) {
				level.setBlock(root.above(tall + 2), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}
		// Undergrowth, so it is a wood rather than seven trees in a row.
		for (int i = 0; i < 40; i++) {
			BlockPos at = middle.offset(random.nextInt(13) - 6, 1, random.nextInt(11) - 5);
			if (!level.getBlockState(at).isAir()
				|| !level.getBlockState(at.below()).isSolid()) {
				continue;
			}
			level.setBlock(at, switch (random.nextInt(5)) {
				case 0 -> Blocks.FERN.defaultBlockState();
				case 1 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
				case 2 -> Blocks.RED_MUSHROOM.defaultBlockState();
				case 3 -> Blocks.MOSS_CARPET.defaultBlockState();
				default -> Blocks.SHORT_GRASS.defaultBlockState();
			}, 2);
		}
	}

	private static void pens(ServerLevel level, BlockPos middle, RandomSource random) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				BlockPos at = middle.offset(dx, 1, dz);
				if (!level.getBlockState(at).isAir()) {
					continue;
				}
				boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 3;
				if (edge) {
					level.setBlock(at, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
					continue;
				}
				if (random.nextInt(4) == 0) {
					level.setBlock(at, random.nextBoolean()
						? Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()
						: Blocks.MUSHROOM_STEM.defaultBlockState(), 2);
				}
			}
		}
		// The working end: somewhere to put the waste and somewhere to keep the
		// seed, which is the pair of blocks that says this is a system.
		level.setBlock(middle.offset(0, 1, 0), Blocks.COMPOSTER.defaultBlockState(), 2);
		level.setBlock(middle.offset(1, 1, 0), Blocks.BARREL.defaultBlockState()
			.setValue(BlockStateProperties.FACING, net.minecraft.core.Direction.UP), 2);
		level.setBlock(middle.offset(-1, 1, 0), Blocks.HAY_BLOCK.defaultBlockState(), 2);
	}

	/**
	 * Hollow it, and vault the ceiling.
	 *
	 * The roof curves down toward the walls rather than stopping flat, which is
	 * the single thing that separates this from a very large room. A flat
	 * ceiling at this span would need pillars every four blocks to look
	 * plausible and would still read as a warehouse; a barrel vault carries
	 * itself and lets the middle stay open.
	 */
	/**
	 * HOW MANY BEARINGS THE OUTLINE IS DRAWN ON.
	 *
	 * Sixty-four is fine enough that no straight facets show at this span and
	 * coarse enough that the wall wanders in bays rather than in fringe.
	 */
	private static final int BEARINGS = 64;

	private static void chamber(ServerLevel level, BlockPos floor, RandomSource random) {
		// THE SHAPE WAS THE WHOLE PROBLEM, AND NO AMOUNT OF DRESSING FIXES IT.
		//
		// This was sqrt(dx*dx + dz*dz) tested against a constant, with a cosine
		// vault over it — a mathematically perfect disc under a mathematically
		// perfect dome. Every complaint about the undercity reading fake was a
		// complaint about those two lines: wood framing, water channels and
		// overgrown grass laid inside a shape like that still read as decoration
		// applied to a generated volume, because they are.
		//
		// So the outline is drawn once per build as a wandering radius — three
		// harmonics at random phases summed around the circle — and every column
		// reads its limit off that. Three, because one gives an egg and a dozen
		// gives noise: three gives LOBES, which is what a worked-out cavern is.
		// Bays, headlands, a couple of places where the wall comes in close and
		// somewhere it opens right out.
		double[] rim = new double[BEARINGS];
		double[] cap = new double[BEARINGS];
		double p1 = random.nextDouble() * Math.PI * 2.0;
		double p2 = random.nextDouble() * Math.PI * 2.0;
		double p3 = random.nextDouble() * Math.PI * 2.0;
		for (int b = 0; b < BEARINGS; b++) {
			double a = b * Math.PI * 2.0 / BEARINGS;
			rim[b] = SPAN
				+ Math.sin(a * 2 + p1) * (SPAN * 0.22)
				+ Math.sin(a * 3 + p2) * (SPAN * 0.13)
				+ Math.sin(a * 5 + p3) * (SPAN * 0.07);
			// The roof wanders on its own phases, so the highest part of the
			// ceiling is NOT over the middle of the floor. A dome's apex sitting
			// dead centre is the other half of what gives it away.
			cap[b] = HEIGHT + Math.sin(a * 2 + p3) * 2.5 + Math.sin(a * 3 + p1) * 1.5;
		}

		for (int dx = -SPAN - 6; dx <= SPAN + 6; dx++) {
			for (int dz = -SPAN - 6; dz <= SPAN + 6; dz++) {
				double reach = Math.sqrt(dx * dx + dz * dz);
				if (reach > SPAN + 6) {
					continue;
				}
				// Which bearing this column sits on, interpolated between the two
				// nearest so the wall curves instead of stepping.
				double turn = (Math.atan2(dz, dx) + Math.PI * 2.0) % (Math.PI * 2.0);
				double slot = turn / (Math.PI * 2.0) * BEARINGS;
				int lo = (int)Math.floor(slot) % BEARINGS;
				int hi = (lo + 1) % BEARINGS;
				double mix = slot - Math.floor(slot);
				double edge = rim[lo] + (rim[hi] - rim[lo]) * mix;
				double top = cap[lo] + (cap[hi] - cap[lo]) * mix;
				if (reach > edge + 2) {
					continue;
				}

				double t = Math.min(1.0, reach / edge);
				int roof = (int)Math.round(top * Math.cos(t * Math.PI / 2.0));

				for (int dy = -1; dy <= HEIGHT + 4; dy++) {
					BlockPos at = floor.offset(dx, dy, dz);
					if (dy == -1) {
						level.setBlock(at, ground(random, reach / edge), 2);
					} else if (dy <= roof && reach <= edge) {
						level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
					} else if (dy <= roof + 1) {
						level.setBlock(at, vaulting(random), 2);
					}
				}
			}
		}
	}

	/**
	 * THE FLOOR STOPS BEING A DISC OF PAVING.
	 *
	 * Laid stone in the middle where people actually stand and work, and then it
	 * gives out — the edges are dirt, gravel and moss with grass growing over
	 * them, because nobody paves the far corners of a cavern and because a floor
	 * that changes underfoot is the cheapest way to say which parts of a room are
	 * used.
	 *
	 * @param out how far toward the wall this column is, nought at the middle
	 */
	private static BlockState ground(RandomSource random, double out) {
		if (out < 0.45) {
			return paving(random);
		}
		// A ragged margin rather than a ring: the further out, the likelier it
		// has gone back to earth, so the boundary frays instead of drawing a line.
		if (random.nextDouble() > (out - 0.35) * 1.6) {
			return paving(random);
		}
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSS_BLOCK.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.COARSE_DIRT.defaultBlockState();
		}
		return roll < 9
			? Blocks.GRAVEL.defaultBlockState()
			: Blocks.PODZOL.defaultBlockState();
	}

	/**
	 * Two rings of them, and they are not structural.
	 *
	 * They hold nothing up — the vault does that. They are there because a
	 * forty-block room with nothing in the middle distance has no sense of
	 * scale, and a player walking between columns can finally tell how far
	 * across it is.
	 */
	private static void pillars(ServerLevel level, BlockPos floor, RandomSource random) {
		for (int ring : new int[] { 9, 17 }) {
			int count = ring == 9 ? 6 : 10;
			for (int i = 0; i < count; i++) {
				double angle = i * (Math.PI * 2.0 / count);
				int px = floor.getX() + (int)Math.round(Math.cos(angle) * ring);
				int pz = floor.getZ() + (int)Math.round(Math.sin(angle) * ring);
				double reach = Math.hypot(px - floor.getX(), pz - floor.getZ());
				int top = (int)Math.round(HEIGHT * Math.cos(Math.min(1.0, reach / SPAN)
					* Math.PI / 2.0));

				for (int dy = 0; dy <= top; dy++) {
					BlockPos at = new BlockPos(px, floor.getY() + dy, pz);
					level.setBlock(at, dy == top
						? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				// A lantern on each, at head height, which is what actually
				// lights the room.
				level.setBlock(new BlockPos(px, floor.getY() + 3, pz)
					.relative(Direction.Plane.HORIZONTAL.iterator().next()),
					Blocks.AIR.defaultBlockState(), 2);
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos hook = new BlockPos(px, floor.getY() + 3, pz).relative(side);
					if (level.getBlockState(hook).isAir()) {
						level.setBlock(hook, Blocks.LANTERN.defaultBlockState(), 2);
						break;
					}
				}
			}
		}
	}

	/** Four roads out from the middle, so the floor is not one flat disc. */
	private static void streets(ServerLevel level, BlockPos floor, RandomSource random) {
		for (Direction lane : Direction.Plane.HORIZONTAL) {
			for (int out = 3; out <= SPAN - 1; out++) {
				for (int side = -1; side <= 1; side++) {
					Direction across = lane.getClockWise();
					BlockPos at = floor.relative(lane, out).relative(across, side).below();
					level.setBlock(at, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The pool the well drops into.
	 *
	 * Three deep, because that is the shallowest a player can fall into from
	 * any height without being hurt, and the whole water entrance depends on
	 * somebody surviving the arrival without being told they would.
	 */
	private static void pool(ServerLevel level, BlockPos floor) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (dx * dx + dz * dz > 10) {
					continue;
				}
				for (int dy = -3; dy <= -1; dy++) {
					level.setBlock(floor.offset(dx, dy, dz),
						Blocks.WATER.defaultBlockState(), 2);
				}
				level.setBlock(floor.offset(dx, -4, dz),
					Blocks.STONE_BRICKS.defaultBlockState(), 2);
			}
		}
		// A kerb, so it reads as a basin rather than as a hole that flooded.
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > 3.4 && reach <= 4.4) {
					level.setBlock(floor.offset(dx, -1, dz),
						Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The well, all the way down, full of water.
	 *
	 * Water the entire height rather than a drop into a pool at the bottom.
	 * A shaft that is dry until the last three blocks kills the first player
	 * who tries it, and a secret entrance that kills you is not a secret
	 * entrance, it is a trap — and DESIGN §9 does not allow one.
	 */
	private static void wellShaft(ServerLevel level, BlockPos square, BlockPos floor) {
		for (int y = floor.getY() + 1; y <= square.getY(); y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos at = new BlockPos(square.getX() + dx, y, square.getZ() + dz);
					boolean core = dx == 0 && dz == 0;
					level.setBlock(at, core
						? Blocks.WATER.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * And the dry way, down from behind the altar.
	 *
	 * A proper spiral rather than a ladder. The player should be walking for
	 * long enough to stop believing it is a cellar.
	 */
	/**
	 * Cut from the foot of a shaft to the middle of the chamber.
	 *
	 * Walked as a straight line rather than pathfound, and it simply stops as soon
	 * as it is standing in cave air — so on a bearing where the rim is generous it
	 * cuts almost nothing, and on one where the wall has closed in it cuts
	 * however far it has to. Either way the stair arrives somewhere.
	 */
	private static void join(ServerLevel level, BlockPos from, BlockPos floor) {
		int steps = (int)Math.ceil(Math.sqrt(from.distSqr(floor)));
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 1.0 : (double)i / steps;
			int x = (int)Math.round(from.getX() + (floor.getX() - from.getX()) * t);
			int z = (int)Math.round(from.getZ() + (floor.getZ() - from.getZ()) * t);
			BlockPos at = new BlockPos(x, floor.getY(), z);
			// Already inside the room, and there is nothing left to cut.
			//
			// SEVEN, NOT TWO, AND THAT TWO WAS THE DEAD END. cryptStair finishes
			// by carving a five-by-five-by-four box of CAVE_AIR where the shaft
			// bottoms out, to break it into the chamber. This bore starts in the
			// middle of that box — so with the exemption at two, the very next
			// step tested a block of the spiral's OWN breakout, found air,
			// concluded it had arrived, and returned without cutting anything.
			//
			// The player walked a minute down the stair behind the altar and
			// arrived in a five-by-five room with nothing in it and no way on.
			//
			// It only happened SOMETIMES, which is what made it hard to see: the
			// box reaches up to four blocks from this bore's start, but only on
			// the side the spiral's last step happened to land. When the chamber
			// lay the other way the third step was already in rock and the bore
			// cut normally. Half the towns were fine.
			//
			// Seven clears the box with two blocks to spare, and the two extra
			// steps cost nothing if the chamber wall really is that close — the
			// cut is a no-op in air and the paving goes under a floor that is
			// already floor. And because nothing else down here is CAVE_AIR at
			// this level, the first air found after that genuinely is the room.
			if (i > 6 && level.getBlockState(at.above()).is(Blocks.CAVE_AIR)) {
				return;
			}
			for (int wide = -1; wide <= 1; wide++) {
				for (int up = 0; up <= 3; up++) {
					level.setBlock(at.offset(wide, up, 0), Blocks.CAVE_AIR.defaultBlockState(), 2);
					level.setBlock(at.offset(0, up, wide), Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
			level.setBlock(at.below(), paving(level.getRandom()), 2);
		}
	}

	private static void cryptStair(ServerLevel level, BlockPos crypt, BlockPos floor) {
		// A TIGHT SPIRAL IN A SHAFT, not a staircase that wanders.
		//
		// The first version walked three blocks along its heading per level and
		// only turned every fourth one, which over twenty-six levels of descent
		// carries it something like seventy blocks sideways — straight out from
		// under the church, out past the wall, and up through the hillside
		// outside the town. The "secret way in" came out in a field.
		//
		// Wound round a five-by-five shaft instead, so it descends where it
		// starts and the entrance stays inside the building it belongs to. The
		// player is still walking for the best part of a minute, which is the
		// only thing the length was ever for.
		int[][] ring = { {1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3},
			{2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0} };

		// AND THE MOUTH ITSELF GETS OPENED, WHICH IT NEVER WAS.
		//
		// The loop below starts at step 0, whose ring cell is {1, 0} — so the first
		// block it ever cuts is one to the side of the crypt position, and the
		// crypt position itself is left exactly as the church left it. The church
		// puts CHISELED_DEEPSLATE there: it is the middle of the reredos, the wall
		// behind the altar.
		//
		// So whether the stair had a doorway at all came down to whether the block
		// beside the reredos happened to be open, which depends on the plot's
		// rotation. Sometimes you walked down. Sometimes you stood in front of a
		// solid wall with a minute of staircase behind it and no way in — reported
		// as "av og til er den stengt", and it was exactly that often.
		for (int dy = 0; dy <= 3; dy++) {
			level.setBlock(crypt.above(dy), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}

		BlockPos head = crypt.offset(-1, 0, -1);
		int step = 0;
		BlockPos at = crypt;

		for (int y = crypt.getY(); y > floor.getY(); y--) {
			int[] cell = ring[step % ring.length];
			at = new BlockPos(head.getX() + cell[0], y, head.getZ() + cell[1]);
			step++;

			// FOUR high, not three. The old passage was cut nought to two and
			// then hung a lantern at two, which is the block a player's head
			// occupies — so the light itself was the thing blocking the way
			// down. Cutting one higher gives them two clear blocks to stand in
			// and a ceiling to hang from.
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(at.above(dy), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
			level.setBlock(at.below(), Blocks.STONE_BRICKS.defaultBlockState(), 2);

			if (step % 5 == 0) {
				level.setBlock(at.above(3), Blocks.LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, true), 2);
			}
		}

		// Break out into the chamber wherever the shaft bottoms out.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlock(at.offset(dx, dy, dz),
						Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The library, and it is the largest building down here.
	 *
	 * More books than the entire surface town owns, in a settlement with no
	 * school. That contrast is the only thing this room has to say and it says
	 * it without a single sign.
	 */
	private static void library(ServerLevel level, BlockPos at, RandomSource random) {
		int w = 11;
		int d = 9;
		int h = 6;

		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				for (int dy = 0; dy <= h; dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
					boolean roof = dy == h;

					if (roof) {
						level.setBlock(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
					} else if (wall && dy > 0) {
						level.setBlock(pos, random.nextInt(5) == 0
							? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState(), 2);
					} else if (dy == 0) {
						level.setBlock(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
					} else {
						level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}

		// The doorway.
		for (int dy = 1; dy <= 2; dy++) {
			level.setBlock(at.offset(w / 2, dy, d - 1), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}

		// Shelves up both long walls, floor to ceiling, and an island in the
		// middle. Stacked rather than a single course — a wall one bookshelf
		// high is a bookcase, and four high is a library.
		for (int dz = 1; dz < d - 1; dz++) {
			for (int dy = 1; dy <= 4; dy++) {
				level.setBlock(at.offset(1, dy, dz), shelf(random), 2);
				level.setBlock(at.offset(w - 2, dy, dz), shelf(random), 2);
			}
		}
		for (int dx = 4; dx <= 6; dx++) {
			for (int dy = 1; dy <= 3; dy++) {
				level.setBlock(at.offset(dx, dy, 3), shelf(random), 2);
				level.setBlock(at.offset(dx, dy, 5), shelf(random), 2);
			}
		}

		// A reading desk, a lectern with something open on it, and light.
		level.setBlock(at.offset(w / 2, 1, 1), Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		level.setBlock(at.offset(3, 1, 7), Blocks.DARK_OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		level.setBlock(at.offset(7, 1, 7), Blocks.DARK_OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		for (int dx : new int[] { 3, 7 }) {
			level.setBlock(at.offset(dx, 5, 4), Blocks.LANTERN.defaultBlockState()
				.setValue(BlockStateProperties.HANGING, true), 2);
		}
	}

	/**
	 * Do two footprints share any ground?
	 *
	 * A margin of one on every side, so buildings do not merely miss each other
	 * but have a gap between them — two stone houses sharing a wall read as one
	 * odd building rather than as two.
	 */
	private static boolean overlaps(int ax, int az, int aw, int ad,
	                                int bx, int bz, int bw, int bd) {
		return ax - 1 < bx + bw && ax + aw + 1 > bx
			&& az - 1 < bz + bd && az + ad + 1 > bz;
	}

	/** One in six is a chiselled shelf, so the wall is not a flat texture. */
	private static BlockState shelf(RandomSource random) {
		return random.nextInt(6) == 0
			? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
			: Blocks.BOOKSHELF.defaultBlockState();
	}

	/**
	 * A small stone house, of which there are five.
	 *
	 * @return its two barrels. The first is the one that gets an account and the
	 *         second is only food — and which corner is which is rolled per
	 *         house, so a player who works out that it is always the barrel by
	 *         the door has worked out nothing.
	 */
	private static BlockPos[] dwelling(ServerLevel level, BlockPos at, RandomSource random) {
		int w = 7;
		int d = 6;
		int h = 4;

		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				for (int dy = 0; dy <= h; dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
					if (dy == h) {
						level.setBlock(pos, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
							.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), 2);
					} else if (wall && dy > 0) {
						level.setBlock(pos, random.nextInt(4) == 0
							? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState(), 2);
					} else if (dy == 0) {
						level.setBlock(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
					} else {
						level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
			}
		}
		for (int dy = 1; dy <= 2; dy++) {
			level.setBlock(at.offset(w / 2, dy, d - 1), Blocks.CAVE_AIR.defaultBlockState(), 2);
		}
		level.setBlock(at.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
		level.setBlock(at.offset(2, 1, d - 2), Blocks.BOOKSHELF.defaultBlockState(), 2);
		level.setBlock(at.offset(w / 2, 3, d / 2), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
		// A trapdoor over each window hole, shut, which is the town's own idiom.
		level.setBlock(at.offset(0, 2, d / 2), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
			.setValue(BlockStateProperties.HALF, Half.BOTTOM), 2);

		// TWO BARRELS, and the second one is the point of having two.
		//
		// One container per house that always holds a book is a chest with a
		// different model on it — the player learns after the second house that
		// every barrel down here is a lore drop, and the searching stops being
		// searching. With two per house and only one filled, most of what they
		// open is somebody's flour, and that is what makes finding an account
		// feel like finding something rather than collecting it.
		//
		// A barrel stands upright with no facing to get wrong, which is also
		// exactly how a household one would sit.
		BlockPos byTheDoor = at.offset(w - 2, 1, 1);
		BlockPos inTheCorner = at.offset(w - 2, 1, d - 2);
		level.setBlock(byTheDoor, Blocks.BARREL.defaultBlockState(), 2);
		level.setBlock(inTheCorner, Blocks.BARREL.defaultBlockState(), 2);
		return random.nextBoolean()
			? new BlockPos[] { byTheDoor, inTheCorner }
			: new BlockPos[] { inTheCorner, byTheDoor };
	}

	/**
	 * The library's own barrel, by the door.
	 *
	 * Placed separately from library() rather than inside it, because the
	 * shelving loop runs the full length of both long walls and would overwrite
	 * anything standing against them. Simpler to put it down after the room is
	 * finished than to carve an exception into the loop.
	 */
	private static BlockPos libraryStore(ServerLevel level, BlockPos at) {
		BlockPos store = at.offset(2, 1, 7);
		level.setBlock(store, Blocks.BARREL.defaultBlockState(), 2);
		return store;
	}

	/**
	 * And people, still living there.
	 *
	 * Ordinary villagers, persistent, unmodified. Nothing is wrong with them
	 * and nothing is supposed to be — the unsettling part is that they are fine
	 * and they are forty blocks under a town that does not mention them.
	 */
	private static final net.minecraft.resources.ResourceKey<
		net.minecraft.world.entity.npc.villager.VillagerProfession>[] TRADES =
			new net.minecraft.resources.ResourceKey[] {
				net.minecraft.world.entity.npc.villager.VillagerProfession.FARMER,
				net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN,
				net.minecraft.world.entity.npc.villager.VillagerProfession.CLERIC,
				net.minecraft.world.entity.npc.villager.VillagerProfession.MASON,
				net.minecraft.world.entity.npc.villager.VillagerProfession.BUTCHER,
				net.minecraft.world.entity.npc.villager.VillagerProfession.SHEPHERD,
			};

	/** How many spots each of them is allowed to try before giving up. */
	private static final int LOOKS_FOR_A_SPOT = 24;

	/**
	 * Somewhere a person can actually stand.
	 *
	 * A chamber this size is not empty ground. There are pillars, five houses, a
	 * library, a pool, streets, and now a field, a grove and a set of pens — and
	 * the old placement was a random polar coordinate with NO TEST AT ALL. It put
	 * villagers inside walls perfectly often, and Minecraft has a message for that
	 * which it prints straight into the chat of whoever is standing nearby.
	 *
	 * Two clear blocks over something solid. That is the whole test and it is the
	 * one that was missing.
	 */
	private static boolean roomToStand(ServerLevel level, BlockPos feet) {
		return level.getBlockState(feet).isAir()
			&& level.getBlockState(feet.above()).isAir()
			&& level.getBlockState(feet.below()).isSolid();
	}

	private static void people(ServerLevel level, BlockPos floor, RandomSource random) {
		int placed = 0;
		for (int i = 0; i < 9; i++) {
			BlockPos feet = null;
			for (int look = 0; look < LOOKS_FOR_A_SPOT && feet == null; look++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double range = 5.0 + random.nextDouble() * (SPAN - 8);
				BlockPos at = new BlockPos(
					floor.getX() + (int)Math.round(Math.cos(angle) * range),
					floor.getY(),
					floor.getZ() + (int)Math.round(Math.sin(angle) * range));
				// A block either side of the floor line as well, because the chamber
				// is not perfectly flat — the streets and the pens sit a course up.
				for (int dy = 0; dy <= 1 && feet == null; dy++) {
					if (roomToStand(level, at.above(dy))) {
						feet = at.above(dy);
					}
				}
			}
			if (feet == null) {
				continue;      // nowhere for this one. better than a wall.
			}

			Mob villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
			if (villager == null) {
				continue;
			}
			placed++;
			villager.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
				random.nextFloat() * 360.0F, 0.0F);
			villager.setPersistenceRequired();
			// THE SAME TRADES AS THE TOWN ABOVE, and that is the whole point.
			//
			// Bare villagers down here would read as a spawner: no trades, no
			// professions, nothing to say when clicked. Giving them the SAME jobs
			// as the people upstairs says something much worse than anything a
			// sign could — this is not a dungeon full of prisoners, it is the town,
			// again, underneath the town, with the same butcher and the same
			// librarian in it. Nobody explains why and nobody has to.
			if (villager instanceof net.minecraft.world.entity.npc.villager.Villager who) {
				who.setVillagerData(who.getVillagerData().withProfession(
					level.registryAccess(), TRADES[random.nextInt(TRADES.length)]));
			}
			level.addFreshEntity(villager);
		}
		HerobrineMod.LOGGER.info("{} of them are still down there", placed);
	}

	private static BlockState paving(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState vaulting(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 5) {
			return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		}
		if (roll < 8) {
			return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
		}
		return Blocks.DEEPSLATE_TILES.defaultBlockState();
	}
}
