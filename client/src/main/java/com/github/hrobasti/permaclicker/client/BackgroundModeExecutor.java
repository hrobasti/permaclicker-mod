package com.github.hrobasti.permaclicker.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Background-mode execution path based on aggressive pause suppression, without cursor/focus-
 * detach hacks. Shared between Fabric and NeoForge.
 */
public final class BackgroundModeExecutor {
    private static final int UNFOCUSED_REPRIME_INTERVAL_TICKS = 1;

    private int unfocusedMissTicks;
    private BlockPos cachedDestroyPos;
    private Direction cachedDestroyDirection;

    public boolean performBackgroundMiningTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        if (minecraft.gui.screen() instanceof PauseScreen) {
            return false;
        }

        updateCachedDestroyTarget(minecraft);
        minecraft.options.keyAttack.setDown(true);

        boolean continueInvoked = invokeContinueAttack(minecraft);
        boolean continueCached = continueDestroyWithCachedTarget(minecraft);
        if (continueInvoked || continueCached) {
            unfocusedMissTicks = 0;
            return true;
        }

        unfocusedMissTicks++;
        if (unfocusedMissTicks >= UNFOCUSED_REPRIME_INTERVAL_TICKS) {
            boolean reprimeInvoked = invokeStartAttack(minecraft)
                || startDestroyWithCachedTarget(minecraft)
                || invokeKeyMappingClick(minecraft);
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

        writePauseOnLostFocusValue(minecraft, false);
        if (minecraft.gui.screen() instanceof PauseScreen) {
            minecraft.setScreenAndShow(null);
            return true;
        }

        return false;
    }

    public void reset() {
        unfocusedMissTicks = 0;
    }

