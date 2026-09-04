package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.GauntEntity;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.entity.TurnedEntity;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.wrath.Wrath;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

/**
 * WHAT HE LEFT ON THE DOOR.
 *
 * Every place after the farm is held. Not by a crowd — by a few, and the few
 * are his: the Turned, who here do not stalk but stand and come for you, and
 * the Gaunt, which does exactly what it always does, which at a door you also
 * have to get through is worse. They are posted once, the first time anybody
 * comes within sight of the place, and they are posted before that person is
 * close enough to see it happen. They do not despawn, they do not wander off,
 * and they do not follow you home: their post is the house.
 *
 * NUMBERS BY CHAPTER. Three Turned and a Gaunt at the town; six and three at
 * the threshold. A guard is a fight you are meant to win, at a
 * cost, and the cost is what makes the book in the chest feel earned. What
 * makes them hard is not the count: a posted Turned has twice the health, iron
 * on him, and no daylight rule, and the Gaunt cannot be watched and fought at
 * the same time.
 *
 * NOTHING HERE RUNS PER TICK. Dwellings asks every two seconds, for the places
 * that are raised, and the answer for a posted place is one attachment read.
 * The posting itself happens once per place per world.
 */
public final class Watch {
	private Watch() {}

	/** Where a posted one stands. Present only on the watch; both entities read it. */
	public static final AttachmentType<Long> POST =
		AttachmentRegistry.createPersistent(HerobrineMod.id("post"), Codec.LONG);

	/** How far from the post a guard will wander, and how far it will chase before it lets go. */
	public static final int HOLDS = 20;
	public static final double LETS_GO = 36.0;

	/** The Turned and the Gaunts posted, by chapter: homestead, town, tower, gaol, church, threshold. */
	private static final int[] TURNED = {0, 3, 3, 4, 5, 6};
	private static final int[] GAUNTS = {0, 1, 2, 2, 3, 3};

	/** Posted once somebody is this close — and nobody is closer than TOO_CLOSE, or they would see it. */
	private static final int POSTS_AT = 96;
	private static final int TOO_CLOSE = 44;
	/** The ring they stand in, measured from the door. Outside most footprints, inside the fence. */
	private static final int RING_MIN = 9;
	private static final int RING_MAX = 16;
	private static final int STANDS_TRIES = 14;
	/** A footing this far above or below the door is a roof or a ravine, not a yard. */
	private static final int LEVEL_WITH = 6;

	private static final AttachmentType<Boolean> TOWN = flag("town");
	private static final AttachmentType<Boolean> TOWER = flag("tower");
	private static final AttachmentType<Boolean> GAOL = flag("gaol");
	private static final AttachmentType<Boolean> CHURCH = flag("church");
	private static final AttachmentType<Boolean> THRESHOLD = flag("threshold");

	private static AttachmentType<Boolean> flag(String place) {
		return AttachmentRegistry.createPersistent(HerobrineMod.id("watch_" + place), Codec.BOOL);
	}

	/** Class initialisation is the registration; the attachment types have to exist before a level loads. */
	public static void register() {
	}

	private static @org.jspecify.annotations.Nullable AttachmentType<Boolean> of(String place) {
		return switch (place) {
			case "TOWN" -> TOWN;
			case "TOWER" -> TOWER;
			case "GAOL" -> GAOL;
			case "CHURCH" -> CHURCH;
			case "THRESHOLD" -> THRESHOLD;
			default -> null;
		};
	}

	/**
	 * Asked by Dwellings for every raised place, every CHECK_INTERVAL. Posts the
	 * watch the first time somebody is walking toward the place from far enough
	 * off not to see it happen. Chapter is the place's position in the chain.
	 */
	public static void post(ServerLevel level, String place, BlockPos site, int chapter) {
		AttachmentType<Boolean> flag = of(place);
		if (flag == null || chapter <= 0 || chapter >= TURNED.length
			|| Boolean.TRUE.equals(level.getAttached(flag))) {
			return;
		}
		if (Wrath.removed(level.getServer())) {
			level.setAttached(flag, true);      // beaten. his things are people again; nothing to post
			return;
		}
		boolean coming = false;
		double cx = site.getX() + 0.5, cy = site.getY(), cz = site.getZ() + 0.5;
		for (ServerPlayer who : level.players()) {
			double d = who.distanceToSqr(cx, cy, cz);
			if (d < (double)TOO_CLOSE * TOO_CLOSE) {
				return;      // they would see it happen. next time they come
			}
			if (d <= (double)POSTS_AT * POSTS_AT) {
				coming = true;
			}
		}
		if (!coming || !level.hasChunk(site.getX() >> 4, site.getZ() >> 4)) {
			return;
		}
		int stood = raise(level, site, chapter);
		level.setAttached(flag, true);
		HerobrineMod.LOGGER.info("the {} is held: {} posted at {}", place.toLowerCase(java.util.Locale.ROOT),
			stood, site.toShortString());
		if (stood > 0) {
			Company.watched(level, site);
		}
	}

	/** Puts a chapter's watch around a spot, now, and says how many found footing. Also behind /herobrine watch. */
	public static int raise(ServerLevel level, BlockPos site, int chapter) {
		int which = Math.max(1, Math.min(chapter, TURNED.length - 1));
		RandomSource random = level.getRandom();
		int stood = 0;
		for (int i = 0; i < TURNED[which]; i++) {
			TurnedEntity him = ModEntities.TURNED.create(level, EntitySpawnReason.EVENT);
			if (him == null) {
				break;
			}
			if (stand(level, him, site, random, 2)) {
				him.guard(site);
				level.addFreshEntity(him);
				stood++;
			}
		}
		for (int i = 0; i < GAUNTS[which]; i++) {
			GauntEntity it = ModEntities.GAUNT.create(level, EntitySpawnReason.EVENT);
			if (it == null) {
				break;
			}
			if (stand(level, it, site, random, 2)) {      // its box is 1.95; the model stoops. See GauntEntity.stooped
				it.guard(site);
				level.addFreshEntity(it);
				stood++;
			}
		}
		return stood;
	}

	/** A spot in the ring with footing level with the door and headroom above it. */
	private static boolean stand(ServerLevel level, Mob who, BlockPos site, RandomSource random, int headroom) {
		for (int tries = 0; tries < STANDS_TRIES; tries++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double r = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
			int x = site.getX() + (int)Math.round(Math.cos(angle) * r);
			int z = site.getZ() + (int)Math.round(Math.sin(angle) * r);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			int y = Ground.topOf(level, x, z);
			if (Math.abs(y - site.getY()) > LEVEL_WITH) {
				continue;
			}
			BlockPos feet = new BlockPos(x, y + 1, z);
			boolean clear = true;
			for (int up = 0; up < headroom && clear; up++) {
				clear = level.getBlockState(feet.above(up)).isAir();
			}
			if (!clear) {
				continue;
			}
			who.snapTo(x + 0.5, y + 1, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			return true;
		}
		return false;
	}
}
