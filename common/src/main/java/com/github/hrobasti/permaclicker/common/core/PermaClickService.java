package com.github.hrobasti.permaclicker.common.core;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;

/**
 * Core state machine for toggling and mining loop execution.
 */
public final class PermaClickService {
    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final String STATUS_ACTIVE = "PermaClicker active";
    private static final String STATUS_INACTIVE = "PermaClicker inactive";

    private boolean enabled;
    private PermaClickConfig config;
    private int remainingTicks;
    private boolean movementLockActive;
    private boolean backgroundCursorFreeActive;
    private int overlayRefreshTicks;
    private String lastActionBarMessage;

    public PermaClickService() {
        this.enabled = false;
        this.config = PermaClickConfig.defaults();
        this.remainingTicks = -1;
        this.movementLockActive = false;
        this.backgroundCursorFreeActive = false;
        this.overlayRefreshTicks = 0;
        this.lastActionBarMessage = null;
    }

    public void applyConfig(PermaClickConfig newConfig) {
        this.config = newConfig;
        this.enabled = newConfig.enabled();

        if (!this.enabled) {
            remainingTicks = -1;
            resetOverlayCache();
            return;
        }

        resetTimerForCurrentState();
        resetOverlayCache();
    }

    public PermaClickConfig currentConfig() {
        return config.withEnabled(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle(PermaClickRuntime runtime) {
        enabled = !enabled;
        if (enabled) {
            resetTimerForCurrentState();
            ensureRuntimeInputState(runtime);
            resetOverlayCache();
        } else {
            remainingTicks = -1;
            ensureRuntimeInputState(runtime);
            resetOverlayCache();
        }
        showToggleMessage(runtime);
    }

    public void tick(PermaClickRuntime runtime) {
        ensureRuntimeInputState(runtime);

        if (!enabled) {
            return;
        }

        boolean runInBackground = config.runWhenUnfocused() || config.runWhenMinimized();

        overlayRefreshTicks++;
        showActiveOverlay(runtime, false);

        if (!runInBackground && !runtime.isGameFocused()) {
            return;
        }

        if (!runInBackground && runtime.isGameMinimized()) {
            return;
        }

        if (!runtime.isPlayerReadyForMining()) {
            return;
        }

        boolean miningPerformed = runtime.performMiningTick();
        if (!miningPerformed) {
            return;
        }

        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                enabled = false;
                ensureRuntimeInputState(runtime);
                showToggleMessage(runtime);
            }
        }
    }

    public void shutdown(PermaClickRuntime runtime) {
        enabled = false;
        remainingTicks = -1;
        ensureRuntimeInputState(runtime);
        resetOverlayCache();
    }

    private void resetTimerForCurrentState() {
        if (config.autoStopMinutes() <= 0) {
            remainingTicks = -1;
            return;
        }
        long ticks = (long) config.autoStopMinutes() * TICKS_PER_SECOND * SECONDS_PER_MINUTE;
        remainingTicks = ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ticks;
    }

    private void ensureRuntimeInputState(PermaClickRuntime runtime) {
        boolean shouldMovementLock = enabled && config.movementLockEnabled();
        if (shouldMovementLock != movementLockActive) {
            runtime.applyMovementLock(shouldMovementLock);
            movementLockActive = shouldMovementLock;
        }

        boolean shouldFreeCursor = enabled && (config.runWhenUnfocused() || config.runWhenMinimized());
        if (shouldFreeCursor != backgroundCursorFreeActive) {
            runtime.setBackgroundCursorFree(shouldFreeCursor);
            backgroundCursorFreeActive = shouldFreeCursor;
        }
    }


    private void showToggleMessage(PermaClickRuntime runtime) {
        if (!config.overlayEnabled()) {
            return;
        }

        if (enabled) {
            showActiveOverlay(runtime, true);
            return;
        }

        runtime.showActionBarText(STATUS_INACTIVE, config.overlayColor());

        resetOverlayCache();
    }

    private void showActiveOverlay(PermaClickRuntime runtime, boolean force) {
        if (!config.overlayEnabled() || !enabled) {
            return;
        }

        String timer = formatTimerText();
        boolean dueByCadence = force || overlayRefreshTicks >= TICKS_PER_SECOND;

        String message = timer == null ? STATUS_ACTIVE : STATUS_ACTIVE + " (" + timer + ")";
        boolean changed = !message.equals(lastActionBarMessage);
        if (!dueByCadence && !changed) {
            return;
        }

        runtime.showActionBarText(message, config.overlayColor());
        overlayRefreshTicks = 0;
        lastActionBarMessage = message;
    }

    private String formatTimerText() {
        if (remainingTicks < 0) {
            return null;
        }

        int totalSeconds = (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        int secondsPerHour = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
        int secondsPerDay = secondsPerHour * HOURS_PER_DAY;

        int days = totalSeconds / secondsPerDay;
        int remainder = totalSeconds % secondsPerDay;
        int hours = remainder / secondsPerHour;
        remainder %= secondsPerHour;
        int minutes = remainder / SECONDS_PER_MINUTE;
        int seconds = remainder % SECONDS_PER_MINUTE;

        if (days > 0) {
            return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void resetOverlayCache() {
        overlayRefreshTicks = 0;
        lastActionBarMessage = null;
    }
}

