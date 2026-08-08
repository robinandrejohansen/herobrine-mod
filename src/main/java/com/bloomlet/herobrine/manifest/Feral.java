package com.bloomlet.herobrine.manifest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * The ones that were shut in.
 *
 * A possessed animal belongs to a player and follows them about; these belong
 * to nobody and have been behind an iron door since long before the player was
 * born. They are the same idea taken to its end — what the lab was for, tried
 * first, under the family's own kitchen.
 *
 * Two behaviours and the difference between them is the whole thing.
 *
 * SHUT IN, it looks. The head sweeps slowly side to side, over and over,
 * watching a room it has been alone in for years. That is what a player sees
 * first through the gap in the door, and it is worse than anything it could be
 * doing — a thing that is pacing is a thing that has been waiting.
 *
 * OUT, it comes at you. Head locked, no wandering, no hesitation, and faster
 * than you walk. It is not stronger than a villager because it does not need
 * to be: it is unarmed, ordinary health, and it will not stop, and the player
 * has to decide in about two seconds whether they are fighting it in a
 * corridor.
 *
 * Iron doors are what keeps them in, and that is deliberate rather than
 * decorative. Villagers cannot open them. Nothing down here gets out unless a
 * player opens it, which makes every one of these a choice the player made.
 */
public final class Feral {
	private Feral() {}

	public static final AttachmentType<Boolean> FERAL =
		AttachmentRegistry.createPersistent(HerobrineMod.id("feral"), Codec.BOOL);

	/** How far it notices somebody. Short — it has been in a small room. */
	private static final double NOTICE = 22.0;
	private static final double STRIKE_RANGE = 2.2;
	private static final int STRIKE_COOLDOWN = 20;
	private static final float STRIKE_DAMAGE = 3.0F;
	/** Faster than a walking player, slower than a sprint. It gets there. */
	private static final double CHARGE = 1.35;

	private static final Map<UUID, Long> lastStruck = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Feral::onTick);
	}

	public static boolean isFeral(Mob mob) {
		return Boolean.TRUE.equals(mob.getAttached(FERAL));
	}

	/**
	 * Shut one in.
	 *
	 * Silent, because the thing that makes these land is finding one already
	 * there rather than hearing it first. Persistent, because it has to still
	 * be behind that door in fifty hours' time. Red-eyed through the same
	 * attachment the possessed animals use, so it needs no rendering of its
	 * own — and red rather than white is exactly right here: white is his, and
	 * this is not him, it is what he leaves behind.
	 */
	public static void shutIn(Mob mob) {
		mob.setAttached(FERAL, true);
		mob.setAttached(Possession.POSSESSED, true);
		mob.setAttached(Possession.MENACE, 2);
		mob.setSilent(true);
		mob.setPersistenceRequired();
	}

	private static void onTick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				AABB around = player.getBoundingBox().inflate(NOTICE);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
					if (isFeral(mob)) {
						hunt(level, mob, player);
					}
				}
			}
		}
		sweepIdle(server);
	}

	private static void hunt(ServerLevel level, Mob mob, ServerPlayer player) {
		mob.setTarget(null);   // no vanilla goal owns this
		mob.getLookControl().setLookAt(player, 90.0F, 90.0F);

		float yaw = (float)(Math.atan2(
			player.getZ() - mob.getZ(), player.getX() - mob.getX()) * (180.0 / Math.PI)) - 90.0F;
		mob.yHeadRot = yaw;

		mob.getNavigation().moveTo(player, CHARGE);
		strike(level, mob, player);
	}

	/**
	 * The head sweep, for the ones nobody has found yet.
	 *
	 * Driven off game time rather than a stored angle so every one of them is
	 * in step, which sounds like a mistake and is not: two of them in
	 * neighbouring cells turning their heads together is far more disturbing
	 * than two of them doing it independently. It reads as one thing wearing
	 * two bodies, which is precisely what it is.
	 */
	private static void sweepIdle(MinecraftServer server) {
		float angle = (float)(Math.sin(server.overworld().getGameTime() / 24.0) * 55.0);
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				AABB around = player.getBoundingBox().inflate(48.0);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
					if (!isFeral(mob) || mob.distanceTo(player) <= NOTICE) {
						continue;
					}
					mob.getNavigation().stop();
					mob.yHeadRot = angle;
					mob.setYRot(angle * 0.25F);
					mob.setYBodyRot(angle * 0.25F);
				}
			}
		}
	}

	private static void strike(ServerLevel level, Mob mob, ServerPlayer player) {
		if (mob.distanceTo(player) > STRIKE_RANGE) {
			return;
		}
		long now = level.getGameTime();
		Long last = lastStruck.get(mob.getUUID());
		if (last != null && now - last < STRIKE_COOLDOWN) {
			return;
		}
		lastStruck.put(mob.getUUID(), now);
		player.hurtServer(level, level.damageSources().mobAttack(mob), STRIKE_DAMAGE);
	}
}
