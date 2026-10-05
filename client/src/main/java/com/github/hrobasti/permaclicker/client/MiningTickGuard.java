package com.github.hrobasti.permaclicker.client;

/**
 * Ensures block mining advances at most once per client tick.
 *
 * <p>PermaClicker's mining tick runs at the start of the client tick, before vanilla's
 * {@code handleKeybinds()} calls {@code Minecraft.continueAttack(boolean)} on its own. Without this
 * guard, one tick could advance destroy progress (or the post-break destroy delay) several times,
 * which mines faster than vanilla and is easily flagged by servers. Worse, while the mouse is not
 * grabbed (background mode), vanilla calls {@code continueAttack(false)}, which runs
 * {@code stopDestroyBlock()} and throws away the progress PermaClicker just made.</p>
 *
 * <p>Once PermaClicker's own tick has advanced mining, {@code MinecraftContinueAttackGuardMixin}
 * skips vanilla's {@code continueAttack} for the rest of that tick. Static because mixins cannot
 * reach the loader entrypoint instance; client-thread-only like the rest of the runtime.</p>
 */
public final class MiningTickGuard {
    private static boolean miningAdvancedThisTick;

    private MiningTickGuard() {
    }

    static void markMiningAdvanced() {
        miningAdvancedThisTick = true;
    }

    static void reset() {
        miningAdvancedThisTick = false;
    }

    public static boolean shouldSkipVanillaContinueAttack() {
        return miningAdvancedThisTick;
    }
}
