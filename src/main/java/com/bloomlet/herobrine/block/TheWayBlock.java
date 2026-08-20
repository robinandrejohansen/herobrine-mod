package com.bloomlet.herobrine.block;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * THE WAY THROUGH, AND IT IS THE ONLY DOOR IN THE MOD THAT OPENS BOTH WAYS.
 *
 * Killing him used to raise a portal in front of the player and then break it,
 * six seconds later, before anybody could reach it. That was a good ending and a
 * closed one — the sentence it spoke was "he was going somewhere and he was
 * almost through", and the only thing the player could do with it was read it.
 *
 * IT HOLDS NOW. And the reason it holds is worse than the reason it broke: he
 * did not finish it. Killing him did. The last thing standing between him and
 * wherever he was going was the fight the player just won, and the frame closes
 * over the body.
 *
 * A CUSTOM BLOCK RATHER THAN A NETHER PORTAL, and there is no way round it.
 * Vanilla's portal block resolves its destination by hardcoded dimension key —
 * the nether one goes to the nether and nowhere else, and the end one is worse.
 * Borrowing either would mean a mixin into vanilla's teleport path to change
 * where somebody else's block sends people, which is exactly the class of change
 * that has broken this mod twice before. Our own block owns its own destination
 * and cannot affect anybody else's.
 *
 * It is deliberately NOT obtainable. No item form, no recipe, no loot table —
 * it exists where the mod puts it and nowhere else. A player who could carry a
 * stack of these home would have a fast-travel network, and this is a door to
 * one place, opened once, by killing something.
 */
public class TheWayBlock extends Block {

	/** Where it goes. Declared in data/herobrine/dimension/his_world.json. */
	public static final ResourceKey<Level> HIS_WORLD =
		ResourceKey.create(Registries.DIMENSION, HerobrineMod.id("his_world"));

	public TheWayBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Step in and you are somewhere else.
	 *
	 * IMMEDIATE, not the nether's four-second stand-and-wait. That delay exists
	 * so a player mining next to a portal does not fall through it by accident,
	 * and nothing here is next to anything by accident: this is one frame, in
	 * one clearing, standing over a body. Somebody in it walked in on purpose.
	 *
	 * The vanilla portal cooldown is still used, because it is the thing that
	 * stops the arrival portal on the far side from throwing them straight back.
	 */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
	                            InsideBlockEffectApplier effects, boolean flag) {
		if (!(level instanceof ServerLevel here) || entity.isPassenger() || entity.isVehicle()) {
			return;
		}
		if (entity.isOnPortalCooldown()) {
			// Refreshed while they stand in it, so stepping out and back in is
			// what it takes to go again rather than simply waiting.
			entity.setPortalCooldown();
			return;
		}
		if (!(entity instanceof ServerPlayer player)) {
			return;   // one door, for people. Not a mob highway
		}

		ServerLevel bound = here.dimension().equals(HIS_WORLD)
			? here.getServer().overworld()
			: here.getServer().getLevel(HIS_WORLD);
		if (bound == null) {
			HerobrineMod.LOGGER.error("his_world is not loaded — the datapack dimension is missing");
			return;
		}

		// WHERE THEY COME OUT IS BUILT BEFORE THEY GET THERE. His world is
		// generated on the nether's noise settings, which means lava at the
		// height anybody would arrive at; dropping somebody into it because the
		// terrain happened to be liquid there would end forty hours of play in a
		// second, through no decision of theirs.
		BlockPos landing = com.bloomlet.herobrine.structure.TheWay.landing(bound, player);
		player.setPortalCooldown();

		// HIS DOOR, HIS SOUND. DO_NOTHING rather than PLAY_PORTAL_SOUND, because
		// that constant plays vanilla's nether travel cue and a player who has
		// been to the nether has heard it a hundred times.
		//
		// Outbound is pitched down and homebound up, off the same asset, so the
		// direction is audible without a second file that could drift from the
		// first. Going somewhere worse should sound lower than coming back.
		boolean leaving = !here.dimension().equals(HIS_WORLD);
		float pitch = leaving ? 0.82F : 1.14F;

		// Where they LEFT, before they are gone, and audible to whoever is still
		// standing there. On a server this is most of the point: the rest of the
		// group does not follow, they just hear the room change and then find
		// themselves alone next to a frame with nobody in it.
		here.playSound(null, pos, com.bloomlet.herobrine.sound.ModSounds.CROSSING,
			net.minecraft.sounds.SoundSource.HOSTILE, 1.1F, pitch);

		player.teleport(new TeleportTransition(bound,
			new net.minecraft.world.phys.Vec3(landing.getX() + 0.5, landing.getY(),
				landing.getZ() + 0.5),
			net.minecraft.world.phys.Vec3.ZERO, player.getYRot(), player.getXRot(),
			TeleportTransition.DO_NOTHING));

		// AND AGAIN ON THE FAR SIDE, after the teleport, so the tail of it is the
		// first thing they hear in the new place rather than the last thing they
		// heard in the old one. Two and a half seconds is about what a dimension
		// change costs, which means the crossing covers the load instead of
		// leaving them in silence looking at chunks arriving.
		bound.playSound(null, landing, com.bloomlet.herobrine.sound.ModSounds.CROSSING,
			net.minecraft.sounds.SoundSource.HOSTILE, 1.1F, pitch);
		HerobrineMod.LOGGER.info("{} went through, to {}", player.getName().getString(),
			bound.dimension().identifier());
	}
}
