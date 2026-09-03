package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.block.TheWayBlock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;

/**
 * THE STORM IN HIS WORLD, AND IT IS NOT VANILLA'S.
 *
 * The obvious build is one line — set his world raining and thundering and never
 * let it lapse. It cannot be done, and the reason is worth writing down before
 * somebody tries it again: in 26.2 WEATHER IS SERVER-WIDE. `WeatherData` is a
 * single field on MinecraftServer and `ServerLevel.getWeatherData()` just hands
 * back the server's copy, so there is no such thing as storming one dimension.
 * Forcing it here would put a permanent thunderstorm over the player's own base,
 * for ever, as the price of a mood in a place they visit at the end.
 *
 * So the storm is assembled out of parts that ARE per-dimension:
 *
 *   THE RAIN is setRainLevel, on BOTH sides. It is a float on the Level object
 *     rather than on the server, so one dimension can be told it is pouring
 *     while the other stays dry. The client draws it; the server BELIEVES it,
 *     which is the half that was missing and cost a playtest — see below.
 *
 *   THE LIGHTNING is ours, from here. Vanilla only throws bolts while the SERVER
 *     says it is thundering, which it deliberately is not, so nothing would ever
 *     strike on its own. This drops them itself, on its own rhythm, around
 *     whoever is in there, and decides which of them are allowed to burn.
 *
 *   THE DARK is the timeline, and needs no code at all. See tools/gen_his_night.
 *
 * FIRE BURNED THE WHOLE DIMENSION DOWN ONCE, AND IT IS WORTH KNOWING HOW.
 *
 * Half the bolts were real, in the most flammable biome in the game, and the
 * rain was CLIENT-SIDE ONLY — so the player watched it pour while the server sat
 * there convinced the place was bone dry. Vanilla puts fire out in FireBlock's
 * random tick by asking isRainingAt, and that question was answered "no" every
 * time. Nothing ever stopped. The wood went, then the city, then the castle.
 *
 * Three things hold it now, and they are meant to overlap rather than each be
 * sufficient: one bolt in four burns instead of one in two; the server is wet,
 * so everything under open sky puts itself out the way a player expects; and
 * every real strike books its own sweep for the canopy, which is the one place
 * rain cannot reach. On top of that, nothing real is ever thrown near the keep.
 *
 * The wood is still allowed to burn, and should — it grows back, nobody built
 * it, and a country that looks worse each time you cross it is the intention.
 * What may not burn is the one thing in the dimension anybody made.
 */
public final class HisWeather {
	private HisWeather() {}

	/** Every two seconds. Lightning is a rhythm rather than a downpour. */
	private static final int CHECK_INTERVAL = 40;

	/**
	 * How often the sky comes down, as one in N per check.
	 *
	 * About one bolt every eight seconds per player. Frequent enough that the
	 * horizon is never quiet for long and rare enough that each one still
	 * lights the trees rather than blurring into a strobe.
	 */
	// ONE IN THIRTY, TWENTY-EIGHT BLOCKS OUT. It was one in four at sixteen: a bolt
	// every eight seconds within sight of the player, which is exactly the "weather,
	// not a move" that arsenal() was just cured of — and in survival, where a bolt
	// is a threat rather than a light show, it read as spam the moment it started.
	// Distant and rare, this is a storm over a country; and none at all once the
	// fight is on, because from the first blow lightning is HIS to throw.
	private static final int STRIKE_CHANCE_IN = 30;
	private static final int NEAR = 28;
	private static final int FAR = 60;

	/**
	 * HOW MANY OF THEM ACTUALLY BURN, AND IT USED TO BE HALF.
	 *
	 * Half, in a dark forest, with nothing putting the fire out, burned the
	 * entire dimension down — the wood, the city and the castle with it. Two
	 * separate mistakes stacked: too many real bolts, and no rain on the server
	 * to stop what they started. Both are fixed, and this is the smaller half of
	 * the fix.
	 *
	 * One in four. Enough that a wood going up is something that happens while
	 * you are in there, rare enough that it is an event rather than the climate.
	 */
	private static final int BURNS_ONE_IN = 4;

