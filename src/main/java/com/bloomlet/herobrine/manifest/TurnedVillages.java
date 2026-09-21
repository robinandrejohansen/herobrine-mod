package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.entity.TurnedEntity;
import com.bloomlet.herobrine.mixin.MobTargetsAccessor;
import com.bloomlet.herobrine.wrath.Phase;
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
 * FROM THE THIRD PLACE, THE VILLAGES ARE HIS.
 *
 * TheTurning put one of them in a village a night — the man who does not sleep
 * — and Villages boarded the doors and dug the graves. The villagers themselves
 * went on trading. From TRESPASSER on they do not: every villager in a vanilla
 * village is one of the turned the moment its chunk comes in, with the same
 * pretence by day and the same coming at night or when you stare at it, and
 * every iron golem changes sides — it stops going for the monsters (they are
 * his now) and comes for you instead.
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

	private static final Phase FROM = Phase.TRESPASSER;
	private static final AttachmentType<Boolean> HIS_GOLEM =
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
		if (Wrath.removed(server) || !Wrath.phase(server).atLeast(FROM)) {
			return;
		}
		if (entity instanceof Villager man) {
			Cadence.in(server, 1, () -> turn(level, man));
		} else if (entity instanceof IronGolem golem && !Boolean.TRUE.equals(golem.getAttached(HIS_GOLEM))) {
			golem.setAttached(HIS_GOLEM, true);
			side(golem);
		}
	}

	private static void turn(ServerLevel level, Villager man) {
		if (!man.isAlive() || man.isRemoved()) {
			return;
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
