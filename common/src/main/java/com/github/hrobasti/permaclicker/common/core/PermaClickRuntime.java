package com.github.hrobasti.permaclicker.common.core;

public interface PermaClickRuntime {
    boolean isGameFocused();

    boolean isGameMinimized();

    boolean isPlayerReadyForMining();

    boolean performMiningTick();

    default void lockCursor() {
        // optional runtime hook
    }

    default void unlockCursor() {
        // optional runtime hook
    }

    default void applyMovementLock(boolean active) {
        // optional runtime hook
    }

    default void setBackgroundCursorFree(boolean active) {
        // optional runtime hook
    }

    void showActionBar(String message);

    default void showActionBarText(String message) {
        showActionBarText(message, null);
    }

    default void showActionBarText(String message, String colorName) {
        showActionBar(message);
    }
}

