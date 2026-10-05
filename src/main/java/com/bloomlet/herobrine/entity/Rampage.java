package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.manifest.HisHost;
import com.bloomlet.herobrine.structure.Ground;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * HE GOES AMOK, AND YOU WATCH.
 *
 * In the first two acts the fight is not a duel. He is not on you; he is taking
 * his own world apart — the trees, the city, his own castle — and the horror is
 * having to watch it from a distance you can survive. Each work is the tallest
 * thing he can see from where he is: a stand of trees, a wall, a tower, a house.
 * He blinks to it and spends six or eight seconds on it — fire into it, a bolt
 * on top, pieces punched out of it — and then the next. Nothing that holds
 * anything (chests, barrels) and not the door you came in by is ever broken
 * (HisHost.punch), and never anything within a few blocks of one of you.
 *
 * And then somebody hits him. Everything stops: he turns on the spot, looks at
 * whoever did it, and stands there two seconds with the heartbeat going. Then he
 * answers — see Duel.amok — and goes back to what he was doing.
 *
 * Owned by Duel, which decides when this runs. Cost: one action a second or so,
 * each one bounded (a punch of radius under three, one fireball, one bolt).
 */
final class Rampage {

	private static final int STARE = 40;
	private static final int WORK_MIN = 120;
	private static final int WORK_SPREAD = 50;
	private static final double LOOKS_MIN = 10.0;
	private static final double LOOKS_MAX = 30.0;
	private static final int TRIES = 28;

	private final HerobrineEntity him;
	private BlockPos spot;
	private int workLeft;
	private int actionIn;
	private UUID starer;
	private int stareLeft;
	private int hits;

	Rampage(HerobrineEntity him) {
		this.him = him;
	}

	/** Somebody hit him while he was at it. */
	void struck(ServerLevel here, ServerPlayer by) {
		this.starer = by.getUUID();
		this.stareLeft = STARE;
		this.hits++;
		this.him.getNavigation().stop();
		this.him.face(by);
		here.playSound(null, this.him.getX(), this.him.getY(), this.him.getZ(),
			SoundEvents.WARDEN_HEARTBEAT, this.him.getSoundSource(), 3.0F, 0.5F);
	}

	boolean staring() {
		return this.stareLeft > 0;
	}

	int hits() {
		return this.hits;
	}

	/** One tick of the stare. Returns the one who hit him on the tick it ends — time to answer — else null. */
	ServerPlayer stare(ServerLevel here) {
		ServerPlayer who = this.starer == null ? null
			: here.getPlayerByUUID(this.starer) instanceof ServerPlayer p ? p : null;
		this.him.getNavigation().stop();
		this.him.setDeltaMovement(0.0, this.him.getDeltaMovement().y, 0.0);
		if (who != null) {
			this.him.face(who);
		}
		if (this.stareLeft % 20 == 0) {
			here.playSound(null, this.him.getX(), this.him.getY(), this.him.getZ(),
				SoundEvents.WARDEN_HEARTBEAT, this.him.getSoundSource(), 3.0F, 0.5F);
		}
		if (--this.stareLeft > 0) {
			return null;
		}
		return who;
	}

	/** One tick of the wrecking, around `around`, never within `spare` of anyone. */
	void tick(ServerLevel here, List<Player> players, BlockPos around, double spare, int act) {
		RandomSource random = this.him.getRandom();
		if (this.spot == null || this.workLeft <= 0 || near(this.spot, players, spare)) {
			this.spot = this.pick(here, around, players, spare, random);
			this.workLeft = WORK_MIN + random.nextInt(WORK_SPREAD);
			this.actionIn = 15;
			if (this.spot == null) {
				this.him.getNavigation().stop();
				return;
			}
			this.standNear(here, this.spot, players, random);
		}
		this.him.getNavigation().stop();
		double tx = this.spot.getX() + 0.5;
		double tz = this.spot.getZ() + 0.5;
		this.him.getLookControl().setLookAt(tx, this.spot.getY() + 3.0, tz, 90.0F, 90.0F);
		float yaw = (float) (Math.atan2(tz - this.him.getZ(), tx - this.him.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.him.setYRot(yaw);
		this.him.setYBodyRot(yaw);
		this.workLeft--;
		if (--this.actionIn > 0) {
			return;
		}
		this.actionIn = 18 + random.nextInt(18);
		BlockPos at = this.pointOn(here, this.spot, random);
		int roll = random.nextInt(100);
		if (roll < 40) {
			this.fireball(here, at, act);
		} else if (roll < 65) {
			int top = here.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
			this.him.strikeAt(here, at.getX() + 0.5, top, at.getZ() + 0.5, act >= 2 && Config.get().realLightning);
		} else if (roll < 90 || act < 2) {
			this.him.swipeAt();
			int gone = HisHost.punch(here, at, 1.6 + random.nextDouble() * (0.5 + 0.4 * act));
			if (gone > 0) {
				here.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), this.him.getSoundSource(), 1.6F, 0.6F);
			}
		} else {
			this.ignite(here, at, random);      // from act two only: act one does not set fire
		}
	}

