package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.manifest.Mimicry;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * SOMEBODY WHO IS NOT ON THE SERVER, WEARING SOMEBODY WHO IS.
 *
 * The tab list says there are seven of you. There are six. The seventh has your
 * friend's skin, your friend's name over its head, and it is four hundred blocks
 * from where your friend says they are.
 *
 * WHY THIS IS THE STRONGEST THING IN THE MOD, and it is worth being explicit
 * because it is also the cheapest: everything else here frightens ONE player at
 * a time and can be talked down afterwards. This one cannot be talked down,
 * because the only way to resolve it is to ask the group, and asking the group
 * spreads it. It attacks the reason people are logged in at all — the other
 * people — and per README.md that is the whole of what he is trying to take.
 *
 * ALMOST LIKE A PLAYER, AND EXACTLY ONE THING WRONG. This is the entire design
 * and both halves are load-bearing:
 *
 *   - Behave perfectly and nobody notices. No scare, no event, wasted work.
 *   - Behave obviously wrong and it is a mob in a costume, which is worse than
 *     nothing, because now the mod has SHOWN them the trick.
 *
 * So he does what a player does — walks, wanders, looks around, crosses water,
 * climbs — and the wrong thing is not a behaviour at all. IT IS THAT HE DOES
 * NOT RESPOND. You wave, he keeps walking. You type his name, nothing. You get
 * close and he walks away, at exactly the pace you are walking, not fleeing and
 * not waiting. There is no animation for being ignored and it is the most
 * unnerving thing a person can do.
 *
 * HE WALKS AWAY RATHER THAN TOWARD, which is the single most important line in
 * this file. A copy of your friend coming at you is a threat, and a threat gets
 * a combat response — you swing at it, it dies or it does not, and either way
 * you know what it was. A copy of your friend walking away into the trees is a
 * QUESTION, and the player has to choose whether to follow it. That choice is
 * the event. Nothing here should ever take it away from them.
 *
 * AND IF YOU HIT HIM THERE IS NOTHING THERE. No death animation, no drops, no
 * damage dealt in either direction — he is simply gone, and so is the seventh
 * name in the tab list. A player who lands a hit has proved nothing to anybody
 * else, and has to go back and say "it vanished", which is not a sentence that
 * gets believed.
 */
public class MimicEntity extends PathfinderMob {

