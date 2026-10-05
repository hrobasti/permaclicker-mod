package com.github.hrobasti.permaclicker.common.core;

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
    void resetClearsAccumulatedChargedTicks() {
        AttackCadence cadence = new AttackCadence();
        for (int i = 0; i < 5; i++) {
            cadence.shouldAttack(false, true, 2);
        }

        cadence.reset();

        assertFalse(cadence.shouldAttack(true, true, 2));
    }
}
