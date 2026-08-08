package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Things that were here before you, which were not here yesterday.
 *
 * NOT his name in block letters. That is the meme version, it announces the
 * mod, and a player who reads "HEROBRINE" on a hillside has been told what is
 * happening rather than left to wonder. These are ruins: a doorway with no
 * house, a cairn with a name worn off, the footprint of a building that burned.
 * The reaction we want is "what is this, and why is it here" — a question, not
 * an answer.
 *
 * MATERIALS DO THE WORK. Everything is mossy, cracked, weathered — the
 * vocabulary of something that has stood for a long time, which is a lie,
 * because it was not there an hour ago. That contradiction is the whole
 * effect. Fresh stone brick would read as construction; mossy cobble reads as
 * history, and history you know is false is far worse than something new.
 *
 * Red is used sparingly and never as a block of colour. Redstone dust on stone
 * reads as a stain rather than as decoration, and a single redstone torch in a
 * dark doorway reads as an eye. Red concrete would look like a build; this
 * looks like something happened here.
 *
 * Everything placed is ordinary and minable, and nothing is placed where the
 * player has built (DESIGN.md §9). He raises ruins at the edge of your world,
 * never in the middle of it.
 */
public final class Ruins {
	private Ruins() {}

	/** Far enough to be "out there", near enough that you will walk into it. */
	private static final int MIN_RANGE = 28;
	private static final int MAX_RANGE = 60;

	public static boolean raise(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();

		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);
			int x = (int)(player.getX() + Math.cos(angle) * range);
			int z = (int)(player.getZ() + Math.sin(angle) * range);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos base = new BlockPos(x, y, z);

			if (!level.isLoaded(base) || isInView(player, base)) {
				continue;
			}
			if (!clearEnough(level, base)) {
				continue;
			}

