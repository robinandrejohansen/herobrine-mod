package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * The one who does not sleep.
 *
 * A villager who has gone wrong. Not possessed, not infected, not wearing
 * anybody's face — the lab register under the threshold is a list of villagers
 * BY TRADE, and this is what one of those entries looks like from the outside.
 *
 * ITS OWN MOB RATHER THAN A REAL VILLAGER, and that is not a shortcut. Two
 * separate rules in this repo both point the same way. DESIGN §9 and Villages
 * refuse to remove villagers, because deleting somebody's cleric to stage a
 * scare costs them hours of trading with no warning and no counter-play. And
 * InfectedEntity's whole comment is about what happened the last time this mod
 * reached into vanilla's villager renderer: two startup crashes and three wrong
 * diagnoses. So nothing that already existed is touched. There is simply one
 * more person in the village than there was.
 *
 * ---
 *
 * WHAT MAKES HIM WORK IS THAT HE IS ORDINARY. He has the villager's model, the
 * villager's walk, the villager's voice and the villager's clothes. In a crowd
 * at fifteen blocks there is nothing to see. The three tells are all things the
 * player has to be close enough, or patient enough, to notice:
 *
 *   THE EYES. A black pupil in the middle of the iris, which no villager in the
 *     game has. It is deliberately not emissive and deliberately not red —
 *     white is HIS and red is what a possessed animal wears when it is about to
 *     hurt you. This is a villager's own green eye with something behind it,
 *     and you have to be four blocks away to see it at all.
 *
 *   HE WILL NOT TRADE. No profession and no menu, so right-clicking him does
 *     nothing whatever. That is how anybody checks a villager is real, and it
 *     is the same tell the possessed villager already uses.
 *
 *   HE DOES NOT SLEEP. Night falls, the square empties, and one man is still
 *     standing in it. The contrast does all of it and it costs nothing to
 *     build, because he is not a Villager and has no bed behaviour to suppress.
 *
 * And by day he simply looks at you. Not approaching, not fleeing, not working
 * — the one villager in the village who stops what he is doing when you walk
 * past and turns to watch you go. He is completely harmless until dark.
 */
public class TurnedEntity extends PathfinderMob {

	/**
	 * How far he notices somebody, and it is the same by day and by night.
	 *
	 * What changes after dark is what he DOES about it, never whether he has
	 * seen them. A villager who only looks at you when it is convenient is a
	 * villager with a detection radius; one who has been watching you since you
	 * came over the bridge, and then the sun goes down, is a person.
	 */
	private static final double NOTICES = 20.0;

	/**
	 * Faster than a zombie, slower than a sprint, and that gap is the whole
	 * fight.
	 *
	 * Sprinting away works. Walking away does not. So being caught out at night
	 * is a decision — spend the hunger, or turn and deal with him — rather than
	 * a coin toss, and it is the same bargain the hunt makes, which means the
	 * player has already been taught how to read it.
	 */
	private static final double CHARGE_SPEED = 0.33;

	/**
	 * Once he has come for you, the morning does not save you.
	 *
	 * The day/night split is what the whole thing is built on, so it is
	 * tempting to have him simply stop at dawn — and that is the version that
	 * makes him a mechanic. Something that gives up on a schedule can be waited
	 * out, and a player who has worked out that they only have to survive until
	 * sunrise is playing a clock rather than running from somebody.
	 *
	 * So the night decides whether it STARTS. Nothing decides whether it stops
	 * except one of them going down.
	 */
	private boolean committed;

	// ---- HE WATCHES BEFORE HE COMES ----------------------------------------
	//
	// The old behaviour was one bit wide: he had a target or he did not, and
	// having one meant running at you with an axe. Which is a fine thing for a
	// village to contain one of and a terrible thing for it to contain forty of —
	// sixteen men sprinting out of sixteen doors is a wave, and a wave is a
	// difficulty setting rather than a fright.
	//
	// So there is a middle. He notices, and then he FOLLOWS at a distance, empty
	// handed, facing you the whole time. Nothing is happening and nothing is
	// going to happen, and the player has to decide what to do about that — which
	// is a far worse position to be in than being chased, because being chased has
	// an obvious correct answer and this has none.
	//
	// THE WHOLE STATE MACHINE IS getTarget() BEING NULL. MeleeAttackGoal cannot
	// run without a target, so while he is stalking there is nothing to suppress
	// and nothing to fight with — the stalk goal simply owns his feet. Setting the
	// target is what ends it, and from that instant the ordinary goal takes over
	// exactly as it always did. No flags, no priorities to balance, no third
	// version of "walk toward the player" to keep in step with the other two.

