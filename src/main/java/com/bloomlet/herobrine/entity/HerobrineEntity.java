package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.WrathTriggers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * He does not fight you. He watches, and he is gone when you look.
 *
 * Deliberately NOT a Monster: Monster brings hostile targeting and melee
 * attack goals, which would turn him into an ordinary mob you can kill and
 * stop being afraid of. PathfinderMob gives navigation and goals with no
 * combat, so the only behaviour is the one that makes him unsettling.
 */
public class HerobrineEntity extends PathfinderMob {
	/** Ticks a player has held their gaze on him before he leaves. */
	private int unseenTicks;
	/**
	 * How long he has been looked at, by anybody, since the first pair of eyes
	 * landed on him. ONE clock for the whole event, deliberately.
	 *
	 * Per-player allowances were tried and were wrong, and the reason is worth
	 * keeping. They guaranteed everyone their own second and a half — which
	 * sounds fair and quietly deletes the best thing the sighting has. If both
	 * of you saw him there is nothing to disagree about, and the entire mod is
	 * built on the player not being able to prove what happened.
	 *
	 * The clock belongs to whoever spots him first, and it runs whether or not
	 * anybody else has turned round. Your friend says "he was standing right
	 * there" and you were looking at the wrong hill and there is nothing to
	 * see, and now one of you has to take the other's word for it. Two people
	 * who cannot agree on what was in the clearing is a far worse place to be
	 * than two people who both watched something vanish.
	 */
	private int watchedTicks;
	private int fleeTicks;
	private boolean fleeing;
	/** Ticks since he arrived. */
	private int age;
	/** Whether any player actually laid eyes on him before he left. */
	private boolean witnessed;
	/** Where the player stood when he arrived. He goes back to it. */
	private @org.jspecify.annotations.Nullable BlockPos anchor;
	private int relocations;

	public void setAnchor(BlockPos pos) {
		this.anchor = pos;
	}

	/**
	 * He leaves on his own after this long, seen or not.
	 *
	 * A haunting is a moment. Left indefinitely he becomes scenery — you walk
	 * over, study him, and discover he does nothing, which is the end of being
	 * afraid of him. Better to be gone before the player is certain of what
	 * they saw.
	 */
	private static final int LIFETIME = 600;          // 30 seconds

	/** Get closer than this and he will not let you get closer still. */
	private static final double TOO_CLOSE = 17.0;

	/**
	 * How close he lets anybody get before he reacts, by phase.
	 *
	 * Seventeen for most of the mod. At HUNTER it collapses to seven, and the
	 * collapse is the content: a player who has spent hours learning that
	 * seventeen blocks is the wall walks straight through it and keeps going,
	 * and nothing happens. That is a far louder change than any new sound.
	 */
	private double standoff(Phase phase) {
		return holdsGround(phase) ? 7.0 : TOO_CLOSE;
	}

	/**
	 * Does he refuse to give the ground up?
	 *
	 * The phase, OR the fact that he is currently hunting. The second half is
	 * not a testing convenience: something that has been following you for a
	 * minute and then backs away the moment you turn on it was never hunting
	 * you in the first place, and a player would read that as the mod losing
	 * its nerve. If he came for you, he does not retreat.
	 */
	private boolean holdsGround(Phase phase) {
		return phase.atLeast(Phase.HUNTER) || this.hunting;
	}

	/** Arm's length. He is gone before anybody finds out what is here. */
	private static final double ARMS_LENGTH = 3.2;
	/**
	 * Walking pace, and walking is the point.
	 *
	 * He must be SEEN to come. A figure that closes instantly is a jump scare
	 * and the player learns nothing from it; a figure that takes four unhurried
	 * steps toward you is one you watched decide.
	 */
	private static final double ADVANCE_SPEED = 0.85;

	// ---- THE HUNT --------------------------------------------------------
	/**
	 * Slower than your sprint, faster than your walk, and that gap is the
	 * whole design.
	 *
	 * Sprinting gets away from him. Walking does not. So the player cannot
	 * ignore him and cannot casually stroll home either — they have to spend
	 * hunger, or they have to hide, and both of those are decisions. A pursuer
	 * you can outwalk is scenery; one you cannot outrun is unfair.
	 */
	private static final double HUNT_SPEED = 0.92;
	/** Break this far away and he stops. Far enough that it costs something. */
	private static final double OUTRUN = 52.0;
	/** A hunt runs longer than a sighting, and is the only thing that does. */
	private static final int HUNT_LIFETIME = 1400;
	/**
	 * Stuck for this long and he stops trying to walk it.
	 *
	 * Pathfinding around a lake or a ravine is exactly the sort of thing that
	 * turns a pursuer into a joke — the player watches him jog into a wall and
	 * the spell is finished. When the route fails he simply is not there any
	 * more, and then he is somewhere closer, behind them. Which is worse.
	 */
	private static final int STUCK_LIMIT = 70;

