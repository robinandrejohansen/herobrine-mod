package com.bloomlet.herobrine.client;

import com.bloomlet.herobrine.HerobrineMod;
import com.bloomlet.herobrine.entity.CompanionEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Vera, drawn as an ordinary villager in a red coat.
 *
 * The same trick TurnedRenderer explains at length: borrow vanilla's villager
 * MODEL and none of its renderer, because the vanilla one composites three
 * textures at draw time off VillagerData the entity does not have. VillagerModel
 * itself never looks at that data, so a plain MobRenderer over a
 * VillagerRenderState draws a perfectly ordinary villager off one flat sheet.
 *
 * AND NOTHING IS DONE TO HER FACE. The Turned gets a swollen nose and a black
 * pupil; the Gaunt gets three blocks of height. She gets neither. Her head is
 * the vanilla villager head, pixel for pixel, and the only thing changed
 * anywhere on the sheet is the colour of her cloth — see tools/gen_vera.py.
 *
 * That is the design and not laziness. She has to be unmistakable at distance
 * and completely ordinary up close, so that on the day something wears her face
 * there is nothing to spot and behaviour is all you have.
 *
 * CrossedArmsItemLayer is the bread. The villager mesh has one joined `arms`
 * part and no wrist to hang anything off, so a held item is carried across the
 * chest — which happens to be exactly right for a woman who has broken off from
 * a fight and is standing behind a rock eating.
 */
public class CompanionRenderer
		extends MobRenderer<CompanionEntity, VillagerRenderState, VillagerModel> {

	private static final Identifier TEXTURE =
		HerobrineMod.id("textures/entity/vera/vera.png");

	private final net.minecraft.client.renderer.item.ItemModelResolver items;

	public CompanionRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
		this.items = context.getItemModelResolver();
		this.addLayer(new CrossedArmsItemLayer<>(this));
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	@Override
	public void extractRenderState(CompanionEntity entity, VillagerRenderState state,
	                               float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		VillagerRenderState.extractHoldingEntityRenderState(entity, state, this.items);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}
}
