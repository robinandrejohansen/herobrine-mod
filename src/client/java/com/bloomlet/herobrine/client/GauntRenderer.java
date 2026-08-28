package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.GauntEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The tall one, drawn by stretching a villager.
 *
 * SAME MESH, DIFFERENT PROPORTIONS, and that is the whole trick. A purpose-built
 * model would be a week of work for a creature whose entire effect is silhouette
 * — and worse, it would break the thing that makes this frightening, which is
 * that it is recognisably one of THEM. Not a monster that came from somewhere.
 * The shape of the man who sold you carrots, pulled.
 *
 * The stretch is a PoseStack scale rather than per-part scaling, unlike the nose
 * work in TurnedRenderer, because it has to apply to every part at once
 * including the ones VillagerModel does not expose — it only publishes getHead —
 * and because a uniform transform keeps the walk animation coherent. Scaling the
 * legs individually and leaving the hips alone gives you a man whose feet come
 * off the floor on every stride.
 *
 * The texture stretches with it, and that is wanted here: the same face pulled
 * long. See tools/gen_gaunt.py, which draws a blank pale face knowing it will
 * arrive on screen a third again as tall as it was painted.
 */
public class GauntRenderer
		extends MobRenderer<GauntEntity, VillagerRenderState, VillagerModel> {

	private static final Identifier TEXTURE =
		HerobrineMod.id("textures/entity/gaunt/gaunt.png");
	private static final Identifier EYES =
		HerobrineMod.id("textures/entity/gaunt/gaunt_eyes.png");

	/**
	 * NARROW AND LONG, and the two numbers are not independent.
	 *
	 * LONG is derived: the villager mesh stands about 1.95 blocks, and GauntEntity
	 * declares a 3.3-block hitbox, so 1.7 is what makes the drawing finish where
	 * the box does. Picking it by eye is how you ship a mob whose head pokes out
	 * of its own hurtbox.
	 *
	 * THIN is chosen: 0.62 takes the body from half a block wide to a third. Far
	 * enough to be unmistakable at range, and stopping well short of the point
	 * where the arms detach from the shoulders visually — past about 0.5 the mesh
	 * starts to look broken rather than thin, and broken reads as a bug.
	 */
	private static final float THIN = 0.62F;
	private static final float LONG = 1.7F;

	/**
	 * And the arms are longer again than the rest of him.
	 *
	 * LONG stretches everything equally, which gives a tall villager rather than a
	 * wrong one — proportion is what the eye actually reads, and a figure scaled
	 * uniformly reads as the same person standing closer. The arms have to break
	 * that ratio to break the read.
	 *
	 * The vanilla arm box is eight units on a twelve-unit body. At 2.2 it is
	 * seventeen and a half, which lands the hands just above the knee — far enough
	 * down to be unmistakable in silhouette, and stopping there because past the
	 * knee it becomes an ape and this is meant to stay a man who is wrong.
	 *
	 * It also lifts the top of the arm about an eighth of a block above the
	 * shoulder line, since the part scales around a pivot in the middle of it.
	 * Left alone rather than corrected: a figure with its shoulders slightly up
	 * around its neck is exactly the posture wanted, and it came free.
	 */
	private static final float REACH = 2.2F;

	private final net.minecraft.client.renderer.item.ItemModelResolver items;

	public GauntRenderer(EntityRendererProvider.Context context) {
		// A tighter shadow than the Turned's. The body is a third of the width and
		// a villager-sized puddle under it would be the one thing on screen
		// insisting the creature is a normal shape.
		super(context, hanging(), 0.35F);
		this.items = context.getItemModelResolver();
		this.addLayer(new Glow(this));
		shrinkTheNose(this.getModel());
		// Lengthened here rather than in the mesh, deliberately. A taller BOX
		// samples a taller rectangle of texture and would start dragging in
		// whatever sits next to the arm on the sheet; scaling the baked part
		// stretches the same texels over more space. Exactly the argument
		// TurnedRenderer makes for the nose.
		this.getModel().root().getChild("arms").yScale = REACH;
	}

	/**
	 * ARMS THAT HANG, WHICH THE VILLAGER MESH WILL NOT DO ON ITS OWN.
	 *
	 * A villager's `arms` is one part holding THREE cubes: a left arm at x -8..-4,
	 * a right arm at x 4..8, and — the problem — an 8x4x4 slab across x -4..4 that
	 * is the two forearms crossed over the belly. The part is posed at -0.75
	 * radians, which is what tips the whole assembly up into the fold.
	 *
	 * So dropping the rotation to zero is not enough. It gives you hanging arms
	 * with a plank welded between them at waist height. The cross-piece has to go,
	 * and a ModelPart has no way to hide one cube out of three — visibility is per
	 * part.
	 *
	 * Which is why this rebuilds the mesh. createBodyModel gives the villager's
	 * own definition, `arms` is a direct child of the root, and addOrReplaceChild
	 * swaps it for the same two arm cubes at the same UVs with the slab left out
	 * and the pose flat. Everything else about the villager is untouched, and
	 * VillagerModel's constructor and setupAnim never notice — setupAnim does not
	 * read `arms` at all, which is also why the flat pose survives every frame
	 * without being reasserted.
	 *
	 * Baked straight off the LayerDefinition rather than registered as a model
	 * layer. There is one consumer and it is this constructor; a registry entry
	 * would be a second name for a thing nothing else can ask for.
	 */
	private static VillagerModel hanging() {
		MeshDefinition mesh = VillagerModel.createBodyModel();
		mesh.getRoot().addOrReplaceChild("arms",
			CubeListBuilder.create()
				.texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F),
			PartPose.offset(0.0F, 3.0F, -1.0F));
		return new VillagerModel(LayerDefinition.create(mesh, 64, 64).bakeRoot());
	}

	/**
	 * ALMOST NO NOSE, and it is the exact inverse of what TurnedRenderer does.
	 *
	 * That one swells it, because the villager nose is the most recognisable
	 * feature in the game and making it heavier says "him, wrong" instantly. This
	 * one needs the face to be a FLAT PLANE with two eyes in it — the texture is
	 * drawn blank on the assumption that nothing is sticking out of it, and a
	 * full-size villager nose on a bleached face would be the one piece of
	 * geometry catching a highlight and ruining the whole read.
	 *
	 * Scaled to a fifth rather than removed. The part could be hidden outright,
	 * but a nose flattened against the face still shades the plane very slightly
	 * where it meets it, and that faint seam is the difference between a face with
	 * no features and a face somebody forgot to finish.
	 */
	private static void shrinkTheNose(VillagerModel model) {
		net.minecraft.client.model.geom.ModelPart nose = model.getHead().getChild("nose");
		nose.xScale = 0.2F;
		nose.yScale = 0.2F;
		nose.zScale = 0.2F;
	}

	@Override
	protected void scale(VillagerRenderState state, PoseStack pose) {
		pose.scale(THIN, LONG, THIN);
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	/**
	 * villagerData stays null, for the reason TurnedRenderer spells out: nothing
	 * this renderer touches reads it, and the one thing that would is vanilla's
	 * texture lookup, which is the method being replaced.
	 */
	@Override
	public void extractRenderState(GauntEntity entity, VillagerRenderState state,
	                               float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		VillagerRenderState.extractHoldingEntityRenderState(entity, state, this.items);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}

	/**
	 * The eyes, lit, and this is the reason the creature works at all.
	 *
	 * Everything else about it is black — the curve in gen_gaunt.py runs over the
	 * hood and the robe as well as the boots, which in an unlit forest at night
	 * means the body is simply not there. What a player at forty blocks actually
	 * receives is two small bright marks, at a height nothing should be, holding
	 * still.
	 *
	 * The same pass spiders and endermen use. It ignores world lighting entirely,
	 * so the marks are exactly as bright under a canopy at midnight as they are at
	 * noon — which is the correct behaviour and also, conveniently, the frightening
	 * one: putting a torch down does not help.
	 */
	private static final class Glow extends EyesLayer<VillagerRenderState, VillagerModel> {
		private static final RenderType LIT = RenderTypes.eyes(EYES);

		private Glow(GauntRenderer parent) {
			super(parent);
		}

		@Override
		public RenderType renderType() {
			return LIT;
		}
	}
}
