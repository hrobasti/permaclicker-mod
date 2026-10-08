package net.kroet.permaclicker.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kroet.permaclicker.client.PermaClickClient;
import net.kroet.permaclicker.client.PermaClickKeyMappings;
import net.kroet.permaclicker.common.update.ModLoader;
import net.minecraft.client.KeyMapping;

/**
 * Fabric entrypoint: registers the key mappings and forwards Fabric's client
 * events to the shared {@link PermaClickClient}.
 */
public final class PermaClickFabricEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PermaClickClient.init(
                FabricLoader.getInstance().getGameDir(),
                ModLoader.FABRIC,
                FabricLoader.getInstance()
                        .getModContainer("permaclicker")
                        .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                        .orElse(null));

        KeyMapping.Category category = KeyMapping.Category.register(PermaClickKeyMappings.CATEGORY_ID);
        PermaClickClient.setKeyMappings(
                KeyMappingHelper.registerKeyMapping(PermaClickKeyMappings.createToggle(category)),
                KeyMappingHelper.registerKeyMapping(PermaClickKeyMappings.createConfig(category)));

        ClientTickEvents.START_CLIENT_TICK.register(PermaClickClient::onClientTickStart);
        ClientTickEvents.END_CLIENT_TICK.register(PermaClickClient::onClientTickEnd);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PermaClickClient.onDisconnect());
    }
}
