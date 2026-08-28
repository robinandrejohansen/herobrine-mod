package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * THE GAUNT — the tall one, and the only thing in his world that never chases.
 *
 * Everything else the mod has built moves toward you. He hunts, the Turned
 * stalk, the infected charge. This one does the opposite and the opposite is
 * worse: WHILE YOU CAN SEE IT, IT DOES NOT MOVE AT ALL. It stands in the trees
 * at the wrong height, facing you, and nothing happens. The moment it leaves
 * your screen it closes — walking if it is near, stepping straight through the
 * distance if it is not — and the next time you turn round it is nearer.
 *
 * That is the entire creature. There is no chase to lose, no aggro to shake, no
 * fight to win by kiting. There is a thing that is closer than it was, and the
 * only reason it has not reached you yet is that you have not blinked.
 *
 * WHY IT IS NOT A TurnedEntity VARIANT. The plan for the rest of the forest
 * family is one class with a variant byte, because the Boy, the Herd and the
 * Follower all share the stalk — they differ in numbers and nerve. This one
 * shares nothing with it. Its movement rule is inverted, its target handling is
 * inverted, it carries no weapon, it makes no sound, and its hitbox is a
 * different shape. Threading all of that through TurnedEntity as conditionals
 * would leave two creatures in one class, both harder to read than either.
 *
 * THE STARE IS NOT FREE, and it must not be, or the counterplay is to walk
 * backwards through the whole forest with it centred on your screen. Looking at
 * it long enough gives you Darkness — so the answer to the tall thing is to
 * blind yourself to everything else, which is not an answer, it is a trade. In a
 * forest that is going to have a Herd in it, that trade is the whole design.
 */
public class GauntEntity extends PathfinderMob {

	/**
	 * IN VIEW, NOT IN THE CROSSHAIR.
	 *
	 * The enderman's own test is dot > 1.0 - 0.025/distance, which is a cone
	 * about a degree wide — it means "aimed at", and it is correct for a mob whose
	 * rule is that staring at it makes it angry. It is wrong here. The rule here
	 * is that being SEEN stops it, and a player sees their whole screen, so a
	 * one-degree cone would let it walk up while it sat in plain sight at the edge
	 * of the display.
	 *
	 * Half of a right angle, roughly, which is a little inside a default field of
	 * view. Slightly narrower than the screen on purpose: something that freezes
	 * exactly at the edge of vision produces an argument about whether it moved,
	 * and this thing is much better when the argument is possible.
	 */
	private static final double IN_VIEW = 0.5;

	/** Inside this it stops caring whether it is watched. */
	private static final double REACHES = 3.2;
	/** Beyond this, unwatched, it does not walk — it arrives. */
	private static final double STEPS_FROM = 14.0;
	/** Ticks between those. Without it, one blink crosses a hundred blocks. */
	private static final int SLIPS_EVERY = 60;
	/** Where it puts itself down, relative to the player. */
	private static final double LANDS_NEAR = 7.0;
	private static final double LANDS_FAR = 13.0;
	/** How many landing spots it will consider before staying where it is. */
	private static final int TRIES = 16;

	/** Continuous ticks of being looked at before the looking starts to cost. */
	private static final int STARE_COSTS = 70;
	private static final int DARK_FOR = 60;

	private int watchedFor;
	private int slipsIn;

	public GauntEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		// Rare and solitary. One of these is an event; two is a queue.
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			// Twice a Turned. It should not be a boss — it is still a villager
			// underneath, and the fight is meant to be winnable the moment you
			// decide to have it. What is expensive about it is deciding.
			.add(Attributes.MAX_HEALTH, 40.0)
			// It gets very few swings, because it only closes when unobserved and
			// the moment it is in reach you are looking at it. The ones it gets
			// have to be worth having run from.
			.add(Attributes.ATTACK_DAMAGE, 9.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.6)
			// Faster than a player walking, slower than one sprinting. Turning your
			// back and running IS an answer — it is just an answer that ends with
			// you somewhere you have not looked at yet.
			.add(Attributes.MOVEMENT_SPEED, 0.36)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	/**
	 * NARROW AND LONG, and the hitbox has to agree with the drawing.
	 *
	 * GauntRenderer stretches the villager mesh — thin on x and z, long on y —
	 * and a visual-only stretch is the classic way to ship a mob you cannot hit
	 * where it looks like it is. The type is registered at the drawn size and this
	 * exists so the scale lives in ONE place: change the constant, both agree.
	 */
	public static final float WIDE = 0.5F;
	public static final float TALL = 3.3F;

