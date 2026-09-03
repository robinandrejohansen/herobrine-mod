package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * The villages get worse.
 *
 * The most lore-consistent thing in the whole mod, and it took far too long to
 * see. The lab under the threshold keeps a numbered register of VILLAGERS, BY
 * TRADE — a fletcher, a farmer, a cleric who would not stop talking. Villages
 * quietly going wrong is not a new idea bolted on; it is the story the books
 * already tell, arriving somewhere the player was going to walk into anyway.
 *
 * Done at runtime rather than at worldgen, and that is not a compromise. A
 * village generated ruined is ruined on the first day, and the entire first act
 * depends on the world starting ordinary and going wrong while the player
 * watches. Worldgen runs once, before anything has happened; only this can
 * escalate.
 *
 * NOTHING HERE IS DESTRUCTIVE, and the rule is worth stating plainly because
 * every obvious idea breaks it. Removing villagers would destroy hours of a
 * player's trading with no warning and no counter-play. Putting the lamps out
 * would spawn hostiles and get the villagers killed, which is the same thing
 * with extra steps. Breaking doors does it faster. So: boarded windows, graves
 * at the treeline, cobwebs, moss. The PLACE decays and the people are left
 * entirely alone.
 *
 * Which is also the better scare. A village where the windows are boarded from
 * outside and there are more graves each time you come back is far worse than
 * an empty one, because somebody is still living there.
 */
public final class Villages {
	private Villages() {}

