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
	RUMOUR(20),
	/** Seen at distance, gone when looked at. */
	WATCHER(30),
	/** He touches the world: signs, small builds, things moved. */
	TRESPASSER(40),
	/** He wears skins and names, possesses mobs, takes things. */
	MIMIC(50),
	/** He stops keeping his distance. */
	HUNTER(60),
	/** Hordes, weather, sustained assault. */
	SIEGE(60);

	/**
	 * HOW LONG THIS CHAPTER MUST BE LIVED IN, in minutes, before the next place
	 * will site — AND IT CLIMBS.
	 *
	 * A flat floor was the first version and it is wrong in both directions at
	 * once. Twenty minutes is generous for RUMOUR, where the content is four
	 * kinds of trace and the whole intent is that nobody is sure anything is
	 * happening yet; the same twenty minutes is nothing at all for HUNTER, which
	 * has the hunt, the dark, the herd, the mimic and the theft in it and cannot
	 * show a fraction of that in twenty minutes.
	 *
	 * So it climbs: twenty, thirty, forty, fifty, sixty. Three hours and twenty
	 * minutes of floor across the five gates, before a single block of travel is
	 * counted.
	 *
	 * The shape of that curve is the point rather than the total. A short first
	 * chapter means the world starts happening quickly, which is what stops a new
	 * player concluding the mod is not installed. Long later chapters mean the
	 * phases with the most in them get room to actually show it — and by then the
	 * players have a base, a reason to be somewhere, and something to lose, so
	 * time spent in a phase has stopped being time spent waiting.
	 */
	public final int duesMinutes;

	Phase(int duesMinutes) {
		this.duesMinutes = duesMinutes;
	}

	/** The floor in ticks. */
	public long dues() {
		return this.duesMinutes * 1200L;
	}

	public boolean atLeast(Phase other) {
		return this.ordinal() >= other.ordinal();
	}

}
