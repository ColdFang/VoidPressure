package de.coldfang.voidpressure.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VoidPressureHudMoveScreen extends Screen {

    public VoidPressureHudMoveScreen() {
        super(Component.literal("Move HUD"));
    }

    @Override
    protected void init() {
        // activate mover when screen opens
        VoidPressureHudMover.start();
    }

    @Override
    public void onClose() {
        // cancel by default when closing via other means
        VoidPressureHudMover.stop(false);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true; // gives you cursor + pauses game
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // no full overlay, keep game visible; HUD overlay is drawn by VoidPressureHUD
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // RMB cancels
        if (button == 1) {
            VoidPressureHudMover.stop(false);
            onClose();
            return true;
        }

        if (VoidPressureHudMover.onMouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (VoidPressureHudMover.onMouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (VoidPressureHudMover.onMouseReleased(mouseX, mouseY, button)) {
            // Mover saved on release; close after save
            onClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC cancels
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            VoidPressureHudMover.stop(false);
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
