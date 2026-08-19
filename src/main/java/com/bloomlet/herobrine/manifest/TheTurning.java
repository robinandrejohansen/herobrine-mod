package com.bloomlet.herobrine.manifest;

import java.util.HashMap;
import java.util.Map;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.entity.TurnedEntity;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * One of them has gone wrong.
 *
 * The village is the only place in this mod with living people in it, and until
 * now the only thing that ever happened to it was the buildings getting worse.
 * Boarded windows and graves at the treeline are a good scare and they are a
 * scare about a PLACE. This is the one about a person, and it is the reason the
 * decay was worth building: a village that has been quietly going wrong for two
 * chapters is a village where the player is already looking at the residents.
 *
 * FROM WATCHER, AND FOR THE REST OF THE GAME. It is not a phase's set piece and
 * it does not escalate into anything — it is simply a thing that can happen any
 * night you sleep near people, which is exactly what makes a village stop being
 * a safe place to stop. The odds climb with the phases; the event never changes.
 *
 * ---
 *
 * NOBODY'S VILLAGER IS TAKEN. The obvious implementation is to pick a resident
 * and turn him, and it is ruled out twice over: DESIGN §9 and the whole of
 * Villages refuse to remove villagers, because deleting somebody's cleric to
 * stage a fright costs them hours of trading with no warning and no
 * counter-play — and there is no way to give him back afterwards.
 *
 * So one more person is added instead. From inside the game the two are
 * indistinguishable, because nobody counts villagers; from the player's side
 * the difference is that their librarian is exactly where they left him. It also
 * means this can happen a dozen times over a campaign without a village ever
 * being emptied by it, which the destructive version could not.
 *
 * He is placed OUT OF SIGHT, like everything else here. Nothing in this mod is
 * ever watched arriving.
 */
public final class TheTurning {
	private TheTurning() {}

	/**
	 * Which villages have already produced one, and on which day.
	 *
	 * Stored as the day number rather than a flag so a village can produce
	 * another one later without being able to produce two in a night. Keyed on
	 * the structure's chunk, the same way Villages tracks its decay, so it
	 * survives a restart and cannot be reset by walking away.
	 */
	private static final AttachmentType<Map<String, Integer>> LAST =
		AttachmentRegistry.createPersistent(HerobrineMod.id("turned_last"),
			Codec.unboundedMap(Codec.STRING, Codec.INT));

	/** Every twenty seconds. A night is ten minutes; this is not a fast check. */
	private static final int CHECK_INTERVAL = 400;

	/**
	 * How likely it is on a night the village is eligible, as one in N.
	 *
	 * It climbs, and it never reaches certainty. A village that ALWAYS has one
	 * is a village with a mechanic in it, and the player would clear it on
	 * arrival like a chest. At one in six a group that sleeps in villages meets
	 * him a few times over a campaign and never knows which night it will be,
	 * which is the only version of this that stays frightening after the first
	 * time.
	 */
	private static int chanceIn(Phase phase) {
		return switch (phase) {
			case RUMOUR -> 0;          // never; the world is still ordinary
			case WATCHER -> 6;
			case TRESPASSER -> 5;
			case MIMIC -> 4;
			case HUNTER -> 3;
			case SIEGE -> 2;
		};
	}

	/**
	 * How many can be alive at once, anywhere.
	 *
	 * Three. Not one, because two players in two villages on a server should
	 * each be able to have their own — this is not him, and the "there is only
	 * ever one" discipline that governs the sighting does not apply to a
	 * villager. Not unbounded, because a group that never kills them would
	 * accumulate a standing army of the things over a long campaign, and the
	 * fortieth is a joke.
	 */
	private static final int AT_ONCE = 3;

