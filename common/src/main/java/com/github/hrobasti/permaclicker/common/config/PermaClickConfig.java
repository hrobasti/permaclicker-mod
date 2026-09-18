package com.github.hrobasti.permaclicker.common.config;

import java.util.Locale;
import java.util.Set;

/**
 * Loader-neutral runtime config model.
 */
public record PermaClickConfig(
    boolean enabled,
    int toggleKeyCode,
    boolean overlayEnabled,
    String overlayColor,
    boolean runInBackground,
    int autoStopMinutes,
    boolean movementLockEnabled,
    boolean updateCheckEnabled,
    UpdateChannel updateChannel
) {
    // SDL scancode for F7 (InputConstants.KEY_F7 in com.mojang.blaze3d.platform, MC 26.3+).
    // Was 296 (GLFW keysym) before the 26.3 GLFW->SDL input backend migration.
    public static final int DEFAULT_TOGGLE_KEY_CODE = 64;
    public static final String DEFAULT_OVERLAY_COLOR = "green";
    private static final Set<String> ALLOWED_OVERLAY_COLORS = Set.of(
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
        "white"
    );
    public static final int MIN_AUTO_STOP_MINUTES = 0;
    public static final int MAX_AUTO_STOP_MINUTES = 9999;

    public PermaClickConfig {
        overlayColor = normalizeOverlayColor(overlayColor);
        autoStopMinutes = clampAutoStopMinutes(autoStopMinutes);
        updateChannel = updateChannel == null ? UpdateChannel.BETA : updateChannel;
    }

    public static PermaClickConfig defaults() {
        return new PermaClickConfig(
            false,
            DEFAULT_TOGGLE_KEY_CODE,
            true,
            DEFAULT_OVERLAY_COLOR,
            false,
            0,
            true,
            true,
            UpdateChannel.BETA
        );
    }

    public PermaClickConfig withEnabled(boolean enabled) {
        return new PermaClickConfig(
            enabled,
            toggleKeyCode,
            overlayEnabled,
            overlayColor,
            runInBackground,
            autoStopMinutes,
            movementLockEnabled,
            updateCheckEnabled,
            updateChannel
        );
    }

    public static String normalizeOverlayColor(String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return DEFAULT_OVERLAY_COLOR;
        }

        String normalized = rawColor.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_OVERLAY_COLORS.contains(normalized) ? normalized : DEFAULT_OVERLAY_COLOR;
    }

    public static int clampAutoStopMinutes(int minutes) {
        return Math.max(MIN_AUTO_STOP_MINUTES, Math.min(MAX_AUTO_STOP_MINUTES, minutes));
    }
}

