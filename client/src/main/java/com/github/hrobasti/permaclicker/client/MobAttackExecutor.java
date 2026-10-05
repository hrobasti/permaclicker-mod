package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.client.mixin.MinecraftAttackInvoker;
import com.github.hrobasti.permaclicker.common.core.AttackCadence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Mob-attack click mode: instead of holding the attack key, performs a single vanilla attack
 * whenever a mob is in the crosshair and the attack cooldown is fully charged (plus the configured
 * buffer, see {@link AttackCadence}). Works in both focused and background runtime modes. Shared
 * between Fabric and NeoForge.
 *
 * <p>The hit itself goes through Minecraft's own {@code startAttack()} so vanilla rules (attack
 * range, item restrictions, piercing weapons, swing animation) apply unchanged. It is only called
 * while the hit result is a valid mob, so it can never fall through into block breaking.</p>
 */
public final class MobAttackExecutor {
    private final AttackCadence cadence = new AttackCadence();

    /**
     * @return true if a hit was performed this tick
     */
    public boolean performMobAttackTick(Minecraft minecraft, int bufferTicks) {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null || minecraft.gameMode == null || player.isSpectator()) {
            cadence.reset();
            return false;
        }

        if (minecraft.gui.screen() instanceof PauseScreen) {
            return false;
        }

        boolean hasTarget = isAttackableMob(minecraft.hitResult);
        boolean cooldownCharged = player.getAttackStrengthScale(0.0F) >= 1.0F;
        if (!cadence.shouldAttack(hasTarget, cooldownCharged, bufferTicks)) {
            return false;
        }

        try {
            ((MinecraftAttackInvoker) minecraft).permaclicker$invokeStartAttack();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public void reset() {
        cadence.reset();
    }

    /**
     * Any living mob is a valid target; players are never {@link Mob}s, and non-mob entities such
     * as armor stands, item frames, boats or minecarts are excluded as well.
     */
    private static boolean isAttackableMob(HitResult hitResult) {
        return hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof Mob mob
            && mob.isAlive();
    }
}
