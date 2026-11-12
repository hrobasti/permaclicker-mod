package net.kroet.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttackCadenceTest {
    @Test
    void neverAttacksWithoutTarget() {
        AttackCadence cadence = new AttackCadence();

        for (int i = 0; i < 50; i++) {
            assertFalse(cadence.shouldAttack(false, true, 2));
        }
    }

    @Test
    void neverAttacksWhileCooldownIsCharging() {
        AttackCadence cadence = new AttackCadence();

        for (int i = 0; i < 50; i++) {
            assertFalse(cadence.shouldAttack(true, false, 0));
        }
    }

    @Test
    void zeroBufferAttacksOnFirstChargedTick() {
        AttackCadence cadence = new AttackCadence();

        assertTrue(cadence.shouldAttack(true, true, 0));
    }

    @Test
    void waitsExactlyBufferTicksAfterCooldownIsCharged() {
        AttackCadence cadence = new AttackCadence();

        assertFalse(cadence.shouldAttack(true, true, 2));
        assertFalse(cadence.shouldAttack(true, true, 2));
        assertTrue(cadence.shouldAttack(true, true, 2));
    }

    @Test
    void maximumBufferWaitsTwentyTicks() {
        AttackCadence cadence = new AttackCadence();

        for (int i = 0; i < 20; i++) {
            assertFalse(cadence.shouldAttack(true, true, 20));
        }
        assertTrue(cadence.shouldAttack(true, true, 20));
    }

    @Test
    void cooldownDropResetsBuffer() {
        AttackCadence cadence = new AttackCadence();

        assertFalse(cadence.shouldAttack(true, true, 2));
        assertFalse(cadence.shouldAttack(true, true, 2));
        assertFalse(cadence.shouldAttack(true, false, 2));

        assertFalse(cadence.shouldAttack(true, true, 2));
        assertFalse(cadence.shouldAttack(true, true, 2));
        assertTrue(cadence.shouldAttack(true, true, 2));
    }

    @Test
    void attackResetsBufferForNextHit() {
        AttackCadence cadence = new AttackCadence();

        assertFalse(cadence.shouldAttack(true, true, 1));
        assertTrue(cadence.shouldAttack(true, true, 1));

        assertFalse(cadence.shouldAttack(true, true, 1));
        assertTrue(cadence.shouldAttack(true, true, 1));
    }

    @Test
    void targetEnteringAlreadyChargedCrosshairIsHitImmediately() {
        AttackCadence cadence = new AttackCadence();

        for (int i = 0; i < 10; i++) {
            assertFalse(cadence.shouldAttack(false, true, 2));
        }
        assertTrue(cadence.shouldAttack(true, true, 2));
    }

    @Test
    void negativeBufferBehavesLikeZero() {
        AttackCadence cadence = new AttackCadence();

        assertTrue(cadence.shouldAttack(true, true, -3));
    }

    @Test
    void minimumHitGapDelaysFastFollowUpHits() {
        AttackCadence cadence = new AttackCadence();
        int gap = AttackCadence.ARMOR_STAND_MIN_HIT_GAP_TICKS;

        assertTrue(cadence.shouldAttack(true, true, 0, gap));
        for (int i = 1; i < gap; i++) {
            assertFalse(cadence.shouldAttack(true, true, 0, gap));
        }
        assertTrue(cadence.shouldAttack(true, true, 0, gap));
    }

    @Test
    void armorStandGapStaysAboveVanillaBreakWindow() {
        assertTrue(AttackCadence.ARMOR_STAND_MIN_HIT_GAP_TICKS > 5);
    }

    @Test
    void minimumHitGapCountsTicksWithoutTargetOrCooldown() {
        AttackCadence cadence = new AttackCadence();
        int gap = AttackCadence.ARMOR_STAND_MIN_HIT_GAP_TICKS;

        assertTrue(cadence.shouldAttack(true, true, 0, gap));
        for (int i = 1; i < gap; i++) {
            assertFalse(cadence.shouldAttack(false, i % 2 == 0, 0, gap));
        }
        assertTrue(cadence.shouldAttack(true, true, 0, gap));
    }

    @Test
    void resetLiftsMinimumHitGap() {
        AttackCadence cadence = new AttackCadence();
        int gap = AttackCadence.ARMOR_STAND_MIN_HIT_GAP_TICKS;
        assertTrue(cadence.shouldAttack(true, true, 0, gap));

        cadence.reset();

        assertTrue(cadence.shouldAttack(true, true, 0, gap));
    }

    @Test
    void resetClearsAccumulatedChargedTicks() {
        AttackCadence cadence = new AttackCadence();
        for (int i = 0; i < 5; i++) {
            cadence.shouldAttack(false, true, 2);
        }

        cadence.reset();

        assertFalse(cadence.shouldAttack(true, true, 2));
    }
}
