package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * WHAT HE TOOK, AND THE THREE PLACES IT COMES BACK.
 *
 * Theft is the most dangerous mechanic in this mod and the one most likely to
 * make somebody quit, so it is built on a single rule that never bends:
 *
 *   NOTHING IS EVER DELETED.
 *
 * Everything he takes goes in here, persisted with the world, and every route
 * out of here puts it back somewhere a player can reach. A chest that is lighter
 * than it was is then not a loss, it is a LEAD — and that inversion is the whole
 * reason this is allowed to exist. Losing four diamonds is a reason to log off.
 * Finding out where they went is a reason to walk four hundred blocks, and the
 * second one is a session.
 *
 * THE THREE WAYS BACK, and they are deliberately three rather than one, because
 * each teaches a different thing about him:
 *
 *   IN THE ANIMALS — the one nobody sees coming. The cow that has been standing
 *     too still at the edge of the field has your diamonds inside it. Nothing
 *     about it looks different; you only ever find out by killing it, which
 *     means every possessed animal anybody has ever walked past becomes a
 *     question in hindsight. It also quietly rewires the possession system: the
 *     staring animals stop being atmosphere and start being worth investigating.
 *
 *   IN THE GRAVES — after a hunt, and only after a hunt. He puts a marker where
 *     he broke off, with a chest under it holding some of what he has taken.
 *     That is the closest he comes to a transaction: you survived, so here is
 *     something, and it is buried under a headstone with somebody's name on it.
 *
 *   IN THE LAST HOUSES — the deep version. Walking into the church at HUNTER and
 *     finding your OWN enchanted pickaxe in a chest is the single strongest
 *     thing this system can produce, because it retroactively explains every
 *     building the players have already looted. Those chests were never
 *     treasure. They were where he keeps things.
 */
public final class Hoard {
	private Hoard() {}

