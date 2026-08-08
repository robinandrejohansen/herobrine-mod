package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Feral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * What is actually under the house.
 *
 * A farmhouse with a hole in the floor, and rather more under it than a farm
 * needed. A cellar is storage; this is not storage.
 *
 * Kept small on purpose. It is the first of five and each is meant to be less
 * like somewhere a person lived than the last, so a house whose cellar already
 * ran to three chambers would leave the later ones nowhere to go. One chamber,
 * and a passage that gives up.
 *
 * Carved rather than built, and that distinction is the whole brief. Rooms have
 * corners, courses and right angles, and every one of those says somebody was
 * making a place to be. These are hollows: irregular, uneven-ceilinged, wider
 * in some places than in others for no reason, following a line that wanders
 * because whoever cut it was not working to a plan. The moment a player can
 * read a rectangle down here it stops being a dig and becomes a basement.
 *
 * Almost nothing is put in it. One chest, a few props, and long stretches of
 * nothing at all. The emptiness IS the content —
 * the question the whole thing exists to ask is why a family with four names in
 * a ledger needed this, and any answer left lying around makes the question
 * smaller.
 *
 * There is no ore, no rail, no branch pattern and no dead-end alcove anybody
 * would recognise as prospecting. It is not a mine. Nobody was looking for
 * anything down here; they were going somewhere.
 */
public final class Undercroft {
	private Undercroft() {}


	/**
	 * Dig it, starting from the cellar the house already has.
	 *
	 * Deliberately modest. This is the FIRST house and it has to leave room for
	 * the ones after it — a farmhouse whose cellar already runs to three
	 * chambers has nowhere left to escalate to, and the whole point of the five
	 * is that each one is less like somewhere a person lived than the last. So:
	 * one chamber, and then a passage that gives up. Enough to say he was
	 * digging, and nowhere near enough to say what for.
	 *
	 * @param mouth the cellar floor position the descent leaves from
	 */
	public static void dig(ServerLevel level, BlockPos mouth, RandomSource random) {
		// Down and away from the house, winding and tight. A squeeze before it
		// opens out, so the player has to commit before they can see whether
		// there is anything worth committing to.
		BlockPos chamber = Digging.bore(level, mouth, new Vec3(0.15, -0.5, 1.0), 18, 1.5, random);
		Digging.hollow(level, chamber, 3.6, random);

		// EVERY passage is cut before anything is put down. The first version
		// bored one out of the chamber after the chest was already in it and
		// drove straight through it — the books ended up on the floor as items,
		// counting down to despawning.
		BlockPos left = Digging.bore(level, chamber, new Vec3(-1.0, -0.15, 0.35), 9, 1.4, random);
		BlockPos right = Digging.bore(level, chamber, new Vec3(0.4, -0.15, -1.0), 11, 1.4, random);
		BlockPos end = Digging.bore(level, chamber, new Vec3(0.85, -0.45, -0.3), 22, 1.4, random);

		// And two rooms nobody dug.
		//
		// The same trick the threshold uses and the reason it works there: cut
		// rock has no corners, so right angles appearing in it are the loudest
		// thing available underground. These are smaller, cruder and older than
		// the lab's cells, which is the point — the farmhouse cellar is where
		// he tried it first, and the complex under the hill is what he learned
		// to build afterwards.
		cell(level, left, random);
		cell(level, right, random);

		crate(level, chamber.offset(2, 0, 0), HouseBooks.brother(), random);
		Digging.props(level, chamber, 4, random);
		unfinished(level, end, random);

		HerobrineMod.LOGGER.info("undercroft dug, ends at [{}, {}, {}]",
			end.getX(), end.getY(), end.getZ());
	}

	/**
	 * A room with a door that only opens from outside.
	 *
	 * Iron, and that is mechanism rather than decoration: villagers cannot open
	 * iron doors. Nothing down here ever gets out on its own, so every one that
	 * is loose in the passage is loose because a player opened it. The whole
	 * encounter is something they chose, which is the difference between a
	 * scare and an ambush.
	 *
	 * Small — five by five and low. A cell you could stand up in and walk about
	 * reads as somewhere someone was kept; one you can barely turn round in
	 * reads as somewhere something was put.
	 */
	private static void cell(ServerLevel level, BlockPos at, RandomSource random) {
		BlockPos floor = Digging.groundUnder(level, at);
		if (floor == null) {
			return;
		}
		BlockPos corner = floor.offset(-2, 0, -2);

		for (int dx = 0; dx < 5; dx++) {
			for (int dz = 0; dz < 5; dz++) {
				for (int dy = 0; dy <= 4; dy++) {
					boolean shell = dx == 0 || dx == 4 || dz == 0 || dz == 4
						|| dy == 0 || dy == 4;
					BlockPos pos = corner.offset(dx, dy, dz);
					level.setBlock(pos, shell ? brick(random)
						: Blocks.CAVE_AIR.defaultBlockState(), 2);
				}
			}
		}

		// The door, in the middle of one wall, and a barred slot beside it so
		// the player sees what is inside before they decide anything.
		BlockPos door = corner.offset(2, 1, 0);
		BlockState iron = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		level.setBlock(door, iron.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER), 2);
		level.setBlock(door.above(), iron.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.UPPER), 2);
		level.setBlock(corner.offset(1, 2, 0), Blocks.IRON_BARS.defaultBlockState(), 2);
		level.setBlock(corner.offset(3, 2, 0), Blocks.IRON_BARS.defaultBlockState(), 2);

		// Nothing in it but the floor and the marks of a long wait.
		for (int i = 0; i < 5; i++) {
			BlockPos spot = corner.offset(1 + random.nextInt(3), 1, 1 + random.nextInt(3));
			if (random.nextBoolean()) {
				level.setBlock(spot, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}

		Mob kept = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
		if (kept != null) {
			kept.snapTo(corner.getX() + 2.5, corner.getY() + 1, corner.getZ() + 2.5, 0.0F, 0.0F);
			Feral.shutIn(kept);
			level.addFreshEntity(kept);
		}
	}

	/**
	 * The passage that gives up, and why it stops.
	 *
	 * A tunnel that simply ends is a tunnel the player assumes is unfinished
	 * content. A tunnel that ends in a rough face, with the cut stone stopping
	 * mid-course and rubble at the foot of it, is one that stopped because
	 * somebody stopped — which is a completely different sentence and takes
	 * about a dozen blocks to say.
	 */
	private static void unfinished(ServerLevel level, BlockPos end, RandomSource random) {
		Digging.props(level, end, 2, random);
		for (int i = 0; i < 14; i++) {
			BlockPos at = end.offset(random.nextInt(5) - 2, random.nextInt(3) - 1,
				random.nextInt(5) - 2);
			if (!level.getBlockState(at).isAir()) {
				continue;
			}
			BlockPos under = Digging.groundUnder(level, at);
			if (under == null) {
				continue;
			}
			level.setBlock(under, random.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState()
				: Blocks.GRAVEL.defaultBlockState(), 2);
		}
	}

	private static BlockState brick(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static void crate(ServerLevel level, BlockPos near,
	                          @org.jspecify.annotations.Nullable ItemStack book,
	                          RandomSource random) {
		BlockPos floor = Digging.groundUnder(level, near);
		if (floor == null) {
			return;
		}
		BlockPos at = floor.above();
		level.setBlock(at, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			if (book != null) {
				chest.setItem(0, book);
			}
			chest.setItem(1, new ItemStack(Items.IRON_PICKAXE));
			Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
		}
	}

}
