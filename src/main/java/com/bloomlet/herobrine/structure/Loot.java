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
		/** A working barrel: what somebody kept to hand rather than what they owned. */
		TOWN_TOOLS,
		/** His city, on the far side of the way. The only pool that pays. */
		HIS_CITY,
		/** Up the tower, past the gap. Paid for in height rather than in distance. */
		TOWER,
		/**
		 * THE GAOL, AND IT HAS TO PAY, because the walk to it is the longest
		 * unrewarded stretch in the mod.
		 *
		 * Sixteen blocks of stair, thirty-four of hall, fourteen cells, and at the
		 * end of it one chest on LARDER — the same pool as a dead farmer's pantry.
		 * A player who goes all the way down and finds bread has been told, by the
		 * only language a chest speaks, that they should not have bothered.
		 *
		 * And unlike the farm, this is not an abandoned building the rule at the
		 * top of this file was written about. It is an INSTITUTION, and it was in
		 * operation: it had warders, it had a payroll, it had a stores cupboard.
		 * Iron, restraint, and record-keeping, which is what a gaol actually owns.
		 * The pickaxes and the coal are in here because the same men were sinking
		 * the shaft behind the warder's room.
		 */
		GAOL,
		/**
		 * HIS CASTLE, AND IT IS A SUPPLY DUMP RATHER THAN A HOARD.
		 *
		 * The blueprint has three chests in seventy-three thousand blocks and
		 * nothing filled them, so the last building in the mod — the one at the end
		 * of the longest walk in it — was completely empty.
		 *
		 * But what it wants is not more treasure. By the time anybody stands in
		 * here they have looted a farm, a town, a tower, a gaol and a church, and
		 * another chest of diamonds changes nothing. What they are short of is what
		 * a long fight actually consumes: FOOD, and BLOCKS TO STAND ON.
		 *
		 * So it is a garrison's stores. Cobblestone by the stack, because the
		 * honest answer to a man who flies is to build, and arriving at the boss
		 * with nothing to pillar with is the most common way this fight is lost.
		 * Food heavy enough to eat through three acts. And rarely — one entry in
		 * about forty — the enchanted apple, which is the only thing in this pool
		 * that changes the fight rather than supplying it, and the only place in
		 * the whole mod one appears.
		 */
		KEEP,
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
		// WIDENED, because twelve entries is not a house.
		//
		// The old list read as a food cupboard with two boots in it, and every
		// house in the town drew from it — so by the fourth one the player knew
		// exactly what was in the fifth. These are the things that make a home
		// specifically rather than a store: something to write with, something to
		// light, something to put a plant in, and a cake nobody ate.
		new Entry(Items.CANDLE, 1, 4, 6, false),
		new Entry(Items.PAPER, 1, 5, 5, false),
		new Entry(Items.BOOK, 1, 2, 5, false),
		new Entry(Items.FLOWER_POT, 1, 2, 5, false),
		new Entry(Items.BOWL, 1, 3, 5, false),
		new Entry(Items.GLASS_BOTTLE, 1, 2, 4, false),
		new Entry(Items.SWEET_BERRIES, 2, 7, 4, false),
		new Entry(Items.EGG, 1, 4, 4, false),
		new Entry(Items.SUGAR, 1, 4, 3, false),
		new Entry(Items.HONEY_BOTTLE, 1, 2, 3, false),
		new Entry(Items.INK_SAC, 1, 3, 3, false),
		new Entry(Items.WOODEN_HOE, 1, 1, 3, true),
		new Entry(Items.SHEARS, 1, 1, 3, true),
		new Entry(Items.BUCKET, 1, 1, 3, false),
		new Entry(Items.CAKE, 1, 1, 1, false),
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
	/**
	 * THE BARREL BY THE DOOR, AND IT IS TOOLS RATHER THAN TREASURE.
	 *
	 * Every pool in this file until now answered "what was this person worth" —
	 * bread, iron, a leather chestplate. This one answers "what were they in the
	 * middle of", which is a completely different and much better question for a
	 * town that has been walked out of. A barrel with a half-worn iron pickaxe, a
	 * bucket, a lead and forty torches in it is not loot. It is somebody's kit,
	 * put down.
	 *
	 * ALL OF IT WORN, or nearly. `worn` puts real damage on a tool, and that is
	 * what stops this being a supply drop: an iron axe at sixty per cent is a gift
	 * AND a reminder that the last person to hold it used it a lot and is not here.
	 * A pristine set would read as a chest the mod put down for you.
	 *
	 * AND NOTHING IN IT IS BETTER THAN IRON. The forge pool is where value lives.
	 * This is where usefulness lives, and the two want to stay separate — a barrel
	 * that might hold diamond turns the whole town into a slot machine and players
	 * stop reading the buildings and start opening containers.
	 */
	private static final Entry[] TOOLS_POOL = {
		new Entry(Items.TORCH, 6, 20, 10, false),
		new Entry(Items.STICK, 4, 14, 9, false),
		new Entry(Items.IRON_PICKAXE, 1, 1, 8, true),
		new Entry(Items.IRON_AXE, 1, 1, 8, true),
		new Entry(Items.IRON_SHOVEL, 1, 1, 8, true),
		new Entry(Items.COAL, 3, 10, 8, false),
		new Entry(Items.STONE_PICKAXE, 1, 1, 7, true),
		new Entry(Items.STONE_AXE, 1, 1, 7, true),
		new Entry(Items.SHEARS, 1, 1, 6, true),
		new Entry(Items.IRON_HOE, 1, 1, 6, true),
		new Entry(Items.BUCKET, 1, 1, 6, false),
		new Entry(Items.FLINT_AND_STEEL, 1, 1, 5, true),
		new Entry(Items.IRON_INGOT, 1, 4, 5, false),
		new Entry(Items.LADDER, 4, 12, 5, false),
		new Entry(Items.STRING, 2, 8, 5, false),
		new Entry(Items.FLINT, 2, 6, 5, false),
		new Entry(Items.LEAD, 1, 2, 4, false),
		new Entry(Items.BRUSH, 1, 1, 4, true),
		new Entry(Items.FISHING_ROD, 1, 1, 4, true),
		new Entry(Items.PAPER, 2, 7, 4, false),
		new Entry(Items.GLASS_BOTTLE, 1, 3, 4, false),
		new Entry(Items.BOWL, 1, 3, 3, false),
		new Entry(Items.CANDLE, 2, 6, 3, false),
		new Entry(Items.FLOWER_POT, 1, 2, 3, false),
		new Entry(Items.NAME_TAG, 1, 1, 2, false),
		new Entry(Items.SPYGLASS, 1, 1, 2, true),
		new Entry(Items.SHIELD, 1, 1, 2, true),
		new Entry(Items.LANTERN, 1, 2, 2, false),
	};

	/**
	 * WHAT IS IN A CUPBOARD IN HIS CITY, AND IT IS THE ONLY POOL THAT PAYS.
	 *
	 * Every other pool in this file is deliberately poor, and that is right for
	 * the overworld: those buildings are somebody's farmhouse and somebody's
	 * village, and putting diamonds in them would turn a story into a loot run.
	 *
	 * This is on the far side of the way, past a portal under a house, in a town
	 * with a castle over it, and the player got here on purpose. A poor chest at
	 * the end of that is not restraint, it is the mod failing to notice what it
	 * asked of somebody. THE JOURNEY HAS TO PAY OR NOBODY MAKES IT TWICE.
	 *
	 * THREE THINGS IN EVERY CHEST, roughly, and they are three different kinds of
	 * good so no single chest is a disappointment:
	 *
	 *   FOOD THAT IS ACTUALLY WORTH CARRYING. Golden carrots and cooked meat, not
	 *   bread and rotten flesh. A player who crossed over is a player who is going
	 *   to be down here a while and the food is what lets them stay.
	 *
	 *   TOOLS ONE RUNG ABOVE WHAT THEY BROUGHT. Diamond, sparingly, and worn —
	 *   somebody used these. Enchanted books, which are the single best thing to
	 *   find because they are worth something whatever the player is carrying.
	 *
	 *   AND MATERIAL. Emeralds, iron and gold blocks, obsidian, ender pearls.
	 *   Bulk rather than a trinket: this was a working city and its cupboards had
	 *   stock in them.
	 */
	/**
	 * WHAT IS UP THE TOWER, AND IT IS ABOUT HEIGHT.
	 *
	 * The city pool pays for distance. This one pays for the climb, and the two
	 * want to be different in KIND rather than in amount — a second pool of
	 * emeralds would just be the city again in a worse room.
	 *
	 * So everything in here is about being off the ground. Rockets and membrane
	 * because of what is in the chest at the top. Ender pearls, because the parkour
	 * has a gap in it and a player who works out they can throw one has beaten it
	 * honestly. Feathers and arrows and a bow. Levitation potions, which are nearly
	 * useless and are the single most thematically correct item in the game to find
	 * halfway up a broken tower.
	 *
	 * NOTHING HERE IS BETTER THAN THE WINGS. The chests are the road and the elytra
	 * is the destination; a chest that outshines it would make the summit an
	 * anticlimax, and the summit is the whole reason anybody is up here.
	 */
	private static final Entry[] TOWER_POOL = {
		new Entry(Items.FIREWORK_ROCKET, 4, 16, 10, false),
		new Entry(Items.PHANTOM_MEMBRANE, 1, 4, 8, false),
		new Entry(Items.ENDER_PEARL, 2, 5, 8, false),
		new Entry(Items.FEATHER, 4, 12, 7, false),
		new Entry(Items.ARROW, 8, 24, 7, false),
		new Entry(Items.PAPER, 3, 9, 6, false),
		new Entry(Items.GUNPOWDER, 3, 9, 6, false),
		new Entry(Items.BOW, 1, 1, 5, true),
		new Entry(Items.RABBIT_FOOT, 1, 2, 5, false),
		new Entry(Items.GOLDEN_CARROT, 2, 6, 5, false),
		new Entry(Items.EMERALD, 2, 7, 5, false),
		new Entry(Items.IRON_INGOT, 3, 8, 5, false),
		new Entry(Items.ENCHANTED_BOOK, 1, 1, 4, false),
		new Entry(Items.LEATHER, 2, 6, 4, false),
		new Entry(Items.STRING, 3, 9, 4, false),
		new Entry(Items.TORCH, 6, 18, 4, false),
		new Entry(Items.LANTERN, 1, 2, 3, false),
		new Entry(Items.EXPERIENCE_BOTTLE, 2, 6, 3, false),
		new Entry(Items.SCAFFOLDING, 6, 16, 3, false),
		new Entry(Items.GOLDEN_APPLE, 1, 1, 2, false),
	};

	/**
	 * A working gaol's stores: iron, restraint, paper, and the shaft out back.
	 *
	 * Chainmail rather than plate, and all of it worn. A warder is not a soldier
	 * — he is issued the cheap mail nobody at the front would take, and he wears
	 * it for years. The chains and the bars are stock rather than salvage: this
	 * was a building that ordered restraint hardware by the crate.
	 *
	 * The milk is the saddest entry in this file. It is in here because somebody
	 * down there thought what was happening to those fourteen people might be a
	 * thing you could cure, and milk is what you reach for when you believe that.
	 */
	private static final Entry[] GAOL_POOL = {
		new Entry(Items.IRON_INGOT, 3, 9, 10, false),
		new Entry(Items.IRON_CHAIN, 3, 10, 9, false),
		new Entry(Items.TORCH, 8, 24, 8, false),
		new Entry(Items.IRON_BARS, 4, 12, 8, false),
		new Entry(Items.BREAD, 3, 9, 7, false),
		new Entry(Items.CHAINMAIL_CHESTPLATE, 1, 1, 6, true),
		new Entry(Items.CHAINMAIL_HELMET, 1, 1, 6, true),
		new Entry(Items.SHIELD, 1, 1, 6, true),
		new Entry(Items.COAL, 4, 14, 6, false),
		new Entry(Items.IRON_SWORD, 1, 1, 5, true),
		new Entry(Items.IRON_PICKAXE, 1, 1, 5, true),
		new Entry(Items.PAPER, 4, 12, 5, false),
		new Entry(Items.BOOK, 1, 4, 5, false),
		new Entry(Items.CHAINMAIL_LEGGINGS, 1, 1, 4, true),
		new Entry(Items.IRON_NUGGET, 4, 12, 4, false),
		new Entry(Items.LANTERN, 1, 3, 4, false),
		new Entry(Items.TRIPWIRE_HOOK, 1, 3, 4, false),
		new Entry(Items.INK_SAC, 1, 3, 4, false),
		new Entry(Items.IRON_BOOTS, 1, 1, 3, true),
		new Entry(Items.MILK_BUCKET, 1, 1, 3, false),
		new Entry(Items.EMERALD, 2, 6, 3, false),
		new Entry(Items.ENCHANTED_BOOK, 1, 1, 3, false),
		new Entry(Items.GOLDEN_APPLE, 1, 1, 2, false),
	};

	private static final Entry[] CITY_POOL = {
		// The food, and it is the good food.
		new Entry(Items.GOLDEN_CARROT, 2, 8, 9, false),
		new Entry(Items.COOKED_BEEF, 3, 9, 9, false),
		new Entry(Items.COOKED_PORKCHOP, 3, 8, 8, false),
		new Entry(Items.BREAD, 4, 10, 7, false),
		new Entry(Items.HONEY_BOTTLE, 1, 3, 5, false),
		new Entry(Items.GOLDEN_APPLE, 1, 1, 2, false),
		// Tools a rung above what they walked in with.
		new Entry(Items.IRON_PICKAXE, 1, 1, 7, true),
		new Entry(Items.IRON_AXE, 1, 1, 6, true),
		new Entry(Items.DIAMOND_PICKAXE, 1, 1, 3, true),
		new Entry(Items.DIAMOND_AXE, 1, 1, 2, true),
		new Entry(Items.DIAMOND_SHOVEL, 1, 1, 2, true),
		new Entry(Items.CROSSBOW, 1, 1, 3, true),
		new Entry(Items.SHIELD, 1, 1, 4, true),
		new Entry(Items.ENCHANTED_BOOK, 1, 1, 6, false),
		// And stock. A working city kept material, not curios.
		new Entry(Items.EMERALD, 2, 9, 8, false),
		new Entry(Items.IRON_INGOT, 4, 12, 8, false),
		new Entry(Items.GOLD_INGOT, 2, 8, 7, false),
		new Entry(Items.LAPIS_LAZULI, 3, 10, 6, false),
		new Entry(Items.OBSIDIAN, 2, 6, 5, false),
		new Entry(Items.ENDER_PEARL, 1, 3, 5, false),
		new Entry(Items.DIAMOND, 1, 3, 4, false),
		new Entry(Items.IRON_BLOCK, 1, 2, 3, false),
		new Entry(Items.GOLD_BLOCK, 1, 1, 2, false),
		new Entry(Items.EXPERIENCE_BOTTLE, 2, 6, 4, false),
		new Entry(Items.GLOWSTONE_DUST, 3, 9, 4, false),
		new Entry(Items.BLAZE_ROD, 1, 3, 3, false),
		// The things that make it a home rather than a vault.
		new Entry(Items.TORCH, 8, 24, 8, false),
		new Entry(Items.LANTERN, 1, 3, 5, false),
		new Entry(Items.BOOK, 1, 4, 5, false),
		new Entry(Items.CANDLE, 2, 6, 4, false),
	};

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
			barrel.setItem(free.remove(random.nextInt(free.size())),
				roll(pool, random, barrel.getLevel() == null
					? null : barrel.getLevel().registryAccess()));
		}
		barrel.setChanged();
	}

	/**
	 * A garrison's stores: what gets used up, not what gets admired.
	 *
	 * Weighted so that roughly half of every chest is material and food, which is
	 * the point of it. The enchanted apple sits at weight 1 against a pool total
	 * near 150 — call it one slot in a hundred and fifty, and with four chests'
	 * worth of slots in a run, a player finds one some of the time and never
	 * counts on it. That is the correct frequency for the only one in the mod.
	 */
	private static final Entry[] KEEP_POOL = {
		// ---- SOMETHING TO STAND ON. The reason this pool exists.
		new Entry(Items.COBBLESTONE, 24, 64, 14, false),
		new Entry(Items.STONE_BRICKS, 12, 32, 7, false),
		new Entry(Items.OAK_PLANKS, 16, 48, 7, false),
		new Entry(Items.DIRT, 16, 48, 5, false),
		new Entry(Items.SCAFFOLDING, 8, 24, 4, false),
		// ---- SOMETHING TO EAT, and it is the good food.
		new Entry(Items.COOKED_BEEF, 6, 14, 12, false),
		new Entry(Items.BREAD, 8, 16, 10, false),
		new Entry(Items.GOLDEN_CARROT, 4, 12, 9, false),
		new Entry(Items.COOKED_PORKCHOP, 5, 12, 8, false),
		new Entry(Items.BAKED_POTATO, 6, 14, 6, false),
		new Entry(Items.HONEY_BOTTLE, 1, 4, 4, false),
		new Entry(Items.GOLDEN_APPLE, 1, 3, 4, false),
		// The only one in the mod.
		new Entry(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 1, false),
		// ---- SOMETHING TO FIGHT WITH, topping up rather than replacing.
		new Entry(Items.ARROW, 12, 32, 8, false),
		new Entry(Items.IRON_INGOT, 4, 12, 6, false),
		new Entry(Items.GOLD_INGOT, 3, 9, 4, false),
		new Entry(Items.EXPERIENCE_BOTTLE, 3, 8, 4, false),
		new Entry(Items.OBSIDIAN, 2, 8, 4, false),
		new Entry(Items.DIAMOND, 1, 3, 3, false),
		new Entry(Items.SHIELD, 1, 1, 3, true),
		new Entry(Items.ENCHANTED_BOOK, 1, 1, 3, false),
		// ---- AND LIGHT, because the place is his and it is dark.
		new Entry(Items.TORCH, 16, 32, 9, false),
		new Entry(Items.LANTERN, 1, 4, 4, false),
	};

	private static Entry[] poolFor(Tier tier) {
		return switch (tier) {
			case HOMESTEAD -> HOMESTEAD_POOL;
			case LARDER -> LARDER_POOL;
			case TOWN_FORGE -> FORGE_POOL;
			case TOWN_TRADE -> TRADE_POOL;
			case TOWN_HOME -> HOME_POOL;
			case TOWN_ARMS -> ARMS_POOL;
			case TOWN_TOOLS -> TOOLS_POOL;
			case HIS_CITY -> CITY_POOL;
			case TOWER -> TOWER_POOL;
			case GAOL -> GAOL_POOL;
			case KEEP -> KEEP_POOL;
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
		// Nullable on purpose: a chest that has not been put in a level yet cannot
		// look anything up, and the honest answer there is a plain book rather than
		// a crash. It does not happen from any current caller.
		RegistryAccess access = chest.getLevel() == null
			? null : chest.getLevel().registryAccess();

		List<Integer> free = new ArrayList<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			if (chest.getItem(slot).isEmpty()) {
				free.add(slot);
			}
		}
		if (free.isEmpty()) {
			return;
		}

		// A dead farm is not a going concern; everything else is. Used below for
		// the enchanted-work chance, which belongs where such work would BE.
		boolean town = tier != Tier.HOMESTEAD && tier != Tier.LARDER;
		// How much is in one chest.
		//
		// A trading town's chests are fuller than a dead farm's: four to eight
		// rather than two to five, which is the difference between rummaging and
		// finding something.
		//
		// AND THE CASTLE IS LEANER THAN EITHER, on purpose. There are eighteen
		// chests in there rather than one or two, so the run is what pays and the
		// individual chest should not — eight stacks times eighteen chests is a
		// shop, and it would undo the whole fight's supply pressure in one room.
		// Three to five means every chest is worth opening and none of them is the
		// end of the problem.
		int stacks = switch (tier) {
			case KEEP -> 3 + random.nextInt(3);
			case HOMESTEAD, LARDER -> 2 + random.nextInt(4);
			default -> 4 + random.nextInt(5);
		};
		for (int i = 0; i < stacks && !free.isEmpty(); i++) {
			int slot = free.remove(random.nextInt(free.size()));
			chest.setItem(slot, roll(pool, random, access));
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
	/**
	 * SOMEBODY TRIED TO LEAVE HERE AND THIS IS WHAT IS LEFT OF IT.
	 *
	 * At the top of the tower, past the gap, and it is the only item in the mod
	 * that is straightforwardly a gift. Everything else he leaves is bait, a joke,
	 * or a warning. This is not his — it belongs to whoever got up here before you
	 * and it is in the state it is in because they did not get any further.
	 *
	 * BROKEN, AND IT STAYS BROKEN. The first version had Mending on it, and Mending
	 * is the one enchantment that undoes the entire idea: a pair of wings you can
	 * repair with experience is a pair of wings, and within an hour of finding them
	 * the player simply owns flight. Everything the tower was for is spent.
	 *
	 * So the damage is the item rather than a condition on it. Four to twelve
	 * points is a few seconds in the air — enough to know exactly what you are
	 * holding, and to work out that you cannot keep it. Repair is phantom membrane
	 * at an anvil, which is finite, which means every glide is spent rather than
	 * borrowed. Unbreaking stretches that and does not change it.
	 *
	 * AND THERE IS A PRICE ON THE FRONT OF IT. Binding, always: put them on and
	 * they do not come off, so wearing the wings costs you the chestplate slot for
	 * good. That is a real decision rather than a drawback — flight or armour, in a
	 * world where the thing hunting you hits for eleven — and it is visible in the
	 * tooltip before anybody commits, because a trap you cannot see is a bug report
	 * and a trap you can read is a choice.
	 *
	 * Vanishing on top of it a third of the time, which is the one that hurts: the
	 * wings that got somebody this far do not survive you dying either.
	 */
	public static ItemStack brokenWings(RegistryAccess access, RandomSource random) {
		Registry<Enchantment> book = access.lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack wings = new ItemStack(Items.ELYTRA);
		put(wings, book, new Charm(Enchantments.UNBREAKING, 1, 2), random);
		put(wings, book, new Charm(Enchantments.BINDING_CURSE, 1, 1), random);
		if (random.nextInt(3) == 0) {
			put(wings, book, new Charm(Enchantments.VANISHING_CURSE, 1, 1), random);
		}
		wings.setDamageValue(wings.getMaxDamage() - (4 + random.nextInt(9)));
		wings.set(DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal("somebody got this far"));
		return wings;
	}

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

	/**
	 * WHAT GOES ON A BOOK, BECAUSE A BLANK ONE IS A BUG AND NOT AN ITEM.
	 *
	 * Items.ENCHANTED_BOOK is a plain item like any other, and putting it in a pool
	 * gets you exactly that: a book with the shimmer on it, no enchantment
	 * component, and no way for a player to tell it apart from a broken drop except
	 * by hovering it and finding nothing there. It shipped in two pools that way.
	 *
	 * Curated rather than pulled from the registry at random, and it matters:
	 * everything available includes Curse of Binding, Curse of Vanishing, and a
	 * long tail of things nobody wants. A found book should always be worth the
	 * find, so this is the short list of ones that are.
	 */
	private static final Charm[] BOOKS = {
		new Charm(Enchantments.MENDING, 1, 1),
		new Charm(Enchantments.UNBREAKING, 2, 3),
		new Charm(Enchantments.EFFICIENCY, 3, 5),
		new Charm(Enchantments.FORTUNE, 2, 3),
		new Charm(Enchantments.SILK_TOUCH, 1, 1),
		new Charm(Enchantments.SHARPNESS, 3, 5),
		new Charm(Enchantments.PROTECTION, 3, 4),
		new Charm(Enchantments.FEATHER_FALLING, 3, 4),
		new Charm(Enchantments.LOOTING, 2, 3),
		new Charm(Enchantments.POWER, 3, 5),
		new Charm(Enchantments.RESPIRATION, 2, 3),
		new Charm(Enchantments.DEPTH_STRIDER, 2, 3),
		new Charm(Enchantments.THORNS, 2, 3),
		new Charm(Enchantments.INFINITY, 1, 1),
	};

	/**
	 * WHAT IS LYING ABOUT IN OTHER PEOPLE'S CUPBOARDS.
	 *
	 * Twelve of them, one page each, and the rule that makes them work is that
	 * NONE OF THEM IS ABOUT HIM. They are about a hole, a smell, a debt, a dog, a
	 * shift somebody covered. Ordinary paper from an ordinary place, and the horror
	 * arrives sideways in the last line or does not arrive at all.
	 *
	 * A found note that says "BEWARE THE ENTITY" tells the reader the mod is trying
	 * to frighten them, and once they know that they are safe. A found note about a
	 * man who is annoyed his brother borrowed a shovel and did not bring it back
	 * does not tell them anything — and it is the fourth one of those, when they
	 * notice how many of these people are writing about somebody who went down and
	 * did not come up, that the mod stops needing to tell them anything at all.
	 *
	 * THE TITLE IS THE HOOK AND IT IS THE WHOLE BUDGET. It sits in a chest slot
	 * next to a stack of iron, and it gets about half a second to earn opening.
	 * Every one of these is a question the reader now wants answered.
	 */
	/**
	 * A NAMED DOCUMENT THAT CORROBORATES A NUMBERED BOOK.
	 *
	 * These were twelve anonymous notes in the domestic-observation style — a
	 * stranger noticing that his brother did not put his hands under the water,
	 * and nothing else. Individually well made and collectively nothing: not one
	 * name from the ten books appeared in any of them, so a player who had read
	 * Addexio's account of Ashfold found, in the next chest, an unsigned note by
	 * nobody about no one.
	 *
	 * That is the exact failure HouseBooks' own header describes and then only
	 * half fixed. Restraint nobody can assemble is not restraint; and a note that
	 * belongs to no story cannot be assembled at all, however well the sentence
	 * is turned.
	 *
	 * SO EVERY ONE OF THEM IS SIGNED NOW, by somebody the books name — Otto,
	 * Marek, Bren, Pip, Joren, Wendel, Steve — and every one of them CONFIRMS one
	 * beat of the numbered account from the other side. Addexio tells you Bren
	 * shot the tall one twice and it kept walking; here is Bren's own note, in his
	 * own words, three days earlier. The books stop being one man's word for it.
	 *
	 * The `book` field is the one it belongs to, and it is in the title so a
	 * player who is collecting them knows what they have got.
	 */
	private record Scrap(String title, String hand, String page) {}

	private static final Scrap[] SCRAPS = {
		// ---- 1-2. THE FARM
		new Scrap("Otto, on the shutters", "Otto",
			"Addexio came round with a bag of nails and would not say why.\n\n"
			+ "He did my windows and then he did old Bren's, and then he sat on my "
			+ "wall for a long time not talking.\n\n"
			+ "I asked him straight out what he had seen in his wheat. He said "
			+ "nothing, and then he said: keep the children in."),

		new Scrap("Marek, before the town", "Marek",
			"Four children and one horse. Addexio has taken the two youngest ahead "
			+ "to Ashfold and is coming back for the others.\n\n"
			+ "I am writing this while I wait because if I stop moving I will think "
			+ "about it.\n\n"
			+ "He says the walls there are good. I have seen the walls. They are "
			+ "good."),

		// ---- 3-4. THE TOWN
		new Scrap("Bren, on the thing in the street", "Bren",
			"I put two arrows in it from the smithy roof.\n\n"
			+ "The first one went in under the arm and the second one went in the "
			+ "neck, and I have shot deer for fifty years and I know what those two "
			+ "shots do.\n\n"
			+ "It turned its head and looked up at me. Then it went on walking.\n\n"
			+ "Do not fight it in the open. Get above it or get behind a door."),

		new Scrap("a warning, pinned to the well", "unsigned",
			"THE ONES IN THE STREET ARE NOT THE PEOPLE.\n\n"
			+ "Nine of them by my count. They have the names right.\n\n"
			+ "Ask them something only the person would know. Ask them what their "
			+ "husband is called. They smile and they do not answer.\n\n"
			+ "Do not go anywhere on your own. It only tries this when you are "
			+ "alone."),

		new Scrap("the tally at the square", "unsigned",
			"Kept because somebody has to keep it.\n\n"
			+ "Through the door, by name, in the order they went: eleven on the "
			+ "first night, nineteen on the second, eleven on the third.\n\n"
			+ "Forty-one. None of them have come back out of it.\n\n"
			+ "The air that comes off it is cold and it smells like a struck match."),

		// ---- 5. THE TOWER
		new Scrap("Pip, the night before", "Pip",
			"Nobody believes me so I am writing it down and then it is written "
			+ "down.\n\n"
			+ "There is a hole in the rock by the spring that was not there "
			+ "yesterday. There are steps in it and the steps are CUT. Somebody cut "
			+ "them square.\n\n"
			+ "There is a light at the bottom the colour of a bruise.\n\n"
			+ "And I could hear somebody counting. I am going down in the morning "
			+ "with Addexio and Otto and I would rather not."),

		new Scrap("two of us came back", "unsigned",
			"Four went down at first light. Addexio, Otto, Pip, and me.\n\n"
			+ "I am not going to write what is down there. I will write the useful "
			+ "part.\n\n"
			+ "The steps are ours. Men from our own town cut them, and they have "
			+ "been cutting them the whole nine years we spent on top of that tower "
			+ "watching the sky."),

		// ---- 6-7. THE GAOL
		new Scrap("Joren, on his own ledger", "Joren",
			"Two columns. In, and out.\n\n"
			+ "I have counted them twice and the second column is shorter, and I am "
			+ "the man who writes both of them.\n\n"
			+ "I asked the man with the book where the difference goes. He said the "
			+ "question was not useful.\n\n"
			+ "COUNT THEM IN. COUNT THEM OUT. I painted that on the wall myself and "
			+ "I meant it as a kindness."),

		new Scrap("scratched inside cell four", "unsigned",
			"Nine days with the lamp on.\n\n"
			+ "I have said my wife's name every hour so that I would still have it "
			+ "at the end, and I still have it.\n\n"
			+ "So write it down: the lamp does not work. Whatever you are looking "
			+ "for, it is not in here.\n\n"
			+ "I was never one of them."),

		new Scrap("Steve, one page of many", "Steve",
			"Subject three continues to answer correctly. This is not evidence of "
			+ "anything and I have told them so.\n\n"
			+ "The answers do not matter. Watch the hands.\n\n"
			+ "The ordinary ones have to be tested the same way or the numbers mean "
			+ "nothing, and if that reads badly then it reads badly.\n\n"
			+ "I need to know whether a man comes up out of that hole the same man. "
			+ "I have a reason for needing to know."),

		// ---- 8. THE CHURCH
		new Scrap("Wendel, the question", "Wendel",
			"They would not let me say it in the room so I am saying it here.\n\n"
			+ "Before you let that man near you, ask Steve what he did to Herobrine "
			+ "under the hill.\n\n"
			+ "Not what happened to him. What Steve DID.\n\n"
			+ "I am old and I am not brave and I am the only one of us who has "
			+ "asked it out loud."),

		// ---- 9. THE LAST HOUSE
		new Scrap("on the sealing of the stair", "unsigned",
			"Stone, then more stone, then earth, and warnings cut in three hands so "
			+ "that nobody takes it for a cellar.\n\n"
			+ "Sixty-one of us died in the hall above this. It took about as long as "
			+ "it takes to boil a pot.\n\n"
			+ "We could not kill him. This is the other thing.\n\n"
			+ "We are going up to tell the valley it is finished. It is not "
			+ "finished. People need to plant wheat."),
	};

	/**
	 * How much a written book leaf holds before it silently stops drawing.
	 *
	 * Third place in this repo to need this number, which is two too many — the
	 * journal counts characters like this and the testimony counts LINES because
	 * its pages are hand-wrapped. Worth collapsing into one utility the next time
	 * anybody is in all three files.
	 */

	/**
	 * THIRTY-TWO CHARACTERS, AND GOING OVER DISCONNECTS THE PLAYER.
	 *
	 * A written book's title is written to the wire by Utf8String with a hard cap
	 * of 32, and nothing checks it on the way in. A 33-character title compiles,
	 * builds, boots, generates, saves — and then the first person to OPEN the chest
	 * gets
	 *
	 *     EncoderException: String too big (was 33 characters, max 32)
	 *
	 * on container_set_content, which does not throw an error in the chest, it
	 * severs the connection. "Internal Exception ... Failed to encode packet" and
	 * they are on the title screen. The chest also fails to serialise on save, so
	 * the item quietly vanishes from the world as well.
	 *
	 * Two of the titles written this week were 33. Both crashed. It presented as
	 * "the map is not in the chest", which is exactly what it looks like from
	 * inside the game, and it cost an evening.
	 *
	 * So no title reaches a book without coming through here. Trimming at a word
	 * boundary rather than mid-word, because a title cut to "an inventory of the sec"
	 * is a bug report of its own — and it SHOUTS in the log, because a silently
	 * shortened title is a thing nobody notices until it is in a release.
	 */
	private static final int TITLE_FITS = 32;

	private static String title(String wanted) {
		if (wanted.length() <= TITLE_FITS) {
			return wanted;
		}
		int cut = wanted.lastIndexOf(' ', TITLE_FITS);
		String short_ = wanted.substring(0, cut > 12 ? cut : TITLE_FITS).trim();
		com.bloomlet.herobrine.HerobrineMod.LOGGER.warn(
			"book title was {} characters and the wire allows {} — \"{}\" became \"{}\"",
			wanted.length(), TITLE_FITS, wanted, short_);
		return short_;
	}

	/**
	 * One of them, written, across as many leaves as the writing needs.
	 *
	 * PAGINATED BY MEASUREMENT NOW, not by a character budget. LEAF was 250
	 * characters, which is thirteen rows of ordinary prose and fifteen of anything
	 * with narrow letters in it, and it could not see that every blank line
	 * between paragraphs costs a row. One of these twelve was losing its last two
	 * rows to that and there was no way to find it by reading the source, because
	 * a fifteenth row is not an error, it is simply not drawn.
	 *
	 * Pages.bind measures in pixels and rows off the font's own advance table, and
	 * splits where the writer already paused. See Pages.
	 *
	 * AND THE HAND IS ON THE COVER. The author field of a written book is where
	 * vanilla puts "by so-and-so", and these were all signed with an em dash —
	 * which is the one thing a note corroborating somebody's account must not be.
	 */
	private static ItemStack scrap(RandomSource random) {
		Scrap what = SCRAPS[random.nextInt(SCRAPS.length)];
		java.util.List<net.minecraft.server.network.Filterable<
			net.minecraft.network.chat.Component>> leaves = new ArrayList<>();
		for (String leaf : Pages.bind(what.page())) {
			leaves.add(net.minecraft.server.network.Filterable.passThrough(
				net.minecraft.network.chat.Component.literal(leaf)));
		}
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT,
			new net.minecraft.world.item.component.WrittenBookContent(
				net.minecraft.server.network.Filterable.passThrough(title(what.title())),
				what.hand(), 0, leaves, true));
		return book;
	}

	private static ItemStack roll(Entry[] pool, RandomSource random,
	                              @org.jspecify.annotations.Nullable RegistryAccess access) {
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
			// A PLAIN BOOK IS CRAFTING MATERIAL AND NOBODY READS CRAFTING MATERIAL.
			//
			// Items.BOOK was in three pools as flavour, and what it actually is is
			// three paper and a leather — the player sweeps it up without looking.
			// In a mod whose whole overworld is a document, a book you cannot open
			// is the single most wasted item on the table.
			//
			// So every one of them is written, and the title does the work. These
			// are found on the way to somewhere, in somebody else's cupboard, and
			// the reader has to be able to tell from the shelf that it is worth the
			// two seconds — which is what a title like "what the well is for" does
			// and what "Book" does not.
			if (stack.is(Items.BOOK)) {
				stack = scrap(random);
			}
			if (stack.is(Items.ENCHANTED_BOOK) && access != null) {
				// ItemStack.enchant routes a book to STORED_ENCHANTMENTS on its own,
				// so this is the same call every other enchant in the file makes.
				put(stack, access.lookupOrThrow(Registries.ENCHANTMENT),
					BOOKS[random.nextInt(BOOKS.length)], random);
			}
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
