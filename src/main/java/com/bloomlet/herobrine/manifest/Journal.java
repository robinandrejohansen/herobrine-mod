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
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.AABB;
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
 * Placed into a chest that was already there if one is near, and left on the
 * floor otherwise. A page inside a mineshaft chest reads as something that has
 * sat there for years; the same page appearing on the ground reads as
 * something that arrived just now, so the chest is preferred wherever the
 * world offers one.
 *
 * Two guarantees the player can rely on without ever being told:
 *
 *   NO PAGE IS EVER LOST. Floor pages do not despawn, and while one is still
 *   waiting no further page is issued. If one is somehow destroyed the same
 *   number is issued again rather than skipped, so the account can never end
 *   up with a hole nothing can fill.
 *
 *   NO PAGE EXISTS TWICE. Page seven is placed once, and until it is
 *   collected there is no page eight anywhere in the world.
 */
public final class Journal {
	private Journal() {}

	/** How far this player has read. Persistent, so it survives a rejoin. */
	private static final AttachmentType<Integer> FOUND =
		AttachmentRegistry.createPersistent(HerobrineMod.id("journal_pages"), Codec.INT);

	/**
	 * Where the last page was left, if it has not been collected.
	 *
	 * Without this, a page issued and never found is gone: the counter had
	 * already advanced, so the account would keep a permanent hole in it that
	 * nothing could fill. Now the position is remembered, and if the page is
	 * still lying there no new one is issued — and if it somehow vanished, the
	 * same number is issued again rather than skipped.
	 *
	 * The consequence the player feels: every page exists exactly once, and no
	 * page can be lost.
	 */
	private static final AttachmentType<long[]> OUTSTANDING =
		AttachmentRegistry.createPersistent(HerobrineMod.id("journal_outstanding"),
			Codec.LONG.listOf().xmap(
				list -> list.stream().mapToLong(Long::longValue).toArray(),
				array -> java.util.Arrays.stream(array).boxed().toList()));

	private static final int SEARCH_RADIUS = 12;

	/** Forces the attachment type to exist before any world loads. */
	public static void register() {
		HerobrineMod.LOGGER.debug("journal attachment registered");
	}

	public static int pagesFound(ServerPlayer player) {
		return player.getAttachedOrElse(FOUND, 0);
	}

	public static boolean leavePage(ServerLevel level, ServerPlayer player) {
		Phase phase = Wrath.phase(level.getServer());

		// A page is still lying somewhere unclaimed. Issuing another would
		// leave two of the same account in the world and let the player skip
		// one entirely.
		if (stillWaiting(level, player)) {
			return false;
		}

		int next = pagesFound(player) + 1;

		// Everything this phase allows has been read. He has nothing further
		// to give yet, and inventing filler would be worse than silence.
		if (next > JournalPages.maxPageFor(phase) || next > JournalPages.count()) {
			return false;
		}

		BlockPos spot = intoChest(level, player, next);
		if (spot == null) {
			spot = onFloor(level, player, next);
		}
		if (spot == null) {
			return false;
		}

		player.setAttached(FOUND, next);
		player.setAttached(OUTSTANDING,
			new long[] { spot.asLong(), next });
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

	/**
	 * Has the last page been collected?
	 *
	 * A page in a chest is checked by looking in the chest; one on the floor by
	 * looking for the item. Either way, if it is still there nothing new is
	 * issued.
	 */
	private static boolean stillWaiting(ServerLevel level, ServerPlayer player) {
		long[] outstanding = player.getAttached(OUTSTANDING);
		if (outstanding == null || outstanding.length != 2) {
			return false;
		}
		BlockPos pos = BlockPos.of(outstanding[0]);
		if (!level.isLoaded(pos)) {
			return true;   // out of range; assume it is still there
		}

		if (level.getBlockEntity(pos) instanceof Container container) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (isPage(container.getItem(slot))) {
					return true;
				}
			}
			return false;
		}

		AABB around = new AABB(pos).inflate(2.0);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around)) {
			if (isPage(item.getItem())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPage(ItemStack stack) {
		if (!stack.is(Items.WRITTEN_BOOK)) {
			return false;
		}
		WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		return content != null && content.title().raw().startsWith("torn page");
	}

	/**
	 * Into a chest that was already there, if one is near.
	 *
	 * Far better than the floor. A page inside a mineshaft chest reads as
	 * something that has been sitting there for years; the same page appearing
	 * on the ground reads as something that arrived just now. Only chests the
	 * mod did not create, and only ones with a free slot — never displacing
	 * anything the player owns.
	 */
	private static BlockPos intoChest(ServerLevel level, ServerPlayer player, int number) {
		BlockPos origin = player.blockPosition();
		int r = SEARCH_RADIUS;
		List<BlockPos> chests = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
			if (!(level.getBlockEntity(pos) instanceof Container container)) {
				continue;
			}
			if (Math.sqrt(pos.distSqr(origin)) < 3.0) {
				continue;
			}
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (container.getItem(slot).isEmpty()) {
					chests.add(pos.immutable());
					break;
				}
			}
		}
		if (chests.isEmpty()) {
			return null;
		}

		BlockPos chosen = chests.get(level.getRandom().nextInt(chests.size()));
		Container container = (Container)level.getBlockEntity(chosen);
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).isEmpty()) {
				container.setItem(slot, page(number));
				container.setChanged();
				return chosen;
			}
		}
		return null;
	}

	/** Somewhere on the floor near the player, out of their current view. */
	private static BlockPos onFloor(ServerLevel level, ServerPlayer player, int number) {
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
		BlockPos spot = candidates.get(level.getRandom().nextInt(candidates.size()));

		ItemEntity dropped = new ItemEntity(level,
			spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, page(number));
		dropped.setDeltaMovement(Vec3.ZERO);
		// It waits. A page that despawns after five minutes is a hole in the
		// account that nothing can fill, and the whole point is that the
		// player finds it in their own time.
		dropped.setUnlimitedLifetime();
		level.addFreshEntity(dropped);
		return spot;
	}
}
