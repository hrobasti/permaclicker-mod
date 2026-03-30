package com.github.hrobasti.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.config.UpdateChannel;
import org.junit.jupiter.api.Test;

class PermaClickClientControllerTest {
    @Test
    void defaultKeyCodeIsF4() {
        PermaClickClientController controller = new PermaClickClientController();
        assertEquals(PermaClickClientController.DEFAULT_F4_KEY_CODE, controller.boundKeyCode());
    }

    @Test
    void tickTriggersMiningWhenEnabledAndReady() {
        PermaClickClientController controller = new PermaClickClientController();
        RuntimeStub runtime = new RuntimeStub();

        controller.applyConfig(new PermaClickConfig(false, 292, true, "green", true, false, 0, true, true, UpdateChannel.BETA));
        controller.onKeyEvent(292, true, runtime);
        controller.onClientTick(runtime);

        assertTrue(runtime.miningCalls > 0);
    }

    @Test
    void applyConfigUpdatesBoundKeyCode() {
        PermaClickClientController controller = new PermaClickClientController();

        controller.applyConfig(new PermaClickConfig(false, 293, true, "green", true, false, 0, true, true, UpdateChannel.BETA));

        assertEquals(293, controller.boundKeyCode());
    }

    private static final class RuntimeStub implements PermaClickRuntime {
        private int miningCalls;

        @Override
        public boolean isGameFocused() {
            return true;
        }

        @Override
        public boolean isGameMinimized() {
            return false;
        }

        @Override
        public boolean isPlayerReadyForMining() {
            return true;
        }

        @Override
        public boolean performMiningTick() {
            miningCalls++;
            return true;
        }

        @Override
        public void showActionBar(String message) {
            // no-op
        }
    }
}

