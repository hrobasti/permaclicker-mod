package net.kroet.permaclicker.common.i18n;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import net.kroet.permaclicker.common.facade.LocaleFacade;

/**
 * Locale helpers for PermaClick based on TurtleLib LangLoader.
 */
public final class PermaClickLocaleService {
    private PermaClickLocaleService() {
    }

    public static String resolveSupportedLocale(String gameLocaleTag, List<String> supportedLocales) {
        return LocaleFacade.resolveSupportedLocale(gameLocaleTag, supportedLocales);
    }

    public static Properties loadMessages(Class<?> resourceAnchor, Path langDir, String gameLocaleTag) {
        return LocaleFacade.loadMessages(resourceAnchor, langDir, gameLocaleTag);
    }

    public static String normalizeGameLocale(String gameLocaleTag) {
        return LocaleFacade.normalizeGameLocale(gameLocaleTag);
    }
}
