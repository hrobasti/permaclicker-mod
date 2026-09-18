package com.github.hrobasti.permaclicker.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return null;
        }

        Object option = null;
        try {
            Method accessor = ReflectionCompat.getAccessibleMethod(options.getClass(), "pauseOnLostFocus");
            option = accessor.invoke(options);
            if (option instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // fallback to field lookup
        }

        if (option == null) {
            try {
                Field field = ReflectionCompat.getAccessibleField(options.getClass(), "pauseOnLostFocus");
                option = field.get(options);
                if (option instanceof Boolean bool) {
                    return bool;
                }
            } catch (Throwable ignored) {
                return null;
            }
        }

        return ReflectionCompat.readBooleanFromOption(option);
    }

    private static void writeValue(Minecraft minecraft, boolean value) {
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return;
        }

        Object option = null;
        try {
            Method accessor = ReflectionCompat.getAccessibleMethod(options.getClass(), "pauseOnLostFocus");
            option = accessor.invoke(options);
            if (option instanceof Boolean) {
                Method setter = ReflectionCompat.getAccessibleMethod(options.getClass(), "pauseOnLostFocus", boolean.class);
                setter.invoke(options, value);
                return;
            }
        } catch (Throwable ignored) {
            // fallback to field lookup
        }

        if (option == null) {
            try {
                Field field = ReflectionCompat.getAccessibleField(options.getClass(), "pauseOnLostFocus");
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

        ReflectionCompat.writeBooleanToOption(option, value);
    }
}
