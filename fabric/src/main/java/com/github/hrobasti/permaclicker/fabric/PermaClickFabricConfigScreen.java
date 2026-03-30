package com.github.hrobasti.permaclicker.fabric;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * Custom vanilla-like configuration screen for PermaClick on Fabric.
 */
public final class PermaClickFabricConfigScreen extends Screen {
    private static final int CONTROL_WIDTH = 150;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 34;
    private static final int PANEL_PADDING_X = 20;
    private static final int CONTENT_PADDING_X = 10;
    private static final int ROW_GAP = 3;
    private static final int INFO_ICON_SIZE = 10;
    private static final int INFO_ICON_GAP = 6;
    private static final int INFO_ICON_BG_COLOR = 0xAA2E5A88;
    private static final int INFO_ICON_TEXT_COLOR = 0xFFFFFFFF;
    private static final List<String> OVERLAY_COLORS = List.of(
        "black",
        "dark_blue",
        "dark_green",
        "dark_aqua",
        "dark_red",
        "dark_purple",
        "gold",
        "gray",
        "dark_gray",
        "blue",
        "green",
        "aqua",
        "red",
        "light_purple",
        "yellow",
        "white"
    );

    private final Screen parent;

    private boolean overlayEnabled;
    private String overlayColor;
    private boolean runInBackground;
    private int autoStopMinutes;
    private boolean movementLockEnabled;
    private boolean updateCheckEnabled;
    private UpdateChannel updateChannel;

    private final List<Row> rows = new ArrayList<>();
    private EditBox autoStopInputField;
    private Button doneButton;
    private Button cancelButton;
    private List<Component> hoveredTooltipLines = List.of();

    private int scrollOffset;
    private int maxScroll;

    public PermaClickFabricConfigScreen(Screen parent) {
        super(Component.translatable("permaclicker.config.title"));
        this.parent = parent;

        PermaClickConfig config = PermaClickFabricEntrypoint.bridge().currentConfig();
        this.overlayEnabled = config.overlayEnabled();
        this.overlayColor = config.overlayColor();
        this.runInBackground = config.runWhenUnfocused() || config.runWhenMinimized();
        this.autoStopMinutes = config.autoStopMinutes();
        this.movementLockEnabled = config.movementLockEnabled();
        this.updateCheckEnabled = config.updateCheckEnabled();
        this.updateChannel = config.updateChannel();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.rows.clear();
        this.scrollOffset = 0;

        buildRows();

        int buttonY = this.height - FOOTER_HEIGHT + 8;
        this.cancelButton = this.addRenderableWidget(
            Button.builder(Component.translatable("permaclicker.config.button.cancel"), button -> onClose())
                .bounds(this.width / 2 - 155, buttonY, 150, 20)
                .build()
        );
        this.doneButton = this.addRenderableWidget(
            Button.builder(Component.translatable("permaclicker.config.button.save_close"), button -> saveAndClose())
                .bounds(this.width / 2 + 5, buttonY, 150, 20)
                .build()
        );

        updateRowsAndLayout();
    }

