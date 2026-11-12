package net.kroet.permaclicker.common.core;

/**
 * Loader-neutral hit timing for the mob-attack click mode.
 *
 * <p>
 * Fed once per client tick. A hit is released only when a valid target is in
 * the crosshair and the player's attack cooldown has been fully charged for at
 * least {@code bufferTicks} consecutive ticks. The buffer is a safety margin
 * against server lag so hits always land at full strength. Because charged
 * ticks keep counting while no target is present, a mob that walks into an
 * already-charged crosshair is hit immediately instead of waiting out the
 * buffer again.
 * </p>
 *
 * <p>
 * An optional minimum gap since the previous hit protects armor stands: vanilla
 * breaks one that is hit twice within 5 game ticks.
 * </p>
 *
 * <p>
 * Client-thread-only, like {@link PermaClickService}.
 * </p>
 */
public final class AttackCadence {
    /**
     * Hit gap for armor stands, with headroom over vanilla's 5-tick break window.
     */
    public static final int ARMOR_STAND_MIN_HIT_GAP_TICKS = 10;

    private int chargedTicks;
    private int ticksSinceLastHit = Integer.MAX_VALUE;

    /**
     * Same as {@link #shouldAttack(boolean, boolean, int, int)} without a minimum
     * hit gap.
     */
    public boolean shouldAttack(boolean hasTarget, boolean cooldownCharged, int bufferTicks) {
        return shouldAttack(hasTarget, cooldownCharged, bufferTicks, 0);
    }

    /**
     * @param hasTarget
     *            a valid attack target is currently in the crosshair
     * @param cooldownCharged
     *            the player's attack strength is at 100%
     * @param bufferTicks
     *            extra full-cooldown ticks to wait before hitting (negative counts
     *            as 0)
     * @param minHitGapTicks
     *            ticks that must have passed since the previous hit (0 for none)
     * @return true if a hit should be performed this tick
     */
    public boolean shouldAttack(boolean hasTarget, boolean cooldownCharged, int bufferTicks, int minHitGapTicks) {
        if (ticksSinceLastHit < Integer.MAX_VALUE) {
            ticksSinceLastHit++;
        }
        if (!cooldownCharged) {
            chargedTicks = 0;
            return false;
        }

        int requiredTicks = Math.max(0, bufferTicks);
        if (hasTarget && chargedTicks >= requiredTicks && ticksSinceLastHit >= minHitGapTicks) {
            chargedTicks = 0;
            ticksSinceLastHit = 0;
            return true;
        }

        if (chargedTicks < requiredTicks) {
            chargedTicks++;
        }
        return false;
    }

    public void reset() {
        chargedTicks = 0;
        ticksSinceLastHit = Integer.MAX_VALUE;
    }
}
