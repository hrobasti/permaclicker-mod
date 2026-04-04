package com.github.hrobasti.permaclicker.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickTextKeys;
import com.github.hrobasti.permaclicker.common.update.PermaClickUpdateService;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric entrypoint and client event wiring for PermaClick.
 */
public final class PermaClickFabricEntrypoint implements ClientModInitializer {
    private static final String KEY_CATEGORY = "permaclicker";
    private static final String KEY_TRANSLATION = "key.permaclicker.toggle";
    private static final String CONFIG_KEY_TRANSLATION = "key.permaclicker.config";

    private static final PermaClickClientController CONTROLLER = new PermaClickClientController();
    private static final FabricPermaClickBridge BRIDGE = new FabricPermaClickBridge(CONTROLLER);
    private static final FabricClientEventLoop EVENT_LOOP = new FabricClientEventLoop(BRIDGE);
    private static final FabricConfigLifecycle CONFIG_LIFECYCLE = new FabricConfigLifecycle();
    private static final FabricFocusedModeExecutor FOCUSED_MODE_EXECUTOR = new FabricFocusedModeExecutor();
    private static final FabricBackgroundModeExecutor BACKGROUND_MODE_EXECUTOR = new FabricBackgroundModeExecutor();

    private enum BackgroundWorkerState {
        IDLE,
        PRIMING_IN_FOCUS,
        DETACHING_FOCUS,
        BACKGROUND_ACTIVE
    }

    private static PermaClickFabricEntrypoint instance;

    private KeyMapping toggleKeyMapping;
    private KeyMapping configKeyMapping;
    private boolean toggleKeyRegistered;
    private boolean configKeyRegistered;
    private boolean toggleKeyUnavailable;
    private boolean keyWasDown;
    private boolean configKeyWasDown;
    private boolean rawConfigKeyWasDown;
    private boolean attackRequestedThisTick;
    private boolean attackHoldForcedByPermaClick;
    private BackgroundWorkerState backgroundWorkerState = BackgroundWorkerState.IDLE;
    private Boolean originalPauseOnLostFocusValue;
    private boolean pauseOnLostFocusOverridden;
    private boolean movementLockActive;
    private boolean backgroundCursorFreeActive;
    private boolean backgroundPauseSuppressedThisTick;
    private long backgroundFocusProxyWindowHandle;
    private boolean hasLockedView;
    private float lockedYaw;
    private float lockedPitch;
    private volatile PermaClickUpdateService.UpdateStatus pendingUpdateStatus;

