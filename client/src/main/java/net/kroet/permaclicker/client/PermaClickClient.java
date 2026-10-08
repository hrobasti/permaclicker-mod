package net.kroet.permaclicker.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import net.kroet.permaclicker.common.config.ClickMode;
import net.kroet.permaclicker.common.config.PermaClickConfigStore;
import net.kroet.permaclicker.common.core.PermaClickBridge;
import net.kroet.permaclicker.common.core.PermaClickClientController;
import net.kroet.permaclicker.common.core.PermaClickClientEventLoop;
import net.kroet.permaclicker.common.core.PermaClickRuntimeBindings;
import net.kroet.permaclicker.common.core.PermaClickTextKeys;
import net.kroet.permaclicker.common.core.StatusOverlay;
import net.kroet.permaclicker.common.update.ModLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

/**
 * PermaClicker's client runtime, shared by both loaders. The loader entrypoints
 * only register the key mappings and forward their tick and disconnect events
 * here; the mixins in {@code client.mixin} ask it for the current context.
 */
public final class PermaClickClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PermaClickClientController CONTROLLER = new PermaClickClientController();
    private static final PermaClickBridge BRIDGE = new PermaClickBridge(CONTROLLER);
    private static final PermaClickClientEventLoop EVENT_LOOP = new PermaClickClientEventLoop(BRIDGE);
    private static final MiningRuntimeState MINING_STATE = new MiningRuntimeState();
    private static final MovementLockController MOVEMENT_LOCK = new MovementLockController();
    private static final PauseOnLostFocusOverride PAUSE_OVERRIDE = new PauseOnLostFocusOverride();
    private static final UpdateNoticeController UPDATE_NOTICE = new UpdateNoticeController();

    private static PermaClickConfigStore configStore;
    private static KeyMapping toggleKey;
    private static KeyMapping configKey;

    private PermaClickClient() {
    }

    /**
     * Loads the config and starts the update check; called once at client start.
     */
    public static void init(Path gameDirectory, ModLoader loader, String modVersion) {
        configStore = PermaClickConfigStore.inGameDirectory(gameDirectory);
        BRIDGE.bind(createBindings());
        PermaClickConfigStore.LoadResult result = configStore.load();
        if (result.brokenFileBackup() != null) {
            LOGGER.warn("PermaClicker config was unreadable and moved to {}; using defaults", result.brokenFileBackup());
        }
        BRIDGE.applyConfig(result.config());
        UPDATE_NOTICE.scheduleUpdateCheck(BRIDGE, loader, modVersion);
    }

    /** Hands over the key mappings after the loader registered them. */
    public static void setKeyMappings(KeyMapping toggle, KeyMapping config) {
        toggleKey = toggle;
        configKey = config;
    }

    public static void onClientTickStart(Minecraft minecraft) {
        UPDATE_NOTICE.flushPendingUpdateMessage(minecraft);
        MINING_STATE.resetTickFlags();
        boolean inGameHotkeyContext = PermaClickGameContext.isInGameHotkeyContext(minecraft);

        // Clicks are counted by Minecraft, so a press is never lost to a screen that
        // was open while the key was released.
        if (toggleKey != null) {
            while (toggleKey.consumeClick()) {
                if (inGameHotkeyContext) {
                    EVENT_LOOP.onTogglePressed();
                }
            }
        }
        if (configKey != null) {
            while (configKey.consumeClick()) {
                if (inGameHotkeyContext) {
                    openConfigScreen(minecraft);
                }
            }
        }

        MINING_STATE.suppressBackgroundPauseScreen(minecraft);
        MINING_STATE.markAttackNotRequestedYet();
        EVENT_LOOP.onClientTick();
        MINING_STATE.syncAttackHoldState(minecraft);
        MOVEMENT_LOCK.applyInputSuppression(minecraft);
    }

    public static void onClientTickEnd(Minecraft minecraft) {
        MINING_STATE.suppressBackgroundPauseScreen(minecraft);
        MOVEMENT_LOCK.applyViewFreeze(minecraft);
    }

    public static void onDisconnect() {
        BRIDGE.onClientShutdown();
        MINING_STATE.resetOnShutdown();
        MOVEMENT_LOCK.setActive(Minecraft.getInstance(), false);
        PAUSE_OVERRIDE.restore(Minecraft.getInstance());
        saveConfig();
    }

    public static PermaClickBridge bridge() {
        return BRIDGE;
    }

    public static void saveConfig() {
        if (configStore == null) {
            return;
        }
        try {
            configStore.save(BRIDGE.currentConfig());
        } catch (IOException e) {
            LOGGER.warn("Could not save the PermaClicker config", e);
        }
    }

    public static Screen createConfigScreen(Screen parent) {
        return new PermaClickConfigScreen(parent);
    }

    private static void openConfigScreen(Minecraft minecraft) {
        if (minecraft != null) {
            Screen screen = createConfigScreen(minecraft.gui.screen());
            minecraft.execute(() -> minecraft.setScreenAndShow(screen));
        }
    }

    public static boolean isPauseBlockContextActive(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static boolean isChatBlockContextActive(Minecraft minecraft) {
        return PermaClickGameContext.isChatBlockContextActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedEscPause(Minecraft minecraft) {
        PermaClickGameContext.stopForFocusedEscPause(minecraft, BRIDGE);
    }

    public static boolean isConfigScreen(Screen screen) {
        return screen instanceof PermaClickConfigScreen;
    }

    private static MutableComponent statusText(StatusOverlay status) {
        if (!status.active()) {
            return Component.translatable(PermaClickTextKeys.STATUS_OFF);
        }
        MutableComponent text = Component.translatable(PermaClickTextKeys.STATUS_ON)
                .append(" - ")
                .append(Component.translatable(status.clickMode() == ClickMode.MOB_ATTACK
                        ? PermaClickTextKeys.CLICK_MODE_MOB_ATTACK
                        : PermaClickTextKeys.CLICK_MODE_MINING));
        return status.timer() == null ? text : text.append(" (" + status.timer() + ")");
    }

    private static PermaClickRuntimeBindings createBindings() {
        return new PermaClickRuntimeBindings(
                () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    return minecraft != null && minecraft.isWindowActive();
                },
                () -> PermaClickGameContext.isWindowMinimized(Minecraft.getInstance()),
                () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    return minecraft != null
                            && minecraft.player != null
                            && minecraft.gameMode != null
                            && PermaClickGameContext.isMiningAllowedScreen(minecraft)
                            && !MINING_STATE.isBackgroundPauseSuppressedThisTick();
                },
                () -> MINING_STATE.performClickTick(Minecraft.getInstance(), BRIDGE.currentConfig()),
                payload -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft != null && minecraft.player != null) {
                        ChatFormatting overlayColor = PermaClickGameContext.resolveOverlayColor(payload.colorName());
                        minecraft.player.sendOverlayMessage(statusText(payload.status()).withStyle(overlayColor));
                    }
                },
                active -> MOVEMENT_LOCK.setActive(Minecraft.getInstance(), active),
                active -> MINING_STATE.setBackgroundCursorFreeActive(Minecraft.getInstance(), active, PAUSE_OVERRIDE));
    }
}
