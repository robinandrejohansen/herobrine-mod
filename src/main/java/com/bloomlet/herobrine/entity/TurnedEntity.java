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
 * ONE OF THE TURNED. A villager he has had, with white eyes and no pupils and
 * an axe.
 *
 * Simple on purpose. They see you first — through walls, floors and trees, out
 * to twenty-four blocks — so a house is no hiding place from one. They follow
 * you at a walk until they are close enough, and then they come, and the axe
 * reaches further than a fist. Hit one and the others near it come too. A wall
 * of wood does not stop them: they chop through it (Forces).
 *
 * Its own mob rather than a real Villager, so no trading, no beds, no brain to
 * suppress. Guards posted at his places (Watch) keep to their post instead of
 * following (Guarding). Only a player can finish one: Addexio stops at a heart.
 * When he is gone for good, every one of them is a villager again (redeem).
 */
public class TurnedEntity extends PathfinderMob {

	/** Walking pace: how fast they close when they come. */
	private static final double CHARGE_SPEED = 0.33;

	/**
	 * THEY SEE YOU FIRST. Through walls, floors and trees, out to SEES; that is
	 * how they find you, and why there is no hiding in a house from one.
	 */
	private static final double SEES = 24.0;
	/** Past STRIKES_FROM they follow, at a walk. Inside it, they come. */
	private static final double STRIKES_FROM = 6.0;
	private static final double FOLLOWS_AT = 0.75;
	private static final double COMES_AT = 1.2;
	/** Hit one and every one of them within SHOUT comes too. */
	private static final double SHOUT = 24.0;

