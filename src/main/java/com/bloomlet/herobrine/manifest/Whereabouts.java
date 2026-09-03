package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.HerobrineEntity;
import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.structure.Dwellings;
import com.bloomlet.herobrine.structure.Ground;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * HE LIVES SOMEWHERE, AND HE IS THERE WHETHER ANYBODY IS LOOKING OR NOT.
 *
 * The single biggest thing wrong with him until now: he SPAWNED. A director
 * decided an event was due and put him twenty-four to forty-eight blocks from a
 * player, and that is why he never once felt like a person. People do not appear
 * near you. People are somewhere, and then they are somewhere else, and one of
 * those places is occasionally near you.
 *
 * So he has a position in the world at all times, and it moves on its own. He
 * keeps a house — the homestead, which the mod already sites and builds — leaves
 * it, wanders, and goes back. None of that requires him to be loaded: what moves
 * is a POSITION, and the entity is only made when somebody gets close enough to
 * see one.
 *
 * WHICH IS THE WHOLE TRICK AND IT IS CHEAP. Minecraft cannot tick an entity two
 * thousand blocks away and nothing here tries to. A player who never goes near
 * him experiences a number changing in a save file. A player who walks into that
 * number meets somebody who has been out here the whole time — and the two are
 * indistinguishable from the inside, which is the only test that matters.
 *
 * IT ALSO DELETES THE HUNT AS AN EVENT. Nothing schedules him any more. He is
 * simply out here, and if he sees you that is a hunt — see HerobrineEntity. The
 * mod stops asking "is it time" and starts asking "where is he", which is a
 * question the player can also ask, and act on.
 */
public final class Whereabouts {
	private Whereabouts() {}

	/** Where he is right now, whether or not anything is rendering him. */
	private static final AttachmentType<Long> AT = AttachmentRegistry
		.createPersistent(HerobrineMod.id("he_is_at"), Codec.LONG);
	/** Where he lives, and what he walks back to. */
	private static final AttachmentType<Long> HOME = AttachmentRegistry
		.createPersistent(HerobrineMod.id("he_lives_at"), Codec.LONG);
	/**
	 * How long he stays in after a hunt.
	 *
	 * Not a cooldown on the event — a cooldown on HIM. He is finished with them,
	 * he has walked home, and he is not coming out for a while. The distinction
	 * matters because the player can see it: they know where he went, and they
	 * know the quiet is him being there rather than a timer somewhere.
	 */
	private static final AttachmentType<Long> INDOORS_UNTIL = AttachmentRegistry
		.createPersistent(HerobrineMod.id("he_is_indoors_until"), Codec.LONG);
	/** The outbuilding, so his land has somewhere to be that is not the house. */
	private static final AttachmentType<Long> SHED = AttachmentRegistry
		.createPersistent(HerobrineMod.id("his_outbuilding"), Codec.LONG);
	/** Whether the first person here has been told where he lives. Once per world. */
	private static final AttachmentType<Boolean> INVITED = AttachmentRegistry
		.createPersistent(HerobrineMod.id("somebody_was_invited"), Codec.BOOL);

	/** Once a second is plenty for somebody walking. */
	private static final int STEP_EVERY = 20;
	/** How far the position drifts per step while nobody is near. */
	private static final int STRIDE = 6;
	/**
	 * Beyond this from home he turns round. He does not emigrate.
	 *
	 * THREE HUNDRED AND TWENTY WAS A COUNTY, NOT A NEIGHBOURHOOD. A random walk
	 * with a turn-back that far out spends almost none of its time near the middle,
	 * so "go to his house and see if he is in" — the one thing the whole address
	 * exists to make possible — came back empty nearly every time.
	 */
	private static final double ROAMS = 140.0;
	/** And past this, half his steps are back toward it. He is not commuting. */
	private static final double SETTLES_AT = 60.0;
	/** Inside this he is on his own doorstep, and mostly stays on it. */
	private static final double IN_THE_YARD = 24.0;
	/** Close enough to a player that he stops being a number. */
	private static final double ARRIVES_AT = 96.0;
	/** And far enough that he can go back to being one. */
	private static final double FORGETS_AT = 140.0;
	/** Two nights in after a hunt. */
	private static final int STAYS_IN = 24000;

	private static int ticks;

	/**
	 * THE LAST LOUD THING SOMEBODY DID, AND WHERE.
	 *
	 * His eyes were doing all the work and they should not be. A figure who notices
	 * you at forty blocks in a wide arc is a searchlight; the tension of walking
	 * near somebody comes from not knowing what gives you away, and the honest
	 * answer in Minecraft is almost never "he looked at me". It is that you dug.
	 *
	 * Breaking a block is the loudest routine thing a player does and the one they
	 * do without thinking — which makes it the perfect giveaway. Crouch past him
	 * all night and he will not see you; take one block out of the wall behind you
	 * and he is walking over.
	 *
	 * One position and one timestamp, world-wide. Not a queue and not per-player:
	 * he is one person and he can only walk toward one noise.
	 */
	private static BlockPos noiseAt;
	private static long noiseWhen = -10000L;
	/** Three seconds of a sound being worth investigating. */
	private static final int NOISE_LASTS = 60;

