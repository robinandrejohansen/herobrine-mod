package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.manifest.Cadence;
import com.bloomlet.herobrine.manifest.ManifestationDirector;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Places him in the world. WHEN is the director's call, not this class's.
 *
 * Deliberately NOT vanilla natural spawning. Biome spawn rules would give him
 * a spawn weight and a pack size, which means groups of Herobrines in caves —
 * exactly wrong. A haunting is one figure, rarely, where you did not expect
 * one.
 *
 * Three rules make it work:
 *   1. DARKNESS, not time of day. Gating on light level means he also appears
 *      down a mineshaft at noon, and the emissive eyes only pay off in the
 *      dark anyway.
 *   2. OUT OF VIEW. He is placed behind you. He should never pop into
 *      existence while you watch — the whole effect is "he was already there".
 *   3. ONE AT A TIME. If a Herobrine already exists near the player, nothing
 *      spawns. Two of him is a mob; one of him is a ghost story.
 */
public final class HauntingSpawner {
	private HauntingSpawner() {}

	/**
	 * How far out he stands.
	 *
	 * Pushed a long way back after playing it. At twenty-six blocks he is a
	 * figure you can make out, and something you can make out is something you
	 * can assess — you look at him, decide what he is, and the moment is over.
	 * At forty-plus he is a shape at the treeline that might be a fence post,
	 * and the player spends several seconds deciding whether to walk towards
	 * it. Those seconds are the entire event.
	 */
	private static final double MIN_RADIUS = 42.0;
	private static final double MAX_RADIUS = 68.0;
	/**
	 * The fallback ring, tried only when the whole of the proper one failed.
	 *
	 * Standing well back is the intent and it is still what gets tried first.
	 * But in broken country -- a peak, a ravine, a coastline -- there may be
	 * no visible standable ground at all between forty and seventy blocks, and
	 * the choice is then between a closer figure and no figure. A closer
	 * figure is a worse version of the event. No figure is not the event.
	 */
	private static final double CLOSE_RADIUS = 26.0;

	/**
	 * At SIEGE he does not simply be there. The sky comes with him.
	 *
	 * Every appearance before this one is built on him never being seen to
	 * arrive — you look up and he is already standing there, and the whole
	 * effect depends on there having been no moment of arrival to point at.
	 * This is the phase that abandons it, and it should be the loudest possible
	 * abandonment: three bolts on the ground he is standing on, so the player
	 * does not find him, they are TOLD where he is from across the valley.
	 *
	 * Visual-only, like everything else that throws lightning here. It cannot
	 * burn anything down, which is the only reason it is allowed to happen in a
	 * player's field at all.
	 */
	private static void arrival(ServerLevel level, BlockPos pos) {
		if (Wrath.phase(level.getServer()) != Phase.SIEGE) {
			return;
		}
		RandomSource random = level.getRandom();
		for (int i = 0; i < 3; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = random.nextDouble() * 3.5;
			final double x = pos.getX() + 0.5 + Math.cos(angle) * range;
			final double z = pos.getZ() + 0.5 + Math.sin(angle) * range;
			Cadence.in(level.getServer(), i * (3 + random.nextInt(5)), () -> {
				net.minecraft.world.entity.LightningBolt bolt =
					net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
						.create(level, EntitySpawnReason.EVENT);
				if (bolt == null) {
					return;
				}
				bolt.setVisualOnly(true);
				bolt.snapTo(x, pos.getY(), z, 0.0F, 0.0F);
				level.addFreshEntity(bolt);
			});
		}
	}

	// ---- DEBUG AID, DELETE WHEN DONE -------------------------------------
	// A visual-only bolt on every placement, so a tester can see where he
	// actually went instead of walking a bearing and hoping.
	//
	// setVisualOnly(true) gates BOTH the fire and the entity damage in
	// LightningBolt.tick, checked against 26.2 rather than assumed — this
	// cannot burn a forest down or hurt anybody, which is the only reason it
	// is safe to leave switched on while playing.
	//
	// To remove: delete this field, the strike() call in spawnAt, the method
	// itself, and the "mark" branch in HerobrineCommand. Nothing else refers
	// to any of it.
	private static boolean markSpawns = true;

	public static boolean toggleMark() {
		markSpawns = !markSpawns;
		return markSpawns;
	}

