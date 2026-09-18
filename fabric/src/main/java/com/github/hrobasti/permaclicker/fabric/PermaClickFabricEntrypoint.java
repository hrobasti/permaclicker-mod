package com.github.hrobasti.permaclicker.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientEventLoop;
import com.github.hrobasti.permaclicker.common.core.PermaClickRuntimeBindings;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
import com.github.hrobasti.permaclicker.client.ReflectionCompat;
import com.github.hrobasti.permaclicker.client.UpdateNoticeController;

/**
 * Fabric entrypoint and client event wiring for PermaClick.
 */
public final class PermaClickFabricEntrypoint implements ClientModInitializer {
    private static final String KEY_CATEGORY = "permaclicker";
    private static final String KEY_TRANSLATION = "key.permaclicker.toggle";
    private static final String CONFIG_KEY_TRANSLATION = "key.permaclicker.config";

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
    private boolean toggleKeyRegistered;
    private boolean configKeyRegistered;
    private boolean toggleKeyUnavailable;
    private boolean keyWasDown;
    private boolean configKeyWasDown;
    private boolean rawConfigKeyWasDown;

    @Override
    public void onInitializeClient() {
        instance = this;
        BRIDGE.bind(createBindings());
        PermaClickConfigStore.LoadResult configLoadResult = CONFIG_LIFECYCLE.loadAndApply(BRIDGE, FabricLoader.getInstance().getGameDir());
        if (configLoadResult.toggleKeyWasReset()) {
            UPDATE_NOTICE.markToggleKeyReset();
        }
        UPDATE_NOTICE.scheduleUpdateCheck(BRIDGE);
        Object resolvedCategory = resolveCategoryObject(KEY_CATEGORY);

        rawConfigKeyWasDown = false;
        configKeyWasDown = false;

        try {
            toggleKeyMapping = registerKeyBindingCompat(
                createKeyMapping(KEY_TRANSLATION, PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, resolvedCategory)
            );
            toggleKeyRegistered = isKeyMappingRegisteredCompat(toggleKeyMapping);
            toggleKeyUnavailable = !toggleKeyRegistered;
        } catch (Throwable ignored) {
            toggleKeyMapping = null;
            toggleKeyRegistered = false;
            toggleKeyUnavailable = true;
        }

        try {
            configKeyMapping = registerKeyBindingCompat(
                createKeyMapping(CONFIG_KEY_TRANSLATION, InputConstants.KEY_U, resolvedCategory)
            );
            configKeyRegistered = isKeyMappingRegisteredCompat(configKeyMapping);
            if (!configKeyRegistered) {
                addToOptionsKeyMappingsCompat(configKeyMapping);
                configKeyRegistered = isKeyMappingRegisteredCompat(configKeyMapping);
            }
        } catch (Throwable ignored) {
            configKeyMapping = null;
            configKeyRegistered = false;
        }

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

    private void onClientTick(Minecraft client) {
        UPDATE_NOTICE.flushPendingUpdateMessage(client);
        UPDATE_NOTICE.flushPendingToggleKeyResetNotice(client);
        MINING_STATE.resetTickFlags();
        boolean inGameHotkeyContext = PermaClickGameContext.isInGameHotkeyContext(client);

        refreshKeyMappingRegistrationState();

        boolean toggleMappedAvailable = toggleKeyMapping != null && toggleKeyRegistered;
        boolean configMappedAvailable = configKeyMapping != null && configKeyRegistered;

        if (toggleMappedAvailable) {
            boolean keyDown = toggleKeyMapping.isDown();
            if (inGameHotkeyContext && keyDown != keyWasDown) {
                EVENT_LOOP.onKeyInput(BRIDGE.boundKeyCode(), keyDown);
            }
            keyWasDown = keyDown;
        } else if (toggleKeyUnavailable || toggleKeyMapping != null) {
            boolean keyDown = isRawKeyDown(BRIDGE.boundKeyCode());
            if (inGameHotkeyContext && keyDown != keyWasDown) {
                EVENT_LOOP.onKeyInput(BRIDGE.boundKeyCode(), keyDown);
            }
            keyWasDown = keyDown;
        }

        boolean openedConfigScreen = false;
        if (inGameHotkeyContext && configMappedAvailable) {
            while (configKeyMapping.consumeClick()) {
                openedConfigScreen = openConfigScreen(client);
            }

            if (!openedConfigScreen) {
                boolean configKeyDownMapped = configKeyMapping.isDown();
                if (configKeyDownMapped && !configKeyWasDown) {
                    openedConfigScreen = openConfigScreen(client);
                }
                configKeyWasDown = configKeyDownMapped;
            } else {
                configKeyWasDown = false;
            }
        } else {
            configKeyWasDown = false;
        }

        boolean configKeyDown = isRawKeyDown(InputConstants.KEY_U);
        if (inGameHotkeyContext && configKeyDown && !rawConfigKeyWasDown && !openedConfigScreen) {
            openConfigScreen(client);
        }
        rawConfigKeyWasDown = configKeyDown;

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

        if (instance != null && !instance.toggleKeyUnavailable && instance.toggleKeyMapping != null) {
            instance.toggleKeyMapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
        }
    }

    private static boolean isRawKeyDown(int keyCode) {
        try {
            return InputConstants.isKeyDown(keyCode);
        } catch (Throwable ignored) {
            return false;
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
            && "com.github.hrobasti.permaclicker.fabric.PermaClickFabricConfigScreen".equals(screen.getClass().getName());
    }

    public static boolean shouldBlockSettingsOpenForBackgroundMode(Minecraft minecraft) {
        return PermaClickGameContext.isBackgroundModeActive(minecraft, BRIDGE.currentConfig());
    }

    public static void stopForFocusedSettingsOpen(Minecraft minecraft) {
        stopForFocusedEscPause(minecraft);
    }

    private static KeyMapping createKeyMapping(String translationKey, int defaultKeyCode, Object categoryObject) {
        Object keyObject = InputConstants.Type.KEYBOARD.getOrCreate(defaultKeyCode);

        Object[][] argumentCandidates = categoryObject == null
            ? new Object[][] {
                new Object[] { translationKey, InputConstants.Type.KEYBOARD, defaultKeyCode, KEY_CATEGORY },
                new Object[] { translationKey, keyObject, KEY_CATEGORY },
                new Object[] { translationKey, defaultKeyCode, KEY_CATEGORY }
            }
            : new Object[][] {
                new Object[] { translationKey, InputConstants.Type.KEYBOARD, defaultKeyCode, categoryObject },
                new Object[] { translationKey, keyObject, categoryObject },
                new Object[] { translationKey, defaultKeyCode, categoryObject },
                new Object[] { translationKey, InputConstants.Type.KEYBOARD, defaultKeyCode, KEY_CATEGORY },
                new Object[] { translationKey, keyObject, KEY_CATEGORY },
                new Object[] { translationKey, defaultKeyCode, KEY_CATEGORY }
            };

        Throwable lastFailure = null;

        for (Constructor<?> constructor : KeyMapping.class.getDeclaredConstructors()) {
            try {
                constructor.setAccessible(true);
            } catch (Throwable ignored) {
                // best effort for non-public constructors
            }

            for (Object[] candidateArgs : argumentCandidates) {
                if (!ReflectionCompat.isCompatible(constructor.getParameterTypes(), candidateArgs)) {
                    continue;
                }

                try {
                    Object instance = constructor.newInstance(candidateArgs);
                    return (KeyMapping) instance;
                } catch (Throwable ex) {
                    lastFailure = ex;
                }
            }
        }

        throw new IllegalStateException("Unable to create Fabric key mapping for PermaClicker", lastFailure);
    }

    private static KeyMapping registerKeyBindingCompat(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return null;
        }

        for (String helperClassName : new String[] {
            "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
            "net.fabricmc.fabric.api.client.keybinding.KeyBindingHelper"
        }) {
            try {
                Class<?> helperClass = Class.forName(helperClassName);
                Method registerMethod = helperClass.getMethod("registerKeyBinding", KeyMapping.class);
                Object registered = registerMethod.invoke(null, keyMapping);
                if (registered instanceof KeyMapping mapping) {
                    return mapping;
                }
                return keyMapping;
            } catch (Throwable ignored) {
                // try next helper candidate
            }
        }

        addToOptionsKeyMappingsCompat(keyMapping);
        return keyMapping;
    }

    private static void addToOptionsKeyMappingsCompat(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return;
        }

        try {
            Field mappingsField = resolveKeyMappingsField(options.getClass());
            if (mappingsField == null) {
                return;
            }

            Object currentValue = mappingsField.get(options);
            if (currentValue instanceof KeyMapping[] currentMappings) {
                for (KeyMapping mapping : currentMappings) {
                    if (mapping == keyMapping) {
                        return;
                    }
                }

                KeyMapping[] expandedMappings = Arrays.copyOf(currentMappings, currentMappings.length + 1);
                expandedMappings[currentMappings.length] = keyMapping;
                mappingsField.set(options, expandedMappings);
            } else if (currentValue instanceof Collection<?> currentCollection) {
                if (currentCollection.contains(keyMapping)) {
                    return;
                }

                @SuppressWarnings("unchecked")
                Collection<Object> mutableCollection = (Collection<Object>) currentCollection;
                mutableCollection.add(keyMapping);
            } else {
                return;
            }

            invokeStaticNoArgCompat(KeyMapping.class, "resetMapping");
            invokeStaticNoArgCompat(KeyMapping.class, "resetMap");
        } catch (Throwable ignored) {
            // best effort fallback
        }
    }

