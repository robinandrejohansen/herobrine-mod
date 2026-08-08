package com.bloomlet.herobrine.entity;

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
	private static final double WATCH_RANGE = 64.0;

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
			.add(Attributes.FOLLOW_RANGE, 64.0);
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
		for (Player watcher : watchers) {
			if (inViewOf(watcher)) {
				seen = true;
				break;
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

		if (closest != null && closestDistance < TOO_CLOSE) {
			// Everyone who closed in paid for it, not only the one who got
			// there first. Two people walking him down is twice the defiance,
			// which is the correct price for twice the pressure.
			for (Player watcher : watchers) {
				if (this.distanceTo(watcher) < TOO_CLOSE && watcher instanceof ServerPlayer chaser) {
					WrathTriggers.defiance(chaser, DEFIANCE_APPROACHED);
				}
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

		// He was always standing there and he goes on standing there. Moving
		// while watched would break the only claim the whole event makes.
		this.getNavigation().stop();

		if (watchers.isEmpty()) {
			this.vanish();
			return;
		}

		if (seen) {
			this.unseenTicks = 0;
			// EARLY ON, BEING LOOKED AT IS ENOUGH TO END IT.
			//
			// At WATCHER he is gone a second and a half after the first pair of
			// eyes lands on him — enough to find a shape, nowhere near enough to
			// be sure of anything. That is the correct first encounter: the
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
			if (allowed > 0 && ++this.watchedTicks > allowed) {
				this.vanish();
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
			this.vanish();
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
			case RUMOUR, WATCHER -> 30;
			case TRESPASSER -> 50;
			case MIMIC -> 120;
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
			this.vanish();
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
			this.vanish();
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
			this.vanish();
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

	private void vanish() {
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
