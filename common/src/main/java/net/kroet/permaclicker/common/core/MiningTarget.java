package net.kroet.permaclicker.common.core;

/**
 * What the crosshair points at in Mining mode. Starting an attack on anything
 * but a block would hit the entity (vanilla attacks it without a miss delay) or
 * swing into the air, so only blocks may start one.
 */
public enum MiningTarget {
    BLOCK, ENTITY, NONE;

    public boolean mayStartAttack() {
        return this == BLOCK;
    }
}
