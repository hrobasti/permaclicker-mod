package com.github.hrobasti.permaclicker.client;

import net.minecraft.client.Minecraft;

/**
 * Temporarily forces Minecraft's "pause on lost focus" option off while background mode is
 * active, restoring the player's original setting once background mode ends. Shared between
 * Fabric and NeoForge; each loader entrypoint owns one instance.
 */
public final class PauseOnLostFocusOverride {
    private Boolean originalValue;
    private boolean overridden;

    public void update(Minecraft minecraft, boolean active) {
        if (active) {
            if (!overridden) {
                Boolean currentValue = readValue(minecraft);
                if (currentValue != null) {
                    originalValue = currentValue;
                    overridden = true;
                }
            }
            writeValue(minecraft, false);
            return;
        }

        restore(minecraft);
    }

    public void restore(Minecraft minecraft) {
        if (!overridden) {
            return;
        }

        if (originalValue != null) {
            writeValue(minecraft, originalValue);
        }
        overridden = false;
        originalValue = null;
    }

    private static Boolean readValue(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return null;
        }
        return minecraft.options.pauseOnLostFocus;
    }

    private static void writeValue(Minecraft minecraft, boolean value) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        minecraft.options.pauseOnLostFocus = value;
    }
}
