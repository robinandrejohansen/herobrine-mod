package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * THE DEAD STAY WHERE THEY FELL.
 *
 * Addexio going down and lying there, arms out, was the best image the fight
 * produced, and it was the only death in the game that left anything behind but
 * a puff of smoke and a few items. So now everything does. A mob killed by
 * somebody — a player, Addexio, him, another mob — does not vanish: it drops
 * where it stands, lies there for good, and what it would have dropped is inside
 * it, opened like a chest with a right-click. Kill a field of them and the field
 * is full of them afterwards. It looks like what it was.
 *
 * Players too. A player's body is its own entity, wearing that player's skin,
 * and everything they carried is in it — nothing drops, so nothing is lost to
 * fire or to lava or to him. Come back and take it.
 *
 * The mob corpse is the mob itself, kept: its death is refused the way a totem
 * refuses it, then it is put to sleep — no AI, invulnerable, silent, persistent,
 * the sleeping pose (which lays any living model flat), and a lying-down hitbox
 * so it can be clicked (CorpseMixin). It keeps gravity, so mine the floor out
 * from under it and it falls like anything else. Natural deaths — falls, lava,
 * drowning, a mob farm — drop as they always did; this is for things somebody
 * killed. Herobrine is the one exception: he leaves.
 */
public final class Corpses {

	private Corpses() {}

	/** Set on the body. Synced, so the client knows what it is looking at and what it clicked. */
	public static final AttachmentType<Boolean> CORPSE = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.syncWith(ByteBufCodecs.BOOL.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("corpse"));

	/** What is in the pockets. Persistent, server only. */
	public static final AttachmentType<List<ItemStack>> LOOT = AttachmentRegistry.createPersistent(
		HerobrineMod.id("corpse_loot"), ItemStack.OPTIONAL_CODEC.listOf());

	public static boolean isCorpse(Entity what) {
		return Boolean.TRUE.equals(what.getAttached(CORPSE));
	}

