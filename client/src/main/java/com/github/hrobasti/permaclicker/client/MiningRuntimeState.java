package com.github.hrobasti.permaclicker.client;

import net.minecraft.client.Minecraft;

/**
 * Holds the focused-/background-mode mining executors plus the tick-scoped state that glues
 * them into the client tick loop (attack-hold bookkeeping, background cursor-free mode, pause
 * screen suppression). Shared between Fabric and NeoForge; each loader entrypoint owns one
 * instance.
 */
public final class MiningRuntimeState {
    private final FocusedModeExecutor focusedModeExecutor = new FocusedModeExecutor();
    private final BackgroundModeExecutor backgroundModeExecutor = new BackgroundModeExecutor();

    private boolean attackRequestedThisTick;
    private boolean attackHoldForcedByPermaClick;
    private boolean backgroundCursorFreeActive;
    private boolean backgroundPauseSuppressedThisTick;

    public void resetTickFlags() {
        backgroundPauseSuppressedThisTick = false;
    }

    public void markAttackNotRequestedYet() {
        attackRequestedThisTick = false;
    }

    public boolean isBackgroundPauseSuppressedThisTick() {
        return backgroundPauseSuppressedThisTick;
    }

    public boolean performHeldAttackTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        attackRequestedThisTick = true;
        attackHoldForcedByPermaClick = true;
        if (backgroundCursorFreeActive) {
            return backgroundModeExecutor.performBackgroundMiningTick(minecraft);
        }

        return focusedModeExecutor.performFocusedMiningTick(minecraft);
    }

    public void syncAttackHoldState(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (!attackRequestedThisTick) {
            if (attackHoldForcedByPermaClick) {
                minecraft.options.keyAttack.setDown(false);
                attackHoldForcedByPermaClick = false;
            }
            if (!backgroundCursorFreeActive) {
                backgroundModeExecutor.reset();
            }
        }
    }

    public void setBackgroundCursorFreeActive(Minecraft minecraft, boolean active, PauseOnLostFocusOverride pauseOverride) {
        if (active != backgroundCursorFreeActive && !active) {
            backgroundModeExecutor.reset();
        }

        backgroundCursorFreeActive = active;
        pauseOverride.update(minecraft, active);
    }

    public void suppressBackgroundPauseScreen(Minecraft minecraft) {
        if (!backgroundCursorFreeActive || minecraft == null) {
            return;
        }
        boolean suppressed = backgroundModeExecutor.aggressivelySuppressPauseScreen(minecraft);
        if (suppressed) {
            backgroundPauseSuppressedThisTick = true;
        }
    }

    public void resetOnShutdown() {
        attackHoldForcedByPermaClick = false;
        backgroundCursorFreeActive = false;
        backgroundPauseSuppressedThisTick = false;
        backgroundModeExecutor.reset();
    }
}
