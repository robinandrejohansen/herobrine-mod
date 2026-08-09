package com.bloomlet.herobrine.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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
 *
 * THERE IS ONE EXCEPTION TO THE SECOND RULE and it is at the bottom of this
 * file. About one chest in six holds something enchanted, and there is always
 * something wrong with it. That is not a loophole in the rule — it is the rule
 * stated by someone else. He is not being generous and he is not being cruel;
 * he is offering, and the offer is never quite enough, which is a far more
 * unpleasant thing to be handed than nothing at all.
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

		// And now and again, on top of all that, the other thing.
		if (!free.isEmpty() && chest.getLevel() != null && random.nextInt(CHANCE_IN) == 0) {
			int slot = free.remove(random.nextInt(free.size()));
			chest.setItem(slot, chance(chest.getLevel().registryAccess(), random));
		}
	}

	// ------------------------------------------------------------------
	// THE CHANCE
	// ------------------------------------------------------------------

	/**
	 * How often a chest holds one. One in six.
	 *
	 * Low on purpose. This is the only thing in the mod that hands a player
	 * power, and the whole effect depends on it being an event — four or five
	 * times across a playthrough, each one remembered and argued about. At one
	 * in two it is a loot table. At one in six it is something that happened.
	 */
	private static final int CHANCE_IN = 6;

	/**
	 * What sort of thing it is, which decides what can be put on it.
	 *
	 * Kept coarse deliberately. The point is never to be encyclopaedic about
	 * which enchantment goes on which item — it is to stop a bow turning up
	 * with Thorns on it, because the moment one of these reads as a bug rather
	 * than as a choice, the whole conceit goes with it.
	 */
	private enum Sort { BLADE, AXE, BOW, ARMOUR }

	private record Base(Item item, Sort sort) {}

	private record Charm(ResourceKey<Enchantment> what, int min, int max) {}

	/** Rubbish steel. Everything here breaks or bends. */
	private static final Base[] POOR = {
		new Base(Items.WOODEN_SWORD, Sort.BLADE),
		new Base(Items.STONE_SWORD, Sort.BLADE),
		new Base(Items.GOLDEN_SWORD, Sort.BLADE),
		new Base(Items.WOODEN_AXE, Sort.AXE),
		new Base(Items.STONE_AXE, Sort.AXE),
		new Base(Items.GOLDEN_AXE, Sort.AXE),
		new Base(Items.BOW, Sort.BOW),
		new Base(Items.LEATHER_HELMET, Sort.ARMOUR),
		new Base(Items.LEATHER_CHESTPLATE, Sort.ARMOUR),
		new Base(Items.GOLDEN_HELMET, Sort.ARMOUR),
		new Base(Items.GOLDEN_BOOTS, Sort.ARMOUR),
		new Base(Items.CHAINMAIL_HELMET, Sort.ARMOUR),
	};

	/** The real thing. Anything here is worth carrying on its own. */
	private static final Base[] GOOD = {
		new Base(Items.IRON_SWORD, Sort.BLADE),
		new Base(Items.DIAMOND_SWORD, Sort.BLADE),
		new Base(Items.IRON_AXE, Sort.AXE),
		new Base(Items.DIAMOND_AXE, Sort.AXE),
		new Base(Items.BOW, Sort.BOW),
		new Base(Items.CROSSBOW, Sort.BOW),
		new Base(Items.IRON_CHESTPLATE, Sort.ARMOUR),
		new Base(Items.IRON_BOOTS, Sort.ARMOUR),
		new Base(Items.DIAMOND_HELMET, Sort.ARMOUR),
		new Base(Items.DIAMOND_BOOTS, Sort.ARMOUR),
	};

	/**
	 * The names, and there is ONE pool for all four kinds.
	 *
	 * This is the most important line in the file and it is easy to get wrong.
	 * The obvious build gives each kind its own words — something ominous on
	 * the cursed ones, something kind on the generous ones — and that is a
	 * disaster, because players compare notes. Two sessions in, "keep it" means
	 * cursed, everybody knows it, and the item is now labelled.
	 *
	 * Sharing the pool means the name tells you only WHO left it and never
	 * WHAT it is. You have to put it on to find out, which is the entire
	 * transaction he is offering.
	 */
	private static final String[] NAMES = {
		"go on then", "take it", "you'll want this", "for the road",
		"better than nothing", "not enough", "if you like", "try",
	};

	/**
	 * Something he left you, and there is something wrong with it.
	 *
	 * This deliberately breaks the second rule at the top of this file — it is
	 * a prize, and it is meant to feel like one for a moment. The rule survives
	 * anyway, because none of these is actually a kit-out: every one of the
	 * four is real power with the legs cut off it, and a player who builds a
	 * plan around one is going to be standing somewhere dark holding a handle.
	 *
	 * FOUR KINDS, and they are four different jokes at your expense:
	 *
	 *   GENEROUS — a genuinely superb enchantment on rubbish steel. Sharpness V
	 *              on a wooden sword. It is not a trick and it is not a lie; it
	 *              will do exactly what it says for about ninety seconds. This
	 *              is the purest version of the offer.
	 *   HOLLOW   — beautiful steel, worthless enchantment. A diamond axe whose
	 *              only magic is Bane of Arthropods I. Nothing is wrong with
	 *              it. That is what is wrong with it.
	 *   CURSED   — the real thing, with a curse riding along. Binding on a
	 *              helmet is the cruel one: you find out by putting it on, and
	 *              then it is yours.
	 *   WORN     — the real thing, at one or two hits from dust. The only kind
	 *              a player can defuse with a glance at the durability bar, and
	 *              the only one that punishes not looking.
	 *
	 * What makes it HIM rather than a loot table is that all four are the same
	 * gesture. He is not arming you and he is not fooling you. He is giving you
	 * a chance, on his terms, and finding it funny.
	 */
	public static ItemStack chance(RegistryAccess access, RandomSource random) {
		Registry<Enchantment> book = access.lookupOrThrow(Registries.ENCHANTMENT);
		int kind = random.nextInt(4);

		Base[] from = kind == 0 ? POOR : GOOD;
		Base base = from[random.nextInt(from.length)];
		ItemStack stack = new ItemStack(base.item());

		Charm[] charms = kind == 1 ? hollow(base.sort()) : strong(base.sort());
		Charm charm = charms[random.nextInt(charms.length)];
		put(stack, book, charm, random);

		switch (kind) {
			case 0 -> {
				// Used, not conjured. He did not make this; he found it, the
				// same as you are about to.
				wear(stack, random, 0.2, 0.4);
			}
			case 1 -> {
				// Left immaculate on purpose. The disappointment lands harder
				// on something that has never been swung.
			}
			case 2 -> {
				// Binding only where it can actually shut on you.
				put(stack, book, base.sort() == Sort.ARMOUR && random.nextBoolean()
					? new Charm(Enchantments.BINDING_CURSE, 1, 1)
					: new Charm(Enchantments.VANISHING_CURSE, 1, 1), random);
				wear(stack, random, 0.0, 0.25);
			}
			default -> {
				// One or two hits left. Not zero: an item that shatters on the
				// first swing reads as broken code, and one that shatters on
				// the third reads as a warning nobody read.
				if (stack.isDamageableItem()) {
					stack.setDamageValue(stack.getMaxDamage() - (1 + random.nextInt(3)));
				}
			}
		}

		stack.set(DataComponents.CUSTOM_NAME,
			Component.literal(NAMES[random.nextInt(NAMES.length)]));
		return stack;
	}

	/** Worth having, per sort. */
	private static Charm[] strong(Sort sort) {
		return switch (sort) {
			case BLADE -> new Charm[] {
				new Charm(Enchantments.SHARPNESS, 4, 5),
				new Charm(Enchantments.FIRE_ASPECT, 2, 2),
				new Charm(Enchantments.LOOTING, 3, 3),
				new Charm(Enchantments.SMITE, 4, 5),
			};
			case AXE -> new Charm[] {
				new Charm(Enchantments.SHARPNESS, 4, 5),
				new Charm(Enchantments.EFFICIENCY, 4, 5),
				new Charm(Enchantments.UNBREAKING, 3, 3),
			};
			case BOW -> new Charm[] {
				new Charm(Enchantments.POWER, 4, 5),
				new Charm(Enchantments.FLAME, 1, 1),
				new Charm(Enchantments.INFINITY, 1, 1),
			};
			case ARMOUR -> new Charm[] {
				new Charm(Enchantments.PROTECTION, 3, 4),
				new Charm(Enchantments.THORNS, 2, 3),
				new Charm(Enchantments.UNBREAKING, 3, 3),
				new Charm(Enchantments.FEATHER_FALLING, 3, 4),
			};
		};
	}

	/**
	 * Technically enchanted. Practically not.
	 *
	 * Every one of these is a real enchantment doing a real thing that will
	 * almost never come up — Bane of Arthropods on a world where you fight
	 * spiders twice, Respiration on a player who does not swim. It has to be
	 * plausible rather than junk, because the feeling being aimed at is not
	 * "he tricked me", it is the smaller and better one of turning the thing
	 * over, reading it twice, and slowly realising.
	 */
	private static Charm[] hollow(Sort sort) {
		return switch (sort) {
			case BLADE -> new Charm[] {
				new Charm(Enchantments.BANE_OF_ARTHROPODS, 1, 1),
				new Charm(Enchantments.KNOCKBACK, 1, 1),
				new Charm(Enchantments.SWEEPING_EDGE, 1, 1),
			};
			case AXE -> new Charm[] {
				new Charm(Enchantments.BANE_OF_ARTHROPODS, 1, 1),
				new Charm(Enchantments.EFFICIENCY, 1, 1),
			};
			case BOW -> new Charm[] {
				new Charm(Enchantments.PUNCH, 1, 1),
			};
			case ARMOUR -> new Charm[] {
				new Charm(Enchantments.RESPIRATION, 1, 1),
				new Charm(Enchantments.AQUA_AFFINITY, 1, 1),
				new Charm(Enchantments.PROTECTION, 1, 1),
			};
		};
	}

	private static void put(ItemStack stack, Registry<Enchantment> book, Charm charm,
	                        RandomSource random) {
		int level = charm.min() + random.nextInt(charm.max() - charm.min() + 1);
		stack.enchant(book.getOrThrow(charm.what()), level);
	}

	private static void wear(ItemStack stack, RandomSource random, double least, double most) {
		if (!stack.isDamageableItem()) {
			return;
		}
		int max = stack.getMaxDamage();
		int low = (int) (max * least);
		int high = Math.max(low + 1, (int) (max * most));
		stack.setDamageValue(low + random.nextInt(high - low));
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
