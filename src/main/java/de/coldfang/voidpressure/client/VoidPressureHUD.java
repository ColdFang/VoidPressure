package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.config.ClientConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Locale;

@EventBusSubscriber(modid = VoidPressure.MODID, value = Dist.CLIENT)
@SuppressWarnings("unused")
public class VoidPressureHUD {

    private static final int TREND_THRESHOLD_MILLI = 1;
    private static final int LINE_GAP_PX = 2;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!ClientConfig.HUD_ENABLED.get()) return;

        OverviewSnapshot snap = getSnapshotClientSafe(mc);

        double scale = ClientConfig.HUD_SCALE.get();
        int offsetX = ClientConfig.HUD_X.get();
        int offsetY = ClientConfig.HUD_Y.get();
        ClientConfig.HudAnchor anchor = ClientConfig.HUD_ANCHOR.get();

        String label = ClientConfig.HUD_LABEL.get();
        label = (label == null) ? "" : label.trim();
        if (label.isBlank()) label = "Void Pressure";

        String mainText = String.format(Locale.ROOT, "%s: %.1f", label, snap.pressure);
        TrendLine trend = ClientConfig.HUD_SHOW_TREND.get() ? buildTrendLine(snap.net60) : null;

        int mainW = mc.font.width(mainText);
        int mainH = mc.font.lineHeight;

        int trendW = (trend != null) ? mc.font.width(trend.text) : 0;
        int trendH = (trend != null) ? mc.font.lineHeight : 0;

        int totalW = Math.max(mainW, trendW);
        int totalH = mainH + ((trend != null) ? (LINE_GAP_PX + trendH) : 0);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int scaledW = (int) Math.ceil(totalW * scale);
        int scaledH = (int) Math.ceil(totalH * scale);

        // base position in GUI pixels (NOT scaled pose space)
        int baseX = computeBaseX(anchor, screenW, offsetX, scaledW);
        int baseY = computeBaseY(anchor, screenH, offsetY, scaledH);

        // If move screen is open: tell mover the actual HUD rect (base coords)
        boolean moveScreen = (mc.screen instanceof VoidPressureHudMoveScreen);
        if (moveScreen) {
            VoidPressureHudMover.setHudRect(baseX, baseY, scaledW, scaledH);
        }

        var gg = event.getGuiGraphics();

        gg.pose().pushPose();
        gg.pose().scale((float) scale, (float) scale, 1.0f);

        // convert base (GUI px) into scaled pose space
        int sx = (int) Math.round(baseX / scale);
        int sy = (int) Math.round(baseY / scale);

        int mainColor = computeHudColorWithPulse(snap.pressure);
        gg.drawString(mc.font, Component.literal(mainText), sx, sy, mainColor, false);

        if (trend != null) {
            int alpha = (mainColor >>> 24) & 0xFF;
            int trendColor = (alpha << 24) | (trend.rgb & 0x00FFFFFF);

            gg.drawString(
                    mc.font,
                    Component.literal(trend.text),
                    sx,
                    sy + mainH + LINE_GAP_PX,
                    trendColor,
                    false
            );
        }

        gg.pose().popPose();

        if (moveScreen) {
            renderMoveOverlay(gg, mc, baseX, baseY, scaledW, scaledH);
        }
    }

    private static int computeBaseX(ClientConfig.HudAnchor anchor, int screenW, int offsetX, int scaledW) {
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - offsetX - scaledW;
        };
    }

    private static int computeBaseY(ClientConfig.HudAnchor anchor, int screenH, int offsetY, int scaledH) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - offsetY - scaledH;
        };
    }

    private static void renderMoveOverlay(net.minecraft.client.gui.GuiGraphics gg, Minecraft mc, int x, int y, int w, int h) {
        int x0 = x - 3;
        int y0 = y - 3;
        int x1 = x + w + 3;
        int y1 = y + h + 3;

        gg.fill(x0, y0, x1, y0 + 1, 0xAAFFFFFF);
        gg.fill(x0, y1 - 1, x1, y1, 0xAAFFFFFF);
        gg.fill(x0, y0, x0 + 1, y1, 0xAAFFFFFF);
        gg.fill(x1 - 1, y0, x1, y1, 0xAAFFFFFF);

        String hint = VoidPressureHudMover.isDragging()
                ? "Release LMB to save, ESC/RMB to cancel"
                : "Drag with LMB, ESC/RMB to cancel";

        int ty = y0 - mc.font.lineHeight - 2;
        if (ty < 2) ty = y1 + 2;

        gg.drawString(mc.font, Component.literal(hint), x0, ty, 0xFFFFFFFF, true);
    }

    private record TrendLine(String text, int rgb) {}

    private static TrendLine buildTrendLine(double net60) {
        long netMilli = Math.round(net60 * 1000.0);

        if (netMilli > TREND_THRESHOLD_MILLI) {
            return new TrendLine("↑ Increasing", 0x0055FF55);
        }
        if (netMilli < -TREND_THRESHOLD_MILLI) {
            return new TrendLine("↓ Decreasing", 0x00FF5555);
        }
        return new TrendLine("→ Stable", 0x00AAAAAA);
    }

    private static int computeHudColorWithPulse(double pressure) {
        int rgb = computeHudRgb(pressure);

        int a = 0xFF;

        if (ClientConfig.HUD_PULSE_ENABLED.get()) {
            double redAt = ClientConfig.HUD_WARN_RED_AT.get();
            if (pressure >= redAt) {
                double strength = clamp01(ClientConfig.HUD_PULSE_STRENGTH.get());
                double speed = Math.max(0.1, ClientConfig.HUD_PULSE_SPEED.get());

                double t = System.nanoTime() * 1.0e-9;
                double s = 0.5 + 0.5 * Math.sin(t * speed);

                double alphaMul = (1.0 - strength) + (strength * s);

                a = (int) Math.round(255.0 * alphaMul);
                a = Math.max(0, Math.min(255, a));
            }
        }

        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int computeHudRgb(double pressure) {
        double yellowAt = ClientConfig.HUD_WARN_YELLOW_AT.get();
        double redAt = ClientConfig.HUD_WARN_RED_AT.get();

        int white = 0x00FFFFFF;
        int yellow = 0x00FFFF55;
        int red = 0x00FF5555;

        if (redAt <= yellowAt) return white;
        if (pressure < yellowAt) return white;

        if (pressure < redAt) {
            float t = (float) ((pressure - yellowAt) / (redAt - yellowAt));
            return lerpRgb(yellow, red, t);
        }

        return red;
    }

    private static int lerpRgb(int aRgb, int bRgb, float t) {
        if (t <= 0f) return aRgb;
        if (t >= 1f) return bRgb;

        int ar = (aRgb >> 16) & 0xFF;
        int ag = (aRgb >> 8) & 0xFF;
        int ab = aRgb & 0xFF;

        int br = (bRgb >> 16) & 0xFF;
        int bg = (bRgb >> 8) & 0xFF;
        int bb = bRgb & 0xFF;

        int rr = ar + Math.round((br - ar) * t);
        int rg = ag + Math.round((bg - ag) * t);
        int rb = ab + Math.round((bb - ab) * t);

        return (rr << 16) | (rg << 8) | rb;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private record OverviewSnapshot(double pressure, double net60) {}

    private static OverviewSnapshot getSnapshotClientSafe(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return new OverviewSnapshot(0.0, 0.0);

        ServerLevel level = server.overworld();
        if (level == null) return new OverviewSnapshot(0.0, 0.0);

        VoidPressureSavedData data = VoidPressureSavedData.get(level);
        data.tickRollingWindow(level);

        return new OverviewSnapshot(
                data.getPressure(),
                data.getNetLast60s()
        );
    }
}
