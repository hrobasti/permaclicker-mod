package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.mojang.blaze3d.platform.Window;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Shared game-context predicates and small helpers used by both loader entrypoints to decide
 * when background-mode blocking (pause screen, PermaClicker's own settings screen, chat) applies.
 */
public final class PermaClickGameContext {
    private PermaClickGameContext() {
    }

    public static boolean isBackgroundModeActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        if (config == null || !config.enabled()) {
            return false;
        }

        return config.runInBackground();
    }

    public static boolean isWindowMinimized(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }
        Window window = minecraft.getWindow();
        return window != null && window.isIconified();
    }

    public static boolean isChatMiningContextActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        if (!isMiningAllowedScreen(minecraft) || minecraft.gui.screen() == null) {
            return false;
        }

        if (config == null || !config.enabled()) {
            return false;
        }

        if (minecraft.isWindowActive()) {
            return true;
        }

        return config.runInBackground();
    }

    public static boolean isChatBlockContextActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        return config != null && config.enabled();
    }

    public static boolean isInGameHotkeyContext(Minecraft minecraft) {
        return minecraft != null
            && minecraft.player != null
            && minecraft.level != null
            && minecraft.gameMode != null
            && minecraft.gui.screen() == null;
    }

    public static boolean isMiningAllowedScreen(Minecraft minecraft) {
        if (minecraft.gui.screen() == null) {
            return true;
        }
        if (minecraft.gui.screen() instanceof ChatScreen) {
            return true;
        }

        String simpleName = minecraft.gui.screen().getClass().getSimpleName();
        return "ChatScreen".equals(simpleName) || simpleName.endsWith("ChatScreen");
    }

    public static void stopForFocusedEscPause(Minecraft minecraft, PermaClickBridge bridge) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        PermaClickConfig config = bridge.currentConfig();
        if (config == null || !config.enabled()) {
            return;
        }

        if (config.runInBackground()) {
            return;
        }

        bridge.stopIfEnabled();
    }

    public static ChatFormatting resolveOverlayColor(String rawColorName) {
        String normalized = PermaClickConfig.normalizeOverlayColor(rawColorName);
        try {
            return ChatFormatting.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChatFormatting.GREEN;
        }
    }
}
