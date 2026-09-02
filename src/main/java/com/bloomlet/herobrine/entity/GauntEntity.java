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
import net.minecraft.world.entity.LivingEntity;
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

	/** Continuous ticks of being looked at before the looking starts to cost. */
	private static final int STARE_COSTS = 70;
	private static final int DARK_FOR = 60;

	private int watchedFor;

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
			// FAR LESS DAMAGE, AND IT THROWS YOU INSTEAD.
			//
			// Nine was a two-hit kill through iron, which makes the thing that never
			// moves while you watch it into something you simply must not let touch
			// you — and that is a stat, not a scare. Four is survivable, which is the
			// point: the blow is not what you remember about it.
			//
			// What you remember is the arc. See launch().
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ATTACK_KNOCKBACK, 1.2)
			// A GOLEM'S PACE, WHICH IS THE SHAPE OF THE THING RATHER THAN A NUMBER.
			//
			// An iron golem walks at 0.25 and reads as unhurried, heavy and certain —
			// it is not chasing you, it is coming, and the difference is legible from
			// across a field. That is exactly the register this wants, and 0.19 was
			// under it: slow enough to read as damaged rather than deliberate.
			//
			// Still comfortably under a sprint. It never catches anybody who is
			// moving. It catches people who stopped.
			//
			// It used to arrive rather than walk when it was far enough out, and
			// that covered a lot: speed did not matter much when the gap could be
			// deleted. With the teleport gone the speed IS the threat, and the
			// threat is not that it is quick. A slow thing you cannot outrun and a
			// slow thing you can are different creatures, and this is the second —
			// walking away works, every time, as long as you keep walking. What it
			// costs you is the rest of the night.
			//
			// Well under a player's walk. It never catches anybody who is moving.
			// It catches people who stopped.
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	/**
	 * THE ENDERMAN'S OWN BOX, because it is now the enderman's own model.
	 *
	 * Every previous version of these two numbers was a guess chased after a
	 * poseStack scale — the villager mesh stretched by hand, the hitbox adjusted
	 * to try to catch up, and a standing invitation to ship a mob you cannot hit
	 * where it looks like it is. Taking vanilla's model takes vanilla's dimensions
	 * with it and the guessing stops: 0.6 by 2.9, no scaling anywhere, drawing and
	 * hurtbox in agreement by construction.
	 *
	 * Still most of a block over a player, which was the whole point of the height.
	 */
	/**
	 * AND THE HITBOX FOLLOWS THE DRAWING. GauntRenderer thickens the limbs by
	 * sixty per cent, which pushes the silhouette out past the enderman's 0.6 —
	 * and ModEntities says the thing about a hitbox that disagrees with the mesh
	 * being the oldest bug in modded Minecraft, so it is not going to be left
	 * disagreeing here.
	 */
	public static final float WIDE = 0.7F;
	public static final float TALL = 2.9F;

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
		// AND IT LOOKS FOR HER TOO, which is what makes her mortal enough to matter.
		//
		// This asked for Player.class, and so does everything else in the mod. Vera
		// is not a player — so the entire flee-and-eat half of CompanionEntity was
		// dead code: real health, a real threshold, a loaf of bread, and nothing in
		// the world able to take one point off her.
		//
		// Nearest wins, so it will leave you for her if she is closer. That is the
		// intended behaviour and not a compromise: something that ignores the person
		// beside you is not frightening, it is scenery, and the moment she is the one
		// being chased is the moment she stops being a follower and becomes a person
		// you are standing between.
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
			this, LivingEntity.class, 10, true, false,
			(who, level) -> CompanionEntity.canBeHurtBy(who)));
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
		this.speak();
		this.echo();

		Player seen = this.watcher();
		if (seen == null) {
			this.watchedFor = 0;
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
			// AND EVERY TIME THE SCREEN GOES, IT IS NEARER.
			if (++this.pulsing >= PULSE) {
				this.pulsing = 0;
				this.step(seen);
			}
		} else {
			this.pulsing = 0;
		}
	}

	/** Vanilla's DARKNESS period. The screen dips once a second. */
	private static final int PULSE = 20;

	/**
	 * How far it comes on each dip, and how near it will get this way.
	 *
	 * FOUR AND A HALF, WHICH IS ITS WALKING PACE. At two and a half the arithmetic
	 * came out the wrong way round: 0.25 movement speed is about 4.9 blocks a
	 * second, so staring closed 2.5 a second and LOOKING AWAY WAS WORSE. The
	 * punishment for solving the puzzle has to cost about what the puzzle costs, or
	 * it is not a punishment, it is a discount.
	 *
	 * Matched to the walk, so neither answer is the answer. The difference between
	 * them is not speed any more, it is that one of them you can see.
	 */
	private static final double STEPS_IN = 4.5;
	private static final double NO_NEARER = 2.0;

	private int pulsing;

	/**
	 * THE PUNISHMENT FOR LOOKING, AND IT CLOSES THE LAST WAY OUT.
	 *
	 * The creature had exactly one rule: frozen while watched, closing when not. A
	 * player who worked that out had a perfect answer — keep it on screen and it
	 * can never reach you — and a monster with a perfect answer is a puzzle that
	 * has been solved.
	 *
	 * So staring costs. Past STARE_COSTS it hands out DARKNESS, and DARKNESS is
	 * the Warden's effect: it PULSES, the screen dips about once a second, and on
	 * every dip this moves. Two and a half blocks, instantly, with no walk — it is
	 * frozen, so it cannot be walking, and something that is nearer without having
	 * crossed the distance is the whole of what this creature is for.
	 *
	 * Now there is no safe action. Look away and it walks at you. Keep looking and
	 * it arrives in the dark between blinks.
	 *
	 * NO_NEARER stops it at two blocks. Inside that it is in reach and REACHES has
	 * already taken the freeze off, so stepping further would be teleporting into
	 * somebody's face — which reads as a bug rather than as dread.
	 */
	private void step(Player seen) {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		Vec3 gap = seen.position().subtract(this.position());
		double away = gap.horizontalDistance();
		if (away <= NO_NEARER + STEPS_IN) {
			return;
		}
		Vec3 to = this.position().add(gap.normalize().scale(STEPS_IN));
		// Down to whatever it lands on, and never up through a ceiling. A blind
		// step into a hillside is how a stalker ends up inside the terrain.
		BlockPos foot = BlockPos.containing(to.x, this.getY(), to.z);
		for (int drop = 0; drop <= 3; drop++) {
			BlockPos at = foot.below(drop);
			if (here.getBlockState(at).isAir()
				&& here.getBlockState(at.above()).isAir()
				&& here.getBlockState(at.below()).isSolid()) {
				this.snapTo(to.x, at.getY(), to.z, this.getYRot(), this.getXRot());
				this.getNavigation().stop();
				here.playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.WARDEN_STEP, this.getSoundSource(), 0.9F, DEEP);
				return;
			}
		}
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
	/** How far down the anger is pitched. Below 0.5 the engine resamples badly. */
	private static final float DEEP = 0.52F;
	private static final int SPEAKS_MIN = 70;
	private static final int SPEAKS_SPREAD = 90;

	private int speaksIn = SPEAKS_MIN;

	/**
	 * SILENT UNTIL IT HAS DECIDED, AND THEN VERY LOW.
	 *
	 * The Turned will not stop muttering — villager ambience pitched down, so you
	 * hear one before you see it and the sound is the warning. This one has no
	 * warning at all while it is only standing there. The only way to find it is
	 * to look at where it is, which is the same act that stops it.
	 *
	 * When it takes a target that reverses. The enderman scream at roughly half
	 * pitch, which drops it about an octave and stretches it to twice the length —
	 * the resampler turns a shriek into something slow and wrong, and slow and
	 * wrong is the register this whole creature works in.
	 *
	 * Half pitch is also the floor. Below it the engine's resampling starts to
	 * tear, and what comes out is a artefact rather than a voice.
	 */
	@Override
	protected @org.jspecify.annotations.Nullable SoundEvent getAmbientSound() {
		return null;      // nothing at all until it wants something
	}

	/** How often it lets the rock know. Roughly every eight to fourteen seconds. */
	private static final int ECHOES_MIN = 160;
	private static final int ECHOES_SPREAD = 120;
	/** And how far the sound carries. Far enough to be followed. */
	private static final float ECHO_CARRIES = 5.0F;

	private int echoesIn = ECHOES_MIN;

	/**
	 * WHAT IT SOUNDS LIKE UNDERGROUND, WHICH IS THE ONLY WAY YOU FIND IT.
	 *
	 * Silence is the right rule out in a forest — the whole creature is built on
	 * there being no warning, and the only way to find it there is to look at where
	 * it already is. Underground that rule stops working and starts hiding it: a
	 * player in a warren has no sightlines at all, so a thing that makes no noise
	 * in a tunnel is a thing nobody ever meets.
	 *
	 * So below the surface it carries. Warden heartbeat pitched down and played
	 * loud enough to travel through stone, on a slow irregular clock — which gives
	 * a player the one thing a tunnel system can use, a direction to walk in. And
	 * it is cursed rather than hostile: soft, low, patient, and coming from further
	 * in every time you stop to listen.
	 *
	 * IRREGULAR ON PURPOSE. A metronome is a mechanic and a player starts counting
	 * it; something that goes quiet for eleven seconds and then does not is a thing
	 * you keep turning round for.
	 */
	private void echo() {
		if (this.isSilent() || !(this.level() instanceof ServerLevel here)) {
			return;
		}
		if (here.canSeeSky(this.blockPosition())) {
			this.echoesIn = ECHOES_MIN;
			return;      // out under the sky it says nothing, as before
		}
		if (--this.echoesIn > 0) {
			return;
		}
		this.echoesIn = ECHOES_MIN + this.random.nextInt(ECHOES_SPREAD);
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, this.getSoundSource(),
			ECHO_CARRIES, DEEP);
		// And the shape of the room answers it. Two beats, quieter and late, so
		// what reaches a player down a passage is a sound and then the rock.
		com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), 9, () ->
			here.playSound(null, this.getX(), this.getY(), this.getZ(),
				net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
				this.getSoundSource(), ECHO_CARRIES * 0.45F, DEEP * 0.9F));
	}

	private void speak() {
		if (this.getTarget() == null || this.isSilent()) {
			this.speaksIn = SPEAKS_MIN;
			return;
		}
		if (--this.speaksIn > 0) {
			return;
		}
		this.speaksIn = SPEAKS_MIN + this.random.nextInt(SPEAKS_SPREAD);
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.ENDERMAN_SCREAM, this.getSoundSource(), 1.1F,
			DEEP + this.random.nextFloat() * 0.05F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDERMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDERMAN_DEATH;
	}

	/**
	 * Everything it makes comes out an octave down, not just the anger.
	 *
	 * getVoicePitch is what LivingEntity multiplies into hurt and death, and left
	 * alone it randomises around 1.0 — so a creature whose one utterance is pitched
	 * to 0.52 would yelp at full pitch the moment it was hit, which is a different
	 * animal making the noise.
	 */
	@Override
	public float getVoicePitch() {
		return DEEP;
	}

	/**
	 * How hard it throws, straight up, in blocks per tick.
	 *
	 * THIS NUMBER IS A FALL, NOT A HEIGHT, and that is the trap in it. A player
	 * launched at v rises roughly 1.25 * (v / 0.42) ^ 2 blocks — a normal jump is
	 * 0.42 and 1.25 blocks — and then comes down, and everything above three blocks
	 * of that descent is damage. So a satisfying-looking 1.0 is seven blocks up and
	 * four points of fall on the way back, which would quietly hand back the five
	 * points we just took off the attack and leave the thing exactly as lethal as
	 * it was, with an extra animation.
	 *
	 * 0.8 is about four and a half blocks: unmistakably thrown, a real moment of
	 * being airborne and not in charge, and a landing that costs a point or two.
	 * Four from the blow plus that is still well under the nine it used to hit for.
	 */
	private static final double THROWN = 0.8;

	/**
	 * It hits you off your feet rather than through your armour.
	 *
	 * An iron golem is the reference the whole creature is being pointed at now:
	 * slow, heavy, and the thing everybody remembers about it is the arc, not the
	 * hit. You do not die to a golem, you get put somewhere else, and the fright is
	 * that you are no longer standing where you had planned to be standing.
	 *
	 * The knockback attribute handles the horizontal — this is only the lift, and
	 * it has to be added AFTER super, because the damage call is what applies the
	 * ordinary knockback and it writes delta movement rather than adding to it.
	 */
	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		if (!super.doHurtTarget(level, target)) {
			return false;
		}
		target.setDeltaMovement(target.getDeltaMovement().add(0.0, THROWN, 0.0));
		target.hurtMarked = true;   // tells the server to send the player the shove
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.IRON_GOLEM_ATTACK, this.getSoundSource(), 1.0F, DEEP);
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer killer) {
			HerobrineMod.LOGGER.info("{} put the tall one down", killer.getName().getString());
		}
	}
}