    private void buildRows() {
        this.rows.add(new SectionRow(Component.translatable("permaclicker.config.section.general")));
        this.rows.add(new ActionButtonRow(
            Component.translatable("permaclicker.config.runtime_mode"),
            "permaclicker.config.runtime_mode.tooltip",
            this::runtimeModeValue,
            btn -> runInBackground = !runInBackground,
            () -> true
        ));
        this.rows.add(new AutoStopRow(
            Component.translatable("permaclicker.config.auto_stop_minutes"),
            "permaclicker.config.auto_stop_minutes.tooltip"
        ));
        this.rows.add(new ToggleRow(
            Component.translatable("permaclicker.config.cursor_lock"),
            "permaclicker.config.cursor_lock.tooltip",
            () -> movementLockEnabled,
            value -> movementLockEnabled = value
        ));

        this.rows.add(new SectionRow(Component.translatable("permaclicker.config.section.overlay")));
        this.rows.add(new ToggleRow(
            Component.translatable("permaclicker.config.overlay.enabled"),
            "permaclicker.config.overlay.enabled.tooltip",
            () -> overlayEnabled,
            value -> overlayEnabled = value
        ));
        this.rows.add(new ActionButtonRow(
            Component.translatable("permaclicker.config.overlay.color"),
            "permaclicker.config.overlay.color.tooltip",
            this::overlayColorValue,
            btn -> overlayColor = nextOverlayColor(overlayColor),
            () -> true
        ));

        this.rows.add(new SectionRow(Component.translatable("permaclicker.config.section.updates")));
        this.rows.add(new ToggleRow(
            Component.translatable("permaclicker.config.update.enabled"),
            "permaclicker.config.update.enabled.tooltip",
            () -> updateCheckEnabled,
            value -> updateCheckEnabled = value
        ));
        this.rows.add(new ActionButtonRow(
            Component.translatable("permaclicker.config.update.channel"),
            "permaclicker.config.update.channel.tooltip",
            this::updateChannelValue,
            btn -> updateChannel = nextChannel(updateChannel),
            () -> updateCheckEnabled
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInBody(mouseY) && this.maxScroll > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - (int) (scrollY * 16), 0, this.maxScroll);
            updateRowsAndLayout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateRowsAndLayout();
        this.hoveredTooltipLines = List.of();

        guiGraphics.fill(0, 0, this.width, this.height, 0x90101010);

        int bodyTop = bodyTop();
        int bodyBottom = bodyBottom();

        guiGraphics.fill(PANEL_PADDING_X, bodyTop + 1, this.width - PANEL_PADDING_X, bodyBottom, 0x66000000);
        guiGraphics.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 1, 0x90FFFFFF);
        guiGraphics.fill(0, bodyBottom, this.width, bodyBottom + 1, 0x90FFFFFF);

        drawTitle(guiGraphics);

        int clipLeft = PANEL_PADDING_X + 1;
        int clipRight = this.width - PANEL_PADDING_X - 1;
        int clipTop = bodyTop + 1;
        int clipBottom = bodyBottom - 1;
        if (clipRight > clipLeft && clipBottom > clipTop) {
            guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            drawRows(guiGraphics, mouseX, mouseY);
            drawScrollbar(guiGraphics);
            guiGraphics.disableScissor();
        } else {
            drawRows(guiGraphics, mouseX, mouseY);
            drawScrollbar(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!this.hoveredTooltipLines.isEmpty()) {
            drawHoverInfoPanel(guiGraphics, mouseX, mouseY, this.hoveredTooltipLines);
        }
    }

    private void drawHoverInfoPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, List<Component> tooltipLines) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component tooltipLine : tooltipLines) {
            lines.addAll(this.font.split(tooltipLine, 220));
        }
        if (lines.isEmpty()) {
            return;
        }

        int lineHeight = 10;
        int maxTextWidth = 0;
        for (FormattedCharSequence line : lines) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(line));
        }

        int padding = 4;
        int boxWidth = maxTextWidth + padding * 2;
        int boxHeight = lines.size() * lineHeight + padding * 2;
        int boxX = Math.min(mouseX + 12, this.width - boxWidth - 6);
        int boxY = Math.min(mouseY + 12, this.height - boxHeight - 6);

        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xD0101010);

        int textY = boxY + padding;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(this.font, line, boxX + padding, textY, 0xFFFFFFFF, false);
            textY += lineHeight;
        }
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        Component titleToDraw = this.title.getString().isBlank() ? Component.literal("PermaClicker Settings") : this.title;
        int titleX = this.width / 2 - this.font.width(titleToDraw) / 2;
        guiGraphics.drawString(this.font, titleToDraw, titleX, 9, 0xFFFFFFFF, true);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelLeft = PANEL_PADDING_X + CONTENT_PADDING_X;
        int panelRight = this.width - PANEL_PADDING_X - CONTENT_PADDING_X;
        int bodyTop = bodyTop();

        int yCursor = bodyTop + 8 - this.scrollOffset;
        for (Row row : this.rows) {
            int y = yCursor;
            int h = row.height();
            if (y + h >= bodyTop + 1 && y <= bodyBottom() - 1) {
                row.drawLabel(guiGraphics, panelLeft, panelRight, y, h, mouseX, mouseY);
            }
            yCursor += h + ROW_GAP;
        }
    }

    private void drawScrollbar(GuiGraphics guiGraphics) {
        if (this.maxScroll <= 0) {
            return;
        }

        int bodyTop = bodyTop() + 2;
        int bodyBottom = bodyBottom() - 2;
        int trackHeight = Math.max(1, bodyBottom - bodyTop);
        int contentHeight = contentHeight();

        int thumbHeight = Mth.clamp((trackHeight * trackHeight) / Math.max(1, contentHeight), 24, trackHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = bodyTop + (int) ((float) this.scrollOffset / Math.max(1, this.maxScroll) * thumbTravel);

        int x0 = this.width - PANEL_PADDING_X - 6;
        int x1 = this.width - PANEL_PADDING_X - 2;

        guiGraphics.fill(x0, bodyTop, x1, bodyBottom, 0x55000000);
        guiGraphics.fill(x0, thumbY, x1, thumbY + thumbHeight, 0xB0B0B0B0);
    }

    private void updateRowsAndLayout() {
        for (Row row : this.rows) {
            row.refresh();
        }

        this.maxScroll = Math.max(0, contentHeight() - bodyHeight());
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

        int panelRight = this.width - PANEL_PADDING_X - CONTENT_PADDING_X;
        int controlX = panelRight - CONTROL_WIDTH;
        int bodyTop = bodyTop();
        int bodyBottom = bodyBottom();

        int yCursor = bodyTop + 8 - this.scrollOffset;
        for (Row row : this.rows) {
            int h = row.height();
            int controlY = yCursor + (h - 20) / 2;
            boolean rowVisible = yCursor + h >= bodyTop + 1 && yCursor <= bodyBottom - 1;
            boolean controlFitsBody = controlY >= bodyTop + 1 && controlY + 20 <= bodyBottom - 1;
            row.layout(controlX, controlY, rowVisible && controlFitsBody);
            yCursor += h + ROW_GAP;
        }

        if (this.doneButton != null && this.cancelButton != null) {
            int buttonY = this.height - FOOTER_HEIGHT + 8;
            this.cancelButton.setPosition(this.width / 2 - 155, buttonY);
            this.doneButton.setPosition(this.width / 2 + 5, buttonY);
        }
    }

    private int bodyTop() {
        return HEADER_HEIGHT + 1;
    }

    private int bodyBottom() {
        return this.height - FOOTER_HEIGHT;
    }

    private int bodyHeight() {
        return Math.max(0, bodyBottom() - bodyTop() - 2);
    }

    private int contentHeight() {
        int total = 8;
        for (Row row : this.rows) {
            total += row.height() + ROW_GAP;
        }
        return Math.max(0, total - ROW_GAP + 8);
    }

    private boolean isInBody(double mouseY) {
        return mouseY >= bodyTop() && mouseY <= bodyBottom();
    }

    private List<Component> buildTooltipLines(String tooltipKey) {
        return switch (tooltipKey) {
            case "permaclicker.config.runtime_mode.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.runtime_mode.focused"), "permaclicker.config.runtime_mode.tooltip.focused.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.runtime_mode.background"), "permaclicker.config.runtime_mode.tooltip.background.desc")
            );
            case "permaclicker.config.auto_stop_minutes.tooltip" -> List.of(
                formatOptionLine(Component.literal("0"), "permaclicker.config.auto_stop_minutes.tooltip.off.desc"),
                formatOptionLine(Component.literal("1-9999"), "permaclicker.config.auto_stop_minutes.tooltip.range.desc")
            );
            case "permaclicker.config.cursor_lock.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.cursor_lock.tooltip.on.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.cursor_lock.tooltip.off.desc")
            );
            case "permaclicker.config.overlay.enabled.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.overlay.enabled.tooltip.on.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.overlay.enabled.tooltip.off.desc")
            );
            case "permaclicker.config.overlay.color.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.overlay.color.tooltip.any.label"), "permaclicker.config.overlay.color.tooltip.any.desc")
            );
            case "permaclicker.config.update.enabled.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.update.enabled.tooltip.on.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.update.enabled.tooltip.off.desc")
            );
            case "permaclicker.config.update.channel.tooltip" -> List.of(
                formatOptionLine(Component.translatable("permaclicker.config.update.channel.stable"), "permaclicker.config.update.channel.tooltip.stable.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.update.channel.beta"), "permaclicker.config.update.channel.tooltip.beta.desc"),
                formatOptionLine(Component.translatable("permaclicker.config.update.channel.alpha"), "permaclicker.config.update.channel.tooltip.alpha.desc")
            );
            default -> List.of(Component.translatable(tooltipKey));
        };
    }

    private static Component formatOptionLine(Component optionLabel, String descriptionKey) {
        MutableComponent line = Component.empty();
        line.append(optionLabel.copy().withStyle(ChatFormatting.BOLD));
        line.append(Component.literal(": ").withStyle(ChatFormatting.RESET));
        line.append(Component.translatable(descriptionKey).withStyle(ChatFormatting.RESET));
        return line;
    }

    private Component runtimeModeValue() {
        return Component.translatable(runInBackground
            ? "permaclicker.config.runtime_mode.background"
            : "permaclicker.config.runtime_mode.focused");
    }

    private Component overlayColorValue() {
        ChatFormatting color = resolveOverlayColor(overlayColor);
        String label = overlayColor.replace('_', ' ');
        label = label.isEmpty()
            ? PermaClickConfig.DEFAULT_OVERLAY_COLOR
            : Character.toUpperCase(label.charAt(0)) + label.substring(1).toLowerCase(Locale.ROOT);
        return Component.literal(label).withStyle(color);
    }

    private Component updateChannelValue() {
        String channelKey = switch (updateChannel) {
            case ALPHA -> "permaclicker.config.update.channel.alpha";
            case STABLE -> "permaclicker.config.update.channel.stable";
            case BETA -> "permaclicker.config.update.channel.beta";
        };

        return updateCheckEnabled
            ? Component.translatable(channelKey)
            : Component.translatable("permaclicker.config.update.channel.disabled");
    }

    private static Component toggleValue(boolean value) {
        return Component.translatable(value ? "permaclicker.config.value.on" : "permaclicker.config.value.off");
    }

    private abstract class Row {
        private final Component label;
        private final String tooltipKey;
        private final int height;

        Row(Component label, String tooltipKey, int height) {
            this.label = label;
            this.tooltipKey = tooltipKey;
            this.height = height;
        }

        int height() {
            return this.height;
        }

        void drawLabel(GuiGraphics guiGraphics, int panelLeft, int panelRight, int y, int h, int mouseX, int mouseY) {
            drawLabelImpl(guiGraphics, panelLeft, panelRight, y, h, this.label);

            if (this.tooltipKey == null) {
                return;
            }

            int iconX = panelRight - CONTROL_WIDTH - INFO_ICON_GAP - INFO_ICON_SIZE;
            int iconY = y + (h - INFO_ICON_SIZE) / 2;
            guiGraphics.fill(iconX, iconY, iconX + INFO_ICON_SIZE, iconY + INFO_ICON_SIZE, INFO_ICON_BG_COLOR);
            guiGraphics.drawString(
                PermaClickFabricConfigScreen.this.font,
                "i",
                iconX + 3,
                iconY + 1,
                INFO_ICON_TEXT_COLOR,
                false
            );

            boolean hovered = mouseX >= iconX
                && mouseX < iconX + INFO_ICON_SIZE
                && mouseY >= iconY
                && mouseY < iconY + INFO_ICON_SIZE;
            if (hovered) {
                PermaClickFabricConfigScreen.this.hoveredTooltipLines = buildTooltipLines(this.tooltipKey);
            }
        }

        abstract void drawLabelImpl(GuiGraphics guiGraphics, int panelLeft, int panelRight, int y, int h, Component text);

        abstract void layout(int controlX, int controlY, boolean visible);

        void refresh() {
        }
    }

    private final class SectionRow extends Row {
        SectionRow(Component label) {
            super(label, null, 22);
        }

        @Override
        void drawLabelImpl(GuiGraphics guiGraphics, int panelLeft, int panelRight, int y, int h, Component text) {
            int centerX = (panelLeft + panelRight) / 2;
            int textX = centerX - PermaClickFabricConfigScreen.this.font.width(text) / 2;
            guiGraphics.drawString(PermaClickFabricConfigScreen.this.font, text, textX, y + (h - 9) / 2, 0xFFE7E7E7, true);
        }

        @Override
        void layout(int controlX, int controlY, boolean visible) {
        }
    }

    private class ActionButtonRow extends Row {
        private final Supplier<Component> messageSupplier;
        private final Consumer<Button> clickHandler;
        private final Supplier<Boolean> activeSupplier;
        private final Button button;

        ActionButtonRow(
            Component label,
            String tooltipKey,
            Supplier<Component> messageSupplier,
            Consumer<Button> clickHandler,
            Supplier<Boolean> activeSupplier
        ) {
            super(label, tooltipKey, 24);
            this.messageSupplier = messageSupplier;
            this.clickHandler = clickHandler;
            this.activeSupplier = activeSupplier;
            this.button = PermaClickFabricConfigScreen.this.addRenderableWidget(
                Button.builder(messageSupplier.get(), btn -> {
                        this.clickHandler.accept(btn);
                        updateRowsAndLayout();
                    })
                    .bounds(0, 0, CONTROL_WIDTH, 20)
                    .build()
            );
        }

        @Override
        void drawLabelImpl(GuiGraphics guiGraphics, int panelLeft, int panelRight, int y, int h, Component text) {
            guiGraphics.drawString(PermaClickFabricConfigScreen.this.font, text, panelLeft, y + (h - 9) / 2, 0xFFFFFFFF, true);
        }

        @Override
        void layout(int controlX, int controlY, boolean visible) {
            this.button.visible = visible;
            this.button.active = visible && this.activeSupplier.get();
            this.button.setPosition(controlX, controlY);
        }

        @Override
        void refresh() {
            this.button.setMessage(this.messageSupplier.get());
            this.button.active = this.activeSupplier.get();
        }
    }

    private final class ToggleRow extends ActionButtonRow {
        ToggleRow(Component label, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(
                label,
                tooltipKey,
                () -> toggleValue(getter.getAsBoolean()),
                btn -> {
                    setter.accept(!getter.getAsBoolean());
                    updateRowsAndLayout();
                },
                () -> true
            );
        }
    }

    private final class AutoStopRow extends Row {
        AutoStopRow(Component label, String tooltipKey) {
            super(label, tooltipKey, 24);
            autoStopInputField = PermaClickFabricConfigScreen.this.addRenderableWidget(new EditBox(
                PermaClickFabricConfigScreen.this.font,
                0,
                0,
                CONTROL_WIDTH,
                20,
                Component.translatable("permaclicker.config.auto_stop_minutes")
            ));
            autoStopInputField.setMaxLength(4);
            autoStopInputField.setValue(Integer.toString(autoStopMinutes));
        }

        @Override
        void drawLabelImpl(GuiGraphics guiGraphics, int panelLeft, int panelRight, int y, int h, Component text) {
            guiGraphics.drawString(PermaClickFabricConfigScreen.this.font, text, panelLeft, y + (h - 9) / 2, 0xFFFFFFFF, true);
        }

        @Override
        void layout(int controlX, int controlY, boolean visible) {
            autoStopInputField.visible = visible;
            autoStopInputField.active = visible;
            autoStopInputField.setPosition(controlX, controlY);
        }
    }

    private void saveAndClose() {
        FabricPermaClickBridge bridge = PermaClickFabricEntrypoint.bridge();
        int parsedAutoStop = autoStopMinutes;
        if (autoStopInputField != null) {
            parsedAutoStop = parseAutoStopInput(autoStopInputField.getValue(), autoStopMinutes);
        }

        PermaClickConfig updated = new PermaClickConfig(
            bridge.currentConfig().enabled(),
            bridge.currentConfig().toggleKeyCode(),
            overlayEnabled,
            overlayColor,
            runInBackground,
            runInBackground,
            PermaClickConfig.clampAutoStopMinutes(parsedAutoStop),
            movementLockEnabled,
            updateCheckEnabled,
            updateChannel
        );

        bridge.applyConfig(updated);
        try {
            new FabricConfigLifecycle().saveCurrent(bridge, FabricLoader.getInstance().getGameDir());
        } catch (IOException ignored) {
            // Best-effort persistence for UI flow.
        }

        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private static int parseAutoStopInput(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static UpdateChannel nextChannel(UpdateChannel current) {
        return switch (current) {
            case STABLE -> UpdateChannel.BETA;
            case BETA -> UpdateChannel.ALPHA;
            case ALPHA -> UpdateChannel.STABLE;
        };
    }

    private static String nextOverlayColor(String current) {
        String normalized = PermaClickConfig.normalizeOverlayColor(current);
        int currentIndex = OVERLAY_COLORS.indexOf(normalized);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % OVERLAY_COLORS.size();
        return OVERLAY_COLORS.get(nextIndex);
    }

    private static ChatFormatting resolveOverlayColor(String rawColorName) {
        ChatFormatting parsed = ChatFormatting.getByName(PermaClickConfig.normalizeOverlayColor(rawColorName));
        if (parsed == null || !parsed.isColor()) {
            return ChatFormatting.GREEN;
        }
        return parsed;
    }
}
