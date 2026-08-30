package com.bloomlet.herobrine.structure;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.Phase;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Where he lived, and when it appears.
 *
 * The house has ONE position per world, fixed by the world seed, decided
 * before anybody goes looking. That matters more than it sounds: a house that
 * appeared near whoever happened to wander furthest would be a house that
 * follows the player, and players work that out immediately. This one has
 * always been there. Two people on a server walk to the same coordinates and
 * find the same building, and a player who reads the seed can find it in a
 * copy of the world — which is exactly the kind of consistency that makes a
 * place feel like part of the map rather than part of the mod.
 *
 * It is only BUILT when somebody gets close, because blocks cannot be placed
 * in chunks that are not loaded and forcing them open across a thousand blocks
 * to furnish a room nobody is in would be indefensible. The position is the
 * real thing; the blocks are just what happens when you arrive.
 *
 * Deliberately not gated on wrath. Everything else in this mod is paced, and
 * this is the one thing that is not: it does not wait for the player to earn
 * it and it does not care what phase they are in. If they walk far enough on
 * their first day, it is there on their first day. It was there before them.
 */
public final class Dwellings {
	private Dwellings() {}

	/** Set once the blocks exist, so it is never built twice. */
	public static final AttachmentType<Boolean> RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_raised"), Codec.BOOL);

