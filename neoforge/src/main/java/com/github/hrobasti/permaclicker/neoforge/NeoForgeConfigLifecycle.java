package com.github.hrobasti.permaclicker.neoforge;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Handles NeoForge-side startup loading and persistence of PermaClick config.
 */
public final class NeoForgeConfigLifecycle {
    public static final String CONFIG_RELATIVE_PATH = "config/permaclicker.json";

    public Path resolveConfigPath(Path gameDirectory) {
        return gameDirectory.resolve(CONFIG_RELATIVE_PATH);
    }

    public PermaClickConfig loadAndApply(NeoForgePermaClickBridge bridge, Path gameDirectory) {
        PermaClickConfigStore store = new PermaClickConfigStore(resolveConfigPath(gameDirectory));
        PermaClickConfig config = store.load();
        bridge.applyConfig(config);
        return config;
    }

    public void saveCurrent(NeoForgePermaClickBridge bridge, Path gameDirectory) throws IOException {
        PermaClickConfigStore store = new PermaClickConfigStore(resolveConfigPath(gameDirectory));
        store.save(bridge.currentConfig());
    }
}

