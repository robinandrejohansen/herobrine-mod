package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.structure.Blueprint;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.structure.Keep;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * THE FIGHT IN HIS CASTLE, AND IT HAS ONE OWNER.
 *
 * <h2>What was wrong</h2>
 *
 * He arrives over the keep and circles it — a good entrance. Then the first blow
 * lands and the HUNT takes him: the code written for chasing somebody across a
 * field at night, with its moods and its watching-from-a-distance and its
 * take-to-the-air-when-the-path-fails. In an open field that reads as a man who
 * will not stop. Next to his own castle it reads as a man who will not fight: he
 * orbits you, you chase him, he goes up, you wait.
 *
 * And it had a second owner. Helpers' blows — Addexio, a golem — went through
 * the hunt's damage ledger, and forty points of that ledger is the hunt ENDING:
 * "driven off", back through the door, and a fresh one of him over the keep with
 * the count reset. Act two, twice, in one fight. Not two systems fighting over a
 * tick; two systems with two different ideas of how the fight finishes.
 *
 * <h2>What this is</h2>
 *
 * From the first blow, this class has the tick. Nothing else moves him. It
 * decides, every tick and with a plain rule, between the four things he can do:
 *
 *     CLOSE      arm's length      he swings. Stand and click and he BLINKS OUT —
 *                                  a fist-fight is what a player wants and what
 *                                  he refuses to give.
 *     MID        four to twelve    he comes at you, or sidesteps round you, or
 *                                  holds and throws. Rolled, so he is never a
 *                                  metronome.
 *     FAR        past twelve       he does not chase. He appears at mid range in
 *                                  front of you, or he throws from where he is.
 *     GONE       no line           he is in another room. So are you, shortly.
 *
 * <h2>Flying, walking, teleporting</h2>
 *
 * THE FIRST BLOW TAKES THE SKY FROM HIM AND HE NEVER GETS IT BACK. That is what
 * `bound` is: a flag on him and on the level, so a save and a respawn find him
 * grounded. Flight was the hunt's answer to every obstacle, and against a single
 * target in a building it is the one answer that makes him unreachable — a boss
 * that cannot be reached is a boss you wait out, and waiting is not a fight.
 *
 * Walking is the default, because a man walking at you is the image this whole
 * mod is built on. Teleporting is the ANSWER TO GEOMETRY: a pillar, a door he no
 * longer fits through at act two, a gap, a room. It is short, it is announced
 * (smoke, the sound), and it never goes further than the next room. He is a
 * thing that is suddenly beside you, not a thing that leaves.
 *
 * The one exception is the pause between acts, which lifts him for three
 * seconds on fire and sets him back down. That is a ceremony, not a tactic, and
 * ascend() is allowed through the guard for exactly its own duration.
 *
 * <h2>The building is the arena</h2>
 *
 * The castle is nine floors of rooms with real ceilings, and at act three he is
 * over three blocks tall — four of those floors will not hold him. So the rooms
 * are read off the world once, with their headroom, and every teleport picks
 * from rooms he fits in and a player can walk to. That measurement is what makes
 * the third act move: he needs the tall rooms, and the fight follows him there.
 */
final class Duel {

	private final HerobrineEntity him;

	// ---- range bands, in blocks
	private static final double CLOSE = 3.2;
	private static final double MID = 12.0;
	private static final double TOO_NEAR_TO_APPEAR = 2.5;

	// ---- clocks, in ticks
	private static final int DECIDE_MIN = 25;
	private static final int DECIDE_SPREAD = 20;
	private static final int BLINK_REST = 60;
	private static final int BLINK_REST_PUNISHED = 30;
	private static final int STUCK_AFTER = 30;
	private static final int WOUND_WINDOW = 60;
	/** Blows taken inside the window before he refuses to stand there. Per act. */
	private static final int[] STANDS_FOR = { 4, 3, 2 };

	// ---- pace
	private static final double WALK = 1.0;
	private static final double CHARGE = 1.25;
	private static final double STRAFE_RADIUS = 5.0;

	private enum Move { ADVANCE, STRAFE, HOLD }

	private Move move = Move.ADVANCE;
	private int decideIn;
	private int castIn = 40;
	private int blinkIn = 20;
	private int stuck;
	private double wasAt = Double.MAX_VALUE;
	private int seenHits;
	private int tookRecently;
	private long lastBlowAt;
	private @Nullable Vec3 strafeTo;
	private int strafeSide = 1;
	private boolean placed;
	private @Nullable List<Room> rooms;
	private long saidAt;

