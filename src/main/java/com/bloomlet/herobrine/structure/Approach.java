package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * HOW YOU FIND THE PLACE, WHICH IS NOW THE WHOLE GAME.
 *
 * A group walked a thousand blocks west, raided villages, read signs, stood on
 * graves, and never found a single building. Everything downstream of that was
 * wasted — six buildings, a town, warrens, a two-hundred-block survey tunnel,
 * all of it sitting in a field nobody visited. That is the largest failure this
 * mod has had, and it is not a balance problem. Nothing in the world pointed
 * anywhere.
 *
 * So the buildings advertise themselves now, at four ranges, and the ranges are
 * the design:
 *
 *   SEVERAL HUNDRED BLOCKS — smoke. A column above the treeline, seen from a
 *     ridge by somebody who was going somewhere else.
 *   ONE HUNDRED — the road. Cross it anywhere along its length and it takes you
 *     in. This is the one that actually works, because it does not require
 *     looking in the right direction: it requires walking, which is all anybody
 *     does.
 *   THE END OF THE ROAD — a sign, so a person who finds the road knows it is a
 *     road and not a game-generated path.
 *   SIXTY — sound. An axe on wood, a bell. Somebody is home.
 *
 * AND ALL FOUR GET WORSE AS THE PHASES CLIMB, which is the part worth caring
 * about. The homestead's approach is a farm track with woodsmoke over it and
 * chopping in the distance — it reads as a place somebody lives, and that is the
 * correct lie for the first building. By the last one the track is a
 * processional in stone with unlit torches down it, the smoke has gone blue, and
 * the sound carrying across the valley is not a tool. Same four systems, and the
 * player is told everything they need to know about how far in they are before
 * they can see a wall.
 */
public final class Approach {
	private Approach() {}

	/** How far the road runs out from the building, and how many ways it leaves. */
	private static final int RUN = 90;
	/**
	 * FIVE, AND THE NUMBER CAME OUT OF ARITHMETIC RATHER THAN TASTE.
	 *
	 * Three was the first guess and it is too thin. Roads leave the building
	 * radially, so what matters is whether somebody walking a straight line
	 * across the area crosses one — and with three spokes a chord through the
	 * outer part of the disc can miss all of them comfortably. Since the entire
	 * purpose of this class is that finding the place must not depend on luck,
	 * "comfortably misses" is a failure and not a near miss.
	 *
	 * Five is also the more honest number for a farmstead: there is a way to the
	 * fields, a way to the water, a way to the woods, a way to the road, and the
	 * way everybody actually uses. Nobody who has looked at an old farm has ever
	 * seen three tidy paths.
	 */
	private static final int SPURS = 5;

	/**
	 * What the approach is made of, which is one per phase.
	 *
	 * The escalation is in materials and in what stands beside the path, never in
	 * how VISIBLE it is. A late building is not harder to find than an early one
	 * — that would punish exactly the players who have got furthest. It is worse
	 * to walk up.
	 */
	private record Manner(BlockState surface, BlockState kerb, BlockState beside,
	                      boolean soulSmoke, SoundEvent sound, String[] words) {}

