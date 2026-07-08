package com.github.hrobasti.permaclicker.neoforge.mixin;

import com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
final class NeoForgePauseScreenSetScreenMixin {
    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void permaclicker$blockPauseScreen(Screen screen, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;

        if (PermaClickNeoForgeEntrypoint.isPermaClickerConfigScreen(screen)) {
            if (PermaClickNeoForgeEntrypoint.shouldBlockSettingsOpenForBackgroundMode(minecraft)) {
                ci.cancel();
                return;
            }

            PermaClickNeoForgeEntrypoint.stopForFocusedSettingsOpen(minecraft);
        }

        if (screen instanceof PauseScreen && PermaClickNeoForgeEntrypoint.isPauseBlockContextActive(minecraft)) {
            ci.cancel();
            return;
        }

        if (isChatScreen(screen) && PermaClickNeoForgeEntrypoint.isChatBlockContextActive(minecraft)) {
            ci.cancel();
        }
    }

    private static boolean isChatScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        if (screen instanceof ChatScreen) {
            return true;
        }

        String simpleName = screen.getClass().getSimpleName();
        return "ChatScreen".equals(simpleName) || simpleName.endsWith("ChatScreen");
    }
}