	/** The tallest thing in sight: a tree, a wall, a tower, a roof. Ground if there is nothing standing. */
	private BlockPos pick(ServerLevel here, BlockPos around, List<Player> players, double spare, RandomSource random) {
		BlockPos best = null;
		int tallest = -1;
		for (int i = 0; i < TRIES; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double r = LOOKS_MIN + random.nextDouble() * (LOOKS_MAX - LOOKS_MIN);
			int x = around.getX() + (int) Math.round(Math.cos(angle) * r);
			int z = around.getZ() + (int) Math.round(Math.sin(angle) * r);
			if (!here.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			int ground = Ground.topOf(here, x, z);
			BlockPos at = new BlockPos(x, ground, z);
			if (near(at, players, spare)) {
				continue;
			}
			int tall = here.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1 - ground;
			if (tall > tallest) {
				tallest = tall;
				best = at;
			}
		}
		return best;
	}

	/** A block of the thing: within two columns of it, anywhere from the ground to its top. */
	private BlockPos pointOn(ServerLevel here, BlockPos spot, RandomSource random) {
		int x = spot.getX() + random.nextInt(5) - 2;
		int z = spot.getZ() + random.nextInt(5) - 2;
		int ground = spot.getY();
		int top = Math.max(ground + 1, here.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1);
		return new BlockPos(x, ground + random.nextInt(Math.max(1, top - ground + 1)), z);
	}

	/** Somewhere to stand five to seven blocks from it, with room for him, clear of everyone. */
	private void standNear(ServerLevel here, BlockPos spot, List<Player> players, RandomSource random) {
		int needs = (int) Math.ceil(1.8 * this.him.getAttributeValue(Attributes.SCALE)) + 1;
		for (int i = 0; i < 12; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double r = 5.0 + random.nextDouble() * 2.0;
			int x = spot.getX() + (int) Math.round(Math.cos(angle) * r);
			int z = spot.getZ() + (int) Math.round(Math.sin(angle) * r);
			if (!here.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos feet = new BlockPos(x, Ground.topOf(here, x, z) + 1, z);
			if (near(feet, players, 3.0) || !here.getBlockState(feet.below()).isSolid()) {
				continue;
			}
			boolean room = true;
			for (int up = 0; up < needs && room; up++) {
				room = here.getBlockState(feet.above(up)).isAir();
			}
			if (!room) {
				continue;
			}
			float yaw = (float) (Math.atan2(spot.getZ() - z, spot.getX() - x) * (180.0 / Math.PI)) - 90.0F;
			this.him.blinkTo(x + 0.5, feet.getY(), z + 0.5, yaw);
			return;
		}
	}

	private void fireball(ServerLevel here, BlockPos at, int act) {
		Vec3 from = this.him.getEyePosition();
		Vec3 to = Vec3.atCenterOf(at).subtract(from);
		LargeFireball ball = new LargeFireball(here, this.him, to.normalize(), act >= 2 ? 2 : 1);
		ball.setAttached(HerobrineEntity.BREACH, true);
		ball.setAttached(HerobrineEntity.BREACH_AT, at.asLong());
		if (act == 1) {
			ball.setAttached(HerobrineEntity.COLD, true);      // act one: it bursts, nothing catches
		}
		ball.snapTo(from.x, from.y, from.z, this.him.getYRot(), this.him.getXRot());
		ball.shoot(to.x, to.y, to.z, 1.4F, 1.0F);
		here.addFreshEntity(ball);
		this.him.swipeAt();
		here.playSound(null, this.him.getX(), this.him.getY(), this.him.getZ(),
			SoundEvents.BLAZE_SHOOT, this.him.getSoundSource(), 1.5F, 0.6F);
	}

	private void ignite(ServerLevel here, BlockPos at, RandomSource random) {
		for (int i = 0; i < 3; i++) {
			BlockPos flame = at.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
			if (here.getBlockState(flame).isAir() && here.getBlockState(flame.below()).isSolid()) {
				here.setBlock(flame, BaseFireBlock.getState(here, flame), 3);
			}
		}
	}

	private static boolean near(BlockPos at, List<Player> players, double spare) {
		for (Player p : players) {
			if (p.isAlive() && !p.isSpectator() && p.blockPosition().closerThan(at, spare)) {
				return true;
			}
		}
		return false;
	}
}
