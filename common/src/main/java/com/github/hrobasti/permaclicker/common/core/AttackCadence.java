package com.github.hrobasti.permaclicker.common.core;

/**
 * Loader-neutral hit timing for the mob-attack click mode.
 *
 * <p>Fed once per client tick. A hit is released only when a valid target is in the crosshair
 * and the player's attack cooldown has been fully charged for at least {@code bufferTicks}
 * consecutive ticks. The buffer is a safety margin against server lag so hits always land at
 * full strength. Because charged ticks keep counting while no target is present, a mob that walks
 * into an already-charged crosshair is hit immediately instead of waiting out the buffer again.</p>
 *
 * <p>Client-thread-only, like {@link PermaClickService}.</p>
 */
public final class AttackCadence {
    private int chargedTicks;

    /**
     * @param hasTarget       a valid attack target is currently in the crosshair
     * @param cooldownCharged the player's attack strength is at 100%
     * @param bufferTicks     extra full-cooldown ticks to wait before hitting (negative counts as 0)
     * @return true if a hit should be performed this tick
     */
    public boolean shouldAttack(boolean hasTarget, boolean cooldownCharged, int bufferTicks) {
        if (!cooldownCharged) {
            chargedTicks = 0;
            return false;
        }

        int requiredTicks = Math.max(0, bufferTicks);
        if (hasTarget && chargedTicks >= requiredTicks) {
            chargedTicks = 0;
            return true;
        }

        if (chargedTicks < requiredTicks) {
            chargedTicks++;
        }
        return false;
    }

    public void reset() {
        chargedTicks = 0;
    }
}
