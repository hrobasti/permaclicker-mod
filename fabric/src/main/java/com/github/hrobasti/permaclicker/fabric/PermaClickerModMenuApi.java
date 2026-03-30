package com.github.hrobasti.permaclicker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.lang.reflect.Constructor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional Mod Menu integration for PermaClicker on Fabric.
 */
public final class PermaClickerModMenuApi implements ModMenuApi {
    @SuppressWarnings("rawtypes")
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory) parent -> {
            Screen screen = createConfigScreenReflective(parent);
            if (screen != null) {
                return screen;
            }

            if (parent instanceof Screen fallback) {
                return fallback;
            }

            return null;
        };
    }

    private static Screen createConfigScreenReflective(Object parent) {
        try {
            Class<?> screenClass = Class.forName("com.github.hrobasti.permaclicker.fabric.PermaClickFabricConfigScreen");
            Constructor<?> constructor = screenClass.getConstructor(Screen.class);
            Screen parentScreen = parent instanceof Screen typedParent ? typedParent : null;
            Object created = constructor.newInstance(parentScreen);
            return created instanceof Screen typedScreen ? typedScreen : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
