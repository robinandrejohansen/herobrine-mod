package com.bloomlet.herobrine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
	private int watchedTicks;
	/** Ticks since he arrived. */
	private int age;
	/** Whether any player actually laid eyes on him before he left. */
	private boolean witnessed;

	/**
	 * He leaves on his own after this long, seen or not.
	 *
	 * A haunting is a moment. Left indefinitely he becomes scenery — you walk
	 * over, study him, and discover he does nothing, which is the end of being
	 * afraid of him. Better to be gone before the player is certain of what
	 * they saw.
	 */
	private static final int LIFETIME = 600;          // 30 seconds

	/** Get closer than this and he is simply not there any more. */
	private static final double TOO_CLOSE = 8.0;

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
	 * How often his arrival makes a sound, one in N.
	 *
	 * Not always, on purpose. A reliable cue becomes a tell — players learn
	 * "noise means he is behind me" and it stops being a fright and starts
	 * being a mechanic. At one in three you cannot trust it to mean anything,
	 * so it never resolves into a signal.
	 */
	private static final int CUE_CHANCE = 3;

	/**
	 * How precisely you must be looking at him for it to count.
	 *
	 * Vanilla's Enderman formula is 0.025/distance, which at 18 blocks is
	 * about 3 degrees — appropriate for a mob you are meant to aggro by
	 * accident, far too strict for one whose whole behaviour is reacting to
	 * being seen. This is roughly 7 degrees at stalking range and still
	 * tightens with distance, so a glance across a valley does not count.
	 */
	private static final double GAZE_TOLERANCE = 0.15;

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
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new StalkPlayerGoal(this, 18.0, 32.0));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 64.0F));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}

		// Gone on his own, before he can become furniture.
		if (++this.age > LIFETIME) {
			this.vanish();
			return;
		}

		Player nearest = this.level().getNearestPlayer(this, 48.0);

		// Did anyone actually see him? Not "was he rendered" — was he in
		// someone's view, unobstructed. A visit nobody perceived should not
		// count against the pacing budget (see ManifestationDirector).
		if (nearest != null && !this.witnessed && inViewOf(nearest)) {
			this.witnessed = true;
		}

		// You never get to reach him. Walking up to something that does not
		// react is how a threat becomes an exhibit.
		if (nearest != null && this.distanceTo(nearest) < TOO_CLOSE) {
			if (nearest instanceof ServerPlayer chaser) {
				WrathTriggers.defiance(chaser, DEFIANCE_APPROACHED);
			}
			this.vanish();
			return;
		}

		if (nearest != null && isLookingAtMe(nearest)) {
			// Hold still while watched — moving would break the illusion that
			// he was always standing there.
			this.getNavigation().stop();
			if (++this.watchedTicks > 40) {
				this.vanish();
			}
		} else {
			this.watchedTicks = 0;
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
		return look.dot(toMe) > 0.55 && player.hasLineOfSight(this);
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

	private void vanish() {
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
			com.bloomlet.herobrine.manifest.ManifestationDirector.wasted(
				com.bloomlet.herobrine.manifest.Manifestation.THE_STARE);
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
	 * The further away he is, the tighter the angle has to be, so a distant
	 * glance does not count. Requires unobstructed line of sight — looking at
	 * a wall he happens to be behind is not seeing him.
	 */
	private boolean isLookingAtMe(Player player) {
		Vec3 view = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		);
		double distance = toMe.length();
		double dot = view.dot(toMe.normalize());
		return dot > 1.0 - GAZE_TOLERANCE / distance && player.hasLineOfSight(this);
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
			this.vanish();
		}
		return false;
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