	private static void strike(ServerLevel level, BlockPos pos) {
		if (!markSpawns) {
			return;
		}
		net.minecraft.world.entity.LightningBolt bolt =
			net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
				.create(level, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return;
		}
		bolt.setVisualOnly(true);
		bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(bolt);
	}
	// ---- END DEBUG AID ---------------------------------------------------

	/** How the sweep is spread: every bearing, a few distances down each. */
	private static final int BEARINGS = 32;
	private static final int DISTANCES = 3;
	/**
	 * How bright it may be where he stands, by phase — and it climbs.
	 *
	 * This was a flat 7, which is "dark enough for monsters", which outdoors
	 * means night and nothing else. That quietly made the mod unplayable for
	 * anyone who sleeps: night is eight minutes in twenty, sleeping removes
	 * almost all of it, and sleeping is ALSO the largest single source of
	 * wrath. So the players provoking him hardest were the ones with no window
	 * left for him to appear in — one outdoor sighting a day of real play. Two
	 * systems that did not know about each other.
	 *
	 * A curve fixes it and says something at the same time. At WATCHER he still
	 * needs proper dark — dusk, dawn, the middle of a storm — and by SIEGE he
	 * will stand in a field at noon. The thing that starts as something you
	 * might have imagined in bad light ends up refusing to need the excuse,
	 * which is the whole arc of the mod expressed as a number.
	 *
	 * Rain does the work in the middle of that range without any special
	 * casing: an overcast sky genuinely lowers the light level, so "he turns up
	 * on grey afternoons" falls out of it for free.
	 */
	private static int maxLight(Phase phase) {
		return switch (phase) {
			case RUMOUR, WATCHER -> 9;
			case TRESPASSER -> 11;
			case MIMIC -> 12;
			case HUNTER -> 14;
			case SIEGE -> 15;
		};
	}
	/**
	 * There is only ever ONE of him, in the entire world.
	 *
	 * This used to be a 96-block radius around the player, which is not the
	 * same thing at all: two players far enough apart would each get their own
	 * simultaneously. Two Herobrines existing at once is the single most
	 * damaging thing that could happen to this mod — the moment there can be
	 * more than one, he is a mob type rather than a person, and everything
	 * else here is built on him being a person.
	 *
	 * It also costs nothing narratively. He is in one place because he is one
	 * thing; if he is with someone else you get a noise instead, and the
	 * director already falls through to the other manifestations.
	 */
	private static boolean existsAnywhere(ServerLevel level) {
		for (ServerLevel other : level.getServer().getAllLevels()) {
			if (!other.getEntities(ModEntities.HEROBRINE, e -> true).isEmpty()) {
				return true;
			}
		}
		return false;
	}
	/** Above this, the position is in front of the player and unusable. */
	private static final double IN_VIEW_DOT = 0.25;

	/** Why a placement did or did not happen — debug commands need the reason. */
	/**
	 * Why he did or did not turn up.
	 *
	 * Split finer than it needs to be for the code, and entirely for the
	 * person testing. "Wrong surroundings" covered five unrelated situations
	 * and named none of them, and the commonest one by far — he is already out
	 * there from the last attempt — looks exactly like a broken command.
	 */
	public enum Outcome {
		PLACED,
		/** One of him already exists somewhere in the world. */
		ALREADY_NEARBY,
		/** Enclosed, and there is no room to stand back far enough. */
		NO_ROOM_HERE,
		/** Somewhere to stand, but all of it lit. */
		TOO_BRIGHT,
		/** Dark enough, but every option was in front of the player. */
		NOTHING_BEHIND,
		/** Behind and dark, but nothing the player could actually see. */
		NOTHING_VISIBLE,
		/** Ground would not take him. */
		NO_FOOTING,
		BAD_PLAYER;

		public String reason() {
			return switch (this) {
				case PLACED -> "";
				case ALREADY_NEARBY -> "he is already somewhere in this world — "
					+ "wait for him to go, or find him";
				case NO_ROOM_HERE -> "you are enclosed and there is nowhere he could stand "
					+ "9 blocks back in this space — step outside or into a bigger cave";
				case TOO_BRIGHT -> "everywhere behind you is lit — he needs light 7 or under. "
					+ "Try at night, or use 'provoke force'";
				case NOTHING_BEHIND -> "everywhere dark enough was in front of you — "
					+ "turn around and try again";
				case NOTHING_VISIBLE -> "every dark spot behind you is hidden by terrain — "
					+ "he would have been standing somewhere you could never see";
				case NO_FOOTING -> "no solid ground to stand on in the ring behind you";
				case BAD_PLAYER -> "you are dead or spectating";
			};
		}
	}

