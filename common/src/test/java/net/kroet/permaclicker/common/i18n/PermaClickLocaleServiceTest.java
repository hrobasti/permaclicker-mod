package net.kroet.permaclicker.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kroet.permaclicker.common.facade.LocaleFacade;
import org.junit.jupiter.api.Test;

class PermaClickLocaleServiceTest {
    @Test
    void resolvesExactLocaleWhenAvailable() {
        String resolved = PermaClickLocaleService.resolveSupportedLocale("de_de", List.of("en_US", "de_DE"));
        assertEquals("de_DE", resolved);
    }

    @Test
    void fallsBackToLanguageMatchThenDefault() {
        String languageMatch = PermaClickLocaleService.resolveSupportedLocale("de_AT", List.of("en_US", "de_DE"));
        assertEquals("de_DE", languageMatch);

        String defaultLocale = PermaClickLocaleService.resolveSupportedLocale("fr_FR", List.of("en_US", "de_DE"));
        assertEquals(LocaleFacade.defaultLocale(), defaultLocale);
    }
}
