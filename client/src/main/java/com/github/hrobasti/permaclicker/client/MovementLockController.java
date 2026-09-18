package com.github.hrobasti.permaclicker.client;

import net.minecraft.client.Minecraft;

/**
 * Freezes player movement input and view direction while active. Shared between Fabric and
 * NeoForge; each loader entrypoint owns one instance.
 */
public final class MovementLockController {
    private boolean active;
    private boolean hasLockedView;
    private float lockedYaw;
    private float lockedPitch;

    public void setActive(Minecraft minecraft, boolean active) {
        this.active = active;
        if (!active) {
            hasLockedView = false;
        } else {
            captureLockedView(minecraft);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void applyInputSuppression(Minecraft minecraft) {
        if (!active || minecraft == null || minecraft.options == null) {
            return;
        }

        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    public void applyViewFreeze(Minecraft minecraft) {
        if (!active || minecraft == null || minecraft.player == null) {
            return;
        }

        if (!hasLockedView) {
            captureLockedView(minecraft);
        }

        minecraft.player.setYRot(lockedYaw);
        minecraft.player.setXRot(lockedPitch);
        minecraft.player.yRotO = lockedYaw;
        minecraft.player.xRotO = lockedPitch;
    }

    private void captureLockedView(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        lockedYaw = minecraft.player.getYRot();
        lockedPitch = minecraft.player.getXRot();
        hasLockedView = true;
    }
}
