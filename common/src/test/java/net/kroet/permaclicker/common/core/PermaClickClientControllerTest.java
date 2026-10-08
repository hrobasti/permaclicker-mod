package net.kroet.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kroet.permaclicker.common.config.PermaClickConfig;
import net.kroet.permaclicker.common.config.UpdateChannel;
import org.junit.jupiter.api.Test;

class PermaClickClientControllerTest {
    @Test
    void tickTriggersMiningWhenEnabledAndReady() {
        PermaClickClientController controller = new PermaClickClientController();
        RuntimeStub runtime = new RuntimeStub();

        controller.applyConfig(new PermaClickConfig(false, true, "green", true, 0, true, true, UpdateChannel.BETA));
        controller.toggle(runtime);
        controller.onClientTick(runtime);

        assertTrue(runtime.miningCalls > 0);
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
        public void showStatus(StatusOverlay status, String colorName) {
            // no-op
        }
    }
}
