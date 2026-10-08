package net.kroet.permaclicker.common.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningTargetTest {
    @Test
    void onlyBlocksMayStartAnAttack() {
        assertTrue(MiningTarget.BLOCK.mayStartAttack());
        assertFalse(MiningTarget.ENTITY.mayStartAttack());
        assertFalse(MiningTarget.NONE.mayStartAttack());
    }
}
