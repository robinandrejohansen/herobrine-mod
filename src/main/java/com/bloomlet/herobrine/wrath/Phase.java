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
