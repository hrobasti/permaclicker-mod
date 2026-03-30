package com.github.hrobasti.permaclicker.fabric.mixin;

import com.github.hrobasti.permaclicker.fabric.PermaClickFabricEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
final class FabricPauseScreenSetScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void permaclicker$blockPauseScreen(Screen screen, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;

        if (PermaClickFabricEntrypoint.isPermaClickerConfigScreen(screen)) {
            if (PermaClickFabricEntrypoint.shouldBlockSettingsOpenForBackgroundMode(minecraft)) {
                ci.cancel();
                return;
            }

            PermaClickFabricEntrypoint.stopForFocusedSettingsOpen(minecraft);
        }

        if (screen instanceof PauseScreen && PermaClickFabricEntrypoint.isPauseBlockContextActive(minecraft)) {
            ci.cancel();
            return;
        }

        if (isChatScreen(screen) && PermaClickFabricEntrypoint.isChatBlockContextActive(minecraft)) {
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