	/**
	 * HOW OFTEN HE IS ALLOWED TO LEAVE.
	 *
	 * The errand was the fallback for "nothing else to do where I am standing",
	 * which in any open field is most rolls — so he set off for a wood every few
	 * seconds and chained them forever. The log is nothing but "sets off for the
	 * wood" and he was never once at his own house.
	 *
	 * It cannot live on the entity either: he is discarded and rebuilt every time a
	 * player walks out of range, so an entity-side cooldown resets constantly, which
	 * is exactly how the chain kept restarting.
	 *
	 * Four minutes, held out here where he cannot forget it.
	 */
	private static final int BETWEEN_ERRANDS = 4800;
	private static long wentOutAt = -100000L;

	public static boolean mayGoOut(ServerLevel level) {
		long now = level.getGameTime();
		if (now < wentOutAt) {
			wentOutAt = -100000L;      // a different world, and a much younger clock
		}
		return now > wentOutAt + BETWEEN_ERRANDS;
	}

	public static void wentOut(ServerLevel level) {
		wentOutAt = level.getGameTime();
	}

	public static void noise(BlockPos where, long now) {
		noiseAt = where;
		noiseWhen = now;
	}

	/** Where the last thing he could hear happened, or null if it has gone quiet. */
	public static @org.jspecify.annotations.Nullable BlockPos heard(ServerLevel level) {
		if (noiseAt == null || level.getGameTime() > noiseWhen + NOISE_LASTS) {
			return null;
		}
		return noiseAt;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Whereabouts::onTick);
		// Every block anybody takes out, anywhere. Cheap — two field writes — and
		// it is the only sense he has that a player can choose not to trip.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
			(world, player, pos, state, entity) -> {
				if (world instanceof ServerLevel here && !player.isSpectator()) {
					noise(pos, here.getGameTime());
				}
			});
	}

	/** Where he lives. Null until the homestead has been sited. */
	public static @org.jspecify.annotations.Nullable BlockPos home(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(HOME);
		return packed == null ? null : BlockPos.of(packed);
	}

	/** Where he is. Null before he has ever been anywhere. */
	public static @org.jspecify.annotations.Nullable BlockPos at(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(AT);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * He is done with them and he is going home.
	 *
	 * Called when a hunt ends, however it ended. The entity discards itself as it
	 * always has; what survives is the position, and the position walks back.
	 */
	/**
	 * THE PLACES ON HIS LAND, IN THE ORDER HE WALKS THEM.
	 *
	 * He was building a hut in a field, which was the wrong answer to the right
	 * question. He already HAS a house, a shed, forty metres of passage under both
	 * and a tower over the lot — the most interesting property in the world — and
	 * he was ignoring all of it to lay cobblestone somewhere else.
	 *
	 * A person at home does not build another home. They walk around the one they
	 * have: out the front, round the back, down the cellar steps, over to the shed,
	 * out to the thing on the hill, back in. That is a ROUTE, and a route is the
	 * one thing that makes following somebody worth doing, because after two laps
	 * you know where he is going next and you can be there first — or not be.
	 *
	 * Each stop is somewhere a player can also stand, which is the point. Two of
	 * them are underground.
	 *
	 * @return his stops, or empty before he has an address
	 */
	public static java.util.List<BlockPos> haunts(ServerLevel level) {
		BlockPos house = home(level);
		if (house == null) {
			return java.util.List.of();
		}
		java.util.List<BlockPos> route = new java.util.ArrayList<>(6);
		route.add(house.offset(0, 1, 5));            // the yard, facing his own door
		route.add(house.offset(6, -2, 14));          // the head of the cellar steps
		Long shed = level.getServer().overworld().getAttached(SHED);
		if (shed != null) {
			route.add(BlockPos.of(shed));            // the outbuilding
		}
		BlockPos deck = com.bloomlet.herobrine.structure.Spire.site(level);
		if (deck != null) {
			// The FOOT of it, not the deck. Twenty-nine blocks of interior stair is
			// more than vanilla navigation will reliably do, and a leg he never
			// completes is a leg he never comes back from.
			route.add(new BlockPos(deck.getX(),
				Ground.topOf(level, deck.getX(), deck.getZ()) + 1, deck.getZ()));
		}
		route.add(house.offset(-6, 1, -6));          // round the back
		return route;
	}

	/**
	 * The ways between his buildings, and the ground either side of them.
	 *
	 * Three legs: the house to the shed, the house to the foot of the tower, and
	 * the shed to the tower. The third one matters more than it sounds — two legs
	 * from a single hub is a driveway, and a triangle is a place people move around
	 * in. It is the difference between a house with outbuildings and a farm.
	 */
	private static void tracks(ServerLevel over, BlockPos house, RandomSource random) {
		BlockPos door = new BlockPos(house.getX()
			+ com.bloomlet.herobrine.structure.Homestead.width() / 2,
			Ground.topOf(over, house.getX(), house.getZ()), house.getZ() - 4);
		Long shedAt = over.getAttached(SHED);
		BlockPos shed = shedAt == null ? null : BlockPos.of(shedAt);
		BlockPos deck = com.bloomlet.herobrine.structure.Spire.site(over);
		BlockPos tower = deck == null ? null : new BlockPos(deck.getX(),
			Ground.topOf(over, deck.getX(), deck.getZ()), deck.getZ());

		int laid = 0;
		if (shed != null && com.bloomlet.herobrine.structure.Grounds
				.track(over, door, shed, random)) {
			laid++;
		}
		if (tower != null && com.bloomlet.herobrine.structure.Grounds
				.track(over, door, tower, random)) {
			laid++;
		}
		if (shed != null && tower != null && com.bloomlet.herobrine.structure.Grounds
				.track(over, shed, tower, random)) {
			laid++;
		}
		// And the two smaller buildings get the same apron the house does, so the
		// track does not arrive at a shed sitting on bare turf.
		if (shed != null) {
			com.bloomlet.herobrine.structure.Grounds.dress(over, shed, 4, 13, random);
			com.bloomlet.herobrine.structure.Grounds.yard(over, shed,
				net.minecraft.core.Direction.SOUTH, random);
		}
		if (tower != null) {
			com.bloomlet.herobrine.structure.Grounds.dress(over, tower, 6, 16, random);
		}
		HerobrineMod.LOGGER.info("{} tracks laid across his ground", laid);
	}

	/**
	 * DRIVEN OFF, AND HE DOES NOT GO AND SIT IN THE HOUSE.
	 *
	 * He used to walk home and shut the door for two days, which was the right
	 * answer when this was where he lived. It is not where he lives. He comes
	 * through the tower and he goes back the same way, and being beaten is exactly
	 * the occasion for it.
	 *
	 * The absence is much longer than the old lockdown, because it has to be worth
	 * something. You did not survive a night — you took the door off him, and the
	 * quiet afterwards is the receipt.
	 */
	private static final int GONE_THROUGH = 96000;

	public static void goesThrough(ServerLevel level) {
		ServerLevel over = level.getServer().overworld();
		BlockPos deck = com.bloomlet.herobrine.structure.Spire.site(over);
		BlockPos back = deck != null ? deck : home(over);
		if (back == null) {
			return;
		}
		over.setAttached(AT, back.asLong());
		over.setAttached(INDOORS_UNTIL, over.getGameTime() + GONE_THROUGH);
		HerobrineMod.LOGGER.info(
			"he has gone back through at [{}, {}, {}] — four days before anybody sees him",
			back.getX(), back.getY(), back.getZ());
	}

	/**
	 * THE FIRST PERSON HERE IS GIVEN HIS ADDRESS.
	 *
	 * A map, in their inventory, the moment the world has one. Which sounds like
	 * the mod giving away its own secret and is the opposite: it is the invitation,
	 * and the whole event depends on somebody choosing to walk toward it.
	 *
	 * WITHOUT IT HE IS A COINCIDENCE. He lives three hundred blocks out and wanders
	 * a circle around it, so a player who happens never to go that way experiences
	 * a mod that does nothing — and one who blunders into him at hour six
	 * experiences an ambush with no story attached. Handing over the address turns
	 * both of those into a decision: you know where he is, you have known since the
	 * first minute, and going there is on you.
	 *
	 * ONE PERSON, ONE TIME, PER WORLD. Not everybody who joins, and not again on
	 * relog — the second copy is somebody else's business to be told about, out
	 * loud, which is how everything in this mod is supposed to travel.
	 *
	 * Scale four rather than the grave map's three, because it has to show both
	 * ends: the house can be five hundred blocks off, and a map with the reader
	 * standing off the edge of it is a map of nowhere.
	 */
	private static void invite(MinecraftServer server, ServerLevel over, BlockPos house) {
		if (over.getAttachedOrElse(INVITED, false)) {
			return;
		}
		ServerPlayer first = server.getPlayerList().getPlayers().stream()
			.filter(p -> !p.isSpectator()).findFirst().orElse(null);
		if (first == null) {
			return;
		}
		over.setAttached(INVITED, true);
		net.minecraft.world.item.ItemStack map = net.minecraft.world.item.MapItem.create(
			over, house.getX(), house.getZ(), (byte) 4, true, true);
		net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
			map, house, "+",
			net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_MARKER);
		// FROM A FRIEND. It used to be named with one of HIS lines — "I would not" —
		// which nobody could read as anything, least of all as an invitation. It is
		// Addexio's map: he sent for you, and the intro says so.
		map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal(
				"from a fellow friend — " + house.getX() + ", " + house.getZ()));
		if (!first.getInventory().add(map)) {
			first.drop(map, false);
		}
		HerobrineMod.LOGGER.info("{} was given the way to [{}, {}]",
			first.getName().getString(), house.getX(), house.getZ());
	}

	/**
	 * What is written on it.
	 *
	 * Nobody signs it and nothing explains it. A map that says "Herobrine's house"
	 * is a quest marker; a map that says "somebody lives out here" is a thing the
	 * player has to decide whether to believe.
	 */
	private static final String[] SAID = {
		"somebody lives out here",
		"there is a house at the mark",
		"do not go at night",
		"the last man back had no hands",
		"we buried what came back",
		"i would not",
	};

	/**
	 * THE REST OF HIS PROPERTY: A SHED, AND WHAT RUNS BETWEEN THEM.
	 *
	 * One building is a diorama. Two, forty-odd blocks apart, with a tunnel joining
	 * them under the ground, is somewhere somebody has been living — and the second
	 * one is deliberately not a house, so finding it re-frames the first.
	 *
	 * THE PASSAGE ARRIVES AT THE CELLS ON PURPOSE. Homestead.build already digs an
	 * undercroft at a fixed offset from itself and shuts things in down there, and
	 * until now the only way to that was through his front door. Boring the tunnel
	 * from the shed to the undercroft means the shed is a BACK WAY IN — a player who
	 * finds the outbuilding first walks the whole length of it in the dark and
	 * arrives underneath his house, in the prison, from the wrong side.
	 *
	 * Which is the best thing on his land, and it costs one line: the destination.
	 */
	private static void steading(ServerLevel over, BlockPos house) {
		RandomSource random = over.getRandom();
		// Forty to seventy blocks. Near enough to be the same property, far enough
		// that you can stand at one and not see the other through the trees.
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = 40.0 + random.nextDouble() * 30.0;
			int x = house.getX() + (int) Math.round(Math.cos(angle) * range);
			int z = house.getZ() + (int) Math.round(Math.sin(angle) * range);
			if (!Ground.dry(over, x, z)) {
				continue;
			}
			over.getChunk(x >> 4, z >> 4);
			BlockPos cellar = com.bloomlet.herobrine.structure.Outbuilding.build(
				over, new BlockPos(x, over.getSeaLevel(), z), random);
			over.setAttached(SHED,
				new BlockPos(x, Ground.topOf(over, x, z) + 1, z).asLong());
			// The undercroft's mouth is a fixed offset from the homestead — see
			// Homestead.build. Aiming at it is what turns two buildings and a hole
			// into one place.
			// THE UNDERCROFT IS ONLY THREE BELOW THE HOUSE, and running the passage
			// straight at it would have the whole thing skimming just under the
			// topsoil — which is not a cave system, it is a trench with a roof.
			//
			// So it dives first. A waypoint twenty-two blocks under the halfway
			// point sends it properly down, along at depth, and back up to arrive at
			// the cells — and the climb at the end is the part a player feels,
			// because they have been walking downhill in the dark for a minute and
			// suddenly the floor is rising toward something.
			BlockPos cells = house.offset(6, -3, 14);
			BlockPos deep = new BlockPos(
				(cellar.getX() + cells.getX()) / 2,
				Math.max(over.getMinY() + 12, Math.min(cellar.getY(), cells.getY()) - 22),
				(cellar.getZ() + cells.getZ()) / 2);
			com.bloomlet.herobrine.structure.Passage.bore(over, cellar, cells, deep, random);
			// AND THE TOWER, which is the only thing on his land visible from off
			// it. Raised last so it can be sited relative to the house rather than
			// the other way round — the house is where he lives and the tower is
			// something that was already here.
			com.bloomlet.herobrine.structure.Spire.raise(over, house, random);
			// AND NOW THEY ARE ONE PROPERTY RATHER THAN THREE BUILDINGS.
			//
			// This is the cheapest thing in the file and close to the most valuable.
			// The house, the shed and the tower have always been sited relative to
			// each other and have never been JOINED — so the player walked forty
			// blocks of untouched forest between two of his buildings and read them
			// as two separate finds. A worn track between them is what makes the
			// second one his as well.
			//
			// Laid last, after everything is standing, because a path has to be able
			// to see what it is going round.
			tracks(over, house, random);
			// The door goes at the bottom of the passage, which is a position we
			// chose rather than found — see below for why it is not called here.
			door(over, house, random);
			return;
		}
		HerobrineMod.LOGGER.info("nowhere dry for the outbuilding — he keeps the one house");
		// AND THE DOOR STILL GOES IN. THIS IS WHY THE LAST WORLD HAD NO WAY OUT.
		//
		// Sanctum used to be the last line inside that loop, so it inherited the
		// loop's failure: one coastal homestead with nowhere dry for a shed, and
		// the mod shipped a save with no portal in it anywhere. The most important
		// structure in the game was a passenger on the least important one.
		//
		// The shed and the passage are scenery. The door is the point, and it is
		// the one thing that has to exist in every world regardless of what the
		// terrain would or would not take.
		com.bloomlet.herobrine.structure.Spire.raise(over, house, random);
		door(over, house, random);
	}

	/**
	 * THE DOOR, AND THE WAY DOWN TO IT.
	 *
	 * A spiral out of the cellar, thirty blocks of it, and the room carved around
	 * wherever it lands. In that order on purpose: the stair builds its own lining
	 * as it goes, and a room hollowed afterwards takes that lining back out of its
	 * own middle. Built the other way round there would be a four-by-four stone
	 * box standing on top of the portal.
	 *
	 * And the depth is measured, not assumed — a homestead already low in the
	 * world gets a shorter stair rather than one that ends in bedrock.
	 */
	private static final int DOWN_TO_IT = 30;

	private static void door(ServerLevel over, BlockPos house, RandomSource random) {
		BlockPos cells = house.offset(6, -3, 14);
		int drop = Math.min(DOWN_TO_IT, Math.max(12, cells.getY() - (over.getMinY() + 16)));
		BlockPos landing = com.bloomlet.herobrine.structure.Descent.stair(
			over, cells.offset(-1, 0, -1), drop,
			net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
			random);
		if (landing == null) {
			landing = cells.below(drop);
		}
		com.bloomlet.herobrine.structure.Sanctum.raise(over, landing.above(2), random);
	}

	private static void onTick(MinecraftServer server) {
		if (com.bloomlet.herobrine.wrath.Wrath.removed(server)) {
			return;      // Removed Herobrine. See Wrath.removed.
		}
		if (++ticks % STEP_EVERY != 0 || !Config.get().enabled) {
			return;
		}
		// HIS OWN WORLD FIRST, BECAUSE THAT IS WHERE HE LIVES.
		//
		// Ahead of every line of overworld bookkeeping below, and independent of it:
		// the homestead, the tower and the walk are all still sited and still
		// tracked, because they are the INVITATION. He is simply not in them.
		overTheKeep(server);

		ServerLevel over = server.overworld();

		// HIS HOUSE, ONCE THE MOD HAS PUT ONE DOWN. Until the homestead is sited
		// he has nowhere to live and none of this runs — which is correct: before
		// there is a house there is no him to be out of it.
		if (over.getAttached(HOME) == null) {
			// The SITE rather than the raised building — chosen during RUMOUR, long
			// before anybody has been near it. He does not wait for his own house
			// to be built before he exists.
			BlockPos raised = Dwellings.origin(over);
			if (raised == null) {
				raised = Dwellings.homesteadSite(over);
			}
			if (raised == null) {
				return;
			}
			raised = new BlockPos(raised.getX(),
				Ground.topOf(over, raised.getX(), raised.getZ()), raised.getZ());
			// AND THE HOUSE GOES UP NOW, NOT WHEN SOMEBODY WALKS PAST IT.
			//
			// Dwellings raises each place on approach, which is the standard trick
			// and is undetectable for a building nobody has any reason to expect. It
			// is detectable for THIS one: he lives here and walks out from it, so a
			// player who meets him three hundred blocks from home and follows him
			// back would arrive at his address and watch it appear.
			//
			// He was here before you. That has to be literally true of the one
			// building the whole mod now hangs off, so it is built at world
			// creation — four chunks generated once, before anybody is near enough
			// for the hitch to matter, and from then on it is simply somewhere that
			// exists.
			if (!Dwellings.raised(over)) {
				over.getChunk(raised.getX() >> 4, raised.getZ() >> 4);
				if (Dwellings.raise(over, raised)) {
					BlockPos built = Dwellings.origin(over);
					if (built != null) {
						raised = built;
					}
					HerobrineMod.LOGGER.info("his house was already standing before anybody looked");
				}
			}
			steading(over, raised);
			over.setAttached(HOME, raised.asLong());
			over.setAttached(AT, raised.asLong());
			HerobrineMod.LOGGER.info("he lives at [{}, {}, {}]",
				raised.getX(), raised.getY(), raised.getZ());
			invite(server, over, raised);
		}

		BlockPos here = at(over);
		BlockPos house = home(over);
		if (here == null || house == null) {
			return;
		}

		// A SAVE THAT CAME UP BEFORE THE TOWER COULD BE STOOD.
		//
		// Siting it used to be allowed to fail, and any world where it did is
		// carrying a homestead with nothing on the skyline and no way out of the
		// mod at all. Nothing would ever ask again — steading runs once, at the
		// moment he gets an address, and that moment has passed. So it is asked
		// for here instead, where it costs one attachment read a second and fixes
		// itself the first time such a world is loaded.
		if (com.bloomlet.herobrine.structure.Spire.site(over) == null) {
			over.getChunk(house.getX() >> 4, house.getZ() >> 4);
			com.bloomlet.herobrine.structure.Spire.raise(over, house, over.getRandom());
		}

		weather(server, over, house);
		fog(server, over, house);

		// Already out there in person? Then the entity owns his position and this
		// only has to keep the record straight behind it.
		HerobrineEntity him = loaded(over);
		if (him != null) {
			over.setAttached(AT, him.blockPosition().asLong());
			return;
		}

		if (over.getGameTime() < over.getAttachedOrElse(INDOORS_UNTIL, 0L)) {
			return;      // in, and not coming out
		}

		// AND NOBODY MEETS HIM OUT HERE ANY MORE.
		//
		// This used to be the whole point of the class: come within ninety-six
		// blocks of where he had walked to and he was standing there. It is what
		// made him a resident of the overworld, and everything awkward about the mod
		// came out of that one fact — the strongest thing in the game on the wrong
		// side of the door, so the overworld had to be survivable, so he had to be
		// defanged; and the traces and the stares could not run because he was
		// already out there being looked at.
		//
		// What is on this side now is his HOUSE. Empty, weathered, raining on, with
		// his address on a map in your inventory. Walk in and there is nobody home,
		// and the only place he can be is through the tower.
		//
		// The overworld is not off limits to him — TheHunt and the manifestations
		// still put him here, and that is what a visit IS. He just does not live
		// here, so `here` is a record of where his walk has got to rather than a
		// place he is about to appear from. See materialise, kept for the visit.

		// Otherwise he is walking, and nobody is watching, so it costs one BlockPos.
		over.setAttached(AT, wander(over, here, house).asLong());
	}

	// ---- THE SKY OVER HIS ADDRESS ----------------------------------------
	//
	// The one place in the overworld that is never having a nice day.
	//
	// Skies.turn already exists and it is EVENT weather — the sky goes over
	// because of something the players just did, which is why it lands. This is
	// the opposite and needs to be: a CLIMATE, and only over one spot. You come
	// over the last ridge toward the homestead and the tower and it is raining
	// there, and it was raining before you arrived, and it will be raining when
	// you leave. Nothing announced it. That is the whole effect.
	//
	// Minecraft weather is per-dimension, so "local" is a fiction maintained by
	// arming it while somebody is close and letting it lapse when they are not.
	// Forty seconds, refreshed every second, so it clears on its own about half a
	// minute after the last person walks out of range — long enough that leaving
	// does not feel like flipping a switch.

	// ---- HIS GROUND -------------------------------------------------------
	//
	// FOG, AND IT IS ALREADY BUILT — it was just world-wide.
	//
	// Atmosphere on the client already drives FOG_COLOR, FOG_END_DISTANCE, the sky
	// and cloud distances that have to agree with it, the light level and the cloud
	// height. All of it good, all of it keyed on one thing: the global phase. So the
	// whole world got the same weather, and there was no way to make one place
	// worse than everywhere else.
	//
	// This is the missing input. One float per player, nought to one, "how far into
	// his ground you are" — and the client folds it into the same layers. The house
	// is foggy on the first day, at RUMOUR, before the story has moved at all, and
	// the walk toward it is the world quietly closing in. Nothing announces it.
	//
	// THE SERVER OWNS THE CURVE, not the client, and the client is never told where
	// he lives. It gets a number between nought and one and no coordinates — same
	// reasoning as SHOWN_PHASE carrying the ordinal rather than the wrath total.
	// A datamined "distance to his house" would be a compass.

	/**
	 * How far into his ground somebody is, for the fog. Nought to one.
	 *
	 * Not persistent — it is recomputed every second from where they are standing,
	 * and a saved copy would only ever be wrong on load.
	 */
	public static final AttachmentType<Float> NEAR_HIS = AttachmentRegistry
		.<Float>builder()
		.syncWith(net.minecraft.network.codec.ByteBufCodecs.FLOAT.cast(),
			net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(HerobrineMod.id("near_his"));

	/** Nothing at all this far out. */
	private static final double FOG_FAR = 200.0;
	/** And everything by here, which is well outside the yard. */
	private static final double FOG_NEAR = 40.0;

	private static void fog(MinecraftServer server, ServerLevel over, BlockPos house) {
		BlockPos tower = com.bloomlet.herobrine.structure.Spire.site(over);
		ServerLevel his = server.getLevel(
			com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD);
		BlockPos keep = his == null
			? null : com.bloomlet.herobrine.structure.Keep.site(his);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			double away = Double.MAX_VALUE;
			if (player.level() == over) {
				// EITHER OF THEM. The tower is half the point — it is on the skyline
				// from further off than the house is, and arriving at the way out
				// through thickening fog is the better of the two approaches.
				away = Math.sqrt(player.blockPosition().distSqr(house));
				if (tower != null) {
					away = Math.min(away, Math.sqrt(player.blockPosition().distSqr(tower)));
				}
			} else if (keep != null && player.level() == his) {
				away = Math.sqrt(player.blockPosition().distSqr(keep));
			}
			float in = thickness(away);
			// Only when it has actually moved. This runs once a second for every
			// player on the server and each write is a packet.
			Float known = player.getAttached(NEAR_HIS);
			if (known == null || Math.abs(known - in) > 0.01F) {
				player.setAttached(NEAR_HIS, in);
			}
		}
	}

	/**
	 * Smoothstep rather than linear, and that is not decoration.
	 *
	 * A straight ramp has a corner at each end — the fog starts and stops changing
	 * on a specific step, and the eye finds that instantly: it reads as a radius,
	 * which is to say as a mechanic. Smoothed at both ends there is no moment when
	 * it begins.
	 */
	private static float thickness(double away) {
		if (away >= FOG_FAR) {
			return 0.0F;
		}
		if (away <= FOG_NEAR) {
			return 1.0F;
		}
		float t = (float) ((FOG_FAR - away) / (FOG_FAR - FOG_NEAR));
		return t * t * (3.0F - 2.0F * t);
	}
	// ---- END HIS GROUND ---------------------------------------------------

	/** How close counts as being at his address. */
	private static final double STORM_NEAR = 96.0;
	/** And how long each arming lasts, refreshed while anybody is inside that. */
	private static final int STORM_HOLDS = 800;
	private static boolean stormed;

	private static void weather(MinecraftServer server, ServerLevel over, BlockPos house) {
		if (!Config.get().weather) {
			return;
		}
		// NOT AT THE FIRST HOUSE. The farm is where you meet Addexio and read book
		// one, and a storm parked over it from the first visit told the whole story
		// before it started. The sky turns when the town has been found.
		if (!com.bloomlet.herobrine.wrath.Wrath.phase(server)
				.atLeast(com.bloomlet.herobrine.wrath.Phase.TRESPASSER)) {
			stormed = false;
			return;
		}
		BlockPos tower = com.bloomlet.herobrine.structure.Spire.site(over);
		boolean near = false;
		for (ServerPlayer player : over.players()) {
			if (player.blockPosition().closerThan(house, STORM_NEAR)
				|| (tower != null && player.blockPosition().closerThan(tower, STORM_NEAR))) {
				near = true;
				break;
			}
		}
		if (!near) {
			stormed = false;
			return;
		}
		// Only when it is not already going. A hunt's storm is nine to eleven
		// minutes and re-arming over the top of it would cut it to forty seconds —
		// the loud weather outranks the resident weather.
		// RAIN FIRST, THUNDER LATER.
		//
		// This armed a full thunderstorm — rain AND thunder — inside ninety-six
		// blocks of the first building in the game, from the first night, forever,
		// refreshed every time it ran out. Thunder is the loudest weather Minecraft
		// has and it darkens the sky enough to spawn mobs in daylight, and it was
		// being used as the ambience of a farmhouse in the phase called RUMOUR.
		//
		// It also made the whole approach unreadable: rain, thunder, fog and a grey
		// sky all arriving together at two hundred blocks means the player has
		// nothing left to escalate INTO.
		//
		// Rain is enough to say this ground is wrong. The thunder waits until he has
		// earned it.
		boolean thunder = com.bloomlet.herobrine.wrath.Wrath.phase(server)
			.atLeast(com.bloomlet.herobrine.wrath.Phase.MIMIC);
		if (!over.isThundering()) {
			server.setWeatherParameters(0, STORM_HOLDS, true, thunder);
		}
		if (!stormed) {
			stormed = true;
			HerobrineMod.LOGGER.info("somebody is at his address — it is raining there");
		}
	}
	// ---- END THE SKY ------------------------------------------------------

	/**
	 * A step, on foot, with a leash to his own house.
	 *
	 * Deliberately a drunk walk rather than a route: he is not going anywhere and
	 * the only rule is that he does not wander off the map. Past three hundred and
	 * twenty blocks from home the step is biased back, which over a night reads as
	 * somebody who has a place and returns to it.
	 */
	private static BlockPos wander(ServerLevel level, BlockPos here, BlockPos house) {
		RandomSource random = level.getRandom();
		double out = Math.sqrt(here.distSqr(house));
		// AND HOME IS A PLACE HE STAYS, NOT A POINT HE PASSES THROUGH.
		//
		// A pull toward the middle still only makes the middle the busiest square on
		// a walk he never stops taking, and "go to his house and see if he is in"
		// was still coming back empty. A person at home is AT HOME: most steps are
		// no step at all.
		//
		// Two in three ticks inside the yard he simply does not move, which turns
		// the address into somewhere he accumulates rather than somewhere he crosses
		// — and turns walking up to his door into a real way of finding him.
		if (out < IN_THE_YARD && random.nextInt(3) != 0) {
			return here;
		}
		double angle;
		// A hard fence at the edge and nothing inside it is still a random walk, and
		// a random walk is almost never where it started. So the pull comes on
		// gradually: free inside sixty, one step in two aimed home beyond that, and
		// every step home past the fence.
		if (out > ROAMS || (out > SETTLES_AT && random.nextBoolean())) {
			angle = Math.atan2(house.getZ() - here.getZ(), house.getX() - here.getX())
				+ (random.nextDouble() - 0.5);
		} else {
			angle = random.nextDouble() * Math.PI * 2.0;
		}
		int x = here.getX() + (int) Math.round(Math.cos(angle) * STRIDE);
		int z = here.getZ() + (int) Math.round(Math.sin(angle) * STRIDE);
		return new BlockPos(x, Ground.topOf(level, x, z), z);
	}

	// ---- HE IS OVER THE KEEP ----------------------------------------------

	/** How far above the keep floor he is put when somebody arrives. */
	/**
	 * How far over the site he hangs — AND IT WAS INSIDE THE BUILDING.
	 *
	 * Twenty-four, measured from Keep.site(), which carries the SURFACE height. The
	 * keep is KEEP_HEIGHT 32 courses on a motte of MOTTE 6, so its roof is
	 * thirty-eight above that surface, and the tutorial-castle blueprint is
	 * forty-nine tall with its ground layer eleven up — thirty-eight again.
	 *
	 * So he was being spawned eleven blocks INSIDE the great hall, in stone. He
	 * prowls out of it, which is why this was never obvious, but the first thing
	 * anybody saw of him was a figure clipping out of a roof.
	 *
	 * Forty-four clears both by six.
	 */
	private static final double OVER_THE_KEEP = 44.0;
	private static boolean announced;

	/**
	 * Anybody in his world finds him already flying over his own castle.
	 *
	 * Placed rather than spawned-in-view — the oldest rule in the mod is that he is
	 * never seen arriving, and here it is easy to honour: the landing is a long way
	 * from the keep, and by the time anybody can see that far he has been circling
	 * it for a minute.
	 *
	 * Keyed on the keep being sited, so before Keep has built anything there is
	 * nobody there, which is correct — a castle with an owner and no castle is not a
	 * thing that can be arranged.
	 */
	private static void overTheKeep(MinecraftServer server) {
		ServerLevel his = server.getLevel(
			com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD);
		if (his == null || his.players().isEmpty()) {
			announced = false;
			return;
		}
		// THE LEVEL, AND NOTHING ELSE. See HerobrineEntity.outInTheOverworld for what
		// the old guard here actually asked and why it never said yes.
		java.util.List<HerobrineEntity> there = HerobrineEntity.all(his);
		if (!there.isEmpty()) {
			// AND IT REPAIRS ITSELF, because a save that ran the broken version has
			// a crowd in it and no amount of correctness from here on removes them.
			// Same shape as the tower repair further up this file: ask the cheap
			// question every second and fix it the first time somebody loads.
			if (there.size() > 1) {
				for (int i = 1; i < there.size(); i++) {
					there.get(i).discard();
				}
				HerobrineMod.LOGGER.warn(
					"there were {} of him over the keep — {} sent back", there.size(),
					there.size() - 1);
			}
			return;
		}
		BlockPos keep = com.bloomlet.herobrine.structure.Keep.site(his);
		if (keep == null) {
			return;
		}
		// AND IF THE KEEP IS NOT LOADED, WE CANNOT SEE HIM, SO WE DO NOT MAKE ANOTHER.
		//
		// HerobrineEntity.all asks the level's entity index, and an index only holds
		// LOADED entities. That was always true and never mattered, because the keep
		// used to be sited eighty to a hundred blocks from the crossing — inside
		// anybody's view distance, so he was loaded whenever anybody was over there
		// and the emptiness check was honest.
		//
		// Moving the castle two hundred and forty to three hundred and forty blocks
		// off the city broke that in one line. At three hundred and one blocks the
		// keep is outside a sixteen-chunk view distance, all() comes back empty
		// whether he is there or not, and this spawns another. Every step. The log
		// read "there were 23 of him over the keep — 22 sent back".
		//
		// The self-heal above was doing its job perfectly and cleaning up after a
		// bug that had not existed when it was written.
		//
		// Absence of evidence is the whole problem, so the fix is to refuse to
		// answer: if the chunk is not there, say nothing and try again in a second.
		if (!his.isLoaded(keep)) {
			return;
		}
		HerobrineEntity him = ModEntities.HEROBRINE.create(his, EntitySpawnReason.EVENT);
		if (him == null) {
			return;
		}
		// FORTY-FOUR UP FOR THE ENTRANCE. ON THE FLOOR IF THE FIGHT HAS STARTED.
		//
		// Once the first blow has bound him he does not fly, so a fresh one of him
		// spawned in the sky would simply fall forty-four blocks onto his own roof.
		// He is put down at the keep instead, and the Duel walks him into a room.
		double y = Reckoning.bound(his) ? keep.getY() + 1.0 : keep.getY() + OVER_THE_KEEP;
		him.snapTo(keep.getX() + 0.5, y, keep.getZ() + 0.5,
			his.getRandom().nextFloat() * 360.0F, 0.0F);
		him.beginProwl();
		his.addFreshEntity(him);
		if (!announced) {
			announced = true;
			HerobrineMod.LOGGER.info("he is over the keep at [{}, {}, {}]",
				keep.getX(), (int) y, keep.getZ());
		}
	}
	// ---- END OVER THE KEEP ------------------------------------------------

	/** The one of him, if he is currently a real thing somewhere. */
	/** The third copy of the world-sized sweep. See HerobrineEntity.all. */
	private static @org.jspecify.annotations.Nullable HerobrineEntity loaded(ServerLevel level) {
		for (HerobrineEntity him : HerobrineEntity.all(level)) {
			return him;
		}
		return null;
	}

	private static @org.jspecify.annotations.Nullable ServerPlayer nearest(
			MinecraftServer server, ServerLevel over, BlockPos here) {
		ServerPlayer best = null;
		double nearest = Double.MAX_VALUE;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level() != over || player.isSpectator()) {
				continue;
			}
			double away = player.distanceToSqr(here.getX(), here.getY(), here.getZ());
			if (away < nearest) {
				nearest = away;
				best = player;
			}
		}
		return best;
	}

	/** Far enough away that he can stop being an entity again. */
	public static boolean strayedOffScreen(ServerLevel level, HerobrineEntity him) {
		// THE LEVEL HE IS IN, not the overworld. Hard-coding server.overworld() here
		// was harmless while that was the only place he ever stood; now that he
		// lives on the far side of the way it would ask how near the nearest
		// OVERWORLD player is to a man flying over his own castle, get "nobody",
		// and forget him with somebody standing underneath.
		ServerPlayer near = nearest(level.getServer(), level, him.blockPosition());
		return near == null
			|| near.distanceToSqr(him) > FORGETS_AT * FORGETS_AT;
	}
}
