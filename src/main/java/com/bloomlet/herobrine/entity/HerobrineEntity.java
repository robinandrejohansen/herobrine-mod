package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import java.util.List;
import java.util.UUID;

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
	private static final double HUNT_SPEED = 1.32;
	/** Break this far away and he stops. Far enough that it costs something. */
	private static final double OUTRUN = 52.0;
	/**
	 * A hunt runs longer than a sighting, and is the only thing that does.
	 *
	 * A hundred seconds, up from seventy, now that it has a rhythm rather than
	 * being one unbroken sprint. Long enough for three or four break-offs, so
	 * the player gets the pause and the return at least twice — once is an
	 * incident, twice is a pattern, and the pattern is what they carry.
	 */
	private static final int HUNT_LIFETIME = 2000;
	/**
	 * Stuck for this long and he stops trying to walk it.
	 *
	 * Pathfinding around a lake or a ravine is exactly the sort of thing that
	 * turns a pursuer into a joke — the player watches him jog into a wall and
	 * the spell is finished. When the route fails he simply is not there any
	 * more, and then he is somewhere closer, behind them. Which is worse.
	 */
	private static final int STUCK_LIMIT = 70;

	/** Anything up to this he jumps. Anything above it he goes over. */
	private static final int VAULT_MAX = 4;
	/** How high he will look for a way up before deciding to fly. */
	private static final int SCAN = 10;
	private static final double FLY_SPEED = 0.42;
	private static final double CLIMB_RATE = 0.38;
	/** Flight is a way past an obstacle, never a way of travelling. */
	private static final int FLY_LIMIT = 120;

	/** Where he goes to watch from, and where he comes back to. */
	private static final double WATCH_NEAR = 26.0;
	private static final double WATCH_FAR = 46.0;
	private static final double RUSH_NEAR = 9.0;
	private static final double RUSH_FAR = 17.0;

	private boolean flying;
	private int flyTicks;

	/**
	 * He does not only ever run at you.
	 *
	 * A pursuit that is one unbroken sprint from start to finish is a chase
	 * scene, and a chase scene is exciting rather than frightening — the player
	 * spends it looking forward, solving a movement problem, and never once has
	 * to wonder where he is. So he breaks off. He is suddenly a long way away,
	 * standing still, watching; and then he is not there; and then he is close
	 * again and coming.
	 *
	 * The variety in the DISTANCE is the part that does the work. Something
	 * that is always eight blocks behind you can be modelled. Something that is
	 * forty blocks away and then nine is not.
	 */
	private boolean watching;
	private int moodTicks;

	/**
	 * How many times he has broken off, and why it is counted.
	 *
	 * An unbounded watch-and-return loop is what "it is just coming back and
	 * back" means: every cycle is the same size as the last, so there is no
	 * way to tell the second from the fifth, and a thing with no shape reads as
	 * a thing with no end. Three, and each return comes in closer and stays
	 * shorter than the one before, so the player can feel it tightening even
	 * without counting.
	 */
	private int breakOffs;
	private static final int MAX_BREAK_OFFS = 3;

	/**
	 * Who he has already reached this round.
	 *
	 * A hunt is not a brawl and it is not a chase after whoever happens to be
	 * nearest. He picks one, he gets to them, and then he is finished with them
	 * and turns to somebody who has not been reached yet — round by round,
	 * until everybody has.
	 *
	 * Keyed on UUID rather than holding the entity, so somebody logging out or
	 * dying mid-round cannot pin a reference or block the round from ever
	 * completing.
	 */
	private final java.util.Set<UUID> struck = new java.util.HashSet<>();

	/**
	 * How long he has been unable to see them, and why hiding has to work.
	 *
	 * He is faster than a sprint, so running is not an escape and was never
	 * going to be. That leaves exactly one thing the player can do, and it had
	 * better be a real answer: get out of sight and stay out of it. Eight
	 * seconds blind and he loses the trail.
	 *
	 * Ticked only while he is NOT digging. A player sealed behind stone has not
	 * escaped him, they have delayed him, and the difference matters — the wall
	 * coming apart is him still on you. But a player who went round a corner,
	 * or underwater, or down a hole, has actually broken it, and that deserves
	 * to work.
	 */
	private int blindTicks;
	private static final int LOSE_TRAIL = 160;

	/** He stops, and lets them watch him stop. */
	private boolean relenting;
	private static final int RELENT_TICKS = 50;

	// ---- BREAKING IN ------------------------------------------------------
	/**
	 * A door is not an answer to this any more.
	 *
	 * This is the one place the mod knowingly breaks its own rule about never
	 * touching a player's build (DESIGN.md §9), and the exception is narrow and
	 * deliberate: shelter is the correct answer to almost everything he does,
	 * and at HUNTER it has to stop being one. A pursuer that gives up at a
	 * wooden door is not a pursuer, it is weather.
	 *
	 * THE RULE IS BENT, NOT ABANDONED. Every block he takes out is DROPPED, so
	 * the player loses the wall and their evening and not one item. That is the
	 * same bargain the torches make, and it is what keeps this the wrong side
	 * of frightening rather than the wrong side of griefing.
	 *
	 * And he is slow about it on purpose. The whole value is watching it
	 * happen — hearing the axe go into the door twice while you decide whether
	 * the back window is a better idea. Something that deletes a wall instantly
	 * is a cutscene; something taking eleven seconds through obsidian is a
	 * decision you are being given time to make.
	 */
	private @org.jspecify.annotations.Nullable BlockPos breaking;
	private int breakTicks;
	private int breakNeeds;

	/** Ticks per point of hardness. Wood is about a second, stone under one. */
	private static final float HARDNESS_TICKS = 12.0F;
	private static final int BREAK_MIN = 18;
	private static final int BREAK_MAX = 220;
	/**
	 * He only reaches for a tool once walking has demonstrably failed.
	 *
	 * Triggering on "something is in the way" would have him mining hillsides
	 * across the countryside, because at forty blocks there is nearly always
	 * terrain on the sightline. Triggering on a stall means he digs exactly
	 * when a player has done the thing this exists to answer — shut a door.
	 */
	private static final int BREAK_AFTER = 25;
	private static final double BREAK_RANGE = 16.0;
	// ---- END BREAKING IN --------------------------------------------------

	/** Two hearts, and not oftener than once a second. */
	private static final float STRIKE_DAMAGE = 4.0F;
	/**
	 * What he hits for once he can be hit back, and it goes THROUGH armour.
	 *
	 * Four damage is two hearts to somebody in a shirt and about half of one to
	 * somebody in enchanted netherite, which would have made the last fight in
	 * the mod the easiest thing in it: the player who did the work to get here
	 * is precisely the player it stops threatening. Scaling the number instead
	 * only moves the problem — it would then flatten anyone who arrived in iron.
	 *
	 * So the damage type ignores armour AND enchantments, declared properly in
	 * data/minecraft/tags/damage_type rather than borrowed from magic(). Eight
	 * is eight whatever they are wearing, which makes the fight about the same
	 * thing for everybody: not being hit. Break his line, use the gaps, do not
	 * stand there. Armour buys nothing here and it is not supposed to.
	 */
	private static final float RECKONING_DAMAGE = 8.0F;

	/** Declared in data/herobrine/damage_type/reckoning.json. */
	private static final net.minecraft.resources.ResourceKey<
			net.minecraft.world.damagesource.DamageType> RECKONING =
		net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.DAMAGE_TYPE,
			HerobrineMod.id("reckoning"));
	private static final int STRIKE_COOLDOWN = 30;
	/** Where he goes the instant a blow lands. Out of sight, not far. */
	private static final double HIT_BACKOFF_NEAR = 12.0;
	private static final double HIT_BACKOFF_FAR = 22.0;
	/**
	 * Never Long.MIN_VALUE, and this is why he never once hit anybody.
	 *
	 * The guard was `now - lastStruck < STRIKE_COOLDOWN`, and with a sentinel
	 * of Long.MIN_VALUE that subtraction OVERFLOWS: a game time of twelve
	 * thousand minus the most negative long wraps round to about negative nine
	 * quintillion, which is comfortably less than twenty-two. So the cooldown
	 * reported itself as still running on the very first swing, returned early,
	 * and never assigned lastStruck — leaving it wrong forever. He walked up to
	 * players and stood there for three rounds of testing because of a sentinel
	 * value.
	 *
	 * A small negative works because game time only ever counts up from zero,
	 * so nothing here can overflow. The comparison is written as an addition
	 * now as well, which cannot wrap at all.
	 */
	private long lastStruck = -1000L;

	private boolean hunting;
	private int stuckTicks;
	private double lastDistance = Double.MAX_VALUE;

	public void beginHunt() {
		this.hunting = true;
		this.moodTicks = chaseSpell();
	}

	/**
	 * Who he is going for, out of everyone in range.
	 *
	 * Nearest of the ones he has NOT reached yet. Nearest matters because he
	 * should not walk past somebody to get to a person on the far hill;
	 * not-yet-reached matters because otherwise the fastest player in a group
	 * could keep drawing him off and nobody else would ever be touched.
	 *
	 * @return null when everyone present has been reached, which ends the round
	 */
	private @org.jspecify.annotations.Nullable Player pickQuarry(List<Player> watchers) {
		Player best = null;
		double nearest = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			if (this.struck.contains(watcher.getUUID())) {
				continue;
			}
			double distance = this.distanceTo(watcher);
			if (distance < nearest) {
				nearest = distance;
				best = watcher;
			}
		}
		return best;
	}

	/**
	 * Everybody here has been reached. That is one round.
	 *
	 * The round is the unit the hunt is counted in rather than the individual
	 * blow, and that is what makes it scale: alone you are reached three times
	 * across a hunt, and in a party of four everybody is reached three times.
	 * The pressure per person is the same either way, so a group cannot dilute
	 * him simply by being a group.
	 */
	private void roundOver(@org.jspecify.annotations.Nullable Player anybody) {
		this.struck.clear();
		this.breakOffs++;
		HerobrineMod.LOGGER.info("hunt: round {} of {} — everyone here has been reached",
			this.breakOffs, MAX_BREAK_OFFS);
		if (anybody == null) {
			this.vanish("hunt: nobody left to follow");
			return;
		}
		if (this.breakOffs >= MAX_BREAK_OFFS) {
			this.relent(anybody);
			return;
		}
		if (reappearAt(anybody, WATCH_NEAR, WATCH_FAR, true)) {
			this.watching = true;
			this.moodTicks = watchSpell();
		}
	}

	/**
	 * How long he comes at you before breaking off.
	 *
	 * Shorter each time round, so the rhythm accelerates: a long first run that
	 * gives the player time to work out what is happening, then progressively
	 * less room to think in.
	 */
	private int chaseSpell() {
		return Math.max(70, 200 - this.breakOffs * 45) + this.random.nextInt(90);
	}

	/** And how long he stands and watches before coming back. 3–5 seconds. */
	private int watchSpell() {
		return 60 + this.random.nextInt(50);
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
	 * What surviving a hunt costs you, and it is the largest number here.
	 *
	 * This is the whole engine of the mod stated in one constant. You cannot
	 * kill him, so the only thing you can do to a hunt is outlast it — and
	 * outlasting it is the loudest defiance available, so it brings him on.
	 * HUNTER is a thousand and SIEGE is eighteen hundred, which is six or seven
	 * survived hunts: enough that the ladder is felt rather than climbed in an
	 * evening.
	 *
	 * Enduring it is worth more than slipping it. A player who hid in a hole
	 * until he lost interest has done something cleverer and less defiant than
	 * one who was reached three times and was still standing, and the numbers
	 * should say which of those he minds more.
	 */
	// ---- THE RECKONING ----------------------------------------------------
	/**
	 * How many blows it takes, and why it is counted in blows.
	 *
	 * Damage would make this fight a different length for every player: a
	 * netherite axe would end it in four swings and a stone sword would take
	 * thirty, and every scripted beat in between would land in the wrong place
	 * or not at all. Counting hits means the fight has the SHAPE it was written
	 * with — the tenth blow is the tenth blow for everybody.
	 *
	 * It also removes the incentive to spend an hour on gear before starting.
	 * What decides this is whether the player can survive thirty exchanges,
	 * which is a question about them rather than about their inventory.
	 *
	 * Ten is the marker, not the total. Three acts of ten: he gets angrier, then
	 * the church arrives and tells them what they have done, then it gets much
	 * worse.
	 */
	private static final int TOTAL_HITS = 30;
	public static final int THE_WARNING = 10;

	private int hits;
	// ---- END THE RECKONING ------------------------------------------------

	private static final int DEFIANCE_ENDURED = 130;
	private static final int DEFIANCE_EVADED = 55;

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

	/**
	 * The head does not obey the neck.
	 *
	 * Seventy-five degrees is the vanilla limit and it is a limit about
	 * anatomy — past it, a mob's head snaps back to the body. That is correct
	 * for a cow and wrong for this: the image the whole hunt is built around is
	 * a figure walking one way with its face still pointed at you, and at
	 * seventy-five it gives up and looks where it is going like anything else.
	 *
	 * A hundred and fifty is well past where a neck stops. It is meant to be.
	 */
	@Override
	public int getMaxHeadYRot() {
		return 150;
	}

	/**
	 * And it tracks fast enough to stay there.
	 *
	 * Ten degrees a tick is a slow, natural swivel that visibly lags behind a
	 * player circling him, which reads as him losing track. Forty keeps him
	 * locked on through anything short of a sprint around him.
	 */
	@Override
	public int getHeadRotSpeed() {
		return 40;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, TOTAL_HITS)
			// createMobAttributes does NOT include ATTACK_DAMAGE — it is
			// LivingEntity's set plus FOLLOW_RANGE and nothing else — so
			// doHurtTarget would have read the bare default and swung for
			// nothing. Monsters get this from createMonsterAttributes; he does
			// not extend Monster, so he has to ask for it.
			.add(Attributes.ATTACK_DAMAGE, STRIKE_DAMAGE)
			.add(Attributes.ATTACK_KNOCKBACK, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			// He needs to be aware of you from much further than he ever
			// approaches — the whole behaviour is about distance.
			.add(Attributes.FOLLOW_RANGE, 96.0)
			// He swims like he walks.
			//
			// travelInWater accelerates at a flat 0.02 unless this attribute
			// says otherwise, which is why an unmodified mob crossing a river
			// looks like it is wading through setting concrete. At 1.0 the
			// acceleration term becomes his actual movement speed — the same
			// mechanism Depth Strider uses — so a lake stops being a moat.
			.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
			// A full block, and then some, taken in his stride.
			//
			// Vanilla is 0.6, which is a slab — so a single block of terrain
			// made him stop and jump like anything else, and a fence line or a
			// stepped hillside broke the walk into a series of hops. At 1.6 he
			// comes up a block without altering his pace at all, which is much
			// worse to watch than a thing that has to climb.
			.add(Attributes.STEP_HEIGHT, 1.6);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.carry(Items.DIAMOND_AXE);
		// Nothing he carries is ever left on the ground. He does not die — he
		// is invulnerable and discards himself — but a guaranteed-drop slot
		// would hand a player a free diamond axe the first time anything else
		// managed to remove him.
		this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
		// Water does not stop him.
		//
		// Ground navigation treats it as something to path AROUND, so a river
		// between him and the player turned a pursuit into a figure jogging up
		// and down the bank. Both halves are needed: setCanFloat keeps him
		// swimming instead of sinking, and zeroing the malus stops the path
		// finder pricing water as a thing to avoid in the first place.
		this.getNavigation().setCanFloat(true);
		// And the pathfinder routes THROUGH doorways rather than treating them
		// as wall, so he walks in rather than only ever arriving at one.
		this.getNavigation().setCanOpenDoors(true);
		this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
		this.setPathfindingMalus(
			net.minecraft.world.level.pathfinder.PathType.WATER_BORDER, 0.0F);
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

		// On a hunt he is not simply going for whoever is closest. He has
		// somebody in mind, and he keeps them in mind until he has reached
		// them.
		if (this.hunting && !this.relenting) {
			Player next = this.pickQuarry(watchers);
			if (next == null) {
				this.roundOver(closest);
				return;
			}
			closest = next;
			closestDistance = this.distanceTo(next);
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
		this.scorch(level, 4 + this.random.nextInt(3));
	}

	/**
	 * @param wanted how many to try for. Every one of them still has to pass
	 *               safeToBurn, so this is an intention rather than a promise —
	 *               swinging at him inside a wooden house leaves nothing at all,
	 *               which is exactly right.
	 */
	private void scorch(ServerLevel level, int wanted) {
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
		// The outrun check is skipped while he is watching, because he chose
		// that distance himself and it would be absurd for him to give up on
		// account of it.
		if (!this.watching && distance > OUTRUN) {
			this.vanish("hunt: outrun");
			return;
		}

		// Has he lost them?
		if (this.hasLineOfSight(quarry) || this.breaking != null) {
			this.blindTicks = 0;
		} else if (++this.blindTicks > LOSE_TRAIL) {
			// Slipped rather than endured, and worth less accordingly.
			this.relent(quarry, DEFIANCE_EVADED);
		}

		if (this.relenting) {
			this.getNavigation().stop();
			this.setDeltaMovement(Vec3.ZERO);
			this.faceOneOf(java.util.List.of(quarry));
			if (--this.moodTicks <= 0) {
				this.vanish("hunt: he stopped");
			}
			return;
		}

		if (this.watching) {
			this.watch(quarry);
			return;
		}

		// ONLY the head. Setting yRot and yBodyRot as well was what made him
		// crab sideways across the field: the body was pinned at the player
		// while the navigation pushed him along a path going somewhere else,
		// so he slid rather than walked and every step animated wrong.
		//
		// Left alone, LivingEntity turns the body to follow the path by itself,
		// which is what a walk cycle needs. The head is the only thing that has
		// to disobey — and getMaxHeadYRot below is what lets it keep you while
		// the body goes past.
		this.getLookControl().setLookAt(quarry, 90.0F, 90.0F);

		// And a shove while he is actually in it. The attribute fixes the
		// acceleration but travelInWater halves it again the moment he is off
		// the bottom, which is most of any real crossing.
		if (this.isInWater()) {
			Vec3 swim = new Vec3(quarry.getX() - this.getX(), 0.0,
				quarry.getZ() - this.getZ());
			if (swim.lengthSqr() > 1.0E-4) {
				swim = swim.normalize().scale(0.09);
				this.setDeltaMovement(this.getDeltaMovement().add(swim.x, 0.012, swim.z));
			}
		}

		// Already over it.
		if (this.flying) {
			this.glide(quarry);
			return;
		}

		// Or already through it.
		if (this.level() instanceof ServerLevel here) {
			boolean started = this.breaking != null && breakable(here, this.breaking);
			if (started || (this.stuckTicks > BREAK_AFTER && distance < BREAK_RANGE)) {
				BlockPos wall = started ? this.breaking : blockingBetween(quarry);
				if (wall != null) {
					this.breakThrough(wall);
					return;
				}
				this.stopBreaking(here);
			}
		}

		// What is in the way, and is it worth leaving the ground for?
		//
		// Measured rather than inferred from a failed path, because by the time
		// the navigator has given up the player has already watched him stand
		// at a wall looking stupid, and that is the moment the whole thing
		// stops working.
		int wall = wallAhead(quarry);
		if (wall > 0 && this.onGround()) {
			if (wall <= VAULT_MAX) {
				this.vault(quarry, wall);
			} else {
				this.takeOff();
			}
			return;
		}

		this.getNavigation().moveTo(quarry, HUNT_SPEED);

		// Is he actually getting anywhere? Measured on distance to the player
		// rather than on distance travelled, because a mob happily jogging back
		// and forth along the near side of a ravine is moving the whole time
		// and getting nowhere, and only one of those two numbers notices.
		if (distance < this.lastDistance - 0.05) {
			this.stuckTicks = 0;
		} else if (++this.stuckTicks > STUCK_LIMIT) {
			this.stuckTicks = 0;
			// Beaten by the terrain rather than by a wall — a ravine, a lake
			// edge, a path that loops. Going over it is better than vanishing,
			// because the player gets to SEE him solve it, and a pursuer you
			// watched come over the ridge is worse than one that was simply
			// closer when you looked again.
			this.takeOff();
		}
		this.lastDistance = distance;

		// Long enough at this one. He gives them a moment, and it costs him
		// nothing — this is a BREATHER, not a round.
		//
		// Rounds are counted by roundOver, when everybody present has actually
		// been reached, and only there. Counting them here as well meant a
		// chase that never landed a blow could still burn through all three and
		// end a hunt in which nothing had happened to anybody.
		if (--this.moodTicks <= 0) {
			if (reappearAt(quarry, WATCH_NEAR, WATCH_FAR, true)) {
				this.watching = true;
				this.moodTicks = watchSpell();
				HerobrineMod.LOGGER.info("hunt: paused, watching from {} blocks",
					String.format("%.0f", this.distanceTo(quarry)));
			} else {
				// Nowhere to stand and be seen from. He simply keeps coming,
				// which is the right failure: the alternative is him blinking
				// out for no reason the player can perceive.
				this.moodTicks = chaseSpell();
			}
		}
	}

	/**
	 * He stops, and they get to see him stop.
	 *
	 * A hunt that ends by the pursuer quietly ceasing to exist somewhere behind
	 * you does not end at all — the player keeps checking over their shoulder
	 * for the next ten minutes, which sounds like a triumph and is actually the
	 * event failing to resolve. So the last beat is deliberate and legible: he
	 * stops dead, in the open, and looks at them for two and a half seconds
	 * while doing nothing whatever. Then he goes.
	 *
	 * That pause is the only full stop this phase has. It is also the thing
	 * that makes the NEXT hunt frightening, because they now know what it looks
	 * like when he is finished, and they will be waiting for it.
	 */
	private void relent(Player quarry) {
		this.relent(quarry, DEFIANCE_ENDURED);
	}

	private void relent(Player quarry, int defiance) {
		// AND IT COSTS THEM.
		//
		// Everyone still here, not only the quarry — surviving a hunt as a
		// group is a group's defiance, and paying it to one of them would make
		// standing near the others free.
		if (this.level() instanceof ServerLevel here) {
			for (ServerPlayer survivor : here.getEntitiesOfClass(ServerPlayer.class,
					this.getBoundingBox().inflate(WATCH_RANGE),
					other -> other.isAlive() && !other.isSpectator())) {
				WrathTriggers.defiance(survivor, defiance);
			}
		}
		this.relenting = true;
		this.watching = false;
		this.moodTicks = RELENT_TICKS;
		this.getNavigation().stop();
		HerobrineMod.LOGGER.info("hunt: done after {} ticks and {} break-offs",
			this.age, this.breakOffs);
	}

	/**
	 * Standing off, watching, doing nothing at all.
	 *
	 * The whole value of this is that it is a PAUSE in something that was
	 * frightening because it would not stop. He is visible, he is a long way
	 * off, and he is not approaching — which gives the player just long enough
	 * to think it might be over.
	 */
	private void watch(Player quarry) {
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		float yaw = (float)(net.minecraft.util.Mth.atan2(
			quarry.getZ() - this.getZ(), quarry.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.setYBodyRot(yaw);

		if (--this.moodTicks > 0) {
			return;
		}
		this.watching = false;
		this.moodTicks = chaseSpell();
		// And then he is close. Out of their view for the move itself, because
		// the oldest rule in the mod is that he is never seen arriving — they
		// look back at where he was standing and he is not there any more.
		// Tighter every time. The third return starts about where the first one
		// ended, which is the whole reason for counting them.
		double squeeze = 2.5 * this.breakOffs;
		if (reappearAt(quarry, Math.max(6.0, RUSH_NEAR - squeeze),
				Math.max(8.0, RUSH_FAR - squeeze), false)) {
			HerobrineMod.LOGGER.info("hunt: back in at {} blocks",
				String.format("%.0f", this.distanceTo(quarry)));
		}
		this.lastDistance = Double.MAX_VALUE;
		this.stuckTicks = 0;
	}

	/**
	 * The first thing between his eye and theirs, if anything is.
	 *
	 * Uses the sightline rather than the navigator, and that is the point: a
	 * player standing in a sealed room produces a perfectly happy path right up
	 * to the outside of the wall, so asking the pathfinder never reveals that
	 * they are enclosed. Asking what is in the way does.
	 */
	private @org.jspecify.annotations.Nullable BlockPos blockingBetween(Player quarry) {
		if (!(this.level() instanceof ServerLevel here)) {
			return null;
		}
		net.minecraft.world.phys.HitResult hit = here.clip(
			new net.minecraft.world.level.ClipContext(this.getEyePosition(),
				quarry.getEyePosition(),
				net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE, this));
		if (!(hit instanceof net.minecraft.world.phys.BlockHitResult block)
			|| hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
			return null;
		}
		return breakable(here, block.getBlockPos()) ? block.getBlockPos() : null;
	}

	/**
	 * Is this something he is willing to take out?
	 *
	 * Indestructible blocks are refused outright rather than attempted slowly,
	 * because a figure standing at bedrock swinging forever is the single most
	 * ridiculous thing this mod could show anybody. Containers are refused too:
	 * he is coming through the wall, not through the chest, and breaking one
	 * would scatter a player's belongings across the floor — which is the exact
	 * line the dropped blocks are drawn to avoid crossing.
	 */
	private static boolean breakable(ServerLevel level, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
			return false;
		}
		return !(level.getBlockEntity(pos)
			instanceof net.minecraft.world.Container);
	}

	/**
	 * Go through it, with the right tool and in full view.
	 *
	 * The tool is chosen from the block's own mineable tag rather than from a
	 * list of blocks, so it is right for anything the game or another mod adds,
	 * and it is put in his HAND — the player should be able to see the axe
	 * before they hear it. destroyBlockProgress sends the cracking overlay to
	 * everybody nearby, which is the whole performance: they watch the block
	 * fail in ten visible stages and get to decide what to do about it.
	 */
	private void breakThrough(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		// If it opens, he opens it. Chopping through a door he could have
		// simply pushed is the sort of thing that makes a frightening thing
		// look stupid, and a door swinging open on its own is worse than a door
		// being destroyed anyway — one of those is somebody coming in, and the
		// other is only weather with an axe.
		if (this.openInstead(here, pos)) {
			this.stopBreaking(here);
			return;
		}
		if (!pos.equals(this.breaking)) {
			this.stopBreaking(here);
			this.breaking = pos;
			this.breakTicks = 0;
			float hardness = here.getBlockState(pos).getDestroySpeed(here, pos);
			this.breakNeeds = net.minecraft.util.Mth.clamp(
				Math.round(hardness * HARDNESS_TICKS), BREAK_MIN, BREAK_MAX);
			this.carry(toolFor(here.getBlockState(pos)));
		}

		this.getNavigation().stop();
		this.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

		this.breakTicks++;
		if (this.breakTicks % 6 == 0) {
			this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			here.playSound(null, pos, here.getBlockState(pos).getSoundType().getHitSound(),
				net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 0.85F);
		}
		here.destroyBlockProgress(this.getId(), pos,
			Math.min(9, this.breakTicks * 10 / Math.max(1, this.breakNeeds)));

		if (this.breakTicks >= this.breakNeeds) {
			// Dropped, always. He takes the wall, never the materials.
			here.destroyBlock(pos, true, this);
			this.stopBreaking(here);
		}
	}

	/**
	 * Push it, if it is the kind of thing that pushes.
	 *
	 * Iron is the exception on purpose, and it is the same sentence the cells
	 * downstairs are written in: iron is what holds. canOpenByHand comes off
	 * the block's own BlockSetType rather than a hardcoded list, so it is right
	 * for every wood in the game and for any a mod adds. An iron door still
	 * stops him — he has to cut it, which at hardness five is three seconds of
	 * standing there doing it. That is not an obstacle so much as a receipt for
	 * having built properly.
	 *
	 * @return true if it is open, or has just been opened, and there is nothing
	 *         left here to break
	 */
	private boolean openInstead(ServerLevel here, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = here.getBlockState(pos);
		if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door) {
			if (!door.type().canOpenByHand()) {
				return false;   // iron. He cuts it instead.
			}
			if (!state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) {
				door.setOpen(this, here, state, pos, true);
			}
			return true;
		}
		// Trapdoors and gates have no accessible type() from outside, so the
		// tags carry it instead. WOODEN_TRAPDOORS excludes the iron one by
		// construction, which is the same iron rule the doors follow.
		if (state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)) {
			return this.swingOpen(here, state, pos,
				net.minecraft.world.level.block.TrapDoorBlock.OPEN,
				SoundEvents.WOODEN_TRAPDOOR_OPEN);
		}
		if (state.is(net.minecraft.tags.BlockTags.FENCE_GATES)) {
			return this.swingOpen(here, state, pos,
				net.minecraft.world.level.block.FenceGateBlock.OPEN,
				SoundEvents.FENCE_GATE_OPEN);
		}
		return false;
	}

	private boolean swingOpen(ServerLevel here,
			net.minecraft.world.level.block.state.BlockState state, BlockPos pos,
			net.minecraft.world.level.block.state.properties.BooleanProperty open,
			net.minecraft.sounds.SoundEvent sound) {
		if (!state.getValue(open)) {
			here.setBlock(pos, state.setValue(open, true), 10);
			here.playSound(null, pos, sound, net.minecraft.sounds.SoundSource.BLOCKS,
				1.0F, here.getRandom().nextFloat() * 0.1F + 0.9F);
		}
		return true;
	}

	private void stopBreaking(ServerLevel here) {
		if (this.breaking != null) {
			here.destroyBlockProgress(this.getId(), this.breaking, -1);
			this.breaking = null;
			// Back to the axe, so he is never seen walking about with a shovel.
			this.carry(Items.DIAMOND_AXE);
		}
		this.breakTicks = 0;
	}

	/**
	 * What he is carrying, and it is never nothing.
	 *
	 * Empty hands make him look like he is out for a walk. The axe is the
	 * default because it reads as a weapon at a distance and as a tool up
	 * close, which is exactly what he uses it for.
	 *
	 * COSMETIC, DELIBERATELY. An item in a mob's main hand applies its own
	 * attribute modifiers, so handing him a diamond axe would silently take him
	 * from four damage to thirteen and every number tuned above would be a
	 * lie — and it would move again the moment the tool swapped to a pickaxe.
	 * Clearing ATTRIBUTE_MODIFIERS makes the thing purely something he is
	 * holding, so what he hits for is what STRIKE_DAMAGE says and nothing else.
	 */
	private void carry(net.minecraft.world.item.Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
			net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
		this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack);
	}

	private static net.minecraft.world.item.Item toolFor(
			net.minecraft.world.level.block.state.BlockState state) {
		if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)) {
			return Items.DIAMOND_AXE;
		}
		if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
			return Items.DIAMOND_SHOVEL;
		}
		return Items.DIAMOND_PICKAXE;
	}

	/**
	 * How tall the thing directly in his way is, in blocks.
	 *
	 * Looks at the column one step along the line to the player and counts the
	 * solid blocks stacked from his own feet upward. Zero means the way is
	 * clear and he should simply walk.
	 */
	private int wallAhead(Player quarry) {
		Vec3 flat = new Vec3(quarry.getX() - this.getX(), 0.0, quarry.getZ() - this.getZ());
		if (flat.lengthSqr() < 1.0E-4) {
			return 0;
		}
		Vec3 step = flat.normalize();
		BlockPos ahead = BlockPos.containing(
			this.getX() + step.x, this.getY(), this.getZ() + step.z);
		int height = 0;
		while (height < SCAN && this.level().getBlockState(ahead.above(height)).blocksMotion()) {
			height++;
		}
		// A block he can simply step onto is not an obstacle at all — the raised
		// STEP_HEIGHT swallows it, and treating it as one would have him
		// hopping over every kerb.
		return height <= 1 ? 0 : height;
	}

	/**
	 * Over it, in one movement.
	 *
	 * The impulse is worked out from the height rather than fixed, because a
	 * jump tuned for a fence looks feeble at a four-block cliff and one tuned
	 * for the cliff sends him sailing over a fence. Vanilla's 0.42 clears about
	 * a block and a quarter and height goes as the square of the launch speed,
	 * so the rest is arithmetic — plus a tenth for the margin, since falling
	 * just short of the ledge is the one outcome that looks broken.
	 */
	private void vault(Player quarry, int height) {
		Vec3 flat = new Vec3(quarry.getX() - this.getX(), 0.0, quarry.getZ() - this.getZ())
			.normalize();
		double lift = 0.42 * Math.sqrt(height / 1.25) * 1.1;
		this.setDeltaMovement(flat.x * 0.34, lift, flat.z * 0.34);
		// 26.2 has no hasImpulse; hurtMarked is what forces the velocity down
		// to the client now. Without it the server knows he jumped and the
		// player watches him slide up the wall.
		this.hurtMarked = true;
	}

	private void takeOff() {
		if (this.flying) {
			return;
		}
		this.flying = true;
		this.flyTicks = 0;
		this.setNoGravity(true);
		HerobrineMod.LOGGER.info("hunt: going over");
	}

	/**
	 * Over the top of whatever it was.
	 *
	 * Moved by position rather than by velocity, the same way fleeing is, and
	 * for the same reason: something being pathed can be cornered, and being
	 * cornered forces the honest answer about what he is. It also means he goes
	 * straight over a mountain rather than around its shoulder.
	 *
	 * Strictly a way PAST something. He comes down as soon as there is ground
	 * to come down on, and FLY_LIMIT ends it regardless — a Herobrine who
	 * simply flies everywhere is a different and much sillier character.
	 */
	private void glide(Player quarry) {
		Vec3 flat = new Vec3(quarry.getX() - this.getX(), 0.0, quarry.getZ() - this.getZ());
		double away = flat.length();

		// High enough to clear the ground at both ends, which is the cheap
		// approximation of clearing everything between.
		double ceiling = Math.max(this.getY(), quarry.getY()) + 3.0;
		double y = this.getY() < ceiling
			? Math.min(ceiling, this.getY() + CLIMB_RATE)
			: this.getY();

		Vec3 step = away < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(FLY_SPEED);
		this.snapTo(this.getX() + step.x, y, this.getZ() + step.z, this.getYRot(), 0.0F);
		this.setDeltaMovement(Vec3.ZERO);

		boolean overhead = this.getY() - quarry.getY() > 1.0;
		if ((away < 2.5 && !overhead) || ++this.flyTicks > FLY_LIMIT) {
			this.land();
		}
	}

	private void land() {
		this.flying = false;
		this.setNoGravity(false);
		this.fallDistance = 0.0;
		this.lastDistance = Double.MAX_VALUE;
		this.stuckTicks = 0;
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
		return reappearAt(quarry, 12.0, 24.0, false);
	}

	/**
	 * Be somewhere else, chosen rather than stumbled into.
	 *
	 * @param wantSeen true when the point is to be LOOKED at — the standing-off
	 *                 half of a hunt only works if the player actually finds
	 *                 him out there; false when he is coming back in, because
	 *                 the oldest rule in the mod is that he is never seen
	 *                 arriving.
	 */
	private boolean reappearAt(Player quarry, double min, double max, boolean wantSeen) {
		if (!(this.level() instanceof ServerLevel here)) {
			return false;
		}
		// Whatever he was doing, he is on the ground where he turns up, and not
		// still credited with a block he has walked away from.
		if (this.flying) {
			this.land();
		}
		this.stopBreaking(here);
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = min + this.random.nextDouble() * (max - min);
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
			boolean inFront = look.dot(toSpot) > (wantSeen ? 0.35 : 0.1);

			if (wantSeen) {
				// In front of them AND actually visible from where they stand.
				// A spot behind a hill satisfies the cone and wastes the whole
				// pause — they turn, see nothing, and decide it is over.
				if (!inFront || !clearTo(here, quarry, at)) {
					continue;
				}
			} else if (inFront) {
				continue;
			}

			this.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.getYRot(), 0.0F);
			this.lastDistance = Double.MAX_VALUE;
			return true;
		}
		return false;
	}

	/** Nothing solid between their eye and where his head would be. */
	private static boolean clearTo(ServerLevel level, Player quarry, BlockPos at) {
		Vec3 head = new Vec3(at.getX() + 0.5, at.getY() + 1.7, at.getZ() + 0.5);
		return level.clip(new net.minecraft.world.level.ClipContext(
			quarry.getEyePosition(), head,
			net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, quarry))
			.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
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
		// Feet down first. The standoff branch runs BEFORE the hunt branch, so
		// a player who lets him get within seven blocks while he is still over
		// the wall would take control away from glide() and leave him walking
		// on air with gravity switched off.
		if (this.flying) {
			this.land();
		}
		this.getLookControl().setLookAt(player, 90.0F, 90.0F);

		// AND HERE IS WHERE HE DIGS, which is why he never did.
		//
		// The breaking check lived in pursue() and pursue only runs beyond the
		// standoff. A player behind a wall two blocks away puts him INSIDE the
		// standoff, so he went to closeOn instead, where there was no breaking
		// code at all — and stuckTicks, which was the trigger, is only counted
		// in pursue and so never moved either. He shuffled at the wall for the
		// whole hunt.
		//
		// No line of sight at this range means a wall, not distance. That is a
		// better trigger than the stall was: it is the actual condition, rather
		// than a symptom of it.
		if (this.hunting && !this.hasLineOfSight(player)
			&& this.level() instanceof ServerLevel here) {
			BlockPos wall = this.breaking != null && breakable(here, this.breaking)
				? this.breaking : blockingBetween(player);
			if (wall != null) {
				this.breakThrough(wall);
				return;
			}
			this.stopBreaking(here);
		}

		if (distance > ARMS_LENGTH) {
			boolean routed = this.getNavigation()
				.moveTo(player, this.hunting ? HUNT_SPEED : ADVANCE_SPEED);

			// AND IF THE NAVIGATOR WILL NOT TAKE HIM, HE WALKS.
			//
			// This is why he stood at four blocks and never landed a blow.
			// moveTo returns false whenever createPath cannot route — the
			// player up a ladder, on a slab, over a fence, one block into a
			// doorway, standing anywhere the node graph does not like — and the
			// old code ignored the return value entirely. He would arrive
			// inside the standoff, the path would fail, and he would simply
			// stop: close enough to look menacing, never close enough to reach.
			//
			// At melee range pathfinding earns nothing anyway. There is no
			// route to plan across three blocks, so when it fails he is pushed
			// straight at the player instead. move() rather than setPos, so
			// walls still stop him and the raised STEP_HEIGHT still carries him
			// up a kerb — he closes the gap, he does not slide through the
			// world to do it.
			if (this.hunting && (!routed || this.getNavigation().isDone())) {
				Vec3 step = new Vec3(player.getX() - this.getX(), 0.0,
					player.getZ() - this.getZ());
				if (step.lengthSqr() > 1.0E-4) {
					this.move(net.minecraft.world.entity.MoverType.SELF,
						step.normalize().scale(0.16));
				}
			}
			return;
		}

		// A hunt does not end politely.
		//
		// Vanishing at arm's length is the right ending for a STARE — the
		// player walked him down and he refused them. It is the wrong ending
		// for something that has chased them across a field: a pursuer that
		// arrives and then tactfully disappears was never a pursuer, and reads
		// as the mod losing its nerve at the last moment.
		if (this.hunting) {
			this.strike(player);
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

	/**
	 * He reaches you.
	 *
	 * He is still invulnerable and still relocates the moment anybody swings
	 * back, so this does not make him a mob to be killed — the ending has to
	 * keep that. What it makes him is something with a cost attached, so
	 * standing your ground stops being free and running becomes a decision
	 * rather than a preference.
	 */
	private void strike(ServerPlayer player) {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		// NOT THROUGH A WALL.
		//
		// doHurtTarget does not test this and neither did anything here, so
		// standing on the far side of two blocks of stone was close enough to
		// be hit — which is the one thing guaranteed to read as broken rather
		// than as frightening. Vanilla melee goals check the same thing before
		// swinging; the difference is that they own the check and this had to
		// be given one.
		if (!this.hasLineOfSight(player)) {
			return;
		}
		long now = here.getGameTime();
		if (now < this.lastStruck + STRIKE_COOLDOWN) {
			return;
		}
		this.lastStruck = now;
		this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

		// ONE PERSON. Not a swipe that catches whoever is standing about.
		//
		// The vanilla path, the same one an iron golem and a wither skeleton
		// take. doHurtTarget reads ATTACK_DAMAGE, applies the knockback, plays
		// the sound and runs the post-attack effects — all of which the
		// hand-rolled hurtServer call skipped, so even once the cooldown was
		// fixed he would have been hitting for damage with no shove behind it.
		// At SIEGE he stops caring what they are wearing.
		boolean landed;
		if (Wrath.phase(here.getServer()) == Phase.SIEGE) {
			landed = player.hurtServer(here,
				new net.minecraft.world.damagesource.DamageSource(
					here.registryAccess()
						.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
						.getOrThrow(RECKONING), this),
				RECKONING_DAMAGE);
		} else {
			landed = this.doHurtTarget(here, player);
		}

		// AND THEN HE IS NOT THERE. He does not stand and trade.
		//
		// This is what makes the phase survivable without slowing him down. He
		// is faster than a sprint by design, so once he arrives the player
		// cannot leave — and a thing that is faster than you AND stays on you
		// is not frightening, it is arithmetic, and the arithmetic says you die
		// in nine seconds every time.
		//
		// So each blow is its own event. He hits once, he is gone before the
		// screen has stopped shaking, and he comes back from somewhere else.
		// The player takes four or five hits across a whole hunt instead of
		// twelve in a row, every one of them lands as a scare rather than as a
		// tick of damage, and the gaps are where they get to do something about
		// it: run, eat, climb, shut a door.
		if (landed) {
			// AND NOW IT IS SOMEBODY ELSE'S TURN.
			//
			// He is finished with this one. Marking them means the next quarry
			// is chosen from whoever has NOT been reached yet, so he works
			// through a group deliberately rather than staying on whoever
			// happens to be nearest — which would let a fast player draw him
			// off their whole party indefinitely.
			//
			// It is also much worse to be on the receiving end of. Being chased
			// is frightening; watching him finish with your friend and turn
			// toward you, and knowing he is going to get to everybody, is a
			// different thing entirely.
			this.struck.add(player.getUUID());
			if (reappearAt(player, HIT_BACKOFF_NEAR, HIT_BACKOFF_FAR, false)) {
				this.watching = true;
				this.moodTicks = 30 + this.random.nextInt(25);
			}
		}
		// Logged with the answer, not just the attempt. "He is not hitting me"
		// has two completely different causes — he never got in range, or he
		// swung and the damage was refused (creative, invulnerable, a totem) —
		// and they are indistinguishable from the outside.
		HerobrineMod.LOGGER.info("hunt: swung at {} blocks from {}, landed={}",
			String.format("%.1f", this.distanceTo(player)),
			player.getName().getString(), landed);
	}

	private void vanish(String why) {
		HerobrineMod.LOGGER.info("stare over after {} ticks: {}", this.age, why);
		// Otherwise the half-cracked block keeps its overlay for as long as the
		// chunk stays loaded, which is a very odd souvenir to leave behind.
		if (this.level() instanceof ServerLevel clearing) {
			this.stopBreaking(clearing);
		}
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
		// UNTIL SIEGE, AND ONLY UNTIL SIEGE.
		//
		// Five phases of a thing that cannot be touched is what gives the sixth
		// its weight. A player who has spent forty hours learning that swinging
		// at him does nothing, and then feels a sword actually connect, has been
		// told something no message box could tell them.
		//
		// Environmental damage stays off permanently. He is not to be finished
		// by a cactus, a fall or somebody's lava bucket — this ends with a
		// player hitting him or it does not end.
		if (!(source.getEntity() instanceof ServerPlayer)) {
			return true;
		}
		return Wrath.phase(level.getServer()) != Phase.SIEGE;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// THE RECKONING. He is being killed, and every blow counts the same.
		//
		// The incoming number is thrown away on purpose — see TOTAL_HITS. What
		// a player is swinging decides how the fight LOOKS, never how long it
		// lasts, so the tenth blow is the tenth blow whether it came from a
		// stone sword or a netherite axe.
		if (source.getEntity() instanceof ServerPlayer striker
			&& Wrath.phase(level.getServer()) == Phase.SIEGE) {
			return this.takeTheBlow(level, source, striker);
		}

		if (source.getEntity() instanceof ServerPlayer attacker) {
			// Swinging at him is the loudest possible defiance.
			WrathTriggers.defiance(attacker, DEFIANCE_STRUCK);
			// Something is left standing where he was.
			//
			// Swinging at him and having him simply not be there is the correct
			// answer and a slightly empty one — the player gets no
			// acknowledgement that anything happened, and a hit that reads as
			// nothing reads as a bug. Three fires on the spot he was occupying
			// says the swing landed on something, without conceding that it
			// hurt him.
			//
			// Same safeguards as the trespasser scorch, which is why it reuses
			// it rather than lighting its own: never within two blocks of
			// anything flammable, never on burnable ground, and gone after six
			// seconds whatever happens. Take a swing at him indoors and there
			// will be no fire at all, which is the right outcome.
			this.scorch(level, 3);

			// Mid-hunt he does not leave, he only gets out of reach.
			//
			// Fleeing on a hit would hand the player a way to end a hunt with
			// one swing, and would undo the entire point of the phase: the
			// thing that will not stop turning out to stop the moment you show
			// it a sword. He reappears behind them and keeps coming.
			if (this.hunting) {
				this.reappearNear(attacker);
				return false;
			}
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
	 * One blow of thirty.
	 *
	 * He does not relocate, does not flee and does not vanish. That is the
	 * whole difference between this and every other time a player has swung at
	 * him: for five phases the answer to a sword was that he was somewhere else
	 * by the time it arrived, and here he simply stands and takes it and gets
	 * worse.
	 */
	private boolean takeTheBlow(ServerLevel level, DamageSource source, ServerPlayer striker) {
		this.hits++;
		this.hunting = true;      // whatever he was doing, he is doing this now
		this.relenting = false;
		this.watching = false;
		this.struck.clear();

		WrathTriggers.defiance(striker, DEFIANCE_STRUCK);
		this.anger(level);

		if (this.hits >= TOTAL_HITS) {
			super.hurtServer(level, source, Float.MAX_VALUE);
			return true;
		}
		if (this.hits == THE_WARNING) {
			com.bloomlet.herobrine.manifest.Reckoning.theWarning(level, striker, this);
		}
		// One point of the health bar per blow, which is why MAX_HEALTH is the
		// hit count rather than a number of hearts. The bar is the honest
		// progress meter and it is the only one the player gets.
		this.setHealth(Math.max(1.0F, TOTAL_HITS - this.hits));
		this.hurtTime = 10;
		this.hurtDuration = 10;
		return true;
	}

	/**
	 * He gets worse, and it has to be visible without a new texture.
	 *
	 * The enderman note was the right reference: what makes one frightening
	 * when provoked is that it visibly changes while doing nothing else
	 * differently. So the escalation is particles and fire, both scaling with
	 * the count, because those cost nothing to add and — unlike a colour on the
	 * model — cannot crash a client if the mixin selector is wrong, which has
	 * already happened twice on this project.
	 */
	private void anger(ServerLevel level) {
		int stage = 1 + this.hits / THE_WARNING;
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			this.getX(), this.getY() + 1.1, this.getZ(),
			12 * stage, 0.45, 0.7, 0.45, 0.02);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
			this.getX(), this.getY() + 1.0, this.getZ(),
			4 * stage, 0.4, 0.6, 0.4, 0.01);
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.WARDEN_ANGRY, this.getSoundSource(), 1.0F, 0.6F + stage * 0.15F);
		// More of it every act, and still refused wherever it would spread.
		this.scorch(level, stage * 2);
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
