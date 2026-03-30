package com.github.hrobasti.permaclicker.common.facade;

import com.github.hrobasti.turtlelib.LangLoader.LangLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Single facade for all locale operations delegated to TurtleLib LangLoader.
 */
public final class LocaleFacade {
    private LocaleFacade() {
    }

    public static String defaultLocale() {
        return LangLoader.DEFAULT_LOCALE;
    }

    public static String resolveSupportedLocale(String gameLocaleTag, List<String> supportedLocales) {
        if (supportedLocales == null || supportedLocales.isEmpty()) {
            return LangLoader.DEFAULT_LOCALE;
        }

        String normalized = normalizeGameLocale(gameLocaleTag);
        if (supportedLocales.contains(normalized)) {
            return normalized;
        }

        String languagePrefix = normalized.length() >= 2 ? normalized.substring(0, 2).toLowerCase(Locale.ROOT) : "en";
        for (String candidate : supportedLocales) {
            if (candidate != null && candidate.length() >= 2 && candidate.substring(0, 2).equalsIgnoreCase(languagePrefix)) {
                return candidate;
            }
        }

        if (supportedLocales.contains(LangLoader.DEFAULT_LOCALE)) {
            return LangLoader.DEFAULT_LOCALE;
        }

        return supportedLocales.get(0);
    }

    public static Properties loadMessages(Class<?> resourceAnchor, Path langDir, String gameLocaleTag) {
        List<String> available = LangLoader.getAvailableLocales(resourceAnchor, langDir);
        String resolvedLocale = resolveSupportedLocale(gameLocaleTag, available);
        return LangLoader.loadLocale(resourceAnchor, langDir, resolvedLocale);
    }

    public static String normalizeGameLocale(String gameLocaleTag) {
        if (gameLocaleTag == null || gameLocaleTag.isBlank()) {
            return LangLoader.DEFAULT_LOCALE;
        }

        String normalized = gameLocaleTag.trim();
        if (normalized.contains("-")) {
            normalized = normalized.replace('-', '_');
        }

        String[] parts = normalized.split("_");
        if (parts.length == 2) {
            return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
        }

        if (parts.length == 1 && parts[0].length() == 2) {
            return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[0].toUpperCase(Locale.ROOT);
        }

        return LangLoader.DEFAULT_LOCALE;
    }
}
