package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.common.config.ClickMode;
import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import net.minecraft.client.Minecraft;

/**
 * Holds the focused-/background-mode mining executors and the mob-attack executor, plus the
 * tick-scoped state that glues them into the client tick loop (attack-hold bookkeeping,
 * background cursor-free mode, pause screen suppression). Shared between Fabric and NeoForge;
 * each loader entrypoint owns one instance.
 */
public final class MiningRuntimeState {
    private final FocusedModeExecutor focusedModeExecutor = new FocusedModeExecutor();
    private final BackgroundModeExecutor backgroundModeExecutor = new BackgroundModeExecutor();
    private final MobAttackExecutor mobAttackExecutor = new MobAttackExecutor();

    private boolean attackRequestedThisTick;
    private boolean attackHoldForcedByPermaClick;
    private boolean backgroundCursorFreeActive;
    private boolean backgroundPauseSuppressedThisTick;

    public void resetTickFlags() {
        backgroundPauseSuppressedThisTick = false;
        MiningTickGuard.reset();
    }

    public void markAttackNotRequestedYet() {
        attackRequestedThisTick = false;
    }

    public boolean isBackgroundPauseSuppressedThisTick() {
        return backgroundPauseSuppressedThisTick;
    }

    /**
     * Runs one PermaClicker tick in the configured click mode.
     *
     * @return for mining, whether mining advanced this tick; for mob attack, whether a hit was performed
     */
    public boolean performClickTick(Minecraft minecraft, PermaClickConfig config) {
        if (minecraft == null || minecraft.options == null || config == null) {
            return false;
        }

        if (config.clickMode() == ClickMode.MOB_ATTACK) {
            // Mob attack never holds the attack key: a held key would make vanilla's continueAttack
            // break blocks behind or around the mob. attackRequestedThisTick stays false, so
            // syncAttackHoldState releases any hold left over from a previous mining tick.
            return mobAttackExecutor.performMobAttackTick(minecraft, config.attackBufferTicks());
        }

        attackRequestedThisTick = true;
        attackHoldForcedByPermaClick = true;
        boolean advanced = backgroundCursorFreeActive
            ? backgroundModeExecutor.performBackgroundMiningTick(minecraft)
            : focusedModeExecutor.performFocusedMiningTick(minecraft);
        if (advanced) {
            MiningTickGuard.markMiningAdvanced();
        }
        return advanced;
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
        mobAttackExecutor.reset();
        MiningTickGuard.reset();
    }
}
