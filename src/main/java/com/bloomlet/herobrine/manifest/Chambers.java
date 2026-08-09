package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.structure.Loot;

import net.minecraft.world.entity.Mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Rooms cut into the rock behind your caves.
 *
 * The five houses are a sequence you find once and read in order. These are the
 * opposite: eight kinds of small chamber, scattered, unnumbered, each carrying
 * one fact and no context. A player who finds three of them over a week has
 * assembled something nobody wrote down for them, which is a completely
 * different pleasure from being handed a story in five chapters.
 *
 * THE GRAVITY FALLS IDEA, AND WHAT IT ACTUALLY IS. The bunker in that programme
 * works because of the ORDER things are discovered in: a lever disguised as a
 * branch, then a staircase, then a shelter stocked for ninety years, then a
 * room with windows looking into a cell, then the fact that the cell was opened
 * from the inside. Nothing is explained. Each room silently makes the last one
 * worse. That structure is the thing worth stealing, not the aesthetic.
 *
 * So these are built to be met in any order and to poison each other whichever
 * way round they come. The observation room is unsettling alone and much worse
 * after the cell; the cell is odd alone and much worse after the observation
 * room. Neither is the "right" one to find first.
 *
 * AND THEY ARE HIS, not a dungeon. Every one is a room a person made, with
 * hand tools, in rock, and then left. No mob spawners, no treasure worth the
 * trip, no puzzle to solve. What is in them is furniture and evidence.
 *
 * EVERY ONE HAS A DOOR YOU CAN WALK IN THROUGH, and that took a rewrite. The
 * first version cut the room in solid rock and ran a passage that stopped short
 * of the player's tunnel, so the last two blocks were theirs to dig. That is a
 * better arrival and it is worth nothing, because nobody ever gets there: a
 * room thirty blocks inside bedrock with no opening is not hidden, it is
 * absent. Being told it was "south west, 33 blocks" and having no way to act on
 * that is the whole of the experience it produced.
 *
 * So the corridor now runs all the way through into the cave, and where it
 * breaks in it gets a squared frame with a lintel. That frame is the entire
 * mechanism by which any of this is ever found — a hole in a cave wall is
 * terrain and a player walks past it, and a doorway is not.
 */
public final class Chambers {
	private Chambers() {}

	/** How far from the player one may be cut. Near enough to stumble into. */
	private static final int NEAR = 12;
	private static final int FAR = 28;

	/**
	 * The eight, and each is one sentence.
	 *
	 * Deliberately no ordering and no numbering. The moment these are chapters
	 * the player starts looking for chapter one, and looking for things is a
	 * completely different activity from finding them.
	 */
	private enum Kind {
		/** Stocked for decades by somebody who expected to be here. */
		SHELTER,
		/** A window, and a cell on the other side of it. */
		OBSERVATION,
		/** The cell. The door was forced from the inside. */
		CELL,
		/** Tools, a bench, and something half-made that is not finished. */
		WORKSHOP,
		/** Abandoned mid-swing. The pick is still on the floor. */
		DIG,
		/** Books, a lectern, one candle, and a chair facing the wall. */
		READING,
		/** Four walls of tally marks and nothing else at all. */
		TALLY,
		/** A floor of pale tiles, and the outline of somebody who stood here. */
		TILES,
	}

