package com.github.hrobasti.permaclicker.common.config;

/**
 * User-facing update release channels for PermaClicker.
 */
public enum UpdateChannel {
    STABLE,
    BETA,
    ALPHA;

    public static UpdateChannel fromString(String raw, UpdateChannel fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return UpdateChannel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}