package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class VoidPressurePages {

    private VoidPressurePages() {
    }

    @SuppressWarnings("unused")
    public static void buildOverviewComponents(List<Component> out) {
        out.add(Component.literal("Overview"));
        out.add(Component.empty());

        ServerContext ctx = getServerContext();
        if (ctx == null) {
            out.add(Component.literal("Current Pressure: (no server data)"));
            return;
        }

        double current = ctx.data.getPressure();
        double gain60 = ctx.data.getGainLast60s();
        double loss60 = ctx.data.getLossLast60s();
        double net60 = ctx.data.getNetLast60s();

        out.add(Component.literal(String.format(Locale.ROOT, "Current Pressure: %.1f", current)));
        out.add(Component.empty());

        out.add(Component.literal(String.format(Locale.ROOT, "Gained (last 60s): +%.3f", gain60)));
        out.add(Component.literal(String.format(Locale.ROOT, "Lost   (last 60s): -%.3f", loss60)));
        out.add(Component.empty());

        out.add(buildTrendComponent(net60));
    }

    @SuppressWarnings("unused")
    public static void buildOverview(List<String> out) {
        out.add("Overview");
        out.add("");

        ServerContext ctx = getServerContext();
        if (ctx == null) {
            out.add("Current Pressure: (no server data)");
            return;
        }

        out.add(String.format(Locale.ROOT, "Current Pressure: %.1f", ctx.data.getPressure()));
        out.add("");
        out.add(String.format(Locale.ROOT, "Gained (last 60s): +%.3f", ctx.data.getGainLast60s()));
        out.add(String.format(Locale.ROOT, "Lost   (last 60s): -%.3f", ctx.data.getLossLast60s()));
        out.add("");
        out.add(String.format(Locale.ROOT, "Net    (last 60s): %.3f", ctx.data.getNetLast60s()));
    }

    public static void buildTriggered(List<String> out) {
        out.add("Events Triggered");
        out.add("");

        ServerContext ctx = getServerContext();
        if (ctx == null) {
            out.add("No server data (multiplayer not synced yet).");
            return;
        }

        var hist = ctx.data.getHistoryNewestFirst();
        if (hist == null || hist.isEmpty()) {
            out.add("No events triggered yet.");
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);

        for (VoidPressureSavedData.HistoryEntry e : hist) {
            String time = fmt.format(new Date(e.timeMs()));
            String p = String.format(Locale.ROOT, "%.1f", e.pressureMilli() / 1000.0);
            out.add(time + " | " + p + " | " + safe(e.summary()));
            out.add("");
        }

        removeTrailingBlankLine(out);
    }

    public static void buildConfigList(List<String> out, String header, List<? extends String> rawLines) {
        if (header != null && !header.isBlank()) {
            out.add(header);
            out.add("");
        }

        if (rawLines == null || rawLines.isEmpty()) {
            out.add("No entries in config.");
            return;
        }

        for (int idx = 0; idx < rawLines.size(); idx++) {
            String raw = rawLines.get(idx);
            if (raw == null || raw.isBlank()) continue;

            out.add(idx + ": " + summarizeConfigLine(raw));
            out.add("");
        }

        removeTrailingBlankLine(out);
    }

    public static void buildGainPlayerKillOverrides(List<String> out, List<? extends String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            out.add("No overrides set.");
            return;
        }

        for (int idx = 0; idx < rawLines.size(); idx++) {
            String raw = rawLines.get(idx);
            if (raw == null || raw.isBlank()) continue;

            out.add(idx + ": " + summarizeOverrideLine(raw));
            out.add("");
        }

        removeTrailingBlankLine(out);
    }

    private static Component buildTrendComponent(double net60) {
        if (net60 > 0.0005) {
            return Component.literal(
                    String.format(Locale.ROOT, "Trend: Increasing (+%.3f / 60s)", net60)
            ).withStyle(ChatFormatting.GREEN);
        }

        if (net60 < -0.0005) {
            return Component.literal(
                    String.format(Locale.ROOT, "Trend: Decreasing (%.3f / 60s)", net60)
            ).withStyle(ChatFormatting.RED);
        }

        return Component.literal("Trend: Stable").withStyle(ChatFormatting.GRAY);
    }

    private static String summarizeOverrideLine(String raw) {
        String s = raw.trim();
        int eq = s.indexOf('=');
        if (eq < 0) return s;

        String left = s.substring(0, eq).trim();
        String right = s.substring(eq + 1).trim();

        if (left.isEmpty()) left = "?";
        if (right.isEmpty()) right = "?";

        return left + " = " + right;
    }

    private static String summarizeConfigLine(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 3) return raw.trim();

        String pressure = parts[0].trim();
        String value = parts[2].trim();

        if (pressure.isEmpty()) pressure = "?";
        if (value.isEmpty()) value = "(empty)";

        return pressure + " | " + value;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void removeTrailingBlankLine(List<String> out) {
        if (out.isEmpty()) return;
        if (!out.getLast().isBlank()) return;
        out.removeLast();
    }

    private record ServerContext(ServerLevel level, VoidPressureSavedData data) {
    }

    private static ServerContext getServerContext() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return null;

        ServerLevel level = server.overworld();
        return new ServerContext(level, VoidPressureSavedData.get(level));
    }
}
