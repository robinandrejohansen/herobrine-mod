package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;
import com.bloomlet.herobrine.entity.ModEntities;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * THE ONE THING THAT PROVES IT, AND IT IS NOT A NUMBER.
 *
 * Everything else in this mod says he is dangerous by doing things to YOU, and a
 * player has no way to price that. They have never fought anything else in this
 * game that behaves like him, so "he is strong" is a claim about a scale they do
 * not have. A hundred more damage or a thousand more health lands as difficulty,
 * not as rank.
 *
 * THE ENDER DRAGON IS THE SCALE. It is the one thing every Minecraft player has
 * measured themselves against — the end of the game, the fight you prepare for
 * over a hundred hours. So he takes it apart in front of them, in seven minutes,
 * having not been invited, and the entire question is settled without a word.
 *
 * AND THEY HAVE TO SEE IT. Not be told, not find the aftermath. They arrive for
 * the fight of their lives and they are an audience for somebody else's errand —
 * which is a far worse feeling than losing would have been, because losing would
 * at least have been about them.
 *
 * IT ENDS WITH HIM LOOKING AT ONE OF THEM. Not all of them. One, picked, from a
 * pillar, for as long as it takes. And if anybody walks toward him the sky takes
 * him and he is not there.
 *
 * AND THEN IT SNOWS IN THE END, which is impossible, and stays.
 */
public final class Demonstration {
	private Demonstration() {}

	/** Once per world. It cannot be the thing that happens every time. */
	private static final AttachmentType<Boolean> SHOWN = AttachmentRegistry
		.createPersistent(HerobrineMod.id("he_took_the_dragon"), Codec.BOOL);

	/**
	 * SEVEN MINUTES, AND THE BEATS ARE DRIVEN BY WHAT HAPPENS, NOT BY A CLOCK.
	 *
	 * He goes to the endermen when the dragon is HALF DOWN, not at some tick — the
	 * whole point of the beat is that he is bored of it, and boredom has to be
	 * caused by something the players watched happen. Every phase carries a ceiling
	 * as well, so a dragon that flies out of reach or an End with no endermen in it
	 * cannot leave the thing running all night.
	 */
	private static final int HANGS = 200;         // ten seconds of nothing
	private static final int DRAGON_CAP = 3200;
	private static final int CROWD_FOR = 1800;
	private static final int FINISH_CAP = 2200;
	private static final int WATCH_FOR = 1800;
	/**
	 * AND THE BITES ARE SIZED TO THE SEVEN MINUTES, NOT TO A DAMAGE FIGURE.
	 *
	 * The first cut took three off it every bolt, which had the dragon at half in
	 * twenty-three seconds and the whole performance done in three minutes. That is
	 * not a demonstration, it is a mugging — the players need long enough to stop
	 * looking for a way to help and start watching.
	 *
	 * Two hundred health, a bolt every second: seven tenths to get it to half over
	 * two and a half minutes, and one and a fifth to finish it in ninety seconds
	 * once he has come back for it. He is visibly quicker the second time.
	 */
	private static final float SLOW_BITE = 0.7F;
	private static final float FAST_BITE = 1.2F;
	private static final int BOLT_EVERY = 20;
	private static final int BLAST_EVERY = 32;
	/** He leaves a few standing. Wiping the room would say less than sparing it. */
	private static final int LEAVES_A_FEW = 3;
	/** He stops working on it at half and goes looking for something else. */
	private static final float BORED_AT = 0.5F;

	private static final int HANGING = 1;
	private static final int WORKING = 2;
	private static final int CROWDING = 3;
	private static final int FINISHING = 4;
	private static final int WATCHING = 5;
	private static int phase;
	private static int phaseAt;

	/** How high above it he works, and how wide he circles. */
	private static final double ABOVE = 12.0;
	private static final double ORBIT = 22.0;
	/** And how close anybody may get to the last beat before it ends. */
	private static final double TOO_NEAR = 14.0;

