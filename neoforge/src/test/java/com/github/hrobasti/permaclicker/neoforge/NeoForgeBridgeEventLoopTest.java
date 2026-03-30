package com.github.hrobasti.permaclicker.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.hrobasti.permaclicker.common.core.PermaClickClientController;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NeoForgeBridgeEventLoopTest {
    @Test
    void keyPressAndTickTriggerMiningWhenRuntimeReady() {
        NeoForgePermaClickBridge bridge = new NeoForgePermaClickBridge(new PermaClickClientController());
        NeoForgeClientEventLoop loop = new NeoForgeClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new NeoForgeRuntimeBindings(
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
        NeoForgePermaClickBridge bridge = new NeoForgePermaClickBridge(new PermaClickClientController());
        NeoForgeClientEventLoop loop = new NeoForgeClientEventLoop(bridge);

        AtomicInteger miningCalls = new AtomicInteger();
        bridge.bind(new NeoForgeRuntimeBindings(
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

