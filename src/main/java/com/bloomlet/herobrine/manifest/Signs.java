package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.Heat;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * He writes on your walls.
 *
 * The first thing in the whole mod that leaves something you cannot argue
 * with. A torch on the floor might have fallen; four words on a wall did not
 * get there by themselves. This is where the player stops being able to talk
 * themselves out of it, which is exactly why it belongs at TRESPASSER and not
 * before — spend deniability late (README.md).
 *
 * Placement rules, all of which matter:
 *
 *   BEHIND YOU and out of sight, so the sign is discovered rather than
 *   witnessed appearing. Watching a sign pop into existence makes it a spawn;
 *   turning round and finding it makes it a message.
 *
 *   ON A WALL YOU WILL PASS. Preferring positions near the player means it is
 *   found in minutes rather than never — an unread sign is a wasted event.
 *
 *   NEVER MORE THAN ONE. A wall of them is funny. One in a dead end is not.
 *
 * The sign is a normal oak sign and can be broken and kept, which matters
 * twice over: it obeys the anti-frustration rule, and per README.md a
 * broken sign is one of the Effigy reagents. Tearing his message down is both
 * the biggest single source of wrath and the thing that eventually lets you
 * end him.
 */
public final class Signs {
	private Signs() {}

	/**
	 * Marks a sign as his.
	 *
	 * Persistent, so it survives a reload — a player who quits, comes back and
	 * then tears the sign down must still be committing defiance. Matching on
	 * the text instead would be fragile the moment a line contains the
	 * player's own name, and would also let a player fake defiance by writing
	 * his words themselves.
	 */
	public static final AttachmentType<Boolean> HIS =
		AttachmentRegistry.createPersistent(HerobrineMod.id("his_sign"), Codec.BOOL);

	/** Forces the attachment type to exist before any world loads. */
	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer breaker)) {
				return;
			}
			if (entity != null && Boolean.TRUE.equals(entity.getAttached(HIS))) {
				// Per README.md this is the single biggest riser: tearing down
				// his message is what summons the next thing. The player's own
				// reaction drives the escalation, which is a far better engine
				// than a timer.
				Heat.noticed(breaker, DEFIANCE_BROKEN);
				HerobrineMod.LOGGER.info("sign broken at [{}, {}, {}] — defiance",
					pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	private static final int DEFIANCE_BROKEN = 60;

	private static final int SEARCH_RADIUS = 10;
	private static final double MIN_DISTANCE = 3.0;

	/**
	 * Him, at the wall, for a second, with an arm moving.
	 *
	 * Particles and a sound rather than an entity. Spawning the real one would
	 * mean a second Herobrine in the world for a second, and "there is only
	 * ever one of him" is a rule the whole mod is built on — a player who
	 * caught two at once would have learned something true and ruinous.
	 *
	 * So it is the SHAPE of him: a column of soul flame at the wall, the sound
	 * of a block being placed, and then nothing there. Enough to say somebody
	 * did this, without ever answering who was standing in the room.
	 */
	private static void writing(ServerLevel level, Placement spot) {
		double x = spot.pos.getX() + 0.5 - spot.facing.getStepX() * 0.6;
		double y = spot.pos.getY();
		double z = spot.pos.getZ() + 0.5 - spot.facing.getStepZ() * 0.6;

		for (int beat = 0; beat < 4; beat++) {
			final double height = y + beat * 0.35;
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), beat * 4, () -> {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
					x, height, z, 6, 0.14, 0.2, 0.14, 0.0);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
					x, height, z, 3, 0.2, 0.2, 0.2, 0.005);
			});
		}
		Cadence.in(level.getServer(), 14, () -> level.playSound(null,
			spot.pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE,
			net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 0.8F));
	}

	public static boolean write(ServerLevel level, ServerPlayer player) {
		List<Placement> options = findWalls(level, player);
		if (options.isEmpty()) {
			return false;
		}

		Phase phase = Wrath.phase(level.getServer());
		Placement spot = options.get(level.getRandom().nextInt(options.size()));
		String[] lines = SignLines.pick(phase, player, level.getRandom());
		if (lines == null) {
			return false;   // everything written lately; say nothing
		}

		// HE PUTS IT UP, and it takes him about a second.
		//
		// The sign used to simply exist, which is the one thing everything else
		// in this mod refuses to do — he is never seen arriving, but he is not
		// meant to be invisible either, and a message that materialises on a
		// wall reads as a script running rather than as somebody having been in
		// the room.
		//
		// So: he is there, facing the wall, for four beats. On the last one the
		// sign is on it and he is gone. Fast enough that a player who was
		// looking elsewhere sees only the aftermath — which is still the
		// default outcome, because these are placed on walls behind them — and
		// slow enough that a player who happened to be watching will not be
		// sure what they saw.
		BlockState sign = Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(WallSignBlock.FACING, spot.facing);
		writing(level, spot);
		level.setBlockAndUpdate(spot.pos, sign);

		if (!(level.getBlockEntity(spot.pos) instanceof SignBlockEntity entity)) {
			return false;
		}
		ManifestationDirector.noteLocation(spot.pos);
		entity.setAttached(HIS, true);
		entity.updateText(text -> {
			net.minecraft.world.level.block.entity.SignText updated = text;
			for (int i = 0; i < lines.length && i < 4; i++) {
				updated = updated.setMessage(i, Component.literal(lines[i]));
			}
			return updated;
		}, true);

		return true;
	}

	private record Placement(BlockPos pos, Direction facing) {}

	/**
	 * Air blocks with a solid block behind them — somewhere a wall sign can
	 * legally hang — that are behind the player and unlit enough to be his.
	 */
	private static List<Placement> findWalls(ServerLevel level, ServerPlayer player) {
		List<Placement> found = new ArrayList<>();
		BlockPos origin = player.blockPosition();
		Vec3 look = player.getViewVector(1.0F).normalize();
		int r = SEARCH_RADIUS;

		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-r, -3, -r), origin.offset(r, 3, r))) {
			if (!level.getBlockState(pos).isAir()) {
				continue;
			}
			double distance = Math.sqrt(pos.distSqr(origin));
			if (distance < MIN_DISTANCE) {
				continue;   // not against your face
			}
			if (isInFrontOf(player, look, pos)) {
				continue;   // never watched being placed
			}

			for (Direction facing : Direction.Plane.HORIZONTAL) {
				// A wall sign hangs on the block opposite the way it faces.
				BlockPos support = pos.relative(facing.getOpposite());
				if (level.getBlockState(support).isSolid()) {
					found.add(new Placement(pos.immutable(), facing));
					break;
				}
			}
		}
		return found;
	}

	private static boolean isInFrontOf(ServerPlayer player, Vec3 look, BlockPos pos) {
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() + 0.5 - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) > 0.1;
	}
}