	/** How far from the player he is put. Across the square, not next to them. */
	private static final int NEAR = 12;
	private static final int FAR = 30;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TheTurning::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!Config.get().enabled || !Config.get().theTurning) {
			return;
		}
		Phase phase = Wrath.phase(server);
		if (chanceIn(phase) == 0) {
			return;
		}

		for (ServerLevel level : server.getAllLevels()) {
			// AFTER DARK, AND ONLY AFTER DARK. He is a thing you find because
			// you stayed the night, and a village you passed through at noon
			// has to be a village you passed through at noon.
			if (level.isBrightOutside()) {
				continue;
			}
			if (alive(level) >= AT_ONCE) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				StructureStart village = level.structureManager()
					.getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE);
				if (village == null || !village.isValid()) {
					continue;
				}
				if (consider(level, player, village, phase)) {
					return;   // one a tick at the very most
				}
			}
		}
	}

	private static boolean consider(ServerLevel level, ServerPlayer player,
	                                StructureStart village, Phase phase) {
		String key = village.getChunkPos().toString();
		Map<String, Integer> last = level.getServer().overworld()
			.getAttachedOrElse(LAST, Map.of());
		// The world clock rather than the game time, so a "day" here starts
		// where the game's day starts. Counting from the raw tick count would
		// put the boundary at an arbitrary hour, and a village could then have
		// two of these in one night simply because midnight fell in the middle
		// of it.
		int today = (int)(level.getOverworldClockTime() / 24000L);
		if (last.getOrDefault(key, -1) >= today) {
			return false;      // this village has had its night
		}

		RandomSource random = level.getRandom();
		// The roll is spent whether or not it succeeds, and the day is written
		// down either way. Without that, a player standing in a village all
		// night gets a check every twenty seconds and it becomes a certainty
		// after twenty minutes — the odds above would be describing something
		// that does not happen.
		Map<String, Integer> updated = new HashMap<>(last);
		updated.put(key, today);
		level.getServer().overworld().setAttached(LAST, updated);

		if (random.nextInt(chanceIn(phase)) != 0) {
			HerobrineMod.LOGGER.debug("village {} kept its night", key);
			return false;
		}

		BlockPos spot = somewhere(level, player, village.getBoundingBox(), random);
		if (spot == null) {
			HerobrineMod.LOGGER.info("village {}: nowhere out of sight to put him", key);
			return false;
		}
		TurnedEntity him = ModEntities.TURNED.create(level, EntitySpawnReason.EVENT);
		if (him == null) {
			return false;
		}
		him.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
			random.nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(him);
		HerobrineMod.LOGGER.info("one of them turned at [{}, {}, {}] in village {} ({})",
			spot.getX(), spot.getY(), spot.getZ(), key, phase.name());
		return true;
	}

	/**
	 * Somewhere in the village, on the ground, that nobody can see.
	 *
	 * The oldest rule in the mod and the one that costs the least to keep. A
	 * villager who fades in twenty blocks away in an empty square is a spawner;
	 * the same villager, found standing there when you come back round the
	 * well, has been there all evening.
	 *
	 * Inside the structure's own bounding box, so he is always somewhere a
	 * villager could plausibly be rather than in the field behind it.
	 */
	private static @org.jspecify.annotations.Nullable BlockPos somewhere(
			ServerLevel level, ServerPlayer player, BoundingBox bounds, RandomSource random) {
		for (int attempt = 0; attempt < 60; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			int x = player.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
			int z = player.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
			if (!bounds.intersects(x, z, x, z)) {
				continue;
			}
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			BlockPos at = new BlockPos(x, level.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
			if (!level.getBlockState(at).isAir()
				|| !level.getBlockState(at.above()).isAir()
				|| !level.getBlockState(at.below()).isSolid()
				|| !level.getFluidState(at).isEmpty()) {
				continue;
			}
			// NOT ON THE ROOF, and this needs saying because the heightmap will
			// happily offer one. MOTION_BLOCKING returns the top of the column,
			// which over a village house is the ridge — so the naive placement
			// puts a villager standing on somebody's thatch in the middle of the
			// night, and that reads as a bug rather than as a fright.
			//
			// Two cheap tests catch essentially all of it: village roofs are
			// stairs and slabs, and a roof is well above the square the player
			// is standing in.
			net.minecraft.world.level.block.state.BlockState under =
				level.getBlockState(at.below());
			if (under.is(net.minecraft.tags.BlockTags.STAIRS)
				|| under.is(net.minecraft.tags.BlockTags.SLABS)) {
				continue;
			}
			if (Math.abs(at.getY() - player.blockPosition().getY()) > 4) {
				continue;
			}
			// Out of everybody's sight, not just the one who triggered it. On a
			// server the whole thing is lost if he appears in somebody else's
			// screen while their friend is indoors.
			boolean watched = false;
			for (ServerPlayer eyes : level.players()) {
				if (eyes.blockPosition().closerThan(at, 64.0)
					&& com.bloomlet.herobrine.entity.HauntingSpawner.visibleFrom(level, eyes, at)) {
					watched = true;
					break;
				}
			}
			if (!watched) {
				return at;
			}
		}
		return null;
	}

	/** How many are standing in this world right now. */
	private static int alive(ServerLevel level) {
		return level.getEntities(ModEntities.TURNED, e -> e.isAlive()).size();
	}
}
