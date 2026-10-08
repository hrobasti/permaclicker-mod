package net.kroet.permaclicker.common.facade;

import java.util.EnumMap;
import java.util.Map;
import net.kroet.permaclicker.common.config.UpdateChannel;
import net.kroet.permaclicker.common.update.ModLoader;
import net.kroet.permaclicker.common.update.ProviderRelease;
import net.kroet.permaclicker.common.update.UpdateProvider;
import net.kroet.turtlelib.UpdateChecker.UpdateChecker;
import net.kroet.turtlelib.UpdateChecker.UpdateLoader;
import net.kroet.turtlelib.UpdateChecker.UpdatePlatform;
import net.kroet.turtlelib.UpdateChecker.UpdateReleaseChannel;
import net.kroet.turtlelib.UpdateChecker.UpdateResult;

/**
 * Single facade for update-check operations delegated to TurtleLib
 * UpdateChecker. Blocking: call from a background thread.
 */
public final class UpdateFacade {
    private static final String USER_AGENT_SUFFIX = " (+https://kroet.net)";

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
            ModLoader loader,
            String modrinthProjectId,
            String curseforgeProjectId) {
        UpdateResult result = new UpdateChecker(
                userAgent(currentVersion),
                modrinthProjectId,
                curseforgeProjectId,
                mapChannel(updateChannel),
                minecraftVersion,
                mapLoader(loader)).check(currentVersion);

        Map<UpdateProvider, ProviderRelease> releases = new EnumMap<>(UpdateProvider.class);
        result.releases().forEach((platform, release) -> releases.put(
                mapPlatform(platform),
                new ProviderRelease(release.version(), release.url())));
        Map<UpdateProvider, String> errors = new EnumMap<>(UpdateProvider.class);
        result.errors().forEach((platform, reason) -> errors.put(mapPlatform(platform), reason));
        return new UpdateCheckResult(result.currentVersion(), result.latestVersion(), releases, errors);
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

    private static UpdateLoader mapLoader(ModLoader loader) {
        if (loader == null) {
            return null;
        }
        return switch (loader) {
            case FABRIC -> UpdateLoader.FABRIC;
            case NEOFORGE -> UpdateLoader.NEOFORGE;
        };
    }

    private static UpdateProvider mapPlatform(UpdatePlatform platform) {
        return switch (platform) {
            case MODRINTH -> UpdateProvider.MODRINTH;
            case CURSEFORGE -> UpdateProvider.CURSEFORGE;
        };
    }

    /**
     * {@code releases} and {@code errors} only contain platforms that were queried;
     * neither holds null values.
     */
    public record UpdateCheckResult(
            String currentVersion,
            String latestVersion,
            Map<UpdateProvider, ProviderRelease> releases,
            Map<UpdateProvider, String> errors) {
        public UpdateCheckResult {
            releases = Map.copyOf(releases);
            errors = Map.copyOf(errors);
        }
    }
}
