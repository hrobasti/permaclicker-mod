package com.github.hrobasti.permaclicker.fabric;

/**
 * Fabric-side event loop adapter.
 */
public final class FabricClientEventLoop {
    private final FabricPermaClickBridge bridge;

    public FabricClientEventLoop(FabricPermaClickBridge bridge) {
        this.bridge = bridge;
    }

    public void onKeyInput(int keyCode, boolean pressed) {
        bridge.onKeyStateChanged(keyCode, pressed);
    }

    public void onClientTick() {
        bridge.onClientTick();
    }
}

