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
		reshape(this.getModel());
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
	/**
	 * A VILLAGER'S HEAD AND A BODY WITH SOMETHING IN IT.
	 *
	 * The enderman mesh brought its proportions with it, and those proportions are
	 * the one thing about it that is wrong for this: an eight-cube head where a
	 * villager's is eight by ten, and limbs two units square. So it read as an
	 * enderman wearing villager paint — which was the third of three attempts this
	 * creature has had, arrived at from the other direction.
	 *
	 * SCALED, NOT REMODELLED, which is the same call TurnedRenderer makes about the
	 * nose and for the same reason. A larger cube samples a larger UV rectangle and
	 * starts pulling in whatever sits next to it on the sheet — and this sheet has
	 * alpha-0 padding around the parts, which solid materials draw as BLACK. That
	 * cost an evening once already, on the shoulders and the arms. Scaling a baked
	 * part stretches the SAME texels over more space: safe, and exactly the look.
	 *
	 * A quarter taller on the head, which is the villager's own ratio. Sixty per
	 * cent thicker on the limbs — two units is a stick, and the thing is meant to
	 * read as a man who has been drawn out rather than as a spider. The body only a
	 * sixth, because the enderman torso is already wide and the silhouette should
	 * stay narrow-shouldered.
	 *
	 * The hat is the mouth. It is left alone — see the note above. Scaling it moves
	 * the recess off the hole and the whole effect with it.
	 */
	private static void reshape(EndermanModel<EndermanRenderState> model) {
		// THE PUBLIC FIELDS, NOT root().getChild("head").
		//
		// HumanoidModel hands these out directly as public final ModelParts, and
		// getChild is a string lookup that throws at RUNTIME on a name that is not
		// there — so a typo or a rename compiles perfectly and then crashes the
		// client the first time one of these walks on screen. The fields are checked
		// by the compiler and cannot be wrong.
		model.head.yScale = 1.25F;

		model.body.xScale = 1.16F;
		model.body.zScale = 1.16F;

		for (net.minecraft.client.model.geom.ModelPart limb : new
				net.minecraft.client.model.geom.ModelPart[] {
					model.rightArm, model.leftArm, model.rightLeg, model.leftLeg }) {
			limb.xScale = 1.6F;
			limb.zScale = 1.6F;
		}
	}

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
