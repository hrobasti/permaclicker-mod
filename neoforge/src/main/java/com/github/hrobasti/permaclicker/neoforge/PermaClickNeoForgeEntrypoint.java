package com.github.hrobasti.permaclicker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientEventLoop;
import com.github.hrobasti.permaclicker.common.core.PermaClickRuntimeBindings;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import com.github.hrobasti.permaclicker.client.MiningRuntimeState;
import com.github.hrobasti.permaclicker.client.MovementLockController;
import com.github.hrobasti.permaclicker.client.PauseOnLostFocusOverride;
import com.github.hrobasti.permaclicker.client.PermaClickGameContext;
import com.github.hrobasti.permaclicker.client.PermaClickKeyMappings;
import com.github.hrobasti.permaclicker.client.UpdateNoticeController;

/**
 * NeoForge entrypoint and client event wiring for PermaClick.
 */
@Mod("permaclicker")
public final class PermaClickNeoForgeEntrypoint {
    private static final PermaClickClientController CONTROLLER = new PermaClickClientController();
    private static final PermaClickBridge BRIDGE = new PermaClickBridge(CONTROLLER);
    private static final PermaClickClientEventLoop EVENT_LOOP = new PermaClickClientEventLoop(BRIDGE);
    private static final NeoForgeConfigLifecycle CONFIG_LIFECYCLE = new NeoForgeConfigLifecycle();
    private static final MiningRuntimeState MINING_STATE = new MiningRuntimeState();
    private static final MovementLockController MOVEMENT_LOCK = new MovementLockController();
    private static final PauseOnLostFocusOverride PAUSE_OVERRIDE = new PauseOnLostFocusOverride();
    private static final UpdateNoticeController UPDATE_NOTICE = new UpdateNoticeController();

    private static PermaClickNeoForgeEntrypoint instance;

    private KeyMapping toggleKeyMapping;
    private KeyMapping configKeyMapping;
    private boolean keyWasDown;

    public PermaClickNeoForgeEntrypoint(IEventBus modEventBus) {
        instance = this;
        BRIDGE.bind(createBindings());
        PermaClickConfigStore.LoadResult configLoadResult = CONFIG_LIFECYCLE.loadAndApply(BRIDGE, FMLPaths.GAMEDIR.get());
        if (configLoadResult.toggleKeyWasReset()) {
            UPDATE_NOTICE.markToggleKeyReset();
        }
        UPDATE_NOTICE.scheduleUpdateCheck(BRIDGE);
        ModLoadingContext.get().registerExtensionPoint(
            IConfigScreenFactory.class,
            () -> (modContainer, parent) -> new PermaClickNeoForgeConfigScreen(parent)
        );

        modEventBus.addListener(this::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
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

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(PermaClickKeyMappings.CATEGORY_ID);
        event.registerCategory(category);
        toggleKeyMapping = PermaClickKeyMappings.createToggle(category);
        configKeyMapping = PermaClickKeyMappings.createConfig(category);
        keyWasDown = false;
        event.register(toggleKeyMapping);
        event.register(configKeyMapping);
    }

    public static void updateToggleKeyCode(int keyCode) {
        BRIDGE.setBoundKeyCode(keyCode);

        if (instance != null && instance.toggleKeyMapping != null) {
            instance.toggleKeyMapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
        }
    }

    private void onClientTick(ClientTickEvent.Pre event) {
        if (toggleKeyMapping == null) {
            return;
        }

        UPDATE_NOTICE.flushPendingUpdateMessage(Minecraft.getInstance());
        UPDATE_NOTICE.flushPendingToggleKeyResetNotice(Minecraft.getInstance());
        Minecraft minecraft = Minecraft.getInstance();
        MINING_STATE.resetTickFlags();
        boolean inGameHotkeyContext = PermaClickGameContext.isInGameHotkeyContext(minecraft);

        boolean keyDown = toggleKeyMapping.isDown();
        if (inGameHotkeyContext && keyDown != keyWasDown) {
            EVENT_LOOP.onKeyInput(BRIDGE.boundKeyCode(), keyDown);
        }
        keyWasDown = keyDown;

        if (inGameHotkeyContext && configKeyMapping != null) {
            while (configKeyMapping.consumeClick()) {
                if (minecraft != null) {
                    Screen configScreen = new PermaClickNeoForgeConfigScreen(minecraft.gui.screen());
                    minecraft.execute(() -> minecraft.setScreenAndShow(configScreen));
                }
            }
        }

        MINING_STATE.suppressBackgroundPauseScreen(minecraft);
        MINING_STATE.markAttackNotRequestedYet();
        EVENT_LOOP.onClientTick();
        MINING_STATE.syncAttackHoldState(minecraft);
        MOVEMENT_LOCK.applyInputSuppression(minecraft);
    }

    private void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        MINING_STATE.suppressBackgroundPauseScreen(minecraft);
        MOVEMENT_LOCK.applyViewFreeze(minecraft);
    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BRIDGE.onClientShutdown();
        MINING_STATE.resetOnShutdown();
        MOVEMENT_LOCK.setActive(Minecraft.getInstance(), false);
        PAUSE_OVERRIDE.restore(Minecraft.getInstance());
        try {
            CONFIG_LIFECYCLE.saveCurrent(BRIDGE, FMLPaths.GAMEDIR.get());
        } catch (IOException ignored) {
            // Best-effort config persistence; runtime should continue safely.
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
        return screen instanceof PermaClickNeoForgeConfigScreen;
    }

    public static boolean shouldBlockSettingsOpenForBackgroundMode(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedSettingsOpen(Minecraft minecraft) {
        stopForFocusedEscPause(minecraft);
    }
}
