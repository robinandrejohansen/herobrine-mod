package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.entity.TurnedEntity;
import com.bloomlet.herobrine.mixin.MobTargetsAccessor;
import com.bloomlet.herobrine.wrath.Wrath;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * HALF THE VILLAGES ARE HIS.
 *
 * Not by phase — phases decide which house comes next and nothing else. Each
 * village rolls once, from the world seed and where it stands (isHis), so the
 * answer is the same every time anyone walks in and different from village to
 * village: you find one that is his, and the next one over is people. In his
 * villages every villager is one of the turned the moment its chunk comes in,
 * and every iron golem changes sides — it stops going for the monsters (they
 * are his now) and comes for you instead. Villages boards up the same ones.
 *
 * Done on ENTITY_LOAD, which fires once for every entity a chunk brings in and
 * once for every one that spawns, so there is no scan and nothing per tick. The
 * swap itself is deferred one tick: the world is still adding the villager when
 * the event fires, and pulling him out from under it is asking for trouble.
 * Golems keep their instance across the swap of goals, so a transient mark
 * stops a second load from arming them twice.
 */
public final class TurnedVillages {

	private TurnedVillages() {}

	/** One village in this many is his. */
	private static final int ONE_IN = 2;

	/** Whether this village is his: fixed per village, from the seed and its position. */
	public static boolean isHis(ServerLevel level, net.minecraft.world.level.levelgen.structure.StructureStart village) {
		long h = level.getSeed() ^ (village.getChunkPos().pack() * 0x9E3779B97F4A7C15L);
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return Math.floorMod(h, ONE_IN) == 0;
	}

	/** The village this entity stands in, if it is one of his; null otherwise. */
	private static boolean inHisVillage(ServerLevel level, Entity entity) {
		net.minecraft.world.level.levelgen.structure.StructureStart village = level.structureManager()
			.getStructureWithPieceAt(entity.blockPosition(), net.minecraft.tags.StructureTags.VILLAGE);
		return village != null && village.isValid() && isHis(level, village);
	}
	public static final AttachmentType<Boolean> HIS_GOLEM =
		AttachmentRegistry.create(HerobrineMod.id("his_golem"));
	private static int turned;

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(TurnedVillages::onLoad);
	}

	private static void onLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof Villager) && !(entity instanceof IronGolem)) {
			return;      // the cheap test first: this fires for every entity in the game
		}
		if (!Config.get().enabled || !Config.get().theTurning
			|| level.dimension().equals(TheWayBlock.HIS_WORLD)) {
			return;
		}
		MinecraftServer server = level.getServer();
		if (Wrath.removed(server)) {
			return;
		}
		// A tick later: the world is still adding it, and the structure lookup is
		// better done outside the load. See the class note.
		if (entity instanceof Villager man) {
			Cadence.in(server, 1, () -> {
				if (man.isAlive() && inHisVillage(level, man)) {
					turn(level, man);
				}
			});
		} else if (entity instanceof IronGolem golem && !Boolean.TRUE.equals(golem.getAttached(HIS_GOLEM))) {
			Cadence.in(server, 1, () -> {
				if (golem.isAlive() && !Boolean.TRUE.equals(golem.getAttached(HIS_GOLEM))
					&& inHisVillage(level, golem)) {
					golem.setAttached(HIS_GOLEM, true);
					side(golem);
				}
			});
		}
	}

	private static void turn(ServerLevel level, Villager man) {
		if (!man.isAlive() || man.isRemoved()) {
			return;
		}
		if (man.getVillagerXp() > 0 || man.hasCustomName()) {
			return;      // somebody's trader, or somebody's friend: a trading hall is not his to take
		}
		TurnedEntity him = ModEntities.TURNED.create(level, EntitySpawnReason.CONVERSION);
		if (him == null) {
			return;
		}
		him.snapTo(man.getX(), man.getY(), man.getZ(), man.getYRot(), man.getXRot());
		if (man.getCustomName() != null) {
			him.setCustomName(man.getCustomName());
		}
		him.setPersistenceRequired();
		man.discard();
		level.addFreshEntity(him);
		if (++turned % 10 == 1) {
			HerobrineMod.LOGGER.info("a villager turned at [{}, {}, {}] — {} so far this session",
				man.getBlockX(), man.getBlockY(), man.getBlockZ(), turned);
		}
	}

	/** The golem's own targeting goes; one that comes for players goes in. Nothing else about it changes. */
	private static void side(IronGolem golem) {
		GoalSelector targets = ((MobTargetsAccessor) golem).herobrine$targets();
		for (WrappedGoal wrapped : List.copyOf(targets.getAvailableGoals())) {
			if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?>) {
				targets.removeGoal(wrapped.getGoal());      // the one that went for zombies. They are his now
			}
		}
		targets.addGoal(2, new NearestAttackableTargetGoal<>(golem, Player.class, true));
		golem.setPlayerCreated(false);
	}
}
