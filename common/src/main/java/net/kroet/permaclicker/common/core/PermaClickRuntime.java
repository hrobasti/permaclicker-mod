package net.kroet.permaclicker.common.core;

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

    /** Shows the action-bar status; colorName may be null for the default color. */
    void showStatus(StatusOverlay status, String colorName);
}
