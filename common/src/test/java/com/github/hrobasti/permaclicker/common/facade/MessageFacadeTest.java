package com.github.hrobasti.permaclicker.common.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageFacadeTest {
    @TempDir
    Path tempDir;

    @Test
    void formatsAndPlainRendersMessagesViaFacade() throws IOException {
        Files.writeString(
            tempDir.resolve("en_US.properties"),
            "ui.prefix=[<prefix_label>]\n"
                + "greet=<prefix> Hello <name>\n"
                + "tagged=<red>Hi</red> {name}\n"
        );

        MessageFacade facade = new MessageFacade(MessageFacadeTest.class, tempDir, "[<prefix_label>]", "PermaClick");
        facade.load("en_US");
        facade.setPrefixLabel("PC");

        String formatted = facade.format("greet", Map.of("name", "Max"));
        String plain = facade.plain("tagged", Map.of("name", "Alex"));

        assertTrue(formatted.contains("[PC] Hello Max"));
        assertEquals("Hi Alex", plain);
        assertEquals("en_US", facade.getLanguage());
    }
}
