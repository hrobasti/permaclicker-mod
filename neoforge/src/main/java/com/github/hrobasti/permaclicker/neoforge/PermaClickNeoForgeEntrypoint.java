package com.github.hrobasti.permaclicker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickTextKeys;
import com.github.hrobasti.permaclicker.common.update.PermaClickUpdateService;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import org.lwjgl.glfw.GLFW;

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
    private static final NeoForgePermaClickBridge BRIDGE = new NeoForgePermaClickBridge(CONTROLLER);
    private static final NeoForgeClientEventLoop EVENT_LOOP = new NeoForgeClientEventLoop(BRIDGE);
    private static final NeoForgeConfigLifecycle CONFIG_LIFECYCLE = new NeoForgeConfigLifecycle();
    private static final NeoForgeFocusedModeExecutor FOCUSED_MODE_EXECUTOR = new NeoForgeFocusedModeExecutor();
    private static final NeoForgeBackgroundModeExecutor BACKGROUND_MODE_EXECUTOR = new NeoForgeBackgroundModeExecutor();

    private enum BackgroundWorkerState {
        IDLE,
        PRIMING_IN_FOCUS,
        DETACHING_FOCUS,
        BACKGROUND_ACTIVE
    }

    private static PermaClickNeoForgeEntrypoint instance;

    private KeyMapping toggleKeyMapping;
    private KeyMapping configKeyMapping;
    private boolean keyWasDown;
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

    public PermaClickNeoForgeEntrypoint(IEventBus modEventBus) {
        instance = this;
        BRIDGE.bind(createBindings());
        CONFIG_LIFECYCLE.loadAndApply(BRIDGE, FMLPaths.GAMEDIR.get());
        scheduleUpdateCheck();
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

    private NeoForgeRuntimeBindings createBindings() {
        return new NeoForgeRuntimeBindings(
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

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKeyMapping = createKeyMapping(KEY_TRANSLATION, PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE);
        configKeyMapping = createKeyMapping(CONFIG_KEY_TRANSLATION, GLFW.GLFW_KEY_O);
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
            Method register = getAccessibleMethod(categoryClass, "register", String.class);
            return register.invoke(null, KEY_CATEGORY);
        } catch (NoSuchMethodException ignored) {
            Object categoryId = null;

            try {
                categoryId = createCategoryResourceId();
            } catch (ReflectiveOperationException ignoredResourceIdCreation) {
                Method createId = getAccessibleMethod(categoryClass, "createId", String.class);
                categoryId = createId.invoke(null, KEY_CATEGORY);
            }

            try {
                Method registerById = getAccessibleMethod(categoryClass, "register", categoryId.getClass());
                return registerById.invoke(null, categoryId);
            } catch (NoSuchMethodException ignoredRegisterById) {
                return categoryId;
            }
        }
    }

    private static Object createCategoryResourceId() throws ReflectiveOperationException {
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");

        try {
            Method fromNamespaceAndPath = getAccessibleMethod(resourceLocationClass, "fromNamespaceAndPath", String.class, String.class);
            return fromNamespaceAndPath.invoke(null, "permaclicker", KEY_CATEGORY);
        } catch (NoSuchMethodException ignored) {
            Method parse = getAccessibleMethod(resourceLocationClass, "parse", String.class);
            return parse.invoke(null, "permaclicker:" + KEY_CATEGORY);
        }
    }

    public static void updateToggleKeyCode(int keyCode) {
        BRIDGE.setBoundKeyCode(keyCode);

        if (instance != null && instance.toggleKeyMapping != null) {
            instance.toggleKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        }
    }

    private void onClientTick(ClientTickEvent.Pre event) {
        if (toggleKeyMapping == null) {
            return;
        }

        flushPendingUpdateMessage();
        Minecraft minecraft = Minecraft.getInstance();
        backgroundPauseSuppressedThisTick = false;
        boolean inGameHotkeyContext = isInGameHotkeyContext(minecraft);

        boolean keyDown = toggleKeyMapping.isDown();
        if (inGameHotkeyContext && keyDown != keyWasDown) {
            EVENT_LOOP.onKeyInput(BRIDGE.boundKeyCode(), keyDown);
        }
        keyWasDown = keyDown;

        if (inGameHotkeyContext && configKeyMapping != null) {
            while (configKeyMapping.consumeClick()) {
                if (minecraft != null) {
                    Screen configScreen = createConfigScreenCompat(minecraft.screen);
                    if (configScreen != null) {
                        minecraft.execute(() -> minecraft.setScreen(configScreen));
                    }
                }
            }
        }

        suppressBackgroundPauseScreen(minecraft);
        attackRequestedThisTick = false;
        EVENT_LOOP.onClientTick();
        syncAttackHoldState(minecraft);
        applyMovementLockInputSuppression(minecraft);
        updateCursorCapture(minecraft);
    }

    private static boolean isInGameHotkeyContext(Minecraft minecraft) {
        return minecraft != null
            && minecraft.player != null
            && minecraft.level != null
            && minecraft.gameMode != null
            && minecraft.screen == null;
    }

    private void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        suppressBackgroundPauseScreen(minecraft);
        applyMovementLockViewFreeze(minecraft);
    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
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
            CONFIG_LIFECYCLE.saveCurrent(BRIDGE, FMLPaths.GAMEDIR.get());
        } catch (IOException ignored) {
            // Best-effort config persistence; runtime should continue safely.
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

    public static NeoForgePermaClickBridge bridge() {
        return BRIDGE;
    }

    public static NeoForgeClientEventLoop eventLoop() {
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
            && "com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeConfigScreen".equals(screen.getClass().getName());
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

    private static String resolveCurrentVersion() {
        String implementationVersion = PermaClickNeoForgeEntrypoint.class.getPackage().getImplementationVersion();
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

