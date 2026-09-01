package com.bloomlet.herobrine.structure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bloomlet.herobrine.HerobrineMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PLACE A BUILDING THAT WAS MEASURED SOMEWHERE ELSE, BLOCK FOR BLOCK.
 *
 * Keep.java builds a castle from NUMBERS taken off the Legacy Console tutorial
 * world — the wall's reach by bearing, the stall pattern, the sign pairs. That is
 * a parametric castle wearing TU19's proportions, and asked directly whether it
 * was the actual TU19 build, the answer is no. This is the other thing: the
 * blocks themselves, in their own positions, with their own states.
 *
 * NOTHING SHIPS WITH THE MOD, AND THAT IS DELIBERATE. A blueprint of somebody
 * else's build is their build; this repository is public and a release goes to a
 * website. So the mod carries the READER and the file lives in the player's own
 * config directory:
 *
 *     config/herobrine/blueprints/<name>.json
 *
 * Which is also simply better. Drop any building in there and it can be placed —
 * the format is not specific to a castle and tools/castle will export any box of
 * any 1.13-era world into it.
 *
 * THE FORMAT, which tools/castle/extract.py writes:
 *
 *     { "size":     { "x": 71, "y": 49, "z": 72 },
 *       "palette":  [ "cobblestone", "oak_stairs[facing=south,half=bottom]", ... ],
 *       "blocks":   [ [x, y, z, paletteIndex], ... ] }
 *
 * Offsets are relative to the blueprint's own corner, so the same file can be
 * placed anywhere. The palette entries are ordinary block-state strings — the
 * same syntax /setblock takes — so BlockStateParser resolves them and an entry
 * naming a block this version does not have fails loudly on its own line rather
 * than taking the whole building down.
 *
 * SPREAD ACROSS TICKS for the reason Keep.raise is: twenty thousand blocks in one
 * tick is a visible stall, and a stall is a worse first impression than a wait.
 */
public final class Blueprint {
	private Blueprint() {}

	private static final Gson GSON = new Gson();

	/** How many blocks go down per tick. */
	private static final int PER_TICK = 2000;

	/**
	 * Make the folder and drop a note in it, once, at startup.
	 *
	 * An empty directory that exists is a hundred times more discoverable than a
	 * path in a log line nobody reads. Called from the mod initialiser.
	 */
	public static void ready() {
		Path dir = folder();
		try {
			Files.createDirectories(dir);
			Path note = dir.resolve("README.txt");
			if (!Files.exists(note)) {
				Files.writeString(note,
					"Put blueprint .json files in this folder.\n\n"
					+ "  /herobrine blueprint <name>   places one where you stand\n\n"
					+ "Set keepBlueprint in herobrine.json to the name of one and it\n"
					+ "becomes his castle in the dimension instead of the built-in\n"
					+ "one. Blank it to go back.\n\n"
					+ "Format:\n"
					+ "  { \"size\":    { \"x\": 71, \"y\": 49, \"z\": 72 },\n"
					+ "    \"ground\":  11,\n"
					+ "    \"palette\": [ \"cobblestone\", \"oak_stairs[facing=south]\" ],\n"
					+ "    \"blocks\":  [ [x, y, z, paletteIndex] ] }\n\n"
					+ "\"ground\" is the local y that sits at the terrain surface —\n"
					+ "everything below it is foundation. Omit it and 0 is used.\n");
			}
		} catch (Exception broken) {
			HerobrineMod.LOGGER.warn("could not make {}: {}", dir, broken.getMessage());
		}
	}

	/** Where the player puts the files. */
	public static Path folder() {
		return FabricLoader.getInstance().getConfigDir()
			.resolve("herobrine").resolve("blueprints");
	}

	/** What is in that folder, without the .json. */
	public static List<String> available() {
		List<String> out = new ArrayList<>();
		Path dir = folder();
		if (!Files.isDirectory(dir)) {
			return out;
		}
		try (var files = Files.list(dir)) {
			files.filter(p -> p.getFileName().toString().endsWith(".json"))
				.forEach(p -> {
					String n = p.getFileName().toString();
					out.add(n.substring(0, n.length() - 5));
				});
		} catch (Exception broken) {
			HerobrineMod.LOGGER.warn("could not list {}: {}", dir, broken.getMessage());
		}
		java.util.Collections.sort(out);
		return out;
	}

	/** What a load either produced or could not. */
	public record Placed(int blocks, int skipped, int sizeX, int sizeY, int sizeZ) {}

