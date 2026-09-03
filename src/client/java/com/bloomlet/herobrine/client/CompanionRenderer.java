package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.CompanionEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Addexio, and he is the only human-shaped thing on your side of the world.
 *
 * HE WAS A VILLAGER AND HE WAS CALLED VERA. That version leaned the other way:
 * an ordinary villager in a red coat, on the argument that a companion should be
 * utterly unremarkable up close so that the day something wore her face there was
 * nothing to spot. Red was picked because it was the one hue nothing else in the
 * mod used, and the head was left exactly vanilla, deliberately.
 *
 * It worked and it cost him a face. A villager head is a nose the size of a fist
 * and no expression, and something you are meant to care about — something whose
 * death this mod holds a four-minute vigil over — cannot be a trade menu with
 * legs. It also cost him arms: the villager mesh has one joined `arms` part with
 * no wrist to hang anything off, which is why the old renderer carried his bread
 * across his chest through CrossedArmsItemLayer. There was nowhere else to put it.
 *
 * SO HE IS DRAWN ON THE HUMANOID MESH NOW, off ModelLayers.ZOMBIE, which is the
 * same borrow HerobrineRenderer makes and for the same reason: it is the 1.8
 * player sheet with four-wide arms, so an ordinary skin fits it with nothing
 * remapped. Separate arms, a pale tunic, a strap across the chest and boots — the
 * SILHOUETTE identifies him before any colour does, which is what has to work at
 * distance and in the dark. Everything else in the mod is a robe.
 *
 * HumanoidMobRenderer rather than MobRenderer, because HumanoidModel.setupAnim
 * reads a dozen fields off HumanoidRenderState — attack swing, crouch, sprint,
 * held items — and MobRenderer fills none of them. It also gives him a hand to
 * hold the bread in, which is where a man eating would hold it.
 */
public class CompanionRenderer extends HumanoidMobRenderer<
		CompanionEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

	private static final Identifier TEXTURE =
		HerobrineMod.id("textures/entity/addexio/addexio.png");

	public CompanionRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		// THE ARMOUR. HumanoidMobRenderer's constructor adds the head, the item in
		// hand and the wings — and not the armour layer, which every vanilla
		// humanoid adds for itself (see AbstractZombieRenderer). So he wore enchanted
		// diamond from head to foot and rendered in a tunic. Player armour models on
		// the player-shaped mesh, drawn by the game's own equipment renderer.
		this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
			net.minecraft.client.renderer.entity.ArmorModelSet
				.<HumanoidModel<HumanoidRenderState>>bake(ModelLayers.PLAYER_ARMOR,
					context.getModelSet(), HumanoidModel::new),
			context.getEquipmentRenderer()));
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
