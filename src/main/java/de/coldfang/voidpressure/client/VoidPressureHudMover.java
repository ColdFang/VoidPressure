package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.config.ClientConfig;
import net.minecraft.client.Minecraft;

public final class VoidPressureHudMover {

    private static boolean active = false;
    private static boolean dragging = false;

    // HUD rect in GUI pixels (real on-screen position)
    private static int hudBaseX, hudBaseY, hudW, hudH;

    // drag offset inside the HUD rect
    private static int dragOffX, dragOffY;

    private VoidPressureHudMover() {}

    public static boolean isActive() {
        return active;
    }

    public static boolean isDragging() {
        return dragging;
    }

    public static void start() {
        active = true;
        dragging = false;
    }

    public static void stop(boolean saveToDisk) {
        if (saveToDisk) {
            VoidPressureConfigWriter.setHudPosition(ClientConfig.HUD_X.get(), ClientConfig.HUD_Y.get());
        }
        active = false;
        dragging = false;
    }

    public static void setHudRect(int baseX, int baseY, int w, int h) {
        hudBaseX = baseX;
        hudBaseY = baseY;
        hudW = w;
        hudH = h;
    }

    public static boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;
        if (button != 0) return false; // LMB only

        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);

        if (mx >= hudBaseX && mx < hudBaseX + hudW && my >= hudBaseY && my < hudBaseY + hudH) {
            dragging = true;
            dragOffX = mx - hudBaseX;
            dragOffY = my - hudBaseY;
            return true;
        }
        return false;
    }

    public static boolean onMouseDragged(double mouseX, double mouseY, int button) {
        if (!active) return false;
        if (!dragging) return false;
        if (button != 0) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return false;

        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int newBaseX = mx - dragOffX;
        int newBaseY = my - dragOffY;

        // clamp so it stays fully on screen
        newBaseX = clamp(newBaseX, 0, Math.max(0, sw - hudW));
        newBaseY = clamp(newBaseY, 0, Math.max(0, sh - hudH));

        // Convert base position back into offset values (anchor-aware)
        ClientConfig.HudAnchor anchor = ClientConfig.HUD_ANCHOR.get();

        int newOffsetX = switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> newBaseX;
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, sw - newBaseX - hudW);
        };

        int newOffsetY = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> newBaseY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.max(0, sh - newBaseY - hudH);
        };

        ClientConfig.HUD_X.set(newOffsetX);
        ClientConfig.HUD_Y.set(newOffsetY);

        return true;
    }

    public static boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (!active) return false;
        if (button != 0) return false;

        if (dragging) {
            dragging = false;
            // persist now
            VoidPressureConfigWriter.setHudPosition(ClientConfig.HUD_X.get(), ClientConfig.HUD_Y.get());
            return true;
        }
        return false;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
