package com.bloomlet.herobrine.manifest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

	/**
	 * Synced, and it has to be.
	 *
	 * This is read on the CLIENT to decide whether to draw the infected skin,
	 * and a plain persistent attachment lives only on the server — so the
	 * client read null every time, decided the villager was fine, and drew an
	 * ordinary one. The red eyes worked throughout because MENACE was synced
	 * and this was not, which is a maddening way for it to fail: half the
	 * effect appears and the half that needs a restart looks like it needs
	 * another restart.
	 */
	public static final AttachmentType<Boolean> FERAL = AttachmentRegistry
		.<Boolean>builder()
		.persistent(Codec.BOOL)
		.syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL.cast(),
			net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("feral"));

	/** How far it notices somebody. Short — it has been in a small room. */
	private static final double NOTICE = 22.0;
	private static final double STRIKE_RANGE = 2.2;
	private static final int STRIKE_COOLDOWN = 20;
	private static final float STRIKE_DAMAGE = 3.0F;
	/**
	 * Faster than a walking player, slower than a sprint.
	 *
	 * Pulled back from 1.35. A thing that closes on you quicker than you can
	 * think about it is a jump scare, and this is meant to be a decision — you
	 * see it start, and you have the two or three seconds it takes to arrive to
	 * choose between the sword and the ladder.
	 */
	private static final double CHARGE = 1.08;

	private static final Map<UUID, Long> lastStruck = new HashMap<>();
	/** Which of them are currently coming, so the turn can be heard once. */
	private static final Set<UUID> roused = new HashSet<>();

	/** Seven seconds between beats, staggered per creature. */
	private static final int HEARTBEAT = 140;

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
	 * be behind that door in fifty hours' time.
	 */
	public static void shutIn(Mob mob) {
		// Nothing it used to want. A villager brain keeps a job site, a bed and
		// a home and will walk off to any of them the moment it is left alone,
		// which is not what something that has been in a cell for years does.
		mob.getBrain().clearMemories();
		mob.setAttached(FERAL, true);
		// No eyes. Those are his, and the animals he is wearing — this is not
		// being worn by anything, it is what is left when he has finished.
		mob.setSilent(true);
		mob.setPersistenceRequired();

		// Read straight back. If the server cannot see what it just wrote, the
		// problem is the write; if it can and the client still draws a plain
		// villager, the problem is the sync. Two guesses have already been
		// spent on this, so the next run says which.
		HerobrineMod.LOGGER.info("shut in a {}: feral={} silent={}",
			mob.getType().toShortString(), isFeral(mob), mob.isSilent());
	}

	/**
	 * How often this looks. EVERY TICK WAS THE MOST EXPENSIVE THING IN THE MOD.
	 *
	 * It ran twenty times a second and, for every player, asked the level for every
	 * Mob in a forty-four block cube — eighty-five thousand blocks — then called
	 * hasLineOfSight on each. hasLineOfSight is a RAY TRACE. Thirty mobs near a
	 * player is six hundred ray traces a second, sixty is twelve hundred, and this
	 * mod has a phase whose entire point is a lot of mobs near a player.
	 *
	 * Every fifth tick is four looks a second, which is far finer than anything it
	 * drives: hunt() sets a target and watch() turns a head. Neither is perceptible
	 * at 20Hz and neither is missed at 4Hz; at 2.5Hz a turned head starts to lag
	 * a running player, so it stops here.
	 */
	private static final int LOOKS_EVERY = 5;

	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		if (server.getTickCount() % LOOKS_EVERY != 0) {
			return;
		}
		float sweep = (float)(Math.sin(server.overworld().getGameTime() / 24.0) * 55.0);
		double reach = NOTICE * NOTICE;

		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				AABB around = player.getBoundingBox().inflate(NOTICE);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, around)) {
					if (!isFeral(mob)) {
						continue;
					}
					// THE CORNERS OF THE CUBE, THROWN AWAY BEFORE THE RAY TRACE.
					//
					// getEntitiesOfClass takes a BOX and the range is a RADIUS, so
					// nearly half of what it hands back is further off than NOTICE
					// and was being ray traced anyway. A squared distance is three
					// multiplies; the trace it replaces walks blocks.
					if (mob.distanceToSqr(player) > reach) {
						continue;
					}
					// Line of sight is the switch, and it is the right one
					// because it means the DOOR decides. Behind bars it cannot
					// see out properly, so it goes on watching the room it has
					// been alone in; the moment a player opens the door it can
					// see them, and it comes. Nothing had to be told about the
					// door — the geometry says it.
					if (mob.hasLineOfSight(player)) {
						hunt(level, mob, player);
					} else {
						watch(level, mob, sweep);
					}
				}
			}
		}
	}

	/**
	 * It has seen you, and now it is coming.
	 *
	 * No vanilla goal owns this — the target is cleared every tick so the
	 * villager's own brain cannot take over and wander it off to a job site.
	 * Head locked the whole way, which is what separates this from an angry
	 * mob: an angry mob looks where it is going.
	 */
	private static void hunt(ServerLevel level, Mob mob, ServerPlayer player) {
		// The moment it turns, once and loudly. Pitched UP rather than down:
		// the warden's own register belongs to something enormous, and raising
		// it puts the same wrongness in a body the size of a man.
		if (roused.add(mob.getUUID())) {
			level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
				SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 1.0F, 1.45F);
		} else if ((level.getGameTime() + Math.floorMod(mob.getUUID().hashCode(), 70)) % 70 == 0) {
			level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
				SoundEvents.WARDEN_AGITATED, SoundSource.HOSTILE, 0.8F, 1.4F);
		}
		mob.setTarget(null);
		mob.getLookControl().setLookAt(player, 90.0F, 90.0F);

		float yaw = (float)(Math.atan2(
			player.getZ() - mob.getZ(), player.getX() - mob.getX()) * (180.0 / Math.PI)) - 90.0F;
		mob.yHeadRot = yaw;

		mob.getNavigation().moveTo(player, CHARGE);
		strike(level, mob, player);
	}

	/**
	 * The head sweep, for the ones that cannot see out yet.
	 *
	 * Driven off game time rather than a stored angle so every one of them is
	 * in step, which sounds like a mistake and is not: two of them in
	 * neighbouring cells turning their heads together is far more disturbing
	 * than two of them doing it independently. It reads as one thing wearing
	 * two bodies, which is precisely what it is.
	 */
	private static void watch(ServerLevel level, Mob mob, float angle) {
		roused.remove(mob.getUUID());
		mob.getNavigation().stop();

		// A heartbeat through the door, every seven seconds or so.
		//
		// Borrowed from the warden and pitched down, because the game has no
		// sound for "something alive is in there and has been for years" and
		// that one is the closest thing to it. Quiet enough that a player in
		// the passage hears it before they can tell where it is coming from,
		// which is the entire reason it is a heartbeat and not a groan.
		// Offset per creature, which is the fix for the echo.
		//
		// Keyed on game time alone, every one of them beat on exactly the same
		// tick — so two cells a few blocks apart fired the same sound at the
		// same instant and it came back as one doubled, slightly flanged noise
		// rather than as two things breathing. Their own id spreads them out,
		// and two heartbeats that are close but not together is far worse than
		// either one alone.
		long own = Math.floorMod(mob.getUUID().hashCode(), HEARTBEAT);
		if ((level.getGameTime() + own) % HEARTBEAT == 0) {
			level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
				com.bloomlet.herobrine.sound.ModSounds.BREATH, SoundSource.HOSTILE, 0.45F, 1.1F);
		}
		mob.yHeadRot = angle;
		mob.setYRot(angle * 0.3F);
		mob.setYBodyRot(angle * 0.3F);
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