	/** How far off he holds while he is only watching. */
	private static final double STANDS_OFF = 7.0;
	/** Slack either side of it, so he is not oscillating on the spot. */
	private static final double SLACK = 1.5;
	/** Inside this he stops watching. */
	private static final double SNAPS_AT = 3.5;
	/** How long he keeps the mark after losing sight of them. */
	private static final int REMEMBERS = 400;
	/** Both of you motionless for this long and he takes a step. */
	private static final int CREEPS_AFTER = 60;
	/** How far a step is. */
	private static final double A_STEP = 1.4;
	/** How far a shout carries when somebody puts a sword in him. */
	private static final double SHOUT = 24.0;
	/** Under this much movement in a tick, both of you count as standing still. */
	private static final double STILL = 0.01;

	private java.util.@org.jspecify.annotations.Nullable UUID mark;
	private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos markAt;
	private int patience;
	private int stillFor;
	private double standing = STANDS_OFF;

	/** He has seen somebody. Not a target — a mark. */
	public void notice(Player who) {
		if (this.getTarget() != null) {
			return;      // already past watching
		}
		if (this.mark == null) {
			// Each of them picks his own distance, once, and keeps it. A row of
			// them all holding station at exactly seven blocks is a firing line;
			// six to nine, chosen per man, is a group of people watching you.
			this.standing = STANDS_OFF - 1.0 + this.random.nextDouble() * 3.0;
		}
		this.mark = who.getUUID();
		this.markAt = who.blockPosition();
		this.patience = REMEMBERS;
		this.empty();
	}

	/**
	 * AND NOW HE HAS IT OUT.
	 *
	 * @param shout whether the others should hear about it
	 *
	 * The axe appearing is the entire transition and it wants to be visible: the
	 * player has spent a minute being followed by somebody holding nothing, and
	 * the moment that changes they should be able to SEE that it changed rather
	 * than infer it from him moving faster.
	 */
	public void snap(Player at, boolean shout) {
		this.carry();
		this.setTarget(at);
		this.committed = true;
		this.mark = null;
		this.patience = 0;
		if (!shout || !(this.level() instanceof ServerLevel here)) {
			return;
		}
		// AND THEY ALL COME. Hitting one of them is the loudest thing a player can
		// do in that town, and it should cost accordingly — every one of them
		// within the shout drops the pretence at once. It also fixes the reading:
		// they are not forty individuals who happen to look alike, they are one
		// thing distributed across forty bodies.
		int woke = 0;
		for (TurnedEntity other : here.getEntitiesOfClass(TurnedEntity.class,
				this.getBoundingBox().inflate(SHOUT))) {
			if (other == this || other.getTarget() != null) {
				continue;
			}
			other.carry();
			other.setTarget(at);
			other.committed = true;
			other.mark = null;
			woke++;
		}
		if (woke > 0) {
			com.bloomlet.herobrine.HerobrineMod.LOGGER.info(
				"one of them was struck — {} more put the pretence down", woke);
		}
	}

	/** Nothing in his hands, which is most of what makes the watching bearable. */
	private void empty() {
		this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
	}

	/** Whoever he is watching, if they are still here. */
	private @org.jspecify.annotations.Nullable Player marked() {
		if (this.mark == null || !(this.level() instanceof ServerLevel here)) {
			return null;
		}
		Player who = here.getPlayerByUUID(this.mark);
		return who != null && who.isAlive() && !who.isSpectator() ? who : null;
	}
	// ---- END HE WATCHES ----------------------------------------------------

	/**
	 * How long since he last said something.
	 *
	 * He talks more, which is the first thing anybody notices without knowing
	 * they have noticed it. Villagers are quiet — one grunt every ten to twenty
	 * seconds — and this is every two to five, which does not register as a
	 * different sound so much as a person who will not stop muttering.
	 */
	private static final int TALKS_MIN = 40;
	private static final int TALKS_SPREAD = 60;
	private int talksIn;