	/** A place to stand indoors, and how much air is over it. */
	record Room(BlockPos at, int headroom) {}

	Duel(HerobrineEntity him) {
		this.him = him;
	}

	/**
	 * Whether this owns the tick. Only once he is bound — the entrance, the circling
	 * over the keep, is the old code and it is right; it is what happens after the
	 * first blow that this replaces.
	 */
	boolean owns(List<Player> watchers) {
		if (!this.him.hisGround() || !(this.him.level() instanceof ServerLevel his)
			|| Keep.site(his) == null) {
			return false;
		}
		// BOUND, HE IS THIS AND NOTHING ELSE — even with nobody in range. Otherwise
		// two minutes of an empty castle hands him back to the hunt's stalemate,
		// which loses interest, which puts him back to prowling, and the prowl is
		// written for a wood: it fells trees and digs into hills. In his own city.
		// With nobody here tick() finds no target and he simply stands.
		return this.him.isBound() || !watchers.isEmpty();
	}

	void tick(List<Player> watchers) {
		if (!(this.him.level() instanceof ServerLevel here)) {
			return;
		}
		this.him.noStalemate();
		if (!this.him.isBound()) {
			this.idle(here, watchers);
			return;
		}
		this.him.engaged();

		ServerPlayer target = this.pick(watchers);
		if (target == null) {
			return;
		}
		this.him.getLookControl().setLookAt(target, 90.0F, 90.0F);

		// THE PAUSE. He is up there on fire and the bolts are coming down round
		// him; the only thing he does is look at you.
		if (this.him.isAscending()) {
			return;
		}
		if (this.him.inTheAir()) {
			this.him.down();     // whatever put him up, the fight is on the ground
		}

		if (this.blinkIn > 0) {
			this.blinkIn--;
		}
		if (this.castIn > 0) {
			this.castIn--;
		}
		this.wounds(here);

		// FIRST TICK BACK. A save reloaded, or a fresh one of him after the last was
		// unloaded, arrives on the keep with the count intact and nowhere in
		// particular. Put him in a room near whoever is here.
		if (!this.placed) {
			this.placed = true;
			if (this.toARoom(here, target, 14.0, "back for the rest of it")) {
				return;
			}
		}

		// HE HAS OUTGROWN THE ROOM. Act three is 3.06 blocks; a three-block ceiling
		// pins him in the floor. The fight moves to somewhere tall.
		if (this.blinkIn <= 0 && this.headroomHere(here) < this.needs()) {
			if (this.toARoom(here, target, 30.0, "too tall for this room now")) {
				return;
			}
		}

		double d = this.him.distanceTo(target);
		boolean sees = this.him.hasLineOfSight(target);

		if (!sees) {
			this.gone(here, target, d);
			return;
		}
		if (d <= CLOSE) {
			this.close(here, target);
		} else if (d <= MID) {
			this.mid(here, target, d);
		} else {
			this.far(here, target, d);
		}
	}


	// ---- BEFORE THE FIRST BLOW ----------------------------------------------------
	//
	// He used to circle the keep at twenty-four blocks until somebody shot him.
	// That is an entrance, and it is a good one, and it was also the whole of what
	// he did: a melee player could stand under him for an hour. So the circling is
	// now only the first of four stages, read off how close you have come:
	//
	//     OUT      > 60 from the keep    the silhouette. He circles, and looks.
	//     WALLS    > 34, or outside      he is down, standing on a battlement,
	//                                    watching you cross his city. Look away and
	//                                    he is on a different tower.
	//     HALL     inside the footprint  he is gone from the wall. He is in the
	//                                    great hall, at the far end, facing the door.
	//     NEAR     in the hall with him  he speaks once. He walks to five blocks and
	//                                    stops. Stand there long enough and HE
	//                                    starts it — the first blow is his.
	//
	// The teleports are silent here on purpose: blink() only announces itself once
	// he is bound. Before that he is a ghost in his own house, and the horror of a
	// ghost is that you did not see it move.

	private static final double OUT_PAST = 60.0;
	private static final double WALLS_PAST = 34.0;
	private static final double SPEAKS_WITHIN = 24.0;
	private static final double STARES_WITHIN = 12.0;
	private static final double STOPS_AT = 5.0;
	private static final int PATIENCE = 400;          // twenty seconds of you standing there
	private static final int PERCH_REST = 100;
	private static final int UNSEEN_MOVES_HIM = 160;
	private static final int SPEAK_REST = 400;
	private static final double APPROACH_PACE = 0.6;

