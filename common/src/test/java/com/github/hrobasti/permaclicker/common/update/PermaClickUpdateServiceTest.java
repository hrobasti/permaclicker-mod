package com.github.hrobasti.permaclicker.common.update;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hrobasti.permaclicker.common.facade.UpdateFacade.UpdateCheckResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PermaClickUpdateServiceTest {
    @Test
    void detectsOnlyStrictlyGreaterVersions() {
        UpdateCheckResult newer = new UpdateCheckResult("1.2.0", "1.3.0", Map.of("modrinth", "1.3.0"));
        assertTrue(PermaClickUpdateService.hasActualUpdate(newer));

        UpdateCheckResult equal = new UpdateCheckResult("1.2.0", "1.2.0", Map.of("modrinth", "1.2.0"));
        assertFalse(PermaClickUpdateService.hasActualUpdate(equal));

        UpdateCheckResult older = new UpdateCheckResult("1.2.0", "1.1.9", Map.of("modrinth", "1.1.9"));
        assertFalse(PermaClickUpdateService.hasActualUpdate(older));
    }

    @Test
    void noUpdateStatusKeepsProviderSlotsButHasNoUpdateFlag() {
        PermaClickUpdateService.UpdateStatus status = PermaClickUpdateService.UpdateStatus.noUpdate(
            Map.of("modrinth", "1.2.0", "curseforge", "1.2.1")
        );

        assertFalse(status.hasUpdate());
        assertTrue(status.currentVersion() == null || status.currentVersion().isBlank());
        assertTrue(status.latestVersion() == null || status.latestVersion().isBlank());
        assertTrue(status.providerVersions().containsKey("modrinth"));
        assertTrue(status.providerVersions().containsKey("curseforge"));
        assertTrue(status.providerVersionUrls().containsKey("modrinth"));
        assertTrue(status.providerVersionUrls().containsKey("curseforge"));
    }
}

