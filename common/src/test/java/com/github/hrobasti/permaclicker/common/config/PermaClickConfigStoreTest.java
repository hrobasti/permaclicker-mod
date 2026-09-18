package com.github.hrobasti.permaclicker.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PermaClickConfigStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenFileMissing() {
        PermaClickConfigStore store = new PermaClickConfigStore(tempDir.resolve("permaclick.json"));

        PermaClickConfigStore.LoadResult result = store.load();

        assertEquals(PermaClickConfig.defaults(), result.config());
        assertFalse(result.toggleKeyWasReset());
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        Path file = tempDir.resolve("config/permaclick.json");
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfig original = new PermaClickConfig(
            true,
            293,
            false,
            "red",
            true,
            42,
            false,
            false,
            UpdateChannel.ALPHA
        );
        store.save(original);

        PermaClickConfigStore.LoadResult result = store.load();
        assertEquals(original, result.config());
        assertFalse(result.toggleKeyWasReset());
    }

    @Test
    void loadFallsBackOnInvalidJson() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, "{ not-valid-json ");
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfigStore.LoadResult result = store.load();

        assertEquals(PermaClickConfig.defaults(), result.config());
        assertFalse(result.toggleKeyWasReset());
    }

    @Test
    void loadNormalizesInvalidFields() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
            {
              "configVersion": 2,
              "enabled": true,
              "toggleKeyCode": -1,
              "overlayEnabled": false,
              "overlayColor": "invalid",
              "runInBackground": false,
              "autoStopMinutes": 12000,
                            "movementLockEnabled": false,
                            "updateCheckEnabled": false,
                            "updateChannel": "invalid"
            }
            """);

        PermaClickConfigStore store = new PermaClickConfigStore(file);
        PermaClickConfigStore.LoadResult result = store.load();
        PermaClickConfig loaded = result.config();

        assertFalse(result.toggleKeyWasReset());
        assertTrue(loaded.enabled());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, loaded.toggleKeyCode());
        assertFalse(loaded.overlayEnabled());
        assertEquals(PermaClickConfig.DEFAULT_OVERLAY_COLOR, loaded.overlayColor());
        assertFalse(loaded.runInBackground());
        assertEquals(PermaClickConfig.MAX_AUTO_STOP_MINUTES, loaded.autoStopMinutes());
        assertFalse(loaded.movementLockEnabled());
        assertFalse(loaded.updateCheckEnabled());
        assertEquals(UpdateChannel.BETA, loaded.updateChannel());
    }

    @Test
    void loadMigratesLegacyToggleKeyToDefaultOnce() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
            {
              "enabled": true,
              "toggleKeyCode": 298,
              "overlayEnabled": true,
              "overlayColor": "green",
              "runInBackground": false,
              "autoStopMinutes": 0,
              "movementLockEnabled": true,
              "updateCheckEnabled": true,
              "updateChannel": "BETA"
            }
            """);
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfigStore.LoadResult firstLoad = store.load();

        assertTrue(firstLoad.toggleKeyWasReset());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, firstLoad.config().toggleKeyCode());

        PermaClickConfigStore.LoadResult secondLoad = store.load();

        assertFalse(secondLoad.toggleKeyWasReset());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, secondLoad.config().toggleKeyCode());
    }
}

