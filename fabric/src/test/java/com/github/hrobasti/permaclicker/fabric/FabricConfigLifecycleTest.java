package com.github.hrobasti.permaclicker.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfigStore;
import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
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
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        FabricConfigLifecycle lifecycle = new FabricConfigLifecycle();

        PermaClickConfigStore.LoadResult result = lifecycle.loadAndApply(bridge, tempDir);

        assertEquals(PermaClickConfig.defaults(), result.config());
        assertFalse(result.toggleKeyWasReset());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, bridge.boundKeyCode());
    }

    @Test
    void saveCurrentWritesExpectedFile() throws IOException {
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        FabricConfigLifecycle lifecycle = new FabricConfigLifecycle();
        PermaClickConfig source = new PermaClickConfig(
            true,
            293,
            false,
            "yellow",
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
            20,
            true,
            true,
            UpdateChannel.BETA
        );

        bridge.applyConfig(source);
        lifecycle.saveCurrent(bridge, tempDir);

        Path configPath = lifecycle.resolveConfigPath(tempDir);
        PermaClickConfigStore store = new PermaClickConfigStore(configPath);
        assertEquals(expectedLoaded, store.load().config());
    }
}

