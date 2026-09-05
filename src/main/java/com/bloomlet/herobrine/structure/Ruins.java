package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.Corpses;
import com.bloomlet.herobrine.manifest.Cadence;
import com.bloomlet.herobrine.manifest.DwellTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * WHAT IT LOOKS LIKE AFTER HE HAS BEEN. He came down out of the sky, stood in
 * the street and stared, and then he killed everyone he could reach and burned
 * and blew up everything he could not. Every place on the road gets that:
 * burned-out cottages with their roofs gone, craters lined with black stone
 * and still smouldering, scorched ground, fires that do not go out, and the
 * dead where they fell — villagers laid in the yards and the streets, lootable
 * like every other body. One iron golem at the town, on its face.
 *
 * Laid a few seconds after the place itself, one piece per tick, so nothing
 * here costs the server a long tick, and nothing lands on a building
 * (DwellTracker) or a path.
 */
public final class Ruins {
	private Ruins() {}

	/** Per place: how many burned cottages, craters and bodies, and how far out from the door they lie. */
	private record Toll(int cottages, int craters, int bodies, int near, int far) {}

	private static Toll tollFor(String place) {
		return switch (place) {
			case "HOMESTEAD" -> new Toll(2, 2, 3, 18, 40);
			case "TOWN" -> new Toll(4, 6, 9, 14, 56);
			case "TOWER" -> new Toll(2, 3, 3, 16, 40);
			case "GAOL" -> new Toll(2, 3, 4, 16, 42);
			case "CHURCH" -> new Toll(3, 4, 6, 16, 44);
			case "THRESHOLD" -> new Toll(3, 5, 6, 18, 46);
			default -> new Toll(2, 2, 3, 16, 40);
		};
	}

	/** Called by Dwellings once a place stands. Everything is scheduled; nothing runs now. */
	public static void around(ServerLevel level, String place, BlockPos site) {
		Toll toll = tollFor(place);
		RandomSource random = level.getRandom();
		int at = 100;      // the town's own pieces are still going up for the first three seconds
		for (int i = 0; i < toll.cottages(); i++, at += 2) {
			Cadence.in(level.getServer(), at, () -> cottage(level, spot(level, site, toll, random), random));
		}
		for (int i = 0; i < toll.craters(); i++, at += 2) {
			Cadence.in(level.getServer(), at, () -> crater(level, spot(level, site, toll, random), random));
		}
		Cadence.in(level.getServer(), at, () -> scorch(level, site, toll, random));
		at += 2;
		for (int i = 0; i < toll.bodies(); i++, at += 1) {
			boolean golem = place.equals("TOWN") && i == 0;
			Cadence.in(level.getServer(), at, () -> body(level, spot(level, site, toll, random), random, golem));
		}
		HerobrineMod.LOGGER.info("the {} will be a battlefield: {} burned houses, {} craters, {} dead",
			place.toLowerCase(java.util.Locale.ROOT), toll.cottages(), toll.craters(), toll.bodies());
	}