	public TurnedEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		// The pathfinder has to know a door is a way through, or he will route
		// round the building and Forces will never get a chance to run.
		this.getNavigation().setCanOpenDoors(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			// A villager's health exactly. He is a man with an axe, not a boss,
			// and the fight should be over in the four or five hits it takes to
			// kill anything else that walks into you.
			// Was a villager's twenty exactly. A little over now — enough that a
			// diamond sword wants one more swing than it used to and a crowd of them
			// stops being arithmetic, and well short of anything that reads as a
			// health bar to be ground down.
			.add(Attributes.MAX_HEALTH, 26.0)
			// Enough to matter in leather and survivable in iron. Ordinary
			// damage on purpose — unlike the Reckoning this does NOT go through
			// armour, because a player who prepared should be rewarded for it
			// and this is not the fight the mod is building towards.
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.4)
			.add(Attributes.MOVEMENT_SPEED, CHARGE_SPEED)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	// ---- THE WATCH ---------------------------------------------------------
	// A posted one; see manifest.Watch for why and how many. Everything below is
	// the whole difference between a guard and a stalker, and it is deliberately
	// small: he does not freeze, he does not stalk, he does not force his way
	// through the house he was posted on, he comes for anyone near the door and
	// lets go of anyone who leaves. The rest of him is unchanged.

	private static final double GUARD_HEALTH = 48.0;
	private static final double GUARD_ARMOR = 8.0;
	private static final double GUARD_DAMAGE = 7.0;
	private static final double GUARD_KNOCKBACK_RESISTANCE = 0.5;
	/** Anyone this close to the post is his business. */
	private static final double GUARDS = 20.0;

	public void guard(net.minecraft.core.BlockPos post) {
		this.setAttached(com.bloomlet.herobrine.manifest.Watch.POST, post.asLong());
		this.setHomeTo(post, com.bloomlet.herobrine.manifest.Watch.HOLDS);
		this.committed = true;      // daylight is no protection from a guard
		this.raise(Attributes.MAX_HEALTH, GUARD_HEALTH);
		this.raise(Attributes.ARMOR, GUARD_ARMOR);
		this.raise(Attributes.ATTACK_DAMAGE, GUARD_DAMAGE);
		this.raise(Attributes.KNOCKBACK_RESISTANCE, GUARD_KNOCKBACK_RESISTANCE);
		this.setHealth(this.getMaxHealth());
		this.setPersistenceRequired();
	}

	private void raise(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> which, double to) {
		net.minecraft.world.entity.ai.attributes.AttributeInstance it = this.getAttribute(which);
		if (it != null) {
			it.setBaseValue(to);
		}
	}

	public boolean isGuard() {
		return this.hasAttached(com.bloomlet.herobrine.manifest.Watch.POST);
	}

	private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos post() {
		Long at = this.getAttached(com.bloomlet.herobrine.manifest.Watch.POST);
		return at == null ? null : net.minecraft.core.BlockPos.of(at);
	}