    @Override
    public void onInitializeClient() {
        instance = this;
        BRIDGE.bind(createBindings());
        CONFIG_LIFECYCLE.loadAndApply(BRIDGE, FabricLoader.getInstance().getGameDir());
        scheduleUpdateCheck();
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
                createKeyMapping(CONFIG_KEY_TRANSLATION, GLFW.GLFW_KEY_O, resolvedCategory)
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

    private FabricRuntimeBindings createBindings() {
        return new FabricRuntimeBindings(
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return minecraft != null && minecraft.isWindowActive();
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return isWindowMinimized(minecraft);
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return minecraft != null
                    && minecraft.player != null
                    && minecraft.gameMode != null
                    && isMiningAllowedScreen(minecraft)
                    && !backgroundPauseSuppressedThisTick;
            },
            () -> {
                Minecraft minecraft = Minecraft.getInstance();
                return performHeldAttackTick(minecraft);
            },
            payload -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null) {
                    ChatFormatting overlayColor = resolveOverlayColor(payload.colorName());
                    Component overlayMessage = Component.literal(payload.message()).withStyle(overlayColor);
                    if (!displayActionBarCompat(minecraft, overlayMessage) && minecraft.player != null) {
                        displayClientMessageCompat(minecraft.player, overlayMessage, true);
                    }
                }
            },
            active -> setMovementLockActive(Minecraft.getInstance(), active),
            active -> setBackgroundCursorFreeActive(Minecraft.getInstance(), active)
        );
    }

    private void onClientTick(Minecraft client) {
        flushPendingUpdateMessage();
        backgroundPauseSuppressedThisTick = false;
        boolean inGameHotkeyContext = isInGameHotkeyContext(client);

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

        boolean configKeyDown = isRawKeyDown(GLFW.GLFW_KEY_O);
        if (inGameHotkeyContext && configKeyDown && !rawConfigKeyWasDown && !openedConfigScreen) {
            openConfigScreen(client);
        }
        rawConfigKeyWasDown = configKeyDown;

        suppressBackgroundPauseScreen(client);
        attackRequestedThisTick = false;
        EVENT_LOOP.onClientTick();
        syncAttackHoldState(client);
        applyMovementLockInputSuppression(client);
        updateCursorCapture(client);
    }

    private void onClientTickPost(Minecraft client) {
        suppressBackgroundPauseScreen(client);
        applyMovementLockViewFreeze(client);
    }

    private static boolean isInGameHotkeyContext(Minecraft minecraft) {
        return minecraft != null
            && minecraft.player != null
            && minecraft.level != null
            && minecraft.gameMode != null
            && minecraft.screen == null;
    }

    private void onClientDisconnect() {
        BRIDGE.onClientShutdown();
        attackHoldForcedByPermaClick = false;
        movementLockActive = false;
        backgroundCursorFreeActive = false;
        backgroundPauseSuppressedThisTick = false;
        BACKGROUND_MODE_EXECUTOR.reset();
        resetBackgroundWorkerState();
        restorePauseOnLostFocus(Minecraft.getInstance());
        destroyBackgroundFocusProxyWindow();
        hasLockedView = false;
        updateCursorCapture(Minecraft.getInstance());
        try {
            CONFIG_LIFECYCLE.saveCurrent(BRIDGE, FabricLoader.getInstance().getGameDir());
        } catch (IOException ignored) {
            // Best-effort config persistence; runtime should continue safely.
        }
    }

    public static void updateToggleKeyCode(int keyCode) {
        BRIDGE.setBoundKeyCode(keyCode);

        if (instance != null && !instance.toggleKeyUnavailable && instance.toggleKeyMapping != null) {
            instance.toggleKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        }
    }

    private static boolean isWindowMinimized(Minecraft minecraft) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        long handle = resolveWindowHandle(minecraft);
        if (handle == 0L) {
            return false;
        }

        try {
            return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isRawKeyDown(int keyCode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        long handle = resolveWindowHandle(minecraft);
        if (handle == 0L) {
            return false;
        }

        try {
            return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long resolveWindowHandle(Minecraft minecraft) {
        try {
            Object window = minecraft.getWindow();
            if (window == null) {
                return 0L;
            }

            for (String methodName : new String[] { "handle", "getWindow", "window" }) {
                try {
                    Method method = window.getClass().getMethod(methodName);
                    Object value = method.invoke(window);
                    if (value instanceof Long l) {
                        return l;
                    }
                    if (value instanceof Number n) {
                        return n.longValue();
                    }
                } catch (NoSuchMethodException ignored) {
                    // try next accessor
                }
            }
        } catch (Throwable ignored) {
            // best effort
        }
        return 0L;
    }

    public static PermaClickClientController controller() {
        return CONTROLLER;
    }

    public static FabricPermaClickBridge bridge() {
        return BRIDGE;
    }

    public static FabricClientEventLoop eventLoop() {
        return EVENT_LOOP;
    }

    public static boolean isPauseBlockContextActive(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        PermaClickConfig config = BRIDGE.currentConfig();
        if (config == null || !config.enabled()) {
            return false;
        }

        return config.runWhenUnfocused() || config.runWhenMinimized();
    }

    public static boolean isChatMiningContextActive(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        if (!isMiningAllowedScreen(minecraft) || minecraft.screen == null) {
            return false;
        }

        PermaClickConfig config = BRIDGE.currentConfig();
        if (config == null || !config.enabled()) {
            return false;
        }

        if (minecraft.isWindowActive()) {
            return true;
        }

        return config.runWhenUnfocused() || config.runWhenMinimized();
    }

    public static boolean isChatBlockContextActive(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        PermaClickConfig config = BRIDGE.currentConfig();
        return config != null && config.enabled();
    }

    public static void stopForFocusedEscPause(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        PermaClickConfig config = BRIDGE.currentConfig();
        if (config == null || !config.enabled()) {
            return;
        }

        if (config.runWhenUnfocused() || config.runWhenMinimized()) {
            return;
        }

        BRIDGE.stopIfEnabled();
    }

    public static boolean isPermaClickerConfigScreen(Screen screen) {
        return screen != null
            && "com.github.hrobasti.permaclicker.fabric.PermaClickFabricConfigScreen".equals(screen.getClass().getName());
    }

    public static boolean shouldBlockSettingsOpenForBackgroundMode(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        PermaClickConfig config = BRIDGE.currentConfig();
        if (config == null || !config.enabled()) {
            return false;
        }

        return config.runWhenUnfocused() || config.runWhenMinimized();
    }

    public static void stopForFocusedSettingsOpen(Minecraft minecraft) {
        stopForFocusedEscPause(minecraft);
    }

    private static KeyMapping createKeyMapping(String translationKey, int defaultKeyCode, Object categoryObject) {
        Object keyObject = InputConstants.Type.KEYSYM.getOrCreate(defaultKeyCode);

        Object[][] argumentCandidates = categoryObject == null
            ? new Object[][] {
                new Object[] { translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, KEY_CATEGORY },
                new Object[] { translationKey, keyObject, KEY_CATEGORY },
                new Object[] { translationKey, defaultKeyCode, KEY_CATEGORY }
            }
            : new Object[][] {
                new Object[] { translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, categoryObject },
                new Object[] { translationKey, keyObject, categoryObject },
                new Object[] { translationKey, defaultKeyCode, categoryObject },
                new Object[] { translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, KEY_CATEGORY },
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
                if (!isCompatible(constructor.getParameterTypes(), candidateArgs)) {
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

        Screen configScreen = createConfigScreenCompat(client.screen);
        if (configScreen == null) {
            return false;
        }

        client.setScreen(configScreen);
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

    private static boolean isCompatible(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isAssignable(parameterTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignable(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }

        if (parameterType.isPrimitive()) {
            if (parameterType == int.class) {
                return arg instanceof Integer;
            }
            if (parameterType == long.class) {
                return arg instanceof Long;
            }
            if (parameterType == boolean.class) {
                return arg instanceof Boolean;
            }
            if (parameterType == float.class) {
                return arg instanceof Float;
            }
            if (parameterType == double.class) {
                return arg instanceof Double;
            }
            if (parameterType == byte.class) {
                return arg instanceof Byte;
            }
            if (parameterType == short.class) {
                return arg instanceof Short;
            }
            if (parameterType == char.class) {
                return arg instanceof Character;
            }
            return false;
        }

        return parameterType.isInstance(arg);
    }

    private void scheduleUpdateCheck() {
        PermaClickConfig config = BRIDGE.currentConfig();
        if (config == null || !config.updateCheckEnabled()) {
            return;
        }

        String currentVersion = resolveCurrentVersion();
        String minecraftVersion = SharedConstants.getCurrentVersion().toString();
        var updateChannel = config.updateChannel();

        CompletableFuture.runAsync(() -> {
            PermaClickUpdateService.UpdateStatus status = PermaClickUpdateService.checkForUpdates(
                currentVersion,
                minecraftVersion,
                updateChannel
            );

            if (status.hasUpdate()) {
                pendingUpdateStatus = status;
            }
        });
    }

    private void flushPendingUpdateMessage() {
        PermaClickUpdateService.UpdateStatus status = pendingUpdateStatus;
        if (status == null || !status.hasUpdate()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        var player = minecraft.player;
        if (player == null) {
            return;
        }

        displayClientMessageCompat(
            player,
            Component.translatable(PermaClickTextKeys.UPDATE_AVAILABLE, status.currentVersion(), status.latestVersion())
                .withStyle(ChatFormatting.WHITE),
            false
        );
        displayProviderLine(player, status, "modrinth", "Modrinth");
        displayProviderLine(player, status, "curseforge", "CurseForge");

        pendingUpdateStatus = null;
    }

    private static void displayProviderLine(
        net.minecraft.client.player.LocalPlayer player,
        PermaClickUpdateService.UpdateStatus status,
        String providerKey,
        String providerLabel
    ) {
        String version = status.providerVersions().get(providerKey);
        String url = status.providerVersionUrls().get(providerKey);

        if (version == null || version.isBlank()) {
            displayClientMessageCompat(
                player,
                Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_ERROR, providerLabel).withStyle(ChatFormatting.GRAY),
                false
            );
            return;
        }

        MutableComponent line = Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_OK, providerLabel, version)
            .append(Component.literal(" "))
            .withStyle(ChatFormatting.GRAY);

        if (url != null && !url.isBlank()) {
            MutableComponent link = Component.literal("[" + url + "]")
                .withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                );
            line.append(link);
        }

        displayClientMessageCompat(player, line, false);
    }

    private static void displayClientMessageCompat(net.minecraft.client.player.LocalPlayer player, Component message, boolean actionBar) {
        if (player == null || message == null) {
            return;
        }

        if (invokeCompatibleMethod(player, "displayClientMessage", message, actionBar)) {
            return;
        }

        if (invokeCompatibleMethod(player, "sendSystemMessage", message, actionBar)) {
            return;
        }

        if (invokeCompatibleMethod(player, "sendMessage", message, actionBar)) {
            return;
        }

        if (invokeCompatibleMethodByShape(player, new Object[] { message, actionBar })) {
            return;
        }

        if (invokeCompatibleMethod(player, "sendSystemMessage", message)) {
            return;
        }

        invokeCompatibleMethodByShape(player, new Object[] { message });
    }

    private static boolean displayActionBarCompat(Minecraft minecraft, Component message) {
        if (minecraft == null || message == null) {
            return false;
        }

        Object gui = resolveMinecraftGui(minecraft);
        if (gui != null) {
            if (invokeCompatibleMethod(gui, "setOverlayMessage", message, false)) {
                return true;
            }
            if (invokeCompatibleMethod(gui, "setOverlayMessage", message, Boolean.FALSE)) {
                return true;
            }
            if (invokeCompatibleMethod(gui, "setOverlayMessage", message)) {
                return true;
            }
            if (invokeCompatibleMethod(gui, "setActionBarText", message)) {
                return true;
            }
            if (invokeCompatibleMethod(gui, "displayClientMessage", message, true)) {
                return true;
            }

            if (invokeCompatibleMethodByShape(gui, new Object[] { message, false })) {
                return true;
            }

            if (invokeCompatibleMethodByShape(gui, new Object[] { message, true })) {
                return true;
            }

            if (invokeCompatibleMethodByShape(gui, new Object[] { message })) {
                return true;
            }
        }

        if (minecraft.player == null) {
            return false;
        }

        return invokeCompatibleMethod(minecraft.player, "displayClientMessage", message, true)
            || invokeCompatibleMethodByShape(minecraft.player, new Object[] { message, true })
            || invokeCompatibleMethodByShape(minecraft.player, new Object[] { message });
    }

    private static Object resolveMinecraftGui(Minecraft minecraft) {
        try {
            Field field = getAccessibleField(minecraft.getClass(), "gui");
            Object value = field.get(minecraft);
            if (value != null) {
                return value;
            }
        } catch (Throwable ignored) {
            // try method fallback
        }

        try {
            Method method = getAccessibleMethod(minecraft.getClass(), "gui");
            return method.invoke(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeCompatibleMethod(Object receiver, String methodName, Object... args) {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (!isCompatible(method.getParameterTypes(), args)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // best effort
                }

                try {
                    method.invoke(receiver, args);
                    return true;
                } catch (Throwable ignored) {
                    // try next candidate
                }
            }
        }

        return false;
    }

    private static boolean invokeCompatibleMethodByShape(Object receiver, Object[] args) {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!isCompatible(method.getParameterTypes(), args)) {
                    continue;
                }

                if (method.getReturnType() != void.class) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // best effort
                }

                try {
                    method.invoke(receiver, args);
                    return true;
                } catch (Throwable ignored) {
                    // try next candidate
                }
            }
        }

        return false;
    }

    private static String resolveCurrentVersion() {
        String implementationVersion = PermaClickFabricEntrypoint.class.getPackage().getImplementationVersion();
        return (implementationVersion == null || implementationVersion.isBlank()) ? "0.0.0" : implementationVersion;
    }

    private static ChatFormatting resolveOverlayColor(String rawColorName) {
        ChatFormatting parsed = ChatFormatting.getByName(PermaClickConfig.normalizeOverlayColor(rawColorName));
        if (parsed == null || !parsed.isColor()) {
            return ChatFormatting.GREEN;
        }
        return parsed;
    }

    private boolean performHeldAttackTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        attackRequestedThisTick = true;
        attackHoldForcedByPermaClick = true;
        if (backgroundCursorFreeActive) {
            return BACKGROUND_MODE_EXECUTOR.performBackgroundMiningTick(minecraft);
        }

        return FOCUSED_MODE_EXECUTOR.performFocusedMiningTick(minecraft);
    }

    private void syncAttackHoldState(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (!attackRequestedThisTick) {
            if (attackHoldForcedByPermaClick) {
                minecraft.options.keyAttack.setDown(false);
                attackHoldForcedByPermaClick = false;
            }
            if (!backgroundCursorFreeActive) {
                BACKGROUND_MODE_EXECUTOR.reset();
                resetBackgroundWorkerState();
            }
        }
    }

    private void setMovementLockActive(Minecraft minecraft, boolean active) {
        movementLockActive = active;
        if (!active) {
            hasLockedView = false;
        } else {
            captureLockedView(minecraft);
        }
        updateCursorCapture(minecraft);
    }

    private void setBackgroundCursorFreeActive(Minecraft minecraft, boolean active) {
        if (active != backgroundCursorFreeActive) {
            resetBackgroundWorkerState();
            if (!active) {
                BACKGROUND_MODE_EXECUTOR.reset();
            }
        }

        if (!active) {
            destroyBackgroundFocusProxyWindow();
            transitionWorkerState(BackgroundWorkerState.IDLE);
        }

        backgroundCursorFreeActive = active;
        updatePauseOnLostFocusOverride(minecraft, active);
    }

    private void suppressBackgroundPauseScreen(Minecraft minecraft) {
        if (!backgroundCursorFreeActive || minecraft == null) {
            return;
        }
        boolean suppressed = BACKGROUND_MODE_EXECUTOR.aggressivelySuppressPauseScreen(minecraft);
        if (suppressed) {
            backgroundPauseSuppressedThisTick = true;
        }
    }

    private void updatePauseOnLostFocusOverride(Minecraft minecraft, boolean active) {
        if (active) {
            if (!pauseOnLostFocusOverridden) {
                Boolean currentValue = readPauseOnLostFocusValue(minecraft);
                if (currentValue != null) {
                    originalPauseOnLostFocusValue = currentValue;
                    pauseOnLostFocusOverridden = true;
                }
            }
            writePauseOnLostFocusValue(minecraft, false);
            return;
        }

        restorePauseOnLostFocus(minecraft);
    }

    private void restorePauseOnLostFocus(Minecraft minecraft) {
        if (!pauseOnLostFocusOverridden) {
            return;
        }

        if (originalPauseOnLostFocusValue != null) {
            writePauseOnLostFocusValue(minecraft, originalPauseOnLostFocusValue);
        }
        pauseOnLostFocusOverridden = false;
        originalPauseOnLostFocusValue = null;
    }

    private static Boolean readPauseOnLostFocusValue(Minecraft minecraft) {
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return null;
        }

        Object option = null;
        try {
            Method accessor = getAccessibleMethod(options.getClass(), "pauseOnLostFocus");
            option = accessor.invoke(options);
            if (option instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // fallback to field lookup
        }

        if (option == null) {
            try {
                Field field = getAccessibleField(options.getClass(), "pauseOnLostFocus");
                option = field.get(options);
                if (option instanceof Boolean bool) {
                    return bool;
                }
            } catch (Throwable ignored) {
                return null;
            }
        }

        return readBooleanFromOption(option);
    }

    private static void writePauseOnLostFocusValue(Minecraft minecraft, boolean value) {
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return;
        }

        Object option = null;
        try {
            Method accessor = getAccessibleMethod(options.getClass(), "pauseOnLostFocus");
            option = accessor.invoke(options);
            if (option instanceof Boolean) {
                Method setter = getAccessibleMethod(options.getClass(), "pauseOnLostFocus", boolean.class);
                setter.invoke(options, value);
                return;
            }
        } catch (Throwable ignored) {
            // fallback to field lookup
        }

        if (option == null) {
            try {
                Field field = getAccessibleField(options.getClass(), "pauseOnLostFocus");
                Object fieldValue = field.get(options);
                if (fieldValue instanceof Boolean) {
                    field.set(options, value);
                    return;
                }
                option = fieldValue;
            } catch (Throwable ignored) {
                return;
            }
        }

        writeBooleanToOption(option, value);
    }

    private static Boolean readBooleanFromOption(Object option) {
        if (option == null) {
            return null;
        }

        for (String methodName : new String[] { "get", "value" }) {
            try {
                Method getter = getAccessibleMethod(option.getClass(), methodName);
                Object value = getter.invoke(option);
                if (value instanceof Boolean bool) {
                    return bool;
                }
            } catch (Throwable ignored) {
                // try next getter candidate
            }
        }
        return null;
    }

    private static void writeBooleanToOption(Object option, boolean value) {
        if (option == null) {
            return;
        }

        try {
            Method setPrimitive = getAccessibleMethod(option.getClass(), "set", boolean.class);
            setPrimitive.invoke(option, value);
            return;
        } catch (Throwable ignored) {
            // try boxed overload
        }

        try {
            Method setBoxed = getAccessibleMethod(option.getClass(), "set", Object.class);
            setBoxed.invoke(option, Boolean.valueOf(value));
            return;
        } catch (Throwable ignored) {
            // try setValue variant
        }

        try {
            Method setValue = getAccessibleMethod(option.getClass(), "setValue", Object.class);
            setValue.invoke(option, Boolean.valueOf(value));
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static Field getAccessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException ignored) {
            Field declared = owner.getDeclaredField(name);
            try {
                declared.setAccessible(true);
            } catch (Throwable ignoredSetAccessible) {
                // best effort
            }
            return declared;
        }
    }

    private void applyMovementLockInputSuppression(Minecraft minecraft) {
        if (!movementLockActive || minecraft == null || minecraft.options == null) {
            return;
        }

        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    private void applyMovementLockViewFreeze(Minecraft minecraft) {
        if (!movementLockActive || minecraft == null || minecraft.player == null) {
            return;
        }

        if (!hasLockedView) {
            captureLockedView(minecraft);
        }

        minecraft.player.setYRot(lockedYaw);
        minecraft.player.setXRot(lockedPitch);
        minecraft.player.yRotO = lockedYaw;
        minecraft.player.xRotO = lockedPitch;
    }

    private void captureLockedView(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        lockedYaw = minecraft.player.getYRot();
        lockedPitch = minecraft.player.getXRot();
        hasLockedView = true;
    }

    private static boolean isMiningAllowedScreen(Minecraft minecraft) {
        if (minecraft.screen == null) {
            return true;
        }
        if (minecraft.screen instanceof ChatScreen) {
            return true;
        }

        String simpleName = minecraft.screen.getClass().getSimpleName();
        return "ChatScreen".equals(simpleName) || simpleName.endsWith("ChatScreen");
    }

    private void updateCursorCapture(Minecraft minecraft) {
        // Background mode intentionally avoids cursor/focus detach hacks.
    }

    private void transitionWorkerState(BackgroundWorkerState nextState) {
        if (nextState == null || backgroundWorkerState == nextState) {
            return;
        }
        backgroundWorkerState = nextState;
    }

    private void resetBackgroundWorkerState() {
        transitionWorkerState(BackgroundWorkerState.IDLE);
    }

    private void destroyBackgroundFocusProxyWindow() {
        long handle = backgroundFocusProxyWindowHandle;
        if (handle == 0L) {
            return;
        }

        backgroundFocusProxyWindowHandle = 0L;
        try {
            GLFW.glfwDestroyWindow(handle);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static Method getAccessibleMethod(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Method declared = owner.getDeclaredMethod(name, parameterTypes);
            try {
                declared.setAccessible(true);
            } catch (Throwable ignoredSetAccessible) {
                // best effort for stricter access rules
            }
            return declared;
        }
    }

}

