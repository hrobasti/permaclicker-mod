package com.github.hrobasti.permaclicker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu integration for PermaClicker on Fabric.
 */
public final class PermaClickerModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PermaClickFabricConfigScreen::new;
    }
}
