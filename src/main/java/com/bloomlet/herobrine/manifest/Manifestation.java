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

	/** He is simply there, at distance, and gone when you look. */
	THE_STARE(Phase.WATCHER, 10) {
		@Override
		public boolean run(ServerLevel level, ServerPlayer player) {
			return HauntingSpawner.spawnBehind(level, player);
		}
	};

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
