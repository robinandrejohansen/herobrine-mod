package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * HIS STATUE, ONE BLOCK TO THE PIXEL.
 *
 * The memorial used to be a seven-by-seven blackstone yard with the effigy block
 * in the middle — a head-sized thing that read as a head-sized thing. This is his
 * skin, the actual texture the renderer draws him with, put up in blocks at one
 * block per pixel: eight-wide head, thirty-two tall, the white of the eyes in sea
 * lanterns so they are lit at night, and the sword in his right hand with its
 * point on the plinth. It stands on the city square facing the way home, so the
 * first thing anybody sees coming back through is him, looking at them.
 *
 * Reading the texture rather than hand-placing colours means the statue is
 * whatever the skin is. Change the skin and the statue follows.
 */
public final class Statue {

	private Statue() {}

	private static final String SKIN = "/assets/herobrine/textures/entity/herobrine.png";
	private static final String EYES = "/assets/herobrine/textures/entity/herobrine_eyes.png";

	/** Blocks the pixels can be, and roughly what colour each one is. */
	private record Paint(Block block, int r, int g, int b) {}

	private static final List<Paint> PALETTE = List.of(
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE), 207, 213, 214),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY), 125, 125, 115),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.GRAY), 55, 58, 62),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK), 8, 10, 15),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE), 209, 178, 161),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY), 135, 107, 98),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.GRAY), 58, 42, 36),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BLACK), 37, 23, 16),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BROWN), 77, 51, 35),
		new Paint(Blocks.TERRACOTTA, 152, 94, 67),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE), 161, 83, 37),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.RED), 143, 61, 46),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.CYAN), 21, 119, 136),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.CYAN), 87, 91, 91),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE), 36, 137, 199),
		new Paint(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE), 45, 47, 143),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BLUE), 74, 60, 91),
		new Paint(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.PURPLE), 118, 70, 86),
		new Paint(Blocks.DARK_OAK_PLANKS, 67, 43, 20),
		new Paint(Blocks.SPRUCE_PLANKS, 115, 85, 49),
		new Paint(Blocks.JUNGLE_PLANKS, 160, 115, 81),
		new Paint(Blocks.STRIPPED_JUNGLE_LOG, 171, 132, 84),
		new Paint(Blocks.STRIPPED_ACACIA_LOG, 174, 92, 59),
		new Paint(Blocks.STRIPPED_OAK_LOG, 178, 144, 86),
		new Paint(Blocks.DEEPSLATE, 80, 80, 82),
		new Paint(Blocks.TUFF, 108, 109, 102));

	/** One cuboid of the skin: where its box sits in the texture and how big it is. */
	private record Part(int u, int v, int w, int h, int d, int xRight, int yTop, int zFront) {}

	/**
	 * Raises him at `feet`: the block his boots stand on is two above it (the plinth
	 * is two layers), and he faces north — toward smaller z — so put the thing he
	 * should be looking at on that side.
	 */
	public static void raise(ServerLevel level, BlockPos feet, @Nullable ServerPlayer killer) {
		BufferedImage skin = read(SKIN);
		BufferedImage eyes = read(EYES);
		if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 64) {
			HerobrineMod.LOGGER.warn("no skin to build the statue from — the square keeps its cobbles");
			return;
		}
		int cx = feet.getX();
		int yG = feet.getY() + 2;                 // the boots
		int zF = feet.getZ();                     // the front of the body

		// Room for him: nothing in the way from the plinth to a little above the head.
		for (BlockPos pos : BlockPos.betweenClosed(
				new BlockPos(cx - 10, feet.getY(), zF - 5), new BlockPos(cx + 10, yG + 36, zF + 8))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		plinth(level, cx, feet.getY(), zF);

		// The six boxes of a player model, in the order the eye reads them.
		Part[] parts = {
			new Part(0, 16, 4, 12, 4, cx + 3, yG + 11, zF),      // right leg
			new Part(16, 48, 4, 12, 4, cx - 1, yG + 11, zF),     // left leg
			new Part(16, 16, 8, 12, 4, cx + 3, yG + 23, zF),     // body
			new Part(40, 16, 4, 12, 4, cx + 7, yG + 23, zF),     // right arm
			new Part(32, 48, 4, 12, 4, cx - 5, yG + 23, zF),     // left arm
			new Part(0, 0, 8, 8, 8, cx + 3, yG + 31, zF - 2),    // head
		};
		int placed = 0;
		for (Part part : parts) {
			placed += box(level, skin, eyes, part);
		}
		sword(level, cx, yG, zF);
		dressing(level, cx, feet.getY(), zF, killer);
		HerobrineMod.LOGGER.info("his statue stands on the square at [{}, {}, {}] — {} blocks of him",
			cx, yG, zF, placed);
	}

	/**
	 * One box. Every voxel on the surface takes the colour of the pixel on the face
	 * it belongs to; the inside is blackstone. The statue faces north, so his right
	 * is +x, the front is the smaller z, and the texture's front face — which runs
	 * from his right to his left — runs from +x down to -x.
	 */
	private static int box(ServerLevel level, BufferedImage skin, @Nullable BufferedImage eyes, Part p) {
		int put = 0;
		for (int lx = 0; lx < p.w(); lx++) {
			for (int ly = 0; ly < p.h(); ly++) {
				for (int lz = 0; lz < p.d(); lz++) {
					BlockPos at = new BlockPos(p.xRight() - lx, p.yTop() - ly, p.zFront() + lz);
					boolean surface = lx == 0 || lx == p.w() - 1 || ly == 0 || ly == p.h() - 1
						|| lz == 0 || lz == p.d() - 1;
					if (!surface) {
						level.setBlock(at, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
						put++;
						continue;
					}
					int u;
					int v;
					if (lz == 0) {                                   // front
						u = p.u() + p.d() + lx;
						v = p.v() + p.d() + ly;
					} else if (lz == p.d() - 1) {                    // back, mirrored
						u = p.u() + 2 * p.d() + p.w() + (p.w() - 1 - lx);
						v = p.v() + p.d() + ly;
					} else if (lx == 0) {                            // his right side (+x)
						u = p.u() + (p.d() - 1 - lz);
						v = p.v() + p.d() + ly;
					} else if (lx == p.w() - 1) {                    // his left side (-x)
						u = p.u() + p.d() + p.w() + lz;
						v = p.v() + p.d() + ly;
					} else if (ly == 0) {                            // top
						u = p.u() + p.d() + lx;
						v = p.v() + (p.d() - 1 - lz);
					} else {                                         // bottom
						u = p.u() + p.d() + p.w() + lx;
						v = p.v() + (p.d() - 1 - lz);
					}
					BlockState paint = paint(skin, eyes, u, v);
					if (paint == null) {
						continue;
					}
					level.setBlock(at, paint, 2);
					put++;
				}
			}
		}
		return put;
	}

	private static @Nullable BlockState paint(BufferedImage skin, @Nullable BufferedImage eyes, int u, int v) {
		if (u < 0 || v < 0 || u >= skin.getWidth() || v >= skin.getHeight()) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		// THE EYES. Whatever the eyes layer marks is lit — that layer is what the
		// renderer draws fullbright, and the statue should hold the same stare.
		if (eyes != null && u < eyes.getWidth() && v < eyes.getHeight()
			&& ((eyes.getRGB(u, v) >>> 24) & 0xFF) > 127) {
			return Blocks.SEA_LANTERN.defaultBlockState();
		}
		int argb = skin.getRGB(u, v);
		if (((argb >>> 24) & 0xFF) < 128) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();   // nothing painted there
		}
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		if (r > 225 && g > 225 && b > 225) {
			return Blocks.SEA_LANTERN.defaultBlockState();
		}
		Paint best = PALETTE.get(0);
		int bestAt = Integer.MAX_VALUE;
		for (Paint paint : PALETTE) {
			int dr = paint.r() - r;
			int dg = paint.g() - g;
			int db = paint.b() - b;
			int at = dr * dr + dg * dg + db * db;
			if (at < bestAt) {
				bestAt = at;
				best = paint;
			}
		}
		return best.block().defaultBlockState();
	}

	/** The sword, in front of the right arm, held low: grip in the hand, point on the stone. */
	private static void sword(ServerLevel level, int cx, int yG, int zF) {
		int z = zF - 1;
		for (int x = cx + 5; x <= cx + 6; x++) {
			for (int y = yG + 12; y <= yG + 13; y++) {
				level.setBlock(new BlockPos(x, y, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
			}
			for (int y = yG + 2; y <= yG + 10; y++) {
				level.setBlock(new BlockPos(x, y, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
			}
		}
		for (int x = cx + 4; x <= cx + 7; x++) {
			level.setBlock(new BlockPos(x, yG + 11, z), Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState(), 2);
		}
		level.setBlock(new BlockPos(cx + 5, yG + 1, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx + 6, yG + 1, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx + 5, yG, z), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
	}

	/** Two layers of blackstone under him, and whatever is under those filled solid. */
	private static void plinth(ServerLevel level, int cx, int paving, int zF) {
		for (BlockPos pos : BlockPos.betweenClosed(
				new BlockPos(cx - 10, paving, zF - 5), new BlockPos(cx + 10, paving, zF + 8))) {
			level.setBlock(pos, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
			for (int down = 1; down <= 6; down++) {
				BlockPos under = pos.below(down);
				if (level.getBlockState(under).isSolid()) {
					break;
				}
				level.setBlock(under, Blocks.BLACKSTONE.defaultBlockState(), 2);
			}
		}
		for (BlockPos pos : BlockPos.betweenClosed(
				new BlockPos(cx - 9, paving + 1, zF - 4), new BlockPos(cx + 9, paving + 1, zF + 7))) {
			level.setBlock(pos, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
		}
	}

	/** Lanterns at the corners, and a plaque at his feet saying who did it. */
	private static void dressing(ServerLevel level, int cx, int paving, int zF, @Nullable ServerPlayer killer) {
		for (int x : new int[] { cx - 9, cx + 9 }) {
			for (int z : new int[] { zF - 4, zF + 7 }) {
				level.setBlock(new BlockPos(x, paving + 2, z), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
			}
		}
		BlockPos plaque = new BlockPos(cx, paving + 2, zF - 4);
		level.setBlock(plaque, Blocks.DARK_OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 8), 2);      // faces north, toward the way
		if (level.getBlockEntity(plaque) instanceof SignBlockEntity sign) {
			String who = killer == null ? "nobody who stayed" : killer.getName().getString();
			if (who.length() > 15) {
				who = who.substring(0, 15);
			}
			long day = level.getGameTime() / 24000L;   // 26.2 has no getDayTime; the world's age will do for a plaque
			sign.setText(new SignText()
				.setMessage(0, Component.literal("HEROBRINE"))
				.setMessage(1, Component.literal("fell here"))
				.setMessage(2, Component.literal("to " + who))
				.setMessage(3, Component.literal("day " + day)), true);
		}
	}

	private static @Nullable BufferedImage read(String path) {
		try (InputStream in = Statue.class.getResourceAsStream(path)) {
			return in == null ? null : javax.imageio.ImageIO.read(in);
		} catch (java.io.IOException e) {
			HerobrineMod.LOGGER.warn("could not read {} for the statue: {}", path, e.toString());
			return null;
		}
	}
}
