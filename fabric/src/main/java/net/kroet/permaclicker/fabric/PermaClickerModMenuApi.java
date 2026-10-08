package net.kroet.permaclicker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.kroet.permaclicker.client.PermaClickClient;

/**
 * Optional Mod Menu integration for PermaClicker on Fabric.
 */
public final class PermaClickerModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PermaClickClient::createConfigScreen;
    }
}
