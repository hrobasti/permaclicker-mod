package com.github.hrobasti.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import org.junit.jupiter.api.Test;

class PermaClickServiceTest {
    @Test
    void togglesAndEmitsOverlayMessage() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        assertTrue(service.isEnabled());
        assertTrue(runtime.actionBarShown);
        assertEquals("PermaClicker active", runtime.lastActionBarMessage);
        assertEquals("green", runtime.lastActionBarColor);
    }

    @Test
    void overlayUsesConfiguredColor() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "red", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        assertEquals("red", runtime.lastActionBarColor);
    }

    @Test
    void activeOverlayStaysVisibleAndShowsActionBarTimer() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, true, 1, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertEquals("PermaClicker active (00:01:00)", runtime.lastActionBarMessage);
    }

    @Test
    void actionBarTimerShowsHoursWhenNeeded() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, true, 90, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertEquals("PermaClicker active (01:30:00)", runtime.lastActionBarMessage);
    }

    @Test
    void actionBarTimerShowsDaysWhenNeeded() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, true, 1500, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertEquals("PermaClicker active (01:01:00:00)", runtime.lastActionBarMessage);
    }

    @Test
    void doesNotMineWhenMinimizedInFocusedOnlyMode() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = true;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertFalse(runtime.miningPerformed);
    }

    @Test
    void minesWhenMinimizedInBackgroundMode() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = false;
        runtime.minimized = true;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertTrue(runtime.miningPerformed);
    }

    @Test
    void minesWhenEnabledAndReady() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertTrue(runtime.miningPerformed);
    }

    @Test
    void configCanStartServiceEnabledWithoutManualToggle() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(true, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.tick(runtime);

        assertTrue(service.isEnabled());
        assertTrue(runtime.miningPerformed);
    }

    @Test
    void timerAutoDisablesAndUnlocksCursor() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, true, 1, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        for (int i = 0; i < 1200; i++) {
            service.tick(runtime);
        }

        assertFalse(service.isEnabled());
        assertTrue(runtime.movementLockTransitions > 1);
    }

    @Test
    void toggleOnLocksCursorWhenEnabledInConfig() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        assertTrue(runtime.movementLockActive);
    }

    @Test
    void shutdownAlwaysUnlocksCursor() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.shutdown(runtime);

        assertFalse(service.isEnabled());
        assertFalse(runtime.movementLockActive);
    }

    @Test
    void backgroundModeRequestsCursorFreeWhileActive() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertTrue(runtime.backgroundCursorFreeActive);
        service.toggle(runtime);
        assertFalse(runtime.backgroundCursorFreeActive);
    }

    @Test
    void focusedModeDoesNotRequestBackgroundCursorFreeEvenWithMovementLock() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 0, true, true, UpdateChannel.BETA));
        service.toggle(runtime);
        service.tick(runtime);

        assertTrue(runtime.movementLockActive);
        assertFalse(runtime.backgroundCursorFreeActive);
    }

    @Test
    void timerDoesNotTickDownWhileMiningIsPausedByRuntimeConditions() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = false;
        runtime.minimized = false;
        runtime.playerReady = true;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", false, false, 1, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        for (int i = 0; i < 200; i++) {
            service.tick(runtime);
        }

        assertTrue(service.isEnabled());
        assertFalse(runtime.miningPerformed);
        assertEquals("PermaClicker active (00:01:00)", runtime.lastActionBarMessage);
    }

    @Test
    void timerDoesNotTickDownWhenRuntimeCannotPerformMiningTick() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();

        runtime.focused = true;
        runtime.minimized = false;
        runtime.playerReady = true;
        runtime.miningTickSucceeds = false;

        service.applyConfig(new PermaClickConfig(false, 292, true, "green", true, false, 1, true, true, UpdateChannel.BETA));
        service.toggle(runtime);

        for (int i = 0; i < 200; i++) {
            service.tick(runtime);
        }

        assertTrue(service.isEnabled());
        assertFalse(runtime.miningPerformed);
        assertEquals("PermaClicker active (00:01:00)", runtime.lastActionBarMessage);
    }

    private static final class RuntimeStub implements PermaClickRuntime {
        private boolean focused = true;
        private boolean minimized = false;
        private boolean playerReady = false;
        private boolean miningPerformed = false;
        private boolean actionBarShown = false;
        private String lastActionBarMessage;
        private String lastActionBarColor;
        private boolean movementLockActive = false;
        private int movementLockTransitions = 0;
        private boolean backgroundCursorFreeActive = false;
        private boolean miningTickSucceeds = true;

        @Override
        public boolean isGameFocused() {
            return focused;
        }

        @Override
        public boolean isGameMinimized() {
            return minimized;
        }

        @Override
        public boolean isPlayerReadyForMining() {
            return playerReady;
        }

        @Override
        public boolean performMiningTick() {
            miningPerformed = miningTickSucceeds;
            return miningTickSucceeds;
        }

        @Override
        public void applyMovementLock(boolean active) {
            movementLockActive = active;
            movementLockTransitions++;
        }

        @Override
        public void setBackgroundCursorFree(boolean active) {
            backgroundCursorFreeActive = active;
        }

        @Override
        public void showActionBar(String message) {
            actionBarShown = true;
            lastActionBarMessage = message;
        }

        @Override
        public void showActionBarText(String message, String colorName) {
            actionBarShown = true;
            lastActionBarMessage = message;
            lastActionBarColor = colorName;
        }
    }
}

