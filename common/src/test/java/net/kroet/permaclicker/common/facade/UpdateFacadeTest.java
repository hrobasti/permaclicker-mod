package net.kroet.permaclicker.common.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.kroet.permaclicker.common.config.UpdateChannel;
import org.junit.jupiter.api.Test;

class UpdateFacadeTest {
    @Test
    void checkGracefullyReturnsFallbackWhenProvidersFailOrAreUnavailable() {
        UpdateFacade.UpdateCheckResult result = UpdateFacade.check(
                "1.0.0",
                "1.21.1",
                UpdateChannel.BETA,
                "definitely-invalid-project-id",
                "also-invalid");

        assertEquals("1.0.0", result.currentVersion());
        assertNotNull(result.providerVersions());
    }
}