	/** Hit one, and it — and the others near it — come for you. */
	public void snap(Player at, boolean shout) {
		this.carry();
		this.setTarget(at);
		if (!shout || !(this.level() instanceof ServerLevel here)) {
			return;
		}
		for (TurnedEntity other : here.getEntitiesOfClass(TurnedEntity.class,
				this.getBoundingBox().inflate(SHOUT))) {
			if (other != this && other.getTarget() == null && !other.isGuard()) {
				other.carry();
				other.setTarget(at);
			}
		}
	}

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
		this.goalSelector.addGoal(0, new Forces(this));
		this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, false));
		this.goalSelector.addGoal(1, new Strike(this));
		this.goalSelector.addGoal(2, new Follow(this));
		this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.6));
		this.targetSelector.addGoal(0, new Guarding(this));
		this.targetSelector.addGoal(1, new Senses(this));
		this.carry();
	}

	/**
	 * SIMPLE: they find you (Senses, through walls), they follow you at a walk
	 * (Follow) until they are close enough, and then they come and hit you
	 * (Strike). That is the whole of it.
	 */
	private static final class Senses
			extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<Player> {
		private final TurnedEntity him;

		Senses(TurnedEntity him) {
			super(him, Player.class, 10, false, false, (who, level) -> !who.isSpectator() && !((Player) who).isCreative());
			this.him = him;
		}

		@Override
		public boolean canUse() {
			return !this.him.isGuard() && super.canUse();      // a guard keeps to his post. See Guarding
		}

		@Override
		protected double getFollowDistance() {
			return SEES;
		}
	}

	private static final class Follow extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;
		private int pathIn;

		Follow(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity at = this.him.getTarget();
			return at != null && at.isAlive() && this.him.distanceTo(at) > STRIKES_FROM;
		}

		@Override
		public void start() {
			this.pathIn = 0;
		}

		@Override
		public void stop() {
			this.him.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity at = this.him.getTarget();
			if (at == null) {
				return;
			}
			this.him.getLookControl().setLookAt(at, 30.0F, 30.0F);
			if (--this.pathIn <= 0) {
				this.pathIn = 10;
				this.him.getNavigation().moveTo(at, FOLLOWS_AT);
			}
		}
	}

	private static final class Strike extends MeleeAttackGoal {
		private final TurnedEntity him;

		Strike(TurnedEntity him) {
			super(him, COMES_AT, true);
			this.him = him;
		}

		@Override
		public boolean canUse() {
			LivingEntity at = this.him.getTarget();
			return at != null && this.him.distanceTo(at) <= STRIKES_FROM && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity at = this.him.getTarget();
			return at != null && this.him.distanceTo(at) <= STRIKES_FROM + 2.0 && super.canContinueToUse();
		}
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

		/**
		 * Doors, glass and bars, as before — and now anything made of wood, and
		 * leaves. A wall of planks with no door in it used to be the one thing
		 * that stopped him: he stood outside it with an axe in his hand. He has
		 * the axe. He uses it, two blocks high, the way a zombie takes a door.
		 */
		private static boolean inTheWay(net.minecraft.world.level.block.state.BlockState state) {
			return state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
				|| state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
				|| state.is(net.minecraft.tags.BlockTags.IMPERMEABLE)
				|| state.getBlock()
					instanceof net.minecraft.world.level.block.IronBarsBlock
				|| state.is(net.minecraft.tags.BlockTags.PLANKS)
				|| state.is(net.minecraft.tags.BlockTags.LOGS)
				|| state.is(net.minecraft.tags.BlockTags.WOODEN_SLABS)
				|| state.is(net.minecraft.tags.BlockTags.WOODEN_STAIRS)
				|| state.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)
				|| state.is(net.minecraft.tags.BlockTags.FENCE_GATES)
				|| state.is(net.minecraft.tags.BlockTags.LEAVES);
		}

		/** How far ahead he looks for the wall — right in front of him first, then further out. */
		private static final double[] REACHES = {1.0, 1.8, FORCES_REACH};
		/** One block takes between two and five and a half seconds. */
		private static final int TAKES_MIN = 40;
		private static final int TAKES_SPREAD = 70;
		/** And now and then, when one is through, he stands and looks at the hole for a bit. Not clever. Gets there. */
		private static final int DAWDLES_ONE_IN = 3;
		private static final int DAWDLES_MIN = 20;
		private static final int DAWDLES_SPREAD = 40;
		private int takes = FORCES_TICKS;
		private int dawdleUntil;

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
			net.minecraft.world.phys.Vec3 toward = way.normalize();
			for (double reach : REACHES) {
				net.minecraft.world.phys.Vec3 step = this.him.position().add(toward.scale(reach));
				for (int up = 0; up <= 1; up++) {
					net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(
						step.x, this.him.getY() + up, step.z);
					if (inTheWay(this.him.level().getBlockState(pos))) {
						return pos;
					}
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
			if (this.him.tickCount < this.dawdleUntil) {
				return false;      // looking at the last hole
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
			this.takes = TAKES_MIN + this.him.random.nextInt(TAKES_SPREAD);
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
				(int) (this.worked / (float) this.takes * 10.0F));
			if (this.worked % 8 == 0) {
				here.playSound(null, this.onIt, net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
					this.him.getSoundSource(), 1.0F, 0.6F);
			}
			if (this.worked >= this.takes) {
				here.destroyBlock(this.onIt, true, this.him);
				here.destroyBlockProgress(this.him.getId(), this.onIt, -1);
				this.onIt = null;
				this.worked = 0;
				if (this.him.random.nextInt(DAWDLES_ONE_IN) == 0) {
					this.dawdleUntil = this.him.tickCount + DAWDLES_MIN + this.him.random.nextInt(DAWDLES_SPREAD);
				}
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

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		if (Corpses.isCorpse(this)) {
			return;      // a dead one does not talk or come
		}
		if (this.tickCount % 20 == 0 && this.level().getServer() != null
			&& com.bloomlet.herobrine.wrath.Wrath.removed(this.level().getServer())) {
			this.redeem();
			return;
		}
		this.talk();
		if (this.isGuard()) {
			this.hold();
		}
	}

	/**
	 * A LONG ARM, LIKE A GOLEM'S. The axe reaches further than a fist: a swing
	 * lands from most of a block further out than a zombie's would.
	 */
	@Override
	protected net.minecraft.world.phys.AABB getAttackBoundingBox(double expansion) {
		return super.getAttackBoundingBox(expansion).inflate(0.9, 0.0, 0.9);
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
		if (source.getEntity() instanceof CompanionEntity he) {
			if (!(this.getTarget() instanceof Player)) {      // a player is the prize; otherwise, him
				this.carry();
				this.setTarget(he);
			}
			// ADDEXIO WOUNDS, YOU FINISH. His sword counts — a man swinging forever
			// at something he cannot dent is a man who never leaves — but it stops at
			// one heart. The kill is the player's, every time, or the fight is his.
			float room = this.getHealth() - 1.0F;
			return room > 0.0F && super.hurtServer(level, source, Math.min(damage, room));
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

	/** There is no pretence left to keep: Addexio, golems and everyone else may see him for what he is. */
	@Override
	public boolean canBeSeenAsEnemy() {
		return true;
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
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof ServerPlayer killer) {
			HerobrineMod.LOGGER.info("{} put the turned one down", killer.getName().getString());
		}
	}
}
