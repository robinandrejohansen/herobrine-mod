package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * HOUSE THREE. It has stopped being a house.
 *
 * There is no building. There is a hole in a hillside, and some way in there is
 * a bed on the bare stone with a lamp beside it, and from that hollow four
 * tunnels go off and none of them arrive anywhere.
 *
 * THE PROGRESSION IS THE POINT AND IT IS ARCHITECTURAL. One was a home with the
 * furniture still in it. Two was the same home with the windows filled in.
 * Three has no walls, no door, no floor and no roof — it has a bed, because a
 * person still has to sleep, and nothing else, because he has stopped wanting
 * anything else. A player who has seen the first two reads that instantly and
 * without a word of explanation.
 *
 * The tunnels are the other half. They are long, they wander, they are cut for
 * one person, and every single one of them simply stops — no chamber, no ore,
 * no reason. Nobody was prospecting. He was digging because digging was the
 * last thing he was still doing on purpose, which is what the book beside the
 * bed says and is much worse when you have already walked one of them to the
 * end before reading it.
 *
 * Deliberately hard to read as content. There is one chest. Everything else in
 * here is absence, and the absence had to be BUILT — a hundred blocks of empty
 * tunnel is a deliberate object, and it costs the same as a room full of loot.
 */
public final class TheDig {
	private TheDig() {}

	public static void build(ServerLevel level, BlockPos surface, RandomSource random) {
		// The mouth: a scar in the hillside, not a doorway.
		BlockPos mouth = surface.below(1);
		BlockPos hollow = Digging.bore(level, mouth, new Vec3(0.3, -0.55, 1.0), 22, 1.6, random);
		Digging.hollow(level, hollow, 4.2, random);

		// EVERY tunnel is cut before anything is put down. The homestead taught
		// this the hard way: boring a passage after the chest was placed drove
		// straight through it and left the books on the floor as items counting
		// down to despawning.
		BlockPos[] ends = {
			Digging.bore(level, hollow, new Vec3(-1.0, -0.2, 0.4), 30, 1.4, random),
			Digging.bore(level, hollow, new Vec3(0.5, -0.35, -1.0), 26, 1.4, random),
			Digging.bore(level, hollow, new Vec3(1.0, -0.1, 0.3), 34, 1.4, random),
			Digging.bore(level, hollow, new Vec3(-0.4, -0.5, -0.8), 21, 1.4, random),
		};

		sleeping(level, hollow, random);
		for (BlockPos end : ends) {
			givingUp(level, end, random);
		}

		HerobrineMod.LOGGER.info("the dig opened at [{}, {}, {}], four tunnels",
			hollow.getX(), hollow.getY(), hollow.getZ());
	}

	/**
	 * A bed on the rock, a lamp, and one chest.
	 *
	 * The bed is the only comfortable thing in a hundred blocks and it has not
	 * been given a room, a floor or a wall to stand against. That contrast is
	 * the whole of it: somebody is sleeping here, and somebody has decided that
	 * sleeping is all a place needs to provide.
	 */
	private static void sleeping(ServerLevel level, BlockPos hollow, RandomSource random) {
		BlockPos floor = Digging.groundUnder(level, hollow);
		if (floor == null) {
			return;
		}
		BlockPos at = floor.above();

		BlockState bed = Blocks.BED.pick(DyeColor.RED).defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		level.setBlock(at, bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2);
		level.setBlock(at.east(), bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), 2);

		level.setBlock(at.north(), Blocks.LANTERN.defaultBlockState(), 2);

		BlockPos chestAt = at.south(2);
		BlockPos under = Digging.groundUnder(level, chestAt);
		if (under != null) {
			chestAt = under.above();
		}
		level.setBlock(chestAt, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
		if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
			ItemStack book = HouseBooks.theDig();
			if (book != null) {
				chest.setItem(0, book);
			}
			// Worn out, not broken. He was still using it this morning.
			ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
			pick.setDamageValue(pick.getMaxDamage() - 14);
			chest.setItem(1, pick);
			Loot.scatter(chest, random, Loot.Tier.HOMESTEAD);
		}

		Digging.props(level, hollow, 5, random);
	}

	/**
	 * How a tunnel stops when nobody decided to stop.
	 *
	 * Not a wall and not a chamber. The cut simply gets rougher, some rubble
	 * accumulates at the foot of it, and there is nothing there. A tunnel that
	 * ends in a room is a tunnel that was going somewhere; a tunnel that ends
	 * mid-swing is a person who put the pick down one day and did not pick it
	 * back up, and that is a completely different sentence for the cost of
	 * about a dozen blocks.
	 */
	private static void givingUp(ServerLevel level, BlockPos end, RandomSource random) {
		for (int i = 0; i < 12; i++) {
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
		// One torch, burnt out, at the very end of each. He got this far in the
		// dark and then stopped bothering with the light too.
		BlockPos snuffed = Digging.groundUnder(level, end);
		if (snuffed != null && level.getBlockState(snuffed.above()).isAir()) {
			level.setBlock(snuffed.above(), Blocks.SOUL_TORCH.defaultBlockState(), 2);
		}
	}
}
