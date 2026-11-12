package net.kroet.permaclicker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kroet.permaclicker.common.config.ClickMode;
import net.kroet.permaclicker.common.config.PermaClickConfig;
import net.kroet.permaclicker.common.config.UpdateChannel;
import net.kroet.permaclicker.common.core.PermaClickBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * PermaClicker config screen with vanilla options-layout styling.
 */
public final class PermaClickConfigScreen extends OptionsSubScreen {
    private static final int ROW_HEIGHT = 20;
    private static final int LABEL_WIDTH = 150;
    private static final int CONTROL_WIDTH = 150;
    private static final int INFO_ICON_SIZE = 10;
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
            "white");

    private final @Nullable Screen parent;

    private boolean overlayEnabled;
    private String overlayColor;
    private boolean runInBackground;
    private ClickMode clickMode;
    private int attackBufferTicks;
    private boolean hitArmorStands;
    private int autoStopMinutes;
    private boolean movementLockEnabled;
    private boolean updateCheckEnabled;
    private UpdateChannel updateChannel;

    private @Nullable Button runtimeModeButton;
    private @Nullable Button clickModeButton;
    private @Nullable Button hitArmorStandsButton;
    private @Nullable Button movementLockButton;
    private @Nullable Button overlayEnabledButton;
    private @Nullable Button overlayColorButton;
    private @Nullable Button updateCheckButton;
    private @Nullable Button updateChannelButton;
    private @Nullable EditBox autoStopInputField;
    private @Nullable EditBox attackBufferInputField;
    private @Nullable OptionsList optionsList;

    public PermaClickConfigScreen(@Nullable Screen parent) {
        super(uncheckedParent(parent), Minecraft.getInstance().options, Component.translatable("permaclicker.config.title"));
        this.parent = parent;

        PermaClickConfig config = PermaClickClient.bridge().currentConfig();

        this.overlayEnabled = config.overlayEnabled();
        this.overlayColor = config.overlayColor();
        this.runInBackground = config.runInBackground();
        this.clickMode = config.clickMode();
        this.attackBufferTicks = config.attackBufferTicks();
        this.hitArmorStands = config.hitArmorStands();
        this.autoStopMinutes = config.autoStopMinutes();
        this.movementLockEnabled = config.movementLockEnabled();
        this.updateCheckEnabled = config.updateCheckEnabled();
        this.updateChannel = config.updateChannel();
    }

    @Override
    protected void addContents() {
        this.optionsList = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this));
        this.list = this.optionsList;
        addOptions();
    }

    @Override
    protected void addOptions() {
        if (this.optionsList == null) {
            return;
        }

        addSectionRow(Component.translatable("permaclicker.config.section.general"));
        Button runtimeMode = this.createToggleButton(runtimeModeValue(), button -> {
            runInBackground = !runInBackground;
            refreshButtonMessages();
        });
        this.runtimeModeButton = runtimeMode;
        addLabeledRow(
                Component.translatable("permaclicker.config.runtime_mode"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.runtime_mode.focused"), "permaclicker.config.runtime_mode.tooltip.focused.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.runtime_mode.background"), "permaclicker.config.runtime_mode.tooltip.background.desc")),
                runtimeMode);

        Button clickModeToggle = this.createToggleButton(clickModeValue(), button -> {
            clickMode = clickMode == ClickMode.MOB_ATTACK ? ClickMode.MINING : ClickMode.MOB_ATTACK;
            refreshButtonMessages();
        });
        this.clickModeButton = clickModeToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.click_mode"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.click_mode.mining"), "permaclicker.config.click_mode.tooltip.mining.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.click_mode.mob_attack"), "permaclicker.config.click_mode.tooltip.mob_attack.desc")),
                clickModeToggle);

        EditBox attackBufferInput = new EditBox(
                this.font,
                0,
                0,
                CONTROL_WIDTH,
                ROW_HEIGHT,
                Component.translatable("permaclicker.config.attack_buffer_ticks"));
        this.attackBufferInputField = attackBufferInput;
        attackBufferInput.setMaxLength(2);
        attackBufferInput.setValue(Integer.toString(attackBufferTicks));
        addLabeledRow(
                Component.translatable("permaclicker.config.attack_buffer_ticks"),
                tooltipLines(
                        tooltipLine(
                                Component.literal(PermaClickConfig.MIN_ATTACK_BUFFER_TICKS + "-" + PermaClickConfig.MAX_ATTACK_BUFFER_TICKS),
                                "permaclicker.config.attack_buffer_ticks.tooltip.range.desc")),
                attackBufferInput);

        Button hitArmorStandsToggle = this.createToggleButton(toggleValue(hitArmorStands), button -> {
            hitArmorStands = !hitArmorStands;
            refreshButtonMessages();
        });
        this.hitArmorStandsButton = hitArmorStandsToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.hit_armor_stands"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.hit_armor_stands.tooltip.on.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.hit_armor_stands.tooltip.off.desc")),
                hitArmorStandsToggle);

        EditBox autoStopInput = new EditBox(
                this.font,
                0,
                0,
                CONTROL_WIDTH,
                ROW_HEIGHT,
                Component.translatable("permaclicker.config.auto_stop_minutes"));
        this.autoStopInputField = autoStopInput;
        autoStopInput.setMaxLength(4);
        autoStopInput.setValue(Integer.toString(autoStopMinutes));
        addLabeledRow(
                Component.translatable("permaclicker.config.auto_stop_minutes"),
                tooltipLines(
                        tooltipLine(Component.literal("0"), "permaclicker.config.auto_stop_minutes.tooltip.off.desc"),
                        tooltipLine(Component.literal("1-9999"), "permaclicker.config.auto_stop_minutes.tooltip.range.desc")),
                autoStopInput);

        Button movementLockToggle = this.createToggleButton(toggleValue(movementLockEnabled), button -> {
            movementLockEnabled = !movementLockEnabled;
            refreshButtonMessages();
        });
        this.movementLockButton = movementLockToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.cursor_lock"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.cursor_lock.tooltip.on.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.cursor_lock.tooltip.off.desc")),
                movementLockToggle);

        addSectionRow(Component.translatable("permaclicker.config.section.overlay"));
        Button overlayEnabledToggle = this.createToggleButton(toggleValue(overlayEnabled), button -> {
            overlayEnabled = !overlayEnabled;
            refreshButtonMessages();
        });
        this.overlayEnabledButton = overlayEnabledToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.overlay.enabled"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.overlay.enabled.tooltip.on.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.overlay.enabled.tooltip.off.desc")),
                overlayEnabledToggle);

        Button overlayColorToggle = this.createToggleButton(overlayColorValue(), button -> {
            overlayColor = nextOverlayColor(overlayColor);
            refreshButtonMessages();
        });
        this.overlayColorButton = overlayColorToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.overlay.color"),
                tooltipLines(
                        tooltipLine(
                                Component.translatable("permaclicker.config.overlay.color.tooltip.any.label"),
                                "permaclicker.config.overlay.color.tooltip.any.desc")),
                overlayColorToggle);

        addSectionRow(Component.translatable("permaclicker.config.section.updates"));
        Button updateCheckToggle = this.createToggleButton(toggleValue(updateCheckEnabled), button -> {
            updateCheckEnabled = !updateCheckEnabled;
            refreshButtonMessages();
        });
        this.updateCheckButton = updateCheckToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.update.enabled"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.value.on"), "permaclicker.config.update.enabled.tooltip.on.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.value.off"), "permaclicker.config.update.enabled.tooltip.off.desc")),
                updateCheckToggle);

        Button updateChannelToggle = this.createToggleButton(updateChannelValue(), button -> {
            updateChannel = nextChannel(updateChannel);
            refreshButtonMessages();
        });
        this.updateChannelButton = updateChannelToggle;
        addLabeledRow(
                Component.translatable("permaclicker.config.update.channel"),
                tooltipLines(
                        tooltipLine(Component.translatable("permaclicker.config.update.channel.stable"), "permaclicker.config.update.channel.tooltip.stable.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.update.channel.beta"), "permaclicker.config.update.channel.tooltip.beta.desc"),
                        tooltipLine(Component.translatable("permaclicker.config.update.channel.alpha"), "permaclicker.config.update.channel.tooltip.alpha.desc")),
                updateChannelToggle);

        refreshButtonMessages();
    }

    @Override
    protected void addFooter() {
        LinearLayout footerButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footerButtons.addChild(
                Button.builder(Component.translatable("permaclicker.config.button.cancel"), btn -> onClose())
                        .width(150)
                        .build());
        footerButtons.addChild(
                Button.builder(Component.translatable("permaclicker.config.button.save_close"), btn -> saveAndClose())
                        .width(150)
                        .build());
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
    }

    private void refreshButtonMessages() {
        Button runtimeMode = runtimeModeButton;
        if (runtimeMode != null) {
            runtimeMode.setMessage(runtimeModeValue());
        }
        Button clickModeToggle = clickModeButton;
        if (clickModeToggle != null) {
            clickModeToggle.setMessage(clickModeValue());
        }
        // Buffer and armor stands only affect mob-attack mode; grey them out while
        // mining.
        boolean mobAttackMode = clickMode == ClickMode.MOB_ATTACK;
        EditBox attackBufferInput = attackBufferInputField;
        if (attackBufferInput != null) {
            attackBufferInput.active = mobAttackMode;
            attackBufferInput.setEditable(mobAttackMode);
        }
        Button hitArmorStandsToggle = hitArmorStandsButton;
        if (hitArmorStandsToggle != null) {
            hitArmorStandsToggle.setMessage(toggleValue(hitArmorStands));
            hitArmorStandsToggle.active = mobAttackMode;
        }
        Button movementLockToggle = movementLockButton;
        if (movementLockToggle != null) {
            movementLockToggle.setMessage(toggleValue(movementLockEnabled));
        }
        Button overlayEnabledToggle = overlayEnabledButton;
        if (overlayEnabledToggle != null) {
            overlayEnabledToggle.setMessage(toggleValue(overlayEnabled));
        }
        Button overlayColorToggle = overlayColorButton;
        if (overlayColorToggle != null) {
            overlayColorToggle.setMessage(overlayColorValue());
        }
        Button updateCheckToggle = updateCheckButton;
        if (updateCheckToggle != null) {
            updateCheckToggle.setMessage(toggleValue(updateCheckEnabled));
        }
        Button updateChannelToggle = updateChannelButton;
        if (updateChannelToggle != null) {
            updateChannelToggle.setMessage(updateChannelValue());
            updateChannelToggle.active = updateCheckEnabled;
        }
    }

    private Button createToggleButton(Component initialText, Button.OnPress onPress) {
        return Button.builder(initialText, onPress)
                .bounds(0, 0, CONTROL_WIDTH, ROW_HEIGHT)
                .build();
    }

    private void addLabeledRow(Component labelText, List<Component> infoTooltipLines, AbstractWidget controlWidget) {
        InfoLabelWidget labelWidget = new InfoLabelWidget(labelText, infoTooltipLines);
        controlWidget.setWidth(CONTROL_WIDTH);
        Objects.requireNonNull(this.optionsList, "optionsList is created in addContents").addSmall(labelWidget, controlWidget);
    }

    private void addSectionRow(Component sectionText) {
        SectionLabelWidget left = new SectionLabelWidget(sectionText);
        SpacerWidget right = new SpacerWidget();
        Objects.requireNonNull(this.optionsList, "optionsList is created in addContents").addSmall(left, right);
    }

    private static List<Component> tooltipLines(Component... lines) {
        List<Component> result = new ArrayList<>();
        for (Component line : lines) {
            if (line == null || line.getString().isBlank()) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    private static Component tooltipLine(Component value, String descriptionKey) {
        MutableComponent line = Component.empty();
        line.append(value.copy().withStyle(ChatFormatting.BOLD));
        line.append(Component.literal(": "));
        line.append(Component.translatable(descriptionKey));
        return line;
    }

    private final class InfoLabelWidget extends AbstractWidget {
        private final List<Component> infoTooltipLines;

        InfoLabelWidget(Component label, List<Component> infoTooltipLines) {
            super(0, 0, LABEL_WIDTH, ROW_HEIGHT, label);
            this.infoTooltipLines = infoTooltipLines == null ? List.of() : infoTooltipLines;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            int textY = this.getY() + (this.height - 9) / 2;
            guiGraphics.text(PermaClickConfigScreen.this.font, this.getMessage(), this.getX(), textY, 0xFFFFFFFF, true);

            int iconX = this.getX() + this.width - INFO_ICON_SIZE;
            int iconY = this.getY() + (this.height - INFO_ICON_SIZE) / 2;
            guiGraphics.fill(iconX, iconY, iconX + INFO_ICON_SIZE, iconY + INFO_ICON_SIZE, INFO_ICON_BG_COLOR);
            guiGraphics.text(PermaClickConfigScreen.this.font, Component.literal("i"), iconX + 3, iconY + 1, INFO_ICON_TEXT_COLOR, false);

            boolean hoveredIcon = mouseX >= iconX && mouseX < iconX + INFO_ICON_SIZE
                    && mouseY >= iconY && mouseY < iconY + INFO_ICON_SIZE;
            if (hoveredIcon && !this.infoTooltipLines.isEmpty()) {
                guiGraphics.setComponentTooltipForNextFrame(
                        PermaClickConfigScreen.this.font,
                        this.infoTooltipLines,
                        mouseX,
                        mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }

    private final class SectionLabelWidget extends AbstractWidget {
        SectionLabelWidget(Component label) {
            super(0, 0, LABEL_WIDTH, ROW_HEIGHT, label);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            int centeredX = PermaClickConfigScreen.this.width / 2 - PermaClickConfigScreen.this.font.width(this.getMessage()) / 2;
            int textY = this.getY() + (this.height - 9) / 2;
            guiGraphics.text(PermaClickConfigScreen.this.font, this.getMessage(), centeredX, textY, 0xFFE7E7E7, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }

    private static final class SpacerWidget extends AbstractWidget {
        SpacerWidget() {
            super(0, 0, CONTROL_WIDTH, ROW_HEIGHT, Component.empty());
            this.active = false;
            this.visible = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private Component runtimeModeValue() {
        return Component.translatable(runInBackground
                ? "permaclicker.config.runtime_mode.background"
                : "permaclicker.config.runtime_mode.focused");
    }

    private Component clickModeValue() {
        return Component.translatable(clickMode == ClickMode.MOB_ATTACK
                ? "permaclicker.config.click_mode.mob_attack"
                : "permaclicker.config.click_mode.mining");
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

    private void saveAndClose() {
        PermaClickBridge bridge = PermaClickClient.bridge();
        PermaClickConfig current = bridge.currentConfig();

        int parsedAutoStop = autoStopMinutes;
        if (autoStopInputField != null) {
            parsedAutoStop = parseIntInput(autoStopInputField.getValue(), autoStopMinutes);
        }
        int parsedAttackBuffer = attackBufferTicks;
        if (attackBufferInputField != null) {
            parsedAttackBuffer = parseIntInput(attackBufferInputField.getValue(), attackBufferTicks);
        }

        PermaClickConfig updated = new PermaClickConfig(
                current.enabled(),
                overlayEnabled,
                overlayColor,
                runInBackground,
                PermaClickConfig.clampAutoStopMinutes(parsedAutoStop),
                movementLockEnabled,
                updateCheckEnabled,
                updateChannel,
                clickMode,
                PermaClickConfig.clampAttackBufferTicks(parsedAttackBuffer),
                hitArmorStands);

        bridge.applyConfig(updated);
        PermaClickClient.saveConfig();

        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            PermaClickGameContext.setScreenAndShow(this.minecraft, parent);
        }
    }

    // The in-game hotkey opens this screen without a parent; OptionsSubScreen
    // declares its parent non-null but only hands it to the nullable Gui.setScreen.
    @SuppressWarnings("null")
    private static Screen uncheckedParent(@Nullable Screen parent) {
        return parent;
    }

    private static int parseIntInput(String raw, int fallback) {
        if (raw.isBlank()) {
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
        String normalized = PermaClickConfig.normalizeOverlayColor(rawColorName);
        try {
            return ChatFormatting.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChatFormatting.GREEN;
        }
    }
}
