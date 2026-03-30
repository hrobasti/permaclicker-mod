package com.github.hrobasti.permaclicker.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Branding-neutral alias entrypoint that delegates to the existing wiring.
 */
public final class PermaClickerFabricEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new PermaClickFabricEntrypoint().onInitializeClient();
    }
}
