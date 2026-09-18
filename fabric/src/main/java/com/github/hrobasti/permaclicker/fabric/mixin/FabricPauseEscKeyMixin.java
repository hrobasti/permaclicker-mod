package com.github.hrobasti.permaclicker.fabric.mixin;

import com.github.hrobasti.permaclicker.fabric.PermaClickFabricEntrypoint;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
final class FabricPauseEscKeyMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void permaclicker$blockEscPauseOpening(long window, int unknown, KeyEvent keyEvent, CallbackInfo ci) {
        if (keyEvent == null || keyEvent.key() != InputConstants.KEY_ESCAPE) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gui.screen() != null) {
            return;
        }

        if (PermaClickFabricEntrypoint.isPauseBlockContextActive(minecraft)) {
            ci.cancel();
            return;
        }

        PermaClickFabricEntrypoint.stopForFocusedEscPause(minecraft);
    }
}