	/**
	 * The fake profile's id, as text.
	 *
	 * Sent to the client so the renderer knows whose skin to wear. Text rather
	 * than a UUID because 26.2 has no UUID entity-data serializer — there is a
	 * RESOLVABLE_PROFILE one, which would carry the textures itself and skip the
	 * tab list entirely, but the tab-list entry has to be sent anyway and
	 * resolving through it is the same path every real player already renders
	 * through. Fewer new failure modes than driving the skin manager by hand.
	 */
	private static final EntityDataAccessor<String> WEARING =
		SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);

	/**
	 * How long he lasts, in ticks. Two to four minutes.
	 *
	 * Long enough to be found, followed, lost, and argued about — the arguing is
	 * the point and it needs time to start. Short enough that he is gone before
	 * anybody organises a search party, because a search party that FINDS him
	 * standing still in a field has turned a ghost into an exhibit.
	 */
	/** He does not swing back until it is nearly over. Six of twenty. */
	private static final float FIGHTS_BACK_UNDER = 6.0F;

	private static final int LIFETIME = 2400;
	private static final int SPREAD = 2400;

	private int lifetime = LIFETIME;

	public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
		super(type, level);
		// Doors open for him the way they open for a player, and he steps up a
		// block without jumping. Both are things people never consciously notice
		// and both are instantly wrong when missing.
		this.getNavigation().setCanOpenDoors(true);
		this.getNavigation().setCanFloat(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			// A walking player does about 4.3 blocks a second. A zombie's 0.23
			// is visibly a shamble, and a shamble is a tell at any distance, so
			// this is set to read as somebody walking with somewhere to be.
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 20.0)
			// THREE OF THESE, AND WITHOUT THIS LINE IT CRASHED THE SERVER.
			//
			// Mob.createMobAttributes gives health, speed, armour, follow range and
			// knockback resistance. It does NOT give attack damage — that comes from
			// Monster.createMonsterAttributes or from the entity itself — and this
			// one is a PathfinderMob, so it had none.
			//
			// Mob.doHurtTarget reads getAttributeValue(ATTACK_DAMAGE) with nothing
			// guarding it, and AttributeMap throws on an attribute the supplier does
			// not carry. So TheFriend.strike took the server down the first time one
			// of these ever actually hit somebody.
			//
			// WHICH IS WHY IT SURVIVED THIS LONG. Striking is stage four of a goal
			// that has to come to you, greet you, go through your chests and change
			// its coat first, and it only runs at all while you are alone. Every
			// earlier stage works perfectly. The mod shipped a betrayal that could
			// not be reached without a hundred and eighty ticks of theatre, and the
			// hundred and eighty-first killed it.
			//
			// Three, so three hits is four and a half hearts. Between the Gaunt's
			// two and Herobrine's five, and survivable on purpose: this is the
			// moment you learn what it is, not the moment it kills you.
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// PRIORITY 1, ABOVE STROLLING: getting away is the one thing he is
		// deliberate about. Both speeds are 1.0 rather than the usual 1.0/1.2
		// walk-then-sprint, because a player who has decided to leave does not
		// break into a run, and a sprint would read as fleeing.
		// ABOVE THE AVOIDANCE, AND ONLY IN HIS WORLD. Out in the overworld the whole
		// point of him is that he will not come near you and will not look at you.
		// Over there you are the one who is a long way from anybody, and the thing
		// that is worth doing is the opposite.
		this.goalSelector.addGoal(1, new TheFriend(this));
		// Only ever reached once hurtServer has given him a target, which only
		// happens in his world and only under six health.
		this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(
			this, 1.15, true));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0, 1.0));
		// Above strolling, so that when he has a pickaxe he is a person who came
		// down here to do something rather than a person wandering a cave.
		this.goalSelector.addGoal(3, new Mining(this));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
		// And no LookAtPlayerGoal anywhere in here, which is the omission that
		// does the work. Every mob in the game turns to watch you; being looked
		// straight through is what people report as the thing that got them.
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	/**
	 * THE ONE WHO COMES OVER AND SAYS HELLO.
	 *
	 * The mimic already wears somebody's skin, carries their name over its head and
	 * puts a seventh row in a six-player tab list. What it has never done is BEHAVE
	 * like them, and the behaviour is where the whole thing pays off — because
	 * every single beat below is something a real player does, and a player who has
	 * been on a server for a week knows all of them by heart.
	 *
	 *     walks over            not toward you exactly. over.
	 *     crouch-spams          the universal I-am-friendly. everybody does this.
	 *     goes through a chest  and takes the good stuff, the way anybody would
	 *     PUTS THE ARMOUR ON    in front of you, piece by piece
	 *     hits you              three times, which is a joke between friends
	 *     and sprints off       at twice the speed anything alive can run
	 *
	 * The sequence is doing one job: every step reads as a person right up until
	 * the last one, and the last one is not survivable as an interpretation. There
	 * is no moment where a player thinks "that is a mob". There is a moment where
	 * they think "why is he not answering" and then a much worse one.
	 *
	 * THE ARMOUR IS THE CENTREPIECE. Taking it out of the chest is a mechanic;
	 * standing there putting it on, one piece every second, while you watch, is a
	 * PERSON. It also has a mechanical point: whatever he leaves with is gone, so
	 * the visit costs something real, and the next time you see that skin it will
	 * be wearing your iron.
	 *
	 * ONLY WHERE NOBODY CAN CHECK. Mimicry already refuses to place him unless the
	 * players are eighty blocks apart — the entire scare rests on not being able to
	 * shout across and ask. This runs in his world, where they are further apart
	 * than that and the map does not help.
	 */
	private static final class TheFriend extends net.minecraft.world.entity.ai.goal.Goal {
		private static final double COMES_TO = 6.0;
		private static final int GREET_FOR = 90;
		private static final int GREET_EVERY = 7;
		private static final double LOOKS_FOR_A_CHEST = 22.0;
		private static final int DRESSES_EVERY = 22;
		private static final int STRIKES = 3;
		private static final double GETS_AWAY = 46.0;
		/** Twice a sprint. Nothing alive moves like this and that is the point. */
		private static final double BOLTS = 2.0;

		private final MimicEntity him;
		private net.minecraft.world.entity.player.@org.jspecify.annotations.Nullable Player mark;
		private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos box;
		private final java.util.List<net.minecraft.world.item.ItemStack> took =
			new java.util.ArrayList<>();
		private int stage;
		private int held;

		private TheFriend(MimicEntity him) {
			this.him = him;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!this.him.level().dimension().equals(
					com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
				return false;
			}
			this.mark = this.him.level().getNearestPlayer(this.him, 40.0);
			return this.mark != null;
		}

		@Override
		public boolean canContinueToUse() {
			// A fight ends the visit. Once he has turned round he is not going back
			// to crouching at you.
			return this.mark != null && this.mark.isAlive() && this.stage <= 5
				&& this.him.getTarget() == null;
		}

		@Override
		public void start() {
			this.stage = 0;
			this.held = 0;
			this.took.clear();
			this.box = null;
		}

		@Override
		public void stop() {
			this.him.setShiftKeyDown(false);
			this.him.setSprinting(false);
			this.mark = null;
		}

		@Override
		public void tick() {
			if (this.mark == null || !(this.him.level() instanceof ServerLevel here)) {
				return;
			}
			this.held++;
			switch (this.stage) {
				case 0 -> approach();
				case 1 -> greet();
				case 2 -> ransack(here);
				case 3 -> dress();
				case 4 -> strike(here);
				default -> flee();
			}
		}

		/** Walks over. Ordinary pace — a person crossing a clearing. */
		private void approach() {
			this.him.getLookControl().setLookAt(this.mark, 30.0F, 30.0F);
			this.him.getNavigation().moveTo(this.mark, 1.0);
			if (this.him.distanceTo(this.mark) <= COMES_TO) {
				this.him.getNavigation().stop();
				this.stage = 1;
				this.held = 0;
			}
		}

		/**
		 * Crouch-spam, which is the closest thing Minecraft has to a handshake.
		 *
		 * It is also the only part of this a player will describe afterwards as the
		 * bit that got them, because it is not a threat — it is somebody being
		 * NICE, and it is being done by something that is about to hit them.
		 */
		private void greet() {
			this.him.getLookControl().setLookAt(this.mark, 30.0F, 30.0F);
			this.him.setShiftKeyDown((this.held / GREET_EVERY) % 2 == 0);
			if (this.held >= GREET_FOR) {
				this.him.setShiftKeyDown(false);
				this.stage = 2;
				this.held = 0;
			}
		}

		/** Goes through the nearest chest, and takes what anybody would take. */
		private void ransack(ServerLevel here) {
			if (this.box == null) {
				this.box = nearestBox(here);
				if (this.box == null) {
					this.stage = 4;      // nothing to rob. straight to the joke.
					this.held = 0;
					return;
				}
			}
			this.him.getLookControl().setLookAt(this.box.getX() + 0.5,
				this.box.getY() + 0.5, this.box.getZ() + 0.5, 30.0F, 30.0F);
			this.him.getNavigation().moveTo(this.box.getX() + 0.5, this.box.getY(),
				this.box.getZ() + 0.5, 1.0);
			if (this.him.blockPosition().distSqr(this.box) > 9.0 && this.held < 200) {
				return;
			}
			this.him.getNavigation().stop();
			if (here.getBlockEntity(this.box)
					instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				here.playSound(null, this.box,
					net.minecraft.sounds.SoundEvents.CHEST_OPEN,
					this.him.getSoundSource(), 0.8F, 1.0F);
				for (int slot = 0; slot < chest.getContainerSize(); slot++) {
					net.minecraft.world.item.ItemStack in = chest.getItem(slot);
					if (in.isEmpty() || this.took.size() >= 5) {
						continue;
					}
					net.minecraft.world.entity.EquipmentSlot where =
						this.him.getEquipmentSlotForItem(in);
					// SwordItem and AxeItem stopped being classes — weapons are
					// data-driven now and the tags are the only honest question.
					if (where == net.minecraft.world.entity.EquipmentSlot.MAINHAND
						&& !in.is(net.minecraft.tags.ItemTags.SWORDS)
						&& !in.is(net.minecraft.tags.ItemTags.AXES)) {
						continue;      // he is not here for your carrots
					}
					this.took.add(in.copy());
					chest.setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
				}
				chest.setChanged();
				here.playSound(null, this.box,
					net.minecraft.sounds.SoundEvents.CHEST_CLOSE,
					this.him.getSoundSource(), 0.8F, 1.0F);
			}
			this.stage = 3;
			this.held = 0;
		}

		/** And puts it on, one piece at a time, standing where you can see him. */
		private void dress() {
			this.him.getNavigation().stop();
			this.him.getLookControl().setLookAt(this.mark, 30.0F, 30.0F);
			if (this.held % DRESSES_EVERY != 0) {
				return;
			}
			if (this.took.isEmpty()) {
				this.stage = 4;
				this.held = 0;
				return;
			}
			net.minecraft.world.item.ItemStack on = this.took.remove(0);
			net.minecraft.world.entity.EquipmentSlot where =
				this.him.getEquipmentSlotForItem(on);
			this.him.setItemSlot(where, on);
			// AND IT DROPS. He took it out of your chest in front of you; killing him
			// is how you get it back, and that is the only reason to try.
			this.him.setDropChance(where, 1.0F);
			if (this.him.level() instanceof ServerLevel here) {
				here.playSound(null, this.him.blockPosition(),
					net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON.value(),
					this.him.getSoundSource(), 0.9F, 1.0F);
			}
		}

		/** Three hits. Between friends that is a joke. */
		private void strike(ServerLevel here) {
			this.him.getLookControl().setLookAt(this.mark, 30.0F, 30.0F);
			this.him.getNavigation().moveTo(this.mark, 1.1);
			if (this.him.distanceTo(this.mark) > 2.6) {
				return;
			}
			if (this.held % 14 != 0) {
				return;
			}
			this.him.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			this.him.doHurtTarget(here, this.mark);
			if (this.held / 14 >= STRIKES) {
				this.stage = 5;
				this.held = 0;
			}
		}

		/**
		 * And then he leaves at twice the speed of anything that runs.
		 *
		 * This is the beat the whole visit was built to deliver. Everything before
		 * it is deniable — a quiet player, a rude player, a player messing about.
		 * A figure crossing open ground at double sprint is not a player and cannot
		 * be made into one, and it happens AFTER the player has spent a minute
		 * deciding it was one.
		 */
		private void flee() {
			this.him.setSprinting(true);
			double dx = this.him.getX() - this.mark.getX();
			double dz = this.him.getZ() - this.mark.getZ();
			double span = Math.max(0.001, Math.hypot(dx, dz));
			this.him.getNavigation().moveTo(
				this.him.getX() + dx / span * 24.0, this.him.getY(),
				this.him.getZ() + dz / span * 24.0, BOLTS);
			if (this.him.distanceTo(this.mark) >= GETS_AWAY || this.held > 400) {
				this.him.vanish();
				this.stage = 6;
			}
		}

		private net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos nearestBox(
				ServerLevel here) {
			int r = (int) LOOKS_FOR_A_CHEST;
			net.minecraft.core.BlockPos best = null;
			double nearest = Double.MAX_VALUE;
			for (net.minecraft.core.BlockPos at : net.minecraft.core.BlockPos.betweenClosed(
					this.him.blockPosition().offset(-r, -4, -r),
					this.him.blockPosition().offset(r, 4, r))) {
				if (!here.getBlockState(at).is(net.minecraft.world.level.block.Blocks.CHEST)) {
					continue;
				}
				double away = at.distSqr(this.him.blockPosition());
				if (away < nearest) {
					nearest = away;
					best = at.immutable();
				}
			}
			return best;
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WEARING, "");
	}

	public void wear(java.util.UUID profile, String name) {
		this.entityData.set(WEARING, profile.toString());
		// The floating nameplate, which is not decoration — a figure with no
		// name over it is identifiable as not-a-player from across a valley.
		this.setCustomName(net.minecraft.network.chat.Component.literal(name));
		this.setCustomNameVisible(true);
	}

	public String wearing() {
		return this.entityData.get(WEARING);
	}

	/** What somebody who came down here would be holding. */
	public void giveMiningKit() {
		this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
			new net.minecraft.world.item.ItemStack(
				net.minecraft.world.item.Items.IRON_PICKAXE));
	}

	private boolean hasPickaxe() {
		return this.getMainHandItem().is(net.minecraft.world.item.Items.IRON_PICKAXE);
	}

	/**
	 * HE IS MINING, AND THE STONE ACTUALLY COMES AWAY.
	 *
	 * The tell that sells everything else. A figure walking through a cave is a
	 * mob taking a path; a figure facing a wall, swinging, with the cracks
	 * spreading across the block in front of him, is unmistakably somebody
	 * playing the game. It is also the behaviour a player is LEAST suspicious of,
	 * because it is what they came down here to do themselves.
	 *
	 * It really breaks the block, and the crack overlay really advances, because
	 * the alternative — swinging at a wall that never yields — is the exact
	 * uncanny detail that makes a watcher realise they are looking at a script.
	 * Better to lose a block of stone.
	 *
	 * AND ONLY PLAIN ROCK, from a whitelist. Not ores, not anything crafted,
	 * nothing anybody put anywhere. This is the one system in the mod that
	 * removes blocks near a player without being asked to, and README.md's rule
	 * about the player's world versus the world's world applies hardest here:
	 * mining a metre of stone in a cave costs nobody anything, and a fake player
	 * who chews through somebody's wall has stopped being a scare and become the
	 * reason they uninstall.
	 *
	 * He also drops nothing. What he mines, he keeps — which is what a player
	 * does, and which means no trail of cobblestone items pointing at where he
	 * was standing.
	 */
	private static class Mining extends net.minecraft.world.entity.ai.goal.Goal {
		/** About two seconds a block: an iron pickaxe on stone, roughly. */
		private static final int SWING_EVERY = 6;
		private static final int SWINGS = 5;

		private final MimicEntity mob;
		private @Nullable BlockPos face;
		private int swings;
		private int cooldown;

		Mining(MimicEntity mob) {
			this.mob = mob;
			this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!this.mob.hasPickaxe() || this.mob.getRandom().nextInt(60) != 0) {
				return false;
			}
			this.face = this.wall();
			return this.face != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.face != null && this.swings < SWINGS
				&& worthMining(this.mob.level(), this.face);
		}

		@Override
		public void start() {
			this.swings = 0;
			this.cooldown = 0;
			this.mob.getNavigation().stop();
		}

		@Override
		public void stop() {
			// Clear the crack overlay whichever way this ended, or a half-broken
			// block is left sitting there as a signpost saying somebody was here.
			if (this.face != null) {
				this.mob.level().destroyBlockProgress(this.mob.getId(), this.face, -1);
			}
			this.face = null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (this.face == null) {
				return;
			}
			this.mob.getLookControl().setLookAt(
				this.face.getX() + 0.5, this.face.getY() + 0.5, this.face.getZ() + 0.5);
			if (--this.cooldown > 0) {
				return;
			}
			this.cooldown = SWING_EVERY;
			this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			this.swings++;
			this.mob.level().destroyBlockProgress(this.mob.getId(), this.face,
				Math.min(9, this.swings * 10 / SWINGS));
			if (this.swings >= SWINGS) {
				this.mob.level().destroyBlock(this.face, false, this.mob, 512);
			}
		}

		/** A block at head or chest height he could plausibly be working on. */
		private @Nullable BlockPos wall() {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				for (int up = 0; up <= 1; up++) {
					BlockPos at =
						this.mob.blockPosition().above(up).relative(dir);
					if (worthMining(this.mob.level(), at)) {
						return at;
					}
				}
			}
			return null;
		}

		private static boolean worthMining(net.minecraft.world.level.Level level,
		                                   BlockPos at) {
			net.minecraft.world.level.block.state.BlockState state = level.getBlockState(at);
			return state.is(net.minecraft.world.level.block.Blocks.STONE)
				|| state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)
				|| state.is(net.minecraft.world.level.block.Blocks.ANDESITE)
				|| state.is(net.minecraft.world.level.block.Blocks.DIORITE)
				|| state.is(net.minecraft.world.level.block.Blocks.GRANITE)
				|| state.is(net.minecraft.world.level.block.Blocks.TUFF)
				|| state.is(net.minecraft.world.level.block.Blocks.DIRT)
				|| state.is(net.minecraft.world.level.block.Blocks.GRAVEL);
		}
	}

	public void setLifetime(net.minecraft.util.RandomSource random) {
		this.lifetime = LIFETIME + random.nextInt(SPREAD);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && --this.lifetime <= 0) {
			this.vanish();
		}
	}

	/**
	 * Gone, rather than dead.
	 *
	 * discard() and not kill(): no death animation, no sound, no drops, no
	 * particles. The distinction matters more than it looks. A death is an
	 * outcome — it happened, it is over, the player won. A disappearance is
	 * unfinished business, and unfinished business is what gets talked about in
	 * voice chat for the next hour.
	 */
	public void vanish() {
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// OUT IN THE OVERWORLD HE IS ABSENT. A hit does not hurt him because there
		// is nothing there to hurt, and the swing that proves it is also the swing
		// that removes the evidence. That is the seventh-name scare and it depends
		// on him not being a thing you can fight.
		if (!this.level().dimension().equals(
				com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			this.vanish();
			return false;
		}

		// IN HIS WORLD HE BLEEDS, AND THAT IS A DIFFERENT AND BETTER SCARE.
		//
		// Vanishing says "it was never real" and hands the player an explanation.
		// Twenty health, ordinary damage, an ordinary death and your own iron on
		// the ground afterwards says the opposite, and there is no explanation to
		// reach for. You killed somebody wearing your friend's face and their name
		// is still on the tab list.
		//
		// Twenty is a player's exactly, and it is deliberate: he goes down in the
		// number of hits a player would, which is the last thing making him read as
		// one right up to the moment he stops.
		boolean took = super.hurtServer(level, source, damage);
		if (took && this.getHealth() <= FIGHTS_BACK_UNDER
			&& source.getEntity() instanceof Player who) {
			// AND HE ONLY SWINGS BACK AT THE END, which is the note as given: just
			// enough to seem real. Somebody who fights from the first hit is a mob.
			// Somebody who takes four in silence and then turns round is a person
			// who has decided you meant it.
			this.setTarget(who);
		}
		return took;
	}

	/**
	 * THE ROW COMES OUT NO MATTER HOW HE LEAVES.
	 *
	 * vanish() already does this, but vanish() is only one of the ways an entity
	 * stops existing — /kill, a chunk unloading, a dimension change, an admin
	 * clearing entities, something in another mod. Every one of those paths ends
	 * here, so every one of them cleans up.
	 *
	 * Worth the belt and braces because the failure is not a missed scare, it is
	 * a name wedged in everybody's tab list until the server restarts. That does
	 * not read as haunted. It reads as a broken mod, which is the one impression
	 * none of this survives.
	 */
	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide()) {
			Mimicry.retire(this);
		}
		super.remove(reason);
	}

	/** He never fights, so nothing about him is ever a fight. */
	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		// His own clock ends him. Vanilla despawning would take him out mid-walk
		// while somebody was following him from just outside tracking range,
		// which is the one moment this whole thing exists to produce.
		return false;
	}
}
