package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * WHAT IS ALREADY LIVING THERE.
 *
 * His world is not empty and it is not fair. Everything that walks in it has
 * been there longer than the player has been alive, and it is all equipped —
 * not with a boss's numbers, with GEAR, which is a different kind of
 * frightening. A zombie with thirty extra health is a health bar. A zombie in
 * enchanted diamond, on a horse, is a person who has been getting ready.
 *
 * VANILLA MOBS, DRESSED, rather than four new entities. Everything the player
 * has learned in forty hours about how a skeleton moves and what a creeper is
 * about to do still applies — which is the whole point, because the ONE thing
 * that has changed about each of them is then unmissable. A new mob teaches
 * nothing; a familiar one holding something it should not have teaches
 * instantly.
 *
 * Dressed on load rather than on a tick sweep, so it happens once per creature
 * and the gear is saved with it. Nothing here re-checks or re-rolls: a skeleton
 * that has lost its helmet to a player has lost it.
 *
 * ENDERMEN ARE LEFT ALONE, deliberately and by name. Everything else in the
 * dimension has been changed, so the one thing that has not is the only thing
 * behaving normally — and after an hour of white eyes in the trees, a creature
 * that is exactly as you remember it is its own kind of unsettling.
 */
public final class HisHost {
	private HisHost() {}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(HisHost::onLoad);
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER
			.register(HisHost::afterBreak);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
			.register(HisHost::hive);
	}

	// ---- TRIPLE ------------------------------------------------------------
	/**
	 * EVERYTHING YOU BREAK THERE GIVES THREE.
	 *
	 * The one straightforwardly generous thing in the whole dimension, and it
	 * needs to be, because everything else about the place is a cost. It is dark,
	 * it never stops raining, every mob in it is in enchanted plate, the creepers
	 * are all charged and there is nothing to take home from any of them — the
	 * garrison drops nothing at all on purpose.
	 *
	 * So what the player gets for going is the GROUND. Three iron for one, three
	 * diamonds for one, three logs for one. It costs nothing narratively — this
	 * is his country and its stone was never anybody's — and it turns the last
	 * chapter from a corridor you walk down once into somewhere worth the trip
	 * back, which is what a dimension has to be to justify existing.
	 *
	 * Two EXTRA sets rather than a multiplier on the loot table, so it stacks
	 * honestly with Fortune and Silk Touch: whatever the player's own tools would
	 * have produced, they get three of. A silk-touched block yields three of the
	 * block, which is correct and slightly absurd and entirely in keeping.
	 */
	private static final int TIMES = 3;

	private static void afterBreak(net.minecraft.world.level.Level level,
	                               net.minecraft.world.entity.player.Player player,
	                               BlockPos pos,
	                               net.minecraft.world.level.block.state.BlockState state,
	                               net.minecraft.world.level.block.entity.
	                                   @org.jspecify.annotations.Nullable BlockEntity be) {
		if (!(level instanceof ServerLevel here) || !Config.get().enabled
			|| !Config.get().hisHost
			|| !here.dimension().equals(TheWayBlock.HIS_WORLD)) {
			return;
		}
		// Creative breaks nothing loose in the first place, so there is nothing
		// to triple and doing it anyway would hand an operator a pile of stone
		// every time they cleared a wall.
		if (player.getAbilities().instabuild) {
			return;
		}
		java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
			state, here, pos, be, player, player.getMainHandItem());
		if (drops.isEmpty()) {
			return;
		}
		for (int again = 1; again < TIMES; again++) {
			for (ItemStack stack : drops) {
				net.minecraft.world.level.block.Block.popResource(here, pos, stack.copy());
			}
		}
	}

	// ---- THE HIVE ----------------------------------------------------------
	/**
	 * ONE OF THEM SEES YOU AND THEY ALL COME.
	 *
	 * The single change that makes this dimension feel defended rather than
	 * populated. Vanilla hostiles are individuals: each one notices you inside
	 * its own follow range and the rest of the wood carries on regardless, so a
	 * player picks them off one at a time in a corridor of their own choosing.
	 * Here the first one to spot you tells everything within forty blocks, and
	 * what arrives is not a mob, it is the garrison.
	 *
	 * It also fixes the thing that was quietly wrong about the place: a castle
	 * with a standing army in enchanted plate, whose soldiers ignore an intruder
	 * walking past ten blocks away because he happened to be behind a tree.
	 *
	 * PROPAGATED FROM THE SPOTTER, NOT FROM THE PLAYER. Alerting everything near
	 * the PLAYER would be a smaller radius doing a bigger job, and it would drag
	 * mobs the player has never been near. Alerting everything near the one that
	 * saw them means the call travels outward from where the sighting happened,
	 * and a second sighting on the far side of the wood pulls a different group.
	 */
	private static final int HIVE_INTERVAL = 20;
	private static final double HIVE_REACH = 40.0;
	/** How far out a spotter is looked for at all. */
	private static final double HIVE_SEARCH = 64.0;

	private static int tickCounter;

	private static void hive(net.minecraft.server.MinecraftServer server) {
		if (++tickCounter % HIVE_INTERVAL != 0
			|| !Config.get().enabled || !Config.get().hisHost) {
			return;
		}
		ServerLevel his = server.getLevel(TheWayBlock.HIS_WORLD);
		if (his == null || his.players().isEmpty()) {
			return;
		}
		for (net.minecraft.server.level.ServerPlayer player : his.players()) {
			if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
				continue;
			}
			closing(his, player);
			theVisit(his, player);
			for (Mob spotter : his.getEntitiesOfClass(Mob.class,
					player.getBoundingBox().inflate(HIVE_SEARCH),
					mob -> mob.getTarget() == player)) {
				call(his, spotter, player);
				// One caller a tick is plenty. The next pass picks up whoever
				// this one just woke, so the alarm spreads outward through the
				// wood rather than resolving in a single frame — which is both
				// cheaper and much better to be on the receiving end of.
				break;
			}
		}
	}

	/** How long between visits, at the very least. Twelve minutes. */
	private static final int VISITS_EVERY = 12 * 60 * 20;
	/** And how far apart the group has to be for one to be possible. */
	private static final double SEPARATED = 80.0;
	private static long lastVisit = Long.MIN_VALUE;

	/**
	 * SOMEBODY COMES OVER, ONCE IN A WHILE, WHEN NOBODY CAN CHECK.
	 *
	 * Mimicry already knows how to put a figure wearing somebody's skin and name
	 * into the world and a seventh row into a six-player tab list, and MimicEntity
	 * now knows how to behave like a person for a minute and then stop. What was
	 * missing is the one place it belongs most: his world, where the group is
	 * spread across a forest in permanent rain and the only way to check who that
	 * is would be to walk to them.
	 *
	 * TWO PLAYERS MINIMUM, EIGHTY BLOCKS APART. Both halves are load-bearing.
	 * With one player online the figure has nobody to be — copying the person
	 * looking at it is a famous image that resolves in one second, because you know
	 * where you are. And within shouting distance the whole thing dies to a glance.
	 *
	 * Twelve minutes at the very least, and even then only one roll in four, so
	 * this stays something that happened once on a Tuesday rather than a mechanic.
	 * The scare is entirely in nobody expecting it, and a scheduled visitor is a
	 * feature.
	 */
	private static void theVisit(ServerLevel his,
	                             net.minecraft.server.level.ServerPlayer player) {
		if (his.players().size() < 2) {
			return;
		}
		long now = his.getGameTime();
		if (now - lastVisit < VISITS_EVERY) {
			return;
		}
		boolean alone = true;
		for (net.minecraft.server.level.ServerPlayer other : his.players()) {
			if (other != player && other.distanceTo(player) < SEPARATED) {
				alone = false;
				break;
			}
		}
		if (!alone || his.getRandom().nextInt(4) != 0) {
			return;
		}
		if (com.bloomlet.herobrine.manifest.Mimicry.appear(his, player)) {
			lastVisit = now;
			HerobrineMod.LOGGER.info("somebody walked over to {} in his world",
				player.getName().getString());
		}
	}

	/** How far out he starts pulling the storm in after him. */
	private static final double STORM_FEELS = 90.0;
	/** And how close it has to be before every second is carrying a strike. */
	private static final double STORM_ON_TOP = 18.0;
	/** How far from the player the bolts land. Never on them. */
	private static final double BOLT_NEAR = 6.0;
	private static final double BOLT_FAR = 22.0;

	/**
	 * THE STORM KNOWS HE IS COMING BEFORE YOU DO.
	 *
	 * His world rains permanently and that rain is scenery — constant, and anything
	 * constant stops being information within a minute. So the weather has never
	 * once told a player anything, in a place whose entire mood is weather.
	 *
	 * This makes the sky the tell. Ninety blocks out the first bolt lands somewhere
	 * off in the trees, maybe one every twenty seconds. At eighteen it is most
	 * seconds and it is landing close enough to light the ground you are standing
	 * on. Nothing has appeared, nothing has made a sound at you, and the horizon is
	 * coming apart.
	 *
	 * AND IT IS REAL, which is the whole reason it works. Every one of these goes
	 * through struck() like any other bolt over here, so what a player watches
	 * approach is not an effect — it is craters and fire arriving in a line, and
	 * the line has a direction, and the direction is him.
	 *
	 * They never land ON anybody. Six blocks is the closest, which is near enough
	 * to be frightening and far enough that the warning is not also the damage.
	 */
	private static void closing(ServerLevel his,
	                            net.minecraft.server.level.ServerPlayer player) {
		com.bloomlet.herobrine.entity.HerobrineEntity him =
			com.bloomlet.herobrine.entity.HerobrineEntity.oneIn(his);
		if (him == null) {
			return;
		}
		double away = him.distanceTo(player);
		if (away > STORM_FEELS) {
			return;
		}
		// Linear from nothing at ninety blocks to about one a second on top of you.
		double near = (STORM_FEELS - away) / (STORM_FEELS - STORM_ON_TOP);
		double chance = Math.min(1.0, Math.max(0.0, near)) * 0.9 + 0.05;
		RandomSource random = his.getRandom();
		if (random.nextDouble() > chance) {
			return;
		}

		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = BOLT_NEAR + random.nextDouble() * (BOLT_FAR - BOLT_NEAR);
		int x = (int) Math.round(player.getX() + Math.cos(angle) * range);
		int z = (int) Math.round(player.getZ() + Math.sin(angle) * range);
		int y = his.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
			x, z);

		net.minecraft.world.entity.LightningBolt bolt =
			EntityTypes.LIGHTNING_BOLT.create(his, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}
		bolt.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
		his.addFreshEntity(bolt);
	}

	private static void call(ServerLevel his, Mob spotter,
	                         net.minecraft.server.level.ServerPlayer player) {
		for (Mob other : his.getEntitiesOfClass(Mob.class,
				spotter.getBoundingBox().inflate(HIVE_REACH),
				mob -> mob != spotter && mob.getTarget() == null
					&& mob instanceof net.minecraft.world.entity.monster.Monster)) {
			other.setTarget(player);
		}
	}

	/**
	 * ONE OF HIS, OR NOT THERE AT ALL.
	 *
	 * Enemy rather than Monster, because Monster misses slimes — Slime extends Mob
	 * and implements Enemy directly, and a guard that lists the classes it knows
	 * about is exactly the shape of bug this file keeps finding. Enemy is the
	 * marker every hostile thing in the game carries.
	 *
	 * InfectedEntity is ours and it EXTENDS Zombie, so it comes through this net
	 * unless it is named. It is the only one that does: the Turned, the mimics and
	 * he himself are all PathfinderMob and were never Enemy to begin with.
	 */
	private static boolean notOneOfHis(Mob mob) {
		return mob instanceof net.minecraft.world.entity.monster.Enemy
			&& !(mob instanceof com.bloomlet.herobrine.entity.InfectedEntity);
	}

	private static void onLoad(Entity entity, ServerLevel level) {
		if (!Config.get().enabled) {
			return;
		}
		if (!level.dimension().equals(TheWayBlock.HIS_WORLD)) {
			return;
		}

		// EVERY BOLT OVER THERE LEAVES A HOLE AND A FIRE, NOT JUST HIS.
		//
		// The crater and the scorch were built for the ending — for him, throwing
		// lightning while he circles his own castle — and they are the best thing in
		// the fight. The weather in his world has been dropping perfectly ordinary
		// vanilla bolts the entire time, which set a tree alight if you were lucky
		// and otherwise did nothing at all.
		//
		// Wiring the same two effects onto the WEATHER is the cheapest way to make
		// the storm read as him. A player who has fought him at the keep already
		// knows what a strike of his looks like: a dished-out crater and fire round
		// the rim. Then it starts happening on the walk home, in weather nobody is
		// controlling, and the dimension stops having weather and starts having HIM.
		if (entity instanceof net.minecraft.world.entity.LightningBolt bolt) {
			struck(level, bolt.blockPosition());
			return;
		}

		if (!(entity instanceof Mob mob)) {
			return;
		}
		// AND THE SWEEP RUNS BEFORE THE ARMING, and outside the hisHost gate.
		//
		// The biome does the real work — his forest has no monster list, so none of
		// this is ever rolled. But a dimension that has ALREADY generated keeps the
		// biome its chunks were written with, and every one of those chunks goes on
		// spawning zombies forever. This catches those, and anything that arrives by
		// spawner, egg or command.
		//
		// Outside the hisHost gate on purpose: hisHost is "arm what is there", and
		// turning it off should leave the place undefended, not repopulate it with
		// vanilla mobs.
		if (Config.get().hisOwnOnly && notOneOfHis(mob)) {
			mob.discard();
			return;
		}
		if (!Config.get().hisHost) {
			return;
		}
		RandomSource random = level.getRandom();
		if (mob instanceof AbstractSkeleton bones) {
			arm(bones, random, level.registryAccess());
		} else if (mob instanceof Zombie walker) {
			arm(walker, random, level.registryAccess());
		} else if (mob instanceof Creeper creeper) {
			charge(creeper);
		}
	}

	// ---- THE SKELETONS -----------------------------------------------------
	/**
	 * Iron, enchanted, and the bow throws fire.
	 *
	 * The armour is the same set on every one of them on purpose. Skeletons are
	 * the rank and file of this place — they are ISSUED with something, and a
	 * uniform reads as an army where a scatter of mismatched pieces reads as
	 * scavengers. The zombies get the variety; these get the discipline.
	 *
	 * Iron rather than diamond, because they are not the threat. What makes a
	 * skeleton dangerous here is what comes off the bowstring, and putting them
	 * in diamond as well would make a rank-and-file mob harder to kill than the
	 * thing it is shooting at you.
	 */
	private static void arm(AbstractSkeleton bones, RandomSource random, RegistryAccess access) {
		wear(bones, random, access, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
			Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		// The bow is left in its hand and it still governs the AI — hold the
		// range, lead the shot, back away when crowded. What leaves it is not an
		// arrow; see HisSkeletonMixin.
		ItemStack bow = new ItemStack(Items.BOW);
		enchant(bow, random, access, 12);
		bones.setItemSlot(EquipmentSlot.MAINHAND, bow);
		bones.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		bones.reassessWeaponGoal();
		reach(bones);
	}

	/**
	 * THEY SHOOT FROM MUCH FURTHER, AND IT TAKES TWO CHANGES RATHER THAN ONE.
	 *
	 * FOLLOW_RANGE alone is the obvious lever and it does half the job: it is
	 * what decides how far away a skeleton NOTICES somebody. Raised on its own,
	 * all it produces is a skeleton that spots you at forty blocks and then jogs
	 * in to fifteen before it shoots, because the range it actually opens fire at
	 * belongs to the bow goal — a fixed 15.0F baked into AbstractSkeleton's own
	 * construction of RangedBowAttackGoal.
	 *
	 * So the goal is REPLACED. reassessWeaponGoal is called first, so vanilla
	 * builds its version and settles, and then that one is removed and ours put
	 * in at thirty-two blocks. Which is roughly where the fog begins to take
	 * things, and that is the intended feeling: the shot comes out of a part of
	 * the wood you cannot see into.
	 *
	 * Removed by CLASS rather than by holding a reference, because the field
	 * vanilla keeps it in is private and an accessor mixin for one goal swap is
	 * a permanent dependency on somebody else's internals.
	 */
	private static final double SEES = 48.0;
	private static final float SHOOTS_AT = 32.0F;
	/** Ticks between shots. Vanilla is 20 at hard difficulty, 40 otherwise. */
	private static final int CADENCE = 50;

	private static void reach(AbstractSkeleton bones) {
		if (bones.getAttribute(Attributes.FOLLOW_RANGE) != null) {
			bones.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(SEES);
		}
		bones.getGoalSelector().removeAllGoals(
			goal -> goal instanceof net.minecraft.world.entity.ai.goal.RangedBowAttackGoal);
		// SLOWER THAN VANILLA, deliberately, and it is the price of the range.
		// A fireball every second from thirty-two blocks, from something the
		// player cannot see, is not a fight — it is weather with a damage value.
		// Two and a half seconds gives them time to find the shooter, and the
		// travel time on a lobbed fireball gives them time to move.
		bones.getGoalSelector().addGoal(4,
			new net.minecraft.world.entity.ai.goal.RangedBowAttackGoal<>(
				bones, 1.0, CADENCE, SHOOTS_AT));
	}

	// ---- THE ZOMBIES -------------------------------------------------------
	/**
	 * Every one of them different, and some of them mounted.
	 *
	 * The opposite decision from the skeletons and it is the same decision: what
	 * a group of these should communicate is that they were PEOPLE, separately,
	 * who each got hold of what they could. So the armour is rolled per piece
	 * and per tier — one in netherite boots and nothing else, one in a full set
	 * of gold, one bare-headed in diamond — and the weapon is the only thing
	 * they all agree on.
	 *
	 * AND THE SPEED IS ROLLED TOO, which does more than any of the gear. A
	 * crowd that all moves at exactly one pace is a wave; a crowd where three
	 * are shambling and one is coming much faster than the rest is a crowd you
	 * have to keep looking at. The fast ones are the minority, always.
	 *
	 * One in six is on a horse. That is the silhouette this dimension is for —
	 * something in enchanted diamond, at a canter, in the rain, at night.
	 */
	private static final Item[][] SUITS = {
		{ Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE,
		  Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS },
		{ Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
		  Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS },
		{ Items.IRON_HELMET, Items.IRON_CHESTPLATE,
		  Items.IRON_LEGGINGS, Items.IRON_BOOTS },
		{ Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE,
		  Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS },
		{ Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
		  Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS },
	};

	private static final Item[] BLADES = {
		Items.DIAMOND_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND_AXE,
	};

	private static void arm(Zombie walker, RandomSource random, RegistryAccess access) {
		Item[] suit = SUITS[random.nextInt(SUITS.length)];
		EquipmentSlot[] slots = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		};
		for (int i = 0; i < slots.length; i++) {
			// A gap in the set is worth more than a full one. Somebody who is
			// wearing three quarters of a suit of armour lost the other quarter.
			if (random.nextInt(5) == 0) {
				continue;
			}
			ItemStack piece = new ItemStack(suit[i]);
			enchant(piece, random, access, 8 + random.nextInt(18));
			walker.setItemSlot(slots[i], piece);
			walker.setDropChance(slots[i], 0.0F);
		}
		ItemStack blade = new ItemStack(BLADES[random.nextInt(BLADES.length)]);
		enchant(blade, random, access, 15 + random.nextInt(16));
		walker.setItemSlot(EquipmentSlot.MAINHAND, blade);
		walker.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

		// The pace. Most are slower than a walk; one in five is faster than one,
		// and that one is the reason the group cannot be ignored.
		double pace = random.nextInt(5) == 0
			? 0.30 + random.nextDouble() * 0.09
			: 0.17 + random.nextDouble() * 0.08;
		if (walker.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
			walker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(pace);
		}

		if (random.nextInt(6) == 0) {
			mount(walker, random);
		}
	}

	/**
	 * And one in six is riding.
	 *
	 * A skeleton horse rather than a living one, because it does not burn, does
	 * not panic, does not wander off to eat and cannot be tamed out from under
	 * him by a player with wheat. It is also the only horse in the game that
	 * looks like it belongs here.
	 */
	private static void mount(Zombie walker, RandomSource random) {
		if (!(walker.level() instanceof ServerLevel level)) {
			return;
		}
		net.minecraft.world.entity.animal.equine.SkeletonHorse horse =
			EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.EVENT);
		if (horse == null) {
			return;
		}
		horse.snapTo(walker.getX(), walker.getY(), walker.getZ(),
			walker.getYRot(), 0.0F);
		horse.setTamed(true);
		horse.setPersistenceRequired();
		if (horse.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
			horse.getAttribute(Attributes.MOVEMENT_SPEED)
				.setBaseValue(0.22 + random.nextDouble() * 0.14);
		}
		level.addFreshEntity(horse);
		walker.startRiding(horse);
	}

	// ---- THE CREEPERS ------------------------------------------------------
	/**
	 * Every one of them charged.
	 *
	 * Vanilla only makes these when lightning strikes one, which in an ordinary
	 * world happens perhaps twice in a save. Here it is the default, and the
	 * blast radius is the reason the place cannot be walked through carelessly:
	 * a charged creeper takes out a hole you can stand in and most of your
	 * hearts through iron.
	 *
	 * IT IS ALSO THE FAIREST THING IN THE DIMENSION. A charged creeper is
	 * visible from a long way off — it is blue, it crackles, and it is the one
	 * threat here that announces exactly what it is before it is in range. A
	 * player who dies to one was told.
	 */
	private static void charge(Creeper creeper) {
		if (!(creeper.level() instanceof ServerLevel here)) {
			return;
		}
		// STRUCK, RATHER THAN FLAGGED.
		//
		// The powered state lives in a private EntityDataAccessor on Creeper with
		// no setter, because vanilla only ever reaches it from thunderHit. The
		// obvious ways in are both bad: reflection on the field NAME works in a
		// dev environment and breaks the moment the mod is remapped for release,
		// and an accessor mixin is a permanent dependency on a private field for
		// one boolean.
		//
		// thunderHit is public, and it is also simply TRUE here. This is a
		// dimension in a storm that never stops; a creeper standing in it has
		// been hit by lightning, and that is the entire explanation for why every
		// single one of them is charged.
		//
		// The bolt is never added to the world — it exists for the length of this
		// call as the cause of the hit — so nothing is set alight and nobody is
		// hurt by the arming of a mob that spawned in a wood forty blocks away.
		LightningBolt cause = EntityTypes.LIGHTNING_BOLT.create(here, EntitySpawnReason.EVENT);
		if (cause == null) {
			return;
		}
		cause.setVisualOnly(true);
		cause.snapTo(creeper.getX(), creeper.getY(), creeper.getZ(), 0.0F, 0.0F);
		creeper.thunderHit(here, cause);
	}

	// ---- WHAT THE FIRE LEAVES ----------------------------------------------
	/**
	 * A SHALLOW DISH IN THE GROUND UNDER WHERE IT WENT OFF.
	 *
	 * Called from HisFireballMixin, which explains why this exists at all: the
	 * explosion goes off against a TRUNK at chest height, so its own block damage
	 * takes some leaves and never reaches the floor. Twenty fights' worth of that
	 * left the wood looking untouched.
	 *
	 * Small on purpose. Two across, one or two deep, and RAGGED — a clean bowl
	 * reads as something stamped, and the whole value of these is that the ground
	 * should look chewed rather than excavated. It is the accumulation that does
	 * the work: one is nothing, and coming back through a clearing you fought in
	 * an hour ago and finding it pitted is the thing.
	 *
	 * Natural ground only, and it never touches anything crafted. The keep and
	 * the city are the one thing in this dimension that must not erode — a castle
	 * that his own garrison shells to rubble over a week is a bug, not a story.
	 */
	private static final int DENT = 2;
	/** A weather strike bites deeper than a fireball. */
	private static final int BOLT_DENT = 3;
	/** And how many fires it leaves round the rim. */
	private static final int BOLT_FIRES = 5;

	/**
	 * What a bolt does to his ground.
	 *
	 * Gated the same three ways every other terrain effect in this file is: only in
	 * his world, only when mobGriefing allows it, and only when hisHost is on.
	 * Somebody who has turned mobGriefing off has said they do not want terrain
	 * touched, and this is terrain being touched however it is dressed up.
	 *
	 * The fires burn out on their own after six seconds. A permanent fire in a dark
	 * forest under permanent rain is a forest fire, and the point of this is a
	 * pockmarked wood rather than a burnt one.
	 */
	public static void struck(ServerLevel here, BlockPos at) {
		if (!Config.get().hisHost
			|| !here.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		dent(here, at, BOLT_DENT);

		RandomSource random = here.getRandom();
		int lit = 0;
		for (int attempt = 0; attempt < 16 && lit < BOLT_FIRES; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 1.4 + random.nextDouble() * 2.6;
			BlockPos near = BlockPos.containing(at.getX() + Math.cos(angle) * range,
				at.getY(), at.getZ() + Math.sin(angle) * range);
			BlockPos ground = null;
			for (int down = 0; down <= 3 && ground == null; down++) {
				if (here.getBlockState(near.below(down)).isSolid()) {
					ground = near.below(down);
				}
			}
			if (ground == null || !here.getBlockState(ground.above()).isAir()) {
				continue;
			}
			final BlockPos flame = ground.above();
			here.setBlock(flame, Blocks.FIRE.defaultBlockState(), 2);
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), 120, () -> {
				if (here.getBlockState(flame).is(Blocks.FIRE)) {
					here.setBlock(flame, Blocks.AIR.defaultBlockState(), 2);
				}
			});
			lit++;
		}
	}

	public static void dent(ServerLevel here, BlockPos impact) {
		dent(here, impact, DENT);
	}

	public static void dent(ServerLevel here, BlockPos impact, int wide) {
		RandomSource random = here.getRandom();
		// Down to the floor from wherever it burst. Six is enough to reach the
		// ground from a shot that went off in a trunk or a low branch, and short
		// enough that one detonating up in the canopy simply does nothing.
		BlockPos ground = null;
		for (int down = 0; down <= 6 && ground == null; down++) {
			BlockPos maybe = impact.below(down);
			if (here.getBlockState(maybe).isSolid() && diggable(here, maybe)) {
				ground = maybe;
			}
		}
		if (ground == null) {
			return;
		}
		int taken = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				ground.offset(-wide, -wide + 1, -wide), ground.offset(wide, 0, wide))) {
			double away = Math.sqrt(pos.distSqr(ground));
			if (away > wide - 0.2 || random.nextInt(5) == 0) {
				continue;
			}
			if (!diggable(here, pos)) {
				continue;
			}
			here.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			taken++;
		}
		// The rim scorched, which is what says a thing went off here rather than
		// that somebody dug. One block of it, so the wood does not turn brown.
		if (taken > 0 && diggable(here, ground.below(2))) {
			here.setBlock(ground.below(2), Blocks.COARSE_DIRT.defaultBlockState(), 3);
		}
	}

	/**
	 * Ground, and only ground.
	 *
	 * Anything crafted is refused outright — the castle, the city walls, the
	 * houses and their lamps. The garrison is allowed to wreck the country it
	 * stands in and not the thing it is standing in.
	 */
	private static boolean diggable(ServerLevel here, BlockPos at) {
		net.minecraft.world.level.block.state.BlockState state = here.getBlockState(at);
		if (state.isAir() || state.getDestroySpeed(here, at) < 0
			|| here.getBlockEntity(at) != null
			|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(here, at)) {
			return false;
		}
		return state.is(net.minecraft.tags.BlockTags.DIRT)
			|| state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
			|| state.is(net.minecraft.tags.BlockTags.SAND)
			|| state.is(Blocks.GRAVEL)
			|| state.is(net.minecraft.tags.BlockTags.SNOW)
			|| state.is(net.minecraft.tags.BlockTags.REPLACEABLE);
	}

	// ---- THE WORKSHOP ------------------------------------------------------
	private static void wear(Mob mob, RandomSource random, RegistryAccess access,
	                         Item head, Item chest, Item legs, Item feet) {
		EquipmentSlot[] slots = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		};
		Item[] pieces = { head, chest, legs, feet };
		for (int i = 0; i < slots.length; i++) {
			ItemStack piece = new ItemStack(pieces[i]);
			enchant(piece, random, access, 10 + random.nextInt(15));
			mob.setItemSlot(slots[i], piece);
			// NOTHING DROPS, and this is not meanness. A dimension full of
			// enchanted diamond that a player can farm is a dimension they will
			// farm, and the last chapter of this mod would become a gear run.
			mob.setDropChance(slots[i], 0.0F);
		}
	}

	/**
	 * Real enchantments, rolled the way an enchanting table rolls them.
	 *
	 * enchantItem rather than a hand-picked list, so what comes out is whatever
	 * that piece could legitimately have — and the player reading a mob's gear
	 * sees the same names and combinations they would see on their own. A
	 * hardcoded Protection IV on everything would be legible as a mod within a
	 * minute.
	 */
	/**
	 * Public, because the shelter after a hunt wants the same roll.
	 *
	 * A second enchanting helper would drift from this one, and the whole reason
	 * this uses vanilla's own enchantItem is that what comes out has to be
	 * indistinguishable from what a player's own table produces.
	 */
	public static void enchant(ItemStack stack, RandomSource random,
	                           RegistryAccess access, int power) {
		try {
			EnchantmentHelper.enchantItem(random, stack, power, access,
				access.lookup(Registries.ENCHANTMENT)
					.flatMap(registry -> registry.get(
						net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT)));
		} catch (RuntimeException broken) {
			// A failed roll costs a glint, not a mob. Never worth a crash in the
			// last chapter of somebody's save.
			HerobrineMod.LOGGER.debug("could not enchant {}: {}",
				stack.getItem(), broken.getMessage());
		}
	}
}
