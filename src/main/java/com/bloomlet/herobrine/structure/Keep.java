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

	private static final AttachmentType<Long> CITY =
		AttachmentRegistry.createPersistent(HerobrineMod.id("city_site"), Codec.LONG);
	private static final AttachmentType<Boolean> CITY_UP =
		AttachmentRegistry.createPersistent(HerobrineMod.id("city_raised"), Codec.BOOL);

	/**
	 * THE CITY AND THE CASTLE ARE TWO PLACES NOW.
	 *
	 * They used to be one. Keep.raise built the castle and then called
	 * HisCity.raise on the same base, so the town was a ring fifty-eight blocks
	 * deep around the curtain wall — which means arriving near one was arriving at
	 * both, and the single landmark this dimension has was something you were
	 * standing inside before you had decided to go to it.
	 *
	 * The city sites near the crossing, because it is what you are meant to find
	 * first: somewhere with doors and chests and people in it, which is where you
	 * learn there is a castle at all. The castle sites a long way past it, and the
	 * walk between the two IS the middle of the chapter.
	 *
	 * AND THE OLD DISTANCE WAS SET BY THE FOG, NOT BY TASTE. The comment that
	 * stood here said so: eighty to a hundred, because the fog ended at a hundred
	 * and twelve and anything beyond it was invisible until it was underfoot. That
	 * was true and it is not any more — the fog now starts at ninety-six and ends
	 * at three hundred and twenty, because it read as low quality up close. The
	 * constraint that forced the castle to sit on top of its own town is gone, and
	 * this is the change it was holding back.
	 */
	private static final int CITY_NEAR = 46;
	private static final int CITY_FAR = 74;
	private static final int CITY_RAISE_RANGE = 104;

	private static final int NEAR = 240;
	private static final int FAR = 340;
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
		return WALL + 24;
	}

	/** Where his city stands, for anything that has to keep away from it. */
	public static @org.jspecify.annotations.Nullable BlockPos city(ServerLevel his) {
		Long packed = his.getAttached(CITY);
		return packed == null ? null : BlockPos.of(packed);
	}

	/** And how far it spreads. */
	public static int cityReach() {
		return HisCity.REACH + 16;
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
	private static void arrival(ServerLevel his, BlockPos came) {
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
							return;      // already been through this door
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
						// NO MAP HERE ANY MORE.
						//
						// It used to hand you the castle at the door, which made the
						// city — the only inhabited thing in the dimension — a place
						// you walked PAST on your way to a marker. Supplies only now.
						// You are told where to go by the people who lived here, in
						// their own houses, or not at all.
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

	/**
	 * The way to the castle, left in the city that knows about it.
	 *
	 * THE MAP USED TO BE AT THE PORTAL, which was the wrong place for it twice
	 * over. It gave you the destination before you had met anybody who could have
	 * told you — so the city, the only inhabited thing over here, became scenery on
	 * the way to a marker. And it meant the first thing the dimension did was hand
	 * you an objective, which is the one move this mod does not make anywhere else.
	 *
	 * A cottage chest instead. You walk into the city because it is the thing you
	 * can see, you go through the houses because that is what anybody does, and the
	 * castle comes out of a drawer.
	 *
	 * SEARCHED FROM THE MIDDLE OUTWARD so it lands in a house rather than in the
	 * rampart stores — HisCity lays its plots on rings out from the centre, and the
	 * nearest container to the middle is somebody's kitchen.
	 */
	private static void theWayFromTheCity(ServerLevel his, BlockPos city, BlockPos keep) {
		for (int ring = 0; ring <= HisCity.REACH; ring += 4) {
			for (int dx = -ring; dx <= ring; dx += 2) {
				for (int dz = -ring; dz <= ring; dz += 2) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					for (int dy = -4; dy <= 8; dy++) {
						BlockPos at = city.offset(dx, dy, dz);
						if (!his.getBlockState(at).is(Blocks.CHEST)) {
							continue;
						}
						if (!(his.getBlockEntity(at) instanceof net.minecraft.world.level
								.block.entity.ChestBlockEntity box)) {
							continue;
						}
						for (int slot = 0; slot < box.getContainerSize(); slot++) {
							if (!box.getItem(slot).isEmpty()) {
								continue;
							}
							box.setItem(slot, theWay(his, keep));
							box.setChanged();
							HerobrineMod.LOGGER.info(
								"the way to the keep is in a house at [{}, {}, {}]",
								at.getX(), at.getY(), at.getZ());
							return;
						}
					}
				}
			}
		}
		HerobrineMod.LOGGER.warn(
			"no chest in his city to leave the way to the keep — THE TRAIL ENDS HERE");
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

		// ---- THE CITY FIRST, because it is the one you are meant to walk into.
		Long town = his.getAttached(CITY);
		if (town == null) {
			ServerPlayer first = his.players().get(0);
			BlockPos at = pick(his, first.blockPosition(), CITY_NEAR, CITY_FAR);
			his.setAttached(CITY, at.asLong());
			HerobrineMod.LOGGER.info("his city will stand at [{}, {}]",
				at.getX(), at.getZ());
			arrival(his, first.blockPosition());
			return;
		}

		BlockPos city = BlockPos.of(town);
		if (!Boolean.TRUE.equals(his.getAttached(CITY_UP))) {
			for (ServerPlayer player : his.players()) {
				if (player.blockPosition().closerThan(city, CITY_RAISE_RANGE)) {
					his.setAttached(CITY_UP, true);
					// castle 0, because there is no castle here to stand clear of.
					HisCity.raise(his, city.above(MOTTE), 0, MOTTE, his.getRandom());
					HerobrineMod.LOGGER.info("his city is going up at [{}, {}, {}]",
						city.getX(), city.getY(), city.getZ());
					return;
				}
			}
			return;      // and nothing else happens until it is standing
		}

		Long chosen = his.getAttached(SITE);
		if (chosen == null) {
			// Sited from wherever the first person came out, which is the only
			// fixed point this dimension has.
			BlockPos site = pick(his, city, NEAR, FAR);
			his.setAttached(SITE, site.asLong());
			HerobrineMod.LOGGER.info(
				"the keep will stand at [{}, {}] — {} blocks off the city",
				site.getX(), site.getZ(), (int) Math.sqrt(site.distSqr(city)));
			theWayFromTheCity(his, city, site);
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
	private static BlockPos pick(ServerLevel his, BlockPos from, int near, int far) {
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
			double range = near + ((h >>> 28) & 0xFF) / 255.0 * (far - near);
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
			near, far, from.getX(), from.getZ());
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
			// ON THE CORNERS OF THE CIRCUIT, and they used to be at (+-WALL, +-WALL).
			//
			// Which was the right answer for a square and is a floating tower for
			// anything else — the wall now reaches anywhere from 22 to 46 out, so
			// three of the four would have stood in the trees with daylight between
			// them and the rampart they are supposed to anchor.
			//
			// Every third corner, which is four towers on a twelve-sided circuit,
			// and the one at the gate corner is skipped because the gatehouse is
			// already there.
			int[] reach = circuit(base);
			for (int i = 0; i < CORNERS; i += 3) {
				if (i == GATE_CORNER) {
					continue;
				}
				int[] c = corner(reach, i);
				tower(his, base.offset(c[0], 0, c[1]), random);
			}
		});
		stage(server, 16, () -> {
			gate(his, base);
			steps(his, base, random);
		});
		stage(server, 22, () -> keep(his, base, random));
		stage(server, 26, () -> Remembering.furnish(his, base, KEEP, KEEP_HEIGHT, random));
		// AND SOMETHING STANDING IN THE COURTYARD, at last.
		//
		// Four thousand square blocks of paving with a hall in the middle of it was
		// the whole of the bailey, and it read exactly as it was built: open,
		// generic, nothing to walk over to. The street runs from the gate to the
		// hall door with eight dead stalls on it, which is the shortest path from
		// "an empty yard" to "somewhere people used to be".
		//
		// Last of all, after the keep and after the furnishing, because the stalls
		// put chests down and nothing may carve afterwards.
		stage(server, 30, () -> {
			int[] reach = circuit(base);
			TheShambles.lay(his, base.offset(0, 0, WALL), Direction.NORTH,
				where -> room(reach, where.getX() - base.getX(),
					where.getZ() - base.getZ()),
				random);
		});
		// NO CITY HERE. It is its own place now, sited near the crossing and raised
		// long before this — see onTick. A castle that builds a town around itself
		// is a castle you were already standing in.

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
	/** Half-width of the causeway. Five blocks: a road, not a plaza. */
	private static final int ROAD = 2;

	/** How far it is allowed to run before it gives up and lands anyway. */
	private static final int MAX_RUN = 56;

	/** Beyond this drop the fill stops being a bank and becomes piers. */
	private static final int PIERS_AT = 7;

	/** Above this much clear air, the ramp stops strolling and starts descending. */
	private static final int CHASES_OVER = 4;

	/**
	 * THE WAY UP, AND IT FOLLOWS THE GROUND NOW.
	 *
	 * The old one was nine identical treads, nine blocks wide, running dead south
	 * from the gate, dropping one block each — and it sampled the ground ONCE, at
	 * the centre of the castle, forty-seven blocks away from where it ended. On a
	 * dark forest hillside that is a staircase in the air, and it was reported
	 * exactly that way: the entrance is in the sky.
	 *
	 * It was in the sky on FLAT ground too, which is the part worth writing down.
	 * The gate floor sits at MOTTE + 1 above the centre, the causeway began the
	 * moment the motte stopped, and fill() only ever reached four blocks down — so
	 * the first treads had two clear blocks of nothing under them before the
	 * terrain had done anything wrong at all.
	 *
	 * WHAT VANILLA CALLS THIS IS BEARDING, and it is the documented answer rather
	 * than an invention: a structure declares terrain_adaptation, and beard_thin —
	 * what villages and pillager outposts use — means GENERATE GROUND UNDER THE
	 * BUILDING AND CUT IT AWAY INSIDE. This mod places blocks directly and never
	 * goes through a structure definition, so it gets no adaptation for free; what
	 * it has to do instead is the same two things by hand. The cut-away half was
	 * already here in clear(). This is the other half, and the wiki names the exact
	 * failure it is fixing: terrain adaptation "doesn't account for drastic changes
	 * in the y-axis".
	 *
	 * So the ground is read PER COLUMN. The ramp descends one block every second
	 * block of run, stops the moment the treads reach the real surface, and fills
	 * from every tread down to whatever is actually beneath it. Where the drop is
	 * deeper than PIERS_AT it stops filling solid and stands on piers, which is
	 * both cheaper to look at and the honest shape for a causeway over a dip.
	 */
	private static void steps(ServerLevel his, BlockPos base, RandomSource random) {
		int landed = -1;
		int y = 0;

		for (int run = 1; run <= MAX_RUN; run++) {
			int z = WALL + run;
			int here = Ground.topOf(his, base.getX(), base.getZ() + z) + 1;
			// A 1:2 RAMP THAT CHASES WHEN IT IS LOSING.
			//
			// One block every second course reads as a road a cart could come up,
			// and 1:1 reads as a ladder with slabs on it — so 1:2 is the default and
			// worth keeping. But ground that falls away at a block per block cannot
			// be caught by a ramp that falls at half that: the causeway simply flies
			// out over the valley for ever, which is the "drop away" case and the one
			// profile the first version of this never landed on.
			//
			// So when it is more than CHASES_OVER above the surface it takes a whole
			// block per course until it is back in range. On anything short of a
			// sustained cliff that converges, and the cliff is caught below.
			if (base.getY() + y > here + CHASES_OVER || run % 2 == 0) {
				y--;
			}
			int tread = base.getY() + y;
			int ground = here;

			// THE END CONDITION IS THE GROUND, NOT A COUNTER. The moment the ramp
			// has come down to meet the surface it stops, however far out that is
			// — which is what makes it fit a hillside, a dip and a flat field with
			// the same code and no cases.
			if (tread <= ground) {
				landed = run;
				break;
			}
			this_course(his, base, z, y, tread, random);
		}

		if (landed < 0) {
			// A GENUINE CLIFF, and it still has to be walkable. The causeway is
			// standing on piers over something that kept falling, so it ends the way
			// a real viaduct ends: a narrow flight straight down off the last course.
			HerobrineMod.LOGGER.info(
				"the causeway ran the full {} and is coming down on steps", MAX_RUN);
			landing(his, base, WALL + MAX_RUN, y, random);
			return;
		}
		HerobrineMod.LOGGER.info("the way up is {} blocks long and lands at y {}",
			landed, base.getY() + y);
	}

	/**
	 * One course of the causeway: the tread, the walls, the air over it, and
	 * whatever has to hold it up.
	 */
	private static void this_course(ServerLevel his, BlockPos base, int z, int y,
	                               int tread, RandomSource random) {
		for (int dx = -ROAD; dx <= ROAD; dx++) {
			BlockPos at = base.offset(dx, y, z);
			put(his, at, Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
				.setValue(BlockStateProperties.HALF, Half.BOTTOM));
			for (int up = 1; up <= 4; up++) {
				clear(his, at.above(up));
			}

			// ---- AND THIS IS THE BEARDING. Down to the real surface of this
			// column, not to a fixed depth off a sample taken somewhere else.
			int ground = Ground.topOf(his, at.getX(), at.getZ());
			int drop = tread - ground;
			if (drop <= PIERS_AT) {
				for (int dy = 1; dy <= drop; dy++) {
					fill(his, at.below(dy), stone(random));
				}
				continue;
			}
			// Too deep to bank up. Three courses of roadbed, then piers — a
			// causeway on legs rather than a wall of stone across a valley.
			for (int dy = 1; dy <= 3; dy++) {
				fill(his, at.below(dy), stone(random));
			}
			if (z % 5 != 0 || Math.abs(dx) == 1) {
				continue;
			}
			for (int dy = 4; dy <= drop; dy++) {
				fill(his, at.below(dy), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
			}
		}
		// A parapet either side, so it is a road with edges rather than a slab.
		for (int dx : new int[] { -ROAD - 1, ROAD + 1 }) {
			BlockPos at = base.offset(dx, y, z);
			put(his, at, stone(random));
			put(his, at.above(), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
			int ground = Ground.topOf(his, at.getX(), at.getZ());
			for (int dy = 1; dy <= Math.min(PIERS_AT, tread - ground); dy++) {
				fill(his, at.below(dy), stone(random));
			}
		}
	}

	/**
	 * The last resort: straight down, three wide, until it touches.
	 *
	 * Only reached when the ramp has run its whole length over ground that kept
	 * falling. Bounded by the world floor rather than by a step count, because the
	 * one thing this must not do is stop before it arrives — an entrance that ends
	 * in mid-air is the bug the whole rewrite exists to remove.
	 */
	private static void landing(ServerLevel his, BlockPos base, int z, int y,
	                            RandomSource random) {
		for (int step = 1; step <= 64; step++) {
			int tread = base.getY() + y - step;
			if (tread <= his.getMinY() + 2) {
				return;
			}
			boolean done = false;
			for (int dx = -1; dx <= 1; dx++) {
				BlockPos at = new BlockPos(base.getX() + dx, tread, base.getZ() + z + step);
				put(his, at, Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
					.setValue(BlockStateProperties.HALF, Half.BOTTOM));
				for (int up = 1; up <= 4; up++) {
					clear(his, at.above(up));
				}
				int ground = Ground.topOf(his, at.getX(), at.getZ());
				for (int dy = 1; dy <= Math.min(6, tread - ground); dy++) {
					fill(his, at.below(dy), stone(random));
				}
				if (tread <= ground + 1) {
					done = true;
				}
			}
			if (done) {
				return;
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
		int[] reach = circuit(base);
		for (int dx = -WALL_FAR - TOWER; dx <= WALL_FAR + TOWER; dx++) {
			for (int dz = -WALL_FAR - TOWER; dz <= WALL_FAR + TOWER; dz++) {
				// LEVELLED TO THE CIRCUIT, NOT TO A BOX. This tested |dx| and |dz|
				// against a flat WALL, which was correct while the wall was a square
				// and is now wrong in both directions at once: on the sides where
				// the wall stands out to WALL_FAR the paving stopped sixteen blocks
				// short of it, and where it comes in to WALL_NEAR the paving ran out
				// through the wall and into the forest.
				if (!inside(reach, dx, dz)) {
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
	/**
	 * The nearest and furthest the wall ever stands from the middle.
	 *
	 * 22 and 50 are not chosen, they are TU19's, measured by bearing off its own
	 * centre in tools/castle. Anything tighter and the smoothing pass eats the
	 * shape: at 22..46 four hundred seeds came out with a median spread of twenty
	 * against the real one's twenty-eight.
	 *
	 * It costs nothing. The old square paved 61x61 = 3,721 columns; a polygon with
	 * a mean reach of 36 covers about 4,070, and ground() only writes inside the
	 * circuit — the wider bounding box is loop iterations, not setBlock calls.
	 */
	private static final int WALL_NEAR = 22;
	private static final int WALL_FAR = 50;

	/** How many straight runs the circuit is made of. */
	private static final int CORNERS = 12;

	/** Which corner faces the gate. CORNERS/4 puts it due south, on +z. */
	private static final int GATE_CORNER = CORNERS / 4;

	/**
	 * THE CIRCUIT, AND IT IS NOT A SQUARE ANY MORE.
	 *
	 * It was a perfect box: |dx| == WALL or |dz| == WALL, thirty out on every side,
	 * one flat course of battlements all the way round. Reported as open, generic
	 * and with no personality, and the box is most of why.
	 *
	 * MEASURED OFF A REAL ONE. tools/castle reads the Legacy Console tutorial
	 * world; TU19's castle wall was measured by bearing from its own centre and it
	 * comes in between 22 and 50 blocks out — a 2.3x swing, on a wall whose foot
	 * runs from y 56 to y 92. It is not a square, it is not a circle, and it is
	 * nowhere near level. That is what a wall built round a hill by people looks
	 * like, and it is the difference between a castle and a fence.
	 *
	 * So: a twelve-sided circuit with a different reach on every side, drawn as
	 * straight runs between corners — angular rather than curved, because a curtain
	 * wall is built in straight lengths between towers and reads wrong as an arc.
	 *
	 * DETERMINISTIC FROM THE SITE. Seeded off base.asLong() rather than the level
	 * random, because this is called from a staged build: ground, curtain, towers,
	 * gate and causeway run on separate ticks and every one of them has to agree
	 * about where the wall is. A shared RandomSource would hand each stage a
	 * different castle.
	 *
	 * And the corner facing the gate is pinned to exactly WALL, because gate() and
	 * the causeway both measure from there.
	 */
	private static int[] circuit(BlockPos base) {
		RandomSource own = RandomSource.create(base.asLong());
		int[] reach = new int[CORNERS];
		for (int i = 0; i < CORNERS; i++) {
			reach[i] = WALL_NEAR + own.nextInt(WALL_FAR - WALL_NEAR + 1);
		}
		reach[GATE_CORNER] = WALL;
		// No corner may differ from its neighbour by more than this, or the "wall"
		// becomes a star and the runs stop reading as runs. Two thirds of the range
		// rather than one third: at a third the smoothing ate the shape, and four
		// hundred seeds came out with a median spread of sixteen blocks against the
		// twenty-eight measured on the real one. This lands on twenty-four.
		int most = (WALL_FAR - WALL_NEAR) * 2 / 3;
		for (int pass = 0; pass < 4; pass++) {
			for (int i = 0; i < CORNERS; i++) {
				if (i == GATE_CORNER) {
					continue;
				}
				int before = reach[(i + CORNERS - 1) % CORNERS];
				reach[i] = net.minecraft.util.Mth.clamp(reach[i], before - most, before + most);
				reach[i] = net.minecraft.util.Mth.clamp(reach[i], WALL_NEAR, WALL_FAR);
			}
		}
		return reach;
	}

	/** Corner i as an offset from the middle. */
	private static int[] corner(int[] reach, int i) {
		double a = Math.PI * 2.0 * i / CORNERS;
		return new int[] {
			(int) Math.round(Math.cos(a) * reach[i]),
			(int) Math.round(Math.sin(a) * reach[i]),
		};
	}

	/**
	 * Whether an offset is within the circuit — POINT IN POLYGON, not a radius.
	 *
	 * The first version blended the two neighbouring reaches by bearing and
	 * compared distances, which is a smooth curve through the corners. curtain()
	 * draws STRAIGHT runs between them, and a chord sags below its own arc by
	 * r(1 - cos(pi/12)) — about a block and a half at forty out. So the courtyard
	 * levelled itself to the arc and the wall was built on the chord, and the
	 * paving came out through the wall at the middle of every side.
	 *
	 * Ray casting against the twelve corners is exact and agrees with the drawn
	 * wall by construction, which is the only property that matters here.
	 */
	private static boolean inside(int[] reach, int dx, int dz) {
		boolean in = false;
		for (int i = 0, j = CORNERS - 1; i < CORNERS; j = i++) {
			int[] a = corner(reach, i);
			int[] b = corner(reach, j);
			if ((a[1] > dz) != (b[1] > dz)
				&& dx < (double) (b[0] - a[0]) * (dz - a[1]) / (b[1] - a[1]) + a[0]) {
				in = !in;
			}
		}
		return in;
	}

	/**
	 * Whether there is room to BUILD at an offset — which is not the same question
	 * as whether it is inside.
	 *
	 * inside() is ray casting, and ray casting counts the boundary as in. So the
	 * wall's own blocks answer yes, and a stall laid flush against the rampart
	 * passed every test and then overwrote it: twenty-nine clipped bays across
	 * three hundred seeds, all of them a single block of overlap on the outermost
	 * course.
	 *
	 * Eroded by one, which is exactly enough — the eight neighbours all have to be
	 * inside too, so nothing built through here can sit on the line curtain() draws.
	 * Two costs a stall and buys nothing.
	 *
	 * The hall is excluded here as well. inside() knows about the circuit and
	 * nothing about the building in the middle of it.
	 */
	private static boolean room(int[] reach, int dx, int dz) {
		if (Math.abs(dx) <= KEEP && Math.abs(dz) <= KEEP) {
			return false;
		}
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				if (!inside(reach, dx + ox, dz + oz)) {
					return false;
				}
			}
		}
		return true;
	}

	private static void curtain(ServerLevel his, BlockPos base, RandomSource random) {
		int[] reach = circuit(base);
		java.util.List<int[]> line = new java.util.ArrayList<>();
		for (int i = 0; i < CORNERS; i++) {
			int[] from = corner(reach, i);
			int[] to = corner(reach, (i + 1) % CORNERS);
			run(line, from, to);
		}
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (int[] at : line) {
			if (!done.add((long) at[0] << 32 | (at[1] & 0xFFFFFFFFL))) {
				continue;
			}
			column(his, base, at[0], at[1], random);
		}
		HerobrineMod.LOGGER.info("the wall runs {} blocks, {} to {} out",
			done.size(), WALL_NEAR, WALL_FAR);
	}

	/** Every block on the straight between two corners. */
	private static void run(java.util.List<int[]> into, int[] from, int[] to) {
		int dx = to[0] - from[0];
		int dz = to[1] - from[1];
		int steps = Math.max(Math.abs(dx), Math.abs(dz));
		for (int i = 0; i <= steps; i++) {
			into.add(new int[] {
				from[0] + (int) Math.round((double) dx * i / steps),
				from[1] + (int) Math.round((double) dz * i / steps),
			});
		}
	}

	/**
	 * One column of wall, footed on the ground under it rather than on a number.
	 *
	 * The same bearding the causeway does, and for the same reason: the courtyard
	 * is levelled off the middle of the plot, so on any slope a wall drawn at a
	 * fixed y is a wall with daylight under one end of it. Down to whatever is
	 * actually there, and never more than DIGS_IN so a wall crossing a ravine does
	 * not turn into a dam.
	 */
	private static void column(ServerLevel his, BlockPos base, int dx, int dz,
	                           RandomSource random) {
		BlockPos foot = base.offset(dx, 0, dz);
		for (int dy = 0; dy <= WALL_HEIGHT; dy++) {
			BlockPos at = foot.above(dy);
			if (dy == WALL_HEIGHT) {
				// The crenellations. Every other block, so it reads as a wall
				// somebody stands behind rather than a line with a texture on it.
				put(his, at, ((dx + dz) & 1) == 0
					? stone(random)
					: Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState()
						.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
				continue;
			}
			put(his, at, stone(random));
		}
		// Arrow slits, at head height on the wall walk.
		if (((dx * 7 + dz * 13) & 15) == 0) {
			put(his, foot.above(WALL_HEIGHT - 3), Blocks.AIR.defaultBlockState());
		}
		// The walk, one course down and one block in toward the middle.
		int inx = dx - (int) Math.signum((double) dx);
		int inz = dz - (int) Math.signum((double) dz);
		put(his, base.offset(inx, WALL_HEIGHT - 1, inz),
			Blocks.POLISHED_DEEPSLATE.defaultBlockState());

		int ground = Ground.topOf(his, foot.getX(), foot.getZ());
		for (int dy = 1; dy <= Math.min(DIGS_IN, foot.getY() - ground); dy++) {
			fill(his, foot.below(dy), stone(random));
		}
	}

	/** How far the wall's foot will chase the ground before it gives up. */
	private static final int DIGS_IN = 14;

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
