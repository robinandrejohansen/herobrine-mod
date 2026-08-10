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
 * people — and per DESIGN.md §0 that is the whole of what he is trying to take.
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
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// PRIORITY 1, ABOVE STROLLING: getting away is the one thing he is
		// deliberate about. Both speeds are 1.0 rather than the usual 1.0/1.2
		// walk-then-sprint, because a player who has decided to leave does not
		// break into a run, and a sprint would read as fleeing.
		this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0, 1.0));
		// Above strolling, so that when he has a pickaxe he is a person who came
		// down here to do something rather than a person wandering a cave.
		this.goalSelector.addGoal(2, new Mining(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
		// And no LookAtPlayerGoal anywhere in here, which is the omission that
		// does the work. Every mob in the game turns to watch you; being looked
		// straight through is what people report as the thing that got them.
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
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
	 * removes blocks near a player without being asked to, and DESIGN.md's rule
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
		// Not invulnerable, not tough: ABSENT. A hit does not hurt him because
		// there is nothing there to hurt, and the swing that proves it is also
		// the swing that removes the evidence.
		this.vanish();
		return false;
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
