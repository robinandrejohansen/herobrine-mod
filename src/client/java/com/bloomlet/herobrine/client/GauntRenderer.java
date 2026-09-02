package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.GauntEntity;

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
		super(context, new Stretched(mesh().bakeRoot()), 0.5F);
		this.addLayer(new GauntEyesLayer<>(this));
	}

	/**
	 * THE ENDERMAN'S MESH, TRANSCRIBED, PLUS A NOSE.
	 *
	 * A villager's nose is the one feature that says which of them this used to be,
	 * and it cannot be painted. The face is a flat plane; a nose drawn on it is a
	 * darker rectangle on a flat plane, and the mod has already learned that lesson
	 * once about the mouth. It has to be a cube standing off the front of the head.
	 *
	 * WHICH MEANS OWNING THE MESH, because there is nowhere to hang a seventh cube
	 * otherwise. ModelPart's children map is private and final, so a baked part
	 * cannot be added to; and this Fabric API has no EntityModelLayerRegistry, so a
	 * new layer cannot be registered for the game to bake. What is left is public
	 * and simple: build the MeshDefinition here and bake it with
	 * LayerDefinition.bakeRoot(), which needs no registration at all.
	 *
	 * EVERY NUMBER BELOW IS VANILLA'S, read out of EndermanModel.createBodyLayer's
	 * own bytecode rather than remembered — head 8x8x8 at (0,-13,0), the hat the
	 * same box deformed by -0.5, the body 8x12x4 at (0,-14,0), and four limbs of
	 * 2x30x2 which are the proportions this creature exists for. A wrong number
	 * here is not subtle: the thing arrives visibly broken, which is the one mercy
	 * of transcribing geometry.
	 *
	 * The nose is 2x2x2 at (-1,-6,-5): two texels wide, centred, standing one texel
	 * clear of the face. It lands between the eyes and above the mouth by
	 * construction — the eyes occupy face texels 8-10 and 13-15 and the nose
	 * occupies 11-12, so they cannot collide however the head is scaled.
	 *
	 * Its net goes at texOffs(32,0), which is the only free ground on the sheet:
	 * the head has x 0-32 y 0-16, the hat x 0-32 y 16-32, the body x 32-56 y 16-32
	 * and the limbs x 56-64. That leaves x 32-56, y 0-16 untouched by vanilla.
	 */
	private static net.minecraft.client.model.geom.builders.LayerDefinition mesh() {
		net.minecraft.client.model.geom.builders.MeshDefinition mesh =
			net.minecraft.client.model.HumanoidModel.createMesh(
				net.minecraft.client.model.geom.builders.CubeDeformation.NONE, -14.0F);
		net.minecraft.client.model.geom.builders.PartDefinition root = mesh.getRoot();

		net.minecraft.client.model.geom.builders.PartDefinition head =
			root.addOrReplaceChild("head",
				net.minecraft.client.model.geom.builders.CubeListBuilder.create()
					.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				net.minecraft.client.model.geom.PartPose.offset(0.0F, -13.0F, 0.0F));

		// The mouth's back wall. Negative deformation, so it is SMALLER than the
		// head and sits half a unit inside it — that recess is the whole reason
		// this creature is built on an enderman. See gen_gaunt.py.
		head.addOrReplaceChild("hat",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
					new net.minecraft.client.model.geom.builders.CubeDeformation(-0.5F)),
			net.minecraft.client.model.geom.PartPose.ZERO);

		// AND THE NOSE, which is the only thing here vanilla does not have.
		head.addOrReplaceChild("nose",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(32, 0).addBox(-1.0F, -6.0F, -5.0F, 2.0F, 2.0F, 2.0F),
			net.minecraft.client.model.geom.PartPose.ZERO);

		root.addOrReplaceChild("body",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(32, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
			net.minecraft.client.model.geom.PartPose.offset(0.0F, -14.0F, 0.0F));

		root.addOrReplaceChild("right_arm",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(56, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F),
			net.minecraft.client.model.geom.PartPose.offset(-5.0F, -12.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(56, 0).mirror()
				.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F),
			net.minecraft.client.model.geom.PartPose.offset(5.0F, -12.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(56, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F),
			net.minecraft.client.model.geom.PartPose.offset(-2.0F, -5.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			net.minecraft.client.model.geom.builders.CubeListBuilder.create()
				.texOffs(56, 0).mirror()
				.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F),
			net.minecraft.client.model.geom.PartPose.offset(2.0F, -5.0F, 0.0F));

		return net.minecraft.client.model.geom.builders.LayerDefinition
			.create(mesh, 64, 32);
	}

	/**
	 * THE RESHAPE HAS TO RUN EVERY FRAME, AND FOR ITS WHOLE LIFE IT RAN ONCE.
	 *
	 * It was in the constructor, which is where one-time setup belongs and is
	 * exactly where this particular setup does nothing whatsoever:
	 *
	 *     Model.setupAnim  ->  resetPose()
	 *       ->  every part:  ModelPart.loadPose(initialPose)
	 *             ->  writes x, y, z, xRot, yRot, zRot, xScale, yScale, zScale
	 *
	 * PartPose CARRIES SCALE. resetPose does not merely put the rotations back, it
	 * puts the scale back — and setupAnim runs once per frame before anything is
	 * drawn. Every number reshape() assigned was overwritten before the first pixel
	 * of the first frame.
	 *
	 * Which is why it kept reading as an enderman wearing villager paint whatever
	 * the values below said: it WAS one. An eight-unit head, two-unit limbs, and
	 * none of the proportions this file spends a page arguing for ever reached the
	 * screen. "He looks cute with a small head" was a correct bug report about a
	 * line of code that could not be seen to be wrong — the constructor is right,
	 * the fields are right, the compiler is happy, and in the game you are looking
	 * at a coherent creature. Just the other one.
	 *
	 * So it goes on AFTER super.setupAnim: last thing before the draw, with nothing
	 * left to run that could undo it.
	 *
	 * WORTH KNOWING GENERALLY. Anything written onto a ModelPart from outside
	 * setupAnim is written onto a surface that is wiped every frame. There is no
	 * error, no warning, and no visible fault — only a model that quietly ignores
	 * you.
	 */
	private static final class Stretched extends EndermanModel<EndermanRenderState> {
		Stretched(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(EndermanRenderState state) {
			super.setupAnim(state);
			reshape(this);
			slam(this, state.attackTime);
		}
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
	 * HALF AGAIN ON THE HEAD, WHICH IS PAST A VILLAGER ON PURPOSE.
	 *
	 * A villager's head is eight by ten and the first attempt at this matched it
	 * exactly — 1.25 — on the reasoning that the creature is a villager underneath
	 * and should wear a villager's proportion. That reasoning ignores what the head
	 * is sitting ON. A villager is thirty-one units tall, so ten units of head is a
	 * third of it; this thing is fifty, so the same ten units is a fifth, and a
	 * fifth reads as a small head on a long body no matter whose ratio it came
	 * from. Proportion is a fraction and only one of its terms was being copied.
	 *
	 * Twelve units, so: half again the enderman's, a fifth over a villager's, and
	 * near enough a villager's FRACTION of this body. Eight wide and twelve tall is
	 * also a long skull rather than a big one, which is the difference between the
	 * thing being imposing and the thing being a bobblehead.
	 *
	 * Sixty per cent thicker on the limbs — two units is a stick, and it should
	 * read as a man drawn out rather than as a spider. The body only a sixth,
	 * because the enderman torso is already wide and the silhouette wants to stay
	 * narrow-shouldered.
	 */
	/**
	 * IT SWINGS LIKE AN IRON GOLEM, WHICH MEANS BOTH ARMS AT ONCE.
	 *
	 * The enderman model has no attack animation. HumanoidModel has one and it is
	 * the wrong one: a single arm scything across the body, which is a person
	 * hitting something. This creature is three blocks of it and the whole of what
	 * it does to you is pick you up and put you somewhere else, so the swing has to
	 * be the golem's — both arms up over the head together, then down.
	 *
	 * Vanilla's own numbers, out of IronGolemModel.setupAnim:
	 *
	 *     xRot = -2.0 + 1.5 * triangleWave(t, 10)
	 *
	 * with t counting down from ten. triangleWave runs -1 to 1 across the period,
	 * so the arms travel from -0.5 radians to -3.5 and back: raised, then hurled
	 * forward past vertical. Both arms take the same value, which is the entire
	 * reason it reads as a golem rather than as a man.
	 *
	 * DRIVEN OFF attackTime, WHICH IS THE ONE THE STATE ACTUALLY HAS.
	 * IronGolemRenderState carries attackTicksRemaining counting 10 down to 0;
	 * EndermanRenderState carries attackTime from ArmedEntityRenderState, which
	 * runs 0 up to 1 across the same swing. So the clock is turned back round —
	 * (1 - attackTime) * 10 — rather than a second animation being invented for it.
	 *
	 * Last of all, after reshape, because the limbs are scaled there and this
	 * writes rotations: the two do not fight, but setupAnim resets both every frame
	 * and whichever runs last is the one that survives.
	 */
	private static void slam(EndermanModel<EndermanRenderState> model, float attackTime) {
		if (attackTime <= 0.0F) {
			return;         // not swinging: the enderman's own angry pose stands
		}
		float swing = -2.0F + 1.5F * net.minecraft.util.Mth.triangleWave(
			(1.0F - attackTime) * 10.0F, 10.0F);
		model.rightArm.xRot = swing;
		model.leftArm.xRot = swing;
		// And square on. A golem does not twist to hit you.
		model.rightArm.zRot = 0.0F;
		model.leftArm.zRot = 0.0F;
	}

	/** Twelve units against the enderman's eight and the villager's ten. */
	private static final float HEAD_TALL = 1.5F;

	private static void reshape(EndermanModel<EndermanRenderState> model) {
		// THE PUBLIC FIELDS, NOT root().getChild("head").
		//
		// HumanoidModel hands these out directly as public final ModelParts, and
		// getChild is a string lookup that throws at RUNTIME on a name that is not
		// there — so a typo or a rename compiles perfectly and then crashes the
		// client the first time one of these walks on screen. The fields are checked
		// by the compiler and cannot be wrong.
		model.head.yScale = HEAD_TALL;

		// AND THE MOUTH KEEPS ITS OWN DEPTH.
		//
		// `hat` is head.getChild("hat") — a CHILD — so the head's scale multiplies
		// the hat's own translation as well as its cube. EndermanModel's angry pose
		// lifts the skull five units (head.y -= 5) and holds the mouth back by the
		// same five (hat.y += 5), which is how an enderman's face appears to open.
		// Under a 1.5 head that five becomes seven and a half, and the recess slides
		// down off the back of the hole it exists to sit behind.
		//
		// Pre-divided, so the five stays five whatever this scale is set to. There
		// is slack either way — the hole is two texel rows at the bottom of the face
		// and the hat cube is as tall as the head — but "there is slack" is how the
		// bug above went unseen for a year.
		model.hat.y /= HEAD_TALL;

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
