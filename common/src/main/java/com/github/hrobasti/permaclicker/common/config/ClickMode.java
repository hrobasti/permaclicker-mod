package com.github.hrobasti.permaclicker.common.config;

import java.util.Locale;

/**
 * What PermaClicker's held attack input is used for while active.
 */
public enum ClickMode {
    /** Keep the attack key held to continuously mine the targeted block (original behavior). */
    MINING,
    /** Hit the targeted mob once per fully recharged attack cooldown (plus a configurable buffer). */
    MOB_ATTACK;

    public static ClickMode fromString(String raw, ClickMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return ClickMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
