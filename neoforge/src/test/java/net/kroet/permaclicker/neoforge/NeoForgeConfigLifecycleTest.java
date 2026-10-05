package net.kroet.permaclicker.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Path;
import net.kroet.permaclicker.common.config.PermaClickConfig;
import net.kroet.permaclicker.common.config.PermaClickConfigStore;
import net.kroet.permaclicker.common.config.UpdateChannel;
import net.kroet.permaclicker.common.core.PermaClickBridge;
import net.kroet.permaclicker.common.core.PermaClickClientController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NeoForgeConfigLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void loadAndApplyUsesDefaultsWhenMissing() {
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        NeoForgeConfigLifecycle lifecycle = new NeoForgeConfigLifecycle();

        PermaClickConfigStore.LoadResult result = lifecycle.loadAndApply(bridge, tempDir);

        assertEquals(PermaClickConfig.defaults(), result.config());
        assertFalse(result.toggleKeyWasReset());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, bridge.boundKeyCode());
    }

    @Test
    void saveCurrentWritesExpectedFile() throws IOException {
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        NeoForgeConfigLifecycle lifecycle = new NeoForgeConfigLifecycle();
        PermaClickConfig source = new PermaClickConfig(
                true,
                293,
                false,
                "yellow",
                true,
                20,
                true,
                true,
                UpdateChannel.BETA);
        PermaClickConfig expectedLoaded = new PermaClickConfig(
                true,
                293,
                false,
                "yellow",
                true,
                20,
                true,
                true,
                UpdateChannel.BETA);

        bridge.applyConfig(source);
        lifecycle.saveCurrent(bridge, tempDir);

        Path configPath = lifecycle.resolveConfigPath(tempDir);
        PermaClickConfigStore store = new PermaClickConfigStore(configPath);
        assertEquals(expectedLoaded, store.load().config());
    }
}
