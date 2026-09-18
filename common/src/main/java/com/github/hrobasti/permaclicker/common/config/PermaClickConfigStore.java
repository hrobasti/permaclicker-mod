package com.github.hrobasti.permaclicker.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * JSON-backed config persistence for PermaClick.
 */
public final class PermaClickConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Bumped from the implicit unversioned format to 2 when MC 26.3 switched the input backend
     * from GLFW to SDL, which changed key-code numbering (see PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE).
     * A file with no configVersion predates that change, so its toggleKeyCode is stale.
     */
    static final int CURRENT_CONFIG_VERSION = 2;

    private final Path configFile;

    public PermaClickConfigStore(Path configFile) {
        this.configFile = Objects.requireNonNull(configFile, "configFile");
    }

    /**
     * @param config the resolved, normalized config
     * @param toggleKeyWasReset true if this file predated the 26.3 key-code migration and its
     *                          toggle key was reset to the current default as a result
     */
    public record LoadResult(PermaClickConfig config, boolean toggleKeyWasReset) {
    }

    public LoadResult load() {
        if (!Files.exists(configFile)) {
            return new LoadResult(PermaClickConfig.defaults(), false);
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            if (model == null) {
                return new LoadResult(PermaClickConfig.defaults(), false);
            }

            boolean legacyToggleKeyFormat = model.configVersion() == null;
            PermaClickConfig config = model.toConfig(legacyToggleKeyFormat);

            if (legacyToggleKeyFormat) {
                trySave(config);
            }

            return new LoadResult(config, legacyToggleKeyFormat);
        } catch (IOException | JsonSyntaxException ignored) {
            return new LoadResult(PermaClickConfig.defaults(), false);
        }
    }

    public void save(PermaClickConfig config) throws IOException {
        if (configFile.getParent() != null) {
            Files.createDirectories(configFile.getParent());
        }

        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(FileModel.fromConfig(config), writer);
        }
    }

    private void trySave(PermaClickConfig config) {
        try {
            save(config);
        } catch (IOException ignored) {
            // Best-effort persistence of the migrated config; it will simply migrate again next load.
        }
    }

    private record FileModel(
        Integer configVersion,
        Boolean enabled,
        Integer toggleKeyCode,
        Boolean overlayEnabled,
        String overlayColor,
        Boolean runInBackground,
        Integer autoStopMinutes,
        Boolean movementLockEnabled,
        Boolean updateCheckEnabled,
        String updateChannel
    ) {
        static FileModel fromConfig(PermaClickConfig config) {
            return new FileModel(
                CURRENT_CONFIG_VERSION,
                config.enabled(),
                config.toggleKeyCode(),
                config.overlayEnabled(),
                config.overlayColor(),
                config.runInBackground(),
                config.autoStopMinutes(),
                config.movementLockEnabled(),
                config.updateCheckEnabled(),
                config.updateChannel().name()
            );
        }

        PermaClickConfig toConfig(boolean legacyToggleKeyFormat) {
            PermaClickConfig defaults = PermaClickConfig.defaults();

            boolean resolvedEnabled = enabled == null ? defaults.enabled() : enabled;

            int resolvedKeyCode = legacyToggleKeyFormat || toggleKeyCode == null || toggleKeyCode <= 0
                ? defaults.toggleKeyCode()
                : toggleKeyCode;

            boolean resolvedOverlayEnabled = overlayEnabled == null ? defaults.overlayEnabled() : overlayEnabled;
            String resolvedOverlayColor = PermaClickConfig.normalizeOverlayColor(
                overlayColor == null ? defaults.overlayColor() : overlayColor
            );
            boolean resolvedRunInBackground = runInBackground == null ? defaults.runInBackground() : runInBackground;
            int resolvedAutoStopMinutes = autoStopMinutes == null
                ? defaults.autoStopMinutes()
                : PermaClickConfig.clampAutoStopMinutes(autoStopMinutes);
            boolean resolvedMovementLockEnabled = movementLockEnabled == null
                ? defaults.movementLockEnabled()
                : movementLockEnabled;
            boolean resolvedUpdateCheckEnabled = updateCheckEnabled == null
                ? defaults.updateCheckEnabled()
                : updateCheckEnabled;
            UpdateChannel resolvedUpdateChannel = UpdateChannel.fromString(updateChannel, defaults.updateChannel());

            return new PermaClickConfig(
                resolvedEnabled,
                resolvedKeyCode,
                resolvedOverlayEnabled,
                resolvedOverlayColor,
                resolvedRunInBackground,
                resolvedAutoStopMinutes,
                resolvedMovementLockEnabled,
                resolvedUpdateCheckEnabled,
                resolvedUpdateChannel
            );
        }
    }
}

