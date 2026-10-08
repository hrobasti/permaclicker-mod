package net.kroet.permaclicker.common.core;

import net.kroet.permaclicker.common.config.PermaClickConfig;

/**
 * Coordinates runtime ticking and toggling for client integrations.
 */
public final class PermaClickClientController {
    private final PermaClickService service;

    public PermaClickClientController() {
        this(new PermaClickService());
    }

    PermaClickClientController(PermaClickService service) {
        this.service = service;
    }

    public PermaClickService service() {
        return service;
    }

    public void applyConfig(PermaClickConfig config) {
        service.applyConfig(config);
    }

    public PermaClickConfig currentConfig() {
        return service.currentConfig();
    }

    public void toggle(PermaClickRuntime runtime) {
        service.toggle(runtime);
    }

    public void onClientTick(PermaClickRuntime runtime) {
        service.tick(runtime);
    }

    public void onClientShutdown(PermaClickRuntime runtime) {
        service.shutdown(runtime);
    }
}
