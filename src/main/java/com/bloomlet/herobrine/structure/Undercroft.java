package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
		crate(level, chamber.offset(2, 0, 0), HouseBooks.brother(), random);
		Digging.props(level, chamber, 4, random);

		// And on, deeper, until it simply stops. No wall, no door, no chamber —
		// the pick marks end mid-stone. Whatever he was going towards, he did
		// not reach it from this end, and nothing down here says what it was.
		BlockPos end = Digging.bore(level, chamber, new Vec3(0.85, -0.45, -0.3), 22, 1.4, random);
		Digging.props(level, end, 2, random);

		HerobrineMod.LOGGER.info("undercroft dug, ends at [{}, {}, {}]",
			end.getX(), end.getY(), end.getZ());
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
