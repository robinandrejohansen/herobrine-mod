package com.bloomlet.herobrine.manifest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;

/**
 * Things that happen a moment later.
 *
 * Everything he does was fired in a single tick, which is fine for a torch
 * going out and useless for anything with rhythm. Four footsteps played in one
 * tick are not four footsteps — they are one loud noise, and that is exactly
 * what the first version of them sounded like.
 *
 * Sound is almost entirely timing. A step every four ticks is running, every
 * eight is walking, every twenty is somebody being careful, and the difference
 * between those three is the difference between three completely different
 * events. None of it is available without somewhere to put "and then, later".
 *
 * Kept deliberately tiny: a list of things to run at a game tick, drained in
 * order. Not persisted, because nothing scheduled here is ever more than a few
 * seconds out — if the server stops mid-footstep the correct behaviour is for
 * the footstep to simply not happen.
 */
public final class Cadence {
	private Cadence() {}

	private record Pending(long at, Runnable action) {}

	private static final List<Pending> pending = new ArrayList<>();

	/** A runaway guard. Nothing here should ever queue more than a handful. */
	private static final int CEILING = 512;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Cadence::onTick);
	}

	/** Run this in {@code ticks} ticks' time, on the server thread. */
	public static void in(MinecraftServer server, int ticks, Runnable action) {
		if (pending.size() >= CEILING) {
			return;
		}
		pending.add(new Pending(server.overworld().getGameTime() + Math.max(0, ticks), action));
	}

	private static void onTick(MinecraftServer server) {
		if (pending.isEmpty()) {
			return;
		}
		// DRAINED FIRST, RUN AFTER, and that is not tidiness.
		//
		// This used to call the action from inside the iteration, which works
		// perfectly right up until an action schedules something of its own —
		// and then the append lands in the list being iterated and the next
		// hasNext() throws ConcurrentModificationException. Nothing in the mod
		// nested a schedule for a long time, so the fault sat here harmless;
		// the first thing that did it was the city, which queues a house a tick
		// from inside a stage that was itself queued.
		//
		// Anything scheduled while these are running simply lands in `pending`
		// and fires on a later tick, which is what a caller expects anyway.
		long now = server.overworld().getGameTime();
		List<Runnable> due = new ArrayList<>();
		Iterator<Pending> it = pending.iterator();
		while (it.hasNext()) {
			Pending next = it.next();
			if (next.at() > now) {
				continue;
			}
			it.remove();
			due.add(next.action());
		}
		for (Runnable action : due) {
			action.run();
		}
	}
}