	private static int beat;
	private static @org.jspecify.annotations.Nullable HerobrineEntity him;
	private static java.util.@org.jspecify.annotations.Nullable UUID watched;
	private static int boltIn;
	private static int burstIn;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Demonstration::onTick);
	}

	/**
	 * Every tenth tick. A scripted one-off does not need 20Hz.
	 *
	 * It returns early on four conditions so the cost is normally nothing — but
	 * once somebody is standing in the End and the show has not run, it asks the
	 * level for every living ender dragon EVERY TICK for as long as they are there.
	 * The beats it drives are seconds long.
	 */
	private static final int WATCHES_EVERY = 10;

	private static void onTick(MinecraftServer server) {
		if (server.getTickCount() % WATCHES_EVERY != 0 || !Config.get().enabled) {
			return;
		}
		ServerLevel end = server.getLevel(Level.END);
		if (end == null || Boolean.TRUE.equals(
				server.overworld().getAttachedOrElse(SHOWN, false))) {
			return;
		}
		if (end.players().isEmpty()) {
			return;
		}
		EnderDragon dragon = null;
		for (EnderDragon found : end.getEntities(EntityTypes.ENDER_DRAGON,
				d -> d.isAlive())) {
			dragon = found;
			break;
		}
		if (beat == 0) {
			// Nothing starts until there is a dragon and somebody in the room to
			// watch it happen to.
			if (dragon == null) {
				return;
			}
			arrive(end, dragon);
			return;
		}
		if (him == null || !him.isAlive()) {
			stop(end, false);
			return;
		}
		beat++;
		int held = beat - phaseAt;
		switch (phase) {
			case HANGING -> {
				hold(end, dragon);
				if (held > HANGS) {
					go(WORKING);
				}
			}
			case WORKING -> {
				if (dragon == null) {
					go(WATCHING);
					return;
				}
				work(end, dragon, SLOW_BITE);
				// HALF DOWN AND HE LOSES INTEREST. The players watched him do it,
				// so they get to watch him stop caring.
				if (dragon.getHealth() <= dragon.getMaxHealth() * BORED_AT
					|| held > DRAGON_CAP) {
					HerobrineMod.LOGGER.info("half of it is gone. he turns on the room");
					go(CROWDING);
				}
			}
			case CROWDING -> {
				if (!crowd(end) || held > CROWD_FOR) {
					go(FINISHING);
				}
			}
			case FINISHING -> {
				if (dragon == null) {
					HerobrineMod.LOGGER.info("the dragon is his");
					go(WATCHING);
					return;
				}
				work(end, dragon, FAST_BITE);
				if (held > FINISH_CAP) {
					go(WATCHING);
				}
			}
			default -> {
				watch(end, held);
				if (held > WATCH_FOR) {
					stop(end, true);
				}
			}
		}
	}

	private static void go(int next) {
		phase = next;
		phaseAt = beat;
		boltIn = 0;
		burstIn = 0;
	}

	/**
	 * He does not fly in. He is above them, and then the sky says so.
	 */
	private static void arrive(ServerLevel end, EnderDragon dragon) {
		HerobrineEntity made = ModEntities.HEROBRINE.create(end, EntitySpawnReason.EVENT);
		if (made == null) {
			return;
		}
		Vec3 at = dragon.position().add(0.0, ABOVE, 0.0);
		made.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		made.beginShowing();
		end.addFreshEntity(made);
		him = made;
		beat = 1;
		watched = null;
		go(HANGING);
		for (ServerPlayer near : end.players()) {
			end.playSound(null, near.blockPosition(),
				com.bloomlet.herobrine.sound.ModSounds.HIS_WORLD,
				SoundSource.HOSTILE, 1.4F, 0.6F);
		}
		strike(end, made.blockPosition(), false);
		HerobrineMod.LOGGER.info("he has come for the dragon");
	}

	/** Hanging there while everybody works out what they are looking at. */
	private static void hold(ServerLevel end, @org.jspecify.annotations.Nullable
			EnderDragon dragon) {
		if (him == null) {
			return;
		}
		if (dragon != null) {
			him.getLookControl().setLookAt(dragon, 90.0F, 90.0F);
		}
		him.setDeltaMovement(Vec3.ZERO);
	}

	/**
	 * ROUND IT, AND DOWN ON IT.
	 *
	 * Orbiting rather than chasing, because the dragon is the one that is supposed
	 * to circle and it puts him in its place. Bolts on it and charges under it, and
	 * a fixed bite out of its health every second — a real fight would take as long
	 * as a real fight, and this is not one.
	 */
	private static void work(ServerLevel end, EnderDragon dragon, float bite) {
		if (him == null) {
			return;
		}
		double turn = beat * 0.045;
		Vec3 at = dragon.position().add(Math.cos(turn) * ORBIT, ABOVE, Math.sin(turn) * ORBIT);
		him.snapTo(at.x, at.y, at.z, (float) (-turn * (180.0 / Math.PI)) - 90.0F, 0.0F);
		him.setDeltaMovement(Vec3.ZERO);
		him.getLookControl().setLookAt(dragon, 90.0F, 90.0F);

		if (--boltIn <= 0) {
			boltIn = BOLT_EVERY;
			strike(end, dragon.blockPosition(), false);
			if (bite > 0.0F) {
				// Through the HEAD PART, which is the only path the dragon fight
				// itself understands. A bare hurtServer on the dragon is refused,
				// so the first cut of this would have flashed lightning at it for
				// seven minutes and taken nothing off — and worse, killing it any
				// other way would skip the portal, the egg and the gateway.
				dragon.hurt(end, dragon.head, end.damageSources().magic(), bite);
			}
		}
		if (--burstIn <= 0) {
			burstIn = BLAST_EVERY;
			end.explode(him, dragon.getX(), dragon.getY(), dragon.getZ(), 3.0F,
				Level.ExplosionInteraction.NONE);
		}
	}

	/**
	 * AND THEN HE TURNS ON THE ROOM.
	 *
	 * Halfway through the dragon he leaves it and starts on the endermen, which is
	 * the part that says this is not a duel. They answer — every one of them looks
	 * at him at once, which is a thing no player has ever seen — and it changes
	 * nothing at all.
	 */
	private static boolean crowd(ServerLevel end) {
		if (him == null) {
			return false;
		}
		java.util.List<EnderMan> lot = end.getEntitiesOfClass(EnderMan.class,
			him.getBoundingBox().inflate(64.0), e -> e.isAlive());
		if (lot.size() <= LEAVES_A_FEW) {
			him.setDeltaMovement(Vec3.ZERO);
			return false;      // near enough nothing left in the room
		}
		EnderMan next = lot.get(0);
		Vec3 over = next.position().add(0.0, 4.0, 0.0);
		him.snapTo(over.x, over.y, over.z, him.getYRot(), 0.0F);
		him.setDeltaMovement(Vec3.ZERO);
		him.getLookControl().setLookAt(next, 90.0F, 90.0F);
		for (EnderMan angry : lot) {
			angry.setTarget(him);
		}
		if (--boltIn <= 0) {
			boltIn = 20;
			strike(end, next.blockPosition(), true);
			next.hurtServer(end, end.damageSources().magic(), Float.MAX_VALUE);
			HerobrineMod.LOGGER.info("one of them looked at him. {} left",
				Math.max(0, lot.size() - 1));
		}
		return true;
	}

	/**
	 * IT ENDS WITH HIM LOOKING AT ONE OF THEM.
	 *
	 * From the tallest thing he can find, at a distance, at one player — chosen and
	 * then kept, so on a server the other three watch somebody else being decided
	 * about. Which is worse for all four of them.
	 */
	private static void watch(ServerLevel end, int held) {
		if (him == null) {
			return;
		}
		java.util.List<ServerPlayer> here = end.players();
		if (here.isEmpty()) {
			stop(end, true);
			return;
		}
		ServerPlayer one = null;
		for (ServerPlayer maybe : here) {
			if (watched == null || maybe.getUUID().equals(watched)) {
				one = maybe;
				break;
			}
		}
		if (one == null) {
			one = here.get(0);
		}
		watched = one.getUUID();

		if (held <= 1) {
			BlockPos perch = high(end, one.blockPosition());
			him.snapTo(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5, 0.0F, 0.0F);
			HerobrineMod.LOGGER.info("he is watching {} from [{}, {}, {}]",
				one.getName().getString(), perch.getX(), perch.getY(), perch.getZ());
		}
		him.setDeltaMovement(Vec3.ZERO);
		him.getLookControl().setLookAt(one, 90.0F, 90.0F);

		// And nobody gets to walk up to it.
		for (ServerPlayer near : here) {
			if (near.distanceTo(him) < TOO_NEAR) {
				stop(end, true);
				return;
			}
		}
	}

	/** The tallest thing within sight of them, or a pillar of his own making. */
	private static BlockPos high(ServerLevel end, BlockPos from) {
		BlockPos best = null;
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = end.getRandom().nextDouble() * Math.PI * 2.0;
			double out = 26.0 + end.getRandom().nextDouble() * 18.0;
			int x = from.getX() + (int) Math.round(Math.cos(angle) * out);
			int z = from.getZ() + (int) Math.round(Math.sin(angle) * out);
			if (!end.isLoaded(new BlockPos(x, 64, z))) {
				continue;
			}
			int top = end.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			if (best == null || top > best.getY()) {
				best = new BlockPos(x, top, z);
			}
		}
		return best != null ? best : from.offset(28, 18, 0);
	}

	/**
	 * THE SKY TAKES HIM.
	 *
	 * Being approached does not make him leave — nothing does. A bolt comes down on
	 * HIM and he is not there afterwards, which is a completely different sentence:
	 * whatever removed him, it was not the four people standing in the crater.
	 */
	private static void stop(ServerLevel end, boolean finish) {
		if (him != null) {
			if (finish) {
				strike(end, him.blockPosition(), false);
			}
			him.discard();
		}
		him = null;
		beat = 0;
		phase = 0;
		phaseAt = 0;
		watched = null;
		if (!finish) {
			return;
		}
		end.getServer().overworld().setAttached(SHOWN, true);
		snow(end);
		HerobrineMod.LOGGER.info("it is over, and it is snowing in the end");
	}

	/**
	 * AND THEN IT SNOWS IN THE END.
	 *
	 * There is no weather here, no water, no sky and no season. Snow is the one
	 * thing this place categorically cannot have, which is exactly why it is the
	 * right thing to leave behind: every other mark he makes could be argued with,
	 * and this one cannot be explained at all.
	 *
	 * On the ground rather than falling, so it is still there tomorrow.
	 */
	/** Rows of the snowfall per tick. See the note in snow(). */
	private static final int SNOW_ROWS = 12;

	/**
	 * STAGED, because it is thirteen thousand columns.
	 *
	 * A hundred and twenty-nine across, culled to a circle, and every column that
	 * survives costs a heightmap lookup and two block reads before it decides
	 * anything. That is around forty thousand operations, and it was all in one
	 * tick — a visible couple of hundred milliseconds, immediately after the one
	 * scripted set piece in the mod, which is the worst possible moment for a
	 * hitch.
	 *
	 * It is a one-off, so this is not about throughput. It is about the stutter
	 * landing on the beat the player is supposed to be looking at.
	 */
	private static void snow(ServerLevel end) {
		for (int from = -64; from <= 64; from += SNOW_ROWS) {
			final int start = from;
			com.bloomlet.herobrine.manifest.Cadence.in(end.getServer(),
					(from + 64) / SNOW_ROWS, () -> snowRows(end, start));
		}
	}

	private static void snowRows(ServerLevel end, int fromX) {
		BlockPos middle = new BlockPos(0, 64, 0);   // the End's island is always the origin
		int laid = 0;
		for (int dx = fromX; dx <= Math.min(64, fromX + SNOW_ROWS - 1); dx++) {
			for (int dz = -64; dz <= 64; dz++) {
				if (dx * dx + dz * dz > 64 * 64) {
					continue;
				}
				int x = middle.getX() + dx;
				int z = middle.getZ() + dz;
				if (!end.isLoaded(new BlockPos(x, 64, z))) {
					continue;
				}
				int top = end.getHeight(
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
				BlockPos on = new BlockPos(x, top, z);
				if (!end.getBlockState(on.below()).isSolid()
					|| !end.getBlockState(on).isAir()) {
					continue;
				}
				double out = Math.sqrt(dx * dx + dz * dz) / 64.0;
				// Thick at the middle and thinning to nothing at the edge, so it
				// has a weather front rather than a boundary.
				if (end.getRandom().nextDouble() < out * 0.9) {
					continue;
				}
				end.setBlock(on, end.getRandom().nextInt(4) == 0
					? Blocks.SNOW_BLOCK.defaultBlockState()
					: Blocks.SNOW.defaultBlockState(), 2);
				laid++;
			}
		}
		HerobrineMod.LOGGER.info("{} of it settled", laid);
	}

	private static void strike(ServerLevel end, BlockPos at, boolean real) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(end, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}
		bolt.setVisualOnly(!real);
		bolt.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		end.addFreshEntity(bolt);
	}
}
