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

	void setWho(UUID id, String name) {
		this.setAttached(WHO_ID, id);
		this.setAttached(WHO_NAME, name);
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
