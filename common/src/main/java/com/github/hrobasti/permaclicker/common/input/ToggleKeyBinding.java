package com.github.hrobasti.permaclicker.common.input;

import com.github.hrobasti.permaclicker.common.core.PermaClickRuntime;
import com.github.hrobasti.permaclicker.common.core.PermaClickService;

/**
 * Loader-neutral key toggle helper.
 *
 * It toggles only on rising-edge key presses to prevent repeated toggles
 * while a key is held down.
 */
public final class ToggleKeyBinding {
    private int boundKeyCode;
    private boolean currentlyPressed;

    public ToggleKeyBinding(int defaultKeyCode) {
        this.boundKeyCode = defaultKeyCode;
        this.currentlyPressed = false;
    }

    public int boundKeyCode() {
        return boundKeyCode;
    }

    public void setBoundKeyCode(int keyCode) {
        this.boundKeyCode = keyCode;
        this.currentlyPressed = false;
    }

    public void onKeyEvent(int keyCode, boolean pressed, PermaClickService service, PermaClickRuntime runtime) {
        if (keyCode != boundKeyCode) {
            return;
        }

        if (pressed && !currentlyPressed) {
            service.toggle(runtime);
        }

        currentlyPressed = pressed;
    }
}

