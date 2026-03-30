package com.github.hrobasti.permaclicker.common.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocaleFacadeTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizesLocaleAndFallsBackToDefault() {
        assertEquals(LocaleFacade.defaultLocale(), LocaleFacade.normalizeGameLocale(null));
        assertEquals("de_DE", LocaleFacade.normalizeGameLocale("de-de"));
        assertEquals("fr_FR", LocaleFacade.normalizeGameLocale("fr_fr"));
        assertEquals(LocaleFacade.defaultLocale(), LocaleFacade.normalizeGameLocale("invalid_locale_value"));
    }

    @Test
    void resolvesExactLanguageMatchAndDefault() {
        assertEquals("de_DE", LocaleFacade.resolveSupportedLocale("de_de", List.of("en_US", "de_DE")));
        assertEquals("de_DE", LocaleFacade.resolveSupportedLocale("de_AT", List.of("en_US", "de_DE")));
        assertEquals("en_US", LocaleFacade.resolveSupportedLocale("fr_FR", List.of("en_US", "de_DE")));
    }

    @Test
    void loadsMessagesFromConfiguredLocaleDirectory() throws IOException {
        Files.writeString(tempDir.resolve("en_US.properties"), "greeting=Hello\n");
        Files.writeString(tempDir.resolve("de_DE.properties"), "greeting=Hallo\n");

        Properties props = LocaleFacade.loadMessages(LocaleFacadeTest.class, tempDir, "de_de");

        assertEquals("Hallo", props.getProperty("greeting"));
    }
}