	private static final String[] WAITING = {
		"you came all this way",
		"i have been standing here a long time",
		"close the door behind you",
		"they all stood where you are standing",
		"i know what you brought",
		"come closer. i want to see it",
		"this is the last house",
	};

	private int stage = -1;
	private int perchIn;
	private int unseenFor;
	private int patience;
	private long spokeAt = -10000L;
	private boolean inTheHall;
	private @Nullable List<BlockPos> perches;
	private @Nullable BlockPos hall;
	private int surface;
	private @Nullable BlockPos corner;
	private @Nullable BlockPos size;

	private void idle(ServerLevel here, List<Player> watchers) {
		ServerPlayer target = this.pick(watchers);
		BlockPos site = Keep.site(here);
		if (target == null || site == null) {
			return;
		}
		this.rooms(here);            // measures the building the first time through
		double fromKeep = Math.sqrt(site.distToCenterSqr(target.getX(), site.getY(), target.getZ()));
		boolean inside = this.insideTheWalls(target);

		int now = fromKeep > OUT_PAST ? 0 : (!inside && fromKeep > WALLS_PAST) || this.perches == null
			|| this.hall == null ? 1 : 2;
		if (now != this.stage) {
			this.stage = now;
			this.perchIn = 0;
			this.patience = 0;
			this.inTheHall = false;
			this.say(here, switch (now) {
				case 0 -> "circling — " + (int) fromKeep + " blocks out";
				case 1 -> "on the walls, watching";
				default -> "in the hall, waiting";
			});
		}

		switch (now) {
			case 0 -> {
				// The entrance: the old orbit, exactly as it was.
				if (!this.him.circleTheKeep()) {
					this.him.face(target);
				}
			}
			case 1 -> {
				if (this.him.inTheAir()) {
					this.him.down();
				}
				this.him.getNavigation().stop();
				this.him.face(target);
				this.watchFromTheWalls(here, target);
			}
			default -> {
				if (this.him.inTheAir()) {
					this.him.down();
				}
				this.waitInTheHall(here, target);
			}
		}
	}

