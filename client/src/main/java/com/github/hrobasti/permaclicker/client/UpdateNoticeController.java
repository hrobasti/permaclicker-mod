package com.github.hrobasti.permaclicker.client;

import com.github.hrobasti.permaclicker.common.config.PermaClickConfig;
import com.github.hrobasti.permaclicker.common.core.PermaClickBridge;
import com.github.hrobasti.permaclicker.common.core.PermaClickTextKeys;
import com.github.hrobasti.permaclicker.common.update.PermaClickUpdateService;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Background update check plus the one-time toggle-key-reset notice (see the 26.3 GLFW-&gt;SDL
 * migration note in CLAUDE.md), both flushed to chat once the player is available. Shared
 * between Fabric and NeoForge; each loader entrypoint owns one instance.
 */
public final class UpdateNoticeController {
    private volatile PermaClickUpdateService.UpdateStatus pendingUpdateStatus;
    private boolean pendingToggleKeyResetNotice;

    public void scheduleUpdateCheck(PermaClickBridge bridge) {
        PermaClickConfig config = bridge.currentConfig();
        if (config == null || !config.updateCheckEnabled()) {
            return;
        }

        String currentVersion = ClientMessages.resolveCurrentVersion();
        String minecraftVersion = SharedConstants.getCurrentVersion().toString();
        var updateChannel = config.updateChannel();

        CompletableFuture.runAsync(() -> {
            PermaClickUpdateService.UpdateStatus status = PermaClickUpdateService.checkForUpdates(
                currentVersion,
                minecraftVersion,
                updateChannel
            );

            if (status.hasUpdate()) {
                pendingUpdateStatus = status;
            }
        });
    }

    public void markToggleKeyReset() {
        pendingToggleKeyResetNotice = true;
    }

    public void flushPendingUpdateMessage(Minecraft minecraft) {
        PermaClickUpdateService.UpdateStatus status = pendingUpdateStatus;
        if (status == null || !status.hasUpdate()) {
            return;
        }
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        LocalPlayer player = minecraft.player;

        ClientMessages.displayClientMessageCompat(
            player,
            Component.translatable(PermaClickTextKeys.UPDATE_AVAILABLE, status.currentVersion(), status.latestVersion())
                .withStyle(ChatFormatting.WHITE),
            false
        );
        ClientMessages.displayProviderLine(player, status, "modrinth", "Modrinth");
        ClientMessages.displayProviderLine(player, status, "curseforge", "CurseForge");

        pendingUpdateStatus = null;
    }

    public void flushPendingToggleKeyResetNotice(Minecraft minecraft) {
        if (!pendingToggleKeyResetNotice) {
            return;
        }

        if (minecraft == null || minecraft.player == null) {
            return;
        }

        ClientMessages.displayClientMessageCompat(
            minecraft.player,
            Component.translatable(PermaClickTextKeys.TOGGLE_KEY_RESET).withStyle(ChatFormatting.YELLOW),
            false
        );
        pendingToggleKeyResetNotice = false;
    }
}
