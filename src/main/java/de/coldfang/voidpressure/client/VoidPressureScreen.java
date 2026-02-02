package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.config.ClientConfig;
import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoidPressureScreen extends Screen {

    public enum Page {
        OVERVIEW,
        GENERATOR,
        TRIGGERED,
        COMMANDS,
        EFFECTS,
        MOBS,
        KILL_OVERRIDES
    }

    private static final String WIKI_URL = "https://www.coldfang.de/minecraft/void-pressure-wiki/";

    private Page page = Page.OVERVIEW;

    private int panelX, panelY, panelW, panelH;
    private int rightX, rightY, rightW, rightH;

    // Separator between left nav and right content
    private int sepX;
    private int sepY0;
    private int sepY1;

    private Button btnOverview;
    private Button btnGenerator;
    private Button btnTriggered;
    private Button btnCommands;
    private Button btnEffects;
    private Button btnMobs;
    private Button btnKillOverrides;

    private VoidPressureScrollArea scrollArea;
    private VoidPressureGeneratorPanel generatorPanel;

    private Button btnAddToConfig;
    private Button btnWiki;

    private EditBox edDeleteIndex;
    private Button btnDelete;

    private Checkbox cbShowTrendHud;

    private EditBox edOverrideEntityId;
    private EditBox edOverrideGain;

    private Button btnMoveHud;
    private Button btnResetHud;

    private static final int PAD = 12;
    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 38;

    private int overviewRefreshTicks = 0;

    private static final int OVERVIEW_CARD_GAP = 6;
    private static final int OVERVIEW_CARD_H = 48;
    private static final int OVERVIEW_DASHBOARD_ROWS = 2;
    private static final int OVERVIEW_DASHBOARD_H =
            OVERVIEW_DASHBOARD_ROWS * OVERVIEW_CARD_H + (OVERVIEW_DASHBOARD_ROWS - 1) * OVERVIEW_CARD_GAP;

    private boolean lastHudTrendUiValue = false;

    private static final int OVERRIDE_ROW_H = 20;
    private static final int OVERRIDE_GAP = 6;
    private static final int OVERRIDE_FORM_H = OVERRIDE_ROW_H * 2 + OVERRIDE_GAP;

    public VoidPressureScreen() {
        super(Component.literal("Void Pressure"));
    }

    @Override
    protected void init() {
        panelW = 460;
        panelH = 270;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int leftW = 140;
        int leftX = panelX + PAD;
        int leftY = panelY + HEADER_H;
        int leftH = panelH - HEADER_H - FOOTER_H - PAD;

        rightX = leftX + leftW + PAD;
        rightY = leftY;
        rightW = panelX + panelW - PAD - rightX;
        rightH = leftH;

        // separator sits in middle of the gap (PAD) between left and right
        sepX = leftX + leftW + (PAD / 2);

        sepY0 = panelY + 1;
        sepY1 = panelY + panelH - 1;

        int btnH = 20;
        int gap = 6;
        int by = leftY;

        btnOverview = addRenderableWidget(Button.builder(Component.literal("Overview"), b -> switchPage(Page.OVERVIEW))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnGenerator = addRenderableWidget(Button.builder(Component.literal("Event Generator"), b -> switchPage(Page.GENERATOR))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnTriggered = addRenderableWidget(Button.builder(Component.literal("Event History"), b -> switchPage(Page.TRIGGERED))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnCommands = addRenderableWidget(Button.builder(Component.literal("Command Events"), b -> switchPage(Page.COMMANDS))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnEffects = addRenderableWidget(Button.builder(Component.literal("Effect Events"), b -> switchPage(Page.EFFECTS))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnMobs = addRenderableWidget(Button.builder(Component.literal("Mob Events"), b -> switchPage(Page.MOBS))
                .bounds(leftX, by, leftW, btnH).build());
        by += btnH + gap;

        btnKillOverrides = addRenderableWidget(Button.builder(Component.literal("Pressure Overrides"), b -> switchPage(Page.KILL_OVERRIDES))
                .bounds(leftX, by, leftW, btnH).build());

        scrollArea = new VoidPressureScrollArea(rightX, rightY, rightW, rightH);
        generatorPanel = new VoidPressureGeneratorPanel(this, rightX, rightY, rightW);

        int bottomY = panelY + panelH - 28;

        btnAddToConfig = addRenderableWidget(Button.builder(Component.literal("Add to config"), b -> {
                    if (page == Page.GENERATOR) {
                        generatorPanel.onAddClicked();
                    } else if (page == Page.KILL_OVERRIDES) {
                        onAddKillOverrideClicked();
                    }
                })
                .bounds(panelX + PAD + 150, bottomY, 90, 20)
                .build());
        btnAddToConfig.setTooltip(Tooltip.create(Component.literal("Adds the current form entry into the config.")));

        btnWiki = addRenderableWidget(Button.builder(Component.literal("Wiki"), b -> openWikiConfirm())
                .bounds(panelX + PAD + 265, bottomY, 70, 20)
                .build());
        btnWiki.setTooltip(Tooltip.create(Component.literal("Open the Void Pressure Wiki in your browser.")));

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(panelX + panelW - PAD - 70, bottomY, 70, 20)
                .build());

        int closeX = panelX + panelW - PAD - 70;
        int idxW = 40;
        int delW = 70;
        int g = 6;

        int idxX = closeX - g - idxW;
        int delX = idxX - g - delW;

        edDeleteIndex = new EditBox(this.font, idxX, bottomY, idxW, 20, Component.literal("idx"));
        edDeleteIndex.setMaxLength(6);
        edDeleteIndex.setValue("");
        edDeleteIndex.setTooltip(Tooltip.create(Component.literal("Index in the list (0-based).")));
        addRenderableWidget(edDeleteIndex);

        btnDelete = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> onDeleteClicked())
                .bounds(delX, bottomY, delW, 20)
                .build());
        btnDelete.setTooltip(Tooltip.create(Component.literal("Deletes the entry at index from the config list.")));

        boolean initial = ClientConfig.HUD_SHOW_TREND.get();
        lastHudTrendUiValue = initial;

        cbShowTrendHud = addRenderableWidget(
                Checkbox.builder(Component.literal("Show trend in HUD"), this.font)
                        .selected(initial)
                        .pos(rightX + 2, rightY + OVERVIEW_DASHBOARD_H + 12)
                        .build()
        );
        cbShowTrendHud.setTooltip(Tooltip.create(Component.literal("If enabled, the HUD shows the current 60s trend under the pressure value.")));

        btnMoveHud = addRenderableWidget(
                Button.builder(Component.literal("Move HUD"), b -> startMoveHudMode())
                        .bounds(rightX + 2, rightY + OVERVIEW_DASHBOARD_H + 12 + 22, 96, 20)
                        .build()
        );
        btnMoveHud.setTooltip(Tooltip.create(Component.literal("Drag the HUD with your mouse and save the new position.")));

        btnResetHud = addRenderableWidget(
                Button.builder(Component.literal("Reset HUD"), b -> resetHudToDefault())
                        .bounds(rightX + 2, rightY + OVERVIEW_DASHBOARD_H + 12 + 44, 96, 20)
                        .build()
        );
        btnResetHud.setTooltip(Tooltip.create(Component.literal("Resets HUD position back to top-left (default).")));

        edOverrideEntityId = new EditBox(this.font, rightX, rightY, rightW, OVERRIDE_ROW_H, Component.literal("Entity Id"));
        edOverrideEntityId.setHint(Component.literal("e.g. minecraft:iron_golem"));
        edOverrideEntityId.setMaxLength(256);
        edOverrideEntityId.setValue("");
        edOverrideEntityId.setTooltip(Tooltip.create(Component.literal("Entity type id (ResourceLocation), e.g. minecraft:zombie")));
        addRenderableWidget(edOverrideEntityId);

        edOverrideGain = new EditBox(this.font, rightX, rightY, rightW, OVERRIDE_ROW_H, Component.literal("Gain"));
        edOverrideGain.setHint(Component.literal("e.g. 2.0"));
        edOverrideGain.setMaxLength(32);
        edOverrideGain.setValue("");
        edOverrideGain.setTooltip(Tooltip.create(Component.literal("Override value for gainPlayerKill (>= 0). Example: 2.0")));
        addRenderableWidget(edOverrideGain);

        overviewRefreshTicks = 0;

        layoutForPage();
        reloadRightSide();
        updateActiveButtonStyles();
        updatePanelsVisibility();
        updateBottomControlsVisibility();
    }

    @Override
    public void tick() {
        super.tick();

        if (page == Page.OVERVIEW) {
            overviewRefreshTicks++;
            if (overviewRefreshTicks >= 20) {
                overviewRefreshTicks = 0;
                reloadRightSide();
            }
        } else {
            overviewRefreshTicks = 0;
        }

        if (cbShowTrendHud != null && cbShowTrendHud.visible) {
            boolean ui = cbShowTrendHud.selected();
            if (ui != lastHudTrendUiValue) {
                lastHudTrendUiValue = ui;
                ClientConfig.HUD_SHOW_TREND.set(ui);
                VoidPressureConfigWriter.setHudShowTrend(ui);
            }
        }
    }

    public <T extends AbstractWidget> T addWidget(T w) {
        return this.addRenderableWidget(w);
    }

    public Font getScreenFont() {
        return this.font;
    }

    @SuppressWarnings("unused")
    public Minecraft getScreenMinecraft() {
        return this.minecraft;
    }

    private void switchPage(Page p) {
        if (this.page == p) return;
        this.page = p;

        overviewRefreshTicks = 0;

        layoutForPage();
        reloadRightSide();
        updateActiveButtonStyles();
        updatePanelsVisibility();
        updateBottomControlsVisibility();
    }

    private void updateActiveButtonStyles() {
        btnOverview.active = page != Page.OVERVIEW;
        btnGenerator.active = page != Page.GENERATOR;
        btnTriggered.active = page != Page.TRIGGERED;
        btnCommands.active = page != Page.COMMANDS;
        btnEffects.active = page != Page.EFFECTS;
        btnMobs.active = page != Page.MOBS;
        btnKillOverrides.active = page != Page.KILL_OVERRIDES;
    }

    private void updatePanelsVisibility() {
        generatorPanel.setVisible(page == Page.GENERATOR);

        boolean showOverrides = page == Page.KILL_OVERRIDES;
        edOverrideEntityId.setVisible(showOverrides);
        edOverrideGain.setVisible(showOverrides);
    }

    private void updateBottomControlsVisibility() {
        btnAddToConfig.visible = (page == Page.GENERATOR || page == Page.KILL_OVERRIDES);
        btnWiki.visible = (page == Page.GENERATOR);

        boolean showDelete = (page == Page.COMMANDS || page == Page.EFFECTS || page == Page.MOBS || page == Page.KILL_OVERRIDES);
        edDeleteIndex.setVisible(showDelete);
        btnDelete.visible = showDelete;

        boolean showOverviewControls = (page == Page.OVERVIEW);
        cbShowTrendHud.visible = showOverviewControls;
        if (btnMoveHud != null) btnMoveHud.visible = showOverviewControls;
        if (btnResetHud != null) btnResetHud.visible = showOverviewControls;
    }

    private void layoutForPage() {
        generatorPanel.layout(rightX, rightY, rightW);

        if (page == Page.OVERVIEW) {
            int top = rightY + OVERVIEW_DASHBOARD_H + 8;
            int h = Math.max(10, rightH - (OVERVIEW_DASHBOARD_H + 8));
            scrollArea.setBounds(rightX, top, rightW, h);

            cbShowTrendHud.setX(rightX + 2);
            cbShowTrendHud.setY(top + 4);

            if (btnMoveHud != null) {
                btnMoveHud.setX(rightX + 2);
                btnMoveHud.setY(top + 4 + 22);
                btnMoveHud.setWidth(96);
            }
            if (btnResetHud != null) {
                btnResetHud.setX(rightX + 2);
                btnResetHud.setY(top + 4 + 44);
                btnResetHud.setWidth(96);
            }
            return;
        }

        if (page == Page.KILL_OVERRIDES) {
            edOverrideEntityId.setX(rightX);
            edOverrideEntityId.setY(rightY);
            edOverrideEntityId.setWidth(rightW);

            edOverrideGain.setX(rightX);
            edOverrideGain.setY(rightY + OVERRIDE_ROW_H + OVERRIDE_GAP);
            edOverrideGain.setWidth(rightW);

            int top = rightY + OVERRIDE_FORM_H + 8;
            int h = Math.max(10, rightH - (OVERRIDE_FORM_H + 8));
            scrollArea.setBounds(rightX, top, rightW, h);
            return;
        }

        if (page != Page.GENERATOR) {
            scrollArea.setBounds(rightX, rightY, rightW, rightH);
            return;
        }

        int formH = generatorPanel.getFormHeight();
        scrollArea.setBounds(rightX, rightY + formH, rightW, Math.max(10, rightH - formH));
    }

    @Override
    public void resize(@NotNull Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollArea != null && scrollArea.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // IMPORTANT: Do NOT draw a dimming overlay over the whole screen.
        // Leaving this empty keeps the world fully visible (no dark shimmer outside the panel).
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick);

        gg.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);
        gg.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xAA000000);

        renderSeparator(gg);

        Component title = Component.literal("Void Pressure Config");
        int leftW = 140;
        int leftX = panelX + PAD;
        int titleX = leftX + (leftW - this.font.width(title)) / 2;
        int titleY = panelY + 8;
        gg.drawString(this.font, title, titleX, titleY, 0xFFFFFFFF, true);

        if (page == Page.OVERVIEW) {
            renderOverviewDashboard(gg);
        }

        scrollArea.render(gg);

        super.render(gg, mouseX, mouseY, partialTick);
    }

    private void renderSeparator(GuiGraphics gg) {
        int x0 = sepX;
        int x1 = sepX + 1;

        gg.fill(x0, sepY0, x0 + 1, sepY1, 0xFF3A3A3A);
        gg.fill(x1, sepY0, x1 + 1, sepY1, 0xFF5A5A5A);
    }

    private void renderOverviewDashboard(GuiGraphics gg) {
        OverviewSnapshot s = readOverviewSnapshot();

        int dashX = rightX;
        int dashY = rightY;
        int dashW = rightW;

        int colGap = OVERVIEW_CARD_GAP;
        int colW = (dashW - colGap) / 2;

        int x1 = dashX + colW + colGap;
        int y1 = dashY + OVERVIEW_CARD_H + OVERVIEW_CARD_GAP;

        drawCard(gg, dashX, dashY, colW, "Current Pressure", s.hasData ? fmt1(s.current) : "(no server)", 0xFFFFFFFF);

        String trendLabel;
        int trendColor;
        if (!s.hasData) {
            trendLabel = "-";
            trendColor = 0xFFAAAAAA;
        } else if (s.net60 > 0.0005) {
            trendLabel = "↑ Increasing (" + fmt3(s.net60) + ")";
            trendColor = 0xFF55FF55;
        } else if (s.net60 < -0.0005) {
            trendLabel = "↓ Decreasing (" + fmt3(s.net60) + ")";
            trendColor = 0xFFFF5555;
        } else {
            trendLabel = "→ Stable";
            trendColor = 0xFFAAAAAA;
        }

        drawCard(gg, x1, dashY, colW, "Trend", trendLabel, trendColor);

        drawCard(gg, dashX, y1, colW, "Lost (last 60s)", s.hasData ? ("-" + fmt3(s.loss60)) : "-", 0xFFFF5555);
        drawCard(gg, x1, y1, colW, "Gained (last 60s)", s.hasData ? ("+" + fmt3(s.gain60)) : "-", 0xFF55FF55);
    }

    private void drawCard(GuiGraphics gg, int x, int y, int w, String title, String value, int valueColor) {
        gg.fill(x, y, x + w, y + OVERVIEW_CARD_H, 0xAA000000);
        gg.fill(x + 1, y + 1, x + w - 1, y + OVERVIEW_CARD_H - 1, 0x66000000);

        gg.drawString(this.font, title, x + 6, y + 5, 0xFFCCCCCC, false);
        gg.drawString(this.font, value, x + 6, y + 22, valueColor, false);
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String fmt3(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private record OverviewSnapshot(boolean hasData, double current, double gain60, double loss60, double net60) {
    }

    private OverviewSnapshot readOverviewSnapshot() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return new OverviewSnapshot(false, 0, 0, 0, 0);

        ServerLevel level = server.overworld();

        VoidPressureSavedData data = VoidPressureSavedData.get(level);
        data.tickRollingWindow(level);

        return new OverviewSnapshot(
                true,
                data.getPressure(),
                data.getGainLast60s(),
                data.getLossLast60s(),
                data.getNetLast60s()
        );
    }

    private void reloadRightSide() {
        if (page == Page.OVERVIEW) {
            scrollArea.setRawLines(List.of());
            return;
        }

        List<String> lines = new ArrayList<>();

        switch (page) {
            case GENERATOR -> { }
            case TRIGGERED -> VoidPressurePages.buildTriggered(lines);
            case COMMANDS -> {
                lines.add("Command Events");
                lines.add("");
                lines.add("Delete: enter index (0-based) bottom and click Delete.");
                lines.add("");
                VoidPressurePages.buildConfigList(lines, "", CommonConfig.COMMAND_EVENTS.get());
            }
            case EFFECTS -> {
                lines.add("Effect Events");
                lines.add("");
                lines.add("Delete: enter index (0-based) bottom and click Delete.");
                lines.add("");
                VoidPressurePages.buildConfigList(lines, "", CommonConfig.EFFECT_EVENTS.get());
            }
            case MOBS -> {
                lines.add("Mob Events");
                lines.add("");
                lines.add("Delete: enter index (0-based) bottom and click Delete.");
                lines.add("");
                VoidPressurePages.buildConfigList(lines, "", CommonConfig.MOB_CHANGE_EVENTS.get());
            }
            case KILL_OVERRIDES -> {
                lines.add("gainPlayerKill Overrides");
                lines.add("");
                lines.add("Add: enter entity id + gain at the top and click Add to config.");
                lines.add("Delete: enter index (0-based) bottom and click Delete.");
                lines.add("");
                VoidPressurePages.buildGainPlayerKillOverrides(lines, CommonConfig.GAIN_PLAYER_KILL_OVERRIDES.get());
            }
        }

        scrollArea.setRawLines(lines);
    }

    public void refreshRightSide() {
        layoutForPage();
        reloadRightSide();
        updatePanelsVisibility();
        updateBottomControlsVisibility();
    }

    private void openWikiConfirm() {
        if (this.minecraft == null) return;

        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) Util.getPlatform().openUri(WIKI_URL);
                    this.minecraft.setScreen(this);
                },
                Component.literal("Open Wiki"),
                Component.literal("This will open your web browser.\nDo you want to continue?")
        ));
    }

    private void onAddKillOverrideClicked() {
        String id = edOverrideEntityId.getValue().trim();
        String gainS = edOverrideGain.getValue().trim();

        if (id.isEmpty() || gainS.isEmpty()) return;

        double gain;
        try {
            gain = Double.parseDouble(gainS);
        } catch (Exception ignored) {
            return;
        }
        if (gain < 0.0) gain = 0.0;

        String rawLine = id + "=" + String.format(Locale.ROOT, "%.3f", gain);

        appendToKillOverrides(rawLine);
        MinecraftServerRunner.runOnServerThread(() -> VoidPressureConfigWriter.appendGainPlayerKillOverride(rawLine));

        edOverrideEntityId.setValue("");
        edOverrideGain.setValue("");

        refreshRightSide();
    }

    private void onDeleteClicked() {
        String s = edDeleteIndex.getValue().trim();
        if (s.isEmpty()) return;

        int idx;
        try {
            idx = Integer.parseInt(s);
        } catch (Exception ignored) {
            return;
        }

        if (page == Page.COMMANDS) {
            deleteAt(CommonConfig.COMMAND_EVENTS, "commandEvents", idx);
        } else if (page == Page.EFFECTS) {
            deleteAt(CommonConfig.EFFECT_EVENTS, "effectEvents", idx);
        } else if (page == Page.MOBS) {
            deleteAt(CommonConfig.MOB_CHANGE_EVENTS, "mobChangeEvents", idx);
        } else if (page == Page.KILL_OVERRIDES) {
            deleteAt(CommonConfig.GAIN_PLAYER_KILL_OVERRIDES, "gainPlayerKillOverrides", idx);
        }

        edDeleteIndex.setValue("");
        refreshRightSide();
    }

    private static void deleteAt(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<List<? extends String>> cfg, String tomlKey, int idx) {
        List<? extends String> current = cfg.get();
        if (idx < 0 || idx >= current.size()) return;

        List<String> copy = new ArrayList<>(current);
        copy.remove(idx);
        cfg.set(copy);

        MinecraftServerRunner.runOnServerThread(() -> VoidPressureConfigWriter.deleteAtIndex(tomlKey, idx));
    }

    private static void appendToKillOverrides(String line) {
        List<? extends String> current = CommonConfig.GAIN_PLAYER_KILL_OVERRIDES.get();
        List<String> copy = new ArrayList<>(current.size() + 1);
        for (String s : current) {
            if (s != null) copy.add(s);
        }
        copy.add(line);
        CommonConfig.GAIN_PLAYER_KILL_OVERRIDES.set(copy);
    }

    private void startMoveHudMode() {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new VoidPressureHudMoveScreen());
    }

    private void resetHudToDefault() {
        int x = 10;
        int y = 10;

        ClientConfig.HUD_ANCHOR.set(ClientConfig.HudAnchor.TOP_LEFT);
        ClientConfig.HUD_X.set(x);
        ClientConfig.HUD_Y.set(y);

        VoidPressureConfigWriter.setHudPosition(x, y);
    }

    private static final class MinecraftServerRunner {
        private MinecraftServerRunner() {
        }

        static void runOnServerThread(Runnable r) {
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) server.execute(r);
        }
    }
}
