package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A PLAYER'S BODY. Where they died, wearing their skin, holding what they had.
 * See Corpses. It has no mind and no goals; it lies down and it stays.
 */
public class PlayerCorpseEntity extends Mob {

	public static final AttachmentType<String> WHO_NAME = AttachmentRegistry.<String>builder()
		.persistent(Codec.STRING)
		.syncWith(ByteBufCodecs.STRING_UTF8.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("body_of_name"));
	public static final AttachmentType<UUID> WHO_ID = AttachmentRegistry.<UUID>builder()
		.persistent(UUIDUtil.CODEC)
		.syncWith(UUIDUtil.STREAM_CODEC.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("body_of"));
	/**
	 * THE SKIN TRAVELS WITH THE BODY. A profile with only a name and an id gets the
	 * default skin — the client never fetches textures for it — which is why your
	 * friend's body was Steve while yours (already cached by your own client) was
	 * you. The server has the texture property on the player's profile at the
	 * moment they die; it is written onto the body, synced, and the renderer
	 * builds a full profile from it. Works after they log off, and on servers
	 * where the skin was never in the tab list.
	 */
	public static final AttachmentType<String> WHO_SKIN = AttachmentRegistry.<String>builder()
		.persistent(Codec.STRING)
		.syncWith(ByteBufCodecs.STRING_UTF8.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("body_of_skin"));
	public static final AttachmentType<String> WHO_SIG = AttachmentRegistry.<String>builder()
		.persistent(Codec.STRING)
		.syncWith(ByteBufCodecs.STRING_UTF8.cast(), AttachmentSyncPredicate.all())
		.buildAndRegister(HerobrineMod.id("body_of_sig"));

	public PlayerCorpseEntity(EntityType<? extends PlayerCorpseEntity> type, Level level) {
		super(type, level);
		this.setNoAi(true);
		this.setInvulnerable(true);
		this.setSilent(true);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	void setWho(UUID id, String name, @org.jspecify.annotations.Nullable String skin,
	            @org.jspecify.annotations.Nullable String signature) {
		this.setAttached(WHO_ID, id);
		this.setAttached(WHO_NAME, name);
		if (skin != null && !skin.isEmpty()) {
			this.setAttached(WHO_SKIN, skin);
			this.setAttached(WHO_SIG, signature == null ? "" : signature);
		}
	}

	public String whoSkin() {
		return this.getAttachedOrElse(WHO_SKIN, "");
	}

	public String whoSig() {
		return this.getAttachedOrElse(WHO_SIG, "");
	}

	public String whoName() {
		return this.getAttachedOrElse(WHO_NAME, "somebody");
	}

	public java.util.@org.jspecify.annotations.Nullable UUID whoId() {
		return this.getAttached(WHO_ID);
	}

	void lay() {
		this.setAttached(Corpses.CORPSE, true);
		this.setPose(Pose.SLEEPING);
		this.refreshDimensions();
	}

	@Override
	protected void registerGoals() {
		// none. It is dead.
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getPose() != Pose.SLEEPING) {
			this.setPose(Pose.SLEEPING);
		}
		if (this.getRemainingFireTicks() > 0) {
			this.clearFire();
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return EntityDimensions.fixed(2.2F, 1.0F);      // a body lying down: a metre tall and a body-length wide, so it opens from where you stand
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}
}
