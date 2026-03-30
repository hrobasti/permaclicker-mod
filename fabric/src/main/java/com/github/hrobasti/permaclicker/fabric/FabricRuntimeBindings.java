package com.github.hrobasti.permaclicker.fabric;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-side bindings that connect common PermaClick runtime operations
 * to Fabric client implementations.
 */
public record FabricRuntimeBindings(
    Supplier<Boolean> focusedSupplier,
    Supplier<Boolean> minimizedSupplier,
    Supplier<Boolean> miningReadySupplier,
    BooleanSupplier miningTickAction,
    Consumer<ActionBarPayload> actionBarMessage,
    Consumer<Boolean> movementLockAction,
    Consumer<Boolean> backgroundCursorFreeAction
) {
    public FabricRuntimeBindings {
        Objects.requireNonNull(focusedSupplier, "focusedSupplier");
        Objects.requireNonNull(minimizedSupplier, "minimizedSupplier");
        Objects.requireNonNull(miningReadySupplier, "miningReadySupplier");
        Objects.requireNonNull(miningTickAction, "miningTickAction");
        Objects.requireNonNull(actionBarMessage, "actionBarMessage");
        Objects.requireNonNull(movementLockAction, "movementLockAction");
        Objects.requireNonNull(backgroundCursorFreeAction, "backgroundCursorFreeAction");
    }

    public static FabricRuntimeBindings noop() {
        return new FabricRuntimeBindings(
            () -> true,
            () -> false,
            () -> false,
            () -> false,
            ignored -> {
            },
            ignored -> {
            },
            ignored -> {
            }
        );
    }

    public void applyMovementLock(boolean active) {
        movementLockAction.accept(active);
    }

    public void setBackgroundCursorFree(boolean active) {
        backgroundCursorFreeAction.accept(active);
    }

    public record ActionBarPayload(String message, String colorName) {
    }
}