	/**
	 * Cut one behind the cave the player is in, with a way back into it.
	 *
	 * The room is still in solid rock rather than dropped into an existing
	 * cavern — a chamber opening straight off a cave is scenery — but it is
	 * reached down a corridor that genuinely connects, from a doorway that
	 * genuinely reads as one.
	 */
	public static boolean cut(ServerLevel level, ServerPlayer player) {
		if (level.canSeeSky(player.blockPosition()) || player.getY() > 54) {
			return false;
		}
		RandomSource random = level.getRandom();
		Kind kind = Kind.values()[random.nextInt(Kind.values().length)];

		// START FROM THE CAVE, NOT FROM THE ROOM.
		//
		// The first version chose a spot in solid rock and cut a passage that
		// stopped short of the player's tunnel, on the theory that digging the
		// last two blocks yourself is a better arrival than walking in. It is —
		// and it is worth nothing, because nobody ever gets there. A room
		// thirty blocks inside bedrock with no opening is not hidden, it is
		// absent: the only way to find it is to dig in exactly the right
		// direction for no reason.
		//
		// So it works backwards now. Find somewhere the player could actually
		// walk to, go inward from there, and tunnel back — which guarantees a
		// way in, because the way in is where it started.
		BlockPos mouth = openNear(level, player, random);
		if (mouth == null) {
			return false;
		}

		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			BlockPos at = new BlockPos(
				mouth.getX() + (int)Math.round(Math.cos(angle) * range),
				mouth.getY() + random.nextInt(9) - 4,
				mouth.getZ() + (int)Math.round(Math.sin(angle) * range));

			if (!buried(level, at, 5)) {
				continue;
			}
			room(level, at, random);
			furnish(level, at, kind, random);
			BlockPos joined = bore(level, at, mouth);
			doorway(level, joined, at, random);

			HerobrineMod.LOGGER.info("a {} was cut at [{}, {}, {}], opening at [{}, {}, {}]",
				kind.name().toLowerCase(java.util.Locale.ROOT),
				at.getX(), at.getY(), at.getZ(),
				joined.getX(), joined.getY(), joined.getZ());
			ManifestationDirector.noteLocation(joined);
			return true;
		}
		return false;
	}

	/**
	 * Somewhere the player could stand, in the cave they are already in.
	 *
	 * The anchor for everything else. It has to be real open space with a floor
	 * — not a one-block pocket in the rock — because the whole point is that a
	 * passage arriving here arrives somewhere a person walks.
	 */
	private static @org.jspecify.annotations.Nullable BlockPos openNear(
			ServerLevel level, ServerPlayer player, RandomSource random) {
		BlockPos standing = player.blockPosition();
		if (walkable(level, standing)) {
			return standing;
		}
		for (int attempt = 0; attempt < 60; attempt++) {
			BlockPos at = standing.offset(random.nextInt(17) - 8,
				random.nextInt(9) - 4, random.nextInt(17) - 8);
			if (walkable(level, at)) {
				return at;
			}
		}
		return null;
	}

	private static boolean walkable(ServerLevel level, BlockPos at) {
		return level.getBlockState(at).isAir()
			&& level.getBlockState(at.above()).isAir()
			&& level.getBlockState(at.below()).isSolid();
	}

	/**
	 * Cut from the room back to the cave, and do not stop until it is through.
	 *
	 * Walks the straight line and keeps going past where it thinks it should
	 * end, so an off-by-one or a bit of terrain in the way cannot leave the
	 * corridor sealed one block short — which is the failure this whole rewrite
	 * exists to remove, and it would be invisible from the log.
	 *
	 * @return where it met open air, so the doorway can be framed there
	 */
	private static BlockPos bore(ServerLevel level, BlockPos from, BlockPos to) {
		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double dz = to.getZ() - from.getZ();
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0) {
			return from;
		}
		int steps = (int)Math.ceil(len) + 3;
		// Where it BROKE THROUGH, which is not where it stopped. The walk
		// deliberately overshoots by three so a rounding error cannot leave the
		// corridor sealed one block short — but that means the last position is
		// somewhere past the cave, in whatever is on the far side of it, and
		// framing the doorway there would put an arch in the middle of a wall
		// nobody will ever stand next to.
		//
		// The junction is the first block that was ALREADY air before this cut
		// it: that is the moment the tunnel met the space the player walks in,
		// and it is the only place a doorway means anything.
		BlockPos junction = null;
		BlockPos last = from;

		for (int step = 2; step <= steps; step++) {
			double t = step / len;
			BlockPos at = new BlockPos(
				from.getX() + (int)Math.round(dx * t),
				from.getY() + (int)Math.round(dy * t),
				from.getZ() + (int)Math.round(dz * t));
			last = at;
			if (junction == null && step > 4 && level.getBlockState(at).isAir()
				&& level.getBlockState(at.above()).isAir()) {
				junction = at;
			}
			for (int dyy = 0; dyy <= 1; dyy++) {
				// One wide, two high, and cut a little either side so it does
				// not scrape. Still narrow enough to read as a person's tunnel
				// rather than as a cave.
				level.setBlock(at.above(dyy), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
			if (!level.getBlockState(at.below()).isSolid()) {
				level.setBlock(at.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
			}
		}
		return junction != null ? junction : last;
	}

	/**
	 * A framed opening where the tunnel meets the cave.
	 *
	 * The thing that makes any of this findable. A hole in a cave wall is
	 * terrain and a player walks past it; a squared opening with a lintel and a
	 * course of brick down each side is unmistakably a doorway, from a distance,
	 * in torchlight, while walking. That single detail is the difference
	 * between a room that exists and a room that gets found.
	 *
	 * One torch beside it, and only one. Two would look like a corridor
	 * somebody maintains.
	 */
	private static void doorway(ServerLevel level, BlockPos at, BlockPos towards,
	                            RandomSource random) {
		Direction into = Direction.getApproximateNearest(
			towards.getX() - at.getX(), 0.0, towards.getZ() - at.getZ());
		Direction across = into.getClockWise();

		for (int side = -1; side <= 1; side++) {
			for (int dy = -1; dy <= 2; dy++) {
				BlockPos pos = at.relative(across, side).above(dy);
				boolean frame = Math.abs(side) == 1 || dy == -1 || dy == 2;
				if (frame) {
					level.setBlock(pos, brick(random), 2);
				} else {
					level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
		BlockPos lamp = at.relative(across).above();
		if (level.getBlockState(lamp).isSolid()) {
			BlockPos hang = at.above(1).relative(into.getOpposite());
			if (level.getBlockState(hang).isAir()) {
				level.setBlock(hang, Blocks.TORCH.defaultBlockState(), 2);
			}
		}
	}

	/** Solid all round, so it is genuinely hidden rather than open to a cave. */
	private static boolean buried(ServerLevel level, BlockPos middle, int reach) {
		for (BlockPos at : BlockPos.betweenClosed(
				middle.offset(-reach, -2, -reach), middle.offset(reach, 5, reach))) {
			if (!level.getBlockState(at).isSolid()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The shell: seven by seven, four high, squared off.
	 *
	 * SQUARE, and that is the whole trick. Everything a player meets underground
	 * is irregular, because caves are; a room with corners and a level floor
	 * announces that a person made it before they have looked at anything in it.
	 */
	private static void room(ServerLevel level, BlockPos middle, RandomSource random) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -1; dy <= 4; dy++) {
					BlockPos at = middle.offset(dx, dy, dz);
					boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3
						|| dy == -1 || dy == 4;
					level.setBlock(at, shell ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}
		level.setBlock(middle.offset(0, 3, 0), Blocks.LANTERN.defaultBlockState()
			.setValue(BlockStateProperties.HANGING, true), 2);
	}

	private static void furnish(ServerLevel level, BlockPos m, Kind kind,
	                            RandomSource random) {
		BlockPos f = m.below();   // floor level
		switch (kind) {
			// Stocked for far longer than anybody plans for. The quantity is
			// the story: one barrel is a cupboard, nine is somebody who did not
			// expect to leave.
			case SHELTER -> {
				for (int dx = -2; dx <= 2; dx++) {
					level.setBlock(f.offset(dx, 1, -2), Blocks.BARREL.defaultBlockState(), 2);
					if (dx % 2 == 0) {
						level.setBlock(f.offset(dx, 2, -2), Blocks.BARREL.defaultBlockState(), 2);
					}
				}
				bed(level, f.offset(2, 1, 1), Direction.SOUTH);
				chest(level, f.offset(-2, 1, 2), random, Loot.Tier.LARDER,
					new ItemStack(Items.BREAD, 12));
				sign(level, f.offset(0, 1, -1), new String[] { "ENOUGH", "FOR", "AS LONG AS", "IT TAKES" });
			}
			// A window into a room that is not attached to this one. The player
			// can see a cell and cannot get to it from here, which is worse
			// than being able to.
			case OBSERVATION -> {
				for (int dx = -1; dx <= 1; dx++) {
					level.setBlock(m.offset(dx, 0, 3), Blocks.IRON_BARS.defaultBlockState(), 2);
					level.setBlock(m.offset(dx, 1, 3), Blocks.IRON_BARS.defaultBlockState(), 2);
				}
				// The cell on the far side, sealed, with nothing in it.
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = 4; dz <= 7; dz++) {
						for (int dy = -1; dy <= 3; dy++) {
							boolean shell = Math.abs(dx) == 2 || dz == 7
								|| dy == -1 || dy == 3;
							level.setBlock(m.offset(dx, dy, dz), shell
								? brick(random) : Blocks.CAVE_AIR.defaultBlockState(), 2);
						}
					}
				}
				// A mat on the floor, not a hay bale. A bale is a farm block —
				// it is animal feed, it reads as one, and putting it in a cell
				// says somebody was fed rather than that somebody slept there.
				level.setBlock(f.offset(0, 1, 5), Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN)
					.defaultBlockState(), 2);
				level.setBlock(f.offset(-2, 1, 0), Blocks.LECTERN.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
				sign(level, f.offset(2, 1, 0), new String[] { "DAY 41", "NO CHANGE", "", "DAY 42" });
				chest(level, f.offset(-2, 1, -2), random, Loot.Tier.HOMESTEAD,
					new ItemStack(Items.GLASS_BOTTLE, 3));
				shutIn(level, m.offset(0, 0, 5));
			}
			// And the other half of that, met on its own: a cell whose door
			// went outward. Nothing about it says what came out.
			case CELL -> {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = 0; dy <= 2; dy++) {
						level.setBlock(m.offset(dx, dy, -3),
							Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
				}
				for (int i = 0; i < 9; i++) {
					BlockPos at = m.offset(random.nextInt(5) - 2, random.nextInt(3) - 1,
						-4 - random.nextInt(3));
					level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
				// The bars are on the floor rather than in the frame.
				for (int dx = -1; dx <= 1; dx++) {
					level.setBlock(f.offset(dx, 1, -2), Blocks.IRON_BARS.defaultBlockState(), 2);
				}
				level.setBlock(f.offset(1, 1, 2), Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN)
					.defaultBlockState(), 2);
				scratches(level, m, random);
				chest(level, f.offset(-2, 1, 2), random, Loot.Tier.LARDER,
					worn(Items.IRON_AXE));
			}
			case WORKSHOP -> {
				level.setBlock(f.offset(-2, 1, -2), Blocks.SMITHING_TABLE.defaultBlockState(), 2);
				level.setBlock(f.offset(-1, 1, -2), Blocks.ANVIL.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 2);
				level.setBlock(f.offset(1, 1, -2), Blocks.GRINDSTONE.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
				level.setBlock(f.offset(2, 1, -2), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
				chest(level, f.offset(-2, 1, 2), random, Loot.Tier.HOMESTEAD,
					worn(Items.IRON_PICKAXE));
				sign(level, f.offset(2, 1, 2), new String[] { "IT DOES NOT", "HOLD", "TRY AGAIN", "" });
			}
			// Stopped mid-swing, and the room is the wrong shape because of it.
			case DIG -> {
				for (int dz = 3; dz <= 9; dz++) {
					for (int dx = -1; dx <= 0; dx++) {
						for (int dy = 0; dy <= 1; dy++) {
							level.setBlock(m.offset(dx, dy, dz),
								Blocks.CAVE_AIR.defaultBlockState(), 2);
						}
					}
				}
				level.setBlock(f.offset(0, 1, 8), Blocks.COBBLESTONE.defaultBlockState(), 2);
				level.setBlock(f.offset(-1, 1, 7), Blocks.SOUL_TORCH.defaultBlockState(), 2);
				chest(level, f.offset(2, 1, 0), random, Loot.Tier.HOMESTEAD,
					worn(Items.IRON_SHOVEL));
			}
			case READING -> {
				chest(level, f.offset(1, 1, 2), random, Loot.Tier.HOMESTEAD,
					new ItemStack(Items.BOOK, 4));
				for (int dz = -2; dz <= 2; dz++) {
					for (int dy = 1; dy <= 3; dy++) {
						level.setBlock(f.offset(-2, dy, dz), shelf(random), 2);
						level.setBlock(f.offset(2, dy, dz), shelf(random), 2);
					}
				}
				level.setBlock(f.offset(0, 1, -1), Blocks.LECTERN.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
				// A chair facing the wall, not the books.
				level.setBlock(f.offset(0, 1, 1), Blocks.DARK_OAK_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
				level.setBlock(f.offset(0, 2, -2), Blocks.CANDLE.defaultBlockState()
					.setValue(BlockStateProperties.LIT, true), 2);
			}
			// Nothing in it. That is the entire room.
			case TALLY -> {
				scratches(level, m, random);
				scratches(level, m, random);
				chest(level, f.offset(2, 1, 2), random, Loot.Tier.LARDER,
					new ItemStack(Items.CLOCK));
				sign(level, f.offset(0, 1, -2), new String[] { "", "I STOPPED", "COUNTING", "" });
			}
			// The Gravity Falls tile floor, without the trap. A pale, perfectly
			// regular floor in a rock room, and one square darker than the rest
			// where somebody stood for a very long time.
			case TILES -> {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						level.setBlock(f, Blocks.POLISHED_DIORITE.defaultBlockState(), 2);
						level.setBlock(f.offset(dx, 0, dz),
							(dx + dz) % 2 == 0
								? Blocks.POLISHED_DIORITE.defaultBlockState()
								: Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
					}
				}
				level.setBlock(f.offset(0, 0, 0), Blocks.COAL_BLOCK.defaultBlockState(), 2);
				sign(level, f.offset(0, 1, -2), new String[] { "STAND", "HERE", "", "" });
				chest(level, f.offset(-2, 1, 2), random, Loot.Tier.HOMESTEAD,
					new ItemStack(Items.REDSTONE_TORCH, 4));
				shutIn(level, m.offset(2, 0, 2));
			}
		}
	}

	/**
	 * One of the shut-in ones, and it is a locator as much as a scare.
	 *
	 * The chambers had a findability problem that the doorway only half solved:
	 * a doorway still has to be walked past to be seen. A shut-in makes the
	 * warden's heartbeat every seven seconds, which carries about as far as a
	 * player's own footsteps and through rock, so a chamber with one in it can
	 * be heard before it can be seen — and a player who stops mining because
	 * something down here has a pulse is going to find the room.
	 *
	 * Only in the two rooms that are about containment. Putting one in the
	 * reading room or the workshop would make every chamber a monster closet,
	 * and the whole point of eight kinds is that most of them are furniture.
	 */
	private static void shutIn(ServerLevel level, BlockPos at) {
		Mob kept = com.bloomlet.herobrine.entity.ModEntities.INFECTED
			.create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
		if (kept == null) {
			return;
		}
		kept.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		// Into the world FIRST, then marked. Attachments set on an entity that
		// is not in a level yet have nowhere to be tracked from and never sync.
		level.addFreshEntity(kept);
		Feral.shutIn(kept);
	}

	/** Marks on the wall, in blocks, because nothing else counts down here. */
	private static void scratches(ServerLevel level, BlockPos m, RandomSource random) {
		Direction wall = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		Direction across = wall.getClockWise();
		for (int side = -2; side <= 2; side++) {
			for (int dy = 0; dy <= 2; dy++) {
				if (random.nextInt(3) == 0) {
					continue;
				}
				BlockPos at = m.relative(wall, 2).relative(across, side).above(dy);
				level.setBlock(at, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
			}
		}
	}

	/**
	 * A short cut from the room toward the player's cave, and it stops short.
	 *
	 * It does not connect all the way on purpose. A passage that opens into
	 * their tunnel is a door; one that runs eight blocks and then meets rock is
	 * something they have to break the last of themselves — and having dug the
	 * final two blocks into a finished room is a much better way to arrive.
	 */
	private static void connect(ServerLevel level, BlockPos from, ServerPlayer player,
	                            RandomSource random) {
		double dx = player.getX() - from.getX();
		double dz = player.getZ() - from.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0) {
			return;
		}
		int steps = 4 + random.nextInt(6);
		for (int step = 3; step < 3 + steps; step++) {
			BlockPos at = new BlockPos(
				from.getX() + (int)Math.round(dx / len * step),
				from.getY() + (step > steps / 2 ? random.nextInt(2) : 0),
				from.getZ() + (int)Math.round(dz / len * step));
			for (int dy = 0; dy <= 1; dy++) {
				level.setBlock(at.above(dy), Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
	}

	private static void bed(ServerLevel level, BlockPos at, Direction facing) {
		BlockState bed = Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED)
			.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		level.setBlock(at, bed.setValue(BlockStateProperties.BED_PART,
			net.minecraft.world.level.block.state.properties.BedPart.FOOT), 2);
		level.setBlock(at.relative(facing), bed.setValue(BlockStateProperties.BED_PART,
			net.minecraft.world.level.block.state.properties.BedPart.HEAD), 2);
	}

	private static void chest(ServerLevel level, BlockPos at, RandomSource random,
	                          Loot.Tier tier, ItemStack first) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			chest.setItem(0, first);
			Loot.scatter(chest, random, tier);
		}
	}

	private static ItemStack worn(net.minecraft.world.item.Item item) {
		ItemStack stack = new ItemStack(item);
		if (stack.isDamageableItem()) {
			stack.setDamageValue(stack.getMaxDamage() - 30);
		}
		return stack;
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
			sign.setWaxed(true);
		}
	}

	private static BlockState shelf(RandomSource random) {
		return random.nextInt(6) == 0
			? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
			: Blocks.BOOKSHELF.defaultBlockState();
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
