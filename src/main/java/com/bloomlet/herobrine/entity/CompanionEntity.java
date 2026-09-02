package com.bloomlet.herobrine.entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * SOMEBODY WHO COMES WITH YOU.
 *
 * The one friendly thing in the mod, and the reason she exists is a problem the
 * books could not solve on their own. The whole story is written down — twenty-two
 * books, six of them Steve's — and a player finds them one at a time, hours
 * apart, and has to read them. She says it out loud instead, in the place it
 * happened, at the moment it matters. See Sayings.
 *
 * SHE CANNOT DIE, AND SHE MUST NEVER LOOK IMMORTAL. Asked for exactly that way,
 * and the two halves are both load-bearing. A companion who can be lost for good
 * turns every corridor into a thing you are failing at; a companion who visibly
 * shrugs off a Gaunt is furniture. So: real health, real damage, real blood, and
 * a floor at LOWEST that the player never sees the number of. When she is nearly
 * gone she breaks off and runs — that is the part you remember — and then she
 * sits down somewhere out of the way and eats, and comes back.
 *
 * WHAT KEEPS UP WITH A SPRINT. Base speed a little over a wolf's, a catch-up
 * modifier when she has fallen behind, and a teleport as the backstop. The
 * teleport is not a cheat and every tamed animal in the game does it: pathing
 * cannot be trusted across a ravine at eight blocks a second, and a companion
 * who is reliably lost is worse than no companion.
 *
 * THE SILHOUETTE IS WHAT IDENTIFIES HIM, NOT THE COLOUR. tools/gen_addexio.py
 * has the reasoning: every other humanoid in this mod wears a ROBE — villager
 * brown, the Turned's ashen grey, the Gaunt's three blocks of it — and a player
 * who cannot tell him from a Turned across a clearing will kill him. He is the
 * only human-shaped thing on your side of the world: separate arms, a pale
 * tunic, a strap across the chest, boots.
 *
 * HE WAS A VILLAGER IN A RED COAT AND HE WAS CALLED VERA. That version identified
 * by hue instead — red because nothing else in the mod was — and left the head
 * exactly vanilla so that the day a mimic wore her face there was nothing to
 * spot. It worked and it cost him a face: a villager head is a nose the size of
 * a fist and no expression, and a companion this mod holds a four-minute vigil
 * over cannot be a trade menu with legs. A name does that work better than a
 * disguise ever did — a plate reading Addexio over the wrong thing is colder
 * than an unnamed villager standing where one should not be.
 */
public class CompanionEntity extends PathfinderMob {
	/** A person's health, and a person's armour. She is not a boss. */
	private static final double LIVES = 24.0;

	/**
	 * The floor. Damage is clamped so it can never carry her below this.
	 *
	 * One heart short of nothing. Low enough that the hurt animation, the red
	 * flash and the flight all read as a death she is about to have, and hard
	 * enough that she does not have it.
	 */
	private static final float LOWEST = 2.0F;

	/** She breaks off below this, and does not come back until RECOVERED. */
	private static final float FALTERS_UNDER = 9.0F;
	private static final float RECOVERED = 20.0F;

	/** How far she lets you get before she stops strolling and starts running. */
	private static final double DAWDLES_WITHIN = 4.0;
	private static final double HURRIES_AFTER = 9.0;
	private static final double GIVES_UP_AND_APPEARS = 26.0;

	/** Who she is with. Persistent, because she has to still be yours tomorrow. */
	private static final AttachmentType<String> WITH =
		AttachmentRegistry.createPersistent(HerobrineMod.id("companion_with"),
			Codec.STRING);

	public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.setCustomName(Component.literal("Addexio"));
		// ALWAYS ON, and it is the single most important line in the file.
		//
		// The mod is full of humanoids that want to kill you and one that does
		// not, they are all built on the same villager mesh, and the difference
		// at thirty blocks in fog is a name floating over her head.
		this.setCustomNameVisible(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, LIVES)
			// A WOLF'S PACE AND A BIT. A tamed wolf at 0.3 keeps up with a
			// sprinting player and this has to as well, with the catch-up modifier
			// in Follow doing the rest. Too much and she runs ahead, which reads as
			// her leading you, and she is not leading you anywhere.
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			// Wide, because she has to notice you have walked off across a field.
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// FALTER OUTRANKS FOLLOW. When she is nearly finished she stops being your
		// companion and becomes a frightened person, and that inversion is the
		// whole effect — you notice her leaving.
		this.goalSelector.addGoal(1, new Falter(this));
		this.goalSelector.addGoal(2, new Follow(this));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	// ---- WHO SHE IS WITH ---------------------------------------------------

	public void goWith(Player player) {
		this.setAttached(WITH, player.getUUID().toString());
	}

