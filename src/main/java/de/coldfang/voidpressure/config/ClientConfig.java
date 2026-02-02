package de.coldfang.voidpressure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    public enum HudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;
    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;

    // Label shown before the number
    public static final ModConfigSpec.ConfigValue<String> HUD_LABEL;

    // NEW: Trend line under HUD
    public static final ModConfigSpec.BooleanValue HUD_SHOW_TREND;

    // Warning thresholds + pulse
    public static final ModConfigSpec.DoubleValue HUD_WARN_YELLOW_AT;
    public static final ModConfigSpec.DoubleValue HUD_WARN_RED_AT;
    public static final ModConfigSpec.BooleanValue HUD_PULSE_ENABLED;
    public static final ModConfigSpec.DoubleValue HUD_PULSE_STRENGTH;
    public static final ModConfigSpec.DoubleValue HUD_PULSE_SPEED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hud");

        HUD_ENABLED = builder
                .comment("Enable the Void Pressure HUD.")
                .define("enabled", true);

        HUD_LABEL = builder
                .comment(
                        "Label shown in the HUD before the value.",
                        "Example: 'Void Pressure' -> 'Void Pressure: 12.3'",
                        "You can set this to anything you want."
                )
                .define("label", "Void Pressure");

        HUD_SCALE = builder
                .comment("Scale of the Void Pressure HUD text. 1.0 = normal size.")
                .defineInRange("scale", 1.0, 0.5, 4.0);

        HUD_ANCHOR = builder
                .comment("Anchor corner for the Void Pressure HUD.")
                .defineEnum("anchor", HudAnchor.TOP_LEFT);

        HUD_X = builder
                .comment("HUD X offset in pixels from the anchor.")
                .defineInRange("x", 10, 0, 10000);

        HUD_Y = builder
                .comment("HUD Y offset in pixels from the anchor.")
                .defineInRange("y", 10, 0, 10000);

        // Trend display

        HUD_SHOW_TREND = builder
                .comment(
                        "If enabled, I also show the 60s pressure trend under the HUD value.",
                        "This uses the same rolling-window logic as the Overview screen.",
                        "Disabled by default to keep the HUD minimal."
                )
                .define("showTrend", false);

        // Warning colors / pulse behavior

        HUD_WARN_YELLOW_AT = builder
                .comment(
                        "Pressure value at which the HUD starts turning yellow (warning).",
                        "If pressure is below this value, the HUD stays white."
                )
                .defineInRange("warnYellowAt", 50.0, 0.0, 1_000_000.0);

        HUD_WARN_RED_AT = builder
                .comment(
                        "Pressure value at which the HUD becomes red (danger).",
                        "Between warnYellowAt and warnRedAt the color blends from yellow to red.",
                        "Tip: keep warnRedAt > warnYellowAt."
                )
                .defineInRange("warnRedAt", 100.0, 0.0, 1_000_000.0);

        HUD_PULSE_ENABLED = builder
                .comment(
                        "If enabled, the HUD text will pulse once pressure reaches warnRedAt."
                )
                .define("pulseEnabled", false);

        HUD_PULSE_STRENGTH = builder
                .comment(
                        "How strong the pulse effect is (0.0 = no pulse, 1.0 = very strong).",
                        "This affects the text alpha (brightness)."
                )
                .defineInRange("pulseStrength", 0.35, 0.0, 1.0);

        HUD_PULSE_SPEED = builder
                .comment(
                        "Pulse speed. Higher values = faster pulsing."
                )
                .defineInRange("pulseSpeed", 1.8, 0.1, 20.0);

        builder.pop();
        SPEC = builder.build();
    }
}