	private boolean hunting;
	private int stuckTicks;
	private double lastDistance = Double.MAX_VALUE;

	public void beginHunt() {
		this.hunting = true;
	}
	// ---- END THE HUNT ----------------------------------------------------

	/**
	 * How long he stays once you have stopped looking.
	 *
	 * The single best moment available: the player breaks line of sight for a
	 * second — a tree, a corner, a glance at their hotbar — and when they look
	 * back the figure is gone. Nothing had to move while they were watching,
	 * which is what makes it impossible to argue with. Short enough that a
	 * quick glance away is enough, long enough that a flickering sightline
	 * through leaves does not fire it by accident.
	 */
	private static final int UNSEEN_GRACE = 16;

	/**
	 * Two cones, because "on screen" and "being looked at" are different
	 * questions and sharing one answer between them broke the timer.
	 *
	 * SEEN_CONE is wide — over a hundred degrees — and decides whether the
	 * visit counted and whether everybody has lost him.
	 *
	 * HELD_CONE is narrow, about twenty degrees, and is the only thing that
	 * runs the countdown. He is near the middle of your view and you have him,
	 * which is what the allowance was always meant to be measuring.
	 */
	private static final double SEEN_CONE = 0.55;
	private static final double HELD_CONE = 0.93;

	/**
	 * Faster than a sprinting player, and by a margin.
	 *
	 * A sprint is about 0.28 blocks a tick, so the old 0.34 opened the gap at a
	 * walking pace and a determined player stayed on him the whole way. Being
	 * ALMOST able to catch him is the worst possible outcome: it makes him a
	 * mob with a speed stat rather than something that leaves when it chooses.
	 */
	private static final double FLEE_SPEED = 0.52;
	/** He does not run for long. He runs until he is out of sight. */
	private static final int FLEE_LIMIT = 70;

	/** How far out he cares who is watching. */
	/**
	 * How far away somebody still counts as present.
	 *
	 * NINETY-SIX, and it has to be bigger than the furthest the spawner will
	 * ever put him. It was 64 while HauntingSpawner.MAX_RADIUS was 68, so a
	 * placement out at the far end had NOBODY inside its own watcher box and
	 * hit the `watchers.isEmpty()` branch on its very first tick — placed and
	 * discarded before a single packet reached anyone. Two numbers that had to
	 * agree and did not, which is the shape of most of the bugs in this repo.
	 */
	private static final double WATCH_RANGE = 96.0;

	/**
	 * Chasing him costs you.
	 *
	 * Walking at him used to be free: he dissolved, you felt powerful, and the
	 * fear was spent. Now closing the distance is read as defiance and raises
	 * wrath sharply — which per LORE.md is precisely the thing that thins the
	 * seal. Players will chase him; the design should make chasing him the
	 * mistake rather than the solution, and it should teach that through
	 * consequence rather than a message.
	 */
	private static final int DEFIANCE_APPROACHED = 25;
	private static final int DEFIANCE_STRUCK = 40;

	/**
	 * How many times a chase relocates him before he actually goes.
	 *
	 * Bounded so a player cannot herd him around indefinitely — by the third
	 * approach the trick would be a mechanic rather than a fright.
	 */
	private static final int MAX_RELOCATIONS = 2;

	/**
	 * How likely he is to reappear behind you rather than simply leave,
	 * as a one-in-N chance, by phase.
	 *
	 * Not always, for the same reason the arrival cue is not always: a
	 * reliable response is a rule, and a rule you have learned is a mechanic
	 * rather than a fright. Not knowing whether chasing him will make him
	 * vanish or put him at your back is worse than either certainty.
	 *
	 * It rises with wrath because his tolerance for being chased should fall.
	 * Early he mostly gets out of your way; by MIMIC he almost always answers.
	 *
	 * The end of this curve is not "always relocates" — it is that he stops
	 * retreating at all. See DESIGN.md: at HUNTER he should hold his ground
	 * when you close, and that moment lands precisely because the player spent
	 * hours learning that he never does.
	 */
	private static int relocateChanceIn(Phase phase) {
		if (phase.atLeast(Phase.MIMIC)) {
			return 1;    // always
		}
		if (phase.atLeast(Phase.TRESPASSER)) {
			return 2;
		}
		return 3;
	}

