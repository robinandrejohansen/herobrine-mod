package com.bloomlet.herobrine.manifest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

/**
 * At SIEGE, the animals turn.
 *
 * Not possessed — possession is a MIMIC event, it takes one creature, it gives
 * it white eyes and it means something. This is the opposite of that and has to
 * read differently: every cow, every pig, every chicken in the field, all of
 * them at once, none of them changed to look at. They simply will not leave the
 * player alone.
 *
 * WEAK ON PURPOSE. Half a heart, and not oftener than every two seconds. The
 * whole design brief for this is "irritating", and that is a real target rather
 * than a compromise: a herd that could kill you is a combat encounter, and the
 * player would fight it, win, and feel capable. A herd that cannot kill you but
 * will not stop is something you cannot resolve — you swat at it, it comes
 * back, and it goes on doing that for as long as the phase lasts. There is
 * nothing to beat, which is the point of the entire phase.
 *
 * It is also the cheapest possible way to say the world has changed sides. The
 * player has spent forty hours around these animals treating them as scenery.
 *
 * Tamed animals are left alone, and that is deliberate on two counts. TheDogKnows
 * already owns the player's relationship with their dog and two systems writing
 * the same mob is how most of the bugs in this repo started. And a wolf does
 * real damage, which would break the "weak" rule immediately.
 */
public final class TheHerd {
	private TheHerd() {}

	/** Half a heart. It is meant to be beneath contempt and impossible to stop. */
	private static final float NIP = 1.0F;
	private static final int NIP_COOLDOWN = 40;
	private static final double REACH = 1.8;
	private static final double NOTICE = 20.0;
	private static final double PLOD = 1.0;

	/** Every fifth tick. They are not fast and nobody will see the difference. */
	private static final int INTERVAL = 5;

	/** Enough to feel surrounded, few enough that a farm does not cost a tick. */
	private static final int PER_PLAYER = 12;

	private static final Map<UUID, Long> lastNipped = new HashMap<>();
	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TheHerd::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		if (++tickCounter % INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled
			|| !com.bloomlet.herobrine.Config.get().hostileAnimals) {
			return;
		}
		if (Wrath.phase(server) != Phase.SIEGE) {
			return;
		}

		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator() || player.isCreative()) {
					continue;
				}
				turn(level, player);
			}
		}
	}

	private static void turn(ServerLevel level, ServerPlayer player) {
		AABB around = player.getBoundingBox().inflate(NOTICE);
		int handled = 0;
		for (Animal animal : level.getEntitiesOfClass(Animal.class, around)) {
			if (handled >= PER_PLAYER) {
				return;
			}
			// NOTHING THAT LIVES IN WATER. Animal.class catches turtles,
			// axolotls and frogs, and a turtle shuffling over to nip somebody's
			// ankle is the single most ridiculous thing this mod could show.
			// Restricting to the CREATURE category keeps it to the land animals
			// a farm actually has, which is the only place this idea works.
			if (animal.getType().getCategory()
				!= net.minecraft.world.entity.MobCategory.CREATURE) {
				continue;
			}
			if (animal instanceof TamableAnimal || Feral.isFeral(animal)) {
				continue;
			}
			handled++;

			// Driven rather than given a target, because a cow has no attack
			// goal to give one to. Its brain will keep trying to wander off to
			// grass every tick and this simply outvotes it, which is also why
			// the navigation is re-issued each time rather than once.
			animal.getNavigation().moveTo(player, PLOD);
			animal.getLookControl().setLookAt(player, 30.0F, 30.0F);
			nip(level, animal, player);
		}
	}

	private static void nip(ServerLevel level, Animal animal, ServerPlayer player) {
		if (animal.distanceTo(player) > REACH) {
			return;
		}
		long now = level.getGameTime();
		Long last = lastNipped.get(animal.getUUID());
		// A null check rather than a sentinel. A Long.MIN_VALUE seed here would
		// overflow the subtraction and the cooldown would never expire, which
		// is exactly the bug that stopped Herobrine ever landing a blow.
		if (last != null && now - last < NIP_COOLDOWN) {
			return;
		}
		lastNipped.put(animal.getUUID(), now);
		player.hurtServer(level, level.damageSources().mobAttack(animal), NIP);
	}
}
