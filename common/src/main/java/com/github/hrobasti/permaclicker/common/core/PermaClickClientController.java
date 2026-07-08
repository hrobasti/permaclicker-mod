package com.github.hrobasti.permaclicker.common.core;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.input.ToggleKeyBinding;

/**
 * Coordinates runtime ticking and key toggle handling for client integrations.
 */
public final class PermaClickClientController {
    public static final int DEFAULT_TOGGLE_KEY_CODE = 296;

    private final PermaClickService service;
    private final ToggleKeyBinding toggleKeyBinding;

    public PermaClickClientController() {
        this(new PermaClickService(), new ToggleKeyBinding(DEFAULT_TOGGLE_KEY_CODE));
    }

    PermaClickClientController(PermaClickService service, ToggleKeyBinding toggleKeyBinding) {
        this.service = service;
        this.toggleKeyBinding = toggleKeyBinding;
    }

    public PermaClickService service() {
        return service;
    }

    public int boundKeyCode() {
        return toggleKeyBinding.boundKeyCode();
    }

    public void setBoundKeyCode(int keyCode) {
        toggleKeyBinding.setBoundKeyCode(keyCode);
    }

    public void applyConfig(PermaClickConfig config) {
        setBoundKeyCode(config.toggleKeyCode());
        service.applyConfig(config);
    }

    public PermaClickConfig currentConfig() {
        return service.currentConfig();
    }

    public void onKeyEvent(int keyCode, boolean pressed, PermaClickRuntime runtime) {
        toggleKeyBinding.onKeyEvent(keyCode, pressed, service, runtime);
    }

    public void onClientTick(PermaClickRuntime runtime) {
        service.tick(runtime);
    }

    public void onClientShutdown(PermaClickRuntime runtime) {
        service.shutdown(runtime);
    }
}

