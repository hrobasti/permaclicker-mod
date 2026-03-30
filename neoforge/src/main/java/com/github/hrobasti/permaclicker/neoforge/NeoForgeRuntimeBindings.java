package com.github.hrobasti.permaclicker.neoforge;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-side bindings that connect common PermaClick runtime operations
 * to NeoForge client implementations.
 */
public record NeoForgeRuntimeBindings(
    Supplier<Boolean> focusedSupplier,
    Supplier<Boolean> minimizedSupplier,
    Supplier<Boolean> miningReadySupplier,
    BooleanSupplier miningTickAction,
    Consumer<ActionBarPayload> actionBarMessage,
    Consumer<Boolean> movementLockAction,
    Consumer<Boolean> backgroundCursorFreeAction
) {
    public NeoForgeRuntimeBindings {
        Objects.requireNonNull(focusedSupplier, "focusedSupplier");
        Objects.requireNonNull(minimizedSupplier, "minimizedSupplier");
        Objects.requireNonNull(miningReadySupplier, "miningReadySupplier");
        Objects.requireNonNull(miningTickAction, "miningTickAction");
        Objects.requireNonNull(actionBarMessage, "actionBarMessage");
        Objects.requireNonNull(movementLockAction, "movementLockAction");
        Objects.requireNonNull(backgroundCursorFreeAction, "backgroundCursorFreeAction");
    }

    public static NeoForgeRuntimeBindings noop() {
        return new NeoForgeRuntimeBindings(
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

