package net.kroet.permaclicker.client;

import com.mojang.blaze3d.platform.Window;
import java.util.Locale;
import net.kroet.permaclicker.common.config.ClickMode;
import net.kroet.permaclicker.common.config.PermaClickConfig;
import net.kroet.permaclicker.common.core.PermaClickBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * Shared game-context predicates and small helpers used by both loader
 * entrypoints to decide when background-mode blocking (pause screen,
 * PermaClicker's own settings screen, chat) applies.
 */
public final class PermaClickGameContext {
    private PermaClickGameContext() {
    }

    public static boolean isBackgroundModeActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        if (!config.enabled()) {
            return false;
        }

        return config.runInBackground();
    }

    public static boolean isWindowMinimized(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return window != null && window.isIconified();
    }

    public static boolean isChatMiningContextActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        if (!isMiningAllowedScreen(minecraft) || minecraft.gui.screen() == null) {
            return false;
        }

        if (!config.enabled()) {
            return false;
        }

        // Forcing continueAttack(true) while chat is open would break blocks; mob
        // attack never mines.
        if (config.clickMode() == ClickMode.MOB_ATTACK) {
            return false;
        }

        if (minecraft.isWindowActive()) {
            return true;
        }

        return config.runInBackground();
    }

    public static boolean isChatBlockContextActive(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }

        return config.enabled();
    }

    public static boolean isInGameHotkeyContext(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.level != null
                && minecraft.gameMode != null
                && minecraft.gui.screen() == null;
    }

    public static boolean isMiningAllowedScreen(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        if (screen == null) {
            return true;
        }
        if (screen instanceof ChatScreen) {
            return true;
        }

        String simpleName = screen.getClass().getSimpleName();
        return "ChatScreen".equals(simpleName) || simpleName.endsWith("ChatScreen");
    }

    public static void stopForFocusedEscPause(Minecraft minecraft, PermaClickBridge bridge) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        PermaClickConfig config = bridge.currentConfig();
        if (!config.enabled()) {
            return;
        }

        if (config.runInBackground()) {
            return;
        }

        bridge.stopIfEnabled();
    }

    /** Shows {@code screen} at once; null closes the current screen. */
    // Minecraft.setScreenAndShow forwards to Gui.setScreen, which accepts null;
    // only its own parameter lacks the annotation.
    @SuppressWarnings("null")
    public static void setScreenAndShow(Minecraft minecraft, @Nullable Screen screen) {
        minecraft.setScreenAndShow(screen);
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
