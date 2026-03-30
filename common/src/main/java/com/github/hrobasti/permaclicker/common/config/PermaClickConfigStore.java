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

    private final Path configFile;

    public PermaClickConfigStore(Path configFile) {
        this.configFile = Objects.requireNonNull(configFile, "configFile");
    }

    public PermaClickConfig load() {
        if (!Files.exists(configFile)) {
            return PermaClickConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            if (model == null) {
                return PermaClickConfig.defaults();
            }
            return model.toConfig();
        } catch (IOException | JsonSyntaxException ignored) {
            return PermaClickConfig.defaults();
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

    private record FileModel(
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
                config.enabled(),
                config.toggleKeyCode(),
                config.overlayEnabled(),
                config.overlayColor(),
                config.runWhenUnfocused() || config.runWhenMinimized(),
                config.autoStopMinutes(),
                config.movementLockEnabled(),
                config.updateCheckEnabled(),
                config.updateChannel().name()
            );
        }

        PermaClickConfig toConfig() {
            PermaClickConfig defaults = PermaClickConfig.defaults();

            boolean resolvedEnabled = enabled == null ? defaults.enabled() : enabled;

            int resolvedKeyCode = toggleKeyCode == null || toggleKeyCode <= 0
                ? defaults.toggleKeyCode()
                : toggleKeyCode;

            boolean resolvedOverlayEnabled = overlayEnabled == null ? defaults.overlayEnabled() : overlayEnabled;
            String resolvedOverlayColor = PermaClickConfig.normalizeOverlayColor(
                overlayColor == null ? defaults.overlayColor() : overlayColor
            );
            boolean defaultRunInBackground = defaults.runWhenUnfocused() || defaults.runWhenMinimized();
            boolean resolvedRunInBackground = runInBackground == null ? defaultRunInBackground : runInBackground;
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
                resolvedRunInBackground,
                resolvedAutoStopMinutes,
                resolvedMovementLockEnabled,
                resolvedUpdateCheckEnabled,
                resolvedUpdateChannel
            );
        }
    }
}

