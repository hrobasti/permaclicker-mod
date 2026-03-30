package com.github.hrobasti.permaclicker.common.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.hrobasti.permaclicker.common.core.PermaClickRuntime;
import com.github.hrobasti.permaclicker.common.core.PermaClickService;
import org.junit.jupiter.api.Test;

class ToggleKeyBindingTest {
    @Test
    void togglesOnlyOnRisingEdge() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();
        ToggleKeyBinding binding = new ToggleKeyBinding(292);

        binding.onKeyEvent(292, true, service, runtime);
        assertEquals(1, runtime.overlayCalls);
        binding.onKeyEvent(292, true, service, runtime);
        assertEquals(1, runtime.overlayCalls);

        binding.onKeyEvent(292, false, service, runtime);
        binding.onKeyEvent(292, true, service, runtime);
        assertEquals(2, runtime.overlayCalls);
    }

    @Test
    void ignoresOtherKeys() {
        PermaClickService service = new PermaClickService();
        RuntimeStub runtime = new RuntimeStub();
        ToggleKeyBinding binding = new ToggleKeyBinding(292);

        binding.onKeyEvent(65, true, service, runtime);

        assertFalse(service.isEnabled());
        assertEquals(0, runtime.overlayCalls);
    }

    private static final class RuntimeStub implements PermaClickRuntime {
        private int overlayCalls;

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
            return false;
        }

        @Override
        public boolean performMiningTick() {
            // no-op
            return false;
        }

        @Override
        public void showActionBar(String translationKey) {
            overlayCalls++;
        }
    }
}

