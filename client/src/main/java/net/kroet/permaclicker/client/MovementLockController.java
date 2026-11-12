package net.kroet.permaclicker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Freezes player movement input and view direction while active. Shared between
 * Fabric and NeoForge; each loader entrypoint owns one instance.
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
        if (!active) {
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
        @Nullable
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (!active || player == null) {
            return;
        }

        if (!hasLockedView) {
            captureLockedView(minecraft);
        }

        player.setYRot(lockedYaw);
        player.setXRot(lockedPitch);
        player.yRotO = lockedYaw;
        player.xRotO = lockedPitch;
    }

    private void captureLockedView(Minecraft minecraft) {
        @Nullable
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        lockedYaw = player.getYRot();
        lockedPitch = player.getXRot();
        hasLockedView = true;
    }
}