	public boolean isSpokenFor() {
		return this.getAttached(WITH) != null;
	}

	/** The player she is with, if they are in this level right now. */
	public @org.jspecify.annotations.Nullable Player companion() {
		String who = this.getAttached(WITH);
		if (who == null) {
			return null;
		}
		try {
			return this.level().getPlayerByUUID(UUID.fromString(who));
		} catch (IllegalArgumentException bad) {
			return null;
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(this.level() instanceof ServerLevel here) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (!this.isSpokenFor()) {
			this.goWith(player);
			Sayings.say(here, this, player, Sayings.JOINING);
			HerobrineMod.LOGGER.info("{} is coming with {}",
				this.getName().getString(), player.getName().getString());
			return InteractionResult.SUCCESS;
		}
		Sayings.say(here, this, player, Sayings.NUDGED);
		return InteractionResult.SUCCESS;
	}

	// ---- BEING HURT --------------------------------------------------------

	/**
	 * She takes it, and it never finishes her.
	 *
	 * Clamped HERE rather than in a tick check, because a tick check heals her
	 * back up AFTER she has died and the death has already been broadcast: the
	 * sound plays, the animation runs, and on a server everybody reads it in the
	 * chat. Nothing downstream of this ever sees a lethal number.
	 */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		float room = Math.max(0.0F, this.getHealth() - LOWEST);
		if (damage >= room) {
			damage = room;
			if (damage <= 0.0F) {
				// Already at the floor. Still flash and still make the noise —
				// silence here would give the immortality away immediately.
				this.hurtTime = this.hurtDuration = 10;
				this.playHurtSound(source);
				return false;
			}
		}
		return super.hurtServer(level, source, damage);
	}

	// NO VOID EXEMPTION, AND THE FIRST ATTEMPT HAD ONE THE WRONG WAY ROUND.
	//
	// Making her invulnerable to the void does not save her from it — it means she
	// falls out of the world for ever at two hearts instead of dying at the bottom
	// of it, which is losing her with extra steps. The clamp already stops the
	// death; what she needs is to be picked up, and Company does that by height.

	/** When she last said anything. Sayings gates on this; see QUIET_FOR. */
	long lastSpoke;

