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
	RUMOUR(0, 20),
	/** Seen at distance, gone when looked at. */
	WATCHER(60, 30),
	/** He touches the world: signs, small builds, things moved. */
	TRESPASSER(200, 40),
	/** He wears skins and names, possesses mobs, takes things. */
	MIMIC(500, 50),
	/** He stops keeping his distance. */
	HUNTER(1000, 60),
	/** Hordes, weather, sustained assault. */
	SIEGE(1800, 60);

	public final int threshold;

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

	Phase(int threshold, int duesMinutes) {
		this.threshold = threshold;
		this.duesMinutes = duesMinutes;
	}

	/** The floor in ticks. */
	public long dues() {
		return this.duesMinutes * 1200L;
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

	/**
	 * HOW DEEP INTO THIS PHASE, nought to one.
	 *
	 * Phases were a step function: you were in MIMIC or you were not, and
	 * nothing about the first minute of it differed from the last hour. Six
	 * steps across a whole campaign is not a shape, it is a staircase, and it is
	 * why each phase read as flat however good the individual events were.
	 *
	 * This is the ramp. Measured from the wrath total recorded when the phase
	 * BEGAN rather than from the phase's own threshold, because once the story
	 * advances on discovery the two have nothing to do with each other — a group
	 * can walk into the church at four hundred wrath or at four thousand.
	 *
	 * The span is the gap this phase used to occupy on the old wrath ladder,
	 * which is a reasonable measure of "a phase's worth of provocation" and
	 * costs nothing to reuse.
	 */
	public float into(int wrathNow, int wrathAtEntry) {
		int span = this.span();
		if (span <= 0) {
			return 1.0F;
		}
		float done = (float)(wrathNow - wrathAtEntry) / span;
		return done < 0.0F ? 0.0F : (done > 1.0F ? 1.0F : done);
	}

	/** A phase's worth of provocation, off the old ladder. */
	public int span() {
		int next = this.ordinal() + 1;
		return next < values().length
			? values()[next].threshold - this.threshold
			: 800;
	}

	/** Wrath still to earn before the next phase, or -1 at the last one. */
	public int remaining(int wrath) {
		int next = this.ordinal() + 1;
		return next < values().length ? values()[next].threshold - wrath : -1;
	}
}
