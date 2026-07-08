package com.github.hrobasti.permaclicker.common.facade;

import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import com.github.hrobasti.turtlelib.UpdateChecker.UpdateChecker;
import com.github.hrobasti.turtlelib.UpdateChecker.UpdateReleaseChannel;
import com.github.hrobasti.turtlelib.UpdateChecker.UpdateResult;
import java.util.Map;

/**
 * Single facade for update-check operations delegated to TurtleLib UpdateChecker.
 */
public final class UpdateFacade {
    private static final String USER_AGENT_SUFFIX = " (+https://github.com/hrobasti)";

    private UpdateFacade() {
    }

    private static String userAgent(String currentVersion) {
        String version = currentVersion == null || currentVersion.isBlank() ? "dev" : currentVersion.trim();
        return "PermaClicker/" + version + USER_AGENT_SUFFIX;
    }

    public static UpdateCheckResult check(
        String currentVersion,
        String minecraftVersion,
        UpdateChannel updateChannel,
        String modrinthProjectId,
        String curseforgeProjectId
    ) {
        try {
            UpdateChecker checker = new UpdateChecker(
                userAgent(currentVersion),
                emptyToNull(modrinthProjectId),
                emptyToNull(curseforgeProjectId),
                mapChannel(updateChannel),
                true,
                minecraftVersion
            );

            UpdateResult result = checker.check(currentVersion);
            return new UpdateCheckResult(result.currentVersion(), result.latestVersion(), result.providerVersions());
        } catch (RuntimeException ignored) {
            return new UpdateCheckResult(currentVersion, null, Map.of());
        }
    }

    private static UpdateReleaseChannel mapChannel(UpdateChannel channel) {
        if (channel == null) {
            return UpdateReleaseChannel.BETA;
        }
        return switch (channel) {
            case STABLE -> UpdateReleaseChannel.STABLE;
            case BETA -> UpdateReleaseChannel.BETA;
            case ALPHA -> UpdateReleaseChannel.ALPHA;
        };
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record UpdateCheckResult(String currentVersion, String latestVersion, Map<?, ?> providerVersions) {
    }
}
