package net.kroet.permaclicker.common.update;

/**
 * Platform queried by the update check, with its display name.
 */
public enum UpdateProvider {
    MODRINTH("Modrinth"), CURSEFORGE("CurseForge");

    private final String label;

    UpdateProvider(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
