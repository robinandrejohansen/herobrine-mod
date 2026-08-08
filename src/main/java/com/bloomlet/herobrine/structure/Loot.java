package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * What is left in the cupboards.
 *
 * Two rules, and the second one is the hard one.
 *
 * FIRST: the books are never part of this. They are placed directly, before
 * anything here runs, into a slot nothing else can take. A player who walks a
 * thousand blocks and finds the sealed room empty because a dice roll went the
 * wrong way has lost the entire point of the building, and no amount of
 * variety is worth that risk. Loot is decoration; the books are the content.
 *
 * SECOND: it must feel like a help and never like a prize. The failure mode is
 * not "boring", it is "oh, I am now kitted out" — the moment a player finds
 * diamonds in here the house stops being somewhere people lived and becomes a
 * loot chest with a story attached, and every careful thing about it is spent.
 * So the ceiling is deliberately low: bread, wheat, wool, string, coal, a
 * couple of iron at the very most, and tools that are already half worn out.
 * Nothing here beats what a player has after one afternoon of their own, which
 * is the correct level for a farmhouse that was never wealthy.
 *
 * The variety is in WHICH mundane things, not in how good they are. Two
 * players comparing notes should find different chests and the same story.
 */
public final class Loot {
	private Loot() {}

	/**
	 * How well off the place was.
	 *
	 * HOMESTEAD is a working farm's cupboard. LARDER is a food store that has
	 * been standing a while, and it is the one that carries a room — a chest of
	 * bread and carrots says somebody shops here, and the same chest with
	 * rotten flesh and a poisonous potato in it says somebody shopped here and
	 * then stopped coming.
	 *
	 * An enum rather than a hardcoded list because the later houses are meant
	 * to be poorer and stranger as he stops being a person who owns things, and
	 * that progression wants somewhere to live from the start.
	 */
	public enum Tier { HOMESTEAD, LARDER }

	private record Entry(Item item, int min, int max, int weight, boolean worn) {}

	/**
	 * A working farm's cupboard, and nothing better.
	 *
	 * Weights do the balancing rather than a rarity roll: iron and shears are
	 * in the pool at a fraction of the weight of wheat, so they turn up
	 * occasionally and never in quantity. Rotten flesh is in here on purpose —
	 * it is the only entry that is not useful, and a chest that is not worth
	 * opening is what makes the ones that are feel found rather than awarded.
	 */
	private static final Entry[] HOMESTEAD_POOL = {
		new Entry(Items.WHEAT, 4, 14, 10, false),
		new Entry(Items.WHEAT_SEEDS, 3, 9, 9, false),
		new Entry(Items.BREAD, 1, 3, 8, false),
		new Entry(Items.STICK, 4, 12, 8, false),
		new Entry(Items.STRING, 2, 6, 7, false),
		new Entry(Items.COAL, 2, 6, 7, false),
		new Entry(Items.BONE_MEAL, 2, 6, 6, false),
		new Entry(Items.TORCH, 3, 9, 6, false),
		new Entry(Items.FEATHER, 2, 5, 6, false),
		new Entry(Items.LEATHER, 1, 3, 5, false),
		new Entry(Items.WOOL.pick(DyeColor.WHITE), 1, 4, 5, false),
		new Entry(Items.WOOL.pick(DyeColor.BROWN), 1, 2, 4, false),
		new Entry(Items.CLAY_BALL, 2, 5, 4, false),
		new Entry(Items.FLINT, 1, 3, 4, false),
		new Entry(Items.EGG, 1, 3, 4, false),
		new Entry(Items.BOWL, 1, 2, 4, false),
		new Entry(Items.APPLE, 1, 2, 3, false),
		new Entry(Items.ROTTEN_FLESH, 1, 3, 3, false),
		new Entry(Items.WOODEN_HOE, 1, 1, 3, true),
		new Entry(Items.STONE_AXE, 1, 1, 2, true),
		new Entry(Items.STONE_SHOVEL, 1, 1, 2, true),
		new Entry(Items.SHEARS, 1, 1, 2, true),
		new Entry(Items.IRON_INGOT, 1, 2, 2, false),
		new Entry(Items.BUCKET, 1, 1, 1, false),
	};