	/**
	 * How often his arrival makes a sound, one in N.
	 *
	 * Not always, on purpose. A reliable cue becomes a tell — players learn
	 * "noise means he is behind me" and it stops being a fright and starts
	 * being a mechanic. At one in three you cannot trust it to mean anything,
	 * so it never resolves into a signal.
	 */
	private static final int CUE_CHANCE = 3;


	public HerobrineEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	/**
	 * One sound as he arrives, sometimes — a reason to turn around.
	 *
	 * Uses the step sound of whatever he is standing on: grass outdoors, stone
	 * in a cave, gravel on a beach. It always suits the surroundings, so it
	 * reads as the world making an ordinary noise rather than as a mod cue,
	 * and it can never be learned as one specific "Herobrine sound".
	 *
	 * That ordinariness is the point. Your brain files it as an animal and you
	 * turn round casually — and he is there. Being casually wrong is worse
	 * than being forewarned, which is why this is a rustle and not a stick
	 * snapping or a block being placed. Those are sounds only a person can
	 * make, so they alarm you before you have even turned.
	 */
	public void announceArrival() {
		if (!(this.level() instanceof ServerLevel server)
			|| this.random.nextInt(CUE_CHANCE) != 0) {
			return;
		}
		BlockPos below = this.blockPosition().below();
		SoundEvent step = server.getBlockState(below).getSoundType().getStepSound();
		server.playSound(null, this.getX(), this.getY(), this.getZ(),
			step, this.getSoundSource(), 0.5F, 0.9F + this.random.nextFloat() * 0.1F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			// He needs to be aware of you from much further than he ever
			// approaches — the whole behaviour is about distance.
			.add(Attributes.FOLLOW_RANGE, 96.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// No approach goal, deliberately. He used to close to a standoff
		// distance, which meant the player watched him WALK, and something you
		// watch cross a field is something you are studying rather than
		// something you have caught sight of. He is placed where he is placed
		// and he stays there: the whole event is a figure at a distance that
		// was already standing there when you looked up.
		// No LookAtPlayerGoal. Facing the player is not something to leave to a
		// probability, so tick() drives the rotation directly — and two things
		// both writing yaw is how most of the bugs in this repo started.
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}

		// Gone on his own, before he can become furniture.
		if (++this.age > (this.hunting ? HUNT_LIFETIME : LIFETIME)) {
			this.vanish("aged out, nobody ever turned round");
			return;
		}

		// Everyone, not just whoever happens to be closest.
		//
		// Nearest-player-only was wrong in every direction the moment a second
		// person was in the world. A friend sprinting at him from behind while
		// you stood still did nothing; you looking away made him leave even
		// though your friend was staring straight at him; and fleeing from the
		// nearest player ran him directly into the other one.
		List<Player> watchers = this.level().getEntitiesOfClass(
			Player.class, this.getBoundingBox().inflate(WATCH_RANGE),
			player -> player.isAlive() && !player.isSpectator());

		// Did ANYONE actually see him? Not "was he rendered" — was he in
		// somebody's view, unobstructed. A visit nobody perceived should not
		// count against the pacing budget (see ManifestationDirector).
		boolean seen = false;
		// And separately: has anybody actually got him, rather than merely
		// having him somewhere on their screen? Only that runs the countdown.
		boolean held = false;
		for (Player watcher : watchers) {
			if (inViewOf(watcher)) {
				seen = true;
				if (beingLookedAt(watcher)) {
					held = true;
					break;
				}
			}
		}
		if (seen) {
			this.witnessed = true;
		}

		if (this.fleeing) {
			this.flee(watchers, seen);
			return;
		}

		// You never get to reach him, and it does not matter which of you tries.
		Player closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			double distance = this.distanceTo(watcher);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = watcher;
			}
		}

		Phase phase = this.level() instanceof ServerLevel now
			? Wrath.phase(now.getServer()) : Phase.RUMOUR;
		double standoff = standoff(phase);

