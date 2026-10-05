package com.github.hrobasti.permaclicker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Background-mode execution path based on aggressive pause suppression, without cursor/focus-
 * detach hacks. Shared between Fabric and NeoForge.
 */
public final class BackgroundModeExecutor {
    private static final int UNFOCUSED_REPRIME_INTERVAL_TICKS = 1;

    private final MiningActions actions = new MiningActions();
    private int unfocusedMissTicks;

    public boolean performBackgroundMiningTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        if (minecraft.gui.screen() instanceof PauseScreen) {
            return false;
        }

        actions.updateCachedDestroyTarget(minecraft);
        minecraft.options.keyAttack.setDown(true);

        // Stop at the first call that advances mining: at most one destroy step per tick.
        if (actions.continueAttack(minecraft) || actions.continueDestroyWithCachedTarget(minecraft)) {
            unfocusedMissTicks = 0;
            return true;
        }

        unfocusedMissTicks++;
        if (unfocusedMissTicks >= UNFOCUSED_REPRIME_INTERVAL_TICKS) {
            boolean reprimeInvoked = actions.startAttack(minecraft)
                || actions.startDestroyWithCachedTarget(minecraft)
                || actions.clickAttackKey(minecraft);
            unfocusedMissTicks = 0;
            if (reprimeInvoked) {
                return true;
            }
        }

        return false;
    }

    public boolean aggressivelySuppressPauseScreen(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }

        if (minecraft.options != null) {
            minecraft.options.pauseOnLostFocus = false;
        }
        if (minecraft.gui.screen() instanceof PauseScreen) {
            minecraft.setScreenAndShow(null);
            return true;
        }

        return false;
    }

    public void reset() {
        unfocusedMissTicks = 0;
    }
}