	/** Where it went, once it went somewhere. */
	public static final AttachmentType<Long> ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("homestead_origin"), Codec.LONG);

	public static final AttachmentType<Boolean> THRESHOLD_RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_raised"), Codec.BOOL);

	public static final AttachmentType<Long> THRESHOLD_ORIGIN =
		AttachmentRegistry.createPersistent(HerobrineMod.id("threshold_origin"), Codec.LONG);

	/**
	 * Eight bytes each, and not nine.
	 *
	 * These are spelled-out words in hex because a salt you can read is a salt
	 * you can tell apart at a glance — but a long is eight bytes, and the first
	 * attempt spelled longer words than that and would not compile.
	 */
	/**
	 * The town, and it had no way of existing until now.
	 *
	 * Township.raise was only ever called from /herobrine town here, so on an
	 * ordinary world the whole settlement — walls, hall, forge, church and the
	 * chamber under the square — simply never appeared. It was advertised and
	 * unreachable, which is the worst of both.
	 *
	 * Sited nearer than any of the houses, because it is the one thing here
	 * that is meant to be FOUND rather than sought: a walled town with people
	 * in it, at the edge of where somebody's first world reaches, and then
	 * everything wrong with it discovered afterwards.
	 */
	private static final AttachmentType<Boolean> TOWN_RAISED =
		AttachmentRegistry.createPersistent(HerobrineMod.id("town_raised"), Codec.BOOL);

	/** Far enough to be a journey, near enough to be reachable on foot. */

	/** Build when somebody is this close. Inside a default simulation radius. */
	/**
	 * How close somebody has to get before it builds.
	 *
	 * Raised from 112. A building is one point at one random bearing eleven
	 * hundred blocks out, and 112 asks a player to pass through a 224-block
	 * window on a circle 7,000 blocks around — which a server walking in
	 * straight lines will essentially never do, and did not.
	 *
	 * 192 is still inside a default simulation radius, so it cannot try to
	 * build in chunks the server is not ticking, and it nearly quadruples the
	 * chance of an ordinary journey catching one.
	 */
	/**
	 * How close somebody has to get before it builds.
	 *
	 * Chunks have to be ticking for this to be safe, so it stays inside a
	 * default simulation radius.
	 */
	private static final int RAISE_RANGE = 192;
	private static final int CHECK_INTERVAL = 40;

	/**
	 * How far from the players a new building is put.
	 *
	 * Far enough to be a walk with a reason, near enough that a server which
	 * has settled in one valley will actually meet it. The old scheme sited
	 * everything on a ring around WORLD SPAWN, eleven hundred to thirty-six
	 * hundred blocks out, which quietly assumed the players would explore
	 * outward for hours — and a group that builds a base together and stays
	 * near it never went anywhere near any of them.
	 */
	/**
	 * How much further than its own band a place is allowed to be ignored.
	 *
	 * Was a flat 1400, which stopped working the moment the bands started
	 * climbing: the threshold sites out to 1300, so a flat threshold would have
	 * called it abandoned almost the instant it was chosen. Relative to the
	 * band, it means the same thing at every distance — "further away than
	 * anybody who was coming would be".
	 */
	private static final int ABANDONED_MARGIN = 620;

	private static int tickCounter;

	/**
	 * The five houses, the town, and when each is allowed to exist.
	 *
	 * ONE PLACE PER PHASE, IN ORDER, AND NEVER TWO AT ONCE. Six phases and six
	 * buildings is not a coincidence any more — each phase brings exactly one
	 * new place, so every time the world gets worse there is also somewhere new
	 * out there, and the two arrive together. Two of them used to share RUMOUR,
	 * which spent the opening move twice.
	 *
	 * The order is the story. The homestead first, because it has to establish
	 * what a home of his looks like before anything can be measured against it.
	 * Then the town, arriving exactly when he starts being SEEN — the one place
	 * with living people in it, found at the moment the world stops being
	 * ordinary. Then the four buildings that are each less like somewhere a
	 * person lived than the last, and the threshold at the end, which is the
	 * only one with an answer in it.
	 *
	 * AND THE NEXT IS NOT SITED UNTIL THE LAST HAS BEEN FOUND. That is what
	 * makes it readable rather than scattered: a player cannot stumble into the
	 * gaol before the tower and wonder what they missed, because until the
	 * tower is standing the gaol does not exist. Skipping ahead with
	 * /herobrine wrath does not skip the sequence either — it only unlocks how
	 * far it is allowed to get.
	 */
	// ---- THE CHAIN ---------------------------------------------------------
	//
	// A MAP IN EACH HOUSE TO THE NEXT ONE.
	//
	// The sequence already worked and nothing pointed along it. A player found the
	// homestead, the story moved, the town was sited four hundred blocks away — and
	// the only thing telling them so was a road, if the terrain had allowed one. The
	// distances climb deliberately (280 out for the first, 800 for the last), which
	// is right for the journey and hopeless as a search.
	//
	// So each place is a signpost to the one after it. Not handed over, FOUND: it is
	// in a chest inside the building you have just walked into, which makes the
	// building the reward rather than a waypoint, and makes the chain something the
	// player assembles rather than a quest marker they follow.
	//
	// TIMED ON THE SITING, NOT THE BUILDING, and that ordering is the whole trick.
	// Place N+1 does not exist while you are standing in place N — it is not sited
	// until this chapter has had its hour. So the map cannot be placed when N is
	// built. It is placed the moment N+1 is DECIDED, into a building that is already
	// standing and probably already explored — which means the chest you looked in
	// an hour ago has something in it now. That is a better feeling than finding it
	// first time and it costs nothing to arrange.

	/** How far around the last building to look for somewhere to leave it. */
	private static final int LOOKS_FOR_A_CHEST = 24;

	private static void leaveTheWay(ServerLevel over, Place next, BlockPos to) {
		Place[] all = Place.values();
		if (next.ordinal() == 0) {
			return;      // nothing stands before the homestead
		}
		Place from = all[next.ordinal() - 1];
		Long where = over.getAttached(from.site);
		if (where == null || !Boolean.TRUE.equals(over.getAttached(from.up))) {
			return;      // the one before it was never built; nothing to leave it in
		}
		BlockPos anchor = BlockPos.of(where);
		// THE FIRST ONE IS ON TOP OF THE TOWER, AND THAT IS WORTH THE SPECIAL CASE.
		//
		// Every other link in the chain can sit in whatever cupboard the building
		// already has, because by then the player knows they are following
		// something. The FIRST one has to teach them that there is a chain at all,
		// and a map found in a kitchen drawer teaches nothing — it reads as loot.
		//
		// The tower is the only thing on his land visible from off it. You see it
		// from the ridge before you see the house, you climb twenty-nine blocks of
		// interior stair to get to the deck, and there is one chest at the top with
		// one thing in it. Nobody mistakes that for loot. It is the difference
		// between finding a map and being GIVEN one.
		// AND SOMETHING HAPPENED ON THE ROAD BETWEEN THEM.
		//
		// This is the one moment in the mod that knows BOTH ends of a journey — the
		// place the player is standing in and the place the map is about to point
		// at. Everything the mod has to say lives inside a building, so the four
		// hundred blocks between two of them has always been the loading screen.
		//
		// Laid before the map goes in the chest rather than after, so by the time
		// anybody has the map the road already has things on it. See Wayside.
		Wayside.lay(over, anchor, to, over.getRandom());

		net.minecraft.world.level.block.entity.BlockEntity holder = switch (from) {
			case HOMESTEAD -> onTheTower(over);
			// AND THE TOWN'S GOES DOWN WITH THE PEOPLE WHO ARE STILL ALIVE.
			//
			// Two wrong answers before this one. First it was "nearest chest within
			// twenty-four blocks of the town site" — and the site is the middle of
			// an OPEN SQUARE, so it landed in whichever building happened to sit
			// closest, differently every world, which is a needle in a haystack the
			// mod built for itself. Then it went beside the well in the square,
			// which is deterministic and findable and still WRONG, because the
			// interesting half of that settlement is forty blocks underneath it and
			// that is where anybody who has understood the place goes looking.
			//
			// The library in the undercity. The survivors keep their accounts on
			// that shelf and this goes on it with them, which is the only version of
			// this that makes sense in the fiction: the people who know where the
			// next place is are the ones hiding from what is in it, and you have to
			// find them before they will tell you.
			//
			// It also means the town's own secret — the way down — is now load
			// bearing rather than optional. You cannot follow the chain past the
			// town without finding the people under it.
			case TOWN -> inTheLibrary(over, anchor);
			// AND THE TOWER'S GOES IN THE CELLAR, WITH THE BOOK THAT SENDS YOU ON.
			//
			// THIS IS WHERE THE STORY STOPPED. The default below is "nearest chest
			// within twenty-four blocks of the site", and the tower's only chest is
			// fourteen down a stair whose landing wanders several blocks sideways —
			// so the one container in the building sat right on the edge of that
			// cube and sometimes outside it. When it fell outside, leaveTheWay found
			// nothing, logged one line nobody was reading, and the chain simply
			// ended: cellar, tunnel, deep hole, no map, no next place.
			//
			// Aimed at the cellar now rather than at the doorstep. And it is the
			// right chest for it on its own merits — HouseBooks.buried is in there,
			// and that book's last line is go to the gaol on the ridge. The
			// direction and the means to follow it end up in the same box.
			case TOWER -> inTheCellar(over, anchor);
			default -> nearestHolder(over, anchor);
		};
		if (holder == null) {
			// WARN, NOT INFO. This line IS the chain breaking — the next place has
			// been sited and there is now no way for anybody to learn where. It sat
			// at info level among a hundred other info lines and went unread through
			// a whole playthrough that dead-ended because of it.
			HerobrineMod.LOGGER.warn("nowhere in the {} to leave the way to the {}"
					+ " — THE TRAIL ENDS HERE",
				from.name().toLowerCase(java.util.Locale.ROOT),
				next.name().toLowerCase(java.util.Locale.ROOT));
			return;
		}
		// SCALE TWO, AND THE COORDINATES IN THE NAME.
		//
		// This was scale FOUR — two thousand and forty-eight blocks across, the
		// largest map in the game — for a building somewhere between three hundred
		// and thirteen hundred blocks away. A map cannot be centred on an arbitrary
		// point: createFresh snaps the middle to a grid of 128 << scale, so at that
		// size the marker and the player's own arrow are both dots somewhere in a
		// vast tile, and often the arrow is not on it at all. See Keep.theWay, which
		// hit exactly this and was reported as "the map leads to the wrong place".
		//
		// The name is the part that cannot fail, and it reads in the tooltip before
		// the map is ever opened.
		net.minecraft.world.item.ItemStack map =
			net.minecraft.world.item.MapItem.create(over, to.getX(), to.getZ(),
				(byte) 2, true, true);
		net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
			map, to, "+",
			net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_MARKER);
		// Named for the place it points at, in his register rather than his voice —
		// these are not messages to the player, they are somebody's papers.
		map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal(
				WAY_TO[next.ordinal()] + " — " + to.getX() + ", " + to.getZ()));
		if (!(holder instanceof net.minecraft.world.Container box)) {
			return;
		}
		for (int slot = 0; slot < box.getContainerSize(); slot++) {
			if (box.getItem(slot).isEmpty()) {
				box.setItem(slot, map);
				box.setChanged();
				HerobrineMod.LOGGER.info(
					"the way to the {} was left {}, at [{}, {}, {}]",
					next.name().toLowerCase(java.util.Locale.ROOT),
					switch (from) {
						case HOMESTEAD -> "on the tower";
						case TOWN -> "in the undercity library";
						default -> "in the " + from.name().toLowerCase(java.util.Locale.ROOT);
					},
					holder.getBlockPos().getX(), holder.getBlockPos().getY(),
					holder.getBlockPos().getZ());
				return;
			}
		}
		HerobrineMod.LOGGER.info("every container in the {} was full", from.name());
	}

	/**
	 * A chest on the tower deck, put there for this if there is not one already.
	 *
	 * PLACED RATHER THAN FOUND, unlike every other link. The deck was emptied when
	 * the portal moved off it and under the house, and it has been a viewing
	 * platform with nothing on it ever since — which is a waste of the one
	 * structure in the overworld that advertises itself.
	 *
	 * Idempotent, because this runs the moment the town is sited and the tower may
	 * have been standing for an hour by then with a player having already been up
	 * it. If there is a chest on the deck it uses that one; only an empty deck gets
	 * a new one. Otherwise a second visit finds two chests and the deliberateness —
	 * which is the whole effect — is gone.
	 */
	private static net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable
			BlockEntity onTheTower(ServerLevel over) {
		BlockPos deck = com.bloomlet.herobrine.structure.Spire.site(over);
		if (deck == null) {
			return null;      // no tower on this save; the search below can have it
		}
		over.getChunk(deck.getX() >> 4, deck.getZ() >> 4);
		// THE SUMMIT FIRST, AND THAT IS WHERE IT BELONGS.
		//
		// It went on the deck, which is at the top of the STAIR — and the stair is
		// only the first half of the tower. Everything past it, the lava, the gap,
		// the jumps and the chest with the wings in it, was optional: you could get
		// the map, turn round, and never learn there was more above you.
		//
		// In the same chest as the wings, the climb has one destination and it pays
		// once, at the top, with both halves of what the tower is for — the thing
		// somebody died holding, and the way on to the next building.
		BlockPos summit = com.bloomlet.herobrine.structure.Spire.wings(over);
		if (summit != null && over.getBlockState(summit)
				.is(net.minecraft.world.level.block.Blocks.CHEST)) {
			return over.getBlockEntity(summit);
		}
		// Outward from the middle of the deck, first standable square. The rings
		// matter: dead centre is where a player walks out of the stair, and a chest
		// they have to step round is a chest in the way rather than a chest waiting.
		for (int ring = 1; ring <= 3; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					for (int dy = 0; dy <= 3; dy++) {
						BlockPos at = deck.offset(dx, dy, dz);
						net.minecraft.world.level.block.state.BlockState here =
							over.getBlockState(at);
						if (here.is(net.minecraft.world.level.block.Blocks.CHEST)) {
							return over.getBlockEntity(at);      // already been done
						}
						if (!here.isAir()
							|| !over.getBlockState(at.below()).isSolid()
							|| !over.getBlockState(at.above()).isAir()) {
							continue;
						}
						over.setBlock(at,
							net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),
							3);
						// A light beside it, so it is the thing you see when your head
						// comes up through the floor rather than something you find
						// after a minute of looking round in the dark.
						over.setBlock(at.above(),
							net.minecraft.world.level.block.Blocks.SOUL_LANTERN
								.defaultBlockState()
								.setValue(net.minecraft.world.level.block.state.properties
									.BlockStateProperties.HANGING, false),
							3);
						HerobrineMod.LOGGER.info(
							"a chest was left on the tower deck at [{}, {}, {}]",
							at.getX(), at.getY(), at.getZ());
						return over.getBlockEntity(at);
					}
				}
			}
		}
		return null;
	}

	/**
	 * The tower's cellar, where its book already is.
	 *
	 * Falls back to the doorstep if the cellar could not be cut — the ground under
	 * a tower is not always diggable, and a map in a slightly duller place beats
	 * the chain ending.
	 */
	private static net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable
			BlockEntity inTheCellar(ServerLevel over, BlockPos site) {
		BlockPos down = site.below(
			com.bloomlet.herobrine.structure.SecondHouse.cellarDepth());
		over.getChunk(down.getX() >> 4, down.getZ() >> 4);
		net.minecraft.world.level.block.entity.BlockEntity found =
			nearestHolder(over, down);
		if (found != null) {
			return found;
		}
		HerobrineMod.LOGGER.info("no cellar under this tower — the map stays up top");
		return nearestHolder(over, site);
	}

	/**
	 * A shelf in the undercity library, on the survivors' own bookcase run.
	 *
	 * Idempotent like the tower's, because the town may have been standing for an
	 * hour before its map is due and somebody may already have been down there. A
	 * second chest appearing beside the first is the effect gone.
	 *
	 * Falls back to the square if the undercity is not there — a town can be sited
	 * where the ground refuses a chamber, and a map nobody can reach is worse than
	 * a map in a slightly duller place.
	 */
	private static net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable
			BlockEntity inTheLibrary(ServerLevel over, BlockPos square) {
		BlockPos shelf = com.bloomlet.herobrine.town.Undercity.libraryAt(square);
		over.getChunk(shelf.getX() >> 4, shelf.getZ() >> 4);
		net.minecraft.world.level.block.entity.BlockEntity found =
			nearestHolder(over, shelf);
		if (found != null) {
			return found;      // their own store, which is exactly the right shelf
		}
		HerobrineMod.LOGGER.info("no undercity under this town — the map stays up top");
		return atTheWell(over, square);
	}

	/**
	 * A chest beside the well in the middle of the square, put there if need be.
	 *
	 * Kept as the fallback rather than deleted: not every town gets a chamber under
	 * it, and the square is the one place in a settlement that is always there.
	 */
	private static net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable
			BlockEntity atTheWell(ServerLevel over, BlockPos centre) {
		over.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
		// Two out from the rim, so it is beside the well rather than in it. The
		// well itself is a three by three with a beam over it — see Township.square.
		for (int ring = 2; ring <= 5; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					for (int dy = 0; dy <= 2; dy++) {
						BlockPos at = centre.offset(dx, dy, dz);
						net.minecraft.world.level.block.state.BlockState here =
							over.getBlockState(at);
						if (here.is(net.minecraft.world.level.block.Blocks.CHEST)) {
							return over.getBlockEntity(at);
						}
						if (!here.isAir()
							|| !over.getBlockState(at.below()).isSolid()
							|| !over.getBlockState(at.above()).isAir()) {
							continue;
						}
						over.setBlock(at,
							net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),
							3);
						over.setBlock(at.above(),
							net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState()
								.setValue(net.minecraft.world.level.block.state.properties
									.BlockStateProperties.HANGING, false),
							3);
						HerobrineMod.LOGGER.info(
							"a chest was left at the well at [{}, {}, {}]",
							at.getX(), at.getY(), at.getZ());
						return over.getBlockEntity(at);
					}
				}
			}
		}
		return nearestHolder(over, centre);
	}

	/**
	 * A chest or a barrel near the last building, and it does not much matter which.
	 *
	 * Searched rather than remembered on purpose. Every one of these buildings puts
	 * containers down through a different method — Loot.scatter in the town, the
	 * sealed room in the houses, Remembering in the keep — and threading a "put the
	 * map here" position out of all of them is six places to keep in step. Asking the
	 * world where the chests are is one place, and it is also robust to a player
	 * having moved things about.
	 */
	private static net.minecraft.world.level.block.entity.@org.jspecify.annotations.Nullable
			BlockEntity nearestHolder(
			ServerLevel over, BlockPos anchor) {
		net.minecraft.world.level.block.entity.BlockEntity best = null;
		double nearest = Double.MAX_VALUE;
		int r = LOOKS_FOR_A_CHEST;
		for (BlockPos at : BlockPos.betweenClosed(
				anchor.offset(-r, -r, -r), anchor.offset(r, r, r))) {
			if (!over.isLoaded(at)) {
				continue;
			}
			net.minecraft.world.level.block.state.BlockState state = over.getBlockState(at);
			if (!state.is(net.minecraft.world.level.block.Blocks.CHEST)
				&& !state.is(net.minecraft.world.level.block.Blocks.BARREL)) {
				continue;
			}
			double away = at.distSqr(anchor);
			if (away < nearest) {
				net.minecraft.world.level.block.entity.BlockEntity found =
					over.getBlockEntity(at);
				if (found instanceof net.minecraft.world.Container) {
					nearest = away;
					best = found;
				}
			}
		}
		return best;
	}

	/**
	 * AND THE LAST ONE POINTS BACK AT THE FIRST.
	 *
	 * The chain has run five buildings and something like three thousand blocks,
	 * and every map in it has pointed outward — further from spawn, further from
	 * the house, deeper into the story. This is the one that turns round.
	 *
	 * WHY THE LOOP IS THE ENDING. A sequence that finishes at its furthest point
	 * finishes by running out, and the player's last act is closing a chest in a
	 * building nobody will visit again. A sequence that finishes by sending you
	 * HOME finishes with a walk you have already done, past four buildings you
	 * already know, to a floor you have stood on a dozen times — carrying the one
	 * fact that recontextualises all of it. The distance is the same. What changes
	 * is that every step of it is somewhere you have been.
	 *
	 * A MAP AND A BOOK, because neither works alone. A map to the homestead on its
	 * own says "go back", which they will read as the chain glitching — they have
	 * BEEN there, it was the first thing they found. The book is what makes the
	 * map mean something new, and the map is what stops the book being a riddle
	 * they have to solve with coordinates.
	 *
	 * And the book is the only place in the mod that says the word outright. It
	 * has earned it by being five buildings deep and by being written by somebody
	 * who is plainly past caring who reads it.
	 */
	private static void theLastWord(ServerLevel over, BlockPos threshold) {
		Long home = over.getAttached(Place.HOMESTEAD.site);
		if (home == null) {
			return;
		}
		BlockPos house = BlockPos.of(home);
		net.minecraft.world.level.block.entity.BlockEntity holder =
			nearestHolder(over, threshold);
		if (!(holder instanceof net.minecraft.world.Container box)) {
			HerobrineMod.LOGGER.info("nowhere in the threshold to leave the last word");
			return;
		}

		// The elder brother's account, in the same box as the map home. It is the
		// only book in the mod written by somebody who WON, and it is the bleakest
		// thing in it — see HouseBooks.theThresholdAfter.
		for (int slot = 0; slot < box.getContainerSize(); slot++) {
			if (box.getItem(slot).isEmpty()) {
				box.setItem(slot, com.bloomlet.herobrine.structure.HouseBooks
					.theThresholdAfter());
				break;
			}
		}

		// Scale two and the coordinates in the name, same as every other map on the
		// trail. Four is two thousand blocks across and the arrow falls off it.
		net.minecraft.world.item.ItemStack map =
			net.minecraft.world.item.MapItem.create(over, house.getX(), house.getZ(),
				(byte) 2, true, true);
		net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
			map, house, "+",
			net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_MARKER);
		map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
			net.minecraft.network.chat.Component.literal("back to the first house"));

		net.minecraft.world.item.ItemStack book =
			new net.minecraft.world.item.ItemStack(
				net.minecraft.world.item.Items.WRITTEN_BOOK);
		book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
			new net.minecraft.world.item.component.WrittenBookContent(
				net.minecraft.server.network.Filterable.passThrough("under the floor"),
				"", 0,
				java.util.List.of(
					net.minecraft.server.network.Filterable.passThrough(
						net.minecraft.network.chat.Component.literal(LAST_PAGE)),
					net.minecraft.server.network.Filterable.passThrough(
						net.minecraft.network.chat.Component.literal(LAST_PAGE_TWO))),
				true));

		int put = 0;
		for (int slot = 0; slot < box.getContainerSize() && put < 2; slot++) {
			if (!box.getItem(slot).isEmpty()) {
				continue;
			}
			box.setItem(slot, put == 0 ? book : map);
			put++;
		}
		box.setChanged();
		HerobrineMod.LOGGER.info(
			"the last word was left in the threshold at [{}, {}, {}] — it points home",
			holder.getBlockPos().getX(), holder.getBlockPos().getY(),
			holder.getBlockPos().getZ());
	}

	/**
	 * WHAT IT SAYS, AND IT IS THE PLAINEST WRITING IN THE MOD.
	 *
	 * Every sign and every page until now has been four lowercase words with no
	 * full stop — somebody being careful, or somebody who cannot manage more. This
	 * is neither. It is a man leaving an instruction, because he has decided
	 * somebody is going to come and he would rather they knew.
	 *
	 * It never says what is down there. "The floor of the back room" and "it is
	 * not a cellar" is the whole of it — enough to send them, not enough to spoil
	 * the moment they get the plank up.
	 */
	private static final String LAST_PAGE =
		"You will have been to the farmhouse. Everyone goes there first.\n\n"
		+ "Go back.\n\n"
		+ "The back room. The floor, four paces in from the store. Lift the "
		+ "boards and mind the third one, there is still hair caught under it.";

	private static final String LAST_PAGE_TWO =
		"It is not a cellar.\n\n"
		+ "I laid those boards over my brother's stair with his blood still "
		+ "tacky on my hands and I have not been able to make myself take them "
		+ "up since. Four of us went down. I came back with two of my fingers "
		+ "and the smell in my clothes, and I burned the clothes.\n\n"
		+ "You will not have that problem. Nobody who reads this ever does.";

	/** What each one is written on the back of. Indexed by the place it points to. */
	private static final String[] WAY_TO = {
		"",
		"the road east",
		"where the tower stands",
		"the cut in the hill",
		"the long nave",
		"the last door",
	};
	// ---- END THE CHAIN -----------------------------------------------------

	private enum Place {
		HOMESTEAD("homestead", Phase.RUMOUR, 280, 520),
		TOWN("town", Phase.WATCHER, 340, 620),
		TOWER("house_two", Phase.TRESPASSER, 450, 800),
		GAOL("house_three", Phase.MIMIC, 550, 950),
		/**
		 * AND THIS ONE IS NOT SITED UNTIL A HUNT HAS BEEN SURVIVED.
		 *
		 * Every other place waits on time and on the one before it. The church
		 * waits on something that has to be got THROUGH, and that single line
		 * is what turns HUNTER from a phase with a nasty random event in it into
		 * a chapter with a middle.
		 *
		 * Before this, the hunt was one weight of fourteen in a pool of forty:
		 * a group could pass the whole of HUNTER without drawing it, and the
		 * church would arrive anyway, on a clock, having asked nothing of
		 * anybody. The building that comes after the worst night in the mod
		 * ought to be the thing that night was for.
		 *
		 * It cannot stall, because the hunt is now owed rather than rolled for
		 * — see TheHunt. Once the chapter has had its hour he stops waiting to
		 * be drawn and simply comes.
		 */
		CHURCH("house_four", Phase.HUNTER, 650, 1100, true),
		THRESHOLD("threshold", Phase.SIEGE, 800, 1300);

		final Phase from;
		/** Whether a hunt has to have happened first. Only the church. */
		final boolean afterAHunt;
		/**
		 * How far out this one goes, and IT CLIMBS.
		 *
		 * Every place used to be sited in the same 340–780 band, which quietly
		 * flattened the whole journey: the threshold was the same walk as the
		 * homestead, so going deeper into the story never meant going further
		 * into the world.
		 *
		 * It climbs now, and it is only safe to climb because the buildings
		 * advertise themselves — roads, smoke, a sign, a sound, and a map from
		 * the graves. Distance without any of that is what produced a group
		 * walking a thousand blocks and finding nothing. Distance WITH it is the
		 * thing that makes the last one feel like the edge of the map.
		 */
		final int near;
		final int far;
		/** Where it was decided to go, once anybody was around to decide near. */
		final AttachmentType<Long> site;
		/** Whether the blocks exist. */
		final AttachmentType<Boolean> up;
		/** Whether anybody has walked up on it yet. Spent once, for good. */
		final AttachmentType<Boolean> met;
		/** Whether anybody has been into its chests. Also once, for good. */
		final AttachmentType<Boolean> rifled;

		Place(String key, Phase from, int near, int far) {
			this(key, from, near, far, false);
		}

		Place(String key, Phase from, int near, int far, boolean afterAHunt) {
			this.from = from;
			this.afterAHunt = afterAHunt;
			this.near = near;
			this.far = far;
			this.site = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_site"), Codec.LONG);
			this.up = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_up"), Codec.BOOL);
			this.met = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_met"), Codec.BOOL);
			this.rifled = AttachmentRegistry.createPersistent(
				HerobrineMod.id(key + "_rifled"), Codec.BOOL);
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Dwellings::onTick);
	}

	private static void onTick(MinecraftServer server) {
		if (++tickCounter % CHECK_INTERVAL != 0) {
			return;
		}
		if (!com.bloomlet.herobrine.Config.get().enabled) {
			return;
		}
		ServerLevel overworld = server.overworld();
		if (overworld.players().isEmpty()) {
			return;
		}
		Phase phase = Wrath.phase(server);

		for (Place place : Place.values()) {
			boolean wanted = place == Place.TOWN
				? com.bloomlet.herobrine.Config.get().town
				: com.bloomlet.herobrine.Config.get().houses;
			if (!wanted) {
				continue;
			}
			if (Boolean.TRUE.equals(overworld.getAttached(place.up))) {
				arriving(overworld, place);
				Long where = overworld.getAttached(place.site);
				if (where != null) {
					Approach.heard(overworld, BlockPos.of(where), place.from);
				}
				// THE STORY MOVES ON WHEN SOMEBODY FINDS IT, NOT WHEN IT IS BUILT.
				//
				// This used to advance on `up`, and `up` happens at a hundred and
				// ninety-two blocks — so a player passing a hundred and ninety
				// blocks away built the place, never saw it, and unlocked the next
				// chapter anyway. That is precisely how a group ends up three
				// buildings deep having read none of them, which is the failure
				// this whole direction exists to fix.
				//
				// `met` is set on the approach, within sixty blocks, so it means
				// somebody actually stood outside it.
				rifling(overworld, place);
				if (Boolean.TRUE.equals(overworld.getAttached(place.met))) {
					comingHome(overworld, place, phase);
					continue;   // found; on to the next chapter
				}
				// THE FREE PASS THE HOMESTEAD HAD HERE IS GONE, AND IT WAS A
				// WORKAROUND FOR A BUG I HAD NOT FOUND YET.
				//
				// The symptom was that the chain never moved, and the theory was
				// that requiring somebody to walk up to a house they were handed a
				// map to was too strict. It was not. The real fault was that the
				// path Whereabouts uses to build the house never set place.up, so
				// arriving() was never reached and the homestead could never be
				// recorded as found however close anybody stood. See raise().
				//
				// With that fixed, letting it through unfound does the wrong thing
				// instead: the town sites four seconds into a new world, before
				// anybody has seen the first building, and the sequence stops being
				// a sequence. Find the house, and the town appears.
				// AND IT NEVER STALLS FOREVER. A place that was built and then
				// walked away from would otherwise hold the entire sequence shut
				// with nobody left to open it. If everybody has gone a long way
				// off, the story goes on without them — the building stays exactly
				// where it is, to be found whenever they come back.
				double away = Double.MAX_VALUE;
				if (where != null) {
					for (ServerPlayer player : overworld.players()) {
						away = Math.min(away, Math.sqrt(
							BlockPos.of(where).distSqr(player.blockPosition())));
					}
				}
				if (away > place.far + ABANDONED_MARGIN) {
					overworld.setAttached(place.met, true);
					Wrath.discovered(server);
					HerobrineMod.LOGGER.info("{} was built and never visited — moving on",
						place.name().toLowerCase(java.util.Locale.ROOT));
					continue;
				}
				return;     // standing, unfound, and somebody is still near enough
			}
			// FIND ONE, THE NEXT ONE APPEARS. THAT IS THE WHOLE RULE NOW.
			//
			// Three gates used to stand here and between them they made a simple
			// sequence very hard to observe:
			//
			//   the phase ladder   the next place waited for a chapter
			//   the dues floor     and then for that chapter to have had its
			//                      twenty or thirty minutes
			//   a survived hunt    and the church waited on one of those as well
			//
			// Every one of them was defensible while the buildings WERE the pacing.
			// They are not any more. The fight lives on the far side of the way, the
			// traces keep their own weights, and these six buildings are the trail
			// that leads somebody there — and a trail with a thirty minute pause
			// built into it is not paced, it is broken. It was also
			// indistinguishable from broken from the outside, which is how an
			// evening went on looking for a map that was working perfectly and had
			// simply not been placed yet.
			//
			// THE SEQUENCE STILL CANNOT BE SKIPPED, and nothing here was needed to
			// stop that. The loop above returns unless this place is built AND has
			// been walked up to, so "the one before it has been found" is the gate.
			// It is the only one that was ever doing necessary work.
			//
			// The hunt gate on the church goes with them, and it had stopped being
			// a gate and become a wall: hunts only happen in his world now, so a
			// player who had not crossed over could never open it and the last two
			// buildings would have sat unsited for good.
			// AND THE TOWN WAITS FOR SOMEWHERE TO PUT ITS MAP.
			//
			// Siting the town is what places the first map, and the first map goes in
			// the chest at the top of the tower. Whereabouts raises the house and the
			// tower in one call, but this loop ticks on its own clock and can land
			// between the two — in which case onTheTower finds nothing, the map falls
			// back into a kitchen cupboard in the house, and the one link in the
			// chain that has to teach the player what a chain is has taught nothing.
			//
			// One tick of patience costs nothing and removes the race.
			if (place == Place.TOWN
				&& com.bloomlet.herobrine.structure.Spire.site(overworld) == null) {
				return;
			}

			Long chosen = overworld.getAttached(place.site);
			if (chosen == null) {

				BlockPos picked = pick(overworld, place);
				if (picked != null) {
					overworld.setAttached(place.site, picked.asLong());
					HerobrineMod.LOGGER.info("{} will stand near [{}, {}] ({})",
						place.name().toLowerCase(java.util.Locale.ROOT),
						picked.getX(), picked.getZ(), phase.name());
					// AND THE ONE BEFORE IT LEARNS WHERE THIS ONE IS.
					leaveTheWay(overworld, place, picked);
				}
				return;     // one at a time, and the next waits for this one
			}

			BlockPos site = BlockPos.of(chosen);
			double nearest = Double.MAX_VALUE;
			for (ServerPlayer player : overworld.players()) {
				nearest = Math.min(nearest, Math.sqrt(
					site.distSqr(player.blockPosition())));
			}

			// IT FOLLOWS THEM IF THEY NEVER CAME.
			//
			// A place chosen near where the group was an hour ago is no use to
			// a group that has since moved five hundred blocks and built
			// somewhere else — it sits there, unfound, and the sequence stops
			// dead behind it because nothing after it is allowed to exist yet.
			//
			// So if everybody is a long way off, it is forgotten and chosen
			// again. Fourteen hundred is far enough that nobody walking toward
			// it can trip this by accident; at that distance they are not
			// coming, and the story is waiting on somebody who does not know
			// it is waiting.
			if (nearest > place.far + ABANDONED_MARGIN) {
				overworld.setAttached(place.site, null);
				HerobrineMod.LOGGER.info("{} was never found at [{}, {}] — moving it",
					place.name().toLowerCase(java.util.Locale.ROOT),
					site.getX(), site.getZ());
				return;
			}

			if (nearest <= RAISE_RANGE && build(overworld, place, site)) {
				overworld.setAttached(place.up, true);
				if (place == Place.THRESHOLD) {
					theLastWord(overworld, site);
				}
				// Roads, smoke and a sign, laid at build time for the same reason
				// the building is: nobody is close enough to watch it happen.
				Approach.lay(overworld, site, phase);
			}
			return;         // whatever happened, the next one is not due yet
		}
	}

	/**
	 * HE KNOWS YOU OPENED IT.
	 *
	 * The first chest in each of his buildings, once, for good. Two things happen
	 * together and neither is an attack:
	 *
	 *   THE SKY TURNS. Thunder, immediately, and it lasts. Weather arriving
	 *     BECAUSE of something somebody just did reads nothing like weather
	 *     arriving on a timer — and this is the moment it is most obviously
	 *     causal, because the lid is still open.
	 *
	 *   AND EVERY LIGHT IN THE BUILDING GOES OUT. That is the unnatural half, and
	 *     it is better than any noise: torches do not go out. There is no vanilla
	 *     mechanic that snuffs a torch, so a player standing in a room that just
	 *     went dark knows with total certainty that something did it on purpose.
	 *     It is also the only effect here that changes what they can SEE, in a
	 *     building they were reading in.
	 *
	 * Nothing is destroyed and nothing is taken. The torches are snuffed rather
	 * than broken — they go back with a flint and steel, so the cost is a moment
	 * of blindness and not a repair bill.
	 */
	private static final double IN_THE_HOUSE = 24.0;

	private static void opened(ServerLevel level, Place place, BlockPos site,
	                          ServerPlayer who) {
		if (Boolean.TRUE.equals(level.getAttached(place.rifled))) {
			return;
		}
		level.setAttached(place.rifled, true);
		com.bloomlet.herobrine.manifest.Skies.turn(level);
		int snuffed = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
				site.offset(-16, -12, -16), site.offset(16, 12, 16))) {
			if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TORCH)) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR
					.defaultBlockState(), 3);
				snuffed++;
			} else if (level.getBlockState(pos)
					.is(net.minecraft.world.level.block.Blocks.WALL_TORCH)) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR
					.defaultBlockState(), 3);
				snuffed++;
			}
		}
		level.playSound(null, who.blockPosition(),
			net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
			net.minecraft.sounds.SoundSource.AMBIENT, 1.4F, 0.6F);
		HerobrineMod.LOGGER.info("{} opened his chest in the {} — {} lights out",
			who.getName().getString(),
			place.name().toLowerCase(java.util.Locale.ROOT), snuffed);
	}

	/**
	 * Somebody has a chest open inside one of his buildings.
	 *
	 * Checked from the tick rather than hooked onto the interaction, because the
	 * container-open path is a menu and reaching it means a mixin; the open lid is
	 * already a property of the block entity and asking every two seconds is
	 * enough for something that only fires once per building ever.
	 */
	private static void rifling(ServerLevel level, Place place) {
		if (Boolean.TRUE.equals(level.getAttached(place.rifled))) {
			return;
		}
		Long where = level.getAttached(place.site);
		if (where == null) {
			return;
		}
		BlockPos site = BlockPos.of(where);
		for (ServerPlayer player : level.players()) {
			if (Math.sqrt(site.distSqr(player.blockPosition())) > IN_THE_HOUSE) {
				continue;
			}
			if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu) {
				opened(level, place, site, player);
				return;
			}
		}
	}

	/**
	 * The next one nobody has walked up on yet.
	 *
	 * Reuses the same `met` flag the arrival sighting spends, which is exactly
	 * the right definition: a building somebody has already stood outside is not
	 * somewhere to be sent, and one that is merely SITED and not yet built still
	 * is — it will exist by the time anybody gets near it, because building is
	 * what happens when a player comes within range.
	 *
	 * @return where to point somebody, or null if there is nothing left to find
	 */
	public static @org.jspecify.annotations.Nullable BlockPos unfound(ServerLevel level) {
		for (Place place : Place.values()) {
			if (Boolean.TRUE.equals(level.getAttached(place.met))) {
				continue;
			}
			Long chosen = level.getAttached(place.site);
			if (chosen != null) {
				return BlockPos.of(chosen);
			}
		}
		return null;
	}

	/**
	 * SOMEBODY IS WALKING UP ON IT FOR THE FIRST TIME, AND HE IS OUTSIDE.
	 *
	 * The payoff for the four-hundred-block walk. Finding one of these is the
	 * biggest thing that happens in a session, and until now the reward for it
	 * was a building — good, but silent. Standing at the door watching the group
	 * come over the rise turns finding a structure into being SHOWN one, which
	 * is the difference between world generation and somebody having been here.
	 *
	 * ONCE PER BUILDING, EVER, and persistent so it survives a restart. The
	 * second time is a spawner and everybody knows it.
	 *
	 * Sixty blocks rather than the raise range: they have to be close enough
	 * that the house is already in view, or he is a figure standing in a field
	 * for no reason. This wants to land in the same breath as "there it is".
	 */
	private static final int ARRIVING = 60;

	/**
	 * HE COMES HOME, AND THE BUILDING HAS A CLOCK ON IT.
	 *
	 * Finding one of his places used to be entirely safe. You walked up, the
	 * sighting fired, and then it was a museum — read the books, empty the
	 * chests, leave whenever. That is the wrong feeling for the one place in the
	 * world that is HIS, and it is also a wasted opportunity: the building
	 * already has the players standing still in one spot, engrossed, for several
	 * minutes, which is the best setup this mod will ever get handed.
	 *
	 * So arriving starts a clock. Two to four minutes of being left alone —
	 * enough to get into the cellar, find the chest, start reading — and then he
	 * is there, in the doorway, and it is not a sighting.
	 *
	 * WHICH TURNS LOOTING INTO A DECISION, which is the real prize. Do you read
	 * the second book or take the chest and go? Nothing has to be explained for
	 * that question to arrive; it arrives the first time somebody hears him and
	 * is still holding an open inventory.
	 *
	 * AND IT SCALES, because the same event cannot be right for the homestead and
	 * for the threshold. Early it is weather and a sighting — the sky turns while
	 * you are inside, and something is standing at the treeline when you come
	 * out. Late he walks in hunting. Same trigger, same clock, and the first two
	 * buildings teach the players to be afraid of the timer before the timer can
	 * actually hurt them.
	 */
	private static final int COMES_HOME_MIN = 2400;
	private static final int COMES_HOME_SPREAD = 2400;
	private static final double STILL_THERE = 48.0;

	/** Set when somebody arrives; counted down while anybody is still inside. */
	private static final java.util.Map<Place, Integer> homeIn =
		new java.util.EnumMap<>(Place.class);

	private static void comingHome(ServerLevel level, Place place, Phase phase) {
		Long where = level.getAttached(place.site);
		if (where == null) {
			return;
		}
		BlockPos site = BlockPos.of(where);
		ServerPlayer inside = null;
		for (ServerPlayer player : level.players()) {
			if (Math.sqrt(site.distSqr(player.blockPosition())) <= STILL_THERE) {
				inside = player;
				break;
			}
		}
		// Nobody there: the clock resets rather than pausing. He is not waiting
		// for them to come back — coming home only means anything if it lands
		// while they are still in the house.
		if (inside == null) {
			homeIn.remove(place);
			return;
		}
		int left = homeIn.getOrDefault(place,
			COMES_HOME_MIN + level.getRandom().nextInt(COMES_HOME_SPREAD));
		left -= CHECK_INTERVAL;
		if (left > 0) {
			homeIn.put(place, left);
			return;
		}
		homeIn.remove(place);
		// HE IS ALWAYS THE SKY AND A FIGURE OUT HERE.
		//
		// This was `phase.atLeast(MIMIC)` — from the fourth chapter on, finding one
		// of his places got you a hunt rather than a sighting. The fight is in his
		// world now, so what you get for finding a building is the building and him
		// standing in it, which is what the beat was originally for.
		boolean hunting = false;
		com.bloomlet.herobrine.manifest.Skies.turn(level);
		// THE ARGUMENTS WERE THE WRONG WAY ROUND, AND HE HAS NEVER ONCE HUNTED
		// HERE. place() takes (ignoreLight, hunting) and this passed
		// (hunting, false) — so from MIMIC the flag went into the LIGHT check
		// and the hunting flag was hard false. Every "he comes home" in the
		// mod's history has been him standing there watching, while the line
		// below printed "hunting" and everybody believed it.
		//
		// ignoreLight is true outright now rather than riding on the phase.
		// This event is a clock running out while somebody is inside a building
		// they have lit; whether the ground outside is dark is not a question
		// worth asking, and asking it meant the whole beat silently failed in
		// daylight.
		// AND HE DOES NOT COME HOME EITHER, for the same reason. See arriving().
		//
		// This was the other half: linger in one of his buildings long enough and he
		// turns up in it. Better written than the arrival spawn and the same
		// problem — it makes the six buildings places where he can be met, and they
		// are supposed to be the reason to go and look for him somewhere else.
		//
		// Left as a log line rather than deleted outright, because the clock that
		// drives it is worth keeping: something SHOULD happen to somebody who makes
		// camp in his house, and when we know what it is this is where it goes.
		HerobrineMod.LOGGER.info("{} is still in the {} — nothing came ({})",
			inside.getName().getString(), place.name().toLowerCase(java.util.Locale.ROOT),
			hunting ? "would have hunted" : "would have watched");
	}

	private static void arriving(ServerLevel level, Place place) {
		if (Boolean.TRUE.equals(level.getAttached(place.met))) {
			return;
		}
		Long chosen = level.getAttached(place.site);
		if (chosen == null) {
			return;
		}
		BlockPos site = BlockPos.of(chosen);
		for (ServerPlayer player : level.players()) {
			if (site.distSqr(player.blockPosition()) > (double)ARRIVING * ARRIVING) {
				continue;
			}
			// Marked spent on the approach rather than on a successful placement.
			// If the geometry refuses — they came in through the back, or it is
			// a hillside with no sightline — the moment is gone, and trying
			// again every two seconds until it works would put him outside the
			// house long after they had walked into it, which is worse than
			// nothing.
			level.setAttached(place.met, true);
			// AND THE STORY MOVES, because somebody found one of his places. This
			// is the only call site that advances a phase anywhere in the mod.
			Wrath.discovered(level.getServer());
			// AND HE IS NOT STANDING THERE WHEN YOU ARRIVE. NOT ANY MORE.
			//
			// This put him at the building the moment somebody walked up to it, which
			// is the single most reliable way to see him in the overworld: find a
			// house, and he is at it. Reported after a playthrough as "he was shown in
			// the town, the only thing I did was walk there".
			//
			// It contradicts the thing the whole mod was rebuilt around. He LIVES on
			// the far side of the way now — over the keep, circling it — and what is
			// left out here is his places and the traces. A building that produces him
			// on arrival makes the overworld somewhere he still is, and then crossing
			// over is a formality rather than the point.
			//
			// The apparitions are untouched and they are a different thing: the stare,
			// the glimpse and the passage fire on their own clock, wherever the player
			// happens to be, and are gone before anybody can walk up to them. Those
			// are atmosphere. This was residence.
			return;
		}
	}

	/**
	 * Somewhere out of sight of everybody, at a walkable distance.
	 *
	 * Measured from the middle of wherever the players actually are rather than
	 * from any one of them, so on a server it lands somewhere the group might
	 * plausibly go instead of behind whoever happened to be furthest out.
	 *
	 * Refuses anything closer than this place's own near band to ANY player. A house that appears
	 * three hundred blocks from the base of the one person who went mining is
	 * a house somebody watches arrive, and nothing here is ever watched
	 * arriving.
	 */
	/** Candidate sites examined per tick. Each one may generate a chunk. */
	private static final int TRIES = 6;
	/** How many ticks of failure before it says so. */
	private static final int GRUMBLES_AFTER = 30;
	private static final java.util.Map<Place, Integer> refused =
		new java.util.EnumMap<>(Place.class);

	private static @org.jspecify.annotations.Nullable BlockPos pick(ServerLevel level, Place place) {
		double cx = 0;
		double cz = 0;
		for (ServerPlayer player : level.players()) {
			cx += player.getX();
			cz += player.getZ();
		}
		cx /= level.players().size();
		cz /= level.players().size();

		RandomSource random = level.getRandom();
		// Six a tick, every two seconds. Sampling is free now, so this could be far
		// higher — it stays low because a candidate that PASSES generates a chunk,
		// and there is no reason to find six winners when the loop only uses one.
		for (int attempt = 0; attempt < TRIES; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = place.near + random.nextDouble() * (place.far - place.near);
			int x = (int)Math.round(cx + Math.cos(angle) * range);
			int z = (int)Math.round(cz + Math.sin(angle) * range);

			// Judged straight off the noise, with nothing loaded and nothing
			// generated — see buildable(), which is where the four evenings went.
			if (!ready(level, x, z)) {
				continue;
			}
			BlockPos at = new BlockPos(x, Ground.topOf(level, x, z), z);
			boolean tooNear = false;
			for (ServerPlayer player : level.players()) {
				if (player.blockPosition().closerThan(at, place.near)) {
					tooNear = true;
					break;
				}
			}
			if (!tooNear) {
				return at;
			}
		}
		// AND IT SAYS SO WHEN IT CANNOT. A silent null here is indistinguishable
		// from "not due yet", which is exactly how the last four buildings went
		// missing without a single line in the log to point at.
		int missed = refused.merge(place, 1, Integer::sum);
		if (missed % GRUMBLES_AFTER == 0) {
			HerobrineMod.LOGGER.warn(
				"nowhere to put the {} after {} tries — wanted {}-{} blocks out from"
					+ " the players, and every candidate was water, sea level or"
					+ " unbuildable", place.name().toLowerCase(java.util.Locale.ROOT),
				missed * TRIES, place.near, place.far);
		}
		return null;
	}

	/** Put the right building on the chosen ground. */
	private static boolean build(ServerLevel level, Place place, BlockPos site) {
		RandomSource random = level.getRandom();
		return switch (place) {
			case TOWN -> {
				com.bloomlet.herobrine.town.Township.raise(level, site, random);
				yield true;
			}
			case HOMESTEAD -> raise(level, site);
			case TOWER -> raiseMiddle(level, site, 0);
			case GAOL -> raiseMiddle(level, site, 1);
			case CHURCH -> raiseMiddle(level, site, 2);
			case THRESHOLD -> raiseThreshold(level, site);
		};
	}

	/**
	 * Forget where everything was going to be, so it is chosen again.
	 *
	 * Needed because a jar swap changes nothing about a world. All of this
	 * lives in persistent attachments — that is what makes a site survive a
	 * restart, which is the whole point of it — so a server that ran the old
	 * spawn-relative scheme still has those far-off positions recorded after
	 * updating, and would go on waiting for somebody to walk eleven hundred
	 * blocks to a coordinate the new code would never have chosen.
	 *
	 * This clears the bookkeeping ONLY. Anything already standing in the world
	 * stays standing; the mod simply stops believing it owns those places and
	 * picks new ones near the players at the next tick. That is the honest
	 * behaviour — deleting somebody's discovered buildings to tidy up a
	 * migration would be a far worse trade than leaving a spare farmhouse
	 * somewhere.
	 *
	 * @return how many places were forgotten
	 */
	public static int forget(ServerLevel level) {
		ServerLevel overworld = level.getServer().overworld();
		int cleared = 0;
		for (Place place : Place.values()) {
			if (overworld.getAttached(place.site) != null
				|| Boolean.TRUE.equals(overworld.getAttached(place.up))) {
				cleared++;
			}
			overworld.setAttached(place.site, null);
			overworld.setAttached(place.up, null);
		}
		// The two left over from the old scheme, or /herobrine house goes on
		// reporting a farmhouse that is no longer anybody's business.
		overworld.setAttached(ORIGIN, null);
		overworld.setAttached(THRESHOLD_ORIGIN, null);
		HerobrineMod.LOGGER.info("forgot {} sites; they will be chosen again", cleared);
		return cleared;
	}

	/** Every building and where it ended up, for /herobrine locate. */
	public static java.util.List<String> report(ServerLevel level) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		Phase phase = Wrath.phase(level.getServer());
		boolean reached = true;
		for (Place place : Place.values()) {
			String name = place.name().toLowerCase(java.util.Locale.ROOT);
			boolean built = Boolean.TRUE.equals(level.getAttached(place.up));
			Long at = level.getAttached(place.site);

			if (built) {
				BlockPos pos = BlockPos.of(at == null ? 0L : at);
				lines.add(String.format("%-11s found        x %d z %d",
					name, pos.getX(), pos.getZ()));
				continue;
			}
			if (!reached) {
				// Everything after the current chapter, and saying so is more
				// use than six identical "not sited" lines.
				lines.add(String.format("%-11s later        after %s is found",
					name, Place.values()[place.ordinal() - 1]
						.name().toLowerCase(java.util.Locale.ROOT)));
				continue;
			}
			reached = false;
			if (at == null) {
				// One thing left it can be waiting on, so it says that rather than
				// working out which of three. The old version could print "waiting
				// for HUNTER" while the world was at HUNTER, which is the most
				// confusing line this command has ever produced.
				lines.add(String.format("%-11s waiting for  the one before it", name));
			} else {
				BlockPos pos = BlockPos.of(at);
				lines.add(String.format("%-11s OUT THERE    x %d z %d",
					name, pos.getX(), pos.getZ()));
			}
		}
		lines.add("phase " + phase.name());
		return lines;
	}

	public static boolean raiseMiddle(ServerLevel level, BlockPos near, int which) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!ready(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Ground.topOf(level, x, z) + 1, z);
			RandomSource random = level.getRandom();
			switch (which) {
				case 0 -> SecondHouse.build(level, origin, random);
				case 1 -> TheDig.build(level, origin, random);
				default -> Shrine.build(level, origin, random);
			}
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for house {} near [{}, {}]",
			which + 2, near.getX(), near.getZ());
		return false;
	}


	public static @org.jspecify.annotations.Nullable BlockPos thresholdOrigin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(THRESHOLD_ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	/**
	 * Raise the threshold near its site.
	 *
	 * Far less fussy about ground than the homestead, and deliberately so:
	 * almost nothing of it is above the surface, so a slope that would tilt a
	 * farmhouse does not matter to a stair mouth. All it needs is dry land.
	 */
	public static boolean raiseThreshold(ServerLevel level, BlockPos near) {
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			int ground = Ground.topOf(level, x, z);
			if (ground <= level.getSeaLevel()
				|| !level.getFluidState(new BlockPos(x, ground, z)).isEmpty()) {
				continue;
			}
			BlockPos origin = new BlockPos(x, ground, z);
			Threshold.raise(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(THRESHOLD_ORIGIN, origin.asLong());
			return true;
		}
		HerobrineMod.LOGGER.warn("no dry ground for the threshold near [{}, {}]",
			near.getX(), near.getZ());
		return false;
	}


	/**
	 * WHERE THE HOMESTEAD IS GOING TO BE, chosen long before it is built.
	 *
	 * origin() below only answers once the place has actually been raised, which is
	 * when a player first walks near it — and that is too late for the one thing
	 * that needs it. He has to LIVE somewhere from the first minute of the world,
	 * not from whenever somebody happens to wander past his address.
	 *
	 * The site is picked during RUMOUR, immediately, and is what the map in the
	 * grave points at. Handing it to Whereabouts means he is out there from the
	 * start, walking around a house that has not been built yet — which is also
	 * the honest reading, since he is the reason it is there.
	 */
	public static @org.jspecify.annotations.Nullable BlockPos homesteadSite(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(Place.HOMESTEAD.site);
		return packed == null ? null : BlockPos.of(packed);
	}

	/** Where it actually stands, once raised. */
	public static @org.jspecify.annotations.Nullable BlockPos origin(ServerLevel level) {
		Long packed = level.getServer().overworld().getAttached(ORIGIN);
		return packed == null ? null : BlockPos.of(packed);
	}

	public static boolean raised(ServerLevel level) {
		return Boolean.TRUE.equals(level.getServer().overworld().getAttached(RAISED));
	}

	/**
	 * Put it down, near the site, wherever the ground will take it.
	 *
	 * The seed picks the neighbourhood and the terrain picks the spot. Dropping
	 * it on the exact seeded block would put it in a lake or halfway up a cliff
	 * often enough to matter, and a house standing in water is not eerie, it is
	 * broken.
	 */
	/**
	 * ONCE. RAISED WAS DECLARED, READ, AND NEVER WRITTEN.
	 *
	 * The flag has existed the whole time and nothing ever set it, so raised()
	 * answered false forever and this could be entered twice. It did not matter
	 * while only the approach path called it — the tick raises each place once and
	 * moves on. It matters now that Whereabouts builds his house up front: the
	 * house went up at world creation, and then the tick walked past thirty seconds
	 * later and built a SECOND one forty blocks away.
	 *
	 * A playtest found two homesteads. Setting the flag it already has is the whole
	 * fix.
	 */
	public static boolean raise(ServerLevel level, BlockPos near) {
		if (raised(level)) {
			return false;
		}
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = near.getX() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			int z = near.getZ() + (attempt == 0 ? 0 : level.getRandom().nextInt(96) - 48);
			if (!ready(level, x, z)) {
				continue;
			}
			BlockPos origin = new BlockPos(x, Homestead.floorHeightAt(level, x, z), z);
			Homestead.build(level, origin, level.getRandom());
			ServerLevel overworld = level.getServer().overworld();
			overworld.setAttached(ORIGIN, origin.asLong());
			overworld.setAttached(RAISED, true);
			// AND THE SEQUENCE HAS TO BE TOLD, WHICH IS THE BUG THAT ATE THE MAP.
			//
			// There are two ways the homestead gets built and only one of them was
			// telling anybody. Dwellings' own loop builds a place and then sets
			// place.up on the next line; Whereabouts calls THIS method directly at
			// world creation, because the one building the whole mod hangs off has
			// to exist before anybody logs in rather than appearing when they walk
			// past it.
			//
			// That second path set ORIGIN and RAISED and nothing else. So the loop
			// looked at HOMESTEAD, saw up == false, went to build it, got false back
			// from here — already raised — and never marked it. Every tick, for the
			// life of the world. The sequence could not get past the first entry, so
			// the town was never sited, so the map was never made, and the chest at
			// the top of the tower was empty for a reason that had nothing to do
			// with the tower.
			//
			// The site goes with it. raise() picks its own ground and may land up to
			// forty-eight blocks from where pick() suggested, and arriving() measures
			// sixty blocks from the SITE — so a house that wandered was a house you
			// could stand inside without ever being recorded as having found it.
			overworld.setAttached(Place.HOMESTEAD.site, origin.asLong());
			overworld.setAttached(Place.HOMESTEAD.up, true);
			HerobrineMod.LOGGER.info("homestead marked up at [{}, {}, {}] — the"
				+ " sequence can move", origin.getX(), origin.getY(), origin.getZ());
			return true;
		}
		HerobrineMod.LOGGER.warn("no buildable ground for the homestead near [{}, {}]",
			near.getX(), near.getZ());
		return false;
	}

	/**
	 * Is there dry, loaded, roughly level ground here?
	 *
	 * Samples the corners and the middle rather than every column — the ground
	 * only has to be good enough that levelling it does not leave a four-block
	 * step of dirt down one side.
	 */
	/**
	 * ASKED OF THE GENERATOR, NOT OF THE WORLD. Third fix to these lines and the
	 * first one that can work.
	 *
	 * IT USED TO REFUSE EVERY SITE EVER CONSIDERED. The grid below reads columns
	 * from x+2 to x+18 — SEVENTEEN BLOCKS, wider than a chunk — so the footprint
	 * straddled a chunk boundary for every possible value of x. The isLoaded guard
	 * therefore always found a column in a chunk nobody had generated and returned
	 * false. Not usually, not only at range: always, 256 candidates out of 256.
	 *
	 * So pick() returned null on every attempt on every tick since this was
	 * written, and the six-building sequence — every house, every map, the whole
	 * trail — could only appear by accident, on a candidate that happened to land
	 * inside chunks somebody had already walked through. Which is precisely how it
	 * behaved, and precisely what four evenings of "the map is not in the chest"
	 * actually were.
	 *
	 * The previous attempt generated the chunk at (x, z) before judging. Right
	 * instinct, fixed nothing: it generated ONE chunk where the check needs up to
	 * four. It also made the fault expensive — six generations a tick, every one
	 * thrown away, which is the "Can't keep up! Running 289 ticks behind".
	 *
	 * So it stops asking the world. ChunkGenerator.getBaseHeight samples the noise
	 * the terrain will be built from without generating anything, which is how
	 * vanilla places its own structures: correct at any distance, and free.
	 *
	 * THE TWO HEIGHTMAPS ARE THE WATER TEST. WORLD_SURFACE_WG stops at the first
	 * thing that is not air, so over a lake it returns the lake's surface;
	 * OCEAN_FLOOR_WG ignores fluid and returns the bed. On dry land they agree
	 * exactly, so one comparison replaces both the old sea-level check and the old
	 * fluid probe.
	 *
	 * It reads the noise floor rather than the canopy for the same reason the old
	 * code called floorOver instead of the heightmap: judging a forest site by the
	 * treetops makes it look wildly uneven and puts the floor above the leaves.
	 * WG-suffixed heightmaps are pre-feature, so trees are not there yet.
	 */
	/**
	 * Buildable, AND the ground under it actually exists.
	 *
	 * THIS IS THE HALF OF buildable() I DELETED, and deleting it broke three
	 * callers at once in a way that was worse than the bug it fixed.
	 *
	 * The old check opened with isLoaded and bailed if the chunk was not there.
	 * That was wrong as a SITING test — it refused every candidate, always, which
	 * is why no building could ever be placed — but it was doing a second job
	 * nobody had written down: it guaranteed that by the time a caller got a
	 * `true` back, it could safely read blocks. Every caller relied on that, and
	 * none of them said so.
	 *
	 * So buildable() now samples the generator and answers correctly at any
	 * distance, and this puts the second job back where it can be seen. Judged and
	 * real, in one call, so the two cannot come apart again.
	 *
	 * WHAT HAPPENS WITHOUT IT is not a refusal, it is a house at Y -65. Ground.topOf
	 * asks the heightmap of an ungenerated column, gets the world floor, walks down
	 * looking for footing, finds none, and returns from - 1 — one block BELOW the
	 * bottom of the world. Homestead.floorHeightAt takes the median of forty of
	 * those and builds the whole farm in the bedrock.
	 *
	 * THE WHOLE FOOTPRINT, not the one chunk at the corner. floorHeightAt reads a
	 * grid across the entire twenty by sixteen, which straddles up to four chunks —
	 * generating only the one containing (x, z) leaves three quarters of the reads
	 * still lying, and the median hides it.
	 *
	 * The last line is the cross-check and it is cheap insurance. buildable has
	 * just sworn this ground is dry and above sea level; if the real blocks
	 * disagree the read is not to be trusted whatever the reason — a cave, a
	 * feature, a chunk that did not generate — and the candidate is dropped.
	 */
	private static boolean ready(ServerLevel level, int x, int z) {
		if (!buildable(level, x, z)) {
			return false;
		}
		for (int cx = x >> 4; cx <= (x + FOOT_X) >> 4; cx++) {
			for (int cz = z >> 4; cz <= (z + FOOT_Z) >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}
		return Ground.topOf(level, x, z) > level.getSeaLevel();
	}

	/** How far the biggest of these buildings reaches from its origin. */
	private static final int FOOT_X = 20;
	private static final int FOOT_Z = 16;

	private static boolean buildable(ServerLevel level, int x, int z) {
		net.minecraft.world.level.chunk.ChunkGenerator shape =
			level.getChunkSource().getGenerator();
		net.minecraft.world.level.levelgen.RandomState noise =
			level.getChunkSource().randomState();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		// Only the building has to be level. The yard follows the ground now,
		// so a site is judged on the ground under the HOUSE rather than on the
		// whole map — which was rejecting perfectly good spots because a
		// grave marker forty blocks away would have been on a hill.
		for (int dz = 2; dz <= 14; dz += 4) {
			for (int dx = 2; dx <= 18; dx += 4) {
				int surface = shape.getBaseHeight(x + dx, z + dz,
					net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
					level, noise);
				int floor = shape.getBaseHeight(x + dx, z + dz,
					net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
					level, noise);
				if (surface != floor) {
					return false;   // standing water over it
				}
				if (floor <= level.getSeaLevel()) {
					return false;   // in the sea, or in a lake
				}
				low = Math.min(low, floor);
				high = Math.max(high, floor);
			}
		}
		// Two, not three. The footing is only three deep now, so a site that
		// varies more than this cannot be built on without a visible plinth —
		// better to walk on and find flatter ground.
		return high - low <= 2;
	}
}
