package com.github.hrobasti.permaclicker.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FabricBridgeEventLoopTest {
    @Test
    void keyPressAndTickTriggerMiningWhenRuntimeReady() {
        FabricPermaClickBridge bridge = new FabricPermaClickBridge(new PermaClickClientController());
        FabricClientEventLoop loop = new FabricClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new FabricRuntimeBindings(
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
            }
        ));

        loop.onKeyInput(bridge.boundKeyCode(), true);
        loop.onClientTick();

        assertEquals(1, miningCalls.get());
    }

    @Test
    void secondPressTogglesOff() {
        FabricPermaClickBridge bridge = new FabricPermaClickBridge(new PermaClickClientController());
        FabricClientEventLoop loop = new FabricClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new FabricRuntimeBindings(
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
            }
        ));

        int key = bridge.boundKeyCode();

        loop.onKeyInput(key, true);
        loop.onClientTick();

        loop.onKeyInput(key, false);
        loop.onKeyInput(key, true);
        loop.onClientTick();

        assertEquals(1, miningCalls.get());
    }
}

