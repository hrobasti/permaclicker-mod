package net.kroet.permaclicker.client;

import net.kroet.permaclicker.client.mixin.MinecraftAttackInvoker;
import net.kroet.permaclicker.common.core.AttackCadence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Mob-attack click mode: instead of holding the attack key, performs a single
 * vanilla attack whenever a mob (or, if enabled, an armor stand) is in the
 * crosshair and the attack cooldown is fully charged (plus the configured
 * buffer, see {@link AttackCadence}). Works in both focused and background
 * runtime modes. Shared between Fabric and NeoForge.
 *
 * <p>
 * The hit itself goes through Minecraft's own {@code startAttack()} so vanilla
 * rules (attack range, item restrictions, piercing weapons, sweeping, swing
 * animation) apply unchanged. It is only called while the hit result is a valid
 * target, so it can never fall through into block breaking.
 * </p>
 */
public final class MobAttackExecutor {
    private final AttackCadence cadence = new AttackCadence();

    /**
     * @return true if a hit was performed this tick
     */
    public boolean performMobAttackTick(Minecraft minecraft, int bufferTicks, boolean hitArmorStands) {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null || minecraft.gameMode == null || player.isSpectator()) {
            cadence.reset();
            return false;
        }

        if (minecraft.gui.screen() instanceof PauseScreen) {
            return false;
        }

        Entity target = minecraft.hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        boolean armorStandTarget = hitArmorStands && isHittableArmorStand(target, player);
        boolean hasTarget = isAttackableMob(target) || armorStandTarget;
        boolean cooldownCharged = player.getAttackStrengthScale(0.0F) >= 1.0F;
        int minHitGapTicks = armorStandTarget ? AttackCadence.ARMOR_STAND_MIN_HIT_GAP_TICKS : 0;
        if (!cadence.shouldAttack(hasTarget, cooldownCharged, bufferTicks, minHitGapTicks)) {
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
     * Any living mob is a valid target; players are never {@link Mob}s, and non-mob
     * entities such as item frames, boats or minecarts are excluded as well.
     */
    private static boolean isAttackableMob(Entity target) {
        return target instanceof Mob mob && mob.isAlive();
    }

    /**
     * Farms hit armor stands to trigger sweeping attacks. Creative players would
     * destroy the stand with a single hit, so they never target one.
     */
    private static boolean isHittableArmorStand(Entity target, LocalPlayer player) {
        return target instanceof ArmorStand stand
                && stand.isAlive()
                && !stand.isMarker()
                && !player.getAbilities().instabuild;
    }
}
