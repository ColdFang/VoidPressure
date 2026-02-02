package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.config.CommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidPressureGeneratorPanel {

    public enum AddType {
        COMMAND,
        EFFECT,
        MOB_CHANGE
    }

    private static final int ROW_H = 20;
    private static final int GAP = 6;

    private static final Tooltip TIP_TYPE = tip("Select which event list to write into.");
    private static final Tooltip TIP_PRESSURE = tip("Pressure threshold number (e.g. 5.0).");
    private static final Tooltip TIP_ABS = tip("""
            true: triggers when crossing the threshold.
            false: triggers each multiple (pressure, 2x, 3x...)""");
    private static final Tooltip TIP_CONS = tip("""
            Command: true = run once ever.
            Effect/Mob: true = persists across relog.""");

    private static final Tooltip TIP_VALUE = tip("""
            COMMAND: command without '/'
            Example: say Hello

            EFFECT: value string
            Example: potion;minecraft:darkness;200;0""");

    private static final Tooltip TIP_ANNOUNCE = tip("If not empty, this text is sent to chat when event triggers.");
    private static final Tooltip TIP_SOUND = tip("Minecraft sound id, e.g. minecraft:entity.player.levelup\nEmpty = no sound.");

    private static final Tooltip TIP_MOB_TARGET = tip("""
            Only for MOB_CHANGE.
            Examples:
            mob:zombie
            tag:undead
            category:monster
            Empty = all mobs""");

    private static final Tooltip TIP_MOB_RULES = tip("""
            Only for MOB_CHANGE.
            Comma-separated rules.
            Examples:
            health:+4,armor:+2
            speed:+0.05
            helmet:iron""");

    private final VoidPressureScreen screen;

    private int x, y, w;

    private Button btnType;
    private AddType selectedType = AddType.COMMAND;

    private EditBox edPressure;
    private CycleButton<Boolean> edIsAbsolute;
    private CycleButton<Boolean> edIsConsistent;

    private EditBox edValue;
    private EditBox edAnnounce;
    private EditBox edSoundId;

    private EditBox edMobTarget;
    private EditBox edMobRules;

    private String message = "";

    public VoidPressureGeneratorPanel(VoidPressureScreen screen, int x, int y, int w) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.w = w;

        createWidgets();
        setVisible(false);
    }

    private static Tooltip tip(String text) {
        return Tooltip.create(Component.literal(text));
    }

    public int getFormHeight() {
        int rows = 0;
        rows += 1; // type
        rows += 1; // pressure
        rows += 1; // abs
        rows += 1; // cons
        rows += isMobMode() ? 0 : 1; // value
        rows += 1; // announce
        rows += 1; // sound
        rows += isMobMode() ? 2 : 0; // mob target + rules
        return rows * ROW_H + Math.max(0, rows - 1) * GAP;
    }

    public void layout(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = Math.max(10, w);

        applyVisibilityByMode();

        int cy = y;

        cy = layoutButton(btnType, x, cy, this.w);
        cy = layoutEdit(edPressure, x, cy, this.w);
        cy = layoutCycle(edIsAbsolute, x, cy, this.w);
        cy = layoutCycle(edIsConsistent, x, cy, this.w);

        if (!isMobMode()) {
            cy = layoutEdit(edValue, x, cy, this.w);
        }

        cy = layoutEdit(edAnnounce, x, cy, this.w);
        cy = layoutEdit(edSoundId, x, cy, this.w);

        if (isMobMode()) {
            cy = layoutEdit(edMobTarget, x, cy, this.w);
            layoutEdit(edMobRules, x, cy, this.w);
        }

        reapplyTooltips();
    }

    public void setVisible(boolean visible) {
        if (btnType != null) btnType.visible = visible;
        if (edPressure != null) edPressure.setVisible(visible);
        if (edIsAbsolute != null) edIsAbsolute.visible = visible;
        if (edIsConsistent != null) edIsConsistent.visible = visible;

        if (edAnnounce != null) edAnnounce.setVisible(visible);
        if (edSoundId != null) edSoundId.setVisible(visible);

        applyVisibilityByMode();
        reapplyTooltips();
    }

    @SuppressWarnings("unused")
    public void buildHelp(List<String> out) {
        out.add("Event Generator");
        out.add("");
        if (!message.isBlank()) out.add(message);
    }

    public void onAddClicked() {
        if (btnType == null || edPressure == null || edIsAbsolute == null || edIsConsistent == null
                || edAnnounce == null || edSoundId == null) {
            message = "Generator widgets not ready.";
            screen.refreshRightSide();
            return;
        }

        AddType type = selectedType;

        double pressure;
        try {
            pressure = Double.parseDouble(edPressure.getValue().trim());
        } catch (Exception e) {
            message = "Pressure is not a valid number.";
            screen.refreshRightSide();
            return;
        }

        boolean isAbs = edIsAbsolute.getValue();
        boolean isCons = edIsConsistent.getValue();

        String announce = edAnnounce.getValue().trim();
        String soundId = edSoundId.getValue().trim();

        String value = buildValueForType(type);
        String rawLine = buildRawLine(pressure, isAbs, value, announce, soundId, isCons);

        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            final AddType tf = type;
            final String rf = rawLine;
            server.execute(() -> {
                switch (tf) {
                    case COMMAND -> VoidPressureConfigWriter.appendCommandEvent(rf);
                    case EFFECT -> VoidPressureConfigWriter.appendEffectEvent(rf);
                    case MOB_CHANGE -> VoidPressureConfigWriter.appendMobChangeEvent(rf);
                }
            });
        }

        switch (type) {
            case COMMAND -> appendToConfigList(CommonConfig.COMMAND_EVENTS, rawLine);
            case EFFECT -> appendToConfigList(CommonConfig.EFFECT_EVENTS, rawLine);
            case MOB_CHANGE -> appendToConfigList(CommonConfig.MOB_CHANGE_EVENTS, rawLine);
        }

        message = "Added.";

        if (edValue != null) edValue.setValue("");
        edAnnounce.setValue("");
        edSoundId.setValue("");
        if (edMobTarget != null) edMobTarget.setValue("");
        if (edMobRules != null) edMobRules.setValue("");

        screen.refreshRightSide();
    }

    public boolean isMobMode() {
        return selectedType == AddType.MOB_CHANGE;
    }

    private String buildValueForType(AddType type) {
        if (type == AddType.MOB_CHANGE) {
            String target = (edMobTarget == null) ? "" : edMobTarget.getValue().trim();
            String rules = (edMobRules == null) ? "" : edMobRules.getValue().trim();
            return (target.isBlank() ? "" : target) + ";" + rules;
        }
        return (edValue == null) ? "" : edValue.getValue().trim();
    }

    private static String buildRawLine(double pressure, boolean isAbs, String value, String announce, String soundId, boolean isCons) {
        return String.format(
                Locale.ROOT,
                "%.1f|%b|%s|%s|%s|%b",
                pressure,
                isAbs,
                value,
                announce,
                soundId,
                isCons
        );
    }

    private void applyVisibilityByMode() {
        boolean panelVisible = btnType != null && btnType.visible;
        boolean mob = panelVisible && isMobMode();

        if (edValue != null) {
            edValue.setVisible(panelVisible && !mob);
            if (mob) edValue.setValue("");
        }

        if (edMobTarget != null) edMobTarget.setVisible(mob);
        if (edMobRules != null) edMobRules.setVisible(mob);

        if (!mob) {
            if (edMobTarget != null) edMobTarget.setValue("");
            if (edMobRules != null) edMobRules.setValue("");
        }
    }

    private void reapplyTooltips() {
        if (btnType != null) btnType.setTooltip(TIP_TYPE);
        if (edPressure != null) edPressure.setTooltip(TIP_PRESSURE);
        if (edIsAbsolute != null) edIsAbsolute.setTooltip(TIP_ABS);
        if (edIsConsistent != null) edIsConsistent.setTooltip(TIP_CONS);

        if (edValue != null) edValue.setTooltip(TIP_VALUE);
        if (edAnnounce != null) edAnnounce.setTooltip(TIP_ANNOUNCE);
        if (edSoundId != null) edSoundId.setTooltip(TIP_SOUND);

        if (edMobTarget != null) edMobTarget.setTooltip(TIP_MOB_TARGET);
        if (edMobRules != null) edMobRules.setTooltip(TIP_MOB_RULES);
    }

    private void createWidgets() {
        selectedType = AddType.COMMAND;

        btnType = screen.addWidget(
                Button.builder(Component.literal(typeLabel(selectedType)), b -> {
                            selectedType = nextType(selectedType);
                            b.setMessage(Component.literal(typeLabel(selectedType)));
                            message = "";
                            screen.refreshRightSide();
                        })
                        .bounds(x, y, w, ROW_H)
                        .build()
        );

        edPressure = createEditBox("Pressure", "pressure threshold", 32, "5.0");
        screen.addWidget(edPressure);

        edIsAbsolute = screen.addWidget(
                CycleButton.<Boolean>builder(b -> Component.literal(b ? "isAbsolute = true" : "isAbsolute = false"))
                        .withValues(Boolean.TRUE, Boolean.FALSE)
                        .create(x, y, w, ROW_H, Component.empty(), (btn, val) -> {
                        })
        );

        edIsConsistent = screen.addWidget(
                CycleButton.<Boolean>builder(b -> Component.literal(b ? "isConsistent = true" : "isConsistent = false"))
                        .withValues(Boolean.TRUE, Boolean.FALSE)
                        .create(x, y, w, ROW_H, Component.empty(), (btn, val) -> {
                        })
        );

        edValue = createEditBox("Value / Command", "command or effect value", 5000, "");
        screen.addWidget(edValue);

        edAnnounce = createEditBox("Announce (optional)", "announce message (optional)", 5000, "");
        screen.addWidget(edAnnounce);

        edSoundId = createEditBox("SoundId (optional)", "sound id (optional)", 128, "");
        screen.addWidget(edSoundId);

        edMobTarget = createEditBox("Mob Target (optional)", "target (all mobs if empty)", 256, "");
        screen.addWidget(edMobTarget);

        edMobRules = createEditBox("Mob Rules (rule1,rule2,...)", "rules", 5000, "");
        screen.addWidget(edMobRules);

        reapplyTooltips();
        applyVisibilityByMode();
    }

    private static String typeLabel(AddType type) {
        return switch (type) {
            case COMMAND -> "Command";
            case EFFECT -> "Effect";
            case MOB_CHANGE -> "Mob Change";
        };
    }

    private static AddType nextType(AddType current) {
        AddType[] vals = AddType.values();
        int next = (current.ordinal() + 1) % vals.length;
        return vals[next];
    }

    private EditBox createEditBox(String label, String hint, int maxLen, String defaultValue) {
        EditBox box = new EditBox(screen.getScreenFont(), x, y, w, ROW_H, Component.literal(label));
        box.setHint(Component.literal(hint));
        box.setMaxLength(maxLen);
        box.setValue(defaultValue);
        return box;
    }

    private static int layoutEdit(EditBox box, int x, int y, int w) {
        if (box != null) {
            box.setPosition(x, y);
            box.setWidth(w);
        }
        return y + ROW_H + GAP;
    }

    private static int layoutCycle(CycleButton<?> btn, int x, int y, int w) {
        if (btn != null) {
            btn.setPosition(x, y);
            btn.setWidth(w);
        }
        return y + ROW_H + GAP;
    }

    private static int layoutButton(Button btn, int x, int y, int w) {
        if (btn != null) {
            btn.setPosition(x, y);
            btn.setWidth(w);
        }
        return y + ROW_H + GAP;
    }

    private static void appendToConfigList(ModConfigSpec.ConfigValue<List<? extends String>> cfg, String line) {
        List<? extends String> current = cfg.get();
        List<String> copy = new ArrayList<>(current.size() + 1);
        for (String s : current) {
            if (s != null) copy.add(s);
        }
        copy.add(line);
        cfg.set(copy);
    }
}