	/** A spot in the ring round the site on dry, unbuilt, open ground; null after enough misses. */
	private static @org.jspecify.annotations.Nullable BlockPos spot(ServerLevel level, BlockPos site, Toll toll, RandomSource random) {
		for (int tries = 0; tries < 24; tries++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double out = toll.near() + random.nextDouble() * (toll.far() - toll.near());
			int x = site.getX() + (int) Math.round(Math.cos(angle) * out);
			int z = site.getZ() + (int) Math.round(Math.sin(angle) * out);
			if (!level.hasChunk(x >> 4, z >> 4) || !Ground.dry(level, x, z)) {
				continue;
			}
			int y = Ground.topOf(level, x, z);
			BlockPos on = new BlockPos(x, y, z);
			BlockState ground = level.getBlockState(on);
			if (!ground.isSolid() || ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.POLISHED_ANDESITE)
				|| DwellTracker.isBuilt(level, on) || DwellTracker.isBuilt(level, on.above())
				|| Math.abs(y - site.getY()) > 8) {
				continue;
			}
			return on;
		}
		return null;
	}

	// ---- A BURNED-OUT COTTAGE ---------------------------------------------------

	private static void cottage(ServerLevel level, @org.jspecify.annotations.Nullable BlockPos on, RandomSource random) {
		if (on == null) {
			return;
		}
		int w = 5 + random.nextInt(3);
		int d = 5 + random.nextInt(3);
		int doorSide = random.nextInt(4);
		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				int x = on.getX() + dx - w / 2;
				int z = on.getZ() + dz - d / 2;
				int y = Ground.topOf(level, x, z) + 1;
				if (Math.abs(y - on.getY() - 1) > 3) {
					continue;
				}
				boolean wall = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
				BlockPos floor = new BlockPos(x, y - 1, z);
				level.setBlock(floor, random.nextInt(3) == 0
					? Blocks.BLACKSTONE.defaultBlockState()
					: Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 2);
				if (!wall) {
					if (random.nextInt(7) == 0) {
						level.setBlock(floor.above(), Blocks.COBWEB.defaultBlockState(), 2);
					}
					continue;
				}
				boolean door = (doorSide == 0 && dz == 0 || doorSide == 1 && dz == d - 1
					|| doorSide == 2 && dx == 0 || doorSide == 3 && dx == w - 1)
					&& (dx == w / 2 || dz == d / 2);
				if (door) {
					continue;
				}
				// THE WALLS ARE WHAT IS LEFT OF THEM. Nothing above three, most of it
				// lower, corners highest, the top row black where the fire took it.
				boolean corner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
				int height = corner ? 2 + random.nextInt(2) : random.nextInt(4);
				for (int up = 0; up < height; up++) {
					boolean top = up == height - 1;
					level.setBlock(new BlockPos(x, y + up, z), top && random.nextBoolean()
						? Blocks.BLACKSTONE.defaultBlockState()
						: random.nextInt(3) == 0 ? Blocks.COBBLESTONE.defaultBlockState()
						: Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
		// A fallen beam, a chest that survived, and a fire that did not go out.
		int bx = on.getX() + random.nextInt(Math.max(1, w - 2)) - (w - 2) / 2;
		int bz = on.getZ() + random.nextInt(Math.max(1, d - 2)) - (d - 2) / 2;
		BlockPos beam = new BlockPos(bx, Ground.topOf(level, bx, bz) + 1, bz);
		if (level.getBlockState(beam).isAir()) {
			level.setBlock(beam, Blocks.BASALT.defaultBlockState()
				.setValue(BlockStateProperties.AXIS, random.nextBoolean() ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z), 2);
		}
		if (random.nextInt(10) < 4) {
			BlockPos chestAt = on.above();
			if (level.getBlockState(chestAt).isAir()) {
				level.setBlock(chestAt, Blocks.CHEST.defaultBlockState(), 2);
				if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
					if (random.nextInt(10) < 3) {
						chest.setItem(0, Loot.tome(level.registryAccess(), random, 1));
					}
					Loot.scatter(chest, random, Loot.Tier.LARDER);
				}
			}
		}
		if (random.nextInt(10) < 4) {
			ember(level, on.offset(1 - random.nextInt(3), 0, 1 - random.nextInt(3)), random);
		}
	}

	// ---- A CRATER ---------------------------------------------------------------

	private static void crater(ServerLevel level, @org.jspecify.annotations.Nullable BlockPos on, RandomSource random) {
		if (on == null) {
			return;
		}
		int r = 2 + random.nextInt(3);
		int depth = 1 + random.nextInt(Math.max(1, r - 1));
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double reach = Math.hypot(dx, dz);
				if (reach > r + 0.4) {
					continue;
				}
				int x = on.getX() + dx;
				int z = on.getZ() + dz;
				int top = Ground.topOf(level, x, z);
				if (DwellTracker.isBuilt(level, new BlockPos(x, top, z))) {
					continue;
				}
				// A bowl: deepest in the middle, one step at the rim.
				int dig = (int) Math.round(depth * (1.0 - reach / (r + 0.4)));
				for (int y = top; y > top - dig; y--) {
					level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				BlockPos lining = new BlockPos(x, top - dig, z);
				int roll = random.nextInt(10);
				level.setBlock(lining, roll < 4 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
					: roll < 7 ? Blocks.BLACKSTONE.defaultBlockState()
					: roll < 9 ? Blocks.GRAVEL.defaultBlockState()
					: Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
				for (int y = top + 1; y <= top + 2; y++) {
					BlockPos over = new BlockPos(x, y, z);
					if (!level.getBlockState(over).isSolid()) {
						break;
					}
					level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);      // whatever stood over it is gone
				}
			}
		}
		if (random.nextInt(10) < 6) {
			ember(level, on.offset(r, 0, 0), random);
		}
		if (random.nextInt(10) < 3) {
			ember(level, on.offset(-r, 0, random.nextInt(3) - 1), random);
		}
	}

	// ---- SCORCHED GROUND AND FIRES THAT DO NOT GO OUT --------------------------------

	private static void scorch(ServerLevel level, BlockPos site, Toll toll, RandomSource random) {
		int patches = 12 + toll.craters() * 6;
		for (int i = 0; i < patches; i++) {
			BlockPos on = spot(level, site, new Toll(0, 0, 0, Math.max(6, toll.near() - 8), toll.far()), random);
			if (on == null) {
				continue;
			}
			int size = 1 + random.nextInt(3);
			for (int dx = -size; dx <= size; dx++) {
				for (int dz = -size; dz <= size; dz++) {
					if (random.nextInt(3) == 0) {
						continue;
					}
					int x = on.getX() + dx;
					int z = on.getZ() + dz;
					BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
					BlockState was = level.getBlockState(ground);
					if (!was.isSolid() || was.is(Blocks.DIRT_PATH) || DwellTracker.isBuilt(level, ground)) {
						continue;
					}
					int roll = random.nextInt(10);
					level.setBlock(ground, roll < 5 ? Blocks.COARSE_DIRT.defaultBlockState()
						: roll < 8 ? Blocks.BLACKSTONE.defaultBlockState()
						: Blocks.GRAVEL.defaultBlockState(), 2);
					BlockPos over = ground.above();
					if (!level.getBlockState(over).isAir()) {
						level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);      // the grass and flowers burned
					}
				}
			}
			if (random.nextInt(5) == 0) {
				ember(level, on, random);
			}
		}
	}

	/** Fire on netherrack, or blue fire on soul soil, burns forever. */
	private static void ember(ServerLevel level, BlockPos near, RandomSource random) {
		int x = near.getX();
		int z = near.getZ();
		BlockPos ground = new BlockPos(x, Ground.topOf(level, x, z), z);
		if (DwellTracker.isBuilt(level, ground) || !level.getBlockState(ground.above()).isAir()) {
			return;
		}
		boolean soul = random.nextInt(4) == 0;
		level.setBlock(ground, (soul ? Blocks.SOUL_SOIL : Blocks.NETHERRACK).defaultBlockState(), 2);
		level.setBlock(ground.above(), (soul ? Blocks.SOUL_FIRE : Blocks.FIRE).defaultBlockState(), 2);
	}

	// ---- THE DEAD WHERE THEY FELL -------------------------------------------------

	private static void body(ServerLevel level, @org.jspecify.annotations.Nullable BlockPos on, RandomSource random, boolean golem) {
		if (on == null) {
			return;
		}
		Mob dead = golem
			? EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.STRUCTURE)
			: EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
		if (dead == null) {
			return;
		}
		dead.snapTo(on.getX() + 0.5, on.getY() + 1, on.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		java.util.List<ItemStack> pockets = new java.util.ArrayList<>();
		if (!golem) {
			if (random.nextBoolean()) {
				pockets.add(new ItemStack(Items.BREAD, 1 + random.nextInt(2)));
			}
			if (random.nextInt(3) == 0) {
				pockets.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(3)));
			}
		} else {
			pockets.add(new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(3)));
			pockets.add(new ItemStack(Items.POPPY));
		}
		Corpses.body(dead, pockets);
		level.addFreshEntity(dead);
	}
}