	/**
	 * Which villages have been touched, and how far.
	 *
	 * Keyed by the structure's own chunk so it survives reloads and a village
	 * is never worked over twice for the same phase. Stored as a string map
	 * because that is what a codec will take without ceremony.
	 */
	public static final AttachmentType<Map<String, Integer>> TOUCHED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("villages_touched"),
			Codec.unboundedMap(Codec.STRING, Codec.INT));

	/** How far around the player a pass reaches. Kept inside loaded chunks. */
	private static final int REACH = 40;
	private static final int CHECK_INTERVAL = 100;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Villages::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled || !com.bloomlet.herobrine.Config.get().villageDecay) {
			return;
		}
		Phase phase = Wrath.phase(server);
		if (!phase.atLeast(Phase.TRESPASSER)) {
			return;   // the world is still ordinary
		}

		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				StructureStart village = level.structureManager()
					.getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE);
				if (village == null || !village.isValid()) {
					continue;
				}
				visit(level, player, village, phase);
			}
		}
	}

	/**
	 * One pass over the part of the village the player is standing in.
	 *
	 * Deliberately only what is near them, rather than the whole structure. A
	 * village spans chunks that may not be loaded, and reaching into those to
	 * age a building nobody is looking at is both expensive and pointless. It
	 * also decays PATCHILY as a result, which is what a real derelict place
	 * looks like — one street gone and the next one fine.
	 */
	private static void visit(ServerLevel level, ServerPlayer player,
	                          StructureStart village, Phase phase) {
		String key = village.getChunkPos().toString();
		Map<String, Integer> touched = level.getServer().overworld()
			.getAttachedOrElse(TOUCHED, Map.of());
		int already = touched.getOrDefault(key, -1);
		if (already >= phase.ordinal()) {
			return;
		}

		BoundingBox bounds = village.getBoundingBox();
		RandomSource random = level.getRandom();
		int severity = phase.ordinal() - Phase.TRESPASSER.ordinal() + 1;

		int boarded = board(level, player, village, random, severity * 3);
		int dug = graves(level, player, bounds, random, severity, phase);
		age(level, player, bounds, random, severity * 6);

		Map<String, Integer> updated = new HashMap<>(touched);
		updated.put(key, phase.ordinal());
		level.getServer().overworld().setAttached(TOUCHED, updated);

		HerobrineMod.LOGGER.info("village at {} worked over at {}: {} boarded, {} graves",
			key, phase.name(), boarded, dug);
	}

	/**
	 * Windows boarded over, from the outside.
	 *
	 * The single most legible thing that can be done to a building without
	 * damaging it, and it costs the player nothing — a boarded window is
	 * strictly safer than a glass one. All the meaning is in the direction:
	 * nobody boards their own windows from outside.
	 */
	private static int board(ServerLevel level, ServerPlayer player, StructureStart village,
	                         RandomSource random, int wanted) {
		List<BlockPos> panes = new ArrayList<>();
		BlockPos at = player.blockPosition();
		BoundingBox reach = new BoundingBox(at.getX() - REACH, at.getY() - 8, at.getZ() - REACH,
			at.getX() + REACH, at.getY() + 12, at.getZ() + REACH);

		// THE PIECES, NOT THE BOX. Eighty-one by twenty-one by eighty-one is 138,000
		// positions, each asking isLoaded and then getBlockState — a stall of
		// hundreds of milliseconds for everyone in the village the moment one player
		// walked in. A village's box is mostly street and air; its glass is in its
		// buildings, and the buildings are the pieces. Same panes, a tenth of the
		// reads, and the loaded test is per chunk rather than per block.
		for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : village.getPieces()) {
			BoundingBox box = piece.getBoundingBox();
			if (!box.intersects(reach)) {
				continue;
			}
			int x0 = Math.max(box.minX(), reach.minX()), x1 = Math.min(box.maxX(), reach.maxX());
			int y0 = Math.max(box.minY(), reach.minY()), y1 = Math.min(box.maxY(), reach.maxY());
			int z0 = Math.max(box.minZ(), reach.minZ()), z1 = Math.min(box.maxZ(), reach.maxZ());
			for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
				for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
					if (!level.hasChunk(cx, cz)) {
						continue;
					}
					for (BlockPos pos : BlockPos.betweenClosed(
							Math.max(x0, cx << 4), y0, Math.max(z0, cz << 4),
							Math.min(x1, (cx << 4) + 15), y1, Math.min(z1, (cz << 4) + 15))) {
						BlockState state = level.getBlockState(pos);
						if (state.is(Blocks.GLASS_PANE) || state.is(Blocks.GLASS)) {
							panes.add(pos.immutable());
						}
					}
				}
			}
		}
		java.util.Collections.shuffle(panes, new java.util.Random(random.nextLong()));

		int done = 0;
		for (BlockPos pos : panes) {
			if (done >= wanted) {
				break;
			}
			level.setBlock(pos, Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), 2);
			done++;
		}
		return done;
	}

	/**
	 * Graves, outside, where the fields are.
	 *
	 * Purely additive — a patch of podzol and a marker on ground nobody was
	 * using. They are the only thing here that says what happened rather than
	 * that time passed, and there are more of them every time the player comes
	 * back, which is a sentence nobody has to write down.
	 *
	 * Placed only under open sky, so they never turn up on somebody's floor.
	 */
	/**
	 * What is actually IN a grave.
	 *
	 * A marker on its own is a prop, and a village of props stops being read
	 * after the third one. Something buried under it is a decision: dig it up
	 * or leave it, and either answer says something about the player.
	 *
	 * DELIBERATELY POOR. What is down there is what somebody was carrying when
	 * they were put there — a worn tool, some bread going over, the odd coin of
	 * an iron ingot. Nothing that rewards grave-robbing on its own terms,
	 * because the moment these are worth opening they stop being graves and
	 * become a loot table with a story painted on.
	 *
	 * And some of them are occupied. Not many, and never before TRESPASSER: a
	 * skeleton climbing out of the first grave anybody finds is a jump scare in
	 * a phase built entirely on deniability. Later, when nothing is deniable
	 * any more, it is exactly right.
	 */
	private static void furnish(ServerLevel level, BlockPos marker, RandomSource random,
	                            Phase phase) {
		BlockPos hollow = marker.below(2);
		if (!level.getBlockState(hollow).isSolid()) {
			return;   // a cave or water under it; leave the ground alone
		}

		// One in three has anything in it at all. Opening two empty ones first
		// is what makes the third worth remembering.
		if (random.nextInt(3) != 0) {
			return;
		}

		level.setBlock(hollow, Blocks.CHEST.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING,
				net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
		level.setBlock(marker.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
		if (level.getBlockEntity(hollow) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			com.bloomlet.herobrine.structure.Loot.scatter(chest, random,
				com.bloomlet.herobrine.structure.Loot.Tier.LARDER);
			if (random.nextInt(4) == 0) {
				chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE, 2 + random.nextInt(4)));
			}
		}

		if (!phase.atLeast(Phase.TRESPASSER) || random.nextInt(4) != 0) {
			return;
		}
		// Somebody still in it. Shut in with the lid, so it is found rather
		// than met — it cannot path out through two blocks of earth, and the
		// player is the one who opens the ground.
		net.minecraft.world.entity.Mob buried = net.minecraft.world.entity.EntityTypes.SKELETON
			.create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
		if (buried == null) {
			return;
		}
		buried.snapTo(hollow.getX() + 0.5, hollow.getY(), hollow.getZ() + 0.5,
			random.nextFloat() * 360.0F, 0.0F);
		buried.setPersistenceRequired();
		level.addFreshEntity(buried);
	}

	private static int graves(ServerLevel level, ServerPlayer player, BoundingBox bounds,
	                          RandomSource random, int wanted, Phase phase) {
		int done = 0;
		for (int attempt = 0; attempt < 120 && done < wanted; attempt++) {
			BlockPos at = player.blockPosition().offset(
				random.nextInt(REACH * 2) - REACH, 0, random.nextInt(REACH * 2) - REACH);
			BlockPos ground = level.getHeightmapPos(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);

			if (!bounds.isInside(ground) || !level.isLoaded(ground)) {
				continue;
			}
			if (!level.canSeeSky(ground) || !level.getBlockState(ground).isAir()) {
				continue;
			}
			BlockState under = level.getBlockState(ground.below());
			if (!under.is(Blocks.GRASS_BLOCK) && !under.is(Blocks.DIRT)
				&& !under.is(Blocks.COARSE_DIRT)) {
				continue;   // not on anybody's path, floor or crops
			}

			level.setBlock(ground.below(), Blocks.PODZOL.defaultBlockState(), 2);
			level.setBlock(ground, Blocks.SPRUCE_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 2);
			if (level.getBlockEntity(ground) instanceof SignBlockEntity sign) {
				String[] lines = SignLines.grave(phase, player, null, random);
				SignText text = sign.getFrontText();
				for (int i = 0; i < lines.length && i < 4; i++) {
					text = text.setMessage(i, Component.literal(lines[i]));
				}
				sign.setText(text, true);
				sign.setWaxed(true);
			}
			furnish(level, ground, random, phase);
			done++;
		}
		return done;
	}

	/**
	 * Time passing, and nothing more than that.
	 *
	 * Moss on stone, a cobweb in a corner. No structural change at all — not a
	 * plank moved, not a door touched, not a light removed. Everything here
	 * could be undone by a player with a shovel in a minute, which is exactly
	 * the level of harm a place somebody else lives in is allowed to take.
	 */
	private static void age(ServerLevel level, ServerPlayer player, BoundingBox bounds,
	                        RandomSource random, int passes) {
		BlockPos at = player.blockPosition();
		for (int i = 0; i < passes * 8; i++) {
			BlockPos pos = at.offset(random.nextInt(REACH * 2) - REACH,
				random.nextInt(14) - 6, random.nextInt(REACH * 2) - REACH);
			if (!bounds.isInside(pos) || !level.isLoaded(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);

			if (state.is(Blocks.COBBLESTONE)) {
				level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
			} else if (state.is(Blocks.STONE_BRICKS)) {
				level.setBlock(pos, random.nextBoolean()
					? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
					: Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 2);
			} else if (state.isAir() && random.nextInt(6) == 0
				&& !level.getBlockState(pos.above()).isAir()
				&& !level.canSeeSky(pos)) {
				level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 2);
			}
		}
	}
}
