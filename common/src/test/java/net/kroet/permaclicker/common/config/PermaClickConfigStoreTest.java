package net.kroet.permaclicker.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
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
        assertNull(result.brokenFileBackup());
    }

    @Test
    void inGameDirectoryUsesTheConfigFolder() throws IOException {
        PermaClickConfigStore store = PermaClickConfigStore.inGameDirectory(tempDir);

        store.save(PermaClickConfig.defaults());

        assertTrue(Files.exists(tempDir.resolve("config/permaclicker.json")));
    }

    @Test
    void saveAndLoadRoundTripWithoutLeftoverFiles() throws IOException {
        Path file = tempDir.resolve("config/permaclick.json");
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfig original = new PermaClickConfig(
                true,
                false,
                "red",
                true,
                42,
                false,
                false,
                UpdateChannel.ALPHA,
                ClickMode.MOB_ATTACK,
                7);
        store.save(original);
        store.save(original);

        assertEquals(original, store.load().config());
        try (Stream<Path> files = Files.list(file.getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void brokenFileIsBackedUpInsteadOfOverwritten() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, "{ not-valid-json ");
        PermaClickConfigStore store = new PermaClickConfigStore(file);

        PermaClickConfigStore.LoadResult result = store.load();

        assertEquals(PermaClickConfig.defaults(), result.config());
        assertEquals(tempDir.resolve("permaclick.json.broken"), result.brokenFileBackup());
        assertEquals("{ not-valid-json ", Files.readString(result.brokenFileBackup()));
        assertFalse(Files.exists(file));
    }

    @Test
    void loadNormalizesInvalidFields() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "enabled": true,
                  "overlayEnabled": false,
                  "overlayColor": "invalid",
                  "runInBackground": false,
                  "autoStopMinutes": 12000,
                  "movementLockEnabled": false,
                  "updateCheckEnabled": false,
                  "updateChannel": "invalid",
                  "clickMode": "invalid",
                  "attackBufferTicks": 99
                }
                """);

        PermaClickConfig loaded = new PermaClickConfigStore(file).load().config();

        assertTrue(loaded.enabled());
        assertFalse(loaded.overlayEnabled());
        assertEquals(PermaClickConfig.DEFAULT_OVERLAY_COLOR, loaded.overlayColor());
        assertFalse(loaded.runInBackground());
        assertEquals(PermaClickConfig.MAX_AUTO_STOP_MINUTES, loaded.autoStopMinutes());
        assertFalse(loaded.movementLockEnabled());
        assertFalse(loaded.updateCheckEnabled());
        assertEquals(UpdateChannel.BETA, loaded.updateChannel());
        assertEquals(ClickMode.MINING, loaded.clickMode());
        assertEquals(PermaClickConfig.MAX_ATTACK_BUFFER_TICKS, loaded.attackBufferTicks());
    }

    @Test
    void olderFilesStillLoad() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
                {
                  "enabled": false,
                  "toggleKeyCode": 298,
                  "overlayEnabled": true,
                  "overlayColor": "gold",
                  "runInBackground": false,
                  "autoStopMinutes": 5,
                  "movementLockEnabled": true,
                  "updateCheckEnabled": true,
                  "updateChannel": "BETA"
                }
                """);

        PermaClickConfigStore.LoadResult result = new PermaClickConfigStore(file).load();

        assertNull(result.brokenFileBackup());
        assertEquals("gold", result.config().overlayColor());
        assertEquals(5, result.config().autoStopMinutes());
        assertEquals(ClickMode.MINING, result.config().clickMode());
        assertEquals(PermaClickConfig.DEFAULT_ATTACK_BUFFER_TICKS, result.config().attackBufferTicks());
    }

    @Test
    void loadClampsNegativeAttackBufferAndParsesClickModeCaseInsensitively() throws IOException {
        Path file = tempDir.resolve("permaclick.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "clickMode": "mob_attack",
                  "attackBufferTicks": -5
                }
                """);

        PermaClickConfig loaded = new PermaClickConfigStore(file).load().config();

        assertEquals(ClickMode.MOB_ATTACK, loaded.clickMode());
        assertEquals(PermaClickConfig.MIN_ATTACK_BUFFER_TICKS, loaded.attackBufferTicks());
    }
}
