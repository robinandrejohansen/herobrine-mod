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
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			// A villager's health exactly. He is a man with an axe, not a boss,
			// and the fight should be over in the four or five hits it takes to
			// kill anything else that walks into you.
			.add(Attributes.MAX_HEALTH, 20.0)
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

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// No door-opening and no door-breaking. He is one of them and he lives
		// here — a locked door is a real answer to him, which is the difference
		// between this and the hunt, and it is what keeps a village at night
		// survivable rather than a siege.
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		// Awake, and visibly with nothing to do. Villagers at night are in
		// their beds; the whole event is one man walking the square.
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.6));
		this.targetSelector.addGoal(1, new NightWatch(this));
		this.carry();
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
	private static class NightWatch extends net.minecraft.world.entity.ai.goal.Goal {
		private final TurnedEntity him;

		NightWatch(TurnedEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
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
			this.him.setTarget(quarry);
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
	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		this.talk();
		if (this.getTarget() != null) {
			return;      // he is busy
		}
		Player watching = this.level().getNearestPlayer(this, NOTICES);
		if (watching == null || !this.hasLineOfSight(watching)) {
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
		if (source.getEntity() instanceof Player) {
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
