package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.block.TheWayBlock;
import com.bloomlet.herobrine.entity.GauntEntity;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.structure.Keep;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * THE GAUNTS IN HIS FOREST.
 *
 * His world is one forest, midnight, rain, with a city in it and a castle over
 * the city. The city and the castle have their garrisons. The forest between the
 * portal and the walls had nothing in it but sheep. Now it has these.
 *
 * Every ten seconds, for each player out in the trees, a coin is tossed. When it
 * lands, one to three gaunts stand up under the leaves twenty-four to forty
 * blocks BEHIND that player — never in front, never in view. They do what gaunts
 * do: hold still when watched, come on when not, sense twenty blocks through
 * trunks. So the forest finds you before you find it.
 *
 * They are the only gaunts in the game that go away again. Everything else of
 * that kind guards a post or a cell and stays there for good; these are marked
 * with GauntEntity.roam and fade once nobody is within ninety-six blocks, so a
 * party walking to the castle leaves nothing behind it and the count in the
 * woods never climbs past what one player can see at once. See GauntEntity.
 * removeWhenFarAway.
 *
 * Cost: one gated pass every EVERY ticks over his.players(); a box search of
 * radius NEAR only on the checks that pass the coin; a handful of block reads on
 * a chunk that is already loaded. Nothing scans the world.
 */
public final class HisWoods {

	private HisWoods() {}

	/** How often a player in the woods is considered at all. */
	private static final int EVERY = 200;

	/** One in this many considerations brings a pack, if nothing else stops it. */
	private static final int CHANCE = 3;

	/** After a pack has come for a player, no more for this long. */
	private static final long REST = 1800L;

	/** No new pack while this many woods gaunts are already within NEAR of the player. */
	private static final int CAP = 3;
	private static final double NEAR = 64.0;

	/** Pack size. */
	private static final int PACK_MIN = 1;
	private static final int PACK_MAX = 3;

	/** How far out they stand, and how far to either side of straight behind. */
	private static final double OUT_MIN = 24.0;
	private static final double OUT_MAX = 40.0;
	private static final double BEHIND_SPREAD = Math.toRadians(70.0);

	/** The rest of the pack stands this close to the first. */
	private static final int TOGETHER = 6;

	/** A spot is forest when there are leaves this far over the footing. */
	private static final int CANOPY = 14;

	private static final int SPOT_TRIES = 6;
	private static final int PACK_TRIES = 8;

	private static int tickCounter;
	private static final Map<UUID, Long> RESTED = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HisWoods::walk);
	}

	private static void walk(MinecraftServer server) {
		if (++tickCounter % EVERY != 0
			|| !Config.get().enabled || !Config.get().hisHost
			|| com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;
		}
		ServerLevel his = server.getLevel(TheWayBlock.HIS_WORLD);
		if (his == null || his.players().isEmpty()) {
			return;
		}
		long now = his.getGameTime();
		RandomSource random = his.getRandom();
		for (ServerPlayer player : his.players()) {
			if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
				continue;
			}
			Long rested = RESTED.get(player.getUUID());
			if (rested != null && now - rested < REST) {
				continue;
			}
			if (random.nextInt(CHANCE) != 0) {
				continue;
			}
			if (his.getEntitiesOfClass(GauntEntity.class,
					player.getBoundingBox().inflate(NEAR),
					g -> g.isAlive() && g.roams()).size() >= CAP) {
				continue;
			}
			if (come(his, player, random) > 0) {
				RESTED.put(player.getUUID(), now);
			}
		}
	}

	/** Stands a pack behind the player. Returns how many stood. */
	private static int come(ServerLevel his, ServerPlayer player, RandomSource random) {
		BlockPos first = spot(his, player, random);
		if (first == null) {
			return 0;
		}
		int wanted = PACK_MIN + random.nextInt(PACK_MAX - PACK_MIN + 1);
		int stood = 0;
		for (int i = 0; i < wanted; i++) {
			BlockPos at = i == 0 ? first : beside(his, first, random);
			if (at == null) {
				continue;
			}
			GauntEntity it = ModEntities.GAUNT.create(his, EntitySpawnReason.EVENT);
			if (it == null) {
				break;
			}
			it.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			it.roam();
			his.addFreshEntity(it);
			stood++;
		}
		return stood;
	}

	/** Somewhere in the trees, behind the player, out of the city and away from the castle. Feet position, or null. */
	private static BlockPos spot(ServerLevel his, ServerPlayer player, RandomSource random) {
		double back = Math.toRadians(player.getYRot()) + Math.PI;
		for (int tries = 0; tries < SPOT_TRIES; tries++) {
			double angle = back + (random.nextDouble() * 2.0 - 1.0) * BEHIND_SPREAD;
			double out = OUT_MIN + random.nextDouble() * (OUT_MAX - OUT_MIN);
			int x = (int)Math.floor(player.getX() - Math.sin(angle) * out);
			int z = (int)Math.floor(player.getZ() + Math.cos(angle) * out);
			BlockPos feet = footing(his, x, z);
			if (feet != null && inTheWoods(his, feet) && !nearHis(his, feet)) {
				return feet;
			}
		}
		return null;
	}

	/** Another footing within TOGETHER of the first, or null. */
	private static BlockPos beside(ServerLevel his, BlockPos first, RandomSource random) {
		for (int tries = 0; tries < PACK_TRIES; tries++) {
			int x = first.getX() + random.nextInt(TOGETHER * 2 + 1) - TOGETHER;
			int z = first.getZ() + random.nextInt(TOGETHER * 2 + 1) - TOGETHER;
			BlockPos feet = footing(his, x, z);
			if (feet != null && Math.abs(feet.getY() - first.getY()) <= 3) {
				return feet;
			}
		}
		return null;
	}

	/** Dry ground with two blocks of air over it, in a loaded chunk. Feet position, or null. */
	private static BlockPos footing(ServerLevel his, int x, int z) {
		if (!his.hasChunk(x >> 4, z >> 4) || !Ground.dry(his, x, z)) {
			return null;
		}
		int y = Ground.topOf(his, x, z);
		BlockPos feet = new BlockPos(x, y + 1, z);
		if (!his.getBlockState(feet).isAir() || !his.getBlockState(feet.above()).isAir()) {
			return null;
		}
		return feet;
	}

	/** Leaves somewhere overhead: it is under the trees, not in a clearing or on a road. */
	private static boolean inTheWoods(ServerLevel his, BlockPos feet) {
		BlockPos.MutableBlockPos up = feet.mutable();
		for (int dy = 2; dy <= CANOPY; dy++) {
			up.setY(feet.getY() + dy);
			if (his.getBlockState(up).is(BlockTags.LEAVES)) {
				return true;
			}
		}
		return false;
	}

	/** Inside the city's reach, or the castle's. Those have their own garrisons. */
	private static boolean nearHis(ServerLevel his, BlockPos feet) {
		BlockPos city = Keep.city(his);
		if (city != null && flat(city, feet) <= Keep.cityReach()) {
			return true;
		}
		BlockPos castle = Keep.site(his);
		return castle != null && flat(castle, feet) <= Keep.reach();
	}

	private static double flat(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}
}
