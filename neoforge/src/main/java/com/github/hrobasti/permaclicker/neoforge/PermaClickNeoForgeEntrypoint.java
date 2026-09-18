package com.github.hrobasti.permaclicker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientEventLoop;
import com.github.hrobasti.permaclicker.common.core.PermaClickRuntimeBindings;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
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
import com.github.hrobasti.permaclicker.client.ReflectionCompat;
import com.github.hrobasti.permaclicker.client.UpdateNoticeController;

/**
 * NeoForge entrypoint and client event wiring for PermaClick.
 */
@Mod("permaclicker")
public final class PermaClickNeoForgeEntrypoint {
    private static final String KEY_CATEGORY = "permaclicker";
    private static final String KEY_TRANSLATION = "key.permaclicker.toggle";
    private static final String CONFIG_KEY_TRANSLATION = "key.permaclicker.config";
    private static volatile Object cachedKeyCategory;

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
            () -> (modContainer, parent) -> {
                Screen configScreen = createConfigScreenCompat(parent);
                return configScreen != null ? configScreen : parent;
            }
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
                return MINING_STATE.performHeldAttackTick(minecraft);
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
        toggleKeyMapping = createKeyMapping(KEY_TRANSLATION, PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE);
        configKeyMapping = createKeyMapping(CONFIG_KEY_TRANSLATION, InputConstants.KEY_U);
        keyWasDown = false;
        event.register(toggleKeyMapping);
        event.register(configKeyMapping);
    }

    private static KeyMapping createKeyMapping(String translationKey, int defaultKeyCode) {
        try {
            Object category = getOrCreateKeyCategory();
            if (!(category instanceof KeyMapping.Category typedCategory)) {
                throw new IllegalStateException("Resolved key category is not a KeyMapping.Category: " + category);
            }
            return new KeyMapping(translationKey, defaultKeyCode, typedCategory);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Unable to resolve NeoForge key category for PermaClicker ("
                    + ex.getClass().getSimpleName()
                    + ": "
                    + ex.getMessage()
                    + ")",
                ex
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to create NeoForge key mapping for PermaClicker", ex);
        }
    }

    private static Object getOrCreateKeyCategory() throws ReflectiveOperationException {
        Object cached = cachedKeyCategory;
        if (cached != null) {
            return cached;
        }

        synchronized (PermaClickNeoForgeEntrypoint.class) {
            if (cachedKeyCategory != null) {
                return cachedKeyCategory;
            }

            Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
            Object resolved = resolveOrCreateKeyCategory(categoryClass);
            cachedKeyCategory = resolved;
            return resolved;
        }
    }

    private static Object resolveOrCreateKeyCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        try {
            Method register = ReflectionCompat.getAccessibleMethod(categoryClass, "register", String.class);
            return register.invoke(null, KEY_CATEGORY);
        } catch (NoSuchMethodException ignored) {
            Object categoryId = null;

            try {
                categoryId = createCategoryResourceId();
            } catch (ReflectiveOperationException ignoredResourceIdCreation) {
                Method createId = ReflectionCompat.getAccessibleMethod(categoryClass, "createId", String.class);
                categoryId = createId.invoke(null, KEY_CATEGORY);
            }

            try {
                Method registerById = ReflectionCompat.getAccessibleMethod(categoryClass, "register", categoryId.getClass());
                return registerById.invoke(null, categoryId);
            } catch (NoSuchMethodException ignoredRegisterById) {
                return categoryId;
            }
        }
    }

    private static Object createCategoryResourceId() throws ReflectiveOperationException {
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");

        try {
            Method fromNamespaceAndPath = ReflectionCompat.getAccessibleMethod(resourceLocationClass, "fromNamespaceAndPath", String.class, String.class);
            return fromNamespaceAndPath.invoke(null, "permaclicker", KEY_CATEGORY);
        } catch (NoSuchMethodException ignored) {
            Method parse = ReflectionCompat.getAccessibleMethod(resourceLocationClass, "parse", String.class);
            return parse.invoke(null, "permaclicker:" + KEY_CATEGORY);
        }
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
                    Screen configScreen = createConfigScreenCompat(minecraft.gui.screen());
                    if (configScreen != null) {
                        minecraft.execute(() -> minecraft.setScreenAndShow(configScreen));
                    }
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
        return screen != null
            && "com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeConfigScreen".equals(screen.getClass().getName());
    }

    public static boolean shouldBlockSettingsOpenForBackgroundMode(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedSettingsOpen(Minecraft minecraft) {
        stopForFocusedEscPause(minecraft);
    }

    private static Screen createConfigScreenCompat(Screen parent) {
        try {
            Class<?> screenClass = Class.forName("com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeConfigScreen");
            if (!Screen.class.isAssignableFrom(screenClass)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Class<? extends Screen> typedScreenClass = (Class<? extends Screen>) screenClass;

            try {
                return typedScreenClass.getConstructor(Screen.class).newInstance(parent);
            } catch (Throwable primaryFailure) {
                // try broader constructor fallbacks below
            }

            for (Constructor<?> constructor : typedScreenClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 0) {
                    try {
                        Object instance = constructor.newInstance();
                        if (instance instanceof Screen screen) {
                            return screen;
                        }
                    } catch (Throwable ctorFailure) {
                        // try next constructor
                    }
                }

                if (parameterTypes.length == 1 && Screen.class.isAssignableFrom(parameterTypes[0])) {
                    try {
                        Object instance = constructor.newInstance(parent);
                        if (instance instanceof Screen screen) {
                            return screen;
                        }
                    } catch (Throwable ctorFailure) {
                        // try next constructor
                    }
                }
            }
        } catch (Throwable loadFailure) {
            // no compatible config screen class available for this runtime
        }

        return null;
    }
}