	/**
	 * A food store, and most of it has gone over.
	 *
	 * The single cheapest way to make a house feel lived in and then left. A
	 * chest of bread and carrots says somebody shops here; a chest of bread,
	 * carrots, rotten flesh and a poisonous potato says somebody shopped here
	 * and then stopped coming, and the difference is four item types.
	 *
	 * Deliberately still worth opening. Some of it is edible and some of it is
	 * not, which is a far better feeling than either a reward or a joke —
	 * the player rummages, takes the bread, leaves the rest, and has spent ten
	 * seconds thinking about the people who filled it.
	 *
	 * Nothing here is dangerous by accident. The poisonous potato is the only
	 * thing that could hurt anybody and it announces itself in the name.
	 */
	private static final Entry[] LARDER_POOL = {
		new Entry(Items.ROTTEN_FLESH, 2, 6, 10, false),
		new Entry(Items.BREAD, 1, 4, 9, false),
		new Entry(Items.POTATO, 2, 6, 8, false),
		new Entry(Items.POISONOUS_POTATO, 1, 2, 7, false),
		new Entry(Items.CARROT, 2, 5, 7, false),
		new Entry(Items.BEETROOT, 1, 4, 6, false),
		new Entry(Items.WHEAT, 3, 8, 6, false),
		new Entry(Items.DRIED_KELP, 1, 4, 4, false),
		new Entry(Items.BOWL, 1, 3, 4, false),
		new Entry(Items.BEETROOT_SOUP, 1, 1, 3, false),
		new Entry(Items.MUSHROOM_STEW, 1, 1, 3, false),
		new Entry(Items.BONE, 1, 3, 3, false),
		new Entry(Items.EGG, 1, 3, 3, false),
		new Entry(Items.APPLE, 1, 2, 2, false),
		new Entry(Items.MILK_BUCKET, 1, 1, 1, false),
	};

	/**
	 * Fill the slots the books did not take.
	 *
	 * Never touches an occupied slot, so this can be called on any chest
	 * whether or not something important is already in it, and the guarantee
	 * holds without the caller having to remember it.
	 */
	public static void scatter(ChestBlockEntity chest, RandomSource random, Tier tier) {
		Entry[] pool = switch (tier) {
			case HOMESTEAD -> HOMESTEAD_POOL;
			case LARDER -> LARDER_POOL;
		};

		List<Integer> free = new ArrayList<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			if (chest.getItem(slot).isEmpty()) {
				free.add(slot);
			}
		}
		if (free.isEmpty()) {
			return;
		}

		int stacks = 2 + random.nextInt(4);
		for (int i = 0; i < stacks && !free.isEmpty(); i++) {
			int slot = free.remove(random.nextInt(free.size()));
			chest.setItem(slot, roll(pool, random));
		}
	}

	private static ItemStack roll(Entry[] pool, RandomSource random) {
		int total = 0;
		for (Entry entry : pool) {
			total += entry.weight();
		}
		int pick = random.nextInt(total);
		for (Entry entry : pool) {
			pick -= entry.weight();
			if (pick >= 0) {
				continue;
			}
			int count = entry.min() + random.nextInt(entry.max() - entry.min() + 1);
			ItemStack stack = new ItemStack(entry.item(), count);
			if (entry.worn() && stack.isDamageableItem()) {
				// Somebody used this for years. A pristine tool in a house
				// nobody has lived in for a decade is a small lie, and small
				// lies are what stop a place feeling real.
				int max = stack.getMaxDamage();
				stack.setDamageValue(max / 2 + random.nextInt(Math.max(1, max / 3)));
			}
			return stack;
		}
		return ItemStack.EMPTY;
	}
}
