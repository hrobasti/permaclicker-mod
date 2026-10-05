package com.github.hrobasti.permaclicker.client;

import net.minecraft.client.Minecraft;

/**
 * Stable focused-mode mining execution path. Shared between Fabric and NeoForge.
 */
public final class FocusedModeExecutor {
    private final MiningActions actions = new MiningActions();

    public boolean performFocusedMiningTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        actions.updateCachedDestroyTarget(minecraft);
        minecraft.options.keyAttack.setDown(true);

        // Stop at the first call that advances mining: at most one destroy step per tick.
        boolean invoked = actions.continueAttack(minecraft) || actions.continueDestroyWithCachedTarget(minecraft);

        if (!invoked && minecraft.isWindowActive()) {
            invoked = actions.startAttack(minecraft) || actions.startDestroyWithCachedTarget(minecraft);
        }

        if (!invoked && minecraft.isWindowActive()) {
            invoked = actions.clickAttackKey(minecraft);
        }

        return invoked;
    }
}
