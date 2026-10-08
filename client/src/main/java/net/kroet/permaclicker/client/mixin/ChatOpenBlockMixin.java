package net.kroet.permaclicker.client.mixin;

import net.kroet.permaclicker.client.PermaClickClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks opening the chat/command screen while PermaClicker's chat-block
 * context is active. Chat opens through {@code Gui}, not
 * {@code Minecraft.setScreen}, so it is intercepted here.
 */
@Mixin(Gui.class)
final class ChatOpenBlockMixin {
    @Inject(method = {"openChatScreen", "openChatAndAddText"}, at = @At("HEAD"), cancellable = true)
    private void permaclicker$blockChatOpen(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && PermaClickClient.isChatBlockContextActive(minecraft)) {
            ci.cancel();
        }
    }
}
