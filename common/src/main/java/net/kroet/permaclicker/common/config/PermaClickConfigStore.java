package net.kroet.permaclicker.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * JSON-backed config persistence for PermaClick.
 */
public final class PermaClickConfigStore {
    /** Config file relative to the game directory. */
    public static final String RELATIVE_PATH = "config/permaclicker.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Format version written to the file; raise it on incompatible changes. */
    static final int CURRENT_CONFIG_VERSION = 2;

    private final Path configFile;

    public PermaClickConfigStore(Path configFile) {
        this.configFile = Objects.requireNonNull(configFile, "configFile");
    }

    public static PermaClickConfigStore inGameDirectory(Path gameDirectory) {
        return new PermaClickConfigStore(gameDirectory.resolve(RELATIVE_PATH));
    }

    /**
     * @param config
     *            the resolved, normalized config
     * @param brokenFileBackup
     *            where an unreadable file was moved before falling back to
     *            defaults, or null
     */
    public record LoadResult(PermaClickConfig config, Path brokenFileBackup) {
    }

    public LoadResult load() {
        if (!Files.exists(configFile)) {
            return new LoadResult(PermaClickConfig.defaults(), null);
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            return new LoadResult(model == null ? PermaClickConfig.defaults() : model.toConfig(), null);
        } catch (IOException | JsonParseException e) {
            return new LoadResult(PermaClickConfig.defaults(), backUpBrokenFile());
        }
    }

    /**
     * Writes to a temporary file first, so a crash never leaves a half-written
     * config.
     */
    public void save(PermaClickConfig config) throws IOException {
        Path directory = configFile.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, configFile.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(FileModel.fromConfig(config), writer);
            }
            try {
                Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Path backUpBrokenFile() {
        Path backup = configFile.resolveSibling(configFile.getFileName() + ".broken");
        try {
            Files.move(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException e) {
            return null;
        }
    }

    private record FileModel(
            Integer configVersion,
            Boolean enabled,
            Boolean overlayEnabled,
            String overlayColor,
            Boolean runInBackground,
            Integer autoStopMinutes,
            Boolean movementLockEnabled,
            Boolean updateCheckEnabled,
            String updateChannel,
            String clickMode,
            Integer attackBufferTicks) {
        static FileModel fromConfig(PermaClickConfig config) {
            return new FileModel(
                    CURRENT_CONFIG_VERSION,
                    config.enabled(),
                    config.overlayEnabled(),
                    config.overlayColor(),
                    config.runInBackground(),
                    config.autoStopMinutes(),
                    config.movementLockEnabled(),
                    config.updateCheckEnabled(),
                    config.updateChannel().name(),
                    config.clickMode().name(),
                    config.attackBufferTicks());
        }

        PermaClickConfig toConfig() {
            PermaClickConfig defaults = PermaClickConfig.defaults();
            return new PermaClickConfig(
                    enabled == null ? defaults.enabled() : enabled,
                    overlayEnabled == null ? defaults.overlayEnabled() : overlayEnabled,
                    PermaClickConfig.normalizeOverlayColor(overlayColor == null ? defaults.overlayColor() : overlayColor),
                    runInBackground == null ? defaults.runInBackground() : runInBackground,
                    autoStopMinutes == null
                            ? defaults.autoStopMinutes()
                            : PermaClickConfig.clampAutoStopMinutes(autoStopMinutes),
                    movementLockEnabled == null ? defaults.movementLockEnabled() : movementLockEnabled,
                    updateCheckEnabled == null ? defaults.updateCheckEnabled() : updateCheckEnabled,
                    UpdateChannel.fromString(updateChannel, defaults.updateChannel()),
                    ClickMode.fromString(clickMode, defaults.clickMode()),
                    attackBufferTicks == null
                            ? defaults.attackBufferTicks()
                            : PermaClickConfig.clampAttackBufferTicks(attackBufferTicks));
        }
    }
}
