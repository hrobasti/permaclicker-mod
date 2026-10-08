package net.kroet.permaclicker.client;

import java.net.URI;
import java.net.URISyntaxException;
import net.kroet.permaclicker.common.core.PermaClickTextKeys;
import net.kroet.permaclicker.common.update.PermaClickUpdateService;
import net.kroet.permaclicker.common.update.ProviderRelease;
import net.kroet.permaclicker.common.update.UpdateProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Shared chat-message helpers used by both loader entrypoints for update-check
 * and other player-facing notices.
 */
public final class ClientMessages {
    private ClientMessages() {
    }

    public static void sendChat(LocalPlayer player, Component message) {
        if (player != null && message != null) {
            player.sendSystemMessage(message);
        }
    }

    public static void displayProviderLine(LocalPlayer player, PermaClickUpdateService.UpdateStatus status, UpdateProvider provider) {
        ProviderRelease release = status.releases().get(provider);
        if (release == null) {
            if (status.errors().containsKey(provider)) {
                sendChat(player, Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_ERROR, provider.label()).withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        MutableComponent line = Component.translatable(PermaClickTextKeys.UPDATE_PROVIDER_OK, provider.label(), release.version())
                .append(Component.literal(" "))
                .withStyle(ChatFormatting.GRAY);
        URI uri = parseUri(release.url());
        if (uri != null) {
            line.append(Component.literal("[" + release.url() + "]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenUrl(uri))));
        }

        sendChat(player, line);
    }

    private static URI parseUri(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