	// No getDimensions override: LivingEntity marks it final, and it does not need
	// one. EntityType.Builder.sized in ModEntities is fed these same two constants,
	// so the box and the drawing already come from one place.

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// Melee stays, and it is deliberately allowed to run while frozen. isImmobile
		// stops the body; the goal still swings. Something that has reached you does
		// not politely wait for you to look away.
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new Close(this));
		this.targetSelector.addGoal(1,
			new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ---- BEING LOOKED AT ---------------------------------------------------
	/**
	 * Whether anybody has it on their screen.
	 *
	 * ANY player, not its target. Two people in a forest and one of them watching
	 * it is the reason the other one is still alive, and that is a thing worth
	 * being true — it makes "keep eyes on it" a job somebody can be given.
	 */
	private @org.jspecify.annotations.Nullable Player watcher() {
		for (Player who : this.level().players()) {
			if (who.isSpectator() || !who.isAlive()) {
				continue;
			}
			if (this.distanceTo(who) > this.getAttributeValue(Attributes.FOLLOW_RANGE)) {
				continue;
			}
			if (looking(who) && this.hasLineOfSight(who)) {
				return who;
			}
		}
		return null;
	}

	private boolean looking(Player who) {
		Vec3 eye = who.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(this.getX() - who.getX(),
			this.getEyeY() - who.getEyeY(), this.getZ() - who.getZ());
		return eye.dot(toMe.normalize()) > IN_VIEW;
	}

	/**
	 * The freeze itself, and isImmobile is the honest hook for it.
	 *
	 * The tempting version is to zero the velocity in tick() after super.tick()
	 * has run — and that version leaks, because super.tick() is where the AI has
	 * ALREADY applied a step. It would drift toward you a fraction of a block per
	 * tick while nominally frozen, which is both a bug and, infuriatingly, an
	 * effect that half works.
	 *
	 * isImmobile is checked inside LivingEntity before movement is applied, which
	 * is the difference between standing still and almost standing still.
	 */
	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || this.frozen();
	}

	private boolean frozen() {
		// Not once it is on top of you. At that range the game is up and pretending
		// otherwise would mean a mob that can never land the hit it walked over for.
		return this.watchedFor > 0 && (this.getTarget() == null
			|| this.distanceTo(this.getTarget()) > REACHES);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		if (this.slipsIn > 0) {
			this.slipsIn--;
		}

		Player seen = this.watcher();
		if (seen == null) {
			this.watchedFor = 0;
			this.slip();
			return;
		}
		this.watchedFor++;
		// Squared up, so being watched is being LOOKED BACK AT. A head turned to
		// you on a body facing the trees reads as a mob mid-pathfind; the whole
		// body turned reads as attention, and attention is the entire performance.
		this.getNavigation().stop();
		float yaw = (float)(Mth.atan2(seen.getZ() - this.getZ(),
			seen.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.yHeadRotO = yaw;
		this.setYBodyRot(yaw);
		this.getLookControl().setLookAt(seen.getX(), seen.getEyeY(), seen.getZ(),
			90.0F, 90.0F);

		if (this.watchedFor > STARE_COSTS) {
			seen.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARK_FOR, 0,
				false, false));
		}
	}

	// ---- CLOSING -----------------------------------------------------------
	/**
	 * ARRIVING RATHER THAN WALKING, when it is far enough out to get away with it.
	 *
	 * Walking the whole way is wrong twice over. It is slow — a forest at fifty
	 * blocks means a minute of nothing — and it is legible: a player who turns
	 * round twice can measure the speed and stops being frightened of it, because
	 * a thing with a known speed is a thing you can outrun and therefore a thing
	 * you have solved.
	 *
	 * Under STEPS_FROM it walks, because at close range the sudden arrival would
	 * be the mob teleporting into melee, which is not tense, it is unfair. Over
	 * it, it steps — and it lands BEHIND the player specifically, so the next
	 * thing that happens is you turning round.
	 */
	private void slip() {
		Player who = this.getTarget() instanceof Player p ? p
			: this.level().getNearestPlayer(this, this.getAttributeValue(Attributes.FOLLOW_RANGE));
		if (who == null || this.slipsIn > 0 || this.distanceTo(who) < STEPS_FROM) {
			return;
		}
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 back = who.getViewVector(1.0F).normalize().scale(-1.0);
		for (int look = 0; look < TRIES; look++) {
			// Fanned around the player's back rather than dead behind, or the
			// arrivals line up and it reads as a spawn point.
			double swing = (this.random.nextDouble() - 0.5) * Math.PI;
			double range = LANDS_NEAR + this.random.nextDouble() * (LANDS_FAR - LANDS_NEAR);
			double dx = (back.x * Math.cos(swing) - back.z * Math.sin(swing)) * range;
			double dz = (back.x * Math.sin(swing) + back.z * Math.cos(swing)) * range;
			BlockPos at = BlockPos.containing(who.getX() + dx, who.getY(), who.getZ() + dz);
			// Down to the ground, then up to the first place a three-block thing
			// fits. Without the height check it lands under an overhang with its
			// head in stone and suffocates, which is a lesson this codebase has
			// already paid for once with the undercity's villagers.
			at = level.getHeightmapPos(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
			if (!room(level, at)) {
				continue;
			}
			if (this.randomTeleport(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, false)) {
				this.slipsIn = SLIPS_EVERY;
				HerobrineMod.LOGGER.debug("the tall one is closer, [{}, {}, {}]",
					at.getX(), at.getY(), at.getZ());
				return;
			}
		}
	}

	private boolean room(ServerLevel level, BlockPos feet) {
		if (!level.getBlockState(feet.below()).isSolid()) {
			return false;
		}
		for (int up = 0; up < Mth.ceil(TALL); up++) {
			if (!level.getBlockState(feet.above(up)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * It only moves when nobody has it, which is what a Goal is for.
	 *
	 * The freeze is enforced by isImmobile regardless — this exists so the
	 * NAVIGATION stops too. Leaving the path running under a frozen body means the
	 * instant the player looks away it resumes mid-stride from a stale route, and
	 * a stale route through a forest walks it into a tree.
	 */
	private static final class Close extends Goal {
		private final GauntEntity him;

		private Close(GauntEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.him.getTarget() != null && !this.him.frozen();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void stop() {
			this.him.getNavigation().stop();
		}

		@Override
		public void tick() {
			net.minecraft.world.entity.LivingEntity at = this.him.getTarget();
			if (at == null) {
				return;
			}
			this.him.getLookControl().setLookAt(at, 30.0F, 30.0F);
			this.him.getNavigation().moveTo(at, 1.0);
		}
	}

	// ---- IT MAKES NO NOISE -------------------------------------------------
	/**
	 * Silent, and that is the point of it.
	 *
	 * The Turned will not stop muttering — villager ambience pitched down, so you
	 * hear one before you see it and the sound is the warning. This one has no
	 * warning. The only way to know it is there is to look at where it is, which
	 * means the only way to find it is the same act that stops it.
	 */
	@Override
	protected @org.jspecify.annotations.Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("SlipsIn", this.slipsIn);
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		this.slipsIn = input.getIntOr("SlipsIn", 0);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer killer) {
			HerobrineMod.LOGGER.info("{} put the tall one down", killer.getName().getString());
		}
	}
}
