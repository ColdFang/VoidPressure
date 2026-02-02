package de.coldfang.voidpressure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class CommonConfig {

    public static final ModConfigSpec SPEC;

    // Core Balance

    public static final ModConfigSpec.DoubleValue GAIN_PLAYER_KILL;
    public static final ModConfigSpec.DoubleValue GAIN_PASSIVE_KILL;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> GAIN_PLAYER_KILL_OVERRIDES;

    public static final ModConfigSpec.DoubleValue MAX_PRESSURE_PER_MINUTE;
    public static final ModConfigSpec.DoubleValue PRESSURE_DECAY_PER_MINUTE;

    // Event Lists

    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMAND_EVENTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EFFECT_EVENTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_CHANGE_EVENTS;

    // Pressure Altar

    public static final ModConfigSpec.ConfigValue<String> ALTAR_START_ITEM;
    public static final ModConfigSpec.BooleanValue ALTAR_COSTS_XP;
    public static final ModConfigSpec.IntValue ALTAR_XP_LEVELS_PER_RITUAL;
    public static final ModConfigSpec.IntValue ALTAR_COOLDOWN_SECONDS;

    /** Format: "itemId|pressure" e.g. "minecraft:diamond|5.0" */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALTAR_OFFERINGS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("pressure");

        // Balance

        GAIN_PLAYER_KILL = builder
                .comment("Void Pressure gained when a mob is killed by a player (active kill).")
                .defineInRange("gainPlayerKill", 0.1, 0.0, 100000.0);

        GAIN_PASSIVE_KILL = builder
                .comment("Void Pressure gained when a mob dies without a player as killer (passive kill).")
                .defineInRange("gainPassiveKill", 0.05, 0.0, 100000.0);

        GAIN_PLAYER_KILL_OVERRIDES = builder
                .comment(
                        "Overwrite gainPlayerKill for specific mobs / entity types.",
                        "",
                        "Format per entry: namespace:entity=value",
                        "Examples:",
                        "  minecraft:iron_golem=2.0",
                        "  minecraft:zombie=0.2",
                        "",
                        "If multiple entries exist for the same mob, the last one wins."
                )
                .defineListAllowEmpty(
                        "gainPlayerKillOverrides",
                        List.of(
                                "minecraft:iron_golem=2.0"
                        ),
                        () -> "",
                        o -> o instanceof String
                );

        MAX_PRESSURE_PER_MINUTE = builder
                .comment(
                        "Maximum Void Pressure that can be gained per minute (rolling 60s window).",
                        "0.0 = disabled."
                )
                .defineInRange("maxPressurePerMinute", 0.0, 0.0, 100000.0);

        PRESSURE_DECAY_PER_MINUTE = builder
                .comment(
                        "Void Pressure lost per minute.",
                        "0.0 = disabled."
                )
                .defineInRange("pressureDecayPerMinute", 0.0, 0.0, 100000.0);

        // Altar

        builder.push("altar");

        ALTAR_START_ITEM = builder
                .comment(
                        "Item ID used to start the altar ritual (right-click).",
                        "Default: minecraft:flint_and_steel"
                )
                .define("startItem", "minecraft:flint_and_steel");

        ALTAR_COSTS_XP = builder
                .comment("If true, the ritual costs XP levels.")
                .define("costsXp", true);

        ALTAR_XP_LEVELS_PER_RITUAL = builder
                .comment("How many XP LEVELS are consumed per ritual (if costsXp=true).")
                .defineInRange("xpLevelsPerRitual", 1, 0, 1000);

        ALTAR_COOLDOWN_SECONDS = builder
                .comment("Cooldown after a successful ritual, in seconds. 0 = disabled.")
                .defineInRange("cooldownSeconds", 10, 0, 3600);

        ALTAR_OFFERINGS = builder
                .comment(
                        "Offerings list format: itemId|pressure",
                        "Example: minecraft:diamond|5.0",
                        "pressure is how much Void Pressure is REDUCED when this item is sacrificed."
                )
                .defineListAllowEmpty(
                        "offerings",
                        List.of(
                                "minecraft:diamond|5.0"
                        ),
                        () -> "",
                        o -> o instanceof String
                );

        builder.pop(); // altar

        // Events

        builder.push("events");

        // COMMAND Events
        COMMAND_EVENTS = builder
                .comment(
                        "COMMAND events format:",
                        "pressure|isAbsolute|command|announce|soundId|isConsistent",
                        "",
                        "Examples:",
                        "5.0|true|say EXACT 5 reached!|EXACT 5 reached!|minecraft:entity.player.levelup|false",
                        "5.0|false|say Every 5 pressure!||minecraft:entity.evoker.prepare_summon|false",
                        "7.0|true|say One-time ever.|One-time ever.|minecraft:entity.player.levelup|true"
                )
                .defineListAllowEmpty(
                        "commandEvents",
                        List.of(
                                "1.0|true|tellraw @a {\"text\":\"[VoidPressure] Pressure is rising. Entity deaths increase it.\",\"color\":\"light_purple\"}|Pressure is rising.|minecraft:block.amethyst_block.chime|false",
                                "20.0|true|execute as @a at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.6 0.8 0.6 0.03 80 force @s|A void pulse ripples through the air.|minecraft:ambient.cave|false",
                                "50.0|true|title @a title {\"text\":\"VOID PRESSURE: 50\",\"color\":\"dark_purple\",\"bold\":true}|The void stares back.|minecraft:entity.wither.spawn|true"
                        ),
                        () -> "",
                        o -> o instanceof String
                );

        // EFFECT Events
        EFFECT_EVENTS = builder
                .comment(
                        "EFFECT events format:",
                        "pressure|isAbsolute|value|announce|soundId|isConsistent",
                        "",
                        "value examples:",
                        "potion;minecraft:darkness;200;0",
                        "attribute;minecraft:generic.max_health;-4;add",
                        "",
                        "Full examples:",
                        "5.0|true|potion;minecraft:darkness;200;0|Darkness...|minecraft:entity.player.levelup|false",
                        "10.0|false|attribute;minecraft:generic.max_health;-4;add||minecraft:entity.player.levelup|true"
                )
                .defineListAllowEmpty(
                        "effectEvents",
                        List.of(
                                "10.0|true|potion;minecraft:darkness;60;0|Your vision flickers for a moment.|minecraft:ambient.cave|false",
                                "30.0|true|attribute;minecraft:generic.max_health;-2;add|You feel slightly drained by the pressure.|minecraft:entity.warden.heartbeat|true",
                                "45.0|true|attribute;minecraft:generic.movement_speed;-0.02;add|The air grows heavy and resistant.|minecraft:block.respawn_anchor.ambient|true"
                        ),
                        () -> "",
                        o -> o instanceof String
                );

        // MOB_CHANGE Events
        MOB_CHANGE_EVENTS = builder
                .comment(
                        "MOB_CHANGE events format:",
                        "pressure|isAbsolute|value|announce|soundId|isConsistent",
                        "",
                        "value format:",
                        "[target];rule1,rule2,rule3",
                        "",
                        "target (optional):",
                        "  mob:zombie",
                        "  tag:undead",
                        "  category:monster",
                        "  (if omitted -> applies to all mobs)",
                        "",
                        "rule examples:",
                        "  health:+4",
                        "  armor:+2",
                        "  speed:+0.05",
                        "  helmet:iron",
                        "",
                        "Notes:",
                        "- isAbsolute=false is NOT recommended for mob_change",
                        "- isConsistent=true is STRONGLY recommended for mob_change",
                        "",
                        "Full examples:",
                        "10.0|true|armor:+2,health:+4|Mobs feel tougher||true",
                        "15.0|true|mob:zombie;health:+6,helmet:iron||true",
                        "25.0|true|category:monster;speed:+0.05||true"
                )
                .defineListAllowEmpty(
                        "mobChangeEvents",
                        List.of(
                                "15.0|true|armor:+1|Creatures seem a little tougher.|minecraft:entity.evoker.prepare_summon|true",
                                "35.0|true|category:monster;health:+4|Monsters adapt to the rising pressure.|minecraft:entity.iron_golem.repair|true",
                                "50.0|true|tag:undead;helmet:iron,health:+2|The undead reinforce themselves against the light.|minecraft:item.armor.equip_iron|true"
                        ),
                        () -> "",
                        o -> o instanceof String
                );

        builder.pop(); // events
        builder.pop(); // pressure

        SPEC = builder.build();
    }
}
