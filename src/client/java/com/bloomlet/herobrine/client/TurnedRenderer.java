package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.TurnedEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * A villager, drawn by us.
 *
 * The vanilla villager renderer composites three textures at draw time — the
 * body, the biome's clothes and the profession's apron — and picks them from
 * VillagerData held on the entity. Reaching into that to substitute a skin is
 * exactly what InfectedRenderer exists to avoid; it broke the game twice.
 *
 * So this borrows vanilla's MODEL and none of its renderer. VillagerModel is
 * happy to be driven by anything: its setupAnim reads yaw, pitch, ageInTicks
 * and the walk animation and never once looks at the villager data, so a plain
 * MobRenderer over a VillagerRenderState draws a perfectly ordinary villager
 * with one flat texture and no compositing at all.
 *
 * That texture is a 4x upscale, and it has to be. A vanilla villager eye is two
 * pixels — one white, one green — so the black pupil this needs has nowhere to
 * go at 64x64. See tools/gen_turned.py; the model's UVs are fractions, so the
 * bigger file lands on the same mesh with no other change anywhere.
 *
 * CrossedArmsItemLayer is the axe, and it is the vanilla way villagers hold
 * anything: their model has one joined `arms` part rather than a right hand, so
 * there is no wrist to hang a weapon off. Carried across the chest is what the
 * mesh supports and it happens to be the better image — a man walking at you
 * quickly, holding an axe the way you carry firewood.
 */
public class TurnedRenderer
		extends MobRenderer<TurnedEntity, VillagerRenderState, VillagerModel> {

	private static final Identifier TEXTURE =
		HerobrineMod.id("textures/entity/turned/villager.png");

	private final net.minecraft.client.renderer.item.ItemModelResolver items;

	public TurnedRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
		this.items = context.getItemModelResolver();
		this.addLayer(new CrossedArmsItemLayer<>(this));
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	/**
	 * villagerData is left null on purpose.
	 *
	 * Nothing this renderer reaches ever reads it — the model does not, and the
	 * one thing that does is vanilla's own texture lookup, which is the method
	 * being replaced. Filling it in would mean asking the client for registry
	 * holders to describe a profession he pointedly does not have.
	 */
	@Override
	public void extractRenderState(TurnedEntity entity, VillagerRenderState state,
	                               float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		VillagerRenderState.extractHoldingEntityRenderState(entity, state, this.items);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}
}
