package net.kroet.permaclicker.common.core;

import net.kroet.permaclicker.common.config.ClickMode;
import net.kroet.permaclicker.common.config.PermaClickConfig;

/**
 * Core state machine for toggling and mining loop execution.
 *
 * <p>
 * Client-thread-only: all fields are plain (non-volatile, unsynchronized) and
 * assume every call comes from the client tick/render thread, like the existing
 * loader entrypoints do. Do not touch this state from an async callback (e.g. a
 * network response) without adding explicit synchronization or routing through
 * a volatile hand-off field first — see how the loader entrypoints stage
 * {@code pendingUpdateStatus} for the update-check background thread.
 * </p>
 */
public final class PermaClickService {
    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;

    private boolean enabled;
    private PermaClickConfig config;
    private int remainingTicks;
    private boolean movementLockActive;
    private boolean backgroundCursorFreeActive;
    private int overlayRefreshTicks;
    private StatusOverlay lastStatus;

    public PermaClickService() {
        this.enabled = false;
        this.config = PermaClickConfig.defaults();
        this.remainingTicks = -1;
        this.movementLockActive = false;
        this.backgroundCursorFreeActive = false;
        this.overlayRefreshTicks = 0;
        this.lastStatus = null;
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

        boolean runInBackground = config.runInBackground();

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
        // Mob attack mostly waits for cooldown or target, so every active tick
        // counts towards auto-stop; mining counts successful ticks only.
        boolean countsTowardsAutoStop = miningPerformed || config.clickMode() == ClickMode.MOB_ATTACK;
        if (!countsTowardsAutoStop) {
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

        boolean shouldFreeCursor = enabled && config.runInBackground();
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

        runtime.showStatus(StatusOverlay.inactive(config.clickMode()), config.overlayColor());

        resetOverlayCache();
    }

    private void showActiveOverlay(PermaClickRuntime runtime, boolean force) {
        if (!config.overlayEnabled() || !enabled) {
            return;
        }

        String timer = formatTimerText();
        boolean dueByCadence = force || overlayRefreshTicks >= TICKS_PER_SECOND;

        StatusOverlay status = new StatusOverlay(true, config.clickMode(), timer);
        boolean changed = !status.equals(lastStatus);
        if (!dueByCadence && !changed) {
            return;
        }

        runtime.showStatus(status, config.overlayColor());
        overlayRefreshTicks = 0;
        lastStatus = status;
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
        lastStatus = null;
    }
}