	/**
	 * Kept on the overworld and persisted, because a hoard that empties on
	 * restart is a hoard that ate somebody's diamonds.
	 */
	private static final AttachmentType<List<ItemStack>> TAKEN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("hoard"),
			ItemStack.CODEC.listOf());

	/**
	 * How much he will hold at once.
	 *
	 * A cap is needed or a long game accumulates a hundred stacks nobody will
	 * ever collect, and every route out starts handing back junk. When it is
	 * full he simply stops stealing — which is the correct failure, because the
	 * alternative is dropping the oldest stack, and dropping is deleting.
	 */
	private static final int CAPACITY = 48;

	public static int size(ServerLevel level) {
		List<ItemStack> held = level.getAttached(TAKEN);
		return held == null ? 0 : held.size();
	}

	private static void put(ServerLevel level, ItemStack stack) {
		List<ItemStack> held = new ArrayList<>(
			level.getAttached(TAKEN) == null ? List.of() : level.getAttached(TAKEN));
		held.add(stack);
		level.setAttached(TAKEN, List.copyOf(held));
	}

	/** Take one back out. Removed as it is handed over, so it cannot double. */
	public static @Nullable ItemStack draw(ServerLevel level, RandomSource random) {
		List<ItemStack> held = level.getAttached(TAKEN);
		if (held == null || held.isEmpty()) {
			return null;
		}
		List<ItemStack> rest = new ArrayList<>(held);
		ItemStack out = rest.remove(random.nextInt(rest.size()));
		level.setAttached(TAKEN, List.copyOf(rest));
		return out;
	}

	// ------------------------------------------------------------------
	// Taking
	// ------------------------------------------------------------------

	private static final double SEARCH = 24.0;

	/**
	 * HE HAS BEEN IN YOUR CHEST.
	 *
	 * One or two stacks, never the chest, and never while anybody is looking at
	 * it — the same rule every other manifestation follows, and it matters most
	 * here: a stack vanishing from an open container in front of somebody is a
	 * bug report with a screenshot attached.
	 *
	 * AND HE LEAVES ONE BEHIND. If he takes twelve diamonds, one diamond stays
	 * in the chest. That single item is doing more work than the theft is: an
	 * empty slot is ambiguous, and everybody's first thought is that they
	 * misremembered or a friend borrowed it. ONE diamond where there were twelve
	 * cannot be misremembered, cannot be a friend, and cannot be explained. It is
	 * the difference between a suspicion and a fact, and it costs one item.
	 */
	public static boolean steal(ServerLevel level, ServerPlayer player) {
		if (size(level) >= CAPACITY) {
			ManifestationDirector.refused("he is already carrying too much");
			return false;
		}
		RandomSource random = level.getRandom();
		Vec3 look = player.getViewVector(1.0F).normalize();
		List<BlockPos> chests = new ArrayList<>();

		AABB around = player.getBoundingBox().inflate(SEARCH);
		for (BlockPos pos : BlockPos.betweenClosed(
				BlockPos.containing(around.minX, around.minY, around.minZ),
				BlockPos.containing(around.maxX, around.maxY, around.maxZ))) {
			if (!level.getBlockState(pos).is(Blocks.CHEST)) {
				continue;
			}
			// Behind them, and not the one they are standing at.
			Vec3 toChest = Vec3.atCenterOf(pos).subtract(player.position()).normalize();
			if (look.dot(toChest) > 0.2 || pos.distToCenterSqr(player.position()) < 36.0) {
				continue;
			}
			chests.add(pos.immutable());
		}
		if (chests.isEmpty()) {
			ManifestationDirector.refused("nothing of yours to go through");
			return false;
		}

		BlockPos at = chests.get(random.nextInt(chests.size()));
		if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
			return false;
		}

		List<Integer> full = new ArrayList<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			if (!chest.getItem(slot).isEmpty()) {
				full.add(slot);
			}
		}
		if (full.isEmpty()) {
			ManifestationDirector.refused("the chest was already empty");
			return false;
		}

		int wanted = Math.min(1 + random.nextInt(2), full.size());
		int took = 0;
		for (int i = 0; i < wanted; i++) {
			int slot = full.remove(random.nextInt(full.size()));
			ItemStack stack = chest.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack kept = stack.copyWithCount(1);
			ItemStack gone = stack.copy();
			gone.shrink(1);
			if (gone.isEmpty()) {
				// A single item in the slot: leaving one behind would mean taking
				// nothing, so this one is left alone entirely.
				continue;
			}
			chest.setItem(slot, kept);
			put(level, gone);
			took++;
		}
		if (took == 0) {
			ManifestationDirector.refused("nothing worth taking");
			return false;
		}
		chest.setChanged();
		HerobrineMod.LOGGER.info("he went through a chest at [{}, {}, {}] — {} taken, {} held",
			at.getX(), at.getY(), at.getZ(), took, size(level));
		return true;
	}

	// ------------------------------------------------------------------
	// Giving back
	// ------------------------------------------------------------------

	/**
	 * INSIDE THE ANIMAL, and there is no way to tell from outside.
	 *
	 * Held in the hands rather than tracked separately, because vanilla already
	 * drops a mob's equipment on death and setGuaranteedDrop makes it certain —
	 * no death hook, no attachment, nothing to leak. Quadrupeds do not render a
	 * held item, so it stays invisible until the thing is killed, which is
	 * exactly right: this is not a clue, it is a discovery.
	 */
	public static void seed(ServerLevel level, Mob mob) {
		RandomSource random = level.getRandom();
		if (size(level) == 0 || random.nextInt(3) != 0) {
			return;
		}
		for (EquipmentSlot slot : new EquipmentSlot[] {
				EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }) {
			ItemStack stack = draw(level, random);
			if (stack == null) {
				return;
			}
			mob.setItemSlot(slot, stack);
			mob.setGuaranteedDrop(slot);
			if (random.nextBoolean()) {
				return;   // often just the one
			}
		}
	}

	/**
	 * A MARKER WHERE HE BROKE OFF, WITH A CHEST UNDER IT.
	 *
	 * Only ever after a hunt, which is what makes it read as an exchange rather
	 * than as loot. The player has just survived something; this is what is
	 * standing there when they come back to the spot.
	 *
	 * The headstone carries somebody else's name and the chest carries the
	 * player's own belongings, and putting those two things one block apart is
	 * the entire point of siting it here.
	 */
	public static void grave(ServerLevel level, BlockPos where, ServerPlayer player) {
		if (size(level) == 0) {
			return;
		}
		RandomSource random = level.getRandom();
		BlockPos ground = level.getHeightmapPos(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, where);
		BlockPos hole = ground.below();
		if (!level.getBlockState(ground).canBeReplaced()
			|| level.getBlockState(hole).isAir()) {
			return;
		}

		level.setBlock(hole, Blocks.CHEST.defaultBlockState(), 3);
		if (level.getBlockEntity(hole) instanceof ChestBlockEntity chest) {
			int given = 0;
			for (int slot = 0; slot < chest.getContainerSize() && given < 4; slot++) {
				ItemStack stack = draw(level, random);
				if (stack == null) {
					break;
				}
				chest.setItem(slot, stack);
				given++;
			}
			chest.setChanged();
		}
		level.setBlock(ground, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
		BlockPos sign = ground.above();
		if (level.getBlockState(sign).canBeReplaced()) {
			level.setBlock(sign, Blocks.OAK_SIGN.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 3);
			if (level.getBlockEntity(sign)
					instanceof net.minecraft.world.level.block.entity.SignBlockEntity plate) {
				String[] lines = SignLines.grave(
					com.bloomlet.herobrine.wrath.Wrath.phase(level.getServer()),
					player, null, random);
				net.minecraft.world.level.block.entity.SignText text =
					new net.minecraft.world.level.block.entity.SignText();
				for (int row = 0; row < 4; row++) {
					text = text.setMessage(row, net.minecraft.network.chat.Component.literal(
						row < lines.length ? lines[row] : ""));
				}
				plate.setText(text, true);
				plate.setWaxed(true);
			}
		}
		HerobrineMod.LOGGER.info("a grave at [{}, {}, {}] with some of it back",
			ground.getX(), ground.getY(), ground.getZ());
	}
}
