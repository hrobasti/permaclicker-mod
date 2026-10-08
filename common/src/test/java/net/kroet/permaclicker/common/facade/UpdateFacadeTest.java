package net.kroet.permaclicker.common.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kroet.permaclicker.common.config.UpdateChannel;
import net.kroet.permaclicker.common.update.ModLoader;
import org.junit.jupiter.api.Test;

class UpdateFacadeTest {
    @Test
    void withoutProjectIdsNothingIsQueried() {
        UpdateFacade.UpdateCheckResult result = UpdateFacade.check("1.0.0", "26.3", UpdateChannel.BETA, ModLoader.FABRIC, "", null);

        assertEquals("1.0.0", result.currentVersion());
        assertNull(result.latestVersion());
        assertTrue(result.releases().isEmpty());
        assertTrue(result.errors().isEmpty());
    }
}
