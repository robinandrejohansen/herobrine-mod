package com.bloomlet.herobrine.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * THE END. Not the poem — the poem is the game congratulating itself on a
 * dragon it let you win. This is quieter, and true: black, and the lines coming
 * up one at a time, slowly, and then two buttons. Stay in the world he has left,
 * or leave it.
 */
public class PeaceScreen extends Screen {

	private static final String[] LINES = {
		"§fRemoved Herobrine.",
		"",
		"You have beaten Minecraft. For real, this time.",
		"The dragon was a story they let you win.",
		"He was the one they took out of the files —",
		"and you took him out of the world.",
		"",
		"The rain has stopped. The villages are people again.",
		"Nothing is watching from the treeline.",
		"",
		"Thank you for playing to the very end.",
		"§fThere is peace now.",
	};
	private static final String SIGNED = "Herobrine Dimension  ·  © 2026 Robin Johansen · Dimora";

	/** Ticks between one line and the next, and how long each takes to come up. */
	private static final int EACH = 50;
	private static final int FADES_IN = 30;
	private static final int BUTTONS_AFTER = LINES.length * EACH + 40;
	private static final int LINE_HEIGHT = 13;

	private int age;
	private Button stay;
	private Button leave;

	public PeaceScreen() {
		super(Component.literal("Peace"));
	}

	@Override
	protected void init() {
		int y = this.height - 44;
		int mid = this.width / 2;
		this.stay = this.addRenderableWidget(Button.builder(Component.literal("Stay in the world"),
			button -> this.onClose()).bounds(mid - 154, y, 150, 20).build());
		this.leave = this.addRenderableWidget(Button.builder(Component.literal("Leave"),
			button -> this.minecraft.disconnectFromWorld(
				net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE)).bounds(mid + 4, y, 150, 20).build());
		boolean ready = this.age >= BUTTONS_AFTER;
		this.stay.visible = ready;
		this.leave.visible = ready;
	}

	@Override
	public void tick() {
		this.age++;
		if (this.age == BUTTONS_AFTER && this.stay != null) {
			this.stay.visible = true;
			this.leave.visible = true;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return this.age >= BUTTONS_AFTER;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// nothing: the black below is the background
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		float t = this.age + partialTick;
		int black = Math.round(Mth.clamp(t / 40.0F, 0.0F, 1.0F) * 255.0F);
		graphics.fill(0, 0, this.width, this.height, ARGB.color(black, 0, 0, 0));
		int top = this.height / 2 - (LINES.length * LINE_HEIGHT) / 2 - 20;
		for (int i = 0; i < LINES.length; i++) {
			if (LINES[i].isEmpty()) {
				continue;
			}
			float shown = Mth.clamp((t - 30.0F - i * EACH) / FADES_IN, 0.0F, 1.0F);
			if (shown <= 0.0F) {
				continue;
			}
			int alpha = Math.max(4, Math.round(shown * 255.0F));
			graphics.centeredText(this.font, Component.literal(LINES[i]), this.width / 2,
				top + i * LINE_HEIGHT, ARGB.color(alpha, 190, 190, 190));
		}
		float signed = Mth.clamp((t - BUTTONS_AFTER + 20.0F) / FADES_IN, 0.0F, 1.0F);
		if (signed > 0.0F) {
			graphics.centeredText(this.font, Component.literal(SIGNED), this.width / 2, this.height - 66,
				ARGB.color(Math.max(4, Math.round(signed * 160.0F)), 120, 120, 120));
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
}
