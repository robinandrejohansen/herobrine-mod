package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
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
	 * A MAP TO THE NEXT ONE, AND HE IS THE ONE WHO DREW IT.
	 *
	 * Why he would help is the interesting part, and it is not a contradiction.
	 * Per the spine he wants to be FOUND — a haunting nobody investigates is a
	 * haunting nobody is frightened by, and every building in this mod is only
	 * worth standing up if somebody walks into it. So at the end of a hunt he
	 * points the way, because the players going deeper is what he wants. The map
	 * is not a kindness. It is an invitation, and taking it is the players'
	 * decision.
	 *
	 * It also fixes the one genuine content failure this mod has had: a group
	 * walked a thousand blocks west, raided villages, found signs and graves, and
	 * never found a house — so the whole middle of the story sat in a field
	 * nobody visited. Siting near the players helped. A map ends it.
	 *
	 * SCALE 3, the same as a treasure map, with unlimited tracking on. That
	 * combination is what makes it navigable rather than a picture: the player's
	 * own marker pins itself to the edge of the parchment and points, so it works
	 * from four hundred bleak blocks away instead of only once they are already
	 * standing on it.
	 *
	 * Points at the next building NOBODY HAS WALKED UP ON, not simply the next
	 * one in the sequence — so it never sends a group somewhere four of them have
	 * already looted, and it happily sends them to one that is sited but not yet
	 * built, because arriving is what builds it.
	 */
	private static ItemStack chart(ServerLevel level) {
		BlockPos target = com.bloomlet.herobrine.structure.Dwellings.unfound(level);
		if (target == null) {
			return ItemStack.EMPTY;
		}
		ItemStack map = net.minecraft.world.item.MapItem.create(
			level, target.getX(), target.getZ(), (byte)3, true, true);
		net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
			map, target, "+",
			net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_MARKER);
		// Named, because an unnamed filled map is something a player assumes they
		// made themselves and forgot about. This one has to be unmistakably a
		// thing that was left for them.
		map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal(
				INVITATIONS[level.getRandom().nextInt(INVITATIONS.length)]));
		HerobrineMod.LOGGER.info("a map to [{}, {}] left in the grave",
			target.getX(), target.getZ());
		return map;
	}

	/**
	 * What is written on the map.
	 *
	 * The same register as the signs — eviction, not menace — with one difference
	 * that matters: these are all pointing OUTWARD. "there is one more" is him
	 * telling them the world goes on further than they have gone, which is both
	 * an invitation and the thing he most wants them to believe is hopeless.
	 */
	private static final String[] INVITATIONS = {
		"come and see", "this way", "if you must",
		"there is one more", "you will want to know", "go on then",
	};

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
	 * A SHELTER WHERE HE BROKE OFF, WITH THE CHEST INSIDE IT.
	 *
	 * Only ever after a hunt, which is what makes it read as an exchange rather
	 * than as loot. The player has just survived something; this is what is
	 * standing there when they come back to the spot.
	 *
	 * IT WAS A SLAB WITH A CHEST BURIED UNDER IT and that was too small to be
	 * read. Half a metre of stone in a field is indistinguishable from terrain,
	 * and the thing it was competing with for attention was a storm that had just
	 * finished. A building is unmissable from any direction, and — more to the
	 * point — it is the only structure in the mod that was put up FOR the player
	 * rather than against them.
	 *
	 * Five by five outside, three by three in, a door on the side they were last
	 * standing on. Mossy and cracked, so it reads as having been there a while
	 * rather than as having appeared, which is the same lie the rest of his
	 * buildings tell.
	 *
	 * BUILT ONCE. See HerobrineEntity.relent for the reason that has to be said
	 * out loud: this used to run on every tick of the full stop, and because each
	 * pass asked the heightmap for the ground that the previous pass had raised,
	 * it produced a column of chests fifty blocks high with the player's
	 * belongings scattered up it. The guard is in relent, and the extra check
	 * below is a second lock on the same door.
	 */
	public static void shelter(ServerLevel level, BlockPos where, ServerPlayer player) {
		ItemStack map = chart(level);
		RandomSource random = level.getRandom();
		BlockPos ground = level.getHeightmapPos(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, where);

		// Belt and braces. If something already put one here, this is a repeat
		// call and the honest thing to do is nothing at all.
		if (!level.getBlockState(ground.below()).isSolid()
			|| level.getBlockState(ground.above()).is(Blocks.CHEST)) {
			return;
		}

		// The floor has to be somewhere a five by five will sit without hanging
		// off a cliff. Four corners on solid ground is enough of a test — a
		// perfectly level plot would refuse most of the overworld.
		for (int dx = -2; dx <= 2; dx += 4) {
			for (int dz = -2; dz <= 2; dz += 4) {
				if (!level.getBlockState(ground.offset(dx, -1, dz)).isSolid()) {
					return;
				}
			}
		}

		Direction facing = Direction.fromYRot(player.getYRot()).getOpposite();
		hut(level, ground, facing, random);

		BlockPos chest = ground.above().relative(facing.getOpposite());
		level.setBlock(chest, Blocks.CHEST.defaultBlockState()
			.setValue(net.minecraft.world.level.block.ChestBlock.FACING, facing), 3);
		if (level.getBlockEntity(chest) instanceof ChestBlockEntity box) {
			fill(level, box, map, random);
		}
		headstone(level, ground, facing, player, random);
		HerobrineMod.LOGGER.info("a shelter at [{}, {}, {}] with some of it back",
			ground.getX(), ground.getY(), ground.getZ());
	}

	/** Walls, roof, door, and a light so it can be found after dark. */
	private static void hut(ServerLevel level, BlockPos floor, Direction facing,
	                        RandomSource random) {
		for (BlockPos pos : BlockPos.betweenClosed(
				floor.offset(-2, 0, -2), floor.offset(2, 3, 2))) {
			int dx = pos.getX() - floor.getX();
			int dz = pos.getZ() - floor.getZ();
			int dy = pos.getY() - floor.getY();
			boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
			if (dy == 3) {
				// A flat roof of planks, oversailing by nothing, because eaves on
				// a three by three read as a doll's house.
				level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
			} else if (wall) {
				level.setBlock(pos, stone(random), 3);
			} else {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		// The floor, under the room only, so the walls keep their footing.
		for (BlockPos pos : BlockPos.betweenClosed(
				floor.offset(-1, -1, -1), floor.offset(1, -1, 1))) {
			level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
		}
		// The door faces the way they were last looking, so they walk in rather
		// than round.
		BlockPos door = floor.relative(facing, 2);
		level.setBlock(door, Blocks.OAK_DOOR.defaultBlockState()
			.setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing.getOpposite())
			.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
		level.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState()
			.setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing.getOpposite())
			.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
		// One lantern, hung. Not his soul lantern — this building is not his.
		level.setBlock(floor.offset(0, 2, 0), Blocks.LANTERN.defaultBlockState()
			.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 3);
	}

	/** Mossy, cracked or plain, so no two walls are the same age. */
	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 4) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 7) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	/**
	 * THE NAME OUTSIDE AND THEIR OWN THINGS IN.
	 *
	 * Kept from the version this replaces, because it was the best thing about
	 * it: the sign carries somebody else's name and the chest carries the
	 * player's belongings, and one block apart is the whole sentence.
	 */
	private static void headstone(ServerLevel level, BlockPos floor, Direction facing,
	                             ServerPlayer player, RandomSource random) {
		BlockPos post = floor.relative(facing, 3);
		BlockPos plate = post.above();
		if (!level.getBlockState(post).canBeReplaced()
			|| !level.getBlockState(plate).canBeReplaced()) {
			return;
		}
		level.setBlock(post, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
		level.setBlock(plate, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 3);
		if (level.getBlockEntity(plate)
				instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
			String[] lines = SignLines.grave(
				com.bloomlet.herobrine.wrath.Wrath.phase(level.getServer()),
				player, null, random);
			net.minecraft.world.level.block.entity.SignText text =
				new net.minecraft.world.level.block.entity.SignText();
			for (int row = 0; row < 4; row++) {
				text = text.setMessage(row, net.minecraft.network.chat.Component.literal(
					row < lines.length ? lines[row] : ""));
			}
			sign.setText(text, true);
			sign.setWaxed(true);
		}
	}

	/**
	 * The map, then what he took, then something he did not.
	 *
	 * THE ENCHANTED PIECE IS NEW and it is the reason to open the box even on a
	 * hunt where he had stolen nothing. It goes through HisHost.enchant, which is
	 * vanilla's own enchantItem — so what comes out is whatever that piece could
	 * legitimately have rolled, and a player reading it sees the same names and
	 * combinations their own table produces. A hardcoded Sharpness V would be
	 * legible as a mod inside a minute.
	 *
	 * One or two pieces, at a power somebody would need thirty levels for. It is
	 * payment for having survived, and it should be worth more than the walk.
	 */
	private static void fill(ServerLevel level, ChestBlockEntity box,
	                         ItemStack map, RandomSource random) {
		int slot = 0;
		if (!map.isEmpty()) {
			box.setItem(slot++, map);   // first, so it is what they see
		}
		int given = 0;
		while (slot < box.getContainerSize() && given < 4) {
			ItemStack stack = draw(level, random);
			if (stack == null) {
				break;
			}
			box.setItem(slot++, stack);
			given++;
		}
		net.minecraft.core.RegistryAccess access = level.registryAccess();
		int pieces = 1 + random.nextInt(2);
		for (int i = 0; i < pieces && slot < box.getContainerSize(); i++) {
			ItemStack kit = new ItemStack(REWARDS[random.nextInt(REWARDS.length)]);
			com.bloomlet.herobrine.manifest.HisHost.enchant(kit, random, access,
				18 + random.nextInt(15));
			box.setItem(slot++, kit);
		}
		box.setChanged();
	}

	/**
	 * Iron and diamond, never netherite.
	 *
	 * Diamond because surviving him should be worth a real piece; not netherite
	 * because that requires a trip he may not have made yet, and handing somebody
	 * the best armour in the game for their first hunt flattens everything after
	 * it. A mix of tools and weapons so the draw is not always the same slot in
	 * their inventory.
	 */
	private static final net.minecraft.world.item.Item[] REWARDS = {
		net.minecraft.world.item.Items.DIAMOND_SWORD,
		net.minecraft.world.item.Items.DIAMOND_AXE,
		net.minecraft.world.item.Items.DIAMOND_PICKAXE,
		net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
		net.minecraft.world.item.Items.DIAMOND_HELMET,
		net.minecraft.world.item.Items.DIAMOND_BOOTS,
		net.minecraft.world.item.Items.IRON_SWORD,
		net.minecraft.world.item.Items.IRON_CHESTPLATE,
		net.minecraft.world.item.Items.IRON_LEGGINGS,
		net.minecraft.world.item.Items.BOW,
	};
}