	/**
	 * WHERE YOU FELL, AND THE WAY BACK.
	 *
	 * Your body holds everything and lies where you died; finding it again was
	 * the problem, three hundred blocks of forest later with no coordinates. So
	 * the spot is kept across the death (copyOnDeath), and when you come back
	 * to life you are handed a map drawn between where you stand and where you
	 * fell (Charts), with the coordinates in its name — and one line in chat
	 * with a button on it. The button runs /fell, which puts you back on the
	 * spot, once, and forgets it. Not a mechanic he has any say in.
	 */
	private static final AttachmentType<Long> FELL_AT = AttachmentRegistry.<Long>builder()
		.persistent(Codec.LONG)
		.copyOnDeath()
		.buildAndRegister(HerobrineMod.id("fell_at"));
	private static final AttachmentType<String> FELL_IN = AttachmentRegistry.<String>builder()
		.persistent(Codec.STRING)
		.copyOnDeath()
		.buildAndRegister(HerobrineMod.id("fell_in"));

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register(Corpses::onDeath);
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((was, now, alive) -> {
			if (!alive) {
				theWayBack(now);
			}
		});
		// The right button opens it, like a chest. The left clears it — see onHit.
		UseEntityCallback.EVENT.register(Corpses::onUse);
		AttackEntityCallback.EVENT.register(Corpses::onHit);
	}

	/**
	 * A SWING CLEARS IT. Hit a body and it is gone: a puff, a soft sound, and
	 * whatever was still in its pockets on the ground where it lay, so a careless
	 * swing costs nothing but the tidiness. This is how a field of them gets
	 * cleaned up without a shovel, and how a body in a doorway stops being in the
	 * doorway.
	 */
	private static InteractionResult onHit(Player player, Level world, InteractionHand hand, Entity entity,
	                                       @Nullable EntityHitResult hit) {
		if (!(entity instanceof LivingEntity dead) || !isCorpse(dead)) {
			return InteractionResult.PASS;
		}
		if (!(world instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;      // the client swings; the server clears
		}
		for (ItemStack stack : dead.getAttachedOrElse(LOOT, List.of())) {
			if (!stack.isEmpty()) {
				dead.spawnAtLocation(level, stack.copy());
			}
		}
		level.sendParticles(ParticleTypes.POOF, dead.getX(), dead.getY() + 0.4, dead.getZ(),
			12, 0.5, 0.2, 0.5, 0.02);
		level.playSound(null, dead.getX(), dead.getY(), dead.getZ(), SoundEvents.GENERIC_SMALL_FALL,
			SoundSource.NEUTRAL, 0.8F, 0.7F);
		dead.discard();
		HerobrineMod.LOGGER.info("{} cleared a body: {}", player.getName().getString(),
			dead.getType().toShortString());
		return InteractionResult.SUCCESS;
	}

	private static boolean onDeath(LivingEntity died, DamageSource source, float amount) {
		if (died instanceof ServerPlayer fell) {
			fell.setAttached(FELL_AT, fell.blockPosition().asLong());
			fell.setAttached(FELL_IN, fell.level().dimension().identifier().toString());
		}
		if (!Config.get().corpses || !(died.level() instanceof ServerLevel level)) {
			return true;
		}
		if (died instanceof ServerPlayer player) {
			leaveTheBody(level, player);
			return true;                       // the player still dies; only the drops changed
		}
		if (!(died instanceof Mob mob) || isCorpse(mob)
			|| died instanceof HerobrineEntity || died instanceof CompanionEntity
			|| died instanceof PlayerCorpseEntity
			|| !(source.getEntity() instanceof LivingEntity)) {
			return true;
		}
		if (mob.getType() == EntityTypes.ENDERMAN || mob.getType() == EntityTypes.ENDER_DRAGON
			|| mob.getType() == EntityTypes.WITHER || mob.getType().getCategory() == MobCategory.AMBIENT) {
			return true;                       // teleports, bosses, bats: let vanilla have them
		}

		// What the death would have dropped goes in the pockets instead.
		List<ItemStack> loot = new ArrayList<>();
		mob.getLootTable().ifPresent(key -> {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
			LootParams.Builder params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, mob)
				.withParameter(LootContextParams.ORIGIN, mob.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, source)
				.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
				.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
			if (source.getEntity() instanceof Player who) {
				params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, who);
			}
			table.getRandomItems(params.create(LootContextParamSets.ENTITY), mob.getLootTableSeed(), loot::add);
		});
		if (source.getEntity() instanceof Player) {
			int xp = mob.getExperienceReward(level, source.getEntity());
			if (xp > 0) {
				ExperienceOrb.award(level, mob.position(), xp);
			}
		}
		lay(mob, loot);
		return false;
	}

	/** The mob, kept, and put down. */
	/** A body a builder puts down: the dead in the streets, where he left them. It lies like every other. */
	public static void body(Mob mob, List<ItemStack> loot) {
		lay(mob, loot);
	}

	private static void lay(Mob mob, List<ItemStack> loot) {
		mob.setHealth(1.0F);
		mob.setAttached(CORPSE, true);
		mob.setAttached(LOOT, loot);
		mob.setTarget(null);
		mob.getNavigation().stop();
		mob.setNoAi(true);
		mob.setInvulnerable(true);
		mob.setSilent(true);
		mob.setPersistenceRequired();
		mob.setAggressive(false);
		mob.clearFire();
		mob.setDeltaMovement(0.0, 0.0, 0.0);
		mob.refreshDimensions();          // the lying-down box; the roll is the renderer's (CorpseRenderMixin)
	}

	/** A player's body, with everything they carried inside it. */
	private static void leaveTheBody(ServerLevel level, ServerPlayer player) {
		List<ItemStack> carried = new ArrayList<>();
		if (!level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			Inventory inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty()) {
					carried.add(stack.copy());
				}
			}
			inventory.clearContent();
		}
		PlayerCorpseEntity body = ModEntities.PLAYER_CORPSE.create(level, EntitySpawnReason.EVENT);
		if (body == null) {
			return;
		}
		body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
		String skin = null;
		String sig = null;
		for (com.mojang.authlib.properties.Property textures : player.getGameProfile().properties().get("textures")) {
			skin = textures.value();
			sig = textures.signature();
			break;
		}
		body.setWho(player.getUUID(), player.getName().getString(), skin, sig);
		body.setAttached(LOOT, carried);
		body.lay();
		level.addFreshEntity(body);
		HerobrineMod.LOGGER.info("{} fell at [{}, {}, {}] — the body holds {} stacks",
			player.getName().getString(), player.getBlockX(), player.getBlockY(), player.getBlockZ(),
			carried.size());
	}

	private static void theWayBack(ServerPlayer now) {
		Long packed = now.getAttached(FELL_AT);
		String in = now.getAttached(FELL_IN);
		if (packed == null || in == null || !(now.level() instanceof ServerLevel here)) {
			return;
		}
		net.minecraft.core.BlockPos fell = net.minecraft.core.BlockPos.of(packed);
		boolean sameWorld = here.dimension().identifier().toString().equals(in);
		String at = fell.getX() + ", " + fell.getY() + ", " + fell.getZ();
		if (sameWorld) {
			ItemStack map = com.bloomlet.herobrine.structure.Charts.between(here, now.blockPosition(), fell,
				"where you fell — " + at);
			if (!now.getInventory().add(map)) {
				now.drop(map, false);
			}
		}
		Component where = Component.literal("You fell at " + at + (sameWorld ? ". " : ", in another world. "))
			.withStyle(net.minecraft.ChatFormatting.GRAY);
		Component back = Component.literal("[Go back there]").withStyle(style -> style
			.withColor(net.minecraft.ChatFormatting.GOLD)
			.withUnderlined(true)
			.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/fell")));
		now.sendSystemMessage(where.copy().append(back));
	}

	/** /fell — back to where you last died, once. Returns whether it happened. */
	public static boolean goBack(ServerPlayer who) {
		Long packed = who.getAttached(FELL_AT);
		String in = who.getAttached(FELL_IN);
		if (packed == null || in == null) {
			who.sendSystemMessage(Component.literal("Nowhere to go back to.").withStyle(net.minecraft.ChatFormatting.GRAY));
			return false;
		}
		ServerLevel there = null;
		for (ServerLevel level : who.level().getServer().getAllLevels()) {
			if (level.dimension().identifier().toString().equals(in)) {
				there = level;
			}
		}
		if (there == null) {
			who.sendSystemMessage(Component.literal("That world is not here any more.").withStyle(net.minecraft.ChatFormatting.GRAY));
			return false;
		}
		net.minecraft.core.BlockPos fell = net.minecraft.core.BlockPos.of(packed);
		net.minecraft.core.BlockPos feet = footing(there, fell);
		if (feet == null) {
			who.sendSystemMessage(Component.literal("There is nothing to stand on where you fell.").withStyle(net.minecraft.ChatFormatting.GRAY));
			return false;
		}
		who.removeAttached(FELL_AT);
		who.removeAttached(FELL_IN);
		who.teleportTo(there, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
			java.util.Set.of(), who.getYRot(), who.getXRot(), true);
		who.sendSystemMessage(Component.literal("Back where you fell.").withStyle(net.minecraft.ChatFormatting.GRAY));
		HerobrineMod.LOGGER.info("{} went back to where they fell, [{}, {}, {}]",
			who.getName().getString(), feet.getX(), feet.getY(), feet.getZ());
		return true;
	}

	/** Two blocks of air on something solid, out of the water, within eight of where you died — or nothing. */
	private static net.minecraft.core.@Nullable BlockPos footing(ServerLevel level, net.minecraft.core.BlockPos fell) {
		level.getChunk(fell.getX() >> 4, fell.getZ() >> 4);      // once, for one command
		for (int dy = 0; dy <= 8; dy++) {
			for (int sign : new int[] { 1, -1 }) {
				net.minecraft.core.BlockPos feet = fell.above(dy * sign);
				if (feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY() - 1) {
					continue;
				}
				if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
					&& level.getBlockState(feet.below()).isSolid()
					&& level.getFluidState(feet).isEmpty() && level.getFluidState(feet.below()).isEmpty()) {
					return feet;
				}
				if (dy == 0) {
					break;
				}
			}
		}
		return null;
	}

	private static InteractionResult onUse(Player player, Level world, InteractionHand hand, Entity entity,
	                                       @Nullable EntityHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || !(entity instanceof LivingEntity dead) || !isCorpse(dead)) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer who)) {
			return InteractionResult.SUCCESS;      // the client swings; the server opens
		}
		int rows = dead instanceof PlayerCorpseEntity ? 6 : 3;
		Pockets pockets = new Pockets(dead, rows * 9);
		Component name = dead instanceof PlayerCorpseEntity body
			? Component.literal(body.whoName() + "'s body")
			: dead.getDisplayName();
		who.openMenu(new SimpleMenuProvider((id, inventory, p) -> rows == 6
			? ChestMenu.sixRows(id, inventory, pockets)
			: ChestMenu.threeRows(id, inventory, pockets), name));
		return InteractionResult.SUCCESS;
	}

	/** The chest view of a body. Every change is written straight back onto it. */
	private static final class Pockets extends SimpleContainer {
		private final LivingEntity of;

		Pockets(LivingEntity of, int size) {
			super(size);
			this.of = of;
			List<ItemStack> had = of.getAttachedOrElse(LOOT, List.of());
			for (int i = 0; i < had.size() && i < size; i++) {
				this.setItem(i, had.get(i).copy());
			}
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if (this.of == null) {
				return;                            // called from the super constructor
			}
			List<ItemStack> now = new ArrayList<>();
			for (int i = 0; i < this.getContainerSize(); i++) {
				ItemStack stack = this.getItem(i);
				if (!stack.isEmpty()) {
					now.add(stack.copy());
				}
			}
			this.of.setAttached(LOOT, now);
		}
	}
}
