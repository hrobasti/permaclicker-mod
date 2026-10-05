package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * PermaClicker's key mappings and their controls-screen category, shared by both loaders. Each
 * loader registers them through its own API (Fabric {@code KeyMappingHelper}, NeoForge
 * {@code RegisterKeyMappingsEvent}).
 */
public final class PermaClickKeyMappings {
    /** Category label resolves to the {@code key.category.permaclicker.permaclicker} lang key. */
    public static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath("permaclicker", "permaclicker");

    private static final String TOGGLE_TRANSLATION_KEY = "key.permaclicker.toggle";
    private static final String CONFIG_TRANSLATION_KEY = "key.permaclicker.config";

    private PermaClickKeyMappings() {
    }

    public static KeyMapping createToggle(KeyMapping.Category category) {
        return new KeyMapping(
            TOGGLE_TRANSLATION_KEY,
            InputConstants.Type.KEYBOARD,
            PermaClickConfig.DEFAULT_TOGGLE_KEY_CODE,
            category
        );
    }

    public static KeyMapping createConfig(KeyMapping.Category category) {
        return new KeyMapping(CONFIG_TRANSLATION_KEY, InputConstants.Type.KEYBOARD, InputConstants.KEY_U, category);
    }
}