	public boolean isFaltering() {
		return this.getHealth() < FALTERS_UNDER;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	// ---- FOLLOWING --------------------------------------------------------

	/**
	 * Walk when she can, run when she has to, appear when she cannot.
	 *
	 * Three bands rather than one speed. A companion pinned at a fixed distance
	 * looks like a camera on a boom; one that ambles when you amble and breaks
	 * into a run when you get away from her looks like a person keeping up.
	 */
	private static final class Follow extends Goal {
		private final CompanionEntity her;
		private int repath;

		Follow(CompanionEntity her) {
			this.her = her;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			Player with = this.her.companion();
			return with != null && !with.isSpectator()
				&& this.her.distanceTo(with) > CompanionEntity.DAWDLES_WITHIN;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void stop() {
			this.her.getNavigation().stop();
		}

		@Override
		public void tick() {
			Player with = this.her.companion();
			if (with == null) {
				return;
			}
			this.her.getLookControl().setLookAt(with, 30.0F, 30.0F);
			double away = this.her.distanceTo(with);

			if (away > GIVES_UP_AND_APPEARS && this.her.level() instanceof ServerLevel here) {
				this.appearNear(here, with);
				return;
			}

			if (--this.repath > 0) {
				return;
			}
			this.repath = 10;
			// The catch-up modifier. A sprinting player pulls away from 1.0 no
			// matter what the base attribute is, because they are also going in a
			// straight line and she is going round things.
			double pace = away > HURRIES_AFTER ? 1.4 : 1.0;
			this.her.getNavigation().moveTo(with, pace);
		}

		/**
		 * The backstop every tamed animal in the game has.
		 *
		 * Not a convenience. Pathing cannot hold a player crossing a ravine at
		 * eight blocks a second, and a companion who is reliably lost behind
		 * terrain is worse than no companion at all — the player stops trusting
		 * her and starts managing her.
		 */
		private void appearNear(ServerLevel here, Player with) {
			net.minecraft.util.RandomSource roll = this.her.getRandom();
			// A WIDER LOOK THAN THREE BLOCKS. Somebody moving fast is not going to
			// have solid ground in the seven-block box they happen to be over.
			for (int tries = 0; tries < 24; tries++) {
				BlockPos at = with.blockPosition().offset(
					roll.nextInt(11) - 5, roll.nextInt(7) - 3, roll.nextInt(11) - 5);
				if (!here.getBlockState(at).isAir()
					|| !here.getBlockState(at.above()).isAir()
					|| !here.getBlockState(at.below()).isSolid()) {
					continue;
				}
				this.her.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
					this.her.getYRot(), this.her.getXRot());
				this.her.getNavigation().stop();
				return;
			}
			// AND IF THERE IS NO FLOOR AT ALL, GO ANYWAY.
			//
			// Reported as her not keeping up with a player FLYING IN CREATIVE, and
			// this was the whole of it: every candidate had to have a solid block
			// under it, and a player two hundred blocks up over open air has none
			// within reach. All twenty-four tries failed, every tick, so the one
			// mechanism that exists to stop her being lost never fired — and the
			// faster you went the further behind she stayed.
			//
			// Landing her in mid-air and letting her fall is not elegant. It is also
			// unambiguously better than the alternative, which is losing her: she
			// cannot die, Company fishes her out from under the world, and Follow
			// will simply do this again on the way down.
			this.her.snapTo(with.getX(), with.getY(), with.getZ(),
				this.her.getYRot(), this.her.getXRot());
			this.her.setDeltaMovement(Vec3.ZERO);
			this.her.getNavigation().stop();
		}
	}

	// ---- BREAKING OFF -----------------------------------------------------

	/**
	 * She runs, and then she eats, and then she comes back.
	 *
	 * The whole point of her being unkillable is that this has to carry the
	 * weight instead, so it is built to be watched rather than to be efficient:
	 * she turns and goes, she puts distance between herself and the thing, and
	 * then she stands somewhere with a loaf in her hands for the better part of
	 * a minute while you finish the fight without her.
	 *
	 * RECOVERED is well above FALTERS_UNDER on purpose. Equal thresholds make her
	 * flicker in and out of fleeing on a single hit, which reads as a broken mob
	 * rather than a frightened person.
	 */
	private static final class Falter extends Goal {
		private static final double FLEES = 12.0;
		private static final int BITE_EVERY = 25;
		private static final float A_BITE = 1.5F;

		private final CompanionEntity her;
		private int chewing;

		Falter(CompanionEntity her) {
			this.her = her;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.her.getHealth() < FALTERS_UNDER;
		}

		@Override
		public boolean canContinueToUse() {
			return this.her.getHealth() < RECOVERED;
		}

		@Override
		public void start() {
			if (this.her.level() instanceof ServerLevel here) {
				Player with = this.her.companion();
				if (with != null) {
					Sayings.say(here, this.her, with, Sayings.FALTERING);
				}
			}
		}

		@Override
		public void stop() {
			this.her.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			this.her.getNavigation().stop();
			if (this.her.level() instanceof ServerLevel here) {
				Player with = this.her.companion();
				if (with != null) {
					Sayings.say(here, this.her, with, Sayings.BACK_UP);
				}
			}
		}

		@Override
		public void tick() {
			Mob threat = this.nearestThreat();
			if (threat != null) {
				this.her.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				this.her.getLookControl().setLookAt(threat, 30.0F, 30.0F);
				if (this.her.getNavigation().isDone()) {
					Vec3 out = DefaultRandomPos.getPosAway(this.her, 16, 7,
						threat.position());
					if (out != null) {
						this.her.getNavigation().moveTo(out.x, out.y, out.z, 1.6);
					}
				}
				return;
			}
			// Nothing near. Sit down and eat.
			this.her.getNavigation().stop();
			this.her.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
			if (++this.chewing < BITE_EVERY) {
				return;
			}
			this.chewing = 0;
			this.her.heal(A_BITE);
			this.her.level().playSound(null, this.her.getX(), this.her.getY(),
				this.her.getZ(), SoundEvents.GENERIC_EAT, this.her.getSoundSource(),
				0.8F, 1.0F + (this.her.getRandom().nextFloat() - 0.5F) * 0.2F);
		}

		private @org.jspecify.annotations.Nullable Mob nearestThreat() {
			AABB near = this.her.getBoundingBox().inflate(FLEES);
			List<Mob> found = this.her.level().getEntitiesOfClass(Mob.class, near,
				m -> m != this.her && m.isAlive() && Sayings.isHis(m));
			return found.stream()
				.min(Comparator.comparingDouble(this.her::distanceToSqr))
				.orElse(null);
		}
	}

	/**
	 * ANYTHING WITH A HEALTH BAR CAN HURT HER, and one thing on purpose cannot.
	 *
	 * She is not a player, so nothing in the mod targeted her: every hostile here
	 * asks for Player.class. Which would have made the whole of Falter dead code —
	 * real health, a flee, a loaf of bread, and nothing in the world able to take
	 * a single point off her. See ModEntities, where the Gaunt now looks for both.
	 */
	static boolean canBeHurtBy(LivingEntity who) {
		return who instanceof Player || who instanceof CompanionEntity;
	}
}
