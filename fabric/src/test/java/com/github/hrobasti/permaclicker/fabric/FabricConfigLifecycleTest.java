package com.github.hrobasti.permaclicker.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FabricConfigLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void loadAndApplyUsesDefaultsWhenMissing() {
        FabricPermaClickBridge bridge = new FabricPermaClickBridge(new PermaClickClientController());
        FabricConfigLifecycle lifecycle = new FabricConfigLifecycle();

        PermaClickConfig loaded = lifecycle.loadAndApply(bridge, tempDir);

        assertEquals(PermaClickConfig.defaults(), loaded);
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, bridge.boundKeyCode());
    }

    @Test
    void saveCurrentWritesExpectedFile() throws IOException {
        FabricPermaClickBridge bridge = new FabricPermaClickBridge(new PermaClickClientController());
        FabricConfigLifecycle lifecycle = new FabricConfigLifecycle();
        PermaClickConfig source = new PermaClickConfig(
            true,
            293,
            false,
            "yellow",
            false,
            true,
            20,
            true,
            true,
            UpdateChannel.BETA
        );
        PermaClickConfig expectedLoaded = new PermaClickConfig(
            true,
            293,
            false,
            "yellow",
            true,
            true,
            20,
            true,
            true,
            UpdateChannel.BETA
        );

        bridge.applyConfig(source);
        lifecycle.saveCurrent(bridge, tempDir);

        Path configPath = lifecycle.resolveConfigPath(tempDir);
        PermaClickConfigStore store = new PermaClickConfigStore(configPath);
        assertEquals(expectedLoaded, store.load());
    }
}