	/**
	 * A figure on the battlements. He picks a perch that can see you, and when you
	 * stop being able to see him for eight seconds — you went behind a house, you
	 * looked down — he is on another one. The city is crossed under his eye and the
	 * eye is never in the same place twice.
	 */
	private void watchFromTheWalls(ServerLevel here, ServerPlayer target) {
		if (this.perches == null || this.perches.isEmpty()) {
			return;
		}
		boolean seen = target.hasLineOfSight(this.him);
		this.unseenFor = seen ? 0 : this.unseenFor + 1;
		if (--this.perchIn > 0) {
			return;
		}
		boolean onOne = false;
		for (BlockPos p : this.perches) {
			if (p.closerToCenterThan(this.him.position(), 1.5)) {
				onOne = true;
				break;
			}
		}
		if (onOne && this.unseenFor < UNSEEN_MOVES_HIM
			&& this.him.distanceTo(target) > STARES_WITHIN) {
			return;
		}
		// Somewhere on the wall that can see them, not too close, not the one he is on.
		List<BlockPos> fit = new ArrayList<>();
		for (BlockPos p : this.perches) {
			double d = Math.sqrt(p.distToCenterSqr(target.position()));
			if (d < 14.0 || d > 48.0 || p.closerToCenterThan(this.him.position(), 3.0)) {
				continue;
			}
			if (this.him.seesSpot(target, p)) {
				fit.add(p);
			}
		}
		if (fit.isEmpty()) {
			fit.addAll(this.perches);
		}
		BlockPos to = fit.get(this.him.getRandom().nextInt(fit.size()));
		this.him.blinkTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, this.yawTo(to, target));
		this.perchIn = PERCH_REST;
		this.unseenFor = 0;
	}

	/**
	 * The far end of the great hall, facing the way in.
	 *
	 * He speaks once when you can see each other. Inside twelve blocks he comes to
	 * five and stops — close enough that you could hit him, and he knows it, and
	 * he waits to see whether you will. Twenty seconds of neither of you moving and
	 * he settles it himself: the first blow of the fight is his, and from that
	 * swing he is bound and the Duel has him.
	 */
	private void waitInTheHall(ServerLevel here, ServerPlayer target) {
		if (this.hall == null) {
			return;
		}
		if (!this.inTheHall) {
			this.inTheHall = true;
			this.him.blinkTo(this.hall.getX() + 0.5, this.hall.getY(), this.hall.getZ() + 0.5,
				this.yawTo(this.hall, target));
			this.him.getNavigation().stop();
			return;
		}
		double d = this.him.distanceTo(target);
		boolean sees = this.him.hasLineOfSight(target);
		this.him.face(target);

		if (sees && d <= SPEAKS_WITHIN && here.getGameTime() - this.spokeAt > SPEAK_REST) {
			this.spokeAt = here.getGameTime();
			String line = WAITING[this.him.getRandom().nextInt(WAITING.length)];
			target.sendSystemMessage(net.minecraft.network.chat.Component.literal("§8§o" + line));
		}
		if (!sees || d > STARES_WITHIN) {
			this.patience = Math.max(0, this.patience - 2);
			this.him.getNavigation().stop();
			return;
		}
		if (d > STOPS_AT) {
			this.him.getNavigation().moveTo(target, APPROACH_PACE);
		} else {
			this.him.getNavigation().stop();
		}
		if (++this.patience >= PATIENCE && d <= CLOSE + 1.5) {
			this.say(here, "you stood there too long — the first blow is his");
			this.him.bind();
			this.him.slash(target);
		}
	}

	private boolean insideTheWalls(Player who) {
		if (this.corner == null || this.size == null) {
			return false;
		}
		return who.getX() >= this.corner.getX() && who.getX() < this.corner.getX() + this.size.getX()
			&& who.getZ() >= this.corner.getZ() && who.getZ() < this.corner.getZ() + this.size.getZ()
			&& who.getY() >= this.surface - 3;
	}

	private float yawTo(BlockPos from, Player to) {
		return (float) Math.toDegrees(Math.atan2(to.getZ() - (from.getZ() + 0.5),
			to.getX() - (from.getX() + 0.5))) - 90.0F;
	}

	// ---- THE FOUR BANDS -------------------------------------------------------

	/**
	 * Arm's length. He swings, and he does not let you stand here.
	 *
	 * The count of blows taken in the last three seconds is the whole tactic: a
	 * player who plants their feet and holds the button gets STANDS_FOR of them,
	 * and then he is six blocks away and they are swinging at air. Fewer each act.
	 * That is what makes act three feel different from act one with a bigger
	 * model — the window to hit him gets shorter.
	 */
	private void close(ServerLevel here, ServerPlayer target) {
		this.him.getNavigation().stop();
		this.him.face(target);

		int stands = STANDS_FOR[Math.min(STANDS_FOR.length - 1, this.him.actNow() - 1)];
		if (this.tookRecently >= stands && this.blinkIn <= 0) {
			this.tookRecently = 0;
			if (this.appear(here, target, 5.0, 9.0, false)
				|| this.appear(here, target, 5.0, 9.0, true)) {
				this.blinkIn = BLINK_REST_PUNISHED;
				this.castIn = Math.min(this.castIn, 8);   // and something comes back
				this.say(here, "blinked out of a fist-fight");
				return;
			}
		}
		this.him.slash(target);

		// AND A STEP TO THE SIDE, sometimes, after the swing. A man who swings and
		// stands is a training dummy; a man who swings and moves is fighting.
		if (this.him.getRandom().nextInt(3) == 0) {
			this.strafeTo = this.sidestep(here, target);
			this.move = Move.STRAFE;
			this.decideIn = 10;
		}
	}

	/**
	 * Four to twelve blocks. The decision band.
	 *
	 * Every second or two he rolls: come at you, go round you, or hold and let the
	 * ranged clock fire. The roll leans towards coming at you, because that is the
	 * fight; but a third of the time he is doing something else, which is the
	 * difference between a boss and a zombie with more health.
	 */
	private void mid(ServerLevel here, ServerPlayer target, double d) {
		if (--this.decideIn <= 0) {
			this.decideIn = DECIDE_MIN + this.him.getRandom().nextInt(DECIDE_SPREAD);
			int roll = this.him.getRandom().nextInt(100);
			this.move = roll < 55 ? Move.ADVANCE : roll < 85 ? Move.STRAFE : Move.HOLD;
			if (this.move == Move.STRAFE) {
				this.strafeSide = this.him.getRandom().nextBoolean() ? 1 : -1;
				this.strafeTo = this.sidestep(here, target);
			}
			this.stuck = 0;
			this.wasAt = Double.MAX_VALUE;
		}

		this.cast(here, target);

		switch (this.move) {
			case ADVANCE -> {
				if (this.him.theyAreAloft(target) || this.above(target) > 3.0) {
					this.reach(here, target, "they went up");
					return;
				}
				this.him.getNavigation().moveTo(target, this.him.actNow() >= 3 ? CHARGE : WALK);
				if (d < this.wasAt - 0.05) {
					this.stuck = 0;
				} else if (++this.stuck > STUCK_AFTER) {
					this.stuck = 0;
					this.reach(here, target, "no way through on foot");
				}
				this.wasAt = d;
			}
			case STRAFE -> {
				if (this.strafeTo == null
					|| this.him.getNavigation().isDone()
					|| this.him.position().distanceTo(this.strafeTo) < 1.2) {
					this.strafeTo = this.sidestep(here, target);
				}
				if (this.strafeTo != null) {
					this.him.getNavigation().moveTo(this.strafeTo.x, this.strafeTo.y,
						this.strafeTo.z, WALK);
				}
				this.him.face(target);
			}
			case HOLD -> {
				this.him.getNavigation().stop();
				this.him.face(target);
			}
		}
	}

	/**
	 * Past twelve blocks he does not run after anybody.
	 *
	 * Running is what the hunt did and it is the thing this replaces: a chase across
	 * a courtyard is a chase, not a fight, and he loses it to anyone who can sprint.
	 * Instead, on the blink clock, HE IS AT MID RANGE IN FRONT OF YOU — appeared,
	 * announced, six to nine blocks off — and the band above takes over. Between
	 * blinks he throws.
	 */
	private void far(ServerLevel here, ServerPlayer target, double d) {
		this.him.getNavigation().stop();
		this.him.face(target);
		this.cast(here, target);
		if (this.blinkIn <= 0) {
			if (this.appear(here, target, 6.0, 9.0, true)
				|| this.appear(here, target, 6.0, 9.0, false)) {
				this.blinkIn = BLINK_REST;
				this.decideIn = 0;
				this.say(here, "closed " + (int) d + " blocks without crossing them");
			} else {
				// Nowhere in front of them to stand. Walk, then.
				this.him.getNavigation().moveTo(target, WALK);
			}
		}
	}

	/**
	 * No line of sight. Another room, a wall, round a corner.
	 *
	 * He knows where you are — he always does, in here — and the answer is the
	 * building's: a room near you that he fits in, on the blink clock. Failing
	 * that, the door, on foot.
	 */
	private void gone(ServerLevel here, ServerPlayer target, double d) {
		if (this.blinkIn <= 0 && d > TOO_NEAR_TO_APPEAR) {
			if (this.toARoom(here, target, 12.0, "came through the wall")
				|| this.him.beside(target)) {
				this.blinkIn = BLINK_REST;
				return;
			}
		}
		this.him.getNavigation().moveTo(target, WALK);
	}

	// ---- THE PARTS ------------------------------------------------------------

	/**
	 * Something thrown, on its own clock, by act. The same three the arsenal has —
	 * fire, a volley, a bolt — rolled against each other, so a bolt means the dice
	 * came up lightning the way a fireball means they came up fire.
	 */
	private void cast(ServerLevel here, ServerPlayer target) {
		if (this.castIn > 0 || !this.him.hasLineOfSight(target)
			|| this.him.distanceTo(target) < 4.0) {
			return;
		}
		int act = this.him.actNow();
		this.castIn = Math.max(26, 70 - act * 18) + this.him.getRandom().nextInt(20);
		int roll = this.him.getRandom().nextInt(100);
		switch (act) {
			case 1 -> {
				if (roll < 60) {
					this.him.fire(here, target, act);
				} else {
					this.him.volley(here, target, act);
				}
			}
			case 2 -> {
				if (roll < 40) {
					this.him.fire(here, target, act);
				} else if (roll < 75) {
					this.him.volley(here, target, act);
				} else {
					this.him.bolt(here, target);
				}
			}
			default -> {
				if (roll < 35) {
					this.him.fire(here, target, act);
				} else if (roll < 65) {
					this.him.volley(here, target, act);
				} else {
					this.him.bolt(here, target);
				}
			}
		}
	}

	/**
	 * Geometry is in the way: a pillar, a ledge, a door he no longer fits. He does
	 * not fly over it. He is up there with them, or beside them, or in the room.
	 */
	private void reach(ServerLevel here, ServerPlayer target, String why) {
		if (this.blinkIn > 0) {
			return;
		}
		if (this.him.upTo(target) || this.him.beside(target)
			|| this.toARoom(here, target, 10.0, why)) {
			this.blinkIn = BLINK_REST;
			this.say(here, why);
		}
	}

	/**
	 * Be somewhere else, a chosen distance from them, in front of them or not.
	 *
	 * The hunt's version of this reads the heightmap to find the ground, and inside
	 * a building the heightmap is the roof — so it would stand him on the
	 * battlements, or fail. In here the rooms are known, with their headroom, so
	 * the spot is picked from those: right distance, he fits, and either where they
	 * are looking (with a clear line, so the arrival is SEEN) or out of it. The
	 * hunt's method is the fallback for a castle that is not a blueprint.
	 */
	private boolean appear(ServerLevel here, ServerPlayer target, double min, double max,
	                       boolean wantSeen) {
		List<Room> all = this.rooms(here);
		if (all.isEmpty()) {
			return this.him.appearAt(target, min, max, wantSeen);
		}
		int need = this.needs();
		Vec3 look = target.getViewVector(1.0F).normalize();
		List<Room> fit = new ArrayList<>();
		for (Room room : all) {
			if (room.headroom() < need) {
				continue;
			}
			double d = Math.sqrt(room.at().distToCenterSqr(target.position()));
			if (d < min || d > max) {
				continue;
			}
			Vec3 toSpot = new Vec3(room.at().getX() + 0.5 - target.getX(),
				room.at().getY() - target.getEyeY(),
				room.at().getZ() + 0.5 - target.getZ()).normalize();
			boolean inFront = look.dot(toSpot) > (wantSeen ? 0.35 : 0.1);
			if (wantSeen ? (!inFront || !this.him.seesSpot(target, room.at())) : inFront) {
				continue;
			}
			fit.add(room);
		}
		if (fit.isEmpty()) {
			// Measured against the castle: at act three, one player position in
			// eleven has no tall room five to nine blocks away, and one in fifty has
			// none inside twelve. Widen once before giving the heightmap a go.
			if (max < 13.0) {
				return this.appear(here, target, min, max + 4.0, wantSeen);
			}
			return this.him.appearAt(target, min, max, wantSeen);
		}
		Room to = fit.get(this.him.getRandom().nextInt(fit.size()));
		float yaw = (float) Math.toDegrees(Math.atan2(
			target.getZ() - (to.at().getZ() + 0.5),
			target.getX() - (to.at().getX() + 0.5))) - 90.0F;
		this.him.blinkTo(to.at().getX() + 0.5, to.at().getY(), to.at().getZ() + 0.5, yaw);
		return true;
	}

	/** Where he goes to sidestep: the same distance from them, swung round. */
	private @Nullable Vec3 sidestep(ServerLevel here, ServerPlayer target) {
		Vec3 from = this.him.position().subtract(target.position());
		double radius = Math.max(STRAFE_RADIUS, Math.min(9.0, from.horizontalDistance()));
		double bearing = Math.atan2(from.z, from.x)
			+ this.strafeSide * Math.toRadians(35 + this.him.getRandom().nextInt(35));
		for (int attempt = 0; attempt < 6; attempt++) {
			double x = target.getX() + Math.cos(bearing) * radius;
			double z = target.getZ() + Math.sin(bearing) * radius;
			int y = (int) Math.floor(this.him.getY());
			for (int dy = 1; dy >= -2; dy--) {
				BlockPos at = new BlockPos((int) Math.floor(x), y + dy, (int) Math.floor(z));
				if (ConfinedPlacement.canStand(here, at)) {
					return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
				}
			}
			bearing += this.strafeSide * 0.35;
		}
		return null;
	}

	/** Count blows taken inside the last three seconds. */
	private void wounds(ServerLevel here) {
		int hits = this.him.hitsTaken();
		long now = here.getGameTime();
		if (hits > this.seenHits) {
			this.seenHits = hits;
			this.tookRecently++;
			this.lastBlowAt = now;
		} else if (now - this.lastBlowAt > WOUND_WINDOW) {
			this.tookRecently = 0;
		}
	}

	private @Nullable ServerPlayer pick(List<Player> watchers) {
		ServerPlayer best = null;
		double bestAt = Double.MAX_VALUE;
		java.util.UUID struckBy = this.him.lastStruckBy();
		for (Player who : watchers) {
			if (!(who instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			double at = this.him.distanceTo(p);
			// Whoever hit him last has his attention, unless they have run off.
			if (struckBy != null && struckBy.equals(p.getUUID()) && at < 40.0) {
				at -= 20.0;
			}
			if (at < bestAt) {
				bestAt = at;
				best = p;
			}
		}
		return best;
	}

	private double above(Player target) {
		return target.getY() - this.him.getY();
	}

	// ---- THE ROOMS ------------------------------------------------------------

	/** How many blocks of him there are, rounded up to whole blocks of ceiling. */
	private int needs() {
		return (int) Math.ceil(1.8 * this.him.getAttributeValue(Attributes.SCALE));
	}

	private int headroomHere(ServerLevel here) {
		BlockPos at = this.him.blockPosition();
		// solid(), not isAir(): a lantern on a chain is not a ceiling, and a torch
		// over his head is not a reason to leave the room.
		for (int up = 0; up < 6; up++) {
			if (solid(here, at.above(up))) {
				return up;
			}
		}
		return 6;
	}

	/**
	 * Blink to a room near them that he fits in. Not the one they are in — a
	 * different one within reach, so that "he is gone" is answered by "he is
	 * there", through a doorway, rather than by him materialising on top of you.
	 */
	private boolean toARoom(ServerLevel here, ServerPlayer target, double within,
	                        String why) {
		List<Room> all = this.rooms(here);
		if (all.isEmpty()) {
			return false;
		}
		int need = this.needs();
		List<Room> fit = new ArrayList<>();
		for (Room room : all) {
			if (room.headroom() < need) {
				continue;
			}
			double d = Math.sqrt(room.at().distToCenterSqr(target.position()));
			if (d > within || d < TOO_NEAR_TO_APPEAR + 1.0) {
				continue;
			}
			fit.add(room);
		}
		if (fit.isEmpty()) {
			return false;
		}
		Room to = fit.get(this.him.getRandom().nextInt(fit.size()));
		float yaw = (float) Math.toDegrees(Math.atan2(
			target.getZ() - (to.at().getZ() + 0.5),
			target.getX() - (to.at().getX() + 0.5))) - 90.0F;
		this.him.blinkTo(to.at().getX() + 0.5, to.at().getY(), to.at().getZ() + 0.5, yaw);
		this.say(here, why);
		return true;
	}

	/**
	 * Every place indoors a man could stand, with its headroom, read off the world
	 * once — and only the ones somebody can WALK to, because a blueprint this size
	 * has sealed voids in it, and a boss who teleports into the masonry has ended
	 * the fight.
	 *
	 * Same rule and same flood as Hoard, for the same building. A quarter of a
	 * million block reads, once, at the moment of the first blow.
	 */
	private List<Room> rooms(ServerLevel here) {
		if (this.rooms != null) {
			return this.rooms;
		}
		// NOT UNTIL IT IS STANDING. raise() schedules two seconds of placement and
		// a scan inside that window sees a foundation — and would be cached as the
		// castle for the whole fight. Return empty and uncached until the keep says
		// its last course is down; the next tick asks again.
		if (!Keep.standing(here)) {
			return List.of();
		}
		List<Room> found = new ArrayList<>();
		this.rooms = found;
		BlockPos site = Keep.site(here);
		String plan = Config.get().keepBlueprint;
		if (site == null || !Blueprint.have(plan)) {
			return found;
		}
		int surface = Ground.topOf(here, site.getX(), site.getZ()) + 1;
		BlockPos corner = Blueprint.corner(new BlockPos(site.getX(), surface, site.getZ()), plan);
		BlockPos size = Blueprint.measure(plan);
		if (corner == null || size == null) {
			return found;
		}
		int sx = size.getX();
		int sy = size.getY();
		int sz = size.getZ();
		int top = corner.getY() + sy - 1;
		this.corner = corner;
		this.size = size;
		this.surface = surface;

		java.util.Set<BlockPos> open = new java.util.HashSet<>();
		List<BlockPos> outside = new ArrayList<>();
		java.util.Map<BlockPos, Integer> head = new java.util.HashMap<>();
		for (int dx = 0; dx < sx; dx++) {
			for (int dz = 0; dz < sz; dz++) {
				for (int dy = 0; dy < sy - 2; dy++) {
					BlockPos floor = corner.offset(dx, dy, dz);
					if (!solid(here, floor)) {
						continue;
					}
					BlockPos at = floor.above();
					if (!clear(here, at) || !clear(here, at.above())) {
						continue;
					}
					open.add(at);
					int room = 2;
					boolean roofed = false;
					for (int up = at.getY() + 2; up <= top; up++) {
						if (solid(here, new BlockPos(at.getX(), up, at.getZ()))) {
							roofed = true;
							break;
						}
						room++;
					}
					if (!roofed) {
						outside.add(at);
					} else {
						head.put(at, room);
					}
				}
			}
		}
		java.util.Set<BlockPos> reached = new java.util.HashSet<>(outside);
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(outside);
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Direction way : Direction.Plane.HORIZONTAL) {
				for (int step = -1; step <= 1; step++) {
					BlockPos next = at.relative(way).above(step);
					if (open.contains(next) && reached.add(next)) {
						queue.add(next);
					}
				}
			}
		}
		for (var e : head.entrySet()) {
			if (reached.contains(e.getKey())) {
				found.add(new Room(e.getKey(), e.getValue()));
			}
		}
		// THE PERCHES: under open sky, on the building — six or more above its floor
		// keeps the field outside the wall and the courtyard paving off the list.
		List<BlockPos> walls = new ArrayList<>();
		for (BlockPos p : outside) {
			if (p.getY() >= surface + 6 && reached.contains(p)) {
				walls.add(p);
			}
		}
		this.perches = walls;
		// THE HALL: the floor level with the most tiles a man can stand on with three
		// blocks over him, and the tile of it nearest the middle. The tutorial castle
		// has one at 313 tiles and nothing else comes close.
		java.util.Map<Integer, List<BlockPos>> byFloor = new java.util.HashMap<>();
		for (Room room : found) {
			if (room.headroom() >= 3) {
				byFloor.computeIfAbsent(room.at().getY(), k -> new ArrayList<>()).add(room.at());
			}
		}
		List<BlockPos> biggest = List.of();
		for (List<BlockPos> floor : byFloor.values()) {
			if (floor.size() > biggest.size()) {
				biggest = floor;
			}
		}
		if (!biggest.isEmpty()) {
			double cx = 0, cz = 0;
			for (BlockPos p : biggest) {
				cx += p.getX();
				cz += p.getZ();
			}
			cx /= biggest.size();
			cz /= biggest.size();
			BlockPos seat = biggest.get(0);
			for (BlockPos p : biggest) {
				if (Math.hypot(p.getX() - cx, p.getZ() - cz) < Math.hypot(seat.getX() - cx, seat.getZ() - cz)) {
					seat = p;
				}
			}
			this.hall = seat;
		}
		int tall = 0;
		for (Room room : found) {
			if (room.headroom() >= 4) {
				tall++;
			}
		}
		HerobrineMod.LOGGER.info("duel: the castle has {} places to stand indoors, {} of them"
			+ " tall enough for the third act, {} perches on the walls, the hall at {}",
			found.size(), tall, walls.size(), this.hall);
		return found;
	}

	/**
	 * SOLID IS WHAT FEET SAY, NOT WHAT THE RENDERER SAYS. isSolidRender is false for
	 * stairs, slabs, fences and walls — which is every staircase in the building —
	 * so with it the flood could not climb a single floor and 73% of the castle was
	 * "unreachable". blocksMotion is the collision test. Doors and gates block
	 * motion too but open when pushed, so they count as passable rather than wall,
	 * or every room behind a shut door is sealed.
	 */
	private static boolean solid(ServerLevel here, BlockPos at) {
		BlockState state = here.getBlockState(at);
		return state.blocksMotion() && !opens(state);
	}

	private static boolean clear(ServerLevel here, BlockPos at) {
		BlockState state = here.getBlockState(at);
		return !state.blocksMotion() || opens(state);
	}

	private static boolean opens(BlockState state) {
		return state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
			|| state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock;
	}

	/** One line, not more than one every five seconds. */
	private void say(ServerLevel here, String what) {
		long now = here.getGameTime();
		if (now - this.saidAt < 100) {
			return;
		}
		this.saidAt = now;
		HerobrineMod.LOGGER.info("duel: act {}, {} blows — {}", this.him.actNow(),
			this.him.hitsTaken(), what);
	}
}
