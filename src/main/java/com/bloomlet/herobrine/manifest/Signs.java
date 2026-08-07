package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.WrathTriggers;

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
 * before — spend deniability late (DESIGN.md §2).
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
 * twice over: it obeys the anti-frustration rule, and per DESIGN.md §7 a
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
				// Per LORE.md this is the single biggest riser: tearing down
				// his message is what summons the next thing. The player's own
				// reaction drives the escalation, which is a far better engine
				// than a timer.
				WrathTriggers.defiance(breaker, DEFIANCE_BROKEN);
				HerobrineMod.LOGGER.info("sign broken at [{}, {}, {}] — defiance",
					pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	private static final int DEFIANCE_BROKEN = 60;

	private static final int SEARCH_RADIUS = 10;
	private static final double MIN_DISTANCE = 3.0;

	public static boolean write(ServerLevel level, ServerPlayer player) {
		List<Placement> options = findWalls(level, player);
		if (options.isEmpty()) {
			return false;
		}

		Phase phase = Wrath.phase(level.getServer());
		Placement spot = options.get(level.getRandom().nextInt(options.size()));
		String[] lines = SignLines.pick(phase, player, spot.pos, level.getRandom());
		if (lines == null) {
			return false;   // everything written lately; say nothing
		}

		BlockState sign = Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(WallSignBlock.FACING, spot.facing);
		level.setBlockAndUpdate(spot.pos, sign);

		if (!(level.getBlockEntity(spot.pos) instanceof SignBlockEntity entity)) {
			return false;
		}
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
