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
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

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
	public enum Tier {
		HOMESTEAD, LARDER,
		/**
		 * THE TOWN IS THE ONE PLACE THAT IS STILL TRADING, so it is the one place
		 * allowed to be worth robbing.
		 *
		 * This deliberately breaks the ceiling at the top of this file, and the
		 * distinction is honest rather than a loophole: "never a prize" is a rule
		 * about ABANDONED buildings. A farmhouse nobody has lived in for a decade
		 * having diamonds in the cupboard is what makes it stop being a house. A
		 * walled town with a working forge, a fisherman and a full congregation
		 * having iron, tackle and a few books in its chests is simply what a town
		 * is, and finding one poorer than the derelict farm two hundred blocks
		 * back would read as a bug.
		 *
		 * Split by trade, because a smithy full of bread is the same failure as a
		 * larder full of anvils.
		 */
		TOWN_FORGE, TOWN_TRADE, TOWN_HOME,
		/**
		 * WHAT THE WATCH KEEPS, which is not the same thing as what the smith
		 * makes.
		 *
		 * FORGE is a workshop's stock — raw iron, coal, and tools in the middle
		 * of being finished. This is a walled town's ARMOURY: the kit issued to
		 * whoever stands on the gate. Bows and arrows rather than ingots, shields
		 * and mail rather than pickaxes, and rations, because a watch that cannot
		 * eat is not a watch.
		 *
		 * It matters that the town has one at all. Everything else about the
		 * place says people are living ordinary lives forty blocks over a cult;
		 * a hall with arms in the back says somebody there has thought about
		 * what happens if that stops being true.
		 */
		TOWN_ARMS,
	}

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
	 * The forge. Iron, coal, and the tools somebody made out of them.
	 */
	private static final Entry[] FORGE_POOL = {
		new Entry(Items.IRON_INGOT, 3, 9, 10, false),
		new Entry(Items.COAL, 4, 12, 9, false),
		new Entry(Items.IRON_NUGGET, 4, 14, 7, false),
		new Entry(Items.IRON_PICKAXE, 1, 1, 6, true),
		new Entry(Items.IRON_AXE, 1, 1, 6, true),
		new Entry(Items.IRON_SHOVEL, 1, 1, 5, true),
		new Entry(Items.IRON_SWORD, 1, 1, 5, true),
		new Entry(Items.IRON_HELMET, 1, 1, 4, true),
		new Entry(Items.IRON_CHESTPLATE, 1, 1, 3, true),
		new Entry(Items.FLINT_AND_STEEL, 1, 1, 4, true),
		new Entry(Items.SHIELD, 1, 1, 3, true),
		new Entry(Items.ANVIL, 1, 1, 1, false),
	};

	/**
	 * The shops and the hall. What a town buys, sells and writes down.
	 */
	private static final Entry[] TRADE_POOL = {
		new Entry(Items.BOOK, 2, 6, 10, false),
		new Entry(Items.PAPER, 4, 12, 9, false),
		new Entry(Items.EMERALD, 1, 4, 7, false),
		new Entry(Items.FISHING_ROD, 1, 1, 7, true),
		new Entry(Items.COD, 2, 6, 7, false),
		new Entry(Items.SALMON, 1, 4, 6, false),
		new Entry(Items.STRING, 4, 10, 6, false),
		new Entry(Items.BUCKET, 1, 2, 5, false),
		new Entry(Items.LANTERN, 1, 3, 5, false),
		new Entry(Items.IRON_INGOT, 1, 4, 5, false),
		new Entry(Items.BOOKSHELF, 1, 2, 4, false),
		new Entry(Items.INK_SAC, 1, 3, 4, false),
		new Entry(Items.NAME_TAG, 1, 1, 2, false),
	};

	/**
	 * The lodges. People live here, and they are not poor.
	 */
	private static final Entry[] HOME_POOL = {
		new Entry(Items.BREAD, 2, 6, 10, false),
		new Entry(Items.COOKED_COD, 1, 4, 8, false),
		new Entry(Items.WHEAT, 4, 12, 8, false),
		new Entry(Items.CARROT, 3, 8, 7, false),
		new Entry(Items.TORCH, 4, 12, 7, false),
		new Entry(Items.LEATHER_BOOTS, 1, 1, 5, true),
		new Entry(Items.LEATHER_CHESTPLATE, 1, 1, 5, true),
		new Entry(Items.IRON_INGOT, 1, 3, 5, false),
		new Entry(Items.BOOK, 1, 3, 5, false),
		new Entry(Items.FISHING_ROD, 1, 1, 4, true),
		new Entry(Items.CLOCK, 1, 1, 2, false),
		new Entry(Items.COMPASS, 1, 1, 2, false),
	};

	/**
	 * The gate watch's kit. Worn, and none of it matched.
	 *
	 * Deliberately a rung below the forge on raw value and a rung above it on
	 * usefulness: no diamonds, no netherite, nothing a player could not have
	 * made themselves by this point in the game — but a bow, a shield and a stack
	 * of arrows is a genuinely good afternoon for somebody who arrived at the
	 * town with a stone sword.
	 *
	 * Chainmail is in here and nowhere else, because it is the one armour set a
	 * player cannot craft. It is not better than iron and it does not need to be:
	 * it is the piece that says this came from somewhere.
	 */
	private static final Entry[] ARMS_POOL = {
		new Entry(Items.ARROW, 8, 24, 10, false),
		new Entry(Items.BOW, 1, 1, 8, true),
		new Entry(Items.SHIELD, 1, 1, 7, true),
		new Entry(Items.IRON_SWORD, 1, 1, 6, true),
		new Entry(Items.BREAD, 3, 8, 6, false),
		new Entry(Items.CHAINMAIL_HELMET, 1, 1, 5, true),
		new Entry(Items.CHAINMAIL_CHESTPLATE, 1, 1, 4, true),
		new Entry(Items.CHAINMAIL_LEGGINGS, 1, 1, 4, true),
		new Entry(Items.CHAINMAIL_BOOTS, 1, 1, 4, true),
		new Entry(Items.IRON_HELMET, 1, 1, 4, true),
		new Entry(Items.TORCH, 4, 12, 5, false),
		new Entry(Items.LEATHER, 2, 6, 4, false),
		new Entry(Items.IRON_INGOT, 1, 3, 3, false),
		new Entry(Items.SPYGLASS, 1, 1, 1, false),
	};

	/**
	 * A BARREL IS A STORE CUPBOARD, NOT A STRONGBOX.
	 *
	 * Every barrel in the town was placed empty — the blueprints put the block
	 * down and nothing ever filled it — so a walled settlement with a forge and a
	 * working market had a dozen containers in it that opened onto nothing. Which
	 * is worse than having no barrels: an empty container reads as an unfinished
	 * mod, where no container at all reads as a room.
	 *
	 * Filled through here rather than through scatter, because the two are not
	 * the same object and should not pay out the same. A chest in this town is
	 * somebody's strongbox and is allowed to hold the enchanted thing; a barrel
	 * is where the flour and the spare arrows live. So: fewer stacks, and never
	 * the one-in-two enchanted roll. The chests stay the prize and the barrels
	 * stay worth opening, which is the split that keeps a town worth searching
	 * rather than worth clearing.
	 */
	public static void store(BaseContainerBlockEntity barrel, RandomSource random, Tier tier) {
		Entry[] pool = poolFor(tier);
		List<Integer> free = new ArrayList<>();
		for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
			if (barrel.getItem(slot).isEmpty()) {
				free.add(slot);
			}
		}
		// Two to four, and one barrel in five is simply empty. A settlement where
		// every single container pays out is a settlement being farmed; the empty
		// ones are what make opening the next one a question.
		if (free.isEmpty() || random.nextInt(5) == 0) {
			return;
		}
		int stacks = 2 + random.nextInt(3);
		for (int i = 0; i < stacks && !free.isEmpty(); i++) {
			barrel.setItem(free.remove(random.nextInt(free.size())), roll(pool, random));
		}
		barrel.setChanged();
	}

	private static Entry[] poolFor(Tier tier) {
		return switch (tier) {
			case HOMESTEAD -> HOMESTEAD_POOL;
			case LARDER -> LARDER_POOL;
			case TOWN_FORGE -> FORGE_POOL;
			case TOWN_TRADE -> TRADE_POOL;
			case TOWN_HOME -> HOME_POOL;
			case TOWN_ARMS -> ARMS_POOL;
		};
	}

	/**
	 * Fill the slots the books did not take.
	 *
	 * Never touches an occupied slot, so this can be called on any chest
	 * whether or not something important is already in it, and the guarantee
	 * holds without the caller having to remember it.
	 */
	public static void scatter(BaseContainerBlockEntity chest, RandomSource random, Tier tier) {
		Entry[] pool = poolFor(tier);

		List<Integer> free = new ArrayList<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			if (chest.getItem(slot).isEmpty()) {
				free.add(slot);
			}
		}
		if (free.isEmpty()) {
			return;
		}

		boolean town = tier != Tier.HOMESTEAD && tier != Tier.LARDER;
		// A trading town's chests are fuller than a dead farm's. Four to eight
		// rather than two to five, which is the difference between rummaging and
		// finding something.
		int stacks = town ? 4 + random.nextInt(5) : 2 + random.nextInt(4);
		for (int i = 0; i < stacks && !free.isEmpty(); i++) {
			int slot = free.remove(random.nextInt(free.size()));
			chest.setItem(slot, roll(pool, random));
		}

		// AND IN THE LAST HOUSES, SOME OF IT IS YOURS.
		//
		// The deepest thing this mod does with theft. Walking into the church and
		// finding your own enchanted pickaxe in a chest retroactively explains
		// every building anybody has already looted: those chests were never
		// treasure, they were where he keeps things.
		//
		// Gated on the phase rather than on which building this is, because the
		// late houses are the only ones that GENERATE from HUNTER onward — so the
		// phase is a faithful proxy and it needs no plumbing through six callers.
		// The honest cost of that: a chest placed in a cave chamber at HUNTER can
		// carry stolen goods too, which is a wider net than "the last houses".
		// That reads fine — he keeps things underground as well.
		if (!free.isEmpty() && chest.getLevel() instanceof net.minecraft.server.level.ServerLevel here
			&& com.bloomlet.herobrine.wrath.Wrath.phase(here.getServer())
				.atLeast(com.bloomlet.herobrine.wrath.Phase.HUNTER)) {
			net.minecraft.world.item.ItemStack stolen =
				com.bloomlet.herobrine.manifest.Hoard.draw(here, random);
			if (stolen != null) {
				chest.setItem(free.remove(random.nextInt(free.size())), stolen);
			}
		}

		// And now and again, on top of all that, the other thing.
		// One in six everywhere else; one in two in the town, because the forge
		// and the shops are where enchanted work would actually BE.
		if (!free.isEmpty() && chest.getLevel() != null
			&& random.nextInt(town ? 2 : CHANCE_IN) == 0) {
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
