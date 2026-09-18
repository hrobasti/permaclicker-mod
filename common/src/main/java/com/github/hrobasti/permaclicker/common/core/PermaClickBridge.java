package com.github.hrobasti.permaclicker.common.core;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;

/**
 * Loader-facing bridge used by client event handlers. Shared verbatim between Fabric and
 * NeoForge; loader modules only ever supply {@link PermaClickRuntimeBindings}.
 */
public final class PermaClickBridge {
    private final PermaClickClientController controller;
    private PermaClickRuntime runtime;

    public PermaClickBridge(PermaClickClientController controller) {
        this.controller = controller;
        this.runtime = new BoundRuntime(PermaClickRuntimeBindings.noop());
    }

    public void bind(PermaClickRuntimeBindings bindings) {
        this.runtime = new BoundRuntime(bindings);
    }

    public void applyConfig(PermaClickConfig config) {
        controller.applyConfig(config);
    }

    public PermaClickConfig currentConfig() {
        return controller.currentConfig();
    }

    public void setBoundKeyCode(int keyCode) {
        controller.setBoundKeyCode(keyCode);
    }

    public int boundKeyCode() {
        return controller.boundKeyCode();
    }

    public void onKeyStateChanged(int keyCode, boolean pressed) {
        controller.onKeyEvent(keyCode, pressed, runtime);
    }

    public void onClientTick() {
        controller.onClientTick(runtime);
    }

    public void onClientShutdown() {
        controller.onClientShutdown(runtime);
    }

    public void stopIfEnabled() {
        if (!controller.service().isEnabled()) {
            return;
        }

        controller.service().toggle(runtime);
    }

    private static final class BoundRuntime implements PermaClickRuntime {
        private final PermaClickRuntimeBindings bindings;

        private BoundRuntime(PermaClickRuntimeBindings bindings) {
            this.bindings = bindings;
        }

        @Override
        public boolean isGameFocused() {
            return bindings.focusedSupplier().get();
        }

        @Override
        public boolean isGameMinimized() {
            return bindings.minimizedSupplier().get();
        }

        @Override
        public boolean isPlayerReadyForMining() {
            return bindings.miningReadySupplier().get();
        }

        @Override
        public boolean performMiningTick() {
            return bindings.miningTickAction().getAsBoolean();
        }

        @Override
        public void applyMovementLock(boolean active) {
            bindings.applyMovementLock(active);
        }

        @Override
        public void setBackgroundCursorFree(boolean active) {
            bindings.setBackgroundCursorFree(active);
        }

        @Override
        public void showActionBar(String message) {
            bindings.actionBarMessage().accept(new PermaClickRuntimeBindings.ActionBarPayload(message, null));
        }

        @Override
        public void showActionBarText(String message, String colorName) {
            bindings.actionBarMessage().accept(new PermaClickRuntimeBindings.ActionBarPayload(message, colorName));
        }
    }
}
