package com.github.hrobasti.permaclicker.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientEventLoop;
import com.github.hrobasti.permaclicker.common.core.PermaClickRuntimeBindings;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.github.hrobasti.permaclicker.client.MiningRuntimeState;
import com.github.hrobasti.permaclicker.client.MovementLockController;
import com.github.hrobasti.permaclicker.client.PauseOnLostFocusOverride;
import com.github.hrobasti.permaclicker.client.PermaClickGameContext;
import com.github.hrobasti.permaclicker.client.PermaClickKeyMappings;
import com.github.hrobasti.permaclicker.client.UpdateNoticeController;

/**
 * Fabric entrypoint and client event wiring for PermaClick.
 */
public final class PermaClickFabricEntrypoint implements ClientModInitializer {
    private static final PermaClickClientController CONTROLLER = new PermaClickClientController();
    private static final PermaClickBridge BRIDGE = new PermaClickBridge(CONTROLLER);
    private static final PermaClickClientEventLoop EVENT_LOOP = new PermaClickClientEventLoop(BRIDGE);
    private static final FabricConfigLifecycle CONFIG_LIFECYCLE = new FabricConfigLifecycle();
    private static final MiningRuntimeState MINING_STATE = new MiningRuntimeState();
    private static final MovementLockController MOVEMENT_LOCK = new MovementLockController();
    private static final PauseOnLostFocusOverride PAUSE_OVERRIDE = new PauseOnLostFocusOverride();
    private static final UpdateNoticeController UPDATE_NOTICE = new UpdateNoticeController();

    private static PermaClickFabricEntrypoint instance;

    private KeyMapping toggleKeyMapping;
    private KeyMapping configKeyMapping;
    private boolean keyWasDown;

    @Override
    public void onInitializeClient() {
        instance = this;
        BRIDGE.bind(createBindings());
        PermaClickConfigStore.LoadResult configLoadResult = CONFIG_LIFECYCLE.loadAndApply(BRIDGE, FabricLoader.getInstance().getGameDir());
        if (configLoadResult.toggleKeyWasReset()) {
            UPDATE_NOTICE.markToggleKeyReset();
        }
        UPDATE_NOTICE.scheduleUpdateCheck(BRIDGE);
        KeyMapping.Category category = KeyMapping.Category.register(PermaClickKeyMappings.CATEGORY_ID);
        toggleKeyMapping = KeyMappingHelper.registerKeyMapping(PermaClickKeyMappings.createToggle(category));
        configKeyMapping = KeyMappingHelper.registerKeyMapping(PermaClickKeyMappings.createConfig(category));
        keyWasDown = false;

        ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTickPost);
        try {
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnect());
        } catch (Throwable ignored) {
            // Keep core functionality active even if this optional hook differs across runtime variants.
        }
    }

    private PermaClickRuntimeBindings createBindings() {
        return new PermaClickRuntimeBindings(
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return minecraft != null && minecraft.isWindowActive();
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return PermaClickGameContext.isWindowMinimized(minecraft);
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return minecraft != null
                    && minecraft.player != null
                    && minecraft.gameMode != null
                    && PermaClickGameContext.isMiningAllowedScreen(minecraft)
                    && !MINING_STATE.isBackgroundPauseSuppressedThisTick();
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return MINING_STATE.performClickTick(minecraft, BRIDGE.currentConfig());
            },
            payload -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.player != null) {
                    ChatFormatting overlayColor = PermaClickGameContext.resolveOverlayColor(payload.colorName());
                    Component overlayMessage = Component.literal(payload.message()).withStyle(overlayColor);
                    // 26.2: action-bar text is LocalPlayer.sendOverlayMessage(Component). Call it directly
                    // so the loader toolchain remaps it; reflection by Mojang name is unreliable on Fabric.
                    minecraft.player.sendOverlayMessage(overlayMessage);
                }
            },
            active -> MOVEMENT_LOCK.setActive(Minecraft.getInstance(), active),
            active -> MINING_STATE.setBackgroundCursorFreeActive(Minecraft.getInstance(), active, PAUSE_OVERRIDE)
        );
    }

    private void onClientTick(Minecraft client) {
        UPDATE_NOTICE.flushPendingUpdateMessage(client);
        UPDATE_NOTICE.flushPendingToggleKeyResetNotice(client);
        MINING_STATE.resetTickFlags();
        boolean inGameHotkeyContext = PermaClickGameContext.isInGameHotkeyContext(client);

        boolean keyDown = toggleKeyMapping.isDown();
        if (inGameHotkeyContext && keyDown != keyWasDown) {
            EVENT_LOOP.onKeyInput(BRIDGE.boundKeyCode(), keyDown);
        }
        keyWasDown = keyDown;

        if (inGameHotkeyContext) {
            while (configKeyMapping.consumeClick()) {
                openConfigScreen(client);
            }
        }

        MINING_STATE.suppressBackgroundPauseScreen(client);
        MINING_STATE.markAttackNotRequestedYet();
        EVENT_LOOP.onClientTick();
        MINING_STATE.syncAttackHoldState(client);
        MOVEMENT_LOCK.applyInputSuppression(client);
    }

    private void onClientTickPost(Minecraft client) {
        MINING_STATE.suppressBackgroundPauseScreen(client);
        MOVEMENT_LOCK.applyViewFreeze(client);
    }

    private void onClientDisconnect() {
        BRIDGE.onClientShutdown();
        MINING_STATE.resetOnShutdown();
        MOVEMENT_LOCK.setActive(Minecraft.getInstance(), false);
        PAUSE_OVERRIDE.restore(Minecraft.getInstance());
        try {
            CONFIG_LIFECYCLE.saveCurrent(BRIDGE, FabricLoader.getInstance().getGameDir());
        } catch (IOException ignored) {
            // Best-effort config persistence; runtime should continue safely.
        }
    }

    public static void updateToggleKeyCode(int keyCode) {
        BRIDGE.setBoundKeyCode(keyCode);

        if (instance != null && instance.toggleKeyMapping != null) {
            instance.toggleKeyMapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
        }
    }

    public static PermaClickClientController controller() {
        return CONTROLLER;
    }

    public static PermaClickBridge bridge() {
        return BRIDGE;
    }

    public static PermaClickClientEventLoop eventLoop() {
        return EVENT_LOOP;
    }

    public static boolean isPauseBlockContextActive(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static boolean isChatMiningContextActive(Minecraft minecraft) {
        return PermaClickGameContext.isChatMiningContextActive(minecraft, BRIDGE.currentConfig());
    }

    public static boolean isChatBlockContextActive(Minecraft minecraft) {
        return PermaClickGameContext.isChatBlockContextActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedEscPause(Minecraft minecraft) {
        PermaClickGameContext.stopForFocusedEscPause(minecraft, BRIDGE);
    }

    public static boolean isPermaClickerConfigScreen(Screen screen) {
        return screen instanceof PermaClickFabricConfigScreen;
    }

    public static boolean shouldBlockSettingsOpenForBackgroundMode(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedSettingsOpen(Minecraft minecraft) {
        stopForFocusedEscPause(minecraft);
    }

    private static void openConfigScreen(Minecraft client) {
        if (client != null) {
            client.setScreenAndShow(new PermaClickFabricConfigScreen(client.gui.screen()));
        }
    }
}
