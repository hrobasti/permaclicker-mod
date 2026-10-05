package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.client.mixin.MinecraftAttackInvoker;
import com.github.hrobasti.permaclicker.client.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Block-mining primitives shared by the focused- and background-mode executors: vanilla attack
 * entry points (via compile-checked mixin invokers), a cached destroy target that survives brief
 * crosshair/hit-result gaps, and detection of whether a call advanced mining. Each executor owns
 * one instance because the cached target is per-executor state.
 *
 * <p>Every method returns whether mining advanced this tick: destroy progress grew, destroying
 * started, the block broke, or the post-break destroy delay counted down. Executors stop after the
 * first advancing call so mining advances at most once per tick (see {@link MiningTickGuard}).</p>
 */
final class MiningActions {
    private BlockPos cachedDestroyPos;
    private Direction cachedDestroyDirection;

    void updateCachedDestroyTarget(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }

        BlockPos currentPos = hit.getBlockPos();
        if (minecraft.level.getBlockState(currentPos).isAir()) {
            return;
        }

        cachedDestroyPos = currentPos.immutable();
        cachedDestroyDirection = hit.getDirection();
    }

    boolean continueAttack(Minecraft minecraft) {
        DestroySnapshot before = DestroySnapshot.of(minecraft.gameMode);
        try {
            ((MinecraftAttackInvoker) minecraft).permaclicker$invokeContinueAttack(true);
        } catch (RuntimeException ignored) {
            return false;
        }
        return before.hasAdvancedIn(minecraft.gameMode);
    }

    boolean startAttack(Minecraft minecraft) {
        DestroySnapshot before = DestroySnapshot.of(minecraft.gameMode);
        try {
            ((MinecraftAttackInvoker) minecraft).permaclicker$invokeStartAttack();
        } catch (RuntimeException ignored) {
            return false;
        }
        return before.hasAdvancedIn(minecraft.gameMode);
    }

    boolean clickAttackKey(Minecraft minecraft) {
        try {
            KeyMapping.click(minecraft.options.keyAttack.getDefaultKey());
            return minecraft.gameMode != null && minecraft.gameMode.isDestroying();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    boolean continueDestroyWithCachedTarget(Minecraft minecraft) {
        return destroyWithCachedTarget(minecraft, MultiPlayerGameMode::continueDestroyBlock);
    }

    boolean startDestroyWithCachedTarget(Minecraft minecraft) {
        return destroyWithCachedTarget(minecraft, MultiPlayerGameMode::startDestroyBlock);
    }

    private boolean destroyWithCachedTarget(Minecraft minecraft, DestroyCall destroyCall) {
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (gameMode == null || minecraft.level == null || cachedDestroyPos == null || cachedDestroyDirection == null) {
            return false;
        }

        if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
            clearCachedDestroyTarget();
            return false;
        }

        try {
            DestroySnapshot before = DestroySnapshot.of(gameMode);
            destroyCall.apply(gameMode, cachedDestroyPos, cachedDestroyDirection);

            if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
                clearCachedDestroyTarget();
                return true;
            }

            return before.hasAdvancedIn(gameMode);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void clearCachedDestroyTarget() {
        cachedDestroyPos = null;
        cachedDestroyDirection = null;
    }

    @FunctionalInterface
    private interface DestroyCall {
        void apply(MultiPlayerGameMode gameMode, BlockPos pos, Direction direction);
    }

    private record DestroySnapshot(boolean destroying, int stage, float progress, int delay) {
        static DestroySnapshot of(MultiPlayerGameMode gameMode) {
            if (gameMode == null) {
                return new DestroySnapshot(false, Integer.MIN_VALUE, Float.NaN, Integer.MIN_VALUE);
            }
            return new DestroySnapshot(
                gameMode.isDestroying(),
                gameMode.getDestroyStage(),
                destroyProgress(gameMode),
                destroyDelay(gameMode)
            );
        }

        boolean hasAdvancedIn(MultiPlayerGameMode gameMode) {
            if (gameMode == null) {
                return false;
            }

            // The destroy delay counts down once per continueDestroyBlock call after a block broke,
            // and is re-armed when a block breaks; either change means this call used up the tick.
            int currentDelay = destroyDelay(gameMode);
            if (delay != Integer.MIN_VALUE && currentDelay != Integer.MIN_VALUE && currentDelay != delay) {
                return true;
            }

            boolean currentDestroying = gameMode.isDestroying();
            int currentStage = gameMode.getDestroyStage();
            float currentProgress = destroyProgress(gameMode);

            if (!Float.isNaN(progress) && !Float.isNaN(currentProgress) && currentProgress > progress + 0.0001F) {
                return true;
            }

            if (currentStage > stage && currentStage > 0) {
                return true;
            }

            return !destroying && currentDestroying && currentStage > 0;
        }

        private static int destroyDelay(MultiPlayerGameMode gameMode) {
            try {
                return ((MultiPlayerGameModeAccessor) gameMode).permaclicker$getDestroyDelay();
            } catch (RuntimeException ignored) {
                return Integer.MIN_VALUE;
            }
        }

        private static float destroyProgress(MultiPlayerGameMode gameMode) {
            try {
                return ((MultiPlayerGameModeAccessor) gameMode).permaclicker$getDestroyProgress();
            } catch (RuntimeException ignored) {
                return Float.NaN;
            }
        }
    }
}
