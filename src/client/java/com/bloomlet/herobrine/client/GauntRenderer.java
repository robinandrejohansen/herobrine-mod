package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.GauntEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;

/**
 * The tall one, drawn on the ENDERMAN.
 *
 * THREE ATTEMPTS AT THIS WERE A STRETCHED VILLAGER and none of them worked. The
 * thing wanted is a mouth that is always open, and on a villager head that has to
 * be a dark rectangle painted on a flat face — which reads as a dark rectangle
 * painted on a flat face. Vanilla already solved it, and the solution is one no
 * amount of paint reaches:
 *
 *     THE ENDERMAN'S FACE IS TRANSPARENT WHERE ITS MOUTH IS.
 *
 * Not dark. Absent. The front of the head has a hole in it, and behind the hole,
 * half a unit further in, is the `hat` cube — built with a NEGATIVE deformation
 * so it is smaller than the head instead of larger — carrying its own art. So the
 * mouth is genuinely recessed: a hole, a surface behind it, its own lighting, and
 * a parallax that behaves correctly as the head turns, because it is geometry
 * rather than a picture of geometry.
 *
 * Taking the model wholesale takes the proportions with it, and they are the
 * proportions that were being reached for the whole time: two-unit limbs thirty
 * units long, a narrow body, and a head that sits high on a neck that is not
 * there. Vanilla built the tall thin man already. The mod was rebuilding it badly.
 *
 * WHAT MAKES IT NOT AN ENDERMAN is two changes and they are both subtractions.
 * The skin is lifted from black to bone by tools/gen_gaunt.py, and THE EYES ARE
 * PAINTED OUT — no white bars on row 12, and no EndermanEyesLayer over them. Two
 * purple eyes are the most recognisable thing in the game; without them the face
 * has nothing in it at all except the hole, which is the entire idea.
 *
 * HumanoidMobRenderer rather than MobRenderer, because EndermanModel is a
 * HumanoidModel and its setupAnim reads a dozen fields off HumanoidRenderState —
 * attack swing, crouch, sprint, held items. MobRenderer fills none of them, and a
 * model driven from an unfilled state animates as a scarecrow.
 */
public class GauntRenderer extends HumanoidMobRenderer<
		GauntEntity, EndermanRenderState, EndermanModel<EndermanRenderState>> {

	private static final Identifier TEXTURE =
		HerobrineMod.id("textures/entity/gaunt/gaunt.png");

	public GauntRenderer(EntityRendererProvider.Context context) {
		super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
	}

	@Override
	public EndermanRenderState createRenderState() {
		return new EndermanRenderState();
	}

	/**
	 * isCreepy IS THE ANGRY POSE, and it is free.
	 *
	 * EndermanModel.setupAnim reads it and lifts the arms into the vanilla
	 * aggravated stance. Wiring it to "has a target" means the thing standing in
	 * the trees is at rest, and the thing that has decided about you is not — a
	 * silhouette change at any distance, costing one boolean.
	 *
	 * carriedBlock is left empty. It never picks anything up, and an enderman
	 * holding a block is a creature going about its business, which is the exact
	 * opposite of what this one is doing.
	 */
	@Override
	public void extractRenderState(GauntEntity entity, EndermanRenderState state,
	                               float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.isCreepy = entity.getTarget() != null;
	}

	@Override
	public Identifier getTextureLocation(EndermanRenderState state) {
		return TEXTURE;
	}
}
