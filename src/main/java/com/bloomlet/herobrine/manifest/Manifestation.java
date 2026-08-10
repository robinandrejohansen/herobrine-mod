package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.entity.HauntingSpawner;
import com.bloomlet.herobrine.wrath.Phase;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * One thing he can do.
 *
 * Everything he ever does goes through this list, so the director (and only
 * the director) decides pacing. Adding content later means adding a constant
 * here, not another tick handler quietly firing on its own schedule — which
 * is how mods like this end up feeling like noise.
 */
public enum Manifestation {

	/** Footsteps behind you, once, with nothing there. */
	FOOTSTEPS(Phase.RUMOUR, 10) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Traces.footsteps(level, player);
		}
	},

	/** A sound that belongs somewhere else. */
	WRONG_SOUND(Phase.RUMOUR, 8) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Traces.wrongSound(level, player);
		}
	},

	/** A torch you placed, on the ground, unlit. */
	SNUFFED_TORCH(Phase.RUMOUR, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Traces.snuffTorch(level, player);
		}
	},

	/** A creeper behind you that never goes off. */
	THE_FUSE(Phase.RUMOUR, 6) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Traces.fuse(level, player);
		}
	},

	/** One of your animals stops being an animal. */
	POSSESSED_MOB(Phase.MIMIC, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Possession.take(level, player);
		}
	},

	/**
	 * One of your friends, four hundred blocks from where your friend is.
	 *
	 * Weighted LOW for the phase it unlocks in, and it is the one number in this
	 * table worth defending. Every other event here can happen twice in an
	 * evening and still land, because they are all things you half-saw. This one
	 * is a thing the whole server discusses, and a mystery that recurs on a
	 * schedule is not a mystery — it is a feature, and once it is a feature the
	 * tab list never lies again.
	 */
	THE_STRANGER(Phase.MIMIC, 4) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Mimicry.appear(level, player);
		}
	},

	/**
	 * He has been in your chest, and he left one behind.
	 *
	 * The only event that touches a player's belongings, so it is weighted low
	 * and it is the one with the strictest rule behind it: everything taken is
	 * kept and comes back. See Hoard.
	 */
	THE_TAKING(Phase.TRESPASSER, 5) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Hoard.steal(level, player);
		}
	},

	/** Something old, at the edge of your world, that was not there yesterday. */
	THE_RUIN(Phase.TRESPASSER, 8) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Ruins.raise(level, player);
		}
	},

	/** A page of someone else's account, on the floor where you will find it. */
	THE_PAGE(Phase.TRESPASSER, 9) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Journal.leavePage(level, player);
		}
	},

	/** Four words on your wall. The first thing you cannot argue with. */
	THE_SIGN(Phase.TRESPASSER, 14) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signs.write(level, player);
		}
	},

	/**
	 * Something breathing behind the rock, that is not behind the rock.
	 *
	 * Weighted high for a phase 0 event because it refuses itself most of the
	 * time — it needs the player deep, roofed, and away from the real ones, so
	 * the effective rate is far lower than the number suggests.
	 */
	THE_BREATHING(Phase.RUMOUR, 11) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Deeps.breathing(level, player);
		}
	},

	/**
	 * The day ends, the storm arrives, and every torch you own goes out.
	 *
	 * The first event that happens TO the world rather than in it. Everything
	 * before this is deniable and local; nobody talks themselves out of the
	 * afternoon ending.
	 */
	THE_DARK(Phase.HUNTER, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return TheDark.fall(level, player);
		}
	},

	/**
	 * He follows, and none of the rules you learned about him apply.
	 *
	 * The only manifestation that does not end when the player stops paying
	 * attention, which is the entire reason HUNTER needed something of its
	 * own: every event before this one is something that happens to you while
	 * you get on with your day, and this is the one that will not let you.
	 */
	THE_HUNT(Phase.HUNTER, 14) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			HauntingSpawner.Outcome outcome = HauntingSpawner.place(level, player, false, true);
			if (outcome == HauntingSpawner.Outcome.PLACED) {
				return true;
			}
			ManifestationDirector.refused(outcome.reason());
			return false;
		}
	},

	// ---- THE SIGNS FROM THE ORIGINAL STORY -------------------------------
	//
	// Weighted heavily, and at RUMOUR, because these are what the legend
	// actually consists of. The 2010 account is almost entirely a list of marks
	// left on a world, not a list of things that happen to you — and a mod
	// about Herobrine that opens with footsteps and creeper sounds is a horror
	// mod wearing his name, while one that opens with a stripped grove and a
	// tunnel nobody dug is the story itself.

	/**
	 * A room cut into the rock behind your caves.
	 *
	 * Eight kinds, scattered and unnumbered. The five houses are a sequence
	 * read in order; these are the opposite — one fact each and no context, so
	 * a player who finds three over a week has assembled something nobody wrote
	 * down for them.
	 */
	THE_CHAMBER(Phase.RUMOUR, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Chambers.cut(level, player);
		}
	},

	/** Trees with every leaf taken off, in a rough circle. */
	THE_GROVE(Phase.RUMOUR, 13) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signature.grove(level, player);
		}
	},

	/** One redstone torch, burning, in a cave nobody has been in. */
	THE_TORCH(Phase.RUMOUR, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signature.torch(level, player);
		}
	},

	/**
	 * A small perfect pyramid of sand, standing in open water.
	 *
	 * Held to WATCHER only because it needs an ocean and will refuse itself
	 * most of the time inland — not because it is too strong for phase one. If
	 * anything it is the most deniable thing in the mod: it is just sand.
	 */
	THE_PYRAMID(Phase.WATCHER, 11) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signature.pyramid(level, player);
		}
	},

	/** Two blocks square, dead straight, eighty long, going nowhere. */
	THE_TUNNEL(Phase.WATCHER, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signature.tunnel(level, player);
		}
	},

	/**
	 * A passage, bricked up.
	 *
	 * The one trace made in response to the player rather than to the world, so
	 * it waits until TRESPASSER — the phase whose whole job is him crossing
	 * from being in your world to being in your business.
	 */
	THE_SEAL(Phase.TRESPASSER, 11) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return Signature.seal(level, player);
		}
	},

	// ---- END THE SIGNS ---------------------------------------------------

	/**
	 * Him, in a cave, for half a second.
	 *
	 * At RUMOUR, and that is the point of it: this is the only way he can
	 * appear in the first phase without breaking deniability, because the
	 * player never gets long enough to be sure. A stare at RUMOUR would answer
	 * the question the whole phase exists to keep open. A glimpse asks it.
	 */
	THE_GLIMPSE(Phase.RUMOUR, 12) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return HauntingSpawner.glimpse(level, player)
				== HauntingSpawner.Outcome.PLACED;
		}
	},

	/**
	 * Him, down the tunnel, and there is no way round him.
	 *
	 * The cave counterpart to the stare rather than a version of it. At
	 * WATCHER, because unlike the glimpse this one IS resolvable — you get
	 * long enough to be certain — and being certain is what WATCHER is for.
	 */
	THE_PASSAGE(Phase.WATCHER, 14) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return HauntingSpawner.passage(level, player)
				== HauntingSpawner.Outcome.PLACED;
		}
	},

	/** He is simply there, at distance, and gone when you look. */
	/**
	 * Weighted 18 rather than 10, which is the heaviest thing in the mod.
	 *
	 * He is the mod. Everything else here is evidence that he exists, and it
	 * was collectively drowning him out: at WATCHER the stare was about
	 * eighteen per cent of picks, so roughly one candidate an hour before the
	 * light gate had even been consulted. A player can meet nine traces and
	 * still not have met HIM, which is the wrong way round.
	 */
	THE_STARE(Phase.WATCHER, 18) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			HauntingSpawner.Outcome outcome = HauntingSpawner.place(level, player, false);
			if (outcome == HauntingSpawner.Outcome.PLACED) {
				return true;
			}
			ManifestationDirector.refused(outcome.reason());
			return false;
		}
	};

	/**
	 * Has the player switched this one off?
	 *
	 * Per event rather than one master toggle, because the requests DESIGN §9
	 * anticipated are specific: people want the stare and not the theft, or the
	 * whole thing except the part that takes a wall out. A single on/off would
	 * answer none of them.
	 */
	public boolean allowed() {
		com.bloomlet.herobrine.Config config = com.bloomlet.herobrine.Config.get();
		if (!config.enabled) {
			return false;
		}
		return switch (this) {
			case FOOTSTEPS, WRONG_SOUND, SNUFFED_TORCH, THE_FUSE -> config.traces;
			case THE_GROVE, THE_TORCH, THE_PYRAMID, THE_TUNNEL, THE_SEAL,
			     THE_CHAMBER -> config.signs;
			case THE_BREATHING -> config.theBreathing;
			case POSSESSED_MOB -> config.possession;
			case THE_STRANGER -> config.theStranger;
			case THE_TAKING -> config.theTaking;
			case THE_RUIN -> config.ruins;
			case THE_PAGE, THE_SIGN -> config.signsAndPages;
			case THE_DARK -> config.theDark;
			case THE_HUNT -> config.theHunt;
			case THE_STARE, THE_GLIMPSE, THE_PASSAGE -> config.theStare;
		};
	}

	/** Earliest phase this can appear in. */
	public final Phase minimum;
	/** Relative likelihood among everything else eligible. */
	public final int weight;

	Manifestation(Phase minimum, int weight) {
		this.minimum = minimum;
		this.weight = weight;
	}

	/**
	 * @return false if the world could not accommodate it right now — too
	 *         bright, nowhere to stand, already one nearby. Returning false
	 *         must be cheap and silent: he simply did not appear this time,
	 *         and the director spends nothing.
	 */
	public abstract boolean run(ServerLevel level, ServerPlayer player);
}
