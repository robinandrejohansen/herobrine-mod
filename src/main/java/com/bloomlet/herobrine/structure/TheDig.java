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

	/**
	 * Where the one chest in this building ends up, from the surface origin alone.
	 *
	 * Public for the same reason Undercity publishes libraryAt: the map chain has
	 * to leave the way to the church somewhere findable, and the gaol's only
	 * container is at the far end of a thirty-four block hall sixteen down, which
	 * is nowhere near the doorstep the default search starts from.
	 *
	 * This is the arithmetic in build(), read forwards. The stair drops DROP from a
	 * corner one block back from the origin, the hall starts two under its landing
	 * and runs HALL along z, and keeps() sets the chest three across and five
	 * beyond the far end. Approximate by a block or two on x and z — the spiral's
	 * last tread lands on whichever corner DROP happens to end on — and that is
	 * fine, because the caller searches a radius around this rather than reading
	 * the single block. It only has to land inside the right room.
	 */
	public static BlockPos keepAt(ServerLevel level, BlockPos origin) {
		int ground = Ground.topOf(level, origin.getX(), origin.getZ()) + 1;
		return new BlockPos(origin.getX() - 3, ground - DROP - 2, origin.getZ() + HALL + 5);
	}
	private static final int HALL = 34;
	private static final int CELLS_PER_SIDE = 7;

	public static void build(ServerLevel level, BlockPos origin, RandomSource random) {
		BlockPos top = new BlockPos(origin.getX(),
			Ground.topOf(level, origin.getX(), origin.getZ()) + 1, origin.getZ());

		gatehouse(level, top, random);
		BlockPos landing = Descent.stair(level, top.offset(-1, 0, -1), DROP,
			brick(random), random);

		BlockPos start = landing.below(2);
		BlockPos far = hall(level, start, random);
		// SHELL, THEN CARVE, THEN FURNISH — and it was shell, furnish, carve.
		//
		// workings() calls Warren.dig from a block inside the warder's back wall,
		// and Warren.dig bores a six-leg trunk and a dozen spurs out of it. The
		// chest was already standing in that room when it started. Warren's own
		// comment states the rule — "the homestead taught this the hard way: boring
		// a passage after a chest was already placed drove straight through it and
		// left the books on the floor as items counting down to despawning" — and
		// this file broke it at the call site.
		//
		// The room still has to be BUILT before the bore, or the back wall goes up
		// after the tunnel and seals its mouth. So it is split: warder raises the
		// room and everything fixed in it, workings cuts, and keeps() puts the chest
		// down last, when nothing is going to dig any more.
		warder(level, far, random);
		workings(level, far, random);
		keeps(level, far, random);
		// AND THE SCRATCHES GO IN AFTER THE BORE, for the reason the chest does.
		// workings() cuts a Warren out of the back of the warder's room, and a
		// Warren spur is free to come back along the hall. A sign is not a chest —
		// nothing despawns — but a note with a tunnel through it is a note nobody
		// reads, and being read is the whole of what they are for.
		dressCells(level, start, random);

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

		for (Cell spot : cells(start)) {
			cell(level, spot.mouth(), spot.into(), random, spot.shut());
		}
		return start.offset(0, 0, HALL - 1);
	}

	/** A cell mouth, the way it opens, and whether it was left shut. */
	private record Cell(BlockPos mouth, Direction into, boolean shut) {}

	/**
	 * THE FOURTEEN, IN THE ORDER THEY WERE NUMBERED, as one list rather than two
	 * copies of the same arithmetic.
	 *
	 * hall() carves from this and dressCells() writes into it, and those two run
	 * at opposite ends of build() with a Warren bore in between. Two loops each
	 * working out `spacing` for themselves is the setup for the quietest bug in
	 * the file: change CELLS_PER_SIDE and every note in the building lands in a
	 * wall one cell over from the person it belongs to, with nothing failing.
	 *
	 * West before east at each rank, and that is what makes the shut one the
	 * NINTH. It is not arranged — CELLS_PER_SIDE - 2 falls on rank five, west
	 * side, which is index eight, which is cell nine. theGaolAfter() has called
	 * it cell nine since it was written, and it turns out to be telling the truth.
	 */
	private static java.util.List<Cell> cells(BlockPos start) {
		java.util.List<Cell> found = new java.util.ArrayList<>();
		int spacing = HALL / (CELLS_PER_SIDE + 1);
		for (int i = 1; i <= CELLS_PER_SIDE; i++) {
			int out = i * spacing;
			// The shut one is always the same distance in, so it is not the
			// last cell and not the first — it is one they walk past twice.
			boolean shut = i == CELLS_PER_SIDE - 2;
			// AWAY FROM THE CORRIDOR, WHICH IS THE OPPOSITE OF WHAT THIS SAID.
			//
			// `into` is the direction a cell extends from its own mouth, and both of
			// these named the way the DOOR faces instead. So the cell on the left of
			// the hall was carved eastward — straight through the corridor the hall
			// had just cut, into the cell opposite — and each one dropped its back
			// wall at in == 5 in the middle of the walkway.
			//
			// The two then overwrote each other, last one wins, and what a player
			// found on opening a door was the far cell's brickwork a block away.
			// Reported as "the cell is blocked again on the other side of the door",
			// which is exactly what it was.
			found.add(new Cell(start.offset(-3, 0, out), Direction.WEST, shut));
			found.add(new Cell(start.offset(3, 0, out), Direction.EAST, false));
		}
		return found;
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

		// ---- THE FRONTAGE, and it is built properly now.
		//
		// It used to be a three-wide strip of iron bars with a hole in the middle,
		// floating: no frame, no lintel, nothing for the bars to be fixed to, and
		// the top course level with the door so the whole thing read as a fence
		// somebody had leaned against a hole. "Metal gjerde er dårlig laget", and
		// it was.
		//
		// A stone frame either side and a lintel over, the bars running the full
		// height between them, and the doorway two clear blocks. Which is how a
		// barred frontage is actually built — the iron spans an opening in a wall
		// rather than being the wall.
		for (int up = 0; up <= 3; up++) {
			for (int side = -2; side <= 2; side++) {
				BlockPos at = mouth.relative(across, side).above(up);
				if (Math.abs(side) == 2 || up == 3) {
					level.setBlock(at, brick(random), 2);      // frame and lintel
				} else if (side == 0) {
					level.setBlock(at, up <= 1
						? Blocks.CAVE_AIR.defaultBlockState()
						: brick(random), 2);                   // the doorway
				} else {
					level.setBlock(at, Blocks.IRON_BARS.defaultBlockState(), 2);
				}
			}
		}
		BlockState door = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, into.getOpposite())
			.setValue(BlockStateProperties.OPEN, !shut);
		level.setBlock(mouth, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(mouth.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);

		// AND THE LEVER THAT WORKS IT, WHICH THERE WAS NOT ONE OF.
		//
		// A shut cell was an IRON door and nothing else. Iron doors do not open by
		// hand — that is the entire difference between iron and oak — so every
		// locked cell in the building was sealed for good, and the fourteen rooms
		// this whole floor exists to show you could not be looked into.
		//
		// A lever on the corridor wall beside each one. It is also the correct
		// answer fictionally: a warder does not carry fourteen keys, he throws the
		// bolt from where he is standing, and COUNT THEM IN / COUNT THEM OUT is
		// written on the sign four blocks away.
		BlockPos switchAt = mouth.relative(across, 2)
			.relative(into.getOpposite(), 1).above();
		if (level.getBlockState(switchAt).isAir()
			&& level.getBlockState(switchAt.relative(into)).isSolid()) {
			level.setBlock(switchAt, Blocks.LEVER.defaultBlockState()
				.setValue(BlockStateProperties.ATTACH_FACE,
					net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, into.getOpposite()), 2);
		}

		// Straw, a bucket, and cobwebs. The same in every one of them, which is
		// what makes fourteen of them read as a system rather than as rooms.
		BlockPos in = mouth.relative(into, 3);
		// A mat, not a bale. Hay is animal feed and looks like it; a cell with
		// a bale in it says somebody was kept the way livestock is kept, which
		// is a different and much sillier claim than the one this makes.
		level.setBlock(in.relative(across, -1),
			Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN)
				.defaultBlockState(), 2);
		level.setBlock(in.relative(across, 1), Blocks.CAULDRON.defaultBlockState(), 2);
		// COBWEBS IN THE THIRTEEN EMPTY ONES AND NOT IN THE FOURTEENTH.
		//
		// Not a look. A cobweb is a movement block: a mob standing in one is held
		// almost still, and the whole point of the locked cell is that the thing
		// inside it comes OUT. Four webs thrown at random into a five block room
		// had a good chance of landing on the one square it stands on, and then the
		// door opens and nothing walks through it.
		//
		// It is also better fiction the other way round. Thirteen rooms nothing has
		// disturbed in a decade, and one with the webs broken.
		if (!shut) {
			for (int i = 0; i < 4; i++) {
				BlockPos web = mouth.relative(into, 1 + random.nextInt(4))
					.relative(across, random.nextInt(3) - 1).above(random.nextInt(3));
				if (level.getBlockState(web).isAir() && random.nextBoolean()) {
					level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
				}
			}
		}

		if (!shut) {
			return;
		}
		// ---- AND WHAT IS STILL IN THERE.
		//
		// It was an INFECTED — a zombie in a box, which is the one thing fourteen
		// cells full of straw did not need. The building's whole claim is that they
		// were locking up people who came back from the wood WRONG, and a zombie
		// resolves that instantly and downward: it is just a zombie, the gaol was
		// for zombies, there is nothing else to think about.
		//
		// The tall one cannot be resolved like that. It is three blocks of pale,
		// silent, villager-shaped nothing standing at the back of a cell that has
		// been locked from the outside since before you were born, and it does not
		// move while you are looking at it. The book on the desk four rooms away
		// says "they say a thing only a dead man could know" and now there is
		// something behind the iron that the sentence could be about.
		//
		// It also makes the lever a decision rather than a formality.
		Mob kept = com.bloomlet.herobrine.entity.ModEntities.GAUNT
			.create(level, EntitySpawnReason.STRUCTURE);
		if (kept == null) {
			return;
		}
		BlockPos stand = mouth.relative(into, 3);
		kept.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
		kept.setPersistenceRequired();
		// AND IT IS TOLD WHERE ITS OWN DOOR IS.
		//
		// The lever on the corridor wall still works and is still the honest way in.
		// This is the other one: walk up to the only shut cell in the building and
		// the bolt goes over on its own. Which needs the creature to know which of
		// the fourteen doors is holding it, and the only moment anybody knows that
		// is right here, while it is being put in the room.
		if (kept instanceof com.bloomlet.herobrine.entity.GauntEntity tall) {
			tall.keptBehind(mouth);
		}
		level.addFreshEntity(kept);
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

		sign(level, far.offset(2, 1, 5),
			new String[] { "COUNT", "THEM IN", "COUNT", "THEM OUT" });
	}

	/** The warder's chest, put down after every tunnel in the place is cut. */
	private static void keeps(ServerLevel level, BlockPos far, RandomSource random) {
		BlockPos chestAt = far.offset(-3, 0, 5);
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			ItemStack book = HouseBooks.theDig();
			if (book != null) {
				chest.setItem(0, book);
			}
			chest.setItem(1, HouseBooks.theGaolAfter());
			// THE THING YOU CAME DOWN HERE FOR.
			//
			// The other two books tell you what happened. This one tells you what to
			// DO, and it is the only place in the mod that does: four tells for the
			// mimic, read off MimicEntity.TheFriend in the order that goal runs
			// them. The gaol sits on Phase.MIMIC, so it arrives exactly when it
			// becomes useful and long before it becomes urgent.
			chest.setItem(2, HouseBooks.theProtocol());
			chest.setItem(3, new ItemStack(Items.IRON_INGOT, 6));
			// LARDER was a dead farmer's pantry, at the bottom of sixteen blocks of
			// stair and thirty-four of hall. A chest speaks one language and that
			// one was saying "you should not have come down".
			Loot.scatter(chest, random, Loot.Tier.GAOL);
		}
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
		// WORKED: wide trunk, ordered spurs, everything squared. The same hand
		// that built fourteen identical cells dug this, and it shows — which is
		// the connection the whole building needs. The gaol is not the end of
		// the story, it is a thing put on top of a hole he was already in.
		BlockPos back = far.offset(0, 1, 7);
		Warren.warn(level, far.offset(2, 1, 6),
			new String[] { "COUNT", "THEM IN", "COUNT", "THEM OUT" });
		Warren.dig(level, back, Warren.Manner.WORKED, random);
	}


	/**
	 * WHAT FOURTEEN PEOPLE SCRATCHED INTO THE BACK WALL.
	 *
	 * The furniture in these rooms is identical on purpose — cell() says so, and
	 * it is right: a mat, a bucket and cobwebs in every one is what makes fourteen
	 * doors read as an institution rather than as fourteen rooms. Sameness is the
	 * horror of the place. An institution processed these people the same way.
	 *
	 * So the difference goes somewhere else. One line of handwriting each, and
	 * nothing else changes. Same room, same straw, same bucket, fourteen separate
	 * people — which is a harder thing to look at than fourteen different rooms.
	 *
	 * They are in the order cells() numbers them, which puts INDEX EIGHT in the
	 * shut cell: the ninth. The one theGaolAfter() says went quiet on a Tuesday
	 * and had blood on the ceiling. What is standing in there now is standing in
	 * front of a man's handwriting saying he was still himself.
	 */
	private static final String[][] SCRATCHED = {
		new String[] { "MARTA", "DAY SIX", "STILL", "MYSELF" },
		new String[] { "IIII IIII", "IIII III", "ELEVEN", "DAYS" },
		new String[] { "I AM NOT", "HIM", "I AM NOT", "HIM" },
		new String[] { "TELL MY", "BROTHER", "I WENT IN", "CLEAN" },
		new String[] { "WHOEVER", "READS THIS", "I WAS", "REAL" },
		new String[] { "DAY ONE", "SCARED", "DAY NINE", "HUNGRY" },
		new String[] { "ASK ME", "SOMETHING", "ONLY I", "WOULD KNOW" },
		new String[] { "HE KNEW", "THE ANSWER", "HOW DID", "HE KNOW" },
		new String[] { "NINE", "I AM", "STILL", "MYSELF" },
		new String[] { "COUNT ME", "OUT", "PLEASE", "COUNT ME" },
		new String[] { "IIII", "I SLEPT", "THAT IS", "GOOD" },
		new String[] { "IF I STOP", "EATING", "DO NOT", "OPEN THIS" },
		new String[] { "JOREN", "WARDER", "SECOND", "MONTH" },
		new String[] { "NOBODY", "HAS COME", "IN FOUR", "DAYS" },
	};

	private static void dressCells(ServerLevel level, BlockPos start, RandomSource random) {
		java.util.List<Cell> spots = cells(start);
		for (int n = 0; n < spots.size(); n++) {
			Cell spot = spots.get(n);
			// The back wall of the cell is at in == 5, so the sign hangs on the face
			// of it at in == 4, at head height, facing back toward the door. Which
			// means you cannot read it from the corridor. You have to go in.
			BlockPos at = spot.mouth().relative(spot.into(), 4).above();
			scratch(level, at, spot.into().getOpposite(),
				SCRATCHED[n % SCRATCHED.length]);
		}
	}

	/**
	 * A wall sign, set over whatever was there.
	 *
	 * Unconditional, unlike sign() — this runs after cell() has thrown cobwebs
	 * around at random, and one of the four it throws can land on exactly this
	 * block. sign()'s air check would silently drop the note, and a missing note
	 * is indistinguishable from a cell nobody wrote in.
	 */
	private static void scratch(ServerLevel level, BlockPos at, Direction faces,
	                            String[] lines) {
		level.setBlock(at, Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, faces), 2);
		if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
			SignText text = new SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, Component.literal(
					row < lines.length ? lines[row] : ""));
			}
			sign.setText(text, true);
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
