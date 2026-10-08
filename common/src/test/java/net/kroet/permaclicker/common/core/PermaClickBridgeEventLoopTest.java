package net.kroet.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PermaClickBridgeEventLoopTest {
    @Test
    void togglePressAndTickTriggerMiningWhenRuntimeReady() {
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        PermaClickClientEventLoop loop = new PermaClickClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new PermaClickRuntimeBindings(
                () -> true,
                () -> false,
                () -> true,
                () -> {
                    miningCalls.incrementAndGet();
                    return true;
                },
                ignored -> {
                },
                ignored -> {
                },
                ignored -> {
                }));

        loop.onTogglePressed();
        loop.onClientTick();

        assertEquals(1, miningCalls.get());
    }

    @Test
    void secondPressTogglesOff() {
        PermaClickBridge bridge = new PermaClickBridge(new PermaClickClientController());
        PermaClickClientEventLoop loop = new PermaClickClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new PermaClickRuntimeBindings(
                () -> true,
                () -> false,
                () -> true,
                () -> {
                    miningCalls.incrementAndGet();
                    return true;
                },
                ignored -> {
                },
                ignored -> {
                },
                ignored -> {
                }));

        loop.onTogglePressed();
        loop.onClientTick();

        loop.onTogglePressed();
        loop.onClientTick();

        assertEquals(1, miningCalls.get());
    }
}