    private static boolean invokeContinueAttack(Minecraft minecraft) {
        boolean previousDestroying = isDestroyingBlock(minecraft);
        int previousStage = getDestroyStage(minecraft);
        float previousProgress = getDestroyProgressValue(minecraft);
        try {
            Method withBoolean = getAccessibleMethod(minecraft.getClass(), "continueAttack", boolean.class);
            withBoolean.invoke(minecraft, true);
            return hasDestroyProgress(minecraft.gameMode, previousDestroying, previousStage, previousProgress);
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Throwable ignored) {
            return false;
        }

        try {
            Method noArg = getAccessibleMethod(minecraft.getClass(), "continueAttack");
            noArg.invoke(minecraft);
            return hasDestroyProgress(minecraft.gameMode, previousDestroying, previousStage, previousProgress);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeStartAttack(Minecraft minecraft) {
        boolean previousDestroying = isDestroyingBlock(minecraft);
        int previousStage = getDestroyStage(minecraft);
        float previousProgress = getDestroyProgressValue(minecraft);
        try {
            Method startAttack = getAccessibleMethod(minecraft.getClass(), "startAttack");
            startAttack.invoke(minecraft);
            return hasDestroyProgress(minecraft.gameMode, previousDestroying, previousStage, previousProgress);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeKeyMappingClick(Minecraft minecraft) {
        try {
            KeyMapping.click(minecraft.options.keyAttack.getDefaultKey());
            return isDestroyingBlock(minecraft);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateCachedDestroyTarget(Minecraft minecraft) {
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

    private boolean continueDestroyWithCachedTarget(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (gameMode == null || minecraft.level == null || cachedDestroyPos == null || cachedDestroyDirection == null) {
            return false;
        }

        if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
            cachedDestroyPos = null;
            cachedDestroyDirection = null;
            return false;
        }

        try {
            boolean previousDestroying = gameMode.isDestroying();
            int previousStage = gameMode.getDestroyStage();
            float previousProgress = getDestroyProgressValue(gameMode);
            gameMode.continueDestroyBlock(cachedDestroyPos, cachedDestroyDirection);

            if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
                cachedDestroyPos = null;
                cachedDestroyDirection = null;
                return true;
            }

            return hasDestroyProgress(gameMode, previousDestroying, previousStage, previousProgress);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean startDestroyWithCachedTarget(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (gameMode == null || minecraft.level == null || cachedDestroyPos == null || cachedDestroyDirection == null) {
            return false;
        }

        if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
            cachedDestroyPos = null;
            cachedDestroyDirection = null;
            return false;
        }

        try {
            boolean previousDestroying = gameMode.isDestroying();
            int previousStage = gameMode.getDestroyStage();
            float previousProgress = getDestroyProgressValue(gameMode);
            gameMode.startDestroyBlock(cachedDestroyPos, cachedDestroyDirection);

            if (minecraft.level.getBlockState(cachedDestroyPos).isAir()) {
                cachedDestroyPos = null;
                cachedDestroyDirection = null;
                return true;
            }

            return hasDestroyProgress(gameMode, previousDestroying, previousStage, previousProgress);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasDestroyProgress(
        MultiPlayerGameMode gameMode,
        boolean previousDestroying,
        int previousStage,
        float previousProgress
    ) {
        if (gameMode == null) {
            return false;
        }

        boolean currentDestroying = gameMode.isDestroying();
        int currentStage = gameMode.getDestroyStage();
        float currentProgress = getDestroyProgressValue(gameMode);

        if (!Float.isNaN(previousProgress) && !Float.isNaN(currentProgress) && currentProgress > previousProgress + 0.0001F) {
            return true;
        }

        if (currentStage > previousStage && currentStage > 0) {
            return true;
        }

        if (!previousDestroying && currentDestroying && currentStage > 0) {
            return true;
        }

        return false;
    }

    private static float getDestroyProgressValue(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft == null ? null : minecraft.gameMode;
        return getDestroyProgressValue(gameMode);
    }

    private static float getDestroyProgressValue(MultiPlayerGameMode gameMode) {
        if (gameMode == null) {
            return Float.NaN;
        }

        try {
            Field field = getAccessibleField(gameMode.getClass(), "destroyProgress");
            Object value = field.get(gameMode);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
            // best effort
        }

        return Float.NaN;
    }

    private static boolean isDestroyingBlock(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft == null ? null : minecraft.gameMode;
        return gameMode != null && gameMode.isDestroying();
    }

    private static int getDestroyStage(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft == null ? null : minecraft.gameMode;
        return gameMode == null ? Integer.MIN_VALUE : gameMode.getDestroyStage();
    }

    private static void writePauseOnLostFocusValue(Minecraft minecraft, boolean value) {
        Object options = minecraft == null ? null : minecraft.options;
        if (options == null) {
            return;
        }

        Object option = null;
        try {
            Method accessor = getAccessibleMethod(options.getClass(), "pauseOnLostFocus");
            option = accessor.invoke(options);
            if (option instanceof Boolean) {
                Method setter = getAccessibleMethod(options.getClass(), "pauseOnLostFocus", boolean.class);
                setter.invoke(options, value);
                return;
            }
        } catch (Throwable ignored) {
            // fallback
        }

        if (option == null) {
            try {
                Field field = getAccessibleField(options.getClass(), "pauseOnLostFocus");
                Object fieldValue = field.get(options);
                if (fieldValue instanceof Boolean) {
                    field.set(options, value);
                    return;
                }
                option = fieldValue;
            } catch (Throwable ignored) {
                return;
            }
        }

        writeBooleanToOption(option, value);
    }

    private static void writeBooleanToOption(Object option, boolean value) {
        if (option == null) {
            return;
        }

        try {
            Method setPrimitive = getAccessibleMethod(option.getClass(), "set", boolean.class);
            setPrimitive.invoke(option, value);
            return;
        } catch (Throwable ignored) {
            // try boxed overload
        }

        try {
            Method setBoxed = getAccessibleMethod(option.getClass(), "set", Object.class);
            setBoxed.invoke(option, Boolean.valueOf(value));
            return;
        } catch (Throwable ignored) {
            // try setValue overload
        }

        try {
            Method setValue = getAccessibleMethod(option.getClass(), "setValue", Object.class);
            setValue.invoke(option, Boolean.valueOf(value));
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static Method getAccessibleMethod(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Method declared = owner.getDeclaredMethod(name, parameterTypes);
            declared.setAccessible(true);
            return declared;
        }
    }

    private static Field getAccessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException ignored) {
            Field declared = owner.getDeclaredField(name);
            declared.setAccessible(true);
            return declared;
        }
    }
}
