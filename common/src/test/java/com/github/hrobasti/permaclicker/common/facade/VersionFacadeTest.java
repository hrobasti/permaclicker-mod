package com.github.hrobasti.permaclicker.common.facade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionFacadeTest {
    @Test
    void comparesSemanticVersionsViaFacade() {
        assertTrue(VersionFacade.compare("1.2.0", "1.1.9") > 0);
        assertTrue(VersionFacade.isGreater("1.2.0", "1.1.9"));
        assertFalse(VersionFacade.isGreater("1.2.0", "1.2.0"));
    }
}
