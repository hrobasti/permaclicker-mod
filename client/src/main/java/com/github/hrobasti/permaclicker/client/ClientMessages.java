package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.common.core.PermaClickTextKeys;
import com.github.hrobasti.permaclicker.common.update.PermaClickUpdateService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Shared chat-message helpers used by both loader entrypoints for update-check and other
 * player-facing notices, including a version-compat dispatcher for sending a client message
 * across Minecraft API shapes.
 */
public final class ClientMessages {
    private ClientMessages() {
    }

    public static String resolveCurrentVersion() {
        String implementationVersion = ClientMessages.class.getPackage().getImplementationVersion();
        return (implementationVersion == null || implementationVersion.isBlank()) ? "0.0.0" : implementationVersion;
    }

    public static void displayClientMessageCompat(LocalPlayer player, Component message, boolean actionBar) {
        if (player == null || message == null) {
            return;
        }

        if (ReflectionCompat.invokeCompatibleMethod(player, "displayClientMessage", message, actionBar)) {
            return;
        }

        if (ReflectionCompat.invokeCompatibleMethod(player, "sendSystemMessage", message, actionBar)) {
            return;
        }

        if (ReflectionCompat.invokeCompatibleMethod(player, "sendMessage", message, actionBar)) {
            return;
        }

        if (ReflectionCompat.invokeCompatibleMethodByShape(player, new Object[] { message, actionBar })) {
            return;
        }

        if (ReflectionCompat.invokeCompatibleMethod(player, "sendSystemMessage", message)) {
            return;
        }

        ReflectionCompat.invokeCompatibleMethodByShape(player, new Object[] { message });
    }

    public static void displayProviderLine(
        LocalPlayer player,
        PermaClickUpdateService.UpdateStatus status,
        String providerKey,
        String providerLabel
    ) {
        String version = status.providerVersions().get(providerKey);
        String url = status.providerVersionUrls().get(providerKey);

        if (version == null || version.isBlank()) {
            displayClientMessageCompat(
                player,
                Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_ERROR, providerLabel).withStyle(ChatFormatting.GRAY),
                false
            );
            return;
        }

        MutableComponent line = Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_OK, providerLabel, version)
            .append(Component.literal(" "))
            .withStyle(ChatFormatting.GRAY);

        if (url != null && !url.isBlank()) {
            MutableComponent link = Component.literal("[" + url + "]")
                .withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                );
            line.append(link);
        }

        displayClientMessageCompat(player, line, false);
    }
}
