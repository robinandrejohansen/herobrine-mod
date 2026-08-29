package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * WHAT HE WAS COMING BACK TO.
 *
 * Every building in this mod so far has been somewhere he STOPPED — a farmhouse
 * he left, a tower he abandoned, a gaol he cut and walked away from. Each one is
 * smaller and stranger than the last, and read in order they say a man coming
 * apart. This is the other end of that line, and the whole reason the dimension
 * exists: the place he was going, still standing, still lit, and built by
 * somebody who had not come apart at all.
 *
 * SO IT IS THE ONLY COMPETENT BUILDING IN THE MOD. Square, walled, towered,
 * garrisoned. The homestead's roof sags and the church's warren is a hole; this
 * has battlements that line up. That contrast is the entire point of putting it
 * here — the player has spent forty hours reading decay as evidence of him, and
 * then finds the one place he kept.
 *
 * IT IS FOUND BY ITS LIGHT, not by a road. Soul fire on four towers, above a
 * canopy, in a dimension whose fog closes at a hundred and twelve blocks — so it
 * is sited inside that, and the first thing anybody sees after stepping out of
 * the landing is a blue glow through the trees with no explanation attached to
 * it. A path would have answered the question before it was asked.
 *
 * DETERMINISTIC FROM THE LANDING. The bearing and distance are derived from the
 * arrival coordinates rather than rolled, so two players on a server walk to the
 * same castle, and somebody who leaves and comes back finds it where they left
 * it.
 *
 * The shell only, for now. Walls, towers, gate, courtyard and keep — no
 * interior, no contents and nothing living in it beyond what already walks the
 * dimension. What goes inside is the next thing.
 */
public final class Keep {
	private Keep() {}

