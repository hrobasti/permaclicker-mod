package net.kroet.permaclicker.common.core;

/**
 * Client-side event loop adapter. Shared verbatim between Fabric and NeoForge;
 * loader modules call it from their own event subscribers.
 */
public final class PermaClickClientEventLoop {
    private final PermaClickBridge bridge;

    public PermaClickClientEventLoop(PermaClickBridge bridge) {
        this.bridge = bridge;
    }

    public void onTogglePressed() {
        bridge.toggle();
    }

    public void onClientTick() {
        bridge.onClientTick();
    }
}