	private static Manner manner(Phase phase) {
		return switch (phase) {
			// A farm track. Somebody walks this every day, and the sign is theirs
			// rather than his — which is the whole trick of the first building.
			case RUMOUR -> new Manner(
				Blocks.DIRT_PATH.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState(),
				Blocks.OAK_FENCE.defaultBlockState(),
				false, SoundEvents.AXE_STRIP,
				new String[] { "", "home", "not far", "" });
			// A road between real places, because this one has a town on it.
			case WATCHER -> new Manner(
				Blocks.DIRT_PATH.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState(),
				Blocks.OAK_FENCE.defaultBlockState(),
				false, SoundEvents.BELL_BLOCK,
				new String[] { "", "the town", "keep on", "" });
			// Nobody has maintained it in years. The kerb is still there under
			// the gravel, which is what says it USED to be looked after.
			case TRESPASSER -> new Manner(
				Blocks.COARSE_DIRT.defaultBlockState(),
				Blocks.GRAVEL.defaultBlockState(),
				Blocks.OAK_FENCE.defaultBlockState(),
				false, SoundEvents.ANVIL_LAND,
				new String[] { "", "no one", "lives here", "" });
			// Marked rather than built. Somebody walked this often enough to need
			// to know where they were, and put stones up to tell themselves.
			case MIMIC -> new Manner(
				Blocks.COARSE_DIRT.defaultBlockState(),
				Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
				Blocks.COBBLESTONE_WALL.defaultBlockState(),
				true, SoundEvents.CHAIN_BREAK,
				new String[] { "", "they came", "this way", "too" });
			// A processional. Stone, laid properly, with lights that are out.
			case HUNTER -> new Manner(
				Blocks.STONE_BRICKS.defaultBlockState(),
				Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
				Blocks.COBBLESTONE_WALL.defaultBlockState(),
				true, SoundEvents.SOUL_ESCAPE.value(),
				new String[] { "", "nobody", "walks back", "" });
			// And at the end it is not a road any more, it is the way in.
			case SIEGE -> new Manner(
				Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
				Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
				Blocks.COBBLESTONE_WALL.defaultBlockState(),
				true, SoundEvents.SOUL_ESCAPE.value(),
				new String[] { "", "you found it", "", "" });
		};
	}

	/**
	 * Lay everything, once, when the building goes up.
	 *
	 * Deliberately all at build time rather than ticked: a road that appears
	 * while somebody is standing on it would break the oldest rule in the mod,
	 * and the building is already only raised when nobody is close.
	 */
	public static void lay(ServerLevel level, BlockPos site, Phase phase) {
		RandomSource random = level.getRandom();
		Manner manner = manner(phase);
		double turn = random.nextDouble() * Math.PI * 2.0;

		for (int spur = 0; spur < SPURS; spur++) {
			// Spread rather than random, so three roads never leave by almost the
			// same bearing and leave three quarters of the compass with nothing.
			double bearing = turn + spur * (Math.PI * 2.0 / SPURS)
				+ (random.nextDouble() - 0.5) * 0.7;
			road(level, site, bearing, manner, random);
		}
		smoke(level, site, manner, random);
		HerobrineMod.LOGGER.info("an approach to [{}, {}] laid for {}: {} roads out",
			site.getX(), site.getZ(), phase.name().toLowerCase(java.util.Locale.ROOT), SPURS);
	}

