package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.config.ClientConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class VoidPressureConfigWriter {

    private static final String COMMON_FILE = "voidpressure-common.toml";

    private static final String KEY_COMMAND = "commandEvents";
    private static final String KEY_EFFECT = "effectEvents";
    private static final String KEY_MOB = "mobChangeEvents";
    private static final String KEY_GAIN_PLAYER_KILL_OVERRIDES = "gainPlayerKillOverrides";

    private static final String CLIENT_FILE = "voidpressure-client.toml";
    private static final String SECTION_HUD = "hud";
    private static final String KEY_SHOW_TREND = "showTrend";
    private static final String KEY_HUD_X = "x";
    private static final String KEY_HUD_Y = "y";
    private static final String KEY_HUD_ANCHOR = "anchor";

    private VoidPressureConfigWriter() {
    }

    // Common config API

    public static void appendCommandEvent(String rawLine) {
        appendToCommonTomlList(KEY_COMMAND, rawLine);
    }

    public static void appendEffectEvent(String rawLine) {
        appendToCommonTomlList(KEY_EFFECT, rawLine);
    }

    public static void appendMobChangeEvent(String rawLine) {
        appendToCommonTomlList(KEY_MOB, rawLine);
    }

    public static void appendGainPlayerKillOverride(String rawLine) {
        appendToCommonTomlList(KEY_GAIN_PLAYER_KILL_OVERRIDES, rawLine);
    }

    public static void deleteAtIndex(String key, int index) {
        if (index < 0) return;

        Path cfg = FMLPaths.CONFIGDIR.get().resolve(COMMON_FILE);
        if (!Files.exists(cfg)) return;

        try {
            String content = Files.readString(cfg, StandardCharsets.UTF_8);
            String updated = deleteFromKeyList(content, key, index);
            if (!updated.equals(content)) {
                Files.writeString(cfg, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    // Client config API

    public static void setHudShowTrend(boolean enabled) {
        setClientHudShowTrend(enabled);
    }

    public static void setHudPosition(int x, int y) {
        setClientHudInt(KEY_HUD_X, x);
        setClientHudInt(KEY_HUD_Y, y);
    }

    public static void setHudAnchor(ClientConfig.HudAnchor anchor) {
        if (anchor == null) return;
        setClientHudString(KEY_HUD_ANCHOR, anchor.name());
    }

    // Common file helpers

    private static void appendToCommonTomlList(String key, String rawLine) {
        Path cfg = FMLPaths.CONFIGDIR.get().resolve(COMMON_FILE);
        if (!Files.exists(cfg)) {
            return;
        }

        String escaped = escapeTomlString(rawLine);

        try {
            String content = Files.readString(cfg, StandardCharsets.UTF_8);
            String updated = insertIntoKeyList(content, key, escaped);

            if (!updated.equals(content)) {
                Files.writeString(cfg, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static String escapeTomlString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String insertIntoKeyList(String content, String key, String escapedValue) {
        String needle = key + " =";
        int idx = content.indexOf(needle);
        if (idx < 0) {
            String nl = content.endsWith("\n") ? "" : "\n";
            return content + nl + key + " = [\"" + escapedValue + "\"]\n";
        }

        int open = content.indexOf('[', idx);
        if (open < 0) return content;

        int close = findMatchingBracket(content, open);
        if (close < 0) return content;

        String inside = content.substring(open + 1, close).trim();

        if (inside.isEmpty()) {
            return content.substring(0, open + 1)
                    + "\"" + escapedValue + "\""
                    + content.substring(close);
        }

        boolean multiline = content.substring(open, close).contains("\n");

        if (!multiline) {
            String insert = ", " + "\"" + escapedValue + "\"";
            return content.substring(0, close) + insert + content.substring(close);
        } else {
            String beforeClose = content.substring(0, close);
            beforeClose = ensureLastEntryHasComma(beforeClose, open);

            String indent = "  ";
            String insert = "\n" + indent + "\"" + escapedValue + "\"";
            return beforeClose + insert + content.substring(close);
        }
    }

    private static String deleteFromKeyList(String content, String key, int index) {
        String needle = key + " =";
        int idx = content.indexOf(needle);
        if (idx < 0) return content;

        int open = content.indexOf('[', idx);
        if (open < 0) return content;

        int close = findMatchingBracket(content, open);
        if (close < 0) return content;

        String listBody = content.substring(open + 1, close);

        List<String> values = parseTomlBasicStringList(listBody);
        if (index >= values.size()) return content;

        values.remove(index);

        boolean multiline = listBody.contains("\n");
        String rebuilt = renderTomlStringList(values, multiline);

        return content.substring(0, open + 1) + rebuilt + content.substring(close);
    }

    private static List<String> parseTomlBasicStringList(String body) {
        List<String> out = new ArrayList<>();

        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);

            if (c != '"') {
                i++;
                continue;
            }

            i++;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;

            while (i < body.length()) {
                char ch = body.charAt(i);

                if (escaped) {
                    sb.append(ch);
                    escaped = false;
                    i++;
                    continue;
                }

                if (ch == '\\') {
                    escaped = true;
                    i++;
                    continue;
                }

                if (ch == '"') {
                    out.add(sb.toString());
                    i++;
                    break;
                }

                sb.append(ch);
                i++;
            }
        }

        return out;
    }

    private static String renderTomlStringList(List<String> values, boolean multiline) {
        if (values.isEmpty()) {
            return "";
        }

        if (!multiline) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String v : values) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("\"").append(escapeTomlString(v)).append("\"");
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        String indent = "  ";
        for (int i = 0; i < values.size(); i++) {
            sb.append("\n").append(indent).append("\"").append(escapeTomlString(values.get(i))).append("\"");
            if (i != values.size() - 1) sb.append(",");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static int findMatchingBracket(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String ensureLastEntryHasComma(String beforeClose, int openBracketIdx) {
        int i = beforeClose.length() - 1;
        while (i > openBracketIdx) {
            char c = beforeClose.charAt(i);
            if (!Character.isWhitespace(c)) break;
            i--;
        }
        if (i <= openBracketIdx) return beforeClose;

        char last = beforeClose.charAt(i);
        if (last == ',' || last == '[') return beforeClose;

        return beforeClose.substring(0, i + 1) + "," + beforeClose.substring(i + 1);
    }

    // Client file helpers

    private static void setClientHudShowTrend(boolean enabled) {
        Path cfg = FMLPaths.CONFIGDIR.get().resolve(CLIENT_FILE);
        String boolStr = enabled ? "true" : "false";

        try {
            String content = Files.exists(cfg)
                    ? Files.readString(cfg, StandardCharsets.UTF_8)
                    : "";

            String updated = upsertTomlValue(content, SECTION_HUD, KEY_SHOW_TREND, boolStr);

            if (!updated.equals(content)) {
                Files.writeString(cfg, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static void setClientHudInt(String key, int value) {
        Path cfg = FMLPaths.CONFIGDIR.get().resolve(CLIENT_FILE);

        try {
            String content = Files.exists(cfg)
                    ? Files.readString(cfg, StandardCharsets.UTF_8)
                    : "";

            String updated = upsertTomlValue(content, SECTION_HUD, key, Integer.toString(value));

            if (!updated.equals(content)) {
                Files.writeString(cfg, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static void setClientHudString(String key, String value) {
        Path cfg = FMLPaths.CONFIGDIR.get().resolve(CLIENT_FILE);

        // TOML basic string literal with escaping
        String escaped = (value == null) ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        String literal = "\"" + escaped + "\"";

        try {
            String content = Files.exists(cfg)
                    ? Files.readString(cfg, StandardCharsets.UTF_8)
                    : "";

            String updated = upsertTomlValue(content, SECTION_HUD, key, literal);

            if (!updated.equals(content)) {
                Files.writeString(cfg, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static String upsertTomlValue(String content, String section, String key, String valueLiteral) {
        String normalized = content == null ? "" : content;

        String sectionHeader = "[" + section + "]";
        int secIdx = indexOfLine(normalized, sectionHeader);
        if (secIdx < 0) {
            String nl = normalized.endsWith("\n") || normalized.isEmpty() ? "" : "\n";
            return normalized + nl + sectionHeader + "\n" + key + " = " + valueLiteral + "\n";
        }

        int secLineEnd = normalized.indexOf('\n', secIdx);
        if (secLineEnd < 0) secLineEnd = normalized.length();

        int bodyStart = (secLineEnd < normalized.length()) ? (secLineEnd + 1) : secLineEnd;
        int bodyEnd = findNextSectionHeader(normalized, bodyStart);
        if (bodyEnd < 0) bodyEnd = normalized.length();

        int keyLineStart = findKeyLineStart(normalized, bodyStart, bodyEnd, key);
        if (keyLineStart >= 0) {
            int keyLineEnd = normalized.indexOf('\n', keyLineStart);
            if (keyLineEnd < 0) keyLineEnd = normalized.length();

            String before = normalized.substring(0, keyLineStart);
            String after = (keyLineEnd < normalized.length())
                    ? normalized.substring(keyLineEnd + 1)
                    : "";

            return before + key + " = " + valueLiteral + "\n" + after;

        }

        String before = normalized.substring(0, bodyStart);
        String after = normalized.substring(bodyStart);
        return before + key + " = " + valueLiteral + "\n" + after;
    }

    private static int indexOfLine(String content, String line) {
        int idx = 0;
        while (idx <= content.length()) {
            int next = content.indexOf('\n', idx);
            if (next < 0) next = content.length();
            String cur = content.substring(idx, next).trim();
            if (cur.equals(line)) return idx;
            if (next == content.length()) break;
            idx = next + 1;
        }
        return -1;
    }

    private static int findNextSectionHeader(String content, int fromIdx) {
        int idx = fromIdx;
        while (idx < content.length()) {
            int lineEnd = content.indexOf('\n', idx);
            if (lineEnd < 0) lineEnd = content.length();
            String line = content.substring(idx, lineEnd).trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                return idx;
            }
            if (lineEnd == content.length()) break;
            idx = lineEnd + 1;
        }
        return -1;
    }

    private static int findKeyLineStart(String content, int start, int end, String key) {
        int idx = start;
        while (idx < end) {
            int lineEnd = content.indexOf('\n', idx);
            if (lineEnd < 0 || lineEnd > end) lineEnd = end;

            String line = content.substring(idx, lineEnd).trim();

            if (!line.isEmpty() && !line.startsWith("#")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String left = line.substring(0, eq).trim();
                    if (left.equals(key)) return idx;
                }
            }

            if (lineEnd == end) break;
            idx = lineEnd + 1;
        }
        return -1;
    }
}
