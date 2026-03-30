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

        PermaClickConfig loaded = store.load();

        assertEquals(PermaClickConfig.defaults(), loaded);
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
            true,
            42,
            false,
            false,
            UpdateChannel.ALPHA
        );
        store.save(original);

        PermaClickConfig loaded = store.load();
        assertEquals(original, loaded);
    }

    @Test
    void loadFallsBackOnInvalidJson() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, "{ not-valid-json ");
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfig loaded = store.load();

        assertEquals(PermaClickConfig.defaults(), loaded);
    }

    @Test
    void loadNormalizesInvalidFields() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
            {
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
        PermaClickConfig loaded = store.load();

        assertTrue(loaded.enabled());
        assertEquals(PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE, loaded.toggleKeyCode());
        assertFalse(loaded.overlayEnabled());
        assertEquals(PermaClickConfig.DEFAULT_OVERLAY_COLOR, loaded.overlayColor());
        assertFalse(loaded.runWhenUnfocused());
        assertFalse(loaded.runWhenMinimized());
        assertEquals(PermaClickConfig.MAX_AUTO_STOP_MINUTES, loaded.autoStopMinutes());
        assertFalse(loaded.movementLockEnabled());
        assertFalse(loaded.updateCheckEnabled());
        assertEquals(UpdateChannel.BETA, loaded.updateChannel());
    }
}