	/**
	 * AND THE RAIN PUTS THEM OUT, WHICH IS THE REST OF THE FIX.
	 *
	 * The rain was made client-side, because weather is server-wide in 26.2 and
	 * forcing it would have stormed the player's own overworld. That was right
	 * about the storm and wrong about everything downstream of it: the SERVER
	 * still thought the dimension was bone dry, so vanilla's own extinguishing —
	 * FireBlock checks isRainingAt on every random tick — never once ran. Fire
	 * lit, spread, and never stopped, under a sky the player could see raining
	 * on it.
	 *
	 * Level.setRainLevel is a float on the LEVEL rather than on the server, the
	 * same as the client-side one, so the server can be told it is pouring in
	 * this dimension and nowhere else. isRaining() then returns true here, and
	 * every piece of vanilla fire behaviour that has ever depended on weather
	 * starts working the way a player expects.
	 *
	 * Set every tick because vanilla eases it back toward what the server-wide
	 * WeatherData says, which is clear.
	 *
	 * Thunder is deliberately left LOW on the server. isThundering() is
	 * thunderLevel above nine tenths, and at that point vanilla starts throwing
	 * its own lightning on top of ours — uncapped, unfiltered, and perfectly
	 * happy to land on the keep. The client draws the dark sky; the server does
	 * not need to believe in it.
	 */
	private static final float SERVER_RAIN = 1.0F;
	private static final float SERVER_THUNDER = 0.0F;

