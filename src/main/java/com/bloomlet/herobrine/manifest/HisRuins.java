package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;
import com.bloomlet.herobrine.entity.Corpses;
import com.bloomlet.herobrine.structure.Ground;
import com.bloomlet.herobrine.structure.Keep;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * WHAT IS LEFT OF THE WAR IN HIS FOREST.
 *
 * His world was a forest, a city and a castle, and nothing between. Somebody
 * fought him here before you — Addexio's nineteen, and whoever came before them
 * — and the forest should say so. As a player walks the woods, now and then the
 * ground ahead of them has one of these on it, laid in the same grammar as the
 * battlefields round the houses in the overworld:
 *
 *   a WATCHTOWER   twelve to eighteen high in deepslate and moss; half of them
 *                  fallen, the top lying in the trees beside the stump; the
 *                  standing ones with a ladder inside, and on top a chest and
 *                  somebody who did not come down
 *   a SIEGE CAMP   a burned siege tower standing against nothing, a ladder
 *                  leaning on air, a tent, a cold fire with the pot still on it
 *   FOUNDATIONS    the outline of a house in mossy stone, a doorframe with no
 *                  house, a furnace on its own
 *   A GALLOWS      and a line of skulls on posts along what was a road
 *   A BATTLEFIELD  craters with soul fire in them, the fallen, a grave with a
 *                  board and no name on it
 *
 * Laid out of sight, 40 to 72 blocks out, never inside the city's reach or the
 * castle's, never within MIN_APART of another — so roughly one every three or
 * four chunks of forest. Remembered on his level so the spacing survives a
 * restart. Cost: one gated pass every EVERY ticks over his.players(), and a few
 * hundred blocks set in the tick one is laid — no more than one per player per
 * pass.
 */
public final class HisRuins {

	private HisRuins() {}

	private static final int EVERY = 200;
	private static final int CHANCE = 2;
	private static final double OUT_MIN = 40.0;
	private static final double OUT_MAX = 72.0;
	private static final double MIN_APART = 56.0;

