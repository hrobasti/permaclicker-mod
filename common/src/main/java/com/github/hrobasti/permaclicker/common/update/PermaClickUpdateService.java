package com.github.hrobasti.permaclicker.common.update;

import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import com.github.hrobasti.permaclicker.common.facade.UpdateFacade;
import com.github.hrobasti.permaclicker.common.facade.UpdateFacade.UpdateCheckResult;
import com.github.hrobasti.permaclicker.common.facade.VersionFacade;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Integrates TurtleLib update + locale helpers for PermaClick.
 */
public final class PermaClickUpdateService {
    private static final String UPDATE_SOURCES_RESOURCE = "permaclicker-update-sources.properties";
    private static final String PROVIDER_MODRINTH = "modrinth";
    private static final String PROVIDER_CURSEFORGE = "curseforge";

    private PermaClickUpdateService() {
    }

    public static UpdateStatus checkForUpdates(
        String currentVersion,
        String minecraftVersion,
        UpdateChannel updateChannel
    ) {
        UpdateSources sources = loadBundledSources();
        String modrinthProjectId = sources.modrinthProjectId();
        String curseforgeProjectId = sources.curseforgeProjectId();
        if (isBlank(modrinthProjectId) && isBlank(curseforgeProjectId)) {
            return UpdateStatus.notConfigured();
        }

        UpdateCheckResult result = UpdateFacade.check(
            currentVersion,
            minecraftVersion,
            updateChannel,
            modrinthProjectId,
            curseforgeProjectId
        );
        boolean hasUpdate = hasActualUpdate(result);
        if (!hasUpdate) {
            return UpdateStatus.noUpdate(result.providerVersions());
        }

        return new UpdateStatus(
            true,
            safeVersion(currentVersion),
            result.latestVersion(),
            normalizeProviderVersions(result.providerVersions()),
            providerMap(
                PROVIDER_MODRINTH,
                buildProviderVersionUrl(
                    PROVIDER_MODRINTH,
                    sources.modrinthUrl(),
                    safeToNullableString(result.providerVersions().get(PROVIDER_MODRINTH))
                ),
                PROVIDER_CURSEFORGE,
                buildProviderVersionUrl(
                    PROVIDER_CURSEFORGE,
                    sources.curseforgeUrl(),
                    safeToNullableString(result.providerVersions().get(PROVIDER_CURSEFORGE))
                )
            )
        );
    }

    public static boolean hasActualUpdate(UpdateCheckResult result) {
        if (result == null || result.latestVersion() == null || result.latestVersion().isBlank()) {
            return false;
        }
        return VersionFacade.isGreater(result.latestVersion(), safeVersion(result.currentVersion()));
    }

    private static String safeVersion(String version) {
        return (version == null || version.isBlank()) ? "0.0.0" : version;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String sanitized = baseUrl.trim();
        while (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.isBlank() ? null : sanitized;
    }

    private static String encodeUrlPart(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    private static String buildProviderVersionUrl(String providerKey, String providerBaseUrl, String version) {
        String baseUrl = sanitizeBaseUrl(providerBaseUrl);
        if (baseUrl == null) {
            return null;
        }
        String encodedVersion = encodeUrlPart(version);
        if (encodedVersion == null) {
            return baseUrl;
        }

        return switch (providerKey) {
            case PROVIDER_MODRINTH -> baseUrl + "/version/" + encodedVersion;
            case PROVIDER_CURSEFORGE -> baseUrl + "/files?version=" + encodedVersion;
            default -> baseUrl;
        };
    }

    private static Map<String, String> normalizeProviderVersions(Map<?, ?> rawVersions) {
        return Map.of(
            PROVIDER_MODRINTH,
            safeToNullableString(rawVersions.get(PROVIDER_MODRINTH)),
            PROVIDER_CURSEFORGE,
            safeToNullableString(rawVersions.get(PROVIDER_CURSEFORGE))
        );
    }

    private static String safeToNullableString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private static Map<String, String> providerMap(String keyA, String valueA, String keyB, String valueB) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(keyA, valueA);
        map.put(keyB, valueB);
        return Collections.unmodifiableMap(map);
    }

    private static UpdateSources loadBundledSources() {
        Properties props = new Properties();
        try (InputStream in = PermaClickUpdateService.class.getClassLoader().getResourceAsStream(UPDATE_SOURCES_RESOURCE)) {
            if (in == null) {
                return UpdateSources.empty();
            }
            props.load(in);
        } catch (Exception ignored) {
            return UpdateSources.empty();
        }

        return new UpdateSources(
            props.getProperty("modrinth.url"),
            props.getProperty("modrinth.projectId"),
            props.getProperty("curseforge.url"),
            props.getProperty("curseforge.projectId")
        );
    }

    private record UpdateSources(String modrinthUrl, String modrinthProjectId, String curseforgeUrl, String curseforgeProjectId) {
        static UpdateSources empty() {
            return new UpdateSources(null, null, null, null);
        }
    }

    public record UpdateStatus(
        boolean hasUpdate,
        String currentVersion,
        String latestVersion,
        Map<String, String> providerVersions,
        Map<String, String> providerVersionUrls
    ) {
        public static UpdateStatus noUpdate(Map<?, ?> providerVersions) {
            return new UpdateStatus(
                false,
                null,
                null,
                normalizeProviderVersions(providerVersions),
                providerMap(PROVIDER_MODRINTH, null, PROVIDER_CURSEFORGE, null)
            );
        }

        public static UpdateStatus notConfigured() {
            return new UpdateStatus(
                false,
                null,
                null,
                providerMap(PROVIDER_MODRINTH, null, PROVIDER_CURSEFORGE, null),
                providerMap(PROVIDER_MODRINTH, null, PROVIDER_CURSEFORGE, null)
            );
        }
    }
}