	private static int tickCounter;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HisWeather::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (!Config.get().enabled) {
			return;
		}
		ServerLevel his = server.getLevel(TheWayBlock.HIS_WORLD);
		if (his == null) {
			return;
		}
		// Removed Herobrine. The rain over his world was his; it stops with him.
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			his.setRainLevel(0.0F);
			his.setThunderLevel(0.0F);
			if (!Boolean.TRUE.equals(his.getAttached(com.bloomlet.herobrine.wrath.Wrath.CLEAR_SKY))) {
				his.setAttached(com.bloomlet.herobrine.wrath.Wrath.CLEAR_SKY, true);
			}
			if (his.getAttached(com.bloomlet.herobrine.wrath.Wrath.CLEARED_AT) == null) {
				his.setAttached(com.bloomlet.herobrine.wrath.Wrath.CLEARED_AT, his.getGameTime());
			}
			// AND SAY SO. Vanilla sends the rain-level packet only when ITS OWN step
			// changed the level; ours is forced after that step, so the client kept
			// the last value it was ever told — 0.99 — and drew rain in a dry sky for
			// as long as anybody stood there. Every five seconds, to everyone here.
			if (++tickCounter % 100 == 0) {
				for (ServerPlayer player : his.players()) {
					tellDry(player);
				}
			}
			return;
		}
		// Every tick, and before the interval check — this is what makes it wet
		// and it has to hold whether or not anybody is standing in it, or a fire
		// started on the way out keeps burning after they leave.
		his.setRainLevel(SERVER_RAIN);
		his.setThunderLevel(SERVER_THUNDER);

		if (++tickCounter % CHECK_INTERVAL != 0 || his.players().isEmpty()) {
			return;
		}
		RandomSource random = his.getRandom();
		for (ServerPlayer player : his.players()) {
			if (random.nextInt(strikeChance(his, player)) == 0 && !Reckoning.bound(his)) {
				strike(his, player, random);
			}
			if (random.nextInt(3) == 0) {
				breath(his, player, random);
			}
			halo(his, player);
			bed(his, player);
		}
	}

	// ---- THE BED -----------------------------------------------------------
	/**
	 * A DRONE UNDER THE WHOLE DIMENSION, AND IT NEVER STOPS.
	 *
	 * Twenty-two seconds, seamless, played at the player rather than at a place —
	 * so it does not fall off as they walk and there is nowhere to stand that is
	 * quiet. That is the one thing an ambient bed has to get right: the moment it
	 * has a source, the player locates it, and a located sound is a sound they
	 * have solved.
	 *
	 * IT IS ALSO THE ONLY SOUND IN THIS MOD NOBODY IS MEANT TO NOTICE. Everything
	 * else here is an event — a bolt, a creak, a window going. This is the room
	 * tone, and the test of it is that a player who leaves for the overworld feels
	 * the silence rather than remembering the noise.
	 *
	 * Re-issued a little before it runs out. Minecraft will not loop a sound for
	 * us and there is no way to ask whether one is still playing, so it is timed:
	 * a shade under the file's length, which overlaps by a fraction of a second
	 * and covers any tick the server ran late.
	 */
	private static final int BED_TICKS = 420;
	private static final java.util.Map<java.util.UUID, Integer> bedIn =
		new java.util.HashMap<>();

	private static void bed(ServerLevel his, ServerPlayer player) {
		int left = bedIn.getOrDefault(player.getUUID(), 0) - CHECK_INTERVAL;
		if (left > 0) {
			bedIn.put(player.getUUID(), left);
			return;
		}
		bedIn.put(player.getUUID(), BED_TICKS);
		// ATTACHED TO THE PLAYER, not played at a position.
		//
		// The entity overload of playSound is the whole reason this works: a
		// positional sound anchored where they were standing twenty seconds ago
		// has faded to nothing by the time it is re-issued, and the bed would
		// come and go as they walked. Attached, it follows, and there is nowhere
		// in the dimension that is quiet.
		//
		// Quiet, because it has to survive being heard for an hour.
		his.playSound(null, player, com.bloomlet.herobrine.sound.ModSounds.HIS_WORLD,
			net.minecraft.sounds.SoundSource.AMBIENT, 0.55F, 1.0F);
	}

	// ---- THE LIGHT YOU BRING WITH YOU --------------------------------------
	/**
	 * A TORCH REACHES FURTHER HERE, AND IT CANNOT BE DONE THE OBVIOUS WAY.
	 *
	 * There is no per-dimension light radius. A torch is light level fourteen
	 * everywhere in the game and the falloff is one per block, baked into the
	 * light engine; the only dimension-level dials are ambient_light and the
	 * sky, and both of those lift the WHOLE place rather than the ground around
	 * somebody's torch — which would undo the dark that the dimension is for.
	 *
	 * So the reach is added rather than the radius changed: a ring of vanilla
	 * LIGHT blocks around each torch, three or four out, at full fifteen. LIGHT
	 * is invisible, has no collision and no drops, and exists in vanilla for
	 * exactly this — lighting somewhere without putting an object in it. What the
	 * player sees is a torch throwing about twice as far as one at home, in the
	 * one place in the game where that is worth something.
	 *
	 * AND IT CLEANS UP AFTER ITSELF. Every pass also removes any of these with no
	 * torch left near it, so mining a torch out takes its halo with it and
	 * nothing is left littering the wood. That self-repair is why this is a
	 * SWEEP rather than a place-time hook: a hook would have to be right first
	 * time, and a sweep that runs every two seconds only has to be right
	 * eventually.
	 *
	 * Bounded hard. Thirteen blocks around each player, which is a couple of
	 * thousand lookups on a two-second beat and never leaves the chunks they are
	 * standing in.
	 */
	private static final int HALO_LOOK = 6;
	private static final int HALO_REACH = 4;
	/** Where spread() puts its light round a torch — and therefore where a light's torch must be. */
	private static final int[][] HALO_STEPS = {
		{ HALO_REACH, 0, 0 }, { -HALO_REACH, 0, 0 },
		{ 0, 0, HALO_REACH }, { 0, 0, -HALO_REACH },
		{ 0, HALO_REACH - 1, 0 }, { 0, -HALO_REACH + 1, 0 } };

	private static void halo(ServerLevel his, ServerPlayer player) {
		BlockPos middle = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(
				middle.offset(-HALO_LOOK, -HALO_LOOK, -HALO_LOOK),
				middle.offset(HALO_LOOK, HALO_LOOK, HALO_LOOK))) {
			net.minecraft.world.level.block.state.BlockState state = his.getBlockState(pos);
			if (isTorch(state)) {
				spread(his, pos.immutable());
			} else if (state.is(Blocks.LIGHT) && !nearATorch(his, pos)) {
				// The torch it belonged to is gone. So is it.
				his.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	/** Six points on a ring, which is enough to read as a wider pool of light. */
	private static void spread(ServerLevel his, BlockPos torch) {
		for (int[] step : HALO_STEPS) {
			BlockPos at = torch.offset(step[0], step[1], step[2]);
			if (his.getBlockState(at).isAir()) {
				his.setBlock(at, Blocks.LIGHT.defaultBlockState()
					.setValue(net.minecraft.world.level.block.state.properties
						.BlockStateProperties.LEVEL, 15), 2);
			}
		}
	}

	private static boolean nearATorch(ServerLevel his, BlockPos at) {
		// A LIGHT block in his world only ever comes from spread(), and spread()
		// puts them at six fixed offsets from a torch. So the torch that owns this
		// one is at one of six places, not somewhere in a nine-cube: 729 reads
		// became 6, and a well-lit base stopped being quadratic.
		for (int[] step : HALO_STEPS) {
			if (isTorch(his.getBlockState(at.offset(-step[0], -step[1], -step[2])))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What counts as a torch.
	 *
	 * The player's own light, and nothing the mod placed. Soul lanterns are all
	 * over the castle and the city and giving those haloes would light the whole
	 * settlement like a football ground — the point of this is the pool of light
	 * somebody carries into a wood, not the place they are walking towards.
	 */
	private static boolean isTorch(net.minecraft.world.level.block.state.BlockState state) {
		return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
			|| state.is(Blocks.LANTERN) || state.is(Blocks.CAMPFIRE);
	}

	/**
	 * A bolt, out in the trees.
	 *
	 * Never within sixteen blocks of anybody, which is the same number the hunt's
	 * treeline rung uses and is why nothing here ever lands on the person
	 * watching it.
	 *
	 * Most of them are a flash and nothing else. A quarter of the far ones burn,
	 * they are refused outright anywhere near the keep, and what they start is
	 * on a clock from the moment it is lit.
	 */
	/**
	 * THE STORM KNOWS HE IS COMING — and this is now the only place that says so.
	 *
	 * HisHost had its own version: a REAL bolt six to twenty-two blocks from every
	 * survival player, rolled every second, at up to 95% once he was near. With him
	 * circling the keep that was a crater-digging strike most seconds, and it is
	 * what "lightning spam the moment I went survival" was — the hive tick skips
	 * creative players, so creative never saw it. Two ambient lightning systems in
	 * one dimension is one too many; that one is gone and its one good idea, that
	 * the sky tightens as he closes, lives here: one in thirty with nobody around,
	 * one in eight when he is within ninety blocks. Still distant, still mostly
	 * visual, still nothing at all once the fight is on.
	 */
	private static final double HE_IS_NEAR = 90.0;
	private static final int STRIKE_CHANCE_NEAR = 8;

	private static int strikeChance(ServerLevel his, ServerPlayer player) {
		com.bloomlet.herobrine.entity.HerobrineEntity him =
			com.bloomlet.herobrine.entity.HerobrineEntity.oneIn(his);
		if (him == null || him.distanceTo(player) > HE_IS_NEAR) {
			return STRIKE_CHANCE_IN;
		}
		return STRIKE_CHANCE_NEAR;
	}

	/** The three packets that make a client stop drawing rain, right now. */
	public static void tellDry(ServerPlayer player) {
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
			net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING, 0.0F));
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
			net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F));
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
			net.minecraft.network.protocol.game.ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
	}

	private static void strike(ServerLevel his, ServerPlayer player, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = NEAR + random.nextDouble() * (FAR - NEAR);
		int x = player.blockPosition().getX() + (int)Math.round(Math.cos(angle) * range);
		int z = player.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * range);
		BlockPos where = new BlockPos(x, his.getSeaLevel(), z);
		if (!his.isLoaded(where)) {
			return;
		}
		int y = his.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(his, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}

		// Far enough off to be scenery rather than an attack, rare enough to be
		// an event — and NEVER on the one thing in this dimension that cannot be
		// rebuilt.
		boolean burns = range >= 30.0
			&& random.nextInt(BURNS_ONE_IN) == 0
			&& clearOfTheKeep(his, where);
		bolt.setVisualOnly(!burns);
		bolt.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
		his.addFreshEntity(bolt);

		if (burns) {
			douseLater(his, new BlockPos(x, y, z));
		}
	}

	/**
	 * NOT ON THE CASTLE, AND NOT ON THE TOWN.
	 *
	 * The one thing here that must never burn. Everything else in the dimension
	 * is trees and it can go — it grows back, nobody built it, and a wood that
	 * looks worse every time you come back is the whole intention. The keep and
	 * the city are the opposite: they are the only thing in the place anybody
	 * made, they are the reason to walk there at all, and there is no mechanism
	 * anywhere in this mod that would put them back.
	 *
	 * The exclusion is the whole works plus a margin, so fire started at the
	 * edge of the radius still has a long way to travel before it reaches a
	 * house — and the rain gets it first, because the streets and roofs are the
	 * one part of this dimension that IS open to the sky.
	 */
	private static boolean clearOfTheKeep(ServerLevel his, BlockPos at) {
		// TWO PLACES TO SPARE, NOT ONE.
		//
		// reach() used to be the castle wall PLUS the whole city, because the city
		// was a ring around the castle and one radius covered both. They are a
		// couple of hundred blocks apart now, so a radius drawn round the castle
		// stopped protecting the town entirely — and a real bolt in the town is the
		// town burning down, which is the one thing this check exists to prevent.
		BlockPos keep = com.bloomlet.herobrine.structure.Keep.site(his);
		if (keep != null
			&& keep.closerThan(at, com.bloomlet.herobrine.structure.Keep.reach())) {
			return false;
		}
		BlockPos city = com.bloomlet.herobrine.structure.Keep.city(his);
		return city == null
			|| !city.closerThan(at, com.bloomlet.herobrine.structure.Keep.cityReach());
	}

	/**
	 * AND IT GOES OUT, EVEN WHERE THE RAIN CANNOT REACH.
	 *
	 * The server rain handles everything under open sky, which is most of the
	 * dimension and all of the city. What it cannot touch is fire under a dark
	 * forest canopy — isRainingAt needs canSeeSky, and a dark oak roof is the
	 * densest cover in the game. Left alone, one bolt in the wrong stand of
	 * trees still crawls outward for as long as the chunk is loaded.
	 *
	 * So every real strike books its own cleanup: a sweep at thirty seconds and
	 * a wider one at a minute. Long enough that the player gets a wood genuinely
	 * on fire and time to stand and watch it, short enough that it is over
	 * before it becomes the map.
	 */
	private static void douseLater(ServerLevel his, BlockPos at) {
		Cadence.in(his.getServer(), 600, () -> douse(his, at, 14));
		Cadence.in(his.getServer(), 1300, () -> douse(his, at, 22));
	}

	private static void douse(ServerLevel his, BlockPos at, int radius) {
		if (!his.isLoaded(at)) {
			return;
		}
		int out = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				at.offset(-radius, -8, -radius), at.offset(radius, 10, radius))) {
			if (his.getBlockState(pos).is(Blocks.FIRE)) {
				his.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				out++;
			}
		}
		if (out > 0) {
			his.playSound(null, at, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
				net.minecraft.sounds.SoundSource.WEATHER, 1.6F, 0.8F);
			HerobrineMod.LOGGER.debug("his world: the rain took {} fires at [{}, {}]",
				out, at.getX(), at.getZ());
		}
	}

	/**
	 * And something is always moving in the trees.
	 *
	 * Not a mob and not an event — one distant sound every few seconds, chosen
	 * from things a wood makes. The dark forest is dense enough that a player
	 * cannot see more than a few blocks between the trunks, so the only thing
	 * that can fill it is noise, and noise is free.
	 *
	 * Placed a long way off, always, so it can never be resolved by turning
	 * round. Anything a player can walk to and find is a thing they have solved.
	 *
	 * Every one of these is a sound they have heard a thousand times in an
	 * ordinary world, which is the entire trick: a wood that growls is a wood
	 * with a wolf in it, and a wood that creaks and settles and drips is a wood
	 * with nothing in it you can point at.
	 */
	private static void breath(ServerLevel his, ServerPlayer player, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double range = 18.0 + random.nextDouble() * 26.0;
		BlockPos at = player.blockPosition().offset(
			(int)Math.round(Math.cos(angle) * range), random.nextInt(7) - 3,
			(int)Math.round(Math.sin(angle) * range));
		his.playSound(null, at, VOICES[random.nextInt(VOICES.length)],
			net.minecraft.sounds.SoundSource.AMBIENT,
			0.6F + random.nextFloat() * 0.4F, 0.5F + random.nextFloat() * 0.4F);
	}

	private static final SoundEvent[] VOICES = {
		SoundEvents.CREAKING_HEART_IDLE,
		SoundEvents.WOOD_STEP,
		SoundEvents.PALE_HANGING_MOSS_IDLE,
		SoundEvents.AMBIENT_CAVE.value(),
		SoundEvents.WOOD_BREAK,
		SoundEvents.BIG_DRIPLEAF_TILT_DOWN,
	};

	static {
		HerobrineMod.LOGGER.debug("his weather ready");
	}
}