	/**
	 * One road, running out until it stops being one.
	 *
	 * IT FADES RATHER THAN ENDING. The last third is laid at falling odds, so it
	 * thins to nothing instead of stopping at a clean line — a road that ends
	 * square in open grass is unmistakably generated, and the whole point of this
	 * is to read as somebody's feet.
	 *
	 * It also follows the ground. No cutting, no bridging: it goes over what is
	 * there, which is what a path is.
	 */
	private static void road(ServerLevel level, BlockPos site, double bearing,
	                         Manner manner, RandomSource random) {
		double wander = 0.0;
		for (int step = 3; step < RUN; step++) {
			// A slow drift, so it curves the way a walked path curves rather than
			// running dead straight like something surveyed.
			wander += (random.nextDouble() - 0.5) * 0.09;
			double angle = bearing + wander;
			int x = site.getX() + (int)Math.round(Math.cos(angle) * step);
			int z = site.getZ() + (int)Math.round(Math.sin(angle) * step);

			// Thinning out over the last third.
			double survives = step < RUN * 2 / 3 ? 1.0
				: 1.0 - (double)(step - RUN * 2 / 3) / (RUN / 3.0);
			if (random.nextDouble() > survives) {
				continue;
			}

			BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				new BlockPos(x, 0, z));
			BlockPos on = top.below();
			if (!level.getBlockState(on).isSolid() || !level.getFluidState(top).isEmpty()) {
				continue;   // water, cliff face, or nothing to lay it on
			}
			// Anything already built here is somebody's, and a road through their
			// floor is vandalism rather than atmosphere.
			if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(level, on)) {
				continue;
			}
			level.setBlock(top, Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(on, random.nextInt(7) == 0 ? manner.kerb() : manner.surface(), 2);

			// A fence post or a wall stub every so often, on one side only. Both
			// sides reads as a corridor; one side reads as a boundary somebody
			// put up and never finished.
			if (step % 11 == 4 && random.nextBoolean()) {
				BlockPos beside = on.offset(
					(int)Math.round(-Math.sin(angle)), 1, (int)Math.round(Math.cos(angle)));
				if (level.getBlockState(beside).canBeReplaced()) {
					level.setBlock(beside, manner.beside(), 2);
				}
			}
			// Unlit torches from HUNTER on: the lights are there and they are out,
			// which says more about the place than any amount of darkness does.
			if (manner.soulSmoke() && step % 17 == 9) {
				BlockPos post = on.above();
				if (level.getBlockState(post).canBeReplaced()) {
					level.setBlock(post, Blocks.REDSTONE_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.LIT, false), 2);
				}
			}
			// And at the far end, a sign, so whoever finds the road knows it is one.
			if (step == RUN - 4) {
				sign(level, on.above(), manner, random);
			}
		}
	}

	private static void sign(ServerLevel level, BlockPos at, Manner manner,
	                         RandomSource random) {
		if (!level.getBlockState(at).canBeReplaced()) {
			return;
		}
		level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 3);
		if (level.getBlockEntity(at) instanceof SignBlockEntity plate) {
			SignText text = new SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, Component.literal(manner.words()[row]));
			}
			plate.setText(text, true);
			plate.setWaxed(true);
		}
	}

	/**
	 * A COLUMN OF SMOKE, WHICH IS THE ONLY THING VISIBLE FROM A RIDGE.
	 *
	 * SIGNAL_FIRE is a blockstate rather than a consequence of a hay bale under
	 * it, so this is one setBlock and it carries ten blocks up. Buried in the
	 * roof so the fire itself is never the thing anybody finds — the smoke is.
	 *
	 * And it turns blue. Soul fire smoke from MIMIC on is the single cheapest
	 * escalation available anywhere in this mod: the same silhouette on the same
	 * horizon, and the colour tells a player who has seen the earlier ones that
	 * this is not another farm.
	 */
	private static void smoke(ServerLevel level, BlockPos site, Manner manner,
	                          RandomSource random) {
		for (int attempt = 0; attempt < 24; attempt++) {
			BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				site.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));
			if (!level.getBlockState(at).canBeReplaced()
				|| !level.getBlockState(at.below()).isSolid()) {
				continue;
			}
			level.setBlock(at, (manner.soulSmoke() ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE)
				.defaultBlockState()
				.setValue(CampfireBlock.LIT, true)
				.setValue(CampfireBlock.SIGNAL_FIRE, true), 3);
			return;
		}
	}

	// ------------------------------------------------------------------
	// Sound
	// ------------------------------------------------------------------

	/** Inside this and somebody is home. Outside it and there is nothing. */
	private static final double EARSHOT = 70.0;

	/**
	 * SOMEBODY IS WORKING, JUST OUT OF SIGHT.
	 *
	 * The last sixty blocks are the ones where a player is closest to walking
	 * past without noticing — near enough that the smoke is behind the trees and
	 * the road may have been crossed without being recognised. A sound fixes it,
	 * and it is the only one of the four that arrives whether or not anybody is
	 * looking anywhere in particular.
	 *
	 * Played AT the building rather than at the player, so it attenuates and
	 * pans correctly and can be walked toward. That is the entire requirement:
	 * a cue that cannot be followed is just a noise.
	 */
	public static void heard(ServerLevel level, BlockPos site, Phase phase) {
		RandomSource random = level.getRandom();
		if (random.nextInt(14) != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			double away = Math.sqrt(site.distSqr(player.blockPosition()));
			if (away > EARSHOT || away < 24.0) {
				continue;   // too far to reach them, or close enough to see it
			}
			level.playSound(null, site, manner(phase).sound(), SoundSource.AMBIENT,
				2.6F, 0.7F + random.nextFloat() * 0.2F);
			return;
		}
	}
}
