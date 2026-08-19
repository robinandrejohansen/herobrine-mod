package com.bloomlet.herobrine.manifest;

import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Something breathing on the other side of the rock.
 *
 * The cheapest frightening thing in the whole mod, and possibly the best,
 * because the content is what is NOT there.
 *
 * A player alone underground hears a slow heartbeat from somewhere close. It
 * moves a little between beats, so it reads as a thing rather than as a
 * machine. They stop. They listen. They work out roughly which wall it is
 * behind and they start digging — and then it gets further away, and quieter,
 * and stops, and they break through into ordinary stone. There was never
 * anything there and there never will be.
 *
 * THE DENIABILITY IS THE POINT, and this one has the best excuse in the game
 * built into it. It is the warden's heartbeat, so every player who hears it
 * has an immediate, sensible, entirely wrong explanation ready: there is a
 * deep dark somewhere near. They will tell themselves that, and go back to
 * mining, and be wrong. A sound the player cannot explain is a mod announcing
 * itself; a sound they CAN explain is the one that gets under the door.
 *
 * Deliberately placed inside solid rock. Not in a cave they could walk to, not
 * behind a corner, not anywhere a search could resolve — buried, so that
 * digging toward it is a decision with a cost and no payoff. The player spends
 * four minutes and twenty blocks of pickaxe on nothing, which is exactly what
 * happened to everybody in the journal.
 *
 * Nothing here spawns, damages, takes or blocks anything. It is sound and
 * silence and the player's own imagination doing all of the work.
 */
public final class Deeps {
	private Deeps() {}

	/** Below this, and with no sky, it is a cave rather than a cellar. */
	private static final int DEEP_ENOUGH = 52;

	/**
	 * How far off it starts.
	 *
	 * Close enough to be worth digging at, far enough that the player has to
	 * commit before they can find out. Under about five and they would break
	 * through in three blocks and the whole thing collapses into "nothing
	 * there"; past twelve and the sound is too faint to act on.
	 */
	private static final double NEAR = 6.0;
	private static final double FAR = 11.0;

	/** Real ones are down here too, and they must not be drowned out. */
	private static final double CLEAR_OF_REAL_ONES = 48.0;

	/**
	 * Play it, if the player is somewhere it would land.
	 *
	 * @return false, silently, if they are on the surface, if they are near
	 *         something that is genuinely breathing, or if there is no solid
	 *         rock nearby to put it behind
	 */
	public static boolean breathing(ServerLevel level, ServerPlayer player) {
		if (!underground(level, player)) {
			return false;
		}
		if (realOneNearby(level, player)) {
			return false;
		}

		RandomSource random = level.getRandom();
		Vec3 from = buried(level, player, random);
		if (from == null) {
			return false;
		}

		Phase phase = Wrath.phase(level.getServer());
		// It goes on longer the further in you are. Early it is four or five
		// beats and easy to miss; by the end it will keep at it for half a
		// minute, which is long enough that a player has to decide whether to
		// stay down here.
		int beats = switch (phase) {
			case RUMOUR, WATCHER -> 4 + random.nextInt(3);
			case TRESPASSER, MIMIC -> 6 + random.nextInt(4);
			case HUNTER, SIEGE -> 9 + random.nextInt(5);
		};

		Vec3 at = from;
		int when = 0;
		for (int i = 0; i < beats; i++) {
			// It wanders while it is there and then it leaves.
			//
			// A sound repeating at one fixed point is a speaker in a wall. The
			// same sound arriving from slightly different places each time is
			// something moving about, and the player's ear makes that
			// distinction long before they have thought about it — the same
			// reason the footsteps walk a line instead of stacking up.
			//
			// The last third drifts AWAY. That is what turns a player digging
			// into a player who was too slow: the wall opens onto plain stone
			// and the thing they were chasing is already further off.
			boolean leaving = i >= beats - Math.max(2, beats / 3);
			Vec3 away = at.subtract(player.position()).normalize();
			Vec3 wander = new Vec3(random.nextDouble() - 0.5, (random.nextDouble() - 0.5) * 0.6,
				random.nextDouble() - 0.5).scale(1.6);
			at = at.add(wander).add(leaving ? away.scale(1.5 + random.nextDouble()) : Vec3.ZERO);

			double range = at.distanceTo(player.position());
			// Loud enough to carry that far through rock — Minecraft has no
			// occlusion, so the only thing standing between the player and this
			// is the falloff, and the falloff is 16 blocks times the volume.
			final float volume = (float)Math.min(1.0, 0.40 + range * 0.055);
			// Under the warden's own register. Slower and lower than the thing
			// they will assume it is, which is the detail that nags at anybody
			// who has actually met one.
			final float pitch = 0.66F + random.nextFloat() * 0.10F;
			final double x = at.x;
			final double y = at.y;
			final double z = at.z;
			Cadence.in(level.getServer(), when, () ->
				level.playSound(null, x, y, z, com.bloomlet.herobrine.sound.ModSounds.BREATH,
					SoundSource.HOSTILE, volume, pitch));

			// Roughly two seconds, never exactly. A metronome is a machine.
			when += 34 + random.nextInt(14);
		}
		return true;
	}

	/**
	 * Somewhere inside the rock, out of reach and out of sight.
	 *
	 * Every candidate has to be solid AND surrounded by solid, because a spot
	 * chosen on the far wall of the cave the player is standing in is a spot
	 * they can walk to and look at, and finding an empty corner is a much
	 * smaller thing than finding no way to look at all.
	 */
	private static @Nullable Vec3 buried(ServerLevel level, ServerPlayer player,
	                                     RandomSource random) {
		for (int attempt = 0; attempt < 32; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double range = NEAR + random.nextDouble() * (FAR - NEAR);
			double x = player.getX() + Math.cos(angle) * range;
			double z = player.getZ() + Math.sin(angle) * range;
			double y = player.getY() + random.nextInt(7) - 3;

			BlockPos at = BlockPos.containing(x, y, z);
			if (!solid(level, at)) {
				continue;
			}
			boolean walled = true;
			for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
				if (!solid(level, at.relative(side))) {
					walled = false;
					break;
				}
			}
			if (walled) {
				return new Vec3(x, y, z);
			}
		}
		return null;
	}

	private static boolean solid(ServerLevel level, BlockPos at) {
		return level.getBlockState(at).isSolidRender();
	}

	/**
	 * Deep, and with nothing above.
	 *
	 * Both tests, because either alone is wrong. Depth alone fires in a player's
	 * own strip mine two blocks under a lit farm; sky alone fires the moment
	 * they walk into a barn.
	 */
	private static boolean underground(ServerLevel level, ServerPlayer player) {
		return player.getY() < DEEP_ENOUGH && !level.canSeeSky(player.blockPosition());
	}

	/**
	 * Is one of the real ones within earshot?
	 *
	 * If a player is outside the cells and hears this, the two sounds argue
	 * with each other — the shut-in has its own heartbeat and the whole reason
	 * that one works is that there IS something behind the door. Nothing is
	 * gained by making them doubt the one thing down here that is true.
	 */
	private static boolean realOneNearby(ServerLevel level, ServerPlayer player) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class,
				player.getBoundingBox().inflate(CLEAR_OF_REAL_ONES))) {
			if (Feral.isFeral(mob)) {
				return true;
			}
		}
		return false;
	}
}