    private static boolean isKeyMappingRegisteredCompat(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return false;
        }

        try {
            Field mappingsField = resolveKeyMappingsField(options.getClass());
            if (mappingsField == null) {
                return false;
            }

            Object currentValue = mappingsField.get(options);
            if (currentValue instanceof KeyMapping[] currentMappings) {
                for (KeyMapping mapping : currentMappings) {
                    if (mapping == keyMapping) {
                        return true;
                    }
                }
                return false;
            }

            if (currentValue instanceof Collection<?> currentCollection) {
                return currentCollection.contains(keyMapping);
            }
        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    private void refreshKeyMappingRegistrationState() {
        if (toggleKeyMapping != null && !toggleKeyRegistered) {
            addToOptionsKeyMappingsCompat(toggleKeyMapping);
            toggleKeyRegistered = isKeyMappingRegisteredCompat(toggleKeyMapping);
            toggleKeyUnavailable = !toggleKeyRegistered;
        }

        if (configKeyMapping != null && !configKeyRegistered) {
            addToOptionsKeyMappingsCompat(configKeyMapping);
            configKeyRegistered = isKeyMappingRegisteredCompat(configKeyMapping);
        }
    }

    private static Field resolveKeyMappingsField(Class<?> optionsClass) {
        for (Class<?> type = optionsClass; type != null; type = type.getSuperclass()) {
            try {
                Field preferred = type.getDeclaredField("keyMappings");
                Class<?> fieldType = preferred.getType();
                if (fieldType.isArray() && fieldType.getComponentType() == KeyMapping.class) {
                    try {
                        preferred.setAccessible(true);
                    } catch (Throwable ignored) {
                        // best effort
                    }
                    return preferred;
                }
            } catch (NoSuchFieldException ignored) {
                // continue with generic fallback
            }
        }

        for (Class<?> type = optionsClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                boolean supportedType = (fieldType.isArray() && fieldType.getComponentType() == KeyMapping.class)
                    || Collection.class.isAssignableFrom(fieldType);
                if (!supportedType) {
                    continue;
                }

                String normalizedName = field.getName().toLowerCase(java.util.Locale.ROOT);
                if (normalizedName.contains("hotbar") || normalizedName.contains("debug")) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                    // best effort
                }
                return field;
            }
        }