	/** The post holds him: home set again after a reload, and a target let go once it is out past the fence. */
	private void hold() {
		net.minecraft.core.BlockPos post = this.post();
		if (post == null) {
			return;
		}
		if (!this.hasHome()) {
			this.setHomeTo(post, com.bloomlet.herobrine.manifest.Watch.HOLDS);
		}
		LivingEntity at = this.getTarget();
		if (at != null && at.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5)
				> com.bloomlet.herobrine.manifest.Watch.LETS_GO * com.bloomlet.herobrine.manifest.Watch.LETS_GO) {
			this.setTarget(null);
		}
	}

	/**
	 * Anyone near the door. A countdown rather than a tickCount test, because the
	 * target selector only asks on every other tick and which parity depends on
	 * the entity id — a tickCount gate would never fire for half of them.
	 */
	private static final class Guarding extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;
		private int looksIn;

		Guarding(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!this.him.isGuard()) {
				return false;
			}
			LivingEntity at = this.him.getTarget();
			if (at != null && at.isAlive()) {
				return false;
			}
			if (--this.looksIn > 0) {
				return false;
			}
			this.looksIn = 10;
			return this.pick() != null;
		}

		@Override
		public void start() {
			Player who = this.pick();
			if (who != null) {
				this.him.setTarget(who);
			}
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		private @org.jspecify.annotations.Nullable Player pick() {
			net.minecraft.core.BlockPos post = this.him.post();
			if (post == null) {
				return null;
			}
			Player best = null;
			double nearest = GUARDS * GUARDS;
			for (Player who : this.him.level().players()) {
				if (who.isSpectator() || who.isCreative() || !who.isAlive()) {
					continue;
				}
				double d = who.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
				if (d <= nearest) {
					best = who;
					nearest = d;
				}
			}
			return best;
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// A LOCKED DOOR STOPPED BEING AN ANSWER.
		//
		// This used to say: no door-opening, no door-breaking, because a door he
		// cannot pass is the difference between him and the hunt and it keeps a
		// village at night survivable. That was written when he lived in a village
		// in the overworld and the player was outside it.
		//
		// In his own world the player is the one indoors, and a creature that stops
		// at a door is not a threat, it is scenery — you walk into any building and
		// the whole street stands outside it. Worse, the cottages there have doors
		// that jam, so half the time the answer was not even a decision somebody
		// made. It was a bug the mob was politely respecting.
		//
		// He opens ordinary doors while he walks, and when he is ANGRY he goes
		// through whatever is in the way. See Forces.
		this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(
			this, false));
		this.goalSelector.addGoal(1, new Forces(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		// Awake, and visibly with nothing to do. Villagers at night are in
		// their beds; the whole event is one man walking the square.
		// ABOVE THE STROLL AND BELOW THE MELEE, which is the whole ordering: if he
		// has a target the melee wins, if he has a mark this wins, and otherwise he
		// wanders. Three states, one line.
		this.goalSelector.addGoal(2, new Stalk(this));
		this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.6));
		this.targetSelector.addGoal(0, new Guarding(this));
		this.targetSelector.addGoal(1, new NightWatch(this));
		this.carry();
	}

	/** How long he works at one block before it gives. */
	private static final int FORCES_TICKS = 55;
	/** And how far in front of him he will reach to do it. */
	private static final double FORCES_REACH = 2.6;

	/**
	 * WHAT HE DOES ABOUT A DOOR HE CANNOT OPEN, once he is angry.
	 *
	 * Vanilla has BreakDoorGoal and it only knows wooden doors, on hard difficulty,
	 * and it will not touch glass or iron. All three of those are the things people
	 * actually hide behind — a cottage window, an iron door, a door somebody jammed
	 * shut. A creature that walks up to a pane of glass and stops is not
	 * frightening, it is a demonstration that the glass works.
	 *
	 * So this is one goal for all of it: while he HAS A TARGET and cannot reach
	 * them, whatever is at head or foot height directly in front gets worked on for
	 * about three seconds and then goes. Glass, panes, doors, trapdoors, iron
	 * included.
	 *
	 * ONLY WHILE HE HAS A TARGET, which is the whole safety valve. He is not
	 * demolishing the village on a quiet night — he walks past every window in the
	 * place until somebody hits him or gets too close, and from then on there is no
	 * building he cannot come into.
	 *
	 * The block is BROKEN rather than removed, so it drops. Nothing is lost but
	 * the window.
	 */
	private static final class Forces extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;
		private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos onIt;
		private int worked;

		private Forces(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(
				net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
		}

		private static boolean inTheWay(net.minecraft.world.level.block.state.BlockState state) {
			return state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
				|| state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
				|| state.is(net.minecraft.tags.BlockTags.IMPERMEABLE)
				|| state.getBlock()
					instanceof net.minecraft.world.level.block.IronBarsBlock;
		}

		private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos ahead() {
			net.minecraft.world.entity.LivingEntity at = this.him.getTarget();
			if (at == null) {
				return null;
			}
			net.minecraft.world.phys.Vec3 way = at.position()
				.subtract(this.him.position());
			if (way.lengthSqr() < 0.01) {
				return null;
			}
			net.minecraft.world.phys.Vec3 step = this.him.position()
				.add(way.normalize().scale(FORCES_REACH));
			for (int up = 0; up <= 1; up++) {
				net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(
					step.x, this.him.getY() + up, step.z);
				if (inTheWay(this.him.level().getBlockState(pos))) {
					return pos;
				}
			}
			return null;
		}

		@Override
		public boolean canUse() {
			if (this.him.isGuard()) {
				return false;      // he does not break the house he was posted on
			}
			net.minecraft.world.entity.LivingEntity at = this.him.getTarget();
			if (at == null || this.him.distanceTo(at) < 2.0) {
				return false;      // he can reach them. nothing is in the way.
			}
			this.onIt = this.ahead();
			return this.onIt != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.onIt != null && this.him.getTarget() != null
				&& inTheWay(this.him.level().getBlockState(this.onIt));
		}

		@Override
		public void start() {
			this.worked = 0;
		}

		@Override
		public void stop() {
			this.onIt = null;
			if (this.him.level() instanceof ServerLevel here) {
				here.destroyBlockProgress(this.him.getId(), net.minecraft.core.BlockPos.ZERO, -1);
			}
		}

		@Override
		public void tick() {
			if (this.onIt == null || !(this.him.level() instanceof ServerLevel here)) {
				return;
			}
			this.him.getLookControl().setLookAt(this.onIt.getX() + 0.5,
				this.onIt.getY() + 0.5, this.onIt.getZ() + 0.5, 30.0F, 30.0F);
			this.worked++;
			// The cracks, so it is legible from the other side of the glass that
			// something is coming through and roughly when.
			here.destroyBlockProgress(this.him.getId(), this.onIt,
				(int) (this.worked / (float) FORCES_TICKS * 10.0F));
			if (this.worked % 8 == 0) {
				here.playSound(null, this.onIt, net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
					this.him.getSoundSource(), 1.0F, 0.6F);
			}
			if (this.worked >= FORCES_TICKS) {
				here.destroyBlock(this.onIt, true, this.him);
				here.destroyBlockProgress(this.him.getId(), this.onIt, -1);
				this.onIt = null;
				this.worked = 0;
			}
		}
	}

	/**
	 * The axe.
	 *
	 * Cosmetic, exactly as his is: an item in a mob's main hand applies its own
	 * attribute modifiers, so an iron axe would silently take him from five
	 * damage to nine and every number above would be a lie. Clearing the
	 * modifiers makes it a thing he is carrying and nothing else.
	 *
	 * Iron rather than stone or diamond, and that is a deliberate reading of
	 * the fiction: it is the axe a villager would have had. He did not go and
	 * find a weapon; he picked up the one that was already leaning against the
	 * wall.
	 */
	private void carry() {
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		axe.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		this.setItemSlot(EquipmentSlot.MAINHAND, axe);
		// Nothing of his is ever left on the ground. A guaranteed iron axe every
		// time one of these dies would make him a farm.
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	/**
	 * WHO HE COMES FOR, AND WHEN.
	 *
	 * Written rather than assembled out of NearestAttackableTargetGoal, because
	 * that goal is built around the two behaviours this must not have: it drops
	 * a target the moment line of sight is lost for a few seconds, and it
	 * re-evaluates every tick to find somebody nearer. Both of those are correct
	 * for a zombie in a field and wrong for a person who has decided about you.
	 *
	 * He picks the nearest player he can actually SEE, after dark, and then he
	 * keeps them. Going round a corner is not an escape and neither is putting
	 * somebody else between you.
	 */
	/**
	 * KEEPING PACE, AND NOTHING ELSE.
	 *
	 * @see TurnedEntity#notice for why the state is simply getTarget() being null
	 *
	 * Three things happen in tick() and they are in order of how much the player
	 * will notice them. He holds his distance. If neither of you has moved for
	 * three seconds he closes a step — which is the beat the whole goal exists for,
	 * because a stalker who only ever mirrors you is a shadow, and one that gains
	 * ground when you stop is a decision being made. And if he loses sight of you
	 * he walks to where you were, which is not the same as giving up.
	 */
	private static class Stalk extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;
		private net.minecraft.world.phys.Vec3 wasAt =
			net.minecraft.world.phys.Vec3.ZERO;

		Stalk(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return !this.him.isGuard() && this.him.getTarget() == null && this.him.marked() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse() && this.him.patience > 0;
		}

		@Override
		public void stop() {
			this.him.mark = null;
			this.him.markAt = null;
			this.him.stillFor = 0;
			this.him.getNavigation().stop();
		}

		@Override
		public void tick() {
			Player who = this.him.marked();
			if (who == null) {
				return;
			}
			// He does not stop looking. Ever. The head is doing more work here than
			// the feet are — a man keeping pace behind you is ordinary until you
			// realise he has not once looked anywhere else.
			this.him.getLookControl().setLookAt(who, 30.0F, 30.0F);

			boolean seen = this.him.hasLineOfSight(who);
			if (seen) {
				this.him.markAt = who.blockPosition();
				this.him.patience = REMEMBERS;
			} else {
				// SOME TRACK, NOT PERFECT TRACK. Twenty seconds of walking to the
				// last place you were, and then he genuinely does not know. Running
				// away works; running away and then standing still forty blocks off
				// in the open does not.
				this.him.patience--;
				if (this.him.markAt != null) {
					this.him.getNavigation().moveTo(this.him.markAt.getX() + 0.5,
						this.him.markAt.getY(), this.him.markAt.getZ() + 0.5, 0.55);
				}
				return;
			}

			double away = this.him.distanceTo(who);
			if (away < SNAPS_AT) {
				// TOO CLOSE. Whether they walked into him or he closed the last step
				// himself does not matter — at arm's length there is nothing left to
				// watch. No shout: this one is on the player and they can see it
				// coming, so it stays between the two of them.
				this.him.snap(who, false);
				return;
			}

			net.minecraft.world.phys.Vec3 now = who.position();
			boolean bothStill = now.distanceToSqr(this.wasAt) < STILL
				&& this.him.getDeltaMovement().horizontalDistanceSqr() < STILL;
			this.wasAt = now;

			if (away > this.him.standing + SLACK) {
				this.him.getNavigation().moveTo(who, 0.62);
				this.him.stillFor = 0;
				return;
			}
			if (away < this.him.standing - SLACK) {
				// Backing off, and by position rather than by path — a mob asked to
				// pathfind AWAY from something turns its back to do it, and the one
				// thing he must never do is stop facing you.
				net.minecraft.world.phys.Vec3 back = this.him.position()
					.subtract(who.position()).normalize().scale(0.06);
				this.him.setDeltaMovement(this.him.getDeltaMovement()
					.add(back.x, 0.0, back.z));
				this.him.stillFor = 0;
				return;
			}

			this.him.getNavigation().stop();
			if (!bothStill) {
				this.him.stillFor = 0;
				return;
			}
			// AND HERE IS THE ONE THAT LANDS. Stand and look at him and he waits —
			// and then, three seconds in, he is a block and a half nearer than he
			// was, and he did not run and there was no sound. Waiting him out is
			// not an option, and finding that out is the entire point of the state.
			if (++this.him.stillFor >= CREEPS_AFTER) {
				this.him.stillFor = 0;
				net.minecraft.world.phys.Vec3 in = who.position()
					.subtract(this.him.position()).normalize().scale(A_STEP);
				this.him.getNavigation().moveTo(this.him.getX() + in.x,
					this.him.getY(), this.him.getZ() + in.z, 0.4);
				this.him.standing = Math.max(SNAPS_AT + 0.5,
					this.him.standing - A_STEP);
			}
		}
	}

	private static class NightWatch extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;

		NightWatch(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (this.him.isGuard()) {
				return false;      // Guarding picks for a posted one, and only near the door
			}
			if (this.him.getTarget() != null && this.him.getTarget().isAlive()) {
				return false;
			}
			// isBrightOutside rather than a time-of-day comparison, so a
			// thunderstorm dark enough to spawn mobs is dark enough for him.
			// That is the right reading of "night": what he waits for is the
			// village being asleep and the light being gone, and a storm at
			// four in the afternoon delivers both.
			//
			// `committed` gets him past this, which only matters in the window
			// where the person he was going for has vanished — logged out,
			// teleported — and somebody else is standing there. He does not go
			// back to being a villager in the middle of it.
			if (!this.him.committed && this.him.level().isBrightOutside()) {
				return false;
			}
			return this.pick() != null;
		}

		/**
		 * AND THE MORNING IS NOT AN ANSWER.
		 *
		 * Nothing about the sky is checked here, and that is the whole of the
		 * "til you die or he die" rule. The night decides whether it STARTS;
		 * once it has, the only two things that end it are one of them going
		 * down. A pursuer who gives up at sunrise can be waited out, and a
		 * player who has worked that out is playing a clock rather than running
		 * from somebody.
		 *
		 * Distance is not an answer either — there is no forget-range here on
		 * purpose. Sprinting still gets you away, because he is slower than a
		 * sprint, but it gets you away with him still coming.
		 */
		@Override
		public boolean canContinueToUse() {
			LivingEntity target = this.him.getTarget();
			return target != null && target.isAlive()
				&& !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
		}

		/**
		 * It is over, and he is a villager again.
		 *
		 * Reached only when the person is dead or gone, because nothing else
		 * ends canContinueToUse. Clearing `committed` here is what stops one
		 * night turning him permanently hostile to everybody who walks past for
		 * the rest of the save.
		 */
		@Override
		public void stop() {
			this.him.setTarget(null);
			this.him.committed = false;
		}

		@Override
		public void start() {
			Player quarry = this.pick();
			if (quarry == null) {
				return;
			}
			// A MARK, NOT A TARGET. This used to hand him a target directly, which
			// meant the first thing the night ever did was start a fight. He notices
			// now, and what happens next is up to how close the player comes.
			this.him.notice(quarry);
			// THE MOMENT, and it wants exactly one sound. He has been standing
			// there muttering all evening and now he is not.
			if (!this.him.committed) {
				this.him.committed = true;
				this.him.level().playSound(null, this.him.getX(), this.him.getY(),
					this.him.getZ(), SoundEvents.VILLAGER_NO,
					this.him.getSoundSource(), 1.6F, 0.6F);
				HerobrineMod.LOGGER.info("the turned one has come for {} at [{}, {}, {}]",
					quarry.getName().getString(), this.him.blockPosition().getX(),
					this.him.blockPosition().getY(), this.him.blockPosition().getZ());
			}
		}

		/** Nearest, and he has to be able to see them. */
		private @org.jspecify.annotations.Nullable Player pick() {
			Player best = null;
			double nearest = Double.MAX_VALUE;
			for (Player player : this.him.level().players()) {
				if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
					continue;
				}
				double away = this.him.distanceTo(player);
				if (away > NOTICES || away >= nearest) {
					continue;
				}
				// SEEN, not merely near. "If you are seen he comes running" is
				// the whole rule, and it is also the counter-play: staying
				// behind the wall works, and works for as long as you keep it
				// between you.
				if (!this.him.hasLineOfSight(player)) {
					continue;
				}
				nearest = away;
				best = player;
			}
			return best;
		}
	}

	/**
	 * BY DAY HE WATCHES, AND THAT IS ALL HE DOES.
	 *
	 * Driven straight from the geometry rather than left to LookAtPlayerGoal,
	 * for the same reason his stare is: that goal picks a target on a
	 * probability and lets go of it after a random number of ticks, because it
	 * was written to make idle villagers glance at passers-by. Applied here it
	 * would give a man who mostly looks at you, and a villager who mostly looks
	 * at you is a villager.
	 *
	 * The body turns too. A head swivelled round on a body still facing its
	 * work bench is the wrong image — this one wants him squared up to you,
	 * doing nothing, in the middle of the afternoon.
	 */
	/**
	 * HE GETS BETTER.
	 *
	 * When Herobrine is removed, whatever was wrong with these people goes with
	 * him. The ones in loaded chunks are cured from die() directly; this is for
	 * the rest — a Turned that loads in a week later, in a town nobody has been
	 * back to, checks the flag once a second and becomes a villager on the spot.
	 *
	 * A real Villager, spawned as a CONVERSION so finalizeSpawn dresses him for
	 * the biome, with the name kept if he had one, and no profession — he has
	 * been through enough to be allowed to stand around. The Turned is discarded
	 * rather than killed, so nothing drops and no death is counted.
	 */
	public void redeem() {
		if (!(this.level() instanceof ServerLevel here) || !this.isAlive()) {
			return;
		}
		net.minecraft.world.entity.npc.villager.Villager man =
			net.minecraft.world.entity.EntityTypes.VILLAGER.create(here,
				net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
		if (man == null) {
			return;
		}
		man.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
		man.finalizeSpawn(here, here.getCurrentDifficultyAt(this.blockPosition()),
			net.minecraft.world.entity.EntitySpawnReason.CONVERSION, null);
		if (this.getCustomName() != null) {
			man.setCustomName(this.getCustomName());
		}
		man.setPersistenceRequired();
		here.addFreshEntity(man);
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
			this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.5, 0.8, 0.5, 0.0);
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, this.getSoundSource(),
			1.0F, 1.0F);
		this.discard();
	}

	/** Who had his eye at the last look; see tick(). */
	private @org.jspecify.annotations.Nullable Player watcher;

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		if (Corpses.isCorpse(this)) {
			return;      // a dead one does not talk, watch, or turn to face you
		}
		if (this.tickCount % 20 == 0 && this.level().getServer() != null
			&& com.bloomlet.herobrine.wrath.Wrath.removed(this.level().getServer())) {
			this.redeem();
			return;
		}
		this.talk();
		if (this.isGuard()) {
			this.hold();
			return;      // a guard does not play the game below. See Watch
		}
		if (this.getTarget() != null) {
			return;      // he is busy
		}
		if ((this.tickCount & 3) == 0) {
			// A village holds many of these. The look, nearest player then a raycast,
			// is taken every fourth tick and remembered. The freeze below runs every
			// tick, or he would creep three ticks in four while being watched.
			Player near = this.level().getNearestPlayer(this, NOTICES);
			this.watcher = near != null && this.hasLineOfSight(near) ? near : null;
		}
		Player watching = this.watcher;
		if (watching == null || watching.isRemoved()) {
			return;
		}
		this.getNavigation().stop();
		float yaw = (float)(net.minecraft.util.Mth.atan2(
			watching.getZ() - this.getZ(), watching.getX() - this.getX())
			* (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		// The previous-tick value as well, or the client interpolates from
		// wherever he was facing and the turn arrives as a visible snap.
		this.yHeadRotO = yaw;
		this.setYBodyRot(yaw);
		// The yaw is pinned above; this is only here for the PITCH, so he looks
		// up at somebody on a roof and down at somebody in a ditch. Exactly the
		// split HerobrineEntity.faceOneOf uses, and for the same reason — the
		// look control on its own lags behind anybody walking round him, which
		// reads as losing track of them.
		this.getLookControl().setLookAt(watching.getX(), watching.getEyeY(),
			watching.getZ(), 90.0F, 90.0F);
	}

	/** He will not stop muttering, and it is pitched a little low. */
	private void talk() {
		if (--this.talksIn > 0 || this.isSilent()) {
			return;
		}
		this.talksIn = TALKS_MIN + this.random.nextInt(TALKS_SPREAD);
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
			this.getTarget() == null ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.VILLAGER_TRADE,
			this.getSoundSource(), 1.0F, 0.72F + this.random.nextFloat() * 0.1F);
	}

	/**
	 * NOTHING ELSE IN THE DARK GETS TO HAVE HIM.
	 *
	 * The single most likely way for this event to be lost, and it would be lost
	 * silently: he is out in the open all night in a village, which is the exact
	 * profile of a thing zombies kill before breakfast. A player who never met
	 * him would have no way of knowing there had been anything to meet.
	 *
	 * Not invulnerability — a PLAYER can kill him, and that is the entire point
	 * of him. It is only that nothing else can. Skeletons, creepers, fire, fall
	 * damage and somebody else's dog all do nothing, and what is left is a fight
	 * between him and whoever he came for.
	 *
	 * Projectiles are resolved by their OWNER rather than by the arrow, or a
	 * skeleton's shot would count as the player's and a player's would not
	 * count at all.
	 */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getEntity() instanceof Player striker) {
			// AND THAT IS THE OTHER WAY OUT OF WATCHING. Swinging at one of them is
			// a decision, and unlike walking too close it is one the player cannot
			// make by accident — so it is the trigger that carries the shout.
			this.snap(striker, true);
			return super.hurtServer(level, source, damage);
		}
		// The escape hatch every invulnerable thing in the game keeps: /kill,
		// the void, and anything else tagged as bypassing invulnerability. A mob
		// an operator cannot remove is a bug report, and one that sits at the
		// bottom of the world forever taking no damage is a worse one.
		if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		return false;
	}

	/**
	 * And zombies do not queue up for him either.
	 *
	 * Refusing the damage stops him dying and does not stop six of them
	 * following him round the square all night, which looks ridiculous and
	 * pins his pathfinding. He is not a Villager, so nothing targets him by
	 * type — this only has to catch anything that picked him up as a generic
	 * nearby living thing.
	 */
	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	/**
	 * RIGHT-CLICKING HIM OPENS NOTHING.
	 *
	 * Free, because he was never a Villager and has no trades to suppress — but
	 * worth saying out loud, because it is the check every player already
	 * performs on a villager they are suspicious of, and it is the answer they
	 * get.
	 */
	@Override
	public net.minecraft.world.InteractionResult mobInteract(Player player,
			net.minecraft.world.InteractionHand hand) {
		return net.minecraft.world.InteractionResult.PASS;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;    // talk() owns his voice; two systems would overlap
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	/** Never. He was put here on purpose and he is meant to be found. */
	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Committed", this.committed);
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		this.committed = input.getBooleanOr("Committed", false);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof ServerPlayer killer) {
			HerobrineMod.LOGGER.info("{} put the turned one down", killer.getName().getString());
		}
	}
}
