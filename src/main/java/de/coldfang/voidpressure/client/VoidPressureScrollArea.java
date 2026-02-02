package de.coldfang.voidpressure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class VoidPressureScrollArea {

    private int x, y, w, h;

    private int padding = 6;
    private boolean drawBackground = true;

    private List<FormattedCharSequence> wrappedLines = new ArrayList<>();
    private double scroll = 0.0;

    public VoidPressureScrollArea(int x, int y, int w, int h) {
        setBounds(x, y, w, h);
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(10, w);
        this.h = Math.max(10, h);
        clampScroll();
    }

    @SuppressWarnings("unused")
    public void setPadding(int padding) {
        this.padding = Math.max(0, padding);
        clampScroll();
    }

    @SuppressWarnings("unused")
    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    public int getInnerWrapWidth() {
        return Math.max(10, w - padding * 2);
    }

    public void setRawLines(List<String> lines) {
        Font font = Minecraft.getInstance().font;
        int wrapW = getInnerWrapWidth();

        double oldScroll = this.scroll;

        List<FormattedCharSequence> out = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                if (s == null) continue;

                String t = s.trim();
                if (t.isEmpty()) {
                    out.add(FormattedCharSequence.EMPTY);
                    continue;
                }

                out.addAll(font.split(Component.literal(t), wrapW));
            }
        }

        this.wrappedLines = out;
        this.scroll = oldScroll;
        clampScroll();
    }

    @SuppressWarnings("unused")
    public void setRawComponents(List<Component> comps) {
        Font font = Minecraft.getInstance().font;
        int wrapW = getInnerWrapWidth();

        double oldScroll = this.scroll;

        List<FormattedCharSequence> out = new ArrayList<>();
        if (comps != null) {
            for (Component c : comps) {
                if (c == null) continue;

                if (isEffectivelyEmptyLine(c)) {
                    out.add(FormattedCharSequence.EMPTY);
                    continue;
                }

                out.addAll(font.split(c, wrapW));
            }
        }

        this.wrappedLines = out;
        this.scroll = oldScroll;
        clampScroll();
    }

    private static boolean isEffectivelyEmptyLine(Component c) {
        return c.getString().isEmpty() && c.getSiblings().isEmpty();
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) return false;

        scroll -= delta * 12.0;
        clampScroll();
        return true;
    }

    public void render(GuiGraphics gg) {
        if (drawBackground) {
            gg.fill(x, y, x + w, y + h, 0xAA000000);
            gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x66000000);
        }

        gg.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight;

        int startY = y + padding - (int) Math.round(scroll);
        int drawX = x + padding;
        int drawY = startY;

        for (FormattedCharSequence seq : wrappedLines) {
            if (drawY > y + h) break;
            if (drawY + lineH >= y) {
                gg.drawString(font, seq, drawX, drawY, 0xFFFFFFFF, false);
            }
            drawY += lineH;
        }

        gg.disableScissor();

        if (drawBackground) {
            renderScrollbar(gg);
        }
    }

    private void renderScrollbar(GuiGraphics gg) {
        if (wrappedLines == null || wrappedLines.isEmpty()) return;

        Font font = Minecraft.getInstance().font;

        int contentH = wrappedLines.size() * font.lineHeight;
        int viewH = Math.max(0, h - padding * 2);

        if (viewH <= 0) return;
        if (contentH <= viewH) return;

        int barX0 = x + w - 6;
        int barX1 = x + w - 3;
        int barY0 = y + 2;
        int barY1 = y + h - 2;

        gg.fill(barX0, barY0, barX1, barY1, 0x55000000);

        double max = getMaxScroll();
        double t = (max <= 0.0) ? 0.0 : (scroll / max);
        t = Math.max(0.0, Math.min(1.0, t));

        int trackH = barY1 - barY0;
        int thumbH = Math.max(12, (int) Math.round((double) viewH / (double) contentH * trackH));
        int thumbY = (int) Math.round(barY0 + t * (trackH - thumbH));

        gg.fill(barX0, thumbY, barX1, thumbY + thumbH, 0xFFBBBBBB);
    }

    private double getMaxScroll() {
        if (wrappedLines == null || wrappedLines.isEmpty()) return 0.0;

        Font font = Minecraft.getInstance().font;

        int contentH = wrappedLines.size() * font.lineHeight;
        int viewH = Math.max(0, h - padding * 2);

        if (viewH <= 0) return 0.0;
        return Math.max(0.0, contentH - viewH);
    }

    private void clampScroll() {
        double max = getMaxScroll();
        if (scroll < 0.0) scroll = 0.0;
        if (scroll > max) scroll = max;
    }
}