        return null;
    }

    private static void invokeStaticNoArgCompat(Class<?> owner, String methodName) {
        try {
            Method method = owner.getDeclaredMethod(methodName);
            try {
                method.setAccessible(true);
            } catch (Throwable ignored) {
                // best effort
            }
            method.invoke(null);
        } catch (Throwable ignored) {
            // optional static method; ignore when absent
        }
    }

    private static boolean openConfigScreen(Minecraft client) {
        if (client == null) {
            return false;
        }

        Screen configScreen = createConfigScreenCompat(client.gui.screen());
        if (configScreen == null) {
            return false;
        }

        client.setScreenAndShow(configScreen);
        return true;
    }

    private static Screen createConfigScreenCompat(Screen parent) {
        try {
            Class<?> screenClass = Class.forName("com.github.hrobasti.permaclicker.fabric.PermaClickFabricConfigScreen");
            if (!Screen.class.isAssignableFrom(screenClass)) {
                return null;
            }

            Constructor<?> constructor = screenClass.getConstructor(Screen.class);
            Object instance = constructor.newInstance(parent);
            return (Screen) instance;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object resolveCategoryObject(String translationKey) {
        try {
            for (Class<?> nestedClass : KeyMapping.class.getDeclaredClasses()) {
                for (Method method : nestedClass.getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }
                    if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                        continue;
                    }
                    if (!nestedClass.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }

                    try {
                        method.setAccessible(true);
                        return method.invoke(null, translationKey);
                    } catch (Throwable ignored) {
                        // try next factory method candidate
                    }
                }
            }

            for (Method method : KeyMapping.class.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                    continue;
                }

                Class<?> returnType = method.getReturnType();
                if (returnType == void.class || returnType == String.class) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    Object value = method.invoke(null, translationKey);
                    if (value != null) {
                        return value;
                    }
                } catch (Throwable ignored) {
                    // try next static method candidate
                }
            }
        } catch (Throwable ignored) {
            // best effort
        }
        return null;
    }
}
