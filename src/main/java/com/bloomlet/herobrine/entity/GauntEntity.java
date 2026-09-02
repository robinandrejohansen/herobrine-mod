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

	/**
	 * TWO THINGS THE CLIENT HAS TO KNOW, AND IT CANNOT WORK EITHER OUT ITSELF.
	 *
	 * Everything this creature does is decided on the server — who is watching it,
	 * when it speaks — and both of those now change how it is DRAWN. The renderer
	 * runs on the client off a render state copied out of a client-side entity, and
	 * a plain field on the server object is not in it.
	 *
	 * So the two are synched. Cheap: a boolean and a byte, sent only when they
	 * change, on a creature there is one of.
	 *
	 * STARING is "it has turned round and is looking back at somebody", which is
	 * exactly watchedFor > 0 and is the moment the head goes over — see
	 * GauntRenderer.tilt.
	 *
	 * VOICE counts down from whatever made the last noise. The client reads it as a
	 * fraction and swells the head on it, so the mouth opens ON the sound rather
	 * than near it.
	 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STARING =
		net.minecraft.network.syncher.SynchedEntityData.defineId(GauntEntity.class,
			net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
	private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> VOICE =
		net.minecraft.network.syncher.SynchedEntityData.defineId(GauntEntity.class,
			net.minecraft.network.syncher.EntityDataSerializers.BYTE);

	/** How long the head stays swollen after a noise. Twelve ticks, and it falls. */
	private static final int VOICE_TICKS = 12;

	@Override
	protected void defineSynchedData(
			net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STARING, false);
		builder.define(VOICE, (byte) 0);
	}

	/** Called wherever it makes a noise, so the mouth is on the same tick as the sound. */
	private void spoke() {
		this.entityData.set(VOICE, (byte) VOICE_TICKS);
	}

	public boolean staring() {
		return this.entityData.get(STARING);
	}

	/** 1 the tick it speaks, falling to 0. What the head swells on. */
	public float voice() {
		return this.entityData.get(VOICE) / (float) VOICE_TICKS;
	}

	/**
	 * TEN SECONDS BEFORE IT MAY TOUCH ANYBODY, COUNTED FROM THE FIRST MEETING.
	 *
	 * The first thing this creature does to a player is the only thing it needs to
	 * do, and that is be there. A hit inside the first few seconds spends the whole
	 * encounter as arithmetic — how much did that take, what have I got left — and
	 * everything the standing and the staring and the closing was for is gone,
	 * traded for a number in the corner of the screen.
	 *
	 * So the first ten seconds cost nothing at all. It closes, it stares, the
	 * screen dips, the heartbeat lands, the floor answers its feet — and it cannot
	 * hurt you while any of that is happening. Whatever you decide in that window
	 * you decide out of what it looks like, not out of what it did.
	 *
	 * IT STARTS ONCE AND IT PERSISTS. Not a cooldown and not per-player: the clock
	 * belongs to the creature, runs once in its life, and is written to disk. A
	 * field that reset on reload would hand out another free ten seconds for the
	 * price of quitting to the title screen, which is the shape of every exploit
	 * this mod has had.
	 */
	private static final int HOLDS_OFF = 200;

	private int met;

	/**
	 * THE CELL DOOR, IF THIS IS THE ONE IN THE GAOL.
	 *
	 * Null for every other one of these. TheDig hands it the position of the iron
	 * door that is holding it, at the moment it is put in the room, because that is
	 * the only point at which anybody knows which of fourteen doors it is.
	 */
	private @org.jspecify.annotations.Nullable BlockPos cell;

	/** How close somebody has to get before the bolt goes over on its own. */
	private static final int LETS_ITSELF_OUT = 7;

	public void keptBehind(BlockPos door) {
		this.cell = door;
	}

	/**
	 * AND THE DOOR OPENS BY ITSELF.
	 *
	 * A shut iron door and a lever four blocks away is a decision, and it was the
	 * right one to offer — but it is a decision made from OUTSIDE, in a corridor,
	 * about a room. Nothing about it happens to you. You throw a switch and then
	 * you look.
	 *
	 * This is the other version of the same moment and it costs nothing: walk up to
	 * the one shut cell in a hall of thirteen open ones, and the bolt goes over on
	 * its own. The lever still works, and anybody who finds it first still gets to
	 * choose. What this removes is the case where nobody ever pulls it and the
	 * building's one occupant is never met at all.
	 *
	 * ONCE, AND THEN NEVER AGAIN. The reference is dropped the moment it fires, so
	 * a player who shuts the door behind them has shut it — the door does not fight
	 * them for it, and a door that reopens every time you close it is a joke rather
	 * than a fright.
	 *
	 * DoorBlock.setOpen rather than two setBlock calls. An iron door is two block
	 * states that have to agree, and it also wants the sound: the vanilla method
	 * does both halves and plays the latch, which is the part you actually hear
	 * happen behind you.
	 */
	private void unbolt() {
		this.unbolt(false);
	}

	private void unbolt(boolean regardless) {
		if (this.cell == null || !(this.level() instanceof ServerLevel here)) {
			return;
		}
		// `regardless` is the one that is let out because somebody killed its
		// neighbour. Nobody has to be standing at ITS door for that — the point is
		// that it happens somewhere up the hall behind you.
		if (!regardless) {
			Player near = here.getNearestPlayer(this.cell.getX() + 0.5,
				this.cell.getY() + 0.5, this.cell.getZ() + 0.5, LETS_ITSELF_OUT, false);
			if (near == null) {
				return;
			}
		}
		net.minecraft.world.level.block.state.BlockState was =
			here.getBlockState(this.cell);
		if (was.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door
			&& !door.isOpen(was)) {
			door.setOpen(this, here, was, this.cell, true);
			HerobrineMod.LOGGER.info("the cell door at [{}, {}, {}] opened on its own",
				this.cell.getX(), this.cell.getY(), this.cell.getZ());
		}
		this.cell = null;
	}

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
			// you — and that is a stat, not a scare. Four was survivable. Two is
			// nothing, and nothing is the number this wants.
			//
			// ONE HEART, AND NO FALL ON TOP OF IT. Four plus the landing came to
			// about three hearts a swing, which is still a fight — you back off and
			// count, and once you are counting you are playing a health bar. At one
			// heart there is nothing to count. What happens to you is that you are
			// suddenly eight blocks away facing the wrong direction, and the only
			// thing it cost was the ground you were standing on.
			//
			// What you remember is the arc. See THROWN.
			.add(Attributes.ATTACK_DAMAGE, 2.0)
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
	/**
	 * IT HAS TO GET THROUGH A DOOR, AND AT 2.9 IT COULD NOT.
	 *
	 * A hitbox of 2.9 needs THREE clear blocks to path through. Every door in this
	 * mod is two. So the thing let itself out of its cell — the trapdoor opened,
	 * the latch sounded, everything worked — and then stood in the doorway forever,
	 * because vanilla's pathfinder will not route a body through a gap it does not
	 * fit in and there is no error when it refuses.
	 *
	 * Reported as "he won't move", and then as "the sound is gone", which was the
	 * same bug seen twice: tread() plays a footstep every 2.4 blocks of ground
	 * COVERED, so a creature that cannot leave a doorway is also a silent one.
	 *
	 * 1.95, which is a villager's, a player's, and the height every two-block
	 * opening in the game is cut for.
	 *
	 * THE MODEL STAYS THREE BLOCKS TALL AND OVERHANGS IT, deliberately. That is
	 * vanilla's own trick — an enderman is 2.9 with fifty units of mesh — and it is
	 * the whole silhouette: a thing that has to stoop through your door and then
	 * straightens up on the other side. The alternative was shrinking the creature
	 * to fit its own hitbox, which is the one thing everybody has asked me not to
	 * do to it.
	 */
	public static final float TALL = 1.95F;

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
		// This asked for Player.class, and so does everything else in the mod. Addexio
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
		this.tread();
		this.unbolt();

		byte left = this.entityData.get(VOICE);
		if (left > 0) {
			this.entityData.set(VOICE, (byte) (left - 1));
		}

		Player seen = this.watcher();
		this.entityData.set(STARING, seen != null);
		// THE CLOCK STARTS THE MOMENT IT IS IN THE ROOM WITH SOMEBODY.
		//
		// Either half will do to start it — being looked at, or having decided
		// about somebody — and it has to be either, not just the first. Being seen
		// alone would leave a hole for the one thing this creature is built to do:
		// close on you while you are facing the other way. It would arrive having
		// never been looked at, with a clock that had never started, and hold off
		// forever.
		if (this.met < HOLDS_OFF && (seen != null || this.getTarget() != null)) {
			this.met++;
		}
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
				this.beat();
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
				// tread() measures ground covered, and this covered none of it — the
				// whole point is that it did not walk. Left alone, four and a half
				// blocks of teleport would register as two strides and lay a pair of
				// footsteps over a thing that is standing perfectly still.
				this.lastX = this.getX();
				this.lastZ = this.getZ();
				this.strode = 0.0;
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
		// ONE HEARTBEAT AT A TIME. beat() is on the same sound, once a second, from
		// the moment the staring starts to cost — and two clocks on one sample, one
		// of them irregular, is not two sounds, it is mud. The slow one yields.
		if (this.watchedFor > STARE_COSTS) {
			this.echoesIn = ECHOES_MIN;
			return;
		}
		if (--this.echoesIn > 0) {
			return;
		}
		this.echoesIn = ECHOES_MIN + this.random.nextInt(ECHOES_SPREAD);
		this.spoke();
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

	/**
	 * THE DARK HAD NO SOUND IN IT, WHICH IS HALF OF WHAT DARKNESS IS FOR.
	 *
	 * Past STARE_COSTS the screen dips once a second and the thing is four and a
	 * half blocks nearer each time — and all of that was silent. A dip with nothing
	 * in it reads as a graphical fault. The player's own screen was the only thing
	 * telling them anything was happening, and a screen that flickers with no sound
	 * behind it is a bug, not a monster.
	 *
	 * So the beat lands ON the dip. Same clock, same tick, deliberately not
	 * randomised: this is the one moment in the creature's whole behaviour that is
	 * meant to feel mechanical, because something arriving on a schedule you can
	 * hear is worse than something arriving at random.
	 *
	 * Louder than the underground call. playSound treats volume above 1 as range —
	 * sixteen blocks per unit — so 1.6 carries about twenty-five, which is roughly
	 * the distance this thing stares from. It should be in the room with you.
	 */
	private static final float PULSE_CARRIES = 1.6F;

	private void beat() {
		if (this.isSilent() || !(this.level() instanceof ServerLevel here)) {
			return;
		}
		this.spoke();
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, this.getSoundSource(),
			PULSE_CARRIES, DEEP);
	}

	/** How far it goes between footfalls, in blocks. */
	private static final double STRIDE = 2.4;
	/** How late the room answers, and how much of it comes back. */
	private static final int TREAD_ECHO = 6;
	private static final float TREAD_BACK = 0.42F;
	private static final float TREAD_CARRIES = 0.7F;

	private double strode;
	private double lastX = Double.NaN;
	private double lastZ;

	/**
	 * IT WEIGHS THREE HUNDRED KILOS AND IT WALKED LIKE A CAT.
	 *
	 * The only footstep it ever had was on the teleport-step, so the one kind of
	 * movement that made a noise was the kind that crosses ground without walking
	 * over it. Ordinary walking — which is what it does for the entire approach,
	 * every time nobody is looking at it — was silent. A thing that size closing on
	 * you at a golem's pace should be audible before it is visible, and it was the
	 * other way round.
	 *
	 * MEASURED IN DISTANCE, NOT TICKS. A step every N ticks desynchronises from the
	 * actual walk the first time it is slowed, blocked, in water, or pathing round
	 * a tree — and then it is a metronome playing over a creature that is standing
	 * still. Every 2.4 blocks of ground actually covered cannot drift, because the
	 * ground covered is the thing being measured.
	 *
	 * Its own two doubles rather than xOld/zOld, which are vanilla's and are
	 * written at a point in the tick this method has no contract with. Four lines
	 * to not depend on somebody else's ordering.
	 *
	 * AND THE ROCK ANSWERS. Six ticks later at four tenths the volume and a shade
	 * flatter — the same trick echo() uses, and the reason both of them are in a
	 * mod whose creature lives underground. Minecraft has no reverb; two plays of
	 * one sound is the whole of it.
	 */
	private void tread() {
		double wasX = this.lastX;
		double wasZ = this.lastZ;
		this.lastX = this.getX();
		this.lastZ = this.getZ();
		if (Double.isNaN(wasX) || this.isSilent() || !this.onGround()
			|| !(this.level() instanceof ServerLevel here)) {
			return;
		}
		double dx = this.getX() - wasX;
		double dz = this.getZ() - wasZ;
		this.strode += Math.sqrt(dx * dx + dz * dz);
		if (this.strode < STRIDE) {
			return;
		}
		this.strode = 0.0;
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.WARDEN_STEP, this.getSoundSource(), TREAD_CARRIES, DEEP);
		com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), TREAD_ECHO, () ->
			here.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.WARDEN_STEP, this.getSoundSource(),
				TREAD_CARRIES * TREAD_BACK, DEEP * 0.92F));
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
		this.spoke();
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
	 * AND IT WAS NOT A FALL OF FOUR AND A HALF BLOCKS. IT WAS EIGHT.
	 *
	 * Because 0.8 was never the launch velocity. LivingEntity.knockback, which
	 * super has already run by the time this lands, writes
	 *
	 *     y = min(0.4, y / 2 + strength)      -> 0.4 for anyone on the ground
	 *
	 * and this then ADDED to it. So the player left at 1.2, not 0.8. Simulating the
	 * real tick loop — y += v; v = (v - 0.08) * 0.98 — rather than trusting the
	 * closed form:
	 *
	 *     v = 0.8   ->  3.97 blocks  ->  1 point
	 *     v = 1.2   ->  8.19 blocks  ->  6 points      <- what actually shipped
	 *
	 * Four from the blow and six from the landing is ten a swing: MORE than the
	 * nine this whole change was made to get away from, arriving by a door nobody
	 * was watching. The comment above was arithmetic done on the wrong number, and
	 * it read as a careful argument for a year.
	 *
	 * SO THE Y IS SET, NOT ADDED. x and z still come from super — the horizontal
	 * shove is vanilla's and it is right — but the height is written outright, so
	 * the arc is this constant and nothing else, whatever anybody upstream put
	 * there. Two systems adding into one field is the bug; one system owning it is
	 * the fix.
	 *
	 * 0.64 -> 2.67 blocks: two and an eighth times a normal jump, a long wrong
	 * second of not being in charge, and ceil(2.67 - 3) = 0 on the way down. The
	 * largest launch that still lands free is 0.685; this sits under it on purpose,
	 * so that being thrown onto ground a little lower than you left is still free
	 * too.
	 */
	private static final double THROWN = 0.64;

	/**
	 * It hits you off your feet rather than through your armour.
	 *
	 * An iron golem is the reference the whole creature is being pointed at now:
	 * slow, heavy, and the thing everybody remembers about it is the arc, not the
	 * hit. You do not die to a golem, you get put somewhere else, and the fright is
	 * that you are no longer standing where you had planned to be standing.
	 *
	 * The knockback attribute handles the horizontal. This is only the lift, and it
	 * has to happen AFTER super, because the damage call is what applies the
	 * ordinary knockback and it writes delta movement rather than adding to it.
	 */
	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		// BEFORE super, so nothing at all happens — no damage, no knockback, no
		// sound. It reaches you, it swings, and the swing goes through you. See
		// HOLDS_OFF: the first ten seconds of this creature are free.
		if (this.met < HOLDS_OFF) {
			return false;
		}
		if (!super.doHurtTarget(level, target)) {
			return false;
		}
		Vec3 shove = target.getDeltaMovement();
		target.setDeltaMovement(shove.x, THROWN, shove.z);
		target.hurtMarked = true;   // tells the server to send the player the shove
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.IRON_GOLEM_ATTACK, this.getSoundSource(), 1.0F, DEEP);
		return true;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Met", this.met);
		// The cell has to survive a reload or the one in the gaol loses its door
		// and goes back to waiting on the lever for good.
		if (this.cell != null) {
			output.putLong("Cell", this.cell.asLong());
		}
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		this.met = input.getIntOr("Met", 0);
		long door = input.getLongOr("Cell", 0L);
		this.cell = door == 0L ? null : BlockPos.of(door);
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	/**
	 * AND WHEN ONE GOES DOWN, THE NEXT DOOR GOES OVER.
	 *
	 * Four of them are shut in down there and only the first opens for you. Kill
	 * that one and a bolt draws somewhere further up the hall, and then you are
	 * standing in a corridor of thirteen empty cells and three that are not,
	 * knowing exactly what the sound was.
	 *
	 * The other three never have to be found, which is the good part: they let
	 * themselves out on your progress rather than on your searching, so the room
	 * fills up behind you whether you looked in it or not.
	 *
	 * NEAREST FIRST, so it walks up the hall towards you rather than opening the
	 * far end and leaving something to cross fourteen cells in the dark. It is a
	 * gaol, not a joke.
	 */
	private static final double SAME_GAOL = 96.0;

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer killer) {
			HerobrineMod.LOGGER.info("{} put the tall one down", killer.getName().getString());
		}
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		GauntEntity next = null;
		double nearest = Double.MAX_VALUE;
		for (GauntEntity other : here.getEntitiesOfClass(GauntEntity.class,
				this.getBoundingBox().inflate(SAME_GAOL),
				kept -> kept != this && kept.isAlive() && kept.cell != null)) {
			double away = this.distanceToSqr(other);
			if (away < nearest) {
				nearest = away;
				next = other;
			}
		}
		if (next != null) {
			next.unbolt(true);
		}
	}
}