		if (closest != null && closestDistance < standoff) {
			// Everyone who closed in paid for it, not only the one who got
			// there first. Two people walking him down is twice the defiance,
			// which is the correct price for twice the pressure.
			for (Player watcher : watchers) {
				if (this.distanceTo(watcher) < standoff && watcher instanceof ServerPlayer chaser) {
					WrathTriggers.defiance(chaser, DEFIANCE_APPROACHED);
				}
			}
			// AT HUNTER HE DOES NOT GIVE THE GROUND UP.
			//
			// Everything before this taught one rule and taught it for hours:
			// walk at him and he leaves. It is the only power the player has
			// over him and they will have used it a dozen times. So this is
			// where it stops working, and it has to stop working by being
			// broken rather than by being tightened — he does not flee further
			// or sooner, he simply does not flee, and then he closes the last
			// of the distance himself.
			//
			// He is never reached, and that is not a detail. The moment a
			// player can touch him the mod has to answer whether he is a mob
			// with a hitbox, and every restraint in here exists so that
			// question never comes up. He goes at arm's length, and what is
			// left behind is the answer instead.
			if (holdsGround(phase) && closest instanceof ServerPlayer near) {
				this.closeOn(near, closestDistance);
				return;
			}
			if (!relocateBehind(watchers)) {
				// He does not pop out of existence in your face. He turns and
				// puts something between you, and THEN he is gone — which
				// leaves the player having watched him leave rather than
				// having watched him cease to exist. One is a person avoiding
				// them; the other is a special effect.
				this.fleeing = true;
			}
			return;
		}

		// THE HUNT. He does not wait to be looked at and he does not leave
		// because you stopped looking — the whole point is that none of the
		// rules you learned about him apply any more.
		if (this.hunting) {
			this.pursue(closest, closestDistance);
			return;
		}

		// He was always standing there and he goes on standing there. Moving
		// while watched would break the only claim the whole event makes.
		this.getNavigation().stop();

		if (watchers.isEmpty()) {
			this.vanish("no player within WATCH_RANGE");
			return;
		}

		// AND HE IS FACING YOU. Not "usually", not "after a moment".
		//
		// This was left to LookAtPlayerGoal, which is the wrong tool: that goal
		// picks a target on a probability, holds it for a random number of
		// ticks and then lets go, because it exists to make idle villagers
		// glance at passers-by. Applied here it meant he was often standing at
		// three-quarters profile staring off at a hillside, and a figure that
		// is not looking at you is a figure that has not noticed you — which is
		// the exact opposite of the only thing this event says.
		//
		// Driven straight from the geometry every tick instead, so there is
		// nothing to be probabilistic about. Body and head both, or the head
		// swivels on a body still facing wherever he was put.
		this.faceOneOf(watchers);

