package com.github.hrobasti.permaclicker.neoforge;

/**
 * NeoForge-side event loop adapter.
 *
 * This class is intentionally loader-API-agnostic in v0.1 core wiring and is
 * meant to be called from actual NeoForge event subscribers.
 */
public final class NeoForgeClientEventLoop {
    private final NeoForgePermaClickBridge bridge;

    public NeoForgeClientEventLoop(NeoForgePermaClickBridge bridge) {
        this.bridge = bridge;
    }

    public void onKeyInput(int keyCode, boolean pressed) {
        bridge.onKeyStateChanged(keyCode, pressed);
    }

    public void onClientTick() {
        bridge.onClientTick();
    }
}

