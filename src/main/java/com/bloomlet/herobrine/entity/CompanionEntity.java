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
	private static final double LIVES = 40.0;   // twenty hearts: he fell in seconds at twelve

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
	private static final double DAWDLES_WITHIN = 5.5;      // he does not stand on your heels
	private static final double HURRIES_AFTER = 14.0;      // a jog, and only when you are well ahead
	private static final double FIGHTS_APART = 24.0;      // in a fight he lets you range; he has his own
	private static final double GIVES_UP_AND_APPEARS = 40.0;
	/** How far he aims at a time while walking in. Inside what pathing solves. */
	private static final double LEG = 20.0;

	/**
	 * HE IS ALLOWED TO WALK IN, ONCE, AND THE TELEPORT IS SWITCHED OFF WHILE HE DOES.
	 *
	 * Company.arrives puts him down fifty-six to eighty-four blocks off so that you
	 * see a figure on a ridge and watch it come. Follow's backstop teleports him
	 * whenever he is more than twenty-six blocks out, which is correct for the whole
	 * rest of the mod and destroyed the arrival on its first tick: he was over the
	 * line the instant he existed, so the entrance was a man blinking into being
	 * four blocks from your face.
	 *
	 * Reported as "is addexio coming?" over a log line saying he was seventy-nine
	 * blocks off. He had already arrived. There was nothing to come.
	 *
	 * So the entrance gets a budget. While it lasts he walks and cannot teleport;
	 * when it runs out, or when he is close enough to be a person rather than a
	 * silhouette, the ordinary rules come back and never leave again.
	 *
	 * TWO MINUTES, WHICH IS FOUR TIMES WHAT THE WALK NEEDS. Eighty blocks at his
	 * pace is about twenty seconds on the flat, and this has to survive a mountain,
	 * a lake and a fence. The budget is not the plan, it is the give-up: if he is
	 * still out there after two minutes he is stuck on something and the teleport is
	 * the right answer after all.
	 */
	private static final int WALKS_IN_FOR = 2400;
	/** Near enough that watching him arrive is over and following begins. */
	private static final double ARRIVED_WITHIN = 10.0;

	private int walkingIn;

	public void beginTheWalkIn() {
		this.walkingIn = WALKS_IN_FOR;
	}

	/** True while the entrance is still happening and the teleport is held off. */
	public boolean walkingIn() {
		return this.walkingIn > 0;
	}

	/** How far below the local surface counts as down there rather than indoors. */
	private static final int TOO_DEEP = 8;
	private static final int SAYS_SO_EVERY = 200;

	private int excused;

	/**
	 * HE DOES NOT COME UNDERGROUND, AND HE NEVER COULD CROSS OVER.
	 *
	 * The dimension was already handled and by accident: companion() looks the
	 * player up with level().getPlayerByUUID, which is level-LOCAL, so the moment
	 * you step through the way he simply has no companion any more and Follow
	 * stops. He was never going to be over there. The portal only moves
	 * ServerPlayers anyway — see TheWayBlock.entityInside.
	 *
	 * Underground was not handled at all. He was dragged down every shaft, into the
	 * gaol, through the warren and down forty blocks of infected cave, and the
	 * teleport made sure of it: over twenty-six blocks he appears next to you
	 * wherever you are.
	 *
	 * SO HE STOPS AT THE HOLE. Which is what book ten says he would — "I will help
	 * you as far as I can still walk" — and it is what makes the underground worth
	 * anything: every frightening room in this mod is a room you are in on your
	 * own, and the way you know that is the man who has been behind you all day
	 * stopping at the top of the stair.
	 *
	 * EIGHT BLOCKS BELOW THE LOCAL SURFACE, not "cannot see sky". A doorway is not
	 * a cave and neither is a cellar with a window; measuring against the ground
	 * directly overhead means a player in a house is still outdoors as far as he is
	 * concerned, and a player who has gone down a ladder is not.
	 */
	/**
	 * HE GOES WHERE YOU GO. There used to be a depth at which he stopped — eight
	 * blocks under the surface and he turned back with an apology — and it read
	 * as exactly what it was: the man who said "everyone I walked this road with
	 * is dead" refusing to walk down a staircase. He follows into the mines, into
	 * the undercity, through the way, up to the keep. The only thing that ends
	 * it is Herobrine.
	 */
	public boolean willNotGoDown(Player with) {
		return false;
	}

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
		// THE ROAD IS LONG AND WET. Two and a half times the usual search depth, so
		// a river bend or a ravine gets a way round rather than a shrug; and he
		// floats, so a current carries him along the top and not along the bottom.
		this.getNavigation().setCanFloat(true);
		this.getNavigation().setMaxVisitedNodesMultiplier(2.5F);
	}

	/**
	 * WHAT HE IS WEARING, AND HE IS NOT CARRYING IT FOR YOU.
	 *
	 * Diamond, enchanted, all four pieces and a sword. It reads as absurd for two
	 * seconds and then reads as the only sensible thing about him: he is the last
	 * survivor of a valley that lost four hundred people, he has been at this for
	 * sixty years, and the one thing sixty years of failing at something teaches
	 * you is what to wear.
	 *
	 * IT IS ALSO WHAT MAKES HIM USEFUL RATHER THAN A LIABILITY. He cannot die —
	 * damage is clamped above zero — so without armour he would spend every fight
	 * at two hearts running away, which is a companion who is always broken. In
	 * plate he wins most of what he starts, and the flee is what happens when he
	 * meets something that is actually a problem.
	 *
	 * NOTHING DROPS. Set to zero explicitly rather than left to chance: a full set
	 * of enchanted diamond is the best loot in the mod by a distance, and a
	 * companion who cannot die but can be farmed for his boots is a bug with a
	 * story attached.
	 */
	private void kit(net.minecraft.util.RandomSource random) {
		java.util.Map<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.Item>
			gear = new java.util.EnumMap<>(net.minecraft.world.entity.EquipmentSlot.class);
		gear.put(net.minecraft.world.entity.EquipmentSlot.HEAD,
			net.minecraft.world.item.Items.DIAMOND_HELMET);
		gear.put(net.minecraft.world.entity.EquipmentSlot.CHEST,
			net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
		gear.put(net.minecraft.world.entity.EquipmentSlot.LEGS,
			net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
		gear.put(net.minecraft.world.entity.EquipmentSlot.FEET,
			net.minecraft.world.item.Items.DIAMOND_BOOTS);
		gear.put(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
			net.minecraft.world.item.Items.DIAMOND_SWORD);
		for (var slot : gear.entrySet()) {
			net.minecraft.world.item.ItemStack stack =
				new net.minecraft.world.item.ItemStack(slot.getValue());
			com.bloomlet.herobrine.manifest.HisHost.enchant(stack, random,
				this.level().registryAccess(), 20);
			this.setItemSlot(slot.getKey(), stack);
			this.setDropChance(slot.getKey(), 0.0F);
		}
		// AND A SHIELD, in the off hand, which is also where the bread goes when he
		// eats — Falter swaps them and puts the shield back. See guard() for when he
		// raises it.
		this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
			new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
		this.setDropChance(net.minecraft.world.entity.EquipmentSlot.OFFHAND, 0.0F);
	}

	/** The off hand goes back to the shield — after eating, after anything. */
	void shieldUp() {
		if (!this.getOffhandItem().is(Items.SHIELD)) {
			this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
		}
	}

	/**
	 * The kit goes on at spawn, once, on the server.
	 *
	 * Not in the constructor: that runs on the client too, and enchanting needs a
	 * RegistryAccess the client copy has no business being asked for. finalizeSpawn
	 * is the hook vanilla itself uses to dress a mob, and it fires exactly once per
	 * creature however it was created — structure, command or event.
	 */
	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
			net.minecraft.world.level.ServerLevelAccessor level,
			net.minecraft.world.DifficultyInstance difficulty,
			net.minecraft.world.entity.EntitySpawnReason reason,
			net.minecraft.world.entity.@org.jspecify.annotations.Nullable SpawnGroupData data) {
		this.kit(level.getRandom());
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, LIVES)
			// HE SWINGS NOW, SO HE NEEDS THIS OR HE CRASHES THE SERVER.
			//
			// Mob.createMobAttributes does not include attack damage, and
			// Mob.doHurtTarget reads it with nothing guarding it — which is exactly
			// how the mimic took the server down the first time one of them ever
			// landed a hit. He had no attack goal until now, so he was never at
			// risk; adding one without this line would have re-created that bug on
			// purpose.
			//
			// Two, and the diamond sword adds its own seven on top through the
			// item's attribute modifiers. Base low on purpose: unarmed he is a man
			// with sixty years of failing behind him, and what makes him dangerous
			// is the kit.
			// NINE, AND IT WAS TWO.
			//
			// Two plus a diamond sword's seven is nine on paper and reads as nothing
			// in practice, because what he is being asked to fight is not a zombie.
			// A Turned has twenty-six health, a Gaunt forty, and the Gaunt freezes
			// while it is watched and steps four blocks at a time in the dark — so
			// five clean hits is not five swings, it is a minute of him chasing
			// something that keeps not being where he swung.
			//
			// Reported as "he does so little damage on them, making them too
			// powerful", and the report is right: an ally who cannot finish anything
			// is not an ally, he is a second health bar you have to watch.
			//
			// Nine base, so about sixteen with the sword. Two hits for a Turned,
			// three for a Gaunt, two for a mimic. He wins the fights he starts and
			// he still cannot touch Herobrine, who is the only thing here that is
			// not supposed to be winnable by somebody else.
			.add(Attributes.ATTACK_DAMAGE, 9.0)
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
		// HUNTING COMES BEFORE FOLLOWING AND AFTER FALTERING, and that order is the
		// whole of his behaviour.
		//
		// Falter is first, so a man on two hearts runs whatever is in front of him.
		// Then the fight, so anything hostile within reach gets dealt with before
		// he thinks about where you are. Follow last, so catching up is what he
		// does when there is nothing else — which is what a person walking with you
		// through a bad country actually looks like.
		this.goalSelector.addGoal(2,
			new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.15, true));
		// LEADING SITS ABOVE FOLLOWING. While he is bringing you to the farm he is
		// nobody's companion yet — Follow has no one to follow — so the two never
		// compete; Falter and the fight still outrank both.
		this.goalSelector.addGoal(3, new Lead(this));
		this.goalSelector.addGoal(4, new Follow(this));
		// ---- AND WHEN THERE IS NOTHING TO DO HE DOES NOT FREEZE.
		//
		// Follow stops inside four blocks and there was nothing under it, so a man
		// who had caught up stood perfectly still until you moved again. Which is
		// what a mob does and not what a person does — and this one has a name over
		// his head, so the stillness reads as broken rather than as calm.
		//
		// The stroll can only ever take him about four blocks: past that Follow
		// outranks it and reclaims the movement to bring him back. So it is not
		// wandering, it is shifting about near you, which is the thing being asked
		// for.
		this.goalSelector.addGoal(4,
			new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(
				this, 0.6));
		// SIX TENTHS, NOT VANILLA'S TWO HUNDREDTHS. LookAtPlayerGoal's default
		// probability is 0.02 — it looks at you on one tick in fifty, which is
		// right for a cow in a field and reads as ignoring you from somebody who
		// is meant to be with you. Ten blocks, and most of the time.
		this.goalSelector.addGoal(5,
			new LookAtPlayerGoal(this, Player.class, 10.0F, 0.6F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

		// ---- AND WHAT HE GOES AFTER.
		//
		// The nearest hostile thing, and he picks it himself rather than
		// waiting to be hit — HurtByTargetGoal alone would make him a
		// bodyguard who only ever reacts, and a man who has been doing this
		// for sixty years does not wait to be bitten.
		//
		// Monster rather than Mob, so he never picks a fight with a cow, a
		// villager, or the player's own wolf. He also cannot target the player:
		// Monster excludes them by type, which is a stronger guarantee than a
		// predicate somebody can widen later.
		// HIS THINGS. The Gaunt, the Turned, the Infected, the mimic and the man
		// himself are not Monsters, so the Monster goal below never saw them: a Gaunt
		// could beat him to the floor and he would stand there wondering what happened.
		this.targetSelector.addGoal(3,
			new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
				this, Mob.class, 10, true, false,
				(candidate, level) -> candidate instanceof Mob m && Sayings.isHis(m)
					&& this.worthAFight(m)));
		this.targetSelector.addGoal(1,
			new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
		this.targetSelector.addGoal(2,
			new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
				this, net.minecraft.world.entity.monster.Monster.class, 10, true, false,
				(candidate, level) -> this.worthAFight(candidate)));
	}

	// ---- THE ROAD COMES FIRST ----------------------------------------------

	/** While leading: a thing this close to him is a fight whether he likes it or not. */
	private static final double FIGHTS_WHILE_LEADING = 5.0;
	/** And a thing this close to the person he is leading, and going for them, is his job. */
	private static final double DEFENDS_WHILE_LEADING = 7.0;

	/**
	 * WHILE HE IS LEADING, THE ROAD COMES FIRST. The melee goal outranks the
	 * lead, so without this a man taking you somewhere stopped for every skeleton
	 * in the trees and you stood there watching him fence. A zombie forty blocks
	 * off is not his problem. One at his heels, or one at yours, is.
	 */
	boolean worthAFight(LivingEntity what) {
		if (!this.isLeading()) {
			return true;
		}
		if (this.distanceToSqr(what) <= FIGHTS_WHILE_LEADING * FIGHTS_WHILE_LEADING) {
			return true;
		}
		Player who = this.leading();
		return who != null && what instanceof Mob it && it.getTarget() == who
			&& what.distanceToSqr(who) <= DEFENDS_WHILE_LEADING * DEFENDS_WHILE_LEADING;
	}

	/** Twice a second while leading: a target no longer worth the stop is let go, and the melee goal yields the road back. */
	private void keepToTheRoad() {
		if ((this.tickCount % 10) != 0 || !this.isLeading()) {
			return;
		}
		LivingEntity at = this.getTarget();
		if (at != null && !this.worthAFight(at)) {
			this.setTarget(null);
		}
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
	/**
	 * The entrance ends when it is over, one way or the other.
	 *
	 * Called from tick rather than from Follow, because Follow only runs while he
	 * is further than DAWDLES_WITHIN — so the goal that would notice he had arrived
	 * is the one that stops running the moment he does.
	 */
	/**
	 * HE SPRINT-JUMPS, BECAUSE THAT IS HOW A PLAYER RUNS.
	 *
	 * Nobody who has played this game for an hour sprints on the flat without
	 * jumping. It is faster, and past that it is a habit — the rhythm of somebody
	 * covering ground. A companion who runs beside you without it is running like
	 * a mob, and you feel the difference before you can name it.
	 *
	 * So while he is sprinting and actually moving, on the ground and not in water
	 * or mid-sandwich, he hops on a loose clock: every one to three seconds, never
	 * evenly. The forward kick comes for free — LivingEntity.jumpFromGround adds it
	 * to anything that isSprinting(), which is why the sprint flag is real and not
	 * just a speed multiplier.
	 *
	 * And the flag comes off the moment he stops moving, or he would stand next to
	 * you at the door emitting sprint particles like an idling engine.
	 */
	private static final int HOP_MIN = 24;
	private static final int HOP_SPREAD = 36;
	private int hopIn;

	private void hop() {
		if (!this.isSprinting()) {
			return;
		}
		if (this.getNavigation().isDone()
			|| this.getDeltaMovement().horizontalDistanceSqr() < 0.004) {
			this.setSprinting(false);
			return;
		}
		if (!this.onGround() || this.isInWater() || this.isUsingItem()) {
			return;
		}
		if (--this.hopIn > 0) {
			return;
		}
		// NOT OFF A LEDGE. A hop carries him a block and a half forward; if there is
		// nothing under that within four blocks it is a jump off a cliff, and the
		// fall damage is what "that's too much, I'm going" half a minute after he
		// joined was probably about.
		net.minecraft.world.phys.Vec3 ahead = this.getDeltaMovement().normalize().scale(1.5);
		BlockPos landing = BlockPos.containing(this.getX() + ahead.x, this.getY(), this.getZ() + ahead.z);
		boolean floor = false;
		for (int down = 0; down <= 4; down++) {
			if (this.level().getBlockState(landing.below(down)).blocksMotion()) {
				floor = true;
				break;
			}
		}
		if (!floor) {
			this.hopIn = 10;
			return;
		}
		this.hopIn = HOP_MIN + this.random.nextInt(HOP_SPREAD);
		this.getJumpControl().jump();
	}

	private void theWalkIn() {
		if (this.walkingIn <= 0) {
			return;
		}
		Player with = this.companion();
		if (with == null || this.distanceTo(with) <= ARRIVED_WITHIN) {
			if (with != null) {
				HerobrineMod.LOGGER.info("addexio walked in — {} blocks and {} seconds",
					(int) this.distanceTo(with), (WALKS_IN_FOR - this.walkingIn) / 20);
			}
			this.walkingIn = 0;
			return;
		}
		if (--this.walkingIn <= 0) {
			HerobrineMod.LOGGER.info(
				"addexio could not walk it — still {} blocks off after {} seconds,"
					+ " the teleport has him now",
				(int) this.distanceTo(with), WALKS_IN_FOR / 20);
		}
	}

	/**
	 * HE HAD NO TICK OF HIS OWN, and the entrance needs one.
	 *
	 * Everything he did lived in goals, which is the right place for behaviour and
	 * the wrong place for the thing that decides a goal's rules have changed.
	 * theWalkIn cannot live in Follow: Follow only runs while he is further than
	 * DAWDLES_WITHIN, so the goal that would notice he had finally arrived is the
	 * one that stops running the moment he does.
	 */
	/**
	 * THE KIT IS PUT ON HIM WITH A LIVE LEVEL UNDER HIM — the same lesson blade()
	 * taught for Herobrine. kit() ran from finalizeSpawn, and Company does not
	 * call finalizeSpawn: it creates him, places him and addFreshEntity()s him. So
	 * the man who walked sixty blocks to your door had no sword, no armour and no
	 * shield, and the armour layer I added drew nothing because there was nothing
	 * to draw. Once, on the first server tick, if his hand is empty.
	 */
	private boolean armed;

	/** When he last swung, synced, so the client can draw the arm. See CompanionRenderer. */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> SWUNG =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
			.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.cast(),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
			.buildAndRegister(HerobrineMod.id("addexio_swung_at"));
	public static final int SWING_SHOWS = 6;

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			if (this.isFallen() && this.level() instanceof ServerLevel here) {
				this.lieThere(here);
				return;
			}
			if (!this.armed) {
				this.armed = true;
				if (this.getMainHandItem().isEmpty()) {
					this.kit(this.getRandom());
				}
			}
			this.keepToTheRoad();
			this.theWalkIn();
			this.hop();
			this.theIntroduction();
			this.guard();
		}
	}

	/**
	 * THE SHIELD COMES UP WHEN THE BLOW IS COMING.
	 *
	 * Not held up permanently — a man behind a shield is not fighting — and not
	 * random. When the thing he is on is within reach and mid-swing, or simply
	 * close and it is a moment since he last struck, up it goes for a second; then
	 * down, and a short rest before the next. LivingEntity's own blocking does the
	 * rest: the arm pose, the damage, the knockback. swing() drops it first so his
	 * own blow is never thrown from behind it.
	 */
	private void guard() {
		if (this.guardRest > 0) {
			this.guardRest--;
		}
		if (!this.getOffhandItem().is(Items.SHIELD) || this.fleeing) {
			if (this.fleeing && this.guardFor > 0) {
				this.guardFor = 0;
				this.stopUsingItem();
			}
			return;      // eating, unarmed, or running — the shield is for standing
		}
		net.minecraft.world.entity.LivingEntity foe = this.getTarget();
		if (this.guardFor > 0) {
			this.guardFor--;
			if (foe != null) {
				this.getLookControl().setLookAt(foe, 90.0F, 90.0F);
			}
			if (this.guardFor == 0 || foe == null || !foe.isAlive()
				|| this.distanceTo(foe) > GUARDS_WITHIN + 1.5) {
				this.guardFor = 0;
				this.stopUsingItem();
				this.guardRest = GUARD_REST;
			}
			return;
		}
		if (foe == null || !foe.isAlive() || this.guardRest > 0 || this.swinging
			|| this.isUsingItem() || this.distanceTo(foe) > GUARDS_WITHIN) {
			return;
		}
		if (foe.swinging || this.random.nextInt(12) == 0) {
			this.startUsingItem(InteractionHand.OFF_HAND);
			this.guardFor = GUARD_HOLDS;
			this.getLookControl().setLookAt(foe, 90.0F, 90.0F);
		}
	}

	@Override
	public void swing(InteractionHand hand, boolean updateSelf) {
		// His own blow is never thrown from behind the shield.
		if (this.isUsingItem() && this.getOffhandItem().is(Items.SHIELD)) {
			this.stopUsingItem();
			this.guardFor = 0;
			this.guardRest = GUARD_REST;
		}
		super.swing(hand, updateSelf);
		// THE SWING IS TOLD TO THE CLIENT BY HAND. In 26.2 nothing in the humanoid
		// render pipeline writes attackTime for a mob — Herobrine's renderer had to
		// compute it from a synced timestamp, and so does his. Without this the arm
		// never moves, whatever the model says it can do.
		if (this.level() instanceof ServerLevel here) {
			this.setAttached(SWUNG, here.getGameTime());
		}
	}

	/**
	 * HE COMES TO YOU, AND HE SAYS WHO HE IS.
	 *
	 * The walk-in brought him to ten blocks and stopped, and there he stood until
	 * somebody worked out he could be clicked. Now, once he is in and nobody has
	 * claimed him, he closes the last of it himself — to four blocks, looking at
	 * you — and gives the introduction (Sayings.INTRODUCTION), standing still for
	 * the length of it. When the last line lands he is yours: goWith(), without a
	 * click, because the last line is him saying so.
	 *
	 * Once per world. A save reloaded mid-way, or a second player arriving later,
	 * gets a man who already knows you, not the speech again.
	 */
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> INTRODUCED =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.createPersistent(
			HerobrineMod.id("addexio_introduced"), com.mojang.serialization.Codec.BOOL);
	private static final double COMES_TO_YOU_FROM = 24.0;
	private static final double SPEAKS_FROM = 4.0;
	private int introducing;
	private java.util.@org.jspecify.annotations.Nullable UUID introducingTo;
	/** Whoever hit him last, and when — a threat whether or not it is one of his. */
	private net.minecraft.world.entity.@org.jspecify.annotations.Nullable Mob lastAttacker;
	private long lastAttackedAt;
	private static final double GUARDS_WITHIN = 3.5;
	private static final int GUARD_HOLDS = 20;
	private static final int GUARD_REST = 25;
	private int guardFor;
	private int guardRest;
	/** Set by Falter while he is running: the shield stays down and the legs do the work. */
	boolean fleeing;

	private void theIntroduction() {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		if (this.introducing > 0) {
			this.introducing--;
			this.getNavigation().stop();
			// THE ONE HE IS TALKING TO, not the nearest. Walk off in the middle of it
			// and he used to finish the speech to an empty room and then stand there
			// for good, because "nearest player within twelve" had nobody in it. He
			// follows the person he introduced himself to, wherever they have got to;
			// Follow fetches him the rest of the way.
			Player to = this.introducingTo == null ? null : here.getPlayerByUUID(this.introducingTo);
			if (to == null) {
				to = here.getNearestPlayer(this, 64.0);
			}
			if (to != null) {
				this.getLookControl().setLookAt(to, 60.0F, 60.0F);
			}
			if (this.introducing == 0) {
				this.introducingTo = null;
				if (to != null && !this.isSpokenFor()) {
					this.goWith(to);
					HerobrineMod.LOGGER.info("{} is coming with {} — he said so himself",
						this.getName().getString(), to.getName().getString());
				}
			}
			return;
		}
		if (this.walkingIn > 0 || this.isSpokenFor() || this.isLeading()
			|| Boolean.TRUE.equals(here.getServer().overworld().getAttached(INTRODUCED))) {
			return;      // not while he is bringing you somewhere — the introduction is for when you are there
		}
		if (!(here.getNearestPlayer(this, COMES_TO_YOU_FROM) instanceof ServerPlayer to)
			|| !to.isAlive() || to.isSpectator()) {
			return;
		}
		if (this.distanceTo(to) > SPEAKS_FROM) {
			if (this.tickCount % 10 == 0) {
				this.getNavigation().moveTo(to, 1.0);
			}
			return;
		}
		here.getServer().overworld().setAttached(INTRODUCED, true);
		this.introducingTo = to.getUUID();
		this.introducing = Sayings.introductionLength();
		this.getNavigation().stop();
		Sayings.introduce(here, this, to);
	}

	/**
	 * ONLY HE CAN KILL HIM — AND IT LEAVES A BODY.
	 *
	 * Everything else in the world is clamped at two hearts: zombies, falls, fire,
	 * the Gaunt. Herobrine is not. His blows, his fireballs, anything whose source
	 * is him, take Addexio all the way down — through twenty-four hearts and
	 * enchanted diamond, so it takes him a while, and a player who is fighting
	 * beside him can stop it. A player who is not, watches it happen.
	 *
	 * And he does not vanish in a puff of smoke like a mob. He falls where he
	 * stands and lies there: no AI, no health bar, sword still in his hand, the
	 * sleeping pose laid flat with the arms out (see CompanionRenderer). For half an
	 * hour of game time, then he is gone — and he does not come back. Once per
	 * world, like the introduction; the story has a cost now.
	 */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> FALLEN_UNTIL =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
			.persistent(com.mojang.serialization.Codec.LONG)
			.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.cast(),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
			.buildAndRegister(HerobrineMod.id("addexio_fallen_until"));
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> LOST =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.createPersistent(
			HerobrineMod.id("addexio_lost"), com.mojang.serialization.Codec.BOOL);
	private static final long LIES_FOR = 36000L;   // thirty minutes

	public boolean isFallen() {
		return this.getAttached(FALLEN_UNTIL) != null;
	}

	/**
	 * HE COMES WITH YOU — through the way, or through a command.
	 *
	 * The crossing code brought him along; /herobrine boss did not, because it
	 * teleports the player itself and never touches the way. So the one test route
	 * everybody actually uses left him standing on the lawn. Anyone within thirty
	 * blocks who is yours and on his feet crosses, and lands a step to your side.
	 */
	public static int crossWith(ServerPlayer player, ServerLevel from, ServerLevel to,
	                            net.minecraft.world.phys.Vec3 at) {
		int came = 0;
		for (CompanionEntity her : from.getEntitiesOfClass(CompanionEntity.class,
				player.getBoundingBox().inflate(30.0, 16.0, 30.0),
				h -> h.isAlive() && !h.isFallen() && h.companion() == player)) {
			net.minecraft.world.entity.Entity moved = her.teleport(
				new net.minecraft.world.level.portal.TeleportTransition(to,
					at.add(1.5, 0.0, 1.5), net.minecraft.world.phys.Vec3.ZERO, her.getYRot(), 0.0F,
					net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
			came++;
			// WHAT HE SAYS ON THE OTHER SIDE, once. The copy that arrives is a new
			// entity — teleport() across a dimension makes one — so the story is told
			// by the one that is actually standing there.
			if (moved instanceof CompanionEntity arrived
				&& !Boolean.TRUE.equals(to.getAttached(TOLD_HIS_WORLD))) {
				to.setAttached(TOLD_HIS_WORLD, true);
				Sayings.tell(to, arrived, player, Sayings.CROSSED, 80);
			}
			HerobrineMod.LOGGER.info("addexio crossed with {}", player.getName().getString());
		}
		return came;
	}

	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> TOLD_HIS_WORLD =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.createPersistent(
			HerobrineMod.id("addexio_told_his_world"), com.mojang.serialization.Codec.BOOL);

	/** Whether this world's Addexio has been killed. Company does not send another. */
	public static boolean lost(ServerLevel level) {
		return Boolean.TRUE.equals(level.getServer().overworld().getAttached(LOST));
	}

	private void fall(ServerLevel level, DamageSource source) {
		this.setHealth(1.0F);
		this.setAttached(FALLEN_UNTIL, level.getGameTime() + LIES_FOR);
		level.getServer().overworld().setAttached(LOST, true);
		this.stopUsingItem();
		this.getNavigation().stop();
		this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		this.setTarget(null);
		this.setNoAi(true);
		this.setInvulnerable(true);
		this.setPose(net.minecraft.world.entity.Pose.SLEEPING);
		this.fleeing = false;
		this.guardFor = 0;
		Player with = this.companion();
		if (with instanceof ServerPlayer heard) {
			this.lastSpoke = -100000L;     // past the quiet timer: these words are not optional
			Sayings.say(level, this, heard, Sayings.FALLEN);
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.PLAYER_DEATH, this.getSoundSource(), 1.0F, 0.85F);
		HerobrineMod.LOGGER.info("{} has fallen to {} at [{}, {}, {}] — he lies there {} minutes",
			this.getName().getString(), source.getEntity() == null ? "him"
				: source.getEntity().getType().toShortString(),
			this.getBlockX(), this.getBlockY(), this.getBlockZ(), LIES_FOR / 1200);
	}

	private void lieThere(ServerLevel level) {
		Long until = this.getAttached(FALLEN_UNTIL);
		if (until == null) {
			return;
		}
		if (!this.isNoAi()) {              // a reload: the pose and the stillness come back
			this.setNoAi(true);
			this.setInvulnerable(true);
		}
		this.setPose(net.minecraft.world.entity.Pose.SLEEPING);
		this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		if (level.getGameTime() >= until) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
				this.getX(), this.getY() + 0.3, this.getZ(), 30, 0.6, 0.2, 0.6, 0.01);
			HerobrineMod.LOGGER.info("{} is gone", this.getName().getString());
			this.discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isFallen()) {
			return false;
		}
		// HIS HAND IS THE ONE THAT CAN CLOSE. See fall().
		boolean his = source.getEntity() instanceof HerobrineEntity
			|| (source.getDirectEntity() != null
				&& source.getDirectEntity().getType().toShortString().contains("fireball")
				&& source.getEntity() instanceof HerobrineEntity);
		if (his) {
			// HIS FIREBALLS ARE NOT HOW THIS ENDS. A power-three ball is twenty-odd damage
			// and lands at the player's feet — which is where Addexio stands — so he was
			// dying to splash in seconds, before anybody saw him fight. The blows he is
			// meant to die to are the ones Herobrine walks over and gives him (see
			// breakHelper); the explosions and arrows are a third of themselves.
			if (source.getDirectEntity() != source.getEntity()
				|| source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
				damage *= 0.3F;
			}
			if (this.getHealth() - damage <= 0.0F) {
				this.fall(level, source);
				return true;
			}
			return super.hurtServer(level, source, damage);
		}
		// FACE IT. A man who is being hit turns round — before anything else, and
		// with the sandwich down. He was finishing his bread with his back to a
		// zombie because the zombie was not one of Herobrine's and so did not count.
		if (source.getEntity() instanceof Mob attacker && attacker != this) {
			this.lastAttacker = attacker;
			this.lastAttackedAt = level.getGameTime();
			this.getLookControl().setLookAt(attacker, 90.0F, 90.0F);
			if (this.getOffhandItem().is(Items.BREAD)) {
				this.stopUsingItem();
				this.shieldUp();
			}
			if (this.getTarget() == null) {
				this.setTarget(attacker);
			}
		}
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

	/**
	 * A MAN'S VOICE, NOT A VILLAGER'S.
	 *
	 * He grunted like a villager, which was correct while he WAS one — the old
	 * version was an ordinary villager in a red coat, drawn on the villager mesh,
	 * and a villager noise was the whole point of the disguise.
	 *
	 * He is not one any more. He is on the humanoid mesh in enchanted plate with a
	 * name over his head, and the hnnn is the single most identifying sound in the
	 * game: it says trade menu, it says scenery, and it says it every time he takes
	 * a hit for you. Steve's own hurt and death, so what you hear when something
	 * lands on him is a person.
	 *
	 * NO AMBIENT. The Turned mutter constantly because that is their tell. He talks
	 * — see Sayings — and something that both talks and idles would be talking over
	 * itself.
	 */
	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound() {
		// He cannot die; damage is clamped above zero. This is here for the one
		// path that could still reach it — /kill, the void before Company fishes
		// him out — and a villager death rattle on that would be the last thing
		// anybody heard of him.
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	// ---- FOLLOWING --------------------------------------------------------

	/**
	 * Walk when she can, run when she has to, appear when she cannot.
	 *
	 * Three bands rather than one speed. A companion pinned at a fixed distance
	 * looks like a camera on a boom; one that ambles when you amble and breaks
	 * into a run when you get away from her looks like a person keeping up.
	 */
	// ---- LEADING -------------------------------------------------------------
	/** Where he is taking you, and whom. Persistent, so a restart mid-walk resumes it. */
	private static final AttachmentType<Long> LEAD_TO =
		AttachmentRegistry.createPersistent(HerobrineMod.id("lead_to"), Codec.LONG);
	private static final AttachmentType<String> LEADING =
		AttachmentRegistry.createPersistent(HerobrineMod.id("leading"), Codec.STRING);

	public void lead(BlockPos to, Player who) {
		this.setAttached(LEAD_TO, to.asLong());
		this.setAttached(LEADING, who.getUUID().toString());
	}

	public boolean isLeading() {
		return this.getAttached(LEAD_TO) != null;
	}

	@org.jspecify.annotations.Nullable BlockPos leadingTo() {
		Long packed = this.getAttached(LEAD_TO);
		return packed == null ? null : BlockPos.of(packed);
	}

	@org.jspecify.annotations.Nullable Player leading() {
		String who = this.getAttached(LEADING);
		if (who == null) {
			return null;
		}
		try {
			return this.level().getPlayerByUUID(java.util.UUID.fromString(who));
		} catch (IllegalArgumentException malformed) {
			return null;
		}
	}

	void stopLeading() {
		this.removeAttached(LEAD_TO);
		this.removeAttached(LEADING);
	}

	/**
	 * THE WALK TO THE FARM.
	 *
	 * He comes up to you first — one sentence at nine blocks — then turns and
	 * goes, eighteen blocks a leg, and stops and looks back whenever you fall
	 * more than fourteen behind. Every thirty seconds of standing he says a word.
	 * Forty blocks behind for two minutes and he gives up on your legs, not on
	 * you: the map, and he follows at your pace instead. Nine blocks from the
	 * farm he stops; the introduction, which has been waiting on isLeading(),
	 * takes it from there.
	 *
	 * COST. No scans. A distance and a heightmap read every ten ticks, one
	 * moveTo. The stuck check is a position compare every sixty. If he cannot
	 * path — water, a cliff — he steps to a spot ten blocks ahead of you toward
	 * the farm, the way Follow already does when it loses you.
	 */
	private static final class Lead extends Goal {
		private static final double GREETS_AT = 9.0;
		private static final double WAITS_AT = 14.0;
		private static final double TOO_FAR = 40.0;
		private static final double LEG = 18.0;
		private static final double ARRIVES_AT = 9.0;
		private static final int NAGS_EVERY = 600;
		private static final int GIVES_UP_AFTER = 2400;
		/** Nearer the farm than he is by this much and they are AHEAD: he hurries and overtakes, he does not wait. */
		private static final double AHEAD_BY = 3.0;
		/** Ahead of him by this much and he stops arguing with the ground and simply is ahead of them again. */
		private static final double CATCHES_UP_FROM = 24.0;
		/** Two paces: a man walking ahead of you, and a man who has just watched you sprint past him. */
		private static final double STROLL = 1.12;
		private static final double HURRY = 1.35;
		private static final double HURRIES_PAST = 9.0;
		/** Looks are ten ticks. Not moved and no nearer for five of them, and the terrain has won; he steps ahead. */
		private static final int STUCK_AT = 5;
		private static final double MOVED = 1.5;
		private static final double PROGRESS = 0.6;
		/** In water and no nearer for three looks is a current; five below the player and no nearer is a hole. Same answer, sooner. */
		private static final int HELD_AT = 3;
		private static final double FELL = 5.0;

		private final CompanionEntity her;
		private int repath;
		private int waited;
		private int ignored;
		private int looks;
		private int stuck;
		private int wet;
		private int fell;
		private boolean greeted;
		private double wasX;
		private double wasZ;
		private double wasLeft = -1.0;

		Lead(CompanionEntity her) {
			this.her = her;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!this.her.isLeading()) {
				return false;
			}
			Player who = this.her.leading();
			return who != null && who.isAlive() && !who.isSpectator();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void stop() {
			this.her.getNavigation().stop();
			this.her.setSprinting(false);
		}

		@Override
		public void tick() {
			if (!(this.her.level() instanceof ServerLevel here)) {
				return;
			}
			Player who = this.her.leading();
			BlockPos to = this.her.leadingTo();
			if (who == null || to == null) {
				return;
			}
			if (--this.repath > 0) {
				return;
			}
			this.repath = 10;
			double away = this.her.distanceTo(who);
			if (!this.greeted) {
				if (away > GREETS_AT) {
					this.her.getNavigation().moveTo(who, 1.3);
					this.her.getLookControl().setLookAt(who, 30.0F, 30.0F);
					return;
				}
				this.greeted = true;
				Sayings.say(here, this.her, who, Sayings.LEAD_FIRST);
				HerobrineMod.LOGGER.info("addexio met {} and is leading them to [{}, {}]",
					who.getName().getString(), to.getX(), to.getZ());
			}
			double hisLeft = flat(this.her.getX(), this.her.getZ(), to);
			double theirLeft = flat(who.getX(), who.getZ(), to);
			if (hisLeft < ARRIVES_AT) {
				this.arrive(here, who);
				return;
			}
			// THEY ARE BEHIND, OR THEY ARE AHEAD, and those are different jobs.
			// Behind: he waits past fourteen blocks and nags. Ahead — they have run
			// past him toward the farm — he never waits: he hurries, and if they are
			// well clear he is simply ahead of them again. The old code waited either
			// way, which is why he "walked slowly" when you ran: he was standing
			// still behind you, being patient.
			boolean theyAreAhead = theirLeft < hisLeft - AHEAD_BY;
			if (away > TOO_FAR && !theyAreAhead) {
				this.ignored += 10;
				if (this.ignored >= GIVES_UP_AFTER) {
					this.handTheMap(here, who, to);
					return;
				}
			} else {
				this.ignored = Math.max(0, this.ignored - 10);
			}
			if (!theyAreAhead && away > WAITS_AT) {
				this.her.getNavigation().stop();
				this.her.setSprinting(false);
				this.her.getLookControl().setLookAt(who, 30.0F, 30.0F);
				this.waited += 10;
				if (this.waited % NAGS_EVERY == 0) {
					Sayings.say(here, this.her, who, Sayings.LEAD_WAITING);
				}
				this.rested();
				return;
			}
			this.waited = 0;
			if (theyAreAhead && away > CATCHES_UP_FROM) {
				this.stepAhead(here, who, to);
				this.rested();
				return;
			}
			// The next leg: eighteen blocks toward the farm — from him, or from
			// them when they are ahead, so the leg ends in front of them and not
			// somewhere behind their back.
			double fromX = theyAreAhead ? who.getX() : this.her.getX();
			double fromZ = theyAreAhead ? who.getZ() : this.her.getZ();
			double lx = to.getX() + 0.5 - fromX;
			double lz = to.getZ() + 0.5 - fromZ;
			double len = Math.sqrt(lx * lx + lz * lz);
			double gx = len > LEG ? fromX + lx / len * LEG : to.getX() + 0.5;
			double gz = len > LEG ? fromZ + lz / len * LEG : to.getZ() + 0.5;
			int bx = net.minecraft.util.Mth.floor(gx);
			int bz = net.minecraft.util.Mth.floor(gz);
			int gy = com.bloomlet.herobrine.structure.Ground.topOf(here, bx, bz) + 1;
			double pace = theyAreAhead || away > HURRIES_PAST ? HURRY : STROLL;
			this.her.setSprinting(pace >= HURRY);
			if (this.her.getNavigation().isDone() || ++this.looks % 3 == 0) {
				this.her.getNavigation().moveTo(bx + 0.5, gy, bz + 0.5, pace);
			}
			// IS HE GETTING ANYWHERE. He has not moved and the farm is no nearer;
			// he is in water and no nearer (a current has him); he is five below
			// the player and no nearer (a hole has him). Walking a detour still
			// moves him, and walking downhill ahead of you still gains ground, so
			// neither of those trips it. When one does, he stops arguing with the
			// terrain and is ten blocks ahead of you instead. He knows the way; how
			// he got there is his business.
			double moved = Math.abs(this.her.getX() - this.wasX) + Math.abs(this.her.getZ() - this.wasZ);
			double progress = this.wasLeft < 0 ? PROGRESS : this.wasLeft - hisLeft;
			this.wasX = this.her.getX();
			this.wasZ = this.her.getZ();
			this.wasLeft = hisLeft;
			boolean noNearer = progress < PROGRESS;
			this.stuck = noNearer && moved < MOVED ? this.stuck + 1 : 0;
			this.wet = noNearer && this.her.isInWater() ? this.wet + 1 : 0;
			this.fell = noNearer && this.her.getY() < who.getY() - FELL && away > 6.0 ? this.fell + 1 : 0;
			if (this.stuck >= STUCK_AT || this.wet >= HELD_AT || this.fell >= HELD_AT) {
				this.stepAhead(here, who, to);
				this.rested();
			}
		}

		private static double flat(double x, double z, BlockPos to) {
			double dx = x - (to.getX() + 0.5);
			double dz = z - (to.getZ() + 0.5);
			return Math.sqrt(dx * dx + dz * dz);
		}

		/** Forget the run-up: after a wait or a step there is nothing to be stuck on. */
		private void rested() {
			this.stuck = 0;
			this.wet = 0;
			this.fell = 0;
			this.wasLeft = -1.0;
			this.wasX = this.her.getX();
			this.wasZ = this.her.getZ();
		}

		private void arrive(ServerLevel here, Player who) {
			this.her.stopLeading();
			this.her.getNavigation().stop();
			this.her.getLookControl().setLookAt(who, 30.0F, 30.0F);
			Sayings.say(here, this.her, who, Sayings.LEAD_ARRIVED);
			HerobrineMod.LOGGER.info("addexio brought {} to the farm — the introduction is his next word",
				who.getName().getString());
		}

		private void handTheMap(ServerLevel here, Player who, BlockPos to) {
			ItemStack map = com.bloomlet.herobrine.manifest.Whereabouts.theWay(here, to);
			if (who instanceof ServerPlayer player && !player.getInventory().add(map)) {
				player.drop(map, false);
			}
			Sayings.say(here, this.her, who, Sayings.LEAD_MAP);
			this.her.stopLeading();
			this.her.goWith(who);
			HerobrineMod.LOGGER.info("{} would not follow — addexio handed them the map and fell in behind",
				who.getName().getString());
		}

		private void stepAhead(ServerLevel here, Player who, BlockPos to) {
			double dx = to.getX() + 0.5 - who.getX();
			double dz = to.getZ() + 0.5 - who.getZ();
			double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
			dx /= len;
			dz /= len;
			net.minecraft.util.RandomSource roll = this.her.getRandom();
			for (int tries = 0; tries < 16; tries++) {
				double off = 10.0 + roll.nextDouble() * 4.0;
				double turn = (roll.nextDouble() - 0.5) * 0.8;
				double c = Math.cos(turn);
				double sn = Math.sin(turn);
				int x = net.minecraft.util.Mth.floor(who.getX() + (dx * c - dz * sn) * off);
				int z = net.minecraft.util.Mth.floor(who.getZ() + (dx * sn + dz * c) * off);
				BlockPos at = new BlockPos(x, com.bloomlet.herobrine.structure.Ground.topOf(here, x, z) + 1, z);
				if (!here.getBlockState(at).isAir() || !here.getBlockState(at.above()).isAir()
					|| !here.getBlockState(at.below()).isSolid() || !here.getFluidState(at).isEmpty()) {
					continue;
				}
				this.her.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.her.getYRot(), 0.0F);
				this.her.getNavigation().stop();
				HerobrineMod.LOGGER.info("addexio could not path the way — he is ten blocks ahead of {} instead",
					who.getName().getString());
				return;
			}
		}
	}

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
				&& !this.her.willNotGoDown(with)
				&& this.her.distanceTo(with) > (this.her.getTarget() != null || this.her.fleeing
					? CompanionEntity.FIGHTS_APART : CompanionEntity.DAWDLES_WITHIN);
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

			if (away > GIVES_UP_AND_APPEARS && !this.her.walkingIn()
				&& !this.her.willNotGoDown(with)
				&& this.her.level() instanceof ServerLevel here) {
				this.appearNear(here, with);
				return;
			}

			if (--this.repath > 0) {
				return;
			}
			this.repath = 14;                  // fewer corrections; less of the twitch

			// ---- AND OVER A LONG DISTANCE HE WALKS IT IN LEGS.
			//
			// Vanilla's navigation will not path much past thirty blocks — the
			// follow-range attribute caps it, and beyond that moveTo simply fails
			// and he stands still. That never showed up while the teleport was
			// covering every distance over twenty-six, because nothing was ever
			// asked to walk further than that.
			//
			// So while the entrance is running he is aimed at a point twenty blocks
			// along the line to you rather than at you, and re-aimed every ten
			// ticks as he closes. Each leg is inside what the pathfinder can
			// actually solve, and the legs are what make eighty blocks a walk
			// instead of a stand.
			if (this.her.walkingIn() && away > LEG) {
				net.minecraft.world.phys.Vec3 toward = with.position()
					.subtract(this.her.position()).normalize().scale(LEG);
				net.minecraft.core.BlockPos leg = net.minecraft.core.BlockPos.containing(
					this.her.position().add(toward));
				this.her.setSprinting(true);
				this.her.getNavigation().moveTo(leg.getX() + 0.5, leg.getY(),
					leg.getZ() + 0.5, 1.15);
				return;
			}
			// The catch-up modifier. A sprinting player pulls away from 1.0 no
			// matter what the base attribute is, because they are also going in a
			// straight line and she is going round things.
			double pace = away > HURRIES_AFTER ? 1.2 : 0.95;
			// SPRINTING, NOT JUST FASTER. He moved at 1.4x and looked like a villager
			// on a treadmill. Sprinting is what a player does and it is what a player
			// reads: the particles at his feet, and — see hop() — the jumps.
			this.her.setSprinting(pace > 1.0);
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
				// INTO THE AREA, NOT ONTO YOU. Eight to thirteen blocks off, any side:
				// you turn round and he is coming through the trees, not standing in
				// your face.
				double angle = roll.nextDouble() * Math.PI * 2.0;
				double off = 8.0 + roll.nextDouble() * 5.0;
				BlockPos at = with.blockPosition().offset(
					(int) Math.round(Math.cos(angle) * off), roll.nextInt(7) - 3,
					(int) Math.round(Math.sin(angle) * off));
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
		/**
		 * THE LOOP, AND WHY IT IS GONE.
		 *
		 * Hurt, he fled anything within twelve blocks, ate when it was further than
		 * twelve, dropped the bread when it came back, fled again — and since a
		 * zombie walks as fast as he does he never once got to twenty hearts, this
		 * goal never ended, and the sword goal under it never ran. Twenty loaves,
		 * no swing, a man running in a circle from a zombie with a diamond sword in
		 * his hand.
		 *
		 * Now he disengages only when he HAS room: nothing within FIGHTS_AT. A threat
		 * closer than that ends the goal and he turns and fights it — he cannot die,
		 * see hurtServer, so fighting is never the wrong call — and he eats when it is
		 * dead or he has genuinely got away. A loaf heals four, so recovering is three
		 * bites, not eight.
		 */
		private static final double FLEES = 8.0;
		private static final double FIGHTS_AT = 5.0;
		private static final int BITE_EVERY = 30;
		/** Vanilla's own eating cadence: LivingEntity spawns its crumbs on every 4th. */
		private static final int CRUMBS_EVERY = 4;
		private static final float A_BITE = 4.0F;

		private final CompanionEntity her;
		private int chewing;

		Falter(CompanionEntity her) {
			this.her = her;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.her.getHealth() < FALTERS_UNDER && this.nearestThreat(FIGHTS_AT) == null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.her.getHealth() < RECOVERED && this.nearestThreat(FIGHTS_AT) == null;
		}

		@Override
		public void start() {
			if (this.her.level() instanceof ServerLevel here) {
				Player with = this.her.companion();
				if (with != null) {
					Sayings.say(here, this.her, with, Sayings.SCARED);
				}
			}
		}

		@Override
		public void stop() {
			// THE BREAD IS IN THE OFF HAND, AND THE SWORD IS NEVER TOUCHED.
			//
			// This used to swap the MAIN hand between empty and bread, which was
			// harmless while his hands were empty and is not any more: he carries
			// enchanted diamond now, setItemInHand overwrites rather than stores,
			// and the first time he broke off to eat his sword would have stopped
			// existing. Permanently, and with no way to notice except wondering why
			// he had got worse.
			this.her.fleeing = false;
			this.her.stopUsingItem();
			this.her.shieldUp();
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
			Mob threat = this.nearestThreat(FLEES);
			this.her.fleeing = threat != null;
			if (threat != null) {
				// Bread away, shield back; the sword is already where it always is.
				if (this.her.getOffhandItem().is(Items.BREAD)) {
					this.her.stopUsingItem();
					this.her.shieldUp();
				}
				this.her.getLookControl().setLookAt(threat, 90.0F, 90.0F);
				if (this.her.getNavigation().isDone()) {
					Vec3 out = DefaultRandomPos.getPosAway(this.her, 16, 7,
						threat.position());
					if (out != null) {
						this.her.getNavigation().moveTo(out.x, out.y, out.z, 1.6);
					}
				}
				return;
			}
			// ---- NOTHING NEAR. SIT DOWN AND EAT, AND LOOK LIKE IT.
			//
			// This used to be a loaf appearing in his hand and one GENERIC_EAT every
			// twenty-five ticks. Three things were missing and all three are what
			// make eating READ as eating in vanilla: the arm does not come up, no
			// crumbs come off it, and a single sample every second and a quarter is
			// heard as a sound that keeps getting cut off rather than as somebody
			// chewing.
			//
			// startUsingItem is what raises the arm. It is the same call a player's
			// own eating goes through, so the pose, the timing and the item held to
			// the mouth are vanilla's rather than a guess.
			this.her.getNavigation().stop();
			// A MAN EATING LOOKS AROUND. He stood with the loaf and stared at one point
			// for the length of it, which is what a statue does. Every second: you,
			// if you are near; where the last thing that hit him was; or just the
			// room.
			if (this.her.tickCount % 20 == 0) {
				Player with = this.her.companion();
				int roll = this.her.getRandom().nextInt(10);
				if (with != null && this.her.distanceTo(with) < 10.0 && roll < 5) {
					this.her.getLookControl().setLookAt(with, 30.0F, 30.0F);
				} else if (this.her.lastAttacker != null && this.her.lastAttacker.isAlive()
					&& this.her.distanceTo(this.her.lastAttacker) < 16.0 && roll < 8) {
					this.her.getLookControl().setLookAt(this.her.lastAttacker, 30.0F, 30.0F);
				} else {
					double a = this.her.getRandom().nextDouble() * Math.PI * 2.0;
					this.her.getLookControl().setLookAt(this.her.getX() + Math.cos(a) * 6.0,
						this.her.getEyeY() - 0.5 + this.her.getRandom().nextDouble(),
						this.her.getZ() + Math.sin(a) * 6.0);
				}
			}
			ItemStack loaf = this.her.getOffhandItem();
			if (!loaf.is(Items.BREAD)) {
				loaf = new ItemStack(Items.BREAD);
				this.her.setItemInHand(InteractionHand.OFF_HAND, loaf);
			}
			if (!this.her.isUsingItem()) {
				this.her.startUsingItem(InteractionHand.OFF_HAND);
			}

			this.chewing++;
			// EVERY FOUR TICKS, WHICH IS VANILLA'S OWN RATE. LivingEntity fires its
			// eating effects on (useTime - remaining) % 4 == 0, and matching it is
			// the difference between a chew and a stutter.
			if (this.chewing % CRUMBS_EVERY == 0 && this.her.level()
					instanceof ServerLevel here) {
				crumbs(here, loaf);
				here.playSound(null, this.her.getX(), this.her.getY(), this.her.getZ(),
					SoundEvents.GENERIC_EAT, this.her.getSoundSource(), 0.9F,
					1.0F + (this.her.getRandom().nextFloat() - 0.5F) * 0.2F);
			}
			if (this.chewing < BITE_EVERY) {
				return;
			}
			this.chewing = 0;
			this.her.heal(A_BITE);
			if (this.her.level() instanceof ServerLevel here) {
				here.playSound(null, this.her.getX(), this.her.getY(), this.her.getZ(),
					SoundEvents.PLAYER_BURP, this.her.getSoundSource(), 0.5F,
					0.9F + this.her.getRandom().nextFloat() * 0.2F);
			}
		}

		/**
		 * The crumbs, thrown from his mouth in the direction he is facing.
		 *
		 * LivingEntity.spawnItemParticles does exactly this and is private, so it is
		 * reproduced: a point just under the eyes, pushed forward out of the head so
		 * the particles do not spawn inside it, and a small random shove so they
		 * scatter instead of falling in a line.
		 */
		private void crumbs(ServerLevel here, ItemStack loaf) {
			double yaw = -this.her.getYRot() * (Math.PI / 180.0);
			double pitch = -this.her.getXRot() * (Math.PI / 180.0);
			Vec3 out = new Vec3(
				(this.her.getRandom().nextDouble() - 0.5) * 0.3,
				-this.her.getRandom().nextDouble() * 0.6 - 0.3,
				0.6)
				.xRot((float) pitch).yRot((float) yaw);
			Vec3 at = this.her.position()
				.add(0.0, this.her.getEyeHeight() - 0.35, 0.0)
				.add(out.scale(0.4));
			here.sendParticles(
				// The ITEM, not the stack. 26.2's constructor takes an Item or an
				// ItemStackTemplate; the stack overload is gone.
				new net.minecraft.core.particles.ItemParticleOption(
					net.minecraft.core.particles.ParticleTypes.ITEM, loaf.getItem()),
				at.x, at.y, at.z, 4, out.x * 0.2, out.y * 0.2 + 0.05, out.z * 0.2, 0.02);
		}

		/**
		 * A THREAT IS ANYTHING THAT MEANS HIM HARM — not only one of Herobrine's.
		 * It used to be isHis() alone, which is how a zombie got to hit a man for a
		 * minute while he ate a loaf with his back to it: the zombie was not on the
		 * list. Now: his things, anything that has him as its target, and whatever
		 * hit him in the last three seconds.
		 */
		private long threatsAt = Long.MIN_VALUE;
		private List<Mob> threats = List.of();

		/**
		 * Asked by canUse, canContinueToUse and tick — several times a tick, each a
		 * box scan and a stream. The scan now runs once every five ticks at the
		 * widest radius anybody asks for, and every question is answered from that
		 * list with the same box test the scan used.
		 */
		private @org.jspecify.annotations.Nullable Mob nearestThreat(double within) {
			long now = this.her.level().getGameTime();
			if (now - this.threatsAt >= 5) {
				this.threatsAt = now;
				AABB wide = this.her.getBoundingBox().inflate(Math.max(within, FLEES));
				this.threats = this.her.level().getEntitiesOfClass(Mob.class, wide,
					m -> m != this.her && m.isAlive() && (Sayings.isHis(m)
						|| m.getTarget() == this.her
						|| (m == this.her.lastAttacker && now - this.her.lastAttackedAt < 60)));
			}
			AABB near = this.her.getBoundingBox().inflate(within);
			Mob best = null;
			double closest = Double.MAX_VALUE;
			for (Mob m : this.threats) {
				if (!m.isAlive() || !near.intersects(m.getBoundingBox())) {
					continue;
				}
				double d = this.her.distanceToSqr(m);
				if (d < closest) {
					closest = d;
					best = m;
				}
			}
			return best;
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
