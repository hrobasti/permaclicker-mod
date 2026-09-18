package com.github.hrobasti.permaclicker.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Thin entrypoint registered in fabric.mod.json; delegates to {@link PermaClickFabricEntrypoint}
 * for all actual wiring. Kept as a separate class (distinctly named from PermaClickFabricEntrypoint,
 * a single "er") so the public mod.json entrypoint reference doesn't need to change if the
 * internal wiring class is ever renamed.
 */
public final class PermaClickerFabricBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new PermaClickFabricEntrypoint().onInitializeClient();
    }
}