	/**
	 * Places him behind the player, if the world allows it.
	 *
	 * The director treats anything but PLACED as a quiet night rather than
	 * retrying — see ManifestationDirector.
	 */
	public static boolean spawnBehind(ServerLevel level, ServerPlayer player) {
		return place(level, player, false) == Outcome.PLACED;
	}

	/**
	 * @param ignoreLight debug only. Skips the darkness requirement so he can
	 *                    be placed in daylight for appearance testing.
	 */
	public static Outcome place(ServerLevel level, ServerPlayer player, boolean ignoreLight) {
		return place(level, player, ignoreLight, false);
	}

	/**
	 * @param hunting he follows instead of standing. HUNTER and up only — the
	 *                caller decides, because the spawner has no business
	 *                knowing which manifestation asked for him.
	 */
	public static Outcome place(ServerLevel level, ServerPlayer player, boolean ignoreLight,
	                            boolean hunting) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}

		if (existsAnywhere(level)) {
			return Outcome.ALREADY_NEARBY;
		}

		// THE STARE IS AN OUTDOOR EVENT AND NOTHING ELSE.
		//
		// It used to fall back to a cave placement when the player was
		// enclosed, which made one piece of code responsible for two
		// experiences that have nothing in common. Outdoors he is a shape at
		// the treeline sixty blocks off, read against a horizon, and the
		// whole effect is distance. In a cave there is no horizon, no distance
		// and no sky — he is in the corridor with you, and the effect is that
		// there is no way round him.
		//
		// Sharing the code made the cave version a worse copy of the outdoor
		// one: too far to matter in a passage, judged by rules about being
		// behind you that mean nothing in a tunnel with two ends. Underground
		// belongs to glimpse() and passage(), which own their own logic.
		if (ConfinedPlacement.isConfined(level, player)) {
			return Outcome.NO_ROOM_HERE;
		}

		RandomSource random = level.getRandom();
		// Several attempts, because most candidate rings will be too bright or
		// in front of the player. Failing quietly is correct — he simply does
		// not appear this time.
		// Counted rather than merely skipped, so a refusal can say which of
		// these it was. "Too bright" is a different night from "you were
		// facing the only dark side", and only one of them is worth waiting
		// out.
		int tooBright = 0;
		int inFront = 0;
		int roofed = 0;
		int blocked = 0;
		int noFooting = 0;

		// SWEEP THE CIRCLE, do not sample it.
		//
		// Forty independent random angles sound like good coverage and are
		// not: they clump, and they leave wedges of the horizon untried. From
		// a mountaintop that is fatal, because the player can see an enormous
		// amount of ground and the one direction that was never tested is
		// exactly the one with a visible ridge in it.
		//
		// So every direction gets looked at. Thirty-two evenly spaced
		// bearings, offset by a random amount so he does not favour the
		// compass points, three distances down each one. Ninety-six candidates
		// covering the whole horizon instead of forty landing wherever they
		// fell.
		//
		// Then, only if the whole ring failed, the same sweep again closer in.
		// He is meant to stand well back and that is still tried first, but a
		// player who never sees him has lost the event entirely, and thirty
		// blocks away is a great deal better than nothing at all.
		double[][] rings = {
			{ MIN_RADIUS, MAX_RADIUS },
			{ CLOSE_RADIUS, MIN_RADIUS },
		};
		for (double[] ring : rings) {
			double spin = random.nextDouble() * Math.PI * 2.0;
			for (int attempt = 0; attempt < BEARINGS * DISTANCES; attempt++) {
				double angle = spin + (attempt % BEARINGS) * (Math.PI * 2.0 / BEARINGS);
				double step = (attempt / BEARINGS) + random.nextDouble();
				double radius = ring[0] + (ring[1] - ring[0]) * (step / DISTANCES);
				int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
				int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				BlockPos pos = new BlockPos(x, y, z);

				// Cheapest test first. This is pure arithmetic on two vectors and
				// it throws away roughly a third of every sweep, so running it
				// after two chunk lookups and a raycast was simply paying for
				// answers about candidates that were never going to be used.
				if (isInFrontOf(player, pos)) {
					inFront++;
					continue;
				}

				// GROUND THAT WOULD ACTUALLY HOLD HIM, and this is where he was
				// standing on the sea.
				//
				// Every Minecraft heightmap counts fluid as surface — the real
				// predicate is `blocksMotion() || !getFluidState().isEmpty()` —
				// so over an ocean this returns the top of the water, and
				// nothing downstream ever asked whether that was a floor. He
				// was placed on the waves; and because open water has no trees,
				// no ridge and no canopy, that was ALSO the only place the
				// sightline test could pass. One bug produced the other, and
				// the sea was very nearly the only place he was ever put.
				//
				// It walks DOWN as well, because the heightmap stops above a
				// snow layer, a carpet or a bottom slab and none of those has a
				// sturdy top face — which failed an entire mountainside for
				// ground a mob stands on quite happily. Four blocks clears any
				// stack of those and cannot reach a seabed, so water still
				// refuses at every level.
				BlockPos stand = null;
				for (int down = 0; down <= 4 && stand == null; down++) {
					BlockPos maybe = pos.below(down);
					if (ConfinedPlacement.canStand(level, maybe)) {
						stand = maybe;
					}
				}
				if (stand == null) {
					noFooting++;
					continue;
				}
				pos = stand;
				// And he stands in the open.
				//
				// A spot under a forest canopy satisfies every other rule and is
				// unlookable-at from fifty blocks, because leaves are colliders and
				// the ray dies in them. Requiring sky above him costs nothing, puts
				// him on ridges and in clearings and at treelines where the whole
				// image belongs, and stops the placement loop burning all twenty
				// attempts inside a wood and reporting NOTHING_VISIBLE.
				if (!level.canSeeSky(pos)) {
					roofed++;
					continue;
				}

				if (!ignoreLight && level.getMaxLocalRawBrightness(pos)
					> maxLight(Wrath.phase(level.getServer()))) {
					tooBright++;
					continue;
				}
				// And the player must actually be able to SEE the spot.
				//
				// Behind them and dark enough was not sufficient: a hill, a stand
				// of trees or the far side of a ridge would satisfy both and put
				// him somewhere nobody could ever look at. The whole event is
				// being seen, so a placement that cannot be seen is not a quiet
				// night — it is a wasted one, and the player is left turning on the
				// spot wondering what the command did.
				if (!visibleFrom(level, player, pos)) {
					blocked++;
					continue;
				}

				Outcome result = spawnAt(level, player, pos, hunting);
				if (result == Outcome.PLACED) {
					return result;
				}
				noFooting++;
				HerobrineMod.LOGGER.info("stare: could not create at [{}, {}, {}]",
					pos.getX(), pos.getY(), pos.getZ());
			}
		}
		// Roofed and blocked are counted apart on purpose: "under a canopy" and
		// "behind the brow of the hill" are the same refusal to the player and
		// completely different things to fix.
		int hidden = roofed + blocked;
		HerobrineMod.LOGGER.info(
			"stare refused after {} tries from y={}: {} lit, {} in front, "
				+ "{} roofed, {} behind terrain, {} no footing",
			BEARINGS * DISTANCES * rings.length, player.blockPosition().getY(),
			tooBright, inFront, roofed, blocked, noFooting);
		if (tooBright >= inFront && tooBright >= hidden && tooBright >= noFooting) {
			return Outcome.TOO_BRIGHT;
		}
		if (hidden >= inFront && hidden >= noFooting) {
			return Outcome.NOTHING_VISIBLE;
		}
		return inFront >= noFooting ? Outcome.NOTHING_BEHIND : Outcome.NO_FOOTING;
	}

	/**
	 * A GLIMPSE, underground, and it is the opposite of the stare in almost
	 * every respect.
	 *
	 * The stare puts him BEHIND you, a long way off, and lets you look at him
	 * until you have had enough. This puts him in FRONT of you, close, in a
	 * cave, for about half a second — and it is gone before you have decided
	 * whether it was there. The player is not meant to resolve it. They are
	 * meant to stop, look again at an empty passage, and carry on mining
	 * slightly differently.
	 *
	 * That is the original account of meeting him, almost verbatim: "he looked
	 * at me and quickly ran into the fog". One sentence, no confrontation, and
	 * over immediately.
	 *
	 * NO SOUND, and that is deliberate. Every instinct says to put a cue on it,
	 * and a cue is exactly what turns this from something a player half-saw
	 * into something the mod told them about. If they were looking the other
	 * way, they missed it. That has to be a real possibility or the ones they
	 * do catch are worth nothing.
	 */
	public static Outcome glimpse(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsAnywhere(level)) {
			return Outcome.ALREADY_NEARBY;
		}
		// Underground, or it is not this event. Above ground he has the stare,
		// which is a better version of being seen at distance in the open.
		if (level.canSeeSky(player.blockPosition())) {
			return Outcome.NOTHING_VISIBLE;
		}

		RandomSource random = level.getRandom();
		Vec3 look = player.getViewVector(1.0F).normalize();

		for (int attempt = 0; attempt < 48; attempt++) {
			// Close. A figure forty blocks down a tunnel is a shape; one nine
			// blocks away is a person, and nine blocks is also about as far as
			// a torch reaches, so he is at the edge of what the player can see.
			double range = 7.0 + random.nextDouble() * 11.0;
			// Roughly where they are looking, with enough spread that it is not
			// always dead centre — being slightly off to one side is worse,
			// because it makes them turn their head.
			double spread = (random.nextDouble() - 0.5) * 1.1;
			double cos = Math.cos(spread);
			double sin = Math.sin(spread);
			Vec3 out = new Vec3(look.x * cos - look.z * sin, 0.0,
				look.x * sin + look.z * cos).normalize().scale(range);

			BlockPos at = BlockPos.containing(
				player.getX() + out.x,
				player.getY() + random.nextInt(5) - 2,
				player.getZ() + out.z);

			BlockPos stand = null;
			for (int down = 0; down <= 3 && stand == null; down++) {
				BlockPos maybe = at.below(down);
				if (ConfinedPlacement.canStand(level, maybe)) {
					stand = maybe;
				}
			}
			if (stand == null || !visibleFrom(level, player, stand)) {
				continue;
			}

			HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
			if (him == null) {
				return Outcome.NO_FOOTING;
			}
			double dx = player.getX() - (stand.getX() + 0.5);
			double dz = player.getZ() - (stand.getZ() + 0.5);
			float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
			him.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 0.0F);
			// Ten to eighteen ticks. Long enough to register as a figure,
			// nowhere near long enough to be studied, and short enough that a
			// player who blinked genuinely did miss it.
			him.beGlimpse(10 + random.nextInt(9));
			him.setAnchor(player.blockPosition());
			level.addFreshEntity(him);
			ManifestationDirector.noteLocation(stand);
			HerobrineMod.LOGGER.info("a glimpse at [{}, {}, {}], {} blocks off",
				stand.getX(), stand.getY(), stand.getZ(),
				(int)Math.sqrt(stand.distSqr(player.blockPosition())));
			return Outcome.PLACED;
		}
		return Outcome.NOTHING_VISIBLE;
	}

	/**
	 * HIM, IN THE PASSAGE, AND THERE IS NO WAY ROUND HIM.
	 *
	 * The third of the three, and the one the cave actually asks for. The
	 * glimpse is half a second and gone before you are sure. The stare is
	 * sixty blocks of open country. This is neither: he is nine to twenty-four
	 * blocks down the tunnel you were walking along, standing still, facing
	 * you, for five to eight seconds — and the tunnel is the only way through.
	 *
	 * IT WORKS BECAUSE OF THE GEOMETRY, not because of anything he does. Above
	 * ground a figure at distance is one of many things on a horizon and the
	 * player can simply walk elsewhere. In a corridor there is no elsewhere.
	 * The stare's rules — a long way off, behind you, judged against a skyline
	 * — mean nothing in a tunnel with two ends, which is exactly why this
	 * cannot share its code and did not deserve to.
	 *
	 * ConfinedPlacement is used because it is genuinely a cave tool: it floods
	 * the space the player is standing in and comes back with somewhere in it,
	 * so what it returns is always down a passage that connects. The rules
	 * about how long he stays and what ends it are this method's own.
	 */
	public static Outcome passage(ServerLevel level, ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return Outcome.BAD_PLAYER;
		}
		if (existsAnywhere(level)) {
			return Outcome.ALREADY_NEARBY;
		}
		if (!ConfinedPlacement.isConfined(level, player)) {
			return Outcome.NO_ROOM_HERE;   // out in the open; that is the stare
		}

		BlockPos spot = ConfinedPlacement.find(level, player);
		if (spot == null) {
			return Outcome.NO_ROOM_HERE;
		}
		// No light check at all, deliberately. A cave a player has torched is
		// still a cave, and refusing to appear in the one they have lit would
		// mean he only ever turns up where they cannot see him.
		HerobrineEntity him = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (him == null) {
			return Outcome.NO_FOOTING;
		}
		double dx = player.getX() - (spot.getX() + 0.5);
		double dz = player.getZ() - (spot.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		him.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);

		// Long enough to be certain, short enough that waiting him out is not a
		// plan. Walking at him still ends it early — the standoff owns that,
		// and being made to back down a tunnel is the best thing that can
		// happen here.
		RandomSource random = level.getRandom();
		him.beGlimpse(100 + random.nextInt(60));
		him.setAnchor(player.blockPosition());
		level.addFreshEntity(him);
		ManifestationDirector.noteLocation(spot);
		HerobrineMod.LOGGER.info("he is in the passage at [{}, {}, {}], {} blocks off",
			spot.getX(), spot.getY(), spot.getZ(),
			(int)Math.sqrt(spot.distSqr(player.blockPosition())));
		return Outcome.PLACED;
	}

	/** Puts him at a chosen spot, already facing the player. */
	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos) {
		return spawnAt(level, player, pos, false);
	}

	private static Outcome spawnAt(ServerLevel level, ServerPlayer player, BlockPos pos,
	                               boolean hunting) {
		HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (herobrine == null) {
			return Outcome.NO_FOOTING;
		}
		// Facing the player from the moment he exists. Turning to look at you
		// afterwards would give away that he had just arrived.
		double dx = player.getX() - (pos.getX() + 0.5);
		double dz = player.getZ() - (pos.getZ() + 0.5);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		herobrine.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		// Where the player is standing right now. Chase him and he comes back
		// to it, so the ground you gave up is the ground he takes.
		herobrine.setAnchor(player.blockPosition());
		if (hunting) {
			herobrine.beginHunt();
		}
		level.addFreshEntity(herobrine);
		// Say where he is, which the stare never did.
		//
		// Every other manifestation reports its position and this one did not,
		// so `/herobrine provoke` answered with the coordinates of whatever
		// sign or page had been left last — a real place, tens or hundreds of
		// blocks from him, with a plausible distance beside it. A tester
		// following that number is looking at empty ground and concluding he
		// never spawned, and there is nothing in the output to suggest
		// otherwise. Of all the ways to lose a week, being confidently told
		// the wrong coordinates is the worst.
		ManifestationDirector.noteLocation(pos);
		arrival(level, pos);
		strike(level, pos);   // DEBUG AID — see markSpawns above
		// A reason to turn around — sometimes. He is still never SEEN
		// arriving; you turn and find him already standing there.
		herobrine.announceArrival();
		return Outcome.PLACED;
	}

	/** True if the position falls inside the player's rough view cone. */
	/**
	 * Could the player see that spot if they turned round?
	 *
	 * Aimed a little above the block, because he stands ON it and it is his
	 * head and shoulders that have to clear the ridge, not his feet. Checking
	 * the ground itself would reject a perfectly good spot just over the brow
	 * of a hill — which is one of the better places for him to be.
	 */
	private static boolean visibleFrom(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 head = new Vec3(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
		return level.clip(new net.minecraft.world.level.ClipContext(eye, head,
			net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, player))
			.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	private static boolean isInFrontOf(ServerPlayer player, BlockPos pos) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toPos = new Vec3(
			pos.getX() + 0.5 - player.getX(),
			pos.getY() - player.getEyeY(),
			pos.getZ() + 0.5 - player.getZ()
		).normalize();
		return look.dot(toPos) > IN_VIEW_DOT;
	}
}
