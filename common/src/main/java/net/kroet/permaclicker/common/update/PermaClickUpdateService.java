package net.kroet.permaclicker.common.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import net.kroet.permaclicker.common.config.UpdateChannel;
import net.kroet.permaclicker.common.facade.UpdateFacade;
import net.kroet.permaclicker.common.facade.UpdateFacade.UpdateCheckResult;

/**
 * Runs the TurtleLib update check with the bundled project ids and prepares the
 * result for the chat notice.
 */
public final class PermaClickUpdateService {
    private static final String UPDATE_SOURCES_RESOURCE = "permaclicker-update-sources.properties";

    private PermaClickUpdateService() {
    }

    /** Blocking: call from a background thread. */
    public static UpdateStatus checkForUpdates(
            String currentVersion,
            String minecraftVersion,
            UpdateChannel updateChannel,
            ModLoader loader) {
        UpdateSources sources = loadBundledSources();
        if (isBlank(sources.modrinthProjectId()) && isBlank(sources.curseforgeProjectId())) {
            return UpdateStatus.notConfigured();
        }

        UpdateCheckResult result = UpdateFacade.check(
                currentVersion,
                minecraftVersion,
                updateChannel,
                loader,
                sources.modrinthProjectId(),
                sources.curseforgeProjectId());
        return toStatus(result, sources);
    }

    static UpdateStatus toStatus(UpdateCheckResult result, UpdateSources sources) {
        Map<UpdateProvider, ProviderRelease> releases = new EnumMap<>(UpdateProvider.class);
        result.releases().forEach((provider, release) -> releases.put(
                provider,
                new ProviderRelease(release.version(), release.url() != null ? release.url() : sources.pageUrl(provider))));
        return new UpdateStatus(result.currentVersion(), result.latestVersion(), releases, result.errors());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static UpdateSources loadBundledSources() {
        Properties props = new Properties();
        try (InputStream in = PermaClickUpdateService.class.getClassLoader().getResourceAsStream(UPDATE_SOURCES_RESOURCE)) {
            if (in == null) {
                return UpdateSources.empty();
            }
            props.load(in);
        } catch (IOException e) {
            return UpdateSources.empty();
        }

        return new UpdateSources(
                props.getProperty("modrinth.url"),
                props.getProperty("modrinth.projectId"),
                props.getProperty("curseforge.url"),
                props.getProperty("curseforge.projectId"));
    }

    record UpdateSources(String modrinthUrl, String modrinthProjectId, String curseforgeUrl, String curseforgeProjectId) {
        static UpdateSources empty() {
            return new UpdateSources(null, null, null, null);
        }

        String pageUrl(UpdateProvider provider) {
            String url = provider == UpdateProvider.MODRINTH ? modrinthUrl : curseforgeUrl;
            return isBlank(url) ? null : url.trim();
        }
    }

    /**
     * {@code releases} and {@code errors} only contain platforms that were queried;
     * neither holds null values.
     */
    public record UpdateStatus(
            String currentVersion,
            String latestVersion,
            Map<UpdateProvider, ProviderRelease> releases,
            Map<UpdateProvider, String> errors) {
        public UpdateStatus {
            releases = Map.copyOf(releases);
            errors = Map.copyOf(errors);
        }

        public static UpdateStatus notConfigured() {
            return new UpdateStatus(null, null, Map.of(), Map.of());
        }

        public boolean hasUpdate() {
            return latestVersion != null && !latestVersion.isBlank();
        }
    }
}
