package com.github.hrobasti.permaclicker.neoforge;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
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

    public PermaClickConfigStore.LoadResult loadAndApply(PermaClickBridge bridge, Path gameDirectory) {
        PermaClickConfigStore store = new PermaClickConfigStore(resolveConfigPath(gameDirectory));
        PermaClickConfigStore.LoadResult result = store.load();
        bridge.applyConfig(result.config());
        return result;
    }

    public void saveCurrent(PermaClickBridge bridge, Path gameDirectory) throws IOException {
        PermaClickConfigStore store = new PermaClickConfigStore(resolveConfigPath(gameDirectory));
        store.save(bridge.currentConfig());
    }
}