			String built = switch (random.nextInt(3)) {
				case 0 -> doorway(level, base, random) ? "doorway" : null;
				case 1 -> cairn(level, base, random, player) ? "cairn" : null;
				default -> foundation(level, base, random) ? "foundation" : null;
			};
			if (built == null) {
				continue;
			}
			ManifestationDirector.noteLocation(base);
			HerobrineMod.LOGGER.info("ruin ({}) raised at [{}, {}, {}]",
				built, base.getX(), base.getY(), base.getZ());
			return true;
		}
		return false;
	}

	/**
	 * A doorway standing on its own, with nothing behind it.
	 *
	 * The best of the three, because a door implies a building and there is no
	 * building. A wall is rubble; a doorway is a room that has gone missing.
	 * The redstone torch inside it sits at head height and is the only light
	 * for thirty blocks.
	 */
	private static boolean doorway(ServerLevel level, BlockPos base, RandomSource random) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		Direction across = facing.getClockWise();

		for (int side = -1; side <= 1; side += 2) {
			BlockPos post = base.relative(across, side);
			for (int h = 0; h < 3; h++) {
				level.setBlockAndUpdate(post.above(h), weathered(random));
			}
		}
		// Lintel, deliberately incomplete — a whole one looks maintained.
		for (int side = -1; side <= 1; side++) {
			if (side == 0 && random.nextBoolean()) {
				continue;
			}
			level.setBlockAndUpdate(base.relative(across, side).above(3), weathered(random));
		}

		// The eye in the dark. A WALL torch on the inside of a post — a
		// standing torch here has nothing under it, because base is the first
		// air block above the terrain, and it floated.
		BlockPos post = base.relative(across, -1).above(1);
		BlockPos socket = base.above(1);
		if (level.getBlockState(socket).isAir()
			&& level.getBlockState(post).isSolid()) {
			level.setBlockAndUpdate(socket, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, across));
		}
		stain(level, base, random, 3);
		scatter(level, base, random);
		return true;
	}

	/**
	 * A cairn with a sign, and the name worn off.
	 *
	 * Per LORE.md the name has been scratched out so long that the scratching
	 * is the only legible part, so the sign reads as damage rather than as a
	 * message. It is the only ruin that says anything at all, and it does not
	 * say much.
	 */
	private static boolean cairn(ServerLevel level, BlockPos base, RandomSource random,
	                             ServerPlayer player) {
		for (int h = 0; h < 2; h++) {
			level.setBlockAndUpdate(base.above(h), weathered(random));
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (random.nextInt(3) == 0) {
				level.setBlockAndUpdate(base.relative(dir), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			}
		}

		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos signPos = base.above(1).relative(facing);
		if (level.getBlockState(signPos).isAir()) {
			level.setBlockAndUpdate(signPos, Blocks.OAK_WALL_SIGN.defaultBlockState()
				.setValue(WallSignBlock.FACING, facing));
			if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
				String[] lines = SignLines.grave(
					com.bloomlet.herobrine.wrath.Wrath.phase(level.getServer()),
					player, someoneElse(level, player), random);
				sign.setAttached(Signs.HIS, true);
				sign.updateText(text -> {
					net.minecraft.world.level.block.entity.SignText updated = text;
					// Offset by one so short markers sit centred on the board
					// rather than jammed against the top edge.
					int offset = lines.length < 3 ? 1 : 0;
					for (int i = 0; i < lines.length && i + offset < 4; i++) {
						updated = updated.setMessage(i + offset, Component.literal(lines[i]));
					}
					return updated;
				}, true);
			}
		}
		stain(level, base, random, 4);
		scatter(level, base, random);
		return true;
	}

	/**
	 * The outline of a house that is no longer there.
	 *
	 * Only the footprint, one block high, broken in places. A player who walks
	 * into it works out what it is a second after standing in the middle of
	 * it, which is the right order.
	 */
	private static boolean foundation(ServerLevel level, BlockPos base, RandomSource random) {
		int w = 5 + random.nextInt(3);
		int d = 4 + random.nextInt(3);

		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
				if (!edge || random.nextInt(5) == 0) {
					continue;   // gaps: a complete outline looks built, not left
				}
				BlockPos pos = base.offset(dx - w / 2, 0, dz - d / 2);
				int surface = level.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
				level.setBlockAndUpdate(new BlockPos(pos.getX(), surface, pos.getZ()),
					weathered(random));
			}
		}
		// A hearth: the one part of a burned house that survives.
		level.setBlockAndUpdate(base, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
		stain(level, base, random, 5);
		scatter(level, base, random);
		return true;
	}

	/**
	 * Another player in this world, if there is one.
	 *
	 * A grave bearing a friend's name is worse than one bearing your own,
	 * because you cannot tell whether it is a threat or a report.
	 */
	private static String someoneElse(ServerLevel level, ServerPlayer player) {
		List<ServerPlayer> others = new ArrayList<>();
		for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
			if (!candidate.getUUID().equals(player.getUUID())) {
				others.add(candidate);
			}
		}
		if (others.isEmpty()) {
			return null;
		}
		return others.get(level.getRandom().nextInt(others.size())).getName().getString();
	}

	/** Mossy, cracked, old. Never fresh — fresh reads as construction. */
	private static BlockState weathered(RandomSource random) {
		return switch (random.nextInt(5)) {
			case 0 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			case 1 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			case 2 -> Blocks.COBBLESTONE.defaultBlockState();
			case 3 -> Blocks.INFESTED_MOSSY_STONE_BRICKS.defaultBlockState();
			default -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		};
	}

	/**
	 * Redstone dust on the ground. It reads as a stain, not as a block of
	 * colour — red concrete would look like somebody decorated.
	 */
	private static void stain(ServerLevel level, BlockPos base, RandomSource random, int count) {
		for (int i = 0; i < count; i++) {
			BlockPos pos = base.offset(
				random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			int surface = level.getHeight(
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
			BlockPos at = new BlockPos(pos.getX(), surface, pos.getZ());
			if (level.getBlockState(at).isAir()
				&& level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
				level.setBlockAndUpdate(at, Blocks.REDSTONE_WIRE.defaultBlockState());
			}
		}
	}

	/**
	 * Cobwebs and dead bushes. Cheap, and they say "nobody has been here".
	 *
	 * Both need something holding them up, for different reasons. A dead bush
	 * requires soil beneath it or it pops off the moment the block updates.
	 * A cobweb is legal in mid-air in vanilla, but one hanging in open space
	 * reads as a glitch rather than as neglect — webs belong in corners, so
	 * this only places them touching the structure.
	 */
	private static void scatter(ServerLevel level, BlockPos base, RandomSource random) {
		for (int i = 0; i < 6; i++) {
			BlockPos pos = base.offset(random.nextInt(5) - 2, random.nextInt(3), random.nextInt(5) - 2);
			if (!level.getBlockState(pos).isAir()) {
				continue;
			}

			boolean grounded = level.getBlockState(pos.below())
				.isFaceSturdy(level, pos.below(), Direction.UP);

			if (random.nextBoolean()) {
				if (touchesSomething(level, pos)) {
					level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
				}
			} else if (grounded && level.getBlockState(pos.below()).isSolid()) {
				BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
				// Ask the block itself whether it can live here rather than
				// guessing at the soil list, which differs by version.
				if (bush.canSurvive(level, pos)) {
					level.setBlockAndUpdate(pos, bush);
				}
			}
		}
	}

	/** At least one solid neighbour, so a web has a corner to sit in. */
	private static boolean touchesSomething(ServerLevel level, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (level.getBlockState(pos.relative(dir)).isSolid()) {
				return true;
			}
		}
		return false;
	}

	/** Nothing of the player's, and reasonably flat. */
	private static boolean clearEnough(ServerLevel level, BlockPos base) {
		int floor = base.getY();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				BlockPos pos = base.offset(dx, 0, dz);
				int surface = level.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
				if (Math.abs(surface - floor) > 2) {
					return false;   // too steep; it would float or bury
				}
				// Never on top of anything the player made. Ruins go at the
				// edge of your world, never in the middle of it.
				if (DwellTracker.isBuilt(level, new BlockPos(pos.getX(), surface - 1, pos.getZ()))) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isInView(ServerPlayer player, BlockPos pos) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() + 0.5 - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) > 0.2;
	}
}