	private static final AttachmentType<List<Long>> LAID = AttachmentRegistry.createPersistent(
		HerobrineMod.id("his_ruins"), Codec.LONG.listOf());

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HisRuins::walk);
	}

	private static void walk(MinecraftServer server) {
		if (++tickCounter % EVERY != 0 || !Config.get().enabled || !Config.get().hisHost
			|| !Config.get().ruins) {
			return;
		}
		ServerLevel his = server.getLevel(TheWayBlock.HIS_WORLD);
		if (his == null || his.players().isEmpty()) {
			return;
		}
		RandomSource random = his.getRandom();
		for (ServerPlayer player : his.players()) {
			if (player.isSpectator() || random.nextInt(CHANCE) != 0) {
				continue;
			}
			BlockPos at = spot(his, player, random);
			if (at == null) {
				continue;
			}
			String what = lay(his, at, random);
			List<Long> laid = new ArrayList<>(his.getAttachedOrElse(LAID, List.of()));
			laid.add(at.asLong());
			his.setAttached(LAID, List.copyOf(laid));
			HerobrineMod.LOGGER.info("his forest: {} at [{}, {}, {}]", what, at.getX(), at.getY(), at.getZ());
		}
	}

	private static BlockPos spot(ServerLevel his, ServerPlayer player, RandomSource random) {
		List<Long> laid = his.getAttachedOrElse(LAID, List.of());
		for (int tries = 0; tries < 6; tries++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double out = OUT_MIN + random.nextDouble() * (OUT_MAX - OUT_MIN);
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * out);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * out);
			if (!loaded(his, x, z) || !Ground.dry(his, x, z) || nearHis(his, x, z)) {
				continue;
			}
			int y = Ground.topOf(his, x, z);
			if (!flat(his, x, y, z)) {
				continue;
			}
			BlockPos at = new BlockPos(x, y, z);
			boolean crowded = false;
			for (long one : laid) {
				if (BlockPos.of(one).closerThan(at, MIN_APART)) {
					crowded = true;
					break;
				}
			}
			if (!crowded) {
				return at;
			}
		}
		return null;
	}

	/** Every chunk a ruin can touch is already loaded: never generate one to build in it. */
	private static boolean loaded(ServerLevel his, int x, int z) {
		for (int dx = -12; dx <= 12; dx += 12) {
			for (int dz = -12; dz <= 12; dz += 12) {
				if (!his.hasChunk((x + dx) >> 4, (z + dz) >> 4)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean flat(ServerLevel his, int x, int y, int z) {
		for (int dx = -4; dx <= 4; dx += 4) {
			for (int dz = -4; dz <= 4; dz += 4) {
				if (Math.abs(Ground.topOf(his, x + dx, z + dz) - y) > 3) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean nearHis(ServerLevel his, int x, int z) {
		BlockPos at = new BlockPos(x, 0, z);
		BlockPos city = Keep.city(his);
		if (city != null && city.atY(0).closerThan(at, Keep.cityReach() + 16)) {
			return true;
		}
		BlockPos keep = Keep.site(his);
		return keep != null && keep.atY(0).closerThan(at, Keep.reach() + 16);
	}

	private static String lay(ServerLevel his, BlockPos at, RandomSource random) {
		return switch (random.nextInt(5)) {
			case 0 -> tower(his, at, random);
			case 1 -> camp(his, at, random);
			case 2 -> foundations(his, at, random);
			case 3 -> gallows(his, at, random);
			default -> battlefield(his, at, random);
		};
	}

	// ---- THE WATCHTOWER ------------------------------------------------------

	private static String tower(ServerLevel his, BlockPos base, RandomSource random) {
		int full = 12 + random.nextInt(7);
		boolean fallen = random.nextBoolean();
		int height = fallen ? 4 + random.nextInt(6) : full;
		Direction ladderSide = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		for (int y = 1; y <= height; y++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					BlockPos at = base.offset(dx, y, dz);
					if (!wall) {
						set(his, at, Blocks.AIR.defaultBlockState());
						continue;
					}
					if (fallen && y >= height - 1 && random.nextInt(3) == 0) {
						continue;      // a ragged top
					}
					if (dx == -2 * ladderSide.getStepX() && dz == -2 * ladderSide.getStepZ() && (y == 1 || y == 2)) {
						set(his, at, Blocks.AIR.defaultBlockState());      // the door, on the wall across from the ladder
						continue;
					}
					set(his, at, stone(random));
				}
			}
			if (!fallen || y < height - 1) {
				// against the inside of the ladder-side wall, facing in
				BlockPos rung = base.offset(ladderSide.getStepX(), y, ladderSide.getStepZ());
				set(his, rung, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderSide.getOpposite()));
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				set(his, base.offset(dx, 0, dz), random.nextInt(4) == 0
					? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
			}
		}
		if (!fallen) {
			BlockPos top = base.above(full + 1);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockPos floor = top.offset(dx, -1, dz);
					if (!(dx == ladderSide.getStepX() && dz == ladderSide.getStepZ())) {      // the hole the ladder comes up through
						set(his, floor, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
					}
					if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && (dx + dz) % 2 == 0) {
						set(his, top.offset(dx, 0, dz), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
					}
				}
			}
			chest(his, top.offset(1, 0, 1), random);
			fallen(his, top.offset(-1, 0, 1), random);
			return "a watchtower, standing, " + full + " high";
		}
		// the top lies in the trees beside the stump
		Direction fell = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		for (int along = 3; along < full - height + 4; along++) {
			for (int spread = -1; spread <= 1; spread++) {
				if (random.nextInt(3) == 0) {
					continue;
				}
				int x = base.getX() + fell.getStepX() * along + (fell.getStepZ() != 0 ? spread : 0);
				int z = base.getZ() + fell.getStepZ() * along + (fell.getStepX() != 0 ? spread : 0);
				set(his, new BlockPos(x, Ground.topOf(his, x, z) + 1, z), stone(random));
			}
		}
		return "a watchtower, fallen";
	}

	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(10);
		return roll < 4 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
			: roll < 6 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
			: roll < 8 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
			: Blocks.MOSSY_COBBLESTONE.defaultBlockState();
	}

	// ---- THE SIEGE CAMP ------------------------------------------------------

	private static String camp(ServerLevel his, BlockPos base, RandomSource random) {
		// a burned siege tower against nothing
		int high = 8 + random.nextInt(3);
		for (int y = 1; y <= high; y++) {
			for (int[] post : new int[][] { { 0, 0 }, { 2, 0 }, { 0, 2 }, { 2, 2 } }) {
				if (y > high - 2 && random.nextInt(2) == 0) {
					continue;
				}
				set(his, base.offset(post[0], y, post[1]), random.nextInt(4) == 0
					? Blocks.BLACKSTONE.defaultBlockState() : Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
			}
			if (y % 3 == 0) {
				for (int i = 0; i <= 2; i++) {
					set(his, base.offset(i, y, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
					set(his, base.offset(i, y, 2), Blocks.DARK_OAK_FENCE.defaultBlockState());
				}
			}
		}
		// a ladder leaning on the air
		for (int y = 1; y <= 5; y++) {
			set(his, base.offset(4, y, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		// a tent
		BlockPos tent = base.offset(-6, 0, 3);
		for (int i = 0; i < 4; i++) {
			set(his, tent.offset(i, 1, 0), Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState());
			set(his, tent.offset(i, 1, 2), Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState());
			set(his, tent.offset(i, 2, 1), random.nextInt(4) == 0 ? Blocks.AIR.defaultBlockState()
				: Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState());
		}
		set(his, tent.offset(-1, 1, 1), Blocks.DARK_OAK_FENCE.defaultBlockState());
		set(his, tent.offset(4, 1, 1), Blocks.DARK_OAK_FENCE.defaultBlockState());
		// a cold fire with the pot still on it
		BlockPos fire = base.offset(-2, 1, -3);
		set(his, fire, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
		set(his, fire.east(), Blocks.CAULDRON.defaultBlockState());
		fallen(his, base.offset(-3, 1, 0), random);
		return "a burned siege camp";
	}

	// ---- FOUNDATIONS ---------------------------------------------------------

	private static String foundations(ServerLevel his, BlockPos base, RandomSource random) {
		int w = 6 + random.nextInt(3);
		int d = 7 + random.nextInt(3);
		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
				BlockPos floor = base.offset(dx, 0, dz);
				set(his, floor, random.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
					: Blocks.COBBLESTONE.defaultBlockState());
				if (edge && random.nextInt(3) != 0) {
					set(his, floor.above(), random.nextInt(2) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
						: Blocks.COBBLESTONE.defaultBlockState());
					if (random.nextInt(4) == 0) {
						set(his, floor.above(2), Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
					}
				} else if (!edge && random.nextInt(5) == 0) {
					set(his, floor.above(), Blocks.MOSS_CARPET.defaultBlockState());
				}
			}
		}
		// a doorframe with no house
		BlockPos door = base.offset(w / 2, 0, 0);
		for (int y = 1; y <= 3; y++) {
			set(his, door.offset(-1, y, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
			set(his, door.offset(1, y, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
		}
		set(his, door.above(1), Blocks.AIR.defaultBlockState());
		set(his, door.above(2), Blocks.AIR.defaultBlockState());
		set(his, door.offset(0, 3, 0), Blocks.DARK_OAK_LOG.defaultBlockState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		// a furnace on its own
		set(his, base.offset(w - 2, 1, d - 2), Blocks.FURNACE.defaultBlockState());
		return "the foundations of a house";
	}

	// ---- THE GALLOWS ---------------------------------------------------------

	private static String gallows(ServerLevel his, BlockPos base, RandomSource random) {
		for (int y = 1; y <= 4; y++) {
			set(his, base.offset(0, y, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
			set(his, base.offset(3, y, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
		}
		for (int x = 0; x <= 3; x++) {
			set(his, base.offset(x, 5, 0), Blocks.DARK_OAK_LOG.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		}
		for (int y = 4; y >= 3; y--) {
			set(his, base.offset(1, y, 0), Blocks.IRON_CHAIN.defaultBlockState());
			set(his, base.offset(2, y, 0), Blocks.IRON_CHAIN.defaultBlockState());
		}
		// skulls on posts along what was a road
		Direction road = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int posts = 4 + random.nextInt(3);
		for (int i = 1; i <= posts; i++) {
			int x = base.getX() + road.getStepX() * (3 + i * 3) + (road.getStepZ() != 0 ? 3 : 0);
			int z = base.getZ() + road.getStepZ() * (3 + i * 3) + (road.getStepX() != 0 ? 3 : 0);
			int y = Ground.topOf(his, x, z);
			BlockPos post = new BlockPos(x, y + 1, z);
			set(his, post, Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(his, post.above(), Blocks.SKELETON_SKULL.defaultBlockState()
				.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)));
			set(his, new BlockPos(x, y, z), Blocks.COARSE_DIRT.defaultBlockState());
		}
		return "a gallows, and skulls along the road";
	}

	// ---- THE BATTLEFIELD -----------------------------------------------------

	private static String battlefield(ServerLevel his, BlockPos base, RandomSource random) {
		int craters = 2 + random.nextInt(2);
		for (int i = 0; i < craters; i++) {
			crater(his, base.offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8), random);
		}
		int dead = 3 + random.nextInt(3);
		for (int i = 0; i < dead; i++) {
			int x = base.getX() + random.nextInt(19) - 9;
			int z = base.getZ() + random.nextInt(19) - 9;
			fallen(his, new BlockPos(x, Ground.topOf(his, x, z) + 1, z), random);
		}
		// a grave with a board and no name on it
		BlockPos grave = base.offset(6, 0, -6);
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 5; dz++) {
				int x = grave.getX() + dx;
				int z = grave.getZ() + dz;
				set(his, new BlockPos(x, Ground.topOf(his, x, z), z), Blocks.COARSE_DIRT.defaultBlockState());
			}
		}
		int gx = grave.getX() + 1;
		int gz = grave.getZ() - 1;
		set(his, new BlockPos(gx, Ground.topOf(his, gx, gz) + 1, gz), Blocks.DARK_OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)));
		return "a battlefield: " + craters + " craters, " + dead + " dead";
	}

	private static void crater(ServerLevel his, BlockPos on, RandomSource random) {
		int r = 2 + random.nextInt(2);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > r + 0.4) {
					continue;
				}
				int x = on.getX() + dx;
				int z = on.getZ() + dz;
				int top = Ground.topOf(his, x, z);
				int dig = (int) Math.round(2 * (1.0 - reach / (r + 0.4)));
				for (int y = top; y > top - dig; y--) {
					set(his, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				set(his, new BlockPos(x, top - dig, z), random.nextInt(3) == 0
					? Blocks.BLACKSTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
				BlockPos over = new BlockPos(x, top + 1, z);
				if (!his.getBlockState(over).isAir() && !his.getBlockState(over).isSolid()) {
					set(his, over, Blocks.AIR.defaultBlockState());
				}
			}
		}
		int sx = on.getX() + r;
		int sz = on.getZ();
		int sy = Ground.topOf(his, sx, sz);
		set(his, new BlockPos(sx, sy, sz), Blocks.SOUL_SOIL.defaultBlockState());
		set(his, new BlockPos(sx, sy + 1, sz), Blocks.SOUL_FIRE.defaultBlockState());
	}

	// ---- THE FALLEN ----------------------------------------------------------

	/** One of the nineteen, or one before them: a body in his pockets' worth of iron. Laid out to stay. */
	private static void fallen(ServerLevel his, BlockPos on, RandomSource random) {
		Mob dead = random.nextInt(6) == 0
			? EntityTypes.IRON_GOLEM.create(his, EntitySpawnReason.STRUCTURE)
			: EntityTypes.VILLAGER.create(his, EntitySpawnReason.STRUCTURE);
		if (dead == null) {
			return;
		}
		dead.snapTo(on.getX() + 0.5, on.getY(), on.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		List<ItemStack> pockets = new ArrayList<>();
		ItemStack[] gear = { new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
			new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD), new ItemStack(Items.ARROW, 4 + random.nextInt(9)),
			new ItemStack(Items.BREAD, 1 + random.nextInt(3)), new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(4)) };
		for (ItemStack one : gear) {
			if (random.nextInt(3) == 0) {
				if (one.isDamageableItem()) {
					one.setDamageValue(one.getMaxDamage() / 2 + random.nextInt(Math.max(1, one.getMaxDamage() / 3)));
				}
				pockets.add(one);
			}
		}
		Corpses.body(dead, pockets);
		his.addFreshEntity(dead);
	}

	private static void chest(ServerLevel his, BlockPos at, RandomSource random) {
		set(his, at, Blocks.CHEST.defaultBlockState());
		if (his.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			com.bloomlet.herobrine.structure.Loot.scatter(chest, random, com.bloomlet.herobrine.structure.Loot.Tier.HIS_CITY);
		}
	}

	private static void set(ServerLevel his, BlockPos at, BlockState state) {
		his.setBlock(at, state, 2);
	}
}
