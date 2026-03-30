package com.github.hrobasti.permaclicker.neoforge;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import com.github.hrobasti.permaclicker.common.core.PermaClickRuntime;

/**
 * NeoForge-facing bridge used by loader event handlers.
 */
public final class NeoForgePermaClickBridge {
    private final PermaClickClientController controller;
    private PermaClickRuntime runtime;

    public NeoForgePermaClickBridge(PermaClickClientController controller) {
        this.controller = controller;
        this.runtime = new BoundRuntime(NeoForgeRuntimeBindings.noop());
    }

    public void bind(NeoForgeRuntimeBindings bindings) {
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
        private final NeoForgeRuntimeBindings bindings;

        private BoundRuntime(NeoForgeRuntimeBindings bindings) {
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
            bindings.actionBarMessage().accept(new NeoForgeRuntimeBindings.ActionBarPayload(message, null));
        }

        @Override
        public void showActionBarText(String message, String colorName) {
            bindings.actionBarMessage().accept(new NeoForgeRuntimeBindings.ActionBarPayload(message, colorName));
        }
    }
}

