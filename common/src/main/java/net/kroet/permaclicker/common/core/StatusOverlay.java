package net.kroet.permaclicker.common.core;

import net.kroet.permaclicker.common.config.ClickMode;

/**
 * Content of the action-bar status; the client renders it from the lang files.
 *
 * @param timer
 *            remaining auto-stop time such as {@code 00:01:00}, or null
 */
public record StatusOverlay(boolean active, ClickMode clickMode, String timer) {
    public static StatusOverlay inactive(ClickMode clickMode) {
        return new StatusOverlay(false, clickMode, null);
    }

    /** English rendering, used for logs and tests. */
    public String plainText() {
        if (!active) {
            return "PermaClicker inactive";
        }
        String status = "PermaClicker active - " + (clickMode == ClickMode.MOB_ATTACK ? "Mobs & Animals" : "Mining");
        return timer == null ? status : status + " (" + timer + ")";
    }
}