		if (seen) {
			this.unseenTicks = 0;
			// EARLY ON, BEING LOOKED AT IS ENOUGH TO END IT.
			//
			// At WATCHER he is gone three and a half seconds after somebody
			// actually has him — enough to find a shape and start walking
			// toward it, nowhere near enough to be sure of anything. That is the correct first encounter: the
			// player has seen something and has nothing to show for it, and
			// every later sighting is measured against a memory they do not
			// trust.
			//
			// The clock is shared and starts on FIRST sight, which is what
			// makes this work with company. Whoever spots him spends it, and
			// anybody still facing the other way arrives at an empty hill.
			//
			// The limit stretches with the phases until it stops existing, so
			// the same act of looking at him gets a longer and longer answer.
			// By HUNTER he simply looks back for as long as you care to stand
			// there, which is only frightening because of how briefly he used
			// to allow it.
			int allowed = this.level() instanceof ServerLevel here
				? staredDown(Wrath.phase(here.getServer())) : 0;
			// `held`, not `seen`. Being on somebody's screen is not being
			// looked at, and spending the allowance on the first is what left
			// nothing for the second.
			if (allowed > 0 && held && ++this.watchedTicks > allowed) {
				this.vanish("stared down");
			}
		} else if (this.witnessed && ++this.unseenTicks > UNSEEN_GRACE) {
			// Seen, then not seen, then not there. Nobody watches him go; they
			// simply find that he has gone, which is the one version of this
			// they cannot talk themselves out of.
			//
			// It takes EVERY pair of eyes losing him. Two people who split up
			// and keep him between them hold him there far longer than one
			// person can, which is the right reward for co-ordinating — and
			// FLEE_LIMIT still stops it becoming a stalemate.
			this.vanish("seen, then lost by everybody");
		}
	}

	/**
	 * How long he will let himself be looked at.
	 *
	 * Zero means indefinitely. The curve is the whole arc of the mod in one
	 * method: at first he cannot be held in the eye at all, and by the end he
	 * does not mind being seen.
	 */
	private static int staredDown(Phase phase) {
		return switch (phase) {
			// Long enough to register, and no longer.
			//
			// Twelve ticks was under the threshold at which a person can see
			// anything they were not already looking at — by the time the
			// figure has reached the client and been drawn, half of it is gone,
			// and the player's honest report was "I saw nothing". A sighting
			// nobody perceives is not a subtle sighting, it is a missing one.
			//
			// Thirty is a second and a half: enough to turn your head and find
			// a shape, nowhere near enough to study it. That gap is where the
			// doubt lives.
			case RUMOUR, WATCHER -> 70;
			case TRESPASSER -> 110;
			case MIMIC -> 200;
			case HUNTER, SIEGE -> 0;
		};
	}

	/**
	 * He breaks your line of sight, and then he is not there.
	 *
	 * Moved directly rather than pathfound, and through solid rock rather than
	 * around it. That sounds like cheating and is the opposite: a figure that
	 * has to find a route is a figure the player can corner, and being cornered
	 * would force the honest answer — that he is a mob with a hitbox. Backing
	 * into a cave wall and being gone never has to answer that question.
	 *
	 * He does not sprint away across open ground for long. FLEE_LIMIT is short
	 * because the goal is not escape, it is to be out of sight; the moment the
	 * player's view of him is broken by anything at all, that is the end of it.
	 */
	private void flee(List<Player> from, boolean seen) {
		if (from.isEmpty() || ++this.fleeTicks > FLEE_LIMIT) {
			this.vanish("fled far enough");
			return;
		}

		this.noPhysics = true;
		this.setNoGravity(true);
		this.getNavigation().stop();

		// Away from all of them at once, each pulling in inverse proportion to
		// how close they are. Running from only the nearest would have walked
		// him straight into whoever was flanking, which is exactly the move two
		// players will try the first time they see him.
		Vec3 away = Vec3.ZERO;
		for (Player watcher : from) {
			Vec3 apart = new Vec3(this.getX() - watcher.getX(), 0.0, this.getZ() - watcher.getZ());
			double distance = Math.max(1.0, apart.length());
			away = away.add(apart.normalize().scale(1.0 / distance));
		}
		if (away.lengthSqr() < 1.0E-4) {
			// Surrounded, or dead centre between them. He goes now rather than
			// picking an arbitrary direction and jittering.
			this.vanish("surrounded, nowhere to flee");
			return;
		}
		away = away.normalize();

		// FACING THEM the whole way, and this is the note that matters most.
		//
		// Something that turns its back and runs is frightened, and a
		// frightened thing is one the player has beaten. Backing away while
		// still looking at you is not a retreat at all — it is somebody
		// declining to let you any closer, without once looking away, and it
		// reverses who is in charge of the distance between you.
		Player watching = from.get(0);
		double nearest = Double.MAX_VALUE;
		for (Player candidate : from) {
			double gap = this.distanceToSqr(candidate);
			if (gap < nearest) {
				nearest = gap;
				watching = candidate;
			}
		}
		float yaw = (float)(Math.atan2(watching.getZ() - this.getZ(),
			watching.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);

		this.setPos(this.getX() + away.x * FLEE_SPEED,
			this.getY(), this.getZ() + away.z * FLEE_SPEED);

		if (!seen || this.level().getBlockState(this.blockPosition()).isSolid()) {
			this.vanish("broke line of sight while fleeing");
		}
	}

	/**
	 * He is simply not there any more.
	 *
	 * No smoke, no teleport sound. An earlier version had both and they were
	 * the same mistake as glow on his body: a departure effect announces a
	 * supernatural ability, which files him alongside endermen. People do not
	 * dissolve. The rule is that he is never seen arriving — the mirror of it
	 * is that he is never seen leaving, and absence with nothing marking the
	 * transition is far worse than any animation.
	 *
	 * What replaces it is one footstep, once, from somewhere else. It does not
	 * show anything and it does not explain anything. It only says he is not
	 * gone, he has moved — which is the opposite of the closure a puff of
	 * smoke gives you.
	 *
	 * level().playSound rather than this.playSound: he is isSilent(), and that
	 * suppression is wanted everywhere except here.
	 */
	/** Loosely in front of the player, with line of sight. Not aiming at him. */
	private boolean inViewOf(Player player) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		).normalize();
		return look.dot(toMe) > SEEN_CONE && player.hasLineOfSight(this);
	}

	/**
	 * Not "he is on screen" — "you are looking at him".
	 *
	 * These were one test and that is why the timer felt broken. The wide cone
	 * is over a hundred degrees across, so the clock started the instant he
	 * entered the far corner of the player's vision, at fifty blocks, as a
	 * two-pixel smudge. Most of the allowance was spent before anybody had
	 * found him, and what was left was the tail end — which is precisely the
	 * "no time to see it" being reported.
	 *
	 * The wide cone still decides whether he counts as witnessed and whether
	 * everyone has lost him. Only the countdown uses this one, and it means
	 * what it says: he is near the middle of your view and you have him.
	 */
	private boolean beingLookedAt(Player player) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		).normalize();
		return look.dot(toMe) > HELD_CONE && player.hasLineOfSight(this);
	}

	/**
	 * Turn and face whoever has him, or the nearest person if nobody does.
	 */
	private void faceOneOf(List<Player> watchers) {
		Player face = null;
		double nearest = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			if (this.beingLookedAt(watcher)) {
				face = watcher;
				break;
			}
			double distance = this.distanceTo(watcher);
			if (distance < nearest) {
				nearest = distance;
				face = watcher;
			}
		}
		if (face == null) {
			return;
		}
		float yaw = (float)(net.minecraft.util.Mth.atan2(
			face.getZ() - this.getZ(), face.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.yHeadRotO = yaw;
		this.setYBodyRot(yaw);
		this.getLookControl().setLookAt(face.getX(), face.getEyeY(), face.getZ(), 90.0F, 90.0F);
	}

	/**
	 * Sometimes the lights go with him.
	 *
	 * Approaching and dissolving is one readable pattern, and a pattern you
	 * have read is not frightening. Leaving something behind makes the visit
	 * an event rather than a sighting — and darkness closing in as he goes is
	 * the version that costs the player nothing permanent: every torch is
	 * dropped, and can be put straight back.
	 *
	 * Capped at three, and only ever torches. Anything that strands a player
	 * without a pickaxe or destroys a build is out of bounds (DESIGN.md §9).
	 */
	private void takeTheLight(ServerLevel server, Player player) {
		BlockPos origin = player.blockPosition();
		int taken = 0;
		int r = 8;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -3, -r), origin.offset(r, 3, r))) {
			if (taken >= 3) {
				break;
			}
			if (!server.getBlockState(pos).is(Blocks.TORCH)
				&& !server.getBlockState(pos).is(Blocks.WALL_TORCH)) {
				continue;
			}
			BlockPos at = pos.immutable();
			server.removeBlock(at, false);
			server.addFreshEntity(new ItemEntity(server,
				at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5,
				new ItemStack(Items.TORCH)));
			server.playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
				SoundEvents.FIRE_EXTINGUISH, this.getSoundSource(), 0.5F, 1.3F);
			taken++;
		}
	}

	/**
	 * What is left where he was standing.
	 *
	 * From TRESPASSER he leaves a few small fires behind him, which is the
	 * first thing in the mod that says outright that something was there —
	 * every trace before this could be argued with, and a ring of fire on the
	 * grass cannot be.
	 *
	 * FIRE IS THE MOST DANGEROUS BLOCK IN THIS MOD and it gets three separate
	 * safeguards, because burning down somebody's base or their forest by
	 * accident is not a scare, it is the end of their save. It is never placed
	 * where anything nearby can catch; it is never placed on anything that
	 * burns; and every one of them is put out after six seconds whether or not
	 * a player is there to see it.
	 *
	 * Six seconds is long enough to walk back and find it burning, and short
	 * enough that fire spread — which needs random ticks and time — almost
	 * never gets a turn.
	 */
	private void scorch(ServerLevel level) {
		int wanted = 4 + this.random.nextInt(3);
		int lit = 0;

		for (int attempt = 0; attempt < 24 && lit < wanted; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = 1.2 + this.random.nextDouble() * 2.4;
			BlockPos at = BlockPos.containing(
				this.getX() + Math.cos(angle) * range,
				this.getY(),
				this.getZ() + Math.sin(angle) * range);

			BlockPos ground = null;
			for (int down = 0; down <= 3; down++) {
				if (level.getBlockState(at.below(down)).isSolid()) {
					ground = at.below(down);
					break;
				}
			}
			if (ground == null || !level.getBlockState(ground.above()).isAir()) {
				continue;
			}
			if (!safeToBurn(level, ground)) {
				continue;
			}

			BlockPos flame = ground.above();
			level.setBlock(flame, Blocks.FIRE.defaultBlockState(), 2);
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), 120, () -> {
				if (level.getBlockState(flame).is(Blocks.FIRE)) {
					level.setBlock(flame, Blocks.AIR.defaultBlockState(), 2);
				}
			});
			lit++;
		}
	}

	/** Nothing within reach may be able to catch, including the floor itself. */
	private static boolean safeToBurn(ServerLevel level, BlockPos ground) {
		for (BlockPos near : BlockPos.betweenClosed(ground.offset(-2, -1, -2),
				ground.offset(2, 3, 2))) {
			if (level.getBlockState(near).ignitedByLava()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * He follows. That is all he does, and it is enough.
	 *
	 * No lunge, no shortcut, no line of sight required. A player who breaks
	 * away and keeps breaking away is rid of him; a player who stops to fight
	 * or to build finds him arriving. The only way to end it early is distance
	 * that costs hunger, and that is the point — this is the phase where he is
	 * no longer something that happens to you while you get on with your day.
	 */
	private void pursue(@org.jspecify.annotations.Nullable Player quarry, double distance) {
		if (quarry == null) {
			this.vanish("hunt: nobody left to follow");
			return;
		}
		if (distance > OUTRUN) {
			this.vanish("hunt: outrun");
			return;
		}

		float yaw = (float)(net.minecraft.util.Mth.atan2(
			quarry.getZ() - this.getZ(), quarry.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.setYBodyRot(yaw);
		this.getLookControl().setLookAt(quarry, 90.0F, 90.0F);
		this.getNavigation().moveTo(quarry, HUNT_SPEED);

		// Is he actually getting anywhere? Measured on distance to the player
		// rather than on distance travelled, because a mob happily jogging back
		// and forth along the near side of a ravine is moving the whole time
		// and getting nowhere, and only one of those two numbers notices.
		if (distance < this.lastDistance - 0.05) {
			this.stuckTicks = 0;
		} else if (++this.stuckTicks > STUCK_LIMIT) {
			this.stuckTicks = 0;
			if (!this.reappearNear(quarry)) {
				this.vanish("hunt: no way through and nowhere to reappear");
			}
		}
		this.lastDistance = distance;
	}

	/**
	 * Give up on the route and simply be closer, out of sight.
	 *
	 * Behind them and unseen, never in front and never where they are looking,
	 * so it still obeys the oldest rule in the mod: he is not seen arriving.
	 * The player loses him behind a hill, and the next time they check over
	 * their shoulder the gap has halved.
	 */
	private boolean reappearNear(Player quarry) {
		if (!(this.level() instanceof ServerLevel here)) {
			return false;
		}
		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = 12.0 + this.random.nextDouble() * 12.0;
			int x = net.minecraft.util.Mth.floor(quarry.getX() + Math.cos(angle) * range);
			int z = net.minecraft.util.Mth.floor(quarry.getZ() + Math.sin(angle) * range);
			int y = here.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos at = null;
			for (int down = 0; down <= 4 && at == null; down++) {
				BlockPos maybe = new BlockPos(x, y - down, z);
				if (ConfinedPlacement.canStand(here, maybe)) {
					at = maybe;
				}
			}
			if (at == null) {
				continue;
			}
			Vec3 look = quarry.getViewVector(1.0F).normalize();
			Vec3 toSpot = new Vec3(at.getX() + 0.5 - quarry.getX(),
				at.getY() - quarry.getEyeY(), at.getZ() + 0.5 - quarry.getZ()).normalize();
			if (look.dot(toSpot) > 0.1) {
				continue;   // in front of them; he would be seen arriving
			}
			this.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.getYRot(), 0.0F);
			this.lastDistance = Double.MAX_VALUE;
			return true;
		}
		return false;
	}

	/**
	 * The last few blocks, taken by him.
	 *
	 * Deliberately not a lunge. He walks, at less than your own pace, so there
	 * is time to understand what is happening and time to decide to run — and
	 * running is the correct answer, which is why he must never be so fast that
	 * it stops being a choice.
	 */
	private void closeOn(ServerPlayer player, double distance) {
		this.getLookControl().setLookAt(player, 90.0F, 90.0F);
		float yaw = (float)(net.minecraft.util.Mth.atan2(
			player.getZ() - this.getZ(), player.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.setYBodyRot(yaw);

		if (distance > ARMS_LENGTH) {
			this.getNavigation().moveTo(player, ADVANCE_SPEED);
			return;
		}

		// And then he is not there, and the room is dark.
		//
		// takeTheLight is normally one visit in three. Here it is every time,
		// because this is the one moment that has to leave a mark: a player who
		// walked him down and got nothing would conclude the standoff was a
		// bug. Torches are dropped rather than destroyed, so it costs them
		// nothing they cannot pick back up (DESIGN.md §9).
		if (this.level() instanceof ServerLevel here) {
			takeTheLight(here, player);
		}
		this.vanish("closed to arm's length");
	}

	private void vanish(String why) {
		HerobrineMod.LOGGER.info("stare over after {} ticks: {}", this.age, why);
		if (this.witnessed && this.level() instanceof ServerLevel burning
			&& Wrath.phase(burning.getServer()).atLeast(Phase.TRESPASSER)) {
			this.scorch(burning);
		}

		// One visit in three takes the light with it, so the departure is not
		// one memorised beat.
		if (this.witnessed && this.random.nextInt(3) == 0
			&& this.level() instanceof ServerLevel lights) {
			Player nearby = lights.getNearestPlayer(this, 24.0);
			if (nearby != null) {
				takeTheLight(lights, nearby);
			}
		}

		if (!this.witnessed) {
			Player missedBy = this.level().getNearestPlayer(this, 96.0);
			if (missedBy instanceof ServerPlayer sp) {
				com.bloomlet.herobrine.manifest.ManifestationDirector.wasted(
					com.bloomlet.herobrine.manifest.Manifestation.THE_STARE, sp);
			}
		}
		if (this.level() instanceof ServerLevel server) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double distance = 14.0 + this.random.nextDouble() * 8.0;
			server.playSound(
				null,
				this.getX() + Math.cos(angle) * distance,
				this.getY(),
				this.getZ() + Math.sin(angle) * distance,
				SoundEvents.STONE_STEP, this.getSoundSource(), 0.3F, 0.9F
			);
		}
		this.discard();
	}


	/**
	 * Nothing touches him.
	 *
	 * He had 40 health and no protection, so the first player to swing a sword
	 * ended the premise — the whole design rests on being unable to fight him
	 * until the Effigy. Damage now does nothing except make him leave, and
	 * leave angrier.
	 *
	 * This is also how the player is taught the rule. Nobody reads a manual:
	 * they hit him, watch it do nothing, and understand. Being told "you
	 * cannot kill this yet" is a worse lesson than finding out.
	 */
	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getEntity() instanceof ServerPlayer attacker) {
			// Swinging at him is the loudest possible defiance.
			WrathTriggers.defiance(attacker, DEFIANCE_STRUCK);
			// Whoever swung is not necessarily the only one here, so the same
			// all-players check applies before he reappears anywhere.
			if (!relocateBehind(level.getEntitiesOfClass(Player.class,
					this.getBoundingBox().inflate(WATCH_RANGE)))) {
				// Struck rather than merely approached: he leaves the same way,
				// which keeps the two responses consistent.
				this.fleeing = true;
			}
		}
		return false;
	}

	/**
	 * Chase him and he is behind you.
	 *
	 * Vanishing when approached was a resolution, and resolutions end fear —
	 * you walked at him, he dissolved, you won. Going back to where you were
	 * standing when he arrived turns the chase itself into the scare: you
	 * closed the distance for nothing, and the ground you gave up is now
	 * occupied.
	 *
	 * No effect, no sound, no motion. You never see him move — you turn round
	 * and he is simply at the other end, which is the same rule that governs
	 * his arrival.
	 *
	 * @return false when he has run out of relocations or there is nowhere
	 *         valid, in which case the caller makes him leave for good.
	 */
	private boolean relocateBehind(List<Player> watchers) {
		if (this.relocations >= MAX_RELOCATIONS
			|| this.anchor == null
			|| !(this.level() instanceof ServerLevel server)) {
			return false;
		}
		Phase phase = Wrath.phase(server.getServer());
		if (this.random.nextInt(relocateChanceIn(phase)) != 0) {
			return false;   // this time he simply goes
		}
		// The anchor may have been mined out, flooded, or built over since.
		if (!ConfinedPlacement.canStand(server, this.anchor)) {
			return false;
		}
		// Clear of everybody, not just the one who walked him down. Dropping
		// him behind the player who charged is worthless if it puts him in
		// their friend's face.
		for (Player watcher : watchers) {
			if (this.anchor.distToCenterSqr(watcher.getX(), watcher.getY(), watcher.getZ())
				< TOO_CLOSE * TOO_CLOSE) {
				return false;   // too near one of them; it would look like a stutter
			}
		}

		// Facing whoever is nearest the place he reappears, so he is looking at
		// somebody rather than off into the trees.
		Player facing = watchers.get(0);
		double best = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			double distance = this.anchor.distToCenterSqr(
				watcher.getX(), watcher.getY(), watcher.getZ());
			if (distance < best) {
				best = distance;
				facing = watcher;
			}
		}
		double dx = facing.getX() - (this.anchor.getX() + 0.5);
		double dz = facing.getZ() - (this.anchor.getZ() + 0.5);
		float yaw = (float)(net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		this.snapTo(this.anchor.getX() + 0.5, this.anchor.getY(), this.anchor.getZ() + 0.5, yaw, 0.0F);

		this.relocations++;
		this.unseenTicks = 0;
		// The clock back to zero, because this is a new sighting. Without it the
		// second appearance is instant and the player walks round the rock to
		// find him already gone.
		this.watchedTicks = 0;
		this.age = 0;          // a fresh visit; you have earned the second look
		return true;
	}

	/** Silent by design — no idle noise to give away where he is standing. */
	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return true;
	}
}
