package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;

/**
 * Leaving a page of the account where the player will find it.
 *
 * Implemented as a vanilla written book rather than a custom item. That is not
 * laziness — it means no model, no texture and no registration, and the player
 * gets Minecraft's own reading UI, which they already know how to use and
 * which already handles page turning and wrapping. A custom item would be more
 * work for a worse result.
 *
 * Pages arrive IN ORDER but gated by phase, so the arc always makes sense
 * while still leaving gaps: a player at TRESPASSER can reach page six and no
 * further, no matter how many they find. What they cannot yet reach, they
 * invent — and per LORE.md their invention is better than ours.
 *
 * Dropped as an item on the floor, never placed in a chest. A page on the
 * ground reads as something a person left behind; the same page inside your
 * own chest reads as a mod granting you a reward.
 */
public final class Journal {
	private Journal() {}

	/** How far this player has read. Persistent, so it survives a rejoin. */
	private static final AttachmentType<Integer> FOUND =
		AttachmentRegistry.createPersistent(HerobrineMod.id("journal_pages"), Codec.INT);

	private static final int SEARCH_RADIUS = 8;

	/** Forces the attachment type to exist before any world loads. */
	public static void register() {
		HerobrineMod.LOGGER.debug("journal attachment registered");
	}

	public static int pagesFound(ServerPlayer player) {
		return player.getAttachedOrElse(FOUND, 0);
	}

	public static boolean leavePage(ServerLevel level, ServerPlayer player) {
		Phase phase = Wrath.phase(level.getServer());
		int next = pagesFound(player) + 1;

		// Everything this phase allows has been read. He has nothing further
		// to give yet, and inventing filler would be worse than silence.
		if (next > JournalPages.maxPageFor(phase) || next > JournalPages.count()) {
			return false;
		}

		BlockPos spot = findFloor(level, player);
		if (spot == null) {
			return false;
		}

		ItemEntity dropped = new ItemEntity(level,
			spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5,
			page(next));
		dropped.setDeltaMovement(Vec3.ZERO);
		// No pickup delay games; it should behave like any other dropped item.
		level.addFreshEntity(dropped);

		player.setAttached(FOUND, next);
		HerobrineMod.LOGGER.info("journal page {} left at [{}, {}, {}] for {}",
			next, spot.getX(), spot.getY(), spot.getZ(), player.getName().getString());
		return true;
	}

	/**
	 * A written book carrying one page.
	 *
	 * The author is a single em dash. The name was scratched out so long ago
	 * that the scratching is the only part still legible (LORE.md), and an
	 * empty author field would just look like a bug.
	 */
	private static ItemStack page(int number) {
		List<Filterable<Component>> pages = new ArrayList<>();
		pages.add(Filterable.passThrough(Component.literal(JournalPages.text(number))));

		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("torn page " + number),
			"—",
			0,
			pages,
			true
		));
		return book;
	}

	/** Somewhere on the floor near the player, out of their current view. */
	private static BlockPos findFloor(ServerLevel level, ServerPlayer player) {
		List<BlockPos> candidates = new ArrayList<>();
		BlockPos origin = player.blockPosition();
		Vec3 look = player.getViewVector(1.0F).normalize();
		int r = SEARCH_RADIUS;

		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-r, -3, -r), origin.offset(r, 3, r))) {
			if (!level.getBlockState(pos).isAir()
				|| !level.getBlockState(pos.below()).isSolid()) {
				continue;
			}
			double distance = Math.sqrt(pos.distSqr(origin));
			if (distance < 3.0) {
				continue;
			}
			Vec3 toPos = new Vec3(
				pos.getX() + 0.5 - player.getX(),
				pos.getY() + 0.5 - player.getEyeY(),
				pos.getZ() + 0.5 - player.getZ()
			).normalize();
			if (look.dot(toPos) > 0.1) {
				continue;   // never watched appearing
			}
			candidates.add(pos.immutable());
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(level.getRandom().nextInt(candidates.size()));
	}
}