	/**
	 * STAND IT UP ON THE GROUND, CENTRED, which is what a building wants.
	 *
	 * place() puts a corner at a point, which is right for a command where somebody
	 * is standing where they want the corner. It is wrong for everything else. A
	 * castle handed to Keep has to be CENTRED on the site — Herobrine is spawned
	 * over Keep.site() and the weather measures from it, so a corner-placed castle
	 * would put him hovering over one of its towers — and it has to sit at the
	 * right HEIGHT, which is not its own y 0.
	 *
	 * The file says where its ground is. The tutorial castle's widest layer is
	 * local y 11 with 1330 blocks in it; that is its main floor, and the eleven
	 * courses below are foundation and cellar. Placing y 0 at the surface would
	 * float the whole building eleven blocks.
	 *
	 * And it beards: every column of the footprint is filled from the blueprint's
	 * own lowest course down to whatever the terrain actually is, the same fix the
	 * causeway and the curtain got. A building measured off a hill, dropped on a
	 * slope, otherwise has daylight under half of it.
	 */
	public static @org.jspecify.annotations.Nullable Placed stand(
			ServerLevel level, BlockPos centre, String name) {
		JsonObject root = read(name);
		if (root == null) {
			return null;
		}
		JsonObject size = root.getAsJsonObject("size");
		int sx = size.get("x").getAsInt();
		int sy = size.get("y").getAsInt();
		int sz = size.get("z").getAsInt();
		int ground = root.has("ground") ? root.get("ground").getAsInt() : 0;

		BlockPos corner = new BlockPos(centre.getX() - sx / 2,
			centre.getY() - ground, centre.getZ() - sz / 2);
		for (int cx = corner.getX() >> 4; cx <= (corner.getX() + sx) >> 4; cx++) {
			for (int cz = corner.getZ() >> 4; cz <= (corner.getZ() + sz) >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}
		// Cut the space out first, then hold it up, then build in it. Same order
		// Keep.ground uses and for the same reason: a tree left standing inside a
		// hall is worse than no hall.
		clear(level, corner, sx, sy, sz);
		beard(level, corner, sx, sz);
		return place(level, corner, name);
	}

	/**
	 * Hold the footprint up. Down to the real surface, per column, capped.
	 */
	private static void beard(ServerLevel level, BlockPos corner, int sx, int sz) {
		for (int dx = 0; dx < sx; dx++) {
			for (int dz = 0; dz < sz; dz++) {
				int x = corner.getX() + dx;
				int z = corner.getZ() + dz;
				int surface = Ground.topOf(level, x, z);
				for (int y = corner.getY() - 1; y > surface && y > corner.getY() - 24; y--) {
					BlockPos at = new BlockPos(x, y, z);
					if (!level.getBlockState(at).isSolid()) {
						level.setBlock(at, Blocks.DEEPSLATE.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	private static @org.jspecify.annotations.Nullable JsonObject read(String name) {
		Path file = folder().resolve(name + ".json");
		if (!Files.isRegularFile(file)) {
			HerobrineMod.LOGGER.warn("no blueprint at {}", file);
			return null;
		}
		try {
			return GSON.fromJson(Files.readString(file), JsonObject.class);
		} catch (Exception broken) {
			HerobrineMod.LOGGER.warn("{} did not read as a blueprint: {}",
				file, broken.getMessage());
			return null;
		}
	}

	/** Whether a named blueprint is there to be placed. */
	public static boolean have(String name) {
		return name != null && !name.isBlank()
			&& Files.isRegularFile(folder().resolve(name + ".json"));
	}

	/** How many palette entries had to drop their properties, for the last load. */
	private static int lastPlain;

	public static int lastPlain() {
		return lastPlain;
	}

	/**
	 * Read one blueprint and stand it up with its corner at `at`.
	 *
	 * Returns null and logs if the file is missing or unreadable — the caller is a
	 * command and wants to say so rather than throw.
	 */
	public static @org.jspecify.annotations.Nullable Placed place(
			ServerLevel level, BlockPos at, String name) {
		JsonObject root = read(name);
		if (root == null) {
			return null;
		}

		// ---- the palette, resolved once
		JsonArray pal = root.getAsJsonArray("palette");
		BlockState[] states = new BlockState[pal.size()];
		int lost = 0;
		var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
		int plain = 0;
		for (int i = 0; i < pal.size(); i++) {
			String spec = modernise(pal.get(i).getAsString());
			try {
				states[i] = BlockStateParser
					.parseForBlock(blocks, "minecraft:" + spec, false).blockState();
				continue;
			} catch (Exception stale) {
				// EXPECTED, AND OFTEN. A blueprint measured out of a 1.13 world
				// carries 1.13 block states, and properties have moved since: walls
				// were four booleans then and are four WallSide enums now, so every
				// cobblestone_wall[north=true,...] in the file is unparseable here.
				// That is 394 blocks in the tutorial castle alone.
				//
				// So the properties are dropped and the block is kept. A wall in its
				// default state is visibly not the wall that was measured; a hole
				// where a wall should be is not a building.
			}
			int bracket = spec.indexOf('[');
			if (bracket > 0) {
				try {
					states[i] = BlockStateParser.parseForBlock(blocks,
						"minecraft:" + spec.substring(0, bracket), false).blockState();
					plain++;
					continue;
				} catch (Exception gone) {
					// falls through
				}
			}
			// The block itself does not exist any more. Nothing to place.
			states[i] = null;
			lost++;
			HerobrineMod.LOGGER.warn("blueprint {}: no block called {}", name, spec);
		}
		lastPlain = plain;
		if (plain > 0) {
			HerobrineMod.LOGGER.info(
				"blueprint {}: {} of {} palette entries placed without their"
					+ " properties — the file is from an older version", name,
				plain, pal.size());
		}

		JsonObject size = root.getAsJsonObject("size");
		JsonArray rows = root.getAsJsonArray("blocks");
		MinecraftServer server = level.getServer();

		// ---- the chunks, before anything is written
		int sx = size.get("x").getAsInt();
		int sz = size.get("z").getAsInt();
		for (int cx = at.getX() >> 4; cx <= (at.getX() + sx) >> 4; cx++) {
			for (int cz = at.getZ() >> 4; cz <= (at.getZ() + sz) >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}

		// ---- and the blocks, PER_TICK at a time
		int placed = 0;
		for (int from = 0; from < rows.size(); from += PER_TICK) {
			final int start = from;
			final int end = Math.min(rows.size(), from + PER_TICK);
			com.bloomlet.herobrine.manifest.Cadence.in(server, from / PER_TICK, () -> {
				for (int i = start; i < end; i++) {
					JsonArray r = rows.get(i).getAsJsonArray();
					BlockState state = states[r.get(3).getAsInt()];
					if (state == null) {
						continue;
					}
					level.setBlock(at.offset(r.get(0).getAsInt(), r.get(1).getAsInt(),
						r.get(2).getAsInt()), state, 2);
				}
			});
			placed += end - start;
		}
		HerobrineMod.LOGGER.info("blueprint {}: {} blocks at [{}, {}, {}], {} palette"
			+ " entries unplaceable", name, placed, at.getX(), at.getY(), at.getZ(), lost);
		return new Placed(placed, lost, sx, size.get("y").getAsInt(), sz);
	}

	/**
	 * TRANSLATE A 1.13 BLOCK STATE INTO ONE THIS VERSION KNOWS.
	 *
	 * Measured against the tutorial-castle blueprint: 243 palette entries, and
	 * 98.6% of the twenty thousand blocks land byte-identical without any of this.
	 * The remaining 1.4% is two specific changes, and both are worth handling
	 * rather than logging:
	 *
	 *   WALLS. 269 blocks. In 1.13 a wall's four sides were booleans; 1.16 made
	 *   them a WallSide enum, so every cobblestone_wall[north=true,...] in an old
	 *   file is unparseable. Vanilla's own converter mapped true to LOW and false
	 *   to NONE and so does this.
	 *
	 *   RENAMES. 20 blocks. wall_sign became oak_wall_sign in 1.14 when the other
	 *   woods arrived, and a renamed block does not fail on its properties, it
	 *   fails entirely — those twenty were being dropped on the floor.
	 *
	 * Everything not listed here is passed through untouched. This is a translation
	 * table for what the exporter has actually produced, not an attempt at a
	 * general data-fixer: an unknown entry still falls back to its bare name and
	 * then to being skipped, which is the honest behaviour for a file this code has
	 * never seen.
	 */
	private static String modernise(String spec) {
		int bracket = spec.indexOf('[');
		String name = bracket < 0 ? spec : spec.substring(0, bracket);
		String props = bracket < 0 ? "" : spec.substring(bracket);

		switch (name) {
			case "sign" -> name = "oak_sign";
			case "wall_sign" -> name = "oak_wall_sign";
			case "grass" -> name = "short_grass";
			default -> { }
		}
		if (name.endsWith("_wall") && !props.isEmpty()) {
			for (String side : new String[] { "north", "east", "south", "west" }) {
				props = props.replace(side + "=true", side + "=low")
					.replace(side + "=false", side + "=none");
			}
		}
		return name + props;
	}

	/**
	 * Clear the box a blueprint will occupy, so it is not standing in a hillside.
	 *
	 * Separate from place() on purpose. A blueprint dropped into open ground wants
	 * this; one being fitted into an existing build does not, and the caller is the
	 * only one who knows which.
	 */
	public static void clear(ServerLevel level, BlockPos at, int sx, int sy, int sz) {
		for (int dx = 0; dx < sx; dx++) {
			for (int dz = 0; dz < sz; dz++) {
				for (int dy = 0; dy < sy; dy++) {
					level.setBlock(at.offset(dx, dy, dz),
						Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}
}
