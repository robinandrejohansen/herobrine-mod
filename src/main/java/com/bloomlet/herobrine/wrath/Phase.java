package com.bloomlet.herobrine.wrath;

/**
 * How far along the haunting is. See DESIGN.md §3.
 *
 * Phases ADD to what he can do; they never replace it. A phase 5 world can
 * still get a silent phase 1 stare, and should — escalation that only ramps
 * upward becomes exhausting, and the quiet ones are what make the loud ones
 * land.
 */
public enum Phase {
	/** Traces only. The player should not yet believe anything is happening. */
	RUMOUR(0),
	/** Seen at distance, gone when looked at. */
	WATCHER(60),
	/** He touches the world: signs, small builds, things moved. */
	TRESPASSER(200),
	/** He wears skins and names, possesses mobs, takes things. */
	MIMIC(500),
	/** He stops keeping his distance. */
	HUNTER(1000),
	/** Hordes, weather, sustained assault. */
	SIEGE(1800);

	public final int threshold;

	Phase(int threshold) {
		this.threshold = threshold;
	}

	public static Phase forWrath(int wrath) {
		Phase result = RUMOUR;
		for (Phase phase : values()) {
			if (wrath >= phase.threshold) {
				result = phase;
			}
		}
		return result;
	}

	public boolean atLeast(Phase other) {
		return this.ordinal() >= other.ordinal();
	}

	/** Wrath still to earn before the next phase, or -1 at the last one. */
	public int remaining(int wrath) {
		int next = this.ordinal() + 1;
		return next < values().length ? values()[next].threshold - wrath : -1;
	}
}
