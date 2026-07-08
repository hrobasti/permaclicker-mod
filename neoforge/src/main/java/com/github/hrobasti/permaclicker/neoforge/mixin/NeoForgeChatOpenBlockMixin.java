package com.github.hrobasti.permaclicker.neoforge.mixin;

import com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks opening the chat/command screen while PermaClicker's chat-block context is active.
 *
 * <p>In 26.2 chat is opened through {@code Gui.openChatScreen} / {@code Gui.openChatAndAddText}
 * instead of {@code Minecraft.setScreen}, so this must be intercepted on {@code Gui} directly.</p>
 */
@Mixin(Gui.class)
final class NeoForgeChatOpenBlockMixin {
    @Inject(method = { "openChatScreen", "openChatAndAddText" }, at = @At("HEAD"), cancellable = true)
    private void permaclicker$blockChatOpen(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && PermaClickNeoForgeEntrypoint.isChatBlockContextActive(minecraft)) {
            ci.cancel();
        }
    }
}