	/** Where it stands, once decided. One per world. */
	private static final AttachmentType<Long> SITE =
		AttachmentRegistry.createPersistent(HerobrineMod.id("keep_site"), Codec.LONG);
	private static final AttachmentType<Boolean> RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("keep_raised"), Codec.BOOL);

	/**
	 * How far out, and it is short on purpose. THE FOG SETS THIS, NOT TASTE.
	 *
	 * The dimension's fog ends at a hundred and twelve blocks and a dark forest
	 * canopy closes whatever it leaves. Anything sited beyond that is invisible
	 * until it is underfoot, which turns the one landmark in the place into
	 * something found by accident — and the whole design of this build is that
	 * it is found by its light.
	 *
	 * IT WAS BRIEFLY 96–128, WHICH BROKE THAT. The castle doubled in size and the
	 * distance got dragged up with it out of instinct, straight past the fog: a
	 * site rolled at the far end stood sixteen blocks beyond anything the player
	 * could ever see, in a wood with no sightlines, and the blue glow through the
	 * trees simply was not there.
	 *
	 * Eighty to a hundred. The tower tops sit thirty above the ground on the
	 * motte, so the furthest one is about a hundred and four blocks away as the
	 * eye measures it — inside the fog with room to spare, and still a real walk
	 * through the dark to get there.
	 */
	private static final int NEAR = 80;
	private static final int FAR = 100;
	/** Built when somebody is this close. Inside a default simulation radius. */
	private static final int RAISE_RANGE = 144;
	/**
	 * How far a stored site's height may be from the ground before it is a lie.
	 *
	 * pick() and highest() both take their Y straight off Ground.topOf at the
	 * position they return, so a site chosen correctly agrees exactly. Twelve is
	 * slack for nothing in particular — it only has to be small enough to catch a
	 * site carrying a player's arrival altitude, which is out by tens of blocks or
	 * it would not have been noticed.
	 */
	private static final int SLIPPED = 12;
	private static final int CHECK_INTERVAL = 40;

	// ---- THE SHELL, AND IT IS MEANT TO BE TOO BIG ---------------------------
	//
	// Doubled from the first pass, and the size is the point rather than a
	// setting. Everything else he ever built is a room or two — the homestead is
	// a farmhouse, the gaol is four cells, the church would fit inside this
	// courtyard twice over. A castle at the same scale as those would have read
	// as one more of his places, which is exactly what it must not be.
	//
	// Sixty-one blocks across the curtain and a keep at thirty-two is beyond
	// what a person builds by hand in a survival world, and that is the whole
	// sentence: whatever put this here had help, or time, or both.
	private static final int WALL = 30;      // half-width of the curtain
	private static final int WALL_HEIGHT = 11;
	private static final int TOWER = 5;      // half-width of a corner tower
	private static final int TOWER_HEIGHT = 24;
	private static final int KEEP = 11;      // half-width of the keep
	private static final int KEEP_HEIGHT = 32;
	/**
	 * THE MOTTE. The castle stands ABOVE its town and is reached by steps.
	 *
	 * Fallen Kingdom opens on the king walking down out of his castle into the
	 * village below, and that shot is the whole relationship between the two
	 * halves of this build in one image. A castle on the same level as its town
	 * is a large building in a street; a castle six blocks up, behind a wall,
	 * with a stair down into the houses, is somebody who lives ABOVE the people
	 * who live there.
	 */
	private static final int MOTTE = 6;

	private static int tickCounter;

	/**
	 * Where it stands, for anything that has to keep away from it.
	 *
	 * The weather asks: a real bolt in the wood is the point of the place and a
	 * real bolt in the city is the city burning down, and the only way to tell
	 * those apart is to know where the city is.
	 *
	 * @return null before it has been sited, which is most of a world's life
	 */
	public static @org.jspecify.annotations.Nullable BlockPos site(ServerLevel his) {
		Long chosen = his.getAttached(SITE);
		return chosen == null ? null : BlockPos.of(chosen);
	}

	/**
	 * How far the whole works reaches — curtain, town, rampart, and a firebreak.
	 *
	 * THE MARGIN IS THE POINT AND TWELVE WAS NOT ENOUGH. Refusing to throw a real
	 * bolt inside the town does not stop the town burning; it stops it being LIT
	 * directly, and fire does not need to be lit directly. A strike at a hundred
	 * and one blocks with the rampart at eighty-eight had twelve blocks of forest
	 * to cross, and it gets thirty seconds before the first sweep.
	 *
	 * Twenty-four. Wide enough that nothing started outside it arrives before the
	 * rain and the sweep have both had a go, and narrow enough that the walk in
	 * from the landing still has weather in it — which is where the player spends
	 * the most time looking at this dimension.
	 */
	/** Whether the castle is actually standing, as opposed to merely chosen. */
	public static boolean raised(ServerLevel his) {
		return Boolean.TRUE.equals(his.getAttached(RAISED));
	}

	public static int reach() {
		return WALL + HisCity.REACH + 24;
	}

	/**
	 * A CHEST WHERE YOU COME OUT, WITH THE WAY TO THE CASTLE IN IT.
	 *
	 * The dimension has one landmark and it is eighty to a hundred blocks from the
	 * arrival in a random direction, through a dark forest, in permanent rain, with
	 * a garrison in it. That is a fair walk if you know where you are going and it
	 * is a coin toss if you do not — and the first thing a player does on stepping
	 * out of a portal is turn round on the spot looking for a reason to go
	 * anywhere.
	 *
	 * So the reason is on the floor in front of them. Same grammar as the overworld
	 * chain: you do not get told where things are, you FIND the note that says.
	 *
	 * SITED, NOT BUILT, is the timing and it is deliberate. This runs the instant
	 * the keep's position is chosen, which is the first tick anybody is over here —
	 * so the chest is waiting before they have finished loading in, and nobody
	 * watches it appear.
	 */
	private static void arrival(ServerLevel his, BlockPos came, BlockPos keep) {
		his.getChunk(came.getX() >> 4, came.getZ() >> 4);
		for (int ring = 1; ring <= 4; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					for (int dy = 0; dy >= -2; dy--) {
						BlockPos at = came.offset(dx, dy, dz);
						if (his.getBlockState(at).is(Blocks.CHEST)) {
							// AND ITS MAP IS REPLACED, not left to rot.
							//
							// This used to just return. Which was right while a site
							// was chosen once and never moved — and wrong the moment
							// onTick learned to throw a bad one away, because the
							// castle would move and the map in the chest would go on
							// pointing where it used to be. A chest that hands you a
							// wrong map is worse than a chest with nothing in it.
							if (his.getBlockEntity(at) instanceof net.minecraft.world
									.level.block.entity.ChestBlockEntity old) {
								old.setItem(13, theWay(his, keep));
								HerobrineMod.LOGGER.info(
									"the map at the crossing was redrawn for [{}, {}]",
									keep.getX(), keep.getZ());
							}
							return;
						}
						if (!his.getBlockState(at).isAir()
							|| !his.getBlockState(at.below()).isSolid()
							|| !his.getBlockState(at.above()).isAir()) {
							continue;
						}
						his.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
						his.setBlock(at.above(), Blocks.SOUL_LANTERN.defaultBlockState()
							.setValue(net.minecraft.world.level.block.state.properties
								.BlockStateProperties.HANGING, false), 3);
						if (!(his.getBlockEntity(at)
								instanceof net.minecraft.world.level.block.entity
									.ChestBlockEntity box)) {
							return;
						}
						box.setItem(13, theWay(his, keep));
						// And enough to walk there on. Not a haul — the city pays,
						// and a full chest at the door would mean never leaving it.
						com.bloomlet.herobrine.structure.Loot.scatter(box,
							his.getRandom(), com.bloomlet.herobrine.structure.Loot.Tier.TOWER);
						HerobrineMod.LOGGER.info(
							"the way to the keep was left at the crossing, [{}, {}, {}]",
							at.getX(), at.getY(), at.getZ());
						return;
					}
				}
			}
		}
		HerobrineMod.LOGGER.info("nowhere at the crossing to leave the way to the keep");
	}

	/**
	 * The map that says where the castle is, and it says so in WORDS as well.
	 *
	 * A MAP CANNOT BE CENTRED ON AN ARBITRARY POINT. MapItemSavedData.createFresh
	 * snaps the middle to a grid of 128 << scale — so asking for a map "of the
	 * keep" gets you a map of whichever standard tile the keep happens to fall in,
	 * and the keep can sit anywhere in it, including hard against an edge.
	 *
	 * At the scale this shipped with — 3, a thousand and twenty-four blocks across
	 * — that had two consequences, measured over twenty thousand random arrivals:
	 *
	 *     the red marker was silently dropped   3% of the time
	 *     the PLAYER'S OWN ARROW was off it    12% of the time
	 *
	 * The second is the one that hurt. With no arrow there is nothing to read the
	 * map against, so it stops being a direction and becomes a picture — and the
	 * only thing left to walk toward is the middle, which is a grid centre and can
	 * be five hundred blocks from anything. That is a map leading you to the wrong
	 * place, exactly as reported, while the castle stood where it was supposed to.
	 *
	 * Scale 2 rather than 3: four blocks a pixel instead of eight, so ninety blocks
	 * is a legible run across the sheet rather than eleven pixels of nothing.
	 *
	 * AND THE COORDINATES GO IN THE NAME, which is the part that cannot fail. Every
	 * bound above is a percentage; a number on the item is not. If the marker is
	 * dropped and the arrow is off the edge and the whole sheet is blank, the thing
	 * still says where to go, and it says it in the tooltip before the map is even
	 * opened.
	 */
	private static net.minecraft.world.item.ItemStack theWay(ServerLevel his, BlockPos keep) {
		net.minecraft.world.item.ItemStack map = net.minecraft.world.item.MapItem
			.create(his, keep.getX(), keep.getZ(), (byte) 2, true, true);
		net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
			map, keep, "+", net.minecraft.world.level.saveddata.maps
				.MapDecorationTypes.RED_MARKER);
		map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal(
				"he is building something — " + keep.getX() + ", " + keep.getZ()));
		return map;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Keep::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!Config.get().enabled || !Config.get().hisKeep) {
			return;
		}
		ServerLevel his = server.getLevel(TheWayBlock.HIS_WORLD);
		if (his == null || his.players().isEmpty()
			|| Boolean.TRUE.equals(his.getAttached(RAISED))) {
			return;
		}

		Long chosen = his.getAttached(SITE);
		if (chosen == null) {
			// Sited from wherever the first person came out, which is the only
			// fixed point this dimension has.
			ServerPlayer first = his.players().get(0);
			BlockPos site = pick(his, first.blockPosition());
			his.setAttached(SITE, site.asLong());
			HerobrineMod.LOGGER.info("the keep will stand at [{}, {}]",
				site.getX(), site.getZ());
			arrival(his, first.blockPosition(), site);
			return;
		}

		BlockPos site = BlockPos.of(chosen);

		// AND A SITE FROM THE BROKEN VERSION IS THROWN AWAY.
		//
		// SITE is written once and never re-rolled, so a world that chose its
		// bearing before pick() generated its candidates is stuck with a height
		// that was never a fact about the ground — it was whatever Y the first
		// person happened to be standing at when they stepped out of the portal.
		// No amount of correctness from here on moves it, and the symptom is both
		// the castle AND him missing, because overTheKeep hangs off this same Y.
		//
		// Same shape as the repair in Whereabouts.overTheKeep: ask the cheap
		// question once a couple of seconds and fix it the first time somebody is
		// over there. Only ever runs before RAISED — a keep already standing is
		// never moved, whatever its Y says.
		his.getChunk(site.getX() >> 4, site.getZ() >> 4);
		int ground = Ground.topOf(his, site.getX(), site.getZ());
		if (Math.abs(site.getY() - ground) > SLIPPED) {
			his.setAttached(SITE, null);
			HerobrineMod.LOGGER.warn(
				"the keep was sited at Y {} where the ground is {} — forgetting it,"
					+ " it will be chosen again", site.getY(), ground);
			return;
		}

		for (ServerPlayer player : his.players()) {
			if (player.blockPosition().closerThan(site, RAISE_RANGE)) {
				raise(his, site);
				his.setAttached(RAISED, true);
				return;
			}
		}
	}

	/**
	 * A bearing and a distance, derived rather than rolled.
	 *
	 * Hashed off the arrival coordinates so it is the same answer every time it
	 * is asked. Rolling it would mean two players who arrive a minute apart on a
	 * server could each site a different castle, and the second one would be
	 * standing somewhere the first had already walked.
	 */
	private static BlockPos pick(ServerLevel his, BlockPos from) {
		// SEVERAL BEARINGS, AND THE FIRST DRY ONE WINS.
		//
		// One hashed bearing was enough while nothing checked what was under it,
		// and what was under it was sometimes the sea. His world runs on the
		// OVERWORLD noise settings — that is what gives it hills, caves and a
		// forest floor worth walking on — and the same settings give it oceans,
		// which the fixed dark-forest biome does nothing whatever to remove.
		//
		// So the site is re-rolled deterministically until it finds land. Each
		// attempt re-hashes rather than shifting the same value, because bits
		// pulled out of one hash at neighbouring offsets are correlated and the
		// "different" bearings would have clustered.
		BlockPos fallback = null;
		for (int attempt = 0; attempt < 16; attempt++) {
			long h = (from.getX() * 341873128712L + from.getZ() * 132897987541L
				+ attempt * 6364136223846793005L);
			h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
			h = h ^ (h >>> 32);
			double angle = ((h >>> 12) & 0xFFFF) / 65536.0 * Math.PI * 2.0;
			double range = NEAR + ((h >>> 28) & 0xFF) / 255.0 * (FAR - NEAR);
			int x = from.getX() + (int)Math.round(Math.cos(angle) * range);
			int z = from.getZ() + (int)Math.round(Math.sin(angle) * range);
			BlockPos at = new BlockPos(x, from.getY(), z);
			if (fallback == null) {
				fallback = at;
			}
			// GENERATED, NOT SKIPPED, and this is the whole bug.
			//
			// It used to skip any bearing that was not already loaded — and this
			// runs on the FIRST tick anybody stands in the dimension, at eighty to
			// a hundred blocks out, while the chunks around a fresh portal are
			// still catching up. So all sixteen bearings skipped, every time,
			// and it fell through to the fallback.
			//
			// The fallback is an unchecked bearing carrying from.getY() — THE
			// PLAYER'S OWN Y. And SITE is written once and never re-rolled, so that
			// wrong height is permanent for the life of the world. The castle goes
			// somewhere nobody can reach and overTheKeep puts him at siteY + 24,
			// which is inside the rock. Both of them missing, from one line.
			his.getChunk(x >> 4, z >> 4);
			if (Ground.dry(his, x, z)) {
				return highest(his, at);
			}
		}
		// Sixteen bearings and every one of them wet, which means the arrival
		// landed on a coast or an island. Better a castle with its feet in the
		// water than no castle at all — and it is logged, because if this ever
		// fires in practice the answer is a wider search rather than a shrug.
		HerobrineMod.LOGGER.warn("no dry ground for the keep within {}–{} of [{}, {}]",
			NEAR, FAR, from.getX(), from.getZ());
		// AND EVEN THE GIVING-UP ANSWER GETS ITS HEIGHT OFF THE GROUND.
		//
		// It used to hand back a position still carrying the player's Y, which is
		// the height somebody happened to be standing at when they stepped out of a
		// portal. That is not a fact about the world. Feet in the water is an
		// acceptable castle; a castle at the arrival's altitude is not a castle.
		BlockPos wet = fallback == null ? from : fallback;
		his.getChunk(wet.getX() >> 4, wet.getZ() >> 4);
		return new BlockPos(wet.getX(),
			Ground.topOf(his, wet.getX(), wet.getZ()), wet.getZ());
	}

	/**
	 * AND IT TAKES THE HIGH GROUND, WHICH IT WAS NOT DOING.
	 *
	 * The bearing and distance were hashed and then the castle was simply dropped
	 * wherever that landed. A castle does not get built wherever the surveyor
	 * stopped walking — it gets built on the rise, because that is the entire
	 * point of a castle, and every real one the player has ever seen a picture of
	 * is on one.
	 *
	 * It also matters for finding it. The towers are the only landmark in a wood
	 * with no sightlines, and a castle sitting in a dip has the canopy on the
	 * ridge around it standing directly between the player and the light it is
	 * supposed to be found by.
	 *
	 * A FIXED GRID RATHER THAN A ROLL, so this stays deterministic: the same
	 * arrival gives the same answer, on a server and after a restart. Twenty-five
	 * samples across a forty-eight-block box, and the highest DRY one wins.
	 *
	 * Anything not loaded is skipped rather than waited for. Reading a heightmap
	 * out of an ungenerated chunk drags worldgen onto the server thread, and this
	 * runs the instant somebody steps through the portal — which is the worst
	 * possible moment to stall.
	 */
	private static BlockPos highest(ServerLevel his, BlockPos around) {
		BlockPos best = around;
		int top = Integer.MIN_VALUE;
		for (int dx = -24; dx <= 24; dx += 12) {
			for (int dz = -24; dz <= 24; dz += 12) {
				BlockPos at = around.offset(dx, 0, dz);
				// Same again: skipping unloaded samples here left `best` as the
				// centre it was handed, Y and all, which is how the player's height
				// survived even when pick() found dry land.
				his.getChunk(at.getX() >> 4, at.getZ() >> 4);
				if (!Ground.dry(his, at.getX(), at.getZ())) {
					continue;
				}
				int y = Ground.topOf(his, at.getX(), at.getZ());
				if (y > top) {
					top = y;
					best = at;
				}
			}
		}
		// AND THE Y COMES BACK OFF THE GROUND, never off whatever was passed in.
		// If every sample was wet, `best` is still the centre — which used to mean
		// the caller's Y went straight through untouched.
		return top == Integer.MIN_VALUE
			? new BlockPos(best.getX(),
				Ground.topOf(his, best.getX(), best.getZ()), best.getZ())
			: best.atY(top);
	}

	// ---- THE BUILD ---------------------------------------------------------
	/**
	 * SPREAD ACROSS TICKS, because this is a quarter of a million blocks.
	 *
	 * A curtain sixty-one across, a keep thirty-two high and a city outside it
	 * is far past what can be placed in one tick without the server visibly
	 * stopping — and a freeze on arrival in the last chapter is the worst
	 * possible first impression of the best thing in the mod.
	 *
	 * So it goes up in stages, a few ticks apart, in the order somebody would
	 * actually have built it: the ground, then the walls, then the towers, then
	 * the gate, then the keep, and the city last. Nobody is standing in it while
	 * this happens — it fires at a hundred and forty-four blocks, well beyond
	 * sight in this fog — so the staging is invisible and costs nothing.
	 */
	private static void raise(ServerLevel his, BlockPos site) {
		RandomSource random = his.getRandom();
		// LEVELLED OFF THE MIDDLE, not off each column. A castle that follows
		// the ground is a wall with a wobble in it, and the whole claim this
		// building makes is that whoever put it up could build.
		int floor = Ground.topOf(his, site.getX(), site.getZ()) + 1 + MOTTE;
		BlockPos base = new BlockPos(site.getX(), floor, site.getZ());
		MinecraftServer server = his.getServer();

		stage(server, 0, () -> ground(his, base, random));
		stage(server, 6, () -> curtain(his, base, random));
		stage(server, 12, () -> {
			for (int sx : new int[] { -1, 1 }) {
				for (int sz : new int[] { -1, 1 }) {
					tower(his, base.offset(sx * WALL, 0, sz * WALL), random);
				}
			}
		});
		stage(server, 16, () -> {
			gate(his, base);
			steps(his, base, random);
		});
		stage(server, 22, () -> keep(his, base, random));
		stage(server, 26, () -> Remembering.furnish(his, base, KEEP, KEEP_HEIGHT, random));
		stage(server, 30, () -> HisCity.raise(his, base, WALL, MOTTE, random));

		HerobrineMod.LOGGER.info("the keep is going up at [{}, {}, {}]",
			base.getX(), base.getY(), base.getZ());
	}

	private static void stage(MinecraftServer server, int delay, Runnable work) {
		com.bloomlet.herobrine.manifest.Cadence.in(server, delay, work);
	}

	/**
	 * ONLY WRITE WHAT IS DIFFERENT.
	 *
	 * The clearing pass alone walks sixty-one by sixty-one by fourteen, and the
	 * overwhelming majority of that is already air above the treetops. setBlock
	 * on a block that is already what you are setting it to still costs a
	 * neighbour update, a lighting update and a chunk section dirty flag; the
	 * comparison is a hash lookup. On a build this size that difference is the
	 * difference between a pause and a stall.
	 */
	private static void put(ServerLevel his, BlockPos at, BlockState state) {
		if (!his.getBlockState(at).equals(state)) {
			his.setBlock(at, state, 2);
		}
	}

	/**
	 * THE GREAT STAIR, down off the motte and into the town.
	 *
	 * The one shot Fallen Kingdom opens on. It is worth building for that alone,
	 * and it also does the practical job the motte creates: without it the
	 * castle is six blocks up with no way down that is not a jump.
	 */
	private static void steps(ServerLevel his, BlockPos base, RandomSource random) {
		for (int step = 0; step <= MOTTE + 2; step++) {
			int z = WALL + 1 + step * 2;
			int y = -step;
			for (int dx = -4; dx <= 4; dx++) {
				put(his, base.offset(dx, y, z),
					Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
						.setValue(BlockStateProperties.HALF, Half.BOTTOM));
				put(his, base.offset(dx, y, z + 1), paving(random));
				for (int dy = 1; dy <= 3; dy++) {
					clear(his, base.offset(dx, y + dy, z));
					clear(his, base.offset(dx, y + dy, z + 1));
				}
				for (int dy = -1; dy >= -4; dy--) {
					fill(his, base.offset(dx, y + dy, z), stone(random));
					fill(his, base.offset(dx, y + dy, z + 1), stone(random));
				}
			}
			// A balustrade, so it reads as a stair rather than a ramp.
			for (int dx : new int[] { -5, 5 }) {
				put(his, base.offset(dx, y, z), stone(random));
				put(his, base.offset(dx, y, z + 1), stone(random));
				put(his, base.offset(dx, y + 1, z),
					Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
			}
		}
	}

	private static void clear(ServerLevel his, BlockPos at) {
		if (!his.getBlockState(at).isAir()) {
			his.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void fill(ServerLevel his, BlockPos at, BlockState state) {
		if (!his.getBlockState(at).isSolid()) {
			put(his, at, state);
		}
	}

	/**
	 * The ground it stands on, cut flat and filled under.
	 *
	 * Both directions, because a dark forest is neither flat nor kind: the
	 * courtyard is cleared upward to the height of the wall so no tree is left
	 * growing through the parade ground, and filled downward so the wall does
	 * not stand on stilts over a dip.
	 */
	private static void ground(ServerLevel his, BlockPos base, RandomSource random) {
		for (int dx = -WALL - TOWER; dx <= WALL + TOWER; dx++) {
			for (int dz = -WALL - TOWER; dz <= WALL + TOWER; dz++) {
				if (Math.abs(dx) > WALL || Math.abs(dz) > WALL) {
					continue;
				}
				for (int dy = 0; dy <= WALL_HEIGHT + 2; dy++) {
					clear(his, base.offset(dx, dy, dz));
				}
				put(his, base.offset(dx, -1, dz), paving(random));
				// THE MOTTE ITSELF. Filled down to whatever the forest floor is
				// doing under it, so the castle stands on a mound rather than on
				// a slab hanging over a dip. Batter the edge by a block per
				// course, which is what stops it reading as a cardboard box.
				int edge = Math.max(Math.abs(dx), Math.abs(dz));
				int down = MOTTE + 8 + (WALL - edge > 3 ? 0 : 4);
				for (int dy = -2; dy >= -down; dy--) {
					BlockPos under = base.offset(dx, dy, dz);
					if (his.getBlockState(under).isSolid()) {
						continue;
					}
					fill(his, under, dy > -MOTTE - 2
						? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
						: Blocks.DEEPSLATE.defaultBlockState());
				}
			}
		}
	}

	/** The curtain, battlemented, with an arrow slit every eight. */
	private static void curtain(ServerLevel his, BlockPos base, RandomSource random) {
		for (int dx = -WALL; dx <= WALL; dx++) {
			for (int dz = -WALL; dz <= WALL; dz++) {
				if (Math.abs(dx) != WALL && Math.abs(dz) != WALL) {
					continue;
				}
				for (int dy = 0; dy <= WALL_HEIGHT; dy++) {
					BlockPos at = base.offset(dx, dy, dz);
					if (dy == WALL_HEIGHT) {
						// The crenellations. Every other block, so it reads as a
						// wall somebody stands behind rather than a straight
						// line with a texture on it.
						boolean merlon = ((dx + dz) & 1) == 0;
						put(his, at, merlon
							? stone(random)
							: Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState()
								.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
						continue;
					}
					put(his, at, stone(random));
				}
				// Arrow slits, at head height on the wall walk.
				if ((dx + dz) % 8 == 0 && Math.abs(dx) + Math.abs(dz) > WALL) {
					put(his, base.offset(dx, WALL_HEIGHT - 3, dz),
						Blocks.AIR.defaultBlockState());
				}
			}
		}
		// The wall walk itself, one course below the battlements and one in.
		for (int dx = -WALL + 1; dx <= WALL - 1; dx++) {
			for (int dz = -WALL + 1; dz <= WALL - 1; dz++) {
				if (Math.abs(dx) != WALL - 1 && Math.abs(dz) != WALL - 1) {
					continue;
				}
				put(his, base.offset(dx, WALL_HEIGHT - 1, dz),
					Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
		}
	}

	/**
	 * A corner tower, and the fire on top of it is the whole navigation system.
	 *
	 * Soul fire rather than an ordinary flame, because in a dimension where
	 * every light on the ground is orange, the four blue ones above the canopy
	 * are the only thing that could not be a lightning strike or a burning tree.
	 * They are also the one thing here that is genuinely helpful, which is worth
	 * having exactly once.
	 */
	private static void tower(ServerLevel his, BlockPos at, RandomSource random) {
		for (int dx = -TOWER; dx <= TOWER; dx++) {
			for (int dz = -TOWER; dz <= TOWER; dz++) {
				boolean wall = Math.abs(dx) == TOWER || Math.abs(dz) == TOWER;
				for (int dy = -1; dy <= TOWER_HEIGHT; dy++) {
					BlockPos pos = at.offset(dx, dy, dz);
					if (dy == TOWER_HEIGHT) {
						put(his, pos, wall && ((dx + dz) & 1) == 0
							? stone(random)
							: Blocks.AIR.defaultBlockState());
					} else if (dy == TOWER_HEIGHT - 1) {
						put(his, pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
					} else if (wall || dy == -1) {
						put(his, pos, stone(random));
					} else {
						clear(his, pos);
					}
				}
			}
		}
		BlockPos brazier = at.above(TOWER_HEIGHT);
		put(his, brazier, Blocks.SOUL_CAMPFIRE.defaultBlockState());
	}

	/**
	 * The gate, and it is standing open.
	 *
	 * Open rather than barred, and that is the single most important decision in
	 * this building. A locked castle is a puzzle and the player starts looking
	 * for the key; an open one is an invitation, and an invitation is far worse —
	 * whoever lives here is not worried about anybody walking in.
	 *
	 * The portcullis is up, in its slot, where anybody can see it could come
	 * down.
	 */
	private static void gate(ServerLevel his, BlockPos base) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				boolean arch = Math.abs(dx) == 2 && dy == 4 || dy == 4 && Math.abs(dx) < 2;
				BlockPos at = base.offset(dx, dy, WALL);
				if (arch) {
					put(his, at, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
				} else if (Math.abs(dx) <= 2 && dy < 4) {
					clear(his, at);
				}
			}
			// The portcullis, raised into the arch.
			put(his, base.offset(dx, 5, WALL), Blocks.IRON_BARS.defaultBlockState());
		}
		for (int dx : new int[] { -3, 3 }) {
			put(his, base.offset(dx, 3, WALL), Blocks.SOUL_LANTERN.defaultBlockState());
		}
	}

	/**
	 * The keep, hollow, with a stair to the top.
	 *
	 * Deliberately empty inside. What is in it is the next thing to build, and
	 * an empty keep with a stair in it is a far better placeholder than a
	 * furnished one — a player who climbs it and finds nothing has found nothing,
	 * which is honest, where a player who finds decorative barrels has been told
	 * the building is finished.
	 */
	private static void keep(ServerLevel his, BlockPos base, RandomSource random) {
		for (int dx = -KEEP; dx <= KEEP; dx++) {
			for (int dz = -KEEP; dz <= KEEP; dz++) {
				boolean wall = Math.abs(dx) == KEEP || Math.abs(dz) == KEEP;
				for (int dy = 0; dy <= KEEP_HEIGHT; dy++) {
					BlockPos at = base.offset(dx, dy, dz);
					if (dy == KEEP_HEIGHT) {
						put(his, at, wall && ((dx + dz) & 1) == 0
							? stone(random)
							: Blocks.AIR.defaultBlockState());
					} else if (dy == KEEP_HEIGHT - 1) {
						put(his, at, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
					} else if (wall) {
						put(his, at, stone(random));
					} else {
						clear(his, at);
					}
				}
				put(his, base.offset(dx, -1, dz),
					Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
		}
		// The door, facing the gate.
		for (int dy = 0; dy <= 2; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				clear(his, base.offset(dx, dy, KEEP));
			}
		}
		// Narrow windows up the front, so it is not a slab with a hole in it.
		for (int dy = 5; dy < KEEP_HEIGHT - 2; dy += 4) {
			for (int dx : new int[] { -3, 3 }) {
				clear(his, base.offset(dx, dy, KEEP));
				clear(his, base.offset(dx, dy + 1, KEEP));
			}
		}
		stair(his, base, random);
		for (int dy = 4; dy < KEEP_HEIGHT; dy += 5) {
			for (int dx : new int[] { -KEEP + 1, KEEP - 1 }) {
				put(his, base.offset(dx, dy, 0), Blocks.SOUL_LANTERN.defaultBlockState()
					.setValue(BlockStateProperties.HANGING, false));
			}
		}
	}

	/** A spiral up the inside wall, because a ladder is not architecture. */
	private static void stair(ServerLevel his, BlockPos base, RandomSource random) {
		int[][] ring = { {1, 0}, {0, 1}, {-1, 0}, {0, -1} };
		int side = KEEP - 1;
		int step = 0;
		for (int dy = 0; dy < KEEP_HEIGHT - 1; dy++) {
			// Round the inside of the shell, one block per course.
			int leg = (dy / (side * 2)) % 4;
			int along = dy % (side * 2) - side;
			int[] face = ring[leg];
			int x = face[0] != 0 ? face[0] * side : along;
			int z = face[1] != 0 ? face[1] * side : along;
			BlockPos at = base.offset(x, dy, z);
			put(his, at, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
				.setValue(BlockStateProperties.HALF, Half.BOTTOM));
			step++;
		}
		HerobrineMod.LOGGER.debug("keep stair: {} steps", step);
	}

	private static BlockState stone(RandomSource random) {
		int roll = random.nextInt(12);
		if (roll < 2) {
			return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
		}
		if (roll < 4) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}
		return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	}

	private static BlockState paving(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 3) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}
		if (roll < 5) {
			return Blocks.DEEPSLATE_TILES.defaultBlockState();
		}
		return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	}
}
